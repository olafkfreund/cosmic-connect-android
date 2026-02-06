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
    private val onPairPacket: (NetworkPacket) -> Unit,
    private val onDataPacket: (NetworkPacket) -> Unit,
    private val isPaired: () -> Boolean,
    private val onUnpair: () -> Unit,
    private val onLinksChanged: (link: BaseLink) -> Unit,
    private val onLinksEmpty: () -> Unit
) : PacketReceiver {

    data class NetworkPacketWithCallback(
        val np: NetworkPacket,
        val callback: Device.SendPacketStatusCallback
    )

    private val links = CopyOnWriteArrayList<BaseLink>()
    private val sendChannel = Channel<NetworkPacketWithCallback>(Channel.BUFFERED)
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
                    for ((np, callback) in sendChannel) {
                        sendPacketBlocking(np, callback)
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

    override fun onPacketReceived(np: NetworkPacket) {
        countReceived(deviceId, np.type)

        if (NetworkPacket.PACKET_TYPE_PAIR == np.type) {
            Log.i("Cosmic/ConnectionManager", "Pair packet")
            onPairPacket(np)
            return
        }

        if (!isPaired()) {
            onUnpair()
        }

        onDataPacket(np)
    }

    @AnyThread
    fun sendPacket(np: NetworkPacket, callback: Device.SendPacketStatusCallback) {
        sendChannel.trySend(NetworkPacketWithCallback(np, callback))
    }

    @AnyThread
    fun sendPacket(np: NetworkPacket) = sendPacket(np, defaultCallback)

    @WorkerThread
    fun sendPacketBlocking(np: NetworkPacket, callback: Device.SendPacketStatusCallback): Boolean =
        sendPacketBlocking(np, callback, false)

    @WorkerThread
    fun sendPacketBlocking(np: NetworkPacket): Boolean = sendPacketBlocking(np, defaultCallback, false)

    @WorkerThread
    fun sendPacketBlocking(
        np: NetworkPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean {
        val success = links.any { link ->
            try {
                link.sendPacket(np, callback, sendPayloadFromSameThread)
            } catch (e: IOException) {
                Log.w("Cosmic/sendPacket", "Failed to send packet", e)
                false
            }.also { sent ->
                countSent(deviceId, np.type, sent)
            }
        }

        if (!success) {
            Log.e(
                "Cosmic/sendPacket",
                "No device link (of ${links.size} available) could send the packet. Packet ${np.type} to ${deviceName()} lost!"
            )
        }

        return success
    }

    val linkCount: Int
        get() = links.size
}
