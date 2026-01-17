package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Material3 Text Field Components
 *
 * Provides consistent text input components following Material3 design guidelines.
 * Supports validation states, icons, and accessibility.
 */

/**
 * Standard COSMIC text field with Material3 styling.
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param modifier Modifier for customization
 * @param label Label text displayed above the field
 * @param placeholder Placeholder text when field is empty
 * @param leadingIcon Optional icon at the start of the field
 * @param trailingIcon Optional icon at the end of the field
 * @param supportingText Optional helper or error text below the field
 * @param isError Whether the field is in error state
 * @param enabled Whether the field is enabled
 * @param readOnly Whether the field is read-only
 * @param singleLine Whether to restrict input to a single line
 * @param maxLines Maximum number of visible lines
 * @param keyboardOptions Keyboard configuration
 * @param keyboardActions Keyboard action handlers
 */
@OptIn(ExperimentalMaterial3Api::class)
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
) {
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    label = label?.let { { Text(it) } },
    placeholder = placeholder?.let { { Text(it) } },
    leadingIcon = leadingIcon?.let {
      {
        Icon(
          imageVector = it,
          contentDescription = null
        )
      }
    },
    trailingIcon = trailingIcon?.let {
      {
        IconButton(onClick = { onTrailingIconClick?.invoke() }) {
          Icon(
            imageVector = it,
            contentDescription = null
          )
        }
      }
    },
    supportingText = supportingText?.let { { Text(it) } },
    isError = isError,
    enabled = enabled,
    readOnly = readOnly,
    singleLine = singleLine,
    maxLines = maxLines,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = TextFieldDefaults.colors()
  )
}

/**
 * Outlined variant of COSMIC text field.
 *
 * @param value Current text value
 * @param onValueChange Callback when text changes
 * @param modifier Modifier for customization
 * @param label Label text displayed in the border
 * @param placeholder Placeholder text when field is empty
 * @param leadingIcon Optional icon at the start of the field
 * @param trailingIcon Optional icon at the end of the field
 * @param supportingText Optional helper or error text below the field
 * @param isError Whether the field is in error state
 * @param enabled Whether the field is enabled
 * @param readOnly Whether the field is read-only
 * @param singleLine Whether to restrict input to a single line
 * @param maxLines Maximum number of visible lines
 * @param keyboardOptions Keyboard configuration
 * @param keyboardActions Keyboard action handlers
 */
@Composable
fun CosmicOutlinedTextField(
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
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.fillMaxWidth(),
    label = label?.let { { Text(it) } },
    placeholder = placeholder?.let { { Text(it) } },
    leadingIcon = leadingIcon?.let {
      {
        Icon(
          imageVector = it,
          contentDescription = null
        )
      }
    },
    trailingIcon = trailingIcon?.let {
      {
        IconButton(onClick = { onTrailingIconClick?.invoke() }) {
          Icon(
            imageVector = it,
            contentDescription = null
          )
        }
      }
    },
    supportingText = supportingText?.let { { Text(it) } },
    isError = isError,
    enabled = enabled,
    readOnly = readOnly,
    singleLine = singleLine,
    maxLines = maxLines,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = OutlinedTextFieldDefaults.colors()
  )
}

/**
 * Password text field with visibility toggle.
 *
 * @param value Current password value
 * @param onValueChange Callback when password changes
 * @param modifier Modifier for customization
 * @param label Label text
 * @param placeholder Placeholder text
 * @param supportingText Optional helper or error text
 * @param isError Whether the field is in error state
 * @param enabled Whether the field is enabled
 * @param keyboardActions Keyboard action handlers
 */
