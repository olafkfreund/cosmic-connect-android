package org.cosmic.cosmicconnect.Core

import android.util.Log
import uniffi.cosmic_connect_core.*

/**
 * Cosmic Connect Core - Main entry point for Rust FFI layer
 *
 * This singleton manages the lifecycle of the cosmic-connect-core Rust library
 * and provides idiomatic Kotlin wrappers around the raw FFI bindings.
 *
 * ## Features
 * - Network packet handling (KDE Connect protocol v7)
 * - TLS/Certificate management (self-signed RSA 2048-bit)
 * - Plugin system (Battery, Ping, Share, etc.)
 * - Device discovery (UDP broadcast on port 1716)
 *
 * ## Architecture Support
 * - arm64-v8a (64-bit ARM) - Modern phones
 * - armeabi-v7a (32-bit ARM) - Older phones
 * - x86_64 (64-bit emulator)
 * - x86 (32-bit emulator)
 *
 * ## Usage
 * ```kotlin
 * // Initialize on app startup
 * CosmicConnectCore.initialize(logLevel = "info")
 *
 * // Check status
 * if (CosmicConnectCore.isReady) {
 *     val version = CosmicConnectCore.version
 *     Log.i("CosmicConnect", "Core version: $version")
 * }
 *
 * // Cleanup on shutdown
 * CosmicConnectCore.shutdown()
 * ```
 */
object CosmicConnectCore {

    private const val TAG = "CosmicConnectCore"

    private var libraryLoaded = false
    private var runtimeInitialized = false

    /**
     * Check if the native library is loaded and runtime is initialized
     */
    val isReady: Boolean
        get() = libraryLoaded && runtimeInitialized

    /**
     * Get the core library version
     */
    val version: String
        get() {
            checkInitialized()
            return getVersion()
        }

    /**
     * Get the KDE Connect protocol version (always 7)
     */
    val protocolVersion: Int
        get() {
            checkInitialized()
            return getProtocolVersion()
        }

    init {
        try {
            // Load the cosmic-connect-core native library
            // cargo-ndk places this in jniLibs/<abi>/libcosmic_connect_core.so
            System.loadLibrary("cosmic_connect_core")
            libraryLoaded = true
            Log.i(TAG, "✅ Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Failed to load native library", e)
            Log.e(TAG, "   Ensure cargo-ndk built the library for your device architecture")
            throw CosmicConnectException("Failed to load native library: ${e.message}", e)
        }
    }

    /**
     * Initialize the Rust runtime with logging
     *
     * This must be called before using any FFI functions.
     *
     * @param logLevel Log level: "trace", "debug", "info", "warn", "error"
     * @throws CosmicConnectException if initialization fails
     */
    @Synchronized
    fun initialize(logLevel: String = "info") {
        if (runtimeInitialized) {
            Log.w(TAG, "Already initialized, skipping")
            return
        }

        check(libraryLoaded) { "Native library not loaded" }

        try {
            // Initialize Rust logging
            uniffi.cosmic_connect_core.initialize(logLevel)
            runtimeInitialized = true

            Log.i(TAG, "✅ Runtime initialized (log level: $logLevel)")
            Log.i(TAG, "   Version: ${getVersion()}")
            Log.i(TAG, "   Protocol: v${getProtocolVersion()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize runtime", e)
            throw CosmicConnectException("Failed to initialize runtime: ${e.message}", e)
        }
    }

    /**
     * Shutdown and cleanup resources
     *
     * Call this when the app is being destroyed to cleanup resources.
     */
    @Synchronized
    fun shutdown() {
        if (!runtimeInitialized) {
            return
        }

        try {
            // Cleanup will be implemented when needed
            runtimeInitialized = false
            Log.i(TAG, "✅ Shutdown complete")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error during shutdown", e)
        }
    }

    /**
     * Check if the core is initialized, throw if not
     */
    private fun checkInitialized() {
        check(isReady) {
            "Cosmic Connect Core not initialized. Call CosmicConnectCore.initialize() first."
        }
    }
}

/**
 * Exception thrown by Cosmic Connect Core operations
 */
class CosmicConnectException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
