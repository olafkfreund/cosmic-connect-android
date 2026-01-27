/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import org.cosmic.cosmicconnect.UserInterface.compose.SimpleListItem
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing
import org.cosmic.cosmicconnect.UserInterface.compose.getDeviceIcon
import org.cosmic.cosmicconnect.ui.components.status.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    viewModel: ShareViewModel,
    shareIntent: Intent?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDevices(shareIntent)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.share_to),
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.intentHasUrl) {
                        item {
                            Text(
                                text = stringResource(R.string.unreachable_device_url_share_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(Spacing.medium)
                            )
                        }
                    }

                    items(uiState.devices) { device ->
                        if (device.isPaired && (uiState.intentHasUrl || device.isReachable)) {
                            SimpleListItem(
                                text = device.name,
                                secondaryText = if (device.isReachable) "Connected" else "Unreachable",
                                icon = getDeviceIcon(device.deviceType.toString()),
                                onClick = { 
                                    if (shareIntent != null) {
                                        viewModel.onDeviceClick(device, shareIntent, onNavigateBack)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
