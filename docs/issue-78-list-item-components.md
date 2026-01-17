# Issue #78: List Item Components

**Status**: ✅ COMPLETE
**Created**: 2026-01-17
**Phase**: Phase 4.2 - Foundation Components

## Overview

Implemented four reusable list item components for COSMIC Connect Android, optimized for LazyColumn usage with dividers, proper spacing, and full accessibility support.

## Implementation Summary

### Created Files

1. **ListItems.kt** - Four list item components (476 lines)

### Components Implemented

1. **DeviceListItem** - Display devices in vertical lists
2. **PluginListItem** - Display plugins with toggle switch
3. **SectionHeader** - Section dividers with title
4. **SimpleListItem** - Generic list item for various use cases

## Component Details

### 1. DeviceListItem

List item component for displaying devices in LazyColumn layouts.

#### Features

✅ Device icon based on type
✅ Device name and optional subtitle
✅ Connection indicator icon
✅ Standard 56dp height
✅ Bottom divider (optional)
✅ Clickable with proper feedback
✅ Full accessibility support

#### Parameters

```kotlin
@Composable
fun DeviceListItem(
  deviceName: String,              // Device name
  deviceType: String,               // Device type (phone, laptop, etc.)
  subtitle: String? = null,         // Optional subtitle (status, last seen)
  isConnected: Boolean,             // Connection state
  showDivider: Boolean = true,      // Show bottom divider
  onClick: () -> Unit,              // Click callback
  modifier: Modifier = Modifier
)
```

#### Design Specifications

- **Height**: 56dp (Dimensions.ListItem.standardHeight)
- **No elevation**: Flat design for list context
- **Divider**: 1dp thickness, offset by icon width
- **Icon sizes**: 56dp leading (device), 24dp trailing (status)
- **Padding**: 16dp horizontal
- **Spacing**: 16dp icon to text, 4dp title to subtitle

#### Usage Example

```kotlin
LazyColumn {
  items(devices) { device ->
    DeviceListItem(
      deviceName = device.name,
      deviceType = device.type,
      subtitle = if (device.isConnected) "Connected" else "Last seen ${device.lastSeen}",
      isConnected = device.isConnected,
      onClick = { navigateToDevice(device) }
    )
  }
}
```

#### Visual States

**Connected:**
- Primary color for device icon
- Primary color for subtitle text
- Connected indicator icon

**Disconnected:**
- OnSurfaceVariant for device icon
- OnSurfaceVariant for subtitle text
- Disconnected indicator icon

#### Accessibility

- Role: Button
- Content description: "$deviceName, $deviceType, $subtitle, connected/disconnected"
- Touch target: 56dp height meets 48dp minimum

### 2. PluginListItem

List item component for displaying plugins with enable/disable toggle.

#### Features

✅ Plugin icon
✅ Plugin name and description
✅ Toggle switch
✅ Available/unavailable state
✅ Optional settings click
✅ 72dp height for two-line content
✅ Bottom divider (optional)
✅ Full accessibility support

#### Parameters

```kotlin
@Composable
fun PluginListItem(
  pluginName: String,               // Plugin name
  pluginDescription: String,        // Plugin description
  pluginIcon: Int,                  // Drawable resource ID
  isEnabled: Boolean,               // Enabled state
  isAvailable: Boolean = true,      // Availability
  showDivider: Boolean = true,      // Show bottom divider
  onToggle: (Boolean) -> Unit,      // Toggle callback
  onClick: (() -> Unit)? = null,    // Optional settings callback
  modifier: Modifier = Modifier
)
```

#### Design Specifications

- **Height**: 72dp (Dimensions.ListItem.largeHeight)
- **No elevation**: Flat design for list context
- **Divider**: 1dp thickness, offset by icon width
- **Icon size**: 32dp plugin icon
- **Switch size**: 52dp × 32dp
- **Padding**: 16dp horizontal
- **Spacing**: 16dp icon to text, 4dp title to description

#### Usage Example

```kotlin
LazyColumn {
  items(plugins) { plugin ->
    PluginListItem(
      pluginName = plugin.name,
      pluginDescription = plugin.description,
      pluginIcon = plugin.iconResId,
      isEnabled = plugin.isEnabled,
      isAvailable = plugin.isAvailable,
      onToggle = { enabled -> viewModel.togglePlugin(plugin.id, enabled) },
      onClick = if (plugin.hasSettings) {
        { navigateToPluginSettings(plugin.id) }
      } else {
        null
      }
    )
  }
}
```

