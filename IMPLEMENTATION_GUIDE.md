# COSMIC Connect - Quick Implementation Guide
## Getting Started with Rust + Kotlin Hybrid Architecture

## 🚀 Quick Start

This guide helps you get started implementing the Rust + Kotlin hybrid architecture for COSMIC Connect.

### Prerequisites

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Add Android targets
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi  
rustup target add x86_64-linux-android
rustup target add i686-linux-android

# Install cargo tools
cargo install cargo-ndk
cargo install uniffi-bindgen

# Install Android Studio
# Download from https://developer.android.com/studio
```

---

## 📁 Repository Structure

```
cosmic-connect-ecosystem/
├── cosmic-connect-core/          # NEW: Create this
│   ├── src/
│   │   ├── protocol/
│   │   ├── network/
│   │   ├── crypto/
│   │   ├── plugins/
│   │   └── ffi/
│   ├── Cargo.toml
│   └── cosmic_connect_core.udl
│
├── cosmic-connect-android/       # Update this
│   ├── rust-ffi/                 # NEW: Add this
│   ├── src/
│   └── build.gradle.kts
│
└── cosmic-applet-kdeconnect/    # Update this
    └── Cargo.toml
```

---

## 🔧 Step 1: Create Rust Core Library

### Initialize Project

```bash
cargo new --lib cosmic-connect-core
cd cosmic-connect-core
```

### Configure Cargo.toml

```toml
[package]
name = "cosmic-connect-core"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["lib", "staticlib", "cdylib"]

[dependencies]
tokio = { version = "1.35", features = ["full"] }
rustls = "0.22"
rcgen = "0.12"
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
uniffi = "0.26"
log = "0.4"
thiserror = "1.0"

[build-dependencies]
uniffi = { version = "0.26", features = ["build"] }

[dev-dependencies]
tokio-test = "0.4"

[profile.release]
opt-level = 3
lto = true
```

### Create uniffi IDL (`cosmic_connect_core.udl`)

```idl
namespace cosmic_connect_core {
  // Initialization
  void initialize();
  
  // Packet operations
  FfiNetworkPacket create_network_packet(string packet_type, i64 id, string body);
  bytes serialize_packet(FfiNetworkPacket packet);
  FfiNetworkPacket deserialize_packet(bytes data);
  
  // Discovery
  void start_discovery();
  void stop_discovery();
  void broadcast_identity(FfiDeviceIdentity identity);
  
  // Certificate operations
  bytes generate_certificate(string device_id);
  string get_certificate_fingerprint(bytes cert);
  
  // Connection
  void connect_device(string device_id, string address, u16 port);
  void disconnect_device(string device_id);
  
  // Plugin operations
  void send_battery_update(string device_id, i32 level, boolean charging);
  void send_file(string device_id, string file_path);
};

// Records (structs)
dictionary FfiNetworkPacket {
  string packet_type;
  i64 id;
  string body;
};

dictionary FfiDeviceIdentity {
  string device_id;
  string device_name;
  string device_type;
  u32 protocol_version;
  u16 tcp_port;
};

// Callback interface for events from Rust to platform
callback interface EventCallback {
  void on_device_discovered(FfiDeviceIdentity identity);
  void on_device_connected(string device_id);
  void on_device_disconnected(string device_id);
  void on_packet_received(string device_id, FfiNetworkPacket packet);
  void on_pairing_request(string device_id);
};
```

### Create build.rs

```rust
fn main() {
    uniffi::generate_scaffolding("src/cosmic_connect_core.udl").unwrap();
}
```

---

## 📝 Step 2: Implement Core Protocol

### src/protocol/packet.rs

```rust
use serde::{Deserialize, Serialize};
use thiserror::Error;

