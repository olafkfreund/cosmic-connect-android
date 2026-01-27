# Issue #28: Set Up Integration Test Framework

**Created:** 2026-01-17
**Status:** ✅ Completed
**Effort:** 8 hours
**Phase:** 4.4 - Testing

## Overview

This document details the setup of comprehensive integration testing infrastructure for COSMIC Connect Android with Rust FFI testing support. The framework enables testing of:

1. **FFI Layer** - Android ↔ Rust bindings via uniffi
2. **Integration Flows** - Complete Android → Rust → Network flows
3. **Compose UI** - Screen and component testing
4. **End-to-End** - Full Android ↔ COSMIC Desktop scenarios

## Components Implemented

### 1. TestUtils

Core testing utilities for all test types.

**File:** `src/androidTest/java/org/cosmic/cosmicconnect/test/TestUtils.kt`

**Features:**
- Application context access
- Conditional waiting with timeout
- UI thread synchronization
- Test data cleanup
- Random ID/name generation

**Key Methods:**
```kotlin
// Wait for condition
TestUtils.waitFor(timeoutMs = 5000) { condition() }

// Wait for latch
TestUtils.waitForLatch(latch, timeoutMs = 5000)

// Run on UI thread
TestUtils.runOnUiThreadBlocking { /* code */ }

// Generate test data
TestUtils.randomDeviceId()
TestUtils.randomTestName()

// Cleanup
TestUtils.cleanupTestData()
```

### 2. MockFactory

Factory for creating test data and mock objects.

**File:** `src/androidTest/java/org/cosmic/cosmicconnect/test/MockFactory.kt`

**Features:**
- NetworkPacket creation via FFI
- All COSMIC Connect packet types
- Mock device information
- Mock plugin information
- Test device lists

**Packet Types Supported:**
```kotlin
// Identity packets
MockFactory.createIdentityPacket(deviceId, deviceName, deviceType)

// Pairing packets
MockFactory.createPairRequestPacket(deviceId)
MockFactory.createPairResponsePacket(deviceId, accepted = true)

// Plugin packets
MockFactory.createBatteryPacket(deviceId, batteryLevel = 75, isCharging = false)
MockFactory.createClipboardPacket(deviceId, content = "text")
MockFactory.createSharePacket(deviceId, filename = "test.txt")
MockFactory.createPingPacket(deviceId, message = "Hello")
MockFactory.createRunCommandPacket(deviceId, key = "cmd", command = "echo test")
```

**Mock Data Creation:**
```kotlin
// Device info for UI tests
MockFactory.createMockDeviceInfo(
  deviceId = "test_123",
  deviceName = "Test Phone",
  isPaired = true,
  isReachable = true,
  batteryLevel = 75
)

// Device list
MockFactory.createMockDeviceList(count = 3)

// Plugin info
MockFactory.createMockPluginInfo(
  pluginKey = "battery",
  pluginName = "Battery Monitor",
  isEnabled = true
)

// Plugin list
MockFactory.createMockPluginList()
```

### 3. ComposeTestUtils

Utilities for testing Jetpack Compose UI.

**File:** `src/androidTest/java/org/cosmic/cosmicconnect/test/ComposeTestUtils.kt`

**Features:**
- Waiting for composables
- Common assertions
- Text input helpers
- Toggle/switch helpers
- Dialog helpers
- List scrolling

**Common Patterns:**
```kotlin
// Wait for elements
composeRule.waitForText("Device Name", timeoutMs = 5000)
composeRule.waitForContentDescription("Back button")
composeRule.waitForDialog()

// Assertions
composeRule.assertTextDisplayed("Settings")
composeRule.assertButtonEnabled("Pair")
composeRule.assertToggleOn("Bluetooth Support")
composeRule.assertLoading()
composeRule.assertError("Connection failed")

// Actions
composeRule.clickButton("Accept")
composeRule.clickListItem("Device Name")
composeRule.enterText("Device Name", "My Phone")
composeRule.toggleSwitch("Bluetooth Support")
composeRule.scrollToItem("Advanced")

// Dialogs
composeRule.confirmDialog("Accept")
composeRule.cancelDialog("Cancel")
composeRule.dismissDialog()
```

### 4. FfiTestUtils

Utilities for testing Rust FFI layer.

