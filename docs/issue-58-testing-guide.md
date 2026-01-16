# Issue #58 Clipboard Plugin Testing Guide

**Date:** 2026-01-16
**Plugin:** ClipboardPlugin (FFI-based)
**Issue:** #58 - Clipboard Plugin FFI Migration

---

## Overview

This guide covers comprehensive testing of the ClipboardPlugin after FFI migration. The plugin now uses ClipboardPacketsFFI wrapper for type-safe packet creation through the cosmic-connect-core Rust FFI layer.

---

## Prerequisites

### Build cosmic-connect-core

```bash
cd /home/olafkfreund/Source/GitHub/cosmic-connect-core
cargo build --release
```

### Build cosmic-connect-android

```bash
cd /home/olafkfreund/Source/GitHub/cosmic-connect-android
./gradlew assembleDebug
```

### Install on Device

```bash
adb install -r build/outputs/apk/debug/cosmic-connect-android-debug.apk
```

---

## Test Suite

### 1. Unit Tests - FFI Validation

**Location:** `tests/org/cosmic/cosmicconnect/FFIValidationTest.kt`

**Run tests:**
```bash
./gradlew test --tests "*FFIValidationTest.testClipboardPlugin"
```

**Tests Included:**

#### Test 3.6: Clipboard Plugin FFI
Tests both FFI functions with various inputs:

1. **Standard clipboard update packet**
   - `createClipboardPacket(content)`
   - Verifies packet type: `kdeconnect.clipboard`
   - Verifies no timestamp field

2. **Clipboard connect packet with timestamp**
   - `createClipboardConnectPacket(content, timestamp)`
   - Verifies packet type: `kdeconnect.clipboard.connect`
   - Verifies timestamp field present

3. **Empty content handling**
   - Empty string should create valid packet
   - Empty content preserved

4. **Zero timestamp (unknown time)**
   - Zero timestamp indicates unknown clipboard update time
   - Should be valid and preserved

5. **Large content (5000+ characters)**
   - Test with 5000 character string
   - Verify no truncation or corruption

6. **Special characters and unicode**
   - Test emoji: 👍🎈🚀
   - Test unicode: 世界
   - Test special chars: <>&"'
   - Verify preservation through FFI

7. **Future timestamps**
   - Test with timestamp > current time
   - Verify future timestamps allowed

**Expected Output:**
```
✅ All clipboard plugin FFI tests passed (7/7)
   - Standard update packets
   - Connect packets with timestamps
   - Empty content handling
   - Zero timestamp (unknown time)
   - Large content (5000+ chars)
   - Special characters and unicode
   - Future timestamps
```

---

### 2. Manual Testing - Android Device

#### 2.1 Setup

**Enable READ_LOGS Permission (Android 10+):**
1. Connect device via ADB
2. Run: `adb shell pm grant org.cosmic.cosmicconnect android.permission.READ_LOGS`
3. Verify: Button should appear in plugin UI

**Pair Devices:**
1. Open COSMIC Connect on Android
2. Open COSMIC Desktop applet
3. Pair devices via QR code or manual pairing
4. Verify connection established

#### 2.2 Test: Basic Clipboard Sync (Android → Desktop)

**Steps:**
1. Copy text on Android (long-press → Copy)
2. Wait 1 second
3. Check COSMIC Desktop clipboard (Ctrl+V)

**Expected Behavior:**
- ✅ Text appears in desktop clipboard within 1 second
- ✅ Content matches exactly
- ✅ Special characters preserved

**Verification:**
```bash
# On Android, check logcat
adb logcat | grep "ClipboardPlugin"

# Expected log:
# ClipboardPlugin: Sending clipboard update: "Hello World"
```

**Pass Criteria:**
- Text synced to desktop
- Content matches exactly
- No crashes in logcat

#### 2.3 Test: Basic Clipboard Sync (Desktop → Android)

**Steps:**
1. Copy text on COSMIC Desktop (Ctrl+C)
2. Wait 1 second
3. Paste on Android (long-press → Paste)

**Expected Behavior:**
- ✅ Text appears in Android clipboard within 1 second
- ✅ Content matches exactly
- ✅ Can paste in any app

**Verification:**
```bash
# Check for clipboard update packet received
adb logcat | grep "kdeconnect.clipboard"

# Expected log:
# ClipboardPlugin: Received clipboard update: "Desktop text"
```

**Pass Criteria:**
- Text synced to Android
- Content matches exactly
- Paste works in all apps

#### 2.4 Test: Connect Packet Sync

**Steps:**
1. Disconnect devices (turn off WiFi on one)
2. Copy different text on each device
3. Reconnect devices

