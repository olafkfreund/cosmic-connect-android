package org.cosmic.cosmicconnect.Helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.security.cert.Certificate
import android.util.Base64
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper.parseCertificate

private const val TRUSTED_DEVICES_PREFERENCES_NAME = "trusted_devices"

val Context.trustedDevicesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = TRUSTED_DEVICES_PREFERENCES_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, TRUSTED_DEVICES_PREFERENCES_NAME))
    }
)

object TrustedDevices {

    @JvmStatic
    fun isTrustedDevice(context: Context, deviceId: String): Boolean = runBlocking {
        context.trustedDevicesDataStore.data.map { it[booleanPreferencesKey(deviceId)] ?: false }.first()
    }

    fun addTrustedDevice(context: Context, deviceId: String) = runBlocking {
        context.trustedDevicesDataStore.edit { it[booleanPreferencesKey(deviceId)] = true }
    }

    fun removeTrustedDevice(context: Context, deviceId: String) = runBlocking {
        context.trustedDevicesDataStore.edit { it.remove(booleanPreferencesKey(deviceId)) }
        // Note: per-device settings are still in SharedPreferences for now as they are dynamic
        val deviceSettings = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        deviceSettings.edit().clear().apply()
    }

    fun getAllTrustedDevices(context: Context): List<String> = runBlocking {
        context.trustedDevicesDataStore.data.map { prefs ->
            prefs.asMap().keys.map { it.name }.filter { isTrustedDevice(context, it) }
        }.first()
    }

    fun removeAllTrustedDevices(context: Context) = runBlocking {
        context.trustedDevicesDataStore.edit { it.clear() }
    }

    fun getDeviceCertificate(context: Context, deviceId: String): Certificate {
        val devicePreferences = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        val certificateBytes = Base64.decode(devicePreferences.getString("certificate", ""), 0)
        return parseCertificate(certificateBytes)
    }

    @JvmStatic
    fun isCertificateStored(context: Context, deviceId: String): Boolean {
        val devicePreferences = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        val cert: String = devicePreferences.getString("certificate", "")!!
        return cert.isNotEmpty()
    }

    // Keep this for now as Plugins expect SharedPreferences
    fun getDeviceSettings(context: Context, deviceId: String): android.content.SharedPreferences {
        return context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
    }
}
