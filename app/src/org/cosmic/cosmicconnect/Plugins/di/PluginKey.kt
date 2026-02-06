/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.di

import dagger.MapKey

/**
 * Dagger map key annotation for plugin multi-binding.
 *
 * Maps plugin key strings (e.g. "PingPlugin") to their [PluginCreator] factories
 * in the Hilt-managed plugin creator map.
 */
@MapKey
annotation class PluginKey(val value: String)
