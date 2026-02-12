package org.cosmicext.connect.Core

import android.util.Log
import uniffi.cosmic_ext_connect_core.*

/**
 * BatteryState - Battery status information
 */
data class BatteryState(
    val isCharging: Boolean,
    val currentCharge: Int, // 0-100
    val thresholdEvent: Int = 0
) {

    companion object {
        /**
         * Convert FFI battery state to Kotlin BatteryState
         */
        internal fun fromFfiBatteryState(ffi: FfiBatteryState): BatteryState {
            return BatteryState(
                isCharging = ffi.isCharging,
                currentCharge = ffi.currentCharge,
                thresholdEvent = ffi.thresholdEvent
            )
        }
    }

    /**
     * Convert to FFI battery state for Rust calls
     */
    internal fun toFfiBatteryState(): FfiBatteryState {
        return FfiBatteryState(
            isCharging = isCharging,
            currentCharge = currentCharge,
            thresholdEvent = thresholdEvent
        )
    }

    /**
     * Get battery level category
     */
    val level: BatteryLevel
        get() = when {
            currentCharge >= 80 -> BatteryLevel.HIGH
            currentCharge >= 50 -> BatteryLevel.MEDIUM
            currentCharge >= 20 -> BatteryLevel.LOW
            else -> BatteryLevel.CRITICAL
        }

    override fun toString(): String {
        val status = if (isCharging) "Charging" else "Discharging"
        return "BatteryState($currentCharge%, $status)"
    }
}

/**
 * BatteryLevel - Battery charge level categories
 */
enum class BatteryLevel {
    HIGH,    // 80-100%
    MEDIUM,  // 50-79%
    LOW,     // 20-49%
    CRITICAL // 0-19%
}

/**
 * PingStats - Statistics for ping plugin
 */
data class PingStats(
    val pingsReceived: ULong,
    val pingsSent: ULong
) {

    companion object {
        /**
         * Convert FFI ping stats to Kotlin PingStats
         */
        internal fun fromFfiPingStats(ffi: FfiPingStats): PingStats {
            return PingStats(
                pingsReceived = ffi.pingsReceived,
                pingsSent = ffi.pingsSent
            )
        }
    }

    override fun toString(): String {
        return "PingStats(received=$pingsReceived, sent=$pingsSent)"
    }
}

/**
 * PluginCapabilities - Plugin capabilities (incoming/outgoing packet types)
 */
data class PluginCapabilities(
    val incoming: List<String>,
    val outgoing: List<String>
) {

    companion object {
        /**
         * Convert FFI capabilities to Kotlin PluginCapabilities
         */
        internal fun fromFfiCapabilities(ffi: FfiCapabilities): PluginCapabilities {
            return PluginCapabilities(
                incoming = ffi.incoming,
                outgoing = ffi.outgoing
            )
        }
    }

    /**
     * Check if this plugin handles a specific packet type
     */
    fun handlesPacketType(packetType: String): Boolean {
        return packetType in incoming
    }

    /**
     * Check if this plugin sends a specific packet type
     */
    fun sendsPacketType(packetType: String): Boolean {
        return packetType in outgoing
    }

    override fun toString(): String {
        return "PluginCapabilities(incoming=${incoming.size}, outgoing=${outgoing.size})"
    }
}

/**
 * PluginManager - Manages COSMIC Connect plugins
 *
 * Provides a clean Kotlin API for the Rust plugin system.
 *
 * ## Supported Plugins
 * - **ping**: Connectivity testing
 * - **battery**: Battery state sharing
 * - **share**: File sharing (requires additional setup)
 * - **clipboard**: Clipboard sync (requires additional setup)
 *
 * ## Usage
 * ```kotlin
 * val manager = PluginManager.create()
 *
 * // Register plugins
 * manager.registerPlugin("ping")
 * manager.registerPlugin("battery")
 *
 * // Update battery state (will be sent to connected devices)
 * manager.updateBattery(BatteryState(isCharging = true, currentCharge = 85))
 *
 * // Send ping
 * val pingPacket = manager.createPing("Hello from Android!")
 *
 * // Route incoming packet to appropriate plugin
 * manager.routePacket(receivedPacket)
 *
 * // Cleanup
 * manager.shutdownAll()
 * ```
 */
