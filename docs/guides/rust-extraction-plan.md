# Rust Core Extraction Plan - Phase 0

> **Status**: Planning
> **Duration**: 3 weeks (Weeks 1-3)
> **Issues**: #43-#50
> **Goal**: Extract KDE Connect protocol implementation from COSMIC applet into shared `cosmic-connect-core` library

---

## 📋 Executive Summary

This document outlines the complete plan for **Phase 0: Rust Core Extraction**, the foundation of the hybrid Rust + Kotlin architecture. We will extract the proven protocol implementation from the existing COSMIC Desktop applet into a standalone Rust library that both platforms can share.

### Why Extract to Rust?

1. **Single Source of Truth**: Fix protocol bugs once, both platforms benefit
2. **Memory Safety**: Rust's ownership system prevents entire classes of bugs
3. **70%+ Code Sharing**: Protocol logic shared between Android and COSMIC Desktop
4. **Performance**: Native speed with zero-cost abstractions
5. **Best Language for Layer**: Protocol/networking is perfect for Rust

---

## 🎯 Extraction Goals

### Primary Goals

1. **Extract Core Protocol Components**:
   - NetworkPacket serialization/deserialization
   - Device discovery (UDP multicast)
   - TLS certificate management and handshake
   - Plugin system architecture

2. **Create Clean FFI Boundaries**:
   - Design interfaces suitable for uniffi-rs
   - Minimize cross-language data transfer
   - Handle errors gracefully across FFI

3. **Maintain Protocol Compatibility**:
   - 100% KDE Connect protocol v7 compliance
   - Newline-terminated JSON packets
   - Correct TLS role determination
   - UDP discovery on 224.0.0.251:1716

4. **Comprehensive Testing**:
   - Unit tests for all protocol logic
   - Integration tests for network operations
   - Protocol compatibility tests with existing KDE Connect clients

### Success Criteria

- ✅ cosmic-connect-core compiles without warnings
- ✅ All Rust tests pass (unit + integration)
- ✅ COSMIC Desktop applet works with new core
- ✅ uniffi-rs generates valid Kotlin bindings
- ✅ No protocol regressions (tested against KDE Connect Android)

---

## 📂 Source Analysis

### What to Extract from COSMIC Applet

Based on Issue #43 analysis, we need to extract from `cosmic-applet-kdeconnect`:

#### 1. **NetworkPacket** (`src/protocol/packet.rs` - to be created)
**Location in applet**: `src/network/packet.rs` or similar
**Components**:
- Packet structure (id, type, body)
- JSON serialization with serde
- **CRITICAL**: Newline termination (`\n`)
- Type-safe packet body handling
- Packet validation

```rust
// Target structure
pub struct NetworkPacket {
    pub id: i64,
    pub packet_type: String,
    pub body: serde_json::Value,
}

// CRITICAL: Must serialize with \n terminator
impl NetworkPacket {
    pub fn serialize(&self) -> Result<Vec<u8>, PacketError> {
        let json = serde_json::to_string(self)?;
        let mut bytes = json.into_bytes();
        bytes.push(b'\n');  // REQUIRED!
        Ok(bytes)
    }
}
```

#### 2. **Discovery** (`src/network/discovery.rs` - to be created)
**Location in applet**: Discovery implementation
**Components**:
- UDP multicast listener (224.0.0.251:1716)
- Identity packet broadcasting
- Device announcement
- Async tokio implementation
- Network interface enumeration

```rust
// Target structure
pub struct DiscoveryService {
    socket: UdpSocket,
    identity: Identity,
}

impl DiscoveryService {
    pub async fn start(&self) -> Result<(), DiscoveryError> {
        // Bind to 224.0.0.251:1716
        // Send identity broadcasts every 5 seconds
        // Listen for other devices
    }
}
```

#### 3. **TLS & Certificates** (`src/crypto/` - to be created)
**Location in applet**: Certificate and TLS handling
**Components**:
- RSA 2048-bit certificate generation (rcgen)
- Certificate storage abstraction
- TLS connection wrapper (rustls)
- **CRITICAL**: TLS role determination (deviceId comparison)
- Certificate pinning
- Handshake logic

