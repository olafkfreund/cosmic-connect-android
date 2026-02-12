/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ui

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.cosmicext.connect.R

/**
 * Settings fragment for Extended Display preferences.
 */
class ExtendedDisplaySettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.extendeddisplay_preferences, rootKey)

        // Connection mode preference
        findPreference<ListPreference>(getString(R.string.extendeddisplay_preference_key_connection_mode))?.apply {
            setDefaultValue("wifi")
        }

        // Display mode preference
        findPreference<ListPreference>(getString(R.string.extendeddisplay_preference_key_display_mode))?.apply {
            setDefaultValue("fit")
        }

        // Debug info toggle
        findPreference<SwitchPreferenceCompat>(getString(R.string.extendeddisplay_preference_key_show_debug))?.apply {
            setDefaultValue(false)
        }

        // Latency overlay toggle
        findPreference<SwitchPreferenceCompat>(getString(R.string.extendeddisplay_preference_key_show_latency))?.apply {
            setDefaultValue(false)
        }
    }

    companion object {
        fun newInstance(): ExtendedDisplaySettingsFragment {
            return ExtendedDisplaySettingsFragment()
        }
    }
}
