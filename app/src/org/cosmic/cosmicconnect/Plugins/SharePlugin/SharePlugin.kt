/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.SharePlugin

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.LocusIdCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.BundleCompat
import androidx.preference.PreferenceManager
import org.apache.commons.lang3.ArrayUtils
import org.cosmic.cosmicconnect.Helpers.FilesHelper
import org.cosmic.cosmicconnect.Helpers.IntentHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.cosmic.cosmicconnect.async.BackgroundJob
import org.cosmic.cosmicconnect.async.BackgroundJobHandler
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL

/**
 * A Plugin for sharing and receiving files and uris.
 *
 * All of the associated I/O work is scheduled on background
 * threads by [BackgroundJobHandler].
 */
@LoadablePlugin
class SharePlugin : Plugin() {

    private val backgroundJobHandler: BackgroundJobHandler = BackgroundJobHandler.newFixedThreadPoolBackgroundJobHandler(5)
    private val handler: Handler = Handler(Looper.getMainLooper())

    private var receiveFileJob: CompositeReceiveFileJob? = null
    private var uploadFileJob: CompositeUploadFileJob? = null
    private val receiveFileJobCallback = JobCallback()

    private var mSharedPrefs: SharedPreferences? = null

    override fun onCreate(): Boolean {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        createOrUpdateDynamicShortcut(null)
        // Deliver URLs previously shared to this device now that it's connected
        deliverPreviouslySentIntents()
        return true
    }

    override fun onDestroy() {
        val shortcuts = ShortcutManagerCompat.getDynamicShortcuts(context)
        for (shortcut in shortcuts) {
            if (shortcut.id != device.deviceId) continue
            if (!device.isReachable && shortcut.isPinned) {
                // Create an updated shortcut with the same ID
                createOrUpdateDynamicShortcut(shortcut)
                break
            } else {
                ShortcutManagerCompat.removeLongLivedShortcuts(context, listOf(shortcut.id))
            }
        }
        super.onDestroy()
    }

    private fun createOrUpdateDynamicShortcut(shortcutToUpdate: ShortcutInfoCompat?) {
        val isNewShortcut = shortcutToUpdate == null
        val icon = IconCompat.createWithResource(context, device.deviceType.toShortcutDrawableId())
        var shortcutIntent: Intent? = null
        if (isNewShortcut) {
            shortcutIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(MainActivity.EXTRA_DEVICE_ID, device.deviceId)
            }
        }
        val categories = if (isNewShortcut) {
            setOf("org.cosmic.cosmicconnect.category.SHARE_TARGET")
        } else {
            shortcutToUpdate!!.categories ?: emptySet()
        }

        val shortcut = ShortcutInfoCompat.Builder(context, device.deviceId)
            .setIntent(if (isNewShortcut) shortcutIntent!! else shortcutToUpdate!!.intent)
            .setIcon(icon)
            .setShortLabel(
                if (isNewShortcut) device.name
                else context.getString(R.string.unreachable_device_dynamic_shortcut, shortcutToUpdate!!.shortLabel)
            )
            .setCategories(categories)
            .setLocusId(
                if (isNewShortcut) LocusIdCompat(device.deviceId)
                else shortcutToUpdate!!.locusId
            )
            .build()

