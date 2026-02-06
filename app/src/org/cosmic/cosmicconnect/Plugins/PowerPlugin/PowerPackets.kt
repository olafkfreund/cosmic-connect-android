/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.PowerPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket

/**
 * Extension properties for type-safe inspection of Power packets.
 *
 * No FFI wrapper needed — cosmic-connect-core does not have a Power plugin.
 * Packets are created directly via the NetworkPacket data class constructor.
 *
 * ## Packet Types
 *
 * - **Power Status** (`cconnect.power`): Desktop power state (incoming)
 * - **Power Request** (`cconnect.power.request`): Power command (outgoing)
 *
 * ## Power Status Fields (incoming from desktop)
 *
 * - `hasBattery` (boolean): Whether the desktop has a battery (laptop)
 * - `batteryCharge` (int, optional): Battery percentage 0-100
 * - `isCharging` (boolean, optional): Whether the desktop is plugged in
 * - `isLidClosed` (boolean, optional): Whether the laptop lid is closed
 *
 * ## Power Request Fields (outgoing to desktop)
 *
 * - `action` (string): One of "shutdown", "reboot", "suspend", "hibernate"
 */

/**
 * Check if this packet is a power status packet from the desktop.
 */
val NetworkPacket.isPowerStatusPacket: Boolean
    get() = type == "cconnect.power"

/**
 * Check if this packet is a power command request.
 */
val NetworkPacket.isPowerRequestPacket: Boolean
    get() = type == "cconnect.power.request" && body.containsKey("action")

/**
 * Whether the desktop reports having a battery.
 */
val NetworkPacket.powerHasBattery: Boolean?
    get() = if (isPowerStatusPacket) body["hasBattery"] as? Boolean else null

/**
 * Desktop battery charge percentage (0-100).
 */
val NetworkPacket.powerBatteryCharge: Int?
    get() = if (isPowerStatusPacket) (body["batteryCharge"] as? Number)?.toInt() else null

/**
 * Whether the desktop is plugged in / charging.
 */
val NetworkPacket.powerIsCharging: Boolean?
    get() = if (isPowerStatusPacket) body["isCharging"] as? Boolean else null

/**
 * Whether the laptop lid is closed.
 */
val NetworkPacket.powerIsLidClosed: Boolean?
    get() = if (isPowerStatusPacket) body["isLidClosed"] as? Boolean else null

/**
 * The power action requested (shutdown, reboot, suspend, hibernate).
 */
val NetworkPacket.powerAction: String?
    get() = if (isPowerRequestPacket) body["action"] as? String else null
