# Debugging Skill for Android and Rust Development

## Overview
This skill provides comprehensive debugging techniques, tools, and best practices for both Android (Kotlin/Java) and Rust/COSMIC Desktop development, with focus on network programming and COSMIC Connect protocol debugging.

## Android Debugging

### Logcat Best Practices

#### Custom Logging Tags
```kotlin
object Log {
    private const val APP_TAG = "CosmicConnect"
    
    fun d(tag: String, message: String) {
        android.util.Log.d("$APP_TAG:$tag", message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        android.util.Log.e("$APP_TAG:$tag", message, throwable)
    }
    
    fun i(tag: String, message: String) {
        android.util.Log.i("$APP_TAG:$tag", message)
    }
    
    fun w(tag: String, message: String) {
        android.util.Log.w("$APP_TAG:$tag", message)
    }
    
    fun v(tag: String, message: String) {
        android.util.Log.v("$APP_TAG:$tag", message)
    }
}

// Usage
Log.d("Discovery", "Broadcasting identity packet")
Log.e("TLS", "Handshake failed", exception)
```

#### Filtering Logcat
```bash
# Filter by tag
adb logcat -s CosmicConnect:D

# Filter multiple tags
adb logcat -s CosmicConnect:D NetworkPacket:D TLS:V

# Filter by process ID
adb logcat --pid=$(adb shell pidof -s org.cosmic.cosmicconnect)

# Filter with grep
adb logcat | grep "CosmicConnect"

# Save to file
adb logcat -d > logcat.txt

# Clear buffer
adb logcat -c

# Monitor specific time
adb logcat -T '01-15 10:00:00.000'
```

### Android Studio Debugger

#### Breakpoint Techniques
```kotlin
class DeviceConnection {
    fun connect() {
        // Conditional breakpoint: right-click breakpoint, set condition
        // Condition: deviceId == "target-device-id"
        val socket = createSocket()
        
        // Log breakpoint: disable suspension, add log message
        // Message: "Connecting to {deviceId}"
        performHandshake(socket)
        
        // Temporary breakpoint: Ctrl+Alt+Shift+F8
        // Removes automatically after first hit
        establishConnection(socket)
    }
}
```

#### Evaluate Expression
```kotlin
// While paused at breakpoint, evaluate expressions in Debug window
// Alt+F8 to open Evaluate Expression dialog

// Check device state
deviceManager.getDevice(deviceId).isConnected

// Modify variables
deviceState = DeviceState.CONNECTED

// Call methods
certificateManager.getCertificateFingerprint()

// Complex expressions
devices.filter { it.isConnected }.map { it.name }
```

### Network Traffic Analysis

#### Android Network Profiler
1. Open Android Studio Profiler
2. Select "Network" profiler
3. Monitor connections in real-time
4. Inspect request/response details
5. Analyze traffic patterns

#### Packet Capture with tcpdump
```bash
# Install tcpdump on device (requires root)
adb root
adb remount
adb push tcpdump /system/xbin/
adb shell chmod 755 /system/xbin/tcpdump

# Capture packets
adb shell tcpdump -i any -s 0 -w /sdcard/capture.pcap

# Filter by port
adb shell tcpdump -i any port 1714 -w /sdcard/cosmicconnect.pcap

# Pull and analyze
adb pull /sdcard/cosmicconnect.pcap
wireshark cosmicconnect.pcap
```

### Memory Profiling

#### Detecting Memory Leaks
```kotlin
// Use LeakCanary
dependencies {
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}

// Custom memory monitoring
class MemoryMonitor {
    fun logMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        
        Log.d("Memory", "Used: ${usedMemory}MB / Max: ${maxMemory}MB")
    }
}
```

#### Heap Dump Analysis
```bash
# Capture heap dump
adb shell am dumpheap org.cosmic.cosmicconnect /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof

# Convert to standard format
hprof-conv heap.hprof heap-converted.hprof

# Analyze in Android Studio
# File > Open... > Select heap-converted.hprof
```

### Crash Reporting

#### Firebase Crashlytics Integration
```kotlin
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}

// Log custom information
FirebaseCrashlytics.getInstance().apply {
    setUserId(deviceId)
    setCustomKey("device_type", deviceType)
    setCustomKey("protocol_version", protocolVersion)
    log("Attempting connection to device: $deviceId")
}

// Log non-fatal exceptions
try {
    connectToDevice()
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
    throw e
}
```

