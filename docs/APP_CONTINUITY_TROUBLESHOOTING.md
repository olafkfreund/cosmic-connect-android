# App Continuity Troubleshooting

**Last Updated:** January 31, 2026

This guide helps you solve common issues when using the App Continuity feature (opening links and content across devices) in COSMIC Connect.

---

## Table of Contents

- [Quick Diagnostics](#quick-diagnostics)
- [URL Not Opening](#url-not-opening)
- [File Transfer Issues](#file-transfer-issues)
- [Notification Problems](#notification-problems)
- [Security Prompts](#security-prompts)
- [Desktop-Specific Issues](#desktop-specific-issues)
- [Android-Specific Issues](#android-specific-issues)
- [Error Messages](#error-messages)
- [Getting Help](#getting-help)

---

## Quick Diagnostics

Before troubleshooting specific issues, verify your basic setup:

### Connectivity Checklist

- [ ] Both devices are paired and showing "Connected" status
- [ ] Both devices are on the same WiFi network (or Bluetooth is connected)
- [ ] COSMIC Connect is running on both devices
- [ ] Share plugin is enabled on both devices

### Test Your Connection

1. **From Android**: Go to device details, tap "Ping" - you should see a response
2. **From Desktop**: Click the device in the applet, send a test ping
3. **Verify**: Both devices show the connection as active

If the ping fails, see the main [Troubleshooting Guide](USER_GUIDE.md#troubleshooting) for connection issues.

---

## URL Not Opening

### Problem: URL Sent But Nothing Happens

**Symptoms:**
- You share a URL but it doesn't open on the target device
- No error message appears
- The sending device shows "sent" confirmation

**Solutions:**

1. **Check URL Scheme Support**

   Not all URL types are supported. Verify your URL starts with:
   - `https://` (recommended)
   - `http://`
   - `mailto:`
   - `tel:`
   - `sms:`
   - `geo:`

   **Blocked URLs:**
   - `javascript:` links
   - `file://` local files
   - `data:` embedded content
   - Internal IP addresses (192.168.x.x, 10.x.x.x, 127.0.0.1)

2. **Check Notification Settings**

   The URL might be waiting for approval:
   - **Android**: Check notification shade for COSMIC Connect notification
   - **Desktop**: Check system notifications or panel applet

3. **Verify Share Plugin is Enabled**
   - Open device settings in COSMIC Connect
   - Ensure "Share" plugin is toggled ON

4. **Try a Simple Test URL**

   Send `https://example.com` as a test. If this works, the issue is with the specific URL.

### Problem: URL Opens Wrong Application

**Symptoms:**
- URL opens, but in the wrong browser/app
- Links always open in a specific app instead of browser

**Solutions:**

1. **Android: Set Default Browser**
   - Go to Settings > Apps > Default apps > Browser app
   - Select your preferred browser

2. **Desktop: Set Default Browser**
   - COSMIC Settings > Applications > Default Applications
   - Set your preferred web browser

3. **App-Specific URLs**

   Some URLs are designed to open specific apps:
   - `mailto:` opens email client (expected behavior)
   - `tel:` opens phone/dialer
   - YouTube/Twitter links may open their apps if installed

### Problem: "Blocked for Security" Message

**Symptoms:**
- URL is rejected with security warning
- Message says URL scheme is not allowed

**Solutions:**

1. **This is intentional** - some URL types are blocked for your protection:
   - `javascript:` - Could execute malicious code
   - `file://` - Could access local files
   - Internal IPs - Could access private network resources

2. **Workaround for internal URLs:**
   - Copy the URL text manually instead
   - Or access it directly on the target device

---

## File Transfer Issues

### Problem: File Transfer Fails

**Symptoms:**
- "Transfer failed" error
- Progress bar stops partway
- File never arrives

**Solutions:**

1. **Check File Size**

   Very large files may timeout:
   - Files over 100 MB may take longer
   - Ensure stable WiFi connection
   - Try over faster network (5GHz WiFi)

2. **Check Storage Space**

   Receiving device needs free space:
   - Android: Settings > Storage
   - Desktop: Check available disk space

3. **Check Permissions**

   **Android:**
   - Settings > Apps > COSMIC Connect > Permissions
   - Enable "Files and media" or "Storage"

   **Desktop:**
   - Ensure download directory is writable

4. **Network Stability**

   If transfers frequently fail:
   - Move closer to WiFi router
   - Check for network interference
   - Try with other devices to rule out network issues

### Problem: File Opens with Wrong Application

**Symptoms:**
- File transfers successfully
- Opens in unexpected application

**Solutions:**

1. **Check file associations**
   - Desktop: Right-click file > Open With > Set default
   - Android: Settings > Apps > Default apps

2. **File type not recognized**

   If file has unusual extension:
   - Rename to standard extension
   - Manually choose "Open with" application

---

## Notification Problems

### Problem: No Notification When URL/File Arrives

**Symptoms:**
- Content arrives but no notification appears
- You only notice shared content later

**Solutions:**

1. **Enable COSMIC Connect Notifications**

   **Android:**
   - Settings > Apps > COSMIC Connect > Notifications
   - Enable all notification categories

   **Desktop:**
   - Check notification settings in COSMIC Settings
   - Ensure COSMIC Connect notifications aren't muted

2. **Check Do Not Disturb Mode**

   DND may be blocking notifications:
   - Android: Check quick settings for DND
   - Desktop: Check system tray for DND status

3. **Check Plugin Settings**

   In COSMIC Connect app:
   - Open device settings
   - Share plugin > Enable notifications

### Problem: Too Many Notifications

**Symptoms:**
- Getting notification for every URL/file
- Want automatic opening instead

**Solutions:**

1. **Enable Auto-Open**

   In device settings:
   - Share plugin > Auto-open URLs
   - This opens trusted content without prompts

2. **Add Device to Trusted List**

   Desktop settings:
   - COSMIC Connect > Security
   - Add device to "Trusted devices" list

---

## Security Prompts

### Problem: Always Asked to Confirm

**Symptoms:**
- Every URL/file requires confirmation
- Want faster sharing without prompts

**Solutions:**

1. **Enable Auto-Open for URLs**

   In COSMIC Connect settings:
   - Device settings > Share
   - Toggle "Auto-open URLs"

2. **Trust the Sending Device**

   Desktop:
   - COSMIC Connect settings
   - Mark device as "Trusted"

**Note:** We recommend keeping confirmations enabled for security.

### Problem: Can't Send Certain URLs

**Symptoms:**
- Specific URLs always rejected
- Security error message

**Solutions:**

1. **Check if URL is blocked**

   These URL patterns are blocked for security:
   - Internal network addresses
   - JavaScript URLs
   - File system URLs
   - Data URLs

2. **Use Alternative Method**

   For blocked URLs:
   - Copy URL as text and paste manually
   - Use clipboard sync to transfer the URL text

---

## Desktop-Specific Issues

### Problem: "Open on Phone" Menu Missing

**Symptoms:**
- Right-click menu doesn't show COSMIC Connect option
- Can't send URLs from desktop

**Solutions:**

1. **Check COSMIC Connect is Running**
   - Look for applet in system panel
   - Start COSMIC Connect if not running

2. **Check Integration is Installed**
   - Browser extension may be required
   - Verify COSMIC Connect desktop integration

3. **Restart Desktop Session**
   - Log out and back in
   - Or restart COSMIC Connect applet

### Problem: Browser Extension Not Working

**Symptoms:**
- Browser doesn't show COSMIC Connect options
- Extension icon missing or grayed out

**Solutions:**

1. **Reinstall Extension**
   - Remove and re-add browser extension
   - Check extension has permissions

2. **Check Native Messaging Host**
   - COSMIC Connect installs a native messaging component
   - May need to be installed separately on some distros

---

## Android-Specific Issues

### Problem: Share Sheet Doesn't Show COSMIC Connect

**Symptoms:**
- Can't find COSMIC Connect in share menu
- Only other apps appear

**Solutions:**

1. **Check App is Running**
   - Open COSMIC Connect app
   - Ensure at least one device is paired

2. **Clear Share Sheet Cache**
   - Android caches share targets
   - Restart phone or clear cache:
     - Settings > Apps > Android System > Clear cache

3. **Check App Isn't Restricted**
   - Settings > Apps > COSMIC Connect
   - Ensure app isn't "Disabled" or "Restricted"

### Problem: App Crashes When Sharing

**Symptoms:**
- COSMIC Connect crashes when receiving share intent
- Error message or app closes

**Solutions:**

1. **Update the App**
   - Check for updates in Play Store/F-Droid

2. **Clear App Data**
   - Settings > Apps > COSMIC Connect
   - Storage > Clear Cache (try this first)
   - Storage > Clear Data (warning: removes pairing)

3. **Reinstall App**
   - Uninstall and reinstall COSMIC Connect
   - Re-pair your devices

---

## Error Messages

### "URL scheme not supported"

**Meaning:** The URL type isn't allowed for security reasons.

**Common causes:**
- `javascript:` links
- Custom app schemes (some are blocked)
- `file://` URLs

**Solution:** Use a standard URL format (https://) or copy as text.

### "Connection refused"

**Meaning:** The target device rejected the connection.

**Common causes:**
- Device is offline
- Firewall blocking connection
- COSMIC Connect not running

**Solution:** Verify device is connected and COSMIC Connect is active.

### "Transfer timeout"

**Meaning:** File transfer took too long and was cancelled.

**Common causes:**
- Network too slow
- File too large
- Connection unstable

**Solution:** Try smaller file, faster network, or check connection stability.

### "Permission denied"

**Meaning:** App doesn't have required permissions.

**Common causes:**
- Storage permission not granted
- Network permission revoked

**Solution:** Check and grant permissions in device settings.

### "Device not trusted"

**Meaning:** The sending device isn't in your trusted list.

**Common causes:**
- Devices paired but not trusted
- Trust was revoked

**Solution:** Add device to trusted list or approve manually.

---

## Getting Help

If these solutions don't resolve your issue:

1. **Check Main Troubleshooting Guide**
   - [User Guide - Troubleshooting](USER_GUIDE.md#troubleshooting)

2. **Search Existing Issues**
   - [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)

3. **Report a Bug**
   - [Create New Issue](https://github.com/olafkfreund/cosmic-connect-android/issues/new)
   - Include:
     - Android version and device model
     - COSMIC Desktop version
     - Steps to reproduce
     - Error messages (if any)
     - Logs (if possible)

4. **Community Help**
   - [GitHub Discussions](https://github.com/olafkfreund/cosmic-connect-android/discussions)

---

## Related Documentation

- [App Continuity Guide](APP_CONTINUITY.md) - Feature overview and usage
- [User Guide](USER_GUIDE.md) - Complete COSMIC Connect guide
- [FAQ](FAQ.md) - Frequently asked questions

---

**Still having issues?** Don't hesitate to reach out - we're here to help!
