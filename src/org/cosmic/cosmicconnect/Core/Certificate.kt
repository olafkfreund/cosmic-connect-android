package org.cosmic.cosmicconnect.Core

import android.util.Log
import uniffi.cosmic_connect_core.*
import java.io.File

/**
 * Certificate - TLS certificate management wrapper
 *
 * Wraps Rust FFI certificate operations with idiomatic Kotlin API.
 *
 * ## Certificate Details
 * - Type: Self-signed RSA 2048-bit
 * - Hash: SHA-256 fingerprint
 * - Format: PEM (certificate + private key)
 * - Validity: 10 years
 *
 * ## Usage
 * ```kotlin
 * // Generate new certificate
 * val cert = Certificate.generate("device_id_12345")
 *
 * // Save to files
 * cert.save(certPath = "/path/cert.pem", keyPath = "/path/key.pem")
 *
 * // Load from files
 * val loaded = Certificate.load(certPath = "/path/cert.pem", keyPath = "/path/key.pem")
 *
 * // Get fingerprint for pairing
 * val fingerprint = cert.fingerprint
 * ```
 */
data class Certificate(
    val deviceId: String,
    val certificatePem: ByteArray,
    val privateKeyPem: ByteArray,
    val fingerprint: String
) {

    companion object {
        private const val TAG = "Certificate"

        /**
         * Generate a new self-signed certificate
         *
         * Creates an RSA 2048-bit certificate valid for 10 years.
         * The certificate is self-signed and suitable for KDE Connect's
         * Trust-On-First-Use (TOFU) security model.
         *
         * @param deviceId Unique device identifier (UUID format recommended)
         * @return New Certificate with generated keys
         * @throws CosmicConnectException if generation fails
         */
        fun generate(deviceId: String): Certificate {
            try {
                Log.d(TAG, "Generating certificate for device: $deviceId")
                val ffiCert = generateCertificate(deviceId)
                Log.i(TAG, "✅ Certificate generated: ${ffiCert.fingerprint}")
                return fromFfiCertificate(ffiCert)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to generate certificate", e)
                throw CosmicConnectException("Failed to generate certificate: ${e.message}", e)
            }
        }

        /**
         * Load certificate from PEM files
         *
         * @param certPath Path to certificate PEM file
         * @param keyPath Path to private key PEM file
         * @return Certificate loaded from files
         * @throws CosmicConnectException if loading fails
         */
        fun load(certPath: String, keyPath: String): Certificate {
            try {
                Log.d(TAG, "Loading certificate from: $certPath")

                // Validate files exist
                require(File(certPath).exists()) { "Certificate file not found: $certPath" }
                require(File(keyPath).exists()) { "Private key file not found: $keyPath" }

                val ffiCert = loadCertificate(certPath, keyPath)
                Log.i(TAG, "✅ Certificate loaded: ${ffiCert.fingerprint}")
                return fromFfiCertificate(ffiCert)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load certificate", e)
                throw CosmicConnectException("Failed to load certificate: ${e.message}", e)
            }
        }

        /**
         * Load certificate from File objects
         */
        fun load(certFile: File, keyFile: File): Certificate {
            return load(certFile.absolutePath, keyFile.absolutePath)
        }

        /**
         * Convert FFI certificate to Kotlin Certificate
         */
        internal fun fromFfiCertificate(ffiCert: FfiCertificate): Certificate {
            return Certificate(
                deviceId = ffiCert.deviceId,
                certificatePem = ffiCert.certificate,
                privateKeyPem = ffiCert.privateKey,
                fingerprint = ffiCert.fingerprint
            )
        }
    }

    /**
     * Save certificate to PEM files
     *
     * @param certPath Path where certificate PEM will be saved
     * @param keyPath Path where private key PEM will be saved
     * @throws CosmicConnectException if saving fails
     */
    fun save(certPath: String, keyPath: String) {
        try {
            Log.d(TAG, "Saving certificate to: $certPath")

            // Ensure parent directories exist
            File(certPath).parentFile?.mkdirs()
            File(keyPath).parentFile?.mkdirs()

            val ffiCert = toFfiCertificate()
            saveCertificate(ffiCert, certPath, keyPath)

            Log.i(TAG, "✅ Certificate saved")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save certificate", e)
            throw CosmicConnectException("Failed to save certificate: ${e.message}", e)
        }
    }

    /**
     * Save certificate to File objects
     */
    fun save(certFile: File, keyFile: File) {
        save(certFile.absolutePath, keyFile.absolutePath)
    }

    /**
     * Get SHA-256 fingerprint
     *
     * The fingerprint is used for device pairing verification.
     * Users compare fingerprints to ensure they're connecting to the correct device.
     */
    fun getFingerprint(): String {
        return fingerprint
    }

    /**
     * Get fingerprint in human-readable format (with colons)
     *
     * Example: "AB:CD:EF:12:34:56:..."
     */
    fun getFingerprintFormatted(): String {
        return fingerprint.chunked(2).joinToString(":")
    }

    /**
     * Convert to FFI certificate for Rust calls
     */
    internal fun toFfiCertificate(): FfiCertificate {
        return FfiCertificate(
            deviceId = deviceId,
            certificate = certificatePem,
            privateKey = privateKeyPem,
            fingerprint = fingerprint
        )
    }

    /**
     * Check if certificate is valid (has required data)
     */
    val isValid: Boolean
        get() = deviceId.isNotEmpty() &&
                certificatePem.isNotEmpty() &&
                privateKeyPem.isNotEmpty() &&
                fingerprint.isNotEmpty()

    override fun toString(): String {
        return "Certificate(deviceId='$deviceId', fingerprint='${getFingerprintFormatted()}')"
    }

    // Implement equals/hashCode properly for ByteArray fields
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Certificate

        if (deviceId != other.deviceId) return false
        if (!certificatePem.contentEquals(other.certificatePem)) return false
        if (!privateKeyPem.contentEquals(other.privateKeyPem)) return false
        if (fingerprint != other.fingerprint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceId.hashCode()
        result = 31 * result + certificatePem.contentHashCode()
        result = 31 * result + privateKeyPem.contentHashCode()
        result = 31 * result + fingerprint.hashCode()
        return result
    }
}

