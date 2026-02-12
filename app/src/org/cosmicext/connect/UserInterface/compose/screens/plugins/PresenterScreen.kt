/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.plugins

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cosmicext.connect.R
import org.cosmicext.connect.Plugins.PresenterPlugin.PresenterPlugin
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.Spacing

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PresenterScreen(
    plugin: PresenterPlugin,
    sensorEventListener: SensorEventListener,
    offScreenControlsSupported: Boolean,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as? SensorManager }
    var dropdownShownState by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.pref_plugin_presenter),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { dropdownShownState = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.extra_options))
                    }
                    DropdownMenu(expanded = dropdownShownState, onDismissRequest = { dropdownShownState = false }) {
                        DropdownMenuItem(
                            onClick = { 
                                plugin.sendFullscreen()
                                dropdownShownState = false
                            },
                            text = { Text(stringResource(R.string.presenter_fullscreen)) },
                        )
                        DropdownMenuItem(
                            onClick = { 
                                plugin.sendEsc()
                                dropdownShownState = false
                            },
                            text = { Text(stringResource(R.string.presenter_exit)) },
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            if (offScreenControlsSupported) {
                Text(
                    text = stringResource(R.string.presenter_lock_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.medium)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Button(
                    onClick = { plugin.sendPrevious() },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.mpris_previous),
                        modifier = Modifier.size(48.dp)
                    )
                }
                Button(
                    onClick = { plugin.sendNext() },
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = stringResource(R.string.mpris_next),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            if (sensorManager != null) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    sensorManager.registerListener(
                                        sensorEventListener,
                                        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                                        SensorManager.SENSOR_DELAY_GAME
                                    )
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    sensorManager.unregisterListener(sensorEventListener)
                                    plugin.stopPointer()
                                    true
                                }
                                else -> false
                            }
                        },
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.presenter_pointer),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}
