/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualMonitorScreen(
    viewModel: VirtualMonitorViewModel,
    deviceId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(deviceId) {
        viewModel.loadDevice(deviceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.virtualmonitor_title),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Status card
            item {
                MonitorStatusCard(uiState = uiState)
            }

            // Configuration form
            item {
                ConfigurationCard(
                    uiState = uiState,
                    onWidthChange = { viewModel.updateConfigWidth(it) },
                    onHeightChange = { viewModel.updateConfigHeight(it) },
                    onDpiChange = { viewModel.updateConfigDpi(it) },
                    onPositionChange = { viewModel.updateConfigPosition(it) },
                    onRefreshRateChange = { viewModel.updateConfigRefreshRate(it) }
                )
            }

            // Enable/Disable button
            item {
                if (uiState.isActive) {
                    OutlinedButton(
                        onClick = { viewModel.disableMonitor() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.virtualmonitor_disable))
                    }
                } else {
                    Button(
                        onClick = { viewModel.enableMonitor() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.virtualmonitor_enable))
                    }
                }
            }

            // Open Viewer button (only when session is active)
            if (uiState.hasActiveSession) {
                item {
                    Button(
                        onClick = { viewModel.launchViewer(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.virtualmonitor_open_viewer))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorStatusCard(
    uiState: VirtualMonitorUiState,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = stringResource(R.string.virtualmonitor_status),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            StatusRow(
                label = stringResource(R.string.virtualmonitor_status),
                value = if (uiState.isActive) {
                    stringResource(R.string.virtualmonitor_active)
                } else {
                    stringResource(R.string.virtualmonitor_inactive)
                },
                valueColor = if (uiState.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (uiState.isActive) {
                uiState.width?.let { w ->
                    uiState.height?.let { h ->
                        StatusRow(
                            label = "Resolution",
                            value = "${w}x${h}"
                        )
                    }
                }
                uiState.dpi?.let {
                    StatusRow(
                        label = stringResource(R.string.virtualmonitor_dpi),
                        value = "$it"
                    )
                }
                uiState.position?.let {
                    StatusRow(
                        label = stringResource(R.string.virtualmonitor_position),
                        value = it.replaceFirstChar { c -> c.uppercase() }
                    )
                }
                uiState.refreshRate?.let {
                    StatusRow(
                        label = stringResource(R.string.virtualmonitor_refresh_rate),
                        value = "$it Hz"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurationCard(
    uiState: VirtualMonitorUiState,
    onWidthChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onDpiChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onRefreshRateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = stringResource(R.string.virtualmonitor_config),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Spacing.medium))

            // Width and Height on the same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedTextField(
                    value = uiState.configWidth,
                    onValueChange = { onWidthChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(stringResource(R.string.virtualmonitor_width)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.configHeight,
                    onValueChange = { onHeightChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(stringResource(R.string.virtualmonitor_height)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // DPI and Refresh Rate on the same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                OutlinedTextField(
                    value = uiState.configDpi,
                    onValueChange = { onDpiChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(stringResource(R.string.virtualmonitor_dpi)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.configRefreshRate,
                    onValueChange = { onRefreshRateChange(it.filter { c -> c.isDigit() }) },
                    label = { Text(stringResource(R.string.virtualmonitor_refresh_rate)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.small))

            // Position dropdown
            PositionDropdown(
                selectedPosition = uiState.configPosition,
                onPositionSelected = onPositionChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionDropdown(
    selectedPosition: String,
    onPositionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val positions = listOf(
        "left" to stringResource(R.string.virtualmonitor_position_left),
        "right" to stringResource(R.string.virtualmonitor_position_right),
        "above" to stringResource(R.string.virtualmonitor_position_above),
        "below" to stringResource(R.string.virtualmonitor_position_below)
    )

    val selectedLabel = positions.find { it.first == selectedPosition }?.second
        ?: selectedPosition.replaceFirstChar { it.uppercase() }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.virtualmonitor_position)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            positions.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onPositionSelected(value)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
