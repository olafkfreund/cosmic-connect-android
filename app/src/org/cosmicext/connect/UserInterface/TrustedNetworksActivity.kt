/*
 * SPDX-FileCopyrightText: 2019 Juan David Vega <jdvr.93@hotmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmicext.connect.UserInterface

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.config.TrustedNetworksScreen
import org.cosmicext.connect.UserInterface.compose.screens.config.TrustedNetworksViewModel

@AndroidEntryPoint
class TrustedNetworksActivity : ComponentActivity() {

    private val viewModel: TrustedNetworksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CosmicTheme(context = this) {
                TrustedNetworksScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onRequestPermissions = { permissions ->
                        requestPermissions(permissions, 0)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSettings()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        viewModel.loadSettings()
    }
}