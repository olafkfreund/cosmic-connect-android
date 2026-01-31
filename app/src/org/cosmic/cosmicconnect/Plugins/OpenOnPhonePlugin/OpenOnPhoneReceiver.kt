/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.cosmic.cosmicconnect.BackgroundService
import org.cosmic.cosmicconnect.Device

/**
 * OpenOnPhoneReceiver - Handle notification actions for open requests
 *
 * This BroadcastReceiver handles the approve/reject actions from the
 * confirmation notification shown by OpenOnPhonePlugin.
 *
 * ## Actions
 *
 * **APPROVE:** Open the requested URL and send success response
 * **REJECT:** Dismiss notification and send rejection response
 *
 * ## Flow
 *
 * 1. User taps notification action
 * 2. Receiver extracts requestId, url, deviceId from intent
 * 3. Gets device and plugin instances from BackgroundService
 * 4. Calls plugin methods to open URL and send response
 * 5. Hides notification
 *
 * @see OpenOnPhonePlugin
 */
class OpenOnPhoneReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "OpenOnPhoneReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val requestId = intent.getStringExtra(OpenOnPhonePlugin.EXTRA_REQUEST_ID) ?: return
        val deviceId = intent.getStringExtra(OpenOnPhonePlugin.EXTRA_DEVICE_ID) ?: return

        // Get device from BackgroundService
        val device = BackgroundService.RunCommand(context, { service ->
            service.getDevice(deviceId)
        }) ?: run {
            Log.e(TAG, "Device not found: $deviceId")
            return
        }

        // Get plugin instance
        val plugin = device.getPlugin(OpenOnPhonePlugin::class.java)
        if (plugin == null || !plugin.isDeviceInitialized) {
            Log.e(TAG, "Plugin not available for device: $deviceId")
            return
        }

        when (action) {
            OpenOnPhonePlugin.ACTION_APPROVE_OPEN -> {
                handleApprove(plugin, requestId, intent)
            }
            OpenOnPhonePlugin.ACTION_REJECT_OPEN -> {
                handleReject(plugin, requestId)
            }
        }

        // Hide notification
        plugin.hideNotification(requestId)
    }

    /**
     * Handle approve action - open URL and send success response
     */
    private fun handleApprove(plugin: OpenOnPhonePlugin, requestId: String, intent: Intent) {
        val url = intent.getStringExtra(OpenOnPhonePlugin.EXTRA_URL)
        if (url == null) {
            Log.e(TAG, "URL missing from approve intent")
            plugin.sendOpenResponse(requestId, false, "URL missing")
            return
        }

        // Re-validate URL for security (in case state changed)
        val validationError = plugin.validateUrl(url)
        if (validationError != null) {
            Log.w(TAG, "URL validation failed on approve: $validationError")
            plugin.sendOpenResponse(requestId, false, validationError)
            return
        }

        // Open URL
        val success = plugin.openUrl(url)

        if (success) {
            Log.i(TAG, "Successfully opened URL: $url")
            plugin.sendOpenResponse(requestId, true, null)
        } else {
            Log.e(TAG, "Failed to open URL: $url")
            plugin.sendOpenResponse(requestId, false, "Failed to open URL")
        }
    }

    /**
     * Handle reject action - send rejection response
     */
    private fun handleReject(plugin: OpenOnPhonePlugin, requestId: String) {
        Log.i(TAG, "User rejected open request: $requestId")
        plugin.sendOpenResponse(requestId, false, "User rejected")
    }
}
