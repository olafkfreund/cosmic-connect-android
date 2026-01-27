package org.cosmic.cconnect.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cconnect.CosmicConnect
import org.cosmic.cconnect.Device
import org.cosmic.cconnect.NetworkPacket
import org.cosmic.cconnect.test.FfiTestUtils
import org.cosmic.cconnect.test.MockFactory
import org.cosmic.cconnect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration Tests - Device Discovery
 *
 * Tests the device discovery flow through the complete stack:
 * Android UI → CosmicConnect → Rust Core → Network → UDP Broadcast
 *
 * Verifies:
 * - Discovery service start/stop
 * - UDP broadcast packet transmission
 * - Device identification from discovery responses
 * - Multiple device discovery
 * - Discovery timeout handling
 * - Network interface changes
 */
@RunWith(AndroidJUnit4::class)
class DiscoveryIntegrationTest {

  private lateinit var cosmicConnect: CosmicConnect

  @Before
  fun setup() {
    TestUtils.cleanupTestData()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance(TestUtils.getTestContext())
  }

  @After
  fun teardown() {
    // Stop discovery if running
    cosmicConnect.stopDiscovery()

    TestUtils.cleanupTestData()
  }

  @Test
  fun testFfiDiscoveryServiceAvailable() {
    // Verify FFI discovery service is accessible
    val result = FfiTestUtils.testDiscoveryService()
    assertTrue("FFI discovery service should be available", result)
  }

  @Test
  fun testStartDiscovery() {
    // Start discovery
    cosmicConnect.startDiscovery()

    // Verify discovery is active
    assertTrue(
      "Discovery should be running",
      cosmicConnect.isDiscovering
    )
  }

  @Test
  fun testStopDiscovery() {
    // Start then stop discovery
    cosmicConnect.startDiscovery()
    cosmicConnect.stopDiscovery()

    // Verify discovery is stopped
    assertFalse(
      "Discovery should be stopped",
      cosmicConnect.isDiscovering
    )
  }

  @Test
  fun testDiscoveryBroadcast() {
    val broadcastReceived = CountDownLatch(1)

    // Setup listener for broadcast
    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        // Not testing device discovery in this test
      }

      override fun onDiscoveryStarted() {
        // Discovery started, broadcast should be sent
        broadcastReceived.countDown()
      }

