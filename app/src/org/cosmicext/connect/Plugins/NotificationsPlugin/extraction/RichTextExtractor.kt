/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.NotificationsPlugin.extraction

import android.graphics.Typeface
import android.os.Build
import android.service.notification.StatusBarNotification
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Data class representing color information for styled text.
 *
 * @property start Starting character index (inclusive)
 * @property end Ending character index (exclusive)
 * @property color ARGB color value
 */
data class ColorSpan(
    val start: Int,
    val end: Int,
    val color: Int
)

/**
 * Data class representing formatting information extracted from notification text.
 *
 * Stores character ranges for different text formatting styles supported
 * by Android's SpannableString and compatible with Freedesktop notification spec.
 *
 * ## Supported Formatting
 * - **Bold**: StyleSpan with BOLD or BOLD_ITALIC
 * - **Italic**: StyleSpan with ITALIC or BOLD_ITALIC
 * - **Underline**: UnderlineSpan
 * - **Colors**: ForegroundColorSpan with ARGB values
 * - **Links**: URLSpan with href URLs
 *
 * @property boldRanges List of character ranges formatted as bold
 * @property italicRanges List of character ranges formatted as italic
 * @property underlineRanges List of character ranges formatted as underline
 * @property colorSpans List of colored text spans with ARGB values
 * @property linkSpans Map of character ranges to their URL targets
 */
data class TextFormatting(
    val boldRanges: List<IntRange> = emptyList(),
    val italicRanges: List<IntRange> = emptyList(),
    val underlineRanges: List<IntRange> = emptyList(),
    val colorSpans: List<ColorSpan> = emptyList(),
    val linkSpans: Map<IntRange, String> = emptyMap()
) {
    /**
     * Check if this formatting contains any styled content.
     *
     * @return true if any formatting is present, false if all plain text
     */
    fun hasFormatting(): Boolean {
        return boldRanges.isNotEmpty() ||
               italicRanges.isNotEmpty() ||
               underlineRanges.isNotEmpty() ||
               colorSpans.isNotEmpty() ||
               linkSpans.isNotEmpty()
    }
}

/**
 * Extract and convert rich text formatting from Android notifications.
 *
 * This class provides utilities for extracting styled text (bold, italic, underline,
 * colors, links) from Android StatusBarNotifications and converting them to
 * HTML format compatible with Freedesktop Desktop Notifications Specification.
 *
 * ## Supported Formatting
 *
 * ### Android Spans → HTML Tags
 * - `StyleSpan(BOLD)` → `<b>text</b>`
 * - `StyleSpan(ITALIC)` → `<i>text</i>`
 * - `UnderlineSpan` → `<u>text</u>`
 * - `ForegroundColorSpan` → `<span foreground="#RRGGBB">text</span>`
 * - `URLSpan` → `<a href="url">text</a>`
 *
 * ## Freedesktop Compatibility
 *
 * The HTML output follows the subset defined in:
 * https://specifications.freedesktop.org/notification-spec/latest/ar01s04.html
 *
 * Supported tags: `<b>`, `<i>`, `<u>`, `<a href="...">`, `<span foreground="...">`
 *
 * ## Example Usage
 *
 * ```kotlin
 * val extractor = RichTextExtractor()
 * val notification = statusBarNotification
 *
 * // Check if notification has rich content
 * if (extractor.hasRichContent(notification)) {
 *     // Extract as HTML for desktop display
 *     val html = extractor.extractAsHtml(notification)
 *     notificationInfo.titleHtml = html
 *
 *     // Or extract structured formatting
 *     val formatting = extractor.extractFormatting(notification)
 *     if (formatting?.hasFormatting() == true) {
 *         // Process formatting ranges...
 *     }
 * }
 * ```
 *
 * ## Nested Formatting
 *
 * Handles overlapping and nested spans correctly:
 * - Bold + Italic: `<b><i>text</i></b>`
 * - Bold + Color: `<span foreground="#FF0000"><b>text</b></span>`
 *
 * ## Plain Text Fallback
 *
 * If no formatting is detected or extraction fails, returns plain text.
 * Always safe to call - never throws exceptions.
 *
 * @see android.text.Spanned
 * @see android.service.notification.StatusBarNotification
 */
class RichTextExtractor {

