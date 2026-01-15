# COSMIC Connect Protocol Debug Document

## Purpose
This document provides a comprehensive reference for debugging discovery and pairing issues in a Rust implementation of the COSMIC Connect protocol for COSMIC Desktop, specifically when communicating with the COSMIC Connect Android app.

---

## 1. Protocol Overview

### Key Constants
```rust
const UDP_PORT: u16 = 1716;
const MIN_TCP_PORT: u16 = 1714;
const MAX_TCP_PORT: u16 = 1764;
const PROTOCOL_VERSION: u8 = 7;
const PACKET_TYPE_IDENTITY: &str = "cosmicconnect.identity";
const PACKET_TYPE_PAIR: &str = "cosmicconnect.pair";
```

### Communication Flow Summary
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       KDE CONNECT PAIRING SEQUENCE                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   COSMIC App (Desktop)                    Android App (Phone)               │
│        │                                        │                           │
│        │  ──── UDP Broadcast (1716) ──────────► │                           │
│        │       Identity Packet + tcpPort        │                           │
│        │                                        │                           │
│        │  ◄──── TCP Connect (to tcpPort) ────── │                           │
│        │                                        │                           │
│        │  ◄──── Identity Packet (TCP) ───────── │                           │
│        │                                        │                           │
│        │  ════ TLS Handshake (both sides) ════  │                           │
│        │       (Server mode determined by       │                           │
│        │        deviceId comparison)            │                           │
│        │                                        │                           │
│        │  ──── Identity Packet (encrypted) ───► │                           │
│        │                                        │                           │
│        │  ═══════════ CONNECTED ════════════════│                           │
│        │                                        │                           │
│        │  ──── Pair Request (pair: true) ─────► │                           │
│        │                                        │                           │
│        │  ◄──── Pair Accept (pair: true) ────── │                           │
│        │                                        │                           │
│        │  ════════════ PAIRED ══════════════════│                           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Discovery Phase (UDP Broadcast)

### What Desktop Must Do
1. **Bind UDP socket** to port `1716` on `0.0.0.0`
2. **Send UDP broadcast** containing identity packet to `255.255.255.255:1716`
3. **Listen for incoming UDP** identity packets from other devices
4. **Also listen on TCP** port (1714-1764 range) for incoming connections

### Identity Packet Format (UDP)
```json
{
    "id": 1642176543000,
    "type": "cosmicconnect.identity",
    "body": {
        "deviceId": "740bd4b9_b418_4ee4_97d6_caf1da8151be",
        "deviceName": "My COSMIC Desktop",
        "deviceType": "desktop",
        "protocolVersion": 7,
        "tcpPort": 1716,
        "incomingCapabilities": [
            "cosmicconnect.battery.request",
            "cosmicconnect.clipboard",
            "cosmicconnect.clipboard.connect",
            "cosmicconnect.connectivity_report.request",
            "cosmicconnect.contacts.request_all_uids_timestamps",
            "cosmicconnect.contacts.request_vcards_by_uid",
            "cosmicconnect.findmyphone.request",
            "cosmicconnect.mousepad.keyboardstate",
            "cosmicconnect.mousepad.request",
            "cosmicconnect.mpris",
            "cosmicconnect.mpris.request",
            "cosmicconnect.notification",
            "cosmicconnect.notification.action",
            "cosmicconnect.notification.reply",
            "cosmicconnect.notification.request",
            "cosmicconnect.ping",
            "cosmicconnect.runcommand",
            "cosmicconnect.runcommand.request",
            "cosmicconnect.sftp.request",
            "cosmicconnect.share.request",
            "cosmicconnect.sms.request",
            "cosmicconnect.sms.request_conversation",
            "cosmicconnect.sms.request_conversations",
            "cosmicconnect.systemvolume.request",
            "cosmicconnect.telephony.request_mute"
        ],
        "outgoingCapabilities": [
            "cosmicconnect.battery",
            "cosmicconnect.clipboard",
            "cosmicconnect.clipboard.connect",
            "cosmicconnect.connectivity_report",
            "cosmicconnect.contacts.response_uids_timestamps",
            "cosmicconnect.contacts.response_vcards",
            "cosmicconnect.findmyphone.request",
            "cosmicconnect.mousepad.echo",
            "cosmicconnect.mousepad.keyboardstate",
            "cosmicconnect.mousepad.request",
            "cosmicconnect.mpris",
            "cosmicconnect.mpris.request",
            "cosmicconnect.notification",
            "cosmicconnect.notification.request",
            "cosmicconnect.ping",
            "cosmicconnect.runcommand",
            "cosmicconnect.sftp",
            "cosmicconnect.share.request",
            "cosmicconnect.sms.messages",
            "cosmicconnect.systemvolume",
            "cosmicconnect.telephony"
        ]
    }
}
```

