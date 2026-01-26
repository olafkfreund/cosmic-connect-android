/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Enhanced Material3 TopAppBar for COSMIC Connect.
 *
 * Provides consistent app bar styling with navigation icon, title, and action buttons.
 *
 * @param title App bar title
 * @param navigationIcon Optional navigation icon (hamburger, back arrow, etc.)
 * @param onNavigationClick Callback when navigation icon is clicked
 * @param actions Optional trailing action buttons
 * @param scrollBehavior Optional scroll behavior for collapsing/expanding
 * @param windowInsets Window insets for edge-to-edge support
 * @param modifier Modifier for the app bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmicTopAppBar(
  title: String,
  navigationIcon: Int? = null,
  onNavigationClick: () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
  scrollBehavior: TopAppBarScrollBehavior? = null,
  windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    navigationIcon = {
      navigationIcon?.let { icon ->
        IconButton(
          onClick = onNavigationClick,
          modifier = Modifier.semantics {
            contentDescription = "Navigation"
          }
        ) {
          Icon(
            painter = painterResource(icon),
            contentDescription = null
          )
        }
      }
    },
    actions = actions,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    scrollBehavior = scrollBehavior,
    windowInsets = windowInsets,
    modifier = modifier
  )
}

/**
 * Navigation destination for bottom navigation and drawer.
 *
 * @param id Unique identifier for the destination
 * @param label Display label
 * @param icon Drawable resource ID for icon
 * @param selectedIcon Optional selected state icon (defaults to same as icon)
 * @param badgeCount Optional badge count (null for no badge)
 * @param contentDescription Accessibility description
 */
data class NavigationDestination(
  val id: String,
  val label: String,
  val icon: Int,
  val selectedIcon: Int? = null,
  val badgeCount: Int? = null,
  val contentDescription: String = label
)

/**
 * Material3 Bottom Navigation Bar for COSMIC Connect.
 *
 * Primary navigation for 3-5 top-level destinations.
 *
 * @param destinations List of navigation destinations (3-5 items recommended)
 * @param selectedDestination Currently selected destination ID
 * @param onDestinationSelected Callback when destination is selected
 * @param modifier Modifier for the navigation bar
 */
@Composable
fun CosmicBottomNavigationBar(
  destinations: List<NavigationDestination>,
  selectedDestination: String,
  onDestinationSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation = Elevation.level2,
    modifier = modifier
  ) {
    destinations.forEach { destination ->
      NavigationBarItem(
        selected = selectedDestination == destination.id,
        onClick = { onDestinationSelected(destination.id) },
        icon = {
          val iconRes = if (selectedDestination == destination.id) {
            destination.selectedIcon ?: destination.icon
          } else {
            destination.icon
          }

          if (destination.badgeCount != null && destination.badgeCount > 0) {
            BadgedBox(
              badge = {
                Badge {
                  Text(
                    text = if (destination.badgeCount > 99) "99+" else destination.badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              }
            ) {
              Icon(
                painter = painterResource(iconRes),
                contentDescription = null
              )
            }
          } else {
            Icon(
              painter = painterResource(iconRes),
              contentDescription = null
            )
          }
        },
        label = {
          Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        alwaysShowLabel = true,
        modifier = Modifier.semantics {
          contentDescription = buildString {
            append(destination.contentDescription)
            if (selectedDestination == destination.id) {
              append(", selected")
            }
            destination.badgeCount?.let { count ->
              if (count > 0) {
                append(", $count notifications")
              }
            }
          }
        }
      )
    }
  }
}

/**
 * Material3 Modal Navigation Drawer for COSMIC Connect.
 *
 * Side navigation drawer for accessing all app sections and settings.
 *
 * @param destinations List of navigation destinations
 * @param selectedDestination Currently selected destination ID
 * @param onDestinationSelected Callback when destination is selected
 * @param drawerState Drawer state for controlling open/close
 * @param header Optional header composable (user profile, app logo, etc.)
 * @param footer Optional footer composable (settings, about, etc.)
 * @param content Main content shown when drawer is closed
 */
@Composable
fun CosmicNavigationDrawer(
  destinations: List<NavigationDestination>,
  selectedDestination: String,
  onDestinationSelected: (String) -> Unit,
  drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
  header: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
  footer: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  val scope = rememberCoroutineScope()

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
      ) {
        header?.let { it() }

        destinations.forEach { destination ->
          NavigationDrawerItem(
            label = {
              Text(
                text = destination.label,
                style = MaterialTheme.typography.labelLarge
              )
            },
            icon = {
              val iconRes = if (selectedDestination == destination.id) {
                destination.selectedIcon ?: destination.icon
              } else {
                destination.icon
              }

              if (destination.badgeCount != null && destination.badgeCount > 0) {
                BadgedBox(
                  badge = {
                    Badge {
                      Text(
                        text = if (destination.badgeCount > 99) "99+" else destination.badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                      )
                    }
                  }
                ) {
                  Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null
                  )
                }
              } else {
                Icon(
                  painter = painterResource(iconRes),
                  contentDescription = null
                )
              }
            },
            selected = selectedDestination == destination.id,
            onClick = {
              onDestinationSelected(destination.id)
              scope.launch { drawerState.close() }
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedContainerColor = MaterialTheme.colorScheme.surface,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.semantics {
              contentDescription = buildString {
                append(destination.contentDescription)
                if (selectedDestination == destination.id) {
                  append(", selected")
                }
                destination.badgeCount?.let { count ->
                  if (count > 0) {
                    append(", $count notifications")
                  }
                }
              }
            }
          )
        }

        footer?.let { it() }
      }
    },
    content = content
  )
}

