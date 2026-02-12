/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.cosmicext.connect.BuildConfig
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.NotificationHelper
import org.cosmicext.connect.Helpers.PreferenceDataStore
import org.cosmicext.connect.UserInterface.CustomDevicesActivity
import org.cosmicext.connect.UserInterface.ThemeUtil
import javax.inject.Inject

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

/**
 * Settings ViewModel
 *
 * Manages the state and business logic for the Settings screen.
 * Handles preference changes and provides current settings values.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

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