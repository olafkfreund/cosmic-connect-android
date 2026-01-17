/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview

/**
 * Device card component for displaying device information in lists.
 *
 * Features:
 * - Device icon based on type
 * - Device name and status
 * - Connection indicator
 * - Battery level (optional)
 * - Clickable with proper touch target
 * - Accessibility support
 *
 * @param deviceName Name of the device
 * @param deviceType Type of device (phone, laptop, desktop, etc.)
 * @param connectionStatus Connection status text
 * @param isConnected Whether the device is currently connected
 * @param batteryLevel Battery level percentage (0-100), null if not available
 * @param onClick Callback when card is clicked
 * @param modifier Modifier for the card
 */
@Composable
fun DeviceCard(
  deviceName: String,
  deviceType: String,
  connectionStatus: String,
  isConnected: Boolean,
  batteryLevel: Int? = null,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .height(Dimensions.ListItem.deviceItemHeight)
      .clickable(
        onClick = onClick,
        onClickLabel = "Open $deviceName details"
      )
      .semantics {
        role = Role.Button
        contentDescription = "$deviceName, $deviceType, $connectionStatus" +
          (batteryLevel?.let { ", battery $it percent" } ?: "")
      },
    shape = CustomShapes.deviceCard,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = Elevation.level1
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(SpecialSpacing.ListItem.contentPadding),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Device icon
      Icon(
        painter = painterResource(getDeviceIcon(deviceType)),
        contentDescription = null, // Decorative, described in card semantics
        modifier = Modifier.size(Dimensions.Icon.device),
        tint = if (isConnected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
      )

      Spacer(modifier = Modifier.width(SpecialSpacing.ListItem.iconToText))

      // Device info
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
      ) {
        // Device name
        Text(
          text = deviceName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Connection status
        Text(
          text = connectionStatus,
          style = MaterialTheme.typography.bodySmall,
          color = if (isConnected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        )

        // Battery level (if available)
        batteryLevel?.let { level ->
          Text(
            text = "Battery: $level%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Connection status indicator
      Icon(
        painter = painterResource(
          if (isConnected) CosmicIcons.Pairing.connected
          else CosmicIcons.Pairing.disconnected
        ),
        contentDescription = null, // Decorative, described in card semantics
        modifier = Modifier.size(Dimensions.Icon.standard),
        tint = if (isConnected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
      )
    }
  }
}

/**
 * Plugin card component for displaying plugin information.
 *
 * Features:
 * - Plugin icon
 * - Plugin name and description
 * - Toggle switch for enable/disable
 * - Clickable for settings
 * - Proper interactive states
 * - Accessibility support
 *
 * @param pluginName Name of the plugin
 * @param pluginDescription Brief description of the plugin
 * @param pluginIcon Drawable resource ID for the plugin icon
 * @param isEnabled Whether the plugin is currently enabled
 * @param isAvailable Whether the plugin is available (some plugins require device features)
 * @param onToggle Callback when toggle switch is changed
 * @param onClick Callback when card is clicked (for settings)
 * @param modifier Modifier for the card
 */
@Composable
fun PluginCard(
  pluginName: String,
  pluginDescription: String,
  pluginIcon: Int,
  isEnabled: Boolean,
  isAvailable: Boolean = true,
  onToggle: (Boolean) -> Unit,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .then(
        if (onClick != null && isAvailable) {
          Modifier.clickable(
            onClick = onClick,
            onClickLabel = "Open $pluginName settings"
          )
        } else {
          Modifier
        }
      )
      .semantics {
        contentDescription = "$pluginName, $pluginDescription, " +
          if (!isAvailable) "not available"
          else if (isEnabled) "enabled"
          else "disabled"
      },
    shape = CustomShapes.pluginCard,
    colors = CardDefaults.cardColors(
      containerColor = if (isAvailable) {
        MaterialTheme.colorScheme.surfaceVariant
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      }
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = Elevation.level1
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(SpecialSpacing.Card.padding),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Plugin icon
      Icon(
        painter = painterResource(pluginIcon),
        contentDescription = null, // Decorative, described in card semantics
        modifier = Modifier.size(Dimensions.Icon.large),
        tint = if (isAvailable) {
          if (isEnabled) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        }
      )

      Spacer(modifier = Modifier.width(SpecialSpacing.Icon.toText))

      // Plugin info
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
      ) {
        // Plugin name
        Text(
          text = pluginName,
          style = MaterialTheme.typography.titleMedium,
          color = if (isAvailable) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          }
        )

        // Plugin description
        Text(
          text = if (!isAvailable) {
            "$pluginDescription (Not available)"
          } else {
            pluginDescription
          },
          style = MaterialTheme.typography.bodySmall,
          color = if (isAvailable) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          }
        )
      }

      // Toggle switch
      Switch(
        checked = isEnabled,
        onCheckedChange = onToggle,
        enabled = isAvailable,
        modifier = Modifier
          .size(
            width = Dimensions.Toggle.switchWidth,
            height = Dimensions.Toggle.switchHeight
          )
          .semantics {
            contentDescription = if (isEnabled) {
              "Disable $pluginName"
            } else {
              "Enable $pluginName"
            }
          }
      )
    }
  }
}

