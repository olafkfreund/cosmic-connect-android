# Issue #56: Battery Plugin FFI Migration - Testing Guide

**Issue:** #56
**Component:** Battery Plugin
**Status:** Testing Phase
**Created:** 2026-01-16
**Updated:** 2026-01-16

## Overview

This guide provides comprehensive testing instructions for the Battery Plugin FFI migration. The battery plugin shares battery status between Android and COSMIC Desktop, supporting both status updates and battery requests.

### Migration Scope

- **Rust Core:** `cosmic-connect-core/src/plugins/battery.rs` (verified, no changes needed)
- **FFI Layer:** 2 new functions in `cosmic-connect-core/src/ffi/mod.rs`
- **Kotlin Wrapper:** `BatteryPacketsFFI.kt` (380 lines)
- **Plugin Integration:** `BatteryPlugin.kt` (updated to use FFI)

### Packet Types

1. **kdeconnect.battery** - Battery status (bi-directional)
   - Fields: `isCharging` (boolean), `currentCharge` (0-100), `thresholdEvent` (0=none, 1=low)

2. **kdeconnect.battery.request** - Request battery status (incoming)
   - No fields (empty body)

## Testing Layers

### Layer 1: FFI Layer (Rust)

#### Test 1.1: Battery Packet Creation
**Location:** `cosmic-connect-core/src/ffi/mod.rs::create_battery_packet()`

**Test Cases:**
```bash
cd /home/olafkfreund/Source/GitHub/cosmic-connect-core
cargo test create_battery_packet
```

**Expected Behavior:**
- Accepts `is_charging`, `current_charge`, `threshold_event` parameters
- Clamps `current_charge` to 0-100 range
- Creates packet with type "kdeconnect.battery"
- Returns `FfiPacket` with correct fields

**Manual Test:**
```rust
#[test]
fn test_ffi_battery_packet_creation() {
    // Normal case
    let packet = create_battery_packet(true, 85, 0).unwrap();
    assert_eq!(packet.packet_type, "kdeconnect.battery");

    // Out-of-range clamping
    let packet_low = create_battery_packet(false, -10, 1).unwrap();
    // Should clamp to 0

    let packet_high = create_battery_packet(true, 150, 0).unwrap();
    // Should clamp to 100
}
```

**Pass Criteria:**
- ✅ All existing battery.rs tests pass (11 tests)
- ✅ Packet type is "kdeconnect.battery"
- ✅ All fields present in body
- ✅ Charge values clamped to 0-100

#### Test 1.2: Battery Request Creation
**Location:** `cosmic-connect-core/src/ffi/mod.rs::create_battery_request()`

**Test Cases:**
```bash
cargo test create_battery_request
```

**Expected Behavior:**
- No parameters required
- Creates packet with type "kdeconnect.battery.request"
- Empty body
- Returns `FfiPacket`

**Manual Test:**
```rust
#[test]
fn test_ffi_battery_request_creation() {
    let packet = create_battery_request().unwrap();
    assert_eq!(packet.packet_type, "kdeconnect.battery.request");
    assert_eq!(packet.body, "{}");
}
```

**Pass Criteria:**
- ✅ Packet type is "kdeconnect.battery.request"
- ✅ Body is empty JSON object
- ✅ No errors returned

#### Test 1.3: UniFFI Bindings Generation
**Location:** Build system

**Test Cases:**
```bash
# Clean and rebuild to verify UniFFI scaffolding
cargo clean
cargo build --release
```

**Expected Behavior:**
- UniFFI scaffolding generates without errors
- Kotlin bindings include `createBatteryPacket()` and `createBatteryRequest()`
- Function signatures match UDL definition

**Pass Criteria:**
- ✅ Build succeeds
- ✅ No UniFFI errors or warnings
- ✅ Generated Kotlin files present in build output

### Layer 2: Kotlin Wrapper Testing

#### Test 2.1: BatteryPacketsFFI.createBatteryPacket()
**Location:** `BatteryPacketsFFI.kt::createBatteryPacket()`

**Test Cases:**

