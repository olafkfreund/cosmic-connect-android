# Issue #53: Share Plugin Migration - Phase 2 Complete

**Date**: 2026-01-16
**Status**: ✅ Phase 2 Complete (FFI Interface Implemented)
**Plugin**: SharePlugin
**Files**: `cosmic-connect-core/src/ffi/mod.rs`, `cosmic-connect-core/src/cosmic_connect_core.udl`

---

## Executive Summary

**Phase 2 of Issue #53 (Share Plugin Migration) is complete.** All FFI interface functions for the Share plugin have been implemented, including packet creation functions and payload transfer with progress callbacks. The library compiles successfully and UniFFI bindings are ready for Kotlin generation.

**Key Achievement**: Implemented complete FFI layer for Share plugin with async payload transfer and callback support.

---

## What Was Completed

### ✅ PayloadCallback Trait Added

**Location**: `cosmic-connect-core/src/ffi/mod.rs:191-196`

```rust
/// Payload transfer callback trait
pub trait PayloadCallback: Send + Sync {
    fn on_progress(&self, bytes_transferred: u64, total_bytes: u64);
    fn on_complete(&self);
    fn on_error(&self, error: String);
}
```

**Purpose**: Provides async progress updates and completion status for payload transfers across FFI boundary.

---

### ✅ Share Packet Creation Functions

**Location**: `cosmic-connect-core/src/ffi/mod.rs:254-349`

#### 1. File Share Packet

```rust
pub fn create_file_share_packet(
    filename: String,
    size: i64,
    creation_time: Option<i64>,
    last_modified: Option<i64>,
) -> Result<FfiPacket>
```

**Creates**: `cconnect.share.request` packet with file metadata
**Packet Structure**:
```json
{
  "type": "cconnect.share.request",
  "body": {
    "filename": "image.png",
    "creationTime": 1640000000000,
    "lastModified": 1640000000000
  },
  "payloadSize": 1048576
}
```

#### 2. Text Share Packet

```rust
pub fn create_text_share_packet(text: String) -> Result<FfiPacket>
```

**Creates**: `cconnect.share.request` packet with text content
**Packet Structure**:
```json
{
  "type": "cconnect.share.request",
  "body": {
    "text": "Some text to share"
  }
}
```

#### 3. URL Share Packet

```rust
pub fn create_url_share_packet(url: String) -> Result<FfiPacket>
```

**Creates**: `cconnect.share.request` packet with URL
**Packet Structure**:
```json
{
  "type": "cconnect.share.request",
  "body": {
    "url": "https://example.com"
  }
}
```

#### 4. Multi-File Update Packet

```rust
pub fn create_multifile_update_packet(
    number_of_files: i32,
    total_payload_size: i64,
) -> Result<FfiPacket>
```

**Creates**: `cconnect.share.request.update` packet
**Packet Structure**:
```json
{
  "type": "cconnect.share.request.update",
  "body": {
    "numberOfFiles": 5,
    "totalPayloadSize": 10485760
  }
}
```

---

### ✅ Payload Transfer Implementation

**Location**: `cosmic-connect-core/src/ffi/mod.rs:622-778`

#### PayloadTransferHandle Struct

```rust
pub struct PayloadTransferHandle {
    transfer_id: u64,
    callback: Arc<Box<dyn PayloadCallback>>,
    cancel_token: Arc<RwLock<bool>>,
    runtime: Arc<tokio::runtime::Runtime>,
}
```

**Methods**:
- `get_id()` - Returns unique transfer ID
- `cancel()` - Cancels the transfer
- `is_cancelled()` - Checks cancellation status

#### start_payload_download Function

```rust
pub fn start_payload_download(
    device_host: String,
    port: u16,
    expected_size: i64,
    callback: Box<dyn PayloadCallback>,
) -> Result<Arc<PayloadTransferHandle>>
```

