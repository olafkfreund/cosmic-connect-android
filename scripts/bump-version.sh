#!/usr/bin/env bash

# COSMIC Connect Android — Version Bump Script
#
# Updates versionName, increments versionCode, updates CHANGELOG,
# creates a git commit and tag.
#
# Usage: ./scripts/bump-version.sh 1.2.0-beta

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_GRADLE="$PROJECT_ROOT/app/build.gradle.kts"
CHANGELOG="$PROJECT_ROOT/CHANGELOG.md"

log() { echo -e "${BLUE}[version]${NC} $*"; }
ok()  { echo -e "${GREEN}  ✓${NC} $*"; }
err() { echo -e "${RED}  ✗${NC} $*"; }

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <version>"
    echo "  Example: $0 1.2.0-beta"
    echo "  Example: $0 1.3.0"
    exit 1
fi

NEW_VERSION="$1"

# Validate version format (semver with optional pre-release)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.]+)?$ ]]; then
    err "Invalid version format: $NEW_VERSION"
    echo "  Expected: MAJOR.MINOR.PATCH[-prerelease]"
    exit 1
fi

cd "$PROJECT_ROOT"

# Check for clean working tree
if ! git diff --quiet HEAD 2>/dev/null; then
    err "Working tree is not clean. Commit or stash changes first."
    exit 1
fi

log "Bumping version to $NEW_VERSION"
echo

# --- Update versionName ---
log "Updating versionName in build.gradle.kts..."
CURRENT_NAME=$(grep -oP 'versionName = "\K[^"]+' "$BUILD_GRADLE")
sed -i "s/versionName = \"$CURRENT_NAME\"/versionName = \"$NEW_VERSION\"/" "$BUILD_GRADLE"
ok "versionName: $CURRENT_NAME → $NEW_VERSION"

# --- Increment versionCode ---
log "Incrementing versionCode..."
CURRENT_CODE=$(grep -oP 'versionCode = \K[0-9]+' "$BUILD_GRADLE")
NEW_CODE=$((CURRENT_CODE + 1))
sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$BUILD_GRADLE"
ok "versionCode: $CURRENT_CODE → $NEW_CODE"

# --- Update CHANGELOG ---
log "Updating CHANGELOG.md..."
TODAY=$(date +%Y-%m-%d)
sed -i "s/## \[Unreleased\]/## [Unreleased]\n\n---\n\n## [$NEW_VERSION] - $TODAY/" "$CHANGELOG"
ok "Added [$NEW_VERSION] - $TODAY section"

# --- Update version footer ---
sed -i "s/^**Version:** .*/**Version:** $NEW_VERSION/" "$CHANGELOG"

echo

# --- Git commit and tag ---
log "Creating git commit..."
git add "$BUILD_GRADLE" "$CHANGELOG"
git commit -m "release: bump version to $NEW_VERSION

- versionName: $CURRENT_NAME → $NEW_VERSION
- versionCode: $CURRENT_CODE → $NEW_CODE
- Updated CHANGELOG.md"
ok "Committed"

log "Creating git tag v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Release $NEW_VERSION"
ok "Tagged v$NEW_VERSION"

echo
log "Done! To push:"
echo "  git push origin master --tags"
