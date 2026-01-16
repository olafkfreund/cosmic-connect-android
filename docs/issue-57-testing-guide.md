# Issue #57 Notifications Plugin Testing Guide

**Date:** 2026-01-16
**Plugin:** NotificationsPlugin (FFI-based)
**Issue:** #57 - Notifications Plugin FFI Migration

---

## Overview

This guide covers comprehensive testing of the NotificationsPlugin after FFI migration. The plugin now uses NotificationsPacketsFFI wrapper for type-safe packet creation through the cosmic-connect-core Rust FFI layer.

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
./gradlew test
```

**Tests Included:**

#### Test 3.4: Basic Notification FFI
Tests all 6 FFI functions:
- `createNotificationPacket()` ✓
- `createCancelNotificationPacket()` ✓
- `createNotificationRequestPacket()` ✓
- `createDismissNotificationPacket()` ✓
- `createNotificationActionPacket()` ✓
- `createNotificationReplyPacket()` ✓

**Expected Output:**
```
✅ Notification packet creation successful
✅ Cancel notification packet creation successful
✅ Notification request packet creation successful
✅ Dismiss notification packet creation successful
✅ Notification action packet creation successful
✅ Notification reply packet creation successful
✅ All notification plugin FFI tests passed (6/6)
```

#### Test 3.5: Complex Notification
Tests notification with all optional fields:
- Basic fields (id, appName, title, text)
- Optional fields (ticker, onlyOnce, requestReplyId)
- Arrays (actions)
- Payload (payloadHash)

**Expected Output:**
```
ID: complex-notif-001
App: WhatsApp
Title: Group Chat
Has reply: true
Has actions: true
Has icon: true
✅ Complex notification test passed
```

---

### 2. Manual Testing - Android Device

#### 2.1 Setup

**Enable Notifications Permission:**
1. Open Android Settings
2. Go to "Apps & notifications"
3. Select "Special app access"
4. Select "Notification access"
5. Enable "COSMIC Connect"

**Pair Devices:**
1. Open COSMIC Connect on Android
2. Open COSMIC Desktop applet
3. Pair devices via QR code or manual pairing
4. Verify connection established

#### 2.2 Test: Basic Notification Forwarding

**Steps:**
1. Send yourself a test message (SMS, WhatsApp, etc.)
2. Notification should appear on Android
3. Check COSMIC Desktop for mirrored notification

**Expected Behavior:**
- ✅ Notification appears on desktop within 1 second
- ✅ Title and text match Android notification
- ✅ App name displayed correctly
- ✅ Icon transferred (if privacy allows)

**Verification:**
```bash
# On Android, check logcat
adb logcat | grep "NotificationsPlugin"

# Expected log:
# NotificationsPlugin: Sending notification id=notif-123 app=Messages
```

**Pass Criteria:**
- Notification appears on desktop
- Title, text, app name correct
- No crashes in logcat

#### 2.3 Test: Notification Cancellation

**Steps:**
1. Generate notification on Android (send yourself a message)
2. Wait for desktop to show notification
3. Dismiss notification on Android (swipe away)
4. Check desktop - notification should disappear

**Expected Behavior:**
- ✅ Desktop notification dismissed within 1 second
- ✅ No error messages in logs

**Verification:**
```bash
# Check for cancel packet
adb logcat | grep "isCancel"

# Expected log:
# NetworkPacket: {"type":"kdeconnect.notification","body":{"id":"notif-123","isCancel":true}}
```

**Pass Criteria:**
- Desktop notification removed when Android notification dismissed
- Bi-directional sync working

#### 2.4 Test: Privacy Controls

**Setup:**
1. Open COSMIC Connect settings
2. Go to "Notifications"
3. Select an app (e.g., WhatsApp)
4. Enable "Block notification contents"

**Steps:**
1. Send message to trigger notification
2. Check desktop notification

**Expected Behavior:**
- ✅ Notification shows "New notification from WhatsApp"
- ✅ No title or text content displayed
- ✅ App name still visible
- ✅ Icon blocked if "Block images" enabled

**Pass Criteria:**
- Privacy settings respected
- Sensitive content not transmitted
- App name still visible for context

#### 2.5 Test: Inline Replies (Messaging Apps)

**Supported Apps:**
- WhatsApp
- Telegram
- Signal
- SMS/MMS

**Steps:**
1. Receive message notification with inline reply support
2. Check desktop notification has "Reply" button
3. Click Reply on desktop
4. Type message and send
5. Verify reply sent from Android

**Expected Behavior:**
- ✅ Desktop shows Reply button
- ✅ Reply dialog appears
- ✅ Message sent via Android
- ✅ Conversation continues normally

**Verification:**
```bash
# Check for reply packet
adb logcat | grep "notification.reply"

