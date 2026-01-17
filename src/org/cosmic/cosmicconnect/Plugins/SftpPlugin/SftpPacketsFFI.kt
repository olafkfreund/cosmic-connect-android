package org.cosmic.cosmicconnect.Plugins.SftpPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import uniffi.cosmic_connect_core.createSftpPacket

/**
 * FFI wrapper for creating SFTP plugin packets
 *
 * The SFTP plugin provides file system access over SSH File Transfer Protocol,
 * allowing the desktop to browse and access files on the Android device.
 * This wrapper provides a clean Kotlin API over the Rust FFI core functions.
 *
 * ## Features
 * - Send SFTP server connection details (IP, port, credentials, paths)
 * - Send error messages for permission or configuration issues
 *
 * ## Usage
 *
 * **Sending SFTP server info:**
 * ```kotlin
 * import org.json.JSONObject
 *
 * val serverInfo = JSONObject(mapOf(
 *     "ip" to "192.168.1.100",
 *     "port" to 1739,
 *     "user" to "sftpuser",
 *     "password" to "secret123",
 *     "path" to "/storage/emulated/0",
 *     "multiPaths" to listOf("/storage/emulated/0", "/storage/sdcard1"),
 *     "pathNames" to listOf("Internal Storage", "SD Card")
 * ))
 * val packet = SftpPacketsFFI.createSftpPacket(serverInfo.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * **Sending error message:**
 * ```kotlin
 * val errorPacket = JSONObject(mapOf(
 *     "errorMessage" to "Permission denied"
 * ))
 * val packet = SftpPacketsFFI.createSftpPacket(errorPacket.toString())
 * device.sendPacket(packet.toLegacyPacket())
 * ```
 *
 * @see SftpPlugin
 * @see SimpleSftpServer
 */
object SftpPacketsFFI {

    /**
     * Create an SFTP packet
     *
     * Creates a packet containing SFTP server connection details or
     * an error message.
     *
     * **For server connection info**, the body should contain:
     * - `ip`: Server IP address (required)
     * - `port`: Server port number (required)
     * - `user`: Username for SFTP authentication (required)
     * - `password`: Password for SFTP authentication (required)
     * - `path`: Root path for single mount point (required)
     * - `multiPaths`: Array of paths for multiple mount points (optional)
     * - `pathNames`: Array of display names for paths (optional, pairs with multiPaths)
     *
     * **For error messages**, the body should contain:
     * - `errorMessage`: Error description string
     *
     * Example JSON for server info:
     * ```json
     * {
     *   "ip": "192.168.1.100",
     *   "port": 1739,
     *   "user": "sftpuser",
     *   "password": "secret123",
     *   "path": "/storage/emulated/0",
     *   "multiPaths": ["/storage/emulated/0", "/storage/sdcard1"],
     *   "pathNames": ["Internal Storage", "SD Card"]
     * }
     * ```
     *
     * Example JSON for error:
     * ```json
     * {
     *   "errorMessage": "Permission denied"
     * }
     * ```
     *
     * @param bodyJson JSON string containing SFTP data
     * @return NetworkPacket ready to send
     *
     * @throws CosmicConnectException if packet creation fails
     * @throws CosmicConnectException if JSON parsing fails
     */
    fun createSftpPacket(bodyJson: String): NetworkPacket {
        val ffiPacket = uniffi.cosmic_connect_core.createSftpPacket(bodyJson)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}
