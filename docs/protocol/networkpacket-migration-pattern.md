# NetworkPacket Migration Pattern

**Status**: Template ✅
**Created**: 2025-01-15
**Related Issue**: Part of #64 (Plugin Refactoring for Immutable NetworkPacket)

## Summary

This document provides the migration pattern for converting plugins from the old mutable `NetworkPacket` to the new immutable `Core.NetworkPacket` (from FFI). This is a key step in Issue #64, separate from full FFI plugin integration.

---

## Why Migrate?

### Old Pattern (Mutable NetworkPacket)
```kotlin
import org.cosmic.cconnect.NetworkPacket

// Mutable - can be modified after creation
val packet = NetworkPacket("cconnect.ping")
packet.set("message", "Hello")  // ❌ Mutability can cause bugs
device.sendPacket(packet)
```

### New Pattern (Immutable NetworkPacket)
```kotlin
import org.cosmic.cconnect.Core.NetworkPacket
import org.cosmic.cconnect.NetworkPacket as LegacyNetworkPacket

// Immutable - thread-safe, no accidental modifications
val packet = NetworkPacket.create("cconnect.ping", mapOf(
    "message" to "Hello"
))  // ✅ Created with all data upfront

// Convert to legacy for Device.sendPacket()
val legacyPacket = convertToLegacyPacket(packet)
device.sendPacket(legacyPacket)
```

**Benefits**:
- ✅ **Immutability**: Thread-safe, no accidental modifications
- ✅ **FFI Compatible**: Works with Rust cosmic-connect-core
- ✅ **Type Safety**: Kotlin data classes instead of JSON manipulation
- ✅ **Cross-Platform**: Same packet structure on Android and COSMIC Desktop
- ✅ **Gradual Migration**: Can coexist with legacy code via type alias

---

## Migration Steps

### Step 1: Update Imports

**Before**:
```kotlin
import org.cosmic.cconnect.NetworkPacket
```

**After**:
```kotlin
import org.cosmic.cconnect.Core.NetworkPacket
import org.cosmic.cconnect.NetworkPacket as LegacyNetworkPacket
```

**Why**:
- Import the new immutable `NetworkPacket` from `Core` package
- Create type alias `LegacyNetworkPacket` for old code that still needs it
- Allows gradual migration without breaking existing API contracts

---

### Step 2: Update Method Signatures

**Before**:
```kotlin
override fun onPacketReceived(np: NetworkPacket): Boolean {
    // ...
}
```

**After**:
```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    // ... (implementation unchanged for now)
}
```

**Why**:
- The `Plugin` interface still expects `LegacyNetworkPacket`
- This maintains compatibility with existing device infrastructure
- Future work: Update `Plugin` interface to accept both types

---

### Step 3: Create Packets Immutably

#### Simple Packet (No Body Data)

**Before**:
```kotlin
device.sendPacket(NetworkPacket("cconnect.ping"))
```

**After**:
```kotlin
// Create immutable packet
val packet = NetworkPacket.create("cconnect.ping", emptyMap())

// Convert to legacy for Device.sendPacket()
val legacyPacket = LegacyNetworkPacket(packet.type)
device.sendPacket(legacyPacket)
```

#### Packet with Data

**Before**:
```kotlin
val np = NetworkPacket("cconnect.battery")
np.set("currentCharge", 75)
np.set("isCharging", true)
np.set("thresholdEvent", 0)
device.sendPacket(np)
```

**After**:
```kotlin
// Create immutable packet with all data upfront
val packet = NetworkPacket.create("cconnect.battery", mapOf(
    "currentCharge" to 75,
    "isCharging" to true,
    "thresholdEvent" to 0
))

// Convert to legacy
val legacyPacket = convertToLegacyPacket(packet)
device.sendPacket(legacyPacket)
```

---

### Step 4: Add Conversion Helpers (If Needed)

If your plugin sends multiple packets, add these helper methods:

```kotlin
/**
 * Convert immutable NetworkPacket to legacy NetworkPacket for sending
 */
private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
    val legacy = LegacyNetworkPacket(ffi.type)

    // Copy all body fields
    ffi.body.forEach { (key, value) ->
        when (value) {
            is String -> legacy.set(key, value)
            is Int -> legacy.set(key, value)
            is Long -> legacy.set(key, value)
            is Boolean -> legacy.set(key, value)
            is Double -> legacy.set(key, value)
            // Add other types as needed
        }
    }

    return legacy
}

/**
 * Convert legacy NetworkPacket to immutable NetworkPacket for FFI
 */
private fun convertLegacyPacket(legacy: LegacyNetworkPacket): NetworkPacket {
    val body = mutableMapOf<String, Any>()

    // Extract known fields from legacy packet
    if (legacy.has("currentCharge")) {
        body["currentCharge"] = legacy.getInt("currentCharge", 0)
    }
    // ... add other fields

    return NetworkPacket.create(legacy.type, body)
}
```

