/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.NetworkInfoPlugin

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
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
class NetworkInfoPluginTest {

    private lateinit var plugin: NetworkInfoPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = NetworkInfoPlugin(context, mockDevice)
    }

    // ========================================================================
    // Packet type arrays
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains networkinfo request`() {
        assertEquals(
            listOf("cconnect.networkinfo.request"),
            plugin.supportedPacketTypes.toList()
        )
    }

    @Test
    fun `outgoingPacketTypes contains networkinfo`() {
        assertEquals(
            listOf("cconnect.networkinfo"),
            plugin.outgoingPacketTypes.toList()
        )
    }

    // ========================================================================
    // onPacketReceived
    // ========================================================================

    @Test
    fun `request packet triggers network info response`() {
        plugin.onCreate()

        val request = TransferPacket(
            NetworkPacket(
                id = 1L,
                type = "cconnect.networkinfo.request",
                body = emptyMap()
            )
        )
        val result = plugin.onPacketReceived(request)
        assertTrue(result)

        // Should have sent at least one packet (initial state + response to request)
        verify(atLeast = 1) { mockDevice.sendPacket(any()) }
    }

    @Test
    fun `wrong packet type returns false`() {
        plugin.onCreate()

        val packet = TransferPacket(
            NetworkPacket(
                id = 1L,
                type = "cconnect.battery",
                body = emptyMap()
            )
        )
        val result = plugin.onPacketReceived(packet)
        assertFalse(result)
    }

    @Test
    fun `onCreate sends initial network state`() {
        val result = plugin.onCreate()
        assertTrue(result)

        // Should send initial state on creation
        verify(atLeast = 1) { mockDevice.sendPacket(any()) }
    }

    @Test
    fun `initial packet contains connected field`() {
        val packetSlot = slot<TransferPacket>()

        plugin.onCreate()

        verify(atLeast = 1) { mockDevice.sendPacket(capture(packetSlot)) }
        val sentPacket = packetSlot.captured.packet
        assertEquals("cconnect.networkinfo", sentPacket.type)
        assertTrue(sentPacket.body.containsKey("connected"))
    }

    @Test
    fun `onDestroy does not crash when called before onCreate`() {
        // Should not throw
        plugin.onDestroy()
    }

    @Test
    fun `onDestroy does not crash when called twice`() {
        plugin.onCreate()
        plugin.onDestroy()
        // Should not throw
        plugin.onDestroy()
    }

    // ========================================================================
    // Extension properties
    // ========================================================================

    @Test
    fun `isNetworkInfoPacket true for valid packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true, "networkType" to "WiFi")
        )
        assertTrue(packet.isNetworkInfoPacket)
    }

    @Test
    fun `isNetworkInfoPacket false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("connected" to true)
        )
        assertFalse(packet.isNetworkInfoPacket)
    }

    @Test
    fun `isNetworkInfoPacket false when missing connected field`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("networkType" to "WiFi")
        )
        assertFalse(packet.isNetworkInfoPacket)
    }

    @Test
    fun `isNetworkInfoRequest true for request packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo.request",
            body = emptyMap()
        )
        assertTrue(packet.isNetworkInfoRequest)
    }

    @Test
    fun `networkInfoConnected extracts value`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true)
        )
        assertEquals(true, packet.networkInfoConnected)
    }

    @Test
    fun `networkInfoConnected null for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("connected" to true)
        )
        assertNull(packet.networkInfoConnected)
    }

    @Test
    fun `networkInfoType extracts value`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true, "networkType" to "WiFi")
        )
        assertEquals("WiFi", packet.networkInfoType)
    }

    @Test
    fun `networkInfoWifiSsid extracts value`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true, "wifiSsid" to "MyNetwork")
        )
        assertEquals("MyNetwork", packet.networkInfoWifiSsid)
    }

    @Test
    fun `networkInfoWifiSignalStrength extracts value`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true, "wifiSignalStrength" to 3)
        )
        assertEquals(3, packet.networkInfoWifiSignalStrength)
    }

    @Test
    fun `networkInfoIsMetered extracts value`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true, "isMetered" to false)
        )
        assertEquals(false, packet.networkInfoIsMetered)
    }

    @Test
    fun `networkInfoIsVpn extracts value`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to true, "isVpn" to true)
        )
        assertEquals(true, packet.networkInfoIsVpn)
    }

    @Test
    fun `disconnected packet has connected false and no other fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.networkinfo",
            body = mapOf("connected" to false)
        )
        assertTrue(packet.isNetworkInfoPacket)
        assertEquals(false, packet.networkInfoConnected)
        assertNull(packet.networkInfoType)
        assertNull(packet.networkInfoWifiSsid)
        assertNull(packet.networkInfoIsMetered)
    }
}
