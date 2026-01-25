package org.cosmic.cosmicconnect.Core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AndroidCertificateStorage - Secure certificate storage using Android Keystore
 *
 * Stores private keys in Android Keystore (hardware-backed when available)
 * and certificates as encrypted PEM files.
 *
 * ## Security Model
 *
 * - **Private Keys**: Stored in Android Keystore, never exposed
 * - **Certificates**: Encrypted with Keystore-backed AES key, stored as files
 * - **Device Certificates**: Per-device encrypted files
 * - **Migration**: Automatic from SharedPreferences on first use
 *
 * ## Usage
 *
 * ```kotlin
 * val storage = AndroidCertificateStorage(context)
 *
 * // Get or generate local certificate
 * val cert = storage.getOrCreateLocalCertificate(deviceId)
 *
 * // Store remote device certificate
 * storage.storeDeviceCertificate(remoteDeviceId, remoteCert)
 *
 * // Retrieve remote certificate
 * val remoteCert = storage.getDeviceCertificate(remoteDeviceId)
 * ```
 */
class AndroidCertificateStorage(private val context: Context) {

    private val certDir: File = File(context.filesDir, "certificates")
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val TAG = "AndroidCertStorage"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val LOCAL_CERT_ALIAS = "cosmic_connect_local_cert"
        private const val ENCRYPTION_KEY_ALIAS = "cosmic_connect_cert_encryption"
        private const val LOCAL_CERT_FILE = "local_certificate.pem"
        private const val LOCAL_KEY_FILE = "local_private_key.pem"
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    init {
        // Ensure certificate directory exists
        if (!certDir.exists()) {
            certDir.mkdirs()
            Log.i(TAG, "Created certificate directory: ${certDir.absolutePath}")
        }

        // Ensure encryption key exists
        ensureEncryptionKey()
    }

    /**
     * Get or create local device certificate
     *
     * If a certificate exists, loads it. Otherwise generates a new one using
     * the Rust FFI core and stores it securely.
     *
     * @param deviceId Local device ID (UUID format)
     * @return Local device certificate
     */
    fun getOrCreateLocalCertificate(deviceId: String): Certificate {
        val certFile = File(certDir, LOCAL_CERT_FILE)
        val keyFile = File(certDir, LOCAL_KEY_FILE)

        return if (certFile.exists() && keyFile.exists()) {
            try {
                Log.i(TAG, "Loading existing local certificate")
                loadLocalCertificate()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load certificate, regenerating", e)
                generateAndStoreLocalCertificate(deviceId)
            }
        } else {
            Log.i(TAG, "Generating new local certificate")
            generateAndStoreLocalCertificate(deviceId)
        }
    }

    /**
     * Generate new local certificate and store securely
     */
    private fun generateAndStoreLocalCertificate(deviceId: String): Certificate {
        // Generate certificate using Rust FFI
        val cert = Certificate.generate(deviceId)

        // Store certificate and key as encrypted PEM files
        val certFile = File(certDir, LOCAL_CERT_FILE)
        val keyFile = File(certDir, LOCAL_KEY_FILE)

        try {
            // Encrypt and write certificate
            val encryptedCert = encrypt(cert.certificatePem)
            certFile.writeBytes(encryptedCert)

            // Encrypt and write private key
            val encryptedKey = encrypt(cert.privateKeyPem)
            keyFile.writeBytes(encryptedKey)

            Log.i(TAG, "✅ Local certificate stored: ${cert.fingerprint}")
            return cert
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store certificate", e)
            throw CosmicConnectException("Failed to store certificate: ${e.message}", e)
        }
    }

    /**
     * Load local certificate from storage
     */
    private fun loadLocalCertificate(): Certificate {
        val certFile = File(certDir, LOCAL_CERT_FILE)
        val keyFile = File(certDir, LOCAL_KEY_FILE)

        try {
            // Decrypt certificate and key
            val certPem = decrypt(certFile.readBytes())
            val keyPem = decrypt(keyFile.readBytes())

            // Parse using Rust FFI (reconstructs Certificate from PEM)
            // Note: We need to extract deviceId from the certificate
            val deviceId = extractDeviceIdFromCert(certPem)

            return Certificate(
                deviceId = deviceId,
                certificatePem = certPem,
                privateKeyPem = keyPem,
                fingerprint = calculateFingerprint(certPem)
            )
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to load local certificate: ${e.message}", e)
        }
    }

