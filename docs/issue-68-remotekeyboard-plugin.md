# Issue #68: RemoteKeyboard Plugin FFI Migration

**Status**: 🔄 IN PROGRESS
**Date**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 3 - Remaining Plugins
**Related**: Issue #67 (MousePad) complete

## Overview

Migrate the RemoteKeyboard Plugin to use dedicated Rust FFI functions for packet creation. The RemoteKeyboard plugin enables the device to act as a remote keyboard input method, allowing the desktop to type on the Android device.

## Current Status

**RemoteKeyboard Plugin uses legacy NetworkPacket directly**:
- Still in Java (not Kotlin)
- Directly creates legacy NetworkPacket instances
- **Receives** keyboard events from desktop
- **Sends** two packet types: echo replies and keyboard state

**What's Missing**:
- Dedicated Rust FFI functions for both packet types
- Kotlin FFI wrapper class (RemoteKeyboardPacketsFFI.kt)
- Migration from Java to Kotlin (optional but recommended)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated RemoteKeyboard plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create MOUSEPAD_ECHO packets (acknowledgment/echo replies)
- Create MOUSEPAD_KEYBOARDSTATE packets (keyboard visibility state)

**Packet Types**:
- `cosmicconnect.mousepad.echo`
- `cosmicconnect.mousepad.keyboardstate`

## Android Status

**Files**:
- `RemoteKeyboardPlugin.java` - Plugin implementation (still in Java)
- `RemoteKeyboardService.java` - Input method service

**Current Implementation**:

**Echo Reply (Lines 387-400)**:
```java
NetworkPacket reply = new NetworkPacket(PACKET_TYPE_MOUSEPAD_ECHO);
reply.set("key", np.getString("key"));
if (np.has("specialKey"))
    reply.set("specialKey", np.getInt("specialKey"));
if (np.has("shift"))
    reply.set("shift", np.getBoolean("shift"));
if (np.has("ctrl"))
    reply.set("ctrl", np.getBoolean("ctrl"));
if (np.has("alt"))
    reply.set("alt", np.getBoolean("alt"));
reply.set("isAck", true);
getDevice().sendPacket(reply);
```

**Keyboard State (Lines 410-414)**:
```java
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE);
packet.set("state", state);
getDevice().sendPacket(packet);
```

## Implementation Plan

### Phase 1: Add FFI Functions to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add two FFI functions for RemoteKeyboard packets:

```rust
/// Create MOUSEPAD echo packet (acknowledgment reply)
pub fn create_mousepad_echo(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cosmicconnect.mousepad.echo", body_data);
    Ok(packet.into())
}

/// Create MOUSEPAD keyboard state packet
pub fn create_mousepad_keyboardstate(state: bool) -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "state": state
    });

    let packet = Packet::new("cosmicconnect.mousepad.keyboardstate", body);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// RemoteKeyboard Plugin

/// Create a MOUSEPAD echo packet
///
/// Creates an acknowledgment/echo packet in response to keyboard input.
///
/// # Arguments
///
/// * `body_json` - JSON string containing key, modifiers, and isAck flag
[Throws=ProtocolError]
FfiPacket create_mousepad_echo(string body_json);

/// Create a MOUSEPAD keyboard state packet
///
/// Creates a packet to notify the desktop of keyboard visibility state.
///
/// # Arguments
///
/// * `state` - Keyboard visible/active state (true = active)
[Throws=ProtocolError]
FfiPacket create_mousepad_keyboardstate(boolean state);
```

### Phase 3: Export Functions

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_mousepad_echo,
    create_mousepad_keyboardstate,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/RemoteKeyboardPlugin/RemoteKeyboardPacketsFFI.kt`

```kotlin
package org.cosmic.cosmicconnect.Plugins.RemoteKeyboardPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMousepadEcho
import uniffi.cosmic_connect_core.createMousepadKeyboardstate

/**
 * FFI wrapper for creating RemoteKeyboard plugin packets
 *
 * The RemoteKeyboard plugin enables the device to act as a remote keyboard
 * input method, allowing the desktop to type on the Android device. This
 * wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send echo/acknowledgment replies for keyboard input
 * - Notify desktop of keyboard visibility state
 *
 * @see RemoteKeyboardPlugin
 */
