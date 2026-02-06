/*
 * ContactsPlugin.java - This file is part of COSMIC Connect's Android App
 * Implement a way to request and send contact information
 *
 * SPDX-FileCopyrightText: 2018 Simon Redman <simon@ergotech.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ContactsPlugin

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.ContactsHelper
import org.cosmic.cosmicconnect.Helpers.ContactsHelper.ContactNotFoundException
import org.cosmic.cosmicconnect.Helpers.ContactsHelper.VCardBuilder
import org.cosmic.cosmicconnect.Helpers.ContactsHelper.UID
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.UserInterface.AlertDialogFragment
import org.cosmic.cosmicconnect.R
import org.json.JSONObject

class ContactsPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): ContactsPlugin
    }
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_contacts)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_contacts_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_CONTACTS_REQUEST_ALL_UIDS_TIMESTAMPS, PACKET_TYPE_CONTACTS_REQUEST_VCARDS_BY_UIDS)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_CONTACTS_RESPONSE_UIDS_TIMESTAMPS, PACKET_TYPE_CONTACTS_RESPONSE_VCARDS)

    override val permissionExplanation: Int = R.string.contacts_permission_explanation

    override val isEnabledByDefault: Boolean = true

    // One day maybe we will also support WRITE_CONTACTS, but not yet
    override val requiredPermissions: Array<String> = arrayOf(Manifest.permission.READ_CONTACTS)

    override fun checkRequiredPermissions(): Boolean {
        if (!arePermissionsGranted(requiredPermissions)) {
            return false
        }
        return preferences!!.getBoolean("acceptedToTransferContacts", false)
    }

    override fun supportsDeviceSpecificSettings(): Boolean = true

    override val permissionExplanationDialog: DialogFragment
        get() {
            if (!arePermissionsGranted(requiredPermissions)) {
                return super.permissionExplanationDialog
            }
            return AlertDialogFragment.Builder()
                .setTitle(displayName)
                .setMessage(R.string.contacts_per_device_confirmation)
                .setPositiveButton(R.string.ok)
                .setNegativeButton(R.string.cancel)
                .create()
                .apply {
                    setCallback(object : AlertDialogFragment.Callback() {
                        override fun onPositiveButtonClicked(): Boolean {
                            preferences!!.edit { putBoolean("acceptedToTransferContacts", true) }
                            device.launchBackgroundReloadPluginsFromSettings()
                            return true
                        }
                    })
                }
        }

    /**
     * Add custom fields to the vcard to keep track of COSMIC Connect-specific fields
     *
     *
     * These include the local device's uID as well as last-changed timestamp
     *
     *
     * This might be extended in the future to include more fields
     *
     * @param vcard vcard to apply metadata to
     * @param uID   uID to which the vcard corresponds
     * @throws ContactNotFoundException If the given ID for some reason does not match a contact
     * @return The same VCard as was passed in, but now with COSMIC Connect-specific fields
     */
    @Throws(ContactNotFoundException::class)
    private fun addVCardMetadata(vcard: VCardBuilder, uid: UID): VCardBuilder {
        // Append the device ID line
        // Unclear if the deviceID forms a valid name per the vcard spec. Worry about that later..
        vcard.appendLine("X-KDECONNECT-ID-DEV-${device.deviceId}", uid.toString())

        val timestamp: Long = ContactsHelper.getContactTimestamp(context, uid)
        vcard.appendLine("REV", timestamp.toString())

        return vcard
    }

    /**
     * Return a unique identifier (Contacts.LOOKUP_KEY) for all contacts in the Contacts database
     *
     *
     * The identifiers returned can be used in future requests to get more information about the contact
     *
     * @param np The packet containing the request
     * @return true if successfully handled, false otherwise
     */
    private fun handleRequestAllUIDsTimestamps(@Suppress("unused") np: LegacyNetworkPacket): Boolean {
        val uIDsToTimestamps: Map<UID, Long> = ContactsHelper.getAllContactTimestamps(context)

        // Build packet body with nested "uids" object
        // Desktop expects: {"uids": {"1": 12345, "2": 12346}} (UID as key, timestamp as integer)
        val uidsObject = mutableMapOf<String, Long>()
        for ((contactID: UID, timestamp: Long) in uIDsToTimestamps) {
            uidsObject[contactID.toString()] = timestamp
        }
        val body = mapOf(PACKET_UIDS_KEY to uidsObject)

        // Create packet using FFI
        val json = JSONObject(body).toString()
        val packet = ContactsPacketsFFI.createUidsTimestampsResponse(json)

        // Convert and send
        device.sendPacket(TransferPacket(packet))

        return true
    }

    private fun handleRequestVCardsByUIDs(np: LegacyNetworkPacket): Boolean {
        if (PACKET_UIDS_KEY !in np) {
            Log.e("ContactsPlugin", "handleRequestNamesByUIDs received a malformed packet with no uids key")
            return false
        }

        val storedUIDs: List<UID>? = np.getStringList("uids")?.distinct()?.map { UID(it) }
        if (storedUIDs == null) {
            Log.e("ContactsPlugin", "handleRequestNamesByUIDs received a malformed packet with no uids")
            return false
        }

        val uIDsToVCards: Map<UID, VCardBuilder> = ContactsHelper.getVCardsForContactIDs(context, storedUIDs)

        // Build packet body with nested "vcards" object
        // Desktop expects: {"vcards": {"1": "BEGIN:VCARD...", "2": "BEGIN:VCARD..."}}
        val vcardsObject = mutableMapOf<String, String>()
        // ContactsHelper.getVCardsForContactIDs(..) is allowed to reply without some of the requested uIDs if they were not in the database, so update our list
        for ((uid: UID, vcard: VCardBuilder) in uIDsToVCards) {
            try {
                val vcardWithMetadata = addVCardMetadata(vcard, uid)
                // Add the uid -> vcard pairing to the nested vcards object
                vcardsObject[uid.toString()] = vcardWithMetadata.toString()
            } catch (e: ContactNotFoundException) {
                Log.e("ContactsPlugin", "handleRequestVCardsByUIDs failed to find contact with uID $uid")
            }
        }
        val body = mapOf(PACKET_VCARDS_KEY to vcardsObject)

        // Create packet using FFI
        val json = JSONObject(body).toString()
        val packet = ContactsPacketsFFI.createVCardsResponse(json)

        // Convert and send
        device.sendPacket(TransferPacket(packet))

        return true
    }

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean = when (np.type) {
        PACKET_TYPE_CONTACTS_REQUEST_ALL_UIDS_TIMESTAMPS -> this.handleRequestAllUIDsTimestamps(np)
        PACKET_TYPE_CONTACTS_REQUEST_VCARDS_BY_UIDS -> this.handleRequestVCardsByUIDs(np)
        else -> {
            Log.e("ContactsPlugin", "Contacts plugin received an unexpected packet!")
            false
        }
    }

    companion object {
        private const val PACKET_UIDS_KEY: String = "uids"
        private const val PACKET_VCARDS_KEY: String = "vcards"

        /**
         * Used to request the device send the unique ID of every contact
         */
        private const val PACKET_TYPE_CONTACTS_REQUEST_ALL_UIDS_TIMESTAMPS: String = "cconnect.contacts.request_all_uids_timestamps"

        /**
         * Used to request the names for the contacts corresponding to a list of UIDs
         *
         *
         * It shall contain the key "uids", which will have a list of uIDs (long int, as string)
         */
        private const val PACKET_TYPE_CONTACTS_REQUEST_VCARDS_BY_UIDS: String = "cconnect.contacts.request_vcards_by_uid"

        /**
         * Response indicating the packet contains a list of contact uIDs
         *
         *
         * It shall contain the key "uids", which will mark a list of uIDs (long int, as string)
         * The returned IDs can be used in future requests for more information about the contact
         */
        private const val PACKET_TYPE_CONTACTS_RESPONSE_UIDS_TIMESTAMPS: String = "cconnect.contacts.response_uids_timestamps"

        /**
         * Response indicating the packet contains a list of contact names
         *
         *
         * It shall contain the key "uids", which will mark a list of uIDs (long int, as string)
         * then, for each UID, there shall be a field with the key of that UID and the value of the name of the contact
         *
         *
         * For example:
         * { 'uids' : ['1', '3', '15'],
         * '1'  : 'John Smith',
         * '3'  : 'Abe Lincoln',
         * '15' : 'Mom'
         * }
         */
        private const val PACKET_TYPE_CONTACTS_RESPONSE_VCARDS: String = "cconnect.contacts.response_vcards"
    }
}
