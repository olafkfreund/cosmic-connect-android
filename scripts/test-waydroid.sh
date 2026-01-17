#!/usr/bin/env bash
#
# Automated Waydroid testing for COSMIC Connect
# Usage: ./scripts/test-waydroid.sh [--headless] [--quick]
#

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${PROJECT_ROOT}/build"
APK_PATH="${BUILD_DIR}/outputs/apk/debug/cosmicconnect-android-debug.apk"
WAYDROID_IP="192.168.240.112"
ADB_PORT="5555"
HEADLESS=false
QUICK=false

# Parse arguments
for arg in "$@"; do
  case $arg in
    --headless)
      HEADLESS=true
      shift
      ;;
    --quick)
      QUICK=true
      shift
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo ""
      echo "Options:"
      echo "  --headless    Run tests without UI (for CI/CD)"
      echo "  --quick       Skip clean build and re-initialization"
      echo "  --help        Show this help message"
      exit 0
      ;;
  esac
done

# Logging functions
log() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

success() {
  echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
  echo -e "${YELLOW}[WARNING]${NC} $1"
}

error() {
  echo -e "${RED}[ERROR]${NC} $1"
  exit 1
}

# Check if Waydroid is installed
check_waydroid() {
  log "Checking Waydroid installation..."
  if ! command -v waydroid &> /dev/null; then
    error "Waydroid not found. Please install Waydroid first."
  fi
  success "Waydroid is installed"
}

# Check if Waydroid is initialized
check_initialization() {
  log "Checking Waydroid initialization..."
  if ! waydroid status &> /dev/null; then
    log "Waydroid not initialized. Initializing now..."
    sudo waydroid init || error "Failed to initialize Waydroid"
    success "Waydroid initialized"
  else
    success "Waydroid already initialized"
  fi
}

# Start Waydroid session
start_session() {
  log "Starting Waydroid session..."

  # Check if already running
  if waydroid status | grep -q "RUNNING"; then
    success "Waydroid session already running"
    return 0
  fi

  # Start session
  waydroid session start &> /dev/null &

  # Wait for session to start (max 30 seconds)
  for i in {1..30}; do
    if waydroid status 2>/dev/null | grep -q "RUNNING"; then
      success "Waydroid session started"
      return 0
    fi
    sleep 1
  done

  error "Failed to start Waydroid session"
}

# Start Waydroid UI (if not headless)
start_ui() {
  if [ "$HEADLESS" = true ]; then
    log "Running in headless mode, skipping UI"
    return 0
  fi

  log "Starting Waydroid UI..."

  # Launch UI in background
  waydroid show-full-ui &> /dev/null &

  # Wait a bit for UI to initialize
  sleep 5

  success "Waydroid UI started"
}

# Connect ADB
connect_adb() {
  log "Connecting ADB to Waydroid..."

  # Get actual Waydroid IP (in case it changed)
  WAYDROID_IP=$(waydroid shell getprop persist.waydroid.host_ip 2>/dev/null | tr -d '\r' || echo "192.168.240.112")

  # Try to connect
  adb connect ${WAYDROID_IP}:${ADB_PORT} &> /dev/null || true

  # Wait for device
  adb wait-for-device

  # Verify connection
  if adb devices | grep -q "${WAYDROID_IP}"; then
    success "ADB connected to Waydroid at ${WAYDROID_IP}:${ADB_PORT}"
  else
    error "Failed to connect ADB"
  fi
}

# Build APK
build_apk() {
  if [ "$QUICK" = true ]; then
    log "Quick mode: Skipping clean build"
    if [ ! -f "$APK_PATH" ]; then
      warning "APK not found, building anyway..."
      QUICK=false
    fi
  fi

  cd "$PROJECT_ROOT"

  if [ "$QUICK" = false ]; then
    log "Building debug APK..."
    ./gradlew assembleDebug || error "Build failed"
    success "APK built successfully"
  fi

  if [ ! -f "$APK_PATH" ]; then
    error "APK not found at $APK_PATH"
  fi
}

