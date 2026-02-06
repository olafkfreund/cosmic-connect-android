/*
 * SPDX-FileCopyrightText: 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.MousePadPlugin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceType
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment.Companion.newInstance
import org.cosmic.cosmicconnect.R
import org.json.JSONObject

class MousePadPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): MousePadPlugin
    }

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
        return listOf(mouseAndKeyboardInput)
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
     * Send a pre-constructed legacy NetworkPacket
     *
     * Used by KeyListenerView for special key handling
     */
    fun sendPacket(legacyPacket: LegacyNetworkPacket) {
        device.sendPacket(legacyPacket)
    }

    private fun sendSpecialKey(keyCode: Int) {
        val specialKey = KeyListenerView.SpecialKeysMap.get(keyCode)
        sendMousePacket(mapOf("specialKey" to specialKey))
    }

    private fun sendMousePacket(body: Map<String, Any>) {
        val json = JSONObject(body).toString()
        val packet = MousePadPacketsFFI.createMousePadRequest(json)
        device.sendPacket(packet.toLegacyPacket())
    }

    override val supportedPacketTypes = arrayOf(PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE)
    override val outgoingPacketTypes = arrayOf(PACKET_TYPE_MOUSEPAD_REQUEST)

    companion object {
        const val PACKET_TYPE_MOUSEPAD_REQUEST: String = "cconnect.mousepad.request"
        private const val PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE = "cconnect.mousepad.keyboardstate"
    }
}
