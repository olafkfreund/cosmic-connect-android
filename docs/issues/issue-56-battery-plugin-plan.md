# Issue #56: Battery Plugin FFI Migration Plan

**Created**: 2026-01-16
**Status**: Planning
**Priority**: High
**Estimated Effort**: 6-8 hours
**Pattern**: Packet-based FFI (Issues #54, #55)

---

## Executive Summary

Migrate the Battery Plugin to use the **packet-based FFI approach** established in Issues #54 (Clipboard) and #55 (Telephony). The current `BatteryPluginFFI.kt` uses a PluginManager-based approach which is inconsistent with our new pattern.

**Goal**: Replace PluginManager approach with type-safe packet creation functions, matching clipboard/telephony patterns.

---

## Current State Analysis

### Existing Files

1. **BatteryPlugin.kt** (122 lines)
   - Legacy NetworkPacket-based implementation
   - Manual packet construction with mutable NetworkPacket
   - Direct field manipulation (`batteryInfo["currentCharge"] = value`)

2. **BatteryPluginFFI.kt** (295 lines)
   - Uses PluginManager FFI approach (different from our pattern)
   - Calls `pluginManager.updateBattery(state)` directly
   - Calls `pluginManager.getRemoteBattery()` for remote state
   - **Problem**: Inconsistent with clipboard/telephony packet-based FFI

3. **battery.rs** (369 lines)
   - Has `BatteryState` struct and `BatteryPlugin`
   - Has `create_battery_packet()` method
   - **Problem**: No FFI packet creation functions

### What Needs to Change

**Goal**: Make battery plugin consistent with clipboard/telephony:
- Add FFI packet creation functions to `ffi/mod.rs`
- Create `BatteryPacketsFFI.kt` wrapper (like `ClipboardPacketsFFI.kt`)
- Update `BatteryPlugin.kt` to use packet-based FFI (not PluginManager)

---

## Protocol Specification

### Packet Types

#### 1. kdeconnect.battery (Incoming & Outgoing)
**Direction**: Bi-directional
**Purpose**: Share battery state between devices

**JSON Format**:
```json
{
  "isCharging": true,
  "currentCharge": 85,
  "thresholdEvent": 0
}
```

**Fields**:
- `isCharging` (boolean): Whether device is charging
- `currentCharge` (int): Battery percentage (0-100)
- `thresholdEvent` (int): Threshold event indicator
  - `0`: No event
  - `1`: Battery low (< 15%)

#### 2. kdeconnect.battery.request (Incoming)
**Direction**: Desktop → Android
**Purpose**: Request current battery status

**JSON Format**:
```json
{}
```

**Fields**: Empty body (request only)

---

## Migration Strategy

### Approach: Packet-Based FFI (Not PluginManager)

We'll follow the proven pattern from Issues #54 and #55:

1. **Phase 1**: Refactor Rust (battery.rs)
   - Keep existing code structure
   - No major changes needed (already clean)

2. **Phase 2**: Add FFI packet creation functions (ffi/mod.rs)
   - `create_battery_packet(is_charging, current_charge, threshold_event)` →BatteryState
   - `create_battery_request()` → Empty packet

3. **Phase 3**: Create BatteryPacketsFFI.kt wrapper
   - Type-safe packet creation methods
   - Extension properties for packet inspection
   - Java-compatible helper functions

4. **Phase 4**: Update BatteryPlugin.kt (not BatteryPluginFFI.kt)
   - Replace manual packet construction with `BatteryPacketsFFI.createBatteryPacket()`
   - Replace type checking with extension properties
   - **Decision**: Update original BatteryPlugin.kt, deprecate BatteryPluginFFI.kt

5. **Phase 5**: Testing & Documentation
   - Comprehensive testing guide
   - Completion summary
   - Deprecation notes for BatteryPluginFFI.kt

---

## Detailed Phase Breakdown

### Phase 0: Planning (1 hour) ✅
**Status**: Complete
**Deliverables**:
- This plan document

**Key Decisions**:
1. Use packet-based FFI (not PluginManager approach)
2. Update original BatteryPlugin.kt (deprecate BatteryPluginFFI.kt)
3. Create BatteryPacketsFFI.kt following clipboard/telephony pattern
4. 2 packet types (battery, battery.request)

---

### Phase 1: Rust Refactoring (0.5 hours)
**Status**: Pending
**Files**: `cosmic-connect-core/src/plugins/battery.rs`

**Tasks**:
1. Review current implementation
2. Verify BatteryState struct is correct
3. Verify Plugin trait implementation
4. No major changes expected (already clean)

**Expected Changes**: None (code already follows pattern)

**Testing**:
```bash
cd /home/olafkfreund/Source/GitHub/cosmic-connect-core
cargo test battery
```

---

### Phase 2: FFI Interface Implementation (1.5 hours)
**Status**: Pending
**Files**:
1. `cosmic-connect-core/src/ffi/mod.rs`
2. `cosmic-connect-core/src/cosmic_connect_core.udl`
3. `cosmic-connect-core/src/lib.rs`

**Functions to Implement** (2 total):

#### Function 1: create_battery_packet()
```rust
/// Create a battery status packet
///
/// Creates a packet containing current battery state.
///
/// # Arguments
/// * `is_charging` - Whether device is charging
/// * `current_charge` - Battery percentage (0-100)
/// * `threshold_event` - Threshold event (0=none, 1=low)
///
/// # Example
/// ```rust,no_run
/// let packet = create_battery_packet(true, 85, 0)?;
/// // Send packet...
/// ```
pub fn create_battery_packet(
    is_charging: bool,
    current_charge: i32,
    threshold_event: i32,
) -> Result<FfiPacket> {
    use serde_json::json;

    let packet = Packet::new(
        "kdeconnect.battery".to_string(),
        json!({
            "isCharging": is_charging,
            "currentCharge": current_charge.clamp(0, 100),
            "thresholdEvent": threshold_event,
        }),
    );
    Ok(packet.into())
}
```

#### Function 2: create_battery_request()
```rust
/// Create a battery status request packet
///
/// Creates a packet requesting the remote device's battery status.
///
/// # Example
/// ```rust,no_run
/// let packet = create_battery_request()?;
/// // Send packet...
/// ```
pub fn create_battery_request() -> Result<FfiPacket> {
    use serde_json::json;

    let packet = Packet::new("kdeconnect.battery.request".to_string(), json!({}));
    Ok(packet.into())
}
```

**UDL Updates**:
```udl
  // ========================================================================
  // Battery Plugin
  // ========================================================================

  /// Create a battery status packet
  [Throws=ProtocolError]
  FfiPacket create_battery_packet(
    boolean is_charging,
    i32 current_charge,
    i32 threshold_event
  );

  /// Create a battery status request packet
  [Throws=ProtocolError]
  FfiPacket create_battery_request();
```

**Exports** (lib.rs):
```rust
pub use ffi::{
    // ... existing exports
    create_battery_packet, create_battery_request,
};
```

**Estimated Lines**: ~50 lines total

**Testing**:
```bash
cargo build --release
cargo test --features=uniffi
```

---

### Phase 3: Android Wrapper Creation (2 hours)
**Status**: Pending
**Files**: `src/org/cosmic/cosmicconnect/Plugins/BatteryPlugin/BatteryPacketsFFI.kt` (new file)

**Structure** (following clipboard/telephony pattern):

```kotlin
object BatteryPacketsFFI {
    // Packet creation methods (2 functions)
    fun createBatteryPacket(
        isCharging: Boolean,
        currentCharge: Int,
        thresholdEvent: Int
    ): NetworkPacket

    fun createBatteryRequest(): NetworkPacket
}

// Extension properties for type-safe inspection (6 properties)
val NetworkPacket.isBatteryPacket: Boolean
val NetworkPacket.isBatteryRequest: Boolean
val NetworkPacket.batteryIsCharging: Boolean?
val NetworkPacket.batteryCurrentCharge: Int?
val NetworkPacket.batteryThresholdEvent: Int?
val NetworkPacket.isBatteryLow: Boolean

// Java-compatible functions (6 functions)
fun getIsBatteryPacket(packet: NetworkPacket): Boolean
fun getIsBatteryRequest(packet: NetworkPacket): Boolean
fun getBatteryIsCharging(packet: NetworkPacket): Boolean?
fun getBatteryCurrentCharge(packet: NetworkPacket): Int?
fun getBatteryThresholdEvent(packet: NetworkPacket): Int?
fun getIsBatteryLow(packet: NetworkPacket): Boolean
```

**Validation**:
- `currentCharge` must be 0-100
- `thresholdEvent` must be 0 or 1
- All parameters validated with clear error messages

**Documentation**:
- Comprehensive KDoc for every function/property
- Usage examples
- Protocol details
- Validation rules

**Estimated Lines**: ~300-350 lines

---

### Phase 4: Android Integration (1.5 hours)
**Status**: Pending
**Files**: `src/org/cosmic/cosmicconnect/Plugins/BatteryPlugin/BatteryPlugin.kt`

**Changes to Make**:

#### 1. Packet Creation (lines 20-79)
**Before**:
```kotlin
private val batteryInfo = NetworkPacket(PACKET_TYPE_BATTERY)

// In receiver
batteryInfo["currentCharge"] = currentCharge
batteryInfo["isCharging"] = isCharging
batteryInfo["thresholdEvent"] = thresholdEvent
device.sendPacket(batteryInfo)
```

**After**:
```kotlin
// In receiver
val packet = BatteryPacketsFFI.createBatteryPacket(
    isCharging = isCharging,
    currentCharge = currentCharge,
    thresholdEvent = thresholdEvent
)
val legacyPacket = convertToLegacyPacket(packet)
device.sendPacket(legacyPacket)
```

#### 2. Packet Receiving (lines 98-105)
**Before**:
```kotlin
override fun onPacketReceived(np: NetworkPacket): Boolean {
    if (PACKET_TYPE_BATTERY != np.type) {
        return false
    }
    remoteBatteryInfo = DeviceBatteryInfo.fromPacket(np)
    device.onPluginsChanged()
    return true
}
```

**After**:
```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    val packet = NetworkPacket.fromLegacyPacket(np)

    when {
        packet.isBatteryPacket -> {
            remoteBatteryInfo = DeviceBatteryInfo(
                currentCharge = packet.batteryCurrentCharge ?: 0,
                isCharging = packet.batteryIsCharging ?: false,
                thresholdEvent = packet.batteryThresholdEvent ?: 0
            )
            device.onPluginsChanged()
            return true
        }
        packet.isBatteryRequest -> {
            // Send current battery state
            sendBatteryUpdate()
            return true
        }
        else -> return false
    }
}
```

**Benefits**:
- Type-safe packet creation
- Extension property-based inspection
- Cleaner code (-10-15 lines)
- Validation built-in

**Estimated Changes**: -15 lines net

---

### Phase 5: Testing & Documentation (1.5 hours)
**Status**: Pending
**Files**:
1. `docs/issues/issue-56-testing-guide.md`
2. `docs/issues/issue-56-completion-summary.md`

**Testing Guide Contents**:
- Phase 1: FFI Layer Testing (2 Rust functions)
- Phase 2: Kotlin Wrapper Testing (packet creation, extensions, Java compat)
- Phase 3: Plugin Integration Testing (battery monitoring, state changes)
- Phase 4: End-to-End Testing (Android ↔ COSMIC communication)
- Phase 5: Performance Testing (packet creation speed, memory)
- Phase 6: Regression Testing (permissions, platform compatibility)

**Test Cases**: 15-20 total

**Completion Summary Contents**:
- Phase-by-phase metrics
- Code statistics
- Time tracking
- Known issues
- Sign-off checklist

---

## Comparison with Issues #54 and #55

| Metric | Clipboard #54 | Telephony #55 | Battery #56 |
|--------|---------------|---------------|-------------|
| **FFI Functions** | 2 | 7 | 2 |
| **Extension Properties** | 4 | 22 | 6 |
| **Java Helpers** | 4 | 17 | 6 |
| **Packet Types** | 2 | 7 | 2 |
| **Complexity** | Low | Medium | Low |
| **Estimated Effort** | 6-8h | 10-12h | 6-8h |
| **Actual Effort** | 7h | 11h | TBD |

**Conclusion**: Battery plugin is simpler than telephony, similar to clipboard.

---

## Key Decisions

### Decision 1: Packet-Based FFI (Not PluginManager)
**Rationale**:
- Consistency with clipboard (#54) and telephony (#55)
- Type-safe packet creation
- Clear extension properties for inspection
- Better testability
- Easier to maintain

**Alternative Considered**: Keep PluginManager approach
**Rejected Because**: Inconsistent pattern, harder to test, less type-safe

### Decision 2: Update BatteryPlugin.kt (Deprecate BatteryPluginFFI.kt)
**Rationale**:
- BatteryPlugin.kt is the official plugin (loaded by @LoadablePlugin)
- BatteryPluginFFI.kt was experimental PluginManager approach
- Easier to maintain one implementation
- Users won't notice the change (internal refactoring)

**Alternative Considered**: Keep both plugins
**Rejected Because**: Code duplication, maintenance burden

### Decision 3: Minimal Rust Changes
**Rationale**:
- battery.rs is already clean and well-structured
- No need to refactor working code
- Focus effort on FFI layer and Android side

---

## Timeline & Effort Estimate

| Phase | Description | Estimated | Cumulative |
|-------|-------------|-----------|------------|
| 0 | Planning | 1h | 1h |
| 1 | Rust refactoring | 0.5h | 1.5h |
| 2 | FFI interface | 1.5h | 3h |
| 3 | Android wrapper | 2h | 5h |
| 4 | Android integration | 1.5h | 6.5h |
| 5 | Testing & docs | 1.5h | **8h** |

**Total Estimated**: 8 hours (conservative)
**Expected Actual**: 6-7 hours (based on #54, #55 experience)

---

## Success Criteria

### Functional Requirements ✅
- [ ] Battery state packets created via FFI
- [ ] Battery request packets created via FFI
- [ ] Extension properties work for inspection
- [ ] Java-compatible helpers work
- [ ] Feature parity with original BatteryPlugin

### Technical Requirements ✅
- [ ] Follows clipboard/telephony pattern
- [ ] Type-safe packet creation
- [ ] Comprehensive validation
- [ ] Clean error messages
- [ ] Well-documented code

### Quality Requirements ✅
- [ ] All unit tests passing
- [ ] Rust tests passing (battery.rs)
- [ ] Kotlin tests passing (BatteryPacketsFFI.kt)
- [ ] End-to-end testing complete
- [ ] Performance benchmarks met (< 1ms packet creation)

### Documentation Requirements ✅
- [ ] Comprehensive testing guide
- [ ] Completion summary with metrics
- [ ] Code examples in all documentation
- [ ] Known issues documented

---

## Risk Assessment

### Low Risk ✅
**Rationale**:
- Simpler than telephony (2 vs 7 packet types)
- Pattern is well-established (issues #54, #55)
- Rust code is already clean
- No complex permissions required
- No SMS/multimedia complexity

### Potential Issues
1. **BatteryPluginFFI.kt Deprecation**
   - Risk: Low
   - Mitigation: Add deprecation notice, keep file for reference

2. **DeviceBatteryInfo Compatibility**
   - Risk: Low
   - Mitigation: Keep DeviceBatteryInfo.kt unchanged, just update construction

3. **Threshold Event Logic**
   - Risk: Low
   - Mitigation: Test low battery scenarios thoroughly

---

## Dependencies

### Completed Dependencies ✅
- Issue #64: NetworkPacket migration (immutable packets)
- Issue #45: Protocol implementation
- Issue #46: Discovery service
- Issue #54: Clipboard FFI migration (pattern source)
- Issue #55: Telephony FFI migration (pattern refinement)

### No Blocking Dependencies
All dependencies are complete.

---

## Post-Migration Tasks

### Immediate (After Phase 5)
1. Add deprecation notice to BatteryPluginFFI.kt
2. Update FFI Integration Guide with battery examples
3. Update project status document

### Future (Optional)
1. Remove BatteryPluginFFI.kt entirely (1-2 months after migration)
2. Add battery widget to COSMIC Desktop applet
3. Add battery notification customization

---

## Files to Create/Modify

### cosmic-connect-core Repository
```
src/ffi/mod.rs                   [MODIFY] +50 lines
src/cosmic_connect_core.udl      [MODIFY] +20 lines
src/lib.rs                       [MODIFY] +2 lines
src/plugins/battery.rs           [VERIFY] no changes
```

### cosmic-connect-android Repository
```
src/.../BatteryPacketsFFI.kt     [CREATE] +350 lines
src/.../BatteryPlugin.kt         [MODIFY] -15 lines net
src/.../BatteryPluginFFI.kt      [DEPRECATE] add notice
docs/issues/issue-56-plan.md    [CREATE] +400 lines (this doc)
docs/issues/issue-56-testing.md [CREATE] +400 lines
docs/issues/issue-56-summary.md [CREATE] +300 lines
```

---

## Appendix A: Code Examples

### Example 1: Creating Battery Packet (Kotlin)
```kotlin
// Before (manual construction)
val batteryInfo = NetworkPacket(PACKET_TYPE_BATTERY)
batteryInfo["currentCharge"] = 85
batteryInfo["isCharging"] = true
batteryInfo["thresholdEvent"] = 0

