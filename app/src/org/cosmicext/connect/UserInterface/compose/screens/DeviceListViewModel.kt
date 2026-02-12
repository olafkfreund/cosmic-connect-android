/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.BackgroundService
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Device
import org.cosmicext.connect.Helpers.TrustedNetworkHelper
import javax.inject.Inject

/**
 * Device List ViewModel
 *
 * Manages the state and business logic for the Device List screen.
 * Observes device list changes from DeviceRegistry and provides UI state.
 */
@HiltViewModel
class DeviceListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

  private val _uiState = MutableStateFlow(DeviceListUiState())
  val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  init {
    // Start observing device list changes
    deviceRegistry.addDeviceListChangedCallback("DeviceListViewModel") {
      viewModelScope.launch {
        updateDeviceList()
      }
    }

    // Initial load
    viewModelScope.launch {
      updateDeviceList()
    }

    // Observe connectivity changes
    BackgroundService.instance?.isConnectedToNonCellularNetwork?.observeForever { isConnected ->
      viewModelScope.launch {
        updateConnectivityState(isConnected)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    deviceRegistry.removeDeviceListChangedCallback("DeviceListViewModel")
  }

  /**
   * Refresh the device list by forcing a network re-discovery.
   */
  fun refreshDevices() {
    viewModelScope.launch {
      _isRefreshing.value = true
      BackgroundService.ForceRefreshConnections(context)

      // Keep refreshing indicator visible for at least 1.5 seconds
      delay(1500)
      _isRefreshing.value = false
    }
  }

  /**
   * Unpair a device.
   */
  fun unpairDevice(device: Device) {
    device.unpair()
    // Device list will be updated automatically via callback
  }

  /**
   * Update the device list from DeviceRegistry.
   */
  private fun updateDeviceList() {
    val allDevices = deviceRegistry.devices.values.filter {
      // Filter out unpaired devices that are not reachable
      it.isReachable || it.isPaired
    }
    android.util.Log.d("DeviceListViewModel", "updateDeviceList: found ${allDevices.size} visible devices out of ${deviceRegistry.devices.size} total")
    deviceRegistry.devices.values.forEach {
        android.util.Log.d("DeviceListViewModel", "Device: ${it.name} (${it.deviceId}) reachable=${it.isReachable} paired=${it.isPaired}")
    }

    // Categorize devices
    val categorizedDevices = categorizeDevices(allDevices)

    // Check for duplicate names
    val seenNames = mutableSetOf<String>()
    val hasDuplicateNames = allDevices.any { device ->
      !seenNames.add(device.name)
    }

    _uiState.value = _uiState.value.copy(
      devices = categorizedDevices,
      hasDuplicateNames = hasDuplicateNames,
      isLoading = false,
      error = null
    )
  }

  /**
   * Update connectivity state.
   */
  private fun updateConnectivityState(isConnectedToNonCellular: Boolean?) {
    val hasNotificationsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    val devices = deviceRegistry.devices.values
    val someDevicesReachable = devices.any { it.isReachable }

    val connectivityState = when {
      !hasNotificationsPermission -> ConnectivityState.NO_NOTIFICATIONS
      isConnectedToNonCellular == false && !someDevicesReachable -> ConnectivityState.NO_WIFI
      !TrustedNetworkHelper.isTrustedNetwork(context) && !someDevicesReachable -> ConnectivityState.NOT_TRUSTED
      else -> ConnectivityState.OK
    }

    _uiState.value = _uiState.value.copy(
      connectivityState = connectivityState
    )
  }

  /**
   * Categorize devices into Connected, Available, and Remembered.
   */
  private fun categorizeDevices(devices: Collection<Device>): List<CategorizedDevice> {
    val result = mutableListOf<CategorizedDevice>()

    // Connected devices (reachable + paired)
    devices.filter { it.isReachable && it.isPaired }
      .forEach { device ->
        result.add(CategorizedDevice(device, DeviceCategory.CONNECTED))
      }

    // Available devices (reachable + not paired)
    devices.filter { it.isReachable && !it.isPaired }
      .forEach { device ->
        result.add(CategorizedDevice(device, DeviceCategory.AVAILABLE))
      }

    // Remembered devices (not reachable + paired)
    devices.filter { !it.isReachable && it.isPaired }
      .forEach { device ->
        result.add(CategorizedDevice(device, DeviceCategory.REMEMBERED))
      }

    return result
  }
}

/**
 * UI State for Device List Screen
 */
data class DeviceListUiState(
  val devices: List<CategorizedDevice> = emptyList(),
  val connectivityState: ConnectivityState = ConnectivityState.OK,
  val hasDuplicateNames: Boolean = false,
  val isDiscovering: Boolean = false,
  val isLoading: Boolean = true,
  val error: String? = null
)

/**
 * Device with its category.
 */
data class CategorizedDevice(
  val device: Device,
  val category: DeviceCategory
)

/**
 * Device categories for organization.
 */
enum class DeviceCategory {
  CONNECTED,    // Reachable and paired
  AVAILABLE,    // Reachable but not paired
  REMEMBERED    // Not reachable but paired
}

/**
 * Connectivity state for info headers.
 */
enum class ConnectivityState {
  OK,                 // Connected to network, everything good
  NO_WIFI,           // Not connected to Wi-Fi
  NO_NOTIFICATIONS,  // Notifications permission not granted
  NOT_TRUSTED        // On an untrusted network
}