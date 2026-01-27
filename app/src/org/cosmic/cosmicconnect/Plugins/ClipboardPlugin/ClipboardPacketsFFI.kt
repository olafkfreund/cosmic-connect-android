/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ClipboardPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*

/**
 * FFI wrapper for clipboard packet creation and inspection.
 *
 * Provides type-safe packet creation using the cosmic-connect-core FFI layer
 * and extension properties for inspecting clipboard packets.
 *
 * ## Packet Types
 *
 * - **Standard Update** (`cconnect.clipboard`): Sent when clipboard changes
 * - **Connection Sync** (`cconnect.clipboard.connect`): Sent on device connection with timestamp
 *
 * ## Sync Loop Prevention
 *
 * Connection sync packets include timestamps to prevent infinite sync loops:
 * - Each clipboard update has a timestamp (UNIX epoch milliseconds)
 * - Incoming updates with timestamp ≤ local timestamp are ignored
 * - Incoming updates with timestamp > local timestamp are accepted
 * - Connect packets with timestamp 0 are ignored (no content)
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Create standard clipboard update
 * val packet = ClipboardPacketsFFI.createClipboardUpdate("Hello World")
 * device.sendPacket(packet)
 *
 * // Create connection sync packet
 * val timestamp = System.currentTimeMillis()
 * val connectPacket = ClipboardPacketsFFI.createClipboardConnect("Hello", timestamp)
 * device.sendPacket(connectPacket)
 *
 * // Inspect incoming packets
 * if (packet.isClipboardUpdate) {
 *     val content = packet.clipboardContent
 *     // Update local clipboard
 * }
 * ```
 *
 * @see org.cosmic.cosmicconnect.Plugins.ClipboardPlugin.ClipboardPlugin
 */
object ClipboardPacketsFFI {
    /**
     * Create a standard clipboard update packet.
     *
     * Creates a `cconnect.clipboard` packet for syncing clipboard changes
     * between devices. Does not include a timestamp.
     *
     * ## Validation
     * - Content must not be blank (empty or whitespace-only)
     *
     * ## Example
     * ```kotlin
     * val packet = ClipboardPacketsFFI.createClipboardUpdate("Hello World")
     * device.sendPacket(packet)
     * ```
     *
     * @param content Text content to sync to clipboard
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if content is blank
     */
    fun createClipboardUpdate(content: String): NetworkPacket {
        require(content.isNotBlank()) { "Clipboard content cannot be blank" }

        val ffiPacket = createClipboardPacket(content)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a clipboard connect packet with timestamp.
     *
     * Creates a `cconnect.clipboard.connect` packet for syncing clipboard
     * state when devices connect. Includes timestamp for sync loop prevention.
     *
     * ## Validation
     * - Content must not be blank (empty or whitespace-only)
     * - Timestamp must be non-negative
     *
     * ## Timestamp Guidelines
     * - Use `System.currentTimeMillis()` for current timestamp
     * - Timestamp 0 indicates no content (will be ignored by receiver)
     * - Timestamps are compared to prevent sync loops
     *
     * ## Example
     * ```kotlin
     * val content = "Hello World"
     * val timestamp = System.currentTimeMillis()
     * val packet = ClipboardPacketsFFI.createClipboardConnect(content, timestamp)
     * device.sendPacket(packet)
     * ```
     *
     * @param content Text content to sync to clipboard
     * @param timestamp UNIX epoch timestamp in milliseconds when content was last modified
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if content is blank or timestamp is negative
     */
    fun createClipboardConnect(content: String, timestamp: Long): NetworkPacket {
        require(content.isNotBlank()) { "Clipboard content cannot be blank" }
        require(timestamp >= 0) { "Timestamp cannot be negative" }

        val ffiPacket = createClipboardConnectPacket(content, timestamp)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// =============================================================================
// Extension Properties for Type-Safe Packet Inspection
// =============================================================================

/**
 * Check if packet is a standard clipboard update.
 *
 * Returns true if the packet is a `cconnect.clipboard` packet with content field.
 *
 * ## Example
 * ```kotlin
 * if (packet.isClipboardUpdate) {
 *     val content = packet.clipboardContent
 *     // Update local clipboard
 * }
 * ```
 *
 * @return true if packet is a standard clipboard update, false otherwise
 */
val NetworkPacket.isClipboardUpdate: Boolean
    get() = type == "cconnect.clipboard" && body.containsKey("content")

/**
 * Check if packet is a clipboard connection sync packet.
 *
 * Returns true if the packet is a `cconnect.clipboard.connect` packet
 * with both content and timestamp fields.
 *
 * ## Example
 * ```kotlin
 * if (packet.isClipboardConnect) {
 *     val content = packet.clipboardContent
 *     val timestamp = packet.clipboardTimestamp
 *     // Update clipboard if timestamp is newer
 * }
 * ```
 *
 * @return true if packet is a clipboard connect packet, false otherwise
 */
val NetworkPacket.isClipboardConnect: Boolean
    get() = type == "cconnect.clipboard.connect" &&
            body.containsKey("content") &&
            body.containsKey("timestamp")

/**
 * Extract clipboard content from packet.
 *
 * Returns the text content from either a standard update or connect packet.
 * Returns null if the packet is not a clipboard packet or content is missing.
 *
 * ## Example
 * ```kotlin
 * val content = packet.clipboardContent
 * if (content != null) {
 *     clipboardManager.setText(content)
 * }
 * ```
 *
 * @return Clipboard text content, or null if not a clipboard packet or content missing
 */
val NetworkPacket.clipboardContent: String?
    get() = if (isClipboardUpdate || isClipboardConnect) {
        body["content"] as? String
    } else null

/**
 * Extract timestamp from clipboard connect packet.
 *
 * Returns the UNIX epoch timestamp (milliseconds) from a connect packet.
 * Returns null if the packet is not a connect packet or timestamp is missing.
 *
 * ## Timestamp Usage
 * - Compare with local timestamp to prevent sync loops
 * - Timestamp 0 indicates no content (should be ignored)
 * - Only apply updates with timestamp > local timestamp
 *
 * ## Example
 * ```kotlin
 * if (packet.isClipboardConnect) {
 *     val timestamp = packet.clipboardTimestamp
 *     if (timestamp != null && timestamp > localTimestamp) {
 *         // Apply clipboard update
 *     }
 * }
 * ```
 *
 * @return Timestamp in milliseconds, or null if not a connect packet or timestamp missing
 */
val NetworkPacket.clipboardTimestamp: Long?
    get() = if (isClipboardConnect) {
        (body["timestamp"] as? Number)?.toLong()
    } else null

// =============================================================================
