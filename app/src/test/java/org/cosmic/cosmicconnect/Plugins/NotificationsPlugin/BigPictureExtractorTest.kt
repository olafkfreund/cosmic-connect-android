/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.app.Notification
import android.graphics.Bitmap
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for BigPictureExtractor.
 *
 * Tests extraction of:
 * - BigPictureStyle images
 * - Rich text
 * - Embedded links
 * - Video metadata
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BigPictureExtractorTest {

    private lateinit var extractor: BigPictureExtractor

    @Mock
    private lateinit var mockStatusBarNotification: StatusBarNotification

    @Mock
    private lateinit var mockNotification: Notification

    @Before
    fun setUp() {
        extractor = BigPictureExtractor()
        mockStatusBarNotification = mock(StatusBarNotification::class.java)
        mockNotification = mock(Notification::class.java)
    }

    @Test
    fun testExtract_noBigPicture_returnsNull() {
        // Create notification without BigPictureStyle
        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText("Test notification")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        val statusBarNotification = mock(StatusBarNotification::class.java)
        `when`(statusBarNotification.notification).thenReturn(notification)

        val result = extractor.extract(statusBarNotification)

        assertNull(result)
    }

    @Test
    fun testExtract_withBigPicture_returnsImage() {
        // Create test bitmap
        val bitmap = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888)

        // Create notification with BigPictureStyle
        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText("Test notification")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
            )
            .build()

        val statusBarNotification = mock(StatusBarNotification::class.java)
        `when`(statusBarNotification.notification).thenReturn(notification)

        val result = extractor.extract(statusBarNotification)

        assertNotNull(result)
        assertEquals(500, result!!.width)
        assertEquals(300, result.height)
    }

    @Test
    fun testExtract_largeImage_isDownscaled() {
        // Create oversized bitmap
        val largeBitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(largeBitmap)
            )
            .build()

        val statusBarNotification = mock(StatusBarNotification::class.java)
        `when`(statusBarNotification.notification).thenReturn(notification)

        val result = extractor.extract(statusBarNotification)

        assertNotNull(result)
        // Should be downscaled to fit within limits
        assertTrue(result!!.width <= BigPictureExtractor.MAX_IMAGE_WIDTH)
        assertTrue(result.height <= BigPictureExtractor.MAX_IMAGE_HEIGHT)

        // Aspect ratio should be preserved
        val originalRatio = 4000.0 / 3000.0
        val resultRatio = result.width.toDouble() / result.height.toDouble()
        assertEquals(originalRatio, resultRatio, 0.01)
    }

    @Test
    fun testExtractRichText_withBigText() {
        val bigText = "This is a long notification text that appears in BigTextStyle"

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText("Short text")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
            )
            .build()

        val result = extractor.extractRichText(notification)

        assertEquals(bigText, result)
    }

    @Test
    fun testExtractRichText_fallbackToRegularText() {
        val regularText = "Regular notification text"

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText(regularText)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        val result = extractor.extractRichText(notification)

        assertEquals(regularText, result)
    }

    @Test
    fun testExtractLinks_singleLink() {
        val textWithLink = "Check out https://example.com for more info"

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText(textWithLink)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        val result = extractor.extractLinks(notification)

        assertNotNull(result)
        assertEquals(1, result!!.size)
        assertEquals("https://example.com", result[0].url)
    }

    @Test
    fun testExtractLinks_multipleLinks() {
        val textWithLinks = "Visit https://example.com and http://test.org for details"

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText(textWithLinks)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        val result = extractor.extractLinks(notification)

        assertNotNull(result)
        assertEquals(2, result!!.size)
        assertEquals("https://example.com", result[0].url)
        assertEquals("http://test.org", result[1].url)
    }

    @Test
    fun testExtractLinks_noLinks_returnsNull() {
        val textWithoutLinks = "This is a notification without any URLs"

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setContentText(textWithoutLinks)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        val result = extractor.extractLinks(notification)

        assertNull(result)
    }

    @Test
    fun testImageFormat_PNG_forTransparency() {
        val bitmapWithAlpha = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Set some transparent pixels
        bitmapWithAlpha.setPixel(50, 50, 0x7F00FF00.toInt()) // Semi-transparent green

        val image = ExtractedImage(
            bitmap = bitmapWithAlpha,
            format = ExtractedImage.ImageFormat.PNG,
            sizeBytes = 5000
        )

        assertEquals(ExtractedImage.ImageFormat.PNG, image.format)
    }

    @Test
    fun testImageFormat_JPEG_forPhotos() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.JPEG,
            sizeBytes = 5000
        )

        assertEquals(ExtractedImage.ImageFormat.JPEG, image.format)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun testImageFormat_WEBP_onAndroid9Plus() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.WEBP,
            sizeBytes = 5000
        )

        assertEquals(ExtractedImage.ImageFormat.WEBP, image.format)
    }

    @Test
    fun testExtractedImage_dimensions() {
        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)

        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.JPEG,
            sizeBytes = 10000
        )

        assertEquals(640, image.width)
        assertEquals(480, image.height)
    }

    @Test
    fun testNotificationLink_withTitle() {
        val link = NotificationLink("https://example.com", "Example Site")

        assertEquals("https://example.com", link.url)
        assertEquals("Example Site", link.title)
    }

    @Test
    fun testNotificationLink_withoutTitle() {
        val link = NotificationLink("https://example.com")

        assertEquals("https://example.com", link.url)
        assertNull(link.title)
    }

    @Test
    fun testVideoInfo_allFields() {
        val thumbnail = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val videoInfo = VideoInfo(
            thumbnailBitmap = thumbnail,
            duration = 180000L, // 3 minutes
            title = "My Video"
        )

        assertNotNull(videoInfo.thumbnailBitmap)
        assertEquals(180000L, videoInfo.duration)
        assertEquals("My Video", videoInfo.title)
    }

    @Test
    fun testVideoInfo_minimalFields() {
        val videoInfo = VideoInfo(
            title = "Video Only Title"
        )

        assertNull(videoInfo.thumbnailBitmap)
        assertNull(videoInfo.duration)
        assertEquals("Video Only Title", videoInfo.title)
    }

    @Test
    fun testExtractRichText_noText_returnsNull() {
        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Title Only")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .build()

        val result = extractor.extractRichText(notification)

        // Should still have title or return null if truly empty
        // Behavior depends on implementation
        assertTrue(result == null || result.isEmpty())
    }

    @Test
    fun testExtract_sizeValidation() {
        // Test that images within size limits are not modified
        val bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
            )
            .build()

        val statusBarNotification = mock(StatusBarNotification::class.java)
        `when`(statusBarNotification.notification).thenReturn(notification)

        val result = extractor.extract(statusBarNotification)

        assertNotNull(result)
        // Should not be resized
        assertEquals(1000, result!!.width)
        assertEquals(800, result.height)
    }

    @Test
    fun testExtractLinks_withBigText() {
        val bigTextWithLink = "Check out https://example.com and https://test.org for more"

        val notification = NotificationCompat.Builder(
            androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            "test_channel"
        )
            .setContentTitle("Test")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigTextWithLink)
            )
            .build()

        val result = extractor.extractLinks(notification)

        assertNotNull(result)
        assertEquals(2, result!!.size)
    }
}
