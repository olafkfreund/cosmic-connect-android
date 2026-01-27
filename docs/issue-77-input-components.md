# Issue #77: Input Components

**Created:** 2026-01-17
**Status:** ✅ Completed
**Effort:** 8 hours
**Phase:** 4.2 - Foundation Components

## Overview

This document details the implementation of comprehensive Material3 input components for COSMIC Connect Android. These components provide consistent, accessible, and theme-aware input controls following Material Design 3 guidelines.

## Components Implemented

### 1. Text Field Components

#### CosmicTextField
Standard Material3 text field with full customization support.

**Features:**
- Material3 styling with dynamic colors
- Label and placeholder support
- Leading and trailing icon support
- Supporting text (helper/error)
- Error state handling
- Keyboard configuration
- Accessibility support

**Parameters:**
```kotlin
@Composable
fun CosmicTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  placeholder: String? = null,
  leadingIcon: ImageVector? = null,
  trailingIcon: ImageVector? = null,
  onTrailingIconClick: (() -> Unit)? = null,
  supportingText: String? = null,
  isError: Boolean = false,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  singleLine: Boolean = true,
  maxLines: Int = 1,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default
)
```

**Usage Example:**
```kotlin
CosmicTextField(
  value = deviceName,
  onValueChange = { deviceName = it },
  label = "Device Name",
  placeholder = "Enter device name",
  supportingText = "This name will be visible to other devices",
  leadingIcon = Icons.Default.Phone
)
```

#### CosmicOutlinedTextField
Outlined variant with clearer visual separation.

**Features:**
- All CosmicTextField features
- Outlined border for better distinction
- Better suited for forms and settings

**Usage Example:**
```kotlin
CosmicOutlinedTextField(
  value = email,
  onValueChange = { email = it },
  label = "Email",
  placeholder = "user@example.com",
  supportingText = if (emailError) "Invalid email" else null,
  isError = emailError,
  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
)
```

#### CosmicPasswordField
Specialized password input with visibility toggle.

**Features:**
- Password masking by default
- Visibility toggle button
- Secure keyboard type
- Done IME action

**Usage Example:**
```kotlin
CosmicPasswordField(
  value = password,
  onValueChange = { password = it },
  label = "Password",
  supportingText = if (passwordError) "Password must be at least 8 characters" else null,
  isError = passwordError
)
```

#### CosmicSearchField
Search-optimized text field.

**Features:**
- Search icon
- Clear button when text present
- Search IME action
- onSearch callback

**Usage Example:**
```kotlin
CosmicSearchField(
  value = searchQuery,
  onValueChange = { searchQuery = it },
  placeholder = "Search devices...",
  onSearch = { performSearch(searchQuery) }
)
```

### 2. Slider Components

#### CosmicSlider
Value selection slider with label and value display.

**Features:**
- Continuous or stepped values
- Label and value display
- Custom value formatter
- Disabled state support
- Material3 colors

**Parameters:**
```kotlin
@Composable
fun CosmicSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  steps: Int = 0,
  enabled: Boolean = true,
  showValue: Boolean = true,
  valueFormatter: (Float) -> String = { "%.1f".format(it) }
)
```

**Usage Example:**
```kotlin
CosmicSlider(
  value = volume,
  onValueChange = { volume = it },
  label = "Volume",
  valueFormatter = { "${(it * 100).toInt()}%" }
)

CosmicSlider(
  value = batteryThreshold.toFloat(),
  onValueChange = { batteryThreshold = it.toInt() },
  label = "Battery Alert Threshold",
  valueRange = 0f..100f,
  steps = 19, // 5% increments
  valueFormatter = { "${it.toInt()}%" }
)
```

#### CosmicRangeSlider
Range selection with start and end values.

**Features:**
- Two-thumb slider
- Range value display
- Custom value formatter
- Material3 colors

**Usage Example:**
```kotlin
CosmicRangeSlider(
  value = priceRange,
  onValueChange = { priceRange = it },
  label = "Price Range",
  valueRange = 0f..1000f,
  valueFormatter = { "$${it.toInt()}" }
)
```

### 3. Selection Components

