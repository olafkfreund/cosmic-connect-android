/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.PingPlugin

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.MainActivity

/**
 * PingPlugin - Simple connectivity test plugin
 *
 * This plugin provides a simple way to test connectivity between Android and
 * COSMIC Desktop. Users can send pings with optional messages, and receive
 * ping notifications from the desktop.
 *
 * ## Protocol
 *
 * **Packet Type**: `cconnect.ping`
 * **Direction**: Bidirectional (Android <-> Desktop)
 */
class PingPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): PingPlugin
    }

    companion object {
        private const val TAG = "PingPlugin"
        private const val PACKET_TYPE_PING = "cconnect.ping"
    }

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_ping)

    override val description: String
        get() = context.getString(R.string.pref_plugin_ping_desc)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_PING)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_PING)

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Send a ping packet to the remote device
     */
    fun sendPing(message: String? = null) {
        try {
            val packet = PingPacketsFFI.createPing(message)
            device.sendPacket(TransferPacket(packet))

            if (message != null) {
                Log.d(TAG, "Ping sent with message: $message")
            } else {
                Log.d(TAG, "Ping sent")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ping", e)
        }
    }

    /**
     * Get ping statistics from the Rust FFI core
     */
    fun getPingStats(): org.cosmic.cosmicconnect.Core.PingStats? {
        return try {
            PingPacketsFFI.getPingStats()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ping stats", e)
            null
        }
    }

    // ========================================================================
    // Packet Handling
    // ========================================================================

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        val networkPacket = NetworkPacket.fromLegacy(np)

        if (networkPacket.type != PACKET_TYPE_PING) {
            Log.e(TAG, "Ping plugin should not receive packets other than pings!")
            return false
        }

        val isKeepalive = networkPacket.body["keepalive"] as? Boolean ?: false
        if (isKeepalive) {
            Log.d(TAG, "Received keepalive ping from ${device.name}")
            return true
        }

        val message = networkPacket.body["message"] as? String ?: "Ping!"
        displayPingNotification(message)

        return true
    }

    // ========================================================================
    // UI Integration
    // ========================================================================

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.send_ping)) { _ ->
            sendPing()
        }
    )

    // ========================================================================
    // Private Implementation
    // ========================================================================

    private fun displayPingNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionResult = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        val notificationId = if (message != "Ping!") {
            System.currentTimeMillis().toInt()
        } else {
            42
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.Channels.DEFAULT)
            .setContentTitle(device.name)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setTicker(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.notify(notificationId, notification)
    }
}
