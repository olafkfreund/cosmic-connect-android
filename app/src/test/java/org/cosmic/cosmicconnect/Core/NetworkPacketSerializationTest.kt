/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun `serializeKotlin output matches legacy serialize for same data`() {
        val body = mapOf(
            "key1" to "value1",
            "key2" to 42,
            "key3" to true,
            "key4" to 3.14
        )
        val corePacket = NetworkPacket(
            id = 1000L,
            type = "cconnect.battery",
            body = body
        )

        // Build equivalent legacy packet
        val legacyPacket = org.cosmic.cosmicconnect.NetworkPacket("cconnect.battery")
        legacyPacket["key1"] = "value1"
        legacyPacket["key2"] = 42
        legacyPacket["key3"] = true
        legacyPacket["key4"] = 3.14

        val coreJson = JSONObject(corePacket.serializeKotlin().trimEnd())
        val legacyJson = JSONObject(legacyPacket.serialize().trimEnd())

        // Compare body fields (IDs will differ since legacy uses System.currentTimeMillis)
        assertEquals(coreJson.getString("type"), legacyJson.getString("type"))
        val coreBody = coreJson.getJSONObject("body")
        val legacyBody = legacyJson.getJSONObject("body")
        assertEquals(legacyBody.getString("key1"), coreBody.getString("key1"))
        assertEquals(legacyBody.getInt("key2"), coreBody.getInt("key2"))
        assertEquals(legacyBody.getBoolean("key3"), coreBody.getBoolean("key3"))
        assertEquals(legacyBody.getDouble("key4"), coreBody.getDouble("key4"), 0.001)
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
    fun `fromLegacyPacket extracts body without double-serialize`() {
        val legacy = org.cosmic.cosmicconnect.NetworkPacket("cconnect.battery")
        legacy["currentCharge"] = 85
        legacy["isCharging"] = true
        legacy["thresholdEvent"] = 0

        val core = NetworkPacket.fromLegacyPacket(legacy)

        assertEquals("cconnect.battery", core.type)
        assertEquals(85, core.body["currentCharge"])
        assertEquals(true, core.body["isCharging"])
        assertEquals(0, core.body["thresholdEvent"])
        assertNull(core.payloadSize)
    }

    @Test
    fun `fromLegacyPacket preserves nested JSONObject and JSONArray`() {
        val legacy = org.cosmic.cosmicconnect.NetworkPacket("cconnect.runcommand")
        val commandList = org.json.JSONObject()
        val cmd1 = org.json.JSONObject()
        cmd1.put("name", "Lock Screen")
        cmd1.put("command", "loginctl lock-session")
        commandList.put("cmd1", cmd1)
        legacy["commandList"] = commandList

        val stringList = org.json.JSONArray()
        stringList.put("item1")
        stringList.put("item2")
        legacy["tags"] = stringList

        val core = NetworkPacket.fromLegacyPacket(legacy)

        // Nested JSONObject should become nested Map
        val bodyCommandList = core.body["commandList"]
        assertTrue("commandList should be a Map", bodyCommandList is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        val cmdMap = (bodyCommandList as Map<String, Any>)["cmd1"] as Map<String, Any>
        assertEquals("Lock Screen", cmdMap["name"])
        assertEquals("loginctl lock-session", cmdMap["command"])

        // JSONArray should become List
        val tags = core.body["tags"]
        assertTrue("tags should be a List", tags is List<*>)
        assertEquals(listOf("item1", "item2"), tags)
    }

    @Test
    fun `fromLegacyPacket preserves payload info`() {
        val legacy = org.cosmic.cosmicconnect.NetworkPacket("cconnect.share.request")
        legacy["filename"] = "photo.jpg"
        legacy.payload = org.cosmic.cosmicconnect.NetworkPacket.Payload(2048L)
        val transferInfo = JSONObject()
        transferInfo.put("port", 1740)
        legacy.payloadTransferInfo = transferInfo

        val core = NetworkPacket.fromLegacyPacket(legacy)

        assertEquals(2048L, core.payloadSize)
        assertEquals(1740, core.payloadTransferInfo["port"])
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

    @Test
    fun `fromLegacyPacket with empty body produces empty body map`() {
        val legacy = org.cosmic.cosmicconnect.NetworkPacket("cconnect.ping")
        val core = NetworkPacket.fromLegacyPacket(legacy)
        assertEquals("cconnect.ping", core.type)
        assertTrue(core.body.isEmpty())
    }
}
