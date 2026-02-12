/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.NetworkInfoPlugin

import org.cosmicext.connect.Core.NetworkPacket

/**
 * Extension properties for type-safe inspection of NetworkInfo packets.
 *
 * No FFI wrapper is needed since cosmic-connect-core does not have a
 * NetworkInfo plugin — packets are created directly via the NetworkPacket
 * data class constructor.
 *
 * ## Packet Types
 *
 * - **NetworkInfo** (`cconnect.networkinfo`): Network state information (outgoing)
 * - **NetworkInfo Request** (`cconnect.networkinfo.request`): Request network info (incoming)
 *
 * ## Network State Fields
 *
 * - `connected` (boolean): Whether the device has network connectivity
 * - `networkType` (string): "WiFi", "Cellular", "Ethernet", "Bluetooth", "VPN", "Unknown"
 * - `wifiSsid` (string, optional): WiFi SSID if connected to WiFi
 * - `wifiSignalStrength` (int, optional): WiFi signal level 0-4
 * - `wifiRssi` (int, optional): WiFi RSSI in dBm
 * - `isMetered` (boolean, optional): Whether the connection is metered
 * - `isVpn` (boolean, optional): Whether a VPN is active
 */

/**
 * Check if this packet is a network info status packet.
 */
val NetworkPacket.isNetworkInfoPacket: Boolean
    get() = type == "cconnect.networkinfo" && body.containsKey("connected")

/**
 * Check if this packet is a network info request.
 */
val NetworkPacket.isNetworkInfoRequest: Boolean
    get() = type == "cconnect.networkinfo.request"

/**
 * Whether the device reports having network connectivity.
 * Returns null if not a network info packet.
 */
val NetworkPacket.networkInfoConnected: Boolean?
    get() = if (isNetworkInfoPacket) body["connected"] as? Boolean else null

/**
 * The type of network connection ("WiFi", "Cellular", "Ethernet", etc.).
 * Returns null if not a network info packet or device is disconnected.
 */
val NetworkPacket.networkInfoType: String?
    get() = if (isNetworkInfoPacket) body["networkType"] as? String else null

/**
 * WiFi SSID if connected to WiFi.
 * Returns null if not on WiFi or not a network info packet.
 */
val NetworkPacket.networkInfoWifiSsid: String?
    get() = if (isNetworkInfoPacket) body["wifiSsid"] as? String else null

/**
 * WiFi signal strength level (0-4).
 * Returns null if not on WiFi or not a network info packet.
 */
val NetworkPacket.networkInfoWifiSignalStrength: Int?
    get() = if (isNetworkInfoPacket) (body["wifiSignalStrength"] as? Number)?.toInt() else null

/**
 * Whether the network connection is metered.
 * Returns null if not a network info packet or field is missing.
 */
val NetworkPacket.networkInfoIsMetered: Boolean?
    get() = if (isNetworkInfoPacket) body["isMetered"] as? Boolean else null

/**
 * Whether a VPN is active.
 * Returns null if not a network info packet or field is missing.
 */
val NetworkPacket.networkInfoIsVpn: Boolean?
    get() = if (isNetworkInfoPacket) body["isVpn"] as? Boolean else null