**File:** `src/androidTest/java/org/cosmic/cosmicconnect/test/FfiTestUtils.kt`

**Features:**
- FFI availability testing
- Certificate management
- Packet serialization testing
- Discovery service testing
- Performance benchmarking
- Thread safety testing
- Memory management verification

**FFI Testing Patterns:**
```kotlin
// Test FFI availability
FfiTestUtils.testFfiAvailable()

// Create test certificates
val certificate = FfiTestUtils.createTestCertificate()

// Test packet serialization
FfiTestUtils.testNetworkPacketSerialization(
  type = "cconnect.battery",
  body = mapOf("currentCharge" to 75)
)

// Test discovery service
FfiTestUtils.testDiscoveryService()

// Benchmark FFI overhead
val avgTime = FfiTestUtils.measureFfiOverhead(iterations = 1000)

// Test thread safety
FfiTestUtils.testFfiThreadSafety(threadCount = 10, iterations = 100)

// Test memory management
FfiTestUtils.testFfiMemoryManagement(iterations = 100)
```

## Test Layers

### Layer 1: FFI Unit Tests

Test individual FFI bindings from Android to Rust.

**Purpose:** Verify uniffi-generated bindings work correctly

**Examples:**
```kotlin
@Test
fun testNetworkPacketSerialization() {
  val packet = MockFactory.createBatteryPacket(
    deviceId = "test_device",
    batteryLevel = 75
  )

  // Verify packet created via FFI
  assertNotNull(packet)
  assertEquals("cconnect.battery", packet.type)
}

@Test
fun testCertificateGeneration() {
  val certificate = FfiTestUtils.createTestCertificate()

  // Verify certificate generated via Rust core
  assertNotNull(certificate)
  assertTrue(certificate.isNotEmpty())
}

@Test
fun testDiscoveryService() {
  val result = FfiTestUtils.testDiscoveryService()

  // Verify Rust discovery service accessible
  assertTrue(result)
}
```

### Layer 2: Integration Tests

Test complete flows through Android → Rust → Network.

**Purpose:** Verify end-to-end functionality

**Examples:**
```kotlin
@Test
fun testDeviceDiscoveryFlow() {
  // Start discovery (calls Rust FFI)
  val discovery = startDiscovery()

  // Wait for device found
  TestUtils.waitFor {
    discovery.foundDevices.isNotEmpty()
  }

  // Verify device info
  val device = discovery.foundDevices.first()
  assertNotNull(device.deviceId)
  assertNotNull(device.deviceName)
}

@Test
fun testPairingFlow() {
  // Create devices via FFI
  val device1 = createTestDevice("device1")
  val device2 = createTestDevice("device2")

  // Initiate pairing (Android → Rust)
  device1.requestPairing(device2.deviceId)

  // Verify pairing request sent via Rust
  TestUtils.waitFor {
    device2.isPairRequestedByPeer
  }

  // Accept pairing
  device2.acceptPairing()

  // Verify both paired via Rust core
  TestUtils.waitFor {
    device1.isPaired && device2.isPaired
  }
}
```

### Layer 3: Compose UI Tests

Test Compose screens with test rules.

**Purpose:** Verify UI behavior and interactions

**Examples:**
```kotlin
@get:Rule
val composeRule = createComposeRule()

@Test
fun testDeviceListScreen() {
  composeRule.setContent {
    DeviceListScreen(
      viewModel = testViewModel,
      onDeviceClick = {}
    )
  }

  // Verify screen loads
  composeRule.waitForText("Pair New Device")

  // Verify devices displayed
  composeRule.assertTextDisplayed("Test Device 1")
  composeRule.assertTextDisplayed("Test Device 2")

  // Click device
  composeRule.clickListItem("Test Device 1")
}

@Test
fun testDeviceDetailScreen_Pairing() {
  composeRule.setContent {
    DeviceDetailScreen(
      viewModel = unpairedDeviceViewModel,
      onNavigateBack = {}
    )
  }

  // Verify unpaired state
  composeRule.assertTextDisplayed("Request Pairing")

  // Click pair button
  composeRule.clickButton("Request Pairing")

  // Verify loading state
  composeRule.assertLoading()
}

@Test
fun testSettingsScreen_DeviceRename() {
  composeRule.setContent {
    SettingsScreen(
      viewModel = settingsViewModel,
      onNavigateBack = {}
    )
  }

  // Click device name
  composeRule.clickListItem("Device Name")

  // Verify dialog shown
  composeRule.waitForDialog()
  composeRule.assertTextDisplayed("Rename Device")

  // Enter new name
  composeRule.clearAndEnterText("Device name", "My New Phone")

  // Confirm
  composeRule.confirmDialog("Confirm")

  // Verify name updated
  composeRule.waitForText("My New Phone")
}
```

