/*
 * SPDX-FileCopyrightText: 2021 Maxim Leshchenko <cnmaks90@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.About

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.about.LicensesScreen
import org.cosmicext.connect.UserInterface.compose.screens.about.LicensesViewModel

@AndroidEntryPoint
class LicensesActivity : ComponentActivity() {

    private val viewModel: LicensesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CosmicTheme(context = this) {
                LicensesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
