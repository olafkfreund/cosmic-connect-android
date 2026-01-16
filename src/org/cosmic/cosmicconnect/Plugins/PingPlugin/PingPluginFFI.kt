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
import org.cosmic.cosmicconnect.Core.PluginManager
import org.cosmic.cosmicconnect.Core.PluginManagerProvider
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.MainActivity

/**
 * PingPluginFFI - Ping plugin using Rust FFI core
 *
 * Drop-in replacement for PingPlugin that uses the Rust FFI PluginManager
 * for cross-platform ping functionality.
 *
 * ## Features
 *
 * - Send ping packets to remote devices
 * - Receive ping packets and display notifications
 * - Optional message in ping
 * - Ping statistics tracking (via FFI)
 *
 * ## Architecture
 *
 * - **Sending**: Uses FFI PluginManager.createPing() to create packet
 * - **Receiving**: Routes incoming packets to FFI, displays Android notification
 * - **Stats**: FFI tracks ping counts (sent/received)
 *
 * ## Usage
 *
 * Same as old PingPlugin (drop-in replacement):
 * ```kotlin
 * val pingPlugin = PingPluginFFI()
 * pingPlugin.setContext(context, device)
 * pingPlugin.onCreate()
 * // Send ping from UI
 * device.sendPacket(pingPlugin.createPingPacket())
 * // Receive ping
 * pingPlugin.onPacketReceived(pingPacket)
 * ```
 */
@LoadablePlugin
class PingPluginFFI : Plugin() {

    companion object {
        private const val TAG = "PingPluginFFI"
        const val PACKET_TYPE_PING = "kdeconnect.ping"
    }

    // Plugin manager instance (shared across all plugins)
    private var pluginManager: PluginManager? = null

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_ping)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_ping_desc)

    override fun onCreate(): Boolean {
        try {
            // Get shared plugin manager instance
            pluginManager = PluginManagerProvider.getInstance()

            // Register ping plugin with FFI
            try {
                // FIXME: PluginManager.Plugins enum not available - temporarily commented out
                // if (!pluginManager!!.hasPlugin(PluginManager.Plugins.PING)) {
                //     pluginManager!!.registerPlugin(PluginManager.Plugins.PING)
                //     Log.i(TAG, "Registered ping plugin with FFI")
                // }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register ping plugin", e)
                // Continue anyway - ping will still work without stats
            }

            Log.i(TAG, "✅ PingPluginFFI initialized")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize PingPluginFFI", e)
            return false
        }
    }

    override fun onDestroy() {
        // Note: We don't unregister the plugin from FFI here because
        // the plugin manager is shared across all devices and other
        // devices might still need the ping plugin.
        Log.i(TAG, "PingPluginFFI destroyed")
    }

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        if (np.type != PACKET_TYPE_PING) {
            Log.e(TAG, "Ping plugin should not receive packets other than pings!")
            return false
        }

        try {
            // Convert legacy NetworkPacket to FFI NetworkPacket
            val ffiPacket = convertLegacyPacket(np)

            // Route to FFI plugin manager (this increments ping stats)
            pluginManager?.routePacket(ffiPacket)

            // Display notification to user
            displayPingNotification(np)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process ping packet", e)
            // Still try to show notification even if FFI fails
            displayPingNotification(np)
            return true
        }
    }

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.send_ping)) { parentActivity ->
            sendPing(message = null)
        }
    )

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_PING)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_PING)

    /**
     * Send a ping packet to the remote device
     *
     * @param message Optional message to include in ping
     */
    fun sendPing(message: String? = null) {
        if (!isDeviceInitialized) {
            Log.w(TAG, "Device not initialized, cannot send ping")
            return
        }

        try {
            // Create ping packet via FFI
            val pingPacket = pluginManager?.createPing(message)

            if (pingPacket != null) {
                // Convert to legacy NetworkPacket for device.sendPacket()
                val legacyPacket = convertToLegacyPacket(pingPacket)
                device.sendPacket(legacyPacket)

                Log.d(TAG, "Ping sent" + if (message != null) " with message: $message" else "")
            } else {
                // Fallback: create ping manually if FFI fails
                val legacyPacket = LegacyNetworkPacket(PACKET_TYPE_PING)
                if (message != null) {
                    legacyPacket.set("message", message)
                }
                device.sendPacket(legacyPacket)

                Log.w(TAG, "Ping sent (FFI unavailable, used fallback)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send ping", e)
        }
    }

    /**
     * Get ping statistics from FFI
     *
     * @return PingStats with sent/received counts, or null if unavailable
     */
    fun getPingStats(): org.cosmic.cosmicconnect.Core.PingStats? {
        return try {
            pluginManager?.getPingStats()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ping stats", e)
            null
        }
    }

    /**
     * Display ping notification to user
     *
     * Shows a notification with the ping message (if present) or default "Ping!" message.
     */
    private fun displayPingNotification(np: LegacyNetworkPacket) {
        val mutableUpdateFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val resultPendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), mutableUpdateFlags
        )

        val (id: Int, message: String) = if (np.has("message")) {
            val id = System.currentTimeMillis().toInt()
            Pair(id, np.getString("message"))
        } else {
            val id = 42 // Unique id for default ping notification
            Pair(id, "Ping!")
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionResult = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                // If notifications not allowed, show toast instead
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        val notificationManager = context.getSystemService<NotificationManager>()!!

        val notification = NotificationCompat.Builder(context, NotificationHelper.Channels.DEFAULT)
            .setContentTitle(device.name)
            .setContentText(message)
            .setContentIntent(resultPendingIntent)
            .setTicker(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        notificationManager.notify(id, notification)
    }

    /**
     * Convert legacy NetworkPacket to FFI NetworkPacket
     */
    private fun convertLegacyPacket(legacy: LegacyNetworkPacket): NetworkPacket {
        val body = mutableMapOf<String, Any>()

        if (legacy.has("message")) {
            body["message"] = legacy.getString("message")
        }

        return NetworkPacket.create(PACKET_TYPE_PING, body)
    }

    /**
     * Convert FFI NetworkPacket to legacy NetworkPacket
     */
    private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
        val legacy = LegacyNetworkPacket(PACKET_TYPE_PING)

        // Copy message field if present
        val message = ffi.body["message"]
        if (message is String) {
            legacy.set("message", message)
        }

        return legacy
    }
}
