/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.messaging

data class MessagingApp(
    val packageName: String,
    val displayName: String,
    val webUrl: String,
    val supportsRcs: Boolean = false,
    val supportsReply: Boolean = true
)

object MessagingAppRegistry {
    
    private val apps = mapOf(
        "com.google.android.apps.messaging" to MessagingApp(
            packageName = "com.google.android.apps.messaging",
            displayName = "Google Messages",
            webUrl = "https://messages.google.com/web",
            supportsRcs = true
        ),
        "com.whatsapp" to MessagingApp(
            packageName = "com.whatsapp",
            displayName = "WhatsApp",
            webUrl = "https://web.whatsapp.com"
        ),
        "org.telegram.messenger" to MessagingApp(
            packageName = "org.telegram.messenger",
            displayName = "Telegram",
            webUrl = "https://web.telegram.org"
        )
    )
    
    fun isMessagingApp(packageName: String): Boolean {
        return apps.containsKey(packageName) || isGenericMessagingApp(packageName)
    }
    
    fun getApp(packageName: String): MessagingApp? {
        return apps[packageName]
    }
    
    fun getAllApps(): List<MessagingApp> = apps.values.toList()
    
    private fun isGenericMessagingApp(packageName: String): Boolean {
        val keywords = listOf("message", "messenger", "chat", "sms", "talk", "im")
        return keywords.any { packageName.lowercase().contains(it) }
    }
}