**Test 2.1.1: Valid Input**
```kotlin
@Test
fun testCreateBatteryPacket_validInput() {
    val packet = BatteryPacketsFFI.createBatteryPacket(
        isCharging = true,
        currentCharge = 85,
        thresholdEvent = 0
    )

    assertEquals("kdeconnect.battery", packet.type)
    assertTrue(packet.body.containsKey("isCharging"))
    assertTrue(packet.body.containsKey("currentCharge"))
    assertTrue(packet.body.containsKey("thresholdEvent"))
}
```

**Test 2.1.2: Threshold Event Validation**
```kotlin
@Test
fun testCreateBatteryPacket_invalidThresholdEvent() {
    assertThrows<IllegalArgumentException> {
        BatteryPacketsFFI.createBatteryPacket(
            isCharging = false,
            currentCharge = 50,
            thresholdEvent = 2  // Invalid: must be 0 or 1
        )
    }
}
```

**Test 2.1.3: Edge Cases**
```kotlin
@Test
fun testCreateBatteryPacket_edgeCases() {
    // Low battery warning
    val lowBattery = BatteryPacketsFFI.createBatteryPacket(
        isCharging = false,
        currentCharge = 12,
        thresholdEvent = 1
    )
    assertTrue(lowBattery.isBatteryLow)

    // Critical battery
    val criticalBattery = BatteryPacketsFFI.createBatteryPacket(
        isCharging = false,
        currentCharge = 3,
        thresholdEvent = 1
    )
    assertTrue(criticalBattery.isBatteryCritical)

    // Charging at low battery (not low)
    val chargingLow = BatteryPacketsFFI.createBatteryPacket(
        isCharging = true,
        currentCharge = 10,
        thresholdEvent = 0
    )
    assertFalse(chargingLow.isBatteryLow)
}
```

**Pass Criteria:**
- ✅ Valid inputs create correct packets
- ✅ Invalid threshold events throw IllegalArgumentException
- ✅ Edge cases handled correctly

#### Test 2.2: BatteryPacketsFFI.createBatteryRequest()
**Location:** `BatteryPacketsFFI.kt::createBatteryRequest()`

**Test Cases:**
```kotlin
@Test
fun testCreateBatteryRequest() {
    val packet = BatteryPacketsFFI.createBatteryRequest()

    assertEquals("kdeconnect.battery.request", packet.type)
    assertTrue(packet.isBatteryRequest)
    assertFalse(packet.isBatteryPacket)
}
```

**Pass Criteria:**
- ✅ Packet type correct
- ✅ Extension property identifies request correctly

#### Test 2.3: Extension Properties
**Location:** `BatteryPacketsFFI.kt` extension properties

**Test 2.3.1: Packet Type Detection**
```kotlin
@Test
fun testExtensionProperties_packetTypeDetection() {
    val batteryPacket = BatteryPacketsFFI.createBatteryPacket(true, 85, 0)
    assertTrue(batteryPacket.isBatteryPacket)
    assertFalse(batteryPacket.isBatteryRequest)

    val requestPacket = BatteryPacketsFFI.createBatteryRequest()
    assertTrue(requestPacket.isBatteryRequest)
    assertFalse(requestPacket.isBatteryPacket)

    // Non-battery packet
    val pingPacket = NetworkPacket("kdeconnect.ping", emptyMap())
    assertFalse(pingPacket.isBatteryPacket)
    assertFalse(pingPacket.isBatteryRequest)
}
```

**Test 2.3.2: Field Extraction**
```kotlin
@Test
fun testExtensionProperties_fieldExtraction() {
    val packet = BatteryPacketsFFI.createBatteryPacket(
        isCharging = true,
        currentCharge = 75,
        thresholdEvent = 0
    )

    assertEquals(true, packet.batteryIsCharging)
    assertEquals(75, packet.batteryCurrentCharge)
    assertEquals(0, packet.batteryThresholdEvent)
    assertFalse(packet.isBatteryLow)
    assertFalse(packet.isBatteryCritical)
}
```

