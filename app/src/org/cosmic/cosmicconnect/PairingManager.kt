/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.cosmic.cosmicconnect.Helpers.NotificationHelper
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.Helpers.TrustedDevices
import org.cosmic.cosmicconnect.PairingHandler.PairingCallback
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages pairing state, callbacks, and notification UI for a device.
 */
class PairingManager(
    private val context: Context,
    private val device: Device,
    private val deviceInfo: DeviceInfo,
    private val sslHelper: SslHelper,
    private val onPluginsReload: () -> Unit,
    private val onPluginsUnpaired: (Context, String) -> Unit,
    initialState: PairingHandler.PairState
) {
    private var notificationId = 0
    private val pairingCallbacks = CopyOnWriteArrayList<PairingCallback>()

    val pairingHandler: PairingHandler = PairingHandler(
        device, createDefaultPairingCallback(), initialState, sslHelper
    )

    val isPaired: Boolean
        get() = pairingHandler.state == PairingHandler.PairState.Paired

    val pairStatus: PairingHandler.PairState
        get() = pairingHandler.state

    val verificationKey: String?
        get() = pairingHandler.verificationKey()

    fun addPairingCallback(callback: PairingCallback) = pairingCallbacks.add(callback)

    fun removePairingCallback(callback: PairingCallback) = pairingCallbacks.remove(callback)

    fun requestPairing() = pairingHandler.requestPairing()

    fun unpair() = pairingHandler.unpair()

    fun acceptPairing() {
        Log.i("PairingManager", "Accepted pair request started by the other device")
        pairingHandler.acceptPairing()
    }

    fun cancelPairing() {
        Log.i("PairingManager", "This side cancelled the pair request")
        pairingHandler.cancelPairing()
    }

    private fun createDefaultPairingCallback(): PairingCallback {
        return object : PairingCallback {
            override fun incomingPairRequest() {
                displayPairingNotification()
                pairingCallbacks.forEach(PairingCallback::incomingPairRequest)
            }

            override fun pairingSuccessful() {
                Log.i("PairingManager", "pairing successful, adding to trusted devices list")
                hidePairingNotification()
                deviceInfo.saveInSettings(context)
                TrustedDevices.addTrustedDevice(context, deviceInfo.id)
                try {
                    onPluginsReload()
                    pairingCallbacks.forEach(PairingCallback::pairingSuccessful)
                } catch (e: Exception) {
                    Log.e("PairingManager", "Exception in pairingSuccessful. Not unpairing because saving the trusted device succeeded", e)
                }
            }

            override fun pairingFailed(error: String) {
                hidePairingNotification()
                pairingCallbacks.forEach { it.pairingFailed(error) }
            }

            override fun unpaired(device: Device) {
                assert(device == this@PairingManager.device)
                Log.i("PairingManager", "unpaired, removing from trusted devices list")
                TrustedDevices.removeTrustedDevice(context, deviceInfo.id)
                onPluginsUnpaired(context, deviceInfo.id)
                onPluginsReload()
                pairingCallbacks.forEach { it.unpaired(this@PairingManager.device) }
            }
        }
    }

    fun displayPairingNotification() {
        hidePairingNotification()
        notificationId = System.currentTimeMillis().toInt()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DEVICE_ID, deviceInfo.id)
            putExtra(MainActivity.PAIR_REQUEST_STATUS, MainActivity.PAIRING_PENDING)
        }
        val pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DEVICE_ID, deviceInfo.id)
            putExtra(MainActivity.PAIR_REQUEST_STATUS, MainActivity.PAIRING_ACCEPTED)
        }
        val rejectIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_DEVICE_ID, deviceInfo.id)
            putExtra(MainActivity.PAIR_REQUEST_STATUS, MainActivity.PAIRING_REJECTED)
        }
        val acceptedPendingIntent = PendingIntent.getActivity(context, 2, acceptIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        val rejectedPendingIntent = PendingIntent.getActivity(context, 4, rejectIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val res = context.resources
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)!!

        val noti = NotificationCompat.Builder(context, NotificationHelper.Channels.DEFAULT)
            .setContentTitle(res.getString(R.string.pairing_request_from, device.name))
            .setContentText(res.getString(R.string.pairing_verification_code, verificationKey))
            .setTicker(res.getString(R.string.pair_requested))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_accept_pairing_24dp, res.getString(R.string.pairing_accept), acceptedPendingIntent)
            .addAction(R.drawable.ic_reject_pairing_24dp, res.getString(R.string.pairing_reject), rejectedPendingIntent)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        notificationManager.notify(notificationId, noti)
    }

    fun hidePairingNotification() {
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)!!
        notificationManager.cancel(notificationId)
    }
}
