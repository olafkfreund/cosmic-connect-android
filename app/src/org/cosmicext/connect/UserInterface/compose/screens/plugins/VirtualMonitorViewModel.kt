/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Plugins.ScreenSharePlugin.ui.ScreenShareViewerActivity
import org.cosmicext.connect.Plugins.VirtualMonitorPlugin.VirtualMonitorPlugin
import javax.inject.Inject

data class VirtualMonitorUiState(
    val isActive: Boolean = false,
    val width: Int? = null,
    val height: Int? = null,
    val dpi: Int? = null,
    val position: String? = null,
    val refreshRate: Int? = null,
    val hasActiveSession: Boolean = false,
    val isLoaded: Boolean = false,
    // Configuration form defaults
    val configWidth: String = "1920",
    val configHeight: String = "1080",
    val configDpi: String = "160",
    val configPosition: String = "right",
    val configRefreshRate: String = "60"
)

@HiltViewModel
class VirtualMonitorViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VirtualMonitorUiState())
    val uiState: StateFlow<VirtualMonitorUiState> = _uiState.asStateFlow()

    private var plugin: VirtualMonitorPlugin? = null
    private var deviceId: String? = null
    private var listener: VirtualMonitorPlugin.VirtualMonitorStateListener? = null

    fun loadDevice(deviceId: String?) {
        this.deviceId = deviceId
        plugin = deviceRegistry.getDevicePlugin(deviceId, VirtualMonitorPlugin::class.java)

        listener = object : VirtualMonitorPlugin.VirtualMonitorStateListener {
            override fun onVirtualMonitorStateChanged(
                isActive: Boolean?,
                width: Int?,
                height: Int?,
                dpi: Int?,
                position: String?,
                refreshRate: Int?
            ) {
                viewModelScope.launch { updateState() }
            }
        }
        plugin?.addVirtualMonitorStateListener(listener!!)

        updateState()
    }

    private fun updateState() {
        val p = plugin
        if (p != null) {
            _uiState.value = _uiState.value.copy(
                isActive = p.isActive == true,
                width = p.width,
                height = p.height,
                dpi = p.dpi,
                position = p.position,
                refreshRate = p.refreshRate,
                hasActiveSession = p.activeStreamSession != null,
                isLoaded = true
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoaded = true)
        }
    }

    fun updateConfigWidth(value: String) {
        _uiState.value = _uiState.value.copy(configWidth = value)
    }

    fun updateConfigHeight(value: String) {
        _uiState.value = _uiState.value.copy(configHeight = value)
    }

    fun updateConfigDpi(value: String) {
        _uiState.value = _uiState.value.copy(configDpi = value)
    }

    fun updateConfigPosition(value: String) {
        _uiState.value = _uiState.value.copy(configPosition = value)
    }

    fun updateConfigRefreshRate(value: String) {
        _uiState.value = _uiState.value.copy(configRefreshRate = value)
    }

    fun enableMonitor() {
        val state = _uiState.value
        val width = state.configWidth.toIntOrNull() ?: return
        val height = state.configHeight.toIntOrNull() ?: return
        val dpi = state.configDpi.toIntOrNull() ?: return
        val refreshRate = state.configRefreshRate.toIntOrNull() ?: return
        val position = state.configPosition

        plugin?.enableMonitor(width, height, dpi, position, refreshRate)
    }

    fun disableMonitor() {
        plugin?.disableMonitor()
    }

    fun launchViewer(context: Context) {
        val devId = deviceId ?: return
        val intent = Intent(context, ScreenShareViewerActivity::class.java).apply {
            putExtra(ScreenShareViewerActivity.EXTRA_DEVICE_ID, devId)
            putExtra(ScreenShareViewerActivity.EXTRA_MODE, ScreenShareViewerActivity.MODE_VIRTUALMONITOR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun onCleared() {
        listener?.let { plugin?.removeVirtualMonitorStateListener(it) }
        super.onCleared()
    }
}