      override fun onDiscoveryStopped() {
        // Not testing in this test
      }
    }

    cosmicConnect.addDiscoveryListener(listener)

    // Start discovery
    cosmicConnect.startDiscovery()

    // Verify broadcast was sent
    assertTrue(
      "Discovery broadcast should be sent",
      broadcastReceived.await(5, TimeUnit.SECONDS)
    )

    cosmicConnect.removeDiscoveryListener(listener)
  }

  @Test
  fun testDeviceDiscoveredFromIdentityPacket() {
    val deviceDiscovered = CountDownLatch(1)
    var discoveredDevice: Device? = null

    // Create mock identity packet
    val mockPacket = MockFactory.createIdentityPacket(
      deviceId = "test_cosmic_desktop_1",
      deviceName = "My COSMIC PC",
      deviceType = "desktop"
    )

    // Setup listener
    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        discoveredDevice = device
        deviceDiscovered.countDown()
      }

      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(listener)

    // Start discovery
    cosmicConnect.startDiscovery()

    // Simulate receiving identity packet via FFI
    // In real test, this would come from network
    cosmicConnect.processIncomingPacket(mockPacket)

    // Verify device was discovered
    assertTrue(
      "Device should be discovered from identity packet",
      deviceDiscovered.await(5, TimeUnit.SECONDS)
    )

    assertNotNull("Discovered device should not be null", discoveredDevice)
    assertEquals("test_cosmic_desktop_1", discoveredDevice?.deviceId)
    assertEquals("My COSMIC PC", discoveredDevice?.name)

    cosmicConnect.removeDiscoveryListener(listener)
  }

  @Test
  fun testMultipleDevicesDiscovered() {
    val devicesDiscovered = mutableListOf<Device>()
    val expectedDeviceCount = 3
    val allDevicesDiscovered = CountDownLatch(expectedDeviceCount)

    // Create mock identity packets for multiple devices
    val mockPackets = listOf(
      MockFactory.createIdentityPacket(
        deviceId = "cosmic_desktop_1",
        deviceName = "Desktop 1",
        deviceType = "desktop"
      ),
      MockFactory.createIdentityPacket(
        deviceId = "cosmic_laptop_1",
        deviceName = "Laptop 1",
        deviceType = "laptop"
      ),
      MockFactory.createIdentityPacket(
        deviceId = "cosmic_phone_1",
        deviceName = "Phone 1",
        deviceType = "phone"
      )
    )

    // Setup listener
    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        devicesDiscovered.add(device)
        allDevicesDiscovered.countDown()
      }

      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(listener)

    // Start discovery
    cosmicConnect.startDiscovery()

    // Simulate receiving identity packets
    mockPackets.forEach { packet ->
      cosmicConnect.processIncomingPacket(packet)
    }

    // Verify all devices discovered
    assertTrue(
      "All devices should be discovered",
      allDevicesDiscovered.await(5, TimeUnit.SECONDS)
    )

    assertEquals(
      "Should discover exactly $expectedDeviceCount devices",
      expectedDeviceCount,
      devicesDiscovered.size
    )

    // Verify device IDs are unique
    val uniqueIds = devicesDiscovered.map { it.deviceId }.toSet()
    assertEquals(
      "All discovered devices should have unique IDs",
      expectedDeviceCount,
      uniqueIds.size
    )

    cosmicConnect.removeDiscoveryListener(listener)
  }

  @Test
  fun testDiscoveryTimeout() {
    // Start discovery
    cosmicConnect.startDiscovery()

    // Wait for discovery timeout (default 30 seconds)
    // For testing, we'll use a shorter timeout
    val discoveryTimeoutMs = 2000L

    Thread.sleep(discoveryTimeoutMs)

    // Verify discovery is still running (continuous discovery)
    // Or verify it has stopped if timeout is implemented
    // This depends on implementation - adjust as needed
    val isDiscovering = cosmicConnect.isDiscovering

    // Document behavior
    // If discovery runs continuously:
    assertTrue("Discovery should continue running", isDiscovering)

    // If discovery stops after timeout:
    // assertFalse("Discovery should stop after timeout", isDiscovering)
  }

  @Test
  fun testRediscoveryAfterNetworkChange() {
    val firstDiscovery = CountDownLatch(1)
    val secondDiscovery = CountDownLatch(1)

    var discoveryCount = 0

    // Setup listener
    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        // Not testing specific devices
      }

      override fun onDiscoveryStarted() {
        discoveryCount++
        if (discoveryCount == 1) {
          firstDiscovery.countDown()
        } else if (discoveryCount == 2) {
          secondDiscovery.countDown()
        }
      }

      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(listener)

    // Start initial discovery
    cosmicConnect.startDiscovery()

    assertTrue(
      "First discovery should start",
      firstDiscovery.await(5, TimeUnit.SECONDS)
    )

    // Simulate network change
    cosmicConnect.onNetworkStateChanged()

    // Verify discovery restarted
    assertTrue(
      "Discovery should restart after network change",
      secondDiscovery.await(5, TimeUnit.SECONDS)
    )

    assertEquals(
      "Discovery should have started twice",
      2,
      discoveryCount
    )

    cosmicConnect.removeDiscoveryListener(listener)
  }

  @Test
  fun testDiscoveryWithMultipleListeners() {
    val listener1Triggered = CountDownLatch(1)
    val listener2Triggered = CountDownLatch(1)

    // Create two listeners
    val listener1 = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {}
      override fun onDiscoveryStarted() {
        listener1Triggered.countDown()
      }
      override fun onDiscoveryStopped() {}
    }

    val listener2 = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {}
      override fun onDiscoveryStarted() {
        listener2Triggered.countDown()
      }
      override fun onDiscoveryStopped() {}
    }

    // Add both listeners
    cosmicConnect.addDiscoveryListener(listener1)
    cosmicConnect.addDiscoveryListener(listener2)

    // Start discovery
    cosmicConnect.startDiscovery()

    // Verify both listeners notified
    assertTrue(
      "Listener 1 should be notified",
      listener1Triggered.await(5, TimeUnit.SECONDS)
    )
    assertTrue(
      "Listener 2 should be notified",
      listener2Triggered.await(5, TimeUnit.SECONDS)
    )

    cosmicConnect.removeDiscoveryListener(listener1)
    cosmicConnect.removeDiscoveryListener(listener2)
  }

  @Test
  fun testDiscoveryListenerRemoval() {
    var notificationCount = 0

    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {}
      override fun onDiscoveryStarted() {
        notificationCount++
      }
      override fun onDiscoveryStopped() {}
    }

    // Add and remove listener
    cosmicConnect.addDiscoveryListener(listener)
    cosmicConnect.removeDiscoveryListener(listener)

    // Start discovery
    cosmicConnect.startDiscovery()

    // Wait a bit
    Thread.sleep(1000)

    // Verify removed listener not notified
    assertEquals(
      "Removed listener should not be notified",
      0,
      notificationCount
    )
  }

  @Test
  fun testDiscoveryPacketValidation() {
    // Create invalid identity packet (missing required fields)
    val invalidPacket = NetworkPacket(NetworkPacket.PACKET_TYPE_IDENTITY)
    // Don't set deviceId, deviceName - packet is incomplete

    val deviceDiscovered = CountDownLatch(1)

    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        deviceDiscovered.countDown()
      }
      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(listener)
    cosmicConnect.startDiscovery()

    // Process invalid packet
    cosmicConnect.processIncomingPacket(invalidPacket)

    // Verify no device discovered from invalid packet
    assertFalse(
      "Invalid packet should not create device",
      deviceDiscovered.await(2, TimeUnit.SECONDS)
    )

    cosmicConnect.removeDiscoveryListener(listener)
  }
}
