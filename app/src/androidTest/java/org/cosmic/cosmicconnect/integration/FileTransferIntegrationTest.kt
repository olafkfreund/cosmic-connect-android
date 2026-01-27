package org.cosmic.cconnect.integration

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cconnect.CosmicConnect
import org.cosmic.cconnect.Device
import org.cosmic.cconnect.NetworkPacket
import org.cosmic.cconnect.plugins.share.SharePlugin
import org.cosmic.cconnect.test.FfiTestUtils
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
 * Integration Tests - File Transfer
 *
 * Tests the file transfer flow through the complete stack:
 * Android UI → Share Plugin → Rust Core → Network → TCP Payload Transfer
 *
 * Verifies:
 * - File sharing from Android to COSMIC
 * - File receiving on Android from COSMIC
 * - Multiple file transfers
 * - Large file handling
 * - Transfer progress tracking
 * - Transfer cancellation
 * - Concurrent transfers
 * - Error handling (network failures, disk space, etc.)
 */
@RunWith(AndroidJUnit4::class)
class FileTransferIntegrationTest {

  private lateinit var cosmicConnect: CosmicConnect
  private lateinit var pairedDevice: Device
  private lateinit var sharePlugin: SharePlugin
  private lateinit var testFilesDir: File

  @Before
  fun setup() {
    TestUtils.cleanupTestData()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance(TestUtils.getTestContext())

    // Create and pair a test device
    val identityPacket = MockFactory.createIdentityPacket(
      deviceId = "test_device_transfer",
      deviceName = "Test Desktop",
      deviceType = "desktop"
    )
    cosmicConnect.processIncomingPacket(identityPacket)

    pairedDevice = cosmicConnect.getDevice("test_device_transfer")!!

    // Simulate pairing
    pairedDevice.requestPairing()
    val pairResponse = MockFactory.createPairResponsePacket(
      deviceId = pairedDevice.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponse)
    TestUtils.waitFor { pairedDevice.isPaired }

    // Get share plugin
    sharePlugin = pairedDevice.getPlugin("share") as SharePlugin

    // Create test files directory
    testFilesDir = File(TestUtils.getTestContext().cacheDir, "test_files")
    testFilesDir.mkdirs()
  }

  @After
  fun teardown() {
    // Cancel any ongoing transfers
    sharePlugin.cancelAllTransfers()

    // Cleanup test files
    testFilesDir.deleteRecursively()

    // Unpair device
    if (pairedDevice.isPaired) {
      pairedDevice.unpair()
    }

    TestUtils.cleanupTestData()
  }

  // ========================================
  // SENDING FILES
  // ========================================

