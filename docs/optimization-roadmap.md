# Optimization Roadmap

This document tracks planned optimizations for COSMIC Connect Android and Desktop.

## Current Status

### ✅ Completed Optimizations

#### Issue #52: Connection Cycling Stability ✅
**Repository:** cosmic-applet-kdeconnect
**Date:** 2026-01-15
**Commit:** [d8d5ba2](https://github.com/olafkfreund/cosmic-applet-kdeconnect/commit/d8d5ba2)

**Problem:** Android clients reconnecting every ~5 seconds, causing cascade connection failures.

**Solution:** Socket replacement instead of connection rejection.

**Impact:**
- ✅ Eliminates cascade failures
- ✅ Maintains plugin functionality during reconnections
- ✅ Reduces "early eof" errors
- ✅ Matches official KDE Connect behavior

**Details:** See `docs/issue-52-fix-summary.md`

---

## Planned Optimizations

### Issue #63: Optimal Discovery and Pairing Architecture 🔮
**Repository:** cosmic-connect-android
**Status:** Planned (requires cosmic-connect-core foundation)
**Related:** [cosmic-applet-kdeconnect#53](https://github.com/olafkfreund/cosmic-applet-kdeconnect/issues/53)
**Priority:** P2-Medium
**Timeline:** Q2 2026 (8 weeks after Issue #50)

#### Overview

Major optimization of device discovery and pairing system using modern cryptography and smart discovery strategies.

#### Problems Addressed

Current implementations suffer from:
- ❌ Battery drain (5-10% hourly)
- ❌ Slow discovery (5-10 seconds)
- ❌ Pairing race conditions
- ❌ RSA-2048 performance (500ms key generation)
- ❌ Poor network change handling

#### Proposed Solutions

**1. Discovery Manager (3-Layer Strategy)**
```
Layer 1: Known IPs (<1s, fastest)
Layer 2: mDNS/DNS-SD (1-2s, reliable)
Layer 3: UDP broadcast (2-3s, fallback)
```

**2. Pairing Manager (7-State Machine)**
- Leader-follower distinction
- Idempotent operations
- Comprehensive timeouts

**3. Modern Cryptography**
- Ed25519: 5000x faster key generation
- X25519: 100x faster key exchange
- ChaCha20-Poly1305: Hardware-optimized encryption

#### Performance Targets

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| Discovery | 5-10s | <3s | 2x faster |
| Key gen | 500ms | <10ms | 50x faster |
| Pairing | 10-30s | <5s | 3-6x faster |
| Battery | 5-10%/hr | <2%/hr | 3-5x better |

#### Implementation Phases

**Phase 1: Core Infrastructure (Weeks 1-2)**
- Discovery Manager with state machine
- Exponential backoff logic
- Device cache with TTL

**Phase 2: Discovery Methods (Weeks 3-4)**
- Layer 1: Known IP connections
- Layer 2: mDNS/DNS-SD
- Layer 3: UDP broadcast fallback

**Phase 3: Pairing System (Weeks 5-6)**
- 7-state pairing manager
- Modern cryptography implementation
- RSA fallback for compatibility

**Phase 4: Android Integration (Weeks 7-8)**
- ConnectivityManager callbacks
- WorkManager battery-aware scheduling
- Testing and optimization

#### Dependencies

**Prerequisites (Must Complete First):**
- [x] Issue #44: Create cosmic-connect-core ✅
- [ ] Issue #45: Extract NetworkPacket
- [ ] Issue #46: Extract Discovery service
- [ ] Issue #47: TLS transport with rustls
- [ ] Issue #48: Certificate management
- [ ] Issue #50: FFI bindings

**After this issue:**
- Issue #54: Protocol-level keepalive
- Issue #55: Multi-interface support

#### Rust Crates Required

```toml
# Discovery
mdns-sd = "0.10"
socket2 = "0.5"

# Modern Cryptography
ed25519-dalek = "2.1"
x25519-dalek = "2.0"
chacha20poly1305 = "0.10"

# Async & Utilities
tokio = "1.35"
backoff = "0.4"
lru = "0.12"
```

#### Success Criteria

**Functional:**
- ✅ Pairing in <5 seconds (95th percentile)
- ✅ Discovery in <3 seconds (known devices)
- ✅ Zero pairing race conditions
- ✅ Network changes handled in <2 seconds

**Non-Functional:**
- ✅ Battery drain <2% hourly
- ✅ Memory usage <50MB
- ✅ Ed25519 keygen <10ms
- ✅ 99.9% pairing success rate

#### Compatibility

**Protocol:**
- ✅ Maintains KDE Connect protocol v7
- ✅ Graceful RSA fallback for old clients
- ✅ Compatible with official KDE Connect

**Android:**
- Minimum: Android 7.0 (API 24)
- Target: Android 14 (API 34)
- Optimizations: Android 10+ (ConnectivityManager API)

**Links:**
- **Issue:** [#63](https://github.com/olafkfreund/cosmic-connect-android/issues/63)
- **Full Proposal:** [cosmic-applet-kdeconnect#53](https://github.com/olafkfreund/cosmic-applet-kdeconnect/issues/53)

---

## Future Optimizations (TBD)

### Protocol-Level Keepalive
**Status:** Planned
**Dependencies:** Issue #63
**Impact:** Prevents connection cycling at protocol level

### Multi-Interface Support
**Status:** Planned
**Dependencies:** Issue #63
**Impact:** WiFi + Bluetooth simultaneously

### Connection Pooling
**Status:** Under consideration
**Impact:** Reduced latency for frequent operations

---

## Optimization Strategy

### 1. Foundation First (Q1 2026)
Complete cosmic-connect-core extraction:
- ✅ Issue #44: Project structure
- Issues #45-50: Core protocol implementation

### 2. Stability (Q1 2026)
- ✅ Issue #52: Connection cycling fix

### 3. Performance & Battery (Q2 2026)
- Issue #63: Optimal discovery and pairing

### 4. Advanced Features (Q3+ 2026)
- Protocol-level keepalive
- Multi-interface support
- Connection pooling

---

## Measuring Success

### Key Performance Indicators (KPIs)

**Discovery & Pairing:**
- Time to discover known devices
- Time to complete pairing
- Pairing success rate

**Battery & Resources:**
- Background battery drain (% per hour)
- Foreground battery drain (% per hour)
- Memory usage (MB)
- CPU usage (%)

**Stability:**
- Connection drops per hour
- Reconnection frequency
- Crash rate
- Error rate

**User Experience:**
- Time to first notification
- Plugin responsiveness
- File transfer speed
- Overall satisfaction rating

### Monitoring Tools

**Android:**
- Battery Historian
- Android Profiler
- Firebase Crashlytics
- Custom analytics events

**Desktop:**
- Rust profiling tools
- journalctl logs
- Custom metrics

---

## Testing Strategy

### Unit Tests
- State machine transitions
- Cryptography performance
- Exponential backoff logic
- Device cache behavior

### Integration Tests
- Android ↔ COSMIC pairing
- Network change handling
- Battery drain measurement
- Discovery speed benchmarks

### Real-World Testing
- Beta program with diverse devices
- Various network conditions
- Different Android versions
- Battery profiling on actual devices

---

## References

- **Hybrid Architecture:** `docs/ffi-design.md`
- **Rust Extraction Plan:** `docs/rust-extraction-plan.md`
- **Issue #52 Fix:** `docs/issue-52-fix-summary.md`
- **Applet Architecture:** `docs/applet-architecture.md`

---

**Last Updated:** 2026-01-15
**Status:** Active Planning
**Next Milestone:** Complete Issues #45-50 (cosmic-connect-core foundation)
