/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Core

/**
 * TransferPacket bundles an immutable Core.NetworkPacket with mutable transport state.
 *
 * This bridges the gap between the immutable packet data model and the mutable
 * state needed during transport (payload stream, cancellation, runtime transfer info).
 *
 * ## Usage
 * ```kotlin
 * // Outgoing: plugin creates Core packet, wraps with payload for transport
 * val tp = TransferPacket(corePacket, payload = corePayload)
 * device.sendPacket(tp, callback)
 *
 * // Transport layer sets runtime info and serializes
 * tp.runtimeTransferInfo = mapOf("port" to serverPort)
 * val wireBytes = tp.serializeForWire().toByteArray(Charsets.UTF_8)
 * ```
 */
class TransferPacket(
    val packet: NetworkPacket,
    var payload: Payload? = null,
    @Volatile var isCanceled: Boolean = false
) {
    /**
     * Runtime payloadTransferInfo set by the transport layer (e.g., port for TCP payload).
     * Merged into packet during serialization.
     */
    var runtimeTransferInfo: Map<String, Any> = emptyMap()

    fun cancel() {
        isCanceled = true
    }

    val hasPayload: Boolean
        get() = payload != null && (payload?.payloadSize ?: 0) > 0

    val payloadSize: Long
        get() = payload?.payloadSize ?: 0

    val type: String
        get() = packet.type

    /**
     * Serialize for wire transmission, merging runtime transfer info into the packet.
     *
     * Uses Kotlin-native serialization (no FFI, no legacy intermediate).
     */
    fun serializeForWire(): String {
        val merged = if (hasPayload) {
            packet.copy(
                payloadSize = payloadSize,
                payloadTransferInfo = runtimeTransferInfo.ifEmpty { packet.payloadTransferInfo }
            )
        } else {
            packet
        }
        return merged.serializeKotlin()
    }

    override fun toString(): String = buildString {
        append("TransferPacket(type=${packet.type}")
        if (hasPayload) append(", payloadSize=$payloadSize")
        if (isCanceled) append(", CANCELED")
        append(")")
    }
}
