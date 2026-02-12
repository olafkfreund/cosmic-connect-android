/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FileSyncPlugin

import android.content.Context
import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmicext.connect.Core.NetworkPacket
import org.cosmicext.connect.Core.TransferPacket
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.di.PluginCreator
import org.cosmicext.connect.R
import org.json.JSONArray
import org.json.JSONObject

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
        val syncStatus: SyncStatus = SyncStatus.IDLE,
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

    /** Thread-safe list of sync folders managed by this plugin. */
    private val syncFolders = java.util.Collections.synchronizedList(mutableListOf<SyncFolder>())

    /** Thread-safe backing list of detected conflicts. */
    private val _conflicts = java.util.Collections.synchronizedList(mutableListOf<FileConflict>())

    /** Read-only snapshot of detected conflicts. */
    val conflicts: List<FileConflict> get() = _conflicts.toList()

    /** Listener for sync state changes. */
    var listener: Listener? = null

    /** Monotonically increasing packet ID counter. */
    private var nextPacketId = 1L

    /** Per-file sync state tracker with persistence. */
    private var stateManager: SyncStateManager? = null

    /** Core file-watching and sync orchestrator. */
    private var engine: FileSyncEngine? = null

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

    override fun getUiButtons(): List<PluginUiButton> = listOf(
        PluginUiButton(
            context.getString(R.string.pref_plugin_filesync),
            R.drawable.ic_notification,
        ) { /* Navigation handled by Compose NavGraph */ }
    )

    override fun onCreate(): Boolean {
        loadSyncFolders()

        // Initialize sync state tracker and engine
        stateManager = SyncStateManager(preferences).also { it.load() }
        engine = FileSyncEngine(context, device, stateManager!!, listener)

        val folders = getSyncFolders()
        if (folders.isNotEmpty()) {
            engine?.start(folders)
        }

        requestSyncFolderList()
        return true
    }

    override fun onDestroy() {
        engine?.destroy()
        engine = null
        stateManager = null
        listener = null
        synchronized(syncFolders) { syncFolders.clear() }
        synchronized(_conflicts) { _conflicts.clear() }
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet

        when (np.type) {
            PACKET_TYPE_FILESYNC -> {
                // Check if this is a folder list response
                val foldersJson = np.body["folders"]
                if (foldersJson != null) {
                    handleFolderListResponse(np)
                    return true
                }
                handleFileSyncNotification(np)
            }
            PACKET_TYPE_FILESYNC_CONFLICT -> handleConflictNotification(np)
            else -> return false
        }

        return true
    }

    private fun handleFileSyncNotification(np: NetworkPacket) {
        val action = np.body["action"] as? String
            ?: run { Log.w(TAG, "Malformed filesync packet: missing 'action'"); return }
        val path = np.body["path"] as? String
            ?: run { Log.w(TAG, "Malformed filesync packet: missing 'path'"); return }
        val syncFolderId = np.body["syncFolderId"] as? String
            ?: run { Log.w(TAG, "Malformed filesync packet: missing 'syncFolderId'"); return }
        val checksum = np.body["checksum"] as? String
        val size = (np.body["size"] as? Number)?.toLong()
        val timestamp = (np.body["timestamp"] as? Number)?.toLong()

        Log.d(TAG, "File sync notification: action=$action, path=$path, folder=$syncFolderId")

        // Update sync status for folder (copy-on-write for immutable SyncFolder)
        when (action) {
            "sync_started" -> {
                synchronized(syncFolders) {
                    val index = syncFolders.indexOfFirst { it.id == syncFolderId }
                    if (index >= 0) {
                        syncFolders[index] = syncFolders[index].copy(syncStatus = SyncStatus.SYNCING)
                    }
                }
                safeNotify { listener?.onSyncStatusChanged(syncFolderId, SyncStatus.SYNCING) }
            }
            "sync_complete" -> {
                synchronized(syncFolders) {
                    val index = syncFolders.indexOfFirst { it.id == syncFolderId }
                    if (index >= 0) {
                        syncFolders[index] = syncFolders[index].copy(syncStatus = SyncStatus.COMPLETE)
                    }
                }
                safeNotify { listener?.onSyncStatusChanged(syncFolderId, SyncStatus.COMPLETE) }
            }
            "file_added", "file_changed", "file_deleted" -> {
                safeNotify { listener?.onFileChanged(action, path, syncFolderId) }
            }
        }

        Log.i(TAG, "Handled file sync: $action for $path (checksum=$checksum, size=$size, ts=$timestamp)")
    }

    private fun handleConflictNotification(np: NetworkPacket) {
        val path = np.body["path"] as? String
            ?: run { Log.w(TAG, "Malformed conflict packet: missing 'path'"); return }
        val localChecksum = np.body["localChecksum"] as? String
            ?: run { Log.w(TAG, "Malformed conflict packet: missing 'localChecksum'"); return }
        val remoteChecksum = np.body["remoteChecksum"] as? String
            ?: run { Log.w(TAG, "Malformed conflict packet: missing 'remoteChecksum'"); return }
        val localTimestamp = (np.body["localTimestamp"] as? Number)?.toLong()
            ?: run { Log.w(TAG, "Malformed conflict packet: missing 'localTimestamp'"); return }
        val remoteTimestamp = (np.body["remoteTimestamp"] as? Number)?.toLong()
            ?: run { Log.w(TAG, "Malformed conflict packet: missing 'remoteTimestamp'"); return }
        val syncFolderId = np.body["syncFolderId"] as? String
            ?: run { Log.w(TAG, "Malformed conflict packet: missing 'syncFolderId'"); return }

        val conflict = FileConflict(
            path = path,
            localChecksum = localChecksum,
            remoteChecksum = remoteChecksum,
            localTimestamp = localTimestamp,
            remoteTimestamp = remoteTimestamp,
            syncFolderId = syncFolderId,
        )

        synchronized(_conflicts) {
            // Deduplicate by path: replace existing conflict for same path
            _conflicts.removeAll { it.path == conflict.path }
            _conflicts.add(conflict)
            // Bound the list to prevent unbounded growth
            while (_conflicts.size > MAX_CONFLICTS) {
                _conflicts.removeAt(0)
            }
        }
        safeNotify { listener?.onConflictDetected(conflict) }
        Log.w(TAG, "File conflict detected: $path in folder $syncFolderId")
    }

    /** Handle folder list response from desktop. */
    private fun handleFolderListResponse(np: NetworkPacket) {
        val foldersValue = np.body["folders"]
        if (foldersValue == null) {
            Log.w(TAG, "Folder list response missing 'folders' field")
            return
        }

        try {
            val foldersStr = foldersValue.toString()
            val arr = JSONArray(foldersStr)
            synchronized(syncFolders) {
                syncFolders.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val path = obj.getString("path")
                    if (isValidSyncPath(path)) {
                        syncFolders.add(SyncFolder(id = id, path = path, syncStatus = SyncStatus.IDLE))
                    } else {
                        Log.w(TAG, "Rejected invalid sync path from desktop: $path")
                    }
                }
            }
            saveSyncFolders()

            // Register new folders with the engine
            val currentFolders = getSyncFolders()
            currentFolders.forEach { folder ->
                engine?.watchFolder(folder)
                engine?.scanFolder(folder)
            }
            if (currentFolders.isNotEmpty() && engine?.isRunning == false) {
                engine?.start(currentFolders)
            }

            Log.i(TAG, "Received ${arr.length()} sync folders from desktop")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse folder list response", e)
        }
    }

    /** Request sync of a specific folder. */
    fun requestSync(folderId: String) {
        val packet = NetworkPacket(
            id = nextPacketId++,
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf(
                "requestSync" to true,
                "syncFolderId" to folderId,
            ),
        )
        try {
            device.sendPacket(TransferPacket(packet))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send sync request packet", e)
        }
        Log.i(TAG, "Requested sync for folder: $folderId")
    }

    /** Add a new sync folder. */
    fun addSyncFolder(path: String) {
        if (!isValidSyncPath(path)) {
            Log.w(TAG, "Rejected invalid sync path: $path")
            return
        }
        val packet = NetworkPacket(
            id = nextPacketId++,
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf("addSyncFolder" to path),
        )
        try {
            device.sendPacket(TransferPacket(packet))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send add sync folder packet", e)
        }
        Log.i(TAG, "Requested to add sync folder: $path")
    }

    /** Remove a sync folder. */
    fun removeSyncFolder(folderId: String) {
        val packet = NetworkPacket(
            id = nextPacketId++,
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf("removeSyncFolder" to folderId),
        )
        try {
            device.sendPacket(TransferPacket(packet))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send remove sync folder packet", e)
        }
        engine?.unwatchFolder(folderId)
        synchronized(syncFolders) {
            syncFolders.removeAll { it.id == folderId }
        }
        saveSyncFolders()
        Log.i(TAG, "Requested to remove sync folder: $folderId")
    }

    /** Request list of all sync folders. */
    fun requestSyncFolderList() {
        val packet = NetworkPacket(
            id = nextPacketId++,
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf("listSyncFolders" to true),
        )
        try {
            device.sendPacket(TransferPacket(packet))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send sync folder list request packet", e)
        }
        Log.d(TAG, "Requested sync folder list")
    }

    /** Get current list of sync folders. */
    fun getSyncFolders(): List<SyncFolder> = synchronized(syncFolders) { syncFolders.toList() }

    /**
     * Resolve a file conflict by choosing either the local or remote version.
     *
     * @param conflictPath Path of the conflicted file
     * @param useLocal True to keep local version, false to use remote version
     */
    fun resolveConflict(conflictPath: String, useLocal: Boolean) {
        val packet = NetworkPacket(
            id = nextPacketId++,
            type = PACKET_TYPE_FILESYNC_REQUEST,
            body = mapOf(
                "resolveConflict" to true,
                "path" to conflictPath,
                "resolution" to if (useLocal) "local" else "remote",
            ),
        )
        try {
            device.sendPacket(TransferPacket(packet))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send conflict resolution packet", e)
        }
        synchronized(_conflicts) {
            _conflicts.removeAll { it.path == conflictPath }
        }
        Log.i(TAG, "Resolved conflict for $conflictPath: ${if (useLocal) "local" else "remote"}")
    }

    /** Validate a sync path is safe for use. */
    private fun isValidSyncPath(path: String): Boolean {
        if (path.isBlank()) return false
        if (path.contains("..")) return false
        if (!path.startsWith("/")) return false
        // Block sensitive system paths
        val blocked = listOf("/data/data/", "/system/", "/proc/", "/sys/", "/dev/")
        return blocked.none { path.startsWith(it) }
    }

    /** Safely invoke a listener callback, catching any exceptions. */
    private fun safeNotify(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Listener callback error", e)
        }
    }

    /** Load persisted sync folders from SharedPreferences. */
    private fun loadSyncFolders() {
        val prefs = preferences ?: return
        val json = prefs.getString("sync_folders_json", null) ?: return
        try {
            val arr = JSONArray(json)
            synchronized(syncFolders) {
                syncFolders.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    syncFolders.add(
                        SyncFolder(
                            id = obj.getString("id"),
                            path = obj.getString("path"),
                            syncStatus = SyncStatus.IDLE,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sync folders", e)
        }
    }

    /** Persist sync folders to SharedPreferences. */
    private fun saveSyncFolders() {
        val prefs = preferences ?: return
        val arr = JSONArray()
        synchronized(syncFolders) {
            syncFolders.forEach { folder ->
                val obj = JSONObject().apply {
                    put("id", folder.id)
                    put("path", folder.path)
                }
                arr.put(obj)
            }
        }
        prefs.edit().putString("sync_folders_json", arr.toString()).apply()
    }

    /** Get all tracked file states (delegates to [SyncStateManager]). */
    fun getFileStates(): Map<String, SyncFileInfo> = stateManager?.getAllStates() ?: emptyMap()

    /** Compute SHA-256 checksum for a file (delegates to [FileSyncEngine]). */
    fun computeFileChecksum(path: String): String? = engine?.computeChecksum(path)

    companion object {
        private const val TAG = "FileSyncPlugin"
        private const val MAX_CONFLICTS = 1000
        const val PACKET_TYPE_FILESYNC = "cconnect.filesync"
        const val PACKET_TYPE_FILESYNC_REQUEST = "cconnect.filesync.request"
        const val PACKET_TYPE_FILESYNC_CONFLICT = "cconnect.filesync.conflict"
    }
}
