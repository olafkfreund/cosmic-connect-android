/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Result of image extraction from notification.
 *
 * @property bitmap The extracted and scaled bitmap
 * @property width Final width in pixels
 * @property height Final height in pixels
 * @property mimeType MIME type (always "image/jpeg" for compressed images)
 * @property sizeBytes Size of compressed JPEG data
 * @property hash MD5 hash for deduplication
 * @property compressedData Compressed JPEG bytes ready for network transfer
 */
data class ExtractedImage(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val sizeBytes: Int,
    val hash: String,
    val compressedData: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExtractedImage

        if (hash != other.hash) return false
        return true
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }
}

/**
 * Extracts and processes BigPictureStyle images from notifications.
 *
 * Handles extraction from EXTRA_PICTURE and EXTRA_LARGE_ICON, scales to
 * reasonable dimensions for network transfer, compresses as JPEG, and
 * generates MD5 hashes for deduplication.
 *
 * ## Memory Management
 * - Automatically recycles intermediate bitmaps
 * - Scales images before compression to reduce memory pressure
 * - Uses try-finally blocks to ensure cleanup
 *
 * ## Usage
 * ```kotlin
 * val extractor = BigPictureExtractor(context)
 * val image = extractor.extractBigPicture(statusBarNotification)
 * if (image != null) {
 *     // Send image.compressedData with hash image.hash
 *     image.bitmap.recycle()
 * }
 * ```
 */
class BigPictureExtractor(private val context: Context) {

    companion object {
        private const val TAG = "COSMIC/BigPictureExtractor"

        // Maximum dimensions for transferred images
        private const val MAX_WIDTH = 400
        private const val MAX_HEIGHT = 400

        // JPEG compression quality (1-100, higher = better quality)
        private const val JPEG_QUALITY = 85
    }

    /**
     * Extract BigPictureStyle image from notification.
     *
     * Checks for EXTRA_PICTURE in notification extras, which contains
     * the large image shown in expanded BigPictureStyle notifications.
     *
     * @param statusBarNotification The notification to extract from
     * @return ExtractedImage with scaled and compressed bitmap, or null if none found
     */
    fun extractBigPicture(statusBarNotification: StatusBarNotification): ExtractedImage? {
        val notification = statusBarNotification.notification
        val extras = notification.extras ?: return null

        // BigPictureStyle stores the picture in EXTRA_PICTURE
        if (!extras.containsKey(Notification.EXTRA_PICTURE)) {
            return null
        }

        val bitmap = try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // On M+, EXTRA_PICTURE can be an Icon
                    val picture = extras.get(Notification.EXTRA_PICTURE)
                    when (picture) {
                        is Bitmap -> picture
                        is Icon -> iconToBitmap(statusBarNotification.packageName, picture)
                        else -> {
                            Log.w(TAG, "EXTRA_PICTURE is unexpected type: ${picture?.javaClass?.simpleName}")
                            null
                        }
                    }
                }
                else -> {
                    // On older APIs, EXTRA_PICTURE is always a Bitmap
                    @Suppress("DEPRECATION")
                    extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting big picture", e)
            null
        } ?: return null

