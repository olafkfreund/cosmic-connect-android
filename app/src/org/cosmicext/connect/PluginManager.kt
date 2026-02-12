/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.collections4.MultiValuedMap
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Helpers.TrustedDevices
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.Plugin.Companion.getPluginKey
import org.cosmicext.connect.Plugins.PluginFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages plugin lifecycle, permissions, and packet dispatch for a device.
 */
class PluginManager(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: () -> String,
    private val pluginFactory: PluginFactory,
    private val device: Device,
    private val isPaired: () -> Boolean,
    private val isReachable: () -> Boolean,
    private val getSupportedPlugins: () -> List<String>
) {
    /**
     * Plugins that have been instantiated successfully.
     */
    val loadedPlugins: ConcurrentMap<String, Plugin> = ConcurrentHashMap()

    /**
     * Plugins that have not been instantiated because of missing permissions.
     */
    val pluginsWithoutPermissions: ConcurrentMap<String, Plugin> = ConcurrentHashMap()

    /**
     * Subset of loadedPlugins that will have some limitation because of missing optional permissions.
     */
    val pluginsWithoutOptionalPermissions: ConcurrentMap<String, Plugin> = ConcurrentHashMap()

    /**
     * Same as loadedPlugins but indexed by incoming packet type
     */
    private var pluginsByIncomingInterface: MultiValuedMap<String, String> = ArrayListValuedHashMap()

    val pluginsByIncomingInterfaceEmpty: Boolean
        get() = pluginsByIncomingInterface.isEmpty

    private val pluginsChangedListeners = CopyOnWriteArrayList<Device.PluginsChangedListener>()

    fun <T : Plugin> getPlugin(pluginClass: Class<T>): T? {
        val plugin = getPlugin(getPluginKey(pluginClass))
        return plugin?.let(pluginClass::cast)
    }

    fun getPlugin(pluginKey: String): Plugin? = loadedPlugins[pluginKey]

    fun getPluginIncludingWithoutPermissions(pluginKey: String): Plugin? {
        return loadedPlugins[pluginKey] ?: pluginsWithoutPermissions[pluginKey]
    }

    fun setPluginEnabled(pluginKey: String, value: Boolean) {
        TrustedDevices.getDeviceSettings(context, deviceId).edit { putBoolean(pluginKey, value) }
        reloadPluginsFromSettings()
    }

    fun isPluginEnabled(pluginKey: String): Boolean {
        val enabledByDefault = pluginFactory.getPluginInfo(pluginKey).isEnabledByDefault
        return TrustedDevices.getDeviceSettings(context, deviceId).getBoolean(pluginKey, enabledByDefault)
    }

    fun notifyPluginsOfDeviceUnpaired(context: Context, deviceId: String) {
        for (pluginKey in getSupportedPlugins()) {
            val plugin = getPlugin(pluginKey) ?: pluginFactory.instantiatePluginForDevice(context, pluginKey, device)
            plugin?.onDeviceUnpaired(context, deviceId)
        }
    }

    fun launchBackgroundReloadPluginsFromSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            reloadPluginsFromSettings()
        }
    }

    @Synchronized
    @WorkerThread
    fun reloadPluginsFromSettings() {
        Log.i("PluginManager", "${deviceName()}: reloading plugins")
        val newPluginsByIncomingInterface: MultiValuedMap<String, String> = ArrayListValuedHashMap()

        getSupportedPlugins().forEach { pluginKey ->
            val pluginInfo = pluginFactory.getPluginInfo(pluginKey)
            val listenToUnpaired = pluginInfo.listenToUnpaired

            val pluginEnabled = (isPaired() || listenToUnpaired) && isReachable() && isPluginEnabled(pluginKey)

            if (pluginEnabled && addPlugin(pluginKey)) {
                pluginInfo.supportedPacketTypes.forEach { packetType ->
                    newPluginsByIncomingInterface.put(packetType, pluginKey)
                }
            } else {
                removePlugin(pluginKey)
            }
        }

        pluginsByIncomingInterface = newPluginsByIncomingInterface

        onPluginsChanged()
    }

    fun notifyPluginPacketReceived(tp: TransferPacket) {
        val targetPlugins = pluginsByIncomingInterface[tp.packet.type]
        if (targetPlugins.isEmpty()) {
            Log.w("PluginManager", "Ignoring packet with type ${tp.packet.type} because no plugin can handle it")
            return
        }
        targetPlugins
            .asSequence()
            .mapNotNull { loadedPlugins[it] }
            .forEach { plugin ->
                runCatching {
                    if (isPaired()) {
                        plugin.onPacketReceived(tp)
                    } else {
                        plugin.onUnpairedDevicePacketReceived(tp)
                    }
                }.onFailure { e ->
                    Log.e("PluginManager", "Exception in ${plugin.pluginKey}'s onPacketReceived()", e)
                }
            }
    }

    fun onPluginsChanged() = pluginsChangedListeners.forEach { it.onPluginsChanged(device) }

    fun addPluginsChangedListener(listener: Device.PluginsChangedListener) = pluginsChangedListeners.add(listener)

    fun removePluginsChangedListener(listener: Device.PluginsChangedListener) = pluginsChangedListeners.remove(listener)

    // Helper function for reloadPluginsFromSettings(), do not call from elsewhere
    private fun addPlugin(pluginKey: String): Boolean {
        val isNewPlugin = !loadedPlugins.containsKey(pluginKey)

        val plugin = loadedPlugins[pluginKey]
            ?: pluginFactory.instantiatePluginForDevice(context, pluginKey, device)
                ?: return false

        if (!plugin.isCompatible) {
            Log.d("Cosmic/addPlugin", "Minimum requirements (e.g. API level) not fulfilled $pluginKey")
            return false
        }

        if (!plugin.checkRequiredPermissions()) {
            Log.d("Cosmic/addPlugin", "No permission $pluginKey")
            pluginsWithoutPermissions[pluginKey] = plugin
            if (plugin.loadPluginWhenRequiredPermissionsMissing()) {
                loadedPlugins[pluginKey] = plugin
            } else {
                loadedPlugins.remove(pluginKey)
                return false
            }
        } else {
            Log.d("Cosmic/addPlugin", "Permissions OK $pluginKey")
            loadedPlugins[pluginKey] = plugin
            pluginsWithoutPermissions.remove(pluginKey)
            if (plugin.checkOptionalPermissions()) {
                Log.d("Cosmic/addPlugin", "Optional Permissions OK $pluginKey")
                pluginsWithoutOptionalPermissions.remove(pluginKey)
            } else {
                Log.d("Cosmic/addPlugin", "No optional permission $pluginKey")
                pluginsWithoutOptionalPermissions[pluginKey] = plugin
            }
        }

        if (!isNewPlugin) {
            return true
        }

        return runCatching {
            plugin.onCreate()
        }.onFailure {
            Log.e("Cosmic/addPlugin", "plugin failed to load $pluginKey", it)
        }.getOrDefault(false)
    }

    // Helper function for reloadPluginsFromSettings(), do not call from elsewhere
    private fun removePlugin(pluginKey: String): Boolean {
        val plugin = loadedPlugins.remove(pluginKey) ?: return false

        try {
            plugin.onDestroy()
        } catch (e: Exception) {
            Log.e("Cosmic/removePlugin", "Exception calling onDestroy for plugin $pluginKey", e)
        }

        return true
    }
}
