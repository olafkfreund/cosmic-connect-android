# COSMIC Connect Plugin API Documentation

**Complete reference for developing plugins**

*Last Updated: 2026-01-17*

---

## Table of Contents

1. [Overview](#overview)
2. [Plugin Architecture](#plugin-architecture)
3. [Creating a Plugin](#creating-a-plugin)
4. [FFI Integration](#ffi-integration)
5. [Plugin Lifecycle](#plugin-lifecycle)
6. [Communication Patterns](#communication-patterns)
7. [Testing Plugins](#testing-plugins)
8. [Best Practices](#best-practices)
9. [Examples](#examples)

---

## Overview

Plugins are the core functionality units in COSMIC Connect. Each plugin handles a specific feature (clipboard sync, file transfer, notifications, etc.).

### Plugin Types

**1. Send-Only Plugins**
- Only send packets to remote device
- Example: Battery (only sends battery status)

**2. Receive-Only Plugins**
- Only receive and process packets
- Example: MouseReceiver (only receives mouse events)

**3. Bidirectional Plugins**
- Both send and receive packets
- Example: Clipboard (sends and receives clipboard data)

### Architecture Layers

```
┌────────────────────────────────────┐
│      Plugin Java/Kotlin Class      │ ← Android-specific implementation
├────────────────────────────────────┤
│     Plugin FFI Wrapper (Kotlin)    │ ← Type-safe Kotlin wrappers
├────────────────────────────────────┤
│       UniFFI Generated Bindings    │ ← Auto-generated FFI code
├────────────────────────────────────┤
│    Rust Plugin Core (cosmic-       │ ← Protocol implementation
│      connect-core/src/plugins)     │
└────────────────────────────────────┘
```

---

## Plugin Architecture

### Components

**1. Rust Plugin Core** (`cosmic-connect-core/src/plugins/`)
- Protocol logic
- Packet creation
- Data validation
- No Android dependencies

**2. FFI Interface** (`cosmic-connect-core/src/ffi/`)
- Export functions for packet creation
- Callback interfaces for plugin events
- Error handling

**3. Kotlin FFI Wrapper** (`src/org/cosmic/cosmicconnect/Core/`)
- Type-safe Kotlin API
- Extension properties for packet data
- Memory management

**4. Plugin Implementation** (`src/org/cosmic/cosmicconnect/Plugins/`)
- Android-specific logic
- UI integration
- Permissions handling
- Lifecycle management

---

## Creating a Plugin

### Step 1: Define Plugin Metadata

**In Rust** (`cosmic-connect-core/src/plugins/`):

```rust
// cosmic-connect-core/src/plugins/my_plugin.rs

pub const PLUGIN_KEY: &str = "kdeconnect.my_plugin";
pub const PLUGIN_NAME: &str = "My Plugin";
pub const PLUGIN_DESCRIPTION: &str = "Does something cool";

#[derive(Debug, Clone)]
pub struct MyPluginConfig {
    pub enabled: bool,
    pub setting1: String,
}

impl Default for MyPluginConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            setting1: String::from("default"),
        }
    }
}
```

### Step 2: Implement Packet Creation (Rust)

```rust
use crate::protocol::NetworkPacket;
use crate::error::ProtocolError;

pub fn create_my_plugin_packet(
    data: String,
    timestamp: i64
) -> Result<NetworkPacket, ProtocolError> {
    let mut packet = NetworkPacket::new(PLUGIN_KEY.to_string());

    packet.set_body("data", data);
    packet.set_body("timestamp", timestamp);

    Ok(packet)
}

pub fn parse_my_plugin_packet(
    packet: &NetworkPacket
) -> Result<MyPluginData, ProtocolError> {
    if packet.packet_type != PLUGIN_KEY {
        return Err(ProtocolError::InvalidPacketType {
            expected: PLUGIN_KEY.to_string(),
            actual: packet.packet_type.clone(),
        });
    }

    let data = packet.get_string("data")?;
    let timestamp = packet.get_i64("timestamp")?;

    Ok(MyPluginData { data, timestamp })
}

#[derive(Debug, Clone)]
pub struct MyPluginData {
    pub data: String,
    pub timestamp: i64,
}
```

### Step 3: Add FFI Exports

```rust
// cosmic-connect-core/src/ffi/mod.rs

use crate::ffi::types::{FfiPacket, ProtocolError};

#[uniffi::export]
pub fn create_my_plugin_packet(
    data: String,
    timestamp: i64
) -> Result<FfiPacket, ProtocolError> {
    let packet = crate::plugins::my_plugin::create_my_plugin_packet(
        data,
        timestamp
    )?;
    Ok(FfiPacket::from_packet(packet))
}
```

### Step 4: Create Kotlin FFI Wrapper

```kotlin
// src/org/cosmic/cosmicconnect/Core/MyPluginPacketsFFI.kt

package org.cosmic.cosmicconnect.Core

import uniffi.cosmic_connect_core.Core

object MyPluginPacketsFFI {
    /**
     * Creates a My Plugin packet.
     *
     * @param data The data to send
     * @param timestamp Unix timestamp
     * @return NetworkPacket for My Plugin
     */
    fun createMyPluginPacket(
        data: String,
        timestamp: Long = System.currentTimeMillis()
    ): NetworkPacket {
        val ffiPacket = Core.createMyPluginPacket(
            data = data,
            timestamp = timestamp
        )
        return NetworkPacket(ffiPacket)
    }
}

/**
 * Extension property to get data from My Plugin packet.
 */
val NetworkPacket.myPluginData: String?
    get() = getString("data")

/**
 * Extension property to get timestamp from My Plugin packet.
 */
val NetworkPacket.myPluginTimestamp: Long?
    get() = getLong("timestamp")

/**
 * Check if packet is a My Plugin packet.
 */
val NetworkPacket.isMyPluginPacket: Boolean
    get() = type == "kdeconnect.my_plugin"
```

### Step 5: Implement Plugin Class (Kotlin)

```kotlin
// src/org/cosmic/cosmicconnect/Plugins/MyPlugin/MyPlugin.kt

package org.cosmic.cosmicconnect.Plugins.MyPlugin

import android.content.Context
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Core.MyPluginPacketsFFI
import org.cosmic.cosmicconnect.Core.isMyPluginPacket
import org.cosmic.cosmicconnect.Core.myPluginData
import org.cosmic.cosmicconnect.Core.myPluginTimestamp

@LoadablePlugin
class MyPlugin : Plugin() {

    override val pluginKey: String = "kdeconnect.my_plugin"
    override val displayName: String = "My Plugin"
    override val description: String = "Does something cool"

    override fun onCreate(): Boolean {
        // Initialize plugin
        return true
    }

    override fun onDestroy() {
        // Cleanup resources
    }

    /**
     * Handle incoming packets.
     */
    override fun onPacketReceived(np: NetworkPacket): Boolean {
        if (!np.isMyPluginPacket) return false

        val data = np.myPluginData ?: return false
        val timestamp = np.myPluginTimestamp ?: return false

        // Process received data
        handleReceivedData(data, timestamp)

        return true
    }

    /**
     * Send data to remote device.
     */
    fun sendData(data: String) {
        val packet = MyPluginPacketsFFI.createMyPluginPacket(
            data = data
        )
        device?.sendPacket(packet)
    }

    private fun handleReceivedData(data: String, timestamp: Long) {
        // Process data
        // Update UI, store data, trigger actions, etc.
    }

    override fun checkRequiredPermissions(): Boolean {
        // Check if all required permissions are granted
        return true
    }

    override fun hasSettings(): Boolean {
        return true  // If plugin has settings activity
    }
}
```

### Step 6: Create Plugin Factory

```kotlin
// src/org/cosmic/cosmicconnect/Plugins/MyPlugin/MyPluginFactory.kt

package org.cosmic.cosmicconnect.Plugins.MyPlugin

import android.content.Context
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory

@LoadablePlugin
class MyPluginFactory : PluginFactory() {
    override val pluginKey: String = "kdeconnect.my_plugin"

    override fun create(context: Context, device: Device): Plugin {
        return MyPlugin()
    }

    override fun getSupportedPacketTypes(): Array<String> {
        return arrayOf("kdeconnect.my_plugin")
    }
}
```

---

## FFI Integration

### Type Mapping

**Rust → Kotlin:**
- `String` → `String`
- `i32, i64` → `Int, Long`
- `f32, f64` → `Float, Double`
- `bool` → `Boolean`
- `Option<T>` → `T?` (nullable)
- `Vec<T>` → `List<T>`
- `Result<T, E>` → throws exception on error

### Error Handling

**Rust:**
```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ProtocolError {
    #[error("Invalid packet type: expected {expected}, got {actual}")]
    InvalidPacketType { expected: String, actual: String },

    #[error("Missing required field: {0}")]
    MissingField(String),

    #[error("Invalid value for field {field}: {reason}")]
    InvalidValue { field: String, reason: String },
}
```

**Kotlin:**
```kotlin
try {
    val packet = MyPluginPacketsFFI.createMyPluginPacket(data)
    device.sendPacket(packet)
} catch (e: ProtocolException) {
    Log.e(TAG, "Failed to create packet: ${e.message}")
}
```

### Callback Interfaces

**For async operations or events:**

**Rust:**
```rust
#[uniffi::export(callback_interface)]
pub trait MyPluginCallback: Send + Sync {
    fn on_data_received(&self, data: String);
    fn on_error(&self, error: String);
}

pub struct MyPluginHandle {
    callback: Arc<dyn MyPluginCallback>,
}

#[uniffi::export]
impl MyPluginHandle {
    #[uniffi::constructor]
    pub fn new(callback: Box<dyn MyPluginCallback>) -> Self {
        Self {
            callback: Arc::new(callback),
        }
    }

    pub fn process(&self, data: String) {
        // Do work...
        self.callback.on_data_received(data);
    }
}
```

**Kotlin:**
```kotlin
class MyPluginCallbackImpl : MyPluginCallback {
    override fun onDataReceived(data: String) {
        // Handle data on UI thread if needed
        runOnUiThread {
            updateUI(data)
        }
    }

    override fun onError(error: String) {
        Log.e(TAG, "Error: $error")
    }
}

// Usage:
val callback = MyPluginCallbackImpl()
val handle = MyPluginHandle(callback)
handle.process("some data")
```

---

## Plugin Lifecycle

### Lifecycle Events

```
┌──────────────┐
│   Created    │ ← Plugin instance created
└──────┬───────┘
       │
       ▼
┌──────────────┐
│   onCreate() │ ← Initialize resources
└──────┬───────┘
       │
       ▼
┌──────────────┐
│    Active    │ ← Plugin receiving/sending packets
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ onDestroy()  │ ← Cleanup resources
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Destroyed   │
└──────────────┘
```

### Implementing Lifecycle

```kotlin
class MyPlugin : Plugin() {

    private var receiver: BroadcastReceiver? = null

    override fun onCreate(): Boolean {
        // Initialize resources
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                handleBroadcast(intent)
            }
        }

        // Register receivers
        context.registerReceiver(
            receiver,
            IntentFilter("com.example.ACTION")
        )

        return true
    }

    override fun onDestroy() {
        // Unregister receivers
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister receiver", e)
            }
        }
        receiver = null
    }
}
```

---

## Communication Patterns

### Pattern 1: Request-Response

**Requesting device sends:**
```kotlin
fun requestData() {
    val packet = MyPluginPacketsFFI.createRequest(
        requestId = generateId(),
        dataType = "user_info"
    )
    device.sendPacket(packet)
}

override fun onPacketReceived(np: NetworkPacket): Boolean {
    if (np.isMyPluginResponse) {
        val requestId = np.requestId
        val data = np.responseData
        handleResponse(requestId, data)
        return true
    }
    return false
}
```

**Responding device:**
```kotlin
override fun onPacketReceived(np: NetworkPacket): Boolean {
    if (np.isMyPluginRequest) {
        val requestId = np.requestId
        val dataType = np.requestDataType

        val data = fetchData(dataType)
        val response = MyPluginPacketsFFI.createResponse(
            requestId = requestId,
            data = data
        )
        device.sendPacket(response)
        return true
    }
    return false
}
```

### Pattern 2: Event Broadcasting

**Sender:**
```kotlin
fun broadcastEvent(event: String, data: String) {
    val packet = MyPluginPacketsFFI.createEvent(
        event = event,
        data = data
    )
    device.sendPacket(packet)
}
```

**Receiver:**
```kotlin
override fun onPacketReceived(np: NetworkPacket): Boolean {
    if (np.isMyPluginEvent) {
        val event = np.eventType
        val data = np.eventData

        when (event) {
            "user_logged_in" -> handleLogin(data)
            "data_updated" -> handleUpdate(data)
            else -> Log.w(TAG, "Unknown event: $event")
        }
        return true
    }
    return false
}
```

### Pattern 3: State Synchronization

**On state change:**
```kotlin
private var currentState: String = "idle"

private fun setState(newState: String) {
    currentState = newState
    syncState()
}

private fun syncState() {
    val packet = MyPluginPacketsFFI.createStateUpdate(
        state = currentState
    )
    device.sendPacket(packet)
}
```

**On receiving state:**
```kotlin
override fun onPacketReceived(np: NetworkPacket): Boolean {
    if (np.isMyPluginStateUpdate) {
        val remoteState = np.state
        updateLocalState(remoteState)
        return true
    }
    return false
}
```

---

## Testing Plugins

### Unit Tests

```kotlin
@Test
fun testPacketCreation() {
    val data = "test data"
    val timestamp = System.currentTimeMillis()

    val packet = MyPluginPacketsFFI.createMyPluginPacket(
        data = data,
        timestamp = timestamp
    )

    assertEquals("kdeconnect.my_plugin", packet.type)
    assertEquals(data, packet.myPluginData)
    assertEquals(timestamp, packet.myPluginTimestamp)
}

@Test
fun testPacketProcessing() {
    val plugin = MyPlugin()
    plugin.onCreate()

    val packet = MyPluginPacketsFFI.createMyPluginPacket("test")
    val handled = plugin.onPacketReceived(packet)

    assertTrue(handled)

    plugin.onDestroy()
}
```

### Integration Tests

```kotlin
@Test
fun testPluginCommunication() = runTest {
    // Setup mock devices
    val device1 = createMockDevice("device1")
    val device2 = createMockDevice("device2")

    val plugin1 = MyPlugin().apply {
        setDevice(device1)
        onCreate()
    }

    val plugin2 = MyPlugin().apply {
        setDevice(device2)
        onCreate()
    }

    // Send from plugin1
    plugin1.sendData("test data")

    // Receive on plugin2
    val packet = device1.lastSentPacket
    plugin2.onPacketReceived(packet)

    // Verify
    assertEquals("test data", plugin2.lastReceivedData)
}
```

---

## Best Practices

### 1. Always Use FFI for Packet Creation

**❌ Don't:**
```kotlin
val packet = NetworkPacket("kdeconnect.my_plugin")
packet.set("data", "test")  // Mutable packet
```

**✅ Do:**
```kotlin
val packet = MyPluginPacketsFFI.createMyPluginPacket(
    data = "test"
)  // Immutable packet from FFI
```

### 2. Use Extension Properties

**❌ Don't:**
```kotlin
val data = packet.getString("data")
val timestamp = packet.getLong("timestamp")
```

**✅ Do:**
```kotlin
val data = packet.myPluginData
val timestamp = packet.myPluginTimestamp
```

### 3. Handle Errors Gracefully

```kotlin
override fun onPacketReceived(np: NetworkPacket): Boolean {
    try {
        if (!np.isMyPluginPacket) return false

        val data = np.myPluginData ?: run {
            Log.w(TAG, "Missing data in packet")
            return false
        }

        processData(data)
        return true

    } catch (e: Exception) {
        Log.e(TAG, "Error processing packet", e)
        return false
    }
}
```

### 4. Clean Up Resources

```kotlin
override fun onDestroy() {
    // Cancel ongoing operations
    job?.cancel()

    // Unregister receivers
    receiver?.let { context.unregisterReceiver(it) }

    // Clear callbacks
    callbacks.clear()

    // Close connections
    connection?.close()
}
```

### 5. Use Coroutines for Async Work

```kotlin
class MyPlugin : Plugin() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun doAsyncWork() {
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Long-running work
                    fetchData()
                }
                updateUI(result)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
    }
}
```

### 6. Respect Permissions

```kotlin
override fun checkRequiredPermissions(): Boolean {
    val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    return permissions.all { permission ->
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

---

## Examples

### Example 1: Simple Status Plugin

**Rust:**
```rust
pub fn create_status_packet(status: String) -> Result<NetworkPacket, ProtocolError> {
    let mut packet = NetworkPacket::new("kdeconnect.status".to_string());
    packet.set_body("status", status);
    Ok(packet)
}
```

**FFI:**
```rust
#[uniffi::export]
pub fn create_status_packet(status: String) -> Result<FfiPacket, ProtocolError> {
    let packet = crate::plugins::status::create_status_packet(status)?;
    Ok(FfiPacket::from_packet(packet))
}
```

**Kotlin Wrapper:**
```kotlin
object StatusPacketsFFI {
    fun createStatusPacket(status: String): NetworkPacket {
        val ffiPacket = Core.createStatusPacket(status)
        return NetworkPacket(ffiPacket)
    }
}

val NetworkPacket.status: String?
    get() = getString("status")
```

**Plugin:**
```kotlin
class StatusPlugin : Plugin() {
    override val pluginKey = "kdeconnect.status"

    fun sendStatus(status: String) {
        val packet = StatusPacketsFFI.createStatusPacket(status)
        device?.sendPacket(packet)
    }

    override fun onPacketReceived(np: NetworkPacket): Boolean {
        val status = np.status ?: return false
        showNotification("Device status: $status")
        return true
    }
}
```

### Example 2: Data Sync Plugin

See complete example in [docs/protocol/ping-ffi-integration.md](../protocol/ping-ffi-integration.md)

---

## Additional Resources

- [FFI Integration Guide](FFI_INTEGRATION_GUIDE.md)
- [Architecture Documentation](../architecture/ARCHITECTURE.md)
- [Plugin Examples](../../src/org/cosmic/cosmicconnect/Plugins/)
- [Testing Guide](CONTRIBUTING.md#testing)

---

<div align="center">

**Happy Plugin Development!**

*Building the future of device connectivity*

</div>