**Test 2.3.3: Computed Properties**
```kotlin
@Test
fun testExtensionProperties_computedProperties() {
    // Battery low (< 15%, not charging)
    val lowPacket = BatteryPacketsFFI.createBatteryPacket(false, 12, 1)
    assertTrue(lowPacket.isBatteryLow)
    assertFalse(lowPacket.isBatteryCritical)

    // Battery critical (< 5%, not charging)
    val criticalPacket = BatteryPacketsFFI.createBatteryPacket(false, 3, 1)
    assertTrue(criticalPacket.isBatteryCritical)
    assertTrue(criticalPacket.isBatteryLow)  // Also low

    // Not low when charging
    val chargingLowPacket = BatteryPacketsFFI.createBatteryPacket(true, 10, 0)
    assertFalse(chargingLowPacket.isBatteryLow)

    // Not low when above threshold
    val normalPacket = BatteryPacketsFFI.createBatteryPacket(false, 50, 0)
    assertFalse(normalPacket.isBatteryLow)
}
```

**Test 2.3.4: Null Safety**
```kotlin
@Test
fun testExtensionProperties_nullSafety() {
    // Non-battery packet should return null
    val pingPacket = NetworkPacket("kdeconnect.ping", emptyMap())
    assertNull(pingPacket.batteryIsCharging)
    assertNull(pingPacket.batteryCurrentCharge)
    assertNull(pingPacket.batteryThresholdEvent)
    assertFalse(pingPacket.isBatteryLow)
    assertFalse(pingPacket.isBatteryCritical)
}
```

**Pass Criteria:**
- ✅ Type detection works for all packet types
- ✅ Field extraction returns correct values
- ✅ Computed properties calculate correctly
- ✅ Null safety for non-battery packets

#### Test 2.4: Java Compatibility Functions
**Location:** `BatteryPacketsFFI.kt` Java-compatible functions

**Test Cases:**
```kotlin
@Test
fun testJavaCompatibilityFunctions() {
    val packet = BatteryPacketsFFI.createBatteryPacket(true, 85, 0)

    // Test all Java-compatible functions
    assertTrue(getIsBatteryPacket(packet))
    assertFalse(getIsBatteryRequest(packet))
    assertEquals(true, getBatteryIsCharging(packet))
    assertEquals(85, getBatteryCurrentCharge(packet))
    assertEquals(0, getBatteryThresholdEvent(packet))
    assertFalse(getIsBatteryLow(packet))
    assertFalse(getIsBatteryCritical(packet))
}
```

**Pass Criteria:**
- ✅ All Java functions return same values as extension properties
- ✅ Can be called from Java code (compile test)

### Layer 3: Plugin Integration Testing

#### Test 3.1: BatteryPlugin State Tracking
**Location:** `BatteryPlugin.kt`

**Test 3.1.1: Initial State**
```kotlin
@Test
fun testBatteryPlugin_initialState() {
    val plugin = BatteryPlugin()
    // Should not have remote battery info initially
    assertNull(plugin.remoteBatteryInfo)
}
```

**Test 3.1.2: State Change Detection**
```kotlin
@Test
fun testBatteryPlugin_stateChangeDetection() {
    val plugin = BatteryPlugin()

    // Simulate battery state changes
    // First update should trigger send
    plugin.simulateBatteryChange(isCharging = true, charge = 85, threshold = 0)
    verify(device).sendPacket(any())

    // Same state should not trigger send
    plugin.simulateBatteryChange(isCharging = true, charge = 85, threshold = 0)
    verifyNoMoreInteractions(device)

    // Different state should trigger send
    plugin.simulateBatteryChange(isCharging = true, charge = 84, threshold = 0)
    verify(device, times(2)).sendPacket(any())
}
```

**Pass Criteria:**
- ✅ Initial state is uninitialized
- ✅ State changes trigger packet sends
- ✅ Duplicate states don't trigger sends

#### Test 3.2: Packet Creation via FFI
**Location:** `BatteryPlugin.kt::receiver.onReceive()`

**Test Cases:**
```kotlin
@Test
fun testBatteryPlugin_packetCreation() {
    val plugin = BatteryPlugin()
    plugin.onCreate()

    // Simulate battery broadcast
    val intent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
        putExtra(BatteryManager.EXTRA_LEVEL, 85)
        putExtra(BatteryManager.EXTRA_SCALE, 100)
        putExtra(BatteryManager.EXTRA_PLUGGED, 1)  // Charging
    }

    plugin.receiver.onReceive(context, intent)

    // Verify packet was created via FFI and sent
    argumentCaptor<NetworkPacket>().apply {
        verify(device).sendPacket(capture())

        val sentPacket = firstValue
        assertEquals("kdeconnect.battery", sentPacket.type)
        assertTrue(sentPacket.has("isCharging"))
        assertEquals(85, sentPacket.getInt("currentCharge"))
    }
}
```

