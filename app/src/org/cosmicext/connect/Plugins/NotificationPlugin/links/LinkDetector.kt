/*
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 * SPDX-FileCopyrightText: 2025 COSMIC Connect Contributors
 */

package org.cosmicext.connect.Plugins.NotificationPlugin.links

import android.text.Spanned
import android.text.style.URLSpan
import android.util.Patterns
import java.util.regex.Pattern

/**
 * Type of detected link
 */
enum class LinkType {
    WEB,        // http:// https:// www.
    EMAIL,      // mailto: or email pattern
    PHONE,      // tel: or phone pattern
    MAP,        // geo: or maps link
    DEEP_LINK   // App-specific deep link (custom scheme)
}

/**
 * Represents a detected link with metadata
 */
data class DetectedLink(
    val url: String,
    val label: String,
    val type: LinkType,
    val start: Int,
    val end: Int
) {
    /**
     * Get normalized URL with proper scheme
     */
    fun getNormalizedUrl(): String {
        return when (type) {
            LinkType.WEB -> {
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else if (url.startsWith("www.")) {
                    "https://$url"
                } else {
                    "https://$url"
                }
            }
            LinkType.EMAIL -> {
                if (url.startsWith("mailto:")) url else "mailto:$url"
            }
            LinkType.PHONE -> {
                if (url.startsWith("tel:")) url else "tel:$url"
            }
            LinkType.MAP -> {
                if (url.startsWith("geo:")) url else url
            }
            LinkType.DEEP_LINK -> url
        }
    }
}

/**
 * Detects and classifies links in notification text
 */
class LinkDetector {

    companion object {
        // More permissive web URL pattern that catches URLs without protocol
        private val WEB_URL_PATTERN: Pattern = Pattern.compile(
            "(?i)(?:(?:https?://)|(?:www\\.))[^\\s]+",
            Pattern.CASE_INSENSITIVE
        )

        // Phone number pattern - more flexible than Android's default
        private val PHONE_PATTERN: Pattern = Pattern.compile(
            "(?:tel:|callto:)?[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}",
            Pattern.CASE_INSENSITIVE
        )

        // Geo/map pattern
        private val GEO_PATTERN: Pattern = Pattern.compile(
            "(?:geo:|maps\\.google\\.com|goo\\.gl/maps)[^\\s]+",
            Pattern.CASE_INSENSITIVE
        )

        // Deep link pattern (custom schemes)
        private val DEEP_LINK_PATTERN: Pattern = Pattern.compile(
            "(?!(?:https?|mailto|tel|geo):)[a-zA-Z][a-zA-Z0-9+.-]*://[^\\s]+",
            Pattern.CASE_INSENSITIVE
        )

        // Dangerous URL schemes to reject
        private val DANGEROUS_SCHEMES = setOf(
            "javascript:",
            "file:",
            "data:",
            "vbscript:",
            "about:",
            "chrome:",
            "android-app:",  // Unless explicitly allowed
        )
    }

    /**
     * Detect all links in plain text
     */
    fun detectLinks(text: CharSequence): List<DetectedLink> {
        val links = mutableListOf<DetectedLink>()
        val textString = text.toString()

        // Detect web URLs (including www.)
        detectPattern(textString, WEB_URL_PATTERN, LinkType.WEB, links)

        // Detect email addresses
        detectPattern(textString, Patterns.EMAIL_ADDRESS, LinkType.EMAIL, links)

        // Detect phone numbers
        detectPattern(textString, PHONE_PATTERN, LinkType.PHONE, links)

        // Detect geo/map links
        detectPattern(textString, GEO_PATTERN, LinkType.MAP, links)

        // Detect deep links (custom app schemes)
        detectPattern(textString, DEEP_LINK_PATTERN, LinkType.DEEP_LINK, links)

        // Sort by start position and remove overlaps
        return deduplicateLinks(links)
    }

