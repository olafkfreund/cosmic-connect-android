/*
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cosmicext.connect.UserInterface.compose.InputDialog

class EditTextAlertDialogFragment : AlertDialogFragment() {

    private var hintResId: Int = 0
    private var text: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            hintResId = it.getInt(KEY_HINT_RES_ID)
            text = it.getString(KEY_TEXT, "")
        }
    }

    @Composable
    override fun DialogContent() {
        InputDialog(
            title = if (titleResId != 0) stringResource(titleResId) else "Input",
            label = if (hintResId != 0) stringResource(hintResId) else "Text",
            initialValue = text,
            onConfirm = { result ->
                this.text = result
                if (mCallback?.onPositiveButtonClicked() != false) {
                    dismiss()
                }
            },
            onDismiss = {
                mCallback?.onDismiss()
                dismiss()
            }
        )
    }

    override fun setCallback(callback: Callback?) {
        this.mCallback = callback
    }

    class Builder : AlertDialogFragment.AbstractBuilder<Builder, EditTextAlertDialogFragment>() {
        override fun getThis(): Builder = this

        fun setHint(@StringRes hintResId: Int): Builder {
            args.putInt(KEY_HINT_RES_ID, hintResId)
            return getThis()
        }

        fun setText(text: String): Builder {
            args.putString(KEY_TEXT, text)
            return getThis()
        }

        override fun createFragment(): EditTextAlertDialogFragment = EditTextAlertDialogFragment()
    }

    companion object {
        private const val KEY_HINT_RES_ID = "HintResId"
        private const val KEY_TEXT = "Text"
    }
}
