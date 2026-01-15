# Android Development Skill for COSMIC Connect

## Overview
This skill provides comprehensive guidance for modern Android development with a focus on COSMIC Connect protocol implementation, network programming, and integration with desktop applications.

## Core Android Development Principles

### Modern Android Architecture
- **MVVM Pattern**: Use ViewModel + LiveData/StateFlow for reactive UI
- **Repository Pattern**: Centralize data access logic
- **Use Cases/Interactors**: Encapsulate business logic
- **Dependency Injection**: Use Hilt or Koin for dependency management
- **Coroutines**: Replace callbacks with structured concurrency

### Kotlin Best Practices
```kotlin
// Use data classes for DTOs
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val isConnected: Boolean = false
)

// Use sealed classes for state management
sealed class NetworkState {
    object Idle : NetworkState()
    data class Connecting(val deviceId: String) : NetworkState()
    data class Connected(val device: DeviceInfo) : NetworkState()
    data class Error(val message: String) : NetworkState()
}

// Use extension functions for cleaner code
fun Socket.sendPacket(packet: NetworkPacket) {
    this.getOutputStream().apply {
        write(packet.toByteArray())
        flush()
    }
}

// Use inline functions for resource management
inline fun <T> Socket.use(block: (Socket) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
```

### Android Components

#### Services
```kotlin
// Foreground service for persistent connections
class CosmicConnectService : Service() {
    private lateinit var notificationManager: NotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "COSMIC Connect Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains device connections"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
```

#### Broadcast Receivers
```kotlin
// Network state monitoring
class NetworkStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                    as ConnectivityManager
                val activeNetwork = cm.activeNetworkInfo
                val isConnected = activeNetwork?.isConnected == true
                
                // Notify service about network changes
                Intent(context, CosmicConnectService::class.java).apply {
                    action = "NETWORK_STATE_CHANGED"
                    putExtra("isConnected", isConnected)
                    context.startService(this)
                }
            }
        }
    }
}
```

## COSMIC Connect Protocol Implementation

### Device Discovery
```kotlin
class DeviceDiscoveryManager(private val context: Context) {
    private val multicastSocket: MulticastSocket by lazy {
        MulticastSocket(DISCOVERY_PORT).apply {
            reuseAddress = true
            networkInterface = getActiveNetworkInterface()
        }
    }
    
    suspend fun startDiscovery() = withContext(Dispatchers.IO) {
        val group = InetAddress.getByName(MULTICAST_GROUP)
        multicastSocket.joinGroup(group)
        
        while (isActive) {
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)
            
            try {
                multicastSocket.receive(packet)
                val data = String(packet.data, 0, packet.length)
                parseIdentityPacket(data)?.let { deviceInfo ->
                    emit(DiscoveryEvent.DeviceFound(deviceInfo))
                }
            } catch (e: IOException) {
                emit(DiscoveryEvent.Error(e.message ?: "Discovery error"))
            }
        }
    }
    
    fun broadcastIdentity() {
        val identityPacket = createIdentityPacket()
        val data = identityPacket.toString().toByteArray()
        val packet = DatagramPacket(
            data, 
            data.size, 
            InetAddress.getByName(MULTICAST_GROUP), 
            DISCOVERY_PORT
        )
        multicastSocket.send(packet)
    }
    
    private fun createIdentityPacket(): JSONObject {
        return JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("type", "cosmicconnect.identity")
            put("body", JSONObject().apply {
                put("deviceId", getDeviceId())
                put("deviceName", Build.MODEL)
                put("deviceType", "phone")
                put("protocolVersion", 7)
                put("incomingCapabilities", getSupportedCapabilities())
                put("outgoingCapabilities", getSupportedCapabilities())
                put("tcpPort", TCP_PORT)
            })
        }
    }
}
```

### TLS Certificate Management
```kotlin
class CertificateManager(private val context: Context) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    }
    
    fun getOrCreateCertificate(): X509Certificate {
        return keyStore.getCertificate(ALIAS) as? X509Certificate 
            ?: generateSelfSignedCertificate()
    }
    
    private fun generateSelfSignedCertificate(): X509Certificate {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA, 
            "AndroidKeyStore"
        )
        
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).apply {
            setCertificateSubject(X500Principal("CN=${getDeviceId()}"))
            setDigests(KeyProperties.DIGEST_SHA256)
            setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            setKeySize(2048)
            setKeyValidityStart(Date())
            setKeyValidityEnd(Date(System.currentTimeMillis() + 
                TimeUnit.DAYS.toMillis(365 * 10)))
        }.build()
        
        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()
        
        return keyStore.getCertificate(ALIAS) as X509Certificate
    }
    
    fun createSSLContext(peerCertificate: X509Certificate?): SSLContext {
        val sslContext = SSLContext.getInstance("TLSv1.2")
        
        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        )
        keyManagerFactory.init(keyStore, null)
        
        val trustManagerFactory = if (peerCertificate != null) {
            createTrustManager(peerCertificate)
        } else {
            null
        }
        
        sslContext.init(
            keyManagerFactory.keyManagers,
            trustManagerFactory?.trustManagers,
            SecureRandom()
        )
        
        return sslContext
    }
}
```

