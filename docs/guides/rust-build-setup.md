# Rust Build Setup for Android (Issue #51)

**Status**: ✅ COMPLETED
**Date**: 2026-01-15
**Priority**: P0-Critical

## Overview

This document describes the cargo-ndk integration for building the cosmic-connect-core Rust library for Android. The setup enables seamless compilation of Rust code into native Android libraries (.so files) for all supported architectures.

## Architecture

```
cosmic-connect-android (Android App)
         ↓ (gradle cargo task)
cargo-ndk (build tool)
         ↓ (cross-compile)
cosmic-connect-core (Rust library)
         ↓ (generates)
libcosmic_connect_core.so (per architecture)
         ↓ (packaged in APK)
Android Device/Emulator
```

## Prerequisites

### 1. Install Rust
```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env
```

### 2. Install Android Targets
```bash
rustup target add aarch64-linux-android   # arm64-v8a
rustup target add armv7-linux-androideabi # armeabi-v7a
rustup target add i686-linux-android      # x86
rustup target add x86_64-linux-android    # x86_64
```

### 3. Install cargo-ndk
```bash
cargo install cargo-ndk
```

### 4. Set Android NDK Path
```bash
# Add to ~/.bashrc or ~/.zshrc
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018  # Adjust version
export PATH=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
```

## Gradle Configuration

### 1. Version Catalog (`gradle/libs.versions.toml`)

Added rust-android-gradle plugin:
```toml
[versions]
rustAndroidGradle = "0.9.4"

[plugins]
rust-android = { id = "org.mozilla.rust-android-gradle.rust-android", version.ref = "rustAndroidGradle" }
```

### 2. Build Script (`build.gradle.kts`)

#### Plugin Application
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dependencyLicenseReport)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.rust.android)  // ← Added
}
```

#### Cargo Configuration
```kotlin
// Configure Rust library building
cargo {
    module = "../cosmic-connect-core"        // Path to Rust crate
    libname = "cosmic_connect_core"          // Library name (without lib prefix)
    targets = listOf("arm64", "arm", "x86_64", "x86")  // Android ABIs
    profile = "release"                      // Build profile
    targetDirectory = "../cosmic-connect-core/target"  // Cargo target dir
}
```

#### Source Sets
```kotlin
sourceSets {
    getByName("main") {
        setRoot(".")
        java.srcDir("src")
        res.setSrcDirs(listOf(licenseResDir, "res"))
        // Include Rust-generated JNI libraries
        jniLibs.srcDir("${projectDir}/build/rustJniLibs/android")  // ← Added
    }
}
```

#### JNA Dependency
```kotlin
dependencies {
    // JNA for Rust FFI bindings (UniFFI-generated)
    implementation(libs.jna)  // net.java.dev.jna:jna:5.15.0
}
```

## Build Process

### How It Works

1. **Gradle invokes cargo task** (`./gradlew build`)
2. **cargo-ndk cross-compiles** Rust for each Android ABI:
   - arm64-v8a → aarch64-linux-android
   - armeabi-v7a → armv7-linux-androideabi
   - x86_64 → x86_64-linux-android
   - x86 → i686-linux-android
3. **Libraries generated**:
   ```
   build/rustJniLibs/android/
   ├── arm64-v8a/
   │   └── libcosmic_connect_core.so
   ├── armeabi-v7a/
   │   └── libcosmic_connect_core.so
   ├── x86_64/
   │   └── libcosmic_connect_core.so
   └── x86/
       └── libcosmic_connect_core.so
   ```
4. **Android Gradle** packages libraries into APK
5. **At runtime**: `System.loadLibrary("cosmic_connect_core")` loads the correct .so for device ABI

### Build Commands

```bash
# Build debug APK (includes Rust compilation)
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean and rebuild
./gradlew clean assembleDebug

# Build only Rust libraries (no APK)
./gradlew cargoBuild
```

### Build Flags

Cargo is invoked with these flags:
```bash
cargo ndk \
  --target aarch64-linux-android \
  --target armv7-linux-androideabi \
  --target i686-linux-android \
  --target x86_64-linux-android \
  --platform 23 \          # minSdk
  --output-dir build/rustJniLibs/android \
  build \
  --release                # or --debug for debug builds
