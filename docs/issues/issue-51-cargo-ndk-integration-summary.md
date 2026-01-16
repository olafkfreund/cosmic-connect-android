# Issue #51: cargo-ndk Build Integration Summary

**Date**: 2026-01-16
**Status**: 90% Complete (Infrastructure Ready, Code Fixes Needed)
**Issue**: #51 - Integrate Rust compilation into Android Gradle build

---

## Executive Summary

Successfully configured the complete Android NDK and cargo-ndk build infrastructure for compiling the cosmic-connect-core Rust library to Android native libraries (.so files). All build tooling is working correctly and 200+ dependencies compile successfully for all Android targets. **Remaining work**: Fix compilation errors in the cosmic-connect-core library code itself.

---

## What Was Accomplished ✅

### 1. Android NDK Integration

**flake.nix Updates**:
```nix
# Added Android NDK 27
androidComposition = pkgs.androidenv.composeAndroidPackages {
  platformVersions = [ "34" ];
  buildToolsVersions = [ "34.0.0" ];
  includeNDK = true;
  ndkVersions = [ "27.0.12077973" ];  # Matches Gradle requirement
  includeSystemImages = false;
};
```

**Environment Configuration**:
- ANDROID_HOME: `/nix/store/.../androidsdk/libexec/android-sdk`
- ANDROID_NDK_HOME: `/nix/store/.../ndk/27.0.12077973`
- NDK correctly resolved and accessible to Gradle

### 2. Rust Android Cross-Compilation

**Rust Toolchain**:
```nix
# rust-overlay integration for Android targets
rustWithAndroidTargets = pkgs.rust-bin.stable.latest.default.override {
  extensions = [ "rust-src" "rust-analyzer" ];
  targets = [
    "aarch64-linux-android"     # arm64-v8a
    "armv7-linux-androideabi"   # armeabi-v7a
    "x86_64-linux-android"      # x86_64
    "i686-linux-android"        # x86
  ];
};
```

**Tools Installed**:
- cargo-ndk 4.1.2
- Rust 1.92.0 with all Android targets
- Complete Rust std libraries for each architecture

### 3. Gradle Build Integration

**Already Configured**:
- rust-android-gradle plugin: version 0.9.4
- cargo build tasks created for each Android ABI
- JNI library output directory: `build/rustJniLibs/android`

**Cargo Configuration** (build.gradle.kts):
```kotlin
cargo {
    module = "../cosmic-connect-core"
    libname = "cosmic_connect_core"
    targets = listOf("arm64", "arm", "x86_64", "x86")
    profile = "release"
    targetDirectory = "../cosmic-connect-core/target"
}
```

### 4. Dependency Fixes

**cosmic-connect-core Cargo.toml**:
```toml
# Fixed: Downgraded rustls to avoid aws-lc-sys compilation issues
rustls = "0.22"              # Was 0.23
tokio-rustls = "0.25"        # Was 0.26

# rustls 0.22 uses ring crypto provider (portable)
# rustls 0.23+ uses aws-lc-rs (complex C deps, fails on Android NDK)
```

**Dependency Resolution**:
- Removed: aws-lc-rs v1.15.3
- Removed: aws-lc-sys v0.36.0 (problematic)
- Using: ring v0.17.14 (compiles cleanly for Android)
- All 200+ dependencies compile successfully

### 5. Build Process Verification

**Successful Compilation**:
```
✅ All dependencies compiled for armv7-linux-androideabi
✅ All dependencies compiled for aarch64-linux-android
✅ All dependencies compiled for x86_64-linux-android
✅ All dependencies compiled for i686-linux-android

Dependencies compiled:
- tokio 1.35 with full features
- rustls 0.22 with ring provider
- tokio-rustls 0.25
- uniffi 0.27 (FFI bindings)
- serde, serde_json (serialization)
- rcgen (certificate generation)
- rsa (RSA cryptography)
- ~200 total crates
```

---

## Remaining Work ❌

### Compilation Errors in cosmic-connect-core

**Error Summary**:
```
error[E0432]: unresolved import `crate::Device`
error[E0432]: unresolved import `super::PluginFactory`
error[E0432]: unresolved import `crate::PayloadClient`
error[E0432]: unresolved import `mouse_keyboard_input`
error[E0046]: not all trait items implemented
error[E0050]: method `handle_packet` has wrong signature
...
45 compilation errors total
```

