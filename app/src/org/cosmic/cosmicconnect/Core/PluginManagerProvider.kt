package org.cosmic.cosmicconnect.Core

import android.util.Log

/**
 * PluginManagerProvider - Singleton provider for shared PluginManager instance
 *
 * The PluginManager from FFI is designed to be shared across all devices and all plugins.
 * This singleton ensures we only create one instance and reuse it throughout the app.
 *
 * ## Why Singleton?
 *
 * - **Resource Efficiency**: FFI PluginManager has overhead, create once
 * - **State Consistency**: All plugins share same state (battery, ping stats, etc.)
 * - **Cross-Device Support**: One manager handles multiple connected devices
 * - **Thread Safety**: Synchronized access to shared instance
 *
 * ## Usage
 *
 * ```kotlin
 * // In any plugin
 * val manager = PluginManagerProvider.getInstance()
 * manager.updateBattery(batteryState)
 * ```
 *
 * ## Lifecycle
 *
 * - **Created**: On first access (lazy initialization)
 * - **Destroyed**: When app terminates (cleanup in Application.onTerminate)
 * - **Reset**: Can be reset for testing or error recovery
 */
object PluginManagerProvider {

    private const val TAG = "PluginManagerProvider"

    @Volatile
    private var instance: PluginManager? = null

    /**
     * Get shared PluginManager instance
     *
     * Creates the instance on first access (lazy initialization).
     * Subsequent calls return the same instance.
     *
     * @return Shared PluginManager instance
     * @throws CosmicConnectException if initialization fails
     */
    @Synchronized
    fun getInstance(): PluginManager {
        if (instance == null) {
            try {
                Log.i(TAG, "Creating shared PluginManager instance")

                // Ensure CosmicConnectCore is initialized
                if (!CosmicConnectCore.isReady) {
                    CosmicConnectCore.initialize()
                }

                // Create plugin manager
                instance = PluginManager.create()

                Log.i(TAG, "✅ Shared PluginManager created")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create PluginManager", e)
                throw CosmicConnectException("Failed to create shared PluginManager: ${e.message}", e)
            }
        }

        return instance!!
    }

    /**
     * Check if PluginManager instance exists
     *
     * @return true if instance has been created
     */
    fun isInitialized(): Boolean {
        return instance != null
    }

    /**
     * Reset the PluginManager instance
     *
     * Shuts down the current instance and clears the reference.
     * Next call to getInstance() will create a new instance.
     *
     * **Warning**: This will affect all plugins using the shared instance!
     * Only use for testing or error recovery.
     */
    @Synchronized
    fun reset() {
        if (instance != null) {
            try {
                Log.i(TAG, "Resetting shared PluginManager")
                instance?.shutdownAll()
                instance = null
                Log.i(TAG, "✅ PluginManager reset")
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting PluginManager", e)
                instance = null // Clear anyway
            }
        }
    }

    /**
     * Shutdown the PluginManager and cleanup resources
     *
     * Should be called when the app is terminating.
     * After calling this, getInstance() will create a new instance.
     */
    @Synchronized
    fun shutdown() {
        if (instance != null) {
            try {
                Log.i(TAG, "Shutting down shared PluginManager")
                instance?.shutdownAll()
                Log.i(TAG, "✅ PluginManager shut down")
            } catch (e: Exception) {
                Log.e(TAG, "Error shutting down PluginManager", e)
            } finally {
                instance = null
            }
        }
    }

    /**
     * Get PluginManager if it exists, or null
     *
     * Unlike getInstance(), this doesn't create a new instance.
     * Useful for checking state without triggering initialization.
     *
     * @return PluginManager instance or null
     */
    fun getInstanceOrNull(): PluginManager? {
        return instance
    }
}
