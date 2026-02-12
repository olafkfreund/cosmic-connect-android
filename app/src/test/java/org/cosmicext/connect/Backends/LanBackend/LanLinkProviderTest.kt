/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Backends.LanBackend

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmicext.connect.Backends.BaseLink
import org.cosmicext.connect.Backends.BaseLinkProvider
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.SecurityHelpers.SslHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LanLinkProviderTest {

    private lateinit var provider: LanLinkProvider
    private lateinit var mockDeviceHelper: DeviceHelper
    private lateinit var mockSslHelper: SslHelper
    private lateinit var mockDeviceRegistry: DeviceRegistry

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockDeviceHelper = mockk(relaxed = true)
        mockSslHelper = mockk(relaxed = true)
        mockDeviceRegistry = mockk(relaxed = true)

        every { mockDeviceHelper.getDeviceId() } returns "my-device-id-12345678901234567890"

        provider = LanLinkProvider(context, mockDeviceHelper, mockSslHelper, mockDeviceRegistry)
    }

    // ========================================================================
    // Constants
    // ========================================================================

    @Test
    fun `UDP_PORT is 1816`() {
        assertEquals(1816, LanLinkProvider.UDP_PORT)
    }

    @Test
    fun `MIN_PORT is 1814`() {
        assertEquals(1814, LanLinkProvider.MIN_PORT)
    }

    @Test
    fun `MAX_PORT is 1864`() {
        assertEquals(1864, LanLinkProvider.MAX_PORT)
    }

    @Test
    fun `PAYLOAD_TRANSFER_MIN_PORT is 1839`() {
        assertEquals(1839, LanLinkProvider.PAYLOAD_TRANSFER_MIN_PORT)
    }

    @Test
    fun `MAX_UDP_PACKET_SIZE is 8KB`() {
        assertEquals(1024 * 8, LanLinkProvider.MAX_UDP_PACKET_SIZE)
    }

    @Test
    fun `MAX_IDENTITY_PACKET_SIZE is 512KB`() {
        assertEquals(1024 * 512, LanLinkProvider.MAX_IDENTITY_PACKET_SIZE)
    }

    @Test
    fun `rate limit delay is 1000ms`() {
        assertEquals(1000L, LanLinkProvider.MILLIS_DELAY_BETWEEN_CONNECTIONS_TO_SAME_DEVICE)
    }

    // ========================================================================
    // Name and priority
    // ========================================================================

    @Test
    fun `name returns LanLinkProvider`() {
        assertEquals("LanLinkProvider", provider.name)
    }

    @Test
    fun `priority returns 20`() {
        assertEquals(20, provider.priority)
    }

    // ========================================================================
    // Rate limiting (rateLimitByDeviceId is internal)
    // ========================================================================

    @Test
    fun `rateLimitByDeviceId first call returns false`() {
        assertFalse(provider.rateLimitByDeviceId("device-abc-12345678901234567890"))
    }

    @Test
    fun `rateLimitByDeviceId second immediate call returns true`() {
        val deviceId = "device-xyz-12345678901234567890"
        provider.rateLimitByDeviceId(deviceId)
        assertTrue(provider.rateLimitByDeviceId(deviceId))
    }

    @Test
    fun `rateLimitByDeviceId different devices are independent`() {
        assertFalse(provider.rateLimitByDeviceId("device-aaa-12345678901234567890"))
        assertFalse(provider.rateLimitByDeviceId("device-bbb-12345678901234567890"))
    }

    @Test
    fun `rateLimitByDeviceId eviction does not crash when over max entries`() {
        for (i in 0..LanLinkProvider.MAX_RATE_LIMIT_ENTRIES) {
            provider.rateLimitByDeviceId("device-$i-padding-32-characters!")
        }
        // Should not crash and a new device should not be rate limited
        assertFalse(provider.rateLimitByDeviceId("brand-new-device-32chars-padding"))
    }

    @Test
    fun `rateLimitByDeviceId same device after different device still rate limited`() {
        val deviceA = "device-aaa-12345678901234567890"
        val deviceB = "device-bbb-12345678901234567890"
        provider.rateLimitByDeviceId(deviceA)
        provider.rateLimitByDeviceId(deviceB)
        assertTrue(provider.rateLimitByDeviceId(deviceA))
    }

    // ========================================================================
    // visibleDevices management
    // ========================================================================

    @Test
    fun `visibleDevices initially empty`() {
        assertTrue(provider.visibleDevices.isEmpty())
    }

    @Test
    fun `onConnectionLost removes device from visibleDevices`() {
        val mockLink = mockk<BaseLink>(relaxed = true)
        every { mockLink.deviceId } returns "test-device-abc123456789012345678"

        provider.visibleDevices["test-device-abc123456789012345678"] = mockk(relaxed = true)
        assertEquals(1, provider.visibleDevices.size)

        provider.onConnectionLost(mockLink)
        assertTrue(provider.visibleDevices.isEmpty())
    }

    @Test
    fun `onConnectionLost with unknown device does not crash`() {
        val mockLink = mockk<BaseLink>(relaxed = true)
        every { mockLink.deviceId } returns "unknown-device-12345678901234567"

        provider.onConnectionLost(mockLink)
        assertTrue(provider.visibleDevices.isEmpty())
    }

    @Test
    fun `onConnectionLost only removes matching device`() {
        val mockLinkA: LanLink = mockk(relaxed = true)

        provider.visibleDevices["device-aaa-12345678901234567890"] = mockk(relaxed = true)
        provider.visibleDevices["device-bbb-12345678901234567890"] = mockk(relaxed = true)
        assertEquals(2, provider.visibleDevices.size)

        every { mockLinkA.deviceId } returns "device-aaa-12345678901234567890"
        provider.onConnectionLost(mockLinkA)

        assertEquals(1, provider.visibleDevices.size)
        assertTrue(provider.visibleDevices.containsKey("device-bbb-12345678901234567890"))
    }

    // ========================================================================
    // Observer pattern (inherited from BaseLinkProvider)
    // ========================================================================

    @Test
    fun `onConnectionLost notifies all registered receivers`() {
        val receiver1 = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        val receiver2 = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        provider.addConnectionReceiver(receiver1)
        provider.addConnectionReceiver(receiver2)

        val mockLink = mockk<BaseLink>(relaxed = true)
        every { mockLink.deviceId } returns "test-device-abc123456789012345678"

        provider.onConnectionLost(mockLink)

        verify { receiver1.onConnectionLost(mockLink) }
        verify { receiver2.onConnectionLost(mockLink) }
    }

    @Test
    fun `removeConnectionReceiver stops notifications`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        provider.addConnectionReceiver(receiver)
        provider.removeConnectionReceiver(receiver)

        val mockLink = mockk<BaseLink>(relaxed = true)
        every { mockLink.deviceId } returns "test-device-abc123456789012345678"

        provider.onConnectionLost(mockLink)

        verify(exactly = 0) { receiver.onConnectionLost(any()) }
    }

    @Test
    fun `duplicate addConnectionReceiver notifies receiver twice`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        provider.addConnectionReceiver(receiver)
        provider.addConnectionReceiver(receiver)

        val mockLink = mockk<BaseLink>(relaxed = true)
        every { mockLink.deviceId } returns "test-device-abc123456789012345678"

        provider.onConnectionLost(mockLink)

        verify(exactly = 2) { receiver.onConnectionLost(mockLink) }
    }

    @Test
    fun `removeConnectionReceiver returns true when present`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        provider.addConnectionReceiver(receiver)
        assertTrue(provider.removeConnectionReceiver(receiver))
    }

    @Test
    fun `removeConnectionReceiver returns false when not present`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        assertFalse(provider.removeConnectionReceiver(receiver))
    }

    // ========================================================================
    // openServerSocketOnFreePort (static companion)
    // ========================================================================

    @Test
    fun `openServerSocketOnFreePort returns bound socket`() {
        val serverSocket = LanLinkProvider.openServerSocketOnFreePort(LanLinkProvider.MIN_PORT)
        try {
            assertTrue(serverSocket.isBound)
            assertTrue(serverSocket.localPort > 0)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun `openServerSocketOnFreePort has reuseAddress`() {
        val serverSocket = LanLinkProvider.openServerSocketOnFreePort(LanLinkProvider.MIN_PORT)
        try {
            assertTrue(serverSocket.reuseAddress)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun `openServerSocketOnFreePort with minPort 0 uses OS assignment`() {
        val serverSocket = LanLinkProvider.openServerSocketOnFreePort(0)
        try {
            assertTrue(serverSocket.localPort > 0)
        } finally {
            serverSocket.close()
        }
    }

    @Test
    fun `openServerSocketOnFreePort two calls return different ports`() {
        val socket1 = LanLinkProvider.openServerSocketOnFreePort(LanLinkProvider.MIN_PORT)
        val socket2 = LanLinkProvider.openServerSocketOnFreePort(LanLinkProvider.MIN_PORT)
        try {
            assertTrue(socket1.localPort != socket2.localPort)
        } finally {
            socket1.close()
            socket2.close()
        }
    }
}
