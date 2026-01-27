# Issue #57: Notifications Plugin FFI Migration Plan

**Issue:** #57
**Component:** Notifications Plugin
**Priority:** High (user-visible feature)
**Status:** Planning
**Created:** 2026-01-16

## Overview

Migrate the Notifications Plugin from manual Java packet construction to FFI-based implementation using the established packet-based pattern from Issues #54 (Clipboard), #55 (Telephony), and #56 (Battery).

### Why This Migration?

1. **Type Safety:** Compile-time checking for notification packet structure
2. **Maintainability:** Centralized packet logic in Rust core
3. **Consistency:** Same FFI pattern as other migrated plugins
4. **Documentation:** Better documentation through KDoc and Rust docs
5. **Testing:** Easier unit testing of packet creation logic

### Complexity Assessment

**Rating:** Medium-High (⭐⭐⭐⭐☆)

**Factors:**
- ✅ **Rust implementation exists** - notification.rs already has comprehensive implementation
- ⚠️ **Java → Kotlin conversion needed** - 656-line Java file
- ⚠️ **4 packet types** - More than Battery (2) or Clipboard (2), but less than Telephony (7)
- ⚠️ **Complex notification extraction** - Icon payloads, privacy controls, action buttons
- ⚠️ **Notification struct** - Large struct with many optional fields
- ✅ **Pattern established** - Following proven packet-based FFI approach

---

## Protocol Specification

### Packet Types

The Notifications Plugin handles 4 distinct packet types:

#### 1. `cconnect.notification` (Outgoing - Send Notification)

**Purpose:** Send a notification from Android to desktop

**Required Fields:**
- `id` (string) - Unique notification identifier
- `appName` (string) - Source application name
- `isClearable` (boolean) - Whether user can dismiss
- `time` (string) - UNIX epoch timestamp in milliseconds
- `silent` (string) - "true" for preexisting, "false" for new

**Optional Fields:**
- `title` (string) - Notification title
- `text` (string) - Notification body text
- `ticker` (string) - Combined title and text
- `requestReplyId` (string) - UUID for inline reply support
- `actions` (JSON array) - Available action button names
- `payloadHash` (string) - MD5 hash of icon payload
- `onlyOnce` (boolean) - Whether to show only once

**Payload:** Optional PNG icon bitmap

**Example:**
```json
{
  "id": "notification-123",
  "appName": "Messages",
  "title": "New Message",
  "text": "Hello from your phone!",
  "ticker": "Messages: New Message - Hello from your phone!",
  "isClearable": true,
  "time": "1704067200000",
  "silent": "false",
  "requestReplyId": "uuid-reply-123",
  "actions": ["Reply", "Mark Read"],
  "payloadHash": "a1b2c3d4e5f6..."
}
```

#### 2. `cconnect.notification` (Outgoing - Cancel Notification)

**Purpose:** Cancel a previously sent notification

**Required Fields:**
- `id` (string) - Notification ID to cancel
- `isCancel` (boolean) - true

**Example:**
```json
{
  "id": "notification-123",
  "isCancel": true
}
```

#### 3. `cconnect.notification.request` (Incoming - Request Current Notifications)

**Purpose:** Desktop requests all current Android notifications

**Field:**
- `request` (boolean) - true

**Example:**
```json
{
  "request": true
}
```

#### 4. `cconnect.notification.request` (Incoming - Dismiss Notification)

**Purpose:** Desktop requests to dismiss an Android notification

**Field:**
- `cancel` (string) - Notification ID to dismiss

**Example:**
```json
{
  "cancel": "notification-123"
}
```

#### 5. `cconnect.notification.action` (Incoming - Trigger Action)

**Purpose:** Desktop triggers a notification action button

**Fields:**
- `key` (string) - Notification ID
- `action` (string) - Action button title

**Example:**
```json
{
  "key": "notification-123",
  "action": "Reply"
}
```

#### 6. `cconnect.notification.reply` (Incoming - Send Reply)

**Purpose:** Desktop sends inline reply to notification

**Fields:**
- `requestReplyId` (string) - UUID from notification
- `message` (string) - Reply text

**Example:**
```json
{
  "requestReplyId": "uuid-reply-123",
  "message": "Thanks, I'll be there soon!"
}
```

### Protocol Notes

1. **Bi-directional:** Both Android and desktop send/receive notifications
2. **Icon Payload:** Transferred separately via TCP payload mechanism
3. **Privacy Controls:** App-level blocking of contents/images
4. **Silent Flag:** "true" for preexisting (response to request), "false" for new
5. **String Booleans:** `silent` field uses string "true"/"false" (legacy format)

---

## Current Implementation Analysis

### Rust Implementation (`cosmic-connect-core/src/plugins/notification.rs`)

**Status:** ✅ Comprehensive implementation exists

