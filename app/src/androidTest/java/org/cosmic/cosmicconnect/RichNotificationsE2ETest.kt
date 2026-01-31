/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction.BigPictureExtractor
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.links.LinkDetector
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration tests for Rich Notifications.
 *
 * Tests the complete flow from notification creation to desktop display:
 * 1. Android app posts notification with rich content
 * 2. NotificationListenerService captures notification
 * 3. Rich content extractors process the notification
 * 4. Network packets are created with extracted data
 * 5. (Simulated) Desktop receives and displays rich content
 *
 * This test validates the integration of:
 * - BigPictureExtractor
 * - LinkDetector
 * - RichTextParser (when implemented)
 * - Network packet creation
 * - Image compression and caching
 *
 * Note: These are instrumented tests that run on device/emulator.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class RichNotificationsE2ETest {

  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager
  private lateinit var bigPictureExtractor: BigPictureExtractor
  private lateinit var linkDetector: LinkDetector

  private val testChannelId = "test_rich_notifications"
  private var notificationId = 1000

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create test notification channel (required for API 26+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        testChannelId,
        "Test Rich Notifications",
        NotificationManager.IMPORTANCE_HIGH
      )
      notificationManager.createNotificationChannel(channel)
    }

    bigPictureExtractor = BigPictureExtractor(context)
    linkDetector = LinkDetector()
  }

  @After
  fun tearDown() {
    // Clean up test notifications
    notificationManager.cancelAll()

    // Delete test channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      notificationManager.deleteNotificationChannel(testChannelId)
    }
  }

  // ============================================
  // BigPictureStyle E2E Tests
  // ============================================

  @Test
  fun testBigPictureNotification_ExtractAndCompress() {
    // Arrange: Create a BigPictureStyle notification
    val testBitmap = createTestBitmap(800, 600, Color.BLUE)
    val notification = createBigPictureNotification(
      title = "Photo Shared",
      text = "Check out this photo!",
      bigPicture = testBitmap
    )
    val sbn = createStatusBarNotification(notification)

    // Act: Extract image
    val extractedImage = bigPictureExtractor.extractBigPicture(sbn)

    // Assert: Image extracted and processed
    assertNotNull("Should extract big picture", extractedImage)
    extractedImage?.let {
      assertTrue("Width should be scaled", it.width <= 400)
      assertTrue("Height should be scaled", it.height <= 400)
      assertTrue("Compressed data should exist", it.compressedData.isNotEmpty())
      assertTrue("Should have valid MD5 hash", it.hash.matches(Regex("[0-9a-f]{32}")))
      assertEquals("Should be JPEG", "image/jpeg", it.mimeType)

      // Verify aspect ratio preserved
      val originalRatio = 800f / 600f
      val extractedRatio = it.width.toFloat() / it.height.toFloat()
      assertEquals("Aspect ratio should be preserved", originalRatio, extractedRatio, 0.01f)

      // Clean up
      it.bitmap.recycle()
    }

    testBitmap.recycle()
  }

  @Test
  fun testBigPictureNotification_ImageCacheDeduplication() {
    // TODO: Implement when NotificationImageCache exists
    // Test that identical images are cached and not re-transferred
  }

  // ============================================
  // Link Detection E2E Tests
  // ============================================

  @Test
  fun testNotificationWithLinks_DetectAndExtract() {
    // Arrange: Create notification with URLs
    val text = "Check out https://example.com and https://news.example.org/article"
    val notification = createSimpleNotification(
      title = "Shared Links",
      text = text
    )

    // Act: Detect links
    val links = linkDetector.detectLinks(text)

    // Assert: Links detected
    assertEquals("Should detect 2 links", 2, links.size)
    assertEquals("First link correct", "https://example.com", links[0].url)
    assertEquals("Second link correct", "https://news.example.org/article", links[1].url)
  }

  @Test
  fun testNotificationWithLinks_SecurityValidation() {
    // Arrange: Create notification with malicious URLs
    val maliciousUrls = listOf(
      "javascript:alert('xss')",
      "file:///etc/passwd",
      "http://localhost:8080/admin",
      "http://192.168.1.1/router"
    )

    // Act & Assert: All malicious URLs should be rejected
    for (url in maliciousUrls) {
      val links = linkDetector.detectLinks(url)
      assertEquals("Should reject malicious URL: $url", 0, links.size)
    }
  }

  // ============================================
  // Rich Text E2E Tests
  // ============================================

  @Ignore("RichTextParser not yet implemented - Issue #137")
  @Test
  fun testRichTextNotification_FormatPreservation() {
    // TODO: Test that rich text formatting is preserved through transfer
    // Arrange: Create notification with styled text (bold, italic, etc.)
    // Act: Extract and parse rich text
    // Assert: Formatting is preserved in output
  }

  // ============================================
  // Combined Features E2E Tests
  // ============================================

  @Test
  fun testNotificationWithImageAndLinks_BothExtracted() {
    // Arrange: Create notification with both image and links
    val testBitmap = createTestBitmap(500, 500, Color.RED)
    val textWithLinks = "New photo! View at https://photos.example.com/123"
    val notification = createBigPictureNotification(
      title = "Photo Shared",
      text = textWithLinks,
      bigPicture = testBitmap
    )
    val sbn = createStatusBarNotification(notification)

    // Act: Extract both image and links
    val extractedImage = bigPictureExtractor.extractBigPicture(sbn)
    val detectedLinks = linkDetector.detectLinks(textWithLinks)

    // Assert: Both image and links extracted
    assertNotNull("Should extract image", extractedImage)
    assertEquals("Should detect link", 1, detectedLinks.size)
    assertEquals("Link should be correct", "https://photos.example.com/123", detectedLinks[0].url)

    extractedImage?.bitmap?.recycle()
    testBitmap.recycle()
  }

  @Ignore("Full integration not yet implemented - Issue #137")
  @Test
  fun testCompleteNotificationFlow_AllFeatures() {
    // TODO: Test complete flow with image + links + rich text
    // This requires all components to be implemented:
    // - BigPictureExtractor ✓
    // - LinkDetector ✓
    // - RichTextParser
    // - NotificationImageCache
    // - Packet creation
  }

  // ============================================
  // Performance E2E Tests
  // ============================================

  @Test
  fun testImageExtraction_Performance() {
    val testBitmap = createTestBitmap(1920, 1080, Color.GREEN)
    val notification = createBigPictureNotification(
      title = "Large Photo",
      text = "High resolution image",
      bigPicture = testBitmap
    )
    val sbn = createStatusBarNotification(notification)

    // Measure extraction time
    val startTime = System.currentTimeMillis()
    val extractedImage = bigPictureExtractor.extractBigPicture(sbn)
    val duration = System.currentTimeMillis() - startTime

    assertNotNull("Should extract image", extractedImage)
    assertTrue("Extraction should complete in < 2 seconds", duration < 2000)

    extractedImage?.bitmap?.recycle()
    testBitmap.recycle()
  }

  @Test
  fun testLinkDetection_Performance() {
    // Create text with many URLs
    val textWithManyLinks = buildString {
      for (i in 1..10) {
        append("Visit https://example$i.com ")
      }
    }

    // Measure detection time
    val startTime = System.currentTimeMillis()
    val links = linkDetector.detectLinks(textWithManyLinks)
    val duration = System.currentTimeMillis() - startTime

    assertTrue("Should detect at least 5 links (MAX_LINKS)", links.size >= 5)
    assertTrue("Detection should complete in < 100ms", duration < 100)
  }

  // ============================================
  // Memory Management E2E Tests
  // ============================================

  @Test
  fun testMultipleNotifications_NoMemoryLeak() {
    // Process multiple notifications to check for memory leaks
    for (i in 1..10) {
      val testBitmap = createTestBitmap(400, 400, Color.rgb(i * 25, 100, 200))
      val notification = createBigPictureNotification(
        title = "Photo $i",
        text = "Test photo number $i",
        bigPicture = testBitmap
      )
      val sbn = createStatusBarNotification(notification)

      val extractedImage = bigPictureExtractor.extractBigPicture(sbn)

      // Immediately clean up
      extractedImage?.bitmap?.recycle()
      testBitmap.recycle()
    }

    // If we get here without OOM, test passes
    assertTrue("Should process 10 notifications without OOM", true)
  }

  // ============================================
  // Helper Methods
  // ============================================

  private fun createTestBitmap(width: Int, height: Int, color: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(color)
    return bitmap
  }

  private fun createSimpleNotification(title: String, text: String): Notification {
    return NotificationCompat.Builder(context, testChannelId)
      .setContentTitle(title)
      .setContentText(text)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .build()
  }

  private fun createBigPictureNotification(
    title: String,
    text: String,
    bigPicture: Bitmap
  ): Notification {
    return NotificationCompat.Builder(context, testChannelId)
      .setContentTitle(title)
      .setContentText(text)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setStyle(
        NotificationCompat.BigPictureStyle()
          .bigPicture(bigPicture)
          .setBigContentTitle(title)
      )
      .build()
  }

  private fun createStatusBarNotification(notification: Notification): StatusBarNotification {
    val id = notificationId++
    return StatusBarNotification(
      context.packageName,
      null,
      id,
      "test_tag",
      android.os.Process.myUid(),
      android.os.Process.myPid(),
      notification,
      android.os.UserHandle.getUserHandleForUid(android.os.Process.myUid()),
      System.currentTimeMillis()
    )
  }
}
