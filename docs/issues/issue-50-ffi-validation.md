# Issue #50: FFI Bindings Validation

**Status**: In Progress ⚙️
**Created**: 2026-01-15
**Priority**: P0-Critical
**Estimated Effort**: 1 day
**Issue**: [#50 - FFI Bindings Validation](https://github.com/olafkfreund/cosmic-connect-android/issues/50)

---

## Overview

Comprehensive validation of uniffi-rs bindings between Android (Kotlin/JNI) and cosmic-connect-core (Rust). This validation ensures that the FFI layer is working correctly, performs acceptably, and is ready for full integration.

## Prerequisites

**Completed Issues** ✅:
- Issue #44: cosmic-connect-core library created
- Issue #45: NetworkPacket extracted to Rust
- Issue #46: Discovery service extracted to Rust
- Issue #47: TLS/Certificate management extracted to Rust
- Issue #48: Plugin core logic extracted to Rust

---

## FFI Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│          Android App (Kotlin/Java)                    │
├──────────────────────────────────────────────────────┤
│  Core Package (Kotlin FFI Wrappers)                  │
│  └─ CosmicConnectCore.kt    - Initialization         │
│  └─ NetworkPacket.kt         - Packet handling       │
│  └─ Discovery.kt             - Device discovery      │
│  └─ Certificate.kt           - Certificate mgmt      │
│  └─ PluginManager.kt         - Plugin system         │
│  └─ SslContextFactory.kt     - TLS context           │
├──────────────────────────────────────────────────────┤
│  Generated Bindings (uniffi-rs)                      │
│  └─ bindings/kotlin/uniffi/cosmic_connect_core/      │
│     └─ cosmic_connect_core.kt (124 KB)               │
├──────────────────────────────────────────────────────┤
│  JNI Layer                                            │
│  └─ System.loadLibrary("cosmic_connect_core")        │
│  └─ jniLibs/<abi>/libcosmic_connect_core.so          │
├──────────────────────────────────────────────────────┤
│  cosmic-connect-core (Rust)                          │
│  └─ protocol/   - NetworkPacket, types               │
│  └─ network/    - Discovery, multicast               │
│  └─ crypto/     - TLS, certificates                  │
│  └─ plugins/    - Plugin system                      │
│  └─ ffi/        - FFI interface definitions           │
└──────────────────────────────────────────────────────┘
```

---

## Validation Test Plan

### Phase 1: Basic Connectivity ✅

#### Test 1.1: Native Library Loading
**Objective**: Verify the native library loads correctly on all architectures

**Test Steps**:
1. Check for `libcosmic_connect_core.so` in jniLibs/
2. Call `CosmicConnectCore` initialization
3. Verify `libraryLoaded` flag is true
4. Check for UnsatisfiedLinkError

**Expected Result**: Library loads without errors

**Architecture Coverage**:
- [ ] arm64-v8a (64-bit ARM - modern phones)
- [ ] armeabi-v7a (32-bit ARM - older phones)
- [ ] x86_64 (64-bit emulator)
- [ ] x86 (32-bit emulator)

**Status**: ⚠️ Pending - Need to verify library files exist

---

#### Test 1.2: Runtime Initialization
**Objective**: Verify Rust runtime initializes correctly

**Test Steps**:
1. Call `CosmicConnectCore.initialize(logLevel = "debug")`
2. Verify `runtimeInitialized` flag is true
3. Call `CosmicConnectCore.version` - should return version string
4. Call `CosmicConnectCore.protocolVersion` - should return 7

**Expected Result**:
- No exceptions thrown
- Version string returned (e.g., "0.1.0-alpha")
- Protocol version is 7

**Status**: ⚠️ Pending

---

### Phase 2: Android → Rust FFI Calls 🔄

#### Test 2.1: NetworkPacket Creation
**Objective**: Test packet creation via FFI

**Test Code**:
```kotlin
import org.cosmic.cosmicconnect.Core.NetworkPacket

fun testPacketCreation() {
    val packet = NetworkPacket.create(
        "kdeconnect.identity",
        mapOf(
            "deviceId" to "test-device-123",
            "deviceName" to "Test Device",
            "deviceType" to "phone",
            "protocolVersion" to 7
        )
    )

    assert(packet.type == "kdeconnect.identity")
    assert(packet.body["deviceId"] == "test-device-123")
}
```

**Expected Result**: Packet created successfully, fields accessible

**Status**: ⚠️ Pending

---

#### Test 2.2: NetworkPacket Serialization
**Objective**: Test packet serialization to bytes

**Test Code**:
```kotlin
fun testPacketSerialization() {
    val packet = NetworkPacket.create(
        "kdeconnect.pair",
        mapOf("pair" to true)
    )

    val bytes = packet.serialize()

    // Should be JSON + newline
    val str = bytes.decodeToString()
    assert(str.endsWith("\n"))
    assert(str.contains("\"type\":\"kdeconnect.pair\""))
    assert(str.contains("\"pair\":true"))
}
```

**Expected Result**: Serialized bytes match KDE Connect protocol format

**Status**: ⚠️ Pending

---

#### Test 2.3: NetworkPacket Deserialization
**Objective**: Test packet parsing from bytes

**Test Code**:
```kotlin
fun testPacketDeserialization() {
    val json = """
    {
        "type": "kdeconnect.ping",
        "id": 1234567890,
        "body": {
            "message": "Hello World"
        }
    }
    """.trimIndent() + "\n"

    val packet = NetworkPacket.deserialize(json.encodeToByteArray())

    assert(packet.type == "kdeconnect.ping")
    assert(packet.id == 1234567890L)
    assert(packet.body["message"] == "Hello World")
}
```

**Expected Result**: Packet parsed correctly from bytes

**Status**: ⚠️ Pending

---

#### Test 2.4: Certificate Generation
**Objective**: Test certificate generation via FFI

**Test Code**:
```kotlin
import org.cosmic.cosmicconnect.Core.Certificate

fun testCertificateGeneration() {
    val deviceId = "test-device-${System.currentTimeMillis()}"

    val certInfo = Certificate.generate(deviceId)

    assert(certInfo.deviceId == deviceId)
    assert(certInfo.certificate.isNotEmpty())
    assert(certInfo.privateKey.isNotEmpty())
    assert(certInfo.fingerprint.isNotEmpty())

    // Fingerprint format: XX:XX:XX:... (SHA-256)
    val parts = certInfo.fingerprint.split(":")
    assert(parts.size == 32) // SHA-256 = 32 bytes = 32 hex pairs
}
```

**Expected Result**: Certificate generated with valid format

**Status**: ⚠️ Pending

---

#### Test 2.5: Discovery Start/Stop
**Objective**: Test discovery lifecycle via FFI

**Test Code**:
```kotlin
import org.cosmic.cosmicconnect.Core.Discovery

fun testDiscoveryLifecycle() {
    val discovery = Discovery.create(
        deviceId = "test-device",
        deviceName = "Test Device",
        deviceType = "phone",
        port = 1716
    )

    // Start discovery
    discovery.start()

    // Should be running
    assert(discovery.isRunning())

    // Stop discovery
    discovery.stop()

    // Should be stopped
    assert(!discovery.isRunning())
}
```

**Expected Result**: Discovery starts and stops without errors

**Status**: ⚠️ Pending

---

### Phase 3: Rust → Android Callbacks 📞

#### Test 3.1: Discovery Device Found Callback
**Objective**: Test callback from Rust to Android when device is discovered

**Test Code**:
```kotlin
import org.cosmic.cosmicconnect.Core.Discovery
import org.cosmic.cosmicconnect.Core.DiscoveryCallback

fun testDiscoveryCallback() {
    val devicesFound = mutableListOf<String>()

    val callback = object : DiscoveryCallback {
        override fun onDeviceFound(deviceId: String, deviceInfo: Map<String, String>) {
            devicesFound.add(deviceId)
            Log.d("FFI-Test", "Device found: $deviceId")
        }

        override fun onDeviceLost(deviceId: String) {
            Log.d("FFI-Test", "Device lost: $deviceId")
        }
    }

    val discovery = Discovery.create(
        deviceId = "test-device",
        deviceName = "Test Device",
        deviceType = "phone",
        port = 1716,
        callback = callback
    )

    discovery.start()

    // Wait for discovery events
    Thread.sleep(5000)

    discovery.stop()

    // Should have found at least one device (if any available)
    Log.d("FFI-Test", "Devices found: ${devicesFound.size}")
}
```

**Expected Result**: Callback invoked when devices are discovered

**Status**: ⚠️ Pending

---

#### Test 3.2: Plugin Event Callbacks
**Objective**: Test plugin event callbacks from Rust to Android

**Test Code**:
```kotlin
import org.cosmic.cosmicconnect.Core.PluginManager
import org.cosmic.cosmicconnect.Core.PluginCallback

fun testPluginCallback() {
    val packetsReceived = mutableListOf<String>()

    val callback = object : PluginCallback {
        override fun onPacketReceived(
            pluginName: String,
            packetType: String,
            packet: NetworkPacket
        ) {
            packetsReceived.add(packetType)
            Log.d("FFI-Test", "Plugin $pluginName received $packetType")
        }
    }

    val pluginManager = PluginManager.create(callback)

    // Register plugins
    pluginManager.registerPlugin("battery")
    pluginManager.registerPlugin("ping")

    // Simulate incoming packet
    val packet = NetworkPacket.create(
        "kdeconnect.battery",
        mapOf(
            "currentCharge" to 85,
            "isCharging" to true
        )
    )

    pluginManager.handlePacket(packet)

    assert(packetsReceived.contains("kdeconnect.battery"))
}
```

**Expected Result**: Callback invoked when plugin receives packet

**Status**: ⚠️ Pending

---

### Phase 4: Performance Profiling 📊

#### Test 4.1: FFI Call Overhead
**Objective**: Measure overhead of FFI boundary crossing

**Test Code**:
```kotlin
fun benchmarkFFICalls() {
    val iterations = 10000

    // Benchmark 1: Packet creation
    val start1 = System.nanoTime()
    repeat(iterations) {
        NetworkPacket.create("kdeconnect.ping", mapOf("seq" to it))
    }
    val end1 = System.nanoTime()
    val avgCreate = (end1 - start1) / iterations / 1000.0 // microseconds

    // Benchmark 2: Packet serialization
    val packet = NetworkPacket.create("kdeconnect.ping", mapOf("msg" to "test"))
    val start2 = System.nanoTime()
    repeat(iterations) {
        packet.serialize()
    }
    val end2 = System.nanoTime()
    val avgSerialize = (end2 - start2) / iterations / 1000.0 // microseconds

    // Benchmark 3: Packet deserialization
    val bytes = packet.serialize()
    val start3 = System.nanoTime()
    repeat(iterations) {
        NetworkPacket.deserialize(bytes)
    }
    val end3 = System.nanoTime()
    val avgDeserialize = (end3 - start3) / iterations / 1000.0 // microseconds

    Log.d("FFI-Benchmark", "Packet creation: $avgCreate μs")
    Log.d("FFI-Benchmark", "Serialization: $avgSerialize μs")
    Log.d("FFI-Benchmark", "Deserialization: $avgDeserialize μs")
}
```

**Target Performance**:
- Packet creation: < 10 μs
- Serialization: < 50 μs
- Deserialization: < 100 μs

**Status**: ⚠️ Pending

---

#### Test 4.2: Memory Overhead
**Objective**: Measure memory usage of FFI calls

**Test Code**:
```kotlin
fun benchmarkMemoryUsage() {
    Runtime.getRuntime().gc()
    val initialMemory = Runtime.getRuntime().totalMemory() -
                       Runtime.getRuntime().freeMemory()

    // Create 1000 packets
    val packets = mutableListOf<NetworkPacket>()
    repeat(1000) {
        packets.add(NetworkPacket.create(
            "kdeconnect.ping",
            mapOf("seq" to it, "msg" to "Test message $it")
        ))
    }

    Runtime.getRuntime().gc()
    val finalMemory = Runtime.getRuntime().totalMemory() -
                     Runtime.getRuntime().freeMemory()

    val memoryPerPacket = (finalMemory - initialMemory) / 1000 / 1024.0 // KB

    Log.d("FFI-Benchmark", "Memory per packet: $memoryPerPacket KB")
}
```

**Target**: < 1 KB per packet

**Status**: ⚠️ Pending

---

#### Test 4.3: Callback Latency
**Objective**: Measure latency of Rust → Android callbacks

**Test Code**:
```kotlin
fun benchmarkCallbackLatency() {
    val latencies = mutableListOf<Long>()

    val callback = object : DiscoveryCallback {
        override fun onDeviceFound(deviceId: String, deviceInfo: Map<String, String>) {
            val latency = System.nanoTime() - callbackStartTime
            latencies.add(latency)
        }

        override fun onDeviceLost(deviceId: String) {}
    }

    // Trigger 100 callbacks
    repeat(100) {
        callbackStartTime = System.nanoTime()
        // Trigger callback from Rust
        simulateDiscoveryEvent()
    }

    val avgLatency = latencies.average() / 1000.0 // microseconds
    val maxLatency = latencies.maxOrNull()!! / 1000.0 // microseconds

    Log.d("FFI-Benchmark", "Average callback latency: $avgLatency μs")
    Log.d("FFI-Benchmark", "Max callback latency: $maxLatency μs")
}
```

**Target**: < 100 μs average, < 1 ms max

**Status**: ⚠️ Pending

---

### Phase 5: Integration Testing 🔗

#### Test 5.1: End-to-End Packet Flow
**Objective**: Test complete packet flow through FFI

**Scenario**: Create packet → Serialize → Send → Receive → Deserialize → Process

**Test Code**:
```kotlin
fun testEndToEndPacketFlow() {
    // 1. Create packet on Android
    val packet = NetworkPacket.create(
        "kdeconnect.share.request",
        mapOf(
            "filename" to "test.txt",
            "text" to "Hello from Android via FFI"
        )
    )

    // 2. Serialize via FFI
    val bytes = packet.serialize()
    assert(bytes.isNotEmpty())

    // 3. Simulate network transmission (loopback)
    val receivedBytes = bytes

    // 4. Deserialize via FFI
    val receivedPacket = NetworkPacket.deserialize(receivedBytes)

    // 5. Verify data integrity
    assert(receivedPacket.type == packet.type)
    assert(receivedPacket.body["filename"] == "test.txt")
    assert(receivedPacket.body["text"] == "Hello from Android via FFI")

    Log.d("FFI-Test", "✅ End-to-end packet flow successful")
}
```

**Expected Result**: Data transmitted correctly through FFI without corruption

**Status**: ⚠️ Pending

---

#### Test 5.2: Concurrent FFI Calls
**Objective**: Test thread safety of FFI layer

**Test Code**:
```kotlin
import kotlinx.coroutines.*

fun testConcurrentFFICalls() = runBlocking {
    val jobs = List(10) { threadId ->
        launch(Dispatchers.Default) {
            repeat(100) { iteration ->
                val packet = NetworkPacket.create(
                    "kdeconnect.ping",
                    mapOf(
                        "thread" to threadId,
                        "iteration" to iteration
                    )
                )

                val bytes = packet.serialize()
                val deserialized = NetworkPacket.deserialize(bytes)

                assert(deserialized.body["thread"] == threadId)
                assert(deserialized.body["iteration"] == iteration)
            }
        }
    }

    jobs.joinAll()
    Log.d("FFI-Test", "✅ Concurrent FFI calls successful (1000 total)")
}
```

**Expected Result**: No race conditions or data corruption

**Status**: ⚠️ Pending

---

## Validation Checklist

### FFI Components to Validate

- [ ] **CosmicConnectCore**: Initialization, version, protocol version
- [ ] **NetworkPacket**: Create, serialize, deserialize, validation
- [ ] **Discovery**: Start, stop, callbacks, device found/lost
- [ ] **Certificate**: Generate, load, save, fingerprint, validation
- [ ] **PluginManager**: Register, unregister, handle packet, callbacks
- [ ] **SslContextFactory**: Create context, TLS handshake (if applicable)

### Architecture Support

- [ ] **arm64-v8a** (64-bit ARM)
- [ ] **armeabi-v7a** (32-bit ARM)
- [ ] **x86_64** (64-bit emulator)
- [ ] **x86** (32-bit emulator)

### Performance Targets

- [ ] **Packet creation**: < 10 μs
- [ ] **Serialization**: < 50 μs
- [ ] **Deserialization**: < 100 μs
- [ ] **Callback latency**: < 100 μs avg, < 1 ms max
- [ ] **Memory per packet**: < 1 KB

### Error Handling

- [ ] **Library not found**: Proper error message
- [ ] **Invalid packet data**: Exception with details
- [ ] **Callback exceptions**: Handled gracefully
- [ ] **Memory leaks**: None detected

---

## Known Issues & Limitations

### Issue 1: Native Library Not Built Yet
**Status**: ⚠️ Blocking
**Description**: The `libcosmic_connect_core.so` files need to be built for Android architectures via cargo-ndk
**Impact**: Cannot test FFI until library is built
**Mitigation**: Issue #51 will set up cargo-ndk build integration

### Issue 2: Partial FFI Integration
**Status**: ⚠️ Known
**Description**: NetworkPacket FFI is partially integrated (docs/networkpacket-ffi-integration.md)
**Impact**: Some legacy code still uses mutable NetworkPacket
**Mitigation**: Issue #53 will complete NetworkPacket FFI integration

### Issue 3: Missing Test Infrastructure
**Status**: ⚠️ Known
**Description**: No automated FFI integration tests exist yet
**Impact**: Manual testing required
**Mitigation**: Create instrumented tests in androidTest/

---

## Next Steps

### Immediate (This Issue #50)

1. **Verify library files exist**:
   ```bash
   ls -la src/main/jniLibs/*/libcosmic_connect_core.so
   ```

2. **Run basic connectivity tests**:
   - Test library loading
   - Test runtime initialization
   - Test version retrieval

3. **Create minimal FFI test**:
   - Create `androidTest/java/org/cosmic/cosmicconnect/FFIValidationTest.kt`
   - Implement basic connectivity tests
   - Run on emulator or device

4. **Document findings**:
   - Update this document with test results
   - Note any issues discovered
   - Create follow-up issues if needed

### Follow-up Issues

- **Issue #51**: cargo-ndk build integration (if library doesn't exist)
- **Issue #52**: Android FFI wrapper layer improvements
- **Issue #53**: Complete NetworkPacket FFI integration
- **FFI Performance Optimization**: If benchmarks show issues

---

## Test Results

### Test Execution Date: TBD

**Environment**:
- Android Version: TBD
- Device/Emulator: TBD
- Build Configuration: TBD

**Results Summary**: TBD

---

## Appendix: FFI Files Reference

### Rust Core (cosmic-connect-core)

**Location**: `/home/olafkfreund/Source/GitHub/cosmic-connect-core`

```
cosmic-connect-core/
├── src/
│   ├── protocol/      # NetworkPacket, packet types
│   ├── network/       # Discovery, UDP multicast
│   ├── crypto/        # TLS, certificates
│   ├── plugins/       # Plugin system
│   └── ffi/           # FFI interface definitions
├── bindings/
│   └── kotlin/
│       └── uniffi/
│           └── cosmic_connect_core/
│               └── cosmic_connect_core.kt  (124 KB)
├── uniffi-bindgen.rs  # uniffi configuration
└── build.rs           # Build script
```

### Android App

**Location**: `/home/olafkfreund/Source/GitHub/cosmic-connect-android`

```
cosmic-connect-android/
├── src/org/cosmic/cosmicconnect/Core/
│   ├── CosmicConnectCore.kt       # FFI initialization
│   ├── NetworkPacket.kt           # Packet FFI wrapper
│   ├── Discovery.kt               # Discovery FFI wrapper
│   ├── Certificate.kt             # Certificate FFI wrapper
│   ├── PluginManager.kt           # Plugin manager FFI wrapper
│   └── SslContextFactory.kt       # TLS FFI wrapper
└── src/main/jniLibs/              # Native libraries (TBD)
    ├── arm64-v8a/
    ├── armeabi-v7a/
    ├── x86_64/
    └── x86/
```

---

**Document Version**: 1.0
**Last Updated**: 2026-01-15
**Status**: Test plan defined, execution pending
**Next Update**: After test execution
