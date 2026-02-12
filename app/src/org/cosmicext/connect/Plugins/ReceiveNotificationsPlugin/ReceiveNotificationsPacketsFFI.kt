package org.cosmicext.connect.Plugins.ReceiveNotificationsPlugin

import org.cosmicext.connect.Core.NetworkPacket
import uniffi.cosmic_ext_connect_core.createNotificationRequestPacket

/**
 * FFI wrapper for creating ReceiveNotifications plugin packets
 *
 * The ReceiveNotifications plugin receives notifications from the desktop
 * and displays them on the Android device. This wrapper provides a clean
 * Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Request all current notifications from the remote device
 *
 * ## Usage
 *
 * **Requesting notifications:**
 * ```kotlin
 * val packet = ReceiveNotificationsPacketsFFI.createNotificationRequestPacket()
 * device.sendPacket(TransferPacket(packet))
 * ```
 *
 * This is typically called when the plugin initializes to fetch the current
 * notification state from the remote device (usually a desktop computer).
 *
 * @see ReceiveNotificationsPlugin
 */
object ReceiveNotificationsPacketsFFI {

    /**
     * Create a notification request packet
     *
     * Creates a packet requesting all current notifications from the remote device.
     * This is typically sent when the plugin initializes or when requesting a
     * refresh of notification state.
     *
     * The created packet will have:
     * - Type: `cconnect.notification.request`
     * - Body: `{"request": true}`
     *
     * The remote device will respond with all its current notifications,
     * which will be displayed on this Android device.
     *
     * @return NetworkPacket ready to send
     *
     * @throws CosmicExtConnectException if packet creation fails
     */
    fun createNotificationRequestPacket(): NetworkPacket {
        val ffiPacket = uniffi.cosmic_ext_connect_core.createNotificationRequestPacket()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
