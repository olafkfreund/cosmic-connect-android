# Issue #65: Contacts Plugin FFI Migration

**Status**: ✅ COMPLETE
**Date**: 2026-01-16
**Priority**: MEDIUM
**Phase**: Phase 2 - Advanced Plugins
**Related**: Issues #62, #63, #64 complete

## Overview

Migrate the Contacts Plugin to use dedicated Rust FFI functions for packet creation. The Contacts plugin enables sharing contact information (vCards) between devices, allowing desktop to access the phone's contact list.

## Current Status

**Contacts Plugin uses generic NetworkPacket.create()**:
- Already uses immutable NetworkPacket API
- Uses `NetworkPacket.create()` for packet construction
- Has custom `convertToLegacyPacket()` helper method
- Sends two types of responses: UIDs/timestamps and vCards

**What's Missing**:
- Dedicated Rust FFI functions for contacts response packets
- Kotlin FFI wrapper class (ContactsPacketsFFI.kt)
- Direct FFI function usage (currently uses generic create)

## Rust Core Status

**File**: `cosmic-connect-core/src/plugins/` - No dedicated contacts plugin

**Status**: No existing Rust implementation

**Required Functionality**:
- Create contacts response with UIDs and timestamps
- Create contacts response with vCards
- Two packet types for bidirectional sync

**Packet Types**:
- `cconnect.contacts.response_uids_timestamps`
- `cconnect.contacts.response_vcards`

## Android Status

**Files**:
- `ContactsPlugin.kt` - Plugin implementation (already in Kotlin)

**Current Implementation (Line 124)**:
```kotlin
// Build packet body for UIDs/timestamps
val body = mutableMapOf<String, Any>()
val uIDsAsString = mutableListOf<String>()
for ((contactID: uID, timestamp: Long) in uIDsToTimestamps) {
    body[contactID.toString()] = timestamp.toString()
    uIDsAsString.add(contactID.toString())
}
body[PACKET_UIDS_KEY] = uIDsAsString

val packet = NetworkPacket.create(
    PACKET_TYPE_CONTACTS_RESPONSE_UIDS_TIMESTAMPS,
    body.toMap()
)
device.sendPacket(convertToLegacyPacket(packet))
```

**Current Implementation (Line 164)**:
```kotlin
// Build packet body for vCards
val body = mutableMapOf<String, Any>()
val uIDsAsStrings = mutableListOf<String>()
for ((uID: uID, vcard: VCardBuilder) in uIDsToVCards) {
    val vcardWithMetadata = addVCardMetadata(vcard, uID)
    uIDsAsStrings.add(uID.toString())
    body[uID.toString()] = vcardWithMetadata.toString()
}
body[PACKET_UIDS_KEY] = uIDsAsStrings

val packet = NetworkPacket.create(
    PACKET_TYPE_CONTACTS_RESPONSE_VCARDS,
    body.toMap()
)
device.sendPacket(convertToLegacyPacket(packet))
```

## Implementation Plan

### Phase 1: Add FFI Functions to Rust Core

**File**: `cosmic-connect-core/src/ffi/mod.rs`

Add two standalone contacts packet creation functions:

```rust
/// Create contacts response with UIDs and timestamps
pub fn create_contacts_response_uids(uids_json: String) -> Result<FfiPacket> {
    use serde_json::json;

    // Parse the UIDs/timestamps JSON
    let uids_data: serde_json::Value = serde_json::from_str(&uids_json)?;

    let packet = Packet::new(
        "cconnect.contacts.response_uids_timestamps",
        uids_data
    );
    Ok(packet.into())
}

/// Create contacts response with vCards
pub fn create_contacts_response_vcards(vcards_json: String) -> Result<FfiPacket> {
    use serde_json::json;

    // Parse the vCards JSON
    let vcards_data: serde_json::Value = serde_json::from_str(&vcards_json)?;

    let packet = Packet::new(
        "cconnect.contacts.response_vcards",
        vcards_data
    );
    Ok(packet.into())
}
```

### Phase 2: Update UDL

**File**: `cosmic-connect-core/src/cosmic_connect_core.udl`

```udl
// Contacts Plugin

/// Create a contacts response packet with UIDs and timestamps
///
/// Creates a packet containing contact unique IDs and their last-modified timestamps.
///
/// # Arguments
///
/// * `uids_json` - JSON string containing UIDs and timestamps
[Throws=ProtocolError]
FfiPacket create_contacts_response_uids(string uids_json);

/// Create a contacts response packet with vCards
///
/// Creates a packet containing full vCard data for requested contacts.
///
/// # Arguments
///
/// * `vcards_json` - JSON string containing UIDs and vCard data
[Throws=ProtocolError]
FfiPacket create_contacts_response_vcards(string vcards_json);
```

### Phase 3: Export Functions

**File**: `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_contacts_response_uids,
    create_contacts_response_vcards,
};
```

### Phase 4: Create Kotlin FFI Wrapper

**File**: `src/org/cosmic/cosmicconnect/Plugins/ContactsPlugin/ContactsPacketsFFI.kt`

```kotlin
package org.cosmic.cconnect.Plugins.ContactsPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createContactsResponseUids
import uniffi.cosmic_connect_core.createContactsResponseVcards

object ContactsPacketsFFI {

    fun createUidsTimestampsResponse(uidsJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createContactsResponseUids(uidsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    fun createVCardsResponse(vcardsJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createContactsResponseVcards(vcardsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
```

