/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.LockPlugin

import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.di.PluginCreator
import org.cosmicext.connect.R

/**
 * Allows locking/unlocking the paired desktop's screen from Android.
 * Receives lock status updates and provides lock/unlock actions.
 */
class LockPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): LockPlugin
    }

    /** Whether the remote device's screen is currently locked. Null if unknown. */
    var isRemoteLocked: Boolean? = null
        private set

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_lock)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_lock_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_LOCK)
    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_LOCK_REQUEST)

    override fun onCreate(): Boolean {
        // Request current lock state on connection
        requestLockStatus()
        return true
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_LOCK) return false

        isRemoteLocked = (np.body["isLocked"] as? Boolean) ?: return true
        device.onPluginsChanged()
        Log.d(TAG, "Remote lock state: $isRemoteLocked")
        return true
    }

    /** Send a request to lock or unlock the remote device. */
    fun sendLockCommand(lock: Boolean) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_LOCK_REQUEST,
            body = mapOf("setLocked" to lock),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Sent lock command: lock=$lock to ${device.name}")
    }

    /** Request current lock status from the remote device. */
    fun requestLockStatus() {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_LOCK_REQUEST,
            body = mapOf("requestLocked" to true),
        )
        device.sendPacket(TransferPacket(packet))
    }

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.lock_lock)) { _ ->
            sendLockCommand(true)
        },
        PluginUiMenuEntry(context.getString(R.string.lock_unlock)) { _ ->
            sendLockCommand(false)
        },
    )

    companion object {
        private const val TAG = "LockPlugin"
        const val PACKET_TYPE_LOCK = "cconnect.lock"
        const val PACKET_TYPE_LOCK_REQUEST = "cconnect.lock.request"
    }
}
