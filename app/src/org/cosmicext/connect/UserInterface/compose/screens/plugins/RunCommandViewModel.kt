/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Plugins.RunCommandPlugin.CommandEntry
import org.cosmicext.connect.Plugins.RunCommandPlugin.RunCommandPlugin
import org.json.JSONException
import javax.inject.Inject

data class RunCommandUiState(
    val deviceName: String = "",
    val commands: List<CommandEntry> = emptyList(),
    val canAddCommand: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class RunCommandViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunCommandUiState())
    val uiState: StateFlow<RunCommandUiState> = _uiState.asStateFlow()

    private var plugin: RunCommandPlugin? = null
    private var deviceId: String? = null

    private val commandsChangedCallback = object : RunCommandPlugin.CommandsChangedCallback {
        override fun update() {
            updateCommands()
        }
    }

    fun loadDevice(deviceId: String?) {
        this.deviceId = deviceId
        val device = deviceRegistry.getDevice(deviceId)
        plugin = deviceRegistry.getDevicePlugin(deviceId, RunCommandPlugin::class.java)
        
        _uiState.value = _uiState.value.copy(
            deviceName = device?.name ?: "Unknown Device",
            canAddCommand = plugin?.canAddCommand() ?: false
        )
        
        plugin?.addCommandsUpdatedCallback(commandsChangedCallback)
        updateCommands()
    }

    private fun updateCommands() {
        val plugin = this.plugin ?: return
        val commandEntries = mutableListOf<CommandEntry>()
        for (obj in plugin.commandList) {
            try {
                commandEntries.add(CommandEntry(obj))
            } catch (e: JSONException) {
                // Log error
            }
        }
        commandEntries.sortBy { it.name.lowercase() }
        _uiState.value = _uiState.value.copy(commands = commandEntries)
    }

    fun runCommand(key: String) {
        plugin?.runCommand(key)
    }

    fun sendSetupPacket() {
        plugin?.sendSetupPacket()
    }

    override fun onCleared() {
        plugin?.removeCommandsUpdatedCallback(commandsChangedCallback)
        super.onCleared()
    }
}
