/*
 * SPDX-FileCopyrightText: 2014 Achilleas Koutsou <achilleas.k@gmail.com>
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.UserInterface

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.CustomDevicesViewModel

@AndroidEntryPoint
class CustomDevicesActivity : ComponentActivity() {

    private val viewModel: CustomDevicesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CosmicTheme(context = this) {
                CustomDevicesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val IP_DELIM = ","

        @JvmStatic
        fun getCustomDeviceList(context: android.content.Context): ArrayList<org.cosmic.cosmicconnect.DeviceHost> {
            val deviceListPrefs = org.cosmic.cosmicconnect.Helpers.PreferenceDataStore.getCustomDeviceListSync(context)
            val list = deserializeIpList(deviceListPrefs)
            list.sortBy { it.toString() }
            return list
        }

        private fun deserializeIpList(serialized: String): ArrayList<org.cosmic.cosmicconnect.DeviceHost> {
            val ipList = ArrayList<org.cosmic.cosmicconnect.DeviceHost>()
            if (serialized.isNotEmpty()) {
                for (ip in serialized.split(IP_DELIM)) {
                    val deviceHost = org.cosmic.cosmicconnect.DeviceHost.toDeviceHostOrNull(ip)
                    if (deviceHost != null) {
                        ipList.add(deviceHost)
                    }
                }
            }
            return ipList
        }
    }
}
