package org.cosmic.cosmicconnect.Plugins.SharePlugin

import org.cosmic.cosmicconnect.Core.CosmicConnectException
import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.*

/**
 * SharePacketsFFI - FFI wrapper for Share plugin packet creation
 *
 * Provides idiomatic Kotlin API for creating share-related packets
 * using the Rust FFI core library.
 *
 * ## Share Plugin Protocol (KDE Connect v7)
 *
 * The Share plugin supports three types of content:
 * 1. **Files**: Requires payload transfer via TCP
 * 2. **Text**: Plain text content (no payload)
 * 3. **URLs**: Web links (no payload)
 *
 * ## Usage
 *
 * ### File Sharing
 * ```kotlin
 * // Create file share packet
 * val packet = SharePacketsFFI.createFileShare(
 *     filename = "photo.jpg",
 *     size = 1048576L, // 1 MB
 *     creationTime = System.currentTimeMillis(),
 *     lastModified = System.currentTimeMillis()
 * )
 *
 * // Send packet to device
 * device.sendPacket(packet.toLegacyPacket())
 *
 * // Then initiate payload transfer (see PayloadTransferFFI)
 * ```
 *
 * ### Text Sharing
 * ```kotlin
 * val packet = SharePacketsFFI.createTextShare("Hello from Android!")
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * ### URL Sharing
 * ```kotlin
 * val packet = SharePacketsFFI.createUrlShare("https://example.com")
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * ### Multi-File Transfer
 * ```kotlin
 * // Send update packet before transferring multiple files
 * val updatePacket = SharePacketsFFI.createMultiFileUpdate(
 *     numberOfFiles = 5,
 *     totalPayloadSize = 10485760L // 10 MB total
 * )
 * device.sendPacket(updatePacket.toLegacyPacket())
 *
 * // Then send individual file packets...
 * ```
 */
object SharePacketsFFI {

    /**
     * Create a file share packet
     *
     * Creates a packet for sharing a file with metadata. The file payload
     * must be sent separately using PayloadTransferFFI.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "kdeconnect.share.request",
     *   "body": {
     *     "filename": "photo.jpg",
     *     "creationTime": 1640000000000,
     *     "lastModified": 1640000000000
     *   },
     *   "payloadSize": 1048576
     * }
     * ```
     *
     * @param filename Name of the file being shared (e.g., "photo.jpg")
     * @param size Size of the file in bytes
     * @param creationTime Optional file creation timestamp (milliseconds since epoch)
     * @param lastModified Optional last modified timestamp (milliseconds since epoch)
     * @return NetworkPacket ready to send to remote device
     * @throws CosmicConnectException if packet creation fails
     * @throws IllegalArgumentException if filename is empty or size is negative
     */
    fun createFileShare(
        filename: String,
        size: Long,
        creationTime: Long? = null,
        lastModified: Long? = null
    ): NetworkPacket {
        // Validation
        require(filename.isNotBlank()) { "Filename cannot be empty" }
        require(size >= 0) { "File size cannot be negative" }

        try {
            val ffiPacket = createFileSharePacket(
                filename = filename,
                size = size,
                creationTime = creationTime,
                lastModified = lastModified
            )
            return NetworkPacket.fromFfiPacket(ffiPacket)
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to create file share packet: ${e.message}", e)
        }
    }

    /**
     * Create a text share packet
     *
     * Creates a packet for sharing plain text content. No payload transfer needed.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "kdeconnect.share.request",
     *   "body": {
     *     "text": "Some text to share"
     *   }
     * }
     * ```
     *
     * @param text Text content to share
     * @return NetworkPacket ready to send to remote device
     * @throws CosmicConnectException if packet creation fails
     * @throws IllegalArgumentException if text is empty
     */
    fun createTextShare(text: String): NetworkPacket {
        // Validation
        require(text.isNotBlank()) { "Text cannot be empty" }

        try {
            val ffiPacket = createTextSharePacket(text)
            return NetworkPacket.fromFfiPacket(ffiPacket)
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to create text share packet: ${e.message}", e)
        }
    }

    /**
     * Create a URL share packet
     *
     * Creates a packet for sharing a URL. No payload transfer needed.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "kdeconnect.share.request",
     *   "body": {
     *     "url": "https://example.com"
     *   }
     * }
     * ```
     *
     * @param url URL to share (must be valid HTTP/HTTPS URL)
     * @return NetworkPacket ready to send to remote device
     * @throws CosmicConnectException if packet creation fails
     * @throws IllegalArgumentException if URL is invalid
     */
    fun createUrlShare(url: String): NetworkPacket {
        // Validation
        require(url.isNotBlank()) { "URL cannot be empty" }
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "URL must start with http:// or https://"
        }

