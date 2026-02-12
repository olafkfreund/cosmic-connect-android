/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.Plugins.FileSyncPlugin.FileSyncPlugin
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTopAppBar
import org.cosmic.cosmicconnect.UserInterface.compose.ConfirmationDialog
import org.cosmic.cosmicconnect.UserInterface.compose.InputDialog
import org.cosmic.cosmicconnect.UserInterface.compose.SectionHeader
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSyncScreen(
    viewModel: FileSyncViewModel,
    deviceId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderToRemove by remember { mutableStateOf<FileSyncPlugin.SyncFolder?>(null) }

    LaunchedEffect(deviceId) {
        viewModel.loadDevice(deviceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.filesync_title),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFolderDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.filesync_add_folder))
            }
        }
    ) { paddingValues ->
        if (uiState.syncFolders.isEmpty() && uiState.conflicts.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.filesync_no_folders),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                // Sync folders section
                items(
                    items = uiState.syncFolders,
                    key = { it.id }
                ) { folder ->
                    SyncFolderCard(
                        folder = folder,
                        onSyncClick = { viewModel.requestSync(folder.id) },
                        onRemoveClick = { folderToRemove = folder }
                    )
                }

                // Conflicts section
                if (uiState.conflicts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.filesync_conflicts),
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                    }

                    items(
                        items = uiState.conflicts,
                        key = { it.path }
                    ) { conflict ->
                        ConflictCard(
                            conflict = conflict,
                            onUseLocal = { viewModel.resolveConflict(conflict.path, useLocal = true) },
                            onUseRemote = { viewModel.resolveConflict(conflict.path, useLocal = false) }
                        )
                    }
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(Spacing.xxxl))
                }
            }
        }
    }

    // Add folder dialog
    if (showAddFolderDialog) {
        InputDialog(
            title = stringResource(R.string.filesync_add_folder),
            message = stringResource(R.string.filesync_add_folder_description),
            label = stringResource(R.string.filesync_folder_path),
            initialValue = "",
            placeholder = stringResource(R.string.filesync_folder_path_hint),
            onConfirm = { path ->
                viewModel.addFolder(path)
                showAddFolderDialog = false
            },
            onDismiss = { showAddFolderDialog = false }
        )
    }

    // Remove folder confirmation dialog
    folderToRemove?.let { folder ->
        ConfirmationDialog(
            title = stringResource(R.string.filesync_remove_confirm),
            message = stringResource(R.string.filesync_remove_confirm_message),
            confirmLabel = stringResource(R.string.filesync_remove_folder),
            onConfirm = {
                viewModel.removeFolder(folder.id)
                folderToRemove = null
            },
            onDismiss = { folderToRemove = null }
        )
    }
}

@Composable
private fun SyncFolderCard(
    folder: FileSyncPlugin.SyncFolder,
    onSyncClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    SyncStatusChip(status = folder.syncStatus)
                }
                IconButton(onClick = onRemoveClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.filesync_remove_folder),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            OutlinedButton(
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.filesync_sync_now))
            }
        }
    }
}

@Composable
private fun SyncStatusChip(
    status: FileSyncPlugin.SyncStatus,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        FileSyncPlugin.SyncStatus.IDLE -> "Idle" to MaterialTheme.colorScheme.onSurfaceVariant
        FileSyncPlugin.SyncStatus.SYNCING -> "Syncing" to MaterialTheme.colorScheme.primary
        FileSyncPlugin.SyncStatus.COMPLETE -> "Complete" to MaterialTheme.colorScheme.tertiary
        FileSyncPlugin.SyncStatus.ERROR -> "Error" to MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun ConflictCard(
    conflict: FileSyncPlugin.FileConflict,
    onUseLocal: () -> Unit,
    onUseRemote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = conflict.path,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onUseLocal) {
                    Text(
                        text = stringResource(R.string.filesync_use_local),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.small))
                Button(onClick = onUseRemote) {
                    Text(text = stringResource(R.string.filesync_use_remote))
                }
            }
        }
    }
}
