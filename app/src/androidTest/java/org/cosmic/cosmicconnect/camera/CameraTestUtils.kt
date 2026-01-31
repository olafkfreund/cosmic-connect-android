/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.camera

import org.cosmic.cosmicconnect.Plugins.CameraPlugin.*
import java.nio.ByteBuffer
import java.security.MessageDigest
import kotlin.random.Random

/**
 * Test utilities for camera plugin testing
 *
 * Provides utilities for generating mock H.264 frames, SPS/PPS data,
 * and simulating various camera scenarios for integration testing.
 */
object CameraTestUtils {

    // ========================================================================
    // Mock NAL Unit Generation
    // ========================================================================

    /**
     * Generate a mock SPS (Sequence Parameter Set) NAL unit
     *
     * Creates a realistic-looking SPS NAL unit for 720p H.264 video.
     * The data is not decodable but has the correct structure.
     */
    fun generateMockSps(): ByteArray {
        return byteArrayOf(
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), // NAL unit start code
            0x67.toByte(), // NAL unit type: SPS (7)
            0x42.toByte(), 0xC0.toByte(), 0x1E.toByte(), // Profile, constraints, level
            0xFF.toByte(), 0xE1.toByte(), // More SPS data
            0x00.toByte(), 0x18.toByte(), // SPS size
            // Mock SPS payload
            0x67.toByte(), 0x42.toByte(), 0xC0.toByte(), 0x1E.toByte(),
            0x8C.toByte(), 0x68.toByte(), 0x02.toByte(), 0x80.toByte(),
            0x2D.toByte(), 0xD8.toByte(), 0x0F.toByte(), 0x00.toByte(),
            0x44.toByte(), 0xFC.toByte(), 0xB8.toByte(), 0x08.toByte(),
            0x84.toByte(), 0x00.toByte(), 0x00.toByte(), 0x03.toByte(),
            0x00.toByte(), 0x04.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x03.toByte(), 0x00.toByte(), 0xF0.toByte(), 0x3C.toByte()
        )
    }

    /**
     * Generate a mock PPS (Picture Parameter Set) NAL unit
     *
     * Creates a realistic-looking PPS NAL unit for H.264 video.
     */
    fun generateMockPps(): ByteArray {
        return byteArrayOf(
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), // NAL unit start code
            0x68.toByte(), // NAL unit type: PPS (8)
            0xCE.toByte(), 0x3C.toByte(), 0x80.toByte() // PPS payload
        )
    }

    /**
     * Generate a mock I-frame NAL unit
     *
     * @param size Size of the frame payload (excluding NAL header)
     * @param seed Random seed for reproducible data
     * @return Byte array representing a mock I-frame
     */
    fun generateMockIframe(size: Int = 2048, seed: Int = 42): ByteArray {
        require(size > 0) { "Frame size must be positive" }

        val buffer = ByteBuffer.allocate(size + 5)

        // NAL unit start code
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x01)

        // NAL unit type: IDR slice (5)
        buffer.put(0x65)

        // Fill with pseudo-random data based on seed
        val random = Random(seed)
        val payload = ByteArray(size)
        random.nextBytes(payload)
        buffer.put(payload)

        return buffer.array()
    }

    /**
     * Generate a mock P-frame NAL unit
     *
     * @param size Size of the frame payload (excluding NAL header)
     * @param seed Random seed for reproducible data
     * @return Byte array representing a mock P-frame
     */
    fun generateMockPframe(size: Int = 512, seed: Int = 43): ByteArray {
        require(size > 0) { "Frame size must be positive" }

        val buffer = ByteBuffer.allocate(size + 5)

        // NAL unit start code
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x00)
        buffer.put(0x01)

        // NAL unit type: Non-IDR slice (1)
        buffer.put(0x41)

        // Fill with pseudo-random data based on seed
        val random = Random(seed)
        val payload = ByteArray(size)
        random.nextBytes(payload)
        buffer.put(payload)

        return buffer.array()
    }

    // ========================================================================
    // Mock Camera Info Generation
    // ========================================================================

    /**
     * Generate a mock back camera info
     */
    fun mockBackCamera(): CameraInfo {
        return CameraInfo(
            id = 0,
            name = "Mock Back Camera",
            facing = CameraFacing.BACK,
            maxWidth = 1920,
            maxHeight = 1080,
            resolutions = listOf(
                Resolution(1920, 1080),
                Resolution(1280, 720),
                Resolution(854, 480)
            )
        )
    }

    /**
     * Generate a mock front camera info
     */
    fun mockFrontCamera(): CameraInfo {
        return CameraInfo(
            id = 1,
            name = "Mock Front Camera",
            facing = CameraFacing.FRONT,
            maxWidth = 1280,
            maxHeight = 720,
            resolutions = listOf(
                Resolution(1280, 720),
                Resolution(640, 480)
            )
        )
    }

    /**
     * Generate a list of mock cameras (back + front)
     */
    fun mockCameraList(): List<CameraInfo> {
        return listOf(mockBackCamera(), mockFrontCamera())
    }

    // ========================================================================
    // Mock Streaming Scenarios
    // ========================================================================

    /**
     * Generate a sequence of mock frames simulating a video stream
     *
     * @param durationMs Duration of the stream in milliseconds
     * @param fps Frames per second
     * @param iframeInterval Number of P-frames between I-frames
     * @return List of frame data with timestamps
     */
    fun generateMockFrameSequence(
        durationMs: Long = 1000,
        fps: Int = 30,
        iframeInterval: Int = 30
    ): List<MockFrame> {
        val frames = mutableListOf<MockFrame>()
        val frameIntervalUs = (1_000_000 / fps).toLong()
        val totalFrames = (durationMs * fps / 1000).toInt()

        // Add SPS/PPS at start
        frames.add(MockFrame(
            data = generateMockSps() + generateMockPps(),
            type = FrameType.SPS_PPS,
            timestampUs = 0L
        ))

        // Generate I-frames and P-frames
        for (i in 0 until totalFrames) {
            val isIframe = (i % iframeInterval) == 0
            val frameType = if (isIframe) FrameType.IFRAME else FrameType.PFRAME
            val frameSize = if (isIframe) 2048 else 512

            frames.add(MockFrame(
                data = if (isIframe) {
                    generateMockIframe(frameSize, seed = i)
                } else {
                    generateMockPframe(frameSize, seed = i)
                },
                type = frameType,
                timestampUs = i * frameIntervalUs
            ))
        }

        return frames
    }

    /**
     * Data class representing a mock frame
     */
    data class MockFrame(
        val data: ByteArray,
        val type: FrameType,
        val timestampUs: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MockFrame

            if (!data.contentEquals(other.data)) return false
            if (type != other.type) return false
            if (timestampUs != other.timestampUs) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + timestampUs.hashCode()
            return result
        }
    }

    // ========================================================================
    // Validation Utilities
    // ========================================================================

    /**
     * Verify that frame data has valid NAL unit structure
     */
    fun isValidNalUnit(data: ByteArray): Boolean {
        if (data.size < 5) return false

        // Check for NAL unit start code (0x00 0x00 0x00 0x01)
        return data[0] == 0x00.toByte() &&
               data[1] == 0x00.toByte() &&
               data[2] == 0x00.toByte() &&
               data[3] == 0x01.toByte()
    }

    /**
     * Extract NAL unit type from frame data
     */
    fun getNalUnitType(data: ByteArray): Int? {
        if (!isValidNalUnit(data)) return null
        return (data[4].toInt() and 0x1F)
    }

    /**
     * Calculate checksum of frame data for comparison
     */
    fun calculateFrameChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }

    // ========================================================================
    // Stream Statistics Helpers
    // ========================================================================

    /**
     * Calculate expected bitrate from frame sequence
     */
    fun calculateExpectedBitrate(frames: List<MockFrame>, durationMs: Long): Int {
        val totalBytes = frames.sumOf { it.data.size }
        val totalBits = totalBytes * 8
        val durationSec = durationMs / 1000.0
        return (totalBits / durationSec / 1000).toInt() // kbps
    }

    /**
     * Count frames by type
     */
    fun countFramesByType(frames: List<MockFrame>): Map<FrameType, Int> {
        return frames.groupingBy { it.type }.eachCount()
    }

    // ========================================================================
    // Network Condition Simulation
    // ========================================================================

    /**
     * Simulate packet loss by randomly dropping frames
     *
     * @param frames Original frame sequence
     * @param lossRate Packet loss rate (0.0 to 1.0)
     * @param seed Random seed for reproducible results
     * @return Frame sequence with some frames dropped
     */
    fun simulatePacketLoss(
        frames: List<MockFrame>,
        lossRate: Double = 0.1,
        seed: Int = 100
    ): List<MockFrame> {
        require(lossRate in 0.0..1.0) { "Loss rate must be between 0.0 and 1.0" }

        val random = Random(seed)
        return frames.filter { random.nextDouble() > lossRate }
    }

    /**
     * Simulate network delay by adjusting timestamps
     *
     * @param frames Original frame sequence
     * @param delayMs Fixed delay in milliseconds
     * @return Frame sequence with adjusted timestamps
     */
    fun simulateNetworkDelay(
        frames: List<MockFrame>,
        delayMs: Long = 50
    ): List<MockFrame> {
        val delayUs = delayMs * 1000
        return frames.map { it.copy(timestampUs = it.timestampUs + delayUs) }
    }

    /**
     * Simulate jitter by adding random variations to timestamps
     *
     * @param frames Original frame sequence
     * @param jitterMs Maximum jitter in milliseconds
     * @param seed Random seed for reproducible results
     * @return Frame sequence with jittered timestamps
     */
    fun simulateJitter(
        frames: List<MockFrame>,
        jitterMs: Long = 10,
        seed: Int = 101
    ): List<MockFrame> {
        val random = Random(seed)
        val jitterUs = jitterMs * 1000

        return frames.map { frame ->
            val jitter = random.nextLong(-jitterUs, jitterUs)
            frame.copy(timestampUs = maxOf(0, frame.timestampUs + jitter))
        }
    }
}
