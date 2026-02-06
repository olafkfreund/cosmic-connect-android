/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FindRemoteDevicePlugin

import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

class FindRemoteDevicePlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): FindRemoteDevicePlugin
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice_desc)

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean = true

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.ring)) { parentActivity ->
            val packet = NetworkPacket.create(
                FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST,
                emptyMap()
            )

            val legacyPacket = LegacyNetworkPacket(packet.type)
            device.sendPacket(legacyPacket)
        }
    )

    override val supportedPacketTypes: Array<String> = emptyArray()

    override val outgoingPacketTypes: Array<String> = arrayOf(FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST)
}
