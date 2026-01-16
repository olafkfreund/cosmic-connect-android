/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.BatteryPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*

/**
 * FFI wrapper for battery packet creation and inspection.
 *
 * Provides type-safe packet creation using the cosmic-connect-core FFI layer
 * and extension properties for inspecting battery packets.
 *
 * ## Packet Types
 *
 * - **Battery Status** (`kdeconnect.battery`): Battery state information (bi-directional)
 * - **Battery Request** (`kdeconnect.battery.request`): Request battery status (incoming)
 *
 * ## Battery State Fields
 *
 * - `isCharging` (boolean): Whether the device is currently charging
 * - `currentCharge` (int 0-100): Battery percentage
 * - `thresholdEvent` (int): Threshold event indicator
 *   - `0`: No event
 *   - `1`: Battery low (typically < 15%)
 *
 * ## Usage Examples
 *
 * ### Sending Battery Status
 * ```kotlin
 * val packet = BatteryPacketsFFI.createBatteryPacket(
 *     isCharging = true,
 *     currentCharge = 85,
 *     thresholdEvent = 0
 * )
 * device.sendPacket(packet)
 * ```
 *
 * ### Requesting Battery Status
 * ```kotlin
 * val request = BatteryPacketsFFI.createBatteryRequest()
 * device.sendPacket(request)
 * ```
 *
 * ### Inspecting Packets
 * ```kotlin
 * if (packet.isBatteryPacket) {
 *     val charge = packet.batteryCurrentCharge
 *     val isCharging = packet.batteryIsCharging
 *     if (packet.isBatteryLow) {
 *         showLowBatteryWarning()
 *     }
 * }
 * ```
 *
 * @see org.cosmic.cosmicconnect.Plugins.BatteryPlugin.BatteryPlugin
 */