### CRITICAL: Packet Termination
**Every packet MUST be terminated with a single newline character (`\n` / `0x0A`)**

```rust
// CORRECT
let packet_bytes = format!("{}\n", serde_json::to_string(&packet)?);

// WRONG - will cause parsing failures on Android
let packet_bytes = format!("{}\r\n", serde_json::to_string(&packet)?);  // NO CRLF!
let packet_bytes = serde_json::to_string(&packet)?;  // Missing newline!
```

### Debug Checkpoint 1: UDP Discovery
```bash
# Monitor UDP traffic on port 1716
sudo tcpdump -i any udp port 1716 -vvv -X

# Test if your app is sending UDP broadcasts
sudo tcpdump -i any udp and broadcast -vvv

# Check if firewall is blocking
sudo nft list ruleset | grep 1716
# or
sudo iptables -L -n | grep 1716
```

**Expected behavior**: When the Android app is opened, it should send UDP broadcasts. Your desktop app should receive these AND respond.

---

## 3. TCP Connection Establishment

### Connection Flow
When Device A receives a UDP identity packet from Device B:
1. Device A extracts `tcpPort` from the identity packet
2. Device A initiates TCP connection to `<sender_ip>:<tcpPort>`
3. Device A sends its OWN identity packet over the TCP connection (BEFORE TLS!)
4. TLS handshake begins

### Who Connects to Whom?
Both devices may attempt to connect to each other simultaneously. The protocol handles this gracefully - typically whichever connection succeeds first is used.

### Debug Checkpoint 2: TCP Connection
```bash
# Check if your app is listening on TCP
ss -tlnp | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]"

# Monitor TCP connections
sudo tcpdump -i any tcp port 1716 -vvv

# Test TCP connectivity manually
nc -zv <phone_ip> 1716
```

---

## 4. TLS Handshake (CRITICAL SECTION)

### Certificate Generation
COSMIC Connect uses **self-signed RSA 2048-bit certificates**. The certificate MUST be generated with specific attributes:

```rust
// Certificate requirements:
// - RSA 2048-bit key
// - Self-signed X.509 certificate
// - Common Name (CN) = deviceId (UUID with underscores instead of hyphens)
// - Organization (O) = "KDE"
// - Organizational Unit (OU) = "COSMIC Connect"
// - Valid from: 1 year ago
// - Valid until: 10 years from now
```

**OpenSSL equivalent command** (for reference):
```bash
openssl req -new -x509 -sha256 -newkey rsa:2048 \
    -nodes -days 3650 \
    -keyout privateKey.pem \
    -out certificate.pem \
    -subj "/CN=740bd4b9_b418_4ee4_97d6_caf1da8151be/O=KDE/OU=COSMIC Connect"
```

### TLS Role Determination (CRITICAL!)
The TLS role (client vs server) is determined by **comparing deviceIds lexicographically**:

```rust
// The device with the LARGER deviceId acts as the TLS SERVER
// The device with the SMALLER deviceId acts as the TLS CLIENT

fn determine_tls_role(my_device_id: &str, their_device_id: &str) -> TlsRole {
    if my_device_id > their_device_id {
        TlsRole::Server  // I start the TLS handshake as server (wait for ClientHello)
    } else {
        TlsRole::Client  // I start the TLS handshake as client (send ClientHello)
    }
}
```

