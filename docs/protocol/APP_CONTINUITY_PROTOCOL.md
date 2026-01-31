# App Continuity Protocol Specification

**Status**: Planning
**Created**: 2025-01-31
**Issue**: #114 (Part of #112 - App Continuity)
**Version**: 1.0.0

## Overview

The App Continuity protocol enables seamless content sharing between Android and COSMIC Desktop through bidirectional content requests and file transfers. This protocol prioritizes user security by requiring explicit confirmation before opening any content, preventing silent unauthorized access to applications and files.

### Purpose

- Enable users to open URLs, files, and content from Desktop on Android and vice versa
- Provide seamless protocol for clipboard continuity and cross-platform app launching
- Maintain security through mandatory user confirmation for all operations
- Support both local and remote file transfers

### Key Principles

1. **User-First Security**: Never open content without user confirmation
2. **Bidirectional**: Both Android and Desktop can request content opens
3. **Protocol Simplicity**: Minimal packet overhead, leveraging existing file transfer
4. **Graceful Degradation**: Missing apps result in helpful error messages
5. **Standardized Schemes**: Only support verified, safe URI schemes

### Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                     Android Application                       │
│  (URL handler, File manager, Content consumers)              │
└────────────────┬─────────────────────────────────────────────┘
                 │
                 ▼
        ┌────────────────┐
        │  App Continuity│
        │   Plugin       │
        │  (Validator,   │
        │   Router)      │
        └────────┬───────┘
                 │
     ┌───────────┼───────────┐
     │           │           │
     ▼           ▼           ▼
┌────────┐  ┌────────┐  ┌──────────┐
│ Packet │  │ User   │  │ File     │
│ Handler│  │Confirm.│  │ Transfer │
│        │  │ Dialog │  │ Engine   │
└────────┘  └────────┘  └──────────┘
     │           │           │
     └───────────┼───────────┘
                 │
                 ▼
        ┌────────────────┐
        │   TLS Network  │
        │   (COSMIC      │
        │   Protocol)    │
        └────────────────┘
                 │
                 ▼
        ┌────────────────────────┐
        │ COSMIC Desktop Applet  │
        │ or other receivers     │
        └────────────────────────┘
