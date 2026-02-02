# Notification Sending Documentation

> Version: 1.0
> Last Updated: 2026-02-02

This document explains how the COSMIC Connect Android app extracts, processes, and sends notifications to the desktop.

## Overview

The NotificationsPlugin uses Android's NotificationListenerService to capture system notifications and forwards them to paired desktop devices using the KDE Connect protocol v8.

**Flow:**

```
Android Notification
        |
        v
NotificationListenerService (NotificationReceiver)
        |
        v
NotificationsPlugin.sendNotification()
        |
        v
FFI Packet Creation (NotificationsPacketsFFI)
        |
        v
TLS-encrypted transmission to Desktop
```

## 1. Notification Extraction

The app uses `NotificationReceiver` (a NotificationListenerService) to intercept system notifications.

### Source Location
- `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/NotificationsPlugin.kt`
- `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/NotificationReceiver.kt`

### Filtering

Not all notifications are forwarded. The following are excluded:

| Flag | Description |
|------|-------------|
| `FLAG_FOREGROUND_SERVICE` | Service notifications (e.g., music player status) |
| `FLAG_ONGOING_EVENT` | Persistent notifications |
| `FLAG_LOCAL_ONLY` | Notifications marked as local-only |
| `FLAG_GROUP_SUMMARY` | Group summary notifications |

Additionally, these packages are blocked:

- `org.cosmic.cosmicconnect` (own notifications)
- `com.android.systemui` with tag `low_battery` (spams repeatedly)
- Samsung OneUI's `MediaOngoingActivity` channel (handled by MPRIS)

### Extracted Fields

```kotlin
data class NotificationInfo(
    val id: String,              // Unique notification key
    val appName: String,         // Source app name
    val title: String,           // Notification title
    val text: String,            // Notification body
    val isClearable: Boolean,    // Can user dismiss it
    val time: String?,           // UNIX timestamp (ms)
    val silent: String?,         // "true" for sync, "false" for new
    val ticker: String?,         // Combined title:text
    val requestReplyId: String?, // UUID for inline reply support
    val actions: List<String>?,  // Action button names
    val actionButtons: List<ActionButton>?, // ID + label pairs
    // Messaging app metadata
    val isMessagingApp: Boolean,
    val conversationId: String?,
    val isGroupChat: Boolean,
    // ...
)
```

## 2. Packet Format

All notifications are sent as JSON packets with type `cconnect.notification`.

### Standard Notification Packet

```json
{
  "id": 1234567890123,
  "type": "cconnect.notification",
  "body": {
    "id": "0|com.whatsapp|1234|null|10001",
    "appName": "WhatsApp",
    "title": "Alice",
    "text": "Hey, how are you?",
    "isClearable": true,
    "time": "1706889600000",
    "silent": "false",
    "ticker": "Alice: Hey, how are you?",
    "requestReplyId": "uuid-for-inline-reply",
    "actions": ["Reply", "Mark as Read"],
    "actionButtons": [
      { "id": "0|com.whatsapp|1234_action_0_reply", "label": "Reply" },
      { "id": "0|com.whatsapp|1234_action_1_mark_read", "label": "Mark as Read" }
    ]
  },
  "payloadSize": 8192,
  "payloadTransferInfo": { "port": 1739 }
}
```

### Cancel Notification Packet

Sent when a notification is dismissed on Android:

```json
{
  "id": 1234567890124,
  "type": "cconnect.notification",
  "body": {
    "id": "0|com.whatsapp|1234|null|10001",
    "isCancel": true
  }
}
```

### Packet Types Summary

| Type | Purpose |
|------|---------|
| `cconnect.notification` | Send or cancel notification |
| `cconnect.notification.request` | Request all notifications or dismiss one |
| `cconnect.notification.action` | Trigger action button |
| `cconnect.notification.reply` | Send inline reply |

## 3. Rich Content

The app supports rich notification content beyond plain text.

### BigPictureStyle Images

Large images (photos, screenshots) are extracted and transmitted as payloads.

