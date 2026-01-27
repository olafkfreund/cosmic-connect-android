# COSMIC Connect Modernization - Updated Project Plan
## Rust + Kotlin Hybrid Architecture

## 📋 Executive Summary

**Project:** COSMIC Connect Android Modernization with Shared Rust Core  
**Timeline:** 16-20 weeks  
**Team Size:** 1-3 developers  
**Target:** Modernize COSMIC Connect Android app with shared Rust protocol implementation for seamless COSMIC Desktop integration

**Key Innovation:** Extract KDE Connect protocol implementation from COSMIC Desktop applet into a shared Rust library used by both desktop and Android.

**Key Metrics:**
- Create shared `cosmic-connect-core` Rust library
- 150+ Java files to convert to Kotlin
- Target: 80%+ test coverage
- Goal: Full protocol compatibility with COSMIC Desktop
- Zero breaking changes for existing users
- Single source of truth for protocol logic

---

## 🎯 Architectural Overview

```
cosmic-connect-ecosystem/
├── cosmic-connect-core/          # NEW: Shared Rust Library
│   ├── src/
│   │   ├── protocol/            # KDE Connect protocol
│   │   ├── network/             # Device discovery, packets
│   │   ├── crypto/              # TLS, certificates
│   │   ├── plugins/             # Core plugin logic
│   │   └── ffi/                 # FFI interfaces
│   └── Cargo.toml
│
├── cosmic-connect-android/       # THIS REPO (Updated)
│   ├── rust-ffi/                # NEW: JNI bindings
│   │   ├── uniffi.toml          # uniffi-rs configuration
│   │   └── src/lib.rs           # FFI wrapper
│   ├── src/org/kde/...          # Kotlin UI & Android services
│   └── build.gradle.kts         # Updated with cargo-ndk
│
└── cosmic-applet-kdeconnect/    # EXISTING (Refactored)
    ├── Uses cosmic-connect-core
    └── COSMIC Desktop UI only
```

**Key Benefits:**
- **Single Protocol Implementation**: One Rust codebase for protocol logic
- **Memory Safety**: Rust guarantees for critical networking code
- **Code Reuse**: Share 70%+ of logic between desktop and mobile
- **Easier Debugging**: Fix protocol bugs once, benefit everywhere
- **Better Testing**: Test protocol once at the core level
- **Modern Android**: Keep Kotlin for UI and Android integrations

---

## 📊 Updated Phase Structure

```
Phase 0: Rust Core Extraction (NEW - Weeks 1-3)
├─ Extract protocol from desktop applet
├─ Create cosmic-connect-core library
├─ Set up FFI interfaces
└─ Validate with desktop applet

Phase 1: Foundation & Setup (Weeks 4-5)
├─ Environment setup
├─ Codebase audit
├─ Android FFI integration
└─ Protocol validation

Phase 2: Core Modernization (Weeks 6-9)
├─ Integrate Rust core with Android
├─ Java to Kotlin conversion
├─ Android-specific services
└─ Testing infrastructure

Phase 3: Feature Implementation (Weeks 10-14)
├─ Plugin integration (Rust core + Kotlin UI)
├─ UI modernization
├─ Enhanced testing
└─ Performance optimization

Phase 4: Integration & Testing (Weeks 15-16)
├─ Cross-platform testing
├─ Bug fixes
├─ Documentation
└─ Release preparation

Phase 5: Release & Maintenance (Weeks 17+)
├─ Beta testing
├─ Final polish
├─ Release
└─ Post-release support
```

---

## 📊 Phase 0: Rust Core Extraction (NEW - Weeks 1-3)

### Goal
Extract and modularize KDE Connect protocol implementation from COSMIC Desktop applet into a shared Rust library.

### Epic: Create Shared Protocol Library

#### Issue #43: Analyze COSMIC Applet for Protocol Extraction
**Priority:** P0-Critical  
**Labels:** `rust`, `architecture`, `protocol`, `P0-Critical`  
**Estimated Effort:** 4 hours

**Description:**
Analyze the existing COSMIC Desktop applet to identify all protocol-related code for extraction.

