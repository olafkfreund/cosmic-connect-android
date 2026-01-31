/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.CameraPlugin

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * CameraStreamClient - Network streaming for camera frames
 *
 * Transmits encoded H.264 frames to COSMIC Desktop over the existing
 * COSMIC Connect TLS link. Handles frame framing, SPS/PPS periodic
 * transmission, flow control, and bandwidth monitoring.
 *
 * ## Features
 *
 * - Frame transmission via existing device link
 * - COSMIC Connect protocol packet format
 * - Periodic SPS/PPS resend for decoder recovery
 * - Backpressure to prevent queue buildup
 * - Bandwidth monitoring
 * - Connection loss handling
 *
 * ## Usage
 *
 * ```kotlin
 * val client = CameraStreamClient(device, object : CameraStreamClient.StreamCallback {
 *     override fun onStreamStarted() { /* UI update */ }
 *     override fun onStreamStopped() { /* UI update */ }
 *     override fun onStreamError(error: Throwable) { /* Handle error */ }
 *     override fun onBandwidthUpdate(kbps: Int) { /* Display bandwidth */ }
 * })
 *
 * // Start streaming
 * client.start()
 *
 * // Send SPS/PPS first
 * client.sendSpsPps(spsData, ppsData)
 *
 * // Send frames from encoder
 * client.sendFrame(frameData, FrameType.IFRAME, timestampUs)
 *
 * // Stop streaming
 * client.stop()
 * ```
 *
 * ## Frame Protocol
 *
 * Frames are sent as COSMIC Connect packets with payload:
 * - Packet type: cconnect.camera.frame
 * - Body: frameType, timestampUs, sequenceNumber, size
 * - Payload: H.264 NAL unit data (Annex B format)
 *
 * @param device Device to stream to
 * @param callback Callback for stream events
 */
