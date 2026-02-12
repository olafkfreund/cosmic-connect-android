/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.Html
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Core.getString
import org.cosmic.cosmicconnect.Core.getInt
import org.cosmic.cosmicconnect.Core.getBoolean
import org.cosmic.cosmicconnect.Core.getStringOrNull
import org.cosmic.cosmicconnect.Core.getJSONArray
import org.cosmic.cosmicconnect.Core.contains
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import org.cosmic.cosmicconnect.R

class ReceiveNotificationsPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): ReceiveNotificationsPlugin
    }
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_receive_notifications)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_receive_notifications_desc)

    override val isEnabledByDefault: Boolean = false

    override fun onCreate(): Boolean {
        val packet = ReceiveNotificationsPacketsFFI.createNotificationRequestPacket()
        device.sendPacket(TransferPacket(packet))
        return true
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if ("appName" !in np || "id" !in np) {
            Log.e(TAG, "Received notification packet lacks required properties")
            return true
        }

        val notificationIdStr = np.body["id"]?.toString() ?: ""
        val notifId = notificationIdStr.hashCode()

        // Handle cancellation packets from desktop
        if (np.getBoolean("isCancel", false)) {
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.cancel(NOTIFICATION_TAG, notifId)
            return true
        }

        // Need at least ticker or text for content
        if ("ticker" !in np && "text" !in np) {
            Log.e(TAG, "Received notification packet lacks content (ticker/text)")
            return true
        }

        // Handle both boolean true and string "true" for silent
        val silentValue = np.body["silent"]
        val isSilent = when (silentValue) {
            is Boolean -> silentValue
            is String -> silentValue.equals("true", ignoreCase = true)
            else -> false
        }
        if (isSilent) return true

        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return true

        // Title: prefer "title", fall back to "appName"
        val title = np.getStringOrNull("title") ?: np.getString("appName")

        // Content text: prefer "text", fall back to "ticker"
        val contentText = np.getStringOrNull("text") ?: np.getString("ticker")

        // App name as subtext
        val appName = np.getString("appName")

        // --- Image: try base64 imageData from body, then payload stream ---
        var largeIcon: Bitmap? = null
        val imageDataBase64 = np.getStringOrNull("imageData")
        if (!imageDataBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(imageDataBase64, Base64.DEFAULT)
                largeIcon = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode imageData from body", e)
            }
        }

        // Fall back to payload stream
        if (largeIcon == null) {
            val payload = tp.payload
            if (payload != null && payload.payloadSize != 0L) {
                try {
                    largeIcon = BitmapFactory.decodeStream(payload.inputStream)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to decode image from payload stream", e)
                } finally {
                    payload.close()
                }
            }
        }

        // Scale large icon if needed
        if (largeIcon != null) {
            val width = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
            val height = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
            if (largeIcon.width > width || largeIcon.height > height) {
                largeIcon = largeIcon.scale(width, height, false)
            }
        }

        // Map desktop urgency (0=low, 1=normal, 2=critical) to Android priority
        val priority = when (np.getInt("urgency", 1)) {
            0 -> NotificationCompat.PRIORITY_LOW
            2 -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        // Map desktop category to Android notification category
        val category = np.getStringOrNull("category")
        val androidCategory = when (category) {
            "email" -> NotificationCompat.CATEGORY_EMAIL
            "call" -> NotificationCompat.CATEGORY_CALL
            "msg", "message" -> NotificationCompat.CATEGORY_MESSAGE
            "im", "im.received" -> NotificationCompat.CATEGORY_MESSAGE
            "alarm" -> NotificationCompat.CATEGORY_ALARM
            "reminder" -> NotificationCompat.CATEGORY_REMINDER
            "device" -> NotificationCompat.CATEGORY_SYSTEM
            "network" -> NotificationCompat.CATEGORY_STATUS
            else -> null
        }

        // Rich body (HTML rendering)
        val richBody = np.getStringOrNull("richBody")
        val styledText: CharSequence = if (!richBody.isNullOrEmpty()) {
            Html.fromHtml(richBody, Html.FROM_HTML_MODE_COMPACT)
        } else {
            contentText
        }

        // Content intent: open app
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete intent: sync dismissal to desktop
        val dismissIntent = Intent(NotificationActionReceiver.ACTION_NOTIFICATION_DISMISSED).apply {
            setPackage(context.packageName)
            putExtra(NotificationActionReceiver.EXTRA_DEVICE_ID, device.deviceId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationIdStr)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, notifId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NotificationHelper.Channels.RECEIVENOTIFICATION)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deletePendingIntent)
            .setTicker(np.getStringOrNull("ticker") ?: contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(priority)
            .setSubText(appName)
            .apply { androidCategory?.let { setCategory(it) } }
            .setStyle(NotificationCompat.BigTextStyle().bigText(styledText))

        // Action buttons from desktop
        val actionButtons = np.getJSONArray("actionButtons")
        if (actionButtons != null) {
            for (i in 0 until minOf(actionButtons.length(), MAX_ACTIONS)) {
                try {
                    val actionObj = actionButtons.getJSONObject(i)
                    val actionId = actionObj.getString("id")
                    val actionLabel = actionObj.getString("label")

                    val actionIntent = Intent(NotificationActionReceiver.ACTION_NOTIFICATION_ACTION).apply {
                        setPackage(context.packageName)
                        putExtra(NotificationActionReceiver.EXTRA_DEVICE_ID, device.deviceId)
                        putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationIdStr)
                        putExtra(NotificationActionReceiver.EXTRA_ACTION_ID, actionId)
                    }
                    val actionPendingIntent = PendingIntent.getBroadcast(
                        context, (notificationIdStr + actionId).hashCode(),
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.addAction(0, actionLabel, actionPendingIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse action button at index $i", e)
                }
            }
        }

        notificationManager.notify(NOTIFICATION_TAG, notifId, builder.build())

        return true
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_NOTIFICATION)

    override val outgoingPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_NOTIFICATION_REQUEST,
        PACKET_TYPE_NOTIFICATION,
        PACKET_TYPE_NOTIFICATION_ACTION
    )

    override val requiredPermissions: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf()
    }

    override val permissionExplanation: Int = R.string.receive_notifications_permission_explanation

    companion object {
        private const val TAG = "ReceiveNotificationsPlugin"
        private const val NOTIFICATION_TAG = "cosmicconnect"
        private const val MAX_ACTIONS = 3
        private const val PACKET_TYPE_NOTIFICATION = "cconnect.notification"
        private const val PACKET_TYPE_NOTIFICATION_REQUEST = "cconnect.notification.request"
        private const val PACKET_TYPE_NOTIFICATION_ACTION = "cconnect.notification.action"
    }
}
