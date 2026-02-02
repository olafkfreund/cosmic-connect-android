/*
 * SPDX-FileCopyrightText: 2026 cosmic-connect-android team
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * SignalingMessage - Type-safe signaling protocol messages
 *
 * This sealed class hierarchy represents the different message types exchanged
 * during WebRTC signaling between Android and COSMIC Desktop.
 *
 * ## Message Types
 *
 * - **Offer**: SDP offer from the initiating peer (desktop)
 * - **Answer**: SDP answer from the responding peer (Android)
 * - **IceCandidate**: ICE candidate for connection negotiation
 *
 * ## Protocol Flow
 *
 * 1. Desktop creates offer → sends Offer message
 * 2. Android receives offer → creates answer → sends Answer message
 * 3. Both peers exchange IceCandidate messages until connection established
 */
sealed class SignalingMessage {

    /**
     * Message type discriminator for JSON serialization
     */
    abstract val type: String

    /**
     * Offer message containing SDP offer from initiating peer
     *
     * @property sdp Session Description Protocol offer string
     */
    @JsonClass(generateAdapter = true)
    data class Offer(
        @Json(name = "sdp")
        val sdp: String
    ) : SignalingMessage() {
        @Json(name = "type")
        override val type: String = "offer"
    }

    /**
     * Answer message containing SDP answer from responding peer
     *
     * @property sdp Session Description Protocol answer string
     */
    @JsonClass(generateAdapter = true)
    data class Answer(
        @Json(name = "sdp")
        val sdp: String
    ) : SignalingMessage() {
        @Json(name = "type")
        override val type: String = "answer"
    }

    /**
     * ICE candidate message for connection establishment
     *
     * @property candidate ICE candidate string (format: "candidate:...")
     * @property sdpMid Media stream identification tag
     * @property sdpMLineIndex Media line index in SDP
     */
    @JsonClass(generateAdapter = true)
    data class IceCandidate(
        @Json(name = "candidate")
        val candidate: String,
        @Json(name = "sdpMid")
        val sdpMid: String?,
        @Json(name = "sdpMLineIndex")
        val sdpMLineIndex: Int?
    ) : SignalingMessage() {
        @Json(name = "type")
        override val type: String = "candidate"
    }

    companion object {
        /**
         * Message type constants for discriminator matching
         */
        const val TYPE_OFFER = "offer"
        const val TYPE_ANSWER = "answer"
        const val TYPE_CANDIDATE = "candidate"
    }
}
