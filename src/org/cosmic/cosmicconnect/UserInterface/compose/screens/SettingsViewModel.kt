package org.cosmic.cosmicconnect.UserInterface.compose.screens

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.BuildConfig
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
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
  private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

  init {
    loadSettings()

    // Listen for preference changes
    preferences.registerOnSharedPreferenceChangeListener { _, key ->
      when (key) {
        DeviceHelper.KEY_DEVICE_NAME_PREFERENCE,
        SettingsFragment.KEY_APP_THEME,
        SettingsFragment.KEY_BLUETOOTH_ENABLED -> {
          viewModelScope.launch {
            loadSettings()
          }
        }
      }
    }
  }

  /**
   * Load current settings from preferences.
   */
  private fun loadSettings() {
    val deviceName = DeviceHelper.getDeviceName(context)
    val theme = preferences.getString(SettingsFragment.KEY_APP_THEME, ThemeUtil.DEFAULT_MODE) ?: ThemeUtil.DEFAULT_MODE
    val bluetoothEnabled = preferences.getBoolean(SettingsFragment.KEY_BLUETOOTH_ENABLED, false)
    val persistentNotificationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      null // Handled by system settings on Android O+
    } else {
      NotificationHelper.isPersistentNotificationEnabled(context)
    }
    val customDevicesCount = CustomDevicesActivity.getCustomDeviceList(context).size

    _uiState.value = SettingsUiState(
      deviceName = deviceName,
      theme = theme,
      bluetoothEnabled = bluetoothEnabled,
      persistentNotificationEnabled = persistentNotificationEnabled,
      customDevicesCount = customDevicesCount,
      appVersion = BuildConfig.VERSION_NAME
    )
  }

  /**
   * Update device name.
   */
  fun updateDeviceName(newName: String) {
    if (newName.isBlank()) {
      return
    }

    val filteredName = DeviceHelper.filterInvalidCharactersFromDeviceName(newName)
    preferences.edit()
      .putString(DeviceHelper.KEY_DEVICE_NAME_PREFERENCE, filteredName)
      .apply()
  }

  /**
   * Update theme preference.
   */
  fun updateTheme(theme: String) {
    preferences.edit()
      .putString(SettingsFragment.KEY_APP_THEME, theme)
      .apply()

    ThemeUtil.applyTheme(theme)
  }

  /**
   * Toggle bluetooth support.
   */
  fun toggleBluetooth(enabled: Boolean) {
    preferences.edit()
      .putBoolean(SettingsFragment.KEY_BLUETOOTH_ENABLED, enabled)
      .apply()
  }

  /**
   * Toggle persistent notification (Android < O).
   */
  fun togglePersistentNotification(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      NotificationHelper.setPersistentNotificationEnabled(context, enabled)
      loadSettings()
    }
  }

  /**
   * Refresh custom devices count.
   */
  fun refreshCustomDevicesCount() {
    loadSettings()
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
