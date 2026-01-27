/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.UserInterface

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.BackgroundService
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.navigation.CosmicNavGraph
import org.cosmic.cosmicconnect.UserInterface.compose.navigation.Screen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.MainScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), OnSharedPreferenceChangeListener {

    @Inject lateinit var deviceRegistry: DeviceRegistry
    @Inject lateinit var deviceHelper: DeviceHelper

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceHelper.initializeDeviceId()

        setContent {
            val navController = rememberNavController()
            CosmicTheme(context = this) {
                MainScreen(
                    viewModel = viewModel,
                    onNavigateToPairing = { navController.navigate(Screen.Pairing) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings) },
                    onNavigateToAbout = { navController.navigate(Screen.About) },
                    onNavigateToDevice = { deviceId ->
                        navController.navigate(Screen.deviceDetail(deviceId))
                    }
                ) {
                    CosmicNavGraph(
                        navController = navController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)

        val missingPermissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionResult = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (permissionResult != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 0)
        }
    }

    override fun onStart() {
        super.onStart()
        BackgroundService.Start(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (DeviceHelper.KEY_DEVICE_NAME_PREFERENCE == key) {
            viewModel.refreshDeviceName()
            BackgroundService.ForceRefreshConnections(this)
        }
    }

    fun onDeviceSelected(deviceId: String?, fromDeviceList: Boolean = false) {
        // This method is used by legacy fragments. In pure Compose it might not be needed,
        // but keeping it for compatibility if any fragments are still alive.
        viewModel.selectDevice(deviceId)
    }

    companion object {
        const val EXTRA_DEVICE_ID = "deviceId"
        const val PAIR_REQUEST_STATUS = "pair_req_status"
        const val PAIRING_ACCEPTED = "accepted"
        const val PAIRING_REJECTED = "rejected"
        const val PAIRING_PENDING = "pending"
        const val RESULT_NEEDS_RELOAD = 100
        const val RESULT_NOTIFICATIONS_ENABLED = 101
        const val FLAG_FORCE_OVERVIEW = "forceOverview"
    }
}