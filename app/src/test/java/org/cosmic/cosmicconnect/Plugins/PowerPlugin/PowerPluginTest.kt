/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.PowerPlugin

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PowerPluginTest {

    private lateinit var plugin: PowerPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Desktop"

        plugin = PowerPlugin(context, mockDevice)
    }

    // ========================================================================
    // Packet type arrays
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains power status`() {
        assertEquals(
            listOf("cconnect.power"),
            plugin.supportedPacketTypes.toList()
        )
    }

    @Test
    fun `outgoingPacketTypes contains power request`() {
        assertEquals(
            listOf("cconnect.power.request"),
            plugin.outgoingPacketTypes.toList()
        )
    }

    // ========================================================================
    // onPacketReceived — power status from desktop
    // ========================================================================

    @Test
    fun `receiving power status updates remoteStatus`() {
        val packet = TransferPacket(
            NetworkPacket(
                id = 1L,
                type = "cconnect.power",
                body = mapOf(
                    "hasBattery" to true,
                    "batteryCharge" to 72,
                    "isCharging" to false,
                    "isLidClosed" to false,
                )
            )
        )
        val result = plugin.onPacketReceived(packet)
        assertTrue(result)

        val status = plugin.remoteStatus
        assertNotNull(status)
        assertTrue(status!!.hasBattery)
        assertEquals(72, status.batteryCharge)
        assertEquals(false, status.isCharging)
        assertEquals(false, status.isLidClosed)
    }

    @Test
    fun `receiving power status calls onPluginsChanged`() {
        val packet = TransferPacket(
            NetworkPacket(
                id = 1L,
                type = "cconnect.power",
                body = mapOf("hasBattery" to false)
            )
        )
        plugin.onPacketReceived(packet)
        verify { mockDevice.onPluginsChanged() }
    }

    @Test
    fun `remoteStatus is null before any packet received`() {
        assertNull(plugin.remoteStatus)
    }

    @Test
    fun `wrong packet type returns false`() {
        val packet = TransferPacket(
            NetworkPacket(
                id = 1L,
                type = "cconnect.battery",
                body = emptyMap()
            )
        )
        assertFalse(plugin.onPacketReceived(packet))
    }

    // ========================================================================
    // sendPowerCommand
    // ========================================================================

    @Test
    fun `sendPowerCommand sends shutdown packet`() {
        val packetSlot = slot<TransferPacket>()

        plugin.sendPowerCommand(PowerPlugin.ACTION_SHUTDOWN)

        verify { mockDevice.sendPacket(capture(packetSlot)) }
        val sent = packetSlot.captured.packet
        assertEquals("cconnect.power.request", sent.type)
        assertEquals("shutdown", sent.body["action"])
    }

    @Test
    fun `sendPowerCommand sends reboot packet`() {
        val packetSlot = slot<TransferPacket>()

        plugin.sendPowerCommand(PowerPlugin.ACTION_REBOOT)

        verify { mockDevice.sendPacket(capture(packetSlot)) }
        assertEquals("reboot", packetSlot.captured.packet.body["action"])
    }

    @Test
    fun `sendPowerCommand sends suspend packet`() {
        val packetSlot = slot<TransferPacket>()

        plugin.sendPowerCommand(PowerPlugin.ACTION_SUSPEND)

        verify { mockDevice.sendPacket(capture(packetSlot)) }
        assertEquals("suspend", packetSlot.captured.packet.body["action"])
    }

    @Test
    fun `sendPowerCommand sends hibernate packet`() {
        val packetSlot = slot<TransferPacket>()

        plugin.sendPowerCommand(PowerPlugin.ACTION_HIBERNATE)

        verify { mockDevice.sendPacket(capture(packetSlot)) }
        assertEquals("hibernate", packetSlot.captured.packet.body["action"])
    }

    // ========================================================================
    // UI menu entries
    // ========================================================================

    @Test
    fun `getUiMenuEntries returns four power actions`() {
        val entries = plugin.getUiMenuEntries()
        assertEquals(4, entries.size)
        assertEquals("Shutdown", entries[0].name)
        assertEquals("Reboot", entries[1].name)
        assertEquals("Suspend", entries[2].name)
        assertEquals("Hibernate", entries[3].name)
    }

    // ========================================================================
    // Extension properties
    // ========================================================================

    @Test
    fun `isPowerStatusPacket true for power type`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power", body = mapOf("hasBattery" to true)
        )
        assertTrue(packet.isPowerStatusPacket)
    }

    @Test
    fun `isPowerStatusPacket false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.battery", body = emptyMap()
        )
        assertFalse(packet.isPowerStatusPacket)
    }

    @Test
    fun `isPowerRequestPacket true for request with action`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power.request", body = mapOf("action" to "shutdown")
        )
        assertTrue(packet.isPowerRequestPacket)
    }

    @Test
    fun `isPowerRequestPacket false without action field`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power.request", body = emptyMap()
        )
        assertFalse(packet.isPowerRequestPacket)
    }

    @Test
    fun `powerHasBattery extracts value`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power", body = mapOf("hasBattery" to true)
        )
        assertEquals(true, packet.powerHasBattery)
    }

    @Test
    fun `powerBatteryCharge extracts value`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power",
            body = mapOf("hasBattery" to true, "batteryCharge" to 85)
        )
        assertEquals(85, packet.powerBatteryCharge)
    }

    @Test
    fun `powerIsCharging extracts value`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power",
            body = mapOf("isCharging" to true)
        )
        assertEquals(true, packet.powerIsCharging)
    }

    @Test
    fun `powerIsLidClosed extracts value`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power",
            body = mapOf("isLidClosed" to true)
        )
        assertEquals(true, packet.powerIsLidClosed)
    }

    @Test
    fun `powerAction extracts action string`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power.request",
            body = mapOf("action" to "reboot")
        )
        assertEquals("reboot", packet.powerAction)
    }

    @Test
    fun `powerAction null for non-request packet`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power",
            body = mapOf("action" to "reboot")
        )
        assertNull(packet.powerAction)
    }

    // ========================================================================
    // RemotePowerStatus
    // ========================================================================

    @Test
    fun `RemotePowerStatus fromPacket parses all fields`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power",
            body = mapOf(
                "hasBattery" to true,
                "batteryCharge" to 42,
                "isCharging" to true,
                "isLidClosed" to false,
            )
        )
        val status = RemotePowerStatus.fromPacket(packet)
        assertTrue(status.hasBattery)
        assertEquals(42, status.batteryCharge)
        assertEquals(true, status.isCharging)
        assertEquals(false, status.isLidClosed)
    }

    @Test
    fun `RemotePowerStatus fromPacket handles missing optional fields`() {
        val packet = NetworkPacket(
            id = 1L, type = "cconnect.power",
            body = emptyMap()
        )
        val status = RemotePowerStatus.fromPacket(packet)
        assertFalse(status.hasBattery)
        assertNull(status.batteryCharge)
        assertNull(status.isCharging)
        assertNull(status.isLidClosed)
    }

    // ========================================================================
    // Constants
    // ========================================================================

    @Test
    fun `VALID_ACTIONS contains all four actions`() {
        assertEquals(
            setOf("shutdown", "reboot", "suspend", "hibernate"),
            PowerPlugin.VALID_ACTIONS
        )
    }
}