**Key Components:**

1. **Notification Struct** (Lines 154-199)
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
    pub only_once: Option<Bool>,
    pub request_reply_id: Option<String>,
    pub actions: Option<Vec<String>>,
    pub payload_hash: Option<String>,
}
```

2. **NotificationPlugin** (Lines 318-593)
- `create_notification_packet(notification: &Notification) -> Packet`
- `create_cancel_packet(notification_id: &str) -> Packet`
- `create_request_packet() -> Packet`
- `create_dismiss_packet(notification_id: &str) -> Packet`
- Handler methods for incoming packets

3. **Tests** (Lines 667-893)
- 23 test cases covering all packet types
- Need to verify they compile and pass

**What's Needed:**
- Expose packet creation methods via FFI (4 functions)
- Add FFI functions for action and reply packets (2 more functions)
- Update UDL definitions (6 total functions)

### Android Implementation (`NotificationsPlugin.java`)

**Status:** ⚠️ Java implementation, needs conversion

**File:** `src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/NotificationsPlugin.java`
**Lines:** 656 lines of Java code

**Key Features:**

1. **Notification Listening** (Lines 180-287)
- StatusBarNotification monitoring via NotificationReceiver service
- Complex notification extraction logic
- Privacy controls via AppDatabase
- Icon extraction and bitmap conversion
- Already uses immutable NetworkPacket for sending! (Lines 172, 268)

2. **Packet Receiving** (Lines 540-577)
- Handles action triggers
- Handles notification requests
- Handles dismiss requests
- Handles reply requests

3. **Helper Methods**
- `extractText()` - Extract notification text
- `extractActions()` - Extract action buttons as JSON array
- `extractIcon()` - Extract and convert icon to bitmap
- `replyToNotification()` - Send inline reply via RemoteInput
- `convertToLegacyPacket()` - Already implemented! (Lines 641-652)

**What's Needed:**
- Convert Java to Kotlin (656 lines)
- Replace manual packet construction with FFI calls
- Create NotificationsPacketsFFI.kt wrapper
- Add extension properties for packet inspection
- Maintain privacy controls and icon handling
- Keep NotificationReceiver service integration

---

## Migration Phases

### Phase 0: Planning ✍️

**Deliverable:** This comprehensive plan document

**Tasks:**
- [x] Analyze current implementations (Android + Rust)
- [x] Document protocol specification (6 packet types)
- [x] Identify FFI functions needed (6 functions)
- [x] Plan Kotlin wrapper structure
- [x] Estimate time and complexity
- [x] Create detailed phase breakdown

**Time Estimate:** 1 hour

---

### Phase 1: Rust Core Verification & Enhancement 🦀

**Deliverable:** Verified notification.rs with any needed updates

**Location:** `cosmic-connect-core/src/plugins/notification.rs`

**Tasks:**

1. **Run Existing Tests**
```bash
cd /home/olafkfreund/Source/GitHub/cosmic-connect-core
cargo test notification
```

2. **Verify Packet Creation Methods**
- Check `create_notification_packet()` matches Android requirements
- Check `create_cancel_packet()` structure
- Check `create_request_packet()` structure
- Check `create_dismiss_packet()` structure

3. **Add Missing Methods (if needed)**
- `create_action_packet(key, action)` - For action button triggers
- `create_reply_packet(reply_id, message)` - For inline replies

4. **Run Full Test Suite**
```bash
cargo test notification --verbose
```

**Success Criteria:**
- ✅ All tests pass
- ✅ All 6 packet creation methods exist
- ✅ Notification struct matches protocol

**Time Estimate:** 0.5-1 hour

---

### Phase 2: FFI Interface Implementation 🔌

**Deliverable:** FFI functions exposed via UniFFI

**Files Modified:**
1. `cosmic-connect-core/src/ffi/mod.rs`
2. `cosmic-connect-core/src/cosmic_connect_core.udl`
3. `cosmic-connect-core/src/lib.rs`

**FFI Functions to Add:**

#### Function 1: create_notification_packet

```rust
pub fn create_notification_packet(
    notification_json: String,
) -> Result<FfiPacket> {
    // Parse JSON string into Notification struct
    let notification: Notification = serde_json::from_str(&notification_json)
        .map_err(|e| ProtocolError::Json(e.to_string()))?;

    // Create packet using existing method
    let plugin = NotificationPlugin::new();
    let packet = plugin.create_notification_packet(&notification);

    Ok(packet.into())
}
```

**Rationale:** Pass full notification as JSON to avoid massive parameter list

#### Function 2: create_cancel_notification_packet

```rust
pub fn create_cancel_notification_packet(
    notification_id: String,
) -> Result<FfiPacket> {
    let plugin = NotificationPlugin::new();
    let packet = plugin.create_cancel_packet(&notification_id);
    Ok(packet.into())
}
```

#### Function 3: create_notification_request_packet

```rust
pub fn create_notification_request_packet() -> Result<FfiPacket> {
    let plugin = NotificationPlugin::new();
    let packet = plugin.create_request_packet();
    Ok(packet.into())
}
```

#### Function 4: create_dismiss_notification_packet

```rust
pub fn create_dismiss_notification_packet(
    notification_id: String,
) -> Result<FfiPacket> {
    let plugin = NotificationPlugin::new();
    let packet = plugin.create_dismiss_packet(&notification_id);
    Ok(packet.into())
}
```

#### Function 5: create_notification_action_packet

```rust
pub fn create_notification_action_packet(
    notification_key: String,
    action_name: String,
) -> Result<FfiPacket> {
    let body = json!({
        "key": notification_key,
        "action": action_name,
    });

    let packet = Packet::new("cconnect.notification.action", body);
    Ok(packet.into())
}
```

#### Function 6: create_notification_reply_packet

```rust
pub fn create_notification_reply_packet(
    reply_id: String,
    message: String,
) -> Result<FfiPacket> {
    let body = json!({
        "requestReplyId": reply_id,
        "message": message,
    });

    let packet = Packet::new("cconnect.notification.reply", body);
    Ok(packet.into())
}
```

**UDL Updates:**

```udl
// Notifications Plugin

