/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Backends

import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.cert.Certificate

@RunWith(RobolectricTestRunner::class)
class BaseLinkTest {

    private lateinit var link: TestLink
    private lateinit var mockProvider: BaseLinkProvider
    private lateinit var deviceInfo: DeviceInfo

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockProvider = mockk(relaxed = true)
        val mockCert = mockk<Certificate>(relaxed = true)
        deviceInfo = DeviceInfo(
            id = "test-device-id-32chars-padding!!",
            certificate = mockCert,
            name = "Test Device",
            type = DeviceType.DESKTOP
        )
        link = TestLink(context, mockProvider, deviceInfo)
    }

    // ========================================================================
    // Packet receiver management
    // ========================================================================

    @Test
    fun `packetReceived notifies all registered receivers`() {
        val receiver1 = mockk<BaseLink.PacketReceiver>(relaxed = true)
        val receiver2 = mockk<BaseLink.PacketReceiver>(relaxed = true)
        link.addPacketReceiver(receiver1)
        link.addPacketReceiver(receiver2)

        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap())
        )
        link.packetReceived(tp)

        verify { receiver1.onPacketReceived(tp) }
        verify { receiver2.onPacketReceived(tp) }
    }

    @Test
    fun `removePacketReceiver stops notifications`() {
        val receiver = mockk<BaseLink.PacketReceiver>(relaxed = true)
        link.addPacketReceiver(receiver)
        link.removePacketReceiver(receiver)

        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap())
        )
        link.packetReceived(tp)

        verify(exactly = 0) { receiver.onPacketReceived(any()) }
    }

    @Test
    fun `packetReceived with no receivers does not crash`() {
        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap())
        )
        // Should not throw
        link.packetReceived(tp)
    }

    // ========================================================================
    // deviceId and properties
    // ========================================================================

    @Test
    fun `deviceId delegates to deviceInfo id`() {
        assertEquals("test-device-id-32chars-padding!!", link.deviceId)
    }

    @Test
    fun `name returns subclass name`() {
        assertEquals("TestLink", link.name)
    }

    // ========================================================================
    // disconnect
    // ========================================================================

    @Test
    fun `disconnect calls linkProvider onConnectionLost`() {
        link.disconnect()
        verify { mockProvider.onConnectionLost(link) }
    }

    // ========================================================================
    // Concrete test stub
    // ========================================================================

    /**
     * Minimal concrete implementation of BaseLink for testing
     * the abstract class's packet receiver pattern.
     */
    private class TestLink(
        context: android.content.Context,
        linkProvider: BaseLinkProvider,
        override val deviceInfo: DeviceInfo
    ) : BaseLink(context, linkProvider) {
        override val name = "TestLink"
        override fun sendTransferPacket(
            tp: TransferPacket,
            callback: Device.SendPacketStatusCallback,
            sendPayloadFromSameThread: Boolean
        ) = true
    }
}