```

---

## Packet Types

### Overview

App Continuity uses three packet types following the COSMIC Connect naming convention:

- `cconnect.open.capability` - Advertised capabilities when devices connect
- `cconnect.open.request` - Request to open content
- `cconnect.open.response` - Response to open request

### 1. cconnect.open.capability

**Direction**: Bidirectional (sent on connection establishment)

**Purpose**: Advertise device's support for opening content and supported content types.

**Structure**:
```json
{
  "id": 1704067200000,
  "type": "cconnect.open.capability",
  "body": {
    "supportedUrlSchemes": [
      "http",
      "https",
      "mailto",
      "tel",
      "geo",
      "sms"
    ],
    "supportedMimeTypes": [
      "text/plain",
      "text/html",
      "application/pdf",
      "image/*",
      "video/*",
      "audio/*"
    ],
    "maxUrlLength": 2048,
    "maxFileSize": 5368709120,
    "supportsFileTransfer": true,
    "protocolVersion": 1
  }
}
```

**Field Definitions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| supportedUrlSchemes | Array[String] | Yes | URI schemes the device can handle (http, https, mailto, tel, geo, sms) |
| supportedMimeTypes | Array[String] | Yes | MIME types the device can open (including wildcards) |
| maxUrlLength | Integer | Yes | Maximum URL length in characters (default: 2048) |
| maxFileSize | Integer | Yes | Maximum file size in bytes (default: 5GB) |
| supportsFileTransfer | Boolean | Yes | Whether device supports receiving files before opening |
| protocolVersion | Integer | Yes | App Continuity protocol version (currently 1) |

**Example (Android)**:
```json
{
  "id": 1704067200000,
  "type": "cconnect.open.capability",
  "body": {
    "supportedUrlSchemes": [
      "http",
      "https",
      "mailto",
      "tel",
      "geo",
      "sms",
      "market",
      "intent"
    ],
    "supportedMimeTypes": [
      "text/plain",
      "text/html",
      "application/pdf",
      "image/*",
      "video/*",
      "audio/*",
      "application/json",
      "application/x-tar",
      "application/zip"
    ],
    "maxUrlLength": 2048,
    "maxFileSize": 5368709120,
    "supportsFileTransfer": true,
    "protocolVersion": 1
  }
}
```

---

### 2. cconnect.open.request

**Direction**: Bidirectional (request to open content)

**Purpose**: Request the other device to open a URL or file.

**Structure**:
```json
{
  "id": 1704067201000,
  "type": "cconnect.open.request",
  "body": {
    "requestId": "req-12345-67890",
    "contentType": "url",
    "content": "https://example.com/article",
    "mimeType": "text/html",
    "displayName": "Example Article",
    "sourceApplication": "Chrome",
    "userConfirmed": true,
    "fileTransferId": null
  },
  "payloadSize": null
}
```

**URL Request** (No file transfer):
```json
{
  "id": 1704067201000,
  "type": "cconnect.open.request",
  "body": {
    "requestId": "req-12345-67890",
    "contentType": "url",
    "content": "https://example.com/document.pdf",
    "mimeType": "application/pdf",
    "displayName": "Meeting Notes",
    "sourceApplication": "Chrome",
    "userConfirmed": true,
    "fileTransferId": null
  }
}
```

**File Transfer Request** (With payload):
```json
{
  "id": 1704067202000,
  "type": "cconnect.open.request",
  "body": {
    "requestId": "req-12345-67891",
    "contentType": "file",
    "content": "document.pdf",
    "mimeType": "application/pdf",
    "displayName": "Report.pdf",
    "sourceApplication": "File Manager",
    "userConfirmed": true,
    "fileTransferId": "transfer-file-001",
    "filePath": "/home/user/Documents/report.pdf",
    "fileSize": 1048576,
    "checksum": "sha256:abc123..."
  },
  "payloadSize": 1048576,
  "payloadTransferInfo": {
    "port": 1739,
    "key": "TLS-key-identifier"
  }
}
```

**Field Definitions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| requestId | String | Yes | Unique request identifier (UUID or request ID) |
| contentType | String (url/file) | Yes | Type of content: "url" or "file" |
| content | String | Yes | URL string or filename |
| mimeType | String | Yes | MIME type (e.g., "text/html", "application/pdf") |
| displayName | String | Yes | Human-readable name for user confirmation |
| sourceApplication | String | No | Name of app originating the request |
| userConfirmed | Boolean | Yes | Whether user explicitly confirmed this request |
| fileTransferId | String | Conditional | File transfer identifier (required if contentType="file") |
| filePath | String | Conditional | Original file path (for reference, not used for transfer) |
| fileSize | Integer | Conditional | File size in bytes (required if contentType="file") |
| checksum | String | No | SHA256 checksum of file content (for verification) |

---

### 3. cconnect.open.response

**Direction**: Bidirectional (response to open request)

**Purpose**: Confirm or reject an open request, reporting success or error.

**Structure**:
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345-67890",
    "status": "success",
    "statusCode": 200,
    "message": "Content opened successfully",
    "openedWith": "Chrome",
    "timestamp": 1704067203000
  }
}
```

**Success Response**:
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345-67890",
    "status": "success",
    "statusCode": 200,
    "message": "Content opened successfully in Chrome",
    "openedWith": "Chrome",
    "timestamp": 1704067203000
  }
}
```

**User Rejected Response**:
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345-67890",
    "status": "rejected",
    "statusCode": 403,
    "message": "User declined to open content",
    "reason": "user_rejected",
    "timestamp": 1704067203000
  }
}
```

**Handler Not Available Response**:
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345-67890",
    "status": "error",
    "statusCode": 404,
    "message": "No handler available for MIME type application/pdf",
    "reason": "handler_not_found",
    "suggestedAction": "install_app",
    "suggestedAppName": "Adobe Reader",
    "timestamp": 1704067203000
  }
}
```

**Invalid Content Response**:
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345-67890",
    "status": "error",
    "statusCode": 400,
    "message": "Invalid URL format",
    "reason": "invalid_content",
    "details": "URL contains forbidden characters",
    "timestamp": 1704067203000
  }
}
```

**Field Definitions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| requestId | String | Yes | Echo of the request ID being responded to |
| status | String | Yes | "success", "rejected", or "error" |
| statusCode | Integer | Yes | HTTP-like status code (200, 400, 403, 404, 500) |
| message | String | Yes | Human-readable status message |
| openedWith | String | No | Name of app that opened the content (success only) |
| reason | String | No | Error reason code (required for non-success) |
| suggestedAction | String | No | Suggested action for user (install_app, etc.) |
| suggestedAppName | String | No | Name of suggested app to install |
| details | String | No | Additional error details |
| timestamp | Integer | Yes | Response timestamp in milliseconds |

**Status Code Reference**:

| Code | Status | Meaning |
|------|--------|---------|
| 200 | success | Content opened successfully |
| 400 | error | Bad request (invalid URL, unsupported scheme, etc.) |
| 403 | rejected | User declined to open content |
| 404 | error | Handler/app not found for content type |
| 413 | error | Content too large |
| 415 | error | Unsupported media type |
| 500 | error | Internal error |

---

## Sequence Diagrams

### Android → Desktop: URL Opening

