/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Plugins.AudioStreamPlugin.AudioStreamPlugin
import javax.inject.Inject

data class AudioStreamUiState(
    val isStreaming: Boolean = false,
    val codec: String? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val direction: String? = null,
    val supportedCodecs: List<String> = emptyList(),
    val supportedSampleRates: List<Int> = emptyList(),
    val maxChannels: Int? = null,
    val isLoaded: Boolean = false
)

@HiltViewModel
class AudioStreamViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioStreamUiState())
    val uiState: StateFlow<AudioStreamUiState> = _uiState.asStateFlow()

    private var plugin: AudioStreamPlugin? = null
    private var listener: AudioStreamPlugin.StreamStateListener? = null

    fun loadDevice(deviceId: String?) {
        plugin = deviceRegistry.getDevicePlugin(deviceId, AudioStreamPlugin::class.java)
        updateState()

        listener = AudioStreamPlugin.StreamStateListener {
            viewModelScope.launch {
                updateState()
            }
        }
        plugin?.addStreamStateListener(listener!!)
    }

    private fun updateState() {
        val p = plugin
        _uiState.value = if (p != null) {
            AudioStreamUiState(
                isStreaming = p.isStreaming,
                codec = p.activeCodec,
                sampleRate = p.sampleRate,
                channels = p.channels,
                direction = p.direction,
                supportedCodecs = p.supportedCodecs,
                supportedSampleRates = p.supportedSampleRates,
                maxChannels = p.maxChannels,
                isLoaded = true
            )
        } else {
            AudioStreamUiState(isLoaded = true)
        }
    }

    fun startStream(codec: String, sampleRate: Int, channels: Int) {
        plugin?.sendStreamCommand(
            start = true,
            codec = codec,
            sampleRate = sampleRate,
            channels = channels,
            direction = "receive"
        )
    }

    fun stopStream() {
        plugin?.sendStreamCommand(start = false)
    }

    override fun onCleared() {
        listener?.let { plugin?.removeStreamStateListener(it) }
        super.onCleared()
    }
}
