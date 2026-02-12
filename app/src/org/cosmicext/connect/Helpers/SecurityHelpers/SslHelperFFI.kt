package org.cosmicext.connect.Helpers.SecurityHelpers

import android.content.Context
import android.util.Log
import org.cosmicext.connect.Core.AndroidCertificateStorage
import org.cosmicext.connect.Core.Certificate
import org.cosmicext.connect.Core.CertificateMigration
import org.cosmicext.connect.Core.SslContextFactory
import org.cosmicext.connect.Helpers.DeviceHelper
import java.net.Socket
import javax.net.ssl.SSLSocket

/**
 * SslHelperFFI - Modern SSL helper using Rust FFI certificates
 *
 * Drop-in replacement for the old SslHelper that uses:
 * - Rust-generated certificates via FFI
 * - Android Keystore for secure storage
 * - Modern security practices
 *
 * ## Migration
 *
 * This class automatically migrates old SharedPreferences certificates
 * to the new Keystore-backed storage on first use.
 *
 * ## Usage
 *
 * ```kotlin
 * // Initialize (call once at app startup)
 * SslHelperFFI.initialize(context)
 *
 * // Convert socket to SSL
 * val sslSocket = SslHelperFFI.convertToSslSocket(
 *     context, socket, deviceId, trusted = true, clientMode = true
 * )
 * ```
 */
object SslHelperFFI {

    private const val TAG = "SslHelperFFI"

    private lateinit var certificateStorage: AndroidCertificateStorage
    private lateinit var sslContextFactory: SslContextFactory
    private var initialized = false

    /**
     * Local device certificate (cached after first load)
     */
    private var localCertificate: Certificate? = null

