/*
 * SPDX-FileCopyrightText: 2018 Erik Duisters <e.duisters1@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.UserInterface

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.SettingsViewModel

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CosmicTheme(context = context) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onNavigateToTrustedNetworks = {
                            startActivity(Intent(context, TrustedNetworksActivity::class.java))
                        },
                        onNavigateToCustomDevices = {
                            startActivity(Intent(context, CustomDevicesActivity::class.java))
                        },
                        onExportLogs = {
                            // TODO: Implement log export
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings)
        viewModel.refreshCustomDevicesCount()
    }

    companion object {
        const val KEY_BLUETOOTH_ENABLED = "bluetooth_enabled"
    }
}
