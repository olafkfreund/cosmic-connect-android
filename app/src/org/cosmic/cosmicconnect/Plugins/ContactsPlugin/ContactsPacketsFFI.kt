package org.cosmic.cosmicconnect.Plugins.ContactsPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createContactsResponseUids
import uniffi.cosmic_connect_core.createContactsResponseVcards

/**
 * FFI wrapper for creating Contacts plugin packets
 *
 * The Contacts plugin enables sharing contact information (vCards) between devices.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Share contact UIDs and timestamps for synchronization
 * - Share full vCard data including names, phone numbers, etc.
 * - Support for bulk contact transfer
 *
 * ## Usage
 *
 * **Sending UIDs and timestamps:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val uidsMap = mapOf(
 *     "uids" to listOf("1", "3", "15"),
 *     "1" to "1234567890",
 *     "3" to "1234567891",
 *     "15" to "1234567892"
 * )
 * val json = JSONObject(uidsMap).toString()
 * val packet = ContactsPacketsFFI.createUidsTimestampsResponse(json)
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending vCards:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val vcardsMap = mapOf(
 *     "uids" to listOf("1", "3"),
 *     "1" to "BEGIN:VCARD\nFN:John Smith\nEND:VCARD",
 *     "3" to "BEGIN:VCARD\nFN:Jane Doe\nEND:VCARD"
 * )
 * val json = JSONObject(vcardsMap).toString()
 * val packet = ContactsPacketsFFI.createVCardsResponse(json)
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * @see ContactsPlugin
 */
object ContactsPacketsFFI {

    /**
     * Create a contacts response packet with UIDs and timestamps
     *
     * Creates a packet containing contact unique IDs and their last-modified timestamps.
     * Used for contact synchronization to determine which contacts need to be synced.
     *
     * The UIDs JSON should be formatted as:
     * ```json
     * {
     *   "uids": ["1", "3", "15"],
     *   "1": "1234567890",
     *   "3": "1234567891",
     *   "15": "1234567892"
     * }
     * ```
     *
     * @param uidsJson JSON string containing UIDs and timestamps
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createUidsTimestampsResponse(uidsJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createContactsResponseUids(uidsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a contacts response packet with vCards
     *
     * Creates a packet containing full vCard data for requested contacts.
     * Transfers complete contact information including names, phone numbers,
     * email addresses, and other vCard fields.
     *
     * The vCards JSON should be formatted as:
     * ```json
     * {
     *   "uids": ["1", "3"],
     *   "1": "BEGIN:VCARD\nFN:John Smith\nTEL:555-1234\nEND:VCARD",
     *   "3": "BEGIN:VCARD\nFN:Jane Doe\nTEL:555-5678\nEND:VCARD"
     * }
     * ```
     *
     * @param vcardsJson JSON string containing UIDs and vCard data
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createVCardsResponse(vcardsJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createContactsResponseVcards(vcardsJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
