# Camera Webcam Troubleshooting Guide

**Solutions for common camera streaming issues in COSMIC Connect**

**Version:** 1.0.0
**Last Updated:** January 31, 2026
**Issue:** #111 (Part of Camera Feature #99)

---

## Table of Contents

- [Quick Diagnostic Commands](#quick-diagnostic-commands)
- [Camera Not Detected](#camera-not-detected)
- [Black Screen Issues](#black-screen-issues)
- [High Latency / Lag](#high-latency--lag)
- [Choppy or Stuttering Video](#choppy-or-stuttering-video)
- [v4l2loopback Module Issues](#v4l2loopback-module-issues)
- [Application-Specific Issues](#application-specific-issues)
- [Android-Side Issues](#android-side-issues)
- [Network Issues](#network-issues)
- [Advanced Debugging](#advanced-debugging)

---

## Quick Diagnostic Commands

Run these commands to quickly identify common issues:

```bash
# Check if v4l2loopback is loaded
lsmod | grep v4l2loopback

# List all video devices
v4l2-ctl --list-devices

# Check device capabilities
v4l2-ctl -d /dev/video10 --all

# Watch for video device changes
sudo dmesg -w | grep -i video

# Check COSMIC Connect status
systemctl --user status cosmic-connect
```

---

## Camera Not Detected

### Symptom
Applications do not show "COSMIC Connect Camera" in their camera selection.

### Solution 1: v4l2loopback Not Loaded

**Check if the module is loaded:**
```bash
lsmod | grep v4l2loopback
```

**If no output, load the module:**
```bash
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"
```

**Verify the device was created:**
```bash
v4l2-ctl --list-devices
```

You should see something like:
```
COSMIC Connect Camera (platform:v4l2loopback-000):
        /dev/video10
```

### Solution 2: Module Loaded But No Device

**Check kernel messages for errors:**
```bash
sudo dmesg | grep v4l2loopback
```

**Try unloading and reloading:**
```bash
sudo modprobe -r v4l2loopback
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"
```

### Solution 3: Missing exclusive_caps Option

Many applications require `exclusive_caps=1` to recognize the device.

**Reload with correct options:**
```bash
sudo modprobe -r v4l2loopback
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=10
```

### Solution 4: Device Number Conflict

If another device is using `/dev/video10`:

**List current devices:**
```bash
ls -la /dev/video*
```

**Use a different device number:**
```bash
sudo modprobe -r v4l2loopback
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" video_nr=20
```

---

## Black Screen Issues

### Symptom
Camera appears in application list but shows only a black screen.

### Solution 1: Streaming Not Started

**Verify streaming is active on your phone:**
1. Open COSMIC Connect on your Android device
2. Go to your paired desktop's details
3. Check the Camera plugin shows "Streaming" status
4. If not, tap "Start Camera" to begin streaming

### Solution 2: Wrong Video Device Selected

**Confirm which device COSMIC Connect is using:**
```bash
# Check COSMIC Connect logs
journalctl --user -u cosmic-connect | grep -i video

# Or check which video device is receiving data
v4l2-ctl -d /dev/video10 --stream-count=1
```

**If multiple loopback devices exist, try each one in your app.**

### Solution 3: Permission Issues

**Check device permissions:**
```bash
ls -la /dev/video10
```

**If not accessible by your user:**
```bash
# Add your user to the video group
sudo usermod -aG video $USER

# Log out and log back in, or:
newgrp video
```

### Solution 4: Another Application Holding the Device

**Check if another process is using the camera:**
```bash
sudo lsof /dev/video10
```

**If an application is listed, close it or select a different camera in that app.**

### Solution 5: Application Caching Old Device List

Some applications cache device lists. Try:
1. Close the application completely
2. Reload the v4l2loopback module
3. Reopen the application

---

## High Latency / Lag

### Symptom
Video has noticeable delay (more than 1 second) between action and display.

### Solution 1: Network Congestion

**Test network quality:**
```bash
# Ping your phone (find IP in COSMIC Connect app)
ping -c 10 <phone-ip>

# Check for packet loss or high latency
```

**Improvements:**
- Use 5GHz WiFi instead of 2.4GHz
- Move closer to your WiFi router
- Disconnect other devices using heavy bandwidth
- Disable VPN if active

### Solution 2: Resolution Too High

**Lower the camera resolution:**
1. Open COSMIC Connect on your phone
2. Go to Camera settings
3. Select a lower resolution (720p instead of 1080p)

### Solution 3: Increase Buffer Count

**Modify v4l2loopback settings:**
```bash
sudo modprobe -r v4l2loopback
sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera" max_buffers=4
```

### Solution 4: Frame Rate Too High

**Lower the frame rate:**
1. In COSMIC Connect camera settings, select 15fps or 24fps instead of 30fps
2. Lower frame rates reduce bandwidth requirements and latency

---

## Choppy or Stuttering Video

### Symptom
Video is not smooth; it skips frames or freezes intermittently.

### Solution 1: WiFi Signal Strength

**Check signal quality on your phone:**
- Open WiFi settings
- Check signal strength (should be "Excellent" or "Good")

**If weak signal:**
- Move closer to router
- Remove obstacles between phone and router
- Consider a WiFi extender

### Solution 2: CPU Overload

**Check desktop CPU usage:**
```bash
top -d 1
```

**If CPU is maxed out:**
- Close unnecessary applications
- Lower stream resolution
- Disable video effects in the receiving application

### Solution 3: Encoding Issues on Phone

**Check if your phone supports hardware encoding:**
1. Open COSMIC Connect > Camera settings
2. Enable "Hardware Encoding" if available
3. If hardware encoding causes issues, try software encoding

### Solution 4: USB Debugging Conflict

If USB debugging is connected:
1. Disconnect USB cable
2. Streaming over network may conflict with USB debugging
3. Use only wireless connection

---

## v4l2loopback Module Issues

### Module Fails to Load

**Check for kernel module issues:**
```bash
sudo dmesg | tail -50
```

**Common causes and fixes:**

**Secure Boot blocking module:**
```bash
# Check if Secure Boot is enabled
mokutil --sb-state

# You may need to sign the module or disable Secure Boot
```

**DKMS not building module:**
```bash
# Rebuild DKMS modules
sudo dkms autoinstall

# Check DKMS status
dkms status
```

**Kernel version mismatch:**
```bash
# Install matching kernel headers
# For Arch:
sudo pacman -S linux-headers

# For Ubuntu/Debian:
sudo apt install linux-headers-$(uname -r)

# For Fedora:
sudo dnf install kernel-devel

# Then rebuild module
sudo dkms autoinstall
```

### Module Loads But Device Not Created

**Check module parameters:**
```bash
cat /sys/module/v4l2loopback/parameters/devices
```

**Force device creation:**
```bash
sudo modprobe -r v4l2loopback
sudo modprobe v4l2loopback devices=1 exclusive_caps=1 card_label="COSMIC Connect Camera"
```

### Multiple v4l2loopback Devices

**If you need multiple virtual cameras:**
```bash
sudo modprobe v4l2loopback devices=2 video_nr=10,11 card_label="Camera1","Camera2" exclusive_caps=1,1
```

---

## Application-Specific Issues

### Firefox

**Camera not appearing:**
1. Go to `about:config`
2. Search for `media.navigator.video.enabled`
3. Ensure it is `true`
4. Restart Firefox

**Permission denied:**
1. Click the lock icon in the address bar
2. Clear camera permission
3. Refresh and allow camera access again

### Chrome / Chromium

**Camera access blocked:**
1. Go to `chrome://settings/content/camera`
2. Remove any blocked sites
3. Ensure the correct camera is selected

**Fake device flag (for testing):**
```bash
google-chrome-stable --use-fake-device-for-media-stream --use-fake-ui-for-media-stream
```

### OBS Studio

**Camera shows "No signal":**
1. Ensure Format is set to "Auto" or "YUV420"
2. Try different resolution settings
3. Disable "Use Buffering"

**OBS specific settings:**
- Video Format: YUYV 4:2:2 or Auto
- Resolution: Match your stream resolution
- FPS: Match your stream frame rate

### Zoom

**Black screen in Zoom:**
1. Go to Settings > Video
2. Disable "HD"
3. Disable "Touch up my appearance"
4. Disable "Adjust for low light"
5. These effects may conflict with virtual cameras

### Microsoft Teams

**Camera not working in Teams:**
1. Ensure Teams has camera permission in system settings
2. Try the web version (teams.microsoft.com) if desktop app fails
3. Clear Teams cache: `rm -rf ~/.config/Microsoft/Microsoft\ Teams/`

---

## Android-Side Issues

### Camera Permission Denied

1. Go to Android Settings > Apps > COSMIC Connect
2. Tap Permissions
3. Enable Camera permission
4. Restart the app

### Camera In Use By Another App

1. Close other apps that might be using the camera
2. Force stop camera-related apps if necessary
3. Try switching between front and back cameras

### Streaming Stops Unexpectedly

**Battery optimization killing the service:**
1. Go to Settings > Battery > COSMIC Connect
2. Select "Unrestricted" or "Don't optimize"
3. This prevents Android from killing the background service

**Network switching:**
- Disable automatic network switching while streaming
- Stay connected to the same WiFi network

### Poor Quality from Phone Camera

**Ensure adequate lighting:**
- Phone cameras perform poorly in low light
- Position light sources in front of you, not behind

**Clean the camera lens:**
- Smudges significantly reduce image quality

**Check camera settings:**
- In COSMIC Connect, try different camera quality presets

---

## Network Issues

### Devices Not Finding Each Other

**Verify both devices are on the same network:**
```bash
# On desktop
ip addr | grep "inet "

# Compare with IP shown in COSMIC Connect app on phone
```

**Check for AP isolation:**
- Some routers isolate WiFi clients (common in guest networks)
- Use main network, not guest network
- Check router settings for "AP Isolation" or "Client Isolation"

### Firewall Blocking Connection

**Open required ports:**

For UFW (Ubuntu):
```bash
sudo ufw allow 1716/tcp
sudo ufw allow 1716/udp
sudo ufw allow 1764:1865/tcp
```

For firewalld (Fedora):
```bash
sudo firewall-cmd --permanent --add-port=1716/tcp
sudo firewall-cmd --permanent --add-port=1716/udp
sudo firewall-cmd --permanent --add-port=1764-1865/tcp
sudo firewall-cmd --reload
```

For iptables:
```bash
sudo iptables -A INPUT -p tcp --dport 1716 -j ACCEPT
sudo iptables -A INPUT -p udp --dport 1716 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 1764:1865 -j ACCEPT
```

### VPN Interfering

- Disable VPN on both devices while using camera streaming
- Some VPNs block local network traffic
- Use split tunneling to exclude local network if VPN is required

---

## Advanced Debugging

### Enable Verbose Logging

**On Desktop:**
```bash
# View COSMIC Connect logs
journalctl --user -u cosmic-connect -f

# View kernel video messages
sudo dmesg -w | grep -i "v4l2\|video"
```

**On Android:**
1. Open COSMIC Connect
2. Go to Settings > Advanced > Enable Debug Logging
3. Reproduce the issue
4. Go to Settings > Advanced > Export Logs

### Test Video Pipeline

**Test v4l2loopback with a test pattern:**
```bash
# Install gstreamer if not present
# Ubuntu/Debian: sudo apt install gstreamer1.0-tools
# Fedora: sudo dnf install gstreamer1-plugins-base

# Send test pattern to virtual camera
gst-launch-1.0 videotestsrc ! video/x-raw,width=640,height=480 ! v4l2sink device=/dev/video10
```

**Then open any camera app to verify the test pattern displays.**

### Monitor Network Traffic

**Check if video data is being received:**
```bash
# Watch for incoming packets on camera port
sudo tcpdump -i any port 1764 -c 100
```

### Reset Everything

**Complete reset procedure:**

1. Stop COSMIC Connect on both devices
2. Unload v4l2loopback:
   ```bash
   sudo modprobe -r v4l2loopback
   ```
3. Clear application camera caches (close and reopen apps)
4. Reload module:
   ```bash
   sudo modprobe v4l2loopback exclusive_caps=1 card_label="COSMIC Connect Camera"
   ```
5. Start COSMIC Connect desktop app
6. Start COSMIC Connect on phone
7. Start camera streaming
8. Test in application

---

## Getting Further Help

If these solutions do not resolve your issue:

1. **Search Existing Issues**: [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues?q=camera)

2. **Open a New Issue** with:
   - Your Linux distribution and version
   - Android version and phone model
   - Output of:
     ```bash
     lsmod | grep v4l2loopback
     v4l2-ctl --list-devices
     v4l2-ctl -d /dev/video10 --all
     ```
   - COSMIC Connect logs from both desktop and phone
   - Steps you have already tried

3. **Community Help**: [GitHub Discussions](https://github.com/olafkfreund/cosmic-connect-android/discussions)

---

## Related Documentation

- [Camera Webcam User Guide](CAMERA_WEBCAM.md) - Setup and usage instructions
- [User Guide](USER_GUIDE.md) - General COSMIC Connect usage
- [FAQ](FAQ.md) - Frequently asked questions

---

**Still stuck? We are here to help! Open an issue on GitHub with your debug information.**