**Expected Behavior:**
- ✅ Both devices exchange connect packets with timestamps
- ✅ Device with newer clipboard wins
- ✅ Both devices end up with same (newer) clipboard

**Verification:**
```bash
# Check for connect packets
adb logcat | grep "clipboard.connect"

# Expected log:
# ClipboardPlugin: Sending connect packet: timestamp=1704067200000
# ClipboardPlugin: Received connect packet: timestamp=1704067190000
# ClipboardPlugin: Remote clipboard older, not updating
```

**Pass Criteria:**
- Connect packets sent on reconnection
- Timestamp comparison works
- Newer clipboard wins

#### 2.5 Test: Sync Loop Prevention

**Steps:**
1. Copy text on Android: "Test 1"
2. Wait for sync to desktop (1 second)
3. Copy same text on desktop: "Test 1"
4. Monitor for infinite loop

**Expected Behavior:**
- ✅ Android sends update to desktop
- ✅ Desktop receives update, clipboard already has "Test 1"
- ✅ Desktop does NOT send update back
- ✅ No infinite sync loop

**Verification:**
```bash
# Monitor clipboard updates
adb logcat | grep "ClipboardPlugin" | grep -v "not updating"

# Should see:
# 1. Android sends "Test 1"
# 2. Desktop receives "Test 1"
# 3. NO further updates (loop prevented)
```

**Pass Criteria:**
- No infinite sync loop
- Only one update per clipboard change
- Timestamps working correctly

#### 2.6 Test: Empty Clipboard

**Steps:**
1. Clear clipboard on Android (copy empty string if possible)
2. Try to sync to desktop

**Expected Behavior:**
- ✅ Empty clipboard handled gracefully
- ✅ No crashes or errors
- ✅ Empty string may or may not sync (implementation dependent)

**Pass Criteria:**
- No crashes with empty clipboard
- Graceful handling

#### 2.7 Test: Large Content

**Steps:**
1. Copy very long text on Android (5000+ characters)
2. Verify sync to desktop
3. Copy very long text on desktop
4. Verify sync to Android

**Expected Behavior:**
- ✅ Large content syncs successfully
- ✅ No truncation
- ✅ Content matches exactly
- ✅ Performance acceptable (< 2 seconds)

**Test String:**
Generate with:
```kotlin
val largeText = "A".repeat(10000)
```

**Pass Criteria:**
- Large content syncs both directions
- No truncation or corruption
- Performance acceptable

#### 2.8 Test: Special Characters

**Steps:**
1. Copy text with emoji, unicode, special chars
2. Verify sync to desktop
3. Verify paste works

**Test String:**
```
🔔 Clipboard Test 🎉
Hello 世界!
Special chars: <>&"'
Math: ∑∫∞≠≈
Arrows: ←→↑↓
```

**Expected Behavior:**
- ✅ All characters preserved
- ✅ Emoji displays correctly
- ✅ Unicode characters intact
- ✅ No encoding issues

**Pass Criteria:**
- All special characters preserved
- No encoding corruption
- Emoji visible on both platforms

#### 2.9 Test: Rapid Clipboard Changes

**Steps:**
1. Copy text rapidly on Android (10 times in 5 seconds)
2. Check all updates sync to desktop

**Expected Behavior:**
- ✅ All updates forwarded
- ✅ No updates lost
- ✅ Final clipboard matches last copy
- ✅ No race conditions

**Verification:**
```bash
# Count clipboard updates
adb logcat | grep "ClipboardPlugin: Sending" | wc -l

# Should see 10 updates
```

**Pass Criteria:**
- All updates forwarded
- No lost updates
- Correct final state

#### 2.10 Test: Manual Send Button (Android 10+)

**Prerequisites:**
- Android 10 or higher
- READ_LOGS permission denied (default)

**Steps:**
1. Copy text on Android
2. Open COSMIC Connect app
3. Go to device → ClipboardPlugin
4. Tap "Send Clipboard" button

**Expected Behavior:**
- ✅ Button visible in plugin UI
- ✅ Tapping button sends clipboard
- ✅ Toast notification: "Clipboard sent"
- ✅ Desktop receives clipboard

**Pass Criteria:**
- Button visible and functional
- Manual send works
- Toast notification appears

---

### 3. Edge Cases and Error Handling

#### 3.1 Test: Connection Loss During Sync

**Steps:**
1. Copy text on Android
2. While packet is being sent, turn off WiFi
3. Wait 5 seconds
4. Turn on WiFi
5. Copy different text

