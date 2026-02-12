# Contributing to COSMIC Connect Android

**Thank you for your interest in contributing to COSMIC Connect!** 🎉

We welcome contributions of all kinds: code, documentation, bug reports, feature requests, translations, and more. This document will guide you through the process.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Guidelines](#code-guidelines)
- [Testing](#testing)
- [Documentation](#documentation)
- [Submitting Changes](#submitting-changes)
- [Community](#community)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of experience level, background, or identity.

### Our Standards

**Positive behaviors:**
- Being respectful and considerate
- Welcoming newcomers
- Accepting constructive criticism
- Focusing on what's best for the project
- Showing empathy towards others

**Unacceptable behaviors:**
- Harassment or discrimination
- Trolling or insulting comments
- Personal or political attacks
- Publishing others' private information
- Unprofessional conduct

### Enforcement

Project maintainers will enforce these standards and may take action including warning, temporary ban, or permanent ban for violations.

---

## How Can I Contribute?

### 🐛 Reporting Bugs

Found a bug? Help us fix it!

**Before submitting:**
1. Check [existing issues](https://github.com/olafkfreund/cosmic-connect-android/issues) to avoid duplicates
2. Verify you're using the latest version
3. Test on a clean installation if possible

**When submitting:**
- Use the bug report template
- Include clear steps to reproduce
- Provide system information (Android version, device model)
- Attach logs if available (Settings → Advanced → Debug Logging)
- Include screenshots/videos if helpful

**Good bug report example:**
```
**Bug:** File transfer fails for files > 100MB

**Steps to reproduce:**
1. Pair devices
2. Share a file > 100MB from Android
3. Transfer starts but fails at 50%

**Expected:** File transfers successfully
**Actual:** Transfer fails with "Connection lost" error

**Environment:**
- Android 14 (Pixel 8)
- COSMIC Connect 1.0.0-beta
- COSMIC Desktop (Pop!_OS 24.04)
- WiFi 5GHz

**Logs:** [attached]
```

### 💡 Suggesting Features

Have an idea? We'd love to hear it!

**Before suggesting:**
1. Check if it already exists
2. Search [GitHub Discussions](https://github.com/olafkfreund/cosmic-connect-android/discussions) and Issues
3. Consider if it fits the project scope

**When suggesting:**
- Use the feature request template
- Explain the problem it solves
- Describe your proposed solution
- Consider implementation complexity
- Be open to feedback and alternatives

**Good feature request example:**
```
**Feature:** Automatic clipboard sync with configurable delay

**Problem:**
Current clipboard sync is instant, which can be annoying when
copying passwords or sensitive data.

**Proposed Solution:**
Add a setting to delay clipboard sync by N seconds (default: 2s)
with option to disable sync for next clipboard item.

**Use Cases:**
- Copying passwords without syncing
- Copying work-in-progress text
- Privacy-sensitive copying

**Implementation Notes:**
- Setting in Clipboard plugin config
- Timer-based delay mechanism
- Optional notification to cancel sync
```

### 📝 Improving Documentation

Documentation improvements are always welcome!

**What to document:**
- Fix typos or unclear instructions
- Add missing information
- Update outdated content
- Create tutorials or guides
- Improve code comments
- Translate to other languages (future)

**Where documentation lives:**
- `docs/` - User and developer guides
- `README.md` - Project overview
- Code comments - In-code documentation
- Wiki (future) - Community knowledge base

### 🔧 Contributing Code

Ready to code? Great! See [Development Workflow](#development-workflow) below.

### 🌍 Translating

**Note:** Localization support is planned but not yet implemented.

When ready, translations will be managed through:
- Android strings.xml files
- Crowdin or similar platform
- Community contributors

Stay tuned for updates!

---

## Getting Started

### Prerequisites

**Required:**
- Git
- Android Studio (latest stable)
- JDK 17+
- Rust 1.84+ with Android targets
- Android NDK 27+
- cargo-ndk 4.1+

**Optional but recommended:**
- NixOS with flakes (automatic environment)
- COSMIC Desktop for testing

### Setting Up Development Environment

#### Option 1: NixOS (Recommended)

```bash
# Clone repository
git clone https://github.com/olafkfreund/cosmic-connect-android.git
cd cosmic-connect-android

# Enter dev environment (installs everything automatically)
nix develop

# Build
./gradlew assembleDebug
```

#### Option 2: Manual Setup

**1. Install Rust and Android targets:**
```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Add Android targets
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android

# Install cargo-ndk
cargo install cargo-ndk
```

**2. Set up Android:**
```bash
# Install Android Studio
# Download from https://developer.android.com/studio

# Install NDK via SDK Manager in Android Studio
# Or set ANDROID_NDK_ROOT environment variable
export ANDROID_NDK_ROOT=$HOME/Android/Sdk/ndk/27.0.12077973
```

**3. Clone and build:**
```bash
git clone https://github.com/olafkfreund/cosmic-connect-android.git
cd cosmic-connect-android

# Build Rust libraries
./gradlew cargoBuild

# Build Android app
./gradlew assembleDebug
```

### Repository Structure

```
cosmic-connect-android/
├── src/                          # Android source code
│   ├── main/java/.../            # Kotlin source
│   │   ├── ui/                   # Jetpack Compose UI
│   │   ├── plugins/              # Plugin implementations
│   │   ├── ffi/                  # FFI wrappers
│   │   └── ...
│   ├── androidTest/              # Instrumentation tests
│   └── test/                     # Unit tests
├── docs/                         # Documentation
├── build.gradle.kts              # Build configuration
└── README.md                     # Project overview

External dependency:
cosmic-connect-core/              # Shared Rust library
```

### Building the Project

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Install on connected device
./gradlew installDebug

# Build everything
./gradlew build
```

---

## Development Workflow

### 1. Fork and Clone

```bash
# Fork the repository on GitHub first

# Clone your fork
git clone https://github.com/YOUR_USERNAME/cosmic-connect-android.git
cd cosmic-connect-android

# Add upstream remote
git remote add upstream https://github.com/olafkfreund/cosmic-connect-android.git
```

### 2. Create a Branch

```bash
# Update main branch
git checkout master
git pull upstream master

# Create feature branch
git checkout -b feature/your-feature-name

# Or bug fix branch
git checkout -b fix/bug-description
```

**Branch naming conventions:**
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test additions/improvements
- `chore/` - Maintenance tasks

### 3. Make Changes

**Best practices:**
- Make atomic commits (one logical change per commit)
- Write clear commit messages
- Test your changes thoroughly
- Follow code style guidelines
- Update documentation as needed

### 4. Commit Your Changes

```bash
# Stage changes
git add .

# Commit with descriptive message
git commit -m "Add feature: clipboard sync delay setting"

# Or use conventional commits format
git commit -m "feat(clipboard): add configurable sync delay

- Add delay setting in plugin config
- Default 2 second delay
- Add notification to cancel sync
- Update tests for new behavior

Fixes #123"
```

**Commit message format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `style` - Code style (formatting, etc.)
- `refactor` - Code refactoring
- `test` - Adding tests
- `chore` - Maintenance

### 5. Keep Your Branch Updated

```bash
# Fetch upstream changes
git fetch upstream

# Rebase on upstream master
git rebase upstream/master

# Resolve conflicts if any
# Then continue
git rebase --continue

# Force push to your fork
git push --force-with-lease origin feature/your-feature-name
```

### 6. Submit Pull Request

See [Submitting Changes](#submitting-changes) below.

---

## Code Guidelines

### Kotlin Style

**Follow official Kotlin conventions:**
- Use 4 spaces for indentation
- Braces on same line
- Clear, descriptive names
- Prefer immutability (`val` over `var`)
- Use type inference where obvious

**Example:**
```kotlin
// Good
class DeviceManager(
    private val context: Context,
    private val cosmicConnect: CosmicConnect
) {
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    fun addDevice(device: Device) {
        _devices.update { it + device }
    }
}

// Avoid
class devicemanager(c: Context, cc: CosmicConnect) {
    var devices = mutableListOf<Device>()
    fun add(d: Device) { devices.add(d) }
}
```

### Jetpack Compose Guidelines

**Follow Compose best practices:**
- State hoisting
- Unidirectional data flow
- Reusable composables
- Proper preview annotations

**Example:**
```kotlin
@Composable
fun DeviceCard(
    device: Device,
    onPairClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onPairClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = device.ipAddress,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview
@Composable
private fun DeviceCardPreview() {
    MaterialTheme {
        DeviceCard(
            device = Device(id = "1", name = "Test", ipAddress = "192.168.1.1"),
            onPairClick = {}
        )
    }
}
```

### FFI Guidelines

**When working with Rust FFI:**
- Keep FFI layer thin
- Handle errors properly
- Document memory ownership
- Test FFI boundaries thoroughly

**Example:**
```kotlin
object BatteryPacketsFFI {
    /**
     * Create a battery status packet.
     *
     * @param deviceId The device ID
     * @param level Battery level (0-100)
     * @param isCharging Whether device is charging
     * @return NetworkPacket handle (must be freed)
     */
    fun createBatteryPacket(
        deviceId: String,
        level: Int,
        isCharging: Boolean
    ): NetworkPacket {
        require(level in 0..100) { "Battery level must be 0-100" }

        return createBatteryPacketNative(deviceId, level, isCharging)
            ?: throw IllegalStateException("Failed to create battery packet")
    }

    private external fun createBatteryPacketNative(
        deviceId: String,
        level: Int,
        isCharging: Boolean
    ): NetworkPacket?
}
```

### Code Quality Tools

**Run before committing:**
```bash
# Kotlin linting
./gradlew ktlintCheck

# Auto-format
./gradlew ktlintFormat

# Detekt static analysis
./gradlew detekt

# All checks
./gradlew check
```

---

## Testing

### Writing Tests

**Every change should include tests:**
- Unit tests for logic
- Integration tests for flows
- UI tests for Compose components
- Performance tests for critical paths

### Test Structure

```kotlin
class DeviceManagerTest {
    private lateinit var deviceManager: DeviceManager
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mock()
        deviceManager = DeviceManager(mockContext)
    }

    @Test
    fun `addDevice should add device to list`() {
        // Given
        val device = Device(id = "1", name = "Test")

        // When
        deviceManager.addDevice(device)

        // Then
        val devices = deviceManager.devices.value
        assertEquals(1, devices.size)
        assertEquals(device, devices.first())
    }

    @After
    fun tearDown() {
        // Cleanup
    }
}
```

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests DeviceManagerTest

# All instrumentation tests
./gradlew connectedAndroidTest

# Specific instrumentation test
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.cosmicext.connect.integration.DiscoveryIntegrationTest

# Test with coverage
./gradlew testDebugUnitTestCoverage
```

### Test Coverage

**Aim for:**
- 80%+ overall coverage
- 90%+ for critical paths
- 100% for FFI interfaces

**Check coverage:**
```bash
./gradlew jacocoTestReport
open app/build/reports/jacoco/test/html/index.html
```

---

## Documentation

### Code Documentation

**Document:**
- Public APIs
- Complex logic
- FFI interfaces
- Non-obvious decisions

**Don't document:**
- Obvious code
- Getters/setters
- Self-explanatory functions

**Example:**
```kotlin
/**
 * Manages device discovery and pairing.
 *
 * Handles UDP broadcast for device discovery, certificate exchange
 * during pairing, and maintains paired device state.
 *
 * Thread-safe: All operations use synchronized access.
 */
class DeviceManager {
    /**
     * Pairs with a device.
     *
     * Initiates TLS handshake and certificate exchange. The pairing
     * process requires acceptance on both devices.
     *
     * @param device The device to pair with
     * @throws PairingException if pairing fails
     */
    suspend fun pairDevice(device: Device) {
        // Implementation
    }
}
```

### Updating Documentation

**When changing code, update:**
- Code comments
- README if behavior changes
- User Guide if user-facing
- API docs if interface changes
- CHANGELOG.md for user-visible changes

---

## Submitting Changes

### Before Submitting

**Checklist:**
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] New tests added for new functionality
- [ ] Code follows style guidelines
- [ ] Documentation updated
- [ ] Commit messages are clear
- [ ] Branch is up to date with master

**Run final checks:**
```bash
./gradlew clean build
./gradlew test
./gradlew connectedAndroidTest
./gradlew ktlintCheck
./gradlew detekt
```

### Creating Pull Request

1. **Push to your fork:**
```bash
git push origin feature/your-feature-name
```

2. **Open PR on GitHub:**
- Go to your fork on GitHub
- Click "Pull Request"
- Select your branch
- Fill out the PR template
- Submit

3. **PR Template will guide you through:**
- Description of changes
- Related issues
- Type of change
- Testing performed
- Checklist

### PR Review Process

**What happens next:**
1. **Automated checks run:**
   - Build verification
   - Test suite
   - Code quality checks

2. **Maintainers review:**
   - Code quality
   - Architecture fit
   - Test coverage
   - Documentation

3. **Feedback provided:**
   - Requested changes
   - Questions
   - Suggestions

4. **You respond:**
   - Make requested changes
   - Answer questions
   - Explain decisions

5. **Approval and merge:**
   - Once approved, maintainers merge
   - Your changes are in!

### After Merge

- Delete your branch (optional)
- Update your fork
- Celebrate! 🎉

---

## Community

### Communication Channels

**GitHub:**
- [Issues](https://github.com/olafkfreund/cosmic-connect-android/issues) - Bug reports, feature requests
- [Discussions](https://github.com/olafkfreund/cosmic-connect-android/discussions) - Questions, ideas, general chat
- [Pull Requests](https://github.com/olafkfreund/cosmic-connect-android/pulls) - Code contributions

**Reddit:**
- r/pop_os - Pop!_OS and COSMIC Desktop
- r/CosmicDE - COSMIC Desktop specific

**Matrix/Discord:**
- Links available on GitHub repository

### Getting Help

**Stuck? Ask for help!**
- GitHub Discussions for questions
- Issues for bugs
- Community channels for general help

**Tips for getting help:**
- Explain what you're trying to do
- Share relevant code/errors
- Describe what you've tried
- Be patient and respectful

### Recognition

**Contributors are recognized:**
- GitHub Contributors page
- CHANGELOG.md mentions
- Release notes
- Project README

---

## License

By contributing, you agree that your contributions will be licensed under the GPL-3.0 License, the same license as COSMIC Connect Android.

---

## Questions?

**Still have questions?**
- Check the [FAQ](docs/FAQ.md)
- Ask in [GitHub Discussions](https://github.com/olafkfreund/cosmic-connect-android/discussions)
- Open an issue

---

## Thank You! 🙏

Your contributions make COSMIC Connect better for everyone. Whether you're fixing a typo, adding a feature, or helping other users, every contribution matters.

**Happy coding!** 🚀

---

**Last Updated:** 2026-01-17
**For more information, see:**
- [User Guide](docs/USER_GUIDE.md)
- [Testing Guide](docs/TESTING.md)
- [Privacy Policy](docs/PRIVACY_POLICY.md)
- [Project Plan](docs/guides/PROJECT_PLAN.md)
