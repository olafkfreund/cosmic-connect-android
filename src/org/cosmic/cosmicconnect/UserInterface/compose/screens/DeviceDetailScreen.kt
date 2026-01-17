package org.cosmic.cosmicconnect.UserInterface.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.UserInterface.compose.*
import org.cosmic.cosmicconnect.R

/**
 * Device Detail Screen
 *
 * Shows detailed information about a specific device including:
 * - Device info (name, type, battery, connection status)
 * - Plugin list with enable/disable controls
 * - Device actions (unpair, rename, settings)
 * - Pairing flow for unpaired devices
 *
 * Replaces DeviceFragment with modern Compose implementation.
 */

/**
 * Main device detail screen composable.
 *
 * @param viewModel ViewModel managing device detail state
 * @param onNavigateBack Callback to navigate back
 * @param onPluginSettings Callback to open plugin settings
 * @param onPluginActivity Callback to start plugin activity
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
  viewModel: DeviceDetailViewModel,
  onNavigateBack: () -> Unit,
  onPluginSettings: (String) -> Unit = {},
  onPluginActivity: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  var showUnpairDialog by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }

  Scaffold(
    modifier = modifier,
    topBar = {
      CosmicTopAppBar(
        title = when (val state = uiState) {
          is DeviceDetailUiState.Paired -> state.deviceName
          is DeviceDetailUiState.Unpaired -> state.deviceName
          else -> "Device Details"
        },
        navigationIcon = Icons.Default.ArrowBack,
        onNavigationClick = onNavigateBack,
        actions = {
          // Show actions only for paired devices
          if (uiState is DeviceDetailUiState.Paired) {
            IconButton(onClick = { showUnpairDialog = true }) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Unpair device"
              )
            }
          }
        }
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when (val state = uiState) {
        is DeviceDetailUiState.Loading -> {
          LoadingContent()
        }
        is DeviceDetailUiState.Error -> {
          ErrorContent(message = state.message)
        }
        is DeviceDetailUiState.Unpaired -> {
          UnpairedDeviceContent(
            state = state,
            onRequestPair = { viewModel.requestPair() },
            onAcceptPair = { viewModel.acceptPairing() },
            onRejectPair = { viewModel.rejectPairing() }
          )
        }
        is DeviceDetailUiState.Paired -> {
          PairedDeviceContent(
            state = state,
            onPluginToggle = { pluginKey, enabled ->
              viewModel.togglePlugin(pluginKey, enabled)
            },
            onPluginSettings = onPluginSettings,
            onPluginActivity = onPluginActivity,
            onRenameClick = { showRenameDialog = true }
          )
        }
      }
    }
  }

  // Unpair confirmation dialog
  if (showUnpairDialog && uiState is DeviceDetailUiState.Paired) {
    val deviceName = (uiState as DeviceDetailUiState.Paired).deviceName
    ConfirmationDialog(
      title = "Unpair device?",
      message = "This will remove $deviceName from your paired devices. You can pair again later.",
      confirmLabel = "Unpair",
      onConfirm = {
        viewModel.unpairDevice()
        showUnpairDialog = false
        onNavigateBack()
      },
      onDismiss = { showUnpairDialog = false }
    )
  }

  // Rename dialog
  if (showRenameDialog && uiState is DeviceDetailUiState.Paired) {
    val deviceName = (uiState as DeviceDetailUiState.Paired).deviceName
    InputDialog(
      title = "Rename device",
      message = "Enter a new name for this device",
      label = "Device name",
      initialValue = deviceName,
      placeholder = "Device name",
      onConfirm = { newName ->
        viewModel.renameDevice(newName)
        showRenameDialog = false
      },
      onDismiss = { showRenameDialog = false }
    )
  }
}

/**
 * Loading state content.
 */
@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    LoadingIndicator(
      size = LoadingSize.Large,
      label = "Loading device..."
    )
  }
}

/**
 * Error state content.
 */
