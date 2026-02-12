/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.config

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.SectionHeader
import org.cosmicext.connect.UserInterface.compose.SimpleListItem
import org.cosmicext.connect.UserInterface.compose.Spacing
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedNetworksScreen(
    viewModel: TrustedNetworksViewModel,
    onNavigateBack: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.trusted_networks),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = Spacing.medium)
        ) {
            item {
                SimpleListItem(
                    text = stringResource(R.string.allow_all_networks_text),
                    secondaryText = "Disable verification for all networks",
                    trailingContent = {
                        Switch(
                            checked = uiState.allNetworksAllowed,
                            onCheckedChange = { checked ->
                                if (uiState.hasPermissions) {
                                    viewModel.setAllNetworksAllowed(checked)
                                } else {
                                    onRequestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                }
                            }
                        )
                    }
                )
            }

            if (!uiState.allNetworksAllowed) {
                item {
                    SectionHeader(title = "Trusted Networks")
                }

                if (uiState.trustedNetworks.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.empty_trusted_networks_list_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.medium)
                        )
                    }
                } else {
                    items(uiState.trustedNetworks) { ssid ->
                        SimpleListItem(
                            text = ssid,
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeTrustedNetwork(ssid) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }

                uiState.currentSsid?.let { ssid ->
                    if (ssid !in uiState.trustedNetworks) {
                        item {
                            Button(
                                onClick = { viewModel.addTrustedNetwork(ssid) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.medium)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(
                                    text = stringResource(R.string.add_trusted_network, ssid),
                                    modifier = Modifier.padding(start = Spacing.small)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}