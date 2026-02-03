/*
 * SPDX-FileCopyrightText: 2018 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.SftpPlugin

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SftpSettingsFragment : PluginSettingsFragment(),
    StoragePreferenceDialogFragment.Callback,
    Preference.OnPreferenceChangeListener,
    StoragePreference.OnLongClickListener,
    ActionMode.Callback {

    @Inject override lateinit var deviceRegistry: DeviceRegistry

    private lateinit var storageInfoList: MutableList<SftpPlugin.StorageInfo>
    private var preferenceCategory: PreferenceCategory? = null
    private var actionMode: ActionMode? = null
    private var savedActionModeState: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.findFragmentByTag(KEY_STORAGE_PREFERENCE_DIALOG)?.let {
            (it as StoragePreferenceDialogFragment).callback = this
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ACTION_MODE_STATE)) {
            try {
                savedActionModeState = JSONObject(savedInstanceState.getString(KEY_ACTION_MODE_STATE, "{}"))
            } catch (ignored: JSONException) {
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        val ta = requireContext().obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorAccent))
        val colorAccent = ta.getColor(0, 0)
        ta.recycle()

        storageInfoList = getStorageInfoList(requireContext(), plugin!!).toMutableList()

        val preferenceScreen = preferenceScreen
        preferenceCategory = preferenceScreen.findPreference(getString(R.string.sftp_preference_key_preference_category))

        preferenceCategory?.let { addStoragePreferences(it) }

        val addStoragePreference = preferenceScreen.findPreference<Preference>(getString(R.string.sftp_preference_key_add_storage))
        addStoragePreference?.icon?.setColorFilter(colorAccent, PorterDuff.Mode.SRC_IN)

        // Request storage permission if not granted
        checkAndRequestStoragePermission()
    }

    /**
     * Check if storage permission is granted, and show permission dialog if not
     */
    private fun checkAndRequestStoragePermission() {
        val currentPlugin = plugin ?: return

        if (!currentPlugin.checkRequiredPermissions()) {
            // Show the permission explanation dialog
            currentPlugin.permissionExplanationDialog
                .show(parentFragmentManager, "sftp_permission_dialog")
        }
    }

    private fun addStoragePreferences(preferenceCategory: PreferenceCategory) {
        val context = preferenceManager.context

        storageInfoList.sortBy { it.displayName.lowercase() }

        for (i in storageInfoList.indices) {
            val storageInfo = storageInfoList[i]
            val preference = StoragePreference(context)
            preference.onPreferenceChangeListener = this
            preference.setOnLongClickListener(this)
            preference.key = getString(R.string.sftp_preference_key_storage_info, i)
            preference.setIcon(android.R.color.transparent)
            preference.setStorageInfo(storageInfo)
            preference.dialogTitle = getString(R.string.sftp_preference_edit_storage_location)

            preferenceCategory.addPreference(preference)
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        if (savedActionModeState != null) {
            listView?.post { restoreActionMode() }
        }
        return super.onCreateAdapter(preferenceScreen)
    }

    private fun restoreActionMode() {
        try {
            val state = savedActionModeState ?: return
            if (state.getBoolean(KEY_ACTION_MODE_ENABLED)) {
                actionMode = (requireActivity() as PluginSettingsActivity).startSupportActionMode(this)

                if (actionMode != null) {
                    val jsonArray = state.getJSONArray(KEY_ACTION_MODE_SELECTED_ITEMS)
                    val selectedItems = SparseBooleanArray()

                    for (i in 0 until jsonArray.length()) {
                        selectedItems.put(jsonArray.getInt(i), true)
                    }

                    preferenceCategory?.let { category ->
                        for (i in 0 until category.preferenceCount) {
                            val preference = category.getPreference(i) as StoragePreference
                            preference.inSelectionMode = true
                            preference.checkbox.isChecked = selectedItems.get(i, false)
                        }
                    }
                }
            }
        } catch (ignored: JSONException) {
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is StoragePreference) {
            val fragment = StoragePreferenceDialogFragment.newInstance(preference.key)
            @Suppress("DEPRECATION")
            fragment.setTargetFragment(this, 0)
            fragment.callback = this
            fragment.show(parentFragmentManager, KEY_STORAGE_PREFERENCE_DIALOG)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        try {
            val jsonObject = JSONObject()
            jsonObject.put(KEY_ACTION_MODE_ENABLED, actionMode != null)

            if (actionMode != null) {
                val jsonArray = JSONArray()
                preferenceCategory?.let { category ->
                    for (i in 0 until category.preferenceCount) {
                        val preference = category.getPreference(i) as StoragePreference
                        if (preference.checkbox.isChecked) {
                            jsonArray.put(i)
                        }
                    }
                }
                jsonObject.put(KEY_ACTION_MODE_SELECTED_ITEMS, jsonArray)
            }

            outState.putString(KEY_ACTION_MODE_STATE, jsonObject.toString())
        } catch (ignored: JSONException) {
        }
    }

    private fun saveStorageInfoList() {
        val currentPlugin = plugin ?: return
        val preferences = currentPlugin.preferences ?: return
        val jsonArray = JSONArray()

        try {
            for (storageInfo in storageInfoList) {
                jsonArray.put(storageInfo.toJSON())
            }
        } catch (ignored: JSONException) {
        }

        preferences.edit()
            .putString(requireContext().getString(SftpPlugin.PREFERENCE_KEY_STORAGE_INFO_LIST), jsonArray.toString())
            .apply()
    }

    override fun isDisplayNameAllowed(displayName: String): StoragePreferenceDialogFragment.CallbackResult {
        val result = StoragePreferenceDialogFragment.CallbackResult()
        result.isAllowed = true

        if (displayName.isEmpty()) {
            result.isAllowed = false
            result.errorMessage = getString(R.string.sftp_storage_preference_display_name_cannot_be_empty)
        } else {
            for (storageInfo in storageInfoList) {
                if (storageInfo.displayName == displayName) {
                    result.isAllowed = false
                    result.errorMessage = getString(R.string.sftp_storage_preference_display_name_already_used)
                    break
                }
            }
        }
        return result
    }

    override fun isUriAllowed(uri: Uri): StoragePreferenceDialogFragment.CallbackResult {
        val result = StoragePreferenceDialogFragment.CallbackResult()
        result.isAllowed = true

        for (storageInfo in storageInfoList) {
            if (storageInfo.uri == uri) {
                result.isAllowed = false
                result.errorMessage = getString(R.string.sftp_storage_preference_storage_location_already_configured)
                break
            }
        }
        return result
    }

    override fun addNewStoragePreference(storageInfo: SftpPlugin.StorageInfo, takeFlags: Int) {
        storageInfoList.add(storageInfo)
        handleChangedStorageInfoList()
        requireContext().contentResolver.takePersistableUriPermission(storageInfo.uri, takeFlags)
    }

    private fun handleChangedStorageInfoList() {
        actionMode?.finish()
        saveStorageInfoList()

        preferenceCategory?.removeAll()
        preferenceCategory?.let { addStoragePreferences(it) }

        val id = deviceId ?: return
        val device = deviceRegistry.getDevice(id)
        device?.launchBackgroundReloadPluginsFromSettings()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val newStorageInfo = newValue as SftpPlugin.StorageInfo
        val it = storageInfoList.listIterator()

        while (it.hasNext()) {
            val storageInfo = it.next()
            if (storageInfo.uri == newStorageInfo.uri) {
                it.set(newStorageInfo)
                break
            }
        }

        handleChangedStorageInfoList()
        return false
    }

    override fun onLongClick(storagePreference: StoragePreference) {
        if (actionMode == null) {
            actionMode = (requireActivity() as PluginSettingsActivity).startSupportActionMode(this)

            if (actionMode != null) {
                preferenceCategory?.let { category ->
                    for (i in 0 until category.preferenceCount) {
                        val preference = category.getPreference(i) as StoragePreference
                        preference.inSelectionMode = true
                        if (storagePreference == preference) {
                            preference.checkbox.isChecked = true
                        }
                    }
                }
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.sftp_settings_action_mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        if (item.itemId == R.id.delete) {
            preferenceCategory?.let { category ->
                for (i in category.preferenceCount - 1 downTo 0) {
                    val preference = category.getPreference(i) as StoragePreference
                    if (preference.checkbox.isChecked) {
                        val info = storageInfoList.removeAt(i)
                        try {
                            requireContext().contentResolver.releasePersistableUriPermission(
                                info.uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            Log.e("SFTP Settings", "Exception", e)
                        }
                    }
                }
            }
            handleChangedStorageInfoList()
            return true
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        preferenceCategory?.let { category ->
            for (i in 0 until category.preferenceCount) {
                val preference = category.getPreference(i) as StoragePreference
                preference.inSelectionMode = false
                preference.checkbox.isChecked = false
            }
        }
    }

    companion object {
        private const val KEY_STORAGE_PREFERENCE_DIALOG = "StoragePreferenceDialog"
        private const val KEY_ACTION_MODE_STATE = "ActionModeState"
        private const val KEY_ACTION_MODE_ENABLED = "ActionModeEnabled"
        private const val KEY_ACTION_MODE_SELECTED_ITEMS = "ActionModeSelectedItems"

        @JvmStatic
        fun newInstance(pluginKey: String, layout: Int): SftpSettingsFragment {
            val fragment = SftpSettingsFragment()
            fragment.setArguments(pluginKey, layout)
            return fragment
        }

        @JvmStatic
        fun getStorageInfoList(context: Context, plugin: Plugin): List<SftpPlugin.StorageInfo> {
            val storageInfoList = mutableListOf<SftpPlugin.StorageInfo>()
            val deviceSettings = plugin.preferences ?: return emptyList()
            val jsonString = deviceSettings.getString(context.getString(SftpPlugin.PREFERENCE_KEY_STORAGE_INFO_LIST), "[]")

            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    storageInfoList.add(SftpPlugin.StorageInfo.fromJSON(jsonArray.getJSONObject(i)))
                }
            } catch (e: JSONException) {
                Log.e("SFTPSettings", "Couldn't load storage info", e)
            }
            return storageInfoList
        }
    }
}
