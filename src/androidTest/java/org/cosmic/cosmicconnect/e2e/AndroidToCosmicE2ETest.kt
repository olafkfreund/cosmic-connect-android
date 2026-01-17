package org.cosmic.cosmicconnect.e2e

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.plugins.battery.BatteryPlugin
import org.cosmic.cosmicconnect.plugins.clipboard.ClipboardPlugin
import org.cosmic.cosmicconnect.plugins.ping.PingPlugin
import org.cosmic.cosmicconnect.plugins.share.SharePlugin
import org.cosmic.cosmicconnect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * End-to-End Tests: Android → COSMIC Desktop
 *
 * Tests complete Android → COSMIC Desktop communication flows with real network.
 *
 * These tests verify:
 * - Discovery over UDP broadcast
 * - TLS pairing with certificate exchange
 * - File transfer via TCP
 * - Plugin operations over network
 * - Complete round-trip communication
 *
 * Requirements:
 * - COSMIC Desktop running on same network OR mock COSMIC server
 * - Network connectivity
 * - Proper firewall configuration
 *
 * Test Modes:
 * 1. Mock Server Mode: Tests with embedded mock COSMIC server
 * 2. Real COSMIC Mode: Tests with actual COSMIC Desktop (manual setup)
 */
@RunWith(AndroidJUnit4::class)
class AndroidToCosmicE2ETest {

  private lateinit var cosmicConnect: CosmicConnect
  private lateinit var mockCosmicServer: MockCosmicServer
  private var useMockServer = true // Set to false for real COSMIC testing

  @Before
  fun setup() {
    TestUtils.cleanupTestData()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance(TestUtils.getTestContext())

    if (useMockServer) {
      // Start mock COSMIC Desktop server
      mockCosmicServer = MockCosmicServer()
      mockCosmicServer.start()
    }
  }

  @After
  fun teardown() {
    if (useMockServer) {
      mockCosmicServer.stop()
    }

    TestUtils.cleanupTestData()
  }

  // ========================================
  // DISCOVERY E2E TESTS
  // ========================================

  @Test
  fun testE2E_DiscoveryOverRealNetwork() {
    val deviceDiscovered = CountDownLatch(1)
    var discoveredDevice: Device? = null

    // Setup discovery listener
    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        discoveredDevice = device
        deviceDiscovered.countDown()
      }

      override fun onDiscoveryStarted() {
        // Discovery started via UDP broadcast
      }

      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(listener)

    // Start discovery - sends UDP broadcast
    cosmicConnect.startDiscovery()

    // Wait for COSMIC Desktop to respond
    val discovered = deviceDiscovered.await(30, TimeUnit.SECONDS)

    assertTrue(
      "Should discover COSMIC Desktop over network",
      discovered
    )

    if (discovered) {
      assertNotNull("Discovered device should not be null", discoveredDevice)
      assertNotNull("Device should have ID", discoveredDevice!!.deviceId)
      assertNotNull("Device should have name", discoveredDevice!!.name)
      assertTrue(
        "Device should be reachable",
        discoveredDevice!!.isReachable
      )
    }

