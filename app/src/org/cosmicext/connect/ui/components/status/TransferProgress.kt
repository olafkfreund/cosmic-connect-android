/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.ui.components.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.Spacing

/**
 * Transfer progress indicator for file transfers.
 *
 * Shows progress bar with file name, size, and percentage.
 *
 * @param fileName Name of file being transferred
 * @param progress Transfer progress (0.0 to 1.0)
 * @param totalBytes Total file size in bytes
 * @param transferredBytes Bytes transferred so far
 * @param modifier Modifier for the indicator
 */
@Composable
fun TransferProgressIndicator(
    fileName: String,
    progress: Float,
    totalBytes: Long? = null,
    transferredBytes: Long? = null,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentage = (clampedProgress * 100).toInt()

    val sizeText = if (totalBytes != null && transferredBytes != null) {
        "${formatBytes(transferredBytes)} / ${formatBytes(totalBytes)}"
    } else {
        "$percentage%"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Transferring $fileName, $percentage percent complete"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(Spacing.small))

            Text(
                text = sizeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.size(Spacing.extraSmall))

        LinearProgressIndicator(
            progress = { clampedProgress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/**
 * Format bytes to human-readable string.
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Preview(showBackground = true)
@Composable
private fun TransferProgressIndicatorPreview() {
    CosmicTheme(
        context = androidx.compose.ui.platform.LocalContext.current
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.large)
            ) {
                TransferProgressIndicator(
                    fileName = "vacation_photo.jpg",
                    progress = 0.35f,
                    totalBytes = 5242880,
                    transferredBytes = 1835008
                )

                TransferProgressIndicator(
                    fileName = "document.pdf",
                    progress = 0.75f,
                    totalBytes = 1048576,
                    transferredBytes = 786432
                )

                TransferProgressIndicator(
                    fileName = "very_long_file_name_that_should_be_truncated.mp4",
                    progress = 0.10f,
                    totalBytes = 104857600,
                    transferredBytes = 10485760
                )

                // Without size info
                TransferProgressIndicator(
                    fileName = "music.mp3",
                    progress = 0.50f
                )
            }
        }
    }
}
