# Development Environments

This repository provides **two specialized development environments** via Nix flakes:

1. **Android Development** (default) - For working on the COSMIC Connect Android app
2. **NixOS Development** - For working on NixOS configuration and Nix code

## Quick Start

```bash
# For Android development (default)
nix develop --impure

# For NixOS development
nix develop .#nixos-dev
```

---

## 🤖 Android Development Environment (Default)

**Use this for**: Building and testing the COSMIC Connect Android app

### What's Included

- **Android SDK**: Command-line tools, platform-tools (adb, fastboot)
- **Build Tools**: Gradle, JDK 17
- **Kotlin**: Kotlin compiler, language server, ktlint
- **Rust**: For testing the COSMIC applet (rustc, cargo, rust-analyzer)
- **Network Tools**: wireshark, tcpdump, netcat (for protocol debugging)
- **Version Control**: git, gh (GitHub CLI)

### Enter the Environment

```bash
# Using direnv (automatic when you cd into the directory)
direnv allow

# OR manually
nix develop --impure
```

### Common Tasks

```bash
# Build the Android app
./gradlew assembleDebug

# Install to Waydroid
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Watch logs
adb logcat | grep -i cosmicconnect

# Run tests
./gradlew test

# Lint code
./gradlew lint
```

### Environment Variables Set

- `ANDROID_HOME` - Android SDK location
- `ANDROID_SDK_ROOT` - Same as ANDROID_HOME
- `GRADLE_OPTS` - Gradle optimization flags
- `PATH` - Includes Android SDK tools

---

## 🔧 NixOS Development Environment

**Use this for**: Working on NixOS configuration, Nix code, or flake development

### What's Included

#### Nix Language Tools
- **nixd** - Modern Nix language server (recommended for LSP)
- **nil** - Alternative Nix LSP
- **nixpkgs-fmt** - Fast Nix code formatter
- **nixfmt** - Official Nix formatter (RFC style)
- **statix** - Linter for Nix code (checks best practices)
- **deadnix** - Find unused Nix code

#### Nix Development Tools
- **nix-tree** - Visualize dependency trees
- **nix-diff** - Compare derivations
- **nix-index** - Search for packages
- **nix-prefetch-github** - Fetch GitHub sources for Nix
- **nix-output-monitor** (nom) - Pretty build output

#### NixOS System Tools
- **nixos-rebuild** - Rebuild NixOS system
- **nixos-option** - Query NixOS options
- **nixos-container** - Manage NixOS containers

#### Development Utilities
- **git** / **gh** - Version control
- **jq** / **yq** - JSON/YAML processing
- **ripgrep** / **fd** - Fast search tools
- **bat** / **eza** - Better cat/ls
- **manix** - Search Nix documentation

### Enter the Environment

```bash
nix develop .#nixos-dev
```

### Common Tasks

```bash
# Format all Nix files
nixpkgs-fmt *.nix

# Lint Nix code
statix check

# Find dead Nix code
deadnix

# Check flake
nix flake check --impure

# Update flake inputs
nix flake update

# Rebuild NixOS system
sudo nixos-rebuild switch

# Test NixOS config without activating
sudo nixos-rebuild test

# Visualize package dependencies
nix-tree /nix/store/...

# Compare two derivations
nix-diff /nix/store/old... /nix/store/new...

# Search for packages
nix search nixpkgs kotlin

# Search Nix documentation
manix nixos-option
```

### Helpful Aliases (Auto-loaded)

```bash
nb        # nix build
nf        # nix flake
nfc       # nix flake check --impure
nfu       # nix flake update
nfmt      # nixpkgs-fmt
nlint     # statix check
ndead     # deadnix
nom       # nix-output-monitor (prettier nix builds)
```

### Environment Variables Set

- `NIXPKGS_ALLOW_UNFREE=1` - Allow unfree packages

---

## 🔄 Switching Between Environments

You can easily switch between environments:

