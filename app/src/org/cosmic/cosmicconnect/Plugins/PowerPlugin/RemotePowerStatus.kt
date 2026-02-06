/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.PowerPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket

/**
 * Parsed power status from the remote desktop device.
 */
data class RemotePowerStatus(
    val hasBattery: Boolean,
    val batteryCharge: Int?,
    val isCharging: Boolean?,
    val isLidClosed: Boolean?,
) {
    companion object {
        fun fromPacket(np: NetworkPacket): RemotePowerStatus {
            return RemotePowerStatus(
                hasBattery = np.powerHasBattery ?: false,
                batteryCharge = np.powerBatteryCharge,
                isCharging = np.powerIsCharging,
                isLidClosed = np.powerIsLidClosed,
            )
        }
    }
}
