/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.DeviceStats
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.base.BaseActivity
import org.cosmic.cosmicconnect.databinding.ActivityPluginSettingsBinding
import javax.inject.Inject

@AndroidEntryPoint
class PluginSettingsActivity : BaseActivity<ActivityPluginSettingsBinding>(),
    PluginPreference.PluginPreferenceCallback {

    override val binding by lazy { ActivityPluginSettingsBinding.inflate(layoutInflater) }

    @Inject lateinit var deviceRegistry: DeviceRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        var pluginKey: String? = null

        if (intent.hasExtra(EXTRA_DEVICE_ID)) {
            val id = intent.getStringExtra(EXTRA_DEVICE_ID)
            mDeviceId = id

            if (intent.hasExtra(EXTRA_PLUGIN_KEY)) {
                pluginKey = intent.getStringExtra(EXTRA_PLUGIN_KEY)
            }
        } else if (mDeviceId == null) {
            throw RuntimeException("You must start DeviceSettingActivity using an intent that has a $EXTRA_DEVICE_ID extra")
        }

        val currentDeviceId = mDeviceId ?: return

        var fragment = supportFragmentManager.findFragmentById(R.id.fragmentPlaceHolder)
        if (fragment == null) {
            if (pluginKey != null) {
                val device = deviceRegistry.getDevice(currentDeviceId)
                if (device != null) {
                    val plugin = device.getPluginIncludingWithoutPermissions(pluginKey)
                    if (plugin != null) {
                        fragment = plugin.getSettingsFragment(this)
                    }
                }
            }
            if (fragment == null) {
                fragment = PluginSettingsListFragment.newInstance(currentDeviceId)
            }

            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentPlaceHolder, fragment!!)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val fm = supportFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.clear()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            return false // PacketStats not working in API < 24
        }
        menu.add(R.string.plugin_stats).setOnMenuItemClickListener {
            val id = mDeviceId
            if (id != null) {
                val stats = DeviceStats.getStatsForDevice(id)
                val alertDialog = MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.plugin_stats)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .setMessage(stats)
                    .show()
                val messageView = alertDialog.findViewById<TextView>(android.R.id.message)
                messageView?.setTextIsSelectable(true)
            }
            true
        }
        return true
    }

    override fun onStartPluginSettingsFragment(plugin: Plugin?) {
        if (plugin == null) return
        title = getString(R.string.plugin_settings_with_name, plugin.displayName)

        val fragment = plugin.getSettingsFragment(this) ?: return

        supportFragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragmentPlaceHolder, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onFinish() {
        finish()
    }

    val settingsDeviceId: String?
        get() = mDeviceId

    companion object {
        const val EXTRA_DEVICE_ID = "deviceId"
        const val EXTRA_PLUGIN_KEY = "pluginKey"

        // TODO: Save/restore state
        @JvmStatic
        private var mDeviceId: String? = null // Static because if we get here by using the back button in the action bar, the extra deviceId will not be set.
    }
}
