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
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.OpenOnDesktopPlugin
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.OpenPacketsFFI
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.isOpenRequest
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.openUrl
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.openFileUri
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.openMimeType
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.openText
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for OpenOnDesktopPlugin
 *
 * ## Test Coverage
 *
 * - URL validation with allowed/disallowed schemes
 * - Packet creation for URLs, files, and text
 * - Extension property correctness
 * - Security validation edge cases
 */
@RunWith(AndroidJUnit4::class)
class OpenPluginTest {

    private lateinit var plugin: OpenOnDesktopPlugin
    private lateinit var context: Context
    private lateinit var mockDevice: Device

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockDevice = mock(Device::class.java)

        plugin = OpenOnDesktopPlugin()
        plugin.setContext(context, mockDevice)
    }

    // ========================================================================
    // URL Validation Tests
    // ========================================================================

    @Test
    fun testValidateUrl_AllowedHttpScheme() {
        assertTrue(
            plugin.validateUrl("http://example.com"),
            "HTTP URLs should be allowed"
        )
    }

    @Test
    fun testValidateUrl_AllowedHttpsScheme() {
        assertTrue(
            plugin.validateUrl("https://example.com"),
            "HTTPS URLs should be allowed"
        )
    }

    @Test
    fun testValidateUrl_AllowedMailtoScheme() {
        assertTrue(
            plugin.validateUrl("mailto:user@example.com"),
            "Mailto URLs should be allowed"
        )
    }

    @Test
    fun testValidateUrl_AllowedTelScheme() {
        assertTrue(
            plugin.validateUrl("tel:+1234567890"),
            "Tel URLs should be allowed"
        )
    }

    @Test
    fun testValidateUrl_AllowedGeoScheme() {
        assertTrue(
            plugin.validateUrl("geo:37.7749,-122.4194"),
            "Geo URLs should be allowed"
        )
    }

    @Test
    fun testValidateUrl_AllowedSmsScheme() {
        assertTrue(
            plugin.validateUrl("sms:+1234567890"),
            "SMS URLs should be allowed"
        )
    }

    @Test
    fun testValidateUrl_DisallowedFileScheme() {
        assertFalse(
            plugin.validateUrl("file:///etc/passwd"),
            "File URLs should be rejected for security"
        )
    }

    @Test
    fun testValidateUrl_DisallowedJavascriptScheme() {
        assertFalse(
            plugin.validateUrl("javascript:alert('XSS')"),
            "JavaScript URLs should be rejected for security"
        )
    }

    @Test
    fun testValidateUrl_DisallowedDataScheme() {
        assertFalse(
            plugin.validateUrl("data:text/html,<script>alert('XSS')</script>"),
            "Data URLs should be rejected for security"
        )
    }

    @Test
    fun testValidateUrl_DisallowedBlobScheme() {
        assertFalse(
            plugin.validateUrl("blob:https://example.com/uuid"),
            "Blob URLs should be rejected"
        )
    }

    @Test
    fun testValidateUrl_DisallowedContentScheme() {
        assertFalse(
            plugin.validateUrl("content://com.android.providers.media.documents/document/1"),
            "Content URLs should be rejected (Android-specific)"
        )
    }

    @Test
    fun testValidateUrl_InvalidUrlFormat() {
        assertFalse(
            plugin.validateUrl("not a valid url"),
            "Invalid URLs should be rejected"
        )
    }

    @Test
    fun testValidateUrl_EmptyString() {
        assertFalse(
            plugin.validateUrl(""),
            "Empty URLs should be rejected"
        )
    }

    @Test
    fun testValidateUrl_CaseInsensitiveScheme() {
        assertTrue(
            plugin.validateUrl("HTTPS://example.com"),
            "Scheme validation should be case-insensitive"
        )
    }

    // ========================================================================
    // Packet Creation Tests - URLs
    // ========================================================================

    @Test
    fun testCreateUrlOpenRequest_BasicUrl() {
        val url = "https://example.com"
        val packet = OpenPacketsFFI.createUrlOpenRequest(url)

        assertTrue(packet.isOpenRequest, "Should be an open request packet")
        assertEquals(url, packet.openUrl, "URL should match")
        assertEquals("browser", packet.body["open_in"], "Default open_in should be 'browser'")
    }

    @Test
    fun testCreateUrlOpenRequest_CustomOpenIn() {
        val url = "https://example.com"
        val packet = OpenPacketsFFI.createUrlOpenRequest(url, "editor")

        assertEquals("editor", packet.body["open_in"], "Custom open_in should be preserved")
    }

    @Test
    fun testCreateUrlOpenRequest_MailtoUrl() {
        val url = "mailto:test@example.com"
        val packet = OpenPacketsFFI.createUrlOpenRequest(url)

        assertEquals(url, packet.openUrl, "Mailto URL should be preserved")
    }

    @Test
    fun testCreateUrlOpenRequest_TelUrl() {
        val url = "tel:+1234567890"
        val packet = OpenPacketsFFI.createUrlOpenRequest(url)

        assertEquals(url, packet.openUrl, "Tel URL should be preserved")
    }

    // ========================================================================
    // Packet Creation Tests - Files
    // ========================================================================

    @Test
    fun testCreateFileOpenRequest_BasicFile() {
        val uri = "content://com.android.providers.media/document/123"
        val mimeType = "application/pdf"
        val packet = OpenPacketsFFI.createFileOpenRequest(uri, mimeType)

        assertTrue(packet.isOpenRequest, "Should be an open request packet")
        assertEquals(uri, packet.openFileUri, "File URI should match")
        assertEquals(mimeType, packet.openMimeType, "MIME type should match")
        assertEquals("default", packet.body["open_in"], "Default open_in should be 'default'")
    }

    @Test
    fun testCreateFileOpenRequest_CustomOpenIn() {
        val uri = "content://media/123"
        val mimeType = "text/plain"
        val packet = OpenPacketsFFI.createFileOpenRequest(uri, mimeType, "editor")

        assertEquals("editor", packet.body["open_in"], "Custom open_in should be preserved")
    }

    @Test
    fun testCreateFileOpenRequest_ImageFile() {
        val uri = "content://media/image/456"
        val mimeType = "image/jpeg"
        val packet = OpenPacketsFFI.createFileOpenRequest(uri, mimeType)

        assertEquals(mimeType, packet.openMimeType, "Image MIME type should be preserved")
    }

    // ========================================================================
    // Packet Creation Tests - Text
    // ========================================================================

    @Test
    fun testCreateTextOpenRequest_BasicText() {
        val text = "Hello, COSMIC Desktop!"
        val packet = OpenPacketsFFI.createTextOpenRequest(text)

        assertTrue(packet.isOpenRequest, "Should be an open request packet")
        assertEquals(text, packet.openText, "Text content should match")
        assertEquals("editor", packet.body["open_in"], "Default open_in should be 'editor'")
    }

    @Test
    fun testCreateTextOpenRequest_LongText() {
        val text = "Lorem ipsum ".repeat(100)
        val packet = OpenPacketsFFI.createTextOpenRequest(text)

        assertEquals(text, packet.openText, "Long text should be preserved")
    }

    @Test
    fun testCreateTextOpenRequest_SpecialCharacters() {
        val text = "Special chars: \n\t\r\"'<>&"
        val packet = OpenPacketsFFI.createTextOpenRequest(text)

        assertEquals(text, packet.openText, "Special characters should be preserved")
    }

    @Test
    fun testCreateTextOpenRequest_Unicode() {
        val text = "Unicode: 你好世界 🌍 привет"
        val packet = OpenPacketsFFI.createTextOpenRequest(text)

        assertEquals(text, packet.openText, "Unicode characters should be preserved")
    }

    // ========================================================================
    // Packet Creation Tests - Responses
    // ========================================================================

    @Test
    fun testCreateOpenResponse_Success() {
        val packet = OpenPacketsFFI.createOpenResponse(success = true)

        assertEquals("cconnect.open.response", packet.type, "Packet type should be response")
        assertEquals(true, packet.body["success"], "Success should be true")
        assertNull(packet.body["error"], "No error message should be present")
    }

    @Test
    fun testCreateOpenResponse_FailureWithError() {
        val errorMsg = "File not found"
        val packet = OpenPacketsFFI.createOpenResponse(success = false, error = errorMsg)

        assertEquals(false, packet.body["success"], "Success should be false")
        assertEquals(errorMsg, packet.body["error"], "Error message should match")
    }

    // ========================================================================
    // Packet Creation Tests - Capabilities
    // ========================================================================

    @Test
    fun testCreateCapabilityAnnouncement_Default() {
        val schemes = listOf("http", "https", "mailto")
        val packet = OpenPacketsFFI.createCapabilityAnnouncement(schemes)

        assertEquals(
            "cconnect.open.capability",
            packet.type,
            "Packet type should be capability"
        )
        assertEquals(schemes, packet.body["supported_schemes"], "Schemes should match")
        assertEquals(true, packet.body["can_open_files"], "Default can_open_files should be true")
        assertEquals(true, packet.body["can_open_text"], "Default can_open_text should be true")
    }

    @Test
    fun testCreateCapabilityAnnouncement_CustomCapabilities() {
        val schemes = listOf("http", "https")
        val packet = OpenPacketsFFI.createCapabilityAnnouncement(
            supportedSchemes = schemes,
            canOpenFiles = false,
            canOpenText = true
        )

        assertEquals(false, packet.body["can_open_files"], "Custom can_open_files should be false")
        assertEquals(true, packet.body["can_open_text"], "Custom can_open_text should be true")
    }

    // ========================================================================
    // Extension Property Tests
    // ========================================================================

    @Test
    fun testExtensionProperties_UrlPacket() {
        val url = "https://example.com"
        val packet = OpenPacketsFFI.createUrlOpenRequest(url)

        assertTrue(packet.isOpenRequest, "isOpenRequest should be true")
        assertFalse(packet.type == "cconnect.open.response", "Should not be response")
        assertFalse(packet.type == "cconnect.open.capability", "Should not be capability")
        assertEquals(url, packet.openUrl, "openUrl should extract URL")
        assertNull(packet.openFileUri, "openFileUri should be null for URL packet")
        assertNull(packet.openText, "openText should be null for URL packet")
    }

    @Test
    fun testExtensionProperties_FilePacket() {
        val uri = "content://media/123"
        val mimeType = "application/pdf"
        val packet = OpenPacketsFFI.createFileOpenRequest(uri, mimeType)

        assertTrue(packet.isOpenRequest, "isOpenRequest should be true")
        assertEquals(uri, packet.openFileUri, "openFileUri should extract URI")
        assertEquals(mimeType, packet.openMimeType, "openMimeType should extract MIME type")
        assertNull(packet.openUrl, "openUrl should be null for file packet")
        assertNull(packet.openText, "openText should be null for file packet")
    }

    @Test
    fun testExtensionProperties_TextPacket() {
        val text = "Hello World"
        val packet = OpenPacketsFFI.createTextOpenRequest(text)

        assertTrue(packet.isOpenRequest, "isOpenRequest should be true")
        assertEquals(text, packet.openText, "openText should extract text")
        assertNull(packet.openUrl, "openUrl should be null for text packet")
        assertNull(packet.openFileUri, "openFileUri should be null for text packet")
    }

    // ========================================================================
    // Plugin Metadata Tests
    // ========================================================================

    @Test
    fun testPluginMetadata_DisplayName() {
        assertNotNull(plugin.displayName, "Display name should not be null")
        assertTrue(plugin.displayName.isNotEmpty(), "Display name should not be empty")
    }

    @Test
    fun testPluginMetadata_Description() {
        assertNotNull(plugin.description, "Description should not be null")
        assertTrue(plugin.description.isNotEmpty(), "Description should not be empty")
    }

    @Test
    fun testPluginMetadata_SupportedPacketTypes() {
        val supportedTypes = plugin.supportedPacketTypes

        assertTrue(
            OpenOnDesktopPlugin.PACKET_TYPE_OPEN_RESPONSE in supportedTypes,
            "Should support response packets"
        )
        assertTrue(
            OpenOnDesktopPlugin.PACKET_TYPE_OPEN_CAPABILITY in supportedTypes,
            "Should support capability packets"
        )
        assertEquals(2, supportedTypes.size, "Should support exactly 2 packet types")
    }

    @Test
    fun testPluginMetadata_OutgoingPacketTypes() {
        val outgoingTypes = plugin.outgoingPacketTypes

        assertTrue(
            OpenOnDesktopPlugin.PACKET_TYPE_OPEN_REQUEST in outgoingTypes,
            "Should send request packets"
        )
        assertTrue(
            OpenOnDesktopPlugin.PACKET_TYPE_OPEN_CAPABILITY in outgoingTypes,
            "Should send capability packets"
        )
        assertEquals(2, outgoingTypes.size, "Should send exactly 2 packet types")
    }

    // ========================================================================
    // Security Edge Case Tests
    // ========================================================================

    @Test
    fun testSecurity_MixedCaseScheme() {
        assertTrue(
            plugin.validateUrl("HtTpS://example.com"),
            "Mixed case schemes should be normalized"
        )
    }

    @Test
    fun testSecurity_UrlWithUserInfo() {
        assertTrue(
            plugin.validateUrl("https://user:pass@example.com"),
            "URLs with user info should be allowed (desktop handles validation)"
        )
    }

    @Test
    fun testSecurity_UrlWithPort() {
        assertTrue(
            plugin.validateUrl("https://example.com:8080"),
            "URLs with ports should be allowed"
        )
    }

    @Test
    fun testSecurity_UrlWithFragment() {
        assertTrue(
            plugin.validateUrl("https://example.com#section"),
            "URLs with fragments should be allowed"
        )
    }

    @Test
    fun testSecurity_UrlWithQuery() {
        assertTrue(
            plugin.validateUrl("https://example.com?param=value"),
            "URLs with query parameters should be allowed"
        )
    }

    @Test
    fun testSecurity_FtpSchemeRejected() {
        assertFalse(
            plugin.validateUrl("ftp://example.com"),
            "FTP URLs should be rejected (not in allowed list)"
        )
    }

    @Test
    fun testSecurity_AboutSchemeRejected() {
        assertFalse(
            plugin.validateUrl("about:blank"),
            "About URLs should be rejected"
        )
    }
}
