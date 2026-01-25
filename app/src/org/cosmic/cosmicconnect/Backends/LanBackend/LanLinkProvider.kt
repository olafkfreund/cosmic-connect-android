/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Backends.LanBackend

import android.content.Context
import android.net.Network
import android.util.Log
import android.util.Pair
import androidx.annotation.WorkerThread
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.DeviceHost
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.Helpers.ThreadHelper
import org.cosmic.cosmicconnect.Helpers.TrustedDevices
import org.cosmic.cosmicconnect.Helpers.TrustedNetworkHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.UserInterface.CustomDevicesActivity
import org.json.JSONException
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import javax.net.SocketFactory
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import kotlin.text.Charsets

/**
 * This LanLinkProvider creates [LanLink]s to other devices on the same
 * WiFi network. The first packet sent over a socket must be an
 * [DeviceInfo.toIdentityPacket].
 *
 * @see .identityPacketReceived
 */
class LanLinkProvider(private val context: Context) : BaseLinkProvider() {

    internal val visibleDevices = HashMap<String, LanLink>() // Links by device id

    private val lastConnectionTimeByDeviceId = ConcurrentHashMap<String, Long>()
    private val lastConnectionTimeByIp = ConcurrentHashMap<InetAddress, Long>()

    private var tcpServer: ServerSocket? = null
    private var udpServer: DatagramSocket? = null

    private val mdnsDiscovery = MdnsDiscovery(context, this)
    private val discoveryManager = DiscoveryManager(context, this)

    private var lastBroadcast: Long = 0

    private var listening = false

    override fun onConnectionLost(link: BaseLink) {
        val deviceId = link.deviceId
        visibleDevices.remove(deviceId)
        super.onConnectionLost(link)
    }

    private fun unserializeReceivedIdentityPacket(message: String): Pair<NetworkPacket, Boolean>? {
        val identityPacket: NetworkPacket
        try {
            identityPacket = NetworkPacket.unserialize(message)
        } catch (e: JSONException) {
            Log.w("KDE/LanLinkProvider", "Invalid identity packet received: " + e.message)
            return null
        }

        if (!DeviceInfo.isValidIdentityPacket(identityPacket)) {
            Log.w("KDE/LanLinkProvider", "Invalid identity packet received.")
            return null
        }

        val deviceId = identityPacket.getString("deviceId")
        val myId = DeviceHelper.getDeviceId(context)
        if (deviceId == myId) {
            //Ignore my own broadcast
            return null
        }

        if (rateLimitByDeviceId(deviceId)) {
            Log.i(
                "LanLinkProvider",
                "Discarding second packet from the same device $deviceId received too quickly"
            )
            return null
        }

        val deviceTrusted = TrustedDevices.isTrustedDevice(context, deviceId)
        if (!deviceTrusted && !TrustedNetworkHelper.isTrustedNetwork(context)) {
            Log.i(
                "KDE/LanLinkProvider",
                "Ignoring identity packet because the device is not trusted and I'm not on a trusted network."
            )
            return null
        }

        return Pair(identityPacket, deviceTrusted)
    }

    //They received my UDP broadcast and are connecting to me. The first thing they send should be their identity packet.
    @WorkerThread
    private fun tcpPacketReceived(socket: Socket) {
        val address = socket.inetAddress
        if (rateLimitByIp(address)) {
            Log.i(
                "LanLinkProvider",
                "Discarding second TCP packet from the same ip $address received too quickly"
            )
            return
        }

        val message: String
        try {
            message = readSingleLine(socket)
            //Log.e("TcpListener", "Received TCP packet: " + identityPacket.serialize());
        } catch (e: Exception) {
            Log.e("KDE/LanLinkProvider", "Exception while receiving TCP packet", e)
            return
        }

        val pair = unserializeReceivedIdentityPacket(message) ?: return
        val identityPacket = pair.first
        val deviceTrusted = pair.second

        Log.i(
            "KDE/LanLinkProvider",
            "identity packet received from a TCP connection from " + identityPacket.getString("deviceName")
        )

        val targetDeviceId = identityPacket.getStringOrNull("targetDeviceId")
        val targetProtocolVersion = identityPacket.getIntOrNull("targetProtocolVersion")
        if (targetDeviceId != null && targetDeviceId != DeviceHelper.getDeviceId(context)) {
            Log.e(
                "KDE/LanLinkProvider",
                "Received a connection request for a device that isn't me: $targetDeviceId"
            )
            return
        }
        if (targetProtocolVersion != null && targetProtocolVersion != DeviceHelper.PROTOCOL_VERSION) {
            Log.e(
                "KDE/LanLinkProvider",
                "Received a connection request for a protocol version that isn't mine: $targetProtocolVersion"
            )
            return
        }

        identityPacketReceived(
            identityPacket,
            socket,
            LanLink.ConnectionStarted.Locally,
            deviceTrusted
        )
    }

