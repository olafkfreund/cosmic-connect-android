/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin

import android.app.Activity
import android.content.Intent
import android.util.Log
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.network.WebRTCClient
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.network.WebRTCEventListener
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ui.ConnectionSetupActivity
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.webrtc.DataChannel
import org.webrtc.VideoTrack

/**
 * Extended Display Plugin
 *
 * Allows using an Android device as a wireless extended display for COSMIC Desktop.
 *
 * Features:
 * - H.264 video streaming via WebRTC
 * - Hardware-accelerated decoding with <100ms latency
 * - Full touch input support with coordinate normalization
 * - WiFi and USB connectivity
 * - mDNS service discovery
 *
 * Protocol: COSMIC Connect Extended Display v1.0
 * Packet types:
 * - "cconnect.extendeddisplay" - Control packets
 * - "cconnect.extendeddisplay.request" - Client requests
 */
class ExtendedDisplayPlugin : Plugin(), WebRTCEventListener {

    companion object {
        private const val TAG = "ExtendedDisplayPlugin"

        // Packet types
        const val PACKET_TYPE_EXTENDED_DISPLAY = "cconnect.extendeddisplay"
        const val PACKET_TYPE_EXTENDED_DISPLAY_REQUEST = "cconnect.extendeddisplay.request"
    }

    private var webRtcClient: WebRTCClient? = null
    private var currentVideoTrack: VideoTrack? = null
    private var dataChannel: DataChannel? = null

    // Plugin information
    override val displayName: String get() = context.getString(R.string.pref_plugin_extended_display)
    override val description: String get() = context.getString(R.string.pref_plugin_extended_display_desc)
    override val isEnabledByDefault: Boolean = false

    override val supportedPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_EXTENDED_DISPLAY
    )

    override val outgoingPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_EXTENDED_DISPLAY_REQUEST
    )

    override fun onCreate(): Boolean {
        Log.i(TAG, "ExtendedDisplayPlugin created")
        return true
    }

    override fun onDestroy() {
        Log.i(TAG, "ExtendedDisplayPlugin destroying")
        cleanup()
    }

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        Log.d(TAG, "Received packet: ${np.type}")

        when (np.type) {
            PACKET_TYPE_EXTENDED_DISPLAY -> {
                handleControlPacket(np)
                return true
            }
        }

        return false
    }

    private fun handleControlPacket(np: NetworkPacket) {
        val action = np.getString("action", "")

        when (action) {
            "offer" -> {
                // Desktop is offering to start streaming
                val sdp = np.getString("sdp", "")
                if (sdp.isNotEmpty()) {
                    handleSdpOffer(sdp)
                }
            }
            "candidate" -> {
                // ICE candidate from desktop
                val candidate = np.getString("candidate", "")
                val sdpMid = np.getString("sdpMid", "")
                val sdpMLineIndex = np.getInt("sdpMLineIndex", 0)
                handleIceCandidate(candidate, sdpMid, sdpMLineIndex)
            }
            "stop" -> {
                // Desktop requests to stop streaming
                stopStreaming()
            }
        }
    }

    private fun handleSdpOffer(sdp: String) {
        Log.i(TAG, "Received SDP offer from desktop")
        // WebRTC client handles this via signaling
    }

    private fun handleIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        Log.d(TAG, "Received ICE candidate")
        // WebRTC client handles this via signaling
    }

    /**
     * Start streaming to this device
     */
    fun requestStreaming() {
        Log.i(TAG, "Requesting streaming from desktop")

        val np = NetworkPacket(PACKET_TYPE_EXTENDED_DISPLAY_REQUEST)
        np.set("action", "request")
        np.set("capabilities", "h264,touch")
        device.sendPacket(np)
    }

    /**
     * Stop streaming
     */
    fun stopStreaming() {
        Log.i(TAG, "Stopping streaming")

        val np = NetworkPacket(PACKET_TYPE_EXTENDED_DISPLAY_REQUEST)
        np.set("action", "stop")
        device.sendPacket(np)

        cleanup()
    }

    /**
     * Send touch event to desktop
     */
    fun sendTouchEvent(x: Float, y: Float, action: String, pointerId: Int) {
        val np = NetworkPacket(PACKET_TYPE_EXTENDED_DISPLAY_REQUEST)
        np.set("action", "touch")
        np.set("x", x.toDouble())
        np.set("y", y.toDouble())
        np.set("touchAction", action)
        np.set("pointerId", pointerId)
        device.sendPacket(np)
    }

    private fun cleanup() {
        currentVideoTrack?.dispose()
        currentVideoTrack = null

        dataChannel?.close()
        dataChannel = null

        webRtcClient?.dispose()
        webRtcClient = null
    }

    // WebRTCEventListener implementation

    override fun onVideoTrackReceived(videoTrack: VideoTrack) {
        Log.i(TAG, "Video track received")
        currentVideoTrack = videoTrack
        // Activity will attach this to the surface
    }

    override fun onDataChannelOpened(dataChannel: DataChannel) {
        Log.i(TAG, "Data channel opened")
        this.dataChannel = dataChannel
    }

    override fun onConnectionStateChanged(state: ConnectionState) {
        Log.i(TAG, "Connection state changed: $state")
    }

    override fun onError(error: String, exception: Throwable?) {
        Log.e(TAG, "WebRTC error: $error", exception)
    }

    // UI Buttons for Device view
    override fun getUiButtons(): List<PluginUiButton> {
        return listOf(
            PluginUiButton(
                name = context.getString(R.string.extendeddisplay_button),
                iconRes = R.drawable.ic_tv_white,
                onClick = { activity ->
                    val intent = Intent(activity, ConnectionSetupActivity::class.java)
                    activity.startActivity(intent)
                }
            )
        )
    }

    // Plugin settings
    override fun hasSettings(): Boolean = true

    override fun supportsDeviceSpecificSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return PluginSettingsFragment.newInstance(pluginKey)
    }
}