/// Create a full notification packet
///
/// Pass notification data as JSON string to avoid massive parameter list
[Throws=ProtocolError]
FfiPacket create_notification_packet(string notification_json);

/// Create a cancel notification packet
[Throws=ProtocolError]
FfiPacket create_cancel_notification_packet(string notification_id);

/// Create a request all notifications packet
[Throws=ProtocolError]
FfiPacket create_notification_request_packet();

/// Create a dismiss notification packet
[Throws=ProtocolError]
FfiPacket create_dismiss_notification_packet(string notification_id);

/// Create a notification action packet
[Throws=ProtocolError]
FfiPacket create_notification_action_packet(
  string notification_key,
  string action_name
);

/// Create a notification reply packet
[Throws=ProtocolError]
FfiPacket create_notification_reply_packet(
  string reply_id,
  string message
);
```

**lib.rs Exports:**

```rust
pub use ffi::{
    // ... existing exports
    create_notification_packet,
    create_cancel_notification_packet,
    create_notification_request_packet,
    create_dismiss_notification_packet,
    create_notification_action_packet,
    create_notification_reply_packet,
};
```

**Build and Test:**

```bash
cd cosmic-connect-core
cargo build --release
cargo test
```

**Success Criteria:**
- ✅ All 6 FFI functions compile
- ✅ UniFFI scaffolding generates without errors
- ✅ Kotlin bindings generated
- ✅ Existing tests still pass

**Time Estimate:** 1.5-2 hours

---

### Phase 3: Android Wrapper Creation 📱

**Deliverable:** NotificationsPacketsFFI.kt with type-safe wrappers

**File Created:** `src/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/NotificationsPacketsFFI.kt`

**Estimated Lines:** 600-700 lines

**Structure:**

```kotlin
package org.cosmic.cconnect.Plugins.NotificationsPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * FFI wrapper for notifications plugin packet creation and inspection.
 *
 * Provides type-safe packet creation using the cosmic-connect-core FFI layer
 * and extension properties for inspecting notification packets.
 *
 * ## Packet Types
 *
 * - **Notification** (`cconnect.notification`): Send or cancel notification
 * - **Request** (`cconnect.notification.request`): Request all or dismiss one
 * - **Action** (`cconnect.notification.action`): Trigger action button
 * - **Reply** (`cconnect.notification.reply`): Send inline reply
 *
 * [Comprehensive KDoc documentation with examples]
 */
object NotificationsPacketsFFI {

