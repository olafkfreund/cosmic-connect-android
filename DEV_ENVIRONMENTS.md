# Development Environments

This repository provides **three specialized development environments** via Nix flakes:

1. **Android Development** (default) - For working on the COSMIC Connect Android app with Rust FFI
2. **Rust Core Development** - For working on cosmic-connect-core library
3. **NixOS Development** - For working on NixOS configuration and Nix code

## Quick Start

```bash
# For Android development (default) - includes Rust for FFI
nix develop --impure

# For Rust core development (cosmic-connect-core)
nix develop .#rust-dev

# For NixOS development
nix develop .#nixos-dev
```

---

## 🤖 Android Development Environment (Default)

**Use this for**: Building the Android app with Rust FFI integration

### What's Included

#### Android Tools
- **Android SDK**: Command-line tools, platform-tools (adb, fastboot)
- **Build Tools**: Gradle, JDK 17
- **Kotlin**: Kotlin compiler, language server (kotlin-language-server), ktlint

#### Rust FFI Tools
- **Rust Toolchain**: rustc, cargo, rust-analyzer
- **cargo-ndk**: Compile Rust for Android (aarch64, armv7, x86_64, i686)
- **uniffi-bindgen**: Generate Kotlin bindings from Rust
- **Android Targets**: Pre-installed Rust Android compilation targets
- **Rust Dev Tools**: clippy (linter), rustfmt (formatter), cargo-watch

#### Development Tools
- **Network Tools**: wireshark, tcpdump, netcat (for protocol debugging)
- **Version Control**: git, gh (GitHub CLI)
- **Build Monitoring**: cargo-watch (auto-rebuild Rust on changes)

### Enter the Environment

```bash
# Using direnv (automatic when you cd into the directory)
direnv allow

# OR manually
nix develop --impure
```

### Common Tasks

#### Android App Development
```bash
# Build the Android app (includes Rust compilation via cargo-ndk)
./gradlew assembleDebug

# Install to Waydroid
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Watch logs
adb logcat | grep -i cosmicconnect

# Run Kotlin tests
./gradlew test

# Lint Kotlin code
./gradlew lint
```

#### Rust FFI Development
```bash
# Build Rust library for Android targets
cd ../cosmic-connect-core
cargo ndk -o ../cosmic-connect-android/app/src/main/jniLibs build

# Generate Kotlin bindings from Rust
uniffi-bindgen generate src/core.udl --language kotlin --out-dir ./bindings

# Run Rust tests
cargo test

# Lint Rust code
cargo clippy -- -D warnings

# Format Rust code
cargo fmt

# Watch for changes and auto-rebuild
cargo watch -x test
```

#### Integrated Development
```bash
# Build both Rust and Android (Gradle handles cargo-ndk automatically)
./gradlew assembleDebug

# Clean everything (Rust + Android)
./gradlew clean
cd ../cosmic-connect-core && cargo clean

# Run all tests (Rust + Kotlin)
cd ../cosmic-connect-core && cargo test && cd ../cosmic-connect-android && ./gradlew test
```

### Environment Variables Set

- `ANDROID_HOME` - Android SDK location
- `ANDROID_SDK_ROOT` - Same as ANDROID_HOME
- `ANDROID_NDK_HOME` - Android NDK for Rust compilation
- `GRADLE_OPTS` - Gradle optimization flags
- `CARGO_TARGET_DIR` - Rust build output directory
- `PATH` - Includes Android SDK tools, cargo, cargo-ndk, uniffi-bindgen

---

## 🦀 Rust Core Development Environment

**Use this for**: Working on the cosmic-connect-core Rust library (protocol implementation)

### What's Included

#### Rust Development Tools
- **Rust Toolchain**: rustc 1.70+, cargo, rust-analyzer (LSP)
- **Build Tools**: cargo-watch, cargo-edit, cargo-expand
- **Linting**: clippy (strict mode), rustfmt (code formatter)
- **Testing**: cargo-nextest (faster test runner), cargo-tarpaulin (coverage)
- **Debugging**: rust-gdb, rust-lldb
- **Profiling**: cargo-flamegraph, perf

#### FFI Development Tools
- **uniffi-rs**: uniffi-bindgen (generate Kotlin/Swift bindings)
- **cbindgen**: Generate C headers (if needed)
- **Cross-compilation**: Support for Linux, Android, iOS targets

#### Protocol Development Tools
- **Network Tools**: wireshark, tcpdump, netcat (protocol debugging)
- **TLS Tools**: openssl, rustls utilities
- **JSON Tools**: jq (JSON processing)

#### Documentation Tools
- **cargo-doc**: Generate Rust documentation
- **mdbook**: Create documentation books
- **cargo-readme**: Generate README from doc comments

### Enter the Environment

```bash
nix develop .#rust-dev
```

### Common Tasks

#### Core Development
```bash
cd cosmic-connect-core

# Build the library
cargo build

# Build with all features
cargo build --all-features

# Run tests
cargo test

# Run tests with coverage
cargo tarpaulin --out Html

# Watch for changes and auto-test
cargo watch -x test

# Lint code (strict)
cargo clippy -- -D warnings

# Format code
cargo fmt

# Check without building
cargo check
```

#### FFI Binding Generation
```bash
# Generate Kotlin bindings
uniffi-bindgen generate src/core.udl --language kotlin --out-dir ./bindings/kotlin

# Generate Swift bindings (for future iOS support)
uniffi-bindgen generate src/core.udl --language swift --out-dir ./bindings/swift

# Test FFI bindings
cargo test --features ffi
```