```rust
// Target structure
pub struct TlsManager {
    cert: Certificate,
    device_id: String,
}

impl TlsManager {
    // CRITICAL: Correct TLS role logic
    pub fn determine_role(&self, peer_id: &str) -> TlsRole {
        if self.device_id > peer_id {
            TlsRole::Server
        } else {
            TlsRole::Client
        }
    }
}
```

#### 4. **Plugin Core** (`src/plugins/` - to be created)
**Location in applet**: Plugin trait and manager
**Components**:
- Plugin trait definition
- PluginManager for registration
- Packet routing to plugins
- Plugin lifecycle hooks

```rust
// Target structure
pub trait Plugin: Send + Sync {
    fn name(&self) -> &str;
    fn incoming_capabilities(&self) -> &[&str];
    fn outgoing_capabilities(&self) -> &[&str];

    async fn handle_packet(&self, packet: NetworkPacket) -> Result<(), PluginError>;
}

pub struct PluginManager {
    plugins: HashMap<String, Box<dyn Plugin>>,
}
```

#### 5. **Identity & Device** (`src/protocol/` - to be created)
**Components**:
- Device identity structure
- Device information
- Capability list management

---

## 🏗️ cosmic-connect-core Module Structure

### Crate Organization

```
cosmic-connect-core/
├── Cargo.toml
├── README.md
├── ARCHITECTURE.md
├── src/
│   ├── lib.rs                 # Public API exports
│   │
│   ├── protocol/              # Protocol layer
│   │   ├── mod.rs
│   │   ├── packet.rs          # NetworkPacket
│   │   ├── identity.rs        # Device identity
│   │   └── pairing.rs         # Pairing protocol
│   │
│   ├── network/               # Network layer
│   │   ├── mod.rs
│   │   ├── discovery.rs       # UDP discovery
│   │   ├── connection.rs      # TCP connections
│   │   └── transport.rs       # Packet transport
│   │
│   ├── crypto/                # Crypto layer
│   │   ├── mod.rs
│   │   ├── certificate.rs     # Certificate generation
│   │   ├── tls.rs             # TLS handling
│   │   └── storage.rs         # Storage abstraction
│   │
│   ├── plugins/               # Plugin system
│   │   ├── mod.rs
│   │   ├── plugin.rs          # Plugin trait
│   │   └── manager.rs         # PluginManager
│   │
│   ├── ffi/                   # FFI layer
│   │   ├── mod.rs
│   │   ├── core.udl           # uniffi interface
│   │   └── error.rs           # Error translation
│   │
│   └── error.rs               # Common error types
│
├── tests/                     # Integration tests
│   ├── packet_tests.rs
│   ├── discovery_tests.rs
│   ├── tls_tests.rs
│   └── integration_tests.rs
│
└── benches/                   # Benchmarks
    └── protocol_bench.rs
```

### Cargo.toml Dependencies

```toml
[package]
name = "cosmic-connect-core"
version = "0.1.0"
edition = "2021"
rust-version = "1.70"

[lib]
crate-type = ["lib", "cdylib", "staticlib"]

[dependencies]
# Async runtime
tokio = { version = "1.35", features = ["full"] }

# Networking
socket2 = "0.5"

# TLS
rustls = "0.23"
rcgen = "0.13"
webpki-roots = "0.26"

# Serialization
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"

# Error handling
thiserror = "1.0"
anyhow = "1.0"

# FFI
uniffi = "0.27"

# Crypto
rand = "0.8"
sha2 = "0.10"

# Utilities
tracing = "0.1"
tracing-subscriber = "0.3"
once_cell = "1.19"

[dev-dependencies]
tokio-test = "0.4"
criterion = "0.5"

[build-dependencies]
uniffi = { version = "0.27", features = ["build"] }

[[bin]]
name = "uniffi-bindgen"
path = "uniffi-bindgen.rs"

[profile.release]
opt-level = 3
lto = true
codegen-units = 1
```

---

## 📝 Step-by-Step Extraction Plan

### Week 1: Analysis & Foundation

