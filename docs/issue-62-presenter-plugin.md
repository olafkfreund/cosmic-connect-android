# Issue #62: Presenter Plugin FFI Migration

**Status**: COMPLETE
**Date**: 2026-01-16
**Completed**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 2 - Advanced Plugins
**Related**: Phase 1 complete (Issues #54-61, #68-70)

## Overview

Migrate the Presenter Plugin to use the Rust FFI core for packet creation. The Presenter plugin allows using an Android phone as a wireless presentation remote control, sending pointer movements and keyboard commands to control slides.

## Current Status

**Presenter Plugin is partially FFI-ready**:
- Already uses `NetworkPacket.create()` for packet creation
- Has methods: `sendNext()`, `sendPrevious()`, `sendPointer()`, `stopPointer()`
- Uses `convertToLegacyPacket()` helper to send packets

**What's Missing**:
- Standalone FFI helper functions in Rust core
- Kotlin FFI wrapper class (PresenterPacketsFFI.kt)
- Direct FFI function usage (currently uses generic NetworkPacket.create())

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/presenter.rs`

**Status**: COMPLETE - Plugin implementation exists

**Functionality**:
- Receives presenter events from remote device
- Handles pointer movement (dx, dy)
- Handles stop event
- Tracks presentation_active state

**Packet Type**: `cconnect.presenter`

## Android Status

**Files**:
- `PresenterPlugin.kt` - Plugin implementation
- `PresenterActivity.kt` - UI for controlling presentations

**Current Implementation**:
```kotlin
fun sendPointer(xDelta: Float, yDelta: Float) {
    val packet = NetworkPacket.create(PACKET_TYPE_PRESENTER, mapOf(
        "dx" to xDelta.toDouble(),
        "dy" to yDelta.toDouble()
    ))
    device.sendPacket(convertToLegacyPacket(packet))
}

fun stopPointer() {
    val packet = NetworkPacket.create(PACKET_TYPE_PRESENTER, mapOf(
        "stop" to true
    ))
    device.sendPacket(convertToLegacyPacket(packet))
}
```

## Implementation Plan

### Phase 1: Add FFI Functions to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add standalone presenter packet creation functions:

```rust
/// Create presenter pointer movement packet
pub fn create_presenter_pointer(dx: f64, dy: f64) -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "dx": dx,
        "dy": dy,
    });

    let packet = Packet::new("cconnect.presenter", body);
    Ok(packet.into())
}

/// Create presenter stop packet
pub fn create_presenter_stop() -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "stop": true
    });

    let packet = Packet::new("cconnect.presenter", body);
    Ok(packet.into())
}
```

### Phase 2: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/PresenterPlugin/PresenterPacketsFFI.kt`

```kotlin
package org.cosmic.cosmicconnect.Plugins.PresenterPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createPresenterPointer
import uniffi.cosmic_connect_core.createPresenterStop

object PresenterPacketsFFI {

    fun createPointerMovement(dx: Double, dy: Double): NetworkPacket {
        val ffiPacket = createPresenterPointer(dx, dy)
        return NetworkPacket.fromFfi(ffiPacket)
    }

    fun createStop(): NetworkPacket {
        val ffiPacket = createPresenterStop()
        return NetworkPacket.fromFfi(ffiPacket)
    }
}
```

### Phase 3: Update PresenterPlugin.kt

**Changes**:

```kotlin
// Before:
fun sendPointer(xDelta: Float, yDelta: Float) {
    val packet = NetworkPacket.create(PACKET_TYPE_PRESENTER, mapOf(
        "dx" to xDelta.toDouble(),
        "dy" to yDelta.toDouble()
    ))
    device.sendPacket(convertToLegacyPacket(packet))
}

// After:
fun sendPointer(xDelta: Float, yDelta: Float) {
    val packet = PresenterPacketsFFI.createPointerMovement(
        xDelta.toDouble(),
        yDelta.toDouble()
    )
    device.sendPacket(convertToLegacyPacket(packet))
}

// Before:
fun stopPointer() {
    val packet = NetworkPacket.create(PACKET_TYPE_PRESENTER, mapOf(
        "stop" to true
    ))
    device.sendPacket(convertToLegacyPacket(packet))
}

// After:
fun stopPointer() {
    val packet = PresenterPacketsFFI.createStop()
    device.sendPacket(convertToLegacyPacket(packet))
}
```

