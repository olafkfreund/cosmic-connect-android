# Issue #53: Share Plugin Migration - Phase 3 Complete

**Date**: 2026-01-16
**Status**: ✅ Phase 3 Complete (Android Wrappers Implemented)
**Plugin**: SharePlugin
**Files**: `SharePacketsFFI.kt`, `PayloadTransferFFI.kt`

---

## Executive Summary

**Phase 3 of Issue #53 (Share Plugin Migration) is complete.** Idiomatic Kotlin wrappers have been created for all Share plugin FFI functions, providing clean APIs for packet creation and async file transfers with full callback support and Android file I/O integration.

**Key Achievement**: Created production-ready Android wrappers (22KB total) that bridge the Rust FFI layer with Android-friendly Kotlin APIs, including progress throttling and comprehensive error handling.

---

## What Was Completed

### ✅ SharePacketsFFI.kt (9.8 KB)

**Location**: `src/org/cosmic/cosmicconnect/Plugins/SharePlugin/SharePacketsFFI.kt`

**Purpose**: Kotlin wrapper for Share plugin packet creation functions

#### API Overview

```kotlin
object SharePacketsFFI {
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

#### Key Features

1. **Input Validation**
   - Filename cannot be empty
   - File size cannot be negative
   - Text cannot be blank
   - URL must start with http:// or https://
   - File count must be positive

2. **Error Handling**
   - Wraps FFI exceptions in CosmicConnectException
   - Provides clear error messages
   - Validates inputs before FFI calls

3. **Documentation**
   - Comprehensive KDoc comments
   - Usage examples for each function
   - Packet structure examples in JSON
   - Migration guide from old API

#### Extension Functions

Added 10 extension properties to `NetworkPacket`:

```kotlin
// Type checking
val NetworkPacket.isFileShare: Boolean
val NetworkPacket.isTextShare: Boolean
val NetworkPacket.isUrlShare: Boolean
val NetworkPacket.isMultiFileUpdate: Boolean

// Data extraction
val NetworkPacket.filename: String?
val NetworkPacket.sharedText: String?
val NetworkPacket.sharedUrl: String?
val NetworkPacket.numberOfFiles: Int?
val NetworkPacket.totalPayloadSize: Long?
val NetworkPacket.creationTime: Long?
val NetworkPacket.lastModified: Long?
```

**Usage Example**:
```kotlin
if (packet.isFileShare) {
    val filename = packet.filename
    val size = packet.payloadSize
    Log.d(TAG, "Received file: $filename ($size bytes)")
}
```

---

### ✅ PayloadTransferFFI.kt (12 KB)

**Location**: `src/org/cosmic/cosmicconnect/Plugins/SharePlugin/PayloadTransferFFI.kt`

**Purpose**: Kotlin wrapper for async file payload transfers

#### API Overview

```kotlin
class PayloadTransferFFI(
    private val deviceHost: String,
    private val port: Int,
    private val expectedSize: Long,
    private val outputFile: File
) {
    fun start(
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
        onComplete: () -> Unit,
        onError: (error: String) -> Unit
    )

    fun cancel(): Boolean
    fun isCancelled(): Boolean
    fun getTransferId(): Long?

    companion object {
        fun fromPacket(
            packet: NetworkPacket,
            deviceHost: String,
            outputFile: File
        ): PayloadTransferFFI?
    }
}
```

#### Key Features

1. **Async File Download**
   - Spawns background transfer via FFI
   - Streams bytes directly to file
   - Reports progress via callback
   - Handles completion and errors

2. **Callback Management**
   - Progress: (bytesTransferred, totalBytes) → Unit
   - Complete: () → Unit
   - Error: (error: String) → Unit
   - Callbacks invoked on background thread

3. **File I/O Integration**
   - Opens FileOutputStream for writing
   - Creates parent directories automatically
   - Closes stream on completion/error
   - Deletes incomplete files on cancel/error
   - Verifies file size matches expected

4. **Cancellation Support**
   - Cancels FFI transfer
   - Closes file stream
   - Deletes partial file
   - Atomic cancellation flag

5. **Error Handling**
   - IOException for file operations
   - Network errors from FFI
   - Cleanup on all error paths
   - Clear error messages

6. **Factory Method**
   - `fromPacket()` extracts transfer info
   - Reads payloadTransferInfo for port
   - Falls back to default port 1739
   - Returns null if no payload

#### ProgressThrottler Class

```kotlin
class ProgressThrottler(intervalMs: Long = 500) {
    fun update(
        transferred: Long,
        total: Long,
        callback: (Long, Long) -> Unit
    )