**IMPORTANT**: This is independent of who initiated the TCP connection! Even if you connected TO them (TCP client), you might still be the TLS SERVER.

### TLS Handshake Sequence
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TLS HANDSHAKE DETAIL                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   TCP Connection Established                                                │
│        │                                                                    │
│        ▼                                                                    │
│   Identity Packet Exchanged (PLAINTEXT over TCP)                            │
│        │                                                                    │
│        ▼                                                                    │
│   Compare deviceIds → Determine TLS roles                                   │
│        │                                                                    │
│        ▼                                                                    │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │ If my_device_id > their_device_id:                                  │   │
│   │    I am TLS SERVER → Call accept_tls() / startServerEncryption()   │   │
│   │ Else:                                                               │   │
│   │    I am TLS CLIENT → Call connect_tls() / startClientEncryption()  │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│        │                                                                    │
│        ▼                                                                    │
│   TLS Handshake completes with mutual certificate exchange                  │
│        │                                                                    │
│        ▼                                                                    │
│   Connection is now encrypted                                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Cipher Suites
The Android app (and COSMIC Connect) expects these cipher suites:
```rust
const SUPPORTED_CIPHER_SUITES: &[&str] = &[
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", 
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
    // Legacy (avoid if possible):
    // "TLS_RSA_WITH_RC4_128_SHA",
    // "TLS_RSA_WITH_RC4_128_MD5",
];
```

### TLS Configuration in Rust
```rust
use rustls::{ClientConfig, ServerConfig, Certificate, PrivateKey};
use rustls::version::TLS12;  // COSMIC Connect uses TLS 1.2

// Important settings:
// - TLS 1.2 (minimum, but TLS 1.3 may also work with newer Android apps)
// - Disable certificate verification for untrusted devices
// - Enable certificate verification for paired/trusted devices
// - Use your self-signed certificate for authentication

fn create_tls_config() -> Result<(ClientConfig, ServerConfig), Error> {
    let cert = load_or_generate_certificate()?;
    let key = load_or_generate_private_key()?;
    
    // For UNTRUSTED (not yet paired) devices:
    // - Accept ANY certificate (we verify via the pairing process)
    let client_config = ClientConfig::builder()
        .with_safe_default_cipher_suites()
        .with_safe_default_kx_groups()
        .with_protocol_versions(&[&TLS12])?
        .with_custom_certificate_verifier(Arc::new(NoVerifier))  // Accept any cert
        .with_client_auth_cert(vec![cert.clone()], key.clone())?;
    
    // Server config similar...
}
```

### Debug Checkpoint 3: TLS Handshake
```bash
# Capture TLS handshake with Wireshark/tshark
sudo tshark -i any -f "tcp port 1716" -Y "ssl.handshake" -V

# Check your certificate
openssl x509 -in your_cert.pem -text -noout

# Test TLS connection (if you have the cert)
openssl s_client -connect <phone_ip>:1716 -cert your_cert.pem -key your_key.pem
```

**Common TLS Errors**:
| Error | Cause | Solution |
|-------|-------|----------|
| `Handshake failed` | Wrong TLS role | Check deviceId comparison logic |
| `Certificate verify failed` | Missing client cert | Ensure you send your cert |
| `BLOCK_TYPE_IS_NOT_01` | PKCS1 padding issue | Check RSA key generation |
| `Unexpected handshake message` | Role mismatch | Verify who should be server/client |

---

## 5. Pairing Process

### Pairing Packet Format
```json
// Request pairing
{
    "id": 1642176600000,
    "type": "cosmicconnect.pair",
    "body": {
        "pair": true
    }
}

// Accept pairing
{
    "id": 1642176601000,
    "type": "cosmicconnect.pair", 
    "body": {
        "pair": true
    }
}

// Reject pairing OR unpair
{
    "id": 1642176602000,
    "type": "cosmicconnect.pair",
    "body": {
        "pair": false
    }
}
```

