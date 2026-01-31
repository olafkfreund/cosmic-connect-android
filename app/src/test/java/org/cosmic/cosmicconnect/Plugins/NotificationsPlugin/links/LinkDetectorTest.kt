/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.links

import android.text.SpannableString
import android.text.style.URLSpan
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LinkDetectorTest {

    private lateinit var linkDetector: LinkDetector

    @Before
    fun setUp() {
        linkDetector = LinkDetector()
    }

    @Test
    fun \`detectLinks returns empty list for null text\`() {
        val links = linkDetector.detectLinks(null)
        assertTrue(links.isEmpty())
    }

    @Test
    fun \`detectLinks finds http URL in plain text\`() {
        val text = "Check out http://example.com for more info"
        val links = linkDetector.detectLinks(text)

        assertEquals(1, links.size)
        assertEquals("http://example.com", links[0].url)
    }

    @Test
    fun \`detectLinks rejects localhost URLs\`() {
        val testCases = listOf(
            "http://localhost:8080",
            "http://127.0.0.1"
        )

        for (url in testCases) {
            val text = "Visit $url"
            val links = linkDetector.detectLinks(text)
            assertTrue("Should reject $url", links.isEmpty())
        }
    }
}
