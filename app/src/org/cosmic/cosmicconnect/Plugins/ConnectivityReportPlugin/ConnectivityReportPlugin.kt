/*
 * SPDX-FileCopyrightText: 2025 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin

import android.Manifest
import android.content.Context
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONException
import org.json.JSONObject
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityListener.Companion.getInstance
import org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityListener.SubscriptionState
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

class ConnectivityReportPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): ConnectivityReportPlugin
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_connectivity_report)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_connectivity_report_desc)

    var listener = object : ConnectivityListener.StateCallback {
        override fun statesChanged(states : Map<Int, SubscriptionState>) {
            if (states.isEmpty()) {
                return
            }

            val signalStrengths = JSONObject()
            states.forEach { (subID: Int, subscriptionState: SubscriptionState) ->
                try {
                    val subInfo = JSONObject()
                    subInfo.put("networkType", subscriptionState.networkType)
                    subInfo.put("signalStrength", subscriptionState.signalStrength)
                    signalStrengths.put(subID.toString(), subInfo)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            val packet = ConnectivityPacketsFFI.createConnectivityReport(
                signalStrengths.toString()
            )

            device.sendPacket(packet.toLegacyPacket())
        }
    }

    override fun onCreate(): Boolean {
        getInstance(context).listenStateChanges(listener)
        return true
    }

    override fun onDestroy() {
        getInstance(context).cancelActiveListener(listener)
    }

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        return false
    }

    override val supportedPacketTypes: Array<String> = emptyArray()

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_CONNECTIVITY_REPORT)

    override val requiredPermissions: Array<String> = arrayOf(Manifest.permission.READ_PHONE_STATE)

    companion object {
        private const val PACKET_TYPE_CONNECTIVITY_REPORT = "cconnect.connectivity_report"
    }
}
