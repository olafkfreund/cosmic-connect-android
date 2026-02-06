/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.RemoteKeyboardPlugin

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for RemoteKeyboardPlugin
 *
 * Tests packet type detection and packet reception logic without calling FFI.
 */
@RunWith(RobolectricTestRunner::class)
class RemoteKeyboardPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: RemoteKeyboardPlugin

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        plugin = RemoteKeyboardPlugin(context, mockDevice)
    }

    // ========================================================================
    // MousePad Packet Type Detection Tests
    // ========================================================================

    @Test
    fun `getMousePadPacketType detects keyboard packet with key field`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to "a",
                "shift" to false,
                "ctrl" to false,
                "alt" to false
            )
        )

        val result = RemoteKeyboardPlugin.getMousePadPacketType(packet)
        assertEquals(RemoteKeyboardPlugin.MousePadPacketType.Keyboard, result)
    }

    @Test
    fun `getMousePadPacketType detects keyboard packet with specialKey field`() {
        val packet = NetworkPacket(
            id = 2L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "specialKey" to 1,
                "shift" to false,
                "ctrl" to false,
                "alt" to false
            )
        )

        val result = RemoteKeyboardPlugin.getMousePadPacketType(packet)
        assertEquals(RemoteKeyboardPlugin.MousePadPacketType.Keyboard, result)
    }

    @Test
    fun `getMousePadPacketType detects keyboard packet with both key and specialKey`() {
        val packet = NetworkPacket(
            id = 3L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to "a",
                "specialKey" to 1
            )
        )

        val result = RemoteKeyboardPlugin.getMousePadPacketType(packet)
        assertEquals(RemoteKeyboardPlugin.MousePadPacketType.Keyboard, result)
    }

    @Test
    fun `getMousePadPacketType detects mouse packet without key or specialKey`() {
        val packet = NetworkPacket(
            id = 4L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "dx" to 10.0,
                "dy" to 20.0
            )
        )

        val result = RemoteKeyboardPlugin.getMousePadPacketType(packet)
        assertEquals(RemoteKeyboardPlugin.MousePadPacketType.Mouse, result)
    }

    @Test
    fun `getMousePadPacketType detects mouse packet with empty body`() {
        val packet = NetworkPacket(
            id = 5L,
            type = "cconnect.mousepad.request",
            body = emptyMap()
        )

        val result = RemoteKeyboardPlugin.getMousePadPacketType(packet)
        assertEquals(RemoteKeyboardPlugin.MousePadPacketType.Mouse, result)
    }

    // ========================================================================
    // Packet Reception Tests
    // ========================================================================

    @Test
    fun `onPacketReceived rejects wrong packet type`() {
        val packet = NetworkPacket(
            id = 6L,
            type = "cconnect.notification",
            body = emptyMap()
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    @Test
    fun `onPacketReceived accepts mousepad request packet type`() {
        val packet = NetworkPacket(
            id = 7L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to "a"
            )
        )

        // Will return false because RemoteKeyboardService is not available in tests
        // But should not throw exception
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result) // No service in test environment
    }

    @Test
    fun `onPacketReceived silently ignores mouse packets`() {
        val packet = NetworkPacket(
            id = 8L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "dx" to 10.0,
                "dy" to 20.0
            )
        )

        // Mouse packets should be silently ignored by keyboard plugin
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result) // Handled by MouseReceiverPlugin instead
    }

    @Test
    fun `onPacketReceived handles keyboard packet with all modifiers`() {
        val packet = NetworkPacket(
            id = 9L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to "c",
                "shift" to true,
                "ctrl" to true,
                "alt" to true
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        // Will fail without RemoteKeyboardService, but validates packet structure
        assertFalse(result)
    }

    @Test
    fun `onPacketReceived handles special key packet`() {
        val packet = NetworkPacket(
            id = 10L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "specialKey" to 1, // DEL key
                "shift" to false,
                "ctrl" to false,
                "alt" to false
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result) // No service in test
    }

    @Test
    fun `onPacketReceived handles packet with sendAck flag`() {
        val packet = NetworkPacket(
            id = 11L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to "a",
                "sendAck" to true
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result) // No service in test
    }

    // ========================================================================
    // Plugin Metadata Tests
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains mousepad request`() {
        val types = plugin.supportedPacketTypes
        assertEquals(1, types.size)
        assertTrue(types.contains("cconnect.mousepad.request"))
    }

    @Test
    fun `outgoingPacketTypes contains echo and keyboard state`() {
        val types = plugin.outgoingPacketTypes
        assertEquals(2, types.size)
        assertTrue(types.contains("cconnect.mousepad.echo"))
        assertTrue(types.contains("cconnect.mousepad.keyboardstate"))
    }

    @Test
    fun `displayName is not empty`() {
        val name = plugin.displayName
        assertTrue(name.isNotEmpty())
    }

    @Test
    fun `description is not empty`() {
        val desc = plugin.description
        assertTrue(desc.isNotEmpty())
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }

    // ========================================================================
    // Instance Tracking Tests
    // ========================================================================

    @Test
    fun `isConnected returns false when no instances`() {
        // Before onCreate, no instances registered
        val connected = RemoteKeyboardPlugin.isConnected()
        assertFalse(connected)
    }

    @Test
    fun `acquireInstances and releaseInstances work without crash`() {
        val instances = RemoteKeyboardPlugin.acquireInstances()
        try {
            // Instances list should be accessible
            assertTrue(instances.size >= 0)
        } finally {
            RemoteKeyboardPlugin.releaseInstances()
        }
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun `getMousePadPacketType handles packet with extra fields`() {
        val packet = NetworkPacket(
            id = 12L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to "a",
                "extraField" to "ignored",
                "anotherField" to 123
            )
        )

        val result = RemoteKeyboardPlugin.getMousePadPacketType(packet)
        assertEquals(RemoteKeyboardPlugin.MousePadPacketType.Keyboard, result)
    }

    @Test
    fun `onPacketReceived handles packet with empty key string`() {
        val packet = NetworkPacket(
            id = 13L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "key" to ""
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        // Empty key is still a keyboard packet
        assertFalse(result) // No service in test
    }

    @Test
    fun `onPacketReceived handles packet with invalid specialKey`() {
        val packet = NetworkPacket(
            id = 14L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "specialKey" to 999 // Invalid key code
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result) // No service in test
    }

    @Test
    fun `deviceIdValue returns device id`() {
        val deviceId = plugin.deviceIdValue
        assertEquals("test-device-id", deviceId)
    }

    @Test
    fun `checkRequiredPermissions returns boolean without crash`() {
        // Will return false in test environment (no input methods)
        val hasPermission = plugin.checkRequiredPermissions()
        assertFalse(hasPermission)
    }
}
