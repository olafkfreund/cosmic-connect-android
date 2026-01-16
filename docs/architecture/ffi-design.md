# FFI Design Document - Rust ↔ Kotlin Bridge

> **Status**: Design Phase
> **Target**: Phase 0-2 implementation
> **Goal**: Define clean, safe, and efficient FFI boundaries between Rust core and Kotlin Android app

---

## 📋 Executive Summary

This document defines the Foreign Function Interface (FFI) architecture for `cosmic-connect-core` (Rust) and `cosmic-connect-android` (Kotlin). We use **uniffi-rs** to generate type-safe Kotlin bindings from Rust code, ensuring memory safety and ergonomic APIs across the language boundary.

### Key Principles

1. **Safety First**: No memory leaks, no undefined behavior
2. **Ergonomic APIs**: Natural Kotlin feel, not just Rust wrappers
3. **Performance**: Minimize FFI overhead
4. **Error Handling**: Graceful errors across boundaries
5. **Async Support**: Handle asynchronous operations correctly

---

## 🏗️ Architecture Overview

### Layer Separation

```
┌─────────────────────────────────────────┐
│     Android UI (Jetpack Compose)        │  ← Pure Kotlin
├─────────────────────────────────────────┤
│     ViewModels (MVVM)                   │  ← Pure Kotlin
├─────────────────────────────────────────┤
│     Repositories                        │  ← Pure Kotlin
├─────────────────────────────────────────┤
│     Kotlin FFI Wrappers                 │  ← Kotlin + FFI
│  (Lifecycle, Error Handling, Callbacks) │
├─────────────────────────────────────────┤
│     uniffi-generated Bindings           │  ← Generated
├─────────────────────────────────────────┤
│     Rust FFI Layer (src/ffi/)          │  ← Rust
├─────────────────────────────────────────┤
│     cosmic-connect-core (Protocol)      │  ← Pure Rust
│  ┌────────────────────────────────────┐ │
│  │ Plugins │ Crypto │ Network │ Proto │ │
│  └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Data Flow Example: Send Packet

```
1. Kotlin UI: button click
   ↓
2. ViewModel: handle action
   ↓
3. Repository: sendPacket(packet)
   ↓
4. Kotlin Wrapper: CosmicConnectManager.sendPacket()
   ↓ FFI boundary
5. uniffi binding: cosmicConnectCoreSendPacket()
   ↓
6. Rust FFI: send_packet()
   ↓
7. Rust Core: NetworkManager.send()
   ↓
8. Network: TCP/TLS transmission
```

---

## 🦀 uniffi-rs Architecture

### What is uniffi-rs?

[uniffi-rs](https://mozilla.github.io/uniffi-rs/) is Mozilla's tool for generating foreign language bindings from Rust. It:

- Defines interfaces in `.udl` files (Web IDL-based)
- Generates type-safe Kotlin (and Swift) bindings
- Handles memory management automatically
- Provides error handling across FFI
- Supports complex types (structs, enums, callbacks)

### Core Concepts

#### 1. Interface Definition (`.udl` files)

```
// src/ffi/core.udl
namespace cosmic_connect_core {
    NetworkPacket create_packet(string packet_type, i64 id, string body_json);
};

interface NetworkPacket {
    constructor(string packet_type, i64 id, string body_json);
    bytes serialize();
    string get_type();
    i64 get_id();
    string get_body_json();
};

[Error]
enum CosmicConnectError {
    "SerializationError",
    "NetworkError",
    "TlsError",
    "InvalidPacket",
};
```

#### 2. Rust Implementation

```rust
// src/ffi/mod.rs
use uniffi;

#[uniffi::export]
fn create_packet(packet_type: String, id: i64, body_json: String) -> Result<Arc<NetworkPacket>, CosmicConnectError> {
    let body: serde_json::Value = serde_json::from_str(&body_json)
        .map_err(|_| CosmicConnectError::SerializationError)?;

    Ok(Arc::new(NetworkPacket {
        id,
        packet_type,
        body,
    }))
}

