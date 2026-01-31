# COSMIC Connect Android - Documentation Index

Last Updated: 2026-01-31

## Quick Navigation

- [Getting Started](#getting-started)
- [User Guides](#user-guides)
- [Architecture](#architecture)
- [Protocol Implementation](#protocol-implementation)
- [Issue Tracking](#issue-tracking)
- [Legacy Documentation](#legacy-documentation)

---

## Getting Started

Essential guides for setting up and understanding the project.

### Setup & Configuration
- [Getting Started Guide](guides/GETTING_STARTED.md) - Initial setup and first steps
- [Setup Guide](guides/SETUP_GUIDE.md) - Detailed environment setup
- [Setup Complete](guides/SETUP_COMPLETE.md) - Post-setup verification
- [Development Environments](guides/DEV_ENVIRONMENTS.md) - IDE and tool configuration
- [Quick Reference](guides/QUICK_REFERENCE.md) - Command cheat sheet

### Project Planning
- [Project Scope](guides/PROJECT_SCOPE.md) - Overall project objectives and boundaries
- [Project Plan](guides/PROJECT_PLAN.md) - Current development roadmap
- [Enable Issues First](guides/ENABLE_ISSUES_FIRST.md) - Issue workflow setup
- [Implementation Guide](guides/IMPLEMENTATION_GUIDE.md) - Development best practices

### Build & Development
- [Rust Build Setup](guides/rust-build-setup.md) - Rust toolchain and cargo-ndk configuration
- [Rust Extraction Plan](guides/rust-extraction-plan.md) - Strategy for extracting Rust core library
- [Optimization Roadmap](guides/optimization-roadmap.md) - Performance improvement plans
- [Project Status (2025-01-15)](guides/project-status-2025-01-15.md) - Historical snapshot

### FFI Integration
- [FFI Integration Guide](guides/FFI_INTEGRATION_GUIDE.md) - Complete guide for migrating plugins to FFI (based on PingPluginFFI.kt)

---

## User Guides

End-user documentation for features and troubleshooting.

### General
- [User Guide](USER_GUIDE.md) - Complete guide for using COSMIC Connect
- [FAQ](FAQ.md) - Frequently asked questions

### Camera Webcam Feature
- [Camera Webcam Guide](CAMERA_WEBCAM.md) - Use your phone as a wireless webcam
  - Desktop setup instructions (NixOS, Arch, Fedora, Ubuntu, openSUSE)
  - v4l2loopback configuration
  - Application setup (Firefox, Chrome, OBS, Zoom, Teams, Meet, Discord)
- [Camera Troubleshooting](CAMERA_TROUBLESHOOTING.md) - Solve camera streaming issues
  - Camera not detected
  - Black screen fixes
  - Latency and performance
  - v4l2loopback module issues
  - Application-specific solutions

### App Continuity Feature
- [App Continuity Guide](APP_CONTINUITY.md) - Open content across devices
  - Share URLs from Android to desktop browser
  - Send links from desktop to Android
  - File transfer and automatic opening
  - Supported URL schemes and content types
  - Security and privacy settings
- [App Continuity Troubleshooting](APP_CONTINUITY_TROUBLESHOOTING.md) - Solve sharing issues
  - URL not opening troubleshooting
  - File transfer problems
  - Notification and security prompt issues
  - Platform-specific solutions

---

## Architecture

System design and architectural documentation.

### Overall Architecture
- [Architecture Overview](architecture/ARCHITECTURE.md) - High-level system architecture
- [Architecture Changes Summary](architecture/ARCHITECTURE_CHANGES_SUMMARY.md) - Major architectural decisions

### FFI & Cross-Platform
- [FFI Design](architecture/ffi-design.md) - Foreign Function Interface design
- [FFI Wrapper API](architecture/ffi-wrapper-api.md) - Kotlin/Rust FFI wrapper specification
- [Applet Architecture](architecture/applet-architecture.md) - COSMIC Desktop applet design

---

## Protocol Implementation

KDE Connect protocol and network communication.

### Core Protocol
- [KDE Connect Protocol Debug](protocol/kdeconnect-protocol-debug.md) - Protocol debugging guide
- [KDE Connect Rust Implementation Guide](protocol/kdeconnect-rust-implementation-guide.md) - Rust implementation patterns

### Network Packet System
- [NetworkPacket FFI Integration](protocol/networkpacket-ffi-integration.md) - Packet handling across FFI
- [NetworkPacket Migration Pattern](protocol/networkpacket-migration-pattern.md) - Migration strategy
- [NetworkPacket Code Review Fixes](protocol/networkpacket-code-review-fixes.md) - Code quality improvements

### Security
- [TLS Certificate Security Model](protocol/tls-certificate-security-model.md) - Certificate generation and validation

### Plugin Implementations
- [Battery FFI Integration](protocol/battery-ffi-integration.md) - Battery status plugin
- [Ping FFI Integration](protocol/ping-ffi-integration.md) - Ping/pong plugin
- [Discovery FFI Integration](protocol/discovery-ffi-integration.md) - Device discovery system

---

## Issue Tracking

Completed and in-progress issue documentation.

### Phase 0: Rust Core Extraction (Complete)

**Issue #44: NetworkPacket Rust Implementation**
- Status: Complete
- See: [Issue 64 NetworkPacket Migration Progress](issues/issue-64-networkpacket-migration-progress.md)

**Issue #45: Discovery Service Implementation**
- Status: Complete
- No dedicated summary (integrated into FFI work)

**Issue #46: TLS/Certificate Management**
- Status: Complete
- See: [TLS Certificate Security Model](protocol/tls-certificate-security-model.md)

**Issue #47: Battery Plugin Rust Implementation**
- Status: Complete
- [Issue #47 Completion Summary](issues/issue-47-completion-summary.md)

**Issue #48: Ping Plugin Rust Implementation**
- Status: Complete
- [Issue #48 Completion Summary](issues/issue-48-completion-summary.md)

### Phase 1: Android NDK Integration (Complete)

**Issue #50: FFI Bindings Validation**
- Status: 95% Complete (blocked by build configuration)
- [Issue #50 Validation Plan](issues/issue-50-ffi-validation.md)
- [Issue #50 Validation Summary](issues/issue-50-validation-summary.md)
- [Issue #50 Progress Summary](issues/issue-50-progress-summary.md)

**Issue #51: cargo-ndk Build Integration**
- Status: 100% Complete
- [Issue #51 Integration Summary](issues/issue-51-cargo-ndk-integration-summary.md)
- [Issue #51 Completion Summary](issues/issue-51-completion-summary.md)

**Issue #52: Android FFI Wrapper**
- Status: Partially complete
- [Issue #52 Fix Summary](issues/issue-52-fix-summary.md)

---

## Legacy Documentation

Historical documentation that may be outdated but kept for reference.

- [Project Plan (Old)](legacy/PROJECT_PLAN_OLD.md) - Original project plan
- [Corrections Applied](legacy/CORRECTIONS_APPLIED.md) - Historical bug fixes
- [README Changelog](legacy/README_CHANGELOG.md) - README version history
- [Summary](legacy/SUMMARY.md) - Old project summary

**Note**: These documents are kept for historical reference but may not reflect current implementation.

---

## Document Organization

### Directory Structure

```
docs/
├── INDEX.md                 (this file)
├── architecture/            Architecture and design documents
│   ├── ARCHITECTURE.md
│   ├── ARCHITECTURE_CHANGES_SUMMARY.md
│   ├── applet-architecture.md
│   ├── ffi-design.md
│   └── ffi-wrapper-api.md
├── guides/                  Setup and development guides
│   ├── GETTING_STARTED.md
│   ├── SETUP_GUIDE.md
│   ├── DEV_ENVIRONMENTS.md
│   ├── PROJECT_PLAN.md
│   └── ...
├── issues/                  Issue completion summaries
│   ├── issue-47-completion-summary.md
│   ├── issue-48-completion-summary.md
│   ├── issue-50-*.md
│   └── issue-51-*.md
├── protocol/               Protocol implementation details
│   ├── kdeconnect-protocol-debug.md
│   ├── networkpacket-*.md
│   ├── *-ffi-integration.md
│   └── tls-certificate-security-model.md
└── legacy/                 Historical/outdated documents
    ├── PROJECT_PLAN_OLD.md
    └── ...
```

---

## What's Old vs New

### Old Code (Java/Kotlin Android App)

**Location**: `src/org/cosmic/cosmicconnect/` (excluding `src/org/cosmic/cosmicconnect/Core/`)

The original KDE Connect Android codebase, written in Java and Kotlin:
- Device management
- Plugin system (Java-based)
- Network communication (Java networking)
- UI components
- Background services

**Status**: Being gradually migrated to use Rust core library via FFI.

**Still Used For**:
- Android UI and Activities
- Android-specific integrations (notifications, permissions, etc.)
- Service lifecycle management
- Some plugins not yet ported to Rust

### New Code (Rust Core + FFI)

**Location**:
- Rust library: `cosmic-connect-core/` (separate repository)
- Kotlin FFI wrapper: `src/org/cosmic/cosmicconnect/Core/`
- Generated bindings: `src/uniffi/cosmic_connect_core/`
- Native libraries: `build/rustJniLibs/android/`

**Rust Core (`cosmic-connect-core` repository)**:
- NetworkPacket (Issue #44)
- Discovery service (Issue #45)
- TLS/Certificate management (Issue #46)
- Battery plugin (Issue #47)
- Ping plugin (Issue #48)
- Plugin trait system
- FFI interface (uniffi-rs)

**Status**: Core protocol implementation complete, building successfully for Android.

**Current Work**: FFI validation and integration testing (Issue #50).

### Hybrid Approach

The project currently uses both:
1. **Old Java/Kotlin code** for Android-specific functionality and UI
2. **New Rust core** for protocol implementation, crypto, and cross-platform logic
3. **FFI layer** (uniffi-generated Kotlin bindings) to bridge between them

---

## Moving Forward

### Immediate Next Steps (Issue #50)

**Goal**: Validate FFI bindings work correctly between Android and Rust core.

**Current Status**: 95% complete, blocked by AndroidX library version mismatch.

**Solutions Available**:
1. Update Nix environment to support SDK 35/36
2. Downgrade AndroidX libraries to SDK 34-compatible versions
3. Test FFI standalone without full APK build

**See**: [Issue #50 Progress Summary](issues/issue-50-progress-summary.md)

### Phase 2: Plugin Migration

Once FFI validation is complete (Issue #50):

1. **Issue #52**: Complete Android FFI wrapper layer
2. **Issue #53**: Migrate Share plugin to Rust
3. **Issue #54**: Migrate Clipboard plugin to Rust
4. **Issue #55**: Migrate Notification plugin to Rust
5. Continue migrating remaining plugins one by one

### Phase 3: UI Modernization

After plugin migration:

1. Update Android UI to use Jetpack Compose
2. Implement Material Design 3
3. Add dark mode support
4. Improve accessibility

### Phase 4: COSMIC Desktop Integration

Final phase:

1. Complete COSMIC applet (cosmic-applet-kdeconnect)
2. Test cross-platform communication (Android ↔ COSMIC)
3. Implement COSMIC-specific features
4. Release stable version

---

## Contribution Guidelines

### Working with Documentation

1. **Creating New Docs**: Add to appropriate subdirectory
2. **Updating INDEX.md**: Add entries when creating new documentation
3. **Issue Summaries**: Place in `issues/` with format `issue-NN-*.md`
4. **Architecture Changes**: Document in `architecture/` directory

### Code Organization

- **Old Code**: Legacy Java/Kotlin remains in `src/org/cosmic/cosmicconnect/`
- **New FFI Wrapper**: Place in `src/org/cosmic/cosmicconnect/Core/`
- **Rust Core**: Separate repository at `cosmic-connect-core/`
- **Tests**: FFI tests in `tests/org/cosmic/cosmicconnect/`

---

## Additional Resources

### External Documentation

- [KDE Connect Protocol](https://invent.kde.org/network/kdeconnect-kde)
- [COSMIC Desktop](https://github.com/pop-os/cosmic-epoch)
- [uniffi-rs Documentation](https://mozilla.github.io/uniffi-rs/)
- [Android NDK Guide](https://developer.android.com/ndk)

### Related Repositories

- [cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core) - Rust core library
- [cosmic-applet-kdeconnect](https://github.com/olafkfreund/cosmic-applet-kdeconnect) - COSMIC Desktop applet

---

**Last Updated**: 2026-01-16
**Document Version**: 1.0
**Maintained By**: Project Team
