/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * COSMIC Connect spacing system following Material Design 3 principles.
 *
 * Uses an 8dp grid system for consistent spacing across the application.
 * All spacing values are multiples of 4dp for flexibility while maintaining
 * alignment with the 8dp grid.
 *
 * Spacing Scale:
 * - none (0dp): No spacing
 * - extraSmall (4dp): Minimal spacing within components
 * - small (8dp): Compact spacing, dense layouts
 * - medium (16dp): Standard spacing (default)
 * - large (24dp): Generous spacing
 * - extraLarge (32dp): Maximum spacing
 * - xxl (48dp): Special cases, major sections
 * - xxxl (64dp): Screen-level spacing
 */
object Spacing {
  /**
   * No spacing (0dp)
   *
   * Use sparingly, typically only when:
   * - Removing default padding/margin
   * - Tightly packed components by design
   * - Overriding inherited spacing
   */
  val none: Dp = 0.dp

  /**
   * Extra small spacing (4dp)
   *
   * Use for:
   * - Spacing between related icon and text
   * - Compact lists or grids
   * - Dense UI elements
   * - Chip internal padding
   * - Badge positioning
   */
  val extraSmall: Dp = 4.dp

  /**
   * Small spacing (8dp)
   *
   * Use for:
   * - Spacing within cards
   * - Between form fields in compact layouts
   * - List item internal padding
   * - Button padding (vertical)
   * - Spacing between related elements
   */
  val small: Dp = 8.dp

  /**
   * Medium spacing (16dp) - Default
   *
   * Use for:
   * - Standard card padding
   * - Screen edge padding
   * - Between unrelated UI elements
   * - Section internal padding
   * - Dialog content padding
   * - List item padding
   */
  val medium: Dp = 16.dp

  /**
   * Large spacing (24dp)
   *
   * Use for:
   * - Between major sections
   * - Bottom sheet top padding
   * - Large card padding
   * - Screen header spacing
   * - Prominent separation
   */
  val large: Dp = 24.dp

  /**
   * Extra large spacing (32dp)
   *
   * Use for:
   * - Between major screen sections
   * - Empty state spacing
   * - Large dialog padding
   * - Onboarding screens
   * - Full-width card padding
   */
  val extraLarge: Dp = 32.dp

  /**
   * XXL spacing (48dp)
   *
   * Use for:
   * - Major section dividers
   * - Top/bottom screen padding for emphasis
   * - Hero section spacing
   * - Splash screen element spacing
   */
  val xxl: Dp = 48.dp

  /**
   * XXXL spacing (64dp)
   *
   * Use for:
   * - Screen-level vertical spacing
   * - Major promotional sections
   * - Empty state illustrations
   * - Special full-screen layouts
   */
  val xxxl: Dp = 64.dp
}

/**
 * Specialized spacing values for common UI patterns.
 */
object SpecialSpacing {
  /**
   * List item spacing
   */
  object ListItem {
    /** Padding around list item content */
    val contentPadding: Dp = Spacing.medium

    /** Space between list items */
    val itemSpacing: Dp = Spacing.small

    /** Space between icon and text */
    val iconToText: Dp = Spacing.medium

    /** Vertical padding for dense lists */
    val densePadding: Dp = Spacing.small
  }

  /**
   * Card spacing
   */
  object Card {
    /** Standard card internal padding */
    val padding: Dp = Spacing.medium

    /** Compact card internal padding */
    val compactPadding: Dp = Spacing.small

    /** Large card internal padding */
    val largePadding: Dp = Spacing.large

    /** Space between cards in a list */
    val cardSpacing: Dp = Spacing.small

    /** Space between card sections */
    val sectionSpacing: Dp = Spacing.medium
  }

  /**
   * Button spacing
   */
  object Button {
    /** Horizontal padding inside button */
    val horizontalPadding: Dp = Spacing.medium

    /** Vertical padding inside button */
    val verticalPadding: Dp = Spacing.small

    /** Space between buttons in a row */
    val buttonSpacing: Dp = Spacing.small

    /** Space between icon and text in button */
    val iconToText: Dp = Spacing.small
  }

  /**
   * Dialog spacing
   */
  object Dialog {
    /** Dialog content padding */
    val contentPadding: Dp = Spacing.large

    /** Dialog title bottom spacing */
    val titleSpacing: Dp = Spacing.medium

    /** Space between dialog sections */
    val sectionSpacing: Dp = Spacing.medium

    /** Button area top padding */
    val buttonAreaPadding: Dp = Spacing.large
  }

  /**
   * Screen spacing
   */
  object Screen {
    /** Standard screen horizontal padding */
    val horizontalPadding: Dp = Spacing.medium

    /** Screen top padding (below app bar) */
    val topPadding: Dp = Spacing.medium

    /** Screen bottom padding (above nav bar) */
    val bottomPadding: Dp = Spacing.medium

    /** Space between screen sections */
    val sectionSpacing: Dp = Spacing.large
  }

  /**
   * Form spacing
   */
  object Form {
    /** Space between form fields */
    val fieldSpacing: Dp = Spacing.medium

    /** Space between form sections */
    val sectionSpacing: Dp = Spacing.large

    /** Space between label and field */
    val labelToField: Dp = Spacing.small

    /** Space between field and helper text */
    val fieldToHelper: Dp = Spacing.extraSmall
  }

  /**
   * Icon spacing
   */
  object Icon {
    /** Space between icon and adjacent text */
    val toText: Dp = Spacing.small

    /** Space between icons in a row */
    val iconSpacing: Dp = Spacing.small
  }
}

/**
 * Usage examples:
 *
 * ```kotlin
 * // Standard card padding
 * Card(
 *   modifier = Modifier.padding(Spacing.medium)
 * ) { ... }
 *
 * // List with spacing
 * LazyColumn(
 *   verticalArrangement = Arrangement.spacedBy(Spacing.small),
 *   contentPadding = PaddingValues(Spacing.medium)
 * ) { ... }
 *
 * // Button with icon
 * Button(
 *   contentPadding = PaddingValues(
 *     horizontal = SpecialSpacing.Button.horizontalPadding,
 *     vertical = SpecialSpacing.Button.verticalPadding
 *   )
 * ) {
 *   Icon(...)
 *   Spacer(modifier = Modifier.width(SpecialSpacing.Button.iconToText))
 *   Text(...)
 * }
 *
 * // Screen content
 * Column(
 *   modifier = Modifier
 *     .fillMaxSize()
 *     .padding(horizontal = SpecialSpacing.Screen.horizontalPadding)
 * ) { ... }
 * ```
 */
