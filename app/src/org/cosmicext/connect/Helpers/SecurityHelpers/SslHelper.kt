/*
 * SPDX-FileCopyrightText: 2015 Vineet Garg <grg.vineet@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmicext.connect.Helpers.SecurityHelpers

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.PreferenceDataStore
import org.cosmicext.connect.Helpers.RandomHelper
import org.cosmicext.connect.Helpers.TrustedDevices
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Formatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Singleton
class SslHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rsaHelper: RsaHelper
) {
    private lateinit var _certificate: Certificate
    val certificate: Certificate
        get() = _certificate
    private val factory: CertificateFactory = CertificateFactory.getInstance("X.509")

    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
        private val issuers = emptyArray<X509Certificate>()
        override fun getAcceptedIssuers(): Array<X509Certificate> = issuers
        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) = Unit
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) = Unit
    })

    fun initialiseCertificate() {
        val privateKey: PrivateKey = rsaHelper.getPrivateKey()
        val publicKey: PublicKey = rsaHelper.getPublicKey()

        Log.i(LOG_TAG, "Key algorithm: " + publicKey.algorithm)

        val deviceId = DeviceHelper.getDeviceId(context)

        var needsToGenerateCertificate = false
        val storedCert = PreferenceDataStore.getCertificateSync(context)

        if (storedCert.isNotEmpty()) {
            val currDate = Date()
            try {
                val certificateBytes = Base64.decode(storedCert, 0)
                val cert = parseCertificate(certificateBytes) as X509Certificate

                val certDeviceId = getCommonNameFromCertificate(cert)
                if (certDeviceId != deviceId) {
                    Log.e(LOG_TAG,"The certificate stored is from a different device id! (found: $certDeviceId expected:$deviceId)")
                    needsToGenerateCertificate = true
                } else if (cert.notAfter.time < currDate.time) {
                    Log.e(LOG_TAG, "The certificate expired: " + cert.notAfter)
                    needsToGenerateCertificate = true
                } else if (cert.notBefore.time > currDate.time) {
                    Log.e(LOG_TAG, "The certificate is not effective yet: " + cert.notBefore)
                    needsToGenerateCertificate = true
                } else {
                    _certificate = cert
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Exception reading own certificate", e)
                needsToGenerateCertificate = true
            }
        } else {
            needsToGenerateCertificate = true
        }

        if (needsToGenerateCertificate) {
            TrustedDevices.removeAllTrustedDevices(context)
            Log.i(LOG_TAG, "Generating a certificate")
            //Fix for https://issuetracker.google.com/issues/37095309
            val initialLocale = Locale.getDefault()
            setLocale(Locale.ENGLISH, context)

            val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
            nameBuilder.addRDN(BCStyle.CN, deviceId)
            nameBuilder.addRDN(BCStyle.OU, "COSMIC Connect")
            nameBuilder.addRDN(BCStyle.O, "COSMIC")
            val localDate = LocalDate.now()
            val notBefore = localDate.minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val notAfter = localDate.plusYears(10).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val certificateBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                nameBuilder.build(),
                BigInteger.ONE,
                Date.from(notBefore),
                Date.from(notAfter),
                nameBuilder.build(),
                publicKey
            )
            val keyAlgorithm = privateKey.algorithm
            val signatureAlgorithm = if ("RSA" == keyAlgorithm) "SHA512withRSA" else "SHA512withECDSA"
            val contentSigner = JcaContentSignerBuilder(signatureAlgorithm).build(privateKey)
            val certificateBytes = certificateBuilder.build(contentSigner).encoded
            _certificate = parseCertificate(certificateBytes)

            runBlocking {
                PreferenceDataStore.setCertificate(context, Base64.encodeToString(certificateBytes, 0))
            }

            setLocale(initialLocale, context)
        }
    }

    private fun setLocale(locale: Locale, context: Context) {
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.locale = locale
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun getSslContextForDevice(deviceId: String, isDeviceTrusted: Boolean): SSLContext {
        val privateKey = rsaHelper.getPrivateKey()

        // Setup keystore
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setKeyEntry("key", privateKey, "".toCharArray(), arrayOf(certificate))

        // Add device certificate if device trusted
        if (isDeviceTrusted) {
            val remoteDeviceCertificate = TrustedDevices.getDeviceCertificate(context, deviceId)
            keyStore.setCertificateEntry(deviceId, remoteDeviceCertificate)
        }

        // Setup key manager factory
        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "".toCharArray())

        // Setup default trust manager
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        // Setup custom trust manager if device not trusted
        val tlsContext = SSLContext.getInstance("TLSv1.2") // Use TLS up to 1.2, since 1.3 seems to cause issues in some (older?) devices
        if (isDeviceTrusted) {
            tlsContext.init(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers, RandomHelper.secureRandom)
        } else {
            tlsContext.init(keyManagerFactory.keyManagers, trustAllCerts, RandomHelper.secureRandom)
        }
        return tlsContext
    }

    private fun configureSslSocket(socket: SSLSocket, isDeviceTrusted: Boolean, isClient: Boolean) {
        socket.setSoTimeout(10000)
        if (isClient) {
            socket.useClientMode = true
        } else {
            socket.useClientMode = false
            if (isDeviceTrusted) {
                socket.needClientAuth = true
            } else {
                socket.wantClientAuth = true
            }
        }
    }

    fun convertToSslSocket(socket: Socket, deviceId: String, isDeviceTrusted: Boolean, clientMode: Boolean): SSLSocket {
        // Use BouncyCastle path for TLS — the FFI path (SslHelperFFI/SslContextFactory)
        // generates its own RSA key pair in Rust, which is different from the Android
        // Keystore key used here. The FFI certificates also cause TLS handshake timeouts
        // with KDE Connect peers (likely due to rcgen certificate format differences).
        // TODO(#157): Unify certificate generation to use a single key pair
        val sslSocketFactory = getSslContextForDevice(deviceId, isDeviceTrusted).socketFactory
        val sslSocket = sslSocketFactory.createSocket(socket, socket.inetAddress.hostAddress, socket.port, true) as SSLSocket
        configureSslSocket(sslSocket, isDeviceTrusted, clientMode)
        return sslSocket
    }

    fun getCertificateHash(certificate: Certificate): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
        val formatter = Formatter()
        for (b in hash) {
            formatter.format("%02x:", b)
        }
        return formatter.toString()
    }

    fun parseCertificate(certificateBytes: ByteArray): Certificate {
        return factory.generateCertificate(ByteArrayInputStream(certificateBytes))
    }

    fun getCommonNameFromCertificate(cert: X509Certificate): String {
        val principal = cert.subjectX500Principal
        val x500name = X500Name(principal.name)
        val rdn = x500name.getRDNs(BCStyle.CN).first()
        return IETFUtils.valueToString(rdn.first.value)
    }

    companion object {
        private const val LOG_TAG = "COSMIC/SslHelper"
    }
}