**Tasks:**
- [ ] Clone cosmic-applet-kdeconnect repository
- [ ] Map all protocol-related modules
- [ ] Identify NetworkPacket implementation
- [ ] Identify device discovery code
- [ ] Identify TLS/certificate handling
- [ ] Identify plugin core logic
- [ ] Document dependencies
- [ ] Create extraction plan

**Deliverables:**
- `docs/rust-extraction-plan.md`
- Module dependency diagram
- List of code to extract
- FFI interface requirements

**Success Criteria:**
- Complete understanding of protocol code
- Clear extraction boundaries
- Documented plan

**Dependencies:** None

**Related Documentation:**
- cosmic-applet-kdeconnect source
- `kdeconnect-protocol-debug.md`

---

#### Issue #44: Create cosmic-connect-core Cargo Project
**Priority:** P0-Critical  
**Labels:** `rust`, `infrastructure`, `P0-Critical`  
**Estimated Effort:** 3 hours

**Description:**
Create the new shared Rust library project with proper structure.

**Tasks:**
- [x] Initialize new Cargo workspace ✅
- [x] Set up library crate structure ✅
- [x] Configure Cargo.toml with dependencies ✅
- [x] Set up module structure (protocol, network, crypto, plugins) ✅
- [x] Add uniffi-rs support ✅
- [x] Configure for multi-platform (Android + Linux) ✅
- [ ] Set up CI/CD for the library (Deferred to Issue #51)
- [x] Add comprehensive README ✅

**Status:** ✅ COMPLETED (commit f226ba3, tag v0.1.0-alpha)
**Repository:** `/home/olafkfreund/Source/GitHub/cosmic-connect-core`

**Implementation Requirements:**
```toml
[package]
name = "cosmic-connect-core"
version = "0.1.0"
edition = "2021"

[dependencies]
tokio = { version = "1.0", features = ["full"] }
rustls = "0.21"
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
uniffi = "0.25"

[lib]
crate-type = ["lib", "staticlib", "cdylib"]
```

**Success Criteria:**
- Project compiles
- Structure matches design
- CI/CD configured

**Dependencies:** #43

---

#### Issue #45: Extract NetworkPacket to Rust Core
**Priority:** P0-Critical  
**Labels:** `rust`, `protocol`, `P0-Critical`  
**Estimated Effort:** 5 hours

**Description:**
Extract NetworkPacket implementation from desktop applet to shared core.

**Tasks:**
- [ ] Create `protocol/packet.rs`
- [ ] Implement packet serialization/deserialization
- [ ] Add packet type enums (Identity, Pair, etc.)
- [ ] Implement validation logic
- [ ] **CRITICAL**: Ensure newline termination (\\n)
- [ ] Add comprehensive tests
- [ ] Document packet format
- [ ] Add FFI bindings

**Implementation Requirements:**
```rust
pub struct NetworkPacket {
    pub packet_type: String,
    pub id: i64,
    pub body: serde_json::Value,
    // ... other fields
}

impl NetworkPacket {
    pub fn serialize(&self) -> Result<Vec<u8>>;
    pub fn deserialize(data: &[u8]) -> Result<Self>;
    pub fn validate(&self) -> Result<()>;
}
```

**Success Criteria:**
- All packet types supported
- Protocol compatible
- Tests pass
- FFI works

**Dependencies:** #44

---

#### Issue #46: Extract Device Discovery to Rust Core
**Priority:** P0-Critical  
**Labels:** `rust`, `protocol`, `networking`, `P0-Critical`  
**Estimated Effort:** 6 hours

**Description:**
Extract UDP multicast discovery implementation to shared core.

**Tasks:**
- [ ] Create `network/discovery.rs`
- [ ] Implement multicast listener
- [ ] Implement broadcast scheduler
- [ ] Add device detection logic
- [ ] Handle identity packets
- [ ] Add network state management
- [ ] Write comprehensive tests
- [ ] Add FFI bindings

**Implementation Requirements:**
- Multicast group: 224.0.0.251
- Port: 1716
- Async with tokio
- Cross-platform support

**Success Criteria:**
- Discovery works on Linux
- Discovery works on Android
- Protocol compatible
- Tests pass

**Dependencies:** #45

---

#### Issue #47: Extract TLS/Certificate Management to Rust Core
**Priority:** P0-Critical  
**Labels:** `rust`, `security`, `tls`, `P0-Critical`  
**Estimated Effort:** 8 hours

**Description:**
Extract TLS and certificate management to shared core.

**Tasks:**
- [ ] Create `crypto/certificate.rs`
- [ ] Create `crypto/tls.rs`
- [ ] Implement certificate generation
- [ ] Implement certificate validation
- [ ] Implement TLS context creation
- [ ] Add certificate pinning
- [ ] **CRITICAL**: Implement TLS role logic (deviceId comparison)
- [ ] Add connection manager
- [ ] Write security tests
- [ ] Add FFI bindings

**Implementation Requirements:**
- RSA 2048-bit keys
- 10-year validity
- SHA-256 fingerprints
- TLS 1.2+ support
- Certificate storage abstraction (platform-specific)

**Success Criteria:**
- Secure implementation
- Protocol compatible
- Works on both platforms
- Tests pass

**Dependencies:** #45

---

#### Issue #48: Extract Plugin Core Logic to Rust Core
**Priority:** P1-High  
**Labels:** `rust`, `plugins`, `P1-High`  
**Estimated Effort:** 6 hours

**Description:**
Extract core plugin management and base logic to shared library.

**Tasks:**
- [ ] Create `plugins/mod.rs`
- [ ] Define Plugin trait
- [ ] Implement PluginManager
- [ ] Add packet routing
- [ ] Create plugin base implementations
- [ ] Add lifecycle management
- [ ] Write tests
- [ ] Add FFI bindings

**Implementation Requirements:**
```rust
pub trait Plugin {
    fn name(&self) -> &str;
    fn handle_packet(&mut self, packet: NetworkPacket) -> Result<()>;
    fn initialize(&mut self) -> Result<()>;
    fn shutdown(&mut self) -> Result<()>;
}

pub struct PluginManager {
    plugins: HashMap<String, Box<dyn Plugin>>,
}
```

**Success Criteria:**
- Clean plugin API
- Extensible design
- Works on both platforms
- Tests pass

**Dependencies:** #45

---

#### Issue #49: Set Up uniffi-rs FFI Generation
**Priority:** P0-Critical  
**Labels:** `rust`, `ffi`, `android`, `P0-Critical`  
**Estimated Effort:** 5 hours

**Description:**
Configure uniffi-rs for automatic FFI binding generation.

**Tasks:**
- [ ] Create `src/ffi/mod.rs`
- [ ] Define uniffi interfaces
- [ ] Configure Kotlin codegen
- [ ] Test generated bindings
- [ ] Document FFI API
- [ ] Add examples
- [ ] Set up build integration

**Implementation Requirements:**
```rust
// src/ffi/mod.rs
uniffi::include_scaffolding!("cosmic_connect_core");

#[uniffi::export]
pub fn create_network_packet(
    packet_type: String,
    id: i64,
    body: String,
) -> Arc<NetworkPacket> {
    // Implementation
}
```

**Success Criteria:**
- Bindings generate correctly
- Kotlin can call Rust
- Types map correctly
- Examples work

**Dependencies:** #45, #46, #47, #48

---

#### Issue #50: Validate Rust Core with COSMIC Desktop
**Priority:** P0-Critical  
**Labels:** `rust`, `testing`, `integration`, `P0-Critical`  
**Estimated Effort:** 6 hours

**Description:**
Refactor COSMIC Desktop applet to use new shared core and validate functionality.

**Tasks:**
- [ ] Update cosmic-applet-kdeconnect Cargo.toml
- [ ] Replace internal protocol code with core library
- [ ] Update all call sites
- [ ] Test discovery
- [ ] Test pairing
- [ ] Test all plugins
- [ ] Verify no regressions
- [ ] Update documentation

**Success Criteria:**
- Desktop applet works perfectly
- All features functional
- No performance regressions
- Code is cleaner

**Dependencies:** #49

---

## 📊 Phase 1: Foundation & Setup (Updated - Weeks 4-5)

### Epic: Android Integration Setup

#### Issue #51: Set Up cargo-ndk in Android Build
**Priority:** P0-Critical  
**Labels:** `android`, `rust`, `build-system`, `P0-Critical`  
**Estimated Effort:** 4 hours

**Description:**
Integrate Rust library building into Android Gradle build.

**Tasks:**
- [ ] Add cargo-ndk plugin to build.gradle.kts
- [ ] Configure Android NDK paths
- [ ] Set up target architectures (arm64-v8a, armeabi-v7a, x86_64)
- [ ] Configure Rust compilation in Gradle
- [ ] Add JNI library loading
- [ ] Test build on all architectures
- [ ] Document build process

**Implementation Requirements:**
```kotlin
// build.gradle.kts
plugins {
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.3"
}

cargo {
    module = "../cosmic-connect-core"
    libname = "cosmic_connect_core"
    targets = listOf("arm64", "arm", "x86_64")
}
```

**Success Criteria:**
- Rust builds on Android
- All architectures supported
- CI/CD integration works

**Dependencies:** #50, #1

**Related Skills:** gradle-SKILL.md

---

#### Issue #52: Create Android FFI Wrapper Layer
**Priority:** P0-Critical  
**Labels:** `android`, `kotlin`, `rust`, `P0-Critical`  
**Estimated Effort:** 6 hours

**Description:**
Create Kotlin wrapper layer around Rust FFI for idiomatic Android usage.

**Tasks:**
- [ ] Create CosmicConnectCore singleton
- [ ] Wrap NetworkPacket in Kotlin
- [ ] Wrap Discovery in Kotlin
- [ ] Wrap TLS/Certificate in Kotlin
- [ ] Wrap PluginManager in Kotlin
- [ ] Add lifecycle management
- [ ] Add error handling
- [ ] Write tests

**Implementation Requirements:**
```kotlin
object CosmicConnectCore {
    init {
        System.loadLibrary("cosmic_connect_core")
    }
    
    fun createNetworkPacket(type: String, id: Long, body: String): NetworkPacket {
        return NetworkPacket(createNetworkPacketNative(type, id, body))
    }
}

class NetworkPacket(private val handle: Long) {
    fun serialize(): ByteArray
    fun getType(): String
    // ... other methods
}
```

**Success Criteria:**
- Clean Kotlin API
- Type-safe
- Memory safe
- Tests pass

**Dependencies:** #51

**Related Skills:** android-development-SKILL.md

---

#### Issue #1: Development Environment Setup (Updated)
**Priority:** P0-Critical  
**Labels:** `setup`, `infrastructure`, `rust`, `P0-Critical`  
**Estimated Effort:** 3 hours

**Description:**
Set up complete development environment for Android + Rust development.

**Tasks:**
- [ ] Install Android Studio (latest stable)
- [ ] Install Rust toolchain (1.70+)
- [ ] Install cargo-ndk
- [ ] Install uniffi-bindgen
- [ ] Add Android targets: rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
- [ ] Install NixOS development tools (if using NixOS)
- [ ] Clone all repositories
- [ ] Build cosmic-connect-core
- [ ] Build Android app successfully
- [ ] Build COSMIC applet successfully
- [ ] Set up emulator/test device
- [ ] Configure git hooks

**Success Criteria:**
- All projects build without errors
- Rust compiles for Android
- Tests run successfully
- Git hooks working

**Dependencies:** #50

---

#### Issue #2: Codebase Audit - Android (Same as before)
**Priority:** P0-Critical  
**Labels:** `audit`, `android`, `documentation`, `P0-Critical`  
**Estimated Effort:** 4 hours

[Content remains the same as original]

**Dependencies:** #52

---

#### Issue #3: Test Existing COSMIC Desktop Applet (Updated from original #3)
**Priority:** P0-Critical  
**Labels:** `audit`, `cosmic`, `documentation`, `P0-Critical`  
**Estimated Effort:** 3 hours

[Content remains the same as original]

**Dependencies:** #50

---

#### Issue #4: Protocol Compatibility Testing (Updated)
**Priority:** P0-Critical  
**Labels:** `protocol`, `testing`, `rust`, `P0-Critical`  
**Estimated Effort:** 5 hours

**Description:**
Test Rust core protocol compatibility with Android integration.

**Tasks:**
- [ ] Test NetworkPacket FFI
- [ ] Test Discovery FFI
- [ ] Test TLS/Certificate FFI
- [ ] Test Plugin FFI
- [ ] Verify protocol compatibility
- [ ] Test memory management
- [ ] Test error handling
- [ ] Document findings

**Success Criteria:**
- FFI works correctly
- Protocol compatible
- No memory leaks
- Tests pass

**Dependencies:** #52, #3

---

#### Issue #5: Create Project Board (Same as before)

[Content remains the same as original]

**Dependencies:** #2, #3, #4

---

## 📊 Phase 2: Core Modernization (Updated - Weeks 6-9)

### Note on Issues #6-#8 (Build System)
These remain largely the same but need updates for Rust integration.

#### Issue #6: Convert Root build.gradle to Kotlin DSL
[Keep original content, build succeeds with Rust]

#### Issue #7: Convert App build.gradle to Kotlin DSL (Updated)
[Original content PLUS:]

**Additional Tasks:**
- [ ] Add cargo configuration
- [ ] Configure rust-android plugin
- [ ] Set up JNI library loading

**Dependencies:** #6, #51

#### Issue #8: Set Up Version Catalog
[Keep original content]

---

### Issues #9-#10: NetworkPacket (REPLACED by Rust Core)

**Note:** NetworkPacket is now implemented in Rust core. These issues are replaced by:

#### Issue #53: Integrate NetworkPacket FFI in Android
**Priority:** P0-Critical  
**Labels:** `android`, `kotlin`, `ffi`, `P0-Critical`  
**Estimated Effort:** 4 hours

**Description:**
Replace Java NetworkPacket with Rust core via FFI.

**Tasks:**
- [ ] Remove existing Java NetworkPacket
- [ ] Use Kotlin FFI wrapper
- [ ] Update all call sites
- [ ] Add proper error handling
- [ ] Write integration tests
- [ ] Verify protocol compatibility
- [ ] Document usage

**Success Criteria:**
- All code uses Rust NetworkPacket
- Tests pass
- Protocol compatible

**Dependencies:** #52, #8

---

### Issues #11-#12: Device/DeviceManager (Updated)

#### Issue #11: Convert Device Class to Kotlin (Updated)
**Priority:** P1-High  
**Labels:** `android`, `kotlin-conversion`, `P1-High`  
**Estimated Effort:** 4 hours

**Description:**
Convert Device class to Kotlin, delegating protocol logic to Rust core.

**Tasks:**
- [ ] Convert to Kotlin
- [ ] Use Rust core for protocol operations
- [ ] Keep Android-specific logic (UI state, lifecycle)
- [ ] Add state management with sealed classes
- [ ] Implement coroutines for async ops
- [ ] Write unit tests
- [ ] Update documentation

**Success Criteria:**
- Clean separation: Rust for protocol, Kotlin for Android
- Tests pass
- More maintainable

**Dependencies:** #53

---

#### Issue #12: Convert DeviceManager to Kotlin (Updated)
**Priority:** P1-High  
**Labels:** `android`, `kotlin-conversion`, `architecture`, `P1-High`  
**Estimated Effort:** 5 hours

**Description:**
Convert DeviceManager to Kotlin with Repository pattern, using Rust core.

**Tasks:**
- [ ] Convert to Kotlin
- [ ] Use Rust PluginManager via FFI
- [ ] Implement Repository interface
- [ ] Use StateFlow for device list
- [ ] Add coroutines for operations
- [ ] Write tests

**Success Criteria:**
- Repository pattern implemented
- Uses Rust core
- Tests pass

**Dependencies:** #11

---

### Issues #13-#14: TLS/Certificates (REPLACED by Rust Core)

**Note:** TLS and certificate management are now in Rust core. These issues become:

#### Issue #54: Integrate TLS/Certificate FFI in Android
**Priority:** P0-Critical  
**Labels:** `android`, `kotlin`, `ffi`, `security`, `P0-Critical`  
**Estimated Effort:** 5 hours

**Description:**
Replace Android certificate management with Rust core via FFI, using Android Keystore for storage.

**Tasks:**
- [ ] Remove existing Java CertificateManager
- [ ] Use Rust crypto via FFI
- [ ] Implement Android Keystore storage backend
- [ ] Create Kotlin wrapper for certificate operations
- [ ] Handle certificate storage in Android Keystore
- [ ] Write security tests
- [ ] Document security model

**Implementation:**
```kotlin
class AndroidCertificateStorage : CertificateStorage {
    private val keyStore = AndroidKeyStore()
    
    override fun saveCertificate(deviceId: String, cert: ByteArray) {
        keyStore.setCertificateEntry("kdeconnect_$deviceId", cert)
    }
    
    override fun loadCertificate(deviceId: String): ByteArray? {
        return keyStore.getCertificate("kdeconnect_$deviceId")?.encoded
    }
}
```

**Success Criteria:**
- Rust core handles crypto
- Android Keystore provides storage
- Secure implementation
- Tests pass

**Dependencies:** #52, #12

---

### Issue #15: Modernize Discovery Service (Updated)

#### Issue #55: Integrate Discovery FFI in Android
**Priority:** P1-High  
**Labels:** `android`, `kotlin`, `ffi`, `networking`, `P1-High`  
**Estimated Effort:** 5 hours

**Description:**
Integrate Rust core discovery with Android services.

**Tasks:**
- [ ] Create Android DiscoveryService
- [ ] Use Rust discovery via FFI
- [ ] Handle Android-specific networking
- [ ] Manage wake locks
- [ ] Handle network changes
- [ ] Add proper lifecycle
- [ ] Write tests

**Success Criteria:**
- Discovery uses Rust core
- Android integration proper
- Tests pass

**Dependencies:** #54, original #15 concepts

---

### Issue #16: Test Discovery with COSMIC Desktop (Updated)

[Keep same goals, but test Rust core on both platforms]

**Dependencies:** #55

---

## 📊 Phase 3: Feature Implementation (Updated - Weeks 10-14)

### Epic: Plugin Integration

**Note:** Plugin core logic is in Rust. Android provides:
1. UI for plugin controls
2. Android system integration (notifications, clipboard, etc.)
3. FFI bridges to Rust plugin core

#### Issue #17: Create Plugin Architecture Bridge (Updated)
**Priority:** P1-High  
**Labels:** `android`, `kotlin`, `plugins`, `rust`, `P1-High`  
**Estimated Effort:** 5 hours

**Description:**
Create architecture for integrating Rust plugin core with Android UI/services.

**Tasks:**
- [ ] Design Plugin FFI bridge
- [ ] Create Android Plugin base class
- [ ] Implement Rust-to-Android event flow
- [ ] Implement Android-to-Rust command flow
- [ ] Add plugin lifecycle
- [ ] Create plugin testing base
- [ ] Document architecture

**Implementation:**
```kotlin
abstract class AndroidPlugin(
    private val rustPlugin: RustPluginHandle
) {
    // Bridge between Rust core and Android UI/services
    abstract fun provideUI(): Composable
    abstract fun handleAndroidSpecifics()
    
    fun handlePacket(packet: NetworkPacket) {
        rustPlugin.handlePacket(packet) // Delegates to Rust
    }
}
```

**Success Criteria:**
- Clean bridge architecture
- Easy to implement plugins
- Well documented

**Dependencies:** #12, #54

---

#### Issue #56: Implement Battery Plugin with Rust Core
**Priority:** P1-High  
**Labels:** `android`, `kotlin`, `plugins`, `rust`, `P1-High`  
**Estimated Effort:** 3 hours

**Description:**
Implement battery plugin using Rust core for protocol, Kotlin for Android battery API.

**Tasks:**
- [ ] Use Rust battery plugin core via FFI
- [ ] Read Android battery status
- [ ] Send updates through Rust core
- [ ] Receive updates from Rust core
- [ ] Create simple UI
- [ ] Write tests
- [ ] Test with COSMIC Desktop

**Success Criteria:**
- Plugin works both directions
- Uses Rust core
- COSMIC Desktop compatible

**Dependencies:** #17

---

#### Issue #57: Implement Share Plugin with Rust Core
**Priority:** P1-High  
**Labels:** `android`, `kotlin`, `plugins`, `rust`, `P1-High`  
**Estimated Effort:** 5 hours

**Description:**
Implement file sharing using Rust core for transfer protocol.

**Tasks:**
- [ ] Use Rust share plugin via FFI
- [ ] Handle Android file URIs
- [ ] Implement Android share sheet
- [ ] Stream files through Rust core
- [ ] Show progress UI
- [ ] Write tests

**Success Criteria:**
- File sharing works
- Large files supported
- Progress tracking

**Dependencies:** #17, #54

---

#### Issues #58-#61: Other Plugins
[Similar pattern for Clipboard (#58), Notifications (#59), RunCommand (#60), FindMyPhone (#61)]

Each follows the pattern:
- Rust core for protocol logic
- Kotlin for Android system integration
- FFI bridge between them

---

### Epic: UI Modernization
[Issues #24-#27 remain the same - pure Kotlin/Compose UI]

---

## 📊 Phase 4: Integration & Testing (Weeks 15-16)

### Epic: Comprehensive Testing

[Issues #28-#34 remain similar, but with additional focus on FFI testing]

#### Issue #62: FFI Integration Testing
**Priority:** P0-Critical  
**Labels:** `testing`, `rust`, `ffi`, `P0-Critical`  
**Estimated Effort:** 5 hours

**Description:**
Comprehensive FFI testing for memory safety and correctness.

**Tasks:**
- [ ] Test memory management across FFI boundary
- [ ] Test error propagation
- [ ] Test concurrent access
- [ ] Test large data transfers
- [ ] Run under valgrind/ASAN
- [ ] Document findings

**Success Criteria:**
- No memory leaks
- No crashes
- Thread-safe

**Dependencies:** All plugin issues

---

## 📊 Phase 5: Release & Maintenance (Weeks 17+)

[Issues #35-#40 remain the same]

---

## 🎯 Updated Critical Path

```
Phase 0 (Rust Core):
#43 → #44 → #45 → #46 → #47 → #48 → #49 → #50

Phase 1 (Integration):
#51 → #52 → #1 → #2, #3 → #4 → #5

Phase 2 (Core Android):
#6 → #7 → #8 → #53 → #11 → #12 → #54 → #55 → #16

Phase 3 (Plugins):
#17 → #56-#61 (Plugins) + #24-#27 (UI)

Phase 4 (Testing):
#28-#34 + #62

Phase 5 (Release):
#35-#40
```

---

## 📈 Key Advantages of Rust + Kotlin Architecture

### Code Sharing
- **70%+ protocol logic shared** between desktop and Android
- Single source of truth for KDE Connect protocol
- Fix bugs once, benefit both platforms

### Memory Safety
- Rust guarantees for networking and crypto code
- No segfaults or memory leaks in protocol layer
- Safe concurrency

### Performance
- Native performance for protocol operations
- Efficient memory usage
- Fast file transfers

### Maintainability
- Clear separation of concerns
- Protocol in Rust, UI/Android in Kotlin
- Each platform uses best-suited language

### Testing
- Test protocol once in Rust
- Platform-specific tests only for UI/integration
- Higher confidence in correctness

---

## 🚀 Getting Started (Updated)

```bash
# 1. Set up Rust for Android
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android
cargo install cargo-ndk
cargo install uniffi-bindgen

# 2. Clone repositories
git clone https://github.com/olafkfreund/cosmic-connect-core.git      # NEW
git clone https://github.com/olafkfreund/cosmic-connect-android.git
git clone https://github.com/olafkfreund/cosmic-applet-cconnect.git

# 3. Build Rust core
cd cosmic-connect-core
cargo build --release
cargo test

# 4. Build COSMIC applet with new core
cd ../cosmic-applet-kdeconnect
# Update Cargo.toml to use cosmic-connect-core
cargo build --release

# 5. Build Android app
cd ../cosmic-connect-android
./gradlew build

# 6. Start development
claude-code "Read PROJECT_PLAN_UPDATED.md and help me start Phase 0"
```

---

## 📞 Using Claude Code

```bash
# Help with Rust extraction
claude-code "Read PROJECT_PLAN_UPDATED.md and help me extract protocol code from the COSMIC applet"

# Help with FFI
claude-code "Help me set up uniffi-rs bindings for the NetworkPacket struct"

# Help with Android integration
claude-code "Show me how to integrate the Rust core into the Android build system"
```

---

## ✅ Updated Definition of Done

An issue is considered "Done" when:

**Implementation:**
- [ ] Code complete and compiles (Rust + Kotlin)
- [ ] All tests pass (Rust + Kotlin + FFI)
- [ ] Code reviewed and approved
- [ ] Documentation updated

**FFI Requirements (for FFI-related issues):**
- [ ] No memory leaks (tested with valgrind/ASAN)
- [ ] Error handling across boundary works
- [ ] Thread-safe if needed
- [ ] Performance acceptable

**Cross-Platform:**
- [ ] Works on Android
- [ ] Works on COSMIC Desktop
- [ ] Protocol compatible

[Rest remains the same as original]

---

## 🔮 Future Optimizations

### Issue #63: Optimal Discovery and Pairing Architecture
**Priority:** P2-Medium
**Labels:** `enhancement`, `rust`, `android`, `performance`, `architecture`
**Timeline:** Q2 2026 (8 weeks after Issue #50)

**Related:** [cosmic-applet-kdeconnect#53](https://github.com/olafkfreund/cosmic-applet-kdeconnect/issues/53) - Full architectural proposal

**Overview:**
Major optimization of device discovery and pairing using modern cryptography and smart discovery strategies.

**Problems Addressed:**
- ❌ Battery drain (5-10% hourly)
- ❌ Slow discovery (5-10 seconds)
- ❌ Pairing race conditions
- ❌ RSA-2048 performance (500ms key generation)
- ❌ Poor network change handling

**Key Features:**
- **3-Layer Discovery:** Known IPs → mDNS/DNS-SD → UDP broadcast
- **7-State Pairing Machine:** Prevents race conditions
- **Modern Cryptography:** Ed25519 (5000x faster), X25519, ChaCha20-Poly1305

**Performance Targets:**

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| Discovery | 5-10s | <3s | 2x faster |
| Key generation | 500ms | <10ms | 50x faster |
| Pairing time | 10-30s | <5s | 3-6x faster |
| Battery drain | 5-10%/hr | <2%/hr | 3-5x better |

**Implementation:**
- Weeks 1-2: Core infrastructure (DiscoveryManager, state machine)
- Weeks 3-4: Discovery methods (3 layers)
- Weeks 5-6: Pairing system (modern crypto)
- Weeks 7-8: Android integration & testing

**Dependencies:**
- [x] Issue #44: cosmic-connect-core ✅
- [ ] Issues #45-50: Core protocol implementation

**Status:** Planned (requires cosmic-connect-core foundation)

**Documentation:** See `docs/optimization-roadmap.md` for complete details

---

## 🎉 Summary

This updated plan transforms COSMIC Connect into a truly cross-platform solution with:

✅ **Shared Rust core** for protocol logic  
✅ **Modern Kotlin** for Android UI and services  
✅ **Single source of truth** for KDE Connect protocol  
✅ **Memory safety** where it matters most  
✅ **Easy maintenance** through clear separation of concerns  

**Let's build something incredible!** 🚀
