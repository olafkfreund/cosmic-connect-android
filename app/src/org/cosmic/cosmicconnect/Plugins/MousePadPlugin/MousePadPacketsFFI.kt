package org.cosmic.cosmicconnect.Plugins.MousePadPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMousepadRequest

/**
 * FFI wrapper for creating MousePad plugin packets
 *
 * The MousePad plugin enables remote mouse and keyboard control, allowing
 * the desktop to control the Android device's cursor and keyboard input.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send mouse movement events
 * - Send mouse click events (left, right, middle, double)
 * - Send mouse hold/release events
 * - Send scroll events
 * - Send keyboard input
 * - Send special keys
 *
 * ## Usage
 *
 * **Sending mouse movement:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val moveBody = JSONObject(mapOf(
 *     "dx" to 10.0,
 *     "dy" to -5.0
 * ))
 * val packet = MousePadPacketsFFI.createMousePadRequest(moveBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * **Sending mouse click:**
 * ```kotlin
 * val clickBody = JSONObject(mapOf("singleclick" to true))
 * val packet = MousePadPacketsFFI.createMousePadRequest(clickBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * **Sending scroll:**
 * ```kotlin
 * val scrollBody = JSONObject(mapOf(
 *     "scroll" to true,
 *     "dx" to 0.0,
 *     "dy" to -1.0
 * ))
 * val packet = MousePadPacketsFFI.createMousePadRequest(scrollBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * **Sending keyboard input:**
 * ```kotlin
 * val keyBody = JSONObject(mapOf(
 *     "key" to "a",
 *     "specialKey" to 0
 * ))
 * val packet = MousePadPacketsFFI.createMousePadRequest(keyBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * @see MousePadPlugin
 */
object MousePadPacketsFFI {

    /**
     * Create a MousePad request packet
     *
     * Creates a packet for sending mouse and keyboard events to the remote device.
     * The body should contain the command type and relevant parameters.
     *
     * ## Supported Commands
     *
     * **Mouse movement:**
     * - `dx: <double>` - Horizontal movement delta
     * - `dy: <double>` - Vertical movement delta
     *
     * **Mouse clicks:**
     * - `singleclick: true` - Left click
     * - `doubleclick: true` - Double left click
     * - `middleclick: true` - Middle click
     * - `rightclick: true` - Right click
     * - `singlehold: true` - Press and hold left button
     * - `singlerelease: true` - Release left button
     *
     * **Scrolling:**
     * - `scroll: true` with `dx` and `dy` - Scroll amount
     *
     * **Keyboard:**
     * - `key: <string>` - Character to type
     * - `specialKey: <int>` - Special key code (0 for normal keys)
     *
     * The body JSON should be formatted as:
     * ```json
     * {
     *   "dx": 10.0,
     *   "dy": -5.0
     * }
     * ```
     *
     * @param bodyJson JSON string containing command and parameters
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createMousePadRequest(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMousepadRequest(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
