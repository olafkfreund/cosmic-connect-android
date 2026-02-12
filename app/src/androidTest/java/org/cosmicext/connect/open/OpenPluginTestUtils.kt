/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.open

import org.cosmicext.connect.NetworkPacket

/**
 * OpenPluginTestUtils - Test utilities for Open plugin E2E tests
 *
 * Provides helper functions for creating test packets, URLs, and validating
 * App Continuity functionality.
 */
object OpenPluginTestUtils {

    /**
     * Create a mock open request packet for testing
     *
     * @param url URL to open
     * @param openIn Optional hint for opening application
     * @return NetworkPacket for cconnect.open.request
     */
    fun createMockOpenRequest(
        url: String,
        openIn: String = "default"
    ): NetworkPacket {
        val packet = NetworkPacket("cconnect.open.request")
        packet["url"] = url
        packet["open_in"] = openIn
        return packet
    }

    /**
     * Create a mock file open request packet for testing
     *
     * @param fileUri Android content URI
     * @param mimeType MIME type of file
     * @param openIn Optional hint for opening application
     * @return NetworkPacket for cconnect.open.request
     */
    fun createMockFileOpenRequest(
        fileUri: String,
        mimeType: String,
        openIn: String = "default"
    ): NetworkPacket {
        val packet = NetworkPacket("cconnect.open.request")
        packet["file_uri"] = fileUri
        packet["mime_type"] = mimeType
        packet["open_in"] = openIn
        return packet
    }

    /**
     * Create a mock text open request packet for testing
     *
     * @param text Text content to open
     * @param openIn Optional hint for opening application
     * @return NetworkPacket for cconnect.open.request
     */
    fun createMockTextOpenRequest(
        text: String,
        openIn: String = "editor"
    ): NetworkPacket {
        val packet = NetworkPacket("cconnect.open.request")
        packet["text"] = text
        packet["open_in"] = openIn
        return packet
    }

    /**
     * Create a mock open response packet
     *
     * @param success Whether the open operation succeeded
     * @param error Optional error message
     * @return NetworkPacket for cconnect.open.response
     */
    fun createMockOpenResponse(
        success: Boolean,
        error: String? = null
    ): NetworkPacket {
        val packet = NetworkPacket("cconnect.open.response")
        packet["success"] = success
        error?.let { packet["error"] = it }
        return packet
    }

    /**
     * Create a test URL with specified scheme and host
     *
     * @param scheme URL scheme (http, https, mailto, tel, etc.)
     * @param host Hostname or path
     * @param path Optional URL path
     * @return Complete URL string
     */
    fun createTestUrl(
        scheme: String,
        host: String,
        path: String = ""
    ): String {
        return when (scheme) {
            "http", "https" -> {
                val fullPath = if (path.isNotEmpty()) "/$path" else ""
                "$scheme://$host$fullPath"
            }
            "mailto" -> {
                "$scheme:$host"
            }
            "tel" -> {
                "$scheme:$host"
            }
            "geo" -> {
                "$scheme:$host"
            }
            "sms" -> {
                "$scheme:$host"
            }
            "file" -> {
                "$scheme://$host$path"
            }
            "javascript" -> {
                "$scheme:$host"
            }
            "data" -> {
                "$scheme:$host"
            }
            else -> {
                "$scheme://$host$path"
            }
        }
    }

    /**
     * Create a URL with embedded credentials (for security testing)
     *
     * @param scheme URL scheme
     * @param username Username
     * @param password Password
     * @param host Hostname
     * @return URL with embedded credentials
     */
    fun createUrlWithCredentials(
        scheme: String,
        username: String,
        password: String,
        host: String
    ): String {
        return "$scheme://$username:$password@$host"
    }

    /**
     * Validate that a NetworkPacket is a valid open request
     *
     * @param packet NetworkPacket to validate
     * @return true if packet is valid open request
     */
    fun isValidOpenRequest(packet: NetworkPacket): Boolean {
        return packet.type == "cconnect.open.request" &&
                (packet.has("url") || packet.has("file_uri") || packet.has("text"))
    }

    /**
     * Validate that a NetworkPacket is a valid open response
     *
     * @param packet NetworkPacket to validate
     * @return true if packet is valid open response
     */
    fun isValidOpenResponse(packet: NetworkPacket): Boolean {
        return packet.type == "cconnect.open.response" &&
                packet.has("success")
    }

