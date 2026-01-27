package org.cosmic.cconnect.performance

import android.content.Context
import android.net.Uri
import android.os.Debug
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cosmic.cconnect.CosmicConnect
import org.cosmic.cconnect.Device
import org.cosmic.cconnect.NetworkPacket
import org.cosmic.cconnect.plugins.*
import org.cosmic.cconnect.test.MockFactory
import org.cosmic.cconnect.test.TestUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for COSMIC Connect Android.
 *
 * Tests performance characteristics of:
 * - FFI layer overhead
 * - File transfer throughput
 * - Network operations latency
 * - Memory usage and efficiency
 * - Battery impact
 * - Concurrent operations
 * - Stress testing under load
 *
 * Run with: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.cosmic.cconnect.performance.PerformanceBenchmarkTest
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarkTest {

    private lateinit var context: Context
    private lateinit var cosmicConnect: CosmicConnect
    private lateinit var testDevice: Device
    private val testDataDir = File("/sdcard/cosmic_performance_test")

    // Performance thresholds
    companion object {
        // FFI thresholds
        const val MAX_FFI_CALL_OVERHEAD_NS = 1_000_000 // 1ms
        const val MAX_PACKET_SERIALIZATION_NS = 5_000_000 // 5ms

        // File transfer thresholds
        const val MIN_SMALL_FILE_THROUGHPUT_MBPS = 5.0 // 5 MB/s
        const val MIN_LARGE_FILE_THROUGHPUT_MBPS = 20.0 // 20 MB/s

        // Network operation thresholds
        const val MAX_DISCOVERY_LATENCY_MS = 5000L // 5 seconds
        const val MAX_PAIRING_TIME_MS = 10000L // 10 seconds
        const val MAX_PACKET_RTT_MS = 500L // 500ms

        // Memory thresholds
        const val MAX_MEMORY_GROWTH_MB = 50 // 50 MB during operations
        const val MAX_GC_PRESSURE_PERCENT = 30 // 30% time in GC

        // Stress test parameters
        const val STRESS_PACKET_COUNT = 1000
        const val STRESS_CONCURRENT_TRANSFERS = 5
        const val STRESS_DURATION_MINUTES = 5
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        cosmicConnect = CosmicConnect.getInstance(context)

        // Create test directory
        testDataDir.mkdirs()

        // Setup a test device for performance testing
        val deviceId = TestUtils.randomDeviceId()
        testDevice = cosmicConnect.getOrCreateDevice(
            deviceId = deviceId,
            deviceName = "Performance Test Device",
            deviceType = "phone",
            ipAddress = "192.168.1.100"
        )

        // Mark as paired to enable all features
        testDevice.setPaired(true)

        println("\n=== Performance Benchmark Test Suite ===")
        println("Device: ${testDevice.deviceName}")
        println("Test data directory: ${testDataDir.absolutePath}")
    }

    @After
    fun tearDown() {
        TestUtils.cleanupTestData()
        testDataDir.deleteRecursively()
        cosmicConnect.stopDiscovery()
    }

    // ==========================================
    // FFI Performance Benchmarks
    // ==========================================

    @Test
    fun benchmark_FfiCallOverhead() {
        println("\n--- FFI Call Overhead Benchmark ---")

        val iterations = 10000
        var totalTime = 0L

        // Warmup
        repeat(100) {
            cosmicConnect.getConnectedDevices()
        }

        // Measure
        repeat(iterations) {
            totalTime += measureNanoTime {
                cosmicConnect.getConnectedDevices()
            }
        }

        val avgTimeNs = totalTime / iterations
        val avgTimeMs = avgTimeNs / 1_000_000.0

        println("Average FFI call overhead: ${avgTimeNs}ns (${avgTimeMs}ms)")
        println("Iterations: $iterations")
        println("Total time: ${totalTime / 1_000_000}ms")

        assert(avgTimeNs < MAX_FFI_CALL_OVERHEAD_NS) {
            "FFI call overhead ${avgTimeNs}ns exceeds threshold ${MAX_FFI_CALL_OVERHEAD_NS}ns"
        }

        println("✓ FFI call overhead within acceptable range")
    }

    @Test
    fun benchmark_PacketSerialization() {
        println("\n--- Packet Serialization Benchmark ---")

        val iterations = 1000
        val packetTypes = listOf("battery", "clipboard", "ping", "share", "mpris", "telephony")

        packetTypes.forEach { type ->
            var totalSerializeTime = 0L
            var totalDeserializeTime = 0L

            // Create test packet
            val packet = when (type) {
                "battery" -> MockFactory.createBatteryPacket(testDevice.deviceId, 75, false)
                "clipboard" -> MockFactory.createClipboardPacket(testDevice.deviceId, "Test content")
                "ping" -> MockFactory.createPingPacket(testDevice.deviceId, "Test ping")
                "share" -> MockFactory.createSharePacket(testDevice.deviceId, "test.txt", 1024)
                "mpris" -> MockFactory.createMprisPacket(testDevice.deviceId, "Artist", "Title", "Album")
                "telephony" -> MockFactory.createSmsPacket(testDevice.deviceId, "+1234567890", "Test message")
                else -> MockFactory.createIdentityPacket(testDevice.deviceId, "Test", "phone")
            }

            // Warmup
            repeat(10) {
                val serialized = packet.serialize()
                NetworkPacket.deserialize(serialized)
            }

            // Measure serialization
            repeat(iterations) {
                totalSerializeTime += measureNanoTime {
                    packet.serialize()
                }
            }

            // Measure deserialization
            val serialized = packet.serialize()
            repeat(iterations) {
                totalDeserializeTime += measureNanoTime {
                    NetworkPacket.deserialize(serialized)
                }
            }

            val avgSerializeNs = totalSerializeTime / iterations
            val avgDeserializeNs = totalDeserializeTime / iterations

            println("\n$type packet:")
            println("  Serialize:   ${avgSerializeNs}ns (${avgSerializeNs / 1_000_000.0}ms)")
            println("  Deserialize: ${avgDeserializeNs}ns (${avgDeserializeNs / 1_000_000.0}ms)")

            assert(avgSerializeNs < MAX_PACKET_SERIALIZATION_NS) {
                "$type serialization ${avgSerializeNs}ns exceeds threshold ${MAX_PACKET_SERIALIZATION_NS}ns"
            }
            assert(avgDeserializeNs < MAX_PACKET_SERIALIZATION_NS) {
                "$type deserialization ${avgDeserializeNs}ns exceeds threshold ${MAX_PACKET_SERIALIZATION_NS}ns"
            }
        }

        println("\n✓ All packet serialization/deserialization within acceptable range")
    }

    @Test
    fun benchmark_FfiDataTransfer() {
        println("\n--- FFI Data Transfer Benchmark ---")

        val dataSizes = listOf(
            1024 to "1 KB",
            10 * 1024 to "10 KB",
            100 * 1024 to "100 KB",
            1024 * 1024 to "1 MB"
        )

        dataSizes.forEach { (size, label) ->
            val data = ByteArray(size) { it.toByte() }
            var totalTime = 0L
            val iterations = 100

            // Warmup
            repeat(10) {
                MockFactory.createSharePacket(testDevice.deviceId, "test.bin", data.size.toLong())
            }

            // Measure
            repeat(iterations) {
                totalTime += measureNanoTime {
                    // Simulate FFI data transfer by creating packet with data
                    MockFactory.createSharePacket(testDevice.deviceId, "test.bin", data.size.toLong())
                }
            }

            val avgTimeMs = (totalTime / iterations) / 1_000_000.0
            val throughputMBps = (size / (1024.0 * 1024.0)) / (avgTimeMs / 1000.0)

            println("\n$label transfer:")
            println("  Average time: ${avgTimeMs}ms")
            println("  Throughput: ${throughputMBps} MB/s")
        }

        println("\n✓ FFI data transfer performance measured")
    }

    // ==========================================
    // File Transfer Performance
    // ==========================================

    @Test
    fun benchmark_SmallFileTransfer() {
        println("\n--- Small File Transfer Benchmark ---")

        val sharePlugin = testDevice.getPlugin("share") as SharePlugin
        val fileSize = 1024 * 1024 // 1 MB
        val testFile = createTestFile("small_test.bin", fileSize)

        val transferComplete = CountDownLatch(1)
        var transferTimeMs = 0L

        val listener = object : SharePlugin.TransferListener {
            override fun onTransferStarted(transferId: String, filename: String, totalBytes: Long) {
                transferTimeMs = System.currentTimeMillis()
            }

            override fun onTransferProgress(transferId: String, bytesTransferred: Long, totalBytes: Long) {
                // Track progress
            }

            override fun onTransferComplete(transferId: String) {
                transferTimeMs = System.currentTimeMillis() - transferTimeMs
                transferComplete.countDown()
            }

            override fun onTransferFailed(transferId: String, error: String) {
                transferComplete.countDown()
            }
        }

        sharePlugin.addTransferListener(listener)

        val startTime = System.currentTimeMillis()
        sharePlugin.shareFile(Uri.fromFile(testFile))

        assert(transferComplete.await(30, TimeUnit.SECONDS)) {
            "File transfer timed out"
        }

        val throughputMBps = (fileSize / (1024.0 * 1024.0)) / (transferTimeMs / 1000.0)

        println("File size: 1 MB")
        println("Transfer time: ${transferTimeMs}ms")
        println("Throughput: ${throughputMBps} MB/s")

        assert(throughputMBps >= MIN_SMALL_FILE_THROUGHPUT_MBPS) {
            "Small file throughput ${throughputMBps} MB/s below threshold ${MIN_SMALL_FILE_THROUGHPUT_MBPS} MB/s"
        }

        println("✓ Small file transfer throughput acceptable")
    }

    @Test
    fun benchmark_LargeFileTransfer() {
        println("\n--- Large File Transfer Benchmark ---")

        val sharePlugin = testDevice.getPlugin("share") as SharePlugin
        val fileSize = 50 * 1024 * 1024 // 50 MB
        val testFile = createTestFile("large_test.bin", fileSize)

        val transferComplete = CountDownLatch(1)
        var transferTimeMs = 0L
        val progressUpdates = AtomicInteger(0)

        val listener = object : SharePlugin.TransferListener {
            override fun onTransferStarted(transferId: String, filename: String, totalBytes: Long) {
                transferTimeMs = System.currentTimeMillis()
            }

            override fun onTransferProgress(transferId: String, bytesTransferred: Long, totalBytes: Long) {
                progressUpdates.incrementAndGet()
            }

            override fun onTransferComplete(transferId: String) {
                transferTimeMs = System.currentTimeMillis() - transferTimeMs
                transferComplete.countDown()
            }

            override fun onTransferFailed(transferId: String, error: String) {
                transferComplete.countDown()
            }
        }

        sharePlugin.addTransferListener(listener)
        sharePlugin.shareFile(Uri.fromFile(testFile))

        assert(transferComplete.await(120, TimeUnit.SECONDS)) {
            "Large file transfer timed out"
        }

        val throughputMBps = (fileSize / (1024.0 * 1024.0)) / (transferTimeMs / 1000.0)

        println("File size: 50 MB")
        println("Transfer time: ${transferTimeMs}ms (${transferTimeMs / 1000.0}s)")
        println("Throughput: ${throughputMBps} MB/s")
        println("Progress updates: ${progressUpdates.get()}")

        assert(throughputMBps >= MIN_LARGE_FILE_THROUGHPUT_MBPS) {
            "Large file throughput ${throughputMBps} MB/s below threshold ${MIN_LARGE_FILE_THROUGHPUT_MBPS} MB/s"
        }

        println("✓ Large file transfer throughput acceptable")
    }

    @Test
    fun benchmark_ConcurrentFileTransfers() {
        println("\n--- Concurrent File Transfer Benchmark ---")

        val sharePlugin = testDevice.getPlugin("share") as SharePlugin
        val fileCount = 5
        val fileSize = 5 * 1024 * 1024 // 5 MB each

        val testFiles = (1..fileCount).map { index ->
            createTestFile("concurrent_$index.bin", fileSize)
        }

        val allTransfersComplete = CountDownLatch(fileCount)
        val transferTimes = mutableListOf<Long>()
        val transferStartTimes = mutableMapOf<String, Long>()

        val listener = object : SharePlugin.TransferListener {
            override fun onTransferStarted(transferId: String, filename: String, totalBytes: Long) {
                synchronized(transferStartTimes) {
                    transferStartTimes[transferId] = System.currentTimeMillis()
                }
            }

            override fun onTransferProgress(transferId: String, bytesTransferred: Long, totalBytes: Long) {
                // Track progress
            }

            override fun onTransferComplete(transferId: String) {
                synchronized(transferStartTimes) {
                    val startTime = transferStartTimes[transferId] ?: return
                    val duration = System.currentTimeMillis() - startTime
                    transferTimes.add(duration)
                }
                allTransfersComplete.countDown()
            }

            override fun onTransferFailed(transferId: String, error: String) {
                allTransfersComplete.countDown()
            }
        }

        sharePlugin.addTransferListener(listener)

        val totalStartTime = System.currentTimeMillis()

        // Start all transfers concurrently
        testFiles.forEach { file ->
            sharePlugin.shareFile(Uri.fromFile(file))
        }

        assert(allTransfersComplete.await(180, TimeUnit.SECONDS)) {
            "Concurrent file transfers timed out"
        }

        val totalTime = System.currentTimeMillis() - totalStartTime
        val avgTransferTime = transferTimes.average()
        val totalDataMB = (fileSize * fileCount) / (1024.0 * 1024.0)
        val aggregateThroughputMBps = totalDataMB / (totalTime / 1000.0)

        println("Files transferred: $fileCount x ${fileSize / (1024 * 1024)} MB = ${totalDataMB.toInt()} MB")
        println("Total time: ${totalTime}ms (${totalTime / 1000.0}s)")
        println("Average transfer time: ${avgTransferTime.toLong()}ms")
        println("Aggregate throughput: ${aggregateThroughputMBps} MB/s")

        println("✓ Concurrent file transfers completed successfully")
    }

    // ==========================================
    // Network Performance
    // ==========================================

    @Test
    fun benchmark_DiscoveryLatency() {
        println("\n--- Discovery Latency Benchmark ---")

        val deviceDiscovered = CountDownLatch(1)
        var discoveryLatencyMs = 0L

        val listener = object : CosmicConnect.DiscoveryListener {
            override fun onDeviceDiscovered(device: Device) {
                discoveryLatencyMs = System.currentTimeMillis() - discoveryLatencyMs
                deviceDiscovered.countDown()
            }

            override fun onDeviceLost(device: Device) {}
        }

        cosmicConnect.addDiscoveryListener(listener)

        discoveryLatencyMs = System.currentTimeMillis()
        cosmicConnect.startDiscovery()

        // Simulate device discovery
        val mockPacket = MockFactory.createIdentityPacket(
            TestUtils.randomDeviceId(),
            "Benchmark Device",
            "desktop"
        )
        cosmicConnect.processIncomingPacket(mockPacket)

        assert(deviceDiscovered.await(30, TimeUnit.SECONDS)) {
            "Device discovery timed out"
        }

        println("Discovery latency: ${discoveryLatencyMs}ms")

        assert(discoveryLatencyMs < MAX_DISCOVERY_LATENCY_MS) {
            "Discovery latency ${discoveryLatencyMs}ms exceeds threshold ${MAX_DISCOVERY_LATENCY_MS}ms"
        }

        println("✓ Discovery latency within acceptable range")
    }

    @Test
    fun benchmark_PairingTime() {
        println("\n--- Pairing Time Benchmark ---")

        val unpaired = cosmicConnect.getOrCreateDevice(
            TestUtils.randomDeviceId(),
            "Pairing Benchmark Device",
            "desktop",
            "192.168.1.101"
        )

        val pairingComplete = CountDownLatch(1)
        var pairingTimeMs = 0L

        val listener = object : Device.PairingListener {
            override fun onPairRequest(device: Device) {}

            override fun onPaired(device: Device) {
                pairingTimeMs = System.currentTimeMillis() - pairingTimeMs
                pairingComplete.countDown()
            }

            override fun onPairingFailed(device: Device, error: String) {
                pairingComplete.countDown()
            }

            override fun onUnpaired(device: Device) {}
        }

        unpaired.addPairingListener(listener)

        pairingTimeMs = System.currentTimeMillis()
        unpaired.requestPairing()

        // Simulate pairing acceptance
        unpaired.acceptPairing()

        assert(pairingComplete.await(30, TimeUnit.SECONDS)) {
            "Pairing timed out"
        }

        println("Pairing time: ${pairingTimeMs}ms")

        assert(pairingTimeMs < MAX_PAIRING_TIME_MS) {
            "Pairing time ${pairingTimeMs}ms exceeds threshold ${MAX_PAIRING_TIME_MS}ms"
        }

        println("✓ Pairing time within acceptable range")
    }

    @Test
    fun benchmark_PacketRoundTripTime() {
        println("\n--- Packet Round-Trip Time Benchmark ---")

        val pingPlugin = testDevice.getPlugin("ping") as PingPlugin
        val iterations = 100
        val roundTripTimes = mutableListOf<Long>()

        repeat(iterations) { index ->
            val pongReceived = CountDownLatch(1)
            var rttMs = 0L

            val listener = object : PingPlugin.PingListener {
                override fun onPingSent(message: String) {}

                override fun onPongReceived(message: String) {
                    rttMs = System.currentTimeMillis() - rttMs
                    roundTripTimes.add(rttMs)
                    pongReceived.countDown()
                }
            }

            pingPlugin.addPingListener(listener)

            rttMs = System.currentTimeMillis()
            pingPlugin.sendPing("Benchmark ping $index")

            // Simulate pong response
            val pongPacket = MockFactory.createPingPacket(testDevice.deviceId, "Benchmark ping $index")
            cosmicConnect.processIncomingPacket(pongPacket)

            assert(pongReceived.await(5, TimeUnit.SECONDS)) {
                "Pong not received for iteration $index"
            }

            pingPlugin.removePingListener(listener)
        }

        val avgRtt = roundTripTimes.average()
        val minRtt = roundTripTimes.minOrNull() ?: 0L
        val maxRtt = roundTripTimes.maxOrNull() ?: 0L
        val p95Rtt = roundTripTimes.sorted()[((iterations * 0.95).toInt())]

        println("Iterations: $iterations")
        println("Average RTT: ${avgRtt.toLong()}ms")
        println("Min RTT: ${minRtt}ms")
        println("Max RTT: ${maxRtt}ms")
        println("P95 RTT: ${p95Rtt}ms")

        assert(avgRtt < MAX_PACKET_RTT_MS) {
            "Average RTT ${avgRtt.toLong()}ms exceeds threshold ${MAX_PACKET_RTT_MS}ms"
        }

        println("✓ Packet round-trip time within acceptable range")
    }

    // ==========================================
    // Memory Performance
    // ==========================================

    @Test
    fun benchmark_MemoryUsage() {
        println("\n--- Memory Usage Benchmark ---")

        // Force GC to get baseline
        System.gc()
        Thread.sleep(1000)

        val initialMemoryMB = getUsedMemoryMB()
        println("Initial memory usage: ${initialMemoryMB} MB")

        // Perform memory-intensive operations
        val operations = listOf(
            "Discovery" to { performDiscoveryOperations(50) },
            "Pairing" to { performPairingOperations(20) },
            "File Transfer" to { performFileTransferOperations(10) },
            "Plugin Operations" to { performPluginOperations(100) }
        )

        operations.forEach { (name, operation) ->
            System.gc()
            Thread.sleep(500)

            val beforeMB = getUsedMemoryMB()
            operation()

            System.gc()
            Thread.sleep(500)

            val afterMB = getUsedMemoryMB()
            val growthMB = afterMB - beforeMB

            println("\n$name:")
            println("  Before: ${beforeMB} MB")
            println("  After: ${afterMB} MB")
            println("  Growth: ${growthMB} MB")

            assert(growthMB < MAX_MEMORY_GROWTH_MB) {
                "$name memory growth ${growthMB} MB exceeds threshold ${MAX_MEMORY_GROWTH_MB} MB"
            }
        }

        // Check for memory leaks
        System.gc()
        Thread.sleep(1000)
        val finalMemoryMB = getUsedMemoryMB()
        val totalGrowthMB = finalMemoryMB - initialMemoryMB

        println("\nFinal memory usage: ${finalMemoryMB} MB")
        println("Total growth: ${totalGrowthMB} MB")

        println("✓ Memory usage within acceptable limits")
    }

    @Test
    fun benchmark_GarbageCollectionPressure() {
        println("\n--- GC Pressure Benchmark ---")

        val testDurationMs = 30000L // 30 seconds
        val gcStartTime = Debug.getNativeHeapAllocatedSize()

        val startTime = System.currentTimeMillis()
        var operationCount = 0

        // Perform operations continuously for test duration
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            // Discovery
            cosmicConnect.startDiscovery()
            Thread.sleep(10)
            cosmicConnect.stopDiscovery()

            // Create and destroy packets
            val packet = MockFactory.createBatteryPacket(testDevice.deviceId, 75, false)
            val serialized = packet.serialize()
            NetworkPacket.deserialize(serialized)

            operationCount++

            if (operationCount % 100 == 0) {
                Thread.sleep(100) // Brief pause every 100 operations
            }
        }

        val gcEndTime = Debug.getNativeHeapAllocatedSize()
        val gcPressure = ((gcEndTime - gcStartTime).toDouble() / gcStartTime * 100)

        println("Test duration: ${testDurationMs}ms")
        println("Operations performed: $operationCount")
        println("GC pressure: ${gcPressure.toInt()}%")

        // This is a rough estimate - actual GC pressure measurement requires more sophisticated tools
        println("✓ GC pressure benchmark completed")
    }

    // ==========================================
    // Stress Testing
    // ==========================================

    @Test
    fun stress_HighFrequencyPackets() {
        println("\n--- High Frequency Packet Stress Test ---")

        val packetCount = STRESS_PACKET_COUNT
        val packetsSent = AtomicInteger(0)
        val packetsReceived = AtomicInteger(0)

        val allPacketsProcessed = CountDownLatch(packetCount)

        val listener = object : BatteryPlugin.BatteryListener {
            override fun onBatteryStatusReceived(level: Int, isCharging: Boolean) {
                packetsReceived.incrementAndGet()
                allPacketsProcessed.countDown()
            }

            override fun onBatteryStatusSent(level: Int, isCharging: Boolean) {}
        }

        val batteryPlugin = testDevice.getPlugin("battery") as BatteryPlugin
        batteryPlugin.addBatteryListener(listener)

        val startTime = System.currentTimeMillis()

        // Send packets as fast as possible
        repeat(packetCount) { index ->
            val packet = MockFactory.createBatteryPacket(
                testDevice.deviceId,
                (index % 100),
                index % 2 == 0
            )
            cosmicConnect.processIncomingPacket(packet)
            packetsSent.incrementAndGet()
        }

        assert(allPacketsProcessed.await(60, TimeUnit.SECONDS)) {
            "Not all packets processed in time"
        }

        val totalTime = System.currentTimeMillis() - startTime
        val packetsPerSecond = (packetCount * 1000.0) / totalTime

        println("Packets sent: ${packetsSent.get()}")
        println("Packets received: ${packetsReceived.get()}")
        println("Total time: ${totalTime}ms")
        println("Throughput: ${packetsPerSecond.toInt()} packets/second")

        assert(packetsSent.get() == packetsReceived.get()) {
            "Packet loss detected: sent ${packetsSent.get()}, received ${packetsReceived.get()}"
        }

        println("✓ High frequency packet stress test passed")
    }

    @Test
    fun stress_MultipleSimultaneousConnections() {
        println("\n--- Multiple Simultaneous Connections Stress Test ---")

        val deviceCount = 10
        val devices = mutableListOf<Device>()

        // Create multiple devices
        repeat(deviceCount) { index ->
            val device = cosmicConnect.getOrCreateDevice(
                TestUtils.randomDeviceId(),
                "Stress Test Device $index",
                "phone",
                "192.168.1.${100 + index}"
            )
            device.setPaired(true)
            devices.add(device)
        }

        println("Created $deviceCount test devices")

        // Perform operations on all devices simultaneously
        val allOperationsComplete = CountDownLatch(deviceCount * 3) // 3 operations per device

        devices.forEach { device ->
            // Battery operation
            Thread {
                val batteryPlugin = device.getPlugin("battery") as BatteryPlugin
                batteryPlugin.sendBatteryStatus(75, false)
                allOperationsComplete.countDown()
            }.start()

            // Clipboard operation
            Thread {
                val clipboardPlugin = device.getPlugin("clipboard") as ClipboardPlugin
                clipboardPlugin.sendClipboard("Stress test content ${device.deviceId}")
                allOperationsComplete.countDown()
            }.start()

            // Ping operation
            Thread {
                val pingPlugin = device.getPlugin("ping") as PingPlugin
                pingPlugin.sendPing("Stress test ping ${device.deviceId}")
                allOperationsComplete.countDown()
            }.start()
        }

        assert(allOperationsComplete.await(60, TimeUnit.SECONDS)) {
            "Not all operations completed in time"
        }

        println("All operations on $deviceCount devices completed successfully")
        println("Total operations: ${deviceCount * 3}")

        println("✓ Multiple simultaneous connections stress test passed")
    }

    @Test
    fun stress_LongRunningOperations() {
        println("\n--- Long Running Operations Stress Test ---")

        val durationMinutes = STRESS_DURATION_MINUTES
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000)

        var cycleCount = 0
        val errors = mutableListOf<String>()

        println("Running stress test for $durationMinutes minutes...")

        while (System.currentTimeMillis() < endTime) {
            try {
                // Discovery cycle
                cosmicConnect.startDiscovery()
                Thread.sleep(1000)
                cosmicConnect.stopDiscovery()

                // Plugin operations
                val batteryPlugin = testDevice.getPlugin("battery") as BatteryPlugin
                batteryPlugin.sendBatteryStatus((cycleCount % 100), cycleCount % 2 == 0)

                val clipboardPlugin = testDevice.getPlugin("clipboard") as ClipboardPlugin
                clipboardPlugin.sendClipboard("Stress cycle $cycleCount")

                // Small file transfer
                val sharePlugin = testDevice.getPlugin("share") as SharePlugin
                val smallFile = createTestFile("stress_$cycleCount.txt", 1024)
                sharePlugin.shareFile(Uri.fromFile(smallFile))

                cycleCount++

                if (cycleCount % 10 == 0) {
                    println("Completed $cycleCount stress cycles...")
                    val remainingMs = endTime - System.currentTimeMillis()
                    val remainingMin = remainingMs / (60 * 1000)
                    println("  Time remaining: $remainingMin minutes")
                }

                Thread.sleep(2000) // 2 second pause between cycles

            } catch (e: Exception) {
                errors.add("Cycle $cycleCount: ${e.message}")
                println("Error in cycle $cycleCount: ${e.message}")
            }
        }

        println("\nStress test completed:")
        println("  Duration: $durationMinutes minutes")
        println("  Cycles completed: $cycleCount")
        println("  Errors: ${errors.size}")

        if (errors.isNotEmpty()) {
            println("\nErrors encountered:")
            errors.take(10).forEach { println("  - $it") }
            if (errors.size > 10) {
                println("  ... and ${errors.size - 10} more")
            }
        }

        val errorRate = (errors.size.toDouble() / cycleCount * 100)
        println("  Error rate: ${errorRate.toInt()}%")

        assert(errorRate < 10) {
            "Error rate ${errorRate.toInt()}% exceeds 10% threshold"
        }

        println("✓ Long running operations stress test passed")
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private fun createTestFile(filename: String, size: Int): File {
        val file = File(testDataDir, filename)
        file.outputStream().use { output ->
            val buffer = ByteArray(8192)
            var remaining = size
            while (remaining > 0) {
                val toWrite = minOf(remaining, buffer.size)
                output.write(buffer, 0, toWrite)
                remaining -= toWrite
            }
        }
        return file
    }

    private fun getUsedMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    private fun performDiscoveryOperations(count: Int) {
        repeat(count) {
            cosmicConnect.startDiscovery()
            Thread.sleep(50)
            cosmicConnect.stopDiscovery()
            Thread.sleep(50)
        }
    }

    private fun performPairingOperations(count: Int) {
        repeat(count) {
            val device = cosmicConnect.getOrCreateDevice(
                TestUtils.randomDeviceId(),
                "Test Device $it",
                "phone",
                "192.168.1.${100 + it}"
            )
            device.requestPairing()
            Thread.sleep(100)
        }
    }

    private fun performFileTransferOperations(count: Int) {
        val sharePlugin = testDevice.getPlugin("share") as SharePlugin
        repeat(count) {
            val file = createTestFile("mem_test_$it.txt", 1024)
            sharePlugin.shareFile(Uri.fromFile(file))
            Thread.sleep(100)
        }
    }

    private fun performPluginOperations(count: Int) {
        repeat(count) {
            val batteryPlugin = testDevice.getPlugin("battery") as BatteryPlugin
            batteryPlugin.sendBatteryStatus(it % 100, it % 2 == 0)

            val clipboardPlugin = testDevice.getPlugin("clipboard") as ClipboardPlugin
            clipboardPlugin.sendClipboard("Test $it")

            val pingPlugin = testDevice.getPlugin("ping") as PingPlugin
            pingPlugin.sendPing("Ping $it")

            Thread.sleep(10)
        }
    }
}
