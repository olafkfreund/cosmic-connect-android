/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.json.JSONArray
import org.json.JSONObject
import uniffi.cosmic_connect_core.*

/**
 * FFI wrapper for notification packet creation and inspection.
 *
 * Provides type-safe packet creation using the cosmic-connect-core FFI layer
 * and extension properties for inspecting notification packets.
 *
 * ## Packet Types
 *
 * - **Notification** (`kdeconnect.notification`): Send or cancel notification
 * - **Request** (`kdeconnect.notification.request`): Request all or dismiss one
 * - **Action** (`kdeconnect.notification.action`): Trigger action button
 * - **Reply** (`kdeconnect.notification.reply`): Send inline reply
 *
 * ## Notification Fields
 *
 * ### Required Fields
 * - `id` (String): Unique notification ID
 * - `appName` (String): Source application name
 * - `title` (String): Notification title
 * - `text` (String): Notification body text
 * - `isClearable` (Boolean): Whether user can dismiss
 *
 * ### Optional Fields
 * - `ticker` (String): Combined title+text
 * - `time` (String): UNIX epoch timestamp (ms)
 * - `silent` (String): "true" for preexisting, "false" for new
 * - `onlyOnce` (Boolean): Whether to show only once
 * - `requestReplyId` (String): UUID for inline reply support
 * - `actions` (Array<String>): Available action button names
 * - `payloadHash` (String): MD5 hash of notification icon
 *
 * ## Usage Examples
 *
 * ### Sending a Notification
 * ```kotlin
 * val notification = NotificationInfo(
 *     id = "notif-123",
 *     appName = "Messages",
 *     title = "New Message",
 *     text = "Hello from your phone!",
 *     isClearable = true,
 *     time = System.currentTimeMillis().toString(),
 *     silent = "false"
 * )
 * val packet = NotificationsPacketsFFI.createNotificationPacket(notification)
 * device.sendPacket(packet)
 * ```
 *
 * ### Canceling a Notification
 * ```kotlin
 * val packet = NotificationsPacketsFFI.createCancelNotificationPacket("notif-123")
 * device.sendPacket(packet)
 * ```
 *
 * ### Requesting All Notifications
 * ```kotlin
 * val request = NotificationsPacketsFFI.createNotificationRequestPacket()
 * device.sendPacket(request)
 * ```
 *
 * ### Inspecting Packets
 * ```kotlin
 * if (packet.isNotification && !packet.isCancel) {
 *     val title = packet.notificationTitle
 *     val text = packet.notificationText
 *     val appName = packet.notificationAppName
 *     showNotification(title, text, appName)
 * } else if (packet.isCancel) {
 *     val id = packet.notificationId
 *     dismissNotification(id)
 * }
 * ```
 *
 * @see org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationsPlugin
 */

/**
 * Notification information data class.
 *
 * Kotlin representation of the Rust Notification struct.
 * Used to pass notification data to the FFI layer.
 *
 * ## Example
 * ```kotlin
 * val notification = NotificationInfo(
 *     id = "notif-123",
 *     appName = "Messages",
 *     title = "New Message",
 *     text = "Hello!",
 *     isClearable = true,
 *     time = "1704067200000",
 *     silent = "false",
 *     requestReplyId = "reply-uuid-123",
 *     actions = listOf("Reply", "Mark as Read")
 * )
 * ```
 */
data class NotificationInfo(
    /** Unique notification ID */
    val id: String,

    /** Source application name */
    val appName: String,

    /** Notification title */
    val title: String,

    /** Notification body text */
    val text: String,

    /** Whether user can dismiss this notification */
    val isClearable: Boolean,

    /** UNIX epoch timestamp in milliseconds (as string) */
    val time: String? = null,

    /** "true" for preexisting, "false" for newly received */
    val silent: String? = null,

    /** Combined title and text in a single string */
    val ticker: String? = null,

    /** Whether to only show once */
    val onlyOnce: Boolean? = null,

    /** UUID for inline reply support */
    val requestReplyId: String? = null,

    /** Available action button names */
    val actions: List<String>? = null,

    /** MD5 hash of notification icon */
    val payloadHash: String? = null
) {
    /**
     * Convert to JSON string for FFI layer.
     *
     * Creates a JSON representation that matches the Rust Notification struct.
     *
     * @return JSON string ready for FFI consumption
     */
    fun toJson(): String {
        val json = JSONObject().apply {
            put("id", id)
            put("appName", appName)
            put("title", title)
            put("text", text)
            put("isClearable", isClearable)

            // Optional fields
            time?.let { put("time", it) }
            silent?.let { put("silent", it) }
            ticker?.let { put("ticker", it) }
            onlyOnce?.let { put("onlyOnce", it) }
            requestReplyId?.let { put("requestReplyId", it) }
            payloadHash?.let { put("payloadHash", it) }

            // Actions array
            actions?.let { actionsList ->
                val actionsArray = JSONArray()
                actionsList.forEach { actionsArray.put(it) }
                put("actions", actionsArray)
            }
        }
        return json.toString()
    }
}

