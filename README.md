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

**COSMIC Connect** is a modernized Android application designed specifically for **[COSMIC Desktop](https://system76.com/cosmic)** integration. This project takes the proven COSMIC Connect protocol and brings it to COSMIC Desktop with a completely modernized Android codebase.

### Modern Android Stack

- ✅ **Kotlin** - Modern, safe, and concise code (converting from Java)
- ✅ **MVVM Architecture** - Clean separation of concerns
- ✅ **Jetpack Compose** - Modern declarative UI
- ✅ **Coroutines & Flow** - Efficient async operations
- ✅ **Material 3** - Beautiful, modern design
- ✅ **Android 14+** - Latest platform features
- ✅ **80%+ Test Coverage** - Reliable and maintainable

### COSMIC Desktop Integration

Works seamlessly with the [COSMIC Connect Desktop Applet](https://github.com/olafkfreund/cosmic-applet-cosmicconnect) built specifically for COSMIC Desktop using Rust and libcosmic.

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

```bash
# Clone the repository
git clone https://github.com/olafkfreund/cosmic-connect-android
cd cosmic-connect-android

# View the first setup issue
gh issue view 1

# Start development
# See GETTING_STARTED.md for full instructions
```

---

## 🏗️ Project Status

**Current Phase**: Foundation & Setup (Phase 1 of 5)

We are actively modernizing the Android codebase:

| Phase | Status | Description |
|-------|--------|-------------|
| **Phase 1** | 🚧 In Progress | Foundation & Setup |
| **Phase 2** | 📋 Planned | Core Modernization |
| **Phase 3** | 📋 Planned | Feature Implementation |
| **Phase 4** | 📋 Planned | Integration & Testing |
| **Phase 5** | 📋 Planned | Release |

**Timeline**: 12-16 weeks

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

- **Language**: Kotlin (we're converting from Java)
- **Architecture**: MVVM with Repository pattern
- **UI**: Jetpack Compose with Material 3
- **Async**: Coroutines and Flow
- **Testing**: Aim for 80%+ coverage
- **Code Style**: Follow Android Kotlin style guide

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
| [GETTING_STARTED.md](GETTING_STARTED.md) | Complete getting started guide |
| [PROJECT_PLAN.md](PROJECT_PLAN.md) | All 41 issues detailed |
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

**Phase 1** (Current): Foundation & Setup
- Development environment
- Codebase audits
- Protocol testing

**Phase 2**: Core Modernization
- Gradle modernization
- NetworkPacket → Kotlin
- TLS & Certificate management
- Device discovery

**Phase 3**: Feature Implementation
- Plugin modernization
- Jetpack Compose UI
- MVVM architecture

**Phase 4**: Integration & Testing
- Comprehensive testing
- COSMIC Desktop integration testing
- Performance optimization

**Phase 5**: Release
- Beta testing
- Play Store release
- F-Droid release

See [PROJECT_PLAN.md](PROJECT_PLAN.md) for complete roadmap.

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