#### Day 1-2: Issue #43 - Analyze COSMIC Applet (1-2 days)
**Goal**: Map all protocol-related code

**Tasks**:
1. Clone `cosmic-applet-kdeconnect` repository
2. Identify protocol implementation files:
   - Packet serialization/deserialization
   - Discovery implementation
   - TLS/certificate handling
   - Plugin system
3. Document current architecture:
   - Module dependencies
   - Data structures
   - Async patterns
4. Create `docs/applet-architecture.md`:
   - Component diagram
   - File-to-component mapping
   - API boundaries
5. Identify extraction boundaries:
   - What moves to core (protocol)
   - What stays in applet (UI, desktop integration)
6. Create this document (`docs/rust-extraction-plan.md`)

**Deliverable**: docs/applet-architecture.md, this document completed

#### Day 3: Issue #44 - Create Cargo Project (4 hours)
**Goal**: Initialize cosmic-connect-core

**Tasks**:
1. Create new GitHub repository: `cosmic-connect-core`
2. Initialize Cargo library project:
   ```bash
   cargo new --lib cosmic-connect-core
   cd cosmic-connect-core
   ```
3. Set up module structure (create directories from above)
4. Configure Cargo.toml:
   - Add all dependencies
   - Configure uniffi build
   - Set crate-type for FFI
5. Create basic lib.rs with exports
6. Add README.md and ARCHITECTURE.md
7. Set up CI/CD (GitHub Actions):
   - Rust tests
   - Clippy linting
   - Format checking
8. First commit and tag (v0.1.0-alpha)

**Deliverable**: Working Cargo project with CI/CD

#### Day 4-5: Issue #45 - Extract NetworkPacket (1 day)
**Goal**: Implement packet serialization

**Tasks**:
1. Create `src/protocol/packet.rs`
2. Define NetworkPacket struct:
   ```rust
   #[derive(Debug, Clone, Serialize, Deserialize)]
   pub struct NetworkPacket {
       pub id: i64,
       #[serde(rename = "type")]
       pub packet_type: String,
       pub body: serde_json::Value,
   }
   ```
3. Implement methods:
   - `new()` - create packet with auto-generated ID
   - `serialize()` - **CRITICAL**: Add `\n` terminator
   - `deserialize()` - parse from bytes
   - `packet_type()` - type-safe packet type enum
4. Add comprehensive tests:
   - Serialization roundtrip
   - Newline termination verification
   - Invalid packet handling
   - Edge cases (empty body, large packets)
5. Add integration test with real KDE Connect packets

**Deliverable**: Working NetworkPacket with tests passing

### Week 2: Core Components

#### Day 1: Issue #46 - Extract Discovery (1 day)
**Goal**: Implement UDP multicast discovery

**Tasks**:
1. Create `src/network/discovery.rs`
2. Implement DiscoveryService:
   - UDP socket bound to 224.0.0.251:1716
   - Identity packet broadcasting (every 5 seconds)
   - Listen for other devices
   - Parse incoming identity packets
3. Handle network interface enumeration:
   - Enumerate all network interfaces
   - Broadcast on each interface
4. Async implementation with tokio:
   ```rust
   pub struct DiscoveryService {
       socket: UdpSocket,
       identity: Identity,
       tx: mpsc::Sender<Device>,
   }

   impl DiscoveryService {
       pub async fn start(&mut self) -> Result<(), DiscoveryError> {
           // Broadcast loop
           // Listen loop
       }
   }
   ```
5. Write tests:
   - Mock UDP socket
   - Test identity broadcasting
   - Test packet parsing
   - Integration test with two discovery services

**Deliverable**: Working discovery service with tests

#### Day 2: Issue #47 - Extract TLS/Certificates (2 days)
**Goal**: Implement certificate and TLS handling

**Tasks**:
1. Create `src/crypto/certificate.rs`:
   - Generate RSA 2048-bit certificates with rcgen
   - Certificate serialization (PEM format)
   - Device ID from certificate fingerprint
