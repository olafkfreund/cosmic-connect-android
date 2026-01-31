package org.cosmic.cosmicconnect.test

import android.content.Context
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.NetworkPacket
import java.security.cert.Certificate

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Mock Factory
 *
 * Factory for creating mock objects for testing.
 * Creates realistic test data for devices, network packets, and FFI interactions.
 */
object MockFactory {

  class MockLinkProvider : BaseLinkProvider() {
      override fun onStart() {}
      override fun onStop() {}
      override fun onNetworkChange(network: android.net.Network?) {}
      override val name: String = "MockLinkProvider"
      override val priority: Int = 0
  }

  class MockLink(
      context: Context,
      linkProvider: BaseLinkProvider,
      override val deviceInfo: DeviceInfo
  ) : BaseLink(context, linkProvider) {
      override val name: String = "MockLink"
      
      val sentPackets = CopyOnWriteArrayList<NetworkPacket>()

      override fun sendPacket(
          np: NetworkPacket,
          callback: Device.SendPacketStatusCallback,
          sendPayloadFromSameThread: Boolean
      ): Boolean {
          sentPackets.add(np)
          callback.onSuccess()
          return true
      }
  }

  private fun createDummyCertificate(): Certificate {
      return object : Certificate("X.509") {
          override fun getEncoded(): ByteArray = ByteArray(0)
          override fun verify(key: java.security.PublicKey?) {}
          override fun verify(key: java.security.PublicKey?, sigProvider: String?) {}
          override fun toString(): String = "MockCertificate"
          override fun getPublicKey(): java.security.PublicKey? = null
      }
  }

  /**
   * Create a mock Link for device registration.
   */
  fun createMockLink(
      context: Context,
      deviceId: String,
      deviceName: String = "Test Device",
      deviceType: org.cosmic.cosmicconnect.DeviceType = org.cosmic.cosmicconnect.DeviceType.DESKTOP
  ): BaseLink {
      val linkProvider = MockLinkProvider()
      val cert = createDummyCertificate()
      val deviceInfo = DeviceInfo(
          id = deviceId,
          certificate = cert,
          name = deviceName,
          type = deviceType,
          protocolVersion = 8,
          incomingCapabilities = setOf("cconnect.battery", "cconnect.clipboard", "cconnect.share", "cconnect.ping", "cconnect.runcommand", "cconnect.mpris", "cconnect.telephony"),
          outgoingCapabilities = setOf("cconnect.battery", "cconnect.clipboard", "cconnect.share", "cconnect.ping", "cconnect.runcommand", "cconnect.mpris", "cconnect.telephony")
      )
      return MockLink(context, linkProvider, deviceInfo)
  }

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
    val packet = NetworkPacket(type)
    
    // Populate packet body
    body.forEach { (key, value) ->
        when (value) {
            is String -> packet[key] = value
            is Int -> packet[key] = value
            is Long -> packet[key] = value
            is Boolean -> packet[key] = value
            is Double -> packet[key] = value
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                if (value.isNotEmpty() && value.first() is String) {
                    packet[key] = value as List<String>
                }
            }
        }
    }
    
    return packet
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
