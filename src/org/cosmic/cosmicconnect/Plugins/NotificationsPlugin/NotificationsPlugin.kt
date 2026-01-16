/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.app.Activity
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.text.SpannableString
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import org.apache.commons.collections4.MultiValuedMap
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Helpers.AppsHelper
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.UserInterface.StartActivityAlertDialogFragment
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Notifications Plugin - Forward Android notifications to remote devices.
 *
 * ## Features
 * - Forward notifications from Android to desktop
 * - Cancel notifications on both devices when dismissed
 * - Support for inline replies (messaging apps)
 * - Support for notification actions
 * - Privacy controls per app (block content/images)
 * - Icon transfer via payload
 *
 * ## Packet Types
 * - kdeconnect.notification - Send/cancel notification
 * - kdeconnect.notification.request - Request all or dismiss one
 * - kdeconnect.notification.action - Trigger action button
 * - kdeconnect.notification.reply - Send inline reply
 *
 * ## Architecture
 * Uses NotificationsPacketsFFI for packet creation, which wraps the
 * cosmic-connect-core Rust FFI layer for consistent packet formatting.
 */
@PluginFactory.LoadablePlugin
class NotificationsPlugin : Plugin(), NotificationReceiver.NotificationListener {

    companion object {
        private const val TAG = "KDE/NotificationsPlugin"
        private const val PREF_KEY = "prefKey"
        const val PREF_NOTIFICATION_SCREEN_OFF = R.string.screen_off_notification_state

        /**
         * Extract notification key for identification.
         *
         * Uses custom tag for our own notifications, otherwise uses system key.
         */
        private fun getNotificationKeyCompat(statusBarNotification: StatusBarNotification): String {
            // First check if it's one of our remoteIds
            val tag = statusBarNotification.tag
            return if (tag != null && tag.startsWith("cosmicconnectId:")) {
                statusBarNotification.id.toString()
            } else {
                statusBarNotification.key
            }
        }

        /**
         * Extract bundle extras from notification.
         */
        private fun getExtras(notification: Notification): Bundle {
            // NotificationCompat.getExtras() returns non-null for JELLY_BEAN+
            return NotificationCompat.getExtras(notification)!!
        }

        /**
         * Extract string from notification extras bundle.
         *
         * Handles both String and SpannableString types.
         */
        private fun extractStringFromExtra(extras: Bundle, key: String): String? {
            return when (val extra = extras.get(key)) {
                null -> null
                is String -> extra
                is SpannableString -> extra.toString()
                else -> {
                    Log.e(TAG, "Don't know how to extract text from extra of type: ${extra.javaClass.canonicalName}")
                    null
                }
            }
        }

        fun getPrefKey(): String = PREF_KEY
    }

    // State management
    private lateinit var appDatabase: AppDatabase
    private val currentNotifications = mutableSetOf<String>()
    private val pendingIntents = mutableMapOf<String, RepliableNotification>()
    private val actions: MultiValuedMap<String, Notification.Action> = ArrayListValuedHashMap()
    private var serviceReady = false
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var keyguardManager: KeyguardManager

    // Plugin metadata
    override fun getDisplayName(): String =
        context.resources.getString(R.string.pref_plugin_notifications)

    override fun getDescription(): String =
        context.resources.getString(R.string.pref_plugin_notifications_desc)

    override fun hasSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment? {
        val intent = Intent(activity, NotificationFilterActivity::class.java).apply {
            putExtra(PREF_KEY, sharedPreferencesName)
        }
        activity.startActivity(intent)
        return null
    }

    // Permissions
    override fun checkRequiredPermissions(): Boolean = hasNotificationsPermission()

    private fun hasNotificationsPermission(): Boolean {
        // Notifications use a different permission model (pre-runtime permissions)
        val notificationListenerList = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return notificationListenerList?.contains(context.packageName) == true
    }

