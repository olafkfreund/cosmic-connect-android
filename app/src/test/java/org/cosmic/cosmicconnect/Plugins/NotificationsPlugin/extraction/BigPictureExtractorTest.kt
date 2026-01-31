/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction

import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for BigPictureExtractor.
 *
 * Tests image extraction, scaling, compression, and hash generation.
 */
@RunWith(MockitoJUnitRunner::class)
class BigPictureExtractorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockStatusBarNotification: StatusBarNotification

    @Mock
    private lateinit var mockNotification: Notification

    private lateinit var extractor: BigPictureExtractor

    @Before
    fun setUp() {
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockContext.createPackageContext(anyString(), anyInt())).thenReturn(mockContext)

        // Setup notification mocks
        `when`(mockStatusBarNotification.notification).thenReturn(mockNotification)
        `when`(mockStatusBarNotification.packageName).thenReturn("com.test.app")

        extractor = BigPictureExtractor(mockContext)
    }

    @Test
    fun testScaleToBounds_NoScalingNeeded() {
        // Small image that fits within bounds
        val bitmap = createTestBitmap(200, 200)

        val scaled = extractor.scaleToBounds(bitmap, 400, 400)

        assertEquals(200, scaled.width)
        assertEquals(200, scaled.height)
        assertSame("Should return same bitmap when no scaling needed", bitmap, scaled)

        bitmap.recycle()
    }

    @Test
    fun testScaleToBounds_ScaleWidth() {
        // Wide image that exceeds max width
        val bitmap = createTestBitmap(800, 200)

        val scaled = extractor.scaleToBounds(bitmap, 400, 400)

        assertEquals(400, scaled.width)
        assertEquals(100, scaled.height) // Aspect ratio maintained
        assertNotSame("Should return new bitmap after scaling", bitmap, scaled)

        bitmap.recycle()
        scaled.recycle()
    }

    @Test
    fun testScaleToBounds_ScaleHeight() {
        // Tall image that exceeds max height
        val bitmap = createTestBitmap(200, 800)

        val scaled = extractor.scaleToBounds(bitmap, 400, 400)

        assertEquals(100, scaled.width) // Aspect ratio maintained
        assertEquals(400, scaled.height)
        assertNotSame("Should return new bitmap after scaling", bitmap, scaled)

        bitmap.recycle()
        scaled.recycle()
    }

    @Test
    fun testScaleToBounds_ScaleBoth() {
        // Large image that exceeds both dimensions
        val bitmap = createTestBitmap(1000, 800)

        val scaled = extractor.scaleToBounds(bitmap, 400, 400)

        assertTrue("Width should be <= 400", scaled.width <= 400)
        assertTrue("Height should be <= 400", scaled.height <= 400)

        // Check aspect ratio is maintained (within rounding)
        val originalRatio = 1000f / 800f
        val scaledRatio = scaled.width.toFloat() / scaled.height.toFloat()
        assertEquals(originalRatio, scaledRatio, 0.01f)

        bitmap.recycle()
        scaled.recycle()
    }

    @Test
    fun testCompressToBytes_ValidBitmap() {
        val bitmap = createTestBitmap(100, 100)

        val compressed = extractor.compressToBytes(bitmap, 85)

        assertTrue("Compressed data should not be empty", compressed.isNotEmpty())
        assertTrue("Compressed size should be reasonable", compressed.size < 100000)

        bitmap.recycle()
    }

    @Test
    fun testCompressToBytes_DifferentQuality() {
        val bitmap = createTestBitmap(200, 200)

        val highQuality = extractor.compressToBytes(bitmap, 95)
        val lowQuality = extractor.compressToBytes(bitmap, 50)

        assertTrue("High quality should produce larger file", highQuality.size > lowQuality.size)

        bitmap.recycle()
    }

    @Test
    fun testExtractBigPicture_NoBitmap() {
        // Notification with no extras
        `when`(mockNotification.extras).thenReturn(Bundle())

        val result = extractor.extractBigPicture(mockStatusBarNotification)

        assertNull("Should return null when no big picture", result)
    }

    @Test
    fun testExtractBigPicture_WithBitmap() {
        // Create test bitmap and add to notification extras
        val testBitmap = createTestBitmap(500, 500)
        val extras = Bundle().apply {
            putParcelable(Notification.EXTRA_PICTURE, testBitmap)
        }
        `when`(mockNotification.extras).thenReturn(extras)

        val result = extractor.extractBigPicture(mockStatusBarNotification)

        assertNotNull("Should extract big picture", result)
        result?.let {
            assertTrue("Width should be scaled to <= 400", it.width <= 400)
            assertTrue("Height should be scaled to <= 400", it.height <= 400)
            assertEquals("MIME type should be image/jpeg", "image/jpeg", it.mimeType)
            assertTrue("Should have compressed data", it.compressedData.isNotEmpty())
            assertTrue("Should have valid hash", it.hash.matches(Regex("[0-9a-f]{32}")))

            it.bitmap.recycle()
        }

        testBitmap.recycle()
    }

    @Test
    fun testExtractLargeIcon_NoBitmap() {
        // Notification with no large icon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            `when`(mockNotification.getLargeIcon()).thenReturn(null)
        } else {
            @Suppress("DEPRECATION")
            mockNotification.largeIcon = null
        }

        val result = extractor.extractLargeIcon(mockStatusBarNotification)

        assertNull("Should return null when no large icon", result)
    }

    @Test
    fun testExtractedImage_HashEquality() {
        val bitmap1 = createTestBitmap(100, 100)
        val bitmap2 = createTestBitmap(100, 100)

        val data = extractor.compressToBytes(bitmap1)

        val image1 = ExtractedImage(
            bitmap = bitmap1,
            width = 100,
            height = 100,
            mimeType = "image/jpeg",
            sizeBytes = data.size,
            hash = "abc123",
            compressedData = data
        )

        val image2 = ExtractedImage(
            bitmap = bitmap2,
            width = 100,
            height = 100,
            mimeType = "image/jpeg",
            sizeBytes = data.size,
            hash = "abc123",
            compressedData = data
        )

        assertEquals("Images with same hash should be equal", image1, image2)
        assertEquals("Hash codes should match", image1.hashCode(), image2.hashCode())

        bitmap1.recycle()
        bitmap2.recycle()
    }

    @Test
    fun testExtractedImage_HashInequality() {
        val bitmap = createTestBitmap(100, 100)
        val data = extractor.compressToBytes(bitmap)

        val image1 = ExtractedImage(
            bitmap = bitmap,
            width = 100,
            height = 100,
            mimeType = "image/jpeg",
            sizeBytes = data.size,
            hash = "abc123",
            compressedData = data
        )

        val image2 = ExtractedImage(
            bitmap = bitmap,
            width = 100,
            height = 100,
            mimeType = "image/jpeg",
            sizeBytes = data.size,
            hash = "def456",
            compressedData = data
        )

        assertNotEquals("Images with different hashes should not be equal", image1, image2)

        bitmap.recycle()
    }

    @Test
    fun testScaling_PreservesAspectRatio() {
        val testCases = listOf(
            Pair(1000, 500),  // 2:1 landscape
            Pair(500, 1000),  // 1:2 portrait
            Pair(800, 600),   // 4:3 landscape
            Pair(600, 800)    // 3:4 portrait
        )

        for ((width, height) in testCases) {
            val bitmap = createTestBitmap(width, height)
            val originalRatio = width.toFloat() / height.toFloat()

            val scaled = extractor.scaleToBounds(bitmap, 400, 400)
            val scaledRatio = scaled.width.toFloat() / scaled.height.toFloat()

            assertEquals(
                "Aspect ratio should be preserved for ${width}x${height}",
                originalRatio,
                scaledRatio,
                0.01f
            )

            bitmap.recycle()
            if (scaled != bitmap) {
                scaled.recycle()
            }
        }
    }

    @Test
    fun testCompression_ProducesConsistentHash() {
        val bitmap = createTestBitmap(200, 200)

        val compressed1 = extractor.compressToBytes(bitmap, 85)
        val compressed2 = extractor.compressToBytes(bitmap, 85)

        assertArrayEquals(
            "Same bitmap compressed with same quality should produce identical bytes",
            compressed1,
            compressed2
        )

        bitmap.recycle()
    }

    /**
     * Helper to create a test bitmap.
     *
     * Creates a simple solid color bitmap for testing.
     */
    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}
