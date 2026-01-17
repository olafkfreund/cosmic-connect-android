/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * COSMIC Connect shape system following Material Design 3 principles.
 *
 * Uses rounded corners to create a friendly, modern interface while maintaining
 * consistency across all UI components.
 *
 * Shape Scale:
 * - Extra Small: Minimal rounding for small interactive elements (4dp)
 * - Small: Subtle rounding for chips, buttons (8dp)
 * - Medium: Comfortable rounding for cards, dialogs (12dp)
 * - Large: Prominent rounding for bottom sheets, large cards (16dp)
 * - Extra Large: Maximum rounding for special containers (28dp)
 */
val CosmicShapes = Shapes(
  /**
   * Extra small shape - Minimal corner rounding
   *
   * Used for:
   * - Small text fields
   * - Compact buttons
   * - Badges
   * - Small chips
   */
  extraSmall = RoundedCornerShape(4.dp),

  /**
   * Small shape - Subtle corner rounding
   *
   * Used for:
   * - Standard text fields
   * - Regular buttons
   * - Input chips
   * - Small cards
   * - Tooltips
   */
  small = RoundedCornerShape(8.dp),

  /**
   * Medium shape - Comfortable corner rounding (default)
   *
   * Used for:
   * - Cards
   * - Dialogs
   * - Menus
   * - List containers
   * - Navigation drawer items
   * - Elevated buttons
   */
  medium = RoundedCornerShape(12.dp),

  /**
   * Large shape - Prominent corner rounding
   *
   * Used for:
   * - Bottom sheets
   * - Large cards
   * - Floating action buttons (FAB)
   * - Navigation rail
   * - Modal dialogs
   */
  large = RoundedCornerShape(16.dp),

  /**
   * Extra large shape - Maximum corner rounding
   *
   * Used for:
   * - Bottom app bars
   * - Extended FABs
   * - Large bottom sheets
   * - Full-screen dialogs (top corners)
   * - Special promotional cards
   */
  extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Custom shape definitions for specific components
 */
object CustomShapes {
  /**
   * Shape for device list items
   * Slightly rounded for comfortable visual separation
   */
  val deviceCard = RoundedCornerShape(12.dp)

  /**
   * Shape for plugin cards within device details
   * Matches medium shape for consistency
   */
  val pluginCard = RoundedCornerShape(12.dp)

  /**
   * Shape for notification cards
   * Slightly more rounded for friendliness
   */
  val notificationCard = RoundedCornerShape(16.dp)

  /**
   * Shape for bottom sheets (pairing, settings)
   * Only top corners rounded
   */
  val bottomSheet = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
  )

  /**
   * Shape for status indicators (badges)
   * Fully circular for clear visual distinction
   */
  val statusBadge = RoundedCornerShape(50)

  /**
   * Shape for search bars
   * Fully rounded pill shape
   */
  val searchBar = RoundedCornerShape(50)
}

/**
 * Usage guidelines:
 *
 * - Use extraSmall for compact UI elements that need clear boundaries
 * - Use small for standard interactive elements (buttons, chips)
 * - Use medium (default) for most containers and cards
 * - Use large for prominent containers that need emphasis
 * - Use extraLarge sparingly for special full-width containers
 *
 * - Maintain consistent shape usage within component families
 * - Consider accessibility - very large corners can reduce touch target size
 * - Match shape scale with component size (larger components = larger corners)
 */
