/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.input

import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects common gestures from touch event streams.
 *
 * Supports: tap, double-tap, long press, pinch zoom, two-finger scroll
 */
class GestureDetector(
    private val listener: GestureListener
) {
    companion object {
        private const val TAG = "GestureDetector"

        private const val TAP_TIMEOUT_MS = 200L
        private const val DOUBLE_TAP_TIMEOUT_MS = 300L
        private const val LONG_PRESS_TIMEOUT_MS = 500L

        private const val TAP_SLOP = 0.02f // 2% of screen
        private const val PINCH_THRESHOLD = 0.05f // 5% change to trigger zoom
    }

    private var lastTapTime = 0L
    private var lastTapX = 0f
    private var lastTapY = 0f

    private var touchDownTime = 0L
    private var touchDownX = 0f
    private var touchDownY = 0f

    private var lastPinchDistance = 0f
    private var isPinching = false

    /**
     * Process a touch event.
     *
     * @return true if the event was consumed by a gesture
     */
    fun onTouchEvent(event: TouchEvent): Boolean {
        return when (event.action) {
            TouchAction.DOWN -> handleDown(event)
            TouchAction.MOVE -> handleMove(event)
            TouchAction.UP -> handleUp(event)
            TouchAction.CANCEL -> handleCancel()
        }
    }

    /**
     * Process multiple touch events (for pinch/zoom).
     */
    fun onMultiTouchEvent(events: List<TouchEvent>): Boolean {
        if (events.size < 2) return false

        val first = events[0]
        val second = events[1]

        val distance = calculateDistance(first.x, first.y, second.x, second.y)

        if (!isPinching) {
            isPinching = true
            lastPinchDistance = distance
            return true
        }

        val delta = distance - lastPinchDistance
        if (abs(delta) > PINCH_THRESHOLD) {
            val scale = distance / lastPinchDistance
            val centerX = (first.x + second.x) / 2f
            val centerY = (first.y + second.y) / 2f

            listener.onPinch(scale, centerX, centerY)
            lastPinchDistance = distance
            return true
        }

        return false
    }

    private fun handleDown(event: TouchEvent): Boolean {
        touchDownTime = event.timestamp
        touchDownX = event.x
        touchDownY = event.y
        return false
    }

    private fun handleMove(event: TouchEvent): Boolean {
        val dx = event.x - touchDownX
        val dy = event.y - touchDownY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > TAP_SLOP) {
            // Too much movement, cancel any tap detection
            touchDownTime = 0L
        }

        return false
    }

    private fun handleUp(event: TouchEvent): Boolean {
        isPinching = false

        val tapDuration = event.timestamp - touchDownTime
        val dx = event.x - touchDownX
        val dy = event.y - touchDownY
        val distance = sqrt(dx * dx + dy * dy)

        // Check for tap (short duration, minimal movement)
        if (tapDuration < TAP_TIMEOUT_MS && distance < TAP_SLOP) {
            val timeSinceLastTap = event.timestamp - lastTapTime
            val distanceFromLastTap = sqrt(
                (event.x - lastTapX) * (event.x - lastTapX) +
                (event.y - lastTapY) * (event.y - lastTapY)
            )

            if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && distanceFromLastTap < TAP_SLOP * 2) {
                listener.onDoubleTap(event.x, event.y)
                lastTapTime = 0L
                return true
            } else {
                listener.onTap(event.x, event.y)
                lastTapTime = event.timestamp
                lastTapX = event.x
                lastTapY = event.y
                return true
            }
        }

        // Check for long press
        if (tapDuration >= LONG_PRESS_TIMEOUT_MS && distance < TAP_SLOP) {
            listener.onLongPress(event.x, event.y)
            return true
        }

        return false
    }

    private fun handleCancel(): Boolean {
        isPinching = false
        touchDownTime = 0L
        return false
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Listener for detected gestures.
 */
interface GestureListener {
    fun onTap(x: Float, y: Float) {}
    fun onDoubleTap(x: Float, y: Float) {}
    fun onLongPress(x: Float, y: Float) {}
    fun onPinch(scale: Float, centerX: Float, centerY: Float) {}
    fun onTwoFingerScroll(deltaX: Float, deltaY: Float) {}
}
