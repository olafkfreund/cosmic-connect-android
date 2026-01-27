package org.cosmic.cconnect.e2e

import android.content.ClipboardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cconnect.CosmicConnect
import org.cosmic.cconnect.Device
import org.cosmic.cconnect.plugins.battery.BatteryPlugin
import org.cosmic.cconnect.plugins.clipboard.ClipboardPlugin
import org.cosmic.cconnect.plugins.mpris.MprisPlugin
import org.cosmic.cconnect.plugins.ping.PingPlugin
import org.cosmic.cconnect.plugins.runcommand.RunCommandPlugin
import org.cosmic.cconnect.plugins.share.SharePlugin
import org.cosmic.cconnect.test.MockFactory
import org.cosmic.cconnect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-End Tests: COSMIC Desktop → Android
 *
 * Tests complete COSMIC Desktop → Android communication flows with real network.
 *
 * These tests verify:
 * - Receiving pairing requests from COSMIC
 * - Receiving files from COSMIC via TCP
 * - Receiving plugin data from COSMIC
 * - Complete round-trip communication
 *
 * Requirements:
 * - COSMIC Desktop running on same network OR mock COSMIC client
 * - Paired device
 * - Network connectivity
 *
 * Test Modes:
 * 1. Mock Client Mode: Tests with simulated COSMIC Desktop requests
 * 2. Real COSMIC Mode: Tests with actual COSMIC Desktop (manual setup)
 */
@RunWith(AndroidJUnit4::class)
class CosmicToAndroidE2ETest {

  private lateinit var cosmicConnect: CosmicConnect
  private lateinit var mockCosmicClient: MockCosmicClient
  private lateinit var context: Context
  private var useMockClient = true // Set to false for real COSMIC testing

  @Before
  fun setup() {
    TestUtils.cleanupTestData()
    context = TestUtils.getTestContext()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance(context)

    if (useMockClient) {
      // Start mock COSMIC Desktop client
      mockCosmicClient = MockCosmicClient()
      mockCosmicClient.start()
    }
  }

  @After
  fun teardown() {
    if (useMockClient) {
      mockCosmicClient.stop()
    }

    TestUtils.cleanupTestData()
  }

  // ========================================
  // PAIRING REQUEST E2E TESTS
  // ========================================

  @Test
  fun testE2E_ReceivePairingRequestFromCosmic() {
    val pairRequestReceived = CountDownLatch(1)
    var requestingDevice: Device? = null

    // First, discover a device (or use mock to create one)
    val device = setupUnpairedDevice()

    // Setup pairing listener
    val listener = object : Device.PairingListener {
      override fun onPairRequest(dev: Device) {
        requestingDevice = dev
        pairRequestReceived.countDown()
      }

      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) {}
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device.addPairingListener(listener)

    // Simulate COSMIC sending pairing request
    if (useMockClient) {
      mockCosmicClient.sendPairingRequest(device.deviceId)
    }
    // In real mode, user initiates pairing from COSMIC Desktop

    // Wait for pairing request
    val received = pairRequestReceived.await(30, TimeUnit.SECONDS)

    assertTrue(
      "Should receive pairing request from COSMIC",
      received
    )

    assertNotNull("Requesting device should not be null", requestingDevice)
    assertTrue("Device should show pair requested by peer", device.isPairRequestedByPeer)

    device.removePairingListener(listener)
  }

