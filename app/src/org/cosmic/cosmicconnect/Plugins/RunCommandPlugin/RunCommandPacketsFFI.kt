package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.*
import uniffi.cosmic_connect_core.FfiPacket
import uniffi.cosmic_connect_core.createRuncommandExecute
import uniffi.cosmic_connect_core.createRuncommandRequestList
import uniffi.cosmic_connect_core.createRuncommandSetup

/**
 * FFI packet factory for RunCommand plugin packets.
 *
 * This object provides factory methods to create RunCommand plugin packets using the
 * Rust cosmic-connect-core FFI layer. All packets are created through UniFFI bindings
 * and follow the COSMIC Connect protocol specification.
 *
 * ## Packet Types
 *
 * ### Request Command List
 * ```json
 * {
 *     "type": "cconnect.runcommand.request",
 *     "body": {
 *         "requestCommandList": true
 *     }
 * }
 * ```
 *
 * ### Execute Command
 * ```json
 * {
 *     "type": "cconnect.runcommand.request",
 *     "body": {
 *         "key": "cmd_id"
 *     }
 * }
 * ```
 *
 * ### Setup Command Configuration
 * ```json
 * {
 *     "type": "cconnect.runcommand.request",
 *     "body": {
 *         "setup": true
 *     }
 * }
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Request command list from desktop
 * val requestPacket = RunCommandPacketsFFI.createRequestCommandList()
 * device.sendPacket(requestPacket)
 *
 * // Execute a specific command
 * val executePacket = RunCommandPacketsFFI.createExecuteCommand("backup_home")
 * device.sendPacket(executePacket)
 *
 * // Open command setup UI on desktop
 * val setupPacket = RunCommandPacketsFFI.createSetupRequest()
 * device.sendPacket(setupPacket)
 * ```
 *
 * @see uniffi.cosmic_connect_core.createRuncommandRequestList
 * @see uniffi.cosmic_connect_core.createRuncommandExecute
 * @see uniffi.cosmic_connect_core.createRuncommandSetup
 */
object RunCommandPacketsFFI {