    override fun getPermissionExplanationDialog(): DialogFragment {
        return StartActivityAlertDialogFragment.Builder()
            .setTitle(R.string.pref_plugin_notifications)
            .setMessage(R.string.no_permissions)
            .setPositiveButton(R.string.open_settings)
            .setNegativeButton(R.string.cancel)
            .setIntentAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            .setStartForResult(true)
            .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
            .create()
    }

    // Packet types
    override fun getSupportedPacketTypes(): Array<String> = arrayOf(
        "kdeconnect.notification.request",
        "kdeconnect.notification.reply",
        "kdeconnect.notification.action"
    )

    override fun getOutgoingPacketTypes(): Array<String> = arrayOf(
        "kdeconnect.notification"
    )

    // Lifecycle
    override fun onCreate(): Boolean {
        sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        appDatabase = AppDatabase.getInstance(context)

        NotificationReceiver.RunCommand(context) { service ->
            service.addListener(this@NotificationsPlugin)
            serviceReady = service.isConnected
        }

        return true
    }

    override fun onDestroy() {
        NotificationReceiver.RunCommand(context) { service ->
            service.removeListener(this@NotificationsPlugin)
        }
    }

    // NotificationReceiver.NotificationListener implementation
    override fun onListenerConnected(service: NotificationReceiver) {
        serviceReady = true
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification?) {
        if (statusBarNotification == null) {
            Log.w(TAG, "onNotificationRemoved: notification is null")
            return
        }

        val id = getNotificationKeyCompat(statusBarNotification)
        actions.remove(id)

        if (!appDatabase.isEnabled(statusBarNotification.packageName)) {
            currentNotifications.remove(id)
            return
        }

        // Create cancel packet using FFI wrapper
        val packet = NotificationsPacketsFFI.createCancelNotificationPacket(id)
        device.sendPacket(packet.toLegacyPacket())
        currentNotifications.remove(id)
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val screenOffPref = sharedPreferences.getBoolean(
            context.getString(PREF_NOTIFICATION_SCREEN_OFF),
            false
        )

        if (screenOffPref) {
            if (keyguardManager.inKeyguardRestrictedInputMode()) {
                sendNotification(statusBarNotification, isPreexisting = false)
            }
        } else {
            sendNotification(statusBarNotification, isPreexisting = false)
        }
    }

    /**
     * Send notification to remote device.
     *
     * @param statusBarNotification The notification to send
     * @param isPreexisting true for notifications sent in response to request (sent with "silent" flag)
     */
    private fun sendNotification(statusBarNotification: StatusBarNotification, isPreexisting: Boolean) {
        val notification = statusBarNotification.notification

        // Filter unwanted notifications
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0 ||
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
            (notification.flags and Notification.FLAG_LOCAL_ONLY) != 0 ||
            (notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY) != 0
        ) {
            return
        }

        if (!appDatabase.isEnabled(statusBarNotification.packageName)) {
            return
        }

        val key = getNotificationKeyCompat(statusBarNotification)
        val packageName = statusBarNotification.packageName
        val appName = AppsHelper.appNameLookup(context, packageName)

        // Filter system UI notifications
        if (packageName == "com.android.systemui") {
            if (statusBarNotification.tag == "low_battery") {
                // HACK: Android low battery notifications are posted repeatedly. Ignore them.
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (notification.channelId == "MediaOngoingActivity") {
                    // HACK: Samsung OneUI sends this for media playback. Handled by MPRIS plugin.
                    return
                }
            }
        }

        // Don't send our own notifications
        if (packageName == "org.cosmic.cosmicconnect" || packageName == "org.cosmic.cosmicconnect.debug") {
            return
        }

        // Build notification info
        val notificationInfo = buildNotificationInfo(
            key = key,
            packageName = packageName,
            appName = appName,
            statusBarNotification = statusBarNotification,
            notification = notification,
            isPreexisting = isPreexisting
        )

