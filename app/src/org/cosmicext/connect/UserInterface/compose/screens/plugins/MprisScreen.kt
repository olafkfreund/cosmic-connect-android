/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.cosmicext.connect.R
import org.cosmicext.connect.Plugins.MprisPlugin.MprisPlugin
import org.cosmicext.connect.Plugins.SystemVolumePlugin.Sink
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MprisScreen(
    viewModel: MprisViewModel,
    deviceId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(deviceId) {
        viewModel.loadDevice(deviceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.pref_plugin_mpris),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.mpris_play)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.devices)) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> NowPlayingScreen(
                        players = uiState.players,
                        selectedPlayer = uiState.selectedPlayer,
                        status = uiState.playerStatus,
                        onPlayerSelect = { viewModel.selectPlayer(it) },
                        onAction = { viewModel.sendAction(it) }
                    )
                    1 -> SystemVolumeScreen(
                        sinks = uiState.sinks,
                        onVolumeChange = { name, volume -> viewModel.setVolume(name, volume) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(
    players: List<String>,
    selectedPlayer: String?,
    status: MprisPlugin.MprisPlayer?,
    onPlayerSelect: (String) -> Unit,
    onAction: ((MprisPlugin.MprisPlayer) -> Unit) -> Unit
) {
    if (players.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_players_connected))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Player Selection (simplified for now)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                players.forEach { name ->
                    Tab(
                        selected = name == selectedPlayer,
                        onClick = { onPlayerSelect(name) },
                        text = { Text(name) }
                    )
                }
            }
        }

        if (status != null) {
            item {
                // Album Art
                Card(
                    modifier = Modifier.size(240.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    val albumArt = status.getAlbumArt()
                    if (albumArt != null) {
                        Image(
                            bitmap = albumArt.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(CosmicIcons.Media.albumArtPlaceholder),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                // Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = status.title.ifEmpty { "Unknown Title" },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = status.artist.ifEmpty { "Unknown Artist" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                // Progress (simplified)
                if (status.isSeekAllowed) {
                    Slider(
                        value = status.position.toFloat(),
                        onValueChange = { onAction { p -> p.sendSetPosition(it.toInt()) } },
                        valueRange = 0f..status.length.toFloat().coerceAtLeast(1f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onAction { it.sendPrevious() } }) {
                        Icon(painter = painterResource(CosmicIcons.Media.previousBlack), contentDescription = null)
                    }
                    IconButton(
                        onClick = { onAction { it.sendPlayPause() } },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (status.isPlaying) CosmicIcons.Media.pauseBlack else CosmicIcons.Media.playBlack
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(onClick = { onAction { it.sendNext() } }) {
                        Icon(painter = painterResource(CosmicIcons.Media.nextBlack), contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemVolumeScreen(
    sinks: List<Sink>,
    onVolumeChange: (String, Int) -> Unit
) {
    if (sinks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audio sinks found")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        items(sinks) { sink ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.medium)) {
                    Text(text = sink.description, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(
                                if (sink.mute) CosmicIcons.Media.volumeMute else CosmicIcons.Media.volume
                            ),
                            contentDescription = null
                        )
                        Slider(
                            value = sink.volume.toFloat(),
                            onValueChange = { onVolumeChange(sink.name, it.toInt()) },
                            valueRange = 0f..sink.maxVolume.toFloat(),
                            modifier = Modifier.weight(1f)
                        )
                        Text(text = "${(sink.volume * 100 / sink.maxVolume)}%")
                    }
                }
            }
        }
    }
}