    /**
     * Create a packet to request the command list from the remote device.
     *
     * This packet asks the desktop to send back its list of configured commands.
     * The desktop will respond with a `cconnect.runcommand` packet containing
     * the command list as a JSON string.
     *
     * ## Protocol Details
     * - **Type**: `cconnect.runcommand.request`
     * - **Body**: `{"requestCommandList": true}`
     * - **Direction**: Android → Desktop
     * - **Response**: Desktop sends `cconnect.runcommand` with command list
     *
     * ## Example Response
     * ```json
     * {
     *     "type": "cconnect.runcommand",
     *     "body": {
     *         "commandList": "{\"cmd1\":{\"name\":\"List Files\",\"command\":\"ls -la\"},\"cmd2\":{...}}",
     *         "canAddCommand": true
     *     }
     * }
     * ```
     *
     * @return NetworkPacket requesting command list
     * @throws Exception if packet creation fails
     */
    @JvmStatic
    fun createRequestCommandList(): NetworkPacket {
        val ffiPacket: FfiPacket = createRuncommandRequestList()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a packet to execute a specific command on the remote device.
     *
     * The command key must correspond to a command ID configured on the desktop.
     * If the command doesn't exist, the desktop will log a warning but won't crash.
     *
     * ## Protocol Details
     * - **Type**: `cconnect.runcommand.request`
     * - **Body**: `{"key": "<command_key>"}`
     * - **Direction**: Android → Desktop
     * - **Response**: None (command executes silently)
     *
     * ## Security
     * - Only pre-configured commands can be executed
     * - No arbitrary shell command execution
     * - Commands run with desktop user's permissions
     *
     * @param commandKey The unique identifier of the command to execute
     * @return NetworkPacket requesting command execution
     * @throws Exception if packet creation fails or commandKey is invalid
     *
     * @see RunCommandPlugin.runCommand
     */
    @JvmStatic
    fun createExecuteCommand(commandKey: String): NetworkPacket {
        require(commandKey.isNotBlank()) { "Command key cannot be blank" }
        val ffiPacket: FfiPacket = createRuncommandExecute(commandKey)
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }

    /**
     * Create a packet to request the desktop to open command setup interface.
     *
     * This packet asks the desktop to open its command configuration UI, allowing
     * the user to add, edit, or remove commands. Useful for onboarding flows or
     * settings screens.
     *
     * ## Protocol Details
     * - **Type**: `cconnect.runcommand.request`
     * - **Body**: `{"setup": true}`
     * - **Direction**: Android → Desktop
     * - **Response**: None (desktop opens UI)
     *
     * ## Desktop Behavior
     * The desktop will:
     * 1. Open the command configuration dialog
     * 2. Show existing commands
     * 3. Allow user to add/edit/remove commands
     * 4. Automatically send updated command list when dialog closes
     *
     * @return NetworkPacket requesting setup UI
     * @throws Exception if packet creation fails
     *
     * @see RunCommandPlugin.requestSetup
     */
    @JvmStatic
    fun createSetupRequest(): NetworkPacket {
        val ffiPacket: FfiPacket = createRuncommandSetup()
        return NetworkPacket.fromFfiPacket(ffiPacket)
    }
}

/**
 * Extension properties for type-safe RunCommand packet inspection.
 *
 * These properties allow checking packet types without string comparisons,
 * providing compile-time safety and better IDE support.
 *
 * ## Usage Example
 *
 * ```kotlin
 * override fun onPacketReceived(np: NetworkPacket): Boolean {
 *     return when {
 *         np.isRunCommandList -> {
 *             val commandListJson = np.commandListJson
 *             handleCommandList(commandListJson)
 *             true
 *         }
 *         else -> false
 *     }
 * }
 * ```
 */

/**
 * Check if this packet is a RunCommand command list response.
 *
 * A command list packet contains the desktop's configured commands as a JSON string.
 * The JSON is double-encoded: the `commandList` field contains a JSON string
 * representing a map of command IDs to command objects.
 *
 * ## Packet Structure
 * ```json
 * {
 *     "type": "cconnect.runcommand",
 *     "body": {
 *         "commandList": "{\"cmd1\":{\"name\":\"...\",\"command\":\"...\"},...}",
 *         "canAddCommand": true
 *     }
 * }
 * ```
 *
 * @receiver NetworkPacket to inspect
 * @return true if packet type is "cconnect.runcommand"
 */
val NetworkPacket.isRunCommandList: Boolean
    get() = type == "cconnect.runcommand"

/**
 * Check if this packet is a RunCommand request packet.
 *
 * Request packets are sent from desktop to Android and can contain:
 * - Command list request: `{"requestCommandList": true}`
 * - Execute command: `{"key": "cmd_id"}`
 * - Setup request: `{"setup": true}`
 *
 * @receiver NetworkPacket to inspect
 * @return true if packet type is "cconnect.runcommand.request"
 */
val NetworkPacket.isRunCommandRequest: Boolean
    get() = type == "cconnect.runcommand.request"

/**
 * Check if this packet is a command list request.
 *
 * @receiver NetworkPacket to inspect
 * @return true if packet contains `{"requestCommandList": true}`
 */
val NetworkPacket.isCommandListRequest: Boolean
    get() = isRunCommandRequest && getBoolean("requestCommandList", false)

/**
 * Check if this packet is a command execution request.
 *
 * @receiver NetworkPacket to inspect
 * @return true if packet contains a "key" field
 */
val NetworkPacket.isCommandExecute: Boolean
    get() = isRunCommandRequest && has("key")

/**
 * Check if this packet is a setup request.
 *
 * @receiver NetworkPacket to inspect
 * @return true if packet contains `{"setup": true}`
 */
val NetworkPacket.isSetupRequest: Boolean
    get() = isRunCommandRequest && getBoolean("setup", false)

/**
 * Extract the command list JSON string from a RunCommand list packet.
 *
 * The command list is stored as a JSON-encoded string in the "commandList" field.
 * You need to parse this string to get the actual command map.
 *
 * ## Example
 * ```kotlin
 * val commandListJson = packet.commandListJson
 * val commands: Map<String, Command> = JSONObject(commandListJson).let { json ->
 *     json.keys().asSequence().associateWith { key ->
 *         val cmdObj = json.getJSONObject(key)
 *         Command(cmdObj.getString("name"), cmdObj.getString("command"))
 *     }
 * }
 * ```
 *
 * @receiver NetworkPacket command list packet
 * @return JSON string containing command map, or null if not present
 */
val NetworkPacket.commandListJson: String?
    get() = if (isRunCommandList) getString("commandList") else null

/**
 * Extract the command key from a command execution request packet.
 *
 * @receiver NetworkPacket execute command packet
 * @return Command key/ID to execute, or null if not present
 */
val NetworkPacket.commandKey: String?
    get() = if (isCommandExecute) getString("key") else null

/**
 * Check if the desktop allows adding commands remotely.
 *
 * When true, the Android app could potentially send commands to be added
 * (though this is not currently implemented in the protocol).
 *
 * @receiver NetworkPacket command list packet
 * @return true if desktop allows adding commands, false otherwise
 */
val NetworkPacket.canAddCommand: Boolean
    get() = if (isRunCommandList) getBoolean("canAddCommand", false) else false
