/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Core

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.cosmicext.connect.Backends.BaseLink
import org.cosmicext.connect.Backends.BaseLinkProvider.ConnectionReceiver
import org.cosmicext.connect.Device
import org.cosmicext.connect.DeviceInfo
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.SecurityHelpers.SslHelper
import org.cosmicext.connect.Helpers.TrustedDevices
import org.cosmicext.connect.PairingHandler.PairingCallback
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.PluginFactory
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sslHelper: SslHelper,
    private val deviceHelper: DeviceHelper,
    private val pluginFactory: PluginFactory
) {
    fun interface DeviceListChangedCallback {
        fun onDeviceListChanged()
    }

    val devices: ConcurrentHashMap<String, Device> = ConcurrentHashMap()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val deviceListChangedCallbacks = ConcurrentHashMap<String, DeviceListChangedCallback>()

    private val devicePairingCallback: PairingCallback = object : PairingCallback {
        override fun incomingPairRequest() {
            onDeviceListChanged()
        }

        override fun pairingSuccessful() {
            onDeviceListChanged()
        }

        override fun pairingFailed(error: String) {
            onDeviceListChanged()
        }

        override fun unpaired(device: Device) {
            onDeviceListChanged()
            if (!device.isReachable) {
                scheduleForDeletion(device)
            }
        }
    }

    init {
        loadRememberedDevicesFromSettings()
    }

    fun addDeviceListChangedCallback(key: String, callback: DeviceListChangedCallback) {
        deviceListChangedCallbacks[key] = callback
    }

    fun removeDeviceListChangedCallback(key: String) {
        deviceListChangedCallbacks.remove(key)
    }

    private fun onDeviceListChanged() {
        Log.i("DeviceRegistry", "Device list changed, notifying ${deviceListChangedCallbacks.size} observers.")
        deviceListChangedCallbacks.values.forEach(DeviceListChangedCallback::onDeviceListChanged)
    }

    fun getDevice(id: String?): Device? {
        if (id == null) {
            return null
        }
        return devices[id]
    }

    fun <T : Plugin> getDevicePlugin(deviceId: String?, pluginClass: Class<T>): T? {
        val device = getDevice(deviceId)
        return device?.getPlugin(pluginClass)
    }

    private fun loadRememberedDevicesFromSettings() {
        val trustedDevices = TrustedDevices.getAllTrustedDevices(context)
        trustedDevices.asSequence()
            .onEach { Log.d("DeviceRegistry", "Loading device $it") }
            .forEach {
                try {
                    val device = Device(context, it, deviceHelper, pluginFactory, sslHelper)
                    val now = Date()
                    val x509Cert = device.certificate as X509Certificate
                    if (now < x509Cert.notBefore) {
                        throw CertificateException("Certificate not effective yet: " + x509Cert.notBefore)
                    } else if (now > x509Cert.notAfter) {
                        throw CertificateException("Certificate already expired: " + x509Cert.notAfter)
                    }
                    devices[it] = device
                    device.addPairingCallback(devicePairingCallback)
                } catch (e: CertificateException) {
                    Log.w(
                        "DeviceRegistry",
                        "Couldn't load the certificate for a remembered device. Removing from trusted list.", e
                    )
                    TrustedDevices.removeTrustedDevice(context, it)
                }
            }
    }

    val connectionListener: ConnectionReceiver = object : ConnectionReceiver {
        @WorkerThread
        override fun onConnectionReceived(link: BaseLink) {
            var device = devices[link.deviceId]
            if (device != null) {
                device.addLink(link)
            } else {
                device = Device(context, link, deviceHelper, pluginFactory, sslHelper)
                devices[link.deviceId] = device
                device.addPairingCallback(devicePairingCallback)
            }
            onDeviceListChanged()
        }

        @WorkerThread
        override fun onConnectionLost(link: BaseLink) {
            val device = devices[link.deviceId]
            Log.i("DeviceRegistry/onConnectionLost", "removeLink, deviceId: ${link.deviceId}")
            if (device != null) {
                device.removeLink(link)
                if (!device.isReachable && !device.isPaired) {
                    scheduleForDeletion(device)
                }
            } else {
                Log.d("DeviceRegistry/onConnectionLost", "Removing connection to unknown device")
            }
            onDeviceListChanged()
        }

        @WorkerThread
        override fun onDeviceInfoUpdated(deviceInfo: DeviceInfo) {
            val device = devices[deviceInfo.id]
            if (device == null) {
                Log.e("DeviceRegistry", "onDeviceInfoUpdated for an unknown device")
                return
            }
            val hasChanges = device.updateDeviceInfo(deviceInfo)
            if (hasChanges) {
                onDeviceListChanged()
            }
        }
    }

    fun scheduleForDeletion(device: Device) {
        Log.i("DeviceRegistry", "Scheduled for deletion: $device, paired: ${device.isPaired}, reachable: ${device.isReachable}")
        scope.launch {
            delay(1000)
            if (device.isReachable) {
                Log.i("DeviceRegistry", "Not deleting device since it's reachable again: $device")
                return@launch
            }
            if (device.isPaired) {
                Log.i("DeviceRegistry", "Not deleting device since it's still paired: $device")
                return@launch
            }
            Log.i("DeviceRegistry", "Deleting unpaired and unreachable device: $device")
            device.removePairingCallback(devicePairingCallback)
            devices.remove(device.deviceId)
        }
    }
}