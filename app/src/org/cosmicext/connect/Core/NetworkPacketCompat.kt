package org.cosmicext.connect.Core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Compatibility extensions for NetworkPacket to support legacy API
 *
 * These extensions provide the same API as the old NetworkPacket implementation
 * while using the Rust FFI core underneath.
 */

// ========================================================================
// Legacy constructors
// ========================================================================

/**
 * Create a NetworkPacket with just a type (legacy constructor)
 *
 * This mimics the old `NetworkPacket(type)` constructor.
 */
fun NetworkPacket(type: String): NetworkPacket {
    return NetworkPacket.create(type, emptyMap())
}

// ========================================================================
// String getters/setters
// ========================================================================

/**
 * Get string value from body, or empty string if not found
 */
fun NetworkPacket.getString(key: String): String {
    return (body[key] as? String) ?: ""
}

/**
 * Get string value from body, or null if not found
 */
fun NetworkPacket.getStringOrNull(key: String): String? {
    return body[key] as? String
}

/**
 * Get string value from body, or default value if not found
 */
fun NetworkPacket.getString(key: String, defaultValue: String): String {
    return (body[key] as? String) ?: defaultValue
}

// ========================================================================
// Number conversion helpers (private)
// ========================================================================

/**
 * Generic number conversion with type safety
 */
private inline fun <reified T : Number> NetworkPacket.getNumber(
    key: String,
    crossinline convert: (Number) -> T,
    crossinline parseString: (String) -> T?
): T? {
    return when (val value = body[key]) {
        is Number -> convert(value)
        is String -> parseString(value)
        else -> null
    }
}

// ========================================================================
// Int getters/setters
// ========================================================================

/**
 * Get int value from body, or -1 if not found
 *
 * Note: Cannot distinguish between missing key and value=-1.
 * Use getIntOrNull() if this distinction matters.
 */
fun NetworkPacket.getInt(key: String): Int = getInt(key, -1)

/**
 * Get int value from body, or null if not found
 */
fun NetworkPacket.getIntOrNull(key: String): Int? {
    return getNumber(key, convert = { it.toInt() }, parseString = { it.toIntOrNull() })
}

/**
 * Get int value from body, or default value if not found
 */
fun NetworkPacket.getInt(key: String, defaultValue: Int): Int {
    return getIntOrNull(key) ?: defaultValue
}

// ========================================================================
// Long getters/setters
// ========================================================================

/**
 * Get long value from body, or -1 if not found
 *
 * Note: Cannot distinguish between missing key and value=-1.
 * Use getLongOrNull() if this distinction matters.
 */
fun NetworkPacket.getLong(key: String): Long = getLong(key, -1L)

/**
 * Get long value from body, or null if not found
 */
fun NetworkPacket.getLongOrNull(key: String): Long? {
    return getNumber(key, convert = { it.toLong() }, parseString = { it.toLongOrNull() })
}

/**
 * Get long value from body, or default value if not found
 */
fun NetworkPacket.getLong(key: String, defaultValue: Long): Long {
    return getLongOrNull(key) ?: defaultValue
}

// ========================================================================
// Boolean getters/setters
// ========================================================================

/**
 * Get boolean value from body, or false if not found
 */
fun NetworkPacket.getBoolean(key: String): Boolean {
    return when (val value = body[key]) {
        is Boolean -> value
        is String -> value.toBoolean()
        else -> false
    }
}

/**
 * Get boolean value from body, or default value if not found
 */
fun NetworkPacket.getBoolean(key: String, defaultValue: Boolean): Boolean {
    return when (val value = body[key]) {
        is Boolean -> value
        is String -> value.toBoolean()
        null -> defaultValue
        else -> defaultValue
    }
}

// ========================================================================
// Double getters/setters
// ========================================================================

/**
 * Get double value from body, or NaN if not found
 *
 * Note: Returns NaN for missing keys. Use getDoubleOrNull() if you need
 * to distinguish between missing and actual NaN values.
 */
fun NetworkPacket.getDouble(key: String): Double = getDouble(key, Double.NaN)

/**
 * Get double value from body, or null if not found
 */
fun NetworkPacket.getDoubleOrNull(key: String): Double? {
    return getNumber(key, convert = { it.toDouble() }, parseString = { it.toDoubleOrNull() })
}

/**
 * Get double value from body, or default value if not found
 */
fun NetworkPacket.getDouble(key: String, defaultValue: Double): Double {
    return getDoubleOrNull(key) ?: defaultValue
}

// ========================================================================
// JSONArray/JSONObject getters/setters
// ========================================================================

/**
 * Get JSONArray from body, or null if not found
 */
fun NetworkPacket.getJSONArray(key: String): JSONArray? {
    return when (val value = body[key]) {
        is String -> try { JSONArray(value) } catch (e: Exception) { null }
        else -> null
    }
}

/**
 * Get JSONObject from body, or null if not found
 */