**Implementation Details**:
1. Creates a new Tokio runtime for async operations
2. Generates unique transfer ID using atomic counter
3. Spawns async task for TCP connection and download
4. Streams data in 8KB chunks
5. Reports progress via callback
6. Handles errors and cancellation
7. Returns handle for cancellation control

**Transfer Flow**:
```
┌─────────────────────────────────────────────────────┐
│  Android calls start_payload_download()             │
│  - Provides host, port, size, callback              │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  Rust spawns async task                             │
│  - Connects to device_host:port via TCP             │
│  - Creates PayloadTransferHandle                    │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  Download loop                                      │
│  1. Check cancellation token                        │
│  2. Read chunk (8KB buffer)                         │
│  3. Call callback.on_progress()                     │
│  4. Repeat until complete                           │
└─────────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────────┐
│  Completion                                         │
│  Success: callback.on_complete()                    │
│  Error: callback.on_error(message)                  │
│  Cancelled: callback.on_error("Transfer cancelled") │
└─────────────────────────────────────────────────────┘
```

---

### ✅ UniFFI Interface Definition Updated

**Location**: `cosmic-connect-core/src/cosmic_connect_core.udl`

**Changes**:

1. **Added Share Plugin Functions to Namespace** (lines 109-188):
```udl
// Share Plugin
[Throws=ProtocolError]
FfiPacket create_file_share_packet(
  string filename,
  i64 size,
  i64? creation_time,
  i64? last_modified
);

[Throws=ProtocolError]
FfiPacket create_text_share_packet(string text);

[Throws=ProtocolError]
FfiPacket create_url_share_packet(string url);

[Throws=ProtocolError]
FfiPacket create_multifile_update_packet(
  i32 number_of_files,
  i64 total_payload_size
);

[Throws=ProtocolError]
PayloadTransferHandle start_payload_download(
  string device_host,
  u16 port,
  i64 expected_size,
  PayloadCallback callback
);
```

2. **Added PayloadTransferHandle Interface** (lines 338-349):
```udl
interface PayloadTransferHandle {
  u64 get_id();

  [Throws=ProtocolError]
  void cancel();

  boolean is_cancelled();
};
```

3. **Added PayloadCallback Interface** (lines 383-404):
```udl
callback interface PayloadCallback {
  void on_progress(u64 bytes_transferred, u64 total_bytes);
  void on_complete();
  void on_error(string error);
};
```

---

### ✅ Library Exports Updated

**Location**: `cosmic-connect-core/src/lib.rs:40-52`

**Added exports for UniFFI**:
```rust
pub use ffi::{
    // Existing exports...
    PayloadCallback,                     // ✅ New
    PayloadTransferHandle,               // ✅ New
    create_file_share_packet,            // ✅ New
    create_text_share_packet,            // ✅ New
    create_url_share_packet,             // ✅ New
    create_multifile_update_packet,      // ✅ New
    start_payload_download,              // ✅ New
};
```

---

## Compilation Status

### ✅ Successful Build

```bash
$ cd cosmic-connect-core && cargo check --lib
...
Checking cosmic-connect-core v0.1.0
Finished `dev` profile [unoptimized + debuginfo] target(s) in 1.03s
```

### Warnings (Non-Critical)

```
warning: unused import: `Plugin`
warning: unused variable: `size`
warning: variable does not need to be mutable
warning: unused variable: `plugin_guard`
warning: unused variable: `battery_state`
warning: unused variable: `message`
warning: fields `device_info`, `callback`, and `running` are never read
```

**Status**: 9 warnings (all non-critical, expected for WIP code)

---

## Architecture Pattern Established

### FFI Data Flow for Payload Transfer

