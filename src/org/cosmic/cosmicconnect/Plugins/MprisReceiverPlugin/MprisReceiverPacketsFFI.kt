package org.cosmic.cosmicconnect.Plugins.MprisReceiverPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createMprisRequest

/**
 * FFI wrapper for creating MPRIS receiver plugin packets
 *
 * The MPRIS receiver plugin receives media playback control commands from
 * the desktop and reports current media state from Android media players.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send player list with album art support flag
 * - Send media metadata (title, artist, album, playback state, position, etc.)
 * - Send album art transfer details
 *
 * ## Usage
 *
 * **Sending player list:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val playerList = JSONObject(mapOf(
 *     "playerList" to listOf("Spotify", "YouTube Music"),
 *     "supportAlbumArtPayload" to true
 * ))
 * val packet = MprisReceiverPacketsFFI.createMprisPacket(playerList.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending metadata:**
 * ```kotlin
 * val metadata = JSONObject(mapOf(
 *     "player" to "Spotify",
 *     "title" to "Song Title",
 *     "artist" to "Artist Name",
 *     "album" to "Album Name",
 *     "isPlaying" to true,
 *     "pos" to 12345L,
 *     "length" to 180000L,
 *     "canPlay" to true,
 *     "canPause" to true,
 *     "canGoPrevious" to true,
 *     "canGoNext" to true,
 *     "canSeek" to true,
 *     "volume" to 75,
 *     "albumArtUrl" to "https://example.com/art.jpg"
 * ))
 * val packet = MprisReceiverPacketsFFI.createMprisPacket(metadata.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending album art transfer:**
 * ```kotlin
 * val artTransfer = JSONObject(mapOf(
 *     "player" to "Spotify",
 *     "transferringAlbumArt" to true,
 *     "albumArtUrl" to "https://example.com/art.jpg"
 * ))
 * val packet = MprisReceiverPacketsFFI.createMprisPacket(artTransfer.toString())
 * val legacyPacket = packet.toLegacyPacket()
 * legacyPacket.setPayload(NetworkPacket.Payload(albumArtBytes))
 * device.sendPacket(legacyPacket)
 * ```
 *
 * @see MprisReceiverPlugin
 */
object MprisReceiverPacketsFFI {

    /**
     * Create an MPRIS packet
     *
     * Creates a packet containing media player information, metadata,
     * or album art transfer details.
     *
     * **For player list**, the body should contain:
     * - `playerList`: Array of player names
     * - `supportAlbumArtPayload`: Boolean indicating album art support
     *
     * **For metadata**, the body should contain:
     * - `player`: Player name (required)
     * - `title`: Song title
     * - `artist`: Artist name
     * - `album`: Album name
     * - `nowPlaying`: Formatted "Artist - Title" string
     * - `isPlaying`: Boolean playback state
     * - `pos`: Current position in milliseconds
     * - `length`: Total length in milliseconds
     * - `canPlay`: Boolean indicating if play is supported
     * - `canPause`: Boolean indicating if pause is supported
     * - `canGoPrevious`: Boolean indicating if previous track is supported
     * - `canGoNext`: Boolean indicating if next track is supported
     * - `canSeek`: Boolean indicating if seeking is supported
     * - `volume`: Volume level (0-100)
     * - `albumArtUrl`: URL to album artwork
     *
     * **For album art transfer**, the body should contain:
     * - `player`: Player name (required)
     * - `transferringAlbumArt`: Boolean set to true
     * - `albumArtUrl`: URL to album artwork
     *
     * Example JSON for player list:
     * ```json
     * {
     *   "playerList": ["Spotify", "YouTube Music"],
     *   "supportAlbumArtPayload": true
     * }
     * ```
     *
     * Example JSON for metadata:
     * ```json
     * {
     *   "player": "Spotify",
     *   "title": "Song Title",
     *   "artist": "Artist Name",
     *   "isPlaying": true,
     *   "pos": 12345,
     *   "length": 180000
     * }
     * ```
     *
     * @param bodyJson JSON string containing MPRIS data
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createMprisPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createMprisRequest(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
