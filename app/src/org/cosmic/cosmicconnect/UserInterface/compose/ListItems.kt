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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Device list item component for displaying devices in vertical lists.
 *
 * Simpler than DeviceCard, optimized for LazyColumn usage.
 * No elevation, uses dividers instead of card borders.
 *
 * @param deviceName Name of the device
 * @param deviceType Type of device (phone, laptop, desktop, etc.)
 * @param subtitle Optional subtitle (connection status, last seen, etc.)
 * @param isConnected Whether the device is currently connected
 * @param showDivider Whether to show bottom divider
 * @param onClick Callback when item is clicked
 * @param modifier Modifier for the list item
 */
@Composable
fun DeviceListItem(
  deviceName: String,
  deviceType: String,
  subtitle: String? = null,
  isConnected: Boolean,
  showDivider: Boolean = true,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
      onClick = onClick,
      modifier = Modifier
        .fillMaxWidth()
        .height(Dimensions.ListItem.standardHeight)
        .semantics {
          role = Role.Button
          contentDescription = buildString {
            append(deviceName)
            append(", ")
            append(deviceType)
            subtitle?.let {
              append(", ")
              append(it)
            }
            append(if (isConnected) ", connected" else ", disconnected")
          }
        },
      color = MaterialTheme.colorScheme.surface
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = SpecialSpacing.ListItem.contentPadding),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Device icon
        Icon(
          painter = painterResource(getDeviceIcon(deviceType)),
          contentDescription = null, // Decorative, described in semantics
          modifier = Modifier.size(Dimensions.ListItem.leadingIconSize),
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
          Text(
            text = deviceName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
          )

          subtitle?.let { sub ->
            Text(
              text = sub,
              style = MaterialTheme.typography.bodyMedium,
              color = if (isConnected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }

        // Connection indicator
        Icon(
          painter = painterResource(
            if (isConnected) CosmicIcons.Pairing.connected
            else CosmicIcons.Pairing.disconnected
          ),
          contentDescription = null, // Decorative, described in semantics
          modifier = Modifier.size(Dimensions.ListItem.trailingIconSize),
          tint = if (isConnected) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }
        )
      }
    }

    if (showDivider) {
      HorizontalDivider(
        modifier = Modifier.padding(start = Dimensions.ListItem.leadingIconSize + SpecialSpacing.ListItem.iconToText + SpecialSpacing.ListItem.contentPadding),
        thickness = Dimensions.Divider.thickness,
        color = MaterialTheme.colorScheme.outlineVariant
      )
    }
  }
}

/**
 * Plugin list item component for displaying plugins in vertical lists.
 *
 * Includes toggle switch for enabling/disabling plugins.
 * Simpler than PluginCard, optimized for LazyColumn usage.
 *
 * @param pluginName Name of the plugin
 * @param pluginDescription Brief description of the plugin
 * @param pluginIcon Drawable resource ID for the plugin icon
 * @param isEnabled Whether the plugin is currently enabled
 * @param isAvailable Whether the plugin is available
 * @param showDivider Whether to show bottom divider
 * @param onToggle Callback when toggle is changed
 * @param onClick Optional callback when item body is clicked (for settings)
 * @param modifier Modifier for the list item
 */
