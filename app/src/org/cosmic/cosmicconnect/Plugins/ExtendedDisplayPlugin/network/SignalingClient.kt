/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionMode
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionState
import java.util.concurrent.TimeUnit

/**
 * SignalingClient - WebSocket-based signaling for WebRTC
 *
 * This client handles the signaling phase of WebRTC connection establishment
 * by exchanging SDP offers/answers and ICE candidates with the COSMIC Desktop
 * Extended Display server.
 *
 * @property serverUrl WebSocket server URL (ws:// or wss://)
 * @property mode Connection mode (WiFi or USB)
 */
class SignalingClient(
    private val serverUrl: String,
    private val mode: ConnectionMode
) {

    companion object {
        private const val TAG = "SignalingClient"
        private const val WEBSOCKET_TIMEOUT_SECONDS = 30L
        private const val RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_ATTEMPTS = 5

        /**
         * Build signaling server URL for given mode
         */
        fun buildSignalingUrl(
            desktopIp: String,
            port: Int = 9000,
            mode: ConnectionMode
        ): String {
            val host = when (mode) {
                ConnectionMode.WIFI, ConnectionMode.MANUAL -> desktopIp
                ConnectionMode.USB -> ConnectionMode.USB_LOCALHOST
            }
            return "ws://$host:$port/signaling"
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(WEBSOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(WEBSOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WEBSOCKET_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var webSocket: WebSocket? = null
    private var messageListener: ((SignalingMessage) -> Unit)? = null
    private var reconnectAttempts = 0

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Set message listener for incoming signaling messages
     */
    fun setListener(listener: (SignalingMessage) -> Unit) {
        messageListener = listener
    }

    /**
     * Connect to signaling server
     */
    fun connect() {
        if (webSocket != null) {
            Log.w(TAG, "Already connected or connecting")
            return
        }

        Log.i(TAG, "Connecting to signaling server: $serverUrl (mode: $mode)")
        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, WebSocketHandler())
    }

    /**
     * Disconnect from signaling server
     */
    fun disconnect() {
        Log.i(TAG, "Disconnecting from signaling server")
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        reconnectAttempts = 0
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    /**
     * Send SDP answer to server
     */
    fun sendAnswer(sdp: String) {
        sendMessage(SignalingMessage.Answer(sdp))
    }

    /**
     * Send ICE candidate to server
     */
    fun sendIceCandidate(candidate: String, sdpMid: String?, sdpMLineIndex: Int?) {
        sendMessage(SignalingMessage.IceCandidate(candidate, sdpMid, sdpMLineIndex))
    }

    /**
     * Check if currently connected to signaling server
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    private fun sendMessage(message: SignalingMessage) {
        scope.launch {
            try {
                val json = when (message) {
                    is SignalingMessage.Offer -> {
                        moshi.adapter(SignalingMessage.Offer::class.java).toJson(message)
                    }
                    is SignalingMessage.Answer -> {
                        moshi.adapter(SignalingMessage.Answer::class.java).toJson(message)
                    }
                    is SignalingMessage.IceCandidate -> {
                        moshi.adapter(SignalingMessage.IceCandidate::class.java).toJson(message)
                    }
                }

                Log.d(TAG, "Sending message: ${message.type}")
                webSocket?.send(json)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    private fun parseMessage(json: String): SignalingMessage? {
        return try {
            val typeAdapter = moshi.adapter(Map::class.java)
            @Suppress("UNCHECKED_CAST")
            val map = typeAdapter.fromJson(json) as? Map<String, Any>
            val type = map?.get("type") as? String

            when (type) {
                SignalingMessage.TYPE_OFFER -> {
                    moshi.adapter(SignalingMessage.Offer::class.java).fromJson(json)
                }
                SignalingMessage.TYPE_ANSWER -> {
                    moshi.adapter(SignalingMessage.Answer::class.java).fromJson(json)
                }
                SignalingMessage.TYPE_CANDIDATE -> {
                    moshi.adapter(SignalingMessage.IceCandidate::class.java).fromJson(json)
                }
                else -> {
                    Log.w(TAG, "Unknown message type: $type")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message", e)
            null
        }
    }

    private fun handleConnectionFailure() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            Log.w(TAG, "Connection failed, retry attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")

            scope.launch {
                delay(RECONNECT_DELAY_MS * reconnectAttempts)
                webSocket = null
                connect()
            }
        } else {
            Log.e(TAG, "Max reconnection attempts reached, giving up")
            _connectionState.value = ConnectionState.FAILED
            reconnectAttempts = 0
        }
    }

    private inner class WebSocketHandler : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket connected")
            _connectionState.value = ConnectionState.CONNECTED
            reconnectAttempts = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: ${text.take(100)}...")

            parseMessage(text)?.let { message ->
                messageListener?.invoke(message)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closing: $code $reason")
            _connectionState.value = ConnectionState.DISCONNECTING
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closed: $code $reason")
            _connectionState.value = ConnectionState.CLOSED
            this@SignalingClient.webSocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            _connectionState.value = ConnectionState.FAILED
            this@SignalingClient.webSocket = null
            handleConnectionFailure()
        }
    }
}
