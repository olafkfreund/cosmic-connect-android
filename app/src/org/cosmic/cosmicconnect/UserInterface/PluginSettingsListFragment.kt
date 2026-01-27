/*
 * SPDX-FileCopyrightText: 2018 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.PluginSettingsViewModel
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import javax.inject.Inject

@AndroidEntryPoint
class PluginSettingsListFragment : Fragment() {

    private val viewModel: PluginSettingsViewModel by viewModels()
    
    @Inject lateinit var deviceRegistry: DeviceRegistry

    private var callback: PluginPreference.PluginPreferenceCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity()
        if (activity is PluginPreference.PluginPreferenceCallback) {
            callback = activity
        } else {
            throw RuntimeException(
                "${activity.javaClass.simpleName} must implement PluginPreference.PluginPreferenceCallback"
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val deviceId = arguments?.getString(ARG_DEVICE_ID) ?: ""
        
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CosmicTheme(context = context) {
                    PluginSettingsScreen(
                        viewModel = viewModel,
                        deviceId = deviceId,
                        onNavigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onNavigateToPluginSettings = { pluginKey ->
                            val device = deviceRegistry.getDevice(deviceId)
                            val plugin = device?.getPluginIncludingWithoutPermissions(pluginKey)
                            callback?.onStartPluginSettingsFragment(plugin)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.device_menu_plugins)
    }

    override fun onDestroy() {
        super.onDestroy()
        callback = null
    }

    companion object {
        private const val ARG_DEVICE_ID = "deviceId"

        @JvmStatic
        fun newInstance(deviceId: String): PluginSettingsListFragment {
            return PluginSettingsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DEVICE_ID, deviceId)
                }
            }
        }
    }
}
