/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming

import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Reads CSMR-framed data from a TCP InputStream.
 *
 * Usage:
 * ```
 * val receiver = CsmrStreamReceiver(socket.inputStream)
 * while (true) {
 *     val frame = receiver.readFrame() ?: break  // null = clean EOF
 *     when (frame.type) {
 *         CsmrFrame.TYPE_VIDEO -> decoder.decodeFrame(frame.payload, frame.timestampNs / 1000)
 *         CsmrFrame.TYPE_END_OF_STREAM -> break
 *     }
 * }
 * ```
 */
class CsmrStreamReceiver(inputStream: InputStream) {

    private val dis = DataInputStream(inputStream)

    /**
     * Reads the next CSMR frame from the stream.
     *
     * @return The parsed frame, or null on clean EOF (stream closed).
     * @throws IOException on read error
     * @throws CsmrProtocolException on invalid magic or oversized payload
     */
    fun readFrame(): CsmrFrame? {
        // Read 4-byte magic
        val magicBytes = ByteArray(4)
        try {
            dis.readFully(magicBytes)
        } catch (e: EOFException) {
            return null // Clean EOF
        }

        val magic = String(magicBytes, StandardCharsets.US_ASCII)
        if (magic != CsmrFrame.MAGIC) {
            throw CsmrProtocolException("Invalid CSMR magic: expected '${CsmrFrame.MAGIC}', got '$magic'")
        }

        // Read 1-byte type
        val type = dis.readByte()

        // Read 8-byte timestamp (big-endian)
        val timestampNs = dis.readLong()

        // Read 4-byte payload size (big-endian, unsigned)
        val payloadSize = dis.readInt()
        if (payloadSize < 0 || payloadSize > CsmrFrame.MAX_PAYLOAD_SIZE) {
            throw CsmrProtocolException(
                "Invalid payload size: $payloadSize (max ${CsmrFrame.MAX_PAYLOAD_SIZE})"
            )
        }

        // Read payload
        val payload = ByteArray(payloadSize)
        if (payloadSize > 0) {
            dis.readFully(payload)
        }

        return CsmrFrame(type, timestampNs, payload)
    }
}
