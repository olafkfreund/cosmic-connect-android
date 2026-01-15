# COSMIC Connect Skills - Quick Start Guide

## 📦 What You Got

A comprehensive package of Claude Code skills and agents for modernizing your COSMIC Connect Android app to work with COSMIC Desktop.

**Package Size:** 39KB  
**Files Included:** 5 skills + 3 agents + documentation

## 🚀 Installation (2 minutes)

### Option 1: For cosmic-connect-android repository

```bash
# Navigate to your Android repo
cd ~/path/to/cosmic-connect-android

# Extract the archive
tar -xzf cosmic-connect-skills.tar.gz

# Move contents to .claude directory
mkdir -p .claude
mv cosmic-connect-skills/skills .claude/
mv cosmic-connect-skills/agents .claude/
mv cosmic-connect-skills/README.md .claude/

# Cleanup
rm -rf cosmic-connect-skills

# Verify installation
ls -la .claude/
```

### Option 2: For cosmic-applet-cosmicconnect repository

```bash
# Navigate to your COSMIC repo
cd ~/path/to/cosmic-applet-cosmicconnect

# Extract the archive
tar -xzf cosmic-connect-skills.tar.gz

# Move contents to .claude directory
mkdir -p .claude
mv cosmic-connect-skills/skills .claude/
mv cosmic-connect-skills/agents .claude/
mv cosmic-connect-skills/README.md .claude/

# Cleanup
rm -rf cosmic-connect-skills

# Verify installation
ls -la .claude/
```

### Option 3: Global installation (access from both repos)

```bash
# Create shared skills directory
mkdir -p ~/.claude/cosmic-connect
cd ~/.claude/cosmic-connect

# Extract archive
tar -xzf ~/Downloads/cosmic-connect-skills.tar.gz

# Create symlinks in each repository
cd ~/cosmic-connect-android
ln -s ~/.claude/cosmic-connect/cosmic-connect-skills .claude

cd ~/cosmic-applet-cosmicconnect
ln -s ~/.claude/cosmic-connect/cosmic-connect-skills .claude
```

## 💻 First Commands (Try These!)

### Android Modernization

```bash
# Convert Java file to Kotlin
claude-code "Using android-development skill, convert src/org/cosmic/cosmicconnect/Device.java to modern Kotlin with coroutines"

# Modernize build system
claude-code "Using gradle skill, convert build.gradle to build.gradle.kts with Kotlin DSL and version catalog"

# Implement modern architecture
claude-code "Using android-development skill, refactor DeviceManager to use MVVM with Repository pattern"
```

### COSMIC Desktop Development

```bash
# Add feature to applet
claude-code "Using cosmic-desktop skill, add battery status indicator to the applet popup"

# Implement plugin
claude-code "Using cosmic-desktop and tls-networking skills, implement clipboard sync plugin"

# Fix integration
claude-code "Using cosmic-desktop skill, integrate file picker with XDG Desktop Portal"
```

### Debugging & Testing

```bash
# Debug network issue
claude-code "Using debugging and tls-networking skills, debug TLS handshake failure between Android and COSMIC"

# Analyze traffic
claude-code "Using debugging skill, help me analyze the Wireshark capture for COSMIC Connect packets"

# Test compatibility
claude-code --agent protocol-compatibility "Test file sharing from Android to COSMIC Desktop"
```

## 🎯 Common Tasks

### 1. Modernize Android File

```bash
# Step-by-step approach
claude-code "Using android-development skill, analyze src/org/cosmic/cosmicconnect/NetworkPacket.java and suggest modernization strategy"

claude-code "Convert the file to Kotlin with data class and sealed classes"

claude-code "Add coroutine support and write unit tests"
```

### 2. Implement New Feature

```bash
# Android side
claude-code "Using android-development and tls-networking skills, implement FindMyPhone plugin for Android"

# COSMIC side
claude-code "Using cosmic-desktop skill, implement FindMyPhone plugin UI in applet"

# Test
claude-code --agent protocol-compatibility "Test FindMyPhone plugin works between platforms"
```

