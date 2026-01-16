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

        # Android SDK components with NDK for Rust compilation
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "34" "35" "36" ];
          buildToolsVersions = [ "34.0.0" "35.0.0" ];
          includeNDK = true;
          ndkVersions = [ "27.0.12077973" ];  # NDK 27 for Rust Android targets (matches Gradle requirement)
          includeSystemImages = false;
        };

        # Rust toolchain with Android cross-compilation targets
        rustWithAndroidTargets = pkgs.rust-bin.stable.latest.default.override {
          extensions = [ "rust-src" "rust-analyzer" ];
          targets = [
            "aarch64-linux-android"     # arm64-v8a
            "armv7-linux-androideabi"   # armeabi-v7a
            "x86_64-linux-android"      # x86_64
            "i686-linux-android"        # x86
          ];
        };

      in
      {
        devShells = {
          # Default shell: Android development
          default = pkgs.mkShell {
          buildInputs = with pkgs; [
            # Core Android development
            android-tools           # adb, fastboot, logcat
            gradle                  # Build system
            jdk17                   # Java 17 for Android/Gradle

            # Kotlin development
            kotlin
            kotlin-language-server  # LSP for IDE integration
            ktlint                  # Kotlin linter

            # Rust toolchain with Android cross-compilation
            rustWithAndroidTargets  # Rust with Android targets
            cargo-ndk               # cargo-ndk for Android builds

            # Version control & collaboration
            git
            gh                      # GitHub CLI

            # Network debugging
            wireshark              # For protocol debugging
            tcpdump
            netcat

            # Build & packaging
            gnumake

            # Optional: Editor support
            # Uncomment based on your preference:
            # vscode
            # neovim
          ];

          shellHook = ''
            echo ""
            echo "╔════════════════════════════════════════════════════════════╗"
            echo "║   🚀 COSMIC Connect Android - Development Environment     ║"
            echo "╚════════════════════════════════════════════════════════════╝"
            echo ""
            echo "📦 Environment:"
            echo "  • Android Tools: Ready"
            echo "  • Gradle:        $(gradle --version 2>/dev/null | head -1 || echo 'Ready')"
            echo "  • Java:          $(java --version 2>/dev/null | head -1 || echo 'JDK 17')"
            echo "  • Kotlin:        Ready"
            echo "  • Rust:          $(rustc --version 2>/dev/null || echo 'Ready')"
            echo ""
            echo "🎯 Quick Commands:"
            echo "  • Build:         ./gradlew assembleDebug"
            echo "  • Clean:         ./gradlew clean"
            echo "  • Test:          ./gradlew test"
            echo "  • Lint:          ./gradlew lint"
            echo ""
            echo "📱 Waydroid Commands:"
            echo "  • Check devices: adb devices"
            echo "  • Install APK:   adb install -r app/build/outputs/apk/debug/app-debug.apk"
            echo "  • View logs:     adb logcat | grep -i cosmicconnect"
            echo "  • Clear data:    adb shell pm clear org.cosmic.cosmicconnect"
            echo ""
            echo "🔍 Network Debugging:"
            echo "  • Scan ports:    ss -tulnp | grep 171[4-6]"
            echo "  • Firewall:      sudo nft list ruleset | grep 171[4-6]"
            echo ""
            echo "📚 Documentation:"
            echo "  • Issue #1:      gh issue view 1"
            echo "  • Getting Started: less GETTING_STARTED.md"
            echo "  • Project Plan:  less PROJECT_PLAN.md"
            echo ""
            echo "💡 To start Waydroid:"
            echo "  1. sudo waydroid init     (first time only)"
            echo "  2. waydroid session start"
            echo "  3. waydroid show-full-ui"
            echo ""

            # Set ANDROID_HOME for Gradle
            export ANDROID_HOME="${androidComposition.androidsdk}/libexec/android-sdk"
            export ANDROID_SDK_ROOT="$ANDROID_HOME"

            # Set ANDROID_NDK_HOME for cargo-ndk and Rust compilation
            export ANDROID_NDK_HOME="$ANDROID_HOME/ndk-bundle"
            if [ -d "$ANDROID_HOME/ndk" ]; then
              # Use the specific NDK version if available
              export ANDROID_NDK_HOME="$(ls -d $ANDROID_HOME/ndk/* 2>/dev/null | head -1)"
            fi

            # Add Android SDK tools to PATH
            export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"

            # Gradle configuration
            export GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true"

            echo "✅ Environment ready! Type 'nix develop' to reload."
            echo ""
          '';
        };

        # NixOS development shell
        nixos-dev = pkgs.mkShell {
          name = "nixos-development";

          buildInputs = with pkgs; [
            # Nix development tools
            nixd                    # Nix language server (modern, recommended)
            nil                     # Alternative Nix LSP
            nixpkgs-fmt            # Nix code formatter
            nixfmt                 # Official Nix formatter (RFC style)
            statix                 # Nix linter & best practices checker
            deadnix                # Find unused Nix code
            nix-tree               # Visualize Nix dependencies
            nix-diff               # Compare Nix derivations
            nix-index              # Search for packages
            nix-prefetch-github    # Fetch GitHub sources
            nix-output-monitor     # Beautiful build output

            # NixOS specific tools
            nixos-rebuild          # Rebuild NixOS system
            nixos-option           # Query NixOS options
            nixos-container        # Manage NixOS containers

            # Development utilities
            git                    # Version control
            gh                     # GitHub CLI
            jq                     # JSON processing
            yq                     # YAML processing
            ripgrep                # Fast grep
            fd                     # Fast find
            bat                    # Better cat
            eza                    # Better ls

            # Documentation
            man-pages
            manix                  # Search Nix documentation
          ];

          shellHook = ''
            echo ""
            echo "╔════════════════════════════════════════════════════════════╗"
            echo "║              🔧 NixOS Development Environment              ║"
            echo "╚════════════════════════════════════════════════════════════╝"
            echo ""
            echo "📦 Nix Tools Available:"
            echo "  • nixd              - Nix language server (LSP)"
            echo "  • nil               - Alternative Nix LSP"
            echo "  • nixpkgs-fmt       - Format Nix code"
            echo "  • statix            - Lint Nix code"
            echo "  • deadnix           - Find dead Nix code"
            echo "  • nix-tree          - Visualize dependencies"
            echo "  • nix-diff          - Compare derivations"
            echo ""
            echo "🎯 Common Commands:"
            echo "  • Format:           nixpkgs-fmt *.nix"
            echo "  • Lint:             statix check"
            echo "  • Find dead code:   deadnix"
            echo "  • Build flake:      nix build"
            echo "  • Check flake:      nix flake check --impure"
            echo "  • Update flake:     nix flake update"
            echo ""
            echo "🔍 NixOS System:"
            echo "  • Rebuild:          sudo nixos-rebuild switch"
            echo "  • Test config:      sudo nixos-rebuild test"
            echo "  • Build config:     sudo nixos-rebuild build"
            echo "  • Query options:    nixos-option [option-path]"
            echo ""
            echo "📚 Documentation:"
            echo "  • Search packages:  nix search nixpkgs [package]"
            echo "  • Search docs:      manix [query]"
            echo "  • NixOS manual:     man configuration.nix"
            echo ""
            echo "💡 Tips:"
            echo "  • Use 'nom' instead of 'nix' for prettier build output"
            echo "  • Use 'nix-tree' to understand package dependencies"
            echo "  • Use 'nix-diff' to compare derivation changes"
            echo ""

            # Set up environment variables
            export NIXPKGS_ALLOW_UNFREE=1

            # Helpful aliases
            alias nb="nix build"
            alias nf="nix flake"
            alias nfc="nix flake check --impure"
            alias nfu="nix flake update"
            alias nfmt="nixpkgs-fmt"
            alias nlint="statix check"
            alias ndead="deadnix"
            alias nom="nix-output-monitor"

            echo "✅ NixOS dev environment ready!"
            echo "💡 To switch to Android dev: nix develop"
            echo ""
          '';
        };
      };
      }
    );
}
