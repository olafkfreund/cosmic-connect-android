/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Plugins.DigitizerPlugin.DigitizerPlugin
import org.cosmic.cosmicconnect.Plugins.DigitizerPlugin.ToolEvent
import javax.inject.Inject

data class DigitizerUiState(
    val deviceName: String = "",
    val isFullscreen: Boolean = false,
    val hideDrawButton: Boolean = false,
    val drawButtonSide: String = "bottom_left"
)

@HiltViewModel
class DigitizerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitizerUiState())
    val uiState: StateFlow<DigitizerUiState> = _uiState.asStateFlow()

    private var plugin: DigitizerPlugin? = null

    fun loadDevice(deviceId: String?) {
        val device = deviceRegistry.getDevice(deviceId)
        plugin = deviceRegistry.getDevicePlugin(deviceId, DigitizerPlugin::class.java)
        
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val hideDraw = prefs.getBoolean("digitizer_hide_draw_button", false)
        val drawSide = prefs.getString("digitizer_draw_button_side", "bottom_left") ?: "bottom_left"

        _uiState.value = DigitizerUiState(
            deviceName = device?.name ?: "Unknown Device",
            hideDrawButton = hideDraw,
            drawButtonSide = drawSide
        )
    }

    fun startSession(width: Int, height: Int, xdpi: Int, ydpi: Int) {
        plugin?.startSession(width, height, xdpi, ydpi)
    }

    fun endSession() {
        plugin?.endSession()
    }

    fun reportEvent(event: ToolEvent) {
        plugin?.reportEvent(event)
    }

    fun setFullscreen(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isFullscreen = enabled)
    }
}
