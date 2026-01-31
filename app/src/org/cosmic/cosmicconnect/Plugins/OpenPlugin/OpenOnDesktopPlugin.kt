/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Android Team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.OpenPlugin

import android.net.Uri
import android.util.Log
import android.widget.Toast
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import org.cosmic.cosmicconnect.R
import java.net.URL

/**
 * OpenOnDesktopPlugin - Send URLs, files, and text to COSMIC Desktop for opening
 *
 * This plugin implements the App Continuity feature by allowing Android to send
 * content to COSMIC Desktop for opening in appropriate applications.
 *
 * ## Security
 *
 * Only these URL schemes are allowed:
 * - http, https (web browsing)
 * - mailto (email)
 * - tel (phone calls)
 * - geo (maps/location)
 * - sms (messaging)
 */
@PluginFactory.LoadablePlugin
class OpenOnDesktopPlugin : Plugin() {

    companion object {
        private const val TAG = "OpenOnDesktopPlugin"

        const val PACKET_TYPE_OPEN_REQUEST = "cconnect.open.request"
        const val PACKET_TYPE_OPEN_RESPONSE = "cconnect.open.response"
        const val PACKET_TYPE_OPEN_CAPABILITY = "cconnect.open.capability"

        val ALLOWED_SCHEMES = setOf("http", "https", "mailto", "tel", "geo", "sms")
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_open_on_desktop)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_open_on_desktop_desc)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_OPEN_RESPONSE, PACKET_TYPE_OPEN_CAPABILITY)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_OPEN_REQUEST, PACKET_TYPE_OPEN_CAPABILITY)

    override fun onCreate(): Boolean {
        sendCapabilityAnnouncement()
        return true
    }

    /**
     * Send a URL to COSMIC Desktop for opening
     */
    fun sendUrlToDesktop(url: String): Boolean {
        if (!validateUrl(url)) {
            Log.w(TAG, "Rejected URL with disallowed scheme: $url")
            showToast(R.string.open_plugin_invalid_url)
            return false
        }

        Log.i(TAG, "Sending URL to desktop: $url")
        val packet = OpenPacketsFFI.createUrlOpenRequest(url, "browser")
        device.sendPacket(packet.toLegacyPacket())
        showToast(R.string.open_plugin_sent_url)
        return true
    }

    /**
     * Send a file URI to COSMIC Desktop for opening
     */
    fun sendFileToDesktop(uri: Uri, mimeType: String): Boolean {
        Log.i(TAG, "Sending file to desktop: $uri (type: $mimeType)")
        val packet = OpenPacketsFFI.createFileOpenRequest(uri.toString(), mimeType, "default")
        device.sendPacket(packet.toLegacyPacket())
        showToast(R.string.open_plugin_sent_file)
        return true
    }

    /**
     * Send text content to COSMIC Desktop for opening
     */
    fun sendTextToDesktop(text: String): Boolean {
        if (text.isBlank()) {
            Log.w(TAG, "Rejected empty text content")
            return false
        }

        Log.i(TAG, "Sending text to desktop (${text.length} chars)")
        val packet = OpenPacketsFFI.createTextOpenRequest(text, "editor")
        device.sendPacket(packet.toLegacyPacket())
        showToast(R.string.open_plugin_sent_text)
        return true
    }

    /**
     * Validate URL scheme against allowed list
     */
    fun validateUrl(url: String): Boolean {
        return try {
            val parsedUrl = URL(url)
            val scheme = parsedUrl.protocol.lowercase()
            val isAllowed = scheme in ALLOWED_SCHEMES

            if (!isAllowed) {
                Log.w(TAG, "URL validation failed: scheme '$scheme' not in allowed list")
            }

            isAllowed
        } catch (e: Exception) {
            Log.w(TAG, "URL validation failed: invalid URL format - $url", e)
            false
        }
    }

    override fun onPacketReceived(np: org.cosmic.cosmicconnect.NetworkPacket): Boolean {
        val networkPacket = NetworkPacket.fromLegacy(np)

        return when {
            networkPacket.isOpenResponse -> handleOpenResponse(networkPacket)
            networkPacket.isOpenCapability -> handleCapabilityAnnouncement(networkPacket)
            else -> false
        }
    }

    private fun handleOpenResponse(packet: NetworkPacket): Boolean {
        val success = packet.openSuccess ?: false
        val error = packet.openError

        if (success) {
            Log.i(TAG, "Desktop opened content successfully")
            showToast(R.string.open_plugin_success)
        } else {
            Log.w(TAG, "Desktop failed to open content: $error")
            showToast(R.string.open_plugin_error)
        }

        return true
    }

    private fun handleCapabilityAnnouncement(packet: NetworkPacket): Boolean {
        val schemes = packet.openSupportedSchemes
        val canOpenFiles = packet.openCanOpenFiles ?: false
        val canOpenText = packet.openCanOpenText ?: false

        Log.i(TAG, "Desktop capabilities: schemes=$schemes, files=$canOpenFiles, text=$canOpenText")
        return true
    }

    private fun sendCapabilityAnnouncement() {
        val packet = OpenPacketsFFI.createCapabilityAnnouncement(
            supportedSchemes = ALLOWED_SCHEMES.toList(),
            canOpenFiles = true,
            canOpenText = true
        )

        device.sendPacket(packet.toLegacyPacket())
        Log.d(TAG, "Sent capability announcement to desktop")
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    }
}
