# Issue #66: MPRIS Plugin FFI Migration

**Status**: ✅ COMPLETE
**Date**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 2 - Advanced Plugins
**Related**: Issues #62, #63, #64, #65 complete

## Overview

Migrate the MPRIS Plugin to use dedicated Rust FFI functions for packet creation. The MPRIS plugin enables media control integration, allowing the desktop to control Android media playback (music, videos, podcasts).

## Current Status

**MPRIS Plugin uses generic NetworkPacket.create()**:
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for packet construction
- Has custom `convertToLegacyPacket()` helper method
- Sends control commands (play, pause, next, volume, etc.)

**What's Missing**:
- Dedicated Rust FFI function for MPRIS request packets
- Kotlin FFI wrapper class (MprisPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated MPRIS plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create MPRIS request packets with player name and command
- Single packet type for all control commands
- Variable body based on command type

**Packet Type**: `cconnect.mpris.request`

## Android Status

**Files**:
- `MprisPlugin.kt` - Plugin implementation (already in Kotlin)
- `MprisMediaSession.kt` - Media session integration
- `AlbumArtCache.kt` - Album art caching

**Current Implementation (Line 474)**:
```kotlin
private fun sendMprisPacket(body: Map<String, Any>) {
    // Create immutable packet
    val packet = NetworkPacket.create(PACKET_TYPE_MPRIS_REQUEST, body)

    // Convert and send
    device.sendPacket(convertToLegacyPacket(packet))
}

// Called by various send commands:
sendCommand(playerName, "action", "PlayPause")
sendCommand(playerName, "action", "Play")
sendCommand(playerName, "setVolume", 50)
sendCommand(playerName, "SetPosition", 12345)
```

## Implementation Plan

### Phase 1: Add FFI Function to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add standalone MPRIS request packet creation function:

```rust
/// Create MPRIS request packet
pub fn create_mpris_request(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cconnect.mpris.request", body_data);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// MPRIS Plugin

/// Create an MPRIS request packet
///
/// Creates a packet for controlling media playback on the remote device.
///
/// # Arguments
///
/// * `body_json` - JSON string containing player name and command
[Throws=ProtocolError]
FfiPacket create_mpris_request(string body_json);
```

### Phase 3: Export Function

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_mpris_request,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/MprisPlugin/MprisPacketsFFI.kt`

```kotlin
package org.cosmic.cconnect.Plugins.MprisPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMprisRequest

object MprisPacketsFFI {

    fun createMprisRequest(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMprisRequest(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update MprisPlugin.kt

**Changes to sendMprisPacket() (Line 474)**:

```kotlin
// Before:
private fun sendMprisPacket(body: Map<String, Any>) {
    val packet = NetworkPacket.create(PACKET_TYPE_MPRIS_REQUEST, body)
    device.sendPacket(convertToLegacyPacket(packet))
}

// After:
import org.json.JSONObject

private fun sendMprisPacket(body: Map<String, Any>) {
    val json = JSONObject(body).toString()
    val packet = MprisPacketsFFI.createMprisRequest(json)
    device.sendPacket(packet.toLegacyPacket())
}
```

**Remove** `convertToLegacyPacket()` method (Lines 483-499) - no longer needed

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_mpris_request_playpause() {
        let body_json = json!({
            "player": "spotify",
            "action": "PlayPause"
        }).to_string();

        let packet = create_mpris_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.mpris.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["player"], "spotify");
        assert_eq!(body["action"], "PlayPause");
    }

    #[test]
    fn test_create_mpris_request_volume() {
        let body_json = json!({
            "player": "vlc",
            "setVolume": 75
        }).to_string();

        let packet = create_mpris_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.mpris.request");
    }
}
```

### Integration Tests (Android)

1. Test media control commands (play, pause, next, previous)
2. Test volume control
3. Test seek operations
4. Test loop status changes
5. Test shuffle toggling
6. Verify packet format correctness

## Success Criteria

- Rust FFI function compiles successfully
- Android app builds without errors
- MprisPacketsFFI.kt provides clean API
- MprisPlugin uses FFI function
- Media control works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: MPRIS packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as other Phase 2 plugins
5. **Simplified Code**: Removes custom `convertToLegacyPacket()` helper

## Related Documentation

- Phase 1 Complete: Issues #54-61, #68-70
- Phase 2: Issues #62 (Presenter), #63 (SystemVolume), #64 (ConnectivityReport), #65 (Contacts)
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/MprisPlugin/`

---

## ✅ Completion Summary

**Status**: COMPLETE
**Date**: 2026-01-16
**Build**: SUCCESS (24 MB APK, 0 errors)

### What Was Done

1. **Rust Core Changes** (`cosmic-connect-core`):
   - Added `create_mpris_request()` to `src/ffi/mod.rs`
   - Accepts JSON string for player name and command
   - Parses and embeds in packet body
   - Added UDL declaration for UniFFI bindings
   - Exported function in `src/lib.rs`

2. **Android Changes** (`cosmic-connect-android`):
   - Created `MprisPacketsFFI.kt` wrapper with comprehensive documentation
   - Updated `MprisPlugin.kt` to use FFI wrapper
   - Removed custom `convertToLegacyPacket()` helper method (17 lines removed)
   - Added JSONObject for proper JSON serialization

3. **Build Results**:
   - Rust core: ✅ Compiled successfully
   - UniFFI bindings: ✅ Generated successfully
   - Android APK: ✅ Built successfully (24 MB, 0 errors)

### Technical Details

**Packet Format**:
```json
{
  "type": "cconnect.mpris.request",
  "body": {
    "player": "spotify",
    "action": "PlayPause"
  }
}
```

**Supported Commands**:
- Playback control: `action: "Play"`, `action: "Pause"`, `action: "Next"`, `action: "Previous"`
- Volume control: `setVolume: 75`
- Seeking: `SetPosition: 123000`, `Seek: 5000`
- Playback modes: `setLoopStatus: "Track"`, `setShuffle: true`

**Code Simplification**:
- Removed 17 lines of custom `convertToLegacyPacket()` helper
- Added cleaner FFI wrapper with 1 function
- Improved maintainability and consistency

### Commits

**cosmic-connect-core**:
- Commit: `70be47f` - Add MPRIS plugin FFI support

**cosmic-connect-android**:
- Commit: `c6f34abd` - Issue #66: MPRIS Plugin FFI Migration

---

**Issue #66 Status**: ✅ COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-16
**Next**: Continue Phase 2 plugin migrations (COMPLETE!)
