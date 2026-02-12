package org.cosmicext.connect.Plugins.ContactsPlugin

import android.Manifest
import android.app.Application
import io.mockk.every
import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests for ContactsPlugin
 *
 * Tests plugin metadata, permissions, and packet routing.
 * Does not test FFI handlers (handleRequestAllUIDsTimestamps, handleRequestVCardsByUIDs).
 */
@RunWith(RobolectricTestRunner::class)
class ContactsPluginTest {

    private lateinit var context: Application
    private lateinit var mockDevice: Device
    private lateinit var plugin: ContactsPlugin

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = ContactsPlugin(context, mockDevice)
    }

    @Test
    fun testSupportedPacketTypes() {
        val types = plugin.supportedPacketTypes
        assertEquals(2, types.size)
        assertTrue(types.contains("cconnect.contacts.request_all_uids_timestamps"))
        assertTrue(types.contains("cconnect.contacts.request_vcards_by_uid"))
    }

    @Test
    fun testOutgoingPacketTypes() {
        val types = plugin.outgoingPacketTypes
        assertEquals(2, types.size)
        assertTrue(types.contains("cconnect.contacts.response_uids_timestamps"))
        assertTrue(types.contains("cconnect.contacts.response_vcards"))
    }

    @Test
    fun testDisplayName() {
        val displayName = plugin.displayName
        assertNotNull(displayName)
        assertFalse(displayName.isEmpty())
        assertEquals(context.getString(R.string.pref_plugin_contacts), displayName)
    }

    @Test
    fun testDescription() {
        val description = plugin.description
        assertNotNull(description)
        assertFalse(description.isEmpty())
        assertEquals(context.getString(R.string.pref_plugin_contacts_desc), description)
    }

    @Test
    fun testIsEnabledByDefault() {
        assertTrue(plugin.isEnabledByDefault)
    }

    @Test
    fun testSupportsDeviceSpecificSettings() {
        assertTrue(plugin.supportsDeviceSpecificSettings())
    }

    @Test
    fun testPluginKey() {
        assertEquals("ContactsPlugin", plugin.pluginKey)
    }

    @Test
    fun testCheckRequiredPermissions_withoutReadContactsPermission() {
        // Don't grant permission
        assertFalse(plugin.checkRequiredPermissions())
    }

    @Test
    fun testCheckRequiredPermissions_withPermissionButWithoutPreference() {
        // Grant READ_CONTACTS permission
        shadowOf(context).grantPermissions(Manifest.permission.READ_CONTACTS)

        // Don't set the preference
        assertFalse(plugin.checkRequiredPermissions())
    }

    @Test
    fun testCheckRequiredPermissions_withPermissionAndPreference() {
        // Grant READ_CONTACTS permission
        shadowOf(context).grantPermissions(Manifest.permission.READ_CONTACTS)

        // Set the preference
        plugin.preferences!!.edit()
            .putBoolean("acceptedToTransferContacts", true)
            .commit()

        assertTrue(plugin.checkRequiredPermissions())
    }

    @Test
    fun testOnPacketReceived_unknownTypeReturnsFalse() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.unknown.type",
            body = emptyMap()
        )
        val transferPacket = TransferPacket(packet)

        assertFalse(plugin.onPacketReceived(transferPacket))
    }
}
