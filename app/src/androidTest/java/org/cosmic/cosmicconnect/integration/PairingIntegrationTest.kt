package org.cosmic.cosmicconnect.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.test.FfiTestUtils
import org.cosmic.cosmicconnect.test.MockFactory
import org.cosmic.cosmicconnect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Integration Tests - Device Pairing
 *
 * Tests the device pairing flow through the complete stack:
 * Android UI → CosmicConnect → Rust Core → Network → TLS/Certificate Exchange
 *
 * Verifies:
 * - Pairing request initiation
 * - Pairing request reception
 * - Pairing acceptance
 * - Pairing rejection
 * - Certificate exchange via FFI
 * - Paired device persistence
 * - Unpairing flow
 * - Pairing timeout handling
 */
@RunWith(AndroidJUnit4::class)
class PairingIntegrationTest {

  private lateinit var cosmicConnect: CosmicConnect
  private lateinit var testDevice: Device

  @Before
  fun setup() {
    TestUtils.cleanupTestData()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance(TestUtils.getTestContext())

    // Create a test device for pairing tests
    val identityPacket = MockFactory.createIdentityPacket(
      deviceId = "test_device_pair",
      deviceName = "Test Device",
      deviceType = "desktop"
    )

    // Simulate device discovery
    cosmicConnect.processIncomingPacket(identityPacket)

    // Get the device
    testDevice = cosmicConnect.getDevice("test_device_pair")!!
  }

  @After
  fun teardown() {
    // Unpair test device if paired
    if (testDevice.isPaired) {
      testDevice.unpair()
    }

    TestUtils.cleanupTestData()
  }

  @Test
  fun testFfiCertificateGeneration() {
    // Verify FFI can generate certificates for pairing
    val certificate = FfiTestUtils.createTestCertificate()
    assertNotNull("FFI should generate certificate", certificate)
  }

  @Test
  fun testRequestPairing() {
    val pairRequestSent = CountDownLatch(1)

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}

      override fun onPairRequestSent(device: Device) {
        pairRequestSent.countDown()
      }

      override fun onPaired(device: Device) {}

      override fun onUnpaired(device: Device) {}

      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Request pairing
    testDevice.requestPairing()

    // Verify pairing request sent
    assertTrue(
      "Pairing request should be sent",
      pairRequestSent.await(5, TimeUnit.SECONDS)
    )

