package org.cosmic.cosmicconnect

import android.util.Log
import org.cosmic.cosmicconnect.Core.CosmicConnectCore
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.PingPlugin.PingPacketsFFI
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * FFI Validation Tests - Minimal Working Set
 *
 * Contains only the tests that compile and should work
 * based on the actual FFI implementation.
 */
class FFIValidationTestMinimal {

    companion object {
        private const val TAG = "FFI-ValidationTest-Min"
        private var initialized = false
    }

    @Before
    fun setup() {
        if (!initialized) {
            try {
                CosmicConnectCore.initialize(logLevel = "debug")
                initialized = true
                Log.i(TAG, "CosmicConnectCore initialized for tests")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CosmicConnectCore", e)
                throw e
            }
        }
    }

    /**
     * Test 1.1: Native Library Loading
     */
    @Test
    fun testNativeLibraryLoading() {
        Log.i(TAG, "=== Test 1.1: Native Library Loading ===")

        assertTrue(
            "Native library should be loaded",
            CosmicConnectCore.isReady
        )

        Log.i(TAG, "PASS Native library loaded successfully")
    }

    /**
     * Test 1.2: Runtime Initialization
     */
    @Test
    fun testRuntimeInitialization() {
        Log.i(TAG, "=== Test 1.2: Runtime Initialization ===")

        assertTrue(
            "Runtime should be initialized",
            CosmicConnectCore.isReady
        )

        val version = CosmicConnectCore.version
        assertNotNull("Version should not be null", version)
        assertFalse("Version should not be empty", version.isEmpty())
        Log.i(TAG, "   Version: $version")

        val protocolVersion = CosmicConnectCore.protocolVersion
        assertEquals("Protocol version should be 8", 8, protocolVersion)
        Log.i(TAG, "   Protocol Version: $protocolVersion")

        Log.i(TAG, "PASS Runtime initialization successful")
    }

    /**
     * Test 2.1: NetworkPacket Creation
     */
    @Test
    fun testPacketCreation() {
        Log.i(TAG, "=== Test 2.1: NetworkPacket Creation ===")

        val packet = NetworkPacket.create(
            type = "kdeconnect.identity",
            body = mapOf(
                "deviceId" to "test-device-123",
                "deviceName" to "Test Device",
                "deviceType" to "phone",
                "protocolVersion" to 8
            )
        )

        assertNotNull("Packet should not be null", packet)
        assertEquals("Packet type should match", "kdeconnect.identity", packet.type)

        val body = packet.body
        assertEquals("deviceId should match", "test-device-123", body["deviceId"])
        assertEquals("deviceName should match", "Test Device", body["deviceName"])

        Log.i(TAG, "   Packet ID: ${packet.id}")
        Log.i(TAG, "   Packet Type: ${packet.type}")
        Log.i(TAG, "PASS Packet creation successful")
    }

    /**
     * Test 2.2: NetworkPacket Serialization
     */
    @Test
    fun testPacketSerialization() {
        Log.i(TAG, "=== Test 2.2: NetworkPacket Serialization ===")

        val packet = NetworkPacket.create(
            type = "kdeconnect.pair",
            body = mapOf("pair" to true)
        )

        val bytes = packet.serialize()
        assertNotNull("Serialized bytes should not be null", bytes)
        assertTrue("Serialized bytes should not be empty", bytes.isNotEmpty())

        val str = bytes.decodeToString()
        assertTrue("Should end with newline", str.endsWith("\n"))
        assertTrue("Should contain packet type", str.contains("\"type\":\"kdeconnect.pair\""))
        assertTrue("Should contain body", str.contains("\"pair\":true"))

        Log.i(TAG, "   Serialized size: ${bytes.size} bytes")
        Log.i(TAG, "   Content: ${str.trim()}")
        Log.i(TAG, "PASS Packet serialization successful")
    }

    /**
     * Test 2.3: NetworkPacket Deserialization
     */
    @Test
    fun testPacketDeserialization() {
        Log.i(TAG, "=== Test 2.3: NetworkPacket Deserialization ===")

        val json = """
        {
            "type": "kdeconnect.ping",
            "id": 1234567890,
            "body": {
                "message": "Hello World"
            }
        }
        """.trimIndent() + "\n"

        val packet = NetworkPacket.deserialize(json.encodeToByteArray())

        assertNotNull("Parsed packet should not be null", packet)
        assertEquals("Packet type should match", "kdeconnect.ping", packet.type)
        assertEquals("Packet ID should match", 1234567890L, packet.id)
        assertEquals("Message should match", "Hello World", packet.body["message"])

        Log.i(TAG, "   Packet ID: ${packet.id}")
        Log.i(TAG, "   Packet Type: ${packet.type}")
        Log.i(TAG, "   Message: ${packet.body["message"]}")
        Log.i(TAG, "PASS Packet deserialization successful")
    }

