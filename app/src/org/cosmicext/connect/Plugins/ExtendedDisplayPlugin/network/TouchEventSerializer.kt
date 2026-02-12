/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.network

import org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.input.TouchAction
import org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.input.TouchEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Serializes touch events for network transmission.
 *
 * Binary format (21 bytes total):
 * - 4 bytes: X coordinate (float, normalized 0.0-1.0)
 * - 4 bytes: Y coordinate (float, normalized 0.0-1.0)
 * - 1 byte:  Action type (0=DOWN, 1=MOVE, 2=UP, 3=CANCEL)
 * - 4 bytes: Pointer ID (int)
 * - 8 bytes: Timestamp (long, milliseconds since epoch)
 */
object TouchEventSerializer {

    private const val BUFFER_SIZE = 21

    /**
     * Serialize a single touch event to binary format.
     */
    fun serialize(event: TouchEvent): ByteArray {
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putFloat(event.x)
        buffer.putFloat(event.y)
        buffer.put(event.action.ordinal.toByte())
        buffer.putInt(event.pointerId)
        buffer.putLong(event.timestamp)

        return buffer.array()
    }

    /**
     * Serialize multiple touch events (for multi-touch).
     */
    fun serializeMultiple(events: List<TouchEvent>): ByteArray {
        val buffer = ByteBuffer.allocate(4 + events.size * BUFFER_SIZE)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // Write count first
        buffer.putInt(events.size)

        // Write each event
        events.forEach { event ->
            buffer.putFloat(event.x)
            buffer.putFloat(event.y)
            buffer.put(event.action.ordinal.toByte())
            buffer.putInt(event.pointerId)
            buffer.putLong(event.timestamp)
        }

        return buffer.array()
    }

    /**
     * Deserialize a touch event from binary format.
     */
    fun deserialize(data: ByteArray): TouchEvent? {
        if (data.size < BUFFER_SIZE) return null

        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        return try {
            val x = buffer.float
            val y = buffer.float
            val actionOrdinal = buffer.get().toInt()
            val pointerId = buffer.int
            val timestamp = buffer.long

            val action = TouchAction.entries.getOrNull(actionOrdinal) ?: TouchAction.CANCEL

            TouchEvent(x, y, action, pointerId, timestamp)
        } catch (e: Exception) {
            null
        }
    }
}
