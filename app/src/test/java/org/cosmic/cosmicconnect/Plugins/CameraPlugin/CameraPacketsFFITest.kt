/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.CameraPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for CameraPacketsFFI extension functions
 *
 * Tests the pure inspection/parsing logic for camera packets.
 * Does NOT test FFI packet creation methods.
 */
@RunWith(RobolectricTestRunner::class)
class CameraPacketsFFITest {

    // ========================================================================
    // Extension Property Tests - Packet Type Checks
    // ========================================================================

    @Test
    fun `isCameraCapability returns true for capability packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_CAPABILITY,
            body = emptyMap()
        )
        assertTrue(packet.isCameraCapability)
    }

    @Test
    fun `isCameraCapability returns false for other packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = emptyMap()
        )
        assertFalse(packet.isCameraCapability)
    }

    @Test
    fun `isCameraStart returns true for start packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            body = emptyMap()
        )
        assertTrue(packet.isCameraStart)
    }

    @Test
    fun `isCameraStart returns true for request packet (alternate name)`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_REQUEST,
            body = emptyMap()
        )
        assertTrue(packet.isCameraStart)
    }

    @Test
    fun `isCameraStart returns false for other packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = emptyMap()
        )
        assertFalse(packet.isCameraStart)
    }

    @Test
    fun `isCameraStop returns true for stop packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_STOP,
            body = emptyMap()
        )
        assertTrue(packet.isCameraStop)
    }

    @Test
    fun `isCameraStop returns false for other packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = emptyMap()
        )
        assertFalse(packet.isCameraStop)
    }

    @Test
    fun `isCameraSettings returns true for settings packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            body = emptyMap()
        )
        assertTrue(packet.isCameraSettings)
    }

    @Test
    fun `isCameraSettings returns false for other packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = emptyMap()
        )
        assertFalse(packet.isCameraSettings)
    }

    @Test
    fun `isCameraFrame returns true for frame packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_FRAME,
            body = emptyMap()
        )
        assertTrue(packet.isCameraFrame)
    }

    @Test
    fun `isCameraFrame returns false for other packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = emptyMap()
        )
        assertFalse(packet.isCameraFrame)
    }

    @Test
    fun `isCameraStatus returns true for status packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_STATUS,
            body = emptyMap()
        )
        assertTrue(packet.isCameraStatus)
    }

    @Test
    fun `isCameraStatus returns false for other packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = emptyMap()
        )
        assertFalse(packet.isCameraStatus)
    }

    // ========================================================================
    // toCameraStartRequest() Tests
    // ========================================================================

    @Test
    fun `toCameraStartRequest parses valid start packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            body = mapOf(
                "cameraId" to 1,
                "resolution" to mapOf("width" to 1920, "height" to 1080),
                "fps" to 30,
                "bitrate" to 4000,
                "codec" to "h264"
            )
        )

        val request = packet.toCameraStartRequest()

        assertNotNull(request)
        assertEquals(1, request!!.cameraId)
        assertEquals(1920, request.width)
        assertEquals(1080, request.height)
        assertEquals(30, request.fps)
        assertEquals(4000, request.bitrate)
        assertEquals("h264", request.codec)
    }

    @Test
    fun `toCameraStartRequest uses defaults for missing optional fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            body = mapOf(
                "resolution" to mapOf("width" to 640, "height" to 480)
            )
        )

        val request = packet.toCameraStartRequest()

        assertNotNull(request)
        assertEquals(0, request!!.cameraId) // Default
        assertEquals(640, request.width)
        assertEquals(480, request.height)
        assertEquals(30, request.fps) // Default
        assertEquals(2000, request.bitrate) // Default
        assertEquals("h264", request.codec) // Default
    }

    @Test
    fun `toCameraStartRequest returns null for wrong packet type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = mapOf(
                "resolution" to mapOf("width" to 1920, "height" to 1080)
            )
        )

        val request = packet.toCameraStartRequest()

        assertNull(request)
    }

    @Test
    fun `toCameraStartRequest returns null for missing resolution`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            body = mapOf("cameraId" to 1)
        )

        val request = packet.toCameraStartRequest()

        assertNull(request)
    }

    @Test
    fun `toCameraStartRequest handles Number types for numeric fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            body = mapOf(
                "cameraId" to 2L, // Long instead of Int
                "resolution" to mapOf("width" to 1280.0, "height" to 720.0), // Double
                "fps" to 60.0, // Double
                "bitrate" to 5000L // Long
            )
        )

        val request = packet.toCameraStartRequest()

        assertNotNull(request)
        assertEquals(2, request!!.cameraId)
        assertEquals(1280, request.width)
        assertEquals(720, request.height)
        assertEquals(60, request.fps)
        assertEquals(5000, request.bitrate)
    }

    @Test
    fun `toCameraStartRequest handles partial resolution map`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            body = mapOf(
                "resolution" to mapOf("width" to 1024)
                // Missing height
            )
        )

        val request = packet.toCameraStartRequest()

        assertNotNull(request)
        assertEquals(1024, request!!.width)
        assertEquals(720, request.height) // Default
    }

    // ========================================================================
    // toCameraSettingsRequest() Tests
    // ========================================================================

    @Test
    fun `toCameraSettingsRequest parses all fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            body = mapOf(
                "cameraId" to 1,
                "resolution" to mapOf("width" to 1920, "height" to 1080),
                "fps" to 60,
                "bitrate" to 8000,
                "flash" to true,
                "autofocus" to false
            )
        )

        val request = packet.toCameraSettingsRequest()

        assertNotNull(request)
        assertEquals(1, request!!.cameraId)
        assertEquals(1920, request.width)
        assertEquals(1080, request.height)
        assertEquals(60, request.fps)
        assertEquals(8000, request.bitrate)
        assertEquals(true, request.flash)
        assertEquals(false, request.autofocus)
    }

    @Test
    fun `toCameraSettingsRequest handles partial fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            body = mapOf(
                "cameraId" to 0,
                "fps" to 24
            )
        )

        val request = packet.toCameraSettingsRequest()

        assertNotNull(request)
        assertEquals(0, request!!.cameraId)
        assertNull(request.width)
        assertNull(request.height)
        assertEquals(24, request.fps)
        assertNull(request.bitrate)
        assertNull(request.flash)
        assertNull(request.autofocus)
    }

    @Test
    fun `toCameraSettingsRequest returns null for wrong packet type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.other",
            body = mapOf("cameraId" to 1)
        )

        val request = packet.toCameraSettingsRequest()

        assertNull(request)
    }

    @Test
    fun `toCameraSettingsRequest returns empty request for empty body`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            body = emptyMap()
        )

        val request = packet.toCameraSettingsRequest()

        assertNotNull(request)
        assertNull(request!!.cameraId)
        assertNull(request.width)
        assertNull(request.height)
        assertNull(request.fps)
        assertNull(request.bitrate)
        assertNull(request.flash)
        assertNull(request.autofocus)
    }

    @Test
    fun `toCameraSettingsRequest handles Number types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            body = mapOf(
                "cameraId" to 1L,
                "resolution" to mapOf("width" to 1280.0, "height" to 720.0),
                "fps" to 30.0,
                "bitrate" to 3000L
            )
        )

        val request = packet.toCameraSettingsRequest()

        assertNotNull(request)
        assertEquals(1, request!!.cameraId)
        assertEquals(1280, request.width)
        assertEquals(720, request.height)
        assertEquals(30, request.fps)
        assertEquals(3000, request.bitrate)
    }

    // ========================================================================
    // Enum Tests
    // ========================================================================

    @Test
    fun `CameraFacing fromValue parses all values`() {
        assertEquals(CameraFacing.FRONT, CameraFacing.fromValue("front"))
        assertEquals(CameraFacing.BACK, CameraFacing.fromValue("back"))
        assertEquals(CameraFacing.EXTERNAL, CameraFacing.fromValue("external"))
        assertEquals(CameraFacing.BACK, CameraFacing.fromValue("unknown"))
    }

    @Test
    fun `StreamingStatus fromValue parses all values`() {
        assertEquals(StreamingStatus.STARTING, StreamingStatus.fromValue("starting"))
        assertEquals(StreamingStatus.STREAMING, StreamingStatus.fromValue("streaming"))
        assertEquals(StreamingStatus.STOPPING, StreamingStatus.fromValue("stopping"))
        assertEquals(StreamingStatus.STOPPED, StreamingStatus.fromValue("stopped"))
        assertEquals(StreamingStatus.ERROR, StreamingStatus.fromValue("error"))
        assertEquals(StreamingStatus.STOPPED, StreamingStatus.fromValue("unknown"))
    }

    @Test
    fun `FrameType fromValue parses all values`() {
        assertEquals(FrameType.SPS_PPS, FrameType.fromValue("sps_pps"))
        assertEquals(FrameType.IFRAME, FrameType.fromValue("iframe"))
        assertEquals(FrameType.PFRAME, FrameType.fromValue("pframe"))
        assertEquals(FrameType.PFRAME, FrameType.fromValue("unknown"))
    }

    // ========================================================================
    // Data Class Tests
    // ========================================================================

    @Test
    fun `Resolution has correct predefined values`() {
        assertEquals(1280, Resolution.HD_720.width)
        assertEquals(720, Resolution.HD_720.height)
        assertEquals(1920, Resolution.HD_1080.width)
        assertEquals(1080, Resolution.HD_1080.height)
        assertEquals(854, Resolution.SD_480.width)
        assertEquals(480, Resolution.SD_480.height)
    }

    @Test
    fun `Resolution equality works`() {
        val res1 = Resolution(1920, 1080)
        val res2 = Resolution(1920, 1080)
        val res3 = Resolution(1280, 720)

        assertEquals(res1, res2)
        assertNotEquals(res1, res3)
    }

    @Test
    fun `CameraStartRequest has default codec`() {
        val request = CameraStartRequest(
            cameraId = 0,
            width = 1280,
            height = 720,
            fps = 30,
            bitrate = 2000
        )
        assertEquals("h264", request.codec)
    }

    @Test
    fun `CameraSettingsRequest all fields nullable`() {
        val request = CameraSettingsRequest()
        assertNull(request.cameraId)
        assertNull(request.width)
        assertNull(request.height)
        assertNull(request.fps)
        assertNull(request.bitrate)
        assertNull(request.flash)
        assertNull(request.autofocus)
    }
}
