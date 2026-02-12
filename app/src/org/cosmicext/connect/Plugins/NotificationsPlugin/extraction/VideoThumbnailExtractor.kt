/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.NotificationsPlugin.extraction

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Data class representing extracted video information from a notification.
 *
 * @property thumbnailBitmap The video thumbnail as a Bitmap (nullable)
 * @property videoUrl The URL of the video if available (nullable)
 * @property duration Video duration in milliseconds (nullable)
 * @property mimeType MIME type of the video (nullable, e.g., "video/mp4")
 * @property title Video title or description (nullable)
 */
data class VideoInfo(
    val thumbnailBitmap: Bitmap? = null,
    val videoUrl: String? = null,
    val duration: Long? = null,
    val mimeType: String? = null,
    val title: String? = null
)

/**
 * Extractor for video thumbnails and metadata from notifications.
 *
 * Supports:
 * - YouTube, Netflix, TikTok, and other video apps
 * - MessagingStyle notifications with video attachments
 * - MediaSession-based video players
 * - Direct thumbnail extraction from notification extras
 * - Thumbnail generation from video URLs using MediaMetadataRetriever
 *
 * ## Usage
 * ```kotlin
 * val extractor = VideoThumbnailExtractor(context)
 * val videoInfo = extractor.extractVideoInfo(notification)
 * if (videoInfo != null) {
 *     // Use videoInfo.thumbnailBitmap, videoInfo.duration, etc.
 * }
 * ```
 */
class VideoThumbnailExtractor(private val context: Context) {

    companion object {
        private const val TAG = "COSMIC/VideoThumbnailExtractor"

        // Known video app package names
        private const val PKG_YOUTUBE = "com.google.android.youtube"
        private const val PKG_NETFLIX = "com.netflix.mediaclient"
        private const val PKG_TIKTOK = "com.zhiliaoapp.musically"
        private const val PKG_VLC = "org.videolan.vlc"
        private const val PKG_MX_PLAYER = "com.mxtech.videoplayer.ad"
        private const val PKG_PRIME_VIDEO = "com.amazon.avod.thirdpartyclient"
        private const val PKG_DISNEY_PLUS = "com.disney.disneyplus"

        // Notification extras keys for video content
        private const val EXTRA_MEDIA_SESSION = "android.mediaSession"
        private const val EXTRA_LARGE_ICON = "android.largeIcon"
        private const val EXTRA_LARGE_ICON_BIG = "android.largeIcon.big"
        private const val EXTRA_PICTURE = "android.picture"
        private const val EXTRA_TEXT = "android.text"
        private const val EXTRA_TITLE = "android.title"
        private const val EXTRA_SUB_TEXT = "android.subText"

        // MessagingStyle video attachment keys
        private const val EXTRA_MESSAGES = "android.messages"
        private const val KEY_DATA_MIME_TYPE = "type"
        private const val KEY_DATA_URI = "uri"

        // Video MIME type patterns
        private val VIDEO_MIME_PATTERNS = listOf(
            "video/",
            "application/vnd.android.video"
        )

        // Thumbnail generation settings
        private const val THUMBNAIL_MAX_WIDTH = 512
        private const val THUMBNAIL_MAX_HEIGHT = 512
        private const val THUMBNAIL_QUALITY = 85
        private const val URL_FETCH_TIMEOUT_MS = 5000
    }

    /**
     * Detect if a notification is video-related.
     *
     * Checks:
     * - Known video app packages
     * - MediaSession presence (video players)
     * - MessagingStyle with video attachments
     * - Video-specific notification categories
     *
     * @param notification The notification to check
     * @return true if notification is video-related
     */
    fun detectVideoNotification(notification: StatusBarNotification): Boolean {
        val packageName = notification.packageName

        // Check known video apps
        if (isKnownVideoApp(packageName)) {
            return true
        }

        val extras = NotificationCompat.getExtras(notification.notification) ?: return false

        // Check for MediaSession (video players)
        if (extras.containsKey(EXTRA_MEDIA_SESSION)) {
            return true
        }

        // Check MessagingStyle for video attachments
        if (hasVideoMessagingAttachment(extras)) {
            return true
        }

        // Check notification category
        val category = notification.notification.category
        if (category == Notification.CATEGORY_TRANSPORT ||
            category == Notification.CATEGORY_RECOMMENDATION) {
            // Could be video, check more carefully
            return containsVideoIndicators(extras)
        }

        return false
    }

