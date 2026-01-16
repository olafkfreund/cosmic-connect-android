# Issue #63: SystemVolume Plugin FFI Migration

**Status**: COMPLETE
**Date**: 2026-01-16
**Completed**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 2 - Advanced Plugins
**Related**: Issue #62 (Presenter Plugin) complete

## Overview

Migrate the SystemVolume Plugin to use the Rust FFI core for packet creation. The SystemVolume plugin allows controlling audio sinks (volume, mute, default) on the remote device.

## Current Status

**SystemVolume Plugin uses legacy packet creation**:
- Methods: `sendVolume()`, `sendMute()`, `sendEnable()`, `requestSinkList()`
- Uses `new NetworkPacket(PACKET_TYPE_SYSTEMVOLUME_REQUEST)`
- Creates packets manually with `.set()` calls

**What's Missing**:
- Rust FFI implementation (no systemvolume.rs in Rust core)
- FFI helper functions for packet creation
- Kotlin FFI wrapper class (SystemVolumePacketsFFI.kt)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/systemvolume.rs`

**Status**: DOES NOT EXIST - Needs creation

**Required Functionality**:
- Not a stateful plugin (no state to track)
- Only sends request packets to remote device
- Receives sink list and updates from remote

**Packet Type**: `kdeconnect.systemvolume.request`

## Android Status

**Files**:
- `SystemVolumePlugin.java` - Plugin implementation
- `Sink.java` - Audio sink data class

**Current Implementation**:
```java
void sendVolume(String name, int volume) {
    NetworkPacket packet = new NetworkPacket(PACKET_TYPE_SYSTEMVOLUME_REQUEST);
    packet.set("volume", volume);
    packet.set("name", name);
    getDevice().sendPacket(packet);
}

void sendMute(String name, boolean mute) {
    NetworkPacket packet = new NetworkPacket(PACKET_TYPE_SYSTEMVOLUME_REQUEST);
    packet.set("muted", mute);
    packet.set("name", name);
    getDevice().sendPacket(packet);
}

void sendEnable(String name) {
    NetworkPacket packet = new NetworkPacket(PACKET_TYPE_SYSTEMVOLUME_REQUEST);
    packet.set("enabled", true);
    packet.set("name", name);
    getDevice().sendPacket(packet);
}

void requestSinkList() {
    NetworkPacket packet = new NetworkPacket(PACKET_TYPE_SYSTEMVOLUME_REQUEST);
    packet.set("requestSinks", true);
    getDevice().sendPacket(packet);
}
```

## Implementation Plan

### Phase 1: Add FFI Functions to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add standalone systemvolume packet creation functions:

```rust
/// Create systemvolume set volume request packet
pub fn create_systemvolume_volume(sink_name: String, volume: i32) -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "name": sink_name,
        "volume": volume,
    });

    let packet = Packet::new("kdeconnect.systemvolume.request", body);
    Ok(packet.into())
}

/// Create systemvolume mute request packet
pub fn create_systemvolume_mute(sink_name: String, muted: bool) -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "name": sink_name,
        "muted": muted,
    });

    let packet = Packet::new("kdeconnect.systemvolume.request", body);
    Ok(packet.into())
}

/// Create systemvolume enable (set default) request packet
pub fn create_systemvolume_enable(sink_name: String) -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "name": sink_name,
        "enabled": true,
    });

    let packet = Packet::new("kdeconnect.systemvolume.request", body);
    Ok(packet.into())
}

/// Create systemvolume sink list request packet
pub fn create_systemvolume_request_sinks() -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "requestSinks": true
    });

    let packet = Packet::new("kdeconnect.systemvolume.request", body);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// SystemVolume Plugin

/// Create a systemvolume set volume request packet
[Throws=ProtocolError]
FfiPacket create_systemvolume_volume(string sink_name, i32 volume);

/// Create a systemvolume mute request packet
[Throws=ProtocolError]
FfiPacket create_systemvolume_mute(string sink_name, boolean muted);

/// Create a systemvolume enable (set default) request packet
[Throws=ProtocolError]
FfiPacket create_systemvolume_enable(string sink_name);

/// Create a systemvolume sink list request packet
[Throws=ProtocolError]
FfiPacket create_systemvolume_request_sinks();
```

### Phase 3: Export Functions

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_systemvolume_volume, create_systemvolume_mute,
    create_systemvolume_enable, create_systemvolume_request_sinks,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/SystemVolumePlugin/SystemVolumePacketsFFI.kt`

