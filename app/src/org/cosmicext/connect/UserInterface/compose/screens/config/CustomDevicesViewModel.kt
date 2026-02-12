/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.DeviceHost
import org.cosmicext.connect.Helpers.PreferenceDataStore
import org.cosmicext.connect.UserInterface.CustomDevicesActivity
import javax.inject.Inject

data class CustomDevicesUiState(
    val devices: List<DeviceHost> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CustomDevicesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomDevicesUiState())
    val uiState: StateFlow<CustomDevicesUiState> = _uiState.asStateFlow()

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val devices = CustomDevicesActivity.getCustomDeviceList(context)
            _uiState.value = CustomDevicesUiState(devices = devices, isLoading = false)
            
            // Check reachability for each device
            devices.forEach { host ->
                host.checkReachable {
                    viewModelScope.launch {
                        // Refresh the list to update reachability status
                        _uiState.value = _uiState.value.copy(devices = devices.toList())
                    }
                }
            }
        }
    }

    fun addDevice(hostnameOrIp: String): Boolean {
        val host = DeviceHost.toDeviceHostOrNull(hostnameOrIp) ?: return false
        val currentDevices = _uiState.value.devices.toMutableList()
        
        if (currentDevices.any { it.toString() == host.toString() }) return false
        
        currentDevices.add(host)
        currentDevices.sortBy { it.toString() }
        saveDevices(currentDevices)
        
        host.checkReachable {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(devices = currentDevices.toList())
            }
        }
        
        return true
    }

    fun removeDevice(host: DeviceHost) {
        val currentDevices = _uiState.value.devices.toMutableList()
        currentDevices.remove(host)
        saveDevices(currentDevices)
    }

    fun updateDevice(oldHost: DeviceHost, newHostnameOrIp: String): Boolean {
        val newHost = DeviceHost.toDeviceHostOrNull(newHostnameOrIp) ?: return false
        val currentDevices = _uiState.value.devices.toMutableList()
        val index = currentDevices.indexOf(oldHost)
        
        if (index == -1) return false
        if (currentDevices.any { it.toString() == newHost.toString() && it != oldHost }) return false
        
        currentDevices[index] = newHost
        currentDevices.sortBy { it.toString() }
        saveDevices(currentDevices)
        
        newHost.checkReachable {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(devices = currentDevices.toList())
            }
        }
        
        return true
    }

    private fun saveDevices(devices: List<DeviceHost>) {
        _uiState.value = _uiState.value.copy(devices = devices)
        val serialized = devices.joinToString(",")
        viewModelScope.launch {
            PreferenceDataStore.setCustomDeviceListSync(context, serialized)
        }
    }
}
