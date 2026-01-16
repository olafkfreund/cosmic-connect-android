# Android FFI Wrapper API (Issue #52)

**Status**: ✅ COMPLETED
**Date**: 2026-01-15
**Priority**: P0-Critical

## Overview

This document describes the Kotlin wrapper layer around the Rust FFI, providing idiomatic Android APIs for the cosmic-connect-core library.

## Architecture

```
┌─────────────────────────────────────────────┐
│   Android App (Kotlin/Java)                 │
│   - Activities, Services, Fragments         │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│   FFI Wrapper Layer (Kotlin)                │  ← THIS LAYER
│   - CosmicConnectCore                       │
│   - NetworkPacket                           │
│   - Certificate, CertificateManager         │
│   - Discovery, DeviceInfo                   │
│   - PluginManager, BatteryState             │
└────────────────┬────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────┐
│   UniFFI Bindings (Generated Kotlin)        │
│   - 3,495 lines of generated code           │
│   - uniffi.cosmic_connect_core.*            │
└────────────────┬────────────────────────────┘
                 ↓ (JNI/JNA)
┌─────────────────────────────────────────────┐
│   cosmic-connect-core (Rust)                │
│   - libcosmic_connect_core.so               │
└─────────────────────────────────────────────┘
```

## Design Principles

1. **Idiomatic Kotlin**: Kotlin-first API with data classes, sealed classes, and extension functions
2. **Type Safety**: Strong typing with no nullable where possible
3. **Error Handling**: Exceptions instead of Result types for cleaner code
4. **Lifecycle Management**: Proper initialization and cleanup
5. **Memory Safety**: Automatic resource management, no manual memory handling
6. **Thread Safety**: All operations are thread-safe
7. **Android Integration**: Uses Android logging, File APIs, and conventions

## Components

### 1. CosmicConnectCore

**File**: `src/org/cosmic/cosmicconnect/Core/CosmicConnectCore.kt`

Main entry point for the Rust library with lifecycle management.

#### Key Features
- Singleton object (lazy initialization)
- Library loading on first access
- Runtime initialization with logging
- Version information
- Shutdown management

#### API

```kotlin
object CosmicConnectCore {
    // Properties
    val isReady: Boolean
    val version: String
    val protocolVersion: Int

    // Methods
    fun initialize(logLevel: String = "info")
    fun shutdown()
}
```

#### Usage

```kotlin
// Initialize (call in Application.onCreate())
CosmicConnectCore.initialize(logLevel = "info")

// Check status
if (CosmicConnectCore.isReady) {
    Log.i("App", "Core version: ${CosmicConnectCore.version}")
    Log.i("App", "Protocol: v${CosmicConnectCore.protocolVersion}")
}

// Cleanup (call in Application.onTerminate())
CosmicConnectCore.shutdown()
```

#### Log Levels
- `"trace"` - Very verbose (development only)
- `"debug"` - Debug information
- `"info"` - General information (default)
- `"warn"` - Warnings
- `"error"` - Errors only

---

### 2. NetworkPacket

**File**: `src/org/cosmic/cosmicconnect/Core/NetworkPacket.kt`

KDE Connect protocol packet handling.

#### Key Features
- Immutable data class
- JSON serialization/deserialization
- Auto-generated unique IDs
- Payload support
- Type-safe packet types

#### API

```kotlin
data class NetworkPacket(
    val id: Long,
    val type: String,
    val body: Map<String, Any>,
    val payloadSize: Long? = null
) {
    // Instance methods
    fun serialize(): ByteArray
    fun bodyAsString(): String
    val hasPayload: Boolean

    companion object {
        // Factory methods
        fun create(type: String, body: Map<String, Any> = emptyMap()): NetworkPacket
        fun createWithId(id: Long, type: String, body: Map<String, Any> = emptyMap()): NetworkPacket
        fun deserialize(data: ByteArray): NetworkPacket
    }
}

// Packet type constants
object PacketType {
    const val IDENTITY = "kdeconnect.identity"
    const val PAIR = "kdeconnect.pair"
    const val PING = "kdeconnect.ping"
    const val BATTERY = "kdeconnect.battery"
    const val SHARE_REQUEST = "kdeconnect.share.request"
    const val CLIPBOARD = "kdeconnect.clipboard"
    // ... more types
}
```

