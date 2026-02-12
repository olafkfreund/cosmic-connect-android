/*
 * SPDX-FileCopyrightText: 2019 Simon Redman <simon@ergotech.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

object TelephonyHelper {

    const val LOGGING_TAG = "TelephonyHelper"

    /**
     * Try to get the phone number currently active on the phone
     *
     * Make sure that you have the READ_PHONE_STATE permission!
     *
     * Note that entries of the returned list might return null if the phone number is not known by the device
     */
    @JvmStatic
    @Throws(SecurityException::class)
    fun getAllPhoneNumbers(context: Context): List<LocalPhoneNumber> {
        // Each subscription is a different SIM card
        val subscriptionManager = ContextCompat.getSystemService(context, SubscriptionManager::class.java)
        if (subscriptionManager == null) {
            Log.w(LOGGING_TAG, "Could not get SubscriptionManager")
            return emptyList()
        }
        val subscriptionInfos = subscriptionManager.activeSubscriptionInfoList
        if (subscriptionInfos == null) {
            Log.w(LOGGING_TAG, "Could not get SubscriptionInfos")
            return emptyList()
        }
        val phoneNumbers = ArrayList<LocalPhoneNumber>(subscriptionInfos.size)
        for (info in subscriptionInfos) {
            val thisPhoneNumber = LocalPhoneNumber(info.number, info.subscriptionId)
            phoneNumbers.add(thisPhoneNumber)
        }
        return phoneNumbers.filter { it.number != null }
    }

    /**
     * Try to get the phone number to which the TelephonyManager is pinned
     */
    @JvmStatic
    @Throws(SecurityException::class)
    fun getPhoneNumber(telephonyManager: TelephonyManager): LocalPhoneNumber? {
        @SuppressLint("HardwareIds")
        val maybeNumber = telephonyManager.line1Number

        if (maybeNumber == null) {
            Log.d(LOGGING_TAG, "Got 'null' instead of a phone number")
            return null
        }
        
        var digitCount = 0
        for (digit in "0123456789".toCharArray()) {
            val count = maybeNumber.length - maybeNumber.replace(digit.toString(), "").length
            digitCount += count
        }
        
        return if (maybeNumber.length > digitCount * 4) {
            Log.d(LOGGING_TAG, "Discarding $maybeNumber because it does not contain a high enough digit ratio to be a real phone number")
            null
        } else {
            LocalPhoneNumber(maybeNumber, -1)
        }
    }

    /**
     * Get the APN settings of the current APN for the given subscription ID
     */
    @SuppressLint("InlinedApi", "Range")
    @JvmStatic
    fun getPreferredApn(context: Context, subscriptionId: Int): ApnSetting? {
        val APN_PROJECTION = arrayOf(
            Telephony.Carriers.TYPE,
            Telephony.Carriers.MMSC,
            Telephony.Carriers.MMSPROXY,
            Telephony.Carriers.MMSPORT
        )

        val telephonyCarriersUri = Telephony.Carriers.CONTENT_URI
        val telephonyCarriersPreferredApnUri = Uri.withAppendedPath(telephonyCarriersUri, "/preferapn/subId/$subscriptionId")

        try {
            context.contentResolver.query(
                telephonyCarriersPreferredApnUri,
                APN_PROJECTION,
                null,
                null,
                Telephony.Carriers.DEFAULT_SORT_ORDER
            ).use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    val type = cursor.getString(cursor.getColumnIndex(Telephony.Carriers.TYPE))
                    if (!isValidApnType(type, APN_TYPE_MMS)) continue

                    val apnBuilder = ApnSetting.Builder()
                        .setMmsc(Uri.parse(cursor.getString(cursor.getColumnIndex(Telephony.Carriers.MMSC))))
                        .setMmsProxyAddress(cursor.getString(cursor.getColumnIndex(Telephony.Carriers.MMSPROXY)))

                    val maybeMmsProxyPort = cursor.getString(cursor.getColumnIndex(Telephony.Carriers.MMSPORT))
                    try {
                        val mmsProxyPort = maybeMmsProxyPort.toInt()
                        apnBuilder.setMmsProxyPort(mmsProxyPort)
                    } catch (e: Exception) {
                        // Use default
                    }

                    return apnBuilder.build()
                }
            }
        } catch (e: Exception) {
            Log.e(LOGGING_TAG, "Error encountered while trying to read APNs", e)
        }

        return null
    }

    private const val APN_TYPE_ALL = "*"
    private const val APN_TYPE_MMS = "mms"

    @JvmStatic
    fun isValidApnType(types: String?, requestType: String): Boolean {
        if (types.isNullOrEmpty()) {
            return true
        }
        for (type in types.split(",")) {
            val trimmedType = type.trim()
            if (trimmedType == requestType || trimmedType == APN_TYPE_ALL) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun canonicalizePhoneNumber(phoneNumber: String): String {
        var toReturn = phoneNumber
        toReturn = toReturn.replace(" ", "")
        toReturn = toReturn.replace("-", "")
        toReturn = toReturn.replace("(", "")
        toReturn = toReturn.replace(")", "")
        toReturn = toReturn.replace("+", "")
        toReturn = toReturn.replaceFirst("^0*".toRegex(), "")

        return if (toReturn.isEmpty()) {
            phoneNumber
        } else {
            toReturn
        }
    }

    class ApnSetting private constructor() {
        var mmsc: Uri? = null
            private set
        var mmsProxyAddressAsString: String? = null
            private set
        var mmsProxyPort: Int = 80
            private set

        class Builder {
            private val internalApnSetting = ApnSetting()

            fun setMmsc(mmscUri: Uri?): Builder {
                internalApnSetting.mmsc = mmscUri
                return this
            }

            fun setMmsProxyAddress(mmsProxy: String?): Builder {
                internalApnSetting.mmsProxyAddressAsString = mmsProxy
                return this
            }

            fun setMmsProxyPort(mmsPort: Int): Builder {
                internalApnSetting.mmsProxyPort = mmsPort
                return this
            }

            fun build(): ApnSetting = internalApnSetting
        }
    }

    class LocalPhoneNumber(val number: String?, val subscriptionID: Int) {
        override fun toString(): String = number ?: ""

        fun isMatchingPhoneNumber(potentialMatchingPhoneNumber: String): Boolean {
            val mPhoneNumber = canonicalizePhoneNumber(this.number ?: "")
            val oPhoneNumber = canonicalizePhoneNumber(potentialMatchingPhoneNumber)

            if (mPhoneNumber.isEmpty() || oPhoneNumber.isEmpty()) {
                return false
            }

            val longerNumber = if (mPhoneNumber.length >= oPhoneNumber.length) mPhoneNumber else oPhoneNumber
            val shorterNumber = if (mPhoneNumber.length < oPhoneNumber.length) mPhoneNumber else oPhoneNumber

            if (shorterNumber.length < 0.75 * longerNumber.length) {
                return false
            }

            return longerNumber.endsWith(shorterNumber)
        }
    }
}