    /**
     * Extract video information and thumbnail from a notification.
     *
     * Attempts multiple extraction strategies:
     * 1. Direct thumbnail from notification extras
     * 2. Large icon extraction
     * 3. MessagingStyle video attachment
     * 4. URL-based thumbnail generation
     * 5. MediaSession metadata
     *
     * @param notification The notification to extract from
     * @return VideoInfo object if video content found, null otherwise
     */
    fun extractVideoInfo(notification: StatusBarNotification): VideoInfo? {
        if (!detectVideoNotification(notification)) {
            return null
        }

        val extras = NotificationCompat.getExtras(notification.notification) ?: return null
        val packageName = notification.packageName

        var thumbnailBitmap: Bitmap? = null
        var videoUrl: String? = null
        var duration: Long? = null
        var mimeType: String? = null
        var title: String? = null

        // Extract title
        title = extractTitle(extras)

        // Strategy 1: Direct picture/large icon extraction
        thumbnailBitmap = extractDirectThumbnail(extras)

        // Strategy 2: MessagingStyle video attachment
        if (thumbnailBitmap == null) {
            val messagingVideoInfo = extractMessagingStyleVideo(extras)
            if (messagingVideoInfo != null) {
                thumbnailBitmap = messagingVideoInfo.thumbnailBitmap
                videoUrl = messagingVideoInfo.videoUrl
                mimeType = messagingVideoInfo.mimeType
            }
        }

        // Strategy 3: App-specific extraction
        if (thumbnailBitmap == null || videoUrl == null) {
            val appSpecificInfo = extractAppSpecificVideo(packageName, extras)
            if (appSpecificInfo != null) {
                thumbnailBitmap = thumbnailBitmap ?: appSpecificInfo.thumbnailBitmap
                videoUrl = videoUrl ?: appSpecificInfo.videoUrl
                duration = appSpecificInfo.duration
                mimeType = mimeType ?: appSpecificInfo.mimeType
            }
        }

        // Strategy 4: Generate thumbnail from URL if available
        if (thumbnailBitmap == null && videoUrl != null) {
            thumbnailBitmap = extractThumbnailFromUrl(videoUrl)
        }

        // Return null if we couldn't extract any useful information
        if (thumbnailBitmap == null && videoUrl == null) {
            return null
        }

        return VideoInfo(
            thumbnailBitmap = thumbnailBitmap,
            videoUrl = videoUrl,
            duration = duration,
            mimeType = mimeType,
            title = title
        )
    }

    /**
     * Extract thumbnail from a video URL.
     *
     * Uses MediaMetadataRetriever to generate a thumbnail from the video.
     * Supports both local and remote URLs.
     *
     * @param videoUrl The video URL (file://, content://, http://, or https://)
     * @return Bitmap thumbnail if successful, null otherwise
     */
    fun extractThumbnailFromUrl(videoUrl: String): Bitmap? {
        if (videoUrl.isBlank()) {
            return null
        }

        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()

            when {
                videoUrl.startsWith("http://") || videoUrl.startsWith("https://") -> {
                    // Remote URL - use with timeout
                    retriever.setDataSource(videoUrl, hashMapOf())
                }
                videoUrl.startsWith("content://") || videoUrl.startsWith("file://") -> {
                    // Local content URI or file
                    retriever.setDataSource(context, Uri.parse(videoUrl))
                }
                else -> {
                    // Assume file path
                    retriever.setDataSource(videoUrl)
                }
            }

            // Get frame at 1 second (or first frame if video is shorter)
            val timeUs = 1_000_000L // 1 second in microseconds
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (frame != null) {
                // Scale down if needed
                return scaleBitmap(frame, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT)
            }

            Log.w(TAG, "Failed to extract frame from video URL: $videoUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting thumbnail from URL: $videoUrl", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }

        return null
    }

    // Private helper methods

    /**
     * Check if package is a known video app.
     */
    private fun isKnownVideoApp(packageName: String): Boolean {
        return packageName == PKG_YOUTUBE ||
               packageName == PKG_NETFLIX ||
               packageName == PKG_TIKTOK ||
               packageName == PKG_VLC ||
               packageName == PKG_MX_PLAYER ||
               packageName == PKG_PRIME_VIDEO ||
               packageName == PKG_DISNEY_PLUS
    }

    /**
     * Check if extras contain MessagingStyle with video attachments.
     */
    private fun hasVideoMessagingAttachment(extras: Bundle): Boolean {
        if (!extras.containsKey(EXTRA_MESSAGES)) {
            return false
        }

        val messages = extras.getParcelableArray(EXTRA_MESSAGES) ?: return false

        for (message in messages) {
            val msgBundle = message as? Bundle ?: continue
            val dataUri = msgBundle.getString(KEY_DATA_URI)
            val dataMimeType = msgBundle.getString(KEY_DATA_MIME_TYPE)

            if (dataMimeType != null && isVideoMimeType(dataMimeType)) {
                return true
            }
        }

        return false
    }

    /**
     * Check if extras contain video indicators (keywords, etc.).
     */
    private fun containsVideoIndicators(extras: Bundle): Boolean {
        val text = extras.getCharSequence(EXTRA_TEXT)?.toString()?.lowercase() ?: ""
        val title = extras.getCharSequence(EXTRA_TITLE)?.toString()?.lowercase() ?: ""

        val videoKeywords = listOf("video", "playing", "watching", "stream", "movie", "film")

        return videoKeywords.any { text.contains(it) || title.contains(it) }
    }

    /**
     * Check if MIME type is video-related.
     */
    private fun isVideoMimeType(mimeType: String): Boolean {
        return VIDEO_MIME_PATTERNS.any { mimeType.startsWith(it) }
    }

