/*
 * SPDX-FileCopyrightText: 2023 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect

import android.app.Application
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.LifecycleHelper
import org.cosmicext.connect.Helpers.NotificationHelper
import org.cosmicext.connect.Helpers.SecurityHelpers.RsaHelper
import org.cosmicext.connect.Helpers.SecurityHelpers.SslHelper
import org.cosmicext.connect.Helpers.SecurityHelpers.SslHelperFFI
import org.cosmicext.connect.Plugins.PluginFactory
import org.cosmicext.connect.UserInterface.ThemeUtil
import org.slf4j.impl.HandroidLoggerAdapter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/*
 * This class holds all the active devices and makes them accessible from every other class.
 * It also takes care of initializing all classes that need so when the app boots.
 * It provides a ConnectionReceiver that the BackgroundService uses to ping this class every time a new DeviceLink is created.
 */
@HiltAndroidApp
class CosmicExtConnect : Application() {

    @Inject lateinit var deviceRegistry: DeviceRegistry
    @Inject lateinit var deviceHelper: DeviceHelper
    @Inject lateinit var rsaHelper: RsaHelper
    @Inject lateinit var sslHelper: SslHelper
    @Inject lateinit var pluginFactory: PluginFactory

    override fun onCreate() {
        // Override library name for UniFFI generated bindings
        System.setProperty("uniffi.component.cosmic_ext_connect_core.libraryOverride", "cosmic_ext_connect_core")
        
        super.onCreate()
        _instance = this
        setupSL4JLogging()
        Log.d("CosmicExtConnect/Application", "onCreate")
        ThemeUtil.setUserPreferredTheme(this)
        
        deviceHelper.initializeDeviceId()
        rsaHelper.initialiseRsaKeys()
        sslHelper.initialiseCertificate()

        // Initialize modern Keystore-backed certificate storage
        // This migrates legacy DataStore keys to AES-GCM encrypted files
        // backed by Android Keystore, then delegates all TLS operations
        try {
            SslHelperFFI.initialize(this)
            Log.i("CosmicExtConnect/Application", "Keystore-backed certificate storage initialized")
        } catch (e: Exception) {
            Log.e("CosmicExtConnect/Application", "Failed to initialize Keystore storage, using legacy", e)
        }

        NotificationHelper.initializeChannels(this)
        LifecycleHelper.initializeObserver()

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy())
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectFileUriExposure()
                    .detectContentUriWithoutPermission()
                    .detectCredentialProtectedWhileLocked()
                    .detectIncorrectContextUse()
                    .detectUnsafeIntentLaunch()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder(StrictMode.getThreadPolicy())
                    .detectUnbufferedIo()
                    .detectResourceMismatches()
                    .penaltyLog()
                    .build()
            )
        }
    }

    private fun setupSL4JLogging() {
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        HandroidLoggerAdapter.ANDROID_API_LEVEL = Build.VERSION.SDK_INT
        HandroidLoggerAdapter.APP_NAME = "COSMICConnect"
    }

    override fun onTerminate() {
        Log.d("CosmicExtConnect/Application", "onTerminate")
        super.onTerminate()
    }

    // Temporary bridges for components not yet refactored to use Hilt injection
    val devices get() = deviceRegistry.devices
    val connectionListener get() = deviceRegistry.connectionListener
    fun getDevice(id: String?) = deviceRegistry.getDevice(id)
    fun <T : org.cosmicext.connect.Plugins.Plugin> getDevicePlugin(deviceId: String?, pluginClass: Class<T>) = 
        deviceRegistry.getDevicePlugin(deviceId, pluginClass)
    fun addDeviceListChangedCallback(key: String, callback: DeviceRegistry.DeviceListChangedCallback) = 
        deviceRegistry.addDeviceListChangedCallback(key, callback)
    fun removeDeviceListChangedCallback(key: String) = 
        deviceRegistry.removeDeviceListChangedCallback(key)

    companion object {
        @JvmStatic
        private lateinit var _instance: CosmicExtConnect

        @JvmStatic
        fun getInstance(): CosmicExtConnect = _instance
    }
}