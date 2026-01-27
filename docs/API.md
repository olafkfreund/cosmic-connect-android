# COSMIC Connect API

This document outlines the internal APIs and architecture of COSMIC Connect Android.

## Architecture
COSMIC Connect follows a specific architecture to handle device discovery, pairing, and plugin communication.

### Core Components
*   **DeviceHost:** Manages the list of known devices.
*   **Device:** Represents a connected or known device. Handles packet transmission.
*   **Plugin:** Base class for all features (File Sharing, MPRIS, Input, etc.).
*   **NetworkPacket:** Data structure for communication between devices.

## Plugin API
Plugins are the core unit of functionality.
Each plugin extends `Plugin` and must define:
*   `displayName`: User-visible name.
*   `getUiButtons()`: (Optional) Buttons to show in the device detail view.
*   `onPacketReceived(NetworkPacket)`: Handle incoming packets.

## Protocol
The communication protocol is based on JSON packets sent over TLS/TCP (LanBackend) or Bluetooth.
See `docs/architecture/protocol` for detailed protocol specifications.
