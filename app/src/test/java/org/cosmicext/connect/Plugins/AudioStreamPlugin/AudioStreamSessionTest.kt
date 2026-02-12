/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.AudioStreamPlugin

import android.media.AudioTrack
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [AudioStreamSession] state machine and lifecycle.
 *
 * Uses a test subclass that overrides [createAudioTrack] to return a mock
 * AudioTrack, avoiding AudioTrack.Builder failures under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class AudioStreamSessionTest {

    private val mockAudioTrack: AudioTrack = mockk(relaxed = true)

    /** Test subclass that injects a mock AudioTrack */
    private inner class TestableSession(
        sampleRate: Int = 48000,
        channels: Int = 2,
        codec: String = "pcm",
    ) : AudioStreamSession(sampleRate, channels, codec) {
        override fun createAudioTrack(channelConfig: Int, encoding: Int, bufferSize: Int): AudioTrack {
            return mockAudioTrack
        }
    }

    @Test
    fun `initial state is IDLE`() {
        val session = TestableSession()

        assertEquals(AudioStreamSession.State.IDLE, session.getState())
    }

    @Test
    fun `prepare with valid params succeeds`() {
        val session = TestableSession(sampleRate = 44100, channels = 1, codec = "pcm")

        val result = session.prepare()

        assertTrue(result)
    }

    @Test
    fun `prepare with stereo succeeds`() {
        val session = TestableSession(sampleRate = 48000, channels = 2, codec = "opus")

        val result = session.prepare()

        assertTrue(result)
    }

    @Test
    fun `prepare with unsupported channel count returns false`() {
        val session = TestableSession(sampleRate = 48000, channels = 5, codec = "pcm")
        var errorMessage: String? = null
        session.listener = object : AudioStreamSession.SessionListener {
            override fun onStateChanged(state: AudioStreamSession.State) {}
            override fun onError(message: String) { errorMessage = message }
        }

        val result = session.prepare()

        assertFalse(result)
        assertEquals(AudioStreamSession.State.ERROR, session.getState())
        assertTrue(errorMessage!!.contains("Unsupported channel count"))
    }

    @Test
    fun `play transitions to PLAYING state`() {
        val session = TestableSession()
        session.prepare()

        session.play()

        assertEquals(AudioStreamSession.State.PLAYING, session.getState())
        verify { mockAudioTrack.play() }
    }

    @Test
    fun `pause transitions to PAUSED state`() {
        val session = TestableSession()
        session.prepare()
        session.play()

        session.pause()

        assertEquals(AudioStreamSession.State.PAUSED, session.getState())
        verify { mockAudioTrack.pause() }
    }

    @Test
    fun `resume from PAUSED transitions to PLAYING`() {
        val session = TestableSession()
        session.prepare()
        session.play()
        session.pause()

        session.resume()

        assertEquals(AudioStreamSession.State.PLAYING, session.getState())
        // play() called twice: once for initial play, once for resume
        verify(exactly = 2) { mockAudioTrack.play() }
    }

    @Test
    fun `resume from DUCKED transitions to PLAYING`() {
        val session = TestableSession()
        session.prepare()
        session.play()
        session.duck()

        session.resume()

        assertEquals(AudioStreamSession.State.PLAYING, session.getState())
    }

    @Test
    fun `resume from IDLE does nothing`() {
        val session = TestableSession()
        session.prepare()

        session.resume() // Not paused or ducked, should do nothing

        // State should not change to PLAYING without explicit play()
        assertEquals(AudioStreamSession.State.IDLE, session.getState())
    }

    @Test
    fun `duck sets DUCKED state and reduces volume`() {
        val session = TestableSession()
        session.prepare()
        session.play()

        session.duck()

        assertEquals(AudioStreamSession.State.DUCKED, session.getState())
        verify { mockAudioTrack.setVolume(0.2f) }
    }

    @Test
    fun `unduck restores volume and state to PLAYING`() {
        val session = TestableSession()
        session.prepare()
        session.play()
        session.duck()

        session.unduck()

        assertEquals(AudioStreamSession.State.PLAYING, session.getState())
        verify { mockAudioTrack.setVolume(1.0f) }
    }

    @Test
    fun `stop releases AudioTrack and sets STOPPED state`() {
        val session = TestableSession()
        session.prepare()
        session.play()

        session.stop()

        assertEquals(AudioStreamSession.State.STOPPED, session.getState())
        verify { mockAudioTrack.stop() }
        verify { mockAudioTrack.release() }
    }

    @Test
    fun `writeAudioData returns -1 when stopped`() {
        val session = TestableSession()
        session.prepare()
        session.play()
        session.stop()

        val result = session.writeAudioData(ByteArray(1024))

        assertEquals(-1, result)
    }

    @Test
    fun `writeAudioData delegates to AudioTrack`() {
        val data = ByteArray(512)
        every { mockAudioTrack.write(data, 0, 512) } returns 512
        val session = TestableSession()
        session.prepare()
        session.play()

        val result = session.writeAudioData(data)

        assertEquals(512, result)
        verify { mockAudioTrack.write(data, 0, 512) }
    }

    @Test
    fun `writeAudioData returns -1 in ERROR state`() {
        val session = TestableSession(sampleRate = 48000, channels = 5, codec = "pcm")
        session.prepare() // fails, sets ERROR state

        val result = session.writeAudioData(ByteArray(1024))

        assertEquals(-1, result)
    }

    @Test
    fun `setVolume clamps to 0-1 range`() {
        val session = TestableSession()
        session.prepare()

        session.setVolume(1.5f)
        verify { mockAudioTrack.setVolume(1.0f) }

        session.setVolume(-0.5f)
        verify { mockAudioTrack.setVolume(0.0f) }

        session.setVolume(0.7f)
        verify { mockAudioTrack.setVolume(0.7f) }
    }

    @Test
    fun `listener receives state changes`() {
        val states = mutableListOf<AudioStreamSession.State>()
        val session = TestableSession()
        session.listener = object : AudioStreamSession.SessionListener {
            override fun onStateChanged(state: AudioStreamSession.State) { states.add(state) }
            override fun onError(message: String) {}
        }
        session.prepare()

        session.play()
        session.pause()
        session.resume()
        session.stop()

        assertEquals(
            listOf(
                AudioStreamSession.State.PLAYING,
                AudioStreamSession.State.PAUSED,
                AudioStreamSession.State.PLAYING,
                AudioStreamSession.State.STOPPED,
            ),
            states,
        )
    }

    @Test
    fun `play after stop is ignored`() {
        val session = TestableSession()
        session.prepare()
        session.play()
        session.stop()

        session.play() // Should be ignored since stopped flag is set

        assertEquals(AudioStreamSession.State.STOPPED, session.getState())
    }
}