#[derive(uniffi::Object)]
pub struct NetworkPacket {
    pub id: i64,
    pub packet_type: String,
    pub body: serde_json::Value,
}

#[uniffi::export]
impl NetworkPacket {
    #[uniffi::constructor]
    pub fn new(packet_type: String, id: i64, body_json: String) -> Result<Arc<Self>, CosmicConnectError> {
        create_packet(packet_type, id, body_json)
    }

    pub fn serialize(&self) -> Result<Vec<u8>, CosmicConnectError> {
        let json = serde_json::to_string(self)
            .map_err(|_| CosmicConnectError::SerializationError)?;
        let mut bytes = json.into_bytes();
        bytes.push(b'\n');  // CRITICAL: Protocol requires newline
        Ok(bytes)
    }

    pub fn get_type(&self) -> String {
        self.packet_type.clone()
    }

    pub fn get_id(&self) -> i64 {
        self.id
    }

    pub fn get_body_json(&self) -> String {
        serde_json::to_string(&self.body).unwrap_or_default()
    }
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum CosmicConnectError {
    #[error("Serialization error")]
    SerializationError,

    #[error("Network error: {msg}")]
    NetworkError { msg: String },

    #[error("TLS error: {msg}")]
    TlsError { msg: String },

    #[error("Invalid packet")]
    InvalidPacket,
}
```

#### 3. Generated Kotlin Code

```kotlin
// Generated by uniffi
class NetworkPacket internal constructor(pointer: Pointer) : FFIObject(pointer) {
    companion object {
        fun create(packetType: String, id: Long, bodyJson: String): NetworkPacket {
            return NetworkPacket(/* ... */)
        }
    }

    fun serialize(): ByteArray {
        return /* ... */
    }

    fun getType(): String {
        return /* ... */
    }

    fun getId(): Long {
        return /* ... */
    }

    fun getBodyJson(): String {
        return /* ... */
    }

    override fun destroy() {
        // Automatically calls Rust drop
    }
}

sealed class CosmicConnectError : Exception() {
    object SerializationError : CosmicConnectError()
    data class NetworkError(val msg: String) : CosmicConnectError()
    data class TlsError(val msg: String) : CosmicConnectError()
    object InvalidPacket : CosmicConnectError()
}
```

---

## 🔄 Data Type Mappings

### Primitive Types

| Rust | Kotlin | Notes |
|------|--------|-------|
| `i8`, `i16`, `i32` | `Byte`, `Short`, `Int` | Signed integers |
| `i64` | `Long` | 64-bit signed |
| `u8`, `u16`, `u32` | `UByte`, `UShort`, `UInt` | Unsigned integers |
| `u64` | `ULong` | 64-bit unsigned |
| `f32`, `f64` | `Float`, `Double` | Floating point |
| `bool` | `Boolean` | Boolean |
| `String` | `String` | UTF-8 strings |
| `Vec<u8>` | `ByteArray` | Byte arrays |

### Complex Types

#### Structs → Data Classes

```rust
// Rust
#[derive(uniffi::Record)]
pub struct Device {
    pub id: String,
    pub name: String,
    pub device_type: String,
    pub protocol_version: i32,
}
```

```kotlin
// Generated Kotlin
data class Device(
    val id: String,
    val name: String,
    val deviceType: String,
    val protocolVersion: Int
)
```

#### Enums → Sealed Classes

```rust
// Rust
#[derive(uniffi::Enum)]
pub enum DeviceType {
    Desktop,
    Laptop,
    Phone,
    Tablet,
}
```

```kotlin
// Generated Kotlin
sealed class DeviceType {
    object Desktop : DeviceType()
    object Laptop : DeviceType()
    object Phone : DeviceType()
    object Tablet : DeviceType()
}
```

#### Objects → Classes

```rust
// Rust (Arc<T> for shared ownership)
#[derive(uniffi::Object)]
pub struct DiscoveryService {
    // Internal state
}

#[uniffi::export]
impl DiscoveryService {
    #[uniffi::constructor]
    pub fn new(device_id: String) -> Arc<Self> {
        Arc::new(Self { /* ... */ })
    }