/**
 * CertificateManager - Manages device certificates
 *
 * Handles certificate storage, loading, and lifecycle management
 * for the local device and trusted remote devices.
 */
class CertificateManager(private val certDir: File) {

    companion object {
        private const val TAG = "CertificateManager"
        private const val CERT_FILE_NAME = "certificate.pem"
        private const val KEY_FILE_NAME = "private_key.pem"
    }

    init {
        // Ensure certificate directory exists
        if (!certDir.exists()) {
            certDir.mkdirs()
            Log.i(TAG, "Created certificate directory: ${certDir.absolutePath}")
        }
    }

    /**
     * Get or generate certificate for local device
     *
     * If a certificate already exists, it's loaded.
     * Otherwise, a new certificate is generated and saved.
     *
     * @param deviceId Local device ID
     * @return Certificate for local device
     */
    fun getOrGenerateLocalCertificate(deviceId: String): Certificate {
        val certFile = File(certDir, CERT_FILE_NAME)
        val keyFile = File(certDir, KEY_FILE_NAME)

        return if (certFile.exists() && keyFile.exists()) {
            Log.i(TAG, "Loading existing certificate")
            Certificate.load(certFile, keyFile)
        } else {
            Log.i(TAG, "Generating new certificate")
            val cert = Certificate.generate(deviceId)
            cert.save(certFile, keyFile)
            cert
        }
    }

    /**
     * Delete local certificate
     */
    fun deleteLocalCertificate() {
        val certFile = File(certDir, CERT_FILE_NAME)
        val keyFile = File(certDir, KEY_FILE_NAME)

        certFile.delete()
        keyFile.delete()
        Log.i(TAG, "Deleted local certificate")
    }

    /**
     * Check if local certificate exists
     */
    fun hasLocalCertificate(): Boolean {
        val certFile = File(certDir, CERT_FILE_NAME)
        val keyFile = File(certDir, KEY_FILE_NAME)
        return certFile.exists() && keyFile.exists()
    }
}
