#!/usr/bin/env bash

# COSMIC Connect Android — Local Release Build Script
#
# Builds a signed release APK with full validation.
# Requires: KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD env vars
#           (or pass --unsigned to skip signing)

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
UNSIGNED=false

usage() {
    echo "Usage: $0 [--unsigned]"
    echo
    echo "Options:"
    echo "  --unsigned    Build without signing (for local testing)"
    echo
    echo "Environment variables (required unless --unsigned):"
    echo "  KEYSTORE_FILE       Path to the release keystore"
    echo "  KEYSTORE_PASSWORD   Keystore password"
    echo "  KEY_ALIAS           Key alias"
    echo "  KEY_PASSWORD        Key password"
    exit 1
}

log() { echo -e "${BLUE}[build]${NC} $*"; }
ok()  { echo -e "${GREEN}  ✓${NC} $*"; }
err() { echo -e "${RED}  ✗${NC} $*"; }
warn() { echo -e "${YELLOW}  !${NC} $*"; }

for arg in "$@"; do
    case "$arg" in
        --unsigned) UNSIGNED=true ;;
        --help|-h) usage ;;
        *) echo "Unknown option: $arg"; usage ;;
    esac
done

cd "$PROJECT_ROOT"

log "COSMIC Connect Android — Release Build"
echo

# --- Validate environment ---
log "Validating environment..."

if ! command -v java &>/dev/null; then
    err "Java not found. Install JDK 17+."
    exit 1
fi
ok "Java found: $(java -version 2>&1 | head -1)"

if ! command -v cargo &>/dev/null; then
    err "Rust/Cargo not found. Install Rust toolchain."
    exit 1
fi
ok "Cargo found: $(cargo --version)"

if [[ "$UNSIGNED" == false ]]; then
    if [[ -z "${KEYSTORE_FILE:-}" ]]; then
        err "KEYSTORE_FILE not set. Use --unsigned for unsigned builds."
        exit 1
    fi
    if [[ ! -f "$KEYSTORE_FILE" ]]; then
        err "Keystore not found at: $KEYSTORE_FILE"
        exit 1
    fi
    if [[ -z "${KEYSTORE_PASSWORD:-}" || -z "${KEY_ALIAS:-}" || -z "${KEY_PASSWORD:-}" ]]; then
        err "KEYSTORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD must all be set."
        exit 1
    fi
    ok "Signing config validated"
else
    warn "Building unsigned APK (--unsigned)"
fi

echo

# --- Build Rust native libraries ---
log "Building Rust native libraries (all 4 ABIs)..."
./gradlew cargoBuild
ok "Rust native libraries built"

echo

# --- Run unit tests ---
log "Running unit tests..."
./gradlew testDebugUnitTest
ok "All unit tests passed"

echo

# --- Build release APK ---
log "Building release APK..."
./gradlew assembleRelease
ok "Release APK built"

echo

# --- Generate checksums ---
APK_DIR="app/build/outputs/apk/release"
log "Generating SHA256 checksums..."

if [[ -d "$APK_DIR" ]]; then
    cd "$APK_DIR"
    for apk in *.apk; do
        sha256sum "$apk" > "${apk}.sha256"
        ok "$apk → $(cat "${apk}.sha256" | cut -d' ' -f1 | head -c 16)..."
    done
    cd "$PROJECT_ROOT"
else
    warn "No release APK found in $APK_DIR"
fi

echo
log "Build complete!"
echo
echo "  APK:       $APK_DIR/"
echo "  Checksums: $APK_DIR/*.sha256"
echo
if [[ "$UNSIGNED" == true ]]; then
    warn "APK is unsigned — not suitable for distribution"
fi
