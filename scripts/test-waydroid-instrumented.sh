#!/usr/bin/env bash
#
# Automated Waydroid Instrumented Testing for COSMIC Connect
# Skips unit tests and runs only connectedAndroidTest
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

log() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

error() {
  echo -e "${RED}[ERROR]${NC} $1"
  exit 1
}

success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

main() {
  log "Starting Waydroid Instrumented Tests..."
  cd "$PROJECT_ROOT"

  # Run instrumented tests
  ./gradlew connectedAndroidTest --console=plain || error "Instrumented tests failed"

  success "Instrumented tests passed"
}

main "$@"
