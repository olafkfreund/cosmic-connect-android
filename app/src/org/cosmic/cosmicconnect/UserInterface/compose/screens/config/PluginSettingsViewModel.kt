/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.UserInterface.compose.getPluginIcon
import javax.inject.Inject

data class PluginSettingsUiState(
    val device: Device? = null,
    val supportedPlugins: List<PluginInfo> = emptyList(),
    val isLoading: Boolean = false
)

data class PluginInfo(
    val key: String,
    val name: String,
    val description: String,
    val icon: Int,
    val isEnabled: Boolean,
    val isAvailable: Boolean,
    val hasSettings: Boolean
)

@HiltViewModel
class PluginSettingsViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry,
    private val pluginFactory: PluginFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginSettingsUiState())
    val uiState: StateFlow<PluginSettingsUiState> = _uiState.asStateFlow()

    fun loadDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val device = deviceRegistry.getDevice(deviceId)
            if (device != null) {
                updatePluginList(device)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun updatePluginList(device: Device) {
        val pluginKeys = pluginFactory.sortPluginList(device.supportedPlugins)
        val pluginInfos = pluginKeys.map { key ->
            val info = pluginFactory.getPluginInfo(key)
            val plugin = device.getPluginIncludingWithoutPermissions(key)
            PluginInfo(
                key = key,
                name = info.displayName,
                description = info.description,
                icon = getPluginIcon(key),
                isEnabled = device.isPluginEnabled(key),
                isAvailable = plugin?.checkRequiredPermissions() ?: false,
                hasSettings = plugin?.hasSettings() ?: false
            )
        }
        _uiState.value = PluginSettingsUiState(
            device = device,
            supportedPlugins = pluginInfos,
            isLoading = false
        )
    }

    fun togglePlugin(pluginKey: String, enabled: Boolean) {
        val device = _uiState.value.device ?: return
        device.setPluginEnabled(pluginKey, enabled)
        updatePluginList(device)
    }
}