  @Test
  fun testE2E_AcceptPairingRequestFromCosmic() {
    val device = setupUnpairedDevice()
    val pairingComplete = CountDownLatch(1)

    val listener = object : Device.PairingListener {
      override fun onPairRequest(dev: Device) {
        // Accept pairing request
        dev.acceptPairing()
      }

      override fun onPairRequestSent(device: Device) {}

      override fun onPaired(dev: Device) {
        pairingComplete.countDown()
      }

      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device.addPairingListener(listener)

    // COSMIC sends pairing request
    if (useMockClient) {
      mockCosmicClient.sendPairingRequest(device.deviceId)
      // Simulate COSMIC accepting our response
      Thread.sleep(1000)
      mockCosmicClient.completePairing(device.deviceId)
    }

    // Wait for pairing to complete
    val paired = pairingComplete.await(30, TimeUnit.SECONDS)

    assertTrue("Pairing should complete", paired)
    assertTrue("Device should be paired", device.isPaired)
    assertNotNull("Device should have certificate", device.certificate)

    device.removePairingListener(listener)
  }

  @Test
  fun testE2E_RejectPairingRequestFromCosmic() {
    val device = setupUnpairedDevice()
    val pairRequestReceived = CountDownLatch(1)

    val listener = object : Device.PairingListener {
      override fun onPairRequest(dev: Device) {
        pairRequestReceived.countDown()
        // Reject pairing request
        dev.rejectPairing()
      }

      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(device: Device) {}
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device.addPairingListener(listener)

    // COSMIC sends pairing request
    if (useMockClient) {
      mockCosmicClient.sendPairingRequest(device.deviceId)
    }

    // Wait for request
    assertTrue(
      "Should receive pairing request",
      pairRequestReceived.await(30, TimeUnit.SECONDS)
    )

    // Wait for rejection to process
    Thread.sleep(1000)

    // Verify still not paired
    assertFalse("Device should not be paired after rejection", device.isPaired)

    device.removePairingListener(listener)
  }

  // ========================================
  // FILE RECEIVE E2E TESTS
  // ========================================

  @Test
  fun testE2E_ReceiveFileFromCosmic() {
    val device = setupPairedDevice()
    val sharePlugin = device.getPlugin("share") as SharePlugin

    val fileReceived = CountDownLatch(1)
    var receivedFile: File? = null

    // Setup receive listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFile = file
        fileReceived.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // COSMIC sends file
    val testContent = "E2E Test File from COSMIC Desktop"
    val testFilename = "cosmic_test.txt"

    if (useMockClient) {
      mockCosmicClient.sendFile(
        deviceId = device.deviceId,
        filename = testFilename,
        content = testContent.toByteArray()
      )
    }
    // In real mode, user sends file from COSMIC Desktop

    // Wait for file reception
    val received = fileReceived.await(60, TimeUnit.SECONDS)

    assertTrue("File should be received from COSMIC", received)
    assertNotNull("Received file should not be null", receivedFile)
    assertTrue("Received file should exist", receivedFile!!.exists())

    // Verify content
    val actualContent = receivedFile!!.readText()
    assertEquals("File content should match", testContent, actualContent)

    sharePlugin.removeReceiveListener(listener)

    // Cleanup
    receivedFile?.delete()
  }

  @Test
  fun testE2E_ReceiveLargeFileFromCosmic() {
    val device = setupPairedDevice()
    val sharePlugin = device.getPlugin("share") as SharePlugin

    val fileReceived = CountDownLatch(1)
    var progressUpdates = 0
    var receivedFile: File? = null

    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFile = file
        fileReceived.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {
        progressUpdates++
      }

      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // COSMIC sends large file (5 MB)
    val largeSize = 5 * 1024 * 1024
    val largeContent = ByteArray(largeSize) { (it % 256).toByte() }

    if (useMockClient) {
      mockCosmicClient.sendFile(
        deviceId = device.deviceId,
        filename = "large_file.bin",
        content = largeContent
      )
    }

    // Wait for large file
    val received = fileReceived.await(120, TimeUnit.SECONDS)

    assertTrue("Large file should be received", received)
    assertNotNull("Received file should not be null", receivedFile)
    assertTrue("Should receive progress updates", progressUpdates > 0)

    // Verify size
    assertEquals("File size should match", largeSize.toLong(), receivedFile!!.length())

    sharePlugin.removeReceiveListener(listener)
    receivedFile?.delete()
  }

  @Test
  fun testE2E_ReceiveMultipleFilesFromCosmic() {
    val device = setupPairedDevice()
    val sharePlugin = device.getPlugin("share") as SharePlugin

    val filesCount = 3
    val filesReceived = CountDownLatch(filesCount)
    val receivedFiles = mutableListOf<File>()

    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFiles.add(file)
        filesReceived.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // COSMIC sends multiple files
    if (useMockClient) {
      repeat(filesCount) { index ->
        mockCosmicClient.sendFile(
          deviceId = device.deviceId,
          filename = "file_$index.txt",
          content = "Content $index".toByteArray()
        )
        Thread.sleep(500) // Small delay between files
      }
    }

    // Wait for all files
    val received = filesReceived.await(90, TimeUnit.SECONDS)

    assertTrue("All files should be received", received)
    assertEquals("Should receive $filesCount files", filesCount, receivedFiles.size)
    assertTrue("All files should exist", receivedFiles.all { it.exists() })

    sharePlugin.removeReceiveListener(listener)
    receivedFiles.forEach { it.delete() }
  }

  // ========================================
  // PLUGIN DATA RECEIVE E2E TESTS
  // ========================================

  @Test
  fun testE2E_ReceiveBatteryStatusFromCosmic() {
    val device = setupPairedDevice()
    val batteryPlugin = device.getPlugin("battery") as BatteryPlugin

    val batteryReceived = CountDownLatch(1)
    var receivedLevel = -1
    var receivedCharging = false

    val listener = object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {}

      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {
        receivedLevel = level
        receivedCharging = isCharging
        batteryReceived.countDown()
      }
    }

    batteryPlugin.addBatteryListener(listener)

    // COSMIC sends battery status
    if (useMockClient) {
      mockCosmicClient.sendBatteryStatus(
        deviceId = device.deviceId,
        level = 65,
        isCharging = true
      )
    }

    // Wait for battery update
    val received = batteryReceived.await(10, TimeUnit.SECONDS)

    assertTrue("Should receive battery status from COSMIC", received)
    assertEquals("Battery level should match", 65, receivedLevel)
    assertTrue("Charging state should match", receivedCharging)

    batteryPlugin.removeBatteryListener(listener)
  }

  @Test
  fun testE2E_ReceiveClipboardFromCosmic() {
    val device = setupPairedDevice()
    val clipboardPlugin = device.getPlugin("clipboard") as ClipboardPlugin

    val clipboardReceived = CountDownLatch(1)
    var receivedContent = ""

    val listener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {}

      override fun onRemoteClipboardUpdate(content: String) {
        receivedContent = content
        clipboardReceived.countDown()
      }
    }

    clipboardPlugin.addClipboardListener(listener)

    // COSMIC sends clipboard content
    val testContent = "Clipboard from COSMIC Desktop"
    if (useMockClient) {
      mockCosmicClient.sendClipboard(
        deviceId = device.deviceId,
        content = testContent
      )
    }

    // Wait for clipboard update
    val received = clipboardReceived.await(10, TimeUnit.SECONDS)

    assertTrue("Should receive clipboard from COSMIC", received)
    assertEquals("Clipboard content should match", testContent, receivedContent)

    // Verify Android clipboard updated (if auto-sync enabled)
    if (clipboardPlugin.isAutoSyncEnabled()) {
      val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val androidClipboard = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
      assertEquals("Android clipboard should be updated", testContent, androidClipboard)
    }

    clipboardPlugin.removeClipboardListener(listener)
  }