#### Custom Crash Handler
```kotlin
class CustomExceptionHandler : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Log crash details
        Log.e("Crash", "Uncaught exception in thread ${thread.name}", throwable)
        
        // Save crash report
        saveCrashReport(throwable)
        
        // Call default handler
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    private fun saveCrashReport(throwable: Throwable) {
        val report = buildString {
            appendLine("Crash Report")
            appendLine("Time: ${System.currentTimeMillis()}")
            appendLine("Thread: ${Thread.currentThread().name}")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine("\nStack Trace:")
            throwable.stackTrace.forEach {
                appendLine("  at $it")
            }
        }
        
        // Save to file
        val file = File(context.cacheDir, "crash_${System.currentTimeMillis()}.txt")
        file.writeText(report)
    }
}

// Register in Application class
Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler())
```

### Performance Profiling

#### Method Tracing
```kotlin
// Manual tracing
Debug.startMethodTracing("cosmicconnect")
// ... code to profile
Debug.stopMethodTracing()

// Pull trace file
adb pull /sdcard/Android/data/org.cosmic.cosmicconnect/files/cosmicconnect.trace

// Analyze in Android Studio
// File > Open... > Select trace file
```

#### Systrace
```bash
# Capture system trace
python systrace.py --time=10 -o trace.html sched freq idle am wm gfx view \
    binder_driver hal dalvik camera input res

# View in browser
chrome trace.html
```

## Rust Debugging

### Logging with tracing

#### Setup
```rust
use tracing::{debug, error, info, warn, trace, instrument};
use tracing_subscriber::{fmt, EnvFilter};

fn init_logging() {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| EnvFilter::new("info"))
        )
        .with_target(true)
        .with_thread_ids(true)
        .with_file(true)
        .with_line_number(true)
        .init();
}

// Usage
#[instrument]
async fn connect_to_device(device_id: &str) -> Result<(), Error> {
    debug!("Attempting connection to device: {}", device_id);
    
    match establish_connection(device_id).await {
        Ok(conn) => {
            info!("Successfully connected to {}", device_id);
            Ok(())
        }
        Err(e) => {
            error!("Connection failed: {:?}", e);
            Err(e)
        }
    }
}
```

#### Structured Logging
```rust
use tracing::field;

#[instrument(
    skip(socket),
    fields(
        device_id = %device_id,
        remote_addr = ?socket.peer_addr(),
        connection_time = field::Empty,
    )
)]
async fn handle_connection(device_id: String, socket: TlsStream<TcpStream>) {
    let start = std::time::Instant::now();
    
    // ... handle connection
    
    tracing::Span::current().record(
        "connection_time",
        format!("{:?}", start.elapsed())
    );
}
```

### GDB Debugging

#### Basic GDB Commands
```bash
# Compile with debug symbols
cargo build --bin cosmic-cosmicconnect

# Start GDB
gdb target/debug/cosmic-cosmicconnect

# Common commands
(gdb) break main                  # Set breakpoint
(gdb) break src/connection.rs:42  # Breakpoint at line
(gdb) run                         # Start execution
(gdb) next                        # Step over
(gdb) step                        # Step into
(gdb) continue                    # Continue execution
(gdb) print variable              # Print variable
(gdb) backtrace                   # Show call stack
(gdb) info locals                 # Show local variables
(gdb) watch variable              # Set watchpoint
(gdb) quit                        # Exit GDB
```

#### Rust-specific GDB Commands
```bash
# Pretty-print Rust types
(gdb) set print pretty on
(gdb) set print elements 0

# Print Vec contents
(gdb) print devices

# Print String
(gdb) print *device_name

# Examine memory
(gdb) x/10x $variable_address
```

### LLDB Debugging

#### Basic LLDB Commands
```bash
# Start LLDB
lldb target/debug/cosmic-cosmicconnect

# Common commands
(lldb) breakpoint set --name main
(lldb) breakpoint set --file connection.rs --line 42
(lldb) run
(lldb) next
(lldb) step
(lldb) continue
(lldb) print variable
(lldb) bt
(lldb) frame variable
(lldb) quit
```

#### Rust-specific LLDB
```bash
# Use rust-lldb wrapper
rust-lldb target/debug/cosmic-cosmicconnect

# Pretty-print Rust types automatically enabled

# Examine Option
(lldb) print device
# Shows Some(Device { ... }) or None

# Examine Result
(lldb) print result
# Shows Ok(...) or Err(...)
```

### VS Code Debugging

