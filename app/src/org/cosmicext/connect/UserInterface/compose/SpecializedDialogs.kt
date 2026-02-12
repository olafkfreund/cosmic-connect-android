/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.PluginSettingsActivity

/**
 * Dialog for requesting to be the default SMS app.
 */
@Composable
fun DefaultSmsAppDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = {}
) {
    val context = LocalContext.current
    
    ConfirmationDialog(
        title = "Default SMS App",
        message = "To use SMS features, COSMIC Connect needs to be your default SMS app.",
        confirmLabel = "SET DEFAULT",
        onConfirm = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        context.startActivity(intent)
                    }
                }
            } else {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                }
                context.startActivity(intent)
            }
            onConfirm()
        },
        onDismiss = onDismiss
    )
}

/**
 * Dialog for navigating to plugin settings.
 */
@Composable
fun DeviceSettingsDialog(
    deviceId: String,
    pluginKey: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    ConfirmationDialog(
        title = "Plugin Settings",
        message = "Open settings for this plugin?",
        confirmLabel = "OPEN",
        onConfirm = {
            val intent = Intent(context, PluginSettingsActivity::class.java).apply {
                putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, pluginKey)
            }
            context.startActivity(intent)
        },
        onDismiss = onDismiss
    )
}

/**
 * Generic dialog that starts an activity on confirmation.
 */
@Composable
fun StartActivityDialog(
    title: String,
    message: String,
    intentAction: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    ConfirmationDialog(
        title = title,
        message = message,
        confirmLabel = "OPEN",
        onConfirm = {
            val intent = Intent(intentAction)
            context.startActivity(intent)
        },
        onDismiss = onDismiss
    )
}
