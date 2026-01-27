/*
 * SPDX-FileCopyrightText: 2024 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Helpers

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import com.univocity.parsers.common.TextParsingException
import com.univocity.parsers.csv.CsvParser
import com.univocity.parsers.csv.CsvParserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.DeviceType
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sslHelper: SslHelper,
    private val pluginFactory: PluginFactory
) {
    companion object {
        const val PROTOCOL_VERSION = 8
        const val KEY_DEVICE_NAME_PREFERENCE = "device_name_preference"
        private const val DEVICE_DATABASE = "https://storage.googleapis.com/play_public/supported_devices.csv"
        private val NAME_INVALID_CHARACTERS_REGEX = "[\\\"',;:.!?()\\[\\]<>]".toRegex()
        const val MAX_DEVICE_NAME_LENGTH = 32

        @JvmStatic
        fun getDeviceId(context: Context): String = runBlocking {
            PreferenceDataStore.getDeviceIdSync(context) ?: ""
        }

        @JvmStatic
        fun filterInvalidCharactersFromDeviceName(input: String): String = input.replace(NAME_INVALID_CHARACTERS_REGEX, "")

        @JvmStatic
        fun filterInvalidCharactersFromDeviceNameAndLimitLength(input: String): String = 
            filterInvalidCharactersFromDeviceName(input).trim().take(MAX_DEVICE_NAME_LENGTH)
    }

    private var fetchingName = false

    val isTablet: Boolean by lazy {
        val config = Resources.getSystem().configuration
        ((config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)
    }

    val isTv: Boolean by lazy {
        val uiMode = Resources.getSystem().configuration.uiMode
        (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    }

    val deviceType: DeviceType by lazy {
        if (isTv) {
            DeviceType.TV
        } else if (isTablet) {
            DeviceType.TABLET
        } else {
            DeviceType.PHONE
        }
    }

    fun getDeviceName(): String = runBlocking {
        if (!fetchingName && !PreferenceDataStore.isDeviceNameDownloadedSync(context)) {
            fetchingName = true
            backgroundFetchDeviceName()
        }
        PreferenceDataStore.getDeviceNameSync(context)
    }

    private fun backgroundFetchDeviceName() {
        ThreadHelper.execute {
            try {
                val url = URL(DEVICE_DATABASE)
                val connection = url.openConnection()

                runBlocking {
                    PreferenceDataStore.setDeviceNameDownloaded(context, true)
                }

                BufferedReader(
                    InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_16)
                ).use { reader ->
                    val settings = CsvParserSettings()
                    settings.isHeaderExtractionEnabled = true
                    val parser = CsvParser(settings)
                    var found = false
                    for (records in parser.iterate(reader)) {
                        if (records.size < 4) {
                            continue
                        }
                        val buildModel = records[3]
                        if (Build.MODEL.equals(buildModel, ignoreCase = true)) {
                            val deviceName = records[1]
                            Log.i("DeviceHelper", "Got device name: $deviceName")
                            setDeviceName(deviceName)
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        Log.e("DeviceHelper", "Didn't find a device name for " + Build.MODEL)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: TextParsingException) {
                e.printStackTrace()
            }
            fetchingName = false
        }
    }

    fun setDeviceName(name: String) {
        val filteredName = filterInvalidCharactersFromDeviceNameAndLimitLength(name)
        runBlocking {
            PreferenceDataStore.setDeviceName(context, filteredName)
        }
    }

    fun initializeDeviceId() {
        runBlocking {
            val deviceId = PreferenceDataStore.getDeviceIdSync(context) ?: ""
            if (DeviceInfo.isValidDeviceId(deviceId)) {
                return@runBlocking
            }
            val newDeviceId = UUID.randomUUID().toString().replace("-", "")
            PreferenceDataStore.setDeviceId(context, newDeviceId)
        }
    }

    fun getDeviceId(): String = runBlocking {
        PreferenceDataStore.getDeviceIdSync(context) ?: ""
    }

    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            getDeviceId(),
            sslHelper.certificate,
            getDeviceName(),
            deviceType,
            PROTOCOL_VERSION,
            pluginFactory.incomingCapabilities,
            pluginFactory.outgoingCapabilities
        )
    }
}
