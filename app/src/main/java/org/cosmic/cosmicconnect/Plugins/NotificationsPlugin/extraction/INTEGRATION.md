# BigPictureExtractor Integration Guide

## Overview

The `BigPictureExtractor` extracts, scales, compresses, and hashes images from BigPictureStyle notifications. This guide shows how to integrate it into the NotificationsPlugin.

## Basic Usage

### 1. Import the Extractor

```kotlin
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction.BigPictureExtractor
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction.ExtractedImage
```

### 2. Create Instance

```kotlin
class NotificationsPlugin : Plugin() {
    private lateinit var imageExtractor: BigPictureExtractor

    override fun onCreate(): Boolean {
        // ... existing initialization
        imageExtractor = BigPictureExtractor(context)
        return true
    }
}
```

### 3. Extract Images from Notifications

Add to the `sendNotification` method after existing icon extraction:

```kotlin
private fun sendNotification(statusBarNotification: StatusBarNotification, isPreexisting: Boolean) {
    val notification = statusBarNotification.notification

    // ... existing filter logic ...

    val key = getNotificationKeyCompat(statusBarNotification)
    val packageName = statusBarNotification.packageName

    // Build notification info
    val notificationInfo = buildNotificationInfo(
        key = key,
        packageName = packageName,
        appName = appName,
        statusBarNotification = statusBarNotification,
        notification = notification,
        isPreexisting = isPreexisting
    )

    // Create packet using FFI wrapper
    val packet = NotificationsPacketsFFI.createNotificationPacket(notificationInfo)

    // Handle icon payload for new notifications
    val isUpdate = currentNotifications.contains(key)
    if (!isUpdate) {
        currentNotifications.add(key)

        // Check privacy settings
        val blockImages = appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES)

        if (!blockImages) {
            // EXISTING: Attach app icon
            val appIcon = extractIcon(statusBarNotification, notification)
            if (appIcon != null) {
                attachIcon(packet, appIcon)
            }

            // NEW: Attach BigPicture image if present
            val bigPicture = imageExtractor.extractBigPicture(statusBarNotification)
            if (bigPicture != null) {
                attachBigPicture(packet, bigPicture)
                // Clean up bitmap after use
                bigPicture.bitmap.recycle()
            }
        }
    }

    device.sendPacket(packet.toLegacyPacket())
}
```

### 4. Add Payload Attachment Method

```kotlin
/**
 * Attach BigPicture image as payload to packet.
 *
 * Uses the compressed JPEG data and MD5 hash from ExtractedImage.
 * The desktop can use the hash to deduplicate identical images.
 */
private fun attachBigPicture(packet: NetworkPacket, extractedImage: ExtractedImage) {
    try {
        // Create payload from compressed image data
        val payloadClass = Class.forName("org.cosmic.cosmicconnect.NetworkPacket\$Payload")
        val payload = payloadClass
            .getConstructor(ByteArray::class.java)
            .newInstance(extractedImage.compressedData)

        val setPayloadMethod = LegacyNetworkPacket::class.java
            .getMethod("setPayload", payloadClass)
        setPayloadMethod.invoke(packet.toLegacyPacket(), payload)

        // Set metadata fields
        val legacyPacket = packet.toLegacyPacket()
        val setMethod = LegacyNetworkPacket::class.java
            .getMethod("set", String::class.java, Any::class.java)

        setMethod.invoke(legacyPacket, "bigPictureHash", extractedImage.hash)
        setMethod.invoke(legacyPacket, "bigPictureWidth", extractedImage.width)
        setMethod.invoke(legacyPacket, "bigPictureHeight", extractedImage.height)
        setMethod.invoke(legacyPacket, "bigPictureMimeType", extractedImage.mimeType)
        setMethod.invoke(legacyPacket, "bigPictureSize", extractedImage.sizeBytes)

        Log.d(TAG, "Attached big picture: ${extractedImage.width}x${extractedImage.height}, " +
                   "${extractedImage.sizeBytes} bytes, hash=${extractedImage.hash}")

    } catch (e: Exception) {
        Log.e(TAG, "Error attaching big picture payload", e)
    }
}
```

## Features

### Automatic Scaling

Images larger than 400x400 are automatically scaled down while maintaining aspect ratio:

```kotlin
// Image is 1920x1080
val extracted = imageExtractor.extractBigPicture(notification)
// Result: 400x225 (maintains 16:9 ratio)
```

### Deduplication via Hash

The MD5 hash allows desktop to skip duplicate transfers:

```kotlin
val image1 = imageExtractor.extractBigPicture(notification1)
val image2 = imageExtractor.extractBigPicture(notification2)

if (image1.hash == image2.hash) {
    // Desktop already has this image, skip transfer
}
```

### Memory Efficient

Bitmaps are properly recycled:

```kotlin
val extracted = imageExtractor.extractBigPicture(notification)
if (extracted != null) {
    attachBigPicture(packet, extracted)
    extracted.bitmap.recycle() // Important: prevent memory leaks
}
```

## Privacy Integration

Respects existing privacy settings:

```kotlin
val blockImages = appDatabase.getPrivacy(
    packageName,
    AppDatabase.PrivacyOptions.BLOCK_IMAGES
)

if (!blockImages) {
    // Only extract if user hasn't blocked images for this app
    val bigPicture = imageExtractor.extractBigPicture(statusBarNotification)
    // ...
}
```

## Error Handling

All extraction methods return null on error:

```kotlin
val extracted = imageExtractor.extractBigPicture(notification)

if (extracted == null) {
    // No big picture found, or error during extraction
    Log.d(TAG, "No big picture available")
    return
}

// Use extracted image
```

## Testing

Unit tests verify all functionality:

```bash
./gradlew :app:testDebugUnitTest \
    --tests "org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction.BigPictureExtractorTest"
```

## Performance Characteristics

- **Scaling:** O(n) where n = pixel count, uses Android's built-in scaling
- **Compression:** O(n) JPEG compression, typically 10-20x size reduction
- **Hashing:** O(n) MD5 hash of compressed data
- **Memory:** Allocates scaled bitmap + compressed ByteArray, properly recycled

## Packet Format

The enhanced notification packet includes:

```json
{
  "type": "cconnect.notification",
  "body": {
    "id": "notification_key",
    "appName": "Messaging App",
    "title": "New message",
    "text": "Hello!",
    "bigPictureHash": "a1b2c3d4e5f6...",
    "bigPictureWidth": 400,
    "bigPictureHeight": 300,
    "bigPictureMimeType": "image/jpeg",
    "bigPictureSize": 45678
  },
  "payloadSize": 45678,
  "payloadTransferInfo": {
    "port": 1739
  }
}
```

## Next Steps

After integrating BigPictureExtractor:

1. **Phase 2b:** Implement image caching to avoid re-transferring identical images
2. **Phase 2c:** Add server-side image deduplication using the hash
3. **Phase 2d:** Implement desktop display of BigPicture images
4. **Phase 3:** Add support for MessagingStyle images and contact photos

## See Also

- [NotificationsPlugin.kt](../NotificationsPlugin.kt) - Main plugin implementation
- [BigPictureExtractor.kt](BigPictureExtractor.kt) - Extractor implementation
- [BigPictureExtractorTest.kt](../../../../test/java/org/cosmic/cosmicconnect/Plugins/NotificationsPlugin/extraction/BigPictureExtractorTest.kt) - Unit tests
