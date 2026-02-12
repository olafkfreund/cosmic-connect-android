package org.cosmicext.connect.Plugins.ConnectivityReportPlugin

import android.Manifest
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
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ConnectivityReportPluginTest {
    private lateinit var context: Application
    private lateinit var mockDevice: Device
    private lateinit var plugin: ConnectivityReportPlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        plugin = ConnectivityReportPlugin(context, mockDevice)
    }

    @Test
    fun `supportedPacketTypes is empty`() {
        assertEquals(0, plugin.supportedPacketTypes.size)
    }

    @Test
    fun `outgoingPacketTypes contains connectivity_report`() {
        val outgoing = plugin.outgoingPacketTypes
        assertEquals(1, outgoing.size)
        assertEquals("cconnect.connectivity_report", outgoing[0])
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
    fun `onPacketReceived always returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.connectivity_report",
            body = mapOf()
        )
        val transferPacket = TransferPacket(packet)

        assertFalse(plugin.onPacketReceived(transferPacket))
    }

    @Test
    fun `checkRequiredPermissions returns true when READ_PHONE_STATE granted`() {
        // Grant the required permission
        shadowOf(context).grantPermissions(Manifest.permission.READ_PHONE_STATE)

        // checkRequiredPermissions should return true
        assertTrue(plugin.checkRequiredPermissions())
    }
}
