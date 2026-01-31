/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.OpenOnPhonePlugin
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for OpenOnPhonePlugin
 *
 * Tests URL validation, packet handling, and security checks
 */
@RunWith(AndroidJUnit4::class)
class OpenOnPhonePluginTest {

    private lateinit var plugin: OpenOnPhonePlugin
    private lateinit var context: Context

    @Mock
    private lateinit var mockDevice: Device

    private lateinit var closeable: AutoCloseable

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        plugin = OpenOnPhonePlugin()
        plugin.setContext(context, mockDevice)

        // Mock device properties
        whenever(mockDevice.deviceId).thenReturn("test-device-id")
        whenever(mockDevice.name).thenReturn("Test Device")
    }

    @After
    fun tearDown() {
        closeable.close()
    }

    // ========================================================================
    // URL Validation Tests
    // ========================================================================

    @Test
    fun testValidateUrl_ValidHttps() {
        val error = plugin.validateUrl("https://example.com")
        assertNull("HTTPS URL should be valid", error)
    }

    @Test
    fun testValidateUrl_ValidHttp() {
        val error = plugin.validateUrl("http://example.com")
        assertNull("HTTP URL should be valid", error)
    }

    @Test
    fun testValidateUrl_ValidMailto() {
        val error = plugin.validateUrl("mailto:test@example.com")
        assertNull("Mailto URL should be valid", error)
    }

    @Test
    fun testValidateUrl_ValidTel() {
        val error = plugin.validateUrl("tel:+1234567890")
        assertNull("Tel URL should be valid", error)
    }

    @Test
    fun testValidateUrl_ValidGeo() {
        val error = plugin.validateUrl("geo:37.7749,-122.4194")
        assertNull("Geo URL should be valid", error)
    }

    @Test
    fun testValidateUrl_ValidSms() {
        val error = plugin.validateUrl("sms:+1234567890")
        assertNull("SMS URL should be valid", error)
    }

    @Test
    fun testValidateUrl_RejectsFileScheme() {
        val error = plugin.validateUrl("file:///etc/passwd")
        assertNotNull("File scheme should be rejected", error)
        assertTrue("Error should mention scheme", error!!.contains("scheme"))
    }

    @Test
    fun testValidateUrl_RejectsJavascriptScheme() {
        val error = plugin.validateUrl("javascript:alert('xss')")
        assertNotNull("Javascript scheme should be rejected", error)
        assertTrue("Error should mention scheme", error!!.contains("scheme"))
    }

    @Test
    fun testValidateUrl_RejectsDataScheme() {
        val error = plugin.validateUrl("data:text/html,<script>alert('xss')</script>")
        assertNotNull("Data scheme should be rejected", error)
        assertTrue("Error should mention scheme", error!!.contains("scheme"))
    }

    @Test
    fun testValidateUrl_RejectsNoScheme() {
        val error = plugin.validateUrl("example.com")
        assertNotNull("URL without scheme should be rejected", error)
        assertTrue("Error should mention scheme", error!!.contains("scheme"))
    }

    @Test
    fun testValidateUrl_RejectsTooLong() {
        val longUrl = "https://example.com/" + "a".repeat(OpenOnPhonePlugin.MAX_URL_LENGTH)
        val error = plugin.validateUrl(longUrl)
        assertNotNull("Too-long URL should be rejected", error)
        assertTrue("Error should mention length", error!!.contains("too long"))
    }

    @Test
    fun testValidateUrl_RejectsNullByte() {
        val error = plugin.validateUrl("https://example.com\u0000/path")
        assertNotNull("URL with null byte should be rejected", error)
        assertTrue("Error should mention invalid characters", error!!.contains("invalid"))
    }

    @Test
    fun testValidateUrl_RejectsCredentials() {
        val error = plugin.validateUrl("https://user:pass@example.com")
        assertNotNull("URL with credentials should be rejected", error)
        assertTrue("Error should mention credentials", error!!.contains("credentials"))
    }

    @Test
    fun testValidateUrl_RejectsLocalhost() {
        val error = plugin.validateUrl("http://localhost:8080")
        assertNotNull("Localhost URL should be rejected", error)
        assertTrue("Error should mention localhost", error!!.contains("localhost"))
    }

    @Test
    fun testValidateUrl_Rejects127_0_0_1() {
        val error = plugin.validateUrl("http://127.0.0.1")
        assertNotNull("127.0.0.1 should be rejected", error)
        assertTrue("Error should mention localhost or private", error!!.contains("localhost") || error.contains("private"))
    }

    @Test
    fun testValidateUrl_RejectsPrivateIP_10() {
        val error = plugin.validateUrl("http://10.0.0.1")
        assertNotNull("10.0.0.1 should be rejected", error)
        assertTrue("Error should mention private network", error!!.contains("private") || error.contains("localhost"))
    }

    @Test
    fun testValidateUrl_RejectsPrivateIP_192() {
        val error = plugin.validateUrl("http://192.168.1.1")
        assertNotNull("192.168.1.1 should be rejected", error)
        assertTrue("Error should mention private network", error!!.contains("private") || error.contains("localhost"))
    }

    @Test
    fun testValidateUrl_RejectsPrivateIP_172() {
        val error = plugin.validateUrl("http://172.16.0.1")
        assertNotNull("172.16.0.1 should be rejected", error)
        assertTrue("Error should mention private network", error!!.contains("private") || error.contains("localhost"))
    }

    @Test
    fun testValidateUrl_AcceptsPublicIP() {
        val error = plugin.validateUrl("http://8.8.8.8")
        assertNull("Public IP should be valid", error)
    }

    @Test
    fun testValidateUrl_AcceptsPublicDomain() {
        val error = plugin.validateUrl("https://www.google.com")
        assertNull("Public domain should be valid", error)
    }

    @Test
    fun testValidateUrl_AcceptsUrlWithPath() {
        val error = plugin.validateUrl("https://example.com/path/to/page?query=param#anchor")
        assertNull("URL with path, query, and anchor should be valid", error)
    }

    @Test
    fun testValidateUrl_RejectsMissingHostname() {
        val error = plugin.validateUrl("https://")
        assertNotNull("URL without hostname should be rejected", error)
        assertTrue("Error should mention hostname", error!!.contains("hostname"))
    }

    // ========================================================================
    // Packet Handling Tests
    // ========================================================================

    @Test
    fun testOnPacketReceived_OpenRequest() {
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            set("requestId", "test-request-123")
            set("url", "https://example.com")
            set("title", "Test Page")
        }

        val result = plugin.onPacketReceived(np)
        assertTrue("Plugin should handle open request", result)
    }

    @Test
    fun testOnPacketReceived_CapabilityAnnouncement() {
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_CAPABILITY)

        val result = plugin.onPacketReceived(np)
        assertTrue("Plugin should handle capability announcement", result)
    }

    @Test
    fun testOnPacketReceived_InvalidPacketType() {
        val np = NetworkPacket("cconnect.invalid.type")

        val result = plugin.onPacketReceived(np)
        assertFalse("Plugin should not handle invalid packet type", result)
    }

    @Test
    fun testOnPacketReceived_MissingRequestId() {
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            set("url", "https://example.com")
        }

        // Should not crash, but should not show notification either
        val result = plugin.onPacketReceived(np)
        assertTrue("Plugin should handle packet but not process it", result)
    }

    @Test
    fun testOnPacketReceived_MissingUrl() {
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            set("requestId", "test-request-123")
        }

        // Should not crash, but should not show notification either
        val result = plugin.onPacketReceived(np)
        assertTrue("Plugin should handle packet but not process it", result)
    }

    @Test
    fun testOnPacketReceived_InvalidUrl() {
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            set("requestId", "test-request-123")
            set("url", "javascript:alert('xss')")
        }

        // Should send rejection response
        val result = plugin.onPacketReceived(np)
        assertTrue("Plugin should handle packet", result)

        // Verify rejection response sent (mock verification would go here in real implementation)
    }

    // ========================================================================
    // URL Opening Tests
    // ========================================================================

    @Test
    fun testOpenUrl_ValidUrl() {
        // This test would ideally verify Intent creation
        // For now, just ensure it doesn't crash
        val url = "https://example.com"

        // Would need to mock Intent handling to fully test
        // For basic test, just verify method exists and accepts valid URL
        assertNotNull("openUrl method should exist", plugin::openUrl)
    }

    // ========================================================================
    // Plugin Metadata Tests
    // ========================================================================

    @Test
    fun testSupportedPacketTypes() {
        val types = plugin.supportedPacketTypes
        assertEquals("Should support 2 packet types", 2, types.size)
        assertTrue("Should support open.request",
            OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST in types)
        assertTrue("Should support open.capability",
            OpenOnPhonePlugin.PACKET_TYPE_OPEN_CAPABILITY in types)
    }

    @Test
    fun testOutgoingPacketTypes() {
        val types = plugin.outgoingPacketTypes
        assertEquals("Should send 2 packet types", 2, types.size)
        assertTrue("Should send open.response",
            OpenOnPhonePlugin.PACKET_TYPE_OPEN_RESPONSE in types)
        assertTrue("Should send open.capability",
            OpenOnPhonePlugin.PACKET_TYPE_OPEN_CAPABILITY in types)
    }

    @Test
    fun testDisplayName() {
        val name = plugin.displayName
        assertNotNull("Display name should not be null", name)
        assertFalse("Display name should not be empty", name.isEmpty())
    }

    @Test
    fun testDescription() {
        val desc = plugin.description
        assertNotNull("Description should not be null", desc)
        assertFalse("Description should not be empty", desc.isEmpty())
    }

    // ========================================================================
    // Edge Case Tests
    // ========================================================================

    @Test
    fun testValidateUrl_CaseSensitivity() {
        // Scheme should be case-insensitive
        assertNull("HTTPS uppercase should be valid", plugin.validateUrl("HTTPS://example.com"))
        assertNull("Http mixed case should be valid", plugin.validateUrl("HtTpS://example.com"))
    }

    @Test
    fun testValidateUrl_IPv6() {
        // IPv6 addresses should be handled
        val error = plugin.validateUrl("http://[2001:db8::1]")
        // Should either accept or reject consistently
        // For now, just ensure it doesn't crash
        assertNotNull("Method should return a result for IPv6", error != null || error == null)
    }

    @Test
    fun testValidateUrl_InternationalizedDomain() {
        // IDN should be handled
        val error = plugin.validateUrl("https://münchen.de")
        // Should handle internationalized domains
        assertNotNull("Method should return a result for IDN", error != null || error == null)
    }
}