**Source:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/BigPictureExtractor.kt`

**Limits:**
- Max dimensions: 2048x2048 pixels
- Max size: 10MB
- Formats: PNG, JPEG, WEBP

**Packet fields:**

```json
{
  "hasImage": true,
  "imageFormat": "webp",
  "imageWidth": 1080,
  "imageHeight": 720,
  "imageSize": 54321,
  "payloadHash": "a1b2c3d4e5f6..."
}
```

The image bytes are sent as a binary payload with MD5 hash verification.

### Rich Text (HTML Formatting)

Styled text from notifications is converted to Freedesktop-compatible HTML.

**Source:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/extraction/RichTextExtractor.kt`

**Supported formatting:**

| Android Span | HTML Output |
|--------------|-------------|
| `StyleSpan(BOLD)` | `<b>text</b>` |
| `StyleSpan(ITALIC)` | `<i>text</i>` |
| `UnderlineSpan` | `<u>text</u>` |
| `ForegroundColorSpan` | `<span foreground="#RRGGBB">text</span>` |
| `URLSpan` | `<a href="url">text</a>` |

**Packet field:**

```json
{
  "richText": "<b>Important:</b> Your order has shipped!"
}
```

### Embedded Links

URLs are extracted from notification text and included in the packet.

**Source:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationPlugin/links/LinkDetector.kt`

**Link types detected:**
- Web URLs (`http://`, `https://`, `www.`)
- Email addresses (`mailto:`)
- Phone numbers (`tel:`)
- Map links (`geo:`, `maps.google.com`)
- Deep links (custom app schemes)

**Packet field:**

```json
{
  "links": [
    { "url": "https://example.com/order/123", "title": "View Order" },
    { "url": "tel:+1234567890", "title": "" }
  ]
}
```

### Video Info

For media-style notifications, video metadata is extracted.

```json
{
  "hasVideo": true,
  "videoTitle": "Never Gonna Give You Up",
  "videoDuration": 213000
}
```

## 4. Security Filtering

The app blocks dangerous URL schemes to prevent malicious notifications from exploiting the desktop.

### Blocked URL Schemes

**Source:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationPlugin/links/LinkDetector.kt`

| Scheme | Reason |
|--------|--------|
| `javascript:` | Script injection |
| `file:` | Local file access |
| `data:` | Base64-encoded payloads |
| `vbscript:` | Script injection |
| `about:` | Browser internals |
| `chrome:` | Browser internals |
| `android-app:` | System control (unless explicitly allowed) |

### URL Validation

All URLs are validated before inclusion:

1. Reject if scheme is in blocked list
2. Reject if contains `<script` or embedded scripts
3. Reject if empty or exceeds 2048 characters
4. Must match a valid URL pattern for its type

```kotlin
fun isValidUrl(url: String): Boolean {
    val lowerUrl = url.lowercase()

    // Reject dangerous schemes
    if (DANGEROUS_SCHEMES.any { lowerUrl.startsWith(it) }) {
        return false
    }

    // Reject embedded scripts
    if (lowerUrl.contains("<script") ||
        lowerUrl.contains("javascript:") ||
        lowerUrl.contains("vbscript:")) {
        return false
    }

    // Validate format
    if (url.isBlank() || url.length > 2048) {
        return false
    }

    // ... pattern matching
}
```

## 5. Privacy Controls

Users can configure privacy settings per app.

**Source:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/AppDatabase.kt`

### Privacy Options

| Option | Effect |
|--------|--------|
| `BLOCK_CONTENTS` | Title, text, ticker, actions are not sent |
| `BLOCK_IMAGES` | Icons and BigPicture images are not sent |

### Per-App Settings

Each app can be:
- **Enabled/Disabled**: Control whether notifications are forwarded
- **Content blocked**: Hide notification content (privacy mode)
- **Images blocked**: Don't send icons or images

Settings are stored in a SQLite database and accessible from the Notification Filter screen.

### How Privacy Settings Are Applied