@Composable
fun PluginListItem(
  pluginName: String,
  pluginDescription: String,
  pluginIcon: Int,
  isEnabled: Boolean,
  isAvailable: Boolean = true,
  showDivider: Boolean = true,
  onToggle: (Boolean) -> Unit,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
      onClick = onClick ?: {},
      enabled = onClick != null && isAvailable,
      modifier = Modifier
        .fillMaxWidth()
        .height(Dimensions.ListItem.largeHeight)
        .semantics {
          if (onClick != null) {
            role = Role.Button
          }
          contentDescription = buildString {
            append(pluginName)
            append(", ")
            append(pluginDescription)
            when {
              !isAvailable -> append(", not available")
              isEnabled -> append(", enabled")
              else -> append(", disabled")
            }
          }
        },
      color = MaterialTheme.colorScheme.surface
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = SpecialSpacing.ListItem.contentPadding),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Plugin icon
        Icon(
          painter = painterResource(pluginIcon),
          contentDescription = null, // Decorative, described in semantics
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

        Spacer(modifier = Modifier.width(SpecialSpacing.ListItem.iconToText))

        // Plugin info
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
          Text(
            text = pluginName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isAvailable) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
          )

          Text(
            text = if (!isAvailable) {
              "$pluginDescription (Not available)"
            } else {
              pluginDescription
            },
            style = MaterialTheme.typography.bodyMedium,
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

    if (showDivider) {
      HorizontalDivider(
        modifier = Modifier.padding(start = Dimensions.Icon.large + SpecialSpacing.ListItem.iconToText + SpecialSpacing.ListItem.contentPadding),
        thickness = Dimensions.Divider.thickness,
        color = MaterialTheme.colorScheme.outlineVariant
      )
    }
  }
}

/**
 * Section header component for dividing list sections.
 *
 * Provides visual separation and context for list groups.
 *
 * @param title Section title text
 * @param subtitle Optional subtitle/description
 * @param showTopPadding Whether to add top padding (use false for first section)
 * @param modifier Modifier for the header
 */
