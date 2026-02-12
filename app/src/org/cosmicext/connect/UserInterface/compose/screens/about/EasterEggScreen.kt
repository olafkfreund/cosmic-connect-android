/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.about

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import kotlin.math.PI
import kotlin.math.atan2

private val COSMIC_ICON_BACKGROUND_COLOR = Color(0xFF48B9C7)
private val COSMIC_ACCENT_COLOR = Color(0xFFF07178)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EasterEggScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var backgroundColor by remember { mutableStateOf(COSMIC_ICON_BACKGROUND_COLOR) }
    var currentIcon by remember { mutableIntStateOf(CosmicIcons.Brand.appIcon) } // Default icon
    var colorFilter by remember { mutableStateOf<ColorFilter?>(ColorFilter.tint(Color.White)) }

    val animatedRotation by animateFloatAsState(targetValue = rotationAngle, label = "rotation")

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val axisX = event.values[0]
                    val axisY = event.values[1]
                    val angle = (atan2(axisX, axisY) / (PI / 180)).toFloat()
                    rotationAngle = angle
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .combinedClickable(
                onClick = { },
                onLongClick = {
                    val icons = listOf(
                        CosmicIcons.Action.keyboard, CosmicIcons.Action.refresh,
                        CosmicIcons.Action.edit, CosmicIcons.Navigation.up,
                        CosmicIcons.About.attachMoney, CosmicIcons.About.bugReport,
                        CosmicIcons.About.code, CosmicIcons.About.gavel,
                        CosmicIcons.Status.info, CosmicIcons.Communication.web,
                        CosmicIcons.Action.send, CosmicIcons.Communication.sms,
                        CosmicIcons.Pairing.accept, CosmicIcons.Action.share,
                        CosmicIcons.Action.delete,
                        CosmicIcons.Device.laptop, CosmicIcons.Device.phone,
                        CosmicIcons.Device.tablet, CosmicIcons.Device.tv,
                        CosmicIcons.Action.delete, CosmicIcons.Status.warning,
                        CosmicIcons.Media.volume, CosmicIcons.Pairing.wifi,
                        CosmicIcons.Action.add, CosmicIcons.Plugin.touchpad,
                        CosmicIcons.Brand.appIcon, CosmicIcons.Plugin.runCommand,
                        CosmicIcons.Pairing.connected, CosmicIcons.Pairing.disconnected,
                        CosmicIcons.Status.error, CosmicIcons.Navigation.home,
                        CosmicIcons.Settings.settingsWhite, CosmicIcons.Media.stop,
                        CosmicIcons.Media.rewindBlack, CosmicIcons.Media.playBlack,
                        CosmicIcons.Media.microphone, CosmicIcons.Media.pauseBlack,
                        CosmicIcons.Media.volumeMute, CosmicIcons.Navigation.up,
                        CosmicIcons.Media.nextBlack, CosmicIcons.Media.previousBlack,
                        CosmicIcons.Plugin.presenter, CosmicIcons.Pairing.key,
                        CosmicIcons.Action.keyboardReturn, CosmicIcons.Action.keyboardHide,
                        CosmicIcons.Brand.cosmic24, CosmicIcons.Media.albumArtPlaceholder,
                        CosmicIcons.Navigation.back, CosmicIcons.Plugin.share
                    )
                    
                    val newIcon = icons.random()
                    currentIcon = newIcon

                    if (newIcon == CosmicIcons.Brand.appIcon) {
                        colorFilter = null
                        backgroundColor = COSMIC_ACCENT_COLOR
                    } else {
                        colorFilter = ColorFilter.tint(Color.White)
                        backgroundColor = COSMIC_ICON_BACKGROUND_COLOR
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(currentIcon),
            contentDescription = null,
            modifier = Modifier
                .size(128.dp)
                .rotate(animatedRotation),
            colorFilter = colorFilter
        )
        
        Text(
            text = "${rotationAngle.toInt()}°",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
        )
    }
}
