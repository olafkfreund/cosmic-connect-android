/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * COSMIC Connect color palette following Material Design 3 principles.
 *
 * Colors are inspired by COSMIC Desktop's design language while maintaining
 * Material3 semantic naming and accessibility standards.
 */

// Primary colors - Blue palette (COSMIC Connect identity)
private val CosmicBlue80 = Color(0xFF7EC8E3)
private val CosmicBlue40 = Color(0xFF006B8F)
private val CosmicBlue30 = Color(0xFF004F6B)
private val CosmicBlue20 = Color(0xFF003547)
private val CosmicBlue10 = Color(0xFF001E2B)

// Secondary colors - Teal palette (complementary)
private val CosmicTeal80 = Color(0xFF7DD8C3)
private val CosmicTeal40 = Color(0xFF00897B)
private val CosmicTeal30 = Color(0xFF00695C)
private val CosmicTeal20 = Color(0xFF004D40)
private val CosmicTeal10 = Color(0xFF00251A)

// Tertiary colors - Purple palette (accent)
private val CosmicPurple80 = Color(0xFFCDB4DB)
private val CosmicPurple40 = Color(0xFF8E44AD)
private val CosmicPurple30 = Color(0xFF6C3483)
private val CosmicPurple20 = Color(0xFF512E5F)
private val CosmicPurple10 = Color(0xFF2C1A3D)

// Neutral colors
private val Neutral99 = Color(0xFFFBFCFE)
private val Neutral95 = Color(0xFFEFF1F3)
private val Neutral90 = Color(0xFFE1E3E5)
private val Neutral80 = Color(0xFFC4C7C9)
private val Neutral70 = Color(0xFFA8ABAE)
private val Neutral60 = Color(0xFF8D9093)
private val Neutral50 = Color(0xFF737679)
private val Neutral40 = Color(0xFF5A5D5F)
private val Neutral30 = Color(0xFF424547)
private val Neutral20 = Color(0xFF2C2F30)
private val Neutral10 = Color(0xFF191C1D)
private val Neutral0 = Color(0xFF000000)

// Error colors
private val Error80 = Color(0xFFFFB4AB)
private val Error40 = Color(0xFFBA1A1A)
private val Error30 = Color(0xFF93000A)
private val Error20 = Color(0xFF690005)
private val Error10 = Color(0xFF410002)

/**
 * Light color scheme for COSMIC Connect.
 *
 * Provides high contrast and readability for light theme.
 */
val CosmicLightColorScheme = lightColorScheme(
  // Primary colors
  primary = CosmicBlue40,
  onPrimary = Color.White,
  primaryContainer = CosmicBlue80,
  onPrimaryContainer = CosmicBlue10,

  // Secondary colors
  secondary = CosmicTeal40,
  onSecondary = Color.White,
  secondaryContainer = CosmicTeal80,
  onSecondaryContainer = CosmicTeal10,

  // Tertiary colors
  tertiary = CosmicPurple40,
  onTertiary = Color.White,
  tertiaryContainer = CosmicPurple80,
  onTertiaryContainer = CosmicPurple10,

  // Error colors
  error = Error40,
  onError = Color.White,
  errorContainer = Error80,
  onErrorContainer = Error10,

  // Background colors
  background = Neutral99,
  onBackground = Neutral10,

  // Surface colors
  surface = Neutral99,
  onSurface = Neutral10,
  surfaceVariant = Neutral90,
  onSurfaceVariant = Neutral30,

  // Outline colors
  outline = Neutral50,
  outlineVariant = Neutral80,

  // Inverse colors
  inverseSurface = Neutral20,
  inverseOnSurface = Neutral95,
  inversePrimary = CosmicBlue80,

  // Surface tint
  surfaceTint = CosmicBlue40,

  // Scrim
  scrim = Neutral0
)

/**
 * Dark color scheme for COSMIC Connect.
 *
 * Provides comfortable viewing in low-light conditions with proper contrast.
 */
val CosmicDarkColorScheme = darkColorScheme(
  // Primary colors
  primary = CosmicBlue80,
  onPrimary = CosmicBlue20,
  primaryContainer = CosmicBlue30,
  onPrimaryContainer = CosmicBlue80,

  // Secondary colors
  secondary = CosmicTeal80,
  onSecondary = CosmicTeal20,
  secondaryContainer = CosmicTeal30,
  onSecondaryContainer = CosmicTeal80,

  // Tertiary colors
  tertiary = CosmicPurple80,
  onTertiary = CosmicPurple20,
  tertiaryContainer = CosmicPurple30,
  onTertiaryContainer = CosmicPurple80,

  // Error colors
  error = Error80,
  onError = Error20,
  errorContainer = Error30,
  onErrorContainer = Error80,

  // Background colors
  background = Neutral10,
  onBackground = Neutral90,

  // Surface colors
  surface = Neutral10,
  onSurface = Neutral90,
  surfaceVariant = Neutral30,
  onSurfaceVariant = Neutral80,

  // Outline colors
  outline = Neutral60,
  outlineVariant = Neutral30,

  // Inverse colors
  inverseSurface = Neutral90,
  inverseOnSurface = Neutral20,
  inversePrimary = CosmicBlue40,

  // Surface tint
  surfaceTint = CosmicBlue80,

  // Scrim
  scrim = Neutral0
)
