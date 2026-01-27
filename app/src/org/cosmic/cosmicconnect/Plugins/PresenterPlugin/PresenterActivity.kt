/*
 * SPDX-FileCopyrightText: 2023 Dmitry Yudin <dgyudin@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.PresenterPlugin

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.media.VolumeProviderCompat
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.PresenterScreen
import javax.inject.Inject

private const val VOLUME_UP = 1
private const val VOLUME_DOWN = -1

@AndroidEntryPoint
class PresenterActivity : ComponentActivity(), SensorEventListener {

    @Inject lateinit var deviceRegistry: DeviceRegistry

    private val offScreenControlsSupported = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    private val mediaSession by lazy {
        if (offScreenControlsSupported) MediaSessionCompat(this, "cosmicconnect") else null
    }
    private val powerManager by lazy { getSystemService(POWER_SERVICE) as PowerManager }
    private lateinit var plugin : PresenterPlugin

    private val sensitivity = 0.03f

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            val xPos = -event.values[2] * sensitivity
            val yPos = -event.values[0] * sensitivity

            plugin.sendPointer(xPos, yPos)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //ignored
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plugin = deviceRegistry.getDevicePlugin(intent.getStringExtra("deviceId"), PresenterPlugin::class.java)
            ?: run {
                finish()
                return
            }
        
        setContent { 
            CosmicTheme(context = this) {
                PresenterScreen(
                    plugin = plugin,
                    sensorEventListener = this,
                    offScreenControlsSupported = offScreenControlsSupported,
                    onNavigateBack = { finish() }
                )
            }
        }
        
        createMediaSession()
    }

    override fun onResume() {
        super.onResume()
        mediaSession?.setActive(false)
    }

    override fun onPause() {
        super.onPause()
        mediaSession?.setActive(!powerManager.isInteractive)
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    private fun createMediaSession() {
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0, 0f).build()
        )
        mediaSession?.setPlaybackToRemote(volumeProvider)
    }

    private val volumeProvider = object : VolumeProviderCompat(VOLUME_CONTROL_RELATIVE, 1, 0) {
        override fun onAdjustVolume(direction: Int) {
            if (direction == VOLUME_UP) {
                plugin.sendNext()
            } else if (direction == VOLUME_DOWN) {
                plugin.sendPrevious()
            }
        }
    }
}
