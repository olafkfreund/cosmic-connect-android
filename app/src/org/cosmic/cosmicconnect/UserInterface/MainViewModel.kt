/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Helpers.PreferenceDataStore
import javax.inject.Inject

data class MainUiState(
    val devices: List<Device> = emptyList(),
    val selectedDeviceId: String? = null,
    val myDeviceName: String = "",
    val myDeviceType: String = "phone",
    val currentRoute: String = "devices"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistry: DeviceRegistry,
    private val deviceHelper: DeviceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        deviceHelper.initializeDeviceId()
        
        _uiState.value = _uiState.value.copy(
            myDeviceName = deviceHelper.getDeviceName(),
            myDeviceType = deviceHelper.deviceType.toString()
        )

        deviceRegistry.addDeviceListChangedCallback("MainViewModel") {
            updateDeviceList()
        }
        updateDeviceList()
        
        viewModelScope.launch {
            val savedDevice = PreferenceDataStore.getSelectedDeviceSync(context)
            _uiState.value = _uiState.value.copy(selectedDeviceId = savedDevice)
        }
    }

    private fun updateDeviceList() {
        val devices = deviceRegistry.devices.values.toList()
        android.util.Log.d("MainViewModel", "updateDeviceList: found ${devices.size} devices")
        devices.forEach {
            android.util.Log.d("MainViewModel", "Device: ${it.name} (${it.deviceId}) paired=${it.isPaired}")
        }
        _uiState.value = _uiState.value.copy(devices = devices)
    }

    fun selectDevice(deviceId: String?) {
        _uiState.value = _uiState.value.copy(selectedDeviceId = deviceId)
        viewModelScope.launch {
            PreferenceDataStore.setSelectedDevice(context, deviceId)
        }
    }

    fun setRoute(route: String) {
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }

    fun refreshDeviceName() {
        _uiState.value = _uiState.value.copy(
            myDeviceName = deviceHelper.getDeviceName()
        )
    }

    override fun onCleared() {
        deviceRegistry.removeDeviceListChangedCallback("MainViewModel")
        super.onCleared()
    }
}
