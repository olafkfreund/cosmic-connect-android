package org.cosmicext.connect.Core

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.*

/**
 * SslContextFactory - Creates SSLContext and SSLSocket instances using FFI certificates
 *
 * Replaces the old SslHelper with a cleaner, FFI-based implementation.
 *
 * ## Security Model
 *
 * - Uses Rust-generated certificates via FFI
 * - Certificates stored in Android Keystore (hardware-backed)
 * - Trust-On-First-Use (TOFU) for initial pairing
 * - Certificate pinning for trusted devices
 * - TLS 1.2 (TLS 1.3 causes issues on some older devices)
 *
 * ## Usage
 *
 * ```kotlin
 * val factory = SslContextFactory(context, certificateStorage)
 *
 * // For untrusted device (pairing)
 * val sslSocket = factory.createSslSocket(socket, deviceId, trusted = false, clientMode = true)
 *
 * // For trusted device (normal operation)
 * val sslSocket = factory.createSslSocket(socket, deviceId, trusted = true, clientMode = true)
 * ```
 */
class SslContextFactory(
    private val context: Context,
    private val certificateStorage: AndroidCertificateStorage
) {

    companion object {
        private const val TAG = "SslContextFactory"
        private const val TLS_PROTOCOL = "TLSv1.2" // TLS 1.3 has issues on some devices
        private const val SOCKET_TIMEOUT_MS = 10000
    }

    /**
     * Create SSLSocket from existing socket
     *
     * @param socket Plain TCP socket to upgrade
     * @param deviceId Remote device ID
     * @param trusted Whether device is trusted (affects client auth requirements)
     * @param clientMode Whether to use client mode (true) or server mode (false)
     * @return Configured SSLSocket ready for handshake
     */
    fun createSslSocket(
        socket: Socket,
        deviceId: String,
        trusted: Boolean,
        clientMode: Boolean
    ): SSLSocket {
        try {
            val sslContext = createSslContext(deviceId, trusted)
            val sslSocket = sslContext.socketFactory.createSocket(
                socket,
                socket.inetAddress.hostAddress,
                socket.port,
                true // Auto-close underlying socket
            ) as SSLSocket

            configureSslSocket(sslSocket, trusted, clientMode)

            Log.d(TAG, "Created SSL socket for device: $deviceId (trusted=$trusted, clientMode=$clientMode)")
            return sslSocket
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SSL socket", e)
            throw CosmicExtConnectException("Failed to create SSL socket: ${e.message}", e)
        }
    }

    /**
     * Create SSLContext for device
     *
     * @param deviceId Remote device ID
     * @param trusted Whether device is trusted
     * @return Configured SSLContext
     */
    private fun createSslContext(deviceId: String, trusted: Boolean): SSLContext {
        // Get local certificate
        val localCert = certificateStorage.getOrCreateLocalCertificate(
            org.cosmicext.connect.Helpers.DeviceHelper.getDeviceId(context)
        )

        // Create KeyStore with local certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        // Convert certificate PEM to Java Certificate
        val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
        val javaCert = certFactory.generateCertificate(
            ByteArrayInputStream(localCert.certificatePem)
        )

        // Convert private key PEM to Java PrivateKey
        val privateKey = parsePrivateKey(localCert.privateKeyPem)

        // Add local certificate and private key
        keyStore.setKeyEntry("local", privateKey, "".toCharArray(), arrayOf(javaCert))

        // Add remote device certificate if trusted
        if (trusted && certificateStorage.hasDeviceCertificate(deviceId)) {
            val remoteCert = certificateStorage.getDeviceCertificate(deviceId)
            val remoteJavaCert = certFactory.generateCertificate(
                ByteArrayInputStream(remoteCert.certificatePem)
            )
            keyStore.setCertificateEntry(deviceId, remoteJavaCert)
        }

        // Setup KeyManagerFactory
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "".toCharArray())

        // Setup TrustManagerFactory
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        // Create SSLContext
        val sslContext = SSLContext.getInstance(TLS_PROTOCOL)
        if (trusted) {
            // Use standard trust managers (require valid cert)
            sslContext.init(
                keyManagerFactory.keyManagers,
                trustManagerFactory.trustManagers,
                java.security.SecureRandom()
            )
        } else {
            // Use trust-all manager for pairing
            sslContext.init(
                keyManagerFactory.keyManagers,
                trustAllCerts,
                java.security.SecureRandom()
            )
        }

        return sslContext
    }

    /**
     * Configure SSLSocket settings
     */
    private fun configureSslSocket(sslSocket: SSLSocket, trusted: Boolean, clientMode: Boolean) {
        sslSocket.soTimeout = SOCKET_TIMEOUT_MS

        if (clientMode) {
            sslSocket.useClientMode = true
        } else {
            sslSocket.useClientMode = false
            if (trusted) {
                sslSocket.needClientAuth = true // Require client certificate
            } else {
                sslSocket.wantClientAuth = true // Request but don't require
            }
        }
    }

    /**
     * Parse private key bytes to Java PrivateKey
     *
     * Supports both DER-encoded (from Rust FFI) and PEM-encoded keys.
     * Auto-detects format and tries RSA/EC key factories.
     */
    private fun parsePrivateKey(keyData: ByteArray): java.security.PrivateKey {
        try {
            // Detect if this is PEM or DER format
            val keyBytes = if (isPemEncoded(keyData)) {
                // PEM: strip headers and base64-decode
                val pemString = keyData.toString(Charsets.UTF_8)
                val base64Data = pemString
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----BEGIN EC PRIVATE KEY-----", "")
                    .replace("-----END EC PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("\\s".toRegex(), "")
                android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            } else {
                // DER: use raw bytes directly (Rust FFI returns PKCS#8 DER)
                keyData
            }

            // Try PKCS8 format (RSA first since FFI generates RSA 2048)
            val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
            return try {
                java.security.KeyFactory.getInstance("RSA").generatePrivate(keySpec)
            } catch (e: Exception) {
                java.security.KeyFactory.getInstance("EC").generatePrivate(keySpec)
            }
        } catch (e: Exception) {
            throw CosmicExtConnectException("Failed to parse private key: ${e.message}", e)
        }
    }

    /**
     * Check if byte array is PEM-encoded (starts with "-----BEGIN")
     */
    private fun isPemEncoded(data: ByteArray): Boolean {
        if (data.size < 11) return false
        val header = data.copyOfRange(0, 11).toString(Charsets.UTF_8)
        return header.startsWith("-----BEGIN")
    }

    /**
     * Trust all certificates (for pairing only)
     */
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
        private val issuers = emptyArray<java.security.cert.X509Certificate>()
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = issuers
        override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) = Unit
        override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>?, authType: String?) = Unit
    })

    /**
     * Get certificate fingerprint for verification
     *
     * Used during pairing to display fingerprint to user for verification.
     */
    fun getLocalCertificateFingerprint(): String {
        val localDeviceId = org.cosmicext.connect.Helpers.DeviceHelper.getDeviceId(context)
        val cert = certificateStorage.getOrCreateLocalCertificate(localDeviceId)
        return cert.getFingerprintFormatted()
    }

    /**
     * Get remote certificate fingerprint
     *
     * Used during pairing to verify remote device.
     *
     * @param deviceId Remote device ID
     * @return Formatted fingerprint (AA:BB:CC:...)
     * @throws CosmicExtConnectException if certificate not found
     */
    fun getRemoteCertificateFingerprint(deviceId: String): String {
        val cert = certificateStorage.getDeviceCertificate(deviceId)
        return cert.getFingerprintFormatted()
    }
}
