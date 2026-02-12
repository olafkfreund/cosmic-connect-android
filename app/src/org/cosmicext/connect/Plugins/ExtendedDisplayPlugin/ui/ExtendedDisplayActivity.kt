/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ui

import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.cosmicext.connect.BuildConfig
import org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ConnectionState
import org.cosmicext.connect.R

/**
 * Full-screen activity for Extended Display feature.
 * Renders remote desktop content via WebRTC and handles touch input.
 */
@AndroidEntryPoint
class ExtendedDisplayActivity : ComponentActivity(), SurfaceHolder.Callback {

    private val viewModel: ExtendedDisplayViewModel by viewModels()

    private lateinit var surfaceView: SurfaceView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extended_display)

        // Enable immersive full-screen mode
        enableImmersiveMode()

        // Keep screen on during display session
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize views
        surfaceView = findViewById(R.id.surface_view)
        statusText = findViewById(R.id.status_text)

        // Set up SurfaceView
        surfaceView.holder.addCallback(this)

        // Set up debug overlay with Compose
        setupDebugOverlay()

        // Get server details from intent
        val serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS) ?: ""
        val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 0)

        // Observe connection state
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                updateConnectionStatus(state)
            }
        }

        // Start connection if we have server details
        if (serverAddress.isNotEmpty() && serverPort > 0) {
            viewModel.connect(serverAddress, serverPort)
        }
    }

    /**
     * Enable immersive full-screen mode (hide status bar and navigation)
     */
    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    /**
     * Set up debug overlay with Compose
     */
    private fun setupDebugOverlay() {
        // Create ComposeView for debug overlay
        val composeView = ComposeView(this).apply {
            setContent {
                val debugInfo by viewModel.debugInfo.collectAsState()
                val connectionState by viewModel.connectionState.collectAsState()

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Only show debug overlay in debug builds
                    if (BuildConfig.DEBUG) {
                        DebugOverlay(
                            debugInfo = debugInfo,
                            connectionState = connectionState,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // Add ComposeView to root container
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        rootView?.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    /**
     * Update connection status display
     */
    private fun updateConnectionStatus(state: ConnectionState) {
        runOnUiThread {
            when (state) {
                ConnectionState.CONNECTING -> {
                    statusText.text = getString(R.string.extended_display_connecting)
                    statusText.visibility = View.VISIBLE
                }
                ConnectionState.CONNECTED -> {
                    statusText.visibility = View.GONE
                }
                ConnectionState.DISCONNECTING -> {
                    statusText.text = getString(R.string.extended_display_disconnecting)
                    statusText.visibility = View.VISIBLE
                }
                ConnectionState.DISCONNECTED, ConnectionState.CLOSED -> {
                    statusText.text = getString(R.string.extended_display_disconnected)
                    statusText.visibility = View.VISIBLE
                }
                ConnectionState.FAILED, ConnectionState.ERROR -> {
                    statusText.text = getString(R.string.extended_display_error)
                    statusText.visibility = View.VISIBLE
                }
            }
        }
    }

    // SurfaceHolder.Callback implementation

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Surface is created and ready for rendering
        // TODO: Initialize video decoder and start rendering
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewModel.updateDisplayConfig(
            DisplayConfig(
                width = width,
                height = height,
                fps = 60,
                bitrate = 5_000_000
            )
        )
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface is being destroyed
        // TODO: Stop video decoder and cleanup
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_SERVER_ADDRESS = "server_address"
        const val EXTRA_SERVER_PORT = "server_port"
    }
}
