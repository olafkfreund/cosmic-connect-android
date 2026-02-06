/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import org.cosmic.cosmicconnect.Core.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationsPacketsFFITest {

    @Test
    fun `isNotification returns true for notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "appName" to "Test")
        )

        assertTrue(packet.isNotification)
    }

    @Test
    fun `isNotification returns false for non-notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = emptyMap()
        )

        assertFalse(packet.isNotification)
    }

    @Test
    fun `isNotificationRequest returns true for request packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.request",
            body = mapOf("request" to true)
        )

        assertTrue(packet.isNotificationRequest)
    }

    @Test
    fun `isNotificationRequest returns false for notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = emptyMap()
        )

        assertFalse(packet.isNotificationRequest)
    }

    @Test
    fun `isNotificationAction returns true for action packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.action",
            body = mapOf("key" to "notif-1", "action" to "Reply")
        )

        assertTrue(packet.isNotificationAction)
    }

    @Test
    fun `isNotificationAction returns false for notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = emptyMap()
        )

        assertFalse(packet.isNotificationAction)
    }

    @Test
    fun `isNotificationReply returns true for reply packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.reply",
            body = mapOf("requestReplyId" to "reply-1", "message" to "Thanks!")
        )

        assertTrue(packet.isNotificationReply)
    }

    @Test
    fun `isNotificationReply returns false for notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = emptyMap()
        )

        assertFalse(packet.isNotificationReply)
    }

    @Test
    fun `isCancel returns true when isCancel flag is true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "isCancel" to true)
        )

        assertTrue(packet.isCancel)
    }

    @Test
    fun `isCancel returns false when isCancel flag is false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "isCancel" to false)
        )

        assertFalse(packet.isCancel)
    }

    @Test
    fun `isCancel returns false when isCancel field is missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1")
        )

        assertFalse(packet.isCancel)
    }

    @Test
    fun `isCancel returns false for non-notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("isCancel" to true)
        )

        assertFalse(packet.isCancel)
    }

    @Test
    fun `isClearable extracts clearable flag from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "isClearable" to true)
        )

        assertEquals(true, packet.isClearable)
    }

    @Test
    fun `isClearable returns null for non-notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("isClearable" to true)
        )

        assertNull(packet.isClearable)
    }

    @Test
    fun `isSilent returns true when silent is true string`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "silent" to "true")
        )

        assertEquals(true, packet.isSilent)
    }

    @Test
    fun `isSilent returns false when silent is false string`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "silent" to "false")
        )

        assertEquals(false, packet.isSilent)
    }

    @Test
    fun `isSilent returns false when silent field is missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1")
        )

        assertEquals(false, packet.isSilent)
    }

    @Test
    fun `isSilent returns null for non-notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("silent" to "true")
        )

        assertNull(packet.isSilent)
    }

    @Test
    fun `isRepliable returns true when requestReplyId present`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "requestReplyId" to "reply-uuid-123")
        )

        assertTrue(packet.isRepliable)
    }

    @Test
    fun `isRepliable returns false when requestReplyId missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1")
        )

        assertFalse(packet.isRepliable)
    }

    @Test
    fun `hasActions returns true when actions array present`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "actions" to listOf("Reply", "Mark as Read"))
        )

        assertTrue(packet.hasActions)
    }

    @Test
    fun `hasActions returns false when actions array empty`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "actions" to emptyList<String>())
        )

        assertFalse(packet.hasActions)
    }

    @Test
    fun `hasActions returns false when actions field missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1")
        )

        assertFalse(packet.hasActions)
    }

    @Test
    fun `notificationId extracts id from notification packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-123")
        )

        assertEquals("notif-123", packet.notificationId)
    }

    @Test
    fun `notificationId extracts key from action packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.action",
            body = mapOf("key" to "notif-456")
        )

        assertEquals("notif-456", packet.notificationId)
    }

    @Test
    fun `notificationId returns null for unsupported packet types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("id" to "notif-789")
        )

        assertNull(packet.notificationId)
    }

    @Test
    fun `notificationAppName extracts appName from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "appName" to "Messages")
        )

        assertEquals("Messages", packet.notificationAppName)
    }

    @Test
    fun `notificationAppName returns null for cancel notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "appName" to "Messages", "isCancel" to true)
        )

        assertNull(packet.notificationAppName)
    }

    @Test
    fun `notificationTitle extracts title from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "title" to "New Message")
        )

        assertEquals("New Message", packet.notificationTitle)
    }

    @Test
    fun `notificationText extracts text from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "text" to "Hello from your phone!")
        )

        assertEquals("Hello from your phone!", packet.notificationText)
    }

    @Test
    fun `notificationTicker extracts ticker from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "ticker" to "Alice: Hey there")
        )

        assertEquals("Alice: Hey there", packet.notificationTicker)
    }

    @Test
    fun `notificationTime extracts time from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "time" to "1704067200000")
        )

        assertEquals("1704067200000", packet.notificationTime)
    }

    @Test
    fun `notificationActions extracts actions list from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "actions" to listOf("Reply", "Mark as Read", "Delete"))
        )

        val actions = packet.notificationActions
        assertNotNull(actions)
        assertEquals(3, actions?.size)
        assertEquals("Reply", actions?.get(0))
        assertEquals("Mark as Read", actions?.get(1))
        assertEquals("Delete", actions?.get(2))
    }

    @Test
    fun `notificationRequestReplyId extracts requestReplyId from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "requestReplyId" to "reply-uuid-abc123")
        )

        assertEquals("reply-uuid-abc123", packet.notificationRequestReplyId)
    }

    @Test
    fun `notificationPayloadHash extracts payloadHash from notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification",
            body = mapOf("id" to "notif-1", "payloadHash" to "abc123def456")
        )

        assertEquals("abc123def456", packet.notificationPayloadHash)
    }

    @Test
    fun `notification request packet with request field`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.request",
            body = mapOf("request" to true)
        )

        assertTrue(packet.isNotificationRequest)
        assertTrue(packet.body.containsKey("request"))
    }

    @Test
    fun `notification request packet with cancel field`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.request",
            body = mapOf("cancel" to "notif-123")
        )

        assertTrue(packet.isNotificationRequest)
        assertTrue(packet.body.containsKey("cancel"))
        assertEquals("notif-123", packet.body["cancel"])
    }

    @Test
    fun `action packet extracts action name`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.action",
            body = mapOf("key" to "notif-1", "action" to "Mark as Read")
        )

        assertTrue(packet.isNotificationAction)
        assertEquals("notif-1", packet.notificationId)
        assertEquals("Mark as Read", packet.body["action"])
    }

    @Test
    fun `reply packet extracts message`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.notification.reply",
            body = mapOf("requestReplyId" to "reply-uuid-123", "message" to "Thanks, see you soon!")
        )

        assertTrue(packet.isNotificationReply)
        assertEquals("reply-uuid-123", packet.body["requestReplyId"])
        assertEquals("Thanks, see you soon!", packet.body["message"])
    }

    @Test
    fun `NotificationInfo toJson includes required fields`() {
        val info = NotificationInfo(
            id = "notif-1",
            appName = "Messages",
            title = "Alice",
            text = "Hello!",
            isClearable = true
        )

        val json = info.toJson()
        assertTrue(json.contains("\"id\":\"notif-1\""))
        assertTrue(json.contains("\"appName\":\"Messages\""))
        assertTrue(json.contains("\"title\":\"Alice\""))
        assertTrue(json.contains("\"text\":\"Hello!\""))
        assertTrue(json.contains("\"isClearable\":true"))
    }

    @Test
    fun `NotificationInfo toJson includes optional fields when present`() {
        val info = NotificationInfo(
            id = "notif-1",
            appName = "Messages",
            title = "Alice",
            text = "Hello!",
            isClearable = true,
            time = "1704067200000",
            silent = "false",
            ticker = "Alice: Hello!",
            requestReplyId = "reply-uuid-123",
            actions = listOf("Reply", "Mark as Read")
        )

        val json = info.toJson()
        assertTrue(json.contains("\"time\":\"1704067200000\""))
        assertTrue(json.contains("\"silent\":\"false\""))
        assertTrue(json.contains("\"ticker\":\"Alice: Hello!\""))
        assertTrue(json.contains("\"requestReplyId\":\"reply-uuid-123\""))
        assertTrue(json.contains("\"actions\""))
    }

    @Test
    fun `NotificationInfo toJson includes action buttons when present`() {
        val info = NotificationInfo(
            id = "notif-1",
            appName = "Messages",
            title = "Alice",
            text = "Hello!",
            isClearable = true,
            actionButtons = listOf(
                ActionButton("action_0_reply", "Reply"),
                ActionButton("action_1_mark_read", "Mark as Read")
            )
        )

        val json = info.toJson()
        assertTrue(json.contains("\"actionButtons\""))
        assertTrue(json.contains("\"id\":\"action_0_reply\""))
        assertTrue(json.contains("\"label\":\"Reply\""))
    }

    @Test
    fun `NotificationInfo toJson includes messaging metadata when isMessagingApp true`() {
        val info = NotificationInfo(
            id = "notif-1",
            appName = "WhatsApp",
            title = "Alice",
            text = "Hello!",
            isClearable = true,
            isMessagingApp = true,
            packageName = "com.whatsapp",
            conversationId = "conv-123",
            isGroupChat = true,
            groupName = "Friends"
        )

        val json = info.toJson()
        assertTrue(json.contains("\"isMessagingApp\":true"))
        assertTrue(json.contains("\"packageName\":\"com.whatsapp\""))
        assertTrue(json.contains("\"conversationId\":\"conv-123\""))
        assertTrue(json.contains("\"isGroupChat\":true"))
        assertTrue(json.contains("\"groupName\":\"Friends\""))
    }

    @Test
    fun `NotificationInfo toJson includes inline image data when present`() {
        val info = NotificationInfo(
            id = "notif-1",
            appName = "Messages",
            title = "Alice",
            text = "Hello!",
            isClearable = true,
            appIcon = "base64encodedicon",
            imageData = "base64encodedimage",
            imageMimeType = "image/png"
        )

        val json = info.toJson()
        assertTrue(json.contains("\"appIcon\":\"base64encodedicon\""))
        assertTrue(json.contains("\"imageData\":\"base64encodedimage\""))
        assertTrue(json.contains("\"hasImage\":true"))
        val parsed = org.json.JSONObject(json)
        assertEquals("image/png", parsed.getString("imageMimeType"))
    }

    @Test
    fun `NotificationInfo toJson includes urgency and category when present`() {
        val info = NotificationInfo(
            id = "notif-1",
            appName = "Messages",
            title = "Alice",
            text = "Hello!",
            isClearable = true,
            urgency = 2,
            category = "msg"
        )

        val json = info.toJson()
        assertTrue(json.contains("\"urgency\":2"))
        assertTrue(json.contains("\"category\":\"msg\""))
    }

    @Test
    fun `ActionButton data class stores id and label`() {
        val button = ActionButton("action_0_reply", "Reply")

        assertEquals("action_0_reply", button.id)
        assertEquals("Reply", button.label)
    }
}
