package org.cosmicext.connect.Plugins.SftpPlugin

import android.net.Uri
import androidx.core.net.toUri
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StorageInfoTest {

    @Test
    fun `toJSON produces correct JSON with DisplayName and Uri keys`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Internal Storage",
            uri = "content://com.android.externalstorage.documents/tree/primary".toUri()
        )

        val json = storageInfo.toJSON()

        assertEquals("Internal Storage", json.getString("DisplayName"))
        assertEquals("content://com.android.externalstorage.documents/tree/primary", json.getString("Uri"))
    }

    @Test
    fun `fromJSON parses JSON back to StorageInfo correctly`() {
        val json = JSONObject().apply {
            put("DisplayName", "External SD Card")
            put("Uri", "content://com.android.externalstorage.documents/tree/1234-5678")
        }

        val storageInfo = SftpPlugin.StorageInfo.fromJSON(json)

        assertEquals("External SD Card", storageInfo.displayName)
        assertEquals("content://com.android.externalstorage.documents/tree/1234-5678", storageInfo.uri.toString())
    }

    @Test
    fun `round trip toJSON then fromJSON preserves data`() {
        val original = SftpPlugin.StorageInfo(
            displayName = "My Documents",
            uri = "file:///storage/emulated/0/Documents".toUri()
        )

        val json = original.toJSON()
        val restored = SftpPlugin.StorageInfo.fromJSON(json)

        assertEquals(original.displayName, restored.displayName)
        assertEquals(original.uri, restored.uri)
    }

    @Test
    fun `isFileUri returns true for file scheme`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "File Storage",
            uri = "file:///storage/emulated/0".toUri()
        )

        assertTrue(storageInfo.isFileUri)
        assertFalse(storageInfo.isContentUri)
    }

    @Test
    fun `isContentUri returns true for content scheme`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Content Storage",
            uri = "content://com.android.providers.downloads.documents/tree/downloads".toUri()
        )

        assertTrue(storageInfo.isContentUri)
        assertFalse(storageInfo.isFileUri)
    }

    @Test
    fun `isFileUri returns false for content scheme`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "SAF Storage",
            uri = "content://authority/path".toUri()
        )

        assertFalse(storageInfo.isFileUri)
    }

    @Test
    fun `isContentUri returns false for file scheme`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Native Storage",
            uri = "file:///data/local".toUri()
        )

        assertFalse(storageInfo.isContentUri)
    }

    @Test(expected = JSONException::class)
    fun `fromJSON throws JSONException when DisplayName key is missing`() {
        val json = JSONObject().apply {
            put("Uri", "content://authority/path")
            // Missing DisplayName
        }

        SftpPlugin.StorageInfo.fromJSON(json)
    }

    @Test(expected = JSONException::class)
    fun `fromJSON throws JSONException when Uri key is missing`() {
        val json = JSONObject().apply {
            put("DisplayName", "Test Storage")
            // Missing Uri
        }

        SftpPlugin.StorageInfo.fromJSON(json)
    }

    @Test(expected = JSONException::class)
    fun `fromJSON throws JSONException when both keys are missing`() {
        val json = JSONObject()

        SftpPlugin.StorageInfo.fromJSON(json)
    }

    @Test
    fun `toJSON handles special characters in displayName`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "SD Card (External) - 128GB",
            uri = "content://authority/path".toUri()
        )

        val json = storageInfo.toJSON()
        val restored = SftpPlugin.StorageInfo.fromJSON(json)

        assertEquals("SD Card (External) - 128GB", restored.displayName)
    }

    @Test
    fun `toJSON handles URIs with query parameters`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Cloud Storage",
            uri = "content://authority/path?param1=value1&param2=value2".toUri()
        )

        val json = storageInfo.toJSON()
        val restored = SftpPlugin.StorageInfo.fromJSON(json)

        assertEquals("content://authority/path?param1=value1&param2=value2", restored.uri.toString())
    }

    @Test
    fun `toJSON handles URIs with fragments`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Fragment Storage",
            uri = "content://authority/path#fragment".toUri()
        )

        val json = storageInfo.toJSON()
        val restored = SftpPlugin.StorageInfo.fromJSON(json)

        assertEquals("content://authority/path#fragment", restored.uri.toString())
    }

    @Test
    fun `isFileUri and isContentUri work with different schemes`() {
        val httpStorageInfo = SftpPlugin.StorageInfo(
            displayName = "HTTP Storage",
            uri = "http://example.com/path".toUri()
        )

        assertFalse(httpStorageInfo.isFileUri)
        assertFalse(httpStorageInfo.isContentUri)
    }

    @Test
    fun `displayName can be modified but uri is immutable`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Original Name",
            uri = "content://authority/path".toUri()
        )

        // displayName is var, so it can be modified
        storageInfo.displayName = "Modified Name"

        assertEquals("Modified Name", storageInfo.displayName)
        assertEquals("content://authority/path", storageInfo.uri.toString())
    }

    @Test
    fun `toJSON after displayName modification reflects new name`() {
        val storageInfo = SftpPlugin.StorageInfo(
            displayName = "Original",
            uri = "content://path".toUri()
        )

        storageInfo.displayName = "Updated"
        val json = storageInfo.toJSON()

        assertEquals("Updated", json.getString("DisplayName"))
    }
}
