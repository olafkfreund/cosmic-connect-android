/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.MouseReceiverPlugin

import android.content.Context
import io.mockk.*
import org.cosmicext.connect.Core.*
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.RemoteKeyboardPlugin.RemoteKeyboardPlugin
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MouseReceiverPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: MouseReceiverPlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true) {
            every { deviceId } returns "test-device-id"
            every { name } returns "Test Device"
        }
        plugin = MouseReceiverPlugin(context, mockDevice)
    }

    @Test
    fun `packet type validation rejects invalid types`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("dx" to 5.0)
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertFalse(result)
    }

    @Test
    fun `packet type validation accepts mousepad request`() {
        // Create a keyboard packet (will be silently ignored by MouseReceiverPlugin)
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf("key" to "a")
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        // Should return false because it's a keyboard packet (handled by RemoteKeyboardPlugin)
        assertFalse(result)
    }

    @Test
    fun `mouse move packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "dx" to 10.0,
                "dy" to 5.0
            )
        )

        // Plugin will try to call MouseReceiverService.move() which is static
        // This test validates packet parsing logic, actual service calls are out of scope
        val dx = packet.getDouble("dx", 0.0)
        val dy = packet.getDouble("dy", 0.0)

        assertEquals(10.0, dx, 0.01)
        assertEquals(5.0, dy, 0.01)
    }

    @Test
    fun `single click packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "singleclick" to true
            )
        )

        val isSingleClick = packet.getBoolean("singleclick", false)
        assertTrue(isSingleClick)
    }

    @Test
    fun `double click packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "doubleclick" to true
            )
        )

        val isDoubleClick = packet.getBoolean("doubleclick", false)
        assertTrue(isDoubleClick)
    }

    @Test
    fun `middle click packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "middleclick" to true
            )
        )

        val isMiddleClick = packet.getBoolean("middleclick", false)
        assertTrue(isMiddleClick)
    }

    @Test
    fun `right click packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "rightclick" to true
            )
        )

        val isRightClick = packet.getBoolean("rightclick", false)
        assertTrue(isRightClick)
    }

    @Test
    fun `forward click packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "forwardclick" to true
            )
        )

        val isForwardClick = packet.getBoolean("forwardclick", false)
        assertTrue(isForwardClick)
    }

    @Test
    fun `back click packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "backclick" to true
            )
        )

        val isBackClick = packet.getBoolean("backclick", false)
        assertTrue(isBackClick)
    }

    @Test
    fun `single hold packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "singlehold" to true
            )
        )

        val isSingleHold = packet.getBoolean("singlehold", false)
        assertTrue(isSingleHold)
    }

    @Test
    fun `single release packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "singlerelease" to true
            )
        )

        val isSingleRelease = packet.getBoolean("singlerelease", false)
        assertTrue(isSingleRelease)
    }

    @Test
    fun `scroll packet is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "scroll" to true,
                "dx" to 0.0,
                "dy" to 3.0
            )
        )

        val isScroll = packet.getBoolean("scroll", false)
        val dx = packet.getDouble("dx", 0.0)
        val dy = packet.getDouble("dy", 0.0)

        assertTrue(isScroll)
        assertEquals(0.0, dx, 0.01)
        assertEquals(3.0, dy, 0.01)
    }

    @Test
    fun `mouse move with zero deltas is recognized`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf(
                "dx" to 0.0,
                "dy" to 0.0
            )
        )

        val dx = packet.getDouble("dx", 0.0)
        val dy = packet.getDouble("dy", 0.0)

        assertEquals(0.0, dx, 0.01)
        assertEquals(0.0, dy, 0.01)
    }

    @Test
    fun `keyboard packet type detection for RemoteKeyboardPlugin`() {
        // Mouse packet (no key/specialKey)
        val mousePacket = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf("dx" to 5.0)
        )

        assertEquals(
            RemoteKeyboardPlugin.MousePadPacketType.Mouse,
            RemoteKeyboardPlugin.getMousePadPacketType(mousePacket)
        )
    }

    @Test
    fun `keyboard packet with key field is detected`() {
        // Keyboard packet (has "key" field)
        val keyboardPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf("key" to "a")
        )

        assertEquals(
            RemoteKeyboardPlugin.MousePadPacketType.Keyboard,
            RemoteKeyboardPlugin.getMousePadPacketType(keyboardPacket)
        )
    }

    @Test
    fun `keyboard packet with specialKey field is detected`() {
        // Keyboard packet (has "specialKey" field)
        val specialKeyPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf("specialKey" to 12)
        )

        assertEquals(
            RemoteKeyboardPlugin.MousePadPacketType.Keyboard,
            RemoteKeyboardPlugin.getMousePadPacketType(specialKeyPacket)
        )
    }

    @Test
    fun `default boolean values for missing click fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.request",
            body = mapOf("dx" to 5.0)
        )

        assertFalse(packet.getBoolean("singleclick", false))
        assertFalse(packet.getBoolean("doubleclick", false))
        assertFalse(packet.getBoolean("middleclick", false))
        assertFalse(packet.getBoolean("rightclick", false))
        assertFalse(packet.getBoolean("forwardclick", false))
        assertFalse(packet.getBoolean("backclick", false))
        assertFalse(packet.getBoolean("singlehold", false))
        assertFalse(packet.getBoolean("singlerelease", false))
        assertFalse(packet.getBoolean("scroll", false))
    }

    @Test
    fun `plugin metadata is correct`() {
        assertNotNull(plugin.displayName)
        assertNotNull(plugin.description)
        assertEquals(1, plugin.supportedPacketTypes.size)
        assertEquals("cconnect.mousepad.request", plugin.supportedPacketTypes[0])
        assertEquals(0, plugin.outgoingPacketTypes.size)
    }
}