    companion object {
        private const val TAG = "COSMIC/RichTextExtractor"

        /**
         * Extract notification extras bundle safely.
         */
        private fun getExtras(notification: android.app.Notification): android.os.Bundle? {
            return NotificationCompat.getExtras(notification)
        }

        /**
         * Extract CharSequence from notification extras.
         */
        private fun extractCharSequence(
            extras: android.os.Bundle?,
            key: String
        ): CharSequence? {
            return extras?.getCharSequence(key)
        }

        /**
         * Convert ARGB color integer to hex string for HTML.
         *
         * @param color ARGB color value
         * @return Hex color string in format #RRGGBB
         */
        private fun colorToHex(color: Int): String {
            return String.format(
                "#%06X",
                (0xFFFFFF and color) // Strip alpha channel
            )
        }

        /**
         * Escape HTML special characters for safe HTML output.
         *
         * @param text Plain text to escape
         * @return HTML-safe text
         */
        private fun escapeHtml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }
    }

    /**
     * Extract formatting information from notification text.
     *
     * Analyzes the notification's title and text fields for styled spans
     * and extracts all formatting information.
     *
     * ## Priority Order
     * 1. EXTRA_TITLE_BIG for title (may have formatting)
     * 2. EXTRA_TITLE for title
     * 3. EXTRA_BIG_TEXT for body text (may have formatting)
     * 4. EXTRA_TEXT for body text
     *
     * @param statusBarNotification The notification to analyze
     * @return TextFormatting with all extracted formatting, or null if no text/formatting found
     */
    fun extractFormatting(statusBarNotification: StatusBarNotification): TextFormatting? {
        try {
            val notification = statusBarNotification.notification
            val extras = getExtras(notification) ?: return null

            // Try to extract title with formatting
            val titleText = extractCharSequence(extras, NotificationCompat.EXTRA_TITLE_BIG)
                ?: extractCharSequence(extras, NotificationCompat.EXTRA_TITLE)

            // Try to extract body text with formatting
            val bodyText = extractCharSequence(extras, NotificationCompat.EXTRA_BIG_TEXT)
                ?: extractCharSequence(extras, NotificationCompat.EXTRA_TEXT)

            // Use whichever has formatting, preferring title
            val text = when {
                titleText is Spanned && hasSpans(titleText) -> titleText
                bodyText is Spanned && hasSpans(bodyText) -> bodyText
                else -> return null // No formatting found
            }

            return extractFormattingFromSpanned(text as Spanned)

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting formatting from notification", e)
            return null
        }
    }

    /**
     * Extract notification text as HTML with formatting preserved.
     *
     * Converts Android spans to Freedesktop-compatible HTML markup.
     * Falls back to plain text if no formatting is present.
     *
     * ## Output Examples
     *
     * ```html
     * <!-- Bold text -->
     * <b>Important message</b>
     *
     * <!-- Multiple formats -->
     * <b><i>Bold and italic</i></b>
     *
     * <!-- Colored text -->
     * <span foreground="#FF0000">Red text</span>
     *
     * <!-- Link -->
     * <a href="https://example.com">Click here</a>
     * ```
     *
     * @param statusBarNotification The notification to extract
     * @return HTML-formatted text, or plain text if no formatting
     */
    fun extractAsHtml(statusBarNotification: StatusBarNotification): String {
        try {
            val notification = statusBarNotification.notification
            val extras = getExtras(notification) ?: return ""

            // Try title with formatting first
            val titleText = extractCharSequence(extras, NotificationCompat.EXTRA_TITLE_BIG)
                ?: extractCharSequence(extras, NotificationCompat.EXTRA_TITLE)

            val bodyText = extractCharSequence(extras, NotificationCompat.EXTRA_BIG_TEXT)
                ?: extractCharSequence(extras, NotificationCompat.EXTRA_TEXT)

            // Convert title if it has spans
            val titleHtml = if (titleText is Spanned && hasSpans(titleText)) {
                spannedToHtml(titleText)
            } else {
                escapeHtml(titleText?.toString() ?: "")
            }

            // Convert body if it has spans
            val bodyHtml = if (bodyText is Spanned && hasSpans(bodyText)) {
                spannedToHtml(bodyText)
            } else {
                escapeHtml(bodyText?.toString() ?: "")
            }

            // Combine title and body
            return when {
                titleHtml.isNotEmpty() && bodyHtml.isNotEmpty() -> "$titleHtml: $bodyHtml"
                titleHtml.isNotEmpty() -> titleHtml
                bodyHtml.isNotEmpty() -> bodyHtml
                else -> ""
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error converting notification to HTML", e)
            // Fallback to plain text extraction
            return extractPlainText(statusBarNotification)
        }
    }

    /**
     * Check if notification contains rich formatted content.
     *
     * Returns true if the notification has any styled spans in title or text.
     *
     * @param statusBarNotification The notification to check
     * @return true if rich content is present, false otherwise
     */
    fun hasRichContent(statusBarNotification: StatusBarNotification): Boolean {
        try {
            val notification = statusBarNotification.notification
            val extras = getExtras(notification) ?: return false

            val titleText = extractCharSequence(extras, NotificationCompat.EXTRA_TITLE_BIG)
                ?: extractCharSequence(extras, NotificationCompat.EXTRA_TITLE)

            val bodyText = extractCharSequence(extras, NotificationCompat.EXTRA_BIG_TEXT)
                ?: extractCharSequence(extras, NotificationCompat.EXTRA_TEXT)

            return (titleText is Spanned && hasSpans(titleText)) ||
                   (bodyText is Spanned && hasSpans(bodyText))

        } catch (e: Exception) {
            Log.e(TAG, "Error checking for rich content", e)
            return false
        }
    }

    // =============================================================================
    // Private Helper Methods
    // =============================================================================

    /**
     * Check if a Spanned object has any relevant formatting spans.
     */
    private fun hasSpans(spanned: Spanned): Boolean {
        val spans = spanned.getSpans(0, spanned.length, CharacterStyle::class.java)
        return spans.any { span ->
            span is StyleSpan || span is UnderlineSpan ||
            span is ForegroundColorSpan || span is URLSpan
        }
    }

    /**
     * Extract formatting information from a Spanned object.
     */
    private fun extractFormattingFromSpanned(spanned: Spanned): TextFormatting {
        val boldRanges = mutableListOf<IntRange>()
        val italicRanges = mutableListOf<IntRange>()
        val underlineRanges = mutableListOf<IntRange>()
        val colorSpans = mutableListOf<ColorSpan>()
        val linkSpans = mutableMapOf<IntRange, String>()

        // Extract StyleSpans (bold, italic)
        val styleSpans = spanned.getSpans(0, spanned.length, StyleSpan::class.java)
        for (span in styleSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                when (span.style) {
                    Typeface.BOLD -> boldRanges.add(start until end)
                    Typeface.ITALIC -> italicRanges.add(start until end)
                    Typeface.BOLD_ITALIC -> {
                        boldRanges.add(start until end)
                        italicRanges.add(start until end)
                    }
                }
            }
        }

        // Extract UnderlineSpans
        val underlineSpans = spanned.getSpans(0, spanned.length, UnderlineSpan::class.java)
        for (span in underlineSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                underlineRanges.add(start until end)
            }
        }

        // Extract ForegroundColorSpans
        val foregroundSpans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        for (span in foregroundSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                colorSpans.add(ColorSpan(start, end, span.foregroundColor))
            }
        }

        // Extract URLSpans
        val urlSpans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        for (span in urlSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                linkSpans[start until end] = span.url
            }
        }

        return TextFormatting(
            boldRanges = boldRanges,
            italicRanges = italicRanges,
            underlineRanges = underlineRanges,
            colorSpans = colorSpans,
            linkSpans = linkSpans
        )
    }

    /**
     * Convert Spanned text to Freedesktop-compatible HTML.
     *
     * Handles nested and overlapping spans correctly by building a proper
     * HTML tree structure.
     */
    private fun spannedToHtml(spanned: Spanned): String {
        val text = spanned.toString()
        if (text.isEmpty()) return ""

        // Create event list for span boundaries
        data class SpanEvent(
            val position: Int,
            val isStart: Boolean,
            val type: String,
            val data: Any? = null
        )

        val events = mutableListOf<SpanEvent>()

        // Collect all span events
        val styleSpans = spanned.getSpans(0, spanned.length, StyleSpan::class.java)
        for (span in styleSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                when (span.style) {
                    Typeface.BOLD -> {
                        events.add(SpanEvent(start, true, "bold"))
                        events.add(SpanEvent(end, false, "bold"))
                    }
                    Typeface.ITALIC -> {
                        events.add(SpanEvent(start, true, "italic"))
                        events.add(SpanEvent(end, false, "italic"))
                    }
                    Typeface.BOLD_ITALIC -> {
                        events.add(SpanEvent(start, true, "bold"))
                        events.add(SpanEvent(start, true, "italic"))
                        events.add(SpanEvent(end, false, "italic"))
                        events.add(SpanEvent(end, false, "bold"))
                    }
                }
            }
        }

        val underlineSpans = spanned.getSpans(0, spanned.length, UnderlineSpan::class.java)
        for (span in underlineSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                events.add(SpanEvent(start, true, "underline"))
                events.add(SpanEvent(end, false, "underline"))
            }
        }

        val colorSpans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        for (span in colorSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                val color = colorToHex(span.foregroundColor)
                events.add(SpanEvent(start, true, "color", color))
                events.add(SpanEvent(end, false, "color", color))
            }
        }

        val urlSpans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        for (span in urlSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            if (start >= 0 && end > start) {
                events.add(SpanEvent(start, true, "link", span.url))
                events.add(SpanEvent(end, false, "link", span.url))
            }
        }

        // Sort events by position, with closing tags before opening tags at same position
        events.sortWith(compareBy({ it.position }, { if (it.isStart) 1 else 0 }))

        // Build HTML
        val html = StringBuilder()
        var lastPos = 0
        val openTags = mutableListOf<Pair<String, Any?>>()

        for (event in events) {
            // Add text before this event
            if (event.position > lastPos) {
                val textSegment = text.substring(lastPos, event.position)
                html.append(escapeHtml(textSegment))
                lastPos = event.position
            }

            // Process event
            if (event.isStart) {
                // Opening tag
                when (event.type) {
                    "bold" -> html.append("<b>")
                    "italic" -> html.append("<i>")
                    "underline" -> html.append("<u>")
                    "color" -> html.append("<span foreground=\"${event.data}\">")
                    "link" -> html.append("<a href=\"${escapeHtml(event.data as String)}\">")
                }
                openTags.add(event.type to event.data)
            } else {
                // Closing tag
                val tagIndex = openTags.indexOfLast { it.first == event.type && it.second == event.data }
                if (tagIndex >= 0) {
                    // Close tags in reverse order from this point
                    for (i in openTags.size - 1 downTo tagIndex) {
                        when (openTags[i].first) {
                            "bold" -> html.append("</b>")
                            "italic" -> html.append("</i>")
                            "underline" -> html.append("</u>")
                            "color" -> html.append("</span>")
                            "link" -> html.append("</a>")
                        }
                    }
                    // Reopen tags after the closed one
                    val tagsToReopen = openTags.subList(tagIndex + 1, openTags.size).toList()
                    openTags.removeAll(openTags.subList(tagIndex, openTags.size))
                    for (tag in tagsToReopen) {
                        when (tag.first) {
                            "bold" -> html.append("<b>")
                            "italic" -> html.append("<i>")
                            "underline" -> html.append("<u>")
                            "color" -> html.append("<span foreground=\"${tag.second}\">")
                            "link" -> html.append("<a href=\"${escapeHtml(tag.second as String)}\">")
                        }
                        openTags.add(tag)
                    }
                }
            }
        }

        // Add remaining text
        if (lastPos < text.length) {
            html.append(escapeHtml(text.substring(lastPos)))
        }

        // Close any remaining open tags (shouldn't happen with well-formed spans)
        for (i in openTags.size - 1 downTo 0) {
            when (openTags[i].first) {
                "bold" -> html.append("</b>")
                "italic" -> html.append("</i>")
                "underline" -> html.append("</u>")
                "color" -> html.append("</span>")
                "link" -> html.append("</a>")
            }
        }

        return html.toString()
    }

    /**
     * Extract plain text from notification as fallback.
     */
    private fun extractPlainText(statusBarNotification: StatusBarNotification): String {
        try {
            val notification = statusBarNotification.notification
            val extras = getExtras(notification) ?: return ""

            val title = extractCharSequence(extras, NotificationCompat.EXTRA_TITLE)?.toString() ?: ""
            val text = extractCharSequence(extras, NotificationCompat.EXTRA_TEXT)?.toString() ?: ""

            return when {
                title.isNotEmpty() && text.isNotEmpty() -> "$title: $text"
                title.isNotEmpty() -> title
                text.isNotEmpty() -> text
                else -> ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting plain text", e)
            return ""
        }
    }
}