  @Test
  fun testE2E_ReceivePingFromCosmic() {
    val device = setupPairedDevice()
    val pingPlugin = device.getPlugin("ping") as PingPlugin

    val pingReceived = CountDownLatch(1)
    var receivedMessage: String? = null

    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {}

      override fun onPingReceived(message: String?) {
        receivedMessage = message
        pingReceived.countDown()
      }
    }

    pingPlugin.addPingListener(listener)

    // COSMIC sends ping
    val testMessage = "Ping from COSMIC Desktop"
    if (useMockClient) {
      mockCosmicClient.sendPing(
        deviceId = device.deviceId,
        message = testMessage
      )
    }

    // Wait for ping
    val received = pingReceived.await(10, TimeUnit.SECONDS)

    assertTrue("Should receive ping from COSMIC", received)
    assertEquals("Ping message should match", testMessage, receivedMessage)

    // Verify notification shown (implementation-dependent)
    // In real app, would show notification

    pingPlugin.removePingListener(listener)
  }

  @Test
  fun testE2E_ReceiveCommandListFromCosmic() {
    val device = setupPairedDevice()
    val runCommandPlugin = device.getPlugin("runcommand") as RunCommandPlugin

    val commandListReceived = CountDownLatch(1)
    var receivedCommands: Map<String, String>? = null

    val listener = object : RunCommandPlugin.CommandListener {
      override fun onCommandSent(key: String, command: String) {}

      override fun onCommandListReceived(commands: Map<String, String>) {
        receivedCommands = commands
        commandListReceived.countDown()
      }
    }

    runCommandPlugin.addCommandListener(listener)

    // COSMIC sends command list
    val testCommands = mapOf(
      "lock_screen" to "loginctl lock-session",
      "suspend" to "systemctl suspend",
      "screenshot" to "gnome-screenshot"
    )

    if (useMockClient) {
      mockCosmicClient.sendCommandList(
        deviceId = device.deviceId,
        commands = testCommands
      )
    }

    // Wait for command list
    val received = commandListReceived.await(10, TimeUnit.SECONDS)

    assertTrue("Should receive command list from COSMIC", received)
    assertNotNull("Command list should not be null", receivedCommands)
    assertEquals("Should have 3 commands", 3, receivedCommands!!.size)
    assertTrue("Should have lock_screen command", receivedCommands!!.containsKey("lock_screen"))

    runCommandPlugin.removeCommandListener(listener)
  }

  @Test
  fun testE2E_ReceiveMediaStatusFromCosmic() {
    val device = setupPairedDevice()
    val mprisPlugin = device.getPlugin("mpris") as MprisPlugin

    val statusReceived = CountDownLatch(1)
    var isPlaying = false
    var title: String? = null
    var artist: String? = null

    val listener = object : MprisPlugin.MediaControlListener {
      override fun onMediaControlSent(action: String) {}

      override fun onMediaStatusReceived(
        playing: Boolean,
        songTitle: String?,
        songArtist: String?,
        album: String?
      ) {
        isPlaying = playing
        title = songTitle
        artist = songArtist
        statusReceived.countDown()
      }
    }

    mprisPlugin.addMediaControlListener(listener)

    // COSMIC sends media status
    if (useMockClient) {
      mockCosmicClient.sendMediaStatus(
        deviceId = device.deviceId,
        isPlaying = true,
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album"
      )
    }

    // Wait for status
    val received = statusReceived.await(10, TimeUnit.SECONDS)

    assertTrue("Should receive media status from COSMIC", received)
    assertTrue("Should be playing", isPlaying)
    assertEquals("Title should match", "Test Song", title)
    assertEquals("Artist should match", "Test Artist", artist)

    mprisPlugin.removeMediaControlListener(listener)
  }

  // ========================================
  // COMPLETE SCENARIO E2E TESTS
  // ========================================

  @Test
  fun testE2E_CompleteWorkflow_ReceivePairAndFile() {
    // Step 1: Receive pairing request from COSMIC
    val device = setupUnpairedDevice()
    val pairingComplete = CountDownLatch(1)

    val pairingListener = object : Device.PairingListener {
      override fun onPairRequest(dev: Device) {
        dev.acceptPairing()
      }

      override fun onPairRequestSent(device: Device) {}
      override fun onPaired(dev: Device) { pairingComplete.countDown() }
      override fun onUnpaired(device: Device) {}
      override fun onPairError(device: Device, error: String) {}
    }

    device.addPairingListener(pairingListener)

    if (useMockClient) {
      mockCosmicClient.sendPairingRequest(device.deviceId)
      Thread.sleep(1000)
      mockCosmicClient.completePairing(device.deviceId)
    }

    assertTrue("Step 1: Pairing should complete", pairingComplete.await(30, TimeUnit.SECONDS))
    device.removePairingListener(pairingListener)

    // Step 2: Receive file from COSMIC
    val sharePlugin = device.getPlugin("share") as SharePlugin
    val fileReceived = CountDownLatch(1)
    var receivedFile: File? = null

    val receiveListener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFile = file
        fileReceived.countDown()
      }
      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(receiveListener)

    if (useMockClient) {
      mockCosmicClient.sendFile(
        deviceId = device.deviceId,
        filename = "workflow_test.txt",
        content = "Complete workflow test".toByteArray()
      )
    }

    assertTrue("Step 2: File should be received", fileReceived.await(60, TimeUnit.SECONDS))
    assertNotNull("File should not be null", receivedFile)
    assertTrue("File should exist", receivedFile!!.exists())

    sharePlugin.removeReceiveListener(receiveListener)
    receivedFile?.delete()
  }

  @Test
  fun testE2E_BidirectionalCommunication() {
    val device = setupPairedDevice()
    val pingPlugin = device.getPlugin("ping") as PingPlugin

    // Setup bidirectional ping test
    val pingSent = CountDownLatch(1)
    val pingReceived = CountDownLatch(1)

    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {
        pingSent.countDown()
      }

      override fun onPingReceived(message: String?) {
        pingReceived.countDown()
      }
    }

    pingPlugin.addPingListener(listener)

    // Android sends ping to COSMIC
    pingPlugin.sendPing("Android → COSMIC")

    // Wait for send confirmation
    assertTrue("Ping should be sent to COSMIC", pingSent.await(10, TimeUnit.SECONDS))

    // COSMIC sends ping back to Android
    if (useMockClient) {
      mockCosmicClient.sendPing(
        deviceId = device.deviceId,
        message = "COSMIC → Android"
      )
    }

    // Wait for ping from COSMIC
    assertTrue("Ping should be received from COSMIC", pingReceived.await(10, TimeUnit.SECONDS))

    pingPlugin.removePingListener(listener)
  }

  // ========================================
  // NOTIFICATION E2E TESTS
  // ========================================

  @Test
  fun testE2E_FileTransferNotification() {
    val device = setupPairedDevice()
    val sharePlugin = device.getPlugin("share") as SharePlugin

    val shareRequestReceived = CountDownLatch(1)
    var requestedFilename: String? = null

    // Setup share request listener
    val requestListener = object : SharePlugin.ShareRequestListener {
      override fun onShareRequest(deviceId: String, filename: String) {
        requestedFilename = filename
        shareRequestReceived.countDown()
      }
    }

    sharePlugin.setShareRequestListener(requestListener)

    // COSMIC initiates file transfer (sends request first)
    if (useMockClient) {
      mockCosmicClient.sendShareRequest(
        deviceId = device.deviceId,
        filename = "notification_test.txt",
        fileSize = 1024
      )
    }

    // Wait for share request notification
    val received = shareRequestReceived.await(10, TimeUnit.SECONDS)

    assertTrue("Should receive share request notification", received)
    assertEquals("Filename should match", "notification_test.txt", requestedFilename)

    // User can accept or reject
    // sharePlugin.acceptShare(deviceId, filename)
    // or
    // sharePlugin.rejectShare(deviceId, filename)
  }

  @Test
  fun testE2E_PluginDataNotifications() {
    val device = setupPairedDevice()

    // Test multiple plugin notifications
    val notifications = CountDownLatch(3)

    // Battery notification
    val batteryPlugin = device.getPlugin("battery") as BatteryPlugin
    batteryPlugin.addBatteryListener(object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {}
      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {
        notifications.countDown()
      }
    })

    // Clipboard notification
    val clipboardPlugin = device.getPlugin("clipboard") as ClipboardPlugin
    clipboardPlugin.addClipboardListener(object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {}
      override fun onRemoteClipboardUpdate(content: String) {
        notifications.countDown()
      }
    })

    // Ping notification
    val pingPlugin = device.getPlugin("ping") as PingPlugin
    pingPlugin.addPingListener(object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {}
      override fun onPingReceived(message: String?) {
        notifications.countDown()
      }
    })

    // COSMIC sends all plugin updates
    if (useMockClient) {
      mockCosmicClient.sendBatteryStatus(device.deviceId, 80, true)
      Thread.sleep(500)
      mockCosmicClient.sendClipboard(device.deviceId, "test")
      Thread.sleep(500)
      mockCosmicClient.sendPing(device.deviceId, "test")
    }

    // Verify all notifications received
    assertTrue(
      "All plugin notifications should be received",
      notifications.await(30, TimeUnit.SECONDS)
    )
  }

  // ========================================
  // HELPER METHODS
  // ========================================

  private fun setupUnpairedDevice(): Device {
    // Create unpaired device (either via discovery or mock)
    val deviceId = if (useMockClient) {
      mockCosmicClient.createDevice("Mock COSMIC Desktop")
    } else {
      // In real mode, would discover device
      "real_cosmic_device"
    }

    // Simulate device discovery
    val identityPacket = MockFactory.createIdentityPacket(
      deviceId = deviceId,
      deviceName = "COSMIC Desktop",
      deviceType = "desktop"
    )
    cosmicConnect.processIncomingPacket(identityPacket)

    return cosmicConnect.getDevice(deviceId)!!
  }

  private fun setupPairedDevice(): Device {
    val device = setupUnpairedDevice()

    // Pair the device
    device.requestPairing()

    // Simulate pairing acceptance
    val pairResponse = MockFactory.createPairResponsePacket(
      deviceId = device.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponse)

    TestUtils.waitFor { device.isPaired }

    return device
  }
}

