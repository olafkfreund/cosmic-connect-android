/*
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createFindmyphoneRequest

/**
 * FindMyPhonePacketsFFI - FFI wrapper for Find My Phone plugin
 *
 * This object provides type-safe packet creation for the Find My Phone plugin
 * using the cosmic-connect-core Rust FFI layer.
 *
 * ## Protocol
 *
 * **Packet Type:**
 * - `cconnect.findmyphone.request` - Request to make phone ring
 *
 * **Direction:**
 * - Desktop → Android: Send ring request
 * - Android: Receive request and make phone ring
 *
 * ## Behavior
 *
 * - Packet has empty body (no additional data needed)
 * - Receiving device rings at maximum volume
 * - Sending request again cancels ringing (implementation dependent)
 * - Bypasses silent mode (uses ALARM audio stream)
 *
 * ## Usage
 *
 * **Receiving (Android side - typical):**
 * ```kotlin
 * override fun onPacketReceived(tp: TransferPacket): Boolean {
 *     val np = tp.packet
 *
 *     if (np.isFindMyPhoneRequest) {
 *         // Make phone ring
 *         startRinging()
 *         return true
 *     }
 *     return false
 * }
 * ```
 *
 * **Sending (Desktop side or testing):**
 * ```kotlin
 * val packet = FindMyPhonePacketsFFI.createRingRequest()
 * device.sendPacket(packet)
 * ```
 *
 * @see FindMyPhonePlugin
 */
object FindMyPhonePacketsFFI {

    /**
     * Create a find my phone ring request packet
     *
     * Creates a packet that makes the remote device ring at maximum volume
     * to help locate it. The packet has an empty body as no additional data
     * is needed.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "cconnect.findmyphone.request",
     *   "id": 1234567890,
     *   "body": {}
     * }
     * ```
     *
     * ## Behavior
     * - Makes phone ring at maximum ALARM volume
     * - Bypasses silent mode
     * - Shows notification with "Found It" action
     * - Sending again should cancel ringing
     *
     * @return NetworkPacket ready to send
     *
     * @see isFindMyPhoneRequest
     */
    fun createRingRequest(): NetworkPacket {
        val ffiPacket = createFindmyphoneRequest()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// ==========================================================================
// Extension Properties for Packet Type Inspection
// ==========================================================================

/**
 * Check if packet is a find my phone request
 *
 * Returns true if the packet is a `cconnect.findmyphone.request` packet,
 * which instructs the device to ring at maximum volume.
 *
 * ## Usage
 * ```kotlin
 * if (np.isFindMyPhoneRequest) {
 *     startRinging()
 * }
 * ```
 *
 * @return true if packet is a findmyphone request, false otherwise
 */
val NetworkPacket.isFindMyPhoneRequest: Boolean
    get() = type == "cconnect.findmyphone.request"