#### Protocol Testing
```bash
# Run discovery tests
cargo test discovery

# Run TLS tests
cargo test tls

# Run packet serialization tests
cargo test packet

# Run all integration tests
cargo test --test '*'
```

#### Documentation
```bash
# Generate and open docs
cargo doc --open

# Check documentation
cargo doc --no-deps

# Generate README from lib.rs
cargo readme > README.md
```

#### Benchmarking & Profiling
```bash
# Run benchmarks
cargo bench

# Profile with flamegraph
cargo flamegraph --bin cosmic-connect-core

# Check for performance issues
cargo clippy -- -W clippy::perf
```

### Environment Variables Set

- `RUST_BACKTRACE=1` - Full backtraces on panic
- `RUSTFLAGS` - Optimization and warning flags
- `CARGO_INCREMENTAL=1` - Faster incremental compilation
- `PATH` - Includes cargo, rustc, and Rust tools

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
# Start in Android dev environment (includes Rust FFI tools)
nix develop --impure

# Exit and enter Rust core dev environment
exit
nix develop .#rust-dev

# Exit and enter NixOS dev environment
exit
nix develop .#nixos-dev

# Or open multiple terminals with different environments
# Terminal 1: Android dev (Kotlin + Rust FFI)
nix develop --impure

# Terminal 2: Rust core dev (cosmic-connect-core)
nix develop .#rust-dev

# Terminal 3: NixOS dev (system configuration)
nix develop .#nixos-dev
```

### Recommended Workflow

**Phase 0 (Weeks 1-3): Rust Core Extraction**
- Use **Rust Core Development** environment
- Work in `cosmic-connect-core` repository
- Extract protocol from COSMIC applet

**Phase 1+ (Weeks 4-20): Android Integration**
- Use **Android Development** environment (includes Rust FFI tools)
- Work in `cosmic-connect-android` repository
- Integrate Rust core via FFI

---

## 📝 Using with Your Editor

### VS Code

#### For Android Development:
Install these extensions:
- Kotlin Language
- Android iOS Emulator
- Gradle for Java
- rust-analyzer (for Rust FFI code)

Configure settings:
```json
{
  "kotlin.languageServer.enabled": true,
  "java.home": "/nix/store/.../jdk-17",
  "rust-analyzer.cargo.features": "all",
  "rust-analyzer.checkOnSave.command": "clippy"
}
```

#### For Rust Core Development:
Install these extensions:
- rust-analyzer (Rust LSP)
- CodeLLDB (debugging)
- Better TOML
- crates (dependency management)

Configure settings:
```json
{
  "rust-analyzer.cargo.features": "all",
  "rust-analyzer.checkOnSave.command": "clippy",
  "rust-analyzer.checkOnSave.allTargets": true,
  "rust-analyzer.inlayHints.enable": true,
  "[rust]": {
    "editor.defaultFormatter": "rust-lang.rust-analyzer",
    "editor.formatOnSave": true
  }
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

-- Add Rust LSP (for FFI code)
require('lspconfig').rust_analyzer.setup{
  settings = {
    ['rust-analyzer'] = {
      checkOnSave = {
        command = "clippy"
      }
    }
  }
}

-- Android-specific settings
vim.g.android_sdk_path = vim.env.ANDROID_HOME
```

#### For Rust Core Development:
```lua
-- Add Rust LSP
require('lspconfig').rust_analyzer.setup{
  settings = {
    ['rust-analyzer'] = {
      cargo = {
        features = "all"
      },
      checkOnSave = {
        command = "clippy",
        allTargets = true
      }
    }
  }
}

-- Format on save
vim.api.nvim_create_autocmd("BufWritePre", {
  pattern = "*.rs",
  callback = function()
    vim.lsp.buf.format()
  end,
})
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

# Rust support
[[language]]
name = "rust"
language-server = { command = "rust-analyzer" }
formatter = { command = "rustfmt" }

[language.config.rust-analyzer]
cargo = { features = "all" }
checkOnSave = { command = "clippy" }

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
| **Phase 0: Rust Core Extraction** | |
| Extracting protocol from COSMIC applet | Rust core dev |
| Implementing NetworkPacket in Rust | Rust core dev |
| Implementing TLS/Discovery in Rust | Rust core dev |
| Setting up uniffi-rs FFI | Rust core dev |
| Writing Rust tests | Rust core dev |
| Generating FFI bindings | Rust core dev |
| **Phase 1+: Android Integration** | |
| Building Android app (with Rust) | Android (default) |
| Debugging Android issues | Android (default) |
| Testing with Waydroid | Android (default) |
| Working on build.gradle | Android (default) |
| Integrating cargo-ndk | Android (default) |
| Creating FFI wrapper layer | Android (default) |
| Writing Kotlin tests | Android (default) |
| **System Configuration** | |
| Modifying flake.nix | NixOS dev |
| Updating NixOS config | NixOS dev |
| Formatting Nix code | NixOS dev |
| Creating Nix modules | NixOS dev |
| Adding dev shell tools | NixOS dev |

---

**Happy developing! 🚀**

For questions or issues, see:
- [SETUP_GUIDE.md](SETUP_GUIDE.md)
- [GitHub Issues](https://github.com/olafkfreund/cosmic-connect-android/issues)
- [Claude Code](.claude/README.md)