/**
 * Status card component for displaying status messages, errors, warnings, or success states.
 *
 * Features:
 * - Color-coded by status type
 * - Appropriate icon for each type
 * - Title and message
 * - Optional action button
 * - Dismissible
 * - Accessibility support
 *
 * @param type Status type (error, warning, info, success)
 * @param title Status title
 * @param message Status message
 * @param onDismiss Callback when dismiss is clicked, null if not dismissible
 * @param actionLabel Optional action button label
 * @param onAction Optional action button callback
 * @param modifier Modifier for the card
 */
@Composable
fun StatusCard(
  type: StatusType,
  title: String,
  message: String,
  onDismiss: (() -> Unit)? = null,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val (containerColor, contentColor, icon) = when (type) {
    StatusType.Error -> Triple(
      MaterialTheme.colorScheme.errorContainer,
      MaterialTheme.colorScheme.onErrorContainer,
      CosmicIcons.Status.error
    )
    StatusType.Warning -> Triple(
      MaterialTheme.colorScheme.tertiaryContainer,
      MaterialTheme.colorScheme.onTertiaryContainer,
      CosmicIcons.Status.warning
    )
    StatusType.Info -> Triple(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
      CosmicIcons.Status.info
    )
    StatusType.Success -> Triple(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
      CosmicIcons.Pairing.connected
    )
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .semantics {
        contentDescription = "${type.name}: $title. $message"
      },
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(
      containerColor = containerColor
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = Elevation.level2
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(SpecialSpacing.Card.padding)
    ) {
      // Header row with icon and title
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small)
      ) {
        // Status icon
        Icon(
          painter = painterResource(icon),
          contentDescription = null, // Decorative, described in card semantics
          modifier = Modifier.size(Dimensions.Icon.standard),
          tint = contentColor
        )

        // Title
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = contentColor,
          modifier = Modifier.weight(1f)
        )

        // Dismiss button (if dismissible)
        onDismiss?.let { dismiss ->
          Surface(
            onClick = dismiss,
            modifier = Modifier
              .size(Dimensions.minTouchTarget)
              .semantics {
                role = Role.Button
                contentDescription = "Dismiss"
              },
            color = Color.Transparent
          ) {
            Icon(
              painter = painterResource(CosmicIcons.Action.delete),
              contentDescription = null, // Described in semantics
              modifier = Modifier
                .size(Dimensions.Icon.standard)
                .padding(Spacing.extraSmall),
              tint = contentColor
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(Spacing.small))

      // Message
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor
      )

      // Action button (if provided)
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(Spacing.medium))

        Surface(
          onClick = onAction,
          modifier = Modifier
            .semantics {
              role = Role.Button
              contentDescription = actionLabel
            },
          shape = MaterialTheme.shapes.small,
          color = contentColor.copy(alpha = 0.1f)
        ) {
          Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(
              horizontal = SpecialSpacing.Button.horizontalPadding,
              vertical = SpecialSpacing.Button.verticalPadding
            )
          )
        }
      }
    }
  }
}

/**
 * Status types for StatusCard
 */
enum class StatusType {
  Error,
  Warning,
  Info,
  Success
}

/**
 * Preview composables for development
 */
@Preview(showBackground = true)
@Composable
private fun DeviceCardPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column(
      modifier = Modifier.padding(Spacing.medium),
      verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
      DeviceCard(
        deviceName = "Pixel 8 Pro",
        deviceType = "phone",
        connectionStatus = "Connected",
        isConnected = true,
        batteryLevel = 85,
        onClick = {}
      )

      DeviceCard(
        deviceName = "ThinkPad X1",
        deviceType = "laptop",
        connectionStatus = "Disconnected",
        isConnected = false,
        batteryLevel = null,
        onClick = {}
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PluginCardPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column(
      modifier = Modifier.padding(Spacing.medium),
      verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
      PluginCard(
        pluginName = "MPRIS",
        pluginDescription = "Control media playback from other devices",
        pluginIcon = CosmicIcons.Plugin.mpris,
        isEnabled = true,
        onToggle = {},
        onClick = {}
      )

      PluginCard(
        pluginName = "Share",
        pluginDescription = "Share files and links between devices",
        pluginIcon = CosmicIcons.Plugin.share,
        isEnabled = false,
        onToggle = {},
        onClick = {}
      )

      PluginCard(
        pluginName = "Telephony",
        pluginDescription = "Send and receive SMS messages",
        pluginIcon = CosmicIcons.Communication.sms,
        isEnabled = true,
        isAvailable = false,
        onToggle = {},
        onClick = null
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun StatusCardPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column(
      modifier = Modifier.padding(Spacing.medium),
      verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
      StatusCard(
        type = StatusType.Error,
        title = "Connection Failed",
        message = "Unable to connect to device. Please check your network settings.",
        onDismiss = {},
        actionLabel = "RETRY",
        onAction = {}
      )

      StatusCard(
        type = StatusType.Warning,
        title = "Battery Low",
        message = "Device battery is below 20%. Consider charging soon.",
        onDismiss = {}
      )

      StatusCard(
        type = StatusType.Info,
        title = "New Update Available",
        message = "Version 2.0 is ready to install with new features and improvements.",
        actionLabel = "UPDATE",
        onAction = {}
      )

      StatusCard(
        type = StatusType.Success,
        title = "Pairing Complete",
        message = "Successfully paired with Pixel 8 Pro.",
        onDismiss = {}
      )
    }
  }
}