class CameraStreamClient(
    private val device: Device,
    private val callback: StreamCallback
) {
    companion object {
        private const val TAG = "CameraStreamClient"

        /** Maximum pending frames before dropping P-frames */
        private const val MAX_PENDING_FRAMES = 3

        /** Interval for SPS/PPS resend (in frames) - reduced for better recovery */
        private const val SPS_PPS_RESEND_INTERVAL = 60

        /** Bandwidth monitoring interval in milliseconds */
        private const val BANDWIDTH_MONITOR_INTERVAL_MS = 500L

        /** Target bandwidth utilization for congestion detection */
        private const val CONGESTION_THRESHOLD = 0.8

        /** Minimum bitrate for adaptive control (kbps) */
        private const val MIN_ADAPTIVE_BITRATE_KBPS = 500

        /** Maximum bitrate for adaptive control (kbps) */
        private const val MAX_ADAPTIVE_BITRATE_KBPS = 8000

        /** Bitrate adjustment step (percentage) */
        private const val BITRATE_ADJUSTMENT_PERCENT = 20

        /** Frames to wait before increasing bitrate */
        private const val FRAMES_BEFORE_BITRATE_INCREASE = 90
    }

    // ========================================================================
    // Callback Interface
    // ========================================================================

    /**
     * Callback for stream events
     */
    interface StreamCallback {
        /** Called when streaming starts */
        fun onStreamStarted()

        /** Called when streaming stops */
        fun onStreamStopped()

        /** Called on stream error */
        fun onStreamError(error: Throwable)

        /** Called with bandwidth update (kbps) */
        fun onBandwidthUpdate(kbps: Int)

        /** Called when congestion detected - should reduce bitrate */
        fun onCongestionDetected()
    }

    // ========================================================================
    // State
    // ========================================================================

    /** Whether streaming is active */
    @Volatile
    private var isStreaming: Boolean = false

    /** Cached SPS data */
    private var spsData: ByteArray? = null

    /** Cached PPS data */
    private var ppsData: ByteArray? = null

    /** Frames sent since last SPS/PPS */
    private var framesSinceSpsPps: Int = 0

    /** Frame sequence number */
    private val sequenceNumber = AtomicLong(0)

    /** Number of frames pending send */
    private val pendingFrames = AtomicInteger(0)

    /** Total bytes sent for bandwidth monitoring */
    private val totalBytesSent = AtomicLong(0)

    /** Bytes sent at last bandwidth check */
    private var lastBytesSent: Long = 0

    /** Time of last bandwidth check */
    private var lastBandwidthCheckTime: Long = 0

    /** Target bitrate in kbps (for congestion detection) */
    private var targetBitrateKbps: Int = 2000

    /** Adaptive bitrate enabled */
    @Volatile
    private var adaptiveBitrateEnabled: Boolean = true

    /** Frames since last congestion event (for adaptive bitrate) */
    private var framesSinceCongestion: Int = 0

    /** Performance monitor for network metrics */
    private var performanceMonitor: CameraPerformanceMonitor? = null

    /** Handler for main thread callbacks */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Handler for bandwidth monitoring */
    private var bandwidthMonitorRunnable: Runnable? = null

    // ========================================================================
    // Statistics
    // ========================================================================

    /** Total frames sent */
    private val totalFramesSent = AtomicLong(0)

    /** Total keyframes sent */
    private val totalKeyframesSent = AtomicLong(0)

    /** Frames dropped due to backpressure */
    private val framesDropped = AtomicLong(0)

    /** Send errors */
    private val sendErrors = AtomicLong(0)

    // ========================================================================
    // Lifecycle
    // ========================================================================

    /**
     * Start streaming
     *
     * Initializes state and begins accepting frames for transmission.
     */
    fun start() {
        if (isStreaming) {
            Log.w(TAG, "Already streaming")
            return
        }

        Log.i(TAG, "Starting camera stream to ${device.name}")

        // Reset state
        isStreaming = true
        sequenceNumber.set(0)
        pendingFrames.set(0)
        totalBytesSent.set(0)
        framesSinceSpsPps = 0

        // Reset statistics
        totalFramesSent.set(0)
        totalKeyframesSent.set(0)
        framesDropped.set(0)
        sendErrors.set(0)

        // Start bandwidth monitoring
        startBandwidthMonitoring()

        // Notify callback
        mainHandler.post { callback.onStreamStarted() }
    }

    /**
     * Stop streaming
     *
     * Stops accepting frames and cleans up resources.
     */
    fun stop() {
        if (!isStreaming) {
            Log.d(TAG, "Not streaming")
            return
        }

        Log.i(TAG, "Stopping camera stream")

        isStreaming = false

        // Stop bandwidth monitoring
        stopBandwidthMonitoring()

        // Log statistics
        Log.i(TAG, "Stream stats: sent=${totalFramesSent.get()}, " +
                "keyframes=${totalKeyframesSent.get()}, " +
                "dropped=${framesDropped.get()}, " +
                "errors=${sendErrors.get()}")

        // Notify callback
        mainHandler.post { callback.onStreamStopped() }
    }

    /**
     * Check if streaming is active
     */
    fun isStreaming(): Boolean = isStreaming

    /**
     * Set target bitrate for congestion detection
     */
    fun setTargetBitrate(bitrateKbps: Int) {
        targetBitrateKbps = bitrateKbps
    }

    /**
     * Enable or disable adaptive bitrate (Issue #110)
     *
     * When enabled, the client will automatically adjust bitrate
     * based on network conditions to maintain low latency.
     */
    fun setAdaptiveBitrateEnabled(enabled: Boolean) {
        adaptiveBitrateEnabled = enabled
        Log.d(TAG, "Adaptive bitrate: $enabled")
    }

    /**
     * Set performance monitor for network metrics
     */
    fun setPerformanceMonitor(monitor: CameraPerformanceMonitor?) {
        performanceMonitor = monitor
    }

    // ========================================================================
    // Frame Transmission
    // ========================================================================

    /**
     * Send SPS/PPS decoder configuration
     *
     * Should be called before sending any frames, and the data will be
     * cached for periodic resend.
     *
     * @param sps SPS NAL unit (with start code)
     * @param pps PPS NAL unit (with start code)
     */
    fun sendSpsPps(sps: ByteArray, pps: ByteArray) {
        if (!isStreaming) return

        Log.d(TAG, "Sending SPS/PPS (${sps.size + pps.size} bytes)")

        // Cache for periodic resend
        spsData = sps.copyOf()
        ppsData = pps.copyOf()

        // Combine SPS and PPS into single packet
        val combined = ByteArray(sps.size + pps.size)
        System.arraycopy(sps, 0, combined, 0, sps.size)
        System.arraycopy(pps, 0, combined, sps.size, pps.size)

        // Send as config packet
        sendFramePacket(combined, FrameType.SPS_PPS, 0)
        framesSinceSpsPps = 0
    }

    /**
     * Send encoded frame
     *
     * Transmits an encoded H.264 frame to the remote device.
     * Implements backpressure by dropping P-frames when queue is full.
     *
     * @param data Encoded frame data (Annex B format)
     * @param frameType Type of frame (I-frame or P-frame)
     * @param timestampUs Presentation timestamp in microseconds
     */
    fun sendFrame(data: ByteArray, frameType: FrameType, timestampUs: Long) {
        if (!isStreaming) return

        // Backpressure: drop P-frames if too many pending
        if (pendingFrames.get() >= MAX_PENDING_FRAMES) {
            if (frameType != FrameType.IFRAME) {
                framesDropped.incrementAndGet()
                performanceMonitor?.onFrameDropped()
                Log.v(TAG, "Dropping P-frame due to backpressure")
                return
            }
            // Never drop I-frames, but log warning
            Log.w(TAG, "Queue full but sending I-frame anyway")
        }

        // Periodically resend SPS/PPS for decoder recovery
        if (framesSinceSpsPps >= SPS_PPS_RESEND_INTERVAL) {
            val sps = spsData
            val pps = ppsData
            if (sps != null && pps != null) {
                Log.d(TAG, "Periodic SPS/PPS resend")
                val combined = ByteArray(sps.size + pps.size)
                System.arraycopy(sps, 0, combined, 0, sps.size)
                System.arraycopy(pps, 0, combined, sps.size, pps.size)
                sendFramePacket(combined, FrameType.SPS_PPS, timestampUs)
                framesSinceSpsPps = 0
            }
        }

        // Send frame
        sendFramePacket(data, frameType, timestampUs)
        framesSinceSpsPps++

        // Update statistics
        totalFramesSent.incrementAndGet()
        if (frameType == FrameType.IFRAME) {
            totalKeyframesSent.incrementAndGet()
        }
    }

    /**
     * Send frame packet via device link
     *
     * Optimized for low-latency streaming (Issue #110):
     * - Tracks send timing for latency calculation
     * - Updates performance metrics
     * - Handles adaptive bitrate recovery
     */
    private fun sendFramePacket(data: ByteArray, frameType: FrameType, timestampUs: Long) {
        val seq = sequenceNumber.getAndIncrement()
        val sendStartTime = System.currentTimeMillis()

        // Create frame packet
        val packet = CameraPacketsFFI.createFramePacket(
            frameType = frameType,
            timestampUs = timestampUs,
            sequenceNumber = seq,
            payloadSize = data.size.toLong()
        )

        // Convert to legacy packet and attach payload
        val legacyPacket = packet.toLegacyPacket()
        legacyPacket.payload = NetworkPacket.Payload(data)

        // Track pending
        pendingFrames.incrementAndGet()

        // Send via device link
        device.sendPacket(legacyPacket, object : Device.SendPacketStatusCallback() {
            override fun onSuccess() {
                val sendTimeMs = System.currentTimeMillis() - sendStartTime
                pendingFrames.decrementAndGet()
                totalBytesSent.addAndGet(data.size.toLong())

                // Track performance metrics
                performanceMonitor?.onFrameSent(seq, sendTimeMs, null)
                performanceMonitor?.onBytesSent(data.size.toLong())

                // Track successful frames for adaptive bitrate
                framesSinceCongestion++

                // Consider increasing bitrate after stable period
                if (adaptiveBitrateEnabled &&
                    framesSinceCongestion >= FRAMES_BEFORE_BITRATE_INCREASE) {
                    maybeIncreaseBitrate()
                }
            }

            override fun onFailure(e: Throwable) {
                pendingFrames.decrementAndGet()
                sendErrors.incrementAndGet()
                Log.e(TAG, "Failed to send frame", e)

                // Reset congestion counter
                framesSinceCongestion = 0

                // Check for connection loss
                if (isConnectionError(e)) {
                    handleConnectionLoss(e)
                }
            }
        })
    }

    /**
     * Consider increasing bitrate after stable period
     */
    private fun maybeIncreaseBitrate() {
        if (!adaptiveBitrateEnabled) return

        val currentPending = pendingFrames.get()
        if (currentPending > 1) {
            // Still have pending frames, don't increase
            return
        }

        val newBitrate = (targetBitrateKbps * (100 + BITRATE_ADJUSTMENT_PERCENT) / 100)
            .coerceAtMost(MAX_ADAPTIVE_BITRATE_KBPS)

        if (newBitrate > targetBitrateKbps) {
            Log.d(TAG, "Adaptive bitrate increase: ${targetBitrateKbps} -> ${newBitrate} kbps")
            targetBitrateKbps = newBitrate
            framesSinceCongestion = 0
            // Note: Actual encoder bitrate change is handled by CameraPlugin
        }
    }

    /**
     * Check if exception indicates connection loss
     */
    private fun isConnectionError(e: Throwable): Boolean {
        return e is java.io.IOException ||
                e is java.net.SocketException ||
                e.cause is java.io.IOException
    }

    /**
     * Handle connection loss
     */
    private fun handleConnectionLoss(error: Throwable) {
        Log.e(TAG, "Connection lost: ${error.message}")

        // Stop streaming
        isStreaming = false
        stopBandwidthMonitoring()

        // Notify callback
        mainHandler.post { callback.onStreamError(error) }
    }

    // ========================================================================
    // Bandwidth Monitoring
    // ========================================================================

    /**
     * Start bandwidth monitoring
     */
    private fun startBandwidthMonitoring() {
        lastBytesSent = 0
        lastBandwidthCheckTime = System.currentTimeMillis()

        bandwidthMonitorRunnable = object : Runnable {
            override fun run() {
                if (isStreaming) {
                    checkBandwidth()
                    mainHandler.postDelayed(this, BANDWIDTH_MONITOR_INTERVAL_MS)
                }
            }
        }

        mainHandler.postDelayed(bandwidthMonitorRunnable!!, BANDWIDTH_MONITOR_INTERVAL_MS)
    }

    /**
     * Stop bandwidth monitoring
     */
    private fun stopBandwidthMonitoring() {
        bandwidthMonitorRunnable?.let { mainHandler.removeCallbacks(it) }
        bandwidthMonitorRunnable = null
    }

    /**
     * Check current bandwidth and detect congestion
     *
     * Optimized for adaptive bitrate control (Issue #110):
     * - More frequent checks for faster response
     * - Automatic bitrate reduction on congestion
     * - Gradual recovery after stable period
     */
    private fun checkBandwidth() {
        val currentBytes = totalBytesSent.get()
        val currentTime = System.currentTimeMillis()

        val bytesSent = currentBytes - lastBytesSent
        val elapsedMs = currentTime - lastBandwidthCheckTime

        if (elapsedMs > 0) {
            // Calculate bandwidth in kbps
            val kbps = ((bytesSent * 8) / elapsedMs).toInt()

            // Notify callback
            callback.onBandwidthUpdate(kbps)

            // Check for congestion based on pending frame count and bandwidth
            val pendingCount = pendingFrames.get()
            val isCongested = pendingCount >= MAX_PENDING_FRAMES - 1 ||
                    (kbps > 0 && kbps < targetBitrateKbps * CONGESTION_THRESHOLD)

            if (isCongested) {
                Log.w(TAG, "Congestion detected: ${kbps}kbps, pending=$pendingCount")

                // Reset recovery counter
                framesSinceCongestion = 0

                // Adaptive bitrate reduction
                if (adaptiveBitrateEnabled) {
                    val newBitrate = (targetBitrateKbps * (100 - BITRATE_ADJUSTMENT_PERCENT) / 100)
                        .coerceAtLeast(MIN_ADAPTIVE_BITRATE_KBPS)

                    if (newBitrate < targetBitrateKbps) {
                        Log.d(TAG, "Adaptive bitrate decrease: ${targetBitrateKbps} -> ${newBitrate} kbps")
                        targetBitrateKbps = newBitrate
                    }
                }

                mainHandler.post { callback.onCongestionDetected() }
            }
        }

        lastBytesSent = currentBytes
        lastBandwidthCheckTime = currentTime
    }

    // ========================================================================
    // Statistics
    // ========================================================================

    /**
     * Get streaming statistics
     */
    fun getStats(): StreamStats {
        return StreamStats(
            totalFramesSent = totalFramesSent.get(),
            totalKeyframesSent = totalKeyframesSent.get(),
            framesDropped = framesDropped.get(),
            sendErrors = sendErrors.get(),
            pendingFrames = pendingFrames.get(),
            totalBytesSent = totalBytesSent.get()
        )
    }

    /**
     * Streaming statistics
     */
    data class StreamStats(
        val totalFramesSent: Long,
        val totalKeyframesSent: Long,
        val framesDropped: Long,
        val sendErrors: Long,
        val pendingFrames: Int,
        val totalBytesSent: Long
    )
}
