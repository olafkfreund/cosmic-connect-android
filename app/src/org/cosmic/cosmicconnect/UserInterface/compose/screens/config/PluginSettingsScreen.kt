/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.config

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTopAppBar
import org.cosmic.cosmicconnect.UserInterface.compose.PluginCard
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing
import org.cosmic.cosmicconnect.ui.components.status.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSettingsScreen(
    viewModel: PluginSettingsViewModel,
    deviceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPluginSettings: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(deviceId) {
        viewModel.loadDevice(deviceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.device_menu_plugins),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.supportedPlugins) { plugin ->
                        PluginCard(
                            pluginName = plugin.name,
                            pluginDescription = plugin.description,
                            pluginIcon = plugin.icon,
                            isEnabled = plugin.isEnabled,
                            isAvailable = plugin.isAvailable,
                            onToggle = { enabled -> viewModel.togglePlugin(plugin.key, enabled) },
                            onClick = if (plugin.hasSettings) {
                                { onNavigateToPluginSettings(plugin.key) }
                            } else null,
                            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall)
                        )
                    }
                }
            }
        }
    }
}