**Pass Criteria:**
- ✅ Broadcast intents trigger packet creation
- ✅ Packets created via BatteryPacketsFFI
- ✅ Correct values in packet

#### Test 3.3: Battery Request Handling
**Location:** `BatteryPlugin.kt::onPacketReceived()`

**Test Cases:**
```kotlin
@Test
fun testBatteryPlugin_requestHandling() {
    val plugin = BatteryPlugin()
    plugin.onCreate()

    // Initialize battery state
    plugin.simulateBatteryChange(isCharging = true, charge = 75, threshold = 0)
    reset(device)

    // Receive battery request
    val requestPacket = BatteryPacketsFFI.createBatteryRequest()
    val legacyRequest = convertToLegacyPacket(requestPacket)

    val handled = plugin.onPacketReceived(legacyRequest)

    assertTrue(handled)
    verify(device).sendPacket(any())  // Should send current state
}
```

**Pass Criteria:**
- ✅ Battery requests are recognized
- ✅ Current battery state is sent in response
- ✅ Uninitialized state doesn't send

#### Test 3.4: Remote Battery Info
**Location:** `BatteryPlugin.kt::remoteBatteryInfo`

**Test Cases:**
```kotlin
@Test
fun testBatteryPlugin_remoteBatteryInfo() {
    val plugin = BatteryPlugin()

    // Receive battery packet from remote device
    val remotePacket = BatteryPacketsFFI.createBatteryPacket(
        isCharging = false,
        currentCharge = 45,
        thresholdEvent = 0
    )
    val legacyPacket = convertToLegacyPacket(remotePacket)

    val handled = plugin.onPacketReceived(legacyPacket)

    assertTrue(handled)
    assertNotNull(plugin.remoteBatteryInfo)
    assertEquals(45, plugin.remoteBatteryInfo!!.currentCharge)
    assertFalse(plugin.remoteBatteryInfo!!.isCharging)
    assertEquals(0, plugin.remoteBatteryInfo!!.thresholdEvent)
}
```

**Pass Criteria:**
- ✅ Remote battery packets update remoteBatteryInfo
- ✅ DeviceBatteryInfo fields correct
- ✅ device.onPluginsChanged() called

#### Test 3.5: Threshold Event Handling
**Location:** `BatteryPlugin.kt::receiver.onReceive()`

**Test Cases:**
```kotlin
@Test
fun testBatteryPlugin_thresholdEvents() {
    val plugin = BatteryPlugin()
    plugin.onCreate()

    // Battery okay event
    val okayIntent = Intent(Intent.ACTION_BATTERY_OKAY)
    plugin.receiver.onReceive(context, okayIntent)

    // Battery low event (not charging)
    val lowIntent = Intent(Intent.ACTION_BATTERY_LOW).apply {
        putExtra(BatteryManager.EXTRA_PLUGGED, 0)  // Not charging
    }
    plugin.receiver.onReceive(context, lowIntent)

    // Verify threshold event in packet
    argumentCaptor<NetworkPacket>().apply {
        verify(device, atLeastOnce()).sendPacket(capture())

        // Find the low battery packet
        val lowPacket = allValues.find {
            it.type == "kdeconnect.battery" &&
            it.getInt("thresholdEvent") == 1
        }
        assertNotNull(lowPacket)
    }
}
```

**Pass Criteria:**
- ✅ ACTION_BATTERY_LOW triggers threshold event
- ✅ ACTION_BATTERY_OKAY clears threshold event
- ✅ Only triggers when not charging

#### Test 3.6: Lifecycle Management
**Location:** `BatteryPlugin.kt::onCreate()` and `onDestroy()`

**Test Cases:**
```kotlin
@Test
fun testBatteryPlugin_lifecycle() {
    val plugin = BatteryPlugin()

    // onCreate should register receiver
    val created = plugin.onCreate()
    assertTrue(created)

    // Verify receiver registered
    verify(context).registerReceiver(
        eq(plugin.receiver),
        any<IntentFilter>()
    )

    // onDestroy should unregister receiver
    plugin.onDestroy()
    verify(context).unregisterReceiver(plugin.receiver)
}
```

