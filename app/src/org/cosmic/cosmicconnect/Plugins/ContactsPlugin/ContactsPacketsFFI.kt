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
 * // Desktop expects nested "uids" object with UID as key, timestamp as integer
 * val uidsObject = mapOf("1" to 1234567890L, "3" to 1234567891L, "15" to 1234567892L)
 * val body = mapOf("uids" to uidsObject)
 * val json = JSONObject(body).toString()
 * // Result: {"uids": {"1": 1234567890, "3": 1234567891, "15": 1234567892}}
 * val packet = ContactsPacketsFFI.createUidsTimestampsResponse(json)
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending vCards:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * // Desktop expects nested "vcards" object with UID as key, vCard as value
 * val vcardsObject = mapOf(
 *     "1" to "BEGIN:VCARD\nFN:John Smith\nEND:VCARD",
 *     "3" to "BEGIN:VCARD\nFN:Jane Doe\nEND:VCARD"
 * )
 * val body = mapOf("vcards" to vcardsObject)
 * val json = JSONObject(body).toString()
 * // Result: {"vcards": {"1": "BEGIN:VCARD...", "3": "BEGIN:VCARD..."}}
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
     *   "uids": {
     *     "1": 1234567890,
     *     "3": 1234567891,
     *     "15": 1234567892
     *   }
     * }
     * ```
     *
     * Note: Desktop expects timestamps as integers (i64), not strings.
     *
     * @param uidsJson JSON string containing nested "uids" object with UID keys and integer timestamps
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
     *   "vcards": {
     *     "1": "BEGIN:VCARD\nFN:John Smith\nTEL:555-1234\nEND:VCARD",
     *     "3": "BEGIN:VCARD\nFN:Jane Doe\nTEL:555-5678\nEND:VCARD"
     *   }
     * }
     * ```
     *
     * @param vcardsJson JSON string containing nested "vcards" object with UID keys and vCard strings
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