@Composable
private fun ErrorContent(
  message: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.padding(Spacing.large),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
      Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(Dimensions.Icon.large)
      )
      Text(
        text = "Error",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * Unpaired device content showing pairing options.
 */
@Composable
private fun UnpairedDeviceContent(
  state: DeviceDetailUiState.Unpaired,
  onRequestPair: () -> Unit,
  onAcceptPair: () -> Unit,
  onRejectPair: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(Spacing.medium),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Spacing.large)
  ) {
    Spacer(modifier = Modifier.height(Spacing.extraLarge))

    // Device icon
    Icon(
      imageVector = Icons.Default.Phone,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(Dimensions.Icon.extraLarge)
    )

    // Device info
    Text(
      text = state.deviceName,
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onSurface
    )

    Text(
      text = state.deviceType,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Connection status
    ConnectionStatusIndicator(
      status = if (state.isReachable) ConnectionStatus.Connected else ConnectionStatus.Disconnected,
      modifier = Modifier.padding(vertical = Spacing.small)
    )

    Spacer(modifier = Modifier.height(Spacing.medium))

    // Pairing status and actions
    when {
      !state.isReachable -> {
        InfoCard(
          message = "Device is not reachable. Make sure it's connected to the same network.",
          severity = InfoSeverity.Warning,
          modifier = Modifier.fillMaxWidth()
        )
      }
      state.isPairRequestedByPeer -> {
        // Incoming pairing request
        InfoCard(
          message = "${state.deviceName} wants to pair with this device.",
          severity = InfoSeverity.Info,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
          OutlinedButton(
            onClick = onRejectPair,
            modifier = Modifier.weight(1f)
          ) {
            Text("Reject")
          }
          Button(
            onClick = onAcceptPair,
            modifier = Modifier.weight(1f)
          ) {
            Text("Accept")
          }
        }
      }
      state.isPairRequested -> {
        // Outgoing pairing request (waiting for response)
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
          LoadingIndicator(
            size = LoadingSize.Medium,
            label = "Waiting for ${state.deviceName} to accept..."
          )

          OutlinedButton(onClick = onRejectPair) {
            Text("Cancel")
          }
        }
      }
      else -> {
        // Not paired, no request
        InfoCard(
          message = "This device is not paired. Pair to access its features.",
          severity = InfoSeverity.Info,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        Button(
          onClick = onRequestPair,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.Icon.small)
          )
          Spacer(modifier = Modifier.width(Spacing.small))
          Text("Request Pairing")
        }
      }
    }
  }
}

/**
 * Paired device content showing device info and plugins.
 */
@Composable
private fun PairedDeviceContent(
  state: DeviceDetailUiState.Paired,
  onPluginToggle: (String, Boolean) -> Unit,
  onPluginSettings: (String) -> Unit,
  onPluginActivity: (String) -> Unit,
  onRenameClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(vertical = Spacing.small)
  ) {
    // Device info card
    item {
      DeviceInfoSection(
        state = state,
        onRenameClick = onRenameClick
      )
    }

    // Plugins section
    item {
      SectionHeader(
        title = "Plugins",
        modifier = Modifier.padding(
          horizontal = Spacing.medium,
          vertical = Spacing.small
        )
      )
    }

    if (state.plugins.isEmpty()) {
      item {
        InfoCard(
          message = "No plugins available for this device.",
          severity = InfoSeverity.Info,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium)
        )
      }
    } else {
      items(
        items = state.plugins,
        key = { it.key }
      ) { plugin ->
        PluginCard(
          pluginName = plugin.name,
          pluginDescription = plugin.description,
          pluginIcon = plugin.icon,
          isEnabled = plugin.isEnabled,
          isAvailable = plugin.isAvailable,
          onToggle = { enabled -> onPluginToggle(plugin.key, enabled) },
          onClick = if (plugin.hasMainActivity) {
            { onPluginActivity(plugin.key) }
          } else null,
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.extraSmall
          )
        )
      }
    }

    // Bottom spacing
    item {
      Spacer(modifier = Modifier.height(Spacing.large))
    }
  }
}

/**
 * Device information section.
 */
