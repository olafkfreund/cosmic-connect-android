/*
 * SPDX-FileCopyrightText: 2018 Erik Duisters <e.duisters1@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.SharePlugin

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Helpers.FilesHelper
import org.cosmic.cosmicconnect.Helpers.MediaStoreHelper
import org.cosmic.cosmicconnect.NetworkPacket
import org.cosmic.cosmicconnect.R
import org.cosmic.cosmicconnect.async.BackgroundJob
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.FileTime

/**
 * A type of [BackgroundJob] that reads Files from another device.
 *
 * We receive the requests as [NetworkPacket]s.
 * Each packet should have a 'filename' property and a payload. If the payload is missing,
 * we'll just create an empty file. You can add new packets anytime via
 * [.addNetworkPacket].
 *
 * The I/O-part of this file reading is handled by [receiveFile].
 *
 * @see CompositeUploadFileJob
 */
class CompositeReceiveFileJob(device: Device, callback: Callback<java.lang.Void?>) :
    BackgroundJob<Device, java.lang.Void?>(device, callback) {

    private val receiveNotification: ReceiveNotification
    private var currentNetworkPacket: NetworkPacket? = null
    private var currentFileName = ""
    private var currentFileNum = 0
    private var totalReceived: Long = 0
    private var lastProgressTimeMillis: Long = 0
    private var prevProgressPercentage: Long = 0

    private val lock = Any() // Use to protect concurrent access to the variables below

    @GuardedBy("lock")
    private val networkPacketList: MutableList<NetworkPacket> = ArrayList()

    @GuardedBy("lock")
    private var totalNumFiles = 0

    @GuardedBy("lock")
    private var totalPayloadSize: Long = 0
    private var isRunningInternal = false

    init {
        receiveNotification = ReceiveNotification(device, id)
        currentFileNum = 0
        totalNumFiles = 0
        totalPayloadSize = 0
        totalReceived = 0
        lastProgressTimeMillis = 0
        prevProgressPercentage = 0
    }

    private fun getDevice(): Device = requestInfo

    fun isRunning(): Boolean = isRunningInternal

    fun updateTotals(numberOfFiles: Int, totalPayloadSize: Long) {
        synchronized(lock) {
            this.totalNumFiles = numberOfFiles
            this.totalPayloadSize = totalPayloadSize

            receiveNotification.setTitle(
                getDevice().context.resources
                    .getQuantityString(
                        R.plurals.incoming_file_title,
                        totalNumFiles,
                        totalNumFiles,
                        getDevice().name
                    )
            )
        }
    }

    fun addNetworkPacket(networkPacket: NetworkPacket) {
        synchronized(lock) {
            if (!networkPacketList.contains(networkPacket)) {
                networkPacketList.add(networkPacket)

                totalNumFiles = networkPacket.getInt(SharePlugin.KEY_NUMBER_OF_FILES, 1)
                totalPayloadSize = networkPacket.getLong(SharePlugin.KEY_TOTAL_PAYLOAD_SIZE)

                receiveNotification.setTitle(
                    getDevice().context.resources
                        .getQuantityString(
                            R.plurals.incoming_file_title,
                            totalNumFiles,
                            totalNumFiles,
                            getDevice().name
                        )
                )
            }
        }
    }

    override fun run() {
        var done: Boolean
        var outputStream: OutputStream? = null

        synchronized(lock) {
            done = networkPacketList.isEmpty()
        }

        try {
            var fileDocument: DocumentFile? = null

            isRunningInternal = true

            while (!done && !isCancelled) {
                synchronized(lock) {
                    currentNetworkPacket = networkPacketList[0]
                }
                currentNetworkPacket?.let { packet ->
                    currentFileName = packet.getString("filename", System.currentTimeMillis().toString())
                    currentFileNum++

                    setProgress(prevProgressPercentage.toInt())

                    fileDocument = getDocumentFileFor(currentFileName, packet.getBoolean("open", false))

                    fileDocument?.let { doc ->
                        if (packet.hasPayload()) {
                            outputStream = BufferedOutputStream(getDevice().context.contentResolver.openOutputStream(doc.uri))
                            val inputStream = packet.payload?.inputStream

                            inputStream?.let { input ->
                                val received = receiveFile(input, outputStream!!)

                                packet.payload?.close()

                                if (received != packet.payloadSize) {
                                    doc.delete()

                                    if (!isCancelled) {
                                        throw RuntimeException("Failed to receive: $currentFileName received:$received bytes, expected: ${packet.payloadSize} bytes")
                                    }
                                } else {
                                    publishFile(doc, received)
                                }
                            }
                        } else {
                            // TODO: Only set progress to 100 if this is the only file/packet to send
                            setProgress(100)
                            publishFile(doc, 0)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (packet.has("lastModified")) {
                                try {
                                    val lastModified = packet.getLong("lastModified")
                                    Files.setLastModifiedTime(Paths.get(doc.uri.path!!), FileTime.fromMillis(lastModified))
                                } catch (e: Exception) {
                                    Log.e("SharePlugin", "Can't set date on file")
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                val listIsEmpty: Boolean

                synchronized(lock) {
                    networkPacketList.removeAt(0)
                    listIsEmpty = networkPacketList.isEmpty()
                }

                if (listIsEmpty && !isCancelled) {
                    try {
                        Thread.sleep(1000)
                    } catch (ignored: InterruptedException) {
                    }

                    synchronized(lock) {
                        if (currentFileNum < totalNumFiles && networkPacketList.isEmpty()) {
                            throw RuntimeException("Failed to receive ${totalNumFiles - currentFileNum + 1} files")
                        }
                    }
                }

                synchronized(lock) {
                    done = networkPacketList.isEmpty()
                }
            }

            isRunningInternal = false

            if (isCancelled) {
                receiveNotification.cancel()
                return
            }

            val numFiles: Int
            synchronized(lock) {
                numFiles = totalNumFiles
            }

            if (numFiles == 1 && currentNetworkPacket?.getBoolean("open", false) == true && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                receiveNotification.cancel()
                fileDocument?.let { openFile(it) }
            } else {
                // Update the notification and allow to open the file from it
                receiveNotification.setFinished(
                    getDevice().context.resources.getQuantityString(
                        R.plurals.received_files_title,
                        numFiles,
                        getDevice().name,
                        numFiles
                    )
                )

                if (totalNumFiles == 1 && fileDocument != null) {
                    receiveNotification.setURI(fileDocument!!.uri, fileDocument!!.type, fileDocument!!.name)
                }

                receiveNotification.show()
            }
            reportResult(null)

        } catch (e: ActivityNotFoundException) {
            receiveNotification.setFinished(getDevice().context.getString(R.string.no_app_for_opening))
            receiveNotification.show()
        } catch (e: Exception) {
            isRunningInternal = false

            Log.e("Shareplugin", "Error receiving file", e)

            val failedFiles: Int
            synchronized(lock) {
                failedFiles = totalNumFiles - currentFileNum + 1
            }

            receiveNotification.setFailed(
                getDevice().context.resources.getQuantityString(
                    R.plurals.received_files_fail_title,
                    failedFiles,
                    getDevice().name,
                    failedFiles,
                    totalNumFiles
                )
            )
            receiveNotification.show()
            reportError(e)
        } finally {
            closeAllInputStreams()
            networkPacketList.clear()
            try {
                IOUtils.close(outputStream)
            } catch (ignored: IOException) {
            }
        }
    }

    private fun getDocumentFileFor(filename: String, open: Boolean): DocumentFile {
        val destinationFolderDocument: DocumentFile

        var filenameToUse = filename

        // We need to check for already existing files only when storing in the default path.
        // User-defined paths use the new Storage Access Framework that already handles this.
        // If the file should be opened immediately store it in the standard location to avoid the FileProvider trouble (See ReceiveNotification::setURI)
        if (open || !ShareSettingsFragment.isCustomDestinationEnabled(getDevice().context)) {
            val defaultPath = ShareSettingsFragment.defaultDestinationDirectory.absolutePath
            filenameToUse = FilesHelper.findNonExistingNameForNewFile(defaultPath, filenameToUse)
            destinationFolderDocument = DocumentFile.fromFile(File(defaultPath))
        } else {
            destinationFolderDocument = ShareSettingsFragment.getDestinationDirectory(getDevice().context)
        }
        var displayName = FilenameUtils.getBaseName(filenameToUse)
        val mimeType = FilesHelper.getMimeTypeFromFile(filenameToUse)

        if ("*/*" == mimeType) {
            displayName = filenameToUse
        }

        return destinationFolderDocument.createFile(mimeType, displayName)
            ?: throw RuntimeException(getDevice().context.getString(R.string.cannot_create_file, filenameToUse))
    }

    private fun receiveFile(input: InputStream, output: OutputStream): Long {
        val data = ByteArray(4096)
        var count: Int
        var received: Long = 0

        while (input.read(data).also { count = it } >= 0 && !isCancelled) {
            received += count.toLong()
            totalReceived += count.toLong()

            output.write(data, 0, count)

            val progressPercentage: Long
            synchronized(lock) {
                progressPercentage = if (totalPayloadSize > 0) (totalReceived * 100 / totalPayloadSize) else 0
            }
            val curTimeMillis = System.currentTimeMillis()

            if (progressPercentage != prevProgressPercentage &&
                (progressPercentage == 100L || curTimeMillis - lastProgressTimeMillis >= 500)
            ) {
                prevProgressPercentage = progressPercentage
                lastProgressTimeMillis = curTimeMillis
                setProgress(progressPercentage.toInt())
            }
        }

        output.flush()

        return received
    }

    private fun closeAllInputStreams() {
        for (np in networkPacketList) {
            np.payload?.close()
        }
    }

    private fun setProgress(progress: Int) {
        synchronized(lock) {
            receiveNotification.setProgress(
                progress, getDevice().context.resources
                    .getQuantityString(
                        R.plurals.incoming_files_text,
                        totalNumFiles,
                        currentFileName,
                        currentFileNum,
                        totalNumFiles
                    )
            )
        }
        receiveNotification.show()
    }

    private fun publishFile(fileDocument: DocumentFile, size: Long) {
        if (!ShareSettingsFragment.isCustomDestinationEnabled(getDevice().context)) {
            Log.i("SharePlugin", "Adding to downloads")
            val manager = ContextCompat.getSystemService(
                getDevice().context,
                DownloadManager::class.java
            )
            manager!!.addCompletedDownload(
                fileDocument.uri.lastPathSegment,
                getDevice().name,
                true,
                fileDocument.type,
                fileDocument.uri.path,
                size,
                false
            )
        } else {
            // Make sure it is added to the Android Gallery anyway
            Log.i("SharePlugin", "Adding to gallery")
            MediaStoreHelper.indexFile(getDevice().context, fileDocument.uri)
        }
    }

    private fun openFile(fileDocument: DocumentFile) {
        val mimeType = FilesHelper.getMimeTypeFromFile(fileDocument.name)
        val intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Nougat and later require "content://" uris instead of "file://" uris
            val file = File(fileDocument.uri.path!!)
            val contentUri = FileProvider.getUriForFile(
                getDevice().context,
                "org.cosmic.cosmicconnect.fileprovider",
                file
            )
            intent.setDataAndType(contentUri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            intent.setDataAndType(fileDocument.uri, mimeType)
        }

        // Open files for KDE Itinerary explicitly because Android's activity resolution sucks
        if (fileDocument.name!!.endsWith(".itinerary")) {
            intent.setClassName("org.kde.itinerary", "org.kde.itinerary.Activity")
        }

        getDevice().context.startActivity(intent)
    }
}