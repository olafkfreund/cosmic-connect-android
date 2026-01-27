/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.SharePlugin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.ShareViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    @Inject lateinit var deviceRegistry: DeviceRegistry

    private val viewModel: ShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        var deviceId = intent.getStringExtra("deviceId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && deviceId == null) {
            deviceId = intent.getStringExtra(Intent.EXTRA_SHORTCUT_ID)
        }

        if (deviceId != null) {
            val plugin = deviceRegistry.getDevicePlugin(deviceId, SharePlugin::class.java)
            if (plugin != null) {
                plugin.share(intent)
            }
            finish()
            return
        }

        setContent {
            CosmicTheme(context = this) {
                ShareScreen(
                    viewModel = viewModel,
                    shareIntent = intent,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