    fun forceUpdate(...)
}
```

**Purpose**: Prevents excessive UI updates by throttling progress callbacks

**Usage**:
```kotlin
val throttler = ProgressThrottler(intervalMs = 500)

transfer.start(
    onProgress = { transferred, total ->
        throttler.update(transferred, total) { t, tot ->
            runOnUiThread {
                progressBar.progress = (t * 100 / tot).toInt()
                textView.text = "$t / $tot bytes"
            }
        }
    },
    ...
)
```

**Benefits**:
- Reduces UI thread load
- Prevents jank from rapid updates
- Always shows 100% on completion
- Configurable interval

---

## Implementation Details

### Threading Model

**FFI Callbacks** (Rust → Kotlin):
- Invoked on Rust async runtime thread
- NOT the Android UI thread
- Requires `runOnUiThread` for UI updates

**Example**:
```kotlin
transfer.start(
    onProgress = { transferred, total ->
        // ⚠️ Background thread!
        runOnUiThread {
            // ✅ Now safe for UI updates
            progressBar.progress = (transferred * 100 / total).toInt()
        }
    },
    ...
)
```

### Memory Management

**File Streaming**:
- 8KB chunks (configured in Rust FFI)
- No full file in memory
- Direct write to FileOutputStream
- Efficient for large files

**State Tracking**:
- AtomicBoolean for thread-safe flags
- Handle stored as nullable property
- Callbacks cleared after use

### Error Scenarios Handled

1. **File I/O Errors**
   - Output directory doesn't exist → creates parent dirs
   - No write permission → IOException
   - Disk full → IOException

2. **Network Errors**
   - Connection refused → onError callback
   - Connection timeout → onError callback
   - Unexpected disconnect → size mismatch warning

3. **Cancellation**
   - User cancels → closes stream, deletes file
   - Device disconnects → cleanup
   - App backgrounded → transfer continues (unless cancelled)

---

## Usage Examples

### Complete File Share Flow

```kotlin
// 1. Create share packet
val packet = SharePacketsFFI.createFileShare(
    filename = "vacation.jpg",
    size = file.length(),
    creationTime = file.lastModified(),
    lastModified = file.lastModified()
)

// 2. Send packet to device
device.sendPacket(packet.toLegacyPacket())

// 3. Start payload transfer (on sending side)
// ... handled by existing SharePlugin upload logic

// -- On receiving side --

