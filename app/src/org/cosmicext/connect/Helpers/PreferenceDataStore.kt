package org.cosmicext.connect.Helpers

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.cosmicext.connect.UserInterface.ThemeUtil

private const val USER_PREFERENCES_NAME = "settings"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "${context.packageName}_preferences"),
            SharedPreferencesMigration(context, "stored_menu_selection")
        )
    }
)

object PreferenceDataStore {
    val KEY_DEVICE_NAME = stringPreferencesKey("device_name_preference")
    val KEY_DEVICE_NAME_DOWNLOADED = booleanPreferencesKey("device_name_downloaded_preference")
    val KEY_DEVICE_ID = stringPreferencesKey("device_id_preference")
    val KEY_APP_THEME = stringPreferencesKey("theme_pref")
    val KEY_BLUETOOTH_ENABLED = booleanPreferencesKey("bluetooth_enabled")
    val KEY_CUSTOM_DEVICE_LIST = stringPreferencesKey("device_list_preference")
    val KEY_CUSTOM_TRUST_ALL_NETWORKS = booleanPreferencesKey("trust_all_network_preference")
    val KEY_CUSTOM_TRUSTED_NETWORKS = stringPreferencesKey("trusted_network_preference")
    
    val KEY_CERTIFICATE = stringPreferencesKey("certificate")
    val KEY_ALGORITHM = stringPreferencesKey("keyAlgorithm")
    val KEY_PUBLIC_KEY = stringPreferencesKey("publicKey")
    val KEY_PRIVATE_KEY = stringPreferencesKey("privateKey")
    
    // UI state keys
    val KEY_SELECTED_MENU_ENTRY = intPreferencesKey("selected_entry")
    val KEY_SELECTED_DEVICE = stringPreferencesKey("selected_device")

    fun getDeviceName(context: Context): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_NAME] ?: android.os.Build.MODEL
        }

    suspend fun getDeviceNameSync(context: Context): String = getDeviceName(context).first()

    fun getTheme(context: Context): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_APP_THEME] ?: ThemeUtil.DEFAULT_MODE
        }

    fun isBluetoothEnabled(context: Context): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_BLUETOOTH_ENABLED] ?: false
        }

    fun getDeviceId(context: Context): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_ID]
        }

    suspend fun getDeviceIdSync(context: Context): String? = getDeviceId(context).first()

    fun getCustomDeviceList(context: Context): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CUSTOM_DEVICE_LIST] ?: ""
        }

    @JvmStatic
    fun getCustomDeviceListSync(context: Context): String = runBlocking {
        getCustomDeviceList(context).first()
    }

    fun getTrustAllNetworks(context: Context): Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CUSTOM_TRUST_ALL_NETWORKS] ?: true
        }

    fun getTrustedNetworks(context: Context): Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_CUSTOM_TRUSTED_NETWORKS] ?: ""
        }

    fun getSelectedMenuEntry(context: Context): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SELECTED_MENU_ENTRY] ?: 1 // MENU_ENTRY_ADD_DEVICE
        }

    suspend fun getSelectedMenuEntrySync(context: Context): Int = getSelectedMenuEntry(context).first()

    fun getSelectedDevice(context: Context): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_SELECTED_DEVICE]
        }

    @JvmStatic
    fun getSelectedDeviceSync(context: Context): String? = runBlocking {
        getSelectedDevice(context).first()
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

    suspend fun setDeviceId(context: Context, deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_ID] = deviceId
        }
    }

    suspend fun setDeviceNameDownloaded(context: Context, downloaded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEVICE_NAME_DOWNLOADED] = downloaded
        }
    }

    suspend fun isDeviceNameDownloadedSync(context: Context): Boolean = context.dataStore.data
        .map { preferences ->
            preferences[KEY_DEVICE_NAME_DOWNLOADED] ?: false
        }.first()

    @JvmStatic
    suspend fun setCustomDeviceList(context: Context, list: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_DEVICE_LIST] = list
        }
    }

    @JvmStatic
    fun setCustomDeviceListSync(context: Context, list: String) = runBlocking {
        setCustomDeviceList(context, list)
    }

    @JvmStatic
    suspend fun setTrustAllNetworks(context: Context, trustAll: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_TRUST_ALL_NETWORKS] = trustAll
        }
    }

    @JvmStatic
    suspend fun setTrustedNetworks(context: Context, networks: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CUSTOM_TRUSTED_NETWORKS] = networks
        }
    }

    suspend fun setSelectedMenuEntry(context: Context, entry: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MENU_ENTRY] = entry
        }
    }

    @JvmStatic
    suspend fun setSelectedDevice(context: Context, deviceId: String?) {
        context.dataStore.edit { preferences ->
            if (deviceId == null) {
                preferences.remove(KEY_SELECTED_DEVICE)
            } else {
                preferences[KEY_SELECTED_DEVICE] = deviceId
            }
        }
    }

    @JvmStatic
    fun getCertificateSync(context: Context): String = runBlocking {
        context.dataStore.data.map { it[KEY_CERTIFICATE] ?: "" }.first()
    }

    @JvmStatic
    suspend fun setCertificate(context: Context, certificate: String) {
        context.dataStore.edit { it[KEY_CERTIFICATE] = certificate }
    }

    @JvmStatic
    fun getAlgorithmSync(context: Context, default: String): String = runBlocking {
        context.dataStore.data.map { it[KEY_ALGORITHM] ?: default }.first()
    }

    @JvmStatic
    suspend fun setAlgorithm(context: Context, algorithm: String) {
        context.dataStore.edit { it[KEY_ALGORITHM] = algorithm }
    }

    @JvmStatic
    fun getPublicKeySync(context: Context): String = runBlocking {
        context.dataStore.data.map { it[KEY_PUBLIC_KEY] ?: "" }.first()
    }

    @JvmStatic
    suspend fun setPublicKey(context: Context, publicKey: String) {
        context.dataStore.edit { it[KEY_PUBLIC_KEY] = publicKey }
    }

    @JvmStatic
    fun getPrivateKeySync(context: Context): String = runBlocking {
        context.dataStore.data.map { it[KEY_PRIVATE_KEY] ?: "" }.first()
    }

    @JvmStatic
    suspend fun setPrivateKey(context: Context, privateKey: String) {
        context.dataStore.edit { it[KEY_PRIVATE_KEY] = privateKey }
    }
}