### Layer 4: End-to-End Tests

Test complete scenarios with real Rust core.

**Purpose:** Verify Android ↔ COSMIC Desktop communication

**Examples:**
```kotlin
@Test
fun testFullPairingScenario() {
  // Setup: Start Android app and mock COSMIC Desktop
  val cosmicMock = setupMockCosmicDesktop()
  val androidApp = setupAndroidApp()

  // Discovery: Android discovers COSMIC via Rust
  androidApp.startDiscovery()
  TestUtils.waitFor { androidApp.foundDevices.isNotEmpty() }

  // Pairing: Android requests pair
  val cosmicDevice = androidApp.foundDevices.first()
  androidApp.requestPairing(cosmicDevice.deviceId)

  // Verification: COSMIC receives request via Rust
  TestUtils.waitFor { cosmicMock.receivedPairRequest }

  // Acceptance: COSMIC accepts via Rust
  cosmicMock.acceptPairing()

  // Verification: Android completes pairing
  TestUtils.waitFor { cosmicDevice.isPaired }
}

@Test
fun testFileTransferScenario() {
  // Setup: Paired devices
  val (android, cosmic) = setupPairedDevices()

  // Share: Android sends file to COSMIC via Rust
  val testFile = createTestFile("test.txt", "Hello COSMIC")
  android.shareFile(cosmic.deviceId, testFile)

  // Verification: COSMIC receives file via Rust
  TestUtils.waitFor { cosmic.receivedFiles.isNotEmpty() }

  // Verification: File content matches
  val receivedFile = cosmic.receivedFiles.first()
  assertEquals("test.txt", receivedFile.name)
  assertEquals("Hello COSMIC", receivedFile.readText())
}
```

## Test Configuration

### Dependencies

Add to `build.gradle.kts`:

```kotlin
android {
  // ...

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }
}

dependencies {
  // AndroidX Test
  androidTestImplementation("androidx.test:core:1.5.0")
  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test:rules:1.5.0")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")

  // Compose Testing
  androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")
  debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.0")

  // Coroutines Testing
  androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

  // Mockito/MockK (optional)
  androidTestImplementation("io.mockk:mockk-android:1.13.8")
}
```

### Test Runner Configuration

Create `src/androidTest/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

  <application>
    <uses-library android:name="android.test.runner" />
  </application>

  <instrumentation
    android:name="androidx.test.runner.AndroidJUnitRunner"
    android:targetPackage="org.cosmic.cosmicconnect" />
</manifest>
```

## Usage Patterns

### Basic Integration Test

```kotlin
@RunWith(AndroidJUnit4::class)
class DeviceDiscoveryTest {

  @Before
  fun setup() {
    TestUtils.cleanupTestData()
  }

  @After
  fun teardown() {
    TestUtils.cleanupTestData()
  }

  @Test
  fun testDiscoverDevice() {
    // Use FFI to create mock discovery response
    val mockDevice = FfiTestUtils.createMockDiscoveryResponse(
      deviceId = "cosmic_desktop_1",
      deviceName = "My COSMIC PC"
    )

    // Verify FFI returns correct data
    assertEquals("cosmic_desktop_1", mockDevice["deviceId"])
    assertEquals("My COSMIC PC", mockDevice["deviceName"])
  }
}
```

### Compose UI Test

```kotlin
@RunWith(AndroidJUnit4::class)
class DeviceListScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun testDeviceListDisplay() {
    // Setup mock ViewModel
    val devices = MockFactory.createMockDeviceList(count = 3)
    val viewModel = createTestViewModel(devices)

    // Launch screen
    composeRule.setContent {
      CosmicTheme {
        DeviceListScreen(viewModel = viewModel, onDeviceClick = {})
      }
    }

    // Verify devices displayed
    composeRule.assertTextDisplayed("Test Device 0")
    composeRule.assertTextDisplayed("Test Device 1")
    composeRule.assertTextDisplayed("Test Device 2")
  }
}
```

