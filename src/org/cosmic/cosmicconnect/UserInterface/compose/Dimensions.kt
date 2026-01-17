/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * COSMIC Connect dimension system for consistent component sizing.
 *
 * Defines standard sizes for interactive elements, icons, and other UI components
 * following Material Design 3 guidelines and Android accessibility standards.
 *
 * Minimum Touch Target: 48dp x 48dp (Android accessibility requirement)
 */
object Dimensions {
  /**
   * Minimum touch target size (48dp)
   *
   * Android accessibility requirement. All interactive elements should be
   * at least this size to ensure comfortable touch interaction.
   */
  val minTouchTarget: Dp = 48.dp

  /**
   * Icon sizes following Material Design 3
   */
  object Icon {
    /** Small icon (16dp) - Dense UI, inline icons */
    val small: Dp = 16.dp

    /** Standard icon (24dp) - Most common use case */
    val standard: Dp = 24.dp

    /** Large icon (32dp) - Prominent actions, headers */
    val large: Dp = 32.dp

    /** Extra large icon (48dp) - Featured icons, placeholders */
    val extraLarge: Dp = 48.dp

    /** Device/app icon (56dp) - List items, cards */
    val device: Dp = 56.dp

    /** Hero icon (72dp) - Empty states, onboarding */
    val hero: Dp = 72.dp
  }

  /**
   * Button dimensions
   */
  object Button {
    /** Standard button height (48dp minimum) */
    val height: Dp = 48.dp

    /** Compact button height (40dp) */
    val compactHeight: Dp = 40.dp

    /** Large button height (56dp) */
    val largeHeight: Dp = 56.dp

    /** FAB size (56dp) */
    val fabSize: Dp = 56.dp

    /** Small FAB size (40dp) */
    val smallFabSize: Dp = 40.dp

    /** Large FAB size (96dp) */
    val largeFabSize: Dp = 96.dp

    /** Minimum button width */
    val minWidth: Dp = 64.dp
  }

  /**
   * List item dimensions
   */
  object ListItem {
    /** Standard list item height (56dp) */
    val standardHeight: Dp = 56.dp

    /** Compact list item height (48dp) */
    val compactHeight: Dp = 48.dp

    /** Large list item height (72dp) */
    val largeHeight: Dp = 72.dp

    /** Extra large list item (88dp) - Multi-line with icon */
    val extraLargeHeight: Dp = 88.dp

    /** Device list item height (80dp) */
    val deviceItemHeight: Dp = 80.dp

    /** Leading icon size */
    val leadingIconSize: Dp = Icon.device

    /** Trailing icon size */
    val trailingIconSize: Dp = Icon.standard
  }

  /**
   * Card dimensions
   */
  object Card {
    /** Minimum card height */
    val minHeight: Dp = 80.dp

    /** Standard card elevation */
    val elevation: Dp = 1.dp

    /** Elevated card elevation */
    val elevatedElevation: Dp = 2.dp

    /** Maximum card width for readability */
    val maxWidth: Dp = 600.dp
  }

  /**
   * Text field dimensions
   */
  object TextField {
    /** Standard text field height (56dp) */
    val height: Dp = 56.dp

    /** Dense text field height (48dp) */
    val denseHeight: Dp = 48.dp

    /** Minimum width for comfortable typing */
    val minWidth: Dp = 200.dp
  }

  /**
   * App bar dimensions
   */
  object AppBar {
    /** Standard top app bar height (64dp) */
    val height: Dp = 64.dp

    /** Compact top app bar height (48dp) */
    val compactHeight: Dp = 48.dp

    /** Large top app bar height (152dp) */
    val largeHeight: Dp = 152.dp

    /** Bottom app bar height (80dp) */
    val bottomHeight: Dp = 80.dp

    /** Navigation rail width (80dp) */
    val navigationRailWidth: Dp = 80.dp

    /** Expanded navigation rail width (256dp) */
    val expandedNavigationRailWidth: Dp = 256.dp
  }

  /**
   * Navigation drawer dimensions
   */
  object Drawer {
    /** Standard drawer width (360dp) */
    val width: Dp = 360.dp

    /** Modal drawer width (320dp) */
    val modalWidth: Dp = 320.dp

    /** Mini drawer width (56dp) - Collapsed */
    val miniWidth: Dp = 56.dp
  }

  /**
   * Dialog dimensions
   */
  object Dialog {
    /** Minimum dialog width */
    val minWidth: Dp = 280.dp

