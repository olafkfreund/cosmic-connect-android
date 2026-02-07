/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.VirtualMonitorPlugin

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
 * Virtual Monitor plugin - uses Android device as additional display for desktop.
 * Receives virtual monitor status and sends enable/disable requests.
 */
class VirtualMonitorPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): VirtualMonitorPlugin
    }

    /** Whether the virtual monitor is currently active. Null if unknown. */
    var isActive: Boolean? = null
        private set

    /** Current display width in pixels. Null if not configured. */
    var width: Int? = null
        private set

    /** Current display height in pixels. Null if not configured. */
    var height: Int? = null
        private set

    /** Current display DPI. Null if not configured. */
    var dpi: Int? = null
        private set

    /** Current display position relative to desktop: "left", "right", "above", "below". Null if not configured. */
    var position: String? = null
        private set

    /** Current refresh rate in Hz. Null if not configured. */
    var refreshRate: Int? = null
        private set

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_virtualmonitor)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_virtualmonitor_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_VIRTUALMONITOR)
    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_VIRTUALMONITOR_REQUEST)
    override val isEnabledByDefault: Boolean = false

    /** Listener interface for virtual monitor state changes. */
    interface VirtualMonitorStateListener {
        /** Called when virtual monitor state or configuration changes. */
        fun onVirtualMonitorStateChanged(
            isActive: Boolean?,
            width: Int?,
            height: Int?,
            dpi: Int?,
            position: String?,
            refreshRate: Int?
        )
    }

    private var listener: VirtualMonitorStateListener? = null

    override fun onCreate(): Boolean {
        return true
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_VIRTUALMONITOR) return false

        isActive = (np.body["isActive"] as? Boolean)
        width = (np.body["width"] as? Number)?.toInt()
        height = (np.body["height"] as? Number)?.toInt()
        dpi = (np.body["dpi"] as? Number)?.toInt()
        position = np.body["position"] as? String
        refreshRate = (np.body["refreshRate"] as? Number)?.toInt()

        device.onPluginsChanged()
        listener?.onVirtualMonitorStateChanged(isActive, width, height, dpi, position, refreshRate)

        Log.d(TAG, "Virtual monitor status updated: active=$isActive, ${width}x${height}@${dpi}dpi, position=$position, ${refreshRate}Hz")
        return true
    }

    /** Set a listener to receive virtual monitor state change notifications. */
    fun setVirtualMonitorStateListener(listener: VirtualMonitorStateListener?) {
        this.listener = listener
    }

    /**
     * Send a request to enable the virtual monitor with specific configuration.
     *
     * @param width Display width in pixels
     * @param height Display height in pixels
     * @param dpi Display DPI
     * @param position Display position: "left", "right", "above", "below"
     * @param refreshRate Display refresh rate in Hz
     */
    fun enableMonitor(width: Int, height: Int, dpi: Int, position: String, refreshRate: Int) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_VIRTUALMONITOR_REQUEST,
            body = mapOf(
                "enableMonitor" to true,
                "width" to width,
                "height" to height,
                "dpi" to dpi,
                "position" to position,
                "refreshRate" to refreshRate,
            ),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Sent enable virtual monitor request: ${width}x${height}@${dpi}dpi, position=$position, ${refreshRate}Hz to ${device.name}")
    }

    /**
     * Send a request to disable the virtual monitor.
     */
    fun disableMonitor() {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_VIRTUALMONITOR_REQUEST,
            body = mapOf("disableMonitor" to true),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Sent disable virtual monitor request to ${device.name}")
    }

    companion object {
        private const val TAG = "VirtualMonitorPlugin"
        const val PACKET_TYPE_VIRTUALMONITOR = "cconnect.virtualmonitor"
        const val PACKET_TYPE_VIRTUALMONITOR_REQUEST = "cconnect.virtualmonitor.request"
    }
}
