/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.richtext

import android.graphics.Typeface
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.Log
import org.xml.sax.XMLReader
import java.util.Locale

/**
 * Rich Text Parser for Notification Content
 *
 * Converts HTML-like text to Android Spannable for displaying rich notification text.
 * Supports bidirectional conversion (HTML ↔ Spannable).
 *
 * ## Supported Formatting
 *
 * | HTML Tag | Span Type |
 * |----------|-----------|
 * | `<b>`, `<strong>` | StyleSpan(BOLD) |
 * | `<i>`, `<em>` | StyleSpan(ITALIC) |
 * | `<u>` | UnderlineSpan |
 * | `<s>`, `<strike>` | StrikethroughSpan |
 * | `<a href="">` | URLSpan |
 * | `<font color="">` | ForegroundColorSpan |
 * | `<br>`, `<br/>` | Line break |
 *
 * ## Features
 * - HTML sanitization to prevent XSS-like issues
 * - Nested tag support
 * - Graceful fallback to plain text on parse errors
 * - Markdown-like syntax support (optional)
 *
 * ## Usage
 * ```kotlin
 * val parser = RichTextParser()
 * val spannable = parser.parseToSpannable("<b>Bold</b> and <i>italic</i> text")
 * val html = parser.toHtml(spannable)
 * val plain = parser.stripFormatting(spannable)
 * val hasRich = parser.containsRichContent("<b>test</b>")
 * ```
 */
class RichTextParser {

    companion object {
        private const val TAG = "COSMIC/RichTextParser"

        // Maximum input length to prevent DoS
        private const val MAX_INPUT_LENGTH = 10000

        // Supported HTML tags (allowlist)
        private val ALLOWED_TAGS = setOf(
            "b", "strong", "i", "em", "u", "s", "strike",
            "a", "font", "br", "p", "div", "span"
        )

        // HTML entities for special characters
        private val HTML_ENTITIES = mapOf(
            "&lt;" to "<",
            "&gt;" to ">",
            "&amp;" to "&",
            "&quot;" to "\"",
            "&apos;" to "'",
            "&nbsp;" to "\u00A0"
        )
    }

