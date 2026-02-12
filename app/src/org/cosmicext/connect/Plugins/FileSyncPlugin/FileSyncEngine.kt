/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FileSyncPlugin

import android.content.Context
import android.os.FileObserver
import android.util.Log
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Core sync orchestrator that ties together [FileSyncObserver], [SyncStateManager],
 * and the COSMIC Connect packet transport.
 *
 * Lifecycle:
 * 1. [start] — register [FileObserver]s for each configured folder
 * 2. [scanFolder] — compute checksums for every file in a folder
 * 3. Local changes detected by observers trigger [handleLocalFileChange] which
 *    updates state and sends a `cconnect.filesync` notification to the desktop
 * 4. [stop] / [destroy] — tear down observers and optionally clear persisted state
 *
 * Thread-safety: Observers fire on a background thread; all mutable state lives in
 * [ConcurrentHashMap] and the thread-safe [SyncStateManager].
 */
class FileSyncEngine(
    private val context: Context,
    private val device: Device,
    private val stateManager: SyncStateManager,
    private val listener: FileSyncPlugin.Listener?,
) {
    companion object {
        private const val TAG = "FileSyncEngine"
        private const val PACKET_TYPE_FILESYNC_NOTIFICATION = "cconnect.filesync"
    }

    private val observers = ConcurrentHashMap<String, FileSyncObserver>()
    private var nextPacketId = 1L

    @Volatile
    var isRunning = false
        private set

    /**
     * Start watching all provided folders.
     */
    fun start(folders: List<FileSyncPlugin.SyncFolder>) {
        isRunning = true
        folders.forEach { folder -> watchFolder(folder) }
        Log.i(TAG, "File sync engine started with ${folders.size} folders")
    }

    /**
     * Stop all observers but keep persisted state intact.
     */
    fun stop() {
        isRunning = false
        observers.values.forEach { it.stopWatching() }
        observers.clear()
        Log.i(TAG, "File sync engine stopped")
    }

    /**
     * Register a [FileSyncObserver] for [folder]. Replaces any existing observer
     * for the same folder ID.
     */
    fun watchFolder(folder: FileSyncPlugin.SyncFolder) {
        val existing = observers.remove(folder.id)
        existing?.stopWatching()

        val file = File(folder.path)
        if (!file.exists() || !file.isDirectory) {
            Log.w(TAG, "Cannot watch non-existent folder: ${folder.path}")
            return
        }

        val observer = FileSyncObserver(folder.path, folder.id) { event, path, folderId ->
            if (!isRunning || path == null) return@FileSyncObserver
            handleLocalFileChange(event, path, folderId)
        }
        observers[folder.id] = observer
        observer.startWatching()
        Log.i(TAG, "Watching folder: ${folder.path} (id: ${folder.id})")
    }

    /**
     * Stop watching the folder identified by [folderId].
     */
    fun unwatchFolder(folderId: String) {
        val observer = observers.remove(folderId)
        observer?.stopWatching()
    }

    /**
     * Walk all non-hidden files in [folder] and record their checksums in
     * [stateManager]. Useful after initial folder registration.
     */
    fun scanFolder(folder: FileSyncPlugin.SyncFolder) {
        val dir = File(folder.path)
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()?.forEach { file ->
            if (file.isFile && !file.isHidden) {
                try {
                    val checksum = FileInputStream(file).use { FileChecksumHelper.computeSha256(it) }
                    stateManager.updateFileState(
                        file.absolutePath,
                        SyncFileInfo(
                            path = file.absolutePath,
                            checksum = checksum,
                            lastModified = file.lastModified(),
                            size = file.length(),
                            state = FileSyncState.IDLE,
                        ),
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to scan file: ${file.absolutePath}", e)
                }
            }
        }
    }

    /**
     * Compute the SHA-256 hex digest of the file at [filePath].
     *
     * @return The digest, or null if the file cannot be read
     */
    fun computeChecksum(filePath: String): String? {
        return try {
            FileInputStream(File(filePath)).use { FileChecksumHelper.computeSha256(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute checksum: $filePath", e)
            null
        }
    }

    /** Return the set of folder IDs currently being watched. */
    fun getWatchedFolderIds(): Set<String> = observers.keys.toSet()

    /**
     * Process a local filesystem event and propagate it to the desktop.
     */
    internal fun handleLocalFileChange(event: Int, path: String, folderId: String) {
        val action = when {
            event and FileObserver.CREATE != 0 -> "file_added"
            event and FileObserver.CLOSE_WRITE != 0 -> "file_modified"
            event and FileObserver.DELETE != 0 -> "file_deleted"
            event and FileObserver.MOVED_FROM != 0 -> "file_deleted"
            event and FileObserver.MOVED_TO != 0 -> "file_added"
            else -> return
        }

        Log.i(TAG, "Local change: $action for $path in folder $folderId")

        val file = File(path)
        val checksum = if (file.exists() && file.isFile) computeChecksum(path) else null
        val size = if (file.exists()) file.length() else 0L

        // Update local state
        if (action == "file_deleted") {
            stateManager.removeFileState(path)
        } else if (checksum != null) {
            stateManager.updateFileState(
                path,
                SyncFileInfo(
                    path = path,
                    checksum = checksum,
                    lastModified = file.lastModified(),
                    size = size,
                    state = FileSyncState.PENDING_UPLOAD,
                ),
            )
        }

        // Notify desktop
        try {
            val body = mutableMapOf<String, Any>(
                "action" to action,
                "path" to path,
                "syncFolderId" to folderId,
            )
            if (checksum != null) body["checksum"] = checksum
            if (size > 0) body["size"] = size
            body["timestamp"] = System.currentTimeMillis()

            val packet = NetworkPacket(
                id = nextPacketId++,
                type = PACKET_TYPE_FILESYNC_NOTIFICATION,
                body = body,
            )
            device.sendPacket(TransferPacket(packet))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify desktop of local change", e)
        }

        // Notify UI listener
        try {
            listener?.onFileChanged(action, path, folderId)
        } catch (e: Exception) {
            Log.e(TAG, "Listener error", e)
        }
    }

    /**
     * Full teardown: stop observers and clear persisted state.
     */
    fun destroy() {
        stop()
        stateManager.clearAll()
    }
}
