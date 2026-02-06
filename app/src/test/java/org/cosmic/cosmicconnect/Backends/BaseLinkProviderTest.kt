/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Backends

import android.net.Network
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.DeviceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.cert.Certificate

@RunWith(RobolectricTestRunner::class)
class BaseLinkProviderTest {

    private lateinit var linkProvider: TestLinkProvider

    @Before
    fun setUp() {
        linkProvider = TestLinkProvider()
    }

    // ========================================================================
    // Connection receiver management
    // ========================================================================

    @Test
    fun `addConnectionReceiver and onConnectionReceived notifies receiver`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        val link = mockk<BaseLink>(relaxed = true)

        linkProvider.addConnectionReceiver(receiver)
        linkProvider.fireConnectionReceived(link)

        verify { receiver.onConnectionReceived(link) }
    }

    @Test
    fun `onConnectionLost notifies all receivers`() {
        val receiver1 = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        val receiver2 = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        val link = mockk<BaseLink>(relaxed = true)

        linkProvider.addConnectionReceiver(receiver1)
        linkProvider.addConnectionReceiver(receiver2)
        linkProvider.onConnectionLost(link)

        verify { receiver1.onConnectionLost(link) }
        verify { receiver2.onConnectionLost(link) }
    }

    @Test
    fun `onDeviceInfoUpdated notifies all receivers`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        val mockCert = mockk<Certificate>(relaxed = true)
        val deviceInfo = DeviceInfo(
            id = "test-device-id-32chars-padding!!",
            certificate = mockCert,
            name = "Test Device",
            type = DeviceType.DESKTOP
        )

        linkProvider.addConnectionReceiver(receiver)
        linkProvider.fireDeviceInfoUpdated(deviceInfo)

        verify { receiver.onDeviceInfoUpdated(deviceInfo) }
    }

    @Test
    fun `removeConnectionReceiver stops all notifications`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        val link = mockk<BaseLink>(relaxed = true)

        linkProvider.addConnectionReceiver(receiver)
        linkProvider.removeConnectionReceiver(receiver)

        linkProvider.fireConnectionReceived(link)
        linkProvider.onConnectionLost(link)

        verify(exactly = 0) { receiver.onConnectionReceived(any()) }
        verify(exactly = 0) { receiver.onConnectionLost(any()) }
    }

    @Test
    fun `no receivers does not crash on notification`() {
        val link = mockk<BaseLink>(relaxed = true)
        // Should not throw
        linkProvider.fireConnectionReceived(link)
        linkProvider.onConnectionLost(link)
    }

    @Test
    fun `removeConnectionReceiver returns false for unknown receiver`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        assertFalse(linkProvider.removeConnectionReceiver(receiver))
    }

    @Test
    fun `removeConnectionReceiver returns true for registered receiver`() {
        val receiver = mockk<BaseLinkProvider.ConnectionReceiver>(relaxed = true)
        linkProvider.addConnectionReceiver(receiver)
        assertTrue(linkProvider.removeConnectionReceiver(receiver))
    }

    // ========================================================================
    // Concrete test stub
    // ========================================================================

    /**
     * Minimal concrete implementation of BaseLinkProvider for testing
     * the abstract class's observer pattern.
     */
    private class TestLinkProvider : BaseLinkProvider() {
        override fun onStart() {}
        override fun onStop() {}
        override fun onNetworkChange(network: Network?) {}
        override val name = "TestLinkProvider"
        override val priority = 0

        // Expose protected methods for testing
        fun fireConnectionReceived(link: BaseLink) = onConnectionReceived(link)
        fun fireDeviceInfoUpdated(deviceInfo: DeviceInfo) = onDeviceInfoUpdated(deviceInfo)
    }
}
