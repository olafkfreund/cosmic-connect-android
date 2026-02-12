/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmicext.connect.Plugins.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.cosmicext.connect.Plugins.BatteryPlugin.BatteryPluginFFI
import org.cosmicext.connect.Plugins.CameraPlugin.CameraPlugin
import org.cosmicext.connect.Plugins.ClipboardPlugin.ClipboardPlugin
import org.cosmicext.connect.Plugins.ConnectivityReportPlugin.ConnectivityReportPlugin
import org.cosmicext.connect.Plugins.ContactsPlugin.ContactsPlugin
import org.cosmicext.connect.Plugins.ExtendedDisplayPlugin.ExtendedDisplayPlugin
import org.cosmicext.connect.Plugins.FindMyPhonePlugin.FindMyPhonePlugin
import org.cosmicext.connect.Plugins.FileSyncPlugin.FileSyncPlugin
import org.cosmicext.connect.Plugins.LockPlugin.LockPlugin
import org.cosmicext.connect.Plugins.ScreenSharePlugin.ScreenSharePlugin
import org.cosmicext.connect.Plugins.NetworkInfoPlugin.NetworkInfoPlugin
import org.cosmicext.connect.Plugins.VirtualMonitorPlugin.VirtualMonitorPlugin
import org.cosmicext.connect.Plugins.PowerPlugin.PowerPlugin
import org.cosmicext.connect.Plugins.AudioStreamPlugin.AudioStreamPlugin
import org.cosmicext.connect.Plugins.FindRemoteDevicePlugin.FindRemoteDevicePlugin
import org.cosmicext.connect.Plugins.MousePadPlugin.MousePadPlugin
import org.cosmicext.connect.Plugins.MprisPlugin.MprisPlugin
import org.cosmicext.connect.Plugins.NotificationsPlugin.NotificationsPlugin
import org.cosmicext.connect.Plugins.OpenOnPhonePlugin.OpenOnPhonePlugin
import org.cosmicext.connect.Plugins.OpenPlugin.OpenOnDesktopPlugin
import org.cosmicext.connect.Plugins.PingPlugin.PingPlugin
import org.cosmicext.connect.Plugins.PresenterPlugin.PresenterPlugin
import org.cosmicext.connect.Plugins.ReceiveNotificationsPlugin.ReceiveNotificationsPlugin
import org.cosmicext.connect.Plugins.RemoteKeyboardPlugin.RemoteKeyboardPlugin
import org.cosmicext.connect.Plugins.RunCommandPlugin.RunCommandPlugin
import org.cosmicext.connect.Plugins.SMSPlugin.SMSPlugin
import org.cosmicext.connect.Plugins.SftpPlugin.SftpPlugin
import org.cosmicext.connect.Plugins.SharePlugin.SharePlugin
import org.cosmicext.connect.Plugins.SystemVolumePlugin.SystemVolumePlugin
import org.cosmicext.connect.Plugins.TelephonyPlugin.TelephonyPlugin
import org.cosmicext.connect.Plugins.WebcamPlugin.WebcamPlugin

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

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("LockPlugin")
    fun provideLockPluginCreator(factory: LockPlugin.Factory): PluginCreator = factory

    // ---- Wave 7 plugins (desktop plugin parity #145 continued) ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("ScreenSharePlugin")
    fun provideScreenSharePluginCreator(factory: ScreenSharePlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("FileSyncPlugin")
    fun provideFileSyncPluginCreator(factory: FileSyncPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("VirtualMonitorPlugin")
    fun provideVirtualMonitorPluginCreator(factory: VirtualMonitorPlugin.Factory): PluginCreator = factory

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("AudioStreamPlugin")
    fun provideAudioStreamPluginCreator(factory: AudioStreamPlugin.Factory): PluginCreator = factory

    // ---- Wave 8 plugins (webcam #158) ----

    @Provides
    @dagger.multibindings.IntoMap
    @PluginKey("WebcamPlugin")
    fun provideWebcamPluginCreator(factory: WebcamPlugin.Factory): PluginCreator = factory
}
