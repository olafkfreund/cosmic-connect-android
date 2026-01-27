/*
 * SPDX-FileCopyrightText: 2019 Erik Duisters <e.duisters1@gmail.com>
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cosmic.cosmicconnect.UserInterface.compose.ConfirmationDialog

open class AlertDialogFragment : ComposeDialogFragment() {

    @StringRes protected var titleResId: Int = 0
    protected var title: String? = null
    @StringRes protected var messageResId: Int = 0
    @StringRes protected var positiveButtonResId: Int = 0
    @StringRes protected var negativeButtonResId: Int = 0
    @LayoutRes protected var customViewResId: Int = 0

    protected var mCallback: Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments ?: return

        titleResId = args.getInt(KEY_TITLE_RES_ID)
        title = args.getString(KEY_TITLE)
        messageResId = args.getInt(KEY_MESSAGE_RES_ID)
        positiveButtonResId = args.getInt(KEY_POSITIVE_BUTTON_TEXT_RES_ID)
        negativeButtonResId = args.getInt(KEY_NEGATIVE_BUTTON_TEXT_RES_ID)
        customViewResId = args.getInt(KEY_CUSTOM_VIEW_RES_ID)
    }

    @Composable
    override fun DialogContent() {
        ConfirmationDialog(
            title = if (titleResId > 0) stringResource(titleResId) else title ?: "Alert",
            message = if (messageResId > 0) stringResource(messageResId) else "",
            confirmLabel = if (positiveButtonResId > 0) stringResource(positiveButtonResId) else "OK",
            dismissLabel = if (negativeButtonResId > 0) stringResource(negativeButtonResId) else "CANCEL",
            onConfirm = {
                if (mCallback == null || mCallback!!.onPositiveButtonClicked()) {
                    dismiss()
                }
            },
            onDismiss = {
                mCallback?.onDismiss()
                dismiss()
            }
        )
    }

    open fun setCallback(callback: Callback?) {
        this.mCallback = callback
    }

    abstract class AbstractBuilder<B : AbstractBuilder<B, F>, F : ComposeDialogFragment> internal constructor() {
        val args = Bundle()

        abstract fun getThis(): B

        fun setTitle(@StringRes titleResId: Int): B {
            args.putInt(KEY_TITLE_RES_ID, titleResId)
            return getThis()
        }

        fun setTitle(title: String): B {
            args.putString(KEY_TITLE, title)
            return getThis()
        }

        fun setMessage(@StringRes messageResId: Int): B {
            args.putInt(KEY_MESSAGE_RES_ID, messageResId)
            return getThis()
        }

        fun setPositiveButton(@StringRes positiveButtonResId: Int): B {
            args.putInt(KEY_POSITIVE_BUTTON_TEXT_RES_ID, positiveButtonResId)
            return getThis()
        }

        fun setNegativeButton(@StringRes negativeButtonResId: Int): B {
            args.putInt(KEY_NEGATIVE_BUTTON_TEXT_RES_ID, negativeButtonResId)
            return getThis()
        }

        open fun setView(@LayoutRes customViewResId: Int): B {
            args.putInt(KEY_CUSTOM_VIEW_RES_ID, customViewResId)
            return getThis()
        }

        protected abstract fun createFragment(): F

        fun create(): F {
            val fragment = createFragment()
            fragment.arguments = args
            return fragment
        }
    }

    class Builder : AbstractBuilder<Builder, AlertDialogFragment>() {
        override fun getThis(): Builder = this
        override fun createFragment(): AlertDialogFragment = AlertDialogFragment()
    }

    abstract class Callback {
        open fun onPositiveButtonClicked(): Boolean = true
        open fun onNegativeButtonClicked() {}
        open fun onDismiss() {}
        open fun onCancel() {}
    }

    companion object {
        const val KEY_TITLE_RES_ID = "TitleResId"
        const val KEY_TITLE = "Title"
        const val KEY_MESSAGE_RES_ID = "MessageResId"
        const val KEY_POSITIVE_BUTTON_TEXT_RES_ID = "PositiveButtonResId"
        const val KEY_NEGATIVE_BUTTON_TEXT_RES_ID = "NegativeButtonResId"
        const val KEY_CUSTOM_VIEW_RES_ID = "CustomViewResId"
    }
}