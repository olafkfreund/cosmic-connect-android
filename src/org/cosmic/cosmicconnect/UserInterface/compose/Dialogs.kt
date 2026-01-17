/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties

/**
 * Confirmation dialog for yes/no decisions.
 *
 * Standard Material3 AlertDialog styled for COSMIC Connect with
 * customizable icon, title, message, and button labels.
 *
 * @param title Dialog title
 * @param message Dialog message/description
 * @param icon Optional icon resource ID
 * @param confirmLabel Confirm button label (default: "CONFIRM")
 * @param dismissLabel Dismiss button label (default: "CANCEL")
 * @param confirmDestructive Whether confirm action is destructive (uses error color)
 * @param onConfirm Callback when confirm button is clicked
 * @param onDismiss Callback when dismiss button or outside is clicked
 * @param properties Dialog properties (dismissOnBackPress, dismissOnClickOutside, etc.)
 */
@Composable
fun ConfirmationDialog(
  title: String,
  message: String,
  icon: Int? = null,
  confirmLabel: String = "CONFIRM",
  dismissLabel: String = "CANCEL",
  confirmDestructive: Boolean = false,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  properties: DialogProperties = DialogProperties()
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = icon?.let { iconRes ->
      {
        Icon(
          painter = painterResource(iconRes),
          contentDescription = null,
          modifier = Modifier.size(Dimensions.Icon.extraLarge),
          tint = if (confirmDestructive) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.primary
          }
        )
      }
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm()
          onDismiss()
        },
        colors = if (confirmDestructive) {
          androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          )
        } else {
          androidx.compose.material3.ButtonDefaults.buttonColors()
        }
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(dismissLabel)
      }
    },
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.widthIn(
      min = Dimensions.Dialog.minWidth,
      max = Dimensions.Dialog.maxWidth
    ),
    properties = properties
  )
}

/**
 * Input dialog for single-line text input.
 *
 * Dialog with text field for user input, validation, and optional helper text.
 *
 * @param title Dialog title
 * @param message Optional dialog message/description
 * @param label Text field label
 * @param initialValue Initial text field value
 * @param placeholder Text field placeholder
 * @param helperText Optional helper text below field
 * @param errorText Optional error text (shows when validation fails)
 * @param isError Whether the input is in error state
 * @param keyboardType Keyboard type for input
 * @param capitalization Capitalization mode
 * @param confirmLabel Confirm button label (default: "OK")
 * @param dismissLabel Dismiss button label (default: "CANCEL")
 * @param validate Optional validation function
 * @param onConfirm Callback when confirm button is clicked with input value
 * @param onDismiss Callback when dismiss button or outside is clicked
 * @param properties Dialog properties
 */
@Composable
fun InputDialog(
  title: String,
  message: String? = null,
  label: String,
  initialValue: String = "",
  placeholder: String = "",
  helperText: String? = null,
  errorText: String? = null,
  isError: Boolean = false,
  keyboardType: KeyboardType = KeyboardType.Text,
  capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
  confirmLabel: String = "OK",
  dismissLabel: String = "CANCEL",
  validate: ((String) -> Boolean)? = null,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
  properties: DialogProperties = DialogProperties()
) {
  var text by remember { mutableStateOf(initialValue) }
  var showError by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Column {
        message?.let { msg ->
          Text(
            text = msg,
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(modifier = Modifier.height(Spacing.medium))
        }

        OutlinedTextField(
          value = text,
          onValueChange = {
            text = it
            showError = false
          },
          label = { Text(label) },
          placeholder = { Text(placeholder) },
          supportingText = when {
            showError && errorText != null -> {
              { Text(errorText) }
            }
            isError && errorText != null -> {
              { Text(errorText) }
            }
            helperText != null -> {
              { Text(helperText) }
            }
            else -> null
          },
          isError = showError || isError,
          keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = ImeAction.Done
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              if (validate == null || validate(text)) {
                keyboardController?.hide()
                onConfirm(text)
                onDismiss()
              } else {
                showError = true
              }
            }
          ),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (validate == null || validate(text)) {
            keyboardController?.hide()
            onConfirm(text)
            onDismiss()
          } else {
            showError = true
          }
        }
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          keyboardController?.hide()
          onDismiss()
        }
      ) {
        Text(dismissLabel)
      }
    },
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.widthIn(
      min = Dimensions.Dialog.minWidth,
      max = Dimensions.Dialog.maxWidth
    ),
    properties = properties
  )
}

/**
 * Permission dialog for requesting Android permissions.
 *
 * Explains why a permission is needed and provides actions to grant or deny.
 *
 * @param title Dialog title
 * @param permissionName User-friendly permission name (e.g., "Storage", "Camera")
 * @param rationale Explanation of why the permission is needed
 * @param icon Optional icon representing the permission
 * @param confirmLabel Confirm button label (default: "GRANT")
 * @param dismissLabel Dismiss button label (default: "DENY")
 * @param onConfirm Callback when grant button is clicked
 * @param onDismiss Callback when deny button or outside is clicked
 * @param properties Dialog properties
 */
