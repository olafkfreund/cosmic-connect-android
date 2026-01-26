/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.messaging

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MessagingNotificationHandlerTest {

    @Mock
    private lateinit var mockSbn: StatusBarNotification
    
    @Mock
    private lateinit var mockNotification: Notification

    private lateinit var handler: MessagingNotificationHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        handler = MessagingNotificationHandler(RuntimeEnvironment.getApplication())
        `when`(mockSbn.notification).thenReturn(mockNotification)
    }

    @Test
    def testDetectWhatsApp() {
        `when`(mockSbn.packageName).thenReturn("com.whatsapp")
        
        val extras = Bundle().apply {
            putString(NotificationCompat.EXTRA_TITLE, "John Doe")
            putCharSequence(NotificationCompat.EXTRA_TEXT, "Hey, are you coming?")
        }
        mockNotification.extras = extras
        `when`(mockSbn.postTime).thenReturn(123456789L)
        `when`(mockSbn.key).thenReturn("wa_key")

        val data = handler.processNotification(mockSbn)

        assertNotNull(data)
        assertEquals("WhatsApp", data?.appName)
        assertEquals("John Doe", data?.sender)
        assertEquals("Hey, are you coming?", data?.message)
        assertEquals("https://web.whatsapp.com", data?.webUrl)
        assertTrue(data?.isMessagingApp == true)
    }

    @Test
    def testDetectGoogleMessages() {
        `when`(mockSbn.packageName).thenReturn("com.google.android.apps.messaging")
        
        val extras = Bundle().apply {
            putString(NotificationCompat.EXTRA_TITLE, "Mom")
            putCharSequence(NotificationCompat.EXTRA_TEXT, "Don't forget the milk!")
            putString(NotificationCompat.EXTRA_SHORTCUT_ID, "thread_456")
        }
        mockNotification.extras = extras
        `when`(mockSbn.postTime).thenReturn(987654321L)
        `when`(mockSbn.key).thenReturn("gm_key")

        val data = handler.processNotification(mockSbn)

        assertNotNull(data)
        assertEquals("Google Messages", data?.appName)
        assertEquals("thread_456", data?.conversationId)
        assertEquals("https://messages.google.com/web", data?.webUrl)
    }

    @Test
    def testIgnoreNonMessagingApp() {
        `when`(mockSbn.packageName).thenReturn("com.android.settings")
        val data = handler.processNotification(mockSbn)
        assertNull(data)
    }
}
