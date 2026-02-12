/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.MousePadPlugin

import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MousePadPluginTest {

    private lateinit var plugin: MousePadPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = MousePadPlugin(context, mockDevice)
    }

    // ========================================================================
    // isKeyboardEnabled state management
    // ========================================================================

    @Test
    fun `isKeyboardEnabled defaults to true`() {
        assertTrue(plugin.isKeyboardEnabled)
    }

    @Test
    fun `onPacketReceived sets isKeyboardEnabled to false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.keyboardstate",
            body = mapOf("state" to false)
        )
        plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(plugin.isKeyboardEnabled)
    }

    @Test
    fun `onPacketReceived sets isKeyboardEnabled to true`() {
        // First disable
        val disablePacket = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.keyboardstate",
            body = mapOf("state" to false)
        )
        plugin.onPacketReceived(TransferPacket(disablePacket))
        assertFalse(plugin.isKeyboardEnabled)

        // Then re-enable
        val enablePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.mousepad.keyboardstate",
            body = mapOf("state" to true)
        )
        plugin.onPacketReceived(TransferPacket(enablePacket))
        assertTrue(plugin.isKeyboardEnabled)
    }

    @Test
    fun `onPacketReceived defaults state to true when missing`() {
        // getBoolean with default=true when key is missing
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.keyboardstate",
            body = emptyMap()
        )
        plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(plugin.isKeyboardEnabled)
    }

    @Test
    fun `onPacketReceived always returns true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mousepad.keyboardstate",
            body = mapOf("state" to false)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    @Test
    fun `onPacketReceived returns true even for wrong packet type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains keyboard state`() {
        assertArrayEquals(
            arrayOf("cconnect.mousepad.keyboardstate"),
            plugin.supportedPacketTypes
        )
    }

    @Test
    fun `outgoingPacketTypes contains mousepad request`() {
        assertArrayEquals(
            arrayOf("cconnect.mousepad.request"),
            plugin.outgoingPacketTypes
        )
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }

    // ========================================================================
    // UI buttons
    // ========================================================================

    @Test
    fun `getUiButtons returns one button`() {
        assertEquals(1, plugin.getUiButtons().size)
    }
}
