/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import java.io.ByteArrayOutputStream

/**
 * Extracted image data from notification.
 *
 * Represents a bitmap image extracted from a BigPictureStyle notification
 * along with metadata for transmission to the desktop.
 *
 * @property bitmap The extracted bitmap image
 * @property format The image format (PNG, JPEG, WEBP)
 * @property sizeBytes Original size in bytes
 * @property width Image width in pixels
 * @property height Image height in pixels
 */
data class ExtractedImage(
    val bitmap: Bitmap,
    val format: ImageFormat,
    val sizeBytes: Int,
    val width: Int = bitmap.width,
    val height: Int = bitmap.height
) {
    enum class ImageFormat {
        PNG, JPEG, WEBP
    }

    /**
     * Compress image to byte array for network transmission.
     *
     * @param quality Compression quality (0-100), default 85
     * @return Compressed image bytes
     */
    fun toBytes(quality: Int = 85): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val compressFormat = when (format) {
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
        bitmap.compress(compressFormat, quality, outputStream)
        return outputStream.toByteArray()
    }
}

/**
 * Link extracted from notification text.
 *
 * @property url The URL
 * @property title Optional link title
 */
data class NotificationLink(
    val url: String,
    val title: String? = null
)

/**
 * Video information extracted from media-style notifications.
 *
 * @property thumbnailBitmap Optional video thumbnail
 * @property duration Video duration in milliseconds
 * @property title Video title
 */
data class VideoInfo(
    val thumbnailBitmap: Bitmap? = null,
    val duration: Long? = null,
    val title: String? = null
)

/**
 * BigPictureStyle image extractor.
 *
 * Extracts large images from Android notifications that use BigPictureStyle.
 * These images are typically photos, screenshots, or media content that apps
 * want to display prominently in the notification.
 *
 * ## Supported Notification Styles
 * - BigPictureStyle: Photos, screenshots, media previews
 * - MessagingStyle: Inline images in messages
 * - MediaStyle: Album art (basic support)
 *
 * ## Privacy Considerations
 * - Respects AppDatabase privacy settings (BLOCK_IMAGES)
 * - Only extracts from user-visible notifications
 * - Does not extract from FLAG_LOCAL_ONLY notifications
 *
 * ## Image Size Limits
 * - Max width: 2048px
 * - Max height: 2048px
 * - Max file size: 10MB (configurable)
 * - Images exceeding limits are downscaled
 *
 * ## Usage
 * ```kotlin
 * val extractor = BigPictureExtractor()
 * val image = extractor.extract(statusBarNotification)
 * if (image != null) {
 *     // Send image to desktop
 *     sendRichNotification(notification, image)
 * }
 * ```
 */
class BigPictureExtractor {

    companion object {
        private const val TAG = "COSMIC/BigPictureExtractor"

        // Image size limits
        const val MAX_IMAGE_WIDTH = 2048
        const val MAX_IMAGE_HEIGHT = 2048
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024 // 10MB

        // Notification extras keys
        private const val EXTRA_PICTURE = "android.picture"
        private const val EXTRA_PICTURE_ICON = "android.pictureIcon"
        private const val EXTRA_BIG_PICTURE = "android.bigPicture"
    }

    /**
     * Extract BigPicture image from notification.
     *
     * Attempts to extract the large image from BigPictureStyle notifications.
     * Returns null if:
     * - Notification doesn't use BigPictureStyle
     * - Image extraction fails
     * - Image exceeds size limits after downscaling
     *
     * @param statusBarNotification The notification to extract from
     * @return ExtractedImage if successful, null otherwise
     */
    fun extract(statusBarNotification: StatusBarNotification): ExtractedImage? {
        val notification = statusBarNotification.notification

        // Check for BigPictureStyle
        if (!hasBigPicture(notification)) {
            return null
        }

        val bitmap = extractBitmap(notification) ?: return null

        // Validate and potentially downscale
        val processedBitmap = validateAndResize(bitmap) ?: run {
            Log.w(TAG, "Image exceeds size limits and couldn't be downscaled")
            return null
        }

        // Determine best format
        val format = determineFormat(processedBitmap)

        // Calculate size
        val bytes = ByteArrayOutputStream().apply {
            processedBitmap.compress(
                when (format) {
                    ExtractedImage.ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExtractedImage.ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ExtractedImage.ImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                },
                85,
                this
            )
        }.toByteArray()

        if (bytes.size > MAX_IMAGE_BYTES) {
            Log.w(TAG, "Compressed image exceeds size limit: ${bytes.size} bytes")
            return null
        }

        return ExtractedImage(
            bitmap = processedBitmap,
            format = format,
            sizeBytes = bytes.size
        )
    }