    /**
     * Read a single line from a socket without consuming anything else from the input.
     */
    @Throws(IOException::class)
    private fun readSingleLine(socket: Socket): String {
        val stream = socket.getInputStream()
        val line = StringBuilder(MAX_IDENTITY_PACKET_SIZE)
        var ch: Int
        while (stream.read().also { ch = it } != -1) {
            line.append(ch.toChar())
            if (ch == '\n'.code) {
                return line.toString()
            }
            if (line.length >= MAX_IDENTITY_PACKET_SIZE) {
                break
            }
        }
        throw IOException("Couldn't read a line from the socket")
    }

    private fun rateLimitByIp(address: InetAddress): Boolean {
        val now = System.currentTimeMillis()
        val last = lastConnectionTimeByIp[address]
        if (last != null && last + MILLIS_DELAY_BETWEEN_CONNECTIONS_TO_SAME_DEVICE > now) {
            return true
        }
        lastConnectionTimeByIp[address] = now
        if (lastConnectionTimeByIp.size > MAX_RATE_LIMIT_ENTRIES) {
            lastConnectionTimeByIp.entries.removeIf { e -> e.value + MILLIS_DELAY_BETWEEN_CONNECTIONS_TO_SAME_DEVICE < now }
        }
        return false
    }

    internal fun rateLimitByDeviceId(deviceId: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastConnectionTimeByDeviceId[deviceId]
        if (last != null && last + MILLIS_DELAY_BETWEEN_CONNECTIONS_TO_SAME_DEVICE > now) {
            return true
        }
        lastConnectionTimeByDeviceId[deviceId] = now
        if (lastConnectionTimeByDeviceId.size > MAX_RATE_LIMIT_ENTRIES) {
            lastConnectionTimeByDeviceId.entries.removeIf { e -> e.value + MILLIS_DELAY_BETWEEN_CONNECTIONS_TO_SAME_DEVICE < now }
        }
        return false
    }

    //I've received their broadcast and should connect to their TCP socket and send my identity.
    @WorkerThread
    private fun udpPacketReceived(packet: DatagramPacket) {
        val address = packet.address

        if (rateLimitByIp(address)) {
            Log.i(
                "LanLinkProvider",
                "Discarding second UDP packet from the same ip $address received too quickly"
            )
            return
        }

        val message = String(packet.data, 0, packet.length, Charsets.UTF_8)

        val pair = unserializeReceivedIdentityPacket(message) ?: return
        val identityPacket = pair.first
        val deviceTrusted = pair.second

        Log.i(
            "KDE/LanLinkProvider",
            "Broadcast identity packet received from " + identityPacket.getString("deviceName")
        )

        val tcpPort = identityPacket.getInt("tcpPort", MIN_PORT)
        if (tcpPort < MIN_PORT || tcpPort > MAX_PORT) {
            Log.e("LanLinkProvider", "TCP port outside of cosmicconnect's range")
            return
        }

        val socketFactory = SocketFactory.getDefault()
        val socket = socketFactory.createSocket(address, tcpPort)
        configureSocket(socket)

        val myDeviceInfo = DeviceHelper.getDeviceInfo(context)
        val myIdentity = myDeviceInfo.toIdentityPacket()
        myIdentity.set("targetDeviceId", identityPacket.getString("deviceId"))
        myIdentity.set("targetProtocolVersion", identityPacket.getString("protocolVersion"))
        val out = socket.getOutputStream()
        out.write(myIdentity.serialize().toByteArray())
        out.flush()

        identityPacketReceived(
            identityPacket,
            socket,
            LanLink.ConnectionStarted.Remotely,
            deviceTrusted
        )
    }

    private fun configureSocket(socket: Socket) {
        try {
            socket.keepAlive = true
        } catch (e: SocketException) {
            Log.e("LanLink", "Exception", e)
        }
    }

