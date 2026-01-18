#!/usr/bin/env bash
# Quick wireless ADB connection to Samsung Tab S8 Ultra

# Configuration - UPDATE THESE VALUES
TABLET_IP="192.168.1.100"  # UPDATE with your tablet's IP address
TABLET_PORT="45678"         # UPDATE with wireless debugging port

echo "🔌 Connecting to Samsung Tab S8 Ultra..."
echo "IP: $TABLET_IP"
echo "Port: $TABLET_PORT"
echo ""

# Try to connect
adb connect ${TABLET_IP}:${TABLET_PORT}

# Wait a moment
sleep 2

# Check connection
if adb devices | grep -q "${TABLET_IP}"; then
  echo ""
  echo "✅ Connected successfully!"
  echo ""
  echo "Connected devices:"
  adb devices
  echo ""

  # Get device info
  MODEL=$(adb -s ${TABLET_IP}:${TABLET_PORT} shell getprop ro.product.model | tr -d '\r')
  ANDROID=$(adb -s ${TABLET_IP}:${TABLET_PORT} shell getprop ro.build.version.release | tr -d '\r')
  echo "Device: $MODEL"
  echo "Android: $Android"
else
  echo ""
  echo "❌ Connection failed"
  echo ""
  echo "Troubleshooting:"
  echo "  1. Check tablet is on same Wi-Fi network as this machine"
  echo "  2. Check Wireless debugging is enabled on tablet:"
  echo "     Settings → Developer options → Wireless debugging (ON)"
  echo "  3. Verify IP and port are correct (check wireless debugging screen)"
  echo "  4. Try disconnecting first:"
  echo "     adb disconnect && adb connect ${TABLET_IP}:${TABLET_PORT}"
  echo "  5. If still failing, re-pair using:"
  echo "     adb pair <ip>:<pairing-port>  (use pairing code from tablet)"
  echo ""
  exit 1
fi
