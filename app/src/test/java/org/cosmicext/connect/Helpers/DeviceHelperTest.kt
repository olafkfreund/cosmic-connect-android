/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceHelperTest {

    // ========================================================================
    // filterInvalidCharactersFromDeviceName
    // ========================================================================

    @Test
    fun `filterInvalidCharactersFromDeviceName removes special characters`() {
        val input = """He"ll'o, W;o:r.l!d? (test)[array]<angle>"""
        val result = DeviceHelper.filterInvalidCharactersFromDeviceName(input)
        assertEquals("Hello World testarrayangle", result)
    }

    @Test
    fun `filterInvalidCharactersFromDeviceName preserves valid characters`() {
        val input = "My Device-Name_123 Test"
        val result = DeviceHelper.filterInvalidCharactersFromDeviceName(input)
        assertEquals("My Device-Name_123 Test", result)
    }

    @Test
    fun `filterInvalidCharactersFromDeviceName handles empty string`() {
        val result = DeviceHelper.filterInvalidCharactersFromDeviceName("")
        assertEquals("", result)
    }

    @Test
    fun `filterInvalidCharactersFromDeviceName handles all invalid characters`() {
        val input = "\"',;:.!?()[]<>"
        val result = DeviceHelper.filterInvalidCharactersFromDeviceName(input)
        assertEquals("", result)
    }

    // ========================================================================
    // filterInvalidCharactersFromDeviceNameAndLimitLength
    // ========================================================================

    @Test
    fun `filterInvalidCharactersFromDeviceNameAndLimitLength truncates to 32 chars`() {
        val input = "A".repeat(50)
        val result = DeviceHelper.filterInvalidCharactersFromDeviceNameAndLimitLength(input)
        assertEquals(32, result.length)
        assertEquals("A".repeat(32), result)
    }

    @Test
    fun `filterInvalidCharactersFromDeviceNameAndLimitLength trims whitespace before truncation`() {
        val input = "   My Device   "
        val result = DeviceHelper.filterInvalidCharactersFromDeviceNameAndLimitLength(input)
        assertEquals("My Device", result)
    }

    @Test
    fun `filterInvalidCharactersFromDeviceNameAndLimitLength filters and truncates combined`() {
        val input = "  " + "A!B".repeat(20) + "  "
        val result = DeviceHelper.filterInvalidCharactersFromDeviceNameAndLimitLength(input)
        assertTrue(result.length <= 32)
        assertTrue(!result.contains("!"))
    }

    // ========================================================================
    // Constants
    // ========================================================================

    @Test
    fun `PROTOCOL_VERSION is 8`() {
        assertEquals(8, DeviceHelper.PROTOCOL_VERSION)
    }

    @Test
    fun `MAX_DEVICE_NAME_LENGTH is 32`() {
        assertEquals(32, DeviceHelper.MAX_DEVICE_NAME_LENGTH)
    }
}
