/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.ScreenSharePlugin.streaming

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StreamSessionTest {

    @Test
    fun testInitialStateIsIdle() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        assertEquals(StreamState.Idle, session.state.value)
    }

    @Test
    fun testTcpPortIsZeroBeforePrepare() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        assertEquals(0, session.tcpPort)
    }

    @Test
    fun testPrepareTransitionsToWaitingForConnection() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.prepare()

        val state = session.state.value
        assertTrue(state is StreamState.WaitingForConnection)
        assertTrue((state as StreamState.WaitingForConnection).tcpPort > 0)
    }

    @Test
    fun testTcpPortIsValidAfterPrepare() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.prepare()

        assertTrue(session.tcpPort > 0)
        assertTrue(session.tcpPort < 65536)
    }

    @Test
    fun testPrepareTwiceThrowsException() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.prepare()

        val exception = assertFailsWith<IllegalStateException> {
            session.prepare()
        }
        assertEquals("Session already prepared", exception.message)
    }

    @Test
    fun testStopTransitionsToStoppedState() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.prepare()
        session.stop()

        val state = session.state.value
        assertTrue(state is StreamState.Stopped)
        assertEquals("Stopped by user", (state as StreamState.Stopped).reason)
    }

    @Test
    fun testStopIsIdempotent() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.prepare()
        session.stop()
        session.stop() // Should not throw or change state

        val state = session.state.value
        assertTrue(state is StreamState.Stopped)
        assertEquals("Stopped by user", (state as StreamState.Stopped).reason)
    }

    @Test
    fun testStopBeforePrepare() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.stop()

        val state = session.state.value
        assertTrue(state is StreamState.Stopped)
    }

    @Test
    fun testSessionProperties() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        assertEquals(1920, session.width)
        assertEquals(1080, session.height)
        assertEquals(30, session.fps)
        assertEquals("h264", session.codec)
    }

    @Test
    fun testTcpPortIsSameAsStatePort() {
        val session = StreamSession(
            width = 1920,
            height = 1080,
            fps = 30,
            codec = "h264"
        )

        session.prepare()

        val state = session.state.value as StreamState.WaitingForConnection
        assertEquals(state.tcpPort, session.tcpPort)
    }
}
