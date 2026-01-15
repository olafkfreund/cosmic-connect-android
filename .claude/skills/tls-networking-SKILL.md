# TLS and Networking Skill for KDE Connect

## Overview
This skill provides comprehensive guidance for implementing secure TLS connections, certificate management, and network programming for the KDE Connect protocol on both Android and Rust/COSMIC platforms.

## KDE Connect Protocol Overview

### Protocol Specifications
- **Protocol Version**: 7/8
- **Transport**: TCP with TLS 1.2+
- **Discovery**: UDP multicast (224.0.0.251:1716)
- **Port Range**: 1714-1764 (TCP and UDP)
- **Packet Format**: JSON with newline delimiter

### Protocol Flow
```
1. Discovery Phase (UDP Multicast)
   - Device broadcasts identity packet
   - Other devices respond with their identity
   
2. Pairing Phase (TCP + TLS)
   - Establish TCP connection
   - Perform TLS handshake
   - Exchange pair request/response packets
   - Verify certificate fingerprints
   
3. Communication Phase (TCP + TLS)
   - Send/receive plugin packets
   - Handle payload transfers
   - Maintain connection state
```

## TLS Certificate Management

### Android Implementation

#### Self-Signed Certificate Generation
```kotlin
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.*
import javax.security.auth.x500.X500Principal

class CertificateManager(private val context: Context) {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    companion object {
        private const val KEY_ALIAS = "kdeconnect_certificate"
        private const val KEY_SIZE = 2048
        private const val VALIDITY_YEARS = 10
    }
    
    fun getOrCreateDeviceCertificate(): X509Certificate {
        return getCertificate() ?: generateCertificate()
    }
    
    private fun getCertificate(): X509Certificate? {
        return try {
            keyStore.getCertificate(KEY_ALIAS) as? X509Certificate
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateCertificate(): X509Certificate {
        val deviceId = getDeviceId()
        val startDate = Date()
        val endDate = Calendar.getInstance().apply {
            time = startDate
            add(Calendar.YEAR, VALIDITY_YEARS)
        }.time
        
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).apply {
            setCertificateSubject(X500Principal("CN=$deviceId, O=KDE"))
            setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            setKeySize(KEY_SIZE)
            setKeyValidityStart(startDate)
            setKeyValidityEnd(endDate)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setIsStrongBoxBacked(false)
            }
        }.build()
        
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            "AndroidKeyStore"
        )
        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()
        
        return keyStore.getCertificate(KEY_ALIAS) as X509Certificate
    }
    
    fun getCertificateFingerprint(): String {
        val certificate = getOrCreateDeviceCertificate()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificate.encoded)
        return hash.joinToString(":") { "%02X".format(it) }
    }
    
    private fun getDeviceId(): String {
        val prefs = context.getSharedPreferences("kdeconnect", Context.MODE_PRIVATE)
        return prefs.getString("device_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }
}
```

