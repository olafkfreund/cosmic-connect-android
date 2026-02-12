/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import android.content.Context
import android.content.Intent
import android.webkit.URLUtil
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
import org.cosmicext.connect.Plugins.SharePlugin.SharePlugin
import javax.inject.Inject

data class ShareUiState(
    val devices: List<Device> = emptyList(),
    val intentHasUrl: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class ShareViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun loadDevices(intent: Intent?) {
        val hasUrl = doesIntentContainUrl(intent)
        val devices = deviceRegistry.devices.values.toList()
        _uiState.value = ShareUiState(
            devices = devices,
            intentHasUrl = hasUrl,
            isLoading = false
        )
    }

    private fun doesIntentContainUrl(intent: Intent?): Boolean {
        if (intent != null) {
            val extras = intent.extras
            if (extras != null) {
                val url = extras.getString(Intent.EXTRA_TEXT)
                return url != null && (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url))
            }
        }
        return false
    }

    fun onDeviceClick(device: Device, intent: Intent, onFinish: () -> Unit) {
        val plugin = deviceRegistry.getDevicePlugin(device.deviceId, SharePlugin::class.java)
        if (_uiState.value.intentHasUrl && !device.isReachable) {
            // Handle unreachable URL store logic here or in plugin
            storeUrlForFutureDelivery(device, intent.getStringExtra(Intent.EXTRA_TEXT))
        } else if (plugin != null) {
            plugin.share(intent)
        }
        onFinish()
    }

    private fun storeUrlForFutureDelivery(device: Device, url: String?) {
        if (url == null) return
        val sharedPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val key = "key_unreachable_url_list" + device.deviceId
        val oldUrlSet = sharedPrefs.getStringSet(key, null)
        val newUrlSet = HashSet<String>()
        newUrlSet.add(url)
        if (oldUrlSet != null) {
            newUrlSet.addAll(oldUrlSet)
        }
        sharedPrefs.edit().putStringSet(key, newUrlSet).apply()
    }
}
