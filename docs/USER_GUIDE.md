# COSMIC Connect Android - User Guide

**Welcome to COSMIC Connect!** This guide will help you set up and use COSMIC Connect to seamlessly connect your Android device with your COSMIC Desktop computer.

**Version:** 1.0.0-beta
**Last Updated:** 2026-01-17

---

## Table of Contents

- [What is COSMIC Connect?](#what-is-cosmic-connect)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Initial Setup](#initial-setup)
- [Pairing Your Devices](#pairing-your-devices)
- [Using Features](#using-features)
- [Settings](#settings)
- [Troubleshooting](#troubleshooting)
- [Privacy & Security](#privacy--security)
- [Getting Help](#getting-help)

---

## What is COSMIC Connect?

COSMIC Connect enables your Android phone or tablet to communicate with your COSMIC Desktop computer. Share files, sync your clipboard, see phone notifications on your desktop, control media playback, and much more - all wirelessly and securely.

**Key Points:**
- 🔐 **Secure:** All communication is encrypted with TLS
- 🏠 **Private:** Everything stays local on your network (no cloud services)
- 🆓 **Free & Open Source:** GPL-3.0 licensed
- 🔄 **Compatible:** Works with KDE Connect protocol v8

---

## Features

### 📁 File & URL Sharing
Share files and web links instantly between your phone and desktop.

### 📋 Clipboard Sync
Copy text on your phone, paste on your desktop (and vice versa).

### 🔔 Notification Mirroring
See your phone's notifications on your desktop. Reply to messages without picking up your phone.

### 🎵 Media Control
Use your phone as a remote control for media playing on your desktop.

### 💬 SMS Integration
Send and receive text messages from your desktop.

### 🔋 Battery Monitor
Check your phone's battery level from your desktop.

### 📍 Find My Phone
Make your phone ring to help you find it.

### ⚡ Run Commands
Execute predefined commands on your desktop remotely.

### 🎮 Remote Input
Use your phone as a touchpad or keyboard for your desktop.

---

## Requirements

### Android Device
- **Android Version:** 6.0 (Marshmallow) or later
- **Android 14+ recommended** for best performance
- At least 50 MB free storage
- WiFi or Bluetooth capability

### COSMIC Desktop
- **COSMIC Desktop:** Latest version with cosmic-applet-kdeconnect installed
- **Operating System:** Pop!_OS 24.04+ or any Linux distribution with COSMIC Desktop
- Same WiFi network as your Android device (or Bluetooth)

### Network
- Both devices on the same WiFi network **OR**
- Bluetooth enabled on both devices
- Port 1716 open (for WiFi discovery)
- Ports 1764+ available (for file transfers)

---

## Installation

### Option 1: Google Play Store (Recommended)

1. Open **Google Play Store** on your Android device
2. Search for **"COSMIC Connect"**
3. Tap **Install**
4. Wait for installation to complete
5. Tap **Open** to launch

### Option 2: F-Droid

1. Open **F-Droid** app on your Android device
2. Search for **"COSMIC Connect"**
3. Tap **Install**
4. Grant installation permission if prompted
5. Open the app

### Option 3: Direct APK Download

1. Download the latest APK from [GitHub Releases](https://github.com/olafkfreund/cosmic-connect-android/releases)
2. Open **Settings** → **Security**
3. Enable **"Install from unknown sources"** (or "Install unknown apps")
4. Open the downloaded APK file
5. Tap **Install**
6. Open the app

**Download Links:**
- Universal APK (works on all devices): `cosmicconnect-universal.apk`
- ARM64 (most modern phones): `cosmicconnect-arm64-v8a.apk`
- ARM (older phones): `cosmicconnect-armeabi-v7a.apk`
- x86_64 (emulators/tablets): `cosmicconnect-x86_64.apk`

**Which APK should I download?**
- If unsure, use the **Universal APK** (larger file, works everywhere)
- If you have a modern phone (2018+), use **ARM64**
- For emulators or x86 tablets, use **x86_64**

---

## Initial Setup

### First Launch

When you first open COSMIC Connect, you'll need to grant several permissions:

#### 1. **Location Permission** (Required)
- **Why:** Android requires this to discover nearby devices via WiFi
- **What we do:** We only use it for WiFi network scanning, never for GPS location
- Tap **Allow** when prompted

#### 2. **Notification Access** (Optional)
- **Why:** To mirror your phone notifications to your desktop
- **What we do:** Read notification contents to display on desktop
- Go to **Settings** → **Notification Access** → Enable **COSMIC Connect**

#### 3. **Phone & SMS** (Optional)
- **Why:** To send/receive text messages from your desktop
- **What we do:** Access your SMS messages and phone calls for desktop integration
- Tap **Allow** when prompted

#### 4. **Storage** (Optional)
- **Why:** To send and receive files
- **What we do:** Read files you choose to share and save received files
- Tap **Allow** when prompted

#### 5. **Battery Optimization**
The app will ask to be excluded from battery optimization:
- **Why:** To maintain connection in the background
- **Recommendation:** Tap **Allow** for best experience
- You can change this later in Settings

**Note:** You can skip optional permissions and enable them later when you want to use those features.

---

## Pairing Your Devices

### Step 1: Ensure Both Devices Are Ready

**On Your COSMIC Desktop:**
1. Open **COSMIC Applet Panel**
2. Ensure **cosmic-applet-kdeconnect** is running
3. Icon should appear in system tray

**On Your Android Device:**
1. Open **COSMIC Connect**
2. Ensure WiFi or Bluetooth is enabled
3. Both devices must be on the same network

### Step 2: Discover Devices

**On Your Android Device:**
1. Open **COSMIC Connect**
2. Tap the **Refresh** button (↻) in the top right
3. Wait a few seconds for devices to appear
4. You should see your COSMIC Desktop computer listed

**What if my computer doesn't appear?**
- See [Troubleshooting: Device Not Found](#device-not-found)

### Step 3: Initiate Pairing

**Option A: Pair from Android (Recommended)**
1. Tap on your computer's name in the device list
2. Tap **Pair** button
3. A pairing request appears on your COSMIC Desktop
4. Accept the pairing request on your desktop
5. Pairing complete! 🎉

**Option B: Pair from Desktop**
1. On COSMIC Desktop, click the COSMIC Connect icon
2. Click on your phone's name
3. Click **Pair**
4. Accept the pairing request on your Android device
5. Pairing complete! 🎉

### Step 4: Verify Connection

Once paired, you should see:
- **On Android:** Green checkmark next to your computer's name
- **On Desktop:** Your phone listed as "Connected"
- **Status:** Should say "Paired and Connected"

**Congratulations!** Your devices are now connected.

---

## Using Features

### 📁 Sharing Files

**From Android to Desktop:**
1. Open any app with files (Photos, Files, etc.)
2. Select the file(s) you want to share
3. Tap the **Share** button
4. Select **COSMIC Connect**
5. Choose your desktop computer
6. File transfers automatically!

**From Desktop to Android:**
1. On COSMIC Desktop, right-click any file
2. Select **Send to** → **[Your Phone Name]**
3. File appears in your phone's **Downloads** folder

**Supported File Types:**
- Photos and videos
- Documents (PDF, Office files)
- APK files
- Audio files
- Any file type

**File Size Limits:**
- No hard limit
- Large files (100+ MB) may take time depending on your WiFi speed
- Progress bar shows transfer status

### 📋 Clipboard Sync

**How it works:**
1. Ensure clipboard sync is enabled in settings
2. Copy text on your phone
3. Text is automatically available on your desktop
4. Works both ways!

**Steps:**
1. Open **Settings** in COSMIC Connect
2. Find your computer in the list
3. Enable **Clipboard** plugin
4. Copy any text on either device
5. Paste on the other device

**What can be synced:**
- Plain text
- URLs
- Formatted text (limited)

**What cannot be synced:**
- Images (use file sharing instead)
- Large amounts of text (>100 KB)

### 🔔 Notification Mirroring

**Setup:**
1. Grant Notification Access permission (see Initial Setup)
2. Open **Settings** in COSMIC Connect
3. Enable **Notifications** plugin for your desktop
4. Configure which apps to mirror

**Features:**
- See phone notifications on desktop
- Reply to messages directly
- Dismiss notifications on either device
- Synchronized notification state

**Privacy Controls:**
- Choose which apps to mirror
- Hide sensitive notification content
- Disable for specific times

**Replying to Messages:**
1. When a notification appears on desktop
2. Click the **Reply** button
3. Type your response
4. Press Enter to send

**Supported Messaging Apps:**
- WhatsApp
- Telegram
- Signal
- SMS/MMS
- Most messaging apps

### 🎵 Media Control

**Setup:**
1. Open **Settings** in COSMIC Connect
2. Enable **MPRIS** (Media) plugin
3. Play media on your desktop

**Features:**
- Play/Pause
- Next/Previous track
- Volume control
- See current track info
- Album art display

**Using the Remote:**
1. Open COSMIC Connect
2. Tap on your computer
3. Media controls appear when media is playing
4. Tap buttons to control playback

**Supported Players:**
- Spotify
- VLC
- Firefox/Chrome
- Rhythmbox
- Most media players

### 💬 SMS from Desktop

**Setup:**
1. Grant Phone & SMS permission (see Initial Setup)
2. Enable **SMS** plugin in settings
3. Open COSMIC Connect on desktop

**Features:**
- Read SMS messages on desktop
- Send SMS from desktop
- Conversation history
- Contact integration

**Sending a Message:**
1. On desktop, click COSMIC Connect icon
2. Select **Send SMS**
3. Choose recipient or enter phone number
4. Type your message
5. Click **Send**

**Receiving Messages:**
- New SMS appear as desktop notifications
- Click notification to read and reply
- Full conversation history available

### 🔋 Battery Monitor

**Setup:**
1. Enable **Battery** plugin in settings
2. Battery status appears on desktop automatically

**What You'll See:**
- Current battery level (%)
- Charging status
- Estimated time remaining
- Battery health indicator

**Desktop Display:**
- Icon shows battery level
- Tooltip shows detailed info
- Low battery notifications (optional)

### 📍 Find My Phone

**How to Use:**
1. On COSMIC Desktop, click COSMIC Connect icon
2. Select your phone
3. Click **Ring** or **Find My Phone**
4. Your phone rings at maximum volume
5. Press Volume button on phone to stop ringing

**Use Cases:**
- Phone is misplaced nearby
- Phone is on silent/vibrate mode
- Quick way to locate device

**Note:** Requires phone to be connected and within network range.

### ⚡ Run Commands

**Setup:**
1. Enable **Run Command** plugin
2. On desktop, configure available commands
3. Commands appear on Android app

**Pre-configured Commands:**
- Lock screen
- Suspend computer
- Hibernate
- Open specific applications
- Run custom scripts

**Running a Command:**
1. Open COSMIC Connect on Android
2. Tap on your computer
3. Tap **Run Command**
4. Select command from list
5. Command executes on desktop

**Creating Custom Commands:**
1. On COSMIC Desktop, open COSMIC Connect settings
2. Go to Run Command section
3. Add new command with name and script
4. Command appears on Android

### 🎮 Remote Input (Touchpad & Keyboard)

**Touchpad Mode:**
1. Enable **Mousepad** plugin
2. Tap **Remote Input** on Android
3. Use phone screen as touchpad
4. Move finger to move cursor
5. Tap to click

**Keyboard Mode:**
1. Open **Remote Input**
2. Switch to **Keyboard** tab
3. Type on your phone keyboard
4. Text appears on desktop
5. Use for quick text entry

**Features:**
- Multi-touch gestures
- Right-click support
- Scroll support
- Keyboard shortcuts
- Special keys (Ctrl, Alt, etc.)

---

## Settings

### Accessing Settings

1. Open COSMIC Connect
2. Tap **≡** (menu) in top left
3. Select **Settings**

### General Settings

**Device Name:**
- Customize how your phone appears to other devices
- Tap **Device Name** to change
- Use a recognizable name

**Discovery:**
- Enable/disable device discovery
- Choose discovery method (WiFi/Bluetooth)
- Configure network interfaces

**Connections:**
- Preferred connection method
- Connection timeout settings
- Reconnection behavior

### Plugin Settings

Each paired device has its own plugin configuration:

1. Tap on a paired device in settings
2. See list of available plugins
3. Enable/disable plugins individually
4. Configure plugin-specific settings

**Common Plugin Settings:**

**Battery:**
- Enable/disable battery sharing
- Low battery notification threshold

**Clipboard:**
- Auto-sync clipboard
- Clipboard history size

**Notifications:**
- Choose apps to mirror
- Notification privacy settings
- Quiet hours configuration

**SMS:**
- Default SMS app integration
- Message sync settings

**Media:**
- Media player selection
- Volume control behavior

### Privacy Settings

**What Data is Shared:**
- Only data from enabled plugins
- No automatic data collection
- All communication is local

**Privacy Controls:**
- Disable specific plugins
- Configure notification privacy
- Control file sharing permissions

### Network Settings

**WiFi:**
- Preferred network selection
- Port configuration (advanced)
- Firewall exceptions

**Bluetooth:**
- Enable Bluetooth discovery
- Pairing PIN settings

### Advanced Settings

**Developer Options:**
- Enable debug logging
- View connection statistics
- Export logs for troubleshooting

**Performance:**
- Battery optimization settings
- Background service behavior
- Sync frequency

---

## Troubleshooting

### Device Not Found

**Problem:** Your computer doesn't appear in the device list.

**Solutions:**

1. **Check Network Connection**
   - Ensure both devices are on the same WiFi network
   - Check WiFi is enabled on both devices
   - Try disabling and re-enabling WiFi

2. **Check COSMIC Desktop App**
   - Ensure cosmic-applet-kdeconnect is running
   - Look for COSMIC Connect icon in system tray
   - Restart the applet if needed

3. **Firewall Issues**
   - Port 1716 must be open for discovery
   - Check firewall settings on desktop
   - Temporarily disable firewall to test

4. **Refresh Discovery**
   - Tap refresh button (↻) on Android
   - Wait 10-15 seconds
   - Try multiple times

5. **Restart Both Apps**
   - Close COSMIC Connect on Android
   - Restart cosmic-applet-kdeconnect on desktop
   - Reopen both apps

6. **Network Range**
   - Ensure devices are within WiFi range
   - Move closer to router
   - Check for network interference

### Pairing Fails

**Problem:** Pairing request fails or times out.

**Solutions:**

1. **Check Permissions**
   - Ensure location permission granted on Android
   - Check notification permission for pairing alerts

2. **Clear Old Pairings**
   - Unpair any old/stale connections
   - Remove device from list
   - Try pairing again fresh

3. **Time Synchronization**
   - Ensure both devices have correct time/date
   - Enable automatic time sync
   - Certificates require accurate time

4. **Network Stability**
   - Check WiFi signal strength
   - Avoid crowded networks
   - Try during off-peak times

5. **Restart Devices**
   - Restart COSMIC Connect app
   - Restart cosmic-applet-kdeconnect
   - Reboot devices if needed

### Connection Drops

**Problem:** Devices connect but disconnect frequently.

**Solutions:**

1. **Battery Optimization**
   - Disable battery optimization for COSMIC Connect
   - Go to Settings → Battery → Battery Optimization
   - Find COSMIC Connect and disable

2. **Background Data**
   - Ensure background data is enabled
   - Go to Settings → Data Usage
   - Allow background data for COSMIC Connect

3. **WiFi Sleep Settings**
   - Go to Settings → WiFi → Advanced
   - Set "Keep WiFi on during sleep" to "Always"

4. **Network Stability**
   - Check WiFi signal strength
   - Move closer to router
   - Reduce network congestion

5. **Power Saving Mode**
   - Disable power saving mode
   - Or whitelist COSMIC Connect
   - Aggressive battery savers can kill connections

### File Transfer Issues

**Problem:** Files fail to transfer or transfer very slowly.

**Solutions:**

1. **Check Storage Space**
   - Ensure enough storage on receiving device
   - Free up space if needed
   - Check Downloads folder on Android

2. **Network Speed**
   - WiFi speed affects transfer rate
   - For large files, use 5GHz WiFi if available
   - Avoid network congestion

3. **Firewall/Ports**
   - Ports 1764+ needed for transfers
   - Check firewall allows these ports
   - Temporarily disable firewall to test

4. **File Size**
   - Very large files (>1 GB) may timeout
   - Try breaking into smaller pieces
   - Use alternative transfer methods for huge files

5. **Retry Transfer**
   - Cancel and retry the transfer
   - Restart both apps
   - Check file isn't corrupted

### Notifications Not Working

**Problem:** Phone notifications don't appear on desktop.

**Solutions:**

1. **Permission Granted**
   - Go to Settings → Apps → COSMIC Connect
   - Check "Notification Access" is enabled
   - Grant permission if not

2. **Plugin Enabled**
   - In COSMIC Connect settings
   - Check Notifications plugin is enabled
   - Enable for specific device

3. **App Selection**
   - Choose which apps to mirror
   - Go to Notification settings
   - Enable specific apps

4. **Desktop Settings**
   - Check desktop notifications are enabled
   - Desktop notification settings
   - Ensure not in Do Not Disturb mode

5. **Re-enable Permission**
   - Disable notification access
   - Re-enable notification access
   - Restart app

### Clipboard Sync Not Working

**Problem:** Copied text doesn't sync between devices.

**Solutions:**

1. **Plugin Enabled**
   - Check Clipboard plugin is enabled
   - Enable for connected device
   - Verify in device settings

2. **Text Content**
   - Only plain text and URLs sync
   - Images don't sync (use file sharing)
   - Very long text may not sync

3. **Timing**
   - Sync may take 1-2 seconds
   - Wait a moment before pasting
   - Check connection is active

4. **Restart Connection**
   - Disconnect and reconnect devices
   - Disable and re-enable Clipboard plugin
   - Restart both apps

### Battery Drain

**Problem:** COSMIC Connect uses too much battery.

**Solutions:**

1. **Background Behavior**
   - Adjust background sync frequency
   - Go to Advanced Settings
   - Reduce polling intervals

2. **Disable Unused Plugins**
   - Disable plugins you don't use
   - Each plugin uses resources
   - Keep only needed ones enabled

3. **Discovery**
   - Disable continuous discovery
   - Only refresh when needed
   - Discovery uses significant power

4. **Location Services**
   - Location permission needed but not actively used
   - Android may show high location usage
   - This is normal for network discovery

5. **Connection Method**
   - Bluetooth uses less battery than WiFi scanning
   - Consider Bluetooth if available
   - Turn off when not actively using

---

## Privacy & Security

### What Data Does COSMIC Connect Collect?

**Short Answer:** Nothing! All communication is local.

**Details:**
- ✅ **No Cloud Services:** Everything stays on your local network
- ✅ **No Analytics:** We don't track your usage
- ✅ **No Ads:** Completely ad-free
- ✅ **No Third-Party Services:** No external connections
- ✅ **Open Source:** Code is public, auditable

### What Data is Shared Between Devices?

**Only what you enable:**
- Files you explicitly choose to share
- Clipboard content (if enabled)
- Notifications (if enabled)
- SMS messages (if enabled)
- Battery status (if enabled)
- Media playback info (if enabled)

**Never shared:**
- Your location (permission only used for WiFi scanning)
- Your contacts (unless explicitly shared)
- Your photos (unless explicitly shared)
- Your browsing history
- Any data from disabled plugins

### How is Communication Secured?

**Encryption:**
- All communication uses TLS 1.2+
- 2048-bit RSA keys
- Certificate pinning prevents man-in-the-middle attacks
- Same security level as online banking

**Pairing Security:**
- Initial pairing requires physical access to both devices
- You must accept pairing on both sides
- Certificates are exchanged securely
- Unpair anytime to revoke access

**Network Security:**
- Works on local network only (no internet exposure)
- Firewall-friendly (uses standard ports)
- No open ports to the internet
- Devices must be on same network

### Permissions Explained

**Why We Need Each Permission:**

**Location** (Required):
- Android requires this for WiFi device discovery
- We only use it to scan for nearby devices
- We never access your GPS location
- Can be revoked, but discovery won't work

**Notifications** (Optional):
- To read and mirror notifications to desktop
- Only used if you enable notification plugin
- You choose which apps to mirror

**SMS/Phone** (Optional):
- To send/receive SMS from desktop
- To show incoming call notifications
- Only if you enable SMS plugin

**Storage** (Optional):
- To read files you choose to share
- To save received files
- Only accesses files you interact with

**Contacts** (Optional):
- To show contact names with notifications
- To help you select SMS recipients
- Only if you enable relevant plugins

### Privacy Best Practices

**Recommendations:**
1. Only pair with devices you own/trust
2. Unpair devices when no longer needed
3. Keep COSMIC Connect updated
4. Use strong WiFi passwords
5. Review which plugins are enabled
6. Only enable features you need

**Secure Usage:**
- Don't pair on public WiFi networks
- Keep your phone locked with PIN/biometric
- Review paired devices regularly
- Unpair lost or sold devices immediately

---

## Getting Help

### Documentation

- **User Guide:** This document
- **FAQ:** docs/FAQ.md
- **Troubleshooting:** See above section
- **Developer Docs:** docs/INDEX.md

### Community Support

**GitHub:**
- **Issues:** [Report bugs](https://github.com/olafkfreund/cosmic-connect-android/issues)
- **Discussions:** [Ask questions](https://github.com/olafkfreund/cosmic-connect-android/discussions)

**Reddit:**
- r/pop_os - Pop!_OS and COSMIC Desktop community
- r/CosmicDE - COSMIC Desktop discussion

**Matrix/Discord:**
- [Links available on GitHub]

### Reporting Bugs

When reporting a bug, please include:
1. Android version
2. Device model
3. COSMIC Desktop version
4. Steps to reproduce
5. Expected behavior
6. Actual behavior
7. Logs (if available)

**Enable Debug Logging:**
1. Go to Settings → Advanced
2. Enable "Debug Logging"
3. Reproduce the issue
4. Export logs
5. Attach to bug report

### Feature Requests

Have an idea for a new feature?
1. Check existing issues first
2. Create a new issue on GitHub
3. Describe the feature clearly
4. Explain the use case
5. Be open to discussion

### Contributing

COSMIC Connect is open source! Contributions welcome:
- Code contributions
- Documentation improvements
- Translations
- Bug reports
- Feature ideas

See CONTRIBUTING.md for guidelines.

---

## Advanced Topics

### Custom Commands

**Creating Powerful Commands:**

On your COSMIC Desktop, you can create custom commands that appear on your Android device:

**Example: Lock Screen**
```bash
loginctl lock-session
```

**Example: Take Screenshot**
```bash
gnome-screenshot -f ~/Pictures/screenshot.png
```

**Example: Shutdown with Delay**
```bash
shutdown -h +5
```

Add these in the Run Command plugin settings on your desktop.

### Network Configuration

**Port Usage:**
- **1716:** UDP broadcast for discovery
- **1764-1764+N:** TCP for data transfer (N = number of connections)

**Firewall Rules:**
Allow incoming on port 1716 (UDP) and 1764+ (TCP).

**Multiple Devices:**
Each connection uses a new port starting from 1764.

### Developer Mode

**Enable Developer Options:**
1. Go to Settings → About
2. Tap version number 7 times
3. Developer options appear in Settings

**Features:**
- View raw network packets
- Export detailed logs
- Performance metrics
- Connection statistics

---

## Tips & Tricks

### Productivity Tips

1. **Quick File Sharing**
   - Use share menu from any app
   - Long-press files to share multiple

2. **Keyboard Shortcuts**
   - Use remote keyboard for quick text entry
   - Great for filling forms on desktop

3. **Media Remote**
   - Control desktop media from anywhere in your home
   - Perfect for presentations or home media center

4. **Find Phone Regularly**
   - Set up a command to ring phone from desktop
   - Save time searching for misplaced phone

### Battery Saving

1. **Disable Unused Plugins**
   - Only enable what you need
   - Each plugin uses resources

2. **Manual Discovery**
   - Don't leave discovery running
   - Refresh only when needed

3. **Connection Management**
   - Disconnect when not in use
   - Reconnect when needed

### Best Practices

1. **Keep Both Apps Updated**
   - Update Android app from Play Store/F-Droid
   - Update desktop applet regularly

2. **Secure Network**
   - Use WPA2/WPA3 WiFi encryption
   - Don't pair on public networks

3. **Regular Maintenance**
   - Review paired devices monthly
   - Clear old/unused pairings
   - Check for updates

---

## Frequently Asked Questions

For more FAQs, see [FAQ.md](FAQ.md)

**Q: Is COSMIC Connect free?**
A: Yes! Completely free and open source (GPL-3.0).

**Q: Does it work without internet?**
A: Yes! Works on local network only. No internet required.

**Q: Can I use it with other Linux desktops?**
A: COSMIC Connect is designed for COSMIC Desktop, but is compatible with the KDE Connect protocol.

**Q: Is my data private?**
A: Yes! All data stays local on your network. No cloud services.

**Q: Does it drain battery?**
A: Minimal battery usage with proper configuration. See Battery Saving tips.

---

## Changelog

See [CHANGELOG.md](../CHANGELOG.md) for version history and updates.

---

## Credits

COSMIC Connect is based on [KDE Connect Android](https://community.kde.org/KDEConnect), modernized for COSMIC Desktop.

**Thanks to:**
- KDE Connect team for the original implementation
- COSMIC Desktop team at System76
- Open source community contributors

---

## License

GPL-3.0 License. See [LICENSE](../LICENSE) for details.

---

**Need more help?** Check out our [FAQ](FAQ.md) or [ask on GitHub](https://github.com/olafkfreund/cosmic-connect-android/discussions)!

**Enjoy using COSMIC Connect!** 🚀
