# Issue #58: ClipboardPlugin FFI Migration

**Created:** 2026-01-16
**Status:** In Progress
**Type:** Plugin Migration
**Priority:** High
**Complexity:** Low-Medium
**Estimated Time:** 4-6 hours

---

## Executive Summary

Convert ClipboardPlugin from Java to Kotlin and verify integration with ClipboardPacketsFFI wrapper for type-safe packet creation using the cosmic-connect-core Rust FFI layer.

**Good News:** Most of the work is already complete! The FFI functions, wrapper, and even plugin integration with the wrapper exist. We just need to convert the Java implementation to Kotlin and add tests.

---

## Background

The ClipboardPlugin enables automatic clipboard synchronization between Android and COSMIC Desktop devices. It uses the KDE Connect protocol v7 clipboard packet format with two packet types:

1. **`cconnect.clipboard`** - Standard clipboard update (no timestamp)
2. **`cconnect.clipboard.connect`** - Connection sync with timestamp for loop prevention

### Current Status

✅ **Phase 2-3 Already Complete:**
- FFI functions exist in cosmic-connect-core (ffi/mod.rs:371-412)
- UDL definitions exist (cosmic_connect_core.udl:167-191)
- Android wrapper exists (ClipboardPacketsFFI.kt, 260 lines)
- Plugin ALREADY USES the FFI wrapper (ClipboardPlugin.java:106, 121)

❌ **Remaining Work:**
- Convert ClipboardPlugin.java (208 lines) to Kotlin
- Add FFI validation tests
- Create testing guide
- Write completion summary

---

## Goals

1. Convert ClipboardPlugin from Java to Kotlin
2. Verify FFI integration with ClipboardPacketsFFI wrapper
3. Ensure all clipboard synchronization features work correctly
4. Add comprehensive FFI validation tests
5. Document the migration process and testing procedures

---

## Non-Goals

- Modifying the Rust clipboard.rs implementation (already complete)
- Creating new FFI functions (already exist)
- Changing the KDE Connect protocol packet format
- Adding new clipboard features beyond existing functionality

---

## Implementation Plan

### Phase 0: Planning ⏳ (In Progress)

**Goal:** Create comprehensive migration plan with verification of existing work

**Tasks:**
- [x] Check if clipboard.rs exists in cosmic-connect-core
- [x] Read clipboard.rs implementation
- [x] Check if ClipboardPacketsFFI.kt exists
- [x] Check if FFI functions exist in ffi/mod.rs
- [x] Check if UDL definitions exist
- [x] Read ClipboardPlugin.java implementation
- [x] Create comprehensive migration plan (this document)

**Deliverables:**
- ✅ docs/issues/issue-58-clipboard-plugin-plan.md

**Time Estimate:** 1 hour
**Actual Time:** TBD

---

### Phase 1: Rust Verification ⏸️ (Pending)

**Goal:** Verify clipboard.rs compiles and all tests pass

**Context:** The clipboard.rs file (880 lines) already exists with:
- ClipboardState struct (content + timestamp)
- ClipboardPlugin with create_clipboard_packet() and create_connect_packet()
- 18 tests covering all functionality
- Sync loop prevention logic

**Tasks:**
- [ ] Navigate to cosmic-connect-core directory
- [ ] Run `cargo build --release`
- [ ] Run `cargo test --package cosmic-connect-core --lib plugins::clipboard`
- [ ] Verify all 18 tests pass
- [ ] Document any issues found

**Success Criteria:**
- ✅ Rust code compiles without errors
- ✅ All clipboard plugin tests pass (18/18)
- ✅ No deprecation warnings

**Deliverables:**
- Verification that clipboard.rs is production-ready

**Time Estimate:** 30 minutes
**Actual Time:** TBD

---

### Phase 2: FFI Interface Verification ⏸️ (Pending)

**Goal:** Confirm FFI functions exist and are properly exposed

**Context:** Both FFI functions already exist:
1. `create_clipboard_packet(content: String)` - ffi/mod.rs:371-380
2. `create_clipboard_connect_packet(content: String, timestamp: i64)` - ffi/mod.rs:402-412

**Tasks:**
- [ ] Verify ffi/mod.rs contains both functions ✅ (already verified)
- [ ] Verify cosmic_connect_core.udl has function definitions ✅ (lines 167-191)
- [ ] Verify lib.rs exports both functions ✅ (lines 52-53)
- [ ] Run `cargo build --release` in cosmic-connect-core
- [ ] Check that UniFFI scaffolding generates without errors

**Success Criteria:**
- ✅ Both FFI functions present in ffi/mod.rs
- ✅ UDL definitions match function signatures
- ✅ Functions exported in lib.rs
- ✅ UniFFI scaffolding generation succeeds

