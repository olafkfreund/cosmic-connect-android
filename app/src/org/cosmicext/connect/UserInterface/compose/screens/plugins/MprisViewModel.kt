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
import org.cosmicext.connect.Plugins.MprisPlugin.MprisPlugin
import org.cosmicext.connect.Plugins.SystemVolumePlugin.SystemVolumePlugin
import org.cosmicext.connect.Plugins.SystemVolumePlugin.Sink
import javax.inject.Inject

data class MprisUiState(
    val players: List<String> = emptyList(),
    val selectedPlayer: String? = null,
    val playerStatus: MprisPlugin.MprisPlayer? = null,
    val sinks: List<Sink> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class MprisViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(MprisUiState())
    val uiState: StateFlow<MprisUiState> = _uiState.asStateFlow()

    private var mprisPlugin: MprisPlugin? = null
    private var volumePlugin: SystemVolumePlugin? = null
    private var deviceId: String? = null

    fun loadDevice(deviceId: String?) {
        this.deviceId = deviceId
        mprisPlugin = deviceRegistry.getDevicePlugin(deviceId, MprisPlugin::class.java)
        volumePlugin = deviceRegistry.getDevicePlugin(deviceId, SystemVolumePlugin::class.java)

        mprisPlugin?.let { plugin ->
            plugin.setPlayerListUpdatedHandler("compose") {
                viewModelScope.launch {
                    val players = plugin.playerList
                    var selected = _uiState.value.selectedPlayer
                    if (selected == null || selected !in players) {
                        selected = plugin.playingPlayer?.playerName ?: players.firstOrNull()
                    }
                    _uiState.value = _uiState.value.copy(
                        players = players,
                        selectedPlayer = selected,
                        playerStatus = plugin.getPlayerStatus(selected)
                    )
                }
            }
            plugin.setPlayerStatusUpdatedHandler("compose") {
                viewModelScope.launch {
                    val selected = _uiState.value.selectedPlayer
                    _uiState.value = _uiState.value.copy(
                        playerStatus = plugin.getPlayerStatus(selected)
                    )
                }
            }
        }

        volumePlugin?.let { plugin ->
            plugin.addSinkListener(object : SystemVolumePlugin.SinkListener {
                override fun sinksChanged() {
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(sinks = plugin.sinks.values.toList())
                    }
                }
            })
            plugin.requestSinkList()
        }
    }

    fun selectPlayer(name: String) {
        val plugin = mprisPlugin ?: return
        _uiState.value = _uiState.value.copy(
            selectedPlayer = name,
            playerStatus = plugin.getPlayerStatus(name)
        )
    }

    fun sendAction(action: (MprisPlugin.MprisPlayer) -> Unit) {
        val player = _uiState.value.playerStatus ?: return
        action(player)
    }

    fun setVolume(sinkName: String, volume: Int) {
        volumePlugin?.sendVolume(sinkName, volume)
    }

    override fun onCleared() {
        mprisPlugin?.removePlayerListUpdatedHandler("compose")
        mprisPlugin?.removePlayerStatusUpdatedHandler("compose")
        // Volume plugin doesn't have a simple remove listener yet based on snippet
        super.onCleared()
    }
}