2. Create `src/crypto/storage.rs`:
   - Storage trait abstraction:
     ```rust
     pub trait CertificateStorage: Send + Sync {
         async fn store_certificate(&self, device_id: &str, cert: &[u8]) -> Result<()>;
         async fn load_certificate(&self, device_id: &str) -> Result<Option<Vec<u8>>>;
         async fn remove_certificate(&self, device_id: &str) -> Result<()>;
     }
     ```
   - In-memory implementation (for testing)
   - File-based implementation
3. Create `src/crypto/tls.rs`:
   - TLS connection wrapper using rustls
   - **CRITICAL**: TLS role determination:
     ```rust
     pub fn determine_role(my_id: &str, peer_id: &str) -> TlsRole {
         if my_id > peer_id {
             TlsRole::Server  // Lexicographically larger is server
         } else {
             TlsRole::Client
         }
     }
     ```
   - Handshake implementation
   - Certificate pinning
4. Write security tests:
   - Certificate generation
   - TLS handshake (both roles)
   - Role determination edge cases
   - Certificate pinning validation

**Deliverable**: Working TLS implementation with security tests

#### Day 3-4: Issue #48 - Extract Plugin Core (1 day)
**Goal**: Implement plugin system

**Tasks**:
1. Create `src/plugins/plugin.rs`:
   - Define Plugin trait:
     ```rust
     #[async_trait]
     pub trait Plugin: Send + Sync {
         fn name(&self) -> &str;
         fn incoming_capabilities(&self) -> &[&str];
         fn outgoing_capabilities(&self) -> &[&str];

         async fn handle_packet(&self, packet: NetworkPacket) -> Result<()>;
         async fn on_connected(&self, device: &Device) -> Result<()>;
         async fn on_disconnected(&self) -> Result<()>;
     }
     ```
2. Create `src/plugins/manager.rs`:
   - PluginManager implementation
   - Plugin registration
   - Packet routing by capability
   - Plugin lifecycle management
3. Create example plugin (ping/pong):
   - Simple request/response
   - Demonstrates plugin usage
4. Write tests:
   - Plugin registration
   - Packet routing
   - Capability matching
   - Lifecycle hooks

**Deliverable**: Working plugin system with example

#### Day 5: Issue #49 - Set up uniffi-rs (1 day)
**Goal**: Generate Kotlin FFI bindings

**Tasks**:
1. Create `src/ffi/core.udl`:
   - Define FFI interface for NetworkPacket
   - Define FFI interface for DiscoveryService
   - Define FFI interface for TlsManager
   - Define FFI interface for PluginManager
   - Handle errors gracefully
   Example:
   ```
   namespace cosmic_connect_core {
       NetworkPacket create_packet(string packet_type, i64 id, string body_json);
       bytes serialize_packet(NetworkPacket packet);
       NetworkPacket deserialize_packet(bytes data);
   }

   interface NetworkPacket {
       constructor(string packet_type, i64 id, string body_json);
       bytes serialize();
       string get_type();
       i64 get_id();
       string get_body_json();
   }
   ```
2. Create `build.rs`:
   - Configure uniffi code generation
   - Generate Kotlin bindings
3. Create `src/ffi/mod.rs`:
   - Export FFI functions
   - Error handling translation
4. Test FFI generation:
   ```bash
   cargo build
   uniffi-bindgen generate src/ffi/core.udl --language kotlin --out-dir ./bindings
   ```
5. Validate generated Kotlin code:
   - Check for proper error handling
   - Verify memory safety
   - Test basic FFI calls

**Deliverable**: Working uniffi-rs with Kotlin bindings

### Week 3: Validation

#### Day 1-3: Issue #50 - Validate with COSMIC Desktop (1-3 days)
**Goal**: Refactor COSMIC applet to use cosmic-connect-core

**Tasks**:
1. Update `cosmic-applet-kdeconnect` Cargo.toml:
   ```toml
   [dependencies]
   cosmic-connect-core = { path = "../cosmic-connect-core" }
   ```
2. Replace applet's protocol implementation:
   - Remove old NetworkPacket code
   - Use `cosmic_connect_core::NetworkPacket`
   - Remove old discovery code
   - Use `cosmic_connect_core::DiscoveryService`
   - Remove old TLS code
   - Use `cosmic_connect_core::TlsManager`
