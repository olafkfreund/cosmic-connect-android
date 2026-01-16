# Issue #59: FindMyPhonePlugin FFI Migration

**Created:** 2026-01-16
**Status:** In Progress
**Type:** Plugin Migration
**Priority:** High
**Complexity:** Low-Medium
**Estimated Time:** 5-6 hours

---

## Executive Summary

Convert FindMyPhonePlugin from Java to Kotlin and integrate with FFI (Foreign Function Interface) using cosmic-connect-core Rust library. Unlike ClipboardPlugin, this migration requires creating NEW FFI functions and Android wrapper from scratch.

**Key Feature:** Makes phone ring at maximum volume to help locate lost/misplaced devices. Works even when phone is muted.

---

## Background

The FindMyPhonePlugin allows COSMIC Desktop users to make their Android device ring to locate it. This is a unidirectional plugin:

- **Desktop (Rust):** SENDS `kdeconnect.findmyphone.request` packets
- **Android (Java/Kotlin):** RECEIVES requests and makes phone ring

### Current Status

✅ **Rust Implementation Exists:**
- File: cosmic-connect-core/src/plugins/findmyphone.rs (173 lines)
- Module: Disabled (commented out in plugins/mod.rs)
- Method: `create_ring_request()` - creates empty body packet
- Tests: 5 tests covering plugin lifecycle

❌ **FFI Layer Missing:**
- NO FFI functions in ffi/mod.rs
- NO UDL definitions in cosmic_connect_core.udl
- Need to create: `create_findmyphone_request()`

❌ **Android Wrapper Missing:**
- NO FindMyPhonePacketsFFI.kt wrapper
- Need to create from scratch

✅ **Android Plugin Exists:**
- File: FindMyPhonePlugin.java (240 lines)
- Functionality: MediaPlayer, volume control, notifications, wake locks
- Permissions: POST_NOTIFICATIONS (Android 13+)
- Settings: Custom ringtone selection

---

## Goals

1. Create FFI function `create_findmyphone_request()` in cosmic-connect-core
2. Add UDL definition for FFI function
3. Create FindMyPhonePacketsFFI.kt Android wrapper
4. Convert FindMyPhonePlugin from Java to Kotlin
5. Preserve all existing functionality (MediaPlayer, notifications, volume, wake locks)
6. Add comprehensive FFI validation tests
7. Document migration process and testing procedures

---

## Non-Goals

- Modifying the Rust findmyphone.rs implementation (already complete)
- Enabling the disabled findmyphone module (not required for FFI)
- Changing the KDE Connect protocol packet format
- Adding new features beyond existing functionality
- Making Android send findmyphone requests (desktop-only feature)

---

## Implementation Plan

### Phase 0: Planning ⏳ (In Progress)

**Goal:** Create comprehensive migration plan