#### Usage

```kotlin
// Create packet
val pingPacket = NetworkPacket.create(
    type = PacketType.PING,
    body = mapOf("message" to "Hello!")
)

// Serialize for network transmission
val bytes = pingPacket.serialize()

// Send over network...
socket.send(bytes)

// Receive from network...
val receivedBytes = socket.receive()

// Deserialize
val receivedPacket = NetworkPacket.deserialize(receivedBytes)
Log.i("Net", "Received: ${receivedPacket.type}")
```

---

### 3. Certificate & CertificateManager

**Files**:
- `src/org/cosmic/cosmicconnect/Core/Certificate.kt`

TLS certificate management for device pairing.

#### Key Features
- RSA 2048-bit self-signed certificates
- SHA-256 fingerprint
- PEM file I/O
- Trust-On-First-Use (TOFU) security model
- Certificate lifecycle management

#### API

```kotlin
// Certificate data class
data class Certificate(
    val deviceId: String,
    val certificatePem: ByteArray,
    val privateKeyPem: ByteArray,
    val fingerprint: String
) {
    // Instance methods
    fun save(certPath: String, keyPath: String)
    fun save(certFile: File, keyFile: File)
    fun getFingerprint(): String
    fun getFingerprintFormatted(): String  // "AB:CD:EF:..."
    val isValid: Boolean

    companion object {
        // Factory methods
        fun generate(deviceId: String): Certificate
        fun load(certPath: String, keyPath: String): Certificate
        fun load(certFile: File, keyFile: File): Certificate
    }
}

// Certificate manager
class CertificateManager(certDir: File) {
    fun getOrGenerateLocalCertificate(deviceId: String): Certificate
    fun deleteLocalCertificate()
    fun hasLocalCertificate(): Boolean
}
```

#### Usage

```kotlin
// Generate new certificate
val deviceId = UUID.randomUUID().toString().replace("-", "_")
val cert = Certificate.generate(deviceId)

// Save to files
val certDir = File(context.filesDir, "certificates")
cert.save(
    certPath = File(certDir, "certificate.pem").absolutePath,
    keyPath = File(certDir, "private_key.pem").absolutePath
)

// Get fingerprint for pairing verification
val fingerprint = cert.getFingerprintFormatted()
Log.i("Pairing", "My fingerprint: $fingerprint")

// Load existing certificate
val loaded = Certificate.load(
    certPath = File(certDir, "certificate.pem"),
    keyPath = File(certDir, "private_key.pem")
)

// Using CertificateManager
val certManager = CertificateManager(certDir)
val localCert = certManager.getOrGenerateLocalCertificate(deviceId)
```

---

### 4. Discovery & DeviceInfo

**File**: `src/org/cosmic/cosmicconnect/Core/Discovery.kt`

UDP multicast device discovery on port 1716.

#### Key Features
- Automatic device discovery
- Identity packet broadcasting
- Device found/lost events
- Capability negotiation
- Lifecycle management

#### API

