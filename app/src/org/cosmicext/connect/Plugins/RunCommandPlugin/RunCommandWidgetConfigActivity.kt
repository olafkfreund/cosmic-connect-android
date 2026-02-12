/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmicext.connect.Plugins.RunCommandPlugin

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import org.cosmicext.connect.CosmicExtConnect
import org.cosmicext.connect.Device
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicTheme

class RunCommandWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        setResult(RESULT_CANCELED) // Default result

        appWidgetId = intent.extras?.getInt(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CosmicTheme(context = this) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(getString(R.string.pref_plugin_runcommand)) }
                        )
                    }
                ) { padding ->
                    val pairedDevices = remember {
                        CosmicExtConnect.getInstance().devices.values
                            .filter { it.isPaired }
                            .toList()
                    }

                    if (pairedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(getString(R.string.device_list_empty))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(padding)) {
                            items(pairedDevices) { device ->
                                ListItem(
                                    headlineContent = { Text(device.name) },
                                    modifier = Modifier.clickable { deviceClicked(device) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun deviceClicked(device: Device) {
        val deviceId = device.deviceId
        saveWidgetDeviceIdPref(this, appWidgetId, deviceId)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent()
        resultValue.putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

private const val PREFS_NAME = "org.cosmicext.connect.WidgetProvider"
private const val PREF_PREFIX_KEY = "appwidget_"

internal fun saveWidgetDeviceIdPref(context: Context, appWidgetId: Int, deviceName: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(PREF_PREFIX_KEY + appWidgetId, deviceName)
    }
}

internal fun loadWidgetDeviceIdPref(context: Context, appWidgetId: Int): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
}

internal fun deleteWidgetDeviceIdPref(context: Context, appWidgetId: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        remove(PREF_PREFIX_KEY + appWidgetId)
    }
}
