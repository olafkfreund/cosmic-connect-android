/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.messaging

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import org.cosmic.cosmicconnect.messaging.MessagingAppRegistry

data class MessagingNotificationData(
    val packageName: String,
    val appName: String,
    val webUrl: String?,
    val sender: String,
    val message: String,
    val conversationId: String?,
    val timestamp: Long,
    val isGroupChat: Boolean,
    val groupName: String?,
    val hasReplyAction: Boolean,
    val notificationKey: String
)

class MessagingNotificationHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "CConnect/Messaging"
    }

    fun processNotification(sbn: StatusBarNotification): MessagingNotificationData? {
        val packageName = sbn.packageName
        
        if (!MessagingAppRegistry.isMessagingApp(packageName)) {
            return null
        }
        
        val notification = sbn.notification
        val extras = notification.extras
        
        val app = MessagingAppRegistry.getApp(packageName)
        val appName = app?.displayName ?: packageName
        
        val sender = extras.getString(NotificationCompat.EXTRA_TITLE) ?: "Unknown"
        val message = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString() ?: ""
        
        val conversationId = extras.getString(NotificationCompat.EXTRA_SHORTCUT_ID)
        val isGroup = extras.getBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION, false)
        val groupName = extras.getString(NotificationCompat.EXTRA_CONVERSATION_TITLE)
        
        val hasReply = findReplyAction(notification) != null
        
        return MessagingNotificationData(
            packageName = packageName,
            appName = appName,
            webUrl = app?.webUrl,
            sender = sender,
            message = message,
            conversationId = conversationId,
            timestamp = sbn.postTime,
            isGroupChat = isGroup,
            groupName = groupName,
            hasReplyAction = hasReply,
            notificationKey = sbn.key
        )
    }
    
    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        return actions.find { action ->
            action.remoteInputs?.any { 
                it.resultKey?.contains("reply", ignoreCase = true) == true
            } == true
        }
    }
}
