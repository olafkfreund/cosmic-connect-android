/*
 * SPDX-FileCopyrightText: 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Plugins.PresenterPlugin

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import org.cosmic.cosmicconnect.DeviceType
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.KeyListenerView
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R


@LoadablePlugin
class PresenterPlugin : Plugin() {

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_presenter)

    override val isCompatible: Boolean
        get() = device.deviceType != DeviceType.PHONE && super.isCompatible

    override val description: String
        get() = context.getString(R.string.pref_plugin_presenter_desc)

    override fun hasSettings(): Boolean = false

    override fun getUiButtons(): List<PluginUiButton> = listOf(
        PluginUiButton(
            context.getString(R.string.pref_plugin_presenter),
            R.drawable.ic_presenter_24dp
        ) { parentActivity ->
            val intent = Intent(parentActivity, PresenterActivity::class.java)
            intent.putExtra("deviceId", device.deviceId)
            parentActivity.startActivity(intent)
        })

    override val supportedPacketTypes: Array<String> = emptyArray()

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_MOUSEPAD_REQUEST, PACKET_TYPE_PRESENTER)

    fun sendNext() {
        sendKeyPacket(KeyEvent.KEYCODE_PAGE_DOWN)
    }

    fun sendPrevious() {
        sendKeyPacket(KeyEvent.KEYCODE_PAGE_UP)
    }

    fun sendFullscreen() {
        sendKeyPacket(KeyEvent.KEYCODE_F5)
    }

    fun sendEsc() {
        sendKeyPacket(KeyEvent.KEYCODE_ESCAPE)
    }

    fun sendPointer(xDelta: Float, yDelta: Float) {
        // Create packet using FFI
        val packet = PresenterPacketsFFI.createPointerMovement(
            xDelta.toDouble(),
            yDelta.toDouble()
        )

        // Convert and send
        device.sendPacket(convertToLegacyPacket(packet))
    }

    fun stopPointer() {
        // Create packet using FFI
        val packet = PresenterPacketsFFI.createStop()

        // Convert and send
        device.sendPacket(convertToLegacyPacket(packet))
    }

    /**
     * Helper to send special key packets
     */
    private fun sendKeyPacket(keyCode: Int) {
        val specialKey = KeyListenerView.SpecialKeysMap.get(keyCode)

        // Create immutable packet
        val packet = NetworkPacket.create(PACKET_TYPE_MOUSEPAD_REQUEST, mapOf(
            "specialKey" to specialKey
        ))

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

    companion object {
        private const val PACKET_TYPE_PRESENTER = "cosmicconnect.presenter"
        private const val PACKET_TYPE_MOUSEPAD_REQUEST = "cosmicconnect.mousepad.request"
    }
}
