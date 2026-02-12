package org.cosmicext.connect.Plugins.ConnectivityReportPlugin

import org.cosmicext.connect.Core.NetworkPacket
import uniffi.cosmic_ext_connect_core.createConnectivityReport

/**
 * FFI wrapper for creating ConnectivityReport plugin packets
 *
 * The ConnectivityReport plugin reports network connectivity state including
 * network type and signal strength for cellular connections.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Report cellular network connectivity state
 * - Signal strength reporting per subscription
 * - Network type identification (4G, HSPA, etc.)
 *
 * ## Usage
 *
 * ```kotlin
 * // Build signal strengths JSON
 * val signalStrengths = JSONObject()
 * signalStrengths.put("6", JSONObject().apply {
 *     put("networkType", "4G")
 *     put("signalStrength", 3)
 * })
 *
 * // Create connectivity report packet
 * val packet = ConnectivityPacketsFFI.createConnectivityReport(
 *     signalStrengths.toString()
 * )
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * @see ConnectivityReportPlugin
 * @see ConnectivityListener
 */
object ConnectivityPacketsFFI {

    /**
     * Create a connectivity report packet
     *
     * Creates a packet containing network connectivity state information,
     * including network type and signal strength for each cellular subscription.
     *
     * The signal strengths JSON should be formatted as:
     * ```json
     * {
     *   "subscriptionId": {
     *     "networkType": "4G",
     *     "signalStrength": 3
     *   },
     *   ...
     * }
     * ```
     *
     * @param signalStrengthsJson JSON string containing subscription states
     * @return NetworkPacket ready to send
     *
     * @throws CosmicExtConnectException if packet creation fails
     * @throws CosmicExtConnectException if JSON parsing fails
     */
    fun createConnectivityReport(signalStrengthsJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_ext_connect_core.createConnectivityReport(signalStrengthsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
