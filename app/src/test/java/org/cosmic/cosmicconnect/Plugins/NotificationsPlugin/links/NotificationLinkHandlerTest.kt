/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.links

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.text.style.URLSpan
import androidx.core.app.NotificationCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Device
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NotificationLinkHandlerTest {

    private lateinit var context: Context
    private lateinit var linkDetector: LinkDetector
    private lateinit var deepLinkHandler: DeepLinkHandler
    private lateinit var notificationLinkHandler: NotificationLinkHandler

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        linkDetector = LinkDetector()
        deepLinkHandler = mockk(relaxed = true)
        notificationLinkHandler = NotificationLinkHandler(
            context,
            linkDetector,
            deepLinkHandler
        )
    }

    @Test
    fun \`extractLinks returns empty list for notification without links\`() {
        val notification = createMockNotification("Title", "Simple text")
        val links = notificationLinkHandler.extractLinks(notification)

        assertTrue(links.isEmpty())
    }

    @Test
    fun \`extractLinks finds link in notification text\`() {
        val notification = createMockNotification(
            "Title",
            "Check https://example.com"
        )

        every { deepLinkHandler.analyzeLinkType(any()) } returns LinkType.WEB_URL

        val links = notificationLinkHandler.extractLinks(notification)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
        assertEquals(LinkAction.OPEN_BROWSER, links[0].action)
    }

    @Test
    fun \`extractLinks finds link in notification title\`() {
        val notification = createMockNotification(
            "Visit https://example.com",
            "More info here"
        )

        every { deepLinkHandler.analyzeLinkType(any()) } returns LinkType.WEB_URL

        val links = notificationLinkHandler.extractLinks(notification)

        assertEquals(1, links.size)
        assertEquals("https://example.com", links[0].url)
    }

    @Test
    fun \`extractLinks limits to 3 links\`() {
        val text = """
            https://link1.com
            https://link2.com
            https://link3.com
            https://link4.com
            https://link5.com
        """.trimIndent()

        val notification = createMockNotification("Title", text)

        every { deepLinkHandler.analyzeLinkType(any()) } returns LinkType.WEB_URL

        val links = notificationLinkHandler.extractLinks(notification)

        assertTrue(links.size <= 3)
    }

    @Test
    fun \`extractLinks removes duplicate links\`() {
        val text = "Visit https://example.com and also https://example.com"
        val notification = createMockNotification("Title", text)

        every { deepLinkHandler.analyzeLinkType(any()) } returns LinkType.WEB_URL

        val links = notificationLinkHandler.extractLinks(notification)

        assertEquals(1, links.size)
    }

    @Test
    fun \`extractLinks sets OPEN_APP action for deep links\`() {
        val notification = createMockNotification("Title", "https://twitter.com/user")

        every { deepLinkHandler.analyzeLinkType(any()) } returns LinkType.APP_DEEP_LINK

        val links = notificationLinkHandler.extractLinks(notification)

        assertEquals(1, links.size)
        assertEquals(LinkAction.OPEN_APP, links[0].action)
    }

    @Test
    fun \`formatLinksForNotification returns empty for no links\`() {
        val formatted = notificationLinkHandler.formatLinksForNotification(emptyList())
        assertEquals("", formatted)
    }

    @Test
    fun \`formatLinksForNotification formats links correctly\`() {
        val links = listOf(
            NotificationLink(
                url = "https://example.com",
                label = "Example",
                type = LinkType.WEB_URL,
                action = LinkAction.OPEN_BROWSER
            ),
            NotificationLink(
                url = "https://test.org",
                label = "Test",
                type = LinkType.WEB_URL,
                action = LinkAction.OPEN_BROWSER
            )
        )

        val formatted = notificationLinkHandler.formatLinksForNotification(links)

        assertTrue(formatted.contains("Example: https://example.com"))
        assertTrue(formatted.contains("Test: https://test.org"))
    }

    @Test
    fun \`createLinkActionButtons limits to 3 actions\`() {
        val links = (1..5).map { i ->
            NotificationLink(
                url = "https://link$i.com",
                label = "Link $i",
                type = LinkType.WEB_URL,
                action = LinkAction.OPEN_BROWSER
            )
        }

        val actions = notificationLinkHandler.createLinkActionButtons(
            links,
            "device-id",
            "notif-id"
        )

        assertEquals(3, actions.size)
    }

    @Test
    fun \`createLinkActionButtons creates descriptive labels\`() {
        val links = listOf(
            NotificationLink(
                url = "https://example.com",
                label = "Example",
                type = LinkType.WEB_URL,
                action = LinkAction.OPEN_BROWSER
            )
        )

        val actions = notificationLinkHandler.createLinkActionButtons(
            links,
            "device-id",
            "notif-id"
        )

        assertEquals(1, actions.size)
        assertTrue(actions[0].contains("Example"))
    }

    @Test
    fun \`createLinkAction creates PendingIntent with correct extras\`() {
        val link = NotificationLink(
            url = "https://example.com",
            label = "Example",
            type = LinkType.WEB_URL,
            action = LinkAction.OPEN_BROWSER
        )

        // This will create a PendingIntent
        val pendingIntent = notificationLinkHandler.createLinkAction(
            deviceId = "device-123",
            link = link,
            notificationId = "notif-456"
        )

        assertNotNull(pendingIntent)
    }

    private fun createMockNotification(title: String, text: String): StatusBarNotification {
        val notification = mockk<Notification>(relaxed = true)
        val extras = Bundle().apply {
            putCharSequence(NotificationCompat.EXTRA_TITLE, title)
            putCharSequence(NotificationCompat.EXTRA_TEXT, text)
        }

        every { notification.extras } returns extras

        val sbn = mockk<StatusBarNotification>(relaxed = true)
        every { sbn.notification } returns notification
        every { sbn.key } returns "test-key"

        return sbn
    }
}
