/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.discovery

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents a discovered Extended Display service via mDNS/NSD.
 *
 * @property name Human-readable service name (e.g., "Desktop-PC COSMIC Display")
 * @property host IP address or hostname
 * @property port TCP port number
 * @property attributes Additional service attributes from TXT records
 */
data class DiscoveredService(
    val name: String,
    val host: String,
    val port: Int,
    val attributes: Map<String, String> = emptyMap()
) : Parcelable {

    /**
     * Returns a unique identifier for this service.
     */
    val id: String
        get() = "$host:$port"

    /**
     * Returns a display string for UI presentation.
     */
    val displayName: String
        get() = "$name ($host:$port)"

    /**
     * Checks if this service has valid connection information.
     */
    val isValid: Boolean
        get() = host.isNotBlank() && port in 1..65535

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        mutableMapOf<String, String>().apply {
            val size = parcel.readInt()
            repeat(size) {
                val key = parcel.readString() ?: ""
                val value = parcel.readString() ?: ""
                put(key, value)
            }
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(host)
        parcel.writeInt(port)
        parcel.writeInt(attributes.size)
        attributes.forEach { (key, value) ->
            parcel.writeString(key)
            parcel.writeString(value)
        }
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DiscoveredService> {
        override fun createFromParcel(parcel: Parcel): DiscoveredService {
            return DiscoveredService(parcel)
        }

        override fun newArray(size: Int): Array<DiscoveredService?> {
            return arrayOfNulls(size)
        }

        /**
         * Creates a DiscoveredService from manual connection input.
         */
        fun fromManualEntry(host: String, port: Int, name: String = "Manual Connection"): DiscoveredService {
            return DiscoveredService(
                name = name,
                host = host.trim(),
                port = port,
                attributes = mapOf("source" to "manual")
            )
        }

        /**
         * Default port for COSMIC Display server.
         */
        const val DEFAULT_PORT = 5900
    }
}
