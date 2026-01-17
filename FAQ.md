# COSMIC Connect - Frequently Asked Questions (FAQ)

<div align="center">

**Quick answers to common questions**

*For detailed information, see the [User Guide](docs/guides/USER_GUIDE.md)*

Last Updated: 2026-01-17

</div>

---

## Quick Links

- 📖 [Full User Guide](docs/guides/USER_GUIDE.md)
- 🚀 [Getting Started](docs/guides/GETTING_STARTED.md)
- 🔧 [Troubleshooting](#troubleshooting)
- 🔒 [Security & Privacy](#security--privacy)

---

## General Questions

### What is COSMIC Connect?

COSMIC Connect allows your Android device to communicate with your COSMIC Desktop computer. You can share files, sync your clipboard, control media playback, receive notifications, and more - all wirelessly and securely.

### Do I need an internet connection?

**No!** COSMIC Connect works entirely on your local Wi-Fi network or Bluetooth. No internet connection needed. However, both devices must be on the same local network.

### Is COSMIC Connect free?

**Yes!** COSMIC Connect is completely free and open-source. No ads, no subscriptions, no in-app purchases, ever.

### What devices are supported?

- **Android**: Version 6.0 (Marshmallow) or newer
- **Desktop**: COSMIC Desktop environment
- **Connection**: Wi-Fi or Bluetooth

### How do I install COSMIC Connect?

**Android:**
- Google Play Store (coming soon)
- F-Droid (coming soon)
- [Direct APK download](https://github.com/olafkfreund/cosmic-connect-android/releases)

**COSMIC Desktop:**
- Install from COSMIC App Store (coming soon)
- See [installation guide](docs/guides/USER_GUIDE.md#installation)

### Can I use it with multiple devices?

**Yes!** You can pair:
- One phone with multiple desktops
- Multiple phones with one desktop
- Any combination - each pairing is independent

---

## Features

### What features are available?

- ✅ **File & URL sharing** - Instant transfer between devices
- ✅ **Clipboard sync** - Copy on one device, paste on another
- ✅ **Notification sync** - See phone notifications on desktop
- ✅ **Media control** - Remote control for music/video players
- ✅ **Find My Phone** - Make your phone ring
- ✅ **Battery monitoring** - View phone battery on desktop
- ✅ **Remote control** - Use phone as mouse/keyboard
- ✅ **Run commands** - Execute desktop commands from phone
- ✅ **SMS & calls** - See phone activity on desktop

### Can I send large files?

**Yes!** There's no hard limit, but:
- Large files (>2GB) may take time depending on Wi-Fi speed
- Ensure enough storage space on receiving device
- Transfer continues in background

### Does clipboard sync work with images?

**Currently text only.** Image clipboard sync is planned for future updates.

### Can I reply to messages from desktop?

**Yes, for supported apps.** Some messaging apps support reply actions through notifications, which COSMIC Connect forwards. Most SMS apps support this feature.

### Can I control desktop music from my phone?

**Yes!** Any desktop music player that supports MPRIS (most modern players do) can be controlled from your phone. This includes Spotify, VLC, Rhythmbox, and more.

---

## Pairing & Connection

### How do I pair my devices?

**Quick steps:**
1. Ensure both devices on same Wi-Fi network
2. Open COSMIC Connect on both devices
3. Devices should appear automatically
4. Tap/click device name → "Request Pairing"
5. Accept on other device

See [detailed pairing guide](docs/guides/USER_GUIDE.md#pairing-your-devices) for more help.

### Why can't my devices see each other?

**Common solutions:**
1. ✅ Check both on same Wi-Fi network
2. ✅ Ensure COSMIC Connect is running on both devices
3. ✅ Try refreshing device list
4. ✅ Check firewall allows port 1716
5. ✅ Restart both apps
6. ✅ Check router isn't blocking device-to-device communication

### Do devices stay paired after reboot?

**Yes!** Once paired, devices remember each other. They'll automatically reconnect when both are on the same network.

### How do I unpair a device?

**On Android:**
1. Open COSMIC Connect
2. Tap the paired device
3. Tap menu (⋮) → "Unpair"

**On Desktop:**
1. Right-click device → "Unpair"

You can re-pair later without issues.

---

## Troubleshooting

### Connection keeps dropping

**Try these solutions:**
1. ✅ Disable battery optimization for COSMIC Connect (Android Settings → Battery)
2. ✅ Remove background restrictions (Android Settings → Apps → COSMIC Connect)
3. ✅ Ensure strong Wi-Fi signal
4. ✅ Keep both devices awake during transfers
5. ✅ Avoid switching between Wi-Fi and mobile data

### Files won't transfer

**Check:**
1. ✅ Enough storage space on receiving device
2. ✅ Grant storage permissions (Android Settings → Apps → COSMIC Connect → Permissions)
3. ✅ Stable Wi-Fi connection
4. ✅ Try smaller files first to test

### Notifications not syncing

**Solutions:**
1. ✅ Grant notification access (Android Settings → Apps → COSMIC Connect → Notifications)
2. ✅ Enable Notification Sync plugin on both devices
3. ✅ Select apps in plugin settings
4. ✅ Disable Do Not Disturb mode
5. ✅ Restart both apps

### Clipboard not working

**Check:**
1. ✅ Clipboard plugin enabled on both devices
2. ✅ Devices are connected and paired
3. ✅ Grant clipboard permissions on Android
4. ✅ Try copying simple text first

### App crashes or freezes

**Try:**
1. ✅ Update to latest version
2. ✅ Clear app cache (Android Settings → Apps → COSMIC Connect → Storage → Clear Cache)
3. ✅ Reinstall the app
4. ✅ [Report the bug](https://github.com/olafkfreund/cosmic-connect-android/issues)

---

## Security & Privacy

### Is COSMIC Connect secure?

**Yes!**
- All communication encrypted with TLS (same as banks)
- Data never goes through internet or cloud
- Stays on your local network
- Open-source - anyone can audit the code

### Can someone intercept my data?

**No.** Even if someone is on your Wi-Fi network, they cannot decrypt the TLS-encrypted communication between your paired devices.

### Does COSMIC Connect collect my data?

**Absolutely not!**
- No data collection
- No analytics
- No cloud servers
- No telemetry
- 100% local communication

### What permissions does it need?

**Android permissions:**
- **Storage** - Send/receive files
- **Notifications** - Sync notifications (only if enabled)
- **Network** - Wi-Fi communication
- **Bluetooth** - Optional alternative to Wi-Fi

All permissions are optional and used only for their stated purpose.

### Can COSMIC Connect access my passwords?

**No.** COSMIC Connect can only access:
- Files you explicitly share
- Clipboard content (text you copy)
- Notifications from apps you select
- No access to passwords, system files, or app data

### Can I use it over the internet (remotely)?

**Not recommended.** COSMIC Connect is designed for local network use. While technically possible with a VPN, it's not officially supported and may have security implications.

---

## Development & Contributing

### Is COSMIC Connect open source?

**Yes!** Licensed under GPL-3.0.
- Source code: [GitHub Repository](https://github.com/olafkfreund/cosmic-connect-android)
- Rust core: [cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core)

### Can I contribute?

**Yes, contributions welcome!**
- See [CONTRIBUTING.md](CONTRIBUTING.md)
- Check [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- Read [Developer Documentation](docs/INDEX.md)

### How do I report a bug?

1. Search [existing issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
2. If not found, [create new issue](https://github.com/olafkfreund/cosmic-connect-android/issues/new)
3. Include:
   - Device model & Android version
   - Steps to reproduce
   - Error messages
   - Screenshots (if applicable)

### How can I request a feature?

[Open a feature request](https://github.com/olafkfreund/cosmic-connect-android/issues/new) on GitHub Issues with:
- Clear description of the feature
- Use cases
- Why it would be useful

### Is there a roadmap?

**Yes!** See:
- [Project Plan](docs/guides/PROJECT_PLAN.md)
- [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- [Project Status](docs/guides/project-status-2026-01-17.md)

---

## Compatibility

### Does it work with KDE Connect?

**Yes!** COSMIC Connect uses the same protocol as KDE Connect, so devices running KDE Connect can pair with COSMIC Connect devices.

### Can I use it with GNOME?

For GNOME, use the original KDE Connect or GSConnect. COSMIC Connect is specifically designed for COSMIC Desktop, though it's protocol-compatible.

### Does it work on Windows or macOS?

The Android app works the same regardless of desktop OS. However, the desktop component is designed for COSMIC Desktop. For Windows/macOS, use KDE Connect desktop apps.

### What about iOS?

Currently Android only. There's no official KDE Connect for iOS, so COSMIC Connect doesn't support iOS either.

---

## Getting More Help

### Where can I find more information?

**Documentation:**
- 📖 [Complete User Guide](docs/guides/USER_GUIDE.md)
- 🚀 [Getting Started Guide](docs/guides/GETTING_STARTED.md)
- 📚 [Documentation Index](docs/INDEX.md)

**Support:**
- 🐛 [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- 💬 COSMIC Desktop Community
- 📧 Project maintainers

### How can I stay updated?

- ⭐ Star the [GitHub repository](https://github.com/olafkfreund/cosmic-connect-android)
- 👀 Watch releases for updates
- 📢 Follow COSMIC Desktop news

---

## Still Have Questions?

If your question isn't answered here:

1. **Check the User Guide**: [docs/guides/USER_GUIDE.md](docs/guides/USER_GUIDE.md)
2. **Search GitHub Issues**: [Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
3. **Ask a Question**: [Create new issue](https://github.com/olafkfreund/cosmic-connect-android/issues/new)

---

<div align="center">

**COSMIC Connect - Seamless Android ↔ COSMIC Desktop Integration**

*Open Source • Private • Secure*

[GitHub](https://github.com/olafkfreund/cosmic-connect-android) • [Documentation](docs/INDEX.md) • [User Guide](docs/guides/USER_GUIDE.md)

</div>
