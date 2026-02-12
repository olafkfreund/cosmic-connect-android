/*
 * SPDX-FileCopyrightText: 2015 David Edmundson <david@davidedmundson.co.uk>
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.FindMyPhonePlugin

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoints
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.DeviceType
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.LifecycleHelper
import org.cosmicext.connect.Helpers.NotificationHelper
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.PluginFactory
import org.cosmicext.connect.Plugins.di.PluginCreator
import org.cosmicext.connect.UserInterface.PluginSettingsFragment
import org.cosmicext.connect.R
import org.cosmicext.connect.di.HiltBridges
import java.io.IOException

/**
 * FindMyPhonePlugin - Make phone ring to help locate it
 *
 * This plugin allows COSMIC Desktop users to make their Android device ring
 * at maximum volume to help locate a lost or misplaced device.
 *
 * ## Protocol
 *
 * **Packet Type:**
 * - `cconnect.findmyphone.request` - Ring request (empty body)
 *
 * **Direction:**
 * - Desktop → Android: Send ring request
 * - Android: Receive request and ring
 *
 * ## Behavior
 *
 * When a ring request is received:
 * 1. MediaPlayer loads custom ringtone (or default)
 * 2. Volume set to maximum ALARM level (bypasses silent mode)
 * 3. Phone starts ringing in loop
 * 4. Notification shown with "Found It" action
 * 5. Screen kept dim (wake lock)
 * 6. User taps "Found It" or dismisses notification to stop
 *
 * ## Android Version Differences
 *
 * **Android < 10:**
 * - Launch FindMyPhoneActivity directly
 *
 * **Android 10+ (App in Background):**
 * - **Screen ON:** Show broadcast notification with "Found It" action
 * - **Screen OFF:** Show activity notification (launches activity)
 *
 * ## Audio Configuration
 *
 * - Stream: ALARM (bypasses silent/DND mode)
 * - Usage: USAGE_ALARM
 * - Looping: Yes (until stopped)
 * - Wake Mode: SCREEN_DIM_WAKE_LOCK
 * - Volume: Maximum ALARM volume (restored after)
 *
 * ## FFI Integration
 *
 * This plugin uses FindMyPhonePacketsFFI wrapper for type-safe packet inspection.
 * The plugin is receive-only (desktop sends requests, Android receives).
 *
 * @see FindMyPhonePacketsFFI
 * @see FindMyPhoneActivity
 * @see FindMyPhoneReceiver
 */
class FindMyPhonePlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): FindMyPhonePlugin
    }

    companion object {
        private const val TAG = "FindMyPhonePlugin"

        /**
         * Packet type for find my phone requests
         */
        const val PACKET_TYPE_FINDMYPHONE_REQUEST = "cconnect.findmyphone.request"
    }

    // ========================================================================
    // State Management
    // ========================================================================

    private var notificationManager: NotificationManager? = null
    private var notificationId: Int = 0
    private var audioManager: AudioManager? = null
    private var mediaPlayer: MediaPlayer? = null
    private var previousVolume: Int = -1
    private var powerManager: PowerManager? = null

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    override val displayName: String
        get() {
            val deviceHelper = EntryPoints.get(context.applicationContext, HiltBridges::class.java).deviceHelper()
            return when (deviceHelper.deviceType) {
                DeviceType.TV -> context.getString(R.string.findmyphone_title_tv)
                DeviceType.TABLET -> context.getString(R.string.findmyphone_title_tablet)
                DeviceType.PHONE -> context.getString(R.string.findmyphone_title)
                else -> context.getString(R.string.findmyphone_title)
            }
        }

    override val description: String
        get() = context.getString(R.string.findmyphone_description)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_FINDMYPHONE_REQUEST)

    override val outgoingPacketTypes: Array<String>
        get() = emptyArray() // Receive-only plugin

    // ========================================================================
    // Lifecycle
    // ========================================================================

    override fun onCreate(): Boolean {
        val deviceHelper = EntryPoints.get(context.applicationContext, HiltBridges::class.java).deviceHelper()
        val deviceType = deviceHelper.deviceType
        if (deviceType == DeviceType.PHONE || deviceType == DeviceType.TABLET) {
            // Initialize managers
            notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationId = System.currentTimeMillis().toInt()
            audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)
            powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)

            // Load ringtone preference
            val prefs: SharedPreferences = context.getSharedPreferences("${context.packageName}_preferences", android.content.Context.MODE_PRIVATE)
            val ringtoneString = prefs.getString(
                context.getString(R.string.findmyphone_preference_key_ringtone),
                ""
            ) ?: ""

            val ringtone = if (ringtoneString.isEmpty()) {
                Settings.System.DEFAULT_RINGTONE_URI
            } else {
                Uri.parse(ringtoneString)
            }

            // Initialize MediaPlayer
            return try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, ringtone)

                    // Configure audio attributes
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )

                    // Prevent screen from turning off (requires WAKE_LOCK permission)
                    @Suppress("DEPRECATION")
                    setWakeMode(context, PowerManager.SCREEN_DIM_WAKE_LOCK)

                    isLooping = true
                    prepare()
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MediaPlayer", e)
                false
            }
        }
        return true
    }

    override fun onDestroy() {
        // Stop playing if active
        if (mediaPlayer?.isPlaying == true) {
            stopPlaying()
        }

        // Cleanup resources
        audioManager = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ========================================================================
    // Packet Reception
    // ========================================================================

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet

        // Verify packet type using extension property
        if (!np.isFindMyPhoneRequest) {
            return false
        }

        // Handle based on Android version and app state
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || LifecycleHelper.isInForeground) {
            // Android < 10 OR app in foreground: Launch activity directly
            launchActivity()
        } else {
            // Android 10+ in background: Check permissions
            if (!checkOptionalPermissions()) {
                return false
            }

            // Different behavior based on screen state
            if (powerManager?.isInteractive == true) {
                // Screen ON: Start ringing immediately with broadcast notification
                startPlaying()
                showBroadcastNotification()
            } else {
                // Screen OFF: Show activity notification (launches when tapped)
                showActivityNotification()
            }
        }

        return true
    }

    // ========================================================================
    // Activity Launch
    // ========================================================================

    /**
     * Launch FindMyPhoneActivity
     *
     * Used on Android < 10 or when app is in foreground.
     * Activity handles MediaPlayer and UI directly.
     */
    private fun launchActivity() {
        val intent = Intent(context, FindMyPhoneActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(FindMyPhoneActivity.EXTRA_DEVICE_ID, device.deviceId)
        }
        context.startActivity(intent)
    }

    // ========================================================================
    // Notification Management
    // ========================================================================

    /**
     * Show broadcast notification with "Found It" action
     *
     * Used when screen is ON. The notification includes a broadcast receiver
     * action that stops ringing when tapped.
     */
    fun showBroadcastNotification() {
        val intent = Intent(context, FindMyPhoneReceiver::class.java).apply {
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            action = FindMyPhoneReceiver.ACTION_FOUND_IT
            putExtra(FindMyPhoneReceiver.EXTRA_DEVICE_ID, device.deviceId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotification(pendingIntent)
    }

    /**
     * Show activity notification
     *
     * Used when screen is OFF. The notification launches FindMyPhoneActivity
     * when tapped, which then handles ringing.
     */
    fun showActivityNotification() {
        val intent = Intent(context, FindMyPhoneActivity::class.java).apply {
            putExtra(FindMyPhoneActivity.EXTRA_DEVICE_ID, device.deviceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotification(pendingIntent)
    }

    /**
     * Create and show notification
     *
     * @param pendingIntent Action to perform when notification is tapped
     */
    private fun createNotification(pendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(context, NotificationHelper.Channels.HIGHPRIORITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentTitle(context.getString(R.string.findmyphone_found))
            .setGroup("BackgroundService")
            .build()

        notificationManager?.notify(notificationId, notification)
    }

    // ========================================================================
    // MediaPlayer Control
    // ========================================================================

    /**
     * Start playing ringtone
     *
     * Sets volume to maximum ALARM level (saves original) and starts MediaPlayer.
     * This bypasses silent mode and DND settings.
     */
    fun startPlaying() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) return

        val manager = audioManager ?: return

        // Save current volume and set to maximum
        previousVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        manager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            manager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        player.start()
    }

    /**
     * Stop playing ringtone
     *
     * Stops MediaPlayer, restores original volume, and re-prepares for next use.
     */
    fun stopPlaying() {
        val manager = audioManager
        if (manager == null) {
            // Plugin was destroyed (device disconnected)
            return
        }

        // Restore original volume
        if (previousVolume != -1) {
            manager.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
            previousVolume = -1
        }

        // Stop and re-prepare MediaPlayer
        val player = mediaPlayer ?: return
        player.stop()
        try {
            player.prepare()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to re-prepare MediaPlayer", e)
        }
    }

    /**
     * Check if ringtone is currently playing
     *
     * @return true if MediaPlayer is playing, false otherwise
     */
    internal fun isPlaying(): Boolean =
        mediaPlayer?.isPlaying == true

    /**
     * Hide notification
     *
     * Called when user dismisses or stops ringing.
     */
    fun hideNotification() {
        notificationManager?.cancel(notificationId)
    }

    // ========================================================================
    // Settings
    // ========================================================================

    override fun hasSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment =
        FindMyPhoneSettingsFragment.newInstance(pluginKey, R.xml.findmyphoneplugin_preferences)

    // ========================================================================
    // Permissions
    // ========================================================================

    override val requiredPermissions: Array<String>
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        }

    @get:androidx.annotation.StringRes
    override val permissionExplanation: Int
        get() = R.string.findmyphone_notifications_explanation
}