```mermaid
sequenceDiagram
    participant User as User
    participant Android as Android App
    participant AndroidCC as Android CC Plugin
    participant Network as Network (TLS)
    participant Desktop as COSMIC Desktop
    participant DesktopCC as Desktop CC Plugin
    participant Browser as Browser/App

    User->>Android: Share URL or long-press link
    Android->>AndroidCC: Send URL request
    AndroidCC->>AndroidCC: Validate URL scheme & format
    AndroidCC->>User: Show confirmation dialog
    User->>AndroidCC: Confirm (Accept/Reject)
    alt User Accepts
        AndroidCC->>Network: Send cconnect.open.request
        Network->>DesktopCC: Receive cconnect.open.request
        DesktopCC->>DesktopCC: Validate URL & scheme
        DesktopCC->>DesktopCC: Check capability (http/https)
        DesktopCC->>DesktopCC: Select handler (browser)
        DesktopCC->>Browser: Launch with URL
        DesktopCC->>Network: Send cconnect.open.response (success)
        Network->>AndroidCC: Receive response
        AndroidCC->>User: Notify success
    else User Rejects
        AndroidCC->>Network: Send cconnect.open.request (userConfirmed=false)
        AndroidCC->>User: Operation cancelled
    end
```

### Desktop → Android: URL Opening

```mermaid
sequenceDiagram
    participant User as User
    participant Browser as Browser
    participant DesktopCC as Desktop CC Plugin
    participant Network as Network (TLS)
    participant AndroidCC as Android CC Plugin
    participant AndroidApp as Android App
    participant System as Android System

    User->>Browser: Right-click link, "Open on phone"
    Browser->>DesktopCC: Send URL to Android
    DesktopCC->>DesktopCC: Validate URL scheme & format
    DesktopCC->>Network: Send cconnect.open.request
    Network->>AndroidCC: Receive cconnect.open.request
    AndroidCC->>AndroidCC: Validate URL & scheme
    AndroidCC->>AndroidCC: Query available handlers
    AndroidCC->>User: Show confirmation + app selector
    User->>AndroidCC: Select app & confirm (Accept/Reject)
    alt User Accepts
        AndroidCC->>System: Create Intent with URL
        System->>AndroidApp: Launch app with URL
        AndroidApp->>System: App launched successfully
        AndroidCC->>Network: Send cconnect.open.response (success)
        Network->>DesktopCC: Receive response
        DesktopCC->>User: Notify "Content opened on Android"
    else User Rejects
        AndroidCC->>Network: Send cconnect.open.response (rejected)
        Network->>DesktopCC: Receive response
        DesktopCC->>User: Notify user rejected
    end
```

### File Transfer + Open: Desktop → Android

```mermaid
sequenceDiagram
    participant User as User
    participant Desktop as COSMIC Desktop
    participant DesktopCC as Desktop CC Plugin
    participant Network as Network (TLS)
    participant AndroidCC as Android CC Plugin
    participant Storage as Android Storage
    participant AndroidApp as Android App

    User->>Desktop: Right-click file, "Open on phone"
    Desktop->>DesktopCC: Send file open request
    DesktopCC->>DesktopCC: Validate file & MIME type
    DesktopCC->>AndroidCC: Send cconnect.open.request (contentType=file)
    AndroidCC->>AndroidCC: Validate MIME type
    AndroidCC->>AndroidCC: Check available handlers
    AndroidCC->>User: Show confirmation dialog
    User->>AndroidCC: Confirm (Accept/Reject)
    alt User Accepts
        DesktopCC->>Network: Start file transfer (cconnect.fileTransfer.send)
        Network->>AndroidCC: Receive file in chunks
        AndroidCC->>Storage: Write file to temp location
        AndroidCC->>Storage: Verify checksum
        AndroidCC->>System: Create Intent with file:// URI
        System->>AndroidApp: Launch app with file
        AndroidApp->>Storage: Read file
        AndroidApp->>User: Display file content
        AndroidCC->>Network: Send cconnect.open.response (success)
        Network->>DesktopCC: Receive response
        DesktopCC->>User: Notify "File opened on Android"
        AndroidCC->>Storage: (After app closes) Clean up temp file
    else User Rejects
        AndroidCC->>Network: Send cconnect.open.response (rejected)
        Network->>DesktopCC: Receive response
        DesktopCC->>User: Notify user rejected
    end
```

### Error Handling Flow: Unsupported MIME Type

