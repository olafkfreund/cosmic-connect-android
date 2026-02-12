# NixOS Waydroid Configuration for COSMIC Connect Testing

> **Quick Reference**: NixOS configuration snippets for Waydroid testing setup

---

## System Configuration

Add to your `/etc/nixos/configuration.nix`:

```nix
{ config, pkgs, ... }:

{
  # Enable Waydroid for Android testing
  virtualisation.waydroid.enable = true;

  # Required kernel modules for Waydroid
  boot.kernelModules = [ "ashmem_linux" "binder_linux" ];

  # Network optimization for containers
  boot.kernel.sysctl = {
    "net.ipv4.ip_forward" = 1;
    "net.ipv4.conf.all.forwarding" = 1;
    "kernel.unprivileged_userns_clone" = 1;
  };

  # Firewall rules for COSMIC Connect testing
  networking.firewall = {
    enable = true;
    allowedUDPPorts = [ 1716 ];  # KDE Connect discovery
    allowedTCPPorts = [ 1716 ];  # KDE Connect data transfer
  };

  # Ensure user is in waydroid group
  users.users.YOUR_USERNAME = {
    isNormalUser = true;
    extraGroups = [
      "wheel"
      "networkmanager"
      "waydroid"  # Required for Waydroid access
    ];
  };

  # Optional: Better performance with zram
  zramSwap = {
    enable = true;
    memoryPercent = 50;
  };

  # Optional: Install Waydroid management tools
  environment.systemPackages = with pkgs; [
    waydroid
    android-tools  # adb, fastboot
  ];
}
```

**Apply changes:**
```bash
sudo nixos-rebuild switch
```

---

## Project Flake Configuration

Update `flake.nix` to include Waydroid in development shell:

```nix
{
  description = "COSMIC Connect Android - Development Environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    rust-overlay = {
      url = "github:oxalica/rust-overlay";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs = { self, nixpkgs, flake-utils, rust-overlay }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ rust-overlay.overlays.default ];
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # ... existing androidComposition and rustWithAndroidTargets ...

      in
      {
        devShells = {
          default = pkgs.mkShell {
            buildInputs = with pkgs; [
              # ... existing packages ...

              # Waydroid testing
              waydroid
              wl-clipboard      # Clipboard for Wayland
              cage              # Nested Wayland compositor (for X11)
            ];

            shellHook = ''
              # ... existing shellHook ...

              echo "📱 Waydroid Testing Commands:"
              echo "  • Initialize:       sudo waydroid init"
              echo "  • Start session:    waydroid session start"
              echo "  • Show UI:          waydroid show-full-ui"
              echo "  • Connect ADB:      adb connect 192.168.240.112:5555"
              echo "  • Install APK:      adb install -r build/outputs/apk/debug/*.apk"
              echo "  • Run tests:        ./scripts/test-waydroid.sh"
              echo "  • Headless tests:   ./scripts/test-waydroid.sh --headless"
              echo "  • Quick tests:      ./scripts/test-waydroid.sh --quick"
              echo "  • Stop session:     waydroid session stop"
              echo ""
            '';
          };
        };
      }
    );
}
```

---

## First-Time Setup

After applying the configuration:

```bash
# 1. Rebuild NixOS system
sudo nixos-rebuild switch

# 2. Enter development environment
cd /home/olafkfreund/Source/GitHub/cosmic-connect-android
nix develop

# 3. Initialize Waydroid (first time only)
sudo waydroid init

# Or with Google Apps (for Play Store)
sudo waydroid init -s GAPPS

# 4. Verify installation
waydroid status
```

---

## Quick Reference Commands

### Waydroid Management

```bash
# Start Waydroid session
waydroid session start

# Show Android UI
waydroid show-full-ui

# Check status
waydroid status

# Stop session
waydroid session stop

# Reinstall/reset (destructive!)
sudo waydroid init -f
```

### ADB Connection

```bash
# Connect to Waydroid
adb connect 192.168.240.112:5555

# Check connection
adb devices

# Disconnect
adb disconnect 192.168.240.112:5555

# Kill and restart ADB server
adb kill-server
adb start-server
```

### App Management

```bash
# Install APK
adb install -r app.apk

# List installed apps
adb shell pm list packages | grep cosmic

# Uninstall app
adb uninstall org.cosmicext.connect

# Clear app data
adb shell pm clear org.cosmicext.connect

# Launch app
adb shell am start -n org.cosmicext.connect/.MainActivity
```

### Testing

```bash
# Automated test script
./scripts/test-waydroid.sh

# Headless (CI/CD)
./scripts/test-waydroid.sh --headless

# Quick (skip clean build)
./scripts/test-waydroid.sh --quick

# Manual Gradle tests
./gradlew connectedAndroidTest
```

---

## Troubleshooting

### Waydroid Won't Start

```bash
# Check kernel modules
lsmod | grep -E "binder|ashmem"

# Load modules manually
sudo modprobe binder_linux
sudo modprobe ashmem_linux

# Check logs
sudo journalctl -u waydroid-container -f
```

### ADB Can't Connect

```bash
# Get Waydroid IP
waydroid shell getprop persist.waydroid.host_ip

# Restart ADB
adb kill-server
adb start-server
adb connect 192.168.240.112:5555
```

### Performance Issues

```bash
# Disable animations
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Check resource usage
htop  # Look for waydroid processes
```

---

## Environment Variables

Useful environment variables for scripts:

```bash
# Waydroid configuration
export WAYDROID_IP="192.168.240.112"
export WAYDROID_PORT="5555"

# Android SDK (already set by flake)
export ANDROID_HOME="/nix/store/.../android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk-bundle"

# ADB device
export ANDROID_SERIAL="${WAYDROID_IP}:${WAYDROID_PORT}"
```

---

## Resources

- [NixOS Waydroid Wiki](https://wiki.nixos.org/wiki/Waydroid)
- [Waydroid Documentation](https://docs.waydro.id/)
- [Testing Guide](WAYDROID_TESTING_GUIDE.md)
- [Project README](../../README.md)
