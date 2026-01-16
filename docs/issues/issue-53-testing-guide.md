# Issue #53: Share Plugin Testing Guide

**Date**: 2026-01-16
**Status**: Testing Documentation
**Plugin**: SharePlugin (FFI-enabled)
**Platforms**: Android ↔ COSMIC Desktop

---

## Overview

This guide provides comprehensive testing procedures for the Share plugin after FFI migration. The plugin now uses SharePacketsFFI wrappers for text/URL sharing while maintaining legacy file transfer support.

---

## Test Environment Setup

### Prerequisites

**Android Device/Emulator**:
- Android 10+ (API level 29+)
- COSMIC Connect app installed
- Network connectivity (same LAN as COSMIC Desktop)
- Storage permissions granted

**COSMIC Desktop**:
- COSMIC Desktop environment
- cosmic-connect-desktop-app running
- Device paired with Android device

**Network**:
- Both devices on same network
- Firewall allows UDP (discovery) and TCP (data transfer)
- Ports 1714-1764 open

### Verification Steps

```bash
# On COSMIC Desktop - verify daemon running
systemctl --user status cosmic-connect-daemon

# Check device pairing
cosmic-connect-cli list-devices

# Verify Android device appears and is paired
```

**Android**:
1. Open COSMIC Connect app
2. Go to Devices tab
3. Verify COSMIC Desktop device appears
4. Verify device shows "Connected" status

---

## Test Scenarios

### Test Suite 1: Text Sharing

#### Test 1.1: Send Text from Android to COSMIC

**Steps**:
1. Open any app with text (e.g., Notes, Browser)
2. Select text and tap Share
3. Choose "COSMIC Connect"
4. Select COSMIC Desktop device

**Expected Result**:
- Android shows "Text sent" toast
- COSMIC Desktop receives text
- Text appears in COSMIC Desktop clipboard
- COSMIC notification appears (if enabled)

**Verification**:
```bash
# On COSMIC Desktop - check clipboard
wl-paste
# Should show shared text
```

**Pass/Fail Criteria**:
- ✅ Pass: Text matches exactly (including newlines, special chars)
- ❌ Fail: Text truncated, corrupted, or not received

---

#### Test 1.2: Receive Text on Android from COSMIC

**Steps**:
1. On COSMIC Desktop, select text
2. Right-click → Share → Android device
3. Or use CLI: `cosmic-connect-cli share --text "Hello Android" --device <device-id>`

**Expected Result**:
- Android shows notification "Text received"
- Android clipboard contains shared text
- Android shows toast notification

**Verification**:
1. Open any text field on Android
2. Long-press → Paste
3. Verify text matches

**Pass/Fail Criteria**:
- ✅ Pass: Text appears in Android clipboard
- ❌ Fail: Text not in clipboard or corrupted

---

#### Test 1.3: Text with Special Characters

**Test Data**:
```
Hello 世界! 🌍
Line 1
Line 2
Tab:	indented
Quote: "quoted"
Path: C:\Program Files\App
```

**Steps**:
1. Copy above text to clipboard
2. Share from Android → COSMIC
3. Share from COSMIC → Android

**Expected Result**:
- All characters preserved
- Newlines maintained
- Emoji rendered correctly
- No escape sequence corruption

**Pass/Fail Criteria**:
- ✅ Pass: Character-for-character match
- ❌ Fail: Any character corruption or loss

---

### Test Suite 2: URL Sharing

#### Test 2.1: Send URL from Android to COSMIC

**Steps**:
1. Open Chrome/Firefox on Android
2. Navigate to https://example.com
3. Tap Share → COSMIC Connect
4. Select COSMIC Desktop device

**Expected Result**:
- Android shows "URL sent" toast
- COSMIC Desktop shows notification
- Clicking notification opens URL in default browser

**Verification**:
```bash
# On COSMIC Desktop - check received URL
# Should open https://example.com in browser
```