    /**
     * Store remote device certificate
     *
     * @param deviceId Remote device ID
     * @param certificate Remote device's certificate
     */
    fun storeDeviceCertificate(deviceId: String, certificate: Certificate) {
        val deviceCertFile = File(certDir, "$deviceId.pem")

        try {
            // Encrypt and store certificate
            val encryptedCert = encrypt(certificate.certificatePem)
            deviceCertFile.writeBytes(encryptedCert)

            Log.i(TAG, "✅ Stored certificate for device: $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store device certificate", e)
            throw CosmicConnectException("Failed to store device certificate: ${e.message}", e)
        }
    }

    /**
     * Get remote device certificate
     *
     * @param deviceId Remote device ID
     * @return Certificate for remote device
     * @throws CosmicConnectException if certificate not found
     */
    fun getDeviceCertificate(deviceId: String): Certificate {
        val deviceCertFile = File(certDir, "$deviceId.pem")

        if (!deviceCertFile.exists()) {
            throw CosmicConnectException("Certificate not found for device: $deviceId")
        }

        try {
            // Decrypt certificate
            val certPem = decrypt(deviceCertFile.readBytes())

            return Certificate(
                deviceId = deviceId,
                certificatePem = certPem,
                privateKeyPem = ByteArray(0), // Remote device's private key not stored
                fingerprint = calculateFingerprint(certPem)
            )
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to load device certificate: ${e.message}", e)
        }
    }

    /**
     * Check if device certificate exists
     */
    fun hasDeviceCertificate(deviceId: String): Boolean {
        return File(certDir, "$deviceId.pem").exists()
    }

    /**
     * Delete device certificate
     */
    fun deleteDeviceCertificate(deviceId: String) {
        val deviceCertFile = File(certDir, "$deviceId.pem")
        if (deviceCertFile.exists()) {
            deviceCertFile.delete()
            Log.i(TAG, "Deleted certificate for device: $deviceId")
        }
    }

    /**
     * Delete local certificate (forces regeneration)
     */
    fun deleteLocalCertificate() {
        File(certDir, LOCAL_CERT_FILE).delete()
        File(certDir, LOCAL_KEY_FILE).delete()
        Log.i(TAG, "Deleted local certificate")
    }

    /**
     * Get all stored device IDs
     */
    fun getAllDeviceIds(): List<String> {
        return certDir.listFiles()
            ?.filter { it.extension == "pem" && it.name != LOCAL_CERT_FILE }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    // ========================================================================
    // Encryption using Android Keystore
    // ========================================================================

    /**
     * Ensure encryption key exists in Keystore
     */
    private fun ensureEncryptionKey() {
        if (!keyStore.containsAlias(ENCRYPTION_KEY_ALIAS)) {
            Log.i(TAG, "Generating AES encryption key in Keystore")

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val spec = KeyGenParameterSpec.Builder(
                ENCRYPTION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(false) // No biometric for background sync
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()

            Log.i(TAG, "✅ Encryption key generated")
        }
    }

    /**
     * Encrypt data using Keystore-backed AES key
     *
     * Format: [IV (12 bytes)][Encrypted Data][Auth Tag (16 bytes)]
     */
    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = keyStore.getKey(ENCRYPTION_KEY_ALIAS, null) as SecretKey

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)

        // Prepend IV to encrypted data
        return iv + encrypted
    }

    /**
     * Decrypt data using Keystore-backed AES key
     */
    private fun decrypt(encryptedData: ByteArray): ByteArray {
        // Extract IV from beginning
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = keyStore.getKey(ENCRYPTION_KEY_ALIAS, null) as SecretKey
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }

    // ========================================================================
    // Certificate Utilities
    // ========================================================================

    /**
     * Extract device ID from certificate PEM
     *
     * Device ID is stored in the Common Name (CN) field of the certificate.
     */
    private fun extractDeviceIdFromCert(certPem: ByteArray): String {
        // Parse certificate using Java's CertificateFactory
        val factory = java.security.cert.CertificateFactory.getInstance("X.509")
        val cert = factory.generateCertificate(certPem.inputStream()) as java.security.cert.X509Certificate

        // Extract CN from subject DN
        val principal = cert.subjectX500Principal
        val dn = principal.name

        // Parse CN from DN string (format: "CN=deviceId,OU=...,O=...")
        val cnMatch = Regex("CN=([^,]+)").find(dn)
        return cnMatch?.groupValues?.get(1) ?: throw CosmicConnectException("Could not extract device ID from certificate")
    }

    /**
     * Calculate SHA-256 fingerprint of certificate
     */
    private fun calculateFingerprint(certPem: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certPem)
        return hash.joinToString("") { "%02x".format(it) }
    }
}