```mermaid
sequenceDiagram
    participant Sender as Sender Device
    participant SenderCC as Sender CC Plugin
    participant Network as Network (TLS)
    participant ReceiverCC as Receiver CC Plugin
    participant System as Receiver System
    participant User as User

    Sender->>SenderCC: User initiates content open
    SenderCC->>SenderCC: Validate content
    SenderCC->>Network: Send cconnect.open.request
    Network->>ReceiverCC: Receive request
    ReceiverCC->>ReceiverCC: Parse MIME type
    ReceiverCC->>System: Query available handlers
    System->>ReceiverCC: No handler for MIME type
    ReceiverCC->>ReceiverCC: Check for suggested apps
    alt Suggested app available
        ReceiverCC->>Network: Send response (handler_not_found, suggestedAppName)
        Network->>SenderCC: Receive response
        SenderCC->>User: Notify + offer install link
    else No suggestion
        ReceiverCC->>Network: Send response (handler_not_found)
        Network->>SenderCC: Receive response
        SenderCC->>User: Notify handler not available
    end
```

---

## URL Scheme Allowlist

### Supported Schemes

These URL schemes are **permitted** and safe to open:

| Scheme | Purpose | Example | Validation |
|--------|---------|---------|-----------|
| http | Web pages (non-secure) | `http://example.com` | Must be well-formed URL |
| https | Web pages (secure) | `https://example.com` | Must be well-formed URL |
| mailto | Email composition | `mailto:user@example.com` | Valid email format |
| tel | Phone calls | `tel:+1234567890` | Valid phone number format |
| geo | Maps/location | `geo:37.786971,-122.399677` | Valid coordinates |
| sms | Text messages | `sms:+1234567890?body=text` | Valid phone number |

### Rejected Schemes

These schemes are **strictly forbidden**:

| Scheme | Reason | Example |
|--------|--------|---------|
| file | Direct file access (use file transfer instead) | `file:///etc/passwd` |
| javascript | Code injection vulnerability | `javascript:alert('xss')` |
| data | Embedded scripts and exploits | `data:text/html,<script>alert('xss')</script>` |
| ssh | Unauthorized remote access | `ssh://server.com` |
| ftp | Unencrypted file transfer | `ftp://example.com/file` |
| intent | Android implicit intents (dangerous) | `intent://example.com#Intent;` |

### Android-Specific Additional Schemes

Android may support additional schemes that are safe:

| Scheme | Purpose | Notes |
|--------|---------|-------|
| market | Google Play Store | Only allows app store links, validated against package name |
| content | Android content providers | Limited to safe providers (contacts, calendar, notes) |
| smsto | SMS with pre-filled number | Validated same as tel scheme |

### Validation Rules

1. **Scheme Whitelist Check**: Verify scheme is in allowlist before opening
2. **URL Format Validation**: Use proper URL parser (not regex)
3. **Length Validation**: Enforce max 2048 character limit
4. **Localhost Rejection**: Reject 127.0.0.1, localhost, ::1
5. **Private IP Rejection**: Reject 10.x.x.x, 172.16.x.x, 192.168.x.x ranges
6. **No Credentials in URL**: Reject URLs containing user:password@ format
7. **No Double Encoding**: Reject URLs with double-encoded characters

---

## Security Validation Requirements

### URL Validation

All URLs must be validated before opening using proper URL parsing libraries, not regex patterns.

#### Android Implementation (Kotlin)

```kotlin
fun validateUrl(url: String): ValidationResult {
    // 1. Length check
    if (url.length > 2048) {
        return ValidationResult.TooLong
    }

    // 2. Parse URL
    val uri = try {
        Uri.parse(url)
    } catch (e: Exception) {
        return ValidationResult.InvalidFormat
    }

    // 3. Scheme validation
    val scheme = uri.scheme?.lowercase() ?: ""
    if (scheme !in ALLOWED_SCHEMES) {
        return ValidationResult.UnsupportedScheme(scheme)
    }

    // 4. Reject localhost and private IPs
    val host = uri.host ?: return ValidationResult.MissingHost
    if (isPrivateOrLoopback(host)) {
        return ValidationResult.PrivateIP
    }

    // 5. No embedded credentials
    if (uri.userInfo != null) {
        return ValidationResult.ContainsCredentials
    }

    // 6. No double encoding
    if (url.contains("%25")) { // %25 is encoded %
        return ValidationResult.DoubleEncoded
    }

    return ValidationResult.Valid
}

fun isPrivateOrLoopback(host: String): Boolean {
    // Check for localhost, 127.0.0.1, ::1
    if (host in listOf("localhost", "127.0.0.1", "::1")) {
        return true
    }

    // Check for private IP ranges
    val ipPattern = Regex("""^(\d+)\.(\d+)\.(\d+)\.(\d+)$""")
    val match = ipPattern.find(host)
    if (match != null) {
        val (a, b, c, d) = match.destructured.toList().map { it.toInt() }
        // 10.0.0.0/8
        if (a == 10) return true
        // 172.16.0.0/12
        if (a == 172 && b in 16..31) return true
        // 192.168.0.0/16
        if (a == 192 && b == 168) return true
    }

    return false
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    object TooLong : ValidationResult()
    object InvalidFormat : ValidationResult()
    object MissingHost : ValidationResult()
    object PrivateIP : ValidationResult()
    object ContainsCredentials : ValidationResult()
    object DoubleEncoded : ValidationResult()
    data class UnsupportedScheme(val scheme: String) : ValidationResult()
}

private val ALLOWED_SCHEMES = setOf(
    "http", "https", "mailto", "tel", "geo", "sms",
    "market", "smsto"
)
```