class PluginManager private constructor(
    private val manager: uniffi.cosmic_ext_connect_core.PluginManager
) {

    companion object {
        private const val TAG = "PluginManager"

        /**
         * Available plugin names
         */
        object Plugins {
            const val PING = "ping"
            const val BATTERY = "battery"
            // More plugins will be added as they're implemented in the core
        }

        /**
         * Create a new plugin manager
         *
         * @return PluginManager instance
         * @throws CosmicExtConnectException if creation fails
         */
        fun create(): PluginManager {
            return try {
                Log.i(TAG, "Creating plugin manager")
                val manager = createPluginManager()
                Log.i(TAG, "✅ Plugin manager created")
                PluginManager(manager)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create plugin manager", e)
                throw CosmicExtConnectException("Failed to create plugin manager: ${e.message}", e)
            }
        }
    }

    /**
     * Register a plugin by name
     *
     * @param pluginName Plugin name (e.g., "ping", "battery")
     * @throws CosmicExtConnectException if registration fails
     */
    fun registerPlugin(pluginName: String) {
        try {
            Log.i(TAG, "Registering plugin: $pluginName")
            manager.registerPlugin(pluginName)
            Log.i(TAG, "✅ Plugin registered: $pluginName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register plugin: $pluginName", e)
            throw CosmicExtConnectException("Failed to register plugin '$pluginName': ${e.message}", e)
        }
    }

    /**
     * Unregister a plugin
     *
     * @param pluginName Plugin name to unregister
     * @throws CosmicExtConnectException if unregistration fails
     */
    fun unregisterPlugin(pluginName: String) {
        try {
            Log.i(TAG, "Unregistering plugin: $pluginName")
            manager.unregisterPlugin(pluginName)
            Log.i(TAG, "✅ Plugin unregistered: $pluginName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to unregister plugin: $pluginName", e)
            throw CosmicExtConnectException("Failed to unregister plugin '$pluginName': ${e.message}", e)
        }
    }

    /**
     * Route an incoming packet to the appropriate plugin
     *
     * The plugin manager examines the packet type and routes it to
     * the plugin that handles that packet type.
     *
     * @param packet Incoming network packet
     * @throws CosmicExtConnectException if routing fails
     */
    fun routePacket(packet: NetworkPacket) {
        try {
            Log.d(TAG, "Routing packet: ${packet.type}")
            val ffiPacket = packet.toFfiPacket()
            manager.routePacket(ffiPacket)
            Log.d(TAG, "✅ Packet routed: ${packet.type}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to route packet: ${packet.type}", e)
            throw CosmicExtConnectException("Failed to route packet: ${e.message}", e)
        }
    }

    /**
     * Get all plugin capabilities (incoming and outgoing packet types)
     *
     * @return PluginCapabilities containing all supported packet types
     */
    fun getCapabilities(): PluginCapabilities {
        return try {
            val ffiCaps = manager.getCapabilities()
            PluginCapabilities.fromFfiCapabilities(ffiCaps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get capabilities", e)
            PluginCapabilities(emptyList(), emptyList())
        }
    }

    /**
     * Check if a plugin is registered
     *
     * @param pluginName Plugin name to check
     * @return true if plugin is registered
     */
    fun hasPlugin(pluginName: String): Boolean {
        return try {
            manager.hasPlugin(pluginName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get list of registered plugin names
     *
     * @return List of plugin names
     */
    fun getPluginNames(): List<String> {
        return try {
            manager.pluginNames()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get plugin names", e)
            emptyList()
        }
    }

    /**
     * Shutdown all plugins and cleanup resources
     *
     * Call this when the app is being destroyed.
     *
     * @throws CosmicExtConnectException if shutdown fails
     */
    fun shutdownAll() {
        try {
            Log.i(TAG, "Shutting down all plugins")
            manager.shutdownAll()
            Log.i(TAG, "✅ All plugins shut down")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to shutdown plugins", e)
            throw CosmicExtConnectException("Failed to shutdown plugins: ${e.message}", e)
        }
    }

    // ========================================================================
    // Battery Plugin Methods
    // ========================================================================

    /**
     * Update local battery state
     *
     * This updates the battery plugin with current battery status.
     * The state will be sent to connected devices.
     *
     * @param state Current battery state
     * @throws CosmicExtConnectException if update fails
     */
    fun updateBattery(state: BatteryState) {
        try {
            Log.d(TAG, "Updating battery: ${state.currentCharge}%")
            val ffiState = state.toFfiBatteryState()
            manager.updateBattery(ffiState)
            Log.d(TAG, "✅ Battery updated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update battery", e)
            throw CosmicExtConnectException("Failed to update battery: ${e.message}", e)
        }
    }

    /**
     * Get remote device battery state
     *
     * @return Battery state from remote device, or null if not available
     */
    fun getRemoteBattery(): BatteryState? {
        return try {
            manager.getRemoteBattery()?.let { BatteryState.fromFfiBatteryState(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remote battery", e)
            null
        }
    }

    // ========================================================================
    // Ping Plugin Methods
    // ========================================================================

    /**
     * Create a ping packet
     *
     * @param message Optional message to include in ping
     * @return NetworkPacket ready to send
     * @throws CosmicExtConnectException if ping creation fails
     */
    fun createPing(message: String? = null): NetworkPacket {
        return try {
            Log.d(TAG, "Creating ping packet")
            val ffiPacket = manager.createPing(message)
            NetworkPacket.fromFfiPacket(ffiPacket)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create ping", e)
            throw CosmicExtConnectException("Failed to create ping: ${e.message}", e)
        }
    }

    /**
     * Get ping statistics
     *
     * @return PingStats with sent/received counts
     */
    fun getPingStats(): PingStats {
        return try {
            val ffiStats = manager.getPingStats()
            PingStats.fromFfiPingStats(ffiStats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ping stats", e)
            PingStats(0u, 0u)
        }
    }
}