# Expected log:
# NotificationsPlugin: Replying to notification requestReplyId=uuid-123 message=Thanks!
```

**Pass Criteria:**
- Reply button visible on desktop
- Message sent successfully
- No crashes or errors

#### 2.6 Test: Notification Actions

**Supported Apps:**
- Email apps (Archive, Delete, Reply)
- Calendar (Dismiss, Snooze)
- Messaging (Mark as Read)

**Steps:**
1. Receive notification with action buttons
2. Check desktop shows action buttons
3. Click action button on desktop
4. Verify action executed on Android

**Expected Behavior:**
- ✅ Action buttons displayed on desktop
- ✅ Clicking button triggers action on Android
- ✅ Notification updates appropriately

**Verification:**
```bash
# Check for action packet
adb logcat | grep "notification.action"

# Expected log:
# NotificationsPlugin: Triggering action key=notif-456 action="Mark as Read"
```

**Pass Criteria:**
- Actions visible on desktop
- Actions execute correctly
- No errors in logcat

#### 2.7 Test: Notification Request (Sync)

**Steps:**
1. Have several notifications on Android
2. Connect device to desktop
3. Desktop should request all notifications
4. Android sends all current notifications

**Expected Behavior:**
- ✅ All current notifications appear on desktop
- ✅ Sent with "silent" flag (no sound/vibration)
- ✅ Complete within 2 seconds

**Verification:**
```bash
# Check for request packet
adb logcat | grep "notification.request"

# Expected log:
# NotificationsPlugin: Received notification request, sending 5 notifications
```

**Pass Criteria:**
- All notifications synced
- No duplicates
- "Silent" flag set correctly (preexisting notifications)

#### 2.8 Test: Screen-off Filtering

**Setup:**
1. Open COSMIC Connect settings
2. Go to "Notifications"
3. Enable "Only when screen is off"

**Steps:**
1. With screen ON: Receive notification
   - Should NOT forward to desktop
2. With screen OFF: Receive notification
   - SHOULD forward to desktop

**Expected Behavior:**
- ✅ Screen ON: No desktop notification
- ✅ Screen OFF: Desktop notification appears

**Pass Criteria:**
- Filtering works as expected
- Setting persists across app restarts

#### 2.9 Test: Icon Transfer

**Steps:**
1. Receive notification from app with icon
2. Check desktop notification shows icon
3. Verify icon matches Android app icon

**Expected Behavior:**
- ✅ Icon extracted from notification
- ✅ Icon compressed to PNG
- ✅ MD5 hash calculated
- ✅ Icon transferred via payload
- ✅ Desktop displays icon

**Verification:**
```bash
# Check for payload hash
adb logcat | grep "payloadHash"

