/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.NetworkInfoPlugin

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * Reports the Android device's network connectivity status to paired desktop devices.
 *
 * Complements [ConnectivityReportPlugin] (cellular/SIM) by reporting WiFi and
 * general network state: connection type, WiFi SSID, signal strength, and IP address.
 *
 * Sends updates when network state changes and responds to requests from the desktop.
 */
class NetworkInfoPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): NetworkInfoPlugin
    }

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_networkinfo)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_networkinfo_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_NETWORKINFO_REQUEST)
    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_NETWORKINFO)

    override fun onCreate(): Boolean {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                sendNetworkInfo(capabilities)
            }

            override fun onLost(network: Network) {
                sendDisconnectedInfo()
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, callback)
        networkCallback = callback

        // Send initial state
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager?.getNetworkCapabilities(it) }
        if (capabilities != null) {
            sendNetworkInfo(capabilities)
        } else {
            sendDisconnectedInfo()
        }

        return true
    }

    override fun onDestroy() {
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "NetworkCallback already unregistered", e)
            }
        }
        networkCallback = null
        connectivityManager = null
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        if (tp.packet.type != PACKET_TYPE_NETWORKINFO_REQUEST) return false

        // Desktop requested our network info — send current state
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager?.getNetworkCapabilities(it) }
        if (capabilities != null) {
            sendNetworkInfo(capabilities)
        } else {
            sendDisconnectedInfo()
        }
        return true
    }

    private fun sendNetworkInfo(capabilities: NetworkCapabilities) {
        val body = mutableMapOf<String, Any>(
            "connected" to true,
            "networkType" to getNetworkType(capabilities),
        )

        // WiFi-specific info
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val wifiInfo = getWifiInfo(capabilities)
            wifiInfo?.let { info ->
                val ssid = info.ssid?.removeSurrounding("\"")
                if (ssid != null && ssid != "<unknown ssid>") {
                    body["wifiSsid"] = ssid
                }
                body["wifiSignalStrength"] = WifiManager.calculateSignalLevel(info.rssi, 5)
                body["wifiRssi"] = info.rssi
            }
        }

        // Metered status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            body["isMetered"] = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }

        // VPN status
        body["isVpn"] = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_NETWORKINFO,
            body = body,
        )
        device.sendPacket(TransferPacket(packet))
    }

    private fun sendDisconnectedInfo() {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_NETWORKINFO,
            body = mapOf("connected" to false),
        )
        device.sendPacket(TransferPacket(packet))
    }

    private fun getNetworkType(capabilities: NetworkCapabilities): String {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Unknown"
        }
    }

    @Suppress("DEPRECATION")
    private fun getWifiInfo(capabilities: NetworkCapabilities): WifiInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return capabilities.transportInfo as? WifiInfo
        }
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.connectionInfo
    }

    companion object {
        private const val TAG = "NetworkInfoPlugin"
        const val PACKET_TYPE_NETWORKINFO = "cconnect.networkinfo"
        const val PACKET_TYPE_NETWORKINFO_REQUEST = "cconnect.networkinfo.request"
    }
}
