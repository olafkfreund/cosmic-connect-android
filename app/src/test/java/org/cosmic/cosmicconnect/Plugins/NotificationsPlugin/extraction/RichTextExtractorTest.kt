/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction

import android.app.Notification
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import androidx.core.app.NotificationCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RichTextExtractor.
 *
 * Tests extraction of styled text from Android notifications and
 * conversion to Freedesktop-compatible HTML.
 *
 * ## Test Coverage
 * - Bold, italic, underline, color, link extraction
 * - HTML conversion with proper escaping
 * - Nested and overlapping formatting
 * - Plain text fallback
 * - Edge cases and error handling
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class RichTextExtractorTest {

    private lateinit var extractor: RichTextExtractor

    @Before
    fun setup() {
        extractor = RichTextExtractor()
    }

    // =============================================================================
    // Basic Formatting Extraction Tests
    // =============================================================================

    @Test
    fun `extractFormatting should return null for plain text notification`() {
        val notification = createNotification(title = "Plain Title", text = "Plain Text")
        val formatting = extractor.extractFormatting(notification)

        assertNull("Plain text should return null", formatting)
    }

    @Test
    fun `extractFormatting should detect bold text`() {
        val spanned = SpannableString("Bold Text").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 4, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val formatting = extractor.extractFormatting(notification)

        assertNotNull("Should extract formatting", formatting)
        assertTrue("Should have formatting", formatting!!.hasFormatting())
        assertEquals("Should have one bold range", 1, formatting.boldRanges.size)
        assertEquals("Bold range should be correct", 0 until 4, formatting.boldRanges[0])
    }

    @Test
    fun `extractFormatting should detect italic text`() {
        val spanned = SpannableString("Italic Text").apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, 6, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val formatting = extractor.extractFormatting(notification)

        assertNotNull("Should extract formatting", formatting)
        assertEquals("Should have one italic range", 1, formatting!!.italicRanges.size)
        assertEquals("Italic range should be correct", 0 until 6, formatting.italicRanges[0])
    }

    @Test
    fun `extractFormatting should detect bold and italic text`() {
        val spanned = SpannableString("Bold Italic").apply {
            setSpan(StyleSpan(Typeface.BOLD_ITALIC), 0, 11, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val formatting = extractor.extractFormatting(notification)

        assertNotNull("Should extract formatting", formatting)
        assertEquals("Should have one bold range", 1, formatting!!.boldRanges.size)
        assertEquals("Should have one italic range", 1, formatting.italicRanges.size)
        assertEquals("Bold range should be correct", 0 until 11, formatting.boldRanges[0])
        assertEquals("Italic range should be correct", 0 until 11, formatting.italicRanges[0])
    }

    @Test
    fun `extractFormatting should detect underline text`() {
        val spanned = SpannableString("Underline Text").apply {
            setSpan(UnderlineSpan(), 0, 9, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val formatting = extractor.extractFormatting(notification)

        assertNotNull("Should extract formatting", formatting)
        assertEquals("Should have one underline range", 1, formatting!!.underlineRanges.size)
        assertEquals("Underline range should be correct", 0 until 9, formatting.underlineRanges[0])
    }

    @Test
    fun `extractFormatting should detect colored text`() {
        val spanned = SpannableString("Red Text").apply {
            setSpan(ForegroundColorSpan(0xFFFF0000.toInt()), 0, 3, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val formatting = extractor.extractFormatting(notification)

        assertNotNull("Should extract formatting", formatting)
        assertEquals("Should have one color span", 1, formatting!!.colorSpans.size)
        val colorSpan = formatting.colorSpans[0]
        assertEquals("Color start should be correct", 0, colorSpan.start)
        assertEquals("Color end should be correct", 3, colorSpan.end)
        assertEquals("Color should be red", 0xFFFF0000.toInt(), colorSpan.color)
    }

    @Test
    fun `extractFormatting should detect link text`() {
        val spanned = SpannableString("Click here").apply {
            setSpan(URLSpan("https://example.com"), 6, 10, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val formatting = extractor.extractFormatting(notification)

        assertNotNull("Should extract formatting", formatting)
        assertEquals("Should have one link span", 1, formatting!!.linkSpans.size)
        val linkRange = formatting.linkSpans.keys.first()
        assertEquals("Link range should be correct", 6 until 10, linkRange)
        assertEquals("Link URL should be correct", "https://example.com", formatting.linkSpans[linkRange])
    }

    // =============================================================================
    // HTML Conversion Tests
    // =============================================================================

    @Test
    fun `extractAsHtml should convert bold to HTML`() {
        val spanned = SpannableString("Bold Text").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 4, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals("Should convert to bold HTML", "<b>Bold</b> Text", html)
    }

    @Test
    fun `extractAsHtml should convert italic to HTML`() {
        val spanned = SpannableString("Italic Text").apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, 6, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals("Should convert to italic HTML", "<i>Italic</i> Text", html)
    }

    @Test
    fun `extractAsHtml should convert underline to HTML`() {
        val spanned = SpannableString("Underline Text").apply {
            setSpan(UnderlineSpan(), 0, 9, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals("Should convert to underline HTML", "<u>Underline</u> Text", html)
    }

    @Test
    fun `extractAsHtml should convert colored text to HTML`() {
        val spanned = SpannableString("Red Text").apply {
            setSpan(ForegroundColorSpan(0xFFFF0000.toInt()), 0, 3, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals(
            "Should convert to colored HTML",
            "<span foreground=\"#FF0000\">Red</span> Text",
            html
        )
    }

    @Test
    fun `extractAsHtml should convert link to HTML`() {
        val spanned = SpannableString("Click here").apply {
            setSpan(URLSpan("https://example.com"), 6, 10, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals(
            "Should convert to link HTML",
            "Click <a href=\"https://example.com\">here</a>",
            html
        )
    }

    @Test
    fun `extractAsHtml should escape HTML special characters`() {
        val spanned = SpannableString("Text with <tags> & \"quotes\"").apply {
            setSpan(StyleSpan(Typeface.BOLD), 10, 16, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertTrue("Should escape <", html.contains("&lt;"))
        assertTrue("Should escape >", html.contains("&gt;"))
        assertTrue("Should escape &", html.contains("&amp;"))
        assertTrue("Should escape \"", html.contains("&quot;"))
    }

    // =============================================================================
    // Nested and Overlapping Formatting Tests
    // =============================================================================

    @Test
    fun `extractAsHtml should handle nested bold and italic`() {
        val spanned = SpannableString("Bold and Italic").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 15, 0)
            setSpan(StyleSpan(Typeface.ITALIC), 9, 15, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals(
            "Should handle nested formatting",
            "<b>Bold and <i>Italic</i></b>",
            html
        )
    }

    @Test
    fun `extractAsHtml should handle multiple separate formats`() {
        val spanned = SpannableString("Bold then Italic").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 4, 0)
            setSpan(StyleSpan(Typeface.ITALIC), 10, 16, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals(
            "Should handle multiple formats",
            "<b>Bold</b> then <i>Italic</i>",
            html
        )
    }

    @Test
    fun `extractAsHtml should handle overlapping bold and underline`() {
        val spanned = SpannableString("Bold Underline").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 14, 0)
            setSpan(UnderlineSpan(), 5, 14, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals(
            "Should handle overlapping formats",
            "<b>Bold <u>Underline</u></b>",
            html
        )
    }

    @Test
    fun `extractAsHtml should handle complex nested formatting`() {
        val spanned = SpannableString("Complex Formatting").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 18, 0)
            setSpan(StyleSpan(Typeface.ITALIC), 8, 18, 0)
            setSpan(UnderlineSpan(), 8, 18, 0)
            setSpan(ForegroundColorSpan(0xFFFF0000.toInt()), 8, 18, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        // Should produce properly nested tags
        assertTrue("Should contain bold tag", html.contains("<b>"))
        assertTrue("Should contain italic tag", html.contains("<i>"))
        assertTrue("Should contain underline tag", html.contains("<u>"))
        assertTrue("Should contain color span", html.contains("<span foreground=\"#FF0000\">"))
    }

    // =============================================================================
    // hasRichContent Tests
    // =============================================================================

    @Test
    fun `hasRichContent should return false for plain text`() {
        val notification = createNotification(title = "Plain Title", text = "Plain Text")

        val hasRich = extractor.hasRichContent(notification)

        assertFalse("Plain text should not have rich content", hasRich)
    }

    @Test
    fun `hasRichContent should return true for formatted title`() {
        val spanned = SpannableString("Bold Title").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 4, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val hasRich = extractor.hasRichContent(notification)

        assertTrue("Formatted title should have rich content", hasRich)
    }

    @Test
    fun `hasRichContent should return true for formatted text`() {
        val spanned = SpannableString("Italic Text").apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, 6, 0)
        }
        val notification = createNotification(textSpanned = spanned)

        val hasRich = extractor.hasRichContent(notification)

        assertTrue("Formatted text should have rich content", hasRich)
    }

    // =============================================================================
    // Edge Cases and Error Handling
    // =============================================================================

    @Test
    fun `extractFormatting should handle empty notification`() {
        val notification = createNotification(title = "", text = "")

        val formatting = extractor.extractFormatting(notification)

        assertNull("Empty notification should return null", formatting)
    }

    @Test
    fun `extractAsHtml should handle empty spans`() {
        val spanned = SpannableString("Text").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 0, 0) // Empty span
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        assertEquals("Should return plain text", "Text", html)
    }

    @Test
    fun `extractAsHtml should handle invalid span ranges`() {
        val spanned = SpannableString("Text").apply {
            // Negative start should be ignored
            setSpan(StyleSpan(Typeface.BOLD), -1, 2, 0)
        }
        val notification = createNotification(titleSpanned = spanned)

        val html = extractor.extractAsHtml(notification)

        // Should not crash, return reasonable output
        assertNotNull("Should not crash on invalid ranges", html)
    }

    @Test
    fun `extractAsHtml should handle title and text combination`() {
        val titleSpanned = SpannableString("Bold Title").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, 4, 0)
        }
        val textSpanned = SpannableString("Italic Text").apply {
            setSpan(StyleSpan(Typeface.ITALIC), 0, 6, 0)
        }
        val notification = createNotification(
            titleSpanned = titleSpanned,
            textSpanned = textSpanned
        )

        val html = extractor.extractAsHtml(notification)

        // Should combine title and text
        assertTrue("Should contain bold title", html.contains("<b>Bold</b>"))
        assertTrue("Should contain italic text", html.contains("<i>Italic</i>"))
    }

    @Test
    fun `hasFormatting should return false for empty TextFormatting`() {
        val formatting = TextFormatting()

        assertFalse("Empty formatting should return false", formatting.hasFormatting())
    }

    @Test
    fun `hasFormatting should return true for any formatting present`() {
        val formattings = listOf(
            TextFormatting(boldRanges = listOf(0 until 4)),
            TextFormatting(italicRanges = listOf(0 until 4)),
            TextFormatting(underlineRanges = listOf(0 until 4)),
            TextFormatting(colorSpans = listOf(ColorSpan(0, 4, 0xFF0000))),
            TextFormatting(linkSpans = mapOf((0 until 4) to "url"))
        )

        formattings.forEach { formatting ->
            assertTrue("Should return true for any formatting", formatting.hasFormatting())
        }
    }

    // =============================================================================
    // Helper Methods
    // =============================================================================

    /**
     * Create a mock StatusBarNotification with specified content.
     */
    private fun createNotification(
        title: String? = null,
        text: String? = null,
        titleSpanned: SpannableString? = null,
        textSpanned: SpannableString? = null
    ): StatusBarNotification {
        val bundle = Bundle().apply {
            when {
                titleSpanned != null -> putCharSequence(NotificationCompat.EXTRA_TITLE, titleSpanned)
                title != null -> putCharSequence(NotificationCompat.EXTRA_TITLE, title)
            }
            when {
                textSpanned != null -> putCharSequence(NotificationCompat.EXTRA_TEXT, textSpanned)
                text != null -> putCharSequence(NotificationCompat.EXTRA_TEXT, text)
            }
        }

        val notification = mockk<Notification>(relaxed = true) {
            every { extras } returns bundle
        }

        // Mock NotificationCompat.getExtras() static method
        mockkStatic(NotificationCompat::class)
        every { NotificationCompat.getExtras(any()) } returns bundle

        return mockk<StatusBarNotification>(relaxed = true) {
            every { getNotification() } returns notification
        }
    }
}
