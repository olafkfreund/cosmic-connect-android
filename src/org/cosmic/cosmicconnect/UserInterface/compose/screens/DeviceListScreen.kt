package org.cosmic.cosmicconnect.UserInterface.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.UserInterface.compose.*
import org.cosmic.cosmicconnect.R

/**
 * Device List Screen
 *
 * The main screen showing all discovered, paired, and remembered devices.
 * Replaces PairingFragment with modern Compose implementation.
 *
 * Features:
 * - Device categorization (Connected, Available, Remembered)
 * - Pull-to-refresh device discovery
 * - Status indicators (connection, battery, discovery)
 * - Info headers for connectivity states
 * - Empty state handling
 * - Device actions (connect, disconnect, pair, unpair)
 */

/**
 * Main device list screen composable.
 *
 * @param viewModel ViewModel managing device list state
 * @param onDeviceClick Callback when a device is clicked
 * @param onNavigateToCustomDevices Callback to navigate to custom devices screen
 * @param onNavigateToTrustedNetworks Callback to navigate to trusted networks screen
 * @param onNavigateToSettings Callback to navigate to settings screen
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
  viewModel: DeviceListViewModel,
  onDeviceClick: (Device) -> Unit,
  onNavigateToCustomDevices: () -> Unit = {},
  onNavigateToTrustedNetworks: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

  val pullRefreshState = rememberPullRefreshState(
    refreshing = isRefreshing,
    onRefresh = { viewModel.refreshDevices() }
  )

  var showMenu by remember { mutableStateOf(false) }
  var deviceToUnpair by remember { mutableStateOf<Device?>(null) }

  Scaffold(
    modifier = modifier,
    topBar = {
      CosmicTopAppBar(
        title = stringResource(R.string.pairing_title),
        actions = {
          // Discovery status indicator
          if (uiState.isDiscovering) {
            SyncStatusIndicator(
              status = SyncStatus.Syncing,
              modifier = Modifier.padding(horizontal = Spacing.small)
            )
          }

          // Refresh button
          IconButton(onClick = { viewModel.refreshDevices() }) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh devices"
            )
          }

          // More menu
          IconButton(onClick = { showMenu = true }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "More options"
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Custom Devices") },
              onClick = {
                showMenu = false
                onNavigateToCustomDevices()
              }
            )
            DropdownMenuItem(
              text = { Text("Trusted Networks") },
              onClick = {
                showMenu = false
                onNavigateToTrustedNetworks()
              }
            )
          }
        }
      )
    },
    bottomBar = {
      CosmicBottomNavigationBar(
        selectedItem = 0, // Devices tab selected
        onItemSelected = { index ->
          when (index) {
            1 -> onNavigateToSettings()
          }
        }
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .pullRefresh(pullRefreshState)
    ) {
      DeviceListContent(
        uiState = uiState,
        onDeviceClick = onDeviceClick,
        onDeviceUnpair = { device -> deviceToUnpair = device },
        modifier = Modifier.fillMaxSize()
      )

      PullRefreshIndicator(
        refreshing = isRefreshing,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter),
        backgroundColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
      )
    }
  }

  // Unpair confirmation dialog
  deviceToUnpair?.let { device ->
    ConfirmationDialog(
      title = "Unpair device?",
      message = "This will remove ${device.name} from your paired devices. You can pair again later.",
      confirmText = "Unpair",
      onConfirm = {
        viewModel.unpairDevice(device)
        deviceToUnpair = null
      },
      onDismiss = { deviceToUnpair = null }
    )
  }
}

/**
 * Content area of the device list.
 *
 * Handles different states: loading, empty, error, and success with devices.
 */
