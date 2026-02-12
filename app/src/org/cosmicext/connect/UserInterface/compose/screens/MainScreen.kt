/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.MainViewModel
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicNavigationDrawer
import org.cosmicext.connect.UserInterface.compose.NavigationDestination
import org.cosmicext.connect.UserInterface.compose.Spacing
import org.cosmicext.connect.UserInterface.compose.getDeviceIcon

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToPairing: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDevice: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val destinations = mutableListOf<NavigationDestination>()
    
    // Fixed destinations
    destinations.add(NavigationDestination(
        id = "pairing",
        label = stringResource(R.string.pair_new_device),
        icon = R.drawable.ic_action_content_add_circle_outline_32dp
    ))
    
    // Paired devices
    uiState.devices.filter { it.isPaired }.forEach { device ->
        destinations.add(NavigationDestination(
            id = "device_${device.deviceId}",
            label = device.name,
            icon = getDeviceIcon(device.deviceType.toString())
        ))
    }

    CosmicNavigationDrawer(
        destinations = destinations,
        selectedDestination = if (uiState.selectedDeviceId != null) "device_${uiState.selectedDeviceId}" else "pairing",
        onDestinationSelected = { id ->
            if (id == "pairing") {
                viewModel.selectDevice(null)
                onNavigateToPairing()
            } else if (id.startsWith("device_")) {
                val deviceId = id.removePrefix("device_")
                viewModel.selectDevice(deviceId)
                onNavigateToDevice(deviceId)
            }
        },
        drawerState = drawerState,
        header = {
            Column(modifier = Modifier.padding(Spacing.large)) {
                Image(
                    painter = painterResource(getDeviceIcon(uiState.myDeviceType)),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = uiState.myDeviceName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = uiState.myDeviceType.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))
        },
        footer = {
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.small))
            NavigationDestination(
                id = "settings",
                label = stringResource(R.string.settings),
                icon = R.drawable.ic_settings_white_32dp
            ).let { dest ->
                org.cosmicext.connect.UserInterface.compose.SimpleListItem(
                    text = dest.label,
                    icon = dest.icon,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    }
                )
            }
            NavigationDestination(
                id = "about",
                label = stringResource(R.string.about),
                icon = R.drawable.ic_baseline_info_24
            ).let { dest ->
                org.cosmicext.connect.UserInterface.compose.SimpleListItem(
                    text = dest.label,
                    icon = dest.icon,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToAbout()
                    }
                )
            }
        },
        content = content
    )
}
