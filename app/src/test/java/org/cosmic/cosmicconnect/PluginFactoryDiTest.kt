/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
@RunWith(RobolectricTestRunner::class)
class PluginFactoryDiTest {

    private lateinit var context: Context
    private lateinit var device: Device

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        device = mockk(relaxed = true)
    }

    // ---- Creator map tests (no initPluginInfo needed) ----

    @Test
    fun `instantiatePluginForDevice uses PluginCreator when available`() {
        val mockPlugin = mockk<Plugin>(relaxed = true)
        val mockCreator = mockk<PluginCreator>()
        every { mockCreator.create(device) } returns mockPlugin

        val creators = mapOf("PingPlugin" to mockCreator)
        val factory = PluginFactory(context, creators)
        factory.initPluginInfo()

        val result = factory.instantiatePluginForDevice(context, "PingPlugin", device)
        assertNotNull("Should return plugin from creator", result)
        verify(exactly = 1) { mockCreator.create(device) }
    }

    @Test
    fun `instantiatePluginForDevice returns null for unknown plugin key`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        val result = factory.instantiatePluginForDevice(context, "NonExistentPlugin", device)
        assertNull("Should return null for unknown key", result)
    }

    // ---- initPluginInfo integration tests ----

    @Test
    fun `initPluginInfo loads migrated plugins from static metadata`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        val available = factory.availablePlugins
        // Wave 1
        assertTrue("PingPlugin should be available", available.contains("PingPlugin"))
        assertTrue("FindRemoteDevicePlugin should be available", available.contains("FindRemoteDevicePlugin"))
        assertTrue("ConnectivityReportPlugin should be available", available.contains("ConnectivityReportPlugin"))
        assertTrue("PresenterPlugin should be available", available.contains("PresenterPlugin"))
        assertTrue("MousePadPlugin should be available", available.contains("MousePadPlugin"))
        // Wave 2
        assertTrue("ClipboardPlugin should be available", available.contains("ClipboardPlugin"))
        assertTrue("SystemVolumePlugin should be available", available.contains("SystemVolumePlugin"))
        assertTrue("RemoteKeyboardPlugin should be available", available.contains("RemoteKeyboardPlugin"))
        assertTrue("RunCommandPlugin should be available", available.contains("RunCommandPlugin"))
        assertTrue("OpenOnDesktopPlugin should be available", available.contains("OpenOnDesktopPlugin"))
        // Wave 3
        assertTrue("BatteryPluginFFI should be available", available.contains("BatteryPluginFFI"))
        assertTrue("FindMyPhonePlugin should be available", available.contains("FindMyPhonePlugin"))
        assertTrue("ContactsPlugin should be available", available.contains("ContactsPlugin"))
        assertTrue("TelephonyPlugin should be available", available.contains("TelephonyPlugin"))
        assertTrue("SMSPlugin should be available", available.contains("SMSPlugin"))
        // Wave 4
        assertTrue("SharePlugin should be available", available.contains("SharePlugin"))
        assertTrue("SftpPlugin should be available", available.contains("SftpPlugin"))
        assertTrue("MprisPlugin should be available", available.contains("MprisPlugin"))
        assertTrue("NotificationsPlugin should be available", available.contains("NotificationsPlugin"))
        assertTrue("ReceiveNotificationsPlugin should be available", available.contains("ReceiveNotificationsPlugin"))
        // Wave 5
        assertTrue("OpenOnPhonePlugin should be available", available.contains("OpenOnPhonePlugin"))
        assertTrue("CameraPlugin should be available", available.contains("CameraPlugin"))
        assertTrue("ExtendedDisplayPlugin should be available", available.contains("ExtendedDisplayPlugin"))
    }

    @Test
    fun `all plugins are migrated and no legacy plugins remain`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        // All 23 plugins are now migrated — no legacy reflection path needed
        assertEquals("Should have 23 total plugins", 23, factory.availablePlugins.size)
    }

    @Test
    fun `total plugin count is all migrated`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        // 23 migrated + 0 legacy = 23 total
        assertEquals("Should have 23 total plugins", 23, factory.availablePlugins.size)
    }

    @Test
    fun `migrated plugin metadata has correct display name`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        val info = factory.getPluginInfo("PingPlugin")
        assertEquals("Ping", info.displayName)
        assertEquals("Send and receive pings", info.description)
    }

    @Test
    fun `migrated plugin metadata has correct packet types`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        val pingInfo = factory.getPluginInfo("PingPlugin")
        assertTrue(pingInfo.supportedPacketTypes.contains("cconnect.ping"))
        assertTrue(pingInfo.outgoingPacketTypes.contains("cconnect.ping"))

        val findInfo = factory.getPluginInfo("FindRemoteDevicePlugin")
        assertTrue(findInfo.outgoingPacketTypes.contains("cconnect.findmyphone.request"))
        assertTrue(findInfo.supportedPacketTypes.isEmpty())
    }

    @Test
    fun `MousePadPlugin metadata includes hasSettings flag`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        val info = factory.getPluginInfo("MousePadPlugin")
        assertTrue("MousePadPlugin should have hasSettings=true", info.hasSettings)
    }
}
