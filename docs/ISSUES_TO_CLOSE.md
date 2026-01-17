# Issues to Close - Quick Reference

**Date**: 2026-01-17
**Purpose**: Quick reference for closing completed and obsolete issues

## TL;DR - What to Close

**Total Issues to Close**: 42 issues from #1 to #73

**Completed**: 31 issues (Phases 0-3)
**Obsolete**: 11 issues (Original plan, superseded)

## Quick Close List

### Close as COMPLETE (31 issues)

**Phase 0: Foundation** (#43-52) - 10 issues
```
#43, #44, #45, #46, #47, #48, #49, #50, #51, #52
```

**Phase 1: Core Protocol Plugins** (#53-61) - 9 issues
```
#53, #54, #55, #56, #57, #58, #59, #60, #61
```

**Phase 2: Major Feature Plugins** (#62-66) - 5 issues
```
#62, #63, #64, #65, #66
```

**Phase 3: Remaining Plugins** (#67-73) - 7 issues
```
#67, #68, #69, #70, #71, #72, #73
```

### Close as OBSOLETE (11 issues)

**Original Plan Issues** (superseded by Phase 0)
```
#1, #2, #3, #4, #5, #6, #7, #8, #11, #12, #17
```

## How to Close

### Option 1: Use the Script (Recommended)

```bash
# Automated closure with standard comments
bash scripts/close-completed-issues.sh
```

### Option 2: Manual Closure (GitHub Web)

For each issue:
1. Go to issue page
2. Add comment (use templates below)
3. Click "Close issue"

#### Comment Template for Completed Issues

```
✅ Completed in Phase [N]: [Phase Name]

[Brief description of what was accomplished]

Evidence:
- [Link to commit/documentation]
- [Build status]
- [Any other proof]

See docs/[relevant-doc].md for details.

Closing as complete.
```

#### Comment Template for Obsolete Issues

```
🔄 Obsolete: Superseded by Phase 0 Architecture

This issue from the original project plan was superseded when the project adopted a Rust FFI architecture.

Functionality was either:
- Completed as part of Phase 0 (Rust core extraction)
- No longer needed due to architectural changes
- Superseded by specific FFI migration issues

See docs/issue-cleanup-analysis.md for details.

Closing as obsolete.
```

### Option 3: GitHub CLI (Command Line)

```bash
# Example: Close a single issue
gh issue close 43 --comment "Completed in Phase 0. See docs/issues/"

# Example: Close multiple issues
for i in {43..52}; do
  gh issue close $i --comment "Completed in Phase 0"
done
```

## What NOT to Close

**Issues #9-10, #13-16, #18-42**: Never created, nothing to close

**Issues #74+**: Future work (Phase 4: UI Modernization)

## Issue Number Conflicts

**Note**: Some issue numbers were reused:
- #68: Used for both Presenter (Phase 2) and RemoteKeyboard (Phase 3)
- #69: Used for both Connectivity (Phase 2) and Digitizer (Phase 3)

Both uses are completed. Close once, documents both completions.

## Next Issue Number

**Start Phase 4 issues from**: #74

## Verification

After closing, verify:
```bash
# Check open issues
gh issue list

# Should see no issues #1-73 in open state
```

## Full Details

For complete analysis including:
- Detailed status of each issue
- Evidence of completion
- Rationale for obsolescence
- Conflict resolution

See: `docs/issue-cleanup-analysis.md`

---

**Created**: 2026-01-17
**Script**: `scripts/close-completed-issues.sh`
**Analysis**: `docs/issue-cleanup-analysis.md`
