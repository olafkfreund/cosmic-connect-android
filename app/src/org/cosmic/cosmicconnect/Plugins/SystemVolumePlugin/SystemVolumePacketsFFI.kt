package org.cosmic.cosmicconnect.Plugins.SystemVolumePlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createSystemvolumeVolume
import uniffi.cosmic_connect_core.createSystemvolumeMute
import uniffi.cosmic_connect_core.createSystemvolumeEnable
import uniffi.cosmic_connect_core.createSystemvolumeRequestSinks

/**
 * FFI wrapper for creating SystemVolume plugin packets
 *
 * The SystemVolume plugin allows controlling audio sinks (volume, mute, default) on the remote device.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Volume control for audio sinks
 * - Mute/unmute audio sinks
 * - Set default audio sink
 * - Request list of available sinks
 *
 * ## Usage
 *
 * ```kotlin
 * // Set volume to 75%
 * val volumePacket = SystemVolumePacketsFFI.createVolumeRequest("Speaker", 75)
 * device.sendPacket(volumePacket)
 *
 * // Mute headphones
 * val mutePacket = SystemVolumePacketsFFI.createMuteRequest("Headphones", true)
 * device.sendPacket(mutePacket)
 *
 * // Set HDMI as default sink
 * val enablePacket = SystemVolumePacketsFFI.createEnableRequest("HDMI Output")
 * device.sendPacket(enablePacket)
 *
 * // Request list of sinks
 * val listPacket = SystemVolumePacketsFFI.createSinkListRequest()
 * device.sendPacket(listPacket)
 * ```
 *
 * @see SystemVolumePlugin
 * @see Sink
 */
object SystemVolumePacketsFFI {

    /**
     * Create a volume control request packet
     *
     * Requests the remote device to change the volume of a specific audio sink.
     *
     * @param sinkName Name of the audio sink (e.g., "Speaker", "Headphones")
     * @param volume Volume level (0-100)
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createVolumeRequest(sinkName: String, volume: Int): NetworkPacket {
        val ffiPacket = createSystemvolumeVolume(sinkName, volume)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a mute control request packet
     *
     * Requests the remote device to mute or unmute a specific audio sink.
     *
     * @param sinkName Name of the audio sink (e.g., "Speaker", "Headphones")
     * @param muted True to mute, false to unmute
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createMuteRequest(sinkName: String, muted: Boolean): NetworkPacket {
        val ffiPacket = createSystemvolumeMute(sinkName, muted)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create an enable (set default) request packet
     *
     * Requests the remote device to set a specific audio sink as the default.
     *
     * @param sinkName Name of the audio sink to set as default (e.g., "HDMI Output")
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createEnableRequest(sinkName: String): NetworkPacket {
        val ffiPacket = createSystemvolumeEnable(sinkName)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a sink list request packet
     *
     * Requests the remote device to send the list of available audio sinks.
     * The response will contain sink information including names, volumes, and mute states.
     *
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     */
    fun createSinkListRequest(): NetworkPacket {
        val ffiPacket = createSystemvolumeRequestSinks()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
