/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R
import java.net.InetAddress
import java.net.URI

/**
 * OpenOnPhonePlugin - Receive content from COSMIC Desktop and open on Android
 *
 * This plugin allows COSMIC Desktop users to send URLs, files, or other content
 * to their Android device for opening. All content is subject to user confirmation
 * for security.
 *
 * ## Protocol
 *
 * **Packet Types:**
 * - `cconnect.open.request` - Request to open content
 * - `cconnect.open.response` - Response with success/failure status
 * - `cconnect.open.capability` - Plugin capability announcement
 *
 * **Request Format:**
 * ```json
 * {
 *   "requestId": "uuid",
 *   "url": "https://example.com",
 *   "title": "Optional title"
 * }
 * ```
 *
 * **Response Format:**
 * ```json
 * {
 *   "requestId": "uuid",
 *   "success": true,
 *   "error": "Optional error message"
 * }
 * ```
 *
 * ## Security
 *
 * - All opens require user confirmation via notification
 * - URL scheme validation (http, https, mailto, tel, geo, sms only)
 * - Rejects dangerous schemes (file://, javascript://, data://)
 * - Blocks localhost and internal IPs
 * - Rejects URLs with embedded credentials
 * - Max URL length: 2048 characters
 *
 * ## Behavior
 *
 * When a request is received:
 * 1. Validate URL security
 * 2. Show notification with approve/reject actions
 * 3. User taps "Open" or "Reject"
 * 4. Send response packet with result
 * 5. If approved, open content using Intent.ACTION_VIEW
 *
 * @see OpenOnPhoneReceiver
 */
@LoadablePlugin
class OpenOnPhonePlugin : Plugin() {

    companion object {
        private const val TAG = "OpenOnPhonePlugin"

        /**
         * Allowed URL schemes for security
         */
        val ALLOWED_SCHEMES = setOf("http", "https", "mailto", "tel", "geo", "sms")

        /**
         * Packet types
         */
        const val PACKET_TYPE_OPEN_REQUEST = "cconnect.open.request"
        const val PACKET_TYPE_OPEN_RESPONSE = "cconnect.open.response"
        const val PACKET_TYPE_OPEN_CAPABILITY = "cconnect.open.capability"

        /**
         * Broadcast actions
         */
        const val ACTION_APPROVE_OPEN = "org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin.APPROVE"
        const val ACTION_REJECT_OPEN = "org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin.REJECT"

        /**
         * Intent extras
         */
        const val EXTRA_REQUEST_ID = "requestId"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MIME_TYPE = "mimeType"
        const val EXTRA_DEVICE_ID = "deviceId"

        /**
         * Max URL length for security
         */
        const val MAX_URL_LENGTH = 2048

        /**
         * Blocked IP ranges (private networks, localhost)
         */
        private val BLOCKED_IP_PATTERNS = listOf(
            "^127\\.",          // 127.0.0.0/8 (localhost)
            "^10\\.",           // 10.0.0.0/8 (private)
            "^172\\.(1[6-9]|2[0-9]|3[0-1])\\.", // 172.16.0.0/12 (private)
            "^192\\.168\\.",    // 192.168.0.0/16 (private)
            "^169\\.254\\.",    // 169.254.0.0/16 (link-local)
            "^::1$",            // IPv6 localhost
            "^fe80:",           // IPv6 link-local
            "^fc00:",           // IPv6 private
        ).map { it.toRegex() }
    }

    // ========================================================================
    // State Management
    // ========================================================================

