/*
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 * SPDX-FileCopyrightText: 2025 COSMIC Connect Contributors
 */

package org.cosmic.cosmicconnect.Plugins.NotificationPlugin.links

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName

@DisplayName("DeepLinkHandler Tests")
class DeepLinkHandlerTest {

    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var handler: DeepLinkHandler

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        every { context.packageManager } returns packageManager

        handler = DeepLinkHandler(context)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("Can Handle URL")
    inner class CanHandleUrl {

        @Test
        fun `can handle http URL`() {
            setupResolveActivity("http://example.com")
            assertTrue(handler.canHandle("http://example.com"))
        }

        @Test
        fun `can handle https URL`() {
            setupResolveActivity("https://example.com")
            assertTrue(handler.canHandle("https://example.com"))
        }

        @Test
        fun `can handle mailto URL`() {
            setupResolveActivity("mailto:test@example.com")
            assertTrue(handler.canHandle("mailto:test@example.com"))
        }

        @Test
        fun `can handle tel URL`() {
            setupResolveActivity("tel:1234567890")
            assertTrue(handler.canHandle("tel:1234567890"))
        }

        @Test
        fun `can handle geo URL`() {
            setupResolveActivity("geo:0,0")
            assertTrue(handler.canHandle("geo:0,0"))
        }

        @Test
        fun `can handle custom scheme with resolver`() {
            setupResolveActivity("myapp://page")
            assertTrue(handler.canHandle("myapp://page"))
        }

        @Test
        fun `cannot handle javascript scheme`() {
            assertFalse(handler.canHandle("javascript:alert('xss')"))
        }

        @Test
        fun `cannot handle file scheme`() {
            assertFalse(handler.canHandle("file:///etc/passwd"))
        }

        @Test
        fun `cannot handle data scheme`() {
            assertFalse(handler.canHandle("data:text/html,<script>"))
        }

        @Test
        fun `cannot handle URL without resolver`() {
            every { packageManager.resolveActivity(any(), any<Int>()) } returns null
            assertFalse(handler.canHandle("unknown://scheme"))
        }

        @Test
        fun `cannot handle invalid URL`() {
            assertFalse(handler.canHandle("not a url"))
        }
    }

    @Nested
    @DisplayName("Get Intent for Link")
    inner class GetIntentForLink {

        @Test
        fun `create ACTION_VIEW intent for http URL`() {
            val intent = handler.getIntentForLink("http://example.com")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertEquals("http://example.com", intent?.data.toString())
            assertTrue(intent?.flags?.and(Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        }

        @Test
        fun `create ACTION_VIEW intent for https URL`() {
            val intent = handler.getIntentForLink("https://example.com")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertEquals("https://example.com", intent?.data.toString())
        }

        @Test
        fun `create ACTION_SENDTO intent for mailto`() {
            val intent = handler.getIntentForLink("mailto:test@example.com")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_SENDTO, intent?.action)
            assertEquals("mailto:test@example.com", intent?.data.toString())
        }

        @Test
        fun `create ACTION_DIAL intent for tel`() {
            val intent = handler.getIntentForLink("tel:1234567890")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_DIAL, intent?.action)
            assertEquals("tel:1234567890", intent?.data.toString())
        }

        @Test
        fun `convert callto to tel`() {
            val intent = handler.getIntentForLink("callto:1234567890")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_DIAL, intent?.action)
            assertTrue(intent?.data.toString().startsWith("tel:"))
        }

        @Test
        fun `create ACTION_VIEW intent for sms`() {
            val intent = handler.getIntentForLink("sms:1234567890")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertTrue(intent?.data.toString().startsWith("sms:"))
        }

        @Test
        fun `convert smsto to sms`() {
            val intent = handler.getIntentForLink("smsto:1234567890")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertTrue(intent?.data.toString().startsWith("sms:"))
        }

        @Test
        fun `create ACTION_VIEW intent for geo`() {
            val intent = handler.getIntentForLink("geo:37.7749,-122.4194")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertEquals("geo:37.7749,-122.4194", intent?.data.toString())
        }

        @Test
        fun `create ACTION_VIEW intent for market`() {
            val intent = handler.getIntentForLink("market://details?id=com.example.app")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertTrue(intent?.data.toString().startsWith("market://"))
        }

        @Test
        fun `create intent with read permission for content URI`() {
            val intent = handler.getIntentForLink("content://com.example.provider/file")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertTrue(intent?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        }

        @Test
        fun `create ACTION_VIEW intent for custom scheme`() {
            val intent = handler.getIntentForLink("myapp://page/123")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
            assertEquals("myapp://page/123", intent?.data.toString())
        }

        @Test
        fun `return null for javascript scheme`() {
            val intent = handler.getIntentForLink("javascript:alert('xss')")
            assertNull(intent)
        }

        @Test
        fun `return null for file scheme`() {
            val intent = handler.getIntentForLink("file:///etc/passwd")
            assertNull(intent)
        }

        @Test
        fun `return null for data scheme`() {
            val intent = handler.getIntentForLink("data:text/html,<script>")
            assertNull(intent)
        }

        @Test
        fun `return null for malformed URL`() {
            val intent = handler.getIntentForLink("not a valid url")
            assertNull(intent)
        }
    }

