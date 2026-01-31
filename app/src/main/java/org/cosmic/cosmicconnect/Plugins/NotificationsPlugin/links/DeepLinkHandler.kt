/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.links

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import javax.inject.Inject

/**
 * Handles deep link and app link routing.
 *
 * Determines if a URL should be opened:
 * - In a specific app (deep link)
 * - In the browser
 * - Sent to desktop
 */
class DeepLinkHandler @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "COSMIC/DeepLinkHandler"

        // Common deep link domains
        private val DEEP_LINK_DOMAINS = setOf(
            "twitter.com",
            "x.com",
            "reddit.com",
            "youtube.com",
            "youtu.be",
            "spotify.com",
            "instagram.com",
            "facebook.com",
            "linkedin.com",
            "github.com",
            "play.google.com",
            "maps.google.com"
        )
    }

    /**
     * Analyze URL and determine handling strategy.
     *
     * @param url URL to analyze
     * @return LinkType indicating how to handle the URL
     */
    fun analyzeLinkType(url: String): LinkType {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return LinkType.WEB_URL

            // Check if there's an app that handles this URL
            if (canHandleDeepLink(uri)) {
                LinkType.APP_DEEP_LINK
            } else if (isCommonDeepLinkDomain(host)) {
                // Known deep link domain but app not installed
                LinkType.WEB_URL_WITH_APP_AVAILABLE
            } else {
                LinkType.WEB_URL
            }

        } catch (e: Exception) {
            Log.w(TAG, "Error analyzing link type for: $url", e)
            LinkType.WEB_URL
        }
    }

    /**
     * Check if any app can handle this deep link.
     *
     * @param uri URI to check
     * @return true if an app can handle this link
     */
    private fun canHandleDeepLink(uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val packageManager = context.packageManager

            // Check if any app can handle this intent
            val resolveInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            // Filter out browser apps - we want specific app handlers
            val nonBrowserHandlers = resolveInfo.filter { info ->
                !isBrowserApp(info.activityInfo.packageName)
            }

            nonBrowserHandlers.isNotEmpty()

        } catch (e: Exception) {
            Log.w(TAG, "Error checking deep link handlers", e)
            false
        }
    }

    /**
     * Check if package is a browser app.
     *
     * @param packageName Package to check
     * @return true if package is a known browser
     */
    private fun isBrowserApp(packageName: String): Boolean {
        val browserPackages = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.microsoft.emmx",
            "com.brave.browser",
            "org.chromium.chrome",
            "com.android.browser"
        )
        return browserPackages.contains(packageName)
    }

    /**
     * Check if domain is a known deep link domain.
     *
     * @param host Domain to check
     * @return true if domain commonly has deep links
     */
    private fun isCommonDeepLinkDomain(host: String): Boolean {
        // Check exact match
        if (DEEP_LINK_DOMAINS.contains(host)) {
            return true
        }

        // Check if subdomain of known domain
        for (domain in DEEP_LINK_DOMAINS) {
            if (host.endsWith(".$domain")) {
                return true
            }
        }

        return false
    }

    /**
     * Get app name that handles this URL.
     *
     * @param url URL to check
     * @return App name or null if no specific app handler
     */
    fun getHandlingAppName(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            val packageManager = context.packageManager

            val resolveInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }

            // Find first non-browser handler
            val handler = resolveInfo.firstOrNull { info ->
                !isBrowserApp(info.activityInfo.packageName)
            }

            handler?.loadLabel(packageManager)?.toString()

        } catch (e: Exception) {
            Log.w(TAG, "Error getting handling app name", e)
            null
        }
    }

    /**
     * Create intent to open URL.
     *
     * @param url URL to open
     * @param preferApp If true, prefer app over browser
     * @return Intent to open URL
     */
    fun createOpenIntent(url: String, preferApp: Boolean = true): Intent {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // If we don't prefer app, force browser
        if (!preferApp) {
            // Try to open in Chrome if available, else default browser
            try {
                intent.setPackage("com.android.chrome")
                val pm = context.packageManager
                if (pm.resolveActivity(intent, 0) == null) {
                    // Chrome not available, remove package constraint
                    intent.setPackage(null)
                }
            } catch (e: Exception) {
                // Ignore, will open in default handler
            }
        }

        return intent
    }
}

/**
 * Type of link detected.
 */
enum class LinkType {
    /** Regular web URL with no app handler */
    WEB_URL,

    /** URL that can be opened in a specific app (deep link) */
    APP_DEEP_LINK,

    /** Web URL for which an app is available but not installed */
    WEB_URL_WITH_APP_AVAILABLE
}
