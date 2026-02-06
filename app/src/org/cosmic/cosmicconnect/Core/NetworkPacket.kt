package org.cosmic.cosmicconnect.Core

import uniffi.cosmic_connect_core.*

/**
 * NetworkPacket - Idiomatic Kotlin wrapper for COSMIC Connect protocol packets
 *
 * Wraps the Rust FFI NetworkPacket with a clean Kotlin API.
 *
 * ## COSMIC Connect Protocol v7
 * Packets are JSON-formatted with newline termination:
 * ```json
 * {
 *   "id": 1234567890,
 *   "type": "cconnect.ping",
 *   "body": {"message": "hello"}
 * }
 * ```
 *
 * ## Usage
 * ```kotlin
 * // Create packet
 * val packet = NetworkPacket.create("cconnect.ping", mapOf("message" to "hello"))
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
    val payloadSize: Long? = null,
    val payloadTransferInfo: Map<String, Any> = emptyMap()
) {

    /**
     * Serialize packet to bytes for network transmission
     *
     * Format: JSON with newline terminator (COSMIC Connect requirement)
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

    /**
     * Convert to legacy NetworkPacket for backward compatibility
     *
     * @return Legacy mutable NetworkPacket from org.cosmic.cosmicconnect package
     */
    fun toLegacyPacket(): org.cosmic.cosmicconnect.NetworkPacket {
        val legacyPacket = org.cosmic.cosmicconnect.NetworkPacket(type)

        // Copy all body fields
        body.forEach { (key, value) ->
            setLegacyPacketValue(legacyPacket, key, value)
        }

        // Copy payloadTransferInfo if present
        if (payloadTransferInfo.isNotEmpty()) {
            legacyPacket.payloadTransferInfo = mapToJsonObject(payloadTransferInfo)
        }

        return legacyPacket
    }

    /**
     * Set a value on legacy packet with proper type handling
     */
    private fun setLegacyPacketValue(
        legacyPacket: org.cosmic.cosmicconnect.NetworkPacket,
        key: String,
        value: Any
    ) {
        when (value) {
            is String -> legacyPacket[key] = value
            is Int -> legacyPacket[key] = value
            is Long -> legacyPacket[key] = value
            is Boolean -> legacyPacket[key] = value
            is Double -> legacyPacket[key] = value
            is Float -> legacyPacket[key] = value.toDouble()
            is List<*> -> {
                // Convert List to JSONArray for proper serialization
                val jsonArray = org.json.JSONArray()
                value.forEach { item ->
                    when (item) {
                        is String -> jsonArray.put(item)
                        is Number -> jsonArray.put(item)
                        is Boolean -> jsonArray.put(item)
                        is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                        else -> item?.let { jsonArray.put(it.toString()) }
                    }
                }
                legacyPacket[key] = jsonArray
            }
            is Map<*, *> -> {
                // Convert Map to JSONObject
                legacyPacket[key] = mapToJsonObject(value)
            }
            else -> legacyPacket[key] = value.toString()
        }
    }

    /**
     * Convert a Map to JSONObject recursively
     */
    private fun mapToJsonObject(map: Map<*, *>): org.json.JSONObject {
        val jsonObject = org.json.JSONObject()
        map.forEach { (k, v) ->
            val key = k?.toString() ?: return@forEach
            when (v) {
                null -> jsonObject.put(key, org.json.JSONObject.NULL)
                is String -> jsonObject.put(key, v)
                is Number -> jsonObject.put(key, v)
                is Boolean -> jsonObject.put(key, v)
                is List<*> -> {
                    val jsonArray = org.json.JSONArray()
                    v.forEach { item ->
                        when (item) {
                            is String -> jsonArray.put(item)
                            is Number -> jsonArray.put(item)
                            is Boolean -> jsonArray.put(item)
                            is Map<*, *> -> jsonArray.put(mapToJsonObject(item))
                            else -> item?.let { jsonArray.put(it.toString()) }
                        }
                    }
                    jsonObject.put(key, jsonArray)
                }
                is Map<*, *> -> jsonObject.put(key, mapToJsonObject(v))
                else -> jsonObject.put(key, v.toString())
            }
        }
        return jsonObject
    }

    companion object {

        /**
         * Create a new network packet with auto-generated ID
         *
         * @param type Packet type (e.g., "cconnect.ping", "cconnect.battery")
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
                type = PacketType.normalize(ffiPacket.packetType),
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
            // Direct access to body JSONObject — no serialize/reparse cycle
            val body = jsonObjectToMap(legacyPacket.body)
            val type = PacketType.normalize(legacyPacket.type)

            val payloadSize = if (legacyPacket.hasPayload())
                legacyPacket.payloadSize.takeIf { it > 0 } else null

            val transferInfo = if (legacyPacket.hasPayloadTransferInfo())
                jsonObjectToMap(legacyPacket.payloadTransferInfo) else emptyMap()

            return NetworkPacket(
                id = System.currentTimeMillis(),
                type = type,
                body = body,
                payloadSize = payloadSize,
                payloadTransferInfo = transferInfo
            )
        }

        /**
         * Recursively convert JSONObject to Map<String, Any>
         *
         * Handles nested JSONObject and JSONArray to ensure all values
         * are serializable types (Map, List, String, Number, Boolean)
         * Note: JSON null values are skipped as Map<String, Any> doesn't allow nulls
         */
        private fun jsonObjectToMap(jsonObject: org.json.JSONObject): Map<String, Any> {
            val map = mutableMapOf<String, Any>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                // Skip null values since Map<String, Any> doesn't allow them
                if (value != org.json.JSONObject.NULL) {
                    map[key] = jsonValueToSerializable(value)
                }
            }
            return map
        }

        /**
         * Convert a JSON value to a serializable type
         */
        private fun jsonValueToSerializable(value: Any): Any = when (value) {
            is org.json.JSONObject -> jsonObjectToMap(value)
            is org.json.JSONArray -> jsonArrayToList(value)
            else -> value // String, Number, Boolean pass through
        }

        /**
         * Recursively convert JSONArray to List<Any>
         */
        private fun jsonArrayToList(jsonArray: org.json.JSONArray): List<Any> {
            val list = mutableListOf<Any>()
            for (i in 0 until jsonArray.length()) {
                val value = jsonArray.get(i)
                // Skip null values
                if (value != org.json.JSONObject.NULL) {
                    list.add(jsonValueToSerializable(value))
                }
            }
            return list
        }

        /**
         * Alias for fromLegacyPacket for consistency with some plugin code
         */
        @JvmStatic
        fun fromLegacy(legacyPacket: org.cosmic.cosmicconnect.NetworkPacket): NetworkPacket {
            return fromLegacyPacket(legacyPacket)
        }

        /**
         * Deserialize wire JSON directly to Core.NetworkPacket (no legacy intermediate).
         *
         * Parses the same JSON format produced by [serializeKotlin] and legacy serialize().
         * Use this in the transport layer for zero-copy incoming packet parsing.
         *
         * @param s JSON string (with or without trailing newline)
         * @return Deserialized NetworkPacket
         */
        fun deserializeKotlin(s: String): NetworkPacket {
            val jo = org.json.JSONObject(s)
            val type = PacketType.normalize(jo.getString("type"))
            val body = jsonObjectToMap(jo.getJSONObject("body"))
            val payloadSize = if (jo.has("payloadSize"))
                jo.getLong("payloadSize").takeIf { it > 0 } else null
            val transferInfo = if (jo.has("payloadTransferInfo"))
                jsonObjectToMap(jo.getJSONObject("payloadTransferInfo")) else emptyMap()
            return NetworkPacket(
                id = jo.getLong("id"),
                type = type,
                body = body,
                payloadSize = payloadSize,
                payloadTransferInfo = transferInfo
            )
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
                // Use recursive conversion to handle nested objects and arrays
                jsonObjectToMap(jsonObject)
            } catch (e: Exception) {
                val preview = if (json.length > 100) json.take(100) + "..." else json
                throw CosmicConnectException("Failed to parse JSON body: $preview", e)
            }
        }
    }

    /**
     * Serialize packet to JSON string for wire transmission (Kotlin-native, no FFI).
     *
     * Produces the same QJson-compatible format as legacy NetworkPacket.serialize():
     * `{"id":123,"type":"...","body":{...}}\n`
     *
     * Unlike [serialize] (which goes through FFI and drops payloadTransferInfo),
     * this method includes payloadTransferInfo in the output.
     */
    fun serializeKotlin(): String {
        val jo = org.json.JSONObject()
        jo.put("id", id)
        jo.put("type", type)
        jo.put("body", mapToJsonObject(body))
        if (hasPayload) {
            jo.put("payloadSize", payloadSize)
            if (payloadTransferInfo.isNotEmpty()) {
                jo.put("payloadTransferInfo", mapToJsonObject(payloadTransferInfo))
            }
        }
        // QJSon does not escape slashes, but Java JSONObject does. Converting to QJson format.
        return jo.toString().replace("\\/", "/") + "\n"
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
    private const val CC_PREFIX = "cconnect."
    private const val KDE_PREFIX = "kdeconnect."

    const val IDENTITY = CC_PREFIX + "identity"
    const val PAIR = CC_PREFIX + "pair"
    const val ENCRYPTED = CC_PREFIX + "encrypted"

    // Plugin packet types
    const val PING = CC_PREFIX + "ping"
    const val BATTERY = CC_PREFIX + "battery"
    const val BATTERY_REQUEST = CC_PREFIX + "battery.request"
    const val SHARE_REQUEST = CC_PREFIX + "share.request"
    const val SHARE_REQUEST_UPDATE = CC_PREFIX + "share.request.update"
    const val CLIPBOARD = CC_PREFIX + "clipboard"
    const val CLIPBOARD_CONNECT = CC_PREFIX + "clipboard.connect"
    const val MPRIS = CC_PREFIX + "mpris"
    const val MPRIS_REQUEST = CC_PREFIX + "mpris.request"
    const val NOTIFICATION = CC_PREFIX + "notification"
    const val NOTIFICATION_REQUEST = CC_PREFIX + "notification.request"
    const val RUNCOMMAND = CC_PREFIX + "runcommand"
    const val RUNCOMMAND_REQUEST = CC_PREFIX + "runcommand.request"
    const val TELEPHONY = CC_PREFIX + "telephony"
    const val SMS_REQUEST = CC_PREFIX + "sms.request"
    const val SMS_MESSAGES = CC_PREFIX + "sms.messages"
    const val MOUSEPAD_REQUEST = CC_PREFIX + "mousepad.request"
    const val MOUSEPAD_ECHO = CC_PREFIX + "mousepad.echo"
    const val MOUSEPAD_KEYBOARDSTATE = CC_PREFIX + "mousepad.keyboardstate"
    const val PRESENTER = CC_PREFIX + "presenter"
    const val SFTP = CC_PREFIX + "sftp"
    const val SFTP_REQUEST = CC_PREFIX + "sftp.request"
    const val FINDMYPHONE_REQUEST = CC_PREFIX + "findmyphone.request"

    /**
     * Normalizes a packet type string by converting legacy namespaces to cconnect
     */
    fun normalize(type: String): String {
        return if (type.startsWith(KDE_PREFIX)) {
            type.replace(KDE_PREFIX, CC_PREFIX)
        } else {
            type
        }
    }
}