    pub fn start(&self) -> Result<(), CosmicConnectError> {
        // Implementation
    }
}
```

```kotlin
// Generated Kotlin
class DiscoveryService internal constructor(pointer: Pointer) : FFIObject(pointer) {
    companion object {
        fun create(deviceId: String): DiscoveryService {
            return DiscoveryService(/* ... */)
        }
    }

    fun start() {
        // Calls Rust via FFI
    }

    override fun destroy() {
        // Automatically calls Rust drop
    }
}
```

---

## ⚠️ Error Handling

### Rust → Kotlin Error Translation

#### Define Errors in Rust

```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum CosmicConnectError {
    #[error("Network error: {msg}")]
    NetworkError { msg: String },

    #[error("TLS handshake failed: {reason}")]
    TlsError { reason: String },

    #[error("Invalid packet: {details}")]
    InvalidPacket { details: String },

    #[error("Device not found: {device_id}")]
    DeviceNotFound { device_id: String },

    #[error("Permission denied: {action}")]
    PermissionDenied { action: String },
}
```

#### Use in Kotlin

```kotlin
try {
    val packet = NetworkPacket.create("kdeconnect.ping", 123, "{}")
    packet.serialize()
} catch (e: CosmicConnectError.SerializationError) {
    Log.e(TAG, "Failed to serialize packet", e)
} catch (e: CosmicConnectError.NetworkError) {
    Log.e(TAG, "Network error: ${e.msg}", e)
    showNetworkErrorDialog(e.msg)
} catch (e: CosmicConnectError) {
    Log.e(TAG, "Unknown error", e)
}
```

### Best Practices

1. **Specific Errors**: Use detailed error types, not generic strings
2. **Context**: Include relevant context in error messages
3. **Recovery**: Make errors recoverable when possible
4. **Logging**: Log errors on both sides of FFI boundary

---

## 🧵 Memory Management

### Ownership Model

#### Rust Owns Data

uniffi-rs uses `Arc<T>` (Atomic Reference Counting) for shared ownership:

```rust
#[derive(uniffi::Object)]
pub struct NetworkManager {
    // Internal state
}

#[uniffi::export]
impl NetworkManager {
    #[uniffi::constructor]
    pub fn new() -> Arc<Self> {
        Arc::new(Self { /* ... */ })
    }
}
```

- **Kotlin holds reference**: When Kotlin creates `NetworkManager`, it holds an `Arc` pointer
- **Rust manages memory**: When Kotlin object is GC'd, it calls `destroy()` which drops the `Arc`
- **Thread-safe**: `Arc` allows safe sharing across threads

#### Lifetime Rules

1. **Kotlin object lifetime = Rust Arc lifetime**
   - Kotlin GC triggers Rust `Arc::drop`
   - No manual memory management needed

2. **Callbacks extend lifetime**
   - Rust holds `Arc` to callback object
   - Callback keeps Kotlin object alive

3. **No dangling pointers**
   - uniffi ensures pointer validity
   - Rust panics if pointer is invalid

### Common Patterns

#### Pattern 1: Long-lived Services

```rust
// Rust: Service lives for app lifetime
#[derive(uniffi::Object)]
pub struct CosmicConnectService {
    runtime: tokio::runtime::Runtime,
    discovery: Arc<DiscoveryService>,
    network_manager: Arc<NetworkManager>,
}

#[uniffi::export]
impl CosmicConnectService {
    #[uniffi::constructor]
    pub fn new(device_id: String) -> Arc<Self> {
        Arc::new(Self {
            runtime: tokio::runtime::Runtime::new().unwrap(),
            discovery: Arc::new(DiscoveryService::new(device_id)),
            network_manager: Arc::new(NetworkManager::new()),
        })
    }

    pub fn start(&self) {
        // Start services
    }

    pub fn stop(&self) {
        // Stop services
    }
}
```

```kotlin
// Kotlin: Keep service as singleton
object CosmicConnectManager {
    private lateinit var service: CosmicConnectService

