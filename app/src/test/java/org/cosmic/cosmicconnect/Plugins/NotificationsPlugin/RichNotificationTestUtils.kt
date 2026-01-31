/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import org.mockito.Mockito
import java.io.ByteArrayOutputStream
import kotlin.math.abs

/**
 * Test utilities for Rich Notification testing.
 *
 * Provides mock notification builders and image comparison utilities.
 */
object RichNotificationTestUtils {

  /**
   * Create a mock BigPictureStyle notification.
   *
   * @param packageName Package name of notification sender
   * @param title Notification title
   * @param text Notification text
   * @param bitmap Big picture bitmap (optional, creates test image if null)
   * @return Mock StatusBarNotification with big picture
   */
  fun createMockBigPictureNotification(
    packageName: String = "com.test.app",
    title: String = "Photo Notification",
    text: String = "You have a new photo",
    bitmap: Bitmap? = null
  ): StatusBarNotification {
    val mockSbn = Mockito.mock(StatusBarNotification::class.java)
    val mockNotification = Mockito.mock(Notification::class.java)

    val bigPicture = bitmap ?: createTestBitmap(500, 500, Color.BLUE)

    val extras = Bundle().apply {
      putString(Notification.EXTRA_TITLE, title)
      putString(Notification.EXTRA_TEXT, text)
      putParcelable(Notification.EXTRA_PICTURE, bigPicture)
      putString(Notification.EXTRA_TEMPLATE, "android.app.Notification\$BigPictureStyle")
    }

    Mockito.`when`(mockSbn.packageName).thenReturn(packageName)
    Mockito.`when`(mockSbn.notification).thenReturn(mockNotification)
    Mockito.`when`(mockNotification.extras).thenReturn(extras)

    return mockSbn
  }

  /**
   * Create a mock notification with rich text formatting.
   *
   * @param packageName Package name
   * @param title Notification title
   * @param text Rich text with formatting (bold, italic, etc.)
   * @return Mock StatusBarNotification with formatted text
   */
  fun createMockRichTextNotification(
    packageName: String = "com.test.messenger",
    title: String = "Message",
    text: CharSequence = createRichText("Hello *bold* text")
  ): StatusBarNotification {
    val mockSbn = Mockito.mock(StatusBarNotification::class.java)
    val mockNotification = Mockito.mock(Notification::class.java)

    val extras = Bundle().apply {
      putString(Notification.EXTRA_TITLE, title)
      putCharSequence(Notification.EXTRA_TEXT, text)
    }

    Mockito.`when`(mockSbn.packageName).thenReturn(packageName)
    Mockito.`when`(mockSbn.notification).thenReturn(mockNotification)
    Mockito.`when`(mockNotification.extras).thenReturn(extras)

    return mockSbn
  }

  /**
   * Create a mock notification with video thumbnail.
   *
   * Note: Actual video thumbnail extraction requires MediaMetadataRetriever
   * which is difficult to mock. This creates a notification that would
   * trigger video thumbnail extraction in production.
   *
   * @param packageName Package name
   * @param videoUri URI to video file
   * @return Mock StatusBarNotification indicating video content
   */
  fun createMockVideoNotification(
    packageName: String = "com.test.video",
    videoUri: String = "content://media/external/video/1"
  ): StatusBarNotification {
    val mockSbn = Mockito.mock(StatusBarNotification::class.java)
    val mockNotification = Mockito.mock(Notification::class.java)

    val extras = Bundle().apply {
      putString(Notification.EXTRA_TITLE, "Video Message")
      putString(Notification.EXTRA_TEXT, "Sent you a video")
      // In real notifications, video URI might be in extras or actions
      putString("android.media.extra.URI", videoUri)
    }

    Mockito.`when`(mockSbn.packageName).thenReturn(packageName)
    Mockito.`when`(mockSbn.notification).thenReturn(mockNotification)
    Mockito.`when`(mockNotification.extras).thenReturn(extras)

    return mockSbn
  }

