{
  description = "COSMIC Connect Android - Development Environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        # Android SDK components
        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "34" ];
          buildToolsVersions = [ "34.0.0" ];
          includeNDK = false;
          includeSystemImages = false;
        };

      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            # Core Android development
            android-tools           # adb, fastboot, logcat
            gradle                  # Build system
            jdk17                   # Java 17 for Android/Gradle

            # Kotlin development
            kotlin
            kotlin-language-server  # LSP for IDE integration
            ktlint                  # Kotlin linter

            # Rust toolchain (for COSMIC applet testing)
            rustc
            cargo
            rustfmt
            rust-analyzer

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
            echo "  • View logs:     adb logcat | grep -i kdeconnect"
            echo "  • Clear data:    adb shell pm clear org.kde.kdeconnect_tp"
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

            # Add Android SDK tools to PATH
            export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools:$PATH"

            # Gradle configuration
            export GRADLE_OPTS="-Dorg.gradle.daemon=true -Dorg.gradle.parallel=true"

            echo "✅ Environment ready! Type 'nix develop' to reload."
            echo ""
          '';
        };
      }
    );
}