3. Update applet plugins to use core plugin trait
4. Test all functionality:
   - Device discovery works
   - TLS pairing works
   - Packet exchange works
   - All plugins work
5. Run integration tests:
   - COSMIC applet ↔ KDE Connect Android
   - Verify no regressions
6. Performance testing:
   - Compare before/after
   - Ensure no performance degradation

**Deliverable**: COSMIC applet working with cosmic-connect-core

#### Day 4-5: Documentation & Planning
**Goal**: Document Phase 0 and plan Phase 1

**Tasks**:
1. Complete `docs/ffi-design.md`:
   - Document FFI architecture
   - Explain error handling strategy
   - Memory management patterns
   - Thread safety considerations
2. Update cosmic-connect-core README:
   - API documentation
   - Usage examples
   - Building instructions
3. Create API documentation:
   ```bash
   cargo doc --open
   ```
4. Write integration guide for Android:
   - How to use core in Android via cargo-ndk
   - FFI integration patterns
   - Error handling examples
5. Plan Phase 1 (Android FFI Setup):
   - Review Issue #1 (Dev Environment)
   - Review Issue #51 (cargo-ndk setup)
   - Review Issue #52 (Android FFI wrapper)
6. Create Phase 0 completion report:
   - What was accomplished
   - Lessons learned
   - Blockers encountered
   - Recommendations for Phase 1

**Deliverable**: Complete documentation and Phase 1 plan

---

## 🧪 Testing Strategy

### Unit Tests

Each module must have comprehensive unit tests:

```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_packet_serialization() {
        let packet = NetworkPacket::new("cconnect.ping", 123);
        let bytes = packet.serialize().unwrap();

        // CRITICAL: Verify newline terminator
        assert_eq!(bytes.last(), Some(&b'\n'));

        let deserialized = NetworkPacket::deserialize(&bytes).unwrap();
        assert_eq!(packet.id, deserialized.id);
    }
}
```

### Integration Tests

Create integration tests in `tests/`:

```rust
// tests/integration_tests.rs
#[tokio::test]
async fn test_full_pairing_flow() {
    let device1 = create_test_device("device1");
    let device2 = create_test_device("device2");

    // Start discovery on both devices
    device1.start_discovery().await.unwrap();
    device2.start_discovery().await.unwrap();

    // Wait for discovery
    tokio::time::sleep(Duration::from_secs(2)).await;

    // Initiate pairing
    device1.pair_with(&device2.id).await.unwrap();

    // Verify TLS connection established
    assert!(device1.is_paired(&device2.id));
    assert!(device2.is_paired(&device1.id));
}
```

### Protocol Compatibility Tests

Test against real KDE Connect clients:

```rust
#[tokio::test]
async fn test_kde_connect_compatibility() {
    // Send packet to KDE Connect Android
    let packet = NetworkPacket::identity("test-device");
    send_to_kde_connect(&packet).await.unwrap();

    // Receive response
    let response = receive_from_kde_connect().await.unwrap();
    assert_eq!(response.packet_type, "cconnect.identity");
}
```

### Benchmarks

Create benchmarks in `benches/`:

```rust
use criterion::{criterion_group, criterion_main, Criterion};

fn benchmark_packet_serialization(c: &mut Criterion) {
    c.bench_function("packet serialize", |b| {
        let packet = NetworkPacket::new("test", 123);
        b.iter(|| packet.serialize());
    });
}

criterion_group!(benches, benchmark_packet_serialization);
criterion_main!(benches);
```

---

## ✅ Validation Criteria

### Phase 0 Completion Checklist

Before moving to Phase 1, verify:

- [ ] **Code Quality**
  - [ ] All Rust code compiles without warnings
  - [ ] `cargo clippy` passes with zero warnings
  - [ ] `cargo fmt` applied to all code
  - [ ] No `unsafe` code (or minimal with justification)

