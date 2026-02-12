package org.cosmicext.connect.Plugins.FindRemoteDevicePlugin

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmicext.connect.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Test FindRemoteDevicePlugin which allows this device to trigger "find my phone"
 * on a remote device. This plugin sends findmyphone.request packets but doesn't
 * receive any packets.
 */
@RunWith(RobolectricTestRunner::class)
class FindRemoteDevicePluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: FindRemoteDevicePlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk<Device>(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = FindRemoteDevicePlugin(context, mockDevice)
    }

    @Test
    fun `supportedPacketTypes is empty`() {
        val supportedTypes = plugin.supportedPacketTypes

        assertNotNull(supportedTypes)
        assertEquals(0, supportedTypes.size)
    }

    @Test
    fun `outgoingPacketTypes contains findmyphone request`() {
        val outgoingTypes = plugin.outgoingPacketTypes

        assertNotNull(outgoingTypes)
        assertEquals(1, outgoingTypes.size)
        assertArrayEquals(
            arrayOf(FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST),
            outgoingTypes
        )
    }

    @Test
    fun `onPacketReceived always returns true for any packet type`() {
        val packet1 = NetworkPacket(
            id = 1L,
            type = "kdeconnect.test",
            body = mapOf("key" to "value")
        )

        val packet2 = NetworkPacket(
            id = 2L,
            type = "cconnect.other",
            body = emptyMap()
        )

        val packet3 = NetworkPacket(
            id = 3L,
            type = FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST,
            body = emptyMap()
        )

        assertTrue(plugin.onPacketReceived(TransferPacket(packet1)))
        assertTrue(plugin.onPacketReceived(TransferPacket(packet2)))
        assertTrue(plugin.onPacketReceived(TransferPacket(packet3)))
    }

    @Test
    fun `displayName is non-empty`() {
        val displayName = plugin.displayName

        assertNotNull(displayName)
        assertTrue(displayName.isNotEmpty())
        assertEquals(context.getString(R.string.pref_plugin_findremotedevice), displayName)
    }

    @Test
    fun `description is non-empty`() {
        val description = plugin.description

        assertNotNull(description)
        assertTrue(description.isNotEmpty())
        assertEquals(context.getString(R.string.pref_plugin_findremotedevice_desc), description)
    }

    @Test
    fun `pluginKey is FindRemoteDevicePlugin`() {
        val pluginKey = plugin.pluginKey

        assertEquals("FindRemoteDevicePlugin", pluginKey)
    }
}
