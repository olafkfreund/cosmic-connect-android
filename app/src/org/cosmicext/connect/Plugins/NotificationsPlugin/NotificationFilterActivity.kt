/*
 * SPDX-FileCopyrightText: 2015 Vineet Garg <grg.vineet@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.NotificationsPlugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.plugins.NotificationFilterScreen
import org.cosmicext.connect.UserInterface.compose.screens.plugins.NotificationFilterViewModel

@AndroidEntryPoint
class NotificationFilterActivity : ComponentActivity() {

    private val viewModel: NotificationFilterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefKey = intent.getStringExtra(NotificationsPlugin.getPrefKey())

        setContent {
            CosmicTheme(context = this) {
                NotificationFilterScreen(
                    viewModel = viewModel,
                    prefKey = prefKey,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}