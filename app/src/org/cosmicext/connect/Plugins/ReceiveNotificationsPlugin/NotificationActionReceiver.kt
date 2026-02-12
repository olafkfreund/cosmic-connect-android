/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.ReceiveNotificationsPlugin

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import javax.inject.Inject

/**
 * Handles notification action button taps and dismissal events for
 * desktop notifications mirrored to Android.
 *
 * When the user taps an action button on a mirrored notification,
 * this receiver sends a `cconnect.notification.action` packet back
 * to the desktop so the action is executed there.
 *
 * When a notification is dismissed (swiped away), this receiver sends
 * a `cconnect.notification` packet with `isCancel: true` so the
 * desktop can dismiss it too.
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var deviceRegistry: DeviceRegistry

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_NOTIFICATION_DISMISSED -> handleDismissal(intent)
            ACTION_NOTIFICATION_ACTION -> handleAction(intent)
            else -> Log.d(TAG, "Unhandled action: ${intent.action}")
        }
    }

    private fun handleDismissal(intent: Intent) {
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return

        Log.d(TAG, "Notification dismissed: $notificationId for device $deviceId")

        val device = deviceRegistry.getDevice(deviceId) ?: run {
            Log.w(TAG, "Device not found: $deviceId")
            return
        }

        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_NOTIFICATION,
            body = mapOf(
                "id" to notificationId,
                "isCancel" to true
            )
        )
        device.sendPacket(TransferPacket(packet))
    }

    private fun handleAction(intent: Intent) {
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: return
        val actionId = intent.getStringExtra(EXTRA_ACTION_ID) ?: return

        Log.d(TAG, "Action invoked: $actionId on notification $notificationId for device $deviceId")

        val device = deviceRegistry.getDevice(deviceId) ?: run {
            Log.w(TAG, "Device not found: $deviceId")
            return
        }

        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_NOTIFICATION_ACTION,
            body = mapOf(
                "key" to notificationId,
                "action" to actionId
            )
        )
        device.sendPacket(TransferPacket(packet))
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_NOTIFICATION_DISMISSED =
            "org.cosmicext.connect.Plugins.ReceiveNotificationsPlugin.DISMISSED"
        const val ACTION_NOTIFICATION_ACTION =
            "org.cosmicext.connect.Plugins.ReceiveNotificationsPlugin.ACTION"
        const val EXTRA_DEVICE_ID = "deviceId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_ACTION_ID = "actionId"

        private const val PACKET_TYPE_NOTIFICATION = "cconnect.notification"
        private const val PACKET_TYPE_NOTIFICATION_ACTION = "cconnect.notification.action"
    }
}
