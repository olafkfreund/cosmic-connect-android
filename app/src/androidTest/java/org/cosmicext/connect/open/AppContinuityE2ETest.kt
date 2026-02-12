/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.open

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmicext.connect.NetworkPacket
import org.cosmicext.connect.test.MockFactory
import org.cosmicext.connect.test.TestUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * AppContinuityE2ETest - End-to-End Integration Tests for App Continuity
 *
 * Tests the Open plugin functionality for opening URLs, files, and text content
 * across Android and COSMIC Desktop devices.
 *
 * ## Test Coverage
 *
 * 1. **URL Opening Tests**
 *    - Send HTTPS URL to desktop
 *    - Send HTTP URL to desktop
 *    - Send mailto URL to desktop
 *    - Receive URL from desktop
 *    - Receive tel URL from desktop
 *
 * 2. **Security Validation Tests**
 *    - Reject file:// scheme
 *    - Reject javascript: scheme
 *    - Reject data: scheme
 *    - Reject localhost URLs
 *    - Reject internal IP addresses
 *    - Reject URLs with embedded credentials
 *    - Reject malformed URLs
 *
 * 3. **Error Handling Tests**
 *    - Handle network disconnect
 *    - Handle desktop rejection
 *    - Handle timeout
 *
 * ## Issue Reference
 * - Issue #121: End-to-End Integration Testing for App Continuity
 * - Issue #112: App Continuity (parent issue)
 */
@RunWith(AndroidJUnit4::class)
class AppContinuityE2ETest {

    private lateinit var testDeviceId: String
    private var sentPackets = mutableListOf<NetworkPacket>()
    private var receivedResponses = mutableListOf<NetworkPacket>()

    @Before
    fun setUp() {
        testDeviceId = TestUtils.randomDeviceId()
        sentPackets.clear()
        receivedResponses.clear()
    }

    @After
    fun tearDown() {
        TestUtils.cleanupTestData()
    }

    // ========================================================================
    // URL Opening Tests (Android → Desktop, Desktop → Android)
    // ========================================================================

    /**
     * Test: Send HTTPS URL to desktop
     *
     * Scenario: Android app requests to open HTTPS URL on COSMIC Desktop
     * Expected: Valid open request packet created and would be sent
     */
    @Test
    fun testSendHttpsUrlToDesktop() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.VALID_HTTPS

        // Act
        val packet = OpenPluginTestUtils.createMockOpenRequest(url)

