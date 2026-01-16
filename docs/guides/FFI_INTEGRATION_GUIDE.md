# FFI Integration Guide

> Last Updated: 2026-01-16
> Status: Production Patterns
> Based on: PingPluginFFI.kt successful implementation

## Overview

This guide documents the proven patterns for integrating Rust FFI wrappers into the existing Android plugin codebase. It's based on the successful PingPluginFFI.kt implementation, which serves as a template for migrating other plugins.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Integration Pattern](#integration-pattern)
3. [Step-by-Step Migration](#step-by-step-migration)
4. [Code Patterns](#code-patterns)
5. [Testing Strategy](#testing-strategy)
6. [Common Pitfalls](#common-pitfalls)
7. [Migration Checklist](#migration-checklist)

---

## Architecture Overview

### Hybrid Architecture

The integration maintains backward compatibility while introducing FFI functionality:

```
┌─────────────────────────────────────────────────────────┐
│            Android Plugin (e.g., PingPluginFFI)         │
│                                                          │
│  - Extends old Plugin class                             │
│  - Implements same API surface                          │
│  - Handles Android-specific functionality               │
│  - Manages notifications, permissions, UI               │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
            Packet Conversion (Legacy ↔ FFI)
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│              PluginManagerProvider                      │
│                                                          │
│  - Singleton access to PluginManager                    │
│  - Shared across all plugins                            │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│         FFI Wrapper Layer (Core/)                       │
│                                                          │
│  - NetworkPacket (data class)                           │
│  - PluginManager (plugin routing, stats)                │
│  - Clean Kotlin API                                     │
└─────────────────────────────────────────────────────────┘
                          ↓ ↑
┌─────────────────────────────────────────────────────────┐
│         Rust Core (libcosmic_connect_core.so)           │
│                                                          │
│  - Protocol implementation                              │
│  - Plugin logic                                         │
│  - Cross-platform compatibility                         │
└─────────────────────────────────────────────────────────┘
```

### Key Principles

1. **Drop-in Replacement**: FFI plugin extends old Plugin class, maintaining same API
2. **Graceful Fallback**: Continue working if FFI unavailable
3. **Shared State**: PluginManager singleton shared across all plugins
4. **Type Conversion**: Explicit conversion between legacy and FFI types
5. **Android Integration**: Keep Android-specific code in plugin (notifications, permissions, UI)

---

## Integration Pattern

### 1. Dual Import Pattern

Use type aliases to distinguish between legacy and FFI types:

```kotlin
import org.cosmic.cosmicconnect.Core.NetworkPacket           // FFI wrapper
import org.cosmic.cosmicconnect.Core.PluginManager            // FFI wrapper
import org.cosmic.cosmicconnect.Core.PluginManagerProvider    // Singleton provider
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket  // Old class
```

**Why**: Avoids naming conflicts and makes conversion points explicit.

### 2. Plugin Manager Access

Get the shared PluginManager instance in onCreate():

```kotlin
class YourPluginFFI : Plugin() {
    private var pluginManager: PluginManager? = null

    override fun onCreate(): Boolean {
        try {
            // Get shared plugin manager instance
            pluginManager = PluginManagerProvider.getInstance()

            // Register plugin with FFI
            if (!pluginManager!!.hasPlugin(PluginManager.Plugins.YOUR_PLUGIN)) {
                pluginManager!!.registerPlugin(PluginManager.Plugins.YOUR_PLUGIN)
                Log.i(TAG, "Registered plugin with FFI")
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize plugin", e)
            return false
        }
    }
}
```

**Key Points**:
- Nullable `pluginManager` for safe access
- Check if plugin already registered (idempotent)
- Catch and log errors, but continue if possible
- Shared instance across all plugins

### 3. Packet Conversion

Implement bidirectional conversion methods:

```kotlin
/**
 * Convert legacy NetworkPacket to FFI NetworkPacket
 */
private fun convertLegacyPacket(legacy: LegacyNetworkPacket): NetworkPacket {
    val body = mutableMapOf<String, Any>()

    // Extract all fields from legacy packet
    if (legacy.has("field1")) {
        body["field1"] = legacy.getString("field1")
    }
    if (legacy.has("field2")) {
        body["field2"] = legacy.getLong("field2")
    }
    // ... add all packet fields

    return NetworkPacket.create(PACKET_TYPE, body)
}

/**
 * Convert FFI NetworkPacket to legacy NetworkPacket
 */
private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
    val legacy = LegacyNetworkPacket(PACKET_TYPE)

    // Copy all fields to legacy packet
    val field1 = ffi.body["field1"]
    if (field1 is String) {
        legacy.set("field1", field1)
    }

    val field2 = ffi.body["field2"]
    if (field2 is Long || field2 is Int) {
        legacy.set("field2", field2)
    }
    // ... add all packet fields

    return legacy
}
```

**Key Points**:
- Type-safe conversion with explicit checks
- Handle missing fields gracefully
- Support multiple types (String, Long, Int, Boolean, etc.)
- Document packet structure in comments

### 4. Receiving Packets

Route incoming packets to FFI, then handle Android-specific actions:

```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    if (np.type != PACKET_TYPE) {
        Log.e(TAG, "Plugin should not receive packets other than $PACKET_TYPE")
        return false
    }

    try {
        // Convert legacy packet to FFI
        val ffiPacket = convertLegacyPacket(np)

        // Route to FFI plugin manager (updates stats, processes logic)
        pluginManager?.routePacket(ffiPacket)

        // Handle Android-specific actions (notifications, UI updates, etc.)
        handleAndroidActions(np)

        return true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to process packet", e)
        // Still try to handle Android actions even if FFI fails
        handleAndroidActions(np)
        return true
    }
}
```

**Key Points**:
- Convert to FFI first
- Route through PluginManager for statistics and cross-platform logic
- Handle Android-specific actions separately
- Graceful degradation if FFI fails

### 5. Sending Packets

Create packets via FFI, then send via legacy device:

```kotlin
fun sendAction(param1: String?, param2: Int?) {
    if (!isDeviceInitialized) {
        Log.w(TAG, "Device not initialized")
        return
    }

    try {
        // Create packet via FFI
        val ffiPacket = pluginManager?.createPacket(param1, param2)

        if (ffiPacket != null) {
            // Convert to legacy for device.sendPacket()
            val legacyPacket = convertToLegacyPacket(ffiPacket)
            device.sendPacket(legacyPacket)

            Log.d(TAG, "Packet sent via FFI")
        } else {
            // Fallback: create packet manually
            val legacyPacket = LegacyNetworkPacket(PACKET_TYPE)
            if (param1 != null) legacyPacket.set("param1", param1)
            if (param2 != null) legacyPacket.set("param2", param2)
            device.sendPacket(legacyPacket)

            Log.w(TAG, "Packet sent (FFI unavailable, used fallback)")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send packet", e)
    }
}
```

**Key Points**:
- Use FFI to create packet (ensures correct format)
- Convert back to legacy for device.sendPacket()
- Fallback to manual creation if FFI fails
- Log which path was taken

### 6. Statistics and State

Access plugin state via FFI:

```kotlin
fun getPluginStats(): PluginStats? {
    return try {
        pluginManager?.getPluginStats()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get stats", e)
        null
    }
}
```

**Key Points**:
- Return nullable type for safe access
- Catch and log exceptions
- Document what stats are available

---

## Step-by-Step Migration

### Phase 1: Preparation (30 minutes)

1. **Read the old plugin implementation**
   - Understand all packet types
   - Document all fields in each packet
   - Note any complex logic

2. **Check FFI wrapper availability**
   - Does `PluginManager` support this plugin?
   - Are all packet types defined in `PacketType` object?
   - Are FFI functions available for this plugin?

3. **Create migration plan**
   - List all methods to migrate
   - Identify Android-specific code to preserve
   - Plan testing approach

### Phase 2: Create FFI Plugin Class (1-2 hours)

1. **Create new plugin file**
   ```
   src/org/cosmic/cosmicconnect/Plugins/[PluginName]/[PluginName]FFI.kt
   ```

2. **Add class structure**
   ```kotlin
   @LoadablePlugin
   class YourPluginFFI : Plugin() {
       companion object {
           private const val TAG = "YourPluginFFI"
           const val PACKET_TYPE = "kdeconnect.your.packet"
       }

       private var pluginManager: PluginManager? = null

       override val displayName: String
           get() = context.resources.getString(R.string.pref_plugin_your_plugin)

       override val description: String
           get() = context.resources.getString(R.string.pref_plugin_your_plugin_desc)

       // ... implement methods
   }
   ```

3. **Implement onCreate()**
   - Get PluginManager instance
   - Register plugin
   - Initialize Android components

4. **Implement onDestroy()**
   - Clean up Android resources
   - Note: Don't unregister plugin (shared instance)

### Phase 3: Implement Conversion Methods (1-2 hours)

1. **Document packet structure**
   ```kotlin
   /**
    * Packet structure for kdeconnect.your.packet:
    * {
    *   "field1": String - description
    *   "field2": Long - description (optional)
    *   "field3": Boolean - description
    * }
    */
   ```

2. **Implement convertLegacyPacket()**
   - Extract all fields
   - Handle optional fields
   - Type conversion

3. **Implement convertToLegacyPacket()**
   - Set all fields
   - Type checking
   - Handle missing fields

### Phase 4: Implement Packet Handling (2-3 hours)

1. **Implement onPacketReceived()**
   - Convert to FFI
   - Route through PluginManager
   - Handle Android actions
   - Error handling with fallback

2. **Implement sending methods**
   - Create via FFI
   - Convert to legacy
   - Fallback if FFI unavailable

3. **Preserve Android integration**
   - Notifications
   - Permissions
   - UI updates
   - Intents

### Phase 5: Testing (1-2 hours)

1. **Unit tests**
   - Packet conversion round-trip
   - Error handling
   - Fallback behavior

2. **Integration tests**
   - Send/receive packets
   - Cross-platform compatibility
   - Statistics tracking

3. **Manual testing**
   - Test with real device
   - Verify notifications
   - Check performance

---

## Code Patterns

### Pattern 1: Safe FFI Access

```kotlin
// Good: Safe nullable access
val result = pluginManager?.someMethod() ?: fallbackValue

// Good: Try-catch with fallback
try {
    pluginManager?.routePacket(packet)
} catch (e: Exception) {
    Log.e(TAG, "FFI failed, using fallback", e)
    handlePacketManually(packet)
}

// Bad: Assuming FFI always works
pluginManager!!.routePacket(packet)  // May crash if FFI unavailable
```

### Pattern 2: Type-Safe Conversion

```kotlin
// Good: Explicit type checking
val field = ffi.body["field"]
if (field is String) {
    legacy.set("field", field)
} else if (field is Int) {
    legacy.set("field", field.toLong())
}

// Bad: Unsafe casting
val field = ffi.body["field"] as String  // May crash if wrong type
```

### Pattern 3: Logging Strategy

```kotlin
// Good: Log at appropriate levels
Log.d(TAG, "Packet sent successfully")           // Debug: normal operation
Log.w(TAG, "FFI unavailable, using fallback")    // Warning: degraded mode
Log.e(TAG, "Failed to process packet", e)        // Error: something wrong

// Good: Use emoji for visibility
Log.i(TAG, "✅ Plugin initialized")
Log.e(TAG, "❌ Failed to initialize plugin")
```

### Pattern 4: Error Recovery

```kotlin
// Good: Multi-level fallback
try {
    // Try FFI first
    val packet = pluginManager?.createPacket(data)
    if (packet != null) {
        device.sendPacket(convertToLegacyPacket(packet))
    } else {
        // Fallback: manual creation
        device.sendPacket(createPacketManually(data))
    }
} catch (e: Exception) {
    // Last resort: log and notify user
    Log.e(TAG, "Failed to send packet", e)
    showErrorNotification()
}
```

---

## Testing Strategy

### Unit Tests

Create tests for conversion and logic:

```kotlin
@Test
fun testPacketConversion() {
    // Test legacy → FFI → legacy round-trip
    val original = LegacyNetworkPacket("kdeconnect.ping")
    original.set("message", "Hello")

    val ffi = convertLegacyPacket(original)
    val converted = convertToLegacyPacket(ffi)

    assertEquals(original.type, converted.type)
    assertEquals(original.getString("message"), converted.getString("message"))
}

@Test
fun testFallbackBehavior() {
    // Test plugin works without FFI
    val plugin = YourPluginFFI()
    plugin.pluginManager = null  // Simulate FFI unavailable

    // Should not crash
    plugin.sendAction("test", 42)
}
```

### Integration Tests

Test cross-platform communication:

```kotlin
@Test
fun testSendReceivePacket() {
    // Create plugin
    val plugin = YourPluginFFI()
    plugin.setContext(context, device)
    plugin.onCreate()

    // Send packet
    plugin.sendAction("test", 42)

    // Simulate receiving response
    val response = createResponsePacket()
    val result = plugin.onPacketReceived(response)

    assertTrue(result)
}
```

### Manual Testing Checklist

- [ ] Plugin initializes without errors
- [ ] Can send packets to remote device
- [ ] Can receive packets from remote device
- [ ] Notifications display correctly
- [ ] Statistics tracked accurately (if applicable)
- [ ] Works without FFI (fallback mode)
- [ ] No memory leaks or crashes
- [ ] Performance acceptable

---

## Common Pitfalls

### Pitfall 1: Assuming FFI Always Available

**Problem**: Code crashes if FFI initialization fails

**Solution**: Always use nullable types and provide fallbacks
```kotlin
// Bad
pluginManager!!.routePacket(packet)

// Good
pluginManager?.routePacket(packet) ?: handlePacketManually(packet)
```

### Pitfall 2: Type Mismatches

**Problem**: Packet body fields have wrong types (e.g., Int vs Long)

**Solution**: Explicit type checking and conversion
```kotlin
val field = ffi.body["count"]
when (field) {
    is Int -> legacy.set("count", field.toLong())
    is Long -> legacy.set("count", field)
    else -> Log.w(TAG, "Unexpected type for count: ${field?.javaClass}")
}
```

### Pitfall 3: Missing Error Handling

**Problem**: Exceptions in FFI calls crash the app

**Solution**: Wrap all FFI calls in try-catch
```kotlin
try {
    pluginManager?.routePacket(packet)
    handleAndroidActions(packet)
} catch (e: Exception) {
    Log.e(TAG, "Failed to process packet", e)
    // Continue with Android actions anyway
    handleAndroidActions(packet)
}
```

### Pitfall 4: Shared State Issues

**Problem**: Unregistering plugin in onDestroy() breaks other devices

**Solution**: Don't unregister from shared PluginManager
```kotlin
override fun onDestroy() {
    // Good: Only clean up device-specific resources
    cleanupAndroidResources()

    // Bad: Don't do this (breaks other devices)
    // pluginManager?.unregisterPlugin(PluginManager.Plugins.YOUR_PLUGIN)
}
```

### Pitfall 5: Incomplete Packet Conversion

**Problem**: Some fields not copied during conversion

**Solution**: Document packet structure and test round-trips
```kotlin
/**
 * Complete packet structure:
 * - field1: String (required)
 * - field2: Long (optional)
 * - field3: Boolean (optional)
 * - field4: Array<String> (optional)
 */
private fun convertLegacyPacket(legacy: LegacyNetworkPacket): NetworkPacket {
    // Convert ALL fields, not just the common ones
}
```

---

## Migration Checklist

### Pre-Migration

- [ ] Read and understand old plugin implementation
- [ ] Document all packet types and fields
- [ ] Verify FFI support for this plugin
- [ ] Check PluginManager has required methods
- [ ] Review PingPluginFFI.kt example

### Implementation

- [ ] Create new PluginFFI.kt file
- [ ] Add dual imports (FFI + Legacy)
- [ ] Implement onCreate() with PluginManager access
- [ ] Implement onDestroy() (cleanup only, no unregister)
- [ ] Create convertLegacyPacket() method
- [ ] Create convertToLegacyPacket() method
- [ ] Implement onPacketReceived() with FFI routing
- [ ] Implement send methods with FFI creation
- [ ] Add fallback behavior for FFI unavailable
- [ ] Preserve all Android-specific functionality
- [ ] Add comprehensive logging

### Testing

- [ ] Unit tests for packet conversion
- [ ] Unit tests for fallback behavior
- [ ] Integration tests for send/receive
- [ ] Manual testing with real device
- [ ] Performance profiling
- [ ] Memory leak check
- [ ] Test without FFI (fallback mode)

### Documentation

- [ ] Add KDoc comments to all methods
- [ ] Document packet structure
- [ ] Update plugin README (if exists)
- [ ] Add to migration progress tracking

### Deployment

- [ ] Code review
- [ ] Update PluginFactory to load new plugin
- [ ] Test on multiple Android versions
- [ ] Monitor crash reports
- [ ] Gradual rollout (if possible)

---

## Migration Progress Tracking

### Plugins Status

| Plugin | Status | FFI Support | Complexity | Notes |
|--------|--------|-------------|------------|-------|
| Ping | ✅ Complete | ✅ Yes | Low | Reference implementation |
| Battery | ✅ Ready | ✅ Yes | Low | FFI available, needs integration |
| Share | 📋 Planned | ⚠️ Needs Device refactoring | High | Issue #53 |
| Clipboard | 🔜 Future | ⚠️ Needs Device refactoring | Medium | Issue #54 |
| Notification | 🔜 Future | ⚠️ Needs Device refactoring | Medium | Blocked |
| Telephony | 🔜 Future | ⚠️ Needs Device refactoring | High | Blocked |
| Contacts | 🔜 Future | ⚠️ Needs Device refactoring | Medium | Blocked |
| SFTP | 🔜 Future | ❌ Not yet | High | Requires file system access |
| RunCommand | 🔜 Future | ❌ Not yet | Medium | Requires command storage |

### Next Plugin to Migrate: Battery

The Battery plugin is the ideal next candidate because:
- FFI support already exists in PluginManager
- Similar complexity to Ping (low)
- No payload transfers or complex state
- Simple packet structure

---

## Additional Resources

### Reference Implementations

- **PingPluginFFI.kt** - Production example (see: `src/org/cosmic/cosmicconnect/Plugins/PingPlugin/PingPluginFFI.kt`)
- **Issue #52 Status** - FFI wrapper layer details (see: `docs/issues/issue-52-wrapper-status.md`)
- **Issue #53 Plan** - Share plugin migration plan (see: `docs/issues/issue-53-share-plugin-plan.md`)

### FFI Wrapper Documentation

- **CosmicConnectCore** - Singleton initialization (see: `src/org/cosmic/cosmicconnect/Core/CosmicConnectCore.kt`)
- **NetworkPacket** - Packet wrapper API (see: `src/org/cosmic/cosmicconnect/Core/NetworkPacket.kt`)
- **PluginManager** - Plugin routing and stats (see: `src/org/cosmic/cosmicconnect/Core/PluginManager.kt`)

### Testing Resources

- **FFIValidationTest.kt** - FFI infrastructure tests (see: `tests/org/cosmic/cosmicconnect/FFIValidationTest.kt`)
- **Standalone Validation** - FFI validation script (see: `scripts/validate-ffi.sh`)

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Based On**: PingPluginFFI.kt production implementation
**Status**: Ready for use in plugin migrations
