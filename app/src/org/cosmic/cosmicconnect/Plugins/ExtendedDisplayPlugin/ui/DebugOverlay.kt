/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionState

/**
 * Debug overlay composable for displaying real-time performance metrics
 */
@Composable
fun DebugOverlay(
    debugInfo: DebugInfo,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = "FPS: ${debugInfo.fps}",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Latency: ${debugInfo.latency}ms",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Bitrate: ${formatBitrate(debugInfo.bitrate)}",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Status: ${connectionState.name}",
                color = getConnectionColor(connectionState),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Format bitrate from bps to human-readable string
 */
private fun formatBitrate(bitrate: Long): String {
    return when {
        bitrate >= 1_000_000 -> "${bitrate / 1_000_000}Mbps"
        bitrate >= 1_000 -> "${bitrate / 1_000}Kbps"
        else -> "${bitrate}bps"
    }
}

/**
 * Get color based on connection state
 */
private fun getConnectionColor(state: ConnectionState): Color {
    return when (state) {
        ConnectionState.CONNECTED -> Color.Green
        ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> Color.Yellow
        ConnectionState.DISCONNECTED, ConnectionState.CLOSED -> Color.Gray
        ConnectionState.FAILED, ConnectionState.ERROR -> Color.Red
    }
}

/**
 * Preview composable for debugging overlay
 */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun DebugOverlayPreview() {
    Box(
        modifier = Modifier
            .background(Color.DarkGray)
            .padding(16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        DebugOverlay(
            debugInfo = DebugInfo(
                fps = 60,
                latency = 12,
                bitrate = 5_000_000
            ),
            connectionState = ConnectionState.CONNECTED
        )
    }
}
