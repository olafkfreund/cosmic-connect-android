/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import android.os.Build
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLink.PacketReceiver
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.DeviceStats.countReceived
import org.cosmic.cosmicconnect.DeviceStats.countSent
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the network links (connections) to a remote device.
 * Handles sending packets, receiving packets, and routing them
 * to the appropriate handler (pairing or plugin dispatch).
 */
class ConnectionManager(
    private val deviceId: String,
    private val deviceName: () -> String,
    private val onPairPacket: (TransferPacket) -> Unit,
    private val onDataPacket: (TransferPacket) -> Unit,
    private val isPaired: () -> Boolean,
    private val onUnpair: () -> Unit,
    private val onLinksChanged: (link: BaseLink) -> Unit,
    private val onLinksEmpty: () -> Unit
) : PacketReceiver {

    data class TransferPacketWithCallback(
        val tp: TransferPacket,
        val callback: Device.SendPacketStatusCallback
    )

    private val links = CopyOnWriteArrayList<BaseLink>()
    private val sendChannel = Channel<TransferPacketWithCallback>(Channel.BUFFERED)
    private var sendCoroutine: Job? = null

    private val defaultCallback: Device.SendPacketStatusCallback = object : Device.SendPacketStatusCallback() {
        override fun onSuccess() {}
        override fun onFailure(e: Throwable) {
            Log.e("ConnectionManager", "Send packet exception", e)
        }
    }

    val isReachable: Boolean
        get() = links.isNotEmpty()

    val connectivityType: String?
        get() = links.firstOrNull()?.name

    fun hasLinkFromProvider(provider: BaseLinkProvider): Boolean {
        return links.any { it.linkProvider == provider }
    }

    fun addLink(link: BaseLink) {
        synchronized(sendChannel) {
            if (sendCoroutine == null) {
                sendCoroutine = CoroutineScope(Dispatchers.IO).launch {
                    for ((tp, callback) in sendChannel) {
                        sendPacketBlocking(tp, callback)
                    }
                }
            }
        }

        links.add(link)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val copy = links.toMutableList()
            copy.sortWith { o1, o2 ->
                o2.linkProvider.priority compareTo o1.linkProvider.priority
            }
            links.clear()
            links.addAll(copy)
        } else {
            links.sortWith { o1, o2 ->
                o2.linkProvider.priority compareTo o1.linkProvider.priority
            }
        }

        link.addPacketReceiver(this)
        onLinksChanged(link)
    }

    @WorkerThread
    fun removeLink(link: BaseLink) {
        link.removePacketReceiver(this)
        links.remove(link)
        Log.i(
            "Cosmic/ConnectionManager",
            "removeLink: ${link.linkProvider.name} -> ${deviceName()} active links: ${links.size}"
        )
        if (links.isEmpty()) {
            onLinksEmpty()
            synchronized(sendChannel) {
                sendCoroutine?.cancel(CancellationException("Device disconnected"))
                sendCoroutine = null
            }
        }
    }

    fun disconnect() {
        links.forEach(BaseLink::disconnect)
    }

    override fun onPacketReceived(tp: TransferPacket) {
        countReceived(deviceId, tp.packet.type)

        if (org.cosmic.cosmicconnect.Core.PacketType.PAIR == tp.packet.type) {
            Log.i("Cosmic/ConnectionManager", "Pair packet")
            onPairPacket(tp)
            return
        }

        if (!isPaired()) {
            // Tell the remote device we're not paired (once), then drop the packet.
            // Don't call onUnpair() repeatedly — it triggers the full unpair chain
            // (remove from trusted devices, reload plugins) on every data packet,
            // causing an infinite cycle when the remote device keeps sending packets.
            onUnpair()
            return
        }

        onDataPacket(tp)
    }

    @AnyThread
    fun sendPacket(tp: TransferPacket, callback: Device.SendPacketStatusCallback) {
        sendChannel.trySend(TransferPacketWithCallback(tp, callback))
    }

    @AnyThread
    fun sendPacket(tp: TransferPacket) = sendPacket(tp, defaultCallback)

    @WorkerThread
    fun sendPacketBlocking(tp: TransferPacket, callback: Device.SendPacketStatusCallback): Boolean =
        sendPacketBlocking(tp, callback, false)

    @WorkerThread
    fun sendPacketBlocking(tp: TransferPacket): Boolean =
        sendPacketBlocking(tp, defaultCallback, false)

    @WorkerThread
    fun sendPacketBlocking(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean {
        val success = links.any { link ->
            try {
                link.sendTransferPacket(tp, callback, sendPayloadFromSameThread)
            } catch (e: IOException) {
                Log.w("Cosmic/sendPacket", "Failed to send TransferPacket", e)
                false
            }.also { sent ->
                countSent(deviceId, tp.type, sent)
            }
        }

        if (!success) {
            Log.e(
                "Cosmic/sendPacket",
                "No device link (of ${links.size} available) could send the packet. Packet ${tp.type} to ${deviceName()} lost!"
            )
        }

        return success
    }

    val linkCount: Int
        get() = links.size
}
