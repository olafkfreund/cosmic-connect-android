/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.CameraPlugin

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.cosmic.cosmicconnect.R

/**
 * CameraCaptureService - Foreground service for camera frame capture
 *
 * This service runs in the foreground and captures camera frames using the
 * Camera2 API. Frames are output to a Surface (typically MediaCodec input)
 * for H.264 encoding.
 *
 * ## Features
 *
 * - Camera2 API capture with repeating requests
 * - Front/back camera switching
 * - Resolution and FPS control
 * - Foreground notification with stop action
 * - Clean lifecycle management
 *
 * ## Usage
 *
 * ```kotlin
 * // Start service
 * val intent = Intent(context, CameraCaptureService::class.java)
 * ContextCompat.startForegroundService(context, intent)
 *
 * // Bind to get control
 * bindService(intent, connection, BIND_AUTO_CREATE)
 *
 * // Start capture with MediaCodec surface
 * service.startCapture(cameraId, width, height, fps, encoderSurface)
 *
 * // Switch camera
 * service.switchCamera(newCameraId)
 *
 * // Stop capture
 * service.stopCapture()
 * ```
 *
 * ## Android 14+ Requirements
 *
 * - FOREGROUND_SERVICE_CAMERA permission
 * - Camera permission at runtime
 *
 * @see CameraPlugin
 */
class CameraCaptureService : Service() {

    companion object {
        private const val TAG = "CameraCaptureService"

        /** Notification channel ID */
        const val CHANNEL_ID = "camera_streaming_channel"

        /** Notification ID */
        const val NOTIFICATION_ID = 9102

        /** Action to stop streaming */
        const val ACTION_STOP_STREAMING = "org.cosmic.cosmicconnect.STOP_CAMERA_STREAMING"

        /** Extra: Device name for notification */
        const val EXTRA_DEVICE_NAME = "device_name"
    }

    // ========================================================================
    // Binder for Service Connection
    // ========================================================================

    /**
     * Binder for local service binding
     */
    inner class LocalBinder : Binder() {
        fun getService(): CameraCaptureService = this@CameraCaptureService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    // ========================================================================
    // State
    // ========================================================================

    /** Camera manager for device access */
    private var cameraManager: CameraManager? = null

    /** Currently open camera device */
    private var cameraDevice: CameraDevice? = null

    /** Active capture session */
    private var captureSession: CameraCaptureSession? = null

    /** Surface for frame output (MediaCodec input) */
    private var outputSurface: Surface? = null

    /** Background thread for camera operations */
    private var cameraThread: HandlerThread? = null

    /** Handler for camera callbacks */
    private var cameraHandler: Handler? = null

    /** Current camera ID */
    private var currentCameraId: String? = null

    /** Current resolution */
    private var currentWidth: Int = 1280
    private var currentHeight: Int = 720

    /** Current FPS */
    private var currentFps: Int = 30

    /** Capture state */
    private var isCapturing: Boolean = false

    /** Listener for capture events */
    private var captureListener: CaptureListener? = null

    /** Device name for notification */
    private var deviceName: String? = null

    // ========================================================================
    // Capture Listener Interface
    // ========================================================================

    /**
     * Listener for capture events
     */
    interface CaptureListener {
        /** Called when capture starts */
        fun onCaptureStarted(cameraId: String, width: Int, height: Int, fps: Int)

        /** Called when capture stops */
        fun onCaptureStopped()

        /** Called on capture error */
        fun onCaptureError(error: String)

        /** Called on each frame captured (for frame counting/timing) */
        fun onFrameCaptured(timestampNanos: Long)
    }

    /**
     * Set capture event listener
     */
    fun setCaptureListener(listener: CaptureListener?) {
        captureListener = listener
    }

    // ========================================================================
    // Service Lifecycle
    // ========================================================================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CameraCaptureService onCreate")

        // Create notification channel
        createNotificationChannel()

        // Initialize camera manager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Start background thread
        cameraThread = HandlerThread("CameraThread").apply { start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_STOP_STREAMING -> {
                Log.i(TAG, "Stop action received from notification")
                stopCapture()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Get device name for notification
        deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME)

        // Start as foreground service
        startForegroundWithType()

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "CameraCaptureService onDestroy")

        // Stop capture if active
        stopCapture()

        // Stop background thread
        cameraThread?.quitSafely()
        try {
            cameraThread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Camera thread join interrupted", e)
        }
        cameraThread = null
        cameraHandler = null

        super.onDestroy()
    }

    // ========================================================================
    // Foreground Service
    // ========================================================================