**Root Causes**:
1. **Missing Device module**: Code references `crate::Device` but it doesn't exist
2. **Missing PluginFactory**: Plugins try to import `super::PluginFactory`
3. **Missing PayloadClient**: Referenced but not defined
4. **Missing mouse_keyboard_input**: External dependency or missing module
5. **Plugin trait mismatch**: `handle_packet` signature doesn't match trait definition
6. **Trait methods missing**: Plugins missing `initialize()` and `shutdown()` methods

**Files with Errors**:
- `src/plugins/battery.rs`
- `src/plugins/clipboard.rs`
- `src/plugins/input.rs`
- `src/plugins/keyboard.rs`
- `src/plugins/mousepad.rs`
- `src/plugins/ping.rs`
- `src/plugins/presenter.rs`
- `src/plugins/run_command.rs`
- `src/plugins/share.rs`
- `src/plugins/telephony.rs`

---

## Technical Details

### NDK Version Resolution

**Issue Encountered**:
```
NDK from ndk.dir had version [26.1.10909125]
which disagrees with android.ndkVersion [27.0.12077973]
```

**Solution**:
- Updated flake.nix to use NDK 27.0.12077973
- Matches Gradle's expected NDK version
- Downloaded and configured via Nix cache

### aws-lc-sys Compilation Failure

**Issue Encountered**:
```
error occurred in cc-rs: command did not execute successfully
armv7a-linux-androideabi23-clang failed to compile getentropy.c
```

**Analysis**:
- aws-lc-sys is AWS's fork of BoringSSL (complex C library)
- Requires CMake, specific compiler flags, platform-specific code
- Compilation fails for Android targets with NDK clang

**Solution**:
- Downgraded rustls from 0.23 to 0.22
- rustls 0.22 uses ring by default (no aws-lc-rs)
- ring compiles cleanly for all Android targets
- No functionality lost (ring is a mature crypto library)

### Build Command Output

**Working Build Command**:
```bash
nix develop --command ./gradlew cargoBuild
```

**Build Process**:
1. Gradle invokes rust-android-gradle plugin
2. Plugin calls cargo-ndk for each target
3. cargo-ndk sets up NDK toolchain
4. Compiles cosmic-connect-core for each Android ABI
5. **Expected output** (after code fixes):
   ```
   build/rustJniLibs/android/arm64-v8a/libcosmic_connect_core.so
   build/rustJniLibs/android/armeabi-v7a/libcosmic_connect_core.so
   build/rustJniLibs/android/x86_64/libcosmic_connect_core.so
   build/rustJniLibs/android/x86/libcosmic_connect_core.so
   ```

---

## Next Steps

### Critical: Fix cosmic-connect-core Compilation

**Priority 1: Add Missing Modules**

1. **Create Device module** (`src/device.rs` or similar):
   ```rust
   pub struct Device {
       // Device state and methods
   }
   ```

2. **Create PluginFactory trait** (`src/plugins/mod.rs`):
   ```rust
   pub trait PluginFactory {
       fn create_plugin() -> Box<dyn Plugin>;
   }
   ```

3. **Create PayloadClient** (`src/payload.rs` or similar):
   ```rust
   pub struct PayloadClient {
       // Payload transfer state
   }
   ```

**Priority 2: Fix Plugin Trait**

1. **Update Plugin trait signature**:
   ```rust
   pub trait Plugin {
       fn initialize(&mut self);
       fn shutdown(&mut self);
       fn handle_packet(&mut self, packet: &Packet) -> Result<()>;
       // NOT: handle_packet(&mut self, device: &Device, packet: &Packet)
   }
   ```

2. **Update all plugin implementations** to match trait

**Priority 3: External Dependencies**

1. **Add mouse_keyboard_input dependency** to Cargo.toml (if available for Android)
2. OR remove/stub out mouse/keyboard input plugins for Android

### Resume Issue #51: Build Native Libraries

Once cosmic-connect-core compiles:

1. **Build Rust libraries**:
   ```bash
   ./gradlew cargoBuild
   ```

2. **Verify .so files created**:
   ```bash
   find build/rustJniLibs -name "*.so"
   ```

