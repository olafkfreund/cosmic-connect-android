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
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        id: Int = 42,
        silent: Boolean? = null,
        urgency: Int? = null,
        category: String? = null
    ): NetworkPacket {
        val body = mutableMapOf<String, Any>(
            "ticker" to ticker,
            "appName" to appName,
            "id" to id
        )
        if (silent != null) body["silent"] = silent
        if (urgency != null) body["urgency"] = urgency
        if (category != null) body["category"] = category
        return NetworkPacket(id = 1L, type = "cconnect.notification", body = body)
    }

    // ========================================================================
    // Packet validation — missing required fields
    // ========================================================================

    @Test
    fun `onPacketReceived returns true for packet missing ticker`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("appName" to "App", "id" to 1)
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `onPacketReceived returns true for packet missing appName`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("ticker" to "Hello", "id" to 1)
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
    fun `onPacketReceived does not skip non-silent notification`() {
        val packet = validNotificationPacket(silent = false)
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
        assertTrue(shadowOf(notificationManager).size() > 0)
    }

    // ========================================================================
    // Notification posting — content
    // ========================================================================

    @Test
    fun `onPacketReceived posts notification for valid packet`() {
        val packet = validNotificationPacket(appName = "Firefox", ticker = "New tab opened")
        plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(shadowOf(notificationManager).size() > 0)
    }

    @Test
    fun `onPacketReceived notification has correct title`() {
        val packet = validNotificationPacket(appName = "Firefox", ticker = "Test message")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("Firefox", notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString())
    }

    @Test
    fun `onPacketReceived notification has correct text`() {
        val packet = validNotificationPacket(appName = "App", ticker = "Important message")
        plugin.onPacketReceived(TransferPacket(packet))
        val notification = shadowOf(notificationManager).allNotifications[0]
        assertEquals("Important message", notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
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
    fun `outgoingPacketTypes contains notification request`() {
        assertArrayEquals(
            arrayOf("cconnect.notification.request"),
            plugin.outgoingPacketTypes
        )
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
