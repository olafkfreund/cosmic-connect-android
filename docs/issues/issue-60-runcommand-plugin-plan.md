# Issue #60: RunCommand Plugin FFI Migration

> **Plugin:** RunCommandPlugin
> **Priority:** Medium
> **Complexity:** Medium
> **Estimated Time:** 3.5 hours
> **Status:** Planning

## Executive Summary

Migrate RunCommandPlugin from Java (232 lines) to Kotlin with FFI integration using cosmic-connect-core Rust library. This plugin enables remote command execution - desktop users can trigger pre-configured shell commands on their COSMIC Desktop from their Android device.

**Key Changes:**
- Create 3 FFI functions for packet creation (request list, execute, setup)
- Create Android FFI wrapper with factory methods
- Convert RunCommandPlugin.java to Kotlin with modern patterns
- Add FFI validation tests
- Preserve JSON parsing, SharedPreferences, and widget integration

---

## Table of Contents

1. [Background](#background)
2. [Current Status](#current-status)
3. [Implementation Plan](#implementation-plan)
4. [Technical Specifications](#technical-specifications)
5. [Testing Strategy](#testing-strategy)
6. [Timeline](#timeline)
7. [References](#references)

---

## Background

### Plugin Purpose

RunCommandPlugin allows Android users to:
- View a list of pre-configured commands from COSMIC Desktop
- Execute commands remotely (e.g., "Lock Screen", "Take Screenshot", "Shutdown")
- Trigger commands from widgets, notifications, or the app UI

**Use Cases:**
- Home automation ("Turn off lights")
- System control ("Reboot server")
- Quick actions ("Start backup")
- Productivity ("Open project in IDE")

### Protocol Overview

**Direction:** Bidirectional
- Android receives command list from desktop
- Android sends execution requests to desktop

**Packet Types:**
1. `cconnect.runcommand` (receive) - Command list from desktop
2. `cconnect.runcommand.request` (send) - Command execution requests

**Security:**
- Only pre-configured commands can be executed
- No arbitrary command injection
- Requires paired device
- Commands execute with desktop user's permissions

---

## Current Status

### Existing Implementation

**File:** `src/org/cosmic/cosmicconnect/Plugins/RunCommandPlugin/RunCommandPlugin.java`
- **Lines:** 232
- **Language:** Java
- **FFI Integration:** ❌ None (uses manual packet creation)
- **Last Modified:** January 15, 2026

**Supporting Files:**
- `CommandEntry.java` (933 bytes) - Command data model
- `RunCommandActivity.java` (6.3 KB) - UI for command list
- `RunCommandWidget*.kt` (3 files) - Home screen widgets
- `RunCommandControlsProviderService.kt` - Device controls integration

**Features:**
- JSON command list parsing
- SharedPreferences for caching commands
- Widget support (home screen)
- Device controls integration (Android 11+)
- Command execution via button clicks

### cosmic-connect-core Status

**File:** `src/plugins/runcommand.rs` (751 lines)
- **Status:** ⚠️ Module DISABLED (commented out in plugins/mod.rs)
- **Reason:** "Requires Device architecture refactoring for FFI"
- **Implementation:** Complete plugin with:
  - Command storage (HashMap)
  - Configuration persistence (JSON files)
  - Command execution (shell process spawning)
  - Packet creation methods
  - Comprehensive tests (14 test cases)

**Module Status:**
```rust
// src/plugins/mod.rs:135
// pub mod runcommand;    // ⚠️  TODO: Requires Device architecture refactoring for FFI
```

**Blocker:** The plugin depends on `Device` type for initialization, which isn't exposed via FFI yet.

**Solution for FFI:** Create standalone FFI functions that don't depend on Device instance. The plugin itself doesn't need to be enabled - we just need packet creation functions.

---

## Implementation Plan

### Phase 0: Planning (15 minutes) ✅

**Deliverables:**
- ✅ Create this migration plan document
- ✅ Analyze existing Java implementation
- ✅ Verify cosmic-connect-core runcommand.rs status
- ✅ Define FFI function signatures

**Status:** Complete

---

### Phase 1: Rust Verification (15 minutes)

**Objective:** Verify cosmic-connect-core builds and understand runcommand.rs structure

**Tasks:**
1. Build cosmic-connect-core with latest changes
   ```bash
   cd ../cosmic-connect-core
   cargo build --release
   ```

2. Verify runcommand.rs is disabled but readable
   ```bash
   grep "pub mod runcommand" src/plugins/mod.rs
   # Expected: commented out (line 135)
   ```

3. Understand packet creation patterns
   - Review `create_command_list_packet()` (lines 362-376)
   - Note JSON string serialization for `commandList`
   - Confirm packet structure matches Android expectations

**Expected Result:**
- ✅ cosmic-connect-core builds successfully
- ✅ runcommand.rs structure understood
- ✅ Packet formats documented

**Time:** 15 minutes

---

### Phase 2: FFI Interface Creation (45 minutes)

**Objective:** Create 3 FFI functions for RunCommand packet types

**Location:** `cosmic-connect-core/src/ffi/mod.rs`

#### Function 1: Request Command List

```rust
/// Create a command list request packet
///
/// Creates a packet requesting the list of available commands from the desktop.
/// The desktop will respond with a `cconnect.runcommand` packet containing
/// all configured commands.
///
/// ## Packet Structure
/// ```json
/// {
///   "type": "cconnect.runcommand.request",
///   "id": 1234567890,
///   "body": {
///     "requestCommandList": true
///   }
/// }
/// ```
///
/// ## Usage
/// ```kotlin
/// val packet = createRuncommandRequestList()
/// device.sendPacket(packet)
/// // Desktop responds with command list
/// ```
///
/// # Example
/// ```rust,no_run
/// use cosmic_connect_core::create_runcommand_request_list;
///
/// let packet = create_runcommand_request_list()?;
/// // Send packet to desktop...
/// # Ok::<(), cosmic_connect_core::error::ProtocolError>(())
/// ```
pub fn create_runcommand_request_list() -> Result<FfiPacket> {
    use serde_json::json;

    let packet = Packet::new(
        "cconnect.runcommand.request".to_string(),
        json!({
            "requestCommandList": true
        })
    );

    Ok(packet.into())
}
```

#### Function 2: Execute Command

```rust
/// Create a command execution request packet
///
/// Creates a packet requesting the desktop to execute a specific command
/// by its unique key/ID. The command must have been previously configured
/// on the desktop.
///
/// ## Packet Structure
/// ```json
/// {
///   "type": "cconnect.runcommand.request",
///   "id": 1234567890,
///   "body": {
///     "key": "command-id-123"
///   }
/// }
/// ```
///
/// ## Security
/// - Only pre-configured commands can be executed
/// - No arbitrary command injection
/// - Commands execute with desktop user's permissions
///
/// ## Usage
/// ```kotlin
/// val packet = createRuncommandExecute("screenshot-cmd")
/// device.sendPacket(packet)
/// // Desktop executes the command
/// ```
///
/// # Parameters
///
/// * `command_key` - Unique identifier of the command to execute
///
/// # Example
/// ```rust,no_run
/// use cosmic_connect_core::create_runcommand_execute;
///
/// let packet = create_runcommand_execute("backup-home")?;
/// // Send packet to desktop...
/// # Ok::<(), cosmic_connect_core::error::ProtocolError>(())
/// ```
pub fn create_runcommand_execute(command_key: String) -> Result<FfiPacket> {
    use serde_json::json;

    let packet = Packet::new(
        "cconnect.runcommand.request".to_string(),
        json!({
            "key": command_key
        })
    );

    Ok(packet.into())
}
```

#### Function 3: Setup Mode

```rust
/// Create a command setup request packet
///
/// Creates a packet requesting the desktop to open the command configuration
/// interface. This allows users to add, edit, or remove commands from the
/// desktop side.
///
/// ## Packet Structure
/// ```json
/// {
///   "type": "cconnect.runcommand.request",
///   "id": 1234567890,
///   "body": {
///     "setup": true
///   }
/// }
/// ```
///
/// ## Usage
/// ```kotlin
/// val packet = createRuncommandSetup()
/// device.sendPacket(packet)
/// // Desktop opens command configuration UI
/// ```
///
/// # Example
/// ```rust,no_run
/// use cosmic_connect_core::create_runcommand_setup;
///
/// let packet = create_runcommand_setup()?;
/// // Send packet to desktop...
/// # Ok::<(), cosmic_connect_core::error::ProtocolError>(())
/// ```
pub fn create_runcommand_setup() -> Result<FfiPacket> {
    use serde_json::json;

    let packet = Packet::new(
        "cconnect.runcommand.request".to_string(),
        json!({
            "setup": true
        })
    );

    Ok(packet.into())
}
```

#### UDL Definitions

**File:** `cosmic-connect-core/src/cosmic_connect_core.udl`

```idl
  // ========================================================================
  // RunCommand Plugin
  // ========================================================================

  /// Create a command list request packet
  ///
  /// Creates a packet requesting the list of available commands from desktop.
  [Throws=ProtocolError]
  FfiPacket create_runcommand_request_list();

  /// Create a command execution request packet
  ///
  /// Creates a packet requesting the desktop to execute a specific command.
  ///
  /// # Arguments
  ///
  /// * `command_key` - Unique identifier of the command to execute
  [Throws=ProtocolError]
  FfiPacket create_runcommand_execute(string command_key);

  /// Create a command setup request packet
  ///
  /// Creates a packet requesting the desktop to open command configuration UI.
  [Throws=ProtocolError]
  FfiPacket create_runcommand_setup();
```

#### Export in lib.rs

**File:** `cosmic-connect-core/src/lib.rs`

```rust
pub use ffi::{
    // ... existing exports ...
    create_findmyphone_request,
    create_runcommand_request_list, create_runcommand_execute, create_runcommand_setup,  // NEW
    create_telephony_event, create_mute_request, create_sms_messages,
    // ... more exports ...
};
```

**Build Verification:**
```bash
cd ../cosmic-connect-core
cargo build --release
# Should succeed with no errors
```

**Deliverables:**
- ✅ 3 FFI functions in ffi/mod.rs (~100 lines)
- ✅ 3 UDL definitions (~30 lines)
- ✅ Exports in lib.rs (~1 line)
- ✅ cosmic-connect-core builds successfully

**Time:** 45 minutes

---

### Phase 3: Android Wrapper Creation (30 minutes)

**Objective:** Create Kotlin FFI wrapper with factory methods and extension properties

**File:** `src/org/cosmic/cosmicconnect/Plugins/RunCommandPlugin/RunCommandPacketsFFI.kt`

```kotlin
/*
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cconnect.Plugins.RunCommandPlugin

import org.cosmic.cconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createRuncommandRequestList
import uniffi.cosmic_connect_core.createRuncommandExecute
import uniffi.cosmic_connect_core.createRuncommandSetup

/**
 * RunCommandPacketsFFI - FFI wrapper for RunCommand plugin
 *
 * This object provides type-safe packet creation for the RunCommand plugin
 * using the cosmic-connect-core Rust FFI layer.
 *
 * ## Protocol
 *
 * **Packet Types:**
 * - `cconnect.runcommand` - Command list from desktop (receive)
 * - `cconnect.runcommand.request` - Command execution requests (send)
 *
 * **Direction:**
 * - Desktop → Android: Send command list
 * - Android → Desktop: Request list, execute command, open setup
 *
 * ## Behavior
 *
 * **Command List:**
 * - Desktop sends list of pre-configured commands
 * - Android displays in UI, widgets, device controls
 * - Commands stored in SharedPreferences for offline access
 *
 * **Command Execution:**
 * - Android sends command ID to desktop
 * - Desktop executes shell command
 * - No response packet (fire-and-forget)
 *
 * **Security:**
 * - Only pre-configured commands can be executed
 * - No arbitrary command injection
 * - Commands execute with desktop user's permissions
 *
 * ## Usage
 *
 * **Request Command List:**
 * ```kotlin
 * val packet = RunCommandPacketsFFI.createRequestList()
 * device.sendPacket(packet)
 * // Desktop responds with command list
 * ```
 *
 * **Execute Command:**
 * ```kotlin
 * val packet = RunCommandPacketsFFI.createExecute("screenshot-cmd")
 * device.sendPacket(packet)
 * // Desktop executes the command
 * ```
 *
 * **Open Setup:**
 * ```kotlin
 * val packet = RunCommandPacketsFFI.createSetup()
 * device.sendPacket(packet)
 * // Desktop opens command configuration UI
 * ```
 *
 * **Receive Command List:**
 * ```kotlin
 * override fun onPacketReceived(legacyNp: org.cosmic.cconnect.NetworkPacket): Boolean {
 *     val np = NetworkPacket.fromLegacy(legacyNp)
 *
 *     if (np.isRunCommandList) {
 *         val commandListJson = np.getString("commandList") ?: "{}"
 *         val canAddCommand = np.getBoolean("canAddCommand", false)
 *         // Parse and display commands
 *         return true
 *     }
 *     return false
 * }
 * ```
 *
 * @see RunCommandPlugin
 */
object RunCommandPacketsFFI {

    /**
     * Create a command list request packet
     *
     * Creates a packet requesting the list of available commands from the desktop.
     * The desktop will respond with a `cconnect.runcommand` packet containing
     * all configured commands.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "cconnect.runcommand.request",
     *   "id": 1234567890,
     *   "body": {
     *     "requestCommandList": true
     *   }
     * }
     * ```
     *
     * ## Behavior
     * - Sends request to desktop
     * - Desktop responds with command list
     * - No timeout (best-effort delivery)
     *
     * @return NetworkPacket ready to send
     *
     * @see isRunCommandList
     */
    fun createRequestList(): NetworkPacket {
        val ffiPacket = createRuncommandRequestList()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a command execution request packet
     *
     * Creates a packet requesting the desktop to execute a specific command
     * by its unique key/ID. The command must have been previously configured
     * on the desktop.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "cconnect.runcommand.request",
     *   "id": 1234567890,
     *   "body": {
     *     "key": "command-id-123"
     *   }
     * }
     * ```
     *
     * ## Security
     * - Only pre-configured commands can be executed
     * - No arbitrary command injection
     * - Commands execute with desktop user's permissions
     *
     * ## Behavior
     * - Sends execution request to desktop
     * - Desktop executes the command immediately
     * - No response packet (fire-and-forget)
     * - Command runs in background on desktop
     *
     * @param commandKey Unique identifier of the command to execute
     * @return NetworkPacket ready to send
     */
    fun createExecute(commandKey: String): NetworkPacket {
        val ffiPacket = createRuncommandExecute(commandKey)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a command setup request packet
     *
     * Creates a packet requesting the desktop to open the command configuration
     * interface. This allows users to add, edit, or remove commands from the
     * desktop side.
     *
     * ## Packet Structure
     * ```json
     * {
     *   "type": "cconnect.runcommand.request",
     *   "id": 1234567890,
     *   "body": {
     *     "setup": true
     *   }
     * }
     * ```
     *
     * ## Behavior
     * - Sends setup request to desktop
     * - Desktop opens command configuration UI
     * - User can add/edit/remove commands
     * - Desktop sends updated command list after changes
     *
     * ## Availability
     * Only available if desktop indicated `canAddCommand: true` in command list.
     *
     * @return NetworkPacket ready to send
     *
     * @see canAddCommand
     */
    fun createSetup(): NetworkPacket {
        val ffiPacket = createRuncommandSetup()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

// ==========================================================================
// Extension Properties for Packet Type Inspection
// ==========================================================================

/**
 * Check if packet is a runcommand list
 *
 * Returns true if the packet is a `cconnect.runcommand` packet,
 * which contains the list of available commands from the desktop.
 *
 * ## Usage
 * ```kotlin
 * if (np.isRunCommandList) {
 *     val commandListJson = np.getString("commandList") ?: "{}"
 *     val canAddCommand = np.getBoolean("canAddCommand", false)
 *     parseCommandList(commandListJson)
 * }
 * ```
 *
 * @return true if packet is a command list, false otherwise
 */
val NetworkPacket.isRunCommandList: Boolean
    get() = type == "cconnect.runcommand"

/**
 * Check if packet is a runcommand request
 *
 * Returns true if the packet is a `cconnect.runcommand.request` packet.
 * Android sends these, desktop receives them. This is primarily for
 * desktop-side code or testing.
 *
 * ## Usage
 * ```kotlin
 * if (np.isRunCommandRequest) {
 *     when {
 *         np.has("requestCommandList") -> sendCommandList()
 *         np.has("key") -> executeCommand(np.getString("key"))
 *         np.has("setup") -> openSetupUI()
 *     }
 * }
 * ```
 *
 * @return true if packet is a command request, false otherwise
 */
val NetworkPacket.isRunCommandRequest: Boolean
    get() = type == "cconnect.runcommand.request"
```

**Deliverables:**
- ✅ RunCommandPacketsFFI.kt (~200 lines)
- ✅ Factory methods: createRequestList(), createExecute(), createSetup()
- ✅ Extension properties: isRunCommandList, isRunCommandRequest
- ✅ Comprehensive KDoc documentation

**Time:** 30 minutes

---

### Phase 4: Java → Kotlin Conversion (60 minutes)

**Objective:** Convert RunCommandPlugin.java to modern Kotlin with FFI integration

**File:** `src/org/cosmic/cosmicconnect/Plugins/RunCommandPlugin/RunCommandPlugin.kt`

**Key Changes:**

1. **Use FFI Wrapper:**
```kotlin
// OLD (Java)
Map<String, Object> body = new HashMap<>();
body.put("requestCommandList", true);
NetworkPacket packet = NetworkPacket.create(PACKET_TYPE_RUNCOMMAND_REQUEST, body);
getDevice().sendPacket(convertToLegacyPacket(packet));

// NEW (Kotlin)
val packet = RunCommandPacketsFFI.createRequestList()
device.sendPacket(packet.toLegacy())
```

2. **Extension Property for Type Checking:**
```kotlin
// OLD (Java)
if (np.has("commandList")) {
    // Handle command list
}

// NEW (Kotlin)
val np = NetworkPacket.fromLegacy(legacyNp)
if (np.isRunCommandList) {
    // Handle command list
}
```

3. **Modern Kotlin Patterns:**
- Null-safe operators (`?.`, `?:`)
- `when` expressions instead of if-else chains
- Property access instead of getters
- List/Map builders
- Extension functions

4. **Preserve All Features:**
- ✅ JSON command list parsing
- ✅ SharedPreferences caching
- ✅ CommandEntry data models
- ✅ Widget integration
- ✅ Device controls (Android 11+)
- ✅ CommandsChangedCallback system
- ✅ UI button for RunCommandActivity

**Estimated Size:** ~250 lines (similar to FindMyPhonePlugin.kt)

**Deliverables:**
- ✅ RunCommandPlugin.kt (modern Kotlin)
- ❌ Delete RunCommandPlugin.java
- ✅ All features preserved
- ✅ FFI integration complete

**Time:** 60 minutes

---

### Phase 5: Testing & Documentation (45 minutes)

**Objective:** Add FFI validation tests and comprehensive documentation

#### 5.1: FFI Validation Tests

**File:** `tests/org/cosmic/cosmicconnect/FFIValidationTest.kt`

**Add Test 3.8: RunCommand Plugin FFI**

```kotlin
/**
 * Test 3.8: RunCommand Plugin FFI - Issue #60
 *
 * Verify RunCommand packet creation via FFI functions:
 * - createRuncommandRequestList()
 * - createRuncommandExecute()
 * - createRuncommandSetup()
 */
@Test
fun testRunCommandPlugin() {
    Log.i(TAG, "=== Test 3.8: RunCommand Plugin FFI (Issue #60) ===")

    try {
        // Test 1: Create command list request
        Log.i(TAG, "   Test 1: Create command list request")
        val requestListPacket = createRuncommandRequestList()

        assertNotNull("Request list packet should not be null", requestListPacket)
        assertEquals(
            "Packet type should be runcommand.request",
            "cconnect.runcommand.request",
            requestListPacket.packetType
        )
        assertTrue("Should have requestCommandList field", requestListPacket.body.containsKey("requestCommandList"))
        assertEquals("requestCommandList should be true", true, requestListPacket.body["requestCommandList"])
        Log.i(TAG, "   ✅ Request list packet creation successful")

        // Test 2: Create command execution request
        Log.i(TAG, "   Test 2: Create command execution request")
        val executePacket = createRuncommandExecute("test-command-123")

        assertNotNull("Execute packet should not be null", executePacket)
        assertEquals(
            "Packet type should be runcommand.request",
            "cconnect.runcommand.request",
            executePacket.packetType
        )
        assertTrue("Should have key field", executePacket.body.containsKey("key"))
        assertEquals("key should match", "test-command-123", executePacket.body["key"])
        Log.i(TAG, "   ✅ Execute packet creation successful")

        // Test 3: Create setup request
        Log.i(TAG, "   Test 3: Create setup request")
        val setupPacket = createRuncommandSetup()

        assertNotNull("Setup packet should not be null", setupPacket)
        assertEquals(
            "Packet type should be runcommand.request",
            "cconnect.runcommand.request",
            setupPacket.packetType
        )
        assertTrue("Should have setup field", setupPacket.body.containsKey("setup"))
        assertEquals("setup should be true", true, setupPacket.body["setup"])
        Log.i(TAG, "   ✅ Setup packet creation successful")

        // Test 4: Verify all packets are unique
        Log.i(TAG, "   Test 4: Verify packet uniqueness")
        assertNotEquals("Packet IDs should be unique", requestListPacket.id, executePacket.id)
        assertNotEquals("Packet IDs should be unique", requestListPacket.id, setupPacket.id)
        assertNotEquals("Packet IDs should be unique", executePacket.id, setupPacket.id)
        Log.i(TAG, "   ✅ All packets have unique IDs")

        // Test 5: Verify serialization
        Log.i(TAG, "   Test 5: Verify packet serialization")
        val serialized = serializePacket(executePacket)
        assertNotNull("Serialized bytes should not be null", serialized)
        assertTrue("Serialized bytes should not be empty", serialized.isNotEmpty())

        val serializedStr = serialized.decodeToString()
        assertTrue("Should contain command key", serializedStr.contains("test-command-123"))
        assertTrue("Should end with newline", serializedStr.endsWith("\n"))
        Log.i(TAG, "   ✅ Packet serialization successful")

        // Summary
        Log.i(TAG, "")
        Log.i(TAG, "✅ All RunCommand plugin FFI tests passed (5/5)")
        Log.i(TAG, "   - Request list packet creation")
        Log.i(TAG, "   - Execute packet creation")
        Log.i(TAG, "   - Setup packet creation")
        Log.i(TAG, "   - Packet uniqueness")
        Log.i(TAG, "   - Serialization")
    } catch (e: Exception) {
        Log.e(TAG, "⚠️ RunCommand plugin test failed", e)
        fail("RunCommand FFI tests failed: ${e.message}")
    }
}
```

#### 5.2: Testing Guide

**File:** `docs/issue-60-testing-guide.md`

**Contents:**
- Automated testing procedures (Test 3.8)
- Manual test scenarios (10+ scenarios):
  - Request and receive command list
  - Execute commands from UI
  - Execute commands from widget
  - Execute commands from device controls
  - Setup mode (add/edit commands)
  - Offline mode (cached commands)
  - SharedPreferences persistence
  - Widget refresh after command list update
  - Multiple devices with different command lists
  - Error handling (unknown command, connection loss)
- Expected behaviors
- Android version differences (device controls on Android 11+)
- Troubleshooting guide

#### 5.3: Completion Summary

**File:** `docs/issue-60-completion-summary.md`

**Contents:**
- Phase-by-phase execution summary
- File changes and metrics
- Testing results
- Migration patterns
- Lessons learned
- Future enhancements

**Deliverables:**
- ✅ Test 3.8 in FFIValidationTest.kt (~80 lines)
- ✅ Testing guide (~700 lines)
- ✅ Completion summary (~600 lines)
- ✅ All tests passing

**Time:** 45 minutes

---

## Technical Specifications

### Packet Formats

#### 1. Command List (Desktop → Android)

**Type:** `cconnect.runcommand`

```json
{
  "id": 1234567890,
  "type": "cconnect.runcommand",
  "body": {
    "commandList": "{\"cmd1\":{\"name\":\"Screenshot\",\"command\":\"spectacle -f\"},\"cmd2\":{\"name\":\"Lock Screen\",\"command\":\"loginctl lock-session\"}}",
    "canAddCommand": true
  }
}
```

**Fields:**
- `commandList` (string, required) - JSON string containing command map
- `canAddCommand` (boolean, optional) - Whether user can add commands (default: false)

**CommandList Structure:**
```json
{
  "command-id-1": {
    "name": "User-friendly name",
    "command": "shell command to execute"
  },
  "command-id-2": {
    "name": "Another command",
    "command": "another shell command"
  }
}
```

---

#### 2. Request Command List (Android → Desktop)

**Type:** `cconnect.runcommand.request`

```json
{
  "id": 1234567890,
  "type": "cconnect.runcommand.request",
  "body": {
    "requestCommandList": true
  }
}
```

**Fields:**
- `requestCommandList` (boolean, required) - Must be `true`

---

#### 3. Execute Command (Android → Desktop)

**Type:** `cconnect.runcommand.request`

```json
{
  "id": 1234567890,
  "type": "cconnect.runcommand.request",
  "body": {
    "key": "command-id-123"
  }
}
```

**Fields:**
- `key` (string, required) - Command identifier from command list

---

#### 4. Setup Mode (Android → Desktop)

**Type:** `cconnect.runcommand.request`

```json
{
  "id": 1234567890,
  "type": "cconnect.runcommand.request",
  "body": {
    "setup": true
  }
}
```

**Fields:**
- `setup` (boolean, required) - Must be `true`

---

### Data Flow

```
┌─────────────┐                                    ┌─────────────┐
│   Android   │                                    │   Desktop   │
│   Device    │                                    │   (COSMIC)  │
└─────────────┘                                    └─────────────┘
       │                                                   │
       │  1. createRequestList()                          │
       │─────────────────────────────────────────────────>│
       │     {"requestCommandList": true}                 │
       │                                                   │
       │  2. Command List Response                        │
       │<─────────────────────────────────────────────────│
       │     {"commandList": "{...}", "canAddCommand"}    │
       │                                                   │
       │  3. Parse & Display Commands                     │
       │     - Store in SharedPreferences                 │
       │     - Update UI, Widgets, Device Controls        │
       │                                                   │
       │  [User taps command]                             │
       │                                                   │
       │  4. createExecute("cmd-id")                      │
       │─────────────────────────────────────────────────>│
       │     {"key": "cmd-id"}                            │
       │                                                   │
       │                                     5. Execute Shell Command
       │                                        (Async, no response)
       │                                                   │
       │  [User taps "Add Command"]                       │
       │                                                   │
       │  6. createSetup()                                │
       │─────────────────────────────────────────────────>│
       │     {"setup": true}                              │
       │                                                   │
       │                                     7. Open Config UI
       │                                        (User adds commands)
       │                                                   │
       │  8. Updated Command List                         │
       │<─────────────────────────────────────────────────│
       │     {"commandList": "{... new ...}"}             │
       │                                                   │
```

---

### Command Storage

**Android Side (SharedPreferences):**
```
Key: commands_preference_<device-id>
Value: JSON array of command objects

[
  {"key": "cmd1", "name": "Screenshot", "command": "spectacle -f"},
  {"key": "cmd2", "name": "Lock", "command": "loginctl lock-session"}
]
```

**Desktop Side (COSMIC):**
```
~/.config/kdeconnect/<device-id>/kdeconnect_runcommand/commands.json

{
  "commands": {
    "cmd1": {
      "name": "Screenshot",
      "command": "spectacle -f"
    },
    "cmd2": {
      "name": "Lock Screen",
      "command": "loginctl lock-session"
    }
  }
}
```

---

## Testing Strategy

### Automated Tests

**Test 3.8: RunCommand Plugin FFI**

| Test Case | Coverage | Pass Criteria |
|-----------|----------|---------------|
| Request List | FFI function `createRuncommandRequestList()` | Packet not null, correct type, has requestCommandList field |
| Execute Command | FFI function `createRuncommandExecute()` | Packet not null, correct type, has key field with value |
| Setup Mode | FFI function `createRuncommandSetup()` | Packet not null, correct type, has setup field |
| Uniqueness | Packet ID generation | All packets have unique IDs |
| Serialization | JSON encoding | Valid JSON with newline terminator, contains expected fields |

---

### Manual Tests

#### Scenario 1: Request and Receive Command List

**Preconditions:**
- Desktop has configured commands
- Android device paired

**Steps:**
1. Launch Cosmic Connect app
2. Navigate to connected device
3. Open RunCommand plugin
4. App automatically requests command list

**Expected:**
- ✅ App sends request packet
- ✅ Desktop responds with command list
- ✅ Commands displayed in UI
- ✅ Commands cached in SharedPreferences

---

#### Scenario 2: Execute Command from UI

**Preconditions:**
- Command list received
- Commands displayed in RunCommandActivity

**Steps:**
1. Open RunCommandActivity
2. Tap a command (e.g., "Take Screenshot")
3. Observe desktop behavior

**Expected:**
- ✅ Execute packet sent
- ✅ Desktop executes command immediately
- ✅ No visual feedback needed (fire-and-forget)
- ✅ Command runs in background on desktop

---

#### Scenario 3: Execute Command from Widget

**Preconditions:**
- Command list received
- RunCommand widget added to home screen

**Steps:**
1. Add RunCommand widget to home screen
2. Configure widget to show specific command
3. Tap widget
4. Observe desktop behavior

**Expected:**
- ✅ Widget displays command name
- ✅ Tapping widget sends execute packet
- ✅ Desktop executes command
- ✅ Widget updates if command list changes

---

#### Scenario 4: Device Controls (Android 11+)

**Preconditions:**
- Android 11 or higher
- Command list received
- Device controls permission granted

**Steps:**
1. Long-press power button
2. View device controls
3. Find RunCommand controls
4. Tap a command control

**Expected:**
- ✅ Commands appear in device controls
- ✅ Tapping control sends execute packet
- ✅ Desktop executes command
- ✅ Controls work from locked screen

---

#### Scenario 5: Setup Mode

**Preconditions:**
- Desktop supports command addition (`canAddCommand: true`)
- Command list received

**Steps:**
1. Open RunCommandActivity
2. Tap "Add Command" button
3. Observe desktop behavior

**Expected:**
- ✅ Setup packet sent
- ✅ Desktop opens command configuration UI
- ✅ User can add/edit/remove commands on desktop
- ✅ Desktop sends updated command list after changes
- ✅ Android receives and displays new commands

---

#### Scenario 6: Offline Mode (Cached Commands)

**Preconditions:**
- Commands previously received and cached
- Device disconnected

**Steps:**
1. Disconnect device (turn off Bluetooth/Wi-Fi)
2. Open RunCommandActivity or widget

**Expected:**
- ✅ Cached commands still displayed
- ✅ Tapping command shows "Device not connected" message
- ✅ Reconnection auto-refreshes command list

---

## Timeline

### Summary

| Phase | Time | Status |
|-------|------|--------|
| 0. Planning | 15 min | ✅ Complete |
| 1. Rust verification | 15 min | ⏳ Pending |
| 2. FFI interface | 45 min | ⏳ Pending |
| 3. Android wrapper | 30 min | ⏳ Pending |
| 4. Java → Kotlin | 60 min | ⏳ Pending |
| 5. Testing & docs | 45 min | ⏳ Pending |
| **TOTAL** | **3.5 hours** | **5% Complete** |

### Critical Path

```
Phase 0 (Planning) ──> Phase 1 (Rust) ──> Phase 2 (FFI) ──> Phase 3 (Wrapper) ──> Phase 4 (Kotlin) ──> Phase 5 (Testing)
   15 min                15 min             45 min            30 min               60 min              45 min
```

---

## References

### Related Issues

- **Issue #45:** NetworkPacket FFI Migration (Foundation)
- **Issue #54:** BatteryPlugin FFI Migration (First plugin)
- **Issue #59:** FindMyPhonePlugin FFI Migration (Previous plugin)
- **Issue #60:** RunCommandPlugin FFI Migration (This issue)

### Source Code

**cosmic-connect-core:**
- `src/plugins/runcommand.rs` - Rust implementation (751 lines, disabled)
- `src/ffi/mod.rs` - FFI functions (to be added)
- `src/cosmic_connect_core.udl` - UDL definitions (to be added)

**cosmic-connect-android:**
- `src/.../RunCommandPlugin/RunCommandPlugin.java` - Current implementation (232 lines)
- `src/.../RunCommandPlugin/CommandEntry.java` - Data model
- `src/.../RunCommandPlugin/RunCommandActivity.java` - UI
- `src/.../RunCommandPlugin/RunCommandWidget*.kt` - Widgets

### Documentation

- [KDE Connect RunCommand Plugin](https://github.com/KDE/kdeconnect-kde/tree/master/plugins/runcommand)
- [Valent Protocol - RunCommand](https://valent.andyholmes.ca/documentation/protocol.html)

---

## Notes

### Important Constraints

⚠️ **Build is Currently Broken**

The Android app has 48 compilation errors from incomplete previous FFI migrations. We **cannot test** Phase 4-5 until the build is fixed. Options:

1. **Fix build first** (2-3 hours), then implement RunCommandPlugin (3.5 hours)
2. **Plan now, implement later** - Complete Phase 0-3 (planning + FFI), fix build, then Phase 4-5
3. **Hybrid approach** - Fix critical errors, implement RunCommandPlugin with remaining issues

**Recommendation:** Complete Phase 0-3 (Rust + FFI + Wrapper) now, fix build separately, then complete Phase 4-5.

### Migration Progress

After completing Issue #60:
- **Completed:** 8 plugins (Battery, Telephony, Share, Notifications, Clipboard, FindMyPhone, RunCommand, +1)
- **Remaining:** 10 plugins
- **Progress:** 44% complete

---

**Plan Created:** January 16, 2026
**Status:** Phase 0 Complete
**Next Phase:** Rust Verification (15 minutes)