    /**
     * Start foreground with appropriate service type
     */
    private fun startForegroundWithType() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires service type
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Create notification channel (Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.camera_streaming_notification_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.camera_plugin_description)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground notification
     */
    private fun createNotification(): Notification {
        // Stop action intent
        val stopIntent = Intent(this, CameraCaptureService::class.java).apply {
            action = ACTION_STOP_STREAMING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val contentText = if (deviceName != null) {
            getString(R.string.camera_streaming_notification_text, deviceName)
        } else {
            getString(R.string.camera_plugin_description)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.camera_streaming_notification_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_camera_24dp)
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.camera_streaming_stop),
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Update notification (e.g., when streaming state changes)
     */
    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    // ========================================================================
    // Camera Capture Control
    // ========================================================================

    /**
     * Start camera capture
     *
     * Opens the specified camera and starts capturing frames to the output surface.
     *
     * @param cameraId Camera ID to open (0 = back, 1 = front typically)
     * @param width Capture width
     * @param height Capture height
     * @param fps Target frame rate
     * @param surface Output surface (MediaCodec input surface)
     */
    fun startCapture(
        cameraId: Int,
        width: Int,
        height: Int,
        fps: Int,
        surface: Surface
    ) {
        if (isCapturing) {
            Log.w(TAG, "Already capturing, stopping first")
            stopCapture()
        }

        Log.i(TAG, "Starting capture: camera=$cameraId, ${width}x${height}@${fps}fps")

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Camera permission not granted")
            captureListener?.onCaptureError("Camera permission denied")
            return
        }

        // Store parameters
        currentCameraId = cameraId.toString()
        currentWidth = width
        currentHeight = height
        currentFps = fps
        outputSurface = surface

        // Open camera on background thread
        cameraHandler?.post {
            openCamera(cameraId.toString())
        }
    }

    /**
     * Stop camera capture
     *
     * Stops the capture session and closes the camera.
     */
    fun stopCapture() {
        if (!isCapturing && cameraDevice == null) {
            Log.d(TAG, "Not capturing, nothing to stop")
            return
        }

        Log.i(TAG, "Stopping capture")

        try {
            // Stop repeating request
            captureSession?.stopRepeating()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping repeating request", e)
        }

        // Close session
        captureSession?.close()
        captureSession = null

        // Close camera
        cameraDevice?.close()
        cameraDevice = null

        isCapturing = false
        captureListener?.onCaptureStopped()
    }

    /**
     * Switch to different camera
     *
     * Stops current capture and restarts with new camera.
     *
     * @param newCameraId New camera ID
     */
    fun switchCamera(newCameraId: Int) {
        val surface = outputSurface
        if (surface == null) {
            Log.e(TAG, "Cannot switch camera: no output surface")
            return
        }

        Log.i(TAG, "Switching camera to $newCameraId")
        stopCapture()
        startCapture(newCameraId, currentWidth, currentHeight, currentFps, surface)
    }

    /**
     * Change capture resolution
     *
     * Restarts capture with new resolution.
     *
     * @param width New width
     * @param height New height
     */
    fun changeResolution(width: Int, height: Int) {
        val cameraId = currentCameraId?.toIntOrNull() ?: return
        val surface = outputSurface ?: return

        Log.i(TAG, "Changing resolution to ${width}x${height}")
        stopCapture()
        startCapture(cameraId, width, height, currentFps, surface)
    }

