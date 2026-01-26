/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * COSMIC Connect theme following Material Design 3 principles.
 *
 * Features:
 * - Dynamic color support on Android 12+ (Material You)
 * - Custom COSMIC-branded color schemes on older Android versions
 * - Consistent typography using Material3 type scale
 * - Rounded shapes for modern, friendly interface
 * - Dark and light theme support with automatic switching
 *
 * @param context Android context for dynamic color generation
 * @param useDynamicColors Whether to use dynamic colors on supported devices (default: true)
 * @param darkTheme Whether to use dark theme (default: system preference)
 * @param content Composable content to be themed
 */
@Composable
fun CosmicTheme(
  context: Context,
  useDynamicColors: Boolean = true,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  // Determine color scheme based on Android version and preferences
  val colorScheme = when {
    // Use dynamic colors on Android 12+ if enabled
    useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) {
        dynamicDarkColorScheme(context)
      } else {
        dynamicLightColorScheme(context)
      }
    }
    // Use custom COSMIC color schemes on older versions or if dynamic colors disabled
    darkTheme -> CosmicDarkColorScheme
    else -> CosmicLightColorScheme
  }

  // Provide color scheme through composition local for custom components
  CompositionLocalProvider(
    LocalCosmicColorScheme provides colorScheme
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = CosmicTypography,
      shapes = CosmicShapes,
      content = content
    )
  }
}

/**
 * Composition local for accessing the current color scheme.
 *
 * Use this in custom components to access theme colors:
 * ```
 * val colors = LocalCosmicColorScheme.current
 * ```
 */
val LocalCosmicColorScheme = staticCompositionLocalOf<ColorScheme> {
  CosmicLightColorScheme
}
