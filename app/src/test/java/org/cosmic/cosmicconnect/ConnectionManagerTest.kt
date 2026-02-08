/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Core.PacketType
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectionManagerTest {

    private lateinit var connectionManager: ConnectionManager
    private val receivedPairPackets = mutableListOf<TransferPacket>()
    private val receivedDataPackets = mutableListOf<TransferPacket>()
    private var unpairCalled = false
    private var linksChangedLink: BaseLink? = null
    private var linksEmptyCalled = false
    private var paired = false

    @Before
    fun setUp() {
        receivedPairPackets.clear()
        receivedDataPackets.clear()
        unpairCalled = false
        linksChangedLink = null
        linksEmptyCalled = false
        paired = false

        connectionManager = ConnectionManager(
            deviceId = "test-device-id",
            deviceName = { "Test Device" },
            onPairPacket = { receivedPairPackets.add(it) },
            onDataPacket = { receivedDataPackets.add(it) },
            isPaired = { paired },
            onUnpair = { unpairCalled = true },
            onLinksChanged = { linksChangedLink = it },
            onLinksEmpty = { linksEmptyCalled = true }
        )
    }

    @Test
    fun `isReachable returns false when no links`() {
        assertFalse(connectionManager.isReachable)
    }

    @Test
    fun `isReachable returns true after adding link`() {
        val link = createMockLink()
        connectionManager.addLink(link)
        assertTrue(connectionManager.isReachable)
    }

    @Test
    fun `addLink registers packet receiver`() {
        val link = createMockLink()
        connectionManager.addLink(link)
        verify { link.addPacketReceiver(connectionManager) }
    }

    @Test
    fun `addLink triggers onLinksChanged callback`() {
        val link = createMockLink()
        connectionManager.addLink(link)
        assertEquals(link, linksChangedLink)
    }

    @Test
    fun `removeLink triggers onLinksEmpty when last link removed`() {
        val link = createMockLink()
        connectionManager.addLink(link)
        connectionManager.removeLink(link)
        assertTrue(linksEmptyCalled)
    }

    @Test
    fun `removeLink does not trigger onLinksEmpty when links remain`() {
        val link1 = createMockLink("link1")
        val link2 = createMockLink("link2")
        connectionManager.addLink(link1)
        connectionManager.addLink(link2)
        connectionManager.removeLink(link1)
        assertFalse(linksEmptyCalled)
    }

    @Test
    fun `hasLinkFromProvider returns true for matching provider`() {
        val provider = mockk<BaseLinkProvider>(relaxed = true)
        val link = createMockLink(provider = provider)
        connectionManager.addLink(link)
        assertTrue(connectionManager.hasLinkFromProvider(provider))
    }

    @Test
    fun `hasLinkFromProvider returns false for non-matching provider`() {
        val provider1 = mockk<BaseLinkProvider>(relaxed = true)
        val provider2 = mockk<BaseLinkProvider>(relaxed = true)
        val link = createMockLink(provider = provider1)
        connectionManager.addLink(link)
        assertFalse(connectionManager.hasLinkFromProvider(provider2))
    }

    @Test
    fun `onPacketReceived routes pair packets to onPairPacket`() {
        val pairPacket = TransferPacket(
            org.cosmic.cosmicconnect.Core.NetworkPacket(
                id = 1L, type = PacketType.PAIR, body = mapOf("pair" to true)
            )
        )
        connectionManager.onPacketReceived(pairPacket)
        assertEquals(1, receivedPairPackets.size)
        assertEquals(PacketType.PAIR, receivedPairPackets[0].packet.type)
        assertTrue(receivedDataPackets.isEmpty())
    }

    @Test
    fun `onPacketReceived calls unpair and drops data packets when not paired`() {
        val dataPacket = TransferPacket(
            org.cosmic.cosmicconnect.Core.NetworkPacket(
                id = 2L, type = "cconnect.ping", body = emptyMap()
            )
        )
        paired = false
        connectionManager.onPacketReceived(dataPacket)
        assertTrue(unpairCalled)
        assertTrue(receivedDataPackets.isEmpty())
    }

    @Test
    fun `onPacketReceived routes data packets without unpair when paired`() {
        val dataPacket = TransferPacket(
            org.cosmic.cosmicconnect.Core.NetworkPacket(
                id = 3L, type = "cconnect.ping", body = emptyMap()
            )
        )
        paired = true
        connectionManager.onPacketReceived(dataPacket)
        assertFalse(unpairCalled)
        assertEquals(1, receivedDataPackets.size)
    }

    @Test
    fun `linkCount tracks number of links`() {
        assertEquals(0, connectionManager.linkCount)
        val link1 = createMockLink("link1")
        connectionManager.addLink(link1)
        assertEquals(1, connectionManager.linkCount)
        val link2 = createMockLink("link2")
        connectionManager.addLink(link2)
        assertEquals(2, connectionManager.linkCount)
    }

    @Test
    fun `disconnect calls disconnect on all links`() {
        val link1 = createMockLink("link1")
        val link2 = createMockLink("link2")
        connectionManager.addLink(link1)
        connectionManager.addLink(link2)
        connectionManager.disconnect()
        verify { link1.disconnect() }
        verify { link2.disconnect() }
    }

    // --- TransferPacket tests ---

    @Test
    fun `sendPacketBlocking with TransferPacket calls sendTransferPacket on link`() {
        val link = createMockLink()
        every { link.sendTransferPacket(any(), any(), any()) } returns true
        connectionManager.addLink(link)

        val corePacket = org.cosmic.cosmicconnect.Core.NetworkPacket(
            id = 1L, type = "cconnect.ping", body = mapOf("message" to "test")
        )
        val tp = TransferPacket(corePacket)
        val callback = mockk<Device.SendPacketStatusCallback>(relaxed = true)

        val result = connectionManager.sendPacketBlocking(tp, callback, false)
        assertTrue(result)
        verify { link.sendTransferPacket(tp, callback, false) }
    }

    @Test
    fun `sendPacketBlocking with TransferPacket returns false when no links`() {
        val corePacket = org.cosmic.cosmicconnect.Core.NetworkPacket(
            id = 1L, type = "cconnect.ping", body = emptyMap()
        )
        val tp = TransferPacket(corePacket)
        val callback = mockk<Device.SendPacketStatusCallback>(relaxed = true)

        val result = connectionManager.sendPacketBlocking(tp, callback, false)
        assertFalse(result)
    }

    @Test
    fun `sendPacketBlocking with TransferPacket tries next link on failure`() {
        val link1 = createMockLink("link1")
        val link2 = createMockLink("link2")
        every { link1.sendTransferPacket(any(), any(), any()) } throws java.io.IOException("fail")
        every { link2.sendTransferPacket(any(), any(), any()) } returns true
        connectionManager.addLink(link1)
        connectionManager.addLink(link2)

        val tp = TransferPacket(
            org.cosmic.cosmicconnect.Core.NetworkPacket(
                id = 1L, type = "cconnect.ping", body = emptyMap()
            )
        )
        val callback = mockk<Device.SendPacketStatusCallback>(relaxed = true)

        val result = connectionManager.sendPacketBlocking(tp, callback, false)
        assertTrue(result)
    }

    private fun createMockLink(
        name: String = "TestLink",
        provider: BaseLinkProvider = mockk(relaxed = true)
    ): BaseLink {
        val link = mockk<BaseLink>(relaxed = true)
        every { link.name } returns name
        every { link.linkProvider } returns provider
        every { provider.priority } returns 0
        return link
    }
}
