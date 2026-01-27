/*
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.os.Bundle
import androidx.compose.runtime.Composable
import org.cosmic.cosmicconnect.UserInterface.compose.DefaultSmsAppDialog

class DefaultSmsAppAlertDialogFragment : AlertDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun DialogContent() {
        DefaultSmsAppDialog(
            onDismiss = { dismiss() },
            onConfirm = { dismiss() }
        )
    }

    class Builder : AlertDialogFragment.AbstractBuilder<Builder, DefaultSmsAppAlertDialogFragment>() {
        override fun getThis(): Builder = this

        fun setPermissions(permissions: Array<String>): Builder {
            return getThis()
        }

        fun setRequestCode(requestCode: Int): Builder {
            return getThis()
        }

        override fun createFragment(): DefaultSmsAppAlertDialogFragment = DefaultSmsAppAlertDialogFragment()
    }
}