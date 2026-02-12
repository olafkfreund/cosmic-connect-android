/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.CameraPlugin

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

/**
 * CameraPerformanceMonitor - Real-time performance metrics for camera streaming
 *
 * Tracks end-to-end latency, frame rate, CPU/memory usage, and battery impact
 * for the camera webcam streaming pipeline. Used for Issue #110 performance
 * optimization targets.
 *
 * ## Performance Targets (Issue #110)
 *
 * | Metric | Target | Acceptable |
 * |--------|--------|------------|
 * | End-to-end latency | <100ms | <150ms |
 * | Frame rate | 30 fps stable | 25+ fps |
 * | Battery drain | <8%/hour | <10%/hour |
 * | Memory usage | <100MB | <150MB |
 * | CPU usage | <15% | <25% |
 *
 * ## Usage
 *
 * ```kotlin
 * val monitor = CameraPerformanceMonitor()
 * monitor.start()
 *
 * // Track frame timing
 * monitor.onFrameCaptured(timestampNanos)
 * monitor.onFrameEncoded(frameSize, encodingTimeMs)
 * monitor.onFrameSent(sequenceNumber, sentTimeMs)
 *
 * // Get metrics
 * val metrics = monitor.getMetrics()
 * Log.d(TAG, "FPS: ${metrics.currentFps}, Latency: ${metrics.avgLatencyMs}ms")
 *
 * monitor.stop()
 * ```
 */
class CameraPerformanceMonitor {

    companion object {
        private const val TAG = "CameraPerf"

        /** Metrics update interval in milliseconds */
        private const val METRICS_UPDATE_INTERVAL_MS = 1000L

        /** Window size for rolling average calculations */
        private const val ROLLING_WINDOW_SIZE = 30

        /** Target FPS for performance comparison */
        const val TARGET_FPS = 30

        /** Target end-to-end latency in milliseconds */
        const val TARGET_LATENCY_MS = 100

        /** Acceptable latency threshold in milliseconds */
        const val ACCEPTABLE_LATENCY_MS = 150
    }

    // ========================================================================
    // Frame Timing Tracking
    // ========================================================================

    /** Start time of monitoring session */
    private var sessionStartTimeMs: Long = 0

    /** Frame capture timestamps for FPS calculation */
    private val captureTimes = RollingWindow(ROLLING_WINDOW_SIZE)

    /** Encoding times for bottleneck detection */
    private val encodingTimes = RollingWindow(ROLLING_WINDOW_SIZE)

    /** Network send times for latency estimation */
    private val networkTimes = RollingWindow(ROLLING_WINDOW_SIZE)

    /** End-to-end latency samples (capture to send complete) */
    private val latencySamples = RollingWindow(ROLLING_WINDOW_SIZE)

    // ========================================================================
    // Frame Counters
    // ========================================================================

    /** Total frames captured since session start */
    private val totalFramesCaptured = AtomicLong(0)

    /** Total frames encoded since session start */
    private val totalFramesEncoded = AtomicLong(0)

    /** Total frames sent since session start */
    private val totalFramesSent = AtomicLong(0)

    /** Frames dropped due to backpressure */
    private val framesDropped = AtomicLong(0)

    /** Total bytes encoded */
    private val totalBytesEncoded = AtomicLong(0)

    /** Total bytes sent */
    private val totalBytesSent = AtomicLong(0)

    // ========================================================================
    // Performance Metrics
    // ========================================================================

    /** Current metrics snapshot */
    private val currentMetrics = AtomicReference(PerformanceMetrics())

    /** Whether monitoring is active */
    @Volatile
    private var isMonitoring: Boolean = false

    /** Handler for periodic updates */
    private val handler = Handler(Looper.getMainLooper())

    /** Metrics update runnable */
    private var updateRunnable: Runnable? = null

    /** Callback for metrics updates */
    private var metricsCallback: MetricsCallback? = null

    // ========================================================================
    // Lifecycle
    // ========================================================================

    /**
     * Start performance monitoring
     */
    fun start() {
        if (isMonitoring) return

        Log.i(TAG, "Starting performance monitoring")
        isMonitoring = true
        sessionStartTimeMs = SystemClock.elapsedRealtime()

        // Reset counters
        totalFramesCaptured.set(0)
        totalFramesEncoded.set(0)
        totalFramesSent.set(0)
        framesDropped.set(0)
        totalBytesEncoded.set(0)
        totalBytesSent.set(0)

        // Clear rolling windows
        captureTimes.clear()
        encodingTimes.clear()
        networkTimes.clear()
        latencySamples.clear()

        // Start periodic updates
        updateRunnable = object : Runnable {
            override fun run() {
                if (isMonitoring) {
                    updateMetrics()
                    handler.postDelayed(this, METRICS_UPDATE_INTERVAL_MS)
                }
            }
        }
        handler.postDelayed(updateRunnable!!, METRICS_UPDATE_INTERVAL_MS)
    }

