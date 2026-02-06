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

@RunWith(RobolectricTestRunner::class)
class PluginManagerTest {

    private lateinit var context: Context
    private lateinit var pluginFactory: PluginFactory
    private lateinit var device: Device
    private lateinit var pluginManager: PluginManager
    private var paired = true
    private var reachable = true
    private val supportedPlugins = mutableListOf<String>()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        pluginFactory = mockk(relaxed = true)
        device = mockk(relaxed = true)
        paired = true
        reachable = true
        supportedPlugins.clear()

        pluginManager = PluginManager(
            context = context,
            deviceId = "test-device-id",
            deviceName = { "Test Device" },
            pluginFactory = pluginFactory,
            device = device,
            isPaired = { paired },
            isReachable = { reachable },
            getSupportedPlugins = { supportedPlugins }
        )
    }

    @Test
    fun `getPlugin returns null when no plugins loaded`() {
        assertNull(pluginManager.getPlugin("NonExistentPlugin"))
    }

    @Test
    fun `getPlugin returns plugin when loaded`() {
        val mockPlugin = mockk<Plugin>(relaxed = true)
        pluginManager.loadedPlugins["TestPlugin"] = mockPlugin
        assertEquals(mockPlugin, pluginManager.getPlugin("TestPlugin"))
    }

    @Test
    fun `getPluginIncludingWithoutPermissions checks both maps`() {
        val mockPlugin = mockk<Plugin>(relaxed = true)
        pluginManager.pluginsWithoutPermissions["TestPlugin"] = mockPlugin
        assertNull(pluginManager.getPlugin("TestPlugin"))
        assertNotNull(pluginManager.getPluginIncludingWithoutPermissions("TestPlugin"))
    }

    @Test
    fun `loadedPlugins is empty initially`() {
        assertTrue(pluginManager.loadedPlugins.isEmpty())
    }

    @Test
    fun `pluginsWithoutPermissions is empty initially`() {
        assertTrue(pluginManager.pluginsWithoutPermissions.isEmpty())
    }

    @Test
    fun `pluginsByIncomingInterfaceEmpty is true initially`() {
        assertTrue(pluginManager.pluginsByIncomingInterfaceEmpty)
    }

    @Test
    fun `reloadPluginsFromSettings removes plugins when not reachable`() {
        val mockPlugin = mockk<Plugin>(relaxed = true)
        pluginManager.loadedPlugins["TestPlugin"] = mockPlugin
        reachable = false
        supportedPlugins.add("TestPlugin")

        val pluginInfo = mockk<PluginFactory.PluginInfo>(relaxed = true)
        every { pluginInfo.listenToUnpaired } returns false
        every { pluginInfo.isEnabledByDefault } returns true
        every { pluginFactory.getPluginInfo("TestPlugin") } returns pluginInfo

        pluginManager.reloadPluginsFromSettings()

        assertFalse(pluginManager.loadedPlugins.containsKey("TestPlugin"))
        verify { mockPlugin.onDestroy() }
    }

    @Test
    fun `reloadPluginsFromSettings removes plugins when not paired and not listening to unpaired`() {
        val mockPlugin = mockk<Plugin>(relaxed = true)
        pluginManager.loadedPlugins["TestPlugin"] = mockPlugin
        paired = false
        supportedPlugins.add("TestPlugin")

        val pluginInfo = mockk<PluginFactory.PluginInfo>(relaxed = true)
        every { pluginInfo.listenToUnpaired } returns false
        every { pluginInfo.isEnabledByDefault } returns true
        every { pluginFactory.getPluginInfo("TestPlugin") } returns pluginInfo

        pluginManager.reloadPluginsFromSettings()

        assertFalse(pluginManager.loadedPlugins.containsKey("TestPlugin"))
    }

    @Test
    fun `pluginsChangedListener is notified on onPluginsChanged`() {
        var notified = false
        val listener = Device.PluginsChangedListener { notified = true }
        pluginManager.addPluginsChangedListener(listener)
        pluginManager.onPluginsChanged()
        assertTrue(notified)
    }

    @Test
    fun `removePluginsChangedListener stops notifications`() {
        var notified = false
        val listener = Device.PluginsChangedListener { notified = true }
        pluginManager.addPluginsChangedListener(listener)
        pluginManager.removePluginsChangedListener(listener)
        pluginManager.onPluginsChanged()
        assertFalse(notified)
    }
}
