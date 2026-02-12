package org.cosmicext.connect.Plugins.NotificationsPlugin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Device
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationsPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
    }

    @Test
    fun `NotificationInfo toJson includes required fields`() {
        val notificationInfo = NotificationInfo(
            id = "123",
            appName = "TestApp",
            title = "Test Title",
            text = "Test Text",
            isClearable = true
        )

        val json = notificationInfo.toJson()

        assertTrue(json.contains("\"id\":\"123\""))
        assertTrue(json.contains("\"appName\":\"TestApp\""))
        assertTrue(json.contains("\"title\":\"Test Title\""))
        assertTrue(json.contains("\"text\":\"Test Text\""))
        assertTrue(json.contains("\"isClearable\":true"))
    }

    @Test
    fun `NotificationInfo toJson includes optional time and silent fields`() {
        val notificationInfo = NotificationInfo(
            id = "456",
            appName = "TestApp",
            title = "Title",
            text = "Text",
            isClearable = false,
            time = "2026-02-08T12:00:00Z",
            silent = "true"
        )

        val json = notificationInfo.toJson()

        assertTrue(json.contains("\"time\":\"2026-02-08T12:00:00Z\""))
        assertTrue(json.contains("\"silent\":\"true\""))
    }

    @Test
    fun `NotificationInfo toJson includes actions array`() {
        val notificationInfo = NotificationInfo(
            id = "789",
            appName = "TestApp",
            title = "Title",
            text = "Text",
            isClearable = true,
            actions = listOf("Reply", "Mark as Read", "Delete")
        )

        val json = notificationInfo.toJson()

        assertTrue(json.contains("\"actions\":[\"Reply\",\"Mark as Read\",\"Delete\"]"))
    }

    @Test
    fun `ActionButton data class equality`() {
        val button1 = ActionButton("reply_id", "Reply")
        val button2 = ActionButton("reply_id", "Reply")
        val button3 = ActionButton("delete_id", "Delete")

        assertEquals(button1, button2)
        assertNotEquals(button1, button3)
        assertEquals("reply_id", button1.id)
        assertEquals("Reply", button1.label)
    }

    @Test
    fun `isNotification extension returns true for cconnect notification type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "123", "title" to "Test")
        )

        assertTrue(packet.isNotification)
    }

    @Test
    fun `isNotification extension returns false for other types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.request",
            body = mapOf()
        )

        assertFalse(packet.isNotification)
    }

    @Test
    fun `isCancel extension returns true when isNotification and body has isCancel true`() {
        val cancelPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("isCancel" to true, "id" to "123")
        )

        assertTrue(cancelPacket.isNotification)
        assertTrue(cancelPacket.isCancel)
    }

    @Test
    fun `isCancel extension returns false when isCancel is false`() {
        val notCancelPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("isCancel" to false, "id" to "123")
        )

        assertTrue(notCancelPacket.isNotification)
        assertFalse(notCancelPacket.isCancel)
    }

    @Test
    fun `notificationId extension extracts id from notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notification_123")
        )

        assertEquals("notification_123", packet.notificationId)
    }

    @Test
    fun `notificationId extension extracts key from action packet`() {
        val actionPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.action",
            body = mapOf("key" to "action_key_456")
        )

        assertEquals("action_key_456", actionPacket.notificationId)
    }

    @Test
    fun `notificationTitle extension returns null for cancel packets`() {
        val cancelPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("isCancel" to true, "id" to "123", "title" to "Should be ignored")
        )

        assertNull(cancelPacket.notificationTitle)
    }

    @Test
    fun `notificationTitle extension returns title for non-cancel packets`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "123", "title" to "Test Title")
        )

        assertEquals("Test Title", packet.notificationTitle)
    }

    @Test
    fun `notificationText extension returns null for cancel packets`() {
        val cancelPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("isCancel" to true, "id" to "123", "text" to "Should be ignored")
        )

        assertNull(cancelPacket.notificationText)
    }

    @Test
    fun `notificationText extension returns text for non-cancel packets`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "123", "text" to "Test notification text")
        )

        assertEquals("Test notification text", packet.notificationText)
    }

    @Test
    fun `isClearable extension returns correct value from notification packet`() {
        val clearablePacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "123", "isClearable" to true)
        )

        val notClearablePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.notification",
            body = mapOf("id" to "456", "isClearable" to false)
        )

        assertEquals(true, clearablePacket.isClearable)
        assertEquals(false, notClearablePacket.isClearable)
    }

    @Test
    fun `isClearable extension returns null for non-notification packets`() {
        val requestPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.request",
            body = mapOf()
        )

        assertNull(requestPacket.isClearable)
    }

    @Test
    fun `isNotificationRequest extension returns true for request type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.request",
            body = mapOf()
        )

        assertTrue(packet.isNotificationRequest)
        assertFalse(packet.isNotification)
    }

    @Test
    fun `isNotificationAction extension returns true for action type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.action",
            body = mapOf("key" to "123", "action" to "dismiss")
        )

        assertTrue(packet.isNotificationAction)
        assertFalse(packet.isNotification)
    }

    @Test
    fun `isNotificationReply extension returns true for reply type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.reply",
            body = mapOf("requestReplyId" to "123", "message" to "Reply text")
        )

        assertTrue(packet.isNotificationReply)
        assertFalse(packet.isNotification)
    }
}