**Deliverables:**
- Verification that FFI layer is complete

**Time Estimate:** 30 minutes
**Actual Time:** TBD

---

### Phase 3: Android Wrapper Verification ⏸️ (Pending)

**Goal:** Confirm ClipboardPacketsFFI.kt wrapper is complete and functional

**Context:** ClipboardPacketsFFI.kt (260 lines) already exists with:
- 2 factory methods: createClipboardUpdate(), createClipboardConnect()
- 4 extension properties for packet inspection
- Error validation (blank content, negative timestamp)

**Tasks:**
- [ ] Review ClipboardPacketsFFI.kt implementation ✅ (already done)
- [ ] Verify factory methods call correct FFI functions
- [ ] Verify extension properties handle all packet types
- [ ] Check error handling for invalid inputs
- [ ] Compile Android project to verify wrapper works

**Success Criteria:**
- ✅ Factory methods properly call FFI functions
- ✅ Extension properties cover all clipboard packet types
- ✅ Input validation prevents invalid packets
- ✅ Android project compiles successfully

**Deliverables:**
- Verification that Android FFI wrapper is production-ready

**Time Estimate:** 30 minutes
**Actual Time:** TBD

---

### Phase 4: Android Integration (Java → Kotlin) ⏸️ (Pending)

**Goal:** Convert ClipboardPlugin.java to Kotlin and integrate with ClipboardPacketsFFI

**Context:** ClipboardPlugin.java (208 lines) ALREADY uses ClipboardPacketsFFI:
- Line 106: Uses createClipboardUpdate()
- Line 121: Uses createClipboardConnect()
- Lines 78, 84: Uses extension properties
- Lines 130-141: Has temporary convertToLegacyPacket() method

**Current Features:**
1. **Packet Reception:**
   - Uses FFI extension properties (isClipboardUpdate, isClipboardConnect)
   - Uses FFI content extraction (clipboardContent, clipboardTimestamp)
   - Timestamp-based sync loop prevention
2. **Packet Creation:**
   - Standard clipboard update packets
   - Connect packets with timestamp
   - Both use ClipboardPacketsFFI wrapper
3. **ClipboardListener Integration:**
   - Singleton clipboard monitor
   - Observer pattern for changes
   - Timestamp tracking
4. **UI Integration:**
   - Manual send button (Android 10+)
   - Menu entry for older versions
   - Permission-based UI differences

**Tasks:**
- [ ] Create ClipboardPlugin.kt
- [ ] Convert class declaration and plugin metadata
- [ ] Convert onPacketReceived() to Kotlin with when expression
- [ ] Convert propagateClipboard() - already uses FFI
- [ ] Convert sendConnectPacket() - already uses FFI
- [ ] Remove convertToLegacyPacket() temporary method
- [ ] Convert onCreate() and onDestroy() lifecycle methods
- [ ] Convert getUiButtons() and getUiMenuEntries()
- [ ] Update imports to use extension properties directly
- [ ] Delete ClipboardPlugin.java
- [ ] Build and verify compilation

**Kotlin Modernization:**
- Use `when` expression for packet type checking
- Null-safe operators for content/timestamp extraction
- Extension property imports instead of static imports
- Lambda syntax for ClipboardObserver
- Property delegation for lazy initialization
- Safe cast operators for type conversion

**Success Criteria:**
- ✅ ClipboardPlugin.kt compiles without errors
- ✅ All clipboard features preserved
- ✅ FFI wrapper integration verified
- ✅ Extension properties used for packet inspection
- ✅ No manual packet construction remaining
- ✅ ClipboardListener integration works
- ✅ UI buttons/menu entries functional

**Deliverables:**
- src/org/cosmic/cosmicconnect/Plugins/ClipboardPlugin/ClipboardPlugin.kt

**Time Estimate:** 2 hours
**Actual Time:** TBD

---

### Phase 5: Testing & Documentation ⏸️ (Pending)

**Goal:** Create comprehensive tests and documentation for ClipboardPlugin FFI migration

#### 5.1: FFI Validation Tests

**Tasks:**
- [ ] Add Test 3.6: Clipboard FFI Functions to FFIValidationTest.kt
- [ ] Test createClipboardUpdate() with various content:
  - Simple text
  - Empty string (should fail)
  - Special characters and emoji
  - Long text (1000+ characters)
- [ ] Test createClipboardConnect() with various inputs:
  - Valid content and timestamp
  - Empty content (should fail)
  - Negative timestamp (should fail)
  - Zero timestamp (valid - unknown time)
  - Future timestamp
