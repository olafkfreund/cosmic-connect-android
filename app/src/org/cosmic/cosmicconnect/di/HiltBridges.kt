/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.cosmic.cosmicconnect.Core.DeviceRegistry
import org.cosmic.cosmicconnect.Helpers.DeviceHelper
import org.cosmic.cosmicconnect.Helpers.TrustedDevices
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.RsaHelper
import org.cosmic.cosmicconnect.Helpers.SecurityHelpers.SslHelper
import org.cosmic.cosmicconnect.Plugins.MprisPlugin.MprisMediaSession
import org.cosmic.cosmicconnect.Plugins.PluginFactory

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HiltBridges {
    fun deviceRegistry(): DeviceRegistry
    fun deviceHelper(): DeviceHelper
    fun rsaHelper(): RsaHelper
    fun sslHelper(): SslHelper
    fun pluginFactory(): PluginFactory
    fun mprisMediaSession(): MprisMediaSession
    fun trustedDevices(): TrustedDevices
}
