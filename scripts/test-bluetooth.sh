#!/usr/bin/env bash
# Bluetooth testing on Samsung Tab S8 Ultra

set -euo pipefail

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

DEVICE_SERIAL="${1:-}"

if [ -z "$DEVICE_SERIAL" ]; then
  echo "Usage: $0 <device-serial>"
  echo ""
  echo "Examples:"
  echo "  $0 R5CT60XXXXXX                    # USB connected device"
  echo "  $0 192.168.1.100:45678             # Wireless connected device"
  echo ""
  echo "Get device serial:"
  echo "  adb devices"
  exit 1
fi

log() { echo -e "${BLUE}[INFO]${NC} $1"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║       Bluetooth Testing - Samsung Tab S8 Ultra            ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Device: $DEVICE_SERIAL"
echo ""

# Select specific device
export ANDROID_SERIAL="$DEVICE_SERIAL"

# Check device is connected
adb wait-for-device || error "Device not connected"

# Get device model
MODEL=$(adb shell getprop ro.product.model | tr -d '\r')
log "Connected to: $MODEL"

# Check Bluetooth is enabled
log "Checking Bluetooth status..."
BT_STATUS=$(adb shell settings get global bluetooth_on 2>/dev/null || echo "0")

if [ "$BT_STATUS" != "1" ]; then
  warning "Bluetooth is currently OFF"
  echo ""
  echo "To enable Bluetooth:"
  echo "  Option 1 (Manual): Settings → Connections → Bluetooth → ON"
  echo "  Option 2 (ADB):    adb shell svc bluetooth enable"
  echo ""
  read -p "Enable Bluetooth now via ADB? (y/N) " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    adb shell svc bluetooth enable
    sleep 2
    BT_STATUS=$(adb shell settings get global bluetooth_on)
    if [ "$BT_STATUS" = "1" ]; then
      success "Bluetooth enabled"
    else
      error "Failed to enable Bluetooth"
    fi
  else
    error "Bluetooth must be enabled for testing. Exiting."
  fi
else
  success "Bluetooth is ON"
fi

# Check Bluetooth permissions
log "Checking Bluetooth permissions..."
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN 2>/dev/null || true
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_CONNECT 2>/dev/null || true
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_ADVERTISE 2>/dev/null || true
success "Bluetooth permissions granted"

# Launch app
log "Launching COSMIC Connect..."
adb shell am start -n org.cosmicext.connect/.MainActivity
sleep 3
success "App launched"

# Clear logcat
adb logcat -c

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║                  Manual Bluetooth Tests                    ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo "Perform the following tests on the tablet:"
echo ""
echo "✅ Test 1: Bluetooth Device Discovery"
echo "   1. Ensure COSMIC Desktop has Bluetooth enabled"
echo "   2. In COSMIC Connect, verify desktop appears in device list"
echo "   3. Note: Device should show (Bluetooth) indicator"
echo ""
echo "✅ Test 2: Bluetooth Pairing"
echo "   1. Tap COSMIC Desktop device in list"
echo "   2. Tap 'Request Pairing'"
echo "   3. Accept pairing on COSMIC Desktop"
echo "   4. Verify 'Paired' status appears"
echo ""
echo "✅ Test 3: Bluetooth Connection"
echo "   1. Verify 'Connected' status appears after pairing"
echo "   2. Check connection type shows (Bluetooth)"
echo ""
echo "✅ Test 4: Bluetooth File Transfer"
echo "   1. Send a test file from tablet to desktop"
echo "   2. Monitor transfer progress"
echo "   3. Verify file received successfully on desktop"
echo "   4. Note transfer speed"
echo ""
echo "✅ Test 5: Bluetooth Connection Stability"
echo "   1. With devices paired and connected via Bluetooth"
echo "   2. Move tablet to different distances from desktop"
echo "   3. Walk around with tablet (add obstacles)"
echo "   4. Monitor for disconnections or errors"
echo ""
echo "✅ Test 6: Bluetooth/Wi-Fi Switching"
echo "   1. Start with Bluetooth connection"
echo "   2. Ensure both devices on same Wi-Fi network"
echo "   3. Verify automatic switch to Wi-Fi (faster)"
echo "   4. Disable Wi-Fi on tablet"
echo "   5. Verify fallback to Bluetooth"
echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""
echo "📊 Monitoring Bluetooth logs in real-time..."
echo "   (Logs filtered for: bluetooth, cosmic, pair, connect)"
echo ""
echo "Press Ctrl+C to stop monitoring"
echo ""
echo "═══════════════════════════════════════════════════════════"
echo ""

# Monitor Bluetooth logs
adb logcat | grep -i --line-buffered -E "bluetooth|cosmic|pair|connect|filetransfer" | while read line; do
  # Color code log levels
  if echo "$line" | grep -qi "error\|exception\|crash"; then
    echo -e "${RED}$line${NC}"
  elif echo "$line" | grep -qi "warning\|warn"; then
    echo -e "${YELLOW}$line${NC}"
  elif echo "$line" | grep -qi "pair\|connect"; then
    echo -e "${GREEN}$line${NC}"
  else
    echo "$line"
  fi
done
