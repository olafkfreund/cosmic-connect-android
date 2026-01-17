# Issue #70: SFTP Plugin FFI Migration

**Status**: 🔄 IN PROGRESS
**Date**: 2026-01-17
**Priority**: MEDIUM
**Phase**: Phase 3 - Remaining Plugins
**Related**: Issues #67-69 complete

## Overview

Migrate the SFTP Plugin to use dedicated Rust FFI functions for packet creation. The SFTP plugin provides file system access over SSH File Transfer Protocol, allowing the desktop to browse and access files on the Android device.

## Current Status

**SFTP Plugin uses generic NetworkPacket.create()**:
- Already in Kotlin
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for packet construction (lines 85, 112, 148, 233)
- Has `convertToLegacyPacket()` method that just delegates to `toLegacyPacket()` (line 271)
- Sends SFTP connection details or error messages

**What's Missing**:
- Dedicated Rust FFI function for SFTP packets
- Kotlin FFI wrapper class (SftpPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated SFTP plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create SFTP packets with server connection details or error messages
- Single packet type with variable body

**Packet Type**: `cosmicconnect.sftp`

## Android Status

**Files**:
- `SftpPlugin.kt` - Plugin implementation (already in Kotlin)
- `SimpleSftpServer.kt` - SFTP server implementation
- `SftpSettingsFragment.java` - Settings UI

**Current Implementation**:

**Error Response (Lines 85-88, 112-115)**:
```kotlin
val packet = NetworkPacket.create(PACKET_TYPE_SFTP, mapOf(
    "errorMessage" to context.getString(R.string.sftp_missing_permission_error)
))
device.sendPacket(convertToLegacyPacket(packet))
```

**SFTP Server Info (Lines 132-149)**:
```kotlin
val body = mutableMapOf<String, Any>(
    "ip" to localIpAddress!!.hostAddress!!,
    "port" to server.port,
    "user" to SimpleSftpServer.USER,
    "password" to server.regeneratePassword(),
    "path" to if (paths.size == 1) paths[0] else "/"
)

// Add optional multiPaths fields
if (paths.isNotEmpty()) {
    body["multiPaths"] = paths
    body["pathNames"] = pathNames
}

val packet = NetworkPacket.create(PACKET_TYPE_SFTP, body.toMap())
device.sendPacket(convertToLegacyPacket(packet))
```

## Implementation Plan

### Phase 1: Add FFI Function to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add SFTP packet creation function:

```rust
/// Create SFTP packet
pub fn create_sftp_packet(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cosmicconnect.sftp", body_data);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// SFTP Plugin

/// Create an SFTP packet
///
/// Creates a packet for SFTP server connection details or error messages.
///
/// # Arguments
///
/// * `body_json` - JSON string containing SFTP server info or error message
[Throws=ProtocolError]
FfiPacket create_sftp_packet(string body_json);
```

### Phase 3: Export Function

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_sftp_packet,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/SftpPlugin/SftpPacketsFFI.kt`

```kotlin
package org.cosmic.cosmicconnect.Plugins.SftpPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createSftpPacket

/**
 * FFI wrapper for creating SFTP plugin packets
 *
 * The SFTP plugin provides file system access over SSH File Transfer Protocol,
 * allowing the desktop to browse and access files on the Android device.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * @see SftpPlugin
 */
object SftpPacketsFFI {

    /**
     * Create an SFTP packet
     *
     * Creates a packet containing SFTP server connection details or
     * an error message.
     *
     * For server connection info, the body should contain:
     * - `ip`: Server IP address
     * - `port`: Server port number
     * - `user`: Username for authentication
     * - `password`: Password for authentication
     * - `path`: Root path (for single path)
     * - `multiPaths`: Array of paths (optional, for multiple mount points)
     * - `pathNames`: Array of path display names (optional)
     *
     * For error messages, the body should contain:
     * - `errorMessage`: Error description string
     *
     * @param bodyJson JSON string containing SFTP data
     * @return NetworkPacket ready to send
     */
    fun createSftpPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createSftpPacket(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update SftpPlugin.kt

**Add import**:
```kotlin
import org.json.JSONObject  // Already present
```

**Changes to error responses (Lines 85-88, 112-115)**:
```kotlin
// Before:
val packet = NetworkPacket.create(PACKET_TYPE_SFTP, mapOf(
    "errorMessage" to context.getString(R.string.sftp_missing_permission_error)
))
device.sendPacket(convertToLegacyPacket(packet))

// After:
val json = JSONObject(mapOf(
    "errorMessage" to context.getString(R.string.sftp_missing_permission_error)
)).toString()
val packet = SftpPacketsFFI.createSftpPacket(json)
device.sendPacket(packet.toLegacyPacket())
```

**Changes to server info (Lines 132-149)**:
```kotlin
// Before:
val packet = NetworkPacket.create(PACKET_TYPE_SFTP, body.toMap())
device.sendPacket(convertToLegacyPacket(packet))

// After:
val json = JSONObject(body).toString()
val packet = SftpPacketsFFI.createSftpPacket(json)
device.sendPacket(packet.toLegacyPacket())
```

**Remove** `convertToLegacyPacket()` method (Lines 269-272) - no longer needed

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_sftp_packet_server_info() {
        let body_json = json!({
            "ip": "192.168.1.100",
            "port": 1739,
            "user": "sftpuser",
            "password": "secret123",
            "path": "/storage/emulated/0",
            "multiPaths": ["/storage/emulated/0", "/storage/sdcard1"],
            "pathNames": ["Internal Storage", "SD Card"]
        }).to_string();

        let packet = create_sftp_packet(body_json).unwrap();
        assert_eq!(packet.packet_type, "cosmicconnect.sftp");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["ip"], "192.168.1.100");
        assert_eq!(body["port"], 1739);
    }

    #[test]
    fn test_create_sftp_packet_error() {
        let body_json = json!({
            "errorMessage": "Permission denied"
        }).to_string();

        let packet = create_sftp_packet(body_json).unwrap();
        assert_eq!(packet.packet_type, "cosmicconnect.sftp");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["errorMessage"], "Permission denied");
    }
}
```

### Integration Tests (Android)

1. Test SFTP server startup with connection details
2. Test error message packets (permission denied, no storage)
3. Test multi-path configuration
4. Verify packet format correctness

## Success Criteria

- Rust FFI function compiles successfully
- Android app builds without errors
- SftpPacketsFFI.kt provides clean API
- SftpPlugin uses FFI function
- SFTP file browsing works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: SFTP packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as other migrated plugins
5. **Simplified Code**: Removes delegating `convertToLegacyPacket()` method

## Related Documentation

- Phase 2 Complete: Issues #62-66
- Phase 3: Issues #67-69 complete
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/SftpPlugin/`

---

## Status Updates

**2026-01-17**: Issue created, survey completed
