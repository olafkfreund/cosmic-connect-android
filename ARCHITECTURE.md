# COSMIC Connect - Rust + Kotlin Hybrid Architecture
## Detailed Technical Architecture

## 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [Architecture Overview](#architecture-overview)
3. [Component Breakdown](#component-breakdown)
4. [FFI Interface Design](#ffi-interface-design)
5. [Data Flow](#data-flow)
6. [Security Architecture](#security-architecture)
7. [Threading and Concurrency](#threading-and-concurrency)
8. [Build System Integration](#build-system-integration)
9. [Testing Strategy](#testing-strategy)
10. [Performance Considerations](#performance-considerations)
11. [Migration Path](#migration-path)

---

## Executive Summary

COSMIC Connect uses a hybrid Rust + Kotlin architecture to achieve:
- **70%+ code sharing** between COSMIC Desktop and Android
- **Memory safety** for critical networking and crypto code
- **Modern Android UX** with Kotlin and Jetpack Compose
- **Single protocol implementation** shared across platforms

### Key Design Principles

1. **Separation of Concerns**: Rust for protocol, Kotlin for UI/Android services
2. **Type Safety**: Strong typing across FFI boundary
3. **Memory Safety**: Rust ownership prevents leaks and data races
4. **Platform Optimization**: Each component in its ideal language
5. **Testability**: Independent testing of each layer

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    COSMIC Desktop (Rust)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ COSMIC UI    │  │ D-Bus IPC    │  │   Applet     │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                  │
│         └──────────────────┴──────────────────┘                 │
│                            │                                     │
└────────────────────────────┼─────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                 cosmic-connect-core (Rust)                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Protocol Layer                        │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │  Packets   │  │ Discovery  │  │   Pairing  │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     Network Layer                        │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │ TCP/UDP    │  │  Multicast │  │   Sockets  │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Crypto Layer                          │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │    TLS     │  │    Certs   │  │  Hashing   │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Plugin Layer                          │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │  Manager   │  │  Registry  │  │   Routing  │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                      FFI Layer                           │  │
│  │  ┌────────────────────────────────────────────────────┐ │  │
│  │  │            uniffi-rs Generated Bindings             │ │  │
│  │  └────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              cosmic-connect-android (Kotlin/Java)               │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    FFI Bridge Layer                      │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │  Wrappers  │  │   Memory   │  │   Error    │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Repository Layer (MVVM)                 │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │   Device   │  │   Plugin   │  │   Config   │        │  │
│  │  │  Repository│  │  Repository│  │  Repository│        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   ViewModel Layer                        │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │   Device   │  │   Plugin   │  │  Settings  │        │  │
│  │  │  ViewModel │  │  ViewModel │  │  ViewModel │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 Android Services Layer                   │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │ Background │  │  Receivers │  │  Providers │        │  │
│  │  │  Services  │  │            │  │            │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               Plugin Implementation Layer                │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │  Battery   │  │   Share    │  │ Clipboard  │        │  │
│  │  ├────────────┤  ├────────────┤  ├────────────┤        │  │
│  │  │Notification│  │RunCommand  │  │FindMyPhone │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                   UI Layer (Compose)                     │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐        │  │
│  │  │ DeviceList │  │   Detail   │  │  Settings  │        │  │
│  │  │   Screen   │  │   Screen   │  │   Screen   │        │  │
│  │  └────────────┘  └────────────┘  └────────────┘        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Breakdown

### 1. cosmic-connect-core (Rust)

#### 1.1 Protocol Layer (`src/protocol/`)

**Purpose**: KDE Connect protocol implementation

**Components**:
```rust
// src/protocol/packet.rs
pub struct NetworkPacket {
    pub packet_type: String,
    pub id: i64,
    pub body: serde_json::Value,
}

impl NetworkPacket {
    pub fn new(packet_type: String, id: i64) -> Self;
    pub fn serialize(&self) -> Result<Vec<u8>>;
    pub fn deserialize(data: &[u8]) -> Result<Self>;
    pub fn with_body(self, body: serde_json::Value) -> Self;
    pub fn validate(&self) -> Result<()>;
}

// Packet types
pub enum PacketType {
    Identity,
    Pair,
    Encrypted,
    Battery,
    Share,
    Clipboard,
    Notification,
    FindMyPhone,
    RunCommand,
}

// src/protocol/identity.rs
pub struct DeviceIdentity {
    pub device_id: String,
    pub device_name: String,
    pub device_type: DeviceType,
    pub protocol_version: u32,
    pub incoming_capabilities: Vec<String>,
    pub outgoing_capabilities: Vec<String>,
    pub tcp_port: u16,
}

// src/protocol/pairing.rs
pub struct PairingHandler {
    state: Arc<Mutex<PairingState>>,
}

impl PairingHandler {
    pub async fn request_pairing(&mut self, device_id: &str) -> Result<()>;
    pub async fn accept_pairing(&mut self, device_id: &str) -> Result<()>;
    pub async fn reject_pairing(&mut self, device_id: &str) -> Result<()>;
}
```

**Responsibilities**:
- Packet serialization/deserialization
- Protocol validation
- Identity management
- Pairing state machine

---

#### 1.2 Network Layer (`src/network/`)

**Purpose**: Network communication

**Components**:
```rust
// src/network/discovery.rs
pub struct DiscoveryService {
    multicast_addr: Ipv4Addr,  // 224.0.0.251
    port: u16,                  // 1716
    socket: Arc<UdpSocket>,
}

impl DiscoveryService {
    pub async fn start(&mut self) -> Result<()>;
    pub async fn broadcast_identity(&self) -> Result<()>;
    pub async fn listen(&self) -> Result<DeviceIdentity>;
    pub async fn stop(&mut self) -> Result<()>;
}

// src/network/connection.rs
pub struct ConnectionManager {
    connections: Arc<Mutex<HashMap<String, Connection>>>,
}

impl ConnectionManager {
    pub async fn connect(&mut self, device_id: &str, addr: SocketAddr) -> Result<()>;
    pub async fn disconnect(&mut self, device_id: &str) -> Result<()>;
    pub async fn send_packet(&self, device_id: &str, packet: NetworkPacket) -> Result<()>;
    pub async fn receive_packet(&self, device_id: &str) -> Result<NetworkPacket>;
}

// src/network/tcp.rs
pub struct TcpTransport {
    stream: TcpStream,
}

impl TcpTransport {
    pub async fn send(&mut self, data: &[u8]) -> Result<()>;
    pub async fn receive(&mut self) -> Result<Vec<u8>>;
}
```

**Responsibilities**:
- UDP multicast discovery
- TCP connection management
- Packet routing
- Network state monitoring

---

#### 1.3 Crypto Layer (`src/crypto/`)

**Purpose**: TLS and certificate management

**Components**:
```rust
// src/crypto/certificate.rs
pub trait CertificateStorage {
    fn save_certificate(&self, device_id: &str, cert: &Certificate) -> Result<()>;
    fn load_certificate(&self, device_id: &str) -> Result<Option<Certificate>>;
    fn delete_certificate(&self, device_id: &str) -> Result<()>;
}

pub struct CertificateManager {
    storage: Box<dyn CertificateStorage>,
}

impl CertificateManager {
    pub fn generate_certificate(&self, device_id: &str) -> Result<Certificate>;
    pub fn get_certificate(&self, device_id: &str) -> Result<Certificate>;
    pub fn validate_certificate(&self, cert: &Certificate) -> Result<()>;
    pub fn get_fingerprint(&self, cert: &Certificate) -> String;
}

// src/crypto/tls.rs
pub struct TlsManager {
    cert_manager: Arc<CertificateManager>,
}

impl TlsManager {
    pub async fn connect_as_client(&self, device_id: &str, addr: SocketAddr) -> Result<TlsStream>;
    pub async fn accept_as_server(&self, device_id: &str, stream: TcpStream) -> Result<TlsStream>;
    
    // CRITICAL: TLS role determined by deviceId comparison
    pub fn determine_role(&self, local_id: &str, remote_id: &str) -> TlsRole;
}

pub enum TlsRole {
    Client,  // if local_id < remote_id
    Server,  // if local_id > remote_id
}
```

**Responsibilities**:
- Certificate generation and management
- TLS connection establishment
- Certificate validation and pinning
- Fingerprint calculation
- **CRITICAL**: TLS role determination

---

#### 1.4 Plugin Layer (`src/plugins/`)

**Purpose**: Plugin system and core implementations

**Components**:
```rust
// src/plugins/mod.rs
pub trait Plugin: Send + Sync {
    fn name(&self) -> &str;
    fn incoming_capabilities(&self) -> Vec<String>;
    fn outgoing_capabilities(&self) -> Vec<String>;
    fn handle_packet(&mut self, packet: NetworkPacket) -> Result<()>;
    fn initialize(&mut self) -> Result<()>;
    fn shutdown(&mut self) -> Result<()>;
}

pub struct PluginManager {
    plugins: HashMap<String, Box<dyn Plugin>>,
}

impl PluginManager {
    pub fn register_plugin(&mut self, plugin: Box<dyn Plugin>) -> Result<()>;
    pub async fn route_packet(&mut self, packet: NetworkPacket) -> Result<()>;
    pub fn get_capabilities(&self) -> (Vec<String>, Vec<String>);
}

// src/plugins/battery.rs
pub struct BatteryPlugin {
    state: Arc<Mutex<BatteryState>>,
}

pub struct BatteryState {
    pub level: i32,
    pub is_charging: bool,
    pub threshold: i32,
}

impl Plugin for BatteryPlugin {
    fn name(&self) -> &str { "battery" }
    fn handle_packet(&mut self, packet: NetworkPacket) -> Result<()> {
        // Core battery logic
    }
}

// Similar structure for other plugins:
// - share.rs
// - clipboard.rs  
// - notification.rs
// - run_command.rs
// - find_my_phone.rs
```

**Responsibilities**:
- Plugin registration and lifecycle
- Packet routing to plugins
- Plugin state management
- Core plugin implementations

---

#### 1.5 FFI Layer (`src/ffi/`)

**Purpose**: Foreign Function Interface for Kotlin/Swift

**Components**:
```rust
// src/ffi/mod.rs
uniffi::include_scaffolding!("cosmic_connect_core");

// Exported types
#[derive(uniffi::Record)]
pub struct FfiNetworkPacket {
    pub packet_type: String,
    pub id: i64,
    pub body: String,  // JSON string
}

#[derive(uniffi::Record)]
pub struct FfiDeviceIdentity {
    pub device_id: String,
    pub device_name: String,
    pub device_type: String,
    pub protocol_version: u32,
    pub tcp_port: u16,
}

// Exported functions
#[uniffi::export]
pub fn create_network_packet(packet_type: String, id: i64, body: String) -> Arc<FfiNetworkPacket>;

#[uniffi::export]
pub fn serialize_packet(packet: Arc<FfiNetworkPacket>) -> Vec<u8>;

#[uniffi::export]
pub fn deserialize_packet(data: Vec<u8>) -> Result<Arc<FfiNetworkPacket>, String>;

// Discovery functions
#[uniffi::export]
pub async fn start_discovery() -> Result<(), String>;

#[uniffi::export]
pub async fn broadcast_identity(identity: Arc<FfiDeviceIdentity>) -> Result<(), String>;

// Certificate functions
#[uniffi::export]
pub fn generate_certificate(device_id: String) -> Result<Vec<u8>, String>;

#[uniffi::export]
pub fn get_certificate_fingerprint(cert: Vec<u8>) -> Result<String, String>;

// Plugin functions
#[uniffi::export]
pub async fn send_battery_update(device_id: String, level: i32, charging: bool) -> Result<(), String>;

#[uniffi::export]
pub async fn send_file(device_id: String, file_path: String) -> Result<(), String>;

// Callback interface for events from Rust to platform
#[uniffi::export(callback_interface)]
pub trait EventCallback {
    fn on_device_discovered(&self, identity: Arc<FfiDeviceIdentity>);
    fn on_device_connected(&self, device_id: String);
    fn on_device_disconnected(&self, device_id: String);
    fn on_packet_received(&self, device_id: String, packet: Arc<FfiNetworkPacket>);
    fn on_pairing_request(&self, device_id: String);
}
```

**Responsibilities**:
- Type conversions for FFI
- Error handling across boundary
- Memory management
- Callback interface for events

---

### 2. cosmic-connect-android (Kotlin)

#### 2.1 FFI Bridge Layer (`src/main/kotlin/org/kde/kdeconnect_tp/core/`)

**Purpose**: Kotlin wrappers around Rust FFI

**Components**:
```kotlin
// CosmicConnectCore.kt
object CosmicConnectCore {
    init {
        System.loadLibrary("cosmic_connect_core")
    }
    
    private val callbacks = mutableListOf<EventCallback>()
    
    fun initialize() {
        initializeNative(object : EventCallback {
            override fun onDeviceDiscovered(identity: FfiDeviceIdentity) {
                callbacks.forEach { it.onDeviceDiscovered(identity) }
            }
            // ... other callbacks
        })
    }
    
    fun registerCallback(callback: EventCallback) {
        callbacks.add(callback)
    }
}

// NetworkPacket.kt
class NetworkPacket internal constructor(
    private val handle: Arc<FfiNetworkPacket>
) {
    val type: String get() = handle.packet_type
    val id: Long get() = handle.id
    val body: String get() = handle.body
    
    fun serialize(): ByteArray = serializePacketNative(handle)
    
    companion object {
        fun create(type: String, id: Long, body: String): NetworkPacket {
            val handle = createNetworkPacketNative(type, id, body)
            return NetworkPacket(handle)
        }
        
        fun deserialize(data: ByteArray): NetworkPacket {
            val handle = deserializePacketNative(data)
            return NetworkPacket(handle)
        }
    }
}

// Discovery.kt
class DiscoveryManager {
    suspend fun start() {
        withContext(Dispatchers.IO) {
            startDiscoveryNative()
        }
    }
    
    suspend fun broadcastIdentity(identity: DeviceIdentity) {
        withContext(Dispatchers.IO) {
            val ffiIdentity = identity.toFfi()
            broadcastIdentityNative(ffiIdentity)
        }
    }
}

// CertificateManager.kt
class CertificateManager(
    private val storage: AndroidCertificateStorage
) {
    fun generateCertificate(deviceId: String): ByteArray {
        return generateCertificateNative(deviceId).also { cert ->
            storage.saveCertificate(deviceId, cert)
        }
    }
    
    fun getCertificateFingerprint(cert: ByteArray): String {
        return getCertificateFingerprintNative(cert)
    }
}

// PluginManager.kt
class PluginManager {
    private val plugins = mutableMapOf<String, AndroidPlugin>()
    
    suspend fun sendBatteryUpdate(deviceId: String, level: Int, charging: Boolean) {
        withContext(Dispatchers.IO) {
            sendBatteryUpdateNative(deviceId, level, charging)
        }
    }
    
    suspend fun sendFile(deviceId: String, uri: Uri) {
        withContext(Dispatchers.IO) {
            val filePath = getFilePathFromUri(uri)
            sendFileNative(deviceId, filePath)
        }
    }
}
```

**Responsibilities**:
- Load native library
- Wrap FFI calls in Kotlin-friendly API
- Convert between Kotlin and FFI types
- Handle errors from Rust
- Manage callbacks

---

#### 2.2 Repository Layer (`src/main/kotlin/org/kde/kdeconnect_tp/data/`)

**Purpose**: Data access abstraction following Repository pattern

**Components**:
```kotlin
// DeviceRepository.kt
interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>
    suspend fun getDevice(deviceId: String): Device?
    suspend fun pairDevice(deviceId: String)
    suspend fun unpairDevice(deviceId: String)
}

class DeviceRepositoryImpl(
    private val discoveryManager: DiscoveryManager,
    private val certificateManager: CertificateManager
) : DeviceRepository {
    
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    
    init {
        CosmicConnectCore.registerCallback(object : EventCallback {
            override fun onDeviceDiscovered(identity: FfiDeviceIdentity) {
                // Update device list
            }
            
            override fun onDeviceConnected(deviceId: String) {
                // Update device state
            }
        })
    }
    
    override fun observeDevices() = _devices.asStateFlow()
    
    override suspend fun getDevice(deviceId: String) = 
        _devices.value.find { it.id == deviceId }
    
    override suspend fun pairDevice(deviceId: String) {
        // Use Rust core for pairing
        requestPairingNative(deviceId)
    }
}

// PluginRepository.kt
interface PluginRepository {
    fun observePluginState(deviceId: String, pluginName: String): Flow<PluginState>
    suspend fun sendPluginCommand(deviceId: String, pluginName: String, command: PluginCommand)
}

class PluginRepositoryImpl(
    private val pluginManager: PluginManager
) : PluginRepository {
    // Implementation using Rust core
}
```

**Responsibilities**:
- Abstract data access
- Expose reactive streams (Flow)
- Handle business logic
- Coordinate between Rust core and Android

---

#### 2.3 ViewModel Layer (`src/main/kotlin/org/kde/kdeconnect_tp/ui/viewmodels/`)

**Purpose**: UI state management following MVVM

**Components**:
```kotlin
// DeviceListViewModel.kt
@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    val devices: StateFlow<List<Device>> = deviceRepository
        .observeDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun pairDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                deviceRepository.pairDevice(deviceId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

// BatteryViewModel.kt
@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val pluginRepository: PluginRepository,
    private val batteryManager: BatteryManager  // Android system
) : ViewModel() {
    
    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel = _batteryLevel.asStateFlow()
    
    init {
        // Monitor Android battery
        viewModelScope.launch {
            batteryManager.observeBatteryLevel().collect { level ->
                _batteryLevel.value = level
                // Send to Rust core
                pluginRepository.sendPluginCommand(
                    deviceId = currentDeviceId,
                    pluginName = "battery",
                    command = BatteryCommand.Update(level, isCharging)
                )
            }
        }
    }
}
```

**Responsibilities**:
- Manage UI state
- Handle user interactions
- Coordinate between Repository and UI
- Lifecycle-aware

---

#### 2.4 Android Services Layer (`src/main/kotlin/org/kde/kdeconnect_tp/services/`)

**Purpose**: Background operations and system integration

**Components**:
```kotlin
// BackgroundService.kt
class BackgroundService : Service() {
    
    private val binder = LocalBinder()
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start discovery
        lifecycleScope.launch {
            discoveryManager.start()
        }
        
        return START_STICKY
    }
    
    inner class LocalBinder : Binder() {
        fun getService() = this@BackgroundService
    }
}

// BatteryBroadcastReceiver.kt
class BatteryBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == 
                    BatteryManager.BATTERY_STATUS_CHARGING
                
                // Send to Rust core via plugin
                CoroutineScope(Dispatchers.IO).launch {
                    sendBatteryUpdateNative(currentDeviceId, level, charging)
                }
            }
        }
    }
}

// NotificationListenerService.kt
class CosmicNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Extract notification data
        val notification = extractNotificationData(sbn)
        
        // Send through Rust core
        lifecycleScope.launch {
            sendNotificationNative(currentDeviceId, notification.toJson())
        }
    }
}
```

**Responsibilities**:
- Background service lifecycle
- System broadcast receivers
- Notification listening
- Foreground service for discovery

---

#### 2.5 Plugin Implementation Layer (`src/main/kotlin/org/kde/kdeconnect_tp/plugins/`)

**Purpose**: Android-specific plugin implementations

**Components**:
```kotlin
// AndroidPlugin.kt (Base class)
abstract class AndroidPlugin(
    val name: String,
    protected val pluginRepository: PluginRepository
) {
    abstract fun provideUI(): @Composable () -> Unit
    abstract suspend fun handleAndroidSpecifics()
    
    // Delegates protocol handling to Rust core
    suspend fun sendPacket(deviceId: String, packet: NetworkPacket) {
        pluginRepository.sendPluginCommand(
            deviceId = deviceId,
            pluginName = name,
            command = PluginCommand.SendPacket(packet)
        )
    }
}

// BatteryPlugin.kt
class BatteryPlugin(
    pluginRepository: PluginRepository,
    private val context: Context
) : AndroidPlugin("battery", pluginRepository) {
    
    @Composable
    override fun provideUI() {
        BatteryPluginScreen()
    }
    
    override suspend fun handleAndroidSpecifics() {
        // Register battery receiver
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(BatteryBroadcastReceiver(), filter)
        
        // Start monitoring
        monitorBatteryLevel()
    }
    
    private suspend fun monitorBatteryLevel() {
        // Read Android battery and send through Rust core
    }
}

// SharePlugin.kt
class SharePlugin(
    pluginRepository: PluginRepository,
    private val context: Context
) : AndroidPlugin("share", pluginRepository) {
    
    suspend fun shareFile(deviceId: String, uri: Uri) {
        // Convert Uri to file path
        val filePath = getFilePathFromUri(uri)
        
        // Use Rust core for actual transfer
        sendFileNative(deviceId, filePath)
    }
    
    @Composable
    override fun provideUI() {
        SharePluginScreen()
    }
}

// Similar implementations for:
// - ClipboardPlugin
// - NotificationPlugin  
// - RunCommandPlugin
// - FindMyPhonePlugin
```

**Responsibilities**:
- Android system integration
- File/content access
- UI components
- Delegate protocol handling to Rust

---

#### 2.6 UI Layer (`src/main/kotlin/org/kde/kdeconnect_tp/ui/`)

**Purpose**: Jetpack Compose UI

**Components**:
```kotlin
// DeviceListScreen.kt
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsState()
    
    LazyColumn {
        items(devices) { device ->
            DeviceCard(
                device = device,
                onPairClick = { viewModel.pairDevice(device.id) }
            )
        }
    }
}

// DeviceDetailScreen.kt
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val device by viewModel.device.collectAsState()
    val plugins by viewModel.plugins.collectAsState()
    
    Column {
        DeviceInfo(device)
        
        PluginList(
            plugins = plugins,
            onPluginClick = { plugin ->
                viewModel.openPlugin(plugin)
            }
        )
    }
}

// Material 3 design system components
@Composable
fun DeviceCard(device: Device, onPairClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { /* Navigate to detail */ }
    ) {
        Row {
            DeviceIcon(device.type)
            DeviceInfo(device)
            if (!device.isPaired) {
                IconButton(onClick = onPairClick) {
                    Icon(Icons.Default.Link, "Pair")
                }
            }
        }
    }
}
```

**Responsibilities**:
- Declarative UI with Compose
- Material 3 design
- Navigation
- State observation

---

## FFI Interface Design

### Memory Management

**Ownership Model**:
```rust
// Rust side: Use Arc for shared ownership
#[uniffi::export]
pub fn create_network_packet(...) -> Arc<FfiNetworkPacket> {
    Arc::new(FfiNetworkPacket { ... })
}

// Kotlin side: uniffi handles refcounting
val packet = createNetworkPacket("identity", 123, "{}")
// packet is automatically freed when no longer referenced
```

**Lifetime Rules**:
1. Rust owns all protocol objects
2. Kotlin holds references (Arc)
3. uniffi manages reference counting
4. No manual memory management needed

---

### Error Handling

**Pattern**:
```rust
// Rust: Return Result
#[uniffi::export]
pub fn do_something() -> Result<String, String> {
    do_something_internal()
        .map_err(|e| format!("Error: {}", e))
}

// Kotlin: Throws exception on error
try {
    val result = doSomething()
} catch (e: Exception) {
    // Handle error
}
```

---

### Async Functions

**Pattern**:
```rust
// Rust: async function
#[uniffi::export]
pub async fn connect_device(device_id: String) -> Result<(), String> {
    connection_manager.connect(&device_id).await
        .map_err(|e| e.to_string())
}

// Kotlin: suspend function
suspend fun connectDevice(deviceId: String) {
    withContext(Dispatchers.IO) {
        connectDeviceNative(deviceId)
    }
}
```

---

### Callbacks

**Pattern**:
```rust
// Rust: callback trait
#[uniffi::export(callback_interface)]
pub trait EventCallback {
    fn on_device_discovered(&self, identity: Arc<FfiDeviceIdentity>);
}

// Implementation in Kotlin
CosmicConnectCore.registerCallback(object : EventCallback {
    override fun onDeviceDiscovered(identity: FfiDeviceIdentity) {
        // Handle in Kotlin
    }
})
```

---

## Data Flow

### Discovery Flow

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  User opens app                                             │
│         │                                                    │
│         ▼                                                    │
│  DeviceListScreen                                           │
│         │                                                    │
│         ▼                                                    │
│  DeviceListViewModel                                        │
│         │                                                    │
│         ▼                                                    │
│  DeviceRepository.observeDevices()                          │
│         │                                                    │
│         ▼                                                    │
│  DiscoveryManager (Kotlin)                                  │
│         │                                                    │
│         ▼                                                    │
│  FFI: startDiscoveryNative()                                │
│         │                                                    │
│  ═══════╪═══════════════ FFI Boundary ═══════════════       │
│         │                                                    │
│         ▼                                                    │
│  DiscoveryService.start() (Rust)                            │
│         │                                                    │
│         ├─► Send UDP multicast identity                     │
│         │                                                    │
│         ├─► Listen for UDP responses                        │
│         │                                                    │
│         │   [Device responds]                               │
│         │                                                    │
│         ▼                                                    │
│  Parse NetworkPacket::Identity                              │
│         │                                                    │
│         ▼                                                    │
│  Callback: EventCallback.on_device_discovered()             │
│         │                                                    │
│  ═══════╪═══════════════ FFI Boundary ═══════════════       │
│         │                                                    │
│         ▼                                                    │
│  DeviceRepository updates _devices StateFlow                │
│         │                                                    │
│         ▼                                                    │
│  DeviceListViewModel receives update                        │
│         │                                                    │
│         ▼                                                    │
│  DeviceListScreen recomposes with new device                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### Pairing Flow

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  User clicks "Pair" button                                  │
│         │                                                    │
│         ▼                                                    │
│  DeviceViewModel.pairDevice(deviceId)                       │
│         │                                                    │
│         ▼                                                    │
│  DeviceRepository.pairDevice(deviceId)                      │
│         │                                                    │
│         ▼                                                    │
│  FFI: requestPairingNative(deviceId)                        │
│         │                                                    │
│  ═══════╪═══════════════ FFI Boundary ═══════════════       │
│         │                                                    │
│         ▼                                                    │
│  PairingHandler.request_pairing(device_id) (Rust)           │
│         │                                                    │
│         ├─► Generate pairing request packet                 │
│         │                                                    │
│         ├─► Establish TLS connection                        │
│         │   │                                                │
│         │   ├─► Determine TLS role (deviceId comparison)    │
│         │   │                                                │
│         │   └─► Connect as client OR accept as server       │
│         │                                                    │
│         ├─► Send pair request packet                        │
│         │                                                    │
│         │   [Remote device accepts]                         │
│         │                                                    │
│         ├─► Receive pair response                           │
│         │                                                    │
│         ├─► Exchange certificates                           │
│         │                                                    │
│         ├─► Validate remote certificate                     │
│         │                                                    │
│         ├─► Save trusted certificate                        │
│         │                                                    │
│         ▼                                                    │
│  Callback: EventCallback.on_device_paired()                 │
│         │                                                    │
│  ═══════╪═══════════════ FFI Boundary ═══════════════       │
│         │                                                    │
│         ▼                                                    │
│  CertificateManager.saveCertificate() (Kotlin)              │
│         │                                                    │
│         ├─► Store in Android Keystore                       │
│         │                                                    │
│         ▼                                                    │
│  DeviceRepository updates device state                      │
│         │                                                    │
│         ▼                                                    │
│  UI shows "Paired" status                                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### Plugin Communication Flow (Battery Example)

```
Android → COSMIC Desktop:

┌─────────────────────────────────────────────────────────────┐
│  Android battery level changes                              │
│         │                                                    │
│         ▼                                                    │
│  BatteryBroadcastReceiver.onReceive()                       │
│         │                                                    │
│         ▼                                                    │
│  BatteryPlugin.updateBatteryLevel(level, charging)          │
│         │                                                    │
│         ▼                                                    │
│  FFI: sendBatteryUpdateNative(deviceId, level, charging)    │
│         │                                                    │
│  ═══════╪═══════════════ FFI Boundary ═══════════════       │
│         │                                                    │
│         ▼                                                    │
│  BatteryPlugin.handle_packet() (Rust)                       │
│         │                                                    │
│         ├─► Create NetworkPacket::Battery                   │
│         │                                                    │
│         ├─► Serialize packet                                │
│         │                                                    │
│         ├─► Get TLS connection for deviceId                 │
│         │                                                    │
│         ├─► Send over TLS                                   │
│         │                                                    │
│         │   [Received by COSMIC Desktop]                    │
│         │                                                    │
│         │   COSMIC Desktop's Rust core processes packet     │
│         │                                                    │
│         └─► COSMIC applet updates UI                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘

COSMIC Desktop → Android (similar flow, reversed)
```

---

## Security Architecture

### Certificate Management

**Generation** (Rust):
```rust
pub fn generate_certificate(device_id: &str) -> Result<Certificate> {
    let mut params = CertificateParams::new(vec![device_id.to_string()]);
    params.distinguished_name = DistinguishedName::new();
    params.distinguished_name.push(
        DnType::CommonName,
        device_id
    );
    params.distinguished_name.push(
        DnType::OrganizationName,
        "KDE"
    );
    params.distinguished_name.push(
        DnType::OrganizationalUnitName,
        "KDE Connect"
    );
    
    params.alg = &rcgen::PKCS_RSA_SHA256;
    params.key_pair = Some(KeyPair::generate(&rcgen::PKCS_RSA_SHA256)?);
    params.not_before = OffsetDateTime::now_utc();
    params.not_after = params.not_before + Duration::days(3650); // 10 years
    
    Certificate::from_params(params)
}
```

**Storage** (Kotlin with Android Keystore):
```kotlin
class AndroidCertificateStorage : CertificateStorage {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    override fun saveCertificate(deviceId: String, cert: ByteArray) {
        val alias = "kdeconnect_$deviceId"
        
        val certFactory = CertificateFactory.getInstance("X.509")
        val certificate = certFactory.generateCertificate(ByteArrayInputStream(cert))
        
        keyStore.setCertificateEntry(alias, certificate)
    }
    
    override fun loadCertificate(deviceId: String): ByteArray? {
        val alias = "kdeconnect_$deviceId"
        return keyStore.getCertificate(alias)?.encoded
    }
}
```

---

### TLS Connection

**Role Determination** (CRITICAL):
```rust
pub fn determine_tls_role(local_id: &str, remote_id: &str) -> TlsRole {
    // String comparison determines TLS role
    if local_id < remote_id {
        TlsRole::Client
    } else {
        TlsRole::Server
    }
}
```

**Connection Establishment**:
```rust
pub async fn establish_tls_connection(
    local_id: &str,
    remote_id: &str,
    tcp_stream: TcpStream,
) -> Result<TlsStream> {
    let role = determine_tls_role(local_id, remote_id);
    
    match role {
        TlsRole::Client => {
            let config = create_client_config(local_id)?;
            let connector = TlsConnector::from(Arc::new(config));
            connector.connect(remote_id, tcp_stream).await
        }
        TlsRole::Server => {
            let config = create_server_config(local_id)?;
            let acceptor = TlsAcceptor::from(Arc::new(config));
            acceptor.accept(tcp_stream).await
        }
    }
}
```

---

## Threading and Concurrency

### Rust Core

**Async Runtime**:
```rust
// Use tokio for async operations
#[tokio::main]
async fn main() {
    let discovery = DiscoveryService::new();
    discovery.start().await.unwrap();
}
```

**Thread Safety**:
```rust
// Use Arc<Mutex<T>> for shared mutable state
pub struct ConnectionManager {
    connections: Arc<Mutex<HashMap<String, Connection>>>,
}

// Use channels for communication
let (tx, rx) = mpsc::channel(100);
```

---

### Android/Kotlin

**Coroutines**:
```kotlin
// Use coroutines for async operations
viewModelScope.launch {
    val result = withContext(Dispatchers.IO) {
        deviceRepository.pairDevice(deviceId)
    }
}
```

**Flow for reactive streams**:
```kotlin
val devices: StateFlow<List<Device>> = deviceRepository
    .observeDevices()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

---

## Build System Integration

### Rust Build

**Cargo.toml for Android**:
```toml
[package]
name = "cosmic-connect-core"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["lib", "staticlib", "cdylib"]

[dependencies]
tokio = { version = "1.0", features = ["full"] }
rustls = "0.21"
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
uniffi = "0.25"

[build-dependencies]
uniffi = { version = "0.25", features = ["build"] }

[[bin]]
name = "uniffi-bindgen"
path = "uniffi-bindgen.rs"
```

**build.rs**:
```rust
fn main() {
    uniffi::generate_scaffolding("src/cosmic_connect_core.udl").unwrap();
}
```

---

### Android Build

**build.gradle.kts**:
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.3"
}

cargo {
    module = "../cosmic-connect-core"
    libname = "cosmic_connect_core"
    targets = listOf("arm64", "arm", "x86_64")
    profile = "release"
}

android {
    // ...
    
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("$buildDir/rustJniLibs/android")
        }
    }
}

dependencies {
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    // ... other dependencies
}

tasks.whenTaskAdded {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        dependsOn("cargoBuild")
    }
}
```

---

### CI/CD

**GitHub Actions** (`.github/workflows/build.yml`):
```yaml
name: Build

on: [push, pull_request]

jobs:
  build-rust:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions-rs/toolchain@v1
        with:
          toolchain: stable
      - name: Build Rust core
        run: |
          cd cosmic-connect-core
          cargo build --release
          cargo test

  build-android:
    needs: build-rust
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions-rs/toolchain@v1
      - name: Install Android targets
        run: |
          rustup target add aarch64-linux-android
          rustup target add armv7-linux-androideabi
          rustup target add x86_64-linux-android
      - name: Build Android
        run: |
          cd cosmic-connect-android
          ./gradlew assembleDebug
```

---

## Testing Strategy

### Rust Core Tests

**Unit Tests**:
```rust
#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_network_packet_serialization() {
        let packet = NetworkPacket::new("identity".to_string(), 123);
        let serialized = packet.serialize().unwrap();
        let deserialized = NetworkPacket::deserialize(&serialized).unwrap();
        
        assert_eq!(packet.packet_type, deserialized.packet_type);
        assert_eq!(packet.id, deserialized.id);
    }

    #[tokio::test]
    async fn test_discovery() {
        let discovery = DiscoveryService::new();
        discovery.start().await.unwrap();
        
        // Test discovery
    }
}
```

**Integration Tests** (`tests/`):
```rust
#[tokio::test]
async fn test_full_pairing_flow() {
    // Set up two instances
    let alice = setup_device("alice");
    let bob = setup_device("bob");
    
    // Discover each other
    let bob_identity = alice.discover().await.unwrap();
    
    // Pair
    alice.request_pairing(&bob_identity.device_id).await.unwrap();
    bob.accept_pairing(&alice.device_id).await.unwrap();
    
    // Verify paired
    assert!(alice.is_paired(&bob_identity.device_id));
    assert!(bob.is_paired(&alice.device_id));
}
```

---

### Android Tests

**Unit Tests**:
```kotlin
@Test
fun testNetworkPacketWrapper() {
    val packet = NetworkPacket.create("identity", 123, "{}")
    
    val serialized = packet.serialize()
    val deserialized = NetworkPacket.deserialize(serialized)
    
    assertEquals(packet.type, deserialized.type)
    assertEquals(packet.id, deserialized.id)
}
```

**Integration Tests**:
```kotlin
@Test
fun testDiscoveryFlow() = runTest {
    val repository = DeviceRepository()
    
    repository.startDiscovery()
    
    val devices = repository.observeDevices().first()
    
    assertTrue(devices.isNotEmpty())
}
```

**UI Tests**:
```kotlin
@Test
fun testDeviceListScreen() {
    composeTestRule.setContent {
        DeviceListScreen()
    }
    
    composeTestRule.onNodeWithText("Devices").assertExists()
}
```

---

### FFI Tests

**Memory Leak Tests**:
```kotlin
@Test
fun testNoMemoryLeaks() {
    repeat(1000) {
        val packet = NetworkPacket.create("test", it.toLong(), "{}")
        packet.serialize()
        // Packet should be freed automatically
    }
    
    // Force GC
    System.gc()
    
    // Check memory usage hasn't grown significantly
}
```

---

## Performance Considerations

### Protocol Operations

**Benchmarks** (Rust):
```rust
#[bench]
fn bench_packet_serialization(b: &mut Bencher) {
    let packet = NetworkPacket::new("identity".to_string(), 123);
    b.iter(|| packet.serialize());
}
```

**Expected Performance**:
- Packet serialization: < 1ms
- Discovery response: < 100ms
- TLS handshake: < 500ms
- File transfer: > 10 MB/s on WiFi

---

### FFI Overhead

**Minimize FFI Calls**:
```rust
// BAD: Multiple FFI calls
for item in items {
    send_item_native(item)  // FFI call per item
}

// GOOD: Batch FFI call
send_items_native(items)  // Single FFI call
```

**Batch Updates**:
```kotlin
// BAD: Update UI for each device
devices.forEach { device ->
    updateDevice(device)  // Recompose for each
}

// GOOD: Update list once
updateDevices(devices)  // Single recomposition
```

---

## Migration Path

### Phase 0: Extract Rust Core
1. Clone COSMIC applet
2. Identify protocol code
3. Extract to new library
4. Test with desktop applet
5. Publish cosmic-connect-core v0.1.0

### Phase 1: Android Integration
1. Add cargo-ndk to Android build
2. Create FFI bindings
3. Wrap in Kotlin
4. Basic smoke tests

### Phase 2: Replace Core Components
1. Replace NetworkPacket → use Rust
2. Replace Discovery → use Rust
3. Replace TLS/Certificates → use Rust
4. Replace Device/DeviceManager → use Rust + Kotlin wrapper

### Phase 3: Plugin Migration
1. Update each plugin to use Rust core
2. Keep Android-specific code in Kotlin
3. Test each plugin thoroughly

### Phase 4: UI Modernization
1. Convert to Compose (can be parallel)
2. Material 3 design
3. MVVM architecture

### Phase 5: Testing & Release
1. Comprehensive testing
2. Performance tuning
3. Beta testing
4. Release

---

## Conclusion

This architecture provides:

✅ **Code Sharing**: 70%+ protocol logic shared  
✅ **Memory Safety**: Rust for critical code  
✅ **Modern Android**: Kotlin + Compose  
✅ **Clear Boundaries**: FFI separates concerns  
✅ **Easy Testing**: Each layer testable independently  
✅ **Performance**: Native speed for protocol operations  

The hybrid approach leverages the strengths of both languages:
- **Rust** for protocol, networking, and crypto
- **Kotlin** for UI and Android services

This results in a maintainable, performant, and safe implementation that serves both COSMIC Desktop and Android from a single protocol codebase.
