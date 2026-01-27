/*
 * SPDX-FileCopyrightText: 2016 Richard Wagler <riwag@posteo.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.SharePlugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.R
import java.io.File

class ShareSettingsFragment : PluginSettingsFragment() {

    private var filePicker: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        val customDownloads = findPreference<SwitchPreference>(PREFERENCE_CUSTOMIZE_DESTINATION)
        filePicker = findPreference("share_destination_folder_preference")

        customDownloads?.setOnPreferenceChangeListener { _, newValue ->
            updateFilePickerStatus(newValue as Boolean)
            true
        }
        filePicker?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, RESULT_PICKER)
            true
        }

        val customized = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getBoolean(PREFERENCE_CUSTOMIZE_DESTINATION, false)

        updateFilePickerStatus(customized)
    }

    private fun updateFilePickerStatus(enabled: Boolean) {
        filePicker?.isEnabled = enabled
        val path = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getString(PREFERENCE_DESTINATION, null)

        if (enabled && path != null) {
            filePicker?.summary = Uri.parse(path).path
        } else {
            filePicker?.summary = defaultDestinationDirectory.absolutePath
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == RESULT_PICKER && resultCode == Activity.RESULT_OK && resultData != null) {
            val uri = resultData.data ?: return
            saveStorageLocationPreference(requireContext(), uri)

            val picker = findPreference<Preference>("share_destination_folder_preference")
            picker?.summary = uri.path
        }
    }

    companion object {
        private const val PREFERENCE_CUSTOMIZE_DESTINATION = "share_destination_custom"
        private const val PREFERENCE_DESTINATION = "share_destination_folder_uri"
        private const val RESULT_PICKER = Activity.RESULT_FIRST_USER

        @JvmStatic
        fun newInstance(pluginKey: String, layout: Int): ShareSettingsFragment {
            val fragment = ShareSettingsFragment()
            fragment.setArguments(pluginKey, layout)
            return fragment
        }

        @get:JvmStatic
        val defaultDestinationDirectory: File
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        @JvmStatic
        fun isCustomDestinationEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_CUSTOMIZE_DESTINATION, false)
        }

        @JvmStatic
        fun getDestinationDirectory(context: Context): DocumentFile {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_CUSTOMIZE_DESTINATION, false)) {
                val path = PreferenceManager.getDefaultSharedPreferences(context).getString(PREFERENCE_DESTINATION, null)
                if (path != null) {
                    val treeDocumentFile = DocumentFile.fromTreeUri(context, Uri.parse(path))
                    if (treeDocumentFile != null && treeDocumentFile.canWrite()) {
                        return treeDocumentFile
                    } else {
                        Log.w("SharePlugin", "Share destination is not writable, falling back to default path.")
                    }
                }
            }
            try {
                defaultDestinationDirectory.mkdirs()
            } catch (e: Exception) {
                Log.e("COSMICConnect", "Exception", e)
            }
            return DocumentFile.fromFile(defaultDestinationDirectory)
        }

        @JvmStatic
        fun saveStorageLocationPreference(context: Context, uri: Uri) {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().apply {
                putString(PREFERENCE_DESTINATION, uri.toString())
                putBoolean(PREFERENCE_CUSTOMIZE_DESTINATION, true)
                apply()
            }
        }
    }
}
