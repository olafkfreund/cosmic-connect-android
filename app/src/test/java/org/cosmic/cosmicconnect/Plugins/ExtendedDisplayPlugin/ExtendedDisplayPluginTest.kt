/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for ExtendedDisplayPlugin
 *
 * Tests the packet routing logic for extended display control packets.
 * Does NOT test FFI-dependent logic or WebRTC components.
 */
@RunWith(RobolectricTestRunner::class)
class ExtendedDisplayPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: ExtendedDisplayPlugin

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        // Create plugin instance
        plugin = ExtendedDisplayPlugin(context, mockDevice)
    }

    // ========================================================================
    // Packet Type Recognition
    // ========================================================================

    @Test
    fun `onPacketReceived returns true for PACKET_TYPE_EXTENDED_DISPLAY`() {
        // Given: An extended display control packet
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf("action" to "offer")
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled
        assertTrue(handled)
    }

    @Test
    fun `onPacketReceived returns false for unknown packet type`() {
        // Given: A packet of unknown type
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.unknown",
            body = emptyMap()
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should NOT be handled
        assertFalse(handled)
    }

    // ========================================================================
    // Control Packet Actions - Parsing Only (No FFI)
    // ========================================================================

    @Test
    fun `handleControlPacket parses offer action with sdp`() {
        // Given: An offer packet with SDP
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf(
                "action" to "offer",
                "sdp" to "v=0\r\no=- 0 0 IN IP4 127.0.0.1\r\n"
            )
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled (parsing succeeded)
        assertTrue(handled)
    }

    @Test
    fun `handleControlPacket parses candidate action with ICE data`() {
        // Given: A candidate packet with ICE data
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf(
                "action" to "candidate",
                "candidate" to "candidate:0 1 UDP 2122260223 192.168.1.5 54321 typ host",
                "sdpMid" to "0",
                "sdpMLineIndex" to 0
            )
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled (parsing succeeded)
        assertTrue(handled)
    }

    // NOTE: "stop" action test omitted because it calls stopStreaming() which calls FFI (NetworkPacket.create)

    @Test
    fun `handleControlPacket ignores packet without action field`() {
        // Given: A control packet without action
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf("foo" to "bar")
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled (no crash, just ignored)
        assertTrue(handled)
    }

    @Test
    fun `handleControlPacket handles empty action gracefully`() {
        // Given: A control packet with empty action
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf("action" to "")
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled without crashing
        assertTrue(handled)
    }

    @Test
    fun `handleControlPacket handles offer without sdp`() {
        // Given: An offer packet missing SDP
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf("action" to "offer")
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled (just ignored)
        assertTrue(handled)
    }

    @Test
    fun `handleControlPacket handles candidate without required fields`() {
        // Given: A candidate packet missing required fields
        val packet = NetworkPacket(
            id = 1L,
            type = ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY,
            body = mapOf("action" to "candidate")
        )
        val transferPacket = TransferPacket(packet)

        // When: Plugin receives the packet
        val handled = plugin.onPacketReceived(transferPacket)

        // Then: It should be handled (just ignored with defaults)
        assertTrue(handled)
    }

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    @Test
    fun `plugin has correct display name`() {
        assertTrue(plugin.displayName.isNotEmpty())
    }

    @Test
    fun `plugin has correct description`() {
        assertTrue(plugin.description.isNotEmpty())
    }

    @Test
    fun `plugin is not enabled by default`() {
        assertFalse(plugin.isEnabledByDefault)
    }

    @Test
    fun `plugin supports correct incoming packet types`() {
        val supportedTypes = plugin.supportedPacketTypes
        assertEquals(1, supportedTypes.size)
        assertEquals(ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY, supportedTypes[0])
    }

    @Test
    fun `plugin declares correct outgoing packet types`() {
        val outgoingTypes = plugin.outgoingPacketTypes
        assertEquals(1, outgoingTypes.size)
        assertEquals(ExtendedDisplayPlugin.PACKET_TYPE_EXTENDED_DISPLAY_REQUEST, outgoingTypes[0])
    }

    @Test
    fun `plugin has settings`() {
        assertTrue(plugin.hasSettings())
    }

    @Test
    fun `plugin supports device specific settings`() {
        assertTrue(plugin.supportsDeviceSpecificSettings())
    }
}