    /**
     * Change capture FPS
     *
     * Updates the capture request with new FPS range.
     *
     * @param fps New target FPS
     */
    fun changeFps(fps: Int) {
        currentFps = fps

        // Update capture request if active
        if (isCapturing && captureSession != null && cameraDevice != null) {
            cameraHandler?.post {
                createCaptureRequest()?.let { request ->
                    try {
                        captureSession?.setRepeatingRequest(
                            request,
                            captureCallback,
                            cameraHandler
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to update FPS", e)
                    }
                }
            }
        }
    }

    /**
     * Check if currently capturing
     */
    fun isCapturing(): Boolean = isCapturing

    // ========================================================================
    // Camera2 Implementation
    // ========================================================================

    /**
     * Open camera device
     */
    @Suppress("MissingPermission") // Permission checked in startCapture
    private fun openCamera(cameraId: String) {
        Log.d(TAG, "Opening camera $cameraId")

        try {
            cameraManager?.openCamera(cameraId, cameraStateCallback, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera", e)
            captureListener?.onCaptureError("Failed to open camera: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            captureListener?.onCaptureError("Camera permission denied")
        }
    }

    /**
     * Camera device state callback
     */
    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened: ${camera.id}")
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.w(TAG, "Camera disconnected: ${camera.id}")
            camera.close()
            cameraDevice = null
            isCapturing = false
            captureListener?.onCaptureError("Camera disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errorMsg = when (error) {
                ERROR_CAMERA_IN_USE -> "Camera in use"
                ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                ERROR_CAMERA_DISABLED -> "Camera disabled"
                ERROR_CAMERA_DEVICE -> "Camera device error"
                ERROR_CAMERA_SERVICE -> "Camera service error"
                else -> "Unknown error: $error"
            }
            Log.e(TAG, "Camera error: $errorMsg")
            camera.close()
            cameraDevice = null
            isCapturing = false
            captureListener?.onCaptureError(errorMsg)
        }
    }

    /**
     * Create capture session with output surface
     */
    private fun createCaptureSession() {
        val camera = cameraDevice
        val surface = outputSurface

        if (camera == null || surface == null) {
            Log.e(TAG, "Cannot create session: camera or surface is null")
            return
        }

        Log.d(TAG, "Creating capture session")

        try {
            // Create session with output surface
            val outputs = listOf(surface)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Use OutputConfiguration for Android 9+
                val outputConfigs = outputs.map {
                    android.hardware.camera2.params.OutputConfiguration(it)
                }
                val sessionConfig = android.hardware.camera2.params.SessionConfiguration(
                    android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR,
                    outputConfigs,
                    cameraHandler!!.looper.let { java.util.concurrent.Executors.newSingleThreadExecutor() },
                    captureSessionCallback
                )
                camera.createCaptureSession(sessionConfig)
            } else {
                @Suppress("DEPRECATION")
                camera.createCaptureSession(outputs, captureSessionCallback, cameraHandler)
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture session", e)
            captureListener?.onCaptureError("Failed to create capture session: ${e.message}")
        }
    }

    /**
     * Capture session state callback
     */
    private val captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(TAG, "Capture session configured")
            captureSession = session
            startRepeatingRequest()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "Capture session configuration failed")
            captureListener?.onCaptureError("Capture session configuration failed")
        }
    }

    /**
     * Start repeating capture request
     */
    private fun startRepeatingRequest() {
        val session = captureSession
        if (session == null) {
            Log.e(TAG, "Cannot start request: no session")
            return
        }

        val request = createCaptureRequest()
        if (request == null) {
            Log.e(TAG, "Cannot start request: failed to create request")
            return
        }

        try {
            session.setRepeatingRequest(request, captureCallback, cameraHandler)
            isCapturing = true

            Log.i(TAG, "Capture started: ${currentWidth}x${currentHeight}@${currentFps}fps")
            captureListener?.onCaptureStarted(
                currentCameraId ?: "0",
                currentWidth,
                currentHeight,
                currentFps
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to start repeating request", e)
            captureListener?.onCaptureError("Failed to start capture: ${e.message}")
        }
    }

    /**
     * Create capture request for recording
     */
    private fun createCaptureRequest(): CaptureRequest? {
        val camera = cameraDevice
        val surface = outputSurface

        if (camera == null || surface == null) {
            return null
        }

        return try {
            camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                // Add output surface
                addTarget(surface)

                // Set FPS range
                set(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(currentFps, currentFps)
                )

                // Enable auto-focus
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )

                // Enable auto-exposure
                set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )

                // Enable video stabilization if available
                set(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
            }.build()
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture request", e)
            null
        }
    }

    /**
     * Capture callback for frame timing
     */
    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            // Notify listener of frame capture
            val timestamp = result.get(CaptureResult.SENSOR_TIMESTAMP) ?: System.nanoTime()
            captureListener?.onFrameCaptured(timestamp)
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            Log.w(TAG, "Capture failed: reason=${failure.reason}")
        }
    }

    // ========================================================================
    // Flash Control
    // ========================================================================

    /**
     * Enable or disable flash/torch
     *
     * @param enabled True to enable flash
     */
    fun setFlashEnabled(enabled: Boolean) {
        val camera = cameraDevice
        val session = captureSession
        val surface = outputSurface

        if (camera == null || session == null || surface == null) {
            Log.w(TAG, "Cannot set flash: not capturing")
            return
        }

        try {
            val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(surface)
                set(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(currentFps, currentFps)
                )
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )

                // Set flash mode
                if (enabled) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }
            }.build()

            session.setRepeatingRequest(request, captureCallback, cameraHandler)
            Log.d(TAG, "Flash ${if (enabled) "enabled" else "disabled"}")
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to set flash", e)
        }
    }

    // ========================================================================
    // Camera Queries
    // ========================================================================

    /**
     * Get supported resolutions for camera
     *
     * @param cameraId Camera ID
     * @return List of supported resolutions
     */
    fun getSupportedResolutions(cameraId: Int): List<Size> {
        return try {
            val characteristics = cameraManager?.getCameraCharacteristics(cameraId.toString())
            val streamConfigMap = characteristics?.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            streamConfigMap?.getOutputSizes(android.media.MediaRecorder::class.java)
                ?.filter { it.width <= 1920 && it.height <= 1080 }
                ?.sortedByDescending { it.width * it.height }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get supported resolutions", e)
            emptyList()
        }
    }

    /**
     * Get supported FPS ranges for camera
     *
     * @param cameraId Camera ID
     * @return List of supported FPS values
     */
    fun getSupportedFpsRanges(cameraId: Int): List<Range<Int>> {
        return try {
            val characteristics = cameraManager?.getCameraCharacteristics(cameraId.toString())
            characteristics?.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get supported FPS ranges", e)
            emptyList()
        }
    }

    /**
     * Check if camera has flash
     *
     * @param cameraId Camera ID
     * @return True if flash is available
     */
    fun hasFlash(cameraId: Int): Boolean {
        return try {
            val characteristics = cameraManager?.getCameraCharacteristics(cameraId.toString())
            characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check flash availability", e)
            false
        }
    }
}
