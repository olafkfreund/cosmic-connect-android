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
    fun sendPacket(tp: TransferPacket, callback: Device.SendPacketStatusCallback)

    @AnyThread
    fun sendPacket(tp: TransferPacket)

    @WorkerThread
    fun sendPacketBlocking(tp: TransferPacket, callback: Device.SendPacketStatusCallback): Boolean

    @WorkerThread
    fun sendPacketBlocking(tp: TransferPacket): Boolean

    @WorkerThread
    fun sendPacketBlocking(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean
}