#### CosmicSwitch
Toggle switch with label and optional description.

**Features:**
- Label and description text
- Material3 switch styling
- Proper touch target (56dp minimum)
- Disabled state support

**Parameters:**
```kotlin
@Composable
fun CosmicSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  label: String,
  description: String? = null,
  enabled: Boolean = true
)
```

**Usage Example:**
```kotlin
CosmicSwitch(
  checked = notificationsEnabled,
  onCheckedChange = { notificationsEnabled = it },
  label = "Enable notifications",
  description = "Receive alerts when devices connect or disconnect"
)
```

#### CosmicCheckbox
Checkbox with label for non-exclusive selection.

**Features:**
- Material3 checkbox styling
- Full-width clickable area
- Accessibility support (Role.Checkbox)
- Disabled state support

**Usage Example:**
```kotlin
CosmicCheckbox(
  checked = agreeToTerms,
  onCheckedChange = { agreeToTerms = it },
  label = "I agree to the terms and conditions"
)
```

#### CosmicRadioButton
Radio button with label for exclusive selection.

**Features:**
- Material3 radio button styling
- Full-width clickable area
- Accessibility support (Role.RadioButton)
- Disabled state support

**Usage Example:**
```kotlin
CosmicRadioButton(
  selected = selectedTheme == "Light",
  onClick = { selectedTheme = "Light" },
  label = "Light Theme"
)
```

#### CosmicRadioGroup
Radio button group for exclusive selection.

**Features:**
- Automatic selectableGroup
- List of options with single selection
- Disabled state support

**Usage Example:**
```kotlin
CosmicRadioGroup(
  options = listOf("Light", "Dark", "System"),
  selectedOption = theme,
  onOptionSelected = { theme = it }
)
```

#### CosmicChipGroup
Selectable chip group for filtering/selection.

**Features:**
- Single or multi-select mode
- FlowRow layout (wraps to multiple lines)
- Material3 FilterChip styling
- Disabled state support

**Parameters:**
```kotlin
@Composable
fun CosmicChipGroup(
  chips: List<String>,
  selectedChips: Set<String>,
  onChipSelected: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  multiSelect: Boolean = true
)
```

**Usage Example:**
```kotlin
// Multi-select
CosmicChipGroup(
  chips = listOf("Battery", "Clipboard", "Share", "Ping", "RunCommand", "MPRIS"),
  selectedChips = enabledPlugins,
  onChipSelected = { plugin, selected ->
    enabledPlugins = if (selected) {
      enabledPlugins + plugin
    } else {
      enabledPlugins - plugin
    }
  },
  multiSelect = true
)

// Single-select
CosmicChipGroup(
  chips = listOf("Low", "Medium", "High"),
  selectedChips = setOf(quality),
  onChipSelected = { chip, _ -> quality = chip },
  multiSelect = false
)
```

### 4. Dropdown Components

#### CosmicDropdownMenu
Dropdown menu for selecting from a list.

**Features:**
- ExposedDropdownMenuBox implementation
- Outlined text field anchor
- Material3 menu styling
- Disabled state support

**Parameters:**
```kotlin
@Composable
fun CosmicDropdownMenu(
  options: List<String>,
  selectedOption: String,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  enabled: Boolean = true
)
```

**Usage Example:**
```kotlin
CosmicDropdownMenu(
  options = deviceList.map { it.name },
  selectedOption = selectedDevice,
  onOptionSelected = { selectedDevice = it },
  label = "Select device"
)
```

## Design Principles

### Material Design 3 Compliance

All input components follow Material3 specifications:

1. **Color System**
   - Uses Material3 color roles (primary, surface, error, etc.)
   - Supports light and dark themes
   - Proper disabled state colors (38% opacity)

2. **Typography**
   - Uses Material3 type scale
   - Proper text styles for labels, values, and descriptions
   - Consistent font sizes and weights

3. **Touch Targets**
   - Minimum 48dp touch targets (56dp for switches with descriptions)
   - Full-width clickable areas for checkboxes and radio buttons
   - Proper spacing between interactive elements

4. **States**
   - Default, focused, disabled, error states
   - Animated state transitions
   - Clear visual feedback

