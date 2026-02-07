/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FileSyncPlugin

import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.di.PluginCreator
import org.cosmic.cosmicconnect.R

/**
 * Synchronizes files between devices with bidirectional sync support.
 * Tracks file changes, detects conflicts, and manages sync folders.
 */
class FileSyncPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device) {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): FileSyncPlugin
    }

    data class SyncFolder(
        val id: String,
        val path: String,
        var syncStatus: SyncStatus = SyncStatus.IDLE,
    )

    enum class SyncStatus {
        IDLE,
        SYNCING,
        COMPLETE,
        ERROR,
    }

    data class FileConflict(
        val path: String,
        val localChecksum: String,
        val remoteChecksum: String,
        val localTimestamp: Long,
        val remoteTimestamp: Long,
        val syncFolderId: String,
    )

    /** List of sync folders managed by this plugin. */
    private val syncFolders = mutableListOf<SyncFolder>()

    /** List of detected conflicts. */
    val conflicts = mutableListOf<FileConflict>()

    /** Listener for sync state changes. */
    var listener: Listener? = null

    interface Listener {
        fun onSyncStatusChanged(folderId: String, status: SyncStatus)
        fun onConflictDetected(conflict: FileConflict)
        fun onFileChanged(action: String, path: String, syncFolderId: String)
    }

    override val displayName: String
        get() = context.resources.getString(R.string.pref_plugin_filesync)

    override val description: String
        get() = context.resources.getString(R.string.pref_plugin_filesync_desc)

    override val supportedPacketTypes: Array<String> = arrayOf(
        PACKET_TYPE_FILESYNC,
        PACKET_TYPE_FILESYNC_CONFLICT,
    )

    override val outgoingPacketTypes: Array<String> = arrayOf(PACKET_TYPE_FILESYNC_REQUEST)

    override val isEnabledByDefault: Boolean = false

    override fun onCreate(): Boolean {
        // Request list of sync folders on connection
        requestSyncFolderList()
        return true
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet

        when (np.type) {
            PACKET_TYPE_FILESYNC -> handleFileSyncNotification(np)
            PACKET_TYPE_FILESYNC_CONFLICT -> handleConflictNotification(np)
            else -> return false
        }

        return true
    }

    private fun handleFileSyncNotification(np: NetworkPacket) {
        val action = np.body["action"] as? String ?: return
        val path = np.body["path"] as? String ?: return
        val syncFolderId = np.body["syncFolderId"] as? String ?: return
        val checksum = np.body["checksum"] as? String
        val size = (np.body["size"] as? Number)?.toLong()
        val timestamp = (np.body["timestamp"] as? Number)?.toLong()

        Log.d(TAG, "File sync notification: action=$action, path=$path, folder=$syncFolderId")

        // Update sync status for folder
        when (action) {
            "sync_started" -> {
                val folder = syncFolders.find { it.id == syncFolderId }
                folder?.syncStatus = SyncStatus.SYNCING
                listener?.onSyncStatusChanged(syncFolderId, SyncStatus.SYNCING)
            }
            "sync_complete" -> {
                val folder = syncFolders.find { it.id == syncFolderId }
                folder?.syncStatus = SyncStatus.COMPLETE
                listener?.onSyncStatusChanged(syncFolderId, SyncStatus.COMPLETE)
            }
            "file_added", "file_changed", "file_deleted" -> {
                listener?.onFileChanged(action, path, syncFolderId)
            }
        }

        Log.i(TAG, "Handled file sync: $action for $path (checksum=$checksum, size=$size, ts=$timestamp)")
    }

    private fun handleConflictNotification(np: NetworkPacket) {
        val path = np.body["path"] as? String ?: return
        val localChecksum = np.body["localChecksum"] as? String ?: return
        val remoteChecksum = np.body["remoteChecksum"] as? String ?: return
        val localTimestamp = (np.body["localTimestamp"] as? Number)?.toLong() ?: return
        val remoteTimestamp = (np.body["remoteTimestamp"] as? Number)?.toLong() ?: return
        val syncFolderId = np.body["syncFolderId"] as? String ?: return

        val conflict = FileConflict(
            path = path,
            localChecksum = localChecksum,
            remoteChecksum = remoteChecksum,
            localTimestamp = localTimestamp,
            remoteTimestamp = remoteTimestamp,
            syncFolderId = syncFolderId,
        )

        conflicts.add(conflict)
        listener?.onConflictDetected(conflict)
        Log.w(TAG, "File conflict detected: $path in folder $syncFolderId")
    }

    /** Request sync of a specific folder. */
    fun requestSync(folderId: String) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf(
                "requestSync" to true,
                "syncFolderId" to folderId,
            ),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Requested sync for folder: $folderId")
    }

    /** Add a new sync folder. */
    fun addSyncFolder(path: String) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf("addSyncFolder" to path),
        )
        device.sendPacket(TransferPacket(packet))
        Log.i(TAG, "Requested to add sync folder: $path")
    }

    /** Remove a sync folder. */
    fun removeSyncFolder(folderId: String) {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf("removeSyncFolder" to folderId),
        )
        device.sendPacket(TransferPacket(packet))
        syncFolders.removeAll { it.id == folderId }
        Log.i(TAG, "Requested to remove sync folder: $folderId")
    }

    /** Request list of all sync folders. */
    fun requestSyncFolderList() {
        val packet = NetworkPacket(
            id = System.currentTimeMillis(),
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf("listSyncFolders" to true),
        )
        device.sendPacket(TransferPacket(packet))
        Log.d(TAG, "Requested sync folder list")
    }

    /** Get current list of sync folders. */
    fun getSyncFolders(): List<SyncFolder> = syncFolders.toList()

    companion object {
        private const val TAG = "FileSyncPlugin"
        const val PACKET_TYPE_FILESYNC = "cconnect.filesync"
        const val PACKET_TYPE_FILESYNC_REQUEST = "cconnect.filesync.request"
        const val PACKET_TYPE_FILESYNC_CONFLICT = "cconnect.filesync.conflict"
    }
}
