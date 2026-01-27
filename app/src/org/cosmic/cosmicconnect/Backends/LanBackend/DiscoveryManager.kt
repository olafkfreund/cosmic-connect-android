/*
 * SPDX-FileCopyrightText: 2024 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Backends.LanBackend

import android.content.Context
import android.util.Log
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import uniffi.cosmic_connect_core.DiscoveryService
import uniffi.cosmic_connect_core.DiscoveryCallback
import uniffi.cosmic_connect_core.FfiDeviceInfo
import uniffi.cosmic_connect_core.FfiPacket
import uniffi.cosmic_connect_core.startDiscovery
import java.net.InetAddress

class DiscoveryManager(
    private val context: Context,
    private val lanLinkProvider: LanLinkProvider,
    private val deviceHelper: DeviceHelper
) : DiscoveryCallback {

    private var discovery: DiscoveryService? = null

    fun start() {
        if (discovery != null) return

        Log.i("DiscoveryManager", "Starting FFI Discovery")
        
        // Use device ID from DeviceHelper
        val info = deviceHelper.getDeviceInfo()
        
        // Convert to FfiDeviceInfo
        val ffiInfo = FfiDeviceInfo(
            deviceId = info.id,
            deviceName = info.name,
            deviceType = info.type.toString(),
            protocolVersion = info.protocolVersion,
            incomingCapabilities = info.incomingCapabilities?.toList() ?: emptyList(),
            outgoingCapabilities = info.outgoingCapabilities?.toList() ?: emptyList(),
            tcpPort = lanLinkProvider.tcpPort.toUShort()
        )
        
        // Initialize UniFFI Discovery
        try {
            discovery = startDiscovery(ffiInfo, this)
        } catch (e: Exception) {
            Log.e("DiscoveryManager", "Failed to start discovery", e)
        }
    }

    fun stop() {
        Log.i("DiscoveryManager", "Stopping FFI Discovery")
        discovery?.close() // DiscoveryService implements AutoCloseable/Disposable
        discovery = null
    }

    fun restart() {
        stop()
        start()
    }

    //
    // DiscoveryCallback Implementation
    //

    override fun onDeviceFound(device: FfiDeviceInfo) {
        Log.i("DiscoveryManager", "FFI Discovered device: ${device.deviceName} (${device.deviceId})")
        // FFI layer handles discovery, we might receive identity later
    }

    override fun onDeviceLost(deviceId: String) {
        Log.i("DiscoveryManager", "FFI Device lost: $deviceId")
        // Handle device lost if needed (remove from UI?)
    }

    override fun onIdentityReceived(deviceId: String, packet: FfiPacket) {
        Log.i("DiscoveryManager", "FFI Identity received from: $deviceId")
        // TODO: Pass identity packet to LinkProvider or handle it?
        // For now logging it. LanLinkProvider might need update to accept FfiPacket or NetworkPacket
    }
}