### Network Packet Handling
```kotlin
data class NetworkPacket(
    val id: Long,
    val type: String,
    val body: JSONObject,
    val payloadSize: Long = 0,
    val payloadTransferInfo: PayloadTransferInfo? = null
) {
    fun toByteArray(): ByteArray {
        val json = JSONObject().apply {
            put("id", id)
            put("type", type)
            put("body", body)
            if (payloadSize > 0) {
                put("payloadSize", payloadSize)
                payloadTransferInfo?.let { info ->
                    put("payloadTransferInfo", JSONObject().apply {
                        put("port", info.port)
                    })
                }
            }
        }
        return "${json}\n".toByteArray(Charsets.UTF_8)
    }
    
    companion object {
        fun fromInputStream(input: InputStream): NetworkPacket? {
            return try {
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
                val line = reader.readLine() ?: return null
                val json = JSONObject(line)
                
                NetworkPacket(
                    id = json.getLong("id"),
                    type = json.getString("type"),
                    body = json.getJSONObject("body"),
                    payloadSize = json.optLong("payloadSize", 0),
                    payloadTransferInfo = if (json.has("payloadTransferInfo")) {
                        val info = json.getJSONObject("payloadTransferInfo")
                        PayloadTransferInfo(info.getInt("port"))
                    } else null
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class PayloadTransferInfo(val port: Int)
```

### Plugin Architecture
```kotlin
abstract class Plugin(
    protected val context: Context,
    protected val deviceId: String
) {
    abstract val supportedPacketTypes: Set<String>
    abstract val displayName: String
    
    abstract suspend fun handlePacket(packet: NetworkPacket): Boolean
    abstract suspend fun onDeviceConnected()
    abstract suspend fun onDeviceDisconnected()
    
    protected suspend fun sendPacket(packet: NetworkPacket) {
        // Implementation to send packet through connection manager
    }
}

class BatteryPlugin(
    context: Context,
    deviceId: String
) : Plugin(context, deviceId) {
    override val supportedPacketTypes = setOf(
        "cosmicconnect.battery",
        "cosmicconnect.battery.request"
    )
    override val displayName = "Battery"
    
    private val batteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    override suspend fun handlePacket(packet: NetworkPacket): Boolean {
        return when (packet.type) {
            "cosmicconnect.battery.request" -> {
                sendBatteryStatus()
                true
            }
            "cosmicconnect.battery" -> {
                handleBatteryStatus(packet)
                true
            }
            else -> false
        }
    }
    
    private suspend fun sendBatteryStatus() {
        val batteryIntent = context.registerReceiver(
            null, 
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) {
            (level / scale.toFloat() * 100).toInt()
        } else {
            -1
        }
        
        val status = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_STATUS, 
            -1
        ) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = "cosmicconnect.battery",
            body = JSONObject().apply {
                put("currentCharge", batteryPct)
                put("isCharging", isCharging)
                put("thresholdEvent", 0)
            }
        )
        
        sendPacket(packet)
    }
    
    override suspend fun onDeviceConnected() {
        // Start monitoring battery changes
    }
    
    override suspend fun onDeviceDisconnected() {
        // Stop monitoring battery changes
    }
}
```

## Android-Specific Best Practices

### Permissions Management
```kotlin
class PermissionManager(private val activity: Activity) {
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.NEARBY_WIFI_DEVICES
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) 
                != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
}
```

### Background Work
```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Perform background sync
            performSync()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private suspend fun performSync() {
        // Implementation
    }
    
    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "device_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
```

### UI Components with Jetpack Compose
```kotlin
@Composable
fun DeviceListScreen(
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connected Devices") },
                actions = {
                    IconButton(onClick = { viewModel.refreshDevices() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(devices) { device ->
                DeviceItem(
                    device = device,
                    onConnect = { viewModel.connectToDevice(device.id) },
                    onDisconnect = { viewModel.disconnectFromDevice(device.id) }
                )
            }
        }
        
        if (networkState is NetworkState.Connecting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        }
    }
}

@Composable
fun DeviceItem(
    device: DeviceInfo,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = device.iconRes),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = device.deviceType,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (device.isConnected) {
                Button(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            } else {
                Button(onClick = onConnect) {
                    Text("Connect")
                }
            }
        }
    }
}
```