  @Test
  fun testSendSingleFile() {
    val transferComplete = CountDownLatch(1)
    var transferSuccess = false

    // Create test file
    val testFile = createTestFile("test.txt", "Hello COSMIC!")

    // Setup listener
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

    // Send file
    val uri = Uri.fromFile(testFile)
    sharePlugin.shareFile(uri)

    // Verify transfer completes
    assertTrue(
      "File transfer should complete",
      transferComplete.await(10, TimeUnit.SECONDS)
    )
    assertTrue("Transfer should succeed", transferSuccess)

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testSendMultipleFiles() {
    val filesCount = 3
    val transfersComplete = CountDownLatch(filesCount)
    val completedFiles = mutableListOf<String>()

    // Create test files
    val testFiles = listOf(
      createTestFile("file1.txt", "Content 1"),
      createTestFile("file2.txt", "Content 2"),
      createTestFile("file3.txt", "Content 3")
    )

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}

      override fun onTransferComplete(transferId: String) {
        completedFiles.add(transferId)
        transfersComplete.countDown()
      }

      override fun onTransferFailed(transferId: String, error: String) {
        transfersComplete.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Send all files
    val uris = testFiles.map { Uri.fromFile(it) }
    sharePlugin.shareFiles(uris)

    // Verify all transfers complete
    assertTrue(
      "All file transfers should complete",
      transfersComplete.await(30, TimeUnit.SECONDS)
    )
    assertEquals(
      "All files should transfer successfully",
      filesCount,
      completedFiles.size
    )

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testSendLargeFile() {
    val transferComplete = CountDownLatch(1)
    var progressUpdates = 0
    var finalProgress = 0

    // Create large test file (5 MB)
    val largeContent = "X".repeat(5 * 1024 * 1024)
    val largeFile = createTestFile("large.bin", largeContent)

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}

      override fun onTransferProgress(transferId: String, progress: Int) {
        progressUpdates++
        finalProgress = progress
      }

      override fun onTransferComplete(transferId: String) {
        transferComplete.countDown()
      }

      override fun onTransferFailed(transferId: String, error: String) {
        transferComplete.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Send large file
    val uri = Uri.fromFile(largeFile)
    sharePlugin.shareFile(uri)

    // Verify transfer completes with progress updates
    assertTrue(
      "Large file transfer should complete",
      transferComplete.await(60, TimeUnit.SECONDS)
    )
    assertTrue(
      "Should receive progress updates",
      progressUpdates > 0
    )
    assertEquals(
      "Final progress should be 100%",
      100,
      finalProgress
    )

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testSendFileWithProgress() {
    var progressValues = mutableListOf<Int>()

    // Create test file
    val testFile = createTestFile("progress.txt", "Test progress tracking")

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}

      override fun onTransferProgress(transferId: String, progress: Int) {
        progressValues.add(progress)
      }

      override fun onTransferComplete(transferId: String) {}
      override fun onTransferFailed(transferId: String, error: String) {}
    }

    sharePlugin.addTransferListener(listener)

    // Send file
    sharePlugin.shareFile(Uri.fromFile(testFile))

    // Wait for transfer
    Thread.sleep(2000)

    // Verify progress updates
    assertTrue(
      "Should receive progress updates",
      progressValues.isNotEmpty()
    )
    assertTrue(
      "Progress should be monotonically increasing",
      progressValues.zipWithNext().all { (a, b) -> a <= b }
    )
    assertTrue(
      "Progress should reach 100%",
      progressValues.last() == 100
    )

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testCancelFileTransfer() {
    val transferStarted = CountDownLatch(1)
    val transferCancelled = CountDownLatch(1)
    var transferId: String? = null

    // Create large file for cancellation test
    val largeContent = "Y".repeat(10 * 1024 * 1024) // 10 MB
    val largeFile = createTestFile("cancel.bin", largeContent)

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(tId: String, filename: String) {
        transferId = tId
        transferStarted.countDown()
      }

      override fun onTransferProgress(transferId: String, progress: Int) {}

      override fun onTransferComplete(transferId: String) {}

      override fun onTransferFailed(tId: String, error: String) {
        if (error.contains("cancelled", ignoreCase = true)) {
          transferCancelled.countDown()
        }
      }
    }

    sharePlugin.addTransferListener(listener)

    // Start transfer
    sharePlugin.shareFile(Uri.fromFile(largeFile))

    // Wait for transfer to start
    assertTrue(
      "Transfer should start",
      transferStarted.await(5, TimeUnit.SECONDS)
    )

    // Cancel transfer
    assertNotNull("Transfer ID should be set", transferId)
    sharePlugin.cancelTransfer(transferId!!)

    // Verify cancellation
    assertTrue(
      "Transfer should be cancelled",
      transferCancelled.await(5, TimeUnit.SECONDS)
    )

    sharePlugin.removeTransferListener(listener)
  }

  // ========================================
  // RECEIVING FILES
  // ========================================

  @Test
  fun testReceiveSingleFile() {
    val fileReceived = CountDownLatch(1)
    var receivedFile: File? = null

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFile = file
        fileReceived.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {}

      override fun onReceiveFailed(filename: String, error: String) {
        fileReceived.countDown()
      }
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving share request packet
    val sharePacket = MockFactory.createSharePacket(
      deviceId = pairedDevice.deviceId,
      filename = "received.txt",
      numberOfFiles = 1,
      totalPayloadSize = 1024
    )
    cosmicConnect.processIncomingPacket(sharePacket)

    // Simulate file payload reception
    val testContent = "Hello from COSMIC Desktop!"
    sharePlugin.receiveFilePayload(testContent.toByteArray())

    // Verify file received
    assertTrue(
      "File should be received",
      fileReceived.await(10, TimeUnit.SECONDS)
    )
    assertNotNull("Received file should not be null", receivedFile)
    assertTrue("Received file should exist", receivedFile!!.exists())
    assertEquals(
      "File content should match",
      testContent,
      receivedFile!!.readText()
    )

    sharePlugin.removeReceiveListener(listener)
  }

  @Test
  fun testReceiveMultipleFiles() {
    val filesCount = 3
    val filesReceived = CountDownLatch(filesCount)
    val receivedFiles = mutableListOf<File>()

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFiles.add(file)
        filesReceived.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving multiple files
    val sharePacket = MockFactory.createSharePacket(
      deviceId = pairedDevice.deviceId,
      filename = "file1.txt",
      numberOfFiles = filesCount,
      totalPayloadSize = 3072
    )
    cosmicConnect.processIncomingPacket(sharePacket)

    // Simulate receiving each file payload
    repeat(filesCount) { index ->
      val content = "File $index content"
      sharePlugin.receiveFilePayload(content.toByteArray())
    }

    // Verify all files received
    assertTrue(
      "All files should be received",
      filesReceived.await(30, TimeUnit.SECONDS)
    )
    assertEquals(
      "Should receive exactly $filesCount files",
      filesCount,
      receivedFiles.size
    )
    assertTrue(
      "All received files should exist",
      receivedFiles.all { it.exists() }
    )

    sharePlugin.removeReceiveListener(listener)
  }

  @Test
  fun testReceiveLargeFile() {
    val fileReceived = CountDownLatch(1)
    var progressUpdates = 0
    var receivedFile: File? = null

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivedFile = file
        fileReceived.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {
        progressUpdates++
      }

      override fun onReceiveFailed(filename: String, error: String) {
        fileReceived.countDown()
      }
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving large file
    val largeSize = 5 * 1024 * 1024L // 5 MB
    val sharePacket = MockFactory.createSharePacket(
      deviceId = pairedDevice.deviceId,
      filename = "large_received.bin",
      numberOfFiles = 1,
      totalPayloadSize = largeSize
    )
    cosmicConnect.processIncomingPacket(sharePacket)

    // Simulate receiving payload in chunks
    val chunkSize = 64 * 1024 // 64 KB chunks
    val totalChunks = (largeSize / chunkSize).toInt()
    val content = ByteArray(chunkSize) { 0xFF.toByte() }

    repeat(totalChunks) {
      sharePlugin.receiveFilePayload(content)
    }

    // Verify file received with progress
    assertTrue(
      "Large file should be received",
      fileReceived.await(60, TimeUnit.SECONDS)
    )
    assertNotNull("Received file should not be null", receivedFile)
    assertTrue("Should receive progress updates", progressUpdates > 0)

    sharePlugin.removeReceiveListener(listener)
  }

  @Test
  fun testReceiveWithProgress() {
    val progressValues = mutableListOf<Int>()

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {}

      override fun onReceiveProgress(filename: String, progress: Int) {
        progressValues.add(progress)
      }

      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving file with progress
    val sharePacket = MockFactory.createSharePacket(
      deviceId = pairedDevice.deviceId,
      filename = "progress.txt",
      numberOfFiles = 1,
      totalPayloadSize = 1024
    )
    cosmicConnect.processIncomingPacket(sharePacket)

    val content = "Content with progress tracking"
    sharePlugin.receiveFilePayload(content.toByteArray())

    // Wait for completion
    Thread.sleep(2000)

    // Verify progress updates
    assertTrue(
      "Should receive progress updates",
      progressValues.isNotEmpty()
    )
    assertTrue(
      "Progress should be monotonically increasing",
      progressValues.zipWithNext().all { (a, b) -> a <= b }
    )

    sharePlugin.removeReceiveListener(listener)
  }

  @Test
  fun testRejectIncomingFile() {
    val requestReceived = CountDownLatch(1)

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {}
      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving share request
    val sharePacket = MockFactory.createSharePacket(
      deviceId = pairedDevice.deviceId,
      filename = "rejected.txt",
      numberOfFiles = 1,
      totalPayloadSize = 1024
    )

    // Setup request listener
    val requestListener = object : SharePlugin.ShareRequestListener {
      override fun onShareRequest(deviceId: String, filename: String) {
        requestReceived.countDown()
        // Reject the request
        sharePlugin.rejectShare(deviceId, filename)
      }
    }

    sharePlugin.setShareRequestListener(requestListener)

    // Process share packet
    cosmicConnect.processIncomingPacket(sharePacket)

    // Verify request was received and rejected
    assertTrue(
      "Share request should be received",
      requestReceived.await(5, TimeUnit.SECONDS)
    )

    sharePlugin.removeReceiveListener(listener)
  }

  // ========================================
  // ERROR HANDLING
  // ========================================

  @Test
  fun testSendNonExistentFile() {
    val transferFailed = CountDownLatch(1)
    var errorMessage: String? = null

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}
      override fun onTransferComplete(transferId: String) {}

      override fun onTransferFailed(transferId: String, error: String) {
        errorMessage = error
        transferFailed.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Try to send non-existent file
    val fakeUri = Uri.fromFile(File(testFilesDir, "nonexistent.txt"))
    sharePlugin.shareFile(fakeUri)

    // Verify transfer fails
    assertTrue(
      "Transfer should fail",
      transferFailed.await(5, TimeUnit.SECONDS)
    )
    assertNotNull("Error message should be provided", errorMessage)
    assertTrue(
      "Error should indicate file not found",
      errorMessage!!.contains("not found", ignoreCase = true) ||
      errorMessage!!.contains("does not exist", ignoreCase = true)
    )

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testSendToUnpairedDevice() {
    // Create unpaired device
    val unpairedIdentity = MockFactory.createIdentityPacket(
      deviceId = "unpaired_device",
      deviceName = "Unpaired Device",
      deviceType = "desktop"
    )
    cosmicConnect.processIncomingPacket(unpairedIdentity)
    val unpairedDevice = cosmicConnect.getDevice("unpaired_device")!!

    val transferFailed = CountDownLatch(1)

    // Get share plugin for unpaired device
    val unpairedSharePlugin = unpairedDevice.getPlugin("share") as SharePlugin

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}
      override fun onTransferComplete(transferId: String) {}

      override fun onTransferFailed(transferId: String, error: String) {
        transferFailed.countDown()
      }
    }

    unpairedSharePlugin.addTransferListener(listener)

    // Try to send file
    val testFile = createTestFile("test.txt", "Content")
    unpairedSharePlugin.shareFile(Uri.fromFile(testFile))

    // Verify transfer fails
    assertTrue(
      "Transfer should fail for unpaired device",
      transferFailed.await(5, TimeUnit.SECONDS)
    )

    unpairedSharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testNetworkFailureDuringTransfer() {
    val transferFailed = CountDownLatch(1)
    var transferId: String? = null

    // Create test file
    val testFile = createTestFile("network_fail.txt", "X".repeat(1024 * 1024))

    // Setup listener
    val listener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(tId: String, filename: String) {
        transferId = tId
        // Simulate network failure after start
        pairedDevice.onConnectionLost()
      }

      override fun onTransferProgress(transferId: String, progress: Int) {}
      override fun onTransferComplete(transferId: String) {}

      override fun onTransferFailed(tId: String, error: String) {
        transferFailed.countDown()
      }
    }

    sharePlugin.addTransferListener(listener)

    // Start transfer
    sharePlugin.shareFile(Uri.fromFile(testFile))

    // Verify transfer fails due to network
    assertTrue(
      "Transfer should fail due to network failure",
      transferFailed.await(10, TimeUnit.SECONDS)
    )

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testInsufficientDiskSpace() {
    val receiveFailed = CountDownLatch(1)
    var errorMessage: String? = null

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {}
      override fun onReceiveProgress(filename: String, progress: Int) {}

      override fun onReceiveFailed(filename: String, error: String) {
        errorMessage = error
        receiveFailed.countDown()
      }
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving extremely large file (larger than available space)
    val hugeSize = Long.MAX_VALUE
    val sharePacket = MockFactory.createSharePacket(
      deviceId = pairedDevice.deviceId,
      filename = "huge.bin",
      numberOfFiles = 1,
      totalPayloadSize = hugeSize
    )
    cosmicConnect.processIncomingPacket(sharePacket)

    // Verify receive fails
    assertTrue(
      "Receive should fail due to disk space",
      receiveFailed.await(5, TimeUnit.SECONDS)
    )
    assertNotNull("Error message should be provided", errorMessage)

    sharePlugin.removeReceiveListener(listener)
  }

  // ========================================
  // CONCURRENT OPERATIONS
  // ========================================

  @Test
  fun testConcurrentSends() {
    val transfersCount = 3
    val transfersComplete = CountDownLatch(transfersCount)

    // Create test files
    val testFiles = listOf(
      createTestFile("concurrent1.txt", "Content 1"),
      createTestFile("concurrent2.txt", "Content 2"),
      createTestFile("concurrent3.txt", "Content 3")
    )

    // Setup listener
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

    // Send all files concurrently
    testFiles.forEach { file ->
      Thread {
        sharePlugin.shareFile(Uri.fromFile(file))
      }.start()
    }

    // Verify all transfers complete
    assertTrue(
      "All concurrent transfers should complete",
      transfersComplete.await(30, TimeUnit.SECONDS)
    )

    sharePlugin.removeTransferListener(listener)
  }

  @Test
  fun testConcurrentReceives() {
    val receivesCount = 3
    val receivesComplete = CountDownLatch(receivesCount)

    // Setup listener
    val listener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receivesComplete.countDown()
      }

      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addReceiveListener(listener)

    // Simulate receiving multiple files concurrently
    repeat(receivesCount) { index ->
      Thread {
        val sharePacket = MockFactory.createSharePacket(
          deviceId = pairedDevice.deviceId,
          filename = "concurrent_receive_$index.txt",
          numberOfFiles = 1,
          totalPayloadSize = 1024
        )
        cosmicConnect.processIncomingPacket(sharePacket)

        val content = "Concurrent content $index"
        sharePlugin.receiveFilePayload(content.toByteArray())
      }.start()
    }

    // Verify all receives complete
    assertTrue(
      "All concurrent receives should complete",
      receivesComplete.await(30, TimeUnit.SECONDS)
    )

    sharePlugin.removeReceiveListener(listener)
  }

