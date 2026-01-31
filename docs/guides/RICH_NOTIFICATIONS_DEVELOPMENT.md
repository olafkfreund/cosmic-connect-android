# Rich Notifications Development Guide

**Developer documentation for implementing and extending Rich Notifications in COSMIC Connect.**

**Version:** 1.0.0
**Last Updated:** January 31, 2026
**Audience:** Developers contributing to COSMIC Connect

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Component Breakdown](#component-breakdown)
- [Adding Support for New Content Types](#adding-support-for-new-content-types)
- [Implementing on Android](#implementing-on-android)
- [Implementing on Desktop](#implementing-on-desktop)
- [Testing Strategies](#testing-strategies)
- [Performance Considerations](#performance-considerations)
- [Security Guidelines](#security-guidelines)
- [Code Examples](#code-examples)

---

## Architecture Overview

### High-Level Architecture

```
+------------------------------------------------------------------+
|                        Android Layer                              |
+------------------------------------------------------------------+
|  NotificationsPlugin.kt    | Main plugin coordinating all logic   |
|  NotificationReceiver.kt   | Android notification listener service|
|  NotificationsPacketsFFI.kt| FFI wrapper for packet creation      |
|  MessagingNotificationHandler.kt | Messaging app detection       |
|  AppDatabase.kt            | Privacy settings storage            |
+------------------------------------------------------------------+
                              |
                              | FFI (uniffi)
                              v
+------------------------------------------------------------------+
|                     Rust Core Layer                               |
+------------------------------------------------------------------+
|  notification.rs           | Packet creation/parsing logic        |
|  ffi/mod.rs               | FFI function exports                  |
|  cosmic_connect_core.udl  | UniFFI interface definitions         |
+------------------------------------------------------------------+
                              |
                              | Network (TLS/TCP)
                              v
+------------------------------------------------------------------+
|                      Desktop Layer                                |
+------------------------------------------------------------------+
|  notification_handler.rs   | Incoming notification processing     |
|  freedesktop_notify.rs     | D-Bus notification display           |
|  icon_cache.rs            | Icon storage and retrieval            |
+------------------------------------------------------------------+
```

### Data Flow Diagram

```
Android Notification Posted
         |
         v
+--------------------+
| NotificationReceiver|  (Android Service)
| onNotificationPosted|
+----------+---------+
           |
           v
+--------------------+
| NotificationsPlugin |  (Extract, filter, build info)
| sendNotification()  |
+----------+---------+
           |
           v
+--------------------+
| NotificationsPacketsFFI|  (Create packet via Rust)
| createNotificationPacket|
+----------+---------+
           |
           v
+--------------------+
| Rust: notification.rs|  (Build protocol packet)
| create_notification_packet|
+----------+---------+
           |
           v
+--------------------+
| NetworkPacket       |  (Serialize to JSON + payload)
| serialize()         |
+----------+---------+
           |
           v
+--------------------+
| TLS Connection      |  (Encrypted transfer)
+----------+---------+
           |
           v
+--------------------+
| Desktop Daemon      |  (Receive, parse, display)
+--------------------+
```

---

## Component Breakdown

### Android Components

#### NotificationsPlugin.kt

**Location:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/NotificationsPlugin.kt`

**Responsibilities:**
- Registers as a notification listener
- Extracts notification content (title, text, actions, icon)
- Applies privacy filters
- Coordinates packet creation and sending
- Handles incoming action/reply requests

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `sendNotification()` | Main entry point for forwarding notifications |
| `buildNotificationInfo()` | Extracts all fields from StatusBarNotification |
| `extractIcon()` | Gets notification icon as Bitmap |
| `extractActions()` | Gets available action buttons |
| `extractRepliableNotification()` | Checks for inline reply support |
| `onPacketReceived()` | Handles incoming action/reply packets |
| `replyToNotification()` | Sends inline reply to app |
| `triggerNotificationAction()` | Triggers action button |

#### NotificationsPacketsFFI.kt

**Location:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/NotificationsPacketsFFI.kt`

**Responsibilities:**
- Wraps Rust FFI functions in Kotlin-friendly API
- Provides type-safe packet creation
- Includes extension properties for packet inspection

**Key Functions:**

| Function | Purpose |
|----------|---------|
| `createNotificationPacket()` | Creates full notification packet |
| `createCancelNotificationPacket()` | Creates cancellation packet |
| `createNotificationRequestPacket()` | Requests all notifications |
| `createDismissNotificationPacket()` | Requests dismissal on remote |
| `createNotificationActionPacket()` | Triggers remote action |
| `createNotificationReplyPacket()` | Sends inline reply |

**Extension Properties:**

| Property | Purpose |
|----------|---------|
| `isNotification` | Check if packet is notification type |
| `isCancel` | Check if notification is cancellation |
| `notificationTitle` | Extract title field |
| `notificationText` | Extract text field |
| `notificationActions` | Extract actions array |
| `notificationRequestReplyId` | Extract reply UUID |

#### MessagingNotificationHandler.kt

**Location:** `app/src/org/cosmic/cosmicconnect/messaging/MessagingNotificationHandler.kt`

**Responsibilities:**
- Detects if notification is from a known messaging app
- Extracts messaging-specific metadata
- Provides web URLs for desktop link opening

#### AppDatabase.kt

**Location:** `app/src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/AppDatabase.kt`

**Responsibilities:**
- Stores per-app privacy settings
- Provides methods to check if app is enabled/disabled
- Manages content and image blocking preferences

### Rust Core Components

#### notification.rs

**Location:** `cosmic-connect-core/src/plugins/notification.rs`

**Responsibilities:**
- Defines notification data structures
- Implements packet creation logic
- Provides parsing for incoming packets

**Key Structures:**

```rust
pub struct Notification {
    pub id: String,
    pub app_name: String,
    pub title: String,
    pub text: String,
    pub ticker: Option<String>,
    pub is_clearable: bool,
    pub time: Option<String>,
    pub silent: Option<String>,
    pub only_once: Option<bool>,
    pub request_reply_id: Option<String>,
    pub actions: Option<Vec<String>>,
    pub payload_hash: Option<String>,
    // Messaging metadata
    pub is_messaging_app: Option<bool>,
    pub package_name: Option<String>,
    pub web_url: Option<String>,
    pub conversation_id: Option<String>,
    pub is_group_chat: Option<bool>,
    pub group_name: Option<String>,
    pub has_reply_action: Option<bool>,
}
```

**Key Functions:**

```rust
pub fn create_notification_packet(notification: &Notification) -> Packet;
pub fn create_cancel_packet(notification_id: &str) -> Packet;
pub fn create_request_packet() -> Packet;
pub fn create_dismiss_packet(notification_id: &str) -> Packet;
pub fn create_action_packet(key: &str, action: &str) -> Packet;
pub fn create_reply_packet(reply_id: &str, message: &str) -> Packet;
```

---

## Adding Support for New Content Types

### Example: Adding Video Thumbnail Support

This example shows how to add a new content type (video thumbnails) to Rich Notifications.

#### Step 1: Update Protocol Definition

Add new fields to the notification packet specification:

```json
{
  "body": {
    // ... existing fields ...
    "hasVideoThumbnail": true,
    "videoThumbnailHash": "abc123..."
  },
  "payloadSize": 51200,
  "payloadTransferInfo": {
    "port": 1744
  }
}
```

#### Step 2: Update Rust Notification Struct

**File:** `cosmic-connect-core/src/plugins/notification.rs`

```rust
pub struct Notification {
    // ... existing fields ...

    /// Whether notification includes a video thumbnail
    pub has_video_thumbnail: Option<bool>,

    /// MD5 hash of video thumbnail payload
    pub video_thumbnail_hash: Option<String>,
}
```

#### Step 3: Update Packet Creation

**File:** `cosmic-connect-core/src/plugins/notification.rs`

```rust
pub fn create_notification_packet(notification: &Notification) -> Packet {
    let mut body = json!({
        "id": notification.id,
        "appName": notification.app_name,
        // ... existing fields ...
    });

    // Add video thumbnail fields if present
    if let Some(has_video) = notification.has_video_thumbnail {
        body["hasVideoThumbnail"] = json!(has_video);
    }
    if let Some(hash) = &notification.video_thumbnail_hash {
        body["videoThumbnailHash"] = json!(hash);
    }

    Packet::new(PACKET_TYPE_NOTIFICATION, body)
}
```

#### Step 4: Update Android NotificationInfo

**File:** `NotificationsPacketsFFI.kt`

```kotlin
data class NotificationInfo(
    // ... existing fields ...

    /** Whether notification includes a video thumbnail */
    val hasVideoThumbnail: Boolean = false,

    /** MD5 hash of video thumbnail payload */
    val videoThumbnailHash: String? = null
) {
    fun toJson(): String {
        val json = JSONObject().apply {
            // ... existing fields ...

            if (hasVideoThumbnail) {
                put("hasVideoThumbnail", true)
                videoThumbnailHash?.let { put("videoThumbnailHash", it) }
            }
        }
        return json.toString()
    }
}
```

#### Step 5: Extract Video Thumbnail in Plugin

**File:** `NotificationsPlugin.kt`

```kotlin
private fun extractVideoThumbnail(notification: Notification): Bitmap? {
    // Check if notification has video content
    val extras = getExtras(notification)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Use MessagingStyle for media messages
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)

        style?.messages?.lastOrNull()?.let { message ->
            // Check for media URI and extract thumbnail
            // ...
        }
    }

    return null // Return bitmap if found
}
```

#### Step 6: Add Extension Property

**File:** `NotificationsPacketsFFI.kt`

```kotlin
/**
 * Check if notification has a video thumbnail.
 */
val NetworkPacket.hasVideoThumbnail: Boolean
    get() = isNotification && (body["hasVideoThumbnail"] as? Boolean == true)

/**
 * Extract video thumbnail hash.
 */
val NetworkPacket.videoThumbnailHash: String?
    get() = if (hasVideoThumbnail) {
        body["videoThumbnailHash"] as? String
    } else null
```

#### Step 7: Update Desktop Handler

Handle the new field when displaying notifications on desktop.

#### Step 8: Add Tests

Write unit tests for the new functionality (see Testing section).

---

## Implementing on Android

### Notification Listener Service

The notification listener is implemented as an Android service that receives callbacks when notifications are posted or removed.

**Key Implementation Points:**

1. **Service Declaration** (AndroidManifest.xml):
```xml
<service
    android:name=".Plugins.NotificationsPlugin.NotificationReceiver"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

2. **Listener Interface**:
```kotlin
interface NotificationListener {
    fun onListenerConnected(service: NotificationReceiver)
    fun onNotificationPosted(statusBarNotification: StatusBarNotification)
    fun onNotificationRemoved(statusBarNotification: StatusBarNotification?)
}
```

3. **Filtering Notifications**:
```kotlin
// Filter out system and local notifications
if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0 ||
    (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
    (notification.flags and Notification.FLAG_LOCAL_ONLY) != 0 ||
    (notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY) != 0) {
    return // Skip these notifications
}
```

### Icon Extraction

```kotlin
private fun extractIcon(sbn: StatusBarNotification, notification: Notification): Bitmap? {
    val foreignContext = context.createPackageContext(sbn.packageName, 0)

    // Try large icon first (contact photo, etc.)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notification.getLargeIcon()?.let { icon ->
            return iconToBitmap(foreignContext, icon)
        }
    }

    // Fall back to small icon (app icon)
    val pm = context.packageManager
    val foreignResources = pm.getResourcesForApplication(sbn.packageName)
    val foreignIcon = ResourcesCompat.getDrawable(
        foreignResources,
        notification.icon,
        null
    )
    return drawableToBitmap(foreignIcon)
}
```

### Inline Reply Implementation

```kotlin
private fun replyToNotification(id: String, message: String) {
    val repliable = pendingIntents[id] ?: return

    val remoteInputs = repliable.remoteInputs.toTypedArray()
    val localIntent = Intent().apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val localBundle = Bundle()

    for (remoteInput in remoteInputs) {
        localBundle.putCharSequence(remoteInput.resultKey, message)
    }
    RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle)

    repliable.pendingIntent.send(context, 0, localIntent)
    pendingIntents.remove(id)
}
```

---

## Implementing on Desktop

### Freedesktop Notification Display

The desktop daemon should use the freedesktop.org notification specification for displaying notifications.

**D-Bus Interface:**

```rust
// Using zbus for D-Bus communication
use zbus::{Connection, dbus_proxy};

#[dbus_proxy(
    interface = "org.freedesktop.Notifications",
    default_service = "org.freedesktop.Notifications",
    default_path = "/org/freedesktop/Notifications"
)]
trait Notifications {
    fn notify(
        &self,
        app_name: &str,
        replaces_id: u32,
        app_icon: &str,
        summary: &str,
        body: &str,
        actions: Vec<&str>,
        hints: HashMap<&str, Value>,
        timeout: i32,
    ) -> Result<u32>;

    fn close_notification(&self, id: u32) -> Result<()>;
}
```

**Displaying a Notification:**

```rust
pub async fn show_notification(
    connection: &Connection,
    notification: &Notification,
    icon_path: Option<&Path>,
) -> Result<u32> {
    let proxy = NotificationsProxy::new(connection).await?;

    // Build actions array
    let actions: Vec<&str> = notification.actions
        .as_ref()
        .map(|a| a.iter().flat_map(|s| vec![s.as_str(), s.as_str()]).collect())
        .unwrap_or_default();

    // Build hints
    let mut hints = HashMap::new();
    if let Some(time) = &notification.time {
        hints.insert("x-cosmic-time", Value::new(time.as_str()));
    }
    if let Some(reply_id) = &notification.request_reply_id {
        hints.insert("x-cosmic-reply-id", Value::new(reply_id.as_str()));
    }

    let id = proxy.notify(
        "COSMIC Connect",
        0, // replaces_id
        icon_path.map(|p| p.to_str().unwrap()).unwrap_or(""),
        &notification.title,
        &notification.text,
        actions,
        hints,
        5000, // timeout in ms
    ).await?;

    Ok(id)
}
```

### Icon Caching

```rust
use std::path::PathBuf;
use std::fs;

pub struct IconCache {
    cache_dir: PathBuf,
}

impl IconCache {
    pub fn new() -> Self {
        let cache_dir = dirs::cache_dir()
            .unwrap()
            .join("cosmic-connect")
            .join("icons");
        fs::create_dir_all(&cache_dir).ok();
        Self { cache_dir }
    }

    pub fn get_path(&self, hash: &str) -> PathBuf {
        self.cache_dir.join(format!("{}.png", hash))
    }

    pub fn has_icon(&self, hash: &str) -> bool {
        self.get_path(hash).exists()
    }

    pub fn store_icon(&self, hash: &str, data: &[u8]) -> Result<PathBuf> {
        let path = self.get_path(hash);
        fs::write(&path, data)?;
        Ok(path)
    }
}
```

---

## Testing Strategies

### Unit Tests (Rust)

**File:** `cosmic-connect-core/src/plugins/notification_tests.rs`

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_notification_packet() {
        let notification = Notification {
            id: "test-123".to_string(),
            app_name: "Test App".to_string(),
            title: "Test Title".to_string(),
            text: "Test body text".to_string(),
            is_clearable: true,
            time: Some("1706745600000".to_string()),
            silent: Some("false".to_string()),
            ..Default::default()
        };

        let packet = create_notification_packet(&notification);

        assert_eq!(packet.packet_type, PACKET_TYPE_NOTIFICATION);
        assert_eq!(packet.body["id"], "test-123");
        assert_eq!(packet.body["appName"], "Test App");
        assert_eq!(packet.body["title"], "Test Title");
        assert_eq!(packet.body["text"], "Test body text");
        assert_eq!(packet.body["isClearable"], true);
    }

    #[test]
    fn test_create_cancel_packet() {
        let packet = create_cancel_packet("test-123");

        assert_eq!(packet.packet_type, PACKET_TYPE_NOTIFICATION);
        assert_eq!(packet.body["id"], "test-123");
        assert_eq!(packet.body["isCancel"], true);
    }

    #[test]
    fn test_create_reply_packet() {
        let packet = create_reply_packet(
            "uuid-123",
            "Hello, world!"
        );

        assert_eq!(packet.packet_type, PACKET_TYPE_NOTIFICATION_REPLY);
        assert_eq!(packet.body["requestReplyId"], "uuid-123");
        assert_eq!(packet.body["message"], "Hello, world!");
    }
}
```

### Unit Tests (Kotlin)

**File:** `app/src/test/java/org/cosmic/cosmicconnect/Plugins/NotificationsPluginTest.kt`

```kotlin
class NotificationsPacketsFFITest {

    @Test
    fun `createNotificationPacket creates valid packet`() {
        val notification = NotificationInfo(
            id = "test-123",
            appName = "Test App",
            title = "Test Title",
            text = "Test body",
            isClearable = true,
            time = "1706745600000",
            silent = "false"
        )

        val packet = NotificationsPacketsFFI.createNotificationPacket(notification)

        assertEquals("cconnect.notification", packet.type)
        assertEquals("test-123", packet.body["id"])
        assertEquals("Test App", packet.body["appName"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createNotificationPacket throws for empty id`() {
        val notification = NotificationInfo(
            id = "",
            appName = "Test",
            title = "Title",
            text = "Text",
            isClearable = true
        )

        NotificationsPacketsFFI.createNotificationPacket(notification)
    }

    @Test
    fun `isNotification extension property works`() {
        val packet = NetworkPacket(
            type = "cconnect.notification",
            body = mapOf("id" to "test")
        )

        assertTrue(packet.isNotification)
        assertFalse(packet.isCancel)
    }
}
```

### Integration Tests

**File:** `app/src/androidTest/java/org/cosmic/cosmicconnect/Plugins/NotificationsIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class NotificationsIntegrationTest {

    @Test
    fun testNotificationRoundTrip() {
        // Create a notification
        val notification = NotificationInfo(
            id = "integration-test",
            appName = "Integration Test",
            title = "Test Notification",
            text = "This is a test",
            isClearable = true,
            time = System.currentTimeMillis().toString(),
            silent = "false"
        )

        // Create packet
        val packet = NotificationsPacketsFFI.createNotificationPacket(notification)

        // Serialize to JSON
        val json = packet.serialize()

        // Deserialize
        val restored = NetworkPacket.deserialize(json)

        // Verify
        assertEquals(packet.type, restored.type)
        assertEquals(notification.id, restored.notificationId)
        assertEquals(notification.title, restored.notificationTitle)
    }
}
```

### End-to-End Tests

Test the full flow from Android notification to desktop display:

1. Post a test notification on Android
2. Verify packet is sent over network
3. Verify desktop receives and parses correctly
4. Verify desktop notification is displayed
5. Trigger action from desktop
6. Verify action is executed on Android

---

## Performance Considerations

### Icon Transfer Optimization

1. **Size Limiting**: Always normalize icons to max 96x96 pixels
2. **Compression**: Use PNG at 90% quality
3. **Caching**: Cache icons by hash to avoid re-transfer
4. **Skip Updates**: Don't re-send icon for notification updates

```kotlin
// Only send icon for new notifications, not updates
val isUpdate = currentNotifications.contains(key)
if (!isUpdate && appIcon != null) {
    attachIcon(packet, appIcon)
}
```

### Batch Processing

When syncing all notifications (e.g., on connect), consider:

1. **Throttling**: Add small delays between notifications
2. **Priority**: Send recent notifications first
3. **Limit**: Cap maximum notifications to sync (e.g., 50)

### Memory Management

1. **Clear pending intents**: Remove old reply intents after timeout
2. **Limit action cache**: Prune old notification actions
3. **Icon cache cleanup**: Remove unused cached icons periodically

```kotlin
// Clean up old pending intents
private fun cleanupPendingIntents() {
    val timeout = 24 * 60 * 60 * 1000 // 24 hours
    val now = System.currentTimeMillis()

    pendingIntents.entries.removeIf { (_, notification) ->
        now - notification.timestamp > timeout
    }
}
```

---

## Security Guidelines

### Input Validation

Always validate incoming packets:

```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    val packet = NetworkPacket.fromLegacy(np)

    // Validate required fields
    if (packet.isNotificationAction) {
        val key = packet.notificationId ?: return false
        val action = packet.body["action"] as? String ?: return false

        // Validate key exists in our tracking
        if (!actions.containsKey(key)) {
            Log.w(TAG, "Unknown notification key: $key")
            return false
        }

        triggerNotificationAction(key, action)
    }

    return true
}
```

### Privacy Controls

Respect user privacy settings:

```kotlin
// Check privacy before sending content
if (appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_CONTENTS)) {
    // Send minimal notification
    title = null
    text = null
    ticker = "New notification"
}