#### launch.json Configuration
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "lldb",
            "request": "launch",
            "name": "Debug cosmic-cosmicconnect",
            "cargo": {
                "args": [
                    "build",
                    "--bin=cosmic-cosmicconnect",
                    "--package=cosmic-cosmicconnect"
                ],
                "filter": {
                    "name": "cosmic-cosmicconnect",
                    "kind": "bin"
                }
            },
            "args": [],
            "cwd": "${workspaceFolder}",
            "env": {
                "RUST_LOG": "debug",
                "RUST_BACKTRACE": "1"
            }
        },
        {
            "type": "lldb",
            "request": "launch",
            "name": "Debug tests",
            "cargo": {
                "args": [
                    "test",
                    "--no-run",
                    "--lib",
                    "--package=cosmicconnect-protocol"
                ],
                "filter": {
                    "name": "cosmicconnect-protocol",
                    "kind": "lib"
                }
            },
            "args": [],
            "cwd": "${workspaceFolder}"
        }
    ]
}
```

### Memory Debugging

#### Valgrind
```bash
# Install valgrind
sudo apt install valgrind

# Check for memory leaks
valgrind --leak-check=full --show-leak-kinds=all \
    target/debug/cosmic-cosmicconnect

# Track origins of uninitialized values
valgrind --track-origins=yes target/debug/cosmic-cosmicconnect
```

#### AddressSanitizer
```bash
# Compile with AddressSanitizer
RUSTFLAGS="-Z sanitizer=address" cargo +nightly build --target x86_64-unknown-linux-gnu

# Run
ASAN_OPTIONS=detect_leaks=1 target/x86_64-unknown-linux-gnu/debug/cosmic-cosmicconnect
```

### Performance Profiling

#### cargo-flamegraph
```bash
# Install
cargo install flamegraph

# Profile
cargo flamegraph --bin cosmic-cosmicconnect

# Open flamegraph.svg in browser
```

#### perf
```bash
# Record
perf record --call-graph dwarf target/debug/cosmic-cosmicconnect

# Analyze
perf report

# Generate flamegraph
perf script | stackcollapse-perf.pl | flamegraph.pl > perf.svg
```

## Network Debugging

### Wireshark Analysis

#### Capture Filters
```
# COSMIC Connect specific
tcp port 1714 or tcp portrange 1714-1764 or udp port 1716

# TLS traffic
ssl.handshake or tls.handshake

# Specific IP
ip.addr == 192.168.1.100
```

#### Display Filters
```
# Show only COSMIC Connect packets
tcp.port >= 1714 and tcp.port <= 1764

# Show TLS handshake
ssl.handshake.type == 1

# Show certificate messages
ssl.handshake.type == 11

# Follow TCP stream
tcp.stream eq 0

# Show retransmissions
tcp.analysis.retransmission
```

### TLS Debugging

#### Enable TLS Key Logging (Rust)
```bash
# Set environment variable
export SSLKEYLOGFILE=$PWD/tls-keys.txt

# Run application
RUST_LOG=debug cargo run

# Decrypt in Wireshark
# Edit > Preferences > Protocols > TLS
# (Pre)-Master-Secret log filename: /path/to/tls-keys.txt
```

#### OpenSSL Debugging
```bash
# Test TLS connection
openssl s_client -connect localhost:1714 -showcerts -debug

# View certificate details
openssl x509 -in cert.pem -text -noout

# Verify certificate
openssl verify -CAfile ca.pem cert.pem

# Test cipher suites
openssl s_client -connect localhost:1714 -cipher 'ECDHE-RSA-AES256-GCM-SHA384'
```

## Testing Strategies

### Android Unit Testing

#### JUnit Tests
```kotlin
@RunWith(JUnit4::class)
class NetworkPacketTest {
    @Test
    fun `packet serialization works correctly`() {
        val packet = NetworkPacket(
            id = 123,
            type = "cosmicconnect.ping",
            body = JSONObject().apply {
                put("message", "test")
            }
        )
        
        val bytes = packet.toByteArray()
        val deserialized = NetworkPacket.fromInputStream(
            ByteArrayInputStream(bytes)
        )
        
        assertNotNull(deserialized)
        assertEquals(packet.id, deserialized?.id)
        assertEquals(packet.type, deserialized?.type)
    }
}
```

#### Mockito Tests
```kotlin
@RunWith(MockitoJUnitRunner::class)
class DeviceManagerTest {
    @Mock
    lateinit var certificateManager: CertificateManager
    
    @Mock
    lateinit var context: Context
    
