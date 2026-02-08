/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.WebcamPlugin

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * Allows the paired desktop to use the Android device's camera as a webcam.
 *
 * Receives start/stop/capability requests from the desktop and manages camera
 * capture. Camera frames are transmitted via the device link using the existing
 * CameraPlugin infrastructure (H264Encoder + CameraStreamClient).
 *
 * This plugin is disabled by default and requires the CAMERA permission.
 */
class WebcamPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): WebcamPlugin
    }

    /** Whether the webcam is currently streaming to the desktop. */
    var isStreaming: Boolean = false
        private set

    /** The camera ID currently being used for streaming. */
    var activeCameraId: String? = null
        private set

    /** Active streaming resolution width. */
    var activeWidth: Int? = null
        private set

    /** Active streaming resolution height. */
    var activeHeight: Int? = null
        private set

    private var listener: WebcamStateListener? = null

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_webcam)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_webcam_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_WEBCAM_REQUEST,
        PACKET_TYPE_WEBCAM_CAPABILITY,
    )

    override val outgoingPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_WEBCAM,
        PACKET_TYPE_WEBCAM_CAPABILITY,
    )

    override val isEnabledByDefault: Boolean = false

    override val requiredPermissions: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
    )

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        return when {
            tp.isWebcamStartRequest -> {
                handleStartRequest(tp)
                true
            }
            tp.isWebcamStopRequest -> {
                handleStopRequest()
                true
            }
            tp.isWebcamCapabilityRequest -> {
                handleCapabilityRequest()
                true
            }
            else -> false
        }
    }

    override fun onDestroy() {
        if (isStreaming) {
            stopStreaming()
        }
    }

    fun setWebcamStateListener(l: WebcamStateListener?) {
        listener = l
    }

    private fun handleStartRequest(tp: TransferPacket) {
        val cameraId = tp.webcamRequestCameraId ?: getDefaultCameraId()
        val width = tp.webcamRequestWidth ?: DEFAULT_WIDTH
        val height = tp.webcamRequestHeight ?: DEFAULT_HEIGHT

        activeCameraId = cameraId
        activeWidth = width
        activeHeight = height
        isStreaming = true

        Log.i(TAG, "Webcam start: camera=$cameraId ${width}x${height}")

        val status = WebcamStatus(
            isStreaming = true,
            cameraId = cameraId,
            width = width,
            height = height,
            codec = tp.webcamRequestCodec ?: "h264",
            fps = tp.webcamRequestFps ?: DEFAULT_FPS,
        )
        device.sendPacket(createWebcamStatus(status))
        listener?.onWebcamStateChanged(true, cameraId, width, height)
    }

    private fun handleStopRequest() {
        stopStreaming()
        Log.i(TAG, "Webcam stopped by desktop request")
    }

    private fun stopStreaming() {
        isStreaming = false
        val prevCameraId = activeCameraId
        activeCameraId = null
        activeWidth = null
        activeHeight = null

        device.sendPacket(createWebcamStatus(WebcamStatus(isStreaming = false)))
        listener?.onWebcamStateChanged(false, prevCameraId, null, null)
    }

    private fun handleCapabilityRequest() {
        val capabilities = enumerateCameras()
        device.sendPacket(createWebcamCapabilityResponse(capabilities))
        Log.d(TAG, "Sent ${capabilities.size} camera capabilities")
    }

    internal fun enumerateCameras(): List<WebcamCapability> {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.cameraIdList.mapNotNull { cameraId ->
                val chars = cameraManager.getCameraCharacteristics(cameraId)
                val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: return@mapNotNull null
                val outputSizes = configs.getOutputSizes(android.graphics.ImageFormat.JPEG)
                    ?: return@mapNotNull null
                val maxSize = outputSizes.maxByOrNull { it.width * it.height }
                    ?: return@mapNotNull null
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val facingName = when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back"
                    else -> "External"
                }

                WebcamCapability(
                    cameraId = cameraId,
                    name = "$facingName Camera ($cameraId)",
                    maxWidth = maxSize.width,
                    maxHeight = maxSize.height,
                    supportedCodecs = listOf("h264"),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enumerate cameras", e)
            emptyList()
        }
    }

    private fun getDefaultCameraId(): String {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.cameraIdList.firstOrNull() ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }

    interface WebcamStateListener {
        fun onWebcamStateChanged(isStreaming: Boolean, cameraId: String?, width: Int?, height: Int?)
    }

    companion object {
        private const val TAG = "WebcamPlugin"
        private const val DEFAULT_WIDTH = 1280
        private const val DEFAULT_HEIGHT = 720
        private const val DEFAULT_FPS = 30
    }
}
