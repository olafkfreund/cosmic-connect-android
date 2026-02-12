package org.cosmicext.connect.Plugins.SharePlugin

import android.util.Log
import org.cosmicext.connect.Core.CosmicExtConnectException
import org.cosmicext.connect.Core.NetworkPacket
import uniffi.cosmic_ext_connect_core.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PayloadTransferFFI - FFI wrapper for file payload transfers
 *
 * Handles asynchronous file downloads from remote devices with progress tracking,
 * cancellation support, and error handling.
 *
 * ## Architecture
 *
 * ```
 * Android App
 *     ↓
 * PayloadTransferFFI (this class)
 *     ↓ (implements PayloadCallback)
 * Rust FFI (start_payload_download)
 *     ↓ (spawns async task)
 * TCP Connection → File Download
 *     ↓ (progress callbacks)
 * Android File I/O
 * ```
 *
 * ## Usage
 *
 * ### Simple Download
 * ```kotlin
 * val transfer = PayloadTransferFFI(
 *     deviceHost = "192.168.1.100",
 *     port = 1739,
 *     expectedSize = 1048576L, // 1 MB
 *     outputFile = File(downloadsDir, "photo.jpg")
 * )
 *
 * transfer.start(
 *     onProgress = { transferred, total ->
 *         val percent = (transferred * 100 / total).toInt()
 *         Log.d(TAG, "Progress: $percent%")
 *     },
 *     onComplete = {
 *         Log.d(TAG, "Download complete!")
 *     },
 *     onError = { error ->
 *         Log.e(TAG, "Download failed: $error")
 *     }
 * )
 *
 * // Cancel if needed
 * transfer.cancel()
 * ```
 *
 * ### From Share Packet
 * ```kotlin
 * val packet: NetworkPacket = ... // cconnect.share.request
 * val deviceHost = "192.168.1.100" // From device connection info
 *
 * val transfer = PayloadTransferFFI.fromPacket(
 *     packet = packet,
 *     deviceHost = deviceHost,
 *     outputFile = outputFile
 * )
 *
 * transfer?.start(onProgress, onComplete, onError)
 * ```
 */