**Pass/Fail Criteria**:
- ✅ Pass: Correct URL opens in browser
- ❌ Fail: Wrong URL, no notification, or browser doesn't open

---

#### Test 2.2: Receive URL on Android from COSMIC

**Steps**:
1. On COSMIC Desktop: `cosmic-connect-cli share --url "https://example.com" --device <device-id>`
2. Or right-click URL in browser → Share → Android device

**Expected Result**:
- Android shows notification "URL received"
- Tapping notification opens URL in Chrome/Firefox
- URL is correct

**Verification**:
1. Tap Android notification
2. Verify browser opens
3. Verify URL in address bar matches

**Pass/Fail Criteria**:
- ✅ Pass: Correct URL opens in Android browser
- ❌ Fail: Wrong URL or browser doesn't open

---

#### Test 2.3: YouTube URL Detection

**Test Data**: YouTube share URL
```
My Favorite Song: http://youtu.be/dQw4w9WgXcQ
```

**Steps**:
1. Share above text from Android → COSMIC
2. Subject line should contain "YouTube"

**Expected Result**:
- URL extracted from text automatically
- Browser opens to YouTube video
- Not treated as plain text

**Pass/Fail Criteria**:
- ✅ Pass: Video opens (not text share)
- ❌ Fail: Treated as text instead of URL

---

### Test Suite 3: File Sharing (Legacy System)

#### Test 3.1: Send Single File from Android

**Steps**:
1. Open Files app on Android
2. Select a small file (e.g., image < 5MB)
3. Tap Share → COSMIC Connect
4. Select COSMIC Desktop device

**Expected Result**:
- Android shows upload progress notification
- Progress updates smoothly (0% → 100%)
- COSMIC Desktop shows download notification
- File appears in COSMIC Downloads folder
- File size matches exactly

**Verification**:
```bash
# On COSMIC Desktop
ls -lh ~/Downloads/
md5sum ~/Downloads/<filename>

# Compare with Android original
adb shell md5sum /sdcard/Download/<filename>
```

**Pass/Fail Criteria**:
- ✅ Pass: File received, size matches, MD5 matches
- ❌ Fail: File corrupted, wrong size, or not received

---

#### Test 3.2: Receive Single File on Android

**Steps**:
1. On COSMIC Desktop: Select file in file manager
2. Right-click → Share → Android device
3. Or CLI: `cosmic-connect-cli share --file /path/to/file.jpg --device <device-id>`

**Expected Result**:
- COSMIC shows upload progress
- Android shows notification "Receiving file..."
- Progress updates smoothly
- File appears in Android Downloads folder
- Android media scanner picks up image/video
- Notification shows "Tap to open"

**Verification**:
1. Open Files app on Android
2. Navigate to Downloads
3. Verify file exists and opens correctly
4. Check file properties (size, date)

**Pass/Fail Criteria**:
- ✅ Pass: File received, playable/viewable, correct size
- ❌ Fail: File not found, corrupted, or wrong location

---

#### Test 3.3: Send Multiple Files from Android

**Steps**:
1. Open Files app
2. Long-press to select 5 small files
3. Tap Share → COSMIC Connect
4. Select device

**Expected Result**:
- Android shows aggregate progress (e.g., "2/5 files")
- Update packet sent first (numberOfFiles, totalPayloadSize)
- Individual file packets follow
- All files received on COSMIC
- Progress shows combined size

**Verification**:
```bash
# On COSMIC Desktop
ls -lh ~/Downloads/ | tail -5
# Should show 5 new files
```

**Pass/Fail Criteria**:
- ✅ Pass: All files received, sizes correct
- ❌ Fail: Missing files, wrong count, or corrupted

---

#### Test 3.4: Large File Transfer

**Test File**: 100MB+ file

**Steps**:
1. Share large file Android → COSMIC
2. Monitor progress for several minutes

