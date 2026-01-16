package org.cosmic.cosmicconnect.Core

import uniffi.cosmic_connect_core.*

/**
 * NetworkPacket - Idiomatic Kotlin wrapper for KDE Connect protocol packets
 *
 * Wraps the Rust FFI NetworkPacket with a clean Kotlin API.
 *
 * ## KDE Connect Protocol v7
 * Packets are JSON-formatted with newline termination:
 * ```json
 * {
 *   "id": 1234567890,
 *   "type": "kdeconnect.ping",
 *   "body": {"message": "hello"}
 * }
 * ```
 *
 * ## Usage
 * ```kotlin
 * // Create packet
 * val packet = NetworkPacket.create("kdeconnect.ping", mapOf("message" to "hello"))
 *
 * // Serialize to bytes (for network transmission)
 * val bytes = packet.serialize()
 *
 * // Deserialize from bytes
 * val received = NetworkPacket.deserialize(bytes)
 * ```
 */
data class NetworkPacket(
    val id: Long,
    val type: String,
    val body: Map<String, Any>,
    val payloadSize: Long? = null
) {

    /**
     * Serialize packet to bytes for network transmission
     *
     * Format: JSON with newline terminator (KDE Connect requirement)
     *
     * @return Byte array ready for network transmission
     * @throws CosmicConnectException if serialization fails
     */
    fun serialize(): ByteArray {
        try {
            val ffiPacket = toFfiPacket()
            return serializePacket(ffiPacket)
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to serialize packet: ${e.message}", e)
        }
    }

    /**
     * Convert to FFI packet for Rust calls
     */
    internal fun toFfiPacket(): FfiPacket {
        // Convert body map to JSON string
        val bodyJson = toJsonString(body)

        return FfiPacket(
            id = id,
            packetType = type,
            body = bodyJson,
            payloadSize = payloadSize
        )
    }

    companion object {

        /**
         * Create a new network packet with auto-generated ID
         *
         * @param type Packet type (e.g., "kdeconnect.ping", "kdeconnect.battery")
         * @param body Packet body as key-value map
         * @return New NetworkPacket with unique ID
         * @throws CosmicConnectException if packet creation fails
         * @throws IllegalArgumentException if type is empty or body contains non-serializable values
         */
        fun create(type: String, body: Map<String, Any> = emptyMap()): NetworkPacket {
            // Validation
            require(type.isNotBlank()) { "Packet type cannot be empty" }
            require(body.all { (_, v) -> isSerializable(v) }) {
                "Body contains non-serializable values. Only String, Number, Boolean, null, and collections of these types are allowed."
            }

            try {
                val bodyJson = toJsonString(body)
                val ffiPacket = createPacket(type, bodyJson)
                return fromFfiPacket(ffiPacket)
            } catch (e: Exception) {
                throw CosmicConnectException("Failed to create packet: ${e.message}", e)
            }
        }

        /**
         * Check if a value is serializable for network transmission
         */
        private fun isSerializable(value: Any?): Boolean = when (value) {
            null, is String, is Number, is Boolean -> true
            is Collection<*> -> value.all { isSerializable(it) }
            is Map<*, *> -> value.all { (k, v) -> k is String && isSerializable(v) }
            else -> false
        }

        /**
         * Create a packet with explicit ID
         *
         * @param id Packet ID (must be unique)
         * @param type Packet type
         * @param body Packet body
         * @return NetworkPacket with specified ID
         * @throws CosmicConnectException if packet creation fails
         * @throws IllegalArgumentException if type is empty or body contains non-serializable values
         */
        fun createWithId(id: Long, type: String, body: Map<String, Any> = emptyMap()): NetworkPacket {
            // Validation
            require(type.isNotBlank()) { "Packet type cannot be empty" }
            require(body.all { (_, v) -> isSerializable(v) }) {
                "Body contains non-serializable values. Only String, Number, Boolean, null, and collections of these types are allowed."
            }

            try {
                val bodyJson = toJsonString(body)
                val ffiPacket = createPacketWithId(id, type, bodyJson)
                return fromFfiPacket(ffiPacket)
            } catch (e: Exception) {
                throw CosmicConnectException("Failed to create packet with ID: ${e.message}", e)
            }
        }

        /**
         * Deserialize packet from bytes
         *
         * @param data Byte array received from network
         * @return Deserialized NetworkPacket
         * @throws CosmicConnectException if deserialization fails
         */
        fun deserialize(data: ByteArray): NetworkPacket {
            try {
                val ffiPacket = deserializePacket(data)
                return fromFfiPacket(ffiPacket)
            } catch (e: Exception) {
                throw CosmicConnectException("Failed to deserialize packet: ${e.message}", e)
            }
        }

        /**
         * Convert FFI packet to Kotlin NetworkPacket
         */
        internal fun fromFfiPacket(ffiPacket: FfiPacket): NetworkPacket {
            // Parse JSON body string to map
            val bodyMap = parseJsonString(ffiPacket.body)

            return NetworkPacket(
                id = ffiPacket.id,
                type = ffiPacket.packetType,
                body = bodyMap,
                payloadSize = ffiPacket.payloadSize
            )
        }

        /**
         * Convert legacy NetworkPacket to new immutable NetworkPacket
         *
         * @param legacyPacket Old mutable NetworkPacket from org.cosmic.cosmicconnect package
         * @return New immutable NetworkPacket from org.cosmic.cosmicconnect.Core package
         */
        @JvmStatic
        fun fromLegacyPacket(legacyPacket: org.cosmic.cosmicconnect.NetworkPacket): NetworkPacket {
            // Extract type
            val type = legacyPacket.type

            // Parse serialized packet to extract body
            val serialized = legacyPacket.serialize()
            val jsonPacket = org.json.JSONObject(serialized)
            val jsonBody = jsonPacket.getJSONObject("body")

            // Convert JSONObject to Map<String, Any>
            val body = mutableMapOf<String, Any>()
            val keys = jsonBody.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                body[key] = jsonBody.get(key)
            }

            // Determine payloadSize from packet if available
            val payloadSize = if (jsonPacket.has("payloadSize")) {
                jsonPacket.getLong("payloadSize").takeIf { it > 0 }
            } else null

            // Create new packet
            return create(type, body).copy(payloadSize = payloadSize)
        }

        /**
         * Alias for fromLegacyPacket for consistency with some plugin code
         */
        @JvmStatic
        fun fromLegacy(legacyPacket: org.cosmic.cosmicconnect.NetworkPacket): NetworkPacket {
            return fromLegacyPacket(legacyPacket)
        }

        /**
         * Convert map to JSON string using Android's JSONObject
         *
         * This ensures proper JSON encoding with correct escape sequences
         * for special characters like newlines, tabs, quotes, backslashes, etc.
         */
        private fun toJsonString(map: Map<String, Any>): String {
            if (map.isEmpty()) return "{}"

            return try {
                org.json.JSONObject(map).toString()
            } catch (e: Exception) {
                throw CosmicConnectException("Failed to encode body as JSON: ${e.message}", e)
            }
        }

        /**
         * Parse JSON string to map using Android's JSONObject
         *
         * @throws CosmicConnectException if JSON is invalid
         */
        @Suppress("UNCHECKED_CAST")
        private fun parseJsonString(json: String): Map<String, Any> {
            if (json == "{}") return emptyMap()

            return try {
                val jsonObject = org.json.JSONObject(json)
                val map = mutableMapOf<String, Any>()

                jsonObject.keys().forEach { key ->
                    val value = jsonObject.get(key)
                    map[key] = when (value) {
                        is org.json.JSONObject -> value.toString()
                        is org.json.JSONArray -> value.toString()
                        org.json.JSONObject.NULL -> "null"
                        else -> value
                    }
                }

                map
            } catch (e: Exception) {
                val preview = if (json.length > 100) json.take(100) + "..." else json
                throw CosmicConnectException("Failed to parse JSON body: $preview", e)
            }
        }
    }

    /**
     * Check if this packet has a payload
     */
    val hasPayload: Boolean
        get() = payloadSize != null && payloadSize > 0

    /**
     * Get packet body as string (for debugging)
     */
    fun bodyAsString(): String {
        return body.entries.joinToString(", ") { "${it.key}=${it.value}" }
    }

    override fun toString(): String = buildString {
        append("NetworkPacket(id=$id, type='$type'")
        if (body.isNotEmpty()) append(", body=${bodyAsString()}")
        if (payloadSize != null) append(", payloadSize=$payloadSize")
        append(")")
    }
}