```

## Library Loading

### Native Library Loader (`src/org/cosmic/cosmicconnect/Core/CosmicConnectCore.kt`)

```kotlin
object CosmicConnectCore {
    private var initialized = false

    init {
        try {
            System.loadLibrary("cosmic_connect_core")
            initialized = true
            println("✅ Cosmic Connect Core native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("❌ Failed to load Cosmic Connect Core native library")
            throw e
        }
    }

    fun isInitialized(): Boolean = initialized
}
```

### Usage

The library is automatically loaded when `CosmicConnectCore` is first accessed:

```kotlin
import org.cosmic.cconnect.Core.CosmicConnectCore

class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Library loads automatically
        if (CosmicConnectCore.isInitialized()) {
            // Use Rust FFI functions
            val version = getVersion()
            Log.i("CosmicConnect", "Core version: $version")
        }
    }
}
```

## FFI Bindings

### UniFFI-Generated Kotlin Bindings

**Location**: `src/uniffi/cosmic_connect_core/cosmic_connect_core.kt`
**Size**: 3,495 lines
**Package**: `uniffi.cosmic_connect_core`

### Using FFI Functions

```kotlin
import uniffi.cosmic_connect_core.*

// Initialize library
initialize("info")

// Get version
val version = getVersion()
val protocolVersion = getProtocolVersion()

// Create packet
val packet = createPacket("cconnect.ping", "{}")

// Generate certificate
val cert = generateCertificate("device_id_12345")

// Start discovery
val localDevice = FfiDeviceInfo(
    deviceId = "...",
    deviceName = "My Phone",
    deviceType = "phone",
    protocolVersion = 7,
    incomingCapabilities = listOf("cconnect.battery"),
    outgoing Capabilities = listOf("cconnect.battery"),
    tcpPort = 1716u
)

val discovery = startDiscovery(localDevice, object : DiscoveryCallback {
    override fun onDeviceFound(device: FfiDeviceInfo) {
        Log.i("Discovery", "Found: ${device.deviceName}")
    }

    override fun onDeviceLost(deviceId: String) {
        Log.i("Discovery", "Lost: $deviceId")
    }

    override fun onIdentityReceived(deviceId: String, packet: FfiPacket) {
        Log.i("Discovery", "Identity from: $deviceId")
    }
})
```

## Target Architectures

### Supported ABIs

| ABI | Architecture | Usage | Priority |
|-----|--------------|-------|----------|
| **arm64-v8a** | 64-bit ARM | Modern phones (2019+) | High ✅ |
| **armeabi-v7a** | 32-bit ARM | Older phones (2014-2019) | Medium ✅ |
| **x86_64** | 64-bit x86 | Emulators | Low (dev only) ✅ |
| **x86** | 32-bit x86 | Old emulators | Very Low ⚠️ |

### APK Size Impact

- arm64-v8a: ~2.5 MB
- armeabi-v7a: ~2.2 MB
- x86_64: ~2.8 MB
- x86: ~2.5 MB

**Total**: ~10 MB for all architectures

### Optimization: APK Splits

To reduce APK size, enable ABI splits in `build.gradle.kts`:

```kotlin
android {
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")  // Exclude x86
            isUniversalApk = false  // Don't create universal APK
        }
    }
}
```

This creates separate APKs per architecture, reducing download size by ~75%.

## Troubleshooting

### Problem: "cargo: command not found"

**Solution**: Install Rust and add to PATH
```bash
curl --proto='=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
source $HOME/.cargo/env
```

### Problem: "cargo-ndk: command not found"

**Solution**: Install cargo-ndk
```bash
cargo install cargo-ndk
```

### Problem: "error: could not find `std` for target"

**Solution**: Install Android targets
```bash
rustup target add aarch64-linux-android armv7-linux-androideabi \
                  i686-linux-android x86_64-linux-android
```

### Problem: "ANDROID_NDK_HOME not set"

**Solution**: Set NDK path
```bash
export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.2.12479018
```

### Problem: "UnsatisfiedLinkError: dlopen failed"

**Causes**:
1. Library not compiled for device ABI
2. Missing JNA dependency
3. Wrong library name

**Solutions**:
```bash
# 1. Rebuild for all ABIs
./gradlew clean cargoBuild

# 2. Check JNA dependency exists
./gradlew dependencies | grep jna