    /**
     * Called when a new 'identity' packet is received. Those are passed here by
     * [.tcpPacketReceived] and [.udpPacketReceived].
     * Should be called on a new thread since it blocks until the handshake is completed.
     *
     * @param identityPacket    identity of a remote device
     * @param socket            a new Socket, which should be used to receive packets from the remote device
     * @param connectionStarted which side started this connection
     * @param deviceTrusted     whether the packet comes from a trusted device
     */
    @WorkerThread
    @Throws(IOException::class)
    private fun identityPacketReceived(
        identityPacket: NetworkPacket,
        socket: Socket,
        connectionStarted: LanLink.ConnectionStarted,
        deviceTrusted: Boolean
    ) {
        val deviceId = identityPacket.getString("deviceId")
        val protocolVersion = identityPacket.getInt("protocolVersion")

        if (deviceTrusted && isProtocolDowngrade(deviceId, protocolVersion)) {
            Log.w(
                "KDE/LanLinkProvider",
                "Refusing to connect to a device using an older protocol version:$protocolVersion"
            )
            return
        }

        if (deviceTrusted && !TrustedDevices.isCertificateStored(context, deviceId)) {
            Log.e("KDE/LanLinkProvider", "Device trusted but no cert stored. This should not happen.")
            return
        }

        Log.i(
            "KDE/LanLinkProvider",
            "Starting SSL handshake with $deviceId trusted:$deviceTrusted"
        )

        // If I'm the TCP server I will be the SSL client and vice-versa.
        val clientMode = (connectionStarted == LanLink.ConnectionStarted.Locally)
        val sslSocket = SslHelper.convertToSslSocket(context, socket, deviceId, deviceTrusted, clientMode)
        sslSocket.addHandshakeCompletedListener { event ->
            // Start a new thread because some Android versions don't allow calling sslSocket.getOutputStream() from the callback
            ThreadHelper.execute {
                val mode = if (clientMode) "client" else "server"
                try {
                    val secureIdentityPacket: NetworkPacket
                    if (protocolVersion >= 8) {
                        val myDeviceInfo = DeviceHelper.getDeviceInfo(context)
                        val myIdentity = myDeviceInfo.toIdentityPacket()
                        val writer = sslSocket.outputStream
                        writer.write(myIdentity.serialize().toByteArray(Charsets.UTF_8))
                        writer.flush()
                        val line = readSingleLine(sslSocket)
                        // Do not trust the identity packet we received unencrypted
                        secureIdentityPacket = NetworkPacket.unserialize(line)
                        if (!DeviceInfo.isValidIdentityPacket(secureIdentityPacket)) {
                            Log.e("KDE/LanLinkProvider", "Identity packet isn't valid")
                            socket.close()
                            return@execute
                        }
                        val newProtocolVersion = secureIdentityPacket.getInt("protocolVersion")
                        if (newProtocolVersion != protocolVersion) {
                            Log.e(
                                "KDE/LanLinkProvider",
                                "Protocol version changed half-way through the handshake: $protocolVersion -> $newProtocolVersion"
                            )
                            socket.close()
                            return@execute
                        }
                        val newDeviceId = secureIdentityPacket.getString("deviceId")
                        if (newDeviceId != deviceId) {
                            Log.e(
                                "KDE/LanLinkProvider",
                                "Device ID changed half-way through the handshake: $deviceId -> $newDeviceId"
                            )
                            socket.close()
                            return@execute
                        }
                    } else {
                        secureIdentityPacket = identityPacket
                    }
                    val certificate = event.peerCertificates[0]
                    val deviceInfo = DeviceInfo.fromIdentityPacketAndCert(
                        secureIdentityPacket,
                        certificate
                    )
                    Log.i(
                        "KDE/LanLinkProvider",
                        "Handshake as " + mode + " successful with " + deviceInfo.name + " secured with " + event.cipherSuite
                    )
                    addOrUpdateLink(sslSocket, deviceInfo)
                } catch (e: JSONException) {
                    Log.e(
                        "KDE/LanLinkProvider",
                        "Remote device doesn't correctly implement protocol version 8",
                        e
                    )
                } catch (e: IOException) {
                    Log.e("KDE/LanLinkProvider", "Handshake as $mode failed with $deviceId", e)
                }
            }
        }

        //Handshake is blocking, so do it on another thread and free this thread to keep receiving new connection
        Log.d("LanLinkProvider", "Starting handshake")
        sslSocket.startHandshake()
        Log.d("LanLinkProvider", "Handshake done")
    }

    private fun isProtocolDowngrade(deviceId: String, protocolVersion: Int): Boolean {
        val lastKnownProtocolVersion = 
            DeviceInfo.loadProtocolVersionFromSettings(context, deviceId)
        return lastKnownProtocolVersion > protocolVersion
    }