**Note**: Only add these if your plugin handles complex packet conversions. Simple plugins can inline the conversion.

---

## Example Migration: FindRemoteDevicePlugin

### Before Migration

**File**: `FindRemoteDevicePlugin.kt` (33 lines)

```kotlin
package org.cosmic.cconnect.Plugins.FindRemoteDevicePlugin

import org.cosmic.cconnect.NetworkPacket  // ❌ Old mutable
import org.cosmic.cconnect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmic.cconnect.Plugins.Plugin
import org.cosmic.cconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cconnect.R

@LoadablePlugin
class FindRemoteDevicePlugin : Plugin() {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice_desc)

    override fun onPacketReceived(np: NetworkPacket): Boolean = true  // ❌ Old type

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.ring)) { parentActivity ->
            // ❌ Old mutable packet creation
            device.sendPacket(NetworkPacket(FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST))
        }
    )

    override val supportedPacketTypes: Array<String> = emptyArray()
    override val outgoingPacketTypes: Array<String> = arrayOf(FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST)
}
```

### After Migration

**File**: `FindRemoteDevicePlugin.kt` (41 lines)

```kotlin
package org.cosmic.cconnect.Plugins.FindRemoteDevicePlugin

import org.cosmic.cconnect.Core.NetworkPacket  // ✅ New immutable
import org.cosmic.cconnect.NetworkPacket as LegacyNetworkPacket  // ✅ Type alias
import org.cosmic.cconnect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmic.cconnect.Plugins.Plugin
import org.cosmic.cconnect.Plugins.PluginFactory.LoadablePlugin
import org.cosmic.cconnect.R

@LoadablePlugin
class FindRemoteDevicePlugin : Plugin() {
    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_findremotedevice_desc)

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean = true  // ✅ Legacy type

    override fun getUiMenuEntries(): List<PluginUiMenuEntry> = listOf(
        PluginUiMenuEntry(context.getString(R.string.ring)) { parentActivity ->
            // ✅ Create immutable NetworkPacket via FFI
            val packet = NetworkPacket.create(
                FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST,
                emptyMap()
            )

            // ✅ Convert to legacy packet for Device.sendPacket()
            val legacyPacket = LegacyNetworkPacket(packet.type)
            device.sendPacket(legacyPacket)
        }
    )

    override val supportedPacketTypes: Array<String> = emptyArray()
    override val outgoingPacketTypes: Array<String> = arrayOf(FindMyPhonePlugin.PACKET_TYPE_FINDMYPHONE_REQUEST)
}
```

**Changes**:
- ✅ Import new immutable `NetworkPacket` from Core
- ✅ Added type alias for `LegacyNetworkPacket`
- ✅ Updated method signature to use `LegacyNetworkPacket`
- ✅ Create packet immutably with `NetworkPacket.create()`
- ✅ Convert to legacy for sending

**Impact**: +8 lines, improved type safety and FFI compatibility

---

## Migration Checklist

For each plugin migration:

- [ ] Update imports (add `Core.NetworkPacket` and type alias)
- [ ] Update method signatures (`NetworkPacket` → `LegacyNetworkPacket`)
- [ ] Replace mutable packet creation with `NetworkPacket.create()`
- [ ] Add conversion logic (inline or helper methods)
- [ ] Test packet sending/receiving still works
- [ ] Verify no compilation errors
- [ ] Document any plugin-specific patterns

---

## Plugins to Migrate

From Issue #64, these plugins still use old mutable NetworkPacket:

### Simple Plugins (Like FindRemoteDevice)
- [x] FindRemoteDevicePlugin (✅ Migrated - 33 lines)
- [ ] ConnectivityReportPlugin (93 lines)
- [ ] PresenterPlugin (check complexity)

### Medium Plugins
- [ ] ClipboardPlugin
- [ ] MousePadPlugin
- [ ] DigitizerPlugin
- [ ] SystemVolumePlugin
- [ ] RemoteKeyboardPlugin