class PayloadTransferFFI(
    private val deviceHost: String,
    private val port: Int,
    private val expectedSize: Long,
    private val outputFile: File
) {

    companion object {
        private const val TAG = "PayloadTransferFFI"

        /**
         * Default port for COSMIC Connect payload transfers
         */
        const val DEFAULT_PAYLOAD_PORT = 1739

        /**
         * Create PayloadTransferFFI from a share packet
         *
         * Extracts payload transfer information from the packet and creates
         * a transfer instance ready to start.
         *
         * @param packet Share request packet (must have payload)
         * @param deviceHost IP address of the remote device
         * @param outputFile Destination file for download
         * @return PayloadTransferFFI instance or null if packet has no payload
         * @throws IllegalArgumentException if packet is not a valid share request
         */
        fun fromPacket(
            packet: NetworkPacket,
            deviceHost: String,
            outputFile: File
        ): PayloadTransferFFI? {
            require(packet.type == "cconnect.share.request") {
                "Packet must be a share request"
            }

            // Check if packet has payload
            val payloadSize = packet.payloadSize
            if (payloadSize == null || payloadSize <= 0) {
                Log.w(TAG, "Packet has no payload")
                return null
            }

            // Extract port from payloadTransferInfo (if present)
            val port = when (val info = packet.body["payloadTransferInfo"]) {
                is Map<*, *> -> (info["port"] as? Number)?.toInt() ?: DEFAULT_PAYLOAD_PORT
                else -> DEFAULT_PAYLOAD_PORT
            }

            return PayloadTransferFFI(
                deviceHost = deviceHost,
                port = port,
                expectedSize = payloadSize,
                outputFile = outputFile
            )
        }
    }

    // Transfer state
    private var handle: PayloadTransferHandle? = null
    private val isStarted = AtomicBoolean(false)
    private val isCancelled = AtomicBoolean(false)
    private var outputStream: FileOutputStream? = null

    // Callbacks
    private var progressCallback: ((Long, Long) -> Unit)? = null
    private var completeCallback: (() -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    /**
     * Start the payload download
     *
     * Initiates the async file download from the remote device.
     * Progress, completion, and errors are reported via callbacks.
     *
     * ## Threading
     * - Callbacks are invoked on background thread (Rust async runtime)
     * - UI updates must use Handler.post() or runOnUiThread()
     *
     * @param onProgress Called periodically with (bytesTransferred, totalBytes)
     * @param onComplete Called when transfer completes successfully
     * @param onError Called when transfer fails with error message
     * @throws CosmicExtConnectException if transfer cannot be started
     * @throws IllegalStateException if transfer already started
     */
    fun start(
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
        onComplete: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        // Check if already started
        if (!isStarted.compareAndSet(false, true)) {
            throw IllegalStateException("Transfer already started")
        }

        // Store callbacks
        this.progressCallback = onProgress
        this.completeCallback = onComplete
        this.errorCallback = onError

        try {
            // Ensure output directory exists
            outputFile.parentFile?.mkdirs()

            // Open output stream
            outputStream = FileOutputStream(outputFile)

            // Create FFI callback
            val callback = object : PayloadCallback {
                override fun onProgress(bytesTransferred: ULong, totalBytes: ULong) {
                    if (isCancelled.get()) return

                    try {
                        progressCallback?.invoke(bytesTransferred.toLong(), totalBytes.toLong())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in progress callback", e)
                    }
                }

                override fun onComplete() {
                    if (isCancelled.get()) return

                    try {
                        // Close output stream
                        outputStream?.flush()
                        outputStream?.close()
                        outputStream = null

                        // Verify file size
                        val actualSize = outputFile.length()
                        if (actualSize != expectedSize) {
                            Log.w(TAG, "File size mismatch: expected=$expectedSize, actual=$actualSize")
                        }

                        completeCallback?.invoke()
                        Log.i(TAG, "Transfer complete: ${outputFile.name} ($actualSize bytes)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in complete callback", e)
                        handleError("Completion handler failed: ${e.message}")
                    }
                }

                override fun onError(error: String) {
                    if (isCancelled.get()) return

                    handleError(error)
                }
            }

            // Start FFI download
            handle = startPayloadDownload(
                deviceHost = deviceHost,
                port = port.toUShort(),
                expectedSize = expectedSize,
                callback = callback
            )

            Log.i(TAG, "Started payload download: $deviceHost:$port -> ${outputFile.name} ($expectedSize bytes)")

        } catch (e: IOException) {
            handleError("Failed to open output file: ${e.message}")
            throw CosmicExtConnectException("Failed to start payload transfer: ${e.message}", e)
        } catch (e: Exception) {
            handleError("Failed to start transfer: ${e.message}")
            throw CosmicExtConnectException("Failed to start payload transfer: ${e.message}", e)
        }
    }

    /**
     * Cancel the payload download
     *
     * Stops the transfer and closes the output file. The file may be partially downloaded.
     *
     * @return true if cancellation was requested, false if already cancelled or not started
     */
    fun cancel(): Boolean {
        if (!isCancelled.compareAndSet(false, true)) {
            return false // Already cancelled
        }

        try {
            // Cancel FFI transfer
            handle?.cancel()

            // Close output stream
            outputStream?.close()
            outputStream = null

            // Delete incomplete file
            if (outputFile.exists()) {
                outputFile.delete()
            }

            Log.i(TAG, "Transfer cancelled: ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling transfer", e)
            return false
        }
    }

    /**
     * Check if transfer is cancelled
     */
    fun isCancelled(): Boolean = isCancelled.get()

    /**
     * Get transfer ID (for debugging)
     */
    fun getTransferId(): Long? = handle?.getId()?.toLong()

    /**
     * Handle transfer error
     */
    private fun handleError(error: String) {
        Log.e(TAG, "Transfer error: $error")

        try {
            // Close output stream
            outputStream?.close()
            outputStream = null

            // Delete incomplete file
            if (outputFile.exists()) {
                outputFile.delete()
            }

            // Notify error callback
            errorCallback?.invoke(error)
        } catch (e: Exception) {
            Log.e(TAG, "Error in error handler", e)
        }
    }
}

/**
 * Progress update throttler for UI updates
 *
 * Prevents excessive UI updates by throttling progress callbacks.
 *
 * ## Usage
 * ```kotlin
 * val throttler = ProgressThrottler(intervalMs = 500)
 *
 * transfer.start(
 *     onProgress = { transferred, total ->
 *         throttler.update(transferred, total) { t, tot ->
 *             runOnUiThread {
 *                 progressBar.progress = (t * 100 / tot).toInt()
 *             }
 *         }
 *     },
 *     ...
 * )
 * ```
 */
class ProgressThrottler(
    private val intervalMs: Long = 500
) {
    private var lastUpdateTime = 0L
    private var lastTransferred = 0L
    private var lastTotal = 0L

    /**
     * Update progress with throttling
     *
     * Only invokes the callback if enough time has elapsed since last update,
     * or if the transfer is complete.
     *
     * @param transferred Bytes transferred so far
     * @param total Total bytes to transfer
     * @param callback Callback to invoke (if throttle allows)
     */
    fun update(
        transferred: Long,
        total: Long,
        callback: (transferred: Long, total: Long) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val elapsed = now - lastUpdateTime

        // Always update if complete
        val isComplete = transferred >= total

        // Update if enough time elapsed or complete
        if (isComplete || elapsed >= intervalMs) {
            callback(transferred, total)
            lastUpdateTime = now
            lastTransferred = transferred
            lastTotal = total
        }
    }

    /**
     * Force an immediate update (bypasses throttle)
     */
    fun forceUpdate(
        transferred: Long,
        total: Long,
        callback: (transferred: Long, total: Long) -> Unit
    ) {
        callback(transferred, total)
        lastUpdateTime = System.currentTimeMillis()
        lastTransferred = transferred
        lastTotal = total
    }
}
