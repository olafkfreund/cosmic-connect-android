/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.service.notification.StatusBarNotification
import android.util.Log
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.Payload as CorePayload
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Rich notification packet creation.
 *
 * Creates COSMIC Connect notification packets with rich content support:
 * - BigPictureStyle images
 * - Rich text with formatting
 * - Embedded links
 * - Video thumbnails
 *
 * ## Packet Structure
 * Rich notifications extend the standard notification packet with:
 * - `hasImage` (Boolean): Indicates image payload present
 * - `imageFormat` (String): Image format (png, jpeg, webp)
 * - `imageWidth` (Int): Image width in pixels
 * - `imageHeight` (Int): Image height in pixels
 * - `richText` (String): Formatted text content
 * - `links` (Array): Embedded URLs with titles
 * - `hasVideo` (Boolean): Video content indicator
 * - `videoTitle` (String): Video title
 *
 * ## Image Transmission
 * Images are sent as payload data with:
 * 1. Packet fields describe image metadata
 * 2. Payload contains compressed image bytes
 * 3. payloadHash (MD5) for integrity verification
 *
 * ## Usage
 * ```kotlin
 * val extractor = BigPictureExtractor()
 * val image = extractor.extract(statusBarNotification)
 * val richText = extractor.extractRichText(notification)
 * val links = extractor.extractLinks(notification)
 *
 * val packet = RichNotificationPackets.createRichNotificationPacket(
 *     notification = statusBarNotification,
 *     bigPicture = image,
 *     richText = richText,
 *     links = links,
 *     videoInfo = null
 * )
 *
 * // Attach image payload
 * if (image != null) {
 *     RichNotificationPackets.attachImagePayload(packet, image)
 * }
 *
 * device.sendPacket(TransferPacket(packet))
 * ```
 */
object RichNotificationPackets {

    private const val TAG = "COSMIC/RichNotificationPackets"

    /**
     * Create a rich notification packet.
     *
     * Creates a notification packet with rich content metadata.
     * The base notification info is enhanced with:
     * - Image metadata (if bigPicture present)
     * - Rich text formatting
     * - Embedded links
     * - Video information
     *
     * Note: This creates the packet with metadata only. Use attachImagePayload()
     * to attach the actual image data.
     *
     * @param notification Base notification information
     * @param bigPicture Extracted BigPictureStyle image
     * @param richText Formatted text content
     * @param links Embedded URLs
     * @param videoInfo Video metadata
     * @return NetworkPacket with rich content metadata
     */
    fun createRichNotificationPacket(
        notification: NotificationInfo,
        bigPicture: ExtractedImage? = null,
        richText: String? = null,
        links: List<NotificationLink>? = null,
        videoInfo: VideoInfo? = null
    ): NetworkPacket {
        // Start with base notification packet
        val basePacket = NotificationsPacketsFFI.createNotificationPacket(notification)

        // Create enhanced body with rich content fields
        val enhancedBody = basePacket.body.toMutableMap()

        // Add image metadata
        if (bigPicture != null) {
            enhancedBody["hasImage"] = true
            enhancedBody["imageFormat"] = bigPicture.format.name.lowercase()
            enhancedBody["imageWidth"] = bigPicture.width
            enhancedBody["imageHeight"] = bigPicture.height
            enhancedBody["imageSize"] = bigPicture.sizeBytes

            // Add payload hash (will be set by attachImagePayload)
            // For now, just indicate payload is expected
            enhancedBody["payloadTransfer"] = mapOf(
                "type" to "image",
                "format" to bigPicture.format.name.lowercase()
            )
        }

        // Add rich text
        if (richText != null && richText.isNotEmpty()) {
            enhancedBody["richText"] = richText
        }

        // Add links
        if (links != null && links.isNotEmpty()) {
            val linksArray = links.map { link ->
                mapOf(
                    "url" to link.url,
                    "title" to (link.title ?: "")
                )
            }
            enhancedBody["links"] = linksArray
        }

        // Add video info
        if (videoInfo != null) {
            enhancedBody["hasVideo"] = true
            videoInfo.title?.let { enhancedBody["videoTitle"] = it }
            videoInfo.duration?.let { enhancedBody["videoDuration"] = it }
        }

        return NetworkPacket(
            id = basePacket.id,
            type = basePacket.type,
            body = enhancedBody,
            payloadSize = bigPicture?.sizeBytes?.toLong()
        )
    }

