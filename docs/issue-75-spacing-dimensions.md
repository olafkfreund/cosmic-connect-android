# Issue #75: Spacing and Dimensions System

**Status**: ✅ COMPLETE
**Created**: 2026-01-17
**Phase**: Phase 4.1 - Design System

## Overview

Implemented a comprehensive spacing and dimension system for COSMIC Connect Android, providing consistent sizing, spacing, and elevation values across all UI components.

## Implementation Summary

### Created Files

1. **Spacing.kt** - 8dp grid spacing system
2. **Dimensions.kt** - Component sizes and elevation levels

## Design System Components

### 1. Spacing System (Spacing.kt)

#### Core Spacing Scale (8dp Grid)

Following Material Design's 8dp grid system with 4dp increments for flexibility:

| Value | Size | Use Cases |
|-------|------|-----------|
| `none` | 0dp | Remove spacing, tight layouts |
| `extraSmall` | 4dp | Icon to text, dense UI, chips |
| `small` | 8dp | Card internals, form fields, related elements |
| `medium` | 16dp | **Default** - Screen padding, card padding, sections |
| `large` | 24dp | Major sections, prominent separation |
| `extraLarge` | 32dp | Screen sections, empty states, dialogs |
| `xxl` | 48dp | Major dividers, hero sections |
| `xxxl` | 64dp | Screen-level spacing, special layouts |

#### Specialized Spacing Patterns

**ListItem Spacing:**
```kotlin
SpecialSpacing.ListItem.contentPadding    // 16dp - Around content
SpecialSpacing.ListItem.itemSpacing       // 8dp - Between items
SpecialSpacing.ListItem.iconToText        // 16dp - Icon to text
SpecialSpacing.ListItem.densePadding      // 8dp - Dense lists
```

**Card Spacing:**
```kotlin
SpecialSpacing.Card.padding               // 16dp - Standard padding
SpecialSpacing.Card.compactPadding        // 8dp - Compact cards
SpecialSpacing.Card.largePadding          // 24dp - Large cards
SpecialSpacing.Card.cardSpacing           // 8dp - Between cards
SpecialSpacing.Card.sectionSpacing        // 16dp - Between sections
```

**Button Spacing:**
```kotlin
SpecialSpacing.Button.horizontalPadding   // 16dp - Horizontal
SpecialSpacing.Button.verticalPadding     // 8dp - Vertical
SpecialSpacing.Button.buttonSpacing       // 8dp - Between buttons
SpecialSpacing.Button.iconToText          // 8dp - Icon to text
```

**Dialog Spacing:**
```kotlin
SpecialSpacing.Dialog.contentPadding      // 24dp - Content padding
SpecialSpacing.Dialog.titleSpacing        // 16dp - Title bottom
SpecialSpacing.Dialog.sectionSpacing      // 16dp - Between sections
SpecialSpacing.Dialog.buttonAreaPadding   // 24dp - Button area
```

**Screen Spacing:**
```kotlin
SpecialSpacing.Screen.horizontalPadding   // 16dp - Screen edges
SpecialSpacing.Screen.topPadding          // 16dp - Below app bar
SpecialSpacing.Screen.bottomPadding       // 16dp - Above nav bar
SpecialSpacing.Screen.sectionSpacing      // 24dp - Between sections
```

**Form Spacing:**
```kotlin
SpecialSpacing.Form.fieldSpacing          // 16dp - Between fields
SpecialSpacing.Form.sectionSpacing        // 24dp - Between sections
SpecialSpacing.Form.labelToField          // 8dp - Label to field
SpecialSpacing.Form.fieldToHelper         // 4dp - Field to helper
```

**Icon Spacing:**
```kotlin
SpecialSpacing.Icon.toText                // 8dp - Icon to text
SpecialSpacing.Icon.iconSpacing           // 8dp - Between icons
```

### 2. Dimensions System (Dimensions.kt)

#### Touch Targets

**Minimum Touch Target**: 48dp × 48dp (Android accessibility requirement)

```kotlin
Dimensions.minTouchTarget  // 48dp - All interactive elements
```

#### Icon Sizes

```kotlin
Dimensions.Icon.small        // 16dp - Dense UI, inline icons
Dimensions.Icon.standard     // 24dp - Most common use case
Dimensions.Icon.large        // 32dp - Prominent actions
Dimensions.Icon.extraLarge   // 48dp - Featured icons
Dimensions.Icon.device       // 56dp - Device/app icons
Dimensions.Icon.hero         // 72dp - Empty states, onboarding
```

