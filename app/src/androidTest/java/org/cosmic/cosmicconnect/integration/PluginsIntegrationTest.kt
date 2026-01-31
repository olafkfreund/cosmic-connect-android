package org.cosmic.cosmicconnect.integration

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.BatteryPlugin.BatteryPlugin
import org.cosmic.cosmicconnect.test.MockFactory
import org.cosmic.cosmicconnect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PluginsIntegrationTest {

  private lateinit var cosmicConnect: CosmicConnect
  private lateinit var pairedDevice: Device
  private lateinit var mockLink: MockFactory.MockLink
  private lateinit var context: Context

  @Before
  fun setup() {
    TestUtils.cleanupTestData()
    context = TestUtils.getTestContext()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance()
    cosmicConnect.pluginFactory.initPluginInfo()

    // Create and pair a test device using MockLink
    val deviceId = "test_device_plugins"
    mockLink = MockFactory.createMockLink(context, deviceId, "Test Desktop") as MockFactory.MockLink
    
    // Inject link to DeviceRegistry
    cosmicConnect.deviceRegistry.connectionListener.onConnectionReceived(mockLink)

    pairedDevice = cosmicConnect.getDevice(deviceId)!!
    
    // Set capabilities explicitly to match PluginFactory logic
    pairedDevice.deviceInfo.incomingCapabilities = setOf("cconnect.battery", "cconnect.clipboard", "cconnect.share", "cconnect.ping", "cconnect.runcommand", "cconnect.mpris", "cconnect.telephony")
    pairedDevice.deviceInfo.outgoingCapabilities = setOf("cconnect.battery", "cconnect.clipboard", "cconnect.share", "cconnect.ping", "cconnect.runcommand", "cconnect.mpris", "cconnect.telephony")

    // Explicitly enable BatteryPlugin in settings
    val prefs = org.cosmic.cosmicconnect.Helpers.TrustedDevices.getDeviceSettings(context, deviceId)
    prefs.edit().putBoolean("BatteryPlugin", true).putBoolean("battery", true).apply()
    
    // Simulate pairing
    pairedDevice.requestPairing()
    // Wait for state to update to Requested
    assertTrue("Should be in Requested state", TestUtils.waitFor { 
        pairedDevice.pairStatus == org.cosmic.cosmicconnect.PairingHandler.PairState.Requested 
    })

    val pairResponse = MockFactory.createPairResponsePacket(
      deviceId = pairedDevice.deviceId,
      accepted = true
    )
    pairedDevice.onPacketReceived(pairResponse)
    assertTrue("Should be paired", TestUtils.waitFor { pairedDevice.isPaired })

    // Ensure capabilities are set (Device update might have cleared them)
    pairedDevice.deviceInfo.incomingCapabilities = setOf("cconnect.battery", "cconnect.clipboard", "cconnect.share", "cconnect.ping", "cconnect.runcommand", "cconnect.mpris", "cconnect.telephony")
    pairedDevice.deviceInfo.outgoingCapabilities = setOf("cconnect.battery", "cconnect.clipboard", "cconnect.share", "cconnect.ping", "cconnect.runcommand", "cconnect.mpris", "cconnect.telephony")

    android.util.Log.d("PluginsTest", "Device ID: ${pairedDevice.deviceId}")
    android.util.Log.d("PluginsTest", "isPaired: ${pairedDevice.isPaired}")
    android.util.Log.d("PluginsTest", "isReachable: ${pairedDevice.isReachable}")
    android.util.Log.d("PluginsTest", "isPluginEnabled(BatteryPlugin): ${pairedDevice.isPluginEnabled("BatteryPlugin")}")

    // Force reload plugins now that we are paired and capable
    pairedDevice.reloadPluginsFromSettings()
    
    android.util.Log.d("PluginsTest", "Supported: ${pairedDevice.supportedPlugins}")
    pairedDevice.loadedPlugins.forEach { (key, plugin) ->
        android.util.Log.d("PluginsTest", "Loaded plugin: $key -> ${plugin.javaClass.simpleName}")
    }
  }

  @After
  fun teardown() {
    if (::pairedDevice.isInitialized && pairedDevice.isPaired) {
      pairedDevice.unpair()
    }
    if (::mockLink.isInitialized) {
        cosmicConnect.deviceRegistry.connectionListener.onConnectionLost(mockLink)
    }
    TestUtils.cleanupTestData()
  }

  @Test
  fun testBatteryPluginAvailable() {
    val batteryPlugin = pairedDevice.getPluginIncludingWithoutPermissions(BatteryPlugin::class.java.simpleName)
    if (batteryPlugin == null) {
        android.util.Log.e("PluginsTest", "BatteryPlugin is null. Supported plugins: ${pairedDevice.supportedPlugins}")
    }
    assertNotNull("Battery plugin should be available", batteryPlugin)
    assertTrue("Should be BatteryPlugin instance", batteryPlugin is BatteryPlugin)
  }

  @Test
  fun testSendBatteryStatus() {
    val batteryPlugin = pairedDevice.getPluginIncludingWithoutPermissions(BatteryPlugin::class.java.simpleName) as BatteryPlugin?
    
    // Clear previous packets
    mockLink.sentPackets.clear()

    // FIRST UPDATE: Force a known state (50%) to ensure change detection works
    val intent1 = android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED).apply {
        putExtra(android.os.BatteryManager.EXTRA_LEVEL, 50)
        putExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
        putExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) // Unplugged
    }
    batteryPlugin!!.receiver.onReceive(context, intent1)

    // SECOND UPDATE: Change to 75%
    val intent2 = android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED).apply {
        putExtra(android.os.BatteryManager.EXTRA_LEVEL, 75)
        putExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
        putExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) // Unplugged
    }
    batteryPlugin.receiver.onReceive(context, intent2)

    // Verify packet sent
    TestUtils.waitFor { mockLink.sentPackets.isNotEmpty() }
    assertFalse("Should have sent packets", mockLink.sentPackets.isEmpty())
    
    val packet = mockLink.sentPackets.find { it.type == "cconnect.battery" && it.getInt("currentCharge") == 75 }
    assertNotNull("Should send battery packet with 75%", packet)
    assertEquals(75, packet!!.getInt("currentCharge"))
    assertEquals(false, packet.getBoolean("isCharging"))
  }

  @Test
  fun testReceiveBatteryStatus() {
    val batteryPlugin = pairedDevice.getPluginIncludingWithoutPermissions(BatteryPlugin::class.java.simpleName) as BatteryPlugin
    
    // Simulate receiving battery packet from COSMIC
    val batteryPacket = MockFactory.createBatteryPacket(
      deviceId = pairedDevice.deviceId,
      batteryLevel = 85,
      isCharging = true
    )
    pairedDevice.onPacketReceived(batteryPacket)

    // Verify battery update received
    val info = batteryPlugin.remoteBatteryInfo
    assertNotNull("Remote battery info should be set", info)
    assertEquals("Battery level should match", 85, info?.currentCharge)
    assertTrue("Should be charging", info?.isCharging == true)
  }
}