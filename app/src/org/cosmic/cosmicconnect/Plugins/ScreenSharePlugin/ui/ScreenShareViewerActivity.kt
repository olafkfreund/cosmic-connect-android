/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ui

import android.os.Build
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ScreenSharePlugin
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming.StreamState
import org.cosmic.cosmicconnect.R

/**
 * Full-screen activity for viewing a screen share or virtual monitor stream.
 *
 * Launched via notification when the desktop initiates a screen share.
 * Renders H.264 video frames decoded by VideoDecoder onto a SurfaceView.
 */
class ScreenShareViewerActivity : ComponentActivity(), SurfaceHolder.Callback {

    companion object {
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_MODE = "mode"
        const val MODE_SCREENSHARE = "screenshare"
        const val MODE_VIRTUALMONITOR = "virtualmonitor"
    }

    private val viewModel: ScreenShareViewModel by viewModels()

    private lateinit var surfaceView: SurfaceView
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screenshare_viewer)

        enableImmersiveMode()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = findViewById(R.id.surface_view)
        statusText = findViewById(R.id.status_text)

        surfaceView.holder.addCallback(this)

        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID)
        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SCREENSHARE

        // Set title based on mode
        title = getString(
            if (mode == MODE_VIRTUALMONITOR) R.string.screenshare_viewer_virtualmonitor_title
            else R.string.screenshare_viewer_title
        )

        // Look up the active stream session from the plugin
        val session = findActiveSession(deviceId)
        if (session == null) {
            Toast.makeText(this, R.string.screenshare_viewer_no_session, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel.attachSession(session)

        // Observe stream state
        lifecycleScope.launch {
            viewModel.streamState.collect { state ->
                updateStatus(state)
            }
        }
    }

    private fun findActiveSession(deviceId: String?): org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming.StreamSession? {
        if (deviceId == null) return null
        val device = CosmicConnect.getInstance().getDevice(deviceId) ?: return null
        val plugin = device.getPlugin(ScreenSharePlugin::class.java) ?: return null
        return plugin.activeSession
    }

    private fun updateStatus(state: StreamState) {
        runOnUiThread {
            when (state) {
                is StreamState.Idle,
                is StreamState.WaitingForConnection -> {
                    statusText.text = getString(R.string.screenshare_viewer_connecting)
                    statusText.visibility = View.VISIBLE
                }
                is StreamState.Receiving -> {
                    statusText.visibility = View.GONE
                }
                is StreamState.Stopped -> {
                    statusText.text = getString(R.string.screenshare_viewer_stopped)
                    statusText.visibility = View.VISIBLE
                }
                is StreamState.Error -> {
                    statusText.text = getString(R.string.screenshare_viewer_error)
                    statusText.visibility = View.VISIBLE
                }
            }
        }
    }

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

    // SurfaceHolder.Callback

    override fun surfaceCreated(holder: SurfaceHolder) {
        viewModel.onSurfaceReady(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // No-op for now, could handle resolution changes
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface gone — session continues in background if needed
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
}