// After (FFI)
val packet = BatteryPacketsFFI.createBatteryPacket(
    isCharging = true,
    currentCharge = 85,
    thresholdEvent = 0
)
```

### Example 2: Inspecting Battery Packet (Kotlin)
```kotlin
// Before (manual checking)
if (packet.type == "kdeconnect.battery") {
    val charge = packet.getInt("currentCharge")
    val isCharging = packet.getBoolean("isCharging")
    // ...
}

// After (extension properties)
if (packet.isBatteryPacket) {
    val charge = packet.batteryCurrentCharge ?: 0
    val isCharging = packet.batteryIsCharging ?: false
    if (packet.isBatteryLow) {
        // Show low battery warning
    }
}
```

### Example 3: FFI Function (Rust)
```rust
pub fn create_battery_packet(
    is_charging: bool,
    current_charge: i32,
    threshold_event: i32,
) -> Result<FfiPacket> {
    let packet = Packet::new(
        "kdeconnect.battery".to_string(),
        json!({
            "isCharging": is_charging,
            "currentCharge": current_charge.clamp(0, 100),
            "thresholdEvent": threshold_event,
        }),
    );
    Ok(packet.into())
}
```

---

## Appendix B: Testing Checklist

### Unit Tests
- [ ] Rust: `create_battery_packet()` with valid inputs
- [ ] Rust: `create_battery_packet()` with out-of-range charge (0-100 clamping)
- [ ] Rust: `create_battery_request()` creates empty packet
- [ ] Kotlin: `createBatteryPacket()` with valid inputs
- [ ] Kotlin: `createBatteryPacket()` with invalid charge throws error
- [ ] Kotlin: Extension property `isBatteryPacket` returns true/false
- [ ] Kotlin: Extension property `batteryCurrentCharge` extracts value
- [ ] Kotlin: Extension property `isBatteryLow` calculates correctly

### Integration Tests
- [ ] Android battery receiver triggers packet creation
- [ ] Battery state changes send updated packets
- [ ] Battery request from desktop triggers response
- [ ] Low battery threshold event works correctly
- [ ] Charging state changes detected
- [ ] Remote battery info updates correctly

### End-to-End Tests
- [ ] Android → COSMIC Desktop battery status
- [ ] COSMIC Desktop → Android battery request
- [ ] Low battery notification on COSMIC Desktop
- [ ] Battery widget updates in real-time
- [ ] Works with KDE Connect (backward compatibility)

---

**Status**: Plan Complete ✅
**Ready to Start**: Phase 1 (Rust Refactoring)
**Next Step**: Review battery.rs implementation

---

**End of Plan Document**
