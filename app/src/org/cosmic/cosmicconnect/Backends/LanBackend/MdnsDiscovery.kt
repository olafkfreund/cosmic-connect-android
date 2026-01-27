/*
 * SPDX-FileCopyrightText: 2024 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Backends.LanBackend

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import java.net.InetAddress

class MdnsDiscovery(
    private val context: Context,
    private val lanLinkProvider: LanLinkProvider,
    private val deviceHelper: DeviceHelper
) {

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    private val nsdResolveQueue = NsdResolveQueue()

    fun startDiscovering() {
        if (discoveryListener != null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("MdnsDiscovery", "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d("MdnsDiscovery", "Service discovery success: $service")
                if (service.serviceType != SERVICE_TYPE) {
                    Log.d("MdnsDiscovery", "Unknown Service Type: ${service.serviceType}")
                } else if (service.serviceName.contains(deviceHelper.getDeviceId())) {
                    Log.d("MdnsDiscovery", "Same machine")
                } else {
                    val manager = nsdManager ?: return
                    nsdResolveQueue.resolveOrEnqueue(manager, service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e("MdnsDiscovery", "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            Log.d("MdnsDiscovery", "Resolve Succeeded. $serviceInfo")

                            val host: InetAddress = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                serviceInfo.hostAddresses[0]
                            } else {
                                @Suppress("DEPRECATION")
                                serviceInfo.host
                            }

                            lanLinkProvider.sendUdpIdentityPacket(listOf(host), null)
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e("MdnsDiscovery", "service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("MdnsDiscovery", "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("MdnsDiscovery", "Discovery failed: Error code: $errorCode")
                stopDiscovering()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("MdnsDiscovery", "Discovery failed: Error code: $errorCode")
                stopDiscovering()
            }
        }

        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovering() {
        discoveryListener?.let {
            nsdManager?.stopServiceDiscovery(it)
            discoveryListener = null
        }
    }

    fun startAnnouncing() {
        if (registrationListener != null) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "cosmicconnect-" + deviceHelper.getDeviceId()
            serviceType = SERVICE_TYPE
            port = lanLinkProvider.tcpPort
            // We can't use setAttribute because it's only available since API 21,
            // and we support API 16. Also, NsdManager is quite buggy.
            // Better keep it simple and just use the name to identify the device.
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d("MdnsDiscovery", "Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("MdnsDiscovery", "Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d("MdnsDiscovery", "Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("MdnsDiscovery", "Unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun stopAnnouncing() {
        registrationListener?.let {
            nsdManager?.unregisterService(it)
            registrationListener = null
        }
    }

    companion object {
        const val SERVICE_TYPE = "_cosmicconnect._tcp."
    }
}
