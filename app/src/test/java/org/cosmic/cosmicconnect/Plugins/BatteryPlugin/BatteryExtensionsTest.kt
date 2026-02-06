/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.BatteryPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BatteryExtensionsTest {

    private fun batteryPacket(
        charge: Int = 50,
        charging: Boolean = false,
        thresholdEvent: Int = 0
    ) = NetworkPacket(
        id = 1L,
        type = "cconnect.battery",
        body = mapOf(
            "currentCharge" to charge,
            "isCharging" to charging,
            "thresholdEvent" to thresholdEvent
        )
    )

    // ========================================================================
    // isBatteryPacket
    // ========================================================================

    @Test
    fun `isBatteryPacket returns true for valid battery packet`() {
        assertTrue(batteryPacket().isBatteryPacket)
    }

    @Test
    fun `isBatteryPacket returns false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("currentCharge" to 50, "isCharging" to false)
        )
        assertFalse(packet.isBatteryPacket)
    }

    @Test
    fun `isBatteryPacket returns false for missing fields`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = mapOf("currentCharge" to 50)
        )
        assertFalse(packet.isBatteryPacket)
    }

    // ========================================================================
    // Extension property extraction
    // ========================================================================

    @Test
    fun `batteryCurrentCharge extracts charge level`() {
        assertEquals(85, batteryPacket(charge = 85).batteryCurrentCharge)
    }

    @Test
    fun `batteryIsCharging extracts charging status`() {
        assertEquals(true, batteryPacket(charging = true).batteryIsCharging)
        assertEquals(false, batteryPacket(charging = false).batteryIsCharging)
    }

    @Test
    fun `batteryThresholdEvent extracts threshold event`() {
        assertEquals(0, batteryPacket(thresholdEvent = 0).batteryThresholdEvent)
        assertEquals(1, batteryPacket(thresholdEvent = 1).batteryThresholdEvent)
    }

    @Test
    fun `extension properties return null for non-battery packet`() {
        val packet = NetworkPacket(id = 1L, type = "cconnect.ping", body = emptyMap())
        assertNull(packet.batteryCurrentCharge)
        assertNull(packet.batteryIsCharging)
        assertNull(packet.batteryThresholdEvent)
    }

    // ========================================================================
    // isBatteryLow / isBatteryCritical
    // ========================================================================

    @Test
    fun `isBatteryLow returns true at 14 percent not charging`() {
        assertTrue(batteryPacket(charge = 14, charging = false).isBatteryLow)
    }

    @Test
    fun `isBatteryLow returns false when charging`() {
        assertFalse(batteryPacket(charge = 10, charging = true).isBatteryLow)
    }

    @Test
    fun `isBatteryLow returns false at 15 percent`() {
        assertFalse(batteryPacket(charge = 15, charging = false).isBatteryLow)
    }

    @Test
    fun `isBatteryCritical returns true at 4 percent not charging`() {
        assertTrue(batteryPacket(charge = 4, charging = false).isBatteryCritical)
    }

    @Test
    fun `isBatteryCritical returns false when charging`() {
        assertFalse(batteryPacket(charge = 2, charging = true).isBatteryCritical)
    }

    @Test
    fun `isBatteryCritical returns false at 5 percent`() {
        assertFalse(batteryPacket(charge = 5, charging = false).isBatteryCritical)
    }

    // ========================================================================
    // DeviceBatteryInfo.fromPacket
    // ========================================================================

    @Test
    fun `DeviceBatteryInfo fromPacket extracts all fields`() {
        val packet = batteryPacket(charge = 75, charging = true, thresholdEvent = 0)
        val info = DeviceBatteryInfo.fromPacket(packet)
        assertEquals(75, info.currentCharge)
        assertTrue(info.isCharging)
        assertEquals(0, info.thresholdEvent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `DeviceBatteryInfo fromPacket throws on wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("currentCharge" to 50, "isCharging" to false)
        )
        DeviceBatteryInfo.fromPacket(packet)
    }
}