    /**
     * Attach image payload to packet.
     *
     * Creates a TransferPacket wrapping the Core packet with the image as a Core.Payload.
     * Adds payloadHash (MD5) and payloadSize to the packet body for integrity verification.
     *
     * @param packet NetworkPacket with image metadata
     * @param image ExtractedImage to attach
     * @return TransferPacket with payload attached, ready for sending
     */
    fun attachImagePayload(packet: NetworkPacket, image: ExtractedImage): TransferPacket {
        // Compress image
        val imageBytes = image.toBytes(quality = 85)

        // Calculate MD5 hash
        val hash = calculateMd5(imageBytes)

        // Create updated packet with hash and size in body
        val updatedBody = packet.body.toMutableMap()
        updatedBody["payloadHash"] = hash
        updatedBody["payloadSize"] = imageBytes.size

        val updatedPacket = NetworkPacket(
            id = packet.id,
            type = packet.type,
            body = updatedBody,
            payloadSize = imageBytes.size.toLong()
        )

        Log.d(TAG, "Attached ${imageBytes.size} byte image payload with hash $hash")
        return TransferPacket(updatedPacket, payload = CorePayload(imageBytes))
    }

    /**
     * Calculate MD5 hash of data.
     *
     * @param data Bytes to hash
     * @return MD5 hash as hex string
     */
    private fun calculateMd5(data: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(data)
            bytesToHex(md.digest())
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "MD5 algorithm not available", e)
            ""
        }
    }

    /**
     * Convert byte array to hex string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars).lowercase()
    }

    /**
     * Create rich notification packet from StatusBarNotification.
     *
     * Convenience method that extracts all rich content and creates
     * a complete packet in one call.
     *
     * @param statusBarNotification The notification to process
     * @param baseInfo Base notification information (from NotificationsPlugin)
     * @param extractor BigPictureExtractor instance
     * @return TransferPacket ready for sending (with payload if image present)
     */
    fun createFromStatusBarNotification(
        statusBarNotification: StatusBarNotification,
        baseInfo: NotificationInfo,
        extractor: BigPictureExtractor
    ): TransferPacket {
        val notification = statusBarNotification.notification

        // Extract rich content
        val bigPicture = extractor.extract(statusBarNotification)
        val richText = extractor.extractRichText(notification)
        val links = extractor.extractLinks(notification)
        val videoInfo = extractor.extractVideoInfo(notification)

        // Create packet
        val packet = createRichNotificationPacket(
            notification = baseInfo,
            bigPicture = bigPicture,
            richText = richText,
            links = links,
            videoInfo = videoInfo
        )

        // Attach image if present
        return if (bigPicture != null) {
            attachImagePayload(packet, bigPicture)
        } else {
            TransferPacket(packet)
        }
    }
}

// Extension properties for inspecting rich notification packets

/**
 * Check if notification has an image payload.
 */
val NetworkPacket.hasImage: Boolean
    get() = body["hasImage"] as? Boolean == true

/**
 * Get image format (png, jpeg, webp).
 */
val NetworkPacket.imageFormat: String?
    get() = if (hasImage) body["imageFormat"] as? String else null

/**
 * Get image width in pixels.
 */
val NetworkPacket.imageWidth: Int?
    get() = if (hasImage) body["imageWidth"] as? Int else null

/**
 * Get image height in pixels.
 */
val NetworkPacket.imageHeight: Int?
    get() = if (hasImage) body["imageHeight"] as? Int else null

/**
 * Get rich formatted text.
 */
val NetworkPacket.richText: String?
    get() = body["richText"] as? String

/**
 * Get embedded links.
 */
@Suppress("UNCHECKED_CAST")
val NetworkPacket.links: List<NotificationLink>?
    get() {
        val linksData = body["links"] as? List<Map<String, String>> ?: return null
        return linksData.map { link ->
            NotificationLink(
                url = link["url"] ?: "",
                title = link["title"]?.takeIf { it.isNotEmpty() }
            )
        }
    }

/**
 * Check if notification has video content.
 */
val NetworkPacket.hasVideo: Boolean
    get() = body["hasVideo"] as? Boolean == true

/**
 * Get video title.
 */
val NetworkPacket.videoTitle: String?
    get() = if (hasVideo) body["videoTitle"] as? String else null