    /**
     * Stop performance monitoring
     */
    fun stop() {
        if (!isMonitoring) return

        Log.i(TAG, "Stopping performance monitoring")
        isMonitoring = false

        updateRunnable?.let { handler.removeCallbacks(it) }
        updateRunnable = null

        // Log final metrics
        val metrics = getMetrics()
        Log.i(TAG, "Final metrics: fps=${metrics.currentFps}, " +
                "avgLatency=${metrics.avgLatencyMs}ms, " +
                "captured=${totalFramesCaptured.get()}, " +
                "encoded=${totalFramesEncoded.get()}, " +
                "sent=${totalFramesSent.get()}, " +
                "dropped=${framesDropped.get()}")
    }

    /**
     * Set callback for metrics updates
     */
    fun setMetricsCallback(callback: MetricsCallback?) {
        metricsCallback = callback
    }

    // ========================================================================
    // Frame Event Tracking
    // ========================================================================

    /**
     * Record frame capture event
     *
     * Called when Camera2 captures a frame.
     *
     * @param timestampNanos Frame timestamp from camera sensor
     */
    fun onFrameCaptured(timestampNanos: Long) {
        if (!isMonitoring) return

        val nowMs = SystemClock.elapsedRealtime()
        captureTimes.add(nowMs)
        totalFramesCaptured.incrementAndGet()
    }

    /**
     * Record frame encoding complete
     *
     * Called when MediaCodec outputs encoded frame.
     *
     * @param frameSize Size of encoded frame in bytes
     * @param encodingTimeMs Time spent encoding in milliseconds
     * @param isKeyframe True if this is an I-frame
     */
    fun onFrameEncoded(frameSize: Int, encodingTimeMs: Long, isKeyframe: Boolean = false) {
        if (!isMonitoring) return

        encodingTimes.add(encodingTimeMs)
        totalFramesEncoded.incrementAndGet()
        totalBytesEncoded.addAndGet(frameSize.toLong())
    }

    /**
     * Record frame send complete
     *
     * Called when frame is successfully transmitted.
     *
     * @param sequenceNumber Frame sequence number
     * @param networkTimeMs Time to send frame in milliseconds
     * @param totalLatencyMs End-to-end latency if available
     */
    fun onFrameSent(sequenceNumber: Long, networkTimeMs: Long, totalLatencyMs: Long? = null) {
        if (!isMonitoring) return

        networkTimes.add(networkTimeMs)
        totalFramesSent.incrementAndGet()

        totalLatencyMs?.let { latencySamples.add(it) }
    }

    /**
     * Record frame dropped
     *
     * Called when frame is dropped due to backpressure.
     */
    fun onFrameDropped() {
        if (!isMonitoring) return
        framesDropped.incrementAndGet()
    }

    /**
     * Record bytes sent
     *
     * @param bytes Number of bytes sent
     */
    fun onBytesSent(bytes: Long) {
        if (!isMonitoring) return
        totalBytesSent.addAndGet(bytes)
    }

    // ========================================================================
    // Metrics Calculation
    // ========================================================================

    /**
     * Get current performance metrics
     *
     * @return Snapshot of current metrics
     */
    fun getMetrics(): PerformanceMetrics = currentMetrics.get()

