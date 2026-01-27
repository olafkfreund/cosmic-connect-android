/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.TelephonyPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*

/**
 * FFI wrapper for telephony and SMS packet creation and inspection.
 *
 * Provides type-safe packet creation using the cosmic-connect-core FFI layer
 * and extension properties for inspecting telephony/SMS packets.
 *
 * ## Packet Types
 *
 * ### Telephony (Call Events)
 * - **Call Event** (`cconnect.telephony`): Phone call notifications (incoming)
 * - **Mute Request** (`cconnect.telephony.request_mute`): Request to mute ringer (outgoing)
 *
 * ### SMS Messaging
 * - **SMS Messages** (`cconnect.sms.messages`): SMS conversation data (incoming)
 * - **Conversations Request** (`cconnect.sms.request_conversations`): Request conversation list (outgoing)
 * - **Conversation Request** (`cconnect.sms.request_conversation`): Request thread messages (outgoing)
 * - **Attachment Request** (`cconnect.sms.request_attachment`): Request MMS attachment (outgoing)
 * - **Send SMS Request** (`cconnect.sms.request`): Send SMS message (outgoing)
 *
 * ## Call Events
 *
 * Call events can be:
 * - `ringing`: Phone is ringing (incoming call)
 * - `talking`: Call is active (in conversation)
 * - `missedCall`: Call was missed
 * - `sms`: SMS received (deprecated, use SMS plugin)
 *
 * ## Usage Examples
 *
 * ### Telephony
 * ```kotlin
 * // Send call notification to desktop
 * val callEvent = TelephonyPacketsFFI.createTelephonyEvent(
 *     event = "ringing",
 *     phoneNumber = "+1234567890",
 *     contactName = "John Doe"
 * )
 * device.sendPacket(callEvent)
 *
 * // Request mute from desktop
 * val muteRequest = TelephonyPacketsFFI.createMuteRequest()
 * device.sendPacket(muteRequest)
 * ```
 *
 * ### SMS
 * ```kotlin
 * // Request conversation list from desktop
 * val request = TelephonyPacketsFFI.createConversationsRequest()
 * device.sendPacket(request)
 *
 * // Send SMS from desktop
 * val sendRequest = TelephonyPacketsFFI.createSendSmsRequest(
 *     phoneNumber = "+1234567890",
 *     messageBody = "Hello from desktop!"
 * )
 * device.sendPacket(sendRequest)
 * ```
 *
 * ### Packet Inspection
 * ```kotlin
 * // Handle incoming packets
 * when {
 *     packet.isTelephonyEvent -> {
 *         val event = packet.telephonyEvent
 *         val number = packet.telephonyPhoneNumber
 *         val name = packet.telephonyContactName
 *         // Show notification
 *     }
 *     packet.isMuteRequest -> {
 *         // Mute ringer
 *     }
 *     packet.isConversationsRequest -> {
 *         // Send conversation list
 *     }
 * }
 * ```
 *
 * @see org.cosmic.cosmicconnect.Plugins.TelephonyPlugin.TelephonyPlugin
 */
object TelephonyPacketsFFI {
    // =========================================================================
    // Telephony (Call Events)
    // =========================================================================

