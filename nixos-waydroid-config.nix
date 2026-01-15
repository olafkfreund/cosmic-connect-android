# NixOS Configuration for COSMIC Connect Android Development with Waydroid
#
# Add this to your NixOS configuration (/etc/nixos/configuration.nix)
# or import it as a module.

{ config, pkgs, lib, ... }:

{
  # ============================================================================
  # Waydroid Configuration
  # ============================================================================

  # Enable Waydroid for Android container support
  virtualisation.waydroid.enable = true;

  # Required kernel modules for Waydroid
  boot.kernelModules = [ "binder_linux" "ashmem_linux" ];

  # ============================================================================
  # Networking Configuration
  # ============================================================================

  networking = {
    # KDE Connect / COSMIC Connect ports
    firewall = {
      # TCP ports for data transfer
      allowedTCPPorts = [
        1714  # KDE Connect protocol (primary)
        1715  # KDE Connect protocol (secondary)
        1716  # KDE Connect protocol (tertiary)
      ];

      # UDP port for device discovery
      allowedUDPPorts = [
        1716  # KDE Connect discovery broadcasts
      ];
    };
  };

  # ============================================================================
  # User Configuration
  # ============================================================================

  # Add your user to required groups for Waydroid access
  users.users.olafkfreund = {
    extraGroups = [
      "adbusers"    # For ADB access
      "kvm"         # For hardware acceleration (optional)
    ];
  };

  # ============================================================================
  # System Packages
  # ============================================================================

  environment.systemPackages = with pkgs; [
    # Waydroid essentials
    waydroid

    # Android development tools (if not using flake)
    # android-tools

    # Optional: GUI tools for easier Waydroid management
    # wdisplays  # Wayland display configuration
    # cage       # Wayland kiosk compositor (for dedicated Android window)
  ];

  # ============================================================================
  # Optional: Waydroid Service Customization
  # ============================================================================

  # Uncomment to auto-start Waydroid container service on boot
  # systemd.services.waydroid-container = {
  #   wantedBy = [ "multi-user.target" ];
  # };

  # ============================================================================
  # Optional: Performance Tuning
  # ============================================================================

  # For better Waydroid performance, enable PSI (Pressure Stall Information)
  # boot.kernelParams = [ "psi=1" ];

  # ============================================================================
  # Notes
  # ============================================================================

  # After applying this configuration:
  #
  # 1. Rebuild NixOS:
  #    sudo nixos-rebuild switch
  #
  # 2. Initialize Waydroid (first time only):
  #    sudo waydroid init
  #
  # 3. Start Waydroid session:
  #    waydroid session start
  #
  # 4. Show Waydroid UI:
  #    waydroid show-full-ui
  #
  # 5. Check if ADB sees Waydroid:
  #    adb devices
  #
  # 6. If ADB doesn't see Waydroid automatically:
  #    adb connect 192.168.250.2:5555
}
