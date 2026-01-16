# COSMIC Applet Architecture Analysis - Issue #43

> **Created**: 2026-01-15
> **Issue**: #43 - Analyze COSMIC Applet for Protocol Extraction
> **Repository**: `/home/olafkfreund/Source/GitHub/cosmic-applet-kdeconnect`
> **Goal**: Map protocol implementation for extraction to cosmic-connect-core

---

## 📋 Executive Summary

The cosmic-applet-kdeconnect repository contains a complete KDE Connect protocol implementation in Rust, organized as a workspace with three main crates:

1. **kdeconnect-protocol** - Protocol implementation (THIS IS WHAT WE EXTRACT)
2. **kdeconnect-daemon** - Background service/daemon
3. **cosmic-applet-connect** - COSMIC Desktop UI applet

**Key Finding**: The `kdeconnect-protocol` crate is already well-structured and can be extracted almost as-is to become `cosmic-connect-core`.

---

## 🗂️ Repository Structure

```
cosmic-applet-kdeconnect/
├── Cargo.toml                    # Workspace root
├── kdeconnect-protocol/          # ⭐ EXTRACT THIS
│   ├── Cargo.toml
│   ├── src/
│   │   ├── lib.rs
│   │   ├── packet.rs             # NetworkPacket
│   │   ├── discovery/
│   │   │   ├── mod.rs
│   │   │   ├── service.rs        # Discovery service
│   │   │   └── events.rs
│   │   ├── transport/
│   │   │   ├── mod.rs
│   │   │   ├── tcp.rs
│   │   │   ├── tls.rs            # TLS connection
│   │   │   └── tls_config.rs     # TLS configuration
│   │   ├── connection/
│   │   │   ├── mod.rs
│   │   │   ├── manager.rs
│   │   │   └── events.rs
│   │   ├── pairing/
│   │   │   ├── mod.rs
│   │   │   ├── handler.rs
│   │   │   ├── service.rs
│   │   │   └── events.rs
│   │   ├── plugins/
│   │   │   ├── mod.rs
│   │   │   ├── ping.rs
│   │   │   ├── battery.rs
│   │   │   ├── share.rs
│   │   │   ├── clipboard.rs
│   │   │   ├── notification.rs
│   │   │   ├── mpris.rs
│   │   │   ├── findmyphone.rs
│   │   │   ├── runcommand.rs
│   │   │   ├── telephony.rs
│   │   │   ├── contacts.rs
│   │   │   ├── presenter.rs
│   │   │   └── remoteinput.rs
│   │   ├── device.rs             # Device info
│   │   ├── payload.rs            # Payload handling
│   │   └── error.rs              # Error types
│   └── tests/
│       └── integration_tests.rs
│
├── kdeconnect-daemon/            # STAYS (daemon/service)
│   └── ...
│
└── cosmic-applet-connect/        # STAYS (UI applet)
    └── ...
```

---

## 🔍 Component Analysis

### 1. **NetworkPacket (packet.rs)** ⭐ READY

**Location**: `kdeconnect-protocol/src/packet.rs` (442 lines)

**Status**: ✅ **Ready for extraction** - Well-implemented with comprehensive tests

**Features**:
- JSON serialization with `serde_json`
- **CRITICAL**: Newline termination (`\n`) - line 147
- Custom ID serialization (handles both string and number)
- Builder pattern for packet construction
- Payload support (payloadSize, payloadTransferInfo)
- Comprehensive test suite (190 lines of tests)

**Key Implementation**:
```rust
pub fn to_bytes(&self) -> Result<Vec<u8>> {
    let json = serde_json::to_string(self)?;
    let mut bytes = json.into_bytes();
    bytes.push(b'\n');  // ✅ Protocol requirement
    Ok(bytes)
}
```

**Dependencies**:
- `serde`, `serde_json` - Serialization
- `chrono` - Timestamps

**Extraction Plan**: Copy as-is, rename to `NetworkPacket`, adjust module paths.

---

### 2. **Discovery Service (discovery/)** ⭐ READY

**Location**: `kdeconnect-protocol/src/discovery/`

**Status**: ✅ **Ready for extraction** - Complete async implementation

**Files**:
- `service.rs` (498 lines) - Main discovery service
- `events.rs` - Discovery events (DeviceFound, DeviceLost)
- `mod.rs` - Module exports

