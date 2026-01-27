/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
fun SendKeystrokesScreen(
    viewModel: SendKeystrokesViewModel,
    text: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentText by remember { mutableStateOf(text ?: "") }

    LaunchedEffect(text) {
        viewModel.loadDevices(text)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.sendkeystrokes_send_to),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = currentText,
                onValueChange = { currentText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
                label = { Text("Text to send") }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    LoadingIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.devices) { device ->
                            SimpleListItem(
                                text = device.name,
                                secondaryText = "Connected",
                                icon = getDeviceIcon(device.deviceType.toString()),
                                onClick = { 
                                    if (viewModel.sendKeys(device, currentText)) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.sendkeystrokes_sent_text, currentText, device.name),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onNavigateBack()
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
