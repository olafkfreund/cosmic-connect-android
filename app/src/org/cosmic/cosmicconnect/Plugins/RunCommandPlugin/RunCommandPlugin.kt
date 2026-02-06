/*
 * SPDX-FileCopyrightText: 2015 Aleix Pol Gonzalez <aleixpol@kde.org>
 * SPDX-FileCopyrightText: 2015 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 FFI Migration by cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.collections4.iterators.IteratorIterable
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsFragment
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * RunCommandPlugin - Remote command execution from Android
 *
 * This plugin allows Android users to trigger pre-configured shell commands
 * on their COSMIC Desktop remotely. Commands are configured on the desktop
 * and sent to Android as a list. Android displays them in UI, widgets, and
 * device controls (Android 11+).
 *
 * ## Features
 *
 * - **Command List**: Receive and display commands from desktop
 * - **Command Execution**: Trigger desktop commands with a tap
 * - **Widget Support**: Execute commands from home screen widgets
 * - **Device Controls**: Quick access from power menu (Android 11+)
 * - **Setup Mode**: Open desktop configuration UI remotely
 * - **Offline Caching**: Commands cached in SharedPreferences
 *
 * ## Protocol
 *
 * **Packet Types:**
 * - `cconnect.runcommand` (receive) - Command list from desktop
 * - `cconnect.runcommand.request` (send) - Command execution requests
 *
 * **Direction:**
 * - Desktop → Android: Send command list
 * - Android → Desktop: Request list, execute command, open setup
 *
 * ## Security
 *
 * - Only pre-configured commands can be executed
 * - No arbitrary command injection
 * - Commands execute with desktop user's permissions
 * - Requires device pairing
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Request command list from desktop
 * plugin.requestCommandList()
 *
 * // Execute a specific command
 * plugin.runCommand("cmd-screenshot-123")
 *
 * // Open command setup UI on desktop
 * plugin.sendSetupPacket()
 * ```
 *
 * @see RunCommandPacketsFFI
 * @see RunCommandActivity
 * @see RunCommandWidgetProvider
 * @see RunCommandControlsProviderService
 */
class RunCommandPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): RunCommandPlugin
    }

    companion object {
        private const val TAG = "RunCommandPlugin"
        private const val PACKET_TYPE_RUNCOMMAND = "cconnect.runcommand"
        private const val PACKET_TYPE_RUNCOMMAND_REQUEST = "cconnect.runcommand.request"
        const val KEY_COMMANDS_PREFERENCE = "commands_preference_"
    }

    // ========================================================================
    // State Management
    // ========================================================================

    private val _commandList = ArrayList<JSONObject>()
    private val callbacks = ArrayList<CommandsChangedCallback>()
    private val _commandItems = ArrayList<CommandEntry>()

    private lateinit var sharedPreferences: SharedPreferences

    private var canAddCommand = false

    /**
     * Callback interface for command list updates
     */
    interface CommandsChangedCallback {
        fun update()
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Add a callback to be notified when the command list changes
     *
     * @param newCallback Callback to add
     */
    fun addCommandsUpdatedCallback(newCallback: CommandsChangedCallback) {
        callbacks.add(newCallback)
    }

    /**
     * Remove a previously registered callback
     *
     * @param theCallback Callback to remove
     */
    fun removeCommandsUpdatedCallback(theCallback: CommandsChangedCallback) {
        callbacks.remove(theCallback)
    }

    /**
     * Get the raw command list as JSON objects
     *
     * @return List of JSONObject commands
     */
    val commandList: ArrayList<JSONObject>
        get() = _commandList

    /**
     * Get the command list as structured CommandEntry objects
     *
     * @return List of CommandEntry objects, sorted by name
     */
    val commandItems: ArrayList<CommandEntry>
        get() = _commandItems

    /**
     * Check if the desktop supports adding commands remotely
     *
     * @return true if setup mode is available, false otherwise
     */
    fun canAddCommand(): Boolean = canAddCommand

    /**
     * Execute a specific command on the desktop
     *
     * Sends a command execution request to the desktop using the command's
     * unique key. The command must have been previously configured on the desktop.
     *
     * ## Behavior
     * - Fire-and-forget (no response expected)
     * - Command executes immediately on desktop
     * - Runs in background with desktop user's permissions
     *
     * ## Security
     * - Only pre-configured commands can be executed
     * - Invalid command keys are ignored by desktop
     * - No arbitrary shell command execution
     *
     * @param cmdKey Unique identifier of the command to execute
     */
    fun runCommand(cmdKey: String) {
        val packet = RunCommandPacketsFFI.createExecuteCommand(cmdKey)
        device.sendPacket(TransferPacket(packet))
    }

    /**
     * Request the desktop to open command configuration UI
     *
     * Opens the command setup interface on the desktop, allowing the user
     * to add, edit, or remove commands. After configuration changes, the
     * desktop automatically sends an updated command list.
     *
     * ## Availability
     * Only works if desktop indicated `canAddCommand: true` in the command list.
     * Check {@link #canAddCommand()} before calling this.
     *
     * @see canAddCommand
     */
    fun sendSetupPacket() {
        val packet = RunCommandPacketsFFI.createSetupRequest()
        device.sendPacket(TransferPacket(packet))
    }

    // ========================================================================
    // Plugin Lifecycle
    // ========================================================================

    override fun onCreate(): Boolean {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        requestCommandList()
        return true
    }

    // ========================================================================
    // Packet Handling
    // ========================================================================

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet

        if (np.isRunCommandList) {
            handleCommandListPacket(tp)
            return true
        }

        return false
    }

    private fun handleCommandListPacket(tp: TransferPacket) {
        val np = tp.packet

        _commandList.clear()
        _commandItems.clear()

        try {
            val commandListJson = np.body["commandList"] as? String ?: return
            val obj = JSONObject(commandListJson)

            for (key in IteratorIterable(obj.keys())) {
                val commandObj = obj.getJSONObject(key)
                commandObj.put("key", key)
                _commandList.add(commandObj)

                try {
                    _commandItems.add(CommandEntry(commandObj))
                } catch (e: JSONException) {
                    Log.e(TAG, "Error parsing command entry: $key", e)
                }
            }

            // Sort commands alphabetically by name
            _commandItems.sortBy { it.name }

            // Cache commands for offline access (Android 11+ for device controls)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val array = JSONArray()
                for (command in _commandList) {
                    array.put(command)
                }

                sharedPreferences.edit()
                    .putString(KEY_COMMANDS_PREFERENCE + device.deviceId, array.toString())
                    .apply()
            }

            // Refresh widgets to show updated command list
            forceRefreshWidgets(context)

        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing command list JSON", e)
        }

        // Notify all registered callbacks
        for (callback in callbacks) {
            callback.update()
        }

        // Notify device that plugins have changed (updates UI)
        device.onPluginsChanged()

        // Check if desktop supports adding commands
        canAddCommand = (np.body["canAddCommand"] as? Boolean) ?: false
    }

    /**
     * Request the command list from the desktop
     *
     * Sends a request packet to the desktop asking for its configured commands.
     * The desktop responds with a `cconnect.runcommand` packet containing
     * the command list as a JSON string.
     */
    private fun requestCommandList() {
        val packet = RunCommandPacketsFFI.createRequestCommandList()
        device.sendPacket(TransferPacket(packet))
    }

    // ========================================================================
    // Plugin Metadata
    // ========================================================================

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_runcommand)

    override val description: String
        get() = context.getString(R.string.pref_plugin_runcommand_desc)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_RUNCOMMAND)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_RUNCOMMAND_REQUEST)

    override fun hasSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return PluginSettingsFragment.newInstance(pluginKey, R.xml.runcommand_preferences)
    }

    override fun getUiButtons(): List<PluginUiButton> {
        return listOf(
            PluginUiButton(
                context.getString(R.string.pref_plugin_runcommand),
                R.drawable.run_command_plugin_icon_24dp
            ) { parentActivity ->
                val intent = Intent(parentActivity, RunCommandActivity::class.java)
                intent.putExtra("deviceId", device.deviceId)
                parentActivity.startActivity(intent)
            }
        )
    }
}