**Expected Result**:
- Progress updates regularly (every 0.5-1 second)
- Transfer doesn't timeout
- Network interruption shows error
- Can cancel mid-transfer
- Cancelled transfer cleans up partial file

**Monitoring**:
```bash
# On COSMIC Desktop - watch file grow
watch -n 1 'ls -lh ~/Downloads/<filename>'
```

**Pass/Fail Criteria**:
- ✅ Pass: Large file completes successfully
- ❌ Fail: Timeout, freeze, or memory issues

---

#### Test 3.5: Transfer Cancellation

**Steps**:
1. Start file transfer Android → COSMIC
2. Wait until 50% progress
3. Tap "Cancel" on Android notification

**Expected Result**:
- Transfer stops immediately
- Android shows "Transfer cancelled"
- COSMIC shows "Transfer failed" or "Cancelled"
- Partial file deleted on receiving side
- No memory leak or hung connection

**Verification**:
```bash
# On COSMIC Desktop - file should not exist
ls ~/Downloads/<filename>
# Should show "No such file"
```

**Pass/Fail Criteria**:
- ✅ Pass: Clean cancellation, no partial file
- ❌ Fail: Partial file remains or transfer continues

---

### Test Suite 4: Error Scenarios

#### Test 4.1: Network Disconnect During Transfer

**Steps**:
1. Start large file transfer
2. At 50% progress, turn off WiFi on Android
3. Wait 10 seconds
4. Turn WiFi back on

**Expected Result**:
- Transfer shows error within 5-10 seconds
- Error message: "Network error" or similar
- Android cleans up state
- Can retry transfer

**Pass/Fail Criteria**:
- ✅ Pass: Error detected, cleanup successful
- ❌ Fail: Transfer hangs indefinitely

---

#### Test 4.2: Disk Full on Receiver

**Steps**:
1. Fill COSMIC Desktop disk to near capacity
2. Attempt to receive large file

**Expected Result**:
- Transfer starts
- Fails with "Disk full" or "No space" error
- Android shows error notification
- Partial file deleted

**Pass/Fail Criteria**:
- ✅ Pass: Clear error message, cleanup
- ❌ Fail: App crash or silent failure

---

#### Test 4.3: Invalid URL Format

**Steps**:
1. Attempt to share text that looks like URL but isn't
2. Example: "http//missing-colon.com"

**Expected Result**:
- SharePacketsFFI validates URL format
- Shows error or treats as text
- No crash

**Pass/Fail Criteria**:
- ✅ Pass: Handled gracefully
- ❌ Fail: App crash or exception

---

#### Test 4.4: Empty Text/URL Share

**Steps**:
1. Attempt to share empty string
2. Or single space character

**Expected Result**:
- SharePacketsFFI validation rejects empty input
- Error message or no-op
- No crash

**Pass/Fail Criteria**:
- ✅ Pass: Validation prevents empty share
- ❌ Fail: Crash or sends invalid packet

---

### Test Suite 5: Cross-Platform Protocol Compliance

#### Test 5.1: Packet Type Verification

**Method**: Capture network packets with Wireshark

**Steps**:
1. Start Wireshark on COSMIC Desktop
2. Filter: `tcp.port >= 1714 && tcp.port <= 1764`
3. Share text from Android

**Expected Result**:
```json
{
  "id": 1234567890,
  "type": "kdeconnect.share.request",
  "body": {
    "text": "Hello World"
  }
}
```

**Verification**:
- Packet type is `kdeconnect.share.request` (NOT `cosmicconnect.*`)
- JSON is well-formed
- Body contains correct field (text/url/filename)
- Newline terminator present

**Pass/Fail Criteria**:
- ✅ Pass: Packet matches KDE Connect v7 spec
- ❌ Fail: Wrong packet type or malformed JSON

---

#### Test 5.2: Compatibility with KDE Connect

**Setup**: Install KDE Connect on Linux desktop

**Steps**:
1. Pair Android with KDE Connect
2. Share text Android → KDE Connect
3. Share file KDE Connect → Android

