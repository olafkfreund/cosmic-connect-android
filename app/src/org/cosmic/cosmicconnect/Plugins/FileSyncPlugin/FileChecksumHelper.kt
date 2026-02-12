/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FileSyncPlugin

import java.io.InputStream
import java.security.MessageDigest

/**
 * Utility for computing file checksums used in sync conflict detection.
 *
 * Uses SHA-256 for reliable content-based deduplication and change detection.
 */
object FileChecksumHelper {

    private const val ALGORITHM = "SHA-256"
    private const val BUFFER_SIZE = 8192

    /**
     * Compute SHA-256 hex digest from an input stream.
     *
     * Reads the stream in 8 KB chunks to support arbitrarily large files
     * without loading everything into memory.
     *
     * @param inputStream The stream to hash (caller is responsible for closing)
     * @return Lowercase 64-character hex string
     */
    fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compute SHA-256 hex digest from a byte array.
     *
     * Convenience method for small in-memory data.
     *
     * @param data The bytes to hash
     * @return Lowercase 64-character hex string
     */
    fun computeSha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance(ALGORITHM)
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}
