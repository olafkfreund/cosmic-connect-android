/*
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.PingPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.PingStats
import org.cosmic.cosmicconnect.Core.PluginManager
import org.cosmic.cosmicconnect.Core.PluginManagerProvider

/**
 * PingPacketsFFI - FFI wrapper for ping packet creation and statistics
 *
 * This object provides Kotlin-friendly access to the Rust FFI ping functions
 * in cosmic-connect-core. It wraps the PluginManager's ping methods to create
 * type-safe, immutable NetworkPacket instances.
 *
 * ## Functions
 *
 * - **createPing**: Create a ping packet with optional message
 * - **getPingStats**: Get ping statistics (sent/received counts)
 *
 * ## Usage
 *
 * ```kotlin
 * // Create simple ping
 * val pingPacket = PingPacketsFFI.createPing()
 * device.sendPacket(pingPacket.toLegacyPacket())
 *
 * // Create ping with message
 * val messagePacket = PingPacketsFFI.createPing("Hello from Android!")
 * device.sendPacket(messagePacket.toLegacyPacket())
 *
 * // Get statistics
 * val stats = PingPacketsFFI.getPingStats()
 * println("Sent: ${stats.pingsSent}, Received: ${stats.pingsReceived}")
 * ```
 *
 * ## Protocol
 *
 * **Packet Type**: `cconnect.ping`
 *
 * **Body Fields**:
 * - `message` (optional): String - Custom message to display
 *
 * **Example Packet**:
 * ```json
 * {
 *   "id": "1234567890",
 *   "type": "cconnect.ping",
 *   "body": {
 *     "message": "Hello from Android!"
 *   }
 * }
 * ```
 *
 * ## Architecture
 *
 * This wrapper follows the established FFI pattern:
 * 1. Call Rust PluginManager FFI function
 * 2. Receive immutable FfiPacket from Rust
 * 3. Wrap in Kotlin NetworkPacket for type safety
 * 4. Convert to legacy packet for device.sendPacket()
 *
 * @see PingPlugin
 * @see NetworkPacket
 * @see PluginManagerProvider
 */
object PingPacketsFFI {

    /**
     * Create a ping packet
     *
     * Creates a `cconnect.ping` packet using the Rust FFI core. The packet
     * can optionally include a custom message to be displayed on the receiving
     * device.
     *
     * ## Behavior
     * - Without message: Creates basic ping packet (equivalent to "Ping!")
     * - With message: Includes custom message in packet body
     * - Increments ping sent counter in Rust core statistics
     *
     * ## Threading
     * Thread-safe. Can be called from any thread.
     *
     * ## Error Handling
     * Throws exceptions from Rust FFI if:
     * - PluginManager not initialized
     * - Packet creation fails
     *
     * @param message Optional custom message to include in the ping
     * @return Immutable NetworkPacket ready to send via device.sendPacket()
     *
     * @throws uniffi.cosmic_connect_core.ProtocolException if packet creation fails
     */
    fun createPing(message: String? = null): NetworkPacket {
        val pluginManager = PluginManagerProvider.getInstance()
        return pluginManager.createPing(message)
    }

    /**
     * Get ping statistics from Rust core
     *
     * Returns cumulative ping statistics tracked by the Rust core plugin manager.
     * Statistics are maintained across the lifetime of the PluginManager instance.
     *
     * ## Statistics
     * - **pingsSent**: Number of ping packets created via createPing()
     * - **pingsReceived**: Number of ping packets processed by Rust core
     *
     * ## Note
     * These statistics track only FFI-created packets. Legacy ping packets
     * created directly with `NetworkPacket(PACKET_TYPE_PING)` are not counted.
     *
     * ## Threading
     * Thread-safe. Can be called from any thread.
     *
     * @return PingStats with pingsReceived and pingsSent counts
     */
    fun getPingStats(): PingStats {
        val pluginManager = PluginManagerProvider.getInstance()
        return pluginManager.getPingStats()
    }
}
