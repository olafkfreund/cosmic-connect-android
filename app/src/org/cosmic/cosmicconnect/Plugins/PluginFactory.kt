/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Plugins

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Device
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginFactory @Inject constructor(@ApplicationContext private val context: Context) {
    annotation class LoadablePlugin  //Annotate plugins with this so PluginFactory finds them

    private var pluginInfo: Map<String, PluginInfo> = mapOf()

    fun initPluginInfo() {
        try {
            val plugins = listOf(
                org.cosmic.cosmicconnect.Plugins.PingPlugin.PingPlugin::class,
                org.cosmic.cosmicconnect.Plugins.BatteryPlugin.BatteryPlugin::class,
                org.cosmic.cosmicconnect.Plugins.BatteryPlugin.BatteryPluginFFI::class,
                org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisPlugin::class,
                org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationsPlugin::class,
                org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin.ReceiveNotificationsPlugin::class,
                org.cosmic.cosmicconnect.Plugins.SharePlugin.SharePlugin::class,
                org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpPlugin::class,
                org.cosmic.cosmicconnect.Plugins.ContactsPlugin.ContactsPlugin::class,
                org.cosmic.cosmicconnect.Plugins.FindRemoteDevicePlugin.FindRemoteDevicePlugin::class,
                org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadPlugin::class,
                org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterPlugin::class,
                org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityReportPlugin::class,
                org.cosmic.cosmicconnect.Plugins.SMSPlugin.SMSPlugin::class,
                org.cosmic.cosmicconnect.Plugins.TelephonyPlugin.TelephonyPlugin::class
            )

            pluginInfo = plugins
                .asSequence()
                .map { it.java.getDeclaredConstructor().newInstance() as Plugin }
                .onEach { it.setContext(context, null) }
                .associate { Pair(it.pluginKey, PluginInfo(it)) }
        } catch (e: Exception) {
            Log.e("PluginFactory", "Error loading plugins", e)
            // throw RuntimeException(e)
        }
        Log.i("PluginFactory", "Loaded " + pluginInfo.size + " plugins")
    }

    val availablePlugins: Set<String>
        get() = pluginInfo.keys
    val incomingCapabilities: Set<String>
        get() = pluginInfo.values.flatMap { plugin -> plugin.supportedPacketTypes }.toSet()
    val outgoingCapabilities: Set<String>
        get() = pluginInfo.values.flatMap { plugin -> plugin.outgoingPacketTypes }.toSet()

    fun getPluginInfo(pluginKey: String): PluginInfo = pluginInfo[pluginKey]!!

    fun sortPluginList(plugins: List<String>): List<String> {
        return plugins.sortedBy { pluginInfo[it]?.displayName }
    }

    fun instantiatePluginForDevice(context: Context, pluginKey: String, device: Device): Plugin? {
        try {
            val plugin = pluginInfo[pluginKey]?.instantiableClass?.getDeclaredConstructor()?.newInstance()?.apply { setContext(context, device) }
            return plugin
        } catch (e: Exception) {
            Log.e("PluginFactory", "Could not instantiate plugin: $pluginKey", e)
            return null
        }
    }

    fun pluginsForCapabilities(incoming: Set<String>, outgoing: Set<String>): Set<String> {
        fun hasCommonCapabilities(info: PluginInfo): Boolean =
            outgoing.any { it in info.supportedPacketTypes } ||
            incoming.any { it in info.outgoingPacketTypes }

        val (used, unused) = pluginInfo.entries.partition { hasCommonCapabilities(it.value) }

        for (pluginId in unused.map { it.key }) {
            Log.d("PluginFactory", "Won't load $pluginId because of unmatched capabilities")
        }

        return used.map { it.key }.toSet()
    }

    class PluginInfo private constructor(
        val displayName: String,
        val description: String,
        val isEnabledByDefault: Boolean,
        val hasSettings: Boolean,
        val listenToUnpaired: Boolean,
        supportedPacketTypes: Array<String>,
        outgoingPacketTypes: Array<String>,
        val instantiableClass: Class<out Plugin>,
    ) {
        internal constructor(p: Plugin) : this(p.displayName, p.description,
            p.isEnabledByDefault, p.hasSettings(), p.listensToUnpairedDevices(),
            p.supportedPacketTypes, p.outgoingPacketTypes, p.javaClass)

        val supportedPacketTypes: Set<String> = supportedPacketTypes.toSet()
        val outgoingPacketTypes: Set<String> = outgoingPacketTypes.toSet()
    }

    companion object {
        @JvmStatic
        fun getPluginKey(p: Class<out Plugin>): String {
            return p.simpleName
        }
    }
}