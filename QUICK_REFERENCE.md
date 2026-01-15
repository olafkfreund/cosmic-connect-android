# COSMIC Connect Android - Quick Reference Card

## 🚀 Start Here

```bash
# 1. View your first issue
gh issue view 1

# 2. Get help from Claude
claude-code "Help me complete issue #1"

# 3. View all issues
gh issue list --limit 100
```

## 📋 Critical Path (Week by Week)

### Week 1: Foundation
- Issue #1: Dev Environment Setup (START HERE!)
- Issue #2: Android Audit
- Issue #3: COSMIC Audit

### Week 2: Testing & Planning
- Issue #4: Protocol Testing
- Issue #5: Project Board Setup

### Weeks 3-6: Core Modernization
- Issues #6-8: Gradle modernization
- Issue #9: NetworkPacket (CRITICAL!)
- Issues #10-16: Testing, Device, TLS, Discovery

### Weeks 7-10: Features
- Issue #17: Plugin Architecture
- Issues #18-27: Plugins & UI

### Weeks 11-12: Testing
- Issues #28-38, #42: Integration & E2E tests

### Week 13+: Release
- Issues #39-41: Beta & Release

## 🔥 Most Critical Issues (P0)

**Must Complete First:**
1. #1 - Dev Environment (can't work without this!)
2. #2 - Android Audit (understand what you have)
3. #3 - COSMIC Audit (understand integration)
4. #4 - Protocol Testing (establish baseline)

**Then These:**
- #9 - NetworkPacket (foundation of communication)
- #13 - CertificateManager (security foundation)
- #14 - TLS Connection (required for pairing)
- #32, #33 - E2E Tests (verify everything works)

## 💡 Common Commands

### View Issues
```bash
# View specific issue
gh issue view <number>

# List all issues
gh issue list --limit 100

# Filter by priority
gh issue list --label "P0-Critical"

# Filter by category
gh issue list --label "android"
gh issue list --label "protocol"
```

### Using Claude Code
```bash
# General help
claude-code "Help me with issue #N"

# Use android modernization agent
claude-code --agent android-modernization "Work on issue #9"

# Use protocol testing agent
claude-code --agent protocol-compatibility "Test discovery"

# Read documentation
claude-code "Read GETTING_STARTED.md and explain the project"
```

## 📚 Documentation Files

| File | Purpose |
|------|---------|
| **SETUP_COMPLETE.md** | Setup verification & next steps |
| **GETTING_STARTED.md** | Complete walkthrough (READ THIS!) |
| **SUMMARY.md** | Executive summary |
| **PROJECT_PLAN.md** | All 41 issues in detail |
| **kdeconnect-protocol-debug.md** | Protocol reference |
| **CLAUDE.md** | Claude Code skills/agents |

## 🎯 Key Technical Points

### Protocol Requirements (CRITICAL!)
- ✅ Packets MUST end with `\n` (not `\r\n`)
- ✅ TLS role: larger deviceId = TLS server
- ✅ Identity packets sent BEFORE TLS
- ✅ Protocol version MUST be 7

### Network Ports
- UDP Discovery: 1716
- TCP Connection: 1714-1764
- Multicast: 224.0.0.251

### Certificate Requirements
- RSA 2048-bit keys
- CN = deviceId
- O = "KDE"
- OU = "KDE Connect"
- 10-year validity

## ✅ Quick Checks

```bash
# Verify labels created
gh label list | wc -l  # Should be 30+

# Verify issues created
gh issue list --limit 100 | wc -l  # Should be 41

# Check issue #1
gh issue view 1

# View all P0 issues
gh issue list --label "P0-Critical"
```

## 🆘 If You Get Stuck

```bash
# Ask Claude for help
claude-code "I'm stuck on issue #N, can you help?"

# Read the getting started guide
less GETTING_STARTED.md

# Check the protocol debug guide
less kdeconnect-protocol-debug.md

# Use debugging skill
claude-code "Using debugging skill, help me troubleshoot..."
```

## 📊 Project Stats

- **Total Issues**: 41
- **P0-Critical**: 15 issues
- **Timeline**: 12-16 weeks
- **Phases**: 5
- **Files to Modernize**: 150+ Java → Kotlin

## 🎉 You're All Set!

**Next Command:**
```bash
gh issue view 1
```

Then start working through issue #1's tasks!

---

**Quick Tip**: Don't skip the audits (#2-4)! They tell you exactly what needs to be done and save time later.

**Remember**: Use Claude Code agents! They're pre-configured for this project.

Good luck! 🚀
