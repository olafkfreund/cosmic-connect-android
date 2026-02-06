/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
class TransferPacketTest {

    @Test
    fun `serializeForWire produces valid JSON without payload`() {
        val packet = NetworkPacket(
            id = 100L,
            type = "cconnect.ping",
            body = mapOf("message" to "test")
        )
        val tp = TransferPacket(packet)
        val wire = tp.serializeForWire()
        assertTrue(wire.endsWith("\n"))

        val jo = JSONObject(wire.trimEnd())
        assertEquals(100L, jo.getLong("id"))
        assertEquals("cconnect.ping", jo.getString("type"))
        assertFalse(jo.has("payloadSize"))
    }

    @Test
    fun `serializeForWire includes payload info and runtime transfer info`() {
        val packet = NetworkPacket(
            id = 200L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "test.txt")
        )
        val payload = Payload(byteArrayOf(1, 2, 3, 4))
        val tp = TransferPacket(packet, payload = payload)
        tp.runtimeTransferInfo = mapOf("port" to 1739)

        val wire = tp.serializeForWire()
        val jo = JSONObject(wire.trimEnd())
        assertEquals(4L, jo.getLong("payloadSize"))
        assertEquals(1739, jo.getJSONObject("payloadTransferInfo").getInt("port"))
    }

    @Test
    fun `cancel sets isCanceled flag`() {
        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap())
        )
        assertFalse(tp.isCanceled)
        tp.cancel()
        assertTrue(tp.isCanceled)
    }

    @Test
    fun `hasPayload returns false when no payload`() {
        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap())
        )
        assertFalse(tp.hasPayload)
        assertEquals(0L, tp.payloadSize)
    }

    @Test
    fun `hasPayload returns true with payload`() {
        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.share.request", body = emptyMap()),
            payload = Payload(byteArrayOf(1, 2, 3))
        )
        assertTrue(tp.hasPayload)
        assertEquals(3L, tp.payloadSize)
    }

    @Test
    fun `type delegates to packet type`() {
        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.clipboard", body = emptyMap())
        )
        assertEquals("cconnect.clipboard", tp.type)
    }

    @Test
    fun `hasPayload returns false for zero-size payload`() {
        val tp = TransferPacket(
            NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap()),
            payload = Payload(0L)
        )
        assertFalse(tp.hasPayload)
    }

    @Test
    fun `serializeForWire uses packet payloadTransferInfo when no runtime info`() {
        val packet = NetworkPacket(
            id = 600L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "test.txt"),
            payloadTransferInfo = mapOf("port" to 1234)
        )
        val tp = TransferPacket(packet, payload = Payload(byteArrayOf(1)))
        // No runtimeTransferInfo set

        val wire = tp.serializeForWire()
        val jo = JSONObject(wire.trimEnd())
        assertEquals(1234, jo.getJSONObject("payloadTransferInfo").getInt("port"))
    }
}
