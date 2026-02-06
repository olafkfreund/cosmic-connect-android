/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Backends.LanBackend

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import org.apache.commons.io.IOUtils
import org.cosmic.cosmicconnect.Backends.BaseLink
import org.cosmic.cosmicconnect.Backends.BaseLinkProvider
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.DeviceInfo
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.Helpers.ThreadHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.channels.NotYetConnectedException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocket
import kotlin.text.Charsets

class LanLink @WorkerThread constructor(
    context: Context,
    deviceInfo: DeviceInfo,
    linkProvider: BaseLinkProvider,
    socket: SSLSocket,
    private val sslHelper: SslHelper
) : BaseLink(context, linkProvider) {

    enum class ConnectionStarted {
        Locally, Remotely
    }

    override var deviceInfo: DeviceInfo = deviceInfo
        private set

    @Volatile
    private var socket: SSLSocket? = null

    init {
        reset(socket, deviceInfo)
    }

    override fun disconnect() {
        Log.i("LanLink/Disconnect", "socket:" + socket.hashCode())
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e("LanLink", "Error", e)
        }
    }

    //Returns the old socket
    @WorkerThread
    @Throws(IOException::class)
    fun reset(newSocket: SSLSocket, deviceInfo: DeviceInfo): SSLSocket? {
        this.deviceInfo = deviceInfo

        val oldSocket = socket
        socket = newSocket

        IOUtils.close(oldSocket) //This should cancel the readThread

        //Log.e("LanLink", "Start listening");
        //Create a thread to take care of incoming data for the new socket
        ThreadHelper.execute {
            try {
                val reader = BufferedReader(InputStreamReader(newSocket.inputStream, Charsets.UTF_8))
                while (true) {
                    val packet = try {
                        reader.readLine()
                    } catch (e: SocketTimeoutException) {
                        continue
                    }
                    if (packet == null) {
                        throw IOException("End of stream")
                    }
                    if (packet.isEmpty()) {
                        continue
                    }
                    val np = NetworkPacket.unserialize(packet)
                    Log.d("LanLink", "Received packet type: ${np.type} from ${deviceInfo?.name}")
                    receivedNetworkPacket(np)
                }
            } catch (e: Exception) {
                Log.i("LanLink", "Socket closed: " + newSocket.hashCode() + ". Reason: " + e.message)
                try {
                    Thread.sleep(300)
                } catch (ignored: InterruptedException) {
                } // Wait a bit because we might receive a new socket meanwhile
                val thereIsaANewSocket = newSocket != socket
                if (!thereIsaANewSocket) {
                    Log.i("LanLink", "Socket closed and there's no new socket, disconnecting device")
                    linkProvider.onConnectionLost(this@LanLink)
                }
            }
        }

        return oldSocket
    }

    override val name: String
        get() = "LanLink"

    @WorkerThread
    override fun sendPacket(
        np: NetworkPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean {
        val currentSocket = socket
        if (currentSocket == null) {
            Log.e("COSMIC/sendPacket", "Not yet connected")
            callback.onFailure(NotYetConnectedException())
            return false
        }

        try {
            //Prepare socket for the payload
            val server: ServerSocket?
            if (np.hasPayload()) {
                server = LanLinkProvider.openServerSocketOnFreePort(LanLinkProvider.PAYLOAD_TRANSFER_MIN_PORT)
                val payloadTransferInfo = JSONObject()
                payloadTransferInfo.put("port", server.localPort)
                np.payloadTransferInfo = payloadTransferInfo
            } else {
                server = null
            }

            //Log.e("LanLink/sendPacket", np.getType());

            //Send body of the network packet
            try {
                val writer = currentSocket.outputStream
                writer.write(np.serialize().toByteArray(Charsets.UTF_8))
                writer.flush()
            } catch (e: Exception) {
                disconnect() //main socket is broken, disconnect
                try {
                    server?.close()
                } catch (ignored: Exception) {
                }
                throw e
            }

            //Send payload
            if (server != null) {
                if (sendPayloadFromSameThread) {
                    sendPayload(np, callback, server)
                } else {
                    ThreadHelper.execute {
                        try {
                            sendPayload(np, callback, server)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e(
                                "LanLink/sendPacket",
                                "Async sendPayload failed for packet of type " + np.type + ". The Plugin was NOT notified."
                            )
                        }
                    }
                }
            }

            if (!np.isCanceled) {
                callback.onSuccess()
            }
            return true
        } catch (e: Exception) {
            callback.onFailure(e)
            return false
        } finally {
            //Make sure we close the payload stream, if any
            if (np.hasPayload()) {
                np.payload?.close()
            }
        }
    }

    /**
     * Send a TransferPacket using direct Core serialization — no legacy intermediate.
     *
     * This avoids the Map->JSONObject->String->JSONObject->String double-serialize path.
     * Instead: Core.NetworkPacket.serializeKotlin() produces wire JSON in one pass.
     */
    @WorkerThread
    override fun sendTransferPacket(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean {
        val currentSocket = socket
        if (currentSocket == null) {
            Log.e("COSMIC/sendTransferPacket", "Not yet connected")
            callback.onFailure(NotYetConnectedException())
            return false
        }

        try {
            // Prepare socket for the payload
            val server: ServerSocket?
            if (tp.hasPayload) {
                server = LanLinkProvider.openServerSocketOnFreePort(LanLinkProvider.PAYLOAD_TRANSFER_MIN_PORT)
                tp.runtimeTransferInfo = mapOf("port" to server.localPort)
            } else {
                server = null
            }

            // Send body — direct Core serialization, one JSON pass
            try {
                val writer = currentSocket.outputStream
                writer.write(tp.serializeForWire().toByteArray(Charsets.UTF_8))
                writer.flush()
            } catch (e: Exception) {
                disconnect()
                try {
                    server?.close()
                } catch (ignored: Exception) {
                }
                throw e
            }

            // Send payload
            if (server != null) {
                if (sendPayloadFromSameThread) {
                    sendTransferPayload(tp, callback, server)
                } else {
                    ThreadHelper.execute {
                        try {
                            sendTransferPayload(tp, callback, server)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e(
                                "LanLink/sendTransferPacket",
                                "Async sendPayload failed for packet of type ${tp.type}. The Plugin was NOT notified."
                            )
                        }
                    }
                }
            }

            if (!tp.isCanceled) {
                callback.onSuccess()
            }
            return true
        } catch (e: Exception) {
            callback.onFailure(e)
            return false
        } finally {
            if (tp.hasPayload) {
                tp.payload?.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun sendTransferPayload(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        server: ServerSocket
    ) {
        var payloadSocket: Socket? = null
        var outputStream: java.io.OutputStream? = null
        try {
            if (!tp.isCanceled) {
                server.soTimeout = 10 * 1000
                payloadSocket = server.accept()
                payloadSocket = sslHelper.convertToSslSocket(payloadSocket, deviceId, true, false)
                outputStream = payloadSocket.outputStream
                val inputStream = tp.payload?.inputStream

                Log.i("COSMIC/LanLink", "Beginning to send payload for ${tp.type}")
                val buffer = ByteArray(65536)
                var bytesRead: Int = -1
                val size = tp.payloadSize
                var progress: Long = 0
                var timeSinceLastUpdate: Long = -1
                while (!tp.isCanceled && inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                    progress += bytesRead.toLong()
                    outputStream.write(buffer, 0, bytesRead)
                    if (size > 0) {
                        if (timeSinceLastUpdate + 500 < System.currentTimeMillis()) {
                            val percent = 100 * progress / size
                            callback.onPayloadProgressChanged(percent.toInt())
                            timeSinceLastUpdate = System.currentTimeMillis()
                        }
                    }
                }
                outputStream.flush()
                Log.i("COSMIC/LanLink", "Finished sending payload ($progress bytes written)")
            }
        } catch (e: SocketTimeoutException) {
            Log.e("LanLink", "Socket for payload in packet ${tp.type} timed out.")
        } catch (e: SSLHandshakeException) {
            Log.e("sendTransferPacket", "Payload SSLSocket failed")
            e.printStackTrace()
        } finally {
            try { server.close() } catch (ignored: Exception) {}
            try { IOUtils.close(payloadSocket) } catch (ignored: Exception) {}
            tp.payload?.close()
            try { IOUtils.close(outputStream) } catch (ignored: Exception) {}
        }
    }

    @Throws(IOException::class)
    private fun sendPayload(
        np: NetworkPacket,
        callback: Device.SendPacketStatusCallback,
        server: ServerSocket
    ) {
        var payloadSocket: Socket? = null
        var outputStream: java.io.OutputStream? = null
        try {
            if (!np.isCanceled) {
                //Wait a maximum of 10 seconds for the other end to establish a connection with our socket, close it afterwards
                server.soTimeout = 10 * 1000

                payloadSocket = server.accept()

                //Convert to SSL if needed
                payloadSocket =
                    sslHelper.convertToSslSocket(payloadSocket, deviceId, true, false)

                outputStream = payloadSocket.outputStream
                val inputStream = np.payload?.inputStream

                Log.i("COSMIC/LanLink", "Beginning to send payload for " + np.type)
                val buffer = ByteArray(65536)
                var bytesRead: Int = -1
                val size = np.payloadSize
                var progress: Long = 0
                var timeSinceLastUpdate: Long = -1
                while (!np.isCanceled && inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                    //Log.e("ok",""+bytesRead);
                    progress += bytesRead.toLong()
                    outputStream.write(buffer, 0, bytesRead)
                    if (size > 0) {
                        if (timeSinceLastUpdate + 500 < System.currentTimeMillis()) { //Report progress every half a second
                            val percent = 100 * progress / size
                            callback.onPayloadProgressChanged(percent.toInt())
                            timeSinceLastUpdate = System.currentTimeMillis()
                        }
                    }
                }
                outputStream.flush()
                Log.i("COSMIC/LanLink", "Finished sending payload ($progress bytes written)")
            }
        } catch (e: SocketTimeoutException) {
            Log.e(
                "LanLink",
                "Socket for payload in packet " + np.type + " timed out. The other end didn't fetch the payload."
            )
        } catch (e: SSLHandshakeException) {
            // The exception can be due to several causes. "Connection closed by peer" seems to be a common one.
            // If we could distinguish different cases we could react differently for some of them, but I haven't found how.
            Log.e("sendPacket", "Payload SSLSocket failed")
            e.printStackTrace()
        } finally {
            try {
                server.close()
            } catch (ignored: Exception) {
            }
            try {
                IOUtils.close(payloadSocket)
            } catch (ignored: Exception) {
            }
            np.payload?.close()
            try {
                IOUtils.close(outputStream)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun receivedNetworkPacket(np: NetworkPacket) {
        if (np.hasPayloadTransferInfo()) {
            var payloadSocket = Socket()
            try {
                val tcpPort = np.payloadTransferInfo?.getInt("port") ?: throw Exception("No port")
                val deviceAddress = socket!!.remoteSocketAddress as InetSocketAddress
                payloadSocket.connect(InetSocketAddress(deviceAddress.address, tcpPort))
                payloadSocket =
                    sslHelper.convertToSslSocket(payloadSocket, deviceId, true, true)
                np.payload = NetworkPacket.Payload(payloadSocket, np.payloadSize)
            } catch (e: Exception) {
                try {
                    payloadSocket.close()
                } catch (ignored: Exception) {
                }
                Log.e("COSMIC/LanLink", "Exception connecting to payload remote socket", e)
            }
        }

        packetReceived(np)
    }
}