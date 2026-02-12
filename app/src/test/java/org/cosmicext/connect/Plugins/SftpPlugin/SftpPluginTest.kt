package org.cosmicext.connect.Plugins.SftpPlugin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Device
import org.cosmicext.connect.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SftpPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: SftpPlugin

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        // Create plugin without calling onCreate()
        plugin = SftpPlugin(context, mockDevice)
    }

    @Test
    fun `supportedPacketTypes contains sftp request type`() {
        val supportedTypes = plugin.supportedPacketTypes

        assertEquals(1, supportedTypes.size)
        assertTrue(supportedTypes.contains("cconnect.sftp.request"))
    }

    @Test
    fun `outgoingPacketTypes contains sftp packet type`() {
        val outgoingTypes = plugin.outgoingPacketTypes

        assertEquals(1, outgoingTypes.size)
        assertTrue(outgoingTypes.contains("cconnect.sftp"))
    }

    @Test
    fun `displayName returns non-empty string from resources`() {
        val displayName = plugin.displayName

        assertNotNull(displayName)
        assertFalse(displayName.isEmpty())
        // Verify it matches the resource string
        assertEquals(context.getString(R.string.pref_plugin_sftp), displayName)
    }

    @Test
    fun `description returns non-empty string from resources`() {
        val description = plugin.description

        assertNotNull(description)
        assertFalse(description.isEmpty())
        // Verify it matches the resource string
        assertEquals(context.getString(R.string.pref_plugin_sftp_desc), description)
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }

    @Test
    fun `supportsDeviceSpecificSettings returns true`() {
        assertTrue(plugin.supportsDeviceSpecificSettings())
    }

    @Test
    fun `pluginKey returns SftpPlugin`() {
        assertEquals("SftpPlugin", plugin.pluginKey)
    }
}
