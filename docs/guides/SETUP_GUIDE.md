# COSMIC Connect Android - Development Setup Guide

This guide will walk you through setting up your NixOS development environment for COSMIC Connect Android development.

## Prerequisites

- NixOS with flakes enabled
- COSMIC Desktop installed
- Terminal access
- Internet connection

## Step 1: Enable Nix Flakes

If you haven't already enabled flakes, add this to your `/etc/nixos/configuration.nix`:

```nix
nix.settings.experimental-features = [ "nix-command" "flakes" ];
```

Then rebuild:
```bash
sudo nixos-rebuild switch
```

## Step 2: Add Waydroid Configuration

Copy the Waydroid configuration to your NixOS config:

```bash
# Option 1: Import as module (recommended)
sudo cp nixos-waydroid-config.nix /etc/nixos/waydroid.nix
```

Then add to your `/etc/nixos/configuration.nix`:

```nix
imports = [
  # ... your other imports
  ./waydroid.nix
];
```

**OR**

```bash
# Option 2: Merge directly into configuration.nix
# Copy the contents of nixos-waydroid-config.nix into your configuration.nix
```

Apply the configuration:
```bash
sudo nixos-rebuild switch
```

## Step 3: Enter Development Environment

```bash
# Allow direnv (optional but recommended)
direnv allow

# OR manually enter the dev shell
nix develop
```

You should see a colorful welcome message with all available commands!

## Step 4: Initialize Waydroid

```bash
# Initialize Waydroid (downloads Android image - ~1GB)
sudo waydroid init

# Start Waydroid container service
waydroid session start &

# Launch Waydroid UI (opens Android in a window)
waydroid show-full-ui
```

Wait for Android to boot (first boot takes ~2 minutes).

## Step 5: Verify ADB Connection

```bash
# Check if ADB sees Waydroid
adb devices
```

Expected output:
```
List of devices attached
192.168.250.2:5555    device
```

If not listed, manually connect:
```bash
adb connect 192.168.250.2:5555
```

## Step 6: Build the Android App

```bash
# Clean build
./gradlew clean assembleDebug

# This will create: app/build/outputs/apk/debug/app-debug.apk
```

## Step 7: Install App to Waydroid

```bash
# Install the app
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Check it's installed
adb shell pm list packages | grep cosmicconnect
```

Expected output:
```
package:org.cosmicext.connect
```

## Step 8: Launch the App

In Waydroid UI:
1. Look for "COSMIC Connect" app icon
2. Tap to launch
3. Grant required permissions

OR via command line:
```bash
adb shell monkey -p org.cosmicext.connect -c android.intent.category.LAUNCHER 1
```

## Step 9: Clone COSMIC Applet

```bash
# Clone in a separate directory
cd ~/Source/GitHub
git clone https://github.com/olafkfreund/cosmic-applet-cosmicconnect
cd cosmic-applet-cosmicconnect

# Build and run the COSMIC applet
cargo build --release
cargo run
```

## Step 10: Test Device Discovery

### On Android (Waydroid):
1. Open COSMIC Connect app
2. Go to Settings
3. Look for available devices

### On COSMIC Desktop:
1. Check COSMIC applet panel
2. Should show nearby Android devices
3. Click to pair

### Debug if not working:

```bash
# Check if ports are open
ss -tulnp | grep -E "171[4-6]"

# Check firewall rules
sudo nft list ruleset | grep -E "171[4-6]"

# Watch Android logs
adb logcat | grep -i cosmicconnect

# Send test UDP broadcast
echo -n "test" | nc -u -b 255.255.255.255 1716
```

## Troubleshooting

### Waydroid won't start

```bash
# Check status
waydroid status

# Check logs
journalctl -u waydroid-container.service -f

# Reset Waydroid
sudo waydroid upgrade
```

### ADB doesn't see Waydroid

```bash
# Restart ADB
adb kill-server
adb start-server

# Manually connect
adb connect 192.168.250.2:5555

# Check Waydroid IP
waydroid status
```

### Build fails

```bash
# Update Gradle
./gradlew wrapper --gradle-version=8.5

# Clear Gradle cache
rm -rf ~/.gradle/caches

# Clean build
./gradlew clean build --refresh-dependencies
```

### App crashes on launch

```bash
# View crash logs
adb logcat | grep -i "androidruntime\|exception"

# Clear app data
adb shell pm clear org.cosmicext.connect

# Reinstall
adb uninstall org.cosmicext.connect
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Discovery not working

```bash
# Check firewall on both sides
# On NixOS:
sudo nft list ruleset | grep 1716

# Test UDP broadcast
echo "test" | nc -u -b 255.255.255.255 1716

# Check if Android can reach host
adb shell ping -c 3 $(hostname -I | awk '{print $1}')
```

## Development Workflow

```bash
# 1. Make code changes
vim src/...

# 2. Build
./gradlew assembleDebug

# 3. Install to Waydroid
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Watch logs
adb logcat | grep -i cosmicconnect

# 5. Test with COSMIC applet
# Launch applet and verify communication
```

## Useful Commands Reference

### Gradle
```bash
./gradlew tasks                    # List all tasks
./gradlew assembleDebug            # Build debug APK
./gradlew assembleRelease          # Build release APK
./gradlew test                     # Run unit tests
./gradlew lint                     # Run linter
./gradlew clean                    # Clean build artifacts
```

### ADB
```bash
adb devices                        # List connected devices
adb install -r app.apk             # Install/reinstall APK
adb uninstall org.cosmicext.connect # Uninstall app
adb logcat                         # View logs
adb shell                          # Open shell in Android
adb shell pm list packages         # List installed packages
```

### Waydroid
```bash
waydroid status                    # Check status
waydroid session start             # Start container
waydroid session stop              # Stop container
waydroid show-full-ui              # Launch UI
waydroid app list                  # List installed apps
waydroid app launch <package>      # Launch specific app
```

## Next Steps

Once your development environment is working:

1. ✅ Mark Issue #1 as complete
2. 📋 Move to Issue #2: Android Codebase Audit
3. 🧪 Test protocol compatibility (Issue #4)

For detailed next steps, see:
- `GETTING_STARTED.md`
- `PROJECT_PLAN.md`
- `gh issue view 2`

## Success Criteria ✅

You've successfully set up your development environment when:

- ✅ `nix develop` enters the dev shell without errors
- ✅ `./gradlew assembleDebug` builds the APK successfully
- ✅ Waydroid launches and shows Android UI
- ✅ `adb devices` shows Waydroid device
- ✅ App installs and launches in Waydroid
- ✅ COSMIC applet builds and runs
- ✅ Android app and COSMIC applet can discover each other
- ✅ Firewall ports 1714-1716 are open

## Getting Help

If you encounter issues:

1. Check this guide's troubleshooting section
2. Review Issue #1: `gh issue view 1`
3. Check logs: `adb logcat` and `journalctl`
4. Ask Claude Code: `claude-code "Help with Waydroid setup issue"`

---

**You're all set! Happy developing! 🚀**