**Features**:
- UDP broadcasting on port 1716
- Async tokio-based implementation
- Device timeout tracking
- Configurable broadcast interval (default: 5 seconds)
- Fallback port binding (1714-1764 range)

**Key Constants**:
```rust
pub const DISCOVERY_PORT: u16 = 1716;
pub const PORT_RANGE_START: u16 = 1714;
pub const PORT_RANGE_END: u16 = 1764;
pub const BROADCAST_ADDR: Ipv4Addr = Ipv4Addr::new(255, 255, 255, 255);
```

**⚠️ Note**: Uses **broadcast** (255.255.255.255), not **multicast** (224.0.0.251) as I originally documented. This is correct for KDE Connect.

**Dependencies**:
- `tokio` - Async runtime, channels
- Standard library UDP sockets
- `tracing` - Logging

**Extraction Plan**: Copy service.rs, adjust for uniffi compatibility (callbacks for events).

---

### 3. **TLS Transport (transport/)** ⚠️ NEEDS WORK

**Location**: `kdeconnect-protocol/src/transport/`

**Status**: ⚠️ **Needs modification** - Uses OpenSSL, need to switch to rustls

**Files**:
- `tls.rs` (300+ lines) - TLS connection handling
- `tls_config.rs` - TLS configuration with OpenSSL
- `tcp.rs` - TCP transport

**Current Implementation**:
```rust
use openssl::ssl::{Ssl, SslAcceptor};  // ⚠️ Uses OpenSSL
use tokio_openssl::SslStream;
```

**Why Change**:
- OpenSSL has C dependencies (hard for Android FFI)
- rustls is pure Rust (better for cross-compilation)
- rustls is more memory-safe
- Recommended in our docs

**Features**:
- TLS 1.0+ support (for Android compatibility)
- Certificate pinning
- Connection timeout handling
- Packet size limits (10MB max)

**TLS Role Determination**: ⚠️ **NOT FOUND YET**
- Need to find where deviceId comparison happens
- This is CRITICAL for proper TLS handshake

**Extraction Plan**:
1. Rewrite tls_config.rs to use rustls
2. Update tls.rs to use rustls APIs
3. Implement TLS role determination logic
4. Maintain compatibility with Android KDE Connect

**Dependencies to Change**:
- ❌ Remove: `openssl`, `tokio-openssl`
- ✅ Add: `rustls`, `tokio-rustls`, `rcgen` (certificate generation)

---

### 4. **Plugin System (plugins/)** ⭐ GOOD STRUCTURE

**Location**: `kdeconnect-protocol/src/plugins/`

**Status**: ✅ **Good structure** - 13 plugin implementations

**Files**: 13 plugin files (ping, battery, share, clipboard, etc.)

**Current Pattern** (ping.rs example):
```rust
pub struct PingPlugin {
    // Plugin state
}

impl PingPlugin {
    pub async fn handle_packet(&self, packet: &Packet) -> Result<()> {
        // Handle incoming packet
    }

    pub fn create_ping_packet() -> Packet {
        // Create outgoing packet
    }
}
```

**⚠️ Missing**: No unified `Plugin` trait - each plugin is separate

**Extraction Plan**:
1. Create `Plugin` trait:
   ```rust
   #[async_trait]
   pub trait Plugin: Send + Sync {
       fn name(&self) -> &str;
       fn incoming_capabilities(&self) -> &[&str];
       fn outgoing_capabilities(&self) -> &[&str];
       async fn handle_packet(&self, packet: &Packet) -> Result<()>;
   }
   ```
2. Create `PluginManager` for registration and routing
3. Refactor existing plugins to implement trait
4. Keep plugin implementations simple (protocol only, no UI)

---

### 5. **Connection Manager (connection/)** 🤔 EVALUATE

**Location**: `kdeconnect-protocol/src/connection/`

**Status**: 🤔 **Needs evaluation** - May be too high-level for core

**Files**:
- `manager.rs` - Connection lifecycle management
- `events.rs` - Connection events

**Features**:
- Connection state tracking
- Device connection/disconnection
- Event emission

**Decision Needed**:
- ❓ Extract to core? OR
- ✅ Keep in applet as higher-level logic?

**Recommendation**: **Extract to core** but keep it simple. Connection management is protocol-level.

---

### 6. **Pairing Service (pairing/)** ⭐ EXTRACT

**Location**: `kdeconnect-protocol/src/pairing/`

