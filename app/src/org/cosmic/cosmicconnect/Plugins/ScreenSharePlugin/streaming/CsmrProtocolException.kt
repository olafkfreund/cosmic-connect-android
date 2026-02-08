/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.ScreenSharePlugin.streaming

import java.io.IOException

/**
 * Thrown when the CSMR protocol is violated (bad magic, oversized payload, etc.).
 */
class CsmrProtocolException(message: String) : IOException(message)
