package org.cosmicext.connect.Plugins.PresenterPlugin

import org.cosmicext.connect.Core.NetworkPacket
import uniffi.cosmic_ext_connect_core.createPresenterPointer
import uniffi.cosmic_ext_connect_core.createPresenterStop

/**
 * FFI wrapper for creating Presenter plugin packets
 *
 * The Presenter plugin allows using an Android phone as a wireless presentation remote.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Pointer movement (simulates laser pointer)
 * - Stop presentation mode
 * - Keyboard commands (via mousepad.request packets)
 *
 * ## Usage
 *
 * ```kotlin
 * // Send pointer movement
 * val packet = PresenterPacketsFFI.createPointerMovement(10.5, -5.2)
 * device.sendPacket(packet)
 *
 * // Stop presentation
 * val stopPacket = PresenterPacketsFFI.createStop()
 * device.sendPacket(stopPacket)
 * ```
 *
 * @see PresenterPlugin
 * @see PresenterActivity
 */
object PresenterPacketsFFI {

    /**
     * Create a pointer movement packet
     *
     * Sends pointer delta movements to simulate a laser pointer on the remote screen.
     * The remote device will display a pointer that moves according to these deltas.
     *
     * @param dx Horizontal movement delta
     * @param dy Vertical movement delta
     * @return NetworkPacket ready to send
     *
     * @throws CosmicExtConnectException if packet creation fails
     */
    fun createPointerMovement(dx: Double, dy: Double): NetworkPacket {
        val ffiPacket = createPresenterPointer(dx, dy)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a stop presentation packet
     *
     * Stops presentation mode on the remote device, hiding the laser pointer.
     *
     * @return NetworkPacket ready to send
     *
     * @throws CosmicExtConnectException if packet creation fails
     */
    fun createStop(): NetworkPacket {
        val ffiPacket = createPresenterStop()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
