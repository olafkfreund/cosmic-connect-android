#!/bin/bash
# Create all 40 GitHub Issues for COSMIC Connect Android Project

set -e

echo "Creating all 40 GitHub issues for COSMIC Connect Android..."
echo ""

# ============================================================================
# PHASE 1: Foundation & Setup (Weeks 1-2)
# ============================================================================

echo "📋 Creating Phase 1 issues (1-5)..."

# Issue #1
gh issue create \
  --title "Development Environment Setup" \
  --body "## Description
Set up complete development environment for both Android and COSMIC development.

## Tasks
- [ ] Install Android Studio (latest stable)
- [ ] Install Rust toolchain (1.70+)
- [ ] Install NixOS development tools (if using NixOS)
- [ ] Clone both repositories
- [ ] Build Android app successfully
- [ ] Build COSMIC applet successfully
- [ ] Set up emulator/test device
- [ ] Configure git hooks

## Success Criteria
- Both projects build without errors
- Tests run successfully
- Git hooks working
- Claude Code skills installed

## Estimated Effort
2 hours

## Dependencies
None

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/cosmic-desktop-SKILL.md\`" \
  --label "P0-Critical,setup,infrastructure"

# Issue #2
gh issue create \
  --title "Codebase Audit - Android" \
  --body "## Description
Complete audit of Android codebase to understand current state and plan modernization.

## Tasks
- [ ] Count Java vs Kotlin files
- [ ] List all Activity/Fragment classes
- [ ] Identify Services and BroadcastReceivers
- [ ] Document plugin implementations
- [ ] Check dependency versions
- [ ] Review test coverage
- [ ] Identify deprecated APIs in use
- [ ] Document current architecture patterns

## Deliverables
- \`docs/audit-android.md\` with findings
- List of Java files to convert
- List of deprecated APIs to replace
- Current test coverage report

## Success Criteria
- Complete understanding of codebase
- Prioritized modernization list
- Documented technical debt

## Estimated Effort
4 hours

## Dependencies
- Issue #1

## Related Documentation
- \`PROJECT_PLAN.md\` for audit guidelines" \
  --label "P0-Critical,audit,android,documentation"

# Issue #3
gh issue create \
  --title "Codebase Audit - COSMIC Desktop" \
  --body "## Description
Complete audit of COSMIC Desktop implementation to understand integration requirements.

## Tasks
- [ ] Review daemon implementation
- [ ] Check DBus interface completeness
- [ ] Test applet functionality
- [ ] Verify protocol implementation
- [ ] Review system integrations
- [ ] Check plugin implementations
- [ ] Document current features
- [ ] Identify gaps vs Android

## Deliverables
- \`docs/audit-cosmic.md\` with findings
- Protocol compatibility matrix
- Feature parity checklist
- Integration gaps document

## Success Criteria
- Complete understanding of COSMIC implementation
- Clear compatibility requirements
- Feature gaps identified

## Estimated Effort
3 hours

## Dependencies
- Issue #1

## Related Documentation
- \`.claude/skills/cosmic-desktop-SKILL.md\`
- \`cosmicconnect-protocol-debug.md\`" \
  --label "P0-Critical,audit,cosmic,documentation"

# Issue #4
gh issue create \
  --title "Protocol Compatibility Testing" \
  --body "## Description
Test current protocol compatibility between Android and COSMIC implementations.

## Tasks
- [ ] Test device discovery (both directions)
- [ ] Test pairing process (both directions)
- [ ] Test file transfer (both directions)
- [ ] Test all plugins bidirectionally
- [ ] Capture and analyze network traffic
- [ ] Verify TLS implementation
- [ ] Document incompatibilities
- [ ] Create test checklist

## Deliverables
- \`docs/protocol-compatibility-report.md\`
- Test results matrix
- List of compatibility issues
- Network captures

## Success Criteria
- All protocol aspects tested
- Issues documented
- Test methodology established

## Estimated Effort
4 hours

## Dependencies
- Issue #1
- Issue #2
- Issue #3

## Related Documentation
- \`cosmicconnect-protocol-debug.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P0-Critical,protocol,testing"

# Issue #5
gh issue create \
  --title "Create Project Board and Milestones" \
  --body "## Description
Set up GitHub project board with milestones for tracking progress.

## Tasks
- [ ] Create GitHub project board
- [ ] Set up board columns (Backlog, Ready, In Progress, Review, Done)
- [ ] Create milestones for each phase
- [ ] Link all issues to appropriate milestones
- [ ] Set up automation rules
- [ ] Create issue templates for future issues

## Deliverables
- Active GitHub project board
- 5 phase milestones configured
- All issues linked to milestones
- Issue templates available

## Success Criteria
- Project board operational
- Clear progress tracking
- Team can visualize workflow

## Estimated Effort
3 hours

## Dependencies
- Issue #2
- Issue #3
- Issue #4" \
  --label "P0-Critical,project-management"

echo "✅ Phase 1 issues created (1-5)"
echo ""

# ============================================================================
# PHASE 2: Core Modernization (Weeks 3-6)
# ============================================================================

echo "📋 Creating Phase 2 issues (6-16)..."

# Issue #6
gh issue create \
  --title "Convert Root build.gradle to Kotlin DSL" \
  --body "## Description
Modernize root-level build.gradle to Kotlin DSL.

## Tasks
- [ ] Convert build.gradle to build.gradle.kts
- [ ] Update plugin declarations
- [ ] Convert repositories configuration
- [ ] Test build succeeds
- [ ] Update documentation

## Implementation Requirements
- Use modern plugin syntax
- Add version catalog support
- Configure dependency resolution

## Success Criteria
- Build succeeds without warnings
- All tests pass
- Documentation updated

## Estimated Effort
2 hours

## Dependencies
- Issue #5

## Related Documentation
- \`.claude/skills/gradle-SKILL.md\`" \
  --label "P1-High,gradle,build-system"

# Issue #7
gh issue create \
  --title "Convert App build.gradle to Kotlin DSL" \
  --body "## Description
Modernize app-level build.gradle to Kotlin DSL.

## Tasks
- [ ] Convert build.gradle to build.gradle.kts
- [ ] Modernize Android configuration
- [ ] Set up build types properly
- [ ] Configure ProGuard/R8 rules
- [ ] Migrate dependencies
- [ ] Test all build variants
- [ ] Update documentation

## Implementation Requirements
- Use type-safe configuration
- Enable modern Android features
- Optimize build performance

## Success Criteria
- All build variants work
- ProGuard rules correct
- Build time acceptable

## Estimated Effort
3 hours

## Dependencies
- Issue #6

## Related Documentation
- \`.claude/skills/gradle-SKILL.md\`" \
  --label "P1-High,gradle,build-system"

# Issue #8
gh issue create \
  --title "Set Up Version Catalog" \
  --body "## Description
Create version catalog for centralized dependency management.

## Tasks
- [ ] Create gradle/libs.versions.toml
- [ ] Define version variables
- [ ] Define libraries
- [ ] Define plugins
- [ ] Create bundles
- [ ] Migrate dependencies
- [ ] Test build
- [ ] Update documentation

## Success Criteria
- All dependencies in catalog
- Build uses catalog
- Documentation complete

## Estimated Effort
2 hours

## Dependencies
- Issue #7

## Related Documentation
- \`.claude/skills/gradle-SKILL.md\`" \
  --label "P1-High,gradle,build-system"

# Issue #9
gh issue create \
  --title "Convert NetworkPacket to Kotlin" \
  --body "## Description
Convert NetworkPacket class to Kotlin with modern patterns. **CRITICAL**: Foundation of all protocol communication.

## Tasks
- [ ] Convert to Kotlin data class
- [ ] Add sealed classes for packet types
- [ ] Implement proper null safety
- [ ] Add extension functions
- [ ] Write comprehensive tests
- [ ] Update documentation

## Implementation Requirements
- Use data class for immutability
- Add proper serialization
- **CRITICAL**: Maintain protocol compatibility (packets MUST end with \\\\n)
- Add validation logic

## Success Criteria
- All tests pass
- Protocol compatibility verified with COSMIC Connect Android
- Code more readable and safe
- Newline termination preserved

## Estimated Effort
3 hours

## Dependencies
- Issue #8

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`
- \`cosmicconnect-protocol-debug.md\`" \
  --label "P0-Critical,android,kotlin-conversion,protocol"

# Issue #10
gh issue create \
  --title "Implement NetworkPacket Unit Tests" \
  --body "## Description
Create comprehensive unit tests for NetworkPacket.

## Tasks
- [ ] Test serialization/deserialization
- [ ] Test all packet types (identity, pair, etc.)
- [ ] Test validation logic
- [ ] Test error cases
- [ ] Test edge cases (large packets, malformed data)
- [ ] Achieve >90% coverage
- [ ] Test newline termination

## Success Criteria
- All tests pass
- >90% code coverage
- Edge cases covered
- Protocol compatibility verified

## Estimated Effort
2 hours

## Dependencies
- Issue #9

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/debugging-SKILL.md\`" \
  --label "P0-Critical,android,testing"

# Issue #11
gh issue create \
  --title "Convert Device Class to Kotlin" \
  --body "## Description
Convert Device class to Kotlin with modern patterns.

## Tasks
- [ ] Convert to Kotlin
- [ ] Use data class for device info
- [ ] Add state management with sealed classes
- [ ] Implement coroutines for async ops
- [ ] Add proper lifecycle handling
- [ ] Write unit tests
- [ ] Update documentation

## Implementation Requirements
- Thread-safe state management
- Coroutines for network operations
- Proper resource cleanup
- Null safety

## Success Criteria
- All functionality preserved
- Tests pass
- More maintainable code

## Estimated Effort
4 hours

## Dependencies
- Issue #9
- Issue #10

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P0-Critical,android,kotlin-conversion"

# Issue #12
gh issue create \
  --title "Convert DeviceManager to Kotlin with Repository Pattern" \
  --body "## Description
Convert DeviceManager to Kotlin and refactor to Repository pattern.

## Tasks
- [ ] Convert to Kotlin
- [ ] Implement Repository interface
- [ ] Create DeviceRepository implementation
- [ ] Use StateFlow for device list
- [ ] Add coroutines for operations
- [ ] Implement proper error handling
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Implementation Requirements
- Repository pattern for data access
- StateFlow for reactive updates
- Dependency injection ready
- Proper separation of concerns

## Success Criteria
- Repository pattern implemented
- All tests pass
- Better testability

## Estimated Effort
6 hours

## Dependencies
- Issue #11

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P1-High,android,kotlin-conversion,architecture"

# Issue #13
gh issue create \
  --title "Modernize CertificateManager" \
  --body "## Description
Modernize certificate management with Android Keystore. **CRITICAL**: Security foundation for TLS.

## Tasks
- [ ] Convert to Kotlin
- [ ] Use Android Keystore properly
- [ ] Add certificate rotation support
- [ ] Implement proper error handling
- [ ] Add certificate validation
- [ ] Write security tests
- [ ] Document security model

## Implementation Requirements
- Use Android Keystore
- RSA 2048-bit keys
- 10-year validity
- SHA-256 fingerprints
- CN=deviceId, O=KDE, OU=COSMIC Connect

## Success Criteria
- Secure certificate storage
- COSMIC Desktop compatible
- Tests pass

## Estimated Effort
4 hours

## Dependencies
- Issue #11

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`
- \`cosmicconnect-protocol-debug.md\`" \
  --label "P0-Critical,android,security,tls"

# Issue #14
gh issue create \
  --title "Implement TLS Connection Manager" \
  --body "## Description
Create modern TLS connection manager with coroutines. **CRITICAL**: Required for secure pairing.

## Tasks
- [ ] Implement TLS context creation
- [ ] Add certificate pinning
- [ ] Implement connection pooling
- [ ] Add timeout handling
- [ ] Implement retry logic
- [ ] Add connection monitoring
- [ ] Write integration tests
- [ ] Document TLS configuration

## Implementation Requirements
- TLS 1.2+ support
- Certificate validation
- Proper timeout handling
- Connection reuse
- **CRITICAL**: TLS role determined by deviceId comparison

## Success Criteria
- Secure connections
- Protocol compatible with COSMIC
- Well tested

## Estimated Effort
5 hours

## Dependencies
- Issue #13

## Related Documentation
- \`.claude/skills/tls-networking-SKILL.md\`
- \`.claude/skills/android-development-SKILL.md\`
- \`cosmicconnect-protocol-debug.md\`" \
  --label "P0-Critical,android,networking,tls"

# Issue #15
gh issue create \
  --title "Modernize Discovery Service" \
  --body "## Description
Modernize UDP multicast discovery service.

## Tasks
- [ ] Convert to Kotlin
- [ ] Implement with coroutines
- [ ] Add proper multicast lock handling
- [ ] Implement broadcast scheduling
- [ ] Add discovery state management
- [ ] Handle network changes
- [ ] Write tests
- [ ] Update documentation

## Implementation Requirements
- Multicast group: 224.0.0.251
- Port: 1716
- Proper wake locks
- Network change handling

## Success Criteria
- Discovery works reliably
- Network changes handled
- Tests pass

## Estimated Effort
5 hours

## Dependencies
- Issue #9
- Issue #11

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P1-High,android,networking"

# Issue #16
gh issue create \
  --title "Test Discovery with COSMIC Desktop" \
  --body "## Description
Integration testing for device discovery between Android and COSMIC.

## Tasks
- [ ] Test Android discovers COSMIC
- [ ] Test COSMIC discovers Android
- [ ] Verify identity packet format
- [ ] Test on different networks
- [ ] Test with network interruptions
- [ ] Document test results
- [ ] Fix any issues found

## Success Criteria
- Bidirectional discovery works
- Protocol compatible
- Documented test cases

## Estimated Effort
3 hours

## Dependencies
- Issue #15

## Related Documentation
- \`.claude/skills/debugging-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P0-Critical,protocol,testing,integration"

echo "✅ Phase 2 issues created (6-16)"
echo ""

# ============================================================================
# PHASE 3: Feature Implementation (Weeks 7-10)
# ============================================================================

echo "📋 Creating Phase 3 issues (17-27)..."

# Issue #17
gh issue create \
  --title "Create Plugin Base Architecture" \
  --body "## Description
Create modern plugin architecture with coroutines.

## Tasks
- [ ] Design Plugin interface
- [ ] Implement PluginManager
- [ ] Add plugin lifecycle management
- [ ] Implement packet routing
- [ ] Add plugin configuration
- [ ] Create plugin testing base
- [ ] Document plugin API

## Implementation Requirements
- Coroutine-based
- Type-safe packet handling
- Easy to test
- Extensible

## Success Criteria
- Clean plugin API
- Easy to add plugins
- Well documented

## Estimated Effort
4 hours

## Dependencies
- Issue #12

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P1-High,android,architecture,plugins"

# Issue #18
gh issue create \
  --title "Modernize Battery Plugin" \
  --body "## Description
Modernize battery plugin with MVVM.

## Tasks
- [ ] Convert to Kotlin
- [ ] Implement with new plugin base
- [ ] Create BatteryViewModel
- [ ] Add StateFlow for battery state
- [ ] Implement monitoring with coroutines
- [ ] Add low battery notifications
- [ ] Write tests
- [ ] Test with COSMIC Desktop

## Success Criteria
- Plugin works correctly
- COSMIC Desktop compatible
- Tests pass

## Estimated Effort
3 hours

## Dependencies
- Issue #17

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P1-High,android,plugins"

# Issue #19
gh issue create \
  --title "Modernize Share Plugin" \
  --body "## Description
Modernize file sharing plugin.

## Tasks
- [ ] Convert to Kotlin
- [ ] Implement with new plugin base
- [ ] Add coroutines for file transfer
- [ ] Implement progress reporting
- [ ] Add cancellation support
- [ ] Handle large files properly
- [ ] Write tests
- [ ] Test with COSMIC Desktop

## Implementation Requirements
- TCP payload transfer
- Progress callbacks
- Cancellable transfers
- Memory efficient

## Success Criteria
- File sharing works both ways
- Large files supported
- Progress tracking works

## Estimated Effort
5 hours

## Dependencies
- Issue #17
- Issue #14

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P1-High,android,plugins"

# Issue #20
gh issue create \
  --title "Modernize Clipboard Plugin" \
  --body "## Description
Modernize clipboard synchronization plugin.

## Tasks
- [ ] Convert to Kotlin
- [ ] Implement with new plugin base
- [ ] Add bidirectional sync
- [ ] Implement clipboard monitoring
- [ ] Add conflict resolution
- [ ] Handle different content types
- [ ] Write tests
- [ ] Test with COSMIC Desktop

## Success Criteria
- Clipboard syncs both ways
- No sync loops
- Tests pass

## Estimated Effort
4 hours

## Dependencies
- Issue #17

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P1-High,android,plugins"

# Issue #21
gh issue create \
  --title "Modernize Notification Plugin" \
  --body "## Description
Modernize notification synchronization plugin.

## Tasks
- [ ] Convert to Kotlin
- [ ] Implement with new plugin base
- [ ] Add notification listener service
- [ ] Implement notification forwarding
- [ ] Add reply support
- [ ] Handle notification actions
- [ ] Write tests
- [ ] Test with COSMIC Desktop

## Success Criteria
- Notifications forwarded
- Actions work
- Tests pass

## Estimated Effort
4 hours

## Dependencies
- Issue #17

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P2-Medium,android,plugins"

# Issue #22
gh issue create \
  --title "Implement RunCommand Plugin" \
  --body "## Description
Implement remote command execution plugin.

## Tasks
- [ ] Create plugin from scratch in Kotlin
- [ ] Implement command storage
- [ ] Add command execution
- [ ] Implement security checks
- [ ] Add command management UI
- [ ] Write tests
- [ ] Test with COSMIC Desktop

## Implementation Requirements
- Pre-configured commands only
- Non-blocking execution
- Security validation
- Persistent storage

## Success Criteria
- Commands execute correctly
- Secure implementation
- COSMIC Desktop compatible

## Estimated Effort
4 hours

## Dependencies
- Issue #17

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P2-Medium,android,plugins"

# Issue #23
gh issue create \
  --title "Implement FindMyPhone Plugin" \
  --body "## Description
Implement find my phone plugin.

## Tasks
- [ ] Create plugin in Kotlin
- [ ] Implement ring functionality
- [ ] Add notification display
- [ ] Handle volume control
- [ ] Add stop mechanism
- [ ] Write tests
- [ ] Test with COSMIC Desktop

## Success Criteria
- Phone rings on command
- Can be stopped
- Works with COSMIC Desktop

## Estimated Effort
3 hours

## Dependencies
- Issue #17

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P2-Medium,android,plugins"

# Issue #24
gh issue create \
  --title "Create Design System" \
  --body "## Description
Create Material 3 design system for app.

## Tasks
- [ ] Define color scheme
- [ ] Create typography system
- [ ] Define component library
- [ ] Create common composables
- [ ] Document design system

## Success Criteria
- Consistent design
- Reusable components
- Documented

## Estimated Effort
4 hours

## Dependencies
- Issue #17

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P2-Medium,android,ui,design"

# Issue #25
gh issue create \
  --title "Convert Device List to Compose" \
  --body "## Description
Convert device list screen to Jetpack Compose.

## Tasks
- [ ] Create DeviceListScreen composable
- [ ] Implement device list with LazyColumn
- [ ] Add device item card
- [ ] Implement device actions
- [ ] Add loading states
- [ ] Add error states
- [ ] Write UI tests

## Success Criteria
- Modern UI
- Smooth performance
- Tests pass

## Estimated Effort
5 hours

## Dependencies
- Issue #24

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P2-Medium,android,ui,compose"

# Issue #26
gh issue create \
  --title "Convert Device Detail to Compose" \
  --body "## Description
Convert device detail screen to Jetpack Compose.

## Tasks
- [ ] Create DeviceDetailScreen composable
- [ ] Show device information
- [ ] Display plugin list
- [ ] Add plugin controls
- [ ] Implement actions
- [ ] Write UI tests

## Success Criteria
- Modern UI
- All features work
- Tests pass

## Estimated Effort
5 hours

## Dependencies
- Issue #25

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P2-Medium,android,ui,compose"

# Issue #27
gh issue create \
  --title "Convert Settings to Compose" \
  --body "## Description
Convert settings screen to Jetpack Compose.

## Tasks
- [ ] Create SettingsScreen composable
- [ ] Implement preference items
- [ ] Add plugin configuration
- [ ] Implement actions
- [ ] Write UI tests

## Success Criteria
- Modern settings UI
- All settings work
- Tests pass

## Estimated Effort
4 hours

## Dependencies
- Issue #26

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P3-Low,android,ui,compose"

echo "✅ Phase 3 issues created (17-27)"
echo ""

# ============================================================================
# PHASE 4: Integration & Testing (Weeks 11-12)
# ============================================================================

echo "📋 Creating Phase 4 issues (28-37)..."

# Issue #28
gh issue create \
  --title "Set Up Integration Test Framework" \
  --body "## Description
Set up comprehensive integration testing.

## Tasks
- [ ] Configure test devices/emulators
- [ ] Set up mock server
- [ ] Create test utilities
- [ ] Add test data factories
- [ ] Configure CI for tests
- [ ] Document test approach

## Success Criteria
- Tests run on CI
- Easy to write new tests
- Documented

## Estimated Effort
4 hours

## Dependencies
- Issue #27

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/debugging-SKILL.md\`" \
  --label "P1-High,testing,infrastructure"

# Issue #29
gh issue create \
  --title "Integration Tests - Discovery & Pairing" \
  --body "## Description
Create integration tests for discovery and pairing.

## Tasks
- [ ] Test device discovery
- [ ] Test pairing initiation
- [ ] Test pairing acceptance
- [ ] Test pairing rejection
- [ ] Test certificate exchange
- [ ] Test paired device persistence

## Success Criteria
- All scenarios tested
- Tests pass reliably
- Good coverage

## Estimated Effort
5 hours

## Dependencies
- Issue #28

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/debugging-SKILL.md\`" \
  --label "P0-Critical,testing,integration,protocol"

# Issue #30
gh issue create \
  --title "Integration Tests - File Transfer" \
  --body "## Description
Create integration tests for file transfer.

## Tasks
- [ ] Test small file transfer
- [ ] Test large file transfer
- [ ] Test multiple files
- [ ] Test cancellation
- [ ] Test error cases
- [ ] Test progress reporting

## Success Criteria
- All scenarios tested
- Tests pass reliably
- Edge cases covered

## Estimated Effort
5 hours

## Dependencies
- Issue #28
- Issue #19

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P0-Critical,testing,integration,protocol"

# Issue #31
gh issue create \
  --title "Integration Tests - All Plugins" \
  --body "## Description
Create integration tests for all plugins.

## Tasks
- [ ] Test battery plugin
- [ ] Test share plugin
- [ ] Test clipboard plugin
- [ ] Test notification plugin
- [ ] Test RunCommand plugin
- [ ] Test FindMyPhone plugin

## Success Criteria
- All plugins tested
- Tests pass reliably
- Good coverage

## Estimated Effort
6 hours

## Dependencies
- Issue #28
- Issues #18-23

## Related Documentation
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P1-High,testing,integration,plugins"

# Issue #32
gh issue create \
  --title "End-to-End Test: Android → COSMIC" \
  --body "## Description
Full end-to-end testing from Android to COSMIC. **CRITICAL**: Verify complete integration.

## Tasks
- [ ] Test complete flow: discovery → pairing → transfer
- [ ] Test all plugins Android → COSMIC
- [ ] Test error recovery
- [ ] Test reconnection
- [ ] Document test results

## Success Criteria
- All features work
- Issues documented
- Test cases recorded

## Estimated Effort
4 hours

## Dependencies
- Issue #31

## Related Documentation
- \`.claude/skills/debugging-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P0-Critical,testing,e2e,protocol"

# Issue #33
gh issue create \
  --title "End-to-End Test: COSMIC → Android" \
  --body "## Description
Full end-to-end testing from COSMIC to Android. **CRITICAL**: Verify bidirectional compatibility.

## Tasks
- [ ] Test complete flow: discovery → pairing → transfer
- [ ] Test all plugins COSMIC → Android
- [ ] Test error recovery
- [ ] Test reconnection
- [ ] Document test results

## Success Criteria
- All features work
- Issues documented
- Test cases recorded

## Estimated Effort
4 hours

## Dependencies
- Issue #32

## Related Documentation
- \`.claude/skills/debugging-SKILL.md\`
- \`.claude/skills/tls-networking-SKILL.md\`" \
  --label "P0-Critical,testing,e2e,protocol"

# Issue #34
gh issue create \
  --title "Performance Testing" \
  --body "## Description
Comprehensive performance testing.

## Tasks
- [ ] Test app startup time
- [ ] Test discovery time
- [ ] Test connection time
- [ ] Test file transfer speed
- [ ] Test memory usage
- [ ] Test battery impact
- [ ] Document benchmarks

## Success Criteria
- Benchmarks established
- Performance acceptable
- Documented

## Estimated Effort
5 hours

## Dependencies
- Issue #33

## Related Documentation
- \`.claude/skills/debugging-SKILL.md\`
- \`.claude/skills/android-development-SKILL.md\`" \
  --label "P1-High,testing,performance"

# Issue #35
gh issue create \
  --title "Update User Documentation" \
  --body "## Description
Update all user-facing documentation.

## Tasks
- [ ] Update README
- [ ] Update setup guide
- [ ] Create user guide
- [ ] Add screenshots
- [ ] Document features
- [ ] Create FAQ

## Success Criteria
- Complete documentation
- Easy to understand
- Up to date

## Estimated Effort
4 hours

## Dependencies
- Issue #34

## Related Documentation
- N/A" \
  --label "P2-Medium,documentation"

# Issue #36
gh issue create \
  --title "Update Developer Documentation" \
  --body "## Description
Update all developer documentation.

## Tasks
- [ ] Update architecture docs
- [ ] Document plugin API
- [ ] Create contribution guide
- [ ] Add code examples
- [ ] Document protocol implementation
- [ ] Update API docs

## Success Criteria
- Complete dev docs
- Easy to contribute
- Well explained

## Estimated Effort
5 hours

## Dependencies
- Issue #35

## Related Documentation
- N/A" \
  --label "P2-Medium,documentation"

# Issue #37
gh issue create \
  --title "Create Migration Guide" \
  --body "## Description
Create migration guide for users and developers.

## Tasks
- [ ] Document breaking changes
- [ ] Create upgrade guide
- [ ] Add troubleshooting section
- [ ] Document new features
- [ ] Add FAQ

## Success Criteria
- Clear migration path
- Common issues covered
- Easy to follow

## Estimated Effort
3 hours

## Dependencies
- Issue #36

## Related Documentation
- N/A" \
  --label "P2-Medium,documentation"

echo "✅ Phase 4 issues created (28-37)"
echo ""

# ============================================================================
# PHASE 5: Release & Maintenance (Weeks 13+)
# ============================================================================

echo "📋 Creating Phase 5 issues (38-40)..."

# Issue #38
gh issue create \
  --title "Beta Release Preparation" \
  --body "## Description
Prepare for beta release.

## Tasks
- [ ] Create release checklist
- [ ] Test on multiple devices
- [ ] Update version numbers
- [ ] Create release notes
- [ ] Prepare Play Store listing
- [ ] Create promotional materials
- [ ] Set up beta channel

## Success Criteria
- Ready for beta
- Documentation complete
- Tests passing

## Estimated Effort
6 hours

## Dependencies
- Issue #37

## Related Documentation
- N/A" \
  --label "P0-Critical,release"

# Issue #39
gh issue create \
  --title "Beta Testing" \
  --body "## Description
Coordinate beta testing with users.

## Tasks
- [ ] Recruit beta testers
- [ ] Distribute beta build
- [ ] Set up feedback collection
- [ ] Monitor crash reports
- [ ] Triage and fix issues
- [ ] Release beta updates
- [ ] Document feedback

## Success Criteria
- Major issues found and fixed
- Stable for release
- Positive feedback

## Estimated Effort
2 weeks

## Dependencies
- Issue #38

## Related Documentation
- N/A" \
  --label "P0-Critical,testing,release"

# Issue #40
gh issue create \
  --title "Final Release" \
  --body "## Description
Final release preparation and deployment.

## Tasks
- [ ] Final testing
- [ ] Update all documentation
- [ ] Create final release notes
- [ ] Build signed APK
- [ ] Upload to Play Store
- [ ] Upload to F-Droid
- [ ] Announce release
- [ ] Monitor for issues

## Success Criteria
- App released
- No critical issues
- Users can update

## Estimated Effort
4 hours

## Dependencies
- Issue #39

## Related Documentation
- N/A" \
  --label "P0-Critical,release"

echo "✅ Phase 5 issues created (38-40)"
echo ""
echo "🎉 All 40 issues created successfully!"
echo ""
echo "Next steps:"
echo "1. View all issues: gh issue list"
echo "2. Set up project board: Issue #5"
echo "3. Start with Issue #1: Development Environment Setup"
echo ""
echo "Happy coding! 🚀"
