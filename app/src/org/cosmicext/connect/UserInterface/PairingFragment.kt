/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.UserInterface

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.DeviceListScreen
import org.cosmicext.connect.UserInterface.compose.screens.DeviceListViewModel

@AndroidEntryPoint
class PairingFragment : Fragment() {

    private val viewModel: DeviceListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CosmicTheme(context = context) {
                    DeviceListScreen(
                        viewModel = viewModel,
                        onDeviceClick = { device ->
                            (requireActivity() as? MainActivity)?.onDeviceSelected(
                                device.deviceId, 
                                !device.isPaired || !device.isReachable
                            )
                        },
                        onNavigateToCustomDevices = {
                            startActivity(android.content.Intent(context, CustomDevicesActivity::class.java))
                        },
                        onNavigateToTrustedNetworks = {
                            startActivity(android.content.Intent(context, TrustedNetworksActivity::class.java))
                        },
                        onNavigateToSettings = {
                            (requireActivity() as? MainActivity)?.onDeviceSelected(null) // Unselect device
                            // Navigation to settings is handled by NavHost via the activity
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.pairing_title)
    }
}
