/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.ui.components.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.Dimensions
import org.cosmicext.connect.UserInterface.compose.Spacing

/**
 * Sync status states.
 */
enum class SyncStatus {
    Synced,
    Syncing,
    Error,
    Pending
}

/**
 * Sync status indicator for data synchronization.
 *
 * Shows current sync state with icon and optional label.
 *
 * @param status Current sync status
 * @param showLabel Whether to show text label
 * @param label Optional custom label
 * @param modifier Modifier for the indicator
 */
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    showLabel: Boolean = true,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val icon: Int
    val color: Color
    val text: String
    val animated: Boolean

    when (status) {
        SyncStatus.Synced -> {
            icon = CosmicIcons.Pairing.connected
            color = MaterialTheme.colorScheme.primary
            text = "Synced"
            animated = false
        }
        SyncStatus.Syncing -> {
            icon = CosmicIcons.Action.refresh
            color = MaterialTheme.colorScheme.tertiary
            text = "Syncing"
            animated = true
        }
        SyncStatus.Error -> {
            icon = CosmicIcons.Status.error
            color = MaterialTheme.colorScheme.error
            text = "Sync Error"
            animated = false
        }
        SyncStatus.Pending -> {
            icon = CosmicIcons.Status.info
            color = MaterialTheme.colorScheme.onSurfaceVariant
            text = "Pending"
            animated = false
        }
    }

    Row(
        modifier = modifier.semantics {
            contentDescription = label ?: text
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        if (animated) {
            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sync_rotation"
            )

            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .size(Dimensions.Icon.standard)
                    .rotate(rotation),
                tint = color
            )
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(Dimensions.Icon.standard),
                tint = color
            )
        }

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
private fun SyncStatusIndicatorPreview() {
    CosmicTheme(
        context = androidx.compose.ui.platform.LocalContext.current
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                SyncStatusIndicator(
                    status = SyncStatus.Synced,
                    showLabel = true
                )

                SyncStatusIndicator(
                    status = SyncStatus.Syncing,
                    showLabel = true
                )

                SyncStatusIndicator(
                    status = SyncStatus.Error,
                    showLabel = true
                )

                SyncStatusIndicator(
                    status = SyncStatus.Pending,
                    showLabel = true
                )

                // Icon only
                SyncStatusIndicator(
                    status = SyncStatus.Syncing,
                    showLabel = false
                )
            }
        }
    }
}
