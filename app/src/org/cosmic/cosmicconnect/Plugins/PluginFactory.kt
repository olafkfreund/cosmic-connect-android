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
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pluginCreators: Map<String, @JvmSuppressWildcards PluginCreator>,
) {
    annotation class LoadablePlugin  //Annotate plugins with this so PluginFactory finds them

    private var pluginInfo: Map<String, PluginInfo> = mapOf()

    fun initPluginInfo() {
        val result = mutableMapOf<String, PluginInfo>()

        // --- Migrated plugins: build PluginInfo from static registry ---
        for ((pluginClass, metadata) in migratedPlugins) {
            result[metadata.pluginKey] = PluginInfo(context, metadata, pluginClass)
            Log.d("PluginFactory", "Loaded (Hilt): ${metadata.pluginKey}")
        }

        // --- Legacy plugins: build PluginInfo via reflection instantiation ---
        try {
            val legacyInfos = legacyPlugins
                .asSequence()
                .map { it.java.getDeclaredConstructor().newInstance() as Plugin }
                .onEach { it.setContext(context, null) }
                .associate { Pair(it.pluginKey, PluginInfo(it)) }

            result.putAll(legacyInfos)
        } catch (e: Exception) {
            Log.e("PluginFactory", "Error loading legacy plugins", e)
        }

        pluginInfo = result
        Log.i("PluginFactory", "Loaded ${pluginInfo.size} plugins (${migratedPlugins.size} Hilt, ${legacyPlugins.size} legacy)")
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
            // Hilt path: use PluginCreator if available for this plugin key
            pluginCreators[pluginKey]?.let { creator ->
                return creator.create(device)
            }

            // Legacy path: reflection-based instantiation
            val plugin = pluginInfo[pluginKey]?.instantiableClass
                ?.getDeclaredConstructor()?.newInstance()
                ?.apply { setContext(context, device) }
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

    /**
     * Static metadata for migrated plugins, used to build [PluginInfo]
     * without instantiating the plugin class. Avoids the Dagger KSP bug
     * with array-typed annotation parameters on @AssistedInject classes.
     */
    data class StaticPluginMetadata(
        val pluginKey: String,
        val supportedPacketTypes: Array<String>,
        val outgoingPacketTypes: Array<String>,
        val displayNameRes: Int,
        val descriptionRes: Int,
        val isEnabledByDefault: Boolean = true,
        val hasSettings: Boolean = false,
        val listenToUnpaired: Boolean = false,
    )

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
        /** Build PluginInfo from a live plugin instance (legacy path). */
        internal constructor(p: Plugin) : this(p.displayName, p.description,
            p.isEnabledByDefault, p.hasSettings(), p.listensToUnpairedDevices(),
            p.supportedPacketTypes, p.outgoingPacketTypes, p.javaClass)

        /** Build PluginInfo from static metadata registry (Hilt path). */
        internal constructor(context: Context, metadata: StaticPluginMetadata, pluginClass: Class<out Plugin>) : this(
            displayName = context.getString(metadata.displayNameRes),
            description = context.getString(metadata.descriptionRes),
            isEnabledByDefault = metadata.isEnabledByDefault,
            hasSettings = metadata.hasSettings,
            listenToUnpaired = metadata.listenToUnpaired,
            supportedPacketTypes = metadata.supportedPacketTypes,
            outgoingPacketTypes = metadata.outgoingPacketTypes,
            instantiableClass = pluginClass,
        )

        val supportedPacketTypes: Set<String> = supportedPacketTypes.toSet()
        val outgoingPacketTypes: Set<String> = outgoingPacketTypes.toSet()
    }

    companion object {
        @JvmStatic
        fun getPluginKey(p: Class<out Plugin>): String {
            return p.simpleName
        }

        /**
         * Plugins migrated to @AssistedInject with their static metadata.
         * No reflection needed — metadata is provided at compile time.
         * Instance creation uses the Hilt PluginCreator map.
         */
        private val migratedPlugins: Map<Class<out Plugin>, StaticPluginMetadata> = mapOf(
            org.cosmic.cosmicconnect.Plugins.PingPlugin.PingPlugin::class.java to StaticPluginMetadata(
                pluginKey = "PingPlugin",
                supportedPacketTypes = arrayOf("cconnect.ping"),
                outgoingPacketTypes = arrayOf("cconnect.ping"),
                displayNameRes = R.string.pref_plugin_ping,
                descriptionRes = R.string.pref_plugin_ping_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.FindRemoteDevicePlugin.FindRemoteDevicePlugin::class.java to StaticPluginMetadata(
                pluginKey = "FindRemoteDevicePlugin",
                supportedPacketTypes = emptyArray(),
                outgoingPacketTypes = arrayOf("cconnect.findmyphone.request"),
                displayNameRes = R.string.pref_plugin_findremotedevice,
                descriptionRes = R.string.pref_plugin_findremotedevice_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityReportPlugin::class.java to StaticPluginMetadata(
                pluginKey = "ConnectivityReportPlugin",
                supportedPacketTypes = emptyArray(),
                outgoingPacketTypes = arrayOf("cconnect.connectivity_report"),
                displayNameRes = R.string.pref_plugin_connectivity_report,
                descriptionRes = R.string.pref_plugin_connectivity_report_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterPlugin::class.java to StaticPluginMetadata(
                pluginKey = "PresenterPlugin",
                supportedPacketTypes = emptyArray(),
                outgoingPacketTypes = arrayOf("cconnect.mousepad.request", "cconnect.presenter"),
                displayNameRes = R.string.pref_plugin_presenter,
                descriptionRes = R.string.pref_plugin_presenter_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadPlugin::class.java to StaticPluginMetadata(
                pluginKey = "MousePadPlugin",
                supportedPacketTypes = arrayOf("cconnect.mousepad.keyboardstate"),
                outgoingPacketTypes = arrayOf("cconnect.mousepad.request"),
                displayNameRes = R.string.pref_plugin_mousepad,
                descriptionRes = R.string.pref_plugin_mousepad_desc_nontv,
                hasSettings = true,
            ),
            // --- Wave 2 ---
            org.cosmic.cosmicconnect.Plugins.ClipboardPlugin.ClipboardPlugin::class.java to StaticPluginMetadata(
                pluginKey = "ClipboardPlugin",
                supportedPacketTypes = arrayOf("cconnect.clipboard", "cconnect.clipboard.connect"),
                outgoingPacketTypes = arrayOf("cconnect.clipboard", "cconnect.clipboard.connect"),
                displayNameRes = R.string.pref_plugin_clipboard,
                descriptionRes = R.string.pref_plugin_clipboard_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.SystemVolumePlugin.SystemVolumePlugin::class.java to StaticPluginMetadata(
                pluginKey = "SystemVolumePlugin",
                supportedPacketTypes = arrayOf("cconnect.systemvolume"),
                outgoingPacketTypes = arrayOf("cconnect.systemvolume.request"),
                displayNameRes = R.string.pref_plugin_systemvolume,
                descriptionRes = R.string.pref_plugin_systemvolume_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.RemoteKeyboardPlugin.RemoteKeyboardPlugin::class.java to StaticPluginMetadata(
                pluginKey = "RemoteKeyboardPlugin",
                supportedPacketTypes = arrayOf("cconnect.mousepad.request"),
                outgoingPacketTypes = arrayOf("cconnect.mousepad.echo", "cconnect.mousepad.keyboardstate"),
                displayNameRes = R.string.pref_plugin_remotekeyboard,
                descriptionRes = R.string.pref_plugin_remotekeyboard_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.RunCommandPlugin.RunCommandPlugin::class.java to StaticPluginMetadata(
                pluginKey = "RunCommandPlugin",
                supportedPacketTypes = arrayOf("cconnect.runcommand"),
                outgoingPacketTypes = arrayOf("cconnect.runcommand.request"),
                displayNameRes = R.string.pref_plugin_runcommand,
                descriptionRes = R.string.pref_plugin_runcommand_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.OpenPlugin.OpenOnDesktopPlugin::class.java to StaticPluginMetadata(
                pluginKey = "OpenOnDesktopPlugin",
                supportedPacketTypes = arrayOf("cconnect.open.response", "cconnect.open.capability"),
                outgoingPacketTypes = arrayOf("cconnect.open.request", "cconnect.open.capability"),
                displayNameRes = R.string.pref_plugin_open_on_desktop,
                descriptionRes = R.string.pref_plugin_open_on_desktop_desc,
            ),
            // --- Wave 3 ---
            org.cosmic.cosmicconnect.Plugins.BatteryPlugin.BatteryPluginFFI::class.java to StaticPluginMetadata(
                pluginKey = "BatteryPluginFFI",
                supportedPacketTypes = arrayOf("cconnect.battery"),
                outgoingPacketTypes = arrayOf("cconnect.battery"),
                displayNameRes = R.string.pref_plugin_battery,
                descriptionRes = R.string.pref_plugin_battery_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin::class.java to StaticPluginMetadata(
                pluginKey = "FindMyPhonePlugin",
                supportedPacketTypes = arrayOf("cconnect.findmyphone.request"),
                outgoingPacketTypes = emptyArray(),
                displayNameRes = R.string.findmyphone_title,
                descriptionRes = R.string.findmyphone_description,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.ContactsPlugin.ContactsPlugin::class.java to StaticPluginMetadata(
                pluginKey = "ContactsPlugin",
                supportedPacketTypes = arrayOf("cconnect.contacts.request_all_uids_timestamps", "cconnect.contacts.request_vcards_by_uid"),
                outgoingPacketTypes = arrayOf("cconnect.contacts.response_uids_timestamps", "cconnect.contacts.response_vcards"),
                displayNameRes = R.string.pref_plugin_contacts,
                descriptionRes = R.string.pref_plugin_contacts_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.TelephonyPlugin.TelephonyPlugin::class.java to StaticPluginMetadata(
                pluginKey = "TelephonyPlugin",
                supportedPacketTypes = arrayOf("cconnect.telephony.request_mute"),
                outgoingPacketTypes = arrayOf("cconnect.telephony"),
                displayNameRes = R.string.pref_plugin_telephony,
                descriptionRes = R.string.pref_plugin_telephony_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.SMSPlugin.SMSPlugin::class.java to StaticPluginMetadata(
                pluginKey = "SMSPlugin",
                supportedPacketTypes = arrayOf("cconnect.sms.request", "cconnect.sms.request_conversations", "cconnect.sms.request_conversation", "cconnect.sms.request_attachment"),
                outgoingPacketTypes = arrayOf("cconnect.sms.messages", "cconnect.sms.attachment_file"),
                displayNameRes = R.string.pref_plugin_telepathy,
                descriptionRes = R.string.pref_plugin_telepathy_desc,
                hasSettings = true,
            ),
        )

        /**
         * Plugins still using reflection-based instantiation.
         * As plugins are migrated, move them from here to [migratedPlugins].
         */
        private val legacyPlugins = listOf(
            org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisPlugin::class,
            org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationsPlugin::class,
            org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin.ReceiveNotificationsPlugin::class,
            org.cosmic.cosmicconnect.Plugins.SharePlugin.SharePlugin::class,
            org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpPlugin::class,
            // App Continuity plugins (Issues #112-123)
            org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin.OpenOnPhonePlugin::class,
            // Camera Webcam plugin (Issues #102-111)
            org.cosmic.cosmicconnect.Plugins.CameraPlugin.CameraPlugin::class,
            // Extended Display plugin (Issue #138)
            org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ExtendedDisplayPlugin::class,
        )
    }
}