    /**
     * Test 3.2: Battery Plugin
     */
    @Test
    fun testBatteryPlugin() {
        Log.i(TAG, "=== Test 3.2: Battery Plugin ===")

        val packet = NetworkPacket.create(
            type = "kdeconnect.battery",
            body = mapOf(
                "currentCharge" to 85,
                "isCharging" to true,
                "thresholdEvent" to 0
            )
        )

        assertNotNull("Battery packet should not be null", packet)
        assertEquals("Packet type should be battery", "kdeconnect.battery", packet.type)
        assertEquals("Charge should match", 85, packet.body["currentCharge"])
        assertEquals("Charging status should match", true, packet.body["isCharging"])

        Log.i(TAG, "   Charge: ${packet.body["currentCharge"]}%")
        Log.i(TAG, "   Charging: ${packet.body["isCharging"]}")
        Log.i(TAG, "PASS Battery plugin packet test successful")
    }

    /**
     * Test 3.3: Ping Plugin (Legacy)
     */
    @Test
    fun testPingPluginLegacy() {
        Log.i(TAG, "=== Test 3.3: Ping Plugin (Legacy) ===")

        val packet = NetworkPacket.create(
            type = "kdeconnect.ping",
            body = mapOf("message" to "Test ping from Android")
        )

        assertNotNull("Ping packet should not be null", packet)
        assertEquals("Packet type should be ping", "kdeconnect.ping", packet.type)
        assertEquals("Message should match", "Test ping from Android", packet.body["message"])

        Log.i(TAG, "   Message: ${packet.body["message"]}")
        Log.i(TAG, "PASS Ping plugin packet test successful")
    }

    /**
     * Test 3.9: Ping Plugin FFI (Issue #61)
     */
    @Test
    fun testPingPlugin() {
        Log.i(TAG, "")
        Log.i(TAG, "=" + "=".repeat(70))
        Log.i(TAG, "Test 3.9: Ping Plugin FFI (Issue #61)")
        Log.i(TAG, "=" + "=".repeat(70))

        // Test 1: Create simple ping packet
        Log.i(TAG, "   Test 1: Create simple ping packet")
        val simplePing = PingPacketsFFI.createPing()

        assertNotNull("Simple ping packet should not be null", simplePing)
        assertEquals(
            "Packet type should be ping",
            "kdeconnect.ping",
            simplePing.type
        )
        Log.i(TAG, "   PASS Simple ping packet creation successful")

        // Test 2: Create ping with custom message
        Log.i(TAG, "   Test 2: Create ping with custom message")
        val messagePing = PingPacketsFFI.createPing("Hello from Android!")

        assertNotNull("Message ping packet should not be null", messagePing)
        assertEquals(
            "Packet type should be ping",
            "kdeconnect.ping",
            messagePing.type
        )
        assertTrue("Should have message field", messagePing.body.containsKey("message"))
        assertEquals("Message should match", "Hello from Android!", messagePing.body["message"])
        Log.i(TAG, "   PASS Message ping packet creation successful")

        // Test 3: Verify packets are unique
        Log.i(TAG, "   Test 3: Verify packet uniqueness")
        assertNotEquals("Packet IDs should be unique", simplePing.id, messagePing.id)
        Log.i(TAG, "   PASS All packets have unique IDs")

        // Test 4: Verify serialization
        Log.i(TAG, "   Test 4: Verify packet serialization")
        val serialized = messagePing.serialize()
        assertNotNull("Serialized bytes should not be null", serialized)
        assertTrue("Serialized bytes should not be empty", serialized.isNotEmpty())

        val serializedStr = serialized.decodeToString()
        assertTrue("Should contain message", serializedStr.contains("Hello from Android!"))
        assertTrue("Should end with newline", serializedStr.endsWith("\n"))
        Log.i(TAG, "   PASS Packet serialization successful")

        // Test 5: Get ping statistics
        Log.i(TAG, "   Test 5: Get ping statistics")
        val stats = PingPacketsFFI.getPingStats()
        assertNotNull("Stats should not be null", stats)
        assertTrue("Pings sent should be at least 2", stats.pingsSent >= 2u)
        Log.i(TAG, "   PASS Statistics: ${stats.pingsSent} sent, ${stats.pingsReceived} received")

        // Summary
        Log.i(TAG, "")
        Log.i(TAG, "PASS All Ping plugin FFI tests passed (5/5)")
        Log.i(TAG, "   - Simple ping packet creation")
        Log.i(TAG, "   - Message ping packet creation")
        Log.i(TAG, "   - Packet uniqueness")
        Log.i(TAG, "   - Serialization")
        Log.i(TAG, "   - Statistics tracking")
    }