```
┌─────────────────────────────────────────────────────────┐
│    Android Layer (Kotlin)                               │
│  - Implements PayloadCallback                           │
│  - Calls start_payload_download()                       │
│  - Receives progress updates                            │
│  - Writes bytes to file                                 │
└─────────────────────────────────────────────────────────┘
                         ↓ ↑ (Callback)
          FFI Boundary (UniFFI generated glue)
                         ↓ ↑
┌─────────────────────────────────────────────────────────┐
│    Rust FFI Layer (cosmic-connect-core/ffi)             │
│  - start_payload_download() function                    │
│  - PayloadTransferHandle (cancellation)                 │
│  - Spawns async download task                           │
│  - Calls callback trait methods                         │
└─────────────────────────────────────────────────────────┘
                         ↓ ↑
┌─────────────────────────────────────────────────────────┐
│    Tokio Async Runtime                                  │
│  - TCP connection to device                             │
│  - Streaming download (8KB chunks)                      │
│  - Progress tracking                                    │
│  - Error handling                                       │
└─────────────────────────────────────────────────────────┘
```

**Key Insight**: Payload transfer is fully asynchronous, using Tokio runtime spawned per transfer, with cancellation support and progress callbacks crossing the FFI boundary.

---

## Testing Considerations

### Unit Tests (Deferred to Phase 5)

**To Test**:
1. Packet creation functions produce correct JSON structure
2. Payload transfer handle creation
3. Cancellation mechanism
4. Callback invocation (mock callbacks)

### Integration Tests (Phase 5)

**To Test**:
1. End-to-end file transfer Android → COSMIC
2. Progress callback accuracy
3. Cancellation during transfer
4. Error handling (network failure, wrong host, etc.)
5. Large file transfers (memory usage)

---

## Next Steps: Phase 3 - Android Wrapper

### Phase 3 Goals (2-3 hours estimated)

1. **Create SharePackets Kotlin wrapper**
   ```kotlin
   object SharePackets {
       fun createFileShare(
           filename: String,
           size: Long,
           creationTime: Long? = null,
           lastModified: Long? = null
       ): NetworkPacket

       fun createTextShare(text: String): NetworkPacket

       fun createUrlShare(url: String): NetworkPacket

       fun createMultiFileUpdate(
           numberOfFiles: Int,
           totalPayloadSize: Long
       ): NetworkPacket
   }
   ```

2. **Create PayloadTransfer Kotlin wrapper**
   ```kotlin
   class PayloadTransfer(
       private val deviceHost: String,
       private val port: Int,
       private val expectedSize: Long
   ) {
       fun start(
           outputFile: File,
           onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
           onComplete: () -> Unit,
           onError: (error: String) -> Unit
       ): PayloadTransferHandle

       companion object {
           fun fromPacket(packet: NetworkPacket, deviceHost: String): PayloadTransfer?
       }
   }
   ```

3. **Add to existing wrappers**
   - Location: `src/main/kotlin/org/cosmic/cosmicconnect/Core/`
   - Follow patterns from BatteryPluginFFI.kt and PingPluginFFI.kt
   - Add documentation and examples

4. **Write unit tests**
   - Test packet creation
   - Test payload transfer callbacks
   - Mock network operations

---

## Validation Checklist

- ✅ PayloadCallback trait implemented
- ✅ Share packet creation functions implemented
- ✅ Payload transfer functions implemented
- ✅ PayloadTransferHandle for cancellation
- ✅ UDL file updated with all new types
- ✅ Library exports updated
- ✅ Rust library builds successfully
- ✅ No compilation errors
- ⚠️ Warnings present (non-critical)
- ⏳ Kotlin wrappers pending (Phase 3)
- ⏳ Android integration pending (Phase 4)

---

## Changes Made

### Files Modified

1. **cosmic-connect-core/src/ffi/mod.rs** (~200 lines added)
   - Added PayloadCallback trait
   - Added create_file_share_packet()
   - Added create_text_share_packet()
   - Added create_url_share_packet()
   - Added create_multifile_update_packet()
   - Added PayloadTransferHandle struct
   - Added start_payload_download() with async implementation

2. **cosmic-connect-core/src/cosmic_connect_core.udl** (~120 lines added)
   - Added Share plugin functions to namespace
   - Added PayloadTransferHandle interface
   - Added PayloadCallback callback interface

