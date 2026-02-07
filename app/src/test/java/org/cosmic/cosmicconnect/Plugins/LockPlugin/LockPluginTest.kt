/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.LockPlugin

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class LockPluginTest {

    private lateinit var plugin: LockPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        plugin = LockPlugin(context, mockDevice)
    }

    // ========================================================================
    // onPacketReceived — lock status
    // ========================================================================

    @Test
    fun `onPacketReceived updates isRemoteLocked to true`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.lock", body = mapOf("isLocked" to true))
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(true, plugin.isRemoteLocked)
    }

    @Test
    fun `onPacketReceived updates isRemoteLocked to false`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.lock", body = mapOf("isLocked" to false))
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(false, plugin.isRemoteLocked)
    }

    @Test
    fun `onPacketReceived calls onPluginsChanged`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.lock", body = mapOf("isLocked" to true))
        plugin.onPacketReceived(TransferPacket(packet))
        verify { mockDevice.onPluginsChanged() }
    }

    @Test
    fun `onPacketReceived returns false for wrong type`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.ping", body = mapOf("isLocked" to true))
        assertFalse(plugin.onPacketReceived(TransferPacket(packet)))
    }

    @Test
    fun `onPacketReceived returns true for lock type`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.lock", body = mapOf("isLocked" to true))
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
    }

    @Test
    fun `onPacketReceived returns true for lock type with missing isLocked`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.lock", body = emptyMap())
        assertTrue(plugin.onPacketReceived(TransferPacket(packet)))
    }

    @Test
    fun `isRemoteLocked is null initially`() {
        assertNull(plugin.isRemoteLocked)
    }

    @Test
    fun `onPacketReceived preserves last known state`() {
        val locked = NetworkPacket(id = 1L, type = "cconnect.lock", body = mapOf("isLocked" to true))
        plugin.onPacketReceived(TransferPacket(locked))
        assertEquals(true, plugin.isRemoteLocked)

        val unlocked = NetworkPacket(id = 2L, type = "cconnect.lock", body = mapOf("isLocked" to false))
        plugin.onPacketReceived(TransferPacket(unlocked))
        assertEquals(false, plugin.isRemoteLocked)
    }

    // ========================================================================
    // sendLockCommand
    // ========================================================================

    @Test
    fun `sendLockCommand sends lock request with setLocked true`() {
        plugin.sendLockCommand(true)
        verify {
            mockDevice.sendPacket(match { tp ->
                tp.packet.type == "cconnect.lock.request" &&
                tp.packet.body["setLocked"] == true
            })
        }
    }

    @Test
    fun `sendLockCommand sends unlock request with setLocked false`() {
        plugin.sendLockCommand(false)
        verify {
            mockDevice.sendPacket(match { tp ->
                tp.packet.type == "cconnect.lock.request" &&
                tp.packet.body["setLocked"] == false
            })
        }
    }

    // ========================================================================
    // requestLockStatus
    // ========================================================================

    @Test
    fun `requestLockStatus sends request with requestLocked`() {
        plugin.requestLockStatus()
        verify {
            mockDevice.sendPacket(match { tp ->
                tp.packet.type == "cconnect.lock.request" &&
                tp.packet.body["requestLocked"] == true
            })
        }
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains lock`() {
        assertArrayEquals(arrayOf("cconnect.lock"), plugin.supportedPacketTypes)
    }

    @Test
    fun `outgoingPacketTypes contains lock request`() {
        assertArrayEquals(arrayOf("cconnect.lock.request"), plugin.outgoingPacketTypes)
    }

    // ========================================================================
    // UI menu entries
    // ========================================================================

    @Test
    fun `getUiMenuEntries returns 2 entries`() {
        assertEquals(2, plugin.getUiMenuEntries().size)
    }
}
