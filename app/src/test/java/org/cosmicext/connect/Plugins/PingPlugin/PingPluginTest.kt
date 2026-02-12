/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.PingPlugin

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
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
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PingPluginTest {

    private lateinit var plugin: PingPlugin
    private lateinit var mockDevice: Device
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        // Grant POST_NOTIFICATIONS so displayPingNotification uses NotificationManager
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = PingPlugin(context, mockDevice)

        notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }

    // ========================================================================
    // onPacketReceived
    // ========================================================================

    @Test
    fun `keepalive ping returns true and does not show notification`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("keepalive" to true)
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `message ping shows notification with message text`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("message" to "Hello from desktop!")
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)

        val shadow = shadowOf(notificationManager)
        assertTrue(shadow.size() > 0)
        val notification = shadow.allNotifications.first()
        val extras = notification.extras
        assertEquals("Hello from desktop!", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun `no-message ping uses default Ping text`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)

        val shadow = shadowOf(notificationManager)
        assertTrue(shadow.size() > 0)
        val notification = shadow.allNotifications.first()
        val extras = notification.extras
        assertEquals("Ping!", extras.getCharSequence(Notification.EXTRA_TEXT)?.toString())
    }

    @Test
    fun `wrong packet type returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    // ========================================================================
    // Packet type arrays
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains cconnect ping`() {
        assertArrayEquals(arrayOf("cconnect.ping"), plugin.supportedPacketTypes)
    }

    @Test
    fun `outgoingPacketTypes contains cconnect ping`() {
        assertArrayEquals(arrayOf("cconnect.ping"), plugin.outgoingPacketTypes)
    }
}