```kotlin
// Check privacy settings before building packet
val blockImages = appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES)
val blockContents = appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_CONTENTS)

// Extract rich content only if allowed
val bigPicture = if (!blockImages && !isPreexisting) {
    bigPictureExtractor.extract(statusBarNotification)
} else null

val richText = if (!blockContents && richTextExtractor.hasRichContent(notification)) {
    richTextExtractor.extractAsHtml(notification)
} else null
```

## 6. Icon Transfer

App icons are sent as payloads when a new notification is posted.

### Icon Extraction Priority

1. Large icon (notification.getLargeIcon()) - API 23+
2. Large icon bitmap (notification.largeIcon) - Legacy
3. Small icon from app resources

### Icon Processing

- Normalized to 96x96 pixels
- Compressed as PNG (90% quality)
- MD5 hash calculated for caching

### Payload Attachment

```kotlin
// Icon is attached to the legacy packet
val payload = LegacyNetworkPacket.Payload(bitmapData)
legacyPacket.payload = payload
legacyPacket["payloadHash"] = md5Hash
```

## 7. Inline Reply Support

Notifications with reply actions get a `requestReplyId` UUID.

### How Replies Work

1. **Android sends notification with `requestReplyId`**
2. **Desktop displays quick reply input**
3. **User types reply on desktop**
4. **Desktop sends `cconnect.notification.reply` packet:**

```json
{
  "type": "cconnect.notification.reply",
  "body": {
    "requestReplyId": "uuid-from-notification",
    "message": "Thanks! See you at 1pm."
  }
}
```

5. **Android receives packet and triggers the app's reply intent**

### Reply Handling Code

```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    val networkPacket = NetworkPacket.fromLegacy(np)

    when {
        networkPacket.isNotificationReply -> {
            val replyId = networkPacket.notificationRequestReplyId
            val message = networkPacket.body["message"] as? String
            if (replyId != null && message != null) {
                replyToNotification(replyId, message)
            }
        }
        // ...
    }
}
```

## Quick Reference

### FFI Wrapper Functions

| Function | Purpose |
|----------|---------|
| `NotificationsPacketsFFI.createNotificationPacket()` | Create notification packet |
| `NotificationsPacketsFFI.createCancelNotificationPacket()` | Cancel notification |
| `NotificationsPacketsFFI.createNotificationRequestPacket()` | Request all notifications |
| `NotificationsPacketsFFI.createDismissNotificationPacket()` | Dismiss on remote |
| `NotificationsPacketsFFI.createNotificationActionPacket()` | Trigger action |
| `NotificationsPacketsFFI.createNotificationReplyPacket()` | Send inline reply |

### Extension Properties

```kotlin
// Type checking
packet.isNotification          // cconnect.notification
packet.isNotificationRequest   // cconnect.notification.request
packet.isNotificationAction    // cconnect.notification.action
packet.isNotificationReply     // cconnect.notification.reply
packet.isCancel               // Cancel notification

// Field extraction
packet.notificationId          // Notification ID
packet.notificationAppName     // App name
packet.notificationTitle       // Title
packet.notificationText        // Body text
packet.notificationActions     // Action button names
packet.notificationRequestReplyId // Reply UUID

// Rich content
packet.hasImage               // Has BigPicture
packet.richText               // HTML formatted text
packet.links                  // Extracted URLs
```

## Related Files

| File | Description |
|------|-------------|
| `NotificationsPlugin.kt` | Main plugin implementation |
| `NotificationsPacketsFFI.kt` | FFI wrapper for packet creation |
| `RichNotificationPackets.kt` | Rich content packet creation |
| `BigPictureExtractor.kt` | Image extraction |
| `RichTextExtractor.kt` | HTML formatting extraction |
| `LinkDetector.kt` | URL detection and security |
| `DeepLinkHandler.kt` | Deep link handling |
| `AppDatabase.kt` | Privacy settings storage |
| `NotificationReceiver.kt` | NotificationListenerService |
