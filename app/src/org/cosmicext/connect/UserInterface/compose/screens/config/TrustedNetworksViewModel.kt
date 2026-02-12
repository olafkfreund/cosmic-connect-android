/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.config

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Helpers.TrustedNetworkHelper
import javax.inject.Inject

data class TrustedNetworksUiState(
    val trustedNetworks: List<String> = emptyList(),
    val allNetworksAllowed: Boolean = false,
    val currentSsid: String? = null,
    val hasPermissions: Boolean = false
)

@HiltViewModel
class TrustedNetworksViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val helper = TrustedNetworkHelper(context)
    private val _uiState = MutableStateFlow(TrustedNetworksUiState())
    val uiState: StateFlow<TrustedNetworksUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        _uiState.value = TrustedNetworksUiState(
            trustedNetworks = helper.trustedNetworks.toList(),
            allNetworksAllowed = helper.allNetworksAllowed,
            currentSsid = helper.currentSSID,
            hasPermissions = helper.hasPermissions
        )
    }

    fun setAllNetworksAllowed(allowed: Boolean) {
        helper.allNetworksAllowed = allowed
        loadSettings()
    }

    fun addTrustedNetwork(ssid: String) {
        val current = helper.trustedNetworks.toMutableList()
        if (ssid !in current) {
            current.add(ssid)
            helper.trustedNetworks = current
            loadSettings()
        }
    }

    fun removeTrustedNetwork(ssid: String) {
        val current = helper.trustedNetworks.toMutableList()
        if (current.remove(ssid)) {
            helper.trustedNetworks = current
            loadSettings()
        }
    }
}