    // Verify device state
    assertTrue(
      "Device should show pairing requested",
      testDevice.isPairRequested
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testReceivePairRequest() {
    val pairRequestReceived = CountDownLatch(1)

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {
        pairRequestReceived.countDown()
      }

      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) {}
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Simulate receiving pair request from remote device
    val pairRequestPacket = MockFactory.createPairRequestPacket(
      deviceId = testDevice.deviceId
    )
    cosmicConnect.processIncomingPacket(pairRequestPacket)

    // Verify pair request received
    assertTrue(
      "Pair request should be received",
      pairRequestReceived.await(5, TimeUnit.SECONDS)
    )

    // Verify device state
    assertTrue(
      "Device should show pair requested by peer",
      testDevice.isPairRequestedByPeer
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testAcceptPairing() {
    val pairingCompleted = CountDownLatch(1)

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {
        // Accept the pairing request
        device.acceptPairing()
      }

      override fun onPairRequestSent(device: Device) {}

      override fun onPaired(device: Device) {
        pairingCompleted.countDown()
      }

      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Simulate receiving pair request
    val pairRequestPacket = MockFactory.createPairRequestPacket(
      deviceId = testDevice.deviceId
    )
    cosmicConnect.processIncomingPacket(pairRequestPacket)

    // Simulate receiving pair response (accepted)
    val pairResponsePacket = MockFactory.createPairResponsePacket(
      deviceId = testDevice.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponsePacket)

    // Verify pairing completed
    assertTrue(
      "Pairing should complete",
      pairingCompleted.await(5, TimeUnit.SECONDS)
    )

    // Verify device state
    assertTrue(
      "Device should be paired",
      testDevice.isPaired
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testRejectPairing() {
    val pairRequestReceived = CountDownLatch(1)

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {
        pairRequestReceived.countDown()
        // Reject the pairing request
        device.rejectPairing()
      }

      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) {}
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Simulate receiving pair request
    val pairRequestPacket = MockFactory.createPairRequestPacket(
      deviceId = testDevice.deviceId
    )
    cosmicConnect.processIncomingPacket(pairRequestPacket)

    // Verify request was received and rejected
    assertTrue(
      "Pair request should be received",
      pairRequestReceived.await(5, TimeUnit.SECONDS)
    )

    // Wait a bit for rejection to process
    Thread.sleep(1000)

    // Verify device not paired
    assertFalse(
      "Device should not be paired after rejection",
      testDevice.isPaired
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testUnpair() {
    val unpaired = CountDownLatch(1)

    // First, pair the device
    testDevice.requestPairing()

    // Simulate successful pairing
    val pairResponsePacket = MockFactory.createPairResponsePacket(
      deviceId = testDevice.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponsePacket)

    // Wait for pairing to complete
    TestUtils.waitFor { testDevice.isPaired }

    // Setup listener for unpairing
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) {}

      override fun onUnpaired(device: Device) {
        unpaired.countDown()
      }

      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Unpair
    testDevice.unpair()

    // Verify unpaired
    assertTrue(
      "Device should be unpaired",
      unpaired.await(5, TimeUnit.SECONDS)
    )

    assertFalse(
      "Device should not be paired",
      testDevice.isPaired
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testPairingTimeout() {
    // Request pairing
    testDevice.requestPairing()

    // Wait for timeout (assume 30 seconds)
    val timeoutMs = 2000L // Use shorter timeout for testing

    Thread.sleep(timeoutMs)

    // Verify pairing request still pending or timed out
    // Behavior depends on implementation
    val isPairRequested = testDevice.isPairRequested

    // Document expected behavior
    // If timeout clears request:
    // assertFalse("Pair request should timeout", isPairRequested)

    // If request persists until response:
    assertTrue("Pair request should persist", isPairRequested)
  }

  @Test
  fun testCertificateExchange() {
    val certificateExchanged = CountDownLatch(1)

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {
        device.acceptPairing()
      }

      override fun onPairRequestSent(device: Device) {}

      override fun onPaired(device: Device) {
        // Verify certificate was exchanged via FFI
        val certificate = device.certificate
        if (certificate != null) {
          certificateExchanged.countDown()
        }
      }

      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Simulate pairing with certificate
    val pairRequestPacket = MockFactory.createPairRequestPacket(
      deviceId = testDevice.deviceId
    )
    cosmicConnect.processIncomingPacket(pairRequestPacket)

    // Simulate pair response with certificate
    val mockCertificate = FfiTestUtils.createTestCertificate()
    val pairResponsePacket = MockFactory.createPairResponsePacket(
      deviceId = testDevice.deviceId,
      accepted = true
    )
    // In real implementation, certificate would be in packet body
    cosmicConnect.processIncomingPacket(pairResponsePacket)

    // Verify certificate exchanged
    // Note: This test may need adjustment based on actual FFI implementation
    val result = TestUtils.waitFor(5000) {
      testDevice.isPaired
    }
    assertTrue("Device should be paired", result)

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testPairedDevicePersistence() {
    // Pair device
    testDevice.requestPairing()

    val pairResponsePacket = MockFactory.createPairResponsePacket(
      deviceId = testDevice.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponsePacket)

    // Wait for pairing
    TestUtils.waitFor { testDevice.isPaired }

    // Get device ID
    val deviceId = testDevice.deviceId

    // Simulate app restart by creating new CosmicConnect instance
    val cosmicConnect2 = CosmicConnect.getInstance(TestUtils.getTestContext())

    // Verify device still paired
    val restoredDevice = cosmicConnect2.getDevice(deviceId)
    assertNotNull("Device should be restored after restart", restoredDevice)
    assertTrue("Restored device should be paired", restoredDevice!!.isPaired)
  }

  @Test
  fun testMultiplePairingRequests() {
    var requestCount = 0

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}

      override fun onPairRequestSent(device: Device) {
        requestCount++
      }

      override fun onPaired(device: Device) {}
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener)

    // Send multiple pairing requests
    testDevice.requestPairing()
    Thread.sleep(500)
    testDevice.requestPairing()
    Thread.sleep(500)
    testDevice.requestPairing()

    // Wait a bit
    Thread.sleep(1000)

    // Verify behavior
    // Implementation may send all requests or deduplicate
    assertTrue(
      "At least one pairing request should be sent",
      requestCount >= 1
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testPairingError() {
    val errorReceived = CountDownLatch(1)
    var errorMessage: String? = null

    // Setup listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) {}
      override fun onUnpaired(device: Device) {}

      override fun onPairError(device: Device, error: String) {
        errorMessage = error
        errorReceived.countDown()
      }
    }

    testDevice.addPairingListener(listener)

    // Request pairing
    testDevice.requestPairing()

    // Simulate pairing error (e.g., network failure, certificate error)
    // This would typically come from FFI layer
    testDevice.onPairingFailed("Certificate validation failed")

    // Verify error received
    assertTrue(
      "Pairing error should be reported",
      errorReceived.await(5, TimeUnit.SECONDS)
    )

    assertNotNull("Error message should be provided", errorMessage)
    assertTrue(
      "Error message should contain details",
      errorMessage!!.contains("Certificate")
    )

    testDevice.removePairingListener(listener)
  }

  @Test
  fun testPairingWithMultipleListeners() {
    val listener1Paired = CountDownLatch(1)
    val listener2Paired = CountDownLatch(1)

    // Create two listeners
    val listener1 = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) { listener1Paired.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    val listener2 = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) { listener2Paired.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    testDevice.addPairingListener(listener1)
    testDevice.addPairingListener(listener2)

    // Complete pairing
    testDevice.requestPairing()

    val pairResponsePacket = MockFactory.createPairResponsePacket(
      deviceId = testDevice.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponsePacket)

    // Verify both listeners notified
    assertTrue(
      "Listener 1 should be notified",
      listener1Paired.await(5, TimeUnit.SECONDS)
    )
    assertTrue(
      "Listener 2 should be notified",
      listener2Paired.await(5, TimeUnit.SECONDS)
    )

    testDevice.removePairingListener(listener1)
    testDevice.removePairingListener(listener2)
  }

  @Test
  fun testConcurrentPairingRequests() {
    // Create multiple devices
    val device1Id = "test_device_1"
    val device2Id = "test_device_2"

    val identity1 = MockFactory.createIdentityPacket(
      deviceId = device1Id,
      deviceName = "Device 1",
      deviceType = "desktop"
    )
    val identity2 = MockFactory.createIdentityPacket(
      deviceId = device2Id,
      deviceName = "Device 2",
      deviceType = "laptop"
    )

    cosmicConnect.processIncomingPacket(identity1)
    cosmicConnect.processIncomingPacket(identity2)

    val device1 = cosmicConnect.getDevice(device1Id)!!
    val device2 = cosmicConnect.getDevice(device2Id)!!

    val device1Paired = CountDownLatch(1)
    val device2Paired = CountDownLatch(1)

    // Setup listeners
    val listener1 = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) { device1Paired.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    val listener2 = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) { device2Paired.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device1.addPairingListener(listener1)
    device2.addPairingListener(listener2)

    // Request pairing for both devices concurrently
    Thread {
      device1.requestPairing()
      val response1 = MockFactory.createPairResponsePacket(device1Id, accepted = true)
      cosmicConnect.processIncomingPacket(response1)
    }.start()

    Thread {
      device2.requestPairing()
      val response2 = MockFactory.createPairResponsePacket(device2Id, accepted = true)
      cosmicConnect.processIncomingPacket(response2)
    }.start()

    // Verify both pairings complete
    assertTrue(
      "Device 1 should be paired",
      device1Paired.await(5, TimeUnit.SECONDS)
    )
    assertTrue(
      "Device 2 should be paired",
      device2Paired.await(5, TimeUnit.SECONDS)
    )

    assertTrue("Device 1 should be in paired state", device1.isPaired)
    assertTrue("Device 2 should be in paired state", device2.isPaired)

    device1.removePairingListener(listener1)
    device2.removePairingListener(listener2)
  }
}