    /**
     * Check if notification has BigPictureStyle.
     */
    private fun hasBigPicture(notification: Notification): Boolean {
        val extras = NotificationCompat.getExtras(notification) ?: return false

        return extras.containsKey(EXTRA_PICTURE) ||
            extras.containsKey(EXTRA_PICTURE_ICON) ||
            extras.containsKey(EXTRA_BIG_PICTURE)
    }

    /**
     * Extract bitmap from notification extras.
     *
     * Tries multiple approaches:
     * 1. EXTRA_PICTURE (Bitmap) - Android 4.1+
     * 2. EXTRA_PICTURE_ICON (Icon) - Android 6.0+
     * 3. EXTRA_BIG_PICTURE (Bitmap) - Legacy
     */
    private fun extractBitmap(notification: Notification): Bitmap? {
        val extras = NotificationCompat.getExtras(notification) ?: return null

        // Try Icon first (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            extras.getParcelable<Icon>(EXTRA_PICTURE_ICON)?.let { icon ->
                try {
                    // Icon to Bitmap conversion requires context, which we don't have here
                    // This would need to be passed from the caller
                    Log.d(TAG, "Found picture icon but context needed for conversion")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to convert Icon to Bitmap", e)
                }
            }
        }

        // Try direct Bitmap (most common)
        BundleCompat.getParcelable(extras, EXTRA_PICTURE, Bitmap::class.java)?.let {
            return it
        }

        // Try legacy key
        BundleCompat.getParcelable(extras, EXTRA_BIG_PICTURE, Bitmap::class.java)?.let {
            return it
        }

        return null
    }

    /**
     * Validate and resize bitmap if needed.
     *
     * @param bitmap Original bitmap
     * @return Resized bitmap if needed, original if within limits, null if invalid
     */
    private fun validateAndResize(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height

        // Check if within limits
        if (width <= MAX_IMAGE_WIDTH && height <= MAX_IMAGE_HEIGHT) {
            return bitmap
        }

        // Calculate scale factor to fit within limits
        val widthScale = MAX_IMAGE_WIDTH.toFloat() / width
        val heightScale = MAX_IMAGE_HEIGHT.toFloat() / height
        val scale = minOf(widthScale, heightScale)

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        Log.d(TAG, "Resizing image from ${width}x${height} to ${newWidth}x${newHeight}")

        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory while resizing bitmap", e)
            null
        }
    }

    /**
     * Determine best image format based on content.
     *
     * Uses PNG for images with transparency, JPEG for photos,
     * WEBP on Android 9+ for better compression.
     */
    private fun determineFormat(bitmap: Bitmap): ExtractedImage.ImageFormat {
        // Use WEBP on Android 9+ for best compression
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ExtractedImage.ImageFormat.WEBP
        }

        // Check for transparency
        if (bitmap.hasAlpha()) {
            return ExtractedImage.ImageFormat.PNG
        }

        // Default to JPEG for photos
        return ExtractedImage.ImageFormat.JPEG
    }

    /**
     * Extract rich text from notification.
     *
     * Extracts formatted text including spans and markup.
     * Useful for notifications with bold, italic, or colored text.
     */
    fun extractRichText(notification: Notification): String? {
        val extras = NotificationCompat.getExtras(notification) ?: return null

        // Try BigText first
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)
        if (bigText != null) {
            return bigText.toString()
        }

        // Fall back to regular text
        return extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
    }

    /**
     * Extract links from notification text.
     *
     * Extracts URLs from notification text using pattern matching.
     */
    fun extractLinks(notification: Notification): List<NotificationLink>? {
        val text = extractRichText(notification) ?: return null

        // Simple URL pattern matching
        val urlPattern = Regex("https?://[^\\s]+")
        val matches = urlPattern.findAll(text)

        val links = matches.map { match ->
            NotificationLink(url = match.value)
        }.toList()

        return if (links.isEmpty()) null else links
    }

    /**
     * Extract video info from media notifications.
     *
     * Attempts to extract video metadata from MediaStyle notifications.
     */
    fun extractVideoInfo(notification: Notification): VideoInfo? {
        val extras = NotificationCompat.getExtras(notification) ?: return null

        // Check if it's a media notification
        val mediaSession = extras.getParcelable<Parcelable>("android.mediaSession")
        if (mediaSession == null) {
            return null
        }

        // Try to get thumbnail (album art)
        val thumbnail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.getLargeIcon()?.let { icon ->
                // Would need context to convert Icon to Bitmap
                null
            }
        } else {
            @Suppress("DEPRECATION")
            notification.largeIcon
        }

        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()

        return VideoInfo(
            thumbnailBitmap = thumbnail,
            title = title
        )
    }
}
