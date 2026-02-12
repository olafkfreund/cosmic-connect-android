/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.CameraPlugin

import org.cosmicext.connect.Core.CosmicExtConnectException
import org.cosmicext.connect.Core.NetworkPacket

/**
 * CameraPacketsFFI - FFI wrapper for Camera Webcam Streaming plugin
 *
 * This object provides type-safe packet creation and inspection for the Camera
 * plugin, enabling Android device cameras to be used as virtual webcams on
 * COSMIC Desktop through V4L2 loopback.
 *
 * ## Protocol Overview
 *
 * **Phone → Desktop:**
 * - `cconnect.camera.capability` - Advertise camera capabilities
 * - `cconnect.camera.frame` - Video frame data (H.264)
 * - `cconnect.camera.status` - Streaming status updates
 *
 * **Desktop → Phone:**
 * - `cconnect.camera.start` - Start streaming
 * - `cconnect.camera.stop` - Stop streaming
 * - `cconnect.camera.settings` - Change settings (camera switch, resolution, etc.)
 *
 * ## Connection Flow
 *
 * ```
 * 1. Phone sends capability packet after connection
 * 2. Desktop sends start request with resolution/fps/bitrate
 * 3. Phone sends status (starting) and begins streaming
 * 4. Phone sends SPS/PPS + I-Frame + P-Frames
 * 5. Desktop sends stop request to end streaming
 * ```
 *
 * ## H.264 Frame Types
 *
 * - **sps_pps**: Decoder configuration (must be first)
 * - **iframe**: Keyframe (independently decodable)
 * - **pframe**: Delta frame (depends on previous)
 *
 * @see CameraPlugin
 */
object CameraPacketsFFI {

    // ========================================================================
    // Packet Type Constants
    // ========================================================================

    /** Camera capability advertisement packet type */
    const val PACKET_TYPE_CAMERA_CAPABILITY = "cconnect.camera.capability"

    /** Start streaming request packet type */
    const val PACKET_TYPE_CAMERA_START = "cconnect.camera.start"

    /** Start streaming request packet type (alternate name used by some desktop clients) */
    const val PACKET_TYPE_CAMERA_REQUEST = "cconnect.camera.request"

    /** Stop streaming request packet type */
    const val PACKET_TYPE_CAMERA_STOP = "cconnect.camera.stop"

    /** Settings change request packet type */
    const val PACKET_TYPE_CAMERA_SETTINGS = "cconnect.camera.settings"

    /** Video frame data packet type */
    const val PACKET_TYPE_CAMERA_FRAME = "cconnect.camera.frame"

    /** Streaming status update packet type */
    const val PACKET_TYPE_CAMERA_STATUS = "cconnect.camera.status"

    // ========================================================================
    // Packet Creation - Phone → Desktop
    // ========================================================================

    /**
     * Create a camera capability packet
     *
     * Advertises available cameras and their capabilities to the desktop.
     * Should be sent after device connection/pairing.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "cconnect.camera.capability",
     *   "body": {
     *     "cameras": [
     *       {
     *         "id": 0,
     *         "name": "Back Camera",
     *         "facing": "back",
     *         "maxResolution": { "width": 1920, "height": 1080 },
     *         "resolutions": [
     *           { "width": 1920, "height": 1080 },
     *           { "width": 1280, "height": 720 }
     *         ]
     *       }
     *     ],
     *     "supportedCodecs": ["h264"],
     *     "audioSupported": false,
     *     "maxResolution": { "width": 1920, "height": 1080 },
     *     "maxBitrate": 8000,
     *     "maxFps": 60
     *   }
     * }
     * ```
     *
     * @param cameras List of available cameras with their capabilities
     * @param maxBitrate Maximum supported bitrate in kbps
     * @param maxFps Maximum supported frame rate
     * @return NetworkPacket ready to send
     */
    fun createCapabilityPacket(
        cameras: List<CameraInfo>,
        maxBitrate: Int = 8000,
        maxFps: Int = 60
    ): NetworkPacket {
        require(cameras.isNotEmpty()) { "At least one camera must be available" }

        val maxResolution = cameras.maxByOrNull { it.maxWidth * it.maxHeight }
            ?.let { mapOf("width" to it.maxWidth, "height" to it.maxHeight) }
            ?: mapOf("width" to 1920, "height" to 1080)

        val camerasJson = cameras.map { camera ->
            mapOf(
                "id" to camera.id,
                "name" to camera.name,
                "facing" to camera.facing.value,
                "maxResolution" to mapOf("width" to camera.maxWidth, "height" to camera.maxHeight),
                "resolutions" to camera.resolutions.map { res ->
                    mapOf("width" to res.width, "height" to res.height)
                }
            )
        }

        val body = mapOf(
            "cameras" to camerasJson,
            "supportedCodecs" to listOf("h264"),
            "audioSupported" to false,
            "maxResolution" to maxResolution,
            "maxBitrate" to maxBitrate,
            "maxFps" to maxFps
        )

        return NetworkPacket.create(PACKET_TYPE_CAMERA_CAPABILITY, body)
    }

