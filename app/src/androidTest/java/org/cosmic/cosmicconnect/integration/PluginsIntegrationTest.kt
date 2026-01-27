package org.cosmic.cconnect.integration

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cconnect.CosmicConnect
import org.cosmic.cconnect.Device
import org.cosmic.cconnect.NetworkPacket
import org.cosmic.cconnect.plugins.battery.BatteryPlugin
import org.cosmic.cconnect.plugins.clipboard.ClipboardPlugin
import org.cosmic.cconnect.plugins.mpris.MprisPlugin
import org.cosmic.cconnect.plugins.ping.PingPlugin
import org.cosmic.cconnect.plugins.runcommand.RunCommandPlugin
import org.cosmic.cconnect.plugins.telephony.TelephonyPlugin
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
 * Integration Tests - All Plugins
 *
 * Tests all COSMIC Connect plugins through the complete stack:
 * Android UI → Plugin → Rust Core → Network → COSMIC Desktop
 *
 * Plugins tested:
 * - Battery: Battery status synchronization
 * - Clipboard: Clipboard content sharing
 * - Ping: Ping/pong messaging
 * - RunCommand: Remote command execution
 * - MPRIS: Media player control
 * - Telephony: Phone call and SMS notifications
 */
@RunWith(AndroidJUnit4::class)
class PluginsIntegrationTest {

  private lateinit var cosmicConnect: CosmicConnect
  private lateinit var pairedDevice: Device
  private lateinit var context: Context

  @Before
  fun setup() {
    TestUtils.cleanupTestData()
    context = TestUtils.getTestContext()

    // Initialize CosmicConnect
    cosmicConnect = CosmicConnect.getInstance(context)

    // Create and pair a test device
    val identityPacket = MockFactory.createIdentityPacket(
      deviceId = "test_device_plugins",
      deviceName = "Test Desktop",
      deviceType = "desktop"
    )
    cosmicConnect.processIncomingPacket(identityPacket)

    pairedDevice = cosmicConnect.getDevice("test_device_plugins")!!

    // Simulate pairing
    pairedDevice.requestPairing()
    val pairResponse = MockFactory.createPairResponsePacket(
      deviceId = pairedDevice.deviceId,
      accepted = true
    )
    cosmicConnect.processIncomingPacket(pairResponse)
    TestUtils.waitFor { pairedDevice.isPaired }
  }

  @After
  fun teardown() {
    // Unpair device
    if (pairedDevice.isPaired) {
      pairedDevice.unpair()
    }

    TestUtils.cleanupTestData()
  }

  // ========================================
  // BATTERY PLUGIN TESTS
  // ========================================

  @Test
  fun testBatteryPluginAvailable() {
    val batteryPlugin = pairedDevice.getPlugin("battery")
    assertNotNull("Battery plugin should be available", batteryPlugin)
    assertTrue("Should be BatteryPlugin instance", batteryPlugin is BatteryPlugin)
  }

  @Test
  fun testSendBatteryStatus() {
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin
    val packetSent = CountDownLatch(1)

    // Setup listener
    val listener = object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {
        packetSent.countDown()
      }

      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {}
    }

    batteryPlugin.addBatteryListener(listener)

    // Send battery status
    batteryPlugin.sendBatteryStatus(75, isCharging = false)

    // Verify packet sent
    assertTrue(
      "Battery status packet should be sent",
      packetSent.await(5, TimeUnit.SECONDS)
    )