    fun initialize(context: Context) {
        service = CosmicConnectService.create(getDeviceId(context))
        service.start()
    }

    fun shutdown() {
        service.stop()
        service.destroy()  // Drop Rust side
    }
}
```

#### Pattern 2: Short-lived Objects

```rust
// Rust: Packet lives briefly
#[uniffi::export]
fn create_and_send_packet(device_id: String, packet_type: String) -> Result<(), CosmicConnectError> {
    let packet = NetworkPacket::new(packet_type, generate_id(), "{}".to_string())?;
    send_packet_to_device(&device_id, packet)?;
    // packet dropped here
    Ok(())
}
```

```kotlin
// Kotlin: No need to hold reference
CosmicConnectCore.createAndSendPacket(deviceId, "kdeconnect.ping")
// Rust handles packet lifecycle internally
```

---

## 🔐 Thread Safety

### Rust Side

All FFI objects must be `Send + Sync`:

```rust
#[derive(uniffi::Object)]
pub struct DiscoveryService {
    inner: Arc<Mutex<DiscoveryServiceInner>>,
}

// Safe to send across threads
unsafe impl Send for DiscoveryService {}
unsafe impl Sync for DiscoveryService {}

impl DiscoveryService {
    pub fn start(&self) {
        let mut inner = self.inner.lock().unwrap();
        inner.start_discovery();
    }
}
```

### Kotlin Side

```kotlin
class DiscoveryRepository(
    private val discoveryService: DiscoveryService
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startDiscovery() {
        scope.launch {
            try {
                // Safe to call from coroutine
                discoveryService.start()
            } catch (e: CosmicConnectError) {
                Log.e(TAG, "Discovery failed", e)
            }
        }
    }
}
```

### Best Practices

1. **Use Rust Mutexes**: `Arc<Mutex<T>>` for shared mutable state in Rust
2. **Kotlin Coroutines**: Use `Dispatchers.IO` for FFI calls (don't block Main thread)
3. **No shared mutable state across FFI**: Pass immutable data or use callbacks
4. **Document thread safety**: Clearly document which methods are thread-safe

---

## ⏱️ Async Handling

### Challenge: uniffi doesn't support async directly

uniffi-rs doesn't yet support Rust `async fn` across FFI. We have several strategies:

### Strategy 1: Callbacks (Recommended)

```rust
// Rust
#[uniffi::export(callback_interface)]
pub trait DiscoveryListener: Send + Sync {
    fn on_device_found(&self, device: Device);
    fn on_device_lost(&self, device_id: String);
}

#[derive(uniffi::Object)]
pub struct DiscoveryService {
    listener: Option<Box<dyn DiscoveryListener>>,
}

#[uniffi::export]
impl DiscoveryService {
    pub fn set_listener(&mut self, listener: Box<dyn DiscoveryListener>) {
        self.listener = Some(listener);
    }

    pub fn start(&self) {
        let listener = self.listener.clone();
        tokio::spawn(async move {
            // Async discovery logic
            let device = discover_device().await;
            if let Some(listener) = listener {
                listener.on_device_found(device);
            }
        });
    }
}
```

```kotlin
// Kotlin
class DiscoveryViewModel : ViewModel(), DiscoveryListener {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    fun startDiscovery() {
        val service = DiscoveryService.create()
        service.setListener(this)
        service.start()
    }

    override fun onDeviceFound(device: Device) {
        viewModelScope.launch {
            _devices.update { it + device }
        }
    }