**Pass Criteria:**
- ✅ onCreate registers broadcast receiver
- ✅ onCreate returns true
- ✅ onDestroy unregisters receiver

### Layer 4: End-to-End Testing

#### Test 4.1: Android → COSMIC Communication
**Prerequisites:**
- Paired Android device and COSMIC Desktop
- Both devices on same network
- Battery plugin enabled on both sides

**Test Procedure:**

**Step 1: Initial Battery Sync**
1. Ensure Android device is connected to COSMIC Desktop
2. Check COSMIC applet displays Android battery status
3. Verify battery percentage matches
4. Verify charging status icon correct

**Step 2: Battery State Changes**
1. Unplug Android device (if charging)
2. Verify COSMIC applet updates within 5 seconds
3. Plug in Android device
4. Verify charging icon appears in COSMIC applet

**Step 3: Low Battery Warning**
1. Let Android battery drain to < 15% (or simulate)
2. Verify COSMIC Desktop shows low battery notification
3. Charge device above 15%
4. Verify notification clears

**Step 4: Battery Request**
1. Restart Android app (disconnect/reconnect)
2. COSMIC should request battery status
3. Verify Android responds with current battery state
4. Verify COSMIC displays correct battery info

**Pass Criteria:**
- ✅ Initial sync works within 5 seconds
- ✅ State changes reflected in real-time
- ✅ Low battery notifications appear
- ✅ Battery requests work correctly

#### Test 4.2: COSMIC → Android Communication
**Prerequisites:**
- Same as Test 4.1

**Test Procedure:**

**Step 1: Desktop Battery Status**
1. Check Android app shows COSMIC Desktop battery (if laptop)
2. Verify battery percentage displayed
3. Verify charging status correct

**Step 2: Battery Request from Android**
1. Restart COSMIC applet
2. Android should receive battery update
3. Verify desktop battery status displayed correctly

**Pass Criteria:**
- ✅ Desktop battery status shown on Android (if applicable)
- ✅ Status updates work bi-directionally

#### Test 4.3: Network Reliability
**Test Procedure:**

**Step 1: Poor Network Conditions**
1. Move devices to edge of WiFi range
2. Verify battery updates still work
3. Check for packet loss handling

**Step 2: Network Reconnection**
1. Disable WiFi on Android
2. Re-enable WiFi
3. Verify battery sync resumes
4. Check battery request sent on reconnection

**Step 3: Rapid State Changes**
1. Rapidly plug/unplug Android device
2. Verify all state changes transmitted
3. Check for race conditions

**Pass Criteria:**
- ✅ Works with poor network
- ✅ Recovers from disconnection
- ✅ Handles rapid changes correctly

### Layer 5: Performance Testing

#### Test 5.1: Packet Creation Speed
**Goal:** Verify FFI overhead is minimal

**Test Cases:**
```kotlin
@Test
fun testPerformance_packetCreation() {
    val iterations = 10000

    val startTime = System.nanoTime()
    repeat(iterations) {
        BatteryPacketsFFI.createBatteryPacket(
            isCharging = true,
            currentCharge = 85,
            thresholdEvent = 0
        )
    }
    val endTime = System.nanoTime()

    val avgTimeMs = (endTime - startTime) / iterations / 1_000_000.0

    // Should be < 1ms per packet
    assertTrue(avgTimeMs < 1.0, "Average time: ${avgTimeMs}ms")
}
```

**Pass Criteria:**
- ✅ Average packet creation < 1ms
- ✅ No memory leaks
- ✅ Comparable to manual packet construction

#### Test 5.2: Extension Property Performance
**Test Cases:**
```kotlin
@Test
fun testPerformance_extensionProperties() {
    val packet = BatteryPacketsFFI.createBatteryPacket(true, 85, 0)
    val iterations = 100000

    val startTime = System.nanoTime()
    repeat(iterations) {
        val isCharging = packet.batteryIsCharging
        val charge = packet.batteryCurrentCharge
        val threshold = packet.batteryThresholdEvent
    }
    val endTime = System.nanoTime()

    val avgTimeNs = (endTime - startTime) / iterations

    // Should be < 100ns per property access
    assertTrue(avgTimeNs < 100, "Average time: ${avgTimeNs}ns")
}
```

