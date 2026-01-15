#!/bin/bash
# Create GitHub Issues for COSMIC Connect Android Project
# Run this script from the repository root after creating labels

set -e

echo "Creating GitHub issues for COSMIC Connect Android..."
echo ""

# ============================================================================
# PHASE 1: Foundation & Setup (Weeks 1-2)
# ============================================================================

echo "Creating Phase 1 issues..."

gh issue create \
  --title "Development Environment Setup" \
  --body "$(cat <<'EOF'
## Description
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
- See `.claude/skills/android-development-SKILL.md`
- See `.claude/skills/cosmic-desktop-SKILL.md`
EOF
)" \
  --label "P0-Critical,setup,infrastructure" \
  --milestone "Phase 1: Foundation & Setup"

gh issue create \
  --title "Codebase Audit - Android" \
  --body "$(cat <<'EOF'
## Description
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
- `docs/audit-android.md` with findings
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
- #1

## Related Documentation
- See `PROJECT_PLAN.md` for audit guidelines
EOF
)" \
  --label "P0-Critical,audit,android,documentation" \
  --milestone "Phase 1: Foundation & Setup"

gh issue create \
  --title "Codebase Audit - COSMIC Desktop" \
  --body "$(cat <<'EOF'
## Description
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
- `docs/audit-cosmic.md` with findings
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
- #1

## Related Documentation
- See `.claude/skills/cosmic-desktop-SKILL.md`
- See `cosmicconnect-protocol-debug.md`
EOF
)" \
  --label "P0-Critical,audit,cosmic,documentation" \
  --milestone "Phase 1: Foundation & Setup"

gh issue create \
  --title "Protocol Compatibility Testing" \
  --body "$(cat <<'EOF'
## Description
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
- `docs/protocol-compatibility-report.md`
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
- #1
- #2
- #3

## Related Documentation
- See `cosmicconnect-protocol-debug.md`
- See `.claude/skills/tls-networking-SKILL.md`
EOF
)" \
  --label "P0-Critical,protocol,testing" \
  --milestone "Phase 1: Foundation & Setup"

gh issue create \
  --title "Create Project Board and Initial Issues" \
  --body "$(cat <<'EOF'
## Description
Set up GitHub project board and create all initial issues for the project.

## Tasks
- [ ] Create GitHub project board
- [ ] Set up board columns (Backlog, Ready, In Progress, Review, Done)
- [ ] Create milestones for each phase
- [ ] Create all Phase 2 issues
- [ ] Create all Phase 3 issues
- [ ] Create all Phase 4 issues
- [ ] Assign labels and priorities
- [ ] Set up issue templates

## Deliverables
- Active GitHub project board
- All issues created and labeled
- Milestones configured
- Issue templates available

## Success Criteria
- Project board populated
- Clear path forward
- Team can start work

## Estimated Effort
3 hours

## Dependencies
- #2
- #3
- #4

## Related Documentation
- See `PROJECT_PLAN.md` for complete issue list
EOF
)" \
  --label "P0-Critical,project-management" \
  --milestone "Phase 1: Foundation & Setup"

# ============================================================================
# PHASE 2: Core Modernization (Weeks 3-6)
# ============================================================================

echo "Creating Phase 2 issues..."

gh issue create \
  --title "Convert Root build.gradle to Kotlin DSL" \
  --body "$(cat <<'EOF'
## Description
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
- #5

## Related Documentation
- See `.claude/skills/gradle-SKILL.md`
EOF
)" \
  --label "P1-High,gradle,build-system" \
  --milestone "Phase 2: Core Modernization"

gh issue create \
  --title "Convert App build.gradle to Kotlin DSL" \
  --body "$(cat <<'EOF'
## Description
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
- #6

## Related Documentation
- See `.claude/skills/gradle-SKILL.md`
EOF
)" \
  --label "P1-High,gradle,build-system" \
  --milestone "Phase 2: Core Modernization"

gh issue create \
  --title "Set Up Version Catalog" \
  --body "$(cat <<'EOF'
## Description
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
- #7

## Related Documentation
- See `.claude/skills/gradle-SKILL.md`
EOF
)" \
  --label "P1-High,gradle,build-system" \
  --milestone "Phase 2: Core Modernization"

gh issue create \
  --title "Convert NetworkPacket to Kotlin" \
  --body "$(cat <<'EOF'
## Description
Convert NetworkPacket class to Kotlin with modern patterns.

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
- Maintain protocol compatibility
- Add validation logic

## Success Criteria
- All tests pass
- Protocol compatibility verified
- Code more readable and safe

## Estimated Effort
3 hours

## Dependencies
- #8

## Related Documentation
- See `.claude/skills/android-development-SKILL.md`
- See `.claude/skills/tls-networking-SKILL.md`
- See `cosmicconnect-protocol-debug.md`
EOF
)" \
  --label "P0-Critical,android,kotlin-conversion,protocol" \
  --milestone "Phase 2: Core Modernization"

gh issue create \
  --title "Implement NetworkPacket Unit Tests" \
  --body "$(cat <<'EOF'
## Description
Create comprehensive unit tests for NetworkPacket.

## Tasks
- [ ] Test serialization/deserialization
- [ ] Test all packet types
- [ ] Test validation logic
- [ ] Test error cases
- [ ] Test edge cases
- [ ] Achieve >90% coverage

## Success Criteria
- All tests pass
- >90% code coverage
- Edge cases covered

## Estimated Effort
2 hours

## Dependencies
- #9

## Related Documentation
- See `.claude/skills/android-development-SKILL.md`
- See `.claude/skills/debugging-SKILL.md`
EOF
)" \
  --label "P0-Critical,android,testing" \
  --milestone "Phase 2: Core Modernization"

echo "✅ Created Phase 1 and Phase 2 (partial) issues"
echo ""
echo "Note: This script creates the first 10 issues."
echo "To create all 40 issues, continue running similar commands for the remaining issues."
echo ""
echo "Next steps:"
echo "1. Run: chmod +x create-github-labels.sh"
echo "2. Run: ./create-github-labels.sh"
echo "3. Run: chmod +x create-github-issues.sh"
echo "4. Run: ./create-github-issues.sh"
echo "5. View issues: gh issue list"