    @InjectMocks
    lateinit var deviceManager: DeviceManager
    
    @Test
    fun `device connection succeeds with valid certificate`() {
        val mockCert = mock(X509Certificate::class.java)
        whenever(certificateManager.getDeviceCertificate())
            .thenReturn(mockCert)
        
        val result = deviceManager.connectToDevice("test-device")
        
        assertTrue(result.isSuccess)
        verify(certificateManager).getDeviceCertificate()
    }
}
```

### Rust Testing

#### Unit Tests
```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_packet_serialization() {
        let packet = NetworkPacket {
            id: 123,
            packet_type: "cosmicconnect.ping".to_string(),
            body: serde_json::json!({
                "message": "test"
            }),
            payload_size: None,
            payload_transfer_info: None,
        };
        
        let json = serde_json::to_string(&packet).unwrap();
        let deserialized: NetworkPacket = serde_json::from_str(&json).unwrap();
        
        assert_eq!(packet.id, deserialized.id);
        assert_eq!(packet.packet_type, deserialized.packet_type);
    }
    
    #[tokio::test]
    async fn test_device_discovery() {
        let discovery = Discovery::new("test-device".to_string(), 1714)
            .await
            .unwrap();
        
        let result = discovery.broadcast_identity().await;
        assert!(result.is_ok());
    }
}
```

#### Integration Tests
```rust
// tests/integration_test.rs
use cosmicconnect_protocol::*;

#[tokio::test]
async fn test_full_connection_flow() {
    // Start server
    let server = spawn_test_server().await;
    
    // Connect client
    let client = DeviceConnection::connect(
        "localhost",
        server.port(),
        "client-id",
    ).await.unwrap();
    
    // Send ping
    client.send_ping("Hello").await.unwrap();
    
    // Verify received
    let received = server.receive_packet().await.unwrap();
    assert_eq!(received.packet_type, "cosmicconnect.ping");
}
```

#### Property-Based Testing
```rust
use proptest::prelude::*;

proptest! {
    #[test]
    fn test_packet_roundtrip(
        id in any::<i64>(),
        msg in "\\PC*",
    ) {
        let packet = NetworkPacket {
            id,
            packet_type: "cosmicconnect.ping".to_string(),
            body: serde_json::json!({ "message": msg }),
            payload_size: None,
            payload_transfer_info: None,
        };
        
        let json = serde_json::to_string(&packet).unwrap();
        let deserialized: NetworkPacket = serde_json::from_str(&json).unwrap();
        
        prop_assert_eq!(packet.id, deserialized.id);
        prop_assert_eq!(packet.packet_type, deserialized.packet_type);
    }
}
```

## Debugging Checklist

### Before Debugging
- [ ] Reproduce the issue consistently
- [ ] Gather relevant logs and crash reports
- [ ] Note environment details (device, OS version, network)
- [ ] Identify recent changes that might have caused the issue

### During Debugging
- [ ] Set appropriate breakpoints
- [ ] Use conditional breakpoints for specific scenarios
- [ ] Monitor memory usage
- [ ] Check network traffic
- [ ] Verify TLS handshake success
- [ ] Validate packet formats

### After Debugging
- [ ] Document the root cause
- [ ] Write a regression test
- [ ] Update error handling
- [ ] Add logging for future debugging
- [ ] Review related code for similar issues

## Common Issues and Solutions

### Android
1. **Network on Main Thread**: Use coroutines or AsyncTask
2. **SSL Handshake Timeout**: Check firewall, increase timeout
3. **Certificate Validation Failed**: Verify fingerprints, check certificate format
4. **Memory Leak**: Use LeakCanary, check for context leaks
5. **ANR**: Profile with systrace, optimize long operations

### Rust
1. **Borrow Checker Errors**: Use Arc/Mutex for shared state
2. **Async Runtime Issues**: Ensure proper tokio runtime setup
3. **TLS Handshake Failed**: Verify certificate format, check cipher suites
4. **Deadlock**: Use tokio::sync primitives, avoid blocking operations
5. **Performance Issues**: Profile with flamegraph, optimize hot paths

## Resources

- [Android Debug Bridge (adb)](https://developer.android.com/studio/command-line/adb)
- [Android Profiler](https://developer.android.com/studio/profile/android-profiler)
- [Rust Debugging](https://doc.rust-lang.org/book/ch09-00-error-handling.html)
- [tracing Documentation](https://docs.rs/tracing/)
- [Wireshark User Guide](https://www.wireshark.org/docs/wsug_html/)
