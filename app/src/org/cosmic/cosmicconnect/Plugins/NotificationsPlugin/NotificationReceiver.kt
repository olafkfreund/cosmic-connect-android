/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin

import android.app.Service
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock

class NotificationReceiver : NotificationListenerService() {

    private var connected = false

    interface NotificationListener {
        fun onNotificationPosted(statusBarNotification: StatusBarNotification)
        fun onNotificationRemoved(statusBarNotification: StatusBarNotification?)
        fun onListenerConnected(service: NotificationReceiver)
    }

    private val listeners = CopyOnWriteArrayList<NotificationListener>()

    fun addListener(listener: NotificationListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: NotificationListener) {
        listeners.remove(listener)
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        for (listener in listeners) {
            listener.onNotificationPosted(statusBarNotification)
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification?) {
        for (listener in listeners) {
            listener.onNotificationRemoved(statusBarNotification)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        for (listener in listeners) {
            listener.onListenerConnected(this)
        }
        connected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connected = false
    }

    fun isConnected(): Boolean = connected

    // To use the service from the outer (name)space

    fun interface InstanceCallback {
        fun onServiceStart(service: NotificationReceiver)
    }

    // This will be called for each intent launch, even if the service is already started and is reused
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mutex.lock()
        try {
            for (c in callbacks) {
                c.onServiceStart(this)
            }
            callbacks.clear()
        } finally {
            mutex.unlock()
        }
        return Service.START_STICKY
    }

    companion object {
        private val callbacks = mutableListOf<InstanceCallback>()
        private val mutex = ReentrantLock(true)

        @JvmStatic
        fun Start(c: Context) {
            RunCommand(c, null)
        }

        @JvmStatic
        fun RunCommand(c: Context, callback: InstanceCallback?) {
            if (callback != null) {
                mutex.lock()
                try {
                    callbacks.add(callback)
                } finally {
                    mutex.unlock()
                }
            }
            val serviceIntent = Intent(c, NotificationReceiver::class.java)
            c.startService(serviceIntent)
        }
    }
}
