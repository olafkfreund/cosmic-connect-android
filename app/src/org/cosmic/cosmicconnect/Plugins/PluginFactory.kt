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
    private var pluginInfo: Map<String, PluginInfo> = mapOf()

    init {
        initPluginInfo()
    }

    fun initPluginInfo() {
        val result = mutableMapOf<String, PluginInfo>()

        for ((pluginClass, metadata) in pluginRegistry) {
            result[metadata.pluginKey] = PluginInfo(context, metadata, pluginClass)
            Log.d("PluginFactory", "Loaded: ${metadata.pluginKey}")
        }

        pluginInfo = result
        Log.i("PluginFactory", "Loaded ${pluginInfo.size} plugins")
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
            return pluginCreators[pluginKey]?.create(device)
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

    class PluginInfo internal constructor(
        val displayName: String,
        val description: String,
        val isEnabledByDefault: Boolean,
        val hasSettings: Boolean,
        val listenToUnpaired: Boolean,
        supportedPacketTypes: Array<String>,
        outgoingPacketTypes: Array<String>,
    ) {
        internal constructor(context: Context, metadata: StaticPluginMetadata, pluginClass: Class<out Plugin>) : this(
            displayName = context.getString(metadata.displayNameRes),
            description = context.getString(metadata.descriptionRes),
            isEnabledByDefault = metadata.isEnabledByDefault,
            hasSettings = metadata.hasSettings,
            listenToUnpaired = metadata.listenToUnpaired,
            supportedPacketTypes = metadata.supportedPacketTypes,
            outgoingPacketTypes = metadata.outgoingPacketTypes,
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
         * Plugin registry with static metadata.
         * No reflection needed — metadata is provided at compile time.
         * Instance creation uses the Hilt PluginCreator map.
         */
        private val pluginRegistry: Map<Class<out Plugin>, StaticPluginMetadata> = mapOf(
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
            // --- Wave 4 ---
            org.cosmic.cosmicconnect.Plugins.SharePlugin.SharePlugin::class.java to StaticPluginMetadata(
                pluginKey = "SharePlugin",
                supportedPacketTypes = arrayOf("cconnect.share.request", "cconnect.share.request.update"),
                outgoingPacketTypes = arrayOf("cconnect.share.request"),
                displayNameRes = R.string.pref_plugin_sharereceiver,
                descriptionRes = R.string.pref_plugin_sharereceiver_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpPlugin::class.java to StaticPluginMetadata(
                pluginKey = "SftpPlugin",
                supportedPacketTypes = arrayOf("cconnect.sftp.request"),
                outgoingPacketTypes = arrayOf("cconnect.sftp"),
                displayNameRes = R.string.pref_plugin_sftp,
                descriptionRes = R.string.pref_plugin_sftp_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisPlugin::class.java to StaticPluginMetadata(
                pluginKey = "MprisPlugin",
                supportedPacketTypes = arrayOf("cconnect.mpris"),
                outgoingPacketTypes = arrayOf("cconnect.mpris.request"),
                displayNameRes = R.string.pref_plugin_mpris,
                descriptionRes = R.string.pref_plugin_mpris_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationsPlugin::class.java to StaticPluginMetadata(
                pluginKey = "NotificationsPlugin",
                supportedPacketTypes = arrayOf("cconnect.notification.request", "cconnect.notification.reply", "cconnect.notification.action"),
                outgoingPacketTypes = arrayOf("cconnect.notification"),
                displayNameRes = R.string.pref_plugin_notifications,
                descriptionRes = R.string.pref_plugin_notifications_desc,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin.ReceiveNotificationsPlugin::class.java to StaticPluginMetadata(
                pluginKey = "ReceiveNotificationsPlugin",
                supportedPacketTypes = arrayOf("cconnect.notification"),
                outgoingPacketTypes = arrayOf("cconnect.notification.request"),
                displayNameRes = R.string.pref_plugin_receive_notifications,
                descriptionRes = R.string.pref_plugin_receive_notifications_desc,
                isEnabledByDefault = false,
            ),
            // --- Wave 5 ---
            org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin.OpenOnPhonePlugin::class.java to StaticPluginMetadata(
                pluginKey = "OpenOnPhonePlugin",
                supportedPacketTypes = arrayOf("cconnect.open.request", "cconnect.open.capability"),
                outgoingPacketTypes = arrayOf("cconnect.open.response", "cconnect.open.capability"),
                displayNameRes = R.string.pref_plugin_open,
                descriptionRes = R.string.pref_plugin_open_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.CameraPlugin.CameraPlugin::class.java to StaticPluginMetadata(
                pluginKey = "CameraPlugin",
                supportedPacketTypes = arrayOf("cconnect.camera.start", "cconnect.camera.request", "cconnect.camera.stop", "cconnect.camera.settings"),
                outgoingPacketTypes = arrayOf("cconnect.camera.capability", "cconnect.camera.status", "cconnect.camera.frame"),
                displayNameRes = R.string.camera_plugin_title,
                descriptionRes = R.string.camera_plugin_description,
                hasSettings = true,
            ),
            org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ExtendedDisplayPlugin::class.java to StaticPluginMetadata(
                pluginKey = "ExtendedDisplayPlugin",
                supportedPacketTypes = arrayOf("cconnect.extendeddisplay"),
                outgoingPacketTypes = arrayOf("cconnect.extendeddisplay.request"),
                displayNameRes = R.string.pref_plugin_extended_display,
                descriptionRes = R.string.pref_plugin_extended_display_desc,
                isEnabledByDefault = false,
                hasSettings = true,
            ),
            // --- Wave 6 (desktop plugin parity #145) ---
            org.cosmic.cosmicconnect.Plugins.NetworkInfoPlugin.NetworkInfoPlugin::class.java to StaticPluginMetadata(
                pluginKey = "NetworkInfoPlugin",
                supportedPacketTypes = arrayOf("cconnect.networkinfo.request"),
                outgoingPacketTypes = arrayOf("cconnect.networkinfo"),
                displayNameRes = R.string.pref_plugin_networkinfo,
                descriptionRes = R.string.pref_plugin_networkinfo_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.PowerPlugin.PowerPlugin::class.java to StaticPluginMetadata(
                pluginKey = "PowerPlugin",
                supportedPacketTypes = arrayOf("cconnect.power"),
                outgoingPacketTypes = arrayOf("cconnect.power.request"),
                displayNameRes = R.string.pref_plugin_power,
                descriptionRes = R.string.pref_plugin_power_desc,
            ),
            org.cosmic.cosmicconnect.Plugins.LockPlugin.LockPlugin::class.java to StaticPluginMetadata(
                pluginKey = "LockPlugin",
                supportedPacketTypes = arrayOf("cconnect.lock"),
                outgoingPacketTypes = arrayOf("cconnect.lock.request"),
                displayNameRes = R.string.pref_plugin_lock,
                descriptionRes = R.string.pref_plugin_lock_desc,
            ),
            // --- Wave 7 ---
            org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ScreenSharePlugin::class.java to StaticPluginMetadata(
                pluginKey = "ScreenSharePlugin",
                supportedPacketTypes = arrayOf("cconnect.screenshare", "cconnect.screenshare.start", "cconnect.screenshare.stop"),
                outgoingPacketTypes = arrayOf("cconnect.screenshare.request", "cconnect.screenshare.ready"),
                displayNameRes = R.string.pref_plugin_screenshare,
                descriptionRes = R.string.pref_plugin_screenshare_desc,
                isEnabledByDefault = false,
            ),
            org.cosmic.cosmicconnect.Plugins.FileSyncPlugin.FileSyncPlugin::class.java to StaticPluginMetadata(
                pluginKey = "FileSyncPlugin",
                supportedPacketTypes = arrayOf("cconnect.filesync", "cconnect.filesync.conflict"),
                outgoingPacketTypes = arrayOf("cconnect.filesync.request"),
                displayNameRes = R.string.pref_plugin_filesync,
                descriptionRes = R.string.pref_plugin_filesync_desc,
                isEnabledByDefault = false,
            ),
            org.cosmic.cosmicconnect.Plugins.VirtualMonitorPlugin.VirtualMonitorPlugin::class.java to StaticPluginMetadata(
                pluginKey = "VirtualMonitorPlugin",
                supportedPacketTypes = arrayOf("cconnect.virtualmonitor"),
                outgoingPacketTypes = arrayOf("cconnect.virtualmonitor.request"),
                displayNameRes = R.string.pref_plugin_virtualmonitor,
                descriptionRes = R.string.pref_plugin_virtualmonitor_desc,
                isEnabledByDefault = false,
            ),
            org.cosmic.cosmicconnect.Plugins.AudioStreamPlugin.AudioStreamPlugin::class.java to StaticPluginMetadata(
                pluginKey = "AudioStreamPlugin",
                supportedPacketTypes = arrayOf("cconnect.audiostream", "cconnect.audiostream.capability"),
                outgoingPacketTypes = arrayOf("cconnect.audiostream.request"),
                displayNameRes = R.string.pref_plugin_audiostream,
                descriptionRes = R.string.pref_plugin_audiostream_desc,
                isEnabledByDefault = false,
            ),
            // --- Wave 8 (webcam #158) ---
            org.cosmic.cosmicconnect.Plugins.WebcamPlugin.WebcamPlugin::class.java to StaticPluginMetadata(
                pluginKey = "WebcamPlugin",
                supportedPacketTypes = arrayOf("cconnect.webcam.request", "cconnect.webcam.capability"),
                outgoingPacketTypes = arrayOf("cconnect.webcam", "cconnect.webcam.capability"),
                displayNameRes = R.string.pref_plugin_webcam,
                descriptionRes = R.string.pref_plugin_webcam_desc,
                isEnabledByDefault = false,
            ),
        )
    }
}