#[derive(Error, Debug)]
pub enum PacketError {
    #[error("Serialization error: {0}")]
    Serialization(#[from] serde_json::Error),
    #[error("Invalid packet format")]
    InvalidFormat,
    #[error("Missing newline terminator")]
    MissingNewline,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkPacket {
    #[serde(rename = "type")]
    pub packet_type: String,
    pub id: i64,
    pub body: serde_json::Value,
}

impl NetworkPacket {
    pub fn new(packet_type: String, id: i64) -> Self {
        Self {
            packet_type,
            id,
            body: serde_json::Value::Object(Default::default()),
        }
    }
    
    pub fn with_body(mut self, body: serde_json::Value) -> Self {
        self.body = body;
        self
    }
    
    /// Serialize packet to bytes with newline termination (CRITICAL for protocol)
    pub fn serialize(&self) -> Result<Vec<u8>, PacketError> {
        let json = serde_json::to_string(self)?;
        let mut bytes = json.into_bytes();
        bytes.push(b'\n');  // CRITICAL: Protocol requires \n termination
        Ok(bytes)
    }
    
    /// Deserialize from bytes (expects newline termination)
    pub fn deserialize(data: &[u8]) -> Result<Self, PacketError> {
        // Check for newline termination
        if data.last() != Some(&b'\n') {
            return Err(PacketError::MissingNewline);
        }
        
        // Remove newline and parse
        let data = &data[..data.len() - 1];
        let packet = serde_json::from_slice(data)?;
        Ok(packet)
    }
    
    pub fn validate(&self) -> Result<(), PacketError> {
        if self.packet_type.is_empty() {
            return Err(PacketError::InvalidFormat);
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_packet_serialization() {
        let packet = NetworkPacket::new("identity".to_string(), 123);
        let serialized = packet.serialize().unwrap();
        
        // Check newline termination
        assert_eq!(serialized.last(), Some(&b'\n'));
        
        let deserialized = NetworkPacket::deserialize(&serialized).unwrap();
        assert_eq!(packet.packet_type, deserialized.packet_type);
    }
}
```

### src/ffi/mod.rs

```rust
use std::sync::Arc;

// Include the uniffi scaffolding
uniffi::include_scaffolding!("cosmic_connect_core");

// FFI types
#[derive(uniffi::Record)]
pub struct FfiNetworkPacket {
    pub packet_type: String,
    pub id: i64,
    pub body: String,
}

#[derive(uniffi::Record)]
pub struct FfiDeviceIdentity {
    pub device_id: String,
    pub device_name: String,
    pub device_type: String,
    pub protocol_version: u32,
    pub tcp_port: u16,
}

// FFI functions
#[uniffi::export]
pub fn create_network_packet(
    packet_type: String,
    id: i64,
    body: String,
) -> Arc<FfiNetworkPacket> {
    Arc::new(FfiNetworkPacket {
        packet_type,
        id,
        body,
    })
}

#[uniffi::export]
pub fn serialize_packet(packet: Arc<FfiNetworkPacket>) -> Vec<u8> {
    use crate::protocol::packet::NetworkPacket;
    
    let body: serde_json::Value = serde_json::from_str(&packet.body)
        .unwrap_or(serde_json::Value::Null);
    
    let internal_packet = NetworkPacket::new(
        packet.packet_type.clone(),
        packet.id,
    ).with_body(body);
    
    internal_packet.serialize().unwrap_or_default()
}

#[uniffi::export]
pub fn deserialize_packet(data: Vec<u8>) -> Result<Arc<FfiNetworkPacket>, String> {
    use crate::protocol::packet::NetworkPacket;
    
    let internal_packet = NetworkPacket::deserialize(&data)
        .map_err(|e| e.to_string())?;
    
    Ok(Arc::new(FfiNetworkPacket {
        packet_type: internal_packet.packet_type,
        id: internal_packet.id,
        body: internal_packet.body.to_string(),
    }))
}

// Callback interface
#[uniffi::export(callback_interface)]
pub trait EventCallback: Send + Sync {
    fn on_device_discovered(&self, identity: Arc<FfiDeviceIdentity>);
    fn on_device_connected(&self, device_id: String);
    fn on_device_disconnected(&self, device_id: String);
    fn on_packet_received(&self, device_id: String, packet: Arc<FfiNetworkPacket>);
    fn on_pairing_request(&self, device_id: String);
}
```

---

## 🔨 Step 3: Build Rust Core

```bash
cd cosmic-connect-core

# Build for Linux (testing)
cargo build --release
cargo test

# Build for Android
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 build --release
```

---

## 📱 Step 4: Integrate with Android

### Update build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.3"
}

cargo {
    module = "../cosmic-connect-core"
    libname = "cosmic_connect_core"
    targets = listOf("arm64", "arm", "x86_64", "x86")
    profile = "release"
}

android {
    namespace = "org.kde.kdeconnect_tp"
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("$buildDir/rustJniLibs/android")
        }
    }
}

dependencies {
    // JNA for uniffi
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    
    // Existing dependencies...
}

// Ensure Rust builds before Android
tasks.whenTaskAdded {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("cargoBuild")
    }
}
```

### Generate Kotlin Bindings

```bash
cd cosmic-connect-core
uniffi-bindgen generate src/cosmic_connect_core.udl --language kotlin --out-dir ../cosmic-connect-android/app/src/main/kotlin/uniffi
```

### Create Kotlin Wrapper

`src/main/kotlin/org/kde/kdeconnect_tp/core/CosmicConnectCore.kt`:

```kotlin
package org.kde.kdeconnect_tp.core

import uniffi.cosmic_connect_core.*
import java.util.concurrent.ConcurrentHashMap

object CosmicConnectCore {
    private var initialized = false
    private val callbacks = ConcurrentHashMap<String, EventCallback>()
    
    init {
        System.loadLibrary("cosmic_connect_core")
    }
    
    fun initialize() {
        if (!initialized) {
            initializeNative()
            initialized = true
        }
    }
    
    fun registerCallback(key: String, callback: EventCallback) {
        callbacks[key] = callback
    }
    
    fun unregisterCallback(key: String) {
        callbacks.remove(key)
    }
    
    private fun notifyCallbacks(action: (EventCallback) -> Unit) {
        callbacks.values.forEach(action)
    }
}

// Kotlin-friendly wrappers
class NetworkPacket private constructor(
    private val ffiPacket: FfiNetworkPacket
) {
    val type: String get() = ffiPacket.packetType
    val id: Long get() = ffiPacket.id
    val body: String get() = ffiPacket.body
    
    fun serialize(): ByteArray = serializePacket(ffiPacket)
    
    companion object {
        fun create(type: String, id: Long, body: String): NetworkPacket {
            val ffiPacket = createNetworkPacket(type, id, body)
            return NetworkPacket(ffiPacket)
        }
        
        fun deserialize(data: ByteArray): NetworkPacket {
            val ffiPacket = deserializePacket(data)
            return NetworkPacket(ffiPacket)
        }
    }
}
```

---

## 🧪 Step 5: Test Integration

### Create Test

`src/test/kotlin/org/kde/kdeconnect_tp/core/NetworkPacketTest.kt`:

```kotlin
import org.junit.Test
import org.junit.Assert.*
import org.kde.kdeconnect_tp.core.NetworkPacket

class NetworkPacketTest {
    @Test
    fun testPacketCreation() {
        val packet = NetworkPacket.create("identity", 123, "{}")
        
        assertEquals("identity", packet.type)
        assertEquals(123L, packet.id)
    }
    
    @Test
    fun testPacketSerialization() {
        val packet = NetworkPacket.create("identity", 123, "{\"test\": \"value\"}")
        
        val serialized = packet.serialize()
        
        // Check newline termination
        assertEquals('\n'.code.toByte(), serialized.last())
        
        val deserialized = NetworkPacket.deserialize(serialized)
        
        assertEquals(packet.type, deserialized.type)
        assertEquals(packet.id, deserialized.id)
    }
}
```

### Run Tests

```bash
cd cosmic-connect-android
./gradlew test
```

---

## 🎯 Step 6: Implement First Feature (Discovery)

### Rust Side (`src/network/discovery.rs`)

```rust
use tokio::net::UdpSocket;
use std::net::Ipv4Addr;

const MULTICAST_GROUP: Ipv4Addr = Ipv4Addr::new(224, 0, 0, 251);
const PORT: u16 = 1716;

pub struct DiscoveryService {
    socket: Option<UdpSocket>,
}

impl DiscoveryService {
    pub fn new() -> Self {
        Self { socket: None }
    }
    
    pub async fn start(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        let socket = UdpSocket::bind(("0.0.0.0", PORT)).await?;
        socket.set_broadcast(true)?;
        socket.join_multicast_v4(MULTICAST_GROUP, Ipv4Addr::UNSPECIFIED)?;
        
        self.socket = Some(socket);
        Ok(())
    }
    
    pub async fn broadcast_identity(
        &self,
        identity: &crate::protocol::identity::DeviceIdentity,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let packet = identity.to_network_packet();
        let data = packet.serialize()?;
        
        if let Some(socket) = &self.socket {
            socket.send_to(&data, (MULTICAST_GROUP, PORT)).await?;
        }
        
        Ok(())
    }
}
```

### FFI Wrapper

```rust
#[uniffi::export]
pub async fn start_discovery() -> Result<(), String> {
    // Implementation
    Ok(())
}

#[uniffi::export]
pub async fn broadcast_identity(identity: Arc<FfiDeviceIdentity>) -> Result<(), String> {
    // Implementation
    Ok(())
}
```

### Kotlin Integration

```kotlin
class DiscoveryManager {
    suspend fun start() = withContext(Dispatchers.IO) {
        startDiscovery()
    }
    
    suspend fun broadcastIdentity(identity: DeviceIdentity) = withContext(Dispatchers.IO) {
        val ffiIdentity = FfiDeviceIdentity(
            deviceId = identity.id,
            deviceName = identity.name,
            deviceType = identity.type,
            protocolVersion = identity.protocolVersion,
            tcpPort = identity.tcpPort
        )
        broadcastIdentityNative(ffiIdentity)
    }
}
```

---

## 🏃 Development Workflow

### Daily Development

```bash
# 1. Make changes to Rust core
cd cosmic-connect-core
cargo build
cargo test

# 2. Regenerate Kotlin bindings (if .udl changed)
uniffi-bindgen generate src/cosmic_connect_core.udl \
  --language kotlin \
  --out-dir ../cosmic-connect-android/app/src/main/kotlin/uniffi

# 3. Build Android app
cd ../cosmic-connect-android
./gradlew build

# 4. Run on device/emulator
./gradlew installDebug
```

### Debugging

**Rust Logs (logcat)**:
```bash
adb logcat | grep RustCore
```

**Kotlin Logs**:
```kotlin
Log.d("CosmicConnect", "Device discovered: ${device.name}")
```

**Memory Leaks**:
```bash
# Run with LeakCanary
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
```

---

## 📚 Common Patterns

### Error Handling Across FFI

**Rust**:
```rust
#[uniffi::export]
pub fn risky_operation() -> Result<String, String> {
    do_something()
        .map_err(|e| format!("Operation failed: {}", e))
}
```

**Kotlin**:
```kotlin
try {
    val result = riskyOperation()
    // Success
} catch (e: Exception) {
    // Handle error
    Log.e("CosmicConnect", "Error: ${e.message}")
}
```

### Async Operations

**Rust**:
```rust
#[uniffi::export]
pub async fn async_operation() -> Result<(), String> {
    // Use tokio
    tokio::time::sleep(Duration::from_secs(1)).await;
    Ok(())
}
```

**Kotlin**:
```kotlin
suspend fun asyncOperation() {
    withContext(Dispatchers.IO) {
        asyncOperationNative()
    }
}
```

### Callbacks

**Rust**:
```rust
static CALLBACK: OnceCell<Arc<dyn EventCallback>> = OnceCell::new();

fn trigger_event(device_id: String) {
    if let Some(callback) = CALLBACK.get() {
        callback.on_device_connected(device_id);
    }
}
```

**Kotlin**:
```kotlin
CosmicConnectCore.registerCallback("main", object : EventCallback {
    override fun onDeviceConnected(deviceId: String) {
        // Handle event
    }
})
```

---

## 🐛 Troubleshooting

### Build Issues

**Rust not found**:
```bash
rustup show  # Verify installation
```

**Android targets missing**:
```bash
rustup target list --installed
rustup target add aarch64-linux-android
```

**cargo-ndk not found**:
```bash
cargo install cargo-ndk
```

### Runtime Issues

**Library not loaded**:
- Check `jniLibs` directory exists
- Verify SO files for all architectures
- Check System.loadLibrary() is called

**FFI crashes**:
- Run with RUST_BACKTRACE=1
- Check for null pointer dereferences
- Verify Arc usage for shared objects

### Performance Issues

**Slow builds**:
- Use `sccache` for Rust
- Enable parallel compilation
- Use `--release` only when needed

**High memory usage**:
- Profile with Android Profiler
- Check for Arc cycles
- Verify FFI objects are freed

---

## 🎓 Next Steps

1. **Complete Protocol Implementation**: Finish all packet types in Rust
2. **Implement Plugins**: Battery, Share, Clipboard, etc.
3. **Add TLS/Certificates**: Security implementation
4. **Build UI**: Jetpack Compose screens
5. **Testing**: Comprehensive test suite
6. **Documentation**: API docs and guides

---

## 📖 Resources

- [uniffi-rs Book](https://mozilla.github.io/uniffi-rs/)
- [Rust Async Book](https://rust-lang.github.io/async-book/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android NDK](https://developer.android.com/ndk)
- [KDE Connect Protocol](https://invent.kde.org/network/kdeconnect-kde/-/tree/master/core)

---

## 💡 Tips

1. **Start Small**: Get basic packet serialization working first
2. **Test Often**: Run tests after each change
3. **Use Types**: Leverage Rust's type system for safety
4. **Document FFI**: Clear comments on boundary functions
5. **Profile Early**: Watch for performance issues
6. **Version Carefully**: Semantic versioning for the core library

---

## ✅ Checklist

- [ ] Rust toolchain installed
- [ ] Android targets added
- [ ] cargo-ndk installed
- [ ] cosmic-connect-core created
- [ ] Basic NetworkPacket implemented
- [ ] FFI bindings generated
- [ ] Kotlin wrappers created
- [ ] First test passing
- [ ] Discovery working
- [ ] Ready for next steps!

**Happy coding! 🚀**
