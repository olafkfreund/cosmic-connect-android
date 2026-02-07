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
import org.cosmic.cosmicconnect.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        // Add folder to track sync status
        plugin.addSyncFolder("/test/path")

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
        assertEquals(1, plugin.conflicts.size)

        val conflict = plugin.conflicts[0]
        assertEquals("docs/report.docx", conflict.path)
        assertEquals("local-hash-123", conflict.localChecksum)
        assertEquals("remote-hash-456", conflict.remoteChecksum)
        assertEquals(1675000000000L, conflict.localTimestamp)
        assertEquals(1675001000000L, conflict.remoteTimestamp)
        assertEquals("folder-conflict", conflict.syncFolderId)

        verify { mockListener.onConflictDetected(conflict) }
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
}
