package org.cosmic.cosmicconnect.Core

import android.util.Log
import uniffi.cosmic_connect_core.*

/**
 * DeviceInfo - Information about a device (local or remote)
 *
 * Represents a COSMIC Connect device with its identity and capabilities.
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val protocolVersion: Int = 8,
    val incomingCapabilities: List<String> = emptyList(),
    val outgoingCapabilities: List<String> = emptyList(),
    val tcpPort: UShort = 1716u
) {

    companion object {
        /**
         * Convert FFI device info to Kotlin DeviceInfo
         */
        internal fun fromFfiDeviceInfo(ffi: FfiDeviceInfo): DeviceInfo {
            return DeviceInfo(
                deviceId = ffi.deviceId,
                deviceName = ffi.deviceName,
                deviceType = DeviceType.fromString(ffi.deviceType),
                protocolVersion = ffi.protocolVersion,
                incomingCapabilities = ffi.incomingCapabilities,
                outgoingCapabilities = ffi.outgoingCapabilities,
                tcpPort = ffi.tcpPort
            )
        }
    }

    /**
     * Convert to FFI device info for Rust calls
     */
    internal fun toFfiDeviceInfo(): FfiDeviceInfo {
        return FfiDeviceInfo(
            deviceId = deviceId,
            deviceName = deviceName,
            deviceType = deviceType.toString(),
            protocolVersion = protocolVersion,
            incomingCapabilities = incomingCapabilities,
            outgoingCapabilities = outgoingCapabilities,
            tcpPort = tcpPort
        )
    }

    override fun toString(): String {
        return "DeviceInfo(id='$deviceId', name='$deviceName', type=$deviceType, port=$tcpPort)"
    }
}

/**
 * DeviceType - Type of COSMIC Connect device
 */
enum class DeviceType {
    DESKTOP,
    LAPTOP,
    PHONE,
    TABLET,
    TV;

    companion object {
        /**
         * Parse device type from string
         */
        fun fromString(type: String): DeviceType {
            return when (type.lowercase()) {
                "desktop" -> DESKTOP
                "laptop" -> LAPTOP
                "phone" -> PHONE
                "tablet" -> TABLET
                "tv" -> TV
                else -> PHONE // Default to phone
            }
        }
    }

    override fun toString(): String {
        return name.lowercase()
    }
}

/**
 * DiscoveryEvent - Events from device discovery
 */
sealed class DiscoveryEvent {
    /**
     * A new device was discovered
     */
    data class DeviceFound(val device: DeviceInfo) : DiscoveryEvent()

    /**
     * A previously discovered device is no longer visible
     */
    data class DeviceLost(val deviceId: String) : DiscoveryEvent()

    /**
     * Identity packet received from a device
     */
    data class IdentityReceived(
        val deviceId: String,
        val packet: NetworkPacket
    ) : DiscoveryEvent()
}

/**
 * DiscoveryListener - Callback interface for discovery events
 */
interface DiscoveryListener {
    /**
     * Called when a device is discovered
     */
    fun onDeviceFound(device: DeviceInfo)

    /**
     * Called when a device is lost (no longer visible)
     */
    fun onDeviceLost(deviceId: String)

    /**
     * Called when an identity packet is received
     */
    fun onIdentityReceived(deviceId: String, packet: NetworkPacket)
}

/**
 * Discovery - Device discovery service wrapper
 *
 * Manages UDP broadcast discovery on port 1716 to find nearby COSMIC Connect devices.
 *
 * ## How it works
 * 1. Broadcasts identity packets via UDP multicast
 * 2. Listens for identity packets from other devices
 * 3. Notifies listeners when devices are found/lost
 *
 * ## Usage
 * ```kotlin
 * val localDevice = DeviceInfo(
 *     deviceId = "12345...",
 *     deviceName = "My Phone",
 *     deviceType = DeviceType.PHONE,
 *     incomingCapabilities = listOf("cconnect.battery", "cconnect.ping"),
 *     outgoingCapabilities = listOf("cconnect.battery", "cconnect.ping")
 * )
 *
 * val discovery = Discovery.start(localDevice) { event ->
 *     when (event) {
 *         is DiscoveryEvent.DeviceFound -> {
 *             println("Found: ${event.device.deviceName}")
 *         }
 *         is DiscoveryEvent.DeviceLost -> {
 *             println("Lost: ${event.deviceId}")
 *         }
 *         is DiscoveryEvent.IdentityReceived -> {
 *             println("Identity from: ${event.deviceId}")
 *         }
 *     }
 * }
 *
 * // Later: stop discovery
 * discovery.stop()
 * ```
 */
