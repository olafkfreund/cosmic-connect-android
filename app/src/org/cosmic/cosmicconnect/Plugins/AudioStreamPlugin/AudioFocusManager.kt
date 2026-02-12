/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.AudioStreamPlugin

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/**
 * Manages Android audio focus requests for audio streaming playback.
 * Handles focus gain/loss/ducking callbacks and delegates to a [FocusListener].
 */
class AudioFocusManager(context: Context) {
    companion object {
        private const val TAG = "AudioFocusManager"
    }

    interface FocusListener {
        fun onFocusGained()
        fun onFocusLost(transient: Boolean)
        fun onDuck()
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    var listener: FocusListener? = null

    /** Visible for testing: invoke to simulate audio focus changes. */
    internal val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                Log.i(TAG, "Audio focus gained")
                listener?.onFocusGained()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false
                Log.i(TAG, "Audio focus lost permanently")
                listener?.onFocusLost(transient = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                Log.i(TAG, "Audio focus lost transiently")
                listener?.onFocusLost(transient = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.i(TAG, "Audio focus ducking")
                listener?.onDuck()
            }
        }
    }

    fun requestFocus(): Boolean {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusChangeListener)
            .setWillPauseWhenDucked(false)
            .build()

        focusRequest = request
        val result = audioManager.requestAudioFocus(request)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        Log.i(TAG, "Audio focus request: ${if (hasFocus) "granted" else "denied"}")
        return hasFocus
    }

    fun abandonFocus() {
        focusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            Log.i(TAG, "Audio focus abandoned")
        }
        focusRequest = null
        hasFocus = false
    }

    fun hasFocus(): Boolean = hasFocus

    fun destroy() {
        abandonFocus()
        listener = null
    }
}