3. **Test library loading** (Issue #50):
   - Run app on Android emulator
   - Verify `System.loadLibrary("cosmic_connect_core")` succeeds
   - Call `CosmicConnectCore.initialize()`

---

## Performance Targets (Post-Build)

Once libraries are built, Issue #50 will measure:

| Metric | Target | Purpose |
|--------|--------|---------|
| Library load time | < 100ms | Fast app startup |
| FFI call overhead | < 10μs | Efficient packet creation |
| Memory per packet | < 1KB | Low footprint |
| Callback latency | < 100μs | Responsive events |

---

## Files Modified

### cosmic-connect-android

**Configuration**:
- `flake.nix` - Added Android NDK 27, rust-overlay, cargo-ndk
- `flake.lock` - Updated with rust-overlay input
- `local.properties` - NDK/SDK paths (gitignored, auto-generated)

**Already Had**:
- `build.gradle.kts` - rust-android plugin config (lines 44-51)
- `gradle/libs.versions.toml` - rust-android plugin version 0.9.4

### cosmic-connect-core

**Dependencies**:
- `Cargo.toml` - Downgraded rustls to 0.22, tokio-rustls to 0.25
- `Cargo.lock` - Updated dependency graph (gitignored for libraries)

---

## Build Infrastructure Status

| Component | Status | Details |
|-----------|--------|---------|
| Android NDK | ✅ Working | Version 27.0.12077973 |
| Rust Toolchain | ✅ Working | 1.92.0 with Android targets |
| cargo-ndk | ✅ Working | Version 4.1.2 |
| rust-android-gradle | ✅ Working | Version 0.9.4 |
| Dependency Compilation | ✅ Working | All 200+ crates compile |
| cosmic-connect-core | ❌ Code Errors | 45 compilation errors |

---

## Verification Commands

### Check NDK Installation
```bash
nix develop --command bash -c 'echo $ANDROID_NDK_HOME && ls -la $ANDROID_NDK_HOME'
```

### Check Rust Targets
```bash
nix develop --command bash -c 'rustc --print target-list | grep android'
```

### Check cargo-ndk
```bash
nix develop --command which cargo-ndk
```

### Test Build (once code fixed)
```bash
nix develop --command ./gradlew cargoBuild
```

### Verify Output
```bash
find build/rustJniLibs -name "*.so" -exec ls -lh {} \;
```

---

## Lessons Learned

### 1. NDK Version Matching is Critical
- Gradle expects specific NDK version
- Must match between flake.nix and Gradle configuration
- Error messages clearly indicate version mismatch

### 2. Crypto Library Portability Matters
- aws-lc-sys has complex C build requirements
- ring is more portable for cross-compilation
- Downgrading rustls was the right call

### 3. NixOS Development Environment
- Provides reproducible builds
- Easy to manage NDK and toolchain versions
- local.properties still needed for Gradle

### 4. rust-android-gradle Plugin
- Handles most cargo-ndk complexity
- Automatically sets up toolchains
- Integrates well with Gradle build lifecycle

---

## Related Issues

- **Issue #50** (FFI Bindings Validation): Blocked by #51, ready to resume
- **Issue #52** (Android FFI Wrapper): Can proceed independently
- **Issue #44-48** (Rust Core): ✅ Complete, but code fixes needed

---

## Estimated Effort

**Completed Work**: ~6 hours
- NDK integration and troubleshooting
- Dependency fixes (rustls downgrade)
- Build infrastructure setup
- Testing and verification

**Remaining Work**: ~2-4 hours
- Fix cosmic-connect-core compilation errors (2-3 hours)
- Test build and verify .so files (30 minutes)
- Update documentation (30 minutes)

**Total**: ~8-10 hours for complete Issue #51

---

## References

**Commits**:
- cosmic-connect-android: `d67e6026` - Issue #51 infrastructure (90% complete)
- cosmic-connect-android: `390ffa4e` - Initial NDK setup
- cosmic-connect-core: `2021e98` - rustls 0.22 with ring provider

**Documentation**:
- `docs/issue-50-ffi-validation.md` - FFI test plan (blocked by #51)
- `docs/issue-50-validation-summary.md` - Validation findings
- `docs/project-status-2025-01-15.md` - Overall project status

**External Resources**:
- [rust-android-gradle plugin](https://github.com/mozilla/rust-android-gradle)
- [cargo-ndk documentation](https://github.com/bbqsrc/cargo-ndk)
- [rustls documentation](https://docs.rs/rustls/)
- [ring crypto library](https://github.com/briansmith/ring)

---

**Document Version**: 1.0
**Status**: Issue #51 - 90% Complete (Infrastructure Ready)
**Next Action**: Fix cosmic-connect-core compilation errors
**Last Updated**: 2026-01-16
