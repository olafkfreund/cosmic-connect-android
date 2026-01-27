package org.cosmic.cconnect.test

import org.cosmic.cconnect.Device
import org.cosmic.cconnect.NetworkPacket

/**
 * Mock Factory
 *
 * Factory for creating mock objects for testing.
 * Creates realistic test data for devices, network packets, and FFI interactions.
 */
object MockFactory {

  /**
   * Create a mock NetworkPacket via FFI.
   *
   * @param type Packet type
   * @param deviceId Device ID
   * @param body Optional packet body
   * @return Mock NetworkPacket
   */
  fun createMockPacket(
    type: String,
    deviceId: String = TestUtils.randomDeviceId(),
    body: Map<String, Any> = emptyMap()
  ): NetworkPacket {
    // This would call into Rust FFI to create a proper NetworkPacket
    // For now, create via constructor if available
    // TODO: Use FFI to create NetworkPacket from Rust core
    return NetworkPacket(type)
  }

  /**
   * Create a mock identity packet.
   */
  fun createIdentityPacket(
    deviceId: String = TestUtils.randomDeviceId(),
    deviceName: String = TestUtils.randomTestName(),
    deviceType: String = "phone",
    protocolVersion: Int = 7
  ): NetworkPacket {
    return createMockPacket(
      type = NetworkPacket.PACKET_TYPE_IDENTITY,
      deviceId = deviceId,
      body = mapOf(
        "deviceId" to deviceId,
        "deviceName" to deviceName,
        "deviceType" to deviceType,
        "protocolVersion" to protocolVersion,
        "tcpPort" to 1716,
        "incomingCapabilities" to listOf(
          "cconnect.battery",
          "cconnect.clipboard",
          "cconnect.share"
        ),
        "outgoingCapabilities" to listOf(
          "cconnect.battery",
          "cconnect.clipboard",
          "cconnect.share"
        )
      )
    )
  }

  /**
   * Create a mock pairing request packet.
   */
  fun createPairRequestPacket(deviceId: String = TestUtils.randomDeviceId()): NetworkPacket {
    return createMockPacket(
      type = NetworkPacket.PACKET_TYPE_PAIR,
      deviceId = deviceId,
      body = mapOf("pair" to true)
    )
  }

  /**
   * Create a mock pairing response packet.
   */
  fun createPairResponsePacket(
    deviceId: String = TestUtils.randomDeviceId(),
    accepted: Boolean = true
  ): NetworkPacket {
    return createMockPacket(
      type = NetworkPacket.PACKET_TYPE_PAIR,
      deviceId = deviceId,
      body = mapOf("pair" to accepted)
    )
  }

  /**
   * Create a mock battery status packet.
   */
  fun createBatteryPacket(
    deviceId: String = TestUtils.randomDeviceId(),
    batteryLevel: Int = 75,
    isCharging: Boolean = false
  ): NetworkPacket {
    return createMockPacket(
      type = "cconnect.battery",
      deviceId = deviceId,
      body = mapOf(
        "currentCharge" to batteryLevel,
        "isCharging" to isCharging,
        "thresholdEvent" to 0
      )
    )
  }

  /**
   * Create a mock clipboard packet.
   */
  fun createClipboardPacket(
    deviceId: String = TestUtils.randomDeviceId(),
    content: String = "Test clipboard content"
  ): NetworkPacket {
    return createMockPacket(
      type = "cconnect.clipboard",
      deviceId = deviceId,
      body = mapOf("content" to content)
    )
  }

  /**
   * Create a mock share packet.
   */
  fun createSharePacket(
    deviceId: String = TestUtils.randomDeviceId(),
    filename: String = "test.txt",
    numberOfFiles: Int = 1,
    totalPayloadSize: Long = 1024
  ): NetworkPacket {
    return createMockPacket(
      type = "cconnect.share.request",
      deviceId = deviceId,
      body = mapOf(
        "filename" to filename,
        "numberOfFiles" to numberOfFiles,
        "totalPayloadSize" to totalPayloadSize
      )
    )
  }

  /**
   * Create a mock ping packet.
   */
  fun createPingPacket(
    deviceId: String = TestUtils.randomDeviceId(),
    message: String? = null
  ): NetworkPacket {
    val body = mutableMapOf<String, Any>()
    message?.let { body["message"] = it }

    return createMockPacket(
      type = "cconnect.ping",
      deviceId = deviceId,
      body = body
    )
  }

  /**
   * Create a mock runcommand packet.
   */
  fun createRunCommandPacket(
    deviceId: String = TestUtils.randomDeviceId(),
    key: String = "test_command",
    command: String = "echo test"
  ): NetworkPacket {
    return createMockPacket(
      type = "cconnect.runcommand",
      deviceId = deviceId,
      body = mapOf(
        "key" to key,
        "command" to command
      )
    )
  }

  /**
   * Create a mock device for testing.
   *
   * Note: Creating actual Device objects requires CosmicConnect instance
   * and proper initialization. This is a placeholder for UI tests.
   */
  fun createMockDeviceInfo(
    deviceId: String = TestUtils.randomDeviceId(),
    deviceName: String = TestUtils.randomTestName(),
    isPaired: Boolean = false,
    isReachable: Boolean = true,
    batteryLevel: Int = -1
  ): Map<String, Any> {
    return mapOf(
      "deviceId" to deviceId,
      "deviceName" to deviceName,
      "isPaired" to isPaired,
      "isReachable" to isReachable,
      "batteryLevel" to batteryLevel,
      "deviceType" to "phone"
    )
  }

  /**
   * Create multiple mock device infos for list testing.
   */
  fun createMockDeviceList(count: Int = 3): List<Map<String, Any>> {
    return (0 until count).map { index ->
      createMockDeviceInfo(
        deviceName = "Test Device $index",
        isPaired = index % 2 == 0,
        isReachable = index % 3 != 0,
        batteryLevel = if (index % 2 == 0) (50 + index * 10) else -1
      )
    }
  }

  /**
   * Create mock plugin info for testing.
   */
  fun createMockPluginInfo(
    pluginKey: String,
    pluginName: String,
    isEnabled: Boolean = true,
    isAvailable: Boolean = true
  ): Map<String, Any> {
    return mapOf(
      "key" to pluginKey,
      "name" to pluginName,
      "description" to "Test plugin: $pluginName",
      "isEnabled" to isEnabled,
      "isAvailable" to isAvailable,
      "hasSettings" to true,
      "hasMainActivity" to false
    )
  }

  /**
   * Create mock plugin list for device detail testing.
   */
  fun createMockPluginList(): List<Map<String, Any>> {
    return listOf(
      createMockPluginInfo("battery", "Battery Monitor", isEnabled = true),
      createMockPluginInfo("clipboard", "Clipboard Sync", isEnabled = true),
      createMockPluginInfo("share", "Share & Receive", isEnabled = false),
      createMockPluginInfo("ping", "Ping", isEnabled = true),
      createMockPluginInfo("runcommand", "Run Command", isEnabled = false),
      createMockPluginInfo("mpris", "Media Control", isEnabled = true)
    )
  }
}