if (appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES)) {
    // Don't send icon
    appIcon = null
}
```

### Secure Communication

All communication must use TLS:

```rust
// Ensure TLS is enabled for all communication
impl NotificationHandler {
    pub fn send_notification(&self, notification: &Notification) -> Result<()> {
        // TLS is enforced at the connection layer
        // Never send notifications over unencrypted channels
        self.tls_connection.send(notification.to_packet())?;
        Ok(())
    }
}
```

---

## Code Examples

### Complete Android Plugin Flow

```kotlin
class NotificationsPlugin : Plugin(), NotificationReceiver.NotificationListener {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 1. Check if app is enabled
        if (!appDatabase.isEnabled(sbn.packageName)) return

        // 2. Filter unwanted notifications
        if (shouldFilter(sbn.notification)) return

        // 3. Build notification info with privacy applied
        val info = buildNotificationInfo(sbn)

        // 4. Create packet via FFI
        val packet = NotificationsPacketsFFI.createNotificationPacket(info)

        // 5. Attach icon if allowed and not an update
        if (!isUpdate(sbn.key) && shouldIncludeIcon(sbn.packageName)) {
            extractIcon(sbn)?.let { attachIcon(packet, it) }
        }

        // 6. Send to device
        device.sendPacket(packet.toLegacyPacket())
    }
}
```

### Complete Desktop Handler Flow

```rust
impl NotificationHandler {
    pub async fn handle_packet(&self, packet: Packet) -> Result<()> {
        match packet.packet_type.as_str() {
            PACKET_TYPE_NOTIFICATION => {
                if packet.body.get("isCancel").map(|v| v.as_bool().unwrap_or(false)).unwrap_or(false) {
                    self.handle_cancel(&packet).await?;
                } else {
                    self.handle_notification(&packet).await?;
                }
            }
            PACKET_TYPE_NOTIFICATION_REQUEST => {
                // Handle request (typically ignored on desktop)
            }
            _ => {
                log::warn!("Unknown notification packet type: {}", packet.packet_type);
            }
        }
        Ok(())
    }