**Expected Result**:
- COSMIC Connect Android works with KDE Connect
- Packets interoperate correctly
- No protocol violations

**Pass/Fail Criteria**:
- ✅ Pass: Full interoperability
- ❌ Fail: Packets rejected or errors

---

### Test Suite 6: Performance & Stability

#### Test 6.1: Rapid Successive Shares

**Steps**:
1. Share text 10 times in rapid succession
2. Share 5 URLs back-to-back
3. Share 3 files simultaneously

**Expected Result**:
- All shares complete successfully
- No queue overflow
- No memory leaks
- UI remains responsive

**Pass/Fail Criteria**:
- ✅ Pass: All shares successful, app stable
- ❌ Fail: Lost shares, crash, or UI freeze

---

#### Test 6.2: Background Transfer

**Steps**:
1. Start large file transfer
2. Press Home button (app backgrounds)
3. Wait for transfer to complete
4. Return to app

**Expected Result**:
- Transfer continues in background
- Notification shows progress
- Completes successfully
- App state correct when returned

**Pass/Fail Criteria**:
- ✅ Pass: Background transfer works
- ❌ Fail: Transfer stops when backgrounded

---

#### Test 6.3: Memory Usage

**Method**: Use Android Studio Profiler

**Steps**:
1. Connect Android device to PC
2. Open Android Studio Profiler
3. Start memory recording
4. Perform 10 file transfers (100MB each)
5. Check memory graph

**Expected Result**:
- Memory usage stays relatively flat
- Garbage collection occurs regularly
- No sustained memory growth (leak)
- Heap size reasonable (< 200MB)

**Pass/Fail Criteria**:
- ✅ Pass: No memory leaks detected
- ❌ Fail: Memory grows unbounded

---

## Test Results Template

### Test Execution Record

**Date**: ___________
**Tester**: ___________
**Android Device**: ___________
**Android Version**: ___________
**COSMIC Connect Version**: ___________
**COSMIC Desktop Version**: ___________

### Results Summary

| Test ID | Test Name | Status | Notes |
|---------|-----------|--------|-------|
| 1.1 | Send Text Android → COSMIC | ☐ Pass ☐ Fail | |
| 1.2 | Receive Text COSMIC → Android | ☐ Pass ☐ Fail | |
| 1.3 | Text Special Characters | ☐ Pass ☐ Fail | |
| 2.1 | Send URL Android → COSMIC | ☐ Pass ☐ Fail | |
| 2.2 | Receive URL COSMIC → Android | ☐ Pass ☐ Fail | |
| 2.3 | YouTube URL Detection | ☐ Pass ☐ Fail | |
| 3.1 | Send Single File | ☐ Pass ☐ Fail | |
| 3.2 | Receive Single File | ☐ Pass ☐ Fail | |
| 3.3 | Send Multiple Files | ☐ Pass ☐ Fail | |
| 3.4 | Large File Transfer | ☐ Pass ☐ Fail | |
| 3.5 | Transfer Cancellation | ☐ Pass ☐ Fail | |
| 4.1 | Network Disconnect | ☐ Pass ☐ Fail | |
| 4.2 | Disk Full | ☐ Pass ☐ Fail | |
| 4.3 | Invalid URL Format | ☐ Pass ☐ Fail | |
| 4.4 | Empty Text/URL | ☐ Pass ☐ Fail | |
| 5.1 | Packet Type Verification | ☐ Pass ☐ Fail | |
| 5.2 | KDE Connect Compatibility | ☐ Pass ☐ Fail | |
| 6.1 | Rapid Successive Shares | ☐ Pass ☐ Fail | |
| 6.2 | Background Transfer | ☐ Pass ☐ Fail | |
| 6.3 | Memory Usage | ☐ Pass ☐ Fail | |

### Issues Found

| Issue # | Severity | Description | Reproduction Steps | Workaround |
|---------|----------|-------------|-------------------|------------|
| | Critical / High / Medium / Low | | | |

