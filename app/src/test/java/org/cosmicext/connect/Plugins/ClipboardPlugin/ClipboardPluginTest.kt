/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.ClipboardPlugin

import android.os.Looper
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
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
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ClipboardPluginTest {

    private lateinit var plugin: ClipboardPlugin
    private lateinit var mockDevice: Device
    private lateinit var clipboardListener: ClipboardListener

    @Before
    fun setUp() {
        // Reset ClipboardListener singleton so tests are independent
        val field = ClipboardListener::class.java.getDeclaredField("instanceInternal")
        field.isAccessible = true
        field.set(null, null)

        val context = RuntimeEnvironment.getApplication()

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = ClipboardPlugin(context, mockDevice)

        // ClipboardListener initializes ClipboardManager via Handler.post —
        // idle the main looper so cm is set before setText() calls
        clipboardListener = ClipboardListener.instance(context)
        shadowOf(Looper.getMainLooper()).idle()
    }

    // ========================================================================
    // Extension properties — isClipboardUpdate
    // ========================================================================

    @Test
    fun `isClipboardUpdate true for correct type with content`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = mapOf("content" to "hello")
        )
        assertTrue(packet.isClipboardUpdate)
    }

    @Test
    fun `isClipboardUpdate false for correct type without content`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = emptyMap()
        )
        assertFalse(packet.isClipboardUpdate)
    }

    @Test
    fun `isClipboardUpdate false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("content" to "hello")
        )
        assertFalse(packet.isClipboardUpdate)
    }

    // ========================================================================
    // Extension properties — isClipboardConnect
    // ========================================================================

    @Test
    fun `isClipboardConnect true for connect type with content and timestamp`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "hello", "timestamp" to 1000L)
        )
        assertTrue(packet.isClipboardConnect)
    }

    @Test
    fun `isClipboardConnect false when missing timestamp`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "hello")
        )
        assertFalse(packet.isClipboardConnect)
    }

    @Test
    fun `isClipboardConnect false when missing content`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("timestamp" to 1000L)
        )
        assertFalse(packet.isClipboardConnect)
    }

    @Test
    fun `isClipboardConnect false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = mapOf("content" to "hello", "timestamp" to 1000L)
        )
        assertFalse(packet.isClipboardConnect)
    }

    // ========================================================================
    // Extension properties — clipboardContent
    // ========================================================================

    @Test
    fun `clipboardContent returns content for clipboard update`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = mapOf("content" to "clipboard text")
        )
        assertEquals("clipboard text", packet.clipboardContent)
    }

    @Test
    fun `clipboardContent returns content for clipboard connect`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "connect text", "timestamp" to 5000L)
        )
        assertEquals("connect text", packet.clipboardContent)
    }

    @Test
    fun `clipboardContent returns null for wrong packet type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("content" to "should not be returned")
        )
        assertNull(packet.clipboardContent)
    }

    // ========================================================================
    // Extension properties — clipboardTimestamp
    // ========================================================================

    @Test
    fun `clipboardTimestamp returns timestamp for connect packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "hello", "timestamp" to 42000L)
        )
        assertEquals(42000L, packet.clipboardTimestamp)
    }

    @Test
    fun `clipboardTimestamp returns null for standard update`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = mapOf("content" to "hello")
        )
        assertNull(packet.clipboardTimestamp)
    }

    @Test
    fun `clipboardTimestamp handles Int value via Number cast`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "hello", "timestamp" to 999)
        )
        assertEquals(999L, packet.clipboardTimestamp)
    }

    // ========================================================================
    // onPacketReceived — standard clipboard update
    // ========================================================================

    @Test
    fun `onPacketReceived clipboard update with content returns true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = mapOf("content" to "synced text")
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
        assertEquals("synced text", clipboardListener.currentContent)
    }

    @Test
    fun `onPacketReceived clipboard update with null content returns false`() {
        // No "content" key — isClipboardUpdate will be false (requires content key)
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    // ========================================================================
    // onPacketReceived — clipboard connect with timestamp sync
    // ========================================================================

    @Test
    fun `onPacketReceived connect with newer timestamp updates clipboard`() {
        // ClipboardListener starts with updateTimestamp = 0, so any positive timestamp is newer
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "newer text", "timestamp" to 5000L)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
        assertEquals("newer text", clipboardListener.currentContent)
    }

    @Test
    fun `onPacketReceived connect with zero timestamp returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "should not apply", "timestamp" to 0L)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    @Test
    fun `onPacketReceived connect with older timestamp than local returns false`() {
        // First set the local clipboard to establish a timestamp
        clipboardListener.setText("local content")
        val localTimestamp = clipboardListener.updateTimestamp

        // Now send a connect packet with an older timestamp
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("content" to "old text", "timestamp" to localTimestamp - 1000)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
        assertEquals("local content", clipboardListener.currentContent)
    }

    @Test
    fun `onPacketReceived connect with missing content returns false`() {
        // Has timestamp but missing content — isClipboardConnect will be false
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.clipboard.connect",
            body = mapOf("timestamp" to 5000L)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    // ========================================================================
    // onPacketReceived — wrong packet type
    // ========================================================================

    @Test
    fun `onPacketReceived wrong type returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("content" to "should not apply")
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    // ========================================================================
    // Packet type arrays
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains both clipboard types`() {
        val expected = arrayOf("cconnect.clipboard", "cconnect.clipboard.connect")
        assertArrayEquals(expected, plugin.supportedPacketTypes)
    }

    @Test
    fun `outgoingPacketTypes contains both clipboard types`() {
        val expected = arrayOf("cconnect.clipboard", "cconnect.clipboard.connect")
        assertArrayEquals(expected, plugin.outgoingPacketTypes)
    }
}
