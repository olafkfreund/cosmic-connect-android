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
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.Helpers.DeviceHelper.getDeviceInfo
import org.cosmic.cosmicconnect.NetworkPacket

class LoopbackLink : BaseLink {
    constructor(context: Context, linkProvider: BaseLinkProvider) : super(context, linkProvider)

    override val name: String = "LoopbackLink"
    override val deviceInfo: DeviceInfo
        get() = getDeviceInfo(context)

    @WorkerThread
    override fun sendPacket(np: NetworkPacket, callback: Device.SendPacketStatusCallback, sendPayloadFromSameThread: Boolean): Boolean {
        packetReceived(np)
        if (np.hasPayload()) {
            callback.onPayloadProgressChanged(0)
            np.payload = np.payload // this triggers logic in the setter
            callback.onPayloadProgressChanged(100)
        }
        callback.onSuccess()
        return true
    }
}