    private var notificationManager: NotificationManager? = null

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_open)

    override val description: String
        get() = context.getString(R.string.pref_plugin_open_desc)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_OPEN_REQUEST, PACKET_TYPE_OPEN_CAPABILITY)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_OPEN_RESPONSE, PACKET_TYPE_OPEN_CAPABILITY)

    // ========================================================================
    // Lifecycle
    // ========================================================================

    override fun onCreate(): Boolean {
        notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        return true
    }

    override fun onDestroy() {
        notificationManager = null
    }

    // ========================================================================
    // Packet Reception
    // ========================================================================

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        when (np.type) {
            PACKET_TYPE_OPEN_REQUEST -> handleOpenRequest(np)
            PACKET_TYPE_OPEN_CAPABILITY -> {
                // Capability announcement, no action needed
                Log.d(TAG, "Received capability announcement from ${device.name}")
            }
            else -> return false
        }
        return true
    }

    /**
     * Handle incoming open request
     *
     * Validates the URL and shows confirmation notification
     */
    private fun handleOpenRequest(np: NetworkPacket) {
        val requestId = np.getString("requestId")
        val url = np.getString("url")
        val title = np.getString("title", "")

        if (requestId == null || url == null) {
            Log.e(TAG, "Invalid open request: missing requestId or url")
            return
        }

        // Validate URL
        val validationError = validateUrl(url)
        if (validationError != null) {
            Log.w(TAG, "URL validation failed: $validationError")
            sendOpenResponse(requestId, false, validationError)
            return
        }

        // Show confirmation notification
        showConfirmationNotification(requestId, url, title)
    }

    // ========================================================================
    // URL Validation
    // ========================================================================

    /**
     * Validate URL for security
     *
     * @param url URL to validate
     * @return Error message if invalid, null if valid
     */
    fun validateUrl(url: String): String? {
        // Check length
        if (url.length > MAX_URL_LENGTH) {
            return "URL too long (max $MAX_URL_LENGTH characters)"
        }

        // Check for null bytes (could indicate injection)
        if (url.contains('\u0000')) {
            return "URL contains invalid characters"
        }

        // Parse URI
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return "Invalid URL format: ${e.message}"
        }

        // Check scheme
        val scheme = uri.scheme?.lowercase()
        if (scheme == null) {
            return "URL missing scheme"
        }

        if (scheme !in ALLOWED_SCHEMES) {
            return "URL scheme '$scheme' not allowed"
        }

        // Check for credentials in URL
        if (uri.userInfo != null) {
            return "URLs with embedded credentials not allowed"
        }

        // For http/https, validate hostname
        if (scheme == "http" || scheme == "https") {
            val host = uri.host
            if (host == null || host.isEmpty()) {
                return "URL missing hostname"
            }

            // Check for blocked IPs
            if (isBlockedHost(host)) {
                return "Cannot open localhost or private network URLs"
            }
        }

        return null
    }

    /**
     * Check if hostname is blocked (localhost, private IPs)
     *
     * @param host Hostname to check
     * @return true if blocked
     */
    private fun isBlockedHost(host: String): Boolean {
        // Check for "localhost" literal
        if (host.equals("localhost", ignoreCase = true)) {
            return true
        }

        // Try to resolve as IP
        val ipAddress = try {
            InetAddress.getByName(host).hostAddress ?: return false
        } catch (e: Exception) {
            // Not an IP, probably a domain name - allow it
            return false
        }

        // Check against blocked patterns
        return BLOCKED_IP_PATTERNS.any { pattern ->
            pattern.containsMatchIn(ipAddress)
        }
    }

    // ========================================================================
    // Notification Management
    // ========================================================================

    /**
     * Show confirmation notification with approve/reject actions
     *
     * @param requestId Unique request ID
     * @param url URL to open
     * @param title Optional title for display
     */
    fun showConfirmationNotification(requestId: String, url: String, title: String) {
        // Create approve intent
        val approveIntent = Intent(context, OpenOnPhoneReceiver::class.java).apply {
            action = ACTION_APPROVE_OPEN
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_DEVICE_ID, device.deviceId)
        }

        val approvePendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode(),
            approveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create reject intent
        val rejectIntent = Intent(context, OpenOnPhoneReceiver::class.java).apply {
            action = ACTION_REJECT_OPEN
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_DEVICE_ID, device.deviceId)
        }

        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            requestId.hashCode() + 1,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val displayTitle = title.ifEmpty {
            context.getString(R.string.open_plugin_notification_title, device.name)
        }

        val displayUrl = if (url.length > 50) {
            url.substring(0, 47) + "..."
        } else {
            url
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.Channels.HIGHPRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentText(displayUrl)
            .setStyle(NotificationCompat.BigTextStyle().bigText(url))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_accept_pairing_24dp,
                context.getString(R.string.open_plugin_action_open),
                approvePendingIntent
            )
            .addAction(
                R.drawable.ic_reject_pairing_24dp,
                context.getString(R.string.open_plugin_action_reject),
                rejectPendingIntent
            )
            .build()

        notificationManager?.notify(requestId.hashCode(), notification)
    }

    /**
     * Hide notification
     *
     * @param requestId Request ID to identify notification
     */
    fun hideNotification(requestId: String) {
        notificationManager?.cancel(requestId.hashCode())
    }

    // ========================================================================
    // Content Opening
    // ========================================================================

    /**
     * Open URL using Intent.ACTION_VIEW
     *
     * @param url URL to open
     * @return true if successful
     */
    fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
            false
        }
    }

    /**
     * Open file using Intent.ACTION_VIEW
     *
     * @param uri File URI
     * @param mimeType MIME type of file
     * @return true if successful
     */
    fun openFile(uri: Uri, mimeType: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: $uri", e)
            false
        }
    }

    // ========================================================================
    // Response Handling
    // ========================================================================

    /**
     * Send open response packet to desktop
     *
     * @param requestId Request ID from original request
     * @param success Whether open was successful
     * @param error Optional error message
     */
    fun sendOpenResponse(requestId: String, success: Boolean, error: String?) {
        val np = NetworkPacket(PACKET_TYPE_OPEN_RESPONSE).apply {
            set("requestId", requestId)
            set("success", success)
            if (error != null) {
                set("error", error)
            }
        }
        device.sendPacket(np)
    }

    // ========================================================================
    // Permissions
    // ========================================================================

    override val requiredPermissions: Array<String>
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        }

    override val permissionExplanation: Int
        get() = R.string.open_plugin_permission_explanation
}
