/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.di

/**
 * Annotation providing static plugin metadata without needing to instantiate
 * the plugin class. Used by [org.cosmicext.connect.Plugins.PluginFactory]
 * to build [org.cosmicext.connect.Plugins.PluginFactory.PluginInfo] from
 * annotation values rather than from a temporary plugin instance.
 *
 * Only applied to plugins that have been migrated to `@AssistedInject`.
 * Legacy plugins continue using reflection-based metadata extraction.
 *
 * Note: [displayNameRes] and [descriptionRes] are string resource IDs (R.string.*).
 * We intentionally omit @StringRes to avoid KSP/Dagger annotation processing conflicts.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PluginMetadata(
    val pluginKey: String,
    val supportedPacketTypes: Array<String>,
    val outgoingPacketTypes: Array<String>,
    val displayNameRes: Int,
    val descriptionRes: Int,
    val isEnabledByDefault: Boolean = true,
    val hasSettings: Boolean = false,
    val listenToUnpaired: Boolean = false,
)