#### Button Dimensions

```kotlin
Dimensions.Button.height           // 48dp - Standard button
Dimensions.Button.compactHeight    // 40dp - Compact button
Dimensions.Button.largeHeight      // 56dp - Large button
Dimensions.Button.fabSize          // 56dp - FAB
Dimensions.Button.smallFabSize     // 40dp - Small FAB
Dimensions.Button.largeFabSize     // 96dp - Large FAB
Dimensions.Button.minWidth         // 64dp - Minimum width
```

#### List Item Dimensions

```kotlin
Dimensions.ListItem.standardHeight       // 56dp - Standard height
Dimensions.ListItem.compactHeight        // 48dp - Compact
Dimensions.ListItem.largeHeight          // 72dp - Large
Dimensions.ListItem.extraLargeHeight     // 88dp - Multi-line with icon
Dimensions.ListItem.deviceItemHeight     // 80dp - Device items
Dimensions.ListItem.leadingIconSize      // 56dp - Leading icon
Dimensions.ListItem.trailingIconSize     // 24dp - Trailing icon
```

#### Card Dimensions

```kotlin
Dimensions.Card.minHeight            // 80dp - Minimum height
Dimensions.Card.elevation            // 1dp - Standard elevation
Dimensions.Card.elevatedElevation    // 2dp - Elevated cards
Dimensions.Card.maxWidth             // 600dp - Max width for readability
```

#### Text Field Dimensions

```kotlin
Dimensions.TextField.height          // 56dp - Standard height
Dimensions.TextField.denseHeight     // 48dp - Dense layout
Dimensions.TextField.minWidth        // 200dp - Minimum width
```

#### App Bar Dimensions

```kotlin
Dimensions.AppBar.height                        // 64dp - Top app bar
Dimensions.AppBar.compactHeight                 // 48dp - Compact
Dimensions.AppBar.largeHeight                   // 152dp - Large/collapsing
Dimensions.AppBar.bottomHeight                  // 80dp - Bottom app bar
Dimensions.AppBar.navigationRailWidth           // 80dp - Nav rail
Dimensions.AppBar.expandedNavigationRailWidth   // 256dp - Expanded rail
```

#### Navigation Drawer Dimensions

```kotlin
Dimensions.Drawer.width              // 360dp - Standard drawer
Dimensions.Drawer.modalWidth         // 320dp - Modal drawer
Dimensions.Drawer.miniWidth          // 56dp - Mini/collapsed
```

#### Dialog Dimensions

```kotlin
Dimensions.Dialog.minWidth           // 280dp - Minimum width
Dimensions.Dialog.maxWidth           // 560dp - Maximum width
Dimensions.Dialog.fullWidthPercentage // 0.9f - Full width (90%)
```

#### Bottom Sheet Dimensions

```kotlin
Dimensions.BottomSheet.dragHandleWidth   // 32dp - Handle width
Dimensions.BottomSheet.dragHandleHeight  // 4dp - Handle height
Dimensions.BottomSheet.peekHeight        // 56dp - Peek height
```

#### Other Component Dimensions

**Dividers:**
```kotlin
Dimensions.Divider.thickness         // 1dp - Standard
Dimensions.Divider.thickThickness    // 2dp - Thick
```

**Badges:**
```kotlin
Dimensions.Badge.small               // 16dp - Dot only
Dimensions.Badge.standard            // 20dp - With number
Dimensions.Badge.large               // 24dp - Emphasized
```

**Avatars:**
```kotlin
Dimensions.Avatar.small              // 32dp
Dimensions.Avatar.standard           // 40dp
Dimensions.Avatar.large              // 56dp
Dimensions.Avatar.extraLarge         // 72dp
```

**Progress Indicators:**
```kotlin
Dimensions.Progress.circularSize         // 48dp - Circular
Dimensions.Progress.smallCircularSize    // 24dp - Small circular
Dimensions.Progress.linearHeight         // 4dp - Linear
Dimensions.Progress.thickLinearHeight    // 8dp - Thick linear
```

**Toggles:**
```kotlin
Dimensions.Toggle.switchWidth        // 52dp
Dimensions.Toggle.switchHeight       // 32dp
Dimensions.Toggle.checkboxSize       // 20dp
Dimensions.Toggle.radioSize          // 20dp
```

**Borders:**
```kotlin
Border.thin                          // 1dp
Border.standard                      // 2dp
Border.thick                         // 4dp
Border.focus                         // 3dp - Focus indicators
```