class DataStorePreferenceAdapter(private val context: Context) : androidx.preference.PreferenceDataStore() {
    override fun putString(key: String, value: String?) {
        runBlocking {
            context.dataStore.edit { it[stringPreferencesKey(key)] = value ?: "" }
        }
    }

    override fun getString(key: String, defaultValue: String?): String? = runBlocking {
        context.dataStore.data.map { it[stringPreferencesKey(key)] ?: defaultValue }.first()
    }

    override fun putBoolean(key: String, value: Boolean) {
        runBlocking {
            context.dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = runBlocking {
        context.dataStore.data.map { it[booleanPreferencesKey(key)] ?: defaultValue }.first()
    }

    override fun putInt(key: String, value: Int) {
        runBlocking {
            context.dataStore.edit { it[intPreferencesKey(key)] = value }
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int = runBlocking {
        context.dataStore.data.map { it[intPreferencesKey(key)] ?: defaultValue }.first()
    }

    override fun putLong(key: String, value: Long) {
        runBlocking {
            context.dataStore.edit { it[longPreferencesKey(key)] = value }
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long = runBlocking {
        context.dataStore.data.map { it[longPreferencesKey(key)] ?: defaultValue }.first()
    }

    override fun putFloat(key: String, value: Float) {
        runBlocking {
            context.dataStore.edit { it[floatPreferencesKey(key)] = value }
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float = runBlocking {
        context.dataStore.data.map { it[floatPreferencesKey(key)] ?: defaultValue }.first()
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        runBlocking {
            context.dataStore.edit { it[stringSetPreferencesKey(key)] = values ?: emptySet() }
        }
    }

    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? = runBlocking {
        context.dataStore.data.map { it[stringSetPreferencesKey(key)] ?: defaultValue }.first()
    }
}