#### Visual States

**Enabled & Available:**
- Primary color for icon
- Normal text colors
- Switch enabled and checked
- Clickable if settings available

**Disabled & Available:**
- OnSurfaceVariant for icon
- Normal text colors
- Switch enabled and unchecked
- Clickable if settings available

**Unavailable:**
- 50% alpha on icon and text
- "(Not available)" appended to description
- Switch disabled
- Not clickable

#### Accessibility

- Content description: "$pluginName, $pluginDescription, enabled/disabled/not available"
- Switch description: "Enable/Disable $pluginName"
- Optional button role for settings click
- Touch targets meet minimum requirements

### 3. SectionHeader

Section header component for dividing list sections with titles.

#### Features

✅ Section title text
✅ Optional subtitle/description
✅ Configurable top padding
✅ Primary color for visual hierarchy
✅ Proper spacing above and below

#### Parameters

```kotlin
@Composable
fun SectionHeader(
  title: String,                    // Section title
  subtitle: String? = null,         // Optional subtitle/description
  showTopPadding: Boolean = true,   // Add top padding (false for first section)
  modifier: Modifier = Modifier
)
```

#### Design Specifications

- **Top padding**: 24dp (when showTopPadding = true)
- **Bottom padding**: 16dp
- **Horizontal padding**: 16dp (screen padding)
- **Title style**: titleMedium, primary color
- **Subtitle style**: bodySmall, onSurfaceVariant
- **Spacing**: 4dp title to subtitle

#### Usage Example

```kotlin
LazyColumn {
  item {
    SectionHeader(
      title = "Connected Devices",
      subtitle = "2 devices connected",
      showTopPadding = false  // First section
    )
  }

  items(connectedDevices) { device ->
    DeviceListItem(...)
  }

  item {
    SectionHeader(
      title = "Available Devices",
      subtitle = "Tap to pair",
      showTopPadding = true  // Subsequent section
    )
  }

  items(availableDevices) { device ->
    DeviceListItem(...)
  }
}
```

#### Visual Style

- Primary color for title (visual emphasis)
- OnSurfaceVariant for subtitle
- No background or border
- Clear visual separation from list items

### 4. SimpleListItem

Generic list item component for various use cases (settings, navigation, etc.)

#### Features

✅ Optional leading icon
✅ Primary and secondary text
✅ Optional trailing icon
✅ Optional custom trailing content
✅ Flexible height (56dp or 72dp)
✅ Bottom divider (optional)
✅ Optional click handler
✅ Full accessibility support

#### Parameters

```kotlin
@Composable
fun SimpleListItem(
  text: String,                              // Primary text
  icon: Int? = null,                         // Optional leading icon
  secondaryText: String? = null,             // Optional secondary text
  trailingIcon: Int? = null,                 // Optional trailing icon
  trailingContent: (@Composable () -> Unit)? = null,  // Optional custom trailing content
  showDivider: Boolean = true,               // Show bottom divider
  onClick: (() -> Unit)? = null,             // Optional click callback
  modifier: Modifier = Modifier
)
```

#### Design Specifications

- **Height**: 56dp (single-line) or 72dp (two-line)
- **No elevation**: Flat design
- **Divider**: 1dp thickness, offset by icon width if present
- **Icon sizes**: 24dp standard
- **Padding**: 16dp horizontal
- **Spacing**: 16dp icon to text, 4dp text lines

#### Usage Examples

**Settings Item:**
```kotlin
SimpleListItem(
  text = "Network Settings",
  icon = CosmicIcons.Pairing.wifi,
  trailingIcon = CosmicIcons.Navigation.forward,
  onClick = { navigateToNetworkSettings() }
)
```

**Status Display:**
```kotlin
SimpleListItem(
  text = "Connection Status",
  icon = CosmicIcons.Status.info,
  secondaryText = "Connected to 2 devices",
  trailingIcon = CosmicIcons.Navigation.forward,
  onClick = { showConnectionDetails() }
)
```

**Custom Trailing Content:**
```kotlin
SimpleListItem(
  text = "Auto-sync",
  icon = CosmicIcons.Action.refresh,
  secondaryText = "Sync data automatically",
  trailingContent = {
    Switch(
      checked = autoSyncEnabled,
      onCheckedChange = { toggleAutoSync() }
    )
  }
)
```

#### Accessibility

- Optional button role when clickable
- Content description: "$text, $secondaryText"
- Icons marked as decorative
- Proper touch targets