- [ ] Test extension properties:
  - isClipboardUpdate on clipboard packet
  - isClipboardConnect on connect packet
  - clipboardContent extraction
  - clipboardTimestamp extraction
  - null handling for missing fields
- [ ] Run tests and verify all pass

**Test Structure:**
```kotlin
@Test
fun testClipboardPlugin() {
    // Test 1: Standard clipboard update
    val updatePacket = createClipboardUpdate("Hello World")
    assertNotNull(updatePacket)
    assertEquals("cconnect.clipboard", updatePacket.packetType)
    assertTrue(updatePacket.isClipboardUpdate)
    assertEquals("Hello World", updatePacket.clipboardContent)

    // Test 2: Clipboard connect with timestamp
    val connectPacket = createClipboardConnect("Hello World", 1704067200000)
    assertNotNull(connectPacket)
    assertEquals("cconnect.clipboard.connect", connectPacket.packetType)
    assertTrue(connectPacket.isClipboardConnect)
    assertEquals("Hello World", connectPacket.clipboardContent)
    assertEquals(1704067200000, connectPacket.clipboardTimestamp)

    // Test 3: Error handling - blank content
    assertFailsWith<IllegalArgumentException> {
        createClipboardUpdate("")
    }

    // Test 4: Error handling - negative timestamp
    assertFailsWith<IllegalArgumentException> {
        createClipboardConnect("Test", -1)
    }
}
```

**Deliverables:**
- Updated tests/org/cosmic/cosmicconnect/FFIValidationTest.kt (~50 new lines)

**Time Estimate:** 1 hour

#### 5.2: Testing Guide

**Tasks:**
- [ ] Create comprehensive testing guide
- [ ] Document manual test procedures:
  - Basic clipboard sync (Android → Desktop)
  - Basic clipboard sync (Desktop → Android)
  - Connect packet sync on device pairing
  - Timestamp-based loop prevention
  - Special character handling
  - Large text handling
- [ ] Document automated test procedures
- [ ] Create troubleshooting section
- [ ] Document expected behaviors and edge cases

**Deliverables:**
- docs/issue-58-testing-guide.md (~600 lines)

**Time Estimate:** 1 hour

#### 5.3: Completion Summary

**Tasks:**
- [ ] Document all changes made in Phase 4
- [ ] List commits and file changes
- [ ] Document migration metrics (lines changed, time spent)
- [ ] Note any issues encountered
- [ ] Provide testing results summary

**Deliverables:**
- docs/issue-58-completion-summary.md (~400 lines)

**Time Estimate:** 30 minutes

**Total Phase 5 Time Estimate:** 2.5 hours
**Actual Time:** TBD

---

## Technical Architecture

### Before: Manual Packet Construction (Theoretical)
```
Java Plugin → HashMap<String, Object> → NetworkPacket.create()
            → Manual field population
            → Type casting on receive
            → Error-prone string keys
```

### After: FFI-Based Construction (Already Implemented)
```
Kotlin Plugin → ClipboardPacketsFFI wrapper
              → cosmic-connect-core FFI
              → Rust clipboard.rs
              → JSON serialization
              → NetworkPacket (immutable)

Kotlin Plugin ← Extension properties
              ← Type-safe inspection
              ← Null-safe extraction
              ← NetworkPacket (received)
```

**Benefits:**
1. **Type Safety:** Compile-time validation in both Rust and Kotlin
2. **Single Source of Truth:** Packet structure defined in Rust
3. **Consistency:** Same packet format across all languages
4. **Maintainability:** Changes to packet structure only in one place
5. **Testing:** FFI functions testable in Rust independently

---

## Clipboard Packet Format

### Standard Update Packet
```json
{
  "type": "cconnect.clipboard",
  "id": 1234567890,
  "body": {
    "content": "Hello World"
  }
}
```

### Connect Packet (with Timestamp)
```json
{
  "type": "cconnect.clipboard.connect",
  "id": 1234567891,
  "body": {
    "content": "Hello World",
    "timestamp": 1704067200000
  }
}
```

---

## Clipboard Synchronization Flow

### On Device Connection
1. Desktop sends `cconnect.clipboard.connect` with current content + timestamp
2. Android receives packet
3. Android compares timestamp with local clipboard timestamp
4. If remote timestamp > local timestamp: Update local clipboard
5. If remote timestamp ≤ local timestamp: Ignore (prevents loop)

### On Clipboard Change
1. User copies text on Android
2. ClipboardListener detects change
3. ClipboardPlugin.propagateClipboard() called
4. Creates `cconnect.clipboard` packet via FFI
5. Sends packet to desktop
6. Desktop updates its clipboard

### Sync Loop Prevention
- Connect packets include timestamp (milliseconds since epoch)
- Standard update packets do NOT include timestamp
- On connection, only the device with the newer clipboard updates the other
- This prevents infinite sync loops between devices