**Pass Criteria:**
- ✅ Property access very fast (< 100ns)
- ✅ No unnecessary object allocation

#### Test 5.3: Memory Usage
**Test Cases:**
```kotlin
@Test
fun testPerformance_memoryUsage() {
    val runtime = Runtime.getRuntime()
    runtime.gc()

    val beforeMemory = runtime.totalMemory() - runtime.freeMemory()

    // Create many packets
    val packets = List(1000) {
        BatteryPacketsFFI.createBatteryPacket(
            isCharging = it % 2 == 0,
            currentCharge = it % 100,
            thresholdEvent = if (it % 10 == 0) 1 else 0
        )
    }

    val afterMemory = runtime.totalMemory() - runtime.freeMemory()
    val memoryPerPacket = (afterMemory - beforeMemory) / 1000

    // Should be < 1KB per packet
    assertTrue(memoryPerPacket < 1024, "Memory per packet: ${memoryPerPacket} bytes")
}
```

**Pass Criteria:**
- ✅ Reasonable memory usage per packet
- ✅ No memory leaks over time

### Layer 6: Regression Testing

#### Test 6.1: Backward Compatibility
**Test Cases:**

**Test 6.1.1: Legacy Packet Format**
```kotlin
@Test
fun testBackwardCompatibility_legacyPackets() {
    // Create packet the old way
    val legacyPacket = NetworkPacket("kdeconnect.battery")
    legacyPacket.set("isCharging", true)
    legacyPacket.set("currentCharge", 85)
    legacyPacket.set("thresholdEvent", 0)

    // Should still work with extension properties
    val immutablePacket = NetworkPacket.fromLegacyPacket(legacyPacket)
    assertTrue(immutablePacket.isBatteryPacket)
    assertEquals(85, immutablePacket.batteryCurrentCharge)
}
```

**Pass Criteria:**
- ✅ Old packet format still works
- ✅ No breaking changes to API

#### Test 6.2: Permission Handling
**Test Cases:**
```kotlin
@Test
fun testRegression_permissions() {
    // Verify no new permissions required
    val plugin = BatteryPlugin()

    // Battery status is a normal permission (no runtime request needed)
    val created = plugin.onCreate()
    assertTrue(created)
}
```

**Pass Criteria:**
- ✅ No new permissions required
- ✅ Works on Android 6.0+

#### Test 6.3: Platform Compatibility
**Test Procedure:**

**Test on Multiple Android Versions:**
1. Android 6.0 (API 23)
2. Android 8.0 (API 26)
3. Android 10 (API 29)
4. Android 12 (API 31)
5. Android 14 (API 34)

**Verify:**
- Plugin loads correctly
- Battery broadcasts received
- Packets sent successfully
- No crashes or errors

**Pass Criteria:**
- ✅ Works on Android 6.0+
- ✅ No version-specific issues

#### Test 6.4: Existing Plugin Interactions
**Test Cases:**
```kotlin
@Test
fun testRegression_pluginInteractions() {
    // Verify battery plugin doesn't interfere with other plugins
    val batteryPlugin = BatteryPlugin()
    val clipboardPlugin = ClipboardPlugin()
    val telephonyPlugin = TelephonyPlugin()

    // All should initialize successfully
    assertTrue(batteryPlugin.onCreate())
    assertTrue(clipboardPlugin.onCreate())
    assertTrue(telephonyPlugin.onCreate())

    // Battery packets should not be handled by other plugins
    val batteryPacket = BatteryPacketsFFI.createBatteryPacket(true, 85, 0)
    val legacyPacket = convertToLegacyPacket(batteryPacket)

    assertTrue(batteryPlugin.onPacketReceived(legacyPacket))
    assertFalse(clipboardPlugin.onPacketReceived(legacyPacket))
    assertFalse(telephonyPlugin.onPacketReceived(legacyPacket))
}
```

**Pass Criteria:**
- ✅ No plugin conflicts
- ✅ Packet routing works correctly

## Test Execution Plan