## Complete Usage Examples

### Example 1: Device List with Sections

```kotlin
@Composable
fun DeviceListScreen(
  connectedDevices: List<Device>,
  availableDevices: List<Device>,
  onDeviceClick: (Device) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize()
  ) {
    // Connected devices section
    if (connectedDevices.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Connected Devices",
          subtitle = "${connectedDevices.size} device(s) connected",
          showTopPadding = false
        )
      }

      items(connectedDevices) { device ->
        DeviceListItem(
          deviceName = device.name,
          deviceType = device.type,
          subtitle = "Connected",
          isConnected = true,
          showDivider = device != connectedDevices.last(),
          onClick = { onDeviceClick(device) }
        )
      }
    }

    // Available devices section
    if (availableDevices.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Available Devices",
          subtitle = "Tap to pair",
          showTopPadding = true
        )
      }

      items(availableDevices) { device ->
        DeviceListItem(
          deviceName = device.name,
          deviceType = device.type,
          subtitle = "Last seen ${device.lastSeen}",
          isConnected = false,
          showDivider = device != availableDevices.last(),
          onClick = { onDeviceClick(device) }
        )
      }
    }
  }
}
```

### Example 2: Plugin Management Screen

```kotlin
@Composable
fun PluginManagementScreen(
  plugins: List<Plugin>,
  onPluginToggle: (String, Boolean) -> Unit,
  onPluginSettings: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      SectionHeader(
        title = "Plugins",
        subtitle = "Enable plugins to extend functionality",
        showTopPadding = false
      )
    }

    items(plugins) { plugin ->
      PluginListItem(
        pluginName = plugin.name,
        pluginDescription = plugin.description,
        pluginIcon = plugin.iconResId,
        isEnabled = plugin.isEnabled,
        isAvailable = plugin.isAvailable,
        showDivider = plugin != plugins.last(),
        onToggle = { enabled -> onPluginToggle(plugin.id, enabled) },
        onClick = if (plugin.hasSettings) {
          { onPluginSettings(plugin.id) }
        } else {
          null
        }
      )
    }
  }
}
```

### Example 3: Settings Screen

```kotlin
@Composable
fun SettingsScreen(
  onNavigateToNetwork: () -> Unit,
  onNavigateToNotifications: () -> Unit,
  onNavigateToAbout: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      SectionHeader(
        title = "General",
        showTopPadding = false
      )
    }

    item {
      SimpleListItem(
        text = "Network Settings",
        icon = CosmicIcons.Pairing.wifi,
        secondaryText = "Configure network preferences",
        trailingIcon = CosmicIcons.Navigation.forward,
        onClick = onNavigateToNetwork
      )
    }

    item {
      SimpleListItem(
        text = "Notifications",
        icon = CosmicIcons.Status.info,
        secondaryText = "Manage notification preferences",
        trailingIcon = CosmicIcons.Navigation.forward,
        showDivider = false,
        onClick = onNavigateToNotifications
      )
    }

    item {
      SectionHeader(
        title = "About",
        showTopPadding = true
      )
    }

    item {
      SimpleListItem(
        text = "About COSMIC Connect",
        secondaryText = "Version 2.0.0",
        trailingIcon = CosmicIcons.Navigation.forward,
        showDivider = false,
        onClick = onNavigateToAbout
      )
    }
  }
}
```

### Example 4: Mixed List Types

```kotlin
@Composable
fun DeviceDetailsScreen(
  device: Device,
  plugins: List<Plugin>,
  onPluginToggle: (String, Boolean) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize()
  ) {
    // Device info section
    item {
      SectionHeader(
        title = "Device Information",
        showTopPadding = false
      )
    }

    item {
      SimpleListItem(
        text = "Device Name",
        secondaryText = device.name,
        icon = CosmicIcons.Action.edit,
        trailingIcon = CosmicIcons.Navigation.forward,
        onClick = { /* Edit name */ }
      )
    }

    item {
      SimpleListItem(
        text = "Device Type",
        secondaryText = device.type,
        showDivider = false
      )
    }

    // Plugins section
    item {
      SectionHeader(
        title = "Plugins",
        subtitle = "Manage device plugins",
        showTopPadding = true
      )
    }

    items(plugins) { plugin ->
      PluginListItem(
        pluginName = plugin.name,
        pluginDescription = plugin.description,
        pluginIcon = plugin.iconResId,
        isEnabled = plugin.isEnabled,
        showDivider = plugin != plugins.last(),
        onToggle = { enabled -> onPluginToggle(plugin.id, enabled) }
      )
    }
  }
}
```