    /**
     * Add or update a link in the [.visibleDevices] map.
     *
     * @param socket           a new Socket, which should be used to send and receive packets from the remote device
     * @param deviceInfo       remote device info
     * @throws IOException if an exception is thrown by [LanLink.reset]
     */
    @WorkerThread
    @Throws(IOException::class)
    private fun addOrUpdateLink(socket: SSLSocket, deviceInfo: DeviceInfo) {
        var link = visibleDevices[deviceInfo.id]
        if (link != null) {
            if (link.deviceInfo.certificate != deviceInfo.certificate) {
                Log.e(
                    "KDE/LanLinkProvider",
                    "LanLink was asked to replace a socket but the certificate doesn't match, aborting"
                )
                return
            }
            // Update existing link
            Log.d("KDE/LanLinkProvider", "Reusing same link for device " + deviceInfo.id)
            link.reset(socket, deviceInfo)
            onDeviceInfoUpdated(deviceInfo)
        } else {
            // Create a new link
            Log.d("KDE/LanLinkProvider", "Creating a new link for device " + deviceInfo.id)
            link = LanLink(context, deviceInfo, this, socket)
            visibleDevices[deviceInfo.id] = link
            onConnectionReceived(link)
        }
    }

    private fun setupUdpListener() {
        try {
            udpServer = DatagramSocket(null)
            udpServer?.reuseAddress = true
            udpServer?.broadcast = true
        } catch (e: SocketException) {
            Log.e("LanLinkProvider", "Error creating udp server", e)
            throw RuntimeException(e)
        }
        try {
            udpServer?.bind(InetSocketAddress(UDP_PORT))
        } catch (e: SocketException) {
            // We ignore this exception and continue without being able to receive broadcasts instead of crashing the app.
            Log.e(
                "LanLinkProvider",
                "Error binding udp server. We can send udp broadcasts but not receive them",
                e
            )
        }
        ThreadHelper.execute {
            Log.i("UdpListener", "Starting UDP listener")
            while (listening) {
                try {
                    val packet = DatagramPacket(
                        ByteArray(MAX_UDP_PACKET_SIZE),
                        MAX_UDP_PACKET_SIZE
                    )
                    udpServer!!.receive(packet)
                    ThreadHelper.execute {
                        try {
                            udpPacketReceived(packet)
                        } catch (e: JSONException) {
                            Log.e("LanLinkProvider", "Exception receiving incoming UDP connection", e)
                        } catch (e: IOException) {
                            Log.e("LanLinkProvider", "Exception receiving incoming UDP connection", e)
                        }
                    }
                } catch (e: IOException) {
                    Log.e("LanLinkProvider", "UdpReceive exception", e)
                    onNetworkChange(null) // Trigger a UDP broadcast to try to get them to connect to us instead
                }
            }
            Log.w("UdpListener", "Stopping UDP listener")
        }
    }

