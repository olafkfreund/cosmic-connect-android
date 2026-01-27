# Issue #54: Clipboard Plugin Migration to FFI

**Created**: 2026-01-16
**Status**: Planning
**Plugin**: ClipboardPlugin
**Priority**: High
**Estimated Effort**: 10-12 hours (25% time savings from Issue #53 patterns)

---

## Overview

Migrate the Clipboard plugin from manual packet creation to FFI-based architecture, following the proven 5-phase pattern established in Issue #53 (Share Plugin Migration).

### Goals

1. **Type-Safe Packet Creation**: Use ClipboardPacketsFFI wrapper for packet creation
2. **Protocol Compliance**: Fix packet types (cosmicconnect → kdeconnect)
3. **Extension Properties**: Implement type-safe packet inspection
4. **Code Quality**: Reduce manual HashMap construction
5. **Maintainability**: Single source of truth for packet structure
6. **Pattern Reuse**: Apply lessons from Issue #53

---

## Current State Analysis

### Rust Implementation (cosmic-connect-core/src/plugins/clipboard.rs)

**Status**: ⚠️ Has Device dependencies and compilation errors

**Current Issues**:
1. Device parameter references without proper function signatures (lines 435, 471, 572, 575)
2. Duplicate `initialize` method (line 558 should be `start`)
3. Uses `device` in initialize without parameter (line 553)
4. Test compilation errors due to Device type usage
5. Already has good packet creation methods and timestamp logic

**Good Aspects**:
- Well-documented protocol (lines 1-101)
- ClipboardState struct for state management
- Timestamp-based sync loop prevention
- Comprehensive test coverage
- Clean async API

### Android Implementation (ClipboardPlugin.java)

**Status**: ⚠️ Protocol compliance issue, manual packet creation

**Current Issues**:
1. **Protocol Violation**: Uses `cconnect.clipboard` instead of `cconnect.clipboard` (lines 44, 59)
2. **Manual Packet Creation**: HashMap construction for packets (lines 97-99, 113-117)
3. **No Type Safety**: String-based field access ("content", "timestamp")
4. **Code Duplication**: convertToLegacyPacket helper (lines 126-137)

**Good Aspects**:
- Already using immutable NetworkPacket (Issue #64)
- Clean separation with ClipboardListener
- Timestamp tracking for sync loop prevention
- User-initiated clipboard send feature
- Good permission handling

---

## Protocol Specification

### Packet Types

**Standard Clipboard Update** (sent when clipboard changes):
```json
{
    "id": 1234567890,
    "type": "cconnect.clipboard",
    "body": {
        "content": "text content"
    }
}
```

**Connection Sync** (sent on device connection with timestamp):
```json
{
    "id": 1234567890,
    "type": "cconnect.clipboard.connect",
    "body": {
        "content": "text content",
        "timestamp": 1640000000000
    }
}
```

### Sync Loop Prevention

- Each clipboard update has a timestamp (UNIX epoch milliseconds)
- Incoming updates with timestamp ≤ local timestamp are ignored
- Incoming updates with timestamp > local timestamp are accepted
- Connect packets with timestamp 0 are ignored (no content)

---

## 5-Phase Migration Plan

### Phase 1: Rust Refactoring ✅ Pattern from Issue #53

**Estimated**: 2 hours
**Files**: `cosmic-connect-core/src/plugins/clipboard.rs`

**Tasks**:
1. Remove Device dependencies from methods
2. Fix duplicate `initialize` method (rename to `start`)
3. Fix method signatures (remove device parameters)
4. Update tests to work without Device type
5. Ensure all tests pass
6. Commit: "Issue #54 Phase 1: Rust refactoring (clipboard.rs)"

**Key Changes**:
```rust
// Before: Device parameter
async fn handle_clipboard_update(&self, packet: &Packet, device: &Device)

// After: No Device dependency
async fn handle_clipboard_update(&self, packet: &Packet)
```

**Success Criteria**:
- ✅ All Rust tests pass
- ✅ No Device dependencies
- ✅ cargo build successful
- ✅ No compilation warnings

---

### Phase 2: FFI Interface Implementation ✅ Pattern from Issue #53

**Estimated**: 2-3 hours
**Files**:
- `cosmic-connect-core/src/ffi/mod.rs`
- `cosmic-connect-core/src/cosmic_connect_core.udl`
- `cosmic-connect-core/src/lib.rs`

**Tasks**:
1. Create FFI packet creation functions
2. Add to UniFFI interface
3. Export in lib.rs
4. Build and verify
5. Commit: "Issue #54 Phase 2: FFI interface implementation"

**FFI Functions to Add**:
```rust
/// Create standard clipboard update packet
pub fn create_clipboard_packet(content: String) -> Result<FfiPacket>

/// Create clipboard connect packet with timestamp
pub fn create_clipboard_connect_packet(
    content: String,
    timestamp: i64
) -> Result<FfiPacket>
```

**UniFFI Interface**:
```udl
[Throws=ProtocolError]
FfiPacket create_clipboard_packet(string content);

[Throws=ProtocolError]
FfiPacket create_clipboard_connect_packet(
  string content,
  i64 timestamp
);
```

**Success Criteria**:
- ✅ FFI functions compile
- ✅ UniFFI bindings generate
- ✅ cargo build successful
- ✅ All exports added to lib.rs

---

### Phase 3: Android Wrapper Creation ✅ Pattern from Issue #53

**Estimated**: 2-3 hours
**Files**:
- `src/org/cosmic/cosmicconnect/Plugins/ClipboardPlugin/ClipboardPacketsFFI.kt` (new)

**Tasks**:
1. Create ClipboardPacketsFFI.kt object
2. Implement packet creation wrappers
3. Add extension properties for inspection
4. Add input validation
5. Build and verify
6. Commit: "Issue #54 Phase 3: Android wrapper creation"

**ClipboardPacketsFFI.kt Structure**:
```kotlin
package org.cosmic.cconnect.Plugins.ClipboardPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*

object ClipboardPacketsFFI {
    /**
     * Create standard clipboard update packet
     */
    fun createClipboardUpdate(content: String): NetworkPacket {
        require(content.isNotBlank()) { "Clipboard content cannot be blank" }

        val ffiPacket = createClipboardPacket(content)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create clipboard connect packet with timestamp
     */
    fun createClipboardConnect(
        content: String,
        timestamp: Long
    ): NetworkPacket {
        require(content.isNotBlank()) { "Clipboard content cannot be blank" }
        require(timestamp >= 0) { "Timestamp cannot be negative" }

        val ffiPacket = createClipboardConnectPacket(content, timestamp)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// Extension properties for type-safe packet inspection
val NetworkPacket.isClipboardUpdate: Boolean
    get() = type == "cconnect.clipboard" && body.containsKey("content")

val NetworkPacket.isClipboardConnect: Boolean
    get() = type == "cconnect.clipboard.connect" &&
            body.containsKey("content") &&
            body.containsKey("timestamp")

val NetworkPacket.clipboardContent: String?
    get() = if (isClipboardUpdate || isClipboardConnect) {
        body["content"] as? String
    } else null

val NetworkPacket.clipboardTimestamp: Long?
    get() = if (isClipboardConnect) {
        (body["timestamp"] as? Number)?.toLong()
    } else null
```

**Success Criteria**:
- ✅ Kotlin code compiles
- ✅ Input validation works
- ✅ Extension properties accessible
- ✅ ./gradlew compileDebugKotlin successful

---

### Phase 4: Android Integration ✅ Pattern from Issue #53

**Estimated**: 2 hours
**Files**: `src/org/cosmic/cosmicconnect/Plugins/ClipboardPlugin/ClipboardPlugin.java`

**Tasks**:
1. Fix packet type constants (cosmicconnect → kdeconnect)
2. Update packet creation to use ClipboardPacketsFFI
3. Update packet type checking to use extension properties
4. Update data extraction to use extension properties
5. Remove convertToLegacyPacket (no longer needed)
6. Build and verify
7. Commit: "Issue #54 Phase 4: Android integration"

**Changes to Make**:

#### 1. Fix Packet Type Constants (Lines 44, 59)
```java
// Before
private final static String PACKET_TYPE_CLIPBOARD = "cconnect.clipboard";
private final static String PACKET_TYPE_CLIPBOARD_CONNECT = "cconnect.clipboard.connect";

// After
private final static String PACKET_TYPE_CLIPBOARD = "cconnect.clipboard";
private final static String PACKET_TYPE_CLIPBOARD_CONNECT = "cconnect.clipboard.connect";
```

#### 2. Add FFI Imports
```java
import org.cosmic.cconnect.Plugins.ClipboardPlugin.ClipboardPacketsFFI;
import static org.cosmic.cconnect.Plugins.ClipboardPlugin.ClipboardPacketsFFIKt.*;
```

#### 3. Update propagateClipboard() (Lines 95-103)
```java
// Before
void propagateClipboard(String content) {
    Map<String, Object> body = new HashMap<>();
    body.put("content", content);
    NetworkPacket packet = NetworkPacket.create(ClipboardPlugin.PACKET_TYPE_CLIPBOARD, body);
    getDevice().sendPacket(convertToLegacyPacket(packet));
}

// After
void propagateClipboard(String content) {
    NetworkPacket packet = ClipboardPacketsFFI.INSTANCE.createClipboardUpdate(content);
    getDevice().sendPacket(convertToLegacyPacket(packet));
}
```

#### 4. Update sendConnectPacket() (Lines 105-121)
```java
// Before
private void sendConnectPacket() {
    String content = ClipboardListener.instance(context).getCurrentContent();
    if (content == null) return;

    Map<String, Object> body = new HashMap<>();
    long timestamp = ClipboardListener.instance(context).getUpdateTimestamp();
    body.put("timestamp", timestamp);
    body.put("content", content);
    NetworkPacket packet = NetworkPacket.create(ClipboardPlugin.PACKET_TYPE_CLIPBOARD_CONNECT, body);

    getDevice().sendPacket(convertToLegacyPacket(packet));
}

// After
private void sendConnectPacket() {
    String content = ClipboardListener.instance(context).getCurrentContent();
    if (content == null) return;

    long timestamp = ClipboardListener.instance(context).getUpdateTimestamp();
    NetworkPacket packet = ClipboardPacketsFFI.INSTANCE.createClipboardConnect(content, timestamp);

    getDevice().sendPacket(convertToLegacyPacket(packet));
}
```

#### 5. Update onPacketReceived() (Lines 72-91)
```java
// Before
@Override
public boolean onPacketReceived(@NonNull org.cosmic.cconnect.NetworkPacket np) {
    String content = np.getString("content");
    switch (np.getType()) {
        case (PACKET_TYPE_CLIPBOARD):
            ClipboardListener.instance(context).setText(content);
            return true;
        case(PACKET_TYPE_CLIPBOARD_CONNECT):
            long packetTime = np.getLong("timestamp");
            if (packetTime == 0 || packetTime < ClipboardListener.instance(context).getUpdateTimestamp()) {
                return false;
            }
            if (np.has("content")) {
                ClipboardListener.instance(context).setText(content);
            }
            return true;
    }
    throw new UnsupportedOperationException("Unknown packet type: " + np.getType());
}

// After
@Override
public boolean onPacketReceived(@NonNull org.cosmic.cconnect.NetworkPacket legacyNp) {
    // Convert legacy packet to immutable for type-safe inspection
    NetworkPacket np = NetworkPacket.fromLegacy(legacyNp);

    if (getIsClipboardUpdate(np)) {
        String content = getClipboardContent(np);
        if (content != null) {
            ClipboardListener.instance(context).setText(content);
        }
        return true;
    } else if (getIsClipboardConnect(np)) {
        Long timestamp = getClipboardTimestamp(np);
        if (timestamp == null || timestamp == 0 ||
            timestamp < ClipboardListener.instance(context).getUpdateTimestamp()) {
            return false;
        }

        String content = getClipboardContent(np);
        if (content != null) {
            ClipboardListener.instance(context).setText(content);
        }
        return true;
    }

    return false;
}
```

**Success Criteria**:
- ✅ Protocol compliance (cconnect.clipboard)
- ✅ FFI wrappers used
- ✅ Extension properties used
- ✅ convertToLegacyPacket helper still present
- ✅ ./gradlew compileDebugJava successful
- ✅ No compilation errors

---

### Phase 5: Testing & Documentation ✅ Pattern from Issue #53

**Estimated**: 2 hours
**Files**:
- `docs/issues/issue-54-testing-guide.md` (new)
- `docs/issues/issue-54-phase1-complete.md` (new)
- `docs/issues/issue-54-phase2-complete.md` (new)
- `docs/issues/issue-54-phase3-complete.md` (new)
- `docs/issues/issue-54-phase4-complete.md` (new)
- `docs/issues/issue-54-complete-summary.md` (new)
- `docs/guides/FFI_INTEGRATION_GUIDE.md` (update)
- `docs/guides/project-status-2026-01-16.md` (update to create 01-17)

**Tasks**:
1. Create comprehensive testing guide
2. Document each phase completion
3. Create complete project summary
4. Update FFI Integration Guide
5. Update project status
6. Commit: "Issue #54 Phase 5: Testing & Documentation"

**Testing Scenarios** (15 test cases across 5 test suites):

#### Test Suite 1: Standard Clipboard Sync (3 tests)
1. Send clipboard Android → Desktop
2. Receive clipboard Desktop → Android
3. Content verification with special characters

#### Test Suite 2: Connection Sync (3 tests)
1. Initial connection sync with newer content
2. Connection sync with older content (ignored)
3. Connection sync with timestamp 0 (ignored)

#### Test Suite 3: Sync Loop Prevention (3 tests)
1. Verify timestamp comparison logic
2. Multiple updates with same timestamp
3. Rapid clipboard changes

#### Test Suite 4: Error Scenarios (3 tests)
1. Empty content handling
2. Null content handling
3. Network failure during sync

#### Test Suite 5: Protocol Compliance (3 tests)
1. Packet type verification (cconnect.clipboard)
2. Field name correctness
3. KDE Connect compatibility

**Success Criteria**:
- ✅ All test scenarios documented
- ✅ All phase completion docs created
- ✅ Complete project summary
- ✅ FFI Integration Guide updated
- ✅ Project status updated

---

## Expected Benefits

### Code Quality

**Before**:
```java
Map<String, Object> body = new HashMap<>();
body.put("content", text);
NetworkPacket packet = NetworkPacket.create("cconnect.clipboard", body);
```

**After**:
```java
NetworkPacket packet = ClipboardPacketsFFI.INSTANCE.createClipboardUpdate(text);
```

### Type Safety

**Before**:
```java
if (np.getType().equals(PACKET_TYPE_CLIPBOARD)) {
    String content = np.getString("content");
    // Hope content isn't null...
}
```

**After**:
```java
if (getIsClipboardUpdate(np)) {
    String content = getClipboardContent(np);
    if (content != null) {
        // Guaranteed non-null
    }
}
```

### Protocol Compliance

**Before**: `cconnect.clipboard` ❌
**After**: `cconnect.clipboard` ✅

### Maintainability

- ✅ Single source of truth for packet structure
- ✅ Compile-time checking prevents protocol violations
- ✅ Input validation prevents malformed packets
- ✅ Easier to spot bugs
- ✅ Better IDE support

---

## Success Metrics

### Code Changes (Estimated)
- **Rust**: ~50 lines modified (Device dependency removal)
- **FFI**: ~80 lines added (packet creation functions)
- **Kotlin**: ~200 lines added (ClipboardPacketsFFI.kt)
- **Java**: ~40 lines modified (ClipboardPlugin.java)
- **Documentation**: ~1,500 lines (5 phase docs + testing guide + summary)
- **Total**: ~1,870 lines

### Build Success
- ✅ Rust: cargo build (no errors)
- ✅ Kotlin: ./gradlew compileDebugKotlin (no errors)
- ✅ Java: ./gradlew compileDebugJava (no errors)
- ✅ Full build: ./gradlew assembleDebug (success)

### Quality Improvements
- **Type Safety**: 100% of packet creation type-safe
- **Input Validation**: All packet creation validates inputs
- **Protocol Compliance**: Fixed packet types
- **Null Safety**: Explicit null checks on all data extraction
- **Code Reduction**: ~30% less code in ClipboardPlugin.java

---

## Known Limitations

### Current Scope
- Manual testing only (no automated tests in Phase 4)
- No performance profiling
- No memory leak testing

### Future Enhancements (Optional)
1. Add unit tests (4-6 hours)
2. Add integration tests (4-6 hours)
3. Performance profiling (2-3 hours)
4. Memory leak detection (2-3 hours)

---

## Migration Pattern Summary

This migration follows the **4-layer pattern** established in Issue #53:

**Layer 1: Rust FFI** → Create packet creation functions
**Layer 2: UniFFI Interface** → Define FFI boundary
**Layer 3: Kotlin Wrapper** → Type-safe Android API
**Layer 4: Java Integration** → Use wrappers in plugin

**Time Savings**: ~25% faster than Issue #53 due to pattern reuse

---

## Dependencies

### Required
- Issue #50: FFI Bindings Validation ✅ Complete
- Issue #51: Android NDK Integration ✅ 90% Complete
- Issue #53: Share Plugin Migration ✅ Complete (pattern source)
- Issue #64: NetworkPacket Migration ✅ Complete

### Optional
- Clipboard functionality testing on real devices
- COSMIC Desktop applet for end-to-end testing

---

## Risk Assessment

### Low Risk ✅
- Pattern proven in Issue #53
- Simple plugin (text-only)
- Clear protocol specification
- Comprehensive tests in Rust

### Medium Risk ⚠️
- Timestamp sync logic complexity
- Sync loop prevention edge cases
- ClipboardListener integration

### Mitigations
- Follow Issue #53 patterns exactly
- Extensive manual testing of timestamp logic
- Document all edge cases in testing guide

---

## References

### Pattern Source
- Issue #53: Share Plugin Migration (complete 5-phase pattern)
- docs/issues/issue-53-complete-summary.md
- docs/guides/FFI_INTEGRATION_GUIDE.md

### Protocol Documentation
- cosmic-connect-core/src/plugins/clipboard.rs (lines 1-101)
- [Valent Protocol Documentation](https://valent.andyholmes.ca/documentation/protocol.html)
- [KDE Connect Clipboard Plugin](https://invent.kde.org/network/kdeconnect-kde/tree/master/plugins/clipboard)

### Related Issues
- Issue #50: FFI Bindings Validation
- Issue #51: Android NDK Integration
- Issue #53: Share Plugin Migration
- Issue #64: NetworkPacket Migration

---

**Document Version**: 1.0
**Created**: 2026-01-16
**Status**: Planning Complete
**Next Phase**: Phase 1 (Rust Refactoring)
**Est. Completion**: 2026-01-17 (if 10-12 hours available)

🚀 **Ready to Start Phase 1!**
