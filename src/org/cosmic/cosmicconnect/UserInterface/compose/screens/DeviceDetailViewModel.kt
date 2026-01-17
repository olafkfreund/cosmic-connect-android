package org.cosmic.cosmicconnect.UserInterface.compose.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Plugins.Plugin

/**
 * Device Detail ViewModel
 *
 * Manages the state and business logic for the Device Detail screen.
 * Observes device state changes and plugin updates.
 */
class DeviceDetailViewModel(
  application: Application,
  private val deviceId: String
) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow<DeviceDetailUiState>(DeviceDetailUiState.Loading)
  val uiState: StateFlow<DeviceDetailUiState> = _uiState.asStateFlow()

  private var device: Device? = null

  init {
    loadDevice()
  }

  /**
   * Load device and start observing changes.
   */
  private fun loadDevice() {
    device = CosmicConnect.getInstance().getDevice(deviceId)

    if (device == null) {
      _uiState.value = DeviceDetailUiState.Error("Device not found")
      return
    }

    // Observe device changes
    device?.addPairingCallback(deviceId) { pairingHandler ->
      viewModelScope.launch {
        updateDeviceState()
      }
    }

    device?.addPluginsChangedListener {
      viewModelScope.launch {
        updateDeviceState()
      }
    }

    // Initial state
    updateDeviceState()
  }

  override fun onCleared() {
    super.onCleared()
    device?.removePairingCallback(deviceId)
    device?.removePluginsChangedListener()
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
          isPairRequested = dev.isPairRequested,
          isPairRequestedByPeer = dev.isPairRequestedByPeer
        )
      }
      else -> {
        val plugins = dev.loadedPlugins.values.map { plugin ->
          PluginInfo(
            key = plugin.pluginKey,
            name = plugin.displayName,
            description = plugin.description ?: "",
            icon = plugin.actionIcon ?: 0,
            isEnabled = plugin.isPluginEnabled,
            isAvailable = plugin.checkRequiredPermissions(),
            hasSettings = plugin.hasSettings(),
            hasMainActivity = plugin.hasMainActivity()
          )
        }.sortedBy { it.name }

        _uiState.value = DeviceDetailUiState.Paired(
          deviceName = dev.name,
          deviceType = dev.deviceType.toString(),
          isReachable = dev.isReachable,
          batteryLevel = dev.batteryLevel.takeIf { it >= 0 },
          isCharging = dev.isCharging,
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
    val plugin = device?.getPlugin(pluginKey)
    plugin?.setPluginEnabled(enabled)
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
    val plugin = device?.getPlugin(pluginKey) ?: return false
    return plugin.hasMainActivity()
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
