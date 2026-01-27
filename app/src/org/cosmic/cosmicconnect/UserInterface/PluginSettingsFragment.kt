/*
 * SPDX-FileCopyrightText: 2018 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.UserInterface

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.DataStorePreferenceAdapter
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.R
import javax.inject.Inject

@AndroidEntryPoint
open class PluginSettingsFragment : PreferenceFragmentCompat() {

    @Inject open lateinit var deviceRegistry: DeviceRegistry
    @Inject lateinit var pluginFactory: PluginFactory

    private var pluginKey: String? = null
    private var layouts: IntArray? = null

    protected var device: Device? = null

    @JvmField
    protected var plugin: Plugin? = null

    protected fun setArguments(pluginKey: String, vararg settingsLayouts: Int): Bundle {
        val args = Bundle()
        args.putString(ARG_PLUGIN_KEY, pluginKey)
        args.putIntArray(ARG_LAYOUT, settingsLayouts)
        setArguments(args)
        return args
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val arguments = requireArguments()
        if (!arguments.containsKey(ARG_PLUGIN_KEY)) {
            throw RuntimeException("You must provide a pluginKey by calling setArguments(@NonNull String pluginKey)")
        }
        this.pluginKey = arguments.getString(ARG_PLUGIN_KEY)
        this.layouts = arguments.getIntArray(ARG_LAYOUT)
        this.device = deviceRegistry.getDevice(this.deviceId)
        this.plugin = device!!.getPluginIncludingWithoutPermissions(pluginKey!!)
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val prefsManager = getPreferenceManager()
        prefsManager.preferenceDataStore = DataStorePreferenceAdapter(requireContext())

        if (this.plugin != null && this.plugin!!.supportsDeviceSpecificSettings()) {
            // Note: DataStore handles scoping by keys, but for now we use the global settings DataStore
            // with SharedPreferencesMigration for legacy files if needed.
            // Ideally, per-device settings would also be in DataStore.
            // For now, we set the DataStore as the backend.
        }

        for (layout in this.layouts!!) {
            addPreferencesFromResource(layout)
        }
    }

    override fun onResume() {
        super.onResume()

        val info = pluginFactory.getPluginInfo(pluginKey!!)
        requireActivity().title = getString(R.string.plugin_settings_with_name, info.displayName)
    }

    val deviceId: String?
        get() = (activity as? PluginSettingsActivity)?.settingsDeviceId

    companion object {
        private const val ARG_PLUGIN_KEY = "plugin_key"
        private const val ARG_LAYOUT = "layout"

        @JvmStatic
        fun newInstance(pluginKey: String, vararg settingsLayout: Int): PluginSettingsFragment {
            val fragment = PluginSettingsFragment()
            fragment.setArguments(pluginKey, *settingsLayout)
            return fragment
        }
    }
}