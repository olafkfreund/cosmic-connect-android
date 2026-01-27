/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.DeviceHost
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTopAppBar
import org.cosmic.cosmicconnect.UserInterface.compose.SimpleListItem
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing
import org.cosmic.cosmicconnect.ui.components.status.LoadingIndicator
import org.cosmic.cosmicconnect.UserInterface.compose.InputDialog
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDevicesScreen(
    viewModel: CustomDevicesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<DeviceHost?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.custom_devices_settings),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_device_dialog_title))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.devices.isEmpty()) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.devices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.custom_device_list_help),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.xxl) // Space for FAB
                ) {
                    items(uiState.devices) { host ->
                        CustomDeviceItem(
                            host = host,
                            onClick = { deviceToEdit = host },
                            onDelete = { viewModel.removeDevice(host) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        InputDialog(
            title = stringResource(R.string.add_device_dialog_title),
            message = stringResource(R.string.add_device_hint),
            label = "IP or Hostname",
            onConfirm = { hostnameOrIp ->
                if (viewModel.addDevice(hostnameOrIp)) {
                    showAddDialog = false
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    deviceToEdit?.let { host ->
        InputDialog(
            title = "Edit device",
            message = stringResource(R.string.add_device_hint),
            label = "IP or Hostname",
            initialValue = host.toString(),
            onConfirm = { newHostnameOrIp ->
                if (viewModel.updateDevice(host, newHostnameOrIp)) {
                    deviceToEdit = null
                }
            },
            onDismiss = { deviceToEdit = null }
        )
    }
}

@Composable
private fun CustomDeviceItem(
    host: DeviceHost,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isReachable = host.ping != null && host.ping?.latency != null
    SimpleListItem(
        text = host.toString(),
        secondaryText = if (isReachable) "Reachable" else "Unreachable",
        icon = if (isReachable) CosmicIcons.Status.info else CosmicIcons.Status.warning,
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        onClick = onClick
    )
}