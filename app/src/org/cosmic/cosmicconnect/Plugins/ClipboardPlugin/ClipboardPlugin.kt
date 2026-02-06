/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ClipboardPlugin

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * ClipboardPlugin - Sync clipboard between Android and COSMIC Desktop
 *
 * This plugin enables automatic clipboard synchronization using the COSMIC Connect protocol v7.
 * It uses FFI (Foreign Function Interface) for type-safe packet creation via cosmic-connect-core.
 *
 * ## Packet Types
 *
 * 1. **cconnect.clipboard** - Standard clipboard update (no timestamp)
 *    ```json
 *    {
 *      "content": "clipboard text"
 *    }
 *    ```
 *
 * 2. **cconnect.clipboard.connect** - Connection sync with timestamp
 *    ```json
 *    {
 *      "content": "clipboard text",
 *      "timestamp": 1704067200000
 *    }
 *    ```
 *
 * ## Sync Loop Prevention
 *
 * Connect packets include a timestamp (milliseconds since epoch) for sync loop prevention.
 * On connection, only the device with the newer clipboard updates the other device.
 * Standard update packets do NOT include timestamps to avoid infinite sync loops.
 *
 * ## FFI Integration
 *
 * This plugin uses ClipboardPacketsFFI wrapper for type-safe packet creation:
 * - `createClipboardUpdate(content)` - Standard clipboard update
 * - `createClipboardConnect(content, timestamp)` - Connect sync with timestamp
 *
 * Extension properties for packet inspection:
 * - `isClipboardUpdate` - Check if packet is a standard update
 * - `isClipboardConnect` - Check if packet is a connect packet
 * - `clipboardContent` - Extract clipboard content
 * - `clipboardTimestamp` - Extract timestamp from connect packet
 */
class ClipboardPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): ClipboardPlugin
    }

    companion object {
        /**
         * Standard clipboard update packet type (no timestamp)
         */
        private const val PACKET_TYPE_CLIPBOARD = "cconnect.clipboard"

        /**
         * Clipboard connect packet type (with timestamp for sync loop prevention)
         */
        private const val PACKET_TYPE_CLIPBOARD_CONNECT = "cconnect.clipboard.connect"
    }

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_clipboard)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_clipboard_desc)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_CLIPBOARD, PACKET_TYPE_CLIPBOARD_CONNECT)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_CLIPBOARD, PACKET_TYPE_CLIPBOARD_CONNECT)

    // ========================================================================
    // Packet Reception
    // ========================================================================

    override fun onPacketReceived(np: org.cosmic.cosmicconnect.NetworkPacket): Boolean {
        // Convert legacy packet to immutable for type-safe inspection
        val networkPacket = NetworkPacket.fromLegacy(np)

        return when {
            // Handle standard clipboard update (no timestamp)
            networkPacket.isClipboardUpdate -> {
                val content = networkPacket.clipboardContent
                if (content != null) {
                    ClipboardListener.instance(context).setText(content)
                    true
                } else {
                    false
                }
            }

            // Handle clipboard connect with timestamp (sync loop prevention)
            networkPacket.isClipboardConnect -> {
                val timestamp = networkPacket.clipboardTimestamp
                val localTimestamp = ClipboardListener.instance(context).updateTimestamp

                // Ignore if timestamp is null, 0 (unknown), or older than local
                if (timestamp == null || timestamp == 0L || timestamp < localTimestamp) {
                    return false
                }

                val content = networkPacket.clipboardContent
                if (content != null) {
                    ClipboardListener.instance(context).setText(content)
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }

    // ========================================================================
    // Clipboard Propagation
    // ========================================================================

    /**
     * Observer callback for clipboard changes
     */
    private val observer: ClipboardListener.ClipboardObserver = object : ClipboardListener.ClipboardObserver {
        override fun clipboardChanged(content: String) {
            propagateClipboard(content)
        }
    }

    /**
     * Propagate clipboard change to remote device
     *
     * Creates a standard clipboard update packet (no timestamp) and sends it to
     * the paired device. Standard updates do not include timestamps to avoid
     * infinite sync loops.
     *
     * @param content Clipboard text content
     */
    private fun propagateClipboard(content: String) {
        // Create packet using FFI wrapper
        val packet = ClipboardPacketsFFI.createClipboardUpdate(content)

        // Send to remote device (convert to legacy packet)
        device.sendPacket(packet.toLegacyPacket())
    }

    /**
     * Send clipboard state on device connection
     *
     * Creates a connect packet with timestamp for sync loop prevention.
     * Only sends if clipboard has been initialized (not null).
     *
     * The timestamp allows devices to determine which clipboard is newer and
     * prevents infinite sync loops on connection.
     */
    private fun sendConnectPacket() {
        val content = ClipboardListener.instance(context).currentContent
            ?: return // Don't send if clipboard not initialized

        // Create connect packet with timestamp using FFI wrapper
        val timestamp = ClipboardListener.instance(context).updateTimestamp
        val packet = ClipboardPacketsFFI.createClipboardConnect(content, timestamp)

        // Send to remote device (convert to legacy packet)
        device.sendPacket(packet.toLegacyPacket())
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    override fun onCreate(): Boolean {
        // Register for clipboard change notifications
        ClipboardListener.instance(context).registerObserver(observer)

        // Send current clipboard state on connection
        sendConnectPacket()

        return true
    }

    override fun onDestroy() {
        // Unregister clipboard observer
        ClipboardListener.instance(context).removeObserver(observer)
    }

    // ========================================================================
    // UI Integration
    // ========================================================================

    override fun getUiButtons(): List<PluginUiButton> {
        // Show button only on Android 10+ when logs permission denied
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && canAccessLogs()) {
            listOf(
                PluginUiButton(
                    context.getString(R.string.send_clipboard),
                    R.drawable.ic_baseline_content_paste_24
                ) {
                    userInitiatedSendClipboard()
                }
            )
        } else {
            emptyList()
        }
    }

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> {
        // Show menu entry on Android 10+ when logs permission granted
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P && !canAccessLogs()) {
            listOf(
                PluginUiMenuEntry(
                    context.getString(R.string.send_clipboard)
                ) {
                    userInitiatedSendClipboard()
                }
            )
        } else {
            emptyList()
        }
    }

    /**
     * Handle user-initiated clipboard send
     *
     * Reads current clipboard content and propagates it to the remote device.
     * Shows a toast notification to confirm the action.
     */
    private fun userInitiatedSendClipboard() {
        val clipboardManager = ContextCompat.getSystemService(context, ClipboardManager::class.java)
            ?: return

        if (!clipboardManager.hasPrimaryClip()) {
            return
        }

        val item = clipboardManager.primaryClip?.getItemAt(0) ?: return
        val content = item.coerceToText(context).toString()

        propagateClipboard(content)

        Toast.makeText(
            context,
            R.string.pref_plugin_clipboard_sent,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Check if READ_LOGS permission is denied
     *
     * Used to determine which UI element to show (button vs menu entry).
     * On Android 10+, READ_LOGS permission is required for background clipboard access.
     *
     * @return true if permission is denied (show button), false if granted (show menu entry)
     */
    private fun canAccessLogs(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_LOGS
        ) == PackageManager.PERMISSION_DENIED
    }
}
