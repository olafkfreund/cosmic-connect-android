package org.cosmicext.connect.Plugins.DigitizerPlugin

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DigitizerPluginTest {
    private lateinit var context: Application
    private lateinit var mockDevice: Device
    private lateinit var plugin: DigitizerPlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        plugin = DigitizerPlugin(context, mockDevice)
    }

    @Test
    fun `supportedPacketTypes is empty`() {
        assertEquals(0, plugin.supportedPacketTypes.size)
    }

    @Test
    fun `outgoingPacketTypes has digitizer session and digitizer`() {
        val outgoing = plugin.outgoingPacketTypes
        assertEquals(2, outgoing.size)
        assertTrue(outgoing.contains("cconnect.digitizer.session"))
        assertTrue(outgoing.contains("cconnect.digitizer"))
    }

    @Test
    fun `displayName is non-empty`() {
        val displayName = plugin.displayName
        assertNotNull(displayName)
        assertTrue(displayName.isNotEmpty())
    }

    @Test
    fun `description is non-empty`() {
        val description = plugin.description
        assertNotNull(description)
        assertTrue(description.isNotEmpty())
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }

    @Test
    fun `onPacketReceived returns false for any packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.digitizer",
            body = mapOf("action" to "test")
        )
        val transferPacket = TransferPacket(packet)

        assertFalse(plugin.onPacketReceived(transferPacket))
    }
}