**Tasks:**
- [x] Check if findmyphone.rs exists
- [x] Check if FFI functions exist (they don't)
- [x] Check if Android wrapper exists (it doesn't)
- [x] Read FindMyPhonePlugin.java implementation
- [x] Understand Android-specific functionality
- [x] Create comprehensive migration plan (this document)

**Deliverables:**
- ✅ docs/issues/issue-59-findmyphone-plugin-plan.md

**Time Estimate:** 30 minutes
**Actual Time:** TBD

---

### Phase 1: Rust Verification ⏸️ (Pending)

**Goal:** Verify findmyphone.rs compiles (even though module is disabled)

**Context:** findmyphone.rs exists but has compilation issues:
- Duplicate `initialize()` method (lines 88, 94)
- Missing Device import
- Module is disabled in plugins/mod.rs

**Note:** We'll create FFI functions independently of the findmyphone module, so these issues won't block us.

**Tasks:**
- [ ] Verify cosmic-connect-core builds: `cargo build --release`
- [ ] Document that findmyphone module is disabled
- [ ] Confirm FFI functions can be independent

**Success Criteria:**
- ✅ cosmic-connect-core builds successfully
- ✅ Understand findmyphone module status
- ✅ Confirm FFI approach

**Deliverables:**
- Verification that Rust layer is ready for FFI additions

**Time Estimate:** 15 minutes
**Actual Time:** TBD

---

### Phase 2: FFI Interface Implementation ⏸️ (Pending)

**Goal:** Create FFI function in cosmic-connect-core for findmyphone request packet

#### 2.1: Create FFI Function

**Location:** cosmic-connect-core/src/ffi/mod.rs

**Function to Add:**
```rust
/// Create a find my phone request packet
///
/// Creates a packet to make a remote device (usually a phone) ring
/// at maximum volume to help locate it. The packet has an empty body.
///
/// Sending this packet makes the phone ring. Sending it again should
/// cancel the ringing (implementation dependent on receiving side).
///
/// # Example
/// ```rust,no_run
/// use cosmic_connect_core::create_findmyphone_request;
///
/// let packet = create_findmyphone_request()?;
/// // Send packet to Android device...
/// # Ok::<(), cosmic_connect_core::error::ProtocolError>(())
/// ```
pub fn create_findmyphone_request() -> Result<FfiPacket> {
    use serde_json::json;

    let packet = Packet::new("kdeconnect.findmyphone.request".to_string(), json!({}));
    Ok(packet.into())
}
```

**Location in ffi/mod.rs:** After clipboard functions, around line 413

#### 2.2: Add UDL Definition

**Location:** cosmic-connect-core/src/cosmic_connect_core.udl

**Definition to Add (after clipboard functions, around line 192):**
```idl
  /// Create a find my phone request packet
  ///
  /// Creates a packet to make a remote device ring at maximum volume.
  /// Packet has an empty body.
  [Throws=ProtocolError]
  FfiPacket create_findmyphone_request();
```

#### 2.3: Export Function

**Location:** cosmic-connect-core/src/lib.rs

**Add to exports (around line 60):**
```rust
pub use ffi::{
    // ... existing exports ...
    create_findmyphone_request,
};
```

**Tasks:**
- [ ] Add `create_findmyphone_request()` to ffi/mod.rs
- [ ] Add UDL definition to cosmic_connect_core.udl
- [ ] Export function in lib.rs
- [ ] Build and verify: `cargo build --release`
- [ ] Run FFI tests: `cargo test --lib ffi`

**Success Criteria:**
- ✅ FFI function creates packet with correct type
- ✅ Empty body `{}`
- ✅ UDL definition matches function signature
- ✅ Function exported and accessible
- ✅ No compilation errors

**Deliverables:**
- Updated cosmic-connect-core/src/ffi/mod.rs (+15 lines)
- Updated cosmic-connect-core/src/cosmic_connect_core.udl (+7 lines)
- Updated cosmic-connect-core/src/lib.rs (+1 line)

**Time Estimate:** 30 minutes
**Actual Time:** TBD

---

### Phase 3: Android Wrapper Creation ⏸️ (Pending)

**Goal:** Create FindMyPhonePacketsFFI.kt wrapper for type-safe packet handling

**File to Create:** src/org/cosmic/cosmicconnect/Plugins/FindMyPhonePlugin/FindMyPhonePacketsFFI.kt

**Implementation:**
```kotlin
package org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createFindmyphoneRequest

/**
 * FindMyPhonePacketsFFI - FFI wrapper for Find My Phone plugin
 *
 * This object provides type-safe packet creation for the Find My Phone plugin
 * using the cosmic-connect-core Rust FFI layer.
 *
 * ## Packet Type
 *
 * - `kdeconnect.findmyphone.request` - Request to make phone ring
 *
 * ## Usage
 *
 * ```kotlin
 * // Create ring request packet
 * val packet = FindMyPhonePacketsFFI.createRingRequest()
 * device.sendPacket(packet)
 * ```
 *
 * Note: On Android, this plugin primarily RECEIVES ring requests from desktop.
 * This factory method is provided for completeness and potential future use.
 */
object FindMyPhonePacketsFFI {

    /**
     * Create a find my phone ring request packet
     *
     * Creates a packet that makes the remote device ring at maximum volume.
     * The packet has an empty body as no additional data is needed.
     *
     * @return NetworkPacket ready to send
     */
    fun createRingRequest(): NetworkPacket {
        val ffiPacket = createFindmyphoneRequest()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// ==========================================================================
// Extension Properties for Packet Type Inspection
// ==========================================================================

/**
 * Check if packet is a find my phone request
 */
val NetworkPacket.isFindMyPhoneRequest: Boolean
    get() = type == "kdeconnect.findmyphone.request"
```

**Tasks:**
- [ ] Create FindMyPhonePacketsFFI.kt
- [ ] Add factory method: `createRingRequest()`
- [ ] Add extension property: `isFindMyPhoneRequest`
- [ ] Add comprehensive KDoc comments
- [ ] Build and verify: `./gradlew compileDebugKotlin`

**Success Criteria:**
- ✅ Wrapper calls FFI function correctly
- ✅ Extension property checks packet type
- ✅ Comprehensive documentation
- ✅ Compiles without errors

**Deliverables:**
- FindMyPhonePacketsFFI.kt (~80 lines)

**Time Estimate:** 30 minutes
**Actual Time:** TBD

---

### Phase 4: Android Integration (Java → Kotlin) ⏸️ (Pending)

**Goal:** Convert FindMyPhonePlugin.java to Kotlin and integrate with FFI wrapper

**Current Features (Java):**
1. **Packet Reception:**
   - Receives `kdeconnect.findmyphone.request`
   - Triggers phone ringing

2. **MediaPlayer Management:**
   - Loads custom ringtone from settings
   - Plays in loop until cancelled
   - Sets USAGE_ALARM audio attributes
   - Wake lock to keep screen dim

3. **Volume Control:**
   - Saves current ALARM volume
   - Sets to maximum volume while ringing
   - Restores original volume when stopped

4. **Notification Management:**
   - Shows notification while ringing
   - Different notification types:
     - BroadcastReceiver action (Android 10+, screen on)
     - Activity intent (Android 10+, screen off)
     - Direct activity launch (Android <10)
   - Full screen intent for visibility
   - "Found It" action to stop ringing

5. **Device-Specific UI:**
   - Different titles: Phone/Tablet/TV
   - Appropriate descriptions per device type

6. **Permissions:**
   - POST_NOTIFICATIONS (Android 13+)
   - WAKE_LOCK (for screen dim)

7. **Settings:**
   - Custom ringtone selection
   - Settings fragment integration

**Tasks:**
- [ ] Create FindMyPhonePlugin.kt
- [ ] Convert packet reception with extension property
- [ ] Convert MediaPlayer initialization and management
- [ ] Convert volume control logic
- [ ] Convert notification creation (broadcast vs activity)
- [ ] Convert device-specific display names
- [ ] Convert permission checks
- [ ] Convert settings integration
- [ ] Remove manual packet creation (none exists - receive only)
- [ ] Delete FindMyPhonePlugin.java
- [ ] Build and verify compilation

**Kotlin Modernization:**
- Use `when` expression for packet type checking
- Extension property `isFindMyPhoneRequest`
- Null-safe operators for MediaPlayer checks
- Lazy initialization for managers
- Scoped functions for resource management
- Comprehensive KDoc comments

**Success Criteria:**
- ✅ FindMyPhonePlugin.kt compiles without errors
- ✅ All features preserved
- ✅ Extension property used for packet inspection
- ✅ MediaPlayer lifecycle managed properly
- ✅ Volume control working
- ✅ Notifications displaying correctly
- ✅ Settings accessible
- ✅ Permissions checked

**Deliverables:**
- src/org/cosmic/cosmicconnect/Plugins/FindMyPhonePlugin/FindMyPhonePlugin.kt (~260 lines)

**Time Estimate:** 2-2.5 hours
**Actual Time:** TBD

---

### Phase 5: Testing & Documentation ⏸️ (Pending)

**Goal:** Create comprehensive tests and documentation

#### 5.1: FFI Validation Tests

**Tasks:**
- [ ] Add Test 3.7: FindMyPhone Plugin FFI to FFIValidationTest.kt
- [ ] Test `createRingRequest()` packet creation
- [ ] Verify packet type: `kdeconnect.findmyphone.request`
- [ ] Verify empty body
- [ ] Test extension property `isFindMyPhoneRequest`
- [ ] Run tests and verify all pass

**Test Structure:**
```kotlin
@Test
fun testFindMyPhonePlugin() {
    Log.i(TAG, "=== Test 3.7: FindMyPhone Plugin FFI ===")

    // Test 1: Create ring request packet
    val ringPacket = createFindmyphoneRequest()
    assertNotNull(ringPacket)
    assertEquals("kdeconnect.findmyphone.request", ringPacket.packetType)
    assertTrue("Body should be empty", ringPacket.body.isEmpty())

    // Test 2: Extension property
    assertTrue("Should be recognized as findmyphone request", ringPacket.isFindMyPhoneRequest)

    Log.i(TAG, "✅ FindMyPhone plugin FFI test passed")
}
```

**Deliverables:**
- Updated tests/org/cosmic/cosmicconnect/FFIValidationTest.kt (~30 new lines)

**Time Estimate:** 30 minutes

#### 5.2: Testing Guide

**Tasks:**
- [ ] Create comprehensive testing guide
- [ ] Document manual test procedures:
  - Basic ring request from desktop
  - Volume control verification
  - Notification display
  - "Found It" action
  - Custom ringtone setting
  - Screen on/off behavior
  - Permission checks
- [ ] Document automated test procedures
- [ ] Create troubleshooting section
- [ ] Document expected behaviors

**Deliverables:**
- docs/issue-59-testing-guide.md (~600 lines)

**Time Estimate:** 1 hour

#### 5.3: Completion Summary

**Tasks:**
- [ ] Document all changes made in all phases
- [ ] List commits and file changes
- [ ] Document migration metrics
- [ ] Note any issues encountered
- [ ] Provide testing results summary

**Deliverables:**
- docs/issue-59-completion-summary.md (~500 lines)

**Time Estimate:** 30 minutes

**Total Phase 5 Time Estimate:** 2 hours
**Actual Time:** TBD

---

## Technical Architecture

### Packet Format

#### Ring Request Packet
```json
{
  "type": "kdeconnect.findmyphone.request",
  "id": 1234567890,
  "body": {}
}
```

**Usage:** Sent by desktop to make phone ring

**Note:** Empty body - no additional data needed

---

## FindMyPhone Flow

### Desktop → Android
1. User clicks "Find My Phone" in COSMIC applet
2. Desktop creates `kdeconnect.findmyphone.request` packet (empty body)
3. Packet sent to Android device
4. Android receives packet
5. Android plays ringtone at max volume
6. Android shows notification with "Found It" action
7. User taps "Found It" or dismisses notification
8. Ringing stops, volume restored

### Android Behavior Details

**When Request Received:**

1. **Android < 10 OR App in Foreground:**
   - Launch FindMyPhoneActivity directly
   - Activity handles MediaPlayer and UI

2. **Android 10+ AND App in Background:**
   - Check POST_NOTIFICATIONS permission
   - **If screen ON:**
     - Start playing immediately
     - Show broadcast notification (with "Found It" action)
   - **If screen OFF:**
     - Show activity notification (launches activity when tapped)
     - Wait for user interaction

**MediaPlayer Configuration:**
- Stream: ALARM (bypasses silent mode)
- Usage: USAGE_ALARM
- Wake Mode: SCREEN_DIM (keeps screen dim but on)
- Looping: Yes (until stopped)
- Volume: Maximum ALARM volume (saves and restores original)

---

## Known Issues and Limitations

### 1. Disabled Rust Module

**Issue:** findmyphone.rs is commented out in plugins/mod.rs
**Reason:** Requires Device architecture refactoring
**Impact:** Tests in findmyphone.rs can't run
**Mitigation:** FFI functions work independently
**Priority:** Low (doesn't affect Android integration)

### 2. Duplicate Initialize Method

**Issue:** findmyphone.rs has two `initialize()` methods (lines 88, 94)
**Impact:** Compilation error if module enabled
**Mitigation:** Not a blocker since module is disabled
**Priority:** Low (will be fixed if module is re-enabled)

### 3. Android 10+ Background Restrictions

**Issue:** Can't play sound in background without notification
**Reason:** Android platform restriction
**Workaround:** Show notification with "Found It" action
**Status:** By design (Android limitation)

### 4. Silent Mode Bypass

**Behavior:** Ring plays even when phone is in silent mode
**Reason:** Uses ALARM stream (by design)
**Impact:** Expected behavior to help locate phone
**Status:** Feature, not bug

---

## Testing Strategy

### Unit Tests (FFI Validation)
- Test createFindmyphoneRequest() packet creation
- Test extension property isFindMyPhoneRequest
- Verify empty body
- Verify packet type

### Manual Tests
1. **Basic Ring:** Send request from desktop, verify phone rings
2. **Volume Control:** Verify max volume, verify restoration
3. **Silent Mode:** Verify ring plays even in silent mode
4. **Screen On/Off:** Test both screen states (Android 10+)
5. **Notifications:** Verify notification shows with "Found It"
6. **Custom Ringtone:** Set custom ringtone, verify it plays
7. **Permission Check:** Revoke POST_NOTIFICATIONS, verify behavior
8. **Device Types:** Test on phone, tablet (if available)
9. **Lifecycle:** Test onCreate, onDestroy, MediaPlayer cleanup
10. **Multiple Requests:** Send request twice, verify cancellation

### Performance Tests
- Latency: Request → Ring start (target < 1 second)
- MediaPlayer load time
- Volume restore accuracy

---

## Risks and Mitigations

### Risk 1: MediaPlayer Initialization Failure
**Mitigation:** Try/catch in onCreate, return false on failure, use default ringtone as fallback

### Risk 2: Permission Denied (Android 13+)
**Mitigation:** Check permissions before showing notification, graceful fallback to activity launch

### Risk 3: Volume Control Issues
**Mitigation:** Save original volume before changing, restore in finally block or onDestroy

### Risk 4: Wake Lock Not Released
**Mitigation:** Ensure MediaPlayer.release() is called in onDestroy, use try-finally

---

## Success Criteria

Issue #59 is considered complete when:

✅ **All phases complete:**
- Phase 0: Planning document created
- Phase 1: Rust implementation verified
- Phase 2: FFI function created and working
- Phase 3: Android wrapper created
- Phase 4: FindMyPhonePlugin.kt created and functional
- Phase 5: Tests and documentation complete

✅ **All tests pass:**
- FFI validation test (ring request creation)
- Manual testing checklist complete
- No regressions identified

✅ **Documentation complete:**
- Testing guide created
- Completion summary written
- Migration metrics documented

✅ **Code quality:**
- No compilation errors
- Follows Kotlin conventions
- Comprehensive documentation
- Uses FFI wrapper

---

## Timeline

| Phase | Name | Estimate | Actual |
|-------|------|----------|--------|
| 0 | Planning | 0.5h | TBD |
| 1 | Rust Verification | 0.25h | TBD |
| 2 | FFI Implementation | 0.5h | TBD |
| 3 | Android Wrapper | 0.5h | TBD |
| 4 | Java → Kotlin | 2.5h | TBD |
| 5 | Testing & Docs | 2h | TBD |
| **Total** | | **6.25h** | **TBD** |

---

## References

### Related Issues
- Issue #54: SharePlugin FFI Migration (completed)
- Issue #55: TelephonyPlugin FFI Migration (completed)
- Issue #56: BatteryPlugin FFI Migration (completed)
- Issue #57: NotificationsPlugin FFI Migration (completed)
- Issue #58: ClipboardPlugin FFI Migration (completed)

### Code References
- cosmic-connect-core/src/plugins/findmyphone.rs (Rust implementation)
- src/org/cosmic/cosmicconnect/Plugins/FindMyPhonePlugin/FindMyPhonePlugin.java (Current)

### External Documentation
- KDE Connect Protocol v7: https://invent.kde.org/network/kdeconnect-kde
- Android MediaPlayer: https://developer.android.com/reference/android/media/MediaPlayer
- Android AudioManager: https://developer.android.com/reference/android/media/AudioManager

---

**Status:** Phase 0 (Planning) In Progress
**Next Step:** Complete Phase 0 and begin Phase 1 (Rust Verification)
**Blocked By:** None
**Blocking:** None

---

*This plan follows the established migration pattern from Issues #54-58. FindMyPhonePlugin requires creating new FFI infrastructure but has simpler packet structure than previous plugins.*
