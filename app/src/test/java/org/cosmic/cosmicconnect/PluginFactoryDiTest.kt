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
    fun `pluginCreators map is used before reflection fallback`() {
        val mockPlugin = mockk<Plugin>(relaxed = true)
        val mockCreator = mockk<PluginCreator>()
        every { mockCreator.create(device) } returns mockPlugin

        // Also add a legacy plugin key to creators to prove it takes priority
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
        assertTrue("PingPlugin should be available", available.contains("PingPlugin"))
        assertTrue("FindRemoteDevicePlugin should be available", available.contains("FindRemoteDevicePlugin"))
        assertTrue("ConnectivityReportPlugin should be available", available.contains("ConnectivityReportPlugin"))
        assertTrue("PresenterPlugin should be available", available.contains("PresenterPlugin"))
        assertTrue("MousePadPlugin should be available", available.contains("MousePadPlugin"))
    }

    @Test
    fun `initPluginInfo loads legacy plugins via reflection`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        val available = factory.availablePlugins
        assertTrue("BatteryPluginFFI should be available", available.contains("BatteryPluginFFI"))
        assertTrue("FindMyPhonePlugin should be available", available.contains("FindMyPhonePlugin"))
    }

    @Test
    fun `total plugin count includes both migrated and legacy`() {
        val factory = PluginFactory(context, emptyMap())
        factory.initPluginInfo()

        // 5 migrated + 18 legacy = 23 total
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
