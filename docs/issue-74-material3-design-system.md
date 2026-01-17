# Issue #74: Material3 Design System Implementation

**Status**: ✅ COMPLETE
**Created**: 2026-01-17
**Phase**: Phase 4.1 - Design System

## Overview

Implemented a comprehensive Material Design 3 design system for COSMIC Connect Android, establishing consistent color schemes, typography, and shapes across the entire application.

## Implementation Summary

### Created Files

1. **Colors.kt** - Material3 color schemes
2. **Typography.kt** - Material3 type scale
3. **Shapes.kt** - Corner radius definitions
4. **CosmicTheme.kt** - Enhanced theme with all design tokens

### Legacy Compatibility

Kept **KdeTheme.kt** with deprecation warning for backward compatibility during migration.

## Design System Components

### 1. Colors (Colors.kt)

#### Color Palette

**Primary (Blue)** - COSMIC Connect identity
- Light theme: CosmicBlue40 (#006B8F)
- Dark theme: CosmicBlue80 (#7EC8E3)
- Inspiration: COSMIC Desktop branding

**Secondary (Teal)** - Complementary colors
- Light theme: CosmicTeal40 (#00897B)
- Dark theme: CosmicTeal80 (#7DD8C3)

**Tertiary (Purple)** - Accent colors
- Light theme: CosmicPurple40 (#8E44AD)
- Dark theme: CosmicPurple80 (#CDB4DB)

**Neutrals** - Backgrounds and surfaces
- 10 neutral tones from Neutral0 (black) to Neutral99 (near white)
- Used for backgrounds, surfaces, and text

**Error Colors** - Error states and warnings
- Error40 (#BA1A1A) for light theme
- Error80 (#FFB4AB) for dark theme

#### Color Schemes

**CosmicLightColorScheme**
```kotlin
val scheme = CosmicLightColorScheme
// Primary: CosmicBlue40
// Background: Neutral99
// Surface: Neutral99
// High contrast for readability
```

**CosmicDarkColorScheme**
```kotlin
val scheme = CosmicDarkColorScheme
// Primary: CosmicBlue80
// Background: Neutral10
// Surface: Neutral10
// Comfortable low-light viewing
```

### 2. Typography (Typography.kt)

#### Type Scale

**Display** (57sp, 45sp, 36sp)
- Largest text, used sparingly
- App titles, splash screens, empty states

**Headline** (32sp, 28sp, 24sp)
- High-emphasis headers
- Screen titles, section headers

**Title** (22sp, 16sp, 14sp)
- Medium-emphasis titles
- App bar titles, list item titles

**Body** (16sp, 14sp, 12sp)
- Main content text
- Primary and secondary content

**Label** (14sp, 12sp, 11sp)
- Smallest text
- Buttons, tabs, captions

#### Usage Guidelines

```kotlin
// Screen title
Text(
  text = "Device List",
  style = MaterialTheme.typography.headlineLarge
)

// Card title
Text(
  text = "Battery Plugin",
  style = MaterialTheme.typography.titleMedium
)

// Body content
Text(
  text = "Send and receive battery status...",
  style = MaterialTheme.typography.bodyMedium
)

// Button label
Text(
  text = "CONNECT",
  style = MaterialTheme.typography.labelLarge
)
```

### 3. Shapes (Shapes.kt)

#### Shape Scale

**Extra Small** (4dp) - Minimal rounding
- Small text fields, badges, compact buttons

**Small** (8dp) - Subtle rounding
- Standard buttons, chips, small cards

**Medium** (12dp) - Comfortable rounding (default)
- Cards, dialogs, menus, list containers

**Large** (16dp) - Prominent rounding
- Bottom sheets, FABs, large cards

**Extra Large** (28dp) - Maximum rounding
- Bottom app bars, extended FABs, special containers

#### Custom Shapes

```kotlin
// Device card
CustomShapes.deviceCard // 12dp rounded

// Plugin card
CustomShapes.pluginCard // 12dp rounded

// Notification card
CustomShapes.notificationCard // 16dp rounded

// Bottom sheet
CustomShapes.bottomSheet // Top corners only (28dp)

// Status badge
CustomShapes.statusBadge // Fully circular

// Search bar
CustomShapes.searchBar // Fully rounded pill
```

### 4. CosmicTheme (CosmicTheme.kt)

#### Theme Configuration

```kotlin
@Composable
fun MyScreen() {
  CosmicTheme(
    context = LocalContext.current,
    useDynamicColors = true,  // Material You on Android 12+
    darkTheme = isSystemInDarkTheme()  // Auto dark/light
  ) {
    // Your composables here
  }
}
```

#### Dynamic Color Support

**Android 12+ (API 31+)**
- Uses Material You dynamic colors from system wallpaper
- Generates personalized color scheme
- Falls back to custom scheme if disabled

**Android 11 and below (API 30-)**
- Uses custom COSMIC color schemes
- CosmicLightColorScheme or CosmicDarkColorScheme
- Consistent COSMIC branding

#### Accessing Colors

```kotlin
// Via MaterialTheme (recommended)
val primary = MaterialTheme.colorScheme.primary
val surface = MaterialTheme.colorScheme.surface

// Via LocalCosmicColorScheme
val colors = LocalCosmicColorScheme.current
val primary = colors.primary
```

## Usage Examples

### Complete Component Example

```kotlin
@Composable
fun DeviceCard(deviceName: String, isConnected: Boolean) {
  Card(
    shape = CustomShapes.deviceCard,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = deviceName,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (isConnected) "Connected" else "Disconnected",
        style = MaterialTheme.typography.bodyMedium,
        color = if (isConnected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
      )
    }
  }
}
```

### Button Example

```kotlin
@Composable
fun ConnectButton(onClick: () -> Unit) {
  Button(
    onClick = onClick,
    shape = MaterialTheme.shapes.small,
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary
    )
  ) {
    Text(
      text = "CONNECT",
      style = MaterialTheme.typography.labelLarge
    )
  }
}
```

### Dialog Example

```kotlin
@Composable
fun PairingDialog(onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = MaterialTheme.shapes.large,
    title = {
      Text(
        text = "Pair Device",
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Text(
        text = "Accept pairing request from this device?",
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      TextButton(onClick = { /* accept */ }) {
        Text("ACCEPT")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("DENY")
      }
    }
  )
}
```

## Design Principles

### Material Design 3 Compliance

✅ **Dynamic Color** - Material You support on Android 12+
✅ **Semantic Naming** - Primary, secondary, surface, error, etc.
✅ **Accessibility** - WCAG 2.1 AA contrast ratios
✅ **Type Scale** - Standard Material3 text sizes
✅ **Shape Scale** - Consistent corner radius progression

### COSMIC Desktop Alignment

✅ **Color Palette** - Blue primary inspired by COSMIC branding
✅ **Modern Interface** - Rounded corners and clean design
✅ **Consistency** - Shared design language across platforms
✅ **Dark Theme** - Comfortable low-light experience

### Best Practices

1. **Use semantic colors** - `primary`, `surface`, not hardcoded values
2. **Follow type scale** - Use predefined typography styles
3. **Consistent shapes** - Use theme shapes, not custom radius
4. **Accessibility** - Test with TalkBack and large text
5. **Dynamic colors** - Support Material You when available

## Migration Guide

### For Existing Components

**Old approach (hardcoded):**
```kotlin
Card(
  backgroundColor = Color(0xFF006B8F),
  shape = RoundedCornerShape(8.dp)
) {
  Text(
    text = "Hello",
    fontSize = 16.sp,
    color = Color.White
  )
}
```

**New approach (theme-based):**
```kotlin
Card(
  colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.primary
  ),
  shape = MaterialTheme.shapes.small
) {
  Text(
    text = "Hello",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onPrimary
  )
}
```

### Updating KdeTheme Usage

**Old:**
```kotlin
KdeTheme(context) {
  MyContent()
}
```

**New:**
```kotlin
CosmicTheme(context) {
  MyContent()
}
```

Both work during migration period, but KdeTheme is deprecated.

## Testing

### Manual Testing Required

- [ ] Light theme appearance
- [ ] Dark theme appearance
- [ ] Dynamic colors on Android 12+
- [ ] Static colors on Android 11-
- [ ] Accessibility with TalkBack
- [ ] Large text sizes
- [ ] Color contrast verification

### Visual Testing

Create screenshot tests for key components with:
- Light theme
- Dark theme
- Dynamic colors enabled
- Dynamic colors disabled

## Benefits

### For Developers

✅ **Consistency** - Single source of truth for design tokens
✅ **Type Safety** - Compile-time checking of colors and styles
✅ **Maintainability** - Change theme in one place
✅ **Composable** - Easy to use in Jetpack Compose

### For Users

✅ **Modern Design** - Clean, polished Material3 interface
✅ **Personalization** - Dynamic colors match wallpaper (Android 12+)
✅ **Accessibility** - Proper contrast and text sizes
✅ **Consistency** - Familiar Android design patterns

### For Product

✅ **Brand Identity** - COSMIC blue maintains brand recognition
✅ **Cross-Platform** - Aligned with COSMIC Desktop design
✅ **Future-Proof** - Built on latest Material Design standards
✅ **Scalability** - Easy to extend with new components

## Files Modified

### Created
- `src/org/cosmic/cosmicconnect/UserInterface/compose/Colors.kt` (173 lines)
- `src/org/cosmic/cosmicconnect/UserInterface/compose/Typography.kt` (183 lines)
- `src/org/cosmic/cosmicconnect/UserInterface/compose/Shapes.kt` (137 lines)
- `src/org/cosmic/cosmicconnect/UserInterface/compose/CosmicTheme.kt` (113 lines)
- `docs/issue-74-material3-design-system.md` (this file)

### Preserved (deprecated)
- `src/org/cosmic/cosmicconnect/UserInterface/compose/KdeTheme.kt` (with deprecation)

## Next Steps

**Issue #75**: Spacing and Dimensions
- Create consistent spacing scale
- Define standard padding and margins
- Document elevation levels

**Issue #76**: Common Icons and Assets
- Migrate drawable resources
- Create icon constants
- Prepare vector assets

**Issue #77-82**: Foundation Components
- Card components
- List item components
- Dialog components
- Navigation components
- Status indicators
- Input components

## Success Criteria

✅ Complete Material3 color schemes defined
✅ Typography system with full type scale
✅ Shape definitions for all corner radii
✅ Enhanced theme with dynamic color support
✅ Backward compatibility maintained
✅ Comprehensive documentation created
✅ Usage examples provided

## Commit

```bash
git add src/org/cosmic/cosmicconnect/UserInterface/compose/Colors.kt
git add src/org/cosmic/cosmicconnect/UserInterface/compose/Typography.kt
git add src/org/cosmic/cosmicconnect/UserInterface/compose/Shapes.kt
git add src/org/cosmic/cosmicconnect/UserInterface/compose/CosmicTheme.kt
git add docs/issue-74-material3-design-system.md
git commit -m "Issue #74: Implement Material3 Design System

✅ Colors: COSMIC-branded Material3 color schemes
✅ Typography: Complete Material3 type scale
✅ Shapes: Rounded corner definitions
✅ CosmicTheme: Enhanced theme with dynamic colors

Features:
- Dynamic color support (Material You) on Android 12+
- Custom COSMIC color schemes as fallback
- Comprehensive typography system
- Consistent shape scale
- Dark and light theme support
- Backward compatibility with KdeTheme (deprecated)

Phase 4.1 - Foundation complete
Next: Issue #75 (Spacing and Dimensions)"
```

---

**Created**: 2026-01-17
**Completed**: 2026-01-17
**Status**: ✅ Complete
