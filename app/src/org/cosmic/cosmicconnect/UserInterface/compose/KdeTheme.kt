/*
 * SPDX-FileCopyrightText: 2024 Albert Vaca Cintora <albertvaka@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * Legacy theme wrapper for backward compatibility.
 *
 * @deprecated Use [CosmicTheme] instead. This wrapper exists for backward compatibility
 * during the migration period and will be removed in a future version.
 *
 * KdeTheme has been replaced by CosmicTheme which includes:
 * - Custom COSMIC-branded color schemes
 * - Comprehensive typography system
 * - Consistent shape definitions
 * - Better Material3 integration
 */
@Deprecated(
  message = "Use CosmicTheme instead for enhanced design system support",
  replaceWith = ReplaceWith(
    "CosmicTheme(context, content = content)",
    "org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme"
  ),
  level = DeprecationLevel.WARNING
)
@Composable
fun KdeTheme(context: Context, content: @Composable () -> Unit) {
  // Delegate to CosmicTheme with default settings
  CosmicTheme(
    context = context,
    useDynamicColors = true,
    darkTheme = isSystemInDarkTheme(),
    content = content
  )
}
