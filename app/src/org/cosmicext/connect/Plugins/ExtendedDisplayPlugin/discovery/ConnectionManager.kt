/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.discovery

import android.content.Context
import android.content.SharedPreferences
import org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ConnectionMode

/**
 * Manages connection preferences and history for Extended Display.
 */
class ConnectionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Get the last used connection mode.
     */
    var lastConnectionMode: ConnectionMode
        get() {
            val name = prefs.getString(KEY_LAST_MODE, ConnectionMode.WIFI.name)
            return try {
                ConnectionMode.valueOf(name ?: ConnectionMode.WIFI.name)
            } catch (e: IllegalArgumentException) {
                ConnectionMode.WIFI
            }
        }
        set(value) {
            prefs.edit().putString(KEY_LAST_MODE, value.name).apply()
        }

    /**
     * Get the last connected server address.
     */
    var lastServerAddress: String
        get() = prefs.getString(KEY_LAST_ADDRESS, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_LAST_ADDRESS, value).apply()
        }

    /**
     * Get the last connected server port.
     */
    var lastServerPort: Int
        get() = prefs.getInt(KEY_LAST_PORT, DiscoveredService.DEFAULT_PORT)
        set(value) {
            prefs.edit().putInt(KEY_LAST_PORT, value).apply()
        }

    /**
     * Get the list of recent connections.
     */
    fun getRecentConnections(): List<DiscoveredService> {
        val json = prefs.getString(KEY_RECENT_CONNECTIONS, null) ?: return emptyList()

        return try {
            // Simple parsing: "name|host|port;name|host|port;..."
            json.split(";")
                .filter { it.isNotBlank() }
                .mapNotNull { entry ->
                    val parts = entry.split("|")
                    if (parts.size >= 3) {
                        DiscoveredService(
                            name = parts[0],
                            host = parts[1],
                            port = parts[2].toIntOrNull() ?: return@mapNotNull null
                        )
                    } else null
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Add a connection to recent history.
     */
    fun addRecentConnection(service: DiscoveredService) {
        val recent = getRecentConnections().toMutableList()

        // Remove duplicate if exists
        recent.removeAll { it.id == service.id }

        // Add to front
        recent.add(0, service)

        // Keep only last MAX entries
        while (recent.size > MAX_RECENT_CONNECTIONS) {
            recent.removeLast()
        }

        // Save
        val json = recent.joinToString(";") { "${it.name}|${it.host}|${it.port}" }
        prefs.edit().putString(KEY_RECENT_CONNECTIONS, json).apply()
    }

    /**
     * Clear all recent connections.
     */
    fun clearRecentConnections() {
        prefs.edit().remove(KEY_RECENT_CONNECTIONS).apply()
    }

    /**
     * Save the current connection as the last used.
     */
    fun saveLastConnection(mode: ConnectionMode, address: String, port: Int) {
        lastConnectionMode = mode
        lastServerAddress = address
        lastServerPort = port
    }

    companion object {
        private const val PREFS_NAME = "extended_display_prefs"
        private const val KEY_LAST_MODE = "last_connection_mode"
        private const val KEY_LAST_ADDRESS = "last_server_address"
        private const val KEY_LAST_PORT = "last_server_port"
        private const val KEY_RECENT_CONNECTIONS = "recent_connections"
        private const val MAX_RECENT_CONNECTIONS = 10
    }
}
