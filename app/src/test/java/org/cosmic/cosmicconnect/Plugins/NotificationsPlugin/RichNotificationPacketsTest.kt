/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.graphics.Bitmap
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RichNotificationPackets.
 *
 * Tests rich notification packet creation with:
 * - BigPicture images
 * - Rich text
 * - Embedded links
 * - Video metadata
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RichNotificationPacketsTest {

    private lateinit var baseNotificationInfo: NotificationInfo

    @Before
    fun setUp() {
        baseNotificationInfo = NotificationInfo(
            id = "test-notif-123",
            appName = "Test App",
            title = "Test Notification",
            text = "This is a test notification",
            isClearable = true,
            time = "1704067200000",
            silent = "false"
        )
    }

    @Test
    fun testCreateRichNotificationPacket_withImage() {
        // Create test image
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.PNG,
            sizeBytes = 5000
        )

        // Create rich packet
        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            bigPicture = image
        )

        // Verify packet structure
        assertEquals("cconnect.notification", packet.type)
        assertEquals(baseNotificationInfo.id, packet.body["id"])
        assertEquals(baseNotificationInfo.title, packet.body["title"])
        assertEquals(baseNotificationInfo.text, packet.body["text"])

        // Verify image metadata
        assertTrue(packet.body["hasImage"] as Boolean)
        assertEquals("png", packet.body["imageFormat"])
        assertEquals(100, packet.body["imageWidth"])
        assertEquals(100, packet.body["imageHeight"])
        assertEquals(5000, packet.body["imageSize"])
    }

    @Test
    fun testCreateRichNotificationPacket_withRichText() {
        val richText = "**Bold text** and _italic text_"

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            richText = richText
        )

        assertEquals(richText, packet.body["richText"])
    }

    @Test
    fun testCreateRichNotificationPacket_withLinks() {
        val links = listOf(
            NotificationLink("https://example.com", "Example Site"),
            NotificationLink("https://test.com")
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            links = links
        )

        @Suppress("UNCHECKED_CAST")
        val packetLinks = packet.body["links"] as List<Map<String, String>>
        assertEquals(2, packetLinks.size)
        assertEquals("https://example.com", packetLinks[0]["url"])
        assertEquals("Example Site", packetLinks[0]["title"])
        assertEquals("https://test.com", packetLinks[1]["url"])
    }

    @Test
    fun testCreateRichNotificationPacket_withVideo() {
        val videoInfo = VideoInfo(
            title = "Test Video",
            duration = 120000L // 2 minutes
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            videoInfo = videoInfo
        )

        assertTrue(packet.body["hasVideo"] as Boolean)
        assertEquals("Test Video", packet.body["videoTitle"])
        assertEquals(120000L, packet.body["videoDuration"])
    }

    @Test
    fun testCreateRichNotificationPacket_withAllContent() {
        val bitmap = Bitmap.createBitmap(200, 150, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.JPEG,
            sizeBytes = 10000
        )

        val richText = "Formatted **notification** text"
        val links = listOf(NotificationLink("https://example.com"))
        val videoInfo = VideoInfo(title = "Video Title")

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            bigPicture = image,
            richText = richText,
            links = links,
            videoInfo = videoInfo
        )

        // Verify all fields are present
        assertTrue(packet.body["hasImage"] as Boolean)
        assertEquals(richText, packet.body["richText"])
        assertNotNull(packet.body["links"])
        assertTrue(packet.body["hasVideo"] as Boolean)
    }

    @Test
    fun testAttachImagePayload() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.PNG,
            sizeBytes = 2500
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            bigPicture = image
        )

        val packetWithPayload = RichNotificationPackets.attachImagePayload(packet, image)

        // Verify payload hash is set
        assertNotNull(packetWithPayload.body["payloadHash"])
        val hash = packetWithPayload.body["payloadHash"] as String
        assertTrue(hash.matches(Regex("[a-f0-9]{32}"))) // MD5 hash format

        // Verify payload size
        assertEquals(image.sizeBytes, packetWithPayload.body["payloadSize"])
    }

    @Test
    fun testExtractedImage_toBytes_PNG() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.PNG,
            sizeBytes = 5000
        )

        val bytes = image.toBytes()
        assertTrue(bytes.isNotEmpty())

        // PNG signature
        assertEquals(0x89.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('N'.code.toByte(), bytes[2])
        assertEquals('G'.code.toByte(), bytes[3])
    }

    @Test
    fun testExtractedImage_toBytes_JPEG() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.JPEG,
            sizeBytes = 5000
        )

        val bytes = image.toBytes()
        assertTrue(bytes.isNotEmpty())

        // JPEG signature (JFIF)
        assertEquals(0xFF.toByte(), bytes[0])
        assertEquals(0xD8.toByte(), bytes[1]) // SOI marker
    }

    @Test
    fun testExtractedImage_toBytes_customQuality() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.JPEG,
            sizeBytes = 5000
        )

        val highQuality = image.toBytes(quality = 95)
        val lowQuality = image.toBytes(quality = 50)

        // Higher quality should produce larger files
        assertTrue(highQuality.size >= lowQuality.size)
    }

    @Test
    fun testNetworkPacket_hasImage_extension() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.PNG,
            sizeBytes = 5000
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            bigPicture = image
        )

        assertTrue(packet.hasImage)
        assertEquals("png", packet.imageFormat)
        assertEquals(100, packet.imageWidth)
        assertEquals(100, packet.imageHeight)
    }

    @Test
    fun testNetworkPacket_richText_extension() {
        val richText = "**Bold** and _italic_"
        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            richText = richText
        )

        assertEquals(richText, packet.richText)
    }

    @Test
    fun testNetworkPacket_links_extension() {
        val links = listOf(
            NotificationLink("https://example.com", "Example"),
            NotificationLink("https://test.com")
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            links = links
        )

        val extractedLinks = packet.links
        assertNotNull(extractedLinks)
        assertEquals(2, extractedLinks!!.size)
        assertEquals("https://example.com", extractedLinks[0].url)
        assertEquals("Example", extractedLinks[0].title)
        assertEquals("https://test.com", extractedLinks[1].url)
        assertNull(extractedLinks[1].title)
    }

    @Test
    fun testNetworkPacket_hasVideo_extension() {
        val videoInfo = VideoInfo(
            title = "Test Video",
            duration = 60000L
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            videoInfo = videoInfo
        )

        assertTrue(packet.hasVideo)
        assertEquals("Test Video", packet.videoTitle)
    }

    @Test
    fun testNotificationLink_creation() {
        val linkWithTitle = NotificationLink("https://example.com", "Example")
        assertEquals("https://example.com", linkWithTitle.url)
        assertEquals("Example", linkWithTitle.title)

        val linkWithoutTitle = NotificationLink("https://test.com")
        assertEquals("https://test.com", linkWithoutTitle.url)
        assertNull(linkWithoutTitle.title)
    }

    @Test
    fun testVideoInfo_creation() {
        val videoInfo = VideoInfo(
            thumbnailBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
            duration = 120000L,
            title = "My Video"
        )

        assertNotNull(videoInfo.thumbnailBitmap)
        assertEquals(120000L, videoInfo.duration)
        assertEquals("My Video", videoInfo.title)
    }

    @Test
    fun testCreateRichNotificationPacket_emptyOptionalFields() {
        // Create packet with null optional fields
        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            bigPicture = null,
            richText = null,
            links = null,
            videoInfo = null
        )

        // Should create standard notification packet
        assertEquals("cconnect.notification", packet.type)
        assertFalse(packet.body.containsKey("hasImage"))
        assertFalse(packet.body.containsKey("richText"))
        assertFalse(packet.body.containsKey("links"))
        assertFalse(packet.body.containsKey("hasVideo"))
    }

    @Test
    fun testCreateRichNotificationPacket_emptyRichText() {
        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            richText = ""
        )

        // Empty rich text should not be added
        assertFalse(packet.body.containsKey("richText"))
    }

    @Test
    fun testCreateRichNotificationPacket_emptyLinks() {
        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            links = emptyList()
        )

        // Empty links list should not be added
        assertFalse(packet.body.containsKey("links"))
    }

    @Test
    fun testImageFormat_dimensions() {
        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.JPEG,
            sizeBytes = 50000
        )

        assertEquals(1920, image.width)
        assertEquals(1080, image.height)
    }

    @Test
    fun testPayloadSize_inPacket() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val image = ExtractedImage(
            bitmap = bitmap,
            format = ExtractedImage.ImageFormat.PNG,
            sizeBytes = 5000
        )

        val packet = RichNotificationPackets.createRichNotificationPacket(
            notification = baseNotificationInfo,
            bigPicture = image
        )

        // Payload size should be set
        assertEquals(5000L, packet.payloadSize)
    }
}
