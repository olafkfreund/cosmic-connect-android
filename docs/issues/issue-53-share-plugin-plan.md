# Issue #53: Share Plugin Migration - Planning Document

**Issue**: #53 Share Plugin Migration
**Status**: Planning Phase
**Date**: 2026-01-16
**Phase**: 2 - Core Modernization

---

## Overview

Issue #53 involves migrating the Share plugin from the old Java/Kotlin implementation to use the Rust FFI core library. The Share plugin is one of the most complex plugins, handling file transfers, text sharing, and URL sharing between devices.

---

## Current State

### Rust Implementation (cosmic-connect-core)

**File**: `src/plugins/share.rs` (34 KB, ~900 lines)

**Status**: Implemented but disabled

**Why Disabled**:
```rust
// pub mod share;  // ⚠️  TODO: Requires Device architecture refactoring for FFI
```

**Complexity**: HIGH - Most complex plugin after battery/ping

**Device Dependencies**:
- `device.id()` - Device identifier
- `device.name()` - Device name for logging
- `device.host` - Remote host IP for payload transfers (critical!)
- Device state management during transfers

### Android Implementation (Old)

**Location**: `src/org/cosmic/cosmicconnect/Plugins/SharePlugin/`

**Components**:
- SharePlugin.java - Main plugin logic
- ShareNotification.java - Notification UI
- ShareSettingsFragment.java - Settings
- ComposeSendActivity.kt - Compose UI for sending

**Functionality**:
- File sharing (single and multiple files)
- Text content sharing
- URL sharing
- Progress notifications
- Download management
- Media scanning integration

---

## Technical Analysis

### Share Plugin Features

#### 1. File Transfer
```json
{
  "type": "kdeconnect.share.request",
  "body": {
    "filename": "image.png",
    "creationTime": 1640000000000,
    "lastModified": 1640000000000,
    "open": false
  },
  "payloadSize": 1048576,
  "payloadTransferInfo": {
    "port": 1739
  }
}
```

**Requires**:
- Device host IP for TCP connection
- Payload transfer mechanism
- File I/O and storage management
- Progress tracking
- Metadata preservation

#### 2. Text Sharing
```json
{
  "type": "kdeconnect.share.request",
  "body": {
    "text": "Some text to share"
  }
}
```

**Requires**:
- Simple packet handling
- No payload transfer
- Clipboard integration

#### 3. URL Sharing
```json
{
  "type": "kdeconnect.share.request",
  "body": {
    "url": "https://example.com"
  }
}
```

**Requires**:
- Simple packet handling
- Intent launching for URL opening

#### 4. Multi-File Transfer
```json
{
  "type": "kdeconnect.share.request.update",
  "body": {
    "numberOfFiles": 5,
    "totalPayloadSize": 10485760
  }
}
```

**Requires**:
- Transfer progress aggregation
- UI notifications
- Batch handling

---

## Migration Challenges

### Challenge 1: Device Architecture Dependencies ⚠️

**Problem**: Share plugin heavily uses Device object:

```rust
// Current code (doesn't work in FFI)
async fn handle_share_request(&self, packet: &Packet, device: &Device) {
    let device_id = device.id().to_string();
    let device_name = device.name().to_string();
    let host = device.host.clone(); // Critical for payload transfer!

    // Connect to device.host:port for file download
    let stream = TcpStream::connect((host, port)).await?;
}
```

**Solution Options**:

**Option A**: Pass device info separately
```rust
async fn handle_share_request(
    &self,
    packet: &Packet,
    device_id: &str,
    device_host: &str
) -> Result<()>
```

**Option B**: Store device info in plugin state
```rust
pub struct SharePlugin {
    device_id: Option<String>,
    device_host: Option<String>,
    // ...
}
```

**Option C**: Create minimal DeviceInfo struct for FFI
```rust
pub struct FfiDeviceInfo {
    pub id: String,
    pub name: String,
    pub host: String,
    pub port: u16,
}
```

**Recommendation**: Option C - Clean and matches our FFI patterns

### Challenge 2: Payload Transfer Architecture ⚠️

**Problem**: File payloads require TCP connections separate from control channel