        if (isNewShortcut) {
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        } else {
            ShortcutManagerCompat.updateShortcuts(context, listOf(shortcut))
        }
    }

    private fun deliverPreviouslySentIntents() {
        val currentUrlSet = mSharedPrefs?.getStringSet(KEY_UNREACHABLE_URL_LIST + device.deviceId, null)
        if (currentUrlSet != null) {
            for (url in currentUrlSet) {
                try {
                    val intent = Intent.parseUri(url, 0).apply {
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    share(intent)
                } catch (ex: URISyntaxException) {
                    Log.e("SharePlugin", "Malformed URI")
                    continue
                }
            }
            mSharedPrefs?.edit()?.putStringSet(KEY_UNREACHABLE_URL_LIST + device.deviceId, null)?.apply()
        }
    }

    override val optionalPermissionExplanation: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            R.string.share_notifications_explanation
        } else {
            R.string.share_optional_permission_explanation
        }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_sharereceiver)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_sharereceiver_desc)

    override fun getUiButtons(): List<PluginUiButton> {
        return listOf(PluginUiButton(context.getString(R.string.send_files), R.drawable.share_plugin_action_24dp) { parentActivity ->
            val intent = Intent(parentActivity, SendFileActivity::class.java).apply {
                putExtra("deviceId", device.deviceId)
            }
            parentActivity.startActivity(intent)
        })
    }

    override fun hasSettings(): Boolean = true

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        try {
            if (np.type == PACKET_TYPE_SHARE_REQUEST_UPDATE) {
                val job = receiveFileJob
                if (job != null && job.isRunning()) {
                    job.updateTotals(np.getInt(KEY_NUMBER_OF_FILES), np.getLong(KEY_TOTAL_PAYLOAD_SIZE))
                } else {
                    Log.d("SharePlugin", "Received update packet but CompositeUploadJob is null or not running")
                }
                return true
            }

            // Check packet type using legacy NetworkPacket methods
            when {
                np.has("filename") -> receiveFile(np)
                np.has("text") -> {
                    Log.i("SharePlugin", "hasText")
                    receiveText(np)
                }
                np.has("url") -> receiveUrl(np)
                else -> Log.e("SharePlugin", "Error: Nothing attached!")
            }
        } catch (e: Exception) {
            Log.e("SharePlugin", "Exception", e)
        }
        return true
    }

    private fun receiveUrl(np: NetworkPacket) {
        val url = np.getString("url")
        if (url == null) {
            Log.e("SharePlugin", "URL is null")
            return
        }

        Log.i("SharePlugin", "hasUrl: $url")

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        IntentHelper.startActivityFromBackgroundOrCreateNotification(context, browserIntent, url)
    }

    private fun receiveText(np: NetworkPacket) {
        val text = np.getString("text")
        if (text == null) {
            Log.e("SharePlugin", "Text is null")
            return
        }

        val cm = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        cm?.setText(text)
        handler.post { Toast.makeText(context, R.string.shareplugin_text_saved, Toast.LENGTH_LONG).show() }
    }

    private fun receiveFile(np: NetworkPacket) {
        val hasNumberOfFiles = np.has(KEY_NUMBER_OF_FILES)
        val isOpen = np.getBoolean("open", false)

        val job: CompositeReceiveFileJob = if (hasNumberOfFiles && !isOpen && receiveFileJob != null) {
            receiveFileJob!!
        } else {
            CompositeReceiveFileJob(device, receiveFileJobCallback)
        }

        if (!hasNumberOfFiles) {
            np.set(KEY_NUMBER_OF_FILES, 1)
            np.set(KEY_TOTAL_PAYLOAD_SIZE, np.payloadSize)
        }

        job.addNetworkPacket(np)

        if (job != receiveFileJob) {
            if (hasNumberOfFiles && !isOpen) {
                receiveFileJob = job
            }
            backgroundJobHandler.runJob(job)
        }
    }

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return ShareSettingsFragment.newInstance(pluginKey, R.xml.shareplugin_preferences)
    }

    fun sendUriList(uriList: ArrayList<Uri>) {
        val job: CompositeUploadFileJob = uploadFileJob ?: CompositeUploadFileJob(device, receiveFileJobCallback)

        // Read all the data early, as we only have permissions to do it while the activity is alive
        for (uri in uriList) {
            val np = FilesHelper.uriToNetworkPacket(context, uri, PACKET_TYPE_SHARE_REQUEST)
            if (np != null) {
                job.addNetworkPacket(np)
            }
        }

        if (job != uploadFileJob) {
            uploadFileJob = job
            backgroundJobHandler.runJob(job)
        }
    }

    fun share(intent: Intent) {
        val extras = intent.extras
        val streams = streamsFromIntent(intent, extras)
        if (!streams.isNullOrEmpty()) {
            sendUriList(streams)
            return
        }
        if (extras != null) {
            var text = extras.getString(Intent.EXTRA_TEXT)
            if (!text.isNullOrEmpty()) {
                Log.i("SharePlugin", "Intent contains text to share")

                // Hack: Detect shared youtube videos, so we can open them in the browser instead of as text
                val subject = extras.getString(Intent.EXTRA_SUBJECT)
                if (subject != null && subject.endsWith("YouTube")) {
                    val index = text.indexOf(": http://youtu.be/")
                    if (index > 0) {
                        text = text.substring(index + 2) // Skip ": "
                    }
                }

                val isUrl = try {
                    URL(text)
                    true
                } catch (e: MalformedURLException) {
                    false
                }

                // Create packet using FFI wrappers
                val ffiPacket = if (isUrl) {
                    SharePacketsFFI.createUrlShare(text)
                } else {
                    SharePacketsFFI.createTextShare(text)
                }

                // Convert and send
                device.sendPacket(ffiPacket.toLegacyPacket())
                return
            }
        }
        Log.e("SharePlugin", "There's nothing we know how to share")
    }

    private fun streamsFromIntent(intent: Intent, extras: Bundle?): ArrayList<Uri>? {
        if (extras == null || !extras.containsKey(Intent.EXTRA_STREAM)) {
            return null
        }
        Log.i("SharePlugin", "Intent contains streams to share")
        val uriList: ArrayList<Uri> = if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) ?: ArrayList()
        } else {
            val list = ArrayList<Uri>()
            BundleCompat.getParcelable(extras, Intent.EXTRA_STREAM, Uri::class.java)?.let { list.add(it) }
            list
        }
        uriList.removeAll(java.util.Collections.singleton(null))
        if (uriList.isEmpty()) {
            Log.w("SharePlugin", "All streams were null")
        }
        return uriList
    }

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_SHARE_REQUEST, PACKET_TYPE_SHARE_REQUEST_UPDATE)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_SHARE_REQUEST)

    override val optionalPermissions: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> ArrayUtils.EMPTY_STRING_ARRAY
            else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

    private inner class JobCallback : BackgroundJob.Callback<java.lang.Void?> {
        override fun onResult(job: BackgroundJob<*, *>, result: java.lang.Void?) {
            if (job === receiveFileJob) {
                receiveFileJob = null
            } else if (job === uploadFileJob) {
                uploadFileJob = null
            }
        }

        override fun onError(job: BackgroundJob<*, *>, error: Throwable) {
            if (job === receiveFileJob) {
                receiveFileJob = null
            } else if (job === uploadFileJob) {
                uploadFileJob = null
            }
        }
    }

    fun cancelJob(jobId: Long) {
        if (backgroundJobHandler.isRunning(jobId)) {
            val job = backgroundJobHandler.getJob(jobId)
            if (job != null) {
                job.cancel()
                if (job === receiveFileJob) {
                    receiveFileJob = null
                } else if (job === uploadFileJob) {
                    uploadFileJob = null
                }
            }
        }
    }

    override fun onDeviceUnpaired(context: Context, deviceId: String) {
        Log.i("COSMIC/SharePlugin", "onDeviceUnpaired deviceId = $deviceId")
        if (mSharedPrefs == null) {
            mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        }
        mSharedPrefs?.edit()?.remove(KEY_UNREACHABLE_URL_LIST + deviceId)?.apply()
    }

    companion object {
        const val ACTION_CANCEL_SHARE = "org.cosmic.cosmicconnect.Plugins.SharePlugin.CancelShare"
        const val CANCEL_SHARE_DEVICE_ID_EXTRA = "deviceId"
        const val CANCEL_SHARE_BACKGROUND_JOB_ID_EXTRA = "backgroundJobId"

        private const val PACKET_TYPE_SHARE_REQUEST = "cconnect.share.request"
        const val PACKET_TYPE_SHARE_REQUEST_UPDATE = "cconnect.share.request.update"

        const val KEY_NUMBER_OF_FILES = "numberOfFiles"
        const val KEY_TOTAL_PAYLOAD_SIZE = "totalPayloadSize"

        const val KEY_UNREACHABLE_URL_LIST = "key_unreachable_url_list"
    }
}
