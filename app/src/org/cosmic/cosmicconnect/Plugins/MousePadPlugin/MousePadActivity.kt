/*
 * SPDX-FileCopyrightText: 2014 Ahmed I. Khalil <ahmedibrahimkhali@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.MousePadPlugin

import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadScreen
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.MousePadViewModel
import org.cosmic.cosmicconnect.R
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow

@AndroidEntryPoint
class MousePadActivity : ComponentActivity(),
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    MousePadGestureDetector.OnGestureListener,
    SensorEventListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var deviceId: String? = null

    @Inject lateinit var deviceRegistry: DeviceRegistry

    private val viewModel: MousePadViewModel by viewModels()

    private var mPrevX = 0f
    private var mPrevY = 0f
    private var dragging = false
    private var mCurrentSensitivity = 1.0f
    private var displayDpiMultiplier = 1.0f
    private var scrollDirection = 1
    private var scrollCoefficient = 1.0
    private var allowGyro = false
    private var gyroEnabled = false
    private var doubleTapDragEnabled = false
    private var gyroscopeSensitivity = 100
    private var isScrolling = false
    private var accumulatedDistanceY = 0f

    private var mDetector: GestureDetector? = null
    private var mSensorManager: SensorManager? = null
    private var mMousePadGestureDetector: MousePadGestureDetector? = null
    private var mPointerAccelerationProfile: PointerAccelerationProfile? = null

    private var mouseDelta = PointerAccelerationProfile.MouseDelta()

    private var prefs: SharedPreferences? = null
    private var prefsApplied = false

    enum class ClickType {
        LEFT, RIGHT, MIDDLE, NONE;

        companion object {
            fun fromString(s: String?): ClickType {
                return when (s) {
                    "left" -> LEFT
                    "right" -> RIGHT
                    "middle" -> MIDDLE
                    else -> NONE
                }
            }
        }
    }

    private var singleTapAction: ClickType = ClickType.NONE
    private var doubleTapAction: ClickType = ClickType.NONE
    private var tripleTapAction: ClickType = ClickType.NONE

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val values = event.values

        var x = -values[2] * 70 * (gyroscopeSensitivity / 100.0f)
        var y = -values[0] * 70 * (gyroscopeSensitivity / 100.0f)

        if (x < 0.25 && x > -0.25) {
            x = 0f
        } else {
            x *= (gyroscopeSensitivity / 100.0f)
        }

        if (y < 0.25 && y > -0.25) {
            y = 0f
        } else {
            y *= (gyroscopeSensitivity / 100.0f)
        }

        viewModel.sendMouseDelta(x, y)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceId = intent.getStringExtra("deviceId")

        window.decorView.isHapticFeedbackEnabled = true

        mDetector = GestureDetector(this, this)
        mMousePadGestureDetector = MousePadGestureDetector(this)
        mDetector?.setOnDoubleTapListener(this)
        mSensorManager = ContextCompat.getSystemService(this, SensorManager::class.java)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs?.registerOnSharedPreferenceChangeListener(this)

        applyPrefs()

        displayDpiMultiplier = StandardDpi / resources.displayMetrics.xdpi

        setContent {
            CosmicTheme(context = this) {
                MousePadScreen(
                    viewModel = viewModel,
                    deviceId = deviceId,
                    onNavigateBack = { finish() },
                    onOpenSettings = {
                        val intent = Intent(this, PluginSettingsActivity::class.java).apply {
                            putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                            putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, MousePadPlugin::class.java.simpleName)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }

    override fun onResume() {
        applyPrefs()

        if (allowGyro && !gyroEnabled) {
            mSensorManager?.let {
                it.registerListener(this, it.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
                gyroEnabled = true
            }
        }

        super.onResume()
    }

    override fun onPause() {
        if (gyroEnabled) {
            mSensorManager?.unregisterListener(this)
            gyroEnabled = false
        }
        super.onPause()
    }

    override fun onDestroy() {
        prefs?.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mMousePadGestureDetector?.onTouchEvent(event) == true) {
            return true
        }
        if (mDetector?.onTouchEvent(event) == true) {
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPrevX = event.x
                mPrevY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScrolling) return false

                val mCurrentX = event.x
                val mCurrentY = event.y

                val deltaX = (mCurrentX - mPrevX) * displayDpiMultiplier * mCurrentSensitivity
                val deltaY = (mCurrentY - mPrevY) * displayDpiMultiplier * mCurrentSensitivity

                // Run the mouse delta through the pointer acceleration profile
                mPointerAccelerationProfile?.touchMoved(deltaX, deltaY, event.eventTime)
                mouseDelta = mPointerAccelerationProfile?.commitAcceleratedMouseDelta(mouseDelta) ?: mouseDelta

                viewModel.sendMouseDelta(mouseDelta.x, mouseDelta.y)

                mPrevX = mCurrentX
                mPrevY = mCurrentY
            }
            MotionEvent.ACTION_UP -> {
                isScrolling = false
            }
        }
        return true
    }

    override fun onDown(e: MotionEvent): Boolean = false
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val distanceY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            accumulatedDistanceY += distanceY

            if (abs(accumulatedDistanceY) > MinDistanceToSendGenericScroll) {
                viewModel.sendScroll(0f, accumulatedDistanceY)
                accumulatedDistanceY = 0f
            }
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (e2.pointerCount <= 1) {
            return false
        }

        isScrolling = true

        accumulatedDistanceY += distanceY * scrollCoefficient.toFloat()
        if (abs(accumulatedDistanceY) > MinDistanceToSendScroll) {
            viewModel.sendScroll(0f, scrollDirection * accumulatedDistanceY)
            accumulatedDistanceY = 0f
        }

        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (!doubleTapDragEnabled) {
            window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            viewModel.sendSingleHold()
            dragging = true
        }
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        when (singleTapAction) {
            ClickType.LEFT -> sendLeftClick()
            ClickType.RIGHT -> viewModel.sendRightClick()
            ClickType.MIDDLE -> viewModel.sendMiddleClick()
            else -> {}
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (doubleTapDragEnabled) {
            viewModel.sendSingleHold()
            dragging = true
        } else {
            viewModel.sendDoubleClick()
        }
        return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean = true

    override fun onTripleFingerTap(ev: MotionEvent): Boolean {
        when (tripleTapAction) {
            ClickType.LEFT -> sendLeftClick()
            ClickType.RIGHT -> viewModel.sendRightClick()
            ClickType.MIDDLE -> viewModel.sendMiddleClick()
            else -> {}
        }
        return true
    }

    override fun onDoubleFingerTap(ev: MotionEvent): Boolean {
        when (doubleTapAction) {
            ClickType.LEFT -> sendLeftClick()
            ClickType.RIGHT -> viewModel.sendRightClick()
            ClickType.MIDDLE -> viewModel.sendMiddleClick()
            else -> {}
        }
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        prefsApplied = false
    }

    private fun sendLeftClick() {
        if (dragging) {
            viewModel.sendSingleRelease()
            dragging = false
        } else {
            viewModel.sendLeftClick()
        }
    }

    private fun applyPrefs() {
        if (prefsApplied) return
        val currentPrefs = prefs ?: return

        scrollDirection = if (currentPrefs.getBoolean(getString(R.string.mousepad_scroll_direction), false)) -1 else 1

        var scrollSensitivity = currentPrefs.getInt(getString(R.string.mousepad_scroll_sensitivity), 100)
        if (scrollSensitivity == 0) scrollSensitivity = 1
        scrollCoefficient = (scrollSensitivity / 100f).toDouble().pow(1.5)

        allowGyro = isGyroSensorAvailable() && currentPrefs.getBoolean(getString(R.string.gyro_mouse_enabled), false)
        if (allowGyro) gyroscopeSensitivity = currentPrefs.getInt(getString(R.string.gyro_mouse_sensitivity), 100)

        val singleTapSetting = currentPrefs.getString(getString(R.string.mousepad_single_tap_key), getString(R.string.mousepad_default_single))
        val doubleTapSetting = currentPrefs.getString(getString(R.string.mousepad_double_tap_key), getString(R.string.mousepad_default_double))
        val tripleTapSetting = currentPrefs.getString(getString(R.string.mousepad_triple_tap_key), getString(R.string.mousepad_default_triple))
        val sensitivitySetting = currentPrefs.getString(getString(R.string.mousepad_sensitivity_key), getString(R.string.mousepad_default_sensitivity))

        val accelerationProfileName = currentPrefs.getString(
            getString(R.string.mousepad_acceleration_profile_key),
            getString(R.string.mousepad_default_acceleration_profile)
        )

        mPointerAccelerationProfile = PointerAccelerationProfileFactory.getProfileWithName(accelerationProfileName ?: "")

        singleTapAction = ClickType.fromString(singleTapSetting)
        doubleTapAction = ClickType.fromString(doubleTapSetting)
        tripleTapAction = ClickType.fromString(tripleTapSetting)

        mCurrentSensitivity = when (sensitivitySetting) {
            "slowest" -> 0.2f
            "aboveSlowest" -> 0.5f
            "default" -> 1.0f
            "aboveDefault" -> 1.5f
            "fastest" -> 2.0f
            else -> 1.0f
        }

        doubleTapDragEnabled = currentPrefs.getBoolean(getString(R.string.mousepad_doubletap_drag_enabled_pref), true)

        prefsApplied = true
    }

    private fun isGyroSensorAvailable(): Boolean {
        return mSensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
    }

    companion object {
        private const val MinDistanceToSendScroll = 2.5f
        private const val MinDistanceToSendGenericScroll = 0.1f
        private const val StandardDpi = 240.0f
    }
}
