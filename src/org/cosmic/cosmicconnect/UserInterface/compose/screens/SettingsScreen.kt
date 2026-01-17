package org.cosmic.cosmicconnect.UserInterface.compose.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.UserInterface.compose.*
import org.cosmic.cosmicconnect.R

/**
 * Settings Screen
 *
 * Provides app configuration, preferences, and about information.
 * Replaces SettingsFragment with modern Compose implementation.
 *
 * Features:
 * - General settings (device name, theme)
 * - Connection settings (trusted networks, custom devices, bluetooth)
 * - Advanced settings (notifications, logs)
 * - About section (version, licenses, source code)
 */

/**
 * Main settings screen composable.
 *
 * @param viewModel ViewModel managing settings state
 * @param onNavigateBack Callback to navigate back
 * @param onNavigateToTrustedNetworks Callback to navigate to trusted networks
 * @param onNavigateToCustomDevices Callback to navigate to custom devices
 * @param onExportLogs Callback to export logs
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateBack: () -> Unit,
  onNavigateToTrustedNetworks: () -> Unit = {},
  onNavigateToCustomDevices: () -> Unit = {},
  onExportLogs: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  var showRenameDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = {
      CosmicTopAppBar(
        title = "Settings",
        navigationIcon = Icons.Default.ArrowBack,
        onNavigationClick = onNavigateBack
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(vertical = Spacing.small)
    ) {
      // General Settings Section
      item {
        SectionHeader(
          title = "General",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }

      // Device Name
      item {
        SimpleListItem(
          title = "Device Name",
          subtitle = uiState.deviceName,
          leadingIcon = Icons.Default.Phone,
          onClick = { showRenameDialog = true }
        )
      }

      // Theme
      item {
        SimpleListItem(
          title = "Theme",
          subtitle = getThemeDisplayName(uiState.theme),
          leadingIcon = Icons.Default.Palette,
          onClick = { showThemeDialog = true }
        )
      }

      // Connection Settings Section
      item {
        SectionHeader(
          title = "Connection",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }

      // Trusted Networks
      item {
        SimpleListItem(
          title = "Trusted Networks",
          subtitle = "Configure networks where devices can be discovered",
          leadingIcon = Icons.Default.Wifi,
          onClick = onNavigateToTrustedNetworks
        )
      }

      // Custom Devices
      item {
        SimpleListItem(
          title = "Custom Devices",
          subtitle = "${uiState.customDevicesCount} custom device(s) configured",
          leadingIcon = Icons.Default.Devices,
          onClick = {
            onNavigateToCustomDevices()
            viewModel.refreshCustomDevicesCount()
          }
        )
      }

      // Bluetooth
      item {
        CosmicSwitch(
          checked = uiState.bluetoothEnabled,
          onCheckedChange = { enabled ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabled) {
              // Would need to request permissions here
              // For now, just toggle
            }
            viewModel.toggleBluetooth(enabled)
          },
          label = "Bluetooth Support",
          description = "Enable Bluetooth for device discovery and connection"
        )
      }

      // Advanced Settings Section
      item {
        SectionHeader(
          title = "Advanced",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }

      // Persistent Notification
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        item {
          SimpleListItem(
            title = "Persistent Notification",
            subtitle = "Configure notification settings in system preferences",
            leadingIcon = Icons.Default.Notifications,
            onClick = {
              val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
              }
              context.startActivity(intent)
            }
          )
        }
      } else if (uiState.persistentNotificationEnabled != null) {
        item {
          CosmicSwitch(
            checked = uiState.persistentNotificationEnabled!!,
            onCheckedChange = { viewModel.togglePersistentNotification(it) },
            label = "Persistent Notification",
            description = "Show ongoing notification while service is running"
          )
        }
      }

      // Export Logs
      item {
        SimpleListItem(
          title = "Export Logs",
          subtitle = "Export diagnostic logs to a file",
          leadingIcon = Icons.Default.BugReport,
          onClick = onExportLogs
        )
      }

      // About Section
      item {
        SectionHeader(
          title = "About",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }

      // Version
      item {
        SimpleListItem(
          title = "COSMIC Connect",
          subtitle = "Version ${uiState.appVersion}",
          leadingIcon = Icons.Default.Info,
          onClick = { showAboutDialog = true }
        )
      }

      // Source Code
      item {
        SimpleListItem(
          title = "Source Code",
          subtitle = "View on GitHub",
          leadingIcon = Icons.Default.Code,
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW).apply {
              data = android.net.Uri.parse("https://github.com/olafkfreund/cosmic-connect-android")
            }
            context.startActivity(intent)
          }
        )
      }

      // Bottom spacing
      item {
        Spacer(modifier = Modifier.height(Spacing.large))
      }
    }
  }

  // Rename dialog
  if (showRenameDialog) {
    InputDialog(
      title = "Rename Device",
      message = "Enter a new name for this device",
      initialValue = uiState.deviceName,
      placeholder = "Device name",
      onConfirm = { newName ->
        viewModel.updateDeviceName(newName)
        showRenameDialog = false
      },
      onDismiss = { showRenameDialog = false }
    )
  }

  // Theme dialog
  if (showThemeDialog) {
    ThemeSelectionDialog(
      currentTheme = uiState.theme,
      onThemeSelected = { theme ->
        viewModel.updateTheme(theme)
        showThemeDialog = false
      },
      onDismiss = { showThemeDialog = false }
    )
  }

  // About dialog
  if (showAboutDialog) {
    AboutDialog(
      appVersion = uiState.appVersion,
      onDismiss = { showAboutDialog = false }
    )
  }
}

/**
 * Theme selection dialog.
 */