    @Nested
    @DisplayName("Handle Link")
    inner class HandleLink {

        @Test
        fun `launch activity for valid URL`() {
            setupResolveActivity("http://example.com")

            handler.handleLink("http://example.com")

            verify { context.startActivity(any()) }
        }

        @Test
        fun `do not launch for URL without resolver`() {
            every { packageManager.resolveActivity(any(), any<Int>()) } returns null

            handler.handleLink("unknown://scheme")

            verify(exactly = 0) { context.startActivity(any()) }
        }

        @Test
        fun `do not launch for invalid URL`() {
            handler.handleLink("not a url")

            verify(exactly = 0) { context.startActivity(any()) }
        }

        @Test
        fun `handle sendToDesktop parameter`() {
            setupResolveActivity("http://example.com")

            // Should not throw even with sendToDesktop=true
            handler.handleLink("http://example.com", sendToDesktop = true)

            verify { context.startActivity(any()) }
        }

        @Test
        fun `handle ActivityNotFoundException gracefully`() {
            every { context.startActivity(any()) } throws Exception("Activity not found")
            setupResolveActivity("http://example.com")

            // Should not throw
            assertDoesNotThrow {
                handler.handleLink("http://example.com")
            }
        }
    }

    @Nested
    @DisplayName("App Link Detection")
    inner class AppLinkDetection {

        @Test
        fun `detect http URL as app link with non-browser handler`() {
            val resolveInfo = createResolveInfo("com.example.app")
            every {
                packageManager.queryIntentActivities(any(), any<Int>())
            } returns listOf(resolveInfo)

            assertTrue(handler.isAppLink("http://example.com"))
        }

        @Test
        fun `detect https URL as app link with non-browser handler`() {
            val resolveInfo = createResolveInfo("com.example.app")
            every {
                packageManager.queryIntentActivities(any(), any<Int>())
            } returns listOf(resolveInfo)

            assertTrue(handler.isAppLink("https://example.com"))
        }

        @Test
        fun `not app link if only browser handlers`() {
            val resolveInfo = createResolveInfo("com.android.browser")
            every {
                packageManager.queryIntentActivities(any(), any<Int>())
            } returns listOf(resolveInfo)

            assertFalse(handler.isAppLink("http://example.com"))
        }

        @Test
        fun `not app link if only chrome handlers`() {
            val resolveInfo = createResolveInfo("com.android.chrome")
            every {
                packageManager.queryIntentActivities(any(), any<Int>())
            } returns listOf(resolveInfo)

            assertFalse(handler.isAppLink("http://example.com"))
        }

        @Test
        fun `not app link for non-http schemes`() {
            assertFalse(handler.isAppLink("mailto:test@example.com"))
            assertFalse(handler.isAppLink("tel:1234567890"))
            assertFalse(handler.isAppLink("myapp://page"))
        }

        @Test
        fun `not app link if no handlers`() {
            every {
                packageManager.queryIntentActivities(any(), any<Int>())
            } returns emptyList()

            assertFalse(handler.isAppLink("http://example.com"))
        }
    }

    @Nested
    @DisplayName("Handler Description")
    inner class HandlerDescription {

        @Test
        fun `get handler description for valid URL`() {
            val resolveInfo = createResolveInfo("com.example.app")
            setupResolveActivity("http://example.com", resolveInfo)

            val appInfo = mockk<ApplicationInfo>()
            every { packageManager.getApplicationInfo("com.example.app", any<Int>()) } returns appInfo
            every { packageManager.getApplicationLabel(appInfo) } returns "Example App"

            val description = handler.getLinkHandlerDescription("http://example.com")

            assertEquals("Example App", description)
        }

        @Test
        fun `return null for URL without handler`() {
            every { packageManager.resolveActivity(any(), any<Int>()) } returns null

            val description = handler.getLinkHandlerDescription("unknown://scheme")

            assertNull(description)
        }

        @Test
        fun `return null for invalid URL`() {
            val description = handler.getLinkHandlerDescription("not a url")

            assertNull(description)
        }

        @Test
        fun `handle exception getting description`() {
            val resolveInfo = createResolveInfo("com.example.app")
            setupResolveActivity("http://example.com", resolveInfo)

            every {
                packageManager.getApplicationInfo(any(), any<Int>())
            } throws Exception("Failed to get app info")

            val description = handler.getLinkHandlerDescription("http://example.com")

            assertNull(description)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `handle empty URL`() {
            assertFalse(handler.canHandle(""))
            assertNull(handler.getIntentForLink(""))
        }

        @Test
        fun `handle URL with special characters`() {
            setupResolveActivity("http://example.com/path?q=hello%20world")

            val intent = handler.getIntentForLink("http://example.com/path?q=hello%20world")

            assertNotNull(intent)
            assertEquals(Intent.ACTION_VIEW, intent?.action)
        }

        @Test
        fun `handle international URL`() {
            setupResolveActivity("http://例え.jp")

            val intent = handler.getIntentForLink("http://例え.jp")

            assertNotNull(intent)
        }

        @Test
        fun `handle URL with fragment`() {
            val intent = handler.getIntentForLink("http://example.com/page#section")

            assertNotNull(intent)
            assertEquals("http://example.com/page#section", intent?.data.toString())
        }

        @Test
        fun `handle URL with authentication`() {
            val intent = handler.getIntentForLink("http://user:pass@example.com")

            assertNotNull(intent)
            assertTrue(intent?.data.toString().contains("example.com"))
        }
    }

    // Helper functions

    private fun setupResolveActivity(url: String, resolveInfo: ResolveInfo? = createResolveInfo("com.example.app")) {
        every {
            packageManager.resolveActivity(any(), any<Int>())
        } returns resolveInfo
    }

    private fun createResolveInfo(packageName: String): ResolveInfo {
        return ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.MainActivity"
            }
        }
    }
}
