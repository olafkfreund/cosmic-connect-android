/*
 * SPDX-FileCopyrightText: 2018 Nicolas Fella <nicolas.fella@gmx.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.MprisReceiverPlugin

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import org.apache.commons.lang3.StringUtils
import org.cosmic.cosmicconnect.Helpers.AppsHelper
import org.cosmic.cosmicconnect.Helpers.ThreadHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationReceiver
import org.json.JSONObject
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import org.cosmic.cosmicconnect.UserInterface.StartActivityAlertDialogFragment
import org.cosmic.cosmicconnect.R
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

@PluginFactory.LoadablePlugin
class MprisReceiverPlugin : Plugin(), NotificationReceiver.NotificationListener {

    // TODO: Those two are always accessed together, merge them
    private var players = HashMap<String, MprisReceiverPlayer>()
    private var playerCbs = HashMap<String, MprisReceiverCallback>()

    private var mediaSessionChangeListener: MediaSessionChangeListener? = null

    val deviceIdValue: String
        get() = device.deviceId

    override fun onCreate(): Boolean {
        if (!hasPermission()) return false
        
        players = HashMap()
        playerCbs = HashMap()
        try {
            val manager = ContextCompat.getSystemService(context, MediaSessionManager::class.java)
                ?: return false

            assert(mediaSessionChangeListener == null)
            mediaSessionChangeListener = MediaSessionChangeListener()
            manager.addOnActiveSessionsChangedListener(
                mediaSessionChangeListener!!,
                ComponentName(context, NotificationReceiver::class.java),
                Handler(Looper.getMainLooper())
            )

            createPlayers(manager.getActiveSessions(ComponentName(context, NotificationReceiver::class.java)))
            sendPlayerList()
        } catch (e: Exception) {
            Log.e(TAG, "Exception", e)
        }

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        val manager = ContextCompat.getSystemService(context, MediaSessionManager::class.java)
        if (manager != null && mediaSessionChangeListener != null) {
            manager.removeOnActiveSessionsChangedListener(mediaSessionChangeListener!!)
            mediaSessionChangeListener = null
        }
    }

    private fun createPlayers(sessions: List<MediaController>) {
        for (controller in sessions) {
            createPlayer(controller)
        }
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_mprisreceiver)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_mprisreceiver_desc)

    override fun onPacketReceived(np: org.cosmic.cosmicconnect.NetworkPacket): Boolean {
        if (np.getBoolean("requestPlayerList")) {
            sendPlayerList()
            return true
        }

        if (!np.has("player")) {
            return false
        }
        val player = players[np.getString("player")] ?: return false

        val artUrl = np.getString("albumArtUrl", "")
        if (artUrl.isNotEmpty()) {
            val playerName = player.name
            val cb = playerCbs[playerName]
            if (cb == null) {
                Log.e(TAG, "no callback for $playerName (player likely stopped)")
                return false
            }
            // run it on a different thread to avoid blocking
            ThreadHelper.execute { sendAlbumArt(playerName, cb, artUrl) }
            return true
        }

        if (np.getBoolean("requestNowPlaying", false)) {
            sendMetadata(player)
            return true
        }

        if (np.has("SetPosition")) {
            val position = np.getLong("SetPosition", 0)
            player.position = position
        }

        if (np.has("setVolume")) {
            val volume = np.getInt("setVolume", 100)
            player.volume = volume
            // Setting volume doesn't seem to always trigger the callback
            sendMetadata(player)
        }

        if (np.has("action")) {
            val action = np.getString("action")
            when (action) {
                "Play" -> player.play()
                "Pause" -> player.pause()
                "PlayPause" -> player.playPause()
                "Next" -> player.next()
                "Previous" -> player.previous()
                "Stop" -> player.stop()
            }
        }

        return true
    }

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_MPRIS_REQUEST)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_MPRIS)

    private inner class MediaSessionChangeListener : MediaSessionManager.OnActiveSessionsChangedListener {
        override fun onActiveSessionsChanged(controllers: List<MediaController>?) {
            if (controllers == null) return

            // Make a copy to avoid ConcurrentModificationException
            val playersCopy = ArrayList(players.values)
            for (p in playersCopy) {
                p.controller.unregisterCallback(playerCbs[p.name]!!)
            }
            playerCbs.clear()
            players.clear()

            createPlayers(controllers)
            sendPlayerList()
        }
    }

    private fun createPlayer(controller: MediaController) {
        // Skip the media session we created ourselves as COSMIC Connect
        if (controller.packageName == context.packageName) return

        val appName = AppsHelper.appNameLookup(context, controller.packageName) ?: controller.packageName
        val player = MprisReceiverPlayer(controller, appName)
        val cb = MprisReceiverCallback(this, player)
        controller.registerCallback(cb, Handler(Looper.getMainLooper()))
        playerCbs[player.name] = cb
        players[player.name] = player
    }

    private fun sendPlayerList() {
        // Build body
        val body = mutableMapOf<String, Any>()
        body["playerList"] = ArrayList(players.keys)
        body["supportAlbumArtPayload"] = true

        // Create packet using FFI
        val json = JSONObject(body).toString()
        val packet = MprisReceiverPacketsFFI.createMprisPacket(json)

        // Send packet
        device.sendPacket(packet.toLegacyPacket())
    }

    fun sendAlbumArt(playerName: String, cb: MprisReceiverCallback, requestedUrl: String?) {
        val localArtUrl = cb.artUrl ?: run {
            Log.w(TAG, "art not found!")
            return
        }
        val artUrlToUse = requestedUrl ?: localArtUrl
        if (requestedUrl != null && requestedUrl != localArtUrl) {
            Log.w(TAG, "sendAlbumArt: Doesn't match current url")
            Log.d(TAG, "current:   $localArtUrl")
            Log.d(TAG, "requested: $requestedUrl")
            return
        }
        val p = cb.artAsArray ?: run {
            Log.w(TAG, "sendAlbumArt: Failed to get art stream")
            return
        }

        // Build body
        val body = mutableMapOf<String, Any>()
        body["player"] = playerName
        body["transferringAlbumArt"] = true
        body["albumArtUrl"] = artUrlToUse

        // Create packet using FFI
        val json = JSONObject(body).toString()
        val packet = MprisReceiverPacketsFFI.createMprisPacket(json)

        // Convert to legacy and set payload
        val legacyPacket = packet.toLegacyPacket()
        p?.let { 
            legacyPacket.payload = org.cosmic.cosmicconnect.NetworkPacket.Payload(it)
        }

        // Send packet
        device.sendPacket(legacyPacket)
    }

    fun sendMetadata(player: MprisReceiverPlayer) {
        // Prepare all data
        val nowPlaying = Stream.of(player.artist, player.title)
            .filter { StringUtils.isNotEmpty(it) }.collect(Collectors.joining(" - "))
        var artUrl = ""
        val cb = playerCbs[player.name]
        if (cb != null) {
            cb.artUrl?.let { artUrl = it }
        }

        // Build body
        val body = mutableMapOf<String, Any>()
        body["player"] = player.name
        body["title"] = player.title
        body["artist"] = player.artist
        body["nowPlaying"] = nowPlaying // GSConnect 50 (so, Ubuntu 22.04) needs this
        body["album"] = player.album
        body["isPlaying"] = player.isPlaying()
        body["pos"] = player.position
        body["length"] = player.length
        body["canPlay"] = player.canPlay()
        body["canPause"] = player.canPause()
        body["canGoPrevious"] = player.canGoPrevious()
        body["canGoNext"] = player.canGoNext()
        body["canSeek"] = player.canSeek()
        body["volume"] = player.volume
        body["albumArtUrl"] = artUrl

        // Create packet using FFI
        val json = JSONObject(body).toString()
        val packet = MprisReceiverPacketsFFI.createMprisPacket(json)

        // Send packet
        device.sendPacket(packet.toLegacyPacket())
    }

    override fun onNotificationRemoved(statusBarNotification: android.service.notification.StatusBarNotification?) {
        // Not used
    }

    override fun onNotificationPosted(statusBarNotification: android.service.notification.StatusBarNotification) {
        // Not used
    }

    override fun onListenerConnected(service: NotificationReceiver) {
        // Not used
    }

    override val permissionExplanationDialog: DialogFragment
        get() {
            return StartActivityAlertDialogFragment.Builder()
                .setTitle(R.string.pref_plugin_mpris)
                .setMessage(R.string.no_permission_mprisreceiver)
                .setPositiveButton(R.string.open_settings)
                .setNegativeButton(R.string.cancel)
                .setIntentAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                .setStartForResult(true)
                .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
                .create()
        }

    private fun hasPermission(): Boolean {
        val notificationListenerList = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return notificationListenerList != null && notificationListenerList.contains(context.packageName)
    }

    companion object {
        private const val PACKET_TYPE_MPRIS = "cconnect.mpris"
        private const val PACKET_TYPE_MPRIS_REQUEST = "cconnect.mpris.request"
        private const val TAG = "MprisReceiver"
    }
}
