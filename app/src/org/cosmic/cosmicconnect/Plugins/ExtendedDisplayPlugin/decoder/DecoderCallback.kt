/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.decoder

/**
 * Callback interface for video decoder events.
 */
interface DecoderCallback {
    /**
     * Called when a frame has been decoded and rendered.
     *
     * @param presentationTimeUs The presentation timestamp in microseconds
     */
    fun onFrameDecoded(presentationTimeUs: Long)

    /**
     * Called when the output format changes (e.g., resolution change).
     *
     * @param width New width in pixels
     * @param height New height in pixels
     */
    fun onFormatChanged(width: Int, height: Int)

    /**
     * Called when a decoder error occurs.
     *
     * @param error The exception that occurred
     */
    fun onDecoderError(error: Exception)
}
