/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.MousePadPlugin.MousePadPlugin
import javax.inject.Inject

data class SendKeystrokesUiState(
    val devices: List<Device> = emptyList(),
    val textToSend: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class SendKeystrokesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(SendKeystrokesUiState())
    val uiState: StateFlow<SendKeystrokesUiState> = _uiState.asStateFlow()

    fun loadDevices(text: String?) {
        val devices = deviceRegistry.devices.values.filter { it.isReachable && it.isPaired }
        _uiState.value = SendKeystrokesUiState(
            devices = devices,
            textToSend = text ?: "",
            isLoading = false
        )
    }

    fun sendKeys(device: Device, text: String): Boolean {
        if (text.isEmpty()) return false
        val plugin = deviceRegistry.getDevicePlugin(device.deviceId, MousePadPlugin::class.java) ?: return false
        plugin.sendText(text)
        return true
    }
}