**Status**: ✅ **Extract** - Core protocol functionality

**Files**:
- `service.rs` - Pairing service
- `handler.rs` - Pairing request/response handling
- `events.rs` - Pairing events

**Features**:
- Pairing request/response
- Certificate exchange
- Pairing state management

**Extraction Plan**: Extract as-is, add FFI wrappers for callbacks.

---

### 7. **Device Info (device.rs)** ⭐ EXTRACT

**Location**: `kdeconnect-protocol/src/device.rs`

**Status**: ✅ **Extract** - Core data structure

**Features**:
- Device metadata (ID, name, type, version)
- Capability lists
- Device serialization

**Extraction Plan**: Copy as-is, ensure it works with uniffi.

---

### 8. **Error Handling (error.rs)** ⭐ EXTRACT

**Location**: `kdeconnect-protocol/src/error.rs`

**Status**: ✅ **Extract** - Uses `thiserror`

**Current Errors**:
```rust
#[derive(Debug, thiserror::Error)]
pub enum ProtocolError {
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    #[error("JSON error: {0}")]
    Json(#[from] serde_json::Error),

    #[error("Invalid packet: {0}")]
    InvalidPacket(String),

    #[error("TLS error: {0}")]
    Tls(String),

    // ... more
}
```

**Extraction Plan**:
1. Copy error types
2. Add `#[derive(uniffi::Error)]` for FFI
3. Ensure all errors are FFI-compatible

---

## 📦 Dependencies Analysis

### Current Dependencies (kdeconnect-protocol/Cargo.toml)

```toml
[dependencies]
tokio = { workspace = true }              # ✅ Keep
async-trait = { workspace = true }        # ✅ Keep
serde = { workspace = true }              # ✅ Keep
serde_json = { workspace = true }         # ✅ Keep
openssl = { workspace = true }            # ❌ Replace with rustls
tokio-openssl = { workspace = true }      # ❌ Replace with tokio-rustls
mdns-sd = { workspace = true }            # ❓ Evaluate (for mDNS discovery?)
thiserror = { workspace = true }          # ✅ Keep
tracing = { workspace = true }            # ✅ Keep
chrono = { workspace = true }             # ✅ Keep
uuid = { workspace = true }               # ✅ Keep (for device IDs?)
sha2 = { workspace = true }               # ✅ Keep (for hashing)
hex = { workspace = true }                # ✅ Keep
pem = "3.0"                               # ✅ Keep (certificate handling)
mouse-keyboard-input = { workspace = true } # ❌ Remove (desktop-specific)
```

### New Dependencies Needed for cosmic-connect-core

```toml
[dependencies]
# Existing (keep)
tokio = { version = "1.35", features = ["full"] }
async-trait = "0.1"
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
thiserror = "1.0"
tracing = "0.1"
chrono = "0.4"
uuid = "1.6"
sha2 = "0.10"
hex = "0.4"
pem = "3.0"

# New (for rustls)
rustls = "0.23"
tokio-rustls = "0.26"
rcgen = "0.13"             # Certificate generation
webpki-roots = "0.26"

# New (for FFI)
uniffi = "0.27"

# New (for async runtime in FFI)
once_cell = "1.19"

[build-dependencies]
uniffi = { version = "0.27", features = ["build"] }
```

---

## 🎯 Extraction Strategy

### Phase 1: Direct Copy (Week 1)

**What to extract as-is**:
1. ✅ `packet.rs` → `src/protocol/packet.rs`
2. ✅ `device.rs` → `src/protocol/device.rs`
3. ✅ `error.rs` → `src/error.rs`
4. ✅ `discovery/service.rs` → `src/network/discovery.rs`
5. ✅ `payload.rs` → `src/protocol/payload.rs`

**Changes needed**: Just module path adjustments

### Phase 2: Refactor (Week 2)

**What needs refactoring**:
1. ⚠️ TLS transport → Replace OpenSSL with rustls
2. ⚠️ Plugins → Add `Plugin` trait, create `PluginManager`
3. ⚠️ Connection → Simplify for core (remove desktop-specific logic)
4. ⚠️ Pairing → Ensure FFI-compatible

### Phase 3: FFI Layer (Week 2-3)

**Create new**:
1. 🆕 `src/ffi/mod.rs` - FFI exports
2. 🆕 `src/ffi/core.udl` - uniffi interface definition
3. 🆕 `build.rs` - uniffi build script

