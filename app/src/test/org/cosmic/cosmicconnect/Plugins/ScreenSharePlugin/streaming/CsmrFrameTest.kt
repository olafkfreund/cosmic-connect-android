/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CsmrFrameTest {

    @Test
    fun testConstants() {
        assertEquals("CSMR", CsmrFrame.MAGIC)
        assertEquals(17, CsmrFrame.HEADER_SIZE)
        assertEquals(0x01.toByte(), CsmrFrame.TYPE_VIDEO)
        assertEquals(0x02.toByte(), CsmrFrame.TYPE_CURSOR)
        assertEquals(0x03.toByte(), CsmrFrame.TYPE_ANNOTATION)
        assertEquals(0xFF.toByte(), CsmrFrame.TYPE_END_OF_STREAM)
        assertEquals(-1, CsmrFrame.TYPE_END_OF_STREAM) // Verify it's -1 as signed byte
        assertEquals(2 * 1024 * 1024, CsmrFrame.MAX_PAYLOAD_SIZE)
    }

    @Test
    fun testEqualsWithSamePayload() {
        val payload1 = byteArrayOf(1, 2, 3, 4)
        val payload2 = byteArrayOf(1, 2, 3, 4)

        val frame1 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload1)
        val frame2 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload2)

        assertEquals(frame1, frame2)
        assertEquals(frame1.hashCode(), frame2.hashCode())
    }

    @Test
    fun testEqualsWithDifferentPayload() {
        val payload1 = byteArrayOf(1, 2, 3, 4)
        val payload2 = byteArrayOf(1, 2, 3, 5)

        val frame1 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload1)
        val frame2 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload2)

        assertNotEquals(frame1, frame2)
    }

    @Test
    fun testEqualsWithDifferentType() {
        val payload = byteArrayOf(1, 2, 3, 4)

        val frame1 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload)
        val frame2 = CsmrFrame(CsmrFrame.TYPE_CURSOR, 123456789L, payload)

        assertNotEquals(frame1, frame2)
    }

    @Test
    fun testEqualsWithDifferentTimestamp() {
        val payload = byteArrayOf(1, 2, 3, 4)

        val frame1 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload)
        val frame2 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 987654321L, payload)

        assertNotEquals(frame1, frame2)
    }

    @Test
    fun testEqualsSameInstance() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val frame = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload)

        assertTrue(frame.equals(frame))
    }

    @Test
    fun testEqualsNull() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val frame = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload)

        assertFalse(frame.equals(null))
    }

    @Test
    fun testEqualsDifferentClass() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val frame = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123456789L, payload)

        assertFalse(frame.equals("not a frame"))
    }

    @Test
    fun testEmptyPayload() {
        val frame1 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123L, ByteArray(0))
        val frame2 = CsmrFrame(CsmrFrame.TYPE_VIDEO, 123L, ByteArray(0))

        assertEquals(frame1, frame2)
    }

    @Test
    fun testAllFrameTypes() {
        val payload = byteArrayOf(1, 2, 3)

        val videoFrame = CsmrFrame(CsmrFrame.TYPE_VIDEO, 100L, payload)
        val cursorFrame = CsmrFrame(CsmrFrame.TYPE_CURSOR, 200L, payload)
        val annotationFrame = CsmrFrame(CsmrFrame.TYPE_ANNOTATION, 300L, payload)
        val eosFrame = CsmrFrame(CsmrFrame.TYPE_END_OF_STREAM, 400L, payload)

        assertEquals(CsmrFrame.TYPE_VIDEO, videoFrame.type)
        assertEquals(CsmrFrame.TYPE_CURSOR, cursorFrame.type)
        assertEquals(CsmrFrame.TYPE_ANNOTATION, annotationFrame.type)
        assertEquals(CsmrFrame.TYPE_END_OF_STREAM, eosFrame.type)
    }
}
