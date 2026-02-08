# Changelog

All notable changes to COSMIC Connect Android will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned
- Additional plugin implementations
- Performance optimizations
- Enhanced tablet UI
- More COSMIC Desktop integrations
- Localization (multiple languages)

---

## [1.2.0-beta] - 2026-02-08

### Architecture Overhaul

#### NetworkPacket Unification (#142)
- Unified dual NetworkPacket system into single `Core.NetworkPacket` data class
- Migrated all plugins and transport layer to new packet system
- Deleted legacy `NetworkPacket.kt` and all bridge methods (`toLegacy()`, `fromLegacy()`, etc.)
- Added `payloadTransferInfo` field to match desktop Rust `Packet` struct

#### Device.kt Decomposition (#143)
- Decomposed 702-line god class into focused managers via facade pattern
- `ConnectionManager` (179 lines): link management, send channel, packet routing
- `PluginManager` (209 lines): plugin lifecycle, permissions, dispatch
- `PairingManager` (148 lines): pairing state machine, callbacks, notifications
- `PacketSender` interface to break circular dependencies

#### Hilt DI Integration (#144)
- Migrated all 30 plugins from reflection-based loading to Hilt `@AssistedInject`
- `StaticPluginMetadata` pattern to work around Dagger KSP annotation bug
- Zero legacy reflection-based plugin instantiation remaining

### New Plugins (#145)

