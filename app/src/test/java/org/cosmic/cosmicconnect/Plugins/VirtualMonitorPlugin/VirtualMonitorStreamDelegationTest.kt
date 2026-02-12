/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.VirtualMonitorPlugin

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ScreenSharePlugin
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming.StreamSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Tests for VirtualMonitorPlugin's delegation to ScreenSharePlugin for streaming,
 * activeStreamSession tracking, try-catch error handling, bounds-validated delegation,
 * and warning when ScreenSharePlugin is unavailable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VirtualMonitorStreamDelegationTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var screenSharePlugin: ScreenSharePlugin
    private lateinit var plugin: VirtualMonitorPlugin

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        ShadowLog.clear()
        context = RuntimeEnvironment.getApplication()

        screenSharePlugin = mockk(relaxed = true)

        device = mockk(relaxed = true)
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns screenSharePlugin
        every { device.deviceId } returns "test-device-id"

        plugin = VirtualMonitorPlugin(context, device)
    }

    @Test
    fun `active packet delegates to createStreamSession`() {
        val mockSession = mockk<StreamSession>(relaxed = true)
        every { screenSharePlugin.activeSession } returns mockSession

        val np = NetworkPacket(
            id = 1L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
                "refreshRate" to 60,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify { screenSharePlugin.createStreamSession(1920, 1080, 60, "h264") }
    }

    @Test
    fun `active packet uses default 60Hz when refreshRate missing`() {
        val np = NetworkPacket(
            id = 2L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 2560,
                "height" to 1440,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify { screenSharePlugin.createStreamSession(2560, 1440, 60, "h264") }
    }

    @Test
    fun `inactive packet delegates to stopStreamSession`() {
        val np = NetworkPacket(
            id = 3L,
            type = "cconnect.virtualmonitor",
            body = mapOf("isActive" to false),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify { screenSharePlugin.stopStreamSession() }
    }

    @Test
    fun `active packet with null width does not delegate`() {
        val np = NetworkPacket(
            id = 4L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "height" to 1080,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify(exactly = 0) { screenSharePlugin.createStreamSession(any(), any(), any(), any()) }
    }

    @Test
    fun `active packet with null height does not delegate`() {
        val np = NetworkPacket(
            id = 5L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify(exactly = 0) { screenSharePlugin.createStreamSession(any(), any(), any(), any()) }
    }

    @Test
    fun `no ScreenSharePlugin loaded does not crash`() {
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns null

        val np = NetworkPacket(
            id = 6L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))

        assertTrue(result) // still processes the status update
        assertEquals(true, plugin.isActive)
    }

    @Test
    fun `onDestroy stops delegated stream session`() {
        plugin.onDestroy()

        verify { screenSharePlugin.stopStreamSession() }
    }

    @Test
    fun `onDestroy with no ScreenSharePlugin does not crash`() {
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns null

        // Should not throw
        plugin.onDestroy()
    }

    @Test
    fun `getUiButtons empty when no activeStreamSession`() {
        // activeStreamSession is null by default
        val buttons = plugin.getUiButtons()
        assertTrue(buttons.isEmpty())
    }

    @Test
    fun `getUiButtons has entry when activeStreamSession exists`() {
        val mockSession = mockk<StreamSession>(relaxed = true)
        every { screenSharePlugin.activeSession } returns mockSession

        // Trigger delegation to set activeStreamSession
        val np = NetworkPacket(
            id = 10L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )
        plugin.onPacketReceived(TransferPacket(np))

        val buttons = plugin.getUiButtons()
        assertEquals(1, buttons.size)
        assertEquals(
            context.getString(org.cosmic.cosmicconnect.R.string.screenshare_viewer_virtualmonitor_title),
            buttons[0].name
        )
    }

    @Test
    fun `getUiButtons empty when no ScreenSharePlugin`() {
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns null

        val buttons = plugin.getUiButtons()
        assertTrue(buttons.isEmpty())
    }

    @Test
    fun `high refresh rate is passed through`() {
        val np = NetworkPacket(
            id = 7L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 3840,
                "height" to 2160,
                "refreshRate" to 120,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify { screenSharePlugin.createStreamSession(3840, 2160, 120, "h264") }
    }

    // ========== activeStreamSession tracking tests ==========

    @Test
    fun `activeStreamSession is set after successful delegation`() {
        val mockSession = mockk<StreamSession>(relaxed = true)
        every { screenSharePlugin.activeSession } returns mockSession

        val np = NetworkPacket(
            id = 20L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        assertNotNull(plugin.activeStreamSession)
        assertEquals(mockSession, plugin.activeStreamSession)
    }

    @Test
    fun `activeStreamSession cleared on stop`() {
        val mockSession = mockk<StreamSession>(relaxed = true)
        every { screenSharePlugin.activeSession } returns mockSession

        // First activate
        val activateNp = NetworkPacket(
            id = 21L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )
        plugin.onPacketReceived(TransferPacket(activateNp))
        assertNotNull(plugin.activeStreamSession)

        // Then stop
        val stopNp = NetworkPacket(
            id = 22L,
            type = "cconnect.virtualmonitor",
            body = mapOf("isActive" to false),
        )
        plugin.onPacketReceived(TransferPacket(stopNp))

        assertNull(plugin.activeStreamSession)
    }

    @Test
    fun `onDestroy clears activeStreamSession`() {
        val mockSession = mockk<StreamSession>(relaxed = true)
        every { screenSharePlugin.activeSession } returns mockSession

        // First activate
        val np = NetworkPacket(
            id = 23L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )
        plugin.onPacketReceived(TransferPacket(np))
        assertNotNull(plugin.activeStreamSession)

        // Destroy
        plugin.onDestroy()

        assertNull(plugin.activeStreamSession)
    }

    // ========== try-catch error handling tests ==========

    @Test
    fun `delegation exception is caught and does not crash`() {
        every { screenSharePlugin.createStreamSession(any(), any(), any(), any()) } throws RuntimeException("Stream error")

        val np = NetworkPacket(
            id = 30L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )

        // Should not throw
        val result = plugin.onPacketReceived(TransferPacket(np))

        assertTrue(result) // packet still processed
        assertEquals(true, plugin.isActive)
    }

    @Test
    fun `delegation exception logs error`() {
        every { screenSharePlugin.createStreamSession(any(), any(), any(), any()) } throws RuntimeException("Stream error")

        val np = NetworkPacket(
            id = 31L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        val errorLogs = ShadowLog.getLogsForTag("VirtualMonitorPlugin")
            .filter { it.type == android.util.Log.ERROR }
        assertTrue("Expected error log for delegation failure", errorLogs.isNotEmpty())
        assertTrue(errorLogs.any { it.msg.contains("Failed to delegate") })
    }

    // ========== ScreenSharePlugin-missing warning tests ==========

    @Test
    fun `missing ScreenSharePlugin logs warning when activate requested`() {
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns null

        val np = NetworkPacket(
            id = 40L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        val warnLogs = ShadowLog.getLogsForTag("VirtualMonitorPlugin")
            .filter { it.type == android.util.Log.WARN }
        assertTrue("Expected warning log when ScreenSharePlugin unavailable", warnLogs.isNotEmpty())
        assertTrue(warnLogs.any { it.msg.contains("Cannot activate virtual monitor") })
    }

    @Test
    fun `missing ScreenSharePlugin does not warn on inactive`() {
        every { device.getPlugin(ScreenSharePlugin::class.java) } returns null

        val np = NetworkPacket(
            id = 41L,
            type = "cconnect.virtualmonitor",
            body = mapOf("isActive" to false),
        )

        plugin.onPacketReceived(TransferPacket(np))

        val warnLogs = ShadowLog.getLogsForTag("VirtualMonitorPlugin")
            .filter { it.type == android.util.Log.WARN }
        assertFalse(
            "Should not warn on inactive when ScreenSharePlugin missing",
            warnLogs.any { it.msg.contains("Cannot activate") }
        )
    }

    // ========== Bounds-validated delegation tests ==========

    @Test
    fun `out-of-bounds width prevents delegation`() {
        val np = NetworkPacket(
            id = 50L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to -1,
                "height" to 1080,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        // width is null due to bounds check, so delegation should not happen
        verify(exactly = 0) { screenSharePlugin.createStreamSession(any(), any(), any(), any()) }
        assertNull(plugin.activeStreamSession)
    }

    @Test
    fun `out-of-bounds height prevents delegation`() {
        val np = NetworkPacket(
            id = 51L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 99999,
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        verify(exactly = 0) { screenSharePlugin.createStreamSession(any(), any(), any(), any()) }
        assertNull(plugin.activeStreamSession)
    }

    @Test
    fun `out-of-bounds refreshRate uses default 60Hz in delegation`() {
        val mockSession = mockk<StreamSession>(relaxed = true)
        every { screenSharePlugin.activeSession } returns mockSession

        val np = NetworkPacket(
            id = 52L,
            type = "cconnect.virtualmonitor",
            body = mapOf(
                "isActive" to true,
                "width" to 1920,
                "height" to 1080,
                "refreshRate" to 500, // out of bounds -> null -> defaults to 60
            ),
        )

        plugin.onPacketReceived(TransferPacket(np))

        // refreshRate is null (out of bounds), so delegation uses default 60
        verify { screenSharePlugin.createStreamSession(1920, 1080, 60, "h264") }
    }
}
