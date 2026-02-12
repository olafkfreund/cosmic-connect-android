/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.CameraPlugin

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceManager
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.PermissionsAlertDialogFragment
import org.cosmicext.connect.UserInterface.PluginSettingsFragment

/**
 * CameraSettingsFragment - Settings for Camera Webcam Streaming plugin
 *
 * Provides user-configurable options for camera streaming:
 * - Default camera selection (front/back)
 * - Default resolution
 * - Default quality preset
 * - Auto-start streaming on connection
 *
 * @see CameraPlugin
 */
class CameraSettingsFragment : PluginSettingsFragment() {

    private var sharedPreferences: SharedPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // Set up preference listeners
        setupDefaultCameraPreference()
        setupResolutionPreference()
        setupQualityPreference()

        // Request camera permission if not granted
        checkAndRequestCameraPermission()
    }

    /**
     * Check if camera permission is granted, and request if not
     */
    private fun checkAndRequestCameraPermission() {
        val permissions = mutableListOf(Manifest.permission.CAMERA)

        // Android 14+ requires foreground service camera permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_CAMERA)
        }

        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            // Show permission request dialog
            PermissionsAlertDialogFragment.Builder()
                .setTitle(R.string.camera_permission_title)
                .setMessage(R.string.camera_permission_explanation)
                .setPermissions(missingPermissions.toTypedArray())
                .setRequestCode(REQUEST_CAMERA_PERMISSION)
                .create()
                .show(parentFragmentManager, "camera_permission_dialog")
        }
    }

    /**
     * Set up default camera selection preference
     */
    private fun setupDefaultCameraPreference() {
        val preference = findPreference<ListPreference>(
            getString(R.string.camera_preference_key_default_camera)
        ) ?: return

        preference.setOnPreferenceChangeListener { _, newValue ->
            preference.summary = when (newValue as String) {
                "back" -> getString(R.string.camera_back)
                "front" -> getString(R.string.camera_front)
                else -> getString(R.string.camera_back)
            }
            true
        }

        // Set initial summary
        preference.summary = when (preference.value ?: "back") {
            "back" -> getString(R.string.camera_back)
            "front" -> getString(R.string.camera_front)
            else -> getString(R.string.camera_back)
        }
    }

    /**
     * Set up default resolution preference
     */
    private fun setupResolutionPreference() {
        val preference = findPreference<ListPreference>(
            getString(R.string.camera_preference_key_resolution)
        ) ?: return

        preference.setOnPreferenceChangeListener { _, newValue ->
            preference.summary = getResolutionLabel(newValue as String)
            true
        }

        // Set initial summary
        preference.summary = getResolutionLabel(preference.value ?: "720p")
    }

    /**
     * Set up quality preset preference
     */
    private fun setupQualityPreference() {
        val preference = findPreference<ListPreference>(
            getString(R.string.camera_preference_key_quality)
        ) ?: return

        preference.setOnPreferenceChangeListener { _, newValue ->
            preference.summary = getQualityLabel(newValue as String)
            true
        }

        // Set initial summary
        preference.summary = getQualityLabel(preference.value ?: "medium")
    }

    /**
     * Get human-readable label for resolution value
     */
    private fun getResolutionLabel(value: String): String = when (value) {
        "480p" -> "854 × 480 (SD)"
        "720p" -> "1280 × 720 (HD)"
        "1080p" -> "1920 × 1080 (Full HD)"
        else -> "1280 × 720 (HD)"
    }

    /**
     * Get human-readable label for quality preset value
     */
    private fun getQualityLabel(value: String): String = when (value) {
        "low" -> getString(R.string.camera_quality_low)
        "medium" -> getString(R.string.camera_quality_medium)
        "high" -> getString(R.string.camera_quality_high)
        else -> getString(R.string.camera_quality_medium)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001

        /**
         * Create new instance with plugin key and layout
         */
        @JvmStatic
        fun newInstance(pluginKey: String, layout: Int): CameraSettingsFragment {
            val fragment = CameraSettingsFragment()
            fragment.setArguments(pluginKey, layout)
            return fragment
        }
    }
}