#### TLS Context Creation
```kotlin
import javax.net.ssl.*
import java.security.SecureRandom

class TLSContextBuilder(
    private val certificateManager: CertificateManager
) {
    fun createSSLContext(
        peerCertificate: X509Certificate? = null
    ): SSLContext {
        val sslContext = SSLContext.getInstance("TLSv1.2")
        
        // Key manager for our certificate
        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        )
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        keyManagerFactory.init(keyStore, null)
        
        // Trust manager
        val trustManager = if (peerCertificate != null) {
            createPeerTrustManager(peerCertificate)
        } else {
            createAcceptAllTrustManager()
        }
        
        sslContext.init(
            keyManagerFactory.keyManagers,
            arrayOf(trustManager),
            SecureRandom()
        )
        
        return sslContext
    }
    
    private fun createPeerTrustManager(peerCertificate: X509Certificate): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
                // Accept only the peer's certificate
                if (chain.isNullOrEmpty()) {
                    throw CertificateException("Certificate chain is empty")
                }
                
                if (chain[0] != peerCertificate) {
                    throw CertificateException("Certificate does not match expected peer")
                }
            }
            
            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
                checkClientTrusted(chain, authType)
            }
            
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf(peerCertificate)
            }
        }
    }
    
    private fun createAcceptAllTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
                // Accept all for initial pairing
            }
            
            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
                // Accept all for initial pairing
            }
            
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }
    
    fun createSSLSocket(
        host: String,
        port: Int,
        peerCertificate: X509Certificate? = null
    ): SSLSocket {
        val sslContext = createSSLContext(peerCertificate)
        val socket = sslContext.socketFactory.createSocket(host, port) as SSLSocket
        
        // Configure SSL parameters
        socket.apply {
            enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
            enabledCipherSuites = supportedCipherSuites
            useClientMode = true
            
            // Add handshake listener
            addHandshakeCompletedListener { event ->
                val peerCert = event.peerCertificates[0] as X509Certificate
                Log.d("TLS", "Handshake completed with: ${peerCert.subjectDN}")
            }
        }
        
        return socket
    }
    
    fun createSSLServerSocket(port: Int): SSLServerSocket {
        val sslContext = createSSLContext()
        val serverSocket = sslContext.serverSocketFactory
            .createServerSocket(port) as SSLServerSocket
        
        serverSocket.apply {
            enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
            enabledCipherSuites = supportedCipherSuites
            needClientAuth = false
            wantClientAuth = true
        }
        
        return serverSocket
    }
}
```

### Rust/COSMIC Implementation

#### Certificate Generation
```rust
use rcgen::{Certificate, CertificateParams, DistinguishedName, KeyPair};
use time::{Duration, OffsetDateTime};
use std::fs;
use std::path::PathBuf;

pub struct CertificateManager {
    cert_path: PathBuf,
    key_path: PathBuf,
}

impl CertificateManager {
    pub fn new(config_dir: PathBuf) -> Self {
        Self {
            cert_path: config_dir.join("certificate.pem"),
            key_path: config_dir.join("private_key.pem"),
        }
    }
    
    pub fn get_or_create_certificate(&self) -> Result<(Certificate, String), Box<dyn std::error::Error>> {
        if self.cert_path.exists() && self.key_path.exists() {
            self.load_certificate()
        } else {
            self.generate_certificate()
        }
    }
    
    fn generate_certificate(&self) -> Result<(Certificate, String), Box<dyn std::error::Error>> {
        let device_id = self.get_device_id()?;
        
        let mut params = CertificateParams::default();
        
        // Set distinguished name
        let mut dn = DistinguishedName::new();
        dn.push(rcgen::DnType::CommonName, &device_id);
        dn.push(rcgen::DnType::OrganizationName, "KDE");
        params.distinguished_name = dn;
        
        // Set validity period (10 years)
        params.not_before = OffsetDateTime::now_utc();
        params.not_after = OffsetDateTime::now_utc() + Duration::days(3650);
        
        // Generate key pair
        let key_pair = KeyPair::generate(&rcgen::PKCS_RSA_SHA256)?;
        params.key_pair = Some(key_pair);
        
        // Generate certificate
        let cert = Certificate::from_params(params)?;
        
        // Save to disk
        fs::write(&self.cert_path, cert.serialize_pem()?)?;
        fs::write(&self.key_path, cert.serialize_private_key_pem())?;
        
        // Calculate fingerprint
        let fingerprint = self.calculate_fingerprint(&cert)?;
        
        Ok((cert, fingerprint))
    }
    
    fn load_certificate(&self) -> Result<(Certificate, String), Box<dyn std::error::Error>> {
        let cert_pem = fs::read_to_string(&self.cert_path)?;
        let key_pem = fs::read_to_string(&self.key_path)?;
        
        let key_pair = KeyPair::from_pem(&key_pem)?;
        let params = CertificateParams::from_ca_cert_pem(&cert_pem, key_pair)?;
        let cert = Certificate::from_params(params)?;
        
        let fingerprint = self.calculate_fingerprint(&cert)?;
        
        Ok((cert, fingerprint))
    }
    
    fn calculate_fingerprint(&self, cert: &Certificate) -> Result<String, Box<dyn std::error::Error>> {
        use sha2::{Sha256, Digest};
        
        let cert_der = cert.serialize_der()?;
        let mut hasher = Sha256::new();
        hasher.update(&cert_der);
        let hash = hasher.finalize();
        
        Ok(hash.iter()
            .map(|b| format!("{:02X}", b))
            .collect::<Vec<_>>()
            .join(":"))
    }
    
    fn get_device_id(&self) -> Result<String, Box<dyn std::error::Error>> {
        let id_path = self.cert_path.parent().unwrap().join("device_id");
        
        if id_path.exists() {
            Ok(fs::read_to_string(id_path)?.trim().to_string())
        } else {
            let id = uuid::Uuid::new_v4().to_string();
            fs::write(id_path, &id)?;
            Ok(id)
        }
    }
}
```

