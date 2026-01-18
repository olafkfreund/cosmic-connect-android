#!/usr/bin/env bash
#
# Automated testing for Samsung Galaxy Tab S8 Ultra
# Usage: ./scripts/test-samsung.sh [--wireless] [--quick]
#

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${PROJECT_ROOT}/build"
APK_PATH="${BUILD_DIR}/outputs/apk/debug/cosmicconnect-android-debug.apk"
WIRELESS=false
QUICK=false
TABLET_IP="192.168.1.100"  # UPDATE with your tablet IP
TABLET_PORT="45678"         # UPDATE with wireless debugging port

# Parse arguments
for arg in "$@"; do
  case $arg in
    --wireless)
      WIRELESS=true
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
      echo "  --wireless    Use wireless ADB connection"
      echo "  --quick       Skip clean build"
      echo "  --help        Show this help message"
      echo ""
      echo "Examples:"
      echo "  $0                           # USB connection, full build"
      echo "  $0 --wireless                # Wireless connection"
      echo "  $0 --wireless --quick        # Wireless, skip clean build"
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

# Connect to device
connect_device() {
  log "Connecting to Samsung Tab S8 Ultra..."

  if [ "$WIRELESS" = true ]; then
    log "Using wireless ADB connection..."
    adb connect ${TABLET_IP}:${TABLET_PORT} || error "Failed to connect wirelessly. Run ./scripts/connect-tablet.sh to diagnose."
    sleep 2
  fi

  # Wait for device
  adb wait-for-device

  # Get device info
  DEVICE=$(adb devices | grep -v "List" | awk 'NF && $2=="device" {print $1; exit}')
  if [ -z "$DEVICE" ]; then
    error "No device connected. Check USB cable or wireless connection."
  fi

  MODEL=$(adb -s "$DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r')

  if [ "$MODEL" != "SM-X900" ]; then
    warning "Connected device is '$MODEL' (expected SM-X900)"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
  fi

  export ANDROID_SERIAL="$DEVICE"
  success "Connected to $MODEL ($DEVICE)"
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
  log "Installing COSMIC Connect to Samsung tablet..."

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
    "android.permission.BLUETOOTH_SCAN"
    "android.permission.BLUETOOTH_CONNECT"
    "android.permission.BLUETOOTH_ADVERTISE"
  )

  for perm in "${PERMISSIONS[@]}"; do
    adb shell pm grant org.cosmic.cosmicconnect "$perm" 2>/dev/null || true
  done

  success "Permissions granted (including Bluetooth)"
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
  log "Running instrumented tests on Samsung tablet..."
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
  if [ -d "$REPORT_DIR" ]; then
    TEST_XML=$(find "$REPORT_DIR" -name "*.xml" 2>/dev/null || echo "")

    if [ -n "$TEST_XML" ]; then
      TOTAL_TESTS=$(grep -o '<testcase' $TEST_XML 2>/dev/null | wc -l || echo "0")
      FAILED_TESTS=$(grep -o '<failure' $TEST_XML 2>/dev/null | wc -l || echo "0")

      log "Test Summary:"
      log "  Total:  $TOTAL_TESTS"
      log "  Passed: $((TOTAL_TESTS - FAILED_TESTS))"
      log "  Failed: $FAILED_TESTS"
    fi
  fi
}

# Cleanup
cleanup() {
  log "Cleaning up..."

  if [ "$WIRELESS" = true ]; then
    log "Wireless connection left active for further testing"
    log "To disconnect: adb disconnect ${TABLET_IP}:${TABLET_PORT}"
  fi
}

# Main execution
main() {
  echo ""
  echo "╔════════════════════════════════════════════════════════════╗"
  echo "║   COSMIC Connect - Samsung Tab S8 Ultra Testing           ║"
  echo "╚════════════════════════════════════════════════════════════╝"
  echo ""

  # Pre-flight checks
  connect_device

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
  success "✅ Samsung tablet testing complete!"
  echo ""
  echo "💡 Next steps:"
  echo "  • Run Bluetooth tests: ./scripts/test-bluetooth.sh $ANDROID_SERIAL"
  echo "  • View test reports: xdg-open build/reports/androidTests/connected/index.html"
  echo ""
}

# Trap errors and cleanup
trap cleanup EXIT

# Run main
main "$@"
