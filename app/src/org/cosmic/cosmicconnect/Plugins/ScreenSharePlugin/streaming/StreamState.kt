/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming

/**
 * Represents the lifecycle states of a CSMR video streaming session.
 */
sealed class StreamState {
    data object Idle : StreamState()
    data class WaitingForConnection(val tcpPort: Int) : StreamState()
    data class Receiving(val width: Int, val height: Int, val fps: Int, val frameCount: Long = 0) : StreamState()
    data class Stopped(val reason: String) : StreamState()
    data class Error(val error: Throwable) : StreamState()
}
