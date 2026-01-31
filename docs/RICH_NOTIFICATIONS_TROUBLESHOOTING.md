# Rich Notifications Troubleshooting Guide

**Solve common issues with Rich Notifications in COSMIC Connect.**

**Version:** 1.0.0
**Last Updated:** January 31, 2026

---

## Table of Contents

- [Quick Diagnostics](#quick-diagnostics)
- [Images and Icons Not Showing](#images-and-icons-not-showing)
- [Links Not Clickable](#links-not-clickable)
- [Formatting Lost](#formatting-lost)
- [Actions Not Working](#actions-not-working)
- [Replies Not Working](#replies-not-working)
- [Performance Issues](#performance-issues)
- [App-Specific Problems](#app-specific-problems)
- [Connection Issues](#connection-issues)
- [Advanced Debugging](#advanced-debugging)

---

## Quick Diagnostics

Before diving into specific issues, run through this checklist:

### Basic Connectivity Check

```bash
# On COSMIC Desktop, verify devices are connected
# Check the COSMIC Connect applet/panel icon

# If using CLI tools, check for active connections
ss -tuln | grep 171[4-9]
```

### Android Side Check

1. Open **COSMIC Connect** app
2. Verify your desktop shows as **Connected** (green indicator)
3. Check **Notifications** plugin is enabled
4. Tap **Notifications** > **Test Notification** (if available)

### Desktop Side Check

1. Check COSMIC Connect applet shows device as connected
2. Verify notifications are enabled in COSMIC Settings
3. Send a test notification from Android

### Quick Diagnostic Flowchart

```
Notifications not working?
         |
         v
+--------------------+
| Devices connected? |--No--> Check WiFi, pairing
+--------+-----------+
         |Yes
         v
+--------------------+
| Plugin enabled?    |--No--> Enable Notifications plugin
+--------+-----------+
         |Yes
         v
+--------------------+
| Permission granted?|--No--> Grant notification access
+--------+-----------+
         |Yes
         v
+--------------------+
| App not filtered?  |--No--> Check Notification Filter
+--------+-----------+
         |Yes
         v
+--------------------+
| Test notification  |--Fail--> Check connection/firewall
| works?             |
+--------+-----------+
         |Yes
         v
    Specific feature issue
    (see sections below)
```

---

## Images and Icons Not Showing

### Symptom

Notifications appear on desktop but without app icons or images.

### Possible Causes and Solutions

#### 1. Privacy Settings Blocking Images

**Cause**: The app has "Block Images" privacy setting enabled.

**Solution**:
1. Open COSMIC Connect on Android
2. Go to paired device > **Notifications** > **Notification Filter**
3. Find the app in question
4. Ensure "Block Images" is NOT enabled

**How to verify**:
```
If you see: "Block Images: ON" for the app
Then icons will not be transferred for that app
```

#### 2. Payload Transfer Failed

**Cause**: Network issues prevented the icon payload from being received.

**Solution**:
1. Check network stability between devices
2. Ensure both devices are on the same network
3. Try sending a test notification
4. Check if other icons work (issue may be app-specific)

**How to verify**:
```bash
# On desktop, check for payload-related errors in logs
journalctl -f | grep -i cosmic
```

#### 3. App Doesn't Provide Icons

**Cause**: Some apps don't attach icons to their notifications.

**Solution**:
- This is an app limitation, not a COSMIC Connect issue
- The notification will display without an icon
- Consider this normal behavior for such apps

**Apps known to have limited icons**:
- Some system apps
- Older apps not updated for modern Android
- Apps with strict privacy requirements

#### 4. Icon Extraction Failed

**Cause**: Android couldn't extract the icon due to package access issues.

**Solution**:
1. Restart the COSMIC Connect app on Android
2. If persistent, try unpairing and re-pairing devices
3. Clear COSMIC Connect cache on Android

**How to verify**:
```bash
# On Android, check COSMIC Connect logs
adb logcat | grep "COSMIC/NotificationsPlugin"
# Look for "Error extracting icon" messages
```

#### 5. Icon Size/Format Issues

**Cause**: Icon was too large or in an unsupported format.

**Solution**:
- Icons are normalized to 96x96 pixels
- Icons are converted to PNG format
- Very large icons may fail to transfer

**Technical details**:
- Maximum transfer size: ~100KB per icon
- Format: PNG (ARGB_8888)
- Compression: 90% quality

---

## Links Not Clickable

### Symptom

URLs in notifications are displayed as plain text and cannot be clicked.

### Possible Causes and Solutions

#### 1. App Continuity Not Enabled

**Cause**: The App Continuity feature is required for link opening.

**Solution**:
1. Open COSMIC Connect settings
2. Enable **App Continuity** feature
3. Verify desktop browser is configured

See [App Continuity Guide](APP_CONTINUITY.md) for full setup.

#### 2. Messaging App Not Recognized

**Cause**: The messaging app is not in the registry of known apps.

**Solution**:
- Currently supported apps have pre-configured web URLs
- Unsupported apps show notifications but links may not be actionable
- Check if the app has a web interface (Telegram Web, WhatsApp Web, etc.)

**Supported apps for link opening**:
- Telegram (web.telegram.org)
- WhatsApp (web.whatsapp.com)
- Discord (discord.com)
- Slack (app.slack.com)

#### 3. URL Not Detected in Notification

**Cause**: The notification text doesn't contain a recognizable URL.

**Solution**:
- This is expected behavior for notifications without URLs
- Some apps include URLs in expanded content only
- Desktop may only receive summary text

#### 4. Desktop Browser Not Configured

**Cause**: No default browser set on COSMIC Desktop.

**Solution**:
1. Open **COSMIC Settings** > **Default Applications**
2. Set a default web browser
3. Verify the browser can open URLs from command line

```bash
# Test URL opening
xdg-open "https://example.com"
```

---

## Formatting Lost

### Symptom

Notification text appears without formatting (bold, line breaks, etc.).

### Possible Causes and Solutions

#### 1. Android Notification API Limitations

**Cause**: Android notifications use limited formatting.

**Explanation**:
- Android notifications support basic text only
- Rich formatting (HTML, Markdown) is not preserved
- This is a fundamental Android API limitation

**What IS preserved**:
- Line breaks in message text
- Basic text content
- Sender names in conversations

**What is NOT preserved**:
- Bold/italic/underline
- Colors
- Custom fonts
- Complex layouts

#### 2. Privacy Settings Blocking Content

**Cause**: "Block Content" privacy setting hides notification text.

**Solution**:
1. Check Notification Filter for the app
2. Disable "Block Content" if you want full text

**How to verify**:
If you see only "New notification from [App]" instead of actual content,
content blocking is likely enabled.

#### 3. Notification Ticker Text Used

**Cause**: Some apps provide different text in ticker vs. expanded view.

**Explanation**:
- COSMIC Connect prioritizes expanded/big text
- Falls back to ticker text if expanded isn't available
- Some apps only populate ticker

**What COSMIC Connect extracts (in priority order)**:
1. Conversation messages (for messaging apps)
2. Big text (EXTRA_BIG_TEXT)
3. Regular text (EXTRA_TEXT)
4. Ticker text (fallback)

#### 4. Desktop Notification Service Limitations

**Cause**: freedesktop notification spec has its own limitations.

**Solution**:
- Most desktop notification systems support basic text only
- This is expected behavior
- Consider using COSMIC Connect's notification history for full content

---

## Actions Not Working

### Symptom

Clicking action buttons on desktop notifications doesn't do anything.

### Possible Causes and Solutions

#### 1. Notification Already Dismissed on Android

**Cause**: The notification was dismissed on Android before action was triggered.

**Solution**:
1. Ensure the notification is still active on Android
2. Act on notifications promptly before they're cleared
3. Check if Android auto-dismissed the notification

#### 2. Action Not Supported by App

**Cause**: The app's notification action couldn't be triggered.

**Solution**:
- Some actions require the app to be running
- Some actions have expired PendingIntents
- Try the action directly on Android to verify it works

**How to verify**:
```bash
# Check Android logs for action failures
adb logcat | grep "COSMIC/NotificationsPlugin"
# Look for "Triggering action failed" messages
```

#### 3. Network Latency

**Cause**: Action packet didn't reach Android in time.

**Solution**:
1. Check network connection between devices
2. Reduce network latency (use 5GHz WiFi)
3. Retry the action

#### 4. Action Button Not Transferred

**Cause**: The action was filtered or not extracted.

**Explanation**:
- Reply actions are handled separately (not shown as action buttons)
- Some actions may be filtered out
- Maximum actions may be limited

**Actions that may be filtered**:
- Actions with RemoteInputs (converted to reply functionality)
- Actions without titles
- Duplicate actions

---

## Replies Not Working

### Symptom

Inline reply feature doesn't send replies or replies fail silently.

### Possible Causes and Solutions

#### 1. App Doesn't Support Inline Reply

**Cause**: Not all apps implement Android's RemoteInput API.

**How to verify**:
- Check if the notification has a Reply action on Android
- If no Reply action exists, the app doesn't support inline replies

**Apps that support inline replies**:
- Google Messages
- WhatsApp
- Telegram
- Gmail
- Most modern messaging apps

**Apps that may NOT support inline replies**:
- Older versions of apps
- Some third-party email clients
- Social media apps (varies)

#### 2. Notification Expired

**Cause**: The original notification was dismissed, invalidating the reply intent.

**Solution**:
1. Reply before dismissing the notification on Android
2. If notification was cleared, send a new message from Android

**How to verify**:
```bash
# Check for "No such notification" errors
adb logcat | grep "COSMIC/NotificationsPlugin"
```

#### 3. Reply Intent Canceled

**Cause**: The app canceled the PendingIntent.

**Solution**:
- This happens when the app updates or clears its state
- Restart the messaging app on Android
- Send a new message to get a fresh notification

**Error indication**:
```
PendingIntent.CanceledException: ...
```

#### 4. Reply Message Empty

**Cause**: Empty reply message was sent.

**Solution**:
- Ensure you typed a reply message
- Check that the desktop input field captured your text
- COSMIC Connect requires non-empty replies

#### 5. Network Issues

**Cause**: Reply packet didn't reach Android.

**Solution**:
1. Verify device connection
2. Check network stability
3. Try replying again

---

## Performance Issues

### Symptom

Notifications arrive slowly, icons take too long, or system feels sluggish.

### Possible Causes and Solutions

#### 1. Too Many Notifications

**Cause**: High notification volume overwhelming the sync.

**Solution**:
1. Use Notification Filter to disable noisy apps
2. Disable notifications for games and promotional apps
3. Enable "Screen Off Only" mode to reduce volume

**Recommended filtering**:
- Game notifications: Disable
- Promotional app notifications: Disable
- Social media (optional): Consider limiting

#### 2. Large Icon Payloads

**Cause**: Transferring many icons uses bandwidth and CPU.

**Solution**:
1. Enable "Block Images" for non-essential apps
2. Ensure stable WiFi connection
3. Consider notification volume vs. icon importance

**Performance impact of icons**:
- Each icon: ~10-100KB transfer
- Encoding time: ~50-100ms on phone
- Network transfer: Depends on connection

#### 3. Network Congestion

**Cause**: WiFi network is congested or slow.

**Solution**:
1. Use 5GHz WiFi instead of 2.4GHz
2. Reduce other network activity
3. Move closer to WiFi router
4. Check for interference sources

#### 4. Desktop Processing Delays

**Cause**: Desktop notification system is slow.

**Solution**:
1. Check desktop CPU/memory usage
2. Reduce notification timeout if they stack up
3. Consider disabling notification stacking

#### 5. Memory Usage on Android

**Cause**: COSMIC Connect using too much memory.

**Solution**:
1. Restart COSMIC Connect app periodically
2. Clear notification cache if available
3. Check for memory leaks (report if found)

---

## App-Specific Problems

### Google Messages

**Issue**: Messages not showing conversation history

**Solution**: Google Messages shows only the latest message in notifications.
This is expected behavior. Full conversation is in the app.

**Issue**: Replies not appearing in conversation

**Solution**: Ensure Google Messages app is updated. Older versions may have
reply bugs.

### WhatsApp

**Issue**: Media messages show as "Photo" or "Video" only

**Solution**: WhatsApp doesn't include media in notification text.
Only text messages show full content.

**Issue**: Group messages missing sender info

**Solution**: Ensure Android 7.0+ for full conversation support.
Older Android versions may lack MessagingStyle.

### Telegram

**Issue**: Secret chats not syncing

**Solution**: This is expected. Secret chats are encrypted on-device
and intentionally don't sync through Android notification system.

### Gmail

**Issue**: Email body not showing

**Solution**: Gmail notifications typically show subject only.
Full body requires opening the email.

**Issue**: Reply sends but email doesn't appear in Sent

**Solution**: Gmail inline replies use a different flow. Check
Sent folder after a few minutes.

### Banking Apps

**Issue**: Notifications show "Content hidden"

**Solution**: This is expected security behavior. Banking apps
hide content for security. Do not disable this protection.

### Signal

**Issue**: No web URL for opening

**Solution**: Signal doesn't have a traditional web interface.
Use Signal Desktop companion app instead.

---

## Connection Issues

### Symptom

Notifications stop syncing or sync intermittently.

### Solutions

#### Check Device Connection

1. Open COSMIC Connect on Android
2. Verify desktop shows as "Connected"
3. If disconnected, check WiFi on both devices

#### Restart Notification Listener

1. Android Settings > Apps > COSMIC Connect
2. Force Stop the app
3. Reopen COSMIC Connect
4. Check if notification permission needs re-granting

#### Re-pair Devices

1. Unpair devices in COSMIC Connect
2. Pair again
3. Re-enable Notifications plugin

#### Check Firewall

```bash
# Ensure ports are open (NixOS example)
# TCP/UDP ports 1714-1764 should be allowed
sudo nft list ruleset | grep 171
```

---

## Advanced Debugging

### Enable Verbose Logging (Android)

```bash
# Connect phone via USB with debugging enabled
adb logcat -s "COSMIC/NotificationsPlugin" "CConnect/Messaging"
```

### Check Packet Contents

```bash
# Monitor network traffic (requires root/sudo)
sudo tcpdump -i any tcp port 1716 -A | grep notification
```

### Desktop Notification Debugging

```bash
# Monitor freedesktop notifications
dbus-monitor "interface='org.freedesktop.Notifications'"
```

### Verify Packet Format

If you suspect packet format issues, check that notifications contain:

```json
{
  "id": 1234567890,
  "type": "cconnect.notification",
  "body": {
    "id": "notification-key",
    "appName": "App Name",
    "title": "Title",
    "text": "Body text",
    "isClearable": true,
    "time": "1704067200000",
    "silent": "false"
  }
}
```

### Report a Bug

If you've tried everything and the issue persists:

1. Collect logs from both devices
2. Note exact steps to reproduce
3. Include device/OS versions
4. Open an issue on GitHub with details

---

## Related Documentation

- [Rich Notifications User Guide](RICH_NOTIFICATIONS.md) - Feature overview
- [Rich Notifications Protocol](protocol/RICH_NOTIFICATIONS_PROTOCOL.md) - Technical specification
- [App Continuity Troubleshooting](APP_CONTINUITY_TROUBLESHOOTING.md) - For link issues
- [User Guide](USER_GUIDE.md) - General COSMIC Connect usage

---

**Still having issues? Open an issue on the COSMIC Connect GitHub repository with detailed logs and reproduction steps.**
