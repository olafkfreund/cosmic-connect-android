/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.SharePlugin

import io.mockk.every
import io.mockk.mockk
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharePluginTest {

    private lateinit var plugin: SharePlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = SharePlugin(context, mockDevice)
    }

    // ========================================================================
    // Extension properties — isFileShare
    // ========================================================================

    @Test
    fun `isFileShare true for share request with filename`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "photo.jpg")
        )
        assertTrue(packet.isFileShare)
    }

    @Test
    fun `isFileShare false without filename`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "hello")
        )
        assertFalse(packet.isFileShare)
    }

    @Test
    fun `isFileShare false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("filename" to "photo.jpg")
        )
        assertFalse(packet.isFileShare)
    }

    // ========================================================================
    // Extension properties — isTextShare
    // ========================================================================

    @Test
    fun `isTextShare true for share request with text`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "Hello world")
        )
        assertTrue(packet.isTextShare)
    }

    @Test
    fun `isTextShare false without text key`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("url" to "https://example.com")
        )
        assertFalse(packet.isTextShare)
    }

    // ========================================================================
    // Extension properties — isUrlShare
    // ========================================================================

    @Test
    fun `isUrlShare true for share request with url`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("url" to "https://example.com")
        )
        assertTrue(packet.isUrlShare)
    }

    @Test
    fun `isUrlShare false without url key`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "hello")
        )
        assertFalse(packet.isUrlShare)
    }

    // ========================================================================
    // Extension properties — isMultiFileUpdate
    // ========================================================================

    @Test
    fun `isMultiFileUpdate true for update type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request.update",
            body = emptyMap()
        )
        assertTrue(packet.isMultiFileUpdate)
    }

    @Test
    fun `isMultiFileUpdate false for share request type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = emptyMap()
        )
        assertFalse(packet.isMultiFileUpdate)
    }

    // ========================================================================
    // Extension properties — data extraction
    // ========================================================================

    @Test
    fun `filename returns name for file share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "document.pdf")
        )
        assertEquals("document.pdf", packet.filename)
    }

    @Test
    fun `filename returns null for non-file share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "hello")
        )
        assertNull(packet.filename)
    }

    @Test
    fun `sharedText returns text for text share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "Shared message")
        )
        assertEquals("Shared message", packet.sharedText)
    }

    @Test
    fun `sharedText returns null for non-text share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("url" to "https://example.com")
        )
        assertNull(packet.sharedText)
    }

    @Test
    fun `sharedUrl returns url for url share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("url" to "https://example.com/page")
        )
        assertEquals("https://example.com/page", packet.sharedUrl)
    }

    @Test
    fun `sharedUrl returns null for non-url share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "file.txt")
        )
        assertNull(packet.sharedUrl)
    }

    @Test
    fun `numberOfFiles returns count for multi-file update`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request.update",
            body = mapOf("numberOfFiles" to 5)
        )
        assertEquals(5, packet.numberOfFiles)
    }

    @Test
    fun `numberOfFiles returns null for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("numberOfFiles" to 5)
        )
        assertNull(packet.numberOfFiles)
    }

    @Test
    fun `totalPayloadSize returns size for multi-file update`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request.update",
            body = mapOf("totalPayloadSize" to 10485760L)
        )
        assertEquals(10485760L, packet.totalPayloadSize)
    }

    @Test
    fun `totalPayloadSize handles Int value via Number cast`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request.update",
            body = mapOf("totalPayloadSize" to 1024)
        )
        assertEquals(1024L, packet.totalPayloadSize)
    }

    @Test
    fun `creationTime returns timestamp for file share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "photo.jpg", "creationTime" to 1704067200000L)
        )
        assertEquals(1704067200000L, packet.creationTime)
    }

    @Test
    fun `creationTime returns null when missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "photo.jpg")
        )
        assertNull(packet.creationTime)
    }

    @Test
    fun `lastModified returns timestamp for file share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "photo.jpg", "lastModified" to 1704067200000L)
        )
        assertEquals(1704067200000L, packet.lastModified)
    }

    @Test
    fun `lastModified returns null for non-file share`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "hello", "lastModified" to 1704067200000L)
        )
        assertNull(packet.lastModified)
    }

    // ========================================================================
    // onPacketReceived — dispatch
    // ========================================================================

    @Test
    fun `onPacketReceived with url returns true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("url" to "https://example.com")
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    @Test
    fun `onPacketReceived with text returns true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("text" to "Hello from desktop")
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    @Test
    fun `onPacketReceived with update type returns true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request.update",
            body = mapOf("numberOfFiles" to 3, "totalPayloadSize" to 1024L)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    @Test
    fun `onPacketReceived with no recognized fields returns true`() {
        // SharePlugin always returns true even on unrecognized content
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    @Test
    fun `onPacketReceived wrong packet type returns true`() {
        // SharePlugin always returns true
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains share request and update`() {
        val expected = arrayOf("cconnect.share.request", "cconnect.share.request.update")
        assertArrayEquals(expected, plugin.supportedPacketTypes)
    }

    @Test
    fun `outgoingPacketTypes contains share request`() {
        assertArrayEquals(arrayOf("cconnect.share.request"), plugin.outgoingPacketTypes)
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }
}