## Design System Integration

All list item components utilize the design system:

### Colors
- **primary** - Connected states, section titles, enabled plugins
- **onSurface** - Primary text
- **onSurfaceVariant** - Secondary text, disabled states, dividers
- **surface** - List item background
- **outlineVariant** - Divider lines

### Typography
- **bodyLarge** - Primary list item text
- **bodyMedium** - Secondary list item text
- **titleMedium** - Section header titles
- **bodySmall** - Section header subtitles

### Spacing
- **SpecialSpacing.ListItem.contentPadding** - 16dp horizontal padding
- **SpecialSpacing.ListItem.iconToText** - 16dp icon to text spacing
- **SpecialSpacing.Screen.sectionSpacing** - 24dp between sections
- **Spacing.extraSmall** - 4dp tight vertical spacing
- **Spacing.medium** - 16dp standard spacing

### Dimensions
- **Dimensions.ListItem.standardHeight** - 56dp single-line items
- **Dimensions.ListItem.largeHeight** - 72dp two-line items
- **Dimensions.ListItem.leadingIconSize** - 56dp device icons
- **Dimensions.ListItem.trailingIconSize** - 24dp status icons
- **Dimensions.Icon.large** - 32dp plugin icons
- **Dimensions.Icon.standard** - 24dp generic icons
- **Dimensions.Divider.thickness** - 1dp divider lines
- **Dimensions.Toggle.switchWidth/Height** - 52dp × 32dp

### Icons
- All icons from CosmicIcons system
- Proper sizing per context
- Appropriate tint colors

## Differences from Card Components

List items differ from cards in several ways:

| Aspect | List Items | Cards |
|--------|-----------|-------|
| **Elevation** | None (flat) | Level 1-2 (1-3dp) |
| **Separation** | Dividers | Spacing between cards |
| **Shape** | No rounded corners | 12dp rounded corners |
| **Usage** | LazyColumn lists | Grid or scattered layouts |
| **Density** | Higher (56-72dp) | Lower (80dp+) |
| **Context** | Scrolling lists | Featured content |

## Benefits

### For Developers
✅ **Optimized for Lists** - Dividers instead of cards
✅ **Flexible** - Multiple list item types
✅ **Consistent** - Unified design patterns
✅ **Reusable** - Works across all list contexts
✅ **Preview Composables** - Easy development

### For Users
✅ **Familiar Patterns** - Standard list behavior
✅ **Visual Hierarchy** - Clear sections
✅ **Accessible** - Screen reader support
✅ **Efficient Scrolling** - Optimized performance
✅ **Clear Actions** - Obvious interactive elements

### For Design
✅ **Material3 Compliant** - Standard list patterns
✅ **Design System** - All tokens used correctly
✅ **Flexible** - Easy to extend
✅ **Documented** - Clear specifications

## Files Created

- `src/org/cosmic/cosmicconnect/UserInterface/compose/ListItems.kt` (476 lines)
- `docs/issue-78-list-item-components.md` (this file)

## Testing Checklist

Manual verification required:

- [ ] DeviceListItem displays correctly
- [ ] PluginListItem toggle works properly
- [ ] SectionHeader provides clear separation
- [ ] SimpleListItem handles all variants
- [ ] Dividers align properly
- [ ] Colors adapt to light/dark theme
- [ ] Preview composables render correctly
- [ ] Test with TalkBack
- [ ] Touch targets adequate
- [ ] Test in LazyColumn with many items
- [ ] Test on different screen sizes

## Next Steps

**Issue #79**: Dialog Components
- AlertDialog wrapper
- ConfirmationDialog composable
- InputDialog composable
- PermissionDialog composable
- Custom dialog container

**Then continue with Foundation Components** (Issues #80-82):
- Navigation components
- Status indicators
- Input components

## Success Criteria

✅ DeviceListItem component implemented
✅ PluginListItem component implemented
✅ SectionHeader component implemented
✅ SimpleListItem component implemented
✅ Divider support working
✅ Multi-line content supported
✅ Leading/trailing icons supported
✅ Full accessibility support
✅ Design system integration complete
✅ Preview composables created
✅ Comprehensive documentation

---

**Created**: 2026-01-17
**Completed**: 2026-01-17
**Status**: ✅ Complete