#### Tier 1 — Android-Only
- **NetworkInfoPlugin** (#148): WiFi SSID, signal strength, network type, metered/VPN status
- **PowerPlugin** (#149): Remote shutdown/reboot/suspend/hibernate with confirmation dialogs

#### Tier 2 — With Core FFI
- **LockPlugin** (#150): Lock/unlock remote screen with status tracking

#### Tier 3 — Experimental
- **AudioStreamPlugin** (#153): Audio streaming control between devices
- **FileSyncPlugin** (#154): File synchronization with conflict resolution
- **ScreenSharePlugin** (#155): Screen sharing with configurable quality
- **VirtualMonitorPlugin** (#156): Use phone as extra display

Total plugins: 23 → 30

### Test Coverage (#146)
- **787 tests** across 39 test files (was 130 — **+505% increase**)
- Comprehensive unit tests for all major plugins, transport layer, and core components
- Robolectric-based tests with proper Android resource handling

### Security & Build Hardening
- BouncyCastle upgraded `bcpkix-jdk15on:1.70` → `bcpkix-jdk18on:1.80` (CVE fix)
- compileSdk/targetSdk 34 → 35 with core-ktx 1.15.0
- `RunCommandUrlActivity` now requires user confirmation before executing commands
- `FindMyPhoneReceiver`, `ShareBroadcastReceiver`, `TransactionService` set `exported=false`
- `PairingHandler` test backdoor guarded behind `BuildConfig.DEBUG`
- `BackgroundService.initialized` flag made `@Volatile`
- Removed deprecated `onAudioInfoChanged` override (API 35 compatibility)

### Build & Dependency Cleanup
- Removed duplicate WebRTC/OkHttp/Moshi declarations
- Replaced deprecated `lifecycle-extensions` with `lifecycle-process`
- Resolved OSGI-INF packaging conflict via `pickFirsts`
- Thread pool bounded: `newCachedThreadPool()` → `newFixedThreadPool(8)`
- UDP packet size corrected: 512KB → 8KB
- File transfer buffer optimized: 4KB → 64KB

### Release Infrastructure
- GitHub Actions CI pipeline (lint, test, build)
- GitHub Actions Release pipeline (signed APK + checksums on tag push)
- Release signing config via environment variables
- `scripts/build-release.sh` — local release build with validation
- `scripts/bump-version.sh` — version bump with CHANGELOG + git tag
- Release checklist documentation

---

## [1.0.0-beta] - 2026-01-17

### First Beta Release! 🎉

This is the first beta release of COSMIC Connect for Android - a modern reimagining of KDE Connect Android specifically for COSMIC Desktop.

### Major Features

#### 🏗️ **Hybrid Rust + Kotlin Architecture**
- **Rust Core Library** (`cosmic-connect-core`)
  - Shared protocol implementation with COSMIC Desktop
  - NetworkPacket implementation
  - Device discovery and networking
  - TLS/Certificate management
  - Plugin core logic
  - 70%+ code sharing with desktop applet

- **Kotlin/Android Layer**
  - Modern Kotlin with coroutines
  - Jetpack Compose UI
  - Material Design 3
  - Android platform integration
  - FFI wrapper layer via uniffi-rs

#### 🎨 **Complete UI Modernization**
- **Jetpack Compose** migration (100% complete)
- **Material Design 3** throughout
- **8 Complete Component Sets:**
  - Foundation components (Color, Theme, Typography)
  - Button components (5 variants)
  - Input components (TextField, SearchBar, Chip, Switch)
  - Card components (4 variants for devices, plugins, settings)
  - List item components (5 types)
  - Dialog components (Alert, Pairing, Permission, Progress)
  - Navigation components (complete nav system)
  - Status indicators (Connection, Battery, Transfer, Pairing)

- **Modern Android Patterns:**
  - State hoisting and unidirectional data flow
  - Material3 dynamic theming
  - Responsive layouts
  - Accessibility built-in
  - Dark mode support

#### ✅ **Comprehensive Testing (204 Tests)**
- **Unit Tests** (~50 tests)
  - FFI validation
  - Core functionality
  - Packet creation and serialization

- **Integration Tests** (109 tests)
  - Discovery & Pairing (24 tests)
  - File Transfer (19 tests)
  - All Plugins (35 tests)
  - Lifecycle management (8 tests)

- **E2E Tests** (31 tests)
  - Android → COSMIC (15 tests)
  - COSMIC → Android (16 tests)
  - Bidirectional validation

- **Performance Tests** (14 benchmarks)
  - FFI performance (call overhead, serialization)
  - File transfer throughput
  - Network performance
  - Memory usage
  - Stress testing

#### 📁 **Core Features**
- **File & URL Sharing** - Share files instantly between devices
- **Clipboard Sync** - Copy on one device, paste on another
- **Notification Mirroring** - See phone notifications on desktop
- **SMS Integration** - Send/receive texts from desktop
- **Media Control** (MPRIS) - Remote control for desktop media
- **Battery Monitor** - View phone battery on desktop
- **Find My Phone** - Make phone ring to locate it
- **Run Commands** - Execute desktop commands remotely
- **Remote Input** - Use phone as touchpad/keyboard
- **Telephony** - Call notifications on desktop

#### 🔒 **Security & Privacy**
- **TLS 1.2+ Encryption** for all communication
- **Certificate Pinning** to prevent MITM attacks
- **2048-bit RSA Keys** for secure pairing
- **Local-Only Communication** - No cloud services
- **Zero Data Collection** - Complete privacy
- **Open Source** - GPL-3.0 licensed

#### ⚡ **Performance**
All performance targets met or exceeded:
- FFI call overhead: 0.45ms (target: < 1ms) ✅
- File transfer: 21.4 MB/s for large files (target: ≥ 20 MB/s) ✅
- Discovery latency: 2.34s (target: < 5s) ✅
- Memory growth: < 50 MB per operation ✅
- Stress testing: 0% packet loss ✅

### Changed from KDE Connect Android

#### Architecture
- **CHANGED:** Hybrid Rust + Kotlin instead of pure Java/Kotlin
- **CHANGED:** Shared protocol implementation with desktop
- **CHANGED:** Modern FFI layer via uniffi-rs
- **CHANGED:** Jetpack Compose instead of XML layouts

#### UI/UX
- **CHANGED:** Complete Material Design 3 redesign
- **CHANGED:** Modern navigation architecture
- **CHANGED:** Improved accessibility
- **CHANGED:** Better tablet support
- **CHANGED:** Cleaner, more intuitive interface

#### Code Quality
- **ADDED:** Comprehensive test suite (204 tests)
- **ADDED:** Performance benchmarking
- **ADDED:** E2E validation
- **IMPROVED:** Code organization and maintainability
- **IMPROVED:** Type safety throughout
- **IMPROVED:** Error handling

#### Build System
- **CHANGED:** Gradle Kotlin DSL instead of Groovy
- **ADDED:** cargo-ndk integration for Rust builds
- **ADDED:** Version catalog for dependency management
- **CHANGED:** Updated to latest AGP and build tools

### Removed from KDE Connect Android

- **REMOVED:** Legacy Java code (migrated to Kotlin)
- **REMOVED:** Old XML layouts (migrated to Compose)
- **REMOVED:** Duplicate protocol implementations
- **REMOVED:** Obsolete dependencies
- **REMOVED:** Unused resources and code

### Fixed

- **FIXED:** 168 compilation errors during migration
- **FIXED:** FFI binding signature clashes
- **FIXED:** Memory leaks in FFI layer
- **FIXED:** NetworkPacket import issues
- **FIXED:** Plugin lifecycle management
- **FIXED:** Certificate validation edge cases

### Documentation

- **ADDED:** Comprehensive User Guide
- **ADDED:** Detailed FAQ
- **ADDED:** Privacy Policy
- **ADDED:** Testing documentation (TESTING.md)
- **ADDED:** Release preparation guide
- **ADDED:** Architecture documentation
- **ADDED:** Per-issue implementation reports (15+ documents)
- **UPDATED:** README with complete project status

### Known Issues

- Beta software - report issues on GitHub
- Limited testing on older Android versions (< 8.0)
- Some edge cases in plugin synchronization
- Performance may vary on low-end devices

### Upgrading from KDE Connect Android

**Note:** COSMIC Connect is not a direct replacement for KDE Connect Android. It's specifically designed for COSMIC Desktop.

If you're switching from KDE Connect:
1. Unpair devices in KDE Connect
2. Install COSMIC Connect
3. Pair fresh with COSMIC Desktop
4. Configure plugins as needed

**Data is not migrated** between apps. This is a fresh start.

---

## Development History

### Phase 0: Foundation (Complete)
*Duration: Weeks 1-3*

- **Issue #44:** Project restructuring and build system
- **Issue #45:** NetworkPacket FFI implementation
- **Issue #46:** Discovery service FFI
- **Issue #47:** TLS/Certificate management FFI
- **Issue #48:** Core FFI validation
- **Issue #51:** cargo-ndk build integration

**Outcome:** Shared Rust core library established

### Phase 1: Core Protocol Plugins (Complete)
*Duration: Weeks 4-6*

- **Issue #50:** FFI validation test framework
- **Issue #54:** Battery Plugin FFI migration
- **Issue #55:** Telephony Plugin FFI migration
- **Issue #56:** Share Plugin FFI migration
- **Issue #57:** Notifications Plugin FFI migration
- **Issue #58:** Clipboard Plugin FFI migration
- **Issue #59:** FindMyPhone Plugin FFI migration
- **Issue #60:** RunCommand Plugin FFI migration
- **Issue #61:** Ping Plugin FFI migration

**Outcome:** Core plugins migrated to Rust FFI

### Phase 2: Major Feature Plugins (Complete)
*Duration: Weeks 7-9*

- **Issue #62:** NotificationsPlugin FFI migration
- **Issue #63:** SMS Plugin FFI migration
- **Issue #64:** Contacts Plugin FFI migration
- **Issue #65:** SystemVolume Plugin FFI migration
- **Issue #66:** MPRIS Plugin FFI migration
- **Issue #68:** Presenter Plugin FFI migration
- **Issue #69:** Connectivity Plugin FFI migration

**Outcome:** Major feature plugins migrated

### Phase 3: Remaining Plugins (Complete)
*Duration: Weeks 10-12*

- **Issue #67:** MousePad Plugin FFI migration
- **Issue #68:** RemoteKeyboard Plugin FFI migration
- **Issue #69:** Digitizer Plugin FFI migration
- **Issue #70:** SFTP Plugin FFI migration
- **Issue #71:** MprisReceiver Plugin FFI migration
- **Issue #72:** MouseReceiver Plugin analysis
- **Issue #73:** ReceiveNotifications Plugin FFI migration

**Outcome:** All plugins migrated (20/20 complete)

**Milestone:** 100% FFI Migration Complete

### Phase 4.1-4.3: UI Modernization (Complete)
*Duration: Weeks 13-16*

- **Issue #74:** Material Design 3 Foundation
- **Issue #75:** Button Components
- **Issue #76:** Input Components
- **Issue #77:** Card Components
- **Issue #78:** List Item Components
- **Issue #79:** Dialog Components
- **Issue #80:** Navigation Components
- **Issue #81:** Status Indicators

**Outcome:** Complete UI modernization with Jetpack Compose + Material Design 3

### Phase 4.4: Testing Infrastructure (Complete)
*Duration: Weeks 17-19*

- **Issue #28:** Integration Test Framework Setup
- **Issue #30:** Integration Tests - Discovery & Pairing (24 tests)
- **Issue #31:** Integration Tests - File Transfer (19 tests)
- **Issue #32:** Integration Tests - All Plugins (35 tests)
- **Issue #33:** E2E Test: Android → COSMIC (15 tests)
- **Issue #34:** E2E Test: COSMIC → Android (16 tests)
- **Issue #35:** Performance Testing (14 benchmarks)

**Outcome:** Comprehensive test suite with 204 tests, all passing

**Milestone:** Phase 4 Complete - Ready for Beta Release

---

## Statistics

### Code Metrics
- **Lines of Code:** ~50,000 (Kotlin + Rust)
- **Test Coverage:** ~80%
- **Total Tests:** 787
- **Plugins:** 30 (20 migrated + 7 new + 3 existing)
- **Code Sharing:** 70%+ with COSMIC Desktop
- **Compilation Errors Fixed:** 168

### Development
- **Duration:** 19 weeks
- **Issues Completed:** 35+
- **Commits:** 500+
- **Documentation Pages:** 20+
- **Architecture:** Hybrid Rust + Kotlin

### Features
- **Plugins:** 30 working plugins
- **UI Components:** 8 complete component sets
- **Test Suites:** 4 (Unit, Integration, E2E, Performance)
- **Supported Android Versions:** 6.0 - 15
- **Target Architectures:** 4 (arm64, armv7, x86, x86_64)

---

## Credits

### Based On
COSMIC Connect is based on [KDE Connect Android](https://invent.kde.org/network/kdeconnect-android) by the KDE team.

**Original Authors:**
- Albert Vaca Cintora and the KDE Connect team
- Years of development and refinement
- Robust, secure protocol implementation

**We are grateful to:**
- KDE team for creating KDE Connect
- KDE Connect Android contributors
- COSMIC Desktop team at System76
- Rust community for excellent tools
- Android community for modern frameworks
- All open source contributors

### Modernization
- **Architecture Design:** Hybrid Rust + Kotlin approach
- **UI Redesign:** Material Design 3 implementation
- **Testing Infrastructure:** Comprehensive test suite
- **COSMIC Integration:** Desktop-specific optimizations

### Open Source
Licensed under GPL-3.0, same as KDE Connect.

Full source code available:
- Android: github.com/olafkfreund/cosmic-connect-android
- Rust Core: github.com/olafkfreund/cosmic-connect-core
- Desktop: github.com/olafkfreund/cosmic-applet-kdeconnect

---

## Versioning

COSMIC Connect follows [Semantic Versioning](https://semver.org/):
- **MAJOR:** Incompatible API changes
- **MINOR:** New features (backwards compatible)
- **PATCH:** Bug fixes (backwards compatible)

**Pre-release labels:**
- **-alpha:** Early development, unstable
- **-beta:** Feature complete, testing phase
- **-rc:** Release candidate, final testing

---

## Feedback

### Report Bugs
- GitHub Issues: github.com/olafkfreund/cosmic-connect-android/issues

### Request Features
- GitHub Discussions: github.com/olafkfreund/cosmic-connect-android/discussions

### Ask Questions
- GitHub Discussions
- Reddit: /r/pop_os, /r/CosmicDE
- Community channels (links on GitHub)

---

## What's Next?

### Phase 5: Release Preparation (In Progress)
- Final code polish
- Beta testing program
- App store submission
- Marketing materials
- Community engagement

### Phase 6: COSMIC Desktop Integration (Planned)
- Enhanced desktop integration features
- Performance optimizations
- Additional plugins
- Expanded testing

### Future Releases
- Regular bug fixes and updates
- Performance improvements
- New features based on community feedback
- Continued COSMIC Desktop integration

---

**Thank you for using COSMIC Connect!** 🚀

For more information, see:
- [User Guide](docs/USER_GUIDE.md)
- [FAQ](docs/FAQ.md)
- [Privacy Policy](docs/PRIVACY_POLICY.md)
- [Contributing Guidelines](CONTRIBUTING.md)

---

**Version:** 1.2.0-beta
**Release Date:** 2026-02-08
**Status:** Beta Testing
**Next Release:** 1.2.0 (stable)
