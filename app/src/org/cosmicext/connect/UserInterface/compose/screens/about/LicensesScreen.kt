/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.Spacing
import org.cosmicext.connect.ui.components.status.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    viewModel: LicensesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val licenses by viewModel.licenses.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.licenses),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack,
                actions = {
                    // Scroll to top
                    androidx.compose.material3.IconButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(CosmicIcons.Navigation.up),
                            contentDescription = "Scroll to top"
                        )
                    }
                    // Scroll to bottom
                    androidx.compose.material3.IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (licenses.isNotEmpty()) {
                                    listState.animateScrollToItem(licenses.size - 1)
                                }
                            }
                        }
                    ) {
                        androidx.compose.material3.Icon(
                            painter = androidx.compose.ui.res.painterResource(CosmicIcons.Navigation.down),
                            contentDescription = "Scroll to bottom"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (licenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Spacing.medium)
            ) {
                items(licenses) { licenseText ->
                    Text(
                        text = licenseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = Spacing.small)
                    )
                    // Divider or spacing between licenses
                    androidx.compose.material3.HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}