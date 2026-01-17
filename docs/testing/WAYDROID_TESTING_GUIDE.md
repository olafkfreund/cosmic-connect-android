# Waydroid Testing Guide for COSMIC Connect Android

> **Version**: 1.0.0
> **Last Updated**: 2026-01-17
> **Platform**: NixOS with Waydroid
> **Status**: Production Ready

<div align="center">

**Complete guide for automated Android app testing using Waydroid on NixOS**

[Quick Start](#quick-start) • [Capabilities](#waydroid-capabilities) • [Automation](#test-automation) • [CI/CD](#cicd-integration) • [Troubleshooting](#troubleshooting)

</div>

---

## Table of Contents

1. [Overview](#overview)
2. [Waydroid Capabilities](#waydroid-capabilities)
3. [When to Use Real Device](#when-to-use-real-device)
4. [NixOS Configuration](#nixos-configuration)
5. [Quick Start](#quick-start)
6. [Test Automation](#test-automation)
7. [CI/CD Integration](#cicd-integration)
8. [Advanced Usage](#advanced-usage)
9. [Troubleshooting](#troubleshooting)
10. [Performance Optimization](#performance-optimization)

---

## Overview

[Waydroid](https://waydro.id/) is a container-based Android emulator that runs Android applications on Linux using LXC containers. It offers **near-native performance** and is ideal for automated testing on NixOS.

### Why Waydroid for COSMIC Connect Testing?

**Advantages:**
- ✅ **Native Performance**: Runs in container, not full virtualization
- ✅ **Fast Boot**: ~5-10 seconds vs minutes for emulators
- ✅ **Low Overhead**: Minimal resource usage
- ✅ **ADB Support**: Full Android Debug Bridge integration
- ✅ **Automation Friendly**: Works with Gradle, Espresso, instrumented tests
- ✅ **NixOS Integration**: Declarative configuration
- ✅ **Headless Mode**: Can run without GUI for CI/CD

**Key Limitations:**
- ❌ **No Bluetooth**: Cannot test Bluetooth pairing/connectivity
- ❌ **No Telephony**: Cannot test SMS/call features realistically
- ❌ **No Camera**: Cannot test camera-related features
- ❌ **No NFC**: Cannot test NFC functionality
- ⚠️ **Wayland Required**: Needs Wayland compositor (or nested session)
- ⚠️ **Network via Host**: Uses host's network stack

### Research Sources

This guide is based on official documentation and community research:

- [Waydroid Official Documentation](https://docs.waydro.id/)
- [NixOS Waydroid Wiki](https://wiki.nixos.org/wiki/Waydroid)
- [Waydroid GitHub](https://github.com/waydroid/waydroid)
- [Android Instrumentation Testing](https://source.android.com/docs/core/tests/development/instrumentation)
- [Espresso Testing Framework](https://www.getautonoma.com/blog/how-to-test-android-apps-with-espresso)
- [Top Android Emulators 2026](https://dev.to/morrismoses149/top-12-android-emulators-in-2026-best-emulator-for-android-pc-and-testing-5bjh)

---

## Waydroid Capabilities

### ✅ What Waydroid CAN Test

**COSMIC Connect Features:**
1. **Device Discovery (Wi-Fi/Network)** ✅
   - UDP multicast discovery
   - Network packet sending/receiving
   - mDNS service announcement

2. **Pairing (Network-based)** ✅
   - TLS certificate exchange
   - Pairing request/accept flow
   - Certificate validation
   - Trust establishment

3. **File Transfer** ✅
   - Send/receive files
   - Progress tracking
   - Concurrent transfers
   - Large file handling

4. **Clipboard Sync** ✅
   - Text clipboard sync
   - Copy/paste operations

5. **Notification Sync** ✅
   - Send notifications to desktop
   - Notification listener service
   - App selection

6. **Media Control** ✅
   - MPRIS plugin
   - Media player control

7. **Find My Phone** ✅
   - Ring command
   - Volume control

8. **Remote Input** ✅
   - Touchpad emulation
   - Keyboard input

9. **Run Commands** ✅
   - Execute commands
   - Command response

10. **Battery Monitoring** ✅
    - Battery status reporting
    - Charge level updates

### ❌ What Waydroid CANNOT Test

**COSMIC Connect Limitations:**

1. **Bluetooth Pairing** ❌
   - **Issue**: No Bluetooth stack in container
   - **Source**: [Waydroid Bluetooth Issue #155](https://github.com/waydroid/waydroid/issues/155)
   - **Workaround**: Use Samsung tablet for Bluetooth testing
   - **Impact**: Cannot test Bluetooth discovery/pairing flows

2. **Telephony Features (Limited)** ⚠️
   - **Issue**: No real cellular radio
   - **Source**: [LWN Article](https://lwn.net/Articles/901459/)
   - **Workaround**: Can simulate SMS/calls via ADB, but not realistic
   - **Impact**: Limited telephony plugin testing

3. **Camera Features** ❌
   - **Issue**: No camera hardware access
   - **Source**: [Waydroid Camera Discussion](https://codema.in/d/mEO9zb4g/waydroid-camera-and-bluetooth-support)
   - **Workaround**: Use real device if needed
   - **Impact**: Cannot test camera-based features (not used in COSMIC Connect)

4. **NFC** ❌
   - **Issue**: No NFC hardware support
   - **Source**: [Waydroid NFC Discussion](https://furilabs.com/forum/flx1/bluetooth-pass-through-like-nfc-to-waydroid/)
   - **Impact**: Cannot test NFC features (not used in COSMIC Connect)

### Testing Coverage for COSMIC Connect

**Overall Coverage**: **~85-90%** of COSMIC Connect features can be tested with Waydroid

**Testing Matrix:**

| Feature | Waydroid | Real Device | Priority |
|---------|----------|-------------|----------|
| Network Discovery | ✅ | ✅ | High |
| Network Pairing | ✅ | ✅ | High |
| Bluetooth Pairing | ❌ | ✅ | Medium |
| File Transfer | ✅ | ✅ | High |
| Clipboard Sync | ✅ | ✅ | High |
| Notification Sync | ✅ | ✅ | High |
| Media Control | ✅ | ✅ | Medium |
| Find My Phone | ✅ | ✅ | Medium |
| Remote Input | ✅ | ✅ | Low |
| Run Commands | ✅ | ✅ | Medium |
| Battery Monitoring | ✅ | ✅ | Medium |
| Telephony (SMS/Calls) | ⚠️ | ✅ | Low |

**Recommendation**: Use Waydroid for 90% of automated testing, real device for Bluetooth and comprehensive E2E validation.

---

## When to Use Real Device

### Samsung Tablet Testing Scenarios

**Use your Samsung tablet for:**

1. **Bluetooth Testing** (Critical)
   - Bluetooth device discovery
   - Bluetooth pairing
   - Bluetooth data transfer
   - Bluetooth stability testing

2. **Hardware-Specific Issues**
   - Samsung-specific bugs
   - Manufacturer customizations
   - Device-specific performance

3. **Final E2E Validation**
   - Pre-release testing
   - User acceptance testing
   - Complete feature validation

4. **Real-World Network Conditions**
   - Weak Wi-Fi signal handling
   - Network switching (Wi-Fi ↔ mobile data)
   - Roaming scenarios

### Recommended Testing Strategy

```
┌─────────────────────────────────────────────┐
│         Development Cycle                    │
├─────────────────────────────────────────────┤
│ 1. Unit Tests (Local)            │ Always   │
│ 2. Integration Tests (Waydroid)  │ Always   │
│ 3. E2E Tests (Waydroid)          │ Always   │
│ 4. Performance Tests (Waydroid)  │ Always   │
│ 5. Bluetooth Tests (Samsung)     │ Weekly   │
│ 6. Full E2E (Samsung)            │ Pre-rel  │
└─────────────────────────────────────────────┘
```

**Automation Strategy:**
- **Waydroid**: 95% of automated CI/CD testing
- **Samsung Tablet**: 5% manual/semi-automated validation

---

## NixOS Configuration

### System Configuration

Add Waydroid to your NixOS configuration:

```nix
# /etc/nixos/configuration.nix or flake-based config

{
  # Enable Waydroid
  virtualisation.waydroid.enable = true;

  # Required kernel modules
  boot.kernelModules = [ "ashmem_linux" "binder_linux" ];

  # Optional: Better performance
  boot.kernel.sysctl = {
    "net.ipv4.ip_forward" = 1;
  };

  # Optional: Firewall rules for COSMIC Connect
  networking.firewall = {
    allowedUDPPorts = [ 1716 ];  # KDE Connect discovery
    allowedTCPPorts = [ 1716 ];  # KDE Connect data
  };

  # Ensure user is in required groups
  users.users.YOUR_USERNAME = {
    extraGroups = [ "waydroid" ];
  };
}
```

### Project Flake Integration

Update `flake.nix` to include Waydroid tools:

```nix
# Add to buildInputs in default devShell
buildInputs = with pkgs; [
  # ... existing packages ...

  # Waydroid testing
  waydroid
  wl-clipboard      # Clipboard for Wayland
  cage              # Nested Wayland compositor (for X11 hosts)
];

shellHook = ''
  # ... existing shellHook ...

  echo "📱 Waydroid Testing:"
  echo "  • Start session:    waydroid session start"
  echo "  • Start UI:         waydroid show-full-ui"
  echo "  • Install APK:      waydroid app install path/to/app.apk"
  echo "  • List apps:        waydroid app list"
  echo "  • Launch app:       waydroid app launch org.cosmic.cosmicconnect"
  echo "  • ADB connect:      adb connect 192.168.240.112:5555"
  echo "  • Stop session:     waydroid session stop"
  echo ""
'';
```

**Apply Configuration:**
```bash
# System-wide changes
sudo nixos-rebuild switch

# Project-specific (reload flake)
nix develop
```

---

## Quick Start

### 1. Initialize Waydroid (First Time Only)

```bash
# Initialize Waydroid with GApps (Google apps) or vanilla
sudo waydroid init

# Or with Google Apps (for Play Store, etc.)
sudo waydroid init -s GAPPS

# Initialize with specific Android version (optional)
sudo waydroid init -c https://ota.waydro.id/system -v lineage-18.1
```

**Expected Output:**
```
[13:57:27] Downloading https://ota.waydro.id/system...
[13:58:43] Download complete
[13:58:43] Extracting...
[13:59:12] Initialization complete
```

### 2. Start Waydroid Session

```bash
# Start Waydroid background session
waydroid session start

# Verify session is running
waydroid status
```

**Expected Output:**
```
Session:        RUNNING
Container:      RUNNING
Vendor type:    MAINLINE
Session user:   olafkfreund(1000)
Wayland display:wayland-0
```

### 3. Launch Waydroid UI

```bash
# Show full Android UI (for manual testing/debugging)
waydroid show-full-ui

# Or launch in windowed mode
waydroid show-full-ui --windowed
```

### 4. Connect ADB

```bash
# Get Waydroid IP address
WAYDROID_IP=$(waydroid shell getprop persist.waydroid.host_ip | tr -d '\r')
echo "Waydroid IP: $WAYDROID_IP"

# Connect ADB (usually 192.168.240.112)
adb connect ${WAYDROID_IP}:5555

# Verify connection
adb devices
```

**Expected Output:**
```
List of devices attached
192.168.240.112:5555    device
```

### 5. Build and Install COSMIC Connect

```bash
# Navigate to project
cd /home/olafkfreund/Source/GitHub/cosmic-connect-android

# Build debug APK
./gradlew assembleDebug

# Install to Waydroid
adb install -r build/outputs/apk/debug/cosmicconnect-android-debug-*.apk

# Or use Waydroid command
waydroid app install build/outputs/apk/debug/cosmicconnect-android-debug-*.apk

# Launch app
waydroid app launch org.cosmic.cosmicconnect
```

### 6. Run Automated Tests

```bash
# Unit tests (no device needed)
./gradlew test

# Instrumented tests on Waydroid
./gradlew connectedAndroidTest

# Specific test class
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.integration.DiscoveryPairingTest
```

---

## Test Automation

### Automated Test Script

Create `scripts/test-waydroid.sh`:

```bash
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
```

**Make executable:**
```bash
chmod +x scripts/test-waydroid.sh
```

**Usage:**
```bash
# Full automated test run
./scripts/test-waydroid.sh

# Headless mode (for CI/CD)
./scripts/test-waydroid.sh --headless

# Quick mode (skip clean build)
./scripts/test-waydroid.sh --quick

# Headless + quick (fastest CI/CD)
./scripts/test-waydroid.sh --headless --quick
```

---

## CI/CD Integration

### GitHub Actions Workflow

Create `.github/workflows/waydroid-tests.yml`:

```yaml
name: Waydroid Automated Tests

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master ]

jobs:
  waydroid-tests:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Set up Java
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Set up Rust
      uses: actions-rs/toolchain@v1
      with:
        toolchain: stable
        target: aarch64-linux-android
        override: true

    - name: Install cargo-ndk
      run: cargo install cargo-ndk

    - name: Install Waydroid dependencies
      run: |
        sudo apt-get update
        sudo apt-get install -y \
          lxc \
          python3 \
          python3-pip \
          curl \
          ca-certificates

    - name: Install Waydroid
      run: |
        curl https://repo.waydro.id | sudo bash
        sudo apt install waydroid -y

    - name: Initialize Waydroid
      run: |
        sudo waydroid init -s VANILLA

    - name: Start Waydroid session
      run: |
        sudo waydroid session start &
        sleep 10

    - name: Connect ADB
      run: |
        adb connect 192.168.240.112:5555
        adb wait-for-device

    - name: Build Rust core
      run: ./gradlew cargoBuild

    - name: Build debug APK
      run: ./gradlew assembleDebug

    - name: Run unit tests
      run: ./gradlew test

    - name: Install APK to Waydroid
      run: |
        adb install -r build/outputs/apk/debug/cosmicconnect-android-debug-*.apk

    - name: Grant permissions
      run: |
        adb shell pm grant org.cosmic.cosmicconnect android.permission.ACCESS_FINE_LOCATION
        adb shell pm grant org.cosmic.cosmicconnect android.permission.READ_EXTERNAL_STORAGE
        adb shell pm grant org.cosmic.cosmicconnect android.permission.WRITE_EXTERNAL_STORAGE

    - name: Run instrumented tests
      run: ./gradlew connectedAndroidTest

    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: test-reports
        path: build/reports/androidTests/

    - name: Stop Waydroid
      if: always()
      run: sudo waydroid session stop
```

### NixOS Flake CI Integration

Create `scripts/ci-test.sh`:

```bash
#!/usr/bin/env bash
# CI/CD testing with Nix + Waydroid

set -euo pipefail

# Enter Nix development environment
nix develop --command bash << 'EOF'
  # Run automated tests
  ./scripts/test-waydroid.sh --headless --quick
EOF
```

**Make executable:**
```bash
chmod +x scripts/ci-test.sh
```

---

## Advanced Usage

### Headless Testing (No GUI)

For CI/CD or automated testing without GUI:

```bash
# Start Waydroid session without UI
waydroid session start

# Connect ADB
adb connect 192.168.240.112:5555

# Install and test
adb install -r app.apk
adb shell am instrument -w org.cosmic.cosmicconnect.test/androidx.test.runner.AndroidJUnitRunner

# Get logcat
adb logcat -d > test-logs.txt

# Stop session
waydroid session stop
```

### Parallel Testing

Run multiple test suites in parallel:

```bash
#!/usr/bin/env bash
# Parallel test execution

# Start Waydroid
waydroid session start
adb connect 192.168.240.112:5555
adb install -r app.apk

# Run test suites in parallel
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.integration.DiscoveryPairingTest &
PID1=$!

./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.integration.FileTransferTest &
PID2=$!

./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.integration.PluginTest &
PID3=$!

# Wait for all tests to complete
wait $PID1 $PID2 $PID3

echo "All parallel tests complete"
```

### Custom Android Version

Use specific Android version for testing:

```bash
# Reinitialize with specific LineageOS version
sudo waydroid init -f -c https://ota.waydro.id/system -v lineage-18.1  # Android 11
sudo waydroid init -f -c https://ota.waydro.id/system -v lineage-19.1  # Android 12
sudo waydroid init -f -c https://ota.waydro.id/system -v lineage-20.0  # Android 13
```

### Network Testing

Test different network conditions:

```bash
# Simulate slow network
adb shell settings put global network_speed slow

# Simulate poor connectivity
adb shell settings put global network_quality poor

# Reset to normal
adb shell settings put global network_speed full
adb shell settings put global network_quality high

# Disable network
adb shell svc wifi disable
adb shell svc data disable

# Re-enable network
adb shell svc wifi enable
adb shell svc data enable
```

---

## Troubleshooting

### Issue: Waydroid Session Won't Start

**Symptoms:**
```
Failed to start Waydroid session
```

**Solutions:**

1. **Check kernel modules:**
   ```bash
   lsmod | grep binder
   lsmod | grep ashmem

   # If missing, load them
   sudo modprobe binder_linux
   sudo modprobe ashmem_linux
   ```

2. **Reinitialize Waydroid:**
   ```bash
   sudo waydroid init -f
   ```

3. **Check logs:**
   ```bash
   sudo journalctl -u waydroid-container -f
   ```

### Issue: ADB Connection Fails

**Symptoms:**
```
unable to connect to 192.168.240.112:5555
```

**Solutions:**

1. **Verify Waydroid IP:**
   ```bash
   waydroid shell getprop persist.waydroid.host_ip
   ```

2. **Restart ADB server:**
   ```bash
   adb kill-server
   adb start-server
   adb connect 192.168.240.112:5555
   ```

3. **Check network:**
   ```bash
   ping 192.168.240.112
   ```

### Issue: App Installation Fails

**Symptoms:**
```
INSTALL_FAILED_INSUFFICIENT_STORAGE
```

**Solutions:**

1. **Check Waydroid storage:**
   ```bash
   adb shell df /data
   ```

2. **Clean up space:**
   ```bash
   # Uninstall unused apps
   adb shell pm list packages
   adb uninstall com.example.unusedapp

   # Clear cache
   adb shell pm clear org.cosmic.cosmicconnect
   ```

3. **Increase Waydroid partition size:**
   ```bash
   # Stop session
   waydroid session stop

   # Resize (example: 8GB)
   sudo waydroid upgrade
   ```

### Issue: Tests Fail with "Device Offline"

**Symptoms:**
```
error: device offline
```

**Solutions:**

1. **Restart Waydroid:**
   ```bash
   waydroid session stop
   waydroid session start
   adb connect 192.168.240.112:5555
   ```

2. **Check Waydroid status:**
   ```bash
   waydroid status
   ```

### Issue: Slow Performance

**Symptoms:**
- Slow app launching
- Laggy UI
- Test timeouts

**Solutions:**

1. **Allocate more resources:**
   ```bash
   # Edit Waydroid config
   sudo nano /var/lib/waydroid/waydroid.cfg

   # Increase memory
   [properties]
   ro.hardware.ram=4096
   ```

2. **Use CPU pinning:**
   ```bash
   # Pin Waydroid to specific CPU cores
   sudo systemctl edit waydroid-container

   [Service]
   CPUAffinity=0-3
   ```

3. **Disable animations:**
   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```

---

## Performance Optimization

### Waydroid Configuration Tuning

Create/edit `/var/lib/waydroid/waydroid.cfg`:

```ini
[waydroid]
images_path=/var/lib/waydroid/images
user_manager=None
session_manager=None

[properties]
# Performance optimizations
ro.hardware.ram=4096
ro.hardware.cpu.count=4
dalvik.vm.heapsize=512m
persist.waydroid.multi_windows=true
persist.waydroid.invert_colors=false

# Network
persist.waydroid.fake_wifi=192.168.240.1,24,192.168.240.1
```

### NixOS System Optimization

Add to `/etc/nixos/configuration.nix`:

```nix
{
  # Kernel optimization for containers
  boot.kernel.sysctl = {
    "kernel.unprivileged_userns_clone" = 1;
    "net.ipv4.ip_forward" = 1;
    "net.ipv4.conf.all.forwarding" = 1;
  };

  # Better I/O for Waydroid
  boot.kernelParams = [ "elevator=noop" ];

  # Zram for better performance
  zramSwap = {
    enable = true;
    memoryPercent = 50;
  };
}
```

### Test Execution Optimization

**Gradle Configuration** (`gradle.properties`):

```properties
# Parallel execution
org.gradle.parallel=true
org.gradle.workers.max=4

# Daemon
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m

# Faster builds
org.gradle.caching=true
org.gradle.configureondemand=true

# Android test optimization
android.enableJetifier=false
android.useAndroidX=true
```

**Test Timeouts** (`build.gradle.kts`):

```kotlin
android {
    testOptions {
        animationsDisabled = true

        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
        }

        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    androidTestUtil("androidx.test:orchestrator:1.4.2")
}
```

---

## Testing Workflow Examples

### Example 1: Quick Feature Test

Test a single feature quickly:

```bash
#!/usr/bin/env bash
# Test clipboard sync feature

# Start Waydroid (if not running)
waydroid session start
adb connect 192.168.240.112:5555

# Build and install
./gradlew assembleDebug
adb install -r build/outputs/apk/debug/*.apk

# Grant permissions
adb shell pm grant org.cosmic.cosmicconnect android.permission.READ_EXTERNAL_STORAGE

# Run clipboard tests only
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.integration.ClipboardTest

# View results
xdg-open build/reports/androidTests/connected/index.html
```

### Example 2: Full Regression Suite

Run complete test suite before release:

```bash
#!/usr/bin/env bash
# Full regression test suite

set -e

echo "🧪 Starting full regression test suite..."

# 1. Clean build
./gradlew clean

# 2. Build Rust core
./gradlew cargoBuild

# 3. Build APKs
./gradlew assembleDebug assembleRelease

# 4. Unit tests
echo "Running unit tests..."
./gradlew test

# 5. Start Waydroid
waydroid session start
adb connect 192.168.240.112:5555

# 6. Install debug APK
adb install -r build/outputs/apk/debug/*.apk

# 7. Grant all permissions
PERMISSIONS=(
  "android.permission.ACCESS_FINE_LOCATION"
  "android.permission.READ_EXTERNAL_STORAGE"
  "android.permission.WRITE_EXTERNAL_STORAGE"
  "android.permission.POST_NOTIFICATIONS"
)

for perm in "${PERMISSIONS[@]}"; do
  adb shell pm grant org.cosmic.cosmicconnect "$perm"
done

# 8. Run all instrumented tests
echo "Running instrumented tests..."
./gradlew connectedAndroidTest

# 9. Performance tests
echo "Running performance tests..."
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cosmicconnect.performance.PerformanceBenchmarkTest

# 10. Generate reports
echo "Generating test reports..."
./gradlew jacocoTestReport

echo "✅ Full regression suite complete!"
echo "📊 Reports: build/reports/"
```

### Example 3: Continuous Testing (Watch Mode)

Auto-run tests on file changes:

```bash
#!/usr/bin/env bash
# Continuous testing - watch for changes

echo "👀 Watching for file changes..."
echo "Press Ctrl+C to stop"

# Ensure Waydroid is running
waydroid session start &> /dev/null || true
adb connect 192.168.240.112:5555 &> /dev/null || true

# Watch for Kotlin file changes
while inotifywait -r -e modify,create src/; do
  echo ""
  echo "📝 File changed, running tests..."

  # Build and install
  ./gradlew assembleDebug && \
  adb install -r build/outputs/apk/debug/*.apk && \

  # Run tests
  ./gradlew connectedAndroidTest || true

  echo "✅ Test run complete. Watching for more changes..."
done
```

---

## Summary

### Waydroid Testing Capabilities

**✅ Can Test (85-90% of COSMIC Connect):**
- Network-based device discovery
- Wi-Fi pairing and TLS handshake
- File transfers (all sizes)
- Clipboard synchronization
- Notification syncing
- Media control
- Remote input
- Battery monitoring
- Most plugins

**❌ Cannot Test (10-15% of COSMIC Connect):**
- Bluetooth device discovery
- Bluetooth pairing
- Real telephony (SMS/calls)
- Camera features (not used)
- NFC (not used)

### Recommended Testing Strategy

```
Daily Development:
├─ Waydroid: Unit + Integration + E2E tests (automated)
├─ Local: Quick manual testing (Waydroid)
└─ Weekly: Bluetooth tests (Samsung tablet)

Pre-Release:
├─ Waydroid: Full automated regression
├─ Samsung: Complete E2E validation
└─ Samsung: Bluetooth comprehensive testing
```

### Automation Setup

1. **NixOS Configuration**: Enable Waydroid system-wide
2. **Project Flake**: Include Waydroid in dev shell
3. **Test Script**: Use `scripts/test-waydroid.sh` for automation
4. **CI/CD**: GitHub Actions with Waydroid
5. **Performance**: Tune Waydroid and Gradle configurations

### Next Steps

1. Apply NixOS configuration
2. Run initialization: `sudo waydroid init`
3. Test the automation script: `./scripts/test-waydroid.sh`
4. Set up CI/CD workflow
5. Schedule regular Samsung tablet testing for Bluetooth

---

## Additional Resources

### Official Documentation
- [Waydroid Official Docs](https://docs.waydro.id/)
- [Waydroid GitHub](https://github.com/waydroid/waydroid)
- [NixOS Waydroid Wiki](https://wiki.nixos.org/wiki/Waydroid)

### Android Testing
- [Android Instrumentation Testing](https://source.android.com/docs/core/tests/development/instrumentation)
- [Espresso Testing Guide](https://www.getautonoma.com/blog/how-to-test-android-apps-with-espresso)
- [Android Testing Best Practices](https://developer.android.com/training/testing)

### Community
- [Waydroid on NixOS Discourse](https://discourse.nixos.org/t/anyone-got-waydroid-or-genymotion-working/44397)
- [Waydroid Telegram](https://t.me/WayDroid)
- [r/Waydroid](https://reddit.com/r/Waydroid)

---

<div align="center">

**COSMIC Connect - Automated Testing with Waydroid**

*Fast • Automated • 85-90% Coverage • NixOS Native*

[Project Home](../../README.md) • [Testing Docs](../INDEX.md) • [Report Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)

**Last Updated**: 2026-01-17

</div>
