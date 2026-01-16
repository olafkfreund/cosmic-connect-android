# Issue #54: Clipboard Plugin Migration - Testing Guide

**Status**: Phase 5 Complete (100%)
**Date**: January 17, 2026
**Issue**: [#54 - Clipboard Plugin FFI Migration](https://github.com/olafkfreund/cosmic-connect-android/issues/54)

## Overview

This guide provides comprehensive testing procedures for the Clipboard Plugin migration to FFI-based architecture. The migration affects clipboard synchronization between Android and COSMIC Desktop devices, including timestamp-based sync loop prevention.

## Test Environment Setup

### Prerequisites

1. **Development Environment**
   ```bash
   cd /home/olafkfreund/Source/GitHub/cosmic-connect-android
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Test Devices**
   - Android device (API 24+) with clipboard access permissions
   - COSMIC Desktop device with cosmic-connect-applet installed
   - Both devices on the same network
   - Devices paired and trusted

3. **Required Permissions**
   - Android: `FOREGROUND_SERVICE`, notification access
   - COSMIC: Clipboard access (automatically granted)

4. **Logging Setup**
   ```bash
   # Android logs
   adb logcat | grep -E "ClipboardPlugin|ClipboardPacketsFFI|NetworkPacket"

   # Rust core logs (if testing FFI directly)
   RUST_LOG=debug cargo test clipboard
   ```

## Test Suites

### 1. FFI Layer Tests (Rust)

#### Test 1.1: Clipboard Update Packet Creation
**File**: `cosmic-connect-core/src/ffi/mod.rs`

**Objective**: Verify create_clipboard_packet() generates correct packet structure

**Test Code**:
```rust
#[test]
fn test_create_clipboard_packet() {
    let content = "Hello World";
    let packet = create_clipboard_packet(content.to_string()).unwrap();

    assert_eq!(packet.packet_type, "kdeconnect.clipboard");
    assert!(packet.body.contains("\"content\":\"Hello World\""));
    assert!(packet.payload_size.is_none());
}
```

**Expected Result**: Packet with type "kdeconnect.clipboard" and content field

**Pass Criteria**:
- ✅ Packet type is exactly "kdeconnect.clipboard"
- ✅ Body contains content field with correct value
- ✅ No payload_size field present
- ✅ Function returns Ok(FfiPacket)

---

#### Test 1.2: Clipboard Connect Packet Creation
**File**: `cosmic-connect-core/src/ffi/mod.rs`

**Objective**: Verify create_clipboard_connect_packet() includes timestamp

**Test Code**:
```rust
#[test]
fn test_create_clipboard_connect_packet() {
    let content = "Test Content";
    let timestamp = 1705507200000i64; // 2024-01-17 12:00:00 UTC
    let packet = create_clipboard_connect_packet(content.to_string(), timestamp).unwrap();

    assert_eq!(packet.packet_type, "kdeconnect.clipboard.connect");
    assert!(packet.body.contains("\"content\":\"Test Content\""));
    assert!(packet.body.contains("\"timestamp\":1705507200000"));
}
```

**Expected Result**: Packet with type "kdeconnect.clipboard.connect", content, and timestamp

**Pass Criteria**:
- ✅ Packet type is exactly "kdeconnect.clipboard.connect"
- ✅ Body contains content field with correct value
- ✅ Body contains timestamp field with correct value
- ✅ Function returns Ok(FfiPacket)

---

#### Test 1.3: Empty Content Validation
**File**: `cosmic-connect-core/src/ffi/mod.rs`

**Objective**: Verify FFI functions handle empty strings (Kotlin layer validates, Rust accepts)

**Test Code**:
```rust
#[test]
fn test_create_clipboard_packet_empty() {
    let result = create_clipboard_packet("".to_string());
    assert!(result.is_ok()); // Rust accepts, Kotlin validates
}

#[test]
fn test_create_clipboard_connect_packet_empty() {
    let result = create_clipboard_connect_packet("".to_string(), 0);
    assert!(result.is_ok()); // Rust accepts, Kotlin validates
}
```

**Expected Result**: Functions accept empty content (validation at Kotlin layer)

**Pass Criteria**:
- ✅ Both functions return Ok for empty strings
- ✅ Kotlin layer validation tested separately

---

#### Test 1.4: Negative Timestamp Handling
**File**: `cosmic-connect-core/src/ffi/mod.rs`

**Objective**: Verify create_clipboard_connect_packet() accepts negative timestamps (Kotlin validates)

**Test Code**:
```rust
#[test]
fn test_create_clipboard_connect_packet_negative_timestamp() {
    let result = create_clipboard_connect_packet("content".to_string(), -1);
    assert!(result.is_ok()); // Rust accepts, Kotlin validates
}
```

**Expected Result**: Function accepts negative timestamp (validation at Kotlin layer)

**Pass Criteria**:
- ✅ Function returns Ok for negative timestamp
- ✅ Kotlin layer validation tested separately

---

### 2. Kotlin Wrapper Tests (Android)

#### Test 2.1: Standard Update Creation
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify createClipboardUpdate() creates correct immutable NetworkPacket

**Test Code**:
```kotlin
@Test
fun testCreateClipboardUpdate() {
    val content = "Test clipboard content"
    val packet = ClipboardPacketsFFI.createClipboardUpdate(content)

    assertEquals("kdeconnect.clipboard", packet.type)
    assertEquals(content, packet.body["content"])
    assertNull(packet.body["timestamp"])
    assertTrue(packet.isClipboardUpdate)
}
```

**Expected Result**: Immutable NetworkPacket with content field, no timestamp

**Pass Criteria**:
- ✅ Packet type is "kdeconnect.clipboard"
- ✅ Content field matches input
- ✅ No timestamp field present
- ✅ isClipboardUpdate extension property returns true

---

#### Test 2.2: Connect Packet Creation
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify createClipboardConnect() includes timestamp

**Test Code**:
```kotlin
@Test
fun testCreateClipboardConnect() {
    val content = "Connect clipboard"
    val timestamp = System.currentTimeMillis()
    val packet = ClipboardPacketsFFI.createClipboardConnect(content, timestamp)

    assertEquals("kdeconnect.clipboard.connect", packet.type)
    assertEquals(content, packet.body["content"])
    assertEquals(timestamp, (packet.body["timestamp"] as Number).toLong())
    assertTrue(packet.isClipboardConnect)
}
```

**Expected Result**: Immutable NetworkPacket with content and timestamp

**Pass Criteria**:
- ✅ Packet type is "kdeconnect.clipboard.connect"
- ✅ Content field matches input
- ✅ Timestamp field matches input
- ✅ isClipboardConnect extension property returns true

---

#### Test 2.3: Input Validation - Blank Content
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify blank content validation in createClipboardUpdate()

**Test Code**:
```kotlin
@Test(expected = IllegalArgumentException::class)
fun testCreateClipboardUpdateBlankContent() {
    ClipboardPacketsFFI.createClipboardUpdate("")
}

@Test(expected = IllegalArgumentException::class)
fun testCreateClipboardUpdateWhitespaceContent() {
    ClipboardPacketsFFI.createClipboardUpdate("   ")
}
```

**Expected Result**: IllegalArgumentException thrown

**Pass Criteria**:
- ✅ Empty string throws IllegalArgumentException
- ✅ Whitespace-only string throws IllegalArgumentException
- ✅ Error message: "Clipboard content cannot be blank"

---

#### Test 2.4: Input Validation - Negative Timestamp
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify timestamp validation in createClipboardConnect()

**Test Code**:
```kotlin
@Test(expected = IllegalArgumentException::class)
fun testCreateClipboardConnectNegativeTimestamp() {
    ClipboardPacketsFFI.createClipboardConnect("content", -1)
}

@Test
fun testCreateClipboardConnectZeroTimestamp() {
    val packet = ClipboardPacketsFFI.createClipboardConnect("content", 0)
    assertEquals(0L, (packet.body["timestamp"] as Number).toLong())
}
```

**Expected Result**: Negative timestamp rejected, zero timestamp accepted

**Pass Criteria**:
- ✅ Negative timestamp throws IllegalArgumentException
- ✅ Error message: "Timestamp cannot be negative"
- ✅ Zero timestamp accepted (indicates no content)

---

#### Test 2.5: Extension Properties - Type Detection
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify isClipboardUpdate and isClipboardConnect extension properties

**Test Code**:
```kotlin
@Test
fun testExtensionPropertiesTypeDetection() {
    val updatePacket = ClipboardPacketsFFI.createClipboardUpdate("content")
    assertTrue(updatePacket.isClipboardUpdate)
    assertFalse(updatePacket.isClipboardConnect)

    val connectPacket = ClipboardPacketsFFI.createClipboardConnect("content", 100)
    assertTrue(connectPacket.isClipboardConnect)
    assertFalse(connectPacket.isClipboardUpdate)

    // Test with wrong packet type
    val pingPacket = NetworkPacket.fromFfiPacket(
        create_ping_packet("test")
    )
    assertFalse(pingPacket.isClipboardUpdate)
    assertFalse(pingPacket.isClipboardConnect)
}
```

**Expected Result**: Correct type detection for all packet types

**Pass Criteria**:
- ✅ Update packet: isClipboardUpdate=true, isClipboardConnect=false
- ✅ Connect packet: isClipboardConnect=true, isClipboardUpdate=false
- ✅ Other packet types: both properties false

---

#### Test 2.6: Extension Properties - Content Extraction
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify clipboardContent extension property

**Test Code**:
```kotlin
@Test
fun testExtensionPropertiesContentExtraction() {
    val content = "Test clipboard data"

    val updatePacket = ClipboardPacketsFFI.createClipboardUpdate(content)
    assertEquals(content, updatePacket.clipboardContent)

    val connectPacket = ClipboardPacketsFFI.createClipboardConnect(content, 200)
    assertEquals(content, connectPacket.clipboardContent)

    // Test with wrong packet type
    val pingPacket = NetworkPacket.fromFfiPacket(
        create_ping_packet("test")
    )
    assertNull(pingPacket.clipboardContent)
}
```

**Expected Result**: Content correctly extracted from clipboard packets

**Pass Criteria**:
- ✅ Update packet content extracted correctly
- ✅ Connect packet content extracted correctly
- ✅ Non-clipboard packets return null

---

#### Test 2.7: Extension Properties - Timestamp Extraction
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify clipboardTimestamp extension property

**Test Code**:
```kotlin
@Test
fun testExtensionPropertiesTimestampExtraction() {
    val timestamp = System.currentTimeMillis()

    val updatePacket = ClipboardPacketsFFI.createClipboardUpdate("content")
    assertNull(updatePacket.clipboardTimestamp)

    val connectPacket = ClipboardPacketsFFI.createClipboardConnect("content", timestamp)
    assertEquals(timestamp, connectPacket.clipboardTimestamp)

    // Test with wrong packet type
    val pingPacket = NetworkPacket.fromFfiPacket(
        create_ping_packet("test")
    )
    assertNull(pingPacket.clipboardTimestamp)
}
```

**Expected Result**: Timestamp correctly extracted from connect packets only

**Pass Criteria**:
- ✅ Update packet timestamp is null
- ✅ Connect packet timestamp extracted correctly
- ✅ Non-clipboard packets return null

---

#### Test 2.8: Java Interop - Helper Functions
**File**: `ClipboardPacketsFFI.kt`

**Objective**: Verify Java-compatible helper functions work correctly

**Test Code**:
```kotlin
@Test
fun testJavaCompatibleHelpers() {
    val updatePacket = ClipboardPacketsFFI.createClipboardUpdate("content")
    val connectPacket = ClipboardPacketsFFI.createClipboardConnect("content", 300)

    // Test helper functions (callable from Java)
    assertTrue(getIsClipboardUpdate(updatePacket))
    assertFalse(getIsClipboardConnect(updatePacket))
    assertEquals("content", getClipboardContent(updatePacket))
    assertNull(getClipboardTimestamp(updatePacket))

    assertFalse(getIsClipboardUpdate(connectPacket))
    assertTrue(getIsClipboardConnect(connectPacket))
    assertEquals("content", getClipboardContent(connectPacket))
    assertEquals(300L, getClipboardTimestamp(connectPacket))
}
```

**Expected Result**: Helper functions match extension properties

**Pass Criteria**:
- ✅ All helper functions return same values as extension properties
- ✅ Functions callable from Java (static imports work)

---

### 3. Plugin Integration Tests (Android)

#### Test 3.1: Propagate Clipboard (Standard Update)
**File**: `ClipboardPlugin.java`

**Objective**: Verify propagateClipboard() uses FFI wrapper correctly

**Test Code**:
```kotlin
@Test
fun testPropagateClipboard() {
    val plugin = ClipboardPlugin()
    val device = mockDevice()
    plugin.onCreate()

    // Trigger clipboard change
    val content = "Propagated content"
    plugin.propagateClipboard(content)

    // Verify packet sent to device
    val sentPacket = device.lastSentPacket
    assertEquals("kdeconnect.clipboard", sentPacket.type)
    assertEquals(content, sentPacket.getString("content"))
    assertFalse(sentPacket.has("timestamp"))
}
```

**Expected Result**: FFI-created packet sent to device

**Pass Criteria**:
- ✅ Packet type is "kdeconnect.clipboard"
- ✅ Content field matches clipboard content
- ✅ No timestamp field present
- ✅ Packet sent via device.sendPacket()

---

#### Test 3.2: Send Connect Packet (With Timestamp)
**File**: `ClipboardPlugin.java`

**Objective**: Verify sendConnectPacket() includes timestamp

**Test Code**:
```kotlin
@Test
fun testSendConnectPacket() {
    val plugin = ClipboardPlugin()
    val device = mockDevice()

    // Set clipboard content with timestamp
    ClipboardListener.instance(context).setText("Connect content")
    val expectedTimestamp = ClipboardListener.instance(context).getUpdateTimestamp()

    // Trigger onCreate (calls sendConnectPacket)
    plugin.onCreate()

    // Verify packet sent to device
    val sentPacket = device.lastSentPacket
    assertEquals("kdeconnect.clipboard.connect", sentPacket.type)
    assertEquals("Connect content", sentPacket.getString("content"))
    assertEquals(expectedTimestamp, sentPacket.getLong("timestamp"))
}
```

**Expected Result**: FFI-created connect packet with timestamp sent

**Pass Criteria**:
- ✅ Packet type is "kdeconnect.clipboard.connect"
- ✅ Content field matches clipboard content
- ✅ Timestamp field matches ClipboardListener timestamp
- ✅ Packet sent via device.sendPacket()

---

#### Test 3.3: Receive Standard Update
**File**: `ClipboardPlugin.java`

**Objective**: Verify onPacketReceived() handles standard updates

**Test Code**:
```kotlin
@Test
fun testReceiveStandardUpdate() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Create incoming packet
    val content = "Received content"
    val packet = ClipboardPacketsFFI.createClipboardUpdate(content)
    val legacyPacket = convertToLegacyPacket(packet)

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify clipboard updated
    assertTrue(result)
    assertEquals(content, ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Clipboard updated with received content

**Pass Criteria**:
- ✅ onPacketReceived() returns true
- ✅ ClipboardListener content matches received content
- ✅ No errors or exceptions

---

#### Test 3.4: Receive Connect Packet (Newer Timestamp)
**File**: `ClipboardPlugin.java`

**Objective**: Verify timestamp comparison logic accepts newer timestamps

**Test Code**:
```kotlin
@Test
fun testReceiveConnectPacketNewerTimestamp() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Set local clipboard with older timestamp
    ClipboardListener.instance(context).setText("Local content")
    val localTimestamp = ClipboardListener.instance(context).getUpdateTimestamp()

    // Create incoming packet with newer timestamp
    val remoteContent = "Remote content"
    val remoteTimestamp = localTimestamp + 1000
    val packet = ClipboardPacketsFFI.createClipboardConnect(remoteContent, remoteTimestamp)
    val legacyPacket = convertToLegacyPacket(packet)

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify clipboard updated with remote content
    assertTrue(result)
    assertEquals(remoteContent, ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Local clipboard updated with remote content

**Pass Criteria**:
- ✅ onPacketReceived() returns true
- ✅ ClipboardListener content matches remote content
- ✅ Timestamp comparison logic works correctly

---

#### Test 3.5: Receive Connect Packet (Older Timestamp)
**File**: `ClipboardPlugin.java`

**Objective**: Verify timestamp comparison logic rejects older timestamps

**Test Code**:
```kotlin
@Test
fun testReceiveConnectPacketOlderTimestamp() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Set local clipboard with newer timestamp
    ClipboardListener.instance(context).setText("Local content")
    val localTimestamp = ClipboardListener.instance(context).getUpdateTimestamp()

    // Create incoming packet with older timestamp
    val remoteContent = "Remote content"
    val remoteTimestamp = localTimestamp - 1000
    val packet = ClipboardPacketsFFI.createClipboardConnect(remoteContent, remoteTimestamp)
    val legacyPacket = convertToLegacyPacket(packet)

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify clipboard NOT updated
    assertFalse(result)
    assertEquals("Local content", ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Local clipboard unchanged

**Pass Criteria**:
- ✅ onPacketReceived() returns false
- ✅ ClipboardListener content unchanged (still "Local content")
- ✅ Sync loop prevented

---

#### Test 3.6: Receive Connect Packet (Zero Timestamp)
**File**: `ClipboardPlugin.java`

**Objective**: Verify zero timestamp handling (indicates no content)

**Test Code**:
```kotlin
@Test
fun testReceiveConnectPacketZeroTimestamp() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Set local clipboard
    ClipboardListener.instance(context).setText("Local content")

    // Create incoming packet with zero timestamp
    val packet = ClipboardPacketsFFI.createClipboardConnect("Remote content", 0)
    val legacyPacket = convertToLegacyPacket(packet)

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify clipboard NOT updated (zero timestamp means no content)
    assertFalse(result)
    assertEquals("Local content", ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Local clipboard unchanged

**Pass Criteria**:
- ✅ onPacketReceived() returns false
- ✅ ClipboardListener content unchanged
- ✅ Zero timestamp handled correctly

---

#### Test 3.7: Receive Connect Packet (Null Timestamp)
**File**: `ClipboardPlugin.java`

**Objective**: Verify null timestamp handling

**Test Code**:
```kotlin
@Test
fun testReceiveConnectPacketNullTimestamp() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Set local clipboard
    ClipboardListener.instance(context).setText("Local content")

    // Create malformed connect packet (missing timestamp)
    val legacyPacket = org.cosmic.cosmicconnect.NetworkPacket("kdeconnect.clipboard.connect")
    legacyPacket.set("content", "Remote content")
    // No timestamp field

    val immutablePacket = NetworkPacket.fromLegacy(legacyPacket)

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify null timestamp handled gracefully
    assertFalse(result)
    assertEquals("Local content", ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Null timestamp handled gracefully

**Pass Criteria**:
- ✅ onPacketReceived() returns false (no NPE)
- ✅ ClipboardListener content unchanged
- ✅ Explicit null safety working

---

### 4. End-to-End Integration Tests

#### Test 4.1: Android → COSMIC Clipboard Sync
**Devices**: Android + COSMIC Desktop

**Objective**: Verify clipboard content syncs from Android to COSMIC

**Steps**:
1. Pair Android and COSMIC devices
2. On Android: Copy text "Test from Android" to clipboard
3. On COSMIC: Check clipboard contains "Test from Android"

**Expected Result**: COSMIC clipboard updated within 1 second

**Pass Criteria**:
- ✅ COSMIC clipboard contains exact text
- ✅ Sync occurs within 1 second
- ✅ No errors in Android logs
- ✅ No errors in COSMIC logs

---

#### Test 4.2: COSMIC → Android Clipboard Sync
**Devices**: Android + COSMIC Desktop

**Objective**: Verify clipboard content syncs from COSMIC to Android

**Steps**:
1. Pair Android and COSMIC devices
2. On COSMIC: Copy text "Test from COSMIC" to clipboard
3. On Android: Check clipboard contains "Test from COSMIC"

**Expected Result**: Android clipboard updated within 1 second

**Pass Criteria**:
- ✅ Android clipboard contains exact text
- ✅ Sync occurs within 1 second
- ✅ No errors in COSMIC logs
- ✅ No errors in Android logs

---

#### Test 4.3: Rapid Clipboard Changes (Sync Loop Prevention)
**Devices**: Android + COSMIC Desktop

**Objective**: Verify timestamp-based sync loop prevention

**Steps**:
1. Pair Android and COSMIC devices
2. Rapidly alternate clipboard changes:
   - Android: Copy "Change 1"
   - Wait 100ms
   - COSMIC: Copy "Change 2"
   - Wait 100ms
   - Android: Copy "Change 3"
   - Wait 100ms
   - COSMIC: Copy "Change 4"
3. Verify final state after 5 seconds

**Expected Result**: Both devices have "Change 4" (latest change)

**Pass Criteria**:
- ✅ No infinite sync loop detected
- ✅ Both devices converge to latest change
- ✅ Android logs show timestamp comparisons working
- ✅ COSMIC logs show timestamp comparisons working

---

#### Test 4.4: Large Text Content Sync
**Devices**: Android + COSMIC Desktop

**Objective**: Verify clipboard handles large text content

**Steps**:
1. Pair Android and COSMIC devices
2. On Android: Copy 10KB text block to clipboard
3. On COSMIC: Verify full text received

**Expected Result**: Full text content synced correctly

**Pass Criteria**:
- ✅ Full 10KB text received on COSMIC
- ✅ No truncation or corruption
- ✅ Sync completes within 2 seconds
- ✅ No memory issues

---

#### Test 4.5: Unicode and Special Characters
**Devices**: Android + COSMIC Desktop

**Objective**: Verify clipboard handles Unicode and special characters

**Steps**:
1. Pair Android and COSMIC devices
2. Test various character sets:
   - Emoji: "🎉📱💻🚀"
   - Chinese: "你好世界"
   - Arabic: "مرحبا بالعالم"
   - Special: "<>&\"'\n\t"
3. Verify each text block syncs correctly

**Expected Result**: All character sets preserved correctly

**Pass Criteria**:
- ✅ Emoji characters preserved
- ✅ Chinese characters preserved
- ✅ Arabic characters preserved
- ✅ Special characters escaped/preserved correctly
- ✅ No encoding issues

---

#### Test 4.6: Device Connection/Reconnection
**Devices**: Android + COSMIC Desktop

**Objective**: Verify clipboard sync works after connection events

**Steps**:
1. Pair Android and COSMIC devices
2. On Android: Copy "Before disconnect"
3. Disconnect devices (disable WiFi or unpair)
4. On Android: Copy "During disconnect"
5. Reconnect devices
6. Wait for connect packet
7. Verify COSMIC clipboard state

**Expected Result**: COSMIC receives latest clipboard state on reconnect

**Pass Criteria**:
- ✅ Connect packet sent from Android with timestamp
- ✅ COSMIC receives "During disconnect" (not "Before disconnect")
- ✅ Timestamp comparison prevents overwriting newer content
- ✅ Connection re-established cleanly

---

### 5. Regression Tests (Protocol Compliance)

#### Test 5.1: Packet Type Verification
**Objective**: Verify correct packet types used throughout

**Test Code**:
```kotlin
@Test
fun testPacketTypeCompliance() {
    // Verify standard update uses correct packet type
    val updatePacket = ClipboardPacketsFFI.createClipboardUpdate("content")
    assertEquals("kdeconnect.clipboard", updatePacket.type)
    assertNotEquals("cosmicconnect.clipboard", updatePacket.type) // Old incorrect type

    // Verify connect packet uses correct packet type
    val connectPacket = ClipboardPacketsFFI.createClipboardConnect("content", 100)
    assertEquals("kdeconnect.clipboard.connect", connectPacket.type)
    assertNotEquals("cosmicconnect.clipboard.connect", connectPacket.type) // Old incorrect type
}
```

**Expected Result**: All packets use "kdeconnect.*" prefix

**Pass Criteria**:
- ✅ Standard update: "kdeconnect.clipboard"
- ✅ Connect packet: "kdeconnect.clipboard.connect"
- ✅ No "cosmicconnect.*" types used

---

#### Test 5.2: Backward Compatibility
**Objective**: Verify plugin can receive packets from older KDE Connect versions

**Test Code**:
```kotlin
@Test
fun testBackwardCompatibility() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Create legacy-style packet (manually constructed)
    val legacyPacket = org.cosmic.cosmicconnect.NetworkPacket("kdeconnect.clipboard")
    legacyPacket.set("content", "Legacy content")

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify backward compatibility
    assertTrue(result)
    assertEquals("Legacy content", ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Legacy packets processed correctly

**Pass Criteria**:
- ✅ Legacy packets accepted
- ✅ Content extracted correctly
- ✅ No errors or crashes

---

#### Test 5.3: Forward Compatibility (Unknown Fields)
**Objective**: Verify plugin ignores unknown fields in received packets

**Test Code**:
```kotlin
@Test
fun testForwardCompatibility() {
    val plugin = ClipboardPlugin()
    plugin.onCreate()

    // Create packet with unknown fields
    val legacyPacket = org.cosmic.cosmicconnect.NetworkPacket("kdeconnect.clipboard")
    legacyPacket.set("content", "Known field")
    legacyPacket.set("unknown_field", "Unknown value")
    legacyPacket.set("another_unknown", 12345)

    // Process packet
    val result = plugin.onPacketReceived(legacyPacket)

    // Verify unknown fields ignored
    assertTrue(result)
    assertEquals("Known field", ClipboardListener.instance(context).getCurrentContent())
}
```

**Expected Result**: Unknown fields ignored gracefully

**Pass Criteria**:
- ✅ Packet processed successfully
- ✅ Known fields extracted
- ✅ Unknown fields ignored
- ✅ No errors or warnings

---

## Test Execution

### Running All Tests

```bash
# Rust FFI tests
cd /home/olafkfreund/Source/GitHub/cosmic-connect-core
cargo test clipboard --lib

# Android instrumentation tests
cd /home/olafkfreund/Source/GitHub/cosmic-connect-android
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.Plugins.ClipboardPlugin.ClipboardPluginTest

# End-to-end tests (manual)
# Follow Test Suite 4 procedures with physical devices
```

### Test Coverage Metrics

**Target Coverage**:
- Rust FFI layer: 90%+
- Kotlin wrapper: 95%+
- Android plugin: 85%+
- End-to-end integration: 100% (all scenarios)

**Coverage Tools**:
```bash
# Rust coverage
cargo tarpaulin --lib --out Html --output-dir coverage/

# Android coverage
./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## Known Issues and Limitations

### Issue 1: Clipboard Access Permissions (Android 10+)
**Impact**: Android 10+ requires app to be in foreground to read clipboard
**Workaround**: Plugin only syncs when user explicitly copies in app
**Status**: Android OS limitation, cannot fix

### Issue 2: Large Binary Content
**Impact**: Clipboard plugin only handles text, not images or files
**Workaround**: Use Share plugin for binary content
**Status**: By design, matches KDE Connect specification

### Issue 3: Clipboard History
**Impact**: Only current clipboard content synced, no history
**Workaround**: Use clipboard manager app on both devices
**Status**: Feature request for future enhancement

## Success Criteria

✅ **All 22 test cases pass** (4 FFI + 8 Kotlin + 7 Integration + 3 Regression)
✅ **Zero compilation errors**
✅ **Protocol compliance verified** (kdeconnect.* packet types)
✅ **End-to-end sync working** bidirectionally
✅ **Sync loop prevention working** (timestamp comparison)
✅ **Backward compatibility maintained**
✅ **Code coverage targets met** (85%+ overall)

## Completion Checklist

- [ ] All FFI layer tests pass (4/4)
- [ ] All Kotlin wrapper tests pass (8/8)
- [ ] All plugin integration tests pass (7/7)
- [ ] All end-to-end tests pass (6/6)
- [ ] All regression tests pass (3/3)
- [ ] Code coverage meets targets
- [ ] Manual testing on physical devices complete
- [ ] No memory leaks detected
- [ ] No crashes or ANRs
- [ ] Documentation updated

---

**Issue #54 Testing Guide Complete**
**Total Test Cases**: 22 (expanded from initial plan)
**Estimated Testing Time**: 4-6 hours (automated + manual)
