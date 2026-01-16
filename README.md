# COSMIC Connect Android

A modernized Android application for seamless device communication with COSMIC Desktop.

## Overview

COSMIC Connect enables Android devices to communicate with COSMIC Desktop computers, providing features like clipboard sharing, notification sync, file transfer, and remote control. Built with a hybrid Rust + Kotlin architecture for maximum reliability and code reuse.

## Features

- **Shared Clipboard** - Copy and paste between phone and desktop
- **Notification Sync** - Read and reply to Android notifications from desktop
- **File & URL Sharing** - Transfer files and URLs between devices
- **Multimedia Remote Control** - Use phone as media player remote
- **Virtual Touchpad & Keyboard** - Control computer from phone
- **Battery Monitoring** - View phone battery status on desktop
- **Find My Phone** - Make phone ring to locate it
- **Run Commands** - Execute predefined commands remotely

All features work wirelessly over Wi-Fi using secure TLS encryption.

## Architecture

### Hybrid Approach

The project uses a **Rust core + Kotlin UI** architecture:

**Rust Core** (`cosmic-connect-core` repository)
- Protocol implementation (KDE Connect protocol v7)
- Network communication and device discovery
- TLS certificate management and encryption
- Plugin system (Battery, Ping, Share, etc.)
- FFI interface via uniffi-rs

**Kotlin/Android** (this repository)
- Android UI and Activities
- Platform-specific integrations
- Service lifecycle management
- FFI wrapper layer

**Benefits**:
- 70%+ code sharing with COSMIC Desktop applet
- Single source of truth for protocol implementation
- Memory safety via Rust's ownership system
- Fix bugs once, both platforms benefit

### Technology Stack

**Rust Core**:
- Rust 1.92+ with Android targets
- tokio for async runtime
- uniffi-rs 0.27 for FFI bindings
- rustls for TLS
- serde for serialization

**Android**:
- Kotlin for UI and platform integration
- Android SDK 34 (minimum SDK 23)
- Jetpack libraries (future: Compose)
- JNA for native library loading
- Gradle with cargo-ndk integration

## Project Status

**Current Phase**: FFI Integration and Validation

