package org.cosmicext.connect.Helpers

import android.content.Context
import android.content.SharedPreferences
import org.cosmicext.connect.Core.AndroidCertificateStorage
import org.cosmicext.connect.Core.Certificate
import org.cosmicext.connect.Helpers.SecurityHelpers.SslHelperFFI

/**
 * TrustedDevicesFFI - Trusted device management using FFI certificate storage
 *
 * Drop-in replacement for TrustedDevices that uses AndroidCertificateStorage
 * instead of SharedPreferences for certificate storage.
 *
 * ## Security Improvements
 *
 * - Certificates stored in Android Keystore (hardware-backed)
 * - Encrypted at rest with AES-GCM
 * - Trust list still uses SharedPreferences (lightweight)
 * - Certificate storage decoupled from trust management
 *
 * ## Usage
 *
 * ```kotlin
 * // Check if device is trusted
 * if (TrustedDevicesFFI.isTrustedDevice(context, deviceId)) {
 *     // Device is trusted
 * }
 *
 * // Add trusted device
 * TrustedDevicesFFI.addTrustedDevice(context, deviceId, certificate)
 *
 * // Remove trust
 * TrustedDevicesFFI.removeTrustedDevice(context, deviceId)
 * ```
 */
object TrustedDevicesFFI {

    private const val PREFS_NAME = "trusted_devices"

    /**
     * Check if device is trusted
     *
     * @param context Context
     * @param deviceId Device ID to check
     * @return true if device is trusted
     */
    @JvmStatic
    fun isTrustedDevice(context: Context, deviceId: String): Boolean {
        val preferences = getTrustedPreferences(context)
        return preferences.getBoolean(deviceId, false)
    }

    /**
     * Add trusted device
     *
     * Stores the device's certificate and marks it as trusted.
     *
     * @param context Context
     * @param deviceId Device ID to trust
     * @param certificate Device's certificate (PEM bytes)
     */
    fun addTrustedDevice(context: Context, deviceId: String, certificate: ByteArray) {
        // Mark as trusted
        val preferences = getTrustedPreferences(context)
        preferences.edit().putBoolean(deviceId, true).apply()

        // Store certificate
        SslHelperFFI.storeRemoteCertificate(context, deviceId, certificate)
    }

    /**
     * Add trusted device (Certificate object)
     */
    fun addTrustedDevice(context: Context, deviceId: String, certificate: Certificate) {
        addTrustedDevice(context, deviceId, certificate.certificatePem)
    }

    /**
     * Remove trusted device
     *
     * Deletes trust marker and certificate.
     *
     * @param context Context
     * @param deviceId Device ID to untrust
     */
    fun removeTrustedDevice(context: Context, deviceId: String) {
        // Remove trust marker
        val preferences = getTrustedPreferences(context)
        preferences.edit().remove(deviceId).apply()

        // Delete certificate
        SslHelperFFI.deleteDeviceCertificate(deviceId)

        // Clear device-specific settings
        val deviceSettings = context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
        deviceSettings.edit().clear().apply()
    }

    /**
     * Get all trusted device IDs
     *
     * @param context Context
     * @return List of trusted device IDs
     */
    fun getAllTrustedDevices(context: Context): List<String> {
        val preferences = getTrustedPreferences(context)
        return preferences.all.keys.filter { preferences.getBoolean(it, false) }
    }

    /**
     * Remove all trusted devices
     *
     * Clears all trust markers and deletes all certificates.
     *
     * @param context Context
     */
    fun removeAllTrustedDevices(context: Context) {
        val deviceIds = getAllTrustedDevices(context)

        for (deviceId in deviceIds) {
            removeTrustedDevice(context, deviceId)
        }

        // Clear trust preferences
        val preferences = getTrustedPreferences(context)
        preferences.edit().clear().apply()
    }

    /**
     * Get device certificate
     *
     * @param context Context
     * @param deviceId Device ID
     * @return Java Certificate object
     * @throws Exception if certificate not found
     */
    fun getDeviceCertificate(context: Context, deviceId: String): java.security.cert.Certificate {
        val storage = SslHelperFFI.getCertificateStorage()
        val cert = storage.getDeviceCertificate(deviceId)
        return SslHelperFFI.parseCertificate(cert.certificatePem)
    }

    /**
     * Check if certificate is stored for device
     *
     * @param context Context
     * @param deviceId Device ID
     * @return true if certificate is stored
     */
    @JvmStatic
    fun isCertificateStored(context: Context, deviceId: String): Boolean {
        return SslHelperFFI.hasDeviceCertificate(deviceId)
    }

    /**
     * Get device-specific settings
     *
     * @param context Context
     * @param deviceId Device ID
     * @return SharedPreferences for device
     */
    fun getDeviceSettings(context: Context, deviceId: String): SharedPreferences {
        return context.getSharedPreferences(deviceId, Context.MODE_PRIVATE)
    }

    /**
     * Get trusted devices preferences
     */
    private fun getTrustedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Migration note: Check if using old TrustedDevices
     *
     * This helps identify if migration from old storage is needed.
     */
    fun needsMigration(context: Context): Boolean {
        val trustedDevices = getAllTrustedDevices(context)
        if (trustedDevices.isEmpty()) return false

        // Check if any trusted device doesn't have a certificate in new storage
        return trustedDevices.any { deviceId ->
            !SslHelperFFI.hasDeviceCertificate(deviceId)
        }
    }
}
