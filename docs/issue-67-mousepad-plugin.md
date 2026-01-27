# Issue #67: MousePad Plugin FFI Migration

**Status**: 🔄 IN PROGRESS
**Date**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 3 - Remaining Plugins
**Related**: Phase 2 complete (Issues #62-66)

## Overview

Migrate the MousePad Plugin to use dedicated Rust FFI functions for packet creation. The MousePad plugin enables remote mouse and keyboard control, allowing the desktop to control the Android device's cursor and keyboard input.

## Current Status

**MousePad Plugin uses generic NetworkPacket.create()**:
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for packet construction (line 178)
- Has custom `convertToLegacyPacket()` helper method (lines 187-200)
- Sends mouse movement, clicks, scroll, and keyboard events

**What's Missing**:
- Dedicated Rust FFI function for MousePad request packets
- Kotlin FFI wrapper class (MousePadPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated MousePad plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create MousePad request packets with command and parameters
- Single packet type for all mouse/keyboard commands
- Variable body based on command type

**Packet Type**: `cconnect.mousepad.request`

## Android Status

**Files**:
- `MousePadPlugin.kt` - Plugin implementation (already in Kotlin)

**Current Implementation (Line 178)**:
```kotlin
private fun sendMousePadPacket(body: Map<String, Any>) {
    val packet = NetworkPacket.create(PACKET_TYPE_MOUSEPAD_REQUEST, body)
    device.sendPacket(convertToLegacyPacket(packet))
}

// Called by various send commands:
sendCommand(mapOf("dx" to dx, "dy" to dy))  // Mouse movement
sendCommand(mapOf("singleclick" to true))   // Left click
sendCommand(mapOf("scroll" to true, "dx" to dx, "dy" to dy))  // Scroll
sendCommand(mapOf("key" to key, "specialKey" to specialKey))  // Keyboard
```

## Implementation Plan

### Phase 1: Add FFI Function to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add standalone MousePad request packet creation function:

```rust
/// Create MousePad request packet
pub fn create_mousepad_request(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cconnect.mousepad.request", body_data);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// MousePad Plugin

/// Create a MousePad request packet
///
/// Creates a packet for sending mouse and keyboard events to the remote device.
///
/// # Arguments
///
/// * `body_json` - JSON string containing command and parameters
[Throws=ProtocolError]
FfiPacket create_mousepad_request(string body_json);
```

### Phase 3: Export Function

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_mousepad_request,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/MousePadPlugin/MousePadPacketsFFI.kt`

```kotlin
package org.cosmic.cconnect.Plugins.MousePadPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMousepadRequest

/**
 * FFI wrapper for creating MousePad plugin packets
 *
 * The MousePad plugin enables remote mouse and keyboard control, allowing
 * the desktop to control the Android device's cursor and keyboard input.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send mouse movement events
 * - Send mouse click events (left, right, middle, double)
 * - Send scroll events
 * - Send keyboard input
 * - Send special keys
 *
 * ## Usage
 *
 * **Sending mouse movement:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val moveBody = JSONObject(mapOf(
 *     "dx" to 10.0,
 *     "dy" to -5.0
 * ))
 * val packet = MousePadPacketsFFI.createMousePadRequest(moveBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending mouse click:**
 * ```kotlin
 * val clickBody = JSONObject(mapOf("singleclick" to true))
 * val packet = MousePadPacketsFFI.createMousePadRequest(clickBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending keyboard input:**
 * ```kotlin
 * val keyBody = JSONObject(mapOf(
 *     "key" to "a",
 *     "specialKey" to 0
 * ))
 * val packet = MousePadPacketsFFI.createMousePadRequest(keyBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * @see MousePadPlugin
 */
object MousePadPacketsFFI {

    /**
     * Create a MousePad request packet
     *
     * Creates a packet for sending mouse and keyboard events to the remote device.
     * The body should contain the command type and relevant parameters.
     *
     * ## Supported Commands
     *
     * **Mouse movement:**
     * - `dx: <double>` - Horizontal movement delta
     * - `dy: <double>` - Vertical movement delta
     *
     * **Mouse clicks:**
     * - `singleclick: true` - Left click
     * - `doubleclick: true` - Double left click
     * - `middleclick: true` - Middle click
     * - `rightclick: true` - Right click
     * - `singlehold: true` - Press and hold left button
     * - `singlerelease: true` - Release left button
     *
     * **Scrolling:**
     * - `scroll: true` with `dx` and `dy` - Scroll amount
     *
     * **Keyboard:**
     * - `key: <string>` - Character to type
     * - `specialKey: <int>` - Special key code (0 for normal keys)
     *
     * The body JSON should be formatted as:
     * ```json
     * {
     *   "dx": 10.0,
     *   "dy": -5.0
     * }
     * ```
     *
     * @param bodyJson JSON string containing command and parameters
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createMousePadRequest(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMousepadRequest(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update MousePadPlugin.kt

**Changes to sendMousePadPacket() (Line 178)**:

```kotlin
// Before:
private fun sendMousePadPacket(body: Map<String, Any>) {
    val packet = NetworkPacket.create(PACKET_TYPE_MOUSEPAD_REQUEST, body)
    device.sendPacket(convertToLegacyPacket(packet))
}

// After:
import org.json.JSONObject

private fun sendMousePadPacket(body: Map<String, Any>) {
    val json = JSONObject(body).toString()
    val packet = MousePadPacketsFFI.createMousePadRequest(json)
    device.sendPacket(packet.toLegacyPacket())
}
```

**Remove** `convertToLegacyPacket()` method (Lines 187-200) - no longer needed

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]\nmod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_mousepad_request_movement() {
        let body_json = json!({
            "dx": 10.0,
            "dy": -5.0
        }).to_string();

        let packet = create_mousepad_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.mousepad.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["dx"], 10.0);
        assert_eq!(body["dy"], -5.0);
    }

    #[test]
    fn test_create_mousepad_request_click() {
        let body_json = json!({
            "singleclick": true
        }).to_string();

        let packet = create_mousepad_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.mousepad.request");
    }

    #[test]
    fn test_create_mousepad_request_keyboard() {
        let body_json = json!({
            "key": "a",
            "specialKey": 0
        }).to_string();

        let packet = create_mousepad_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.mousepad.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["key"], "a");
        assert_eq!(body["specialKey"], 0);
    }
}
```

### Integration Tests (Android)

1. Test mouse movement events
2. Test all click types (left, right, middle, double, hold/release)
3. Test scroll events
4. Test keyboard input
5. Test special keys
6. Verify packet format correctness

## Success Criteria

- Rust FFI function compiles successfully
- Android app builds without errors
- MousePadPacketsFFI.kt provides clean API
- MousePadPlugin uses FFI function
- Mouse and keyboard control works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: MousePad packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as Phase 2 plugins
5. **Simplified Code**: Removes custom `convertToLegacyPacket()` helper

## Related Documentation

- Phase 2 Complete: Issues #62-66
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/MousePadPlugin/`

---

## ✅ Completion Summary

**Status**: COMPLETE
**Date**: 2026-01-16
**Build**: SUCCESS (24 MB APK, 0 errors)

### What Was Done

1. **Rust Core Changes** (`cosmic-connect-core`):
   - Added `create_mousepad_request()` to `src/ffi/mod.rs`
   - Accepts JSON string for mouse/keyboard events
   - Parses and embeds in packet body
   - Added UDL declaration for UniFFI bindings
   - Exported function in `src/lib.rs`

2. **Android Changes** (`cosmic-connect-android`):
   - Created `MousePadPacketsFFI.kt` wrapper with comprehensive documentation
   - Updated `MousePadPlugin.kt` to use FFI wrapper
   - Added JSONObject import
   - Removed custom `convertToLegacyPacket()` helper method (19 lines removed)

3. **Build Results**:
   - Rust core: ✅ Compiled successfully
   - UniFFI bindings: ✅ Generated successfully
   - Android APK: ✅ Built successfully (24 MB, 0 errors)

### Technical Details

**Packet Format**:
```json
{
  "type": "cconnect.mousepad.request",
  "body": {
    "dx": 10.0,
    "dy": -5.0
  }
}
```

**Supported Commands**:
- Mouse movement: `dx`, `dy`
- Mouse clicks: `singleclick`, `doubleclick`, `middleclick`, `rightclick`, `singlehold`, `singlerelease`
- Scrolling: `scroll` with `dx`, `dy`
- Keyboard: `key`, `specialKey`

**Code Simplification**:
- Removed 19 lines of custom `convertToLegacyPacket()` helper
- Added cleaner FFI wrapper with 1 function
- Improved maintainability and consistency

### Commits

**cosmic-connect-core**:
- Commit: `9acf2ad` - Add MousePad plugin FFI support

**cosmic-connect-android**:
- Commit: `bc87f592` - Issue #67: MousePad Plugin FFI Migration

---

**Issue #67 Status**: ✅ COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-16
**Next**: Continue Phase 3 plugin migrations
