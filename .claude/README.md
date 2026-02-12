# COSMIC Connect Skills & Agents Package

This package contains comprehensive Claude Code skills and agent configurations for modernizing the COSMIC Connect Android app to work seamlessly with the COSMIC Desktop applet.

## 📦 Package Contents

```
cosmic-connect-skills/
├── skills/                          # Claude Code Skills
│   ├── android-development-SKILL.md   # Modern Android development
│   ├── cosmic-desktop-SKILL.md        # COSMIC Desktop & Rust
│   ├── gradle-SKILL.md                # Gradle build system
│   ├── tls-networking-SKILL.md        # TLS & COSMIC Connect protocol
│   └── debugging-SKILL.md             # Debugging for both platforms
├── agents/                          # Agent Configurations
│   ├── android-modernization-agent.md # Android app modernization
│   ├── cosmic-desktop-agent.md        # COSMIC Desktop development
│   └── protocol-compatibility-agent.md # Cross-platform compatibility
└── README.md                        # This file
```

## 🎯 Purpose

This package helps you:
- Modernize the COSMIC Connect Android app (Java → Kotlin, modern architecture)
- Ensure protocol compatibility with COSMIC Desktop applet
- Implement secure TLS communication
- Build and debug both Android and Rust codebases
- Create a seamless cross-platform experience

## 🚀 Quick Start

### 1. Installation

Extract this package into your project repository:

```bash
# For cosmic-connect-android repo
cd /path/to/cosmic-connect-android
tar -xzf cosmic-connect-skills.tar.gz
mv cosmic-connect-skills/.claude .claude
```

Or place it in a shared location:

```bash
# Place in home directory for access from both repos
cd ~
tar -xzf cosmic-connect-skills.tar.gz
```

### 2. Using Skills with Claude Code

Skills are automatically available when you use Claude Code in the directory containing `.claude/`:

```bash
# Ask Claude to use specific skills
claude-code "Using the android-development skill, convert Device.java to Kotlin with coroutines"

claude-code "Using the cosmic-desktop skill, add MPRIS control UI to the applet"

claude-code "Using the tls-networking skill, implement certificate pinning for device pairing"
```

### 3. Invoking Agents

Agents provide focused expertise for specific tasks:

```bash
# Android modernization
claude-code --agent android-modernization "Modernize the battery plugin with MVVM architecture"

# COSMIC Desktop development
claude-code --agent cosmic-desktop "Implement file picker integration for share plugin"

# Protocol compatibility
claude-code --agent protocol-compatibility "Verify identity packet format compatibility"
```

## 📚 Skills Overview

### 1. Android Development Skill
**File:** `skills/android-development-SKILL.md`

**What it covers:**
- Modern Android architecture (MVVM, Repository pattern)
- Kotlin best practices and coroutines
- COSMIC Connect protocol implementation in Android
- Services, BroadcastReceivers, and Android components
- Jetpack Compose UI development
- Dependency injection with Hilt
- Testing strategies (Unit, Integration, UI tests)

**Use when:**
- Converting Java to Kotlin
- Implementing new Android features
- Refactoring to modern architecture
- Working with Android-specific APIs
- Building UI components

### 2. COSMIC Desktop Skill
**File:** `skills/cosmic-desktop-SKILL.md`

**What it covers:**
- libcosmic framework and iced UI toolkit
- COSMIC panel applet development
- Wayland Layer Shell protocol
- DBus integration with zbus
- Async programming with tokio
- COSMIC notifications and system integration
- File picker (XDG Desktop Portal)
- MPRIS media control

**Use when:**
- Building COSMIC applet UI
- Implementing daemon functionality
- Integrating with COSMIC Desktop
- Working with Wayland protocols
- Adding system-level features

### 3. Gradle Build System Skill
**File:** `skills/gradle-SKILL.md`

**What it covers:**
- Modern Gradle Kotlin DSL
- Android build configuration
- Dependency management and version catalogs
- ProGuard/R8 optimization
- Build variants and product flavors
- Custom Gradle tasks
- CI/CD integration
- Build optimization techniques

**Use when:**
- Modernizing build scripts
- Adding dependencies
- Configuring build variants
- Optimizing build performance
- Setting up CI/CD pipelines

### 4. TLS & Networking Skill
**File:** `skills/tls-networking-SKILL.md`

**What it covers:**
- COSMIC Connect protocol (v7/8)
- TLS certificate management (Android & Rust)
- Self-signed certificate generation
- UDP multicast device discovery
- TCP connection establishment
- Secure communication patterns
- Payload file transfer
- Certificate pinning and validation

**Use when:**
- Implementing device discovery
- Setting up TLS connections
- Handling certificate management
- Implementing file transfer
- Debugging network issues