    private fun setupTcpListener() {
        try {
            tcpServer = openServerSocketOnFreePort(MIN_PORT)
        } catch (e: IOException) {
            Log.e("LanLinkProvider", "Error creating tcp server", e)
            throw RuntimeException(e)
        }
        ThreadHelper.execute {
            while (listening) {
                try {
                    val socket = tcpServer!!.accept()
                    configureSocket(socket)
                    ThreadHelper.execute {
                        try {
                            tcpPacketReceived(socket)
                        } catch (e: IOException) {
                            Log.e("LanLinkProvider", "Exception receiving incoming TCP connection", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LanLinkProvider", "TcpReceive exception", e)
                }
            }
            Log.w("TcpListener", "Stopping TCP listener")
        }
    }

    private fun broadcastUdpIdentityPacket(network: Network?) {
        ThreadHelper.execute {
            val hostList = CustomDevicesActivity
                .getCustomDeviceList(context)

            if (TrustedNetworkHelper.isTrustedNetwork(context)) {
                hostList.add(DeviceHost.BROADCAST) //Default: broadcast.
            } else {
                Log.i("LanLinkProvider", "Current network isn't trusted, not broadcasting")
            }

            val ipList = ArrayList<InetAddress>()
            for (host in hostList) {
                try {
                    ipList.add(InetAddress.getByName(host.toString()))
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                }
            }

            if (ipList.isEmpty()) {
                return@execute
            }

            sendUdpIdentityPacket(ipList, network)
        }
    }

    @WorkerThread
    fun sendUdpIdentityPacket(ipList: List<InetAddress>, network: Network?) {
        if (tcpServer == null || !tcpServer!!.isBound) {
            Log.i("LanLinkProvider", "Won't broadcast UDP packet if TCP socket is not ready yet")
            return
        }

        // TODO: In protocol version 8 this packet doesn't need to contain identity info
        //       since it will be exchanged after the socket is encrypted.
        val myDeviceInfo = DeviceHelper.getDeviceInfo(context)
        val identity = myDeviceInfo.toIdentityPacket()
        identity.set("tcpPort", tcpServer!!.localPort)

        val bytes: ByteArray
        try {
            bytes = identity.serialize().toByteArray(Charsets.UTF_8)
        } catch (e: JSONException) {
            Log.e("KDE/LanLinkProvider", "Failed to serialize identity packet", e)
            return
        }

        val socket: DatagramSocket
        try {
            socket = DatagramSocket()
            if (network != null) {
                try {
                    network.bindSocket(socket)
                } catch (e: IOException) {
                    Log.w("LanLinkProvider", "Couldn't bind socket to the network")
                    e.printStackTrace()
                }
            }
            socket.reuseAddress = true
            socket.broadcast = true
        } catch (e: SocketException) {
            Log.e("KDE/LanLinkProvider", "Failed to create DatagramSocket", e)
            return
        }

        for (ip in ipList) {
            try {
                socket.send(DatagramPacket(bytes, bytes.size, ip, MIN_PORT))
                //Log.i("KDE/LanLinkProvider","Udp identity packet sent to address "+client);
            } catch (e: IOException) {
                Log.e(
                    "KDE/LanLinkProvider",
                    "Sending udp identity packet failed. Invalid address? ($ip)",
                    e
                )
            }
        }

        socket.close()
    }

    override fun onStart() {
        //Log.i("KDE/LanLinkProvider", "onStart");
        if (!listening) {
            listening = true

            setupUdpListener()
            setupTcpListener()

            // Start FFI Discovery (replaces UDP broadcasts)
            try {
                discoveryManager.start()
            } catch (e: Exception) {
                Log.e("LanLinkProvider", "Failed to start FFI discovery", e)
            }

            mdnsDiscovery.startDiscovering()
            if (TrustedNetworkHelper.isTrustedNetwork(context)) {
                mdnsDiscovery.startAnnouncing()
            }

            // Keep UDP broadcast for backward compatibility with older devices
            broadcastUdpIdentityPacket(null)
        }
    }

    override fun onNetworkChange(network: Network?) {
        if (System.currentTimeMillis() < lastBroadcast + delayBetweenBroadcasts) {
            Log.i("LanLinkProvider", "onNetworkChange: relax cowboy")
            return
        }
        lastBroadcast = System.currentTimeMillis()

        // Restart FFI Discovery on network change
        try {
            discoveryManager.restart()
        } catch (e: Exception) {
            Log.e("LanLinkProvider", "Failed to restart FFI discovery", e)
        }

        broadcastUdpIdentityPacket(network)
        mdnsDiscovery.stopDiscovering()
        mdnsDiscovery.startDiscovering()
    }

    override fun onStop() {
        //Log.i("KDE/LanLinkProvider", "onStop");
        listening = false

        // Stop FFI Discovery
        try {
            discoveryManager.stop()
        } catch (e: Exception) {
            Log.e("LanLink", "Exception stopping FFI discovery", e)
        }

        mdnsDiscovery.stopAnnouncing()
        mdnsDiscovery.stopDiscovering()
        try {
            tcpServer?.close()
        } catch (e: Exception) {
            Log.e("LanLink", "Exception", e)
        }
        try {
            udpServer?.close()
        } catch (e: Exception) {
            Log.e("LanLink", "Exception", e)
        }
    }

    override val name: String
        get() = "LanLinkProvider"

    override val priority: Int
        get() = 20

    val tcpPort: Int
        get() = tcpServer!!.localPort

    companion object {
        const val UDP_PORT = 1716
        const val MIN_PORT = 1716
        const val MAX_PORT = 1764
        const val PAYLOAD_TRANSFER_MIN_PORT = 1739

        const val MAX_IDENTITY_PACKET_SIZE = 1024 * 512
        const val MAX_UDP_PACKET_SIZE = 1024 * 512

        const val MILLIS_DELAY_BETWEEN_CONNECTIONS_TO_SAME_DEVICE = 1000L

        const val MAX_RATE_LIMIT_ENTRIES = 255
        private const val delayBetweenBroadcasts: Long = 200

        @JvmStatic
        @Throws(IOException::class)
        fun openServerSocketOnFreePort(minPort: Int): ServerSocket {
            var tcpPort = minPort
            while (tcpPort <= MAX_PORT) {
                try {
                    val candidateServer = ServerSocket(tcpPort)
                    Log.i("KDE/LanLink", "Using port $tcpPort")
                    return candidateServer
                } catch (e: IOException) {
                    tcpPort++
                    if (tcpPort == MAX_PORT) {
                        Log.e("KDE/LanLink", "No ports available")
                        throw e //Propagate exception
                    }
                }
            }
            throw RuntimeException("This should not be reachable")
        }
    }
}