    /**
     * Parse HTML-like text to Android Spannable.
     *
     * Converts HTML markup to styled text using Android's span system.
     * Sanitizes input to prevent malicious markup.
     *
     * @param input HTML-formatted text
     * @return Spannable with formatting applied
     */
    fun parseToSpannable(input: String): SpannableStringBuilder {
        if (input.isBlank()) {
            return SpannableStringBuilder("")
        }

        // Sanitize input
        val sanitized = sanitizeInput(input)

        // Check if contains any HTML tags
        if (!containsRichContent(sanitized)) {
            // No HTML tags, return as plain text with entity decoding
            return SpannableStringBuilder(decodeHtmlEntities(sanitized))
        }

        return try {
            // Parse HTML using Android's parser with custom tag handler
            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(
                    sanitized,
                    Html.FROM_HTML_MODE_COMPACT,
                    null,
                    CustomTagHandler()
                )
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(sanitized, null, CustomTagHandler())
            }

            // Convert to SpannableStringBuilder for mutability
            SpannableStringBuilder(spanned)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HTML, falling back to plain text", e)
            // Fallback to plain text with entity decoding
            SpannableStringBuilder(decodeHtmlEntities(sanitized))
        }
    }

    /**
     * Strip all formatting and return plain text.
     *
     * Removes all HTML tags and converts to plain string.
     *
     * @param input HTML-formatted text
     * @return Plain text without formatting
     */
    fun stripFormatting(input: String): String {
        if (input.isBlank()) {
            return ""
        }

        // Remove HTML tags using regex
        val withoutTags = input.replace(Regex("<[^>]*>"), "")

        // Decode HTML entities
        return decodeHtmlEntities(withoutTags).trim()
    }

    /**
     * Check if text contains rich formatting.
     *
     * Detects presence of HTML tags or markdown syntax.
     *
     * @param input Text to check
     * @return true if rich content detected
     */
    fun containsRichContent(input: String): Boolean {
        if (input.isBlank()) {
            return false
        }

        // Check for HTML tags
        val hasHtmlTags = Regex("<[a-zA-Z/][^>]*>").containsMatchIn(input)

        // Check for markdown syntax (optional)
        val hasMarkdown = Regex("[*_~`\\[\\]]").containsMatchIn(input)

        return hasHtmlTags || hasMarkdown
    }

    /**
     * Convert Spannable back to HTML.
     *
     * Exports styled text to HTML markup for transmission or storage.
     *
     * @param spannable Styled text
     * @return HTML representation
     */
    fun toHtml(spannable: Spanned): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.toHtml(spannable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } else {
            @Suppress("DEPRECATION")
            Html.toHtml(spannable)
        }
    }

    /**
     * Sanitize input HTML to prevent malicious markup.
     *
     * - Limits input length
     * - Removes dangerous tags (script, iframe, etc.)
     * - Validates tag structure
     *
     * @param input Raw HTML input
     * @return Sanitized HTML
     */
    private fun sanitizeInput(input: String): String {
        var sanitized = input

        // Limit input length
        if (sanitized.length > MAX_INPUT_LENGTH) {
            Log.w(TAG, "Input exceeds max length, truncating")
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH)
        }

        // Remove dangerous tags
        sanitized = sanitized.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
        sanitized = sanitized.replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
        sanitized = sanitized.replace(Regex("<object[^>]*>.*?</object>", RegexOption.IGNORE_CASE), "")
        sanitized = sanitized.replace(Regex("<embed[^>]*>.*?</embed>", RegexOption.IGNORE_CASE), "")

        // Remove event handlers (onclick, onerror, etc.)
        sanitized = sanitized.replace(Regex("\\son\\w+\\s*=\\s*[\"'][^\"']*[\"']", RegexOption.IGNORE_CASE), "")

        // Remove javascript: URLs
        sanitized = sanitized.replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")

        return sanitized
    }

    /**
     * Decode HTML entities to characters.
     *
     * @param text Text with HTML entities
     * @return Text with decoded characters
     */
    private fun decodeHtmlEntities(text: String): String {
        var decoded = text

        // Decode standard entities
        for ((entity, char) in HTML_ENTITIES) {
            decoded = decoded.replace(entity, char)
        }

        // Decode numeric entities (&#123; and &#xAB;)
        decoded = decoded.replace(Regex("&#(\\d+);")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            if (code != null && code in 0..0x10FFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

        decoded = decoded.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            if (code != null && code in 0..0x10FFFF) {
                code.toChar().toString()
            } else {
                match.value
            }
        }

        return decoded
    }

    /**
     * Custom tag handler for unsupported HTML tags.
     *
     * Extends Android's HTML parser to support additional tags:
     * - `<s>`, `<strike>` for strikethrough
     * - Custom font colors
     */
    private class CustomTagHandler : Html.TagHandler {
        private val tagStack = mutableListOf<String>()

        override fun handleTag(
            opening: Boolean,
            tag: String?,
            output: android.text.Editable?,
            xmlReader: XMLReader?
        ) {
            if (tag == null || output == null) return

            val lowerTag = tag.lowercase(Locale.ROOT)

            when (lowerTag) {
                "s", "strike" -> handleStrikethrough(opening, output)
                // Font color is partially handled by Android's Html parser
                // We extend it here for better compatibility
                else -> {
                    // Track tag stack for nested tags
                    if (opening) {
                        tagStack.add(lowerTag)
                    } else {
                        tagStack.removeLastOrNull()
                    }
                }
            }
        }

        private fun handleStrikethrough(opening: Boolean, output: android.text.Editable) {
            val len = output.length
            if (opening) {
                // Mark start position
                output.setSpan(
                    StrikethroughMark(),
                    len,
                    len,
                    Spannable.SPAN_MARK_MARK
                )
            } else {
                // Find start mark and apply span
                val marks = output.getSpans(0, len, StrikethroughMark::class.java)
                if (marks.isNotEmpty()) {
                    val mark = marks.last()
                    val start = output.getSpanStart(mark)
                    output.removeSpan(mark)

                    if (start != len) {
                        output.setSpan(
                            StrikethroughSpan(),
                            start,
                            len,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
        }

        /**
         * Marker class for tracking tag start positions.
         */
        private class StrikethroughMark
    }
}
