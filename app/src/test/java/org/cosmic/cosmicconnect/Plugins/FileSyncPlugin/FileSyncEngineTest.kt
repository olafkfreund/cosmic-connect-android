/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FileSyncPlugin

import android.content.Context
import android.os.FileObserver
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.cosmic.cosmicconnect.Device
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileSyncEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var stateManager: SyncStateManager
    private lateinit var mockListener: FileSyncPlugin.Listener
    private lateinit var engine: FileSyncEngine

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        stateManager = SyncStateManager(null) // No persistence needed for tests
        mockListener = mockk(relaxed = true)
        engine = FileSyncEngine(context, mockDevice, stateManager, mockListener)
    }

    @After
    fun teardown() {
        engine.destroy()
    }

    @Test
    fun `start sets isRunning to true`() {
        assertFalse(engine.isRunning)
        engine.start(emptyList())
        assertTrue(engine.isRunning)
    }

    @Test
    fun `stop sets isRunning to false`() {
        engine.start(emptyList())
        assertTrue(engine.isRunning)
        engine.stop()
        assertFalse(engine.isRunning)
    }

    @Test
    fun `watchFolder registers observer for valid directory`() {
        val dir = tempFolder.newFolder("sync")
        val folder = FileSyncPlugin.SyncFolder(id = "f1", path = dir.absolutePath)

        engine.start(listOf(folder))
        assertTrue(engine.getWatchedFolderIds().contains("f1"))
    }

    @Test
    fun `watchFolder skips non-existent directory`() {
        val folder = FileSyncPlugin.SyncFolder(id = "missing", path = "/non/existent/path")

        engine.start(listOf(folder))
        assertFalse(engine.getWatchedFolderIds().contains("missing"))
    }

    @Test
    fun `unwatchFolder removes observer`() {
        val dir = tempFolder.newFolder("sync2")
        val folder = FileSyncPlugin.SyncFolder(id = "f2", path = dir.absolutePath)

        engine.start(listOf(folder))
        assertTrue(engine.getWatchedFolderIds().contains("f2"))

        engine.unwatchFolder("f2")
        assertFalse(engine.getWatchedFolderIds().contains("f2"))
    }

    @Test
    fun `scanFolder records checksums for files in directory`() {
        val dir = tempFolder.newFolder("scantest")
        File(dir, "file1.txt").writeText("content one")
        File(dir, "file2.txt").writeText("content two")
        // Hidden file should be skipped
        File(dir, ".hidden").writeText("secret")

        val folder = FileSyncPlugin.SyncFolder(id = "scan1", path = dir.absolutePath)
        engine.scanFolder(folder)

        val states = stateManager.getAllStates()
        assertEquals(2, states.size)
        assertTrue(states.containsKey("${dir.absolutePath}/file1.txt"))
        assertTrue(states.containsKey("${dir.absolutePath}/file2.txt"))
        // Hidden file not tracked
        assertFalse(states.containsKey("${dir.absolutePath}/.hidden"))
    }

    @Test
    fun `scanFolder records correct file metadata`() {
        val dir = tempFolder.newFolder("meta")
        val file = File(dir, "data.bin")
        file.writeBytes(ByteArray(128) { it.toByte() })

        val folder = FileSyncPlugin.SyncFolder(id = "meta1", path = dir.absolutePath)
        engine.scanFolder(folder)

        val info = stateManager.getFileState(file.absolutePath)
        assertNotNull(info)
        assertEquals(file.absolutePath, info!!.path)
        assertEquals(128L, info.size)
        assertEquals(FileSyncState.IDLE, info.state)
        assertEquals(64, info.checksum.length) // SHA-256 hex
    }

    @Test
    fun `computeChecksum returns valid hash for existing file`() {
        val file = tempFolder.newFile("checkme.txt")
        file.writeText("hello world")

        val hash = engine.computeChecksum(file.absolutePath)
        assertNotNull(hash)
        assertEquals(64, hash!!.length)
        // Known SHA-256 of "hello world"
        assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", hash)
    }

    @Test
    fun `computeChecksum returns null for missing file`() {
        val hash = engine.computeChecksum("/does/not/exist")
        assertNull(hash)
    }

    @Test
    fun `handleLocalFileChange sends packet to device for file_added`() {
        val file = tempFolder.newFile("newfile.txt")
        file.writeText("some content")

        engine.start(emptyList())
        engine.handleLocalFileChange(FileObserver.CREATE, file.absolutePath, "folder-x")

        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync" &&
                    it.packet.body["action"] == "file_added" &&
                    it.packet.body["path"] == file.absolutePath &&
                    it.packet.body["syncFolderId"] == "folder-x"
            })
        }
    }

    @Test
    fun `handleLocalFileChange notifies listener`() {
        val file = tempFolder.newFile("changed.txt")
        file.writeText("updated")

        engine.start(emptyList())
        engine.handleLocalFileChange(FileObserver.CLOSE_WRITE, file.absolutePath, "folder-y")

        verify {
            mockListener.onFileChanged("file_modified", file.absolutePath, "folder-y")
        }
    }

    @Test
    fun `handleLocalFileChange for delete removes state and sends packet`() {
        // First track the file
        val path = "/fake/file.txt"
        stateManager.updateFileState(path, SyncFileInfo(path, "oldhash", 1000L, 50L))
        assertNotNull(stateManager.getFileState(path))

        engine.start(emptyList())
        engine.handleLocalFileChange(FileObserver.DELETE, path, "folder-z")

        // State should be removed
        assertNull(stateManager.getFileState(path))

        // Packet should be sent
        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync" &&
                    it.packet.body["action"] == "file_deleted" &&
                    it.packet.body["path"] == path
            })
        }
    }

    @Test
    fun `destroy stops engine and clears state`() {
        stateManager.updateFileState("/a", SyncFileInfo("/a", "h", 1L, 1L))
        engine.start(emptyList())
        assertTrue(engine.isRunning)
        assertEquals(1, stateManager.getAllStates().size)

        engine.destroy()

        assertFalse(engine.isRunning)
        assertTrue(stateManager.getAllStates().isEmpty())
    }
}
