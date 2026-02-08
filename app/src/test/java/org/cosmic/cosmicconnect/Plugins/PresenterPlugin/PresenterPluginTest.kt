package org.cosmic.cosmicconnect.Plugins.PresenterPlugin

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PresenterPluginTest {
    private lateinit var context: Application
    private lateinit var mockDevice: Device
    private lateinit var plugin: PresenterPlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        every { mockDevice.deviceType } returns DeviceType.DESKTOP
        plugin = PresenterPlugin(context, mockDevice)
    }

    @Test
    fun `supportedPacketTypes is empty`() {
        assertEquals(0, plugin.supportedPacketTypes.size)
    }

    @Test
    fun `outgoingPacketTypes has mousepad request and presenter`() {
        val outgoing = plugin.outgoingPacketTypes
        assertEquals(2, outgoing.size)
        assertTrue(outgoing.contains("cconnect.mousepad.request"))
        assertTrue(outgoing.contains("cconnect.presenter"))
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
    fun `hasSettings returns false`() {
        assertFalse(plugin.hasSettings())
    }

    @Test
    fun `isCompatible returns true for DESKTOP device`() {
        every { mockDevice.deviceType } returns DeviceType.DESKTOP
        val desktopPlugin = PresenterPlugin(context, mockDevice)
        assertTrue(desktopPlugin.isCompatible)
    }

    @Test
    fun `isCompatible returns true for LAPTOP device`() {
        every { mockDevice.deviceType } returns DeviceType.LAPTOP
        val laptopPlugin = PresenterPlugin(context, mockDevice)
        assertTrue(laptopPlugin.isCompatible)
    }

    @Test
    fun `isCompatible returns false for PHONE device`() {
        every { mockDevice.deviceType } returns DeviceType.PHONE
        val phonePlugin = PresenterPlugin(context, mockDevice)
        assertFalse(phonePlugin.isCompatible)
    }
}
