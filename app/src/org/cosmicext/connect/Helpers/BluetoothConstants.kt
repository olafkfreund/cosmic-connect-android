/*
 * SPDX-FileCopyrightText: 2025 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Helpers

import java.util.UUID

/**
 * Bluetooth constants for COSMIC Connect / COSMIC Connect protocol
 *
 * These constants define the Bluetooth Low Energy (BLE) service and characteristics
 * used for device communication when WiFi is unavailable. All implementations must
 * use these exact UUIDs for compatibility.
 *
 * ## Transport Architecture
 *
 * COSMIC Connect supports multiple transports:
 * - **TCP/IP**: Primary transport (WiFi, Ethernet)
 * - **Bluetooth**: Alternative transport for when WiFi unavailable
 *
 * ## Usage
 *
 * When advertising or discovering COSMIC Connect devices over Bluetooth:
 * 1. Advertise/scan for [SERVICE_UUID]
 * 2. Use [CHARACTERISTIC_READ_UUID] for receiving packets
 * 3. Use [CHARACTERISTIC_WRITE_UUID] for sending packets
 *
 * ## Packet Format
 *
 * Packets sent over Bluetooth follow the same format as TCP:
 * - 4 bytes: packet length (big-endian)
 * - N bytes: JSON packet data
 *
 * ## MTU Limitations
 *
 * Bluetooth RFCOMM typically has a smaller MTU (~512 bytes) compared to TCP (1 MB).
 * Large packets should be:
 * - Fragmented appropriately, or
 * - Sent via the payload protocol (for files), or
 * - Fallback to TCP if available
 *
 * @see <a href="https://github.com/olafkfreund/cosmic-connect-desktop-app">COSMIC Connect Desktop</a>
 * @see <a href="https://github.com/olafkfreund/cosmic-connect-core">COSMIC Connect Core</a>
 */
object BluetoothConstants {
    /**
     * COSMIC Connect Bluetooth service UUID
     *
     * This UUID identifies the COSMIC Connect / COSMIC Connect service when advertising
     * or discovering devices over Bluetooth. All implementations must use this UUID.
     *
     * UUID: `185f3df4-3268-4e3f-9fca-d4d5059915bd`
     */
    val SERVICE_UUID: UUID = UUID.fromString("185f3df4-3268-4e3f-9fca-d4d5059915bd")

    /**
     * Bluetooth RFCOMM characteristic UUID for reading packets
     *
     * Subscribe to notifications on this characteristic to receive packets
     * from the remote device.
     *
     * UUID: `8667556c-9a37-4c91-84ed-54ee27d90049`
     */
    val CHARACTERISTIC_READ_UUID: UUID = UUID.fromString("8667556c-9a37-4c91-84ed-54ee27d90049")

    /**
     * Bluetooth RFCOMM characteristic UUID for writing packets
     *
     * Write to this characteristic to send packets to the remote device.
     *
     * UUID: `d0e8434d-cd29-0996-af41-6c90f4e0eb2a`
     */
    val CHARACTERISTIC_WRITE_UUID: UUID = UUID.fromString("d0e8434d-cd29-0996-af41-6c90f4e0eb2a")

    /**
     * Maximum packet size for Bluetooth transport (512 bytes)
     *
     * Bluetooth RFCOMM typically has a smaller MTU than TCP.
     * This conservative value ensures compatibility across devices.
     *
     * For packets larger than this:
     * - Use the payload protocol for file transfers
     * - Fragment the packet appropriately
     * - Fall back to TCP/WiFi if available
     */
    const val MAX_PACKET_SIZE: Int = 512

    /**
     * Bluetooth operation timeout (15 seconds)
     *
     * Bluetooth operations typically have higher latency than TCP/WiFi.
     * This timeout allows for:
     * - Device discovery and scanning
     * - Connection establishment
     * - Characteristic discovery
     * - Packet transmission
     */
    const val OPERATION_TIMEOUT_MS: Long = 15_000

    /**
     * Get human-readable service UUID string
     *
     * @return Service UUID as lowercase string without hyphens
     */
    fun getServiceUuidString(): String {
        return SERVICE_UUID.toString().lowercase().replace("-", "_")
    }

    /**
     * Transport capabilities for Bluetooth
     *
     * Describes the characteristics of the Bluetooth transport for use in
     * transport selection algorithms.
     */
    object Capabilities {
        /** Maximum transmission unit */
        const val MAX_MTU: Int = MAX_PACKET_SIZE

        /** Bluetooth provides reliable delivery */
        const val RELIABLE: Boolean = true

        /** Bluetooth is connection-oriented */
        const val CONNECTION_ORIENTED: Boolean = true

        /** Bluetooth typically has medium latency (10-50ms) */
        const val LATENCY_CATEGORY: String = "MEDIUM"
    }
}
