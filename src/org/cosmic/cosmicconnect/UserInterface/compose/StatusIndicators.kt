/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp

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

/**
 * Transfer progress indicator for file transfers.
 *
 * Shows progress bar with file name, size, and percentage.
 *
 * @param fileName Name of file being transferred
 * @param progress Transfer progress (0.0 to 1.0)
 * @param totalBytes Total file size in bytes
 * @param transferredBytes Bytes transferred so far
 * @param modifier Modifier for the indicator
 */
@Composable
fun TransferProgressIndicator(
  fileName: String,
  progress: Float,
  totalBytes: Long? = null,
  transferredBytes: Long? = null,
  modifier: Modifier = Modifier
) {
  val clampedProgress = progress.coerceIn(0f, 1f)
  val percentage = (clampedProgress * 100).toInt()

  val sizeText = if (totalBytes != null && transferredBytes != null) {
    "${formatBytes(transferredBytes)} / ${formatBytes(totalBytes)}"
  } else {
    "$percentage%"
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .semantics {
        contentDescription = "Transferring $fileName, $percentage percent complete"
      }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = fileName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier.weight(1f)
      )

      Spacer(modifier = Modifier.width(Spacing.small))

      Text(
        text = sizeText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.size(Spacing.extraSmall))

    LinearProgressIndicator(
      progress = clampedProgress,
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
  }
}

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

/**
 * Loading indicator variants.
 */
enum class LoadingStyle {
  Circular,
  Linear,
  CircularWithLabel
}

/**
 * Loading indicator with multiple style variants.
 *
 * Shows loading state with progress or indeterminate animation.
 *
 * @param style Loading indicator style
 * @param progress Optional progress (0.0 to 1.0, null for indeterminate)
 * @param label Optional label text (for CircularWithLabel style)
 * @param color Optional custom color
 * @param modifier Modifier for the indicator
 */
@Composable
fun LoadingIndicator(
  style: LoadingStyle = LoadingStyle.Circular,
  progress: Float? = null,
  label: String? = null,
  color: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier
) {
  val contentDesc = buildString {
    append("Loading")
    progress?.let {
      val percentage = (it.coerceIn(0f, 1f) * 100).toInt()
      append(", $percentage percent")
    }
    label?.let { append(", $it") }
  }

  when (style) {
    LoadingStyle.Circular -> {
      if (progress != null) {
        CircularProgressIndicator(
          progress = progress.coerceIn(0f, 1f),
          modifier = modifier
            .size(Dimensions.Icon.large)
            .semantics { contentDescription = contentDesc },
          color = color,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      } else {
        CircularProgressIndicator(
          modifier = modifier
            .size(Dimensions.Icon.large)
            .semantics { contentDescription = contentDesc },
          color = color
        )
      }
    }

    LoadingStyle.Linear -> {
      if (progress != null) {
        LinearProgressIndicator(
          progress = progress.coerceIn(0f, 1f),
          modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc },
          color = color,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      } else {
        LinearProgressIndicator(
          modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = contentDesc },
          color = color
        )
      }
    }

    LoadingStyle.CircularWithLabel -> {
      Column(
        modifier = modifier.semantics { contentDescription = contentDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
      ) {
        if (progress != null) {
          CircularProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.size(Dimensions.Icon.large),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        } else {
          CircularProgressIndicator(
            modifier = Modifier.size(Dimensions.Icon.large),
            color = color
          )
        }

        label?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

/**
 * Format bytes to human-readable string.
 */
private fun formatBytes(bytes: Long): String {
  return when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
  }
}

/**
 * Preview composables for development
 */
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

@Preview(showBackground = true)
@Composable
private fun TransferProgressIndicatorPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Surface {
      Column(
        modifier = Modifier.padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
      ) {
        TransferProgressIndicator(
          fileName = "vacation_photo.jpg",
          progress = 0.35f,
          totalBytes = 5242880,
          transferredBytes = 1835008
        )

        TransferProgressIndicator(
          fileName = "document.pdf",
          progress = 0.75f,
          totalBytes = 1048576,
          transferredBytes = 786432
        )

        TransferProgressIndicator(
          fileName = "very_long_file_name_that_should_be_truncated.mp4",
          progress = 0.10f,
          totalBytes = 104857600,
          transferredBytes = 10485760
        )

        // Without size info
        TransferProgressIndicator(
          fileName = "music.mp3",
          progress = 0.50f
        )
      }
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

@Preview(showBackground = true)
@Composable
private fun LoadingIndicatorPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Surface {
      Column(
        modifier = Modifier.padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
      ) {
        // Circular indeterminate
        Box(modifier = Modifier.size(48.dp)) {
          LoadingIndicator(
            style = LoadingStyle.Circular
          )
        }

        // Circular with progress
        Box(modifier = Modifier.size(48.dp)) {
          LoadingIndicator(
            style = LoadingStyle.Circular,
            progress = 0.65f
          )
        }

        // Linear indeterminate
        LoadingIndicator(
          style = LoadingStyle.Linear
        )

        // Linear with progress
        LoadingIndicator(
          style = LoadingStyle.Linear,
          progress = 0.40f
        )

        // Circular with label (indeterminate)
        LoadingIndicator(
          style = LoadingStyle.CircularWithLabel,
          label = "Loading devices..."
        )

        // Circular with label and progress
        LoadingIndicator(
          style = LoadingStyle.CircularWithLabel,
          progress = 0.80f,
          label = "Syncing..."
        )
      }
    }
  }
}