- [ ] **Testing**
  - [ ] All unit tests pass
  - [ ] All integration tests pass
  - [ ] Protocol compatibility tests pass
  - [ ] Code coverage > 80%

- [ ] **Functionality**
  - [ ] NetworkPacket serialization works (with `\n`)
  - [ ] Discovery service finds devices
  - [ ] TLS handshake works (both roles)
  - [ ] TLS role determination correct
  - [ ] Plugin system routes packets correctly

- [ ] **FFI**
  - [ ] uniffi-rs generates valid Kotlin bindings
  - [ ] Basic FFI calls work from Kotlin
  - [ ] Error handling across FFI works
  - [ ] Memory safety verified

- [ ] **COSMIC Desktop Validation**
  - [ ] COSMIC applet compiles with cosmic-connect-core
  - [ ] Device discovery works
  - [ ] TLS pairing works
  - [ ] All existing plugins work
  - [ ] No performance regressions

- [ ] **Documentation**
  - [ ] README.md complete
  - [ ] ARCHITECTURE.md complete
  - [ ] API documentation generated
  - [ ] docs/ffi-design.md created
  - [ ] Integration examples written

- [ ] **CI/CD**
  - [ ] GitHub Actions running tests
  - [ ] Linting in CI
  - [ ] Format checking in CI
  - [ ] Benchmarks tracked

---

## 🚨 Critical Requirements

### Must-Have Features

1. **Newline Termination**: NetworkPacket serialization MUST append `\n`
2. **TLS Role Logic**: MUST use lexicographic deviceId comparison
3. **UDP Discovery**: MUST use 224.0.0.251:1716
4. **Protocol Compatibility**: 100% KDE Connect protocol v7 compatible
5. **Memory Safety**: No memory leaks across FFI boundary

### Known Challenges

1. **Async Rust ↔ Kotlin FFI**:
   - uniffi-rs doesn't directly support async
   - Solution: Use callbacks or blocking wrappers
   - Document pattern in ffi-design.md

2. **Certificate Storage Abstraction**:
   - Rust core doesn't know about Android Keystore
   - Solution: Storage trait with platform implementations
   - Android provides Keystore implementation

3. **Error Handling Across FFI**:
   - Rust Result<T, E> doesn't map directly to Kotlin
   - Solution: uniffi error translation
   - Document patterns in ffi-design.md

---

## 📚 Resources

### Rust Libraries
- [tokio](https://tokio.rs/) - Async runtime
- [rustls](https://docs.rs/rustls/) - TLS implementation
- [rcgen](https://docs.rs/rcgen/) - Certificate generation
- [uniffi-rs](https://mozilla.github.io/uniffi-rs/) - FFI bindings
- [serde](https://serde.rs/) - Serialization

### KDE Connect Protocol
- [Protocol Specification](https://invent.kde.org/network/kdeconnect-meta)
- [KDE Connect Android](https://invent.kde.org/network/kdeconnect-android)
- Our `cosmicconnect-protocol-debug.md`

### FFI Resources
- [uniffi-rs Book](https://mozilla.github.io/uniffi-rs/)
- [cargo-ndk Guide](https://github.com/bbqsrc/cargo-ndk)
- Our `IMPLEMENTATION_GUIDE.md`

---

## 🎯 Next Steps After Phase 0

Once Phase 0 is complete:

1. **Phase 1: Android FFI Setup** (Weeks 4-5)
   - Issue #1: Dev environment with Rust Android targets
   - Issue #51: cargo-ndk integration in Gradle
   - Issue #52: Android FFI wrapper layer

2. **Phase 2: Core Modernization** (Weeks 6-9)
   - Issue #53: Integrate NetworkPacket FFI
   - Issue #54: Integrate TLS/Certificate FFI
   - Issue #55: Integrate Discovery FFI

3. **Continuous Integration**
   - cosmic-connect-core becomes dependency
   - Both Android and Desktop pull from core
   - Protocol bugs fixed once, everywhere

---

## 📝 Document History

- **2026-01-15**: Initial version created
- **Status**: Planning phase
- **Next Review**: After Issue #43 completion

---

**Ready to extract? Start with Issue #43! 🚀🦀**
