/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin

/**
 * Connection mode for Extended Display.
 *
 * Determines the network transport used for signaling and media:
 * - **WIFI**: Standard network mode using network signaling server
 * - **USB**: Direct localhost connection when phone is connected via USB
 * - **MANUAL**: User-provided IP address and port
 */
enum class ConnectionMode {
    /** Standard WiFi/network connection with mDNS discovery */
    WIFI,

    /** USB tethering mode (signaling via localhost) */
    USB,

    /** Manual IP address entry */
    MANUAL;

    /**
     * Returns a human-readable display name for UI.
     */
    val displayName: String
        get() = when (this) {
            WIFI -> "Wi-Fi"
            USB -> "USB"
            MANUAL -> "Manual"
        }

    /**
     * Returns a description of this connection mode.
     */
    val description: String
        get() = when (this) {
            WIFI -> "Connect over local network using automatic discovery"
            USB -> "Connect via USB cable (requires ADB setup)"
            MANUAL -> "Connect by entering IP address and port manually"
        }

    /**
     * Returns whether this mode requires network access.
     */
    val requiresNetwork: Boolean
        get() = when (this) {
            WIFI, MANUAL -> true
            USB -> false
        }

    /**
     * Returns whether this mode supports automatic discovery.
     */
    val supportsDiscovery: Boolean
        get() = when (this) {
            WIFI -> true
            USB, MANUAL -> false
        }

    companion object {
        /** Returns the default connection mode */
        val DEFAULT = WIFI

        /** Returns the localhost address used for USB connections */
        const val USB_LOCALHOST = "127.0.0.1"
    }
}

/**
 * Connection state for Extended Display client.
 *
 * Tracks the current state of the WebRTC connection lifecycle.
 */
enum class ConnectionState {
    /** Not connected */
    DISCONNECTED,

    /** Establishing connection */
    CONNECTING,

    /** Fully connected and streaming */
    CONNECTED,

    /** Disconnecting from server */
    DISCONNECTING,

    /** Connection failed */
    FAILED,

    /** Connection closed normally */
    CLOSED,

    /** Error state */
    ERROR
}
