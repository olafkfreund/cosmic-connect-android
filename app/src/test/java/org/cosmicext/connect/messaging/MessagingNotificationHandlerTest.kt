/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.messaging

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MessagingNotificationHandlerTest {

    @MockK
    private lateinit var mockSbn: StatusBarNotification
    
    @MockK
    private lateinit var mockNotification: Notification

    private lateinit var handler: MessagingNotificationHandler

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        handler = MessagingNotificationHandler(RuntimeEnvironment.getApplication())
        every { mockSbn.notification } returns mockNotification
    }

    @Test
    fun testDetectWhatsApp() {
        every { mockSbn.packageName } returns "com.whatsapp"
        
        val extras = Bundle().apply {
            putString(NotificationCompat.EXTRA_TITLE, "John Doe")
            putCharSequence(NotificationCompat.EXTRA_TEXT, "Hey, are you coming?")
        }
        mockNotification.extras = extras
        every { mockSbn.postTime } returns 123456789L
        every { mockSbn.key } returns "wa_key"

        val data = handler.processNotification(mockSbn)

        assertNotNull(data)
        assertEquals("WhatsApp", data?.appName)
        assertEquals("John Doe", data?.sender)
        assertEquals("Hey, are you coming?", data?.message)
        assertEquals("https://web.whatsapp.com", data?.webUrl)
    }

    @Test
    fun testDetectGoogleMessages() {
        every { mockSbn.packageName } returns "com.google.android.apps.messaging"
        
        val extras = Bundle().apply {
            putString(NotificationCompat.EXTRA_TITLE, "Mom")
            putCharSequence(NotificationCompat.EXTRA_TEXT, "Don't forget the milk!")
            putString("android.shortcutId", "thread_456")
        }
        mockNotification.extras = extras
        every { mockSbn.postTime } returns 987654321L
        every { mockSbn.key } returns "gm_key"

        val data = handler.processNotification(mockSbn)

        assertNotNull(data)
        assertEquals("Google Messages", data?.appName)
        assertEquals("thread_456", data?.conversationId)
        assertEquals("https://messages.google.com/web", data?.webUrl)
    }

    @Test
    fun testIgnoreNonMessagingApp() {
        every { mockSbn.packageName } returns "com.android.settings"
        val data = handler.processNotification(mockSbn)
        assertNull(data)
    }
}
