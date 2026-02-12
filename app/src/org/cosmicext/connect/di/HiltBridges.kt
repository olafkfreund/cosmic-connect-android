/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Helpers.DeviceHelper
import org.cosmicext.connect.Helpers.TrustedDevices
import org.cosmicext.connect.Helpers.SecurityHelpers.RsaHelper
import org.cosmicext.connect.Helpers.SecurityHelpers.SslHelper
import org.cosmicext.connect.Plugins.MprisPlugin.MprisMediaSession
import org.cosmicext.connect.Plugins.PluginFactory

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
