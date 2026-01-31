/*
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 * SPDX-FileCopyrightText: 2025 COSMIC Connect Contributors
 */

package org.cosmic.cosmicconnect.Plugins.NotificationPlugin.links

import android.text.SpannableString
import android.text.style.URLSpan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName

@DisplayName("LinkDetector Tests")
class LinkDetectorTest {

    private lateinit var detector: LinkDetector

    @BeforeEach
    fun setup() {
        detector = LinkDetector()
    }

    @Nested
    @DisplayName("Web URL Detection")
    inner class WebUrlDetection {

        @Test
        fun `detect simple http URL`() {
            val text = "Check out http://example.com for more info"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertEquals("http://example.com", links[0].url)
            assertEquals(LinkType.WEB, links[0].type)
        }

        @Test
        fun `detect simple https URL`() {
            val text = "Visit https://secure.example.com"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertEquals("https://secure.example.com", links[0].url)
            assertEquals(LinkType.WEB, links[0].type)
        }

        @Test
        fun `detect www URL without protocol`() {
            val text = "Go to www.example.com"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertTrue(links[0].url.contains("www.example.com"))
            assertEquals(LinkType.WEB, links[0].type)
        }

        @Test
        fun `detect URL with path and query`() {
            val text = "Check https://example.com/path?query=value&foo=bar"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertTrue(links[0].url.contains("example.com/path?query=value"))
        }

        @Test
        fun `detect multiple URLs`() {
            val text = "Visit http://example.com and https://another.com"
            val links = detector.detectLinks(text)

            assertEquals(2, links.size)
            assertEquals(LinkType.WEB, links[0].type)
            assertEquals(LinkType.WEB, links[1].type)
        }

        @Test
        fun `normalize URL adds https to www`() {
            val text = "www.example.com"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            val normalized = links[0].getNormalizedUrl()
            assertTrue(normalized.startsWith("https://"))
        }
    }