@Composable
fun CosmicPasswordField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String = "Password",
  placeholder: String? = null,
  supportingText: String? = null,
  isError: Boolean = false,
  enabled: Boolean = true,
  keyboardActions: KeyboardActions = KeyboardActions.Default
) {
  var passwordVisible by remember { mutableStateOf(false) }

  CosmicOutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    label = label,
    placeholder = placeholder,
    trailingIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
    onTrailingIconClick = { passwordVisible = !passwordVisible },
    supportingText = supportingText,
    isError = isError,
    enabled = enabled,
    singleLine = true,
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      imeAction = ImeAction.Done
    ),
    keyboardActions = keyboardActions
  )
}

/**
 * Search text field with search icon and clear button.
 *
 * @param value Current search query
 * @param onValueChange Callback when query changes
 * @param modifier Modifier for customization
 * @param placeholder Placeholder text
 * @param onSearch Callback when search is submitted
 * @param enabled Whether the field is enabled
 */
@Composable
fun CosmicSearchField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  placeholder: String = "Search...",
  onSearch: (() -> Unit)? = null,
  enabled: Boolean = true
) {
  CosmicOutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    placeholder = placeholder,
    leadingIcon = Icons.Default.Search,
    trailingIcon = if (value.isNotEmpty()) Icons.Default.Clear else null,
    onTrailingIconClick = { onValueChange("") },
    enabled = enabled,
    singleLine = true,
    keyboardOptions = KeyboardOptions(
      imeAction = ImeAction.Search
    ),
    keyboardActions = KeyboardActions(
      onSearch = { onSearch?.invoke() }
    )
  )
}

/**
 * Material3 Slider Components
 *
 * Value selection components with visual feedback.
 */

/**
 * Slider with label and value display.
 *
 * @param value Current slider value
 * @param onValueChange Callback when value changes
 * @param modifier Modifier for customization
 * @param label Label text above the slider
 * @param valueRange Range of possible values
 * @param steps Number of discrete steps (0 for continuous)
 * @param enabled Whether the slider is enabled
 * @param showValue Whether to display the current value
 * @param valueFormatter Custom formatter for the value display
 */
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
) {
  Column(modifier = modifier) {
    if (label != null || showValue) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (label != null) {
          Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
          )
        }
        if (showValue) {
          Text(
            text = valueFormatter(value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
      Spacer(modifier = Modifier.height(Spacing.xs))
    }

    Slider(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth(),
      valueRange = valueRange,
      steps = steps,
      enabled = enabled
    )
  }
}

/**
 * Range slider for selecting a value range.
 *
 * @param value Current range values
 * @param onValueChange Callback when range changes
 * @param modifier Modifier for customization
 * @param label Label text above the slider
 * @param valueRange Range of possible values
 * @param steps Number of discrete steps (0 for continuous)
 * @param enabled Whether the slider is enabled
 * @param showValues Whether to display the current values
 * @param valueFormatter Custom formatter for the value display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmicRangeSlider(
  value: ClosedFloatingPointRange<Float>,
  onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  steps: Int = 0,
  enabled: Boolean = true,
  showValues: Boolean = true,
  valueFormatter: (Float) -> String = { "%.1f".format(it) }
) {
  Column(modifier = modifier) {
    if (label != null || showValues) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (label != null) {
          Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
          )
        }
        if (showValues) {
          Text(
            text = "${valueFormatter(value.start)} - ${valueFormatter(value.endInclusive)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
      Spacer(modifier = Modifier.height(Spacing.xs))
    }

    RangeSlider(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth(),
      valueRange = valueRange,
      steps = steps,
      enabled = enabled
    )
  }
}

/**
 * Material3 Selection Components
 *
 * Toggle and selection controls.
 */

/**
 * Switch with label.
 *
 * @param checked Whether the switch is checked
 * @param onCheckedChange Callback when checked state changes
 * @param modifier Modifier for customization
 * @param label Label text
 * @param description Optional description text
 * @param enabled Whether the switch is enabled
 */
@Composable
fun CosmicSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  label: String,
  description: String? = null,
  enabled: Boolean = true
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(if (description != null) 72.dp else 56.dp)
      .padding(horizontal = Spacing.md),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = Spacing.md)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (enabled) {
          MaterialTheme.colorScheme.onSurface
        } else {
          MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
      )
      if (description != null) {
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled
    )
  }
}

