# Issue #71: MprisReceiver Plugin FFI Migration

**Status**: 🔄 IN PROGRESS
**Date**: 2026-01-17
**Priority**: MEDIUM
**Phase**: Phase 3 - Remaining Plugins
**Related**: Issues #67-70 complete

## Overview

Migrate the MprisReceiver Plugin to use dedicated Rust FFI functions for packet creation. The MprisReceiver plugin receives media playback control commands from the desktop and reports current media state from Android media players.

## Current Status

**MprisReceiver Plugin uses legacy NetworkPacket constructor**:
- Still in Java
- Uses legacy mutable NetworkPacket API
- Creates packets with `new NetworkPacket(PACKET_TYPE_MPRIS)`
- Three packet creation locations:
  1. **sendPlayerList()** (lines 225-233): Player list with album art support flag
  2. **sendAlbumArt()** (lines 259-268): Album art transfer with payload
  3. **sendMetadata()** (lines 283-303): Extensive media metadata

**What's Missing**:
- Dedicated Rust FFI function for MPRIS packets
- Kotlin FFI wrapper class (MprisReceiverPacketsFFI.kt)
- Direct FFI function usage (currently uses legacy constructor)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated MprisReceiver plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create MPRIS packets with player list
- Create MPRIS packets with metadata
- Create MPRIS packets with album art transfer info

**Packet Type**: `cosmicconnect.mpris`

## Android Status

**Files**:
- `MprisReceiverPlugin.java` - Plugin implementation (Java)
- `MprisReceiverPlayer.java` - Media player wrapper
- `MprisReceiverCallback.java` - Media session callback

**Current Implementation**:

**Player List Packet (Lines 225-233)**:
```java
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MPRIS);
packet.set("playerList", new ArrayList<>(players.keySet()));
packet.set("supportAlbumArtPayload", true);
getDevice().sendPacket(packet);
```

**Album Art Packet (Lines 259-268)**:
```java
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MPRIS);
packet.set("player", playerName);
packet.set("transferringAlbumArt", true);
packet.set("albumArtUrl", artUrl);
packet.setPayload(new NetworkPacket.Payload(p));
getDevice().sendPacket(packet);
```

**Metadata Packet (Lines 283-303)**:
```java
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MPRIS);
packet.set("player", player.getName());
packet.set("title", player.getTitle());
packet.set("artist", player.getArtist());
packet.set("nowPlaying", nowPlaying);
packet.set("album", player.getAlbum());
packet.set("isPlaying", player.isPlaying());
packet.set("pos", player.getPosition());
packet.set("length", player.getLength());
packet.set("canPlay", player.canPlay());
packet.set("canPause", player.canPause());
packet.set("canGoPrevious", player.canGoPrevious());
packet.set("canGoNext", player.canGoNext());
packet.set("canSeek", player.canSeek());
packet.set("volume", player.getVolume());
packet.set("albumArtUrl", artUrl);
getDevice().sendPacket(packet);
```

## Implementation Plan

### Phase 1: Add FFI Function to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add MPRIS packet creation function:

```rust
/// Create MPRIS packet
pub fn create_mpris_request(body_json: String) -> Result<FfiPacket> {
    // Parse the request body JSON
    let body_data: serde_json::Value = serde_json::from_str(&body_json)?;

    let packet = Packet::new("cosmicconnect.mpris", body_data);
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// MPRIS Receiver Plugin

/// Create an MPRIS packet
///
/// Creates a packet for media player control and metadata reporting.
///
/// # Arguments
///
/// * `body_json` - JSON string containing MPRIS data
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

**File**: `src/org/cosmic/cosmicconnect/Plugins/MprisReceiverPlugin/MprisReceiverPacketsFFI.kt`

```kotlin
package org.cosmic.cosmicconnect.Plugins.MprisReceiverPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMprisRequest

/**
 * FFI wrapper for creating MPRIS receiver plugin packets
 *
 * The MPRIS receiver plugin receives media playback control commands from
 * the desktop and reports current media state from Android media players.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * @see MprisReceiverPlugin
 */
object MprisReceiverPacketsFFI {

