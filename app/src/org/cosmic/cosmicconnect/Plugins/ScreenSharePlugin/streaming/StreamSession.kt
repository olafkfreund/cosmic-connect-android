/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming

import android.util.Log
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.decoder.DecoderCallback
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.decoder.VideoDecoder
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orchestrates a CSMR video streaming session:
 * 1. Opens a TCP ServerSocket on a random port
 * 2. Waits for the desktop to connect
 * 3. Reads CSMR frames and feeds them to VideoDecoder
 * 4. Manages lifecycle via StateFlow<StreamState>
 */
class StreamSession(
    val width: Int,
    val height: Int,
    val fps: Int,
    val codec: String,
) {
    companion object {
        private const val TAG = "StreamSession"
    }

    private val _state = MutableStateFlow<StreamState>(StreamState.Idle)
    val state: StateFlow<StreamState> = _state.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var decoder: VideoDecoder? = null
    private var receiver: CsmrStreamReceiver? = null
    private val stopped = AtomicBoolean(false)

    /** The TCP port the desktop should connect to. Valid after prepare(). */
    val tcpPort: Int
        get() = serverSocket?.localPort ?: 0

    /**
     * Prepares the session by opening a ServerSocket.
     * Call this before sending the "screenshare.ready" packet.
     */
    fun prepare() {
        check(serverSocket == null) { "Session already prepared" }
        serverSocket = ServerSocket(0).also { ss ->
            ss.reuseAddress = true
            ss.soTimeout = 30_000 // 30s timeout for accept()
            Log.i(TAG, "TCP server listening on port ${ss.localPort}")
            _state.value = StreamState.WaitingForConnection(ss.localPort)
        }
    }

    /**
     * Accepts a connection and starts streaming frames to the given Surface.
     * This is a blocking call that runs until stop() is called or the stream ends.
     * Should be called from Dispatchers.IO.
     */
    suspend fun acceptAndStream(surface: Surface) = withContext(Dispatchers.IO) {
        val ss = serverSocket ?: throw IllegalStateException("Call prepare() first")

        try {
            Log.i(TAG, "Waiting for desktop connection on port ${ss.localPort}...")
            val socket: Socket
            try {
                socket = ss.accept()
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timed out waiting for desktop connection", e)
                _state.value = StreamState.Error(e)
                return@withContext
            }
            clientSocket = socket
            socket.soTimeout = 10_000 // 10s read timeout
            Log.i(TAG, "Desktop connected from ${socket.remoteSocketAddress}")

            val videoDecoder = VideoDecoder(surface, width, height, object : DecoderCallback {
                override fun onFrameDecoded(presentationTimeUs: Long) {
                    val current = _state.value
                    if (current is StreamState.Receiving) {
                        _state.value = current.copy(frameCount = current.frameCount + 1)
                    }
                }

                override fun onFormatChanged(width: Int, height: Int) {
                    Log.i(TAG, "Decoder format changed: ${width}x${height}")
                }

                override fun onDecoderError(error: Exception) {
                    Log.e(TAG, "Decoder error", error)
                }
            })
            decoder = videoDecoder
            videoDecoder.start()

            _state.value = StreamState.Receiving(width, height, fps, 0)

            val streamReceiver = CsmrStreamReceiver(socket.getInputStream())
            receiver = streamReceiver

            var consecutiveTimeouts = 0
            val maxConsecutiveTimeouts = 3

            while (isActive && !stopped.get()) {
                val frame = streamReceiver.readFrame()
                if (frame == null) {
                    // null can mean EOF or socket timeout
                    consecutiveTimeouts++
                    if (consecutiveTimeouts >= maxConsecutiveTimeouts) {
                        Log.w(TAG, "Too many consecutive read timeouts ($maxConsecutiveTimeouts), stopping")
                        break
                    }
                    Log.w(TAG, "Read timeout ($consecutiveTimeouts/$maxConsecutiveTimeouts), continuing")
                    continue
                }
                consecutiveTimeouts = 0

                when (frame.type) {
                    CsmrFrame.TYPE_VIDEO -> {
                        // Skip empty payload frames
                        if (frame.payload.isEmpty()) {
                            Log.w(TAG, "Skipping video frame with empty payload")
                            continue
                        }
                        // timestamp from wire is in nanoseconds, MediaCodec expects microseconds
                        try {
                            videoDecoder.decodeFrame(frame.payload, frame.timestampNs / 1000)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding frame", e)
                            _state.value = StreamState.Error(e)
                            break
                        }
                    }
                    CsmrFrame.TYPE_END_OF_STREAM -> {
                        Log.i(TAG, "End of stream received")
                        break
                    }
                    else -> {
                        Log.d(TAG, "Ignoring CSMR frame type: 0x${String.format("%02x", frame.type)}")
                    }
                }
            }

            _state.value = StreamState.Stopped("Stream ended")
            Log.i(TAG, "Streaming finished")

        } catch (e: Exception) {
            if (!stopped.get()) {
                Log.e(TAG, "Stream error", e)
                _state.value = StreamState.Error(e)
            } else {
                _state.value = StreamState.Stopped("Stopped by user")
            }
        } finally {
            cleanup()
        }
    }

    /**
     * Stops the streaming session.
     */
    fun stop() {
        if (stopped.getAndSet(true)) return
        Log.i(TAG, "Stopping stream session")
        cleanup()
        _state.value = StreamState.Stopped("Stopped by user")
    }

    private fun cleanup() {
        try { decoder?.stop() } catch (e: Exception) { Log.w(TAG, "Error stopping decoder", e) }
        try { receiver?.close() } catch (e: Exception) { Log.w(TAG, "Error closing receiver", e) }
        try { clientSocket?.close() } catch (e: Exception) { Log.w(TAG, "Error closing client socket", e) }
        try { serverSocket?.close() } catch (e: Exception) { Log.w(TAG, "Error closing server socket", e) }
        decoder = null
        receiver = null
        clientSocket = null
        serverSocket = null
    }
}
