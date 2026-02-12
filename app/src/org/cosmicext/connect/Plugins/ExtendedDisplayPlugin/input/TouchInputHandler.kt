/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.input

import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Handles touch input capture from a View and delivers events via callback.
 *
 * @property view The View to capture touch events from
 * @property callback Callback to receive normalized touch events
 */
class TouchInputHandler(
    private val view: View,
    private val callback: TouchEventCallback
) {
    private val tag = "TouchInputHandler"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isAttached = false
    private val multiTouchCache = mutableMapOf<Int, TouchEvent>()

    /**
     * Attaches the touch listener to the View.
     */
    fun attach() {
        if (isAttached) {
            Log.w(tag, "TouchInputHandler already attached")
            return
        }

        view.setOnTouchListener { v, event -> handleTouch(v, event) }
        isAttached = true
        Log.d(tag, "TouchInputHandler attached to view")
    }

    /**
     * Detaches the touch listener and cleans up resources.
     */
    fun detach() {
        if (!isAttached) {
            return
        }

        view.setOnTouchListener(null)
        scope.cancel()
        multiTouchCache.clear()
        isAttached = false
        Log.d(tag, "TouchInputHandler detached from view")
    }

    private fun handleTouch(v: View, event: MotionEvent): Boolean {
        try {
            val pointerCount = event.pointerCount

            if (pointerCount > 1) {
                handleMultiTouch(v, event)
            } else {
                handleSingleTouch(v, event)
            }

            return true
        } catch (e: Exception) {
            Log.e(tag, "Error handling touch event", e)
            callback.onTouchError(e)
            return false
        }
    }

    private fun handleSingleTouch(v: View, event: MotionEvent) {
        val touchEvent = createTouchEvent(v, event, 0)

        scope.launch {
            try {
                callback.onTouchEvent(touchEvent)
            } catch (e: Exception) {
                Log.e(tag, "Error in callback.onTouchEvent", e)
                callback.onTouchError(e, touchEvent)
            }
        }
    }

    private fun handleMultiTouch(v: View, event: MotionEvent) {
        val action = event.actionMasked
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)

        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                val touchEvent = createTouchEvent(v, event, actionIndex)
                multiTouchCache[pointerId] = touchEvent

                scope.launch {
                    try {
                        callback.onTouchEvent(touchEvent)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in callback.onTouchEvent (multi-down)", e)
                        callback.onTouchError(e, touchEvent)
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val touchEvent = createTouchEvent(v, event, actionIndex)
                multiTouchCache.remove(pointerId)

                scope.launch {
                    try {
                        callback.onTouchEvent(touchEvent)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in callback.onTouchEvent (multi-up)", e)
                        callback.onTouchError(e, touchEvent)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val events = mutableListOf<TouchEvent>()

                for (i in 0 until event.pointerCount) {
                    val touchEvent = createTouchEvent(v, event, i)
                    events.add(touchEvent)
                    multiTouchCache[event.getPointerId(i)] = touchEvent
                }

                scope.launch {
                    try {
                        callback.onMultiTouchEvent(events)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in callback.onMultiTouchEvent", e)
                        callback.onTouchError(e)
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                val events = multiTouchCache.values.map { cachedEvent ->
                    cachedEvent.copy(action = TouchAction.CANCEL, timestamp = System.currentTimeMillis())
                }

                multiTouchCache.clear()

                scope.launch {
                    try {
                        callback.onMultiTouchEvent(events)
                    } catch (e: Exception) {
                        Log.e(tag, "Error in callback.onMultiTouchEvent (cancel)", e)
                        callback.onTouchError(e)
                    }
                }
            }
        }
    }

    private fun createTouchEvent(view: View, event: MotionEvent, pointerIndex: Int): TouchEvent {
        val rawX = event.getX(pointerIndex)
        val rawY = event.getY(pointerIndex)

        val normalizedX = if (view.width > 0) {
            (rawX / view.width).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        val normalizedY = if (view.height > 0) {
            (rawY / view.height).coerceIn(0f, 1f)
        } else {
            0.5f
        }

        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> TouchAction.DOWN
            MotionEvent.ACTION_MOVE -> TouchAction.MOVE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> TouchAction.UP
            MotionEvent.ACTION_CANCEL -> TouchAction.CANCEL
            else -> TouchAction.CANCEL
        }

        val pointerId = event.getPointerId(pointerIndex)

        return TouchEvent.create(
            x = normalizedX,
            y = normalizedY,
            action = action,
            pointerId = pointerId
        )
    }

    fun isAttached(): Boolean = isAttached

    fun getActivePointerCount(): Int = multiTouchCache.size
}
