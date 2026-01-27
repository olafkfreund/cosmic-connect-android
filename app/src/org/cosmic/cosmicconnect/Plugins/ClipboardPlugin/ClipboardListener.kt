/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2021 Ilmaz Gumerov <ilmaz1309@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.ClipboardPlugin

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.cosmic.cosmicconnect.BuildConfig
import org.cosmic.cosmicconnect.Helpers.ThreadHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class ClipboardListener private constructor(ctx: Context) {

    fun interface ClipboardObserver {
        fun clipboardChanged(content: String)
    }

    private val observers = HashSet<ClipboardObserver>()
    private val context: Context = ctx.applicationContext
    var currentContent: String? = null
        private set
    var updateTimestamp: Long = 0
        private set

    private var cm: ClipboardManager? = null

    init {
        Handler(Looper.getMainLooper()).post {
            cm = ContextCompat.getSystemService(context, ClipboardManager::class.java)
            cm?.addPrimaryClipChangedListener { onClipboardChanged() }
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED
        ) {
            ThreadHelper.execute {
                try {
                    val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                    // Listen only ClipboardService errors after now
                    val logcatFilter = if (Build.VERSION.SDK_INT > 35) { // Android 15 (VANILLA_ICE_CREAM)
                        "E ClipboardService"
                    } else {
                        "ClipboardService:E"
                    }
                    val process = Runtime.getRuntime().exec(arrayOf("logcat", "-T", timeStamp, logcatFilter, "*:S"))
                    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        if (line?.contains(BuildConfig.APPLICATION_ID) == true) {
                            context.startActivity(ClipboardFloatingActivity.getIntent(context, false))
                        }
                    }
                } catch (ignored: Exception) {
                }
            }
        }
    }

    fun registerObserver(observer: ClipboardObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: ClipboardObserver) {
        observers.remove(observer)
    }

    fun onClipboardChanged() {
        try {
            val clipData = cm?.primaryClip ?: return
            val item = clipData.getItemAt(0)
            val content = item.coerceToText(context).toString()

            if (content == currentContent) {
                return
            }
            updateTimestamp = System.currentTimeMillis()
            currentContent = content

            for (observer in observers) {
                observer.clipboardChanged(content)
            }
        } catch (e: Exception) {
            // Probably clipboard was not text
        }
    }

    @Suppress("deprecation")
    fun setText(text: String) {
        if (cm != null) {
            updateTimestamp = System.currentTimeMillis()
            currentContent = text
            cm?.setText(text)
        }
    }

    companion object {
        @Volatile
        private var instanceInternal: ClipboardListener? = null

        @JvmStatic
        fun instance(context: Context): ClipboardListener {
            return instanceInternal ?: synchronized(this) {
                instanceInternal ?: ClipboardListener(context).also { instanceInternal = it }
            }
        }
    }
}