@Composable
private fun DeviceListContent(
  uiState: DeviceListUiState,
  onDeviceClick: (Device) -> Unit,
  onDeviceUnpair: (Device) -> Unit,
  modifier: Modifier = Modifier
) {
  when {
    uiState.isLoading && uiState.devices.isEmpty() -> {
      LoadingState(modifier = modifier)
    }
    uiState.error != null -> {
      ErrorState(
        error = uiState.error,
        modifier = modifier
      )
    }
    uiState.devices.isEmpty() -> {
      EmptyState(
        connectivityState = uiState.connectivityState,
        modifier = modifier
      )
    }
    else -> {
      DeviceList(
        devices = uiState.devices,
        connectivityState = uiState.connectivityState,
        hasDuplicateNames = uiState.hasDuplicateNames,
        onDeviceClick = onDeviceClick,
        onDeviceUnpair = onDeviceUnpair,
        modifier = modifier
      )
    }
  }
}

/**
 * Loading state UI.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
      LoadingIndicator(
        size = LoadingSize.Large,
        label = "Discovering devices..."
      )
    }
  }
}

/**
 * Error state UI.
 */
@Composable
private fun ErrorState(
  error: String,
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
      Text(
        text = "Error",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
      )
      Text(
        text = error,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * Empty state UI.
 */
@Composable
private fun EmptyState(
  connectivityState: ConnectivityState,
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
      Text(
        text = "No devices found",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface
      )

      val message = when (connectivityState) {
        ConnectivityState.NO_WIFI -> "Please connect to Wi-Fi to discover devices"
        ConnectivityState.NO_NOTIFICATIONS -> "Enable notifications to receive pairing requests"
        ConnectivityState.NOT_TRUSTED -> "You're on an untrusted network. Add this network to trusted networks to discover devices."
        ConnectivityState.OK -> "Make sure COSMIC Connect is running on the device you want to connect to"
      }

      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

/**
 * Device list with categorized sections.
 */
@Composable
private fun DeviceList(
  devices: List<CategorizedDevice>,
  connectivityState: ConnectivityState,
  hasDuplicateNames: Boolean,
  onDeviceClick: (Device) -> Unit,
  onDeviceUnpair: (Device) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(vertical = Spacing.small)
  ) {
    // Connectivity info header
    item {
      ConnectivityInfoHeader(
        state = connectivityState,
        hasDuplicateNames = hasDuplicateNames
      )
    }

    // Connected devices section
    val connectedDevices = devices.filter { it.category == DeviceCategory.CONNECTED }
    if (connectedDevices.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Connected Devices",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }
      items(
        items = connectedDevices,
        key = { it.device.deviceId }
      ) { categorizedDevice ->
        DeviceListItemWithActions(
          device = categorizedDevice.device,
          onClick = { onDeviceClick(categorizedDevice.device) },
          onUnpair = { onDeviceUnpair(categorizedDevice.device) }
        )
      }
    }

    // Available devices section
    val availableDevices = devices.filter { it.category == DeviceCategory.AVAILABLE }
    if (availableDevices.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Available Devices",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }
      items(
        items = availableDevices,
        key = { it.device.deviceId }
      ) { categorizedDevice ->
        DeviceListItemWithActions(
          device = categorizedDevice.device,
          onClick = { onDeviceClick(categorizedDevice.device) }
        )
      }
    }

    // Remembered devices section
    val rememberedDevices = devices.filter { it.category == DeviceCategory.REMEMBERED }
    if (rememberedDevices.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Remembered Devices",
          modifier = Modifier.padding(
            horizontal = Spacing.medium,
            vertical = Spacing.small
          )
        )
      }
      items(
        items = rememberedDevices,
        key = { it.device.deviceId }
      ) { categorizedDevice ->
        DeviceListItemWithActions(
          device = categorizedDevice.device,
          onClick = { onDeviceClick(categorizedDevice.device) },
          onUnpair = { onDeviceUnpair(categorizedDevice.device) }
        )
      }
    }
  }
}

/**
 * Connectivity info header with warnings/notifications.
 */
