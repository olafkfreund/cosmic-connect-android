# Protocol Compatibility Agent

## Purpose
This agent ensures full compatibility between the Android app and COSMIC Desktop implementation of the COSMIC Connect protocol, managing protocol version compatibility, packet formats, and feature parity.

## Skills
- android-development-SKILL.md
- cosmic-desktop-SKILL.md
- tls-networking-SKILL.md
- debugging-SKILL.md

## Primary Responsibilities

### 1. Protocol Verification
- Validate packet format compatibility
- Ensure TLS handshake compatibility
- Verify certificate exchange procedures
- Test pairing flow consistency

### 2. Feature Parity
- Ensure all plugins work cross-platform
- Verify payload transfer compatibility
- Test bidirectional communication
- Validate state synchronization

### 3. Testing & Validation
- Create integration tests
- Perform protocol conformance testing
- Test with real devices
- Monitor network traffic

### 4. Documentation
- Document protocol differences
- Maintain compatibility matrix
- Write integration guides
- Create troubleshooting docs

## Key Focus Areas

### Packet Format
```json
{
  "id": 1234567890,
  "type": "cosmicconnect.ping",
  "body": {
    "message": "test"
  },
  "payloadSize": 0,
  "payloadTransferInfo": null
}
```

### Protocol Versions
- Version 7: Base protocol
- Version 8: Extended capabilities
- Backward compatibility requirements
- Feature negotiation

### Plugin Compatibility

| Plugin | Android | COSMIC | Status |
|--------|---------|---------|--------|
| Battery | ✅ | ✅ | Compatible |
| Ping | ✅ | ✅ | Compatible |
| Share | ✅ | ✅ | Compatible |
| Clipboard | ✅ | ✅ | Compatible |
| Notification | ✅ | ✅ | Compatible |
| RunCommand | ✅ | ✅ | Compatible |
| MPRIS | ❌ | ✅ | COSMIC only |
| FindMyPhone | ✅ | ✅ | Compatible |

### TLS Configuration
- Protocol: TLS 1.2+
- Certificate: Self-signed RSA 2048
- Validity: 10 years
- Fingerprint: SHA-256

## Testing Strategy

### Unit Tests
- Packet serialization/deserialization
- Certificate generation
- Protocol version negotiation
- Plugin packet handling

### Integration Tests
```
Android → COSMIC:
- Discovery broadcast → Detection
- Pairing request → Acceptance
- File send → Reception
- Ping → Pong

COSMIC → Android:
- Discovery broadcast → Detection
- Pairing request → Acceptance
- File send → Reception
- Clipboard sync → Update
```

### Network Tests
- Packet capture and analysis
- TLS handshake verification
- Payload transfer validation
- Timeout handling

## Interaction Guidelines

When ensuring compatibility:

1. **Test both directions** (Android→COSMIC and COSMIC→Android)
2. **Verify with real devices** whenever possible
3. **Check protocol versions** are handled correctly
4. **Document any differences** or limitations
5. **Keep reference implementations** in sync

## Example Commands

```bash
# Verify protocol compatibility
claude-code "Verify identity packet format matches between implementations"

# Test feature
claude-code "Test file sharing from Android to COSMIC Desktop"

# Debug issue
claude-code "Debug TLS handshake failure between Android and COSMIC"

# Update documentation
claude-code "Update protocol compatibility matrix with test results"

# Create test
claude-code "Create integration test for bidirectional clipboard sync"
```

## Validation Checklist

### Discovery Phase
- [ ] Android broadcasts identity correctly
- [ ] COSMIC detects Android device
- [ ] COSMIC broadcasts identity correctly
- [ ] Android detects COSMIC device
- [ ] Device info parsed correctly both ways

### Pairing Phase
- [ ] Android→COSMIC pairing request
- [ ] COSMIC→Android pairing request
- [ ] Certificate exchange successful
- [ ] Fingerprint verification works
- [ ] Pairing state persisted correctly

### Communication Phase
- [ ] Ping/pong works bidirectionally
- [ ] Battery status received correctly
- [ ] File sharing works both ways
- [ ] Clipboard syncs bidirectionally
- [ ] Notifications forwarded correctly
- [ ] Run commands execute properly
- [ ] Find phone triggers correctly

### Edge Cases
- [ ] Network disconnection recovery
- [ ] Certificate renewal handling
- [ ] Protocol version mismatch
- [ ] Partial packet reception
- [ ] Large file transfer
- [ ] Connection timeout handling

## Compatibility Matrix

### Network Requirements
- Same subnet (or Tailscale/VPN)
- Ports 1714-1764 TCP open
- Port 1716 UDP open
- Multicast enabled

### Android Requirements
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Permissions: Network, Storage, Notifications

### COSMIC Requirements
- NixOS or Linux with COSMIC DE
- Rust 1.70+
- libcosmic (latest)
- System DBus access

## Debugging Protocol Issues

### Capture Traffic
```bash
# On COSMIC
sudo tcpdump -i any port 1714 or port 1716 -w cosmic.pcap

# On Android (requires root)
adb shell tcpdump -i any port 1714 or port 1716 -w /sdcard/android.pcap
adb pull /sdcard/android.pcap

# Analyze
wireshark cosmic.pcap android.pcap
```

### Compare Packets
```bash
# Extract identity packets
tshark -r cosmic.pcap -Y "udp.port==1716" -T json > cosmic_identity.json
tshark -r android.pcap -Y "udp.port==1716" -T json > android_identity.json

# Compare
diff cosmic_identity.json android_identity.json
```

### Verify TLS
```bash
# Test from COSMIC
openssl s_client -connect android-ip:1714 -showcerts

# Check cipher suites
openssl s_client -connect android-ip:1714 -cipher 'ALL:!aNULL:!eNULL'
```

## Success Criteria

- [ ] All integration tests passing
- [ ] Real device testing successful
- [ ] Protocol documentation complete
- [ ] Compatibility matrix updated
- [ ] Edge cases handled
- [ ] Performance metrics met
- [ ] No packet loss or corruption
- [ ] Stable long-running connections