object BatteryPacketsFFI {
    /**
     * Create a battery status packet.
     *
     * Creates a `kdeconnect.battery` packet for sharing battery state between
     * devices. This packet is sent bi-directionally whenever battery state changes.
     *
     * ## Validation
     * - Current charge must be 0-100 (will be clamped)
     * - Threshold event must be 0 (none) or 1 (low battery)
     *
     * ## Threshold Events
     * - `0`: No threshold event
     * - `1`: Battery low (< 15%)
     *
     * ## Example
     * ```kotlin
     * // Device charging at 85%
     * val packet = BatteryPacketsFFI.createBatteryPacket(
     *     isCharging = true,
     *     currentCharge = 85,
     *     thresholdEvent = 0
     * )
     * device.sendPacket(packet)
     *
     * // Low battery warning (12%, not charging)
     * val lowBattery = BatteryPacketsFFI.createBatteryPacket(
     *     isCharging = false,
     *     currentCharge = 12,
     *     thresholdEvent = 1
     * )
     * device.sendPacket(lowBattery)
     * ```
     *
     * @param isCharging Whether the device is currently charging
     * @param currentCharge Battery percentage (0-100, will be clamped if out of range)
     * @param thresholdEvent Threshold event indicator (0=none, 1=low battery)
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if thresholdEvent is not 0 or 1
     */
    fun createBatteryPacket(
        isCharging: Boolean,
        currentCharge: Int,
        thresholdEvent: Int
    ): NetworkPacket {
        require(thresholdEvent in 0..1) {
            "Threshold event must be 0 (none) or 1 (low battery), got: $thresholdEvent"
        }

        val ffiPacket = uniffi.cosmic_connect_core.createBatteryPacket(isCharging, currentCharge, thresholdEvent)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a battery status request packet.
     *
     * Creates a `kdeconnect.battery.request` packet requesting the remote
     * device's current battery status. When received, the remote device should
     * respond with a battery status packet.
     *
     * ## Example
     * ```kotlin
     * // Request battery status from Android device
     * val packet = BatteryPacketsFFI.createBatteryRequest()
     * device.sendPacket(packet)
     *
     * // Android will respond with current battery state
     * ```
     *
     * @return Immutable NetworkPacket ready to be sent
     */
    fun createBatteryRequest(): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createBatteryRequest()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// =============================================================================
// Extension Properties for Type-Safe Packet Inspection
// =============================================================================

/**
 * Check if packet is a battery status packet.
 *
 * Returns true if the packet is a `kdeconnect.battery` packet with battery
 * state fields.
 *
 * ## Example
 * ```kotlin
 * if (packet.isBatteryPacket) {
 *     val charge = packet.batteryCurrentCharge
 *     val isCharging = packet.batteryIsCharging
 *     updateBatteryDisplay(charge, isCharging)
 * }
 * ```
 *
 * @return true if packet is a battery status packet, false otherwise
 */
val NetworkPacket.isBatteryPacket: Boolean
    get() = type == "kdeconnect.battery" &&
            body.containsKey("isCharging") &&
            body.containsKey("currentCharge")

/**
 * Check if packet is a battery status request.
 *
 * Returns true if the packet is a `kdeconnect.battery.request` packet.
 *
 * ## Example
 * ```kotlin
 * if (packet.isBatteryRequest) {
 *     // Send current battery state as response
 *     val response = BatteryPacketsFFI.createBatteryPacket(
 *         isCharging = batteryManager.isCharging,
 *         currentCharge = batteryManager.level,
 *         thresholdEvent = if (batteryManager.level < 15) 1 else 0
 *     )
 *     device.sendPacket(response)
 * }
 * ```
 *
 * @return true if packet is a battery request, false otherwise
 */
val NetworkPacket.isBatteryRequest: Boolean
    get() = type == "kdeconnect.battery.request"

/**
 * Extract charging status from battery packet.
 *
 * Returns whether the device is currently charging from a battery status packet.
 * Returns null if not a battery packet or field is missing.
 *
 * ## Example
 * ```kotlin
 * val isCharging = packet.batteryIsCharging
 * if (isCharging == true) {
 *     showChargingIcon()
 * } else if (isCharging == false) {
 *     showBatteryIcon()
 * }
 * ```
 *
 * @return Charging status (true/false), or null if not available
 */
val NetworkPacket.batteryIsCharging: Boolean?
    get() = if (isBatteryPacket) {
        body["isCharging"] as? Boolean
    } else null

/**
 * Extract battery charge level from battery packet.
 *
 * Returns the battery percentage (0-100) from a battery status packet.
 * Returns null if not a battery packet or field is missing.
 *
 * ## Example
 * ```kotlin
 * val charge = packet.batteryCurrentCharge
 * if (charge != null) {
 *     batteryLevelView.text = "$charge%"
 *     batteryIcon.level = charge
 * }
 * ```
 *
 * @return Battery percentage (0-100), or null if not available
 */
val NetworkPacket.batteryCurrentCharge: Int?
    get() = if (isBatteryPacket) {
        (body["currentCharge"] as? Number)?.toInt()
    } else null

/**
 * Extract threshold event from battery packet.
 *
 * Returns the threshold event indicator from a battery status packet.
 * - `0`: No event
 * - `1`: Battery low (< 15%)
 *
 * Returns null if not a battery packet or field is missing.
 *
 * ## Example
 * ```kotlin
 * val event = packet.batteryThresholdEvent
 * when (event) {
 *     0 -> { /* No event */ }
 *     1 -> showLowBatteryNotification()
 *     else -> { /* Unknown event */ }
 * }
 * ```
 *
 * @return Threshold event (0 or 1), or null if not available
 */
val NetworkPacket.batteryThresholdEvent: Int?
    get() = if (isBatteryPacket) {
        (body["thresholdEvent"] as? Number)?.toInt()
    } else null

/**
 * Check if battery is low based on charge level.
 *
 * Returns true if the battery charge is below 15% and the device is not charging.
 * This is a convenience property that combines charge level and charging status.
 *
 * ## Example
 * ```kotlin
 * if (packet.isBatteryLow) {
 *     notificationManager.notify(
 *         "Remote device battery is low: ${packet.batteryCurrentCharge}%"
 *     )
 * }
 * ```
 *
 * @return true if battery is low (< 15% and not charging), false otherwise
 */
val NetworkPacket.isBatteryLow: Boolean
    get() {
        if (!isBatteryPacket) return false

        val charge = batteryCurrentCharge ?: return false
        val isCharging = batteryIsCharging ?: false

        return charge < 15 && !isCharging
    }

/**
 * Check if battery is critical based on charge level.
 *
 * Returns true if the battery charge is below 5% and the device is not charging.
 * This indicates a critical battery state requiring immediate attention.
 *
 * ## Example
 * ```kotlin
 * if (packet.isBatteryCritical) {
 *     notificationManager.notifyUrgent(
 *         "Remote device battery critical: ${packet.batteryCurrentCharge}%"
 *     )
 *     vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
 * }
 * ```
 *
 * @return true if battery is critical (< 5% and not charging), false otherwise
 */
val NetworkPacket.isBatteryCritical: Boolean
    get() {
        if (!isBatteryPacket) return false

        val charge = batteryCurrentCharge ?: return false
        val isCharging = batteryIsCharging ?: false

        return charge < 5 && !isCharging
    }

// =============================================================================
// Java-Compatible Extension Functions
// =============================================================================

/**
 * Java-compatible function to check if packet is a battery status packet.
 *
 * Equivalent to the `isBatteryPacket` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if packet is a battery status packet, false otherwise
 */
fun getIsBatteryPacket(packet: NetworkPacket): Boolean {
    return packet.isBatteryPacket
}

/**
 * Java-compatible function to check if packet is a battery request.
 *
 * Equivalent to the `isBatteryRequest` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if packet is a battery request, false otherwise
 */
fun getIsBatteryRequest(packet: NetworkPacket): Boolean {
    return packet.isBatteryRequest
}

/**
 * Java-compatible function to extract charging status.
 *
 * Equivalent to the `batteryIsCharging` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Charging status (true/false), or null if not available
 */
fun getBatteryIsCharging(packet: NetworkPacket): Boolean? {
    return packet.batteryIsCharging
}

/**
 * Java-compatible function to extract battery charge level.
 *
 * Equivalent to the `batteryCurrentCharge` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Battery percentage (0-100), or null if not available
 */
fun getBatteryCurrentCharge(packet: NetworkPacket): Int? {
    return packet.batteryCurrentCharge
}

/**
 * Java-compatible function to extract threshold event.
 *
 * Equivalent to the `batteryThresholdEvent` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Threshold event (0 or 1), or null if not available
 */
fun getBatteryThresholdEvent(packet: NetworkPacket): Int? {
    return packet.batteryThresholdEvent
}

/**
 * Java-compatible function to check if battery is low.
 *
 * Equivalent to the `isBatteryLow` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if battery is low (< 15% and not charging), false otherwise
 */
fun getIsBatteryLow(packet: NetworkPacket): Boolean {
    return packet.isBatteryLow
}

/**
 * Java-compatible function to check if battery is critical.
 *
 * Equivalent to the `isBatteryCritical` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if battery is critical (< 5% and not charging), false otherwise
 */
fun getIsBatteryCritical(packet: NetworkPacket): Boolean {
    return packet.isBatteryCritical
}
