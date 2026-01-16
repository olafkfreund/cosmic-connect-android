# Issue #70: Fix FFI Validation Test Suite Compilation

**Status**: IN PROGRESS
**Date**: 2026-01-16
**Priority**: HIGH
**Blocks**: Runtime FFI verification for Issue #68
**Related**: Issue #50 (FFI Validation), Issue #68 (Build Fix)

## Overview

The FFI validation test suite (FFIValidationTest.kt) has 70+ compilation errors preventing runtime verification of the FFI implementation. Tests were written for an old FFI interface that expected JSON strings and maps, but the current implementation uses typed Kotlin wrapper objects.

## Problem Statement

After syncing from cosmic-connect-desktop-app and cosmic-connect-core:
- Build succeeds (APK assembles)
- Main source compiles (0 errors)
- **Test suite won't compile** (70+ type errors)
- Cannot verify FFI works at runtime

## Root Cause

**Interface Mismatch**: Tests written for prototype FFI interface, but production uses different signatures.

### Example Mismatches

**Test expects**:
```kotlin
createNotificationPacket(jsonString: String): FfiPacket
```

**Actual signature**:
```kotlin
fun createNotificationPacket(notification: NotificationInfo): NetworkPacket
```

**Test expects**:
```kotlin
createPacket(type: String, body: Map<String, Any>): FfiPacket
```

**Actual signature**:
```kotlin
fun createPacket(type: String, bodyJson: String): FfiPacket  // uniffi function
NetworkPacket.create(type: String, body: Map<String, Any>): NetworkPacket  // Kotlin wrapper
```

## Compilation Errors

### Category 1: Type Mismatches (50+ errors)

Tests pass `Map<String, Any>` where `String` (JSON) expected:

```
Line 107: createPacket(type, body: Map) - expects bodyJson: String
Line 463: createPacket(type, mapOf("seq" to it)) - expects String
Line 469: createPacket(type, mapOf("msg" to "test")) - expects String
```

### Category 2: Property Access Errors (10+ errors)

Tests use wrong property names or methods:

```
Line 116: packet.packetType - should be packet.type
Line 119-120: body["deviceId"] as String vs Int confusion
Line 435-437: packet.body.containsKey() - body is Map, has containsKey
Line 213-214: certInfo.certificate.length - String property, not array
```

### Category 3: Helper Function Calls (10+ errors)

Tests call non-existent helper functions:

```
Line 330: createNotificationPacket(jsonString) - expects NotificationInfo object
Line 341: createCancelNotificationPacket(id) - may not exist
Line 558: createClipboardPacket(content) - signature may differ
```

## Affected Tests

| Test | Line | Status | Issues |
|------|------|--------|--------|
| testNativeLibraryLoading | 51 | OK | No issues |
| testRuntimeInitialization | 69 | OK | No issues |
| testPacketCreation | 102 | BROKEN | Map vs String mismatch (line 107) |
| testPacketSerialization | 132 | BROKEN | Map vs String mismatch (line 138) |
| testPacketDeserialization | 160 | BROKEN | Type confusion (lines 179, 183) |
| testCertificateGeneration | 192 | BROKEN | Property access (lines 213-214) |
| testPluginManagerCreation | 232 | OK | Try-catch handles |
| testBatteryPlugin | 254 | BROKEN | Map vs String (line 260) |
| testPingPluginLegacy | 286 | BROKEN | Map vs String (line 293) |
| testNotificationsPlugin | 312 | BROKEN | Wrong function signatures (lines 330+) |
| testComplexNotification | 390 | BROKEN | Wrong function signatures (lines 412+) |
| benchmarkFFICalls | 454 | BROKEN | Map vs String (lines 463, 469) |
| testEndToEndPacketFlow | 507 | BROKEN | Map vs String (line 514) |
| testClipboardPlugin | 552 | BROKEN | Wrong signatures (line 558+) |
| testFindMyPhonePlugin | 643 | OK | Uses correct createFindmyphoneRequest() |
| testRunCommandPlugin | 719 | BROKEN | containsKey issues (lines 734+) |
| testPingPlugin | 808 | FIXED | Fixed serialization |
| printTestSummary | 888 | OK | No issues |

**Summary**: 12/18 tests broken, 6/18 working

## Fix Strategy

### Phase 1: Understand Current API (COMPLETE)

Analyzed actual FFI function signatures:
- `uniffi.cosmic_connect_core.*` - Raw FFI functions (take JSON strings)
- `Core.NetworkPacket.*` - Kotlin wrappers (take Map<String, Any>)
- Plugin-specific wrappers (PingPacketsFFI, NotificationsPacketsFFI, etc.)

### Phase 2: Update Helper Functions

Create test helper functions that bridge the gap:

```kotlin
// Test helper - wraps Kotlin API for testing
private fun testCreatePacket(type: String, body: Map<String, Any>): NetworkPacket {
    return NetworkPacket.create(type, body)
}

// Or use actual API directly:
val packet = NetworkPacket.create("kdeconnect.ping", mapOf("message" to "hello"))
```

### Phase 3: Fix Tests Systematically

Fix tests in order:
1. Basic packet tests (creation, serialization, deserialization)
2. Plugin tests (battery, ping, notifications, etc.)
3. Performance tests (benchmarks)
4. Integration tests (end-to-end)

### Phase 4: Verify Compilation

