/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.ui.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicIcons
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.Dimensions
import org.cosmic.cosmicconnect.UserInterface.compose.Spacing

/**
 * Connection status states for devices.
 */
enum class ConnectionStatus {
    Connected,
    Connecting,
    Disconnected,
    Error
}

/**
 * Connection status indicator for devices.
 *
 * Displays visual feedback for device connection state with icon and optional label.
 *
 * @param status Current connection status
 * @param showLabel Whether to show text label alongside icon
 * @param label Optional custom label (defaults to status name)
 * @param modifier Modifier for the indicator
 */
@Composable
fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    showLabel: Boolean = true,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val icon: Int
    val color: Color
    val text: String

    when (status) {
        ConnectionStatus.Connected -> {
            icon = CosmicIcons.Pairing.connected
            color = MaterialTheme.colorScheme.primary
            text = "Connected"
        }
        ConnectionStatus.Connecting -> {
            icon = CosmicIcons.Pairing.wifi  // Placeholder for connecting icon
            color = MaterialTheme.colorScheme.tertiary
            text = "Connecting"
        }
        ConnectionStatus.Disconnected -> {
            icon = CosmicIcons.Pairing.disconnected
            color = MaterialTheme.colorScheme.onSurfaceVariant
            text = "Disconnected"
        }
        ConnectionStatus.Error -> {
            icon = CosmicIcons.Status.error
            color = MaterialTheme.colorScheme.error
            text = "Connection Error"
        }
    }

    Row(
        modifier = modifier.semantics {
            contentDescription = label ?: text
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.standard),
            tint = color
        )

        if (showLabel) {
            Text(
                text = label ?: text,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectionStatusIndicatorPreview() {
    CosmicTheme(
        context = androidx.compose.ui.platform.LocalContext.current
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                ConnectionStatusIndicator(
                    status = ConnectionStatus.Connected,
                    showLabel = true
                )

                ConnectionStatusIndicator(
                    status = ConnectionStatus.Connecting,
                    showLabel = true
                )

                ConnectionStatusIndicator(
                    status = ConnectionStatus.Disconnected,
                    showLabel = true
                )

                ConnectionStatusIndicator(
                    status = ConnectionStatus.Error,
                    showLabel = true
                )

                // Icon only
                ConnectionStatusIndicator(
                    status = ConnectionStatus.Connected,
                    showLabel = false
                )
            }
        }
    }
}
