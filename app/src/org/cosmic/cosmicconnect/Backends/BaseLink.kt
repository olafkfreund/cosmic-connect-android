/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Backends

import android.content.Context
import androidx.annotation.WorkerThread
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

abstract class BaseLink protected constructor(
    protected val context: Context,
    val linkProvider: BaseLinkProvider
) {

    fun interface PacketReceiver {
        fun onPacketReceived(tp: TransferPacket)
    }

    private val receivers = CopyOnWriteArrayList<PacketReceiver>()

    /* To be implemented by each link for pairing handlers */
    abstract val name: String

    abstract val deviceInfo: DeviceInfo

    val deviceId: String
        get() = deviceInfo.id

    fun addPacketReceiver(pr: PacketReceiver) {
        receivers.add(pr)
    }

    fun removePacketReceiver(pr: PacketReceiver) {
        receivers.remove(pr)
    }

    //Should be called from a background thread listening for packets
    fun packetReceived(tp: TransferPacket) {
        for (pr in receivers) {
            pr.onPacketReceived(tp)
        }
    }

    open fun disconnect() {
        linkProvider.onConnectionLost(this)
    }

    /**
     * Send a TransferPacket (Core.NetworkPacket + payload).
     *
     * @param tp the transfer packet to send
     * @param callback status callback for success/failure/progress
     * @param sendPayloadFromSameThread if false, payload should be sent from a separate thread
     */
    @WorkerThread
    @Throws(IOException::class)
    abstract fun sendTransferPacket(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean
}
