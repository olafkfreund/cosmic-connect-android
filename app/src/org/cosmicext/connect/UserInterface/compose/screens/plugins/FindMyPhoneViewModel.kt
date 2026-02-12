/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import javax.inject.Inject

data class FindMyPhoneUiState(
    val deviceName: String = "",
    val isPlaying: Boolean = false
)

@HiltViewModel
class FindMyPhoneViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(FindMyPhoneUiState())
    val uiState: StateFlow<FindMyPhoneUiState> = _uiState.asStateFlow()

    private var plugin: FindMyPhonePlugin? = null

    fun loadDevice(deviceId: String?) {
        val device = deviceRegistry.getDevice(deviceId)
        plugin = deviceRegistry.getDevicePlugin(deviceId, FindMyPhonePlugin::class.java)
        _uiState.value = FindMyPhoneUiState(
            deviceName = device?.name ?: "Unknown Device",
            isPlaying = false // Plugin doesn't expose isPlaying state easily
        )
    }

    fun startPlaying() {
        plugin?.startPlaying()
        plugin?.hideNotification()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun stopPlaying() {
        plugin?.stopPlaying()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }
}
