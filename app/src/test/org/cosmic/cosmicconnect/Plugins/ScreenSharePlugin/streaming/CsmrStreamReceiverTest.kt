/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming

import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CsmrStreamReceiverTest {

    /**
     * Helper to build a CSMR binary frame.
     * Wire format: 4B magic + 1B type + 8B timestamp + 4B size + payload
     */
    private fun buildCsmrFrame(type: Byte, timestampNs: Long, payload: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(CsmrFrame.HEADER_SIZE + payload.size)
        buffer.put(CsmrFrame.MAGIC.toByteArray(Charsets.US_ASCII)) // 4 bytes
        buffer.put(type) // 1 byte
        buffer.putLong(timestampNs) // 8 bytes big-endian
        buffer.putInt(payload.size) // 4 bytes big-endian
        buffer.put(payload)
        return buffer.array()
    }

    @Test
    fun testReadValidFrame() {
        val payload = byteArrayOf(0x11, 0x22, 0x33, 0x44)
        val frameBytes = buildCsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload)
        val stream = ByteArrayInputStream(frameBytes)
        val receiver = CsmrStreamReceiver(stream)

        val frame = receiver.readFrame()

        assertEquals(CsmrFrame.TYPE_VIDEO, frame?.type)
        assertEquals(123456789L, frame?.timestampNs)
        assertEquals(payload.toList(), frame?.payload?.toList())
    }

    @Test
    fun testReadFrameReturnsNullOnEmptyStream() {
        val stream = ByteArrayInputStream(ByteArray(0))
        val receiver = CsmrStreamReceiver(stream)

        val frame = receiver.readFrame()

        assertNull(frame)
    }

    @Test
    fun testReadFrameThrowsOnInvalidMagic() {
        val buffer = ByteBuffer.allocate(17)
        buffer.put("XXXX".toByteArray(Charsets.US_ASCII)) // Invalid magic
        buffer.put(CsmrFrame.TYPE_VIDEO)
        buffer.putLong(123456789L)
        buffer.putInt(0)

        val stream = ByteArrayInputStream(buffer.array())
        val receiver = CsmrStreamReceiver(stream)

        val exception = assertFailsWith<CsmrProtocolException> {
            receiver.readFrame()
        }
        assertEquals("Invalid CSMR magic: expected 'CSMR', got 'XXXX'", exception.message)
    }

    @Test
    fun testReadFrameThrowsOnOversizedPayload() {
        val buffer = ByteBuffer.allocate(17)
        buffer.put(CsmrFrame.MAGIC.toByteArray(Charsets.US_ASCII))
        buffer.put(CsmrFrame.TYPE_VIDEO)
        buffer.putLong(123456789L)
        buffer.putInt(CsmrFrame.MAX_PAYLOAD_SIZE + 1) // Oversized

        val stream = ByteArrayInputStream(buffer.array())
        val receiver = CsmrStreamReceiver(stream)

        val exception = assertFailsWith<CsmrProtocolException> {
            receiver.readFrame()
        }
        assertEquals(
            "Invalid payload size: ${CsmrFrame.MAX_PAYLOAD_SIZE + 1} (max ${CsmrFrame.MAX_PAYLOAD_SIZE})",
            exception.message
        )
    }

    @Test
    fun testReadFrameThrowsOnNegativePayloadSize() {
        val buffer = ByteBuffer.allocate(17)
        buffer.put(CsmrFrame.MAGIC.toByteArray(Charsets.US_ASCII))
        buffer.put(CsmrFrame.TYPE_VIDEO)
        buffer.putLong(123456789L)
        buffer.putInt(-1) // Negative size (0xFFFFFFFF)

        val stream = ByteArrayInputStream(buffer.array())
        val receiver = CsmrStreamReceiver(stream)

        val exception = assertFailsWith<CsmrProtocolException> {
            receiver.readFrame()
        }
        assertEquals(
            "Invalid payload size: -1 (max ${CsmrFrame.MAX_PAYLOAD_SIZE})",
            exception.message
        )
    }

    @Test
    fun testReadMultipleFrames() {
        val payload1 = byteArrayOf(0x01, 0x02)
        val payload2 = byteArrayOf(0x03, 0x04, 0x05)

        val frame1Bytes = buildCsmrFrame(CsmrFrame.TYPE_VIDEO, 100L, payload1)
        val frame2Bytes = buildCsmrFrame(CsmrFrame.TYPE_CURSOR, 200L, payload2)

        val combinedBytes = frame1Bytes + frame2Bytes
        val stream = ByteArrayInputStream(combinedBytes)
        val receiver = CsmrStreamReceiver(stream)

        val frame1 = receiver.readFrame()
        assertEquals(CsmrFrame.TYPE_VIDEO, frame1?.type)
        assertEquals(100L, frame1?.timestampNs)
        assertEquals(payload1.toList(), frame1?.payload?.toList())

        val frame2 = receiver.readFrame()
        assertEquals(CsmrFrame.TYPE_CURSOR, frame2?.type)
        assertEquals(200L, frame2?.timestampNs)
        assertEquals(payload2.toList(), frame2?.payload?.toList())

        val frame3 = receiver.readFrame()
        assertNull(frame3) // EOF
    }

    @Test
    fun testReadZeroLengthPayload() {
        val emptyPayload = ByteArray(0)
        val frameBytes = buildCsmrFrame(CsmrFrame.TYPE_ANNOTATION, 999L, emptyPayload)
        val stream = ByteArrayInputStream(frameBytes)
        val receiver = CsmrStreamReceiver(stream)

        val frame = receiver.readFrame()

        assertEquals(CsmrFrame.TYPE_ANNOTATION, frame?.type)
        assertEquals(999L, frame?.timestampNs)
        assertEquals(0, frame?.payload?.size)
    }

    @Test
    fun testReadAllFrameTypes() {
        val payload = byteArrayOf(0x99)

        val videoFrame = buildCsmrFrame(CsmrFrame.TYPE_VIDEO, 1L, payload)
        val cursorFrame = buildCsmrFrame(CsmrFrame.TYPE_CURSOR, 2L, payload)
        val annotationFrame = buildCsmrFrame(CsmrFrame.TYPE_ANNOTATION, 3L, payload)
        val eosFrame = buildCsmrFrame(CsmrFrame.TYPE_END_OF_STREAM, 4L, payload)

        val combinedBytes = videoFrame + cursorFrame + annotationFrame + eosFrame
        val stream = ByteArrayInputStream(combinedBytes)
        val receiver = CsmrStreamReceiver(stream)

        assertEquals(CsmrFrame.TYPE_VIDEO, receiver.readFrame()?.type)
        assertEquals(CsmrFrame.TYPE_CURSOR, receiver.readFrame()?.type)
        assertEquals(CsmrFrame.TYPE_ANNOTATION, receiver.readFrame()?.type)
        assertEquals(CsmrFrame.TYPE_END_OF_STREAM, receiver.readFrame()?.type)
        assertNull(receiver.readFrame()) // EOF
    }

    @Test
    fun testReadFrameWithLargeTimestamp() {
        val payload = byteArrayOf(0xAA, 0xBB)
        val largeTimestamp = Long.MAX_VALUE
        val frameBytes = buildCsmrFrame(CsmrFrame.TYPE_VIDEO, largeTimestamp, payload)
        val stream = ByteArrayInputStream(frameBytes)
        val receiver = CsmrStreamReceiver(stream)

        val frame = receiver.readFrame()

        assertEquals(largeTimestamp, frame?.timestampNs)
    }

    @Test
    fun testReadFrameWithMaxValidPayloadSize() {
        val maxPayload = ByteArray(CsmrFrame.MAX_PAYLOAD_SIZE)
        val frameBytes = buildCsmrFrame(CsmrFrame.TYPE_VIDEO, 1000L, maxPayload)
        val stream = ByteArrayInputStream(frameBytes)
        val receiver = CsmrStreamReceiver(stream)

        val frame = receiver.readFrame()

        assertEquals(CsmrFrame.MAX_PAYLOAD_SIZE, frame?.payload?.size)
    }
}
