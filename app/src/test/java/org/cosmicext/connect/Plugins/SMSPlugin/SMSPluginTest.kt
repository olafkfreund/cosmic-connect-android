package org.cosmicext.connect.Plugins.SMSPlugin

import android.Manifest
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Device
import org.cosmicext.connect.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SMSPluginTest {

    private lateinit var context: android.app.Application
    private lateinit var mockDevice: Device
    private lateinit var plugin: SMSPlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        // Create plugin without calling onCreate()
        plugin = SMSPlugin(context, mockDevice)
    }

    @Test
    fun `supportedPacketTypes contains all four SMS packet types`() {
        val supportedTypes = plugin.supportedPacketTypes

        assertEquals(4, supportedTypes.size)
        assertTrue(supportedTypes.contains("cconnect.sms.request"))
        assertTrue(supportedTypes.contains("cconnect.sms.request_conversations"))
        assertTrue(supportedTypes.contains("cconnect.sms.request_conversation"))
        assertTrue(supportedTypes.contains("cconnect.sms.request_attachment"))
    }

    @Test
    fun `outgoingPacketTypes contains sms messages and attachment file types`() {
        val outgoingTypes = plugin.outgoingPacketTypes

        assertEquals(2, outgoingTypes.size)
        assertTrue(outgoingTypes.contains("cconnect.sms.messages"))
        assertTrue(outgoingTypes.contains("cconnect.sms.attachment_file"))
    }

    @Test
    fun `displayName returns non-empty string from resources`() {
        val displayName = plugin.displayName

        assertNotNull(displayName)
        assertFalse(displayName.isEmpty())
        assertEquals(context.getString(R.string.pref_plugin_telepathy), displayName)
    }

    @Test
    fun `description returns non-empty string from resources`() {
        val description = plugin.description

        assertNotNull(description)
        assertFalse(description.isEmpty())
        assertEquals(context.getString(R.string.pref_plugin_telepathy_desc), description)
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }

    @Test
    fun `pluginKey returns SMSPlugin`() {
        assertEquals("SMSPlugin", plugin.pluginKey)
    }

    @Test
    fun `checkRequiredPermissions returns true when all SMS permissions granted`() {
        shadowOf(context).grantPermissions(
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        assertTrue(plugin.checkRequiredPermissions())
    }
}
