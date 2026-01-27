/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Backends.LoopbackBackend

import android.content.Context
import android.net.Network
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoopbackLinkProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceHelper: DeviceHelper
) : BaseLinkProvider() {

    override val name: String = "LoopbackLinkProvider"
    override val priority: Int = 0

    override fun onStart() {
        onNetworkChange(null)
    }

    override fun onStop() { }

    override fun onNetworkChange(network: Network?) {
        val link = LoopbackLink(context, this, deviceHelper)
        onConnectionReceived(link)
    }
}