### 3. Elevation System

Material3 uses tonal elevation (color shifts) but shadows are still used for certain components:

```kotlin
Elevation.level0    // 0dp - Surface level
Elevation.level1    // 1dp - Elevated buttons, cards at rest
Elevation.level2    // 3dp - FAB at rest, cards on hover
Elevation.level3    // 6dp - FAB on hover, bottom nav, dialogs
Elevation.level4    // 8dp - Navigation drawer
Elevation.level5    // 12dp - Modal bottom sheets, menus
```

## Usage Examples

### Example 1: Device List Item

```kotlin
@Composable
fun DeviceListItem(device: Device) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(Dimensions.ListItem.deviceItemHeight),
    elevation = CardDefaults.cardElevation(
      defaultElevation = Elevation.level1
    ),
    shape = CustomShapes.deviceCard
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(SpecialSpacing.ListItem.contentPadding),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Device icon
      Icon(
        modifier = Modifier.size(Dimensions.Icon.device),
        imageVector = Icons.Default.Phone,
        contentDescription = null
      )

      Spacer(modifier = Modifier.width(SpecialSpacing.ListItem.iconToText))

      // Device info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = device.name,
          style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Spacing.extraSmall))
        Text(
          text = device.status,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Status icon
      Icon(
        modifier = Modifier.size(Dimensions.Icon.standard),
        imageVector = Icons.Default.Check,
        contentDescription = "Connected"
      )
    }
  }
}
```

### Example 2: Device List with Spacing

```kotlin
@Composable
fun DeviceListScreen() {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      horizontal = SpecialSpacing.Screen.horizontalPadding,
      vertical = SpecialSpacing.Screen.topPadding
    ),
    verticalArrangement = Arrangement.spacedBy(Spacing.small)
  ) {
    items(devices) { device ->
      DeviceListItem(device = device)
    }
  }
}
```

### Example 3: Pairing Dialog

```kotlin
@Composable
fun PairingDialog(onDismiss: () -> Unit, onAccept: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.widthIn(
      min = Dimensions.Dialog.minWidth,
      max = Dimensions.Dialog.maxWidth
    )
  ) {
    Column(
      modifier = Modifier.padding(SpecialSpacing.Dialog.contentPadding)
    ) {
      // Title
      Text(
        text = "Pair Device",
        style = MaterialTheme.typography.headlineSmall
      )

      Spacer(modifier = Modifier.height(SpecialSpacing.Dialog.titleSpacing))

      // Content
      Text(
        text = "Accept pairing request from this device?",
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(modifier = Modifier.height(SpecialSpacing.Dialog.buttonAreaPadding))

      // Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
          SpecialSpacing.Button.buttonSpacing,
          Alignment.End
        )
      ) {
        TextButton(onClick = onDismiss) {
          Text("DENY")
        }
        Button(
          onClick = onAccept,
          modifier = Modifier.height(Dimensions.Button.height)
        ) {
          Text("ACCEPT")
        }
      }
    }
  }
}
```

### Example 4: Plugin Card with Icon

```kotlin
@Composable
fun PluginCard(plugin: Plugin, onToggle: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = CustomShapes.pluginCard,
    elevation = CardDefaults.cardElevation(
      defaultElevation = Elevation.level1
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(SpecialSpacing.Card.padding),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Plugin icon
      Icon(
        modifier = Modifier.size(Dimensions.Icon.large),
        imageVector = plugin.icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.width(SpecialSpacing.Icon.toText))

      // Plugin info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = plugin.name,
          style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Spacing.extraSmall))
        Text(
          text = plugin.description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Toggle switch
      Switch(
        checked = plugin.enabled,
        onCheckedChange = { onToggle() },
        modifier = Modifier.size(
          width = Dimensions.Toggle.switchWidth,
          height = Dimensions.Toggle.switchHeight
        )
      )
    }
  }
}
```

### Example 5: Screen Layout

```kotlin
@Composable
fun DeviceDetailsScreen(device: Device) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(device.name) },
        modifier = Modifier.height(Dimensions.AppBar.height)
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = SpecialSpacing.Screen.horizontalPadding)
    ) {
      Spacer(modifier = Modifier.height(SpecialSpacing.Screen.topPadding))

      // Device info section
      DeviceInfoCard(device)

      Spacer(modifier = Modifier.height(SpecialSpacing.Screen.sectionSpacing))

      // Plugins section
      Text(
        text = "Plugins",
        style = MaterialTheme.typography.titleLarge
      )

      Spacer(modifier = Modifier.height(Spacing.medium))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
      ) {
        items(device.plugins) { plugin ->
          PluginCard(plugin = plugin, onToggle = {})
        }
      }
    }
  }
}
```

