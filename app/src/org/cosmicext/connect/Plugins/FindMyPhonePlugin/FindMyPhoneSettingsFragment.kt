/*
 * SPDX-FileCopyrightText: 2018 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.FindMyPhonePlugin

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.IntentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import org.cosmicext.connect.UserInterface.PluginSettingsFragment
import org.cosmicext.connect.R

class FindMyPhoneSettingsFragment : PluginSettingsFragment() {

    private var preferenceKeyRingtone: String? = null
    private var sharedPreferences: SharedPreferences? = null
    private var ringtonePreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        preferenceKeyRingtone = getString(R.string.findmyphone_preference_key_ringtone)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        ringtonePreference = preferenceScreen.findPreference(preferenceKeyRingtone!!)

        setRingtoneSummary()
    }

    private fun setRingtoneSummary() {
        val ringtone = sharedPreferences?.getString(preferenceKeyRingtone, Settings.System.DEFAULT_RINGTONE_URI.toString()) ?: return
        val ringtoneUri = Uri.parse(ringtone)
        val title = RingtoneManager.getRingtone(requireContext(), ringtoneUri).getTitle(requireContext())
        ringtonePreference?.summary = title
    }

    @Suppress("DEPRECATION")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        /*
         * There is no RingtonePreference in support library nor androidx, this is the workaround proposed here:
         * https://issuetracker.google.com/issues/37057453
         */
        if (preference.hasKey() && preference.key == preferenceKeyRingtone) {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)

                val existingValue = sharedPreferences?.getString(preferenceKeyRingtone, null)
                if (existingValue != null) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue))
                } else {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_RINGTONE_URI)
                }
            }

            startActivityForResult(intent, REQUEST_CODE_SELECT_RINGTONE)
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SELECT_RINGTONE && resultCode == Activity.RESULT_OK && data != null) {
            val uri = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)

            if (uri != null) {
                sharedPreferences?.edit()
                    ?.putString(preferenceKeyRingtone, uri.toString())
                    ?.apply()

                setRingtoneSummary()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_RINGTONE = 1000

        @JvmStatic
        fun newInstance(pluginKey: String, layout: Int): FindMyPhoneSettingsFragment {
            val fragment = FindMyPhoneSettingsFragment()
            fragment.setArguments(pluginKey, layout)
            return fragment
        }
    }
}
