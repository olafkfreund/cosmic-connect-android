/*
 * SPDX-FileCopyrightText: 2015 Aleix Pol Gonzalez <aleixpol@kde.org>
 * SPDX-FileCopyrightText: 2015 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.RunCommandViewModel

@AndroidEntryPoint
class RunCommandActivity : ComponentActivity() {

    private val viewModel: RunCommandViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceId = intent.getStringExtra("deviceId")

        setContent {
            CosmicTheme(context = this) {
                RunCommandScreen(
                    viewModel = viewModel,
                    deviceId = deviceId,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
