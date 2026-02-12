/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.decoder

import android.util.Log

/**
 * Monitors decode latency and frame rate for performance tracking.
 */
class LatencyMonitor {

    companion object {
        private const val TAG = "LatencyMonitor"
        private const val WINDOW_SIZE = 60 // 1 second at 60fps
    }

    private val frameTimes = ArrayDeque<Long>(WINDOW_SIZE)
    private val latencies = ArrayDeque<Long>(WINDOW_SIZE)

    private var lastFrameTime = 0L
    private var totalFrames = 0L

    /**
     * Record a frame decode completion.
     *
     * @param decodeStartTime When decoding started (from System.nanoTime())
     */
    fun recordFrame(decodeStartTime: Long) {
        val now = System.nanoTime()
        val latencyNs = now - decodeStartTime
        val latencyMs = latencyNs / 1_000_000

        synchronized(this) {
            // Track frame timing
            if (frameTimes.size >= WINDOW_SIZE) {
                frameTimes.removeFirst()
            }
            frameTimes.addLast(now)

            // Track latency
            if (latencies.size >= WINDOW_SIZE) {
                latencies.removeFirst()
            }
            latencies.addLast(latencyMs)

            lastFrameTime = now
            totalFrames++
        }

        if (totalFrames % 300 == 0L) {
            Log.d(TAG, "Stats: fps=${getAverageFps().toInt()}, latency=${getAverageLatency()}ms")
        }
    }

    /**
     * Get average FPS over the last second.
     */
    fun getAverageFps(): Float {
        synchronized(this) {
            if (frameTimes.size < 2) return 0f

            val firstTime = frameTimes.first()
            val lastTime = frameTimes.last()
            val durationNs = lastTime - firstTime

            if (durationNs <= 0) return 0f

            return (frameTimes.size - 1) * 1_000_000_000f / durationNs
        }
    }

    /**
     * Get average decode latency in milliseconds.
     */
    fun getAverageLatency(): Long {
        synchronized(this) {
            if (latencies.isEmpty()) return 0L
            return latencies.average().toLong()
        }
    }

    /**
     * Get the minimum latency over the window.
     */
    fun getMinLatency(): Long {
        synchronized(this) {
            return latencies.minOrNull() ?: 0L
        }
    }

    /**
     * Get the maximum latency over the window.
     */
    fun getMaxLatency(): Long {
        synchronized(this) {
            return latencies.maxOrNull() ?: 0L
        }
    }

    /**
     * Get total frames decoded.
     */
    fun getTotalFrames(): Long = totalFrames

    /**
     * Reset all statistics.
     */
    fun reset() {
        synchronized(this) {
            frameTimes.clear()
            latencies.clear()
            lastFrameTime = 0L
            totalFrames = 0L
        }
    }
}
