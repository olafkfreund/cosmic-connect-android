/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.FileSyncPlugin

import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Core.NetworkPacket
import org.cosmic.cosmicconnect.Core.TransferPacket
import org.cosmic.cosmicconnect.Device
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class FileSyncPluginTest {

    private lateinit var context: Context
    private lateinit var mockDevice: Device
    private lateinit var plugin: FileSyncPlugin
    private lateinit var mockListener: FileSyncPlugin.Listener

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        mockDevice = mockk(relaxed = true)
        plugin = FileSyncPlugin(context, mockDevice)
        mockListener = mockk(relaxed = true)
        plugin.listener = mockListener
    }

    @Test
    fun `test plugin metadata`() {
        assertEquals("cconnect.filesync", FileSyncPlugin.PACKET_TYPE_FILESYNC)
        assertEquals("cconnect.filesync.request", FileSyncPlugin.PACKET_TYPE_FILESYNC_REQUEST)
        assertEquals("cconnect.filesync.conflict", FileSyncPlugin.PACKET_TYPE_FILESYNC_CONFLICT)
        assertFalse(plugin.isEnabledByDefault)
    }

    @Test
    fun `test supported packet types`() {
        assertTrue(plugin.supportedPacketTypes.contains("cconnect.filesync"))
        assertTrue(plugin.supportedPacketTypes.contains("cconnect.filesync.conflict"))
        assertEquals(2, plugin.supportedPacketTypes.size)
    }

    @Test
    fun `test outgoing packet types`() {
        assertTrue(plugin.outgoingPacketTypes.contains("cconnect.filesync.request"))
        assertEquals(1, plugin.outgoingPacketTypes.size)
    }

    @Test
    fun `test file_added notification`() {
        val np = NetworkPacket(
            id = 1L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "file_added",
                "path" to "documents/test.txt",
                "checksum" to "abc123",
                "size" to 1024L,
                "timestamp" to 1675000000000L,
                "syncFolderId" to "folder-123",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
        verify { mockListener.onFileChanged("file_added", "documents/test.txt", "folder-123") }
    }

    @Test
    fun `test file_changed notification`() {
        val np = NetworkPacket(
            id = 2L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "file_changed",
                "path" to "photos/vacation.jpg",
                "syncFolderId" to "folder-456",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
        verify { mockListener.onFileChanged("file_changed", "photos/vacation.jpg", "folder-456") }
    }

    @Test
    fun `test file_deleted notification`() {
        val np = NetworkPacket(
            id = 3L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "file_deleted",
                "path" to "old_file.dat",
                "syncFolderId" to "folder-789",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
        verify { mockListener.onFileChanged("file_deleted", "old_file.dat", "folder-789") }
    }

    @Test
    fun `test sync_started notification`() {
        val np = NetworkPacket(
            id = 4L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "sync_started",
                "path" to "",
                "syncFolderId" to "folder-start",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
        verify { mockListener.onSyncStatusChanged("folder-start", FileSyncPlugin.SyncStatus.SYNCING) }
    }

    @Test
    fun `test sync_complete notification`() {
        val np = NetworkPacket(
            id = 5L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "sync_complete",
                "path" to "",
                "syncFolderId" to "folder-complete",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
        verify { mockListener.onSyncStatusChanged("folder-complete", FileSyncPlugin.SyncStatus.COMPLETE) }
    }

    @Test
    fun `test conflict notification`() {
        val np = NetworkPacket(
            id = 6L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "docs/report.docx",
                "localChecksum" to "local-hash-123",
                "remoteChecksum" to "remote-hash-456",
                "localTimestamp" to 1675000000000L,
                "remoteTimestamp" to 1675001000000L,
                "syncFolderId" to "folder-conflict",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)

        val conflictList = plugin.conflicts
        assertEquals(1, conflictList.size)

        val conflict = conflictList[0]
        assertEquals("docs/report.docx", conflict.path)
        assertEquals("local-hash-123", conflict.localChecksum)
        assertEquals("remote-hash-456", conflict.remoteChecksum)
        assertEquals(1675000000000L, conflict.localTimestamp)
        assertEquals(1675001000000L, conflict.remoteTimestamp)
        assertEquals("folder-conflict", conflict.syncFolderId)

        verify { mockListener.onConflictDetected(conflict) }
    }

    @Test
    fun `test conflict deduplication by path`() {
        // Send first conflict for a path
        val np1 = NetworkPacket(
            id = 1L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "docs/report.docx",
                "localChecksum" to "old-local",
                "remoteChecksum" to "old-remote",
                "localTimestamp" to 1000L,
                "remoteTimestamp" to 2000L,
                "syncFolderId" to "folder-1",
            ),
        )
        plugin.onPacketReceived(TransferPacket(np1))
        assertEquals(1, plugin.conflicts.size)

        // Send second conflict for the same path (should replace, not add)
        val np2 = NetworkPacket(
            id = 2L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "docs/report.docx",
                "localChecksum" to "new-local",
                "remoteChecksum" to "new-remote",
                "localTimestamp" to 3000L,
                "remoteTimestamp" to 4000L,
                "syncFolderId" to "folder-1",
            ),
        )
        plugin.onPacketReceived(TransferPacket(np2))

        // Should still be 1 conflict, with the updated values
        val conflictList = plugin.conflicts
        assertEquals(1, conflictList.size)
        assertEquals("new-local", conflictList[0].localChecksum)
        assertEquals(3000L, conflictList[0].localTimestamp)
    }

    @Test
    fun `test missing action field`() {
        val np = NetworkPacket(
            id = 7L,
            type = "cconnect.filesync",
            body = mapOf(
                "path" to "test.txt",
                "syncFolderId" to "folder-missing",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        // Plugin returns true even if fields are missing (graceful degradation)
        assertTrue(result)
    }

    @Test
    fun `test missing path field`() {
        val np = NetworkPacket(
            id = 8L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "file_added",
                "syncFolderId" to "folder-missing",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
    }

    @Test
    fun `test missing syncFolderId field`() {
        val np = NetworkPacket(
            id = 9L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "file_added",
                "path" to "test.txt",
            ),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)
    }

    @Test
    fun `test requestSync sends correct packet`() {
        plugin.requestSync("folder-sync")

        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync.request" &&
                it.packet.body["requestSync"] == true &&
                it.packet.body["syncFolderId"] == "folder-sync"
            })
        }
    }

    @Test
    fun `test addSyncFolder sends correct packet`() {
        plugin.addSyncFolder("/home/user/Documents")

        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync.request" &&
                it.packet.body["addSyncFolder"] == "/home/user/Documents"
            })
        }
    }

    @Test
    fun `test addSyncFolder rejects path traversal`() {
        plugin.addSyncFolder("/home/user/../../../etc/passwd")

        // Should NOT send any packet for invalid path
        verify(exactly = 0) {
            mockDevice.sendPacket(match {
                it.packet.body.containsKey("addSyncFolder")
            })
        }
    }

    @Test
    fun `test addSyncFolder rejects blank path`() {
        plugin.addSyncFolder("")

        verify(exactly = 0) {
            mockDevice.sendPacket(match {
                it.packet.body.containsKey("addSyncFolder")
            })
        }
    }

    @Test
    fun `test addSyncFolder rejects relative path`() {
        plugin.addSyncFolder("relative/path")

        verify(exactly = 0) {
            mockDevice.sendPacket(match {
                it.packet.body.containsKey("addSyncFolder")
            })
        }
    }

    @Test
    fun `test addSyncFolder rejects system paths`() {
        val blockedPaths = listOf(
            "/data/data/com.example/files",
            "/system/bin/sh",
            "/proc/self/maps",
            "/sys/class/net",
            "/dev/urandom",
        )

        for (path in blockedPaths) {
            plugin.addSyncFolder(path)
        }

        verify(exactly = 0) {
            mockDevice.sendPacket(match {
                it.packet.body.containsKey("addSyncFolder")
            })
        }
    }

    @Test
    fun `test removeSyncFolder sends correct packet`() {
        plugin.removeSyncFolder("folder-remove")

        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync.request" &&
                it.packet.body["removeSyncFolder"] == "folder-remove"
            })
        }
    }

    @Test
    fun `test requestSyncFolderList sends correct packet`() {
        plugin.requestSyncFolderList()

        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync.request" &&
                it.packet.body["listSyncFolders"] == true
            })
        }
    }

    @Test
    fun `test onCreate calls requestSyncFolderList`() {
        val newPlugin = FileSyncPlugin(context, mockDevice)
        newPlugin.onCreate()

        verify {
            mockDevice.sendPacket(match {
                it.packet.type == "cconnect.filesync.request" &&
                it.packet.body["listSyncFolders"] == true
            })
        }
    }

    @Test
    fun `test onPacketReceived returns true for valid filesync packet`() {
        val np = NetworkPacket(
            id = 10L,
            type = "cconnect.filesync",
            body = mapOf(
                "action" to "file_added",
                "path" to "test.txt",
                "syncFolderId" to "folder-123",
            ),
        )

        assertTrue(plugin.onPacketReceived(TransferPacket(np)))
    }

    @Test
    fun `test onPacketReceived returns true for valid conflict packet`() {
        val np = NetworkPacket(
            id = 11L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "test.txt",
                "localChecksum" to "abc",
                "remoteChecksum" to "def",
                "localTimestamp" to 1000L,
                "remoteTimestamp" to 2000L,
                "syncFolderId" to "folder-123",
            ),
        )

        assertTrue(plugin.onPacketReceived(TransferPacket(np)))
    }

    @Test
    fun `test getSyncFolders returns empty list initially`() {
        val folders = plugin.getSyncFolders()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun `test onDestroy clears state`() {
        // Add a conflict first
        val np = NetworkPacket(
            id = 1L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "test.txt",
                "localChecksum" to "abc",
                "remoteChecksum" to "def",
                "localTimestamp" to 1000L,
                "remoteTimestamp" to 2000L,
                "syncFolderId" to "folder-1",
            ),
        )
        plugin.onPacketReceived(TransferPacket(np))
        assertEquals(1, plugin.conflicts.size)

        plugin.onDestroy()

        // Conflicts should be cleared
        assertEquals(0, plugin.conflicts.size)
        // Listener should be null
        assertNull(plugin.listener)
        // Sync folders should be cleared
        assertTrue(plugin.getSyncFolders().isEmpty())
    }

    @Test
    fun `test folder list response populates sync folders`() {
        val foldersArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "folder-1")
                put("path", "/home/user/Documents")
            })
            put(JSONObject().apply {
                put("id", "folder-2")
                put("path", "/home/user/Photos")
            })
        }
        val np = NetworkPacket(
            id = 100L,
            type = "cconnect.filesync",
            body = mapOf("folders" to foldersArr.toString()),
        )

        val result = plugin.onPacketReceived(TransferPacket(np))
        assertTrue(result)

        val folders = plugin.getSyncFolders()
        assertEquals(2, folders.size)
        assertEquals("folder-1", folders[0].id)
        assertEquals("/home/user/Documents", folders[0].path)
        assertEquals(FileSyncPlugin.SyncStatus.IDLE, folders[0].syncStatus)
        assertEquals("folder-2", folders[1].id)
        assertEquals("/home/user/Photos", folders[1].path)
    }

    @Test
    fun `test folder list response rejects invalid paths`() {
        val foldersArr = JSONArray().apply {
            put(JSONObject().apply {
                put("id", "folder-good")
                put("path", "/home/user/Documents")
            })
            put(JSONObject().apply {
                put("id", "folder-bad")
                put("path", "/proc/self/maps")
            })
        }
        val np = NetworkPacket(
            id = 101L,
            type = "cconnect.filesync",
            body = mapOf("folders" to foldersArr.toString()),
        )

        plugin.onPacketReceived(TransferPacket(np))

        val folders = plugin.getSyncFolders()
        // Only the valid folder should be added
        assertEquals(1, folders.size)
        assertEquals("folder-good", folders[0].id)
    }

    @Test
    fun `test onPacketReceived returns false for unknown packet type`() {
        val np = NetworkPacket(
            id = 200L,
            type = "cconnect.unknown",
            body = emptyMap(),
        )

        assertFalse(plugin.onPacketReceived(TransferPacket(np)))
    }

    @Test
    fun `test conflicts returns immutable snapshot`() {
        // Add two distinct conflicts
        val np1 = NetworkPacket(
            id = 1L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "file1.txt",
                "localChecksum" to "a",
                "remoteChecksum" to "b",
                "localTimestamp" to 1000L,
                "remoteTimestamp" to 2000L,
                "syncFolderId" to "f1",
            ),
        )
        val np2 = NetworkPacket(
            id = 2L,
            type = "cconnect.filesync.conflict",
            body = mapOf(
                "path" to "file2.txt",
                "localChecksum" to "c",
                "remoteChecksum" to "d",
                "localTimestamp" to 3000L,
                "remoteTimestamp" to 4000L,
                "syncFolderId" to "f2",
            ),
        )
        plugin.onPacketReceived(TransferPacket(np1))

        // Take a snapshot
        val snapshot = plugin.conflicts
        assertEquals(1, snapshot.size)

        // Add another conflict
        plugin.onPacketReceived(TransferPacket(np2))

        // Snapshot should not have changed (it's a copy)
        assertEquals(1, snapshot.size)
        // But the live list should have 2
        assertEquals(2, plugin.conflicts.size)
    }
}