object NotificationsPacketsFFI {
    /**
     * Create a full notification packet.
     *
     * Creates a `kdeconnect.notification` packet for displaying a notification
     * on the remote device. The notification will appear in the system notification
     * area and can optionally support actions and inline replies.
     *
     * ## Validation
     * - ID must not be empty
     * - App name must not be empty
     * - At least one of title or text must be non-empty
     *
     * ## Optional Features
     * - **Inline Reply**: Set `requestReplyId` to a UUID to enable quick replies
     * - **Action Buttons**: Provide `actions` list for notification buttons
     * - **Icon Transfer**: Set `payloadHash` to enable icon download
     * - **Timestamp**: Set `time` for proper notification ordering
     *
     * ## Example
     * ```kotlin
     * // Simple notification
     * val simple = NotificationInfo(
     *     id = "msg-001",
     *     appName = "Messages",
     *     title = "Alice",
     *     text = "Hey, how are you?",
     *     isClearable = true,
     *     time = System.currentTimeMillis().toString(),
     *     silent = "false"
     * )
     * device.sendPacket(NotificationsPacketsFFI.createNotificationPacket(simple))
     *
     * // Notification with actions and reply
     * val interactive = NotificationInfo(
     *     id = "msg-002",
     *     appName = "Messages",
     *     title = "Bob",
     *     text = "Want to grab lunch?",
     *     isClearable = true,
     *     time = System.currentTimeMillis().toString(),
     *     silent = "false",
     *     requestReplyId = java.util.UUID.randomUUID().toString(),
     *     actions = listOf("Reply", "Mark as Read", "Delete")
     * )
     * device.sendPacket(NotificationsPacketsFFI.createNotificationPacket(interactive))
     * ```
     *
     * @param notification Notification information to send
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if validation fails
     */
    fun createNotificationPacket(notification: NotificationInfo): NetworkPacket {
        require(notification.id.isNotEmpty()) {
            "Notification ID must not be empty"
        }
        require(notification.appName.isNotEmpty()) {
            "Notification app name must not be empty"
        }
        require(notification.title.isNotEmpty() || notification.text.isNotEmpty()) {
            "At least one of title or text must be non-empty"
        }

        val notificationJson = notification.toJson()
        val ffiPacket = createNotificationPacket(notificationJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a cancel notification packet.
     *
     * Creates a `kdeconnect.notification` packet with `isCancel: true` to
     * inform the desktop that a notification has been dismissed on Android.
     * The desktop should remove the notification from its display.
     *
     * ## Example
     * ```kotlin
     * // User dismissed notification on Android
     * val packet = NotificationsPacketsFFI.createCancelNotificationPacket("msg-001")
     * device.sendPacket(packet)
     * ```
     *
     * @param notificationId ID of the notification to cancel
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if notification ID is empty
     */
    fun createCancelNotificationPacket(notificationId: String): NetworkPacket {
        require(notificationId.isNotEmpty()) {
            "Notification ID must not be empty"
        }

        val ffiPacket = createCancelNotificationPacket(notificationId)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a notification request packet.
     *
     * Creates a `kdeconnect.notification.request` packet requesting all
     * current notifications from the remote device. The remote should respond
     * by sending all active notifications.
     *
     * Typically sent when devices first connect to sync notification state.
     *
     * ## Example
     * ```kotlin
     * // Request all notifications on device connection
     * override fun onDeviceConnected(device: Device) {
     *     val request = NotificationsPacketsFFI.createNotificationRequestPacket()
     *     device.sendPacket(request)
     * }
     * ```
     *
     * @return Immutable NetworkPacket ready to be sent
     */
    fun createNotificationRequestPacket(): NetworkPacket {
        val ffiPacket = createNotificationRequestPacket()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a dismiss notification packet.
     *
     * Creates a `kdeconnect.notification.request` packet with `cancel` field
     * to request the remote device to dismiss a specific notification.
     * The remote should dismiss the notification and send a cancel packet back.
     *
     * This is the opposite of createCancelNotificationPacket:
     * - **Cancel**: "I dismissed a notification, you should too"
     * - **Dismiss**: "Please dismiss this notification for me"
     *
     * ## Example
     * ```kotlin
     * // Dismiss notification on remote device from desktop
     * val packet = NotificationsPacketsFFI.createDismissNotificationPacket("msg-001")
     * device.sendPacket(packet)
     * ```
     *
     * @param notificationId ID of the notification to dismiss
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if notification ID is empty
     */
    fun createDismissNotificationPacket(notificationId: String): NetworkPacket {
        require(notificationId.isNotEmpty()) {
            "Notification ID must not be empty"
        }

        val ffiPacket = createDismissNotificationPacket(notificationId)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a notification action packet.
     *
     * Creates a `kdeconnect.notification.action` packet to trigger an action
     * button on a notification displayed on the remote device.
     *
     * Action names must match one of the strings from the notification's `actions`
     * array. Common action names include "Reply", "Mark as Read", "Delete", etc.
     *
     * ## Example
     * ```kotlin
     * // Trigger "Mark as Read" action on remote notification
     * val packet = NotificationsPacketsFFI.createNotificationActionPacket(
     *     notificationKey = "msg-001",
     *     action = "Mark as Read"
     * )
     * device.sendPacket(packet)
     * ```
     *
     * @param notificationKey ID of the notification
     * @param action Name of the action button to trigger
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if key or action is empty
     */
    fun createNotificationActionPacket(
        notificationKey: String,
        action: String
    ): NetworkPacket {
        require(notificationKey.isNotEmpty()) {
            "Notification key must not be empty"
        }
        require(action.isNotEmpty()) {
            "Action name must not be empty"
        }

        val ffiPacket = createNotificationActionPacket(notificationKey, action)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a notification reply packet.
     *
     * Creates a `kdeconnect.notification.reply` packet to send an inline reply
     * to a notification on the remote device. This is typically used for messaging
     * apps that support quick replies.
     *
     * The `replyId` must match the `requestReplyId` UUID from the original
     * notification packet.
     *
     * ## Example
     * ```kotlin
     * // Reply to a message notification
     * val notification = receivedPacket // Has requestReplyId = "uuid-123"
     * val replyPacket = NotificationsPacketsFFI.createNotificationReplyPacket(
     *     replyId = notification.notificationRequestReplyId!!,
     *     message = "Thanks! See you at 1pm."
     * )
     * device.sendPacket(replyPacket)
     * ```
     *
     * @param replyId UUID from the notification's requestReplyId field
     * @param message Reply message text to send
     * @return Immutable NetworkPacket ready to be sent
     * @throws IllegalArgumentException if replyId or message is empty
     */
    fun createNotificationReplyPacket(
        replyId: String,
        message: String
    ): NetworkPacket {
        require(replyId.isNotEmpty()) {
            "Reply ID must not be empty"
        }
        require(message.isNotEmpty()) {
            "Reply message must not be empty"
        }

        val ffiPacket = createNotificationReplyPacket(replyId, message)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// =============================================================================
// Extension Properties for Type-Safe Packet Inspection
// =============================================================================

/**
 * Check if packet is a notification packet.
 *
 * Returns true if the packet is a `kdeconnect.notification` packet.
 * This includes both regular notifications and cancellation packets.
 *
 * ## Example
 * ```kotlin
 * if (packet.isNotification) {
 *     if (packet.isCancel) {
 *         dismissNotification(packet.notificationId)
 *     } else {
 *         showNotification(packet.notificationTitle, packet.notificationText)
 *     }
 * }
 * ```
 *
 * @return true if packet is a notification, false otherwise
 */
val NetworkPacket.isNotification: Boolean
    get() = type == "kdeconnect.notification"

/**
 * Check if packet is a notification request.
 *
 * Returns true if the packet is a `kdeconnect.notification.request` packet.
 * This can be either a request for all notifications or a dismiss request.
 *
 * ## Example
 * ```kotlin
 * if (packet.isNotificationRequest) {
 *     if (body.containsKey("request")) {
 *         // Send all current notifications
 *         sendAllNotifications(device)
 *     } else if (body.containsKey("cancel")) {
 *         val id = body["cancel"] as? String
 *         dismissNotification(id)
 *     }
 * }
 * ```
 *
 * @return true if packet is a notification request, false otherwise
 */
val NetworkPacket.isNotificationRequest: Boolean
    get() = type == "kdeconnect.notification.request"

/**
 * Check if packet is a notification action.
 *
 * Returns true if the packet is a `kdeconnect.notification.action` packet
 * for triggering an action button.
 *
 * ## Example
 * ```kotlin
 * if (packet.isNotificationAction) {
 *     val key = body["key"] as? String
 *     val action = body["action"] as? String
 *     triggerNotificationAction(key, action)
 * }
 * ```
 *
 * @return true if packet is a notification action, false otherwise
 */
val NetworkPacket.isNotificationAction: Boolean
    get() = type == "kdeconnect.notification.action"

/**
 * Check if packet is a notification reply.
 *
 * Returns true if the packet is a `kdeconnect.notification.reply` packet
 * for sending an inline reply.
 *
 * ## Example
 * ```kotlin
 * if (packet.isNotificationReply) {
 *     val replyId = body["requestReplyId"] as? String
 *     val message = body["message"] as? String
 *     sendReplyToApp(replyId, message)
 * }
 * ```
 *
 * @return true if packet is a notification reply, false otherwise
 */
val NetworkPacket.isNotificationReply: Boolean
    get() = type == "kdeconnect.notification.reply"

/**
 * Check if notification packet is a cancellation.
 *
 * Returns true if this is a notification packet with `isCancel: true`,
 * indicating the notification should be dismissed.
 *
 * ## Example
 * ```kotlin
 * if (packet.isNotification && packet.isCancel) {
 *     val id = packet.notificationId
 *     notificationManager.cancel(id.hashCode())
 * }
 * ```
 *
 * @return true if this is a cancel notification, false otherwise
 */
val NetworkPacket.isCancel: Boolean
    get() = isNotification && (body["isCancel"] as? Boolean == true)

/**
 * Check if notification is clearable by the user.
 *
 * Returns whether the notification can be dismissed by the user.
 * Non-clearable notifications are typically ongoing (music player, file transfer).
 *
 * ## Example
 * ```kotlin
 * val builder = NotificationCompat.Builder(context, channelId)
 *     .setContentTitle(packet.notificationTitle)
 *     .setContentText(packet.notificationText)
 *     .setOngoing(packet.isClearable != true)
 * ```
 *
 * @return true if clearable, false if ongoing, null if not available
 */
val NetworkPacket.isClearable: Boolean?
    get() = if (isNotification) {
        body["isClearable"] as? Boolean
    } else null

/**
 * Check if notification is silent (preexisting).
 *
 * Returns true if the notification's `silent` field is "true", indicating
 * it's a preexisting notification sent during initial sync, not a new one.
 *
 * ## Example
 * ```kotlin
 * if (packet.isSilent == true) {
 *     // Don't play sound or vibrate for sync'd notifications
 *     builder.setDefaults(0)
 * }
 * ```
 *
 * @return true if silent, false if new, null if not available
 */
val NetworkPacket.isSilent: Boolean?
    get() = if (isNotification) {
        (body["silent"] as? String) == "true"
    } else null

/**
 * Check if notification supports inline replies.
 *
 * Returns true if the notification has a `requestReplyId` field,
 * indicating it supports quick replies.
 *
 * ## Example
 * ```kotlin
 * if (packet.isRepliable) {
 *     val replyAction = NotificationCompat.Action.Builder(
 *         R.drawable.ic_reply,
 *         "Reply",
 *         getReplyPendingIntent(packet.notificationRequestReplyId)
 *     ).build()
 *     builder.addAction(replyAction)
 * }
 * ```
 *
 * @return true if notification supports replies, false otherwise
 */
val NetworkPacket.isRepliable: Boolean
    get() = isNotification && body.containsKey("requestReplyId")

/**
 * Check if notification has action buttons.
 *
 * Returns true if the notification has an `actions` array with at least
 * one action.
 *
 * ## Example
 * ```kotlin
 * if (packet.hasActions) {
 *     packet.notificationActions?.forEach { actionName ->
 *         val action = NotificationCompat.Action.Builder(
 *             0, actionName, getPendingIntent(packet.notificationId, actionName)
 *         ).build()
 *         builder.addAction(action)
 *     }
 * }
 * ```
 *
 * @return true if notification has actions, false otherwise
 */
val NetworkPacket.hasActions: Boolean
    get() = isNotification && (body["actions"] as? List<*>)?.isNotEmpty() == true

/**
 * Extract notification ID.
 *
 * Returns the unique notification identifier.
 * Works for all notification-related packet types.
 *
 * ## Example
 * ```kotlin
 * val id = packet.notificationId
 * if (id != null) {
 *     notificationCache[id] = packet
 * }
 * ```
 *
 * @return Notification ID, or null if not available
 */
val NetworkPacket.notificationId: String?
    get() = when {
        isNotification -> body["id"] as? String
        isNotificationAction -> body["key"] as? String
        else -> null
    }

/**
 * Extract notification app name.
 *
 * Returns the name of the app that generated the notification.
 *
 * ## Example
 * ```kotlin
 * val appName = packet.notificationAppName ?: "Unknown App"
 * iconView.setImageDrawable(getAppIcon(appName))
 * ```
 *
 * @return App name, or null if not available
 */
val NetworkPacket.notificationAppName: String?
    get() = if (isNotification && !isCancel) {
        body["appName"] as? String
    } else null

/**
 * Extract notification title.
 *
 * Returns the notification title text.
 *
 * ## Example
 * ```kotlin
 * val title = packet.notificationTitle ?: "Notification"
 * titleTextView.text = title
 * ```
 *
 * @return Title text, or null if not available
 */
val NetworkPacket.notificationTitle: String?
    get() = if (isNotification && !isCancel) {
        body["title"] as? String
    } else null

/**
 * Extract notification body text.
 *
 * Returns the notification body/content text.
 *
 * ## Example
 * ```kotlin
 * val text = packet.notificationText ?: ""
 * contentTextView.text = text
 * ```
 *
 * @return Body text, or null if not available
 */
val NetworkPacket.notificationText: String?
    get() = if (isNotification && !isCancel) {
        body["text"] as? String
    } else null

/**
 * Extract notification ticker text.
 *
 * Returns the combined title+text ticker string.
 *
 * ## Example
 * ```kotlin
 * val ticker = packet.notificationTicker
 * builder.setTicker(ticker)
 * ```
 *
 * @return Ticker text, or null if not available
 */
val NetworkPacket.notificationTicker: String?
    get() = if (isNotification && !isCancel) {
        body["ticker"] as? String
    } else null

/**
 * Extract notification timestamp.
 *
 * Returns the UNIX epoch timestamp in milliseconds when the notification
 * was created, as a String.
 *
 * ## Example
 * ```kotlin
 * val timeStr = packet.notificationTime
 * val timeLong = timeStr?.toLongOrNull() ?: System.currentTimeMillis()
 * builder.setWhen(timeLong)
 * ```
 *
 * @return Timestamp string (ms since epoch), or null if not available
 */
val NetworkPacket.notificationTime: String?
    get() = if (isNotification && !isCancel) {
        body["time"] as? String
    } else null

/**
 * Extract notification action button names.
 *
 * Returns the list of available action button names.
 *
 * ## Example
 * ```kotlin
 * val actions = packet.notificationActions ?: emptyList()
 * actions.forEach { actionName ->
 *     builder.addAction(createAction(actionName))
 * }
 * ```
 *
 * @return List of action names, or null if not available
 */
@Suppress("UNCHECKED_CAST")
val NetworkPacket.notificationActions: List<String>?
    get() = if (isNotification && !isCancel) {
        (body["actions"] as? List<String>)
    } else null

/**
 * Extract notification reply ID.
 *
 * Returns the UUID for inline reply support. If present, the notification
 * supports quick replies via createNotificationReplyPacket().
 *
 * ## Example
 * ```kotlin
 * val replyId = packet.notificationRequestReplyId
 * if (replyId != null) {
 *     val replyIntent = getReplyPendingIntent(replyId)
 *     builder.addAction(createReplyAction(replyIntent))
 * }
 * ```
 *
 * @return Reply UUID, or null if not available
 */
val NetworkPacket.notificationRequestReplyId: String?
    get() = if (isNotification && !isCancel) {
        body["requestReplyId"] as? String
    } else null

/**
 * Extract notification icon payload hash.
 *
 * Returns the MD5 hash of the notification icon, which can be used to
 * download the icon via a separate payload transfer.
 *
 * ## Example
 * ```kotlin
 * val hash = packet.notificationPayloadHash
 * if (hash != null && !iconCache.contains(hash)) {
 *     downloadNotificationIcon(device, packet.payloadSize, hash)
 * }
 * ```
 *
 * @return MD5 hash string, or null if not available
 */
val NetworkPacket.notificationPayloadHash: String?
    get() = if (isNotification && !isCancel) {
        body["payloadHash"] as? String
    } else null

// =============================================================================
// Java-Compatible Extension Functions
// =============================================================================

/**
 * Java-compatible function to check if packet is a notification.
 *
 * Equivalent to the `isNotification` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if packet is a notification, false otherwise
 */
fun getIsNotification(packet: NetworkPacket): Boolean {
    return packet.isNotification
}

/**
 * Java-compatible function to check if packet is a notification request.
 *
 * Equivalent to the `isNotificationRequest` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if packet is a notification request, false otherwise
 */
fun getIsNotificationRequest(packet: NetworkPacket): Boolean {
    return packet.isNotificationRequest
}

/**
 * Java-compatible function to check if packet is a notification action.
 *
 * Equivalent to the `isNotificationAction` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if packet is a notification action, false otherwise
 */
fun getIsNotificationAction(packet: NetworkPacket): Boolean {
    return packet.isNotificationAction
}

/**
 * Java-compatible function to check if packet is a notification reply.
 *
 * Equivalent to the `isNotificationReply` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if packet is a notification reply, false otherwise
 */
fun getIsNotificationReply(packet: NetworkPacket): Boolean {
    return packet.isNotificationReply
}

/**
 * Java-compatible function to check if notification is a cancel.
 *
 * Equivalent to the `isCancel` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if notification is a cancel, false otherwise
 */
fun getIsCancel(packet: NetworkPacket): Boolean {
    return packet.isCancel
}

/**
 * Java-compatible function to extract notification ID.
 *
 * Equivalent to the `notificationId` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Notification ID, or null if not available
 */
fun getNotificationId(packet: NetworkPacket): String? {
    return packet.notificationId
}

/**
 * Java-compatible function to extract notification app name.
 *
 * Equivalent to the `notificationAppName` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return App name, or null if not available
 */
fun getNotificationAppName(packet: NetworkPacket): String? {
    return packet.notificationAppName
}

/**
 * Java-compatible function to extract notification title.
 *
 * Equivalent to the `notificationTitle` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Title text, or null if not available
 */
fun getNotificationTitle(packet: NetworkPacket): String? {
    return packet.notificationTitle
}

/**
 * Java-compatible function to extract notification text.
 *
 * Equivalent to the `notificationText` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Body text, or null if not available
 */
fun getNotificationText(packet: NetworkPacket): String? {
    return packet.notificationText
}

/**
 * Java-compatible function to check if notification is clearable.
 *
 * Equivalent to the `isClearable` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if clearable, false if ongoing, null if not available
 */
fun getIsClearable(packet: NetworkPacket): Boolean? {
    return packet.isClearable
}

/**
 * Java-compatible function to check if notification is repliable.
 *
 * Equivalent to the `isRepliable` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if repliable, false otherwise
 */
fun getIsRepliable(packet: NetworkPacket): Boolean {
    return packet.isRepliable
}

/**
 * Java-compatible function to check if notification has actions.
 *
 * Equivalent to the `hasActions` extension property.
 *
 * @param packet NetworkPacket to check
 * @return true if has actions, false otherwise
 */
fun getHasActions(packet: NetworkPacket): Boolean {
    return packet.hasActions
}

/**
 * Java-compatible function to extract notification actions.
 *
 * Equivalent to the `notificationActions` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return List of action names, or null if not available
 */
fun getNotificationActions(packet: NetworkPacket): List<String>? {
    return packet.notificationActions
}

/**
 * Java-compatible function to extract notification reply ID.
 *
 * Equivalent to the `notificationRequestReplyId` extension property.
 *
 * @param packet NetworkPacket to extract from
 * @return Reply UUID, or null if not available
 */
fun getNotificationRequestReplyId(packet: NetworkPacket): String? {
    return packet.notificationRequestReplyId
}
