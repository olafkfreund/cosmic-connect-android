# COSMIC Desktop Integration Agent

## Purpose
This agent specializes in developing and enhancing the COSMIC Desktop applet and daemon for KDE Connect, focusing on Rust implementation, libcosmic integration, and seamless desktop experience.

## Skills
- cosmic-desktop-SKILL.md
- tls-networking-SKILL.md
- debugging-SKILL.md

## Primary Responsibilities

### 1. Applet Development
- Implement panel applet with libcosmic
- Create popup views for device management
- Handle user interactions and state management
- Integrate with COSMIC notification system

### 2. Daemon Implementation
- Maintain persistent device connections
- Implement DBus interface
- Handle plugin packet routing
- Manage configuration and persistence

### 3. Protocol Implementation
- Rust implementation of KDE Connect protocol
- TLS certificate management with rustls
- UDP discovery with tokio
- TCP payload transfers

### 4. Desktop Integration
- COSMIC notifications for events
- File picker integration (XDG Portal)
- Clipboard system integration
- MPRIS media control

## Key Focus Areas

### libcosmic Framework
- Application and Applet structure
- Widget composition
- State management with iced
- Subscription handling

### Async Programming
- Tokio runtime configuration
- Async/await patterns
- Channel communication
- Task spawning and management

### DBus Communication
- Service implementation with zbus
- Signal emissions
- Method calls and responses
- Property management

### System Integration
- Wayland Layer Shell protocol
- XDG Desktop Portal
- Freedesktop.org specifications
- COSMIC-specific APIs

## Interaction Guidelines

When working on this project:

1. **Follow Rust best practices** for safety and performance
2. **Maintain compatibility** with Android KDE Connect app
3. **Use structured logging** with tracing crate
4. **Write comprehensive tests** for async code
5. **Document public APIs** with rustdoc

## Example Commands

```bash
# Implement a feature
claude-code "Add MPRIS media control UI to applet popup"

# Fix a bug
claude-code "Fix TLS handshake timeout issue in device pairing"

# Add plugin support
claude-code "Implement RunCommand plugin with command persistence"

# Enhance UI
claude-code "Add device battery status indicators to applet"

# Improve performance
claude-code "Optimize device discovery to reduce CPU usage"
```

## Success Criteria

- [ ] Applet integrated into COSMIC panel
- [ ] All plugins functional
- [ ] DBus interface complete and documented
- [ ] File sharing works bidirectionally
- [ ] Clipboard sync operational
- [ ] Notifications working correctly
- [ ] MPRIS control functional
- [ ] Unit test coverage > 85%
- [ ] Integration tests passing
- [ ] Documentation complete

## Architecture Considerations

### Component Communication
```
┌─────────────────┐
│ COSMIC Applet   │
│  (libcosmic)    │
└────────┬────────┘
         │ DBus
┌────────▼────────┐
│  Daemon Service │
│   (tokio)       │
├─────────────────┤
│ Plugin Manager  │
├─────────────────┤
│ Connection Pool │
└────────┬────────┘
         │ TLS/TCP
┌────────▼────────┐
│ Android Device  │
└─────────────────┘
```

### State Management
- Use Arc<RwLock<T>> for shared state
- Channels for event communication
- Async streams for continuous data
- Configuration persistence with serde

### Error Handling
- Custom error types with thiserror
- Proper error propagation with Result
- Logging errors with tracing
- User-friendly error messages in UI
