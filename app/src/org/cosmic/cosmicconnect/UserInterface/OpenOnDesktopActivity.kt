/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.SharePlugin.SharePlugin
import org.cosmic.cosmicconnect.R
import javax.inject.Inject

/**
 * Transparent activity that handles "Open on Desktop" share intents.
 *
 * This activity provides ShareSheet integration, allowing users to share
 * content directly to paired COSMIC Desktop devices via Android's native
 * share menu.
 *
 * Behavior:
 * - If exactly one device is paired and connected: shares immediately
 * - If multiple devices are available: delegates to ShareActivity for device selection
 * - If no devices are available: shows error message
 *
 * Supports:
 * - Text sharing (URLs, plain text)
 * - Single file sharing (images, documents, etc.)
 * - Multiple file sharing
 */
@AndroidEntryPoint
class OpenOnDesktopActivity : ComponentActivity() {

    @Inject
    lateinit var deviceRegistry: DeviceRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SEND -> handleSend()
            Intent.ACTION_SEND_MULTIPLE -> handleSendMultiple()
            else -> {
                Log.w(TAG, "OpenOnDesktopActivity called with unsupported action: ${intent?.action}")
                finish()
            }
        }
    }

    private fun handleSend() {
        val mimeType = intent.type
        if (mimeType == null) {
            Log.w(TAG, "No MIME type provided for ACTION_SEND")
            Toast.makeText(this, R.string.share_error_no_mime_type, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        when {
            mimeType == "text/plain" -> handleTextShare()
            mimeType.startsWith("image/") -> handleFileShare()
            mimeType.startsWith("video/") -> handleFileShare()
            mimeType.startsWith("audio/") -> handleFileShare()
            mimeType.startsWith("application/") -> handleFileShare()
            else -> handleFileShare() // Default to file sharing for unknown types
        }
    }

    private fun handleSendMultiple() {
        val mimeType = intent.type
        if (mimeType == null) {
            Log.w(TAG, "No MIME type provided for ACTION_SEND_MULTIPLE")
            Toast.makeText(this, R.string.share_error_no_mime_type, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        shareToDeviceOrPrompt()
    }

    private fun handleTextShare() {
        shareToDeviceOrPrompt()
    }

    private fun handleFileShare() {
        shareToDeviceOrPrompt()
    }

    /**
     * Share to a device if exactly one is available, otherwise show device selection.
     */
    private fun shareToDeviceOrPrompt() {
        val devices = getAvailableDevices()

        when (devices.size) {
            0 -> {
                // No devices available
                Toast.makeText(
                    this,
                    R.string.open_on_desktop_no_devices,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            1 -> {
                // Exactly one device - share immediately
                val device = devices[0]
                shareToDevice(device)
            }
            else -> {
                // Multiple devices - delegate to ShareActivity for selection
                delegateToShareActivity()
            }
        }
    }

    /**
     * Get list of reachable devices with SharePlugin enabled.
     */
    private fun getAvailableDevices(): List<Device> {
        return deviceRegistry.devices.values
            .filter { device ->
                device.isReachable &&
                device.isPaired &&
                device.getPlugin(SharePlugin::class.java) != null
            }
    }

    /**
     * Share to a specific device.
     */
    private fun shareToDevice(device: Device) {
        val plugin = deviceRegistry.getDevicePlugin(device.deviceId, SharePlugin::class.java)

        if (plugin != null) {
            Log.d(TAG, "Sharing to device: ${device.name}")
            plugin.share(intent)

            // Show feedback
            Toast.makeText(
                this,
                getString(R.string.open_on_desktop_sharing_to, device.name),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.e(TAG, "SharePlugin not available for device: ${device.deviceId}")
            Toast.makeText(
                this,
                R.string.open_on_desktop_error,
                Toast.LENGTH_SHORT
            ).show()
        }

        finish()
    }

    /**
     * Delegate to ShareActivity for device selection when multiple devices available.
     */
    private fun delegateToShareActivity() {
        val shareIntent = Intent(this, org.cosmic.cosmicconnect.Plugins.SharePlugin.ShareActivity::class.java).apply {
            action = intent.action
            type = intent.type
            putExtras(intent.extras ?: Bundle())

            // Copy data/clip data
            intent.data?.let { data = it }
            intent.clipData?.let { clipData = it }

            // Preserve flags for URI permissions
            flags = intent.flags
        }

        startActivity(shareIntent)
        finish()
    }

    companion object {
        private const val TAG = "OpenOnDesktopActivity"
    }
}
