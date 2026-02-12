/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.VirtualMonitorPlugin

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.ScreenSharePlugin.ScreenSharePlugin
import org.cosmicext.connect.Plugins.ScreenSharePlugin.streaming.StreamSession
import org.cosmicext.connect.Plugins.ScreenSharePlugin.ui.ScreenShareViewerActivity
import org.cosmicext.connect.Plugins.di.PluginCreator
import org.cosmicext.connect.R

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

    /** Current display width in pixels. Null if not configured or out of bounds. */
    var width: Int? = null
        private set

    /** Current display height in pixels. Null if not configured or out of bounds. */
    var height: Int? = null
        private set

    /** Current display DPI. Null if not configured or out of bounds. */
    var dpi: Int? = null
        private set

    /** Current display position relative to desktop: "left", "right", "above", "below". Null if not configured. */
    var position: String? = null
        private set

    /** Current refresh rate in Hz. Null if not configured or out of bounds. */
    var refreshRate: Int? = null
        private set

    /** The currently active delegated stream session, if any. */
    var activeStreamSession: StreamSession? = null
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

    private val listeners = mutableSetOf<VirtualMonitorStateListener>()

    override fun onCreate(): Boolean {
        return true
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_VIRTUALMONITOR) return false

        isActive = (np.body["isActive"] as? Boolean)
        width = safeInt(np.body["width"], min = 1, max = 7680)
        height = safeInt(np.body["height"], min = 1, max = 4320)
        dpi = safeInt(np.body["dpi"], min = 1, max = 600)
        position = np.body["position"] as? String
        refreshRate = safeInt(np.body["refreshRate"], min = 1, max = 240)

        // Delegate streaming to ScreenSharePlugin
        val screenSharePlugin = device.getPlugin(ScreenSharePlugin::class.java)
        when {
            screenSharePlugin != null && isActive == true && width != null && height != null -> {
                try {
                    screenSharePlugin.createStreamSession(
                        width!!, height!!, refreshRate ?: 60, "h264"
                    )
                    activeStreamSession = screenSharePlugin.activeSession
                    Log.i(TAG, "Delegated stream session to ScreenSharePlugin (${width}x${height}@${refreshRate ?: 60}Hz)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delegate to ScreenSharePlugin", e)
                }
            }
            screenSharePlugin != null && isActive == false -> {
                activeStreamSession = null
                screenSharePlugin.stopStreamSession()
                Log.i(TAG, "Stopped delegated stream session")
            }
            screenSharePlugin == null && isActive == true && width != null && height != null -> {
                Log.w(TAG, "Cannot activate virtual monitor: ScreenSharePlugin not loaded or disabled")
            }
        }

        device.onPluginsChanged()
        listeners.forEach { it.onVirtualMonitorStateChanged(isActive, width, height, dpi, position, refreshRate) }

        Log.d(TAG, "Virtual monitor status updated: active=$isActive, ${width}x${height}@${dpi}dpi, position=$position, ${refreshRate}Hz")
        return true
    }

    override fun onDestroy() {
        listeners.clear()
        activeStreamSession = null
        // Stop any delegated stream session
        device.getPlugin(ScreenSharePlugin::class.java)?.stopStreamSession()
    }

    override fun getUiButtons(): List<PluginUiButton> = listOf(
        PluginUiButton(
            context.getString(R.string.pref_plugin_virtualmonitor),
            R.drawable.ic_notification,
        ) { activity ->
            // When launched from legacy Activity, open the viewer if session is active
            if (activeStreamSession != null) {
                val intent = Intent(activity, ScreenShareViewerActivity::class.java).apply {
                    putExtra(ScreenShareViewerActivity.EXTRA_DEVICE_ID, device.deviceId)
                    putExtra(ScreenShareViewerActivity.EXTRA_MODE, ScreenShareViewerActivity.MODE_VIRTUALMONITOR)
                }
                activity.startActivity(intent)
            }
            // From Compose NavGraph, navigation is handled separately
        }
    )

    /** Add a listener to receive virtual monitor state change notifications. */
    fun addVirtualMonitorStateListener(listener: VirtualMonitorStateListener) {
        listeners.add(listener)
    }

    /** Remove a previously registered listener. */
    fun removeVirtualMonitorStateListener(listener: VirtualMonitorStateListener) {
        listeners.remove(listener)
    }

    /**
     * Validate and clamp a numeric value within bounds.
     * Returns null if the value is not a number or outside the given range.
     */
    private fun safeInt(value: Any?, min: Int = 0, max: Int = Int.MAX_VALUE): Int? {
        val num = value as? Number ?: return null
        val i = num.toInt()
        return if (i in min..max) i else null
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