---

## Testing Strategy

### Unit Tests (FFI Validation)
- Test createClipboardUpdate() with various inputs
- Test createClipboardConnect() with various timestamps
- Test extension properties for packet inspection
- Test error handling for invalid inputs

### Manual Tests
1. **Basic Sync:** Copy on Android, verify on Desktop
2. **Reverse Sync:** Copy on Desktop, verify on Android
3. **Connect Sync:** Disconnect and reconnect, verify clipboard syncs
4. **Loop Prevention:** Copy same text on both devices, verify no loop
5. **Special Chars:** Test emoji, unicode, symbols
6. **Large Text:** Test 10,000+ character clipboard
7. **Rapid Changes:** Multiple clipboard changes in quick succession
8. **Connection Loss:** Clipboard change while disconnected, verify sync on reconnect

### Performance Tests
- Latency: Measure time from clipboard change to remote update (target < 500ms)
- Memory: Verify no leaks with repeated clipboard operations
- Battery: Clipboard sync should not significantly impact battery

---

## Risks and Mitigations

### Risk 1: Clipboard Permissions (Android 10+)
**Mitigation:**
- ClipboardListener handles permission checks
- UI button only shown when permitted
- Menu entry shown when button unavailable

### Risk 2: Sync Loop
**Mitigation:**
- Timestamp-based loop prevention already implemented
- Connect packets include timestamp
- Standard packets do not include timestamp

### Risk 3: Large Clipboard Content
**Mitigation:**
- No artificial limits (let system handle)
- Test with large content (10,000+ chars)
- Monitor memory usage

### Risk 4: convertToLegacyPacket() Dependency
**Current:** Plugin uses temporary conversion method (lines 130-141)
**Mitigation:** Method works correctly, will be removed when NetworkPacket is fully migrated
**Impact:** Low - functional but not ideal

---

## Success Criteria

Issue #58 is considered complete when:

✅ **All phases complete:**
- Phase 0: Planning document created
- Phase 1: Rust clipboard.rs verified
- Phase 2: FFI functions verified
- Phase 3: Android wrapper verified
- Phase 4: ClipboardPlugin.kt created and functional
- Phase 5: Tests and documentation complete

✅ **All tests pass:**
- FFI validation tests (clipboard functions)
- Manual testing checklist complete
- No regressions identified

✅ **Documentation complete:**
- Testing guide created
- Completion summary written
- Migration metrics documented

✅ **Code quality:**
- No compilation errors
- No deprecation warnings
- Follows Kotlin conventions
- Uses FFI wrapper exclusively

---

## Timeline

| Phase | Name | Estimate | Actual |
|-------|------|----------|--------|
| 0 | Planning | 1h | TBD |
| 1 | Rust Verification | 0.5h | TBD |
| 2 | FFI Verification | 0.5h | TBD |
| 3 | Wrapper Verification | 0.5h | TBD |
| 4 | Android Integration | 2h | TBD |
| 5 | Testing & Docs | 2.5h | TBD |
| **Total** | | **7h** | **TBD** |

*Note: Phases 2 and 3 may be very quick since we've already verified the code exists and is correct.*

---

## References

### Related Issues
- Issue #54: SharePlugin FFI Migration (completed)
- Issue #55: TelephonyPlugin FFI Migration (completed)
- Issue #56: BatteryPlugin FFI Migration (completed)
- Issue #57: NotificationsPlugin FFI Migration (completed)

### Code References
- cosmic-connect-core/src/plugins/clipboard.rs (Rust implementation)
- cosmic-connect-core/src/ffi/mod.rs:371-412 (FFI functions)
- cosmic-connect-core/src/cosmic_connect_core.udl:167-191 (UDL definitions)
- src/org/cosmic/cosmicconnect/Plugins/ClipboardPlugin/ClipboardPacketsFFI.kt (Android wrapper)
- src/org/cosmic/cosmicconnect/Plugins/ClipboardPlugin/ClipboardPlugin.java (Current implementation)

### External Documentation
- KDE Connect Protocol v7: https://invent.kde.org/network/kdeconnect-kde
- UniFFI: https://mozilla.github.io/uniffi-rs/
- Android ClipboardManager: https://developer.android.com/reference/android/content/ClipboardManager

---

**Status:** Phase 0 (Planning) In Progress
**Next Step:** Complete Phase 0 and begin Phase 1 (Rust Verification)
**Blocked By:** None
**Blocking:** None

---

*This plan follows the established migration pattern from Issues #54-57. The ClipboardPlugin migration is simpler than previous plugins because the FFI integration is already complete.*