### Pairing Sequence
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PAIRING SEQUENCE                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Desktop                                       Phone                       │
│      │                                            │                         │
│      │  ──── pair: true ──────────────────────►   │  User clicks "Pair"    │
│      │                                            │                         │
│      │                                            │  Phone shows prompt     │
│      │                                            │  "Accept pairing from   │
│      │                                            │   'My COSMIC Desktop'?" │
│      │                                            │                         │
│      │  ◄──── pair: true ─────────────────────    │  User clicks "Accept"  │
│      │                                            │                         │
│      │  ═══════════ PAIRED ════════════════════   │                         │
│      │                                            │                         │
│      │  Store their certificate locally           │                         │
│      │  for future authentication                 │                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Certificate Storage After Pairing
Once paired, store the remote device's certificate:
```rust
// Store in: ~/.config/cosmicconnect/<deviceId>/certificate.pem
// or equivalent for COSMIC

struct TrustedDevice {
    device_id: String,
    device_name: String,
    device_type: String,
    certificate: X509Certificate,
    last_ip: IpAddr,
}
```

### Debug Checkpoint 4: Pairing
```bash
# Monitor encrypted traffic (won't see content, but can see packet flow)
sudo tcpdump -i any tcp port 1716 -vvv

# Check COSMIC Connect's stored devices (for comparison)
ls -la ~/.config/cosmicconnect/
cat ~/.config/cosmicconnect/*/certificate.pem
```

---

## 6. Common Issues and Solutions

### Issue 1: Device Not Discovered
**Symptoms**: Phone doesn't see desktop or vice versa

**Debug Steps**:
```bash
# 1. Verify UDP socket is bound
ss -ulnp | grep 1716

# 2. Verify UDP broadcasts are being sent
sudo tcpdump -i any udp port 1716 -c 10

# 3. Check firewall
sudo ufw status
sudo nft list ruleset

# 4. Verify same network
ip addr show
# Phone and desktop must be on same subnet
```

**Solutions**:
- Open firewall ports 1714-1764 for both UDP and TCP
- Ensure devices are on same network/subnet
- Check for AP isolation on WiFi router

### Issue 2: TCP Connection Fails
**Symptoms**: UDP discovery works, but TCP connection times out

**Debug Steps**:
```bash
# Test TCP connectivity
nc -zv <phone_ip> 1716

# Check if listening
ss -tlnp | grep 171
```

**Solutions**:
- Verify tcpPort in identity packet is correct
- Check TCP firewall rules
- Ensure TCP server is started BEFORE sending UDP broadcast

### Issue 3: TLS Handshake Fails
**Symptoms**: Connection established, then immediately closes

**Debug Steps**:
```bash
# Check certificate
openssl x509 -in your_cert.pem -text -noout | head -20

# Verify key matches certificate
openssl rsa -in your_key.pem -check

# Compare modulus
openssl x509 -noout -modulus -in cert.pem | openssl md5
openssl rsa -noout -modulus -in key.pem | openssl md5
# These should match!
```

**Solutions**:
- Verify TLS role determination logic
- Ensure certificate is RSA 2048-bit
- Check cipher suite compatibility
- Verify certificate CN matches deviceId

### Issue 4: Pairing Request Not Received
**Symptoms**: TLS works, but pairing never starts

**Debug Steps**:
```rust
// Add logging after TLS handshake
println!("TLS handshake complete, waiting for packets...");

// Log all received packets
loop {
    let packet = read_packet(&mut tls_stream)?;
    println!("Received: {}", serde_json::to_string_pretty(&packet)?);
}
```

**Solutions**:
- Ensure you're reading from the TLS stream, not raw TCP
- Check packet parsing (especially newline handling)
- Verify JSON deserialization works

### Issue 5: Paired But No Communication
**Symptoms**: Pairing succeeds, but no subsequent packets work

**Debug Steps**:
```bash
# Check stored certificate
cat ~/.config/your_app/<deviceId>/certificate.pem

# Verify it matches what the device sent
openssl x509 -in <saved_cert> -fingerprint -noout
```

**Solutions**:
- Store the certificate AFTER successful pairing
- Use stored certificate for subsequent TLS verification
- Verify deviceId matching on reconnection