```bash
# Start in Android dev environment
nix develop --impure

# Exit and enter NixOS dev environment
exit
nix develop .#nixos-dev

# Or open multiple terminals with different environments
# Terminal 1: Android dev
nix develop --impure

# Terminal 2: NixOS dev
nix develop .#nixos-dev
```

---

## 📝 Using with Your Editor

### VS Code

#### For Android Development:
Install these extensions:
- Kotlin Language
- Android iOS Emulator
- Gradle for Java

Configure settings:
```json
{
  "kotlin.languageServer.enabled": true,
  "java.home": "/nix/store/.../jdk-17"
}
```

#### For NixOS Development:
Install these extensions:
- Nix IDE
- Nix Environment Selector

Configure settings:
```json
{
  "nix.enableLanguageServer": true,
  "nix.serverPath": "nixd",
  "nix.formatterPath": "nixpkgs-fmt"
}
```

### Neovim

#### For Android Development:
```lua
-- Add Kotlin LSP
require('lspconfig').kotlin_language_server.setup{}

-- Android-specific settings
vim.g.android_sdk_path = vim.env.ANDROID_HOME
```

#### For NixOS Development:
```lua
-- Add nixd LSP
require('lspconfig').nixd.setup{}

-- Format on save with nixpkgs-fmt
vim.api.nvim_create_autocmd("BufWritePre", {
  pattern = "*.nix",
  callback = function()
    vim.lsp.buf.format()
  end,
})
```

### Helix

Add to `~/.config/helix/languages.toml`:

```toml
# Kotlin support
[[language]]
name = "kotlin"
language-server = { command = "kotlin-language-server" }
formatter = { command = "ktlint", args = ["--format"] }

# Nix support
[[language]]
name = "nix"
language-server = { command = "nixd" }
formatter = { command = "nixpkgs-fmt" }
```

---

## 🐛 Troubleshooting

### "unfree package" errors

Use `--impure` flag:
```bash
nix develop --impure
```

### Android SDK not found

The Android SDK is automatically configured. If you see errors:
```bash
# Check ANDROID_HOME is set
echo $ANDROID_HOME

# Verify SDK location
ls $ANDROID_HOME
```

### Nix language server not working

Make sure you're in the correct dev shell:
```bash
# For Nix development
nix develop .#nixos-dev

# Verify nixd is available
which nixd
nixd --help
```

### Gradle daemon issues

```bash
# Stop Gradle daemon
./gradlew --stop

# Clear Gradle cache
rm -rf ~/.gradle/caches
```

### Flake check fails

```bash
# Add --impure for unfree packages
nix flake check --impure

# Show full trace
nix flake check --impure --show-trace
```

---

## 📚 Additional Resources

### Android Development
- [SETUP_GUIDE.md](SETUP_GUIDE.md) - Complete Android setup walkthrough
- [GETTING_STARTED.md](GETTING_STARTED.md) - Project onboarding
- [Issue #1](https://github.com/olafkfreund/cosmic-connect-android/issues/1) - Development environment setup

### NixOS Development
- [nixos-waydroid-config.nix](nixos-waydroid-config.nix) - Waydroid system configuration
- [Nix Pills](https://nixos.org/guides/nix-pills/) - Learn Nix deeply
- [NixOS Manual](https://nixos.org/manual/nixos/stable/) - Official documentation

### Flake Documentation
- [Nix Flakes](https://nixos.wiki/wiki/Flakes) - Flake basics
- [Flake Parts](https://flake.parts/) - Modular flake architecture

---

## 🎯 Which Environment Should I Use?

| Task | Environment |
|------|-------------|
| Building Android app | Android (default) |
| Debugging Android issues | Android (default) |
| Testing with Waydroid | Android (default) |
| Working on build.gradle | Android (default) |
| Modifying flake.nix | NixOS dev |
| Updating NixOS config | NixOS dev |
| Formatting Nix code | NixOS dev |
| Creating Nix modules | NixOS dev |

---

**Happy developing! 🚀**

For questions or issues, see:
- [SETUP_GUIDE.md](SETUP_GUIDE.md)
- [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- [Claude Code](.claude/README.md)
