/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.TelephonyPlugin

import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
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

@RunWith(RobolectricTestRunner::class)
class TelephonyPluginTest {

    private lateinit var plugin: TelephonyPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = TelephonyPlugin(context, mockDevice)
    }

    // ========================================================================
    // Extension properties — isTelephonyEvent
    // ========================================================================

    @Test
    fun `isTelephonyEvent true for telephony type with event`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("event" to "ringing")
        )
        assertTrue(packet.isTelephonyEvent)
    }

    @Test
    fun `isTelephonyEvent false for telephony type without event`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = emptyMap()
        )
        assertFalse(packet.isTelephonyEvent)
    }

    @Test
    fun `isTelephonyEvent false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("event" to "ringing")
        )
        assertFalse(packet.isTelephonyEvent)
    }

    // ========================================================================
    // Extension properties — isMuteRequest
    // ========================================================================

    @Test
    fun `isMuteRequest true for mute request type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony.request_mute",
            body = emptyMap()
        )
        assertTrue(packet.isMuteRequest)
    }

    @Test
    fun `isMuteRequest false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = emptyMap()
        )
        assertFalse(packet.isMuteRequest)
    }

    // ========================================================================
    // Extension properties — telephonyEvent, telephonyPhoneNumber, telephonyContactName
    // ========================================================================

    @Test
    fun `telephonyEvent returns event string`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("event" to "ringing")
        )
        assertEquals("ringing", packet.telephonyEvent)
    }

    @Test
    fun `telephonyEvent returns null for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("event" to "ringing")
        )
        assertNull(packet.telephonyEvent)
    }

    @Test
    fun `telephonyPhoneNumber returns phone number`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("event" to "ringing", "phoneNumber" to "+1234567890")
        )
        assertEquals("+1234567890", packet.telephonyPhoneNumber)
    }

    @Test
    fun `telephonyPhoneNumber returns null when missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("event" to "ringing")
        )
        assertNull(packet.telephonyPhoneNumber)
    }

    @Test
    fun `telephonyContactName returns contact name`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("event" to "ringing", "contactName" to "John Doe")
        )
        assertEquals("John Doe", packet.telephonyContactName)
    }

    @Test
    fun `telephonyContactName returns null for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("event" to "ringing", "contactName" to "John Doe")
        )
        assertNull(packet.telephonyContactName)
    }

    // ========================================================================
    // Extension properties — SMS: isSmsMessages
    // ========================================================================

    @Test
    fun `isSmsMessages true for sms messages type with conversations`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.messages",
            body = mapOf("conversations" to "[]")
        )
        assertTrue(packet.isSmsMessages)
    }

    @Test
    fun `isSmsMessages false without conversations key`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.messages",
            body = emptyMap()
        )
        assertFalse(packet.isSmsMessages)
    }

    @Test
    fun `isSmsMessages false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("conversations" to "[]")
        )
        assertFalse(packet.isSmsMessages)
    }

    // ========================================================================
    // Extension properties — SMS: isConversationsRequest
    // ========================================================================

    @Test
    fun `isConversationsRequest true for request conversations type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversations",
            body = emptyMap()
        )
        assertTrue(packet.isConversationsRequest)
    }

    @Test
    fun `isConversationsRequest false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.messages",
            body = emptyMap()
        )
        assertFalse(packet.isConversationsRequest)
    }

    // ========================================================================
    // Extension properties — SMS: isConversationRequest
    // ========================================================================

    @Test
    fun `isConversationRequest true for request conversation with threadId`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = mapOf("threadId" to 123L)
        )
        assertTrue(packet.isConversationRequest)
    }

    @Test
    fun `isConversationRequest false without threadId`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = emptyMap()
        )
        assertFalse(packet.isConversationRequest)
    }

    // ========================================================================
    // Extension properties — SMS: isAttachmentRequest
    // ========================================================================

    @Test
    fun `isAttachmentRequest true with partId and uniqueIdentifier`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_attachment",
            body = mapOf("partId" to 789L, "uniqueIdentifier" to "abc123")
        )
        assertTrue(packet.isAttachmentRequest)
    }

    @Test
    fun `isAttachmentRequest false without uniqueIdentifier`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_attachment",
            body = mapOf("partId" to 789L)
        )
        assertFalse(packet.isAttachmentRequest)
    }

    // ========================================================================
    // Extension properties — SMS: isSendSmsRequest
    // ========================================================================

    @Test
    fun `isSendSmsRequest true with phoneNumber and messageBody`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request",
            body = mapOf("phoneNumber" to "+1234567890", "messageBody" to "Hello")
        )
        assertTrue(packet.isSendSmsRequest)
    }

    @Test
    fun `isSendSmsRequest false without messageBody`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request",
            body = mapOf("phoneNumber" to "+1234567890")
        )
        assertFalse(packet.isSendSmsRequest)
    }

    // ========================================================================
    // Extension properties — SMS data extraction
    // ========================================================================

    @Test
    fun `smsRequestThreadId returns threadId for conversation request`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = mapOf("threadId" to 42L)
        )
        assertEquals(42L, packet.smsRequestThreadId)
    }

    @Test
    fun `smsRequestThreadId handles Int value via Number cast`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = mapOf("threadId" to 42)
        )
        assertEquals(42L, packet.smsRequestThreadId)
    }

    @Test
    fun `smsRequestThreadId returns null for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("threadId" to 42L)
        )
        assertNull(packet.smsRequestThreadId)
    }

    @Test
    fun `smsRequestStartTimestamp returns timestamp`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = mapOf("threadId" to 1L, "rangeStartTimestamp" to 1704067200000L)
        )
        assertEquals(1704067200000L, packet.smsRequestStartTimestamp)
    }

    @Test
    fun `smsRequestStartTimestamp returns null when missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = mapOf("threadId" to 1L)
        )
        assertNull(packet.smsRequestStartTimestamp)
    }

    @Test
    fun `smsRequestCount returns count`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_conversation",
            body = mapOf("threadId" to 1L, "numberToRequest" to 50)
        )
        assertEquals(50, packet.smsRequestCount)
    }

    @Test
    fun `smsAttachmentPartId returns partId`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_attachment",
            body = mapOf("partId" to 789L, "uniqueIdentifier" to "abc")
        )
        assertEquals(789L, packet.smsAttachmentPartId)
    }

    @Test
    fun `smsAttachmentUniqueId returns uniqueIdentifier`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request_attachment",
            body = mapOf("partId" to 789L, "uniqueIdentifier" to "abc123")
        )
        assertEquals("abc123", packet.smsAttachmentUniqueId)
    }

    @Test
    fun `smsRecipientNumber returns phoneNumber`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request",
            body = mapOf("phoneNumber" to "+9876543210", "messageBody" to "Hi")
        )
        assertEquals("+9876543210", packet.smsRecipientNumber)
    }

    @Test
    fun `smsMessageBody returns messageBody`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.sms.request",
            body = mapOf("phoneNumber" to "+1234567890", "messageBody" to "Hello from desktop")
        )
        assertEquals("Hello from desktop", packet.smsMessageBody)
    }

    @Test
    fun `smsRecipientNumber returns null for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("phoneNumber" to "+1234567890", "messageBody" to "Hi")
        )
        assertNull(packet.smsRecipientNumber)
    }

    // ========================================================================
    // onPacketReceived
    // ========================================================================

    @Test
    fun `onPacketReceived always returns true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony.request_mute",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    @Test
    fun `onPacketReceived returns true for non-mute packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.telephony",
            body = mapOf("event" to "ringing")
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains mute request`() {
        assertArrayEquals(
            arrayOf("cconnect.telephony.request_mute"),
            plugin.supportedPacketTypes
        )
    }

    @Test
    fun `outgoingPacketTypes contains telephony`() {
        assertArrayEquals(
            arrayOf("cconnect.telephony"),
            plugin.outgoingPacketTypes
        )
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }
}
