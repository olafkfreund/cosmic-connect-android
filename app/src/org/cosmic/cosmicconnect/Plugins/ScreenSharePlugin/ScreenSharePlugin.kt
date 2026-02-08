/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming.StreamSession
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ui.ScreenShareViewerActivity
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

        /** Called when the desktop requests to start streaming to this device. */
        fun onStreamStartRequested(width: Int, height: Int, codec: String, fps: Int) {}

        /** Called when the desktop stops streaming. */
        fun onStreamStopped() {}
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

    /** Active streaming session when receiving screen share from desktop. Null if not active. */
    var activeSession: StreamSession? = null
        private set

    private val listeners = mutableSetOf<Listener>()

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_screenshare)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_screenshare_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_SCREENSHARE,
        PACKET_TYPE_SCREENSHARE_START,
        PACKET_TYPE_SCREENSHARE_STOP,
    )
    override val outgoingPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_SCREENSHARE_REQUEST,
        PACKET_TYPE_SCREENSHARE_READY,
    )
    override val isEnabledByDefault: Boolean = false

    override fun onCreate(): Boolean {
        return true
    }

    override fun onDestroy() {
        stopStreamSession()
        dismissViewerNotification()
        listeners.clear()
    }

    override fun getUiButtons(): List<PluginUiButton> {
        if (activeSession == null) return emptyList()
        return listOf(
            PluginUiButton(
                context.getString(R.string.screenshare_viewer_title),
                R.drawable.ic_notification,
            ) { activity ->
                val intent = Intent(activity, ScreenShareViewerActivity::class.java).apply {
                    putExtra(ScreenShareViewerActivity.EXTRA_DEVICE_ID, device.deviceId)
                    putExtra(ScreenShareViewerActivity.EXTRA_MODE, ScreenShareViewerActivity.MODE_SCREENSHARE)
                }
                activity.startActivity(intent)
            }
        )
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        return when (np.type) {
            PACKET_TYPE_SCREENSHARE -> handleStatusPacket(np)
            PACKET_TYPE_SCREENSHARE_START -> handleStartPacket(np)
            PACKET_TYPE_SCREENSHARE_STOP -> handleStopPacket(np)
            else -> false
        }
    }

    private fun handleStatusPacket(np: NetworkPacket): Boolean {
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

    /**
     * Handles screenshare.start from the desktop.
     * Creates a StreamSession, sends back a ready packet with the TCP port,
     * and fires a notification for the user to open the viewer.
     */
    private fun handleStartPacket(np: NetworkPacket): Boolean {
        val startWidth = (np.body["width"] as? Number)?.toInt() ?: 1920
        val startHeight = (np.body["height"] as? Number)?.toInt() ?: 1080
        val startCodec = (np.body["codec"] as? String) ?: "h264"
        val startFps = (np.body["fps"] as? Number)?.toInt() ?: 30

        Log.i(TAG, "Desktop requests screen share: ${startWidth}x${startHeight}, codec=$startCodec, fps=$startFps")

        // Create and prepare the stream session
        val port = createStreamSession(startWidth, startHeight, startFps, startCodec)

        // Send ready packet with TCP port
        val readyPacket = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_SCREENSHARE_READY,
            body = mapOf("tcpPort" to port),
        )
        device.sendPacket(TransferPacket(readyPacket))
        Log.i(TAG, "Sent screenshare.ready with tcpPort=$port")

        // Update state
        isSharing = true
        width = startWidth
        height = startHeight
        codec = startCodec
        fps = startFps
        direction = DIRECTION_DESKTOP_TO_PHONE

        // Notify listeners
        listeners.forEach { it.onStreamStartRequested(startWidth, startHeight, startCodec, startFps) }

        // Fire notification to open viewer
        fireViewerNotification()

        device.onPluginsChanged()
        return true
    }

    private fun handleStopPacket(np: NetworkPacket): Boolean {
        Log.i(TAG, "Desktop stopped screen share")
        stopStreamSession()

        isSharing = false
        direction = DIRECTION_PHONE_TO_DESKTOP

        listeners.forEach { it.onStreamStopped() }
        dismissViewerNotification()

        device.onPluginsChanged()
        return true
    }

    private fun fireViewerNotification() {
        val intent = Intent(context, ScreenShareViewerActivity::class.java).apply {
            putExtra(ScreenShareViewerActivity.EXTRA_DEVICE_ID, device.deviceId)
            putExtra(ScreenShareViewerActivity.EXTRA_MODE, ScreenShareViewerActivity.MODE_SCREENSHARE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.Channels.HIGHPRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.screenshare_viewer_title))
            .setContentText(context.getString(R.string.screenshare_viewer_connecting))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot show notification: missing POST_NOTIFICATIONS permission", e)
        }
    }

    private fun dismissViewerNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
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

    /**
     * Creates and prepares a StreamSession for receiving screen share from desktop.
     * Returns the TCP port the desktop should connect to.
     */
    fun createStreamSession(width: Int, height: Int, fps: Int, codec: String): Int {
        // Clean up any existing session
        activeSession?.stop()

        val session = StreamSession(width, height, fps, codec)
        session.prepare()
        activeSession = session

        Log.i(TAG, "Created stream session on port ${session.tcpPort}")
        return session.tcpPort
    }

    /**
     * Stops and clears the active streaming session.
     */
    fun stopStreamSession() {
        activeSession?.stop()
        activeSession = null
        Log.i(TAG, "Stopped stream session")
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
        private const val NOTIFICATION_ID = 0x5C5E // "SCSE" = ScreenShare

        const val PACKET_TYPE_SCREENSHARE = "cconnect.screenshare"
        const val PACKET_TYPE_SCREENSHARE_REQUEST = "cconnect.screenshare.request"
        const val PACKET_TYPE_SCREENSHARE_START = "cconnect.screenshare.start"
        const val PACKET_TYPE_SCREENSHARE_STOP = "cconnect.screenshare.stop"
        const val PACKET_TYPE_SCREENSHARE_READY = "cconnect.screenshare.ready"

        const val DIRECTION_PHONE_TO_DESKTOP = "phone_to_desktop"
        const val DIRECTION_DESKTOP_TO_PHONE = "desktop_to_phone"
    }
}