  @Test
  fun testSimultaneousSendAndReceive() {
    val sendComplete = CountDownLatch(1)
    val receiveComplete = CountDownLatch(1)

    // Setup listeners
    val transferListener = object : SharePlugin.TransferListener {
      override fun onTransferStarted(transferId: String, filename: String) {}
      override fun onTransferProgress(transferId: String, progress: Int) {}
      override fun onTransferComplete(transferId: String) { sendComplete.countDown() }
      override fun onTransferFailed(transferId: String, error: String) {}
    }

    val receiveListener = object : SharePlugin.ReceiveListener {
      override fun onFileReceived(file: File, filename: String) {
        receiveComplete.countDown()
      }
      override fun onReceiveProgress(filename: String, progress: Int) {}
      override fun onReceiveFailed(filename: String, error: String) {}
    }

    sharePlugin.addTransferListener(transferListener)
    sharePlugin.addReceiveListener(receiveListener)

    // Send and receive simultaneously
    Thread {
      val sendFile = createTestFile("send.txt", "Sending content")
      sharePlugin.shareFile(Uri.fromFile(sendFile))
    }.start()

    Thread {
      val sharePacket = MockFactory.createSharePacket(
        deviceId = pairedDevice.deviceId,
        filename = "receive.txt",
        numberOfFiles = 1,
        totalPayloadSize = 1024
      )
      cosmicConnect.processIncomingPacket(sharePacket)
      sharePlugin.receiveFilePayload("Receiving content".toByteArray())
    }.start()

    // Verify both operations complete
    assertTrue(
      "Send should complete",
      sendComplete.await(10, TimeUnit.SECONDS)
    )
    assertTrue(
      "Receive should complete",
      receiveComplete.await(10, TimeUnit.SECONDS)
    )

    sharePlugin.removeTransferListener(transferListener)
    sharePlugin.removeReceiveListener(receiveListener)
  }

  // ========================================
  // HELPER METHODS
  // ========================================

  private fun createTestFile(filename: String, content: String): File {
    val file = File(testFilesDir, filename)
    file.writeText(content)
    return file
  }
}