@Composable
fun PermissionDialog(
  title: String,
  permissionName: String,
  rationale: String,
  icon: Int? = null,
  confirmLabel: String = "GRANT",
  dismissLabel: String = "DENY",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  properties: DialogProperties = DialogProperties(
    dismissOnBackPress = true,
    dismissOnClickOutside = true
  )
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = icon?.let { iconRes ->
      {
        Icon(
          painter = painterResource(iconRes),
          contentDescription = null,
          modifier = Modifier.size(Dimensions.Icon.extraLarge),
          tint = MaterialTheme.colorScheme.primary
        )
      }
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
      ) {
        Text(
          text = rationale,
          style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(Spacing.extraSmall))

        Text(
          text = "This app needs $permissionName permission to function properly.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(onClick = {
        onConfirm()
        onDismiss()
      }) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(dismissLabel)
      }
    },
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.widthIn(
      min = Dimensions.Dialog.minWidth,
      max = Dimensions.Dialog.maxWidth
    ),
    properties = properties
  )
}

/**
 * Information dialog for displaying important information.
 *
 * Single-button dialog for presenting information that requires acknowledgment.
 *
 * @param title Dialog title
 * @param message Dialog message/content
 * @param icon Optional icon resource ID
 * @param buttonLabel Button label (default: "OK")
 * @param onDismiss Callback when button or outside is clicked
 * @param properties Dialog properties
 */
@Composable
fun InfoDialog(
  title: String,
  message: String,
  icon: Int? = null,
  buttonLabel: String = "OK",
  onDismiss: () -> Unit,
  properties: DialogProperties = DialogProperties()
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = icon?.let { iconRes ->
      {
        Icon(
          painter = painterResource(iconRes),
          contentDescription = null,
          modifier = Modifier.size(Dimensions.Icon.extraLarge),
          tint = MaterialTheme.colorScheme.primary
        )
      }
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text(buttonLabel)
      }
    },
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.widthIn(
      min = Dimensions.Dialog.minWidth,
      max = Dimensions.Dialog.maxWidth
    ),
    properties = properties
  )
}

/**
 * Error dialog for displaying error messages.
 *
 * Styled with error colors to indicate something went wrong.
 *
 * @param title Dialog title (default: "Error")
 * @param message Error message
 * @param buttonLabel Button label (default: "OK")
 * @param onDismiss Callback when button or outside is clicked
 * @param properties Dialog properties
 */
@Composable
fun ErrorDialog(
  title: String = "Error",
  message: String,
  buttonLabel: String = "OK",
  onDismiss: () -> Unit,
  properties: DialogProperties = DialogProperties()
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        painter = painterResource(CosmicIcons.Status.error),
        contentDescription = null,
        modifier = Modifier.size(Dimensions.Icon.extraLarge),
        tint = MaterialTheme.colorScheme.error
      )
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError
        )
      ) {
        Text(buttonLabel)
      }
    },
    shape = MaterialTheme.shapes.large,
    modifier = Modifier.widthIn(
      min = Dimensions.Dialog.minWidth,
      max = Dimensions.Dialog.maxWidth
    ),
    properties = properties
  )
}

/**
 * Preview composables for development
 */
@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    ConfirmationDialog(
      title = "Delete Device",
      message = "Are you sure you want to remove this device? This action cannot be undone.",
      icon = CosmicIcons.Action.delete,
      confirmLabel = "DELETE",
      dismissLabel = "CANCEL",
      confirmDestructive = true,
      onConfirm = {},
      onDismiss = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun InputDialogPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    InputDialog(
      title = "Rename Device",
      message = "Enter a new name for this device",
      label = "Device Name",
      initialValue = "Pixel 8 Pro",
      placeholder = "Enter device name",
      helperText = "Choose a name that helps you identify this device",
      confirmLabel = "RENAME",
      dismissLabel = "CANCEL",
      validate = { it.isNotBlank() },
      onConfirm = {},
      onDismiss = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun PermissionDialogPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    PermissionDialog(
      title = "Storage Permission Required",
      permissionName = "Storage",
      rationale = "COSMIC Connect needs access to your device storage to share files with other devices.",
      icon = CosmicIcons.Plugin.share,
      onConfirm = {},
      onDismiss = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun InfoDialogPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    InfoDialog(
      title = "Pairing Complete",
      message = "Successfully paired with Pixel 8 Pro. You can now share files and control media playback between devices.",
      icon = CosmicIcons.Pairing.connected,
      onDismiss = {}
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDialogPreview() {
  CosmicTheme(
    context = androidx.compose.ui.platform.LocalContext.current
  ) {
    ErrorDialog(
      message = "Failed to connect to device. Please check your network connection and try again.",
      onDismiss = {}
    )
  }
}