#### TLS Connection
```rust
use tokio::net::TcpStream;
use tokio_rustls::{TlsAcceptor, TlsConnector, TlsStream};
use rustls::{Certificate, ClientConfig, ServerConfig, PrivateKey};
use rustls::client::ServerCertVerifier;
use rustls::server::ClientCertVerifier;
use std::sync::Arc;

pub struct TLSManager {
    certificate: Certificate,
    private_key: PrivateKey,
}

impl TLSManager {
    pub fn new(cert_pem: &[u8], key_pem: &[u8]) -> Result<Self, Box<dyn std::error::Error>> {
        let certs = rustls_pemfile::certs(&mut &cert_pem[..])?
            .into_iter()
            .map(Certificate)
            .collect::<Vec<_>>();
        
        let certificate = certs.into_iter().next()
            .ok_or("No certificate found")?;
        
        let keys = rustls_pemfile::pkcs8_private_keys(&mut &key_pem[..])?;
        let private_key = PrivateKey(keys.into_iter().next()
            .ok_or("No private key found")?);
        
        Ok(Self {
            certificate,
            private_key,
        })
    }
    
    pub async fn connect_to_device(
        &self,
        host: &str,
        port: u16,
        peer_certificate: Option<Certificate>,
    ) -> Result<TlsStream<TcpStream>, Box<dyn std::error::Error>> {
        let tcp_stream = TcpStream::connect((host, port)).await?;
        
        let mut config = ClientConfig::builder()
            .with_safe_defaults()
            .with_custom_certificate_verifier(Arc::new(
                CustomCertVerifier::new(peer_certificate)
            ))
            .with_single_cert(
                vec![self.certificate.clone()],
                self.private_key.clone(),
            )?;
        
        config.alpn_protocols = vec![b"kdeconnect".to_vec()];
        
        let connector = TlsConnector::from(Arc::new(config));
        let domain = rustls::ServerName::try_from(host)?;
        
        let tls_stream = connector.connect(domain, tcp_stream).await?;
        
        Ok(tls_stream)
    }
    
    pub async fn accept_connection(
        &self,
        tcp_stream: TcpStream,
    ) -> Result<TlsStream<TcpStream>, Box<dyn std::error::Error>> {
        let config = ServerConfig::builder()
            .with_safe_defaults()
            .with_client_cert_verifier(Arc::new(
                AcceptAllClientVerifier
            ))
            .with_single_cert(
                vec![self.certificate.clone()],
                self.private_key.clone(),
            )?;
        
        let acceptor = TlsAcceptor::from(Arc::new(config));
        let tls_stream = acceptor.accept(tcp_stream).await?;
        
        Ok(tls_stream)
    }
}

// Custom certificate verifier for pairing
struct CustomCertVerifier {
    expected_cert: Option<Certificate>,
}

impl CustomCertVerifier {
    fn new(expected_cert: Option<Certificate>) -> Self {
        Self { expected_cert }
    }
}

impl ServerCertVerifier for CustomCertVerifier {
    fn verify_server_cert(
        &self,
        end_entity: &Certificate,
        _intermediates: &[Certificate],
        _server_name: &rustls::ServerName,
        _scts: &mut dyn Iterator<Item = &[u8]>,
        _ocsp_response: &[u8],
        _now: std::time::SystemTime,
    ) -> Result<rustls::client::ServerCertVerified, rustls::Error> {
        if let Some(expected) = &self.expected_cert {
            if end_entity.0 == expected.0 {
                Ok(rustls::client::ServerCertVerified::assertion())
            } else {
                Err(rustls::Error::InvalidCertificate(
                    rustls::CertificateError::ApplicationVerificationFailure
                ))
            }
        } else {
            // Accept any certificate during pairing
            Ok(rustls::client::ServerCertVerified::assertion())
        }
    }
}

// Accept all client certificates
struct AcceptAllClientVerifier;

impl ClientCertVerifier for AcceptAllClientVerifier {
    fn client_auth_root_subjects(&self) -> &[rustls::DistinguishedName] {
        &[]
    }
    
    fn verify_client_cert(
        &self,
        _end_entity: &Certificate,
        _intermediates: &[Certificate],
        _now: std::time::SystemTime,
    ) -> Result<rustls::server::ClientCertVerified, rustls::Error> {
        Ok(rustls::server::ClientCertVerified::assertion())
    }
}
```

