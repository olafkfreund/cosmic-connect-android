# Issue #73: ReceiveNotifications Plugin FFI Migration

**Status**: 🔄 IN PROGRESS
**Date**: 2026-01-17
**Priority**: MEDIUM
**Phase**: Phase 3 - Remaining Plugins
**Related**: Issues #67-72 complete

## Overview

Migrate the ReceiveNotifications Plugin to use dedicated Rust FFI functions for packet creation. The ReceiveNotifications plugin receives notifications from the desktop and displays them on the Android device.

## Current Status

**ReceiveNotifications Plugin uses NetworkPacket.create()**:
- Already in Kotlin
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for notification request packet (line 41-44)
- Has `convertToLegacyPacket()` method (lines 118-134)
- Sends ONE packet type: notification request

**What's Missing**:
- Kotlin FFI wrapper class (ReceiveNotificationsPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)
- Remove convertToLegacyPacket() helper

## Rust Core Status

**Existing FFI Function**: ✅ `create_notification_request_packet()` already exists

**File**: `cosmic-connect-core/src/ffi/mod.rs` (line 988-995)

**Function Signature:**
```rust
pub fn create_notification_request_packet() -> Result<FfiPacket>
```

**Status**: FFI function already implemented in Phase 2 (Issue #62 - NotificationsPlugin)
- No new Rust code needed
- No parameters required (simple request packet)

## Android Status

**Files**:
- `ReceiveNotificationsPlugin.kt` - Plugin implementation (141 lines, already in Kotlin)

**Current Implementation**:

**Notification Request (Lines 41-48)**:
```kotlin
override fun onCreate(): Boolean {
    // request all existing notifications
    // Create immutable packet
    val packet = NetworkPacket.create(
        PACKET_TYPE_NOTIFICATION_REQUEST,
        mapOf("request" to true)
    )

    // Convert and send
    device.sendPacket(convertToLegacyPacket(packet))
    return true
}
```

**Helper Method to Remove (Lines 118-134)**:
```kotlin
private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
    val legacy = LegacyNetworkPacket(ffi.type)

    // Copy all body fields
    ffi.body.forEach { (key, value) ->
        when (value) {
            is String -> legacy.set(key, value)
            is Int -> legacy.set(key, value)
            is Long -> legacy.set(key, value)
            is Boolean -> legacy.set(key, value)
            is Double -> legacy.set(key, value)
            else -> legacy.set(key, value.toString())
        }
    }

    return legacy
}
```

## Implementation Plan

### Phase 1: Rust Core Status

✅ **No changes needed** - `create_notification_request_packet()` already exists from Phase 2

The function:
- Creates a packet with type `cosmicconnect.notification.request`
- Includes `{"request": true}` in the body
- Takes no parameters

### Phase 2: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/ReceiveNotificationsPlugin/ReceiveNotificationsPacketsFFI.kt`

```kotlin
package org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createNotificationRequestPacket

/**
 * FFI wrapper for creating ReceiveNotifications plugin packets
 *
 * The ReceiveNotifications plugin receives notifications from the desktop
 * and displays them on the Android device. This wrapper provides a clean
 * Kotlin API over the Rust FFI core functions.
 *
 * @see ReceiveNotificationsPlugin
 */
object ReceiveNotificationsPacketsFFI {

    /**
     * Create a notification request packet
     *
     * Creates a packet requesting all current notifications from the remote device.
     * This is typically sent when the plugin initializes or when requesting a
     * refresh of notification state.
     *
     * @return NetworkPacket ready to send
     */
    fun createNotificationRequestPacket(): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createNotificationRequestPacket()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 3: Update ReceiveNotificationsPlugin.kt

**Changes to onCreate() (Lines 38-49)**:
```kotlin
// Before:
override fun onCreate(): Boolean {
    // request all existing notifications
    // Create immutable packet
    val packet = NetworkPacket.create(
        PACKET_TYPE_NOTIFICATION_REQUEST,
        mapOf("request" to true)
    )

    // Convert and send
    device.sendPacket(convertToLegacyPacket(packet))
    return true
}

// After:
override fun onCreate(): Boolean {
    // request all existing notifications
    val packet = ReceiveNotificationsPacketsFFI.createNotificationRequestPacket()
    device.sendPacket(packet.toLegacyPacket())
    return true
}
```

**Remove** `convertToLegacyPacket()` method (Lines 118-134) - no longer needed

## Testing Plan

### Integration Tests (Android)

1. Test notification request packet creation
2. Test plugin onCreate() sends request correctly
3. Test receiving and displaying notifications from desktop
4. Verify packet format correctness

## Success Criteria

- Android app builds without errors
- ReceiveNotificationsPacketsFFI.kt provides clean API
- ReceiveNotificationsPlugin uses FFI function
- Notification request works correctly
- convertToLegacyPacket() helper removed
- No regression in existing functionality

## Benefits

1. **Code Reuse**: Notification request logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as other migrated plugins
5. **Simplified Code**: Removes 17-line helper method

## Related Documentation

- Phase 2: Issue #62 (NotificationsPlugin) - Contains the FFI function
- Phase 3: Issues #67-72 complete
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/ReceiveNotificationsPlugin/`

---

## Status Updates

**2026-01-17**: Issue created, survey completed