# Install APK
install_apk() {
  log "Installing COSMIC Connect to Waydroid..."

  # Uninstall previous version (ignore errors)
  adb uninstall org.cosmic.cosmicconnect &> /dev/null || true

  # Install new version
  adb install -r "$APK_PATH" || error "Failed to install APK"

  success "APK installed successfully"
}

# Grant permissions
grant_permissions() {
  log "Granting runtime permissions..."

  # List of permissions COSMIC Connect needs
  PERMISSIONS=(
    "android.permission.ACCESS_FINE_LOCATION"
    "android.permission.ACCESS_COARSE_LOCATION"
    "android.permission.READ_EXTERNAL_STORAGE"
    "android.permission.WRITE_EXTERNAL_STORAGE"
    "android.permission.POST_NOTIFICATIONS"
  )

  for perm in "${PERMISSIONS[@]}"; do
    adb shell pm grant org.cosmic.cosmicconnect "$perm" 2>/dev/null || true
  done

  success "Permissions granted"
}

# Run unit tests
run_unit_tests() {
  log "Running unit tests..."
  cd "$PROJECT_ROOT"

  ./gradlew test --console=plain || error "Unit tests failed"

  success "Unit tests passed"
}

# Run instrumented tests
run_instrumented_tests() {
  log "Running instrumented tests on Waydroid..."
  cd "$PROJECT_ROOT"

  # Run all instrumented tests
  ./gradlew connectedAndroidTest --console=plain || {
    warning "Some instrumented tests failed. Check reports in build/reports/"
    return 1
  }

  success "Instrumented tests passed"
}

# Generate test report summary
generate_report() {
  log "Generating test report summary..."

  REPORT_DIR="${BUILD_DIR}/reports/androidTests/connected"

  if [ -d "$REPORT_DIR" ]; then
    log "Test reports available at: file://${REPORT_DIR}/index.html"
  fi

  # Count test results
  TEST_RESULTS=$(find "$REPORT_DIR" -name "*.xml" 2>/dev/null || echo "")

  if [ -n "$TEST_RESULTS" ]; then
    TOTAL_TESTS=$(grep -o '<testcase' "$REPORT_DIR"/*.xml 2>/dev/null | wc -l || echo "0")
    FAILED_TESTS=$(grep -o '<failure' "$REPORT_DIR"/*.xml 2>/dev/null | wc -l || echo "0")

    log "Test Summary:"
    log "  Total:  $TOTAL_TESTS"
    log "  Passed: $((TOTAL_TESTS - FAILED_TESTS))"
    log "  Failed: $FAILED_TESTS"
  fi
}

# Cleanup
cleanup() {
  log "Cleaning up..."

  if [ "$HEADLESS" = false ]; then
    # Keep session running for manual inspection
    log "Waydroid session left running for manual inspection"
    log "To stop: waydroid session stop"
  else
    # Stop session in headless mode
    waydroid session stop &> /dev/null || true
    success "Waydroid session stopped"
  fi
}

# Main execution
main() {
  echo ""
  echo "╔════════════════════════════════════════════════════════════╗"
  echo "║      COSMIC Connect - Automated Waydroid Testing          ║"
  echo "╚════════════════════════════════════════════════════════════╝"
  echo ""

  # Pre-flight checks
  check_waydroid
  check_initialization

  # Start Waydroid
  start_session
  start_ui
  connect_adb

  # Build and install
  build_apk
  install_apk
  grant_permissions

  # Run tests
  run_unit_tests
  run_instrumented_tests || true

  # Report
  generate_report

  # Cleanup
  cleanup

  echo ""
  success "✅ Automated testing complete!"
  echo ""
}

# Trap errors and cleanup
trap cleanup EXIT

# Run main
main "$@"