        try {
            val ffiPacket = createUrlSharePacket(url)
            return NetworkPacket.fromFfiPacket(ffiPacket)
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to create URL share packet: ${e.message}", e)
        }
    }

    /**
     * Create a multi-file update packet
     *
     * Creates a packet indicating multiple files will be transferred.
     * This packet should be sent BEFORE transferring multiple files to notify
     * the remote device about the total transfer size and file count.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "kdeconnect.share.request.update",
     *   "body": {
     *     "numberOfFiles": 5,
     *     "totalPayloadSize": 10485760
     *   }
     * }
     * ```
     *
     * @param numberOfFiles Total number of files to be transferred
     * @param totalPayloadSize Combined size of all files in bytes
     * @return NetworkPacket ready to send to remote device
     * @throws CosmicConnectException if packet creation fails
     * @throws IllegalArgumentException if count or size is invalid
     */
    fun createMultiFileUpdate(
        numberOfFiles: Int,
        totalPayloadSize: Long
    ): NetworkPacket {
        // Validation
        require(numberOfFiles > 0) { "Number of files must be positive" }
        require(totalPayloadSize >= 0) { "Total payload size cannot be negative" }

        try {
            val ffiPacket = createMultifileUpdatePacket(numberOfFiles, totalPayloadSize)
            return NetworkPacket.fromFfiPacket(ffiPacket)
        } catch (e: Exception) {
            throw CosmicConnectException("Failed to create multi-file update packet: ${e.message}", e)
        }
    }
}

/**
 * Extension functions for NetworkPacket to check share content type
 */

/**
 * Check if this packet is a file share request
 */
val NetworkPacket.isFileShare: Boolean
    get() = type == "kdeconnect.share.request" && body.containsKey("filename")

/**
 * Check if this packet is a text share request
 */
val NetworkPacket.isTextShare: Boolean
    get() = type == "kdeconnect.share.request" && body.containsKey("text")

/**
 * Check if this packet is a URL share request
 */
val NetworkPacket.isUrlShare: Boolean
    get() = type == "kdeconnect.share.request" && body.containsKey("url")

/**
 * Check if this packet is a multi-file update
 */
val NetworkPacket.isMultiFileUpdate: Boolean
    get() = type == "kdeconnect.share.request.update"

/**
 * Extract filename from file share packet
 *
 * @return Filename or null if not a file share packet
 */
val NetworkPacket.filename: String?
    get() = if (isFileShare) body["filename"] as? String else null

/**
 * Extract text from text share packet
 *
 * @return Text content or null if not a text share packet
 */
val NetworkPacket.sharedText: String?
    get() = if (isTextShare) body["text"] as? String else null

/**
 * Extract URL from URL share packet
 *
 * @return URL or null if not a URL share packet
 */
val NetworkPacket.sharedUrl: String?
    get() = if (isUrlShare) body["url"] as? String else null

/**
 * Extract number of files from multi-file update packet
 *
 * @return Number of files or null if not a multi-file update packet
 */
val NetworkPacket.numberOfFiles: Int?
    get() = if (isMultiFileUpdate) {
        when (val value = body["numberOfFiles"]) {
            is Int -> value
            is Number -> value.toInt()
            else -> null
        }
    } else null

/**
 * Extract total payload size from multi-file update packet
 *
 * @return Total size in bytes or null if not a multi-file update packet
 */
val NetworkPacket.totalPayloadSize: Long?
    get() = if (isMultiFileUpdate) {
        when (val value = body["totalPayloadSize"]) {
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }
    } else null

/**
 * Extract file creation time from file share packet
 *
 * @return Creation timestamp (milliseconds since epoch) or null if not available
 */
val NetworkPacket.creationTime: Long?
    get() = if (isFileShare) {
        when (val value = body["creationTime"]) {
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }
    } else null

/**
 * Extract file last modified time from file share packet
 *
 * @return Last modified timestamp (milliseconds since epoch) or null if not available
 */
val NetworkPacket.lastModified: Long?
    get() = if (isFileShare) {
        when (val value = body["lastModified"]) {
            is Long -> value
            is Number -> value.toLong()
            else -> null
        }
    } else null
