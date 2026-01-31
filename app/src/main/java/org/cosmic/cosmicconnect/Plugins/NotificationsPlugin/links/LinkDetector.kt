/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.links

import android.text.SpannableString
import android.text.style.URLSpan
import android.util.Log
import android.util.Patterns
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

/**
 * Detects URLs in notification text.
 *
 * Supports two detection methods:
 * 1. Extract URLSpan from SpannableString
 * 2. Detect URLs using regex patterns
 */
class LinkDetector {
    companion object {
        private const val TAG = "COSMIC/LinkDetector"

        // Maximum number of links to extract per notification
        private const val MAX_LINKS = 5

        // Comprehensive URL pattern that handles:
        // - http(s) URLs
        // - Common TLDs without protocol
        // - IP addresses
        // - Ports
        // - Paths and query strings
        private val URL_PATTERN: Pattern = Pattern.compile(
            """(?i)\b(?:(?:https?://)|(?:www\.))[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|]|\b(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+(?:com|org|net|edu|gov|mil|info|biz|name|museum|coop|aero|xyz|io|app|dev)\b(?:[-A-Z0-9+&@#/%?=~_|!:,.;]*[-A-Z0-9+&@#/%=~_|])?""",
            Pattern.CASE_INSENSITIVE
        )
    }

    /**
     * Detect URLs in text.
     *
     * @param text Text to search for URLs (can be SpannableString or plain String)
     * @return List of detected URLs with labels
     */
    fun detectLinks(text: CharSequence?): List<DetectedLink> {
        if (text.isNullOrBlank()) {
            return emptyList()
        }

        val links = mutableListOf<DetectedLink>()

        // Method 1: Extract URLSpan from SpannableString
        if (text is SpannableString) {
            val urlSpans = text.getSpans(0, text.length, URLSpan::class.java)
            for (span in urlSpans) {
                val url = span.url
                if (isValidUrl(url)) {
                    val start = text.getSpanStart(span)
                    val end = text.getSpanEnd(span)
                    val label = if (start >= 0 && end <= text.length) {
                        text.substring(start, end)
                    } else {
                        extractDomain(url)
                    }

                    links.add(DetectedLink(
                        url = url,
                        label = label,
                        source = LinkSource.URL_SPAN
                    ))

                    if (links.size >= MAX_LINKS) {
                        break
                    }
                }
            }
        }

        // Method 2: Detect URLs using regex
        if (links.isEmpty()) {
            val matcher = URL_PATTERN.matcher(text)
            while (matcher.find() && links.size < MAX_LINKS) {
                var url = matcher.group()

                // Add protocol if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }

                if (isValidUrl(url)) {
                    links.add(DetectedLink(
                        url = url,
                        label = extractDomain(url),
                        source = LinkSource.REGEX
                    ))
                }
            }
        }

        // Also try Android's built-in pattern
        if (links.isEmpty()) {
            val matcher = Patterns.WEB_URL.matcher(text)
            while (matcher.find() && links.size < MAX_LINKS) {
                var url = matcher.group()

                // Add protocol if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }

                if (isValidUrl(url)) {
                    links.add(DetectedLink(
                        url = url,
                        label = extractDomain(url),
                        source = LinkSource.ANDROID_PATTERN
                    ))
                }
            }
        }

        Log.d(TAG, "Detected ${links.size} links in notification text")
        return links.distinctBy { it.url }
    }

    /**
     * Validate URL format and security.
     *
     * @param url URL to validate
     * @return true if URL is valid and safe
     */
    private fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) {
            return false
        }

        try {
            val parsed = URL(url)

            // Only allow http and https protocols
            val protocol = parsed.protocol.lowercase()
            if (protocol != "http" && protocol != "https") {
                Log.w(TAG, "Rejecting non-http(s) URL: $url")
                return false
            }

            // Ensure host is present
            if (parsed.host.isNullOrBlank()) {
                Log.w(TAG, "Rejecting URL without host: $url")
                return false
            }

            // Block localhost and local IPs for security
            val host = parsed.host.lowercase()
            if (host == "localhost" ||
                host.startsWith("127.") ||
                host.startsWith("192.168.") ||
                host.startsWith("10.") ||
                host.startsWith("172.16.") ||
                host.startsWith("172.17.") ||
                host.startsWith("172.18.") ||
                host.startsWith("172.19.") ||
                host.startsWith("172.20.") ||
                host.startsWith("172.21.") ||
                host.startsWith("172.22.") ||
                host.startsWith("172.23.") ||
                host.startsWith("172.24.") ||
                host.startsWith("172.25.") ||
                host.startsWith("172.26.") ||
                host.startsWith("172.27.") ||
                host.startsWith("172.28.") ||
                host.startsWith("172.29.") ||
                host.startsWith("172.30.") ||
                host.startsWith("172.31.")) {
                Log.w(TAG, "Rejecting local/private IP URL: $url")
                return false
            }

            return true

        } catch (e: MalformedURLException) {
            Log.w(TAG, "Malformed URL: $url", e)
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Error validating URL: $url", e)
            return false
        }
    }

    /**
     * Extract domain from URL for display.
     *
     * @param url URL to extract domain from
     * @return Domain name or original URL if extraction fails
     */
    private fun extractDomain(url: String): String {
        return try {
            val parsed = URL(url)
            parsed.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}

/**
 * Detected link with metadata.
 */
data class DetectedLink(
    val url: String,
    val label: String,
    val source: LinkSource
)

/**
 * Source of detected link.
 */
enum class LinkSource {
    URL_SPAN,           // Extracted from URLSpan in SpannableString
    REGEX,              // Detected using custom regex
    ANDROID_PATTERN     // Detected using Android's Patterns.WEB_URL
}
