/*
 * SPDX-FileCopyrightText: 2021 Daniel Weigl <DanielWeigl@gmx.at>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.MousePadPlugin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.plugins.SendKeystrokesScreen
import org.cosmicext.connect.UserInterface.compose.screens.plugins.SendKeystrokesViewModel

@AndroidEntryPoint
class SendKeystrokesToHostActivity : ComponentActivity() {

    private val viewModel: SendKeystrokesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent ?: return
        val text = if ("text/x-keystrokes" == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            CosmicTheme(context = this) {
                SendKeystrokesScreen(
                    viewModel = viewModel,
                    text = text,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
