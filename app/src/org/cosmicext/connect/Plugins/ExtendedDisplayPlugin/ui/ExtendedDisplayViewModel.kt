/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ConnectionState
import javax.inject.Inject

/**
 * ViewModel for Extended Display feature.
 * Manages connection state, display configuration, and debug information.
 */
@HiltViewModel
class ExtendedDisplayViewModel @Inject constructor() : ViewModel() {

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Display configuration
    private val _displayConfig = MutableStateFlow(DisplayConfig())
    val displayConfig: StateFlow<DisplayConfig> = _displayConfig.asStateFlow()

    // Debug information
    private val _debugInfo = MutableStateFlow(DebugInfo())
    val debugInfo: StateFlow<DebugInfo> = _debugInfo.asStateFlow()

    // Server connection details
    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String> = _serverAddress.asStateFlow()

    private val _serverPort = MutableStateFlow(0)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    /**
     * Connect to the extended display server
     */
    fun connect(address: String, port: Int) {
        viewModelScope.launch {
            _serverAddress.value = address
            _serverPort.value = port
            _connectionState.value = ConnectionState.CONNECTING

            // TODO: Implement actual connection logic with WebRTC
        }
    }

    /**
     * Disconnect from the extended display server
     */
    fun disconnect() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTING

            // TODO: Implement actual disconnection logic
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Update debug information (FPS, latency, etc.)
     */
    fun updateDebugInfo(fps: Int, latency: Long, bitrate: Long) {
        viewModelScope.launch {
            _debugInfo.value = DebugInfo(
                fps = fps,
                latency = latency,
                bitrate = bitrate
            )
        }
    }

    /**
     * Update connection state
     */
    fun updateConnectionState(state: ConnectionState) {
        viewModelScope.launch {
            _connectionState.value = state
        }
    }

    /**
     * Update display configuration
     */
    fun updateDisplayConfig(config: DisplayConfig) {
        viewModelScope.launch {
            _displayConfig.value = config
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_connectionState.value != ConnectionState.DISCONNECTED) {
            disconnect()
        }
    }
}

/**
 * Display configuration data class
 */
data class DisplayConfig(
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 60,
    val bitrate: Long = 5_000_000 // 5 Mbps default
)

/**
 * Debug information data class
 */
data class DebugInfo(
    val fps: Int = 0,
    val latency: Long = 0,
    val bitrate: Long = 0
)
