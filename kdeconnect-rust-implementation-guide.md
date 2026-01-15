# KDE Connect Rust Implementation Guide
## Complete Step-by-Step Process for COSMIC Desktop

> **Target**: Implement KDE Connect protocol in Rust for COSMIC Desktop
> **Based on**: Protocol v7, compatible with KDE Connect Android app
> **Last Updated**: 2025-01-15

---

## Table of Contents
1. [Project Setup & Dependencies](#1-project-setup--dependencies)
2. [Core Data Structures](#2-core-data-structures)
3. [Module 1: Certificate Management](#3-module-1-certificate-management)
4. [Module 2: UDP Discovery](#4-module-2-udp-discovery)
5. [Module 3: TCP Server & Connection](#5-module-3-tcp-server--connection)
6. [Module 4: TLS Handshake](#6-module-4-tls-handshake)
7. [Module 5: Pairing Protocol](#7-module-5-pairing-protocol)
8. [Integration & Main Loop](#8-integration--main-loop)
9. [Testing & Verification](#9-testing--verification)
10. [Debugging Toolkit](#10-debugging-toolkit)

---

## 1. Project Setup & Dependencies

### 1.1 Cargo.toml Configuration

```toml
[package]
name = "kdeconnect-cosmic"
version = "0.1.0"
edition = "2021"

[dependencies]
# Async runtime
tokio = { version = "1.35", features = ["full"] }

# Networking & TLS
tokio-rustls = "0.25"
rustls = { version = "0.22", features = ["dangerous_configuration"] }
rustls-pemfile = "2.0"

# Certificate generation
rcgen = { version = "0.12", features = ["pem"] }

# Serialization
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"

# Utilities
uuid = { version = "1.6", features = ["v4"] }
anyhow = "1.0"
thiserror = "1.0"
tracing = "0.1"
tracing-subscriber = "0.3"

# Optional: For GUI integration with COSMIC
# libcosmic = { git = "https://github.com/pop-os/libcosmic" }
```

### 1.2 Project Structure

```
kdeconnect-cosmic/
├── Cargo.toml
├── src/
│   ├── main.rs                 # Application entry point
│   ├── lib.rs                  # Library exports
│   ├── protocol/
│   │   ├── mod.rs              # Protocol module exports
│   │   ├── packet.rs           # NetworkPacket structures
│   │   └── constants.rs        # Protocol constants
│   ├── discovery/
│   │   ├── mod.rs              # Discovery module
│   │   └── udp.rs              # UDP broadcast handler
│   ├── connection/
│   │   ├── mod.rs              # Connection management
│   │   ├── tcp.rs              # TCP server/client
│   │   └── tls.rs              # TLS handshake logic
│   ├── security/
│   │   ├── mod.rs              # Security module
│   │   ├── certificate.rs      # Certificate generation/storage
│   │   └── verifier.rs         # Custom TLS verifier
│   ├── pairing/
│   │   ├── mod.rs              # Pairing logic
│   │   └── state.rs            # Pairing state machine
│   ├── device/
│   │   ├── mod.rs              # Device management
│   │   └── info.rs             # Device information
│   └── error.rs                # Error types
└── tests/
    ├── integration_tests.rs
    └── protocol_tests.rs
```

---

## 2. Core Data Structures

### 2.1 Protocol Constants (`src/protocol/constants.rs`)

```rust
/// KDE Connect protocol constants
pub const UDP_PORT: u16 = 1716;
pub const MIN_TCP_PORT: u16 = 1714;
pub const MAX_TCP_PORT: u16 = 1764;
pub const PROTOCOL_VERSION: u8 = 7;

/// Packet types
pub const PACKET_TYPE_IDENTITY: &str = "kdeconnect.identity";
pub const PACKET_TYPE_PAIR: &str = "kdeconnect.pair";

/// Default capabilities
pub const DEFAULT_INCOMING_CAPABILITIES: &[&str] = &[
    "kdeconnect.battery.request",
    "kdeconnect.clipboard",
    "kdeconnect.clipboard.connect",
    "kdeconnect.connectivity_report.request",
    "kdeconnect.findmyphone.request",
    "kdeconnect.mousepad.keyboardstate",
    "kdeconnect.mousepad.request",
    "kdeconnect.mpris",
    "kdeconnect.mpris.request",
    "kdeconnect.notification",
    "kdeconnect.notification.action",
    "kdeconnect.notification.reply",
    "kdeconnect.notification.request",
    "kdeconnect.ping",
    "kdeconnect.runcommand",
    "kdeconnect.runcommand.request",
    "kdeconnect.sftp.request",
    "kdeconnect.share.request",
];

pub const DEFAULT_OUTGOING_CAPABILITIES: &[&str] = &[
    "kdeconnect.battery",
    "kdeconnect.clipboard",
    "kdeconnect.clipboard.connect",
    "kdeconnect.connectivity_report",
    "kdeconnect.findmyphone.request",
    "kdeconnect.mousepad.echo",
    "kdeconnect.mousepad.keyboardstate",
    "kdeconnect.mousepad.request",
    "kdeconnect.mpris",
    "kdeconnect.mpris.request",
    "kdeconnect.notification",
    "kdeconnect.notification.request",
    "kdeconnect.ping",
    "kdeconnect.runcommand",
    "kdeconnect.sftp",
    "kdeconnect.share.request",
];
```

### 2.2 Network Packet Structure (`src/protocol/packet.rs`)

```rust
use serde::{Deserialize, Serialize};
use std::time::{SystemTime, UNIX_EPOCH};
use anyhow::{Result, Context};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkPacket {
    /// Unique packet ID (timestamp in milliseconds)
    pub id: i64,
    
    /// Packet type (e.g., "kdeconnect.identity", "kdeconnect.pair")
    #[serde(rename = "type")]
    pub packet_type: String,
    
    /// Packet payload/body
    pub body: serde_json::Value,
    
    /// Optional: size of binary payload
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload_size: Option<i64>,
    
    /// Optional: payload transfer metadata
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload_transfer_info: Option<serde_json::Value>,
}

impl NetworkPacket {
    /// Create a new identity packet
    pub fn new_identity(
        device_id: &str,
        device_name: &str,
        device_type: &str,
        tcp_port: u16,
        incoming_capabilities: Vec<String>,
        outgoing_capabilities: Vec<String>,
    ) -> Self {
        Self {
            id: Self::generate_id(),
            packet_type: "kdeconnect.identity".to_string(),
            body: serde_json::json!({
                "deviceId": device_id,
                "deviceName": device_name,
                "deviceType": device_type,
                "protocolVersion": 7,
                "tcpPort": tcp_port,
                "incomingCapabilities": incoming_capabilities,
                "outgoingCapabilities": outgoing_capabilities,
            }),
            payload_size: None,
            payload_transfer_info: None,
        }
    }
    
    /// Create a new pair request/response packet
    pub fn new_pair(pair: bool) -> Self {
        Self {
            id: Self::generate_id(),
            packet_type: "kdeconnect.pair".to_string(),
            body: serde_json::json!({ "pair": pair }),
            payload_size: None,
            payload_transfer_info: None,
        }
    }
    
    /// Generate timestamp ID in milliseconds
    fn generate_id() -> i64 {
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_millis() as i64
    }
    
    /// Serialize packet to bytes with newline termination
    /// CRITICAL: Must end with single '\n' character!
    pub fn serialize(&self) -> Result<Vec<u8>> {
        let json = serde_json::to_string(self)
            .context("Failed to serialize packet")?;
        Ok(format!("{}\n", json).into_bytes())
    }
    
    /// Deserialize packet from bytes
    pub fn deserialize(data: &[u8]) -> Result<Self> {
        // Remove trailing newline if present
        let data = if data.last() == Some(&b'\n') {
            &data[..data.len() - 1]
        } else {
            data
        };
        
        serde_json::from_slice(data)
            .context("Failed to deserialize packet")
    }
    
    /// Extract device ID from identity packet
    pub fn get_device_id(&self) -> Option<String> {
        self.body.get("deviceId")?.as_str().map(String::from)
    }
    
    /// Extract TCP port from identity packet
    pub fn get_tcp_port(&self) -> Option<u16> {
        self.body.get("tcpPort")?.as_u64().map(|p| p as u16)
    }
    
    /// Check if this is an identity packet
    pub fn is_identity(&self) -> bool {
        self.packet_type == "kdeconnect.identity"
    }
    
    /// Check if this is a pair packet
    pub fn is_pair(&self) -> bool {
        self.packet_type == "kdeconnect.pair"
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_packet_serialization() {
        let packet = NetworkPacket::new_pair(true);
        let bytes = packet.serialize().unwrap();
        
        // Must end with newline
        assert_eq!(bytes.last(), Some(&b'\n'));
        
        // Should deserialize correctly
        let deserialized = NetworkPacket::deserialize(&bytes).unwrap();
        assert_eq!(packet.packet_type, deserialized.packet_type);
    }
    
    #[test]
    fn test_identity_packet() {
        let packet = NetworkPacket::new_identity(
            "test_device_id",
            "Test Device",
            "desktop",
            1716,
            vec!["cap1".to_string()],
            vec!["cap2".to_string()],
        );
        
        assert!(packet.is_identity());
        assert_eq!(packet.get_device_id(), Some("test_device_id".to_string()));
        assert_eq!(packet.get_tcp_port(), Some(1716));
    }
}
```

### 2.3 Device Information (`src/device/info.rs`)

```rust
use uuid::Uuid;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceInfo {
    /// Unique device identifier (UUID with underscores)
    pub device_id: String,
    
    /// Human-readable device name
    pub device_name: String,
    
    /// Device type: "desktop", "laptop", "phone", "tablet"
    pub device_type: String,
    
    /// Capabilities this device can receive
    pub incoming_capabilities: Vec<String>,
    
    /// Capabilities this device can send
    pub outgoing_capabilities: Vec<String>,
}

impl DeviceInfo {
    /// Generate a new device ID (UUID with underscores)
    pub fn generate_device_id() -> String {
        Uuid::new_v4().to_string().replace('-', "_")
    }
    
    /// Create new device info with defaults for COSMIC Desktop
    pub fn new_desktop(device_name: String) -> Self {
        Self {
            device_id: Self::generate_device_id(),
            device_name,
            device_type: "desktop".to_string(),
            incoming_capabilities: crate::protocol::constants::DEFAULT_INCOMING_CAPABILITIES
                .iter()
                .map(|&s| s.to_string())
                .collect(),
            outgoing_capabilities: crate::protocol::constants::DEFAULT_OUTGOING_CAPABILITIES
                .iter()
                .map(|&s| s.to_string())
                .collect(),
        }
    }
}

/// Information about a discovered remote device
#[derive(Debug, Clone)]
pub struct RemoteDevice {
    pub device_id: String,
    pub device_name: String,
    pub device_type: String,
    pub tcp_port: u16,
    pub ip_address: std::net::IpAddr,
    pub incoming_capabilities: Vec<String>,
    pub outgoing_capabilities: Vec<String>,
    pub discovered_at: std::time::Instant,
}
```

### 2.4 Error Types (`src/error.rs`)

```rust
use thiserror::Error;

#[derive(Error, Debug)]
pub enum KdeConnectError {
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),
    
    #[error("JSON serialization error: {0}")]
    Json(#[from] serde_json::Error),
    
    #[error("TLS error: {0}")]
    Tls(#[from] rustls::Error),
    
    #[error("Certificate error: {0}")]
    Certificate(String),
    
    #[error("Protocol error: {0}")]
    Protocol(String),
    
    #[error("Invalid packet: {0}")]
    InvalidPacket(String),
    
    #[error("Connection error: {0}")]
    Connection(String),
    
    #[error("Pairing error: {0}")]
    Pairing(String),
}

pub type Result<T> = std::result::Result<T, KdeConnectError>;
```

---

## 3. Module 1: Certificate Management

### 3.1 Certificate Generation (`src/security/certificate.rs`)

```rust
use rcgen::{CertificateParams, KeyPair, DnType, SignatureAlgorithm};
use rustls::pki_types::{CertificateDer, PrivateKeyDer};
use std::path::{Path, PathBuf};
use std::fs;
use anyhow::{Result, Context};

/// Certificate manager for KDE Connect
pub struct CertificateManager {
    cert_path: PathBuf,
    key_path: PathBuf,
}

impl CertificateManager {
    pub fn new(config_dir: &Path) -> Self {
        Self {
            cert_path: config_dir.join("certificate.pem"),
            key_path: config_dir.join("privateKey.pem"),
        }
    }
    
    /// Load existing certificate or generate new one
    pub fn load_or_generate(&self, device_id: &str) -> Result<(Vec<CertificateDer<'static>>, PrivateKeyDer<'static>)> {
        if self.cert_path.exists() && self.key_path.exists() {
            self.load_certificate()
        } else {
            self.generate_certificate(device_id)
        }
    }
    
    /// Generate a new self-signed certificate for KDE Connect
    /// CRITICAL: Must be RSA 2048-bit with specific attributes
    fn generate_certificate(&self, device_id: &str) -> Result<(Vec<CertificateDer<'static>>, PrivateKeyDer<'static>)> {
        // Generate RSA 2048-bit key pair
        let key_pair = KeyPair::generate_for(&SignatureAlgorithm::RSA_SHA256)?;
        
        // Create certificate parameters
        let mut params = CertificateParams::default();
        
        // Set distinguished name components
        // CN = deviceId (with underscores)
        // O = KDE
        // OU = KDE Connect
        params.distinguished_name.push(DnType::CommonName, device_id);
        params.distinguished_name.push(DnType::OrganizationName, "KDE");
        params.distinguished_name.push(DnType::OrganizationalUnitName, "KDE Connect");
        
        // Set validity period (1 year ago to 10 years from now)
        params.not_before = time::OffsetDateTime::now_utc() - time::Duration::days(365);
        params.not_after = time::OffsetDateTime::now_utc() + time::Duration::days(3650);
        
        // Generate self-signed certificate
        let cert = params.self_signed(&key_pair)?;
        
        // Save to files
        fs::create_dir_all(self.cert_path.parent().unwrap())?;
        fs::write(&self.cert_path, cert.pem())?;
        fs::write(&self.key_path, key_pair.serialize_pem())?;
        
        // Convert to rustls types
        let cert_der = vec![CertificateDer::from(cert.der().clone())];
        let key_der = PrivateKeyDer::try_from(key_pair.serialize_der())?;
        
        Ok((cert_der, key_der))
    }
    
    /// Load existing certificate from disk
    fn load_certificate(&self) -> Result<(Vec<CertificateDer<'static>>, PrivateKeyDer<'static>)> {
        // Load certificate
        let cert_pem = fs::read(&self.cert_path)?;
        let certs = rustls_pemfile::certs(&mut &cert_pem[..])
            .collect::<Result<Vec<_>, _>>()?;
        
        // Load private key
        let key_pem = fs::read(&self.key_path)?;
        let mut key_reader = &key_pem[..];
        let key = rustls_pemfile::private_key(&mut key_reader)?
            .context("No private key found")?;
        
        Ok((certs, key))
    }
    
    /// Get certificate fingerprint (for verification)
    pub fn get_fingerprint(&self) -> Result<String> {
        use sha2::{Sha256, Digest};
        
        let cert_pem = fs::read(&self.cert_path)?;
        let mut hasher = Sha256::new();
        hasher.update(&cert_pem);
        let hash = hasher.finalize();
        
        Ok(hex::encode(hash))
    }
}
```

### 3.2 Custom TLS Verifier (`src/security/verifier.rs`)

```rust
use rustls::client::danger::{HandshakeSignatureValid, ServerCertVerified, ServerCertVerifier};
use rustls::pki_types::{CertificateDer, ServerName, UnixTime};
use rustls::{DigitallySignedStruct, Error, SignatureScheme};
use std::fmt::Debug;

/// Custom certificate verifier that accepts any certificate
/// This is required for KDE Connect's self-signed certificate model
#[derive(Debug)]
pub struct AcceptAnyCertVerifier;

impl ServerCertVerifier for AcceptAnyCertVerifier {
    fn verify_server_cert(
        &self,
        _end_entity: &CertificateDer<'_>,
        _intermediates: &[CertificateDer<'_>],
        _server_name: &ServerName<'_>,
        _ocsp_response: &[u8],
        _now: UnixTime,
    ) -> Result<ServerCertVerified, Error> {
        // Accept any certificate (KDE Connect uses self-signed certs)
        Ok(ServerCertVerified::assertion())
    }
    
    fn verify_tls12_signature(
        &self,
        _message: &[u8],
        _cert: &CertificateDer<'_>,
        _dss: &DigitallySignedStruct,
    ) -> Result<HandshakeSignatureValid, Error> {
        Ok(HandshakeSignatureValid::assertion())
    }
    
    fn verify_tls13_signature(
        &self,
        _message: &[u8],
        _cert: &CertificateDer<'_>,
        _dss: &DigitallySignedStruct,
    ) -> Result<HandshakeSignatureValid, Error> {
        Ok(HandshakeSignatureValid::assertion())
    }
    
    fn supported_verify_schemes(&self) -> Vec<SignatureScheme> {
        vec![
            SignatureScheme::RSA_PKCS1_SHA256,
            SignatureScheme::RSA_PKCS1_SHA384,
            SignatureScheme::RSA_PKCS1_SHA512,
            SignatureScheme::ECDSA_NISTP256_SHA256,
            SignatureScheme::ECDSA_NISTP384_SHA384,
            SignatureScheme::RSA_PSS_SHA256,
            SignatureScheme::RSA_PSS_SHA384,
            SignatureScheme::RSA_PSS_SHA512,
        ]
    }
}
```

**Verification Checkpoint**:
```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_certificate_generation() {
        let temp_dir = tempfile::tempdir().unwrap();
        let manager = CertificateManager::new(temp_dir.path());
        
        let device_id = "test_device_id";
        let result = manager.generate_certificate(device_id);
        
        assert!(result.is_ok());
        assert!(manager.cert_path.exists());
        assert!(manager.key_path.exists());
    }
}
```

---

## 4. Module 2: UDP Discovery

### 4.1 UDP Broadcast Handler (`src/discovery/udp.rs`)

```rust
use tokio::net::UdpSocket;
use tokio::sync::mpsc;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::sync::Arc;
use tracing::{info, warn, error, debug};

use crate::protocol::packet::NetworkPacket;
use crate::device::info::{DeviceInfo, RemoteDevice};
use crate::protocol::constants::UDP_PORT;
use crate::error::Result;

pub struct UdpDiscovery {
    socket: Arc<UdpSocket>,
    device_info: DeviceInfo,
    tcp_port: u16,
}

impl UdpDiscovery {
    /// Create new UDP discovery service
    pub async fn new(device_info: DeviceInfo, tcp_port: u16) -> Result<Self> {
        // Bind to 0.0.0.0:1716 to receive broadcasts
        let socket = UdpSocket::bind(SocketAddr::new(
            IpAddr::V4(Ipv4Addr::UNSPECIFIED),
            UDP_PORT,
        )).await?;
        
        // Enable broadcast
        socket.set_broadcast(true)?;
        
        info!("UDP discovery bound to 0.0.0.0:{}", UDP_PORT);
        
        Ok(Self {
            socket: Arc::new(socket),
            device_info,
            tcp_port,
        })
    }
    
    /// Send identity broadcast to discover devices
    pub async fn send_broadcast(&self) -> Result<()> {
        let packet = NetworkPacket::new_identity(
            &self.device_info.device_id,
            &self.device_info.device_name,
            &self.device_info.device_type,
            self.tcp_port,
            self.device_info.incoming_capabilities.clone(),
            self.device_info.outgoing_capabilities.clone(),
        );
        
        let data = packet.serialize()?;
        let broadcast_addr = SocketAddr::new(
            IpAddr::V4(Ipv4Addr::BROADCAST),
            UDP_PORT,
        );
        
        self.socket.send_to(&data, broadcast_addr).await?;
        info!("Sent UDP broadcast identity packet");
        
        Ok(())
    }
    
    /// Start listening for UDP broadcasts
    /// Returns a channel receiver for discovered devices
    pub async fn start_listening(
        self: Arc<Self>,
    ) -> mpsc::Receiver<RemoteDevice> {
        let (tx, rx) = mpsc::channel(100);
        
        tokio::spawn(async move {
            let mut buf = vec![0u8; 65536];
            
            loop {
                match self.socket.recv_from(&mut buf).await {
                    Ok((len, addr)) => {
                        debug!("Received {} bytes from {}", len, addr);
                        
                        match NetworkPacket::deserialize(&buf[..len]) {
                            Ok(packet) if packet.is_identity() => {
                                if let Some(remote) = self.parse_remote_device(packet, addr) {
                                    info!("Discovered device: {} ({})", remote.device_name, remote.device_id);
                                    let _ = tx.send(remote).await;
                                }
                            }
                            Ok(_) => {
                                debug!("Received non-identity packet via UDP");
                            }
                            Err(e) => {
                                warn!("Failed to parse UDP packet: {}", e);
                            }
                        }
                    }
                    Err(e) => {
                        error!("UDP recv_from error: {}", e);
                        tokio::time::sleep(tokio::time::Duration::from_secs(1)).await;
                    }
                }
            }
        });
        
        rx
    }
    
    /// Parse remote device from identity packet
    fn parse_remote_device(
        &self,
        packet: NetworkPacket,
        addr: SocketAddr,
    ) -> Option<RemoteDevice> {
        let device_id = packet.get_device_id()?;
        
        // Don't discover ourselves
        if device_id == self.device_info.device_id {
            return None;
        }
        
        let device_name = packet.body.get("deviceName")?.as_str()?.to_string();
        let device_type = packet.body.get("deviceType")?.as_str()?.to_string();
        let tcp_port = packet.get_tcp_port()?;
        
        let incoming_capabilities = packet.body
            .get("incomingCapabilities")?
            .as_array()?
            .iter()
            .filter_map(|v| v.as_str().map(String::from))
            .collect();
            
        let outgoing_capabilities = packet.body
            .get("outgoingCapabilities")?
            .as_array()?
            .iter()
            .filter_map(|v| v.as_str().map(String::from))
            .collect();
        
        Some(RemoteDevice {
            device_id,
            device_name,
            device_type,
            tcp_port,
            ip_address: addr.ip(),
            incoming_capabilities,
            outgoing_capabilities,
            discovered_at: std::time::Instant::now(),
        })
    }
}
```

**Verification Steps**:

1. **Test UDP Binding**:
```bash
# Check if app is listening on UDP 1716
sudo ss -ulnp | grep 1716

# Monitor UDP traffic
sudo tcpdump -i any udp port 1716 -vvv -X
```

2. **Test UDP Broadcast**:
```bash
# Send test broadcast manually
echo '{"id":1234567890,"type":"kdeconnect.identity","body":{"deviceId":"test","deviceName":"Test","deviceType":"desktop","protocolVersion":7,"tcpPort":1716}}
' | nc -u -b 255.255.255.255 1716
```

3. **Rust Test**:
```rust
#[tokio::test]
async fn test_udp_broadcast() {
    let device_info = DeviceInfo::new_desktop("Test Device".to_string());
    let discovery = UdpDiscovery::new(device_info, 1716).await.unwrap();
    
    // Should send without error
    discovery.send_broadcast().await.unwrap();
}
```

---

## 5. Module 3: TCP Server & Connection

### 5.1 TCP Connection Manager (`src/connection/tcp.rs`)

```rust
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::mpsc;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use std::net::SocketAddr;
use std::sync::Arc;
use tracing::{info, warn, error};

use crate::protocol::packet::NetworkPacket;
use crate::device::info::{DeviceInfo, RemoteDevice};
use crate::protocol::constants::{MIN_TCP_PORT, MAX_TCP_PORT};
use crate::error::Result;

pub struct TcpConnectionManager {
    device_info: DeviceInfo,
    listener: TcpListener,
    tcp_port: u16,
}

impl TcpConnectionManager {
    /// Create new TCP connection manager
    /// Binds to first available port in range 1714-1764
    pub async fn new(device_info: DeviceInfo) -> Result<Self> {
        let mut tcp_port = MIN_TCP_PORT;
        let listener = loop {
            match TcpListener::bind(format!("0.0.0.0:{}", tcp_port)).await {
                Ok(listener) => break listener,
                Err(_) if tcp_port < MAX_TCP_PORT => {
                    tcp_port += 1;
                }
                Err(e) => return Err(e.into()),
            }
        };
        
        info!("TCP server listening on port {}", tcp_port);
        
        Ok(Self {
            device_info,
            listener,
            tcp_port,
        })
    }
    
    /// Get the TCP port we're listening on
    pub fn port(&self) -> u16 {
        self.tcp_port
    }
    
    /// Start accepting TCP connections
    /// Returns a channel receiver for new connections
    pub async fn start_accepting(
        self: Arc<Self>,
    ) -> mpsc::Receiver<(TcpStream, NetworkPacket)> {
        let (tx, rx) = mpsc::channel(100);
        
        tokio::spawn(async move {
            loop {
                match self.listener.accept().await {
                    Ok((stream, addr)) => {
                        info!("Accepted TCP connection from {}", addr);
                        
                        let tx = tx.clone();
                        let device_info = self.device_info.clone();
                        
                        tokio::spawn(async move {
                            if let Err(e) = Self::handle_connection(stream, addr, device_info, tx).await {
                                error!("Error handling connection from {}: {}", addr, e);
                            }
                        });
                    }
                    Err(e) => {
                        error!("Failed to accept TCP connection: {}", e);
                    }
                }
            }
        });
        
        rx
    }
    
    /// Handle incoming TCP connection
    /// 1. Wait for identity packet (BEFORE TLS!)
    /// 2. Send our identity packet
    /// 3. Return stream and remote identity for TLS setup
    async fn handle_connection(
        mut stream: TcpStream,
        addr: SocketAddr,
        device_info: DeviceInfo,
        tx: mpsc::Sender<(TcpStream, NetworkPacket)>,
    ) -> Result<()> {
        // Read identity packet (terminated by newline)
        let mut buffer = Vec::new();
        let mut byte = [0u8; 1];
        
        loop {
            stream.read_exact(&mut byte).await?;
            buffer.push(byte[0]);
            
            if byte[0] == b'\n' {
                break;
            }
            
            // Prevent infinite reads
            if buffer.len() > 65536 {
                return Err(crate::error::KdeConnectError::Protocol(
                    "Identity packet too large".to_string()
                ));
            }
        }
        
        // Parse identity packet
        let remote_identity = NetworkPacket::deserialize(&buffer)?;
        if !remote_identity.is_identity() {
            return Err(crate::error::KdeConnectError::Protocol(
                "Expected identity packet".to_string()
            ));
        }
        
        info!("Received identity from {}: {:?}", addr, remote_identity.get_device_id());
        
        // Send our identity packet BEFORE TLS
        let our_identity = NetworkPacket::new_identity(
            &device_info.device_id,
            &device_info.device_name,
            &device_info.device_type,
            0, // Port doesn't matter in TCP context
            device_info.incoming_capabilities.clone(),
            device_info.outgoing_capabilities.clone(),
        );
        
        let data = our_identity.serialize()?;
        stream.write_all(&data).await?;
        stream.flush().await?;
        
        info!("Sent identity to {}", addr);
        
        // Pass stream to TLS handler
        let _ = tx.send((stream, remote_identity)).await;
        
        Ok(())
    }
    
    /// Connect to a remote device
    pub async fn connect_to_device(
        &self,
        remote: &RemoteDevice,
    ) -> Result<(TcpStream, NetworkPacket)> {
        let addr = SocketAddr::new(remote.ip_address, remote.tcp_port);
        info!("Connecting to {} at {}", remote.device_name, addr);
        
        let mut stream = TcpStream::connect(addr).await?;
        
        // Send our identity BEFORE TLS
        let our_identity = NetworkPacket::new_identity(
            &self.device_info.device_id,
            &self.device_info.device_name,
            &self.device_info.device_type,
            self.tcp_port,
            self.device_info.incoming_capabilities.clone(),
            self.device_info.outgoing_capabilities.clone(),
        );
        
        let data = our_identity.serialize()?;
        stream.write_all(&data).await?;
        stream.flush().await?;
        
        // Read their identity
        let mut buffer = Vec::new();
        let mut byte = [0u8; 1];
        
        loop {
            stream.read_exact(&mut byte).await?;
            buffer.push(byte[0]);
            
            if byte[0] == b'\n' {
                break;
            }
            
            if buffer.len() > 65536 {
                return Err(crate::error::KdeConnectError::Protocol(
                    "Identity packet too large".to_string()
                ));
            }
        }
        
        let remote_identity = NetworkPacket::deserialize(&buffer)?;
        if !remote_identity.is_identity() {
            return Err(crate::error::KdeConnectError::Protocol(
                "Expected identity packet".to_string()
            ));
        }
        
        info!("Received identity from remote device");
        
        Ok((stream, remote_identity))
    }
}
```

**Verification Steps**:

1. **Test TCP Listening**:
```bash
# Check if listening on correct port
ss -tlnp | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]"

# Test connection manually
nc -zv localhost 1716
```

2. **Monitor TCP Connections**:
```bash
sudo tcpdump -i any tcp port 1716 -A
```

---

## 6. Module 4: TLS Handshake

### 6.1 TLS Role Determination

```rust
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TlsRole {
    Client,
    Server,
}

impl TlsRole {
    /// Determine TLS role by comparing device IDs lexicographically
    /// CRITICAL: Larger deviceId acts as TLS SERVER
    pub fn determine(my_device_id: &str, their_device_id: &str) -> Self {
        if my_device_id > their_device_id {
            TlsRole::Server
        } else {
            TlsRole::Client
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_tls_role_determination() {
        assert_eq!(
            TlsRole::determine("zzzz", "aaaa"),
            TlsRole::Server
        );
        assert_eq!(
            TlsRole::determine("aaaa", "zzzz"),
            TlsRole::Client
        );
    }
}
```

### 6.2 TLS Handler (`src/connection/tls.rs`)

```rust
use tokio::net::TcpStream;
use tokio_rustls::{TlsAcceptor, TlsConnector, TlsStream};
use rustls::{ClientConfig, ServerConfig};
use rustls::pki_types::{CertificateDer, PrivateKeyDer, ServerName};
use std::sync::Arc;
use tracing::{info, debug};

use crate::security::verifier::AcceptAnyCertVerifier;
use crate::error::Result;

pub struct TlsHandler {
    server_config: Arc<ServerConfig>,
    client_config: Arc<ClientConfig>,
}

impl TlsHandler {
    /// Create new TLS handler with our certificate
    pub fn new(
        certs: Vec<CertificateDer<'static>>,
        key: PrivateKeyDer<'static>,
    ) -> Result<Self> {
        // Configure TLS server (for when we're the server)
        let mut server_config = ServerConfig::builder()
            .with_no_client_auth()
            .with_single_cert(certs.clone(), key.clone_key())?;
        
        // Set compatible cipher suites
        server_config.alpn_protocols = vec![b"kdeconnect".to_vec()];
        
        // Configure TLS client (for when we're the client)
        let mut client_config = ClientConfig::builder()
            .dangerous()
            .with_custom_certificate_verifier(Arc::new(AcceptAnyCertVerifier))
            .with_client_auth_cert(certs, key)?;
        
        client_config.alpn_protocols = vec![b"kdeconnect".to_vec()];
        
        Ok(Self {
            server_config: Arc::new(server_config),
            client_config: Arc::new(client_config),
        })
    }
    
    /// Perform TLS handshake as server
    pub async fn handshake_as_server(
        &self,
        stream: TcpStream,
    ) -> Result<TlsStream<TcpStream>> {
        info!("Starting TLS handshake as SERVER");
        
        let acceptor = TlsAcceptor::from(self.server_config.clone());
        let tls_stream = acceptor.accept(stream).await?;
        
        info!("TLS handshake completed as SERVER");
        Ok(tls_stream)
    }
    
    /// Perform TLS handshake as client
    pub async fn handshake_as_client(
        &self,
        stream: TcpStream,
        device_id: &str,
    ) -> Result<TlsStream<TcpStream>> {
        info!("Starting TLS handshake as CLIENT");
        
        // Use device ID as server name (required by rustls but not validated)
        let server_name = ServerName::try_from(device_id.to_string())?;
        
        let connector = TlsConnector::from(self.client_config.clone());
        let tls_stream = connector.connect(server_name, stream).await?;
        
        info!("TLS handshake completed as CLIENT");
        Ok(tls_stream)
    }
}
```

**Verification Steps**:

1. **Test TLS Handshake**:
```rust
#[tokio::test]
async fn test_tls_handshake() {
    // Create two certificate pairs
    let (certs1, key1) = generate_test_certificate("device_a");
    let (certs2, key2) = generate_test_certificate("device_z");
    
    let handler1 = TlsHandler::new(certs1, key1).unwrap();
    let handler2 = TlsHandler::new(certs2, key2).unwrap();
    
    // Create TCP connection pair
    let (stream1, stream2) = create_tcp_pair().await;
    
    // Determine roles
    let role1 = TlsRole::determine("device_a", "device_z");
    let role2 = TlsRole::determine("device_z", "device_a");
    
    assert_eq!(role1, TlsRole::Client);
    assert_eq!(role2, TlsRole::Server);
    
    // Perform handshake
    let (tls1, tls2) = tokio::join!(
        handler1.handshake_as_client(stream1, "device_z"),
        handler2.handshake_as_server(stream2)
    );
    
    assert!(tls1.is_ok());
    assert!(tls2.is_ok());
}
```

2. **OpenSSL Verification**:
```bash
# Test TLS connection
openssl s_client -connect localhost:1716 -showcerts
```

---

## 7. Module 5: Pairing Protocol

### 7.1 Pairing State Machine (`src/pairing/state.rs`)

```rust
use tokio_rustls::TlsStream;
use tokio::net::TcpStream;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tracing::{info, warn};

use crate::protocol::packet::NetworkPacket;
use crate::error::Result;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PairingState {
    Unpaired,
    Requested,
    RequestedByPeer,
    Paired,
}

pub struct PairingManager {
    state: PairingState,
}

impl PairingManager {
    pub fn new() -> Self {
        Self {
            state: PairingState::Unpaired,
        }
    }
    
    /// Send pairing request
    pub async fn request_pairing(
        &mut self,
        stream: &mut TlsStream<TcpStream>,
    ) -> Result<()> {
        info!("Sending pairing request");
        
        let packet = NetworkPacket::new_pair(true);
        let data = packet.serialize()?;
        
        stream.write_all(&data).await?;
        stream.flush().await?;
        
        self.state = PairingState::Requested;
        
        Ok(())
    }
    
    /// Accept pairing request
    pub async fn accept_pairing(
        &mut self,
        stream: &mut TlsStream<TcpStream>,
    ) -> Result<()> {
        info!("Accepting pairing request");
        
        let packet = NetworkPacket::new_pair(true);
        let data = packet.serialize()?;
        
        stream.write_all(&data).await?;
        stream.flush().await?;
        
        self.state = PairingState::Paired;
        
        Ok(())
    }
    
    /// Reject pairing request
    pub async fn reject_pairing(
        &mut self,
        stream: &mut TlsStream<TcpStream>,
    ) -> Result<()> {
        info!("Rejecting pairing request");
        
        let packet = NetworkPacket::new_pair(false);
        let data = packet.serialize()?;
        
        stream.write_all(&data).await?;
        stream.flush().await?;
        
        self.state = PairingState::Unpaired;
        
        Ok(())
    }
    
    /// Read and handle incoming packet
    pub async fn handle_packet(
        &mut self,
        stream: &mut TlsStream<TcpStream>,
    ) -> Result<Option<NetworkPacket>> {
        // Read packet (newline-terminated)
        let mut buffer = Vec::new();
        let mut byte = [0u8; 1];
        
        loop {
            match stream.read(&mut byte).await {
                Ok(0) => return Ok(None), // Connection closed
                Ok(_) => {
                    buffer.push(byte[0]);
                    if byte[0] == b'\n' {
                        break;
                    }
                }
                Err(e) => return Err(e.into()),
            }
            
            if buffer.len() > 65536 {
                return Err(crate::error::KdeConnectError::Protocol(
                    "Packet too large".to_string()
                ));
            }
        }
        
        let packet = NetworkPacket::deserialize(&buffer)?;
        
        // Handle pairing packets
        if packet.is_pair() {
            let pair = packet.body.get("pair")
                .and_then(|v| v.as_bool())
                .unwrap_or(false);
            
            if pair {
                match self.state {
                    PairingState::Requested => {
                        info!("Pairing accepted by peer");
                        self.state = PairingState::Paired;
                    }
                    PairingState::Unpaired => {
                        info!("Received pairing request from peer");
                        self.state = PairingState::RequestedByPeer;
                    }
                    _ => {}
                }
            } else {
                warn!("Pairing rejected by peer");
                self.state = PairingState::Unpaired;
            }
        }
        
        Ok(Some(packet))
    }
    
    pub fn state(&self) -> PairingState {
        self.state
    }
}
```

**Verification Steps**:

1. **Test Pairing Flow**:
```rust
#[tokio::test]
async fn test_pairing_flow() {
    let (mut stream1, mut stream2) = create_tls_pair().await;
    
    let mut manager1 = PairingManager::new();
    let mut manager2 = PairingManager::new();
    
    // Device 1 requests pairing
    manager1.request_pairing(&mut stream1).await.unwrap();
    assert_eq!(manager1.state(), PairingState::Requested);
    
    // Device 2 receives and accepts
    let packet = manager2.handle_packet(&mut stream2).await.unwrap();
    assert!(packet.is_some());
    assert_eq!(manager2.state(), PairingState::RequestedByPeer);
    
    manager2.accept_pairing(&mut stream2).await.unwrap();
    assert_eq!(manager2.state(), PairingState::Paired);
    
    // Device 1 receives acceptance
    let packet = manager1.handle_packet(&mut stream1).await.unwrap();
    assert!(packet.is_some());
    assert_eq!(manager1.state(), PairingState::Paired);
}
```

---

## 8. Integration & Main Loop

### 8.1 Main Application (`src/main.rs`)

```rust
use std::sync::Arc;
use std::path::PathBuf;
use tracing::{info, error};
use tracing_subscriber;

mod protocol;
mod discovery;
mod connection;
mod security;
mod pairing;
mod device;
mod error;

use device::info::DeviceInfo;
use security::certificate::CertificateManager;
use discovery::udp::UdpDiscovery;
use connection::tcp::TcpConnectionManager;
use connection::tls::{TlsHandler, TlsRole};
use pairing::state::PairingManager;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Initialize tracing
    tracing_subscriber::fmt::init();
    
    info!("Starting KDE Connect for COSMIC Desktop");
    
    // 1. Load device information
    let device_info = DeviceInfo::new_desktop(
        hostname::get()?
            .to_string_lossy()
            .to_string()
    );
    
    info!("Device ID: {}", device_info.device_id);
    info!("Device Name: {}", device_info.device_name);
    
    // 2. Load or generate certificate
    let config_dir = dirs::config_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("kdeconnect-cosmic");
    
    let cert_manager = CertificateManager::new(&config_dir);
    let (certs, key) = cert_manager.load_or_generate(&device_info.device_id)?;
    
    info!("Certificate loaded/generated");
    
    // 3. Start TCP server
    let tcp_manager = Arc::new(
        TcpConnectionManager::new(device_info.clone()).await?
    );
    let tcp_port = tcp_manager.port();
    
    info!("TCP server started on port {}", tcp_port);
    
    // 4. Start UDP discovery
    let udp_discovery = Arc::new(
        UdpDiscovery::new(device_info.clone(), tcp_port).await?
    );
    
    // Send initial broadcast
    udp_discovery.send_broadcast().await?;
    
    // 5. Start listening for UDP broadcasts
    let mut discovered_devices = udp_discovery.clone().start_listening().await;
    
    // 6. Start accepting TCP connections
    let mut incoming_connections = tcp_manager.clone().start_accepting().await;
    
    // 7. Create TLS handler
    let tls_handler = Arc::new(TlsHandler::new(certs, key)?);
    
    info!("KDE Connect service running");
    
    // Main event loop
    loop {
        tokio::select! {
            // Handle discovered devices
            Some(remote_device) = discovered_devices.recv() => {
                info!("Discovered: {} ({})", remote_device.device_name, remote_device.device_id);
                
                // Optional: Auto-connect to discovered devices
                let tcp_manager = tcp_manager.clone();
                let tls_handler = tls_handler.clone();
                let device_info = device_info.clone();
                
                tokio::spawn(async move {
                    if let Err(e) = handle_discovered_device(
                        remote_device,
                        tcp_manager,
                        tls_handler,
                        device_info,
                    ).await {
                        error!("Error connecting to discovered device: {}", e);
                    }
                });
            }
            
            // Handle incoming connections
            Some((stream, remote_identity)) = incoming_connections.recv() => {
                let remote_device_id = remote_identity.get_device_id()
                    .unwrap_or_else(|| "unknown".to_string());
                
                info!("Incoming connection from {}", remote_device_id);
                
                let tls_handler = tls_handler.clone();
                let device_info = device_info.clone();
                
                tokio::spawn(async move {
                    if let Err(e) = handle_incoming_connection(
                        stream,
                        remote_identity,
                        tls_handler,
                        device_info,
                    ).await {
                        error!("Error handling incoming connection: {}", e);
                    }
                });
            }
        }
    }
}

async fn handle_discovered_device(
    remote: device::info::RemoteDevice,
    tcp_manager: Arc<TcpConnectionManager>,
    tls_handler: Arc<TlsHandler>,
    device_info: DeviceInfo,
) -> anyhow::Result<()> {
    // Connect to remote device
    let (stream, remote_identity) = tcp_manager.connect_to_device(&remote).await?;
    
    let remote_device_id = remote_identity.get_device_id()
        .ok_or_else(|| anyhow::anyhow!("No device ID in identity"))?;
    
    // Determine TLS role
    let role = TlsRole::determine(&device_info.device_id, &remote_device_id);
    info!("TLS role for {}: {:?}", remote.device_name, role);
    
    // Perform TLS handshake
    let mut tls_stream = match role {
        TlsRole::Server => tls_handler.handshake_as_server(stream).await?,
        TlsRole::Client => tls_handler.handshake_as_client(stream, &remote_device_id).await?,
    };
    
    info!("TLS connection established with {}", remote.device_name);
    
    // Start pairing
    let mut pairing_manager = PairingManager::new();
    pairing_manager.request_pairing(&mut tls_stream).await?;
    
    // Handle packets
    loop {
        match pairing_manager.handle_packet(&mut tls_stream).await {
            Ok(Some(packet)) => {
                info!("Received packet: {:?}", packet.packet_type);
                
                if packet.is_pair() && pairing_manager.state() == pairing::state::PairingState::Paired {
                    info!("Successfully paired with {}", remote.device_name);
                    break;
                }
            }
            Ok(None) => {
                info!("Connection closed by {}", remote.device_name);
                break;
            }
            Err(e) => {
                error!("Error reading packet: {}", e);
                break;
            }
        }
    }
    
    Ok(())
}

async fn handle_incoming_connection(
    stream: tokio::net::TcpStream,
    remote_identity: protocol::packet::NetworkPacket,
    tls_handler: Arc<TlsHandler>,
    device_info: DeviceInfo,
) -> anyhow::Result<()> {
    let remote_device_id = remote_identity.get_device_id()
        .ok_or_else(|| anyhow::anyhow!("No device ID in identity"))?;
    
    // Determine TLS role
    let role = TlsRole::determine(&device_info.device_id, &remote_device_id);
    info!("TLS role for incoming connection: {:?}", role);
    
    // Perform TLS handshake
    let mut tls_stream = match role {
        TlsRole::Server => tls_handler.handshake_as_server(stream).await?,
        TlsRole::Client => tls_handler.handshake_as_client(stream, &remote_device_id).await?,
    };
    
    info!("TLS connection established");
    
    // Handle pairing
    let mut pairing_manager = PairingManager::new();
    
    loop {
        match pairing_manager.handle_packet(&mut tls_stream).await {
            Ok(Some(packet)) => {
                info!("Received packet: {:?}", packet.packet_type);
                
                if packet.is_pair() {
                    if pairing_manager.state() == pairing::state::PairingState::RequestedByPeer {
                        // Auto-accept for now (should show UI prompt)
                        info!("Auto-accepting pairing request");
                        pairing_manager.accept_pairing(&mut tls_stream).await?;
                    }
                    
                    if pairing_manager.state() == pairing::state::PairingState::Paired {
                        info!("Successfully paired");
                        break;
                    }
                }
            }
            Ok(None) => {
                info!("Connection closed");
                break;
            }
            Err(e) => {
                error!("Error reading packet: {}", e);
                break;
            }
        }
    }
    
    Ok(())
}
```

---

## 9. Testing & Verification

### 9.1 Integration Test Suite (`tests/integration_tests.rs`)

```rust
use tokio::time::{timeout, Duration};

#[tokio::test]
async fn test_full_discovery_and_pairing() {
    // Initialize logging for test
    let _ = tracing_subscriber::fmt::try_init();
    
    // Create two instances
    let device1 = create_test_device("Device 1").await;
    let device2 = create_test_device("Device 2").await;
    
    // Send broadcasts
    device1.send_broadcast().await.unwrap();
    device2.send_broadcast().await.unwrap();
    
    // Wait for discovery
    let discovered = timeout(
        Duration::from_secs(5),
        device1.wait_for_discovery()
    ).await;
    
    assert!(discovered.is_ok(), "Device discovery timed out");
    
    // Initiate pairing
    let paired = timeout(
        Duration::from_secs(10),
        device1.pair_with_discovered()
    ).await;
    
    assert!(paired.is_ok(), "Pairing timed out");
    assert!(paired.unwrap().is_ok(), "Pairing failed");
}

#[tokio::test]
async fn test_tls_role_determination() {
    let device_id_a = "aaaaaaaa_aaaa_aaaa_aaaa_aaaaaaaaaaaa";
    let device_id_z = "zzzzzzzz_zzzz_zzzz_zzzz_zzzzzzzzzzzz";
    
    let role_a = TlsRole::determine(device_id_a, device_id_z);
    let role_z = TlsRole::determine(device_id_z, device_id_a);
    
    assert_eq!(role_a, TlsRole::Client);
    assert_eq!(role_z, TlsRole::Server);
}
```

### 9.2 Manual Testing Checklist

```markdown
## Manual Testing Checklist

### Phase 1: UDP Discovery
- [ ] Start the application
- [ ] Open KDE Connect on Android
- [ ] Verify UDP broadcast is sent (check logs)
- [ ] Verify Android device is discovered (check logs)
- [ ] Run tcpdump to confirm UDP traffic
  ```bash
  sudo tcpdump -i any udp port 1716 -vvv
  ```

### Phase 2: TCP Connection
- [ ] After discovery, verify TCP connection is established
- [ ] Check that TCP port is in range 1714-1764
- [ ] Verify identity packets are exchanged BEFORE TLS
  ```bash
  sudo tcpdump -i any tcp port 1716 -A
  ```

### Phase 3: TLS Handshake
- [ ] Verify TLS role is determined correctly (check logs)
- [ ] Confirm TLS handshake completes successfully
- [ ] Check for any TLS errors in logs
- [ ] Verify certificate is RSA 2048-bit
  ```bash
  openssl x509 -in ~/.config/kdeconnect-cosmic/certificate.pem -text -noout
  ```

### Phase 4: Pairing
- [ ] Request pairing from desktop → Android
- [ ] Verify pairing prompt appears on Android
- [ ] Accept pairing on Android
- [ ] Confirm both devices show as paired
- [ ] Test reverse: Request pairing from Android → desktop

### Phase 5: Post-Pairing
- [ ] Send a ping from desktop to Android
- [ ] Verify ping notification appears on Android
- [ ] Send a ping from Android to desktop
- [ ] Test other capabilities (clipboard, file sharing, etc.)
```

---

## 10. Debugging Toolkit

### 10.1 Network Monitoring Commands

```bash
#!/bin/bash
# kdeconnect-debug.sh

echo "=== KDE Connect Network Debugging ==="

echo -e "\n1. Checking UDP Port 1716..."
ss -ulnp | grep 1716 || echo "Not listening on UDP 1716"

echo -e "\n2. Checking TCP Ports 1714-1764..."
ss -tlnp | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]" || echo "No TCP ports in range"

echo -e "\n3. Checking Firewall (nftables)..."
sudo nft list ruleset | grep -E "1716|1714|1764" || echo "No firewall rules found"

echo -e "\n4. Active Connections..."
ss -anp | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]"

echo -e "\n5. Network Interfaces..."
ip addr show

echo -e "\n6. Listening Services..."
netstat -tuln | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]"
```

### 10.2 Packet Capture Script

```bash
#!/bin/bash
# capture-kdeconnect.sh

DURATION=${1:-60}

echo "Capturing KDE Connect traffic for ${DURATION} seconds..."

sudo tcpdump -i any \
    '(udp port 1716) or (tcp portrange 1714-1764)' \
    -w kdeconnect_capture.pcap \
    -v &

PID=$!

sleep $DURATION

sudo kill $PID

echo "Capture complete. Analyzing..."
tcpdump -r kdeconnect_capture.pcap -A | head -100
```

### 10.3 Certificate Validation

```bash
#!/bin/bash
# verify-certificate.sh

CERT_PATH="$HOME/.config/kdeconnect-cosmic/certificate.pem"

if [ ! -f "$CERT_PATH" ]; then
    echo "Certificate not found at $CERT_PATH"
    exit 1
fi

echo "=== Certificate Information ==="
openssl x509 -in "$CERT_PATH" -text -noout | grep -E "Subject:|Issuer:|Not Before|Not After|Public-Key"

echo -e "\n=== Certificate Validation ==="
openssl x509 -in "$CERT_PATH" -noout -checkend 0 && echo "✓ Certificate is valid" || echo "✗ Certificate is expired"

echo -e "\n=== Key Type and Size ==="
openssl x509 -in "$CERT_PATH" -noout -text | grep "Public-Key:"
```

### 10.4 Logging Configuration

```rust
// In main.rs, add detailed logging
use tracing_subscriber::fmt::format::FmtSpan;

fn init_logging() {
    tracing_subscriber::fmt()
        .with_max_level(tracing::Level::DEBUG)
        .with_span_events(FmtSpan::ACTIVE)
        .with_target(true)
        .with_thread_ids(true)
        .with_line_number(true)
        .init();
}
```

### 10.5 Diagnostic Packet Sender

```rust
// Tool to send test packets
use tokio::net::UdpSocket;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let socket = UdpSocket::bind("0.0.0.0:0").await?;
    socket.set_broadcast(true)?;
    
    let packet = NetworkPacket::new_identity(
        "test_device",
        "Test Device",
        "desktop",
        1716,
        vec![],
        vec![],
    );
    
    let data = packet.serialize()?;
    
    socket.send_to(
        &data,
        "255.255.255.255:1716"
    ).await?;
    
    println!("Sent test identity packet");
    Ok(())
}
```

---

## Summary: Critical Implementation Points

### ✅ Must-Have Features

1. **Packet Termination**: Every packet MUST end with `\n` (0x0A), NOT `\r\n`
2. **TLS Role**: Determined by lexicographic comparison of deviceIds
3. **Identity Before TLS**: Identity packets exchanged BEFORE TLS handshake
4. **Certificate Format**: RSA 2048-bit, self-signed, CN=deviceId, O=KDE, OU=KDE Connect
5. **Port Range**: UDP 1716, TCP 1714-1764
6. **Protocol Version**: Must be 7

### 🎯 Implementation Order

1. Certificate management (generate/load)
2. UDP discovery (broadcast/receive)
3. TCP server (accept connections)
4. TCP client (connect to discovered devices)
5. TLS handshake (role-based)
6. Pairing protocol
7. Message handling

### 🔍 Verification Strategy

1. **Unit Tests**: Test each component in isolation
2. **Integration Tests**: Test full discovery-to-pairing flow
3. **Network Tests**: Use tcpdump/wireshark to verify packets
4. **Certificate Tests**: Verify certificate format with OpenSSL
5. **Real Device Tests**: Test with actual KDE Connect Android app

### 📊 Success Metrics

- ✅ UDP broadcasts visible in tcpdump
- ✅ Android device discovers desktop
- ✅ Desktop discovers Android device
- ✅ TCP connection establishes
- ✅ TLS handshake completes
- ✅ Pairing succeeds
- ✅ Can send/receive ping packets

---

*This implementation guide provides a complete, step-by-step approach to building a KDE Connect compatible application in Rust for COSMIC Desktop. Follow the verification steps at each phase to ensure correctness.*