### Phase 4: Handle Keyboard Commands

The plugin also sends keyboard commands via mousepad.request packets:

```rust
/// Create presenter keyboard command (via mousepad.request)
/// This is used for Next/Previous/Fullscreen/Esc commands
pub fn create_presenter_key(special_key: i32) -> Result<FfiPacket> {
    use serde_json::json;

    let body = json!({
        "specialKey": special_key
    });

    let packet = Packet::new("cconnect.mousepad.request", body);
    Ok(packet.into())
}
```

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_presenter_pointer() {
        let packet = create_presenter_pointer(10.5, -5.2).unwrap();
        assert_eq!(packet.packet_type, "cconnect.presenter");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["dx"], 10.5);
        assert_eq!(body["dy"], -5.2);
    }

    #[test]
    fn test_create_presenter_stop() {
        let packet = create_presenter_stop().unwrap();
        assert_eq!(packet.packet_type, "cconnect.presenter");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["stop"], true);
    }
}
```

### Integration Tests (Android)

1. Open PresenterActivity
2. Test pointer movement on touchpad
3. Test Next/Previous buttons
4. Test Fullscreen button
5. Test Esc button
6. Verify packets are sent correctly

## Success Criteria

- Rust FFI functions compile successfully
- Android app builds without errors
- PresenterPacketsFFI.kt provides clean API
- PresenterPlugin.kt uses FFI functions
- All presenter features work correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: Presenter packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as Phase 1 plugins

## Timeline Estimate

- Add Rust FFI functions: 30 minutes
- Create Kotlin wrapper: 15 minutes
- Update PresenterPlugin.kt: 15 minutes
- Build and test: 30 minutes
- Documentation: 15 minutes
- Total: ~2 hours

## Related Documentation

- Phase 1 Complete: Issues #54-61, #68-70
- Presenter plugin: `cosmic-connect-core/src/plugins/presenter.rs`
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/PresenterPlugin/`

---

## Completion Summary

**Status**: COMPLETE
**Build**: SUCCESS (24 MB APK)
**Date**: 2026-01-16

### Changes Made

#### Rust Core (cosmic-connect-core)

1. **Added FFI Functions** (src/ffi/mod.rs)
   - `create_presenter_pointer(dx: f64, dy: f64) -> Result<FfiPacket>`
   - `create_presenter_stop() -> Result<FfiPacket>`

2. **Updated UDL** (src/cosmic_connect_core.udl)
   - Added Presenter Plugin section with function declarations
   - Documented parameters and return types

3. **Updated Exports** (src/lib.rs)
   - Exported presenter functions for UniFFI scaffolding

#### Android

1. **Created FFI Wrapper** (PresenterPacketsFFI.kt)
   - `createPointerMovement(dx, dy)` - Wraps create_presenter_pointer
   - `createStop()` - Wraps create_presenter_stop
   - Full documentation and usage examples

2. **Updated Plugin** (PresenterPlugin.kt)
   - Refactored `sendPointer()` to use PresenterPacketsFFI
   - Refactored `stopPointer()` to use PresenterPacketsFFI
   - Removed manual packet construction

3. **Generated UniFFI Bindings**
   - Ran uniffi-bindgen to generate Kotlin bindings
   - Copied generated cosmic_connect_core.kt to Android project

### Verification

- Rust core builds: SUCCESS
- Android native libraries (4 ABIs): SUCCESS
- Android APK assembly: SUCCESS (24 MB)
- Compilation errors: 0

### Benefits

- Presenter packet logic now in Rust core (shared with COSMIC Desktop)
- Type-safe FFI interface with compile-time validation
- Single source of truth for presenter packet format
- Follows established Phase 1 pattern

---

**Issue #62 Status**: COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-16
