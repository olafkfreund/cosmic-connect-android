/*
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.PermissionDialog

class PermissionsAlertDialogFragment : AlertDialogFragment() {

    private var permissions: Array<String>? = null
    private var requestCode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        permissions = args.getStringArray(KEY_PERMISSIONS)
        requestCode = args.getInt(KEY_REQUEST_CODE, 0)
        titleResId = args.getInt(KEY_TITLE_RES_ID)
        messageResId = args.getInt(KEY_MESSAGE_RES_ID)
    }

    @Composable
    override fun DialogContent() {
        PermissionDialog(
            title = if (titleResId != 0) stringResource(titleResId) else "Permission Required",
            permissionName = "System", // Generic, could be improved
            rationale = if (messageResId != 0) stringResource(messageResId) else "This permission is needed for the app to function.",
            onConfirm = {
                permissions?.let {
                    ActivityCompat.requestPermissions(requireActivity(), it, requestCode)
                }
                dismiss()
            },
            onDismiss = { dismiss() }
        )
    }

    class Builder : AlertDialogFragment.AbstractBuilder<Builder, PermissionsAlertDialogFragment>() {
        override fun getThis(): Builder = this

        fun setPermissions(permissions: Array<String>): Builder {
            args.putStringArray(KEY_PERMISSIONS, permissions)
            return getThis()
        }

        fun setRequestCode(requestCode: Int): Builder {
            args.putInt(KEY_REQUEST_CODE, requestCode)
            return getThis()
        }

        override fun createFragment(): PermissionsAlertDialogFragment = PermissionsAlertDialogFragment()
    }

    companion object {
        private const val KEY_PERMISSIONS = "Permissions"
        private const val KEY_REQUEST_CODE = "RequestCode"
        private const val KEY_TITLE_RES_ID = "TitleResId"
        private const val KEY_MESSAGE_RES_ID = "MessageResId"
    }
}