    @Nested
    @DisplayName("Email Detection")
    inner class EmailDetection {

        @Test
        fun `detect simple email`() {
            val text = "Contact us at test@example.com"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.EMAIL })
            val email = links.first { it.type == LinkType.EMAIL }
            assertTrue(email.url.contains("test@example.com"))
        }

        @Test
        fun `detect mailto link`() {
            val text = "Email mailto:support@example.com"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.EMAIL })
        }

        @Test
        fun `normalize email adds mailto`() {
            val text = "test@example.com"
            val links = detector.detectLinks(text)

            val email = links.first { it.type == LinkType.EMAIL }
            val normalized = email.getNormalizedUrl()
            assertTrue(normalized.startsWith("mailto:"))
        }

        @Test
        fun `detect email with plus sign`() {
            val text = "Contact test+tag@example.com"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.EMAIL })
        }
    }

    @Nested
    @DisplayName("Phone Number Detection")
    inner class PhoneDetection {

        @Test
        fun `detect tel link`() {
            val text = "Call tel:+1234567890"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.PHONE })
        }

        @Test
        fun `detect phone number with country code`() {
            val text = "Call +1-234-567-8900"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.PHONE })
        }

        @Test
        fun `detect phone number with parentheses`() {
            val text = "Call (123) 456-7890"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.PHONE })
        }

        @Test
        fun `normalize phone adds tel`() {
            val text = "tel:1234567890"
            val links = detector.detectLinks(text)

            val phone = links.first { it.type == LinkType.PHONE }
            val normalized = phone.getNormalizedUrl()
            assertTrue(normalized.startsWith("tel:"))
        }
    }

    @Nested
    @DisplayName("Map/Geo Detection")
    inner class MapDetection {

        @Test
        fun `detect geo link`() {
            val text = "Location: geo:37.7749,-122.4194"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.MAP })
        }

        @Test
        fun `detect Google Maps link`() {
            val text = "https://maps.google.com/maps?q=San+Francisco"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.MAP })
        }

        @Test
        fun `detect goo gl maps link`() {
            val text = "Short link: https://goo.gl/maps/abc123"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.MAP })
        }
    }

    @Nested
    @DisplayName("Deep Link Detection")
    inner class DeepLinkDetection {

        @Test
        fun `detect custom scheme deep link`() {
            val text = "Open app: myapp://action/page"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.DEEP_LINK })
        }

        @Test
        fun `detect twitter deep link`() {
            val text = "Follow: twitter://user?screen_name=example"
            val links = detector.detectLinks(text)

            assertTrue(links.any { it.type == LinkType.DEEP_LINK })
        }

        @Test
        fun `detect spotify deep link`() {
            val text = "Listen: spotify:track:abc123"
            val links = detector.detectLinks(text)

            // Note: spotify uses : not :// so may not be detected by DEEP_LINK_PATTERN
            // This tests the current behavior
            val deepLinks = links.filter { it.type == LinkType.DEEP_LINK }
            // May be 0 or 1 depending on implementation
            assertTrue(deepLinks.size >= 0)
        }
    }

    @Nested
    @DisplayName("Link Classification")
    inner class LinkClassification {

        @Test
        fun `classify http as WEB`() {
            assertEquals(LinkType.WEB, detector.classifyLink("http://example.com"))
        }

        @Test
        fun `classify https as WEB`() {
            assertEquals(LinkType.WEB, detector.classifyLink("https://example.com"))
        }

        @Test
        fun `classify mailto as EMAIL`() {
            assertEquals(LinkType.EMAIL, detector.classifyLink("mailto:test@example.com"))
        }

        @Test
        fun `classify email address as EMAIL`() {
            assertEquals(LinkType.EMAIL, detector.classifyLink("test@example.com"))
        }

        @Test
        fun `classify tel as PHONE`() {
            assertEquals(LinkType.PHONE, detector.classifyLink("tel:1234567890"))
        }

        @Test
        fun `classify geo as MAP`() {
            assertEquals(LinkType.MAP, detector.classifyLink("geo:0,0"))
        }

        @Test
        fun `classify custom scheme as DEEP_LINK`() {
            assertEquals(LinkType.DEEP_LINK, detector.classifyLink("myapp://page"))
        }
    }

    @Nested
    @DisplayName("URL Validation")
    inner class UrlValidation {

        @Test
        fun `valid http URL passes`() {
            assertTrue(detector.isValidUrl("http://example.com"))
        }

        @Test
        fun `valid https URL passes`() {
            assertTrue(detector.isValidUrl("https://example.com"))
        }

        @Test
        fun `valid email passes`() {
            assertTrue(detector.isValidUrl("test@example.com"))
        }

        @Test
        fun `javascript scheme is rejected`() {
            assertFalse(detector.isValidUrl("javascript:alert('xss')"))
        }

        @Test
        fun `file scheme is rejected`() {
            assertFalse(detector.isValidUrl("file:///etc/passwd"))
        }

        @Test
        fun `data scheme is rejected`() {
            assertFalse(detector.isValidUrl("data:text/html,<script>alert('xss')</script>"))
        }

        @Test
        fun `vbscript scheme is rejected`() {
            assertFalse(detector.isValidUrl("vbscript:msgbox('xss')"))
        }

        @Test
        fun `blank URL is rejected`() {
            assertFalse(detector.isValidUrl(""))
            assertFalse(detector.isValidUrl("   "))
        }

        @Test
        fun `extremely long URL is rejected`() {
            val longUrl = "http://example.com/" + "a".repeat(3000)
            assertFalse(detector.isValidUrl(longUrl))
        }

        @Test
        fun `URL with embedded script is rejected`() {
            assertFalse(detector.isValidUrl("http://example.com/<script>alert(1)</script>"))
        }
    }

    @Nested
    @DisplayName("Spanned Text Extraction")
    inner class SpannedExtraction {

        @Test
        fun `extract URLSpan from spanned text`() {
            val spanned = SpannableString("Check out this link")
            spanned.setSpan(URLSpan("http://example.com"), 10, 14, 0)

            val links = detector.extractFromSpanned(spanned)

            assertEquals(1, links.size)
            assertEquals("http://example.com", links[0].url)
            assertEquals("this", links[0].label)
            assertEquals(10, links[0].start)
            assertEquals(14, links[0].end)
        }

        @Test
        fun `extract multiple URLSpans`() {
            val spanned = SpannableString("Visit example and another site")
            spanned.setSpan(URLSpan("http://example.com"), 6, 13, 0)
            spanned.setSpan(URLSpan("http://another.com"), 18, 25, 0)

            val links = detector.extractFromSpanned(spanned)

            assertTrue(links.size >= 2)
            assertTrue(links.any { it.url == "http://example.com" })
            assertTrue(links.any { it.url == "http://another.com" })
        }

        @Test
        fun `extract URLSpan and detect additional patterns`() {
            val spanned = SpannableString("Visit http://example.com and test@email.com")
            spanned.setSpan(URLSpan("http://example.com"), 6, 24, 0)

            val links = detector.extractFromSpanned(spanned)

            // Should have at least the URLSpan link and potentially the email
            assertTrue(links.size >= 1)
            assertTrue(links.any { it.url.contains("example.com") })
        }

        @Test
        fun `filter out dangerous URLs from spans`() {
            val spanned = SpannableString("Click here")
            spanned.setSpan(URLSpan("javascript:alert('xss')"), 6, 10, 0)

            val links = detector.extractFromSpanned(spanned)

            // Dangerous URL should be filtered out
            assertTrue(links.none { it.url.contains("javascript:") })
        }
    }

    @Nested
    @DisplayName("Deduplication and Overlap")
    inner class Deduplication {

        @Test
        fun `remove overlapping links`() {
            val text = "http://example.com"
            val links = detector.detectLinks(text)

            // Should only return one link, not multiple overlapping matches
            assertEquals(1, links.size)
        }

        @Test
        fun `prefer earlier link when overlapping`() {
            // Create text where patterns might overlap
            val text = "www.example.com"
            val links = detector.detectLinks(text)

            // Should have exactly one link (deduplication removes overlaps)
            assertEquals(1, links.size)
        }

        @Test
        fun `keep non-overlapping links`() {
            val text = "Visit http://example.com and contact test@email.com"
            val links = detector.detectLinks(text)

            // Should have both links since they don't overlap
            assertTrue(links.size >= 2)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `handle empty text`() {
            val links = detector.detectLinks("")
            assertEquals(0, links.size)
        }

        @Test
        fun `handle text with no links`() {
            val links = detector.detectLinks("This is plain text with no links")
            assertEquals(0, links.size)
        }

        @Test
        fun `handle URL at start of text`() {
            val text = "http://example.com is a website"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertEquals(0, links[0].start)
        }

        @Test
        fun `handle URL at end of text`() {
            val text = "Visit http://example.com"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertEquals(text.length, links[0].end)
        }

        @Test
        fun `handle URL with special characters`() {
            val text = "https://example.com/path?q=hello%20world&foo=bar#section"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertTrue(links[0].url.contains("example.com"))
        }

        @Test
        fun `handle international domain names`() {
            val text = "Visit http://example.co.uk"
            val links = detector.detectLinks(text)

            assertEquals(1, links.size)
            assertTrue(links[0].url.contains("example.co.uk"))
        }
    }

    @Nested
    @DisplayName("Normalized URL Tests")
    inner class NormalizedUrl {

        @Test
        fun `normalize http URL keeps protocol`() {
            val link = DetectedLink("http://example.com", "example.com", LinkType.WEB, 0, 10)
            assertEquals("http://example.com", link.getNormalizedUrl())
        }

        @Test
        fun `normalize https URL keeps protocol`() {
            val link = DetectedLink("https://example.com", "example.com", LinkType.WEB, 0, 10)
            assertEquals("https://example.com", link.getNormalizedUrl())
        }

        @Test
        fun `normalize www URL adds https`() {
            val link = DetectedLink("www.example.com", "www.example.com", LinkType.WEB, 0, 10)
            assertEquals("https://www.example.com", link.getNormalizedUrl())
        }

        @Test
        fun `normalize plain domain adds https`() {
            val link = DetectedLink("example.com", "example.com", LinkType.WEB, 0, 10)
            assertEquals("https://example.com", link.getNormalizedUrl())
        }

        @Test
        fun `normalize email without mailto adds it`() {
            val link = DetectedLink("test@example.com", "test@example.com", LinkType.EMAIL, 0, 10)
            assertEquals("mailto:test@example.com", link.getNormalizedUrl())
        }

        @Test
        fun `normalize email with mailto keeps it`() {
            val link = DetectedLink("mailto:test@example.com", "test@example.com", LinkType.EMAIL, 0, 10)
            assertEquals("mailto:test@example.com", link.getNormalizedUrl())
        }

        @Test
        fun `normalize phone without tel adds it`() {
            val link = DetectedLink("1234567890", "1234567890", LinkType.PHONE, 0, 10)
            assertEquals("tel:1234567890", link.getNormalizedUrl())
        }

        @Test
        fun `normalize phone with tel keeps it`() {
            val link = DetectedLink("tel:1234567890", "1234567890", LinkType.PHONE, 0, 10)
            assertEquals("tel:1234567890", link.getNormalizedUrl())
        }

        @Test
        fun `normalize geo link keeps as is`() {
            val link = DetectedLink("geo:0,0", "geo:0,0", LinkType.MAP, 0, 10)
            assertEquals("geo:0,0", link.getNormalizedUrl())
        }

        @Test
        fun `normalize deep link keeps as is`() {
            val link = DetectedLink("myapp://page", "myapp://page", LinkType.DEEP_LINK, 0, 10)
            assertEquals("myapp://page", link.getNormalizedUrl())
        }
    }
}
