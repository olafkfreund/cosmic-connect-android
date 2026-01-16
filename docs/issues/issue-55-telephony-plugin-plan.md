# Issue #55: Telephony Plugin FFI Migration Plan

**Status**: Planning
**Priority**: High
**Complexity**: High
**Estimated Effort**: 10-12 hours (with pattern reuse from Issue #54)
**Target Completion**: January 19, 2026

---

## Executive Summary

Migrate the Telephony Plugin from manual packet construction to FFI-based architecture, following the proven 5-phase pattern established in Issues #53 and #54. The Telephony plugin is more complex than Clipboard, with **7 packet types** and multiple data structures (call events, SMS messages, conversations), but can leverage established patterns for significant time savings.

---

## Current State Analysis

### Rust Core (telephony.rs)

**Location**: `cosmic-connect-core/src/plugins/telephony.rs`
**Size**: 574 lines
**Status**: Exists but needs refactoring

**Issues Identified**:
1. **Duplicate initialize() methods** (lines 422-431)
   - First one references non-existent `device` parameter
   - Second one is correct (no parameters)
2. **Test dependencies on Device** (lines 464, 479, 482, 569)
   - Tests use `create_test_device()` and `device` parameter
   - Need to remove Device dependencies like Clipboard plugin
3. **Missing ProtocolError import** (line 325)
   - References `ProtocolError` without importing

**Packet Creation Methods** (Already Exist):
- ✅ `create_mute_request()` - Lines 240-243
- ✅ `create_conversations_request()` - Lines 248-251
- ✅ `create_conversation_request()` - Lines 260-284
- ✅ `create_attachment_request()` - Lines 292-302
- ✅ `create_send_sms_request()` - Lines 310-320

**Packet Handling Methods**:
- `handle_telephony_event()` - Lines 323-361
- `handle_sms_messages()` - Lines 364-389

---

### Android Implementation

**Current Files**:
- `TelephonyPlugin.java` - Main plugin class
- Likely uses manual HashMap construction (to be verified)

---

## Protocol Specification

### Packet Types (7 total)

#### 1. Telephony Event (Incoming)
**Type**: `kdeconnect.telephony`
**Direction**: Android → Desktop
**Fields**:
- `event`: String - "ringing", "talking", "missedCall", "sms" (deprecated)
- `phoneNumber`: String (optional) - Caller's phone number
- `contactName`: String (optional) - Contact name from address book
- `messageBody`: String (optional, deprecated) - SMS body

**Example**:
```json
{
  "id": 1234567890,
  "type": "kdeconnect.telephony",
  "body": {
    "event": "ringing",
    "phoneNumber": "+1234567890",
    "contactName": "John Doe"
  }
}
```

---

#### 2. Mute Ringer Request (Outgoing)
**Type**: `kdeconnect.telephony.request_mute`
**Direction**: Desktop → Android
**Fields**: None (empty body)

**Example**:
```json
{
  "id": 1234567891,
  "type": "kdeconnect.telephony.request_mute",
  "body": {}
}
```

---

#### 3. SMS Messages (Incoming)
**Type**: `kdeconnect.sms.messages`
**Direction**: Android → Desktop
**Fields**:
- `conversations`: Array of conversation objects
  - `thread_id`: i64 - Conversation thread ID
  - `messages`: Array of message objects
    - `_id`: i64 - Message ID
    - `thread_id`: i64 - Thread ID
    - `address`: String - Phone number
    - `body`: String - Message text
    - `date`: i64 - Timestamp (ms since epoch)
    - `type`: i32 - 1=received, 2=sent
    - `read`: i32 - 0=unread, 1=read

**Example**:
```json
{
  "id": 1234567892,
  "type": "kdeconnect.sms.messages",
  "body": {
    "conversations": [
      {
        "thread_id": 123,
        "messages": [
          {
            "_id": 456,
            "thread_id": 123,
            "address": "+1234567890",
            "body": "Hello!",
            "date": 1705507200000,
            "type": 1,
            "read": 1
          }
        ]
      }
    ]
  }
}
```

---

#### 4. Request Conversations List (Outgoing)
**Type**: `kdeconnect.sms.request_conversations`
**Direction**: Desktop → Android
**Fields**: None (empty body)

**Example**:
```json
{
  "id": 1234567893,
  "type": "kdeconnect.sms.request_conversations",
  "body": {}
}
```

---

#### 5. Request Conversation Messages (Outgoing)
**Type**: `kdeconnect.sms.request_conversation`
**Direction**: Desktop → Android
**Fields**:
- `threadID`: i64 - Conversation thread ID
- `rangeStartTimestamp`: i64 (optional) - Earliest message timestamp
- `numberToRequest`: i32 (optional) - Max messages to return

**Example**:
```json
{
  "id": 1234567894,
  "type": "kdeconnect.sms.request_conversation",
  "body": {
    "threadID": 123,
    "rangeStartTimestamp": 1705507200000,
    "numberToRequest": 50
  }
}
```

---

#### 6. Request Message Attachment (Outgoing)
**Type**: `kdeconnect.sms.request_attachment`
**Direction**: Desktop → Android
**Fields**:
- `part_id`: i64 - Attachment part ID
- `unique_identifier`: String - Unique file identifier

**Example**:
```json
{
  "id": 1234567895,
  "type": "kdeconnect.sms.request_attachment",
  "body": {
    "part_id": 789,
    "unique_identifier": "abc123"
  }
}
```

---

#### 7. Send SMS (Outgoing)
**Type**: `kdeconnect.sms.request`
**Direction**: Desktop → Android
**Fields**:
- `phoneNumber`: String - Recipient phone number
- `messageBody`: String - Message text

**Example**:
```json
{
  "id": 1234567896,
  "type": "kdeconnect.sms.request",
  "body": {
    "phoneNumber": "+1234567890",
    "messageBody": "Hello from desktop!"
  }
}
```

---

## Migration Strategy

### Phase 1: Rust Refactoring (2 hours)

**Objective**: Remove Device dependencies and fix duplicate methods

**Tasks**:
1. Fix duplicate `initialize()` methods
   - Remove first `initialize()` (lines 422-426) with device parameter
   - Keep second `initialize()` (lines 428-431) without parameters
   - Rename to avoid confusion if needed

2. Update packet handling methods
   - Remove any Device references from `handle_telephony_event()`
   - Remove any Device references from `handle_sms_messages()`

3. Fix test dependencies
   - Remove `create_test_device()` helper
   - Update all 10 tests to work without Device
   - Update `test_plugin_initialization()` (line 477-483)
   - Update `test_plugin_lifecycle()` (line 564-572)

4. Add missing imports
   - Fix `ProtocolError` import (line 325)

5. Verify cargo build
   - Run `cargo build --release`
   - Run `cargo test telephony --lib`
   - Ensure all 10 tests pass

**Files to Modify**: 1
- cosmic-connect-core/src/plugins/telephony.rs

**Expected Changes**:
- ~30 insertions
- ~50 deletions
- Net: -20 lines (simplified)

---

### Phase 2: FFI Interface Implementation (2 hours)

**Objective**: Create FFI functions for all 7 packet types

**Tasks**:
1. Add FFI functions to `ffi/mod.rs` (7 functions)
   - `create_telephony_event(event, phone, contact)` - Call notification
   - `create_mute_request()` - Mute ringer
   - `create_sms_messages(conversations_json)` - SMS conversations
   - `create_sms_conversations_request()` - Request conversation list
   - `create_sms_conversation_request(thread_id, start_ts, count)` - Request messages
   - `create_sms_attachment_request(part_id, unique_id)` - Request attachment
   - `create_send_sms_request(phone, message)` - Send SMS

2. Update `cosmic_connect_core.udl`
   - Add all 7 function signatures
   - Document parameters and return types
   - Add KDoc-style comments

3. Export functions in `lib.rs`
   - Add all 7 functions to public exports

4. Verify UniFFI scaffolding
   - Run `cargo build --features=uniffi`
   - Verify Kotlin bindings generated correctly

**Files to Modify**: 3
- cosmic-connect-core/src/ffi/mod.rs
- cosmic-connect-core/src/cosmic_connect_core.udl
- cosmic-connect-core/src/lib.rs

**Expected Changes**:
- ~200 lines added (7 FFI functions with documentation)

---

### Phase 3: Android Wrapper Creation (3 hours)

**Objective**: Create idiomatic Kotlin wrapper with type-safe API

**Tasks**:
1. Create `TelephonyPacketsFFI.kt` (~400 lines)
   - Packet creation methods (7 methods)
   - Extension properties for type-safe inspection (14+ properties)
   - Java-compatible helper functions (14+ functions)
   - Comprehensive KDoc documentation

2. **Object Structure**:
```kotlin
object TelephonyPacketsFFI {
    // Telephony Event (Call Notifications)
    fun createTelephonyEvent(
        event: String,
        phoneNumber: String?,
        contactName: String?
    ): NetworkPacket

    // Mute Ringer
    fun createMuteRequest(): NetworkPacket

    // SMS Messages
    fun createSmsMessages(conversationsJson: String): NetworkPacket

    // Request Conversations List
    fun createConversationsRequest(): NetworkPacket

    // Request Conversation Messages
    fun createConversationRequest(
        threadId: Long,
        startTimestamp: Long?,
        count: Int?
    ): NetworkPacket

    // Request Attachment
    fun createAttachmentRequest(
        partId: Long,
        uniqueIdentifier: String
    ): NetworkPacket

    // Send SMS
    fun createSendSmsRequest(
        phoneNumber: String,
        messageBody: String
    ): NetworkPacket
}
```

3. **Extension Properties** (Packet Inspection):
```kotlin
// Type detection
val NetworkPacket.isTelephonyEvent: Boolean
val NetworkPacket.isMuteRequest: Boolean
val NetworkPacket.isSmsMessages: Boolean
val NetworkPacket.isSmsConversationsRequest: Boolean
val NetworkPacket.isSmsConversationRequest: Boolean
val NetworkPacket.isSmsAttachmentRequest: Boolean
val NetworkPacket.isSendSmsRequest: Boolean

// Field extraction - Telephony Event
val NetworkPacket.telephonyEventType: String?
val NetworkPacket.phoneNumber: String?
val NetworkPacket.contactName: String?

// Field extraction - SMS Messages
val NetworkPacket.smsConversations: List<SmsConversation>?

// Field extraction - Conversation Request
val NetworkPacket.threadId: Long?
val NetworkPacket.rangeStartTimestamp: Long?
val NetworkPacket.numberToRequest: Int?

// Field extraction - Attachment Request
val NetworkPacket.partId: Long?
val NetworkPacket.uniqueIdentifier: String?

// Field extraction - Send SMS
val NetworkPacket.smsPhoneNumber: String?
val NetworkPacket.smsMessageBody: String?
```

4. **Data Classes** (SMS structures):
```kotlin
data class SmsMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Int
)

data class SmsConversation(
    val threadId: Long,
    val messages: List<SmsMessage>
)
```

5. **Input Validation**:
   - Event type must be valid ("ringing", "talking", "missedCall", "sms")
   - Phone numbers must not be blank
   - Message bodies must not be blank
   - Thread IDs must be positive
   - Timestamps must be non-negative

**Files to Create**: 1
- src/org/cosmic/cosmicconnect/Plugins/TelephonyPlugin/TelephonyPacketsFFI.kt

**Expected Size**: ~400 lines (60% documentation)

---

### Phase 4: Android Integration (2 hours)

**Objective**: Integrate TelephonyPacketsFFI into TelephonyPlugin.java

**Tasks**:
1. Add FFI imports
   - Import TelephonyPacketsFFI object
   - Static import of extension property helpers

2. Update packet creation methods
   - Replace manual HashMap construction
   - Use TelephonyPacketsFFI.createXxx() methods

3. Update packet receiving logic
   - Use extension properties for type checking
   - Use extension properties for field extraction
   - Add explicit null checks

4. Verify Java compilation
   - Run `./gradlew :compileDebugJava`
   - Verify no compilation errors

**Files to Modify**: 1
- src/org/cosmic/cosmicconnect/Plugins/TelephonyPlugin/TelephonyPlugin.java

**Expected Changes**:
- ~50 insertions
- ~60 deletions
- Net: -10 lines (code reduction)

---

### Phase 5: Testing & Documentation (2-3 hours)

**Objective**: Create comprehensive testing guide and documentation

**Tasks**:
1. Create `issue-55-testing-guide.md`
   - 30+ test cases across 6 test suites
   - FFI Layer Tests (7 tests - one per packet type)
   - Kotlin Wrapper Tests (12 tests)
   - Plugin Integration Tests (8 tests)
   - End-to-End Tests (8 tests)
   - Regression Tests (3 tests)
   - Manual testing procedures

2. Create phase completion documents (5 documents)
   - issue-55-phase1-complete.md
   - issue-55-phase2-complete.md
   - issue-55-phase3-complete.md
   - issue-55-phase4-complete.md
   - issue-55-phase5-complete.md

3. Create `issue-55-complete-summary.md`
   - Executive summary
   - Technical overview
   - Phase-by-phase breakdown
   - Code changes summary
   - Testing strategy
   - Lessons learned

4. Update FFI Integration Guide
   - Mark Telephony plugin complete (5/10 plugins, 50%)
   - Update next plugin target
   - Update document version

5. Update project status document
   - Create project-status-2026-01-19.md
   - Update overall progress (50%)
   - Update metrics and timeline

**Files to Create/Update**: 9 documents (~4,000 lines)

---

## Comparison with Issue #54 (Clipboard)

| Metric | Clipboard (#54) | Telephony (#55) | Change |
|--------|-----------------|-----------------|--------|
| Packet Types | 2 | 7 | +250% |
| FFI Functions | 2 | 7 | +250% |
| Extension Properties | 4 | 14+ | +250% |
| Data Structures | None | 2 (SmsMessage, SmsConversation) | +2 |
| Kotlin Wrapper Size | 280 lines | ~400 lines | +43% |
| Test Cases | 22 | 30+ | +36% |
| Estimated Effort | 9.5h | 12h | +26% |
| Actual (Clipboard) | 6.5h | TBD | -32% |

**Key Differences**:
- More packet types (7 vs 2)
- More complex data structures (SMS conversations)
- More extension properties needed
- More test cases required

**Estimated Time Savings**:
- With pattern reuse: 25-30% savings
- Target: 10 hours (vs 12h estimated)

---

## Success Criteria

### Technical
- ✅ All 7 packet types have FFI functions
- ✅ TelephonyPacketsFFI.kt created with 14+ APIs
- ✅ All extension properties working
- ✅ Java-compatible helpers functional
- ✅ Zero compilation errors (Rust + Android)
- ✅ All tests passing

### Documentation
- ✅ Comprehensive testing guide (30+ test cases)
- ✅ All 5 phase completion documents
- ✅ Complete summary document
- ✅ FFI Integration Guide updated
- ✅ Project status updated
- ✅ 100% API documentation

### Testing
- ✅ 30+ test cases documented
- ✅ 6 test suites defined
- ✅ Manual testing procedures
- ✅ Coverage targets met (85%+)

---

## Risk Assessment

### Low Risk ✅
- Established 5-phase pattern (proven with #54)
- FFI workflow streamlined
- Documentation templates ready
- Clear protocol specification

### Medium Risk ⚠️
- More complex than Clipboard (7 packet types vs 2)
- SMS conversation data structures
- Multiple packet handling paths
- Android SMS permissions (runtime)

### Mitigation
- Leverage ClipboardPacketsFFI.kt as template
- Break down complex SMS structures carefully
- Test each packet type independently
- Document permission requirements clearly

---

## Timeline

| Phase | Duration | Target Date |
|-------|----------|-------------|
| Planning | 0.5h | Jan 17 (today) |
| Phase 1 | 2h | Jan 17-18 |
| Phase 2 | 2h | Jan 18 |
| Phase 3 | 3h | Jan 18 |
| Phase 4 | 2h | Jan 18-19 |
| Phase 5 | 2-3h | Jan 19 |
| **Total** | **11-12h** | **Jan 19** |

With 25-30% time savings from pattern reuse: **Target 10 hours**

---

## Next Steps

1. **Immediate**: Start Phase 1 (Rust refactoring)
   - Fix duplicate initialize() methods
   - Remove Device dependencies from tests
   - Fix missing imports

2. **After Phase 1**: Phase 2 (FFI interface)
   - Create 7 FFI functions
   - Update .udl file
   - Verify UniFFI scaffolding

3. **After Phase 2**: Phase 3 (Android wrapper)
   - Create TelephonyPacketsFFI.kt
   - Implement 14+ extension properties
   - Add Java-compatible helpers

---

## References

- Issue #54 (Clipboard): Proven 5-phase pattern
- Issue #53 (Share): Complex plugin migration example
- FFI Integration Guide: v1.2 (updated Jan 17)
- KDE Connect Protocol: Telephony and SMS specifications

---

**Document Version**: 1.0
**Created**: January 17, 2026
**Status**: Ready for implementation
**Next Action**: Begin Phase 1 (Rust refactoring)
