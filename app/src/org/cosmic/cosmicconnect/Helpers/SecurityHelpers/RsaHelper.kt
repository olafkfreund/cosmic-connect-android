/*
 * SPDX-FileCopyrightText: 2015 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect.Helpers.SecurityHelpers

import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.runBlocking
import org.cosmic.cosmicconnect.Helpers.PreferenceDataStore
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object RsaHelper {
    private const val RSA = "RSA" // KeyProperties.KEY_ALGORITHM_RSA isn't available until API 23+

    @JvmStatic
    fun initialiseRsaKeys(context: Context) {
        val publicKeyStored = PreferenceDataStore.getPublicKeySync(context)
        val privateKeyStored = PreferenceDataStore.getPrivateKeySync(context)

        if (publicKeyStored.isEmpty() || privateKeyStored.isEmpty()) {
            val keyPair: KeyPair
            val keyAlgorithm: String
            try {
                keyAlgorithm = KeyProperties.KEY_ALGORITHM_EC
                val generator = KeyPairGenerator.getInstance(keyAlgorithm)
                val spec = ECGenParameterSpec("secp256r1")
                generator.initialize(spec)
                keyPair = generator.generateKeyPair()
            }
            catch (e: Exception) {
                Log.e("KDE/initializeRsaKeys", "Exception", e)
                return
            }

            val publicKey = keyPair.public.encoded
            val privateKey = keyPair.private.encoded

            runBlocking {
                PreferenceDataStore.setPublicKey(context, Base64.encodeToString(publicKey, 0))
                PreferenceDataStore.setPrivateKey(context, Base64.encodeToString(privateKey, 0))
                PreferenceDataStore.setAlgorithm(context, keyAlgorithm)
            }
        }
    }

    @JvmStatic
    fun getPublicKey(context: Context): PublicKey {
        val publicKeyStr = PreferenceDataStore.getPublicKeySync(context)
        val publicKeyBytes = Base64.decode(publicKeyStr, 0)
        val algorithm = PreferenceDataStore.getAlgorithmSync(context, RSA)
        return KeyFactory.getInstance(algorithm).generatePublic(X509EncodedKeySpec(publicKeyBytes))
    }

    @JvmStatic
    fun getPrivateKey(context: Context): PrivateKey {
        val privateKeyStr = PreferenceDataStore.getPrivateKeySync(context)
        val privateKeyBytes = Base64.decode(privateKeyStr, 0)
        val algorithm = PreferenceDataStore.getAlgorithmSync(context, RSA)
        return KeyFactory.getInstance(algorithm).generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
    }
}