### Phase 5: Update ContactsPlugin.kt

**Changes to handleRequestAllUIDsTimestamps() (Line 124)**:

```kotlin
// Before:
val packet = NetworkPacket.create(
    PACKET_TYPE_CONTACTS_RESPONSE_UIDS_TIMESTAMPS,
    body.toMap()
)
device.sendPacket(convertToLegacyPacket(packet))

// After:
import org.json.JSONObject
val json = JSONObject(body).toString()
val packet = ContactsPacketsFFI.createUidsTimestampsResponse(json)
device.sendPacket(packet.toLegacyPacket())
```

**Changes to handleRequestVCardsByUIDs() (Line 164)**:

```kotlin
// Before:
val packet = NetworkPacket.create(
    PACKET_TYPE_CONTACTS_RESPONSE_VCARDS,
    body.toMap()
)
device.sendPacket(convertToLegacyPacket(packet))

// After:
import org.json.JSONObject
val json = JSONObject(body).toString()
val packet = ContactsPacketsFFI.createVCardsResponse(json)
device.sendPacket(packet.toLegacyPacket())
```

**Remove** `convertToLegacyPacket()` method (Lines 184-200) - no longer needed

## Testing Plan

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_create_contacts_response_uids() {
        let uids_json = json!({
            "uids": ["1", "3", "15"],
            "1": "1234567890",
            "3": "1234567891",
            "15": "1234567892"
        }).to_string();

        let packet = create_contacts_response_uids(uids_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.contacts.response_uids_timestamps");
    }

    #[test]
    fn test_create_contacts_response_vcards() {
        let vcards_json = json!({
            "uids": ["1"],
            "1": "BEGIN:VCARD\nFN:John Smith\nEND:VCARD"
        }).to_string();

        let packet = create_contacts_response_vcards(vcards_json).unwrap();
        assert_eq!(packet.packet_type, "cconnect.contacts.response_vcards");
    }
}
```

### Integration Tests (Android)

1. Test requesting all UIDs/timestamps
2. Test requesting vCards by UIDs
3. Verify vCard metadata (device ID, timestamp)
4. Test handling of missing contacts
5. Verify packet format correctness

## Success Criteria

- Rust FFI functions compile successfully
- Android app builds without errors
- ContactsPacketsFFI.kt provides clean API
- ContactsPlugin uses FFI functions
- Contact sync works correctly
- No regression in existing functionality

## Benefits

1. **Code Reuse**: Contacts packet logic in Rust core, shared with desktop
2. **Type Safety**: FFI provides compile-time validation
3. **Maintainability**: Single source of truth for packet format
4. **Consistency**: Follows same pattern as other Phase 2 plugins
5. **Simplified Code**: Removes custom `convertToLegacyPacket()` helper

## Related Documentation

- Phase 1 Complete: Issues #54-61, #68-70
- Phase 2: Issues #62 (Presenter), #63 (SystemVolume), #64 (ConnectivityReport)
- Android plugin: `src/org/cosmic/cosmicconnect/Plugins/ContactsPlugin/`

---

## ✅ Completion Summary

**Status**: COMPLETE
**Date**: 2026-01-16
**Build**: SUCCESS (24 MB APK, 0 errors)

### What Was Done

1. **Rust Core Changes** (`cosmic-connect-core`):
   - Added `create_contacts_response_uids()` to `src/ffi/mod.rs`
   - Added `create_contacts_response_vcards()` to `src/ffi/mod.rs`
   - Accepts JSON strings for UIDs/timestamps and vCard data
   - Parses and embeds in packet body
   - Added UDL declarations for UniFFI bindings
   - Exported functions in `src/lib.rs`

2. **Android Changes** (`cosmic-connect-android`):
   - Created `ContactsPacketsFFI.kt` wrapper with comprehensive documentation
   - Updated `ContactsPlugin.kt` to use FFI for both response types
   - Removed custom `convertToLegacyPacket()` helper method (no longer needed)
   - Added JSONObject for proper JSON serialization

3. **Build Results**:
   - Rust core: ✅ Compiled successfully (with clean rebuild)
   - UniFFI bindings: ✅ Generated successfully
   - Android APK: ✅ Built successfully (24 MB, 0 errors)

### Technical Details

**Packet Formats**:
```json
// UIDs/timestamps response
{
  "type": "cconnect.contacts.response_uids_timestamps",
  "body": {
    "uids": ["1", "3", "15"],
    "1": "1234567890",
    "3": "1234567891",
    "15": "1234567892"
  }
}

// vCards response
{
  "type": "cconnect.contacts.response_vcards",
  "body": {
    "uids": ["1", "3"],
    "1": "BEGIN:VCARD\nFN:John Smith\nEND:VCARD",
    "3": "BEGIN:VCARD\nFN:Jane Doe\nEND:VCARD"
  }
}
```

**Code Simplification**:
- Removed 17 lines of custom `convertToLegacyPacket()` helper
- Added cleaner FFI wrapper with 2 functions
- Improved maintainability and consistency

### Commits

**cosmic-connect-core**:
- Commit: `8ead308` - Add Contacts plugin FFI support

**cosmic-connect-android**:
- Commit: `87a8afc4` - Issue #65: Contacts Plugin FFI Migration

---

**Issue #65 Status**: ✅ COMPLETE
**Started**: 2026-01-16
**Completed**: 2026-01-16
**Next**: Continue Phase 2 plugin migrations
