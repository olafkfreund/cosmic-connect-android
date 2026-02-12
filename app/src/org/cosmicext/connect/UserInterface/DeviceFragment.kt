/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.UserInterface

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
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.DeviceDetailScreen
import org.cosmicext.connect.UserInterface.compose.screens.DeviceDetailViewModel

@AndroidEntryPoint
class DeviceFragment : Fragment() {

    private val viewModel: DeviceDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deviceId = arguments?.getString(ARG_DEVICE_ID) ?: ""
        
        // Load device in ViewModel
        viewModel.loadDevice(deviceId)

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CosmicTheme(context = context) {
                    DeviceDetailScreen(
                        viewModel = viewModel,
                        onNavigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onPluginSettings = { pluginKey ->
                            val intent = Intent(requireContext(), PluginSettingsActivity::class.java).apply {
                                putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                                putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, pluginKey)
                            }
                            startActivity(intent)
                        },
                        onPluginActivity = { pluginKey ->
                            // TODO: Launch appropriate plugin activity
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val ARG_DEVICE_ID = "deviceId"
        private const val ARG_FROM_DEVICE_LIST = "fromDeviceList"

        @JvmStatic
        fun newInstance(deviceId: String?, fromDeviceList: Boolean): DeviceFragment {
            val frag = DeviceFragment()
            val args = Bundle()
            args.putString(ARG_DEVICE_ID, deviceId)
            args.putBoolean(ARG_FROM_DEVICE_LIST, fromDeviceList)
            frag.arguments = args
            return frag
        }
    }
}
