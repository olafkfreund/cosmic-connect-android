/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.DeviceStats
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.R
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import javax.inject.Inject

@AndroidEntryPoint
class PluginSettingsActivity : AppCompatActivity() {

    @Inject lateinit var deviceRegistry: DeviceRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plugin_settings)

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
            finish()
            return
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
            
            if (fragment != null) {
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragmentPlaceHolder, fragment)
                    .commit()
            } else {
                // No plugin settings to show
                finish()
            }
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
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
