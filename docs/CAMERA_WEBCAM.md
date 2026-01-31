# Camera Webcam Streaming - User Guide

**Use your Android phone's camera as a wireless webcam on your COSMIC Desktop!**

**Version:** 1.0.0
**Last Updated:** January 31, 2026
**Issue:** #111 (Part of Camera Feature #99)

---

## Table of Contents

- [Overview](#overview)
- [How It Works](#how-it-works)
- [Requirements](#requirements)
- [Desktop Setup](#desktop-setup)
  - [NixOS Setup](#nixos-setup)
  - [Arch Linux Setup](#arch-linux-setup)
  - [Fedora Setup](#fedora-setup)
  - [Ubuntu / Debian Setup](#ubuntu--debian-setup)
  - [openSUSE Setup](#opensuse-setup)
- [Using the Camera](#using-the-camera)
- [Selecting the Camera in Apps](#selecting-the-camera-in-apps)
  - [Firefox](#firefox)
  - [Chrome / Chromium](#chrome--chromium)
  - [OBS Studio](#obs-studio)
  - [Zoom](#zoom)
  - [Microsoft Teams](#microsoft-teams)
  - [Google Meet](#google-meet)
  - [Discord](#discord)
- [Configuration Options](#configuration-options)
- [Performance Tips](#performance-tips)
- [Security Considerations](#security-considerations)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Camera Webcam feature transforms your Android phone into a wireless webcam for your COSMIC Desktop. This is perfect for:

- **Video Calls**: Use your phone's excellent camera for Zoom, Teams, or Google Meet
- **Streaming**: Add a high-quality mobile camera source in OBS Studio
- **Content Creation**: Get flexible camera angles without buying a dedicated webcam
- **Remote Work**: Use your phone when your laptop webcam quality is poor

### Key Benefits

- **Higher Quality**: Modern phone cameras far exceed most built-in laptop webcams
- **Flexibility**: Position your camera anywhere within WiFi range
- **Wireless**: No cables required - works over your local network
- **Front or Back Camera**: Choose which camera to use on your phone
- **Cost Effective**: No need to purchase a separate webcam

---

## How It Works

```
+------------------+        WiFi/Network         +--------------------+
|  Android Phone   | ------------------------>  |   COSMIC Desktop   |
|                  |     H.264 Video Stream     |                    |
|  [Camera App]    |                            |  [v4l2loopback]    |
|       |          |                            |        |           |
|   Camera2 API    |                            |  Virtual Webcam    |
|       |          |                            |   /dev/video*      |
|   H.264 Encoder  |                            |        |           |
|       |          |                            |   Applications     |
|  Network Stream  | -------------------------> | (Firefox, OBS, etc)|
+------------------+                            +--------------------+
```

1. **Android** captures video from your phone's camera using the Camera2 API
2. **H.264 Encoding** compresses the video stream efficiently
3. **Network Transfer** sends the stream to your desktop via your local network
4. **v4l2loopback** creates a virtual webcam device on Linux
5. **Applications** see this as a normal webcam and can use it directly

---

## Requirements

### Android Device
- **Android Version**: 6.0 (Marshmallow) or later
- **Camera**: Any phone with a working camera
- **COSMIC Connect App**: Latest version installed
- **Permissions**: Camera and network access granted

### COSMIC Desktop
- **Linux Kernel**: 4.0 or later (v4l2loopback support)
- **v4l2loopback Module**: Kernel module for virtual webcam (see setup below)
- **COSMIC Connect**: Desktop app installed and paired with phone
- **Same Network**: Both devices connected to the same WiFi network

---

## Desktop Setup

The **v4l2loopback** kernel module creates a virtual webcam device that applications can use. Here is how to set it up on various Linux distributions.

### NixOS Setup

NixOS provides declarative configuration for v4l2loopback.

#### Option 1: System Configuration (Recommended)

Add to your `/etc/nixos/configuration.nix`:

```nix
{ config, pkgs, ... }:

{
  # Load v4l2loopback kernel module
  boot.extraModulePackages = [ config.boot.kernelPackages.v4l2loopback ];
  boot.kernelModules = [ "v4l2loopback" ];

  # Configure the virtual webcam device
  boot.extraModprobeConfig = ''
    options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
  '';

  # Optional: Add v4l-utils for debugging
  environment.systemPackages = with pkgs; [
    v4l-utils
  ];
}
```

Then rebuild your system:

```bash
sudo nixos-rebuild switch
```

#### Option 2: Flake Configuration

If you are using flakes, add to your `flake.nix` modules:

```nix
{
  # In your NixOS module
  boot.extraModulePackages = with config.boot.kernelPackages; [ v4l2loopback ];
  boot.kernelModules = [ "v4l2loopback" ];
  boot.extraModprobeConfig = ''
    options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
  '';
}
```

#### Option 3: Temporary Loading (Testing)

For temporary testing without system rebuild:

```bash
# Load the module manually
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"

# Verify the device was created
v4l2-ctl --list-devices
```

**Note**: This will not persist across reboots.

---

### Arch Linux Setup

#### Install v4l2loopback

```bash
# Install DKMS version (automatically rebuilds with kernel updates)
sudo pacman -S v4l2loopback-dkms

# Or install from AUR for more options
yay -S v4l2loopback-dkms
```

#### Load the Module

```bash
# Load with recommended options
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"

# Verify device creation
v4l2-ctl --list-devices
```

#### Make Persistent (Automatic Load on Boot)

Create `/etc/modules-load.d/v4l2loopback.conf`:

```
v4l2loopback
```

Create `/etc/modprobe.d/v4l2loopback.conf`:

```
options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
```

---

### Fedora Setup

#### Install v4l2loopback

```bash
# Enable RPM Fusion repository if not already enabled
sudo dnf install https://mirrors.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm

# Install v4l2loopback
sudo dnf install v4l2loopback

# Install optional utilities
sudo dnf install v4l-utils
```

#### Load the Module

```bash
# Load the module
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"

# Verify
v4l2-ctl --list-devices
```

#### Make Persistent

Create `/etc/modules-load.d/v4l2loopback.conf`:

```
v4l2loopback
```

Create `/etc/modprobe.d/v4l2loopback.conf`:

```
options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
```

---

### Ubuntu / Debian Setup

#### Install v4l2loopback

```bash
# Update package list
sudo apt update

# Install v4l2loopback DKMS package
sudo apt install v4l2loopback-dkms v4l2loopback-utils

# Install optional utilities
sudo apt install v4l-utils
```

#### Load the Module

```bash
# Load the module
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"

# Verify device creation
v4l2-ctl --list-devices
```

#### Make Persistent

Create `/etc/modules-load.d/v4l2loopback.conf`:

```
v4l2loopback
```

Create `/etc/modprobe.d/v4l2loopback.conf`:

```
options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
```

Reload configuration:

```bash
sudo update-initramfs -u
```

---

### openSUSE Setup

#### Install v4l2loopback

```bash
# Install from standard repository
sudo zypper install v4l2loopback-kmp-default

# Or install DKMS version
sudo zypper install v4l2loopback-dkms
```

#### Load the Module

```bash
# Load the module
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"

# Verify
v4l2-ctl --list-devices
```

#### Make Persistent

Add to `/etc/modules-load.d/v4l2loopback.conf`:

```
v4l2loopback
```

Add options to `/etc/modprobe.d/99-v4l2loopback.conf`:

```
options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
```

---

## Using the Camera

### Starting Camera Streaming

1. **Ensure Devices are Paired**: Your phone and desktop must be paired via COSMIC Connect
2. **Open COSMIC Connect** on your Android device
3. **Navigate to Device Details**: Tap on your paired desktop
4. **Find Camera Plugin**: Scroll to the "Camera" plugin card
5. **Start Streaming**: Tap the **Start Camera** button
6. **Select Camera** (Optional): Tap settings to switch between front and back cameras
7. **Verify Connection**: The status should show "Streaming to [device name]"

### Stopping Camera Streaming

- **From Android**: Tap the **Stop Camera** button in the Camera plugin
- **From Notification**: Pull down notification shade and tap "Stop Streaming"
- **Automatic**: Streaming stops when you disconnect from the paired device

### Camera Controls

While streaming, you can:

- **Switch Cameras**: Toggle between front and back cameras
- **Pause/Resume**: Temporarily pause the stream without stopping
- **Adjust Quality**: Change resolution and frame rate in settings

---

## Selecting the Camera in Apps

After starting camera streaming from your phone, you need to select the virtual webcam in your application.

### Firefox

1. Open Firefox and navigate to a site using your camera (e.g., Google Meet)
2. When prompted for camera access, click **Allow**
3. Click the camera icon in the URL bar
4. Select **"COSMIC Connect Camera"** from the dropdown
5. Click **Apply**

For manual configuration:
1. Go to `about:preferences#privacy`
2. Scroll to **Permissions** > **Camera**
3. Click **Settings**
4. Choose **"COSMIC Connect Camera"** as the default

### Chrome / Chromium

1. Click the three-dot menu > **Settings**
2. Navigate to **Privacy and security** > **Site settings**
3. Click **Camera**
4. Select **"COSMIC Connect Camera"** from the dropdown

Or during a call:
1. Click the camera icon in the address bar
2. Select **"COSMIC Connect Camera"**

### OBS Studio

1. Open OBS Studio
2. In **Sources**, click **+** (Add)
3. Select **Video Capture Device**
4. Name it (e.g., "Phone Camera")
5. In the **Device** dropdown, select **"COSMIC Connect Camera"**
6. Adjust resolution and FPS as needed
7. Click **OK**

**Pro Tip**: Set the resolution to match your phone's output for best quality.

### Zoom

1. Open Zoom and join or start a meeting
2. Click the **^** arrow next to the camera icon
3. Select **"COSMIC Connect Camera"** from the list
4. Your phone camera should now be visible

For persistent settings:
1. Go to **Settings** > **Video**
2. Select **"COSMIC Connect Camera"** as the default

### Microsoft Teams

1. Open Microsoft Teams
2. Click your profile picture > **Settings**
3. Go to **Devices**
4. Under **Camera**, select **"COSMIC Connect Camera"**
5. You should see a preview of your phone camera

During a call:
1. Click the **...** menu in the call controls
2. Select **Device settings**
3. Choose **"COSMIC Connect Camera"**

### Google Meet

1. Join or start a Google Meet
2. Click the three dots > **Settings**
3. Go to **Video**
4. Select **"COSMIC Connect Camera"** from the Camera dropdown
5. Click **Done**

### Discord

1. Open Discord and start a video call
2. Click the gear icon for **User Settings**
3. Go to **Voice & Video**
4. Under **Video Settings**, select **"COSMIC Connect Camera"**
5. You can also change this during a call by clicking the camera dropdown

---

## Configuration Options

### v4l2loopback Module Options

| Option | Description | Default | Recommended |
|--------|-------------|---------|-------------|
| `exclusive_caps=1` | Prevents apps from requesting unsupported formats | 0 | **1** (required for most apps) |
| `card_label="..."` | Display name in application camera lists | "Dummy" | "COSMIC Connect Camera" |
| `video_nr=N` | Device number (/dev/videoN) | Auto | 10 (avoids conflicts) |
| `max_buffers=N` | Maximum frame buffers | 2 | 4 (reduces latency) |
| `max_width=N` | Maximum supported width | 8192 | 1920 |
| `max_height=N` | Maximum supported height | 8192 | 1080 |

### Advanced Configuration Example

```
options v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10 max_buffers=4 max_width=1920 max_height=1080
```

---

## Performance Tips

### For Best Video Quality

1. **Use 5GHz WiFi**: Faster and less congested than 2.4GHz
2. **Strong Signal**: Stay close to your WiFi router
3. **Stable Mount**: Use a phone stand or mount to avoid shaky video
4. **Good Lighting**: Phone cameras perform best with adequate lighting

### For Lower Latency

1. **Lower Resolution**: 720p has less latency than 1080p
2. **Consistent Network**: Avoid bandwidth-heavy activities during calls
3. **Disable Other Streams**: Close other video streaming apps

### For Battery Conservation

1. **Keep Phone Plugged In**: Streaming uses significant battery
2. **Lower Brightness**: Reduce screen brightness while streaming
3. **Use Back Camera**: Front camera preview uses more power

---

## Security Considerations

### Network Security

- **Local Network Only**: Camera streaming only works on your local network
- **TLS Encrypted**: All data is encrypted between phone and desktop
- **No Cloud**: Video never leaves your local network
- **Paired Devices Only**: Only paired (trusted) devices can access your camera

### Privacy

- **Manual Start Required**: Camera streaming must be manually started each time
- **Visible Indicator**: Your phone shows a notification when camera is active
- **Easy Stop**: Stop streaming anytime from phone notification or app
- **No Background Streaming**: Camera stops when app is closed

---

## Troubleshooting

For detailed troubleshooting, see [Camera Troubleshooting Guide](CAMERA_TROUBLESHOOTING.md).

### Quick Fixes

**Camera not appearing in apps?**
```bash
# Check if v4l2loopback is loaded
lsmod | grep v4l2loopback

# List available cameras
v4l2-ctl --list-devices
```

**Black screen in video apps?**
- Ensure streaming is started on your phone
- Check if another app is using the virtual camera
- Try restarting the streaming

**High latency?**
- Switch to 5GHz WiFi
- Lower the resolution in COSMIC Connect camera settings
- Ensure good WiFi signal strength

---

## Related Documentation

- [Camera Troubleshooting Guide](CAMERA_TROUBLESHOOTING.md) - Detailed problem solving
- [User Guide](USER_GUIDE.md) - General COSMIC Connect usage
- [FAQ](FAQ.md) - Frequently asked questions

---

**Enjoy using your phone as a high-quality wireless webcam!**