    /**
     * Extract URL from open request packet
     *
     * @param packet NetworkPacket to extract from
     * @return URL string or null
     */
    fun extractUrl(packet: NetworkPacket): String? {
        return if (packet.type == "cconnect.open.request") {
            packet.getString("url")
        } else {
            null
        }
    }

    /**
     * Extract success status from open response packet
     *
     * @param packet NetworkPacket to extract from
     * @return Success boolean or null
     */
    fun extractSuccess(packet: NetworkPacket): Boolean? {
        return if (packet.type == "cconnect.open.response") {
            packet.getBoolean("success")
        } else {
            null
        }
    }

    /**
     * Extract error message from open response packet
     *
     * @param packet NetworkPacket to extract from
     * @return Error string or null
     */
    fun extractError(packet: NetworkPacket): String? {
        return if (packet.type == "cconnect.open.response") {
            packet.getString("error")
        } else {
            null
        }
    }

    /**
     * Common test URLs for security validation
     */
    object TestUrls {
        // Valid URLs
        const val VALID_HTTPS = "https://example.com"
        const val VALID_HTTP = "https://example.org/path/to/page"
        const val VALID_MAILTO = "mailto:test@example.com"
        const val VALID_TEL = "tel:+1234567890"
        const val VALID_GEO = "geo:37.7749,-122.4194"
        const val VALID_SMS = "sms:+1234567890"

        // Invalid URLs (security risks)
        const val INVALID_FILE = "file:///etc/passwd"
        const val INVALID_JAVASCRIPT = "javascript:alert(1)"
        const val INVALID_DATA = "data:text/html,<script>alert(1)</script>"
        const val INVALID_LOCALHOST = "http://localhost:8080"
        const val INVALID_LOCALHOST_HTTPS = "https://localhost"
        const val INVALID_IP_127 = "http://127.0.0.1"
        const val INVALID_IP_INTERNAL = "http://192.168.1.1"
        const val INVALID_IP_INTERNAL_10 = "http://10.0.0.1"
        const val INVALID_IP_INTERNAL_172 = "http://172.16.0.1"
        const val INVALID_CREDS = "https://user:pass@example.com"

        // Malformed URLs
        const val MALFORMED_NO_SCHEME = "example.com"
        const val MALFORMED_SPACES = "https://example .com"
        const val MALFORMED_INVALID_CHARS = "https://exam<>ple.com"
    }

    /**
     * Check if URL should be rejected for security reasons
     *
     * @param url URL to validate
     * @return true if URL should be rejected
     */
    fun shouldRejectUrl(url: String): Boolean {
        val urlLower = url.lowercase()

        // Reject dangerous schemes
        if (urlLower.startsWith("file:") ||
            urlLower.startsWith("javascript:") ||
            urlLower.startsWith("data:") ||
            urlLower.startsWith("vbscript:") ||
            urlLower.startsWith("about:")) {
            return true
        }

        // Reject localhost
        if (urlLower.contains("localhost")) {
            return true
        }

        // Reject internal IP addresses
        val ipPatterns = listOf(
            "127\\.0\\.0\\.1",
            "127\\.\\d+\\.\\d+\\.\\d+",
            "10\\.\\d+\\.\\d+\\.\\d+",
            "172\\.1[6-9]\\.\\d+\\.\\d+",
            "172\\.2[0-9]\\.\\d+\\.\\d+",
            "172\\.3[0-1]\\.\\d+\\.\\d+",
            "192\\.168\\.\\d+\\.\\d+"
        )

        if (ipPatterns.any { pattern -> Regex(pattern).containsMatchIn(urlLower) }) {
            return true
        }

        // Reject URLs with embedded credentials
        val credentialsPattern = Regex("://[^/@]+:[^/@]+@")
        if (credentialsPattern.containsMatchIn(url)) {
            return true
        }

        // Reject malformed URLs
        if (url.contains(" ") || url.contains("<") || url.contains(">")) {
            return true
        }

        return false
    }

    /**
     * Validate URL scheme is allowed
     *
     * @param url URL to validate
     * @return true if scheme is allowed
     */
    fun isAllowedScheme(url: String): Boolean {
        val allowedSchemes = listOf("http", "https", "mailto", "tel", "geo", "sms")
        val scheme = url.substringBefore(":").lowercase()
        return allowedSchemes.contains(scheme)
    }
}
