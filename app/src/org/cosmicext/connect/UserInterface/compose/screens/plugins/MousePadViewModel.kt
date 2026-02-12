/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Plugins.MousePadPlugin.MousePadPlugin
import javax.inject.Inject

data class MousePadUiState(
    val deviceName: String = "",
    val isKeyboardEnabled: Boolean = false,
    val mouseButtonsEnabled: Boolean = true
)

@HiltViewModel
class MousePadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(MousePadUiState())
    val uiState: StateFlow<MousePadUiState> = _uiState.asStateFlow()

    private var plugin: MousePadPlugin? = null

    fun loadDevice(deviceId: String?) {
        val device = deviceRegistry.getDevice(deviceId)
        plugin = deviceRegistry.getDevicePlugin(deviceId, MousePadPlugin::class.java)
        
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val mouseButtons = prefs.getBoolean("mousepad_mouse_buttons_enabled_pref", true)

        _uiState.value = MousePadUiState(
            deviceName = device?.name ?: "Unknown Device",
            isKeyboardEnabled = plugin?.isKeyboardEnabled ?: false,
            mouseButtonsEnabled = mouseButtons
        )
    }

    fun sendLeftClick() { plugin?.sendLeftClick() }
    fun sendMiddleClick() { plugin?.sendMiddleClick() }
    fun sendRightClick() { plugin?.sendRightClick() }
    fun sendMouseDelta(x: Float, y: Float) { plugin?.sendMouseDelta(x, y) }
    fun sendScroll(x: Float, y: Float) { plugin?.sendScroll(x, y) }
    fun sendSingleHold() { plugin?.sendSingleHold() }
    fun sendSingleRelease() { plugin?.sendSingleRelease() }
    fun sendDoubleClick() { plugin?.sendDoubleClick() }
}