#### COSMIC Desktop Implementation (Rust)

```rust
use url::Url;
use std::net::IpAddr;

pub fn validate_url(url: &str) -> ValidationResult {
    // 1. Length check
    if url.len() > 2048 {
        return ValidationResult::TooLong;
    }

    // 2. Parse URL
    let parsed_url = match Url::parse(url) {
        Ok(u) => u,
        Err(_) => return ValidationResult::InvalidFormat,
    };

    // 3. Scheme validation
    let scheme = parsed_url.scheme();
    if !ALLOWED_SCHEMES.contains(&scheme) {
        return ValidationResult::UnsupportedScheme(scheme.to_string());
    }

    // 4. Check for localhost/private IPs
    if let Some(host) = parsed_url.host_str() {
        if is_private_or_loopback(host) {
            return ValidationResult::PrivateIP;
        }
    } else if scheme != "mailto" && scheme != "tel" && scheme != "sms" {
        return ValidationResult::MissingHost;
    }

    // 5. No embedded credentials
    if parsed_url.username() != "" || parsed_url.password().is_some() {
        return ValidationResult::ContainsCredentials;
    }

    // 6. Check for double encoding
    if url.contains("%25") {
        return ValidationResult::DoubleEncoded;
    }

    ValidationResult::Valid
}

fn is_private_or_loopback(host: &str) -> bool {
    // Localhost
    if host == "localhost" {
        return true;
    }

    // Parse as IP address
    match host.parse::<IpAddr>() {
        Ok(ip) => {
            ip.is_loopback() ||
            ip.is_private() ||
            ip.is_link_local()
        }
        Err(_) => false,
    }
}

#[derive(Debug)]
pub enum ValidationResult {
    Valid,
    TooLong,
    InvalidFormat,
    MissingHost,
    PrivateIP,
    ContainsCredentials,
    DoubleEncoded,
    UnsupportedScheme(String),
}

const ALLOWED_SCHEMES: &[&str] = &[
    "http", "https", "mailto", "tel", "geo", "sms",
];
```

### User Confirmation Requirements

#### Dialog Requirements

All open requests must show a user confirmation dialog containing:

1. **Source Information**
   - Device name that sent the request
   - Application name (if available)
   - Timestamp

2. **Content Information**
   - Display name (sanitized, max 100 chars)
   - MIME type icon
   - Content preview (first 200 chars for URLs)
   - File size (for file transfers)

3. **Action Options**
   - Accept button (default)
   - Reject button
   - Additional handler selection (if multiple apps available)

4. **Security Indicators**
   - HTTPS indicator for secure URLs
   - Warning icon for non-https URLs
   - File type icon with clear labeling

#### Dialog Implementation Pattern (Android)

```kotlin
fun showOpenConfirmationDialog(
    request: OpenRequest,
    handlers: List<AppHandler>,
    onAccept: (handler: AppHandler) -> Unit,
    onReject: () -> Unit
) {
    val dialog = AlertDialog.Builder(context)
        .setTitle("Open content from ${request.sourceApplication}")
        .setView(createDialogView(request, handlers))
        .setPositiveButton("Accept") { _, _ ->
            val selectedHandler = handlers[selectedIndex]
            onAccept(selectedHandler)
        }
        .setNegativeButton("Reject") { _, _ ->
            onReject()
        }
        .setCancelable(false)
        .show()

    // Dialog content shows:
    // - Source: "Device Name - Chrome"
    // - Content: "https://example.com/article" [HTTPS icon]
    // - Type: "Web page"
    // - Preview: "Example Article | Example Domain"
    // - Handler selector: "Open with: [Chrome ▼]"
}
```

### Auto-Open Trusted Content Settings

Devices may optionally support auto-opening trusted content without confirmation.

