/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.PacketType
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.PairingHandler.PairState
import org.cosmic.cosmicconnect.PairingHandler.PairingCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.KeyPairGenerator
import java.security.cert.Certificate

@RunWith(RobolectricTestRunner::class)
class PairingHandlerTest {

    private lateinit var device: Device
    private lateinit var callback: PairingCallback
    private lateinit var sslHelper: SslHelper
    private lateinit var mockCertificateA: Certificate
    private lateinit var mockCertificateB: Certificate

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        device = mockk(relaxed = true)
        every { device.context } returns context
        every { device.protocolVersion } returns 7
        every { device.deviceId } returns "abcdef1234567890abcdef1234567890"

        callback = mockk(relaxed = true)
        sslHelper = mockk(relaxed = true)

        // Generate real key pairs for deterministic verification key tests
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024)
        mockCertificateA = mockk(relaxed = true)
        mockCertificateB = mockk(relaxed = true)
        val keyPairA = keyGen.generateKeyPair()
        val keyPairB = keyGen.generateKeyPair()
        every { mockCertificateA.publicKey } returns keyPairA.public
        every { mockCertificateB.publicKey } returns keyPairB.public

        every { sslHelper.certificate } returns mockCertificateA
        every { device.certificate } returns mockCertificateB
    }

    private fun createHandler(state: PairState = PairState.NotPaired): PairingHandler {
        return PairingHandler(device, callback, state, sslHelper)
    }

    private fun pairPacket(pair: Boolean, timestamp: Long? = null): TransferPacket {
        val body = mutableMapOf<String, Any>("pair" to pair)
        if (timestamp != null) {
            body["timestamp"] = timestamp
        }
        return TransferPacket(
            NetworkPacket(id = 1L, type = PacketType.PAIR, body = body)
        )
    }

    // ========================================================================
    // packetReceived — pair=true transitions
    // ========================================================================

    @Test
    fun `pair true from NotPaired transitions to RequestedByPeer v7`() {
        val handler = createHandler(PairState.NotPaired)
        handler.packetReceived(pairPacket(pair = true))

        assertEquals(PairState.RequestedByPeer, handler.state)
        verify { callback.incomingPairRequest() }
    }

    @Test
    fun `pair true from Requested transitions to Paired`() {
        val handler = createHandler(PairState.Requested)
        handler.packetReceived(pairPacket(pair = true))

        assertEquals(PairState.Paired, handler.state)
        verify { callback.pairingSuccessful() }
    }

    @Test
    fun `pair true from RequestedByPeer is ignored`() {
        val handler = createHandler(PairState.RequestedByPeer)
        handler.packetReceived(pairPacket(pair = true))

        assertEquals(PairState.RequestedByPeer, handler.state)
        verify(exactly = 0) { callback.incomingPairRequest() }
        verify(exactly = 0) { callback.pairingSuccessful() }
    }

    @Test
    fun `pair true from Paired unpairs then re-requests v7`() {
        val handler = createHandler(PairState.Paired)
        handler.packetReceived(pairPacket(pair = true))

        assertEquals(PairState.RequestedByPeer, handler.state)
        verify { callback.unpaired(device) }
        verify { callback.incomingPairRequest() }
    }

    // ========================================================================
    // packetReceived — pair=false transitions
    // ========================================================================

    @Test
    fun `pair false from Paired transitions to NotPaired`() {
        val handler = createHandler(PairState.Paired)
        handler.packetReceived(pairPacket(pair = false))

        assertEquals(PairState.NotPaired, handler.state)
        verify { callback.unpaired(device) }
    }

    @Test
    fun `pair false from Requested transitions to NotPaired with pairingFailed`() {
        val handler = createHandler(PairState.Requested)
        handler.packetReceived(pairPacket(pair = false))

        assertEquals(PairState.NotPaired, handler.state)
        verify { callback.pairingFailed(any()) }
    }

    @Test
    fun `pair false from NotPaired stays NotPaired`() {
        val handler = createHandler(PairState.NotPaired)
        handler.packetReceived(pairPacket(pair = false))

        assertEquals(PairState.NotPaired, handler.state)
        verify(exactly = 0) { callback.unpaired(any()) }
        verify(exactly = 0) { callback.pairingFailed(any()) }
    }

    // ========================================================================
    // Protocol v8 timestamp checks
    // ========================================================================

    @Test
    fun `pair true v8 with bad timestamp fails pairing`() {
        every { device.protocolVersion } returns 8
        val handler = createHandler(PairState.NotPaired)

        handler.packetReceived(pairPacket(pair = true, timestamp = -1L))

        assertEquals(PairState.NotPaired, handler.state)
        verify { callback.unpaired(device) }
    }

    @Test
    fun `pair true v8 with good timestamp transitions to RequestedByPeer`() {
        every { device.protocolVersion } returns 8
        val handler = createHandler(PairState.NotPaired)

        val currentTimestamp = System.currentTimeMillis() / 1000L
        handler.packetReceived(pairPacket(pair = true, timestamp = currentTimestamp))

        assertEquals(PairState.RequestedByPeer, handler.state)
        verify { callback.incomingPairRequest() }
    }

    @Test
    fun `pair true v8 with timestamp too far in future fails`() {
        every { device.protocolVersion } returns 8
        val handler = createHandler(PairState.NotPaired)

        val farFuture = (System.currentTimeMillis() / 1000L) + 3600L // 1 hour ahead
        handler.packetReceived(pairPacket(pair = true, timestamp = farFuture))

        assertEquals(PairState.NotPaired, handler.state)
        verify { callback.pairingFailed(any()) }
    }

    // ========================================================================
    // pairingDone — callback exception handling
    // ========================================================================

    @Test
    fun `pairingDone handles callback exception gracefully`() {
        every { callback.pairingSuccessful() } throws RuntimeException("callback error")
        val handler = createHandler(PairState.Requested)

        handler.pairingDone()

        assertEquals(PairState.NotPaired, handler.state)
    }

    // ========================================================================
    // Companion methods — verification keys
    // ========================================================================

    @Test
    fun `getVerificationKey is deterministic for same inputs`() {
        val key1 = PairingHandler.getVerificationKey(mockCertificateA, mockCertificateB, 1000L)
        val key2 = PairingHandler.getVerificationKey(mockCertificateA, mockCertificateB, 1000L)
        assertEquals(key1, key2)
        assertNotNull(key1)
        assertEquals(8, key1.length)
    }

    @Test
    fun `getVerificationKeyV7 is deterministic for same inputs`() {
        val key1 = PairingHandler.getVerificationKeyV7(mockCertificateA, mockCertificateB)
        val key2 = PairingHandler.getVerificationKeyV7(mockCertificateA, mockCertificateB)
        assertEquals(key1, key2)
        assertNotNull(key1)
        assertEquals(8, key1.length)
    }
}
