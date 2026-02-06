/*
 * SPDX-FileCopyrightText: 2018 Nicolas Fella <nicolas.fella@gmx.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.SystemVolumePlugin

import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONException
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Core.has
import org.cosmic.cosmicconnect.Core.getString
import org.cosmic.cosmicconnect.Core.getInt
import org.cosmic.cosmicconnect.Core.getBoolean
import org.cosmic.cosmicconnect.Core.getJSONArray
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class SystemVolumePlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): SystemVolumePlugin
    }

    fun interface SinkListener {
        fun sinksChanged()
    }

    @JvmField
    internal val sinks = ConcurrentHashMap<String, Sink>()
    private val listeners = CopyOnWriteArrayList<SinkListener>()

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_systemvolume)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_systemvolume_desc)

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.has("sinkList")) {
            sinks.clear()

            try {
                val sinkArray = np.getJSONArray("sinkList")
                if (sinkArray != null) {
                    for (i in 0 until sinkArray.length()) {
                        val sinkObj = sinkArray.getJSONObject(i)
                        val sink = Sink(sinkObj)
                        sinks[sink.name] = sink
                    }
                }
            } catch (e: JSONException) {
                Log.e("COSMICConnect", "Exception", e)
            }

            for (l in listeners) {
                l.sinksChanged()
            }
        } else {
            val name = np.getString("name")
            sinks[name]?.let { sink ->
                if (np.has("volume")) {
                    sink.setVolume(np.getInt("volume"))
                }
                if (np.has("muted")) {
                    sink.setMute(np.getBoolean("muted"))
                }
                if (np.has("enabled")) {
                    sink.isDefault = np.getBoolean("enabled")
                }
            }
        }
        return true
    }

    fun sendVolume(name: String, volume: Int) {
        val ffiPacket = SystemVolumePacketsFFI.createVolumeRequest(name, volume)
        device.sendPacket(TransferPacket(ffiPacket))
    }

    fun sendMute(name: String, mute: Boolean) {
        val ffiPacket = SystemVolumePacketsFFI.createMuteRequest(name, mute)
        device.sendPacket(TransferPacket(ffiPacket))
    }

    fun sendEnable(name: String) {
        val ffiPacket = SystemVolumePacketsFFI.createEnableRequest(name)
        device.sendPacket(TransferPacket(ffiPacket))
    }

    fun requestSinkList() {
        val ffiPacket = SystemVolumePacketsFFI.createSinkListRequest()
        device.sendPacket(TransferPacket(ffiPacket))
    }

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_SYSTEMVOLUME)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_SYSTEMVOLUME_REQUEST)

    fun getSinks(): Collection<Sink> = sinks.values

    fun addSinkListener(listener: SinkListener) {
        listeners.add(listener)
    }

    fun removeSinkListener(listener: SinkListener) {
        listeners.remove(listener)
    }

    companion object {
        private const val PACKET_TYPE_SYSTEMVOLUME = "cconnect.systemvolume"
        private const val PACKET_TYPE_SYSTEMVOLUME_REQUEST = "cconnect.systemvolume.request"
    }
}