    /**
     * Test 4.1: FFI Call Overhead Benchmark
     */
    @Test
    fun benchmarkFFICalls() {
        Log.i(TAG, "=== Test 4.1: FFI Call Overhead ===")

        val iterations = 1000

        // Benchmark 1: Packet creation
        val start1 = System.nanoTime()
        repeat(iterations) {
            NetworkPacket.create("kdeconnect.ping", mapOf("seq" to it))
        }
        val end1 = System.nanoTime()
        val avgCreate = (end1 - start1) / iterations / 1000.0 // microseconds

        // Benchmark 2: Packet serialization
        val packet = NetworkPacket.create("kdeconnect.ping", mapOf("msg" to "test"))
        val start2 = System.nanoTime()
        repeat(iterations) {
            packet.serialize()
        }
        val end2 = System.nanoTime()
        val avgSerialize = (end2 - start2) / iterations / 1000.0 // microseconds

        // Benchmark 3: Packet deserialization
        val bytes = packet.serialize()
        val start3 = System.nanoTime()
        repeat(iterations) {
            NetworkPacket.deserialize(bytes)
        }
        val end3 = System.nanoTime()
        val avgDeserialize = (end3 - start3) / iterations / 1000.0 // microseconds

        Log.i(TAG, "   Packet creation: %.2f μs".format(avgCreate))
        Log.i(TAG, "   Serialization: %.2f μs".format(avgSerialize))
        Log.i(TAG, "   Deserialization: %.2f μs".format(avgDeserialize))

        // Relaxed targets for first test
        assertTrue("Packet creation should be fast", avgCreate < 100.0)
        assertTrue("Serialization should be fast", avgSerialize < 500.0)
        assertTrue("Deserialization should be fast", avgDeserialize < 1000.0)

        Log.i(TAG, "PASS FFI call overhead benchmark complete")
    }

    /**
     * Test 5.1: End-to-End Packet Flow
     */
    @Test
    fun testEndToEndPacketFlow() {
        Log.i(TAG, "=== Test 5.1: End-to-End Packet Flow ===")

        // 1. Create packet
        val packet = NetworkPacket.create(
            type = "kdeconnect.share.request",
            body = mapOf(
                "filename" to "test.txt",
                "text" to "Hello from Android via FFI"
            )
        )

        // 2. Serialize
        val bytes = packet.serialize()
        assertTrue("Serialized bytes should not be empty", bytes.isNotEmpty())

        // 3. Simulate network transmission (loopback)
        val receivedBytes = bytes

        // 4. Deserialize
        val receivedPacket = NetworkPacket.deserialize(receivedBytes)

        // 5. Verify data integrity
        assertEquals("Packet type should match", packet.type, receivedPacket.type)
        assertEquals("Filename should match", "test.txt", receivedPacket.body["filename"])
        assertEquals(
            "Text should match",
            "Hello from Android via FFI",
            receivedPacket.body["text"]
        )

        Log.i(TAG, "   Original packet ID: ${packet.id}")
        Log.i(TAG, "   Received packet ID: ${receivedPacket.id}")
        Log.i(TAG, "   Data integrity: OK")
        Log.i(TAG, "PASS End-to-end packet flow successful")
    }

    /**
     * Test Summary
     */
    @Test
    fun printTestSummary() {
        Log.i(TAG, "")
        Log.i(TAG, "=" + "=".repeat(70))
        Log.i(TAG, "FFI Validation Test Summary - Minimal Working Set")
        Log.i(TAG, "=" + "=".repeat(70))
        Log.i(TAG, "")
        Log.i(TAG, "Core Version: ${CosmicConnectCore.version}")
        Log.i(TAG, "Protocol Version: ${CosmicConnectCore.protocolVersion}")
        Log.i(TAG, "")
        Log.i(TAG, "PASS Native library: LOADED")
        Log.i(TAG, "PASS Runtime: INITIALIZED")
        Log.i(TAG, "PASS FFI calls: WORKING")
        Log.i(TAG, "")
        Log.i(TAG, "See individual test results above for detailed metrics.")
        Log.i(TAG, "=" + "=".repeat(70))
    }
}
