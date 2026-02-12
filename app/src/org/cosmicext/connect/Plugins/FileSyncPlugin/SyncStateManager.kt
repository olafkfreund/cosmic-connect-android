/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FileSyncPlugin

import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Possible states for a tracked file in the sync engine. */
enum class FileSyncState {
    IDLE,
    PENDING_UPLOAD,
    UPLOADING,
    PENDING_DOWNLOAD,
    DOWNLOADING,
    CONFLICT,
    ERROR,
}

/**
 * Immutable snapshot of a single file's sync metadata.
 *
 * @property path Absolute file path on the local device
 * @property checksum SHA-256 hex digest of file content
 * @property lastModified Epoch millis of last modification
 * @property size File size in bytes
 * @property state Current sync state
 */
data class SyncFileInfo(
    val path: String,
    val checksum: String,
    val lastModified: Long,
    val size: Long,
    val state: FileSyncState = FileSyncState.IDLE,
)

/**
 * Thread-safe per-file sync state tracker with optional SharedPreferences persistence.
 *
 * Stores [SyncFileInfo] keyed by absolute path in a [ConcurrentHashMap].
 * When [preferences] is non-null, every mutation is persisted as a JSON blob
 * under the key `"file_sync_states"`.
 */
class SyncStateManager(private val preferences: SharedPreferences?) {

    private val fileStates = ConcurrentHashMap<String, SyncFileInfo>()

    /** Retrieve sync info for [path], or null if not tracked. */
    fun getFileState(path: String): SyncFileInfo? = fileStates[path]

    /** Insert or replace sync info for [path]. Persists to preferences. */
    fun updateFileState(path: String, info: SyncFileInfo) {
        fileStates[path] = info
        persist()
    }

    /** Remove tracking for [path]. Persists to preferences. */
    fun removeFileState(path: String) {
        fileStates.remove(path)
        persist()
    }

    /** Return a snapshot of all tracked file states. */
    fun getAllStates(): Map<String, SyncFileInfo> = fileStates.toMap()

    /** Return all states whose path starts with [folderId]. */
    fun getStatesByFolder(folderId: String): List<SyncFileInfo> {
        return fileStates.values.filter { it.path.startsWith(folderId) }
    }

    /** Remove all tracked states. Persists to preferences. */
    fun clearAll() {
        fileStates.clear()
        persist()
    }

    /** Reload state from SharedPreferences (call once at startup). */
    fun load() {
        val json = preferences?.getString(PREFS_KEY, null) ?: return
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                val fileObj = obj.getJSONObject(key)
                fileStates[key] = SyncFileInfo(
                    path = fileObj.getString("path"),
                    checksum = fileObj.getString("checksum"),
                    lastModified = fileObj.getLong("lastModified"),
                    size = fileObj.getLong("size"),
                    state = FileSyncState.valueOf(fileObj.optString("state", "IDLE")),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sync states", e)
        }
    }

    /** Write current state map to SharedPreferences as JSON. */
    private fun persist() {
        val obj = JSONObject()
        fileStates.forEach { (key, info) ->
            obj.put(key, JSONObject().apply {
                put("path", info.path)
                put("checksum", info.checksum)
                put("lastModified", info.lastModified)
                put("size", info.size)
                put("state", info.state.name)
            })
        }
        preferences?.edit()?.putString(PREFS_KEY, obj.toString())?.apply()
    }

    companion object {
        private const val TAG = "SyncStateManager"
        private const val PREFS_KEY = "file_sync_states"
    }
}
