/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Plugins.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityReportPlugin
import org.cosmic.cosmicconnect.Plugins.FindRemoteDevicePlugin.FindRemoteDevicePlugin
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadPlugin
import org.cosmic.cosmicconnect.Plugins.PingPlugin.PingPlugin
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterPlugin

/**
 * Hilt module that binds migrated plugin factories into a
 * `Map<String, PluginCreator>` via `@IntoMap` + `@PluginKey`.
 *
 * As plugins are migrated from reflection to `@AssistedInject`,
 * add a new `@Provides @IntoMap` method here.
 */
@Module
@InstallIn(SingletonComponent::class)
object PluginModule {

    // ---- Wave 1 plugins ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("PingPlugin")
    fun providePingPluginCreator(factory: PingPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("FindRemoteDevicePlugin")
    fun provideFindRemoteDevicePluginCreator(factory: FindRemoteDevicePlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("ConnectivityReportPlugin")
    fun provideConnectivityReportPluginCreator(factory: ConnectivityReportPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("PresenterPlugin")
    fun providePresenterPluginCreator(factory: PresenterPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("MousePadPlugin")
    fun provideMousePadPluginCreator(factory: MousePadPlugin.Factory): PluginCreator = factory
}