## Testing

### Unit Tests
```kotlin
@RunWith(JUnit4::class)
class NetworkPacketTest {
    @Test
    fun `packet serialization and deserialization`() {
        val originalPacket = NetworkPacket(
            id = 12345,
            type = "cosmicconnect.ping",
            body = JSONObject().apply {
                put("message", "test")
            }
        )
        
        val bytes = originalPacket.toByteArray()
        val deserializedPacket = NetworkPacket.fromInputStream(
            ByteArrayInputStream(bytes)
        )
        
        assertNotNull(deserializedPacket)
        assertEquals(originalPacket.id, deserializedPacket?.id)
        assertEquals(originalPacket.type, deserializedPacket?.type)
        assertEquals(
            originalPacket.body.getString("message"),
            deserializedPacket?.body?.getString("message")
        )
    }
}
```

### Integration Tests
```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeviceConnectionTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun testDeviceDiscoveryAndConnection() {
        // Wait for discovery
        onView(withId(R.id.device_list))
            .perform(waitForMatch(hasDescendant(withText("Test Device")), 5000))
        
        // Click on device
        onView(withText("Test Device"))
            .perform(click())
        
        // Verify connection
        onView(withId(R.id.connection_status))
            .check(matches(withText("Connected")))
    }
}
```

## Integration with COSMIC Desktop Applet

### Protocol Compatibility
- Ensure protocol version 7/8 compatibility
- Use same packet formats as cosmicconnect-protocol crate
- Support all packet types defined in the Rust implementation
- Maintain TLS certificate format compatibility

### Testing Against COSMIC Applet
```bash
# Test device discovery
adb shell am broadcast -a org.cosmic.cosmicconnect.ACTION_DISCOVERY

# Test file transfer
adb shell am start -a android.intent.action.SEND \
    -t "text/plain" \
    --es android.intent.extra.TEXT "Test message" \
    org.cosmic.cosmicconnect/.MainActivity

# Monitor logs
adb logcat -s CosmicConnect:D
```

## Common Pitfalls to Avoid

1. **Network on Main Thread**: Always use coroutines or async tasks for network operations
2. **Context Leaks**: Use Application context for long-lived objects
3. **Background Restrictions**: Handle Doze mode and App Standby
4. **Permission Runtime Changes**: Check permissions before each use
5. **TLS Handshake Failures**: Implement proper certificate validation
6. **Battery Optimization**: Request battery optimization exclusion
7. **Multicast Locks**: Acquire WifiManager.MulticastLock for discovery

## Performance Optimization

### Connection Pooling
```kotlin
class ConnectionPool {
    private val connections = ConcurrentHashMap<String, SSLSocket>()
    private val executor = Executors.newCachedThreadPool()
    
    fun getConnection(deviceId: String): SSLSocket? {
        return connections[deviceId]?.takeIf { !it.isClosed }
    }
    
    fun addConnection(deviceId: String, socket: SSLSocket) {
        connections[deviceId] = socket
        executor.execute {
            monitorConnection(deviceId, socket)
        }
    }
    
    private fun monitorConnection(deviceId: String, socket: SSLSocket) {
        try {
            while (!socket.isClosed && socket.isConnected) {
                Thread.sleep(1000)
            }
        } finally {
            connections.remove(deviceId)
        }
    }
}
```

### Memory Management
```kotlin
// Use weak references for callbacks
class DeviceManager {
    private val listeners = mutableListOf<WeakReference<DeviceListener>>()
    
    fun addListener(listener: DeviceListener) {
        listeners.add(WeakReference(listener))
    }
    
    fun notifyListeners(event: DeviceEvent) {
        listeners.removeAll { it.get() == null }
        listeners.forEach { it.get()?.onDeviceEvent(event) }
    }
}
```

## Version Compatibility Matrix

| Android Version | Min SDK | Target SDK | Features |
|----------------|---------|------------|----------|
| Android 14 (API 34) | 24 | 34 | All features |
| Android 13 (API 33) | 24 | 33 | Runtime permissions |
| Android 12 (API 31) | 24 | 31 | Bluetooth LE |
| Android 11 (API 30) | 24 | 30 | Scoped storage |
| Android 10 (API 29) | 24 | 29 | Background restrictions |

## Resources

- [Android Developers Guide](https://developer.android.com/guide)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [COSMIC Connect Protocol Spec](https://invent.kde.org/network/cosmicconnect-kde)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
