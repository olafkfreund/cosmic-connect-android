# COSMIC Connect - Android App

> A modernized Android app for seamless device communication with COSMIC Desktop

**COSMIC Connect** enables your Android device to communicate effortlessly with your COSMIC Desktop computer, providing a native, modern Android experience built with the latest technologies.

## ✨ Features

- **Shared Clipboard**: Copy and paste seamlessly between your phone and COSMIC Desktop
- **Notification Sync**: Read and reply to your Android notifications from your desktop
- **File & URL Sharing**: Instantly share files and URLs between devices
- **Multimedia Remote Control**: Use your phone as a remote for media players
- **Virtual Touchpad & Keyboard**: Control your computer from your phone
- **Battery Monitoring**: View your phone's battery status on your desktop
- **Find My Phone**: Make your phone ring to locate it
- **Run Commands**: Execute predefined commands on your computer from your phone

All features work wirelessly over your existing Wi-Fi network using secure TLS encryption.

---

## 🎯 About This Project

**COSMIC Connect** is a modernized Android application designed specifically for **[COSMIC Desktop](https://system76.com/cosmic)** integration. This project uses a **hybrid Rust + Kotlin architecture** with a shared protocol implementation for maximum reliability and code reuse.

### 🦀 Hybrid Architecture

This project uses a unique **Rust + Kotlin hybrid approach**:

- **Rust Core** (`cosmic-connect-core`): Protocol implementation, networking, TLS, and crypto
- **Kotlin UI**: Android-specific features, UI, and system integration
- **70%+ Code Sharing**: Protocol logic shared between Android and COSMIC Desktop
- **Single Source of Truth**: Fix protocol bugs once, both platforms benefit
- **Memory Safety**: Rust's ownership system prevents entire classes of bugs

### Modern Android Stack

- ✅ **Kotlin** - Modern Android UI and platform integration
- ✅ **Rust Core** - Memory-safe protocol implementation via FFI
- ✅ **MVVM Architecture** - Clean separation of concerns
- ✅ **Jetpack Compose** - Modern declarative UI
- ✅ **Coroutines & Flow** - Efficient async operations
- ✅ **uniffi-rs** - Seamless Rust ↔ Kotlin FFI bindings
- ✅ **Material 3** - Beautiful, modern design
- ✅ **Android 14+** - Latest platform features
- ✅ **80%+ Test Coverage** - Reliable and maintainable

### COSMIC Desktop Integration

Works seamlessly with the [COSMIC Connect Desktop Applet](https://github.com/olafkfreund/cosmic-applet-cosmicconnect) built for COSMIC Desktop using Rust and libcosmic. Both platforms share the same Rust core library, ensuring 100% protocol compatibility.

---

## 🙏 Credit to KDE

This project is built upon the **excellent foundation** laid by the [COSMIC Connect](https://community.kde.org/COSMICConnect) project and team. We are deeply grateful to KDE for:

- Creating the original COSMIC Connect application and protocol
- Years of development and refinement
- Building a robust, secure communication protocol
- Establishing the open-source foundation we build upon

**Original COSMIC Connect repositories:**
- Desktop: https://invent.kde.org/network/cosmicconnect-kde
- Android: https://invent.kde.org/network/cosmicconnect-android

The COSMIC Connect team deserves immense credit for creating such an amazing cross-platform communication solution. This COSMIC Connect project modernizes the Android app and adapts it specifically for COSMIC Desktop while maintaining full protocol compatibility.

---

## 📱 Installation

### For Users

**Coming Soon!**

This modernized app will be available on:
- Google Play Store
- F-Droid
- Direct APK downloads from GitHub Releases

**Desktop Requirement**: You'll need the [COSMIC Connect Desktop Applet](https://github.com/olafkfreund/cosmic-applet-cosmicconnect) installed on your COSMIC Desktop system.

### For Developers

See [GETTING_STARTED.md](GETTING_STARTED.md) for complete setup instructions.

**Requirements**:
- Android Studio Hedgehog or later
- Rust 1.70+ with cargo-ndk and uniffi-bindgen
- Android NDK (for Rust compilation)
- NixOS users: Waydroid for testing

```bash
# Clone the repositories
git clone https://github.com/olafkfreund/cosmic-connect-android
git clone https://github.com/olafkfreund/cosmic-connect-core

# View the first setup issue
cd cosmic-connect-android
gh issue view 1

# Start development
# See GETTING_STARTED.md for full instructions including Rust setup
```

---

## 🏗️ Project Status

**Current Phase**: Rust Core Extraction (Phase 0 of 6)

We are actively building the hybrid Rust + Kotlin architecture:

| Phase | Status | Description |
|-------|--------|-------------|
| **Phase 0** | 🚧 In Progress | Rust Core Extraction |
| **Phase 1** | 📋 Planned | Foundation & FFI Setup |
| **Phase 2** | 📋 Planned | Core Modernization |
| **Phase 3** | 📋 Planned | Feature Implementation |
| **Phase 4** | 📋 Planned | Integration & Testing |
| **Phase 5** | 📋 Planned | Release |

**Timeline**: 16-20 weeks

**Repositories**:
- 📱 [cosmic-connect-android](https://github.com/olafkfreund/cosmic-connect-android) - This repo (Android app)
- 🦀 [cosmic-connect-core](https://github.com/olafkfreund/cosmic-connect-core) - Shared Rust library
- 🖥️ [cosmic-applet-cosmicconnect](https://github.com/olafkfreund/cosmic-applet-cosmicconnect) - COSMIC Desktop applet

Track our progress: [View All Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)

---

## 🤝 Contributing

We welcome contributions! This is an open-source project focused on bringing a modern COSMIC Connect experience to COSMIC Desktop.

### How to Contribute

1. **Read the Documentation**
   - [PROJECT_SCOPE.md](PROJECT_SCOPE.md) - Understand what we're building
   - [GETTING_STARTED.md](GETTING_STARTED.md) - Set up your dev environment
   - [PROJECT_PLAN.md](PROJECT_PLAN.md) - See all planned work

2. **Pick an Issue**
   - View [open issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
   - Look for issues labeled `good first issue`
   - Comment on the issue to claim it

3. **Development Setup**
   - Follow [Issue #1](https://github.com/olafkfreund/cosmic-connect-android/issues/1) for NixOS + Waydroid setup
   - See [GETTING_STARTED.md](GETTING_STARTED.md) for complete instructions

4. **Submit Your Work**
   - Fork the repository
   - Create a feature branch
   - Follow our coding standards (see `.claude/skills/android-development-SKILL.md`)
   - Write tests for your changes
   - Submit a pull request

### Development Standards

- **Languages**:
  - **Rust** for protocol implementation (cosmic-connect-core)
  - **Kotlin** for Android UI and platform integration
- **Architecture**: MVVM with Repository pattern + FFI bridge
- **UI**: Jetpack Compose with Material 3
- **Async**: Coroutines and Flow (Kotlin), tokio (Rust)
- **FFI**: uniffi-rs for Rust ↔ Kotlin bindings
- **Testing**: Aim for 80%+ coverage on both Rust and Kotlin
- **Code Style**: Follow Android Kotlin style guide and Rust conventions

### Using AI Assistance

This project is configured for [Claude Code](https://claude.ai/claude-code):

```bash
# Get help with development
claude-code "Help me with issue #N"

# Use specialized agents
claude-code --agent android-modernization "Modernize the Battery plugin"
```

See [CLAUDE.md](CLAUDE.md) for details on using AI assistance.

---

## 🧪 Testing

### Testing with COSMIC Desktop

You'll need the [COSMIC Connect Desktop Applet](https://github.com/olafkfreund/cosmic-applet-cosmicconnect) running to test functionality:

```bash
# Clone the desktop applet
git clone https://github.com/olafkfreund/cosmic-applet-cosmicconnect

# Run it (see applet repo for instructions)
```

### Android Testing on NixOS

This project supports **Waydroid** for Android testing on NixOS:

```bash
# Enable in configuration.nix
virtualisation.waydroid.enable = true;

# Initialize
sudo waydroid init
waydroid session start
waydroid show-full-ui
```

See [Issue #1](https://github.com/olafkfreund/cosmic-connect-android/issues/1) for complete NixOS setup.

---

## 📖 Documentation

| Document | Purpose |
|----------|---------|
| [PROJECT_SCOPE.md](PROJECT_SCOPE.md) | Clear project definition |
| [GETTING_STARTED.md](GETTING_STARTED.md) | Complete getting started guide (includes Rust setup) |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Hybrid Rust+Kotlin architecture details |
| [ARCHITECTURE_CHANGES_SUMMARY.md](ARCHITECTURE_CHANGES_SUMMARY.md) | Summary of architectural changes |
| [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) | Quick implementation guide for FFI |
| [PROJECT_PLAN_UPDATED.md](PROJECT_PLAN_UPDATED.md) | All 62 issues detailed |
| [CLAUDE.md](CLAUDE.md) | AI development assistance |
| [cosmicconnect-protocol-debug.md](cosmicconnect-protocol-debug.md) | Protocol reference |

---

## 🔒 Security & Privacy

- **TLS Encryption**: All communication is encrypted
- **Certificate Pinning**: Devices trust each other after initial pairing
- **Local Network Only**: No internet connection required
- **Open Source**: Full transparency - review the code yourself

---

## 📜 License

**GNU GPL v2** and **GNU GPL v3**

This project maintains the same licensing as the original COSMIC Connect:
- [GNU GPL v2](https://www.gnu.org/licenses/gpl-2.0.html)
- [GNU GPL v3](https://www.gnu.org/licenses/gpl-3.0.html)

---

## 🔗 Links

### This Project
- **Android App**: https://github.com/olafkfreund/cosmic-connect-android
- **COSMIC Desktop Applet**: https://github.com/olafkfreund/cosmic-applet-cosmicconnect
- **Issues**: https://github.com/olafkfreund/cosmic-connect-android/issues
- **Discussions**: https://github.com/olafkfreund/cosmic-connect-android/discussions

### COSMIC Desktop
- **COSMIC Desktop**: https://system76.com/cosmic
- **COSMIC Epoch**: https://github.com/pop-os/cosmic-epoch

### Original COSMIC Connect (Credit)
- **COSMIC Connect Desktop**: https://invent.kde.org/network/cosmicconnect-kde
- **COSMIC Connect Android**: https://invent.kde.org/network/cosmicconnect-android
- **KDE Community**: https://community.kde.org/COSMICConnect

---

## 💬 Community & Support

- **Issues**: [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- **Discussions**: [GitHub Discussions](https://github.com/olafkfreund/cosmic-connect-android/discussions)
- **COSMIC Desktop**: [COSMIC Chat](https://chat.pop-os.org/)

---

## 🌟 Project Goals

Our mission is to provide COSMIC Desktop users with a modern, reliable, and feature-rich Android companion app that:

- ✅ Works seamlessly with COSMIC Desktop
- ✅ Uses modern Android development practices
- ✅ Maintains protocol compatibility with COSMIC Connect
- ✅ Provides excellent user experience
- ✅ Is well-tested and maintainable
- ✅ Respects user privacy and security

---

## 🚀 Roadmap

**Phase 0** (Current): Rust Core Extraction (Weeks 1-3)
- Analyze COSMIC applet protocol code
- Extract NetworkPacket, Discovery, TLS to Rust
- Set up uniffi-rs FFI bindings
- Validate with COSMIC Desktop applet

**Phase 1**: Foundation & FFI Setup (Weeks 4-5)
- Android development environment + Rust toolchain
- Set up cargo-ndk in Android build
- Create Android FFI wrapper layer
- Codebase audits

**Phase 2**: Core Modernization (Weeks 6-9)
- Integrate NetworkPacket FFI
- Integrate TLS/Certificate FFI with Android Keystore
- Integrate Discovery FFI
- Gradle modernization

**Phase 3**: Feature Implementation (Weeks 10-14)
- Plugin architecture FFI bridge
- Implement 6 core plugins with Rust core
- Jetpack Compose UI
- MVVM architecture

**Phase 4**: Integration & Testing (Weeks 15-18)
- FFI integration testing (memory safety, concurrency)
- COSMIC Desktop integration testing
- Performance optimization
- Protocol compatibility validation

**Phase 5**: Release (Weeks 19-20)
- Beta testing
- Play Store release
- F-Droid release

See [PROJECT_PLAN_UPDATED.md](PROJECT_PLAN_UPDATED.md) and [ARCHITECTURE.md](ARCHITECTURE.md) for complete details.

---

## 📸 Screenshots

_Coming soon!_

---

## ❤️ Acknowledgments

- **COSMIC Connect Team**: For creating the original application and protocol
- **COSMIC Desktop Team**: For building an amazing desktop environment
- **System76**: For supporting open source development
- **All Contributors**: Everyone helping modernize this codebase

---

**Built with ❤️ for COSMIC Desktop users**

*This project is not affiliated with KDE but is built upon their excellent work with full appreciation and proper attribution.*
