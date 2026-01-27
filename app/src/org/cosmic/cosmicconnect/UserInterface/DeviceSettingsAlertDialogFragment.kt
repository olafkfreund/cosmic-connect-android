/*
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.os.Bundle
import androidx.compose.runtime.Composable
import org.cosmic.cosmicconnect.UserInterface.compose.DeviceSettingsDialog

class DeviceSettingsAlertDialogFragment : AlertDialogFragment() {

    private var pluginKey: String? = null
    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        pluginKey = args.getString(KEY_PLUGIN_KEY)
        deviceId = args.getString(KEY_DEVICE_ID)
    }

    @Composable
    override fun DialogContent() {
        if (deviceId != null && pluginKey != null) {
            DeviceSettingsDialog(
                deviceId = deviceId!!,
                pluginKey = pluginKey!!,
                onDismiss = { dismiss() }
            )
        } else {
            dismiss()
        }
    }

    class Builder : AlertDialogFragment.AbstractBuilder<Builder, DeviceSettingsAlertDialogFragment>() {
        override fun getThis(): Builder = this

        fun setPluginKey(pluginKey: String): Builder {
            args.putString(KEY_PLUGIN_KEY, pluginKey)
            return getThis()
        }

        fun setDeviceId(deviceId: String): Builder {
            args.putString(KEY_DEVICE_ID, deviceId)
            return getThis()
        }

        override fun createFragment(): DeviceSettingsAlertDialogFragment = DeviceSettingsAlertDialogFragment()
    }

    companion object {
        private const val KEY_PLUGIN_KEY = "PluginKey"
        private const val KEY_DEVICE_ID = "DeviceId"
    }
}