fun NetworkPacket.getJSONObject(key: String): JSONObject? {
    return when (val value = body[key]) {
        is String -> try { JSONObject(value) } catch (e: Exception) { null }
        else -> null
    }
}

// ========================================================================
// Collection getters/setters
// ========================================================================

/**
 * Get string set from body, or null if not found
 */
fun NetworkPacket.getStringSet(key: String): Set<String>? {
    val jsonArray = getJSONArray(key) ?: return null
    val set = mutableSetOf<String>()
    for (i in 0 until jsonArray.length()) {
        try {
            set.add(jsonArray.getString(i))
        } catch (ignored: Exception) {
        }
    }
    return set
}

/**
 * Get string set from body, or default value if not found
 */
fun NetworkPacket.getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
    return if (has(key)) getStringSet(key) else defaultValue
}

/**
 * Get string list from body, or null if not found
 */
fun NetworkPacket.getStringList(key: String): List<String>? {
    val jsonArray = getJSONArray(key) ?: return null
    val list = mutableListOf<String>()
    for (i in 0 until jsonArray.length()) {
        try {
            list.add(jsonArray.getString(i))
        } catch (ignored: Exception) {
        }
    }
    return list
}

/**
 * Get string list from body, or default value if not found
 */
fun NetworkPacket.getStringList(key: String, defaultValue: List<String>?): List<String>? {
    return if (has(key)) getStringList(key) else defaultValue
}

// ========================================================================
// Has/contains checks
// ========================================================================

/**
 * Check if body contains key
 */
fun NetworkPacket.has(key: String): Boolean = key in body

/**
 * Check if body contains key (operator version)
 */
operator fun NetworkPacket.contains(key: String): Boolean = key in body

// ========================================================================
// Mutable body support via copy
// ========================================================================

/**
 * Create a mutable builder from this packet
 */
fun NetworkPacket.toBuilder(): NetworkPacketBuilder {
    return NetworkPacketBuilder(
        type = type,
        body = body.toMutableMap()
    )
}

/**
 * Builder for creating NetworkPackets with fluent API
 *
 * This builder defers FFI calls until build() for efficiency.
 * Only makes one FFI call regardless of number of fields set.
 *
 * Usage:
 * ```
 * val packet = NetworkPacket.create("cconnect.ping")
 *     .toBuilder()
 *     .set("message", "Hello")
 *     .set("count", 42)
 *     .build()
 * ```
 */
class NetworkPacketBuilder(
    val type: String,
    private val body: MutableMap<String, Any> = mutableMapOf()
) {

    fun set(key: String, value: String) = apply { body[key] = value }

    fun set(key: String, value: Int) = apply { body[key] = value }

    fun set(key: String, value: Long) = apply { body[key] = value }

    fun set(key: String, value: Boolean) = apply { body[key] = value }

    fun set(key: String, value: Double) = apply { body[key] = value }

    fun set(key: String, value: JSONArray) = apply { body[key] = value.toString() }

    fun set(key: String, value: JSONObject) = apply { body[key] = value.toString() }

    fun set(key: String, value: Set<String>) = apply {
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        body[key] = jsonArray.toString()
    }

    fun set(key: String, value: List<String>) = apply {
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        body[key] = jsonArray.toString()
    }

    /**
     * Build the NetworkPacket
     *
     * Makes a single FFI call with all accumulated data.
     */
    fun build(): NetworkPacket {
        return NetworkPacket.create(type, body.toMap())
    }
}

// ========================================================================
// Legacy compatibility
// ========================================================================

/**
 * Serialize to string (legacy format)
 *
 * Note: The FFI version uses ByteArray. This converts it to String for compatibility.
 */
fun NetworkPacket.serializeToString(): String {
    return serialize().toString(Charsets.UTF_8)
}

// ========================================================================
// Mutable Wrapper
// ========================================================================

/**
 * MutableNetworkPacket - Wrapper that provides mutable API over immutable NetworkPacket
 *
 * **DEPRECATED**: This class is inefficient and will be removed.
 *
 * **Problem**: Rebuilds entire packet via FFI on every mutation (O(N) FFI calls for N mutations).
 *
 * **Migration**: Use NetworkPacketBuilder instead:
 * ```kotlin
 * // OLD (inefficient)
 * val packet = MutableNetworkPacket("cconnect.battery")
 * packet["currentCharge"] = 85
 * packet["isCharging"] = true
 * device.sendPacket(packet.toNetworkPacket())
 *
 * // NEW (efficient - single FFI call)
 * val packet = NetworkPacket.create("cconnect.battery", mapOf(
 *     "currentCharge" to 85,
 *     "isCharging" to true
 * ))
 * device.sendPacket(packet)
 *
 * // OR use builder for incremental construction
 * val packet = NetworkPacketBuilder("cconnect.battery")
 *     .set("currentCharge", 85)
 *     .set("isCharging", true)
 *     .build()
 * device.sendPacket(packet)
 * ```
 */
