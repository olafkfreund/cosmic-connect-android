/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import org.cosmic.cosmicconnect.Core.TransferPacket

/**
 * Interface for sending packets to a remote device.
 * Breaks circular dependencies: components that only need to send packets
 * can depend on this interface instead of the full Device class.
 */
interface PacketSender {
    @AnyThread
    fun sendPacket(np: NetworkPacket, callback: Device.SendPacketStatusCallback)

    @AnyThread
    fun sendPacket(np: NetworkPacket)

    @WorkerThread
    fun sendPacketBlocking(np: NetworkPacket, callback: Device.SendPacketStatusCallback): Boolean

    @WorkerThread
    fun sendPacketBlocking(np: NetworkPacket): Boolean

    @WorkerThread
    fun sendPacketBlocking(
        np: NetworkPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean

    /** Send a TransferPacket (Core.NetworkPacket + payload) asynchronously. */
    @AnyThread
    fun sendPacket(tp: TransferPacket, callback: Device.SendPacketStatusCallback) {
        sendPacket(tp.toLegacy(), callback)
    }

    /** Send a TransferPacket (Core.NetworkPacket + payload) asynchronously with default callback. */
    @AnyThread
    fun sendPacket(tp: TransferPacket) {
        sendPacket(tp.toLegacy())
    }

    /** Send a TransferPacket blocking. */
    @WorkerThread
    fun sendPacketBlocking(tp: TransferPacket, callback: Device.SendPacketStatusCallback): Boolean {
        return sendPacketBlocking(tp.toLegacy(), callback)
    }

    /** Send a TransferPacket blocking with default callback. */
    @WorkerThread
    fun sendPacketBlocking(tp: TransferPacket): Boolean {
        return sendPacketBlocking(tp.toLegacy())
    }

    /** Send a TransferPacket blocking with payload thread control. */
    @WorkerThread
    fun sendPacketBlocking(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean {
        return sendPacketBlocking(tp.toLegacy(), callback, sendPayloadFromSameThread)
    }
}
