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
import org.cosmicext.connect.UserInterface.compose.StartActivityDialog

class StartActivityAlertDialogFragment : AlertDialogFragment() {
    private var intentAction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = requireArguments()
        intentAction = args.getString(KEY_INTENT_ACTION)
        titleResId = args.getInt(KEY_TITLE_RES_ID)
        messageResId = args.getInt(KEY_MESSAGE_RES_ID)
    }

    @Composable
    override fun DialogContent() {
        if (intentAction != null) {
            StartActivityDialog(
                title = if (titleResId != 0) stringResource(titleResId) else "Open App",
                message = if (messageResId != 0) stringResource(messageResId) else "Open external activity?",
                intentAction = intentAction!!,
                onDismiss = { dismiss() }
            )
        } else {
            dismiss()
        }
    }

    class Builder : AlertDialogFragment.AbstractBuilder<Builder, StartActivityAlertDialogFragment>() {
        override fun getThis(): Builder = this

        fun setIntentAction(intentAction: String): Builder {
            args.putString(KEY_INTENT_ACTION, intentAction)
            return getThis()
        }

        fun setIntentUrl(intentUrl: String): Builder {
            args.putString(KEY_INTENT_URL, intentUrl)
            return getThis()
        }

        fun setRequestCode(requestCode: Int): Builder {
            args.putInt(KEY_REQUEST_CODE, requestCode)
            return getThis()
        }

        fun setStartForResult(startForResult: Boolean): Builder {
            args.putBoolean(KEY_START_FOR_RESULT, startForResult)
            return getThis()
        }

        override fun createFragment(): StartActivityAlertDialogFragment = StartActivityAlertDialogFragment()
    }

    companion object {
        private const val KEY_INTENT_ACTION = "IntentAction"
        private const val KEY_INTENT_URL = "IntentUrl"
        private const val KEY_REQUEST_CODE = "RequestCode"
        private const val KEY_START_FOR_RESULT = "StartForResult"
        private const val KEY_TITLE_RES_ID = "TitleResId"
        private const val KEY_MESSAGE_RES_ID = "MessageResId"
    }
}