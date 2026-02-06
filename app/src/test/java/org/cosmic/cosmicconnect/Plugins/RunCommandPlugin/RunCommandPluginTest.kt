/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import androidx.preference.PreferenceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RunCommandPluginTest {

    private lateinit var plugin: RunCommandPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()

        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"

        plugin = RunCommandPlugin(context, mockDevice)

        // Initialize sharedPreferences via reflection — can't call onCreate()
        // because it invokes requestCommandList() which uses FFI
        val field = RunCommandPlugin::class.java.getDeclaredField("sharedPreferences")
        field.isAccessible = true
        field.set(plugin, PreferenceManager.getDefaultSharedPreferences(context))
    }

    // ========================================================================
    // Extension properties — isRunCommandList
    // ========================================================================

    @Test
    fun `isRunCommandList true for cconnect runcommand`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = emptyMap()
        )
        assertTrue(packet.isRunCommandList)
    }

    @Test
    fun `isRunCommandList false for wrong type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = emptyMap()
        )
        assertFalse(packet.isRunCommandList)
    }

    // ========================================================================
    // Extension properties — isRunCommandRequest
    // ========================================================================

    @Test
    fun `isRunCommandRequest true for request type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = emptyMap()
        )
        assertTrue(packet.isRunCommandRequest)
    }

    @Test
    fun `isRunCommandRequest false for non-request type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = emptyMap()
        )
        assertFalse(packet.isRunCommandRequest)
    }

    // ========================================================================
    // Extension properties — isCommandListRequest
    // ========================================================================

    @Test
    fun `isCommandListRequest true when requestCommandList is true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = mapOf("requestCommandList" to true)
        )
        assertTrue(packet.isCommandListRequest)
    }

    @Test
    fun `isCommandListRequest false when requestCommandList missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = emptyMap()
        )
        assertFalse(packet.isCommandListRequest)
    }

    // ========================================================================
    // Extension properties — isCommandExecute
    // ========================================================================

    @Test
    fun `isCommandExecute true when key is present`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = mapOf("key" to "cmd-1")
        )
        assertTrue(packet.isCommandExecute)
    }

    @Test
    fun `isCommandExecute false when key is missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = emptyMap()
        )
        assertFalse(packet.isCommandExecute)
    }

    // ========================================================================
    // Extension properties — isSetupRequest
    // ========================================================================

    @Test
    fun `isSetupRequest true when setup is true`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = mapOf("setup" to true)
        )
        assertTrue(packet.isSetupRequest)
    }

    @Test
    fun `isSetupRequest false when setup is missing`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = emptyMap()
        )
        assertFalse(packet.isSetupRequest)
    }

    // ========================================================================
    // Extension properties — commandListJson
    // ========================================================================

    @Test
    fun `commandListJson returns json string for runcommand packet`() {
        val jsonStr = """{"cmd1":{"name":"List","command":"ls"}}"""
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to jsonStr)
        )
        assertEquals(jsonStr, packet.commandListJson)
    }

    @Test
    fun `commandListJson returns null for wrong packet type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("commandList" to "{}")
        )
        assertNull(packet.commandListJson)
    }

    // ========================================================================
    // Extension properties — commandKey
    // ========================================================================

    @Test
    fun `commandKey returns key from execute packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = mapOf("key" to "cmd-screenshot")
        )
        assertEquals("cmd-screenshot", packet.commandKey)
    }

    @Test
    fun `commandKey returns null when not execute packet`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand.request",
            body = mapOf("setup" to true)
        )
        assertNull(packet.commandKey)
    }

    // ========================================================================
    // Extension properties — canAddCommand
    // ========================================================================

    @Test
    fun `canAddCommand returns true when set`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("canAddCommand" to true)
        )
        assertTrue(packet.canAddCommand)
    }

    @Test
    fun `canAddCommand returns false when not set`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = emptyMap()
        )
        assertFalse(packet.canAddCommand)
    }

    @Test
    fun `canAddCommand returns false for wrong packet type`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = mapOf("canAddCommand" to true)
        )
        assertFalse(packet.canAddCommand)
    }

    // ========================================================================
    // onPacketReceived — command list handling
    // ========================================================================

    @Test
    fun `onPacketReceived with command list parses commands`() {
        val commandJson = JSONObject().apply {
            put("cmd1", JSONObject().put("name", "Screenshot").put("command", "scrot"))
            put("cmd2", JSONObject().put("name", "Lock Screen").put("command", "loginctl lock-session"))
        }
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to commandJson.toString())
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)

        assertEquals(2, plugin.commandItems.size)
        // Should be sorted alphabetically by name
        assertEquals("Lock Screen", plugin.commandItems[0].name)
        assertEquals("Screenshot", plugin.commandItems[1].name)
    }

    @Test
    fun `onPacketReceived with command list notifies callbacks`() {
        val callback = mockk<RunCommandPlugin.CommandsChangedCallback>(relaxed = true)
        plugin.addCommandsUpdatedCallback(callback)

        val commandJson = JSONObject().apply {
            put("cmd1", JSONObject().put("name", "Test").put("command", "echo test"))
        }
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to commandJson.toString())
        )

        plugin.onPacketReceived(TransferPacket(packet))

        verify { callback.update() }
    }

    @Test
    fun `onPacketReceived command list sets canAddCommand`() {
        val commandJson = JSONObject().apply {
            put("cmd1", JSONObject().put("name", "Test").put("command", "echo test"))
        }
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf(
                "commandList" to commandJson.toString(),
                "canAddCommand" to true
            )
        )

        plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(plugin.canAddCommand())
    }

    @Test
    fun `onPacketReceived command list clears previous commands`() {
        // First: send 2 commands
        val firstJson = JSONObject().apply {
            put("cmd1", JSONObject().put("name", "A").put("command", "a"))
            put("cmd2", JSONObject().put("name", "B").put("command", "b"))
        }
        plugin.onPacketReceived(TransferPacket(NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to firstJson.toString())
        )))
        assertEquals(2, plugin.commandItems.size)

        // Second: send 1 command — should replace, not append
        val secondJson = JSONObject().apply {
            put("cmd3", JSONObject().put("name", "C").put("command", "c"))
        }
        plugin.onPacketReceived(TransferPacket(NetworkPacket(
            id = 2L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to secondJson.toString())
        )))
        assertEquals(1, plugin.commandItems.size)
        assertEquals("C", plugin.commandItems[0].name)
    }

    @Test
    fun `onPacketReceived with empty command list results in empty items`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to "{}")
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
        assertTrue(plugin.commandItems.isEmpty())
    }

    @Test
    fun `onPacketReceived wrong packet type returns false`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.battery",
            body = emptyMap()
        )
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertFalse(result)
    }

    @Test
    fun `onPacketReceived with missing commandList body does not crash`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = emptyMap()
        )
        // Should return true (isRunCommandList matches) but not crash
        val result = plugin.onPacketReceived(TransferPacket(packet))
        assertTrue(result)
        assertTrue(plugin.commandItems.isEmpty())
    }

    // ========================================================================
    // Callback management
    // ========================================================================

    @Test
    fun `removeCommandsUpdatedCallback stops notifications`() {
        val callback = mockk<RunCommandPlugin.CommandsChangedCallback>(relaxed = true)
        plugin.addCommandsUpdatedCallback(callback)
        plugin.removeCommandsUpdatedCallback(callback)

        val commandJson = JSONObject().apply {
            put("cmd1", JSONObject().put("name", "Test").put("command", "test"))
        }
        plugin.onPacketReceived(TransferPacket(NetworkPacket(
            id = 1L,
            type = "cconnect.runcommand",
            body = mapOf("commandList" to commandJson.toString())
        )))

        verify(exactly = 0) { callback.update() }
    }

    // ========================================================================
    // CommandEntry parsing
    // ========================================================================

    @Test
    fun `CommandEntry parses from JSONObject`() {
        val json = JSONObject().apply {
            put("name", "Screenshot")
            put("command", "scrot -d 3")
            put("key", "cmd-screenshot")
        }
        val entry = CommandEntry(json)

        assertEquals("Screenshot", entry.name)
        assertEquals("scrot -d 3", entry.command)
        assertEquals("cmd-screenshot", entry.key)
    }

    @Test(expected = JSONException::class)
    fun `CommandEntry throws on missing name`() {
        val json = JSONObject().apply {
            put("command", "ls")
            put("key", "cmd-1")
        }
        CommandEntry(json)
    }

    @Test(expected = JSONException::class)
    fun `CommandEntry throws on missing command`() {
        val json = JSONObject().apply {
            put("name", "List")
            put("key", "cmd-1")
        }
        CommandEntry(json)
    }

    @Test(expected = JSONException::class)
    fun `CommandEntry throws on missing key`() {
        val json = JSONObject().apply {
            put("name", "List")
            put("command", "ls")
        }
        CommandEntry(json)
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains cconnect runcommand`() {
        assertArrayEquals(arrayOf("cconnect.runcommand"), plugin.supportedPacketTypes)
    }

    @Test
    fun `outgoingPacketTypes contains cconnect runcommand request`() {
        assertArrayEquals(arrayOf("cconnect.runcommand.request"), plugin.outgoingPacketTypes)
    }

    @Test
    fun `hasSettings returns true`() {
        assertTrue(plugin.hasSettings())
    }
}
