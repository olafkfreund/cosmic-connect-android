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
import org.cosmic.cosmicconnect.Plugins.FileSyncPlugin.FileSyncPlugin
import javax.inject.Inject

data class FileSyncUiState(
    val syncFolders: List<FileSyncPlugin.SyncFolder> = emptyList(),
    val conflicts: List<FileSyncPlugin.FileConflict> = emptyList(),
    val isLoaded: Boolean = false
)

@HiltViewModel
class FileSyncViewModel @Inject constructor(
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileSyncUiState())
    val uiState: StateFlow<FileSyncUiState> = _uiState.asStateFlow()

    private var plugin: FileSyncPlugin? = null
    private var listener: FileSyncPlugin.Listener? = null

    fun loadDevice(deviceId: String?) {
        plugin = deviceRegistry.getDevicePlugin(deviceId, FileSyncPlugin::class.java)

        listener = object : FileSyncPlugin.Listener {
            override fun onSyncStatusChanged(folderId: String, status: FileSyncPlugin.SyncStatus) {
                viewModelScope.launch { updateState() }
            }

            override fun onConflictDetected(conflict: FileSyncPlugin.FileConflict) {
                viewModelScope.launch { updateState() }
            }

            override fun onFileChanged(action: String, path: String, syncFolderId: String) {
                viewModelScope.launch { updateState() }
            }
        }
        plugin?.listener = listener

        updateState()
    }

    private fun updateState() {
        val p = plugin
        _uiState.value = if (p != null) {
            FileSyncUiState(
                syncFolders = p.getSyncFolders(),
                conflicts = p.conflicts,
                isLoaded = true
            )
        } else {
            FileSyncUiState(isLoaded = true)
        }
    }

    fun addFolder(path: String) {
        plugin?.addSyncFolder(path)
        // Request updated list after adding
        plugin?.requestSyncFolderList()
    }

    fun removeFolder(folderId: String) {
        plugin?.removeSyncFolder(folderId)
        updateState()
    }

    fun requestSync(folderId: String) {
        plugin?.requestSync(folderId)
    }

    fun resolveConflict(conflictPath: String, useLocal: Boolean) {
        plugin?.resolveConflict(conflictPath, useLocal)
        updateState()
    }

    override fun onCleared() {
        if (plugin?.listener === listener) {
            plugin?.listener = null
        }
        super.onCleared()
    }
}