@Deprecated(
    message = "Use NetworkPacketBuilder or NetworkPacket.create() with all data upfront. " +
            "MutableNetworkPacket makes O(N) FFI calls for N mutations.",
    replaceWith = ReplaceWith(
        "NetworkPacketBuilder(type).set(key, value).build()",
        "org.cosmicext.connect.Core.NetworkPacketBuilder"
    ),
    level = DeprecationLevel.WARNING
)
class MutableNetworkPacket(
    type: String,
    initialBody: Map<String, Any> = emptyMap()
) {
    private var currentPacket: NetworkPacket = NetworkPacket.create(type, initialBody)
    private val mutableBody = initialBody.toMutableMap()

    val type: String get() = currentPacket.type

    // Setters
    operator fun set(key: String, value: String) {
        mutableBody[key] = value
        rebuild()
    }

    operator fun set(key: String, value: Int) {
        mutableBody[key] = value
        rebuild()
    }

    operator fun set(key: String, value: Long) {
        mutableBody[key] = value
        rebuild()
    }

    operator fun set(key: String, value: Boolean) {
        mutableBody[key] = value
        rebuild()
    }

    operator fun set(key: String, value: Double) {
        mutableBody[key] = value
        rebuild()
    }

    // Getters (delegate to current packet)
    fun getString(key: String): String = currentPacket.getString(key)
    fun getString(key: String, defaultValue: String): String = currentPacket.getString(key, defaultValue)
    fun getStringOrNull(key: String): String? = currentPacket.getStringOrNull(key)

    fun getInt(key: String): Int = currentPacket.getInt(key)
    fun getInt(key: String, defaultValue: Int): Int = currentPacket.getInt(key, defaultValue)
    fun getIntOrNull(key: String): Int? = currentPacket.getIntOrNull(key)

    fun getLong(key: String): Long = currentPacket.getLong(key)
    fun getLong(key: String, defaultValue: Long): Long = currentPacket.getLong(key, defaultValue)

    fun getBoolean(key: String): Boolean = currentPacket.getBoolean(key)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean = currentPacket.getBoolean(key, defaultValue)

    fun getDouble(key: String): Double = currentPacket.getDouble(key)
    fun getDouble(key: String, defaultValue: Double): Double = currentPacket.getDouble(key, defaultValue)

    fun has(key: String): Boolean = currentPacket.has(key)
    operator fun contains(key: String): Boolean = has(key)

    // Convert to immutable NetworkPacket
    fun toNetworkPacket(): NetworkPacket = currentPacket

    private fun rebuild() {
        currentPacket = NetworkPacket.create(type, mutableBody.toMap())
    }

    override fun toString(): String = currentPacket.toString()
}

/**
 * Convert immutable NetworkPacket to mutable wrapper
 */
fun NetworkPacket.toMutable(): MutableNetworkPacket {
    return MutableNetworkPacket(type, body)
}

// ========================================================================
// TransferPacket convenience delegates
// ========================================================================

fun TransferPacket.getString(key: String): String = packet.getString(key)
fun TransferPacket.getString(key: String, defaultValue: String): String = packet.getString(key, defaultValue)
fun TransferPacket.getStringOrNull(key: String): String? = packet.getStringOrNull(key)
fun TransferPacket.getInt(key: String): Int = packet.getInt(key)
fun TransferPacket.getInt(key: String, defaultValue: Int): Int = packet.getInt(key, defaultValue)
fun TransferPacket.getIntOrNull(key: String): Int? = packet.getIntOrNull(key)
fun TransferPacket.getLong(key: String): Long = packet.getLong(key)
fun TransferPacket.getLong(key: String, defaultValue: Long): Long = packet.getLong(key, defaultValue)
fun TransferPacket.getBoolean(key: String): Boolean = packet.getBoolean(key)
fun TransferPacket.getBoolean(key: String, defaultValue: Boolean): Boolean = packet.getBoolean(key, defaultValue)
fun TransferPacket.getDouble(key: String): Double = packet.getDouble(key)
fun TransferPacket.getDouble(key: String, defaultValue: Double): Double = packet.getDouble(key, defaultValue)
fun TransferPacket.getStringList(key: String): List<String>? = packet.getStringList(key)
fun TransferPacket.getStringList(key: String, defaultValue: List<String>?): List<String>? = packet.getStringList(key, defaultValue)
fun TransferPacket.getStringSet(key: String): Set<String>? = packet.getStringSet(key)
fun TransferPacket.getStringSet(key: String, defaultValue: Set<String>?): Set<String>? = packet.getStringSet(key, defaultValue)
fun TransferPacket.getJSONArray(key: String): JSONArray? = packet.getJSONArray(key)
fun TransferPacket.getJSONObject(key: String): JSONObject? = packet.getJSONObject(key)
fun TransferPacket.has(key: String): Boolean = packet.has(key)
operator fun TransferPacket.contains(key: String): Boolean = packet.has(key)
