/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FileSyncPlugin

import android.os.FileObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
class FileSyncObserverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `onEvent skips null path`() {
        val events = CopyOnWriteArrayList<String>()
        val observer = FileSyncObserver(
            path = tempFolder.root.absolutePath,
            folderId = "test-folder",
        ) { _, path, _ ->
            if (path != null) events.add(path)
        }

        // Simulate null path
        observer.onEvent(FileObserver.CREATE, null)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `onEvent skips hidden files starting with dot`() {
        val events = CopyOnWriteArrayList<String>()
        val observer = FileSyncObserver(
            path = tempFolder.root.absolutePath,
            folderId = "test-folder",
        ) { _, path, _ ->
            if (path != null) events.add(path)
        }

        observer.onEvent(FileObserver.CREATE, ".hidden_file")
        assertTrue(events.isEmpty())
    }

    @Test
    fun `onEvent skips tmp files`() {
        val events = CopyOnWriteArrayList<String>()
        val observer = FileSyncObserver(
            path = tempFolder.root.absolutePath,
            folderId = "test-folder",
        ) { _, path, _ ->
            if (path != null) events.add(path)
        }

        observer.onEvent(FileObserver.CLOSE_WRITE, "document.tmp")
        assertTrue(events.isEmpty())
    }

    @Test
    fun `onEvent skips tilde backup files`() {
        val events = CopyOnWriteArrayList<String>()
        val observer = FileSyncObserver(
            path = tempFolder.root.absolutePath,
            folderId = "test-folder",
        ) { _, path, _ ->
            if (path != null) events.add(path)
        }

        observer.onEvent(FileObserver.CLOSE_WRITE, "document.txt~")
        assertTrue(events.isEmpty())
    }

    @Test
    fun `onEvent fires for normal file with full path and correct folderId`() {
        val receivedEvents = CopyOnWriteArrayList<Triple<Int, String?, String>>()
        val basePath = tempFolder.root.absolutePath
        val observer = FileSyncObserver(
            path = basePath,
            folderId = "sync-1",
        ) { event, path, folderId ->
            receivedEvents.add(Triple(event, path, folderId))
        }

        observer.onEvent(FileObserver.CREATE, "readme.md")

        assertEquals(1, receivedEvents.size)
        val (event, path, folderId) = receivedEvents[0]
        assertEquals(FileObserver.CREATE, event)
        assertEquals("$basePath/readme.md", path)
        assertEquals("sync-1", folderId)
    }
}