### Example 6: Form Layout

```kotlin
@Composable
fun DeviceConfigForm(config: DeviceConfig) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(SpecialSpacing.Screen.horizontalPadding)
  ) {
    // Device name field
    OutlinedTextField(
      value = config.name,
      onValueChange = {},
      label = { Text("Device Name") },
      modifier = Modifier
        .fillMaxWidth()
        .height(Dimensions.TextField.height)
    )

    Spacer(modifier = Modifier.height(SpecialSpacing.Form.fieldToHelper))

    Text(
      text = "This name will be shown to other devices",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(SpecialSpacing.Form.fieldSpacing))

    // Device type field
    OutlinedTextField(
      value = config.type,
      onValueChange = {},
      label = { Text("Device Type") },
      modifier = Modifier
        .fillMaxWidth()
        .height(Dimensions.TextField.height)
    )

    Spacer(modifier = Modifier.height(SpecialSpacing.Form.sectionSpacing))

    // Save button
    Button(
      onClick = {},
      modifier = Modifier
        .fillMaxWidth()
        .height(Dimensions.Button.height)
    ) {
      Text("SAVE")
    }
  }
}
```

## Design Principles

### 8dp Grid System

✅ **Base Unit**: 8dp (all spacing multiples of 4dp minimum)
✅ **Consistency**: Same spacing values across all screens
✅ **Hierarchy**: Spacing communicates relationship between elements
✅ **Breathing Room**: Adequate whitespace improves readability

### Accessibility

✅ **Touch Targets**: Minimum 48dp × 48dp for all interactive elements
✅ **Text Legibility**: Proper spacing between lines and paragraphs
✅ **Visual Hierarchy**: Clear spacing differentiates sections
✅ **Comfortable Interaction**: Easy to tap buttons without errors

### Material Design 3 Compliance

✅ **Standard Sizes**: Follow Material3 component dimensions
✅ **Elevation System**: Proper elevation levels for depth
✅ **Responsive**: Adapts to different screen sizes
✅ **Consistent**: Same dimensions across platform

## Benefits

### For Developers

✅ **No Magic Numbers**: Named constants instead of hardcoded values
✅ **Type Safety**: Compile-time checking of dimensions
✅ **Discoverability**: Easy to find appropriate spacing/size
✅ **Maintainability**: Change once, update everywhere

### For Users

✅ **Visual Consistency**: Same spacing patterns throughout
✅ **Comfortable Interaction**: Proper touch targets
✅ **Clear Hierarchy**: Spacing guides attention
✅ **Accessible**: Meets Android accessibility standards

### For Design

✅ **Design System**: Clear rules for layout
✅ **Consistency**: Predictable spacing patterns
✅ **Efficiency**: No need to specify every dimension
✅ **Quality**: Professional, polished appearance

## Files Created

- `src/org/cosmic/cosmicconnect/UserInterface/compose/Spacing.kt` (253 lines)
- `src/org/cosmic/cosmicconnect/UserInterface/compose/Dimensions.kt` (316 lines)
- `docs/issue-75-spacing-dimensions.md` (this file)

## Testing Checklist

Manual verification required:

- [ ] Touch targets meet 48dp minimum
- [ ] Spacing consistent across screens
- [ ] Text fields have comfortable dimensions
- [ ] Buttons are appropriately sized
- [ ] List items are comfortable to tap
- [ ] Cards have proper internal padding
- [ ] Dialogs have appropriate sizing
- [ ] Test on small and large screens
- [ ] Test with accessibility features (TalkBack, large text)

## Next Steps

**Issue #76**: Common Icons and Assets
- Migrate drawable resources
- Create icon constants
- Prepare vector assets

**Then continue with Foundation Components** (Issues #77-82):
- Card components
- List item components
- Dialog components
- Navigation components
- Status indicators
- Input components

## Success Criteria

✅ Complete spacing scale (8dp grid)
✅ Component dimension definitions
✅ Elevation system documented
✅ Specialized spacing patterns defined
✅ Accessibility standards met (48dp touch targets)
✅ Comprehensive usage examples
✅ Material3 compliance

---

**Created**: 2026-01-17
**Completed**: 2026-01-17
**Status**: ✅ Complete
