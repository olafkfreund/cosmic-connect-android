/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin

import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * Allows screen sharing between devices with configurable resolution, codec, and direction.
 * Receives sharing status updates and provides start/stop controls.
 */
class ScreenSharePlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): ScreenSharePlugin
    }

    interface Listener {
        fun onSharingStateChanged(
            isSharing: Boolean,
            width: Int?,
            height: Int?,
            codec: String?,
            fps: Int?,
            direction: String
        )
    }

    /** Whether screen sharing is currently active. Null if unknown. */
    var isSharing: Boolean? = null
        private set

    /** Frame width in pixels. Null if not sharing or unknown. */
    var width: Int? = null
        private set

    /** Frame height in pixels. Null if not sharing or unknown. */
    var height: Int? = null
        private set

    /** Video codec (e.g., "h264", "vp8"). Null if not sharing or unknown. */
    var codec: String? = null
        private set

    /** Frames per second. Null if not sharing or unknown. */
    var fps: Int? = null
        private set

    /** Direction of sharing: "phone_to_desktop" or "desktop_to_phone". */
    var direction: String = DIRECTION_PHONE_TO_DESKTOP
        private set

    private val listeners = mutableSetOf<Listener>()

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_screenshare)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_screenshare_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_SCREENSHARE)
    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_SCREENSHARE_REQUEST)
    override val isEnabledByDefault: Boolean = false

    override fun onCreate(): Boolean {
        return true
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_SCREENSHARE) return false

        isSharing = (np.body["isSharing"] as? Boolean) ?: false
        width = (np.body["width"] as? Number)?.toInt()
        height = (np.body["height"] as? Number)?.toInt()
        codec = np.body["codec"] as? String
        fps = (np.body["fps"] as? Number)?.toInt()
        direction = (np.body["direction"] as? String) ?: DIRECTION_PHONE_TO_DESKTOP

        Log.d(
            TAG,
            "Screen sharing status: isSharing=$isSharing, ${width}x$height, " +
                    "codec=$codec, fps=$fps, direction=$direction"
        )

        notifyListeners()
        device.onPluginsChanged()
        return true
    }

    /** Send a request to start screen sharing with specific configuration. */
    fun startSharing(
        width: Int,
        height: Int,
        codec: String,
        fps: Int,
        direction: String,
        enableInput: Boolean = false
    ) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_SCREENSHARE_REQUEST,
            body = mapOf(
                "startSharing" to true,
                "width" to width,
                "height" to height,
                "codec" to codec,
                "fps" to fps,
                "direction" to direction,
                "enableInput" to enableInput,
            ),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(
            TAG,
            "Sent start screen sharing request: ${width}x${height}, " +
                    "codec=$codec, fps=$fps, direction=$direction, enableInput=$enableInput"
        )
    }

    /** Send a request to stop screen sharing. */
    fun stopSharing() {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_SCREENSHARE_REQUEST,
            body = mapOf("stopSharing" to true),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Sent stop screen sharing request")
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { listener ->
            listener.onSharingStateChanged(
                isSharing ?: false,
                width,
                height,
                codec,
                fps,
                direction
            )
        }
    }

    companion object {
        private const val TAG = "ScreenSharePlugin"
        const val PACKET_TYPE_SCREENSHARE = "cconnect.screenshare"
        const val PACKET_TYPE_SCREENSHARE_REQUEST = "cconnect.screenshare.request"

        const val DIRECTION_PHONE_TO_DESKTOP = "phone_to_desktop"
        const val DIRECTION_DESKTOP_TO_PHONE = "desktop_to_phone"
    }
}