/**
 * Common packet types (for convenience)
 */
object PacketType {
    const val IDENTITY = "kdeconnect.identity"
    const val PAIR = "kdeconnect.pair"
    const val ENCRYPTED = "kdeconnect.encrypted"

    // Plugin packet types
    const val PING = "kdeconnect.ping"
    const val BATTERY = "kdeconnect.battery"
    const val BATTERY_REQUEST = "kdeconnect.battery.request"
    const val SHARE_REQUEST = "kdeconnect.share.request"
    const val SHARE_REQUEST_UPDATE = "kdeconnect.share.request.update"
    const val CLIPBOARD = "kdeconnect.clipboard"
    const val CLIPBOARD_CONNECT = "kdeconnect.clipboard.connect"
    const val MPRIS = "kdeconnect.mpris"
    const val MPRIS_REQUEST = "kdeconnect.mpris.request"
    const val NOTIFICATION = "kdeconnect.notification"
    const val NOTIFICATION_REQUEST = "kdeconnect.notification.request"
    const val RUNCOMMAND = "kdeconnect.runcommand"
    const val RUNCOMMAND_REQUEST = "kdeconnect.runcommand.request"
    const val TELEPHONY = "kdeconnect.telephony"
    const val SMS_REQUEST = "kdeconnect.sms.request"
    const val SMS_MESSAGES = "kdeconnect.sms.messages"
    const val MOUSEPAD_REQUEST = "kdeconnect.mousepad.request"
    const val MOUSEPAD_ECHO = "kdeconnect.mousepad.echo"
    const val MOUSEPAD_KEYBOARDSTATE = "kdeconnect.mousepad.keyboardstate"
    const val PRESENTER = "kdeconnect.presenter"
    const val SFTP = "kdeconnect.sftp"
    const val SFTP_REQUEST = "kdeconnect.sftp.request"
    const val FINDMYPHONE_REQUEST = "kdeconnect.findmyphone.request"
}
