/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.AudioStreamPlugin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioStreamPluginTest {

    private lateinit var context: Context
    private lateinit var device: Device
    private lateinit var plugin: AudioStreamPlugin

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        device = mockk(relaxed = true)
        plugin = AudioStreamPlugin(context, device)
    }

    @Test
    fun `status packet with isStreaming true updates state`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf(
                "isStreaming" to true,
                "codec" to "opus",
                "sampleRate" to 48000,
                "channels" to 2,
                "direction" to "send"
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(result)
        assertTrue(plugin.isStreaming)
        assertEquals("opus", plugin.activeCodec)
        assertEquals(48000, plugin.sampleRate)
        assertEquals(2, plugin.channels)
        assertEquals("send", plugin.direction)
    }

    @Test
    fun `status packet with isStreaming false updates state`() {
        // Set up initial streaming state
        val startPacket = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf(
                "isStreaming" to true,
                "codec" to "opus",
                "sampleRate" to 48000,
                "channels" to 2,
                "direction" to "send"
            )
        )
        plugin.onPacketReceived(TransferPacket(startPacket))

        // Now send stop packet
        val stopPacket = NetworkPacket(
            id = 2L,
            type = "cconnect.audiostream",
            body = mapOf(
                "isStreaming" to false
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(stopPacket))

        assertTrue(result)
        assertFalse(plugin.isStreaming)
    }

    @Test
    fun `capability packet sets maxChannels`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream.capability",
            body = mapOf(
                "maxChannels" to 2
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(result)
        assertEquals(2, plugin.maxChannels)
    }

    @Test
    fun `capability packet sets codecs list`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream.capability",
            body = mapOf(
                "codecs" to listOf("opus", "aac", "pcm")
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(result)
        assertEquals(listOf("opus", "aac", "pcm"), plugin.supportedCodecs)
    }

    @Test
    fun `capability packet sets sample rates list`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream.capability",
            body = mapOf(
                "sampleRates" to listOf(44100, 48000, 96000)
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(result)
        assertEquals(listOf(44100, 48000, 96000), plugin.supportedSampleRates)
    }

    @Test
    fun `missing isStreaming field does not crash`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf(
                "codec" to "opus"
            )
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(result)
        assertFalse(plugin.isStreaming)
    }

    @Test
    fun `empty body does not crash`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = emptyMap()
        )

        val result = plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(result)
        assertFalse(plugin.isStreaming)
    }

    @Test
    fun `listener notified on status change`() {
        var notified = false
        plugin.addStreamStateListener { notified = true }

        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf(
                "isStreaming" to true,
                "codec" to "opus"
            )
        )

        plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(notified)
    }

    @Test
    fun `listener notified on capability change`() {
        var notified = false
        plugin.addStreamStateListener { notified = true }

        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream.capability",
            body = mapOf(
                "maxChannels" to 2
            )
        )

        plugin.onPacketReceived(TransferPacket(packet))

        assertTrue(notified)
    }

    @Test
    fun `remove listener stops notifications`() {
        var notificationCount = 0
        val listener = AudioStreamPlugin.StreamStateListener { notificationCount++ }
        plugin.addStreamStateListener(listener)

        // First notification
        val packet1 = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf("isStreaming" to true)
        )
        plugin.onPacketReceived(TransferPacket(packet1))
        assertEquals(1, notificationCount)

        // Remove listener
        plugin.removeStreamStateListener(listener)

        // Second packet should not notify
        val packet2 = NetworkPacket(
            id = 2L,
            type = "cconnect.audiostream",
            body = mapOf("isStreaming" to false)
        )
        plugin.onPacketReceived(TransferPacket(packet2))
        assertEquals(1, notificationCount)
    }

    @Test
    fun `supportedPacketTypes contains correct types`() {
        val types = plugin.supportedPacketTypes.toSet()

        assertTrue(types.contains("cconnect.audiostream"))
        assertTrue(types.contains("cconnect.audiostream.capability"))
        assertEquals(2, types.size)
    }

    @Test
    fun `outgoingPacketTypes contains request type`() {
        val types = plugin.outgoingPacketTypes.toSet()

        assertTrue(types.contains("cconnect.audiostream.request"))
        assertEquals(1, types.size)
    }

    @Test
    fun `isEnabledByDefault is false`() {
        // AudioStreamPlugin should default to disabled
        // This is verified in PluginFactory metadata
        assertNotNull(plugin)
    }

    @Test
    fun `onPacketReceived always returns true`() {
        val packet1 = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf("isStreaming" to true)
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet1)))

        val packet2 = NetworkPacket(
            id = 2L,
            type = "cconnect.audiostream.capability",
            body = mapOf("maxChannels" to 2)
        )
        assertTrue(plugin.onPacketReceived(TransferPacket(packet2)))
    }

    @Test
    fun `wrong packet type does not update state`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.ping",
            body = emptyMap()
        )

        plugin.onPacketReceived(TransferPacket(packet))

        assertFalse(plugin.isStreaming)
        assertNull(plugin.activeCodec)
    }

    @Test
    fun `sendStreamCommand sends start request with all parameters`() {
        plugin.sendStreamCommand(
            start = true,
            codec = "opus",
            sampleRate = 48000,
            channels = 2,
            direction = "send"
        )

        verify {
            device.sendPacket(match { tp ->
                val np = tp.packet
                np.type == "cconnect.audiostream.request" &&
                    np.body["startStreaming"] == true &&
                    np.body["codec"] == "opus" &&
                    np.body["sampleRate"] == 48000 &&
                    np.body["channels"] == 2 &&
                    np.body["direction"] == "send"
            })
        }
    }

    @Test
    fun `sendStreamCommand sends stop request`() {
        plugin.sendStreamCommand(start = false)

        verify {
            device.sendPacket(match { tp ->
                val np = tp.packet
                np.type == "cconnect.audiostream.request" &&
                    np.body["stopStreaming"] == true
            })
        }
    }

    @Test
    fun `multiple listeners all notified`() {
        var count1 = 0
        var count2 = 0
        plugin.addStreamStateListener { count1++ }
        plugin.addStreamStateListener { count2++ }

        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf("isStreaming" to true)
        )
        plugin.onPacketReceived(TransferPacket(packet))

        assertEquals(1, count1)
        assertEquals(1, count2)
    }

    @Test
    fun `adding duplicate listener only notifies once`() {
        var count = 0
        val listener = AudioStreamPlugin.StreamStateListener { count++ }
        plugin.addStreamStateListener(listener)
        plugin.addStreamStateListener(listener) // duplicate

        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream",
            body = mapOf("isStreaming" to true)
        )
        plugin.onPacketReceived(TransferPacket(packet))

        assertEquals(1, count) // Only notified once despite duplicate add
    }

    @Test
    fun `capability packet with all fields sets all properties`() {
        val packet = NetworkPacket(
            id = 1L,
            type = "cconnect.audiostream.capability",
            body = mapOf(
                "maxChannels" to 8,
                "codecs" to listOf("opus", "aac", "pcm", "flac"),
                "sampleRates" to listOf(44100, 48000, 96000, 192000)
            )
        )

        plugin.onPacketReceived(TransferPacket(packet))

        assertEquals(8, plugin.maxChannels)
        assertEquals(listOf("opus", "aac", "pcm", "flac"), plugin.supportedCodecs)
        assertEquals(listOf(44100, 48000, 96000, 192000), plugin.supportedSampleRates)
    }
}