    /**
     * Create a full notification packet.
     *
     * [Detailed KDoc]
     *
     * @param id Unique notification identifier
     * @param appName Source application name
     * @param isClearable Whether user can dismiss
     * @param time UNIX epoch timestamp in milliseconds
     * @param silent "true" for preexisting, "false" for new
     * @param title Optional notification title
     * @param text Optional notification body
     * @param ticker Optional combined title and text
     * @param requestReplyId Optional UUID for inline replies
     * @param actions Optional action button names
     * @param payloadHash Optional MD5 hash of icon
     * @return Immutable NetworkPacket ready to send
     */
    fun createNotificationPacket(
        id: String,
        appName: String,
        isClearable: Boolean,
        time: String,
        silent: String,
        title: String? = null,
        text: String? = null,
        ticker: String? = null,
        requestReplyId: String? = null,
        actions: JSONArray? = null,
        payloadHash: String? = null
    ): NetworkPacket {
        // Build JSON string
        val json = buildNotificationJson(...)

        // Call FFI
        val ffiPacket = createNotificationPacket(json)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a cancel notification packet.
     *
     * [Detailed KDoc]
     */
    fun createCancelNotificationPacket(
        notificationId: String
    ): NetworkPacket {
        val ffiPacket = createCancelNotificationPacket(notificationId)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a notification request packet.
     *
     * [Detailed KDoc]
     */
    fun createNotificationRequestPacket(): NetworkPacket {
        val ffiPacket = createNotificationRequestPacket()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a dismiss notification packet.
     *
     * [Detailed KDoc]
     */
    fun createDismissNotificationPacket(
        notificationId: String
    ): NetworkPacket {
        val ffiPacket = createDismissNotificationPacket(notificationId)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a notification action packet.
     *
     * [Detailed KDoc]
     */
    fun createNotificationActionPacket(
        notificationKey: String,
        actionName: String
    ): NetworkPacket {
        val ffiPacket = createNotificationActionPacket(notificationKey, actionName)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a notification reply packet.
     *
     * [Detailed KDoc]
     */
    fun createNotificationReplyPacket(
        replyId: String,
        message: String
    ): NetworkPacket {
        val ffiPacket = createNotificationReplyPacket(replyId, message)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Helper: Build notification JSON from parameters
     */
    private fun buildNotificationJson(...): String {
        val json = JSONObject()
        json.put("id", id)
        json.put("appName", appName)
        json.put("isClearable", isClearable)
        json.put("time", time)
        json.put("silent", silent)

        title?.let { json.put("title", it) }
        text?.let { json.put("text", it) }
        ticker?.let { json.put("ticker", it) }
        requestReplyId?.let { json.put("requestReplyId", it) }
        actions?.let { json.put("actions", it) }
        payloadHash?.let { json.put("payloadHash", it) }

        return json.toString()
    }
}

// =============================================================================
// Extension Properties for Type-Safe Packet Inspection
// =============================================================================

/**
 * Check if packet is a notification packet.
 */
val NetworkPacket.isNotificationPacket: Boolean
    get() = type == "cconnect.notification" && body.containsKey("id")

/**
 * Check if notification packet is a cancellation.
 */
val NetworkPacket.isNotificationCancel: Boolean
    get() = isNotificationPacket && (body["isCancel"] as? Boolean) == true

/**
 * Check if packet is a notification request.
 */
val NetworkPacket.isNotificationRequest: Boolean
    get() = type == "cconnect.notification.request" &&
            (body["request"] as? Boolean) == true

/**
 * Check if packet is a dismiss request.
 */
val NetworkPacket.isNotificationDismiss: Boolean
    get() = type == "cconnect.notification.request" &&
            body.containsKey("cancel")

/**
 * Check if packet is a notification action.
 */
val NetworkPacket.isNotificationAction: Boolean
    get() = type == "cconnect.notification.action" &&
            body.containsKey("key") && body.containsKey("action")

/**
 * Check if packet is a notification reply.
 */
val NetworkPacket.isNotificationReply: Boolean
    get() = type == "cconnect.notification.reply" &&
            body.containsKey("requestReplyId") && body.containsKey("message")

// Field extraction extension properties (15-20 more)
val NetworkPacket.notificationId: String?
val NetworkPacket.notificationAppName: String?
val NetworkPacket.notificationTitle: String?
val NetworkPacket.notificationText: String?
val NetworkPacket.notificationTicker: String?
val NetworkPacket.notificationIsClearable: Boolean?
val NetworkPacket.notificationTime: String?
val NetworkPacket.notificationSilent: String?
val NetworkPacket.notificationRequestReplyId: String?
val NetworkPacket.notificationActions: List<String>?
val NetworkPacket.notificationPayloadHash: String?
val NetworkPacket.notificationCancelId: String?
val NetworkPacket.notificationDismissId: String?
val NetworkPacket.notificationActionKey: String?
val NetworkPacket.notificationActionName: String?
val NetworkPacket.notificationReplyId: String?
val NetworkPacket.notificationReplyMessage: String?

// Java-compatible functions (matching all extension properties)
fun getIsNotificationPacket(packet: NetworkPacket): Boolean
fun getIsNotificationCancel(packet: NetworkPacket): Boolean
// ... 20+ more functions
```

**Documentation Requirements:**
- KDoc for all public functions (85+ lines)
- Usage examples for each packet type (12 examples)
- Protocol specification reference
- Field descriptions with validation rules

**Success Criteria:**
- ✅ All 6 packet creation functions
- ✅ 17+ extension properties
- ✅ 17+ Java-compatible functions
- ✅ Comprehensive KDoc documentation
- ✅ Code compiles without errors

**Time Estimate:** 2.5-3 hours

---

### Phase 4: Android Integration 🔧

**Deliverable:** Convert NotificationsPlugin.java to Kotlin using FFI

**Files:**
1. **Convert:** `NotificationsPlugin.java` → `NotificationsPlugin.kt`
2. **Keep:** `NotificationReceiver.java` (service - no packet logic)
3. **Keep:** `RepliableNotification.java` (data class)
4. **Keep:** `NotificationFilterActivity.java` (UI)
5. **Keep:** `AppDatabase.java` (privacy controls)

**NotificationsPlugin.kt Changes:**

#### 1. Convert to Kotlin
```bash
# Initial conversion (Android Studio)
# Then manual refinement for idioms
```

#### 2. Update Packet Sending (Lines ~169-176, 234-286)

**Before:**
```java
// Create immutable packet
Map<String, Object> body = new HashMap<>();
body.put("id", id);
body.put("isCancel", true);
NetworkPacket packet = NetworkPacket.create(PACKET_TYPE_NOTIFICATION, body);

// Convert and send
getDevice().sendPacket(convertToLegacyPacket(packet));
```

**After:**
```kotlin
// Create packet via FFI
val packet = NotificationsPacketsFFI.createCancelNotificationPacket(
    notificationId = id
)
val legacyPacket = convertToLegacyPacket(packet)
device.sendPacket(legacyPacket)
```

#### 3. Update Full Notification Sending (Lines ~192-287)

**After:**
```kotlin
private fun sendNotification(
    statusBarNotification: StatusBarNotification,
    isPreexisting: Boolean
) {
    // ... existing extraction logic ...

    // Create packet via FFI
    val packet = NotificationsPacketsFFI.createNotificationPacket(
        id = key,
        appName = appName ?: packageName,
        isClearable = statusBarNotification.isClearable,
        time = statusBarNotification.postTime.toString(),
        silent = isPreexisting.toString(),
        title = title,
        text = text,
        ticker = ticker,
        requestReplyId = repliableNotification?.id,
        actions = actionsJson,
        payloadHash = null  // Will be set after icon extraction
    )

    // Convert to legacy
    val legacyPacket = convertToLegacyPacket(packet)

    // Handle icon payload (existing logic)
    if (!isUpdate && appIcon != null) {
        attachIcon(legacyPacket, appIcon)
    }

    device.sendPacket(legacyPacket)
}
```

#### 4. Update Packet Receiving (Lines ~540-577)

**Before:**
```java
if (np.getType().equals(PACKET_TYPE_NOTIFICATION_ACTION)) {
    String key = np.getString("key");
    String title = np.getString("action");
    // ... handle action
}
```

**After:**
```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    val packet = NetworkPacket.fromLegacyPacket(np)

    return when {
        packet.isNotificationAction -> {
            val key = packet.notificationActionKey ?: return false
            val actionName = packet.notificationActionName ?: return false
            handleNotificationAction(key, actionName)
            true
        }

        packet.isNotificationRequest -> {
            if (serviceReady) {
                NotificationReceiver.runCommand(context) { service ->
                    sendCurrentNotifications(service)
                }
            }
            true
        }

        packet.isNotificationDismiss -> {
            val dismissId = packet.notificationDismissId ?: return false
            currentNotifications.remove(dismissId)
            NotificationReceiver.runCommand(context) { service ->
                service.cancelNotification(dismissId)
            }
            true
        }

        packet.isNotificationReply -> {
            val replyId = packet.notificationReplyId ?: return false
            val message = packet.notificationReplyMessage ?: return false
            replyToNotification(replyId, message)
            true
        }

        else -> false
    }
}
```

#### 5. Keep Existing Helper Methods
- `extractText()` - No changes
- `extractIcon()` - No changes
- `attachIcon()` - No changes
- `extractActions()` - No changes
- `replyToNotification()` - No changes
- `convertToLegacyPacket()` - Already exists! (Lines 641-652)

#### 6. Update Supported Packet Types

**Before:**
```java
return new String[]{
    PACKET_TYPE_NOTIFICATION_REQUEST,
    PACKET_TYPE_NOTIFICATION_REPLY,
    PACKET_TYPE_NOTIFICATION_ACTION
};
```

**After:**
```kotlin
override val supportedPacketTypes: Array<String> = arrayOf(
    PACKET_TYPE_NOTIFICATION_REQUEST,
    PACKET_TYPE_NOTIFICATION_REPLY,
    PACKET_TYPE_NOTIFICATION_ACTION
)

override val outgoingPacketTypes: Array<String> = arrayOf(
    PACKET_TYPE_NOTIFICATION
)
```

**Testing:**
1. Build and install APK
2. Enable notification listener permission
3. Trigger notification on Android
4. Verify desktop receives notification
5. Test action buttons from desktop
6. Test inline replies from desktop
7. Test dismiss sync

**Success Criteria:**
- ✅ NotificationsPlugin.kt compiles
- ✅ Notifications sent to desktop
- ✅ Actions work from desktop
- ✅ Replies work from desktop
- ✅ Dismiss sync works
- ✅ Icon payloads transfer
- ✅ Privacy controls work

**Time Estimate:** 3-4 hours

---

### Phase 5: Testing & Documentation 📋

**Deliverable:** Comprehensive testing guide and completion summary

**Files Created:**
1. `docs/issues/issue-57-testing-guide.md` (~1000 lines)
2. `docs/issues/issue-57-completion-summary.md` (~1000 lines)

**Testing Guide Contents:**

**Layer 1: FFI Layer (Rust)**
- Test 1.1: Notification packet creation (with JSON)
- Test 1.2: Cancel packet creation
- Test 1.3: Request packet creation
- Test 1.4: Dismiss packet creation
- Test 1.5: Action packet creation
- Test 1.6: Reply packet creation
- Test 1.7: UniFFI bindings generation

**Layer 2: Kotlin Wrapper**
- Test 2.1: createNotificationPacket()
- Test 2.2: createCancelNotificationPacket()
- Test 2.3: createNotificationRequestPacket()
- Test 2.4: createDismissNotificationPacket()
- Test 2.5: createNotificationActionPacket()
- Test 2.6: createNotificationReplyPacket()
- Test 2.7: Extension properties (17+ properties)
- Test 2.8: Java compatibility functions

**Layer 3: Plugin Integration**
- Test 3.1: NotificationReceiver service integration
- Test 3.2: Notification extraction and sending
- Test 3.3: Icon payload handling
- Test 3.4: Privacy controls
- Test 3.5: Action handling
- Test 3.6: Reply handling
- Test 3.7: Dismiss handling
- Test 3.8: Lifecycle management

**Layer 4: End-to-End**
- Test 4.1: Android → COSMIC notification display
- Test 4.2: COSMIC → Android notification display
- Test 4.3: Action buttons from desktop
- Test 4.4: Inline replies from desktop
- Test 4.5: Dismiss sync
- Test 4.6: Icon transfer

**Layer 5: Performance**
- Test 5.1: Packet creation speed
- Test 5.2: JSON serialization overhead
- Test 5.3: Memory usage with many notifications

**Layer 6: Regression**
- Test 6.1: Backward compatibility
- Test 6.2: Permission handling
- Test 6.3: Platform compatibility (Android 6.0+)
- Test 6.4: Privacy controls still work

**Completion Summary Contents:**
- Phase-by-phase breakdown
- Code statistics
- Time tracking
- Comparison with Issues #54, #55, #56
- Lessons learned
- Known issues
- Follow-up actions

**Time Estimate:** 2-2.5 hours

---

## Timeline & Estimates

### Total Estimated Time: 11-14 hours

| Phase | Tasks | Estimated Time | Complexity |
|-------|-------|----------------|------------|
| Phase 0 | Planning | 1h | Low |
| Phase 1 | Rust verification | 0.5-1h | Low |
| Phase 2 | FFI interface (6 functions) | 1.5-2h | Medium |
| Phase 3 | Android wrapper (600-700 lines) | 2.5-3h | Medium-High |
| Phase 4 | Plugin integration (Java→Kotlin) | 3-4h | High |
| Phase 5 | Testing & docs | 2-2.5h | Medium |
| **Total** | | **11-14h** | **Medium-High** |

### Comparison with Previous Migrations

| Plugin | Packet Types | FFI Functions | Wrapper Lines | Total Time | Complexity |
|--------|--------------|---------------|---------------|------------|------------|
| Clipboard (#54) | 2 | 2 | 325 | 7h | Medium |
| Telephony (#55) | 7 | 7 | 950 | 14h | High |
| Battery (#56) | 2 | 2 | 380 | 8.25h | Medium |
| **Notifications (#57)** | **4** | **6** | **600-700** | **11-14h** | **Medium-High** |

**Analysis:**
- More complex than Battery due to 4 packet types and Java→Kotlin conversion
- Simpler than Telephony (fewer packet types, cleaner structure)
- JSON parameter approach reduces FFI function complexity
- Existing convertToLegacyPacket() helper saves time

---

## Key Architectural Decisions

### Decision 1: JSON Parameter for Notification Creation

**Context:** Notification struct has 12 fields (5 required, 7 optional)

**Options:**
1. 12-parameter FFI function
2. Pass full notification as JSON string
3. Multiple FFI functions (one for each field combination)

**Decision:** Pass notification as JSON string

**Rationale:**
- **Simpler FFI:** Single string parameter vs 12 parameters
- **Flexibility:** Easy to add/remove fields without breaking FFI
- **Type Safety:** Rust validates JSON structure
- **Maintainability:** Kotlin builds JSON, Rust parses it
- **Performance:** Minimal overhead (JSON parsing is fast)

**Implementation:**
```kotlin
val json = buildNotificationJson(...)
val ffiPacket = createNotificationPacket(json)
```

```rust
pub fn create_notification_packet(notification_json: String) -> Result<FfiPacket> {
    let notification: Notification = serde_json::from_str(&notification_json)?;
    // ... create packet
}
```

---

### Decision 2: Keep Java Helper Classes Unchanged

**Context:** NotificationReceiver.java, AppDatabase.java, etc. contain no packet logic

**Options:**
1. Convert all files to Kotlin
2. Keep non-packet-related files as Java

**Decision:** Keep as Java (only convert NotificationsPlugin.java)

**Rationale:**
- **Risk Reduction:** Less code to change = less chance of bugs
- **Focused Migration:** Only packet-related code needs FFI
- **Time Savings:** Avoid converting 1000+ lines of working code
- **Interoperability:** Kotlin and Java work seamlessly together

**Files to Keep as Java:**
- `NotificationReceiver.java` - Service implementation
- `RepliableNotification.java` - Data class
- `NotificationFilterActivity.java` - UI
- `AppDatabase.java` - Privacy database

**Files to Convert:**
- `NotificationsPlugin.java` → `NotificationsPlugin.kt` (packet logic)

---

### Decision 3: Comprehensive Extension Properties

**Context:** Notification packets have many optional fields

**Decision:** Create extension property for every field (17+ properties)

**Rationale:**
- **Type Safety:** Null-safe field access
- **Convenience:** Clean inspection syntax
- **Consistency:** Matches Clipboard, Telephony, Battery patterns
- **Documentation:** Each property has KDoc

**Example:**
```kotlin
when {
    packet.isNotificationPacket -> {
        val title = packet.notificationTitle ?: "No title"
        val text = packet.notificationText ?: "No text"
        val isClearable = packet.notificationIsClearable ?: true
        // ... handle notification
    }
}
```

---

## Risk Assessment

### High Risks ⚠️

#### Risk 1: Java → Kotlin Conversion Errors

**Probability:** Medium
**Impact:** High

**Mitigation:**
- Use Android Studio's automatic conversion
- Manual review of all converted code
- Test each method individually
- Compare with original Java line-by-line

#### Risk 2: Icon Payload Handling

**Probability:** Medium
**Impact:** Medium

**Mitigation:**
- Keep existing attachIcon() logic unchanged
- Test with various icon sizes
- Verify payload transfer still works
- Check MD5 hash generation

### Medium Risks ⚠️

#### Risk 3: NotificationReceiver Service Integration

**Probability:** Low
**Impact:** High

**Mitigation:**
- Don't modify NotificationReceiver.java
- Only change how NotificationsPlugin interacts with it
- Test service binding still works

#### Risk 4: Privacy Controls Breaking

**Probability:** Low
**Impact:** Medium

**Mitigation:**
- Keep AppDatabase logic unchanged
- Test privacy blocking still works
- Verify per-app settings persist

### Low Risks ⚠️

#### Risk 5: JSON Serialization Performance

**Probability:** Low
**Impact:** Low

**Mitigation:**
- Benchmark JSON creation vs direct FFI
- Use efficient JSONObject building
- Cache notification JSON if needed

---

## Success Criteria

The Notifications Plugin FFI migration is successful when:

### Code Quality
- [ ] All Rust tests pass (23+ tests in notification.rs)
- [ ] Rust code compiles without errors or warnings
- [ ] Kotlin code compiles without errors or warnings
- [ ] Code follows project style guidelines

### Functionality
- [ ] Notifications sent from Android to desktop
- [ ] Notifications appear on desktop correctly
- [ ] Icons transfer and display
- [ ] Action buttons work from desktop
- [ ] Inline replies work from desktop
- [ ] Dismiss sync works bi-directionally
- [ ] Privacy controls work (block contents/images per app)
- [ ] Silent flag works (preexisting vs new)

### Performance
- [ ] Notification sending latency < 100ms
- [ ] JSON serialization overhead < 1ms
- [ ] Memory usage comparable to before
- [ ] No performance regressions

### Compatibility
- [ ] Works on Android 6.0+ (API 23+)
- [ ] Notification listener permission works
- [ ] Backward compatible with desktop (KDE Connect)
- [ ] No changes to packet format

### Documentation
- [ ] Migration plan complete (this document)
- [ ] Testing guide created (~1000 lines)
- [ ] Completion summary created (~1000 lines)
- [ ] KDoc comprehensive (100+ lines)
- [ ] Usage examples provided (15+ examples)

### Testing
- [ ] FFI layer tests pass
- [ ] Kotlin wrapper unit tests created
- [ ] Plugin integration tests pass
- [ ] End-to-end tests pass
- [ ] Performance benchmarks run
- [ ] Regression tests pass

---

## Follow-Up Actions

### Immediate (After Migration)
- [ ] Run full test suite
- [ ] Performance benchmarking
- [ ] User acceptance testing
- [ ] Update project README

### Short-Term (Next Sprint)
- [ ] Extract convertToLegacyPacket() to shared utility
- [ ] Create automated UI tests
- [ ] Add telemetry for notification stats
- [ ] Optimize icon transfer

### Long-Term (Future)
- [ ] Complete remaining plugin migrations
- [ ] Migrate away from legacy NetworkPacket entirely
- [ ] Add notification grouping support
- [ ] Add notification categories

---

## Comparison with Previous Migrations

### Issue #54: Clipboard Plugin

| Aspect | Clipboard | Notifications | Difference |
|--------|-----------|---------------|------------|
| Packet Types | 2 | 4 | +100% |
| FFI Functions | 2 | 6 | +200% |
| Wrapper Lines | 325 | 600-700 | +85-115% |
| Optional Fields | 1 | 7 | +600% |
| Payload | None | Icon bitmap | +complexity |
| Java→Kotlin | No | Yes | +complexity |

**Key Differences:**
- Notifications has icon payload transfer
- Requires Java→Kotlin conversion
- More complex packet inspection
- Privacy controls integration

### Issue #55: Telephony Plugin

| Aspect | Telephony | Notifications | Difference |
|--------|-----------|---------------|------------|
| Packet Types | 7 | 4 | -43% |
| FFI Functions | 7 | 6 | -14% |
| Wrapper Lines | 950 | 600-700 | -26-37% |
| Time Estimate | 12-16h | 11-14h | -8-13% |
| Complexity | High | Medium-High | Lower |

**Key Differences:**
- Telephony has more packet types
- Notifications has icon payload
- Notifications simpler overall structure
- Both require complex Android permissions

### Issue #56: Battery Plugin

| Aspect | Battery | Notifications | Difference |
|--------|-----------|---------------|------------|
| Packet Types | 2 | 4 | +100% |
| FFI Functions | 2 | 6 | +200% |
| Wrapper Lines | 380 | 600-700 | +58-84% |
| Time Estimate | 6-8h | 11-14h | +63-88% |
| Complexity | Medium | Medium-High | Higher |

**Key Differences:**
- Battery much simpler (2 packet types)
- Notifications has icon payload
- Notifications requires Java→Kotlin
- Battery had no conversion needed

---

## Lessons from Previous Migrations

### From Issue #54 (Clipboard)
✅ **Apply:** Extension properties pattern works great
✅ **Apply:** Java-compatible functions needed
✅ **Apply:** Comprehensive KDoc with examples
⚠️ **Improve:** Extract convertToLegacyPacket() to shared utility

### From Issue #55 (Telephony)
✅ **Apply:** Large wrapper files benefit from section comments
✅ **Apply:** Complex packet inspection needs many extension properties
✅ **Apply:** Test each packet type individually
⚠️ **Improve:** Consider JSON parameters for complex packets (doing this!)

### From Issue #56 (Battery)
✅ **Apply:** State tracking pattern for change detection
✅ **Apply:** Computed properties (like isBatteryLow)
✅ **Apply:** Mark todos as complete immediately
⚠️ **Improve:** Test plan execution (not just documentation)

---

## Notes

### Packet Type Naming

**Important:** The Android implementation uses "cosmicconnect" prefix, but the Rust implementation uses "kdeconnect" prefix. This is intentional:

- **Android packets:** `cconnect.notification` (Lines 67-70)
- **Rust packets:** `cconnect.notification` (Line 406)

**Action:** Verify packet type compatibility during Phase 1. May need to update Rust to use "cosmicconnect" prefix for consistency.

### Icon Payload Transfer

The icon payload is handled separately from the packet body:

1. Extract icon bitmap (Lines 279-284)
2. Compress to PNG
3. Attach as payload to legacy packet
4. Add MD5 hash to packet body
5. Desktop downloads payload via TCP

This flow should remain unchanged during migration.

### Privacy Controls

The AppDatabase provides per-app privacy settings:

- `BLOCK_CONTENTS` - Hide title/text, show only "New notification"
- `BLOCK_IMAGES` - Don't send icon payload

These controls are checked during notification extraction (Lines 243-244, 281) and must be preserved.

---

## Sign-Off

**Issue:** #57 - Notifications Plugin FFI Migration
**Status:** ✅ Plan Complete - Ready for Implementation
**Created:** 2026-01-16
**Author:** Claude Code Agent

**Next Step:** Phase 1 - Rust Core Verification & Enhancement

---

**Version:** 1.0
**Last Updated:** 2026-01-16