@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  showTopPadding: Boolean = true,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = SpecialSpacing.Screen.horizontalPadding,
        end = SpecialSpacing.Screen.horizontalPadding,
        top = if (showTopPadding) SpecialSpacing.Screen.sectionSpacing else 0.dp,
        bottom = Spacing.medium
      )
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary
    )

    subtitle?.let { sub ->
      Spacer(modifier = Modifier.height(Spacing.extraSmall))
      Text(
        text = sub,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

/**
 * Simple list item with icon, text, and optional trailing content.
 *
 * Generic list item for various use cases (settings, navigation, etc.)
 *
 * @param text Primary text (can also use 'title' as alias for better readability)
 * @param icon Optional leading icon (drawable resource)
 * @param iconVector Optional leading icon (ImageVector) - takes priority over icon
 * @param secondaryText Optional secondary text (can also use 'subtitle' as alias)
 * @param trailingIcon Optional trailing icon (drawable resource)
 * @param trailingIconVector Optional trailing icon (ImageVector)
 * @param trailingContent Optional custom trailing content
 * @param showDivider Whether to show bottom divider
 * @param onClick Optional click callback
 * @param modifier Modifier for the list item
 */
@Composable
fun SimpleListItem(
  text: String? = null,
  icon: Int? = null,
  iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
  secondaryText: String? = null,
  trailingIcon: Int? = null,
  trailingIconVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  showDivider: Boolean = true,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  // Alternate parameter names for better API
  title: String? = null,
  subtitle: String? = null,
  leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
  // Resolve parameter aliases
  val finalText = text ?: title ?: ""
  val finalSecondaryText = secondaryText ?: subtitle
  val finalIconVector = iconVector ?: leadingIcon
  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
      onClick = onClick ?: {},
      enabled = onClick != null,
      modifier = Modifier
        .fillMaxWidth()
        .height(
          if (finalSecondaryText != null) Dimensions.ListItem.largeHeight
          else Dimensions.ListItem.standardHeight
        )
        .then(
          if (onClick != null) {
            Modifier.semantics {
              role = Role.Button
              contentDescription = buildString {
                append(finalText)
                finalSecondaryText?.let {
                  append(", ")
                  append(it)
                }
              }
            }
          } else {
            Modifier
          }
        ),
      color = MaterialTheme.colorScheme.surface
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = SpecialSpacing.ListItem.contentPadding),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Leading icon (ImageVector takes priority, then drawable resource)
        if (finalIconVector != null) {
          Icon(
            imageVector = finalIconVector,
            contentDescription = null, // Decorative
            modifier = Modifier.size(Dimensions.Icon.standard),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(SpecialSpacing.ListItem.iconToText))
        } else if (icon != null) {
          Icon(
            painter = painterResource(icon),
            contentDescription = null, // Decorative
            modifier = Modifier.size(Dimensions.Icon.standard),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(SpecialSpacing.ListItem.iconToText))
        }

        // Text content
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
          Text(
            text = finalText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
          )

          finalSecondaryText?.let { secondary ->
            Text(
              text = secondary,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Trailing content (ImageVector takes priority, then drawable resource)
        when {
          trailingContent != null -> trailingContent()
          trailingIconVector != null -> {
            Icon(
              imageVector = trailingIconVector,
              contentDescription = null, // Decorative
              modifier = Modifier.size(Dimensions.Icon.standard),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          trailingIcon != null -> {
            Icon(
              painter = painterResource(trailingIcon),
              contentDescription = null, // Decorative
              modifier = Modifier.size(Dimensions.Icon.standard),
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    if (showDivider) {
      HorizontalDivider(
        modifier = Modifier.padding(
          start = if (finalIconVector != null || icon != null) {
            Dimensions.Icon.standard + SpecialSpacing.ListItem.iconToText + SpecialSpacing.ListItem.contentPadding
          } else {
            SpecialSpacing.ListItem.contentPadding
          }
        ),
        thickness = Dimensions.Divider.thickness,
        color = MaterialTheme.colorScheme.outlineVariant
      )
    }
  }
}

/**
 * Preview composables for development
 */
@Preview(showBackground = true)
@Composable
private fun DeviceListItemPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column {
      DeviceListItem(
        deviceName = "Pixel 8 Pro",
        deviceType = "phone",
        subtitle = "Connected",
        isConnected = true,
        onClick = {}
      )

      DeviceListItem(
        deviceName = "ThinkPad X1",
        deviceType = "laptop",
        subtitle = "Last seen 5 minutes ago",
        isConnected = false,
        onClick = {}
      )

      DeviceListItem(
        deviceName = "Desktop PC",
        deviceType = "desktop",
        subtitle = null,
        isConnected = true,
        showDivider = false,
        onClick = {}
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun PluginListItemPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column {
      PluginListItem(
        pluginName = "MPRIS",
        pluginDescription = "Control media playback",
        pluginIcon = CosmicIcons.Plugin.mpris,
        isEnabled = true,
        onToggle = {},
        onClick = {}
      )

      PluginListItem(
        pluginName = "Share",
        pluginDescription = "Share files and links",
        pluginIcon = CosmicIcons.Plugin.share,
        isEnabled = false,
        onToggle = {},
        onClick = {}
      )

      PluginListItem(
        pluginName = "Telephony",
        pluginDescription = "Send and receive SMS",
        pluginIcon = CosmicIcons.Communication.sms,
        isEnabled = true,
        isAvailable = false,
        showDivider = false,
        onToggle = {},
        onClick = null
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SectionHeaderPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column {
      SectionHeader(
        title = "Connected Devices",
        subtitle = "2 devices connected",
        showTopPadding = false
      )

      SectionHeader(
        title = "Available Plugins",
        subtitle = null,
        showTopPadding = true
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SimpleListItemPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    Column {
      SimpleListItem(
        text = "Settings",
        icon = CosmicIcons.Settings.settings,
        trailingIcon = CosmicIcons.Navigation.forward,
        onClick = {}
      )

      SimpleListItem(
        text = "Network Status",
        icon = CosmicIcons.Pairing.wifi,
        secondaryText = "Connected to WiFi",
        trailingIcon = CosmicIcons.Navigation.forward,
        onClick = {}
      )

      SimpleListItem(
        text = "About",
        secondaryText = "Version 2.0.0",
        trailingIcon = CosmicIcons.Navigation.forward,
        showDivider = false,
        onClick = {}
      )
    }
  }
}
