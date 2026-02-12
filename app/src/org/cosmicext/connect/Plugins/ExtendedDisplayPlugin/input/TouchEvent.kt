/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.input

/**
 * Represents a normalized touch event for remote input transmission.
 *
 * Coordinates are normalized to 0.0-1.0 range to be resolution-independent.
 *
 * @property x Normalized X coordinate (0.0 = left edge, 1.0 = right edge)
 * @property y Normalized Y coordinate (0.0 = top edge, 1.0 = bottom edge)
 * @property action The type of touch action performed
 * @property pointerId Unique identifier for this touch pointer (supports multi-touch)
 * @property timestamp Milliseconds since epoch when event occurred
 */
data class TouchEvent(
    val x: Float,
    val y: Float,
    val action: TouchAction,
    val pointerId: Int,
    val timestamp: Long
) {
    init {
        require(x in 0f..1f) { "x coordinate must be in range [0.0, 1.0], got $x" }
        require(y in 0f..1f) { "y coordinate must be in range [0.0, 1.0], got $y" }
        require(pointerId >= 0) { "pointerId must be non-negative, got $pointerId" }
        require(timestamp > 0) { "timestamp must be positive, got $timestamp" }
    }

    /**
     * Converts this touch event to a JSON string for network transmission.
     */
    fun toJson(): String {
        return """{"x":$x,"y":$y,"action":"${action.name}","pointerId":$pointerId,"timestamp":$timestamp}"""
    }

    companion object {
        /**
         * Creates a TouchEvent with the current system time.
         */
        fun create(
            x: Float,
            y: Float,
            action: TouchAction,
            pointerId: Int
        ): TouchEvent {
            return TouchEvent(
                x = x.coerceIn(0f, 1f),
                y = y.coerceIn(0f, 1f),
                action = action,
                pointerId = pointerId,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Touch action types corresponding to Android MotionEvent actions.
 */
enum class TouchAction {
    DOWN,
    MOVE,
    UP,
    CANCEL
}
