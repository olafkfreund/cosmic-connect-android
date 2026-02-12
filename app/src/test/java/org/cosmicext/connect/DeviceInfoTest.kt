/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect

import io.mockk.mockk
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.PacketType
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.cert.Certificate

@RunWith(RobolectricTestRunner::class)
class DeviceInfoTest {

    private val mockCertificate: Certificate = mockk(relaxed = true)

    // ========================================================================
    // isValidDeviceId
    // ========================================================================

    @Test
    fun `isValidDeviceId accepts 32-char alphanumeric ID`() {
        assertTrue(DeviceInfo.isValidDeviceId("abcdef1234567890abcdef1234567890"))
    }

    @Test
    fun `isValidDeviceId accepts 36-char ID with underscores and hyphens`() {
        assertTrue(DeviceInfo.isValidDeviceId("4dc6f1cb_ef38_4ce3_a9c6_e469cc6ba147"))
    }

    @Test
    fun `isValidDeviceId rejects too short ID`() {
        assertFalse(DeviceInfo.isValidDeviceId("abc123"))
    }

    @Test
    fun `isValidDeviceId rejects too long ID`() {
        assertFalse(DeviceInfo.isValidDeviceId("a".repeat(39)))
    }

    @Test
    fun `isValidDeviceId rejects special characters`() {
        assertFalse(DeviceInfo.isValidDeviceId("abcdef1234567890abcdef123456789!"))
    }

    @Test
    fun `isValidDeviceId rejects empty string`() {
        assertFalse(DeviceInfo.isValidDeviceId(""))
    }

    // ========================================================================
    // fromIdentityPacketAndCert
    // ========================================================================

    @Test
    fun `fromIdentityPacketAndCert parses all fields`() {
        val inCaps = JSONArray(listOf("cconnect.ping", "cconnect.battery")).toString()
        val outCaps = JSONArray(listOf("cconnect.clipboard")).toString()
        val packet = NetworkPacket(
            id = 1L,
            type = PacketType.IDENTITY,
            body = mapOf(
                "deviceId" to "abcdef1234567890abcdef1234567890",
                "deviceName" to "Test Device",
                "deviceType" to "phone",
                "protocolVersion" to 8,
                "incomingCapabilities" to inCaps,
                "outgoingCapabilities" to outCaps
            )
        )

        val info = DeviceInfo.fromIdentityPacketAndCert(packet, mockCertificate)

        assertEquals("abcdef1234567890abcdef1234567890", info.id)
        assertEquals("Test Device", info.name)
        assertEquals(DeviceType.PHONE, info.type)
        assertEquals(8, info.protocolVersion)
        assertEquals(setOf("cconnect.ping", "cconnect.battery"), info.incomingCapabilities)
        assertEquals(setOf("cconnect.clipboard"), info.outgoingCapabilities)
    }

    @Test
    fun `fromIdentityPacketAndCert filters name with special characters`() {
        val packet = NetworkPacket(
            id = 1L,
            type = PacketType.IDENTITY,
            body = mapOf(
                "deviceId" to "abcdef1234567890abcdef1234567890",
                "deviceName" to "My \"Device\" (test)",
                "deviceType" to "desktop",
                "protocolVersion" to 7
            )
        )

        val info = DeviceInfo.fromIdentityPacketAndCert(packet, mockCertificate)
        assertEquals("My Device test", info.name)
    }

    // ========================================================================
    // isValidIdentityPacket
    // ========================================================================

    @Test
    fun `isValidIdentityPacket returns true for valid packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = PacketType.IDENTITY,
            body = mapOf(
                "deviceId" to "abcdef1234567890abcdef1234567890",
                "deviceName" to "Test Device"
            )
        )
        assertTrue(DeviceInfo.isValidIdentityPacket(packet))
    }

    @Test
    fun `isValidIdentityPacket returns false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = PacketType.PING,
            body = mapOf(
                "deviceId" to "abcdef1234567890abcdef1234567890",
                "deviceName" to "Test Device"
            )
        )
        assertFalse(DeviceInfo.isValidIdentityPacket(packet))
    }

    @Test
    fun `isValidIdentityPacket returns false for blank name`() {
        val packet = NetworkPacket(
            id = 1L,
            type = PacketType.IDENTITY,
            body = mapOf(
                "deviceId" to "abcdef1234567890abcdef1234567890",
                "deviceName" to ""
            )
        )
        assertFalse(DeviceInfo.isValidIdentityPacket(packet))
    }

    @Test
    fun `isValidIdentityPacket returns false for invalid deviceId`() {
        val packet = NetworkPacket(
            id = 1L,
            type = PacketType.IDENTITY,
            body = mapOf(
                "deviceId" to "short",
                "deviceName" to "Test Device"
            )
        )
        assertFalse(DeviceInfo.isValidIdentityPacket(packet))
    }

    // ========================================================================
    // DeviceType.fromString
    // ========================================================================

    @Test
    fun `DeviceType fromString round-trips all types`() {
        assertEquals(DeviceType.PHONE, DeviceType.fromString("phone"))
        assertEquals(DeviceType.TABLET, DeviceType.fromString("tablet"))
        assertEquals(DeviceType.TV, DeviceType.fromString("tv"))
        assertEquals(DeviceType.LAPTOP, DeviceType.fromString("laptop"))
        assertEquals(DeviceType.DESKTOP, DeviceType.fromString("desktop"))
        assertEquals(DeviceType.DESKTOP, DeviceType.fromString("unknown"))
    }

    @Test
    fun `DeviceType toString round-trips all types`() {
        assertEquals("phone", DeviceType.PHONE.toString())
        assertEquals("tablet", DeviceType.TABLET.toString())
        assertEquals("tv", DeviceType.TV.toString())
        assertEquals("laptop", DeviceType.LAPTOP.toString())
        assertEquals("desktop", DeviceType.DESKTOP.toString())
    }
}