/**
 * Material3 Navigation Rail for COSMIC Connect (Tablet/Desktop).
 *
 * Vertical navigation optimized for larger screens and landscape orientation.
 *
 * @param destinations List of navigation destinations (3-7 items recommended)
 * @param selectedDestination Currently selected destination ID
 * @param onDestinationSelected Callback when destination is selected
 * @param header Optional header composable (FAB, logo, etc.)
 * @param modifier Modifier for the navigation rail
 */
@Composable
fun CosmicNavigationRail(
  destinations: List<NavigationDestination>,
  selectedDestination: String,
  onDestinationSelected: (String) -> Unit,
  header: @Composable (androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  NavigationRail(
    header = header,
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    modifier = modifier
  ) {
    destinations.forEach { destination ->
      NavigationRailItem(
        selected = selectedDestination == destination.id,
        onClick = { onDestinationSelected(destination.id) },
        icon = {
          val iconRes = if (selectedDestination == destination.id) {
            destination.selectedIcon ?: destination.icon
          } else {
            destination.icon
          }

          if (destination.badgeCount != null && destination.badgeCount > 0) {
            BadgedBox(
              badge = {
                Badge {
                  Text(
                    text = if (destination.badgeCount > 99) "99+" else destination.badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall
                  )
                }
              }
            ) {
              Icon(
                painter = painterResource(iconRes),
                contentDescription = null
              )
            }
          } else {
            Icon(
              painter = painterResource(iconRes),
              contentDescription = null
            )
          }
        },
        label = {
          Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        },
        alwaysShowLabel = false,
        modifier = Modifier.semantics {
          contentDescription = buildString {
            append(destination.contentDescription)
            if (selectedDestination == destination.id) {
              append(", selected")
            }
            destination.badgeCount?.let { count ->
              if (count > 0) {
                append(", $count notifications")
              }
            }
          }
        }
      )
    }
  }
}

/**
 * Material3 Bottom App Bar for COSMIC Connect.
 *
 * Alternative to BottomNavigationBar when fewer destinations or FAB integration needed.
 *
 * @param actions Action buttons to display in the app bar
 * @param floatingActionButton Optional FAB integrated with the bar
 * @param modifier Modifier for the app bar
 */
@Composable
fun CosmicBottomAppBar(
  actions: @Composable RowScope.() -> Unit = {},
  floatingActionButton: @Composable (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  BottomAppBar(
    actions = actions,
    floatingActionButton = floatingActionButton,
    containerColor = MaterialTheme.colorScheme.surfaceVariant,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation = Elevation.level2,
    modifier = modifier
  )
}

/**
 * Preview composables for development
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CosmicTopAppBarPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    CosmicTopAppBar(
      title = "COSMIC Connect",
      navigationIcon = CosmicIcons.Navigation.menu,
      onNavigationClick = {},
      actions = {
        IconButton(onClick = {}) {
          Icon(
            painter = painterResource(CosmicIcons.Action.search),
            contentDescription = "Search"
          )
        }
        IconButton(onClick = {}) {
          Icon(
            painter = painterResource(CosmicIcons.Action.more),
            contentDescription = "More options"
          )
        }
      }
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun CosmicBottomNavigationBarPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    CosmicBottomNavigationBar(
      destinations = listOf(
        NavigationDestination(
          id = "devices",
          label = "Devices",
          icon = CosmicIcons.Device.phone,
          selectedIcon = CosmicIcons.Device.phone,
          badgeCount = 3
        ),
        NavigationDestination(
          id = "plugins",
          label = "Plugins",
          icon = CosmicIcons.Plugin.share,
          selectedIcon = CosmicIcons.Plugin.share
        ),
        NavigationDestination(
          id = "settings",
          label = "Settings",
          icon = CosmicIcons.Settings.settings,
          selectedIcon = CosmicIcons.Settings.settings
        )
      ),
      selectedDestination = "devices",
      onDestinationSelected = {}
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CosmicNavigationDrawerPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    CosmicNavigationDrawer(
      destinations = listOf(
        NavigationDestination(
          id = "devices",
          label = "Devices",
          icon = CosmicIcons.Device.phone,
          badgeCount = 2
        ),
        NavigationDestination(
          id = "plugins",
          label = "Plugins",
          icon = CosmicIcons.Plugin.share
        ),
        NavigationDestination(
          id = "notifications",
          label = "Notifications",
          icon = CosmicIcons.Communication.notification,
          badgeCount = 5
        ),
        NavigationDestination(
          id = "settings",
          label = "Settings",
          icon = CosmicIcons.Settings.settings
        )
      ),
      selectedDestination = "devices",
      onDestinationSelected = {},
      drawerState = rememberDrawerState(DrawerValue.Open)
    ) {
      Text("Main content")
    }
  }
}

@Preview(showBackground = true, widthDp = 840, heightDp = 480)
@Composable
private fun CosmicNavigationRailPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    CosmicNavigationRail(
      destinations = listOf(
        NavigationDestination(
          id = "devices",
          label = "Devices",
          icon = CosmicIcons.Device.phone,
          badgeCount = 2
        ),
        NavigationDestination(
          id = "plugins",
          label = "Plugins",
          icon = CosmicIcons.Plugin.share
        ),
        NavigationDestination(
          id = "notifications",
          label = "Notifications",
          icon = CosmicIcons.Communication.notification,
          badgeCount = 5
        ),
        NavigationDestination(
          id = "settings",
          label = "Settings",
          icon = CosmicIcons.Settings.settings
        )
      ),
      selectedDestination = "devices",
      onDestinationSelected = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun CosmicBottomAppBarPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    CosmicBottomAppBar(
      actions = {
        IconButton(onClick = {}) {
          Icon(
            painter = painterResource(CosmicIcons.Action.search),
            contentDescription = "Search"
          )
        }
        IconButton(onClick = {}) {
          Icon(
            painter = painterResource(CosmicIcons.Communication.notification),
            contentDescription = "Notifications"
          )
        }
        IconButton(onClick = {}) {
          Icon(
            painter = painterResource(CosmicIcons.Action.more),
            contentDescription = "More options"
          )
        }
      }
    )
  }
}
