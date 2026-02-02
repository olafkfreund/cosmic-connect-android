/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionMode
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionState
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

/**
 * WebRTCClient - Main WebRTC client for Extended Display
 *
 * Manages the complete WebRTC lifecycle for receiving desktop
 * display streams from COSMIC Desktop.
 *
 * @property context Android application context
 * @property listener Callback listener for WebRTC events
 * @property mode Connection mode (WiFi or USB)
 */
class WebRTCClient(
    private val context: Context,
    private val listener: WebRTCEventListener,
    private val mode: ConnectionMode
) {

    companion object {
        private const val TAG = "WebRTCClient"
        private const val STUN_SERVER = "stun:stun.l.google.com:19302"
        private const val DATA_CHANNEL_LABEL = "extended-display-control"
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_DELAY_MS = 2000L
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var signalingClient: SignalingClient? = null
    private var dataChannel: DataChannel? = null

    private var reconnectAttempts = 0
    private var currentDesktopIp: String? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    init {
        initializePeerConnectionFactory()
    }

    private fun initializePeerConnectionFactory() {
        Log.i(TAG, "Initializing PeerConnectionFactory")

        try {
            eglBase = EglBase.create()

            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
            )

            val options = PeerConnectionFactory.Options()

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(
                    DefaultVideoEncoderFactory(
                        eglBase?.eglBaseContext,
                        true,
                        true
                    )
                )
                .setVideoDecoderFactory(
                    DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
                )
                .createPeerConnectionFactory()

            Log.i(TAG, "PeerConnectionFactory initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PeerConnectionFactory", e)
            listener.onError("Failed to initialize WebRTC", e)
        }
    }

    /**
     * Connect to desktop Extended Display server
     */
    fun connect(desktopIp: String, signalingPort: Int = 9000) {
        Log.i(TAG, "Connecting to desktop: $desktopIp (mode: $mode)")

        currentDesktopIp = desktopIp
        reconnectAttempts = 0
        _connectionState.value = ConnectionState.CONNECTING

        val signalingUrl = SignalingClient.buildSignalingUrl(desktopIp, signalingPort, mode)

        signalingClient = SignalingClient(signalingUrl, mode).apply {
            setListener { message -> handleSignalingMessage(message) }
        }

        signalingClient?.connect()
    }

    /**
     * Disconnect from desktop
     */
    fun disconnect() {
        Log.i(TAG, "Disconnecting from desktop")

        reconnectAttempts = 0
        currentDesktopIp = null

        dataChannel?.close()
        dataChannel?.dispose()
        dataChannel = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        signalingClient?.disconnect()
        signalingClient = null

        _connectionState.value = ConnectionState.DISCONNECTED
        listener.onConnectionStateChanged(ConnectionState.DISCONNECTED)
    }

    /**
     * Send data via data channel
     */
    fun sendData(data: ByteArray, isBinary: Boolean = false): Boolean {
        val channel = dataChannel
        if (channel == null || channel.state() != DataChannel.State.OPEN) {
            Log.w(TAG, "Data channel not open, cannot send data")
            return false
        }

        return try {
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(data),
                isBinary
            )
            channel.send(buffer)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data", e)
            false
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED &&
               peerConnection?.connectionState() == PeerConnection.PeerConnectionState.CONNECTED
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        Log.i(TAG, "Disposing WebRTCClient")

        disconnect()

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        eglBase?.release()
        eglBase = null
    }

    /**
     * Get the EGL context for video rendering
     */
    fun getEglBase(): EglBase? = eglBase

    private fun handleSignalingMessage(message: SignalingMessage) {
        scope.launch {
            when (message) {
                is SignalingMessage.Offer -> handleRemoteOffer(message.sdp)
                is SignalingMessage.Answer -> handleRemoteAnswer(message.sdp)
                is SignalingMessage.IceCandidate -> handleRemoteIceCandidate(message)
            }
        }
    }

    private fun handleRemoteOffer(sdp: String) {
        Log.i(TAG, "Received remote offer")

        if (peerConnection == null) {
            createPeerConnection()
        }

        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sd: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.i(TAG, "Remote description set successfully")
                createAnswer()
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
                listener.onError("Failed to set remote description: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
                listener.onError("Failed to set remote description: $error")
            }
        }, sessionDescription)
    }

    private fun handleRemoteAnswer(sdp: String) {
        Log.w(TAG, "Received unexpected remote answer (desktop should send offers)")

        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sd: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.i(TAG, "Remote answer set successfully")
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote answer: $error")
                listener.onError("Failed to set remote answer: $error")
            }
        }, sessionDescription)
    }

    private fun handleRemoteIceCandidate(message: SignalingMessage.IceCandidate) {
        Log.d(TAG, "Received remote ICE candidate")

        val iceCandidate = IceCandidate(
            message.sdpMid,
            message.sdpMLineIndex ?: 0,
            message.candidate
        )

        peerConnection?.addIceCandidate(iceCandidate)
    }

    private fun createPeerConnection() {
        Log.i(TAG, "Creating peer connection")

        val factory = peerConnectionFactory
        if (factory == null) {
            Log.e(TAG, "PeerConnectionFactory not initialized")
            listener.onError("PeerConnectionFactory not initialized")
            return
        }

        val iceServers = listOf(
            PeerConnection.IceServer.builder(STUN_SERVER).createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }

        peerConnection = factory.createPeerConnection(
            rtcConfig,
            PeerConnectionObserver()
        )

        if (peerConnection == null) {
            Log.e(TAG, "Failed to create peer connection")
            listener.onError("Failed to create peer connection")
            return
        }

        Log.i(TAG, "Peer connection created successfully")
    }

    private fun createAnswer() {
        Log.i(TAG, "Creating SDP answer")

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                if (sessionDescription == null) {
                    Log.e(TAG, "Created null session description")
                    return
                }

                Log.i(TAG, "Answer created successfully")

                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(sd: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.i(TAG, "Local description set successfully")
                        signalingClient?.sendAnswer(sessionDescription.description)
                    }
                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "Failed to set local description: $error")
                        listener.onError("Failed to set local description: $error")
                    }
                }, sessionDescription)
            }

            override fun onSetSuccess() {}

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create answer: $error")
                listener.onError("Failed to create answer: $error")
            }

            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun attemptReconnection() {
        val desktopIp = currentDesktopIp
        if (desktopIp == null) {
            Log.w(TAG, "Cannot reconnect: no desktop IP")
            return
        }

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached")
            _connectionState.value = ConnectionState.FAILED
            listener.onConnectionStateChanged(ConnectionState.FAILED)
            listener.onError("Connection failed after $MAX_RECONNECT_ATTEMPTS attempts")
            return
        }

        reconnectAttempts++
        Log.i(TAG, "Attempting reconnection $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS")

        scope.launch {
            delay(RECONNECT_DELAY_MS * reconnectAttempts)
            disconnect()
            connect(desktopIp)
        }
    }

    private inner class PeerConnectionObserver : PeerConnection.Observer {

        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            if (iceCandidate == null) return

            Log.d(TAG, "Local ICE candidate: ${iceCandidate.sdp}")
            signalingClient?.sendIceCandidate(
                iceCandidate.sdp,
                iceCandidate.sdpMid,
                iceCandidate.sdpMLineIndex
            )
        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
            Log.d(TAG, "ICE candidates removed: ${iceCandidates?.size ?: 0}")
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            Log.i(TAG, "Remote stream added")

            mediaStream?.videoTracks?.firstOrNull()?.let { videoTrack ->
                Log.i(TAG, "Remote video track received")
                listener.onVideoTrackReceived(videoTrack)
            }
        }

        override fun onDataChannel(dc: DataChannel?) {
            if (dc == null) return

            Log.i(TAG, "Data channel opened: ${dc.label()}")
            dataChannel = dc

            dc.registerObserver(object : DataChannel.Observer {
                override fun onBufferedAmountChange(amount: Long) {}

                override fun onStateChange() {
                    Log.d(TAG, "Data channel state: ${dc.state()}")
                    if (dc.state() == DataChannel.State.OPEN) {
                        listener.onDataChannelOpened(dc)
                    }
                }

                override fun onMessage(buffer: DataChannel.Buffer?) {
                    Log.d(TAG, "Data channel message received")
                }
            })
        }

        override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
            Log.i(TAG, "ICE connection state: $newState")

            when (newState) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    Log.i(TAG, "ICE connected")
                    _connectionState.value = ConnectionState.CONNECTED
                    listener.onConnectionStateChanged(ConnectionState.CONNECTED)
                    reconnectAttempts = 0
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    Log.w(TAG, "ICE disconnected")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    listener.onConnectionStateChanged(ConnectionState.DISCONNECTED)
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    Log.e(TAG, "ICE connection failed")
                    _connectionState.value = ConnectionState.FAILED
                    listener.onConnectionStateChanged(ConnectionState.FAILED)
                    attemptReconnection()
                }
                PeerConnection.IceConnectionState.CLOSED -> {
                    Log.i(TAG, "ICE connection closed")
                    _connectionState.value = ConnectionState.CLOSED
                    listener.onConnectionStateChanged(ConnectionState.CLOSED)
                }
                else -> {}
            }
        }

        override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
            Log.i(TAG, "Peer connection state: $newState")
        }

        override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
        override fun onRemoveStream(mediaStream: MediaStream?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
    }
}
