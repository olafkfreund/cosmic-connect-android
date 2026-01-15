# COSMIC Connect Android - Getting Started Guide

## ⚠️ IMPORTANT: Project Scope

**This project ONLY modernizes the Android app!**

- ✅ **COSMIC Desktop applet is ALREADY BUILT**: https://github.com/olafkfreund/cosmic-applet-kdeconnect
- 🎯 **We are ONLY working on the Android app**
- 🔗 **Goal**: Modernize Android to work perfectly with existing COSMIC applet

## 📋 Project Overview

This project modernizes the KDE Connect Android app for seamless integration with the **existing** COSMIC Desktop applet. It involves:

1. **Android App** (MAIN FOCUS): Converting 150+ Java files to modern Kotlin with MVVM architecture
2. **COSMIC Desktop** (TESTING ONLY): Test with existing applet to ensure compatibility
3. **Protocol Compatibility**: Ensuring perfect communication between Android and COSMIC

**Timeline**: 12-16 weeks
**Total Issues**: 40
**Phases**: 5

---

## 🎯 Where to Start

### Critical First Steps (Week 1)

**Start Here**: Issue #1 - Development Environment Setup

This is THE foundation. You cannot proceed without:
- Android Studio (latest stable)
- Rust toolchain (1.70+)
- Both repositories building successfully
- Test devices/emulators configured

### Critical Path

The project has a strict dependency chain:

```
Phase 1: Setup & Foundation
├─ #1: Dev Environment Setup (2h)
├─ #2: Android Audit (4h) ──┐
├─ #3: COSMIC Audit (3h) ───┤
├─ #4: Protocol Testing (4h) ◄─┘
└─ #5: Project Board (3h)

Phase 2: Core Modernization
├─ #6: Root Gradle → Kotlin DSL (2h)
├─ #7: App Gradle → Kotlin DSL (3h)
├─ #8: Version Catalog (2h)
├─ #9: NetworkPacket → Kotlin (3h) ← CRITICAL
├─ #10: NetworkPacket Tests (2h)
├─ #11: Device Class → Kotlin (4h)
├─ #12: DeviceManager → Repository (6h)
├─ #13: CertificateManager (4h) ← SECURITY CRITICAL
├─ #14: TLS Connection Manager (5h)
├─ #15: Discovery Service (5h)
└─ #16: Discovery Integration Test (3h)

Phase 3: Feature Implementation
├─ #17: Plugin Base Architecture (4h)
└─ #18-27: Individual plugins (3-5h each)

Phase 4: Integration & Testing
└─ #28-37: Comprehensive testing (4-6h each)

Phase 5: Release
└─ #38-40: Beta, testing, release (varies)
```

**Key Insight**: You CANNOT skip Phase 1. Issues #2, #3, and #4 inform all future work.

---

## 🏷️ GitHub Labels Required