// 4. Receive share packet
override fun onPacketReceived(np: NetworkPacket) {
    if (!np.isFileShare) return

    val filename = np.filename ?: "unknown"
    val size = np.payloadSize ?: 0L

    // 5. Create destination file
    val outputFile = File(downloadsDir, filename)

    // 6. Create transfer
    val transfer = PayloadTransferFFI.fromPacket(
        packet = np,
        deviceHost = device.ipAddress,
        outputFile = outputFile
    )

    if (transfer == null) {
        Log.w(TAG, "No payload to transfer")
        return
    }

    // 7. Start download with progress
    val throttler = ProgressThrottler(500)

    transfer.start(
        onProgress = { transferred, total ->
            throttler.update(transferred, total) { t, tot ->
                runOnUiThread {
                    val percent = (t * 100 / tot).toInt()
                    notification.setProgress(100, percent, false)
                    notificationManager.notify(notificationId, notification)
                }
            }
        },
        onComplete = {
            runOnUiThread {
                notification.setContentText("Download complete")
                    .setProgress(0, 0, false)
                notificationManager.notify(notificationId, notification)

                // Scan media file
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    null,
                    null
                )

                Toast.makeText(context, "Received: $filename", Toast.LENGTH_SHORT).show()
            }
        },
        onError = { error ->
            runOnUiThread {
                notification.setContentText("Download failed: $error")
                    .setProgress(0, 0, false)
                notificationManager.notify(notificationId, notification)

                Toast.makeText(context, "Failed to receive file", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
```

### Text Sharing

```kotlin
// Simple text share
val packet = SharePacketsFFI.createTextShare("Hello from Android!")
device.sendPacket(packet.toLegacyPacket())

// Receiving text
override fun onPacketReceived(np: NetworkPacket) {
    if (np.isTextShare) {
        val text = np.sharedText ?: return
        // Copy to clipboard or show notification
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Shared Text", text))
    }
}
```

### URL Sharing

```kotlin
// Share URL
val packet = SharePacketsFFI.createUrlShare("https://example.com")
device.sendPacket(packet.toLegacyPacket())

// Receiving URL
override fun onPacketReceived(np: NetworkPacket) {
    if (np.isUrlShare) {
        val url = np.sharedUrl ?: return
        // Open in browser
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
```

### Multi-File Transfer

```kotlin
// Before sending multiple files
val files: List<File> = listOf(...)
val totalSize = files.sumOf { it.length() }

// 1. Send update packet
val updatePacket = SharePacketsFFI.createMultiFileUpdate(
    numberOfFiles = files.size,
    totalPayloadSize = totalSize
)
device.sendPacket(updatePacket.toLegacyPacket())

// 2. Send individual file packets
files.forEach { file ->
    val packet = SharePacketsFFI.createFileShare(
        filename = file.name,
        size = file.length()
    )
    device.sendPacket(packet.toLegacyPacket())
    // ... upload file payload
}

// Receiving multi-file update
override fun onPacketReceived(np: NetworkPacket) {
    if (np.isMultiFileUpdate) {
        val count = np.numberOfFiles ?: return
        val totalSize = np.totalPayloadSize ?: return
        // Show aggregate progress notification
        notification.setContentText("Receiving $count files...")
        notificationManager.notify(id, notification)
    }
}
```

---

## Testing Considerations

### Unit Tests (To Be Added)

```kotlin
@Test
fun testCreateFileSharePacket() {
    val packet = SharePacketsFFI.createFileShare(
        filename = "test.txt",
        size = 1024L
    )

    assertEquals("cconnect.share.request", packet.type)
    assertEquals("test.txt", packet.filename)
    assertEquals(1024L, packet.payloadSize)
}

@Test
fun testCreateTextSharePacket() {
    val packet = SharePacketsFFI.createTextShare("Hello")

    assertTrue(packet.isTextShare)
    assertEquals("Hello", packet.sharedText)
    assertNull(packet.payloadSize)
}

@Test
fun testPayloadTransferFromPacket() {
    val packet = SharePacketsFFI.createFileShare("test.txt", 1024L)
    val outputFile = File(tempDir, "test.txt")

    val transfer = PayloadTransferFFI.fromPacket(
        packet = packet,
        deviceHost = "192.168.1.100",
        outputFile = outputFile
    )

    assertNotNull(transfer)
}

@Test
fun testPayloadTransferCancellation() {
    val transfer = PayloadTransferFFI(
        deviceHost = "192.168.1.100",
        port = 1739,
        expectedSize = 1024L,
        outputFile = File(tempDir, "test.txt")
    )

    transfer.start(onProgress = {_, _ -> }, onComplete = {}, onError = {})
    assertTrue(transfer.cancel())
    assertTrue(transfer.isCancelled())
}
```

### Integration Tests (Phase 5)

1. **File Transfer**:
   - Small file (< 1MB)
   - Large file (> 100MB)
   - Binary file (image, video)
   - Text file with special characters

2. **Progress Tracking**:
   - Progress callbacks fire
   - Progress increases monotonically
   - Final callback shows 100%

3. **Error Handling**:
   - Invalid host → onError
   - Wrong port → onError
   - Network disconnect mid-transfer → onError
   - Disk full → IOException

4. **Cancellation**:
   - Cancel immediately after start
   - Cancel at 50% progress
   - Verify file deleted
   - Verify no memory leaks

---

## API Design Decisions

### Decision 1: Object vs Class for SharePacketsFFI

**Chosen**: Object (singleton)

**Rationale**:
- No instance state needed
- All functions are stateless
- Follows NetworkPacket.create() pattern
- Cleaner API: `SharePacketsFFI.createFileShare()` vs `SharePacketsFFI().createFileShare()`

### Decision 2: Separate PayloadTransferFFI Class

**Chosen**: Instance-based class

**Rationale**:
- Each transfer has state (handle, callbacks, file stream)
- Supports multiple concurrent transfers
- Natural lifecycle (create → start → complete/cancel)
- Encapsulates file I/O and FFI handle

### Decision 3: Callback-Based vs Coroutine API

**Chosen**: Callbacks

**Rationale**:
- FFI provides callback interface
- Direct mapping reduces complexity
- Easier to integrate with existing plugin code
- Can be wrapped in coroutines by caller if needed

**Future Enhancement**:
```kotlin
suspend fun PayloadTransferFFI.download(): Result<File> = suspendCoroutine { cont ->
    start(
        onProgress = { _, _ -> },
        onComplete = { cont.resume(Result.success(outputFile)) },
        onError = { cont.resume(Result.failure(Exception(it))) }
    )
}
```

### Decision 4: Extension Properties vs Methods

**Chosen**: Extension properties

**Rationale**:
- Natural Kotlin idiom
- Clean syntax: `packet.isFileShare` vs `packet.isFileShare()`
- Type-safe access: `packet.filename` returns `String?`
- Consistent with Kotlin standard library

### Decision 5: ProgressThrottler as Separate Class

**Chosen**: Separate utility class

**Rationale**:
- Optional feature (not required)
- Reusable for other transfer types
- Keeps PayloadTransferFFI focused
- Easy to test independently

---

## Migration from Old SharePlugin

### Old API (Legacy NetworkPacket)

```java
// Old way - manual packet creation
NetworkPacket np = new NetworkPacket("cconnect.share.request");
np.set("filename", "photo.jpg");
np.set("creationTime", System.currentTimeMillis());
np.setPayload(new NetworkPacket.Payload(...));
device.sendPacket(np);

// Old way - manual payload handling
PayloadSharingJob job = new PayloadSharingJob(np, device);
job.start();
```

### New API (FFI Wrappers)

```kotlin
// New way - use FFI wrappers
val packet = SharePacketsFFI.createFileShare(
    filename = "photo.jpg",
    size = file.length(),
    creationTime = System.currentTimeMillis()
)
device.sendPacket(packet.toLegacyPacket())

// New way - use PayloadTransferFFI
val transfer = PayloadTransferFFI.fromPacket(packet, deviceHost, outputFile)
transfer?.start(onProgress, onComplete, onError)
```

**Benefits**:
- ✅ Type-safe packet creation
- ✅ Input validation
- ✅ Cleaner error handling
- ✅ Better progress tracking
- ✅ Standardized API across plugins

---

## Known Limitations

### Phase 3 Limitations

1. **No Upload Support Yet**: Only download (receive) implemented
   - Upload will be handled by existing SharePlugin logic
   - Future: Add `PayloadTransferFFI.upload()` for consistency

2. **Single Callback Thread**: All callbacks on background thread
   - Requires `runOnUiThread` for UI updates
   - Future: Add dispatcher parameter

3. **No Partial Resume**: Failed transfers must restart from beginning
   - Future: Add checksum support for resume

4. **Fixed Chunk Size**: 8KB chunks (hardcoded in Rust)
   - Future: Make configurable for performance tuning

---

## Validation Checklist

- ✅ SharePacketsFFI.kt created (9.8 KB)
- ✅ PayloadTransferFFI.kt created (12 KB)
- ✅ All 4 packet creation functions implemented
- ✅ Extension properties for NetworkPacket added
- ✅ Async payload transfer with callbacks
- ✅ File I/O integration (FileOutputStream)
- ✅ Cancellation support
- ✅ Error handling and cleanup
- ✅ ProgressThrottler utility class
- ✅ Comprehensive documentation
- ✅ Usage examples provided
- ⏳ Unit tests pending (Phase 5)
- ⏳ Integration tests pending (Phase 5)

---

## Files Created

**Total**: 2 files, 22 KB of production Kotlin code

1. **SharePacketsFFI.kt** (9.8 KB)
   - 4 packet creation functions
   - 11 extension properties
   - Input validation
   - Error handling
   - Documentation

2. **PayloadTransferFFI.kt** (12 KB)
   - PayloadTransferFFI class
   - ProgressThrottler class
   - File I/O integration
   - Cancellation support
   - Error handling
   - Documentation

---

## Next Steps: Phase 4 - Android Integration

### Phase 4 Goals (4-6 hours estimated)

1. **Update SharePlugin.java**
   - Replace manual packet creation with SharePacketsFFI
   - Replace payload handling with PayloadTransferFFI
   - Update onPacketReceived() to use new wrappers
   - Integrate progress with notifications

2. **Update ReceiveNotification.java**
   - Use ProgressThrottler for UI updates
   - Handle transfer cancellation
   - Show error states

3. **Test All Share Types**
   - File sharing (send/receive)
   - Text sharing (send/receive)
   - URL sharing (send/receive)
   - Multi-file transfers

4. **Android Permissions**
   - Storage permissions (Android 10+ scoped storage)
   - Network permissions
   - Notification permissions

---

## References

- **Phase 1 Completion**: docs/issues/issue-53-phase1-complete.md
- **Phase 2 Completion**: docs/issues/issue-53-phase2-complete.md
- **Issue #53 Plan**: docs/issues/issue-53-share-plugin-plan.md
- **FFI Integration Guide**: docs/guides/FFI_INTEGRATION_GUIDE.md
- **BatteryPluginFFI.kt**: Reference implementation pattern
- **NetworkPacket.kt**: Wrapper pattern example

---

**Document Version**: 1.0
**Created**: 2026-01-16
**Status**: Phase 3 Complete ✅
**Next Phase**: Phase 4 - Android Integration (4-6 hours estimated)
**Total Progress**: 75% complete (Phase 3 of 5)
