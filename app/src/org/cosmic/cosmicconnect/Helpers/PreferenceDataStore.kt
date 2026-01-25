package org.cosmic.cosmicconnect.Helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.cosmic.cosmicconnect.UserInterface.ThemeUtil

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceDataStore {
    val KEY_DEVICE_NAME = stringPreferencesKey("deviceName")
    val KEY_APP_THEME = stringPreferencesKey("theme")
    val KEY_BLUETOOTH_ENABLED = booleanPreferencesKey("bluetooth_enabled")

    fun getDeviceName(context: Context): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_NAME] ?: DeviceHelper.getDeviceName(context)
        }

    fun getTheme(context: Context): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_APP_THEME] ?: ThemeUtil.DEFAULT_MODE
        }

    fun isBluetoothEnabled(context: Context): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BLUETOOTH_ENABLED] ?: false
        }

    suspend fun setDeviceName(context: Context, name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_NAME] = name
        }
    }

    suspend fun setTheme(context: Context, theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME] = theme
        }
    }

    suspend fun setBluetoothEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BLUETOOTH_ENABLED] = enabled
        }
    }
}
