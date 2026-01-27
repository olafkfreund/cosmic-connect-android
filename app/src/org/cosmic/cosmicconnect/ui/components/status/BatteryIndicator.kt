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
 * Battery status indicator with level and charging state.
 *
 * Displays battery level as percentage with appropriate icon and color coding.
 *
 * @param batteryLevel Battery percentage (0-100)
 * @param isCharging Whether device is currently charging
 * @param showPercentage Whether to show percentage text
 * @param modifier Modifier for the indicator
 */
@Composable
fun BatteryStatusIndicator(
    batteryLevel: Int,
    isCharging: Boolean = false,
    showPercentage: Boolean = true,
    modifier: Modifier = Modifier
) {
    val clampedLevel = batteryLevel.coerceIn(0, 100)

    val icon: Int
    val color: Color

    when {
        isCharging -> {
            icon = CosmicIcons.Pairing.connected  // Placeholder for charging icon
            color = MaterialTheme.colorScheme.tertiary
        }
        clampedLevel >= 90 -> {
            icon = CosmicIcons.Pairing.connected  // Placeholder for full battery
            color = MaterialTheme.colorScheme.primary
        }
        clampedLevel >= 50 -> {
            icon = CosmicIcons.Status.info  // Placeholder for normal battery
            color = MaterialTheme.colorScheme.primary
        }
        clampedLevel >= 20 -> {
            icon = CosmicIcons.Status.warning  // Placeholder for low battery
            color = MaterialTheme.colorScheme.tertiary
        }
        else -> {
            icon = CosmicIcons.Status.error  // Alert for critical battery
            color = MaterialTheme.colorScheme.error
        }
    }

    val statusText = buildString {
        append("Battery: $clampedLevel%")
        if (isCharging) append(", charging")
    }

    Row(
        modifier = modifier.semantics {
            contentDescription = statusText
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.standard),
            tint = color
        )

        if (showPercentage) {
            Text(
                text = "$clampedLevel%",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryStatusIndicatorPreview() {
    CosmicTheme(
        context = androidx.compose.ui.platform.LocalContext.current
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium)
            ) {
                BatteryStatusIndicator(
                    batteryLevel = 95,
                    isCharging = false,
                    showPercentage = true
                )

                BatteryStatusIndicator(
                    batteryLevel = 60,
                    isCharging = false,
                    showPercentage = true
                )

                BatteryStatusIndicator(
                    batteryLevel = 25,
                    isCharging = false,
                    showPercentage = true
                )

                BatteryStatusIndicator(
                    batteryLevel = 10,
                    isCharging = false,
                    showPercentage = true
                )

                BatteryStatusIndicator(
                    batteryLevel = 45,
                    isCharging = true,
                    showPercentage = true
                )

                // Icon only
                BatteryStatusIndicator(
                    batteryLevel = 75,
                    isCharging = false,
                    showPercentage = false
                )
            }
        }
    }
}
