# Rich Notifications Protocol - Issue #136 Phase 3

## Implementation Status

✅ Android FFI Layer: Complete (`RichNotificationsFFI.kt`)
⏳ Rust FFI Functions: See implementation guide below
⏳ Desktop Handler: To be implemented
⏳ Tests: To be written

## Android FFI API

File created: `app/src/org/cosmic/cosmicconnect/Plugins/NotificationPlugin/RichNotificationsFFI.kt`

### Create Rich Notification
\`\`\`kotlin
val packet = RichNotificationsFFI.createRichNotificationPacket(
    id = "msg-001",
    appName = "Messages",
    title = "Alice",
    text = "Check out this photo!",
    richText = "<b>Check out</b> this <i>photo</i>!",
    hasImage = true,
    imageWidth = 1024,
    imageHeight = 768,
    hasVideo = false,
    videoUrl = null,
    videoThumbnailUrl = null,
    videoDuration = null,
    links = listOf(
        LinkInfo("https://example.com", "View", "WEB")
    )
)
\`\`\`

### Attach Image
\`\`\`kotlin
val imageData = loadImageBytes("photo.jpg")
RichNotificationsFFI.attachImageData(packet, imageData)
device.sendPacket(packet)
\`\`\`

## Rust FFI Implementation

Add to \`cosmic-connect-core/src/cosmic_connect_core.udl\`:

\`\`\`
[Throws=ProtocolError]
FfiPacket create_rich_notification_packet(string notification_json);

[Throws=ProtocolError]
boolean attach_image_to_notification_packet(FfiPacket packet, sequence<u8> image_data);
\`\`\`

Add to \`cosmic-connect-core/src/ffi/mod.rs\` after existing notification functions.

## Desktop Implementation

Parse rich fields in notification handler and use Freedesktop \`image-data\` hint for images.