# Expected log:
# NotificationsPlugin: Attaching icon payload hash=1a2b3c4d5e6f size=4096 bytes
```

**Pass Criteria:**
- Icon appears on desktop
- Icon recognizable and correct size
- No image quality issues

#### 2.10 Test: System Notification Filtering

**Android generates many system notifications that should be filtered:**

**Should be FILTERED (not sent):**
- ❌ Low battery notifications (handled by Battery plugin)
- ❌ Media playback controls (handled by MPRIS plugin)
- ❌ Foreground service notifications (ongoing)
- ❌ Local-only notifications
- ❌ Group summary notifications
- ❌ COSMIC Connect's own notifications

**Should be FORWARDED:**
- ✅ Regular app notifications
- ✅ Messaging notifications
- ✅ Email notifications
- ✅ Calendar reminders

**Steps:**
1. Generate various types of notifications
2. Observe which ones appear on desktop

**Pass Criteria:**
- System notifications filtered correctly
- Only user-facing notifications forwarded
- No infinite loops (own notifications)

---

### 3. Edge Cases and Error Handling

#### 3.1 Test: Large Notification Text

**Steps:**
1. Send notification with very long text (1000+ characters)
2. Check desktop notification

**Expected Behavior:**
- ✅ Long text handled gracefully
- ✅ No crashes or truncation issues
- ✅ Desktop shows appropriate preview

#### 3.2 Test: Special Characters

**Steps:**
1. Send notification with emoji, unicode, special chars
2. Check desktop notification

**Expected Behavior:**
- ✅ Emoji displayed correctly
- ✅ Unicode characters preserved
- ✅ Special characters not breaking packet

**Test String:**
```
Title: 🔔 Notification Test 🎉
Text: Hello 世界! Testing special chars: <>&"' and emoji: 👍🎈🚀
```

#### 3.3 Test: Rapid Notifications

**Steps:**
1. Generate 10+ notifications rapidly (spam test app)
2. Check all appear on desktop
3. Dismiss several quickly

**Expected Behavior:**
- ✅ All notifications forwarded
- ✅ No missed notifications
- ✅ No race conditions
- ✅ Dismissals sync correctly

#### 3.4 Test: Connection Loss

**Steps:**
1. Receive notification while connected
2. Disconnect device (turn off WiFi)
3. Receive notification
4. Reconnect
5. Check notification sync

**Expected Behavior:**
- ✅ Missed notifications sent on reconnect
- ✅ No duplicate notifications
- ✅ Graceful reconnection

#### 3.5 Test: App Uninstall

**Steps:**
1. Receive notification from app
2. Uninstall app
3. Try to interact with notification

**Expected Behavior:**
- ✅ No crashes
- ✅ Graceful error handling
- ✅ Notification cleared or marked as invalid

---

### 4. Performance Testing

#### 4.1 Latency Measurement

**Objective:** Measure time from Android notification to desktop appearance

**Method:**
1. Use timestamp in notification creation
2. Log timestamp on desktop reception
3. Calculate delta

**Target:** < 500ms end-to-end latency

**Verification:**
```bash
# Android
adb logcat -v time | grep "NotificationsPlugin: Sending"

# Desktop
journalctl -f | grep "Notification received"

# Compare timestamps
```

#### 4.2 Memory Usage

**Objective:** Ensure no memory leaks with many notifications

**Method:**
1. Generate 100+ notifications
2. Monitor memory usage
3. Dismiss all notifications
4. Check memory returns to baseline

**Tools:**
```bash
# Android memory profiling
adb shell dumpsys meminfo org.cosmic.cosmicconnect

# Before test
# After 100 notifications
# After dismissing all
```

**Target:** < 10MB increase for notification tracking

#### 4.3 Battery Impact

**Objective:** Measure battery drain from notification forwarding

**Method:**
1. Charge device to 100%
2. Run for 1 hour with normal notification load
3. Check battery usage

**Target:** < 2% battery per hour for notifications

**Tools:**
```bash
adb shell dumpsys batterystats | grep cosmic
```

---

### 5. Regression Testing

**Verify no regressions from Java → Kotlin migration:**

✅ **Feature Parity Checklist:**
- [ ] All notification types supported
- [ ] Privacy controls working
- [ ] Inline replies functional
- [ ] Action buttons working
- [ ] Icon transfer working
- [ ] Screen-off filtering working
- [ ] Notification sync working
- [ ] Bi-directional cancel working
- [ ] System notification filtering working
- [ ] Conversation extraction working
- [ ] Ticker text generation working
- [ ] RemoteInput handling working
- [ ] PendingIntent execution working
- [ ] Notification grouping respect
- [ ] Permission checks working
- [ ] Settings page accessible

---

## Test Results Template

```markdown
# NotificationsPlugin Test Results - Issue #57

**Date:** YYYY-MM-DD
**Tester:** [Name]
**Device:** [Model + Android Version]
**Desktop:** [COSMIC Version]
**Build:** [cosmic-connect-android commit hash]

