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
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.MainActivity

/**
 * PingPlugin - Simple connectivity test plugin
 *
 * This plugin provides a simple way to test connectivity between Android and
 * COSMIC Desktop. Users can send pings with optional messages, and receive
 * ping notifications from the desktop.
 *
 * ## Features
 *
 * - **Send Pings**: Send ping packets with optional custom messages
 * - **Receive Pings**: Display notifications for incoming pings
 * - **Statistics**: Track sent/received ping counts via FFI
 * - **Fallback UI**: Shows toast if notifications are disabled (Android 13+)
 *
 * ## Protocol
 *
 * **Packet Type**: `cconnect.ping`
 *
 * **Direction**: Bidirectional (Android ↔ Desktop)
 *
 * **Body Fields**:
 * - `message` (optional): String - Custom message to display in notification
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Send simple ping
 * plugin.sendPing()
 *
 * // Send ping with message
 * plugin.sendPing("Hello from Android!")
 *
 * // Get statistics
 * val stats = plugin.getPingStats()
 * Log.d(TAG, "Sent: ${stats.pingsSent}, Received: ${stats.pingsReceived}")
 * ```
 *
 * @see PingPacketsFFI
 */
@LoadablePlugin
class PingPlugin : Plugin() {

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
     *
     * Creates a ping packet using the FFI wrapper and sends it to the connected
     * device. The ping can optionally include a custom message to be displayed
     * in the notification on the receiving device.
     *
     * ## Behavior
     * - Without message: Sends basic ping (displays "Ping!" on receiver)
     * - With message: Sends ping with custom message
     * - Statistics: Increments ping sent counter in Rust core
     *
     * ## Error Handling
     * Logs errors but does not throw exceptions. Failed pings are silently ignored.
     *
     * @param message Optional custom message to include in the ping
     */
    fun sendPing(message: String? = null) {
        if (!isDeviceInitialized) {
            Log.w(TAG, "Device not initialized, cannot send ping")
            return
        }

        try {
            val packet = PingPacketsFFI.createPing(message)
            device.sendPacket(packet.toLegacyPacket())

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
     *
     * Returns cumulative ping statistics tracked by the Rust plugin manager.
     * Only counts pings created via FFI (not legacy packets).
     *
     * @return PingStats with pingsReceived and pingsSent counts, or null if unavailable
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
        // Convert to immutable NetworkPacket for type-safe inspection
        val networkPacket = NetworkPacket.fromLegacy(np)

        if (networkPacket.type != PACKET_TYPE_PING) {
            Log.e(TAG, "Ping plugin should not receive packets other than pings!")
            return false
        }

        // Extract message from packet body
        val message = networkPacket.body["message"] as? String ?: "Ping!"

        // Display notification to user
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

    /**
     * Display ping notification to user
     *
     * Shows a notification with the ping message. If notification permission
     * is denied on Android 13+, falls back to showing a toast message.
     *
     * @param message Message to display in notification/toast
     */
    private fun displayPingNotification(message: String) {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionResult = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                // Fallback: Show toast if notifications not allowed
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        // Determine notification ID
        val notificationId = if (message != "Ping!") {
            // Unique ID for custom messages (allows multiple notifications)
            System.currentTimeMillis().toInt()
        } else {
            // Fixed ID for default pings (replaces previous notification)
            42
        }

        // Create pending intent for notification tap
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
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

        // Show notification
        val notificationManager = context.getSystemService<NotificationManager>()!!
        notificationManager.notify(notificationId, notification)
    }
}