## Network Programming

### UDP Discovery (Android)
```kotlin
class DeviceDiscovery(
    private val context: Context,
    private val onDeviceFound: (DeviceInfo) -> Unit
) {
    private val multicastGroup = InetAddress.getByName("224.0.0.251")
    private val discoveryPort = 1716
    private var socket: MulticastSocket? = null
    private var isRunning = false
    
    companion object {
        private const val BUFFER_SIZE = 1024
        private const val BROADCAST_INTERVAL_MS = 5000L
    }
    
    suspend fun start() = withContext(Dispatchers.IO) {
        isRunning = true
        
        // Acquire multicast lock
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val multicastLock = wifiManager.createMulticastLock("kdeconnect_discovery")
        multicastLock.acquire()
        
        try {
            socket = MulticastSocket(discoveryPort).apply {
                reuseAddress = true
                networkInterface = getActiveNetworkInterface()
                joinGroup(multicastGroup)
            }
            
            // Start broadcast coroutine
            launch {
                while (isRunning) {
                    broadcastIdentity()
                    delay(BROADCAST_INTERVAL_MS)
                }
            }
            
            // Listen for responses
            val buffer = ByteArray(BUFFER_SIZE)
            while (isRunning) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket?.receive(packet)
                
                val data = String(packet.data, 0, packet.length)
                parseIdentityPacket(data)?.let { deviceInfo ->
                    onDeviceFound(deviceInfo)
                }
            }
        } finally {
            socket?.leaveGroup(multicastGroup)
            socket?.close()
            multicastLock.release()
        }
    }
    
    fun stop() {
        isRunning = false
    }
    
    private fun broadcastIdentity() {
        val identity = createIdentityPacket()
        val data = identity.toByteArray()
        val packet = DatagramPacket(
            data,
            data.size,
            multicastGroup,
            discoveryPort
        )
        socket?.send(packet)
    }
    
    private fun createIdentityPacket(): String {
        return JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("type", "kdeconnect.identity")
            put("body", JSONObject().apply {
                put("deviceId", getDeviceId())
                put("deviceName", Build.MODEL)
                put("deviceType", "phone")
                put("protocolVersion", 7)
                put("incomingCapabilities", JSONArray(listOf(
                    "kdeconnect.battery",
                    "kdeconnect.ping",
                    "kdeconnect.share.request"
                )))
                put("outgoingCapabilities", JSONArray(listOf(
                    "kdeconnect.battery",
                    "kdeconnect.ping",
                    "kdeconnect.share.request"
                )))
                put("tcpPort", getTcpPort())
            })
        }.toString()
    }
    
    private fun parseIdentityPacket(data: String): DeviceInfo? {
        return try {
            val json = JSONObject(data)
            if (json.getString("type") == "kdeconnect.identity") {
                val body = json.getJSONObject("body")
                DeviceInfo(
                    id = body.getString("deviceId"),
                    name = body.getString("deviceName"),
                    deviceType = body.getString("deviceType"),
                    protocolVersion = body.getInt("protocolVersion"),
                    tcpPort = body.getInt("tcpPort")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun getActiveNetworkInterface(): NetworkInterface? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress
            
            if (ipAddress != 0) {
                val address = InetAddress.getByAddress(ByteBuffer.allocate(4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(ipAddress)
                    .array())
                
                NetworkInterface.getByInetAddress(address)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
```