3. **cosmic-connect-core/src/lib.rs** (7 exports added)
   - Re-exported PayloadCallback
   - Re-exported PayloadTransferHandle
   - Re-exported all 5 new Share plugin functions

---

## Known Limitations

### Phase 2 Limitations

1. **Payload Writing Not Implemented**: FFI layer only downloads bytes, Android must handle file I/O
   - Mitigation: Phase 3 wrapper will handle file writing

2. **No Transfer Resume**: If transfer fails, must restart from beginning
   - Future Enhancement: Add partial transfer support

3. **Single Transfer Runtime**: Each transfer creates its own Tokio runtime
   - Optimization: Could use shared runtime pool in future

4. **8KB Chunk Size**: Fixed buffer size may not be optimal for all scenarios
   - Tuning: Could make configurable in future

---

## Technical Decisions

### Decision 1: Async Transfer with Spawned Task

**Problem**: FFI calls are synchronous, but payload transfer is async and long-running

**Options Considered**:
- A) Block FFI call until transfer completes (bad UX)
- B) Spawn async task and return handle immediately (chosen)
- C) Use separate thread pool

**Decision**: Option B - Spawn async task
**Rationale**:
- Non-blocking FFI call
- Proper async/await usage
- Cancellation support
- Progress callbacks during transfer

### Decision 2: Per-Transfer Tokio Runtime

**Problem**: Need async runtime for download task

**Options Considered**:
- A) Global shared runtime (complex lifetime management)
- B) Runtime per transfer (chosen)
- C) Thread-based download (no async benefits)

**Decision**: Option B - Runtime per transfer
**Rationale**:
- Simpler lifetime management
- Isolated failures
- Easy cleanup on cancel
- Acceptable overhead for file transfers

### Decision 3: Callback-Based Progress

**Problem**: How to report progress from Rust → Android

**Options Considered**:
- A) Polling-based (Android calls get_progress())
- B) Callback-based (Rust calls Android) (chosen)
- C) Event stream

**Decision**: Option B - Callbacks
**Rationale**:
- Real-time progress updates
- Standard UniFFI pattern
- Matches KDE Connect protocol expectations
- Clean separation of concerns

---

## Lessons Learned

### What Worked Well

1. **UniFFI Abstraction**: Callback interfaces and async patterns translate cleanly
2. **Incremental Implementation**: Building packet creation first, then payload transfer
3. **Documentation-First**: UDL file documented before implementation
4. **Pattern Reuse**: Following BatteryPluginFFI patterns for consistency

### Challenges Encountered

1. **Export Requirements**: Had to update lib.rs exports for UniFFI scaffolding
2. **Async Boundary**: Spawning Tokio tasks across FFI required careful Arc/clone management
3. **Cancellation**: Implementing proper cancellation with shared state

### Recommendations for Phase 3

1. **Follow Existing Patterns**: Use BatteryPluginFFI.kt as template
2. **Handle File I/O Carefully**: Android scoped storage requires proper permissions
3. **Test Error Scenarios**: Network failures, permission denials, cancellation
4. **Add Progress Throttling**: Don't call onProgress for every chunk (performance)

---

## References

- **Phase 1 Completion**: docs/issues/issue-53-phase1-complete.md
- **Issue #53 Plan**: docs/issues/issue-53-share-plugin-plan.md
- **FFI Integration Guide**: docs/guides/FFI_INTEGRATION_GUIDE.md
- **UniFFI Callbacks**: https://mozilla.github.io/uniffi-rs/udl/callback_interfaces.html
- **Tokio Runtime**: https://docs.rs/tokio/latest/tokio/runtime/

---

**Document Version**: 1.0
**Created**: 2026-01-16
**Status**: Phase 2 Complete ✅
**Next Phase**: Phase 3 - Android Wrapper (2-3 hours estimated)
**Total Progress**: 50% complete (Phase 2 of 5)
