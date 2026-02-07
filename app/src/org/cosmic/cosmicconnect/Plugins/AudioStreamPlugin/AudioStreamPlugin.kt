/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.AudioStreamPlugin

import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Core.getBoolean
import org.cosmic.cosmicconnect.Core.getInt
import org.cosmic.cosmicconnect.Core.getString
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * Allows streaming audio between Android and paired desktop.
 * Receives audio stream status updates and provides stream control actions.
 */
class AudioStreamPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): AudioStreamPlugin
    }

    /** Whether audio streaming is currently active. */
    var isStreaming: Boolean = false
        private set

    /** Active codec being used for streaming. Null if not streaming. */
    var activeCodec: String? = null
        private set

    /** Current sample rate in Hz. Null if not streaming. */
    var sampleRate: Int? = null
        private set

    /** Number of audio channels (1=mono, 2=stereo). Null if not streaming. */
    var channels: Int? = null
        private set

    /** Stream direction ("send" or "receive"). Null if not streaming. */
    var direction: String? = null
        private set

    /** List of supported audio codecs from capability packet. */
    var supportedCodecs: List<String> = emptyList()
        private set

    /** List of supported sample rates from capability packet. */
    var supportedSampleRates: List<Int> = emptyList()
        private set

    /** Maximum number of channels supported. Null if unknown. */
    var maxChannels: Int? = null
        private set

    fun interface StreamStateListener {
        fun onStreamStateChanged()
    }

    private val listeners = mutableListOf<StreamStateListener>()

    fun addStreamStateListener(listener: StreamStateListener) {
        if (listener !in listeners) listeners.add(listener)
    }

    fun removeStreamStateListener(listener: StreamStateListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it.onStreamStateChanged() }
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_audiostream)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_audiostream_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_AUDIOSTREAM,
        PACKET_TYPE_AUDIOSTREAM_CAPABILITY
    )
    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_AUDIOSTREAM_REQUEST)

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        when (np.type) {
            PACKET_TYPE_AUDIOSTREAM -> {
                val hasStreaming = np.body.containsKey("isStreaming")
                if (hasStreaming) {
                    isStreaming = np.getBoolean("isStreaming")
                    activeCodec = np.getString("codec")
                    sampleRate = np.body["sampleRate"] as? Int
                    channels = np.body["channels"] as? Int
                    direction = np.getString("direction")
                    notifyListeners()
                    device.onPluginsChanged()
                    Log.d(TAG, "Audio stream state: isStreaming=$isStreaming, codec=$activeCodec, sampleRate=$sampleRate")
                }
            }
            PACKET_TYPE_AUDIOSTREAM_CAPABILITY -> {
                // Parse capability info
                maxChannels = np.body["maxChannels"] as? Int

                @Suppress("UNCHECKED_CAST")
                val codecsList = np.body["codecs"] as? List<String>
                if (codecsList != null) {
                    supportedCodecs = codecsList
                }

                @Suppress("UNCHECKED_CAST")
                val sampleRatesList = np.body["sampleRates"] as? List<Int>
                if (sampleRatesList != null) {
                    supportedSampleRates = sampleRatesList
                }

                notifyListeners()
                Log.d(TAG, "Audio stream capabilities: codecs=$supportedCodecs, sampleRates=$supportedSampleRates, maxChannels=$maxChannels")
            }
        }
        return true
    }

    /** Send a request to start or stop audio streaming. */
    fun sendStreamCommand(start: Boolean, codec: String? = null, sampleRate: Int? = null, channels: Int? = null, direction: String? = null) {
        val body = mutableMapOf<String, Any>(
            if (start) "startStreaming" to true else "stopStreaming" to true
        )

        if (start) {
            codec?.let { body["codec"] = it }
            sampleRate?.let { body["sampleRate"] = it }
            channels?.let { body["channels"] = it }
            direction?.let { body["direction"] = it }
        }

        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_AUDIOSTREAM_REQUEST,
            body = body,
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Sent stream command: start=$start to ${device.name}")
    }

    companion object {
        private const val TAG = "AudioStreamPlugin"
        const val PACKET_TYPE_AUDIOSTREAM = "cconnect.audiostream"
        const val PACKET_TYPE_AUDIOSTREAM_REQUEST = "cconnect.audiostream.request"
        const val PACKET_TYPE_AUDIOSTREAM_CAPABILITY = "cconnect.audiostream.capability"
    }
}
