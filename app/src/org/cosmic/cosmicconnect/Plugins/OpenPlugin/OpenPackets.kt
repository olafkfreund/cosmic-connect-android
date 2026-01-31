/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.OpenPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket

/**
 * OpenPackets - Type-safe packet creation for OpenOnDesktopPlugin
 *
 * This file provides FFI wrappers for creating COSMIC Connect protocol packets
 * that enable opening URLs, files, and text content on COSMIC Desktop.
 *
 * ## Packet Types
 *
 * 1. **cconnect.open.request** - Request to open content on desktop
 *    ```json
 *    {
 *      "url": "https://example.com",          // URL to open
 *      "mime_type": "text/html",              // Optional: MIME type for files
 *      "text": "text content",                // Optional: plain text to open
 *      "file_uri": "content://...",           // Optional: Android URI for file
 *      "open_in": "browser|editor|default"    // Optional: application hint
 *    }
 *    ```
 *
 * 2. **cconnect.open.response** - Response from desktop about open status
 *    ```json
 *    {
 *      "success": true,
 *      "error": "optional error message"
 *    }
 *    ```
 *
 * 3. **cconnect.open.capability** - Announce open capabilities
 *    ```json
 *    {
 *      "supported_schemes": ["http", "https", "mailto", "tel"],
 *      "can_open_files": true,
 *      "can_open_text": true
 *    }
 *    ```
 *
 * ## Security
 *
 * URL validation is critical for security:
 * - Only allowed schemes: http, https, mailto, tel, geo, sms
 * - Never send file:// or javascript: URLs
 * - Validate URLs before creating packets
 *
 * ## FFI Integration
 *
 * These wrappers use cosmic-connect-core's Rust packet builders for type safety.
 * Each function creates an immutable NetworkPacket that can be converted to
 * legacy format for sending via Device.sendPacket().
 */

/**
 * FFI wrapper object for creating open request packets
 */
object OpenPacketsFFI {

    /**
     * Create a packet to open a URL on COSMIC Desktop
     *
     * @param url The URL to open (must use allowed scheme)
     * @param openIn Optional hint: "browser", "editor", or "default"
     * @return Immutable NetworkPacket ready to send
     */
    fun createUrlOpenRequest(url: String, openIn: String = "default"): NetworkPacket {
        return NetworkPacket.create(
            "cconnect.open.request",
            mapOf(
                "url" to url,
                "open_in" to openIn
            )
        )
    }

    /**
     * Create a packet to open a file on COSMIC Desktop
     *
     * @param fileUri Android content URI for the file
     * @param mimeType MIME type of the file
     * @param openIn Optional hint: "editor", "viewer", or "default"
     * @return Immutable NetworkPacket ready to send
     */
    fun createFileOpenRequest(
        fileUri: String,
        mimeType: String,
        openIn: String = "default"
    ): NetworkPacket {
        return NetworkPacket.create(
            "cconnect.open.request",
            mapOf(
                "file_uri" to fileUri,
                "mime_type" to mimeType,
                "open_in" to openIn
            )
        )
    }

    /**
     * Create a packet to open text content on COSMIC Desktop
     *
     * @param text Plain text content to open
     * @param openIn Optional hint: "editor" or "default"
     * @return Immutable NetworkPacket ready to send
     */
    fun createTextOpenRequest(text: String, openIn: String = "editor"): NetworkPacket {
        return NetworkPacket.create(
            "cconnect.open.request",
            mapOf(
                "text" to text,
                "open_in" to openIn
            )
        )
    }

    /**
     * Create a response packet indicating success or failure
     *
     * @param success Whether the open operation succeeded
     * @param error Optional error message if success is false
     * @return Immutable NetworkPacket ready to send
     */
    fun createOpenResponse(success: Boolean, error: String? = null): NetworkPacket {
        val body = mutableMapOf<String, Any>("success" to success)
        if (error != null) {
            body["error"] = error
        }

        return NetworkPacket.create("cconnect.open.response", body)
    }

    /**
     * Create a capability announcement packet
     *
     * @param supportedSchemes List of URL schemes this device can handle
     * @param canOpenFiles Whether this device can open files
     * @param canOpenText Whether this device can open text content
     * @return Immutable NetworkPacket ready to send
     */
    fun createCapabilityAnnouncement(
        supportedSchemes: List<String>,
        canOpenFiles: Boolean = true,
        canOpenText: Boolean = true
    ): NetworkPacket {
        return NetworkPacket.create(
            "cconnect.open.capability",
            mapOf(
                "supported_schemes" to supportedSchemes,
                "can_open_files" to canOpenFiles,
                "can_open_text" to canOpenText
            )
        )
    }
}

// ============================================================================
// Extension Properties for Type-Safe Packet Inspection
// ============================================================================

/**
 * Check if this packet is an open request
 */
val NetworkPacket.isOpenRequest: Boolean
    get() = type == "cconnect.open.request"

/**
 * Check if this packet is an open response
 */
val NetworkPacket.isOpenResponse: Boolean
    get() = type == "cconnect.open.response"

/**
 * Check if this packet is a capability announcement
 */
val NetworkPacket.isOpenCapability: Boolean
    get() = type == "cconnect.open.capability"

/**
 * Extract URL from open request packet
 */
val NetworkPacket.openUrl: String?
    get() = if (isOpenRequest) body["url"] as? String else null

/**
 * Extract file URI from open request packet
 */
val NetworkPacket.openFileUri: String?
    get() = if (isOpenRequest) body["file_uri"] as? String else null

/**
 * Extract MIME type from open request packet
 */
val NetworkPacket.openMimeType: String?
    get() = if (isOpenRequest) body["mime_type"] as? String else null

/**
 * Extract text content from open request packet
 */
val NetworkPacket.openText: String?
    get() = if (isOpenRequest) body["text"] as? String else null

/**
 * Extract open hint from open request packet
 */
val NetworkPacket.openIn: String?
    get() = if (isOpenRequest) body["open_in"] as? String else null

/**
 * Extract success status from open response packet
 */
val NetworkPacket.openSuccess: Boolean?
    get() = if (isOpenResponse) body["success"] as? Boolean else null

/**
 * Extract error message from open response packet
 */
val NetworkPacket.openError: String?
    get() = if (isOpenResponse) body["error"] as? String else null

/**
 * Extract supported schemes from capability packet
 */
val NetworkPacket.openSupportedSchemes: List<String>?
    get() = if (isOpenCapability) {
        @Suppress("UNCHECKED_CAST")
        body["supported_schemes"] as? List<String>
    } else null

/**
 * Extract file opening capability from capability packet
 */
val NetworkPacket.openCanOpenFiles: Boolean?
    get() = if (isOpenCapability) body["can_open_files"] as? Boolean else null

/**
 * Extract text opening capability from capability packet
 */
val NetworkPacket.openCanOpenText: Boolean?
    get() = if (isOpenCapability) body["can_open_text"] as? Boolean else null
