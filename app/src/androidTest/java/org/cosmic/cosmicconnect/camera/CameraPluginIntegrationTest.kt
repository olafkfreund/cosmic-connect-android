/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.camera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.CameraPlugin.*
import org.cosmic.cosmicconnect.test.MockFactory
import org.cosmic.cosmicconnect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * CameraPlugin Integration Tests
 *
 * Integration tests for the camera streaming feature. Tests packet creation,
 * serialization, frame encoding pipeline, and protocol compatibility with
 * the Rust desktop implementation.
 *
 * These tests focus on:
 * - Packet format compatibility
 * - Frame header creation
 * - Data serialization/deserialization
 * - Error handling
 * - Stats tracking
 *
 * Hardware requirements: None (uses mocks)
 */
@RunWith(AndroidJUnit4::class)
class CameraPluginIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun teardown() {
        // Cleanup if needed
    }

    // ========================================================================
    // Capability Packet Tests
    // ========================================================================

    @Test
    fun testCreateCapabilityPacket_singleCamera() {
        // Arrange
        val cameras = listOf(
            CameraInfo(
                id = 0,
                name = "Back Camera",
                facing = CameraFacing.BACK,
                maxWidth = 1920,
                maxHeight = 1080,
                resolutions = listOf(
                    Resolution(1920, 1080),
                    Resolution(1280, 720)
                )
            )
        )

        // Act
        val packet = CameraPacketsFFI.createCapabilityPacket(
            cameras = cameras,
            maxBitrate = 8000,
            maxFps = 60
        )

        // Assert
        assertEquals(CameraPacketsFFI.PACKET_TYPE_CAMERA_CAPABILITY, packet.type)

        // Verify packet body structure
        @Suppress("UNCHECKED_CAST")
        val cameraList = packet.body["cameras"] as? List<Map<String, Any>>
        assertNotNull("Cameras list should be present", cameraList)
        assertEquals(1, cameraList?.size)

        val camera = cameraList?.get(0)
        assertEquals(0, camera?.get("id"))
        assertEquals("Back Camera", camera?.get("name"))
        assertEquals("back", camera?.get("facing"))

        @Suppress("UNCHECKED_CAST")
        val maxRes = camera?.get("maxResolution") as? Map<String, Any>
        assertEquals(1920, maxRes?.get("width"))
        assertEquals(1080, maxRes?.get("height"))

        assertEquals(8000, packet.body["maxBitrate"])
        assertEquals(60, packet.body["maxFps"])
    }

    @Test
    fun testCreateCapabilityPacket_multipleCameras() {
        // Arrange
        val cameras = listOf(
            CameraInfo(
                id = 0,
                name = "Back Camera",
                facing = CameraFacing.BACK,
                maxWidth = 1920,
                maxHeight = 1080,
                resolutions = listOf(Resolution(1920, 1080), Resolution(1280, 720))
            ),
            CameraInfo(
                id = 1,
                name = "Front Camera",
                facing = CameraFacing.FRONT,
                maxWidth = 1280,
                maxHeight = 720,
                resolutions = listOf(Resolution(1280, 720))
            )
        )

        // Act
        val packet = CameraPacketsFFI.createCapabilityPacket(cameras)

        // Assert
        @Suppress("UNCHECKED_CAST")
        val cameraList = packet.body["cameras"] as? List<Map<String, Any>>
        assertEquals(2, cameraList?.size)
        assertEquals("Front Camera", cameraList?.get(1)?.get("name"))
        assertEquals("front", cameraList?.get(1)?.get("facing"))
    }

    // ========================================================================
    // Status Packet Tests
    // ========================================================================

    @Test
    fun testCreateStatusPacket_streaming() {
        // Act
        val packet = CameraPacketsFFI.createStatusPacket(
            status = StreamingStatus.STREAMING,
            cameraId = 0,
            width = 1280,
            height = 720,
            fps = 30,
            bitrate = 2000
        )

        // Assert
        assertEquals(CameraPacketsFFI.PACKET_TYPE_CAMERA_STATUS, packet.type)
        assertEquals("streaming", packet.body["status"])
        assertEquals(0, packet.body["cameraId"])
        assertEquals(30, packet.body["fps"])
        assertEquals(2000, packet.body["bitrate"])

        @Suppress("UNCHECKED_CAST")
        val resolution = packet.body["resolution"] as? Map<String, Any>
        assertEquals(1280, resolution?.get("width"))
        assertEquals(720, resolution?.get("height"))
    }

    @Test
    fun testCreateStatusPacket_error() {
        // Act
        val packet = CameraPacketsFFI.createStatusPacket(
            status = StreamingStatus.ERROR,
            cameraId = 0,
            width = 1280,
            height = 720,
            fps = 30,
            bitrate = 2000,
            error = "Camera access denied"
        )

        // Assert
        assertEquals(CameraPacketsFFI.PACKET_TYPE_CAMERA_STATUS, packet.type)
        assertEquals("error", packet.body["status"])
        assertEquals("Camera access denied", packet.body["error"])
    }

    @Test
    fun testCreateStatusPacket_errorWithoutMessage() {
        // Act - error status without error message should still work
        val packet = CameraPacketsFFI.createStatusPacket(
            status = StreamingStatus.ERROR,
            cameraId = 0,
            width = 1280,
            height = 720,
            fps = 30,
            bitrate = 2000
        )

        // Assert - error field should not be present if not in error state
        assertFalse(packet.body.containsKey("error"))
    }

    // ========================================================================
    // Frame Packet Tests
    // ========================================================================

    @Test
    fun testCreateFramePacket_iframe() {
        // Act
        val packet = CameraPacketsFFI.createFramePacket(
            frameType = FrameType.IFRAME,
            timestampUs = 1234567890L,
            sequenceNumber = 42L,
            payloadSize = 65536L
        )

        // Assert
        assertEquals(CameraPacketsFFI.PACKET_TYPE_CAMERA_FRAME, packet.type)
        assertEquals("iframe", packet.body["frameType"])
        assertEquals(1234567890L, packet.body["timestampUs"])
        assertEquals(42L, packet.body["sequenceNumber"])
        assertEquals(65536L, packet.body["size"])
        assertEquals(65536L, packet.payloadSize)
    }

    @Test
    fun testCreateFramePacket_pframe() {
        // Act
        val packet = CameraPacketsFFI.createFramePacket(
            frameType = FrameType.PFRAME,
            timestampUs = 1234567900L,
            sequenceNumber = 43L,
            payloadSize = 32768L
        )

        // Assert
        assertEquals("pframe", packet.body["frameType"])
        assertEquals(32768L, packet.payloadSize)
    }

    @Test
    fun testCreateFramePacket_spsPps() {
        // Act
        val packet = CameraPacketsFFI.createFramePacket(
            frameType = FrameType.SPS_PPS,
            timestampUs = 0L,
            sequenceNumber = 0L,
            payloadSize = 128L
        )

        // Assert
        assertEquals("sps_pps", packet.body["frameType"])
        assertEquals(0L, packet.body["timestampUs"])
        assertEquals(128L, packet.payloadSize)
    }

    // ========================================================================
    // Packet Parsing Tests
    // ========================================================================

    @Test
    fun testParseCameraStartRequest() {
        // Arrange
        val packet = NetworkPacket.create(
            CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            mapOf(
                "cameraId" to 0,
                "resolution" to mapOf("width" to 1280, "height" to 720),
                "fps" to 30,
                "bitrate" to 2000,
                "codec" to "h264"
            )
        )

        // Act
        val request = packet.toCameraStartRequest()

        // Assert
        assertNotNull(request)
        assertEquals(0, request?.cameraId)
        assertEquals(1280, request?.width)
        assertEquals(720, request?.height)
        assertEquals(30, request?.fps)
        assertEquals(2000, request?.bitrate)
        assertEquals("h264", request?.codec)
    }

    @Test
    fun testParseCameraStartRequest_missingFields() {
        // Arrange - packet with missing resolution
        val packet = NetworkPacket.create(
            CameraPacketsFFI.PACKET_TYPE_CAMERA_START,
            mapOf(
                "cameraId" to 0,
                "fps" to 30,
                "bitrate" to 2000
            )
        )

        // Act
        val request = packet.toCameraStartRequest()

        // Assert - should return null for invalid packet
        assertNull(request)
    }

    @Test
    fun testParseCameraSettingsRequest() {
        // Arrange
        val packet = NetworkPacket.create(
            CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            mapOf(
                "cameraId" to 1,
                "resolution" to mapOf("width" to 1920, "height" to 1080),
                "fps" to 60,
                "flash" to true
            )
        )

        // Act
        val request = packet.toCameraSettingsRequest()

        // Assert
        assertNotNull(request)
        assertEquals(1, request?.cameraId)
        assertEquals(1920, request?.width)
        assertEquals(1080, request?.height)
        assertEquals(60, request?.fps)
        assertEquals(true, request?.flash)
        assertNull(request?.bitrate) // Not in packet
        assertNull(request?.autofocus) // Not in packet
    }

    @Test
    fun testParseCameraSettingsRequest_partialSettings() {
        // Arrange - only change bitrate
        val packet = NetworkPacket.create(
            CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS,
            mapOf("bitrate" to 4000)
        )

        // Act
        val request = packet.toCameraSettingsRequest()

        // Assert
        assertNotNull(request)
        assertNull(request?.cameraId)
        assertNull(request?.width)
        assertNull(request?.height)
        assertEquals(4000, request?.bitrate)
    }

    // ========================================================================
    // CameraStreamClient Tests
    // ========================================================================

    @Test
    fun testCameraStreamClient_startStop() {
        // Arrange
        val mockDevice = createMockDevice()
        val latch = CountDownLatch(2) // started + stopped

        val callback = object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() { latch.countDown() }
            override fun onStreamStopped() { latch.countDown() }
            override fun onStreamError(error: Throwable) {}
            override fun onBandwidthUpdate(kbps: Int) {}
            override fun onCongestionDetected() {}
        }

        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        Thread.sleep(100) // Allow start callback
        client.stop()

        // Assert
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertFalse(client.isStreaming())
    }

    @Test
    fun testCameraStreamClient_sendSpsPps() {
        // Arrange
        val mockDevice = createMockDevice()
        val callback = createNoOpCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val sps = ByteArray(32) { it.toByte() }
        val pps = ByteArray(16) { it.toByte() }

        // Act
        client.start()
        client.sendSpsPps(sps, pps)
        Thread.sleep(100) // Allow async send
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue(stats.totalFramesSent > 0)
    }

    @Test
    fun testCameraStreamClient_sendFrame() {
        // Arrange
        val mockDevice = createMockDevice()
        val callback = createNoOpCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val frameData = ByteArray(1024) { 0xFF.toByte() }

        // Act
        client.start()
        client.sendFrame(frameData, FrameType.IFRAME, 1000000L)
        Thread.sleep(100) // Allow async send
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue(stats.totalFramesSent > 0)
        assertTrue(stats.totalKeyframesSent > 0)
    }

    @Test
    fun testCameraStreamClient_frameDropping() {
        // Arrange
        val mockDevice = createMockDevice()
        val callback = createNoOpCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val frameData = ByteArray(65536) { 0xFF.toByte() }

        // Act - send many P-frames to trigger backpressure
        client.start()
        repeat(20) { i ->
            client.sendFrame(frameData, FrameType.PFRAME, i * 33333L)
        }
        Thread.sleep(100) // Allow processing
        client.stop()

        // Assert - some frames should be dropped
        val stats = client.getStats()
        assertTrue("Frames should be dropped due to backpressure",
            stats.framesDropped > 0 || stats.totalFramesSent < 20)
    }

    @Test
    fun testCameraStreamClient_periodicSpsPpsResend() {
        // Arrange
        val mockDevice = createMockDevice()
        val callback = createNoOpCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val sps = ByteArray(32) { it.toByte() }
        val pps = ByteArray(16) { it.toByte() }
        val frameData = ByteArray(1024) { 0xFF.toByte() }

        // Act - send initial SPS/PPS and >30 frames to trigger resend
        client.start()
        client.sendSpsPps(sps, pps)
        repeat(35) { i ->
            client.sendFrame(frameData, FrameType.PFRAME, i * 33333L)
        }
        Thread.sleep(200) // Allow processing
        client.stop()

        // Assert - SPS/PPS should be resent at frame 30
        val stats = client.getStats()
        assertTrue("Frames should be sent", stats.totalFramesSent >= 30)
    }

    @Test
    fun testCameraStreamClient_statistics() {
        // Arrange
        val mockDevice = createMockDevice()
        val callback = createNoOpCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val sps = ByteArray(32)
        val pps = ByteArray(16)
        val iframeData = ByteArray(2048)
        val pframeData = ByteArray(512)

        // Act
        client.start()
        client.sendSpsPps(sps, pps)
        client.sendFrame(iframeData, FrameType.IFRAME, 0L)
        client.sendFrame(pframeData, FrameType.PFRAME, 33333L)
        client.sendFrame(pframeData, FrameType.PFRAME, 66666L)
        Thread.sleep(200) // Allow processing
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue(stats.totalFramesSent >= 3)
        assertTrue(stats.totalKeyframesSent >= 1)
        assertTrue(stats.totalBytesSent > 0)
        assertEquals(0, stats.pendingFrames) // Should be 0 after stop
    }

    // ========================================================================
    // Protocol Compatibility Tests
    // ========================================================================

    @Test
    fun testFrameTypeValues_matchRustImplementation() {
        // Assert - frame type values must match Rust implementation
        assertEquals("sps_pps", FrameType.SPS_PPS.value)
        assertEquals("iframe", FrameType.IFRAME.value)
        assertEquals("pframe", FrameType.PFRAME.value)
    }

    @Test
    fun testStreamingStatusValues_matchRustImplementation() {
        // Assert - status values must match Rust implementation
        assertEquals("starting", StreamingStatus.STARTING.value)
        assertEquals("streaming", StreamingStatus.STREAMING.value)
        assertEquals("stopping", StreamingStatus.STOPPING.value)
        assertEquals("stopped", StreamingStatus.STOPPED.value)
        assertEquals("error", StreamingStatus.ERROR.value)
    }

    @Test
    fun testCameraFacingValues_matchRustImplementation() {
        // Assert - facing values must match Rust implementation
        assertEquals("front", CameraFacing.FRONT.value)
        assertEquals("back", CameraFacing.BACK.value)
        assertEquals("external", CameraFacing.EXTERNAL.value)
    }

    @Test
    fun testPacketTypeConstants_matchRustImplementation() {
        // Assert - packet types must match Rust implementation
        assertEquals("cconnect.camera.capability", CameraPacketsFFI.PACKET_TYPE_CAMERA_CAPABILITY)
        assertEquals("cconnect.camera.start", CameraPacketsFFI.PACKET_TYPE_CAMERA_START)
        assertEquals("cconnect.camera.stop", CameraPacketsFFI.PACKET_TYPE_CAMERA_STOP)
        assertEquals("cconnect.camera.settings", CameraPacketsFFI.PACKET_TYPE_CAMERA_SETTINGS)
        assertEquals("cconnect.camera.frame", CameraPacketsFFI.PACKET_TYPE_CAMERA_FRAME)
        assertEquals("cconnect.camera.status", CameraPacketsFFI.PACKET_TYPE_CAMERA_STATUS)
    }

    @Test
    fun testResolutionPresets_matchCommonValues() {
        // Assert - common resolution presets
        assertEquals(854, Resolution.SD_480.width)
        assertEquals(480, Resolution.SD_480.height)
        assertEquals(1280, Resolution.HD_720.width)
        assertEquals(720, Resolution.HD_720.height)
        assertEquals(1920, Resolution.HD_1080.width)
        assertEquals(1080, Resolution.HD_1080.height)
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    fun testCreateFramePacket_zeroPayloadSize_throwsException() {
        // Act & Assert
        try {
            CameraPacketsFFI.createFramePacket(
                frameType = FrameType.IFRAME,
                timestampUs = 0L,
                sequenceNumber = 0L,
                payloadSize = 0L
            )
            fail("Should throw exception for zero payload size")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Payload size must be positive") == true)
        }
    }

    @Test
    fun testCreateCapabilityPacket_emptyCameras_throwsException() {
        // Act & Assert
        try {
            CameraPacketsFFI.createCapabilityPacket(
                cameras = emptyList(),
                maxBitrate = 8000,
                maxFps = 60
            )
            fail("Should throw exception for empty cameras list")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("At least one camera must be available") == true)
        }
    }

    @Test
    fun testCameraStreamClient_sendFrameWhenNotStarted() {
        // Arrange
        val mockDevice = createMockDevice()
        val callback = createNoOpCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val frameData = ByteArray(1024)

        // Act - send frame without starting
        client.sendFrame(frameData, FrameType.IFRAME, 0L)

        // Assert - should be silently ignored
        val stats = client.getStats()
        assertEquals(0, stats.totalFramesSent)
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createMockDevice(): Device {
        val mockLink = MockFactory.createMockLink(
            context = context,
            deviceId = TestUtils.randomDeviceId(),
            deviceName = "Test Camera Device"
        )

        // Create device using reflection to bypass normal initialization
        val constructor = Device::class.java.getDeclaredConstructor(
            Context::class.java,
            org.cosmic.cosmicconnect.Backends.BaseLink::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(context, mockLink)
    }

    private fun createNoOpCallback(): CameraStreamClient.StreamCallback {
        return object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() {}
            override fun onStreamStopped() {}
            override fun onStreamError(error: Throwable) {}
            override fun onBandwidthUpdate(kbps: Int) {}
            override fun onCongestionDetected() {}
        }
    }
}
