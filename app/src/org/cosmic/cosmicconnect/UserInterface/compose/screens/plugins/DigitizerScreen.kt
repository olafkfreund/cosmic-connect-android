/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.Plugins.DigitizerPlugin.DrawingPadView
import org.cosmic.cosmicconnect.Plugins.DigitizerPlugin.ToolEvent
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTopAppBar
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DigitizerScreen(
    viewModel: DigitizerViewModel,
    deviceId: String?,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var drawingPadView by remember { mutableStateOf<DrawingPadView?>(null) }

    LaunchedEffect(deviceId) {
        viewModel.loadDevice(deviceId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (!uiState.isFullscreen) {
                CosmicTopAppBar(
                    title = stringResource(R.string.pref_plugin_digitizer),
                    navigationIcon = CosmicIcons.Navigation.back,
                    onNavigationClick = onNavigateBack,
                    actions = {
                        IconButton(onClick = { onToggleFullscreen(true) }) {
                            Icon(painter = painterResource(CosmicIcons.Action.openInFull), contentDescription = "Fullscreen")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (uiState.isFullscreen) PaddingValues(0.dp) else paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    DrawingPadView(ctx, null).apply {
                        eventListener = object : DrawingPadView.EventListener {
                            override fun onToolEvent(event: ToolEvent) {
                                viewModel.reportEvent(event)
                            }
                            override fun onFingerTouchEvent(touching: Boolean) {
                                // Handled via state if needed
                            }
                        }
                        drawingPadView = this
                        
                        // Wait for layout to start session
                        post {
                            viewModel.startSession(
                                width, height,
                                (resources.displayMetrics.xdpi * 0.0393701).roundToInt(),
                                (resources.displayMetrics.ydpi * 0.0393701).roundToInt()
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!uiState.hideDrawButton) {
                val alignment = when (uiState.drawButtonSide) {
                    "top_left" -> Alignment.TopStart
                    "top_right" -> Alignment.TopEnd
                    "bottom_left" -> Alignment.BottomStart
                    "bottom_right" -> Alignment.BottomEnd
                    else -> Alignment.BottomStart
                }
                
                Button(
                    onClick = { },
                    modifier = Modifier
                        .align(alignment)
                        .padding(16.dp)
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    drawingPadView?.fingerTouchEventsEnabled = true
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    drawingPadView?.fingerTouchEventsEnabled = false
                                    true
                                }
                                else -> false
                            }
                        }
                ) {
                    Text(stringResource(R.string.digitizer_draw_button))
                }
            }
        }
    }
}