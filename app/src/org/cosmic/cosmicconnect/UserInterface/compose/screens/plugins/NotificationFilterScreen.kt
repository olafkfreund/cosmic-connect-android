/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.AppDatabase
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTopAppBar
import org.cosmic.cosmicconnect.UserInterface.compose.SimpleListItem
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing
import org.cosmic.cosmicconnect.ui.components.status.LoadingIndicator
import org.cosmic.cosmicconnect.UserInterface.compose.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationFilterScreen(
    viewModel: NotificationFilterViewModel,
    prefKey: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedAppForPrivacy by remember { mutableStateOf<AppListInfo?>(null) }

    LaunchedEffect(prefKey) {
        viewModel.loadApps(prefKey)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                CosmicTopAppBar(
                    title = stringResource(R.string.title_activity_notification_filter),
                    navigationIcon = CosmicIcons.Navigation.back,
                    onNavigationClick = onNavigateBack
                )
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.apps.isEmpty()) {
                LoadingIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SimpleListItem(
                            text = stringResource(R.string.show_notification_if_screen_off),
                            trailingContent = {
                                Switch(
                                    checked = uiState.screenOffNotification,
                                    onCheckedChange = { viewModel.setScreenOffNotification(it) }
                                )
                            }
                        )
                    }
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item {
                        SimpleListItem(
                            text = stringResource(R.string.all),
                            trailingContent = {
                                Checkbox(
                                    checked = uiState.allEnabled,
                                    onCheckedChange = { viewModel.toggleAll(it) }
                                )
                            }
                        )
                    }
                    items(uiState.apps, key = { it.pkg }) { app ->
                        AppItem(
                            app = app,
                            onToggle = { viewModel.toggleApp(app.pkg, it) },
                            onLongClick = { selectedAppForPrivacy = app }
                        )
                    }
                }
            }
        }
    }

    selectedAppForPrivacy?.let { app ->
        PrivacyOptionsDialog(
            app = app,
            onDismiss = { selectedAppForPrivacy = null },
            onUpdatePrivacy = { option, enabled -> 
                viewModel.updatePrivacy(app.pkg, option, enabled)
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
        placeholder = { Text("Search apps...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = MaterialTheme.shapes.medium,
        singleLine = true
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    app: AppListInfo,
    onToggle: (Boolean) -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
            .combinedClickable(
                onClick = { onToggle(!app.isEnabled) },
                onLongClick = onLongClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let {
            Image(
                bitmap = it.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = app.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.medium)
        )
        Checkbox(
            checked = app.isEnabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun PrivacyOptionsDialog(
    app: AppListInfo,
    onDismiss: () -> Unit,
    onUpdatePrivacy: (AppDatabase.PrivacyOptions, Boolean) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.privacy_options)) },
        text = {
            Column {
                Text(text = app.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Spacing.medium))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = app.blockContents,
                        onCheckedChange = { onUpdatePrivacy(AppDatabase.PrivacyOptions.BLOCK_CONTENTS, it) }
                    )
                    Text(stringResource(R.string.block_notification_contents))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = app.blockImages,
                        onCheckedChange = { onUpdatePrivacy(AppDatabase.PrivacyOptions.BLOCK_IMAGES, it) }
                    )
                    Text(stringResource(R.string.block_notification_images))
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}