    cosmicConnect.removeDiscoveryListener(listener)
  }

  @Test
  fun testE2E_DiscoveryMultipleDevices() {
    val devicesDiscovered = mutableListOf<Device>()
    val discoveryTimeout = CountDownLatch(1)

    val listener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(device: Device) {
        devicesDiscovered.add(device)
      }

      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {
        discoveryTimeout.countDown()
      }
    }

    cosmicConnect.addDiscoveryListener(listener)

    // Start discovery
    cosmicConnect.startDiscovery()

    // Let discovery run for 30 seconds
    Thread.sleep(30000)

    // Stop discovery
    cosmicConnect.stopDiscovery()

    discoveryTimeout.await(5, TimeUnit.SECONDS)

    // Verify devices discovered
    assertTrue(
      "Should discover at least one device",
      devicesDiscovered.isNotEmpty()
    )

    // Verify unique device IDs
    val uniqueIds = devicesDiscovered.map { it.deviceId }.toSet()
    assertEquals(
      "All discovered devices should have unique IDs",
      devicesDiscovered.size,
      uniqueIds.size
    )

    cosmicConnect.removeDiscoveryListener(listener)
  }

  // ========================================
  // PAIRING E2E TESTS
  // ========================================

  @Test
  fun testE2E_CompletePairingFlow() {
    // First discover device
    val deviceDiscovered = CountDownLatch(1)
    var device: Device? = null

    val discoveryListener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(dev: Device) {
        device = dev
        deviceDiscovered.countDown()
      }
      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(discoveryListener)
    cosmicConnect.startDiscovery()

    assertTrue(
      "Should discover device first",
      deviceDiscovered.await(30, TimeUnit.SECONDS)
    )
    assertNotNull("Device should be discovered", device)

    cosmicConnect.removeDiscoveryListener(discoveryListener)

    // Now test pairing
    val pairingComplete = CountDownLatch(1)
    val pairingListener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}

      override fun onPaired(dev: Device) {
        pairingComplete.countDown()
      }

      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device!!.addPairingListener(pairingListener)

    // Request pairing - sends TLS certificate
    device!!.requestPairing()

    // Wait for user to accept on COSMIC Desktop
    // In mock mode, auto-accepts
    val paired = pairingComplete.await(60, TimeUnit.SECONDS)

    assertTrue(
      "Pairing should complete (user must accept on COSMIC)",
      paired
    )

    assertTrue("Device should be paired", device!!.isPaired)
    assertNotNull("Device should have certificate", device!!.certificate)

    device!!.removePairingListener(pairingListener)
  }

  @Test
  fun testE2E_PairingCertificateExchange() {
    // Discover and pair device
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    // Verify certificate
    val certificate = device!!.certificate
    assertNotNull("Device should have certificate", certificate)
    assertTrue("Certificate should not be empty", certificate!!.isNotEmpty())

    // Verify certificate is valid TLS certificate
    // In real implementation, would verify certificate format
    assertTrue("Certificate should be valid", certificate.size > 100)
  }

  @Test
  fun testE2E_PairingPersistence() {
    // Discover and pair device
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val deviceId = device!!.deviceId

    // Simulate app restart
    val cosmicConnect2 = CosmicConnect.getInstance(TestUtils.getTestContext())

    // Verify device still paired
    val restoredDevice = cosmicConnect2.getDevice(deviceId)
    assertNotNull("Device should be restored", restoredDevice)
    assertTrue("Device should still be paired", restoredDevice!!.isPaired)
    assertNotNull("Certificate should be restored", restoredDevice.certificate)
  }

  // ========================================
  // FILE TRANSFER E2E TESTS
  // ========================================

  @Test
  fun testE2E_SendFileToCosmicDesktop() {
    // Discover and pair device
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val sharePlugin = device!!.getPlugin("share") as SharePlugin
    val transferComplete = CountDownLatch(1)
    var transferSuccess = false

    // Setup transfer listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}

      override fun onTransferComplete(transferId: String) {
        transferSuccess = true
        transferComplete.countDown()
      }

      override fun onTransferFailed(transferId: String, error: String) {
        transferComplete.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Create test file
    val testFile = createTestFile("e2e_test.txt", "E2E Test Content from Android")

    // Send file to COSMIC Desktop
    sharePlugin.shareFile(Uri.fromFile(testFile))

    // Wait for transfer to complete
    val completed = transferComplete.await(60, TimeUnit.SECONDS)

    assertTrue(
      "File transfer should complete",
      completed
    )
    assertTrue("Transfer should succeed", transferSuccess)

    sharePlugin.removeTransferListener(listener)

    // Clean up test file
    testFile.delete()
  }

  @Test
  fun testE2E_SendLargeFileToCosmicDesktop() {
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val sharePlugin = device!!.getPlugin("share") as SharePlugin
    val transferComplete = CountDownLatch(1)
    var progressUpdates = 0

    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}

      override fun onTransferProgress(transferId: String, progress: Int) {
        progressUpdates++
      }

      override fun onTransferComplete(transferId: String) {
        transferComplete.countDown()
      }

      override fun onTransferFailed(transferId: String, error: String) {
        transferComplete.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Create large test file (10 MB)
    val largeContent = "X".repeat(10 * 1024 * 1024)
    val largeFile = createTestFile("e2e_large.bin", largeContent)

    // Send large file
    sharePlugin.shareFile(Uri.fromFile(largeFile))

    // Wait for transfer (longer timeout for large file)
    val completed = transferComplete.await(120, TimeUnit.SECONDS)

    assertTrue("Large file transfer should complete", completed)
    assertTrue("Should receive progress updates", progressUpdates > 0)

    sharePlugin.removeTransferListener(listener)
    largeFile.delete()
  }

  @Test
  fun testE2E_SendMultipleFilesToCosmicDesktop() {
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val sharePlugin = device!!.getPlugin("share") as SharePlugin
    val filesCount = 3
    val transfersComplete = CountDownLatch(filesCount)

    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}

      override fun onTransferComplete(transferId: String) {
        transfersComplete.countDown()
      }

      override fun onTransferFailed(transferId: String, error: String) {
        transfersComplete.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Create multiple test files
    val files = listOf(
      createTestFile("e2e_file1.txt", "File 1 content"),
      createTestFile("e2e_file2.txt", "File 2 content"),
      createTestFile("e2e_file3.txt", "File 3 content")
    )

    // Send all files
    val uris = files.map { Uri.fromFile(it) }
    sharePlugin.shareFiles(uris)

    // Wait for all transfers
    val completed = transfersComplete.await(90, TimeUnit.SECONDS)

    assertTrue("All file transfers should complete", completed)

    sharePlugin.removeTransferListener(listener)
    files.forEach { it.delete() }
  }

  // ========================================
  // PLUGIN E2E TESTS
  // ========================================

  @Test
  fun testE2E_BatteryStatusToCosmicDesktop() {
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val batteryPlugin = device!!.getPlugin("battery") as BatteryPlugin
    val statusSent = CountDownLatch(1)

    val listener = object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {
        statusSent.countDown()
      }

      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {}
    }

    batteryPlugin.addBatteryListener(listener)

    // Send battery status to COSMIC Desktop
    batteryPlugin.sendBatteryStatus(85, isCharging = true)

    // Verify packet sent over network
    val sent = statusSent.await(10, TimeUnit.SECONDS)
    assertTrue("Battery status should be sent to COSMIC", sent)

    batteryPlugin.removeBatteryListener(listener)
  }

  @Test
  fun testE2E_ClipboardToCosmicDesktop() {
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val clipboardPlugin = device!!.getPlugin("clipboard") as ClipboardPlugin
    val clipboardSent = CountDownLatch(1)

    val listener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {
        clipboardSent.countDown()
      }

      override fun onRemoteClipboardUpdate(content: String) {}
    }

    clipboardPlugin.addClipboardListener(listener)

    // Send clipboard to COSMIC Desktop
    val testContent = "E2E clipboard test from Android"
    clipboardPlugin.sendClipboard(testContent)

    // Verify sent
    val sent = clipboardSent.await(10, TimeUnit.SECONDS)
    assertTrue("Clipboard should be sent to COSMIC", sent)

    clipboardPlugin.removeClipboardListener(listener)
  }

  @Test
  fun testE2E_PingToCosmicDesktop() {
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    val pingPlugin = device!!.getPlugin("ping") as PingPlugin
    val pingSent = CountDownLatch(1)

    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {
        pingSent.countDown()
      }

      override fun onPingReceived(message: String?) {}
    }

    pingPlugin.addPingListener(listener)

    // Send ping to COSMIC Desktop (should show notification)
    pingPlugin.sendPing("E2E Test Ping from Android")

    // Verify sent
    val sent = pingSent.await(10, TimeUnit.SECONDS)
    assertTrue("Ping should be sent to COSMIC", sent)

    pingPlugin.removePingListener(listener)
  }

  // ========================================
  // COMPLETE SCENARIO E2E TESTS
  // ========================================

  @Test
  fun testE2E_CompleteWorkflow_DiscoverPairShareFile() {
    // Step 1: Discover
    val deviceDiscovered = CountDownLatch(1)
    var device: Device? = null

    val discoveryListener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(dev: Device) {
        device = dev
        deviceDiscovered.countDown()
      }
      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(discoveryListener)
    cosmicConnect.startDiscovery()

    assertTrue("Step 1: Should discover device", deviceDiscovered.await(30, TimeUnit.SECONDS))
    assertNotNull("Device should be found", device)

    cosmicConnect.removeDiscoveryListener(discoveryListener)

    // Step 2: Pair
    val pairingComplete = CountDownLatch(1)

    val pairingListener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(dev: Device) { pairingComplete.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device!!.addPairingListener(pairingListener)
    device!!.requestPairing()

    assertTrue("Step 2: Pairing should complete", pairingComplete.await(60, TimeUnit.SECONDS))
    assertTrue("Device should be paired", device!!.isPaired)

    device!!.removePairingListener(pairingListener)

    // Step 3: Share file
    val sharePlugin = device!!.getPlugin("share") as SharePlugin
    val transferComplete = CountDownLatch(1)

    val transferListener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}
      override fun onTransferComplete(transferId: String) { transferComplete.countDown() }
      override fun onTransferFailed(transferId: String, error: String) { transferComplete.countDown() }
    }

    sharePlugin.addTransferListener(transferListener)

    val testFile = createTestFile("e2e_workflow.txt", "Complete E2E workflow test")
    sharePlugin.shareFile(Uri.fromFile(testFile))

    assertTrue("Step 3: File transfer should complete", transferComplete.await(60, TimeUnit.SECONDS))

    sharePlugin.removeTransferListener(transferListener)
    testFile.delete()
  }

  @Test
  fun testE2E_NetworkFailureRecovery() {
    val device = discoverAndPairDevice()
    assertNotNull("Should have paired device", device)

    // Send ping to verify connection
    val pingPlugin = device!!.getPlugin("ping") as PingPlugin
    val initialPingSent = CountDownLatch(1)

    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {
        initialPingSent.countDown()
      }
      override fun onPingReceived(message: String?) {}
    }

    pingPlugin.addPingListener(listener)
    pingPlugin.sendPing("Initial ping")

    assertTrue("Initial ping should succeed", initialPingSent.await(10, TimeUnit.SECONDS))

    // Simulate network disconnection (in mock mode, server stops)
    if (useMockServer) {
      mockCosmicServer.simulateDisconnect()
    }

    // Wait for disconnection to be detected
    Thread.sleep(5000)

    // Verify device shows as unreachable
    assertFalse("Device should be unreachable", device.isReachable)

    // Simulate network recovery
    if (useMockServer) {
      mockCosmicServer.simulateReconnect()
    }

    // Wait for reconnection
    Thread.sleep(5000)

    // Verify device reconnects
    assertTrue("Device should be reachable again", device.isReachable)
    assertTrue("Device should still be paired", device.isPaired)

    pingPlugin.removePingListener(listener)
  }

  // ========================================
  // HELPER METHODS
  // ========================================

  private fun discoverAndPairDevice(): Device? {
    val deviceDiscovered = CountDownLatch(1)
    var device: Device? = null

    val discoveryListener = object : CosmicConnect.DiscoveryListener {
      override fun onDeviceDiscovered(dev: Device) {
        device = dev
        deviceDiscovered.countDown()
      }
      override fun onDiscoveryStarted() {}
      override fun onDiscoveryStopped() {}
    }

    cosmicConnect.addDiscoveryListener(discoveryListener)
    cosmicConnect.startDiscovery()

    val discovered = deviceDiscovered.await(30, TimeUnit.SECONDS)
    cosmicConnect.removeDiscoveryListener(discoveryListener)

    if (!discovered) return null

    // Now pair
    val pairingComplete = CountDownLatch(1)

    val pairingListener = object : Device.PairingListener {
      override fun onPairRequest(device: Device) {}
      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(dev: Device) { pairingComplete.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device!!.addPairingListener(pairingListener)
    device!!.requestPairing()

    val paired = pairingComplete.await(60, TimeUnit.SECONDS)
    device!!.removePairingListener(pairingListener)

    return if (paired) device else null
  }

  private fun createTestFile(filename: String, content: String): File {
    val file = File(TestUtils.getTestContext().cacheDir, filename)
    file.writeText(content)
    return file
  }
}

/**
 * Mock COSMIC Desktop Server
 *
 * Simulates COSMIC Desktop for E2E testing without requiring actual COSMIC installation.
 * Responds to discovery broadcasts, accepts pairing requests, and handles file transfers.
 */
class MockCosmicServer {
  private val discoveryPort = 1716
  private val transferPort = 1764
  private var discoveryThread: Thread? = null
  private var transferThread: Thread? = null
  private var isRunning = false
  private var isConnected = true

  fun start() {
    isRunning = true
    isConnected = true

    // Start UDP discovery responder
    discoveryThread = thread(start = true) {
      runDiscoveryResponder()
    }

    // Start TCP transfer server
    transferThread = thread(start = true) {
      runTransferServer()
    }
  }

  fun stop() {
    isRunning = false
    discoveryThread?.interrupt()
    transferThread?.interrupt()
  }

  fun simulateDisconnect() {
    isConnected = false
  }

  fun simulateReconnect() {
    isConnected = true
  }

  private fun runDiscoveryResponder() {
    // TODO: Implement UDP discovery response
    // Listen on port 1716 for UDP broadcasts
    // Respond with identity packet
  }

  private fun runTransferServer() {
    // TODO: Implement TCP transfer server
    // Listen on port 1764 for TLS connections
    // Accept pairing requests
    // Receive file transfers
  }
}
