/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.WebcamPlugin

import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebcamPacketsFFITest {

    // ========================================================================
    // Status packet creation
    // ========================================================================

    @Test
    fun `createWebcamStatus with streaming true includes all fields`() {
        val status = WebcamStatus(
            isStreaming = true,
            cameraId = "0",
            width = 1920,
            height = 1080,
            codec = "h264",
            fps = 30,
        )
        val tp = createWebcamStatus(status)
        assertEquals(PACKET_TYPE_WEBCAM, tp.packet.type)
        assertEquals(true, tp.packet.body["isStreaming"])
        assertEquals("0", tp.packet.body["cameraId"])
        assertEquals(1920, tp.packet.body["width"])
        assertEquals(1080, tp.packet.body["height"])
        assertEquals("h264", tp.packet.body["codec"])
        assertEquals(30, tp.packet.body["fps"])
    }

    @Test
    fun `createWebcamStatus with streaming false has minimal fields`() {
        val status = WebcamStatus(isStreaming = false)
        val tp = createWebcamStatus(status)
        assertEquals(PACKET_TYPE_WEBCAM, tp.packet.type)
        assertEquals(false, tp.packet.body["isStreaming"])
        assertNull(tp.packet.body["cameraId"])
        assertNull(tp.packet.body["width"])
        assertNull(tp.packet.body["height"])
    }

    @Test
    fun `createWebcamStatus with partial optional fields`() {
        val status = WebcamStatus(isStreaming = true, cameraId = "1", width = 1280, height = 720)
        val tp = createWebcamStatus(status)
        assertEquals("1", tp.packet.body["cameraId"])
        assertEquals(1280, tp.packet.body["width"])
        assertNull(tp.packet.body["codec"])
        assertNull(tp.packet.body["fps"])
    }

    // ========================================================================
    // Capability response creation
    // ========================================================================

    @Test
    fun `createWebcamCapabilityResponse with multiple cameras`() {
        val caps = listOf(
            WebcamCapability("0", "Back Camera", 1920, 1080, listOf("h264")),
            WebcamCapability("1", "Front Camera", 1280, 720, listOf("h264", "vp8")),
        )
        val tp = createWebcamCapabilityResponse(caps)
        assertEquals(PACKET_TYPE_WEBCAM_CAPABILITY, tp.packet.type)
        @Suppress("UNCHECKED_CAST")
        val cameras = tp.packet.body["cameras"] as List<Map<String, Any>>
        assertEquals(2, cameras.size)
        assertEquals("0", cameras[0]["cameraId"])
        assertEquals("Back Camera", cameras[0]["name"])
        assertEquals(1920, cameras[0]["maxWidth"])
        assertEquals(1080, cameras[0]["maxHeight"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("h264"), cameras[0]["supportedCodecs"] as List<String>)
    }

    @Test
    fun `createWebcamCapabilityResponse with empty list`() {
        val tp = createWebcamCapabilityResponse(emptyList())
        @Suppress("UNCHECKED_CAST")
        val cameras = tp.packet.body["cameras"] as List<Map<String, Any>>
        assertTrue(cameras.isEmpty())
    }

    @Test
    fun `createWebcamCapabilityResponse second camera fields`() {
        val caps = listOf(
            WebcamCapability("1", "Front Camera", 1280, 720, listOf("h264", "vp8")),
        )
        val tp = createWebcamCapabilityResponse(caps)
        @Suppress("UNCHECKED_CAST")
        val cameras = tp.packet.body["cameras"] as List<Map<String, Any>>
        assertEquals("1", cameras[0]["cameraId"])
        assertEquals(1280, cameras[0]["maxWidth"])
        assertEquals(720, cameras[0]["maxHeight"])
        @Suppress("UNCHECKED_CAST")
        assertEquals(listOf("h264", "vp8"), cameras[0]["supportedCodecs"] as List<String>)
    }

    // ========================================================================
    // Start request inspection
    // ========================================================================

    @Test
    fun `isWebcamStartRequest returns true for start request`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("start" to true, "cameraId" to "0", "width" to 1920, "height" to 1080),
        ))
        assertTrue(tp.isWebcamStartRequest)
        assertFalse(tp.isWebcamStopRequest)
    }

    @Test
    fun `isWebcamStartRequest returns false for stop request`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("stop" to true),
        ))
        assertFalse(tp.isWebcamStartRequest)
    }

    @Test
    fun `start request fields are extractable`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf(
                "start" to true,
                "cameraId" to "2",
                "width" to 640,
                "height" to 480,
                "codec" to "h264",
                "fps" to 15,
            ),
        ))
        assertEquals("2", tp.webcamRequestCameraId)
        assertEquals(640, tp.webcamRequestWidth)
        assertEquals(480, tp.webcamRequestHeight)
        assertEquals("h264", tp.webcamRequestCodec)
        assertEquals(15, tp.webcamRequestFps)
    }

    @Test
    fun `start request with missing optional fields returns null`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("start" to true),
        ))
        assertNull(tp.webcamRequestCameraId)
        assertNull(tp.webcamRequestWidth)
        assertNull(tp.webcamRequestHeight)
        assertNull(tp.webcamRequestCodec)
        assertNull(tp.webcamRequestFps)
    }

    // ========================================================================
    // Stop request inspection
    // ========================================================================

    @Test
    fun `isWebcamStopRequest returns true for stop request`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("stop" to true),
        ))
        assertTrue(tp.isWebcamStopRequest)
        assertFalse(tp.isWebcamStartRequest)
    }

    @Test
    fun `isWebcamStopRequest returns false for wrong type`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("stop" to true),
        ))
        assertFalse(tp.isWebcamStopRequest)
    }

    // ========================================================================
    // Capability request inspection
    // ========================================================================

    @Test
    fun `isWebcamCapabilityRequest returns true`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_CAPABILITY,
            body = mapOf("requestCapabilities" to true),
        ))
        assertTrue(tp.isWebcamCapabilityRequest)
    }

    @Test
    fun `isWebcamCapabilityRequest returns false without flag`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_CAPABILITY,
            body = mapOf("cameras" to emptyList<Any>()),
        ))
        assertFalse(tp.isWebcamCapabilityRequest)
    }
}