    /**
     * Extract title from notification extras.
     */
    private fun extractTitle(extras: Bundle): String? {
        return extras.getCharSequence(EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(EXTRA_SUB_TEXT)?.toString()
    }

    /**
     * Extract thumbnail directly from notification extras (picture or large icon).
     */
    private fun extractDirectThumbnail(extras: Bundle): Bitmap? {
        // Try android.picture first (BigPictureStyle)
        val picture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            extras.getParcelable(EXTRA_PICTURE, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(EXTRA_PICTURE)
        }

        if (picture != null) {
            return scaleBitmap(picture, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT)
        }

        // Try android.largeIcon.big
        val largeIconBig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            extras.getParcelable(EXTRA_LARGE_ICON_BIG, Icon::class.java)
        } else {
            null
        }

        if (largeIconBig != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bitmap = largeIconBig.loadDrawable(context)?.let { drawable ->
                drawableToBitmap(drawable)
            }
            if (bitmap != null) {
                return scaleBitmap(bitmap, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT)
            }
        }

        // Try android.largeIcon
        val largeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            extras.getParcelable(EXTRA_LARGE_ICON, Icon::class.java)
        } else {
            null
        }

        if (largeIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bitmap = largeIcon.loadDrawable(context)?.let { drawable ->
                drawableToBitmap(drawable)
            }
            if (bitmap != null) {
                return scaleBitmap(bitmap, THUMBNAIL_MAX_WIDTH, THUMBNAIL_MAX_HEIGHT)
            }
        }

        return null
    }

    /**
     * Extract video from MessagingStyle notification.
     */
    private fun extractMessagingStyleVideo(extras: Bundle): VideoInfo? {
        if (!extras.containsKey(EXTRA_MESSAGES)) {
            return null
        }

        val messages = extras.getParcelableArray(EXTRA_MESSAGES) ?: return null

        // Look for most recent video message
        for (i in messages.indices.reversed()) {
            val message = messages[i] as? Bundle ?: continue
            val dataUri = message.getString(KEY_DATA_URI)
            val dataMimeType = message.getString(KEY_DATA_MIME_TYPE)

            if (dataMimeType != null && isVideoMimeType(dataMimeType) && dataUri != null) {
                val thumbnail = extractThumbnailFromUrl(dataUri)

                return VideoInfo(
                    thumbnailBitmap = thumbnail,
                    videoUrl = dataUri,
                    mimeType = dataMimeType
                )
            }
        }

        return null
    }

    /**
     * Extract app-specific video information.
     */
    private fun extractAppSpecificVideo(packageName: String, extras: Bundle): VideoInfo? {
        return when (packageName) {
            PKG_YOUTUBE -> extractYouTubeVideo(extras)
            PKG_NETFLIX -> extractNetflixVideo(extras)
            PKG_TIKTOK -> extractTikTokVideo(extras)
            else -> null
        }
    }

    /**
     * Extract YouTube video information.
     */
    private fun extractYouTubeVideo(extras: Bundle): VideoInfo? {
        // YouTube typically includes thumbnail in large icon
        val thumbnail = extractDirectThumbnail(extras)

        // Try to extract video URL from text (if present)
        val text = extras.getCharSequence(EXTRA_TEXT)?.toString()
        val videoUrl = extractUrlFromText(text)

        return if (thumbnail != null || videoUrl != null) {
            VideoInfo(
                thumbnailBitmap = thumbnail,
                videoUrl = videoUrl,
                mimeType = "video/youtube"
            )
        } else {
            null
        }
    }

    /**
     * Extract Netflix video information.
     */
    private fun extractNetflixVideo(extras: Bundle): VideoInfo? {
        // Netflix uses large icon for thumbnails
        val thumbnail = extractDirectThumbnail(extras)

        return if (thumbnail != null) {
            VideoInfo(
                thumbnailBitmap = thumbnail,
                mimeType = "video/netflix"
            )
        } else {
            null
        }
    }

    /**
     * Extract TikTok video information.
     */
    private fun extractTikTokVideo(extras: Bundle): VideoInfo? {
        // TikTok uses large icon for thumbnails
        val thumbnail = extractDirectThumbnail(extras)

        return if (thumbnail != null) {
            VideoInfo(
                thumbnailBitmap = thumbnail,
                mimeType = "video/tiktok"
            )
        } else {
            null
        }
    }

    /**
     * Extract URL from text string (simple pattern matching).
     */
    private fun extractUrlFromText(text: String?): String? {
        if (text.isNullOrBlank()) {
            return null
        }

        // Simple URL regex
        val urlPattern = Regex("""https?://[^\s]+""")
        val match = urlPattern.find(text)
        return match?.value
    }

    /**
     * Convert Drawable to Bitmap.
     */
    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )

        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    /**
     * Scale bitmap to fit within max dimensions while maintaining aspect ratio.
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val aspectRatio = width.toFloat() / height.toFloat()

        val (newWidth, newHeight) = if (width > height) {
            maxWidth to (maxWidth / aspectRatio).toInt()
        } else {
            (maxHeight * aspectRatio).toInt() to maxHeight
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
