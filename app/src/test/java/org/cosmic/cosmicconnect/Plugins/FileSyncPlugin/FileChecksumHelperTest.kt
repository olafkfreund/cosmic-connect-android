/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FileSyncPlugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FileChecksumHelperTest {

    @Test
    fun `computeSha256 of empty stream returns known SHA-256 of empty input`() {
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val result = FileChecksumHelper.computeSha256(ByteArrayInputStream(ByteArray(0)))
        assertEquals(expected, result)
    }

    @Test
    fun `computeSha256 of hello world returns known hash`() {
        // SHA-256("hello world") well-known value
        val expected = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
        val result = FileChecksumHelper.computeSha256(ByteArrayInputStream("hello world".toByteArray()))
        assertEquals(expected, result)
    }

    @Test
    fun `computeSha256Hex of empty byte array matches known hash`() {
        val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val result = FileChecksumHelper.computeSha256Hex(ByteArray(0))
        assertEquals(expected, result)
    }

    @Test
    fun `computeSha256Hex of hello world matches stream variant`() {
        val data = "hello world".toByteArray()
        val fromHex = FileChecksumHelper.computeSha256Hex(data)
        val fromStream = FileChecksumHelper.computeSha256(ByteArrayInputStream(data))
        assertEquals(fromHex, fromStream)
    }

    @Test
    fun `output is 64 lowercase hex characters`() {
        val result = FileChecksumHelper.computeSha256Hex("test data".toByteArray())
        assertEquals(64, result.length)
        assertTrue(result.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `binary data produces valid hash`() {
        val binaryData = ByteArray(256) { it.toByte() }
        val result = FileChecksumHelper.computeSha256Hex(binaryData)
        assertEquals(64, result.length)
        assertTrue(result.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `large input stream is hashed correctly`() {
        // Create a stream larger than the 8192-byte buffer
        val data = ByteArray(32_000) { (it % 256).toByte() }
        val fromStream = FileChecksumHelper.computeSha256(ByteArrayInputStream(data))
        val fromHex = FileChecksumHelper.computeSha256Hex(data)
        assertEquals(fromHex, fromStream)
    }

    @Test
    fun `different inputs produce different hashes`() {
        val hash1 = FileChecksumHelper.computeSha256Hex("alpha".toByteArray())
        val hash2 = FileChecksumHelper.computeSha256Hex("beta".toByteArray())
        assertNotEquals(hash1, hash2)
    }
}
