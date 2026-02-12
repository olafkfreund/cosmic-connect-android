/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Device
import org.cosmicext.connect.PairingHandler
import org.cosmicext.connect.Plugins.PluginFactory
import javax.inject.Inject

/**
 * Device Detail ViewModel
 *
 * Manages the state and business logic for the Device Detail screen.
 * Observes device state changes and plugin updates.
 */
@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry,
    private val pluginFactory: PluginFactory,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

  private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])

  private val _uiState = MutableStateFlow<DeviceDetailUiState>(DeviceDetailUiState.Loading)
  val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

  private var device: Device? = null
  private var pairingCallback: PairingHandler.PairingCallback? = null
  private var pluginsChangedListener: Device.PluginsChangedListener? = null

  init {
    savedStateHandle.get<String>("deviceId")?.let { loadDevice(it) }
  }

  /**
   * Load device and start observing changes.
   */
  fun loadDevice(deviceId: String) {
    if (this.device?.deviceId == deviceId) return
    
    // Cleanup previous device
    pairingCallback?.let { device?.removePairingCallback(it) }
    pluginsChangedListener?.let { device?.removePluginsChangedListener(it) }

    device = deviceRegistry.getDevice(deviceId)

    if (device == null) {
      _uiState.value = DeviceDetailUiState.Error("Device not found")
      return
    }

    // Observe device changes
    pairingCallback = object : PairingHandler.PairingCallback {
      override fun incomingPairRequest() {
        viewModelScope.launch {
          updateDeviceState()
        }
      }

      override fun pairingSuccessful() {
        viewModelScope.launch {
          updateDeviceState()
        }
      }

      override fun pairingFailed(error: String) {
        viewModelScope.launch {
          updateDeviceState()
        }
      }

      override fun unpaired(device: Device) {
        viewModelScope.launch {
          updateDeviceState()
        }
      }
    }
    pairingCallback?.let { device?.addPairingCallback(it) }

    pluginsChangedListener = Device.PluginsChangedListener {
      viewModelScope.launch {
        updateDeviceState()
      }
    }
    pluginsChangedListener?.let { device?.addPluginsChangedListener(it) }

    // Initial state
    updateDeviceState()
  }

  override fun onCleared() {
    super.onCleared()
    pairingCallback?.let { device?.removePairingCallback(it) }
    pluginsChangedListener?.let { device?.removePluginsChangedListener(it) }
  }

  /**
   * Update device state from current device.
   */
  private fun updateDeviceState() {
    val dev = device ?: run {
      _uiState.value = DeviceDetailUiState.Error("Device not found")
      return
    }

    when {
      !dev.isPaired -> {
        _uiState.value = DeviceDetailUiState.Unpaired(
          deviceName = dev.name,
          deviceType = dev.deviceType.toString(),
          isReachable = dev.isReachable,
          isPairRequested = dev.pairStatus == PairingHandler.PairState.Requested,
          isPairRequestedByPeer = dev.pairStatus == PairingHandler.PairState.RequestedByPeer
        )
      }
      else -> {
        // Show all supported plugins, not just loaded ones
        // This ensures disabled plugins can still be re-enabled from settings
        val plugins = dev.supportedPlugins.mapNotNull { pluginKey ->
          val loadedPlugin = dev.loadedPlugins[pluginKey]
          val factoryInfo = pluginFactory.getPluginInfo(pluginKey)

          PluginInfo(
            key = pluginKey,
            name = factoryInfo.displayName,
            description = factoryInfo.description,
            icon = org.cosmicext.connect.UserInterface.compose.getPluginIcon(pluginKey),
            isEnabled = dev.isPluginEnabled(pluginKey),
            isAvailable = loadedPlugin?.checkRequiredPermissions() ?: true,
            hasSettings = factoryInfo.hasSettings,
            hasMainActivity = loadedPlugin?.getUiButtons()?.isNotEmpty() ?: false
          )
        }.sortedBy { it.name }

        _uiState.value = DeviceDetailUiState.Paired(
          deviceName = dev.name,
          deviceType = dev.deviceType.toString(),
          isReachable = dev.isReachable,
          batteryLevel = null, // TODO: Battery level comes from battery plugin
          isCharging = false, // TODO: Charging status comes from battery plugin
          plugins = plugins
        )
      }
    }
  }

  /**
   * Request pairing with the device.
   */
  fun requestPair() {
    device?.requestPairing()
  }

  /**
   * Accept incoming pairing request.
   */
  fun acceptPairing() {
    device?.acceptPairing()
  }

  /**
   * Reject/cancel pairing.
   */
  fun rejectPairing() {
    device?.cancelPairing()
  }

  /**
   * Unpair the device.
   */
  fun unpairDevice() {
    device?.unpair()
  }

  /**
   * Refresh plugin state.
   * Call this when returning from settings to update permission status.
   */
  fun refresh() {
    updateDeviceState()
  }

  /**
   * Rename the device.
   */
  fun renameDevice(newName: String) {
    // Device renaming would need to be implemented in Device class
    // For now, this is a placeholder
  }

  /**
   * Toggle plugin enabled state.
   */
  fun togglePlugin(pluginKey: String, enabled: Boolean) {
    device?.setPluginEnabled(pluginKey, enabled)
    updateDeviceState()
  }

  /**
   * Open plugin settings.
   */
  fun openPluginSettings(pluginKey: String): Boolean {
    val plugin = device?.getPlugin(pluginKey) ?: return false
    return plugin.hasSettings()
  }

  /**
   * Start plugin activity.
   */
  fun startPluginActivity(pluginKey: String): Boolean {
    // TODO: hasMainActivity needs to be implemented on Plugin class
    return false
  }
}

/**
 * UI State for Device Detail Screen
 */
sealed class DeviceDetailUiState {
  /**
   * Loading state while fetching device.
   */
  data object Loading : DeviceDetailUiState()

  /**
   * Error state when device not found or error occurred.
   */
  data class Error(val message: String) : DeviceDetailUiState()

  /**
   * Unpaired device state showing pairing UI.
   */
  data class Unpaired(
    val deviceName: String,
    val deviceType: String,
    val isReachable: Boolean,
    val isPairRequested: Boolean,
    val isPairRequestedByPeer: Boolean
  ) : DeviceDetailUiState()

  /**
   * Paired device state showing full device details and plugins.
   */
  data class Paired(
    val deviceName: String,
    val deviceType: String,
    val isReachable: Boolean,
    val batteryLevel: Int?,
    val isCharging: Boolean,
    val plugins: List<PluginInfo>
  ) : DeviceDetailUiState()
}

/**
 * Plugin information for display.
 */
data class PluginInfo(
  val key: String,
  val name: String,
  val description: String,
  val icon: Int,
  val isEnabled: Boolean,
  val isAvailable: Boolean,
  val hasSettings: Boolean,
  val hasMainActivity: Boolean
)