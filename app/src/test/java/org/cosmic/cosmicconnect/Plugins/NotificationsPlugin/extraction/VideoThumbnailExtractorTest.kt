/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.extraction

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for VideoThumbnailExtractor.
 *
 * Tests:
 * - Video notification detection
 * - Known app detection
 * - Thumbnail extraction from extras
 * - MessagingStyle video attachments
 * - URL-based thumbnail generation
 * - Scaling and bitmap conversion
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class VideoThumbnailExtractorTest {

    private lateinit var context: Context
    private lateinit var extractor: VideoThumbnailExtractor

    private val mockBitmap = mockk<Bitmap>(relaxed = true) {
        every { width } returns 1920
        every { height } returns 1080
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        extractor = VideoThumbnailExtractor(context)

        // Mock static Bitmap methods
        mockkStatic(Bitmap::class)
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns mockBitmap
        every { Bitmap.createBitmap(any<Int>(), any(), any()) } returns mockBitmap
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== Detection Tests ==========

    @Test
    fun `detectVideoNotification returns true for YouTube`() {
        val notification = createMockNotification("com.google.android.youtube")

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect YouTube notifications", result)
    }

    @Test
    fun `detectVideoNotification returns true for Netflix`() {
        val notification = createMockNotification("com.netflix.mediaclient")

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect Netflix notifications", result)
    }

    @Test
    fun `detectVideoNotification returns true for TikTok`() {
        val notification = createMockNotification("com.zhiliaoapp.musically")

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect TikTok notifications", result)
    }

    @Test
    fun `detectVideoNotification returns true for VLC`() {
        val notification = createMockNotification("org.videolan.vlc")

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect VLC notifications", result)
    }

    @Test
    fun `detectVideoNotification returns true for notifications with MediaSession`() {
        val notification = createMockNotification("com.example.videoplayer").apply {
            val extras = Bundle().apply {
                putParcelable("android.mediaSession", mockk())
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect notifications with MediaSession", result)
    }

    @Test
    fun `detectVideoNotification returns true for MessagingStyle with video attachment`() {
        val notification = createMockNotification("com.example.messenger").apply {
            val extras = Bundle().apply {
                val message = Bundle().apply {
                    putString("type", "video/mp4")
                    putString("uri", "content://video.mp4")
                }
                putParcelableArray("android.messages", arrayOf(message))
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect MessagingStyle with video", result)
    }

    @Test
    fun `detectVideoNotification returns false for non-video notifications`() {
        val notification = createMockNotification("com.example.regularapp")

        val result = extractor.detectVideoNotification(notification)

        assertFalse("Should not detect non-video notifications", result)
    }

    @Test
    fun `detectVideoNotification returns true for transport category with video keywords`() {
        val notification = createMockNotification("com.example.app").apply {
            every { notification.category } returns Notification.CATEGORY_TRANSPORT
            val extras = Bundle().apply {
                putCharSequence("android.title", "Now Playing Video")
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.detectVideoNotification(notification)

        assertTrue("Should detect transport category with video keywords", result)
    }

    // ========== Extraction Tests ==========

    @Test
    fun `extractVideoInfo returns null for non-video notification`() {
        val notification = createMockNotification("com.example.regularapp")

        val result = extractor.extractVideoInfo(notification)

        assertNull("Should return null for non-video notification", result)
    }

    @Test
    fun `extractVideoInfo extracts title from extras`() {
        val notification = createMockNotification("com.google.android.youtube").apply {
            val extras = Bundle().apply {
                putCharSequence("android.title", "Awesome Video Title")
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract video info", result)
        assertEquals("Should extract title", "Awesome Video Title", result?.title)
    }

    @Test
    fun `extractVideoInfo extracts thumbnail from picture extra`() {
        val notification = createMockNotification("com.google.android.youtube").apply {
            val extras = Bundle().apply {
                putParcelable("android.picture", mockBitmap)
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract video info", result)
        assertNotNull("Should extract thumbnail", result?.thumbnailBitmap)
    }

    @Test
    fun `extractVideoInfo extracts thumbnail from large icon on API 23+`() {
        val mockIcon = mockk<Icon>(relaxed = true)
        val mockDrawable = mockk<BitmapDrawable>(relaxed = true) {
            every { bitmap } returns mockBitmap
        }
        every { mockIcon.loadDrawable(any()) } returns mockDrawable

        val notification = createMockNotification("com.netflix.mediaclient").apply {
            val extras = Bundle().apply {
                putParcelable("android.largeIcon", mockIcon)
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract video info", result)
        assertNotNull("Should extract thumbnail from large icon", result?.thumbnailBitmap)
    }

    @Test
    fun `extractVideoInfo extracts MessagingStyle video attachment`() {
        val notification = createMockNotification("com.example.messenger").apply {
            val extras = Bundle().apply {
                val message = Bundle().apply {
                    putString("type", "video/mp4")
                    putString("uri", "content://video.mp4")
                }
                putParcelableArray("android.messages", arrayOf(message))
            }
            mockNotificationExtras(this, extras)
        }

        // Mock extractThumbnailFromUrl to return a bitmap
        every { extractor.extractThumbnailFromUrl(any()) } returns mockBitmap

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract video info", result)
        assertEquals("Should extract video URL", "content://video.mp4", result?.videoUrl)
        assertEquals("Should extract MIME type", "video/mp4", result?.mimeType)
    }

    @Test
    fun `extractVideoInfo prioritizes most recent MessagingStyle video`() {
        val notification = createMockNotification("com.example.messenger").apply {
            val extras = Bundle().apply {
                val message1 = Bundle().apply {
                    putString("type", "video/mp4")
                    putString("uri", "content://old_video.mp4")
                }
                val message2 = Bundle().apply {
                    putString("type", "video/webm")
                    putString("uri", "content://new_video.webm")
                }
                putParcelableArray("android.messages", arrayOf(message1, message2))
            }
            mockNotificationExtras(this, extras)
        }

        every { extractor.extractThumbnailFromUrl(any()) } returns mockBitmap

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract video info", result)
        assertEquals("Should extract most recent video URL", "content://new_video.webm", result?.videoUrl)
        assertEquals("Should extract correct MIME type", "video/webm", result?.mimeType)
    }

    @Test
    fun `extractVideoInfo returns null when no video content found`() {
        val notification = createMockNotification("com.google.android.youtube").apply {
            val extras = Bundle()
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        // Should return null since no thumbnail or URL extracted
        // (in real implementation, app-specific extraction might still work)
        assertNull("Should return null when no video content found", result)
    }

    // ========== URL Extraction Tests ==========

    @Test
    fun `extractThumbnailFromUrl returns null for blank URL`() {
        val result = extractor.extractThumbnailFromUrl("")

        assertNull("Should return null for blank URL", result)
    }

    @Test
    fun `extractThumbnailFromUrl handles exceptions gracefully`() {
        // This test verifies error handling - actual thumbnail extraction
        // would require mocking MediaMetadataRetriever which is complex
        val result = extractor.extractThumbnailFromUrl("invalid://url")

        // Should return null on error, not crash
        assertNull("Should handle invalid URL gracefully", result)
    }

    // ========== Helper Tests ==========

    @Test
    fun `VideoInfo can be created with all parameters`() {
        val videoInfo = VideoInfo(
            thumbnailBitmap = mockBitmap,
            videoUrl = "https://example.com/video.mp4",
            duration = 120000L,
            mimeType = "video/mp4",
            title = "Test Video"
        )

        assertNotNull("VideoInfo should be created", videoInfo)
        assertEquals("Should set URL", "https://example.com/video.mp4", videoInfo.videoUrl)
        assertEquals("Should set duration", 120000L, videoInfo.duration)
        assertEquals("Should set MIME type", "video/mp4", videoInfo.mimeType)
        assertEquals("Should set title", "Test Video", videoInfo.title)
        assertNotNull("Should set thumbnail", videoInfo.thumbnailBitmap)
    }

    @Test
    fun `VideoInfo can be created with null parameters`() {
        val videoInfo = VideoInfo()

        assertNotNull("VideoInfo should be created", videoInfo)
        assertNull("URL should be null", videoInfo.videoUrl)
        assertNull("Duration should be null", videoInfo.duration)
        assertNull("MIME type should be null", videoInfo.mimeType)
        assertNull("Title should be null", videoInfo.title)
        assertNull("Thumbnail should be null", videoInfo.thumbnailBitmap)
    }

    // ========== Edge Cases ==========

    @Test
    fun `extractVideoInfo handles null notification extras`() {
        val notification = createMockNotification("com.google.android.youtube").apply {
            every { notification.extras } returns null
        }

        val result = extractor.extractVideoInfo(notification)

        assertNull("Should handle null extras gracefully", result)
    }

    @Test
    fun `extractVideoInfo handles empty MessagingStyle messages`() {
        val notification = createMockNotification("com.example.messenger").apply {
            val extras = Bundle().apply {
                putParcelableArray("android.messages", emptyArray())
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        // Should not crash, may return null or extract from other sources
        // depending on implementation
    }

    @Test
    fun `extractVideoInfo handles malformed MessagingStyle messages`() {
        val notification = createMockNotification("com.example.messenger").apply {
            val extras = Bundle().apply {
                val message = Bundle().apply {
                    // Missing type and uri
                }
                putParcelableArray("android.messages", arrayOf(message))
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        // Should not crash
    }

    // ========== App-Specific Tests ==========

    @Test
    fun `extractVideoInfo handles YouTube-specific extraction`() {
        val notification = createMockNotification("com.google.android.youtube").apply {
            val extras = Bundle().apply {
                putCharSequence("android.title", "YouTube Video")
                putCharSequence("android.text", "Check out this video: https://youtube.com/watch?v=abc123")
                putParcelable("android.picture", mockBitmap)
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract YouTube video info", result)
        assertEquals("Should extract title", "YouTube Video", result?.title)
        assertNotNull("Should extract thumbnail", result?.thumbnailBitmap)
        // URL extraction from text may or may not work depending on implementation
    }

    @Test
    fun `extractVideoInfo handles Netflix-specific extraction`() {
        val notification = createMockNotification("com.netflix.mediaclient").apply {
            val extras = Bundle().apply {
                putCharSequence("android.title", "Now Playing: Movie Title")
                putParcelable("android.picture", mockBitmap)
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract Netflix video info", result)
        assertEquals("Should extract title", "Now Playing: Movie Title", result?.title)
        assertNotNull("Should extract thumbnail", result?.thumbnailBitmap)
    }

    @Test
    fun `extractVideoInfo handles TikTok-specific extraction`() {
        val notification = createMockNotification("com.zhiliaoapp.musically").apply {
            val extras = Bundle().apply {
                putCharSequence("android.title", "New TikTok")
                putParcelable("android.largeIcon.big", mockk<Icon>(relaxed = true) {
                    every { loadDrawable(any()) } returns BitmapDrawable(mockk(), mockBitmap)
                })
            }
            mockNotificationExtras(this, extras)
        }

        val result = extractor.extractVideoInfo(notification)

        assertNotNull("Should extract TikTok video info", result)
        assertEquals("Should extract title", "New TikTok", result?.title)
    }

    // ========== Helper Methods ==========

    /**
     * Create a mock StatusBarNotification with given package name.
     */
    private fun createMockNotification(packageName: String): StatusBarNotification {
        val mockNotification = mockk<Notification>(relaxed = true) {
            every { extras } returns Bundle()
        }

        return mockk<StatusBarNotification>(relaxed = true) {
            every { this@mockk.packageName } returns packageName
            every { notification } returns mockNotification
            every { key } returns "test_key"
            every { tag } returns null
            every { id } returns 1
        }
    }

    /**
     * Mock notification extras for a StatusBarNotification.
     */
    private fun mockNotificationExtras(
        statusBarNotification: StatusBarNotification,
        extras: Bundle
    ) {
        every { statusBarNotification.notification.extras } returns extras
    }
}
