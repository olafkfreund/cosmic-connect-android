/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.WebcamPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket

// ============================================================================
// Packet Type Constants
// ============================================================================

/** Webcam status/response packet type (phone → desktop) */
const val PACKET_TYPE_WEBCAM = "cconnect.webcam"

/** Webcam start/stop request packet type (desktop → phone) */
const val PACKET_TYPE_WEBCAM_REQUEST = "cconnect.webcam.request"

/** Webcam capability packet type (bidirectional) */
const val PACKET_TYPE_WEBCAM_CAPABILITY = "cconnect.webcam.capability"

// ============================================================================
// Data Classes
// ============================================================================

data class WebcamCapability(
    val cameraId: String,
    val name: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val supportedCodecs: List<String>,
)

data class WebcamStatus(
    val isStreaming: Boolean,
    val cameraId: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val codec: String? = null,
    val fps: Int? = null,
)

// ============================================================================
// Packet Creation (Phone → Desktop)
// ============================================================================

fun createWebcamStatus(status: WebcamStatus): TransferPacket {
    val body = mutableMapOf<String, Any>(
        "isStreaming" to status.isStreaming,
    )
    status.cameraId?.let { body["cameraId"] = it }
    status.width?.let { body["width"] = it }
    status.height?.let { body["height"] = it }
    status.codec?.let { body["codec"] = it }
    status.fps?.let { body["fps"] = it }

    return TransferPacket(
        NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_WEBCAM,
            body = body,
        )
    )
}

fun createWebcamCapabilityResponse(capabilities: List<WebcamCapability>): TransferPacket {
    val camerasJson = capabilities.map { cap ->
        mapOf(
            "cameraId" to cap.cameraId,
            "name" to cap.name,
            "maxWidth" to cap.maxWidth,
            "maxHeight" to cap.maxHeight,
            "supportedCodecs" to cap.supportedCodecs,
        )
    }
    return TransferPacket(
        NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_WEBCAM_CAPABILITY,
            body = mapOf("cameras" to camerasJson),
        )
    )
}

// ============================================================================
// TransferPacket Inspection Extensions (Desktop → Phone)
// ============================================================================

val TransferPacket.isWebcamStartRequest: Boolean
    get() = packet.type == PACKET_TYPE_WEBCAM_REQUEST &&
            packet.body["start"] == true

val TransferPacket.isWebcamStopRequest: Boolean
    get() = packet.type == PACKET_TYPE_WEBCAM_REQUEST &&
            packet.body["stop"] == true

val TransferPacket.isWebcamCapabilityRequest: Boolean
    get() = packet.type == PACKET_TYPE_WEBCAM_CAPABILITY &&
            packet.body["requestCapabilities"] == true

val TransferPacket.webcamRequestCameraId: String?
    get() = packet.body["cameraId"] as? String

val TransferPacket.webcamRequestWidth: Int?
    get() = (packet.body["width"] as? Number)?.toInt()

val TransferPacket.webcamRequestHeight: Int?
    get() = (packet.body["height"] as? Number)?.toInt()

val TransferPacket.webcamRequestCodec: String?
    get() = packet.body["codec"] as? String

val TransferPacket.webcamRequestFps: Int?
    get() = (packet.body["fps"] as? Number)?.toInt()
