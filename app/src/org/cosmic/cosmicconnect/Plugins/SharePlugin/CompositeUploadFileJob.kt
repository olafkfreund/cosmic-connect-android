/*
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.SharePlugin

import android.os.Handler
import android.os.Looper
import androidx.annotation.GuardedBy
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.async.BackgroundJob

/**
 * A type of [BackgroundJob] that sends Files to another device.
 *
 * We represent the individual upload requests as [NetworkPacket]s.
 * Each packet should have a 'filename' property and a payload. If the payload is
 * missing, we'll just send an empty file. You can add new packets anytime via
 * [.addNetworkPacket].
 *
 * The I/O-part of this file sending is handled by
 * [Device.sendPacketBlocking].
 *
 * @see CompositeReceiveFileJob
 * @see SendPacketStatusCallback
 */
class CompositeUploadFileJob(device: Device, callback: Callback<java.lang.Void?>) :
    BackgroundJob<Device, java.lang.Void?>(device, callback) {

    private var isRunningInternal = false
    private val handler = Handler(Looper.getMainLooper())
    private var currentFileName = ""
    private var currentFileNum = 0
    private var updatePacketPending = false
    private var totalSend: Long = 0
    private var prevProgressPercentage = 0
    private val uploadNotification: UploadNotification

    private val lock = Any() // Use to protect concurrent access to the variables below

    @GuardedBy("lock")
    private val networkPacketList: MutableList<NetworkPacket> = ArrayList()
    private var currentNetworkPacket: NetworkPacket? = null
    private val sendPacketStatusCallback: SendPacketStatusCallback
    @GuardedBy("lock")
    private var totalNumFiles = 0
    @GuardedBy("lock")
    private var totalPayloadSize: Long = 0

    init {
        isRunningInternal = false
        currentFileNum = 0
        currentFileName = ""
        updatePacketPending = false
        totalNumFiles = 0
        totalPayloadSize = 0
        totalSend = 0
        prevProgressPercentage = 0
        uploadNotification = UploadNotification(getDevice(), id)
        sendPacketStatusCallback = SendPacketStatusCallback()
    }

    private fun getDevice(): Device = requestInfo

    override fun run() {
        var done: Boolean

        isRunningInternal = true

        synchronized(lock) {
            done = networkPacketList.isEmpty()
        }

        try {
            while (!done && !isCancelled) {
                synchronized(lock) {
                    currentNetworkPacket = networkPacketList.removeAt(0)
                }

                currentNetworkPacket?.let { packet ->
                    currentFileName = packet.getString("filename")
                    currentFileNum++

                    setProgress(prevProgressPercentage)

                    addTotalsToNetworkPacket(packet)

                    // We set sendPayloadFromSameThread to true so this call blocks until the payload
                    // has been received by the other end, so payloads are sent one by one.
                    if (!getDevice().sendPacketBlocking(packet, sendPacketStatusCallback, true)) {
                        throw RuntimeException("Sending packet failed")
                    }
                }

                synchronized(lock) {
                    done = networkPacketList.isEmpty()
                }
            }

            if (isCancelled) {
                uploadNotification.cancel()
            } else {
                uploadNotification.setFinished(
                    getDevice().context.resources.getQuantityString(
                        R.plurals.sent_files_title,
                        currentFileNum,
                        getDevice().name,
                        currentFileNum
                    )
                )
                uploadNotification.show()

                reportResult(null)
            }
        } catch (e: RuntimeException) {
            val failedFiles: Int
            synchronized(lock) {
                failedFiles = totalNumFiles - currentFileNum + 1
                uploadNotification.setFailed(
                    getDevice().context.resources
                        .getQuantityString(
                            R.plurals.send_files_fail_title, failedFiles, getDevice().name,
                            failedFiles, totalNumFiles
                        )
                )
            }

            uploadNotification.show()
            reportError(e)
        } finally {
            isRunningInternal = false

            for (networkPacket in networkPacketList) {
                networkPacket.payload?.close()
            }
            networkPacketList.clear()
        }
    }

    private fun addTotalsToNetworkPacket(networkPacket: NetworkPacket) {
        synchronized(lock) {
            networkPacket.set(SharePlugin.KEY_NUMBER_OF_FILES, totalNumFiles)
            networkPacket.set(SharePlugin.KEY_TOTAL_PAYLOAD_SIZE, totalPayloadSize)
        }
    }

    private fun setProgress(progress: Int) {
        synchronized(lock) {
            uploadNotification.setProgress(
                progress, getDevice().context.resources
                    .getQuantityString(
                        R.plurals.outgoing_files_text,
                        totalNumFiles,
                        currentFileName,
                        currentFileNum,
                        totalNumFiles
                    )
            )
        }
        uploadNotification.show()
    }

    fun addNetworkPacket(networkPacket: NetworkPacket) {
        synchronized(lock) {
            networkPacketList.add(networkPacket)

            totalNumFiles++

            if (networkPacket.payloadSize >= 0) {
                totalPayloadSize += networkPacket.payloadSize
            }

            uploadNotification.setTitle(
                getDevice().context.resources
                    .getQuantityString(
                        R.plurals.outgoing_file_title,
                        totalNumFiles,
                        totalNumFiles,
                        getDevice().name
                    )
            )

            // Give SharePlugin some time to add more NetworkPackets
            if (isRunningInternal && !updatePacketPending) {
                updatePacketPending = true
                handler.post { sendUpdatePacket() }
            }
        }
    }

    fun isRunning(): Boolean = isRunningInternal

    /**
     * Use this to send metadata ahead of all the other [.networkPacketList] packets.
     */
    private fun sendUpdatePacket() {
        // Create legacy packet directly
        val packet = NetworkPacket(SharePlugin.PACKET_TYPE_SHARE_REQUEST_UPDATE)

        synchronized(lock) {
            packet.set("numberOfFiles", totalNumFiles)
            packet.set("totalPayloadSize", totalPayloadSize)
            updatePacketPending = false
        }

        // Send packet
        getDevice().sendPacket(packet)
    }

    override fun cancel() {
        super.cancel()
        currentNetworkPacket?.cancel()
    }

    private inner class SendPacketStatusCallback : Device.SendPacketStatusCallback() {
        override fun onPayloadProgressChanged(percent: Int) {
            currentNetworkPacket?.let { packet ->
                val send = totalSend + (packet.payloadSize * (percent.toFloat() / 100))
                val progress = if (totalPayloadSize > 0) ((send * 100) / totalPayloadSize).toInt() else 0

                if (progress != prevProgressPercentage) {
                    setProgress(progress)
                    prevProgressPercentage = progress
                }
            }
        }

        override fun onSuccess() {
            currentNetworkPacket?.let { packet ->
                if (packet.payloadSize == 0L) {
                    synchronized(lock) {
                        if (networkPacketList.isEmpty()) {
                            setProgress(100)
                        }
                    }
                }

                totalSend += packet.payloadSize
            }
        }

        override fun onFailure(e: Throwable) {
            // Handled in the run() function when sendPacketBlocking returns false
        }
    }
}