### 5. Debugging Skill
**File:** `skills/debugging-SKILL.md`

**What it covers:**
- Android debugging (Logcat, Android Studio, Profiler)
- Rust debugging (GDB, LLDB, tracing)
- Network traffic analysis (Wireshark, tcpdump)
- Memory profiling and leak detection
- TLS debugging and certificate validation
- Performance profiling
- Testing strategies for both platforms

**Use when:**
- Troubleshooting issues
- Analyzing network traffic
- Profiling performance
- Finding memory leaks
- Debugging TLS handshakes

## 🤖 Agent Configurations

### Android Modernization Agent
**Focus:** Modernizing the Android app codebase

**Specializes in:**
- Java → Kotlin conversion
- MVVM architecture implementation
- Coroutines for async operations
- Jetpack Compose UI
- Modern Android best practices

**Example tasks:**
```bash
claude-code --agent android-modernization "Convert NetworkPacket class to Kotlin data class"
claude-code --agent android-modernization "Implement BatteryPlugin with ViewModel"
claude-code --agent android-modernization "Add Jetpack Compose UI for device list"
```

### COSMIC Desktop Agent
**Focus:** COSMIC Desktop applet and daemon development

**Specializes in:**
- libcosmic applet development
- Rust async programming
- DBus service implementation
- System integration
- Desktop notifications

**Example tasks:**
```bash
claude-code --agent cosmic-desktop "Add battery status indicator to applet"
claude-code --agent cosmic-desktop "Implement clipboard sync plugin"
claude-code --agent cosmic-desktop "Create integration test for file sharing"
```

### Protocol Compatibility Agent
**Focus:** Cross-platform protocol compatibility

**Specializes in:**
- Protocol version compatibility
- Packet format validation
- Feature parity testing
- Integration testing
- Cross-platform debugging

**Example tasks:**
```bash
claude-code --agent protocol-compatibility "Verify pairing flow compatibility"
claude-code --agent protocol-compatibility "Test file transfer Android → COSMIC"
claude-code --agent protocol-compatibility "Debug TLS handshake issue"
```

## 🔧 Integration Guide

### For cosmic-connect-android Repository

1. **Place skills in repository:**
```bash
cd cosmic-connect-android
mkdir -p .claude/skills .claude/agents
cp /path/to/cosmic-connect-skills/skills/* .claude/skills/
cp /path/to/cosmic-connect-skills/agents/* .claude/agents/
```

2. **Configure Claude Code:**
Create `.claude/config.yaml`:
```yaml
skills:
  - android-development-SKILL.md
  - gradle-SKILL.md
  - tls-networking-SKILL.md
  - debugging-SKILL.md

agents:
  android-modernization:
    skills:
      - android-development-SKILL.md
      - gradle-SKILL.md
      - tls-networking-SKILL.md
      - debugging-SKILL.md
  protocol-compatibility:
    skills:
      - android-development-SKILL.md
      - tls-networking-SKILL.md
      - debugging-SKILL.md
```

3. **Start modernizing:**
```bash
# Convert a file to Kotlin
claude-code "Modernize src/org/cosmic/cosmicconnect/Device.java"

# Implement modern architecture
claude-code "Refactor DeviceManager to use Repository pattern and coroutines"

# Update build system
claude-code "Convert build.gradle to build.gradle.kts with Kotlin DSL"
```

### For cosmic-applet-cosmicconnect Repository

1. **Place skills in repository:**
```bash
cd cosmic-applet-cosmicconnect
mkdir -p .claude/skills .claude/agents
cp /path/to/cosmic-connect-skills/skills/* .claude/skills/
cp /path/to/cosmic-connect-skills/agents/* .claude/agents/
```

2. **Configure Claude Code:**
Create `.claude/config.yaml`:
```yaml
skills:
  - cosmic-desktop-SKILL.md
  - tls-networking-SKILL.md
  - debugging-SKILL.md

agents:
  cosmic-desktop:
    skills:
      - cosmic-desktop-SKILL.md
      - tls-networking-SKILL.md
      - debugging-SKILL.md
  protocol-compatibility:
    skills:
      - cosmic-desktop-SKILL.md
      - tls-networking-SKILL.md
      - debugging-SKILL.md
```

3. **Start developing:**
```bash
# Add new feature
claude-code "Implement MPRIS media control UI in applet popup"

# Fix bug
claude-code "Debug TLS handshake timeout in pairing flow"

# Improve integration
claude-code "Add COSMIC notification for low battery alerts"
```

## 💡 Usage Examples

