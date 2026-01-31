/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.links

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.util.Log
import androidx.core.app.NotificationCompat
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationsPacketsFFI
import javax.inject.Inject

/**
 * Handles clickable links in notifications.
 *
 * Extracts links from notification text and creates actions for:
 * - Opening in browser on Android
 * - Opening in app (deep links)
 * - Sending to desktop for opening there
 */
class NotificationLinkHandler @Inject constructor(
    private val context: Context,
    private val linkDetector: LinkDetector,
    private val deepLinkHandler: DeepLinkHandler
) {
    companion object {
        private const val TAG = "COSMIC/NotificationLinkHandler"

        // Intent action for link handling
        const val ACTION_OPEN_LINK = "org.cosmic.cosmicconnect.OPEN_LINK"
        const val ACTION_OPEN_LINK_ON_DESKTOP = "org.cosmic.cosmicconnect.OPEN_LINK_ON_DESKTOP"

        // Intent extras
        const val EXTRA_URL = "url"
        const val EXTRA_DEVICE_ID = "deviceId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }

    /**
     * Extract links from notification.
     *
     * @param notification StatusBarNotification to extract links from
     * @return List of NotificationLink objects
     */
    fun extractLinks(notification: StatusBarNotification): List<NotificationLink> {
        val links = mutableListOf<NotificationLink>()

        try {
            val extras = NotificationCompat.getExtras(notification.notification)
                ?: return emptyList()

            // Extract title and text
            val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)
            val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)
            val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)

            // Detect links in all text fields
            val allText = listOfNotNull(title, text, bigText)

            for (textField in allText) {
                val detectedLinks = linkDetector.detectLinks(textField)

                for (detected in detectedLinks) {
                    val linkType = deepLinkHandler.analyzeLinkType(detected.url)
                    val action = determineAction(linkType)

                    links.add(NotificationLink(
                        url = detected.url,
                        label = detected.label,
                        type = linkType,
                        action = action
                    ))
                }
            }

            Log.d(TAG, "Extracted ${links.size} links from notification ${notification.key}")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting links from notification", e)
        }

        return links.distinctBy { it.url }.take(3) // Limit to 3 links
    }

    /**
     * Determine action based on link type.
     */
    private fun determineAction(linkType: LinkType): LinkAction {
        return when (linkType) {
            LinkType.APP_DEEP_LINK -> LinkAction.OPEN_APP
            LinkType.WEB_URL -> LinkAction.OPEN_BROWSER
            LinkType.WEB_URL_WITH_APP_AVAILABLE -> LinkAction.OPEN_BROWSER
        }
    }

    /**
     * Handle link click.
     *
     * @param link NotificationLink to handle
     */
    fun handleLinkClick(link: NotificationLink) {
        try {
            when (link.action) {
                LinkAction.OPEN_BROWSER -> {
                    val intent = deepLinkHandler.createOpenIntent(link.url, preferApp = false)
                    context.startActivity(intent)
                }

                LinkAction.OPEN_APP -> {
                    val intent = deepLinkHandler.createOpenIntent(link.url, preferApp = true)
                    context.startActivity(intent)
                }

                LinkAction.SEND_TO_DESKTOP -> {
                    // Handled via packet - should not reach here
                    Log.w(TAG, "SEND_TO_DESKTOP action should be handled via packet")
                }
            }

            Log.d(TAG, "Opened link: ${link.url} with action: ${link.action}")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling link click", e)
        }
    }

    /**
     * Create PendingIntent for link action.
     *
     * @param deviceId Device ID for desktop actions
     * @param link NotificationLink to create action for
     * @param notificationId Notification ID for tracking
     * @return PendingIntent for link action
     */
    fun createLinkAction(
        deviceId: String,
        link: NotificationLink,
        notificationId: String
    ): PendingIntent {
        val action = if (link.action == LinkAction.SEND_TO_DESKTOP) {
            ACTION_OPEN_LINK_ON_DESKTOP
        } else {
            ACTION_OPEN_LINK
        }

        val intent = Intent(action).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_URL, link.url)
            putExtra(EXTRA_DEVICE_ID, deviceId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }

        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Use unique request code based on URL hash
        val requestCode = (notificationId + link.url).hashCode()

        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    /**
     * Send "open link" packet to desktop.
     *
     * @param device Target device
     * @param url URL to open
     * @param notificationId Notification ID for context
     */
    fun sendOpenLinkPacket(device: Device, url: String, notificationId: String) {
        try {
            // Create a notification action packet with link data
            val packet = NotificationsPacketsFFI.createNotificationActionPacket(
                notificationId = notificationId,
                action = "openLink"
            )

            // Add URL to packet body
            // Note: This will need to be added to NotificationsPacketsFFI or done via legacy packet
            val legacyPacket = packet.toLegacyPacket()
            legacyPacket.set("url", url)

            device.sendPacket(legacyPacket)

            Log.d(TAG, "Sent open link packet to device: $url")

        } catch (e: Exception) {
            Log.e(TAG, "Error sending open link packet", e)
        }
    }

    /**
     * Format links for notification body.
     *
     * Returns a string representation of links suitable for including
     * in notification text sent to desktop.
     *
     * @param links List of links
     * @return Formatted string (empty if no links)
     */
    fun formatLinksForNotification(links: List<NotificationLink>): String {
        if (links.isEmpty()) {
            return ""
        }

        return links.joinToString("\n") { link ->
            "🔗 ${link.label}: ${link.url}"
        }
    }

    /**
     * Create notification action buttons for links.
     *
     * @param links Links to create actions for
     * @param deviceId Device ID for desktop actions
     * @param notificationId Notification ID
     * @return List of action button titles (max 3)
     */
    fun createLinkActionButtons(
        links: List<NotificationLink>,
        deviceId: String,
        notificationId: String
    ): List<String> {
        return links.take(3).map { link ->
            when (link.action) {
                LinkAction.OPEN_BROWSER -> "Open ${link.label}"
                LinkAction.OPEN_APP -> {
                    val appName = deepLinkHandler.getHandlingAppName(link.url)
                    if (appName != null) {
                        "Open in $appName"
                    } else {
                        "Open ${link.label}"
                    }
                }
                LinkAction.SEND_TO_DESKTOP -> "Open ${link.label} on Desktop"
            }
        }
    }
}

/**
 * Notification link with metadata.
 */
data class NotificationLink(
    val url: String,
    val label: String,
    val type: LinkType,
    val action: LinkAction
)

/**
 * Action to take when link is clicked.
 */
enum class LinkAction {
    /** Open in browser */
    OPEN_BROWSER,

    /** Open in specific app */
    OPEN_APP,

    /** Send to desktop for opening */
    SEND_TO_DESKTOP
}