### UDP Discovery (Rust)
```rust
use tokio::net::UdpSocket;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use serde::{Deserialize, Serialize};

const MULTICAST_ADDR: Ipv4Addr = Ipv4Addr::new(224, 0, 0, 251);
const DISCOVERY_PORT: u16 = 1716;

#[derive(Debug, Serialize, Deserialize)]
pub struct IdentityPacket {
    id: i64,
    #[serde(rename = "type")]
    packet_type: String,
    body: IdentityBody,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct IdentityBody {
    #[serde(rename = "deviceId")]
    device_id: String,
    #[serde(rename = "deviceName")]
    device_name: String,
    #[serde(rename = "deviceType")]
    device_type: String,
    #[serde(rename = "protocolVersion")]
    protocol_version: i32,
    #[serde(rename = "incomingCapabilities")]
    incoming_capabilities: Vec<String>,
    #[serde(rename = "outgoingCapabilities")]
    outgoing_capabilities: Vec<String>,
    #[serde(rename = "tcpPort")]
    tcp_port: u16,
}

pub struct Discovery {
    socket: UdpSocket,
    device_id: String,
    tcp_port: u16,
}

impl Discovery {
    pub async fn new(device_id: String, tcp_port: u16) -> Result<Self, std::io::Error> {
        let socket = UdpSocket::bind(("0.0.0.0", DISCOVERY_PORT)).await?;
        socket.set_broadcast(true)?;
        socket.join_multicast_v4(MULTICAST_ADDR, Ipv4Addr::UNSPECIFIED)?;
        
        Ok(Self {
            socket,
            device_id,
            tcp_port,
        })
    }
    
    pub async fn broadcast_identity(&self) -> Result<(), Box<dyn std::error::Error>> {
        let packet = IdentityPacket {
            id: chrono::Utc::now().timestamp_millis(),
            packet_type: "kdeconnect.identity".to_string(),
            body: IdentityBody {
                device_id: self.device_id.clone(),
                device_name: hostname::get()?.to_string_lossy().to_string(),
                device_type: "desktop".to_string(),
                protocol_version: 7,
                incoming_capabilities: vec![
                    "kdeconnect.battery".to_string(),
                    "kdeconnect.ping".to_string(),
                    "kdeconnect.share.request".to_string(),
                ],
                outgoing_capabilities: vec![
                    "kdeconnect.battery".to_string(),
                    "kdeconnect.ping".to_string(),
                    "kdeconnect.share.request".to_string(),
                ],
                tcp_port: self.tcp_port,
            },
        };
        
        let json = serde_json::to_string(&packet)?;
        let data = json.as_bytes();
        
        self.socket
            .send_to(data, (MULTICAST_ADDR, DISCOVERY_PORT))
            .await?;
        
        Ok(())
    }
    
    pub async fn listen(&self) -> Result<(IdentityPacket, SocketAddr), Box<dyn std::error::Error>> {
        let mut buffer = vec![0u8; 1024];
        let (len, addr) = self.socket.recv_from(&mut buffer).await?;
        
        let data = &buffer[..len];
        let packet: IdentityPacket = serde_json::from_slice(data)?;
        
        Ok((packet, addr))
    }
    
    pub async fn discover_devices(
        &self,
        duration: tokio::time::Duration,
    ) -> Result<Vec<(IdentityPacket, SocketAddr)>, Box<dyn std::error::Error>> {
        let mut devices = Vec::new();
        let deadline = tokio::time::Instant::now() + duration;
        
        // Broadcast our identity
        self.broadcast_identity().await?;
        
        // Listen for responses
        while tokio::time::Instant::now() < deadline {
            match tokio::time::timeout(
                deadline - tokio::time::Instant::now(),
                self.listen()
            ).await {
                Ok(Ok((packet, addr))) => {
                    if packet.body.device_id != self.device_id {
                        devices.push((packet, addr));
                    }
                }
                Ok(Err(e)) => {
                    eprintln!("Error receiving packet: {}", e);
                }
                Err(_) => break,
            }
        }
        
        Ok(devices)
    }
}
```