/**
 * Mock COSMIC Desktop Client
 *
 * Simulates COSMIC Desktop sending data to Android for E2E testing.
 */
class MockCosmicClient {
  private var isRunning = false
  private val deviceId = "mock_cosmic_desktop_${System.currentTimeMillis()}"

  fun start() {
    isRunning = true
  }

  fun stop() {
    isRunning = false
  }

  fun createDevice(name: String): String {
    return deviceId
  }

  fun sendPairingRequest(androidDeviceId: String) {
    // TODO: Send pairing request packet to Android
    // For now, simulated via MockFactory in tests
  }

  fun completePairing(androidDeviceId: String) {
    // TODO: Send pairing response packet
  }

  fun sendFile(deviceId: String, filename: String, content: ByteArray) {
    // TODO: Send file transfer packets
  }

  fun sendBatteryStatus(deviceId: String, level: Int, isCharging: Boolean) {
    // TODO: Send battery status packet
  }

  fun sendClipboard(deviceId: String, content: String) {
    // TODO: Send clipboard packet
  }

  fun sendPing(deviceId: String, message: String) {
    // TODO: Send ping packet
  }

  fun sendCommandList(deviceId: String, commands: Map<String, String>) {
    // TODO: Send command list packet
  }

  fun sendMediaStatus(
    deviceId: String,
    isPlaying: Boolean,
    title: String,
    artist: String,
    album: String
  ) {
    // TODO: Send MPRIS status packet
  }

  fun sendShareRequest(deviceId: String, filename: String, fileSize: Long) {
    // TODO: Send share request packet
  }
}
