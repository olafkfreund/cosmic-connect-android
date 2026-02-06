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
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterActivity
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.R
import org.json.JSONObject

import org.cosmic.cosmicconnect.di.HiltBridges
import dagger.hilt.EntryPoints

class DigitizerPlugin(context: Context, device: Device) : Plugin(context, device) {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_digitizer)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_digitizer_desc)

    override val isCompatible: Boolean
        get() {
            val deviceHelper = EntryPoints.get(context.applicationContext, HiltBridges::class.java).deviceHelper()
            return deviceHelper.isTablet
        }


    override fun getUiButtons(): List<PluginUiButton> = listOf(
        PluginUiButton(
            context.getString(R.string.use_digitizer),
            R.drawable.ic_draw_24dp
        ) { parentActivity ->
            val intent = Intent(parentActivity, DigitizerActivity::class.java)
            intent.putExtra("deviceId", device.deviceId)
            parentActivity.startActivity(intent)
        })

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        Log.e(TAG, "The drawing tablet plugin should not be able to receive any packets!")
        return false
    }

    fun startSession(width: Int, height: Int, resolutionX: Int, resolutionY: Int) {
        val body = mapOf(
            "action" to "start",
            "width" to width,
            "height" to height,
            "resolutionX" to resolutionX,
            "resolutionY" to resolutionY
        )
        val json = JSONObject(body).toString()
        val packet = DigitizerPacketsFFI.createSessionPacket(json)
        device.sendPacket(TransferPacket(packet))
    }

    fun endSession() {
        val json = JSONObject(mapOf("action" to "end")).toString()
        val packet = DigitizerPacketsFFI.createSessionPacket(json)
        device.sendPacket(TransferPacket(packet))
    }

    fun reportEvent(event: ToolEvent) {
        Log.d(TAG, "reportEvent: $event")

        // Build body with optional fields
        val body = mutableMapOf<String, Any>()
        event.active?.let { body["active"] = it }
        event.touching?.let { body["touching"] = it }
        event.tool?.let { body["tool"] = it.name }
        event.x?.let { body["x"] = it }
        event.y?.let { body["y"] = it }
        event.pressure?.let { body["pressure"] = it }

        val json = JSONObject(body).toString()
        val packet = DigitizerPacketsFFI.createEventPacket(json)
        device.sendPacket(TransferPacket(packet))
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
        private const val PACKET_TYPE_DIGITIZER_SESSION = "cconnect.digitizer.session"
        private const val PACKET_TYPE_DIGITIZER = "cconnect.digitizer"

        private const val TAG = "DigitizerPlugin"
    }
}