/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.CameraPlugin

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

/**
 * H264Encoder - Hardware-accelerated H.264 video encoder
 *
 * Wraps Android MediaCodec to provide H.264 encoding with Surface input
 * for efficient camera frame encoding. Uses async callback mode for
 * non-blocking operation.
 *
 * ## Features
 *
 * - Hardware-accelerated H.264 encoding
 * - Surface input for zero-copy from Camera2
 * - Async output processing
 * - SPS/PPS extraction for decoder initialization
 * - Frame type detection (I-frame, P-frame)
 * - Bitrate control
 * - Low-latency mode optimizations (Issue #110)
 *
 * ## Performance Optimizations (Issue #110)
 *
 * - Uses CBR for consistent streaming latency
 * - Enables KEY_LOW_LATENCY on Android 11+
 * - Configures PRIORITY_REALTIME on Android 12+
 * - Optimized I-frame interval for streaming
 * - Prepend SPS/PPS to I-frames for decoder recovery
 *
 * ## Usage
 *
 * ```kotlin
 * val encoder = H264Encoder(
 *     width = 1280,
 *     height = 720,
 *     fps = 30,
 *     bitrateKbps = 2000,
 *     lowLatencyMode = true,
 *     callback = object : H264Encoder.EncoderCallback {
 *         override fun onSpsPpsAvailable(sps: ByteArray, pps: ByteArray) {
 *             // Send decoder config to remote
 *         }
 *         override fun onEncodedFrame(data: ByteArray, frameType: FrameType, timestampUs: Long) {
 *             // Send frame to remote
 *         }
 *         override fun onError(error: Throwable) {
 *             // Handle error
 *         }
 *     }
 * )
 *
 * // Configure encoder
 * encoder.configure()
 *
 * // Get surface for Camera2 output
 * val surface = encoder.getInputSurface()
 *
 * // Start encoding
 * encoder.start()
 *
 * // ... camera frames are encoded automatically ...
 *
 * // Stop encoding
 * encoder.stop()
 *
 * // Release resources
 * encoder.release()
 * ```
 *
 * ## H.264 NAL Unit Format
 *
 * Output frames are in Annex B format with start codes (0x00 0x00 0x00 0x01).
 * SPS and PPS are provided separately for decoder configuration.
 *
 * @param width Video width in pixels
 * @param height Video height in pixels
 * @param fps Target frame rate
 * @param bitrateKbps Target bitrate in kilobits per second
 * @param lowLatencyMode Enable low-latency optimizations (default: true)
 * @param callback Callback for encoded frames and errors
 */