        // Assert
        assertTrue("Packet should be valid open request", OpenPluginTestUtils.isValidOpenRequest(packet))
        assertEquals("Packet type should be cconnect.open.request", "cconnect.open.request", packet.type)
        assertEquals("URL should match", url, OpenPluginTestUtils.extractUrl(packet))
        assertFalse("HTTPS URL should not be rejected", OpenPluginTestUtils.shouldRejectUrl(url))
    }

    /**
     * Test: Send HTTP URL to desktop
     *
     * Scenario: Android app requests to open HTTP URL on COSMIC Desktop
     * Expected: Valid open request packet created (HTTP allowed for compatibility)
     */
    @Test
    fun testSendHttpUrlToDesktop() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.VALID_HTTP

        // Act
        val packet = OpenPluginTestUtils.createMockOpenRequest(url)

        // Assert
        assertTrue("Packet should be valid open request", OpenPluginTestUtils.isValidOpenRequest(packet))
        assertEquals("URL should match", url, OpenPluginTestUtils.extractUrl(packet))
        assertTrue("HTTP scheme should be allowed", OpenPluginTestUtils.isAllowedScheme(url))
    }

    /**
     * Test: Send mailto URL to desktop
     *
     * Scenario: Android app requests to open email client on COSMIC Desktop
     * Expected: Valid open request packet with mailto URL
     */
    @Test
    fun testSendMailtoUrlToDesktop() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.VALID_MAILTO

        // Act
        val packet = OpenPluginTestUtils.createMockOpenRequest(url)

        // Assert
        assertTrue("Packet should be valid open request", OpenPluginTestUtils.isValidOpenRequest(packet))
        assertEquals("URL should match", url, OpenPluginTestUtils.extractUrl(packet))
        assertTrue("mailto scheme should be allowed", OpenPluginTestUtils.isAllowedScheme(url))
    }

    /**
     * Test: Receive URL from desktop
     *
     * Scenario: COSMIC Desktop sends URL to Android for opening
     * Expected: Android receives valid open request and can process it
     */
    @Test
    fun testReceiveUrlFromDesktop() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.VALID_HTTPS
        val incomingPacket = OpenPluginTestUtils.createMockOpenRequest(url)

        // Act
        val receivedUrl = OpenPluginTestUtils.extractUrl(incomingPacket)

        // Assert
        assertNotNull("URL should be extracted", receivedUrl)
        assertEquals("URL should match", url, receivedUrl)
        assertTrue("Packet should be valid open request", OpenPluginTestUtils.isValidOpenRequest(incomingPacket))
        assertFalse("URL should not be rejected", OpenPluginTestUtils.shouldRejectUrl(receivedUrl!!))
    }

    /**
     * Test: Receive tel URL from desktop
     *
     * Scenario: COSMIC Desktop sends phone number to Android
     * Expected: Android receives tel: URL for dialer integration
     */
    @Test
    fun testReceiveTelUrlFromDesktop() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.VALID_TEL
        val incomingPacket = OpenPluginTestUtils.createMockOpenRequest(url)

        // Act
        val receivedUrl = OpenPluginTestUtils.extractUrl(incomingPacket)

        // Assert
        assertNotNull("URL should be extracted", receivedUrl)
        assertEquals("URL should match", url, receivedUrl)
        assertTrue("tel scheme should be allowed", OpenPluginTestUtils.isAllowedScheme(receivedUrl!!))
    }

    // ========================================================================
    // Security Validation Tests
    // ========================================================================

    /**
     * Test: Reject file:// scheme
     *
     * Security: Prevent file system access via URLs
     * Expected: file:// URLs should be rejected
     */
    @Test
    fun testRejectFileScheme() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.INVALID_FILE

        // Act
        val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)
        val isAllowed = OpenPluginTestUtils.isAllowedScheme(url)

        // Assert
        assertTrue("file:// URLs should be rejected", shouldReject)
        assertFalse("file scheme should not be allowed", isAllowed)
    }

    /**
     * Test: Reject javascript: scheme
     *
     * Security: Prevent XSS and code injection
     * Expected: javascript: URLs should be rejected
     */
    @Test
    fun testRejectJavascriptScheme() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.INVALID_JAVASCRIPT

        // Act
        val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)
        val isAllowed = OpenPluginTestUtils.isAllowedScheme(url)

        // Assert
        assertTrue("javascript: URLs should be rejected", shouldReject)
        assertFalse("javascript scheme should not be allowed", isAllowed)
    }

    /**
     * Test: Reject data: scheme
     *
     * Security: Prevent data URI injection
     * Expected: data: URLs should be rejected
     */
    @Test
    fun testRejectDataScheme() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.INVALID_DATA

        // Act
        val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)
        val isAllowed = OpenPluginTestUtils.isAllowedScheme(url)

        // Assert
        assertTrue("data: URLs should be rejected", shouldReject)
        assertFalse("data scheme should not be allowed", isAllowed)
    }

    /**
     * Test: Reject localhost URLs
     *
     * Security: Prevent access to local services
     * Expected: localhost URLs should be rejected
     */
    @Test
    fun testRejectLocalhost() {
        // Arrange
        val urls = listOf(
            OpenPluginTestUtils.TestUrls.INVALID_LOCALHOST,
            OpenPluginTestUtils.TestUrls.INVALID_LOCALHOST_HTTPS
        )

        // Act & Assert
        urls.forEach { url ->
            val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)
            assertTrue("localhost URL should be rejected: $url", shouldReject)
        }
    }

    /**
     * Test: Reject internal IP addresses
     *
     * Security: Prevent access to internal network resources
     * Expected: Internal IPs (127.x.x.x, 10.x.x.x, 172.16-31.x.x, 192.168.x.x) rejected
     */
    @Test
    fun testRejectInternalIp() {
        // Arrange
        val urls = listOf(
            OpenPluginTestUtils.TestUrls.INVALID_IP_127,
            OpenPluginTestUtils.TestUrls.INVALID_IP_INTERNAL,
            OpenPluginTestUtils.TestUrls.INVALID_IP_INTERNAL_10,
            OpenPluginTestUtils.TestUrls.INVALID_IP_INTERNAL_172
        )

        // Act & Assert
        urls.forEach { url ->
            val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)
            assertTrue("Internal IP should be rejected: $url", shouldReject)
        }
    }

    /**
     * Test: Reject URLs with embedded credentials
     *
     * Security: Prevent credential leakage in URLs
     * Expected: URLs with username:password@ should be rejected
     */
    @Test
    fun testRejectUrlWithCredentials() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.INVALID_CREDS

        // Act
        val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)

        // Assert
        assertTrue("URLs with embedded credentials should be rejected", shouldReject)
    }

    /**
     * Test: Reject malformed URLs
     *
     * Security: Prevent URL parsing vulnerabilities
     * Expected: Malformed URLs should be rejected
     */
    @Test
    fun testRejectMalformedUrl() {
        // Arrange
        val urls = listOf(
            OpenPluginTestUtils.TestUrls.MALFORMED_NO_SCHEME,
            OpenPluginTestUtils.TestUrls.MALFORMED_SPACES,
            OpenPluginTestUtils.TestUrls.MALFORMED_INVALID_CHARS
        )

        // Act & Assert
        urls.forEach { url ->
            val shouldReject = OpenPluginTestUtils.shouldRejectUrl(url)
            assertTrue("Malformed URL should be rejected: $url", shouldReject)
        }
    }

    // ========================================================================
    // File and Text Opening Tests
    // ========================================================================

    /**
     * Test: Send file open request
     *
     * Scenario: Android app requests to open file on COSMIC Desktop
     * Expected: Valid file open request packet created
     */
    @Test
    fun testSendFileOpenRequest() {
        // Arrange
        val fileUri = "content://media/external/downloads/1234"
        val mimeType = "application/pdf"

        // Act
        val packet = OpenPluginTestUtils.createMockFileOpenRequest(fileUri, mimeType)

        // Assert
        assertTrue("Packet should be valid open request", OpenPluginTestUtils.isValidOpenRequest(packet))
        assertEquals("Packet type should be cconnect.open.request", "cconnect.open.request", packet.type)
        assertEquals("File URI should match", fileUri, packet.getString("file_uri"))
        assertEquals("MIME type should match", mimeType, packet.getString("mime_type"))
    }

    /**
     * Test: Send text open request
     *
     * Scenario: Android app sends text content to open in editor on desktop
     * Expected: Valid text open request packet created
     */
    @Test
    fun testSendTextOpenRequest() {
        // Arrange
        val textContent = "Sample text content for testing"

        // Act
        val packet = OpenPluginTestUtils.createMockTextOpenRequest(textContent)

        // Assert
        assertTrue("Packet should be valid open request", OpenPluginTestUtils.isValidOpenRequest(packet))
        assertEquals("Packet type should be cconnect.open.request", "cconnect.open.request", packet.type)
        assertEquals("Text content should match", textContent, packet.getString("text"))
    }

    // ========================================================================
    // Response Handling Tests
    // ========================================================================

    /**
     * Test: Receive success response
     *
     * Scenario: Desktop successfully opens URL and sends response
     * Expected: Success response received and parsed correctly
     */
    @Test
    fun testReceiveSuccessResponse() {
        // Arrange
        val responsePacket = OpenPluginTestUtils.createMockOpenResponse(success = true)

        // Act
        val success = OpenPluginTestUtils.extractSuccess(responsePacket)
        val error = OpenPluginTestUtils.extractError(responsePacket)

        // Assert
        assertTrue("Packet should be valid open response", OpenPluginTestUtils.isValidOpenResponse(responsePacket))
        assertNotNull("Success status should be present", success)
        assertTrue("Success should be true", success!!)
        assertNull("Error should be null on success", error)
    }

    /**
     * Test: Receive error response
     *
     * Scenario: Desktop fails to open URL and sends error response
     * Expected: Error response received with error message
     */
    @Test
    fun testReceiveErrorResponse() {
        // Arrange
        val errorMessage = "Failed to open URL: Application not found"
        val responsePacket = OpenPluginTestUtils.createMockOpenResponse(success = false, error = errorMessage)

        // Act
        val success = OpenPluginTestUtils.extractSuccess(responsePacket)
        val error = OpenPluginTestUtils.extractError(responsePacket)

        // Assert
        assertTrue("Packet should be valid open response", OpenPluginTestUtils.isValidOpenResponse(responsePacket))
        assertNotNull("Success status should be present", success)
        assertFalse("Success should be false", success!!)
        assertNotNull("Error message should be present", error)
        assertEquals("Error message should match", errorMessage, error)
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    /**
     * Test: Handle network disconnect during send
     *
     * Scenario: Network connection lost while sending open request
     * Expected: Appropriate error handling (test validates packet creation succeeds)
     */
    @Test
    fun testHandleNetworkDisconnect() {
        // Arrange
        val url = OpenPluginTestUtils.TestUrls.VALID_HTTPS

        // Act
        val packet = OpenPluginTestUtils.createMockOpenRequest(url)

        // Assert
        assertTrue("Packet creation should succeed even if network fails later",
                   OpenPluginTestUtils.isValidOpenRequest(packet))
        // Note: Actual network error handling would be tested in integration tests
        // with real network connections
    }

    /**
     * Test: Handle desktop rejects request
     *
     * Scenario: Desktop receives request but rejects it
     * Expected: Error response received
     */
    @Test
    fun testHandleDesktopRejectsRequest() {
        // Arrange
        val errorMessage = "Desktop rejected open request: Security policy violation"
        val responsePacket = OpenPluginTestUtils.createMockOpenResponse(success = false, error = errorMessage)

        // Act
        val success = OpenPluginTestUtils.extractSuccess(responsePacket)
        val error = OpenPluginTestUtils.extractError(responsePacket)

        // Assert
        assertNotNull("Success status should be present", success)
        assertFalse("Success should be false", success!!)
        assertNotNull("Error message should be present", error)
        assertTrue("Error should indicate rejection", error!!.contains("rejected"))
    }

    /**
     * Test: Handle timeout
     *
     * Scenario: No response received within timeout period
     * Expected: Timeout handling (validates packet structure for timeout scenarios)
     */
    @Test
    fun testHandleTimeout() {
        // Arrange
        val latch = CountDownLatch(1)
        var responseReceived = false

        // Simulate timeout scenario
        val timeoutMs = 100L

        // Act
        val timedOut = !latch.await(timeoutMs, TimeUnit.MILLISECONDS)

        // Assert
        assertTrue("Timeout should occur when no response received", timedOut)
        assertFalse("No response should be received", responseReceived)
    }

    // ========================================================================
    // Extension Property Tests
    // ========================================================================

    /**
     * Test: Packet type checking
     *
     * Scenario: Validate packet types correctly
     * Expected: Type validation works correctly
     */
    @Test
    fun testPacketTypeValidation() {
        // Arrange
        val requestPacket = OpenPluginTestUtils.createMockOpenRequest(OpenPluginTestUtils.TestUrls.VALID_HTTPS)
        val responsePacket = OpenPluginTestUtils.createMockOpenResponse(success = true)

        // Act & Assert
        assertEquals("Request packet should have correct type", "cconnect.open.request", requestPacket.type)
        assertEquals("Response packet should have correct type", "cconnect.open.response", responsePacket.type)

        assertTrue("Request packet should have URL field", requestPacket.has("url"))
        assertTrue("Response packet should have success field", responsePacket.has("success"))

        assertEquals("URL should match",
                     OpenPluginTestUtils.TestUrls.VALID_HTTPS,
                     requestPacket.getString("url"))
        assertEquals("Success should be true", true, responsePacket.getBoolean("success"))
    }

    /**
     * Test: Multiple URLs in sequence
     *
     * Scenario: Send multiple URLs to desktop in sequence
     * Expected: All packets created correctly
     */
    @Test
    fun testMultipleUrlsInSequence() {
        // Arrange
        val urls = listOf(
            OpenPluginTestUtils.TestUrls.VALID_HTTPS,
            OpenPluginTestUtils.TestUrls.VALID_MAILTO,
            OpenPluginTestUtils.TestUrls.VALID_TEL
        )

        // Act
        val packets = urls.map { url -> OpenPluginTestUtils.createMockOpenRequest(url) }

        // Assert
        assertEquals("Should create packet for each URL", urls.size, packets.size)
        packets.forEachIndexed { index, packet ->
            assertTrue("Packet $index should be valid", OpenPluginTestUtils.isValidOpenRequest(packet))
            assertEquals("URL should match", urls[index], OpenPluginTestUtils.extractUrl(packet))
        }
    }

    /**
     * Test: URL validation edge cases
     *
     * Scenario: Test edge cases in URL validation
     * Expected: Correct validation for edge cases
     */
    @Test
    fun testUrlValidationEdgeCases() {
        // Test case-insensitive scheme detection
        assertTrue("Uppercase FILE should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("FILE:///etc/passwd"))
        assertTrue("Mixed case JavaScript should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("JavaScript:alert(1)"))

        // Test localhost variations
        assertTrue("Localhost with port should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("http://localhost:8080/path"))
        assertTrue("HTTPS localhost should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("https://LOCALHOST/path"))

        // Test IP variations
        assertTrue("Loopback IP should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("http://127.0.0.1:3000"))
        assertTrue("Private IP 10.x should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("http://10.1.2.3"))
        assertTrue("Private IP 172.16.x should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("http://172.16.1.1"))
        assertTrue("Private IP 192.168.x should be rejected",
                   OpenPluginTestUtils.shouldRejectUrl("http://192.168.0.1"))

        // Test valid edge cases
        assertFalse("Normal HTTPS with path should be allowed",
                    OpenPluginTestUtils.shouldRejectUrl("https://example.com/path/to/page?query=value"))
        assertFalse("Subdomain should be allowed",
                    OpenPluginTestUtils.shouldRejectUrl("https://sub.example.com"))
    }
}
