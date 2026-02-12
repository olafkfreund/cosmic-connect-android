/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.CameraPlugin

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.Helpers.NotificationHelper
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.di.PluginCreator
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.PluginSettingsFragment

/**
 * CameraPlugin - Use Android device camera as virtual webcam on COSMIC Desktop
 *
 * This plugin enables Android device cameras to be used as virtual webcams on
 * COSMIC Desktop through V4L2 loopback. Video is encoded using H.264 MediaCodec
 * and streamed over the secure COSMIC Connect connection.
 *
 * ## Features
 *
 * - **Camera Selection**: Switch between front/back/external cameras
 * - **Resolution Control**: Desktop can request specific resolutions
 * - **Frame Rate Control**: Configurable FPS (up to device maximum)
 * - **Bitrate Control**: Adaptive bitrate for network conditions
 * - **Flash Control**: Toggle flash/torch mode
 *
 * ## Protocol
 *
 * **Phone → Desktop:**
 * - `cconnect.camera.capability` - Advertise cameras and capabilities
 * - `cconnect.camera.status` - Report streaming status
 * - `cconnect.camera.frame` - Send H.264 encoded frames
 *
 * **Desktop → Phone:**
 * - `cconnect.camera.start` - Start streaming with parameters
 * - `cconnect.camera.stop` - Stop streaming
 * - `cconnect.camera.settings` - Change settings while streaming
 *
 * ## Implementation Notes
 *
 * - Uses Camera2 API for camera access (Android 5.0+)
 * - Uses MediaCodec for hardware H.264 encoding
 * - Frames sent via TCP payload transfer mechanism
 * - Requires CAMERA permission and foreground service
 *
 * ## Android 14+ Requirements
 *
 * - CAMERA permission at runtime
 * - FOREGROUND_SERVICE_CAMERA for streaming in background
 * - Notification while streaming (foreground service)
 *
 * @see CameraPacketsFFI
 * @see CameraStreamingService (TODO: Issue #103)
 */
class CameraPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): CameraPlugin
    }

    companion object {
        private const val TAG = "CameraPlugin"

        /** Notification ID for camera start request */
        private const val NOTIFICATION_ID_CAMERA_REQUEST = 31415

        /** Intent action for starting camera from notification */
        const val ACTION_START_CAMERA = "org.cosmicext.connect.camera.START"

        /** Intent action for denying camera request from notification */
        const val ACTION_DENY_CAMERA = "org.cosmicext.connect.camera.DENY"

        /** Extra key for device ID in intent */
        const val EXTRA_DEVICE_ID = "device_id"

        /**
         * Recommended bitrate for different quality levels (in kbps)
         */
        object BitratePresets {
            const val LOW = 1000      // 480p @ 30fps
            const val MEDIUM = 2000   // 720p @ 30fps
            const val HIGH = 4000     // 1080p @ 30fps
        }
    }

    // ========================================================================
    // State Management
    // ========================================================================

    /** Camera manager for accessing device cameras */
    private var cameraManager: CameraManager? = null

    /** Currently available cameras */
    private var availableCameras: List<CameraInfo> = emptyList()

    /** Current streaming state */
    private var streamingState: StreamingStatus = StreamingStatus.STOPPED

    /** Active camera ID when streaming */
    private var activeCameraId: Int = 0

    /** Current streaming resolution */
    private var currentResolution: Resolution = Resolution.HD_720

    /** Current streaming FPS */
    private var currentFps: Int = 30

    /** Current streaming bitrate */
    private var currentBitrate: Int = BitratePresets.MEDIUM

    /** Pending camera start request (stored when app is in background) */
    private var pendingStartRequest: CameraStartRequest? = null

    /** Notification manager for camera request notifications */
    private var notificationManager: NotificationManager? = null

    /** Broadcast receiver for notification actions */
    private val cameraActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_START_CAMERA -> {
                    Log.d(TAG, "Camera start action received from notification")
                    dismissCameraRequestNotification()
                    // Start camera with pending request parameters
                    pendingStartRequest?.let { request ->
                        activeCameraId = request.cameraId
                        currentResolution = Resolution(request.width, request.height)
                        currentFps = request.fps
                        currentBitrate = request.bitrate
                        startStreaming()
                    }
                    pendingStartRequest = null
                }
                ACTION_DENY_CAMERA -> {
                    Log.d(TAG, "Camera deny action received from notification")
                    dismissCameraRequestNotification()
                    pendingStartRequest = null
                    sendStatus(StreamingStatus.STOPPED, error = "User denied camera request")
                }
            }
        }
    }

    /** Whether the broadcast receiver is registered */
    private var receiverRegistered = false

    // ========================================================================
    // Streaming Components
    // ========================================================================

    /** Capture service for camera access */
    private var captureService: CameraCaptureService? = null

    /** Service bound state */
    private var serviceBound: Boolean = false

    /** H.264 encoder */
    private var encoder: H264Encoder? = null

    /** Network streaming client */
    private var streamClient: CameraStreamClient? = null

    /** Service connection callback */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? CameraCaptureService.LocalBinder
            captureService = localBinder?.getService()
            serviceBound = true
            Log.d(TAG, "CameraCaptureService bound")

            // If streaming was requested, continue with camera setup
            if (streamingState == StreamingStatus.STARTING) {
                initializeStreamingComponents()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureService = null
            serviceBound = false
            Log.d(TAG, "CameraCaptureService disconnected")

            // Handle unexpected disconnection during streaming
            if (streamingState == StreamingStatus.STREAMING) {
                streamingState = StreamingStatus.ERROR
                sendStatus(StreamingStatus.ERROR, error = "Camera service disconnected")
            }
        }
    }

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    override val displayName: String
        get() = context.getString(R.string.camera_plugin_title)

    override val description: String
        get() = context.getString(R.string.camera_plugin_description)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(
            CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            CameraPacketsFFI.PACKET_TYPE_CAMERA_REQUEST, // Alternate name used by some desktop clients
            CameraPacketsFFI.PACKET_TYPE_CAMERA_STOP,
            CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS
        )

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(
            CameraPacketsFFI.PACKET_TYPE_CAMERA_CAPABILITY,
            CameraPacketsFFI.PACKET_TYPE_CAMERA_STATUS,
            CameraPacketsFFI.PACKET_TYPE_CAMERA_FRAME
        )

    // ========================================================================
    // Lifecycle
    // ========================================================================

    override fun onCreate(): Boolean {
        Log.d(TAG, "CameraPlugin onCreate")

        // Initialize camera manager
        cameraManager = ContextCompat.getSystemService(context, CameraManager::class.java)
        if (cameraManager == null) {
            Log.e(TAG, "CameraManager not available")
            return false
        }

        // Initialize notification manager
        notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)

        // Register broadcast receiver for notification actions
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_START_CAMERA)
                addAction(ACTION_DENY_CAMERA)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(cameraActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(cameraActionReceiver, filter)
            }
            receiverRegistered = true
        }

        // Enumerate available cameras
        enumerateCameras()

        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "CameraPlugin onDestroy")

        // Dismiss any pending camera request notification
        dismissCameraRequestNotification()
        pendingStartRequest = null

        // Unregister broadcast receiver
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(cameraActionReceiver)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver was not registered")
            }
            receiverRegistered = false
        }

        // Stop streaming if active
        if (streamingState == StreamingStatus.STREAMING) {
            stopStreaming()
        }

        cameraManager = null
        notificationManager = null
        availableCameras = emptyList()
    }

    /**
     * Called when device connection is established
     *
     * Sends camera capability packet to advertise available cameras.
     */
    fun onDeviceConnected() {
        if (availableCameras.isNotEmpty()) {
            sendCapabilities()
        }
    }

    // ========================================================================
    // Packet Reception
    // ========================================================================

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet

        return when {
            np.isCameraStart -> handleStartRequest(np)
            np.isCameraStop -> handleStopRequest()
            np.isCameraSettings -> handleSettingsRequest(np)
            else -> false
        }
    }

    /**
     * Handle camera start request from desktop
     *
     * @param packet Start request packet with parameters
     * @return true if handled successfully
     */
    private fun handleStartRequest(packet: NetworkPacket): Boolean {
        Log.d(TAG, "Received camera start request")

        val request = packet.toCameraStartRequest()
        if (request == null) {
            Log.e(TAG, "Failed to parse start request")
            sendStatus(StreamingStatus.ERROR, error = "Invalid start request")
            return false
        }

        // Validate camera ID
        if (request.cameraId >= availableCameras.size) {
            Log.e(TAG, "Invalid camera ID: ${request.cameraId}")
            sendStatus(StreamingStatus.ERROR, error = "Invalid camera ID")
            return false
        }

        // Validate codec
        if (request.codec != "h264") {
            Log.e(TAG, "Unsupported codec: ${request.codec}")
            sendStatus(StreamingStatus.ERROR, error = "Unsupported codec: ${request.codec}")
            return false
        }

        // Check if app is in foreground
        // Android 10+ restricts camera access for background-started services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isAppInForeground()) {
            Log.w(TAG, "App is in background, showing notification for user to start camera")
            showCameraRequestNotification(request)
            return true
        }

        // Update streaming parameters
        activeCameraId = request.cameraId
        currentResolution = Resolution(request.width, request.height)
        currentFps = request.fps
        currentBitrate = request.bitrate

        // Start streaming
        return startStreaming()
    }

    /**
     * Handle camera stop request from desktop
     *
     * @return true if handled successfully
     */
    private fun handleStopRequest(): Boolean {
        Log.d(TAG, "Received camera stop request")
        return stopStreaming()
    }

    /**
     * Handle camera settings change request
     *
     * @param packet Settings request packet with changes
     * @return true if handled successfully
     */
    private fun handleSettingsRequest(packet: NetworkPacket): Boolean {
        Log.d(TAG, "Received camera settings request")

        val request = packet.toCameraSettingsRequest()
        if (request == null) {
            Log.e(TAG, "Failed to parse settings request")
            return false
        }

        // Apply changes
        var needsRestart = false

        request.cameraId?.let {
            if (it != activeCameraId && it < availableCameras.size) {
                activeCameraId = it
                needsRestart = true
            }
        }

        if (request.width != null && request.height != null) {
            val newResolution = Resolution(request.width, request.height)
            if (newResolution != currentResolution) {
                currentResolution = newResolution
                needsRestart = true
            }
        }

        request.fps?.let {
            if (it != currentFps) {
                currentFps = it
                needsRestart = true
            }
        }

        request.bitrate?.let {
            if (it != currentBitrate) {
                currentBitrate = it
                // Bitrate can be changed without restart on some devices
            }
        }

        // Handle flash/autofocus (TODO: Issue #103)
        request.flash?.let { flash ->
            Log.d(TAG, "Flash request: $flash (not yet implemented)")
        }

        request.autofocus?.let { autofocus ->
            Log.d(TAG, "Autofocus request: $autofocus (not yet implemented)")
        }

        // Restart streaming if needed
        if (needsRestart && streamingState == StreamingStatus.STREAMING) {
            stopStreaming()
            startStreaming()
        }

        return true
    }

    // ========================================================================
    // Camera Enumeration
    // ========================================================================

    /**
     * Enumerate available cameras and their capabilities
     *
     * Uses Camera2 API to query all cameras and their supported
     * resolutions, frame rates, and other capabilities.
     */
    private fun enumerateCameras() {
        val manager = cameraManager ?: return
        val cameras = mutableListOf<CameraInfo>()

        try {
            for ((index, cameraId) in manager.cameraIdList.withIndex()) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // Get facing direction
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val facing = when (lensFacing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
                    CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
                    else -> CameraFacing.BACK
                }

                // Get camera name
                val name = when (facing) {
                    CameraFacing.FRONT -> context.getString(R.string.camera_front)
                    CameraFacing.BACK -> context.getString(R.string.camera_back)
                    CameraFacing.EXTERNAL -> context.getString(R.string.camera_external)
                }

                // Get supported output sizes
                val streamConfigMap = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

                // Get sizes for video encoder (MediaCodec surface)
                val outputSizes = streamConfigMap?.getOutputSizes(
                    android.media.MediaRecorder::class.java
                ) ?: arrayOf()

                // Filter to common video resolutions
                val supportedResolutions = outputSizes
                    .filter { it.width <= 1920 && it.height <= 1080 }
                    .sortedByDescending { it.width * it.height }
                    .take(5) // Limit to 5 resolutions
                    .map { Resolution(it.width, it.height) }

                // Get max resolution
                val maxSize = supportedResolutions.firstOrNull() ?: Resolution.HD_720

                cameras.add(
                    CameraInfo(
                        id = index,
                        name = name,
                        facing = facing,
                        maxWidth = maxSize.width,
                        maxHeight = maxSize.height,
                        resolutions = supportedResolutions
                    )
                )
            }

            availableCameras = cameras
            Log.d(TAG, "Enumerated ${cameras.size} cameras")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enumerate cameras", e)
        }
    }

    // ========================================================================
    // Streaming Control
    // ========================================================================

    /**
     * Start camera streaming
     *
     * Initializes Camera2 capture session and MediaCodec encoder.
     * Frames are sent to the desktop via the payload transfer mechanism.
     *
     * @return true if streaming started successfully
     */
    private fun startStreaming(): Boolean {
        if (streamingState == StreamingStatus.STREAMING) {
            Log.w(TAG, "Already streaming")
            return true
        }

        Log.i(TAG, "Starting camera streaming: camera=$activeCameraId, " +
                "${currentResolution.width}x${currentResolution.height}@${currentFps}fps, " +
                "${currentBitrate}kbps")

        // Send starting status
        sendStatus(StreamingStatus.STARTING)

        // Bind to capture service (streaming continues in service connection callback)
        if (!serviceBound) {
            val intent = Intent(context, CameraCaptureService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            // Also start as foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Binding to CameraCaptureService...")
        } else {
            // Service already bound, initialize components
            initializeStreamingComponents()
        }

        return true
    }

    /**
     * Initialize streaming components after service is bound
     *
     * Creates encoder and stream client, then starts capture.
     */
    private fun initializeStreamingComponents() {
        val service = captureService
        if (service == null) {
            Log.e(TAG, "Capture service not available")
            sendStatus(StreamingStatus.ERROR, error = "Camera service not available")
            return
        }

        try {
            // Create stream client
            streamClient = CameraStreamClient(device, streamClientCallback)
            streamClient?.setTargetBitrate(currentBitrate)
            streamClient?.start()

            // Create H.264 encoder
            encoder = H264Encoder(
                width = currentResolution.width,
                height = currentResolution.height,
                fps = currentFps,
                bitrateKbps = currentBitrate,
                callback = encoderCallback
            )

            // Configure encoder (must be called before getInputSurface)
            encoder?.configure()

            // Get encoder input surface
            val encoderSurface = encoder?.getInputSurface()
            if (encoderSurface == null) {
                Log.e(TAG, "Failed to get encoder input surface")
                cleanupStreamingComponents()
                sendStatus(StreamingStatus.ERROR, error = "Encoder initialization failed")
                return
            }

            // Start capture service with encoder surface
            service.setCaptureListener(captureListener)
            service.startCapture(
                cameraId = activeCameraId,
                width = currentResolution.width,
                height = currentResolution.height,
                fps = currentFps,
                surface = encoderSurface
            )

            // Encoder started by capture listener when capture starts
            Log.i(TAG, "Streaming components initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize streaming components", e)
            cleanupStreamingComponents()
            sendStatus(StreamingStatus.ERROR, error = "Initialization failed: ${e.message}")
        }
    }

    /**
     * Stop camera streaming
     *
     * Stops the camera capture and encoder, releases resources.
     *
     * @return true if streaming stopped successfully
     */
    private fun stopStreaming(): Boolean {
        if (streamingState == StreamingStatus.STOPPED) {
            Log.w(TAG, "Already stopped")
            return true
        }

        Log.i(TAG, "Stopping camera streaming")

        // Send stopping status
        sendStatus(StreamingStatus.STOPPING)

        // Stop capture service
        captureService?.stopCapture()

        // Cleanup components
        cleanupStreamingComponents()

        // Unbind from service
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }

        // Stop foreground service
        val intent = Intent(context, CameraCaptureService::class.java)
        context.stopService(intent)

        streamingState = StreamingStatus.STOPPED
        sendStatus(StreamingStatus.STOPPED)

        Log.i(TAG, "Camera streaming stopped")
        return true
    }

    /**
     * Cleanup streaming components
     */
    private fun cleanupStreamingComponents() {
        // Stop stream client
        streamClient?.stop()
        streamClient = null

        // Stop encoder
        encoder?.stop()
        encoder?.release()
        encoder = null
    }

    // ========================================================================
    // Component Callbacks
    // ========================================================================

    /** Capture service listener */
    private val captureListener = object : CameraCaptureService.CaptureListener {
        override fun onCaptureStarted(cameraId: String, width: Int, height: Int, fps: Int) {
            Log.i(TAG, "Capture started: camera=$cameraId, ${width}x${height}@${fps}fps")

            // Start encoder
            encoder?.start()

            // Update state
            streamingState = StreamingStatus.STREAMING
            sendStatus(StreamingStatus.STREAMING)
        }

        override fun onCaptureStopped() {
            Log.i(TAG, "Capture stopped")
        }

        override fun onCaptureError(error: String) {
            Log.e(TAG, "Capture error: $error")
            cleanupStreamingComponents()
            streamingState = StreamingStatus.ERROR
            sendStatus(StreamingStatus.ERROR, error = error)
        }

        override fun onFrameCaptured(timestampNanos: Long) {
            // Frame captured, encoder will output encoded data via callback
        }
    }

    /** Encoder callback */
    private val encoderCallback = object : H264Encoder.EncoderCallback {
        override fun onSpsPpsAvailable(sps: ByteArray, pps: ByteArray) {
            Log.d(TAG, "SPS/PPS available: sps=${sps.size}, pps=${pps.size}")
            streamClient?.sendSpsPps(sps, pps)
        }

        override fun onEncodedFrame(data: ByteArray, frameType: FrameType, timestampUs: Long) {
            streamClient?.sendFrame(data, frameType, timestampUs)
        }

        override fun onError(error: Throwable) {
            Log.e(TAG, "Encoder error", error)
            stopStreaming()
            sendStatus(StreamingStatus.ERROR, error = "Encoder error: ${error.message}")
        }
    }

    /** Stream client callback */
    private val streamClientCallback = object : CameraStreamClient.StreamCallback {
        override fun onStreamStarted() {
            Log.d(TAG, "Stream client started")
        }

        override fun onStreamStopped() {
            Log.d(TAG, "Stream client stopped")
        }

        override fun onStreamError(error: Throwable) {
            Log.e(TAG, "Stream error", error)
            stopStreaming()
            sendStatus(StreamingStatus.ERROR, error = "Network error: ${error.message}")
        }

        override fun onBandwidthUpdate(kbps: Int) {
            // Could update UI or log bandwidth stats
        }

        override fun onCongestionDetected() {
            Log.w(TAG, "Network congestion detected, reducing bitrate")
            // Reduce bitrate by 25%
            val newBitrate = (currentBitrate * 0.75).toInt().coerceAtLeast(500)
            if (newBitrate != currentBitrate) {
                currentBitrate = newBitrate
                encoder?.setBitrate(newBitrate)
                streamClient?.setTargetBitrate(newBitrate)
            }
        }
    }

    // ========================================================================
    // Packet Sending
    // ========================================================================

    /**
     * Send camera capabilities to desktop
     */
    private fun sendCapabilities() {
        if (availableCameras.isEmpty()) {
            Log.w(TAG, "No cameras to advertise")
            return
        }

        val maxFps = 60 // TODO: Query from device capabilities
        val maxBitrate = 8000

        val packet = CameraPacketsFFI.createCapabilityPacket(
            cameras = availableCameras,
            maxBitrate = maxBitrate,
            maxFps = maxFps
        )

        device.sendPacket(TransferPacket(packet))
        Log.d(TAG, "Sent camera capabilities: ${availableCameras.size} cameras")
    }

    /**
     * Send streaming status update to desktop
     *
     * @param status Current streaming status
     * @param error Error message (only for error status)
     */
    private fun sendStatus(status: StreamingStatus, error: String? = null) {
        streamingState = status

        val packet = CameraPacketsFFI.createStatusPacket(
            status = status,
            cameraId = activeCameraId,
            width = currentResolution.width,
            height = currentResolution.height,
            fps = currentFps,
            bitrate = currentBitrate,
            error = error
        )

        device.sendPacket(TransferPacket(packet))
        Log.d(TAG, "Sent camera status: $status")
    }

    // ========================================================================
    // Settings
    // ========================================================================

    override fun hasSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment =
        CameraSettingsFragment.newInstance(pluginKey, R.xml.cameraplugin_preferences)

    // ========================================================================
    // Background Camera Request Notification
    // ========================================================================

    /**
     * Check if the app is currently in the foreground.
     *
     * On Android 10+ (API 29), foreground services started from background cannot
     * access camera, microphone, or location. We need to detect this and show
     * a notification to prompt user interaction.
     *
     * @return true if app is in foreground, false if in background
     */
    private fun isAppInForeground(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return false

        val appProcesses = activityManager.runningAppProcesses ?: return false

        for (process in appProcesses) {
            if (process.processName == context.packageName) {
                return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }
        }
        return false
    }

    /**
     * Show a notification to prompt the user to start camera streaming.
     *
     * This is used when a camera start request is received while the app is
     * in the background. Android restricts camera access for background-started
     * services, so we need user interaction to gain foreground status.
     *
     * @param request The camera start request parameters to use when user accepts
     */
    private fun showCameraRequestNotification(request: CameraStartRequest) {
        Log.d(TAG, "Showing camera request notification")

        // Store the request for when user taps the notification
        pendingStartRequest = request

        // Create pending intent for "Start Camera" action
        val startIntent = Intent(ACTION_START_CAMERA).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_DEVICE_ID, device.deviceId)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create pending intent for "Deny" action
        val denyIntent = Intent(ACTION_DENY_CAMERA).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_DEVICE_ID, device.deviceId)
        }
        val denyPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            denyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, NotificationHelper.Channels.CAMERA)
            .setSmallIcon(R.drawable.ic_camera_24dp)
            .setContentTitle(context.getString(R.string.camera_start_request_title, device.name))
            .setContentText(context.getString(R.string.camera_start_request_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setAutoCancel(true)
            .setContentIntent(startPendingIntent)
            .addAction(
                R.drawable.ic_camera_24dp,
                context.getString(R.string.camera_start_request_action),
                startPendingIntent
            )
            .addAction(
                R.drawable.ic_reject_pairing_24dp,
                context.getString(R.string.camera_start_request_deny),
                denyPendingIntent
            )
            .build()

        notificationManager?.notify(NOTIFICATION_ID_CAMERA_REQUEST, notification)

        // Send status indicating we're waiting for user
        sendStatus(StreamingStatus.STARTING, error = "Waiting for user to grant camera access")
    }

    /**
     * Dismiss the camera request notification if it's showing.
     */
    private fun dismissCameraRequestNotification() {
        notificationManager?.cancel(NOTIFICATION_ID_CAMERA_REQUEST)
    }

    // ========================================================================
    // Permissions
    // ========================================================================

    override val requiredPermissions: Array<String>
        get() {
            val permissions = mutableListOf(Manifest.permission.CAMERA)

            // Android 14+ requires foreground service camera permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
            }

            // Android 13+ requires notification permission for foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            return permissions.toTypedArray()
        }

    @get:androidx.annotation.StringRes
    override val permissionExplanation: Int
        get() = R.string.camera_permission_explanation

    /**
     * Allow plugin to load even without camera permission so it appears in UI.
     * Users can then grant permission from the plugin settings.
     */
    override fun loadPluginWhenRequiredPermissionsMissing(): Boolean = true

    // ========================================================================
    // UI Actions
    // ========================================================================

    override fun getUiButtons(): List<PluginUiButton> {
        return listOf(
            PluginUiButton(
                name = context.getString(R.string.camera_plugin_title),
                iconRes = R.drawable.ic_camera_24dp,
                onClick = { activity ->
                    // TODO: Issue #102 - Launch CameraActivity with preview/controls
                    Log.d(TAG, "Camera UI button clicked (not yet implemented)")
                }
            )
        )
    }

    // ========================================================================
    // State Queries
    // ========================================================================

    /**
     * Check if currently streaming
     */
    fun isStreaming(): Boolean = streamingState == StreamingStatus.STREAMING

    /**
     * Get list of available cameras
     */
    fun getAvailableCameras(): List<CameraInfo> = availableCameras

    /**
     * Get current streaming resolution
     */
    fun getCurrentResolution(): Resolution = currentResolution

    /**
     * Get current streaming FPS
     */
    fun getCurrentFps(): Int = currentFps

    /**
     * Get current streaming bitrate
     */
    fun getCurrentBitrate(): Int = currentBitrate
}
