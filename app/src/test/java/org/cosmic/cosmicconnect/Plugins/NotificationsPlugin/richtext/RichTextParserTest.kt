/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.richtext

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RichTextParser.
 *
 * Tests HTML parsing, span conversion, sanitization, and edge cases.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Target Android 9.0 (API 28) for testing
class RichTextParserTest {

    private lateinit var parser: RichTextParser

    @Before
    fun setup() {
        parser = RichTextParser()
    }

    // ============================================================================
    // Basic Parsing Tests
    // ============================================================================

    @Test
    fun testParseEmptyString() {
        val result = parser.parseToSpannable("")
        assertEquals("", result.toString())
    }

    @Test
    fun testParsePlainText() {
        val input = "Plain text without formatting"
        val result = parser.parseToSpannable(input)
        assertEquals(input, result.toString())
    }

    @Test
    fun testParseBoldTag() {
        val result = parser.parseToSpannable("<b>Bold text</b>")
        assertEquals("Bold text", result.toString())

        val spans = result.getSpans(0, result.length, StyleSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(android.graphics.Typeface.BOLD, spans[0].style)
    }

    @Test
    fun testParseStrongTag() {
        val result = parser.parseToSpannable("<strong>Strong text</strong>")
        assertEquals("Strong text", result.toString())

        val spans = result.getSpans(0, result.length, StyleSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(android.graphics.Typeface.BOLD, spans[0].style)
    }

    @Test
    fun testParseItalicTag() {
        val result = parser.parseToSpannable("<i>Italic text</i>")
        assertEquals("Italic text", result.toString())

        val spans = result.getSpans(0, result.length, StyleSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(android.graphics.Typeface.ITALIC, spans[0].style)
    }

    @Test
    fun testParseEmTag() {
        val result = parser.parseToSpannable("<em>Emphasized text</em>")
        assertEquals("Emphasized text", result.toString())

        val spans = result.getSpans(0, result.length, StyleSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(android.graphics.Typeface.ITALIC, spans[0].style)
    }

    @Test
    fun testParseUnderlineTag() {
        val result = parser.parseToSpannable("<u>Underlined text</u>")
        assertEquals("Underlined text", result.toString())

        val spans = result.getSpans(0, result.length, UnderlineSpan::class.java)
        assertEquals(1, spans.size)
    }

    @Test
    fun testParseStrikethroughTag() {
        val result = parser.parseToSpannable("<s>Strikethrough text</s>")
        assertEquals("Strikethrough text", result.toString())

        val spans = result.getSpans(0, result.length, StrikethroughSpan::class.java)
        assertEquals(1, spans.size)
    }

    @Test
    fun testParseStrikeTag() {
        val result = parser.parseToSpannable("<strike>Strike text</strike>")
        assertEquals("Strike text", result.toString())

        val spans = result.getSpans(0, result.length, StrikethroughSpan::class.java)
        assertEquals(1, spans.size)
    }

    @Test
    fun testParseLinkTag() {
        val result = parser.parseToSpannable("<a href=\"https://example.com\">Click here</a>")
        assertEquals("Click here", result.toString())

        val spans = result.getSpans(0, result.length, URLSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals("https://example.com", spans[0].url)
    }

    @Test
    fun testParseLineBreak() {
        val result = parser.parseToSpannable("Line 1<br>Line 2")
        assertTrue(result.toString().contains("\n"))
    }

    @Test
    fun testParseSelfClosingLineBreak() {
        val result = parser.parseToSpannable("Line 1<br/>Line 2")
        assertTrue(result.toString().contains("\n"))
    }

    // ============================================================================
    // Nested Tags Tests
    // ============================================================================

    @Test
    fun testParseNestedBoldItalic() {
        val result = parser.parseToSpannable("<b><i>Bold and italic</i></b>")
        assertEquals("Bold and italic", result.toString())

        val boldSpans = result.getSpans(0, result.length, StyleSpan::class.java)
            .filter { it.style == android.graphics.Typeface.BOLD }
        val italicSpans = result.getSpans(0, result.length, StyleSpan::class.java)
            .filter { it.style == android.graphics.Typeface.ITALIC }

        assertTrue(boldSpans.isNotEmpty())
        assertTrue(italicSpans.isNotEmpty())
    }

    @Test
    fun testParseComplexNesting() {
        val result = parser.parseToSpannable("<b>Bold <i>and italic</i> and <u>underline</u></b>")
        assertTrue(result.toString().contains("Bold and italic and underline"))

        val spans = result.getSpans(0, result.length, Any::class.java)
        assertTrue(spans.isNotEmpty())
    }

    @Test
    fun testParseMultipleFormattingTypes() {
        val input = "<b>Bold</b> and <i>italic</i> and <u>underline</u>"
        val result = parser.parseToSpannable(input)

        val boldSpans = result.getSpans(0, result.length, StyleSpan::class.java)
            .filter { it.style == android.graphics.Typeface.BOLD }
        val italicSpans = result.getSpans(0, result.length, StyleSpan::class.java)
            .filter { it.style == android.graphics.Typeface.ITALIC }
        val underlineSpans = result.getSpans(0, result.length, UnderlineSpan::class.java)

        assertTrue(boldSpans.isNotEmpty())
        assertTrue(italicSpans.isNotEmpty())
        assertTrue(underlineSpans.isNotEmpty())
    }

    // ============================================================================
    // HTML Entities Tests
    // ============================================================================

    @Test
    fun testParseHtmlEntities() {
        val result = parser.parseToSpannable("&lt;tag&gt; &amp; &quot;quotes&quot;")
        assertEquals("<tag> & \"quotes\"", result.toString())
    }

    @Test
    fun testParseNumericEntities() {
        val result = parser.parseToSpannable("&#65; &#x42; &#67;")
        assertEquals("A B C", result.toString())
    }

    @Test
    fun testParseNonBreakingSpace() {
        val result = parser.parseToSpannable("word&nbsp;word")
        assertTrue(result.toString().contains("\u00A0"))
    }

    // ============================================================================
    // Sanitization Tests
    // ============================================================================

    @Test
    fun testSanitizeScriptTag() {
        val result = parser.parseToSpannable("<script>alert('xss')</script>Safe text")
        assertEquals("Safe text", result.toString().trim())
    }

    @Test
    fun testSanitizeIframeTag() {
        val result = parser.parseToSpannable("<iframe src='evil'></iframe>Safe text")
        assertEquals("Safe text", result.toString().trim())
    }

    @Test
    fun testSanitizeEventHandlers() {
        val result = parser.parseToSpannable("<div onclick='alert(1)'>Click me</div>")
        // Should parse content but remove onclick
        assertTrue(result.toString().contains("Click me"))
    }

    @Test
    fun testSanitizeJavaScriptUrls() {
        val result = parser.parseToSpannable("<a href='javascript:alert(1)'>Link</a>")
        val spans = result.getSpans(0, result.length, URLSpan::class.java)
        // URL should be sanitized (not contain javascript:)
        if (spans.isNotEmpty()) {
            assertFalse(spans[0].url.lowercase().contains("javascript"))
        }
    }

    @Test
    fun testSanitizeLongInput() {
        val longInput = "a".repeat(20000) + "<b>bold</b>"
        val result = parser.parseToSpannable(longInput)
        // Should truncate and still parse remaining valid HTML
        assertTrue(result.length <= 10100) // MAX_INPUT_LENGTH + some buffer
    }

    // ============================================================================
    // Strip Formatting Tests
    // ============================================================================

    @Test
    fun testStripFormattingSimple() {
        val input = "<b>Bold</b> and <i>italic</i>"
        val result = parser.stripFormatting(input)
        assertEquals("Bold and italic", result)
    }

    @Test
    fun testStripFormattingComplex() {
        val input = "<b>Bold <i>and italic</i></b> <a href='test'>link</a>"
        val result = parser.stripFormatting(input)
        assertEquals("Bold and italic link", result)
    }

    @Test
    fun testStripFormattingWithEntities() {
        val input = "&lt;b&gt;Not bold&lt;/b&gt; &amp; text"
        val result = parser.stripFormatting(input)
        assertEquals("<b>Not bold</b> & text", result)
    }

    @Test
    fun testStripFormattingEmptyString() {
        val result = parser.stripFormatting("")
        assertEquals("", result)
    }

    @Test
    fun testStripFormattingPlainText() {
        val input = "Plain text"
        val result = parser.stripFormatting(input)
        assertEquals(input, result)
    }

    // ============================================================================
    // Rich Content Detection Tests
    // ============================================================================

    @Test
    fun testContainsRichContentTrue() {
        assertTrue(parser.containsRichContent("<b>text</b>"))
        assertTrue(parser.containsRichContent("<i>text</i>"))
        assertTrue(parser.containsRichContent("text <br> text"))
        assertTrue(parser.containsRichContent("<a href='test'>link</a>"))
    }

    @Test
    fun testContainsRichContentFalse() {
        assertFalse(parser.containsRichContent("Plain text"))
        assertFalse(parser.containsRichContent(""))
        assertFalse(parser.containsRichContent("   "))
    }

    @Test
    fun testContainsRichContentMarkdown() {
        // Should detect markdown-like syntax
        assertTrue(parser.containsRichContent("*bold* text"))
        assertTrue(parser.containsRichContent("_italic_ text"))
        assertTrue(parser.containsRichContent("[link](url)"))
    }

    @Test
    fun testContainsRichContentEscapedTags() {
        // Escaped tags should not be considered rich content
        assertFalse(parser.containsRichContent("&lt;b&gt;text&lt;/b&gt;"))
    }

    // ============================================================================
    // Bidirectional Conversion Tests
    // ============================================================================

    @Test
    fun testToHtmlBold() {
        val spannable = parser.parseToSpannable("<b>Bold text</b>")
        val html = parser.toHtml(spannable)
        assertTrue(html.lowercase().contains("<b>") || html.lowercase().contains("<strong>"))
    }

    @Test
    fun testToHtmlItalic() {
        val spannable = parser.parseToSpannable("<i>Italic text</i>")
        val html = parser.toHtml(spannable)
        assertTrue(html.lowercase().contains("<i>") || html.lowercase().contains("<em>"))
    }

    @Test
    fun testToHtmlLink() {
        val spannable = parser.parseToSpannable("<a href=\"https://example.com\">Link</a>")
        val html = parser.toHtml(spannable)
        assertTrue(html.lowercase().contains("<a "))
        assertTrue(html.lowercase().contains("href"))
    }

    @Test
    fun testRoundTripConversion() {
        val original = "<b>Bold</b> and <i>italic</i> text"
        val spannable = parser.parseToSpannable(original)
        val html = parser.toHtml(spannable)
        val spannableAgain = parser.parseToSpannable(html)

        // Content should be preserved
        assertEquals(
            parser.stripFormatting(original),
            parser.stripFormatting(spannableAgain.toString())
        )
    }

    // ============================================================================
    // Edge Cases Tests
    // ============================================================================

    @Test
    fun testParseMalformedHtml() {
        // Unclosed tags
        val result1 = parser.parseToSpannable("<b>Unclosed tag")
        assertTrue(result1.toString().contains("Unclosed tag"))

        // Mismatched tags
        val result2 = parser.parseToSpannable("<b>Bold<i>Italic</b></i>")
        assertTrue(result2.toString().contains("Bold"))
        assertTrue(result2.toString().contains("Italic"))
    }

    @Test
    fun testParseInvalidTagNames() {
        val result = parser.parseToSpannable("<invalid>Text</invalid>")
        assertEquals("Text", result.toString().trim())
    }

    @Test
    fun testParseWhitespaceOnly() {
        val result = parser.parseToSpannable("   ")
        assertTrue(result.toString().isBlank())
    }

    @Test
    fun testParseSpecialCharacters() {
        val input = "Special: © ® ™ € £ ¥"
        val result = parser.parseToSpannable(input)
        assertEquals(input, result.toString())
    }

    @Test
    fun testParseUnicodeEmoji() {
        val input = "Emoji: 😀 🎉 ❤️"
        val result = parser.parseToSpannable(input)
        assertTrue(result.toString().contains("😀"))
        assertTrue(result.toString().contains("🎉"))
    }

    @Test
    fun testParseMixedContent() {
        val input = "Normal text <b>bold</b> normal <i>italic</i> <u>underline</u> end"
        val result = parser.parseToSpannable(input)
        assertTrue(result.toString().contains("Normal text"))
        assertTrue(result.toString().contains("bold"))
        assertTrue(result.toString().contains("italic"))
        assertTrue(result.toString().contains("underline"))
        assertTrue(result.toString().contains("end"))
    }

    @Test
    fun testParseEmptyTags() {
        val result = parser.parseToSpannable("<b></b><i></i>Text")
        assertEquals("Text", result.toString().trim())
    }

    @Test
    fun testParseConsecutiveSpaces() {
        val input = "Multiple    spaces    between    words"
        val result = parser.parseToSpannable(input)
        // HTML collapses consecutive spaces
        assertTrue(result.toString().length < input.length)
    }

    // ============================================================================
    // Performance Tests
    // ============================================================================

    @Test
    fun testParsePerformanceLargeText() {
        val largeInput = buildString {
            repeat(100) {
                append("<b>Bold text $it</b> ")
                append("<i>Italic text $it</i> ")
                append("Plain text $it ")
            }
        }

        val startTime = System.nanoTime()
        val result = parser.parseToSpannable(largeInput)
        val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms

        assertTrue(result.isNotEmpty())
        assertTrue(duration < 1000) // Should complete in less than 1 second
    }

    @Test
    fun testStripPerformanceLargeText() {
        val largeInput = buildString {
            repeat(1000) {
                append("<b>Text $it</b> ")
            }
        }

        val startTime = System.nanoTime()
        val result = parser.stripFormatting(largeInput)
        val duration = (System.nanoTime() - startTime) / 1_000_000

        assertTrue(result.isNotEmpty())
        assertTrue(duration < 500) // Should be fast
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Test
    fun testRealWorldNotificationText() {
        val input = "<b>New Message</b><br>From: <i>John Doe</i><br>Hey, check out <a href=\"https://example.com\">this link</a>!"
        val result = parser.parseToSpannable(input)

        assertTrue(result.toString().contains("New Message"))
        assertTrue(result.toString().contains("John Doe"))
        assertTrue(result.toString().contains("this link"))

        // Should have multiple span types
        val allSpans = result.getSpans(0, result.length, Any::class.java)
        assertTrue(allSpans.size > 1)
    }

    @Test
    fun testMessagingAppNotification() {
        val input = "<b>WhatsApp</b><br><i>Alice:</i> <b>Can you help with the project?</b>"
        val result = parser.parseToSpannable(input)

        assertTrue(result.toString().contains("WhatsApp"))
        assertTrue(result.toString().contains("Alice"))
        assertTrue(result.toString().contains("Can you help"))
    }

    @Test
    fun testEmailNotification() {
        val input = "<b>New Email</b><br>From: <i>boss@company.com</i><br>Subject: <u>Urgent: Review needed</u>"
        val result = parser.parseToSpannable(input)

        assertTrue(result.toString().contains("New Email"))
        assertTrue(result.toString().contains("boss@company.com"))
        assertTrue(result.toString().contains("Urgent: Review needed"))
    }
}