    /**
     * Create an MPRIS packet
     *
     * Creates a packet containing media player information, metadata,
     * or album art transfer details.
     *
     * @param bodyJson JSON string containing MPRIS data
     * @return NetworkPacket ready to send
     */
    fun createMprisPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMprisRequest(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update MprisReceiverPlugin.java

**Add imports**:
```java
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
```

**Changes to sendPlayerList() (Lines 225-233)**:
```java
// Before:
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MPRIS);
packet.set("playerList", new ArrayList<>(players.keySet()));
packet.set("supportAlbumArtPayload", true);
getDevice().sendPacket(packet);

// After:
Map<String, Object> body = new HashMap<>();
body.put("playerList", new ArrayList<>(players.keySet()));
body.put("supportAlbumArtPayload", true);
String json = new JSONObject(body).toString();
org.cosmic.cosmicconnect.Core.NetworkPacket packet = MprisReceiverPacketsFFI.INSTANCE.createMprisPacket(json);
getDevice().sendPacket(packet.toLegacyPacket());
```

**Changes to sendAlbumArt() (Lines 259-268)**:
```java
// Before:
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MPRIS);
packet.set("player", playerName);
packet.set("transferringAlbumArt", true);
packet.set("albumArtUrl", artUrl);
packet.setPayload(new NetworkPacket.Payload(p));
getDevice().sendPacket(packet);

// After:
Map<String, Object> body = new HashMap<>();
body.put("player", playerName);
body.put("transferringAlbumArt", true);
body.put("albumArtUrl", artUrl);
String json = new JSONObject(body).toString();
org.cosmic.cosmicconnect.Core.NetworkPacket packet = MprisReceiverPacketsFFI.INSTANCE.createMprisPacket(json);
org.cosmic.cosmicconnect.NetworkPacket legacyPacket = packet.toLegacyPacket();
legacyPacket.setPayload(new org.cosmic.cosmicconnect.NetworkPacket.Payload(p));
getDevice().sendPacket(legacyPacket);
```

**Changes to sendMetadata() (Lines 283-303)**:
```java
// Before:
NetworkPacket packet = new NetworkPacket(PACKET_TYPE_MPRIS);
packet.set("player", player.getName());
packet.set("title", player.getTitle());
// ... many more fields ...
getDevice().sendPacket(packet);

// After:
Map<String, Object> body = new HashMap<>();
body.put("player", player.getName());
body.put("title", player.getTitle());
body.put("artist", player.getArtist());
body.put("nowPlaying", nowPlaying);
body.put("album", player.getAlbum());
body.put("isPlaying", player.isPlaying());
body.put("pos", player.getPosition());
body.put("length", player.getLength());
body.put("canPlay", player.canPlay());
body.put("canPause", player.canPause());
body.put("canGoPrevious", player.canGoPrevious());
body.put("canGoNext", player.canGoNext());
body.put("canSeek", player.canSeek());
body.put("volume", player.getVolume());
body.put("albumArtUrl", artUrl);
String json = new JSONObject(body).toString();
org.cosmic.cosmicconnect.Core.NetworkPacket packet = MprisReceiverPacketsFFI.INSTANCE.createMprisPacket(json);
getDevice().sendPacket(packet.toLegacyPacket());
```

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_mpris_request_player_list() {
        let body_json = json!({
            "playerList": ["Spotify", "YouTube Music"],
            "supportAlbumArtPayload": true
        }).to_string();

        let packet = create_mpris_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cosmicconnect.mpris");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["supportAlbumArtPayload"], true);
    }

    #[test]
    fn test_create_mpris_request_metadata() {
        let body_json = json!({
            "player": "Spotify",
            "title": "Test Song",
            "artist": "Test Artist",
            "isPlaying": true,
            "pos": 12345,
            "length": 180000
        }).to_string();

        let packet = create_mpris_request(body_json).unwrap();
        assert_eq!(packet.packet_type, "cosmicconnect.mpris");

        let body: serde_json::Value = serde_json::from_str(&packet.body).unwrap();
        assert_eq!(body["player"], "Spotify");
        assert_eq!(body["title"], "Test Song");
    }
}
```

### Integration Tests (Android)

1. Test player list packet creation
2. Test metadata packet creation with all fields
3. Test album art transfer packet creation
4. Test media control commands
5. Verify packet format correctness

## Success Criteria

- Rust FFI function compiles successfully
- Android app builds without errors
- MprisReceiverPacketsFFI.kt provides clean API
- MprisReceiverPlugin uses FFI function
- Media control and reporting work correctly
- Album art transfer works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: MPRIS packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as other migrated plugins
5. **Protocol Compliance**: Ensures consistent MPRIS implementation

## Related Documentation

- Phase 2 Complete: Issues #62-66
- Phase 3: Issues #67-70 complete
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/MprisReceiverPlugin/`

---

## Status Updates

**2026-01-17**: Issue created, survey completed
