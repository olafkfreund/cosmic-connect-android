# Issue #64: ConnectivityReport Plugin FFI Migration

**Status**: ✅ COMPLETE
**Date**: 2026-01-16
**Priority**: LOW
**Phase**: Phase 2 - Advanced Plugins
**Related**: Issues #62, #63 complete

## Overview

Migrate the ConnectivityReport Plugin to use dedicated Rust FFI functions for packet creation. The ConnectivityReport plugin reports network connectivity state (network type and signal strength) for cellular connections.

## Current Status

**ConnectivityReport Plugin uses generic NetworkPacket.create()**:
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for packet construction
- Sends connectivity reports with signal strength data

**What's Missing**:
- Dedicated Rust FFI function for connectivity report packets
- Kotlin FFI wrapper class (ConnectivityPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated connectivity plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create connectivity report packets with signal strengths
- Single packet type for reporting state

**Packet Type**: `cconnect.connectivity_report`

## Android Status

**Files**:
- `ConnectivityReportPlugin.kt` - Plugin implementation (already in Kotlin)
- `ConnectivityListener.kt` - Monitors network state changes

**Current Implementation**:
```kotlin
val signalStrengths = JSONObject()
states.forEach { (subID, subscriptionState) ->
    val subInfo = JSONObject()
    subInfo.put("networkType", subscriptionState.networkType)
    subInfo.put("signalStrength", subscriptionState.signalStrength)
    signalStrengths.put(subID.toString(), subInfo)
}

val packet = NetworkPacket.create(
    PACKET_TYPE_CONNECTIVITY_REPORT,
    mapOf("signalStrengths" to signalStrengths)
)
device.sendPacket(convertToLegacyPacket(packet))
```

## Implementation Plan

### Phase 1: Add FFI Function to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add standalone connectivity report packet creation function:

```rust
/// Create connectivity report packet
pub fn create_connectivity_report(signal_strengths_json: String) -> Result<FfiPacket> {
    use serde_json::json;

    // Parse the signal strengths JSON
    let signal_strengths: serde_json::Value = serde_json::from_str(&signal_strengths_json)
        .map_err(|e| ProtocolError::Json(e.to_string()))?;

    let body = json!({
        "signalStrengths": signal_strengths
    });

    let packet = Packet::new("cconnect.connectivity_report", body);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// ConnectivityReport Plugin

/// Create a connectivity report packet
///
/// Creates a packet containing network connectivity state information.
///
/// # Arguments
///
/// * `signal_strengths_json` - JSON string containing subscription states
[Throws=ProtocolError]
FfiPacket create_connectivity_report(string signal_strengths_json);
```

### Phase 3: Export Function

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_connectivity_report,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/ConnectivityReportPlugin/ConnectivityPacketsFFI.kt`

```kotlin
package org.cosmic.cconnect.Plugins.ConnectivityReportPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createConnectivityReport

object ConnectivityPacketsFFI {

    fun createConnectivityReport(signalStrengthsJson: String): NetworkPacket {
        val ffiPacket = createConnectivityReport(signalStrengthsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update ConnectivityReportPlugin.kt

**Changes**:

```kotlin
// Before:
val packet = NetworkPacket.create(
    PACKET_TYPE_CONNECTIVITY_REPORT,
    mapOf("signalStrengths" to signalStrengths)
)

// After:
val packet = ConnectivityPacketsFFI.createConnectivityReport(
    signalStrengths.toString()
)
```

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_connectivity_report() {
        let signal_json = json!({
            "6": {
                "networkType": "4G",
                "signalStrength": 3
            }
        }).to_string();

        let packet = create_connectivity_report(signal_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.connectivity_report");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert!(body["signalStrengths"].is_object());
    }
}
```

### Integration Tests (Android)

1. Test connectivity state reporting
2. Verify signal strength updates
3. Test multiple subscription states
4. Verify packet format correctness

## Success Criteria

- Rust FFI function compiles successfully
- Android app builds without errors
- ConnectivityPacketsFFI.kt provides clean API
- ConnectivityReportPlugin uses FFI function
- Connectivity reporting works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: Connectivity packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as other Phase 2 plugins

## Related Documentation

- Phase 1 Complete: Issues #54-61, #68-70
- Phase 2: Issues #62 (Presenter), #63 (SystemVolume)
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/ConnectivityReportPlugin/`

---

## ✅ Completion Summary

**Status**: COMPLETE
**Date**: 2026-01-16
**Build**: SUCCESS (24 MB APK, 0 errors)

### What Was Done

1. **Rust Core Changes** (`cosmic-connect-core`):
   - Added `create_connectivity_report()` to `src/ffi/mod.rs`
   - Accepts JSON string for signal strengths
   - Parses and embeds in packet body
   - Added UDL declaration for UniFFI bindings
   - Exported function in `src/lib.rs`

2. **Android Changes** (`cosmic-connect-android`):
   - Created `ConnectivityPacketsFFI.kt` wrapper
   - Updated `ConnectivityReportPlugin.kt` to use FFI
   - Removed manual packet creation code
   - Fixed function naming conflict with fully qualified uniffi call

3. **Build Results**:
   - Rust core: ✅ Compiled successfully
   - UniFFI bindings: ✅ Generated successfully
   - Android APK: ✅ Built successfully (24 MB, 0 errors)

### Technical Details

**Packet Format**:
```json
{
  "type": "cconnect.connectivity_report",
  "body": {
    "signalStrengths": {
      "6": {
        "networkType": "4G",
        "signalStrength": 3
      }
    }
  }
}
```

**Function Naming Fix**:
- Issue: Kotlin wrapper function had same name as uniffi import
- Solution: Used fully qualified call `uniffi.cosmic_connect_core.createConnectivityReport()`
- Pattern: Prevents recursive call, matches SystemVolume plugin approach

### Commits

**cosmic-connect-core**:
- Commit: `77e5098` - Add ConnectivityReport plugin FFI support

**cosmic-connect-android**:
- Commit: `049b551d` - Issue #64: ConnectivityReport Plugin FFI Migration

---

**Issue #64 Status**: ✅ COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-16
**Next**: Continue Phase 2 plugin migrations
