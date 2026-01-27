/*
 * SPDX-FileCopyrightText: 2025 Martin Sh <hemisputnik@proton.me>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.DigitizerPlugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.DigitizerViewModel

@AndroidEntryPoint
class DigitizerActivity : ComponentActivity() {

    private val viewModel: DigitizerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceId = intent.getStringExtra("deviceId")!!

        setContent {
            CosmicTheme(context = this) {
                DigitizerScreen(
                    viewModel = viewModel,
                    deviceId = deviceId,
                    onNavigateBack = { finish() },
                    onOpenSettings = {
                        startActivity(
                            Intent(this, PluginSettingsActivity::class.java)
                                .putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                                .putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, DigitizerPlugin::class.java.getSimpleName())
                        )
                    },
                    onToggleFullscreen = { enabled ->
                        if (enabled) enableFullscreen() else disableFullscreen()
                        viewModel.setFullscreen(enabled)
                    }
                )
            }
        }
    }

    private fun enableFullscreen() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun disableFullscreen() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onStop() {
        super.onStop()
        viewModel.endSession()
    }
}