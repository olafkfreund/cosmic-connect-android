/*
 * SPDX-FileCopyrightText: 2025 Martin Sh <hemisputnik@proton.me>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.DigitizerPlugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import androidx.preference.PreferenceManager
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterActivity
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.R

@PluginFactory.LoadablePlugin
class DigitizerPlugin : Plugin() {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_digitizer)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_digitizer_desc)

    override val isEnabledByDefault: Boolean
        get() = DeviceHelper.isTablet

    override fun getUiButtons(): List<PluginUiButton> = listOf(
        PluginUiButton(
            context.getString(R.string.use_digitizer),
            R.drawable.ic_draw_24dp
        ) { parentActivity ->
            val intent = Intent(parentActivity, DigitizerActivity::class.java)
            intent.putExtra("deviceId", device.deviceId)
            parentActivity.startActivity(intent)
        })

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        Log.e(TAG, "The drawing tablet plugin should not be able to receive any packets!")
        return false
    }

    fun startSession(width: Int, height: Int, resolutionX: Int, resolutionY: Int) {
        val np = NetworkPacket(PACKET_TYPE_DIGITIZER_SESSION).apply {
            set("action", "start")
            set("width", width)
            set("height", height)
            set("resolutionX", resolutionX)
            set("resolutionY", resolutionY)
        }
        device.sendPacket(np)
    }

    fun endSession() {
        val np = NetworkPacket(PACKET_TYPE_DIGITIZER_SESSION).apply {
            set("action", "end")
        }
        device.sendPacket(np)
    }

    fun reportEvent(event: ToolEvent) {
        Log.d(TAG, "reportEvent: $event")

        val np = NetworkPacket(PACKET_TYPE_DIGITIZER).also { packet ->
            event.active?.let { packet["active"] = it }
            event.touching?.let { packet["touching"] = it }
            event.tool?.let { packet["tool"] = it.name }
            event.x?.let { packet["x"] = it }
            event.y?.let { packet["y"] = it }
            event.pressure?.let { packet["pressure"] = it }
        }
        device.sendPacket(np)
    }

    override fun hasSettings(): Boolean = true
    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment =
        PluginSettingsFragment.newInstance(pluginKey, R.xml.digitizer_preferences)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf()

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(
            PACKET_TYPE_DIGITIZER_SESSION,
            PACKET_TYPE_DIGITIZER,
        )

    companion object {
        private const val PACKET_TYPE_DIGITIZER_SESSION = "cosmicconnect.digitizer.session"
        private const val PACKET_TYPE_DIGITIZER = "cosmicconnect.digitizer"

        private const val TAG = "DigitizerPlugin"
    }
}