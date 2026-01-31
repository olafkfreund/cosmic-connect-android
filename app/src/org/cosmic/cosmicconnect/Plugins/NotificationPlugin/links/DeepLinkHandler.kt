/*
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 * SPDX-FileCopyrightText: 2025 COSMIC Connect Contributors
 */

package org.cosmic.cosmicconnect.Plugins.NotificationPlugin.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles deep links and URL intents for Rich Notifications
 */
@Singleton
class DeepLinkHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeepLinkHandler"

        // Schemes that can be handled directly
        private val SUPPORTED_SCHEMES = setOf(
            "http", "https",
            "mailto",
            "tel", "callto",
            "geo",
            "market",
            "content",
            "sms", "smsto",
            "mms", "mmsto"
        )

        // Schemes that are definitely not safe
        private val BLOCKED_SCHEMES = setOf(
            "javascript",
            "file",
            "data",
            "vbscript",
            "about",
            "chrome"
        )
    }

    /**
     * Check if this handler can process the given URL
     */
    fun canHandle(url: String): Boolean {
        try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: return false

            // Block dangerous schemes
            if (scheme in BLOCKED_SCHEMES) {
                Log.w(TAG, "Blocked dangerous scheme: $scheme")
                return false
            }

            // Check if supported or has a handler
            if (scheme in SUPPORTED_SCHEMES) {
                return true
            }

            // For custom schemes, check if there's an app that can handle it
            val intent = getIntentForLink(url) ?: return false
            return intent.resolveActivity(context.packageManager) != null

        } catch (e: Exception) {
            Log.e(TAG, "Error checking if can handle URL: $url", e)
            return false
        }
    }

    /**
     * Handle a link by launching appropriate activity
     *
     * @param url The URL to handle
     * @param sendToDesktop If true, also send to desktop (future feature)
     */
    fun handleLink(url: String, sendToDesktop: Boolean = false) {
        try {
            val intent = getIntentForLink(url)
            if (intent == null) {
                Log.w(TAG, "No intent available for URL: $url")
                return
            }

            // Verify the intent can be resolved
            if (intent.resolveActivity(context.packageManager) == null) {
                Log.w(TAG, "No activity found to handle URL: $url")
                return
            }

            // Launch the intent
            context.startActivity(intent)
            Log.d(TAG, "Launched intent for URL: $url")

            // TODO: Phase 2 - Send to desktop if requested
            if (sendToDesktop) {
                Log.d(TAG, "Desktop sync not yet implemented")
            }

        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity found for URL: $url", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling URL: $url", e)
        }
    }

    /**
     * Get an Intent for launching the given link
     * Returns null if the URL is invalid or cannot be handled
     */
    fun getIntentForLink(url: String): Intent? {
        try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()

            // Validate scheme
            if (scheme == null || scheme in BLOCKED_SCHEMES) {
                Log.w(TAG, "Invalid or blocked scheme: $scheme")
                return null
            }

            val intent = when (scheme) {
                "http", "https" -> {
                    // Web URLs - use ACTION_VIEW
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "mailto" -> {
                    // Email - use ACTION_SENDTO
                    Intent(Intent.ACTION_SENDTO, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "tel", "callto" -> {
                    // Phone call - use ACTION_DIAL (doesn't require CALL_PHONE permission)
                    val telUri = if (scheme == "callto") {
                        // Convert callto: to tel:
                        Uri.parse("tel:${uri.schemeSpecificPart}")
                    } else {
                        uri
                    }
                    Intent(Intent.ACTION_DIAL, telUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "sms", "smsto" -> {
                    // SMS - use ACTION_VIEW
                    val smsUri = if (scheme == "smsto") {
                        Uri.parse("sms:${uri.schemeSpecificPart}")
                    } else {
                        uri
                    }
                    Intent(Intent.ACTION_VIEW, smsUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "mms", "mmsto" -> {
                    // MMS - use ACTION_VIEW
                    val mmsUri = if (scheme == "mmsto") {
                        Uri.parse("mms:${uri.schemeSpecificPart}")
                    } else {
                        uri
                    }
                    Intent(Intent.ACTION_VIEW, mmsUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "geo" -> {
                    // Maps/location - use ACTION_VIEW
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "market" -> {
                    // Google Play - use ACTION_VIEW
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }

                "content" -> {
                    // Content provider - use ACTION_VIEW
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }

                else -> {
                    // Custom scheme - try ACTION_VIEW
                    // This handles app deep links (e.g., twitter://, spotify://)
                    Intent(Intent.ACTION_VIEW, uri).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }

            return intent

        } catch (e: Exception) {
            Log.e(TAG, "Error creating intent for URL: $url", e)
            return null
        }
    }

    /**
     * Check if a URL is an Android App Link (verified deep link)
     */
    fun isAppLink(url: String): Boolean {
        try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()

            // App Links must use http/https
            if (scheme != "http" && scheme != "https") {
                return false
            }

            // Check if there's an app that handles this domain
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            val activities = context.packageManager.queryIntentActivities(
                intent,
                0
            )

            // If there's a specific app handler (not just browsers), it's likely an App Link
            return activities.any { activityInfo ->
                !activityInfo.activityInfo.packageName.contains("browser", ignoreCase = true) &&
                !activityInfo.activityInfo.packageName.contains("chrome", ignoreCase = true)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking if URL is app link: $url", e)
            return false
        }
    }

    /**
     * Get a user-friendly description of what will handle this link
     */
    fun getLinkHandlerDescription(url: String): String? {
        try {
            val intent = getIntentForLink(url) ?: return null
            val resolveInfo = intent.resolveActivity(context.packageManager) ?: return null

            return context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(
                    resolveInfo.packageName,
                    0
                )
            ).toString()

        } catch (e: Exception) {
            Log.e(TAG, "Error getting handler description for URL: $url", e)
            return null
        }
    }
}
