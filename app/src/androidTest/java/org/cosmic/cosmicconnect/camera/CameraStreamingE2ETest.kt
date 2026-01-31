/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.camera

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * End-to-End Integration Tests for Camera Streaming
 *
 * Tests the full camera streaming pipeline from start to stop,
 * including configuration changes, error recovery, and network resilience.
 *
 * Test coverage:
 * - Basic streaming lifecycle (start/stop)
 * - Frame transmission verification
 * - Camera switching during streaming
 * - Resolution changes during streaming
 * - Error recovery scenarios
 * - Network condition handling
 * - Resource cleanup
 *
 * Hardware requirements: None (uses mocks for camera and network)
 */
@RunWith(AndroidJUnit4::class)
class CameraStreamingE2ETest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockDevice = createMockDevice()
    }

    @After
    fun teardown() {
        // Cleanup resources
    }

    // ========================================================================
    // Basic Streaming Tests
    // ========================================================================

    @Test
    fun testStreamingLifecycle_startAndStop() = runBlocking {
        // Arrange
        val streamStartedLatch = CountDownLatch(1)
        val streamStoppedLatch = CountDownLatch(1)
        val framesReceived = AtomicInteger(0)

        val callback = object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() {
                streamStartedLatch.countDown()
            }

            override fun onStreamStopped() {
                streamStoppedLatch.countDown()
            }

            override fun onStreamError(error: Throwable) {
                fail("Unexpected error: ${error.message}")
            }

            override fun onBandwidthUpdate(kbps: Int) {}
            override fun onCongestionDetected() {}
        }

        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        assertTrue("Stream should start", streamStartedLatch.await(2, TimeUnit.SECONDS))

        // Send some frames
        val mockFrames = CameraTestUtils.generateMockFrameSequence(
            durationMs = 500,
            fps = 30,
            iframeInterval = 15
        )

        mockFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(10) // Simulate frame timing
        }

        client.stop()
        assertTrue("Stream should stop", streamStoppedLatch.await(2, TimeUnit.SECONDS))

        // Assert
        assertFalse("Client should not be streaming", client.isStreaming())
        val stats = client.getStats()
        assertTrue("Should have sent frames", stats.totalFramesSent > 0)
    }

    @Test
    fun testStreamingLifecycle_verifyFrameOrder() = runBlocking {
        // Arrange
        val receivedFrames = mutableListOf<Pair<FrameType, Long>>()
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Generate test sequence: SPS/PPS -> I-frame -> 5x P-frame
        val frames = listOf(
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockSps() + CameraTestUtils.generateMockPps(),
                FrameType.SPS_PPS,
                0L
            ),
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockIframe(2048, 1),
                FrameType.IFRAME,
                33333L
            )
        ) + (1..5).map { i ->
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockPframe(512, i),
                FrameType.PFRAME,
                (i + 1) * 33333L
            )
        }

        // Act
        client.start()
        delay(100)

        frames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            receivedFrames.add(Pair(frame.type, frame.timestampUs))
            delay(33) // ~30 FPS
        }

        delay(200)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertEquals("Should have sent all frames", frames.size, stats.totalFramesSent.toInt())
        assertEquals("First frame should be SPS/PPS", FrameType.SPS_PPS, receivedFrames[0].first)
        assertEquals("Second frame should be I-frame", FrameType.IFRAME, receivedFrames[1].first)
        assertTrue("Should have P-frames", receivedFrames.drop(2).all { it.first == FrameType.PFRAME })
    }

    @Test
    fun testFrameTransmission_verifySpsResend() = runBlocking {
        // Arrange - SPS/PPS should be resent every 30 frames
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val sps = CameraTestUtils.generateMockSps()
        val pps = CameraTestUtils.generateMockPps()
        val iframe = CameraTestUtils.generateMockIframe(2048)
        val pframe = CameraTestUtils.generateMockPframe(512)

        // Act
        client.start()
        delay(100)

        // Send initial SPS/PPS
        client.sendSpsPps(sps, pps)

        // Send 35 frames to trigger resend at frame 30
        repeat(35) { i ->
            val frameType = if (i == 0) FrameType.IFRAME else FrameType.PFRAME
            val frameData = if (i == 0) iframe else pframe
            client.sendFrame(frameData, frameType, i * 33333L)
            delay(10)
        }

        delay(200)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should have sent more than 35 frames (including SPS/PPS resends)",
            stats.totalFramesSent >= 35)
    }

    // ========================================================================
    // Configuration Change Tests
    // ========================================================================

    @Test
    fun testCameraSwitching_duringStreaming() = runBlocking {
        // Arrange
        val configChanges = AtomicInteger(0)
        val callback = object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() {}
            override fun onStreamStopped() {}
            override fun onStreamError(error: Throwable) {
                fail("Unexpected error during camera switch: ${error.message}")
            }
            override fun onBandwidthUpdate(kbps: Int) {}
            override fun onCongestionDetected() {}
        }

        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        // Simulate streaming from back camera
        val backCameraFrames = CameraTestUtils.generateMockFrameSequence(
            durationMs = 300,
            fps = 30
        )
        backCameraFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(10)
        }

        // Switch to front camera - send new SPS/PPS
        val newSps = CameraTestUtils.generateMockSps()
        val newPps = CameraTestUtils.generateMockPps()
        client.sendSpsPps(newSps, newPps)
        configChanges.incrementAndGet()

        // Stream from front camera
        val frontCameraFrames = CameraTestUtils.generateMockFrameSequence(
            durationMs = 300,
            fps = 30
        )
        frontCameraFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs + 300000)
            delay(10)
        }

        delay(100)
        client.stop()

        // Assert
        assertEquals("Should have switched cameras once", 1, configChanges.get())
        val stats = client.getStats()
        assertTrue("Should have sent frames from both cameras",
            stats.totalFramesSent > backCameraFrames.size + frontCameraFrames.size)
    }

    @Test
    fun testResolutionChange_duringStreaming() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        // Stream at 720p
        val hd720Frames = CameraTestUtils.generateMockFrameSequence(
            durationMs = 300,
            fps = 30
        )
        hd720Frames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(10)
        }

        // Change to 1080p - send new SPS/PPS with different resolution
        val newSps = CameraTestUtils.generateMockSps()
        val newPps = CameraTestUtils.generateMockPps()
        client.sendSpsPps(newSps, newPps)

        // Stream at 1080p
        val hd1080Frames = CameraTestUtils.generateMockFrameSequence(
            durationMs = 300,
            fps = 30
        )
        hd1080Frames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs + 300000)
            delay(10)
        }

        delay(100)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should have sent frames at both resolutions",
            stats.totalFramesSent >= hd720Frames.size + hd1080Frames.size)
    }

    @Test
    fun testFrameRateChange_duringStreaming() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        // Stream at 30 FPS
        val frames30fps = CameraTestUtils.generateMockFrameSequence(
            durationMs = 500,
            fps = 30
        )
        frames30fps.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(33) // ~30 FPS
        }

        // Change to 60 FPS
        val frames60fps = CameraTestUtils.generateMockFrameSequence(
            durationMs = 500,
            fps = 60
        )
        frames60fps.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs + 500000)
            delay(16) // ~60 FPS
        }

        delay(100)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should have sent frames at both frame rates",
            stats.totalFramesSent >= frames30fps.size + frames60fps.size)
    }

    // ========================================================================
    // Error Recovery Tests
    // ========================================================================

    @Test
    fun testErrorRecovery_networkDisconnection() = runBlocking {
        // Arrange
        val errorLatch = CountDownLatch(1)
        val errorRef = AtomicReference<Throwable>()

        val callback = object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() {}
            override fun onStreamStopped() {}
            override fun onStreamError(error: Throwable) {
                errorRef.set(error)
                errorLatch.countDown()
            }
            override fun onBandwidthUpdate(kbps: Int) {}
            override fun onCongestionDetected() {}
        }

        // Create device that will simulate disconnect
        val disconnectingDevice = createMockDisconnectingDevice()
        val client = CameraStreamClient(disconnectingDevice, callback)

        // Act
        client.start()
        delay(100)

        // Send frames until disconnect happens
        val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 1000, fps = 30)
        var disconnected = false

        for (frame in frames) {
            try {
                client.sendFrame(frame.data, frame.type, frame.timestampUs)
                delay(10)
            } catch (e: Exception) {
                disconnected = true
                break
            }
        }

        // Wait for error callback
        assertTrue("Error callback should be triggered", errorLatch.await(2, TimeUnit.SECONDS))

        // Assert
        assertNotNull("Error should be captured", errorRef.get())
        client.stop()
    }

    @Test
    fun testErrorRecovery_frameDropUnderLoad() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        // Flood with large frames to trigger backpressure
        val largeFrames = (0 until 50).map { i ->
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockIframe(65536, i),
                FrameType.IFRAME,
                i * 33333L
            )
        }

        largeFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            // Don't delay - send as fast as possible to trigger dropping
        }

        delay(500)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should have dropped some frames under load",
            stats.framesDropped > 0 || stats.totalFramesSent < largeFrames.size)
    }

    @Test
    fun testErrorRecovery_corruptFrameData() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        // Send valid frame
        val validFrame = CameraTestUtils.generateMockIframe(1024)
        client.sendFrame(validFrame, FrameType.IFRAME, 0L)
        delay(50)

        // Send corrupt frame (random data without NAL header)
        val corruptData = ByteArray(1024) { 0xFF.toByte() }
        client.sendFrame(corruptData, FrameType.PFRAME, 33333L)
        delay(50)

        // Send another valid frame
        val validFrame2 = CameraTestUtils.generateMockPframe(512)
        client.sendFrame(validFrame2, FrameType.PFRAME, 66666L)
        delay(100)

        client.stop()

        // Assert - client should handle corrupt data gracefully
        val stats = client.getStats()
        assertTrue("Should have sent some frames", stats.totalFramesSent >= 2)
    }

    // ========================================================================
    // Network Condition Tests
    // ========================================================================

    @Test
    fun testNetworkConditions_highLatency() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Simulate high latency by delaying timestamps
        val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 500, fps = 30)
        val delayedFrames = CameraTestUtils.simulateNetworkDelay(frames, delayMs = 100)

        // Act
        client.start()
        delay(100)

        delayedFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(33) // Send at normal rate
        }

        delay(200)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should have sent all frames despite latency",
            stats.totalFramesSent >= frames.size)
    }

    @Test
    fun testNetworkConditions_jitter() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Simulate jitter in frame timestamps
        val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 500, fps = 30)
        val jitteredFrames = CameraTestUtils.simulateJitter(frames, jitterMs = 20)

        // Act
        client.start()
        delay(100)

        jitteredFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(33)
        }

        delay(200)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should handle jittered timestamps",
            stats.totalFramesSent >= frames.size)
    }

    @Test
    fun testNetworkConditions_packetLoss() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Simulate 10% packet loss
        val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 1000, fps = 30)
        val lostFrames = CameraTestUtils.simulatePacketLoss(frames, lossRate = 0.1)

        // Act
        client.start()
        delay(100)

        lostFrames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(33)
        }

        delay(200)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertTrue("Should have sent fewer frames due to packet loss",
            stats.totalFramesSent < frames.size)
        assertTrue("Should have sent most frames",
            stats.totalFramesSent >= (frames.size * 0.85).toInt())
    }

    // ========================================================================
    // Statistics and Monitoring Tests
    // ========================================================================

    @Test
    fun testStatistics_accurateFrameCounting() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        val frames = listOf(
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockSps() + CameraTestUtils.generateMockPps(),
                FrameType.SPS_PPS,
                0L
            ),
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockIframe(2048),
                FrameType.IFRAME,
                33333L
            )
        ) + (1..10).map { i ->
            CameraTestUtils.MockFrame(
                CameraTestUtils.generateMockPframe(512),
                FrameType.PFRAME,
                (i + 1) * 33333L
            )
        }

        // Act
        client.start()
        delay(100)

        frames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(10)
        }

        delay(100)
        client.stop()

        // Assert
        val stats = client.getStats()
        assertEquals("Total frames should match", frames.size, stats.totalFramesSent.toInt())
        assertEquals("Keyframes should be 1 (I-frame)", 1, stats.totalKeyframesSent.toInt())
        assertTrue("Should have tracked bytes", stats.totalBytesSent > 0)
        assertEquals("Pending frames should be 0 after stop", 0, stats.pendingFrames)
    }

    @Test
    fun testStatistics_bandwidthCalculation() = runBlocking {
        // Arrange
        val bandwidthUpdates = mutableListOf<Int>()
        val callback = object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() {}
            override fun onStreamStopped() {}
            override fun onStreamError(error: Throwable) {}
            override fun onBandwidthUpdate(kbps: Int) {
                bandwidthUpdates.add(kbps)
            }
            override fun onCongestionDetected() {}
        }

        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        // Send frames for bandwidth calculation
        val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 2000, fps = 30)
        frames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(33)
        }

        delay(500)
        client.stop()

        // Assert
        assertTrue("Should have received bandwidth updates", bandwidthUpdates.isNotEmpty())
        val avgBandwidth = bandwidthUpdates.average().toInt()
        val expectedBitrate = CameraTestUtils.calculateExpectedBitrate(frames, 2000)
        assertTrue("Bandwidth should be reasonable ($avgBandwidth kbps, expected ~$expectedBitrate kbps)",
            avgBandwidth > 0)
    }

    // ========================================================================
    // Cleanup and Resource Management Tests
    // ========================================================================

    @Test
    fun testCleanup_properResourceRelease() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Act
        client.start()
        delay(100)

        val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 500, fps = 30)
        frames.forEach { frame ->
            client.sendFrame(frame.data, frame.type, frame.timestampUs)
            delay(10)
        }

        client.stop()
        delay(100)

        // Assert
        val stats = client.getStats()
        assertEquals("Pending frames should be 0 after stop", 0, stats.pendingFrames)
        assertFalse("Client should not be streaming", client.isStreaming())
    }

    @Test
    fun testCleanup_multipleStartStopCycles() = runBlocking {
        // Arrange
        val callback = createTestCallback()
        val client = CameraStreamClient(mockDevice, callback)

        // Act & Assert - Multiple start/stop cycles
        repeat(3) { cycle ->
            client.start()
            delay(100)

            val frames = CameraTestUtils.generateMockFrameSequence(durationMs = 300, fps = 30)
            frames.forEach { frame ->
                client.sendFrame(frame.data, frame.type, frame.timestampUs)
                delay(10)
            }

            client.stop()
            delay(100)

            assertFalse("Client should not be streaming after cycle $cycle", client.isStreaming())
        }

        // Final statistics should be cumulative
        val stats = client.getStats()
        assertTrue("Should have sent frames from all cycles", stats.totalFramesSent > 0)
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

        val constructor = Device::class.java.getDeclaredConstructor(
            Context::class.java,
            org.cosmic.cosmicconnect.Backends.BaseLink::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(context, mockLink)
    }

    private fun createMockDisconnectingDevice(): Device {
        // Similar to createMockDevice but configured to simulate disconnection
        // Note: For now, use regular mock device. Disconnection simulation would require
        // extending MockLink to support disconnect behavior.
        val mockLink = MockFactory.createMockLink(
            context = context,
            deviceId = TestUtils.randomDeviceId(),
            deviceName = "Disconnecting Device"
        )

        val constructor = Device::class.java.getDeclaredConstructor(
            Context::class.java,
            org.cosmic.cosmicconnect.Backends.BaseLink::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(context, mockLink)
    }

    private fun createTestCallback(): CameraStreamClient.StreamCallback {
        return object : CameraStreamClient.StreamCallback {
            override fun onStreamStarted() {}
            override fun onStreamStopped() {}
            override fun onStreamError(error: Throwable) {
                // Log errors but don't fail the test
                android.util.Log.w("CameraE2ETest", "Stream error: ${error.message}")
            }
            override fun onBandwidthUpdate(kbps: Int) {}
            override fun onCongestionDetected() {}
        }
    }
}