### Accessibility

All components include accessibility support:

1. **Semantic Roles**
   - Checkbox role for checkboxes
   - RadioButton role for radio buttons
   - Proper ARIA labels

2. **Content Descriptions**
   - Icons have content descriptions
   - Labels are properly associated with controls

3. **Keyboard Navigation**
   - Keyboard options for text fields
   - Keyboard actions (Done, Search, etc.)
   - Tab navigation support

4. **Visual Feedback**
   - Clear focus indicators
   - Disabled state indicators (reduced opacity)
   - Error state indicators (color + text)

### Consistency

All components maintain consistency:

1. **API Design**
   - Consistent parameter names and order
   - Common patterns (value, onValueChange, modifier, enabled)
   - Optional parameters with sensible defaults

2. **Styling**
   - Uses Spacing constants for consistent padding
   - Uses theme colors exclusively
   - Consistent animation patterns

3. **Behavior**
   - Predictable interactions
   - Standard Material3 behaviors
   - Proper state management

## Integration with Design System

### Dependencies

```kotlin
import org.cosmic.cconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cconnect.UserInterface.compose.Spacing
import org.cosmic.cconnect.UserInterface.compose.Dimensions
```

### Theme Integration

All components automatically adapt to the current theme:

```kotlin
CosmicTheme(darkTheme = isSystemInDarkTheme()) {
  // All input components use theme colors
  CosmicTextField(...)
  CosmicSlider(...)
  CosmicSwitch(...)
}
```

### Spacing Usage

Components use the Spacing system:

```kotlin
// Internal spacing
Column(verticalArrangement = Arrangement.spacedBy(Spacing.md))
Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm))

// Padding
modifier = Modifier.padding(Spacing.md)
modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
```

## Usage Patterns

### Form Example

```kotlin
@Composable
fun DeviceSettingsForm(
  deviceName: String,
  onDeviceNameChange: (String) -> Unit,
  notificationsEnabled: Boolean,
  onNotificationsChange: (Boolean) -> Unit,
  batteryThreshold: Int,
  onBatteryThresholdChange: (Int) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(Spacing.md),
    verticalArrangement = Arrangement.spacedBy(Spacing.md)
  ) {
    CosmicOutlinedTextField(
      value = deviceName,
      onValueChange = onDeviceNameChange,
      label = "Device Name",
      placeholder = "My Phone"
    )

    CosmicSwitch(
      checked = notificationsEnabled,
      onCheckedChange = onNotificationsChange,
      label = "Enable notifications",
      description = "Receive alerts for device events"
    )

    CosmicSlider(
      value = batteryThreshold.toFloat(),
      onValueChange = { onBatteryThresholdChange(it.toInt()) },
      label = "Low Battery Alert",
      valueRange = 0f..100f,
      steps = 19,
      valueFormatter = { "${it.toInt()}%" }
    )
  }
}
```

### Filter Example

```kotlin
@Composable
fun PluginFilter(
  selectedPlugins: Set<String>,
  onPluginSelectionChange: (String, Boolean) -> Unit
) {
  Column(
    modifier = Modifier.padding(Spacing.md),
    verticalArrangement = Arrangement.spacedBy(Spacing.md)
  ) {
    Text(
      text = "Filter by plugins",
      style = MaterialTheme.typography.titleMedium
    )

    CosmicChipGroup(
      chips = listOf("Battery", "Clipboard", "Share", "Ping", "RunCommand", "MPRIS"),
      selectedChips = selectedPlugins,
      onChipSelected = onPluginSelectionChange,
      multiSelect = true
    )
  }
}
```

### Settings Screen Example

