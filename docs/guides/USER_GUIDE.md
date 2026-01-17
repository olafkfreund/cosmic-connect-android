# COSMIC Connect User Guide

<div align="center">
  <img src="../../cosmic-connect_logo.png" alt="COSMIC Connect Logo" width="150"/>

  **Connect your Android device with COSMIC Desktop**

  *Last Updated: 2026-01-17*
</div>

---

## Table of Contents

1. [Welcome](#welcome)
2. [Getting Started](#getting-started)
3. [Features Overview](#features-overview)
4. [Pairing Your Devices](#pairing-your-devices)
5. [Using Features](#using-features)
6. [Managing Devices](#managing-devices)
7. [Troubleshooting](#troubleshooting)
8. [Privacy & Security](#privacy--security)
9. [FAQs](#faqs)

---

## Welcome

COSMIC Connect allows you to seamlessly connect your Android phone or tablet with your COSMIC Desktop computer. Share files, sync your clipboard, control media playback, receive notifications, and much more - all wirelessly and securely.

### What You Can Do

- **Share files and links** instantly between devices
- **Sync your clipboard** - copy on one device, paste on another
- **View notifications** from your phone on your desktop
- **Control media playback** using your phone as a remote
- **Find your phone** by making it ring
- **Run commands** on your computer from your phone
- **And more!**

All communication is encrypted using TLS for your privacy and security.

---

## Getting Started

### Requirements

**On Your Android Device:**
- Android 6.0 (Marshmallow) or newer
- Wi-Fi or Bluetooth connectivity
- COSMIC Connect app (coming soon to Google Play Store and F-Droid)

**On Your COSMIC Desktop:**
- COSMIC Desktop environment
- COSMIC Connect Desktop App installed
- Same Wi-Fi network as your Android device (or Bluetooth enabled)

### Installation

#### Android App

**Option 1: Google Play Store (Coming Soon)**
1. Open Google Play Store
2. Search for "COSMIC Connect"
3. Tap Install

**Option 2: F-Droid (Coming Soon)**
1. Open F-Droid app
2. Search for "COSMIC Connect"
3. Tap Install

**Option 3: Direct APK Download**
1. Visit [GitHub Releases](https://github.com/olafkfreund/cosmic-connect-android/releases)
2. Download the latest APK file
3. Open the APK on your Android device
4. Allow installation from unknown sources if prompted
5. Tap Install

#### Desktop App

1. On your COSMIC Desktop, open the App Store
2. Search for "COSMIC Connect"
3. Click Install

Or install from terminal:
```bash
# Coming soon - desktop app installation instructions
```

---

## Features Overview

### 📋 Clipboard Sync
Copy text on one device, paste it on another. Works both ways!

**Use cases:**
- Copy a URL from your computer to open on your phone
- Copy an address from your phone to paste into a desktop app
- Share code snippets between devices

### 📁 File & URL Sharing
Send files and web links instantly between devices.

**Use cases:**
- Share photos from your phone to your desktop for editing
- Send a document from your desktop to your phone
- Open a website on your phone from your desktop

### 🔔 Notification Sync
Receive and respond to Android notifications on your desktop.

**Use cases:**
- Read messages without picking up your phone
- Dismiss notifications from your desktop
- Reply to messages directly from COSMIC Desktop

### 🎵 Media Control
Control media playback on one device from the other.

**Use cases:**
- Use your phone as a remote for desktop music player
- Pause/play/skip tracks without switching focus
- See what's currently playing

### 📞 Phone Features
Access phone functionality from your desktop.

**Use cases:**
- See incoming call notifications
- View SMS messages
- Check missed calls

### 🔍 Find My Phone
Make your phone ring even if it's on silent mode.

**Use cases:**
- Locate misplaced phone in your house
- Find your phone under couch cushions
- Quickly identify which device is yours

### 🖱️ Remote Control
Use your phone as a wireless mouse and keyboard.

**Use cases:**
- Control presentations from across the room
- Navigate media from your couch
- Type on your desktop from a distance

### ⚡ Run Commands
Execute predefined commands on your desktop from your phone.

**Use cases:**
- Lock your computer remotely
- Start a backup job
- Run custom scripts

### 🔋 Battery Monitoring
View your phone's battery status on your desktop.

**Use cases:**
- Monitor battery level without checking your phone
- Get alerts when battery is low
- See charging status

---

## Pairing Your Devices

Pairing connects your Android device with your COSMIC Desktop computer. You only need to do this once per device.

### Step 1: Ensure Same Network

Both devices must be on the same Wi-Fi network or have Bluetooth enabled.

**Check Wi-Fi:**
- On Android: Settings → Wi-Fi → Verify network name
- On COSMIC Desktop: Click network icon → Verify network name

### Step 2: Open COSMIC Connect

**On Android:**
1. Open the COSMIC Connect app
2. The app will automatically search for nearby devices

**On COSMIC Desktop:**
1. Click the COSMIC panel icon
2. Open COSMIC Connect
3. The app will automatically search for nearby devices

### Step 3: Initiate Pairing

**Method 1: From Android**
1. In the COSMIC Connect app, you'll see your desktop listed under "Available Devices"
2. Tap on your desktop name
3. Tap "Request Pairing"
4. A notification will appear on your desktop
5. Click "Accept" on your desktop

**Method 2: From Desktop**
1. In COSMIC Connect on your desktop, you'll see your phone listed
2. Click on your phone name
3. Click "Request Pairing"
4. A notification will appear on your phone
5. Tap "Accept" on your phone

### Step 4: Verification

Once paired, both devices will show:
- A green "Connected" status
- The device will move to "Paired Devices" section
- You can now use all features

### Pairing Notes

**Security:**
- Pairing is protected by TLS encryption
- Both devices must explicitly accept the pairing
- Certificates are exchanged securely
- Connection is verified on both ends

**Troubleshooting:**
- If devices don't appear, check your Wi-Fi connection
- Make sure both apps are open
- Try refreshing the device list
- Restart both apps if needed

---

## Using Features

### Sharing Files

**From Android to Desktop:**
1. Open any app on your phone
2. Tap the Share button
3. Select "COSMIC Connect"
4. Choose your desktop from the list
5. File appears in your desktop's Downloads folder

**From Desktop to Android:**
1. Right-click on a file in your desktop file manager
2. Select "Share via COSMIC Connect"
3. Choose your Android device
4. File appears in your phone's Downloads folder

**Supported file types:** Photos, videos, documents, PDFs, audio files, APKs, and more!

### Sharing URLs

**From Android to Desktop:**
1. Open a web page in your browser
2. Tap the Share button
3. Select "COSMIC Connect"
4. Choose your desktop
5. URL opens in your desktop browser

**From Desktop to Android:**
1. In your desktop browser, right-click on a link or page
2. Select "Share via COSMIC Connect"
3. Choose your Android device
4. URL opens in your phone's browser

### Clipboard Sync

**From Android to Desktop:**
1. Copy text on your Android device (long-press → Copy)
2. Text is automatically synced to your desktop
3. Paste on your desktop (Ctrl+V)

**From Desktop to Android:**
1. Copy text on your desktop (Ctrl+C)
2. Text is automatically synced to your Android device
3. Paste on your Android device (long-press → Paste)

**Note:** Clipboard sync must be enabled in plugin settings for both devices.

### Notification Sync

**Setup:**
1. On Android, go to COSMIC Connect settings
2. Tap "Notification Sync"
3. Grant notification access permission
4. Select which apps can send notifications

**Using:**
- Notifications from selected apps appear on your desktop
- Click on a notification to open the app on your phone
- Dismiss notifications from either device
- Some apps support replying directly from desktop

### Media Control

**Setup:**
1. On Desktop, start playing media (Spotify, YouTube, etc.)
2. On Android, open COSMIC Connect
3. Tap your desktop device
4. Tap "Media Control" plugin

**Controls:**
- Play/Pause button
- Next/Previous track
- Volume control
- See current track info (title, artist, album)

**Supported players:** Spotify, VLC, Audacious, Rhythmbox, YouTube (in browser), and more!

### Find My Phone

**Using Find My Phone:**
1. On Desktop, open COSMIC Connect
2. Click on your phone device
3. Click "Find My Phone" or "Ring"
4. Your phone will ring at maximum volume for 30 seconds
5. Click "Stop" to silence it

**Note:** Works even if your phone is on silent or vibrate mode!

### Remote Control (Mouse & Keyboard)

**Using as Mouse:**
1. On Android, open COSMIC Connect
2. Tap your desktop device
3. Tap "Remote Control"
4. Tap "Mouse Mode"
5. Use your phone's touchscreen as a trackpad
6. Tap to click, two-finger tap to right-click

**Using as Keyboard:**
1. In Remote Control screen
2. Tap "Keyboard Mode"
3. Use your phone's keyboard to type on desktop
4. Useful for entering passwords or long text

**Tips:**
- Two-finger scroll to scroll on desktop
- Pinch to zoom (if supported)
- Swipe with three fingers to switch apps

### Running Commands

**Setup:**
1. On Desktop, open COSMIC Connect settings
2. Go to "Run Command" plugin settings
3. Add commands you want to run:
   - Name: Lock Screen
   - Command: `cosmic-lock`
4. Save commands

**Using:**
1. On Android, open COSMIC Connect
2. Tap your desktop device
3. Tap "Run Command"
4. Select command from list
5. Tap "Run"

**Common commands:**
- Lock screen: `cosmic-lock`
- Sleep: `systemctl suspend`
- Shutdown: `shutdown now`
- Screenshot: `cosmic-screenshot`

### Battery Monitoring

**Viewing Battery:**
1. On Desktop, open COSMIC Connect
2. Click on your phone device
3. Battery level and charging status shown in device info

**Battery icon indicators:**
- 🔋 Green: Battery > 50%
- 🔋 Yellow: Battery 20-50%
- 🔋 Red: Battery < 20%
- ⚡ Lightning bolt: Charging

---

## Managing Devices

### Renaming a Device

**On Android:**
1. Open COSMIC Connect
2. Go to Settings
3. Tap "Device Name"
4. Enter new name
5. Tap "Save"

**On Desktop:**
1. Open COSMIC Connect
2. Right-click on device
3. Select "Rename"
4. Enter new name
5. Click "Save"

### Unpairing a Device

**On Android:**
1. Open COSMIC Connect
2. Tap on the paired device
3. Tap the menu icon (⋮)
4. Select "Unpair"
5. Confirm

**On Desktop:**
1. Open COSMIC Connect
2. Right-click on device
3. Select "Unpair"
4. Confirm

**Note:** You can re-pair the same device later without any issues.

### Managing Plugins

Each feature in COSMIC Connect is a "plugin" that can be enabled or disabled.

**On Android:**
1. Open COSMIC Connect
2. Tap on a paired device
3. You'll see a list of available plugins
4. Toggle plugins on/off as needed

**On Desktop:**
1. Open COSMIC Connect
2. Click on device
3. Click "Plugins" or "Settings"
4. Enable/disable plugins

**Common plugins:**
- Battery: View phone battery on desktop
- Clipboard: Sync clipboard between devices
- Notifications: Show phone notifications on desktop
- Share: Share files and URLs
- MPRIS: Media playback control
- FindMyPhone: Ring your phone
- RunCommand: Execute commands
- RemoteInput: Use phone as mouse/keyboard

### Plugin Settings

Some plugins have additional settings:

**Notification Sync:**
- Select which apps can send notifications
- Privacy settings (show/hide content)
- Notification actions (reply, dismiss)

**Share:**
- Default download location
- Auto-accept file transfers
- File size limits

**Run Command:**
- Add/edit/delete commands
- Command descriptions
- Safety confirmations

---

## Troubleshooting

### Devices Not Appearing

**Problem:** My desktop/phone doesn't appear in the device list.

**Solutions:**
1. **Check network connection:**
   - Ensure both devices are on the same Wi-Fi network
   - Check if Wi-Fi is enabled and connected
   - Try disabling and re-enabling Wi-Fi

2. **Check Bluetooth (if using):**
   - Ensure Bluetooth is enabled on both devices
   - Make devices discoverable
   - Check Bluetooth permissions

3. **Refresh device list:**
   - On Android: Pull down to refresh or tap refresh icon
   - On Desktop: Click refresh button

4. **Restart apps:**
   - Close and reopen COSMIC Connect on both devices

5. **Check firewall:**
   - Ensure COSMIC Connect is allowed through firewall
   - Default ports: UDP 1716 for discovery, TCP 1716 for communication

### Connection Drops

**Problem:** Connection keeps disconnecting or is unreliable.

**Solutions:**
1. **Check network stability:**
   - Ensure strong Wi-Fi signal
   - Move closer to Wi-Fi router
   - Avoid network congestion

2. **Battery optimization:**
   - On Android: Go to Settings → Battery → COSMIC Connect → Don't optimize
   - Prevents Android from killing the background service

3. **Background restrictions:**
   - On Android: Settings → Apps → COSMIC Connect → Remove restrictions

4. **Network changes:**
   - Ensure you're not switching between Wi-Fi and mobile data
   - Keep both devices on the same network

### File Transfer Issues

**Problem:** Files won't transfer or transfer fails.

**Solutions:**
1. **Check storage space:**
   - Ensure enough free space on receiving device
   - Large files need more space

2. **File size:**
   - Very large files (>2GB) may take a long time
   - Try splitting into smaller files

3. **Network stability:**
   - Use stable Wi-Fi connection for large transfers
   - Avoid network interruptions

4. **Permissions:**
   - Grant COSMIC Connect storage permissions
   - On Android: Settings → Apps → COSMIC Connect → Permissions

### Notifications Not Syncing

**Problem:** Phone notifications don't appear on desktop.

**Solutions:**
1. **Check notification permission:**
   - On Android: Settings → Apps → COSMIC Connect → Notifications → Allow
   - Grant notification access in plugin settings

2. **Enable plugin:**
   - Ensure Notification Sync plugin is enabled on both devices

3. **Select apps:**
   - In plugin settings, ensure apps are selected for sync

4. **Check Do Not Disturb:**
   - Disable Do Not Disturb mode on phone

### Clipboard Not Syncing

**Problem:** Clipboard content doesn't sync between devices.

**Solutions:**
1. **Enable plugin:**
   - Ensure Clipboard plugin is enabled on both devices

2. **Grant permissions:**
   - On Android: Grant clipboard access permission

3. **Check network:**
   - Ensure devices are connected and paired

4. **Restart apps:**
   - Close and reopen both apps to refresh connection

### App Crashes or Freezes

**Problem:** COSMIC Connect crashes or becomes unresponsive.

**Solutions:**
1. **Update app:**
   - Check for latest version in Play Store or F-Droid
   - Install updates

2. **Clear cache:**
   - On Android: Settings → Apps → COSMIC Connect → Storage → Clear Cache

3. **Reinstall:**
   - Uninstall and reinstall the app
   - Re-pair devices

4. **Report bug:**
   - Visit GitHub Issues to report the problem
   - Include device model, Android version, and steps to reproduce

---

## Privacy & Security

### Data Security

**Encryption:**
- All communication uses TLS (Transport Layer Security) encryption
- Same encryption used by banks and secure websites
- Data cannot be intercepted or read by third parties

**Certificate Exchange:**
- When pairing, devices exchange cryptographic certificates
- These certificates uniquely identify each device
- Prevents man-in-the-middle attacks

**Local Network:**
- Communication happens on your local network
- Data doesn't go through the internet
- No cloud servers involved

### Permissions

COSMIC Connect requests several Android permissions:

**Storage Access:**
- Needed to send and receive files
- Read from Downloads, write to Downloads
- No access to system files or app data

**Notification Access:**
- Required for Notification Sync plugin
- Only enabled if you activate the plugin
- Only syncs notifications from selected apps

**Network Access:**
- Required for Wi-Fi communication
- Used to discover devices and transfer data
- No internet access required

**Bluetooth:**
- Optional - only if you want to use Bluetooth
- Can disable and use Wi-Fi only

**Contacts (Optional):**
- Only if using Contacts sync feature
- Can be denied if not using this feature

### Privacy Features

**Notification Privacy:**
- Choose which apps can send notifications to desktop
- Hide notification content (show only app name)
- Disable notification sync anytime

**Device Access:**
- Unpair devices you no longer trust
- Devices must be paired before accessing features
- Pairing requires confirmation on both devices

**Local Control:**
- You control what data is shared
- Enable/disable plugins as needed
- Unpair devices anytime

---

## FAQs

### General

**Q: Do I need an internet connection to use COSMIC Connect?**
A: No! COSMIC Connect works entirely on your local Wi-Fi network or via Bluetooth. No internet connection is needed. However, both devices must be on the same local network.

**Q: Does COSMIC Connect work with other desktop environments?**
A: This version is specifically designed for COSMIC Desktop. However, the original KDE Connect works with KDE Plasma, GNOME, Windows, and macOS. COSMIC Connect uses the same protocol, so it's compatible with KDE Connect devices.

**Q: Can I use COSMIC Connect with multiple devices?**
A: Yes! You can pair your phone with multiple desktops, or pair multiple phones with one desktop. Each pairing is independent.

**Q: Is COSMIC Connect free?**
A: Yes! COSMIC Connect is completely free and open-source software. No ads, no subscriptions, no in-app purchases.

**Q: Does COSMIC Connect use my mobile data?**
A: No. COSMIC Connect only uses Wi-Fi or Bluetooth for local connections. It never uses mobile data.

### Features

**Q: Can I send files larger than 2GB?**
A: Yes, but large files may take a long time to transfer depending on your Wi-Fi speed. The file transfer will continue in the background.

**Q: Can I reply to messages from my desktop?**
A: This depends on the messaging app. Some apps support reply actions through notifications, which COSMIC Connect can forward. Most SMS apps support this.

**Q: Does clipboard sync work with images?**
A: Currently, clipboard sync only works with text. Image clipboard sync is planned for a future update.

**Q: Can I control my desktop's music player from my phone?**
A: Yes! As long as your desktop music player supports MPRIS (most do), you can control playback from your phone.

### Troubleshooting

**Q: Why can't my devices see each other?**
A: Most common reasons:
1. Not on the same Wi-Fi network
2. Firewall blocking ports 1716
3. App not running on one device
4. Router blocking device-to-device communication

**Q: Why do my devices keep disconnecting?**
A: Check:
1. Battery optimization settings on Android
2. Background app restrictions
3. Wi-Fi power saving mode
4. Network stability

**Q: Can I use COSMIC Connect over the internet?**
A: Not directly. COSMIC Connect is designed for local networks. However, you can set up a VPN to connect your devices remotely, though this is advanced.

### Security & Privacy

**Q: Is COSMIC Connect secure?**
A: Yes! All communication is encrypted with TLS. The same encryption technology used by banks and secure websites.

**Q: Can someone on my Wi-Fi network intercept my data?**
A: No. Even if someone is on your Wi-Fi network, they cannot decrypt the TLS-encrypted communication between your devices.

**Q: Does COSMIC Connect collect my data?**
A: No. COSMIC Connect does not collect, store, or transmit any data to servers. All communication is directly between your devices on your local network.

**Q: Can COSMIC Connect access my passwords or sensitive data?**
A: No. COSMIC Connect only has access to what you explicitly share (files, clipboard, notifications from selected apps). It cannot access passwords, system files, or app data.

### Getting Help

**Q: Where can I get help if something doesn't work?**
A: Several options:
1. Check this User Guide's Troubleshooting section
2. Visit the FAQ section (you're here!)
3. Check [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
4. Read the documentation at [docs/INDEX.md](../INDEX.md)

**Q: How do I report a bug?**
A: Visit [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues) and:
1. Search if the bug is already reported
2. If not, create a new issue
3. Include: Device model, Android version, steps to reproduce, error messages
4. Screenshots are helpful!

**Q: Can I contribute or help improve COSMIC Connect?**
A: Yes! COSMIC Connect is open-source. See [CONTRIBUTING.md](CONTRIBUTING.md) for how to help.

---

## Getting More Help

If you need additional assistance:

- **Documentation**: [docs/INDEX.md](../INDEX.md)
- **GitHub Issues**: [Report bugs or request features](https://github.com/olafkfreund/cosmic-connect-android/issues)
- **COSMIC Desktop**: [COSMIC Desktop Documentation](https://system76.com/cosmic)

---

<div align="center">

**Enjoy seamless connectivity between your Android device and COSMIC Desktop!**

**Made with ❤️ for COSMIC Desktop**

*COSMIC Connect - Open Source, Private, Secure*

</div>