    override fun onDeviceLost(deviceId: String) {
        viewModelScope.launch {
            _devices.update { it.filterNot { d -> d.id == deviceId } }
        }
    }
}
```

### Strategy 2: Blocking with Timeout

```rust
// Rust: Block on async operation
#[uniffi::export]
fn discover_devices_blocking(timeout_ms: u64) -> Result<Vec<Device>, CosmicConnectError> {
    let runtime = tokio::runtime::Runtime::new().unwrap();
    runtime.block_on(async {
        tokio::time::timeout(
            Duration::from_millis(timeout_ms),
            discover_devices()
        )
        .await
        .map_err(|_| CosmicConnectError::TimeoutError)?
    })
}
```

```kotlin
// Kotlin: Call from IO dispatcher
viewModelScope.launch(Dispatchers.IO) {
    try {
        val devices = CosmicConnectCore.discoverDevicesBlocking(5000)
        _devices.update { devices }
    } catch (e: CosmicConnectError.TimeoutError) {
        Log.w(TAG, "Discovery timeout")
    }
}
```

### Strategy 3: Polling (Not Recommended)

```rust
// Rust: Return status, poll for completion
#[derive(uniffi::Object)]
pub struct DiscoveryTask {
    status: Arc<Mutex<TaskStatus>>,
}

#[uniffi::export]
impl DiscoveryTask {
    pub fn is_complete(&self) -> bool {
        matches!(*self.status.lock().unwrap(), TaskStatus::Complete)
    }

    pub fn get_result(&self) -> Option<Vec<Device>> {
        // Return result if complete
    }
}
```

**Recommendation**: Use **Strategy 1 (Callbacks)** for most async operations. It's the most ergonomic and efficient.

---

## 🎯 Example FFI Patterns

### Pattern: Repository + FFI Wrapper

```kotlin
// FFI Wrapper: Thin Kotlin wrapper around Rust
class CosmicConnectManager private constructor() {
    private val service: CosmicConnectService = CosmicConnectService.create(DeviceInfo.deviceId)
    private val discoveryListeners = mutableSetOf<DiscoveryListener>()

    init {
        service.setDiscoveryListener(object : RustDiscoveryListener {
            override fun onDeviceFound(device: Device) {
                discoveryListeners.forEach { it.onDeviceDiscovered(device) }
            }
        })
    }

    fun startDiscovery() {
        service.startDiscovery()
    }

    fun stopDiscovery() {
        service.stopDiscovery()
    }

    fun sendPacket(deviceId: String, packet: NetworkPacket) {
        service.sendPacket(deviceId, packet)
    }

    fun registerDiscoveryListener(listener: DiscoveryListener) {
        discoveryListeners.add(listener)
    }

    companion object {
        @Volatile
        private var instance: CosmicConnectManager? = null

        fun getInstance(): CosmicConnectManager {
            return instance ?: synchronized(this) {
                instance ?: CosmicConnectManager().also { instance = it }
            }
        }
    }
}

// Repository: MVVM pattern
class DeviceRepository(
    private val cosmicConnect: CosmicConnectManager
) {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    init {
        cosmicConnect.registerDiscoveryListener(object : DiscoveryListener {
            override fun onDeviceDiscovered(device: Device) {
                _devices.update { currentDevices ->
                    if (currentDevices.none { it.id == device.id }) {
                        currentDevices + device
                    } else {
                        currentDevices
                    }
                }
            }

            override fun onDeviceLost(deviceId: String) {
                _devices.update { it.filterNot { d -> d.id == deviceId } }
            }
        })
    }

    fun startDiscovery() {
        cosmicConnect.startDiscovery()
    }

    fun stopDiscovery() {
        cosmicConnect.stopDiscovery()
    }

    suspend fun sendPacket(deviceId: String, packetType: String, body: String) {
        withContext(Dispatchers.IO) {
            val packet = NetworkPacket.create(packetType, System.currentTimeMillis(), body)
            cosmicConnect.sendPacket(deviceId, packet)
        }
    }
}
```

---

## 📊 Performance Considerations

### FFI Overhead

1. **Minimize FFI calls**: Batch operations when possible
   ```kotlin
   // Bad: Multiple FFI calls
   for (device in devices) {
       cosmicConnect.sendPacket(device.id, packet)
   }

   // Good: Single FFI call
   cosmicConnect.sendPacketToMultiple(devices.map { it.id }, packet)
   ```

2. **Use callbacks for streams**: Don't poll across FFI
   ```rust
   // Good: Push-based with callbacks
   pub trait PacketListener {
       fn on_packet_received(&self, device_id: String, packet: NetworkPacket);
   }

