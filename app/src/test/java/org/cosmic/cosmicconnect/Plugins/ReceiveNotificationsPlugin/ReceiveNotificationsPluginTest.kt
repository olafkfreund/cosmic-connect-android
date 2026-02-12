/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import io.mockk.every
import io.mockk.mockk
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ReceiveNotificationsPluginTest {

    private lateinit var plugin: ReceiveNotificationsPlugin
    private lateinit var mockDevice: Device
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = ReceiveNotificationsPlugin(context, mockDevice)

        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }

    private fun validNotificationPacket(
        ticker: String = "Hello world",
        appName: String = "TestApp",
        id: String = "notification:42",
        silent: Any? = null,
        urgency: Int? = null,
        category: String? = null,
        title: String? = null,
        text: String? = null,
        richBody: String? = null,
        imageData: String? = null,
        actionButtons: String? = null,
        isCancel: Boolean? = null
    ): NetworkPacket {
        val body = mutableMapOf<String, Any>(
            "ticker" to ticker,
            "appName" to appName,
            "id" to id
        )
        if (silent != null) body["silent"] = silent
        if (urgency != null) body["urgency"] = urgency
        if (category != null) body["category"] = category
        if (title != null) body["title"] = title
        if (text != null) body["text"] = text
        if (richBody != null) body["richBody"] = richBody
        if (imageData != null) body["imageData"] = imageData
        if (actionButtons != null) body["actionButtons"] = actionButtons
        if (isCancel != null) body["isCancel"] = isCancel
        return NetworkPacket(id = 1L, type = "cconnect.notification", body = body)
    }

    // ========================================================================
    // Packet validation — missing required fields
    // ========================================================================

    @Test
    fun `onPacketReceived returns true for packet missing appName`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("ticker" to "Hello", "id" to "1")
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived returns true for packet missing id`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("ticker" to "Hello", "appName" to "App")
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived returns true for empty body`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = emptyMap()
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived returns true for packet missing both ticker and text`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("appName" to "App", "id" to "1")
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived posts notification when text present but no ticker`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("appName" to "App", "id" to "1", "text" to "Content")
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertTrue(shadowOf(notificationManager).size() > 0)
    }

    // ========================================================================
    // Silent filtering
    // ========================================================================

    @Test
    fun `onPacketReceived skips silent notification`() {
        val packet = validNotificationPacket(silent = true)
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived skips silent notification with string true`() {
        val packet = validNotificationPacket(silent = "true")
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived does not skip when silent string is false`() {
        val packet = validNotificationPacket(silent = "false")
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertTrue(shadowOf(notificationManager).size() > 0)
    }

    @Test
    fun `onPacketReceived does not skip non-silent notification`() {
        val packet = validNotificationPacket(silent = false)
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertTrue(shadowOf(notificationManager).size() > 0)
    }

    // ========================================================================
    // Notification content — title and text
    // ========================================================================

    @Test
    fun `onPacketReceived posts notification for valid packet`() {
        val packet = validNotificationPacket(appName = "Firefox", ticker = "New tab opened")
        plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(shadowOf(notificationManager).size() > 0)
    }

    @Test
    fun `onPacketReceived uses appName as title when no title field`() {
        val packet = validNotificationPacket(appName = "Firefox", ticker = "Test message")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("Firefox", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    }

    @Test
    fun `onPacketReceived uses title field when present`() {
        val packet = validNotificationPacket(appName = "Firefox", title = "New Email", ticker = "You have a new email")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("New Email", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    }

    @Test
    fun `onPacketReceived uses ticker as text when no text field`() {
        val packet = validNotificationPacket(appName = "App", ticker = "Important message")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("Important message", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun `onPacketReceived uses text field when present`() {
        val packet = validNotificationPacket(appName = "App", ticker = "Ticker text", text = "Full body text")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("Full body text", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun `onPacketReceived sets appName as subtext`() {
        val packet = validNotificationPacket(appName = "Thunderbird", title = "New Mail", ticker = "subject")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("Thunderbird", notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString())
    }

    // ========================================================================
    // Notification ID — uses string hashCode
    // ========================================================================

    @Test
    fun `different notification IDs produce different Android notification IDs`() {
        val packet1 = validNotificationPacket(id = "notification:1", ticker = "First")
        plugin.onPacketReceived(TransferPacket(packet1))

        val packet2 = validNotificationPacket(id = "notification:2", ticker = "Second")
        plugin.onPacketReceived(TransferPacket(packet2))

        assertEquals(2, shadowOf(notificationManager).size())
    }

    @Test
    fun `same notification ID replaces existing notification`() {
        val packet1 = validNotificationPacket(id = "notification:42", ticker = "First")
        plugin.onPacketReceived(TransferPacket(packet1))

        val packet2 = validNotificationPacket(id = "notification:42", ticker = "Updated")
        plugin.onPacketReceived(TransferPacket(packet2))

        assertEquals(1, shadowOf(notificationManager).size())
    }

    // ========================================================================
    // Cancellation packets
    // ========================================================================

    @Test
    fun `onPacketReceived handles cancel packet`() {
        // First post a notification
        val packet = validNotificationPacket(id = "notification:99", ticker = "To be cancelled")
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(1, shadowOf(notificationManager).size())

        // Then cancel it
        val cancelPacket = validNotificationPacket(id = "notification:99", isCancel = true)
        plugin.onPacketReceived(TransferPacket(cancelPacket))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    // ========================================================================
    // Urgency mapping
    // ========================================================================

    @Test
    fun `onPacketReceived maps urgency 0 to low priority`() {
        val packet = validNotificationPacket(urgency = 0)
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals(-1, notification.priority) // PRIORITY_LOW
    }

    @Test
    fun `onPacketReceived maps urgency 1 to default priority`() {
        val packet = validNotificationPacket(urgency = 1)
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals(0, notification.priority) // PRIORITY_DEFAULT
    }

    @Test
    fun `onPacketReceived maps urgency 2 to high priority`() {
        val packet = validNotificationPacket(urgency = 2)
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals(1, notification.priority) // PRIORITY_HIGH
    }

    @Test
    fun `onPacketReceived defaults urgency to normal priority`() {
        val packet = validNotificationPacket() // no urgency
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals(0, notification.priority) // PRIORITY_DEFAULT
    }

    // ========================================================================
    // Category mapping
    // ========================================================================

    @Test
    fun `onPacketReceived maps email category`() {
        val packet = validNotificationPacket(category = "email")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("email", notification.category)
    }

    @Test
    fun `onPacketReceived maps call category`() {
        val packet = validNotificationPacket(category = "call")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("call", notification.category)
    }

    @Test
    fun `onPacketReceived maps msg category to message`() {
        val packet = validNotificationPacket(category = "msg")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("msg", notification.category)
    }

    @Test
    fun `onPacketReceived maps message category`() {
        val packet = validNotificationPacket(category = "message")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("msg", notification.category)
    }

    @Test
    fun `onPacketReceived maps im category to message`() {
        val packet = validNotificationPacket(category = "im")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("msg", notification.category)
    }

    @Test
    fun `onPacketReceived maps im_received category to message`() {
        val packet = validNotificationPacket(category = "im.received")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("msg", notification.category)
    }

    @Test
    fun `onPacketReceived maps alarm category`() {
        val packet = validNotificationPacket(category = "alarm")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("alarm", notification.category)
    }

    @Test
    fun `onPacketReceived maps reminder category`() {
        val packet = validNotificationPacket(category = "reminder")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("reminder", notification.category)
    }

    @Test
    fun `onPacketReceived maps device category to system`() {
        val packet = validNotificationPacket(category = "device")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("sys", notification.category)
    }

    @Test
    fun `onPacketReceived maps network category to status`() {
        val packet = validNotificationPacket(category = "network")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("status", notification.category)
    }

    @Test
    fun `onPacketReceived uses null for unknown category`() {
        val packet = validNotificationPacket(category = "unknown_category")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertNull(notification.category)
    }

    @Test
    fun `onPacketReceived uses null for missing category`() {
        val packet = validNotificationPacket() // no category
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertNull(notification.category)
    }

    // ========================================================================
    // Action buttons
    // ========================================================================

    @Test
    fun `onPacketReceived adds action buttons from desktop`() {
        val actions = JSONArray().apply {
            put(JSONObject().put("id", "reply").put("label", "Reply"))
            put(JSONObject().put("id", "dismiss").put("label", "Dismiss"))
        }
        val packet = validNotificationPacket(actionButtons = actions.toString())
        plugin.onPacketReceived(TransferPacket(packet))

        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals(2, notification.actions?.size ?: 0)
        assertEquals("Reply", notification.actions[0].title.toString())
        assertEquals("Dismiss", notification.actions[1].title.toString())
    }

    @Test
    fun `onPacketReceived limits to 3 action buttons`() {
        val actions = JSONArray().apply {
            put(JSONObject().put("id", "a1").put("label", "Action 1"))
            put(JSONObject().put("id", "a2").put("label", "Action 2"))
            put(JSONObject().put("id", "a3").put("label", "Action 3"))
            put(JSONObject().put("id", "a4").put("label", "Action 4"))
        }
        val packet = validNotificationPacket(actionButtons = actions.toString())
        plugin.onPacketReceived(TransferPacket(packet))

        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals(3, notification.actions?.size ?: 0)
    }

    @Test
    fun `onPacketReceived handles no action buttons gracefully`() {
        val packet = validNotificationPacket()
        plugin.onPacketReceived(TransferPacket(packet))

        val notification = shadowOf(notificationManager).allNotifications[0]
        assertNull(notification.actions)
    }

    // ========================================================================
    // Rich body HTML
    // ========================================================================

    @Test
    fun `onPacketReceived renders richBody HTML in big text style`() {
        val packet = validNotificationPacket(
            richBody = "<b>Bold text</b> and <i>italic</i>",
            ticker = "Bold text and italic"
        )
        plugin.onPacketReceived(TransferPacket(packet))

        val notification = shadowOf(notificationManager).allNotifications[0]
        val bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        assertNotNull(bigText)
        // HTML-rendered text should contain the text content (without tags)
        assertTrue(bigText!!.toString().contains("Bold text"))
    }

    @Test
    fun `onPacketReceived uses contentText when no richBody`() {
        val packet = validNotificationPacket(ticker = "Plain text")
        plugin.onPacketReceived(TransferPacket(packet))

        val notification = shadowOf(notificationManager).allNotifications[0]
        val bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
        assertEquals("Plain text", bigText?.toString())
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains notification`() {
        assertArrayEquals(
            arrayOf("cconnect.notification"),
            plugin.supportedPacketTypes
        )
    }

    @Test
    fun `outgoingPacketTypes contains notification request and action types`() {
        val expected = arrayOf(
            "cconnect.notification.request",
            "cconnect.notification",
            "cconnect.notification.action"
        )
        assertArrayEquals(expected, plugin.outgoingPacketTypes)
    }

    @Test
    fun `isEnabledByDefault is false`() {
        assertFalse(plugin.isEnabledByDefault)
    }

    @Test
    fun `onPacketReceived always returns true`() {
        val valid = validNotificationPacket()
        assertTrue(plugin.onPacketReceived(TransferPacket(valid)))

        notificationManager.cancelAll()
        val invalid = NetworkPacket(id = 2L, type = "cconnect.notification", body = emptyMap())
        assertTrue(plugin.onPacketReceived(TransferPacket(invalid)))
    }
}
