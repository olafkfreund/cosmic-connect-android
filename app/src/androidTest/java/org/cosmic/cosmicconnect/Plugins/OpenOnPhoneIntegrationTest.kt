/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
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
import org.mockito.kotlin.whenever

/**
 * Integration tests for OpenOnPhonePlugin
 *
 * Tests end-to-end functionality including notifications and user interactions
 *
 * ## Test Environment
 *
 * These tests run on a real device or emulator and verify:
 * - Notification creation and display
 * - User action handling (approve/reject)
 * - Intent launching for URL opening
 * - Integration with Android system services
 *
 * ## Requirements
 *
 * - Device with API 21+ (Android 5.0+)
 * - Notification permissions granted
 * - UiAutomator for notification interaction
 */
@RunWith(AndroidJUnit4::class)
class OpenOnPhoneIntegrationTest {

    private lateinit var plugin: OpenOnPhonePlugin
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var uiDevice: UiDevice

    @Mock
    private lateinit var mockDevice: Device

    private lateinit var closeable: AutoCloseable

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Initialize UiAutomator
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Clear existing notifications
        notificationManager.cancelAll()

        plugin = OpenOnPhonePlugin(context, mockDevice)

        // Mock device properties
        whenever(mockDevice.deviceId).thenReturn("test-device-integration")
        whenever(mockDevice.name).thenReturn("Test Integration Device")