    /**
     * Create a camera status packet
     *
     * Reports current streaming status to the desktop.
     *
     * ## Status Values
     * - `starting`: Camera is initializing
     * - `streaming`: Actively streaming frames
     * - `stopping`: Stopping stream
     * - `stopped`: Stream stopped
     * - `error`: Error occurred (includes error message)
     *
     * @param status Current streaming status
     * @param cameraId Active camera ID
     * @param width Current resolution width
     * @param height Current resolution height
     * @param fps Current frame rate
     * @param bitrate Current bitrate in kbps
     * @param error Error message (only for error status)
     * @return NetworkPacket ready to send
     */
    fun createStatusPacket(
        status: StreamingStatus,
        cameraId: Int,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        error: String? = null
    ): NetworkPacket {
        val body = mutableMapOf<String, Any>(
            "status" to status.value,
            "cameraId" to cameraId,
            "resolution" to mapOf("width" to width, "height" to height),
            "fps" to fps,
            "bitrate" to bitrate
        )

        if (error != null && status == StreamingStatus.ERROR) {
            body["error"] = error
        }

        return NetworkPacket.create(PACKET_TYPE_CAMERA_STATUS, body)
    }

    /**
     * Create a camera frame packet
     *
     * Creates a packet header for video frame data. The actual frame payload
     * is sent separately via the payload transfer mechanism.
     *
     * ## Frame Types
     * - **sps_pps**: Decoder configuration (must be sent first)
     * - **iframe**: Keyframe (independently decodable)
     * - **pframe**: Delta frame (depends on previous frames)
     *
     * @param frameType Type of frame (sps_pps, iframe, pframe)
     * @param timestampUs Presentation timestamp in microseconds
     * @param sequenceNumber Frame sequence number
     * @param payloadSize Size of frame data in bytes
     * @return NetworkPacket with payloadSize set
     */
    fun createFramePacket(
        frameType: FrameType,
        timestampUs: Long,
        sequenceNumber: Long,
        payloadSize: Long
    ): NetworkPacket {
        require(payloadSize > 0) { "Payload size must be positive" }

        val body = mapOf(
            "frameType" to frameType.value,
            "timestampUs" to timestampUs,
            "sequenceNumber" to sequenceNumber,
            "size" to payloadSize
        )

        // Create packet and set payloadSize using copy
        return NetworkPacket.create(PACKET_TYPE_CAMERA_FRAME, body)
            .copy(payloadSize = payloadSize)
    }
}

// ============================================================================
// Data Classes
// ============================================================================

/**
 * Camera facing direction
 */
enum class CameraFacing(val value: String) {
    FRONT("front"),
    BACK("back"),
    EXTERNAL("external");

    companion object {
        fun fromValue(value: String): CameraFacing = when (value) {
            "front" -> FRONT
            "back" -> BACK
            "external" -> EXTERNAL
            else -> BACK
        }
    }
}

/**
 * Streaming status
 */
enum class StreamingStatus(val value: String) {
    STARTING("starting"),
    STREAMING("streaming"),
    STOPPING("stopping"),
    STOPPED("stopped"),
    ERROR("error");

    companion object {
        fun fromValue(value: String): StreamingStatus = when (value) {
            "starting" -> STARTING
            "streaming" -> STREAMING
            "stopping" -> STOPPING
            "stopped" -> STOPPED
            "error" -> ERROR
            else -> STOPPED
        }
    }
}

/**
 * H.264 frame type
 */
enum class FrameType(val value: String) {
    SPS_PPS("sps_pps"),
    IFRAME("iframe"),
    PFRAME("pframe");