/**
 * Checkbox with label.
 *
 * @param checked Whether the checkbox is checked
 * @param onCheckedChange Callback when checked state changes
 * @param modifier Modifier for customization
 * @param label Label text
 * @param enabled Whether the checkbox is enabled
 */
@Composable
fun CosmicCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  label: String,
  enabled: Boolean = true
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .selectable(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        role = Role.Checkbox,
        enabled = enabled
      )
      .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(
      checked = checked,
      onCheckedChange = null,
      enabled = enabled
    )
    Spacer(modifier = Modifier.width(Spacing.md))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
      } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
      }
    )
  }
}

/**
 * Radio button with label.
 *
 * @param selected Whether the radio button is selected
 * @param onClick Callback when clicked
 * @param modifier Modifier for customization
 * @param label Label text
 * @param enabled Whether the radio button is enabled
 */
@Composable
fun CosmicRadioButton(
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  label: String,
  enabled: Boolean = true
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
        enabled = enabled
      )
      .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = selected,
      onClick = null,
      enabled = enabled
    )
    Spacer(modifier = Modifier.width(Spacing.md))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
      } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
      }
    )
  }
}

/**
 * Radio button group for exclusive selection.
 *
 * @param options List of options to display
 * @param selectedOption Currently selected option
 * @param onOptionSelected Callback when option is selected
 * @param modifier Modifier for customization
 * @param enabled Whether the group is enabled
 */
@Composable
fun CosmicRadioGroup(
  options: List<String>,
  selectedOption: String,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  Column(
    modifier = modifier.selectableGroup()
  ) {
    options.forEach { option ->
      CosmicRadioButton(
        selected = option == selectedOption,
        onClick = { onOptionSelected(option) },
        label = option,
        enabled = enabled
      )
    }
  }
}

/**
 * Selectable chip group.
 *
 * @param chips List of chip labels
 * @param selectedChips Set of selected chip labels
 * @param onChipSelected Callback when chip selection changes
 * @param modifier Modifier for customization
 * @param enabled Whether the chip group is enabled
 * @param multiSelect Whether multiple chips can be selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmicChipGroup(
  chips: List<String>,
  selectedChips: Set<String>,
  onChipSelected: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  multiSelect: Boolean = true
) {
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
  ) {
    chips.forEach { chip ->
      val isSelected = chip in selectedChips
      FilterChip(
        selected = isSelected,
        onClick = {
          if (multiSelect) {
            onChipSelected(chip, !isSelected)
          } else {
            onChipSelected(chip, true)
          }
        },
        label = { Text(chip) },
        enabled = enabled
      )
    }
  }
}

/**
 * Material3 Dropdown Components
 *
 * Dropdown menus and selection.
 */

/**
 * Dropdown menu for selection.
 *
 * @param options List of options to display
 * @param selectedOption Currently selected option
 * @param onOptionSelected Callback when option is selected
 * @param modifier Modifier for customization
 * @param label Label text
 * @param enabled Whether the dropdown is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CosmicDropdownMenu(
  options: List<String>,
  selectedOption: String,
  onOptionSelected: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  enabled: Boolean = true
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { if (enabled) expanded = it },
    modifier = modifier
  ) {
    CosmicOutlinedTextField(
      value = selectedOption,
      onValueChange = {},
      modifier = Modifier.menuAnchor(),
      label = label,
      readOnly = true,
      enabled = enabled,
      trailingIcon = Icons.Default.ArrowDropDown,
      onTrailingIconClick = { if (enabled) expanded = !expanded }
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false }
    ) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            onOptionSelected(option)
            expanded = false
          }
        )
      }
    }
  }
}

/**
 * Preview Functions
 */

