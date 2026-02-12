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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioStreamScreen(
    viewModel: AudioStreamViewModel,
    deviceId: String?,
    onNavigateBack: () -> Unit,
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
                title = stringResource(R.string.audiostream_title),
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
            // Stream status card
            item {
                StreamStatusCard(uiState = uiState)
            }

            // Capabilities section
            item {
                CapabilitiesCard(uiState = uiState)
            }

            // Start/Stop button
            item {
                StreamControlSection(
                    uiState = uiState,
                    onStartStream = { codec, sampleRate, channels ->
                        viewModel.startStream(codec, sampleRate, channels)
                    },
                    onStopStream = { viewModel.stopStream() }
                )
            }
        }
    }
}

@Composable
private fun StreamStatusCard(
    uiState: AudioStreamUiState,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = stringResource(R.string.audiostream_status),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            StatusRow(
                label = stringResource(R.string.audiostream_status),
                value = if (uiState.isStreaming) {
                    stringResource(R.string.audiostream_streaming)
                } else {
                    stringResource(R.string.audiostream_idle)
                },
                valueColor = if (uiState.isStreaming) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (uiState.isStreaming) {
                uiState.codec?.let {
                    StatusRow(
                        label = stringResource(R.string.audiostream_codec),
                        value = it
                    )
                }
                uiState.sampleRate?.let {
                    StatusRow(
                        label = stringResource(R.string.audiostream_sample_rate),
                        value = "$it Hz"
                    )
                }
                uiState.channels?.let {
                    StatusRow(
                        label = stringResource(R.string.audiostream_channels),
                        value = when (it) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            else -> "$it"
                        }
                    )
                }
                uiState.direction?.let {
                    StatusRow(
                        label = stringResource(R.string.audiostream_direction),
                        value = it.replaceFirstChar { c -> c.uppercase() }
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatusRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

@Composable
private fun CapabilitiesCard(
    uiState: AudioStreamUiState,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(
                text = stringResource(R.string.audiostream_capabilities),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            if (uiState.supportedCodecs.isEmpty() && uiState.supportedSampleRates.isEmpty()) {
                Text(
                    text = stringResource(R.string.audiostream_no_capabilities),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (uiState.supportedCodecs.isNotEmpty()) {
                    StatusRow(
                        label = stringResource(R.string.audiostream_codecs),
                        value = uiState.supportedCodecs.joinToString(", ")
                    )
                }
                if (uiState.supportedSampleRates.isNotEmpty()) {
                    StatusRow(
                        label = stringResource(R.string.audiostream_sample_rates),
                        value = uiState.supportedSampleRates.joinToString(", ") { "$it Hz" }
                    )
                }
                uiState.maxChannels?.let {
                    StatusRow(
                        label = stringResource(R.string.audiostream_channels),
                        value = "$it"
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamControlSection(
    uiState: AudioStreamUiState,
    onStartStream: (String, Int, Int) -> Unit,
    onStopStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isStreaming) {
            Button(
                onClick = onStopStream,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.audiostream_stop))
            }
        } else {
            // Use first available codec and sample rate as defaults
            val defaultCodec = uiState.supportedCodecs.firstOrNull() ?: "opus"
            val defaultSampleRate = uiState.supportedSampleRates.firstOrNull() ?: 48000
            val defaultChannels = 2

            Button(
                onClick = { onStartStream(defaultCodec, defaultSampleRate, defaultChannels) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.audiostream_start))
            }
        }
    }
}
