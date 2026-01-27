/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2018 Simon Redman <simon@ergotech.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Helpers

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.PhoneLookup
import android.util.Base64
import android.util.Base64OutputStream
import android.util.Log
import org.apache.commons.io.IOUtils
import java.io.ByteArrayOutputStream

object ContactsHelper {

    private const val LOG_TAG = "ContactsHelper"

    /**
     * Lookup the name and photoID of a contact given a phone number
     */
    @JvmStatic
    fun phoneNumberLookup(context: Context, number: String?): Map<String, String> {
        val contactInfo = HashMap<String, String>()
        if (number == null) return contactInfo

        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val columns = arrayOf(
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.PHOTO_URI
        )
        try {
            context.contentResolver.query(uri, columns, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { contactInfo["name"] = it }
                    }

                    val photoIndex = cursor.getColumnIndex(PhoneLookup.PHOTO_URI)
                    if (photoIndex != -1) {
                        cursor.getString(photoIndex)?.let { contactInfo["photoID"] = it }
                    }
                }
            }
        } catch (ignored: Exception) { 
        }
        return contactInfo
    }

    @JvmStatic
    fun photoId64Encoded(context: Context, photoId: String?): String {
        if (photoId == null) {
            return ""
        }
        val photoUri = Uri.parse(photoId)

        val encodedPhoto = ByteArrayOutputStream()
        return try {
            context.contentResolver.openInputStream(photoUri).use { input ->
                Base64OutputStream(encodedPhoto, Base64.DEFAULT).use { output ->
                    IOUtils.copy(input, output, 1024)
                }
            }
            encodedPhoto.toString()
        } catch (ex: Exception) {
            Log.e(LOG_TAG, ex.toString())
            ""
        }
    }

    /**
     * Return all the NAME_RAW_CONTACT_IDS which contribute an entry to a Contact in the database
     */
    @JvmStatic
    fun getAllContactContactIDs(context: Context): List<UID> {
        val toReturn = ArrayList<UID>()

        val columns = arrayOf(ContactsContract.Contacts.LOOKUP_KEY)
        val contactsUri = ContactsContract.Contacts.CONTENT_URI
        
        try {
            context.contentResolver.query(contactsUri, columns, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val idIndex = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                    do {
                        if (idIndex != -1) {
                            val lookupKey = cursor.getString(idIndex)
                            if (lookupKey != null) {
                                val contactID = UID(lookupKey)
                                if (!toReturn.contains(contactID)) {
                                    toReturn.add(contactID)
                                }
                            }
                        } else {
                            Log.e(LOG_TAG, "Got a contact which does not have a LOOKUP_KEY")
                        }
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error fetching contact IDs", e)
        }

        return toReturn
    }

    /**
     * Get the VCard for every specified raw contact ID
     */
    @JvmStatic
    fun getVCardsForContactIDs(context: Context, IDs: Collection<UID>): Map<UID, VCardBuilder> {
        val toReturn = HashMap<UID, VCardBuilder>()

        for (ID in IDs) {
            val lookupKey = ID.toString()
            val vcardURI = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey)

            try {
                context.contentResolver.openInputStream(vcardURI).use { input ->
                    if (input == null) {
                        Log.w("Contacts", "ContentResolver did not give us a stream for the VCard for UID $ID")
                    } else {
                        toReturn[ID] = VCardBuilder(IOUtils.toString(input, Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                Log.e("Contacts", "Exception while fetching vcards", e)
            }
        }

        return toReturn
    }

    /**
     * Get the last-modified timestamp for every contact in the database
     */
    @JvmStatic
    fun getAllContactTimestamps(context: Context): Map<UID, Long> {
        val projection = arrayOf(UID.COLUMN, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
        val databaseValues = accessContactsDatabase(context, projection, null, null, null)

        val timestamps = HashMap<UID, Long>()
        for ((contactID, data) in databaseValues) {
            data[ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP]?.toLongOrNull()?.let {
                timestamps[contactID] = it
            }
        }

        return timestamps
    }

    /**
     * Get the last-modified timestamp for the specified contact
     */
    @JvmStatic
    @Throws(ContactNotFoundException::class)
    fun getContactTimestamp(context: Context, contactID: UID): Long {
        val projection = arrayOf(UID.COLUMN, ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)
        val selection = "${UID.COLUMN} = ?"
        val selectionArgs = arrayOf(contactID.toString())

        val databaseValue = accessContactsDatabase(context, projection, selection, selectionArgs, null)

        if (databaseValue.isEmpty()) {
            throw ContactNotFoundException("Querying for contact with id $contactID returned no results.")
        }

        if (databaseValue.size != 1) {
            Log.w(LOG_TAG, "Received an improper number of return values from the database in getContactTimestamp: ${databaseValue.size}")
        }

        return databaseValue[contactID]?.get(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)?.toLongOrNull() ?: 0L
    }

    private fun accessContactsDatabase(
        context: Context,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Map<UID, Map<String, String>> {
        val contactsUri = ContactsContract.Contacts.CONTENT_URI
        val toReturn = HashMap<UID, Map<String, String>>()

        try {
            context.contentResolver.query(contactsUri, projection, selection, selectionArgs, sortOrder).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val uIDIndex = cursor.getColumnIndexOrThrow(UID.COLUMN)
                    do {
                        val requestedData = HashMap<String, String>()
                        val lookupKey = cursor.getString(uIDIndex) ?: continue
                        val uID = UID(lookupKey)

                        for (column in projection) {
                            val index = cursor.getColumnIndex(column)
                            if (index == -1) {
                                Log.e(LOG_TAG, "Got a contact which does not have a requested column")
                                continue
                            }
                            cursor.getString(index)?.let { requestedData[column] = it }
                        }
                        toReturn[uID] = requestedData
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error accessing contacts database", e)
        }
        return toReturn
    }

    class VCardBuilder(vcard: String) {
        private val vcardBody: StringBuilder

        init {
            val endIndex = vcard.indexOf(VCARD_END)
            val baseVcard = if (endIndex != -1) vcard.substring(0, endIndex) else vcard
            vcardBody = StringBuilder(baseVcard)
        }

        fun appendLine(propertyName: String, rawValue: String) {
            vcardBody.append(propertyName)
                .append(VCARD_DATA_SEPARATOR)
                .append(rawValue)
                .append("\n")
        }

        override fun toString(): String {
            return vcardBody.toString() + VCARD_END
        }

        companion object {
            private const val VCARD_END = "END:VCARD"
            private const val VCARD_DATA_SEPARATOR = ":"
        }
    }

    class UID(val contactLookupKey: String) {
        init {
            requireNotNull(contactLookupKey) { "lookUpKey should not be null" }
        }

        override fun toString(): String = contactLookupKey

        override fun hashCode(): Int = contactLookupKey.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other is UID) {
                return contactLookupKey == other.contactLookupKey
            }
            if (other is String) {
                return contactLookupKey == other
            }
            return false
        }

        companion object {
            const val COLUMN = ContactsContract.Contacts.LOOKUP_KEY
        }
    }

    class ContactNotFoundException : Exception {
        constructor(contactID: UID) : super("Unable to find contact with ID $contactID")
        constructor(message: String) : super(message)
    }
}
