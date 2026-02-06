/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.BatteryPlugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.VisibleForTesting
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.NetworkPacket as LegacyNetworkPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.R

class BatteryPlugin(context: Context, device: Device) : Plugin(context, device) {
    // Track last sent battery state for change detection
    private var lastCharge: Int = -1
    private var lastCharging: Boolean = false
    private var lastThresholdEvent: Int = THRESHOLD_EVENT_NONE

    /**
     * The latest battery information about the linked device. Will be null if the linked device
     * has not sent us any such information yet.
     *
     *
     * See [DeviceBatteryInfo] for info on which fields we expect to find.
     *
     *
     * @return the most recent packet received from the remote device. May be null
     */
    var remoteBatteryInfo: DeviceBatteryInfo? = null
        private set

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_battery)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_battery_desc)

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        var wasLowBattery: Boolean = false // will trigger a low battery notification when the device is connected

        override fun onReceive(context: Context, batteryIntent: Intent) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            // Calculate current charge (use last known if unavailable)
            val currentCharge = if (level == -1) lastCharge else level * 100 / scale

            // Determine charging status (use last known if unavailable)
            val isCharging = if (plugged == -1) lastCharging else 0 != plugged

            // Determine threshold event based on battery state
            val thresholdEvent = when (batteryIntent.action) {
                Intent.ACTION_BATTERY_OKAY -> THRESHOLD_EVENT_NONE
                Intent.ACTION_BATTERY_LOW -> if (!wasLowBattery && !isCharging) {
                    THRESHOLD_EVENT_BATTERY_LOW
                } else {
                    THRESHOLD_EVENT_NONE
                }
                else -> THRESHOLD_EVENT_NONE
            }

            // Update wasLowBattery tracking
            wasLowBattery = when (batteryIntent.action) {
                Intent.ACTION_BATTERY_OKAY -> false
                Intent.ACTION_BATTERY_LOW -> true
                else -> wasLowBattery
            }

            // Check if battery state has changed
            if (isCharging != lastCharging || currentCharge != lastCharge || thresholdEvent != lastThresholdEvent) {
                // Create battery packet using FFI
                val packet = BatteryPacketsFFI.createBatteryPacket(
                    isCharging = isCharging,
                    currentCharge = currentCharge,
                    thresholdEvent = thresholdEvent
                )
                val legacyPacket = convertToLegacyPacket(packet)
                device.sendPacket(legacyPacket)

                // Update last known state
                lastCharge = currentCharge
                lastCharging = isCharging
                lastThresholdEvent = thresholdEvent
            }
        }
    }

    override fun onCreate(): Boolean {
        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        val currentState = context.registerReceiver(receiver, intentFilter)
        receiver.onReceive(context, currentState)
        return true
    }

    override fun onDestroy() {
        // It's okay to call this only once, even though we registered it for two filters
        context.unregisterReceiver(receiver)
    }

    override fun onPacketReceived(np: LegacyNetworkPacket): Boolean {
        // Convert to immutable NetworkPacket for type-safe inspection
        val packet = NetworkPacket.fromLegacyPacket(np)

        when {
            packet.isBatteryPacket -> {
                // Received battery status from remote device
                remoteBatteryInfo = DeviceBatteryInfo(
                    currentCharge = packet.batteryCurrentCharge ?: 0,
                    isCharging = packet.batteryIsCharging ?: false,
                    thresholdEvent = packet.batteryThresholdEvent ?: THRESHOLD_EVENT_NONE
                )
                device.onPluginsChanged()
                return true
            }
            packet.isBatteryRequest -> {
                // Remote device requested our battery status - send current state
                sendBatteryUpdate()
                return true
            }
            else -> return false
        }
    }

    override val supportedPacketTypes: Array<String> = arrayOf(PACKET_TYPE_BATTERY, PACKET_TYPE_BATTERY_REQUEST)

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_BATTERY)

    /**
     * Send current battery update to remote device
     */
    private fun sendBatteryUpdate() {
        if (lastCharge < 0) {
            // Battery state not yet known, skip
            return
        }

        val packet = BatteryPacketsFFI.createBatteryPacket(
            isCharging = lastCharging,
            currentCharge = lastCharge,
            thresholdEvent = lastThresholdEvent
        )
        val legacyPacket = convertToLegacyPacket(packet)
        device.sendPacket(legacyPacket)
    }

    /**
     * Convert immutable NetworkPacket to legacy NetworkPacket for sending
     */
    private fun convertToLegacyPacket(ffi: NetworkPacket): LegacyNetworkPacket {
        val legacy = LegacyNetworkPacket(ffi.type)

        // Copy all body fields
        ffi.body.forEach { (key, value) ->
            when (value) {
                is String -> legacy.set(key, value)
                is Int -> legacy.set(key, value)
                is Long -> legacy.set(key, value)
                is Boolean -> legacy.set(key, value)
                is Double -> legacy.set(key, value)
                else -> legacy.set(key, value.toString())
            }
        }

        return legacy
    }

    companion object {
        const val PACKET_TYPE_BATTERY = "cconnect.battery"
        const val PACKET_TYPE_BATTERY_REQUEST = "cconnect.battery.request"

        // keep these fields in sync with cosmicconnect-kded:BatteryPlugin.h:ThresholdBatteryEvent
        private const val THRESHOLD_EVENT_NONE = 0
        private const val THRESHOLD_EVENT_BATTERY_LOW = 1

        fun isLowBattery(info: DeviceBatteryInfo): Boolean {
            return info.thresholdEvent == THRESHOLD_EVENT_BATTERY_LOW
        }
    }
}