object RemoteKeyboardPacketsFFI {

    /**
     * Create a MOUSEPAD echo packet
     *
     * Creates an acknowledgment/echo packet in response to keyboard input
     * from the desktop. Used when sendAck is requested.
     *
     * The body JSON should contain:
     * - `key`: The key character (if visible key)
     * - `specialKey`: Special key code (if special key)
     * - `shift`: Shift modifier state
     * - `ctrl`: Ctrl modifier state
     * - `alt`: Alt modifier state
     * - `isAck`: Always true for echo replies
     *
     * @param bodyJson JSON string containing echo data
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createEchoPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMousepadEcho(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a MOUSEPAD keyboard state packet
     *
     * Creates a packet to notify the desktop of the keyboard's visibility
     * and active state. Sent when keyboard becomes visible/invisible or
     * when plugin is created.
     *
     * @param state Keyboard active state (true = keyboard visible/active)
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createKeyboardStatePacket(state: Boolean): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMousepadKeyboardstate(state)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update RemoteKeyboardPlugin

**Option 1: Keep Java, minimal changes**

Update packet creation to use FFI wrapper (add Kotlin interop).

**Option 2: Convert to Kotlin (recommended)**

Convert entire plugin to Kotlin and use FFI wrapper.

**Changes for Echo Reply (Lines 387-400)**:
```kotlin
// Before (Java):
NetworkPacket reply = new NetworkPacket(PACKET_TYPE_MOUSEPAD_ECHO);
reply.set("key", np.getString("key"));
// ... set all fields ...
getDevice().sendPacket(reply);

// After (Kotlin):
import org.json.JSONObject

val body = mutableMapOf<String, Any>("isAck" to true)
np.getString("key")?.let { body["key"] = it }
if (np.has("specialKey")) body["specialKey"] = np.getInt("specialKey")
if (np.has("shift")) body["shift"] = np.getBoolean("shift")
if (np.has("ctrl")) body["ctrl"] = np.getBoolean("ctrl")
if (np.has("alt")) body["alt"] = np.getBoolean("alt")

val json = JSONObject(body).toString()
val packet = RemoteKeyboardPacketsFFI.createEchoPacket(json)
device.sendPacket(packet.toLegacyPacket())
```

**Changes for Keyboard State (Lines 410-414)**:
```kotlin
// Before (Java):
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE);
packet.set("state", state);
getDevice().sendPacket(packet);

// After (Kotlin):
val packet = RemoteKeyboardPacketsFFI.createKeyboardStatePacket(state)
device.sendPacket(packet.toLegacyPacket())
```

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_mousepad_echo() {
        let body_json = json!({
            "key": "a",
            "shift": false,
            "ctrl": false,
            "alt": false,
            "isAck": true
        }).to_string();

        let packet = create_mousepad_echo(body_json).unwrap();
        assert_eq!(packet.packet_type, "cosmicconnect.mousepad.echo");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["key"], "a");
        assert_eq!(body["isAck"], true);
    }

    #[test]
    fn test_create_mousepad_keyboardstate() {
        let packet = create_mousepad_keyboardstate(true).unwrap();
        assert_eq!(packet.packet_type, "cosmicconnect.mousepad.keyboardstate");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["state"], true);
    }
}
```

### Integration Tests (Android)

1. Test echo packet creation with various modifiers
2. Test keyboard state packet (visible/hidden)
3. Verify packet format correctness
4. Test actual keyboard input flow

## Success Criteria

- Rust FFI functions compile successfully
- Android app builds without errors
- RemoteKeyboardPacketsFFI.kt provides clean API
- RemoteKeyboardPlugin uses FFI functions
- Keyboard input and state notification work correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: RemoteKeyboard packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet formats
4. **Consistency**: Follows same pattern as other migrated plugins
5. **Simplified Code**: Cleaner packet creation

## Related Documentation

- Phase 2 Complete: Issues #62-66
- Issue #67 (MousePad) complete
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/RemoteKeyboardPlugin/`

---

## Status Updates

**2026-01-16**: Issue created, survey completed
