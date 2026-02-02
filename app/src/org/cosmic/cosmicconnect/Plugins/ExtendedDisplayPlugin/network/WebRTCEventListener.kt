/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.network

import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionState
import org.webrtc.DataChannel
import org.webrtc.VideoTrack

/**
 * WebRTCEventListener - Callback interface for WebRTC events
 *
 * This interface defines callbacks for key WebRTC connection events in the
 * Extended Display plugin.
 */
interface WebRTCEventListener {

    /**
     * Called when a remote video track is received
     *
     * @param videoTrack The received video track from desktop
     */
    fun onVideoTrackReceived(videoTrack: VideoTrack)

    /**
     * Called when a data channel is successfully opened
     *
     * @param dataChannel The opened data channel for bidirectional messaging
     */
    fun onDataChannelOpened(dataChannel: DataChannel)

    /**
     * Called when the WebRTC connection state changes
     *
     * @param state The new connection state
     */
    fun onConnectionStateChanged(state: ConnectionState)

    /**
     * Called when an error occurs
     *
     * @param error Human-readable error message
     * @param exception Optional exception with additional context
     */
    fun onError(error: String, exception: Throwable? = null)
}