---

## 7. Debugging Commands Reference

### Network Debugging
```bash
# Monitor all COSMIC Connect traffic
sudo tcpdump -i any "udp port 1716 or tcp portrange 1714-1764" -vvv -w cosmicconnect.pcap

# Real-time packet inspection
sudo tshark -i any -f "port 1716" -Y "json" -T fields -e json.value.string

# Check listening sockets
ss -tulnp | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]"

# Firewall check (nftables)
sudo nft list ruleset | grep -A5 -B5 1716

# Firewall check (iptables)
sudo iptables -L -n -v | grep -E "171[4-9]|17[2-5][0-9]|176[0-4]"
```

### NixOS Specific
```nix
# configuration.nix - Open required ports
networking.firewall = {
  allowedTCPPortRanges = [ { from = 1714; to = 1764; } ];
  allowedUDPPortRanges = [ { from = 1714; to = 1764; } ];
};
```

### Android Debugging
```bash
# Get Android logs (connect phone via USB with debugging enabled)
adb logcat | grep -i cosmicconnect

# More specific
adb logcat --pid=$(adb shell pidof -s org.cosmic.cosmicconnect)

# Filter for connection issues
adb logcat | grep -E "(LanLinkProvider|Handshake|identity|pair)"
```

---

## 8. Rust Implementation Checklist

### □ Discovery Module
- [ ] UDP socket binds to 0.0.0.0:1716
- [ ] UDP socket can receive broadcast packets
- [ ] UDP socket sends broadcast to 255.255.255.255:1716
- [ ] Identity packet includes all required fields
- [ ] Identity packet terminated with single `\n`
- [ ] tcpPort field matches actual TCP listening port

### □ TCP Server Module  
- [ ] TCP server listens on port 1714-1764
- [ ] TCP server accepts incoming connections
- [ ] Identity packet sent BEFORE TLS handshake
- [ ] Received identity packet parsed correctly

### □ TLS Module
- [ ] RSA 2048-bit key generated
- [ ] Self-signed certificate with correct CN/O/OU
- [ ] TLS role determined by deviceId comparison
- [ ] Client/Server mode set correctly
- [ ] Certificate sent during handshake
- [ ] Cipher suites compatible with Android

### □ Pairing Module
- [ ] Pair request packet formatted correctly
- [ ] Pair response handled
- [ ] Remote certificate stored after pairing
- [ ] Trusted devices verified by stored certificate

### □ Packet Protocol
- [ ] All packets terminated with `\n`
- [ ] JSON serialization matches expected format
- [ ] `id` field is integer timestamp (milliseconds)
- [ ] `type` field follows `cosmicconnect.*` pattern
- [ ] `body` field is object (even if empty `{}`)

---

## 9. Reference Implementation Patterns

### Packet Structure
```rust
use serde::{Deserialize, Serialize};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Debug, Serialize, Deserialize)]
pub struct NetworkPacket {
    pub id: i64,
    #[serde(rename = "type")]
    pub packet_type: String,
    pub body: serde_json::Value,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload_size: Option<i64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload_transfer_info: Option<serde_json::Value>,
}

impl NetworkPacket {
    pub fn new_identity(device_info: &DeviceInfo, tcp_port: u16) -> Self {
        Self {
            id: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_millis() as i64,
            packet_type: "cosmicconnect.identity".to_string(),
            body: serde_json::json!({
                "deviceId": device_info.device_id,
                "deviceName": device_info.device_name,
                "deviceType": device_info.device_type,
                "protocolVersion": 7,
                "tcpPort": tcp_port,
                "incomingCapabilities": device_info.incoming_capabilities,
                "outgoingCapabilities": device_info.outgoing_capabilities,
            }),
            payload_size: None,
            payload_transfer_info: None,
        }
    }
    
    pub fn new_pair(pair: bool) -> Self {
        Self {
            id: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_millis() as i64,
            packet_type: "cosmicconnect.pair".to_string(),
            body: serde_json::json!({ "pair": pair }),
            payload_size: None,
            payload_transfer_info: None,
        }
    }
    
    pub fn serialize(&self) -> Result<Vec<u8>, serde_json::Error> {
        let json = serde_json::to_string(self)?;
        Ok(format!("{}\n", json).into_bytes())
    }
    
    pub fn deserialize(data: &[u8]) -> Result<Self, serde_json::Error> {
        // Remove trailing newline if present
        let data = if data.last() == Some(&b'\n') {
            &data[..data.len() - 1]
        } else {
            data
        };
        serde_json::from_slice(data)
    }
}
```