### Priority Labels (4)
- `P0-Critical` - Red (#d73a4a) - Must complete, blocking
- `P1-High` - Orange (#ff6b6b) - High importance
- `P2-Medium` - Yellow (#ffa500) - Medium priority
- `P3-Low` - Light Yellow (#ffcc00) - Future enhancement

### Category Labels (24)
- `setup` - Green (#0e8a16)
- `infrastructure` - Blue (#1d76db)
- `audit` - Purple (#5319e7)
- `android` - Android Green (#3ddc84)
- `cosmic` - COSMIC Orange (#ed8936)
- `documentation` - Light Blue (#0075ca)
- `testing` - Yellow (#fbca04)
- `protocol` - Light Blue (#c5def5)
- `gradle` - Dark Teal (#02303a)
- `build-system` - Teal (#006b75)
- `kotlin-conversion` - Purple (#7f52ff)
- `architecture` - Pink (#d876e3)
- `security` - Dark Red (#b60205)
- `tls` - Orange-Red (#d93f0b)
- `networking` - Blue (#0052cc)
- `plugins` - Light Cyan (#bfdadc)
- `ui` - Light Green (#c2e0c6)
- `design` - Rose (#e99695)
- `compose` - Purple (#7057ff)
- `integration` - Sky Blue (#5ebeff)
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

## 📊 Understanding the 5 Phases

### Phase 1: Foundation & Setup (Weeks 1-2)
**Goal**: Understand what we have and prepare for work

**Issues**: #1-5
**Key Outputs**:
- ✅ Both codebases build
- ✅ Complete audits of Android and COSMIC code
- ✅ Protocol compatibility baseline
- ✅ All remaining issues created

**Why Critical**: Without audits, you're flying blind. Issue #4 (Protocol Testing) tells you what actually works today.

### Phase 2: Core Modernization (Weeks 3-6)
**Goal**: Modernize core architecture and networking

**Issues**: #6-16
**Key Outputs**:
- ✅ Kotlin DSL build system
- ✅ NetworkPacket in modern Kotlin
- ✅ Repository pattern for device management
- ✅ Secure TLS certificate handling
- ✅ Working device discovery

**Critical Issues**:
- #9: NetworkPacket (foundation for ALL communication)
- #13: CertificateManager (security foundation)
- #14: TLS Connection (required for pairing)

### Phase 3: Feature Implementation (Weeks 7-10)
**Goal**: Modernize all plugins and UI

**Issues**: #17-27
**Key Outputs**:
- ✅ Modern plugin architecture
- ✅ All plugins converted (battery, share, clipboard, etc.)
- ✅ Jetpack Compose UI
- ✅ Material 3 design system

**Parallel Work**: UI and plugins can be done concurrently after #17

### Phase 4: Integration & Testing (Weeks 11-12)
**Goal**: Verify everything works together

**Issues**: #28-37
**Key Outputs**:
- ✅ Integration test framework
- ✅ End-to-end tests (Android ↔ COSMIC)
- ✅ Performance benchmarks
- ✅ Complete documentation

**Critical Tests**:
- #32: Android → COSMIC (send from phone)
- #33: COSMIC → Android (send from desktop)

### Phase 5: Release & Maintenance (Weeks 13+)
**Goal**: Ship it!

**Issues**: #38-40
**Key Outputs**:
- ✅ Beta release
- ✅ User testing feedback
- ✅ Production release (Play Store + F-Droid)

---

## 💡 What You Need to Know

### Critical Technical Concepts

#### 1. **KDE Connect Protocol**
- **Version**: 7 (must match)
- **Discovery**: UDP broadcast on port 1716
- **Connection**: TCP ports 1714-1764
- **Encryption**: TLS with self-signed certificates
- **Pairing**: Certificate exchange and trust

📖 **Read**: `kdeconnect-protocol-debug.md` - This is your bible

#### 2. **Packet Format** (CRITICAL!)
```json
{
  "id": 1234567890,
  "type": "kdeconnect.identity",
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

#### 4. **Android Modernization Stack**
- Kotlin (replacing Java)
- Coroutines (replacing AsyncTask)
- StateFlow (replacing LiveData)
- Jetpack Compose (replacing XML layouts)
- Room (if needed for persistence)
- Hilt (dependency injection)

#### 5. **COSMIC Desktop Stack**
- Rust 1.70+
- libcosmic (UI framework)
- tokio (async runtime)
- tokio-rustls (TLS)
- iced (widget system)

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

## 📝 Week 1 Action Plan

### Day 1: Setup
- [ ] Complete Issue #1 (Dev Environment)
- [ ] Verify both codebases build
- [ ] Run existing tests (if any)
- [ ] Read `CLAUDE.md` thoroughly

### Day 2: Android Audit
- [ ] Complete Issue #2 (Android Audit)
- [ ] Count Java/Kotlin files: `find . -name "*.java" | wc -l`
- [ ] List all plugins
- [ ] Check test coverage: `./gradlew testDebugUnitTestCoverage`
- [ ] Document findings in `docs/audit-android.md`

### Day 3: COSMIC Audit
- [ ] Complete Issue #3 (COSMIC Audit)
- [ ] Test current COSMIC applet
- [ ] Review protocol implementation
- [ ] Check DBus interface
- [ ] Document findings in `docs/audit-cosmic.md`

### Day 4-5: Protocol Testing
- [ ] Complete Issue #4 (Protocol Testing)
- [ ] Test Android → COSMIC communication
- [ ] Test COSMIC → Android communication
- [ ] Use Wireshark to capture packets
- [ ] Test all current plugins
- [ ] Document what works, what doesn't

### End of Week: Planning
- [ ] Complete Issue #5 (Project Board)
- [ ] Create all remaining issues
- [ ] Prioritize based on audit findings
- [ ] Plan Week 2 work

---

## 🎯 Success Metrics

### Week 1 Success
- ✅ Both codebases build without errors
- ✅ Comprehensive audits completed
- ✅ Protocol compatibility baseline established
- ✅ All issues created with proper dependencies

### Week 2 Success
- ✅ Gradle build modernized (Issues #6-8)
- ✅ NetworkPacket converted and tested (#9-10)

### Month 1 Success
- ✅ Core architecture modernized (Phase 2 complete)
- ✅ Device discovery working reliably
- ✅ TLS pairing functional

### Project Success
- ✅ 80%+ test coverage
- ✅ All plugins modernized
- ✅ Full Android ↔ COSMIC compatibility
- ✅ Released to Play Store + F-Droid

---

## ⚠️ Common Pitfalls

### 1. Skipping Audits
**Don't**: Jump straight to coding
**Do**: Complete issues #2, #3, #4 thoroughly

### 2. Breaking Protocol Compatibility
**Don't**: Change packet formats
**Do**: Maintain exact KDE Connect protocol v7 compatibility

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

1. **This file** (you're here!)
2. `PROJECT_PLAN.md` - Complete project details
3. `kdeconnect-protocol-debug.md` - Protocol reference
4. `kdeconnect-rust-implementation-guide.md` - COSMIC implementation
5. `CLAUDE.md` - Claude Code usage
6. `.claude/skills/` - Specific skill documentation

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

- KDE Connect Protocol: https://invent.kde.org/network/kdeconnect-meta
- COSMIC Desktop: https://github.com/pop-os/cosmic-epoch
- Android Kotlin Guide: https://developer.android.com/kotlin

---

## ✅ Ready to Start?

### Your First Commands

```bash
# 1. Create labels
chmod +x create-github-labels.sh
./create-github-labels.sh

# 2. Start with Issue #1
gh issue create --title "Development Environment Setup" \
  --label "P0-Critical,setup,infrastructure" \
  --body "$(cat PROJECT_PLAN.md | grep -A 30 'Issue #1')"

# 3. Begin work
claude-code "Read PROJECT_PLAN.md and help me complete Issue #1"
```

### Recommended Tools

- **IDE**: Android Studio (Android), VS Code + rust-analyzer (COSMIC)
- **Networking**: Wireshark, tcpdump
- **Testing**: adb (Android), cargo test (Rust)
- **Version Control**: git with conventional commits

---

## 🎉 You're Ready!

The project is well-documented and structured. Follow the phases, complete the audits, and use Claude Code agents to accelerate development.

**Remember**: Phase 1 (audits) informs everything else. Don't rush it!

Good luck! 🚀

---

**Last Updated**: 2026-01-15
**Project Status**: Phase 1 - Foundation & Setup
**Next Milestone**: Complete Development Environment Setup
