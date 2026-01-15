# COSMIC Connect Android - Getting Started Guide

## ⚠️ IMPORTANT: Project Scope

**This project uses a hybrid Rust + Kotlin architecture!**

- 🦀 **NEW: cosmic-connect-core** - Shared Rust library for protocol implementation
- 📱 **Android app** - Kotlin UI and platform integration using Rust core via FFI
- 🖥️ **COSMIC Desktop applet** - Uses same Rust core directly
- 🔗 **Goal**: 70%+ code sharing between platforms with memory-safe protocol implementation

## 📋 Project Overview

This project builds a **hybrid Rust + Kotlin** Android app that shares protocol implementation with COSMIC Desktop. It involves:

1. **Rust Core Library** (NEW): Extract protocol from COSMIC applet to shared library (cosmic-connect-core)
2. **Android App** (MAIN FOCUS): Convert Java to Kotlin + integrate Rust core via FFI (uniffi-rs)
3. **COSMIC Desktop** (REFACTOR): Update to use shared Rust core library
4. **Protocol Compatibility**: Single source of truth ensures perfect communication

**Timeline**: 16-20 weeks
**Total Issues**: 62
**Phases**: 6 (new Phase 0: Rust Core Extraction)

---

## 🎯 Where to Start

### Critical First Steps (Weeks 1-3: Phase 0)

**Start Here**: Issue #43 - Analyze COSMIC Applet for Protocol Extraction

This is THE foundation. Phase 0 extracts Rust core before Android work begins:
- Rust toolchain 1.70+ with cargo, uniffi-bindgen
- cosmic-applet-cosmicconnect repository cloned
- Understanding of Rust, tokio, async programming
- Protocol extraction plan document

**Then**: Issue #1 - Development Environment Setup (for Android integration)
- Android Studio (latest stable)
- Rust Android targets (aarch64-linux-android, etc.)
- cargo-ndk for Android builds
- Both Rust core and Android app building successfully

### Critical Path

The project has a strict dependency chain:

```
Phase 0: Rust Core Extraction (Weeks 1-3) ← START HERE!
├─ #43: Analyze COSMIC Applet (1 day)
├─ #44: Create cosmic-connect-core Cargo project (4h)
├─ #45: Extract NetworkPacket → Rust (1 day) ← CRITICAL
├─ #46: Extract Discovery → Rust (1 day)
├─ #47: Extract TLS/Certificates → Rust (2 days) ← SECURITY CRITICAL
├─ #48: Extract Plugin Core → Rust (1 day)
├─ #49: Set up uniffi-rs FFI (1 day)
└─ #50: Validate with COSMIC Desktop (1 day)

Phase 1: Foundation & FFI Setup (Weeks 4-5)
├─ #1: Dev Environment + Rust Android targets (4h)
├─ #2: Android Audit (4h) ──┐
├─ #3: COSMIC Audit (3h) ───┤
├─ #4: Protocol Testing (4h) ◄─┘
├─ #5: Project Board (3h)
├─ #51: cargo-ndk in Android Build (1 day) ← CRITICAL
└─ #52: Android FFI Wrapper Layer (2 days)

Phase 2: Core Modernization (Weeks 6-9)
├─ #6: Root Gradle → Kotlin DSL (2h)
├─ #7: App Gradle → Kotlin DSL (3h)
├─ #8: Version Catalog (2h)
├─ #53: Integrate NetworkPacket FFI (1 day) ← REPLACES #9-10
├─ #54: Integrate TLS/Certificate FFI (2 days) ← REPLACES #13-14
├─ #55: Integrate Discovery FFI (1 day) ← UPDATES #15
├─ #11: Device Class → Kotlin (4h)
├─ #12: DeviceManager → Repository (6h)
└─ #16: Discovery Integration Test (3h)

Phase 3: Feature Implementation (Weeks 10-14)
├─ #17: Plugin Architecture Bridge (FFI) (1 day)
├─ #56-61: Plugins with Rust Core (1-2 days each)
└─ #18-27: Jetpack Compose UI (varies)

Phase 4: Integration & Testing (Weeks 15-18)
├─ #62: FFI Integration Testing (2 days) ← NEW
└─ #28-42: Comprehensive testing (varies)

Phase 5: Release (Weeks 19-20)
└─ #38-42: Beta, testing, release (varies)
```

**Key Insight**: You MUST complete Phase 0 first. The Rust core is the foundation for everything.

---

