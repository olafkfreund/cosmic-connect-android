/*
 * SPDX-FileCopyrightText: 2016 Saikrishna Arcot <saiarcot895@gmail.com>
 * SPDX-FileCopyrightText: 2024 Rob Emery <git@mintsoft.net>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/
package org.cosmicext.connect.Backends.BluetoothBackend

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import org.json.JSONException
import org.cosmicext.connect.Backends.BaseLink
import org.cosmicext.connect.Core.NetworkPacket as CoreNetworkPacket
import org.cosmicext.connect.Core.Payload as CorePayload
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.DeviceInfo
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.Reader
import java.util.UUID
import kotlin.text.Charsets.UTF_8

class BluetoothLink(
    context: Context,
    private val connection: ConnectionMultiplexer,
    val input: InputStream,
    val output: OutputStream,
    val remoteAddress: BluetoothDevice,
    private val theDeviceInfo: DeviceInfo,
    private val bluetoothLinkProvider: BluetoothLinkProvider
) : BaseLink(context, bluetoothLinkProvider) {
    private var continueAccepting = true
    private val receivingThread = Thread(object : Runnable {
        override fun run() {
            val sb = StringBuilder()
            try {
                val reader: Reader = InputStreamReader(input, UTF_8)
                val buf = CharArray(512)
                while (continueAccepting) {
                    while (sb.indexOf("\n") == -1 && continueAccepting) {
                        var charsRead: Int
                        if (reader.read(buf).also { charsRead = it } > 0) {
                            sb.append(buf, 0, charsRead)
                        }
                        if (charsRead < 0) {
                            disconnect()
                            return
                        }
                    }
                    if (!continueAccepting) break
                    val endIndex = sb.indexOf("\n")
                    if (endIndex != -1) {
                        val message = sb.substring(0, endIndex + 1)
                        sb.delete(0, endIndex + 1)
                        processMessage(message)
                    }
                }
            } catch (e: IOException) {
                Log.e("BluetoothLink/receiving", "Connection to " + remoteAddress.address + " likely broken.", e)
                disconnect()
            }
        }

        private fun processMessage(message: String) {
            val corePacket = try {
                CoreNetworkPacket.deserializeKotlin(message)
            } catch (e: JSONException) {
                Log.e("BluetoothLink/receiving", "Unable to parse message.", e)
                return
            }
            val transferPacket = if (corePacket.hasPayload) {
                try {
                    val transferUuid = UUID.fromString(corePacket.payloadTransferInfo["uuid"] as? String)
                    val payloadInputStream = connection.getChannelInputStream(transferUuid)
                    val payload = CorePayload(payloadInputStream, corePacket.payloadSize ?: 0)
                    TransferPacket(corePacket, payload)
                } catch (e: Exception) {
                    Log.e("BluetoothLink/receiving", "Unable to get payload", e)
                    TransferPacket(corePacket)
                }
            } else {
                TransferPacket(corePacket)
            }
            packetReceived(transferPacket)
        }
    })

    fun startListening() {
        receivingThread.start()
    }

    override val name: String = "BluetoothLink"

    override val deviceInfo: DeviceInfo = theDeviceInfo

    override fun disconnect() {
        continueAccepting = false
        try {
            connection.close()
        } catch (_: IOException) {
        }
        bluetoothLinkProvider.disconnectedLink(this, remoteAddress)
    }

    @WorkerThread
    @Throws(IOException::class)
    override fun sendTransferPacket(
        tp: TransferPacket,
        callback: Device.SendPacketStatusCallback,
        sendPayloadFromSameThread: Boolean
    ): Boolean {
        // sendPayloadFromSameThread is ignored, we always send from the same thread!

        return try {
            var transferUuid: UUID? = null
            if (tp.hasPayload) {
                transferUuid = connection.newChannel()
                tp.runtimeTransferInfo = mapOf("uuid" to transferUuid.toString())
            }
            // Serialize and send using Core serialization
            val message = tp.serializeForWire().toByteArray(UTF_8)
            output.write(message)
            if (transferUuid != null) {
                try {
                    connection.getChannelOutputStream(transferUuid).use { payloadStream ->
                        val BUFFER_LENGTH = 1024
                        val buffer = ByteArray(BUFFER_LENGTH)
                        var bytesRead: Int
                        var progress: Long = 0
                        val stream = tp.payload?.inputStream
                            ?: throw IOException("Payload input stream is null")
                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            progress += bytesRead.toLong()
                            payloadStream.write(buffer, 0, bytesRead)
                            if (tp.payloadSize > 0) {
                                callback.onPayloadProgressChanged((100 * progress / tp.payloadSize).toInt())
                            }
                        }
                        payloadStream.flush()
                    }
                } catch (e: Exception) {
                    callback.onFailure(e)
                    return false
                }
            }
            callback.onSuccess()
            true
        } catch (e: Exception) {
            callback.onFailure(e)
            false
        }
    }
}