**Requirements for Auto-Open**:
1. Must be explicitly enabled in device settings (default: disabled)
2. Must only apply to explicitly trusted devices
3. Must only apply to whitelisted MIME types (text/plain, image/*)
4. Must only apply to HTTPS URLs (never HTTP)
5. Must log all auto-opened content for audit
6. Must allow per-device revocation

**Implementation Pattern**:
```kotlin
data class AutoOpenPolicy(
    val enabled: Boolean = false,
    val trustedDeviceIds: List<String> = emptyList(),
    val allowedMimeTypes: List<String> = listOf(
        "text/plain", "text/html", "image/*"
    ),
    val requireHttps: Boolean = true,
    val auditLog: List<AutoOpenEntry> = emptyList()
)

data class AutoOpenEntry(
    val timestamp: Long,
    val deviceId: String,
    val deviceName: String,
    val contentType: String,
    val mimeType: String,
    val displayName: String
)

fun shouldAutoOpen(
    request: OpenRequest,
    policy: AutoOpenPolicy
): Boolean {
    if (!policy.enabled) return false

    // Only https
    if (!request.content.startsWith("https://")) return false

    // Device is trusted
    if (request.sourceDeviceId !in policy.trustedDeviceIds) return false

    // MIME type allowed
    if (!isMimeTypeAllowed(request.mimeType, policy.allowedMimeTypes)) {
        return false
    }

    return true
}
```

---

## MIME Type Handling

### MIME Type Determination

MIME types are determined from file extensions and content analysis:

1. **File-Based Detection**: Use file extension mapping
2. **Content Analysis**: For URLs without clear extensions
3. **Explicit Header**: Use mimeType field from request packet
4. **Fallback**: application/octet-stream for unknown types

#### MIME Type Mapping Table

```
Text Files:
  .txt → text/plain
  .html, .htm → text/html
  .xml → application/xml
  .json → application/json
  .csv → text/csv
  .md → text/markdown

Documents:
  .pdf → application/pdf
  .doc, .docx → application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document
  .xls, .xlsx → application/vnd.ms-excel
  .ppt, .pptx → application/vnd.ms-powerpoint

Images:
  .jpg, .jpeg → image/jpeg
  .png → image/png
  .gif → image/gif
  .webp → image/webp
  .svg → image/svg+xml

Audio:
  .mp3 → audio/mpeg
  .wav → audio/wav
  .m4a → audio/mp4
  .ogg → audio/ogg

Video:
  .mp4 → video/mp4
  .webm → video/webm
  .mkv → video/x-matroska
  .mov → video/quicktime

Archives:
  .zip → application/zip
  .tar → application/x-tar
  .gz → application/gzip
  .7z → application/x-7z-compressed
```

### Default Handlers

For each MIME type category, devices maintain default handlers:

#### Android Handler Resolution

```kotlin
fun getDefaultHandlerForMimeType(mimeType: String): AppHandler? {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setType(mimeType)
    }

    val resolveInfo = context.packageManager.resolveActivity(
        intent,
        PackageManager.MATCH_DEFAULT_ONLY
    ) ?: return null

    return AppHandler(
        packageName = resolveInfo.activityInfo.packageName,
        activityName = resolveInfo.activityInfo.name,
        label = resolveInfo.loadLabel(context.packageManager).toString(),
        icon = resolveInfo.loadIcon(context.packageManager)
    )
}

fun getHandlersForMimeType(mimeType: String): List<AppHandler> {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setType(mimeType)
    }

    return context.packageManager
        .queryIntentActivities(intent, 0)
        .map { resolveInfo ->
            AppHandler(
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                label = resolveInfo.loadLabel(context.packageManager).toString(),
                icon = resolveInfo.loadIcon(context.packageManager)
            )
        }
}
```

#### COSMIC Desktop Handler Resolution (DBus)

```rust
async fn get_default_handler(mime_type: &str) -> Result<AppHandler> {
    let proxy = org_freedesktop_portal_desktop::DesktopPortal::new().await?;

    let (default, all_handlers) = proxy
        .get_default_handler(mime_type)
        .await?;

    Ok(AppHandler {
        id: default,
        mime_types: vec![mime_type.to_string()],
        name: fetch_app_name(&default).await?,
        icon: fetch_app_icon(&default).await?,
    })
}
```

---

## File Transfer Integration

### Overview

Files are transferred using the existing COSMIC Connect file transfer mechanism before the app opens them.

### Flow

1. **Request Sent**: Sender includes file metadata (size, MIME type, checksum)
2. **Transfer Initiated**: Receiver acknowledges and prepares storage
3. **File Transfer**: Data transmitted via TLS socket
4. **Verification**: Checksum verified on receipt
5. **File Saved**: Temporary location with unique name
6. **Opening**: App launched with file URI
7. **Cleanup**: Temporary file deleted after app closes

### Packet Structure for File Transfer

#### Initial Request (with file transfer)

```json
{
  "id": 1704067202000,
  "type": "cconnect.open.request",
  "body": {
    "requestId": "req-12345-67891",
    "contentType": "file",
    "content": "document.pdf",
    "mimeType": "application/pdf",
    "displayName": "Report.pdf",
    "sourceApplication": "File Manager",
    "userConfirmed": true,
    "fileTransferId": "transfer-file-001",
    "fileSize": 1048576,
    "checksum": "sha256:abc123def456..."
  },
  "payloadSize": 1048576,
  "payloadTransferInfo": {
    "port": 1739,
    "key": "TLS-key-identifier",
    "algorithm": "AES-256-GCM"
  }
}
```

### Temporary File Management

#### Android Temp File Handling

```kotlin
fun setupFileTransferReceiver(
    request: OpenRequest,
    onFileReceived: (File) -> Unit
): FileTransferReceiver {
    // 1. Create temp file
    val tempFile = File(context.cacheDir, "open_${request.fileTransferId}")

    // 2. Set up transfer receiver
    return FileTransferReceiver(
        targetFile = tempFile,
        expectedSize = request.fileSize,
        expectedChecksum = request.checksum,
        onComplete = { file ->
            // 3. Verify checksum
            val actualChecksum = calculateSha256(file)
            if (actualChecksum != request.checksum) {
                file.delete()
                throw ChecksumMismatchException()
            }

            // 4. Call callback
            onFileReceived(file)
        },
        onError = { error ->
            tempFile.delete()
            throw error
        }
    )
}

fun openReceivedFile(file: File, mimeType: String) {
    // Create content URI for cache file
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    // Create Intent
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // Launch
    context.startActivity(intent)

    // Schedule cleanup (delete after app closes)
    // Note: File is deleted when cache is cleared or after timeout
    scheduleFileCleanup(file, delayMs = 30 * 60 * 1000) // 30 minutes
}
```

#### Cleanup Strategy

1. **On App Close**: Delete file after intent is handled (best effort)
2. **Timeout**: Delete after 30 minutes of last access
3. **Cache Clear**: Automatically deleted with cache
4. **App Uninstall**: Cleanup when app is uninstalled

### Transfer Verification

```kotlin
fun calculateSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { fis ->
        val buffer = ByteArray(8192)
        var bytesRead = fis.read(buffer)
        while (bytesRead > 0) {
            digest.update(buffer, 0, bytesRead)
            bytesRead = fis.read(buffer)
        }
    }
    return "sha256:" + digest.digest().toHexString()
}

fun verifyFileTransfer(file: File, expectedChecksum: String): Boolean {
    val actualChecksum = calculateSha256(file)
    return actualChecksum.equals(expectedChecksum, ignoreCase = true)
}
```

---

## Error Handling

### Error Categories

| Category | Status Code | Examples | Recovery |
|----------|-------------|----------|----------|
| Validation Errors | 400 | Invalid URL format, unsupported scheme | Show error message |
| User Rejection | 403 | User declined to open | No action needed |
| Handler Not Found | 404 | No app for MIME type | Suggest app installation |
| Content Too Large | 413 | File exceeds size limit | Show size requirement |
| Unsupported Type | 415 | Unknown MIME type | Offer file download |
| Transfer Error | 500 | Network interruption, checksum mismatch | Retry or cancel |
| Timeout | 408 | No response within 30 seconds | Show timeout message |

### Error Response Examples

#### Invalid URL Scheme
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345",
    "status": "error",
    "statusCode": 400,
    "message": "Unsupported URL scheme: 'file'",
    "reason": "invalid_scheme",
    "details": "Only http, https, mailto, tel, geo, and sms schemes are allowed",
    "timestamp": 1704067203000
  }
}
```

#### Handler Not Available
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345",
    "status": "error",
    "statusCode": 404,
    "message": "No handler available for application/pdf",
    "reason": "handler_not_found",
    "suggestedAction": "install_app",
    "suggestedAppName": "Google Drive",
    "suggestedAppUrl": "https://play.google.com/store/apps/details?id=com.google.android.apps.docs",
    "timestamp": 1704067203000
  }
}
```

#### File Too Large
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345",
    "status": "error",
    "statusCode": 413,
    "message": "File size exceeds maximum limit",
    "reason": "content_too_large",
    "details": "File is 10GB, maximum supported is 5GB",
    "maxSize": 5368709120,
    "attemptedSize": 10737418240,
    "timestamp": 1704067203000
  }
}
```

#### Checksum Mismatch
```json
{
  "id": 1704067203000,
  "type": "cconnect.open.response",
  "body": {
    "requestId": "req-12345",
    "status": "error",
    "statusCode": 500,
    "message": "File integrity verification failed",
    "reason": "checksum_mismatch",
    "details": "File checksum does not match expected value",
    "expectedChecksum": "sha256:abc123...",
    "actualChecksum": "sha256:def456...",
    "timestamp": 1704067203000
  }
}
```

### Timeout Handling

- **Request Timeout**: 30 seconds to receive response
- **Transfer Timeout**: 5 minutes per megabyte of data
- **Dialog Timeout**: 5 minutes for user to respond (optional auto-reject)

```kotlin
fun sendOpenRequest(
    request: OpenRequest,
    timeoutMs: Long = 30000
): Deferred<OpenResponse> = async {
    val responseChannel = Channel<OpenResponse>()
    val job = launch {
        try {
            val response = withTimeoutOrNull(timeoutMs) {
                responseChannel.receive()
            }
            if (response == null) {
                throw TimeoutException("Open request timed out after ${timeoutMs}ms")
            }
            response
        } catch (e: TimeoutException) {
            OpenResponse.timeout(request.requestId)
        }
    }

    // Send request...
    network.send(request)

    job.await()
}
```

---

## Implementation Checklist

### Android Implementation

- [ ] Create `AppContinuityPlugin` class
- [ ] Implement packet parsing and handling
- [ ] Add URL validation using proper URI parser
- [ ] Create confirmation dialog UI
- [ ] Implement handler resolution and selection
- [ ] Add file transfer receiver integration
- [ ] Implement checksum verification
- [ ] Add error handling and user feedback
- [ ] Create unit tests for URL validation
- [ ] Add integration tests for packet flow
- [ ] Document in AndroidX architecture
- [ ] Add logging for debugging
- [ ] Handle Android-specific schemes (intent, market)

### COSMIC Desktop Implementation

- [ ] Create `AppContinuityPlugin` in applet
- [ ] Implement packet parsing and handling
- [ ] Add URL validation using proper URL parser
- [ ] Create confirmation dialog in libcosmic UI
- [ ] Implement handler resolution via DBus
- [ ] Add file transfer sender integration
- [ ] Implement checksum generation
- [ ] Add error handling and user notifications
- [ ] Create tests for packet handling
- [ ] Add logging for debugging
- [ ] Handle desktop-specific content types
- [ ] Document in applet architecture

---

## Testing Strategy

### Unit Tests

**Android URL Validation**:
```kotlin
@Test
fun testValidUrlScheme() {
    assertThat(validateUrl("https://example.com")).isEqualTo(Valid)
}

@Test
fun testRejectedFileScheme() {
    assertThat(validateUrl("file:///etc/passwd")).isEqualTo(
        UnsupportedScheme("file")
    )
}

@Test
fun testPrivateIPRejection() {
    assertThat(validateUrl("http://192.168.1.1")).isEqualTo(PrivateIP)
}

@Test
fun testLocalhostRejection() {
    assertThat(validateUrl("http://localhost:8080")).isEqualTo(PrivateIP)
}

@Test
fun testMaxLengthEnforcement() {
    val longUrl = "https://example.com/" + "a".repeat(2100)
    assertThat(validateUrl(longUrl)).isEqualTo(TooLong)
}

@Test
fun testNoEmbeddedCredentials() {
    assertThat(validateUrl("https://user:pass@example.com")).isEqualTo(
        ContainsCredentials
    )
}
```

### Integration Tests

- Test complete flow from request to response
- Test file transfer with checksum verification
- Test error responses for various failure modes
- Test handler selection and app launching
- Test packet serialization/deserialization

### Security Tests

- Verify no XSS through URL schemes
- Verify no injection attacks through MIME types
- Test file transfer checksum validation
- Test private IP blocking
- Test credential stripping

---

## References

- [COSMIC Connect Packet Protocol](./networkpacket-code-review-fixes.md)
- [KDE Connect Protocol Reference](https://valent.andyholmes.ca/documentation/protocol.html)
- [Android Intent Specification](https://developer.android.com/guide/components/intents-filters)
- [MIME Types (IANA Registry)](https://www.iana.org/assignments/media-types/media-types.xhtml)
- [URL Specification (RFC 3986)](https://tools.ietf.org/html/rfc3986)
- [Android Security Best Practices](https://developer.android.com/training/articles/security-best-practices)

---

## Changelog

### Version 1.0.0 (2025-01-31)

Initial specification including:
- Overview and architecture
- Three packet types with complete specifications
- Sequence diagrams for all flows
- URL scheme allowlist and validation requirements
- Security validation requirements with code examples
- MIME type handling and default handlers
- File transfer integration details
- Error handling and recovery strategies
- Implementation checklist
- Testing strategy

---

## Contact & Questions

For questions about this specification, please file an issue on GitHub:
- Repository: [cosmic-connect-android](https://github.com/olafkfreund/cosmic-connect-android)
- Issue: [#114 - App Continuity Protocol Specification](https://github.com/olafkfreund/cosmic-connect-android/issues/114)