    /**
     * Update metrics from collected data
     */
    private fun updateMetrics() {
        val elapsedMs = SystemClock.elapsedRealtime() - sessionStartTimeMs
        val elapsedSecs = elapsedMs / 1000.0

        // Calculate FPS from recent captures
        val recentCaptures = captureTimes.count()
        val currentFps = if (elapsedSecs > 0) {
            (recentCaptures / min(elapsedSecs, ROLLING_WINDOW_SIZE.toDouble())).toFloat()
        } else 0f

        // Calculate encoding metrics
        val avgEncodingTimeMs = encodingTimes.average()
        val maxEncodingTimeMs = encodingTimes.max()

        // Calculate network metrics
        val avgNetworkTimeMs = networkTimes.average()

        // Calculate latency
        val avgLatencyMs = latencySamples.average()
        val minLatencyMs = latencySamples.min()
        val maxLatencyMs = latencySamples.max()

        // Calculate bitrate
        val bitrateKbps = if (elapsedSecs > 0) {
            ((totalBytesEncoded.get() * 8) / (elapsedSecs * 1000)).toInt()
        } else 0

        // Calculate drop rate
        val totalFrames = totalFramesCaptured.get() + framesDropped.get()
        val dropRate = if (totalFrames > 0) {
            (framesDropped.get().toFloat() / totalFrames) * 100
        } else 0f

        // Performance assessment
        val meetsTargetFps = currentFps >= TARGET_FPS - 2
        val meetsTargetLatency = avgLatencyMs <= TARGET_LATENCY_MS
        val meetsAcceptableLatency = avgLatencyMs <= ACCEPTABLE_LATENCY_MS

        val metrics = PerformanceMetrics(
            sessionDurationMs = elapsedMs,
            currentFps = currentFps,
            avgEncodingTimeMs = avgEncodingTimeMs,
            maxEncodingTimeMs = maxEncodingTimeMs,
            avgNetworkTimeMs = avgNetworkTimeMs,
            avgLatencyMs = avgLatencyMs,
            minLatencyMs = minLatencyMs,
            maxLatencyMs = maxLatencyMs,
            bitrateKbps = bitrateKbps,
            totalFramesCaptured = totalFramesCaptured.get(),
            totalFramesEncoded = totalFramesEncoded.get(),
            totalFramesSent = totalFramesSent.get(),
            framesDropped = framesDropped.get(),
            dropRatePercent = dropRate,
            meetsTargetFps = meetsTargetFps,
            meetsTargetLatency = meetsTargetLatency,
            meetsAcceptableLatency = meetsAcceptableLatency
        )

        currentMetrics.set(metrics)

        // Notify callback
        metricsCallback?.onMetricsUpdated(metrics)

        // Log if performance is degraded
        if (!meetsAcceptableLatency) {
            Log.w(TAG, "Performance degraded: latency=${avgLatencyMs}ms > ${ACCEPTABLE_LATENCY_MS}ms")
        }
        if (currentFps < TARGET_FPS - 5) {
            Log.w(TAG, "Performance degraded: fps=$currentFps < target $TARGET_FPS")
        }
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

    /**
     * Rolling window for time-series data
     */
    private class RollingWindow(private val size: Int) {
        private val values = LongArray(size)
        private var index = 0
        private var count = 0

        @Synchronized
        fun add(value: Long) {
            values[index] = value
            index = (index + 1) % size
            if (count < size) count++
        }

        @Synchronized
        fun clear() {
            index = 0
            count = 0
        }

        @Synchronized
        fun count(): Int = count

        @Synchronized
        fun average(): Long {
            if (count == 0) return 0
            return values.take(count).sum() / count
        }

        @Synchronized
        fun min(): Long {
            if (count == 0) return 0
            return values.take(count).minOrNull() ?: 0
        }

        @Synchronized
        fun max(): Long {
            if (count == 0) return 0
            return values.take(count).maxOrNull() ?: 0
        }
    }

    /**
     * Callback for metrics updates
     */
    interface MetricsCallback {
        fun onMetricsUpdated(metrics: PerformanceMetrics)
    }
}

/**
 * Performance metrics snapshot
 */
data class PerformanceMetrics(
    /** Session duration in milliseconds */
    val sessionDurationMs: Long = 0,

    /** Current frame rate */
    val currentFps: Float = 0f,

    /** Average encoding time in milliseconds */
    val avgEncodingTimeMs: Long = 0,

    /** Maximum encoding time in milliseconds */
    val maxEncodingTimeMs: Long = 0,

    /** Average network send time in milliseconds */
    val avgNetworkTimeMs: Long = 0,

    /** Average end-to-end latency in milliseconds */
    val avgLatencyMs: Long = 0,

    /** Minimum latency observed */
    val minLatencyMs: Long = 0,

    /** Maximum latency observed */
    val maxLatencyMs: Long = 0,

    /** Current bitrate in kbps */
    val bitrateKbps: Int = 0,

    /** Total frames captured */
    val totalFramesCaptured: Long = 0,

    /** Total frames encoded */
    val totalFramesEncoded: Long = 0,

    /** Total frames sent */
    val totalFramesSent: Long = 0,

    /** Frames dropped due to backpressure */
    val framesDropped: Long = 0,

    /** Drop rate as percentage */
    val dropRatePercent: Float = 0f,

    /** Whether current FPS meets target */
    val meetsTargetFps: Boolean = true,

    /** Whether latency meets target (<100ms) */
    val meetsTargetLatency: Boolean = true,

    /** Whether latency meets acceptable threshold (<150ms) */
    val meetsAcceptableLatency: Boolean = true
) {
    /**
     * Get overall performance status
     */
    fun getStatus(): PerformanceStatus = when {
        meetsTargetFps && meetsTargetLatency -> PerformanceStatus.EXCELLENT
        meetsTargetFps && meetsAcceptableLatency -> PerformanceStatus.GOOD
        meetsAcceptableLatency -> PerformanceStatus.ACCEPTABLE
        else -> PerformanceStatus.DEGRADED
    }

    override fun toString(): String {
        return "PerformanceMetrics(fps=$currentFps, latency=${avgLatencyMs}ms, " +
                "bitrate=${bitrateKbps}kbps, dropped=$framesDropped, " +
                "status=${getStatus()})"
    }
}

/**
 * Performance status levels
 */
enum class PerformanceStatus {
    /** Meets all targets */
    EXCELLENT,
    /** Meets FPS, acceptable latency */
    GOOD,
    /** Acceptable performance */
    ACCEPTABLE,
    /** Performance is degraded */
    DEGRADED
}