   // Bad: Pull-based polling
   pub fn get_next_packet(&self) -> Option<NetworkPacket> { /* ... */ }
   ```

3. **Avoid large data copies**: Use references when possible
   ```rust
   // Good: Return owned data
   pub fn get_devices(&self) -> Vec<Device> { /* ... */ }

   // Bad: Return serialized string (requires parsing on Kotlin side)
   pub fn get_devices_json(&self) -> String { /* ... */ }
   ```

### Benchmarks

Target FFI call overhead: < 1µs per call

```rust
// benches/ffi_bench.rs
use criterion::{criterion_group, criterion_main, Criterion};

fn benchmark_ffi_packet_create(c: &mut Criterion) {
    c.bench_function("ffi packet create", |b| {
        b.iter(|| {
            create_packet("kdeconnect.ping".to_string(), 123, "{}".to_string())
        });
    });
}
```

---

## ✅ Best Practices Summary

### DO:

✅ Use `Arc<T>` for shared ownership
✅ Implement `Send + Sync` for thread safety
✅ Use callbacks for async operations
✅ Define specific error types
✅ Document lifetime expectations
✅ Test FFI boundaries thoroughly
✅ Use `uniffi::export` for public APIs
✅ Batch FFI calls when possible
✅ Keep FFI layer thin

### DON'T:

❌ Pass raw pointers across FFI
❌ Use `unsafe` without documentation
❌ Block main thread with FFI calls
❌ Poll for async results
❌ Copy large data unnecessarily
❌ Use generic error strings
❌ Forget to call `destroy()` in Kotlin
❌ Share mutable state across FFI
❌ Assume thread safety without verification

---

## 🧪 Testing FFI

### Unit Tests (Rust)

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_packet_creation() {
        let packet = create_packet(
            "kdeconnect.ping".to_string(),
            123,
            "{}".to_string()
        ).unwrap();

        assert_eq!(packet.get_type(), "kdeconnect.ping");
        assert_eq!(packet.get_id(), 123);
    }
}
```

### Integration Tests (Kotlin + Rust)

```kotlin
@Test
fun testPacketSerializationRoundtrip() {
    val packet = NetworkPacket.create("kdeconnect.ping", 123, "{}")
    val bytes = packet.serialize()

    // Verify newline terminator
    assertEquals('\n'.code.toByte(), bytes.last())

    val deserialized = NetworkPacket.deserialize(bytes)
    assertEquals("kdeconnect.ping", deserialized.getType())
    assertEquals(123, deserialized.getId())
}
```

### Memory Leak Tests

```kotlin
@Test
fun testNoMemoryLeaks() {
    val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    repeat(10000) {
        val packet = NetworkPacket.create("test", it.toLong(), "{}")
        packet.serialize()
        packet.destroy()  // Explicit destroy
    }

    System.gc()
    val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    // Memory should not grow significantly
    assertTrue(finalMemory - initialMemory < 10_000_000)  // < 10MB
}
```

---

## 📚 Resources

### uniffi-rs
- [Official Documentation](https://mozilla.github.io/uniffi-rs/)
- [UDL File Reference](https://mozilla.github.io/uniffi-rs/udl/index.html)
- [Examples](https://github.com/mozilla/uniffi-rs/tree/main/examples)

### FFI Best Practices
- [The Rustonomicon (FFI)](https://doc.rust-lang.org/nomicon/ffi.html)
- [cargo-ndk Guide](https://github.com/bbqsrc/cargo-ndk)

### Android + Rust
- [Android Rust Introduction](https://source.android.com/docs/setup/build/rust/building-rust-modules/overview)
- [JNI Performance Tips](https://developer.android.com/training/articles/perf-jni)

---

## 📝 Document History

- **2026-01-15**: Initial version created
- **Status**: Design phase
- **Next Review**: After Phase 0 completion

---

**Ready to build the FFI bridge? Start with Issue #49! 🦀↔️**
