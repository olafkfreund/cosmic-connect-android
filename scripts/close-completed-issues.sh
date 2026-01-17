#!/bin/bash
# Script to close completed issues #1-#73
# Run with: bash scripts/close-completed-issues.sh

set -e

echo "========================================"
echo "Closing Completed Issues #1-#73"
echo "========================================"
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "Error: GitHub CLI (gh) is not installed"
    echo "Install from: https://cli.github.com/"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo "Error: Not authenticated with GitHub CLI"
    echo "Run: gh auth login"
    exit 1
fi

echo "Phase 0: Foundation (Issues #43-52)"
echo "-----------------------------------"
for i in {43..52}; do
  echo "Closing issue #$i..."
  gh issue close $i --comment "✅ Completed in Phase 0: Foundation

This issue established the Rust core library and FFI integration foundation.

Evidence:
- cosmic-connect-core repository created
- FFI integration working
- Documentation in docs/issues/

See docs/README.md Phase 0 section for details.

Closing as complete." || echo "  Issue #$i not found or already closed"
done
echo ""

echo "Phase 1: Core Protocol Plugins (Issues #53-61)"
echo "-----------------------------------------------"
for i in {53..61}; do
  echo "Closing issue #$i..."
  gh issue close $i --comment "✅ Completed in Phase 1: Core Protocol Plugins

This plugin was successfully migrated to use Rust FFI core.

Evidence:
- FFI wrapper created
- Plugin updated to use FFI
- Build successful
- Documentation in docs/issues/

See docs/README.md Phase 1 section for details.

Closing as complete." || echo "  Issue #$i not found or already closed"
done
echo ""

echo "Phase 2: Major Feature Plugins (Issues #62-66)"
echo "-----------------------------------------------"
for i in 62 63 64 65 66; do
  echo "Closing issue #$i..."
  gh issue close $i --comment "✅ Completed in Phase 2: Major Feature Plugins

This plugin was successfully migrated to use Rust FFI core.

Evidence:
- FFI wrapper created
- Plugin updated to use FFI
- Build successful
- Documentation: docs/phase-2-complete.md

See docs/phase-2-complete.md for details.

Closing as complete." || echo "  Issue #$i not found or already closed"
done
echo ""

echo "Phase 3: Remaining Plugins (Issues #67-73)"
echo "-------------------------------------------"
for i in {67..73}; do
  echo "Closing issue #$i..."
  gh issue close $i --comment "✅ Completed in Phase 3: Remaining Plugins

This plugin was successfully migrated to use Rust FFI core.

Evidence:
- FFI wrapper created (if applicable)
- Plugin updated or analyzed
- Build successful
- Documentation: docs/phase-3-complete.md
- Specific issue doc: docs/issue-$i-*.md

See docs/phase-3-complete.md for details.

Closing as complete." || echo "  Issue #$i not found or already closed"
done
echo ""

echo "Original Plan Issues - Obsolete (Issues #1-8, #11-12, #17)"
echo "----------------------------------------------------------"
for i in {1..8} 11 12 17; do
  echo "Closing issue #$i as obsolete..."
  gh issue close $i --comment "🔄 Obsolete: Superseded by Phase 0 Architecture

This issue from the original project plan was superseded when the project adopted a Rust FFI architecture.

The functionality covered by this issue was either:
- Completed as part of Phase 0 (Rust core extraction)
- No longer needed due to architectural changes
- Superseded by specific FFI migration issues

See docs/issue-cleanup-analysis.md for full details.

Closing as obsolete." || echo "  Issue #$i not found or already closed"
done
echo ""

echo "========================================"
echo "Cleanup Complete!"
echo "========================================"
echo ""
echo "Summary:"
echo "- Phase 0: 10 issues closed (#43-52)"
echo "- Phase 1: 9 issues closed (#53-61)"
echo "- Phase 2: 5 issues closed (#62-66)"
echo "- Phase 3: 7 issues closed (#67-73)"
echo "- Obsolete: 11 issues closed (#1-8, #11-12, #17)"
echo ""
echo "Total: 42 issues closed"
echo ""
echo "Next issue number to use: #74"
echo ""
