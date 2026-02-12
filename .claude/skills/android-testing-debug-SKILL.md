# Android Testing & Debugging Skill

**Version**: 1.0.0
**Last Updated**: 2026-01-18
**Skill Type**: Testing & Debugging
**Project**: COSMIC Connect Android

## Overview

This skill provides comprehensive guidance for testing and debugging COSMIC Connect Android application using modern Android testing frameworks, tools, and best practices as of 2026. It covers unit testing, instrumented testing, UI testing, debugging with ADB, logcat analysis, and performance profiling on both Waydroid and real devices.

## Table of Contents

1. [Testing Strategy](#testing-strategy)
2. [Testing Frameworks](#testing-frameworks)
3. [Unit Testing](#unit-testing)
4. [Instrumented Testing](#instrumented-testing)
5. [UI Testing with Espresso](#ui-testing-with-espresso)
6. [Integration Testing](#integration-testing)
7. [Performance Testing](#performance-testing)
8. [Debugging with ADB](#debugging-with-adb)
9. [Logcat Analysis](#logcat-analysis)
10. [Waydroid Testing](#waydroid-testing)
11. [Real Device Testing](#real-device-testing)
12. [Common Testing Patterns](#common-testing-patterns)
13. [Debugging Scenarios](#debugging-scenarios)
14. [Best Practices](#best-practices)

---

## Testing Strategy

COSMIC Connect uses a **hybrid testing approach**:

### Coverage Distribution

| Test Type | Coverage | Where | Speed | Purpose |
|-----------|----------|-------|-------|---------|
| **Unit Tests** | 25% | JVM (local) | Very Fast | Business logic, FFI validation |
| **Integration Tests** | 50% | Device/Waydroid | Fast | Plugin interactions, network |
| **UI Tests** | 15% | Device/Waydroid | Moderate | User flows, UI components |
| **E2E Tests** | 10% | Real devices | Slow | Complete user scenarios |

### Testing Pyramid

```
         ╱╲
        ╱E2╲       10% - End-to-End Tests (Real devices)
       ╱────╲
      ╱  UI  ╲     15% - UI Tests (Espresso)
     ╱────────╲
    ╱Integration╲  50% - Integration Tests (AndroidX Test)
   ╱────────────╲
  ╱  Unit Tests  ╲ 25% - Unit Tests (JUnit, Mockito)
 ╱────────────────╲
```

### Platform-Specific Testing

**Waydroid** (90% automated):
- Network discovery and pairing
- File transfers over Wi-Fi
- Plugin functionality
- UI flows
- Performance benchmarks

**Samsung Galaxy Tab S8 Ultra** (10% validation):
- Bluetooth pairing and discovery
- Real hardware validation
- Final E2E scenarios
- User experience testing

---

## Testing Frameworks

### Current Versions (2026)

```kotlin
// build.gradle.kts
dependencies {
    // JUnit 4
    testImplementation("junit:junit:4.13.2")

    // AndroidX Test - Latest as of January 2026
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")

    // AndroidX Test - JUnit
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")

    // Espresso - Latest version
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-web:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-idling-resource:3.7.0")

    // Mockito for mocking
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    androidTestImplementation("org.mockito:mockito-android:5.10.0")

    // Kotlin Coroutines Test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Truth assertions
    testImplementation("com.google.truth:truth:1.4.2")
    androidTestImplementation("com.google.truth:truth:1.4.2")

    // Robolectric for local instrumented-style tests
    testImplementation("org.robolectric:robolectric:4.11.1")
}
```

### Test Runner Configuration

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    androidTestUtil("androidx.test:orchestrator:1.5.0")
}
```

---

## Unit Testing

Unit tests run on the JVM and are fast. Use for business logic, FFI validation, and utilities.

### Basic Unit Test Structure

```kotlin
// src/test/org/cosmic/cosmicconnect/ExampleUnitTest.kt
package org.cosmicext.connect

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

class NetworkPacketTest {

    // Timeout for all tests in this class
    @get:Rule
    val globalTimeout: Timeout = Timeout(10, TimeUnit.SECONDS)

    private lateinit var testPacket: NetworkPacket

    @Before
    fun setUp() {
        // Setup before each test
        testPacket = NetworkPacket(
            id = 123456789,
            type = "cconnect.ping",
            body = mapOf("message" to "test")
        )
    }

    @After
    fun tearDown() {
        // Cleanup after each test
    }

    @Test
    fun `packet serialization produces valid JSON`() {
        // Arrange
        val expectedJson = """{"id":123456789,"type":"cconnect.ping","body":{"message":"test"}}"""

        // Act
        val actualJson = testPacket.serialize()

        // Assert
        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `packet deserialization handles valid JSON`() {
        // Arrange
        val json = """{"id":123456789,"type":"cconnect.ping","body":{"message":"test"}}"""

        // Act
        val packet = NetworkPacket.deserialize(json)

        // Assert
        assertNotNull(packet)
        assertEquals("cconnect.ping", packet?.type)
        assertEquals("test", packet?.body?.get("message"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `packet deserialization throws on invalid JSON`() {
        NetworkPacket.deserialize("invalid json")
    }
}
```

### FFI Validation Tests

```kotlin
// src/test/org/cosmic/cosmicconnect/FFIValidationTest.kt
package org.cosmicext.connect

import org.junit.Test
import org.junit.Assert.*
import org.junit.BeforeClass
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.DeviceInfo

class FFIValidationTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun loadLibrary() {
            // Load native library
            System.loadLibrary("cosmic_connect_core")
        }
    }

    @Test
    fun `FFI battery packet creation works`() {
        val packet = NetworkPacket.createBatteryPacket(
            isCharging = true,
            currentCharge = 85,
            thresholdEvent = 0
        )

        assertNotNull(packet)
        assertEquals("cconnect.battery", packet.type)
        assertTrue(packet.body["isCharging"] as Boolean)
        assertEquals(85, packet.body["currentCharge"])
    }

    @Test
    fun `FFI ping packet has correct timestamp`() {
        val beforeTime = System.currentTimeMillis()
        val packet = NetworkPacket.createPingPacket()
        val afterTime = System.currentTimeMillis()

        assertNotNull(packet)
        assertEquals("cconnect.ping", packet.type)

        val packetId = packet.id
        assertTrue(packetId >= beforeTime)
        assertTrue(packetId <= afterTime)
    }
}
```

### Testing Coroutines

```kotlin
import kotlinx.coroutines.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule

@ExperimentalCoroutinesApi
class CoroutineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `async operation completes successfully`() = runTest {
        // Arrange
        val repository = DeviceRepository()

        // Act
        val result = repository.discoverDevices()

        // Assert
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `flow emits expected values`() = runTest {
        // Arrange
        val viewModel = DeviceViewModel()
        val emissions = mutableListOf<DeviceState>()

        // Act
        val job = launch {
            viewModel.deviceState.collect { emissions.add(it) }
        }

        viewModel.startDiscovery()
        advanceUntilIdle() // Advance coroutines

        job.cancel()

        // Assert
        assertTrue(emissions.contains(DeviceState.Discovering))
    }
}

// Helper rule for main dispatcher
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

---

## Instrumented Testing

Instrumented tests run on Android devices/emulators. Use for Android framework APIs and integration testing.

### Basic Instrumented Test

```kotlin
// src/androidTest/org/cosmic/cosmicconnect/ExampleInstrumentedTest.kt
package org.cosmicext.connect

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
@SmallTest
class DeviceDiscoveryInstrumentedTest {

    @Test
    fun useAppContext() {
        // Context of the app under test
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.cosmicext.connect", appContext.packageName)
    }

    @Test
    fun `UDP broadcast discovers devices`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val discoveryService = DiscoveryService(context)

        // Start discovery
        discoveryService.startDiscovery()

        // Wait for responses
        Thread.sleep(5000)

        // Assert
        val devices = discoveryService.getDiscoveredDevices()
        assertNotNull(devices)
    }
}
```

### Activity Testing

```kotlin
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `activity launches successfully`() {
        activityRule.scenario.use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                assertTrue(activity.isTaskRoot)
            }
        }
    }

    @Test
    fun `activity handles configuration changes`() {
        activityRule.scenario.use { scenario ->
            // Rotate screen
            scenario.onActivity { it.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }

            // Verify activity recreated correctly
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById(R.id.device_list))
            }
        }
    }
}
```

### Service Testing

```kotlin
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import org.junit.Rule
import org.junit.Test

class BackgroundServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun `service binds successfully`() {
        // Create intent
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            BackgroundService::class.java
        )

        // Bind to service
        val binder = serviceRule.bindService(serviceIntent)
        val service = (binder as BackgroundService.LocalBinder).service

        // Assert
        assertNotNull(service)
        assertTrue(service.isRunning())
    }
}
```

---

## UI Testing with Espresso

Espresso provides a fluent API for writing concise, reliable UI tests.

### Espresso Basics

```kotlin
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceListUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun `device list displays correctly`() {
        // Check list is displayed
        onView(withId(R.id.device_list))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `clicking device shows details`() {
        // Find and click first device
        onView(withId(R.id.device_list))
            .perform(click())

        // Verify details screen is shown
        onView(withId(R.id.device_detail_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun `search filters device list`() {
        // Type in search box
        onView(withId(R.id.search_box))
            .perform(typeText("Desktop"), closeSoftKeyboard())

        // Verify filtered results
        onView(withText("COSMIC Desktop"))
            .check(matches(isDisplayed()))
    }
}
```

### Advanced Espresso Patterns

```kotlin
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.recyclerview.widget.RecyclerView
import org.hamcrest.Matchers.*

class AdvancedEspressoTest {

    private lateinit var idlingResource: IdlingResource

    @Before
    fun registerIdlingResource() {
        activityRule.scenario.onActivity { activity ->
            idlingResource = activity.getIdlingResource()
            IdlingRegistry.getInstance().register(idlingResource)
        }
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun `scroll to item in RecyclerView`() {
        // Scroll to position
        onView(withId(R.id.device_list))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(10))

        // Scroll to specific item
        onView(withId(R.id.device_list))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                hasDescendant(withText("COSMIC Desktop"))
            ))
    }

    @Test
    fun `perform action on RecyclerView item`() {
        onView(withId(R.id.device_list))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, click()
            ))
    }

    @Test
    fun `custom matcher for view properties`() {
        onView(withId(R.id.battery_level))
            .check(matches(withBatteryLevel(greaterThan(50))))
    }

    @Test
    fun `test with intents`() {
        // Use IntentsTestRule instead of ActivityScenarioRule
        intended(hasComponent(DeviceDetailActivity::class.java.name))
    }
}

// Custom matcher example
fun withBatteryLevel(expectedLevel: Matcher<Int>): Matcher<View> {
    return object : BoundedMatcher<View, TextView>(TextView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("with battery level: ")
            expectedLevel.describeTo(description)
        }

        override fun matchesSafely(textView: TextView): Boolean {
            val level = textView.text.toString().toIntOrNull() ?: 0
            return expectedLevel.matches(level)
        }
    }
}
```

### Handling Asynchronous Operations

```kotlin
import androidx.test.espresso.IdlingResource

class SimpleIdlingResource : IdlingResource {

    @Volatile
    private var callback: IdlingResource.ResourceCallback? = null

    @Volatile
    private var isIdle = true

    override fun getName(): String = this::class.java.name

    override fun isIdleNow(): Boolean = isIdle

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.callback = callback
    }

    fun setIdleState(isIdle: Boolean) {
        this.isIdle = isIdle
        if (isIdle) {
            callback?.onTransitionToIdle()
        }
    }
}

// Usage in test
@Test
fun `test with idling resource`() {
    val idlingResource = SimpleIdlingResource()
    IdlingRegistry.getInstance().register(idlingResource)

    try {
        // Perform action that triggers async work
        onView(withId(R.id.refresh_button)).perform(click())

        // Espresso waits for idling resource
        onView(withId(R.id.content))
            .check(matches(isDisplayed()))
    } finally {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}
```

---

## Integration Testing

Integration tests verify that multiple components work together correctly.

### Plugin Integration Test

```kotlin
@RunWith(AndroidJUnit4::class)
@MediumTest
class BatteryPluginIntegrationTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var plugin: BatteryPlugin

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        device = createMockDevice()
        plugin = BatteryPlugin(context, device)
    }

    @Test
    fun `battery plugin sends update on battery change`() {
        // Arrange
        val batteryIntent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 85)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
        }

        var packetSent: NetworkPacket? = null
        device.setPacketListener { packet ->
            packetSent = packet
        }

        // Act
        plugin.onBatteryChanged(batteryIntent)

        // Assert
        assertNotNull(packetSent)
        assertEquals("cconnect.battery", packetSent?.type)
        assertEquals(85, packetSent?.body?.get("currentCharge"))
        assertTrue(packetSent?.body?.get("isCharging") as Boolean)
    }

    @Test
    fun `battery plugin handles request packet`() {
        // Arrange
        val requestPacket = NetworkPacket(
            id = 123,
            type = "cconnect.battery.request",
            body = emptyMap()
        )

        var responseSent = false
        device.setPacketListener { packet ->
            if (packet.type == "cconnect.battery") {
                responseSent = true
            }
        }

        // Act
        plugin.handlePacket(requestPacket)

        // Assert
        assertTrue(responseSent)
    }
}
```

### Network Integration Test

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class NetworkIntegrationTest {

    @Test
    fun `device discovery and pairing flow`() = runTest {
        // Arrange
        val discoveryService = DiscoveryService(context)
        val pairingHandler = PairingHandler(context)

        // Act - Start discovery
        discoveryService.startDiscovery()
        delay(5000) // Wait for broadcast

        val devices = discoveryService.getDiscoveredDevices()
        assertTrue(devices.isNotEmpty())

        // Act - Request pairing
        val device = devices.first()
        val pairingResult = pairingHandler.requestPairing(device)

        // Assert
        assertTrue(pairingResult.isSuccess)
    }

    @Test
    fun `file transfer completes successfully`() = runTest {
        // Arrange
        val testFile = createTestFile(size = 1024 * 1024) // 1 MB
        val device = getPairedDevice()
        val sharePlugin = SharePlugin(context, device)

        // Act
        val result = sharePlugin.sendFile(testFile)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(testFile.length(), result.bytesTransferred)
    }
}
```

---

## Performance Testing

Performance tests measure app performance and ensure it meets targets.

### Performance Benchmark Test

```kotlin
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Rule
import org.junit.Test

class PerformanceBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun `benchmark FFI packet creation`() {
        benchmarkRule.measureRepeated {
            NetworkPacket.createPingPacket()
        }
    }

    @Test
    fun `benchmark JSON serialization`() {
        val packet = NetworkPacket.createBatteryPacket(
            isCharging = true,
            currentCharge = 85,
            thresholdEvent = 0
        )

        benchmarkRule.measureRepeated {
            packet.serialize()
        }
    }

    @Test
    fun `benchmark file transfer throughput`() {
        val testFile = createTestFile(size = 10 * 1024 * 1024) // 10 MB
        val device = getMockDevice()
        val sharePlugin = SharePlugin(context, device)

        benchmarkRule.measureRepeated {
            runBlocking {
                sharePlugin.sendFile(testFile)
            }
        }
    }
}
```

### Memory Profiling Test

```kotlin
import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class MemoryProfilingTest {

    @Test
    fun `measure memory usage during file transfer`() {
        // Start memory tracking
        val startMemory = getUsedMemory()

        // Perform operation
        val testFile = createTestFile(size = 50 * 1024 * 1024) // 50 MB
        val sharePlugin = SharePlugin(context, device)
        runBlocking {
            sharePlugin.sendFile(testFile)
        }

        // Measure memory after
        val endMemory = getUsedMemory()
        val memoryGrowth = endMemory - startMemory

        // Assert memory growth is acceptable (< 50 MB)
        assertTrue("Memory growth: $memoryGrowth bytes", memoryGrowth < 50 * 1024 * 1024)
    }

    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
```

---

## Debugging with ADB

Android Debug Bridge (ADB) is essential for debugging on devices and emulators.

### Essential ADB Commands

```bash
# Device Management
adb devices                          # List connected devices
adb devices -l                       # List with details
adb -s <serial> <command>            # Target specific device
export ANDROID_SERIAL=<serial>       # Set default device

# Connect to Waydroid
waydroid session start
adb connect 192.168.240.112:5555

# Connect to Samsung Tablet (Wireless)
adb connect 192.168.1.100:45678

# App Installation & Management
adb install path/to/app.apk         # Install APK
adb install -r app.apk               # Reinstall keeping data
adb uninstall org.cosmicext.connect  # Uninstall app
adb shell pm clear org.cosmicext.connect  # Clear app data

# Starting Activities
adb shell am start -n org.cosmicext.connect/.MainActivity
adb shell am start -a android.intent.action.VIEW -d "cosmic://pair"

# Permissions
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_CONNECT
adb shell pm grant org.cosmicext.connect android.permission.ACCESS_FINE_LOCATION

# File Operations
adb push local/file.txt /sdcard/
adb pull /sdcard/file.txt local/

# Screen Capture
adb shell screencap /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Screen Recording
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4

# Network Debugging
adb shell dumpsys wifi                    # Wi-Fi info
adb shell dumpsys bluetooth_manager       # Bluetooth info
adb shell netstat                         # Network connections
adb shell tcpdump -i any -w /sdcard/capture.pcap  # Packet capture

# System Info
adb shell getprop                         # All system properties
adb shell getprop ro.product.model        # Device model
adb shell dumpsys battery                 # Battery status
adb shell dumpsys meminfo org.cosmicext.connect  # App memory usage
```

### Advanced ADB Techniques

```bash
# Enable Bluetooth via ADB
adb shell svc bluetooth enable

# Simulate battery status
adb shell dumpsys battery set level 50
adb shell dumpsys battery set status 2  # Charging

# Reset battery simulation
adb shell dumpsys battery reset

# Test deep sleep / doze mode
adb shell dumpsys deviceidle force-idle

# Simulate network conditions
adb shell svc wifi enable
adb shell svc wifi disable
adb shell svc data enable
adb shell svc data disable

# Run monkey test (stress testing)
adb shell monkey -p org.cosmicext.connect -v 500

# Profile app performance
adb shell am profile start org.cosmicext.connect /sdcard/profile.trace
# ... use app ...
adb shell am profile stop org.cosmicext.connect
adb pull /sdcard/profile.trace

# Debug native crashes
adb shell setprop debug.checkjni 1
adb logcat -b crash
```

---

## Logcat Analysis

Logcat is the primary tool for viewing Android system logs.

### Basic Logcat Commands

```bash
# View all logs
adb logcat

# Clear log buffer
adb logcat -c

# View specific log buffer
adb logcat -b main          # Main log
adb logcat -b system        # System log
adb logcat -b crash         # Crash log
adb logcat -b events        # Event log

# Filter by priority
adb logcat *:E              # Error and above
adb logcat *:W              # Warning and above
adb logcat *:I              # Info and above
adb logcat *:D              # Debug and above
adb logcat *:V              # Verbose (all)

# Filter by tag
adb logcat CosmicConnect:D *:S  # Only CosmicConnect debug logs

# Multiple tags
adb logcat CosmicConnect:D NetworkPacket:V *:S

# Regex filtering
adb logcat | grep -i "bluetooth"
adb logcat | grep -E "error|exception|crash"

# Save to file
adb logcat -d > logcat.txt
adb logcat -f /sdcard/logcat.txt
```

### Advanced Logcat Filtering

```bash
# Filter by package name
adb logcat --pid=$(adb shell pidof -s org.cosmicext.connect)

# Time-based filtering
adb logcat -t '01-18 12:00:00.000'  # Since specific time
adb logcat -T 100                    # Last 100 lines

# Format output
adb logcat -v brief         # Default format
adb logcat -v time          # With timestamps
adb logcat -v threadtime    # With thread info (recommended)
adb logcat -v long          # Detailed format

# Color-coded output (using external tools)
adb logcat -v color
adb logcat | ccze -A        # Color with ccze

# Filter by priority and tag together
adb logcat CosmicConnect:V NetworkPacket:D TLS:I *:W

# Exclude specific tags
adb logcat -s CosmicConnect:V -s NetworkPacket:D | grep -v "DEBUG_SPAM"
```

### Logcat Best Practices for COSMIC Connect

```kotlin
// Use consistent tag naming
private const val TAG = "CosmicConnect.BatteryPlugin"

// Use appropriate log levels
Log.v(TAG, "Verbose: detailed debugging info")
Log.d(TAG, "Debug: debugging information")
Log.i(TAG, "Info: general informational messages")
Log.w(TAG, "Warning: potential issues")
Log.e(TAG, "Error: errors that need attention")

// Log with exceptions
try {
    // code
} catch (e: Exception) {
    Log.e(TAG, "Failed to send packet", e)
}

// Use string templates for better performance
Log.d(TAG, "Battery level: $level, Charging: $isCharging")

// Conditional logging for performance
if (BuildConfig.DEBUG) {
    Log.v(TAG, "Expensive debug info: ${expensiveOperation()}")
}
```

### Automated Logcat Analysis

```bash
#!/bin/bash
# Analyze logs for errors and warnings

# Capture logs to file
adb logcat -d > full_logcat.txt

# Extract errors
grep -i "error\|exception\|crash" full_logcat.txt > errors.txt

# Count log levels
echo "Error count: $(grep -c " E/" full_logcat.txt)"
echo "Warning count: $(grep -c " W/" full_logcat.txt)"

# Find crashes
adb logcat -b crash -d > crashes.txt

# Analyze Bluetooth issues
adb logcat -d | grep -i bluetooth | grep -i "error\|fail" > bluetooth_issues.txt

# Network packet analysis
adb logcat -d | grep -i "networkpacket\|cosmic" > packet_log.txt
```

---

## Waydroid Testing

Waydroid provides container-based Android for fast automated testing on NixOS.

### Waydroid Setup

```bash
# NixOS configuration (already done)
virtualisation.waydroid.enable = true;

# Initialize Waydroid
sudo waydroid init

# Start Waydroid session
waydroid session start

# Show Waydroid UI (optional, not needed for headless testing)
waydroid show-full-ui &

# Connect ADB
adb connect 192.168.240.112:5555
adb devices

# Verify connection
adb shell getprop ro.product.model  # Should show Waydroid model
```

### Automated Waydroid Testing

```bash
# Use the automated script
./scripts/test-waydroid.sh

# Headless mode (for CI/CD)
./scripts/test-waydroid.sh --headless

# Quick mode (skip clean build)
./scripts/test-waydroid.sh --quick

# Manual workflow
waydroid session start
adb wait-for-device
./gradlew assembleDebug
adb install -r build/outputs/apk/debug/*.apk
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN
./gradlew connectedAndroidTest
```

### Waydroid-Specific Testing

```kotlin
// Detect if running on Waydroid
fun isWaydroid(): Boolean {
    return Build.PRODUCT.contains("waydroid", ignoreCase = true) ||
           Build.DEVICE.contains("waydroid", ignoreCase = true)
}

// Skip Bluetooth tests on Waydroid
@Test
fun `bluetooth discovery works`() {
    assumeFalse("Bluetooth not supported on Waydroid", isWaydroid())

    // Test Bluetooth discovery
    val devices = bluetoothManager.discoverDevices()
    assertTrue(devices.isNotEmpty())
}

// Network tests work fine on Waydroid
@Test
fun `network discovery works on Waydroid`() {
    val discoveryService = DiscoveryService(context)
    discoveryService.startDiscovery()

    // This works on Waydroid
    delay(5000)
    val devices = discoveryService.getDiscoveredDevices()
    assertNotNull(devices)
}
```

### Waydroid Limitations

**Not Available on Waydroid:**
- Bluetooth (no Bluetooth stack)
- Real telephony (can mock)
- Camera hardware (can mock)
- NFC
- Some sensors

**Works Fine on Waydroid:**
- Network operations (Wi-Fi, UDP, TCP)
- File system operations
- UI testing
- Most Android APIs
- Performance testing

---

## Real Device Testing

Samsung Galaxy Tab S8 Ultra for Bluetooth and final validation.

### Samsung Tablet Setup

```bash
# Connect tablet wirelessly
./scripts/connect-tablet.sh

# Run automated tests
./scripts/test-samsung.sh --wireless

# Run Bluetooth-specific tests
./scripts/test-bluetooth.sh <device-serial>

# Manual connection
adb connect 192.168.1.100:45678
adb devices
```

### Bluetooth Testing Scenarios

```kotlin
@RunWith(AndroidJUnit4::class)
@RequiresDevice  // Requires real device
class BluetoothIntegrationTest {

    @Before
    fun setUp() {
        // Ensure Bluetooth is enabled
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        assumeTrue("Bluetooth not available", bluetoothAdapter != null)

        if (!bluetoothAdapter.isEnabled) {
            // Request user to enable Bluetooth
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityRule.scenario.onActivity { activity ->
                activity.startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    @Test
    fun `bluetooth device discovery finds COSMIC Desktop`() {
        val bluetoothManager = BluetoothManager(context)
        val devices = mutableListOf<BluetoothDevice>()

        bluetoothManager.startDiscovery { device ->
            devices.add(device)
        }

        // Wait for discovery
        Thread.sleep(12000)  // Bluetooth discovery takes ~12s

        bluetoothManager.stopDiscovery()

        // Assert
        assertTrue("No Bluetooth devices found", devices.isNotEmpty())
    }

    @Test
    fun `bluetooth pairing with COSMIC Desktop succeeds`() {
        // Find COSMIC Desktop device
        val bluetoothManager = BluetoothManager(context)
        val cosmicDevice = bluetoothManager.findDeviceByName("COSMIC Desktop")

        assumeNotNull("COSMIC Desktop not found", cosmicDevice)

        // Request pairing
        val pairingResult = bluetoothManager.pair(cosmicDevice!!)

        // Assert
        assertTrue("Pairing failed", pairingResult.isSuccess)
    }
}
```

### Real Device Performance Testing

```kotlin
@Test
@RequiresDevice
fun `measure real file transfer speed over Bluetooth`() {
    val testFile = createTestFile(size = 10 * 1024 * 1024) // 10 MB
    val device = getBluetoothPairedDevice()
    val sharePlugin = SharePlugin(context, device)

    val startTime = System.currentTimeMillis()
    runBlocking {
        sharePlugin.sendFile(testFile)
    }
    val endTime = System.currentTimeMillis()

    val durationSeconds = (endTime - startTime) / 1000.0
    val speedMBps = (testFile.length() / (1024.0 * 1024.0)) / durationSeconds

    Log.i(TAG, "Bluetooth transfer speed: $speedMBps MB/s")

    // Assert reasonable Bluetooth speed (> 1 MB/s for BT 5.2)
    assertTrue("Bluetooth too slow: $speedMBps MB/s", speedMBps > 1.0)
}
```

---

## Common Testing Patterns

### Test Data Builders

```kotlin
// Builder pattern for test data
class DeviceBuilder {
    private var id: String = "test-device-id"
    private var name: String = "Test Device"
    private var deviceType: DeviceType = DeviceType.PHONE
    private var isPaired: Boolean = false

    fun withId(id: String) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withDeviceType(type: DeviceType) = apply { this.deviceType = type }
    fun paired() = apply { this.isPaired = true }

    fun build(): Device = Device(
        id = id,
        name = name,
        deviceType = deviceType,
        isPaired = isPaired
    )
}

// Usage in tests
@Test
fun `test with custom device`() {
    val device = DeviceBuilder()
        .withName("COSMIC Desktop")
        .withDeviceType(DeviceType.DESKTOP)
        .paired()
        .build()

    assertTrue(device.isPaired)
}
```

### Test Fixtures

```kotlin
// Create reusable test fixtures
object TestFixtures {

    fun createPingPacket(id: Long = 123456789): NetworkPacket {
        return NetworkPacket(
            id = id,
            type = "cconnect.ping",
            body = emptyMap()
        )
    }

    fun createBatteryPacket(
        level: Int = 85,
        isCharging: Boolean = true
    ): NetworkPacket {
        return NetworkPacket(
            id = System.currentTimeMillis(),
            type = "cconnect.battery",
            body = mapOf(
                "currentCharge" to level,
                "isCharging" to isCharging,
                "thresholdEvent" to 0
            )
        )
    }

    fun createTestFile(size: Long): File {
        val file = File.createTempFile("test", ".dat")
        file.deleteOnExit()
        FileOutputStream(file).use { output ->
            val buffer = ByteArray(8192)
            var remaining = size
            while (remaining > 0) {
                val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                output.write(buffer, 0, toWrite)
                remaining -= toWrite
            }
        }
        return file
    }
}
```

### Parameterized Tests

```kotlin
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class PacketSerializationTest(
    private val packetType: String,
    private val body: Map<String, Any>
) {

    companion object {
        @JvmStatic
        @Parameters(name = "{index}: packetType={0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf("cconnect.ping", emptyMap<String, Any>()),
            arrayOf("cconnect.battery", mapOf("currentCharge" to 85)),
            arrayOf("cconnect.clipboard", mapOf("content" to "test")),
        )
    }

    @Test
    fun `packet serializes and deserializes correctly`() {
        val packet = NetworkPacket(
            id = 123,
            type = packetType,
            body = body
        )

        val json = packet.serialize()
        val deserialized = NetworkPacket.deserialize(json)

        assertEquals(packet.type, deserialized?.type)
        assertEquals(packet.body, deserialized?.body)
    }
}
```

---

## Debugging Scenarios

### Scenario 1: App Crashes on Startup

```bash
# Get crash log
adb logcat -b crash -d

# Look for stack trace
adb logcat -d | grep -A 50 "FATAL EXCEPTION"

# Check native crashes
adb logcat -d | grep "DEBUG.*pid.*signal"

# Get detailed crash info
adb shell dumpsys dropbox --print | grep crash
```

### Scenario 2: Network Discovery Not Working

```bash
# Check network state
adb shell dumpsys wifi
adb shell ip addr show

# Monitor UDP packets
adb logcat | grep -i "udp\|discovery\|broadcast"

# Check permissions
adb shell dumpsys package org.cosmicext.connect | grep permission

# Test network manually
adb shell ping 192.168.1.1
adb shell netstat -an | grep 1716  # KDE Connect port
```

### Scenario 3: Bluetooth Pairing Fails

```bash
# Enable Bluetooth
adb shell svc bluetooth enable

# Check Bluetooth status
adb shell dumpsys bluetooth_manager

# Monitor Bluetooth logs
adb logcat | grep -i bluetooth

# Check Bluetooth permissions
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_SCAN
adb shell pm grant org.cosmicext.connect android.permission.BLUETOOTH_CONNECT
```

### Scenario 4: File Transfer Hangs

```kotlin
// Add detailed logging
Log.d(TAG, "Starting file transfer: ${file.name}, size: ${file.length()}")

// Monitor progress
transferJob = launch {
    var lastProgress = 0L
    while (isActive) {
        val progress = getCurrentProgress()
        Log.d(TAG, "Transfer progress: $progress / ${file.length()}")

        if (progress == lastProgress) {
            Log.w(TAG, "Transfer stalled at $progress bytes")
        }
        lastProgress = progress

        delay(1000)
    }
}

// Use timeout
withTimeout(60000) {  // 60 second timeout
    sendFile(file)
}
```

### Scenario 5: Memory Leak

```bash
# Monitor memory over time
while true; do
    adb shell dumpsys meminfo org.cosmicext.connect | grep "TOTAL"
    sleep 5
done

# Dump heap
adb shell am dumpheap org.cosmicext.connect /sdcard/heap.hprof
adb pull /sdcard/heap.hprof

# Analyze with Android Studio Memory Profiler or MAT (Memory Analyzer Tool)

# Force GC and measure
adb shell am force-stop org.cosmicext.connect
adb shell am start -n org.cosmicext.connect/.MainActivity
# ... use app ...
adb shell am force-stop org.cosmicext.connect
```

---

## Best Practices

### 1. Test Organization

```
src/
├── test/                           # Unit tests (JVM)
│   └── org/cosmic/cosmicconnect/
│       ├── FFIValidationTest.kt
│       ├── NetworkPacketTest.kt
│       └── utils/
│           └── TestFixtures.kt
│
└── androidTest/                    # Instrumented tests (Device)
    └── org/cosmic/cosmicconnect/
        ├── integration/            # Integration tests
        │   ├── DiscoveryTest.kt
        │   ├── PairingTest.kt
        │   └── FileTransferTest.kt
        ├── e2e/                    # End-to-end tests
        │   ├── AndroidToCosmicTest.kt
        │   └── CosmicToAndroidTest.kt
        ├── performance/            # Performance tests
        │   └── BenchmarkTest.kt
        └── ui/                     # UI tests
            ├── DeviceListTest.kt
            └── SettingsTest.kt
```

### 2. Test Naming Conventions

```kotlin
// Use descriptive test names with backticks
@Test
fun `battery packet contains correct charge level`() { }

@Test
fun `pairing request is rejected when device is blacklisted`() { }

@Test
fun `file transfer fails gracefully when network is unavailable`() { }

// Or use convention: methodName_stateUnderTest_expectedBehavior
@Test
fun sendFile_whenNetworkUnavailable_throwsNetworkException() { }
```

### 3. Test Independence

```kotlin
// ❌ Bad: Tests depend on each other
class BadTest {
    private var device: Device? = null

    @Test
    fun test1_createDevice() {
        device = Device("test-id")
    }

    @Test
    fun test2_useDevice() {
        // Fails if test1 doesn't run first!
        device!!.pair()
    }
}

// ✅ Good: Tests are independent
class GoodTest {
    private lateinit var device: Device

    @Before
    fun setUp() {
        device = Device("test-id")
    }

    @Test
    fun `device creation works`() {
        assertNotNull(device)
    }

    @Test
    fun `device pairing works`() {
        val result = device.pair()
        assertTrue(result.isSuccess)
    }
}
```

### 4. Use Test Doubles Appropriately

```kotlin
// Use fakes for complex dependencies
class FakeDeviceRepository : DeviceRepository {
    private val devices = mutableListOf<Device>()

    override suspend fun getDevices(): List<Device> = devices
    override suspend fun addDevice(device: Device) { devices.add(device) }
}

// Use mocks for verification
val mockDevice = mock<Device>()
plugin.handlePacket(packet)
verify(mockDevice).sendPacket(any())

// Use spies for partial mocking
val realDevice = Device("test-id")
val spyDevice = spy(realDevice)
doReturn(true).whenever(spyDevice).isPaired()
```

### 5. Cleanup Resources

```kotlin
@RunWith(AndroidJUnit4::class)
class ResourceCleanupTest {

    private lateinit var tempFile: File
    private lateinit var connection: Connection

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test", ".dat")
        connection = Connection.open()
    }

    @After
    fun tearDown() {
        // Always cleanup in @After
        tempFile.delete()
        connection.close()
    }

    @Test
    fun `test with resources`() {
        // Test using resources
    }
}
```

### 6. Test Performance Targets

```kotlin
@Test
fun `FFI call completes within target time`() {
    val startTime = System.nanoTime()

    val packet = NetworkPacket.createPingPacket()

    val endTime = System.nanoTime()
    val durationMs = (endTime - startTime) / 1_000_000.0

    // Assert meets performance target
    assertTrue("FFI call took ${durationMs}ms (target: < 1ms)", durationMs < 1.0)
}
```

### 7. Conditional Test Execution

```kotlin
import org.junit.Assume.assumeTrue
import org.junit.Assume.assumeFalse

@Test
fun `bluetooth test only on real device`() {
    assumeFalse("Skip on Waydroid", isWaydroid())
    assumeTrue("Bluetooth required", hasBluetoothCapability())

    // Test Bluetooth functionality
}

@Test
fun `network test runs everywhere`() {
    // No assumptions needed - works on Waydroid and real devices
    val discovery = DiscoveryService(context)
    discovery.startDiscovery()
}
```

### 8. Logging in Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class LoggingTest {

    @get:Rule
    val logRule = TestLogRule()

    @Test
    fun `test with detailed logging`() {
        Log.d(TAG, "Test started")

        // Test code

        Log.d(TAG, "Test completed")
    }
}

// Custom rule for test logging
class TestLogRule : TestWatcher() {
    override fun starting(description: Description) {
        Log.i("TEST", "Starting: ${description.methodName}")
    }

    override fun finished(description: Description) {
        Log.i("TEST", "Finished: ${description.methodName}")
    }

    override fun failed(e: Throwable, description: Description) {
        Log.e("TEST", "Failed: ${description.methodName}", e)
    }
}
```

---

## References & Resources

### Official Documentation

- [Android Testing Fundamentals](https://developer.android.com/training/testing/fundamentals) - Core testing concepts
- [AndroidX Test Library](https://developer.android.com/jetpack/androidx/releases/test) - Latest test library versions (updated January 2026)
- [Espresso Testing Guide](https://developer.android.com/training/testing/espresso) - Official Espresso documentation
- [Build Instrumented Tests](https://developer.android.com/training/testing/instrumented-tests) - Instrumented test guide
- [Logcat Command-line Tool](https://developer.android.com/tools/logcat) - Official logcat documentation
- [ADB Documentation](https://developer.android.com/tools/adb) - Android Debug Bridge guide

### Best Practices Articles

- [Best Practices for Testing Android Applications with Espresso and JUnit](https://blair49.medium.com/best-practices-for-testing-android-applications-with-espresso-and-junit-69ab94ba570f) - Comprehensive testing patterns (Medium, 2025)
- [Mastering ADB logcat](https://medium.com/@begunova/mastering-adb-logcat-options-filters-advanced-debugging-techniques-10331a73532f) - Advanced logcat techniques (Medium, February 2025)
- [How to Improve Your Android Debugging Process](https://blog.sentry.io/improve-android-debugging/) - Production debugging with Sentry

### Project-Specific Documentation

- [WAYDROID_TESTING_GUIDE.md](../../docs/testing/WAYDROID_TESTING_GUIDE.md) - Waydroid testing complete guide
- [SAMSUNG_TAB_TESTING_GUIDE.md](../../docs/testing/SAMSUNG_TAB_TESTING_GUIDE.md) - Real device testing guide
- [WAYDROID_TESTING_SUMMARY.md](../../docs/testing/WAYDROID_TESTING_SUMMARY.md) - Testing strategy summary

### Testing Scripts

- `scripts/test-waydroid.sh` - Automated Waydroid testing
- `scripts/test-samsung.sh` - Automated Samsung tablet testing
- `scripts/test-bluetooth.sh` - Bluetooth-specific testing
- `scripts/connect-tablet.sh` - Wireless ADB connection helper

---

## Skill Usage Guidelines

### When to Use This Skill

Use this skill when:
- Writing new tests (unit, integration, UI, E2E)
- Debugging test failures
- Analyzing logcat output
- Profiling app performance
- Setting up testing infrastructure
- Debugging on Waydroid or real devices
- Investigating crashes or ANRs
- Optimizing test execution speed

### How to Invoke This Skill

```bash
# In Claude Code CLI
claude-code "Using android-testing-debug skill, create integration tests for clipboard sync"
claude-code "Using android-testing-debug skill, debug Bluetooth pairing failure"
claude-code "Using android-testing-debug skill, analyze memory leak in file transfer"
```

### Combining with Other Skills

This skill works well with:
- **android-development-SKILL.md** - For implementing features that need tests
- **debugging-SKILL.md** - For general debugging strategies
- **tls-networking-SKILL.md** - For network-specific testing and debugging

---

**Created**: 2026-01-18
**Author**: Claude Code Agent
**Status**: Production Ready ✅
