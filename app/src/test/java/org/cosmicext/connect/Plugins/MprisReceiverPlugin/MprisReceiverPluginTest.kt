package org.cosmicext.connect.Plugins.MprisReceiverPlugin

import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
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

/**
 * Unit tests for MprisReceiverPlugin
 *
 * Tests plugin metadata, packet routing, and notification listener interface.
 * Does not test MediaSession integration or FFI handlers.
 */
@RunWith(RobolectricTestRunner::class)
class MprisReceiverPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: MprisReceiverPlugin

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        // Direct construction (not Hilt)
        plugin = MprisReceiverPlugin(context, mockDevice)
    }

    @Test
    fun testSupportedPacketTypes() {
        val types = plugin.supportedPacketTypes
        assertEquals(1, types.size)
        assertTrue(types.contains("cconnect.mpris.request"))
    }

    @Test
    fun testOutgoingPacketTypes() {
        val types = plugin.outgoingPacketTypes
        assertEquals(1, types.size)
        assertTrue(types.contains("cconnect.mpris"))
    }

    @Test
    fun testDisplayName() {
        val displayName = plugin.displayName
        assertNotNull(displayName)
        assertFalse(displayName.isEmpty())
        assertEquals(context.getString(R.string.pref_plugin_mprisreceiver), displayName)
    }

    @Test
    fun testDescription() {
        val description = plugin.description
        assertNotNull(description)
        assertFalse(description.isEmpty())
        assertEquals(context.getString(R.string.pref_plugin_mprisreceiver_desc), description)
    }

    @Test
    fun testPluginKey() {
        assertEquals("MprisReceiverPlugin", plugin.pluginKey)
    }

    @Test
    fun testDeviceIdValue() {
        assertEquals("test-device-id", plugin.deviceIdValue)
    }

    @Test
    fun testOnPacketReceived_withPlayerKeyButNoMatchingPlayer() {
        // Packet with "player" key pointing to non-existent player
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mpris.request",
            body = mapOf("player" to "nonexistent-player")
        )
        val transferPacket = TransferPacket(packet)

        // Should return false because player doesn't exist
        assertFalse(plugin.onPacketReceived(transferPacket))
    }

    @Test
    fun testOnPacketReceived_withEmptyBodyReturnsFalse() {
        // Packet with no "player" and no "requestPlayerList"
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.mpris.request",
            body = emptyMap()
        )
        val transferPacket = TransferPacket(packet)

        // Should return false (no action taken)
        assertFalse(plugin.onPacketReceived(transferPacket))
    }

    @Test
    fun testCheckRequiredPermissions_returnsTrue() {
        // MprisReceiverPlugin has empty requiredPermissions array
        // so checkRequiredPermissions should return true by default
        assertTrue(plugin.checkRequiredPermissions())
    }

    @Test
    fun testOnNotificationPosted_doesNotCrash() {
        // Test that notification listener method doesn't crash
        val mockNotification = mockk<StatusBarNotification>(relaxed = true)

        // Should not throw exception
        try {
            plugin.onNotificationPosted(mockNotification)
        } catch (e: Exception) {
            fail("onNotificationPosted should not throw: ${e.message}")
        }
    }

    @Test
    fun testOnNotificationRemoved_doesNotCrash() {
        // Test that notification listener method doesn't crash with null

        // Should not throw exception
        try {
            plugin.onNotificationRemoved(null)
        } catch (e: Exception) {
            fail("onNotificationRemoved should not throw: ${e.message}")
        }
    }
}