### 3. Fix Protocol Issue

```bash
# Investigate
claude-code "Using debugging and tls-networking skills, diagnose why identity packets aren't being received"

# Fix Android
claude-code "Using android-development skill, fix UDP broadcast in DeviceDiscovery"

# Fix COSMIC
claude-code "Using cosmic-desktop skill, fix multicast reception in Discovery module"

# Verify
claude-code --agent protocol-compatibility "Verify discovery works bidirectionally"
```

## 📚 Skill Reference

| Skill | Use For | Key Topics |
|-------|---------|------------|
| android-development | Android code modernization | Kotlin, Coroutines, MVVM, Jetpack Compose |
| cosmic-desktop | COSMIC applet development | libcosmic, Rust, DBus, Wayland |
| gradle | Build system | Build scripts, Dependencies, Optimization |
| tls-networking | Protocol & network | TLS, Certificates, UDP/TCP, COSMIC Connect |
| debugging | Troubleshooting | Logcat, GDB, Wireshark, Profiling |

## 🤖 Agent Quick Reference

| Agent | Use For | Example |
|-------|---------|---------|
| android-modernization | Android-specific tasks | `--agent android-modernization "Convert Device.java"` |
| cosmic-desktop | COSMIC/Rust tasks | `--agent cosmic-desktop "Add MPRIS control"` |
| protocol-compatibility | Cross-platform testing | `--agent protocol-compatibility "Test pairing"` |

## ⚡ Pro Tips

1. **Combine skills for better results:**
   ```bash
   claude-code "Using android-development, gradle, and debugging skills, modernize and test the battery plugin"
   ```

2. **Be specific about what you want:**
   ```bash
   # Instead of: "fix the code"
   # Try: "Using android-development skill, refactor DeviceConnection to use coroutines and add proper error handling"
   ```

3. **Use agents for focused work:**
   ```bash
   # When working on Android-specific features
   claude-code --agent android-modernization "task"
   
   # When ensuring cross-platform compatibility
   claude-code --agent protocol-compatibility "task"
   ```

4. **Reference examples from skills:**
   ```bash
   claude-code "Follow the Repository pattern example from android-development skill to create DeviceRepository"
   ```

## 🔍 Finding Information

### In Skills (5 comprehensive guides):
- **android-development-SKILL.md**: 500+ lines of Android expertise
- **cosmic-desktop-SKILL.md**: 400+ lines of COSMIC/Rust guidance
- **gradle-SKILL.md**: 400+ lines of build system knowledge
- **tls-networking-SKILL.md**: 600+ lines of protocol & security
- **debugging-SKILL.md**: 500+ lines of troubleshooting techniques

### In Agents (3 specialized experts):
- **android-modernization-agent.md**: Android modernization specialist
- **cosmic-desktop-agent.md**: COSMIC development specialist
- **protocol-compatibility-agent.md**: Cross-platform compatibility expert

## 🆘 Troubleshooting

### Skills not working?
```bash
# Check installation
ls -la .claude/skills/

# Verify file names
ls .claude/skills/*-SKILL.md

# Test skill access
claude-code "List available skills"
```

### Agent not working?
```bash
# Check agent files
ls -la .claude/agents/

# Test agent
claude-code --agent android-modernization "Hello"
```

### Need help?
```bash
# Ask Claude to read the README
claude-code "Read .claude/README.md and explain how to use the skills"

# Get specific guidance
claude-code "Using debugging skill, help me troubleshoot this issue: [describe issue]"
```

## 📖 Full Documentation

For complete documentation, examples, and detailed guides, see:
- `.claude/README.md` - Comprehensive guide (2000+ lines)
- Individual skill files - Deep dives into each topic
- Agent configurations - Specialized expert guidance

## 🎉 You're Ready!

Start with a simple command and build from there:

```bash
claude-code "Using android-development skill, analyze my codebase and suggest modernization priorities"
```

**Happy coding!** 🚀