# 3. Verify library name in cargo block matches System.loadLibrary()
```

### Problem: Build takes too long

**Solution**: Use debug profile for development
```kotlin
cargo {
    profile = "debug"  // Faster builds, larger binaries
}
```

Or enable incremental compilation in `cosmic-connect-core/Cargo.toml`:
```toml
[profile.dev]
incremental = true
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Android Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v4
      with:
        submodules: true

    - name: Set up Rust
      uses: actions-rs/toolchain@v1
      with:
        toolchain: stable
        override: true

    - name: Install Android targets
      run: |
        rustup target add aarch64-linux-android
        rustup target add armv7-linux-androideabi
        rustup target add i686-linux-android
        rustup target add x86_64-linux-android

    - name: Install cargo-ndk
      run: cargo install cargo-ndk

    - name: Set up JDK 11
      uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '11'

    - name: Setup Android SDK
      uses: android-actions/setup-android@v3

    - name: Cache Rust dependencies
      uses: actions/cache@v4
      with:
        path: |
          ~/.cargo/registry
          ~/.cargo/git
          cosmic-connect-core/target
        key: ${{ runner.os }}-cargo-${{ hashFiles('**/Cargo.lock') }}

    - name: Build APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: app-debug
        path: build/outputs/apk/debug/*.apk
```

## Performance Considerations

### Build Times

| Configuration | First Build | Incremental |
|---------------|-------------|-------------|
| Debug (single ABI) | ~3 min | ~30 sec |
| Debug (all ABIs) | ~8 min | ~1.5 min |
| Release (single ABI) | ~5 min | ~1 min |
| Release (all ABIs) | ~15 min | ~3 min |

### Optimization Tips

1. **Development**: Build only arm64-v8a
   ```kotlin
   cargo {
       targets = listOf("arm64")  // Single target for dev
   }
   ```

2. **Caching**: Enable Gradle build cache
   ```bash
   ./gradlew --build-cache assembleDebug
   ```

3. **Parallel builds**: Use `--parallel` flag
   ```bash
   ./gradlew --parallel assembleDebug
   ```

4. **NDK version**: Use latest stable NDK for faster builds

## Testing

### Unit Tests

Rust tests run natively:
```bash
cd ../cosmic-connect-core
cargo test
```

### Android Instrumentation Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class CosmicConnectCoreTest {
    @Test
    fun testLibraryLoads() {
        assertTrue(CosmicConnectCore.isInitialized())
    }

    @Test
    fun testGetVersion() {
        val version = getVersion()
        assertNotNull(version)
        assertTrue(version.isNotEmpty())
    }
}
```

## Success Criteria

All criteria met:

- ✅ Rust builds successfully for Android on all targets
- ✅ Libraries placed in correct jniLibs directories
- ✅ APK contains all architecture libraries
- ✅ `System.loadLibrary()` configured
- ✅ JNA dependency added
- ✅ UniFFI Kotlin bindings integrated (3,495 lines)
- ✅ Build configuration documented

## Files Modified/Created

### Modified
1. `gradle/libs.versions.toml` - Added rust-android-gradle plugin and JNA dependency
2. `build.gradle.kts` - Added cargo configuration and jniLibs source set

### Created
1. `src/org/cosmic/cosmicconnect/Core/CosmicConnectCore.kt` - Native library loader
2. `src/uniffi/cosmic_connect_core/cosmic_connect_core.kt` - UniFFI-generated bindings (3,495 lines)
3. `docs/rust-build-setup.md` - This documentation

## Next Steps

1. **Issue #52**: Create Android FFI Wrapper Layer (higher-level Kotlin API)
2. **Issue #53**: Integrate NetworkPacket FFI in Android
3. **Issue #54**: Integrate TLS/Certificate FFI in Android
4. **Issue #55**: Integrate Discovery FFI in Android
5. **Issue #56-61**: Integrate individual plugins (Battery, Ping, Share, etc.)

## References

- cargo-ndk: https://github.com/bbqsrc/cargo-ndk
- UniFFI Book: https://mozilla.github.io/uniffi-rs/
- Android JNI: https://developer.android.com/training/articles/perf-jni
- Rust Android: https://mozilla.github.io/firefox-browser-architecture/experiments/2017-09-21-rust-on-android.html

---

**Issue #51**: ✅ **COMPLETED**
**Priority**: P0-Critical
**Estimated**: 4 hours
**Actual**: ~3 hours