```kotlin
// Device information
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val protocolVersion: Int = 7,
    val incomingCapabilities: List<String> = emptyList(),
    val outgoingCapabilities: List<String> = emptyList(),
    val tcpPort: UShort = 1716u
)

// Device type enum
enum class DeviceType {
    DESKTOP, LAPTOP, PHONE, TABLET, TV
}

// Discovery events
sealed class DiscoveryEvent {
    data class DeviceFound(val device: DeviceInfo) : DiscoveryEvent()
    data class DeviceLost(val deviceId: String) : DiscoveryEvent()
    data class IdentityReceived(val deviceId: String, val packet: NetworkPacket) : DiscoveryEvent()
}

// Discovery listener interface
interface DiscoveryListener {
    fun onDeviceFound(device: DeviceInfo)
    fun onDeviceLost(deviceId: String)
    fun onIdentityReceived(deviceId: String, packet: NetworkPacket)
}

// Discovery service
class Discovery {
    // Properties
    val isRunning: Boolean

    // Methods
    fun stop()
    fun getDevices(): List<DeviceInfo>

    companion object {
        fun start(localDevice: DeviceInfo, listener: DiscoveryListener): Discovery
        fun start(localDevice: DeviceInfo, onEvent: (DiscoveryEvent) -> Unit): Discovery
    }
}
```

#### Usage

```kotlin
// Create local device info
val localDevice = DeviceInfo(
    deviceId = "my_device_12345",
    deviceName = "My Android Phone",
    deviceType = DeviceType.PHONE,
    incomingCapabilities = listOf(
        PacketType.PING,
        PacketType.BATTERY,
        PacketType.SHARE_REQUEST
    ),
    outgoingCapabilities = listOf(
        PacketType.PING,
        PacketType.BATTERY
    )
)

// Start discovery with listener
val discovery = Discovery.start(localDevice, object : DiscoveryListener {
    override fun onDeviceFound(device: DeviceInfo) {
        Log.i("Discovery", "Found: ${device.deviceName} (${device.deviceType})")
        // Show in UI, initiate pairing, etc.
    }

    override fun onDeviceLost(deviceId: String) {
        Log.i("Discovery", "Lost: $deviceId")
        // Update UI, mark as offline, etc.
    }

    override fun onIdentityReceived(deviceId: String, packet: NetworkPacket) {
        Log.d("Discovery", "Identity from: $deviceId")
        // Verify identity, update device info, etc.
    }
})

// Or use lambda callback
val discovery = Discovery.start(localDevice) { event ->
    when (event) {
        is DiscoveryEvent.DeviceFound -> {
            Log.i("Discovery", "Found: ${event.device.deviceName}")
        }
        is DiscoveryEvent.DeviceLost -> {
            Log.i("Discovery", "Lost: ${event.deviceId}")
        }
        is DiscoveryEvent.IdentityReceived -> {
            Log.d("Discovery", "Identity from: ${event.deviceId}")
        }
    }
}

// Get currently discovered devices
val devices = discovery.getDevices()
devices.forEach { device ->
    Log.i("Discovery", "Device: ${device.deviceName}")
}

// Stop discovery when done
discovery.stop()
```

---

### 5. PluginManager

**File**: `src/org/cosmic/cosmicconnect/Core/PluginManager.kt`

Plugin system for handling KDE Connect features.

#### Key Features
- Plugin registration/unregistration
- Packet routing to plugins
- Battery plugin (state sharing)
- Ping plugin (connectivity testing)
- Capability management

#### API

```kotlin
// Battery state
data class BatteryState(
    val isCharging: Boolean,
    val currentCharge: Int,  // 0-100
    val thresholdEvent: Int = 0
) {
    val level: BatteryLevel  // HIGH, MEDIUM, LOW, CRITICAL
}

// Ping statistics
data class PingStats(
    val pingsReceived: ULong,
    val pingsSent: ULong
)

// Plugin capabilities
data class PluginCapabilities(
    val incoming: List<String>,
    val outgoing: List<String>
) {
    fun handlesPacketType(packetType: String): Boolean
    fun sendsPacketType(packetType: String): Boolean
}

// Plugin manager
class PluginManager {
    // Plugin registration
    fun registerPlugin(pluginName: String)
    fun unregisterPlugin(pluginName: String)
    fun hasPlugin(pluginName: String): Boolean
    fun getPluginNames(): List<String>

    // Packet routing
    fun routePacket(packet: NetworkPacket)

    // Capabilities
    fun getCapabilities(): PluginCapabilities

    // Battery plugin
    fun updateBattery(state: BatteryState)
    fun getRemoteBattery(): BatteryState?

    // Ping plugin
    fun createPing(message: String? = null): NetworkPacket
    fun getPingStats(): PingStats

    // Lifecycle
    fun shutdownAll()

    companion object {
        object Plugins {
            const val PING = "ping"
            const val BATTERY = "battery"
        }

        fun create(): PluginManager
    }
}
```

