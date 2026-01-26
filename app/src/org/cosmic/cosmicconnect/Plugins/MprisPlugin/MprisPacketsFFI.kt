package org.cosmic.cosmicconnect.Plugins.MprisPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMprisRequest

/**
 * FFI wrapper for creating MPRIS plugin packets
 *
 * The MPRIS plugin enables media control integration, allowing the desktop
 * to control Android media playback (music, videos, podcasts). This wrapper
 * provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send playback control commands (play, pause, stop, next, previous)
 * - Control volume
 * - Seek to positions
 * - Toggle shuffle and loop modes
 *
 * ## Usage
 *
 * **Sending playback commands:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * // Play/Pause
 * val playPauseBody = JSONObject(mapOf(
 *     "player" to "spotify",
 *     "action" to "PlayPause"
 * ))
 * val packet = MprisPacketsFFI.createMprisRequest(playPauseBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Controlling volume:**
 * ```kotlin
 * val volumeBody = JSONObject(mapOf(
 *     "player" to "vlc",
 *     "setVolume" to 75
 * ))
 * val packet = MprisPacketsFFI.createMprisRequest(volumeBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Seeking to position:**
 * ```kotlin
 * val seekBody = JSONObject(mapOf(
 *     "player" to "spotify",
 *     "SetPosition" to 123000  // milliseconds
 * ))
 * val packet = MprisPacketsFFI.createMprisRequest(seekBody.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * @see MprisPlugin
 */
object MprisPacketsFFI {

    /**
     * Create an MPRIS request packet
     *
     * Creates a packet for controlling media playback on the remote device.
     * The body should contain the player name and the command/value pairs.
     *
     * ## Supported Commands
     *
     * **Playback control:**
     * - `action: "PlayPause"` - Toggle play/pause
     * - `action: "Play"` - Start playback
     * - `action: "Pause"` - Pause playback
     * - `action: "Stop"` - Stop playback
     * - `action: "Next"` - Next track
     * - `action: "Previous"` - Previous track
     *
     * **Volume control:**
     * - `setVolume: <int>` - Set volume (0-100)
     *
     * **Seeking:**
     * - `SetPosition: <long>` - Set position in milliseconds
     * - `Seek: <int>` - Seek by offset in milliseconds
     *
     * **Playback modes:**
     * - `setLoopStatus: <string>` - Set loop mode ("None", "Track", "Playlist")
     * - `setShuffle: <boolean>` - Toggle shuffle
     *
     * The body JSON should be formatted as:
     * ```json
     * {
     *   "player": "spotify",
     *   "action": "PlayPause"
     * }
     * ```
     *
     * @param bodyJson JSON string containing player name and command
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createMprisRequest(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMprisRequest(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
