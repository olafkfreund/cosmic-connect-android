/*
 * SPDX-FileCopyrightText: 2025 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmic.cosmicconnect

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLink.PacketReceiver
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.DeviceInfo.Companion.loadFromSettings
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.PairingHandler.PairingCallback
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.PluginFactory
import java.security.cert.Certificate
import java.util.Vector
import java.util.concurrent.ConcurrentMap

class Device : PacketReceiver, PacketSender {

    val context: Context
    private val deviceHelper: DeviceHelper
    private val pluginFactory: PluginFactory
    private val sslHelper: SslHelper

    @VisibleForTesting
    val deviceInfo: DeviceInfo

    internal val connectionManager: ConnectionManager
    internal val pluginManager: PluginManager
    internal val pairingManager: PairingManager

    @VisibleForTesting
    var pairingHandler: PairingHandler
        get() = pairingManager.pairingHandler
        private set(_) {} // kept for binary compat; setter is unused

    var supportedPlugins: List<String>
        private set

    val loadedPlugins: ConcurrentMap<String, Plugin>
        get() = pluginManager.loadedPlugins
    val pluginsWithoutPermissions: ConcurrentMap<String, Plugin>
        get() = pluginManager.pluginsWithoutPermissions
    val pluginsWithoutOptionalPermissions: ConcurrentMap<String, Plugin>
        get() = pluginManager.pluginsWithoutOptionalPermissions

    /**
     * Constructor for remembered, already-trusted devices.
     */
    internal constructor(context: Context, deviceId: String, deviceHelper: DeviceHelper, pluginFactory: PluginFactory, sslHelper: SslHelper) {
        this.context = context
        this.deviceHelper = deviceHelper
        this.pluginFactory = pluginFactory
        this.sslHelper = sslHelper
        this.deviceInfo = loadFromSettings(context, deviceId)
        this.supportedPlugins = Vector(pluginFactory.availablePlugins)
        this.pluginManager = createPluginManager()
        this.pairingManager = PairingManager(context, this, deviceInfo, sslHelper,
            onPluginsReload = { pluginManager.reloadPluginsFromSettings() },
            onPluginsUnpaired = { ctx, id -> pluginManager.notifyPluginsOfDeviceUnpaired(ctx, id) },
            initialState = PairingHandler.PairState.Paired
        )
        this.connectionManager = createConnectionManager()
        Log.i("Device", "Loading trusted device: ${deviceInfo.name}")
    }

    /**
     * Constructor for devices discovered but not trusted yet.
     */
    internal constructor(context: Context, link: BaseLink, deviceHelper: DeviceHelper, pluginFactory: PluginFactory, sslHelper: SslHelper) {
        this.context = context
        this.deviceHelper = deviceHelper
        this.pluginFactory = pluginFactory
        this.sslHelper = sslHelper
        this.deviceInfo = link.deviceInfo
        this.supportedPlugins = Vector(pluginFactory.availablePlugins)
        this.pluginManager = createPluginManager()
        this.pairingManager = PairingManager(context, this, deviceInfo, sslHelper,
            onPluginsReload = { pluginManager.reloadPluginsFromSettings() },
            onPluginsUnpaired = { ctx, id -> pluginManager.notifyPluginsOfDeviceUnpaired(ctx, id) },
            initialState = PairingHandler.PairState.NotPaired
        )
        this.connectionManager = createConnectionManager()
        Log.i("Device", "Creating untrusted device: " + deviceInfo.name)
        addLink(link)
    }

    private fun createConnectionManager(): ConnectionManager {
        return ConnectionManager(
            deviceId = deviceInfo.id,
            deviceName = { deviceInfo.name },
            onPairPacket = { np -> pairingManager.pairingHandler.packetReceived(np) },
            onDataPacket = { np ->
                if (pluginManager.pluginsByIncomingInterfaceEmpty) {
                    pluginManager.reloadPluginsFromSettings()
                }
                pluginManager.notifyPluginPacketReceived(np)
            },
            isPaired = { isPaired },
            onUnpair = { unpair() },
            onLinksChanged = { link ->
                val hasChanges = updateDeviceInfo(link.deviceInfo)
                if (hasChanges || connectionManager.linkCount == 1) {
                    pluginManager.reloadPluginsFromSettings()
                }
            },
            onLinksEmpty = { pluginManager.reloadPluginsFromSettings() }
        )
    }

    private fun createPluginManager(): PluginManager {
        return PluginManager(
            context = context,
            deviceId = deviceInfo.id,
            deviceName = { deviceInfo.name },
            pluginFactory = pluginFactory,
            device = this,
            isPaired = { isPaired },
            isReachable = { isReachable },
            getSupportedPlugins = { supportedPlugins }
        )
    }

    fun supportsPacketType(type: String): Boolean =
        deviceInfo.incomingCapabilities?.contains(type) ?: true

    fun interface PluginsChangedListener {
        fun onPluginsChanged(device: Device)
    }

    //
    // Device info properties
    //
    val connectivityType: String?
        get() = connectionManager.connectivityType

    val name: String
        get() = deviceInfo.name

    val icon: Drawable
        get() = deviceInfo.type.getIcon(context)

    val deviceType: DeviceType
        get() = deviceInfo.type

    val protocolVersion: Int
        get() = deviceInfo.protocolVersion

    val deviceId: String
        get() = deviceInfo.id

    val certificate: Certificate
        get() = deviceInfo.certificate

    val verificationKey: String?
        get() = pairingManager.verificationKey

    fun compareProtocolVersion(): Int =
        deviceInfo.protocolVersion - DeviceHelper.PROTOCOL_VERSION

    //
    // Pairing - delegated to PairingManager
    //
    val isPaired: Boolean
        get() = pairingManager.isPaired

    val pairStatus: PairingHandler.PairState
        get() = pairingManager.pairStatus

    fun addPairingCallback(callback: PairingCallback) = pairingManager.addPairingCallback(callback)

    fun removePairingCallback(callback: PairingCallback) = pairingManager.removePairingCallback(callback)

    fun requestPairing() = pairingManager.requestPairing()

    fun unpair() = pairingManager.unpair()

    fun acceptPairing() = pairingManager.acceptPairing()

    fun cancelPairing() = pairingManager.cancelPairing()

    fun displayPairingNotification() = pairingManager.displayPairingNotification()

    fun hidePairingNotification() = pairingManager.hidePairingNotification()

    //
    // Connection management - delegated to ConnectionManager
    //
    val isReachable: Boolean
        get() = connectionManager.isReachable

    fun hasLinkFromProvider(provider: BaseLinkProvider): Boolean =
        connectionManager.hasLinkFromProvider(provider)

    fun addLink(link: BaseLink) = connectionManager.addLink(link)

    @WorkerThread
    fun removeLink(link: BaseLink) = connectionManager.removeLink(link)

    fun disconnect() = connectionManager.disconnect()

    override fun onPacketReceived(np: NetworkPacket) = connectionManager.onPacketReceived(np)

    abstract class SendPacketStatusCallback {
        abstract fun onSuccess()
        abstract fun onFailure(e: Throwable)
        open fun onPayloadProgressChanged(percent: Int) {}
    }

    @AnyThread
    override fun sendPacket(np: NetworkPacket, callback: SendPacketStatusCallback) =
        connectionManager.sendPacket(np, callback)

    @AnyThread
    override fun sendPacket(np: NetworkPacket) =
        connectionManager.sendPacket(np)

    @WorkerThread
    override fun sendPacketBlocking(np: NetworkPacket, callback: SendPacketStatusCallback): Boolean =
        connectionManager.sendPacketBlocking(np, callback)

    @WorkerThread
    override fun sendPacketBlocking(np: NetworkPacket): Boolean =
        connectionManager.sendPacketBlocking(np)

    @WorkerThread
    override fun sendPacketBlocking(
        np: NetworkPacket,
        callback: SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean = connectionManager.sendPacketBlocking(np, callback, sendPayloadFromSameThread)

    // --- TransferPacket overloads ---

    @AnyThread
    override fun sendPacket(tp: TransferPacket, callback: SendPacketStatusCallback) =
        connectionManager.sendPacket(tp, callback)

    @AnyThread
    override fun sendPacket(tp: TransferPacket) =
        connectionManager.sendPacket(tp)

    @WorkerThread
    override fun sendPacketBlocking(tp: TransferPacket, callback: SendPacketStatusCallback): Boolean =
        connectionManager.sendPacketBlocking(tp, callback)

    @WorkerThread
    override fun sendPacketBlocking(tp: TransferPacket): Boolean =
        connectionManager.sendPacketBlocking(tp)

    @WorkerThread
    override fun sendPacketBlocking(
        tp: TransferPacket,
        callback: SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean = connectionManager.sendPacketBlocking(tp, callback, sendPayloadFromSameThread)

    //
    // Device info management
    //
    fun updateDeviceInfo(newDeviceInfo: DeviceInfo): Boolean {
        var hasChanges = false
        if (deviceInfo.name != newDeviceInfo.name || deviceInfo.type != newDeviceInfo.type || deviceInfo.protocolVersion != newDeviceInfo.protocolVersion) {
            hasChanges = true
            deviceInfo.name = newDeviceInfo.name
            deviceInfo.type = newDeviceInfo.type
            deviceInfo.protocolVersion = newDeviceInfo.protocolVersion
            if (isPaired) {
                deviceInfo.saveInSettings(context)
            }
        }

        val oldIncomingCapabilities = deviceInfo.incomingCapabilities
        val oldOutgoingCapabilities = deviceInfo.outgoingCapabilities
        val newIncomingCapabilities = newDeviceInfo.incomingCapabilities
        val newOutgoingCapabilities = newDeviceInfo.outgoingCapabilities
        if (
            !newIncomingCapabilities.isNullOrEmpty() &&
            !newOutgoingCapabilities.isNullOrEmpty() &&
            (
                oldIncomingCapabilities != newIncomingCapabilities ||
                oldOutgoingCapabilities != newOutgoingCapabilities
            )
        ) {
            hasChanges = true
            Log.i("updateDeviceInfo", "Updating supported plugins according to new capabilities")
            deviceInfo.outgoingCapabilities = newOutgoingCapabilities
            deviceInfo.incomingCapabilities = newIncomingCapabilities
            supportedPlugins = Vector(
                pluginFactory.pluginsForCapabilities(
                    newIncomingCapabilities,
                    newOutgoingCapabilities
                ) + setOf("CameraPlugin")
            )
        }

        return hasChanges
    }

    //
    // Plugin management - delegated to PluginManager
    //
    fun <T : Plugin> getPlugin(pluginClass: Class<T>): T? = pluginManager.getPlugin(pluginClass)

    fun getPlugin(pluginKey: String): Plugin? = pluginManager.getPlugin(pluginKey)

    fun getPluginIncludingWithoutPermissions(pluginKey: String): Plugin? =
        pluginManager.getPluginIncludingWithoutPermissions(pluginKey)

    fun setPluginEnabled(pluginKey: String, value: Boolean) = pluginManager.setPluginEnabled(pluginKey, value)

    fun isPluginEnabled(pluginKey: String): Boolean = pluginManager.isPluginEnabled(pluginKey)

    fun notifyPluginsOfDeviceUnpaired(context: Context, deviceId: String) =
        pluginManager.notifyPluginsOfDeviceUnpaired(context, deviceId)

    fun launchBackgroundReloadPluginsFromSettings() = pluginManager.launchBackgroundReloadPluginsFromSettings()

    @Synchronized
    @WorkerThread
    fun reloadPluginsFromSettings() = pluginManager.reloadPluginsFromSettings()

    fun onPluginsChanged() = pluginManager.onPluginsChanged()

    fun addPluginsChangedListener(listener: PluginsChangedListener) = pluginManager.addPluginsChangedListener(listener)

    fun removePluginsChangedListener(listener: PluginsChangedListener) = pluginManager.removePluginsChangedListener(listener)

    //
    // Identity
    //
    override fun toString(): String = "Device(name=$name, id=$deviceId)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Device) return false
        return deviceId == other.deviceId
    }

    override fun hashCode(): Int = deviceId.hashCode()
}
