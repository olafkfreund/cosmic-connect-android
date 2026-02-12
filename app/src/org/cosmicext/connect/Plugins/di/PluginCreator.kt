/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.di

import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.Plugin

/**
 * Type-erased factory interface for creating [Plugin] instances.
 *
 * Each migrated plugin defines an inner `@AssistedFactory` interface that
 * extends this, allowing Hilt to provide a `Map<String, PluginCreator>`
 * to [org.cosmicext.connect.Plugins.PluginFactory].
 *
 * The [Device] parameter is provided at runtime (not at DI graph construction time),
 * which is why we use `@AssistedInject` / `@AssistedFactory` rather than regular `@Inject`.
 */
interface PluginCreator {
    fun create(device: Device): Plugin
}
