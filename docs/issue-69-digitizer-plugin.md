# Issue #69: Digitizer Plugin FFI Migration

**Status**: 🔄 IN PROGRESS
**Date**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 3 - Remaining Plugins
**Related**: Issues #67 (MousePad), #68 (RemoteKeyboard) complete

## Overview

Migrate the Digitizer Plugin to use dedicated Rust FFI functions for packet creation. The Digitizer plugin enables pen/stylus input for drawing tablets, allowing the desktop to use the Android device as a drawing surface.

## Current Status

**Digitizer Plugin uses generic NetworkPacket.create()**:
- Already in Kotlin
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for packet construction (lines 52, 66, 87)
- Has custom `convertToLegacyPacket()` helper method (lines 96-112)
- Sends session management and pen/stylus events

**What's Missing**:
- Dedicated Rust FFI functions for both packet types
- Kotlin FFI wrapper class (DigitizerPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated Digitizer plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create DIGITIZER_SESSION packets (session start/end with dimensions)
- Create DIGITIZER packets (pen/stylus events with coordinates, pressure, tool type)

**Packet Types**:
- `cconnect.digitizer.session`
- `cconnect.digitizer`

## Android Status

**Files**:
- `DigitizerPlugin.kt` - Plugin implementation (already in Kotlin)
- `DigitizerActivity.kt` - Drawing surface activity
- `ToolEvent.kt` - Data class for pen/stylus events

**Current Implementation**:

**Session Start (Lines 50-62)**:
```kotlin
val packet = NetworkPacket.create(PACKET_TYPE_DIGITIZER_SESSION, mapOf(
    "action" to "start",
    "width" to width,
    "height" to height,
    "resolutionX" to resolutionX,
    "resolutionY" to resolutionY
))
device.sendPacket(convertToLegacyPacket(packet))
```

**Session End (Lines 64-72)**:
```kotlin
val packet = NetworkPacket.create(PACKET_TYPE_DIGITIZER_SESSION, mapOf(
    "action" to "end"
))
device.sendPacket(convertToLegacyPacket(packet))
```

**Pen/Stylus Event (Lines 74-91)**:
```kotlin
val body = mutableMapOf<String, Any>()
event.active?.let { body["active"] = it }
event.touching?.let { body["touching"] = it }
event.tool?.let { body["tool"] = it.name }  // "Pen" or "Rubber"
event.x?.let { body["x"] = it }
event.y?.let { body["y"] = it }
event.pressure?.let { body["pressure"] = it }

val packet = NetworkPacket.create(PACKET_TYPE_DIGITIZER, body.toMap())
device.sendPacket(convertToLegacyPacket(packet))
```

## Implementation Plan

### Phase 1: Add FFI Functions to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add two FFI functions for Digitizer packets:

```rust
/// Create DIGITIZER session packet (start/end)
pub fn create_digitizer_session(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cconnect.digitizer.session", body_data);
    Ok(packet.into())
}

/// Create DIGITIZER event packet (pen/stylus events)
pub fn create_digitizer_event(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cconnect.digitizer", body_data);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// Digitizer Plugin

/// Create a DIGITIZER session packet
///
/// Creates a packet for starting or ending a drawing session.
///
/// # Arguments
///
/// * `body_json` - JSON string containing action and session parameters
[Throws=ProtocolError]
FfiPacket create_digitizer_session(string body_json);

/// Create a DIGITIZER event packet
///
/// Creates a packet for pen/stylus input events.
///
/// # Arguments
///
/// * `body_json` - JSON string containing tool event data
[Throws=ProtocolError]
FfiPacket create_digitizer_event(string body_json);
```

### Phase 3: Export Functions

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_digitizer_session,
    create_digitizer_event,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/DigitizerPlugin/DigitizerPacketsFFI.kt`

```kotlin
package org.cosmic.cconnect.Plugins.DigitizerPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createDigitizerSession
import uniffi.cosmic_connect_core.createDigitizerEvent

/**
 * FFI wrapper for creating Digitizer plugin packets
 *
 * The Digitizer plugin enables pen/stylus input for drawing tablets,
 * allowing the desktop to use the Android device as a drawing surface.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * @see DigitizerPlugin
 */
object DigitizerPacketsFFI {

    /**
     * Create a DIGITIZER session packet
     *
     * Creates a packet for starting or ending a drawing session.
     *
     * For session start, include:
     * - `action`: "start"
     * - `width`: Canvas width in pixels
     * - `height`: Canvas height in pixels
     * - `resolutionX`: Horizontal resolution
     * - `resolutionY`: Vertical resolution
     *
     * For session end, include:
     * - `action`: "end"
     *
     * @param bodyJson JSON string containing session parameters
     * @return NetworkPacket ready to send
     */
    fun createSessionPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createDigitizerSession(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a DIGITIZER event packet
     *
     * Creates a packet for pen/stylus input events with coordinates,
     * pressure, and tool type.
     *
     * Fields (all optional):
     * - `active`: Tool is active
     * - `touching`: Tool is touching surface
     * - `tool`: Tool type ("Pen" or "Rubber")
     * - `x`: X coordinate
     * - `y`: Y coordinate
     * - `pressure`: Pressure level (0.0-1.0)
     *
     * @param bodyJson JSON string containing event data
     * @return NetworkPacket ready to send
     */
    fun createEventPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createDigitizerEvent(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update DigitizerPlugin.kt

**Add import**:
```kotlin
import org.json.JSONObject
```

**Changes to startSession() (Lines 50-62)**:
```kotlin
// Before:
val packet = NetworkPacket.create(PACKET_TYPE_DIGITIZER_SESSION, mapOf(
    "action" to "start",
    "width" to width,
    "height" to height,
    "resolutionX" to resolutionX,
    "resolutionY" to resolutionY
))
device.sendPacket(convertToLegacyPacket(packet))

// After:
val body = mapOf(
    "action" to "start",
    "width" to width,
    "height" to height,
    "resolutionX" to resolutionX,
    "resolutionY" to resolutionY
)
val json = JSONObject(body).toString()
val packet = DigitizerPacketsFFI.createSessionPacket(json)
device.sendPacket(packet.toLegacyPacket())
```

**Changes to endSession() (Lines 64-72)**:
```kotlin
// Before:
val packet = NetworkPacket.create(PACKET_TYPE_DIGITIZER_SESSION, mapOf(
    "action" to "end"
))
device.sendPacket(convertToLegacyPacket(packet))

// After:
val json = JSONObject(mapOf("action" to "end")).toString()
val packet = DigitizerPacketsFFI.createSessionPacket(json)
device.sendPacket(packet.toLegacyPacket())
```

**Changes to reportEvent() (Lines 74-91)**:
```kotlin
// Before:
val body = mutableMapOf<String, Any>()
event.active?.let { body["active"] = it }
event.touching?.let { body["touching"] = it }
event.tool?.let { body["tool"] = it.name }
event.x?.let { body["x"] = it }
event.y?.let { body["y"] = it }
event.pressure?.let { body["pressure"] = it }
val packet = NetworkPacket.create(PACKET_TYPE_DIGITIZER, body.toMap())
device.sendPacket(convertToLegacyPacket(packet))

// After:
val body = mutableMapOf<String, Any>()
event.active?.let { body["active"] = it }
event.touching?.let { body["touching"] = it }
event.tool?.let { body["tool"] = it.name }
event.x?.let { body["x"] = it }
event.y?.let { body["y"] = it }
event.pressure?.let { body["pressure"] = it }
val json = JSONObject(body).toString()
val packet = DigitizerPacketsFFI.createEventPacket(json)
device.sendPacket(packet.toLegacyPacket())
```

**Remove** `convertToLegacyPacket()` method (Lines 96-112) - no longer needed

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_digitizer_session_start() {
        let body_json = json!({
            "action": "start",
            "width": 1920,
            "height": 1080,
            "resolutionX": 96,
            "resolutionY": 96
        }).to_string();

        let packet = create_digitizer_session(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.digitizer.session");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["action"], "start");
        assert_eq!(body["width"], 1920);
    }

    #[test]
    fn test_create_digitizer_event() {
        let body_json = json!({
            "active": true,
            "touching": true,
            "tool": "Pen",
            "x": 500,
            "y": 300,
            "pressure": 0.75
        }).to_string();

        let packet = create_digitizer_event(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.digitizer");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["tool"], "Pen");
        assert_eq!(body["pressure"], 0.75);
    }
}
```

### Integration Tests (Android)

1. Test session start with various screen sizes
2. Test session end
3. Test pen events with different coordinates and pressures
4. Test rubber/eraser tool events
5. Verify packet format correctness

## Success Criteria

- Rust FFI functions compile successfully
- Android app builds without errors
- DigitizerPacketsFFI.kt provides clean API
- DigitizerPlugin uses FFI functions
- Drawing tablet functionality works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: Digitizer packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet formats
4. **Consistency**: Follows same pattern as other migrated plugins
5. **Simplified Code**: Removes custom `convertToLegacyPacket()` helper

## Related Documentation

- Phase 2 Complete: Issues #62-66
- Phase 3: Issues #67 (MousePad), #68 (RemoteKeyboard) complete
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/DigitizerPlugin/`

---

## ✅ Completion Summary

**Status**: COMPLETE
**Date**: 2026-01-16
**Build**: SUCCESS (23 MB APK, 0 errors)

### What Was Done

1. **Rust Core Changes** (`cosmic-connect-core`):
   - Added `create_digitizer_session()` to `src/ffi/mod.rs`
   - Added `create_digitizer_event()` to `src/ffi/mod.rs`
   - Accepts JSON string for session packets (action, dimensions, resolution)
   - Accepts JSON string for event packets (active, touching, tool, x, y, pressure)
   - Added UDL declarations for both functions
   - Exported functions in `src/lib.rs`

2. **Android Changes** (`cosmic-connect-android`):
   - Created `DigitizerPacketsFFI.kt` wrapper with comprehensive documentation
   - Updated `DigitizerPlugin.kt` to use FFI wrapper
   - Added JSONObject import
   - Updated startSession() to use FFI (lines 51-62)
   - Updated endSession() to use FFI (lines 64-68)
   - Updated reportEvent() to use FFI (lines 70-85)
   - Removed custom `convertToLegacyPacket()` helper method (17 lines removed)

3. **Build Results**:
   - Rust core: ✅ Compiled successfully
   - UniFFI bindings: ✅ Generated successfully
   - Android APK: ✅ Built successfully (23 MB, 0 errors)

### Technical Details

**Session Start Packet Format**:
```json
{
  "type": "cconnect.digitizer.session",
  "body": {
    "action": "start",
    "width": 1920,
    "height": 1080,
    "resolutionX": 96,
    "resolutionY": 96
  }
}
```

**Session End Packet Format**:
```json
{
  "type": "cconnect.digitizer.session",
  "body": {
    "action": "end"
  }
}
```

**Pen/Stylus Event Packet Format**:
```json
{
  "type": "cconnect.digitizer",
  "body": {
    "active": true,
    "touching": true,
    "tool": "Pen",
    "x": 500,
    "y": 300,
    "pressure": 0.75
  }
}
```

**Supported Features**:
- Session management (start with dimensions/resolution, end)
- Pen events with coordinates, pressure, and tool type
- Rubber/eraser tool support
- Tablet device optimization

**Code Simplification**:
- Removed 17 lines of custom `convertToLegacyPacket()` helper
- Added cleaner FFI wrapper with 2 functions
- Improved maintainability and consistency

### Commits

**cosmic-connect-core**:
- Commit: `a796bc3` - Add Digitizer plugin FFI support

**cosmic-connect-android**:
- Commit: `c40f8060` - Issue #69: Digitizer Plugin FFI Migration

---

**Issue #69 Status**: ✅ COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-17
**Next**: Continue Phase 3 plugin migrations
