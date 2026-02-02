/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages mDNS/NSD service discovery for Extended Display servers.
 *
 * Discovers services of type "_cosmic-display._tcp" on the local network.
 */
class ServiceDiscovery(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscovering = false

    private val discoveredServicesMap = mutableMapOf<String, DiscoveredService>()

    private val _discoveredServices = MutableStateFlow<List<DiscoveredService>>(emptyList())
    val discoveredServices: StateFlow<List<DiscoveredService>> = _discoveredServices.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Starts mDNS service discovery.
     */
    fun startDiscovery() {
        if (isDiscovering) {
            Log.d(TAG, "Discovery already active, skipping start")
            return
        }

        Log.d(TAG, "Starting service discovery for $SERVICE_TYPE")

        discoveryListener = object : NsdManager.DiscoveryListener {

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
                _error.value = "Failed to start discovery (error $errorCode)"
                isDiscovering = false
                _isActive.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
                _error.value = "Failed to stop discovery (error $errorCode)"
                isDiscovering = false
                _isActive.value = false
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "Discovery started for $serviceType")
                isDiscovering = true
                _isActive.value = true
                _error.value = null
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "Discovery stopped for $serviceType")
                isDiscovering = false
                _isActive.value = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == SERVICE_TYPE) {
                    resolveService(serviceInfo)
                } else {
                    Log.d(TAG, "Ignoring service with type ${serviceInfo.serviceType}")
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                removeService(serviceInfo.serviceName)
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start discovery", e)
            _error.value = "Failed to start discovery: ${e.message}"
            isDiscovering = false
            _isActive.value = false
        }
    }

    /**
     * Stops mDNS service discovery.
     */
    fun stopDiscovery() {
        if (!isDiscovering) {
            Log.d(TAG, "Discovery not active, skipping stop")
            return
        }

        Log.d(TAG, "Stopping service discovery")

        discoveryListener?.let { listener ->
            try {
                nsdManager.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop discovery", e)
                _error.value = "Failed to stop discovery: ${e.message}"
                isDiscovering = false
                _isActive.value = false
            }
        }

        discoveryListener = null
    }

    /**
     * Clears all discovered services.
     */
    fun clearServices() {
        discoveredServicesMap.clear()
        _discoveredServices.value = emptyList()
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        Log.d(TAG, "Resolving service: ${serviceInfo.serviceName}")

        val resolveListener = object : NsdManager.ResolveListener {

            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo?.serviceName}: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: ${serviceInfo.serviceName}")

                val host = serviceInfo.host?.hostAddress ?: run {
                    Log.w(TAG, "Service has no host address, skipping")
                    return
                }

                val port = serviceInfo.port.takeIf { it > 0 } ?: run {
                    Log.w(TAG, "Service has invalid port, skipping")
                    return
                }

                val attributes = extractAttributes(serviceInfo)

                val service = DiscoveredService(
                    name = serviceInfo.serviceName,
                    host = host,
                    port = port,
                    attributes = attributes
                )

                addService(service)
            }
        }

        try {
            nsdManager.resolveService(serviceInfo, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve service", e)
        }
    }

    private fun extractAttributes(serviceInfo: NsdServiceInfo): Map<String, String> {
        val attributes = mutableMapOf<String, String>()

        try {
            serviceInfo.attributes?.forEach { (key, value) ->
                val stringValue = value?.let { String(it, Charsets.UTF_8) } ?: ""
                attributes[key] = stringValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract attributes", e)
        }

        return attributes
    }

    private fun addService(service: DiscoveredService) {
        if (!service.isValid) {
            Log.w(TAG, "Skipping invalid service: $service")
            return
        }

        discoveredServicesMap[service.id] = service
        _discoveredServices.value = discoveredServicesMap.values.toList()

        Log.d(TAG, "Added service: ${service.displayName}")
    }

    private fun removeService(serviceName: String) {
        val toRemove = discoveredServicesMap.filter { it.value.name == serviceName }
        toRemove.keys.forEach { discoveredServicesMap.remove(it) }

        if (toRemove.isNotEmpty()) {
            _discoveredServices.value = discoveredServicesMap.values.toList()
            Log.d(TAG, "Removed service: $serviceName")
        }
    }

    companion object {
        private const val TAG = "ServiceDiscovery"

        /**
         * mDNS service type for COSMIC Display servers.
         */
        const val SERVICE_TYPE = "_cosmic-display._tcp."
    }
}
