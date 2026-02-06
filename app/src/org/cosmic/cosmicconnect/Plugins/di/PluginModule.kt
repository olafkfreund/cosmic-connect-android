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
import org.cosmic.cosmicconnect.Plugins.BatteryPlugin.BatteryPluginFFI
import org.cosmic.cosmicconnect.Plugins.CameraPlugin.CameraPlugin
import org.cosmic.cosmicconnect.Plugins.ClipboardPlugin.ClipboardPlugin
import org.cosmic.cosmicconnect.Plugins.ConnectivityReportPlugin.ConnectivityReportPlugin
import org.cosmic.cosmicconnect.Plugins.ContactsPlugin.ContactsPlugin
import org.cosmic.cosmicconnect.Plugins.ExtendedDisplayPlugin.ExtendedDisplayPlugin
import org.cosmic.cosmicconnect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmic.cosmicconnect.Plugins.NetworkInfoPlugin.NetworkInfoPlugin
import org.cosmic.cosmicconnect.Plugins.PowerPlugin.PowerPlugin
import org.cosmic.cosmicconnect.Plugins.FindRemoteDevicePlugin.FindRemoteDevicePlugin
import org.cosmic.cosmicconnect.Plugins.MousePadPlugin.MousePadPlugin
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisPlugin
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.NotificationsPlugin
import org.cosmic.cosmicconnect.Plugins.OpenOnPhonePlugin.OpenOnPhonePlugin
import org.cosmic.cosmicconnect.Plugins.OpenPlugin.OpenOnDesktopPlugin
import org.cosmic.cosmicconnect.Plugins.PingPlugin.PingPlugin
import org.cosmic.cosmicconnect.Plugins.PresenterPlugin.PresenterPlugin
import org.cosmic.cosmicconnect.Plugins.ReceiveNotificationsPlugin.ReceiveNotificationsPlugin
import org.cosmic.cosmicconnect.Plugins.RemoteKeyboardPlugin.RemoteKeyboardPlugin
import org.cosmic.cosmicconnect.Plugins.RunCommandPlugin.RunCommandPlugin
import org.cosmic.cosmicconnect.Plugins.SMSPlugin.SMSPlugin
import org.cosmic.cosmicconnect.Plugins.SftpPlugin.SftpPlugin
import org.cosmic.cosmicconnect.Plugins.SharePlugin.SharePlugin
import org.cosmic.cosmicconnect.Plugins.SystemVolumePlugin.SystemVolumePlugin
import org.cosmic.cosmicconnect.Plugins.TelephonyPlugin.TelephonyPlugin

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

    // ---- Wave 2 plugins ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("ClipboardPlugin")
    fun provideClipboardPluginCreator(factory: ClipboardPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("SystemVolumePlugin")
    fun provideSystemVolumePluginCreator(factory: SystemVolumePlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("RemoteKeyboardPlugin")
    fun provideRemoteKeyboardPluginCreator(factory: RemoteKeyboardPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("RunCommandPlugin")
    fun provideRunCommandPluginCreator(factory: RunCommandPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("OpenOnDesktopPlugin")
    fun provideOpenOnDesktopPluginCreator(factory: OpenOnDesktopPlugin.Factory): PluginCreator = factory

    // ---- Wave 3 plugins ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("BatteryPluginFFI")
    fun provideBatteryPluginCreator(factory: BatteryPluginFFI.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("FindMyPhonePlugin")
    fun provideFindMyPhonePluginCreator(factory: FindMyPhonePlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("ContactsPlugin")
    fun provideContactsPluginCreator(factory: ContactsPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("TelephonyPlugin")
    fun provideTelephonyPluginCreator(factory: TelephonyPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("SMSPlugin")
    fun provideSMSPluginCreator(factory: SMSPlugin.Factory): PluginCreator = factory

    // ---- Wave 4 plugins ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("SharePlugin")
    fun provideSharePluginCreator(factory: SharePlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("SftpPlugin")
    fun provideSftpPluginCreator(factory: SftpPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("MprisPlugin")
    fun provideMprisPluginCreator(factory: MprisPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("NotificationsPlugin")
    fun provideNotificationsPluginCreator(factory: NotificationsPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("ReceiveNotificationsPlugin")
    fun provideReceiveNotificationsPluginCreator(factory: ReceiveNotificationsPlugin.Factory): PluginCreator = factory

    // ---- Wave 5 plugins ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("OpenOnPhonePlugin")
    fun provideOpenOnPhonePluginCreator(factory: OpenOnPhonePlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("CameraPlugin")
    fun provideCameraPluginCreator(factory: CameraPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("ExtendedDisplayPlugin")
    fun provideExtendedDisplayPluginCreator(factory: ExtendedDisplayPlugin.Factory): PluginCreator = factory

    // ---- Wave 6 plugins (desktop plugin parity #145) ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("NetworkInfoPlugin")
    fun provideNetworkInfoPluginCreator(factory: NetworkInfoPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("PowerPlugin")
    fun providePowerPluginCreator(factory: PowerPlugin.Factory): PluginCreator = factory
}
