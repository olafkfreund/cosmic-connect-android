package org.cosmic.cosmicconnect.Core

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.Socket

/**
 * Payload - Represents file/stream data attached to a NetworkPacket
 *
 * Used for file transfers and large data transmission in KDE Connect protocol.
 * Payloads are sent separately from the packet metadata.
 *
 * ## Important Note
 * Do not close the InputStream directly - call Payload.close() instead.
 * This is because of [Android bug 37018094](https://issuetracker.google.com/issues/37018094)
 * with SSLSocket InputStreams.
 *
 * ## Usage
 * ```kotlin
 * // Create payload from byte array
 * val payload = Payload(byteArrayOf(1, 2, 3))
 *
 * // Create payload from input stream
 * val payload = Payload(inputStream, size)
 *
 * // Create payload from socket (for SSL)
 * val payload = Payload(sslSocket, size)
 *
 * // Use payload
 * payload.inputStream?.use { stream ->
 *     // Read data
 * }
 *
 * // Always close when done
 * payload.close()
 * ```
 */
class Payload {

    /**
     * Input stream for reading payload data
     *
     * NOTE: Do not close the InputStream directly - call Payload.close() instead.
     * This is because of Android bug with SSLSocket InputStreams.
     */
    val inputStream: InputStream?

    /**
     * Socket associated with this payload (if any)
     *
     * Used to properly close SSL sockets and avoid Android bug 37018094.
     */
    private val inputSocket: Socket?

    /**
     * Size of payload data in bytes
     */
    val payloadSize: Long

    /**
     * Create empty placeholder payload with size
     *
     * Used when receiving payload - actual data comes later over TCP.
     *
     * @param payloadSize Size of payload in bytes
     */
    constructor(payloadSize: Long) : this(null, payloadSize)

    /**
     * Create payload from byte array
     *
     * @param data Payload data
     */
    constructor(data: ByteArray) : this(ByteArrayInputStream(data), data.size.toLong())

    /**
     * Create payload from input stream
     *
     * NOTE: Do not use this with SSLSocket InputStreams - use Payload(Socket, long) instead
     * because of Android bug 37018094.
     *
     * @param inputStream Input stream to read from
     * @param payloadSize Size of data in bytes
     */
    constructor(inputStream: InputStream?, payloadSize: Long) {
        this.inputSocket = null
        this.inputStream = inputStream
        this.payloadSize = payloadSize
    }

    /**
     * Create payload from socket
     *
     * Use this constructor for SSLSocket payloads to avoid Android bug 37018094.
     * The socket will be closed when close() is called.
     *
     * @param inputSocket Socket to read from (typically SSLSocket)
     * @param payloadSize Size of data in bytes
     */
    constructor(inputSocket: Socket, payloadSize: Long) {
        this.inputSocket = inputSocket
        this.inputStream = inputSocket.getInputStream()
        this.payloadSize = payloadSize
    }

    /**
     * Close the payload and release resources
     *
     * Always call this method instead of closing the InputStream directly.
     * This properly handles SSLSocket closure to avoid Android bugs.
     */
    fun close() {
        // Close input stream
        try {
            inputStream?.close()
        } catch (ignored: IOException) {
        }

        // Close socket if present (important for SSL sockets)
        try {
            inputSocket?.close()
        } catch (ignored: IOException) {
        }
    }

    override fun toString(): String {
        return "Payload(size=$payloadSize bytes, hasStream=${inputStream != null}, hasSocket=${inputSocket != null})"
    }
}

/**
 * Extension to attach payload to NetworkPacket
 *
 * Creates a new NetworkPacket with payload size set.
 */
fun NetworkPacket.withPayload(payload: Payload): NetworkPacket {
    return NetworkPacket(
        id = id,
        type = type,
        body = body,
        payloadSize = payload.payloadSize
    )
}

/**
 * Check if packet has payload based on payloadSize field
 */
fun NetworkPacket.hasPayloadInfo(): Boolean {
    return payloadSize != null && payloadSize > 0
}
