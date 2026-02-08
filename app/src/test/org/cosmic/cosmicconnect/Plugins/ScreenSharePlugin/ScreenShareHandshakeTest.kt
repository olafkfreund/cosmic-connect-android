/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin

import android.app.Application
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.DeviceType
import org.cosmic.cosmicconnect.PluginManager
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenShareHandshakeTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var plugin: ScreenSharePlugin

    private val sentPackets = mutableListOf<TransferPacket>()

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        context = RuntimeEnvironment.getApplication()

        val deviceInfo = DeviceInfo(
            id = "test-device-id",
            name = "Test Desktop",
            type = DeviceType.DESKTOP,
            protocolVersion = 7,
            incomingCapabilities = setOf("cconnect.screenshare", "cconnect.screenshare.start", "cconnect.screenshare.stop"),
            outgoingCapabilities = setOf("cconnect.screenshare.request", "cconnect.screenshare.ready"),
        )

        device = mockk<Device>(relaxed = true)
        every { device.deviceId } returns "test-device-id"
        every { device.name } returns "Test Desktop"
        every { device.sendPacket(capture(sentPackets)) } returns Unit

        plugin = ScreenSharePlugin(context, device)
    }

    @Test
    fun testStatusPacketUpdatesState() {
        val statusPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.screenshare",
            body = mapOf(
                "isSharing" to true,
                "width" to 1920,
                "height" to 1080,
                "codec" to "h264",
                "fps" to 30,
                "direction" to "desktop_to_phone",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(statusPacket))

        assertTrue(result)
        assertEquals(true, plugin.isSharing)
        assertEquals(1920, plugin.width)
        assertEquals(1080, plugin.height)
        assertEquals("h264", plugin.codec)
        assertEquals(30, plugin.fps)
        assertEquals("desktop_to_phone", plugin.direction)
    }

    @Test
    fun testStartPacketCreatesSessionAndSendsReady() {
        val startPacket = NetworkPacket(
            id = 2L,
            type = "cconnect.screenshare.start",
            body = mapOf(
                "width" to 1920,
                "height" to 1080,
                "codec" to "h264",
                "fps" to 30,
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(startPacket))

        assertTrue(result)
        assertNotNull(plugin.activeSession)
        assertTrue(plugin.activeSession!!.tcpPort > 0)

        // Verify ready packet was sent
        assertEquals(1, sentPackets.size)
        val readyPacket = sentPackets[0].packet
        assertEquals("cconnect.screenshare.ready", readyPacket.type)
        assertEquals(plugin.activeSession!!.tcpPort, (readyPacket.body["tcpPort"] as Number).toInt())

        // Verify plugin state updated
        assertEquals(true, plugin.isSharing)
        assertEquals(1920, plugin.width)
        assertEquals(1080, plugin.height)
        assertEquals("h264", plugin.codec)
        assertEquals(30, plugin.fps)
        assertEquals("desktop_to_phone", plugin.direction)

        // Cleanup
        plugin.stopStreamSession()
    }

    @Test
    fun testStartPacketDefaultValues() {
        val startPacket = NetworkPacket(
            id = 3L,
            type = "cconnect.screenshare.start",
            body = mapOf<String, Any>(),
        )

        plugin.onPacketReceived(TransferPacket(startPacket))

        // Should use defaults
        assertEquals(1920, plugin.width)
        assertEquals(1080, plugin.height)
        assertEquals("h264", plugin.codec)
        assertEquals(30, plugin.fps)

        plugin.stopStreamSession()
    }

    @Test
    fun testStopPacketClearsSession() {
        // First start a session
        val startPacket = NetworkPacket(
            id = 4L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 1920, "height" to 1080, "codec" to "h264", "fps" to 30),
        )
        plugin.onPacketReceived(TransferPacket(startPacket))
        assertNotNull(plugin.activeSession)

        sentPackets.clear()

        // Now stop
        val stopPacket = NetworkPacket(
            id = 5L,
            type = "cconnect.screenshare.stop",
            body = mapOf<String, Any>(),
        )
        val result = plugin.onPacketReceived(TransferPacket(stopPacket))

        assertTrue(result)
        assertNull(plugin.activeSession)
        assertEquals(false, plugin.isSharing)
    }

    @Test
    fun testStartPacketReplacesExistingSession() {
        // Start first session
        val startPacket1 = NetworkPacket(
            id = 6L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 1280, "height" to 720, "codec" to "h264", "fps" to 30),
        )
        plugin.onPacketReceived(TransferPacket(startPacket1))
        val firstPort = plugin.activeSession!!.tcpPort

        sentPackets.clear()

        // Start second session — should replace
        val startPacket2 = NetworkPacket(
            id = 7L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 1920, "height" to 1080, "codec" to "h264", "fps" to 60),
        )
        plugin.onPacketReceived(TransferPacket(startPacket2))

        assertNotNull(plugin.activeSession)
        assertEquals(1920, plugin.width)
        assertEquals(60, plugin.fps)

        // Ready packet should have new port
        assertEquals(1, sentPackets.size)
        val readyPort = (sentPackets[0].packet.body["tcpPort"] as Number).toInt()
        assertTrue(readyPort > 0)

        plugin.stopStreamSession()
    }

    @Test
    fun testUnknownPacketTypeReturnsFalse() {
        val unknownPacket = NetworkPacket(
            id = 8L,
            type = "cconnect.something.else",
            body = mapOf<String, Any>(),
        )

        val result = plugin.onPacketReceived(TransferPacket(unknownPacket))
        assertFalse(result)
    }

    @Test
    fun testListenerNotifiedOnStreamStart() {
        var notifiedWidth = 0
        var notifiedHeight = 0
        var notifiedCodec = ""
        var notifiedFps = 0

        plugin.addListener(object : ScreenSharePlugin.Listener {
            override fun onSharingStateChanged(
                isSharing: Boolean, width: Int?, height: Int?, codec: String?, fps: Int?, direction: String
            ) {}

            override fun onStreamStartRequested(width: Int, height: Int, codec: String, fps: Int) {
                notifiedWidth = width
                notifiedHeight = height
                notifiedCodec = codec
                notifiedFps = fps
            }
        })

        val startPacket = NetworkPacket(
            id = 9L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 2560, "height" to 1440, "codec" to "h264", "fps" to 60),
        )
        plugin.onPacketReceived(TransferPacket(startPacket))

        assertEquals(2560, notifiedWidth)
        assertEquals(1440, notifiedHeight)
        assertEquals("h264", notifiedCodec)
        assertEquals(60, notifiedFps)

        plugin.stopStreamSession()
    }

    @Test
    fun testListenerNotifiedOnStreamStop() {
        var stopCalled = false

        plugin.addListener(object : ScreenSharePlugin.Listener {
            override fun onSharingStateChanged(
                isSharing: Boolean, width: Int?, height: Int?, codec: String?, fps: Int?, direction: String
            ) {}

            override fun onStreamStopped() {
                stopCalled = true
            }
        })

        // Start then stop
        val startPacket = NetworkPacket(
            id = 10L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 1920, "height" to 1080, "codec" to "h264", "fps" to 30),
        )
        plugin.onPacketReceived(TransferPacket(startPacket))

        val stopPacket = NetworkPacket(
            id = 11L,
            type = "cconnect.screenshare.stop",
            body = mapOf<String, Any>(),
        )
        plugin.onPacketReceived(TransferPacket(stopPacket))

        assertTrue(stopCalled)
    }

    @Test
    fun testOnDestroyStopsSession() {
        val startPacket = NetworkPacket(
            id = 12L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 1920, "height" to 1080, "codec" to "h264", "fps" to 30),
        )
        plugin.onPacketReceived(TransferPacket(startPacket))
        assertNotNull(plugin.activeSession)

        plugin.onDestroy()

        assertNull(plugin.activeSession)
    }

    @Test
    fun testGetUiButtonsEmptyWhenNoSession() {
        assertTrue(plugin.getUiButtons().isEmpty())
    }

    @Test
    fun testGetUiButtonsHasEntryWhenSessionActive() {
        val startPacket = NetworkPacket(
            id = 13L,
            type = "cconnect.screenshare.start",
            body = mapOf("width" to 1920, "height" to 1080, "codec" to "h264", "fps" to 30),
        )
        plugin.onPacketReceived(TransferPacket(startPacket))

        val buttons = plugin.getUiButtons()
        assertEquals(1, buttons.size)
        assertEquals(context.getString(org.cosmic.cosmicconnect.R.string.screenshare_viewer_title), buttons[0].name)

        plugin.stopStreamSession()
    }
}