## Unit Tests
- [ ] Test 3.4: Basic Notification FFI - PASS/FAIL
- [ ] Test 3.5: Complex Notification - PASS/FAIL

## Manual Tests
- [ ] 2.2: Basic Notification Forwarding - PASS/FAIL
- [ ] 2.3: Notification Cancellation - PASS/FAIL
- [ ] 2.4: Privacy Controls - PASS/FAIL
- [ ] 2.5: Inline Replies - PASS/FAIL
- [ ] 2.6: Notification Actions - PASS/FAIL
- [ ] 2.7: Notification Request - PASS/FAIL
- [ ] 2.8: Screen-off Filtering - PASS/FAIL
- [ ] 2.9: Icon Transfer - PASS/FAIL
- [ ] 2.10: System Filtering - PASS/FAIL

## Edge Cases
- [ ] 3.1: Large Text - PASS/FAIL
- [ ] 3.2: Special Characters - PASS/FAIL
- [ ] 3.3: Rapid Notifications - PASS/FAIL
- [ ] 3.4: Connection Loss - PASS/FAIL
- [ ] 3.5: App Uninstall - PASS/FAIL

## Performance
- [ ] 4.1: Latency < 500ms - PASS/FAIL
- [ ] 4.2: Memory < 10MB - PASS/FAIL
- [ ] 4.3: Battery < 2%/hr - PASS/FAIL

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

### Issue: Notifications Not Forwarding

**Check:**
1. Notification permission enabled?
   ```bash
   adb shell settings get secure enabled_notification_listeners
   # Should contain org.cosmic.cosmicconnect
   ```

2. Device paired and connected?
   ```bash
   adb logcat | grep "Device: connected"
   ```

3. App not in privacy blocklist?
   - Check COSMIC Connect settings → Notifications

4. Plugin enabled?
   ```bash
   adb logcat | grep "NotificationsPlugin: onCreate"
   ```

### Issue: Icons Not Transferring

**Check:**
1. Image privacy not blocking icons?
2. Icon extraction successful?
   ```bash
   adb logcat | grep "extractIcon"
   ```

3. Payload attachment working?
   ```bash
   adb logcat | grep "attachIcon"
   ```

### Issue: Inline Replies Not Working

**Check:**
1. App supports RemoteInput?
2. RemoteInput extracted correctly?
   ```bash
   adb logcat | grep "extractRepliableNotification"
   ```

3. PendingIntent valid?
4. Reply packet received?
   ```bash
   adb logcat | grep "notification.reply"
   ```

---

## Automated Testing Script

```bash
#!/bin/bash
# test-notifications-plugin.sh

set -e

echo "Starting NotificationsPlugin Test Suite..."

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
./gradlew test --tests "*FFIValidationTest.testNotificationsPlugin"
./gradlew test --tests "*FFIValidationTest.testComplexNotification"

# 4. Install on device
echo "Installing on device..."
adb install -r build/outputs/apk/debug/cosmic-connect-android-debug.apk

# 5. Grant notification permission
echo "Granting notification permission..."
adb shell cmd notification allow_listener org.cosmic.cosmicconnect/org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationReceiver

# 6. Launch app
echo "Launching app..."
adb shell am start -n org.cosmic.cosmicconnect/.UserInterface.MainActivity

echo "✅ Automated setup complete. Proceed with manual testing."
```

---

## Success Criteria

Issue #57 is considered complete when:

✅ **All unit tests pass:**
- FFI validation tests (6/6 functions)
- Complex notification test

✅ **All manual tests pass:**
- Basic forwarding
- Cancellation sync
- Privacy controls
- Inline replies
- Action buttons
- Request sync
- Screen-off filtering
- Icon transfer
- System filtering

✅ **Performance targets met:**
- Latency < 500ms
- Memory < 10MB overhead
- Battery < 2%/hr

✅ **No regressions:**
- Feature parity with Java version
- All edge cases handled

✅ **Documentation complete:**
- Testing guide (this document)
- Completion summary

---

**Happy Testing! 🧪**
