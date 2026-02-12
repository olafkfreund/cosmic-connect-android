/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.ScreenSharePlugin.streaming

/**
 * Represents a single CSMR (COSMIC Media Relay) frame received over TCP.
 *
 * Wire format: 4B "CSMR" | 1B type | 8B timestamp | 4B size | NB payload
 * Total header size: 17 bytes.
 */
data class CsmrFrame(
    val type: Byte,
    val timestampNs: Long,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CsmrFrame) return false
        return type == other.type && timestampNs == other.timestampNs && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + timestampNs.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        const val MAGIC = "CSMR"
        const val HEADER_SIZE = 17 // 4 magic + 1 type + 8 timestamp + 4 size
        const val TYPE_VIDEO: Byte = 0x01
        const val TYPE_CURSOR: Byte = 0x02
        const val TYPE_ANNOTATION: Byte = 0x03
        val TYPE_END_OF_STREAM: Byte = 0xFF.toByte()
        const val MAX_PAYLOAD_SIZE = 2 * 1024 * 1024 // 2MB sanity limit
    }
}