  /**
   * Create a mock notification with embedded links.
   *
   * @param packageName Package name
   * @param title Notification title
   * @param urls URLs to embed in text
   * @return Mock StatusBarNotification with URLs
   */
  fun createMockNotificationWithLinks(
    packageName: String = "com.test.browser",
    title: String = "Shared Link",
    vararg urls: String = arrayOf("https://example.com", "https://another.com")
  ): StatusBarNotification {
    val mockSbn = Mockito.mock(StatusBarNotification::class.java)
    val mockNotification = Mockito.mock(Notification::class.java)

    val text = buildString {
      append("Check out these links: ")
      urls.forEachIndexed { index, url ->
        append(url)
        if (index < urls.size - 1) append(", ")
      }
    }

    // Create SpannableString with URLSpans
    val spannable = SpannableString(text)
    var currentIndex = 0
    for (url in urls) {
      val start = text.indexOf(url, currentIndex)
      if (start >= 0) {
        val end = start + url.length
        spannable.setSpan(
          URLSpan(url),
          start,
          end,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        currentIndex = end
      }
    }

    val extras = Bundle().apply {
      putString(Notification.EXTRA_TITLE, title)
      putCharSequence(Notification.EXTRA_TEXT, spannable)
    }

    Mockito.`when`(mockSbn.packageName).thenReturn(packageName)
    Mockito.`when`(mockSbn.notification).thenReturn(mockNotification)
    Mockito.`when`(mockNotification.extras).thenReturn(extras)

    return mockSbn
  }

  /**
   * Create a SpannableString with rich text formatting.
   *
   * Simple markdown-like syntax:
   * - *text* for bold
   * - _text_ for italic
   *
   * @param markdownText Text with markdown-like formatting
   * @return SpannableString with actual Android text spans
   */
  fun createRichText(markdownText: String): SpannableString {
    val spannable = SpannableString(markdownText.replace("*", "").replace("_", ""))

    // Apply bold spans for *text*
    var index = 0
    while (index < markdownText.length) {
      val boldStart = markdownText.indexOf('*', index)
      if (boldStart < 0) break

      val boldEnd = markdownText.indexOf('*', boldStart + 1)
      if (boldEnd < 0) break

      val textStart = spannable.toString().indexOf(
        markdownText.substring(boldStart + 1, boldEnd)
      )
      val textEnd = textStart + (boldEnd - boldStart - 1)

      spannable.setSpan(
        StyleSpan(android.graphics.Typeface.BOLD),
        textStart,
        textEnd,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )

      index = boldEnd + 1
    }

    return spannable
  }

  /**
   * Create a test bitmap with specified color.
   *
   * @param width Bitmap width
   * @param height Bitmap height
   * @param color Fill color
   * @return Solid color bitmap
   */
  fun createTestBitmap(
    width: Int,
    height: Int,
    color: Int = Color.RED
  ): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(color)
    return bitmap
  }

  /**
   * Create a test bitmap with a pattern for visual comparison.
   *
   * Creates a gradient or checkered pattern for testing
   * image quality degradation.
   *
   * @param width Bitmap width
   * @param height Bitmap height
   * @return Patterned bitmap
   */
  fun createPatternedBitmap(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    // Draw checkerboard pattern
    val squareSize = 20
    for (y in 0 until height step squareSize) {
      for (x in 0 until width step squareSize) {
        paint.color = if ((x / squareSize + y / squareSize) % 2 == 0) {
          Color.WHITE
        } else {
          Color.BLACK
        }
        canvas.drawRect(
          x.toFloat(),
          y.toFloat(),
          (x + squareSize).toFloat(),
          (y + squareSize).toFloat(),
          paint
        )
      }
    }

    return bitmap
  }