```kotlin
@Composable
fun SettingsScreen() {
  var theme by remember { mutableStateOf("System") }
  var autoConnect by remember { mutableStateOf(true) }
  var showNotifications by remember { mutableStateOf(true) }

  Column(modifier = Modifier.fillMaxSize()) {
    CosmicTopAppBar(title = "Settings")

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
      // Theme selection
      SectionHeader(title = "Appearance")
      CosmicRadioGroup(
        options = listOf("Light", "Dark", "System"),
        selectedOption = theme,
        onOptionSelected = { theme = it }
      )

      Divider()

      // Connection settings
      SectionHeader(title = "Connection")
      CosmicSwitch(
        checked = autoConnect,
        onCheckedChange = { autoConnect = it },
        label = "Auto-connect to trusted devices",
        description = "Automatically connect when devices are in range"
      )

      Divider()

      // Notification settings
      SectionHeader(title = "Notifications")
      CosmicSwitch(
        checked = showNotifications,
        onCheckedChange = { showNotifications = it },
        label = "Show notifications"
      )
    }
  }
}
```

## Testing

All components include preview functions for visual testing:

```kotlin
@Preview(name = "Text Fields Light", showBackground = true)
@Composable
private fun PreviewTextFields()

@Preview(name = "Text Fields Dark", showBackground = true)
@Composable
private fun PreviewTextFieldsDark()

@Preview(name = "Sliders", showBackground = true)
@Composable
private fun PreviewSliders()

@Preview(name = "Selection Components", showBackground = true)
@Composable
private fun PreviewSelectionComponents()

@Preview(name = "Chip Group", showBackground = true)
@Composable
private fun PreviewChipGroup()

@Preview(name = "Dropdown Menu", showBackground = true)
@Composable
private fun PreviewDropdownMenu()
```

## File Structure

```
src/org/cosmic/cosmicconnect/UserInterface/compose/
└── InputComponents.kt (733 lines)
    ├── Text Field Components
    │   ├── CosmicTextField
    │   ├── CosmicOutlinedTextField
    │   ├── CosmicPasswordField
    │   └── CosmicSearchField
    ├── Slider Components
    │   ├── CosmicSlider
    │   └── CosmicRangeSlider
    ├── Selection Components
    │   ├── CosmicSwitch
    │   ├── CosmicCheckbox
    │   ├── CosmicRadioButton
    │   ├── CosmicRadioGroup
    │   └── CosmicChipGroup
    ├── Dropdown Components
    │   └── CosmicDropdownMenu
    └── Preview Functions
```

## Dependencies

### Foundation Components Used

- **Issue #69**: Material3 Design System (Colors, Typography, CosmicTheme)
- **Issue #70**: Spacing and Dimensions
- **Issue #71**: Icons and Assets (Material Icons)

### External Dependencies

```kotlin
// Material3
import androidx.compose.material3.*

// Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// Compose Foundation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.*
import androidx.compose.foundation.text.*

// Compose UI
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.*
```

## Future Enhancements

Potential improvements for future iterations:

1. **Additional Components**
   - Date/Time pickers
   - Color picker
   - File picker integration
   - Multi-line text area with character counter

2. **Enhanced Validation**
   - Built-in validation rules
   - Real-time validation feedback
   - Custom validators

3. **Autocomplete Enhancements**
   - Dropdown suggestions
   - Recent searches
   - Fuzzy matching

4. **Accessibility**
   - Screen reader optimization
   - High contrast mode support
   - Larger touch targets option

5. **Animation**
   - Custom entry/exit animations
   - Value change animations
   - Error shake animation

## References

- [Material Design 3 Components](https://m3.material.io/components)
- [Jetpack Compose Material3](https://developer.android.com/jetpack/compose/designsystems/material3)
- [Compose TextField](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#TextField(kotlin.String,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Boolean,kotlin.Boolean,androidx.compose.ui.text.TextStyle,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Boolean,androidx.compose.ui.text.input.VisualTransformation,androidx.compose.foundation.text.KeyboardOptions,androidx.compose.foundation.text.KeyboardActions,kotlin.Boolean,kotlin.Int,kotlin.Int,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.ui.graphics.Shape,androidx.compose.material3.TextFieldColors))
- [Compose Slider](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Slider(kotlin.Float,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Boolean,kotlin.Function0,androidx.compose.material3.SliderColors,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Int,kotlin.Function0,kotlin.Function0,kotlin.ranges.ClosedFloatingPointRange))

---

**Issue #77 Complete** ✅
All input components implemented with comprehensive Material3 support, accessibility features, and preview functions.