**Current Rust Implementation**:
```rust
// Downloads file from remote device
tokio::spawn(async move {
    let mut stream = TcpStream::connect((host, port)).await?;
    let mut file = File::create(file_path).await?;

    let mut total_bytes = 0;
    let mut buffer = vec![0u8; 8192];

    while total_bytes < expected_size {
        let n = stream.read(&mut buffer).await?;
        file.write_all(&buffer[..n]).await?;
        total_bytes += n;

        // Report progress to UI
    }
});
```

**FFI Challenges**:
- Async operation spanning FFI boundary
- Progress callbacks from Rust → Android
- Error handling across FFI
- Cancellation support

**Solution**: Payload transfer abstraction

```rust
// FFI-friendly payload transfer
pub trait PayloadTransferCallback {
    fn on_progress(&self, bytes_transferred: u64, total_bytes: u64);
    fn on_complete(&self);
    fn on_error(&self, error: String);
}

pub fn start_payload_download(
    host: String,
    port: u16,
    file_path: String,
    expected_size: i64,
    callback: Box<dyn PayloadTransferCallback>
) -> Result<TransferId>
```

### Challenge 3: Android Storage and Permissions ⚠️

**Problem**: File storage requires Android-specific APIs

**Android Requirements**:
- Storage permissions (READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
- Scoped storage (Android 10+)
- MediaStore integration
- Download manager
- File provider for sharing

**Solution**: Keep Android side implementation, Rust handles protocol only

**Separation of Concerns**:
- **Rust**: Protocol, packet parsing, network transfer
- **Android**: Storage, permissions, UI, system integration

---

## Proposed Architecture

### Layered Approach

```
┌─────────────────────────────────────────────────────────┐
│         Android Layer (Kotlin/Java)                     │
│  - Storage management (Scoped Storage API)              │
│  - Permissions handling                                 │
│  - UI (notifications, progress, settings)               │
│  - MediaStore integration                               │
│  - Intent handling                                      │
└─────────────────────────────────────────────────────────┘
                         ↓ ↑
          FFI Wrapper (Issue #52 wrappers)
                         ↓ ↑
┌─────────────────────────────────────────────────────────┐
│         Rust FFI Layer (cosmic-connect-core/ffi)        │
│  - Packet creation/parsing                              │
│  - Payload transfer coordination                        │
│  - Progress tracking                                    │
│  - Error handling                                       │
└─────────────────────────────────────────────────────────┘
                         ↓ ↑
┌─────────────────────────────────────────────────────────┐
│      Rust Core (cosmic-connect-core/plugins/share)      │
│  - Protocol logic                                       │
│  - TCP payload transfer                                 │
│  - Multi-file coordination                              │
│  - Network communication                                │
└─────────────────────────────────────────────────────────┘
```

### Component Responsibilities

**Rust Core Responsibilities**:
1. Parse share request packets
2. Establish TCP connection for payload
3. Stream bytes from remote device
4. Report progress via callback
5. Handle transfer errors

**Android Layer Responsibilities**:
1. Request/check storage permissions
2. Create file in appropriate location (Downloads, specific folder)
3. Write bytes received from Rust
4. Show notifications with progress
5. Scan media files
6. Handle share intents (send files)

---

## Implementation Plan

### Phase 1: Refactor Rust Plugin (4-6 hours)

**Tasks**:
1. Create `FfiDeviceInfo` struct
2. Update `SharePlugin::handle_share_request` signature
3. Remove `Device` dependencies
4. Add device info to plugin state
5. Update method signatures to match Plugin trait
6. Enable in `mod.rs`
7. Build and test compilation

**Deliverables**:
- Compiling share.rs
- No Device dependencies
- Ready for FFI exposure

### Phase 2: FFI Interface (3-4 hours)

**Tasks**:
1. Add share packet creation to FFI
2. Add payload transfer functions
3. Add progress callback interface
4. Add device info to FFI types
5. Generate UniFFI bindings
6. Build native libraries

**FFI Functions**:
```rust
// Create share packets
pub fn create_file_share_packet(
    filename: String,
    size: i64,
    creation_time: Option<i64>,
    last_modified: Option<i64>,
) -> Result<FfiPacket>

pub fn create_text_share_packet(text: String) -> Result<FfiPacket>

pub fn create_url_share_packet(url: String) -> Result<FfiPacket>

// Payload transfer
pub fn start_payload_download(
    device_host: String,
    port: u16,
    expected_size: i64,
    callback: Box<dyn PayloadCallback>
) -> Result<TransferId>

pub fn cancel_payload_transfer(transfer_id: TransferId) -> Result<()>
```

### Phase 3: Android Wrapper (2-3 hours)

**Tasks**:
1. Create `SharePacket` Kotlin wrapper
2. Create `PayloadTransfer` wrapper with callbacks
3. Add to existing wrappers in `Core/`
4. Write unit tests

**Wrapper API**:
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
}

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
    )

    fun cancel()
}
```

### Phase 4: Android Integration (4-6 hours)

**Tasks**:
1. Update SharePlugin.java to use FFI wrappers
2. Replace packet creation with wrapper calls
3. Integrate payload transfer with Android file I/O
4. Update notifications to use transfer callbacks
5. Test file, text, and URL sharing
6. Test multi-file transfers

### Phase 5: Testing & Polish (2-3 hours)

**Tasks**:
1. Test sending files from Android
2. Test receiving files on Android
3. Test text and URL sharing
4. Test large file transfers
5. Test error scenarios (network failure, permission denial)
6. Performance testing and optimization

**Total Estimate**: 15-22 hours

---

## Testing Strategy

### Unit Tests

**Rust Tests**:
- Packet parsing
- SharePlugin initialization
- Capability advertisement
- Helper method logic

**Android Tests**:
- Wrapper packet creation
- Payload transfer callback handling
- File path resolution
- Permission checking

### Integration Tests

**Cross-Platform Tests**:
1. Send file from Android → COSMIC Desktop
2. Send file from COSMIC Desktop → Android
3. Send text from Android → COSMIC Desktop
4. Send URL from COSMIC Desktop → Android
5. Multi-file transfer
6. Large file transfer (100+ MB)
7. Transfer cancellation
8. Network interruption handling

### Manual Testing Checklist

- [ ] Share image from gallery
- [ ] Share file from file manager
- [ ] Share text from notes app
- [ ] Share URL from browser
- [ ] Receive file notification works
- [ ] Download progress shows accurately
- [ ] Files appear in correct location
- [ ] Media scanner picks up shared images
- [ ] Multiple files transfer correctly
- [ ] Can cancel ongoing transfer
- [ ] Works with different file types (images, videos, documents)

---

## Dependencies

### Blocked By
- None (Issues #50, #51, #52 provide foundation)

### Blocks
- Issue #54: Clipboard Plugin (shares patterns with Share)
- Issue #55: Notification Plugin (shares progress UI patterns)

---

## Risk Assessment

### High Risk Items

1. **Payload Transfer Complexity**: File transfers across FFI boundary
   - Mitigation: Use proven callback pattern from other plugins

2. **Android Storage Changes**: Scoped storage in Android 10+
   - Mitigation: Use MediaStore APIs, test on multiple Android versions

3. **Memory Usage**: Large file transfers
   - Mitigation: Stream in chunks, don't load entire file

### Medium Risk Items

1. **Progress Accuracy**: Reporting progress across FFI
   - Mitigation: Test with various file sizes

2. **Error Handling**: Network failures during transfer
   - Mitigation: Comprehensive error scenarios and recovery

---

## Success Criteria

✅ **Rust Plugin Compiles**: share.rs builds without errors
✅ **FFI Bindings Generated**: UniFFI generates share packet functions
✅ **Android Wrappers Complete**: SharePackets and PayloadTransfer work
✅ **File Sharing Works**: Can send/receive files Android ↔ COSMIC
✅ **Text/URL Sharing Works**: Can share text and URLs
✅ **Progress Tracking Works**: Accurate progress for file transfers
✅ **Tests Pass**: Unit and integration tests pass
✅ **Performance Acceptable**: Large files transfer efficiently
✅ **No Regressions**: Existing share functionality still works

---

## Next Steps

**When Ready to Start**:

1. Review and approve this plan
2. Allocate 15-22 hours for implementation
3. Start with Phase 1 (Rust refactoring)
4. Proceed sequentially through phases
5. Test thoroughly at each phase

**Prerequisites**:
- Issues #50, #51, #52 complete ✅
- Build environment working (for testing)
- Access to both Android device and COSMIC Desktop for integration testing

---

**Document Version**: 1.0
**Last Updated**: 2026-01-16
**Status**: Ready for Implementation

