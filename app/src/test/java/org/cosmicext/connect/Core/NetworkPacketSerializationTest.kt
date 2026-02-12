/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NetworkPacketSerializationTest {

    @Test
    fun `serializeKotlin produces valid JSON with newline terminator`() {
        val packet = NetworkPacket(
            id = 1234567890L,
            type = "cconnect.ping",
            body = mapOf("message" to "hello")
        )
        val serialized = packet.serializeKotlin()
        assertTrue("Should end with newline", serialized.endsWith("\n"))

        // Parse as JSON (strip newline)
        val jo = JSONObject(serialized.trimEnd())
        assertEquals(1234567890L, jo.getLong("id"))
        assertEquals("cconnect.ping", jo.getString("type"))
        assertEquals("hello", jo.getJSONObject("body").getString("message"))
    }

    @Test
    fun `deserializeKotlin roundtrips correctly`() {
        val original = NetworkPacket(
            id = 9999L,
            type = "cconnect.clipboard",
            body = mapOf("content" to "clipboard text", "isPassword" to false)
        )
        val serialized = original.serializeKotlin()
        val deserialized = NetworkPacket.deserializeKotlin(serialized)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.type, deserialized.type)
        assertEquals(original.body, deserialized.body)
        assertNull(deserialized.payloadSize)
        assertTrue(deserialized.payloadTransferInfo.isEmpty())
    }

    @Test
    fun `deserializeKotlin handles payloadSize and payloadTransferInfo`() {
        val original = NetworkPacket(
            id = 5555L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "test.txt"),
            payloadSize = 1024L,
            payloadTransferInfo = mapOf("port" to 1739)
        )
        val serialized = original.serializeKotlin()
        val deserialized = NetworkPacket.deserializeKotlin(serialized)

        assertEquals(5555L, deserialized.id)
        assertEquals("cconnect.share.request", deserialized.type)
        assertEquals("test.txt", deserialized.body["filename"])
        assertEquals(1024L, deserialized.payloadSize)
        assertEquals(1739, deserialized.payloadTransferInfo["port"])
    }

    @Test
    fun `serializeKotlin unescapes slashes for QJson compatibility`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "path/to/file.txt")
        )
        val serialized = packet.serializeKotlin()
        assertTrue("Should not contain escaped slashes", !serialized.contains("\\/"))
        assertTrue("Should contain unescaped path", serialized.contains("path/to/file.txt"))
    }

    @Test
    fun `deserializeKotlin normalizes kdeconnect prefix`() {
        val json = """{"id":1,"type":"kdeconnect.ping","body":{"message":"hi"}}"""
        val packet = NetworkPacket.deserializeKotlin(json)
        assertEquals("cconnect.ping", packet.type)
    }

    @Test
    fun `serializeKotlin omits payloadSize and payloadTransferInfo when no payload`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("message" to "test")
        )
        val serialized = packet.serializeKotlin()
        val jo = JSONObject(serialized.trimEnd())
        assertFalse("Should not have payloadSize", jo.has("payloadSize"))
        assertFalse("Should not have payloadTransferInfo", jo.has("payloadTransferInfo"))
    }

    @Test
    fun `serializeKotlin includes payloadSize but omits empty payloadTransferInfo`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.share.request",
            body = mapOf("filename" to "test.txt"),
            payloadSize = 512L
        )
        val serialized = packet.serializeKotlin()
        val jo = JSONObject(serialized.trimEnd())
        assertEquals(512L, jo.getLong("payloadSize"))
        assertFalse("Should not have payloadTransferInfo when empty", jo.has("payloadTransferInfo"))
    }

}
