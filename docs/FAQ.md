# COSMIC Connect Android - Frequently Asked Questions (FAQ)

**Quick answers to common questions about COSMIC Connect.**

**Last Updated:** January 27, 2026

---

## Table of Contents

- [General Questions](#general-questions)
- [Installation & Setup](#installation--setup)
- [Pairing & Connection](#pairing--connection)
- [Features](#features)
- [Privacy & Security](#privacy--security)
- [Troubleshooting](#troubleshooting)
- [Technical Questions](#technical-questions)
- [Compatibility](#compatibility)

---

## General Questions

### What is COSMIC Connect?

COSMIC Connect is an Android app that connects your phone with your COSMIC Desktop computer, enabling file sharing, clipboard sync, notification mirroring, and more.

### Is COSMIC Connect free?

Yes! COSMIC Connect is completely free and open source (GPL-3.0 license). No ads, no premium features, no subscriptions.

### Is it the same as KDE Connect?

COSMIC Connect is based on the KDE Connect protocol (v8) but is a **complete modernization** specifically for COSMIC Desktop.
- **Modern UI:** Built entirely with **Jetpack Compose** and **Material Design 3**.
- **Architecture:** Uses a **hybrid Rust + Kotlin** core for shared logic and reliability.
- **Experience:** Designed to feel native to the COSMIC ecosystem.

### Does COSMIC Connect require internet?

No! COSMIC Connect works entirely on your local WiFi network. No internet connection required. All communication is direct between your devices.

### Does it use cloud services?

No. All communication is peer-to-peer between your devices. Nothing is sent to any cloud service or external server. Your data stays private and local.

### What's the difference between COSMIC Connect and other remote access apps?

COSMIC Connect is designed for seamless integration between Android and COSMIC Desktop, not remote desktop access. It's for quick tasks like sharing files, syncing clipboard, seeing notifications - not controlling your computer remotely (though it has limited remote input capabilities).

### Can I use it on multiple phones?

Yes! You can pair multiple Android devices with the same COSMIC Desktop computer. Each device pairs independently.

### Can I pair with multiple computers?

Yes! Your Android device can pair with multiple COSMIC Desktop computers. You'll see all paired computers in the device list.

---

## Installation & Setup

### Where can I download COSMIC Connect?

**Coming Soon:**
- Google Play Store
- F-Droid
- GitHub Releases (direct APK)

### Which APK should I download?

- **Universal APK:** Works on all devices (larger file size)
- **ARM64:** Modern phones (2018+) - Recommended
- **ARMv7:** Older phones (pre-2018)
- **x86_64:** Emulators and x86 tablets

**If unsure, use the Universal APK.**

### What Android version do I need?

Android 6.0 (Marshmallow) or later. Android 14+ recommended for best performance.

### How much storage does it need?

About 50 MB for the app itself. Additional space needed if you receive files.

### Do I need to install anything on my desktop?

Yes! You need `cosmic-applet-kdeconnect` installed on your COSMIC Desktop. This is included with COSMIC Desktop or can be installed separately.

### Why does it ask for so many permissions?

Each permission enables specific features:
- **Location:** For WiFi device discovery (Android requirement)
- **Notifications:** To mirror phone notifications
- **SMS/Phone:** To send/receive texts from desktop
- **Storage:** To share and receive files
- **Contacts:** To show contact names

You can skip optional permissions if you don't need those features.

### Can I use it without granting all permissions?

Yes! Only location permission is required for basic functionality. Other permissions are optional and only needed for specific features.

---

## Pairing & Connection

### How do I pair my devices?

1. Ensure both devices are on the same WiFi network
2. Open COSMIC Connect on Android
3. Tap refresh to discover your computer
4. Tap your computer's name
5. Tap "Pair"
6. Accept pairing request on desktop

See [User Guide - Pairing](USER_GUIDE.md#pairing-your-devices) for detailed steps.

### Why can't I find my computer?

Common causes:
- Devices on different WiFi networks
- cosmic-applet-kdeconnect not running on desktop
- Firewall blocking port 1716
- WiFi not enabled on one device

See [Troubleshooting - Device Not Found](USER_GUIDE.md#device-not-found).

### Do I need to pair every time?

No! Once paired, devices remember each other. They automatically reconnect when both are on the same network.

### How do I unpair a device?

On Android:
1. Go to Settings
2. Find the paired device
3. Tap "Unpair"
4. Confirm

On Desktop:
1. Open COSMIC Connect settings
2. Find your phone
3. Click "Unpair"

### Can I pair over Bluetooth?

Yes! Bluetooth pairing is supported as an alternative to WiFi. Both devices need Bluetooth enabled.

### Why does pairing fail?

Common causes:
- Incorrect time/date on either device
- Network instability
- Firewall interference
- Old pairing data lingering

Try:
- Remove old pairings
- Restart both apps
- Check both devices have correct time
- Temporarily disable firewalls

### How secure is the pairing process?

Very secure! Pairing requires:
- Physical access to both devices
- Manual acceptance on both sides
- TLS encryption with certificate pinning
- 2048-bit RSA key exchange

### Can someone else pair with my phone?

No. Pairing requires you to accept the request on both devices. Nobody can pair without physical access to both devices at the same time.

---

## Features

### What can COSMIC Connect do?

**Main Features:**
- 📁 File & URL sharing
- 📋 Clipboard sync
- 🔔 Notification mirroring
- 💬 SMS from desktop
- 🎵 Media control
- 🔋 Battery monitoring
- 📍 Find my phone
- ⚡ Run commands
- 🎮 Remote input (touchpad/keyboard)

### How do I share a file?

**From Android:**
1. Open any app with files
2. Select the file
3. Tap Share button
4. Choose COSMIC Connect
5. Select your computer

**From Desktop:**
1. Right-click the file
2. Send to → Your phone name

### Does clipboard sync work automatically?

Yes! Once enabled, any text you copy on one device is automatically available on the other. There may be a 1-2 second delay.

### Can I see phone notifications on my desktop?

Yes! Enable the Notifications plugin and grant Notification Access permission. You can choose which apps to mirror and even reply to messages.

### Can I send SMS from my desktop?

Yes! Enable the SMS plugin and grant Phone/SMS permission. You can send and receive SMS directly from your COSMIC Desktop.

### Can I control music playing on my desktop?

Yes! The MPRIS (Media) plugin lets you control media playback on your desktop from your phone. Play/pause, next/previous, volume control, etc.

### Can I use my phone as a remote for my computer?

Yes! The Remote Input feature lets you use your phone as a touchpad and keyboard. Great for presentations or media center control.

### What's "Find My Phone"?

It makes your phone ring at maximum volume, even if it's on silent. Perfect for when you've misplaced your phone nearby.

### Can I run commands on my desktop from my phone?

Yes! The Run Command plugin lets you execute predefined commands. You configure commands on desktop, then trigger them from your phone.

### What file types can be shared?

All file types! Photos, videos, documents, APKs, audio - anything. No file type restrictions.

### Is there a file size limit?

No hard limit, but very large files (>1 GB) may be slow depending on your WiFi speed. Progress bar shows transfer status.

### Can I share multiple files at once?

Yes! Select multiple files in your file manager and share them all at once.

### Where do received files go?

On Android: **Downloads** folder
On Desktop: **Downloads** folder (configurable)

You can change the save location in settings.

---

## Privacy & Security

### Is my data encrypted?

Yes! All communication uses TLS 1.2+ encryption with 2048-bit RSA keys. Same security level as online banking.

### What data does COSMIC Connect collect?

None! We don't collect any data. Everything stays local between your devices. No analytics, no tracking, no cloud services.

### Can you see my messages or files?

No. All communication is direct between your devices. Nothing passes through our servers because there are no servers - it's all peer-to-peer.

### Why does it need location permission?

Android requires location permission for apps that discover nearby devices via WiFi. We only use it for WiFi scanning, never for GPS location tracking.

We understand this seems strange, but it's an Android requirement, not our choice.

### What data is shared between devices?

Only what you enable:
- Files you explicitly share
- Clipboard content (if enabled)
- Notifications you choose to mirror
- Battery status (if enabled)
- etc.

Each feature is opt-in. Disabled plugins don't share any data.

### Can someone intercept my data?

Extremely difficult. Communication is encrypted with TLS and certificate pinning. An attacker would need to break strong encryption or perform a man-in-the-middle attack during initial pairing (which requires physical access to both devices).

### Is it safe to use on public WiFi?

We recommend against pairing on public WiFi. While communication is encrypted, initial pairing on untrusted networks is not recommended. Pair on your home network, then you can use the established connection elsewhere.

### What happens if I lose my phone?

Unpair the phone from your desktop immediately:
1. Open COSMIC Connect on desktop
2. Find your phone in the list
3. Click "Unpair"

This revokes the phone's access to your desktop.

### Can I review paired devices?

Yes! In Settings, you can see all paired devices and unpair any you don't recognize or no longer use.

---

## Troubleshooting

### Connection keeps dropping

**Causes:**
- Battery optimization killing the app
- Background data restricted
- WiFi sleep enabled
- Weak WiFi signal

**Solutions:**
- Disable battery optimization for COSMIC Connect
- Allow background data
- Set WiFi to "Always on during sleep"
- Move closer to router

See [Troubleshooting Guide](USER_GUIDE.md#connection-drops).

### Files transfer slowly

**Causes:**
- Slow WiFi network
- Network congestion
- Using 2.4 GHz WiFi instead of 5 GHz

**Solutions:**
- Use 5 GHz WiFi if available
- Move closer to router
- Reduce network congestion
- Check router performance

### Clipboard sync doesn't work

**Possible causes:**
- Clipboard plugin not enabled
- Text too long (>100 KB)
- Trying to sync images (not supported)

**Solutions:**
- Enable Clipboard plugin in settings
- For images, use file sharing instead
- Check connection is active

### Notifications not appearing on desktop

**Causes:**
- Notification Access permission not granted
- Notifications plugin disabled
- Desktop in Do Not Disturb mode
- App not selected for mirroring

**Solutions:**
- Grant Notification Access in Android settings
- Enable Notifications plugin
- Check desktop notification settings
- Configure which apps to mirror

### Battery drains quickly

**Causes:**
- Continuous device discovery enabled
- Too many plugins enabled
- Background sync too frequent

**Solutions:**
- Disable continuous discovery
- Disable unused plugins
- Adjust sync frequency
- Disconnect when not actively using

For more troubleshooting, see [User Guide - Troubleshooting](USER_GUIDE.md#troubleshooting).

---

## Technical Questions

### What ports does COSMIC Connect use?

- **Port 1716 (UDP):** Device discovery
- **Port 1764+ (TCP):** Data transfer

Firewall must allow these ports.

### What protocol does it use?

KDE Connect protocol version 8. This is an open, well-documented protocol used by KDE Connect and compatible apps.

### Is the protocol compatible with KDE Connect?

Yes! COSMIC Connect uses the same protocol (v8) as KDE Connect, so it can communicate with KDE Connect installations.

### Can I use COSMIC Connect with KDE Plasma?

While COSMIC Connect is designed for COSMIC Desktop, it should work with KDE Plasma since they use the same protocol. However, the desktop side would need KDE Connect, not cosmic-applet-cconnect.

### What's the difference between cosmic-applet-kdeconnect and KDE Connect?

`cosmic-applet-kdeconnect` is a COSMIC Desktop applet implementation of the KDE Connect protocol. It's designed specifically for COSMIC Desktop's architecture and UI patterns.

### How is it different architecturally from KDE Connect Android?

COSMIC Connect uses a modern **hybrid Rust + Kotlin architecture**:
- **Rust core:** Protocol, networking, crypto (shared with desktop)
- **Kotlin/Compose:** UI and Android integration
- **Benefits:** Better code sharing, memory safety, modern UI

### Does it work offline?

Yes! No internet required. Works on local WiFi network only.

### Can it work without WiFi?

Yes, via Bluetooth. Both devices need Bluetooth enabled. Bluetooth is slower than WiFi but works when WiFi isn't available.

### What's the maximum range?

WiFi: Limited by your router's range (typically 50-100m indoors)
Bluetooth: About 10m

### Does it support IPv6?

Yes! Both IPv4 and IPv6 are supported for device discovery and communication.

### Can I use a VPN?

Local VPNs work fine. Internet VPNs may interfere with local device discovery. Disable VPN temporarily if you have connection issues.

### Is the source code available?

Yes! Fully open source under GPL-3.0:
- Android app: github.com/olafkfreund/cosmic-connect-android
- Rust core: github.com/olafkfreund/cosmic-connect-core
- Desktop applet: github.com/olafkfreund/cosmic-applet-kdeconnect

### Can I contribute?

Absolutely! Contributions welcome:
- Code improvements
- Bug reports
- Feature requests
- Documentation
- Translations

See CONTRIBUTING.md for guidelines.

---

## Compatibility

### What Android versions are supported?

- **Minimum:** Android 6.0 (Marshmallow)
- **Recommended:** Android 14+
- **Tested:** Android 6-15

### What devices are supported?

Any Android phone or tablet running Android 6.0+. Tested on:
- Google Pixel
- Samsung Galaxy
- OnePlus
- Xiaomi
- And many more

### Does it work on Android tablets?

Yes! Full tablet support with optimized layouts for larger screens.

### What about Chromebooks?

Chromebooks that support Android apps should work. Limited testing on Chromebooks at this time.

### Does it work on Fire tablets?

Should work on Fire tablets with Google Play Services installed. Not officially tested.

### Can I use it on a desktop that's not COSMIC?

COSMIC Connect is designed for COSMIC Desktop, but since it uses the KDE Connect protocol, it could theoretically work with KDE Connect on other desktops. However, you'd need to install KDE Connect on that desktop, not cosmic-applet-cconnect.

### Does it work with Pop!_OS?

Yes! COSMIC Desktop is included with Pop!_OS 24.04+. Perfect compatibility.

### What Linux distributions are supported?

Any Linux distribution that can run COSMIC Desktop and cosmic-applet-kdeconnect:
- Pop!_OS 24.04+
- Fedora (with COSMIC)
- Arch Linux (with COSMIC)
- Any distro with COSMIC Desktop installed

### Does it work on macOS or Windows?

No. This is specifically for COSMIC Desktop on Linux. For Windows/macOS, you'd need KDE Connect desktop versions.

### Can I use it with my iPhone?

No. This is an Android app. For iOS, there's no direct equivalent, though KDE Connect has some iOS projects in development.

---

## Performance & Battery

### How much battery does it use?

Minimal when properly configured:
- **Idle:** <1% per hour
- **Active use:** 2-5% per hour
- **Continuous file transfer:** Higher usage is normal

Tips to reduce battery use:
- Disable unused plugins
- Don't leave discovery running
- Disconnect when not in use

### Does it slow down my phone?

No. COSMIC Connect uses minimal CPU and memory when idle. Active features like file transfer will use more resources temporarily, but this is normal.

### Will it drain my desktop battery?

Laptop battery usage is minimal. Desktop performance impact is negligible.

### How much data does it use?

No cellular data! All communication is over WiFi (or Bluetooth). Your mobile data is never used.

---

## Updates & Support

### How do I update COSMIC Connect?

**Google Play / F-Droid:** Updates automatically or manually through the app store.
**Direct APK:** Download new version from GitHub Releases and install over existing.

### How often is it updated?

Active development with regular updates:
- Bug fixes: As needed
- Features: Monthly (planned)
- Security updates: Immediate

### Where can I report bugs?

GitHub Issues: github.com/olafkfreund/cosmic-connect-android/issues

Please include:
- Android version
- Device model
- COSMIC Desktop version
- Steps to reproduce

### Where can I request features?

GitHub Issues (use "Feature Request" template) or GitHub Discussions.

### How can I get help?

1. **Documentation:** Check User Guide and FAQ (this document)
2. **GitHub Discussions:** Ask questions
3. **GitHub Issues:** Report bugs
4. **Community:** Reddit (/r/pop_os, /r/CosmicDE)

### Is there a Discord or Matrix?

Community channels links available on the GitHub repository.

---

## Miscellaneous

### Why "COSMIC Connect" and not "KDE Connect"?

This is a modernized fork specifically for COSMIC Desktop. While it uses the KDE Connect protocol, it's been rebuilt with a hybrid Rust + Kotlin architecture and Material Design 3 UI specifically for COSMIC Desktop integration.

### Will KDE Connect Android work with COSMIC Desktop?

Theoretically yes, since they use the same protocol. But COSMIC Connect is optimized for COSMIC Desktop and offers a better, more modern experience.

### Can I use both KDE Connect and COSMIC Connect?

Not recommended. They would conflict since they use the same ports and protocol. Choose one.

### What's the roadmap?

See PROJECT_PLAN.md and GitHub milestones for upcoming features:
- Additional plugins
- Performance optimizations
- UI enhancements
- More COSMIC Desktop integration

### How can I donate or support the project?

COSMIC Connect is free and open source. The best support is:
- Star the GitHub repository
- Report bugs
- Contribute code or documentation
- Spread the word
- Help other users

### What license is it under?

GPL-3.0, same as KDE Connect. See LICENSE file.

### Who develops COSMIC Connect?

Open source project with contributions from the community. Lead developer and contributors listed on GitHub.

### What's the relationship with System76?

COSMIC Connect is an independent project built for COSMIC Desktop (developed by System76), but is not officially developed by System76.

---

## Still Have Questions?

- **User Guide:** [USER_GUIDE.md](USER_GUIDE.md)
- **GitHub Discussions:** Ask the community
- **GitHub Issues:** Report bugs
- **Reddit:** /r/pop_os, /r/CosmicDE

---

**Last Updated:** January 27, 2026
**Version:** 1.0.0-beta

For more detailed information, see the [User Guide](USER_GUIDE.md).