    companion object {
        fun fromValue(value: String): FrameType = when (value) {
            "sps_pps" -> SPS_PPS
            "iframe" -> IFRAME
            "pframe" -> PFRAME
            else -> PFRAME
        }
    }
}

/**
 * Resolution specification
 */
data class Resolution(
    val width: Int,
    val height: Int
) {
    companion object {
        val HD_720 = Resolution(1280, 720)
        val HD_1080 = Resolution(1920, 1080)
        val SD_480 = Resolution(854, 480)
    }
}

/**
 * Camera information for capability advertisement
 */
data class CameraInfo(
    val id: Int,
    val name: String,
    val facing: CameraFacing,
    val maxWidth: Int,
    val maxHeight: Int,
    val resolutions: List<Resolution>
)

/**
 * Camera start request parsed from packet
 */
data class CameraStartRequest(
    val cameraId: Int,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val codec: String = "h264"
)

/**
 * Camera settings request parsed from packet
 */
data class CameraSettingsRequest(
    val cameraId: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fps: Int? = null,
    val bitrate: Int? = null,
    val flash: Boolean? = null,
    val autofocus: Boolean? = null
)

// ============================================================================
// Extension Properties for Packet Type Inspection
// ============================================================================

/**
 * Check if packet is a camera capability advertisement
 */
val NetworkPacket.isCameraCapability: Boolean
    get() = type == CameraPacketsFFI.PACKET_TYPE_CAMERA_CAPABILITY

/**
 * Check if packet is a camera start request
 * Supports both cconnect.camera.start and cconnect.camera.request packet types
 */
val NetworkPacket.isCameraStart: Boolean
    get() = type == CameraPacketsFFI.PACKET_TYPE_CAMERA_START ||
            type == CameraPacketsFFI.PACKET_TYPE_CAMERA_REQUEST

/**
 * Check if packet is a camera stop request
 */
val NetworkPacket.isCameraStop: Boolean
    get() = type == CameraPacketsFFI.PACKET_TYPE_CAMERA_STOP

/**
 * Check if packet is a camera settings request
 */
val NetworkPacket.isCameraSettings: Boolean
    get() = type == CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS

/**
 * Check if packet is a camera frame
 */
val NetworkPacket.isCameraFrame: Boolean
    get() = type == CameraPacketsFFI.PACKET_TYPE_CAMERA_FRAME

/**
 * Check if packet is a camera status update
 */
val NetworkPacket.isCameraStatus: Boolean
    get() = type == CameraPacketsFFI.PACKET_TYPE_CAMERA_STATUS

// ============================================================================
// Extension Functions for Parsing Received Packets
// ============================================================================

/**
 * Parse camera start request from packet
 *
 * @return CameraStartRequest or null if packet is not a start request
 */
fun NetworkPacket.toCameraStartRequest(): CameraStartRequest? {
    if (!isCameraStart) return null

    try {
        val resolution = body["resolution"] as? Map<*, *> ?: return null
        return CameraStartRequest(
            cameraId = (body["cameraId"] as? Number)?.toInt() ?: 0,
            width = (resolution["width"] as? Number)?.toInt() ?: 1280,
            height = (resolution["height"] as? Number)?.toInt() ?: 720,
            fps = (body["fps"] as? Number)?.toInt() ?: 30,
            bitrate = (body["bitrate"] as? Number)?.toInt() ?: 2000,
            codec = body["codec"] as? String ?: "h264"
        )
    } catch (e: Exception) {
        return null
    }
}

/**
 * Parse camera settings request from packet
 *
 * @return CameraSettingsRequest or null if packet is not a settings request
 */
fun NetworkPacket.toCameraSettingsRequest(): CameraSettingsRequest? {
    if (!isCameraSettings) return null

    try {
        val resolution = body["resolution"] as? Map<*, *>
        return CameraSettingsRequest(
            cameraId = (body["cameraId"] as? Number)?.toInt(),
            width = (resolution?.get("width") as? Number)?.toInt(),
            height = (resolution?.get("height") as? Number)?.toInt(),
            fps = (body["fps"] as? Number)?.toInt(),
            bitrate = (body["bitrate"] as? Number)?.toInt(),
            flash = body["flash"] as? Boolean,
            autofocus = body["autofocus"] as? Boolean
        )
    } catch (e: Exception) {
        return null
    }
}