    /**
     * Create a telephony event packet (call notification).
     *
     * Creates a `cconnect.telephony` packet for notifying about phone call
     * events. Sent from Android to desktop when call state changes.
     *
     * ## Validation
     * - Event must be one of: "ringing", "talking", "missedCall", "sms"
     * - Phone number and contact name are optional
     *
     * ## Example
     * ```kotlin
     * // Incoming call
     * val packet = TelephonyPacketsFFI.createTelephonyEvent(
     *     event = "ringing",
     *     phoneNumber = "+1234567890",
     *     contactName = "John Doe"
     * )
     * device.sendPacket(packet)
     *
     * // Missed call (no contact name)
     * val missedCall = TelephonyPacketsFFI.createTelephonyEvent(
     *     event = "missedCall",
     *     phoneNumber = "+1234567890",
     *     contactName = null
     * )
     * ```
     *
     * @param event Event type: "ringing", "talking", "missedCall", or "sms"
     * @param phoneNumber Caller's phone number (optional)
     * @param contactName Contact name from address book (optional)
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if event is not a valid type
     */
    fun createTelephonyEvent(
        event: String,
        phoneNumber: String? = null,
        contactName: String? = null
    ): NetworkPacket {
        require(event in listOf("ringing", "talking", "missedCall", "sms")) {
            "Event must be one of: ringing, talking, missedCall, sms"
        }

        val ffiPacket = uniffi.cosmic_connect_core.createTelephonyEvent(event, phoneNumber, contactName)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a mute ringer request packet.
     *
     * Creates a `cconnect.telephony.request_mute` packet requesting the phone
     * to mute its ringer. Sent from desktop to Android when user wants to
     * silence an incoming call.
     *
     * ## Example
     * ```kotlin
     * val packet = TelephonyPacketsFFI.createMuteRequest()
     * device.sendPacket(packet)
     * ```
     *
     * @return Immutable NetworkPacket ready to be sent
     */
    fun createMuteRequest(): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMuteRequest()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    // =========================================================================
    // SMS Messaging
    // =========================================================================

    /**
     * Create an SMS messages packet.
     *
     * Creates a `cconnect.sms.messages` packet containing SMS conversations
     * with messages. Sent from Android to desktop in response to conversation
     * requests.
     *
     * ## Validation
     * - JSON must be valid and contain "conversations" array
     *
      * ## JSON Format
      * ```json
      * {
      *   "conversations": [
      *     {
      *       "threadId": 123,
      *       "messages": [
      *         {
      *           "_id": 456,
      *           "threadId": 123,
      *           "address": "+1234567890",
      *           "body": "Hello!",
      *           "date": 1705507200000,
      *           "type": 1,
      *           "read": 1
      *         }
      *       ]
      *     }
      *   ]
      * }
      * ```
     
     *
     * ## Example
     * ```kotlin
     * val conversationsJson = buildConversationsJson(conversations)
     * val packet = TelephonyPacketsFFI.createSmsMessages(conversationsJson)
     * device.sendPacket(packet)
     * ```
     *
     * @param conversationsJson JSON string containing array of conversations with messages
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if JSON is invalid or missing required fields
     */
    fun createSmsMessages(conversationsJson: String): NetworkPacket {
        require(conversationsJson.isNotBlank()) { "Conversations JSON cannot be blank" }

        val ffiPacket = uniffi.cosmic_connect_core.createSmsMessages(conversationsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a request for SMS conversations list.
     *
     * Creates a `cconnect.sms.request_conversations` packet requesting the
     * list of SMS conversations (latest message in each thread). Sent from
     * desktop to Android to get overview of SMS threads.
     *
     * ## Example
     * ```kotlin
     * val packet = TelephonyPacketsFFI.createConversationsRequest()
     * device.sendPacket(packet)
     * ```
     *
     * @return Immutable NetworkPacket ready to be sent
     */
    fun createConversationsRequest(): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createConversationsRequest()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a request for messages in a specific conversation.
     *
     * Creates a `cconnect.sms.request_conversation` packet requesting messages
     * from a specific SMS thread. Sent from desktop to Android to view
     * conversation history.
     *
     * ## Validation
     * - Thread ID must be positive
     * - Start timestamp must be non-negative if provided
     * - Count must be positive if provided
     *
     * ## Example
     * ```kotlin
     * // Request latest 50 messages
     * val packet = TelephonyPacketsFFI.createConversationRequest(
     *     threadId = 123,
     *     startTimestamp = null,
     *     count = 50
     * )
     * device.sendPacket(packet)
     *
     * // Request messages after specific timestamp
     * val packet2 = TelephonyPacketsFFI.createConversationRequest(
     *     threadId = 123,
     *     startTimestamp = 1705507200000L,
     *     count = null
     * )
     * ```
     *
     * @param threadId The conversation thread ID
     * @param startTimestamp Optional earliest message timestamp (ms since epoch, for pagination)
     * @param count Optional maximum number of messages to return
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if threadId is not positive or parameters are invalid
     */
    fun createConversationRequest(
        threadId: Long,
        startTimestamp: Long? = null,
        count: Int? = null
    ): NetworkPacket {
        require(threadId > 0) { "Thread ID must be positive" }
        if (startTimestamp != null) {
            require(startTimestamp >= 0) { "Start timestamp cannot be negative" }
        }
        if (count != null) {
            require(count > 0) { "Count must be positive" }
        }

        val ffiPacket = uniffi.cosmic_connect_core.createConversationRequest(threadId, startTimestamp, count)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a request for a message attachment.
     *
     * Creates a `cconnect.sms.request_attachment` packet requesting a message
     * attachment (MMS image, video, etc.). Sent from desktop to Android to
     * download attachment.
     *
     * ## Validation
     * - Part ID must be positive
     * - Unique identifier cannot be blank
     *
     * ## Example
     * ```kotlin
     * val packet = TelephonyPacketsFFI.createAttachmentRequest(
     *     partId = 789,
     *     uniqueIdentifier = "abc123"
     * )
     * device.sendPacket(packet)
     * ```
     *
     * @param partId The attachment part ID from the message
     * @param uniqueIdentifier Unique file identifier for the attachment
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if partId is not positive or uniqueIdentifier is blank
     */
    fun createAttachmentRequest(
        partId: Long,
        uniqueIdentifier: String
    ): NetworkPacket {
        require(partId > 0) { "Part ID must be positive" }
        require(uniqueIdentifier.isNotBlank()) { "Unique identifier cannot be blank" }

        val ffiPacket = uniffi.cosmic_connect_core.createAttachmentRequest(partId, uniqueIdentifier)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a request to send an SMS message.
     *
     * Creates a `cconnect.sms.request` packet requesting to send an SMS from
     * the Android device. Sent from desktop to Android when user composes a message.
     *
     * ## Validation
     * - Phone number cannot be blank
     * - Message body cannot be blank
     *
     * ## Example
     * ```kotlin
     * val packet = TelephonyPacketsFFI.createSendSmsRequest(
     *     phoneNumber = "+1234567890",
     *     messageBody = "Hello from desktop!"
     * )
     * device.sendPacket(packet)
     * ```
     *
     * @param phoneNumber Recipient phone number
     * @param messageBody Message text to send
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if phoneNumber or messageBody is blank
     */
    fun createSendSmsRequest(
        phoneNumber: String,
        messageBody: String
    ): NetworkPacket {
        require(phoneNumber.isNotBlank()) { "Phone number cannot be blank" }
        require(messageBody.isNotBlank()) { "Message body cannot be blank" }

        val ffiPacket = uniffi.cosmic_connect_core.createSendSmsRequest(phoneNumber, messageBody)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// =============================================================================
// Extension Properties for Type-Safe Packet Inspection
// =============================================================================

// -------------------------------------------------------------------------
// Telephony (Call Events)
// -------------------------------------------------------------------------

/**
 * Check if packet is a telephony event (call notification).
 *
 * Returns true if the packet is a `cconnect.telephony` packet with an event field.
 *
 * ## Example
 * ```kotlin
 * if (packet.isTelephonyEvent) {
 *     val event = packet.telephonyEvent
 *     val number = packet.telephonyPhoneNumber
 *     showCallNotification(event, number)
 * }
 * ```
 *
 * @return true if packet is a telephony event, false otherwise
 */
val NetworkPacket.isTelephonyEvent: Boolean
    get() = type == "cconnect.telephony" && body.containsKey("event")

/**
 * Check if packet is a mute ringer request.
 *
 * Returns true if the packet is a `cconnect.telephony.request_mute` packet.
 *
 * ## Example
 * ```kotlin
 * if (packet.isMuteRequest) {
 *     audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT)
 * }
 * ```
 *
 * @return true if packet is a mute request, false otherwise
 */
val NetworkPacket.isMuteRequest: Boolean
    get() = type == "cconnect.telephony.request_mute"

/**
 * Extract call event type from telephony packet.
 *
 * Returns the event type ("ringing", "talking", "missedCall", "sms") from a
 * telephony event packet. Returns null if not a telephony packet or event is missing.
 *
 * ## Example
 * ```kotlin
 * when (packet.telephonyEvent) {
 *     "ringing" -> showIncomingCallNotification()
 *     "talking" -> showOngoingCallNotification()
 *     "missedCall" -> showMissedCallNotification()
 *     else -> { }
 * }
 * ```
 *
 * @return Event type string, or null if not a telephony packet
 */
val NetworkPacket.telephonyEvent: String?
    get() = if (isTelephonyEvent) body["event"] as? String else null

/**
 * Extract phone number from telephony packet.
 *
 * Returns the caller's phone number from a telephony event packet.
 * Returns null if not a telephony packet or phone number is missing.
 *
 * ## Example
 * ```kotlin
 * val number = packet.telephonyPhoneNumber
 * if (number != null) {
 *     val contact = contactsProvider.findByNumber(number)
 *     showCallNotification(number, contact?.name)
 * }
 * ```
 *
 * @return Phone number string, or null if not available
 */
val NetworkPacket.telephonyPhoneNumber: String?
    get() = if (isTelephonyEvent) body["phoneNumber"] as? String else null

/**
 * Extract contact name from telephony packet.
 *
 * Returns the contact name from address book (if available) from a telephony
 * event packet. Returns null if not a telephony packet or name is missing.
 *
 * ## Example
 * ```kotlin
 * val name = packet.telephonyContactName ?: "Unknown"
 * showCallNotification(name)
 * ```
 *
 * @return Contact name string, or null if not available
 */
val NetworkPacket.telephonyContactName: String?
    get() = if (isTelephonyEvent) body["contactName"] as? String else null

// -------------------------------------------------------------------------
// SMS Messaging
// -------------------------------------------------------------------------

/**
 * Check if packet is an SMS messages packet.
 *
 * Returns true if the packet is a `cconnect.sms.messages` packet with
 * conversations data.
 *
 * ## Example
 * ```kotlin
 * if (packet.isSmsMessages) {
 *     val conversations = packet.smsConversations
 *     updateConversationsList(conversations)
 * }
 * ```
 *
 * @return true if packet is SMS messages, false otherwise
 */
val NetworkPacket.isSmsMessages: Boolean
    get() = type == "cconnect.sms.messages" && body.containsKey("conversations")

/**
 * Check if packet is a conversations list request.
 *
 * Returns true if the packet is a `cconnect.sms.request_conversations` packet.
 *
 * ## Example
 * ```kotlin
 * if (packet.isConversationsRequest) {
 *     val conversations = smsProvider.getAllConversations()
 *     device.sendPacket(TelephonyPacketsFFI.createSmsMessages(conversations))
 * }
 * ```
 *
 * @return true if packet is a conversations request, false otherwise
 */
val NetworkPacket.isConversationsRequest: Boolean
    get() = type == "cconnect.sms.request_conversations"

/**
 * Check if packet is a conversation messages request.
 *
 * Returns true if the packet is a `cconnect.sms.request_conversation` packet
 * with a thread ID.
 *
 * ## Example
 * ```kotlin
 * if (packet.isConversationRequest) {
 *     val threadId = packet.smsRequestThreadId
 *     val count = packet.smsRequestCount ?: 50
 *     val messages = smsProvider.getMessages(threadId, count)
 *     device.sendPacket(TelephonyPacketsFFI.createSmsMessages(messages))
 * }
 * ```
 *
 * @return true if packet is a conversation request, false otherwise
 */
val NetworkPacket.isConversationRequest: Boolean
    get() = type == "cconnect.sms.request_conversation" && body.containsKey("threadId")

/**
 * Check if packet is an attachment request.
 *
 * Returns true if the packet is a `cconnect.sms.request_attachment` packet
 * with part ID and unique identifier.
 *
 * ## Example
 * ```kotlin
 * if (packet.isAttachmentRequest) {
 *     val partId = packet.smsAttachmentPartId
 *     val uniqueId = packet.smsAttachmentUniqueId
 *     val attachment = smsProvider.getAttachment(partId, uniqueId)
 *     device.sendPayload(attachment)
 * }
 * ```
 *
 * @return true if packet is an attachment request, false otherwise
 */
val NetworkPacket.isAttachmentRequest: Boolean
    get() = type == "cconnect.sms.request_attachment" &&
            body.containsKey("partId") &&
            body.containsKey("uniqueIdentifier")

/**
 * Check if packet is a send SMS request.
 *
 * Returns true if the packet is a `cconnect.sms.request` packet with phone
 * number and message body.
 *
 * ## Example
 * ```kotlin
 * if (packet.isSendSmsRequest) {
 *     val number = packet.smsRecipientNumber
 *     val message = packet.smsMessageBody
 *     if (number != null && message != null) {
 *         smsManager.sendTextMessage(number, null, message, null, null)
 *     }
 * }
 * ```
 *
 * @return true if packet is a send SMS request, false otherwise
 */
val NetworkPacket.isSendSmsRequest: Boolean
    get() = type == "cconnect.sms.request" &&
            body.containsKey("phoneNumber") &&
            body.containsKey("messageBody")

/**
 * Extract thread ID from conversation request packet.
 *
 * Returns the conversation thread ID from a `cconnect.sms.request_conversation`
 * packet. Returns null if not a conversation request or thread ID is missing.
 *
 * ## Example
 * ```kotlin
 * if (packet.isConversationRequest) {
 *     val threadId = packet.smsRequestThreadId
 *     if (threadId != null) {
 *         val messages = smsProvider.getMessagesForThread(threadId)
 *         sendMessagesToDesktop(messages)
 *     }
 * }
 * ```
 *
 * @return Thread ID as Long, or null if not available
 */
val NetworkPacket.smsRequestThreadId: Long?
    get() = if (isConversationRequest) {
        (body["threadId"] as? Number)?.toLong()
    } else null

/**
 * Extract start timestamp from conversation request packet.
 *
 * Returns the range start timestamp (for pagination) from a conversation request
 * packet. Returns null if not a conversation request or timestamp not specified.
 *
 * ## Example
 * ```kotlin
 * val threadId = packet.smsRequestThreadId ?: return
 * val startTimestamp = packet.smsRequestStartTimestamp ?: 0L
 * val messages = smsProvider.getMessagesAfter(threadId, startTimestamp)
 * ```
 *
 * @return Start timestamp in milliseconds, or null if not specified
 */
val NetworkPacket.smsRequestStartTimestamp: Long?
    get() = if (isConversationRequest) {
        (body["rangeStartTimestamp"] as? Number)?.toLong()
    } else null

/**
 * Extract message count from conversation request packet.
 *
 * Returns the maximum number of messages to return from a conversation request
 * packet. Returns null if not a conversation request or count not specified.
 *
 * ## Example
 * ```kotlin
 * val threadId = packet.smsRequestThreadId ?: return
 * val count = packet.smsRequestCount ?: 50  // Default to 50
 * val messages = smsProvider.getMessages(threadId, count)
 * ```
 *
 * @return Maximum message count as Int, or null if not specified
 */
val NetworkPacket.smsRequestCount: Int?
    get() = if (isConversationRequest) {
        (body["numberToRequest"] as? Number)?.toInt()
    } else null

/**
 * Extract attachment part ID from attachment request packet.
 *
 * Returns the attachment part ID from a `cconnect.sms.request_attachment`
 * packet. Returns null if not an attachment request or part ID is missing.
 *
 * ## Example
 * ```kotlin
 * if (packet.isAttachmentRequest) {
 *     val partId = packet.smsAttachmentPartId
 *     val uniqueId = packet.smsAttachmentUniqueId
 *     if (partId != null && uniqueId != null) {
 *         val attachment = getAttachment(partId, uniqueId)
 *         sendAttachment(attachment)
 *     }
 * }
 * ```
 *
 * @return Attachment part ID as Long, or null if not available
 */
val NetworkPacket.smsAttachmentPartId: Long?
    get() = if (isAttachmentRequest) {
        (body["partId"] as? Number)?.toLong()
    } else null

/**
 * Extract unique identifier from attachment request packet.
 *
 * Returns the unique file identifier from a `cconnect.sms.request_attachment`
 * packet. Returns null if an attachment request or identifier is missing.
 *
 * ## Example
 * ```kotlin
 * val partId = packet.smsAttachmentPartId ?: return
 * val uniqueId = packet.smsAttachmentUniqueId ?: return
 * val file = attachmentCache.get(partId, uniqueId)
 * ```
 *
 * @return Unique identifier string, or null if not available
 */
val NetworkPacket.smsAttachmentUniqueId: String?
    get() = if (isAttachmentRequest) {
        body["uniqueIdentifier"] as? String
    } else null

/**
 * Extract recipient phone number from send SMS request packet.
 *
 * Returns the recipient phone number from a `cconnect.sms.request` packet.
 * Returns null if not a send SMS request or phone number is missing.
 *
 * ## Example
 * ```kotlin
 * if (packet.isSendSmsRequest) {
 *     val number = packet.smsRecipientNumber ?: return
 *     val message = packet.smsMessageBody ?: return
 *     smsManager.sendTextMessage(number, null, message, null, null)
 * }
 * ```
 *
 * @return Recipient phone number string, or null if not available
 */
val NetworkPacket.smsRecipientNumber: String?
    get() = if (isSendSmsRequest) {
        body["phoneNumber"] as? String
    } else null

/**
 * Extract message body from send SMS request packet.
 *
 * Returns the message body text from a `cconnect.sms.request` packet.
 * Returns null if not a send SMS request or message body is missing.
 *
 * ## Example
 * ```kotlin
 * val number = packet.smsRecipientNumber ?: return
 * val message = packet.smsMessageBody ?: return
 * sendSmsMessage(number, message)
 * ```
 *
 * @return Message body text, or null if not available
 */
val NetworkPacket.smsMessageBody: String?
    get() = if (isSendSmsRequest) {
        body["messageBody"] as? String
    } else null

// =============================================================================