### Example 1: Modernizing Android Code
```bash
# Step 1: Convert Java to Kotlin
claude-code "Convert src/org/cosmic/cosmicconnect/NetworkPacket.java to Kotlin"

# Step 2: Add modern patterns
claude-code "Refactor NetworkPacket to use Kotlin data class and sealed classes for state"

# Step 3: Implement coroutines
claude-code "Replace callback-based networking with coroutines in DeviceConnection"

# Step 4: Add tests
claude-code "Create unit tests for NetworkPacket serialization/deserialization"
```

### Example 2: Implementing New Feature
```bash
# Android side
claude-code "Implement RunCommand plugin for Android with command storage"

# COSMIC side
claude-code "Implement RunCommand plugin UI in COSMIC applet"

# Test compatibility
claude-code --agent protocol-compatibility "Test RunCommand plugin between Android and COSMIC"
```

### Example 3: Debugging Issue
```bash
# Analyze problem
claude-code "Debug TLS handshake failure between Android and COSMIC"

# Check Android side
claude-code "Verify certificate generation in Android CertificateManager"

# Check COSMIC side
claude-code "Verify TLS configuration in Rust TLSManager"

# Test fix
claude-code "Test TLS connection with updated certificate validation"
```

## 📖 Best Practices

### When Using Skills

1. **Be specific about which skill to use:**
   ```bash
   # Good
   claude-code "Using android-development skill, implement MVVM pattern for BatteryPlugin"
   
   # Better
   claude-code "Apply android-development and debugging skills to refactor and test BatteryPlugin"
   ```

2. **Reference code examples from skills:**
   ```bash
   claude-code "Follow the Repository pattern example from android-development skill to refactor DeviceRepository"
   ```

3. **Combine skills for complex tasks:**
   ```bash
   claude-code "Using android-development, gradle, and debugging skills, modernize and test the Share plugin"
   ```

### When Using Agents

1. **Use appropriate agent for the task:**
   - Use `android-modernization` for Android-specific work
   - Use `cosmic-desktop` for COSMIC/Rust work
   - Use `protocol-compatibility` for cross-platform issues

2. **Provide context:**
   ```bash
   claude-code --agent protocol-compatibility "Test file sharing. The Android app sends files correctly but COSMIC doesn't receive them."
   ```

3. **Iterate on solutions:**
   ```bash
   # First attempt
   claude-code --agent android-modernization "Convert Device.java to Kotlin"
   
   # Refine
   claude-code --agent android-modernization "The converted Device.kt needs proper null safety and coroutine support"
   ```

## 🔍 Skill Reference Guide

### Quick Lookup: Which Skill to Use?

| Task | Skill | Section |
|------|-------|---------|
| Convert Java to Kotlin | android-development | Kotlin Best Practices |
| Implement ViewModel | android-development | Modern Android Architecture |
| Set up Gradle build | gradle | Modern Gradle Project Structure |
| Generate TLS certificate | tls-networking | TLS Certificate Management |
| UDP device discovery | tls-networking | Network Programming |
| Debug network issues | debugging | Network Debugging |
| Create COSMIC applet | cosmic-desktop | COSMIC Applet Development |
| Implement DBus interface | cosmic-desktop | DBus Integration |
| Profile performance | debugging | Performance Profiling |
| Write unit tests | android-development / cosmic-desktop | Testing |

## 🛠️ Troubleshooting

### Skills not loading?
1. Verify skills are in `.claude/skills/` directory
2. Check file names end with `-SKILL.md`
3. Ensure Claude Code has access to the directory

### Agent not working?
1. Verify agent config in `.claude/agents/`
2. Check agent references correct skill files
3. Use `--agent` flag correctly: `claude-code --agent agent-name "task"`

### Protocol compatibility issues?
1. Use `protocol-compatibility` agent
2. Check both implementations against tls-networking skill
3. Capture and analyze network traffic
4. Verify TLS certificate formats match

## 📝 Contributing

To extend or improve these skills:

1. **Add new examples** to relevant skill files
2. **Document common patterns** you discover
3. **Share debugging techniques** that work
4. **Update compatibility information** after testing

## 📄 License

These skills are provided to assist with the COSMIC Connect project, which is licensed under GPL-2.0/GPL-3.0.

## 🙏 Acknowledgments

Based on:
- [COSMIC Connect Android](https://invent.kde.org/network/cosmic-connect-android)
- [cosmic-applet-cosmicconnect](https://github.com/olafkfreund/cosmic-applet-cosmicconnect)
- COSMIC Connect Protocol Specification
- COSMIC Desktop Development Guide

## 📞 Support

For issues or questions:
- Check the debugging skill for troubleshooting steps
- Review relevant skills for best practices
- Use appropriate agent for focused assistance
- Consult protocol-compatibility agent for cross-platform issues

---

**Happy Coding! 🚀**

Use these skills and agents to create a seamless, modern COSMIC Connect experience across Android and COSMIC Desktop!
