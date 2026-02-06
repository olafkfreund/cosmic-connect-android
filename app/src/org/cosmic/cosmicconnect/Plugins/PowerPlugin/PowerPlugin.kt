/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.PowerPlugin

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * Sends power management commands (shutdown, reboot, suspend, hibernate)
 * to paired desktop devices. Commands require user confirmation via dialog.
 *
 * Also receives power status from the desktop (e.g. laptop battery level).
 */
class PowerPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): PowerPlugin
    }

    /** Latest power status received from the desktop, or null if none received yet. */
    var remoteStatus: RemotePowerStatus? = null
        private set

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_power)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_power_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_POWER)
    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_POWER_REQUEST)

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_POWER) return false

        remoteStatus = RemotePowerStatus.fromPacket(np)
        device.onPluginsChanged()
        return true
    }

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.power_shutdown)) { activity ->
            confirmAndSend(activity, ACTION_SHUTDOWN, R.string.power_confirm_shutdown)
        },
        PluginUiMenuEntry(context.getString(R.string.power_reboot)) { activity ->
            confirmAndSend(activity, ACTION_REBOOT, R.string.power_confirm_reboot)
        },
        PluginUiMenuEntry(context.getString(R.string.power_suspend)) { activity ->
            confirmAndSend(activity, ACTION_SUSPEND, R.string.power_confirm_suspend)
        },
        PluginUiMenuEntry(context.getString(R.string.power_hibernate)) { activity ->
            confirmAndSend(activity, ACTION_HIBERNATE, R.string.power_confirm_hibernate)
        },
    )

    private fun confirmAndSend(activity: android.app.Activity, action: String, messageRes: Int) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.power_confirm_title)
            .setMessage(context.getString(messageRes, device.name))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                sendPowerCommand(action)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    internal fun sendPowerCommand(action: String) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_POWER_REQUEST,
            body = mapOf("action" to action),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Sent power command: $action to ${device.name}")
    }

    companion object {
        private const val TAG = "PowerPlugin"
        const val PACKET_TYPE_POWER = "cconnect.power"
        const val PACKET_TYPE_POWER_REQUEST = "cconnect.power.request"

        const val ACTION_SHUTDOWN = "shutdown"
        const val ACTION_REBOOT = "reboot"
        const val ACTION_SUSPEND = "suspend"
        const val ACTION_HIBERNATE = "hibernate"

        val VALID_ACTIONS = setOf(ACTION_SHUTDOWN, ACTION_REBOOT, ACTION_SUSPEND, ACTION_HIBERNATE)
    }
}
