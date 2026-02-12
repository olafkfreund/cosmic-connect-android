/*
 * SPDX-FileCopyrightText: 2018 Nicolas Fella <nicolas.fella@gmx.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.MprisReceiverPlugin

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.net.Uri
import android.util.Pair
import java.io.ByteArrayOutputStream
import java.util.*

class MprisReceiverCallback(private val plugin: MprisReceiverPlugin, private val player: MprisReceiverPlayer) : MediaController.Callback() {
    private var artHash: Long? = null
    private var displayArt: Bitmap? = null
    var artUrl: String? = null
        private set
    private var album: String? = null
    private var artist: String? = null

    init {
        // fetch the initial art, when player is already running and we start cosmicconnect
        val artAndUri = getArtAndUri(player.metadata)
        if (artAndUri != null) {
            val bitmap = artAndUri.first
            val hash = hashBitmap(bitmap)
            artHash = hash
            artUrl = makeArtUrl(hash, artAndUri.second)
            displayArt = bitmap
            album = player.album
            artist = player.artist
        }
    }

    private fun hashBitmap(bitmap: Bitmap): Long {
        val buffer = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(buffer, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return Arrays.hashCode(buffer).toLong()
    }

    private fun makeArtUrl(artHash: Long, artUrl: String): String {
        return Uri.parse(artUrl)
            .buildUpon()
            .appendQueryParameter("cconnectArtHash", artHash.toString())
            .build()
            .toString()
    }

    override fun onPlaybackStateChanged(state: PlaybackState?) {
        plugin.sendMetadata(player)
    }

    override fun onMetadataChanged(metadata: MediaMetadata?) {
        if (metadata == null) {
            artHash = null
            displayArt = null
            artUrl = null
            artist = null
            album = null
        } else {
            val artAndUri = getArtAndUri(metadata)
            val newAlbum = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val newArtist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            if (artAndUri == null) {
                // check if the album+artist is still the same- some players don't send art every time
                if (newAlbum != album || newArtist != artist) {
                    // there really is no new art
                    artHash = null
                    displayArt = null
                    artUrl = null
                    album = null
                    artist = null
                }
            } else {
                val newHash = hashBitmap(artAndUri.first)
                // In case the hashes are equal, we do a full comparison to protect against collisions
                if (newHash != artHash || !artAndUri.first.sameAs(displayArt)) {
                    artHash = newHash
                    displayArt = artAndUri.first
                    artUrl = makeArtUrl(newHash, artAndUri.second)
                    artist = newArtist
                    album = newAlbum
                }
            }
        }
        plugin.sendMetadata(player)
    }

    /**
     * Get the JPG art of the current track as a bytearray.
     *
     * @return null if no art is available, otherwise a JPEG image serialized into a bytearray
     */
    val artAsArray: ByteArray?
        get() {
            val currentArt = displayArt ?: return null
            val stream = ByteArrayOutputStream()
            currentArt.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            return stream.toByteArray()
        }

    companion object {
        private val PREFERRED_BITMAP_ORDER = arrayOf(
            MediaMetadata.METADATA_KEY_DISPLAY_ICON,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_ALBUM_ART
        )

        private val PREFERRED_URI_ORDER = arrayOf(
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ALBUM,
            MediaMetadata.METADATA_KEY_TITLE,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_ARTIST
        )

        private fun encodeAsUri(kind: String, data: String): String {
            return Uri.Builder()
                .scheme("cosmicextconnect")
                .path("/artUri")
                .appendQueryParameter(kind, data)
                .build().toString()
        }

        /**
         * Extract the art bitmap and corresponding uri from the media metadata.
         *
         * @return Pair of art, artUrl. May be null if either was not found.
         */
        @JvmStatic
        fun getArtAndUri(metadata: MediaMetadata?): Pair<Bitmap, String>? {
            if (metadata == null) return null
            var uri: String? = null
            var art: Bitmap? = null
            
            for (s in PREFERRED_BITMAP_ORDER) {
                val next = metadata.getBitmap(s)
                if (next != null) {
                    art = next
                    break
                }
            }
            
            for (s in PREFERRED_URI_ORDER) {
                val next = metadata.getString(s)
                if (!next.isNullOrEmpty()) {
                    val kind = when (s) {
                        MediaMetadata.METADATA_KEY_ALBUM -> "album"
                        MediaMetadata.METADATA_KEY_TITLE -> "title"
                        MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_ALBUM_ARTIST -> "artist"
                        else -> "orig"
                    }
                    uri = encodeAsUri(kind, next)
                    break
                }
            }

            if (art == null || uri == null) return null
            return Pair(art, uri)
        }
    }
}
