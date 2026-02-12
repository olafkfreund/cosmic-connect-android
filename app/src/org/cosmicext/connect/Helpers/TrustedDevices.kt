/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Helpers

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.EntryPoints
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.cosmicext.connect.di.HiltBridges
import java.security.cert.Certificate
import javax.inject.Inject
import javax.inject.Singleton

private const val TRUSTED_DEVICES_PREFERENCES_NAME = "trusted_devices"

val Context.trustedDevicesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = TRUSTED_DEVICES_PREFERENCES_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, TRUSTED_DEVICES_PREFERENCES_NAME))
    }
)

@Singleton
class TrustedDevices @Inject constructor(@ApplicationContext private val context: Context) {

    fun isTrustedDevice(deviceId: String): Boolean = runBlocking {
        context.trustedDevicesDataStore.data.map { it[booleanPreferencesKey(deviceId)] ?: false }.first()
    }

    fun addTrustedDevice(deviceId: String) = runBlocking {
        context.trustedDevicesDataStore.edit { it[booleanPreferencesKey(deviceId)] = true }
    }

    fun removeTrustedDevice(deviceId: String) = runBlocking {
        context.trustedDevicesDataStore.edit { it.remove(booleanPreferencesKey(deviceId)) }
        // Note: per-device settings are still in SharedPreferences for now as they are dynamic
        val deviceSettings = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        deviceSettings.edit().clear().apply()
    }

    fun getAllTrustedDevices(): List<String> = runBlocking {
        context.trustedDevicesDataStore.data.map { prefs ->
            prefs.asMap().keys.map { it.name }.filter { isTrustedDevice(it) }
        }.first()
    }

    fun removeAllTrustedDevices() = runBlocking {
        context.trustedDevicesDataStore.edit { it.clear() }
    }

    fun getDeviceCertificate(deviceId: String): Certificate {
        val devicePreferences = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        val certificateBytes = Base64.decode(devicePreferences.getString("certificate", ""), 0)
        val sslHelper = EntryPoints.get(context.applicationContext, HiltBridges::class.java).sslHelper()
        return sslHelper.parseCertificate(certificateBytes)
    }

    fun isCertificateStored(deviceId: String): Boolean {
        val devicePreferences = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        val cert: String = devicePreferences.getString("certificate", "")!!
        return cert.isNotEmpty()
    }

    // Keep this for now as Plugins expect SharedPreferences
    fun getDeviceSettings(deviceId: String): android.content.SharedPreferences {
        return context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
    }

    companion object {
        // Temporary static access for non-Hilt components
        @JvmStatic
        fun isTrustedDevice(context: Context, deviceId: String): Boolean {
            return EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().isTrustedDevice(deviceId)
        }

        @JvmStatic
        fun removeTrustedDevice(context: Context, deviceId: String) {
            EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().removeTrustedDevice(deviceId)
        }

        @JvmStatic
        fun addTrustedDevice(context: Context, deviceId: String) {
            EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().addTrustedDevice(deviceId)
        }

        @JvmStatic
        fun getAllTrustedDevices(context: Context): List<String> {
            return EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().getAllTrustedDevices()
        }

        @JvmStatic
        fun removeAllTrustedDevices(context: Context) {
            EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().removeAllTrustedDevices()
        }

        @JvmStatic
        fun getDeviceCertificate(context: Context, deviceId: String): Certificate {
            return EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().getDeviceCertificate(deviceId)
        }

        @JvmStatic
        fun isCertificateStored(context: Context, deviceId: String): Boolean {
            return EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().isCertificateStored(deviceId)
        }

        @JvmStatic
        fun getDeviceSettings(context: Context, deviceId: String): android.content.SharedPreferences {
            return EntryPoints.get(context.applicationContext, HiltBridges::class.java).trustedDevices().getDeviceSettings(deviceId)
        }
    }
}