### TCP Payload Transfer (Both Platforms)

#### Android Payload Transfer
```kotlin
class PayloadTransfer {
    suspend fun sendFile(
        socket: SSLSocket,
        file: File
    ) = withContext(Dispatchers.IO) {
        val payloadSocket = ServerSocket(0).apply {
            reuseAddress = true
        }
        val payloadPort = payloadSocket.localPort
        
        try {
            // Send packet with payload info
            val packet = NetworkPacket(
                id = System.currentTimeMillis(),
                type = "kdeconnect.share.request",
                body = JSONObject().apply {
                    put("filename", file.name)
                    put("filesize", file.length())
                },
                payloadSize = file.length(),
                payloadTransferInfo = PayloadTransferInfo(payloadPort)
            )
            
            socket.getOutputStream().write(packet.toByteArray())
            socket.getOutputStream().flush()
            
            // Wait for connection
            val payloadConnection = withTimeout(30000) {
                payloadSocket.accept()
            }
            
            // Transfer file
            file.inputStream().use { input ->
                payloadConnection.getOutputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalSent = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalSent += bytesRead
                        
                        // Report progress
                        val progress = (totalSent * 100 / file.length()).toInt()
                        // Emit progress event
                    }
                }
            }
        } finally {
            payloadSocket.close()
        }
    }
    
    suspend fun receiveFile(
        packet: NetworkPacket,
        outputFile: File
    ) = withContext(Dispatchers.IO) {
        val port = packet.payloadTransferInfo?.port ?: return@withContext
        val fileSize = packet.payloadSize
        
        Socket().use { socket ->
            socket.connect(InetSocketAddress(
                packet.sourceAddress,
                port
            ), 30000)
            
            socket.getInputStream().use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalReceived = 0L
                    
                    while (totalReceived < fileSize &&
                           input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalReceived += bytesRead
                        
                        // Report progress
                        val progress = (totalReceived * 100 / fileSize).toInt()
                        // Emit progress event
                    }
                }
            }
        }
    }
}
```