### Phase 1: Unit Tests (Estimated: 2-3 hours)
1. Run Rust FFI tests
2. Create Kotlin unit tests for BatteryPacketsFFI
3. Create Kotlin unit tests for BatteryPlugin
4. Verify all tests pass

### Phase 2: Integration Tests (Estimated: 1-2 hours)
1. Test plugin lifecycle
2. Test packet creation and receiving
3. Test state change detection
4. Test battery request handling

### Phase 3: End-to-End Tests (Estimated: 2-3 hours)
1. Set up Android device + COSMIC Desktop
2. Test bi-directional communication
3. Test state changes
4. Test low battery notifications
5. Test network reliability

### Phase 4: Performance Tests (Estimated: 1 hour)
1. Benchmark packet creation
2. Benchmark property access
3. Check memory usage
4. Compare with manual construction

### Phase 5: Regression Tests (Estimated: 1-2 hours)
1. Test backward compatibility
2. Test on multiple Android versions
3. Verify no new permissions
4. Test plugin interactions

**Total Estimated Time:** 7-11 hours

## Test Results Template

```markdown
## Battery Plugin FFI Migration - Test Results

**Date:** YYYY-MM-DD
**Tester:** [Name]
**Build:** [Git SHA]

### Layer 1: FFI Layer (Rust)
- [ ] Test 1.1: Battery packet creation - PASS/FAIL
- [ ] Test 1.2: Battery request creation - PASS/FAIL
- [ ] Test 1.3: UniFFI bindings - PASS/FAIL

### Layer 2: Kotlin Wrapper
- [ ] Test 2.1: createBatteryPacket() - PASS/FAIL
- [ ] Test 2.2: createBatteryRequest() - PASS/FAIL
- [ ] Test 2.3: Extension properties - PASS/FAIL
- [ ] Test 2.4: Java compatibility - PASS/FAIL

### Layer 3: Plugin Integration
- [ ] Test 3.1: State tracking - PASS/FAIL
- [ ] Test 3.2: Packet creation - PASS/FAIL
- [ ] Test 3.3: Request handling - PASS/FAIL
- [ ] Test 3.4: Remote battery info - PASS/FAIL
- [ ] Test 3.5: Threshold events - PASS/FAIL
- [ ] Test 3.6: Lifecycle - PASS/FAIL

### Layer 4: End-to-End
- [ ] Test 4.1: Android → COSMIC - PASS/FAIL
- [ ] Test 4.2: COSMIC → Android - PASS/FAIL
- [ ] Test 4.3: Network reliability - PASS/FAIL

### Layer 5: Performance
- [ ] Test 5.1: Packet creation speed - PASS/FAIL (avg: ___ms)
- [ ] Test 5.2: Property access speed - PASS/FAIL (avg: ___ns)
- [ ] Test 5.3: Memory usage - PASS/FAIL (per packet: ___bytes)

### Layer 6: Regression
- [ ] Test 6.1: Backward compatibility - PASS/FAIL
- [ ] Test 6.2: Permission handling - PASS/FAIL
- [ ] Test 6.3: Platform compatibility - PASS/FAIL
- [ ] Test 6.4: Plugin interactions - PASS/FAIL

### Issues Found
[List any bugs, issues, or concerns discovered during testing]

### Overall Status
- [ ] All critical tests passing
- [ ] Ready for production
```

## Success Criteria

The Battery Plugin FFI migration is considered successful when:

1. ✅ All Rust tests pass (11 tests in battery.rs)
2. ✅ All Kotlin unit tests pass
3. ✅ Battery status syncs between Android and COSMIC
4. ✅ Battery requests work correctly
5. ✅ Low battery notifications appear
6. ✅ Performance is comparable to manual implementation
7. ✅ No regressions in existing functionality
8. ✅ Works on Android 6.0+ (API 23+)
9. ✅ No new permissions required
10. ✅ No memory leaks or crashes

## Known Issues

[To be filled in during testing]

## Next Steps

After testing is complete:
1. Document any issues found
2. Create bug tickets for any failures
3. Update completion summary with test results
4. Consider adding BatteryPluginFFI.kt deprecation notice
5. Plan next plugin migration (Issue #57?)

---

**Testing Guide Version:** 1.0
**Last Updated:** 2026-01-16
**Status:** Ready for Testing