        return processAndCompress(bitmap, shouldRecycleSource = false)
    }

    /**
     * Extract large icon from notification.
     *
     * Falls back to large icon if BigPictureStyle is not available.
     * Large icons are typically used as contact photos in messaging apps.
     *
     * @param statusBarNotification The notification to extract from
     * @return ExtractedImage with scaled and compressed bitmap, or null if none found
     */
    fun extractLargeIcon(statusBarNotification: StatusBarNotification): ExtractedImage? {
        val notification = statusBarNotification.notification

        val bitmap = try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    // On M+, use getLargeIcon() which returns an Icon
                    notification.getLargeIcon()?.let { icon ->
                        iconToBitmap(statusBarNotification.packageName, icon)
                    }
                }
                else -> {
                    // On older APIs, largeIcon is a Bitmap
                    @Suppress("DEPRECATION")
                    notification.largeIcon
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting large icon", e)
            null
        } ?: return null

        return processAndCompress(bitmap, shouldRecycleSource = false)
    }

    /**
     * Scale bitmap to fit within maximum dimensions.
     *
     * Maintains aspect ratio while ensuring neither dimension exceeds
     * the specified maximums. No-op if bitmap is already smaller.
     *
     * @param bitmap Source bitmap to scale
     * @param maxWidth Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @return Scaled bitmap (may be the same instance if no scaling needed)
     */
    fun scaleToBounds(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // No scaling needed
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // Calculate scale factor maintaining aspect ratio
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        Log.d(TAG, "Scaling image from ${width}x${height} to ${newWidth}x${newHeight}")

        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM while scaling bitmap", e)
            bitmap // Return original on OOM
        }
    }

    /**
     * Compress bitmap to JPEG byte array.
     *
     * Uses specified quality level (1-100). Higher quality produces
     * larger files but better image fidelity.
     *
     * @param bitmap Bitmap to compress
     * @param quality JPEG quality (1-100), defaults to 85
     * @return Compressed JPEG data as byte array
     */
    fun compressToBytes(bitmap: Bitmap, quality: Int = JPEG_QUALITY): ByteArray {
        val outputStream = ByteArrayOutputStream()

        return try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val data = outputStream.toByteArray()

            Log.d(TAG, "Compressed ${bitmap.width}x${bitmap.height} image to ${data.size} bytes (quality=$quality)")

            data
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing bitmap", e)
            ByteArray(0)
        } finally {
            outputStream.close()
        }
    }

    /**
     * Convert Icon to Bitmap.
     *
     * Loads the Icon's drawable and renders it to a Bitmap.
     *
     * @param packageName Package name for context creation
     * @param icon Icon to convert
     * @return Bitmap representation of the icon, or null on error
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun iconToBitmap(packageName: String, icon: Icon): Bitmap? {
        return try {
            val foreignContext = context.createPackageContext(packageName, 0)
            val drawable = icon.loadDrawable(foreignContext) ?: return null

            // Create bitmap from drawable
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting Icon to Bitmap", e)
            null
        }
    }

    /**
     * Process bitmap: scale, compress, and generate hash.
     *
     * This is the main processing pipeline that takes a raw bitmap
     * and produces an ExtractedImage ready for network transfer.
     *
     * @param sourceBitmap The bitmap to process
     * @param shouldRecycleSource Whether to recycle the source bitmap after processing
     * @return ExtractedImage with all metadata, or null on error
     */
    private fun processAndCompress(
        sourceBitmap: Bitmap,
        shouldRecycleSource: Boolean
    ): ExtractedImage? {
        var scaledBitmap: Bitmap? = null

        try {
            // Step 1: Scale to bounds
            scaledBitmap = scaleToBounds(sourceBitmap, MAX_WIDTH, MAX_HEIGHT)

            // Step 2: Compress to JPEG
            val compressedData = compressToBytes(scaledBitmap, JPEG_QUALITY)

            if (compressedData.isEmpty()) {
                return null
            }

            // Step 3: Calculate MD5 hash for deduplication
            val hash = calculateMD5(compressedData) ?: return null

            // Step 4: Create result
            return ExtractedImage(
                bitmap = scaledBitmap,
                width = scaledBitmap.width,
                height = scaledBitmap.height,
                mimeType = "image/jpeg",
                sizeBytes = compressedData.size,
                hash = hash,
                compressedData = compressedData
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing bitmap", e)

            // Clean up on error
            if (scaledBitmap != null && scaledBitmap != sourceBitmap) {
                scaledBitmap.recycle()
            }

            return null
        } finally {
            // Clean up source if requested and it's not the scaled bitmap
            if (shouldRecycleSource && sourceBitmap != scaledBitmap) {
                sourceBitmap.recycle()
            }
        }
    }

    /**
     * Calculate MD5 hash of byte array.
     *
     * Used for image deduplication - identical images produce
     * identical hashes, allowing desktop to skip duplicate transfers.
     *
     * @param data Data to hash
     * @return Lowercase hex string of MD5 hash, or null on error
     */
    private fun calculateMD5(data: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(data)
            val digest = md.digest()

            // Convert to hex string
            digest.joinToString("") { byte ->
                "%02x".format(byte)
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "MD5 algorithm not available", e)
            null
        }
    }
}