### DeviceId Generation
```rust
use uuid::Uuid;

pub fn generate_device_id() -> String {
    // COSMIC Connect uses UUIDs with underscores instead of hyphens
    Uuid::new_v4()
        .to_string()
        .replace('-', "_")
}

// Example: "740bd4b9_b418_4ee4_97d6_caf1da8151be"
```

### TLS Role Determination
```rust
pub enum TlsRole {
    Client,
    Server,
}

pub fn determine_tls_role(my_device_id: &str, their_device_id: &str) -> TlsRole {
    // Lexicographic comparison - larger deviceId is the server
    if my_device_id > their_device_id {
        TlsRole::Server
    } else {
        TlsRole::Client
    }
}
```

---

## 10. Test Cases for Verification

### Test 1: UDP Broadcast Reception
```bash
# On desktop, start your app
# On phone, open COSMIC Connect app and pull down to refresh
# Expected: Your app logs receiving identity packet from phone
```

### Test 2: TCP Connection Acceptance
```bash
# After UDP exchange, phone should connect to your TCP port
# Expected: TCP connection accepted, identity packet received
```

### Test 3: TLS Handshake Completion
```bash
# After TCP identity exchange, TLS should start
# Expected: TLS handshake completes without error
```

### Test 4: Pairing Request/Response
```bash
# Click "Request Pair" on either device
# Expected: Pairing prompt appears on other device
# Accept pairing
# Expected: Both devices show as paired
```

### Test 5: Post-Pairing Communication
```bash
# After pairing, send a ping
# Expected: Ping notification appears on other device
```

---

## 11. Useful Resources

- **Official Protocol Spec**: https://invent.kde.org/network/cosmicconnect-meta/blob/master/protocol.md
- **Valent Protocol Reference**: https://valent.andyholmes.ca/documentation/protocol.html
- **COSMIC Connect KDE Source**: https://invent.kde.org/network/cosmicconnect-kde
- **COSMIC Connect Android Source**: https://invent.kde.org/network/cosmicconnect-android
- **mconnect (Vala Implementation)**: https://github.com/bboozzoo/mconnect
- **GSConnect (JavaScript/GNOME)**: https://github.com/GSConnect/gnome-shell-extension-gsconnect

---

## Quick Diagnostic Flowchart

```
START: Device not connecting
         │
         ▼
    ┌────────────────┐
    │ Can you see    │──No──► Check firewall (UDP 1716)
    │ UDP packets?   │        Check same network/subnet
    └───────┬────────┘
            │Yes
            ▼
    ┌────────────────┐
    │ TCP connection │──No──► Check TCP ports 1714-1764
    │ established?   │        Verify tcpPort in identity
    └───────┬────────┘
            │Yes
            ▼
    ┌────────────────┐
    │ Identity sent  │──No──► Send identity BEFORE TLS
    │ before TLS?    │        Check packet format + \n
    └───────┬────────┘
            │Yes
            ▼
    ┌────────────────┐
    │ TLS handshake  │──No──► Check TLS role (deviceId comparison)
    │ succeeds?      │        Verify certificate (RSA 2048)
    └───────┬────────┘        Check cipher suites
            │Yes
            ▼
    ┌────────────────┐
    │ Pairing works? │──No──► Check packet parsing over TLS
    └───────┬────────┘        Verify pair packet format
            │Yes
            ▼
       SUCCESS! 🎉
```

---

*Document Version: 1.0*
*Last Updated: 2025-01-15*
*For: COSMIC Desktop COSMIC Connect Rust Implementation*
