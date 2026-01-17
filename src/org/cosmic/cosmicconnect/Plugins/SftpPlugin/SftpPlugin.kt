/*
 * SPDX-FileCopyrightText: 2014 Samoilenko Yuri <kinnalru@gmail.com>
 * SPDX-FileCopyrightText: 2024 ShellWen Chen <me@shellwen.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Plugins.SftpPlugin

import android.app.Activity
import android.content.ContentResolver
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.core.net.toUri
import org.json.JSONException
import org.json.JSONObject
import org.cosmic.cosmicconnect.Helpers.NetworkHelper.localIpAddress
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.UserInterface.AlertDialogFragment
import org.cosmic.cosmicconnect.UserInterface.DeviceSettingsAlertDialogFragment
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.UserInterface.StartActivityAlertDialogFragment
import org.cosmic.cosmicconnect.BuildConfig
import org.cosmic.cosmicconnect.R
import java.security.GeneralSecurityException

@LoadablePlugin
class SftpPlugin : Plugin(), OnSharedPreferenceChangeListener {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_sftp)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_sftp_desc)

    override fun checkRequiredPermissions(): Boolean {
        return if (SimpleSftpServer.SUPPORTS_NATIVEFS) {
            Environment.isExternalStorageManager()
        } else {
            SftpSettingsFragment.getStorageInfoList(context, this).isNotEmpty()
        }
    }

    override val permissionExplanationDialog: AlertDialogFragment
        get() = if (SimpleSftpServer.SUPPORTS_NATIVEFS) {
            StartActivityAlertDialogFragment.Builder()
                .setTitle(displayName)
                .setMessage(R.string.sftp_manage_storage_permission_explanation)
                .setPositiveButton(R.string.open_settings)
                .setNegativeButton(R.string.cancel)
                .setIntentAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setIntentUrl("package:" + BuildConfig.APPLICATION_ID)
                .setStartForResult(true)
                .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
                .create()
        } else {
            DeviceSettingsAlertDialogFragment.Builder()
                .setTitle(displayName)
                .setMessage(R.string.sftp_saf_permission_explanation)
                .setPositiveButton(R.string.ok)
                .setNegativeButton(R.string.cancel)
                .setDeviceId(device.deviceId)
                .setPluginKey(pluginKey)
                .create()
        }

    override fun onDestroy() {
        server.stop()
        preferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun loadPluginWhenRequiredPermissionsMissing() = true

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        if (!np.getBoolean("startBrowsing")) return false

        if (!checkRequiredPermissions()) {
            val json = JSONObject(mapOf(
                "errorMessage" to context.getString(R.string.sftp_missing_permission_error)
            )).toString()
            val packet = SftpPacketsFFI.createSftpPacket(json)
            device.sendPacket(packet.toLegacyPacket())
            return true
        }

        if (!server.isInitialized || server.isClosed) {
            server.initialize(context, device)
        }

        val paths = mutableListOf<String>()
        val pathNames = mutableListOf<String>()

        if (SimpleSftpServer.SUPPORTS_NATIVEFS) {
            val volumes = context.getSystemService(
                StorageManager::class.java
            ).storageVolumes
            for (sv in volumes) {
                pathNames.add(sv.getDescription(context))
                paths.add(sv.directory!!.path)
            }
        } else {
            val storageInfoList = SftpSettingsFragment.getStorageInfoList(context, this)
            storageInfoList.sortBy { it.uri }
            if (storageInfoList.isEmpty()) {
                val json = JSONObject(mapOf(
                    "errorMessage" to context.getString(R.string.sftp_no_storage_locations_configured)
                )).toString()
                val packet = SftpPacketsFFI.createSftpPacket(json)
                device.sendPacket(packet.toLegacyPacket())
                return true
            }
            getPathsAndNamesForStorageInfoList(paths, pathNames, storageInfoList)
            storageInfoList.removeChildren()
            server.setSafRoots(storageInfoList)
        }

        if (!server.start()) {
            return false
        }

        if (preferences != null) {
            preferences!!.registerOnSharedPreferenceChangeListener(this)
        }

        // Build packet body with required fields
        val body = mutableMapOf<String, Any>(
            "ip" to localIpAddress!!.hostAddress!!,
            "port" to server.port,
            "user" to SimpleSftpServer.USER,
            "password" to server.regeneratePassword(),
            // Kept for compatibility, in case "multiPaths" is not possible or the other end does not support it
            "path" to if (paths.size == 1) paths[0] else "/"
        )

        // Add optional multiPaths fields if paths available
        if (paths.isNotEmpty()) {
            body["multiPaths"] = paths
            body["pathNames"] = pathNames
        }

        val json = JSONObject(body).toString()
        val packet = SftpPacketsFFI.createSftpPacket(json)
        device.sendPacket(packet.toLegacyPacket())

        return true
    }

    private fun getPathsAndNamesForStorageInfoList(
        paths: MutableList<String>,
        pathNames: MutableList<String>,
        storageInfoList: List<StorageInfo>
    ) {
        var prevInfo: StorageInfo? = null
        val pathBuilder = StringBuilder()

        for (curInfo in storageInfoList) {
            pathBuilder.setLength(0)
            pathBuilder.append("/")

            if (prevInfo != null && curInfo.uri.toString().startsWith(prevInfo.uri.toString())) {
                pathBuilder.append(prevInfo.displayName)
                pathBuilder.append("/")
                if (curInfo.uri.path != null && prevInfo.uri.path != null) {
                    pathBuilder.append(curInfo.uri.path!!.substring(prevInfo.uri.path!!.length))
                } else {
                    throw RuntimeException("curInfo.uri.getPath() or parentInfo.uri.getPath() returned null")
                }
            } else {
                pathBuilder.append(curInfo.displayName)

                if (prevInfo == null || !curInfo.uri.toString()
                        .startsWith(prevInfo.uri.toString())
                ) {
                    prevInfo = curInfo
                }
            }

            paths.add(pathBuilder.toString())
            pathNames.add(curInfo.displayName)
        }
    }

    private fun MutableList<StorageInfo>.removeChildren() {
        fun StorageInfo.isParentOf(other: StorageInfo): Boolean =
            other.uri.toString().startsWith(this.uri.toString())

        var currentParent: StorageInfo? = null

        retainAll { curInfo ->
            when {
                currentParent == null -> {
                    currentParent = curInfo
                    true
                }

                currentParent!!.isParentOf(curInfo) -> {
                    false
                }

                else -> {
                    currentParent = curInfo
                    true
                }
            }
        }
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_SFTP_REQUEST)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_SFTP)

    override fun hasSettings(): Boolean = !SimpleSftpServer.SUPPORTS_NATIVEFS

    override fun supportsDeviceSpecificSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return SftpSettingsFragment.newInstance(pluginKey, R.xml.sftpplugin_preferences)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key != context.getString(PREFERENCE_KEY_STORAGE_INFO_LIST)) return
        if (!server.isStarted) return

        server.stop()

        // Create immutable packet
        val packet = NetworkPacket.create(PACKET_TYPE_SFTP_REQUEST, mapOf(
            "startBrowsing" to true
        ))
        onPacketReceived(packet.toLegacyPacket())
    }

    data class StorageInfo(@JvmField var displayName: String, @JvmField val uri: Uri) {
        val isFileUri: Boolean = uri.scheme == ContentResolver.SCHEME_FILE
        val isContentUri: Boolean = uri.scheme == ContentResolver.SCHEME_CONTENT

        @Throws(JSONException::class)
        fun toJSON(): JSONObject {
            return JSONObject().apply {
                put(KEY_DISPLAY_NAME, displayName)
                put(KEY_URI, uri.toString())
            }
        }

        companion object {
            private const val KEY_DISPLAY_NAME = "DisplayName"
            private const val KEY_URI = "Uri"

            @JvmStatic
            @Throws(JSONException::class)
            fun fromJSON(jsonObject: JSONObject): StorageInfo { // TODO: Use Result after migrate callee to Kotlin
                val displayName = jsonObject.getString(KEY_DISPLAY_NAME)
                val uri = jsonObject.getString(KEY_URI).toUri()

                return StorageInfo(displayName, uri)
            }
        }
    }

    companion object {
        private const val PACKET_TYPE_SFTP = "cosmicconnect.sftp"
        private const val PACKET_TYPE_SFTP_REQUEST = "cosmicconnect.sftp.request"

        @JvmField
        val PREFERENCE_KEY_STORAGE_INFO_LIST: Int = R.string.sftp_preference_key_storage_info_list

        private val server = SimpleSftpServer()
    }
}
