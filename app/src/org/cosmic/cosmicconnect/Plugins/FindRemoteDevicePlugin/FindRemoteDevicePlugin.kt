/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FindRemoteDevicePlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R

@LoadablePlugin
class FindRemoteDevicePlugin : Plugin() {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice_desc)

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean = true

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.ring)) { parentActivity ->
            // Create immutable NetworkPacket via FFI
            val packet = NetworkPacket.create(
                FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST,
                emptyMap()
            )

            // Convert to legacy packet for Device.sendPacket()
            val legacyPacket = LegacyNetworkPacket(packet.type)
            device.sendPacket(legacyPacket)
        }
    )

    override val supportedPacketTypes: Array<String> = emptyArray()

    override val outgoingPacketTypes: Array<String> = arrayOf(FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST)
}