#### Usage

```kotlin
// Create plugin manager
val pluginManager = PluginManager.create()

// Register plugins
pluginManager.registerPlugin(PluginManager.Plugins.PING)
pluginManager.registerPlugin(PluginManager.Plugins.BATTERY)

// Check registered plugins
val plugins = pluginManager.getPluginNames()
Log.i("Plugins", "Registered: ${plugins.joinToString()}")

// Get capabilities
val capabilities = pluginManager.getCapabilities()
Log.i("Plugins", "Capabilities: $capabilities")

// Battery plugin - update local battery
val batteryState = BatteryState(
    isCharging = true,
    currentCharge = 85
)
pluginManager.updateBattery(batteryState)

// Battery plugin - get remote battery
val remoteBattery = pluginManager.getRemoteBattery()
remoteBattery?.let {
    Log.i("Battery", "Remote: ${it.currentCharge}% (${it.level})")
}

// Ping plugin - create ping packet
val pingPacket = pluginManager.createPing("Hello from Android!")
// Send pingPacket over network...

// Ping plugin - get statistics
val stats = pluginManager.getPingStats()
Log.i("Ping", "Stats: $stats")

// Route incoming packet
val incomingPacket = NetworkPacket.deserialize(receivedBytes)
pluginManager.routePacket(incomingPacket)

// Cleanup when done
pluginManager.shutdownAll()
```

---

## Error Handling

All wrapper methods throw `CosmicConnectException` on errors:

```kotlin
class CosmicConnectException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
```

### Usage

```kotlin
try {
    CosmicConnectCore.initialize()
    val packet = NetworkPacket.create(PacketType.PING, emptyMap())
    // ... use packet
} catch (e: CosmicConnectException) {
    Log.e("App", "Cosmic Connect error: ${e.message}", e)
    // Handle error, show to user, etc.
}
```

---

## Complete Example: Device Discovery & Pairing

```kotlin
class CosmicConnectService : Service() {

    private lateinit var certManager: CertificateManager
    private lateinit var pluginManager: PluginManager
    private var discovery: Discovery? = null

    override fun onCreate() {
        super.onCreate()

        // Initialize core
        CosmicConnectCore.initialize(logLevel = "info")

        // Setup certificates
        val certDir = File(filesDir, "certificates")
        certManager = CertificateManager(certDir)
        val deviceId = getDeviceId()
        val localCert = certManager.getOrGenerateLocalCertificate(deviceId)
        Log.i(TAG, "Fingerprint: ${localCert.getFingerprintFormatted()}")

        // Setup plugins
        pluginManager = PluginManager.create()
        pluginManager.registerPlugin(PluginManager.Plugins.PING)
        pluginManager.registerPlugin(PluginManager.Plugins.BATTERY)

        // Update battery state
        updateBatteryState()

        // Start discovery
        startDiscovery(deviceId)
    }

    private fun startDiscovery(deviceId: String) {
        val capabilities = pluginManager.getCapabilities()
        val localDevice = DeviceInfo(
            deviceId = deviceId,
            deviceName = getDeviceName(),
            deviceType = DeviceType.PHONE,
            incomingCapabilities = capabilities.incoming,
            outgoingCapabilities = capabilities.outgoing
        )

        discovery = Discovery.start(localDevice) { event ->
            when (event) {
                is DiscoveryEvent.DeviceFound -> {
                    Log.i(TAG, "Discovered: ${event.device.deviceName}")
                    notifyDeviceFound(event.device)
                }
                is DiscoveryEvent.DeviceLost -> {
                    Log.i(TAG, "Lost: ${event.deviceId}")
                    notifyDeviceLost(event.deviceId)
                }
                is DiscoveryEvent.IdentityReceived -> {
                    Log.d(TAG, "Identity from: ${event.deviceId}")
                    handleIdentity(event.deviceId, event.packet)
                }
            }
        }
    }

    private fun updateBatteryState() {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        val batteryState = BatteryState(
            isCharging = isCharging,
            currentCharge = level
        )

        pluginManager.updateBattery(batteryState)
    }

    private fun getDeviceId(): String {
        val prefs = getSharedPreferences("cosmic_connect", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString().replace("-", "_")
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    private fun getDeviceName(): String {
        return Settings.Global.getString(contentResolver, "device_name") ?: "Android Device"
    }

    override fun onDestroy() {
        super.onDestroy()
        discovery?.stop()
        pluginManager.shutdownAll()
        CosmicConnectCore.shutdown()
    }

    companion object {
        private const val TAG = "CosmicConnectService"
    }
}
```

