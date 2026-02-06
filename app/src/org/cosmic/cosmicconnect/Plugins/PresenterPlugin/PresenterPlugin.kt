/*
 * SPDX-FileCopyrightText: 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Plugins.PresenterPlugin

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
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.KeyListenerView
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R


class PresenterPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): PresenterPlugin
    }

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
        val packet = PresenterPacketsFFI.createPointerMovement(
            xDelta.toDouble(),
            yDelta.toDouble()
        )

        device.sendPacket(TransferPacket(packet))
    }

    fun stopPointer() {
        val packet = PresenterPacketsFFI.createStop()

        device.sendPacket(TransferPacket(packet))
    }

    private fun sendKeyPacket(keyCode: Int) {
        val specialKey = KeyListenerView.SpecialKeysMap.get(keyCode)

        val packet = NetworkPacket.create(PACKET_TYPE_MOUSEPAD_REQUEST, mapOf(
            "specialKey" to specialKey
        ))

        device.sendPacket(TransferPacket(packet))
    }


    companion object {
        private const val PACKET_TYPE_PRESENTER = "cconnect.presenter"
        private const val PACKET_TYPE_MOUSEPAD_REQUEST = "cconnect.mousepad.request"
    }
}
