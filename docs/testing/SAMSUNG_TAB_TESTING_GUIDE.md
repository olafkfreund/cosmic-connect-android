# Samsung Galaxy Tab S8 Ultra Testing Guide for COSMIC Connect

> **Device**: Samsung Galaxy Tab S8 Ultra (SM-X900)
> **Android Version**: Android 14
> **Last Updated**: 2026-01-17
> **Purpose**: Complete automation for real device testing, especially Bluetooth

<div align="center">

**Complete guide for automated testing on Samsung Galaxy Tab S8 Ultra**

[Quick Start](#quick-start) • [Bluetooth Testing](#bluetooth-testing) • [Automation](#automation) • [Wireless ADB](#wireless-adb) • [Troubleshooting](#troubleshooting)

</div>

---

## Table of Contents

1. [Overview](#overview)
2. [Device Specifications](#device-specifications)
3. [Initial Setup](#initial-setup)
4. [USB Connection Setup](#usb-connection-setup)
5. [Wireless ADB Setup](#wireless-adb-setup)
6. [Quick Start](#quick-start)
7. [Bluetooth Testing](#bluetooth-testing)
8. [Test Automation](#test-automation)
9. [Unified Testing Workflow](#unified-testing-workflow)
10. [Advanced Usage](#advanced-usage)
11. [Troubleshooting](#troubleshooting)

---

## Overview

The Samsung Galaxy Tab S8 Ultra (SM-X900) is your **primary device for Bluetooth testing** and **final E2E validation** before releases. It provides:

- ✅ **Real Bluetooth hardware** - Test actual Bluetooth pairing/connectivity
- ✅ **Real network conditions** - Wi-Fi signal strength, network switching
- ✅ **Hardware validation** - Samsung-specific issues, manufacturer customizations
- ✅ **User experience** - Real-world performance and usability
- ✅ **Wireless ADB** - No USB cable needed for most testing

### Why This Device?

**Complements Waydroid:**
- Waydroid: 90% of automated testing (fast, CI/CD friendly)
- **Tab S8 Ultra**: 10% critical validation (Bluetooth, real hardware)

**Key Advantages:**
- Large 14.6" display - Easy to debug UI issues
- Android 14 - Latest Android features and APIs
- Powerful hardware - Snapdragon 8 Gen 1
- Wireless debugging - Flexible testing without cables

### Research Sources

This guide is based on official Samsung and Android documentation:

- [Samsung Galaxy Tab S8 Ultra Specifications](https://www.gsmarena.com/samsung_galaxy_tab_s8-12439.php)
- [Samsung Galaxy Tab S8 Ultra Support](https://www.samsung.com/us/business/support/owners/product/galaxy-tab-s8-ultra/)
- [Android Run Apps on Hardware Device](https://developer.android.com/studio/run/device)
- [Android Instrumented Tests](https://developer.android.com/training/testing/instrumented-tests)
- [Espresso Testing Guide](https://www.getautonoma.com/blog/how-to-test-android-apps-with-espresso)
- [Samsung USB Debugging Setup](https://www.hardreset.info/devices/samsung/samsung-galaxy-tab-s8/faq/faq/usb-debugging-samsung-tablet/)

---

## Device Specifications

### Samsung Galaxy Tab S8 Ultra (SM-X900)

**Display:**
- 14.6" Super AMOLED, 120Hz
- 2960 x 1848 pixels

**Performance:**
- Qualcomm Snapdragon 8 Gen 1
- 8GB/12GB/16GB RAM
- 128GB/256GB/512GB storage

**Connectivity:**
- Wi-Fi 6E (802.11ax)
- **Bluetooth 5.2**
- USB Type-C 3.2

**Battery:**
- 11,200 mAh
- 45W fast charging

**OS:**
- Android 12 (launch)
- Android 14 (current via update)
- One UI 6.0

**Perfect for Testing:**
- ✅ Latest Android version (14)
- ✅ Modern Bluetooth 5.2
- ✅ Fast Wi-Fi 6E
- ✅ Large screen for debugging
- ✅ High performance for smooth testing

---

## Initial Setup

### Step 1: Enable Developer Options

1. **Open Settings**
2. **Navigate to**: About tablet → Software information
3. **Tap "Build number" 7 times**
4. **Enter PIN/password** when prompted
5. **Confirmation**: "Developer mode has been enabled"

**Reference**: [Samsung Developer Options Guide](https://www.hardreset.info/devices/samsung/samsung-galaxy-tab-s9-ultra-wi-fi/developer-options/)

### Step 2: Enable USB Debugging

1. **Go back to Settings**
2. **Navigate to**: Developer options
3. **Enable**: USB debugging
4. **Confirm**: "Allow USB debugging?"

**Important Settings in Developer Options:**

```
Developer Options
├─ USB debugging ............................ ✅ ON
├─ Wireless debugging ....................... ✅ ON (for wireless ADB)
├─ Install via USB .......................... ✅ ON
├─ USB debugging (Security settings) ........ ✅ ON (optional)
├─ Stay awake ............................... ✅ ON (convenient)
└─ Select USB configuration ................. MTP or File Transfer
```

### Step 3: Optimize for Testing

**Disable Battery Optimization:**
1. Settings → Apps
2. Tap ⋮ (three dots) → Special access
3. Optimize battery usage
4. Tap dropdown → All
5. Find "COSMIC Connect"
6. Toggle OFF

**Disable Animations** (optional, faster tests):
```
Developer Options
├─ Window animation scale ................... 0.5x or OFF
├─ Transition animation scale ............... 0.5x or OFF
└─ Animator duration scale .................. 0.5x or OFF
```

---

## USB Connection Setup

### Prerequisites

**On Your NixOS System:**
- Android SDK platform-tools (includes `adb`)
- USB cable (USB-C to USB-C or USB-A to USB-C)
- Your development environment active

**Verify ADB:**
```bash
# Check adb is installed
which adb

# Check adb version
adb version
```

### Step 1: Physical Connection

1. **Connect tablet** to NixOS machine via USB cable
2. **Unlock tablet** if locked
3. **Notification should appear**: "USB for file transfer"

### Step 2: Authorize Computer

1. **Tablet will prompt**: "Allow USB debugging?"
   - Fingerprint: RSA key fingerprint
   - Checkbox: "Always allow from this computer"
2. **Tap "Allow"**

### Step 3: Verify Connection

```bash
# List connected devices
adb devices

# Expected output:
# List of devices attached
# R5CT60XXXXXX    device
```

**Device States:**
- `device` - ✅ Connected and authorized
- `unauthorized` - ❌ Need to authorize on tablet
- `offline` - ❌ Connection issue, restart ADB

### Step 4: Test Connection

```bash
# Get device info
adb shell getprop ro.product.model
# Output: SM-X900

# Get Android version
adb shell getprop ro.build.version.release
# Output: 14

# Get device IP (for wireless setup later)
adb shell ip addr show wlan0 | grep 'inet '
```

---

## Wireless ADB Setup

### Why Wireless ADB?

**Advantages:**
- ✅ **No USB cable** needed for daily testing
- ✅ **Flexible placement** of tablet
- ✅ **Easier automation** (no cable management)
- ✅ **Multiple connections** (can connect from different machines)

**When to Use:**
- Daily development testing
- Automated test runs
- Long-running tests

**When to Use USB:**
- First-time setup
- Wireless ADB troubleshooting
- Fastest data transfer (APK installation)

### Method 1: ADB Pair (Android 11+, Recommended)

**Prerequisites:**
- Tablet and NixOS on same Wi-Fi network
- Tablet has USB debugging enabled
- Wireless debugging enabled in Developer options

**Step 1: Enable Wireless Debugging on Tablet**

1. **Settings → Developer options**
2. **Enable "Wireless debugging"**
3. **Tap "Wireless debugging"** to enter settings
4. **Note the IP address and port** (e.g., `192.168.1.100:45678`)

**Step 2: Pair with Pairing Code**

On tablet:
1. In "Wireless debugging" screen
2. Tap "Pair device with pairing code"
3. **Note the pairing code** (6 digits)
4. **Note the IP and port** (e.g., `192.168.1.100:12345`)

On NixOS:
```bash
# Pair using code (one-time only per machine)
adb pair <IP>:<PORT>
# Example: adb pair 192.168.1.100:12345

# Enter pairing code when prompted
# Expected: Successfully paired to 192.168.1.100:12345...
```

**Step 3: Connect Wirelessly**

```bash
# Connect to wireless debugging port (shown in wireless debugging screen)
adb connect <IP>:<PORT>
# Example: adb connect 192.168.1.100:45678

# Verify connection
adb devices

# Expected:
# List of devices attached
# 192.168.1.100:45678    device
```

### Method 2: USB to Wireless (Fallback)

If pairing method doesn't work:

```bash
# Connect via USB first
adb devices  # Verify USB connection

# Enable TCP/IP mode on port 5555
adb tcpip 5555

# Get tablet IP
TABLET_IP=$(adb shell ip addr show wlan0 | grep 'inet ' | awk '{print $2}' | cut -d/ -f1)
echo "Tablet IP: $TABLET_IP"

# Disconnect USB cable

# Connect wirelessly
adb connect $TABLET_IP:5555

# Verify
adb devices
```

### Persistent Wireless Connection

**Create helper script** `scripts/connect-tablet.sh`:

```bash
#!/usr/bin/env bash
# Quick wireless ADB connection to Samsung Tab S8 Ultra

TABLET_IP="192.168.1.100"  # UPDATE with your tablet's IP
TABLET_PORT="45678"         # UPDATE with wireless debugging port

echo "Connecting to Samsung Tab S8 Ultra..."
adb connect ${TABLET_IP}:${TABLET_PORT}

if adb devices | grep -q "${TABLET_IP}"; then
  echo "✅ Connected successfully"
  adb devices
else
  echo "❌ Connection failed"
  echo "Troubleshooting:"
  echo "  1. Check tablet is on same Wi-Fi network"
  echo "  2. Check Wireless debugging is enabled"
  echo "  3. Try: adb disconnect && adb connect ${TABLET_IP}:${TABLET_PORT}"
fi
```

**Make executable:**
```bash
chmod +x scripts/connect-tablet.sh
```

**Usage:**
```bash
# Quick connect
./scripts/connect-tablet.sh
```

---

## Quick Start

### 1. Connect Device

**Option A: USB**
```bash
# Connect USB cable
# Authorize on tablet
adb devices
```

**Option B: Wireless (after setup)**
```bash
./scripts/connect-tablet.sh
```

### 2. Build and Install COSMIC Connect

```bash
cd /home/olafkfreund/Source/GitHub/cosmic-connect-android

# Build debug APK
./gradlew assembleDebug

# Install to tablet
adb install -r build/outputs/apk/debug/cosmicconnect-android-debug-*.apk
```

### 3. Grant Permissions

```bash
# Grant all required permissions
adb shell pm grant org.cosmicext.connect android.permission.ACCESS_FINE_LOCATION
adb shell pm grant org.cosmicext.connect android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant org.cosmicext.connect android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant org.cosmicext.connect android.permission.WRITE_EXTERNAL_STORAGE
adb shell pm grant org.cosmicext.connect android.permission.POST_NOTIFICATIONS
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_CONNECT
```

### 4. Launch App

```bash
# Launch COSMIC Connect
adb shell am start -n org.cosmicext.connect/.MainActivity

# Or use monkey to test
adb shell monkey -p org.cosmicext.connect -c android.intent.category.LAUNCHER 1
```

### 5. Run Tests

```bash
# Run all instrumented tests on tablet
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cconnect.integration.DiscoveryPairingTest
```

---

## Bluetooth Testing

### Why Bluetooth Testing is Critical

**COSMIC Connect Bluetooth Features:**
- Bluetooth device discovery
- Bluetooth pairing as backup to Wi-Fi
- Bluetooth data transfer
- Connection persistence

**Waydroid Cannot Test:**
- ❌ No Bluetooth stack in container
- ❌ Cannot emulate Bluetooth hardware
- ❌ No real-world Bluetooth conditions

**Samsung Tab S8 Ultra Provides:**
- ✅ Real Bluetooth 5.2 hardware
- ✅ Actual pairing workflow
- ✅ Real signal strength/range testing
- ✅ Connection stability validation

### Bluetooth Testing Scenarios

#### Test 1: Bluetooth Device Discovery

**Objective**: Verify COSMIC Connect can discover COSMIC Desktop via Bluetooth

**Manual Test:**
1. Open COSMIC Connect on tablet
2. Enable Bluetooth on tablet
3. Enable Bluetooth on COSMIC Desktop
4. Wait for device discovery
5. Verify desktop appears in device list

**Verification:**
```bash
# Check Bluetooth status
adb shell dumpsys bluetooth_manager | grep "enabled"

# List paired devices
adb shell dumpsys bluetooth_manager | grep "Paired"
```

#### Test 2: Bluetooth Pairing

**Objective**: Successfully pair tablet with desktop via Bluetooth

**Manual Test:**
1. Tap desktop device in COSMIC Connect
2. Tap "Request Pairing"
3. Accept pairing on desktop
4. Verify successful pairing

**Verification:**
```bash
# Check pairing status
adb shell dumpsys bluetooth_manager | grep "bonded"

# View logcat for pairing events
adb logcat -d | grep -i "bluetooth.*pair"
```

#### Test 3: Bluetooth Data Transfer

**Objective**: Verify file transfer works over Bluetooth connection

**Manual Test:**
1. Pair devices via Bluetooth
2. Send file from tablet to desktop
3. Verify transfer completes
4. Check file integrity

**Verification:**
```bash
# Monitor file transfer
adb logcat -d | grep -i "filetransfer"

# Check transfer speed
adb logcat -d | grep -i "transfer.*speed"
```

#### Test 4: Bluetooth Connection Stability

**Objective**: Verify Bluetooth connection remains stable

**Manual Test:**
1. Pair devices via Bluetooth
2. Move tablet around (different distances/obstacles)
3. Monitor connection status
4. Verify no disconnections or errors

**Verification:**
```bash
# Monitor connection events
adb logcat -c  # Clear logcat
# Perform movement tests
adb logcat -d | grep -i "bluetooth.*disconnect"
```

#### Test 5: Bluetooth to Wi-Fi Fallback

**Objective**: Verify connection can switch between Bluetooth and Wi-Fi

**Manual Test:**
1. Pair via Bluetooth
2. Enable Wi-Fi on both devices
3. Verify automatic switch to Wi-Fi (faster)
4. Disable Wi-Fi
5. Verify fallback to Bluetooth

**Verification:**
```bash
# Monitor connection type changes
adb logcat -d | grep -i "connection.*type"
```

### Automated Bluetooth Testing Script

Create `scripts/test-bluetooth.sh`:

```bash
#!/usr/bin/env bash
# Bluetooth testing on Samsung Tab S8 Ultra

set -euo pipefail

DEVICE_SERIAL="${1:-}"

if [ -z "$DEVICE_SERIAL" ]; then
  echo "Usage: $0 <device-serial>"
  echo "Example: $0 192.168.1.100:45678"
  exit 1
fi

echo "🔵 Bluetooth Testing on Samsung Tab S8 Ultra"
echo "Device: $DEVICE_SERIAL"
echo ""

# Select specific device
export ANDROID_SERIAL="$DEVICE_SERIAL"

# Check Bluetooth is enabled
echo "Checking Bluetooth status..."
BT_STATUS=$(adb shell settings get global bluetooth_on)
if [ "$BT_STATUS" != "1" ]; then
  echo "⚠️  Bluetooth is OFF. Please enable Bluetooth on tablet."
  echo "Settings → Connections → Bluetooth"
  exit 1
fi
echo "✅ Bluetooth is ON"

# Check Bluetooth permissions
echo "Checking Bluetooth permissions..."
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN 2>/dev/null || true
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_CONNECT 2>/dev/null || true
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_ADVERTISE 2>/dev/null || true
echo "✅ Bluetooth permissions granted"

# Launch app
echo "Launching COSMIC Connect..."
adb shell am start -n org.cosmicext.connect/.MainActivity
sleep 3

# Clear logcat
adb logcat -c

echo ""
echo "📋 Manual Bluetooth Tests:"
echo "  1. Verify COSMIC Desktop appears in device list (Bluetooth)"
echo "  2. Tap device → Request Pairing"
echo "  3. Accept pairing on desktop"
echo "  4. Send test file via Bluetooth"
echo "  5. Move tablet around to test connection stability"
echo ""
echo "📊 Monitoring Bluetooth logs..."
echo "Press Ctrl+C to stop monitoring"
echo ""

# Monitor Bluetooth logs
adb logcat | grep -i --line-buffered -E "bluetooth|cosmic|pair"
```

**Make executable:**
```bash
chmod +x scripts/test-bluetooth.sh
```

**Usage:**
```bash
# USB connected
./scripts/test-bluetooth.sh $(adb devices | grep -v "List" | awk '{print $1}' | head -1)

# Wireless connected
./scripts/test-bluetooth.sh 192.168.1.100:45678
```

---

## Test Automation

### Automated Test Script for Samsung Tablet

Create `scripts/test-samsung.sh`:

```bash
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
      exit 0
      ;;
  esac
done

# Logging functions
log() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Connect to device
connect_device() {
  log "Connecting to Samsung Tab S8 Ultra..."

  if [ "$WIRELESS" = true ]; then
    log "Using wireless ADB connection..."
    adb connect ${TABLET_IP}:${TABLET_PORT} || error "Failed to connect wirelessly"
  fi

  # Wait for device
  adb wait-for-device

  # Get device info
  DEVICE=$(adb devices | grep -v "List" | head -1 | awk '{print $1}')
  MODEL=$(adb shell getprop ro.product.model | tr -d '\r')

  if [ "$MODEL" != "SM-X900" ]; then
    warning "Connected device is $MODEL (expected SM-X900)"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
    fi
  fi

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

  adb uninstall org.cosmicext.connect &> /dev/null || true
  adb install -r "$APK_PATH" || error "Failed to install APK"

  success "APK installed successfully"
}

# Grant permissions
grant_permissions() {
  log "Granting runtime permissions..."

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
    adb shell pm grant org.cosmicext.connect "$perm" 2>/dev/null || true
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
  log "Running instrumented tests on Samsung tablet..."
  cd "$PROJECT_ROOT"

  ./gradlew connectedAndroidTest --console=plain || {
    warning "Some instrumented tests failed. Check reports in build/reports/"
    return 1
  }

  success "Instrumented tests passed"
}

# Generate report
generate_report() {
  log "Generating test report summary..."

  REPORT_DIR="${BUILD_DIR}/reports/androidTests/connected"

  if [ -d "$REPORT_DIR" ]; then
    log "Test reports available at: file://${REPORT_DIR}/index.html"
  fi
}

# Main execution
main() {
  echo ""
  echo "╔════════════════════════════════════════════════════════════╗"
  echo "║   COSMIC Connect - Samsung Tab S8 Ultra Testing           ║"
  echo "╚════════════════════════════════════════════════════════════╝"
  echo ""

  connect_device
  build_apk
  install_apk
  grant_permissions
  run_unit_tests
  run_instrumented_tests || true
  generate_report

  echo ""
  success "✅ Samsung tablet testing complete!"
  echo ""
}

main "$@"
```

**Make executable:**
```bash
chmod +x scripts/test-samsung.sh
```

**Usage:**
```bash
# USB connection
./scripts/test-samsung.sh

# Wireless connection
./scripts/test-samsung.sh --wireless

# Quick mode (skip clean build)
./scripts/test-samsung.sh --wireless --quick
```

---

## Unified Testing Workflow

### Combined Waydroid + Samsung Testing

Create `scripts/test-all.sh`:

```bash
#!/usr/bin/env bash
#
# Unified testing: Waydroid + Samsung Tab S8 Ultra
# Usage: ./scripts/test-all.sh
#

set -euo pipefail

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       COSMIC Connect - Complete Test Suite                ║"
echo "║       Waydroid (90%) + Samsung Tablet (10%)               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Phase 1: Waydroid Testing (Primary)
log "Phase 1: Running Waydroid tests (network, integration, E2E)..."
./scripts/test-waydroid.sh --headless || {
  error "Waydroid tests failed. Fix issues before continuing."
}
success "✅ Waydroid testing complete"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""

# Phase 2: Samsung Tablet Testing (Bluetooth + Validation)
log "Phase 2: Running Samsung tablet tests (Bluetooth, hardware validation)..."
./scripts/test-samsung.sh --wireless || {
  error "Samsung tablet tests failed."
}
success "✅ Samsung tablet testing complete"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""

# Phase 3: Bluetooth-Specific Tests
log "Phase 3: Running Bluetooth-specific tests..."
DEVICE_SERIAL=$(adb devices | grep "192.168.1" | awk '{print $1}')
if [ -n "$DEVICE_SERIAL" ]; then
  ./scripts/test-bluetooth.sh "$DEVICE_SERIAL" &
  BT_PID=$!

  echo "Bluetooth test monitoring started (PID: $BT_PID)"
  echo "Perform manual Bluetooth tests now..."
  echo "Press Enter when Bluetooth tests are complete..."
  read

  kill $BT_PID 2>/dev/null || true
  success "✅ Bluetooth testing complete"
else
  warning "Samsung tablet not connected wirelessly. Skipping Bluetooth tests."
fi

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              ✅ Complete Test Suite Finished               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "📊 Test Reports:"
echo "  • Waydroid: build/reports/androidTests/connected/"
echo "  • Samsung:  build/reports/androidTests/connected/"
echo ""
echo "✅ All automated tests complete!"
echo "✅ Bluetooth testing validated!"
echo ""
```

**Make executable:**
```bash
chmod +x scripts/test-all.sh
```

**Usage:**
```bash
# Run complete test suite (Waydroid + Samsung)
./scripts/test-all.sh
```

---

## Advanced Usage

### Parallel Testing (Waydroid + Samsung)

Test on both devices simultaneously:

```bash
#!/usr/bin/env bash
# Parallel testing

# Start Waydroid tests in background
./scripts/test-waydroid.sh --headless &
WAYDROID_PID=$!

# Start Samsung tests in background
./scripts/test-samsung.sh --wireless &
SAMSUNG_PID=$!

# Wait for both to complete
echo "Waiting for tests to complete..."
wait $WAYDROID_PID
WAYDROID_RESULT=$?

wait $SAMSUNG_PID
SAMSUNG_RESULT=$?

# Report results
echo ""
if [ $WAYDROID_RESULT -eq 0 ] && [ $SAMSUNG_RESULT -eq 0 ]; then
  echo "✅ All tests passed!"
else
  echo "❌ Some tests failed"
  [ $WAYDROID_RESULT -ne 0 ] && echo "  - Waydroid tests failed"
  [ $SAMSUNG_RESULT -ne 0 ] && echo "  - Samsung tests failed"
fi
```

### Screen Recording

Record test execution on Samsung tablet:

```bash
# Start recording
adb shell screenrecord /sdcard/test-recording.mp4 &
RECORD_PID=$!

# Run tests
./scripts/test-samsung.sh --wireless --quick

# Stop recording (max 3 minutes automatically)
kill $RECORD_PID 2>/dev/null || true

# Pull recording
adb pull /sdcard/test-recording.mp4 ./test-results/
adb shell rm /sdcard/test-recording.mp4

echo "Recording saved: ./test-results/test-recording.mp4"
```

### Performance Profiling

Profile app performance on real hardware:

```bash
# Enable CPU profiling
adb shell am start -n org.cosmicext.connect/.MainActivity \
  --start-profiler cpu.trace

# Run app interactions
sleep 60  # Or perform specific tests

# Stop profiling and pull results
adb shell am profile stop org.cosmicext.connect
adb pull /data/local/tmp/cpu.trace ./
```

---

## Troubleshooting

### Issue: Device Not Detected

**Symptoms:**
```bash
adb devices
# List of devices attached
# (empty)
```

**Solutions:**

1. **Check USB cable**:
   - Try different USB cable
   - Try different USB port
   - Check cable supports data transfer (not charging-only)

2. **Check tablet authorization**:
   - Unlock tablet
   - Look for "Allow USB debugging?" prompt
   - Tap "Always allow from this computer"
   - Tap "Allow"

3. **Restart ADB server**:
   ```bash
   adb kill-server
   adb start-server
   adb devices
   ```

4. **Check USB debugging enabled**:
   - Settings → Developer options → USB debugging (ON)

### Issue: Wireless Connection Fails

**Symptoms:**
```bash
adb connect 192.168.1.100:45678
# failed to connect to 192.168.1.100:45678
```

**Solutions:**

1. **Check same Wi-Fi network**:
   ```bash
   # Check NixOS IP
   ip addr show

   # Check tablet IP
   adb shell ip addr show wlan0
   ```

2. **Check wireless debugging enabled**:
   - Settings → Developer options → Wireless debugging (ON)

3. **Re-pair device**:
   - Settings → Developer options → Wireless debugging
   - Tap "Pair device with pairing code"
   - Use `adb pair` with new code

4. **Use USB to wireless method**:
   ```bash
   # Connect USB
   adb tcpip 5555
   # Disconnect USB
   adb connect <tablet-ip>:5555
   ```

### Issue: App Installation Fails

**Symptoms:**
```bash
adb install app.apk
# Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]
```

**Solutions:**

1. **Uninstall first**:
   ```bash
   adb uninstall org.cosmicext.connect
   adb install app.apk
   ```

2. **Check storage space**:
   ```bash
   adb shell df /data
   ```

3. **Use `-r` flag** (reinstall):
   ```bash
   adb install -r app.apk
   ```

### Issue: Tests Timing Out

**Symptoms:**
```
Test failed to run to completion. Reason: 'Instrumentation run failed due to 'Process crashed.''
```

**Solutions:**

1. **Increase timeout**:
   ```kotlin
   // In build.gradle.kts
   android {
       testOptions {
           animationsDisabled = true
           unitTests {
               all {
                   testLogging {
                       events = setOf(PASSED, SKIPPED, FAILED)
                   }
                   timeout.set(Duration.ofMinutes(10))
               }
           }
       }
   }
   ```

2. **Disable animations**:
   ```bash
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```

3. **Check logcat**:
   ```bash
   adb logcat -d > crash.log
   grep -i "crash\|exception\|error" crash.log
   ```

### Issue: Bluetooth Not Working

**Symptoms:**
- Bluetooth device discovery fails
- Cannot pair via Bluetooth

**Solutions:**

1. **Check Bluetooth enabled**:
   ```bash
   adb shell settings get global bluetooth_on
   # Should return: 1
   ```

2. **Enable Bluetooth**:
   ```bash
   adb shell svc bluetooth enable
   ```

3. **Check permissions**:
   ```bash
   adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN
   adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_CONNECT
   adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_ADVERTISE
   ```

4. **Reset Bluetooth**:
   - Settings → Connections → Bluetooth
   - Toggle OFF then ON
   - Or: `adb shell svc bluetooth disable && adb shell svc bluetooth enable`

---

## Summary

### Samsung Tab S8 Ultra Testing Capabilities

**✅ What It Provides:**
- Real Bluetooth 5.2 hardware testing
- Hardware validation (Samsung-specific)
- Real network conditions (Wi-Fi 6E)
- User experience validation
- Large screen for debugging
- Android 14 latest features

**🎯 Primary Use Cases:**
1. **Bluetooth testing** (weekly validation)
2. **Pre-release E2E** (comprehensive validation)
3. **Hardware-specific issues** (Samsung customizations)
4. **Performance validation** (real-world conditions)

**📊 Testing Strategy:**
- **Waydroid**: 90% of automated testing (daily)
- **Samsung Tablet**: 10% critical validation (weekly + pre-release)

### Quick Reference Commands

```bash
# Connect USB
adb devices

# Connect wireless
adb connect 192.168.1.100:45678

# Install app
adb install -r app.apk

# Run tests
./scripts/test-samsung.sh --wireless

# Bluetooth tests
./scripts/test-bluetooth.sh <device-serial>

# Complete suite
./scripts/test-all.sh
```

---

## Resources

- [Samsung Galaxy Tab S8 Ultra Support](https://www.samsung.com/us/business/support/owners/product/galaxy-tab-s8-ultra/)
- [Android ADB Documentation](https://developer.android.com/studio/command-line/adb)
- [Wireless Debugging Guide](https://developer.android.com/studio/run/device#wireless)
- [Instrumented Tests](https://developer.android.com/training/testing/instrumented-tests)
- [Waydroid Testing Guide](WAYDROID_TESTING_GUIDE.md)
- [Project README](../../README.md)

---

<div align="center">

**COSMIC Connect - Samsung Tab S8 Ultra Testing**

*Real Hardware • Bluetooth Validated • Complete E2E Coverage*

[Documentation Home](../../README.md) • [Waydroid Guide](WAYDROID_TESTING_GUIDE.md) • [Report Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)

**Last Updated**: 2026-01-17

</div>
