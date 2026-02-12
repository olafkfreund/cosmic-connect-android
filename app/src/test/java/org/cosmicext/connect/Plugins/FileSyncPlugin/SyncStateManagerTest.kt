/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.FileSyncPlugin

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SyncStateManagerTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var manager: SyncStateManager

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        prefs = context.getSharedPreferences("test_sync_state", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        manager = SyncStateManager(prefs)
    }

    @Test
    fun `getFileState returns null for unknown path`() {
        assertNull(manager.getFileState("/unknown/path"))
    }

    @Test
    fun `updateFileState and getFileState round-trip`() {
        val info = SyncFileInfo(
            path = "/storage/docs/file.txt",
            checksum = "abc123",
            lastModified = 1000L,
            size = 2048L,
            state = FileSyncState.IDLE,
        )

        manager.updateFileState("/storage/docs/file.txt", info)

        val retrieved = manager.getFileState("/storage/docs/file.txt")
        assertNotNull(retrieved)
        assertEquals(info, retrieved)
    }

    @Test
    fun `removeFileState removes tracked file`() {
        val info = SyncFileInfo(
            path = "/tmp/file.txt",
            checksum = "def456",
            lastModified = 2000L,
            size = 100L,
        )
        manager.updateFileState("/tmp/file.txt", info)
        assertNotNull(manager.getFileState("/tmp/file.txt"))

        manager.removeFileState("/tmp/file.txt")
        assertNull(manager.getFileState("/tmp/file.txt"))
    }

    @Test
    fun `getAllStates returns snapshot of all tracked files`() {
        manager.updateFileState("/a", SyncFileInfo("/a", "h1", 1L, 10L))
        manager.updateFileState("/b", SyncFileInfo("/b", "h2", 2L, 20L))
        manager.updateFileState("/c", SyncFileInfo("/c", "h3", 3L, 30L))

        val all = manager.getAllStates()
        assertEquals(3, all.size)
        assertTrue(all.containsKey("/a"))
        assertTrue(all.containsKey("/b"))
        assertTrue(all.containsKey("/c"))
    }

    @Test
    fun `getStatesByFolder filters by path prefix`() {
        manager.updateFileState("/sync/folder1/a.txt", SyncFileInfo("/sync/folder1/a.txt", "h1", 1L, 10L))
        manager.updateFileState("/sync/folder1/b.txt", SyncFileInfo("/sync/folder1/b.txt", "h2", 2L, 20L))
        manager.updateFileState("/sync/folder2/c.txt", SyncFileInfo("/sync/folder2/c.txt", "h3", 3L, 30L))

        val folder1States = manager.getStatesByFolder("/sync/folder1")
        assertEquals(2, folder1States.size)
        assertTrue(folder1States.all { it.path.startsWith("/sync/folder1") })

        val folder2States = manager.getStatesByFolder("/sync/folder2")
        assertEquals(1, folder2States.size)
    }

    @Test
    fun `clearAll removes all states`() {
        manager.updateFileState("/a", SyncFileInfo("/a", "h1", 1L, 10L))
        manager.updateFileState("/b", SyncFileInfo("/b", "h2", 2L, 20L))
        assertEquals(2, manager.getAllStates().size)

        manager.clearAll()
        assertTrue(manager.getAllStates().isEmpty())
    }

    @Test
    fun `persistence round-trip via load`() {
        val info = SyncFileInfo(
            path = "/persist/file.txt",
            checksum = "persist-hash",
            lastModified = 5000L,
            size = 512L,
            state = FileSyncState.PENDING_UPLOAD,
        )
        manager.updateFileState("/persist/file.txt", info)

        // Create a new manager pointing at the same preferences
        val manager2 = SyncStateManager(prefs)
        // Before load, it should be empty
        assertTrue(manager2.getAllStates().isEmpty())

        manager2.load()

        val loaded = manager2.getFileState("/persist/file.txt")
        assertNotNull(loaded)
        assertEquals("persist-hash", loaded!!.checksum)
        assertEquals(5000L, loaded.lastModified)
        assertEquals(512L, loaded.size)
        assertEquals(FileSyncState.PENDING_UPLOAD, loaded.state)
    }

    @Test
    fun `load with null preferences does not crash`() {
        val nullPrefManager = SyncStateManager(null)
        nullPrefManager.load()
        assertTrue(nullPrefManager.getAllStates().isEmpty())
    }

    @Test
    fun `load with empty preferences does not crash`() {
        // Prefs is already cleared in setup, so no "file_sync_states" key exists
        manager.load()
        assertTrue(manager.getAllStates().isEmpty())
    }

    @Test
    fun `concurrent access does not crash`() {
        // Basic smoke test: rapidly update and read from multiple virtual threads
        val threads = (0 until 10).map { i ->
            Thread {
                val path = "/concurrent/$i"
                manager.updateFileState(path, SyncFileInfo(path, "hash$i", i.toLong(), i.toLong() * 10))
                manager.getFileState(path)
                manager.getAllStates()
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // All 10 entries should exist
        assertEquals(10, manager.getAllStates().size)
    }
}
