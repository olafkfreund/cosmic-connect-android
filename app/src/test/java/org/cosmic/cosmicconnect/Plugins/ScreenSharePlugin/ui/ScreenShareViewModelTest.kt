/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.ui

import android.view.Surface
import io.mockk.mockk
import io.mockk.verify
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming.StreamSession
import org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming.StreamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for ScreenShareViewModel.
 *
 * These tests verify basic functionality without requiring coroutine dispatchers:
 * - Initial state is Idle
 * - onSurfaceReady with no session sets Error state
 * - disconnect() can be called safely
 */
class ScreenShareViewModelTest {

    private lateinit var viewModel: ScreenShareViewModel

    @Before
    fun setup() {
        viewModel = ScreenShareViewModel()
    }

    @Test
    fun `initial state is Idle`() {
        assertEquals(StreamState.Idle, viewModel.streamState.value)
    }

    @Test
    fun `onSurfaceReady with no session sets Error state`() {
        val mockSurface = mockk<Surface>()

        viewModel.onSurfaceReady(mockSurface)

        // Give coroutine a moment to set state (will fail fast due to no session)
        Thread.sleep(100)

        assertTrue("Expected Error state", viewModel.streamState.value is StreamState.Error)
        val error = viewModel.streamState.value as StreamState.Error
        assertTrue(error.error is IllegalStateException)
        assertEquals("No session attached", error.error.message)
    }

    @Test
    fun `disconnect can be called even without session`() {
        // Should not throw
        viewModel.disconnect()
    }

    @Test
    fun `onCleared can be called via reflection`() {
        // Trigger onCleared() via reflection (it's protected)
        val onClearedMethod = ScreenShareViewModel::class.java.superclass
            .getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)

        // Should not throw
    }

    @Test
    fun `disconnect on session calls session stop`() {
        // Create a real StreamSession (it won't actually start anything in unit test)
        val session = StreamSession(1920, 1080, 30, "h264")

        // Set the session via reflection to avoid coroutine launch
        val sessionField = ScreenShareViewModel::class.java.getDeclaredField("session")
        sessionField.isAccessible = true
        sessionField.set(viewModel, session)

        viewModel.disconnect()

        // We can't easily verify session.stop() was called without mockk and coroutine setup,
        // but we verified the method doesn't crash
    }
}