---

## ⚠️ Critical Findings

### 1. **TLS Role Determination - NOT FOUND**

The TLS role logic (lexicographic deviceId comparison) is not obviously present in the TLS code. Need to find where this happens:

```rust
// Expected logic (NOT FOUND YET):
pub fn determine_tls_role(my_id: &str, peer_id: &str) -> TlsRole {
    if my_id > peer_id {
        TlsRole::Server  // Larger deviceId is server
    } else {
        TlsRole::Client
    }
}
```

**Action**: Search deeper in connection/manager.rs or pairing code.

### 2. **Discovery Uses Broadcast, Not Multicast**

Original documentation said multicast (224.0.0.251), but code uses broadcast (255.255.255.255):

```rust
pub const BROADCAST_ADDR: Ipv4Addr = Ipv4Addr::new(255, 255, 255, 255);
```

**Action**: Verify which is correct for KDE Connect protocol. Update documentation if needed.

### 3. **OpenSSL Dependency**

Using OpenSSL adds complexity for Android cross-compilation:
- C library dependency
- Platform-specific builds
- Certificate format issues

**Action**: Priority task to replace with rustls in extraction.

### 4. **No Certificate Generation Code**

Don't see RSA certificate generation code. May be in separate crate or using external tool.

**Action**: Need to implement with `rcgen` crate.

---

## 📊 Extraction Checklist

### Week 1: Core Protocol
- [ ] Copy `packet.rs` → Test serialization with `\n`
- [ ] Copy `device.rs` → Test FFI compatibility
- [ ] Copy `error.rs` → Add uniffi::Error derive
- [ ] Copy `discovery/` → Test UDP broadcasting
- [ ] Set up Cargo.toml with correct dependencies

### Week 2: Transport & Plugins
- [ ] Rewrite TLS with rustls
- [ ] Implement TLS role determination
- [ ] Implement certificate generation (rcgen)
- [ ] Create Plugin trait
- [ ] Create PluginManager
- [ ] Refactor 2-3 example plugins

### Week 3: FFI & Validation
- [ ] Create uniffi .udl interface
- [ ] Generate Kotlin bindings
- [ ] Test FFI calls from Kotlin
- [ ] Validate with COSMIC Desktop
- [ ] Run integration tests

---

## 🚧 Blockers & Risks

### Blockers

1. **TLS Role Logic Missing**: Need to find or implement this CRITICAL logic
2. **Certificate Generation**: Need to implement RSA 2048-bit cert generation

### Risks

1. **OpenSSL → rustls Migration**: May break compatibility if not careful
2. **Android TLS 1.0**: rustls may not support TLS 1.0 (needed for old Android)
3. **Plugin Trait Design**: Need to balance simplicity with flexibility
4. **FFI Async**: uniffi doesn't support async - need callback pattern

### Mitigations

1. Research TLS 1.0 support in rustls or use compatibility layer
2. Test certificate compatibility with Android early
3. Keep plugin trait simple - just packet routing
4. Use callback pattern for all async operations in FFI

---

## 📝 Next Steps for Issue #44

Once this analysis is complete:

1. **Create cosmic-connect-core repository**
   ```bash
   cargo new --lib cosmic-connect-core
   cd cosmic-connect-core
   ```

2. **Set up module structure**:
   ```
   src/
   ├── lib.rs
   ├── protocol/
   │   ├── mod.rs
   │   ├── packet.rs
   │   ├── device.rs
   │   └── payload.rs
   ├── network/
   │   ├── mod.rs
   │   └── discovery.rs
   ├── crypto/
   │   ├── mod.rs
   │   ├── certificate.rs
   │   └── tls.rs
   ├── plugins/
   │   ├── mod.rs
   │   ├── plugin.rs
   │   └── manager.rs
   ├── ffi/
   │   ├── mod.rs
   │   └── core.udl
   └── error.rs
   ```

3. **Start with packet.rs** (Issue #45)

---

## 📚 References

- **COSMIC Applet**: `/home/olafkfreund/Source/GitHub/cosmic-applet-kdeconnect`
- **Protocol Docs**: `kdeconnect-protocol.md` in applet repo
- **KDE Connect Spec**: https://invent.kde.org/network/kdeconnect-meta

---

**Status**: ✅ Analysis Complete - Ready for Issue #44

**Next**: Create cosmic-connect-core Cargo project