### FFI Layer Test

```kotlin
@RunWith(AndroidJUnit4::class)
class NetworkPacketFfiTest {

  @Test
  fun testPacketSerializationViaFfi() {
    // Create packet via FFI
    val packet = MockFactory.createBatteryPacket(
      deviceId = "test_device",
      batteryLevel = 75,
      isCharging = true
    )

    // Verify FFI call succeeded
    assertNotNull(packet)
    assertEquals("cconnect.battery", packet.type)

    // Test serialization roundtrip via FFI
    val result = FfiTestUtils.testNetworkPacketSerialization(
      type = packet.type,
      body = packet.body
    )

    assertTrue(result)
  }
}
```

## Performance Testing

### FFI Overhead Benchmarking

```kotlin
@Test
fun benchmarkFfiOverhead() {
  // Measure average FFI call time
  val avgNanos = FfiTestUtils.measureFfiOverhead(iterations = 1000)

  // Assert acceptable performance
  // FFI calls should be < 100 microseconds on average
  assertTrue(avgNanos < 100_000, "FFI overhead too high: $avgNanos ns")

  Log.i("Performance", "Average FFI overhead: ${avgNanos / 1000} μs")
}
```

### Memory Testing

```kotlin
@Test
fun testFfiMemoryManagement() {
  // Test memory management across FFI boundary
  val result = FfiTestUtils.testFfiMemoryManagement(iterations = 100)

  assertTrue(result, "FFI memory management failed")
}
```

## Continuous Integration

### GitHub Actions Configuration

Add to `.github/workflows/android-tests.yml`:

```yaml
name: Android Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Rust
        uses: actions-rs/toolchain@v1
        with:
          toolchain: stable
          target: aarch64-linux-android

      - name: Run Unit Tests
        run: ./gradlew test

      - name: Run Integration Tests
        run: ./gradlew connectedAndroidTest

      - name: Upload Test Reports
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: test-reports
          path: app/build/reports/
```

## Best Practices

### 1. Test Isolation

- Clean up test data in `@Before` and `@After`
- Use unique IDs for each test
- Don't rely on test execution order

### 2. FFI Testing

- Test FFI availability first
- Handle FFI errors gracefully
- Test thread safety for concurrent FFI calls
- Benchmark performance regularly

### 3. Compose Testing

- Use `waitFor` for asynchronous UI updates
- Test both success and error states
- Verify accessibility (content descriptions)
- Test different screen sizes

### 4. Integration Testing

- Mock external dependencies (COSMIC Desktop)
- Test realistic scenarios
- Verify state changes via callbacks
- Test error recovery

### 5. Performance Testing

- Set acceptable performance targets
- Measure FFI overhead
- Test with realistic data sizes
- Profile memory usage

## Troubleshooting

### FFI Tests Failing

**Problem:** FFI library not loaded

**Solution:**
- Verify Rust library is built for Android
- Check `jniLibs` directory contains `.so` files
- Ensure correct target architecture

### Compose Tests Timing Out

**Problem:** `waitFor` timeouts

**Solution:**
- Increase timeout value
- Check if ViewModel is properly initialized
- Verify StateFlow is updating

### Memory Leaks in FFI

**Problem:** Memory grows over time

**Solution:**
- Ensure proper cleanup in `onCleared`
- Test with `FfiTestUtils.testFfiMemoryManagement()`
- Use weak references where appropriate

## References

- [AndroidX Test Documentation](https://developer.android.com/training/testing/set-up-project)
- [Compose Testing Documentation](https://developer.android.com/jetpack/compose/testing)
- [uniffi-rs Documentation](https://mozilla.github.io/uniffi-rs/)
- [Rust FFI Best Practices](https://doc.rust-lang.org/nomicon/ffi.html)

---

**Issue #28 Complete** ✅

Integration test framework established with FFI, Compose, and E2E testing support!

**Ready for test implementation:**
- Issue #30: Integration Tests - Discovery & Pairing
- Issue #31: Integration Tests - File Transfer
- Issue #32: Integration Tests - All Plugins
- Issue #33: E2E Test: Android → COSMIC
- Issue #34: E2E Test: COSMIC → Android
- Issue #35: Performance Testing