@Composable
private fun ThemeSelectionDialog(
  currentTheme: String,
  onThemeSelected: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val themes = remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      listOf(
        "light" to "Light",
        "dark" to "Dark",
        "follow_system" to "Follow System"
      )
    } else {
      listOf(
        "light" to "Light",
        "dark" to "Dark"
      )
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Choose Theme") },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CosmicRadioGroup(
          options = themes.map { it.second },
          selectedOption = themes.find { it.first == currentTheme }?.second ?: "Follow System",
          onOptionSelected = { selectedName ->
            val theme = themes.find { it.second == selectedName }?.first ?: "follow_system"
            onThemeSelected(theme)
          }
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

/**
 * About dialog with app information.
 */
@Composable
private fun AboutDialog(
  appVersion: String,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
    },
    title = { Text("COSMIC Connect") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
      ) {
        Text(
          text = "Version $appVersion",
          style = MaterialTheme.typography.bodyLarge
        )

        Text(
          text = "COSMIC Connect allows you to integrate your Android device with the COSMIC Desktop environment.",
          style = MaterialTheme.typography.bodyMedium
        )

        Text(
          text = "Features:",
          style = MaterialTheme.typography.titleSmall
        )

        Column(modifier = Modifier.padding(start = Spacing.medium)) {
          listOf(
            "Share files and links",
            "Sync clipboard",
            "Monitor battery status",
            "Control media playback",
            "Send and receive notifications",
            "Run remote commands"
          ).forEach { feature ->
            Text(
              text = "• $feature",
              style = MaterialTheme.typography.bodySmall
            )
          }
        }

        Divider()

        Text(
          text = "Based on KDE Connect",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "Licensed under GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

/**
 * Get display name for theme value.
 */
private fun getThemeDisplayName(theme: String): String = when (theme) {
  "light" -> "Light"
  "dark" -> "Dark"
  "follow_system" -> "Follow System"
  else -> "Follow System"
}

/**
 * Preview Functions
 */

@Preview(name = "Settings Screen", showBackground = true)
@Composable
private fun PreviewSettingsScreen() {
  CosmicTheme {
    Surface {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(title = "General")
        SimpleListItem(
          title = "Device Name",
          subtitle = "Pixel 8 Pro",
          leadingIcon = Icons.Default.Phone,
          onClick = {}
        )
        SimpleListItem(
          title = "Theme",
          subtitle = "Follow System",
          leadingIcon = Icons.Default.Palette,
          onClick = {}
        )

        SectionHeader(title = "Connection")
        SimpleListItem(
          title = "Trusted Networks",
          subtitle = "Configure networks where devices can be discovered",
          leadingIcon = Icons.Default.Wifi,
          onClick = {}
        )
        SimpleListItem(
          title = "Custom Devices",
          subtitle = "3 custom device(s) configured",
          leadingIcon = Icons.Default.Devices,
          onClick = {}
        )
        CosmicSwitch(
          checked = true,
          onCheckedChange = {},
          label = "Bluetooth Support",
          description = "Enable Bluetooth for device discovery and connection"
        )

        SectionHeader(title = "Advanced")
        SimpleListItem(
          title = "Export Logs",
          subtitle = "Export diagnostic logs to a file",
          leadingIcon = Icons.Default.BugReport,
          onClick = {}
        )

        SectionHeader(title = "About")
        SimpleListItem(
          title = "COSMIC Connect",
          subtitle = "Version 1.0.0",
          leadingIcon = Icons.Default.Info,
          onClick = {}
        )
      }
    }
  }
}

@Preview(name = "Theme Dialog", showBackground = true)
@Composable
private fun PreviewThemeDialog() {
  CosmicTheme {
    ThemeSelectionDialog(
      currentTheme = "follow_system",
      onThemeSelected = {},
      onDismiss = {}
    )
  }
}

@Preview(name = "About Dialog", showBackground = true)
@Composable
private fun PreviewAboutDialog() {
  CosmicTheme {
    AboutDialog(
      appVersion = "1.0.0",
      onDismiss = {}
    )
  }
}