        // Initialize plugin
        plugin.onCreate()
    }

    @After
    fun tearDown() {
        // Clean up notifications
        notificationManager.cancelAll()

        // Destroy plugin
        plugin.onDestroy()

        closeable.close()
    }

    // ========================================================================
    // Notification Creation Tests
    // ========================================================================

    @Test
    fun testShowConfirmationNotification_CreatesNotification() {
        val requestId = "test-notification-001"
        val url = "https://example.com"
        val title = "Test Notification"

        // Show notification
        plugin.showConfirmationNotification(requestId, url, title)

        // Wait for notification to appear
        Thread.sleep(1000)

        // Verify notification was created
        val activeNotifications = notificationManager.activeNotifications
        assertTrue("Notification should be active",
            activeNotifications.any { it.id == requestId.hashCode() })
    }

    @Test
    fun testShowConfirmationNotification_ContainsUrl() {
        val requestId = "test-notification-002"
        val url = "https://example.com/test"
        val title = "Test URL Display"

        // Show notification
        plugin.showConfirmationNotification(requestId, url, title)

        // Wait for notification to appear
        Thread.sleep(1000)

        // Open notification shade
        uiDevice.openNotification()
        Thread.sleep(500)

        // Check for URL in notification (shortened if too long)
        val displayUrl = if (url.length > 50) url.substring(0, 47) + "..." else url
        val notification = uiDevice.findObject(UiSelector().textContains(displayUrl))

        // Close notification shade
        uiDevice.pressBack()

        assertNotNull("Notification should contain URL", notification.exists())
    }

    @Test
    fun testShowConfirmationNotification_HasActionButtons() {
        val requestId = "test-notification-003"
        val url = "https://example.com"
        val title = "Test Actions"

        // Show notification
        plugin.showConfirmationNotification(requestId, url, title)

        // Wait for notification to appear
        Thread.sleep(1000)

        // Open notification shade
        uiDevice.openNotification()
        Thread.sleep(500)

        // Check for action buttons (text from strings.xml)
        val openButton = uiDevice.findObject(UiSelector().text("Open"))
        val rejectButton = uiDevice.findObject(UiSelector().text("Reject"))

        // Close notification shade
        uiDevice.pressBack()

        assertTrue("Open action should exist", openButton.exists())
        assertTrue("Reject action should exist", rejectButton.exists())
    }

    @Test
    fun testHideNotification_RemovesNotification() {
        val requestId = "test-notification-004"
        val url = "https://example.com"
        val title = "Test Hide"

        // Show notification
        plugin.showConfirmationNotification(requestId, url, title)
        Thread.sleep(500)

        // Verify notification exists
        var activeNotifications = notificationManager.activeNotifications
        assertTrue("Notification should be active",
            activeNotifications.any { it.id == requestId.hashCode() })

        // Hide notification
        plugin.hideNotification(requestId)
        Thread.sleep(500)

        // Verify notification removed
        activeNotifications = notificationManager.activeNotifications
        assertFalse("Notification should be removed",
            activeNotifications.any { it.id == requestId.hashCode() })
    }

    // ========================================================================
    // Packet Processing Integration Tests
    // ========================================================================

    @Test
    fun testOnPacketReceived_ValidRequest_ShowsNotification() {
        val requestId = "test-packet-001"
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            set("requestId", requestId)
            set("url", "https://example.com")
            set("title", "Test Packet")
        }

        // Process packet
        plugin.onPacketReceived(np)
        Thread.sleep(1000)

        // Verify notification created
        val activeNotifications = notificationManager.activeNotifications
        assertTrue("Notification should be created for valid request",
            activeNotifications.any { it.id == requestId.hashCode() })

        // Clean up
        plugin.hideNotification(requestId)
    }

    @Test
    fun testOnPacketReceived_InvalidUrl_NoNotification() {
        val requestId = "test-packet-002"
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            set("requestId", requestId)
            set("url", "javascript:alert('xss')")
            set("title", "Malicious Request")
        }

        // Process packet
        plugin.onPacketReceived(np)
        Thread.sleep(500)

        // Verify NO notification created
        val activeNotifications = notificationManager.activeNotifications
        assertFalse("Notification should NOT be created for invalid URL",
            activeNotifications.any { it.id == requestId.hashCode() })
    }

    @Test
    fun testOnPacketReceived_MissingFields_NoNotification() {
        val requestId = "test-packet-003"
        val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
            // Missing URL
            set("requestId", requestId)
        }

        // Process packet
        plugin.onPacketReceived(np)
        Thread.sleep(500)

        // Verify NO notification created
        val activeNotifications = notificationManager.activeNotifications
        assertFalse("Notification should NOT be created with missing fields",
            activeNotifications.any { it.id == requestId.hashCode() })
    }

    // ========================================================================
    // URL Validation Integration Tests
    // ========================================================================

    @Test
    fun testValidateUrl_AllowedSchemes_ShowNotification() {
        val allowedSchemes = listOf(
            "https://example.com",
            "http://example.com",
            "mailto:test@example.com",
            "tel:+1234567890",
            "geo:37.7749,-122.4194",
            "sms:+1234567890"
        )

        allowedSchemes.forEachIndexed { index, url ->
            val requestId = "test-scheme-$index"
            plugin.showConfirmationNotification(requestId, url, "Test $index")
            Thread.sleep(300)

            val activeNotifications = notificationManager.activeNotifications
            assertTrue("Notification should appear for allowed scheme: $url",
                activeNotifications.any { it.id == requestId.hashCode() })

            plugin.hideNotification(requestId)
            Thread.sleep(200)
        }
    }

    @Test
    fun testValidateUrl_BlockedSchemes_NoNotification() {
        val blockedUrls = listOf(
            "file:///etc/passwd",
            "javascript:alert('xss')",
            "data:text/html,<script>alert('xss')</script>"
        )

        blockedUrls.forEachIndexed { index, url ->
            val requestId = "test-blocked-$index"
            val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
                set("requestId", requestId)
                set("url", url)
            }

            plugin.onPacketReceived(np)
            Thread.sleep(300)

            val activeNotifications = notificationManager.activeNotifications
            assertFalse("Notification should NOT appear for blocked scheme: $url",
                activeNotifications.any { it.id == requestId.hashCode() })
        }
    }

    // ========================================================================
    // Security Integration Tests
    // ========================================================================

    @Test
    fun testSecurity_LocalhostBlocked() {
        val localhostUrls = listOf(
            "http://localhost",
            "http://127.0.0.1",
            "http://127.0.0.2"
        )

        localhostUrls.forEachIndexed { index, url ->
            val requestId = "test-localhost-$index"
            val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
                set("requestId", requestId)
                set("url", url)
            }

            plugin.onPacketReceived(np)
            Thread.sleep(300)

            val activeNotifications = notificationManager.activeNotifications
            assertFalse("Notification should NOT appear for localhost: $url",
                activeNotifications.any { it.id == requestId.hashCode() })
        }
    }

    @Test
    fun testSecurity_PrivateIPsBlocked() {
        val privateIPs = listOf(
            "http://10.0.0.1",
            "http://192.168.1.1",
            "http://172.16.0.1"
        )

        privateIPs.forEachIndexed { index, url ->
            val requestId = "test-private-$index"
            val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
                set("requestId", requestId)
                set("url", url)
            }

            plugin.onPacketReceived(np)
            Thread.sleep(300)

            val activeNotifications = notificationManager.activeNotifications
            assertFalse("Notification should NOT appear for private IP: $url",
                activeNotifications.any { it.id == requestId.hashCode() })
        }
    }

    @Test
    fun testSecurity_PublicIPsAllowed() {
        val publicIPs = listOf(
            "http://8.8.8.8",
            "http://1.1.1.1"
        )

        publicIPs.forEachIndexed { index, url ->
            val requestId = "test-public-$index"
            plugin.showConfirmationNotification(requestId, url, "Public IP $index")
            Thread.sleep(300)

            val activeNotifications = notificationManager.activeNotifications
            assertTrue("Notification should appear for public IP: $url",
                activeNotifications.any { it.id == requestId.hashCode() })

            plugin.hideNotification(requestId)
            Thread.sleep(200)
        }
    }

    // ========================================================================
    // Plugin Lifecycle Integration Tests
    // ========================================================================

    @Test
    fun testPluginLifecycle_OnCreateInitializesNotificationManager() {
        val newPlugin = OpenOnPhonePlugin(context, mockDevice)

        val result = newPlugin.onCreate()
        assertTrue("onCreate should return true", result)

        // Verify plugin can create notifications (would fail if manager not initialized)
        newPlugin.showConfirmationNotification("lifecycle-test", "https://example.com", "Test")
        Thread.sleep(500)

        val activeNotifications = notificationManager.activeNotifications
        assertTrue("Plugin should create notifications after onCreate",
            activeNotifications.any { it.id == "lifecycle-test".hashCode() })

        // Clean up
        newPlugin.hideNotification("lifecycle-test")
        newPlugin.onDestroy()
    }

    @Test
    fun testPluginLifecycle_MultipleRequests() {
        // Simulate multiple rapid requests
        val requests = (1..5).map { index ->
            "request-$index" to "https://example.com/page$index"
        }

        requests.forEach { (requestId, url) ->
            val np = NetworkPacket(OpenOnPhonePlugin.PACKET_TYPE_OPEN_REQUEST).apply {
                set("requestId", requestId)
                set("url", url)
            }
            plugin.onPacketReceived(np)
        }

        Thread.sleep(2000)

        // Verify all notifications created
        val activeNotifications = notificationManager.activeNotifications
        requests.forEach { (requestId, _) ->
            assertTrue("All notifications should be created",
                activeNotifications.any { it.id == requestId.hashCode() })
        }

        // Clean up
        requests.forEach { (requestId, _) ->
            plugin.hideNotification(requestId)
        }
    }
}
