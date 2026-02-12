/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.AudioStreamPlugin

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages AudioTrack lifecycle and audio playback state for streaming sessions.
 *
 * Supports play/pause/resume/duck/stop lifecycle and PCM 16-bit streaming mode.
 * The [createAudioTrack] method is protected open so tests can override it to
 * avoid AudioTrack.Builder failures under Robolectric.
 */
open class AudioStreamSession(
    private val sampleRate: Int,
    private val channels: Int,
    private val codec: String,
) {
    companion object {
        private const val TAG = "AudioStreamSession"
    }

    enum class State { IDLE, PLAYING, PAUSED, DUCKED, STOPPED, ERROR }

    interface SessionListener {
        fun onStateChanged(state: State)
        fun onError(message: String)
    }

    var listener: SessionListener? = null
    private var audioTrack: AudioTrack? = null
    private var state: State = State.IDLE
    private val stopped = AtomicBoolean(false)

    /**
     * Creates an [AudioTrack] with the given parameters. Protected open so tests
     * can override to return a mock or null without hitting AudioTrack.Builder
     * issues under Robolectric.
     */
    protected open fun createAudioTrack(
        channelConfig: Int,
        encoding: Int,
        bufferSize: Int,
    ): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .setEncoding(encoding)
            .build()

        return AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun prepare(): Boolean {
        return try {
            val channelConfig = when (channels) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> {
                    Log.e(TAG, "Unsupported channel count: $channels")
                    setState(State.ERROR)
                    listener?.onError("Unsupported channel count: $channels")
                    return false
                }
            }

            val encoding = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding)
            if (bufferSize == AudioTrack.ERROR_BAD_VALUE || bufferSize == AudioTrack.ERROR) {
                Log.e(TAG, "Invalid buffer size for sampleRate=$sampleRate, channels=$channels")
                setState(State.ERROR)
                listener?.onError("Invalid audio configuration: sampleRate=$sampleRate, channels=$channels")
                return false
            }

            audioTrack = createAudioTrack(channelConfig, encoding, bufferSize * 2)

            Log.i(TAG, "AudioTrack prepared: ${sampleRate}Hz, ${channels}ch, codec=$codec, buffer=${bufferSize * 2}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare AudioTrack", e)
            setState(State.ERROR)
            listener?.onError("Failed to prepare audio: ${e.message}")
            false
        }
    }

    fun play() {
        if (stopped.get()) return
        try {
            audioTrack?.play()
            setState(State.PLAYING)
            Log.i(TAG, "Playback started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
            setState(State.ERROR)
            listener?.onError("Playback failed: ${e.message}")
        }
    }

    fun writeAudioData(data: ByteArray, offset: Int = 0, size: Int = data.size): Int {
        if (stopped.get() || state == State.STOPPED || state == State.ERROR) return -1
        return try {
            audioTrack?.write(data, offset, size) ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Write error", e)
            -1
        }
    }

    fun pause() {
        try {
            audioTrack?.pause()
            setState(State.PAUSED)
            Log.i(TAG, "Playback paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause", e)
        }
    }

    fun resume() {
        if (state == State.PAUSED || state == State.DUCKED) {
            try {
                audioTrack?.play()
                setState(State.PLAYING)
                Log.i(TAG, "Playback resumed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume", e)
            }
        }
    }

    fun duck() {
        try {
            audioTrack?.setVolume(0.2f)
            setState(State.DUCKED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duck volume", e)
        }
    }

    fun unduck() {
        try {
            audioTrack?.setVolume(1.0f)
            if (state == State.DUCKED) setState(State.PLAYING)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore volume", e)
        }
    }

    fun setVolume(volume: Float) {
        try {
            audioTrack?.setVolume(volume.coerceIn(0f, 1f))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set volume", e)
        }
    }

    fun stop() {
        stopped.set(true)
        try {
            audioTrack?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioTrack", e)
        }
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
        setState(State.STOPPED)
        Log.i(TAG, "Playback stopped and released")
    }

    fun getState(): State = state

    private fun setState(newState: State) {
        state = newState
        try {
            listener?.onStateChanged(newState)
        } catch (e: Exception) {
            Log.e(TAG, "Listener error", e)
        }
    }
}
