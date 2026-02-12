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

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<StreamStateListener>()

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

    override fun onCreate(): Boolean {
        val packet = NetworkPacket(
            id = System.nanoTime(),
            type = PACKET_TYPE_AUDIOSTREAM_REQUEST,
            body = mapOf("queryCapabilities" to true),
        )
        device.sendPacket(TransferPacket(packet))
        return true
    }

    override fun onDestroy() {
        listeners.clear()
        isStreaming = false
        activeCodec = null
        sampleRate = null
        channels = null
        direction = null
        supportedCodecs = emptyList()
        supportedSampleRates = emptyList()
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        when (np.type) {
            PACKET_TYPE_AUDIOSTREAM -> {
                val hasStreaming = np.body.containsKey("isStreaming")
                if (hasStreaming) {
                    isStreaming = np.getBoolean("isStreaming")
                    if (isStreaming) {
                        activeCodec = np.getString("codec")
                        sampleRate = (np.body["sampleRate"] as? Number)?.toInt()?.takeIf { it in 1..192000 }
                        channels = (np.body["channels"] as? Number)?.toInt()?.takeIf { it in 1..8 }
                        direction = np.getString("direction")
                    } else {
                        activeCodec = null
                        sampleRate = null
                        channels = null
                        direction = null
                    }
                    notifyListeners()
                    device.onPluginsChanged()
                    Log.i(TAG, "Audio stream state: isStreaming=$isStreaming, codec=$activeCodec, sampleRate=$sampleRate")
                }
            }
            PACKET_TYPE_AUDIOSTREAM_CAPABILITY -> {
                // Parse capability info
                maxChannels = (np.body["maxChannels"] as? Number)?.toInt()?.takeIf { it in 1..8 }

                val codecsValue = np.body["codecs"]
                supportedCodecs = when (codecsValue) {
                    is List<*> -> codecsValue.filterIsInstance<String>()
                    is String -> try {
                        val arr = org.json.JSONArray(codecsValue)
                        (0 until arr.length()).map { arr.getString(it) }
                    } catch (e: Exception) { emptyList() }
                    else -> emptyList()
                }

                val ratesValue = np.body["sampleRates"]
                supportedSampleRates = when (ratesValue) {
                    is List<*> -> ratesValue.filterIsInstance<Number>().map { it.toInt() }.filter { it in 1..192000 }
                    is String -> try {
                        val arr = org.json.JSONArray(ratesValue)
                        (0 until arr.length()).map { arr.getInt(it) }.filter { it in 1..192000 }
                    } catch (e: Exception) { emptyList() }
                    else -> emptyList()
                }

                notifyListeners()
                Log.i(TAG, "Audio stream capabilities: codecs=$supportedCodecs, sampleRates=$supportedSampleRates, maxChannels=$maxChannels")
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
