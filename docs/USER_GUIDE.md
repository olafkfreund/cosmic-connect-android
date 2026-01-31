# COSMIC Connect Android - User Guide

**Welcome to COSMIC Connect!** This guide will help you set up and use COSMIC Connect to seamlessly connect your Android device with your COSMIC Desktop computer.

**Version:** 1.0.0-beta
**Last Updated:** January 27, 2026

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

### 📷 Camera Webcam (NEW!)
Use your phone's camera as a wireless webcam for video calls and streaming.

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

---

## Initial Setup

### First Launch

When you first open COSMIC Connect, you'll be greeted by the **Device List** screen. You may see a prompt to grant permissions necessary for the app to function.

#### Permissions Explained

1.  **Location (Required):**
    *   **Why:** Android requires this permission to scan for devices on your local WiFi network.
    *   **Privacy:** We **never** access your GPS location. This is strictly a technical requirement for WiFi scanning on modern Android versions.

2.  **Notification Access (Optional):**
    *   **Why:** To sync your phone's notifications to your desktop.
    *   **Action:** You'll be guided to Android Settings to enable this for "COSMIC Connect".

3.  **Storage / Files (Optional):**
    *   **Why:** To send files from your phone and save files sent from your desktop.

4.  **Battery Optimization (Recommended):**
    *   **Why:** To keep the connection alive when your phone screen is off.
    *   **Action:** Allow the app to run in the background for reliable connectivity.

---

## Pairing Your Devices

### Step 1: Ensure Connectivity
*   **On Desktop:** Ensure COSMIC Connect (or cosmic-applet-kdeconnect) is running.
*   **On Android:** Ensure WiFi is on and connected to the same network as your desktop.

### Step 2: Discover Devices
1.  Open the **COSMIC Connect** app on Android.
2.  The **Discovery Screen** automatically scans for devices.
3.  Tap the **Refresh** icon (top right) if your device doesn't appear immediately.
4.  Your computer should appear in the list under "Available Devices".

### Step 3: Initiate Pairing
1.  **Tap the device card** for your computer.
2.  You will see the **Unpaired Device** screen.
3.  Tap the large **Request Pairing** button.
4.  A notification will appear on your desktop asking to accept the pairing request.
5.  **Click Accept** on your desktop.

### Step 4: Pairing Complete
*   The screen on your phone will update to show the **Paired Device** view.
*   You will see a list of available plugins (Battery, Clipboard, etc.).
*   The status indicator will turn green and say "Connected".

---

## Using Features

### 📁 Sharing Files

**From Android to Desktop:**
1.  Open any app (Gallery, Files, etc.).
2.  Select a file and tap **Share**.
3.  Choose **COSMIC Connect** from the share sheet.
4.  Tap your desktop computer's name.
5.  The file will be sent immediately.

**From Desktop to Android:**
1.  Right-click a file on your desktop.
2.  Select **Send to** → **[Your Phone]**.
3.  The file will appear in your phone's **Downloads** folder.

### 📋 Clipboard Sync

*   **Automatic:** Any text you copy on one device is automatically available to paste on the other.
*   **Privacy:** This feature can be disabled in the device settings if you prefer manual sharing.

### 🔔 Notification Mirroring

*   **View:** Notifications from your phone appear as desktop notifications.
*   **Reply:** Click "Reply" on a desktop notification (e.g., WhatsApp, SMS) to send a response directly from your computer.
*   **Dismiss:** Dismissing a notification on the desktop removes it from your phone.

### 🎵 Media Control

1.  Tap your paired device in the COSMIC Connect app.
2.  If media is playing on your desktop, a **Media Control** card will appear.
3.  You can Play/Pause, Skip Tracks, and adjust volume remotely.

### ⚡ Run Commands

1.  Tap your paired device.
2.  Scroll down to the **Run Command** plugin card.
3.  Tap the card to open the command list.
4.  Tap any command (e.g., "Lock Screen", "Take Screenshot") to execute it instantly on your desktop.

*Note: Commands must first be configured in the COSMIC Connect settings on your desktop.*

### 🎮 Remote Input

1.  Tap the **Remote Input** plugin card.
2.  **Touchpad:** Use your phone screen as a trackpad.
    *   Single tap: Left click
    *   Two-finger tap: Right click
    *   Two-finger slide: Scroll
3.  **Keyboard:** Tap the keyboard icon (top right) to type on your desktop using your phone's keyboard.

### 📷 Camera Webcam

Use your phone's high-quality camera as a wireless webcam for video calls.

**Prerequisites:**
*   Your desktop needs the v4l2loopback kernel module installed and loaded
*   See the [Camera Webcam Guide](CAMERA_WEBCAM.md) for desktop setup instructions

**Starting Camera Streaming:**
1.  Tap your paired device in the COSMIC Connect app
2.  Scroll to the **Camera** plugin card
3.  Tap **Start Camera**
4.  Your phone camera will begin streaming to your desktop

**Using in Video Calls:**
1.  In your video application (Zoom, Teams, Meet, etc.), go to camera settings
2.  Select **"COSMIC Connect Camera"** from the camera list
3.  Your phone camera feed should now appear

**Switching Cameras:**
*   Tap the camera flip icon to switch between front and back cameras

**Stopping Streaming:**
*   Tap **Stop Camera** in the app, or use the notification to stop

For detailed setup and troubleshooting, see:
*   [Camera Webcam Setup Guide](CAMERA_WEBCAM.md)
*   [Camera Troubleshooting](CAMERA_TROUBLESHOOTING.md)

---

## Settings

To access settings, tap the **Gear Icon** (or Menu) on the main screen.

### App Settings
*   **Device Name:** Change how your phone appears to other devices.
*   **Theme:** Switch between System, Light, or Dark mode.
*   **Trusted Networks:** Configure which WiFi networks allow auto-connection.

### Plugin Settings (Per Device)
1.  Go to the **Device Details** screen.
2.  Tap the **Settings (Gear)** icon next to a specific plugin (or tap the plugin card itself).
3.  Here you can:
    *   Enable/Disable the plugin.
    *   Configure specific options (e.g., which apps to sync notifications for).

---

## Troubleshooting

### Device Not Found
*   **Check Network:** Are both devices on the *exact* same WiFi network? (Sometimes 2.4GHz and 5GHz are isolated).
*   **Firewall:** Ensure port **1716 (UDP/TCP)** is allowed on your desktop firewall.
*   **VPN:** Disable any VPNs on your phone or desktop, as they can block local discovery.

### Connection Drops
*   **Battery Optimization:** Ensure COSMIC Connect is "Unrestricted" in your phone's battery settings. Android often kills background apps aggressively.
*   **Background Data:** Ensure "Background Data" usage is allowed.

### File Transfer Fails
*   **Permissions:** Ensure the Storage/Files permission is granted.
*   **Ports:** File transfers use ports **1764+**. Ensure these are open on your firewall.

---

## Privacy & Security

*   **End-to-End Encryption:** All data is encrypted using TLS.
*   **Local Only:** No data is ever sent to the cloud. Your devices talk directly to each other.
*   **Permissions:** We only ask for permissions necessary for features you use.

---

## Getting Help

*   **GitHub Issues:** [Report Bugs](https://github.com/olafkfreund/cosmic-connect-android/issues)
*   **Discussions:** [Join the Community](https://github.com/olafkfreund/cosmic-connect-android/discussions)

**Enjoy using COSMIC Connect!** 🚀