<div align="center">
  <img src="cosmic-connect_logo.png" alt="COSMIC Connect Logo" width="200"/>

  # COSMIC Connect Android

  **Seamless device communication between Android and COSMIC Desktop**

  [![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/olafkfreund/cosmic-connect-android)
  [![Protocol Version](https://img.shields.io/badge/protocol-v8-blue)](docs/protocol/)
  [![License](https://img.shields.io/badge/license-GPL--3.0-orange)](LICENSE)
  [![Rust Core](https://img.shields.io/badge/rust-1.84+-red)](https://github.com/olafkfreund/cosmic-connect-core)

</div>

---

## Overview

COSMIC Connect enables Android devices to communicate with COSMIC Desktop computers, providing features like clipboard sharing, notification sync, file transfer, and remote control. Built with a **hybrid Rust + Kotlin architecture** for maximum reliability, security, and code reuse.

## Features

- **Shared Clipboard** - Copy and paste between phone and desktop
- **Notification Sync** - Read and reply to Android notifications from desktop
- **File & URL Sharing** - Transfer files and URLs between devices
- **Multimedia Remote Control** - Use phone as media player remote
- **Virtual Touchpad & Keyboard** - Control computer from phone
- **Battery Monitoring** - View phone battery status on desktop
- **Find My Phone** - Make phone ring to locate it
- **Run Commands** - Execute predefined commands remotely
- **Telephony Integration** - SMS and call notifications on desktop
- **Camera Webcam** - Use phone camera as wireless webcam for video calls
- **App Continuity** - Open URLs and files seamlessly across devices
- **Network Info** - Report device network status (WiFi SSID, signal, type)
- **Power Management** - Remote shutdown/reboot/suspend/hibernate
- **Screen Lock** - Lock or unlock the remote device's screen
- **Audio Streaming** - Stream audio between phone and desktop
- **File Sync** - Synchronize files with conflict resolution
- **Screen Share** - Share screen with configurable quality
- **Virtual Monitor** - Use phone as an extra display for desktop

All features work wirelessly over Wi-Fi or Bluetooth using **secure TLS encryption**.

## Architecture

### Hybrid Approach

The project uses a **Rust core + Kotlin UI** architecture for optimal performance and code sharing:

#### Rust Core ([cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core))
- Protocol implementation (KDE Connect protocol v8)
- Network communication and device discovery
- TLS certificate management and encryption
- Plugin system with FFI interfaces
- Packet creation and serialization
- Cross-platform business logic

#### Kotlin/Android (this repository)
- Android UI and Activities
- Platform-specific integrations
- Service lifecycle management
- FFI wrapper layer with type safety
- Widget and notification providers

#### Benefits
- **70%+ code sharing** with COSMIC Desktop applet
- **Single source of truth** for protocol implementation
- **Memory safety** via Rust's ownership system
- **Fix bugs once**, both platforms benefit
- **Type-safe FFI** via uniffi-rs bindings

### Technology Stack

#### Rust Core
- **Rust**: 1.84+ with Android targets (aarch64, armv7, x86, x86_64)
- **tokio**: Async runtime for network operations
- **uniffi-rs**: 0.27+ for FFI bindings generation
- **rustls**: Modern TLS implementation
- **serde**: JSON serialization/deserialization
- **cargo-ndk**: 4.1+ for Android cross-compilation

#### Android
- **Kotlin**: Modern Android development
- **Android SDK**: 35 (targeting Android 15)
- **Minimum SDK**: 23 (Android 6.0)
- **Gradle**: 8.14.1 with Kotlin DSL
- **AGP**: 8.7.3
- **JNA**: Native library loading
- **AndroidX**: Jetpack libraries

## Project Status

**30 plugins** | **787 unit tests** | **0 failures** | **All major architecture issues resolved**

### Recent Architecture Overhaul (2026-02-06)

| Issue | What Changed | Impact |
|-------|-------------|--------|
| **#142** Unified NetworkPacket | Eliminated dual packet system. Legacy `NetworkPacket.kt` deleted, all code uses `Core.NetworkPacket` data class + `TransferPacket` for payloads. | Zero double-serialization overhead |
| **#143** Device.kt decomposition | God class (702 lines) split into `ConnectionManager`, `PluginManager`, `PairingManager` via facade pattern (325 lines). | Testable, focused components |
| **#144** Hilt DI for plugins | All 30 plugins migrated from reflection to `@AssistedInject` + `@AssistedFactory`. | Type-safe, compile-time verified DI |
| **#145** Desktop plugin parity | Added 7 new plugins across 3 tiers: NetworkInfo, Power, Lock, AudioStream, FileSync, ScreenShare, VirtualMonitor. | 30 plugins (was 23) |
| **#146** Test coverage | 130 tests expanded to 787 across 39 test files. | All testable plugins covered |

### Security & Build Hardening
- BouncyCastle upgraded (`bcpkix-jdk15on:1.70` -> `bcpkix-jdk18on:1.80`) for CVE fix
- `compileSdk`/`targetSdk` 34 -> 35, `core-ktx` 1.15.0
- RunCommand URL execution now requires user confirmation dialog
- Exported receivers locked down (`exported=false`)
- Private keys migrated to Android Keystore

### Plugin Registry (30 plugins)

| Wave | Plugins | Status |
|------|---------|--------|
| **Wave 1** | Ping, FindRemoteDevice, ConnectivityReport, Presenter, MousePad | Shipped |
| **Wave 2** | Clipboard, SystemVolume, RemoteKeyboard, RunCommand, OpenOnDesktop | Shipped |
| **Wave 3** | Battery, FindMyPhone, Contacts, Telephony, SMS | Shipped |
| **Wave 4** | Share, SFTP, MPRIS, Notifications, ReceiveNotifications | Shipped |
| **Wave 5** | OpenOnPhone, Camera, ExtendedDisplay | Shipped |
| **Wave 6** | NetworkInfo, Power, Lock | Shipped |
| **Wave 7** | ScreenShare, FileSync, VirtualMonitor, AudioStream | Shipped (experimental) |

### Test Suite: 787 Tests

| Category | Count | Files |
|----------|-------|-------|
| Plugin tests | ~610 | 29 test files covering all testable plugins |
| Core/architecture | ~110 | ConnectionManager, PluginManager, PairingManager, DeviceInfo, DeviceHelper, PairingHandler |
| Transport layer | ~40 | LanLinkProvider, BaseLink, BaseLinkProvider |
| Utility | ~31 | UrlValidator, MessagingNotificationHandler |

### Open Issues

No open issues — all 51 issues closed.

### Build Status

```
Build: PASSING (0 compilation errors)
Unit Tests: 787/787 passing
Plugins: 30/30 migrated to Hilt DI
Architecture: NetworkPacket unified, Device.kt decomposed
SDK: compileSdk 35, minSdk 23
```

## Installation

### For Users

**Coming Soon** - This modernized app will be available on:
- Google Play Store
- F-Droid
- GitHub Releases (direct APK downloads)

**Desktop Requirement**: Install [cosmic-connect-desktop-app](https://github.com/olafkfreund/cosmic-connect-desktop-app) on your COSMIC Desktop system.

### For Developers

#### Prerequisites

**Option 1: NixOS (Recommended)**
- NixOS with flakes enabled
- Automatic environment provisioning

**Option 2: Manual Setup**
- Android Studio Ladybug or later
- Rust 1.84+ with Android targets
- Android NDK 27.0.12077973
- cargo-ndk 4.1+
- JDK 17+

#### Quick Start (NixOS)

```bash
# Clone repositories
git clone https://github.com/olafkfreund/cosmic-connect-android
git clone https://github.com/olafkfreund/cosmic-connect-core

# Enter development environment
cd cosmic-connect-android
nix develop

# Build native libraries
./gradlew cargoBuild

# Build debug APK
./gradlew assembleDebug

# Output: build/outputs/apk/debug/cosmicconnect-android-debug-*.apk
```

#### Quick Start (Non-NixOS)

See [docs/guides/GETTING_STARTED.md](docs/guides/GETTING_STARTED.md) for detailed setup instructions.

## Documentation

All documentation is organized in the `docs/` directory:

### Quick Links
- [Documentation Index](docs/INDEX.md) - Complete documentation catalog
- [Getting Started](docs/guides/GETTING_STARTED.md) - Setup and first steps
- [Architecture](docs/architecture/ARCHITECTURE.md) - System design
- [Project Plan](docs/guides/PROJECT_PLAN.md) - Development roadmap

### Categories
- `docs/guides/` - Setup, development, and implementation guides
- `docs/architecture/` - System architecture and design documents
- `docs/protocol/` - KDE Connect protocol v8 implementation details
- `docs/issues/` - Completed issue summaries and progress reports
- `docs/legacy/` - Historical documentation (reference only)

### Recent Documentation
- [Phase 3 Complete](docs/phase-3-complete.md) - All remaining plugins migrated
- [Phase 2 Complete](docs/phase-2-complete.md) - Major feature plugins migrated
- [Issue #73: ReceiveNotifications](docs/issue-73-receivenotifications-plugin.md) - Final plugin migration
- [Issue #72: MouseReceiver](docs/issue-72-mousereceiver-plugin.md) - Pure receiver analysis
- [Issue #60 Completion Summary](docs/issue-60-completion-summary.md) - RunCommand plugin migration
- [FFI Validation Plan](docs/issue-50-ffi-validation.md) - Comprehensive FFI testing

## Development

### Building

```bash
# Build Rust core library for Android (all ABIs)
./gradlew cargoBuild

# Build debug APK
./gradlew assembleDebug

# Build release APK (signed)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run FFI validation tests
./gradlew test --tests FFIValidationTest

# Run specific test
./gradlew test --tests FFIValidationTest.testRunCommandPlugin

# Clean build
./gradlew clean
```

### Project Structure

```
cosmic-connect-android/
├── app/src/org/cosmic/cosmicconnect/       # Main source (non-standard layout)
│   ├── Core/                               # Unified packet system
│   │   ├── NetworkPacket.kt                # Immutable data class (the ONE packet type)
│   │   ├── TransferPacket.kt               # Payload-aware wrapper
│   │   ├── NetworkPacketCompat.kt          # Compat extensions (getString, getInt, etc.)
│   │   └── PacketType.kt                   # Packet type constants
│   ├── Plugins/                            # 30 plugins (all @AssistedInject)
│   │   ├── di/                             # Hilt DI module + PluginCreator interface
│   │   ├── BatteryPlugin/                  # Battery status monitoring
│   │   ├── LockPlugin/                     # Remote screen lock/unlock (Tier 2)
│   │   ├── NetworkInfoPlugin/              # Network status reporting
│   │   ├── PowerPlugin/                    # Remote power management
│   │   ├── AudioStreamPlugin/              # Audio streaming (Tier 3, experimental)
│   │   ├── FileSyncPlugin/                # File synchronization (Tier 3, experimental)
│   │   ├── ScreenSharePlugin/             # Screen sharing (Tier 3, experimental)
│   │   ├── VirtualMonitorPlugin/          # Virtual monitor (Tier 3, experimental)
│   │   └── ...                             # 22 more plugins
│   ├── Device.kt                           # Facade (325 lines, was 702)
│   ├── ConnectionManager.kt               # Link management, packet routing
│   ├── PluginManager.kt                    # Plugin lifecycle, permissions
│   ├── PairingManager.kt                   # Pairing state machine
│   └── BackgroundService.kt               # Main Android service
├── app/src/test/                           # 39 test files, 787 tests
├── app/src/uniffi/cosmic_connect_core/     # Generated FFI bindings
├── docs/                                   # Documentation
├── flake.nix                               # NixOS development environment
└── README.md                               # This file

cosmic-connect-core/                        # Rust core library (separate repo)
├── src/
│   ├── protocol/                           # Packet, protocol types
│   ├── network/                            # Discovery, connections
│   ├── crypto/                             # TLS, certificates
│   ├── plugins/                            # Plugin packet creation
│   │   ├── battery.rs, ping.rs, share.rs   # Core plugins
│   │   ├── lock.rs, webcam.rs              # Tier 2 plugins
│   │   ├── audiostream.rs, filesync.rs     # Tier 3 plugins
│   │   ├── screenshare.rs, virtualmonitor.rs
│   │   └── ...
│   ├── ffi/                                # FFI wrappers (uniffi)
│   └── cosmic_connect_core.udl            # UniFFI interface definition
└── bindings/                               # Generated language bindings
```

### Code Organization

**Core Layer** (`app/src/org/cosmic/cosmicconnect/Core/`):
- Unified `NetworkPacket` data class — single packet type for the entire app
- `TransferPacket` for payload-aware transfers
- Compat extensions for type-safe field access

**Plugin Layer** (`app/src/org/cosmic/cosmicconnect/Plugins/`):
- 30 plugins, all using Hilt `@AssistedInject` DI
- Each plugin has a `Factory` implementing `PluginCreator`
- Registered in `PluginFactory` (metadata) + `PluginModule` (Hilt bindings)

**Device Layer** (`app/src/org/cosmic/cosmicconnect/`):
- `Device.kt` facade delegates to focused managers
- `ConnectionManager`, `PluginManager`, `PairingManager`

**Rust Core** ([cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core)):
- Protocol implementation, packet creation, TLS, discovery
- UniFFI bindings generate Kotlin interfaces automatically

### Development Workflow

1. **Make changes** to Rust core or Kotlin code
2. **Build native libs**: `./gradlew cargoBuild` (if Rust changed)
3. **Build APK**: `./gradlew assembleDebug`
4. **Run tests**: `./gradlew test`
5. **Install on device**: `./gradlew installDebug`

## Testing

COSMIC Connect uses a **hybrid testing strategy** combining automated testing on Waydroid (90% coverage) with real device validation on Samsung Galaxy Tab S8 Ultra (10% Bluetooth + final validation). This approach provides comprehensive coverage while maintaining fast development iteration.

### Testing Infrastructure

We use two complementary testing platforms:

| Platform | Coverage | Use Cases | Speed | Automation |
|----------|----------|-----------|-------|------------|
| **Waydroid** | 85-90% | Network features, UI, plugins (except Bluetooth) | Fast (5-10s boot) | Fully automated ✅ |
| **Samsung Tab S8 Ultra** | 10% | Bluetooth testing, final E2E validation | Moderate | Automated via ADB ✅ |

**Why Waydroid?**
- Container-based (near-native performance, ~500 MB RAM)
- Full ADB support for instrumented tests
- Fast boot times (5-10 seconds vs 1-3 minutes for emulators)
- Perfect for CI/CD automation
- NixOS native integration

**Why Real Device?**
- Waydroid has no Bluetooth stack ([Issue #155](https://github.com/waydroid/waydroid/issues/155))
- Final validation on actual hardware
- Real-world network conditions
- User experience validation

### Quick Setup

#### Waydroid Setup (NixOS)

```bash
# 1. Add to /etc/nixos/configuration.nix
virtualisation.waydroid.enable = true;
boot.kernelModules = [ "ashmem_linux" "binder_linux" ];
users.users.YOUR_USERNAME.extraGroups = [ "waydroid" ];

# 2. Apply configuration
sudo nixos-rebuild switch

# 3. Initialize Waydroid (first time only)
sudo waydroid init

# 4. Run automated tests
./scripts/test-waydroid.sh
```

See [docs/testing/WAYDROID_TESTING_GUIDE.md](docs/testing/WAYDROID_TESTING_GUIDE.md) for complete setup and usage guide.

#### Samsung Galaxy Tab S8 Ultra Setup

```bash
# 1. Enable Developer Options on tablet:
#    Settings → About tablet → Tap "Build number" 7 times

# 2. Enable Wireless Debugging:
#    Settings → Developer options → Wireless debugging (ON)

# 3. Configure wireless connection:
#    Edit scripts/connect-tablet.sh with your tablet's IP and port

# 4. Connect and test
./scripts/connect-tablet.sh
./scripts/test-samsung.sh --wireless
```

See [docs/testing/SAMSUNG_TAB_TESTING_GUIDE.md](docs/testing/SAMSUNG_TAB_TESTING_GUIDE.md) for complete setup guide.

### Running Tests

#### Automated Testing Workflows

```bash
# Waydroid - Full automated test suite (recommended for daily development)
./scripts/test-waydroid.sh

# Waydroid - Headless mode (for CI/CD)
./scripts/test-waydroid.sh --headless

# Waydroid - Quick mode (skip clean build)
./scripts/test-waydroid.sh --quick

# Samsung Tablet - Full test suite
./scripts/test-samsung.sh --wireless

# Samsung Tablet - Quick tests
./scripts/test-samsung.sh --wireless --quick

# Samsung Tablet - Bluetooth-specific tests
./scripts/test-bluetooth.sh <device-serial>
```

#### Manual Test Execution

```bash
# Run all unit tests
./gradlew test

# Run all instrumentation tests (integration, E2E, performance)
./gradlew connectedAndroidTest

# Run specific test categories
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=org.cosmic.cosmicconnect.integration

./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=org.cosmic.cosmicconnect.e2e

./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.performance.PerformanceBenchmarkTest

# Run on Waydroid manually
waydroid session start
./gradlew connectedAndroidTest

# Run on Samsung tablet
export ANDROID_SERIAL=<device-serial>
./gradlew connectedAndroidTest
```

### Testing Coverage Matrix

The following table shows which features can be tested on each platform:

| Feature | Waydroid | Samsung Tab S8 | Priority |
|---------|----------|----------------|----------|
| Network Discovery (UDP) | ✅ | ✅ | High |
| Network Pairing (Wi-Fi) | ✅ | ✅ | High |
| **Bluetooth Discovery** | ❌ | ✅ | Medium |
| **Bluetooth Pairing** | ❌ | ✅ | Medium |
| **Bluetooth File Transfer** | ❌ | ✅ | Medium |
| File Transfer (Wi-Fi) | ✅ | ✅ | High |
| Clipboard Sync | ✅ | ✅ | High |
| Notification Sync | ✅ | ✅ | High |
| Media Control (MPRIS) | ✅ | ✅ | Medium |
| Find My Phone | ✅ | ✅ | Medium |
| Remote Input | ✅ | ✅ | Low |
| Run Commands | ✅ | ✅ | Medium |
| Battery Monitoring | ✅ | ✅ | Medium |
| Telephony (SMS/Calls) | ⚠️ (simulated) | ✅ | Low |
| **Overall Coverage** | **85-90%** | **100%** | - |

**Legend:**
- ✅ Fully supported and testable
- ❌ Not supported (hardware limitation)
- ⚠️ Partial support (simulated/mocked)

**Testing Strategy:**
- **Daily Development**: Use Waydroid for fast iteration (90% coverage)
- **Weekly Validation**: Run Bluetooth tests on Samsung tablet
- **Pre-Release**: Full E2E validation on Samsung tablet

### Test Suite Overview

**Total: 787 Unit Tests** | **All Passing** | **39 Test Files**

| Category | Tests | Examples |
|----------|-------|---------|
| **Plugin tests** | ~530 | MprisPluginTest (46), NotificationsPacketsFFITest (46), TelephonyPluginTest (38), OpenOnPhonePluginTest (37), etc. |
| **Core/architecture** | ~110 | PairingHandlerTest, ConnectionManagerTest, PluginManagerTest, DeviceInfoTest, DeviceHelperTest |
| **Transport layer** | ~40 | LanLinkProviderTest (27), BaseLinkProviderTest, BaseLinkTest |
| **Utility** | ~31 | UrlValidatorTest (71 — URL validation edge cases) |

### Detailed Test Coverage

#### Unit Tests (~50 tests)
- FFI library loading and initialization
- NetworkPacket creation and serialization
- Plugin FFI interface validation
- Certificate management
- Device discovery
- Performance benchmarking

#### Integration Tests (109 tests)
**Discovery & Pairing (24 tests):**
- Device discovery via UDP broadcast
- Identity packet processing
- Network state change handling
- Pairing request/accept flow
- Certificate exchange
- TLS handshake validation
- Pairing persistence

**File Transfer (19 tests):**
- Single file send (small, medium, large)
- Multiple file send
- File receive
- Progress tracking
- Error handling
- Concurrent transfers

**All Plugins (35 tests):**
- Battery status (send/receive)
- Clipboard sync (send/receive)
- Ping/pong (send/receive)
- Run command execution
- MPRIS media control
- Telephony notifications

**Lifecycle (8 tests):**
- Plugin load/unload
- Device connect/disconnect
- Service start/stop
- Resource cleanup

#### E2E Tests (31 tests)
**Android → COSMIC (15 tests):**
- Real network discovery
- Complete pairing flow
- File transfer to COSMIC Desktop
- Plugin data to desktop
- Complete user scenarios
- Network resilience

**COSMIC → Android (16 tests):**
- Receive pairing requests
- Receive files from desktop
- Receive plugin data
- Bidirectional communication
- System notifications
- Complete workflows

#### Performance Tests (14 benchmarks)
**FFI Performance (3 tests):**
- Call overhead: < 1ms ✅
- Packet serialization: < 5ms ✅
- Data transfer efficiency

**File Transfer (3 tests):**
- Small files: ≥ 5 MB/s ✅
- Large files: ≥ 20 MB/s ✅
- Concurrent transfers

**Network Performance (3 tests):**
- Discovery latency: < 5s ✅
- Pairing time: < 10s ✅
- Packet RTT: < 500ms ✅

**Memory & Stress (5 tests):**
- Memory profiling
- GC pressure analysis
- High-frequency packets (1000 pkt/s)
- Multiple connections (10 devices)
- Long-running stability (5 minutes)

### Test Infrastructure

**Framework Components:**
- `TestUtils.kt` - Common testing utilities
- `MockFactory.kt` - Test packet creation via FFI
- `ComposeTestUtils.kt` - UI testing helpers
- `FfiTestUtils.kt` - FFI layer testing
- `MockCosmicServer.kt` - Simulated COSMIC Desktop
- `MockCosmicClient.kt` - Simulated COSMIC client

**Documentation:**
- `docs/issue-28-integration-test-framework.md` - Test framework setup
- `docs/issue-30-integration-tests-discovery-pairing.md` - Discovery/pairing tests
- `docs/issue-31-integration-tests-file-transfer.md` - File transfer tests
- `docs/issue-32-integration-tests-all-plugins.md` - Plugin tests
- `docs/issue-33-e2e-test-android-to-cosmic.md` - Android → COSMIC E2E
- `docs/issue-34-e2e-test-cosmic-to-android.md` - COSMIC → Android E2E
- `docs/issue-35-performance-testing.md` - Performance benchmarks

### Testing Platform Documentation

**Comprehensive Testing Guides:**

- **[Waydroid Testing Guide](docs/testing/WAYDROID_TESTING_GUIDE.md)** - Complete guide for automated testing with Waydroid
  - Capabilities and limitations
  - NixOS configuration
  - Automation scripts usage
  - CI/CD integration
  - Performance comparison
  - Troubleshooting

- **[Waydroid NixOS Configuration](docs/testing/WAYDROID_NIXOS_CONFIG.md)** - Quick reference for NixOS setup
  - System configuration snippets
  - Flake integration examples
  - First-time setup steps
  - Command reference

- **[Waydroid Testing Summary](docs/testing/WAYDROID_TESTING_SUMMARY.md)** - Executive summary and decision guide
  - Quick decision matrix
  - Testing coverage breakdown
  - Recommended testing strategy
  - Cost-benefit analysis

- **[Samsung Tab S8 Ultra Testing Guide](docs/testing/SAMSUNG_TAB_TESTING_GUIDE.md)** - Real device testing guide
  - Device specifications
  - Initial setup (USB and Wireless ADB)
  - Bluetooth testing scenarios
  - Automation scripts
  - Troubleshooting

**Testing Scripts:**

All testing automation scripts are located in `scripts/`:

- `test-waydroid.sh` - Automated Waydroid testing workflow
  - Supports `--headless` for CI/CD
  - Supports `--quick` for rapid iteration
  - Full test suite execution
  - Test report generation

- `test-samsung.sh` - Automated Samsung tablet testing
  - Supports `--wireless` for ADB over Wi-Fi
  - Supports `--quick` for skip clean build
  - Device verification
  - Permission grants
  - Full test execution

- `test-bluetooth.sh` - Bluetooth-specific testing
  - Real-time log monitoring
  - 6 manual test scenarios
  - Bluetooth permission handling
  - Color-coded output

- `connect-tablet.sh` - Quick wireless ADB connection helper
  - Automatic device verification
  - Troubleshooting guidance

**Performance Metrics:**

- **Waydroid**: 5-10 second boot time, ~500 MB RAM usage, near-native performance
- **Samsung Tablet**: Real hardware performance, Bluetooth 5.2 support, Android 14
- **CI/CD Ready**: Headless Waydroid mode for fully automated testing pipelines

**Recommended Workflow:**

```bash
# Daily development (fast iteration)
./scripts/test-waydroid.sh --quick

# Weekly Bluetooth validation
./scripts/connect-tablet.sh
./scripts/test-bluetooth.sh <device-serial>

# Pre-release comprehensive testing
./scripts/test-waydroid.sh          # Full Waydroid suite
./scripts/test-samsung.sh --wireless # Full real device validation
```

## Contributing

Contributions are welcome! Please:

1. **Check documentation**: Review [docs/INDEX.md](docs/INDEX.md) for relevant guides
2. **Understand priorities**: See [docs/guides/PROJECT_PLAN.md](docs/guides/PROJECT_PLAN.md)
3. **Follow patterns**: Reference [docs/guides/IMPLEMENTATION_GUIDE.md](docs/guides/IMPLEMENTATION_GUIDE.md)
4. **Write tests**: Add FFI validation tests for new functionality
5. **Update docs**: Keep documentation current with code changes

### Development Standards

- **Code Style**: Follow Kotlin conventions, use ktlint
- **Commits**: Clear, descriptive commit messages
- **PRs**: Include tests and documentation updates
- **FFI Changes**: Update both Rust and Kotlin sides
- **Breaking Changes**: Discuss in issues first

## Credits

This project builds upon the excellent foundation of [KDE Connect](https://community.kde.org/KDEConnect):

**Original KDE Connect repositories**:
- Desktop: https://invent.kde.org/network/kdeconnect-kde
- Android: https://invent.kde.org/network/kdeconnect-android

We are grateful to the KDE team for:
- Creating the original KDE Connect application and protocol
- Years of development and refinement
- Building a robust, secure communication protocol
- Establishing the open-source foundation we build upon

COSMIC Connect modernizes the Android app and adapts it for COSMIC Desktop while maintaining **full protocol compatibility** (v8).

## Related Projects

- [cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core) - Shared Rust core library
- [cosmic-connect-desktop-app](https://github.com/olafkfreund/cosmic-connect-desktop-app) - COSMIC Desktop app
- [COSMIC Desktop](https://github.com/pop-os/cosmic-epoch) - System76's COSMIC desktop environment
- [KDE Connect](https://community.kde.org/KDEConnect) - Original KDE Connect project

## License

This project inherits the **GPL-3.0** license from KDE Connect.

See [LICENSE](LICENSE) for full license text.

## Contact & Support

- **Issues**: [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- **Documentation**: [docs/INDEX.md](docs/INDEX.md)
- **COSMIC Desktop**: [System76 COSMIC](https://system76.com/cosmic)
- **KDE Connect**: [KDE Community](https://community.kde.org/KDEConnect)

---

<div align="center">

**Build**: ![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Tests](https://img.shields.io/badge/tests-787_passing-brightgreen)
![Plugins](https://img.shields.io/badge/plugins-30-blue)
![Compilation](https://img.shields.io/badge/errors-0-brightgreen)

**Last Updated**: 2026-02-06

**Made for COSMIC Desktop**

</div>