class Discovery private constructor(
    private val service: uniffi.cosmic_connect_core.DiscoveryService
) {

    companion object {
        private const val TAG = "Discovery"

        /**
         * Start device discovery
         *
         * @param localDevice Information about this device
         * @param listener Callback for discovery events
         * @return Discovery instance (call stop() to cleanup)
         * @throws CosmicConnectException if discovery fails to start
         */
        fun start(
            localDevice: DeviceInfo,
            listener: DiscoveryListener
        ): Discovery {
            return start(localDevice) { event ->
                when (event) {
                    is DiscoveryEvent.DeviceFound -> listener.onDeviceFound(event.device)
                    is DiscoveryEvent.DeviceLost -> listener.onDeviceLost(event.deviceId)
                    is DiscoveryEvent.IdentityReceived -> listener.onIdentityReceived(event.deviceId, event.packet)
                }
            }
        }

        /**
         * Start device discovery with lambda callback
         *
         * @param localDevice Information about this device
         * @param onEvent Callback for discovery events
         * @return Discovery instance
         * @throws CosmicConnectException if discovery fails to start
         */
        fun start(
            localDevice: DeviceInfo,
            onEvent: (DiscoveryEvent) -> Unit
        ): Discovery {
            try {
                Log.i(TAG, "Starting discovery for device: ${localDevice.deviceName}")

                // Create FFI callback
                val callback = object : uniffi.cosmic_connect_core.DiscoveryCallback {
                    override fun onDeviceFound(device: FfiDeviceInfo) {
                        Log.d(TAG, "Device found: ${device.deviceName}")
                        val deviceInfo = DeviceInfo.fromFfiDeviceInfo(device)
                        onEvent(DiscoveryEvent.DeviceFound(deviceInfo))
                    }

                    override fun onDeviceLost(deviceId: String) {
                        Log.d(TAG, "Device lost: $deviceId")
                        onEvent(DiscoveryEvent.DeviceLost(deviceId))
                    }

                    override fun onIdentityReceived(deviceId: String, packet: FfiPacket) {
                        Log.d(TAG, "Identity received from: $deviceId")
                        val networkPacket = NetworkPacket.fromFfiPacket(packet)
                        onEvent(DiscoveryEvent.IdentityReceived(deviceId, networkPacket))
                    }
                }

                // Start discovery via FFI
                val ffiDevice = localDevice.toFfiDeviceInfo()
                val service = startDiscovery(ffiDevice, callback)

                Log.i(TAG, "✅ Discovery started")
                return Discovery(service)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start discovery", e)
                throw CosmicConnectException("Failed to start discovery: ${e.message}", e)
            }
        }
    }

    /**
     * Stop discovery and cleanup resources
     *
     * @throws CosmicConnectException if stop fails
     */
    fun stop() {
        try {
            Log.i(TAG, "Stopping discovery")
            service.stop()
            Log.i(TAG, "✅ Discovery stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop discovery", e)
            throw CosmicConnectException("Failed to stop discovery: ${e.message}", e)
        }
    }

    /**
     * Get list of currently discovered devices
     *
     * @return List of discovered devices
     */
    fun getDevices(): List<DeviceInfo> {
        return try {
            service.getDevices().map { DeviceInfo.fromFfiDeviceInfo(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get devices", e)
            emptyList()
        }
    }

    /**
     * Check if discovery is currently running
     */
    val isRunning: Boolean
        get() = try {
            service.isRunning()
        } catch (e: Exception) {
            false
        }
}
