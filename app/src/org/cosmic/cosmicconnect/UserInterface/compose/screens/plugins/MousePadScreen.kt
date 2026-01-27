/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.KeyListenerView
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTopAppBar
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MousePadScreen(
    viewModel: MousePadViewModel,
    deviceId: String?,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var keyListenerView by remember { mutableStateOf<KeyListenerView?>(null) }

    LaunchedEffect(deviceId) {
        viewModel.loadDevice(deviceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.pref_plugin_mousepad),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack,
                actions = {
                    if (uiState.isKeyboardEnabled) {
                        IconButton(onClick = {
                            keyListenerView?.requestFocus()
                            val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
                            imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                        }) {
                            Icon(painter = painterResource(CosmicIcons.Action.keyboard), contentDescription = "Show Keyboard")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Hidden KeyListenerView for keyboard input
            AndroidView(
                factory = { ctx ->
                    KeyListenerView(ctx, null).apply {
                        setDeviceId(deviceId)
                        keyListenerView = this
                    }
                },
                modifier = Modifier.size(0.dp)
            )

            // Touchpad Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .pointerInteropFilter { event ->
                        // Forward events to Activity? 
                        // For now this area is just a placeholder, 
                        // real interaction is handled by Activity.onTouchEvent
                        false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Touchpad",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            // Mouse Buttons
            if (uiState.mouseButtonsEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(Spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    Button(
                        onClick = { viewModel.sendLeftClick() },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Left")
                    }
                    Button(
                        onClick = { viewModel.sendMiddleClick() },
                        modifier = Modifier.weight(0.6f).fillMaxSize(),
                        shape = MaterialTheme.shapes.medium,
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("Mid")
                    }
                    Button(
                        onClick = { viewModel.sendRightClick() },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Right")
                    }
                }
            }
        }
    }
}