### Complex Plugins (May need more work)
- [ ] SharePlugin (5 files, file transfers)
- [ ] NotificationsPlugin (complex state)
- [ ] SMSPlugin (multiple packet types)
- [ ] TelephonyPlugin (call handling)
- [ ] RunCommandPlugin (command storage)
- [ ] MprisPlugin (media control)
- [ ] SftpPlugin (file system access)

**Note**: Battery and Ping already have full FFI implementations (BatteryPluginFFI, PingPluginFFI) and don't need this pattern.

---

## Common Patterns

### Pattern 1: Simple Request (No Data)

**Use Case**: Send a trigger packet with no body data

```kotlin
// Immutable creation
val packet = NetworkPacket.create(PACKET_TYPE, emptyMap())

// Convert and send
val legacyPacket = LegacyNetworkPacket(packet.type)
device.sendPacket(legacyPacket)
```

**Examples**: FindRemoteDevice, PresenterPlugin

---

### Pattern 2: Packet with Fixed Fields

**Use Case**: Packet with known structure (battery, ping, etc.)

```kotlin
// Create with all fields upfront
val packet = NetworkPacket.create(PACKET_TYPE, mapOf(
    "field1" to value1,
    "field2" to value2
))

// Convert helper
private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
    val legacy = LegacyNetworkPacket(ffi.type)
    ffi.body["field1"]?.let { legacy.set("field1", it) }
    ffi.body["field2"]?.let { legacy.set("field2", it) }
    return legacy
}
```

**Examples**: BatteryPluginFFI, ConnectivityReport

---

### Pattern 3: Conditional Fields

**Use Case**: Packet where some fields are optional

```kotlin
// Build map conditionally
val body = mutableMapOf<String, Any>()
body["requiredField"] = value

if (optionalValue != null) {
    body["optionalField"] = optionalValue
}

// Create immutably
val packet = NetworkPacket.create(PACKET_TYPE, body.toMap())
```

**Examples**: PingPlugin (optional message)

---

### Pattern 4: Receiving and Processing

**Use Case**: Receive legacy packet, convert to FFI for processing

```kotlin
override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
    // Convert to immutable for processing
    val ffiPacket = convertLegacyPacket(np)

    // Process with FFI or local logic
    processPacket(ffiPacket)

    return true
}

private fun convertLegacyPacket(legacy: LegacyNetworkPacket): NetworkPacket {
    val body = mutableMapOf<String, Any>()

    if (legacy.has("field1")) {
        body["field1"] = legacy.getString("field1")
    }

    return NetworkPacket.create(legacy.type, body)
}
```

---

## Future Work

### Phase 1: Individual Plugin Migration (Current)
- Migrate each plugin to use immutable `NetworkPacket` internally
- Keep `Plugin` interface using `LegacyNetworkPacket`
- Conversion at plugin boundary

### Phase 2: Device Infrastructure Update
- Update `Device.sendPacket()` to accept both types
- Update `Plugin` interface to use immutable `NetworkPacket`
- Remove conversion helpers

### Phase 3: Remove Legacy Code
- Delete old mutable `NetworkPacket` class
- Remove all `LegacyNetworkPacket` type aliases
- Full immutability across codebase

---

## Testing Strategy

For each migrated plugin:

1. **Build Test**: `./gradlew build` - verify compilation
2. **Manual Test**:
   - Install on device
   - Test plugin functionality (send packet)
   - Verify remote device receives correctly
3. **Integration Test**: Test with COSMIC Desktop applet
4. **Regression Test**: Verify no existing functionality broken

---

## Benefits Summary

**Immediate Benefits**:
- ✅ Thread-safe packet handling
- ✅ Type safety (Kotlin data classes)
- ✅ FFI compatibility foundation
- ✅ Prevents accidental packet modifications

**Long-Term Benefits**:
- ✅ Easier full FFI plugin migration later
- ✅ Cross-platform consistency
- ✅ Better debugging (immutable state)
- ✅ Simplified testing

---

## Related Documentation

- `docs/battery-ffi-integration.md` - Full FFI plugin example
- `docs/ping-ffi-integration.md` - Another FFI plugin example
- `docs/ffi-wrapper-api.md` - FFI API reference
- Issue #64: Plugin Refactoring for Immutable NetworkPacket

---

**Conclusion**: This migration pattern provides a gradual, safe path to immutable NetworkPacket usage across all plugins. Start with simple plugins like FindRemoteDevice, establish the pattern, then tackle more complex plugins systematically.
