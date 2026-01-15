/*
 * SPDX-FileCopyrightText: 2025 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin

import android.Manifest
import org.json.JSONException
import org.json.JSONObject
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityListener.Companion.getInstance
import org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityListener.SubscriptionState
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R

@LoadablePlugin
class ConnectivityReportPlugin : Plugin() {

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_connectivity_report)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_connectivity_report_desc)

    /**
     * Connectivity state change listener
     *
     * Reports the current connectivity state when changed.
     *
     * The body should contain a key "signalStrengths" which has a dict that maps
     * a SubscriptionID (opaque value) to a dict with the connection info (See below)
     *
     * For example:
     * {
     *     "signalStrengths": {
     *         "6": {
     *             "networkType": "4G",
     *             "signalStrength": 3
     *         },
     *         "17": {
     *             "networkType": "HSPA",
     *             "signalStrength": 2
     *         },
     *         ...
     *     }
     * }
     */
    var listener = object : ConnectivityListener.StateCallback {
        override fun statesChanged(states : Map<Int, SubscriptionState>) {
            if (states.isEmpty()) {
                return
            }

            // Build signal strengths JSON
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

            // Create immutable packet with signal strengths
            val packet = NetworkPacket.create(
                PACKET_TYPE_CONNECTIVITY_REPORT,
                mapOf("signalStrengths" to signalStrengths)
            )

            // Convert and send
            device.sendPacket(convertToLegacyPacket(packet))
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

    /**
     * Convert immutable NetworkPacket to legacy NetworkPacket for sending
     */
    private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
        val legacy = LegacyNetworkPacket(ffi.type)

        // Copy all body fields
        ffi.body.forEach { (key, value) ->
            legacy.set(key, value)
        }

        return legacy
    }

    companion object {
        private const val PACKET_TYPE_CONNECTIVITY_REPORT = "cosmicconnect.connectivity_report"
    }
}