    /**
     * Initialize SSL helper with FFI certificates
     *
     * Must be called before using any other methods.
     * Safe to call multiple times (idempotent).
     *
     * @param context Application context
     */
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) {
            Log.d(TAG, "Already initialized")
            return
        }

        try {
            Log.i(TAG, "Initializing SSL helper with FFI certificates")

            // Create certificate storage
            certificateStorage = AndroidCertificateStorage(context.applicationContext)

            // Create SSL context factory
            sslContextFactory = SslContextFactory(context.applicationContext, certificateStorage)

            // Migrate old certificates if needed
            val migration = CertificateMigration(context.applicationContext, certificateStorage)
            if (migration.isMigrationNeeded()) {
                Log.i(TAG, "Migrating certificates from SharedPreferences")
                val migrated = migration.migrateIfNeeded()
                if (migrated) {
                    Log.i(TAG, "✅ Certificate migration successful")
                }
            }

            // Load local certificate
            val deviceId = DeviceHelper.getDeviceId(context)
            localCertificate = certificateStorage.getOrCreateLocalCertificate(deviceId)

            initialized = true
            Log.i(TAG, "✅ SSL helper initialized successfully")
            Log.i(TAG, "Local certificate fingerprint: ${localCertificate?.getFingerprintFormatted()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize SSL helper", e)
            throw RuntimeException("Failed to initialize SSL helper: ${e.message}", e)
        }
    }

    /**
     * Convert plain socket to SSL socket
     *
     * @param context Context
     * @param socket Plain TCP socket
     * @param deviceId Remote device ID
     * @param isDeviceTrusted Whether device is trusted
     * @param clientMode Whether to use client mode (true) or server mode (false)
     * @return Configured SSLSocket
     */
    @JvmStatic
    fun convertToSslSocket(
        context: Context,
        socket: Socket,
        deviceId: String,
        isDeviceTrusted: Boolean,
        clientMode: Boolean
    ): SSLSocket {
        ensureInitialized(context)

        return try {
            sslContextFactory.createSslSocket(socket, deviceId, isDeviceTrusted, clientMode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert socket to SSL", e)
            throw e
        }
    }

    /**
     * Get certificate hash (fingerprint) for display
     *
     * @param certificate Certificate (not used in FFI version - uses stored cert)
     * @return Formatted fingerprint
     */
    @JvmStatic
    fun getCertificateHash(certificate: java.security.cert.Certificate): String {
        // In FFI version, we use the stored certificate
        return localCertificate?.getFingerprintFormatted()
            ?: throw IllegalStateException("SSL helper not initialized")
    }

    /**
     * Get local certificate fingerprint
     */
    fun getLocalFingerprint(): String {
        return localCertificate?.getFingerprintFormatted()
            ?: throw IllegalStateException("SSL helper not initialized")
    }

    /**
     * Get remote device certificate fingerprint
     *
     * @param deviceId Remote device ID
     * @return Formatted fingerprint
     */
    fun getRemoteFingerprint(deviceId: String): String {
        ensureInitialized(null)
        return sslContextFactory.getRemoteCertificateFingerprint(deviceId)
    }

    /**
     * Store remote device certificate
     *
     * Called after successful pairing to trust the device.
     *
     * @param context Context
     * @param deviceId Remote device ID
     * @param certificatePem Certificate in PEM format
     */
    fun storeRemoteCertificate(context: Context, deviceId: String, certificatePem: ByteArray) {
        ensureInitialized(context)

        try {
            val fingerprint = calculateFingerprint(certificatePem)
            val certificate = Certificate(
                deviceId = deviceId,
                certificatePem = certificatePem,
                privateKeyPem = ByteArray(0), // Remote devices don't have private keys
                fingerprint = fingerprint
            )

            certificateStorage.storeDeviceCertificate(deviceId, certificate)
            Log.i(TAG, "✅ Stored certificate for device: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store remote certificate", e)
            throw e
        }
    }

    /**
     * Check if device certificate is stored
     */
    fun hasDeviceCertificate(deviceId: String): Boolean {
        ensureInitialized(null)
        return certificateStorage.hasDeviceCertificate(deviceId)
    }

    /**
     * Delete device certificate
     */
    fun deleteDeviceCertificate(deviceId: String) {
        ensureInitialized(null)
        certificateStorage.deleteDeviceCertificate(deviceId)
    }

    /**
     * Get local certificate (for compatibility with old code)
     */
    val certificate: java.security.cert.Certificate
        get() {
            ensureInitialized(null)
            val certPem = localCertificate?.certificatePem
                ?: throw IllegalStateException("Local certificate not loaded")

            val factory = java.security.cert.CertificateFactory.getInstance("X.509")
            return factory.generateCertificate(certPem.inputStream())
        }

    /**
     * Parse certificate from bytes
     */
    fun parseCertificate(certificateBytes: ByteArray): java.security.cert.Certificate {
        val factory = java.security.cert.CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(certificateBytes.inputStream())
    }

    /**
     * Get common name from certificate
     */
    fun getCommonNameFromCertificate(cert: java.security.cert.X509Certificate): String {
        val principal = cert.subjectX500Principal
        val dn = principal.name
        val cnMatch = Regex("CN=([^,]+)").find(dn)
        return cnMatch?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Certificate does not contain CN")
    }

    /**
     * Calculate SHA-256 fingerprint
     */
    private fun calculateFingerprint(certPem: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certPem)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Ensure SSL helper is initialized
     */
    private fun ensureInitialized(context: Context?) {
        if (!initialized) {
            if (context != null) {
                initialize(context)
            } else {
                throw IllegalStateException("SSL helper not initialized. Call initialize(context) first.")
            }
        }
    }

    /**
     * Check if initialized
     */
    val isInitialized: Boolean
        get() = initialized

    /**
     * Get certificate storage (for advanced use)
     */
    fun getCertificateStorage(): AndroidCertificateStorage {
        ensureInitialized(null)
        return certificateStorage
    }

    /**
     * Force regeneration of local certificate
     *
     * WARNING: This will break pairing with all existing devices!
     * Only use for testing or when recovering from corruption.
     */
    fun regenerateLocalCertificate(context: Context) {
        ensureInitialized(context)

        Log.w(TAG, "⚠️ Regenerating local certificate - all pairings will be broken!")

        certificateStorage.deleteLocalCertificate()
        val deviceId = DeviceHelper.getDeviceId(context)
        localCertificate = certificateStorage.getOrCreateLocalCertificate(deviceId)

        Log.i(TAG, "✅ New certificate generated: ${localCertificate?.getFingerprintFormatted()}")
    }
}