  /**
   * Verify image quality after compression/transfer.
   *
   * Compares original and transferred bitmaps using:
   * - Dimension matching
   * - Average color difference (PSNR-like metric)
   *
   * @param original Original bitmap before compression
   * @param transferred Bitmap after compression/decompression
   * @param qualityThreshold Minimum acceptable similarity (0.0-1.0, default 0.95)
   * @return true if images are similar enough
   */
  fun verifyImageQuality(
    original: Bitmap,
    transferred: Bitmap,
    qualityThreshold: Double = 0.95
  ): Boolean {
    // Check dimensions match (or transferred is scaled proportionally)
    val aspectRatioOriginal = original.width.toDouble() / original.height
    val aspectRatioTransferred = transferred.width.toDouble() / transferred.height

    if (abs(aspectRatioOriginal - aspectRatioTransferred) > 0.01) {
      return false
    }

    // Sample pixels for comparison (avoid checking every pixel for performance)
    val sampleCount = 100
    var totalDifference = 0.0

    for (i in 0 until sampleCount) {
      val xOrig = (original.width * i) / sampleCount
      val yOrig = (original.height * i) / sampleCount

      // Map to transferred bitmap coordinates
      val xTrans = (transferred.width * xOrig) / original.width
      val yTrans = (transferred.height * yOrig) / original.height

      val pixelOrig = original.getPixel(xOrig, yOrig)
      val pixelTrans = transferred.getPixel(xTrans, yTrans)

      // Calculate color difference (simplified RGB distance)
      val rDiff = abs(Color.red(pixelOrig) - Color.red(pixelTrans))
      val gDiff = abs(Color.green(pixelOrig) - Color.green(pixelTrans))
      val bDiff = abs(Color.blue(pixelOrig) - Color.blue(pixelTrans))

      totalDifference += (rDiff + gDiff + bDiff) / (3.0 * 255.0)
    }

    val avgDifference = totalDifference / sampleCount
    val similarity = 1.0 - avgDifference

    return similarity >= qualityThreshold
  }

  /**
   * Convert bitmap to byte array (for testing serialization).
   *
   * @param bitmap Bitmap to convert
   * @param format Compression format
   * @param quality Compression quality (0-100)
   * @return Compressed byte array
   */
  fun bitmapToBytes(
    bitmap: Bitmap,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 85
  ): ByteArray {
    val outputStream = ByteArrayOutputStream()
    bitmap.compress(format, quality, outputStream)
    return outputStream.toByteArray()
  }

  /**
   * Convert byte array to bitmap (for testing deserialization).
   *
   * @param bytes Compressed image bytes
   * @return Decoded bitmap
   */
  fun bytesToBitmap(bytes: ByteArray): Bitmap? {
    return try {
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Calculate MD5 hash of bitmap data (for deduplication testing).
   *
   * @param bitmap Bitmap to hash
   * @return MD5 hash string
   */
  fun calculateBitmapHash(bitmap: Bitmap): String {
    val bytes = bitmapToBytes(bitmap)
    return calculateMD5(bytes)
  }

  /**
   * Calculate MD5 hash of byte array.
   *
   * @param data Data to hash
   * @return MD5 hash as hex string
   */
  private fun calculateMD5(data: ByteArray): String {
    val md = java.security.MessageDigest.getInstance("MD5")
    md.update(data)
    return md.digest().joinToString("") { "%02x".format(it) }
  }

  /**
   * Create a mock Context for testing.
   *
   * @return Mock Context with basic functionality
   */
  fun createMockContext(): Context {
    val mockContext = Mockito.mock(Context::class.java)
    val mockPackageManager = Mockito.mock(android.content.pm.PackageManager::class.java)

    Mockito.`when`(mockContext.packageManager).thenReturn(mockPackageManager)
    Mockito.`when`(mockContext.createPackageContext(
      Mockito.anyString(),
      Mockito.anyInt()
    )).thenReturn(mockContext)

    return mockContext
  }
}
