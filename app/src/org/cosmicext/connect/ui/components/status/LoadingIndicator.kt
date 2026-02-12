/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.ui.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.Dimensions
import org.cosmicext.connect.UserInterface.compose.Spacing

/**
 * Loading indicator variants.
 */
enum class LoadingStyle {
    Circular,
    Linear,
    CircularWithLabel
}

/**
 * Loading indicator with multiple style variants.
 *
 * Shows loading state with progress or indeterminate animation.
 *
 * @param style Loading indicator style
 * @param progress Optional progress (0.0 to 1.0, null for indeterminate)
 * @param label Optional label text (for CircularWithLabel style)
 * @param color Optional custom color
 * @param modifier Modifier for the indicator
 */
@Composable
fun LoadingIndicator(
    style: LoadingStyle = LoadingStyle.Circular,
    progress: Float? = null,
    label: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val contentDesc = buildString {
        append("Loading")
        progress?.let {
            val percentage = (it.coerceIn(0f, 1f) * 100).toInt()
            append(", $percentage percent")
        }
        label?.let { append(", $it") }
    }

    when (style) {
        LoadingStyle.Circular -> {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = modifier
                        .size(Dimensions.Icon.large)
                        .semantics { contentDescription = contentDesc },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                CircularProgressIndicator(
                    modifier = modifier
                        .size(Dimensions.Icon.large)
                        .semantics { contentDescription = contentDesc },
                    color = color
                )
            }
        }

        LoadingStyle.Linear -> {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = contentDesc },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    modifier = modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = contentDesc },
                    color = color
                )
            }
        }

        LoadingStyle.CircularWithLabel -> {
            Column(
                modifier = modifier.semantics { contentDescription = contentDesc },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                if (progress != null) {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(Dimensions.Icon.large),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimensions.Icon.large),
                        color = color
                    )
                }

                label?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingIndicatorPreview() {
    CosmicTheme(
        context = androidx.compose.ui.platform.LocalContext.current
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                // Circular indeterminate
                Box(modifier = Modifier.size(48.dp)) {
                    LoadingIndicator(
                        style = LoadingStyle.Circular
                    )
                }

                // Circular with progress
                Box(modifier = Modifier.size(48.dp)) {
                    LoadingIndicator(
                        style = LoadingStyle.Circular,
                        progress = 0.65f
                    )
                }

                // Linear indeterminate
                LoadingIndicator(
                    style = LoadingStyle.Linear
                )

                // Linear with progress
                LoadingIndicator(
                    style = LoadingStyle.Linear,
                    progress = 0.40f
                )

                // Circular with label (indeterminate)
                LoadingIndicator(
                    style = LoadingStyle.CircularWithLabel,
                    label = "Loading devices..."
                )

                // Circular with label and progress
                LoadingIndicator(
                    style = LoadingStyle.CircularWithLabel,
                    progress = 0.80f,
                    label = "Syncing..."
                )
            }
        }
    }
}