## 🏷️ GitHub Labels Required

### Priority Labels (4)
- `P0-Critical` - Red (#d73a4a) - Must complete, blocking
- `P1-High` - Orange (#ff6b6b) - High importance
- `P2-Medium` - Yellow (#ffa500) - Medium priority
- `P3-Low` - Light Yellow (#ffcc00) - Future enhancement

### Category Labels (30+)
- `setup` - Green (#0e8a16)
- `infrastructure` - Blue (#1d76db)
- `audit` - Purple (#5319e7)
- `android` - Android Green (#3ddc84)
- `cosmic` - COSMIC Orange (#ed8936)
- `rust` - Rust Orange (#CE422B) ← NEW
- `kotlin` - Kotlin Purple (#7F52FF) ← NEW
- `ffi` - FFI Purple (#D4C5F9) ← NEW
- `documentation` - Light Blue (#0075ca)
- `testing` - Yellow (#fbca04)
- `protocol` - KDE Blue (#1D76DB) ← NEW
- `gradle` - Dark Teal (#02303a)
- `build-system` - Teal (#006b75)
- `kotlin-conversion` - Purple (#7f52ff)
- `architecture` - Green (#0E8A16) ← UPDATED
- `security` - Dark Red (#b60205)
- `tls` - Orange-Red (#d93f0b)
- `networking` - Blue (#0052cc)
- `plugins` - Light Cyan (#bfdadc)
- `ui` - Light Green (#c2e0c6)
- `design` - Rose (#e99695)
- `compose` - Purple (#7057ff)
- `integration` - Sky Blue (#5ebeff) ← UPDATED
- `e2e` - Blue (#1d76db)
- `performance` - Lavender (#d4c5f9)
- `release` - Green (#0e8a16)
- `project-management` - Light Blue (#bfd4f2)

---

## 🚀 Quick Start Commands

### 1. Create Labels

```bash
# Make the script executable
chmod +x create-github-labels.sh

# Create all labels
./create-github-labels.sh

# Verify
gh label list
```

### 2. Create Initial Issues (Manual Approach Recommended)

Due to the complexity and dependencies, I recommend creating issues manually through the GitHub UI or using the `gh` CLI interactively:

```bash
# Example: Create Issue #1
gh issue create \
  --title "Development Environment Setup" \
  --label "P0-Critical,setup,infrastructure" \
  --body "See PROJECT_PLAN.md for full details"
```

**Why manual?**
- Each issue has detailed requirements
- Dependencies need careful linking
- Milestones need configuration
- Better for understanding the work

### 3. Set Up Project Board

```bash
# Create project board
gh project create --title "COSMIC Connect Modernization" --body "12-week modernization project"

# Or use GitHub UI for better control
```

---

## 📊 Understanding the 6 Phases

### Phase 0: Rust Core Extraction (Weeks 1-3) ← START HERE!
**Goal**: Extract protocol implementation from COSMIC applet into shared Rust library

**Issues**: #43-50
**Key Outputs**:
- ✅ cosmic-connect-core Cargo project created
- ✅ NetworkPacket, Discovery, TLS extracted to Rust
- ✅ uniffi-rs FFI bindings working
- ✅ COSMIC Desktop validated with new core
- ✅ Comprehensive Rust tests passing

**Why Critical**: This is the foundation. All protocol logic moves to Rust for memory safety and code sharing. Without this, there's no hybrid architecture.

### Phase 1: Foundation & FFI Setup (Weeks 4-5)
**Goal**: Prepare Android environment and create FFI bridge

**Issues**: #1-5, #51-52
**Key Outputs**:
- ✅ Rust Android targets configured
- ✅ cargo-ndk integrated in Gradle
- ✅ Android FFI wrapper layer created
- ✅ Both Rust core and Android app building
- ✅ Complete audits of Android and COSMIC code

**Why Critical**: cargo-ndk (#51) is required for all FFI work. The wrapper layer (#52) provides clean Kotlin API over FFI.

### Phase 2: Core Modernization (Weeks 6-9)
**Goal**: Integrate Rust core via FFI and modernize build system

**Issues**: #6-16, #53-55
**Key Outputs**:
- ✅ Kotlin DSL build system
- ✅ NetworkPacket FFI integration (#53 - uses Rust core)
- ✅ TLS/Certificate FFI integration (#54 - Rust + Android Keystore)
- ✅ Discovery FFI integration (#55 - Rust core with Android networking)
- ✅ Repository pattern for device management
- ✅ MVVM architecture foundations

**Critical Issues**:
- #53: NetworkPacket FFI (foundation for ALL communication via Rust)
- #54: TLS/Certificate FFI (security foundation via Rust)
- #51: cargo-ndk (required for compiling Rust for Android)

### Phase 3: Feature Implementation (Weeks 10-14)
**Goal**: Implement plugins with Rust core and modernize UI

**Issues**: #17-27, #56-61
**Key Outputs**:
- ✅ Plugin architecture FFI bridge (#17)
- ✅ All 6 core plugins using Rust core (#56-61):
  - Battery, Share, Clipboard, Notifications, RunCommand, FindMyPhone
- ✅ Jetpack Compose UI (#24-27)
- ✅ Material 3 design system

**Pattern**: Rust core handles protocol, Kotlin handles Android UI and system APIs

### Phase 4: Integration & Testing (Weeks 15-18)
**Goal**: Verify everything works together, especially FFI boundaries

**Issues**: #28-42, #62
**Key Outputs**:
- ✅ FFI integration testing (#62 - memory safety, thread safety)
- ✅ Integration test framework
- ✅ End-to-end tests (Android ↔ COSMIC)
- ✅ Performance benchmarks
- ✅ Complete documentation

**Critical Tests**:
- #62: FFI memory safety and concurrency
- #32: Android → COSMIC (FFI working correctly)
- #33: COSMIC → Android (protocol compatibility)

### Phase 5: Release & Maintenance (Weeks 19-20)
**Goal**: Ship it!

**Issues**: #38-42
**Key Outputs**:
- ✅ Beta release
- ✅ User testing feedback
- ✅ Production release (Play Store + F-Droid)

---

## 💡 What You Need to Know

### Critical Technical Concepts

#### 1. **COSMIC Connect Protocol**
- **Version**: 7 (must match)
- **Discovery**: UDP broadcast on port 1716
- **Connection**: TCP ports 1714-1764
- **Encryption**: TLS with self-signed certificates
- **Pairing**: Certificate exchange and trust

📖 **Read**: `cosmicconnect-protocol-debug.md` - This is your bible

#### 2. **Packet Format** (CRITICAL!)
```json
{
  "id": 1234567890,
  "type": "cosmicconnect.identity",
  "body": { ... }
}\n  ← MUST END WITH NEWLINE!
```

**Common Mistake**: Forgetting the `\n` terminator. This breaks everything.

#### 3. **TLS Role Determination**
```kotlin
// The device with LARGER deviceId is TLS SERVER
if (myDeviceId > theirDeviceId) {
    // I'm the server
} else {
    // I'm the client
}
```

**Critical**: Get this wrong and TLS handshake fails.

#### 4. **Hybrid Architecture Stack**
**Rust Core (cosmic-connect-core)**:
- Rust 1.70+ (protocol implementation)
- tokio (async runtime)
- rustls (TLS implementation)
- rcgen (certificate generation)
- serde/serde_json (serialization)
- uniffi-rs (FFI binding generation)

**Android (cosmic-connect-android)**:
- Kotlin (UI and Android integration)
- uniffi-generated bindings (Rust ↔ Kotlin bridge)
- Coroutines (async operations)
- StateFlow (reactive state)
- Jetpack Compose (declarative UI)
- MVVM architecture (separation of concerns)
- cargo-ndk (Rust Android compilation)

#### 5. **COSMIC Desktop Stack**
- Uses cosmic-connect-core directly (no FFI needed)
- libcosmic (UI framework)
- iced (widget system)
- tokio (provided by core)

---

## 🔍 How to Use Claude Code

### Skills Available

Located in `.claude/skills/`:

1. **android-development-SKILL.md** - Android/Kotlin patterns
2. **cosmic-desktop-SKILL.md** - COSMIC/Rust development
3. **gradle-SKILL.md** - Build system configuration
4. **tls-networking-SKILL.md** - Secure communication
5. **debugging-SKILL.md** - Troubleshooting techniques

### Agents Available

Located in `.claude/agents/`:

1. **android-modernization** - For Issues #6-27 (Android work)
2. **cosmic-desktop** - For COSMIC applet work
3. **protocol-compatibility** - For Issues #4, #16, #32-33

### Example Usage

```bash
# Start work on an issue
claude-code "Read PROJECT_PLAN.md issue #9 and implement NetworkPacket conversion to Kotlin"

# Use specific agent
claude-code --agent android-modernization "Modernize the Battery plugin"

# Debug protocol issues
claude-code --agent protocol-compatibility "Debug TLS handshake failure between Android and COSMIC"
```

---

## 📝 Weeks 1-3 Action Plan (Phase 0: Rust Core Extraction)

### Week 1: Analysis & Setup

**Day 1-2: Protocol Analysis**
- [ ] Complete Issue #43 (Analyze COSMIC Applet)
- [ ] Clone cosmic-applet-cosmicconnect repository
- [ ] Map all protocol-related code in Rust
- [ ] Identify NetworkPacket, Discovery, TLS implementations
- [ ] Create docs/rust-extraction-plan.md

**Day 3: Cargo Project**
- [x] Complete Issue #44 (Create cosmic-connect-core) ✅
- [x] Initialize Cargo library project ✅
- [x] Set up module structure (protocol/, network/, crypto/, plugins/, ffi/) ✅
- [x] Configure uniffi-rs in Cargo.toml ✅
- [x] Create initial README and ARCHITECTURE.md ✅

**Day 4-5: NetworkPacket Extraction**
- [x] Start Issue #45 (Extract NetworkPacket) ✅
- [x] Implement packet serialization/deserialization ✅
- [x] **CRITICAL**: Ensure newline termination (`\n`) ✅
- [x] Write comprehensive unit tests ✅
- [x] Test with sample packets ✅

### Week 2: Core Extraction

**Day 1-2: Discovery & TLS**
- [ ] Complete Issue #46 (Extract Discovery)
- [ ] Complete Issue #47 (Extract TLS/Certificates)
- [ ] Implement UDP multicast discovery (224.0.0.251:1716)
- [ ] Implement certificate generation (RSA 2048-bit)
- [ ] **CRITICAL**: Implement TLS role logic (deviceId comparison)
- [ ] Write security tests

**Day 3-4: Plugin System**
- [ ] Complete Issue #48 (Extract Plugin Core)
- [ ] Define Plugin trait
- [ ] Implement PluginManager
- [ ] Create packet routing logic
- [ ] Test plugin registration

**Day 5: FFI Setup**
- [ ] Complete Issue #49 (Set up uniffi-rs)
- [ ] Define FFI interfaces in .udl files
- [ ] Generate Kotlin bindings
- [ ] Test basic FFI calls
- [ ] Document FFI API

### Week 3: Validation

**Day 1-3: COSMIC Desktop Integration**
- [ ] Complete Issue #50 (Validate with COSMIC Desktop)
- [ ] Refactor cosmic-applet-cosmicconnect to use core
- [ ] Test device discovery
- [ ] Test TLS pairing
- [ ] Test packet exchange
- [ ] Verify no regressions

**Day 4-5: Documentation & Planning**
- [ ] Create docs/ffi-design.md
- [ ] Document all public APIs
- [ ] Write integration examples
- [ ] Plan Phase 1 (Android FFI integration)
- [ ] Celebrate Phase 0 completion! 🎉

---

## 🎯 Success Metrics

### Phase 0 Success (Weeks 1-3)
- ✅ cosmic-connect-core Cargo project created
- ✅ NetworkPacket, Discovery, TLS extracted to Rust
- ✅ uniffi-rs generating Kotlin bindings
- ✅ COSMIC Desktop validated with new core
- ✅ All Rust tests passing

### Phase 1 Success (Weeks 4-5)
- ✅ Rust Android targets configured
- ✅ cargo-ndk integrated in Gradle build
- ✅ Android FFI wrapper layer created
- ✅ Both Rust and Android codebases build
- ✅ Comprehensive audits completed

### Phase 2 Success (Weeks 6-9)
- ✅ NetworkPacket, TLS, Discovery integrated via FFI
- ✅ Gradle build modernized to Kotlin DSL
- ✅ MVVM architecture foundations in place
- ✅ Android ↔ Rust FFI working reliably

### Phase 3 Success (Weeks 10-14)
- ✅ Plugin FFI bridge architecture complete
- ✅ All 6 core plugins using Rust core
- ✅ Jetpack Compose UI implemented
- ✅ Material 3 design system applied

### Phase 4 Success (Weeks 15-18)
- ✅ FFI memory safety tests passing
- ✅ 80%+ test coverage (Rust + Kotlin)
- ✅ Full Android ↔ COSMIC compatibility verified
- ✅ Performance benchmarks meet targets

### Project Success (Weeks 19-20)
- ✅ Beta release to testers
- ✅ User feedback incorporated
- ✅ Released to Play Store + F-Droid
- ✅ Documentation complete

---

## ⚠️ Common Pitfalls

### 1. Skipping Audits
**Don't**: Jump straight to coding
**Do**: Complete issues #2, #3, #4 thoroughly

### 2. Breaking Protocol Compatibility
**Don't**: Change packet formats
**Do**: Maintain exact COSMIC Connect protocol v7 compatibility

### 3. Ignoring Tests
**Don't**: Code without tests
**Do**: Write tests FIRST (TDD approach)

### 4. Working in Isolation
**Don't**: Modernize Android without testing COSMIC
**Do**: Use `protocol-compatibility` agent frequently

### 5. Skipping Documentation
**Don't**: Leave undocumented changes
**Do**: Update docs as you go

---

## 📚 Essential Reading Order

### For Phase 0 (Rust Core Extraction):
1. **This file** (you're here!)
2. `ARCHITECTURE.md` - Hybrid Rust+Kotlin architecture details
3. `ARCHITECTURE_CHANGES_SUMMARY.md` - Why hybrid architecture
4. `PROJECT_PLAN_UPDATED.md` - All 62 issues detailed
5. `IMPLEMENTATION_GUIDE.md` - Quick FFI implementation guide
6. `cosmicconnect-protocol-debug.md` - Protocol reference
7. Issues #43-50 - Phase 0 task details

### For Phase 1+ (Android Integration):
1. `GETTING_STARTED.md` (this file)
2. `ARCHITECTURE.md` - FFI architecture
3. `docs/ffi-design.md` - FFI interface design (created in Phase 0)
4. `docs/rust-extraction-plan.md` - Extraction plan (created in Phase 0)
5. `CLAUDE.md` - Claude Code usage
6. `.claude/skills/` - Android, Rust, and FFI skill documentation
7. Issues #1-42, #51-62 - Phase 1-5 task details

---

## 🤝 Getting Help

### Using Claude Code

```bash
# Understand a component
claude-code "Explain the current Device class implementation"

# Plan implementation
claude-code "Read PROJECT_PLAN.md issue #11 and create an implementation plan"

# Debug issues
claude-code "Using debugging skill, analyze this TLS handshake failure"

# Review code
claude-code "Review my NetworkPacket implementation for protocol compatibility"
```

### Community Resources

- COSMIC Connect Protocol: https://invent.kde.org/network/cosmicconnect-meta
- COSMIC Desktop: https://github.com/pop-os/cosmic-epoch
- Android Kotlin Guide: https://developer.android.com/kotlin

---

## ✅ Ready to Start?

### Your First Commands

```bash
# 1. Clone both repositories
git clone https://github.com/olafkfreund/cosmic-connect-android
git clone https://github.com/olafkfreund/cosmic-applet-cosmicconnect

# 2. Install Rust toolchain
curl --proto='=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
rustup toolchain install stable
cargo install uniffi-bindgen

# 3. Start with Phase 0
cd cosmic-connect-android
gh issue view 43  # Read Issue #43: Analyze COSMIC Applet

# 4. Begin Rust extraction
claude-code "Read ARCHITECTURE.md and PROJECT_PLAN_UPDATED.md and help me with Issue #43"
```

### Recommended Tools

**Phase 0 (Rust extraction)**:
- **IDE**: VS Code + rust-analyzer, RustRover
- **Tools**: cargo, uniffi-bindgen, cargo-watch
- **Testing**: cargo test, cargo clippy
- **Debugging**: rust-gdb, rust-lldb

**Phase 1+ (Android integration)**:
- **IDE**: Android Studio Hedgehog+
- **Build**: cargo-ndk, Android NDK
- **Testing**: adb, cargo test, Gradle
- **Networking**: Wireshark, tcpdump
- **Version Control**: git with conventional commits

---

## 🎉 You're Ready!

The project is well-documented with a clear hybrid Rust+Kotlin architecture. Follow Phase 0 first (Rust extraction), then proceed to Android integration.

**Remember**: Phase 0 (Rust core extraction) is the foundation. Complete it before Android work!

Good luck building the future of COSMIC Connect! 🚀🦀

---

**Last Updated**: 2026-01-15
**Project Status**: Phase 0 - Rust Core Extraction
**Next Milestone**: Complete Issue #43 (Analyze COSMIC Applet for Protocol Extraction)
