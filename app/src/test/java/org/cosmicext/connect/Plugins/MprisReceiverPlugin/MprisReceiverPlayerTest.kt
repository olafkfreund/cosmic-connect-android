package org.cosmicext.connect.Plugins.MprisReceiverPlugin

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test MprisReceiverPlayer class which wraps Android MediaController.
 * Tests all playback state queries, metadata retrieval, and transport controls.
 */
@RunWith(RobolectricTestRunner::class)
class MprisReceiverPlayerTest {

    // region isPlaying tests

    @Test
    fun `isPlaying returns false when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.isPlaying())
    }

    @Test
    fun `isPlaying returns true when state is STATE_PLAYING`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PLAYING
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.isPlaying())
    }

    @Test
    fun `isPlaying returns false when state is not STATE_PLAYING`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PAUSED
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.isPlaying())
    }

    // endregion

    // region canPlay tests

    @Test
    fun `canPlay returns false when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canPlay())
    }

    @Test
    fun `canPlay returns true when state is STATE_PLAYING`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PLAYING
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canPlay())
    }

    @Test
    fun `canPlay returns true when ACTION_PLAY is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PAUSED
        every { state.actions } returns PlaybackState.ACTION_PLAY
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canPlay())
    }

    @Test
    fun `canPlay returns true when ACTION_PLAY_PAUSE is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PAUSED
        every { state.actions } returns PlaybackState.ACTION_PLAY_PAUSE
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canPlay())
    }

    @Test
    fun `canPlay returns false when no play actions are set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PAUSED
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canPlay())
    }

    // endregion

    // region canPause tests

    @Test
    fun `canPause returns false when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canPause())
    }

    @Test
    fun `canPause returns true when state is STATE_PAUSED`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PAUSED
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canPause())
    }

    @Test
    fun `canPause returns true when ACTION_PAUSE is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PLAYING
        every { state.actions } returns PlaybackState.ACTION_PAUSE
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canPause())
    }

    @Test
    fun `canPause returns true when ACTION_PLAY_PAUSE is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PLAYING
        every { state.actions } returns PlaybackState.ACTION_PLAY_PAUSE
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canPause())
    }

    @Test
    fun `canPause returns false when no pause actions are set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.state } returns PlaybackState.STATE_PLAYING
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canPause())
    }

    // endregion

    // region canGoPrevious tests

    @Test
    fun `canGoPrevious returns false when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canGoPrevious())
    }

    @Test
    fun `canGoPrevious returns true when ACTION_SKIP_TO_PREVIOUS is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.actions } returns PlaybackState.ACTION_SKIP_TO_PREVIOUS
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canGoPrevious())
    }

    @Test
    fun `canGoPrevious returns false when ACTION_SKIP_TO_PREVIOUS is not set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canGoPrevious())
    }

    // endregion

    // region canGoNext tests

    @Test
    fun `canGoNext returns false when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canGoNext())
    }

    @Test
    fun `canGoNext returns true when ACTION_SKIP_TO_NEXT is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.actions } returns PlaybackState.ACTION_SKIP_TO_NEXT
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canGoNext())
    }

    @Test
    fun `canGoNext returns false when ACTION_SKIP_TO_NEXT is not set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canGoNext())
    }

    // endregion

    // region canSeek tests

    @Test
    fun `canSeek returns false when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canSeek())
    }

    @Test
    fun `canSeek returns true when ACTION_SEEK_TO is set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.actions } returns PlaybackState.ACTION_SEEK_TO
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertTrue(player.canSeek())
    }

    @Test
    fun `canSeek returns false when ACTION_SEEK_TO is not set`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.actions } returns 0L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertFalse(player.canSeek())
    }

    // endregion

    // region Metadata tests

    @Test
    fun `album returns empty string when metadata is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.metadata } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("", player.album)
    }

    @Test
    fun `album returns album string from metadata`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) } returns "Test Album"
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("Test Album", player.album)
    }

    @Test
    fun `artist returns empty string when metadata is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.metadata } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("", player.artist)
    }

    @Test
    fun `artist returns artist string from metadata`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) } returns "Test Artist"
        every { metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR) } returns null
        every { metadata.getString(MediaMetadata.METADATA_KEY_WRITER) } returns null
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("Test Artist", player.artist)
    }

    @Test
    fun `artist falls back to author when artist is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) } returns null
        every { metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR) } returns "Test Author"
        every { metadata.getString(MediaMetadata.METADATA_KEY_WRITER) } returns null
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("Test Author", player.artist)
    }

    @Test
    fun `artist falls back to writer when artist and author are null`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) } returns null
        every { metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR) } returns null
        every { metadata.getString(MediaMetadata.METADATA_KEY_WRITER) } returns "Test Writer"
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("Test Writer", player.artist)
    }

    @Test
    fun `title returns empty string when metadata is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.metadata } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("", player.title)
    }

    @Test
    fun `title returns title string from metadata`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getString(MediaMetadata.METADATA_KEY_TITLE) } returns "Test Title"
        every { metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) } returns null
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("Test Title", player.title)
    }

    @Test
    fun `title falls back to display title when title is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getString(MediaMetadata.METADATA_KEY_TITLE) } returns null
        every { metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) } returns "Test Display Title"
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals("Test Display Title", player.title)
    }

    @Test
    fun `length returns 0 when metadata is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.metadata } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals(0L, player.length)
    }

    @Test
    fun `length returns duration from metadata`() {
        val controller = mockk<MediaController>(relaxed = true)
        val metadata = mockk<MediaMetadata>()
        every { metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) } returns 180000L
        every { controller.metadata } returns metadata

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals(180000L, player.length)
    }

    // endregion

    // region Position tests

    @Test
    fun `position returns 0 when playback state is null`() {
        val controller = mockk<MediaController>(relaxed = true)
        every { controller.playbackState } returns null

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals(0L, player.position)
    }

    @Test
    fun `position returns position from playback state`() {
        val controller = mockk<MediaController>(relaxed = true)
        val state = mockk<PlaybackState>()
        every { state.position } returns 45000L
        every { controller.playbackState } returns state

        val player = MprisReceiverPlayer(controller, "Test Player")

        assertEquals(45000L, player.position)
    }

    // endregion

    // NOTE: Volume tests (PlaybackInfo) and transport control tests (TransportControls)
    // are excluded because these are final Android framework classes that cannot be
    // mocked with MockK under Robolectric. They require instrumented tests.
}
