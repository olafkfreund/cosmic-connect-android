/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.ScreenSharePlugin.ui

import android.util.Log
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.cosmicext.connect.Plugins.ScreenSharePlugin.streaming.StreamSession
import org.cosmicext.connect.Plugins.ScreenSharePlugin.streaming.StreamState

/**
 * ViewModel for ScreenShareViewerActivity.
 * Manages the stream session lifecycle and exposes state to the UI.
 */
class ScreenShareViewModel : ViewModel() {

    companion object {
        private const val TAG = "ScreenShareViewModel"
    }

    private val _streamState = MutableStateFlow<StreamState>(StreamState.Idle)
    val streamState: StateFlow<StreamState> = _streamState.asStateFlow()

    private var session: StreamSession? = null
    private var stateCollectorJob: Job? = null

    /**
     * Attaches a StreamSession to this ViewModel.
     * Must be called before onSurfaceReady().
     */
    fun attachSession(streamSession: StreamSession) {
        session = streamSession
        // Cancel any previous state collector to avoid coroutine leak
        stateCollectorJob?.cancel()
        // Mirror session state to our exposed flow
        stateCollectorJob = viewModelScope.launch {
            streamSession.state.collect { state ->
                _streamState.value = state
            }
        }
    }

    /**
     * Called when the SurfaceView surface is created and ready for rendering.
     * Starts the streaming loop in a coroutine.
     */
    fun onSurfaceReady(surface: Surface) {
        val currentSession = session
        if (currentSession == null) {
            Log.w(TAG, "No session attached, cannot start streaming")
            _streamState.value = StreamState.Error(IllegalStateException("No session attached"))
            return
        }

        viewModelScope.launch {
            try {
                currentSession.acceptAndStream(surface)
            } catch (e: Exception) {
                Log.e(TAG, "Stream error", e)
                _streamState.value = StreamState.Error(e)
            }
        }
    }

    /**
     * Disconnects and stops the streaming session.
     */
    fun disconnect() {
        session?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        session?.stop()
        session = null
    }
}
