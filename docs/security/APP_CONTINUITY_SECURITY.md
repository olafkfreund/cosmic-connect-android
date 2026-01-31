# App Continuity Security Model

> **Version:** 1.0.0
> **Last Updated:** 2026-01-31
> **Security Contact:** COSMIC Connect Security Team
> **Status:** Implemented

## Overview

App Continuity enables users to seamlessly continue activities between their Android device and COSMIC Desktop. This document describes the security model, threat analysis, and mitigation strategies implemented to protect users from malicious URL handling and other attack vectors.

## Table of Contents

1. [Security Model Overview](#security-model-overview)
2. [URL Validation Rules](#url-validation-rules)
3. [User Confirmation Requirements](#user-confirmation-requirements)
4. [Threat Model](#threat-model)
5. [Mitigation Strategies](#mitigation-strategies)
6. [OWASP Compliance](#owasp-compliance)
7. [Security Checklist](#security-checklist)
8. [Incident Response](#incident-response)

---

## Security Model Overview

### Principle: Defense in Depth

The App Continuity feature implements multiple layers of security:

```
Layer 1: URL Validation (UrlValidator.kt)
    |
    v
Layer 2: User Confirmation (UI Prompt)
    |
    v
Layer 3: Desktop Validation (cosmic-connect-core)
    |
    v
Layer 4: Sandboxed Execution (xdg-open)
```

### Trust Boundaries

```
+-------------------+     Encrypted TLS     +-------------------+
|  Android Device   | <------------------> |   COSMIC Desktop  |
|                   |                      |                   |
|  [User Intent]    |                      |  [xdg-open]       |
|       |           |                      |       ^           |
|       v           |                      |       |           |
|  [UrlValidator]   |  --- NetworkPacket ---> [Validation] -+  |
|       |           |                      |       |           |
|  [User Confirm]   |                      |  [Sandbox]        |
+-------------------+                      +-------------------+
```

### Allowlist-Based Security

The URL validator uses an **allowlist** approach, blocking all schemes by default and only allowing explicitly approved ones. This follows the security principle of "deny by default, allow by exception."

---

## URL Validation Rules

### Allowed URL Schemes

| Scheme | Purpose | Example |
|--------|---------|---------|
| `http` | Web pages (non-secure) | `http://example.com` |
| `https` | Web pages (secure) | `https://example.com` |
| `mailto` | Email composition | `mailto:user@example.com` |
| `tel` | Phone calls | `tel:+1-555-123-4567` |
| `geo` | Geographic coordinates | `geo:37.7749,-122.4194` |
| `sms` | SMS composition | `sms:+15551234567` |
| `smsto` | SMS composition (alt) | `smsto:+15551234567` |

### Blocked URL Schemes

| Scheme | Reason | Attack Vector |
|--------|--------|---------------|
| `javascript` | XSS attacks | Script injection |
| `file` | Local file access | Information disclosure |
| `data` | Inline data execution | XSS via data URLs |
| `blob` | Binary data injection | Memory corruption |
| `about` | Browser internals | Information disclosure |
| `chrome` | Browser internals | Privilege escalation |
| `intent` | Android intent hijacking | App exploitation |
| `ftp` | Unencrypted transfer | MITM attacks |
| `ldap` | Directory access | LDAP injection |
| `gopher` | Legacy protocol | Protocol abuse |

### Validation Rules

#### 1. Length Validation
- **Maximum URL Length:** 2048 characters
- **Maximum Hostname Length:** 253 characters
- **Purpose:** Prevent buffer overflow and DoS attacks

#### 2. Character Validation
- **Null Bytes:** Blocked (`\u0000`)
- **Control Characters:** Blocked (except tab)
- **Purpose:** Prevent injection attacks

#### 3. Credential Detection
- **URLs with userinfo:** Blocked (`https://user:pass@example.com`)
- **Purpose:** Prevent credential leakage in logs/history

#### 4. Host Validation (HTTP/HTTPS only)

**Blocked Hosts:**
- `localhost`, `localhost.localdomain`
- `127.0.0.1`, `0.0.0.0`
- `::1`, `[::1]`

**Blocked IP Ranges:**
| Range | Name | CIDR |
|-------|------|------|
| 10.0.0.0 - 10.255.255.255 | Class A Private | 10.0.0.0/8 |
| 172.16.0.0 - 172.31.255.255 | Class B Private | 172.16.0.0/12 |
| 192.168.0.0 - 192.168.255.255 | Class C Private | 192.168.0.0/16 |
| 169.254.0.0 - 169.254.255.255 | Link-Local | 169.254.0.0/16 |
| 100.64.0.0 - 100.127.255.255 | Carrier NAT | 100.64.0.0/10 |
| 127.0.0.0 - 127.255.255.255 | Loopback | 127.0.0.0/8 |

**Blocked IPv6 Ranges:**
- `::1` - Loopback
- `fe80::/10` - Link-Local
- `fc00::/7` - Unique Local

**Cloud Metadata Endpoints:**
- `169.254.169.254` - AWS/Azure/GCP metadata
- `metadata.google.internal` - GCP metadata
- `169.254.170.2` - AWS ECS metadata

#### 5. Port Validation

**Blocked Ports:**
| Port | Service | Risk |
|------|---------|------|
| 22 | SSH | Remote access |
| 23 | Telnet | Unencrypted access |
| 25 | SMTP | Email relay abuse |
| 110 | POP3 | Email access |
| 143 | IMAP | Email access |
| 445 | SMB | File share access |
| 1433 | MSSQL | Database access |
| 1521 | Oracle | Database access |
| 3306 | MySQL | Database access |
| 3389 | RDP | Remote desktop |
| 5432 | PostgreSQL | Database access |
| 5900 | VNC | Remote desktop |
| 6379 | Redis | Cache access |
| 8080 | HTTP Proxy | Proxy bypass |
| 8443 | HTTPS Alt | Service access |
| 9200 | Elasticsearch | Search access |
| 27017 | MongoDB | Database access |

---

## User Confirmation Requirements

### Mandatory Confirmation

All URL open requests MUST display a user confirmation dialog before execution:

```
+--------------------------------------------------+
|              Open URL on Desktop?                |
+--------------------------------------------------+
|                                                  |
|  The URL below will be opened on your           |
|  COSMIC Desktop:                                 |
|                                                  |
|  https://example.com/page                        |
|                                                  |
|  [x] Don't ask again for this domain            |
|                                                  |
|         [Cancel]         [Open]                  |
+--------------------------------------------------+
```

### Confirmation Requirements

1. **Display Full URL** - Show the complete URL (truncated if > 100 chars)
2. **Highlight Domain** - Emphasize the target domain
3. **No Auto-Accept** - Never automatically accept URL open requests
4. **Timeout Defaults to Cancel** - If dialog times out, do not open
5. **Log All Decisions** - Record user choices for audit

### Domain Trust (Optional)

Users MAY choose to trust specific domains:
- Trust is per-domain, not per-URL
- Trust expires after 7 days of non-use
- Trust can be revoked in settings
- Trust is stored locally, never synced

---

## Threat Model

### Assets to Protect

1. **User Privacy** - Browsing history, personal data
2. **Device Security** - Prevent malware execution
3. **Network Security** - Prevent SSRF attacks
4. **Desktop Integrity** - Prevent desktop compromise

### Threat Actors

| Actor | Capability | Motivation |
|-------|------------|------------|
| Malicious App | High | Data theft, malware distribution |
| MITM Attacker | Medium | Traffic interception |
| Phishing Site | Medium | Credential theft |
| Rogue Network | Low-Medium | Traffic manipulation |

### Attack Vectors

#### A1: Malicious URL Injection

**Attack:** Attacker injects malicious URL via compromised app
**Mitigation:** URL validation + user confirmation
**Residual Risk:** Low

#### A2: SSRF via Internal URLs

**Attack:** Attacker uses internal URLs to probe network
**Mitigation:** Block private IPs, localhost, metadata endpoints
**Residual Risk:** Very Low

#### A3: Credential Theft via URL

**Attack:** Attacker embeds credentials in URL for logging
**Mitigation:** Block URLs with userinfo
**Residual Risk:** Very Low

#### A4: XSS via javascript: URLs

**Attack:** Attacker uses javascript: scheme for code execution
**Mitigation:** Scheme allowlist (javascript blocked)
**Residual Risk:** Very Low

#### A5: Local File Access

**Attack:** Attacker uses file:// to access local files
**Mitigation:** Scheme allowlist (file blocked)
**Residual Risk:** Very Low

#### A6: Null Byte Injection

**Attack:** Attacker uses null bytes to bypass validation
**Mitigation:** Explicit null byte check
**Residual Risk:** Very Low

#### A7: Bypass via IP Encoding

**Attack:** Attacker uses octal/hex/decimal IP representation
**Mitigation:** Parse and validate actual IP addresses
**Residual Risk:** Low

---

## Mitigation Strategies

### M1: Input Validation

```kotlin
// Always validate before processing
val result = UrlValidator.validate(url)
when (result) {
    is ValidationResult.Valid -> proceed(url)
    is ValidationResult.Invalid -> {
        Log.w(TAG, "Blocked URL: ${result.reason}")
        showError(result.reason)
    }
}
```

### M2: User Confirmation

```kotlin
// Always require explicit user consent
showConfirmationDialog(
    url = url,
    onConfirm = { openOnDesktop(url) },
    onCancel = { /* log cancellation */ }
)
```

### M3: Secure Transmission

- All packets encrypted via TLS 1.3
- Certificate pinning for known devices
- No URL data in plain text

### M4: Logging and Monitoring

```kotlin
// Log all URL operations for audit
SecurityAuditLog.log(
    event = "URL_OPEN_REQUEST",
    url = url.hashForLogging(),  // Don't log full URL
    result = validationResult,
    userDecision = confirmed
)
```

### M5: Rate Limiting

- Maximum 10 URL open requests per minute
- Exponential backoff on repeated failures
- Alert user on suspicious activity

---

## OWASP Compliance

### OWASP ASVS v4.0 Compliance

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| V5.1.3 - URL Validation | PASS | UrlValidator.kt |
| V5.1.4 - Allowlist Approach | PASS | ALLOWED_SCHEMES set |
| V5.2.5 - No Credentials in URLs | PASS | userInfo check |
| V5.3.4 - SSRF Prevention | PASS | Private IP blocking |
| V13.1.3 - CSRF Prevention | PASS | User confirmation |

### CWE References

| CWE ID | Name | Mitigation |
|--------|------|------------|
| CWE-20 | Improper Input Validation | UrlValidator |
| CWE-116 | Improper Encoding | Control char check |
| CWE-158 | Null Byte Injection | Null byte check |
| CWE-400 | Resource Exhaustion | Length limits |
| CWE-522 | Credential Exposure | userInfo block |
| CWE-601 | Open Redirect | Scheme allowlist |
| CWE-918 | SSRF | IP validation |

---

## Security Checklist

### Implementation Checklist

- [x] Scheme allowlist enforced
- [x] No credentials allowed in URLs
- [x] No localhost/loopback addresses
- [x] No private IP addresses (10.x, 172.16-31.x, 192.168.x)
- [x] No cloud metadata endpoints
- [x] Proper URL parsing using java.net.URI (not regex)
- [x] Null byte protection
- [x] Control character protection
- [x] Length limits enforced
- [x] User confirmation required
- [x] Blocked port validation
- [x] IPv6 localhost detection
- [x] Security logging implemented

### Testing Checklist

- [x] Valid URL acceptance tests
- [x] Blocked scheme tests
- [x] Credential detection tests
- [x] SSRF prevention tests
- [x] Injection attack tests
- [x] Length limit tests
- [x] Edge case tests
- [x] Bypass attempt tests

### Deployment Checklist

- [ ] Security review completed
- [ ] Penetration testing performed
- [ ] Documentation updated
- [ ] User education materials created
- [ ] Incident response plan updated

---

## Incident Response

### Security Issue Reporting

Report security issues to: security@cosmic-connect.example.com

**Do NOT:**
- Disclose vulnerabilities publicly before fix
- Attempt to exploit vulnerabilities
- Access other users' data

**DO:**
- Provide detailed reproduction steps
- Include version information
- Wait for acknowledgment before disclosure

### Response SLA

| Severity | Response Time | Fix Time |
|----------|---------------|----------|
| Critical | 4 hours | 24 hours |
| High | 24 hours | 7 days |
| Medium | 7 days | 30 days |
| Low | 30 days | 90 days |

---

## Appendix A: Security Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-01-31 | Use allowlist for schemes | Safer than blocklist approach |
| 2026-01-31 | Block all private IPs | SSRF prevention |
| 2026-01-31 | Require user confirmation | Prevent silent exploitation |
| 2026-01-31 | 2048 char URL limit | Balance usability and security |

## Appendix B: References

1. [OWASP SSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html)
2. [OWASP ASVS v4.0](https://owasp.org/www-project-application-security-verification-standard/)
3. [CWE-918: SSRF](https://cwe.mitre.org/data/definitions/918.html)
4. [RFC 3986: URI Generic Syntax](https://tools.ietf.org/html/rfc3986)
5. [AWS SSRF Protection](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instancedata-data-categories.html)

---

**Document Control:**
- **Author:** COSMIC Connect Security Team
- **Reviewers:** [Pending]
- **Approval:** [Pending]
- **Classification:** Public

*This document is part of the COSMIC Connect security documentation.*
