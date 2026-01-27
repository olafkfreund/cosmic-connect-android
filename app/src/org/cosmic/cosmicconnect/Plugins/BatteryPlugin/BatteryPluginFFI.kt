package org.cosmic.cosmicconnect.Plugins.BatteryPlugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import org.cosmic.cosmicconnect.Core.BatteryState
import org.cosmic.cosmicconnect.Core.CosmicConnectCore
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.PluginManager
import org.cosmic.cosmicconnect.Core.PluginManagerProvider
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R

/**
 * BatteryPluginFFI - Battery plugin using Rust FFI core
 *
 * Drop-in replacement for BatteryPlugin that uses the Rust FFI PluginManager
 * for cross-platform battery state sharing.
 *
 * ## Architecture
 *
 * - Monitors Android battery events via BroadcastReceiver (unchanged)
 * - Sends battery state to FFI PluginManager → Rust core → Remote device
 * - Receives battery state from remote devices via FFI
 * - Maintains backward-compatible API (Plugin interface)
 *
 * ## Migration from Old BatteryPlugin
 *
 * This replaces the old NetworkPacket-based implementation with FFI calls:
 * - Old: NetworkPacket created manually, sent via Device.sendPacket()
 * - New: BatteryState object → FFI PluginManager.updateBattery()
 * - Old: Remote battery from NetworkPacket fields
 * - New: Remote battery from FFI PluginManager.getRemoteBattery()
 *
 * ## Usage
 *
 * Same as old BatteryPlugin (drop-in replacement):
 * ```kotlin
 * val batteryPlugin = BatteryPluginFFI()
 * batteryPlugin.setContext(context, device)
 * batteryPlugin.onCreate()
 * // ...
 * val remoteBattery = batteryPlugin.remoteBatteryInfo
 * batteryPlugin.onDestroy()
 * ```
 */
@LoadablePlugin
class BatteryPluginFFI : Plugin() {

    companion object {
        private const val TAG = "BatteryPluginFFI"
        const val PACKET_TYPE_BATTERY = "cconnect.battery"

        // Threshold events (keep in sync with KDE Connect protocol)
        private const val THRESHOLD_EVENT_NONE = 0
        private const val THRESHOLD_EVENT_BATTERY_LOW = 1

        /**
         * Check if battery info indicates low battery
         */
        fun isLowBattery(info: DeviceBatteryInfo): Boolean {
            return info.thresholdEvent == THRESHOLD_EVENT_BATTERY_LOW
        }
    }

    // Plugin manager instance (shared across all plugins)
    private var pluginManager: PluginManager? = null

    // Current local battery state
    private var localBatteryState: BatteryState? = null