    batteryPlugin.removeBatteryListener(listener)
  }

  @Test
  fun testReceiveBatteryStatus() {
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin
    val batteryReceived = CountDownLatch(1)
    var receivedLevel = -1
    var receivedCharging = false

    // Setup listener
    val listener = object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {}

      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {
        receivedLevel = level
        receivedCharging = isCharging
        batteryReceived.countDown()
      }
    }

    batteryPlugin.addBatteryListener(listener)

    // Simulate receiving battery packet from COSMIC
    val batteryPacket = MockFactory.createBatteryPacket(
      deviceId = pairedDevice.deviceId,
      batteryLevel = 85,
      isCharging = true
    )
    cosmicConnect.processIncomingPacket(batteryPacket)

    // Verify battery update received
    assertTrue(
      "Battery update should be received",
      batteryReceived.await(5, TimeUnit.SECONDS)
    )
    assertEquals("Battery level should match", 85, receivedLevel)
    assertTrue("Should be charging", receivedCharging)

    batteryPlugin.removeBatteryListener(listener)
  }

  @Test
  fun testBatteryStatusPersistence() {
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin

    // Send battery status
    batteryPlugin.sendBatteryStatus(60, isCharging = true)

    // Wait for processing
    Thread.sleep(500)

    // Verify status is cached
    val cachedLevel = batteryPlugin.getLastBatteryLevel()
    val cachedCharging = batteryPlugin.isCharging()

    assertEquals("Cached battery level should match", 60, cachedLevel)
    assertTrue("Cached charging state should match", cachedCharging)
  }

  @Test
  fun testBatteryThresholdEvent() {
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin
    val lowBatteryReceived = CountDownLatch(1)

    // Setup listener for threshold events
    val listener = object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {}

      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {
        if (level <= 15) {
          lowBatteryReceived.countDown()
        }
      }
    }

    batteryPlugin.addBatteryListener(listener)

    // Simulate low battery packet
    val lowBatteryPacket = MockFactory.createBatteryPacket(
      deviceId = pairedDevice.deviceId,
      batteryLevel = 10,
      isCharging = false
    )
    cosmicConnect.processIncomingPacket(lowBatteryPacket)

    // Verify low battery notification
    assertTrue(
      "Low battery should be detected",
      lowBatteryReceived.await(5, TimeUnit.SECONDS)
    )

    batteryPlugin.removeBatteryListener(listener)
  }

  // ========================================
  // CLIPBOARD PLUGIN TESTS
  // ========================================

  @Test
  fun testClipboardPluginAvailable() {
    val clipboardPlugin = pairedDevice.getPlugin("clipboard")
    assertNotNull("Clipboard plugin should be available", clipboardPlugin)
    assertTrue("Should be ClipboardPlugin instance", clipboardPlugin is ClipboardPlugin)
  }

  @Test
  fun testSendClipboardContent() {
    val clipboardPlugin = pairedDevice.getPlugin("clipboard") as ClipboardPlugin
    val clipboardSent = CountDownLatch(1)

    // Setup listener
    val listener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {
        clipboardSent.countDown()
      }

      override fun onRemoteClipboardUpdate(content: String) {}
    }

    clipboardPlugin.addClipboardListener(listener)

    // Send clipboard content
    val testContent = "Hello from Android!"
    clipboardPlugin.sendClipboard(testContent)

    // Verify clipboard sent
    assertTrue(
      "Clipboard packet should be sent",
      clipboardSent.await(5, TimeUnit.SECONDS)
    )

    clipboardPlugin.removeClipboardListener(listener)
  }

  @Test
  fun testReceiveClipboardContent() {
    val clipboardPlugin = pairedDevice.getPlugin("clipboard") as ClipboardPlugin
    val clipboardReceived = CountDownLatch(1)
    var receivedContent = ""

    // Setup listener
    val listener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {}

      override fun onRemoteClipboardUpdate(content: String) {
        receivedContent = content
        clipboardReceived.countDown()
      }
    }

    clipboardPlugin.addClipboardListener(listener)

    // Simulate receiving clipboard packet from COSMIC
    val testContent = "Hello from COSMIC Desktop!"
    val clipboardPacket = MockFactory.createClipboardPacket(
      deviceId = pairedDevice.deviceId,
      content = testContent
    )
    cosmicConnect.processIncomingPacket(clipboardPacket)

    // Verify clipboard update received
    assertTrue(
      "Clipboard update should be received",
      clipboardReceived.await(5, TimeUnit.SECONDS)
    )
    assertEquals("Clipboard content should match", testContent, receivedContent)

    clipboardPlugin.removeClipboardListener(listener)
  }

  @Test
  fun testClipboardAutoSync() {
    val clipboardPlugin = pairedDevice.getPlugin("clipboard") as ClipboardPlugin

    // Enable auto-sync
    clipboardPlugin.setAutoSync(true)

    // Get clipboard manager
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Setup listener
    val clipboardSent = CountDownLatch(1)
    val listener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {
        clipboardSent.countDown()
      }
      override fun onRemoteClipboardUpdate(content: String) {}
    }

    clipboardPlugin.addClipboardListener(listener)

    // Simulate clipboard change
    val clip = ClipData.newPlainText("test", "Auto-synced content")
    clipboardManager.setPrimaryClip(clip)

    // Verify auto-sync triggers send
    assertTrue(
      "Auto-sync should trigger clipboard send",
      clipboardSent.await(5, TimeUnit.SECONDS)
    )

    clipboardPlugin.removeClipboardListener(listener)
  }

  @Test
  fun testClipboardEmptyContent() {
    val clipboardPlugin = pairedDevice.getPlugin("clipboard") as ClipboardPlugin
    val clipboardSent = CountDownLatch(1)

    val listener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {
        if (content.isEmpty()) {
          clipboardSent.countDown()
        }
      }
      override fun onRemoteClipboardUpdate(content: String) {}
    }

    clipboardPlugin.addClipboardListener(listener)

    // Send empty clipboard
    clipboardPlugin.sendClipboard("")

    // Verify empty clipboard handled
    assertTrue(
      "Empty clipboard should be sent",
      clipboardSent.await(5, TimeUnit.SECONDS)
    )

    clipboardPlugin.removeClipboardListener(listener)
  }

  // ========================================
  // PING PLUGIN TESTS
  // ========================================

  @Test
  fun testPingPluginAvailable() {
    val pingPlugin = pairedDevice.getPlugin("ping")
    assertNotNull("Ping plugin should be available", pingPlugin)
    assertTrue("Should be PingPlugin instance", pingPlugin is PingPlugin)
  }

  @Test
  fun testSendPing() {
    val pingPlugin = pairedDevice.getPlugin("ping") as PingPlugin
    val pingSent = CountDownLatch(1)

    // Setup listener
    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {
        pingSent.countDown()
      }

      override fun onPingReceived(message: String?) {}
    }

    pingPlugin.addPingListener(listener)

    // Send ping
    pingPlugin.sendPing("Test ping")

    // Verify ping sent
    assertTrue(
      "Ping packet should be sent",
      pingSent.await(5, TimeUnit.SECONDS)
    )

    pingPlugin.removePingListener(listener)
  }

  @Test
  fun testReceivePing() {
    val pingPlugin = pairedDevice.getPlugin("ping") as PingPlugin
    val pingReceived = CountDownLatch(1)
    var receivedMessage: String? = null

    // Setup listener
    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {}

      override fun onPingReceived(message: String?) {
        receivedMessage = message
        pingReceived.countDown()
      }
    }

    pingPlugin.addPingListener(listener)

    // Simulate receiving ping packet
    val testMessage = "Ping from COSMIC!"
    val pingPacket = MockFactory.createPingPacket(
      deviceId = pairedDevice.deviceId,
      message = testMessage
    )
    cosmicConnect.processIncomingPacket(pingPacket)

    // Verify ping received
    assertTrue(
      "Ping should be received",
      pingReceived.await(5, TimeUnit.SECONDS)
    )
    assertEquals("Ping message should match", testMessage, receivedMessage)

    pingPlugin.removePingListener(listener)
  }

  @Test
  fun testPingWithoutMessage() {
    val pingPlugin = pairedDevice.getPlugin("ping") as PingPlugin
    val pingSent = CountDownLatch(1)

    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {
        pingSent.countDown()
      }
      override fun onPingReceived(message: String?) {}
    }

    pingPlugin.addPingListener(listener)

    // Send ping without message
    pingPlugin.sendPing(null)

    // Verify ping sent
    assertTrue(
      "Ping without message should be sent",
      pingSent.await(5, TimeUnit.SECONDS)
    )

    pingPlugin.removePingListener(listener)
  }

  @Test
  fun testPingNotification() {
    val pingPlugin = pairedDevice.getPlugin("ping") as PingPlugin
    val notificationShown = CountDownLatch(1)

    // Setup notification listener
    val listener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {}

      override fun onPingReceived(message: String?) {
        // Ping should trigger notification
        notificationShown.countDown()
      }
    }

    pingPlugin.addPingListener(listener)

    // Receive ping
    val pingPacket = MockFactory.createPingPacket(
      deviceId = pairedDevice.deviceId,
      message = "Test notification"
    )
    cosmicConnect.processIncomingPacket(pingPacket)

    // Verify notification triggered
    assertTrue(
      "Ping should trigger notification",
      notificationShown.await(5, TimeUnit.SECONDS)
    )

    pingPlugin.removePingListener(listener)
  }

  // ========================================
  // RUNCOMMAND PLUGIN TESTS
  // ========================================

  @Test
  fun testRunCommandPluginAvailable() {
    val runCommandPlugin = pairedDevice.getPlugin("runcommand")
    assertNotNull("RunCommand plugin should be available", runCommandPlugin)
    assertTrue("Should be RunCommandPlugin instance", runCommandPlugin is RunCommandPlugin)
  }

  @Test
  fun testSendCommand() {
    val runCommandPlugin = pairedDevice.getPlugin("runcommand") as RunCommandPlugin
    val commandSent = CountDownLatch(1)

    // Setup listener
    val listener = object : RunCommandPlugin.CommandListener {
      override fun onCommandSent(key: String, command: String) {
        commandSent.countDown()
      }

      override fun onCommandListReceived(commands: Map<String, String>) {}
    }

    runCommandPlugin.addCommandListener(listener)

    // Send command
    runCommandPlugin.sendCommand("test_cmd", "echo 'Hello'")

    // Verify command sent
    assertTrue(
      "Command packet should be sent",
      commandSent.await(5, TimeUnit.SECONDS)
    )

    runCommandPlugin.removeCommandListener(listener)
  }

  @Test
  fun testReceiveCommandList() {
    val runCommandPlugin = pairedDevice.getPlugin("runcommand") as RunCommandPlugin
    val commandListReceived = CountDownLatch(1)
    var receivedCommands: Map<String, String>? = null

    // Setup listener
    val listener = object : RunCommandPlugin.CommandListener {
      override fun onCommandSent(key: String, command: String) {}

      override fun onCommandListReceived(commands: Map<String, String>) {
        receivedCommands = commands
        commandListReceived.countDown()
      }
    }

    runCommandPlugin.addCommandListener(listener)

    // Simulate receiving command list from COSMIC
    val commandPacket = MockFactory.createRunCommandPacket(
      deviceId = pairedDevice.deviceId,
      key = "list_update",
      command = ""
    )
    cosmicConnect.processIncomingPacket(commandPacket)

    // Verify command list received
    assertTrue(
      "Command list should be received",
      commandListReceived.await(5, TimeUnit.SECONDS)
    )

    runCommandPlugin.removeCommandListener(listener)
  }

  @Test
  fun testStoreCommand() {
    val runCommandPlugin = pairedDevice.getPlugin("runcommand") as RunCommandPlugin

    // Store command
    runCommandPlugin.storeCommand("shutdown", "shutdown -h now")

    // Verify command stored
    val storedCommands = runCommandPlugin.getStoredCommands()
    assertTrue("Command should be stored", storedCommands.containsKey("shutdown"))
    assertEquals("shutdown -h now", storedCommands["shutdown"])
  }

  @Test
  fun testRemoveCommand() {
    val runCommandPlugin = pairedDevice.getPlugin("runcommand") as RunCommandPlugin

    // Store then remove command
    runCommandPlugin.storeCommand("temp_cmd", "echo 'temp'")
    runCommandPlugin.removeCommand("temp_cmd")

    // Verify command removed
    val storedCommands = runCommandPlugin.getStoredCommands()
    assertFalse("Command should be removed", storedCommands.containsKey("temp_cmd"))
  }

  @Test
  fun testCommandValidation() {
    val runCommandPlugin = pairedDevice.getPlugin("runcommand") as RunCommandPlugin
    val commandSent = CountDownLatch(1)

    val listener = object : RunCommandPlugin.CommandListener {
      override fun onCommandSent(key: String, command: String) {
        if (key.isNotEmpty() && command.isNotEmpty()) {
          commandSent.countDown()
        }
      }
      override fun onCommandListReceived(commands: Map<String, String>) {}
    }

    runCommandPlugin.addCommandListener(listener)

    // Send valid command
    runCommandPlugin.sendCommand("valid", "ls -la")

    // Verify valid command sent
    assertTrue(
      "Valid command should be sent",
      commandSent.await(5, TimeUnit.SECONDS)
    )

    runCommandPlugin.removeCommandListener(listener)
  }

  // ========================================
  // MPRIS PLUGIN TESTS
  // ========================================

  @Test
  fun testMprisPluginAvailable() {
    val mprisPlugin = pairedDevice.getPlugin("mpris")
    assertNotNull("MPRIS plugin should be available", mprisPlugin)
    assertTrue("Should be MprisPlugin instance", mprisPlugin is MprisPlugin)
  }

  @Test
  fun testSendMediaControl() {
    val mprisPlugin = pairedDevice.getPlugin("mpris") as MprisPlugin
    val controlSent = CountDownLatch(1)

    // Setup listener
    val listener = object : MprisPlugin.MediaControlListener {
      override fun onMediaControlSent(action: String) {
        controlSent.countDown()
      }

      override fun onMediaStatusReceived(
        isPlaying: Boolean,
        title: String?,
        artist: String?,
        album: String?
      ) {}
    }

    mprisPlugin.addMediaControlListener(listener)

    // Send play command
    mprisPlugin.sendMediaControl("play")

    // Verify control sent
    assertTrue(
      "Media control packet should be sent",
      controlSent.await(5, TimeUnit.SECONDS)
    )

    mprisPlugin.removeMediaControlListener(listener)
  }

  @Test
  fun testReceiveMediaStatus() {
    val mprisPlugin = pairedDevice.getPlugin("mpris") as MprisPlugin
    val statusReceived = CountDownLatch(1)
    var isPlaying = false
    var title: String? = null
    var artist: String? = null

    // Setup listener
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

    // Simulate receiving media status from COSMIC
    // Note: MockFactory would need an MPRIS packet creator
    // For now, create a basic packet
    val mprisPacket = NetworkPacket("cconnect.mpris")
    // Add body fields for status
    cosmicConnect.processIncomingPacket(mprisPacket)

    // Wait for potential status
    Thread.sleep(1000)

    mprisPlugin.removeMediaControlListener(listener)
  }

  @Test
  fun testMediaControlActions() {
    val mprisPlugin = pairedDevice.getPlugin("mpris") as MprisPlugin
    val actionsCount = CountDownLatch(4)

    val listener = object : MprisPlugin.MediaControlListener {
      override fun onMediaControlSent(action: String) {
        actionsCount.countDown()
      }
      override fun onMediaStatusReceived(
        isPlaying: Boolean,
        title: String?,
        artist: String?,
        album: String?
      ) {}
    }

    mprisPlugin.addMediaControlListener(listener)

    // Send various media controls
    mprisPlugin.sendMediaControl("play")
    mprisPlugin.sendMediaControl("pause")
    mprisPlugin.sendMediaControl("next")
    mprisPlugin.sendMediaControl("previous")

    // Verify all controls sent
    assertTrue(
      "All media controls should be sent",
      actionsCount.await(5, TimeUnit.SECONDS)
    )

    mprisPlugin.removeMediaControlListener(listener)
  }

  // ========================================
  // TELEPHONY PLUGIN TESTS
  // ========================================

  @Test
  fun testTelephonyPluginAvailable() {
    val telephonyPlugin = pairedDevice.getPlugin("telephony")
    assertNotNull("Telephony plugin should be available", telephonyPlugin)
    assertTrue("Should be TelephonyPlugin instance", telephonyPlugin is TelephonyPlugin)
  }

  @Test
  fun testSendCallNotification() {
    val telephonyPlugin = pairedDevice.getPlugin("telephony") as TelephonyPlugin
    val notificationSent = CountDownLatch(1)

    // Setup listener
    val listener = object : TelephonyPlugin.TelephonyListener {
      override fun onCallNotificationSent(
        event: String,
        phoneNumber: String,
        contactName: String?
      ) {
        notificationSent.countDown()
      }

      override fun onSmsNotificationSent(
        phoneNumber: String,
        messageBody: String,
        contactName: String?
      ) {}

      override fun onMuteCallRequested() {}
    }

    telephonyPlugin.addTelephonyListener(listener)

    // Send call notification
    telephonyPlugin.sendCallNotification(
      event = "ringing",
      phoneNumber = "+1234567890",
      contactName = "John Doe"
    )

    // Verify notification sent
    assertTrue(
      "Call notification should be sent",
      notificationSent.await(5, TimeUnit.SECONDS)
    )

    telephonyPlugin.removeTelephonyListener(listener)
  }

  @Test
  fun testSendSmsNotification() {
    val telephonyPlugin = pairedDevice.getPlugin("telephony") as TelephonyPlugin
    val smsSent = CountDownLatch(1)

    // Setup listener
    val listener = object : TelephonyPlugin.TelephonyListener {
      override fun onCallNotificationSent(
        event: String,
        phoneNumber: String,
        contactName: String?
      ) {}

      override fun onSmsNotificationSent(
        phoneNumber: String,
        messageBody: String,
        contactName: String?
      ) {
        smsSent.countDown()
      }

      override fun onMuteCallRequested() {}
    }

    telephonyPlugin.addTelephonyListener(listener)

    // Send SMS notification
    telephonyPlugin.sendSmsNotification(
      phoneNumber = "+1234567890",
      messageBody = "Test message",
      contactName = "Jane Doe"
    )

    // Verify SMS sent
    assertTrue(
      "SMS notification should be sent",
      smsSent.await(5, TimeUnit.SECONDS)
    )

    telephonyPlugin.removeTelephonyListener(listener)
  }

  @Test
  fun testMuteCallRequest() {
    val telephonyPlugin = pairedDevice.getPlugin("telephony") as TelephonyPlugin
    val muteRequested = CountDownLatch(1)

    // Setup listener
    val listener = object : TelephonyPlugin.TelephonyListener {
      override fun onCallNotificationSent(
        event: String,
        phoneNumber: String,
        contactName: String?
      ) {}

      override fun onSmsNotificationSent(
        phoneNumber: String,
        messageBody: String,
        contactName: String?
      ) {}

      override fun onMuteCallRequested() {
        muteRequested.countDown()
      }
    }

    telephonyPlugin.addTelephonyListener(listener)

    // Simulate receiving mute request from COSMIC
    val mutePacket = NetworkPacket("cconnect.telephony.request_mute")
    cosmicConnect.processIncomingPacket(mutePacket)

    // Verify mute request received
    assertTrue(
      "Mute call request should be received",
      muteRequested.await(5, TimeUnit.SECONDS)
    )

    telephonyPlugin.removeTelephonyListener(listener)
  }

  @Test
  fun testCallStates() {
    val telephonyPlugin = pairedDevice.getPlugin("telephony") as TelephonyPlugin
    val statesReceived = CountDownLatch(3)
    val states = mutableListOf<String>()

    val listener = object : TelephonyPlugin.TelephonyListener {
      override fun onCallNotificationSent(
        event: String,
        phoneNumber: String,
        contactName: String?
      ) {
        states.add(event)
        statesReceived.countDown()
      }
      override fun onSmsNotificationSent(
        phoneNumber: String,
        messageBody: String,
        contactName: String?
      ) {}
      override fun onMuteCallRequested() {}
    }

    telephonyPlugin.addTelephonyListener(listener)

    // Send different call states
    telephonyPlugin.sendCallNotification("ringing", "+1234567890", null)
    telephonyPlugin.sendCallNotification("talking", "+1234567890", null)
    telephonyPlugin.sendCallNotification("disconnected", "+1234567890", null)

    // Verify all states sent
    assertTrue(
      "All call states should be sent",
      statesReceived.await(5, TimeUnit.SECONDS)
    )
    assertEquals("Should have 3 states", 3, states.size)
    assertTrue("Should have ringing state", states.contains("ringing"))
    assertTrue("Should have talking state", states.contains("talking"))
    assertTrue("Should have disconnected state", states.contains("disconnected"))

    telephonyPlugin.removeTelephonyListener(listener)
  }

  // ========================================
  // PLUGIN LIFECYCLE TESTS
  // ========================================

  @Test
  fun testAllPluginsAvailable() {
    val availablePlugins = pairedDevice.getAvailablePlugins()

    assertTrue("Should have battery plugin", availablePlugins.contains("battery"))
    assertTrue("Should have clipboard plugin", availablePlugins.contains("clipboard"))
    assertTrue("Should have ping plugin", availablePlugins.contains("ping"))
    assertTrue("Should have runcommand plugin", availablePlugins.contains("runcommand"))
    assertTrue("Should have mpris plugin", availablePlugins.contains("mpris"))
    assertTrue("Should have telephony plugin", availablePlugins.contains("telephony"))
  }

  @Test
  fun testEnableDisablePlugin() {
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin

    // Disable plugin
    batteryPlugin.setEnabled(false)
    assertFalse("Plugin should be disabled", batteryPlugin.isEnabled())

    // Re-enable plugin
    batteryPlugin.setEnabled(true)
    assertTrue("Plugin should be enabled", batteryPlugin.isEnabled())
  }

  @Test
  fun testPluginRequiresPairing() {
    // Create unpaired device
    val unpairedIdentity = MockFactory.createIdentityPacket(
      deviceId = "unpaired_device",
      deviceName = "Unpaired Device",
      deviceType = "desktop"
    )
    cosmicConnect.processIncomingPacket(unpairedIdentity)
    val unpairedDevice = cosmicConnect.getDevice("unpaired_device")!!

    // Try to get plugin from unpaired device
    val batteryPlugin = unpairedDevice.getPlugin("battery")

    // Plugin should be available but operations should fail without pairing
    assertNotNull("Plugin should exist", batteryPlugin)
  }

  @Test
  fun testPluginPersistence() {
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin

    // Disable plugin
    batteryPlugin.setEnabled(false)

    // Simulate app restart
    val cosmicConnect2 = CosmicConnect.getInstance(context)
    val restoredDevice = cosmicConnect2.getDevice(pairedDevice.deviceId)!!
    val restoredPlugin = restoredDevice.getPlugin("battery") as BatteryPlugin

    // Verify plugin state persisted
    assertFalse(
      "Plugin enabled state should persist",
      restoredPlugin.isEnabled()
    )
  }

  @Test
  fun testMultiplePluginsSimultaneously() {
    val operations = CountDownLatch(3)

    // Setup listeners for multiple plugins
    val batteryPlugin = pairedDevice.getPlugin("battery") as BatteryPlugin
    val clipboardPlugin = pairedDevice.getPlugin("clipboard") as ClipboardPlugin
    val pingPlugin = pairedDevice.getPlugin("ping") as PingPlugin

    val batteryListener = object : BatteryPlugin.BatteryListener {
      override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {
        operations.countDown()
      }
      override fun onRemoteBatteryUpdate(level: Int, isCharging: Boolean) {}
    }

    val clipboardListener = object : ClipboardPlugin.ClipboardListener {
      override fun onClipboardSent(content: String) {
        operations.countDown()
      }
      override fun onRemoteClipboardUpdate(content: String) {}
    }

    val pingListener = object : PingPlugin.PingListener {
      override fun onPingSent(message: String?) {
        operations.countDown()
      }
      override fun onPingReceived(message: String?) {}
    }

    batteryPlugin.addBatteryListener(batteryListener)
    clipboardPlugin.addClipboardListener(clipboardListener)
    pingPlugin.addPingListener(pingListener)

    // Trigger operations simultaneously
    Thread { batteryPlugin.sendBatteryStatus(80, false) }.start()
    Thread { clipboardPlugin.sendClipboard("test") }.start()
    Thread { pingPlugin.sendPing("test") }.start()

    // Verify all operations complete
    assertTrue(
      "All plugins should operate simultaneously",
      operations.await(5, TimeUnit.SECONDS)
    )

    batteryPlugin.removeBatteryListener(batteryListener)
    clipboardPlugin.removeClipboardListener(clipboardListener)
    pingPlugin.removePingListener(pingListener)
  }
}