**Expected Behavior:**
- ✅ First update lost (acceptable)
- ✅ Reconnection successful
- ✅ Second update syncs correctly
- ✅ No crashes

#### 3.2 Test: Clipboard App Not Running

**Steps:**
1. Force stop COSMIC Connect app on Android
2. Copy text on desktop
3. Start COSMIC Connect app
4. Reconnect to desktop

**Expected Behavior:**
- ✅ Connect packet sent on reconnection
- ✅ Desktop clipboard synced to Android
- ✅ Normal operation resumes

#### 3.3 Test: Permission Revoked

**Steps:**
1. Revoke READ_LOGS permission (Android 10+)
2. Copy text on Android
3. Check if sync still works

**Expected Behavior:**
- ✅ Background clipboard monitoring may stop (Android 10+)
- ✅ Manual send button should appear
- ✅ Manual send should still work
- ✅ No crashes

#### 3.4 Test: Multi-line Content

**Steps:**
1. Copy multi-line text with newlines
2. Verify sync to desktop
3. Verify paste preserves newlines

**Test String:**
```
Line 1
Line 2
Line 3
```

**Expected Behavior:**
- ✅ Newlines preserved
- ✅ Multi-line format intact
- ✅ Paste works correctly

#### 3.5 Test: Null or Invalid Content

**Steps:**
1. Programmatically try to send null content (dev test)
2. Try to send invalid UTF-8

**Expected Behavior:**
- ✅ Null content handled gracefully
- ✅ Invalid UTF-8 sanitized or rejected
- ✅ No crashes

---

### 4. Performance Testing

#### 4.1 Latency Measurement

**Objective:** Measure time from clipboard change to remote clipboard update

**Method:**
1. Use timestamp in clipboard content: `Test ${System.currentTimeMillis()}`
2. Log timestamp on send
3. Log timestamp on receive
4. Calculate delta

**Target:** < 500ms end-to-end latency

**Verification:**
```bash
# Android
adb logcat -v time | grep "ClipboardPlugin: Sending"

# Desktop
journalctl -f | grep "Clipboard received"

# Compare timestamps
```

**Expected Results:**
- Typical latency: 100-300ms
- Maximum acceptable: 500ms
- 95th percentile: < 400ms

#### 4.2 Memory Usage

**Objective:** Ensure no memory leaks with repeated clipboard operations

**Method:**
1. Copy 100+ clipboard items
2. Monitor memory usage
3. Check for memory leaks

**Tools:**
```bash
# Android memory profiling
adb shell dumpsys meminfo org.cosmic.cosmicconnect | grep "Clipboard"

# Before test
# After 100 clipboard operations
# After clearing clipboard
```

**Target:** < 5MB memory overhead for clipboard plugin

**Expected Results:**
- Baseline: ~2MB
- After 100 operations: < 7MB
- After clearing: Returns to baseline

#### 4.3 Battery Impact

**Objective:** Measure battery drain from clipboard monitoring

**Method:**
1. Charge device to 100%
2. Run for 1 hour with normal clipboard usage
3. Check battery usage

**Target:** < 1% battery per hour for clipboard sync

**Tools:**
```bash
adb shell dumpsys batterystats | grep cosmic
```

**Expected Results:**
- Normal usage: 0.5-1% per hour
- Heavy usage (many clipboard changes): < 2% per hour
- Idle (no clipboard changes): < 0.1% per hour

---

### 5. Regression Testing

**Verify no regressions from Java → Kotlin migration:**

✅ **Feature Parity Checklist:**
- [ ] Clipboard sync (Android → Desktop)
- [ ] Clipboard sync (Desktop → Android)
- [ ] Connect packet with timestamp
- [ ] Sync loop prevention working
- [ ] Empty content handling
- [ ] Large content support
- [ ] Special characters preserved
- [ ] Unicode and emoji support
- [ ] Manual send button (Android 10+)
- [ ] READ_LOGS permission check
- [ ] ClipboardListener integration
- [ ] Observer pattern working
- [ ] onCreate/onDestroy lifecycle

---

## Test Results Template