```kotlin
package org.cosmic.cosmicconnect.Plugins.SystemVolumePlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*

object SystemVolumePacketsFFI {

    fun createVolumeRequest(sinkName: String, volume: Int): NetworkPacket {
        val ffiPacket = createSystemvolumeVolume(sinkName, volume)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    fun createMuteRequest(sinkName: String, muted: Boolean): NetworkPacket {
        val ffiPacket = createSystemvolumeMute(sinkName, muted)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    fun createEnableRequest(sinkName: String): NetworkPacket {
        val ffiPacket = createSystemvolumeEnable(sinkName)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    fun createSinkListRequest(): NetworkPacket {
        val ffiPacket = createSystemvolumeRequestSinks()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update SystemVolumePlugin.java

**Changes**:

```kotlin
// Before:
void sendVolume(String name, int volume) {
    NetworkPacket packet = new NetworkPacket(PACKET_TYPE_SYSTEMVOLUME_REQUEST);
    packet.set("volume", volume);
    packet.set("name", name);
    getDevice().sendPacket(packet);
}

// After:
void sendVolume(String name, int volume) {
    NetworkPacket packet = SystemVolumePacketsFFI.createVolumeRequest(name, volume);
    getDevice().sendPacket(packet);
}

// Similar for sendMute(), sendEnable(), requestSinkList()
```

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_systemvolume_volume() {
        let packet = create_systemvolume_volume("Speaker".to_string(), 75).unwrap();
        assert_eq!(packet.packet_type, "kdeconnect.systemvolume.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["name"], "Speaker");
        assert_eq!(body["volume"], 75);
    }

    #[test]
    fn test_create_systemvolume_mute() {
        let packet = create_systemvolume_mute("Headphones".to_string(), true).unwrap();
        assert_eq!(packet.packet_type, "kdeconnect.systemvolume.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["name"], "Headphones");
        assert_eq!(body["muted"], true);
    }

    #[test]
    fn test_create_systemvolume_enable() {
        let packet = create_systemvolume_enable("HDMI Output".to_string()).unwrap();
        assert_eq!(packet.packet_type, "kdeconnect.systemvolume.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["name"], "HDMI Output");
        assert_eq!(body["enabled"], true);
    }

    #[test]
    fn test_create_systemvolume_request_sinks() {
        let packet = create_systemvolume_request_sinks().unwrap();
        assert_eq!(packet.packet_type, "kdeconnect.systemvolume.request");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["requestSinks"], true);
    }
}
```

### Integration Tests (Android)

1. Test volume control for different sinks
2. Test mute/unmute functionality
3. Test enabling/setting default sink
4. Test requesting sink list
5. Verify packets are sent correctly

## Success Criteria

- Rust FFI functions compile successfully
- Android app builds without errors
- SystemVolumePacketsFFI.kt provides clean API
- SystemVolumePlugin uses FFI functions
- All systemvolume features work correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: SystemVolume packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as Phase 1 and Presenter plugins

## Related Documentation

- Phase 1 Complete: Issues #54-61, #68-70
- Phase 2 Started: Issue #62 (Presenter Plugin)
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/SystemVolumePlugin/`

---

## Completion Summary

**Status**: COMPLETE
**Build**: SUCCESS (24 MB APK)
**Date**: 2026-01-16

### Changes Made

#### Rust Core (cosmic-connect-core)

1. **Added FFI Functions** (src/ffi/mod.rs:1179-1306)
   - `create_systemvolume_volume(sink_name, volume)` - Set volume
   - `create_systemvolume_mute(sink_name, muted)` - Mute/unmute
   - `create_systemvolume_enable(sink_name)` - Set as default
   - `create_systemvolume_request_sinks()` - Request sink list

2. **Updated UDL** (src/cosmic_connect_core.udl:456-496)
   - Added SystemVolume Plugin section with function declarations
   - Documented all parameters and return types

3. **Updated Exports** (src/lib.rs:64-65)
   - Exported all systemvolume functions for UniFFI scaffolding

#### Android

1. **Created FFI Wrapper** (SystemVolumePacketsFFI.kt)
   - `createVolumeRequest(sinkName, volume)` - Wraps create_systemvolume_volume
   - `createMuteRequest(sinkName, muted)` - Wraps create_systemvolume_mute
   - `createEnableRequest(sinkName)` - Wraps create_systemvolume_enable
   - `createSinkListRequest()` - Wraps create_systemvolume_request_sinks
   - Full documentation and usage examples

2. **Updated Plugin** (SystemVolumePlugin.java)
   - Refactored `sendVolume()` to use SystemVolumePacketsFFI
   - Refactored `sendMute()` to use SystemVolumePacketsFFI
   - Refactored `sendEnable()` to use SystemVolumePacketsFFI
   - Refactored `requestSinkList()` to use SystemVolumePacketsFFI
   - Removed all manual packet construction

3. **Generated UniFFI Bindings**
   - Ran uniffi-bindgen to generate Kotlin bindings
   - Copied generated cosmic_connect_core.kt to Android project

### Verification

- Rust core builds: SUCCESS
- Android native libraries (4 ABIs): SUCCESS
- Android APK assembly: SUCCESS (24 MB)
- Compilation errors: 0

### Benefits

- SystemVolume packet logic now in Rust core (shared with COSMIC Desktop)
- Type-safe FFI interface with compile-time validation
- Single source of truth for systemvolume packet format
- Follows established Phase 1 and Phase 2 pattern

---

**Issue #63 Status**: COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-16