    async fn handle_notification(&self, packet: &Packet) -> Result<()> {
        // 1. Parse notification fields
        let notification = Notification::from_packet(packet)?;

        // 2. Handle icon payload if present
        let icon_path = if let Some(hash) = &notification.payload_hash {
            if !self.icon_cache.has_icon(hash) {
                let data = self.download_payload(packet).await?;
                Some(self.icon_cache.store_icon(hash, &data)?)
            } else {
                Some(self.icon_cache.get_path(hash))
            }
        } else {
            None
        };

        // 3. Display via freedesktop
        let notif_id = show_notification(
            &self.dbus_connection,
            &notification,
            icon_path.as_deref()
        ).await?;

        // 4. Store mapping for actions/dismiss
        self.notification_map.insert(notification.id.clone(), notif_id);

        Ok(())
    }
}
```

---

## Related Documentation

- [Rich Notifications User Guide](../RICH_NOTIFICATIONS.md)
- [Rich Notifications Troubleshooting](../RICH_NOTIFICATIONS_TROUBLESHOOTING.md)
- [Rich Notifications Protocol Specification](../protocol/RICH_NOTIFICATIONS_PROTOCOL.md)
- [FFI Integration Guide](FFI_INTEGRATION_GUIDE.md)
- [Plugin API](PLUGIN_API.md)

---

**Happy coding! Contributions are welcome.**