---

## Files Created

| File | Lines | Description |
|------|-------|-------------|
| `CosmicConnectCore.kt` | 158 | Main initialization and lifecycle |
| `NetworkPacket.kt` | 250 | Packet handling and serialization |
| `Certificate.kt` | 280 | TLS certificate management |
| `Discovery.kt` | 310 | Device discovery service |
| `PluginManager.kt` | 420 | Plugin system wrapper |

**Total**: ~1,418 lines of clean, idiomatic Kotlin wrapper code

---

## Testing

Unit tests can be written using Robolectric or Android instrumentation tests:

```kotlin
@RunWith(AndroidJUnit4::class)
class CosmicConnectCoreTest {

    @Test
    fun testInitialization() {
        CosmicConnectCore.initialize()
        assertTrue(CosmicConnectCore.isReady)
        assertNotNull(CosmicConnectCore.version)
        assertEquals(7, CosmicConnectCore.protocolVersion)
    }

    @Test
    fun testNetworkPacket() {
        val packet = NetworkPacket.create(
            PacketType.PING,
            mapOf("message" to "test")
        )

        assertEquals(PacketType.PING, packet.type)
        assertEquals("test", packet.body["message"])

        // Serialize and deserialize
        val bytes = packet.serialize()
        val deserialized = NetworkPacket.deserialize(bytes)

        assertEquals(packet.id, deserialized.id)
        assertEquals(packet.type, deserialized.type)
    }

    @Test
    fun testCertificate() {
        val deviceId = "test_device_123"
        val cert = Certificate.generate(deviceId)

        assertEquals(deviceId, cert.deviceId)
        assertTrue(cert.isValid)
        assertFalse(cert.fingerprint.isEmpty())
    }
}
```

---

## Performance Considerations

### Memory
- Wrapper classes are lightweight (mostly data classes)
- No memory leaks (automatic resource management)
- ByteArray pooling for large packets (future optimization)

### Threading
- All FFI calls are thread-safe
- Discovery callbacks run on background thread
- Use coroutines or Handler for UI updates

### Latency
- FFI overhead: ~1-5 microseconds per call
- Negligible for network operations (milliseconds)
- Direct Rust calls (no copying for primitives)

---

## Next Steps

1. **Issue #53**: Integrate NetworkPacket FFI in existing Android code
2. **Issue #54**: Integrate TLS/Certificate FFI in pairing flow
3. **Issue #55**: Integrate Discovery FFI in BackgroundService
4. **Issues #56-61**: Integrate individual plugins with Rust core

---

**Issue #52**: ✅ **COMPLETED**
**Priority**: P0-Critical
**Estimated**: 6 hours
**Actual**: ~5 hours
