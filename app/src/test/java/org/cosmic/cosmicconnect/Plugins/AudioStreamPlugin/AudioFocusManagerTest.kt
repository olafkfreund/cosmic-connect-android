/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.AudioStreamPlugin

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioFocusManagerTest {

    private lateinit var context: Context
    private lateinit var focusManager: AudioFocusManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        focusManager = AudioFocusManager(context)
    }

    @Test
    fun `requestFocus returns true when focus granted`() {
        // Robolectric's ShadowAudioManager grants focus by default
        val result = focusManager.requestFocus()

        assertTrue(result)
        assertTrue(focusManager.hasFocus())
    }

    @Test
    fun `abandonFocus clears focus state`() {
        focusManager.requestFocus()
        assertTrue(focusManager.hasFocus())

        focusManager.abandonFocus()

        assertFalse(focusManager.hasFocus())
    }

    @Test
    fun `hasFocus returns false initially`() {
        assertFalse(focusManager.hasFocus())
    }

    @Test
    fun `listener receives onFocusGained callback`() {
        var gainedCalled = false
        focusManager.listener = object : AudioFocusManager.FocusListener {
            override fun onFocusGained() { gainedCalled = true }
            override fun onFocusLost(transient: Boolean) {}
            override fun onDuck() {}
        }

        focusManager.requestFocus()
        // Simulate focus change via shadow
        focusManager.focusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertTrue(gainedCalled)
    }

    @Test
    fun `listener receives onFocusLost permanent callback`() {
        var lostTransient: Boolean? = null
        focusManager.listener = object : AudioFocusManager.FocusListener {
            override fun onFocusGained() {}
            override fun onFocusLost(transient: Boolean) { lostTransient = transient }
            override fun onDuck() {}
        }

        focusManager.requestFocus()
        focusManager.focusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        assertFalse(lostTransient!!)
        assertFalse(focusManager.hasFocus())
    }

    @Test
    fun `listener receives onFocusLost transient callback`() {
        var lostTransient: Boolean? = null
        focusManager.listener = object : AudioFocusManager.FocusListener {
            override fun onFocusGained() {}
            override fun onFocusLost(transient: Boolean) { lostTransient = transient }
            override fun onDuck() {}
        }

        focusManager.requestFocus()
        focusManager.focusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        assertTrue(lostTransient!!)
        assertFalse(focusManager.hasFocus())
    }

    @Test
    fun `listener receives onDuck callback`() {
        var duckCalled = false
        focusManager.listener = object : AudioFocusManager.FocusListener {
            override fun onFocusGained() {}
            override fun onFocusLost(transient: Boolean) {}
            override fun onDuck() { duckCalled = true }
        }

        focusManager.requestFocus()
        focusManager.focusChangeListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        assertTrue(duckCalled)
    }

    @Test
    fun `destroy abandons focus and clears listener`() {
        focusManager.listener = object : AudioFocusManager.FocusListener {
            override fun onFocusGained() {}
            override fun onFocusLost(transient: Boolean) {}
            override fun onDuck() {}
        }
        focusManager.requestFocus()

        focusManager.destroy()

        assertFalse(focusManager.hasFocus())
        assertNull(focusManager.listener)
    }
}
