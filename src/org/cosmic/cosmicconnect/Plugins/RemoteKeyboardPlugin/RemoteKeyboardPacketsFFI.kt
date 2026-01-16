package org.cosmic.cosmicconnect.Plugins.RemoteKeyboardPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMousepadEcho
import uniffi.cosmic_connect_core.createMousepadKeyboardstate

/**
 * FFI wrapper for creating RemoteKeyboard plugin packets
 *
 * The RemoteKeyboard plugin enables the device to act as a remote keyboard
 * input method, allowing the desktop to type on the Android device. This
 * wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send echo/acknowledgment replies for keyboard input
 * - Notify desktop of keyboard visibility state
 *
 * ## Usage
 *
 * **Sending echo reply:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val echoBody = JSONObject(mapOf(
 *     "key" to "a",
 *     "shift" to false,
 *     "ctrl" to false,
 *     "alt" to false,
 *     "isAck" to true
 * ))
 * val packet = RemoteKeyboardPacketsFFI.createEchoPacket(echoBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending keyboard state:**
 * ```kotlin
 * // Notify desktop that keyboard is visible
 * val packet = RemoteKeyboardPacketsFFI.createKeyboardStatePacket(true)
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * @see RemoteKeyboardPlugin
 */
object RemoteKeyboardPacketsFFI {

    /**
     * Create a MOUSEPAD echo packet
     *
     * Creates an acknowledgment/echo packet in response to keyboard input
     * from the desktop. Used when sendAck is requested in the incoming packet.
     *
     * The body JSON should contain:
     * - `key`: The key character (if visible key)
     * - `specialKey`: Special key code (if special key)
     * - `shift`: Shift modifier state
     * - `ctrl`: Ctrl modifier state
     * - `alt`: Alt modifier state
     * - `isAck`: Always true for echo replies
     *
     * Example body:
     * ```json
     * {
     *   "key": "a",
     *   "shift": false,
     *   "ctrl": false,
     *   "alt": false,
     *   "isAck": true
     * }
     * ```
     *
     * @param bodyJson JSON string containing echo data
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createEchoPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMousepadEcho(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a MOUSEPAD keyboard state packet
     *
     * Creates a packet to notify the desktop of the keyboard's visibility
     * and active state. This is sent:
     * - When the plugin is created
     * - When the keyboard becomes visible or invisible
     * - When editing-only mode changes
     *
     * The desktop uses this information to show/hide keyboard-related UI
     * and to know when keyboard input is available.
     *
     * @param state Keyboard active state (true = keyboard visible/active)
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createKeyboardStatePacket(state: Boolean): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMousepadKeyboardstate(state)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