---

## Known Issues & Limitations

### Current Limitations

1. **File Transfer Uses Legacy System**
   - Not using PayloadTransferFFI yet
   - Future enhancement (Phase 6)
   - Current system is stable and functional

2. **No Resume Support**
   - Failed transfers must restart from beginning
   - Future: Add partial transfer resume

3. **Progress Throttling Not Applied**
   - File transfer may update UI too frequently
   - Future: Apply ProgressThrottler to notifications

### Known Android Issues

1. **Android 11+ Scoped Storage**
   - Files may not appear immediately in gallery
   - Requires MediaScanner invocation
   - Already handled in current implementation

2. **Android 13+ Notification Permissions**
   - Users must grant POST_NOTIFICATIONS permission
   - Handled by optional permissions system

---

## Regression Testing Checklist

After any changes to Share plugin, verify:

- [ ] Text sharing works both directions
- [ ] URL sharing works both directions
- [ ] Single file transfer works both directions
- [ ] Multi-file transfer works
- [ ] Progress notifications appear and update
- [ ] Cancellation works
- [ ] Special characters preserved in text
- [ ] Large files complete successfully
- [ ] No memory leaks (Profiler check)
- [ ] No crashes under normal use
- [ ] Protocol packets match specification
- [ ] Interoperates with KDE Connect

---

## Automation Opportunities

### Unit Tests (JUnit/Kotlin)

```kotlin
@Test
fun testCreateTextSharePacket() {
    val packet = SharePacketsFFI.createTextShare("Hello")
    assertEquals("kdeconnect.share.request", packet.type)
    assertEquals("Hello", packet.sharedText)
}

@Test
fun testCreateUrlSharePacket() {
    val packet = SharePacketsFFI.createUrlShare("https://example.com")
    assertEquals("kdeconnect.share.request", packet.type)
    assertEquals("https://example.com", packet.sharedUrl)
}

@Test(expected = IllegalArgumentException::class)
fun testEmptyTextShareThrows() {
    SharePacketsFFI.createTextShare("")
}
```

### Integration Tests (Espresso)

```kotlin
@Test
fun testShareTextFromApp() {
    // Launch app, pair device
    // Share text via Share sheet
    // Verify toast appears
    // Verify packet sent (mock backend)
}
```

### Performance Tests

```kotlin
@Test
fun testMemoryUsageDuringTransfer() {
    val initialMemory = getMemoryUsage()
    repeat(10) {
        transferLargeFile()
    }
    val finalMemory = getMemoryUsage()
    assert(finalMemory - initialMemory < 50_MB)
}
```

---

## Test Data Files

### Recommended Test Files

**Small Files** (< 1MB):
- text.txt (plain text)
- image.jpg (JPEG image)
- document.pdf (PDF document)

**Medium Files** (10-50MB):
- video.mp4 (Video)
- presentation.pptx (Office document)

**Large Files** (100MB+):
- movie.mkv (Large video)
- archive.zip (Compressed archive)

**Special Cases**:
- unicode-💩.txt (Unicode in filename)
- spaces in name.jpg (Spaces in filename)
- very-long-filename-that-exceeds-normal-length.txt

---

## References

- **Issue #53 Plan**: docs/issues/issue-53-share-plugin-plan.md
- **Phase Completions**: docs/issues/issue-53-phase{1-4}-complete.md
- **FFI Integration Guide**: docs/guides/FFI_INTEGRATION_GUIDE.md
- **KDE Connect Protocol**: https://invent.kde.org/network/kdeconnect-kde
- **SharePacketsFFI.kt**: Implementation reference
- **PayloadTransferFFI.kt**: Future file transfer reference

---

**Document Version**: 1.0
**Created**: 2026-01-16
**Status**: Testing Documentation Complete
**Target Testers**: Developers, QA, Community testers
**Estimated Testing Time**: 2-3 hours for full suite
