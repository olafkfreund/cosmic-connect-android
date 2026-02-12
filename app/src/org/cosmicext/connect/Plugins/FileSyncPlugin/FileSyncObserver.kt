/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FileSyncPlugin

import android.os.FileObserver
import android.util.Log
import java.io.File

/**
 * Watches a single sync folder for file changes via the Linux inotify API.
 *
 * Only fires for meaningful file mutations (create, write-close, delete, move).
 * Hidden files (dot-prefix), temporary files (`.tmp` / `~` suffix) are silently
 * filtered out to avoid noise from editors and version-control systems.
 *
 * @param path Absolute directory path to watch
 * @param folderId Logical sync-folder identifier passed through to [onFileChanged]
 * @param onFileChanged Callback invoked with `(event, fullPath, folderId)`
 */
class FileSyncObserver(
    private val path: String,
    private val folderId: String,
    private val onFileChanged: (event: Int, path: String?, folderId: String) -> Unit,
) : FileObserver(File(path), CLOSE_WRITE or DELETE or MOVED_FROM or MOVED_TO or CREATE) {

    companion object {
        private const val TAG = "FileSyncObserver"
    }

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return
        // Skip hidden files and temp files
        if (path.startsWith(".") || path.endsWith(".tmp") || path.endsWith("~")) return

        val fullPath = "${this.path}/$path"
        Log.d(TAG, "File event $event for $fullPath in folder $folderId")
        onFileChanged(event, fullPath, folderId)
    }
}
