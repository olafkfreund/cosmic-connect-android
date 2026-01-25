package org.cosmic.cosmicconnect.UserInterface.compose.screens

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.BuildConfig
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.Helpers.PreferenceDataStore
import org.cosmic.cosmicconnect.UserInterface.CustomDevicesActivity
import org.cosmic.cosmicconnect.UserInterface.SettingsFragment
import org.cosmic.cosmicconnect.UserInterface.ThemeUtil

/**
 * Settings ViewModel
 *
 * Manages the state and business logic for the Settings screen.
 * Handles preference changes and provides current settings values.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

  private val context: Context = application.applicationContext

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    // Observe DataStore flows
    combine(
      PreferenceDataStore.getDeviceName(context),
      PreferenceDataStore.getTheme(context),
      PreferenceDataStore.isBluetoothEnabled(context)
    ) { deviceName, theme, bluetoothEnabled ->
      Triple(deviceName, theme, bluetoothEnabled)
    }.onEach { (deviceName, theme, bluetoothEnabled) ->
      val persistentNotificationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        null
      } else {
        NotificationHelper.isPersistentNotificationEnabled(context)
      }
      val customDevicesCount = CustomDevicesActivity.getCustomDeviceList(context).size

      _uiState.update {
        it.copy(
          deviceName = deviceName,
          theme = theme,
          bluetoothEnabled = bluetoothEnabled,
          persistentNotificationEnabled = persistentNotificationEnabled,
          customDevicesCount = customDevicesCount,
          appVersion = BuildConfig.VERSION_NAME
        )
      }
    }.launchIn(viewModelScope)
  }

  /**
   * Update device name.
   */
  fun updateDeviceName(newName: String) {
    if (newName.isBlank()) {
      return
    }

    val filteredName = DeviceHelper.filterInvalidCharactersFromDeviceName(newName)
    viewModelScope.launch {
      PreferenceDataStore.setDeviceName(context, filteredName)
    }
  }

  /**
   * Update theme preference.
   */
  fun updateTheme(theme: String) {
    viewModelScope.launch {
      PreferenceDataStore.setTheme(context, theme)
      ThemeUtil.applyTheme(theme)
    }
  }

  /**
   * Toggle bluetooth support.
   */
  fun toggleBluetooth(enabled: Boolean) {
    viewModelScope.launch {
      PreferenceDataStore.setBluetoothEnabled(context, enabled)
    }
  }

  /**
   * Toggle persistent notification (Android < O).
   */
  fun togglePersistentNotification(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      NotificationHelper.setPersistentNotificationEnabled(context, enabled)
      // UI state will be updated via custom logic if needed, 
      // or we could add persistent notification to DataStore too
    }
  }

  /**
   * Refresh custom devices count.
   */
  fun refreshCustomDevicesCount() {
    val count = CustomDevicesActivity.getCustomDeviceList(context).size
    _uiState.update { it.copy(customDevicesCount = count) }
  }
}

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
  val deviceName: String = "",
  val theme: String = ThemeUtil.DEFAULT_MODE,
  val bluetoothEnabled: Boolean = false,
  val persistentNotificationEnabled: Boolean? = null, // null on Android O+ (system handles it)
  val customDevicesCount: Int = 0,
  val appVersion: String = ""
)
