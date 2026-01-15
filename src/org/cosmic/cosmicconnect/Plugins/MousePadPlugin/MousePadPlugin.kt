/*
 * SPDX-FileCopyrightText: 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.MousePadPlugin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import androidx.preference.PreferenceManager
import org.cosmic.cosmicconnect.DeviceType
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment.Companion.newInstance
import org.cosmic.cosmicconnect.R

@LoadablePlugin
class MousePadPlugin : Plugin() {
    var isKeyboardEnabled: Boolean = true
        private set

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        this.isKeyboardEnabled = np.getBoolean("state", true)
        return true
    }

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_mousepad)

    override fun getUiButtons(): List<PluginUiButton> {
        val mouseAndKeyboardInput = PluginUiButton(
            context.getString(R.string.open_mousepad),
            R.drawable.touchpad_plugin_action_24dp
        ) { parentActivity ->
            val intent = Intent(parentActivity, MousePadActivity::class.java)
            intent.putExtra("deviceId", device.deviceId)
            parentActivity.startActivity(intent)
        }
        return if (device.deviceType == DeviceType.TV) {
            val tvInput = PluginUiButton(
                context.getString(R.string.open_mousepad_tv),
                R.drawable.tv_remote_24px
            ) { parentActivity ->
                val intent = Intent(parentActivity, BigscreenActivity::class.java)
                intent.putExtra("deviceId", device.deviceId)
                parentActivity.startActivity(intent)
            }
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (prefs.getBoolean(context.getString(R.string.pref_bigscreen_hide_mouse_input), false)) {
                listOf(tvInput)
            } else {
                listOf(mouseAndKeyboardInput, tvInput)
            }
        } else {
            listOf(mouseAndKeyboardInput)
        }
    }


    override val description: String
        get() = context.getString(R.string.pref_plugin_mousepad_desc_nontv)

    override fun hasSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment? {
        return if (device.deviceType == DeviceType.TV) {
            newInstance(pluginKey, R.xml.mousepadplugin_preferences, R.xml.mousepadplugin_preferences_tv)
        } else {
            newInstance(pluginKey, R.xml.mousepadplugin_preferences)
        }
    }

    fun sendMouseDelta(dx: Float, dy: Float) {
        sendMousePacket(mapOf(
            "dx" to dx.toDouble(),
            "dy" to dy.toDouble()
        ))
    }

    fun hasMicPermission(): Boolean {
        return isPermissionGranted(Manifest.permission.RECORD_AUDIO)
    }

    fun sendLeftClick() {
        sendMousePacket(mapOf("singleclick" to true))
    }

    fun sendDoubleClick() {
        sendMousePacket(mapOf("doubleclick" to true))
    }

    fun sendMiddleClick() {
        sendMousePacket(mapOf("middleclick" to true))
    }

    fun sendRightClick() {
        sendMousePacket(mapOf("rightclick" to true))
    }

    fun sendSingleHold() {
        sendMousePacket(mapOf("singlehold" to true))
    }

    fun sendSingleRelease() {
        sendMousePacket(mapOf("singlerelease" to true))
    }

    fun sendScroll(dx: Float, dy: Float) {
        sendMousePacket(mapOf(
            "scroll" to true,
            "dx" to dx.toDouble(),
            "dy" to dy.toDouble()
        ))
    }

    fun sendLeft() {
        sendSpecialKey(KeyEvent.KEYCODE_DPAD_LEFT)
    }

    fun sendRight() {
        sendSpecialKey(KeyEvent.KEYCODE_DPAD_RIGHT)
    }

    fun sendUp() {
        sendSpecialKey(KeyEvent.KEYCODE_DPAD_UP)
    }

    fun sendDown() {
        sendSpecialKey(KeyEvent.KEYCODE_DPAD_DOWN)
    }

    fun sendSelect() {
        sendSpecialKey(KeyEvent.KEYCODE_ENTER)
    }

    fun sendHome() {
        sendMousePacket(mapOf(
            "alt" to true,
            "specialKey" to KeyListenerView.SpecialKeysMap.get(KeyEvent.KEYCODE_F4)
        ))
    }

    fun sendBack() {
        sendSpecialKey(KeyEvent.KEYCODE_ESCAPE)
    }

    fun sendText(content: String) {
        sendMousePacket(mapOf("key" to content))
    }

    /**
     * Helper to send special key packets
     */
    private fun sendSpecialKey(keyCode: Int) {
        val specialKey = KeyListenerView.SpecialKeysMap.get(keyCode)
        sendMousePacket(mapOf("specialKey" to specialKey))
    }

    /**
     * Helper to send mouse/keyboard packets
     */
    private fun sendMousePacket(body: Map<String, Any>) {
        // Create immutable packet
        val packet = NetworkPacket.create(PACKET_TYPE_MOUSEPAD_REQUEST, body)

        // Convert and send
        device.sendPacket(convertToLegacyPacket(packet))
    }

    /**
     * Convert immutable NetworkPacket to legacy NetworkPacket for sending
     */
    private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
        val legacy = LegacyNetworkPacket(ffi.type)

        // Copy all body fields
        ffi.body.forEach { (key, value) ->
            when (value) {
                is String -> legacy.set(key, value)
                is Int -> legacy.set(key, value)
                is Long -> legacy.set(key, value)
                is Boolean -> legacy.set(key, value)
                is Double -> legacy.set(key, value)
                else -> legacy.set(key, value.toString())
            }
        }

        return legacy
    }

    override val supportedPacketTypes = arrayOf(PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE)
    override val outgoingPacketTypes = arrayOf(PACKET_TYPE_MOUSEPAD_REQUEST)

    companion object {
        const val PACKET_TYPE_MOUSEPAD_REQUEST: String = "cosmicconnect.mousepad.request"
        private const val PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE = "cosmicconnect.mousepad.keyboardstate"
    }
}
