/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ConnectionMode
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.discovery.ConnectionManager
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.discovery.DiscoveredService
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.discovery.ServiceDiscovery
import org.cosmic.cosmicconnect.R

/**
 * Activity for setting up connection to Extended Display server.
 */
@AndroidEntryPoint
class ConnectionSetupActivity : ComponentActivity() {

    private lateinit var serviceDiscovery: ServiceDiscovery
    private lateinit var connectionManager: ConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceDiscovery = ServiceDiscovery(this)
        connectionManager = ConnectionManager(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConnectionSetupScreen(
                        serviceDiscovery = serviceDiscovery,
                        connectionManager = connectionManager,
                        onConnect = { service ->
                            startExtendedDisplay(service.host, service.port)
                        },
                        onManualConnect = { host, port ->
                            startExtendedDisplay(host, port)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceDiscovery.startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        serviceDiscovery.stopDiscovery()
    }

    private fun startExtendedDisplay(host: String, port: Int) {
        connectionManager.addRecentConnection(
            DiscoveredService.fromManualEntry(host, port)
        )

        val intent = Intent(this, ExtendedDisplayActivity::class.java).apply {
            putExtra(ExtendedDisplayActivity.EXTRA_SERVER_ADDRESS, host)
            putExtra(ExtendedDisplayActivity.EXTRA_SERVER_PORT, port)
        }
        startActivity(intent)
    }
}

@Composable
private fun ConnectionSetupScreen(
    serviceDiscovery: ServiceDiscovery,
    connectionManager: ConnectionManager,
    onConnect: (DiscoveredService) -> Unit,
    onManualConnect: (String, Int) -> Unit
) {
    val discoveredServices by serviceDiscovery.discoveredServices.collectAsState()
    val isDiscovering by serviceDiscovery.isActive.collectAsState()

    var manualHost by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf(DiscoveredService.DEFAULT_PORT.toString()) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = context.getString(R.string.extended_display_connection_setup_title),
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Discovery section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discovered Servers",
                style = MaterialTheme.typography.titleMedium
            )
            if (isDiscovering) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (discoveredServices.isEmpty() && !isDiscovering) {
            Text(
                text = "No servers found. Make sure your COSMIC Desktop is running Extended Display.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(discoveredServices) { service ->
                DiscoveredServiceCard(
                    service = service,
                    onClick = { onConnect(service) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual connection section
        Text(
            text = "Manual Connection",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = manualHost,
            onValueChange = { manualHost = it },
            label = { Text("Host (IP or hostname)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = manualPort,
            onValueChange = { manualPort = it },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val port = manualPort.toIntOrNull() ?: DiscoveredService.DEFAULT_PORT
                if (manualHost.isNotBlank()) {
                    onManualConnect(manualHost, port)
                } else {
                    Toast.makeText(context, "Please enter a host", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }
    }
}

@Composable
private fun DiscoveredServiceCard(
    service: DiscoveredService,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = service.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${service.host}:${service.port}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
