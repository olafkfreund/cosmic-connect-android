/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.SystemVolumePlugin

import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
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
class SystemVolumePluginTest {

    private lateinit var plugin: SystemVolumePlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        plugin = SystemVolumePlugin(context, mockDevice)
    }

    private fun sinkJson(
        name: String = "Main Output",
        volume: Int = 75,
        muted: Boolean = false,
        description: String = "Speakers",
        maxVolume: Int = 100,
        enabled: Boolean = true
    ): JSONObject = JSONObject().apply {
        put("name", name)
        put("volume", volume)
        put("muted", muted)
        put("description", description)
        put("maxVolume", maxVolume)
        put("enabled", enabled)
    }

    private fun sinkListPacket(vararg sinks: JSONObject): NetworkPacket {
        val array = JSONArray()
        sinks.forEach { array.put(it) }
        return NetworkPacket(
            id = 1L,
            type = "cconnect.systemvolume",
            body = mapOf("sinkList" to array.toString())
        )
    }

    // ========================================================================
    // Sink class — construction and state
    // ========================================================================

    @Test
    fun `Sink constructed from JSONObject has correct properties`() {
        val json = sinkJson(
            name = "Headphones", volume = 50, muted = true,
            description = "USB Headphones", maxVolume = 150, enabled = false
        )
        val sink = Sink(json)
        assertEquals("Headphones", sink.name)
        assertEquals(50, sink.volume)
        assertTrue(sink.mute)
        assertEquals("USB Headphones", sink.description)
        assertEquals(150, sink.maxVolume)
        assertFalse(sink.isDefault)
    }

    @Test
    fun `Sink setVolume updates volume and notifies listener`() {
        val sink = Sink(sinkJson())
        var notified = false
        sink.addListener(object : Sink.UpdateListener {
            override fun updateSink(s: Sink) { notified = true }
        })
        sink.setVolume(30)
        assertEquals(30, sink.volume)
        assertTrue(notified)
    }

    @Test
    fun `Sink setMute updates mute and notifies listener`() {
        val sink = Sink(sinkJson(muted = false))
        var notified = false
        sink.addListener(object : Sink.UpdateListener {
            override fun updateSink(s: Sink) { notified = true }
        })
        sink.setMute(true)
        assertTrue(sink.mute)
        assertTrue(notified)
    }

    @Test
    fun `Sink isDefault setter updates enabled and notifies listener`() {
        val sink = Sink(sinkJson(enabled = false))
        var notified = false
        sink.addListener(object : Sink.UpdateListener {
            override fun updateSink(s: Sink) { notified = true }
        })
        sink.isDefault = true
        assertTrue(sink.isDefault)
        assertTrue(notified)
    }

    @Test
    fun `Sink isMute returns mute state`() {
        val mutedSink = Sink(sinkJson(muted = true))
        assertTrue(mutedSink.isMute())
        val unmutedSink = Sink(sinkJson(muted = false))
        assertFalse(unmutedSink.isMute())
    }

    @Test
    fun `Sink addListener does not add duplicate`() {
        val sink = Sink(sinkJson())
        var count = 0
        val listener = object : Sink.UpdateListener {
            override fun updateSink(s: Sink) { count++ }
        }
        sink.addListener(listener)
        sink.addListener(listener) // duplicate
        sink.setVolume(10)
        assertEquals(1, count)
    }

    @Test
    fun `Sink removeListener stops notifications`() {
        val sink = Sink(sinkJson())
        var count = 0
        val listener = object : Sink.UpdateListener {
            override fun updateSink(s: Sink) { count++ }
        }
        sink.addListener(listener)
        sink.setVolume(10)
        assertEquals(1, count)
        sink.removeListener(listener)
        sink.setVolume(20)
        assertEquals(1, count)
    }

    // ========================================================================
    // onPacketReceived — sinkList parsing
    // ========================================================================

    @Test
    fun `onPacketReceived parses single sink from sinkList`() {
        val packet = sinkListPacket(sinkJson(name = "Main"))
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(1, plugin.sinks.size)
        assertNotNull(plugin.sinks["Main"])
        assertEquals(75, plugin.sinks["Main"]!!.volume)
    }

    @Test
    fun `onPacketReceived parses multiple sinks from sinkList`() {
        val packet = sinkListPacket(
            sinkJson(name = "Main", volume = 75),
            sinkJson(name = "Headphones", volume = 50)
        )
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(2, plugin.sinks.size)
        assertEquals(75, plugin.sinks["Main"]!!.volume)
        assertEquals(50, plugin.sinks["Headphones"]!!.volume)
    }

    @Test
    fun `onPacketReceived clears existing sinks on new sinkList`() {
        val packet1 = sinkListPacket(sinkJson(name = "Old Sink"))
        plugin.onPacketReceived(TransferPacket(packet1))
        assertEquals(1, plugin.sinks.size)

        val packet2 = sinkListPacket(sinkJson(name = "New Sink"))
        plugin.onPacketReceived(TransferPacket(packet2))
        assertEquals(1, plugin.sinks.size)
        assertNull(plugin.sinks["Old Sink"])
        assertNotNull(plugin.sinks["New Sink"])
    }

    @Test
    fun `onPacketReceived handles empty sinkList`() {
        val packet1 = sinkListPacket(sinkJson(name = "Main"))
        plugin.onPacketReceived(TransferPacket(packet1))
        assertEquals(1, plugin.sinks.size)

        val packet2 = NetworkPacket(
            id = 2L,
            type = "cconnect.systemvolume",
            body = mapOf("sinkList" to "[]")
        )
        plugin.onPacketReceived(TransferPacket(packet2))
        assertEquals(0, plugin.sinks.size)
    }

    @Test
    fun `onPacketReceived handles invalid JSON in sinkList`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.systemvolume",
            body = mapOf("sinkList" to "not valid json")
        )
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(0, plugin.sinks.size)
    }

    @Test
    fun `onPacketReceived notifies SinkListener on sinkList`() {
        var notified = false
        plugin.addSinkListener { notified = true }

        val packet = sinkListPacket(sinkJson(name = "Main"))
        plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(notified)
    }

    @Test
    fun `onPacketReceived notifies multiple SinkListeners`() {
        var count = 0
        plugin.addSinkListener { count++ }
        plugin.addSinkListener { count++ }

        val packet = sinkListPacket(sinkJson(name = "Main"))
        plugin.onPacketReceived(TransferPacket(packet))
        assertEquals(2, count)
    }

    // ========================================================================
    // onPacketReceived — individual sink updates
    // ========================================================================

    @Test
    fun `onPacketReceived updates volume on existing sink`() {
        val setupPacket = sinkListPacket(sinkJson(name = "Main", volume = 75))
        plugin.onPacketReceived(TransferPacket(setupPacket))

        val updatePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.systemvolume",
            body = mapOf("name" to "Main", "volume" to 30)
        )
        plugin.onPacketReceived(TransferPacket(updatePacket))
        assertEquals(30, plugin.sinks["Main"]!!.volume)
    }

    @Test
    fun `onPacketReceived updates mute on existing sink`() {
        val setupPacket = sinkListPacket(sinkJson(name = "Main", muted = false))
        plugin.onPacketReceived(TransferPacket(setupPacket))

        val updatePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.systemvolume",
            body = mapOf("name" to "Main", "muted" to true)
        )
        plugin.onPacketReceived(TransferPacket(updatePacket))
        assertTrue(plugin.sinks["Main"]!!.mute)
    }

    @Test
    fun `onPacketReceived updates enabled on existing sink`() {
        val setupPacket = sinkListPacket(sinkJson(name = "Main", enabled = false))
        plugin.onPacketReceived(TransferPacket(setupPacket))

        val updatePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.systemvolume",
            body = mapOf("name" to "Main", "enabled" to true)
        )
        plugin.onPacketReceived(TransferPacket(updatePacket))
        assertTrue(plugin.sinks["Main"]!!.isDefault)
    }

    @Test
    fun `onPacketReceived updates multiple fields at once`() {
        val setupPacket = sinkListPacket(sinkJson(name = "Main", volume = 75, muted = false))
        plugin.onPacketReceived(TransferPacket(setupPacket))

        val updatePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.systemvolume",
            body = mapOf("name" to "Main", "volume" to 10, "muted" to true)
        )
        plugin.onPacketReceived(TransferPacket(updatePacket))
        assertEquals(10, plugin.sinks["Main"]!!.volume)
        assertTrue(plugin.sinks["Main"]!!.mute)
    }

    @Test
    fun `onPacketReceived ignores update for non-existent sink`() {
        val updatePacket = NetworkPacket(
            id = 1L,
            type = "cconnect.systemvolume",
            body = mapOf("name" to "NonExistent", "volume" to 50)
        )
        plugin.onPacketReceived(TransferPacket(updatePacket))
        assertEquals(0, plugin.sinks.size)
    }

    @Test
    fun `onPacketReceived always returns true`() {
        val sinkPacket = sinkListPacket(sinkJson())
        assertTrue(plugin.onPacketReceived(TransferPacket(sinkPacket)))

        val updatePacket = NetworkPacket(
            id = 2L,
            type = "cconnect.systemvolume",
            body = mapOf("name" to "Missing", "volume" to 50)
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(updatePacket)))
    }

    // ========================================================================
    // Listener management
    // ========================================================================

    @Test
    fun `removeSinkListener stops notifications`() {
        var count = 0
        val listener = SystemVolumePlugin.SinkListener { count++ }
        plugin.addSinkListener(listener)

        val packet1 = sinkListPacket(sinkJson())
        plugin.onPacketReceived(TransferPacket(packet1))
        assertEquals(1, count)

        plugin.removeSinkListener(listener)
        val packet2 = sinkListPacket(sinkJson())
        plugin.onPacketReceived(TransferPacket(packet2))
        assertEquals(1, count)
    }

    @Test
    fun `getSinks returns sink values`() {
        val packet = sinkListPacket(sinkJson(name = "A"), sinkJson(name = "B"))
        plugin.onPacketReceived(TransferPacket(packet))
        val sinkNames = plugin.getSinks().map { it.name }.toSet()
        assertEquals(setOf("A", "B"), sinkNames)
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains systemvolume`() {
        assertArrayEquals(
            arrayOf("cconnect.systemvolume"),
            plugin.supportedPacketTypes
        )
    }

    @Test
    fun `outgoingPacketTypes contains systemvolume request`() {
        assertArrayEquals(
            arrayOf("cconnect.systemvolume.request"),
            plugin.outgoingPacketTypes
        )
    }
}
