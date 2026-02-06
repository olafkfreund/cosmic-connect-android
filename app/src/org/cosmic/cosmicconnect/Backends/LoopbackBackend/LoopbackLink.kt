/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Backends.LoopbackBackend

import android.content.Context
import androidx.annotation.WorkerThread
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.Helpers.DeviceHelper

class LoopbackLink(
    context: Context,
    linkProvider: BaseLinkProvider,
    private val deviceHelper: DeviceHelper
) : BaseLink(context, linkProvider) {

    override val name: String = "LoopbackLink"
    override val deviceInfo: DeviceInfo
        get() = deviceHelper.getDeviceInfo()

    @WorkerThread
    override fun sendTransferPacket(tp: TransferPacket, callback: Device.SendPacketStatusCallback, sendPayloadFromSameThread: Boolean): Boolean {
        packetReceived(tp)
        if (tp.hasPayload) {
            callback.onPayloadProgressChanged(0)
            callback.onPayloadProgressChanged(100)
        }
        callback.onSuccess()
        return true
    }
}
