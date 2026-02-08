/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.VirtualMonitorPlugin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ScreenSharePlugin
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for VirtualMonitorPlugin.
 *
 * Tests virtual monitor status reception, enable/disable requests, and state management.
 */
@RunWith(RobolectricTestRunner::class)
class VirtualMonitorPluginTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var plugin: VirtualMonitorPlugin

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        device = mockk(relaxed = true)
        // Explicitly return null to avoid ClassCastException from relaxed mock's generic Plugin return
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns null
        plugin = VirtualMonitorPlugin(context, device)
    }

    // ========== Status Packet Reception Tests ==========

    @Test
    fun `receive status packet - active with full config`() {
        val np = NetworkPacket(
            id = 1L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
                "dpi" to 240,
                "position" to "right",
                "refreshRate" to 60,
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertTrue(result)
        assertEquals(true, plugin.isActive)
        assertEquals(1920, plugin.width)
        assertEquals(1080, plugin.height)
        assertEquals(240, plugin.dpi)
        assertEquals("right", plugin.position)
        assertEquals(60, plugin.refreshRate)
    }

    @Test
    fun `receive status packet - inactive`() {
        val np = NetworkPacket(
            id = 2L,
            type = "cconnect.virtualmonitor",
            body = mapOf("isActive" to false),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertTrue(result)
        assertEquals(false, plugin.isActive)
        assertNull(plugin.width)
        assertNull(plugin.height)
    }

    @Test
    fun `receive status packet - active with partial config`() {
        val np = NetworkPacket(
            id = 3L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 2560,
                "height" to 1440,
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertTrue(result)
        assertEquals(true, plugin.isActive)
        assertEquals(2560, plugin.width)
        assertEquals(1440, plugin.height)
        assertNull(plugin.dpi)
        assertNull(plugin.position)
        assertNull(plugin.refreshRate)
    }

    @Test
    fun `receive status packet - position left`() {
        val np = NetworkPacket(
            id = 4L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "position" to "left",
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        assertEquals("left", plugin.position)
    }

    @Test
    fun `receive status packet - position above`() {
        val np = NetworkPacket(
            id = 5L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "position" to "above",
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        assertEquals("above", plugin.position)
    }

    @Test
    fun `receive status packet - position below`() {
        val np = NetworkPacket(
            id = 6L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "position" to "below",
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        assertEquals("below", plugin.position)
    }

    @Test
    fun `receive status packet - high DPI config`() {
        val np = NetworkPacket(
            id = 7L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 3840,
                "height" to 2160,
                "dpi" to 480,
                "refreshRate" to 120,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        assertEquals(3840, plugin.width)
        assertEquals(2160, plugin.height)
        assertEquals(480, plugin.dpi)
        assertEquals(120, plugin.refreshRate)
    }

    // ========== Missing Field Handling Tests ==========

    @Test
    fun `receive status packet - missing isActive defaults to null`() {
        val np = NetworkPacket(
            id = 8L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "width" to 1920,
                "height" to 1080,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        assertNull(plugin.isActive)
        assertEquals(1920, plugin.width)
        assertEquals(1080, plugin.height)
    }

    @Test
    fun `receive status packet - empty body`() {
        val np = NetworkPacket(
            id = 9L,
            type = "cconnect.virtualmonitor",
            body = emptyMap(),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertTrue(result)
        assertNull(plugin.isActive)
        assertNull(plugin.width)
        assertNull(plugin.height)
    }

    // ========== Listener Notification Tests ==========

    @Test
    fun `listener notified on status change`() {
        val listener = mockk<VirtualMonitorPlugin.VirtualMonitorStateListener>(relaxed = true)
        plugin.setVirtualMonitorStateListener(listener)

        val np = NetworkPacket(
            id = 10L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
                "dpi" to 240,
                "position" to "right",
                "refreshRate" to 60,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify {
            listener.onVirtualMonitorStateChanged(true, 1920, 1080, 240, "right", 60)
        }
    }

    @Test
    fun `listener notified with nulls for missing fields`() {
        val listener = mockk<VirtualMonitorPlugin.VirtualMonitorStateListener>(relaxed = true)
        plugin.setVirtualMonitorStateListener(listener)

        val np = NetworkPacket(
            id = 11L,
            type = "cconnect.virtualmonitor",
            body = mapOf("isActive" to false),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify {
            listener.onVirtualMonitorStateChanged(false, null, null, null, null, null)
        }
    }

    @Test
    fun `set listener to null - no crash on packet`() {
        plugin.setVirtualMonitorStateListener(null)

        val np = NetworkPacket(
            id = 12L,
            type = "cconnect.virtualmonitor",
            body = mapOf("isActive" to true),
        )

        // Should not throw exception
        plugin.onPacketReceived(TransferPacket(np))
    }

    // ========== Plugin Metadata Tests ==========

    @Test
    fun `plugin metadata is correct`() {
        assertArrayEquals(
            arrayOf("cconnect.virtualmonitor"),
            plugin.supportedPacketTypes
        )
        assertArrayEquals(
            arrayOf("cconnect.virtualmonitor.request"),
            plugin.outgoingPacketTypes
        )
        assertFalse(plugin.isEnabledByDefault)
    }

    @Test
    fun `plugin display name and description are non-empty`() {
        assertTrue(plugin.displayName.isNotEmpty())
        assertTrue(plugin.description.isNotEmpty())
    }

    // ========== Wrong Packet Type Tests ==========

    @Test
    fun `wrong packet type returns false`() {
        val np = NetworkPacket(
            id = 13L,
            type = "cconnect.ping",
            body = emptyMap(),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertFalse(result)
    }

    @Test
    fun `request packet type returns false`() {
        val np = NetworkPacket(
            id = 14L,
            type = "cconnect.virtualmonitor.request",
            body = mapOf("enableMonitor" to true),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertFalse(result)
    }

    // ========== Always Returns True Tests ==========

    @Test
    fun `onPacketReceived always returns true for correct type`() {
        val packets = listOf(
            NetworkPacket(id = 15L, type = "cconnect.virtualmonitor", body = mapOf("isActive" to true)),
            NetworkPacket(id = 16L, type = "cconnect.virtualmonitor", body = emptyMap()),
            NetworkPacket(id = 17L, type = "cconnect.virtualmonitor", body = mapOf("invalid" to "data")),
        )

        packets.forEach { np ->
            assertTrue(plugin.onPacketReceived(TransferPacket(np)))
        }
    }
}
