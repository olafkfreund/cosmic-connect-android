/*
 * SPDX-FileCopyrightText: 2017 Julian Wolff <wolff@julianwolff.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmicext.connect.Plugins.NotificationsPlugin

import android.app.PendingIntent
import android.app.RemoteInput
import java.util.UUID

class RepliableNotification {
    val id: String = UUID.randomUUID().toString()
    lateinit var pendingIntent: PendingIntent
    val remoteInputs = mutableListOf<RemoteInput>()
    var packageName: String? = null
    var tag: String? = null
}