class H264Encoder(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val bitrateKbps: Int,
    private val lowLatencyMode: Boolean = true,
    private val callback: EncoderCallback
) {
    companion object {
        private const val TAG = "H264Encoder"

        /** MIME type for H.264/AVC */
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC

        /** Default I-frame interval in seconds */
        private const val DEFAULT_I_FRAME_INTERVAL = 1

        /** Low-latency I-frame interval for streaming (more frequent keyframes) */
        private const val LOW_LATENCY_I_FRAME_INTERVAL = 2

        /** Maximum bitrate in kbps */
        const val MAX_BITRATE_KBPS = 8000

        /** Minimum bitrate in kbps */
        const val MIN_BITRATE_KBPS = 500

        /** Low latency priority level (Android 12+) */
        private const val PRIORITY_REALTIME = 0

        /** Default encoding quality for low latency */
        private const val LOW_LATENCY_QUALITY = 0
    }

    // ========================================================================
    // Callback Interface
    // ========================================================================

    /**
     * Callback for encoder events
     */
    interface EncoderCallback {
        /**
         * Called when SPS and PPS are available
         *
         * These must be sent to the decoder before any frames.
         * Typically sent as a single combined buffer with start codes.
         *
         * @param sps Sequence Parameter Set NAL unit (with start code)
         * @param pps Picture Parameter Set NAL unit (with start code)
         */
        fun onSpsPpsAvailable(sps: ByteArray, pps: ByteArray)

        /**
         * Called when an encoded frame is available
         *
         * @param data Encoded frame data (Annex B format with start code)
         * @param frameType Type of frame (I-frame or P-frame)
         * @param timestampUs Presentation timestamp in microseconds
         */
        fun onEncodedFrame(data: ByteArray, frameType: FrameType, timestampUs: Long)

        /**
         * Called on encoder error
         *
         * @param error The error that occurred
         */
        fun onError(error: Throwable)
    }

    // ========================================================================
    // State
    // ========================================================================

    /** MediaCodec encoder instance */
    private var mediaCodec: MediaCodec? = null

    /** Input surface for Camera2 output */
    private var inputSurface: Surface? = null

    /** Background thread for encoder callbacks */
    private var encoderThread: HandlerThread? = null

    /** Handler for encoder callbacks */
    private var encoderHandler: Handler? = null

    /** Whether encoder is currently running */
    private var isRunning: Boolean = false

    /** Whether encoder has been configured */
    private var isConfigured: Boolean = false

    /** Frame sequence number */
    private var frameSequence: Long = 0

    /** SPS data (cached for restart) */
    private var spsData: ByteArray? = null

    /** PPS data (cached for restart) */
    private var ppsData: ByteArray? = null

    /** Last frame input timestamp for latency calculation */
    @Volatile
    private var lastInputTimestampUs: Long = 0

    /** Performance monitor for encoding metrics */
    private var performanceMonitor: CameraPerformanceMonitor? = null

    // ========================================================================
    // Configuration
    // ========================================================================

    /**
     * Configure the encoder
     *
     * Must be called before start(). Creates the MediaCodec instance
     * and configures it for H.264 encoding with Surface input.
     *
     * @throws IllegalStateException if already configured
     */
    fun configure() {
        if (isConfigured) {
            throw IllegalStateException("Encoder already configured")
        }

        Log.d(TAG, "Configuring encoder: ${width}x${height}@${fps}fps, ${bitrateKbps}kbps")

        // Start encoder thread
        encoderThread = HandlerThread("H264EncoderThread").apply { start() }
        encoderHandler = Handler(encoderThread!!.looper)

        // Create encoder
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encoder", e)
            callback.onError(e)
            return
        }

        // Configure format
        val format = createMediaFormat()

        try {
            // Set async callback
            mediaCodec?.setCallback(mediaCodecCallback, encoderHandler)

            // Configure encoder
            mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            // Create input surface
            inputSurface = mediaCodec?.createInputSurface()

            isConfigured = true
            Log.i(TAG, "Encoder configured successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure encoder", e)
            callback.onError(e)
            release()
        }
    }

    /**
     * Create MediaFormat for encoder configuration
     *
     * Applies low-latency optimizations when lowLatencyMode is enabled:
     * - Uses CBR for consistent streaming latency
     * - Enables KEY_LOW_LATENCY on Android 11+
     * - Sets PRIORITY_REALTIME on Android 12+
     * - Uses Constrained Baseline profile for better compatibility
     */
    private fun createMediaFormat(): MediaFormat {
        return MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            // Color format for Surface input (zero-copy)
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            // Bitrate
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)

            // Frame rate
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)

            // I-frame interval - use shorter interval for low latency mode
            val iFrameInterval = if (lowLatencyMode) {
                LOW_LATENCY_I_FRAME_INTERVAL
            } else {
                DEFAULT_I_FRAME_INTERVAL
            }
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)

            // Profile: Constrained Baseline for best compatibility with streaming
            setInteger(
                MediaFormat.KEY_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline
            )

            // Level 3.1 supports up to 720p@30fps or 1080p@30fps
            setInteger(
                MediaFormat.KEY_LEVEL,
                MediaCodecInfo.CodecProfileLevel.AVCLevel31
            )

            // Bitrate mode: CBR for streaming consistency (reduces latency jitter)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bitrateMode = if (lowLatencyMode) {
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                } else {
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                }
                setInteger(MediaFormat.KEY_BITRATE_MODE, bitrateMode)
            }

            // Low latency mode optimizations
            if (lowLatencyMode) {
                // Android 11+ (API 30): Enable low latency mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
                }

                // Android 12+ (API 31): Set realtime priority
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setInteger(MediaFormat.KEY_PRIORITY, PRIORITY_REALTIME)
                }

                // Android 10+ (API 29): Max B-frames = 0 for lower latency
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
                }

                Log.d(TAG, "Low-latency encoding enabled: CBR, priority=realtime, no B-frames")
            }

            Log.d(TAG, "Encoder format: ${width}x${height}@${fps}fps, ${bitrateKbps}kbps, " +
                    "iFrame=${iFrameInterval}s, lowLatency=$lowLatencyMode")
        }
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    /**
     * Get the input surface for Camera2 output
     *
     * Camera2 should output to this surface. Frames written to this
     * surface are automatically encoded.
     *
     * @return Input surface for camera frames
     * @throws IllegalStateException if not configured
     */
    fun getInputSurface(): Surface {
        if (!isConfigured || inputSurface == null) {
            throw IllegalStateException("Encoder not configured")
        }
        return inputSurface!!
    }

    /**
     * Start encoding
     *
     * Begins processing frames written to the input surface.
     *
     * @throws IllegalStateException if not configured
     */
    fun start() {
        if (!isConfigured) {
            throw IllegalStateException("Encoder not configured")
        }

        if (isRunning) {
            Log.w(TAG, "Encoder already running")
            return
        }

        Log.d(TAG, "Starting encoder")
        frameSequence = 0

        try {
            mediaCodec?.start()
            isRunning = true
            Log.i(TAG, "Encoder started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start encoder", e)
            callback.onError(e)
        }
    }

    /**
     * Stop encoding
     *
     * Stops processing and signals end of stream.
     */
    fun stop() {
        if (!isRunning) {
            Log.d(TAG, "Encoder not running")
            return
        }

        Log.d(TAG, "Stopping encoder")

        try {
            // Signal end of stream
            mediaCodec?.signalEndOfInputStream()
        } catch (e: Exception) {
            Log.w(TAG, "Error signaling end of stream", e)
        }

        try {
            mediaCodec?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping encoder", e)
        }

        isRunning = false
        Log.i(TAG, "Encoder stopped")
    }

    /**
     * Release encoder resources
     *
     * Must be called when encoder is no longer needed.
     */
    fun release() {
        Log.d(TAG, "Releasing encoder")

        stop()

        inputSurface?.release()
        inputSurface = null

        mediaCodec?.release()
        mediaCodec = null

        encoderThread?.quitSafely()
        try {
            encoderThread?.join()
        } catch (e: InterruptedException) {
            Log.w(TAG, "Encoder thread join interrupted", e)
        }
        encoderThread = null
        encoderHandler = null

        isConfigured = false
        Log.i(TAG, "Encoder released")
    }

    /**
     * Check if encoder is running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Check if encoder is configured
     */
    fun isConfigured(): Boolean = isConfigured

    // ========================================================================
    // MediaCodec Callback
    // ========================================================================

    /**
     * Async callback for MediaCodec output
     */
    private val mediaCodecCallback = object : MediaCodec.Callback() {

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            // Not used with Surface input
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            try {
                processOutputBuffer(codec, index, info)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing output buffer", e)
                callback.onError(e)
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Log.e(TAG, "MediaCodec error: ${e.diagnosticInfo}", e)
            callback.onError(e)
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "Output format changed: $format")
            extractSpsPps(format)
        }
    }

    /**
     * Process output buffer from MediaCodec
     *
     * Optimized for low-latency streaming with performance tracking.
     */
    private fun processOutputBuffer(
        codec: MediaCodec,
        index: Int,
        info: MediaCodec.BufferInfo
    ) {
        val processStartTime = System.nanoTime()

        // Check for end of stream
        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, "End of stream")
            codec.releaseOutputBuffer(index, false)
            return
        }

        // Get output buffer
        val buffer = codec.getOutputBuffer(index)
        if (buffer == null) {
            Log.w(TAG, "Output buffer is null")
            codec.releaseOutputBuffer(index, false)
            return
        }

        // Skip empty buffers
        if (info.size == 0) {
            codec.releaseOutputBuffer(index, false)
            return
        }

        // Determine frame type
        val frameType = getFrameType(info.flags)

        // Handle codec config (SPS/PPS)
        if (frameType == FrameType.SPS_PPS) {
            handleCodecConfig(buffer, info)
            codec.releaseOutputBuffer(index, false)
            return
        }

        // Extract frame data (optimize: direct buffer access)
        val data = ByteArray(info.size)
        buffer.position(info.offset)
        buffer.get(data, 0, info.size)

        // Increment frame sequence
        frameSequence++

        // Calculate encoding time for performance tracking
        val encodingTimeMs = (System.nanoTime() - processStartTime) / 1_000_000
        val isKeyframe = frameType == FrameType.IFRAME

        // Track performance metrics
        performanceMonitor?.onFrameEncoded(
            frameSize = info.size,
            encodingTimeMs = encodingTimeMs,
            isKeyframe = isKeyframe
        )

        // Deliver frame to callback
        callback.onEncodedFrame(data, frameType, info.presentationTimeUs)

        // Release buffer immediately after copying (reduces latency)
        codec.releaseOutputBuffer(index, false)
    }

    /**
     * Handle codec configuration data (SPS/PPS)
     */
    private fun handleCodecConfig(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val data = ByteArray(info.size)
        buffer.position(info.offset)
        buffer.get(data, 0, info.size)

        // Parse SPS and PPS from combined buffer
        parseSpsPps(data)
    }

    /**
     * Extract SPS and PPS from MediaFormat
     */
    private fun extractSpsPps(format: MediaFormat) {
        try {
            val spsBuffer = format.getByteBuffer("csd-0")
            val ppsBuffer = format.getByteBuffer("csd-1")

            if (spsBuffer != null && ppsBuffer != null) {
                spsData = ByteArray(spsBuffer.remaining()).also { spsBuffer.get(it) }
                ppsData = ByteArray(ppsBuffer.remaining()).also { ppsBuffer.get(it) }

                Log.d(TAG, "Extracted SPS (${spsData!!.size} bytes) and PPS (${ppsData!!.size} bytes)")
                callback.onSpsPpsAvailable(spsData!!, ppsData!!)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract SPS/PPS from format", e)
        }
    }

    /**
     * Parse SPS and PPS from combined codec config data
     *
     * H.264 codec config typically contains SPS and PPS concatenated
     * with start codes (0x00 0x00 0x00 0x01).
     */
    private fun parseSpsPps(data: ByteArray) {
        // Find NAL unit boundaries by looking for start codes
        val nalUnits = findNalUnits(data)

        var sps: ByteArray? = null
        var pps: ByteArray? = null

        for (nal in nalUnits) {
            if (nal.isEmpty()) continue

            // NAL unit type is in the lower 5 bits of the first byte after start code
            val nalType = nal[0].toInt() and 0x1F

            when (nalType) {
                7 -> sps = nal // SPS
                8 -> pps = nal // PPS
            }
        }

        if (sps != null && pps != null) {
            // Add start codes back
            spsData = addStartCode(sps)
            ppsData = addStartCode(pps)

            Log.d(TAG, "Parsed SPS (${spsData!!.size} bytes) and PPS (${ppsData!!.size} bytes)")
            callback.onSpsPpsAvailable(spsData!!, ppsData!!)
        }
    }

    /**
     * Find NAL units in H.264 data
     *
     * Scans for start codes (0x00 0x00 0x00 0x01 or 0x00 0x00 0x01)
     * and extracts NAL units between them.
     */
    private fun findNalUnits(data: ByteArray): List<ByteArray> {
        val nalUnits = mutableListOf<ByteArray>()
        var start = -1
        var i = 0

        while (i < data.size - 3) {
            // Check for 4-byte start code
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() &&
                data[i + 2] == 0.toByte() && data[i + 3] == 1.toByte()
            ) {
                if (start >= 0) {
                    nalUnits.add(data.copyOfRange(start, i))
                }
                start = i + 4
                i += 4
            }
            // Check for 3-byte start code
            else if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() &&
                data[i + 2] == 1.toByte()
            ) {
                if (start >= 0) {
                    nalUnits.add(data.copyOfRange(start, i))
                }
                start = i + 3
                i += 3
            } else {
                i++
            }
        }

        // Add last NAL unit
        if (start >= 0 && start < data.size) {
            nalUnits.add(data.copyOfRange(start, data.size))
        }

        return nalUnits
    }

    /**
     * Add 4-byte start code to NAL unit
     */
    private fun addStartCode(nal: ByteArray): ByteArray {
        val result = ByteArray(4 + nal.size)
        result[0] = 0
        result[1] = 0
        result[2] = 0
        result[3] = 1
        System.arraycopy(nal, 0, result, 4, nal.size)
        return result
    }

    /**
     * Determine frame type from MediaCodec buffer flags
     */
    private fun getFrameType(flags: Int): FrameType {
        return when {
            (flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 -> FrameType.SPS_PPS
            (flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0 -> FrameType.IFRAME
            else -> FrameType.PFRAME
        }
    }

    // ========================================================================
    // Dynamic Control
    // ========================================================================

    /**
     * Request an immediate keyframe
     *
     * Useful for stream recovery or when starting to send to a new receiver.
     */
    fun requestKeyFrame() {
        if (!isRunning) return

        try {
            val params = android.os.Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
            }
            mediaCodec?.setParameters(params)
            Log.d(TAG, "Keyframe requested")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request keyframe", e)
        }
    }

    /**
     * Change bitrate dynamically
     *
     * @param newBitrateKbps New bitrate in kilobits per second
     */
    fun setBitrate(newBitrateKbps: Int) {
        if (!isRunning) return

        val clampedBitrate = newBitrateKbps.coerceIn(MIN_BITRATE_KBPS, MAX_BITRATE_KBPS)

        try {
            val params = android.os.Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, clampedBitrate * 1000)
            }
            mediaCodec?.setParameters(params)
            Log.d(TAG, "Bitrate changed to ${clampedBitrate}kbps")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to change bitrate", e)
        }
    }

    /**
     * Get cached SPS data
     *
     * @return SPS NAL unit with start code, or null if not available
     */
    fun getSpsData(): ByteArray? = spsData?.copyOf()

    /**
     * Get cached PPS data
     *
     * @return PPS NAL unit with start code, or null if not available
     */
    fun getPpsData(): ByteArray? = ppsData?.copyOf()

    /**
     * Get current frame sequence number
     */
    fun getFrameSequence(): Long = frameSequence

    /**
     * Set performance monitor for encoding metrics
     *
     * @param monitor Performance monitor instance
     */
    fun setPerformanceMonitor(monitor: CameraPerformanceMonitor?) {
        performanceMonitor = monitor
    }

    /**
     * Record input frame timestamp for latency tracking
     *
     * Called by CameraCaptureService when frame is captured.
     *
     * @param timestampUs Frame presentation timestamp in microseconds
     */
    fun onInputFrame(timestampUs: Long) {
        lastInputTimestampUs = timestampUs
    }
}