@Composable
private fun ConnectivityInfoHeader(
  state: ConnectivityState,
  hasDuplicateNames: Boolean,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    when (state) {
      ConnectivityState.NO_WIFI -> {
        InfoCard(
          message = "Not connected to Wi-Fi. Connect to Wi-Fi to discover devices.",
          severity = InfoSeverity.Warning,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        )
      }
      ConnectivityState.NO_NOTIFICATIONS -> {
        InfoCard(
          message = "Notifications are disabled. Enable notifications to receive pairing requests.",
          severity = InfoSeverity.Warning,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        )
      }
      ConnectivityState.NOT_TRUSTED -> {
        InfoCard(
          message = "You're on an untrusted network. Add this network to trusted networks to discover devices.",
          severity = InfoSeverity.Info,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        )
      }
      ConnectivityState.OK -> {
        InfoCard(
          message = "Make sure COSMIC Connect is running on the devices you want to connect to.",
          severity = InfoSeverity.Info,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.small)
        )
      }
    }

    if (hasDuplicateNames) {
      InfoCard(
        message = "Warning: Multiple devices with the same name detected. This may cause confusion.",
        severity = InfoSeverity.Warning,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = Spacing.medium, vertical = Spacing.small)
      )
    }
  }
}

/**
 * Device list item with action buttons.
 */
@Composable
private fun DeviceListItemWithActions(
  device: Device,
  onClick: () -> Unit,
  onUnpair: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  DeviceListItem(
    deviceName = device.name,
    deviceType = device.deviceType.toString(),
    isConnected = device.isReachable && device.isPaired,
    isPaired = device.isPaired,
    batteryLevel = device.batteryLevel,
    onClick = onClick,
    onLongClick = onUnpair,
    modifier = modifier
  )
}

/**
 * Preview Functions
 */

@Preview(name = "Device List - Connected", showBackground = true)
@Composable
private fun PreviewDeviceListConnected() {
  CosmicTheme {
    Surface {
      DeviceList(
        devices = listOf(
          CategorizedDevice(
            device = createMockDevice("Laptop", isPaired = true, isReachable = true, battery = 85),
            category = DeviceCategory.CONNECTED
          ),
          CategorizedDevice(
            device = createMockDevice("Phone", isPaired = true, isReachable = true, battery = 42),
            category = DeviceCategory.CONNECTED
          )
        ),
        connectivityState = ConnectivityState.OK,
        hasDuplicateNames = false,
        onDeviceClick = {},
        onDeviceUnpair = {}
      )
    }
  }
}

@Preview(name = "Device List - All Categories", showBackground = true)
@Composable
private fun PreviewDeviceListAllCategories() {
  CosmicTheme {
    Surface {
      DeviceList(
        devices = listOf(
          CategorizedDevice(
            device = createMockDevice("Laptop", isPaired = true, isReachable = true, battery = 85),
            category = DeviceCategory.CONNECTED
          ),
          CategorizedDevice(
            device = createMockDevice("New Phone", isPaired = false, isReachable = true),
            category = DeviceCategory.AVAILABLE
          ),
          CategorizedDevice(
            device = createMockDevice("Old Tablet", isPaired = true, isReachable = false),
            category = DeviceCategory.REMEMBERED
          )
        ),
        connectivityState = ConnectivityState.OK,
        hasDuplicateNames = false,
        onDeviceClick = {},
        onDeviceUnpair = {}
      )
    }
  }
}

@Preview(name = "Empty State - No WiFi", showBackground = true)
@Composable
private fun PreviewEmptyStateNoWiFi() {
  CosmicTheme {
    Surface {
      EmptyState(connectivityState = ConnectivityState.NO_WIFI)
    }
  }
}

@Preview(name = "Empty State - Not Trusted", showBackground = true)
@Composable
private fun PreviewEmptyStateNotTrusted() {
  CosmicTheme {
    Surface {
      EmptyState(connectivityState = ConnectivityState.NOT_TRUSTED)
    }
  }
}

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun PreviewLoadingState() {
  CosmicTheme {
    Surface {
      LoadingState()
    }
  }
}

/**
 * Mock device for previews
 */
private fun createMockDevice(
  name: String,
  isPaired: Boolean,
  isReachable: Boolean,
  battery: Int? = null
): Device {
  return object : Device() {
    override fun getName() = name
    override fun isPaired() = isPaired
    override fun isReachable() = isReachable
    override fun getBatteryLevel() = battery ?: -1
    override fun getDeviceId() = "mock-${name.lowercase()}"
    override fun getDeviceType() = Device.DeviceType.Phone
  }
}