#### Rust Payload Transfer
```rust
use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use std::path::Path;

pub struct PayloadTransfer;

impl PayloadTransfer {
    pub async fn send_file(
        control_stream: &mut TlsStream<TcpStream>,
        file_path: &Path,
    ) -> Result<(), Box<dyn std::error::Error>> {
        // Create listener for payload
        let listener = TcpListener::bind("0.0.0.0:0").await?;
        let payload_port = listener.local_addr()?.port();
        
        // Open file
        let file = tokio::fs::File::open(file_path).await?;
        let file_size = file.metadata().await?.len();
        
        // Send packet with payload info
        let packet = NetworkPacket {
            id: chrono::Utc::now().timestamp_millis(),
            packet_type: "kdeconnect.share.request".to_string(),
            body: serde_json::json!({
                "filename": file_path.file_name().unwrap().to_string_lossy(),
                "filesize": file_size,
            }),
            payload_size: Some(file_size),
            payload_transfer_info: Some(PayloadTransferInfo {
                port: payload_port,
            }),
        };
        
        let packet_json = serde_json::to_string(&packet)?;
        control_stream.write_all(packet_json.as_bytes()).await?;
        control_stream.write_all(b"\n").await?;
        control_stream.flush().await?;
        
        // Wait for connection
        let (mut payload_stream, _) = tokio::time::timeout(
            Duration::from_secs(30),
            listener.accept()
        ).await??;
        
        // Transfer file
        let mut file = file;
        let mut buffer = vec![0u8; 64 * 1024];
        let mut total_sent = 0u64;
        
        loop {
            let bytes_read = file.read(&mut buffer).await?;
            if bytes_read == 0 {
                break;
            }
            
            payload_stream.write_all(&buffer[..bytes_read]).await?;
            total_sent += bytes_read as u64;
            
            // Report progress
            let progress = (total_sent * 100 / file_size) as u8;
            // Emit progress event
        }
        
        payload_stream.flush().await?;
        
        Ok(())
    }
    
    pub async fn receive_file(
        packet: &NetworkPacket,
        host: &str,
        output_path: &Path,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let port = packet.payload_transfer_info
            .as_ref()
            .ok_or("No payload transfer info")?
            .port;
        
        let file_size = packet.payload_size
            .ok_or("No payload size")?;
        
        // Connect to payload port
        let mut stream = TcpStream::connect((host, port)).await?;
        
        // Create output file
        let mut file = tokio::fs::File::create(output_path).await?;
        
        // Receive file
        let mut buffer = vec![0u8; 64 * 1024];
        let mut total_received = 0u64;
        
        while total_received < file_size {
            let bytes_read = stream.read(&mut buffer).await?;
            if bytes_read == 0 {
                break;
            }
            
            file.write_all(&buffer[..bytes_read]).await?;
            total_received += bytes_read as u64;
            
            // Report progress
            let progress = (total_received * 100 / file_size) as u8;
            // Emit progress event
        }
        
        file.flush().await?;
        
        Ok(())
    }
}
```

## Network Security Best Practices

### 1. Certificate Pinning
- Verify peer certificate fingerprint matches expected value
- Store trusted certificates securely
- Implement certificate rotation mechanism

### 2. TLS Configuration
- Use TLS 1.2 or higher
- Disable weak cipher suites
- Enable perfect forward secrecy
- Validate certificate chains

### 3. Network Protection
- Implement connection timeouts
- Rate limit connection attempts
- Validate all input data
- Sanitize file paths and names

### 4. Error Handling
```kotlin
sealed class NetworkError {
    object Timeout : NetworkError()
    object ConnectionRefused : NetworkError()
    data class SSLError(val cause: String) : NetworkError()
    data class ProtocolError(val message: String) : NetworkError()
}
```

## Debugging Network Issues

### Android Debugging
```bash
# Monitor network traffic
adb shell tcpdump -i any -s 0 -w /sdcard/capture.pcap
adb pull /sdcard/capture.pcap

# Check open sockets
adb shell netstat -an | grep 1714

# Test connectivity
adb shell ping -c 4 <device-ip>

# View logs
adb logcat -s KdeConnect:D NetworkPacket:D TLS:D
```

### Rust Debugging
```bash
# Enable TLS debugging
SSLKEYLOGFILE=tls-keys.txt RUST_LOG=debug cargo run

# Analyze with Wireshark
wireshark -i any -f "port 1714 or port 1716"

# Test TLS handshake
openssl s_client -connect localhost:1714 -showcerts
```

## Resources

- [TLS 1.3 RFC](https://datatracker.ietf.org/doc/html/rfc8446)
- [KDE Connect Protocol](https://invent.kde.org/network/kdeconnect-kde)
- [Android Network Security](https://developer.android.com/training/articles/security-ssl)
- [Rust TLS Library (rustls)](https://docs.rs/rustls/)
- [Certificate Transparency](https://certificate.transparency.dev/)