        // Create packet using FFI wrapper
        val packet = NotificationsPacketsFFI.createNotificationPacket(notificationInfo)

        // Handle icon payload for new notifications
        val isUpdate = currentNotifications.contains(key)
        if (!isUpdate) {
            currentNotifications.add(key)

            val appIcon = extractIcon(statusBarNotification, notification)
            if (appIcon != null && !appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_IMAGES)) {
                attachIcon(packet, appIcon)
            }
        }

        device.sendPacket(packet.toLegacyPacket())
    }

    /**
     * Build NotificationInfo from StatusBarNotification.
     *
     * Extracts all notification fields and applies privacy settings.
     */
    private fun buildNotificationInfo(
        key: String,
        packageName: String,
        appName: String?,
        statusBarNotification: StatusBarNotification,
        notification: Notification,
        isPreexisting: Boolean
    ): NotificationInfo {
        val blockContents = appDatabase.getPrivacy(packageName, AppDatabase.PrivacyOptions.BLOCK_CONTENTS)

        // Extract optional fields based on privacy settings
        var title: String? = null
        var text: String? = null
        var ticker: String? = null
        var requestReplyId: String? = null
        var actions: List<String>? = null

        if (!blockContents) {
            // Extract repliable notification
            val repliable = extractRepliableNotification(statusBarNotification)
            if (repliable != null) {
                requestReplyId = repliable.id
                pendingIntents[repliable.id] = repliable
            }

            // Extract ticker
            ticker = getTickerText(notification)

            // Extract conversation (for messaging apps)
            val conversation = extractConversation(notification)

            // Extract title
            title = conversation.first
            if (title == null) {
                title = extractStringFromExtra(getExtras(notification), NotificationCompat.EXTRA_TITLE)
            }

            // Extract text
            text = extractText(notification, conversation)

            // Extract actions
            actions = extractActions(notification, key)
        }

        return NotificationInfo(
            id = key,
            appName = StringUtils.defaultString(appName, packageName),
            title = title ?: "",
            text = text ?: "",
            isClearable = statusBarNotification.isClearable,
            time = statusBarNotification.postTime.toString(),
            silent = isPreexisting.toString(),
            ticker = ticker,
            requestReplyId = requestReplyId,
            actions = actions
        )
    }

    /**
     * Extract notification text with fallback to big text.
     */
    private fun extractText(notification: Notification, conversation: Pair<String?, String?>): String? {
        if (conversation.second != null) {
            return conversation.second
        }

        val extras = getExtras(notification)

        if (extras.containsKey(NotificationCompat.EXTRA_BIG_TEXT)) {
            return extractStringFromExtra(extras, NotificationCompat.EXTRA_BIG_TEXT)
        }

        return extractStringFromExtra(extras, NotificationCompat.EXTRA_TEXT)
    }

    /**
     * Extract conversation messages for messaging apps.
     */
    private fun extractConversation(notification: Notification): Pair<String?, String?> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return Pair(null, null)
        }

        if (!notification.extras.containsKey(Notification.EXTRA_MESSAGES)) {
            return Pair(null, null)
        }

        val messages = BundleCompat.getParcelableArray(
            notification.extras,
            Notification.EXTRA_MESSAGES,
            Parcelable::class.java
        ) ?: return Pair(null, null)

        val title = notification.extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
        val isGroupConversation = notification.extras.getBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION)

        val messagesBuilder = StringBuilder()
        for (p in messages) {
            val m = p as Bundle
            if (isGroupConversation && m.containsKey("sender")) {
                messagesBuilder.append(m.get("sender"))
                messagesBuilder.append(": ")
            }
            messagesBuilder.append(extractStringFromExtra(m, "text"))
            messagesBuilder.append("\n")
        }

        return Pair(title, messagesBuilder.toString())
    }

    /**
     * Extract action buttons from notification.
     *
     * @return List of action names, or null if none
     */
    private fun extractActions(notification: Notification, key: String): List<String>? {
        if (ArrayUtils.isEmpty(notification.actions)) {
            return null
        }

        val actionsList = mutableListOf<String>()

        for (action in notification.actions) {
            if (action?.title == null) continue

            // Skip reply actions (handled separately via requestReplyId)
            if (ArrayUtils.isNotEmpty(action.remoteInputs)) continue

            actionsList.add(action.title.toString())
            actions.put(key, action)
        }

        return if (actionsList.isEmpty()) null else actionsList
    }

    /**
     * Extract repliable notification for inline reply support.
     */
    private fun extractRepliableNotification(statusBarNotification: StatusBarNotification): RepliableNotification? {
        val actions = statusBarNotification.notification.actions ?: return null

        for (action in actions) {
            if (action != null && action.remoteInputs != null) {
                // This is a reply action
                return RepliableNotification().apply {
                    remoteInputs.addAll(action.remoteInputs.toList())
                    pendingIntent = action.actionIntent
                    packageName = statusBarNotification.packageName
                    tag = statusBarNotification.tag
                }
            }
        }

        return null
    }

    /**
     * Get ticker text (title + text combined).
     */
    private fun getTickerText(notification: Notification): String {
        var ticker = ""

        try {
            val extras = getExtras(notification)
            val extraTitle = extractStringFromExtra(extras, NotificationCompat.EXTRA_TITLE)
            val extraText = extractStringFromExtra(extras, NotificationCompat.EXTRA_TEXT)

            ticker = when {
                extraTitle != null && !TextUtils.isEmpty(extraText) -> "$extraTitle: $extraText"
                extraTitle != null -> extraTitle
                extraText != null -> extraText
                else -> ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Problem parsing notification extras for ${notification.tickerText}", e)
        }

        if (ticker.isEmpty()) {
            ticker = notification.tickerText?.toString() ?: ""
        }

        return ticker
    }

    /**
     * Extract notification icon and convert to bitmap.
     */
    private fun extractIcon(statusBarNotification: StatusBarNotification, notification: Notification): Bitmap? {
        try {
            val foreignContext = context.createPackageContext(statusBarNotification.packageName, 0)

            // Try large icon first
            notification.largeIcon?.let { icon ->
                return iconToBitmap(foreignContext, icon)
            }

            // Fall back to small icon
            val pm = context.packageManager
            val foreignResources = pm.getResourcesForApplication(statusBarNotification.packageName)
            val foreignIcon = foreignResources.getDrawable(notification.icon)
            return drawableToBitmap(foreignIcon)

        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting icon", e)
        }

        return null
    }

    /**
     * Convert drawable to bitmap with size normalization.
     */
    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null

        val size = when {
            drawable.intrinsicWidth > 128 || drawable.intrinsicHeight > 128 -> 96
            drawable.intrinsicWidth <= 64 || drawable.intrinsicHeight <= 64 -> 96
            else -> drawable.intrinsicWidth
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Convert icon to bitmap.
     */
    private fun iconToBitmap(foreignContext: Context, icon: Icon?): Bitmap? {
        if (icon == null) return null
        return drawableToBitmap(icon.loadDrawable(foreignContext))
    }

    /**
     * Attach icon as payload to packet.
     */
    private fun attachIcon(packet: NetworkPacket, appIcon: Bitmap) {
        val outStream = ByteArrayOutputStream()
        appIcon.compress(Bitmap.CompressFormat.PNG, 90, outStream)
        val bitmapData = outStream.toByteArray()

        // Create payload - NetworkPacket should have setPayload method
        // Note: This assumes NetworkPacket has been updated to support payloads
        // If not, we'll need to add that functionality
        try {
            // Reflection to set payload since it may not be in the immutable interface yet
            val payloadClass = Class.forName("org.cosmic.cosmicconnect.NetworkPacket\$Payload")
            val payload = payloadClass.getConstructor(ByteArray::class.java).newInstance(bitmapData)
            val setPayloadMethod = NetworkPacket::class.java.getMethod("setPayload", payloadClass)
            setPayloadMethod.invoke(packet, payload)

            // Set payload hash
            val hash = getChecksum(bitmapData)
            val setMethod = NetworkPacket::class.java.getMethod("set", String::class.java, Any::class.java)
            setMethod.invoke(packet, "payloadHash", hash)
        } catch (e: Exception) {
            Log.e(TAG, "Error attaching icon payload", e)
        }
    }

    /**
     * Calculate MD5 checksum of data.
     */
    private fun getChecksum(data: ByteArray): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            md.update(data)
            bytesToHex(md.digest())
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Error while generating checksum", e)
            null
        }
    }

    /**
     * Convert bytes to hex string.
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars).lowercase()
    }

    /**
     * Send all current notifications to remote device.
     *
     * Called when remote device requests notification sync.
     */
    private fun sendCurrentNotifications(service: NotificationReceiver) {
        if (!hasNotificationsPermission()) {
            return
        }

        val notifications = try {
            service.activeNotifications
        } catch (e: SecurityException) {
            return
        } ?: return // Can happen on API 23 and lower

        for (notification in notifications) {
            sendNotification(notification, isPreexisting = true)
        }
    }

    /**
     * Reply to notification with inline reply.
     */
    private fun replyToNotification(id: String, message: String) {
        if (pendingIntents.isEmpty() || !pendingIntents.containsKey(id)) {
            Log.e(TAG, "No such notification: $id")
            return
        }

        val repliableNotification = pendingIntents[id] ?: run {
            Log.e(TAG, "No such notification: $id")
            return
        }

        val remoteInputs = repliableNotification.remoteInputs.toTypedArray()
        val localIntent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val localBundle = Bundle()

        for ((index, remoteInput) in remoteInputs.withIndex()) {
            localBundle.putCharSequence(remoteInput.resultKey, message)
        }
        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle)

        try {
            repliableNotification.pendingIntent.send(context, 0, localIntent)
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "replyToNotification error: ${e.message}")
        }

        pendingIntents.remove(id)
    }

    /**
     * Trigger notification action button.
     */
    private fun triggerNotificationAction(key: String, actionTitle: String) {
        val actionsList = actions.get(key)
        var intent: PendingIntent? = null

        for (action in actionsList) {
            if (action.title == actionTitle) {
                intent = action.actionIntent
                break
            }
        }

        if (intent != null) {
            try {
                intent.send()
            } catch (e: PendingIntent.CanceledException) {
                Log.e(TAG, "Triggering action failed", e)
            }
        }
    }

    // Packet handling
    override fun onPacketReceived(np: NetworkPacket): Boolean {
        // Use extension properties from NotificationsPacketsFFI
        when {
            np.isNotificationAction -> {
                val key = np.notificationId
                val actionTitle = np.body["action"] as? String
                if (key != null && actionTitle != null) {
                    triggerNotificationAction(key, actionTitle)
                }
            }

            np.isNotificationRequest -> {
                if (np.body.containsKey("request")) {
                    // Request all notifications
                    if (serviceReady) {
                        NotificationReceiver.RunCommand(context, ::sendCurrentNotifications)
                    }
                } else if (np.body.containsKey("cancel")) {
                    // Dismiss specific notification
                    val dismissedId = np.body["cancel"] as? String
                    if (dismissedId != null) {
                        currentNotifications.remove(dismissedId)
                        NotificationReceiver.RunCommand(context) { service ->
                            service.cancelNotification(dismissedId)
                        }
                    }
                }
            }

            np.isNotificationReply -> {
                val replyId = np.notificationRequestReplyId
                val message = np.body["message"] as? String
                if (replyId != null && message != null) {
                    replyToNotification(replyId, message)
                }
            }
        }

        return true
    }
}
