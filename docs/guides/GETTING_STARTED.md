# Getting Started with COSMIC Connect Android

This guide will help you set up your development environment to build and run **COSMIC Connect Android**.

**Current Version:** 1.0.0-beta
**Architecture:** Hybrid Rust (Core) + Kotlin (UI)

---

## Prerequisites

To build this project, you need:
1.  **Git** (to clone the repository)
2.  **Android Studio** (Ladybug or later)
3.  **Rust Toolchain** (1.84+)
4.  **Android NDK** (Version 27.0.12077973)
5.  **JDK 17** or later

---

## 🚀 Setup Option 1: NixOS (Recommended)

If you are using NixOS, the development environment is fully automated via `flake.nix`.

1.  **Clone the repositories:**
    ```bash
    git clone https://github.com/olafkfreund/cosmic-connect-android
    git clone https://github.com/olafkfreund/cosmic-connect-core
    cd cosmic-connect-android
    ```

2.  **Enter the environment:**
    ```bash
    nix develop
    # Or if you use direnv:
    direnv allow
    ```

3.  **Build:**
    ```bash
    # Build native libraries (Rust)
    ./gradlew cargoBuild

    # Build Android APK
    ./gradlew assembleDebug
    ```

---

## 🛠️ Setup Option 2: Manual Setup (Linux/macOS/Windows)

### 1. Install Rust & Android Targets
```bash
# Install Rust
curl --proto='=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Add Android targets
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

# Install cargo-ndk (required for building native libs)
cargo install cargo-ndk
```

### 2. Install Android NDK
1.  Open Android Studio.
2.  Go to **Tools > SDK Manager > SDK Tools**.
3.  Check **NDK (Side by side)** and install version **27.0.12077973**.
4.  Set `ANDROID_NDK_HOME` environment variable:
    ```bash
    export ANDROID_NDK_HOME=$HOME/Android/Sdk/ndk/27.0.12077973
    ```

### 3. Clone Repositories
You need both the Android app and the Core library. They should be sibling directories.

```bash
mkdir cosmic-connect
cd cosmic-connect
git clone https://github.com/olafkfreund/cosmic-connect-android
git clone https://github.com/olafkfreund/cosmic-connect-core
```

### 4. Build the Project
```bash
cd cosmic-connect-android

# 1. Build Rust Core (Native Libraries)
./gradlew cargoBuild

# 2. Build Android App
./gradlew assembleDebug
```

The APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📱 Running the App

### On a Real Device
1.  Enable **Developer Options** and **USB Debugging** on your Android phone.
2.  Connect your phone via USB.
3.  Run:
    ```bash
    ./gradlew installDebug
    ```

### On Waydroid (Linux)
If you use Waydroid for testing:
```bash
# Install the APK
waydroid app install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
waydroid app launch org.cosmic.cosmicconnect
```

---

## 🧪 Running Tests

### Unit Tests
Runs standard Kotlin unit tests and FFI validation tests.
```bash
./gradlew test
```

### Integration Tests
Requires a connected device or emulator (e.g., Waydroid).
```bash
./gradlew connectedAndroidTest
```

---

## 📂 Project Structure

*   **`app/src/main/java/`**: Kotlin source code (UI, ViewModels, Hilt modules).
*   **`app/src/main/res/`**: Android resources (Layouts, Strings, Drawables).
*   **`cosmic-connect-core/`**: (Sibling repo) Rust implementation of the protocol.
*   **`app/src/main/java/org/cosmic/cosmicconnect/Core/`**: FFI bridge code connecting Kotlin to Rust.

## 🤝 Contributing

Please read [CONTRIBUTING.md](../../CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.
