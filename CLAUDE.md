# Using Claude Code with Cosmic Connect Android

This repository is configured to work with [Claude Code](https://docs.claude.ai/en/docs/claude-code-overview), an AI-powered development assistant that helps modernize and maintain this KDE Connect Android implementation for COSMIC Desktop integration.

## 📋 Table of Contents

- [Quick Start](#quick-start)
- [Available Skills](#available-skills)
- [Available Agents](#available-agents)
- [Common Tasks](#common-tasks)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## 🚀 Quick Start

### Prerequisites

1. **Install Claude Code**: Follow the [installation guide](https://docs.claude.com/en/docs/claude-code-installation)
2. **Navigate to this repository**: `cd cosmic-connect-android`
3. **Verify configuration**: `claude-code --version`

### Your First Command

```bash
# Convert a Java file to Kotlin
claude-code "Convert src/org/kde/kdeconnect/Device.java to modern Kotlin"

# Or use an agent for complex tasks
claude-code --agent android-modernization "Refactor the entire device discovery module"
```

## 🎯 Available Skills

This repository includes specialized Claude Code skills located in `.claude/skills/`:

### 1. **android-development-SKILL.md**
Modern Android development patterns for KDE Connect protocol implementation.

**What it covers:**
- Java to Kotlin migration strategies
- MVVM architecture with Jetpack components
- Coroutines for async operations
- Android 14+ compatibility
- KDE Connect protocol implementation
- Background services and foreground services
- Runtime permissions handling

**Use when:**
- Modernizing Java code
- Implementing new Android features
- Refactoring architecture
- Adding Android-specific functionality

### 2. **cosmic-desktop-SKILL.md**
COSMIC Desktop integration and Rust development for the companion applet.

**What it covers:**
- libcosmic applet development
- Wayland Layer Shell protocol
- iced framework widgets
- DBus integration
- Daemon architecture
- COSMIC notification integration

**Use when:**
- Working on the companion COSMIC applet
- Implementing desktop integration features
- Adding UI components
- Debugging Rust/COSMIC issues

### 3. **gradle-SKILL.md**
Modern Gradle build system configuration and optimization.

**What it covers:**
- Kotlin DSL (build.gradle.kts)
- Version catalogs
- Build variants and flavors
- Dependency management
- ProGuard/R8 configuration
- Build caching and optimization

**Use when:**
- Updating build configuration
- Adding new dependencies
- Optimizing build times
- Configuring release builds

### 4. **tls-networking-SKILL.md**
Secure communication and KDE Connect protocol implementation.

**What it covers:**
- TLS/SSL certificate management
- UDP device discovery
- TCP payload transfers
- KDE Connect protocol packets
- Network error handling
- Certificate pinning

**Use when:**
- Implementing secure communication
- Debugging connection issues
- Adding new KDE Connect plugins
- Testing cross-platform compatibility

### 5. **debugging-SKILL.md**
Comprehensive debugging techniques for both Android and Rust.

**What it covers:**
- Android Logcat analysis
- Profiling and performance
- Rust debugging with GDB/LLDB
- Wireshark network analysis
- Unit and integration testing
- Common issue patterns

**Use when:**
- Troubleshooting bugs
- Analyzing performance
- Testing protocol compatibility
- Verifying security

## 🤖 Available Agents

Agents are pre-configured combinations of skills for specific workflows, located in `.claude/agents/`:

### 1. **android-modernization**
Specialized in modernizing the Android codebase.

**Includes skills:**
- android-development
- gradle
- tls-networking
- debugging

**Best for:**
- Converting Java to Kotlin
- Implementing MVVM architecture
- Adding coroutines
- Modernizing build system
- Updating to latest Android APIs

**Example usage:**
```bash
claude-code --agent android-modernization "Modernize the entire Bluetooth plugin"
```

### 2. **cosmic-desktop**
Specialized in COSMIC Desktop applet development.

**Includes skills:**
- cosmic-desktop
- tls-networking
- debugging

**Best for:**
- Building applet features
- Implementing desktop notifications
- Adding UI components
- DBus communication
- Daemon services

**Example usage:**
```bash
claude-code --agent cosmic-desktop "Add media control widget to applet"
```

### 3. **protocol-compatibility**
Specialized in ensuring cross-platform protocol compatibility.

**Includes skills:**
- android-development
- cosmic-desktop
- tls-networking
- debugging

**Best for:**
- Testing Android ↔ COSMIC communication
- Debugging protocol issues
- Implementing new plugins on both platforms
- Verifying security

**Example usage:**
```bash
claude-code --agent protocol-compatibility "Test file sharing between Android and COSMIC"
```

## 💡 Common Tasks

### Java to Kotlin Conversion

```bash
# Convert a single file
claude-code "Convert NetworkPacket.java to Kotlin data class"

# Convert with modernization
claude-code "Convert Device.java to Kotlin with coroutines and StateFlow"

# Convert an entire module
claude-code --agent android-modernization "Convert all network classes to Kotlin"
```

### Implementing New Features

```bash
# Android side
claude-code "Implement RunCommand plugin with command storage and execution"

# COSMIC side
claude-code --agent cosmic-desktop "Create RunCommand UI in applet popup"

# Test both sides
claude-code --agent protocol-compatibility "Verify RunCommand works between platforms"
```

### Refactoring Architecture

```bash
# Apply MVVM pattern
claude-code "Refactor DeviceManager to use Repository pattern and ViewModel"

# Add dependency injection
claude-code "Implement Hilt dependency injection for network layer"

# Modernize storage
claude-code "Replace SharedPreferences with DataStore in device settings"
```

### Debugging Issues

```bash
# Network issues
claude-code "Debug TLS handshake failure during device pairing"

# Performance issues
claude-code "Profile battery drain in background service and optimize"

# Protocol issues
claude-code --agent protocol-compatibility "Analyze packet format mismatch in file transfer"
```

### Build System Updates

```bash
# Convert to Kotlin DSL
claude-code "Convert build.gradle to build.gradle.kts with version catalog"

# Update dependencies
claude-code "Update all dependencies to latest stable versions"

# Optimize build
claude-code "Implement Gradle caching and parallel execution"
```

### Testing

```bash
# Unit tests
claude-code "Create unit tests for NetworkPacket serialization"

# Integration tests
claude-code "Create integration test for device discovery flow"

# Protocol tests
claude-code "Create test suite for all KDE Connect packet types"
```

## 🎓 Best Practices

### 1. Be Specific About Which Skill to Use

```bash
# Good
claude-code "Using android-development skill, implement MVVM for BatteryPlugin"

# Better
claude-code "Using android-development and debugging skills, refactor and test BatteryPlugin"
```

### 2. Provide Context for Complex Tasks

```bash
# Include relevant details
claude-code "Using tls-networking skill, implement certificate rotation for device pairing. Current implementation uses static certificates which expire."
```

### 3. Use Agents for Multi-Step Workflows

```bash
# Agent handles the complexity
claude-code --agent android-modernization "Modernize the Share plugin: convert to Kotlin, implement MVVM, add coroutines, create tests"
```

### 4. Reference Code Examples from Skills

```bash
# Skills contain patterns to follow
claude-code "Follow the Repository pattern example from android-development skill to refactor DeviceRepository"
```

### 5. Combine Skills When Needed

```bash
# Multiple skills provide comprehensive coverage
claude-code "Using android-development, gradle, and debugging skills, modernize and optimize the Telephony plugin"
```

### 6. Iterate on Results

```bash
# First pass
claude-code "Convert BluetoothManager.java to Kotlin"

# Review and improve
claude-code "Add error handling and logging to BluetoothManager"

# Add tests
claude-code "Create unit tests for BluetoothManager"
```

### 7. Use Protocol Compatibility Agent for Cross-Platform Work

```bash
# Always verify compatibility
claude-code --agent protocol-compatibility "After implementing clipboard sync on Android, verify it works with COSMIC applet"
```

## 🔧 Troubleshooting

### Claude Code Not Finding Skills

**Problem:** Skills are not being recognized

**Solution:**
```bash
# Verify skills directory exists
ls -la .claude/skills/

# Check config file
cat .claude/config.yaml

# Ensure skills are listed in config
```

### Agent Not Available

**Problem:** `claude-code --agent android-modernization` fails

**Solution:**
```bash
# Verify agents directory exists
ls -la .claude/agents/

# Check agent configuration
cat .claude/agents/android-modernization-agent.md

# Ensure agent is referenced in config
```

### Skill Producing Wrong Results

**Problem:** Skill suggests outdated or incorrect patterns

**Solution:**
```bash
# Be more specific about what you want
claude-code "Using android-development skill, implement WorkManager (not AsyncTask) for background sync"

# Reference specific examples
claude-code "Follow the ViewModel example from android-development skill"

# Provide additional context
claude-code "Using android-development skill, target Android 14 API level 34"
```

### Build Errors After Changes

**Problem:** Code generated by Claude doesn't compile

**Solution:**
```bash
# Ask Claude to fix the issue
claude-code "Fix compilation errors in DeviceManager.kt"

# Or provide error details
claude-code "Fix error: 'Unresolved reference: lifecycleScope' in DeviceFragment"

# Use debugging skill
claude-code "Using debugging skill, analyze and fix Gradle sync errors"
```

### Protocol Compatibility Issues

**Problem:** Android and COSMIC apps can't communicate

**Solution:**
```bash
# Use protocol compatibility agent
claude-code --agent protocol-compatibility "Debug pairing failure between Android and COSMIC"

# Analyze network traffic
claude-code "Using tls-networking and debugging skills, analyze Wireshark capture of failed handshake"

# Verify packet format
claude-code "Verify NetworkPacket serialization format matches KDE Connect spec"
```

## 📚 Additional Resources

- **Skills Documentation**: `.claude/skills/` - Detailed skill documentation
- **Agent Configuration**: `.claude/agents/` - Agent definitions and capabilities
- **Claude Code Docs**: https://docs.claude.com/en/docs/claude-code-overview
- **KDE Connect Protocol**: https://invent.kde.org/network/kdeconnect-kde
- **COSMIC Desktop**: https://github.com/pop-os/cosmic-epoch
- **Android Developers**: https://developer.android.com/

## 🤝 Contributing

When contributing to this repository using Claude Code:

1. **Use appropriate skills/agents** for your changes
2. **Test cross-platform compatibility** with protocol-compatibility agent
3. **Follow established patterns** from the skills
4. **Add tests** for new functionality
5. **Update documentation** if adding new features

## 📝 Examples of Successful Workflows

### Example 1: Complete Feature Implementation

```bash
# Step 1: Implement on Android
claude-code --agent android-modernization "Implement MPRIS plugin for media control"

# Step 2: Implement on COSMIC
claude-code --agent cosmic-desktop "Implement MPRIS UI in applet with play/pause/next/prev controls"

# Step 3: Test compatibility
claude-code --agent protocol-compatibility "Test MPRIS plugin between Android and COSMIC"

# Step 4: Add tests
claude-code "Create integration tests for MPRIS plugin"

# Step 5: Document
claude-code "Update README with MPRIS plugin usage"
```

### Example 2: Bug Fix Workflow

```bash
# Step 1: Reproduce and analyze
claude-code "Using debugging skill, analyze crash in TLS handshake from logcat"

# Step 2: Fix the issue
claude-code "Fix NullPointerException in CertificateManager during handshake"

# Step 3: Add error handling
claude-code "Add proper error handling and logging to TLS handshake flow"

# Step 4: Verify fix
claude-code --agent protocol-compatibility "Test TLS handshake with various certificate configurations"

# Step 5: Add regression test
claude-code "Create test case for TLS handshake error scenarios"
```

### Example 3: Performance Optimization

```bash
# Step 1: Profile
claude-code "Using debugging skill, identify battery drain sources in background service"

# Step 2: Optimize
claude-code "Optimize device discovery to reduce wake locks and CPU usage"

# Step 3: Measure
claude-code "Add performance metrics and logging to measure improvement"

# Step 4: Verify
claude-code "Create battery usage test to verify optimization"
```

## 🎯 Project Goals

This Claude Code configuration helps achieve:

- ✅ **Modernize codebase**: Java → Kotlin, modern Android patterns
- ✅ **COSMIC integration**: Seamless desktop integration
- ✅ **Protocol compatibility**: Ensure Android ↔ COSMIC communication works
- ✅ **Security**: Proper TLS implementation and certificate management
- ✅ **Performance**: Optimized background services and network usage
- ✅ **Testing**: Comprehensive test coverage
- ✅ **Maintainability**: Clean architecture and documentation

---

**Happy coding with Claude! 🚀**

For questions or issues with Claude Code configuration, refer to the skill documentation in `.claude/skills/` or the [Claude Code documentation](https://docs.claude.com/).
