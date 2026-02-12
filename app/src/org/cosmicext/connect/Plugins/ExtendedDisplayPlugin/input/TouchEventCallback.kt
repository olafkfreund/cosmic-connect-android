/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.input

import android.util.Log

/**
 * Callback interface for receiving touch input events from the extended display.
 */
interface TouchEventCallback {
    /**
     * Called when a single touch event occurs.
     */
    fun onTouchEvent(event: TouchEvent)

    /**
     * Called when multiple touch events occur simultaneously (multi-touch).
     */
    fun onMultiTouchEvent(events: List<TouchEvent>)

    /**
     * Called when touch input handling encounters an error.
     */
    fun onTouchError(error: Exception, event: TouchEvent? = null) {
        Log.e("TouchEventCallback", "Touch input error", error)
    }
}

/**
 * Simple implementation that logs touch events (useful for debugging).
 */
class LoggingTouchEventCallback : TouchEventCallback {
    private val tag = "TouchEvents"

    override fun onTouchEvent(event: TouchEvent) {
        Log.d(tag, "Touch: ${event.action} at (${event.x}, ${event.y}) pointer=${event.pointerId}")
    }

    override fun onMultiTouchEvent(events: List<TouchEvent>) {
        Log.d(tag, "Multi-touch: ${events.size} pointers - ${events.joinToString { "${it.action}@${it.pointerId}" }}")
    }
}
