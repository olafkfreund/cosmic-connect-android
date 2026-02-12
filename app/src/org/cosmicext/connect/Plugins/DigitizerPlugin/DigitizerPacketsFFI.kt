package org.cosmicext.connect.Plugins.DigitizerPlugin

import org.cosmicext.connect.Core.NetworkPacket
import uniffi.cosmic_ext_connect_core.createDigitizerSession
import uniffi.cosmic_ext_connect_core.createDigitizerEvent

/**
 * FFI wrapper for creating Digitizer plugin packets
 *
 * The Digitizer plugin enables pen/stylus input for drawing tablets,
 * allowing the desktop to use the Android device as a drawing surface.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Start and end drawing sessions with screen dimensions
 * - Send pen/stylus events with coordinates, pressure, and tool type
 *
 * ## Usage
 *
 * **Starting a drawing session:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val sessionBody = JSONObject(mapOf(
 *     "action" to "start",
 *     "width" to 1920,
 *     "height" to 1080,
 *     "resolutionX" to 96,
 *     "resolutionY" to 96
 * ))
 * val packet = DigitizerPacketsFFI.createSessionPacket(sessionBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * **Ending a drawing session:**
 * ```kotlin
 * val endBody = JSONObject(mapOf("action" to "end"))
 * val packet = DigitizerPacketsFFI.createSessionPacket(endBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * **Sending pen/stylus events:**
 * ```kotlin
 * val eventBody = JSONObject(mapOf(
 *     "active" to true,
 *     "touching" to true,
 *     "tool" to "Pen",
 *     "x" to 500,
 *     "y" to 300,
 *     "pressure" to 0.75
 * ))
 * val packet = DigitizerPacketsFFI.createEventPacket(eventBody.toString())
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * @see DigitizerPlugin
 * @see ToolEvent
 */
object DigitizerPacketsFFI {

    /**
     * Create a DIGITIZER session packet
     *
     * Creates a packet for starting or ending a drawing session.
     *
     * **For session start**, the body JSON should contain:
     * - `action`: "start"
     * - `width`: Canvas width in pixels
     * - `height`: Canvas height in pixels
     * - `resolutionX`: Horizontal resolution (DPI)
     * - `resolutionY`: Vertical resolution (DPI)
     *
     * **For session end**, the body JSON should contain:
     * - `action`: "end"
     *
     * Example JSON for session start:
     * ```json
     * {
     *   "action": "start",
     *   "width": 1920,
     *   "height": 1080,
     *   "resolutionX": 96,
     *   "resolutionY": 96
     * }
     * ```
     *
     * Example JSON for session end:
     * ```json
     * {
     *   "action": "end"
     * }
     * ```
     *
     * @param bodyJson JSON string containing session parameters
     * @return NetworkPacket ready to send
     *
     * @throws CosmicExtConnectException if packet creation fails
     * @throws CosmicExtConnectException if JSON parsing fails
     */
    fun createSessionPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_ext_connect_core.createDigitizerSession(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a DIGITIZER event packet
     *
     * Creates a packet for pen/stylus input events with coordinates,
     * pressure, and tool type.
     *
     * All fields in the body are optional and should only be included
     * when they have changed or are relevant to the event:
     *
     * - `active`: Boolean - Tool is active/in range
     * - `touching`: Boolean - Tool is touching the surface
     * - `tool`: String - Tool type ("Pen" or "Rubber")
     * - `x`: Int - X coordinate (relative to canvas)
     * - `y`: Int - Y coordinate (relative to canvas)
     * - `pressure`: Double - Pressure level (0.0 to 1.0)
     *
     * Example JSON:
     * ```json
     * {
     *   "active": true,
     *   "touching": true,
     *   "tool": "Pen",
     *   "x": 500,
     *   "y": 300,
     *   "pressure": 0.75
     * }
     * ```
     *
     * @param bodyJson JSON string containing event data
     * @return NetworkPacket ready to send
     *
     * @throws CosmicExtConnectException if packet creation fails
     * @throws CosmicExtConnectException if JSON parsing fails
     */
    fun createEventPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_ext_connect_core.createDigitizerEvent(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