```markdown
# ClipboardPlugin Test Results - Issue #58

**Date:** YYYY-MM-DD
**Tester:** [Name]
**Device:** [Model + Android Version]
**Desktop:** [COSMIC Version]
**Build:** [cosmic-connect-android commit hash]

## Unit Tests
- [ ] Test 3.6: Clipboard Plugin FFI - PASS/FAIL

## Manual Tests
- [ ] 2.2: Android → Desktop Sync - PASS/FAIL
- [ ] 2.3: Desktop → Android Sync - PASS/FAIL
- [ ] 2.4: Connect Packet Sync - PASS/FAIL
- [ ] 2.5: Sync Loop Prevention - PASS/FAIL
- [ ] 2.6: Empty Clipboard - PASS/FAIL
- [ ] 2.7: Large Content - PASS/FAIL
- [ ] 2.8: Special Characters - PASS/FAIL
- [ ] 2.9: Rapid Changes - PASS/FAIL
- [ ] 2.10: Manual Send Button - PASS/FAIL

## Edge Cases
- [ ] 3.1: Connection Loss - PASS/FAIL
- [ ] 3.2: App Not Running - PASS/FAIL
- [ ] 3.3: Permission Revoked - PASS/FAIL
- [ ] 3.4: Multi-line Content - PASS/FAIL
- [ ] 3.5: Null/Invalid Content - PASS/FAIL

## Performance
- [ ] 4.1: Latency < 500ms - PASS/FAIL
- [ ] 4.2: Memory < 5MB overhead - PASS/FAIL
- [ ] 4.3: Battery < 1%/hr - PASS/FAIL

## Issues Found
[List any issues discovered during testing]

## Notes
[Additional observations]

## Overall Status
- [ ] READY FOR PRODUCTION
- [ ] NEEDS FIXES
- [ ] BLOCKED BY: [issue]
```

---

## Troubleshooting

### Issue: Clipboard Not Syncing

**Check:**
1. Devices paired and connected?
   ```bash
   adb logcat | grep "Device: connected"
   ```

2. ClipboardListener initialized?
   ```bash
   adb logcat | grep "ClipboardListener"
   ```

3. Plugin enabled?
   ```bash
   adb logcat | grep "ClipboardPlugin: onCreate"
   ```

4. Permission granted (Android 10+)?
   ```bash
   adb shell pm list permissions | grep READ_LOGS
   ```

### Issue: Sync Loop Detected

**Check:**
1. Timestamps being set correctly?
   ```bash
   adb logcat | grep "timestamp"
   ```

2. Connect packets vs standard packets?
   - Connect packets SHOULD have timestamp
   - Standard packets should NOT have timestamp

3. Comparison logic working?
   ```bash
   adb logcat | grep "not updating"
   ```

### Issue: Special Characters Corrupted

**Check:**
1. UTF-8 encoding preserved?
2. JSON serialization working?
3. Network transmission intact?

**Test:**
```bash
# Send test string
adb shell input text "Hello世界🎉"

# Check received on desktop
# Should match exactly
```

### Issue: Manual Send Button Not Visible

**Check:**
1. Android version >= 10?
2. READ_LOGS permission denied?
   ```bash
   adb shell dumpsys package org.cosmic.cosmicconnect | grep READ_LOGS
   ```

3. If permission granted, button won't show (menu entry instead)

---

## Automated Testing Script

```bash
#!/bin/bash
# test-clipboard-plugin.sh

set -e

echo "Starting ClipboardPlugin Test Suite..."

# 1. Build core library
echo "Building cosmic-connect-core..."
cd /home/olafkfreund/Source/GitHub/cosmic-connect-core
cargo build --release

# 2. Build Android app
echo "Building cosmic-connect-android..."
cd /home/olafkfreund/Source/GitHub/cosmic-connect-android
./gradlew assembleDebug

# 3. Run unit tests
echo "Running FFI validation tests..."
./gradlew test --tests "*FFIValidationTest.testClipboardPlugin"

# 4. Install on device
echo "Installing on device..."
adb install -r build/outputs/apk/debug/cosmic-connect-android-debug.apk

# 5. Grant permissions
echo "Granting READ_LOGS permission..."
adb shell pm grant org.cosmic.cosmicconnect android.permission.READ_LOGS || echo "Permission already granted or not required"

# 6. Launch app
echo "Launching app..."
adb shell am start -n org.cosmic.cosmicconnect/.UserInterface.MainActivity

echo "✅ Automated setup complete. Proceed with manual testing."
```

---

## Success Criteria

Issue #58 is considered complete when:

✅ **All unit tests pass:**
- FFI validation tests (7/7 clipboard tests)

✅ **All manual tests pass:**
- Bidirectional clipboard sync
- Connect packet with timestamp
- Sync loop prevention
- Empty content handling
- Large content support
- Special characters preserved
- Manual send button works

✅ **Performance targets met:**
- Latency < 500ms
- Memory < 5MB overhead
- Battery < 1%/hr

✅ **No regressions:**
- Feature parity with Java version
- All edge cases handled

✅ **Documentation complete:**
- Testing guide (this document)
- Completion summary

---

**Happy Testing! 🧪**