    /** Maximum dialog width */
    val maxWidth: Dp = 560.dp

    /** Full width dialog width percentage (0.9f of screen) */
    val fullWidthPercentage: Float = 0.9f
  }

  /**
   * Bottom sheet dimensions
   */
  object BottomSheet {
    /** Drag handle width */
    val dragHandleWidth: Dp = 32.dp

    /** Drag handle height */
    val dragHandleHeight: Dp = 4.dp

    /** Standard bottom sheet peek height */
    val peekHeight: Dp = 56.dp
  }

  /**
   * Divider dimensions
   */
  object Divider {
    /** Standard divider thickness */
    val thickness: Dp = 1.dp

    /** Thick divider thickness */
    val thickThickness: Dp = 2.dp
  }

  /**
   * Badge dimensions
   */
  object Badge {
    /** Small badge size (16dp) - Dot only */
    val small: Dp = 16.dp

    /** Standard badge size (20dp) - With number */
    val standard: Dp = 20.dp

    /** Large badge size (24dp) - Emphasized */
    val large: Dp = 24.dp
  }

  /**
   * Avatar dimensions
   */
  object Avatar {
    /** Small avatar (32dp) */
    val small: Dp = 32.dp

    /** Standard avatar (40dp) */
    val standard: Dp = 40.dp

    /** Large avatar (56dp) */
    val large: Dp = 56.dp

    /** Extra large avatar (72dp) */
    val extraLarge: Dp = 72.dp
  }

  /**
   * Progress indicator dimensions
   */
  object Progress {
    /** Circular progress indicator size (48dp) */
    val circularSize: Dp = 48.dp

    /** Small circular progress (24dp) */
    val smallCircularSize: Dp = 24.dp

    /** Linear progress height (4dp) */
    val linearHeight: Dp = 4.dp

    /** Thick linear progress height (8dp) */
    val thickLinearHeight: Dp = 8.dp
  }

  /**
   * Switch and checkbox dimensions
   */
  object Toggle {
    /** Switch width (52dp) */
    val switchWidth: Dp = 52.dp

    /** Switch height (32dp) */
    val switchHeight: Dp = 32.dp

    /** Checkbox size (20dp) */
    val checkboxSize: Dp = 20.dp

    /** Radio button size (20dp) */
    val radioSize: Dp = 20.dp
  }
}

/**
 * Elevation levels for Material3 components.
 *
 * Material3 uses tonal elevation (color shifts) instead of shadow elevation,
 * but shadow values are still used for certain components.
 */
object Elevation {
  /** No elevation (0dp) - Surface level */
  val level0: Dp = 0.dp

  /** Level 1 (1dp) - Elevated buttons, cards at rest */
  val level1: Dp = 1.dp

  /** Level 2 (3dp) - FAB at rest, cards on hover */
  val level2: Dp = 3.dp

  /** Level 3 (6dp) - FAB on hover, bottom nav, dialogs */
  val level3: Dp = 6.dp

  /** Level 4 (8dp) - Navigation drawer */
  val level4: Dp = 8.dp

  /** Level 5 (12dp) - Modal bottom sheets, menus */
  val level5: Dp = 12.dp
}

/**
 * Border dimensions
 */
object Border {
  /** Thin border (1dp) */
  val thin: Dp = 1.dp

  /** Standard border (2dp) */
  val standard: Dp = 2.dp

  /** Thick border (4dp) */
  val thick: Dp = 4.dp

  /** Focus indicator border (3dp) */
  val focus: Dp = 3.dp
}

/**
 * Usage examples:
 *
 * ```kotlin
 * // Button with standard height
 * Button(
 *   modifier = Modifier.height(Dimensions.Button.height)
 * ) { ... }
 *
 * // List item with device icon
 * ListItem(
 *   leadingContent = {
 *     Icon(
 *       modifier = Modifier.size(Dimensions.ListItem.leadingIconSize),
 *       ...
 *     )
 *   }
 * )
 *
 * // Card with elevation
 * Card(
 *   elevation = CardDefaults.cardElevation(
 *     defaultElevation = Elevation.level1
 *   )
 * ) { ... }
 *
 * // Ensure touch target
 * IconButton(
 *   modifier = Modifier.size(Dimensions.minTouchTarget)
 * ) { ... }
 *
 * // Circular progress
 * CircularProgressIndicator(
 *   modifier = Modifier.size(Dimensions.Progress.circularSize)
 * )
 * ```
 */