@Preview(name = "Text Fields Light", showBackground = true)
@Composable
private fun PreviewTextFields() {
  CosmicTheme(darkTheme = false) {
    Column(
      modifier = Modifier.padding(Spacing.md),
      verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
      CosmicTextField(
        value = "Standard TextField",
        onValueChange = {},
        label = "Label",
        placeholder = "Placeholder"
      )

      CosmicOutlinedTextField(
        value = "Outlined TextField",
        onValueChange = {},
        label = "Label",
        supportingText = "Helper text"
      )

      CosmicOutlinedTextField(
        value = "Error TextField",
        onValueChange = {},
        label = "Label",
        supportingText = "Error message",
        isError = true
      )

      CosmicPasswordField(
        value = "password123",
        onValueChange = {}
      )

      CosmicSearchField(
        value = "Search query",
        onValueChange = {}
      )
    }
  }
}

@Preview(name = "Text Fields Dark", showBackground = true)
@Composable
private fun PreviewTextFieldsDark() {
  CosmicTheme(darkTheme = true) {
    Surface {
      Column(
        modifier = Modifier.padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
      ) {
        CosmicTextField(
          value = "Standard TextField",
          onValueChange = {},
          label = "Label"
        )

        CosmicOutlinedTextField(
          value = "Outlined TextField",
          onValueChange = {},
          label = "Label"
        )

        CosmicPasswordField(
          value = "password123",
          onValueChange = {}
        )
      }
    }
  }
}

@Preview(name = "Sliders", showBackground = true)
@Composable
private fun PreviewSliders() {
  CosmicTheme {
    Column(
      modifier = Modifier.padding(Spacing.md),
      verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
      CosmicSlider(
        value = 0.5f,
        onValueChange = {},
        label = "Volume",
        valueFormatter = { "${(it * 100).toInt()}%" }
      )

      CosmicSlider(
        value = 3f,
        onValueChange = {},
        label = "Steps",
        valueRange = 0f..10f,
        steps = 9,
        valueFormatter = { it.toInt().toString() }
      )

      CosmicRangeSlider(
        value = 0.2f..0.8f,
        onValueChange = {},
        label = "Range",
        valueFormatter = { "${(it * 100).toInt()}%" }
      )
    }
  }
}

@Preview(name = "Selection Components", showBackground = true)
@Composable
private fun PreviewSelectionComponents() {
  CosmicTheme {
    Column {
      CosmicSwitch(
        checked = true,
        onCheckedChange = {},
        label = "Enable notifications",
        description = "Receive alerts when devices connect"
      )

      Divider()

      CosmicCheckbox(
        checked = true,
        onCheckedChange = {},
        label = "I agree to the terms"
      )

      CosmicCheckbox(
        checked = false,
        onCheckedChange = {},
        label = "Subscribe to newsletter"
      )

      Divider()

      CosmicRadioGroup(
        options = listOf("Option 1", "Option 2", "Option 3"),
        selectedOption = "Option 2",
        onOptionSelected = {}
      )
    }
  }
}

@Preview(name = "Chip Group", showBackground = true)
@Composable
private fun PreviewChipGroup() {
  CosmicTheme {
    Surface {
      Column(modifier = Modifier.padding(Spacing.md)) {
        Text(
          text = "Select plugins",
          style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        CosmicChipGroup(
          chips = listOf("Battery", "Clipboard", "Share", "Ping", "RunCommand", "MPRIS"),
          selectedChips = setOf("Battery", "Clipboard", "Share"),
          onChipSelected = { _, _ -> }
        )
      }
    }
  }
}

@Preview(name = "Dropdown Menu", showBackground = true)
@Composable
private fun PreviewDropdownMenu() {
  CosmicTheme {
    Surface {
      Column(modifier = Modifier.padding(Spacing.md)) {
        CosmicDropdownMenu(
          options = listOf("Device 1", "Device 2", "Device 3"),
          selectedOption = "Device 1",
          onOptionSelected = {},
          label = "Select device"
        )
      }
    }
  }
}
