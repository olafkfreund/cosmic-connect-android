# Notification Link Handler - Phase 2c

Clickable link support in notifications for both Android and desktop.

## Components

### LinkDetector
Detects URLs in notification text using:
1. URLSpan extraction from SpannableString
2. Custom regex pattern matching
3. Android's Patterns.WEB_URL fallback

**Security features:**
- Rejects non-http(s) protocols (ftp, javascript, data, etc.)
- Blocks localhost and private IP addresses
- Validates URL format before acceptance
- Limits to 5 links per notification

### DeepLinkHandler
Analyzes URLs to determine handling strategy:
- **APP_DEEP_LINK**: URL can be opened in a specific app
- **WEB_URL**: Regular URL for browser
- **WEB_URL_WITH_APP_AVAILABLE**: App exists but not installed

Creates appropriate intents for opening URLs in apps or browser.

### NotificationLinkHandler
Main integration class that:
- Extracts links from StatusBarNotification
- Creates PendingIntent actions for links
- Formats links for notification display
- Sends "open link" packets to desktop

## Integration with NotificationsPlugin

### 1. Add Link Detection to Notification Sending

In `NotificationsPlugin.kt`, modify `sendNotification()`:

```kotlin
private fun sendNotification(statusBarNotification: StatusBarNotification, isPreexisting: Boolean) {
    // ... existing code ...

    // Extract links if not preexisting
    val links = if (!isPreexisting) {
        linkHandler.extractLinks(statusBarNotification)
    } else {
        emptyList()
    }

    // Add link info to notification packet
    if (links.isNotEmpty()) {
        val linkText = linkHandler.formatLinksForNotification(links)
        // Append to notification text or send as separate field
    }
}
```

### 2. Add Link Action Support

Extend `NotificationInfo` to include links:

```kotlin
data class NotificationInfo(
    // ... existing fields ...
    val links: List<String>? = null,  // JSON array of link URLs
    val linkLabels: List<String>? = null  // JSON array of link labels
)
```

### 3. Create BroadcastReceiver for Link Actions

Create `NotificationLinkReceiver.kt`:

```kotlin
class NotificationLinkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationLinkHandler.ACTION_OPEN_LINK -> {
                val url = intent.getStringExtra(NotificationLinkHandler.EXTRA_URL)
                // Open link locally
            }

            NotificationLinkHandler.ACTION_OPEN_LINK_ON_DESKTOP -> {
                val url = intent.getStringExtra(NotificationLinkHandler.EXTRA_URL)
                val deviceId = intent.getStringExtra(NotificationLinkHandler.EXTRA_DEVICE_ID)
                // Send packet to desktop
            }
        }
    }
}
```

Register in AndroidManifest.xml:

```xml
<receiver android:name=".Plugins.NotificationsPlugin.links.NotificationLinkReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="org.cosmic.cosmicconnect.OPEN_LINK"/>
        <action android:name="org.cosmic.cosmicconnect.OPEN_LINK_ON_DESKTOP"/>
    </intent-filter>
</receiver>
```

### 4. Desktop Integration

Extend notification packet type to include:

```json
{
    "type": "cconnect.notification",
    "id": "notification-123",
    "appName": "Twitter",
    "title": "New Message",
    "text": "Check this out https://example.com",
    "links": [
        {
            "url": "https://example.com",
            "label": "example.com"
        }
    ]
}
```

Desktop handler should:
1. Display links as clickable in notification UI
2. Handle click to open in desktop browser
3. Support "openLink" action packet from Android

## Usage Example

```kotlin
// Inject dependencies
@Inject lateinit var linkHandler: NotificationLinkHandler

// Extract links from notification
val links = linkHandler.extractLinks(statusBarNotification)

// Create action buttons (max 3)
val actionButtons = linkHandler.createLinkActionButtons(
    links = links,
    deviceId = device.deviceId,
    notificationId = notificationId
)

// Send to desktop with link info
val formatted = linkHandler.formatLinksForNotification(links)
// Append to notification text
```

## Testing

Unit tests are provided for:
- LinkDetector: URL detection, security validation
- NotificationLinkHandler: Link extraction, action creation

Run tests with:
```bash
./gradlew :app:testDebugUnitTest
```

## Security Considerations

1. **Protocol Validation**: Only http/https allowed
2. **Private Network Blocking**: Localhost and RFC1918 addresses rejected
3. **Link Limit**: Maximum 5 links to prevent abuse
4. **Intent Security**: PendingIntents use FLAG_IMMUTABLE on API 23+
5. **URL Sanitization**: All URLs validated before use

## Desktop Implementation Notes

The desktop side (cosmic-applet-kdeconnect) should:

1. Parse `links` array from notification packet
2. Display links as clickable UI elements
3. Handle link clicks by opening in browser
4. Support receiving "openLink" action packets
5. Show link preview/security indicators

## Future Enhancements

1. **Link Preview**: Fetch and display link previews
2. **Link Classification**: Detect malicious/phishing URLs
3. **Link Shortener Resolution**: Expand bit.ly, t.co, etc.
4. **Custom Actions**: Per-domain custom handling
5. **Link History**: Track opened links for debugging
