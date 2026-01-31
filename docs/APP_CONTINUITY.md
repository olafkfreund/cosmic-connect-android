# Opening Content Across Devices

**Last Updated:** January 31, 2026

COSMIC Connect lets you seamlessly open links, files, and content on your connected devices. This enables a true multi-device workflow where you can start on one device and continue on another.

---

## Table of Contents

- [Features](#features)
- [From Android to Desktop](#from-android-to-desktop)
- [From Desktop to Android](#from-desktop-to-android)
- [Supported Content Types](#supported-content-types)
- [Configuration](#configuration)
- [Security](#security)
- [Privacy Considerations](#privacy-considerations)

---

## Features

- **Open on Desktop**: Share URLs from Android apps to open in your desktop browser
- **Open on Phone**: Send links from desktop to open on your Android device
- **File Opening**: Transfer and open files with matching applications
- **Deep Link Support**: Open app-specific links (email, phone, maps, etc.)
- **One-Tap Sharing**: Quick access via Android share sheet

---

## From Android to Desktop

### Sharing a URL

1. In any app (Chrome, Firefox, Twitter, etc.), tap **Share**
2. Select **"Open on Desktop"** (or **"COSMIC Connect"**)
3. Choose your COSMIC Desktop device from the list
4. Confirm on desktop when prompted (if security prompts are enabled)
5. The URL opens in your default browser

**Example Use Cases:**
- Found an interesting article on your phone? Send it to your desktop for comfortable reading
- Watching a video on your phone? Continue on your larger desktop screen
- Shopping on mobile? Open the cart on desktop to complete checkout

### Sharing a File

1. In Files, Gallery, or any app, tap **Share** on a file
2. Select **"Open on Desktop"**
3. Choose your COSMIC Desktop device
4. The file transfers automatically and opens with the associated application

**Supported scenarios:**
- Documents (PDF, DOCX, etc.) open in your document viewer
- Images open in your image viewer
- Videos open in your media player
- Archives prompt for extraction

### Quick Share from Browser

1. In your mobile browser, tap the share icon or menu
2. Select **"COSMIC Connect"**
3. The current page URL is sent to your desktop

---

## From Desktop to Android

### Opening a URL from Browser

1. Right-click any link in your browser
2. Select **"Open on Phone"** from the context menu
3. Choose your Android device (if you have multiple)
4. Approve on your phone when the notification appears
5. Opens in your default browser or matching app

### Opening a URL from COSMIC Panel

1. Click the COSMIC Connect applet in your panel
2. Select your Android device
3. Click **"Send to Phone"** and paste or type a URL
4. Confirm on your phone

### Sharing Files from File Manager

1. Right-click a file in your file manager
2. Select **"Send to"** then choose your Android device
3. The file transfers and prompts to open on your phone

---

## Supported Content Types

| Type | Android to Desktop | Desktop to Android | Notes |
|------|-------------------|-------------------|-------|
| Web URLs (`https://`) | Yes | Yes | Opens in default browser |
| HTTP URLs (`http://`) | Yes | Yes | Security warning may appear |
| Email links (`mailto:`) | Yes | Yes | Opens email client |
| Phone numbers (`tel:`) | Yes | Yes | Opens dialer (Android) or shows number (desktop) |
| SMS links (`sms:`) | Yes | Yes | Opens messaging app |
| Maps links (`geo:`) | Yes | Yes | Opens maps application |
| Market links (`market://`) | No | Yes | Opens app store on Android |
| Files (all types) | Yes (transfer + open) | Yes (transfer + open) | Requires matching app |
| Magnet links | No | No | Blocked for security |
| JavaScript links | No | No | Blocked for security |

### URL Scheme Details

**Fully Supported:**
- `https://` - Secure web URLs (recommended)
- `http://` - Unsecured web URLs (warning shown)
- `mailto:user@example.com` - Email composition
- `tel:+1234567890` - Phone dialing
- `sms:+1234567890?body=message` - SMS composition
- `geo:37.7749,-122.4194` - Geographic coordinates

**Blocked for Security:**
- `javascript:` - Script execution
- `file://` - Local file access
- `data:` - Inline data URLs
- Internal/localhost URLs - Private network access
- IP address URLs - Direct IP access

---

## Configuration

### Android Settings

1. Open **COSMIC Connect** app
2. Tap your paired device
3. Tap **Settings** (gear icon)
4. Under **Share Plugin**, configure:
   - **Auto-open URLs**: Automatically open received URLs without confirmation
   - **Show notifications**: Display notification when content is received
   - **Default browser**: Choose which browser opens URLs

### Desktop Settings

1. Open **COSMIC Connect** settings from the panel applet
2. Select your Android device
3. Under **Share settings**, configure:
   - **Require confirmation**: Ask before opening URLs (recommended)
   - **Trusted senders**: Devices that can open content without confirmation
   - **Download location**: Where to save received files

### Notification Behavior

When you receive a URL or file:
- **Android**: Notification appears with options to Open, Save, or Dismiss
- **Desktop**: System notification with quick actions

---

## Security

COSMIC Connect implements multiple security layers for App Continuity:

### URL Validation

All incoming URLs are validated before opening:

1. **Scheme Checking**: Only safe URL schemes are allowed (see supported types above)
2. **Host Validation**: Internal/localhost URLs are blocked
3. **Content Filtering**: Potentially dangerous URLs are flagged
4. **User Confirmation**: By default, all URLs require user approval

### File Security

- Files are scanned for known dangerous extensions
- Executable files prompt additional warnings
- Large files show size confirmation
- Download location is user-controlled

### Trust Model

- Only paired devices can send content
- All communication is TLS encrypted
- Device identity is verified via certificates
- Pairing requires explicit approval on both devices

### Blocked Content

The following are **always blocked** for security:

- Local file URLs (`file://`)
- JavaScript URLs (`javascript:`)
- Data URLs (`data:`)
- Internal IP addresses (192.168.x.x, 10.x.x.x, etc.)
- Localhost references
- URLs with suspicious encodings

---

## Privacy Considerations

### Data Handling

- URLs and files are transmitted directly between your devices
- No cloud servers are involved
- Content is not stored after delivery (unless you save it)
- Transfer history is kept locally and can be cleared

### Permissions

App Continuity requires:
- **Network Access**: To communicate with paired devices
- **Storage** (optional): To save received files

### What We Never Do

- Send your browsing history to any server
- Track which URLs you share
- Store URLs or files on external servers
- Share data with third parties

---

## Tips and Best Practices

### For Best Results

1. **Keep devices on the same network** for fastest transfers
2. **Use HTTPS URLs** when possible for security
3. **Enable notifications** to see when content arrives
4. **Review security settings** for your comfort level

### Keyboard Shortcuts (Desktop)

| Action | Shortcut |
|--------|----------|
| Send current URL to phone | `Ctrl+Shift+P` (configurable) |
| Open COSMIC Connect | `Super+K` (configurable) |

### Troubleshooting Quick Tips

- **URL not opening?** Check that the URL scheme is supported
- **File not transferring?** Ensure both devices are connected
- **Confirmation dialog not appearing?** Check notification settings

For detailed troubleshooting, see [App Continuity Troubleshooting](APP_CONTINUITY_TROUBLESHOOTING.md).

---

## Related Documentation

- [User Guide](USER_GUIDE.md) - Complete COSMIC Connect user guide
- [App Continuity Troubleshooting](APP_CONTINUITY_TROUBLESHOOTING.md) - Solve common issues
- [FAQ](FAQ.md) - Frequently asked questions
- [Privacy Policy](PRIVACY_POLICY.md) - How your data is protected

---

**Enjoy seamless content sharing with COSMIC Connect!**
