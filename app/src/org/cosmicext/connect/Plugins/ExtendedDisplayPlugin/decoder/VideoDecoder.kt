/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.decoder

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface

/**
 * Hardware-accelerated H.264 video decoder using MediaCodec.
 *
 * Decodes H.264 encoded frames and renders them directly to a surface
 * with low-latency configuration optimized for real-time streaming.
 *
 * @param surface The output surface for rendering decoded frames
 * @param width Initial video width in pixels
 * @param height Initial video height in pixels
 * @param callback Callback for decoder events
 */
class VideoDecoder(
    private val surface: Surface,
    private val width: Int,
    private val height: Int,
    private val callback: DecoderCallback
) {
    companion object {
        private const val TAG = "VideoDecoder"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC // H.264
        private const val TIMEOUT_US = 10_000L // 10ms timeout for buffer operations
    }

    private var decoder: MediaCodec? = null
    private val bufferInfo = MediaCodec.BufferInfo()
    private var isRunning = false
    private var frameCount = 0L

    /**
     * Starts the video decoder.
     */
    @Synchronized
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Decoder already running")
            return
        }

        try {
            Log.i(TAG, "Starting video decoder (${width}x${height})")

            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                // Low-latency mode (Android 11+)
                setInteger(MediaFormat.KEY_LOW_LATENCY, 1)

                // Realtime priority
                setInteger(MediaFormat.KEY_PRIORITY, 0)

                // Performance hints
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
                setInteger(MediaFormat.KEY_OPERATING_RATE, 60)
            }

            decoder = MediaCodec.createDecoderByType(MIME_TYPE).apply {
                configure(format, surface, null, 0)
                start()
            }

            isRunning = true
            frameCount = 0
            Log.i(TAG, "Decoder started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start decoder", e)
            cleanup()
            callback.onDecoderError(e)
            throw e
        }
    }

    /**
     * Decodes a single H.264 encoded frame.
     *
     * @param encodedData The H.264 encoded frame data
     * @param timestamp Presentation timestamp in microseconds
     * @return true if frame was queued successfully, false otherwise
     */
    fun decodeFrame(encodedData: ByteArray, timestamp: Long): Boolean {
        if (!isRunning) {
            Log.w(TAG, "Cannot decode frame: decoder not running")
            return false
        }

        val currentDecoder = decoder ?: run {
            Log.w(TAG, "Decoder is null")
            return false
        }

        try {
            val inputBufferIndex = currentDecoder.dequeueInputBuffer(TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = currentDecoder.getInputBuffer(inputBufferIndex)
                inputBuffer?.let {
                    it.clear()
                    it.put(encodedData)

                    currentDecoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        encodedData.size,
                        timestamp,
                        0
                    )
                }
            } else {
                Log.d(TAG, "No input buffer available")
                return false
            }

            processOutputBuffers(currentDecoder)
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error decoding frame", e)
            handleDecoderError(e)
            return false
        }
    }

    private fun processOutputBuffers(currentDecoder: MediaCodec) {
        var outputBufferIndex: Int

        do {
            outputBufferIndex = currentDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = currentDecoder.outputFormat
                    val newWidth = format.getInteger(MediaFormat.KEY_WIDTH)
                    val newHeight = format.getInteger(MediaFormat.KEY_HEIGHT)
                    Log.i(TAG, "Output format changed: ${newWidth}x${newHeight}")
                    callback.onFormatChanged(newWidth, newHeight)
                }

                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    break
                }

                else -> {
                    if (outputBufferIndex >= 0) {
                        currentDecoder.releaseOutputBuffer(outputBufferIndex, true)

                        frameCount++
                        callback.onFrameDecoded(bufferInfo.presentationTimeUs)

                        if (frameCount % 100 == 0L) {
                            Log.d(TAG, "Decoded $frameCount frames")
                        }
                    }
                }
            }
        } while (outputBufferIndex >= 0)
    }

    private fun handleDecoderError(error: Exception) {
        Log.e(TAG, "Decoder error, attempting recovery", error)
        callback.onDecoderError(error)

        try {
            stop()
            start()
            Log.i(TAG, "Decoder restarted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart decoder", e)
            cleanup()
        }
    }

    /**
     * Stops the video decoder.
     */
    @Synchronized
    fun stop() {
        if (!isRunning) {
            Log.w(TAG, "Decoder not running")
            return
        }

        Log.i(TAG, "Stopping video decoder (decoded $frameCount frames)")
        isRunning = false
        cleanup()
    }

    private fun cleanup() {
        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        } finally {
            decoder = null
        }
    }

    fun getFrameCount(): Long = frameCount

    fun isRunning(): Boolean = isRunning
}