    /**
     * Extract links from Android Spanned text (includes URLSpan)
     */
    fun extractFromSpanned(spanned: Spanned): List<DetectedLink> {
        val links = mutableListOf<DetectedLink>()

        // Extract URLSpan links
        val urlSpans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        for (span in urlSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            val url = span.url

            if (isValidUrl(url)) {
                val label = spanned.substring(start, end)
                val type = classifyLink(url)
                links.add(DetectedLink(url, label, type, start, end))
            }
        }

        // Also detect patterns not covered by URLSpan
        val patternLinks = detectLinks(spanned)

        // Merge and deduplicate
        links.addAll(patternLinks)
        return deduplicateLinks(links)
    }

    /**
     * Classify a URL by type
     */
    fun classifyLink(url: String): LinkType {
        val lowerUrl = url.lowercase()

        return when {
            lowerUrl.startsWith("mailto:") -> LinkType.EMAIL
            lowerUrl.startsWith("tel:") || lowerUrl.startsWith("callto:") -> LinkType.PHONE
            lowerUrl.startsWith("geo:") || lowerUrl.contains("maps.google.com") || lowerUrl.contains("goo.gl/maps") -> LinkType.MAP
            lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://") || lowerUrl.startsWith("www.") -> LinkType.WEB
            Patterns.EMAIL_ADDRESS.matcher(url).matches() -> LinkType.EMAIL
            PHONE_PATTERN.matcher(url).matches() -> LinkType.PHONE
            url.contains("://") -> LinkType.DEEP_LINK  // Custom scheme
            else -> LinkType.WEB  // Default to web
        }
    }

    /**
     * Validate URL for security (reject dangerous schemes)
     */
    fun isValidUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()

        // Reject dangerous schemes
        if (DANGEROUS_SCHEMES.any { lowerUrl.startsWith(it) }) {
            return false
        }

        // Reject URLs with embedded scripts
        if (lowerUrl.contains("<script") || lowerUrl.contains("javascript:") || lowerUrl.contains("vbscript:")) {
            return false
        }

        // Reject malformed URLs
        if (url.isBlank() || url.length > 2048) {  // Max URL length
            return false
        }

        // Must match at least one valid pattern
        return when (classifyLink(url)) {
            LinkType.WEB -> WEB_URL_PATTERN.matcher(url).find() || Patterns.WEB_URL.matcher(url).matches()
            LinkType.EMAIL -> Patterns.EMAIL_ADDRESS.matcher(url).matches() || url.startsWith("mailto:")
            LinkType.PHONE -> PHONE_PATTERN.matcher(url).find() || url.startsWith("tel:")
            LinkType.MAP -> GEO_PATTERN.matcher(url).find()
            LinkType.DEEP_LINK -> url.contains("://") && !DANGEROUS_SCHEMES.any { url.startsWith(it) }
        }
    }

    /**
     * Detect links matching a specific pattern
     */
    private fun detectPattern(
        text: String,
        pattern: Pattern,
        type: LinkType,
        links: MutableList<DetectedLink>
    ) {
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val url = matcher.group()
            val start = matcher.start()
            val end = matcher.end()

            if (isValidUrl(url)) {
                links.add(DetectedLink(url, url, type, start, end))
            }
        }
    }

    /**
     * Remove overlapping links, preferring earlier matches
     */
    private fun deduplicateLinks(links: List<DetectedLink>): List<DetectedLink> {
        if (links.isEmpty()) return emptyList()

        val sorted = links.sortedWith(
            compareBy<DetectedLink> { it.start }
                .thenBy { it.end }
        )

        val deduplicated = mutableListOf<DetectedLink>()
        var lastEnd = -1

        for (link in sorted) {
            // Skip if this link overlaps with the previous one
            if (link.start >= lastEnd) {
                deduplicated.add(link)
                lastEnd = link.end
            }
        }

        return deduplicated
    }
}