**Completed**:
- Phase 0: Rust core extraction (Issues #44-48) - 100% Complete
  - NetworkPacket implementation
  - Discovery service
  - TLS/Certificate management
  - Battery plugin
  - Ping plugin
- Issue #51: cargo-ndk build integration - 100% Complete
  - Native libraries building for all Android ABIs
  - Build tooling fully configured

**In Progress**:
- Issue #50: FFI bindings validation - 95% Complete
  - Native libraries: Built (9.3 MB across 4 architectures)
  - Kotlin bindings: Generated (124 KB)
  - Test suite: Written (10 comprehensive tests)
  - Blocker: AndroidX library version mismatch with Nix environment

**Next Steps**:
- Complete Issue #50 (FFI validation)
- Issue #52: Android FFI wrapper layer
- Plugin migration (Share, Clipboard, Notifications, etc.)
- UI modernization with Jetpack Compose

## Installation

### For Users

**Coming Soon** - This modernized app will be available on:
- Google Play Store
- F-Droid
- GitHub Releases (direct APK downloads)

**Desktop Requirement**: Install [cosmic-applet-kdeconnect](https://github.com/olafkfreund/cosmic-applet-kdeconnect) on your COSMIC Desktop system.

### For Developers

**Prerequisites**:
- NixOS with flakes enabled, OR
- Android Studio Hedgehog or later
- Rust 1.92+ with Android targets
- Android NDK 27.0.12077973
- cargo-ndk 4.1+

**Quick Start (NixOS)**:
```bash
# Clone repositories
git clone https://github.com/olafkfreund/cosmic-connect-android
git clone https://github.com/olafkfreund/cosmic-connect-core

# Enter development environment
cd cosmic-connect-android
nix develop

# Build native libraries
./gradlew cargoBuild

# Build APK
./gradlew assembleDebug
```

**Quick Start (Non-NixOS)**:
See [docs/guides/GETTING_STARTED.md](docs/guides/GETTING_STARTED.md) for detailed setup instructions.

## Documentation

All documentation is organized in the `docs/` directory:

**Quick Links**:
- [Documentation Index](docs/INDEX.md) - Complete documentation catalog
- [Getting Started](docs/guides/GETTING_STARTED.md) - Setup and first steps
- [Architecture](docs/architecture/ARCHITECTURE.md) - System design
- [Project Plan](docs/guides/PROJECT_PLAN.md) - Development roadmap

**Categories**:
- `docs/guides/` - Setup, development, and implementation guides
- `docs/architecture/` - System architecture and design documents
- `docs/protocol/` - KDE Connect protocol implementation details
- `docs/issues/` - Completed issue summaries and progress reports
- `docs/legacy/` - Historical documentation (reference only)

## Development

### Building

```bash
# Build Rust core library for Android
./gradlew cargoBuild

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Run FFI validation tests
./gradlew test --tests FFIValidationTest
```

### Project Structure

```
cosmic-connect-android/
├── src/                           # Android source code
│   ├── org/cosmic/cosmicconnect/  # Legacy Java/Kotlin code
│   ├── org/cosmic/cosmicconnect/Core/  # New FFI wrapper
│   └── uniffi/cosmic_connect_core/     # Generated FFI bindings
├── tests/                         # Test suite
│   └── org/cosmic/cosmicconnect/  # FFI validation tests
├── build/rustJniLibs/             # Built native libraries
├── docs/                          # Documentation
├── CLAUDE.md                      # Claude Code configuration
└── README.md                      # This file

cosmic-connect-core/               # Rust core library (separate repo)
├── src/
│   ├── protocol/                  # NetworkPacket, protocol types
│   ├── network/                   # Discovery, connections
│   ├── crypto/                    # TLS, certificates
│   ├── plugins/                   # Plugin implementations
│   └── ffi/                       # FFI interface
└── bindings/                      # Generated language bindings
```

### Code Organization

**Old Code** (Being Gradually Replaced):
- `src/org/cosmic/cosmicconnect/` (excluding `Core/`)
- Original Java/Kotlin KDE Connect Android implementation
- Still used for UI and Android-specific functionality

**New Code** (Active Development):
- `cosmic-connect-core/` - Rust protocol implementation
- `src/org/cosmic/cosmicconnect/Core/` - Kotlin FFI wrapper
- `src/uniffi/cosmic_connect_core/` - Generated bindings
- `build/rustJniLibs/` - Compiled native libraries

## Contributing

Contributions are welcome! Please:

1. Check [docs/INDEX.md](docs/INDEX.md) for relevant documentation
2. Review [docs/guides/PROJECT_PLAN.md](docs/guides/PROJECT_PLAN.md) for current priorities
3. Follow established code patterns (see [docs/guides/IMPLEMENTATION_GUIDE.md](docs/guides/IMPLEMENTATION_GUIDE.md))
4. Write tests for new functionality
5. Update documentation as needed

## Testing

The project includes comprehensive FFI validation tests:

```bash
# Run all tests
./gradlew test

# Run specific FFI tests
./gradlew test --tests FFIValidationTest

# Run on Waydroid (NixOS)
waydroid session start
./gradlew installDebug
```

Test coverage includes:
- Native library loading
- FFI call overhead and performance
- Packet creation, serialization, deserialization
- Plugin system functionality
- End-to-end packet flow

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

COSMIC Connect modernizes the Android app and adapts it for COSMIC Desktop while maintaining full protocol compatibility.

## Related Projects

- [cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core) - Shared Rust core library
- [cosmic-applet-kdeconnect](https://github.com/olafkfreund/cosmic-applet-kdeconnect) - COSMIC Desktop applet
- [COSMIC Desktop](https://github.com/pop-os/cosmic-epoch) - System76's COSMIC desktop environment

## License

This project inherits the GPL-3.0 license from KDE Connect.

See [LICENSE](LICENSE) for details.

## Contact & Support

- Issues: [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- Documentation: [docs/INDEX.md](docs/INDEX.md)
- COSMIC Desktop: [System76 COSMIC](https://system76.com/cosmic)

---

**Status**: Active Development - Phase 1 (FFI Integration)

**Last Updated**: 2026-01-16
