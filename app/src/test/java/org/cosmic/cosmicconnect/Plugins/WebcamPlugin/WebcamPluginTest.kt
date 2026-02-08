/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.WebcamPlugin

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
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
class WebcamPluginTest {

    private lateinit var plugin: WebcamPlugin
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        every { mockDevice.deviceId } returns "test-device-id"
        every { mockDevice.name } returns "Test Device"
        plugin = WebcamPlugin(context, mockDevice)
    }

    // ========================================================================
    // Plugin metadata
    // ========================================================================

    @Test
    fun `supportedPacketTypes contains webcam request and capability`() {
        assertArrayEquals(
            arrayOf(PACKET_TYPE_WEBCAM_REQUEST, PACKET_TYPE_WEBCAM_CAPABILITY),
            plugin.supportedPacketTypes,
        )
    }

    @Test
    fun `outgoingPacketTypes contains webcam and capability`() {
        assertArrayEquals(
            arrayOf(PACKET_TYPE_WEBCAM, PACKET_TYPE_WEBCAM_CAPABILITY),
            plugin.outgoingPacketTypes,
        )
    }

    @Test
    fun `isEnabledByDefault is false`() {
        assertFalse(plugin.isEnabledByDefault)
    }

    @Test
    fun `checkRequiredPermissions returns false without CAMERA permission`() {
        assertFalse(plugin.checkRequiredPermissions())
    }

    @Test
    fun `displayName is not empty`() {
        assertTrue(plugin.displayName.isNotEmpty())
    }

    @Test
    fun `description is not empty`() {
        assertTrue(plugin.description.isNotEmpty())
    }

    // ========================================================================
    // Initial state
    // ========================================================================

    @Test
    fun `initial isStreaming is false`() {
        assertFalse(plugin.isStreaming)
    }

    @Test
    fun `initial activeCameraId is null`() {
        assertNull(plugin.activeCameraId)
    }

    @Test
    fun `initial activeWidth is null`() {
        assertNull(plugin.activeWidth)
    }

    @Test
    fun `initial activeHeight is null`() {
        assertNull(plugin.activeHeight)
    }

    // ========================================================================
    // Receive start request
    // ========================================================================

    @Test
    fun `start request updates isStreaming to true`() {
        val tp = makeStartRequest(cameraId = "0", width = 1920, height = 1080)
        plugin.onPacketReceived(tp)
        assertTrue(plugin.isStreaming)
    }

    @Test
    fun `start request updates activeCameraId`() {
        val tp = makeStartRequest(cameraId = "1")
        plugin.onPacketReceived(tp)
        assertEquals("1", plugin.activeCameraId)
    }

    @Test
    fun `start request updates active resolution`() {
        val tp = makeStartRequest(width = 640, height = 480)
        plugin.onPacketReceived(tp)
        assertEquals(640, plugin.activeWidth)
        assertEquals(480, plugin.activeHeight)
    }

    @Test
    fun `start request sends status packet`() {
        val tp = makeStartRequest()
        plugin.onPacketReceived(tp)
        verify {
            mockDevice.sendPacket(match { sent ->
                sent.packet.type == PACKET_TYPE_WEBCAM &&
                sent.packet.body["isStreaming"] == true
            })
        }
    }

    @Test
    fun `start request returns true`() {
        val tp = makeStartRequest()
        assertTrue(plugin.onPacketReceived(tp))
    }

    @Test
    fun `start request with missing fields uses defaults`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("start" to true),
        ))
        plugin.onPacketReceived(tp)
        assertTrue(plugin.isStreaming)
        assertEquals(1280, plugin.activeWidth)
        assertEquals(720, plugin.activeHeight)
    }

    // ========================================================================
    // Receive stop request
    // ========================================================================

    @Test
    fun `stop request updates isStreaming to false`() {
        plugin.onPacketReceived(makeStartRequest())
        assertTrue(plugin.isStreaming)

        plugin.onPacketReceived(makeStopRequest())
        assertFalse(plugin.isStreaming)
    }

    @Test
    fun `stop request clears activeCameraId`() {
        plugin.onPacketReceived(makeStartRequest(cameraId = "0"))
        plugin.onPacketReceived(makeStopRequest())
        assertNull(plugin.activeCameraId)
    }

    @Test
    fun `stop request clears active resolution`() {
        plugin.onPacketReceived(makeStartRequest(width = 1920, height = 1080))
        plugin.onPacketReceived(makeStopRequest())
        assertNull(plugin.activeWidth)
        assertNull(plugin.activeHeight)
    }

    @Test
    fun `stop request sends status packet with isStreaming false`() {
        plugin.onPacketReceived(makeStartRequest())
        plugin.onPacketReceived(makeStopRequest())
        verify {
            mockDevice.sendPacket(match { sent ->
                sent.packet.type == PACKET_TYPE_WEBCAM &&
                sent.packet.body["isStreaming"] == false
            })
        }
    }

    @Test
    fun `stop request returns true`() {
        assertTrue(plugin.onPacketReceived(makeStopRequest()))
    }

    // ========================================================================
    // Wrong packet type
    // ========================================================================

    @Test
    fun `wrong packet type returns false`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = emptyMap(),
        ))
        assertFalse(plugin.onPacketReceived(tp))
    }

    @Test
    fun `webcam request without start or stop returns false`() {
        val tp = TransferPacket(NetworkPacket(
            id = 1L,
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("unknown" to true),
        ))
        assertFalse(plugin.onPacketReceived(tp))
    }

    // ========================================================================
    // Listener
    // ========================================================================

    @Test
    fun `listener notified on start`() {
        var notifiedStreaming = false
        var notifiedCameraId: String? = null
        plugin.setWebcamStateListener(object : WebcamPlugin.WebcamStateListener {
            override fun onWebcamStateChanged(isStreaming: Boolean, cameraId: String?, width: Int?, height: Int?) {
                notifiedStreaming = isStreaming
                notifiedCameraId = cameraId
            }
        })
        plugin.onPacketReceived(makeStartRequest(cameraId = "0"))
        assertTrue(notifiedStreaming)
        assertEquals("0", notifiedCameraId)
    }

    @Test
    fun `listener notified on stop`() {
        var notifiedStreaming = true
        plugin.setWebcamStateListener(object : WebcamPlugin.WebcamStateListener {
            override fun onWebcamStateChanged(isStreaming: Boolean, cameraId: String?, width: Int?, height: Int?) {
                notifiedStreaming = isStreaming
            }
        })
        plugin.onPacketReceived(makeStartRequest())
        plugin.onPacketReceived(makeStopRequest())
        assertFalse(notifiedStreaming)
    }

    @Test
    fun `removed listener not notified`() {
        var callCount = 0
        plugin.setWebcamStateListener(object : WebcamPlugin.WebcamStateListener {
            override fun onWebcamStateChanged(isStreaming: Boolean, cameraId: String?, width: Int?, height: Int?) {
                callCount++
            }
        })
        plugin.onPacketReceived(makeStartRequest())
        assertEquals(1, callCount)

        plugin.setWebcamStateListener(null)
        plugin.onPacketReceived(makeStopRequest())
        assertEquals(1, callCount)
    }

    // ========================================================================
    // onDestroy
    // ========================================================================

    @Test
    fun `onDestroy stops streaming if active`() {
        plugin.onPacketReceived(makeStartRequest())
        assertTrue(plugin.isStreaming)

        plugin.onDestroy()
        assertFalse(plugin.isStreaming)
    }

    @Test
    fun `onDestroy sends stop status when streaming`() {
        plugin.onPacketReceived(makeStartRequest())
        plugin.onDestroy()
        verify(atLeast = 1) {
            mockDevice.sendPacket(match { sent ->
                sent.packet.type == PACKET_TYPE_WEBCAM &&
                sent.packet.body["isStreaming"] == false
            })
        }
    }

    @Test
    fun `onDestroy does nothing when not streaming`() {
        plugin.onDestroy()
        verify(exactly = 0) {
            mockDevice.sendPacket(any())
        }
    }

    // ========================================================================
    // Multiple start/stop cycles
    // ========================================================================

    @Test
    fun `multiple start stop cycles work correctly`() {
        plugin.onPacketReceived(makeStartRequest(cameraId = "0"))
        assertTrue(plugin.isStreaming)
        assertEquals("0", plugin.activeCameraId)

        plugin.onPacketReceived(makeStopRequest())
        assertFalse(plugin.isStreaming)
        assertNull(plugin.activeCameraId)

        plugin.onPacketReceived(makeStartRequest(cameraId = "1", width = 640, height = 480))
        assertTrue(plugin.isStreaming)
        assertEquals("1", plugin.activeCameraId)
        assertEquals(640, plugin.activeWidth)
        assertEquals(480, plugin.activeHeight)
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun makeStartRequest(
        cameraId: String = "0",
        width: Int = 1280,
        height: Int = 720,
        fps: Int = 30,
    ): TransferPacket {
        return TransferPacket(NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf(
                "start" to true,
                "cameraId" to cameraId,
                "width" to width,
                "height" to height,
                "fps" to fps,
            ),
        ))
    }

    private fun makeStopRequest(): TransferPacket {
        return TransferPacket(NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_WEBCAM_REQUEST,
            body = mapOf("stop" to true),
        ))
    }
}