@Composable
private fun DeviceInfoSection(
  state: DeviceDetailUiState.Paired,
  onRenameClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.padding(Spacing.medium),
    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
  ) {
    DeviceCard(
      deviceName = state.deviceName,
      deviceType = state.deviceType,
      connectionStatus = if (state.isReachable) "Connected" else "Disconnected",
      isConnected = state.isReachable,
      batteryLevel = state.batteryLevel,
      onClick = onRenameClick
    )

    // Connection and battery indicators
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
      ConnectionStatusIndicator(
        status = if (state.isReachable) ConnectionStatus.Connected else ConnectionStatus.Disconnected,
        modifier = Modifier.weight(1f)
      )

      if (state.batteryLevel != null) {
        BatteryStatusIndicator(
          batteryLevel = state.batteryLevel,
          isCharging = state.isCharging,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * Preview Functions
 */

@Preview(name = "Paired Device", showBackground = true)
@Composable
private fun PreviewPairedDevice() {
  CosmicTheme(context = LocalContext.current) {
    Surface {
      PairedDeviceContent(
        state = DeviceDetailUiState.Paired(
          deviceName = "Pixel 8 Pro",
          deviceType = "Phone",
          isReachable = true,
          batteryLevel = 75,
          isCharging = true,
          plugins = listOf(
            PluginInfo(
              key = "battery",
              name = "Battery Monitor",
              description = "Monitor battery status",
              icon = R.drawable.ic_baseline_battery_90_24,
              isEnabled = true,
              isAvailable = true,
              hasSettings = true,
              hasMainActivity = false
            ),
            PluginInfo(
              key = "clipboard",
              name = "Clipboard Sync",
              description = "Sync clipboard content",
              icon = R.drawable.ic_baseline_content_paste_24,
              isEnabled = true,
              isAvailable = true,
              hasSettings = false,
              hasMainActivity = false
            ),
            PluginInfo(
              key = "share",
              name = "Share & Receive",
              description = "Send and receive files",
              icon = R.drawable.ic_baseline_share_24,
              isEnabled = false,
              isAvailable = true,
              hasSettings = true,
              hasMainActivity = true
            )
          )
        ),
        onPluginToggle = { _, _ -> },
        onPluginSettings = {},
        onPluginActivity = {},
        onRenameClick = {}
      )
    }
  }
}

@Preview(name = "Unpaired Device - Ready to Pair", showBackground = true)
@Composable
private fun PreviewUnpairedDevice() {
  CosmicTheme(context = LocalContext.current) {
    Surface {
      UnpairedDeviceContent(
        state = DeviceDetailUiState.Unpaired(
          deviceName = "New Laptop",
          deviceType = "Desktop",
          isReachable = true,
          isPairRequested = false,
          isPairRequestedByPeer = false
        ),
        onRequestPair = {},
        onAcceptPair = {},
        onRejectPair = {}
      )
    }
  }
}

@Preview(name = "Unpaired Device - Incoming Request", showBackground = true)
@Composable
private fun PreviewIncomingPairRequest() {
  CosmicTheme(context = LocalContext.current) {
    Surface {
      UnpairedDeviceContent(
        state = DeviceDetailUiState.Unpaired(
          deviceName = "Work Laptop",
          deviceType = "Desktop",
          isReachable = true,
          isPairRequested = false,
          isPairRequestedByPeer = true
        ),
        onRequestPair = {},
        onAcceptPair = {},
        onRejectPair = {}
      )
    }
  }
}

@Preview(name = "Unpaired Device - Waiting for Response", showBackground = true)
@Composable
private fun PreviewWaitingForPairResponse() {
  CosmicTheme(context = LocalContext.current) {
    Surface {
      UnpairedDeviceContent(
        state = DeviceDetailUiState.Unpaired(
          deviceName = "Home Desktop",
          deviceType = "Desktop",
          isReachable = true,
          isPairRequested = true,
          isPairRequestedByPeer = false
        ),
        onRequestPair = {},
        onAcceptPair = {},
        onRejectPair = {}
      )
    }
  }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun PreviewLoadingState() {
  CosmicTheme(context = LocalContext.current) {
    Surface {
      LoadingContent()
    }
  }
}

@Preview(name = "Error State", showBackground = true)
@Composable
private fun PreviewErrorState() {
  CosmicTheme(context = LocalContext.current) {
    Surface {
      ErrorContent(message = "Device not found")
    }
  }
}