    /**
     * The latest battery information about the linked device.
     * Will be null if the linked device has not sent us any such information yet.
     *
     * This maintains API compatibility with old BatteryPlugin.
     *
     * @return the most recent battery info from the remote device, or null
     */
    var remoteBatteryInfo: DeviceBatteryInfo? = null
        private set
        get() {
            // Fetch latest from FFI on each access
            updateRemoteBatteryInfo()
            return field
        }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_battery)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_battery_desc)

    /**
     * Battery broadcast receiver
     *
     * Monitors Android battery changes and updates FFI plugin manager.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        var wasLowBattery: Boolean = false // Triggers low battery notification

        override fun onReceive(context: Context, batteryIntent: Intent) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            // Calculate current charge percentage
            val currentCharge = if (level == -1) {
                localBatteryState?.currentCharge ?: 50 // Default to 50% if unknown
            } else {
                level * 100 / scale
            }

            // Determine charging status
            val isCharging = if (plugged == -1) {
                localBatteryState?.isCharging ?: false
            } else {
                0 != plugged
            }

            // Determine threshold event
            val thresholdEvent = when (batteryIntent.action) {
                Intent.ACTION_BATTERY_OKAY -> THRESHOLD_EVENT_NONE
                Intent.ACTION_BATTERY_LOW -> if (!wasLowBattery && !isCharging) {
                    THRESHOLD_EVENT_BATTERY_LOW
                } else {
                    THRESHOLD_EVENT_NONE
                }
                else -> THRESHOLD_EVENT_NONE
            }

            // Update wasLowBattery state
            wasLowBattery = when (batteryIntent.action) {
                Intent.ACTION_BATTERY_OKAY -> false
                Intent.ACTION_BATTERY_LOW -> true
                else -> wasLowBattery
            }

            // Check if battery state changed
            val previousState = localBatteryState
            val stateChanged = previousState == null ||
                    isCharging != previousState.isCharging ||
                    currentCharge != previousState.currentCharge ||
                    thresholdEvent != previousState.thresholdEvent

            if (stateChanged) {
                // Create new battery state
                val newState = BatteryState(
                    isCharging = isCharging,
                    currentCharge = currentCharge,
                    thresholdEvent = thresholdEvent
                )
                localBatteryState = newState

                // Send to FFI plugin manager
                sendBatteryUpdate(newState)
            }
        }
    }

    override fun onCreate(): Boolean {
        try {
            // Get shared plugin manager instance
            pluginManager = PluginManagerProvider.getInstance()

            // Register battery plugin with FFI
            try {
                // FIXME: PluginManager.Plugins enum not available - temporarily commented out
                // if (!pluginManager!!.hasPlugin(PluginManager.Plugins.BATTERY)) {
                //     pluginManager!!.registerPlugin(PluginManager.Plugins.BATTERY)
                //     Log.i(TAG, "Registered battery plugin with FFI")
                // }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register battery plugin", e)
                // Continue anyway - battery monitoring will still work locally
            }

            // Register battery broadcast receiver
            val intentFilter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
            }
            val currentState = context.registerReceiver(receiver, intentFilter)

            // Process initial battery state
            if (currentState != null) {
                receiver.onReceive(context, currentState)
            }

            Log.i(TAG, "✅ BatteryPluginFFI initialized")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize BatteryPluginFFI", e)
            return false
        }
    }

    override fun onDestroy() {
        try {
            // Unregister battery broadcast receiver
            context.unregisterReceiver(receiver)

            // Note: We don't unregister the plugin from FFI here because
            // the plugin manager is shared across all devices and other
            // devices might still need the battery plugin.

            Log.i(TAG, "BatteryPluginFFI destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying BatteryPluginFFI", e)
        }
    }

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        if (PACKET_TYPE_BATTERY != np.type) {
            return false
        }

        try {
            // Convert legacy NetworkPacket to FFI NetworkPacket
            val ffiPacket = convertLegacyPacket(np)

            // Route to FFI plugin manager
            pluginManager?.routePacket(ffiPacket)

            // Update cached remote battery info
            updateRemoteBatteryInfo()

            // Notify device of plugin changes
            device.onPluginsChanged()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process battery packet", e)
            return false
        }
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_BATTERY)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_BATTERY)

    /**
     * Send battery update to FFI plugin manager
     */
    private fun sendBatteryUpdate(state: BatteryState) {
        try {
            pluginManager?.updateBattery(state)
            Log.d(TAG, "Battery updated: ${state.currentCharge}%, charging=${state.isCharging}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send battery update", e)
        }
    }

    /**
     * Update cached remote battery info from FFI
     */
    private fun updateRemoteBatteryInfo() {
        try {
            val remoteBattery = pluginManager?.getRemoteBattery()
            if (remoteBattery != null) {
                remoteBatteryInfo = DeviceBatteryInfo(
                    currentCharge = remoteBattery.currentCharge,
                    isCharging = remoteBattery.isCharging,
                    thresholdEvent = remoteBattery.thresholdEvent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remote battery info", e)
        }
    }

    /**
     * Convert legacy NetworkPacket to FFI NetworkPacket
     */
    private fun convertLegacyPacket(legacy: LegacyNetworkPacket): NetworkPacket {
        // Extract battery data from legacy packet
        val body = mapOf(
            "currentCharge" to legacy.getInt("currentCharge", 0),
            "isCharging" to legacy.getBoolean("isCharging", false),
            "thresholdEvent" to legacy.getInt("thresholdEvent", 0)
        )

        // Create FFI NetworkPacket
        return NetworkPacket.create(PACKET_TYPE_BATTERY, body)
    }

}
