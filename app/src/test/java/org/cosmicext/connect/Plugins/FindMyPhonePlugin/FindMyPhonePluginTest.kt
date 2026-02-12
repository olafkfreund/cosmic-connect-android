/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FindMyPhonePlugin

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
class FindMyPhonePluginTest {

    private lateinit var plugin: FindMyPhonePlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = FindMyPhonePlugin(context, mockDevice)
        // Note: We do NOT call onCreate() — it uses Hilt EntryPoints which fail in Robolectric
    }

    // ========================================================================
    // Extension property — isFindMyPhoneRequest
    // ========================================================================

    @Test
    fun `isFindMyPhoneRequest true for correct type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.findmyphone.request",
            body = emptyMap()
        )
        assertTrue(packet.isFindMyPhoneRequest)
    }

    @Test
    fun `isFindMyPhoneRequest false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = emptyMap()
        )
        assertFalse(packet.isFindMyPhoneRequest)
    }

    @Test
    fun `isFindMyPhoneRequest false for similar but wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.findmyphone",
            body = emptyMap()
        )
        assertFalse(packet.isFindMyPhoneRequest)
    }

    // ========================================================================
    // onPacketReceived — packet type filtering
    // ========================================================================

    @Test
    fun `onPacketReceived with wrong type returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    @Test
    fun `onPacketReceived with ping type returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    // ========================================================================
    // Audio state — without onCreate()
    // ========================================================================

    @Test
    fun `isPlaying returns false when mediaPlayer not initialized`() {
        // mediaPlayer is null without calling onCreate()
        assertFalse(plugin.isPlaying())
    }

    @Test
    fun `stopPlaying does not crash when audioManager is null`() {
        // audioManager is null without calling onCreate()
        plugin.stopPlaying() // Should not throw
    }

    @Test
    fun `startPlaying does not crash when mediaPlayer is null`() {
        // mediaPlayer is null without calling onCreate()
        plugin.startPlaying() // Should not throw
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains findmyphone request`() {
        assertArrayEquals(
            arrayOf("cconnect.findmyphone.request"),
            plugin.supportedPacketTypes
        )
    }

    @Test
    fun `outgoingPacketTypes is empty`() {
        assertEquals(0, plugin.outgoingPacketTypes.size)
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }

    @Test
    fun `PACKET_TYPE constant has correct value`() {
        assertEquals("cconnect.findmyphone.request", FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST)
    }
}