Run: `./gradlew testDebugUnitTest --tests "*.FFIValidationTest"`

### Phase 5: Run Tests

After compilation succeeds, run tests to verify FFI works at runtime.

## Implementation Plan

### Task 1: Add Test Helper Functions

Add to FFIValidationTest.kt companion object or top:

```kotlin
companion object {
    // ... existing code ...

    // Test helper - creates packet using Kotlin wrapper API
    private fun createTestPacket(type: String, body: Map<String, Any> = emptyMap()): NetworkPacket {
        return NetworkPacket.create(type, body)
    }

    // Test helper - serializes packet
    private fun serializeTestPacket(packet: NetworkPacket): ByteArray {
        return packet.serialize()
    }

    // Test helper - deserializes packet
    private fun deserializeTestPacket(data: ByteArray): NetworkPacket {
        return NetworkPacket.deserialize(data)
    }
}
```

### Task 2: Fix Basic Packet Tests

**testPacketCreation** (line 102):
```kotlin
// Before:
val packet = createPacket(
    packetType = "kdeconnect.identity",
    body = mapOf(...)
)

// After:
val packet = NetworkPacket.create(
    type = "kdeconnect.identity",
    body = mapOf(...)
)
// Access with packet.type not packet.packetType
```

**testPacketSerialization** (line 132):
```kotlin
// Before:
val packet = createPacket(type, body)
val bytes = serializePacket(packet)

// After:
val packet = NetworkPacket.create(type, body)
val bytes = packet.serialize()
```

**testPacketDeserialization** (line 160):
```kotlin
// Before:
val packet = deserializePacket(json.encodeToByteArray())
assertEquals("kdeconnect.ping", packet.packetType)

// After:
val packet = NetworkPacket.deserialize(json.encodeToByteArray())
assertEquals("kdeconnect.ping", packet.type)
```

### Task 3: Fix Plugin Tests

Tests should use plugin-specific FFI wrappers where available:

**Notifications** (line 312):
```kotlin
// Create NotificationInfo object first
val notificationInfo = NotificationInfo(
    id = "test-notif-123",
    appName = "Messages",
    title = "New Message",
    text = "Hello from Android!",
    isClearable = true,
    timestamp = 1704067200000L,
    silent = false
)

// Then call FFI wrapper
val packet = NotificationsPacketsFFI.createNotificationPacket(notificationInfo)
```

**Clipboard** (line 558):
```kotlin
// Check actual ClipboardPacketsFFI signature and use it
val packet = ClipboardPacketsFFI.createClipboardPacket("Hello World")
```

### Task 4: Fix Property Access

Replace all:
- `packet.packetType` → `packet.type`
- `packet.body["field"] as String` → check actual types returned
- `certInfo.certificate.length` → `certInfo.certificate.size` (if ByteArray) or `.length` (if String)

## Testing Approach

### Compilation Test
```bash
./gradlew testDebugUnitTest --tests "*.FFIValidationTest" 2>&1 | grep "^e:"
```

Should show 0 errors.

### Incremental Testing

Test individual tests as fixed:
```bash
./gradlew testDebugUnitTest --tests "*.FFIValidationTest.testPacketCreation"
./gradlew testDebugUnitTest --tests "*.FFIValidationTest.testPingPlugin"
```

### Full Suite
```bash
./gradlew testDebugUnitTest --tests "*.FFIValidationTest"
```

## Expected Outcomes

### Success Criteria

1. **Compilation**: All tests compile (0 errors)
2. **Tests Run**: Test suite executes
3. **Results**: Identify which FFI functions work vs fail

### Possible Test Results

**Scenario A: Tests Pass**
- FFI implementation works
- Issue #68 fully verified
- Can continue plugin migrations

**Scenario B: Tests Fail (Expected)**
- Compilation succeeds
- Runtime errors reveal placeholder FFI methods
- Confirms Issue #69 needed (implement FFI methods in Rust)
- Provides concrete evidence of what's broken

Either outcome unblocks progress:
- Scenario A: Continue migrations
- Scenario B: Clear requirements for Issue #69

## Progress Tracking

### Completed
- [DONE] Identified 70+ compilation errors
- [DONE] Categorized error types
- [DONE] Analyzed actual API signatures
- [DONE] Created fix strategy

### In Progress
- [TODO] Add test helper functions
- [TODO] Fix basic packet tests (creation, serialization, deserialization)
- [TODO] Fix plugin tests (notifications, clipboard, etc.)
- [TODO] Fix property access issues
- [TODO] Verify compilation succeeds
- [TODO] Run tests and document results

### Blocked
- Runtime verification (blocked until compilation fixed)

## Related Documentation

- Issue #50: docs/issue-50-ffi-validation.md - Original test plan
- Issue #68: docs/issue-68-build-fix-summary.md - Build verification
- Issue #69: (To be created) - Implement FFI methods in Rust
- Test file: tests/org/cosmic/cosmicconnect/FFIValidationTest.kt

## Time Estimate

- Fix compilation: 2-3 hours (systematic fixes)
- Run tests: 10-15 minutes
- Document results: 30 minutes
- Total: 3-4 hours

---

**Issue #70 Status**: IN PROGRESS
**Started**: 2026-01-16
**Compilation Status**: FAILING (70+ errors)
**Next**: Begin systematic fixes starting with helper functions
