# Rich Notifications - User Guide

**Seamlessly sync notifications with rich content between your Android device and COSMIC Desktop!**

**Version:** 1.0.0
**Last Updated:** January 31, 2026
**Feature:** Part of Notifications Plugin

---

## Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Supported Content Types](#supported-content-types)
  - [Images and Icons](#images-and-icons)
  - [Rich Text](#rich-text)
  - [Links and URLs](#links-and-urls)
  - [Action Buttons](#action-buttons)
  - [Inline Replies](#inline-replies)
- [Enabling Rich Notifications](#enabling-rich-notifications)
- [Privacy Settings](#privacy-settings)
- [App Compatibility](#app-compatibility)
- [Configuration Options](#configuration-options)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Overview

Rich Notifications extends the basic notification sync feature to include images, formatted text, clickable links, action buttons, and inline reply capabilities. This allows you to interact with your Android notifications directly from your COSMIC Desktop without picking up your phone.

### Key Features

- **Notification Icons**: App icons and notification images appear on your desktop
- **Messaging Integration**: Deep integration with popular messaging apps
- **Inline Replies**: Reply to messages directly from your desktop
- **Action Buttons**: Trigger notification actions (Mark as Read, Delete, etc.)
- **Link Detection**: URLs in notifications become clickable on desktop
- **Group Chat Support**: See group names and individual senders
- **Privacy Controls**: Fine-grained control over what content is shared

### Benefits

- **Stay Focused**: Handle notifications without context-switching to your phone
- **Faster Responses**: Reply to messages using your desktop keyboard
- **Visual Context**: See notification icons for quick identification
- **Privacy Aware**: Choose which apps share full content vs. minimal info

---

## How It Works

```
+-------------------+        WiFi/Network         +----------------------+
|   Android Phone   | --------------------------> |   COSMIC Desktop     |
|                   |     Encrypted TCP/TLS       |                      |
| [Notification     |                             | [Notification        |
|  Listener Service]|                             |  Display]            |
|       |           |                             |       |              |
|  NotificationsPlugin                            |  Freedesktop Notif.  |
|       |           |                             |       |              |
|  Extract Content  | --- cconnect.notification ---> Parse & Display    |
|       |           |                             |       |              |
|  Encode Icon PNG  | --- payload (icon bytes) --> Decode & Show       |
|       |           |                             |       |              |
|  Detect Actions   | <-- cconnect.notification.action -- Trigger      |
|       |           |                             |       |              |
|  Handle Replies   | <-- cconnect.notification.reply --- Send Reply   |
+-------------------+                             +----------------------+
```

### Data Flow

1. **Notification Posted**: Android notifies COSMIC Connect of a new notification
2. **Content Extraction**: The plugin extracts title, text, icon, actions, and reply support
3. **Privacy Filter**: App-specific privacy settings are applied
4. **Packet Creation**: A `cconnect.notification` packet is created with metadata
5. **Icon Transfer**: If enabled, the app icon is compressed as PNG and attached
6. **Network Transfer**: The packet is sent over the encrypted TLS connection
7. **Desktop Display**: COSMIC Desktop shows the notification using freedesktop standards
8. **Interaction**: User can dismiss, trigger actions, or reply directly from desktop
9. **Sync Back**: Actions/replies are sent back to Android and executed

---

## Supported Content Types

### Images and Icons

Rich Notifications supports transferring notification icons from Android to your desktop.

**What gets transferred:**

| Icon Type | Description | When Used |
|-----------|-------------|-----------|
| **Large Icon** | Main notification image (e.g., contact photo) | Messaging apps, email |
| **App Icon** | Application icon as fallback | All notifications |
| **No Icon** | When privacy blocks images | Privacy-enabled apps |

**Image Format:**
- Format: PNG (compressed)
- Maximum size: 96x96 pixels (normalized)
- Transfer: TCP payload with MD5 hash verification

**Example notification with icon:**

```
+------------------------------------------+
| [App Icon]  Messages                     |
|             Alice                        |
|             Hey, are you coming tonight? |
|                                          |
|  [Reply]  [Mark as Read]                 |
+------------------------------------------+
```

### Rich Text

Notifications preserve text formatting and structure when possible.

**Supported text elements:**

- **Title**: Notification title (sender name, app name)
- **Text**: Main notification body
- **Ticker**: Combined title + text for status bar display
- **Big Text**: Expanded notification content
- **Conversation Messages**: Message history from messaging apps

**Conversation format for messaging apps:**

```
Group Chat: Work Team
  Alice: Can we meet at 3pm?
  Bob: Works for me
  Charlie: I'll be there
```

### Links and URLs

Links detected in notifications can be opened on your desktop.

**How link handling works:**

1. When a messaging notification is received, the plugin checks if the app has a web URL
2. Known messaging apps (Telegram, WhatsApp Web, Discord, etc.) include web URLs
3. Clicking the notification on desktop can open the corresponding web interface

**Supported messaging apps with web access:**

| App | Web URL |
|-----|---------|
| Telegram | web.telegram.org |
| WhatsApp | web.whatsapp.com |
| Discord | discord.com |
| Slack | app.slack.com |
| Signal Desktop | (companion app) |

**Note**: Link opening requires the App Continuity feature. See [App Continuity Guide](APP_CONTINUITY.md).

### Action Buttons

Notification action buttons are synchronized and can be triggered from your desktop.

**Supported actions:**

- **Mark as Read**: Mark message/email as read
- **Delete**: Delete the notification content
- **Archive**: Archive emails or messages
- **Custom Actions**: App-defined actions

**Example with actions:**

```
+------------------------------------------+
| [Gmail]  New Email from Boss             |
|          Project Update Required         |
|                                          |
|  [Archive]  [Delete]  [Reply]            |
+------------------------------------------+
```

**Triggering actions:**
- Click the action button on the desktop notification
- The action is sent to Android and executed
- The notification updates on both devices

### Inline Replies

Reply to notifications directly from your desktop without touching your phone.

**How inline replies work:**

1. Notification arrives with reply capability (has `requestReplyId`)
2. Desktop shows a "Reply" action or text input
3. User types reply on desktop
4. Reply packet is sent to Android
5. Android sends the reply through the original app

**Supported apps for inline replies:**

- Most messaging apps (Messages, WhatsApp, Telegram, etc.)
- Email apps with quick reply (Gmail, Outlook)
- Social media apps (Twitter/X, Instagram DMs)
- Any app using Android's RemoteInput API

**Reply example:**

```
Desktop shows notification:
+------------------------------------------+
| [Messages]  Alice                        |
|             Are you coming to dinner?    |
|                                          |
|  [Reply: Sure, be there at 7!     ] [Send]|
+------------------------------------------+

User types "Sure, be there at 7!" and clicks Send.
Reply is sent through Messages app on Android.
```

---

## Enabling Rich Notifications

### Step 1: Grant Notification Access

1. Open **COSMIC Connect** on your Android device
2. Go to **Settings** > **Notifications**
3. Tap **Enable Notification Access**
4. Enable **COSMIC Connect** in system settings
5. Return to COSMIC Connect

### Step 2: Enable Notification Plugin

1. Open your paired device in COSMIC Connect
2. Find the **Notifications** plugin
3. Toggle **Enable** if not already enabled
4. Notifications will now sync to your desktop

### Step 3: Configure Desktop (Optional)

On COSMIC Desktop:
1. Notifications appear automatically via freedesktop notification system
2. To customize, open **COSMIC Settings** > **Notifications**
3. Configure how COSMIC Connect notifications are displayed

---

## Privacy Settings

Rich Notifications includes fine-grained privacy controls to protect sensitive information.

### Per-App Privacy Options

For each app, you can configure:

| Setting | Effect | Use Case |
|---------|--------|----------|
| **Full Access** | All content including images | Trusted apps like calendar |
| **Block Content** | Shows "New notification" only | Banking apps, 2FA |
| **Block Images** | Text only, no icons | Work apps, sensitive photos |
| **Disabled** | No notifications sent | Spam apps, games |

### Configuring Privacy Settings

1. Open **COSMIC Connect** on Android
2. Navigate to paired device > **Notifications**
3. Tap **Notification Filter**
4. Find the app you want to configure
5. Select your preferred privacy level

### Privacy Best Practices

**Apps to consider blocking content:**
- Banking and financial apps
- Password managers
- Two-factor authentication apps
- Healthcare apps
- Private messaging (if desired)

**Apps safe for full access:**
- Calendar and scheduling
- Weather apps
- News apps
- Social media (if desired)
- Messaging apps (if desired)

---

## App Compatibility

### Fully Compatible Apps

These apps work seamlessly with all Rich Notification features:

**Messaging:**
- Google Messages
- Telegram
- WhatsApp
- Signal
- Discord
- Slack

**Email:**
- Gmail
- Outlook
- ProtonMail

**Social:**
- Twitter/X
- Instagram
- Facebook Messenger

### Partially Compatible Apps

These apps support basic notifications but may lack some features:

| App | Icons | Actions | Reply | Notes |
|-----|-------|---------|-------|-------|
| Snapchat | Yes | Limited | No | Privacy by design |
| TikTok | Yes | No | No | Limited notification actions |
| Some banking apps | No | No | No | Security restrictions |

### Known Limitations

Some apps impose restrictions that limit Rich Notification functionality:

1. **Secure apps**: Banking apps may hide content entirely
2. **DRM content**: Some media notifications are restricted
3. **Sensitive notifications**: Android may redact content on lock screen
4. **Legacy apps**: Apps not using modern notification APIs

---

## Configuration Options

### Android Settings

**In COSMIC Connect app:**

| Setting | Description | Default |
|---------|-------------|---------|
| Enable Notifications | Master toggle for notification sync | On |
| Screen Off Only | Only sync when screen is off | Off |
| Notification Filter | Per-app enable/disable | All enabled |
| Privacy Settings | Per-app content/image blocking | None blocked |

### Desktop Settings

**In COSMIC Settings:**

| Setting | Description | Default |
|---------|-------------|---------|
| Show Notifications | Display incoming notifications | On |
| Notification Position | Screen corner for notifications | Top Right |
| Auto-hide Timeout | How long notifications stay visible | 5 seconds |
| Sound | Play sound for COSMIC Connect notifications | On |

---

## Best Practices

### For Best Experience

1. **Enable full access for frequently used apps**: Messaging apps, email
2. **Block content for sensitive apps**: Banking, 2FA, health
3. **Use inline replies**: Much faster than switching to phone
4. **Configure desktop notification settings**: Set appropriate timeouts
5. **Keep devices on same network**: Reduces latency

### For Battery Life

1. **Use Screen Off Only mode**: Reduces duplicate notifications
2. **Disable notifications for noisy apps**: Games, promotional apps
3. **Keep WiFi stable**: Avoids reconnection overhead

### For Privacy

1. **Review app list regularly**: Disable new apps you don't need
2. **Use content blocking liberally**: When in doubt, block
3. **Consider screen-off mode**: Notifications only when you're not using phone
4. **Check privacy settings after app updates**: Settings may reset

---

## Troubleshooting

For detailed troubleshooting, see [Rich Notifications Troubleshooting Guide](RICH_NOTIFICATIONS_TROUBLESHOOTING.md).

### Quick Fixes

**Notifications not appearing on desktop?**
1. Check that devices are paired and connected
2. Verify notification permission is granted on Android
3. Ensure the app is not disabled in Notification Filter

**Icons not showing?**
1. Check if "Block Images" is enabled for that app
2. Verify payload transfer is working (check connection quality)
3. Some apps don't provide icons

**Replies not working?**
1. Not all apps support inline replies
2. Check if the notification shows a Reply action
3. Ensure the message notification is still active on Android

**Links not opening?**
1. Enable App Continuity feature
2. Check if the app has web URL support
3. Verify desktop browser is configured

---

## Related Documentation

- [Rich Notifications Troubleshooting](RICH_NOTIFICATIONS_TROUBLESHOOTING.md) - Detailed problem solving
- [Rich Notifications Protocol Specification](protocol/RICH_NOTIFICATIONS_PROTOCOL.md) - Technical protocol details
- [Rich Notifications Development Guide](guides/RICH_NOTIFICATIONS_DEVELOPMENT.md) - For developers
- [App Continuity Guide](APP_CONTINUITY.md) - Link opening feature
- [User Guide](USER_GUIDE.md) - General COSMIC Connect usage
- [FAQ](FAQ.md) - Frequently asked questions

---

**Enjoy rich, interactive notifications across your devices!**
