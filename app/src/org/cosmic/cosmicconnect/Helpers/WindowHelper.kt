/*
 * SPDX-FileCopyrightText: 2024 Mash Kyrielight <fiepi@live.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Helpers

import android.view.View
import org.cosmic.cosmicconnect.extensions.setupBottomMargin
import org.cosmic.cosmicconnect.extensions.setupBottomPadding

object WindowHelper {

    // for java only
    @JvmStatic
    fun setupBottomPadding(view: View) {
        view.setupBottomPadding()
    }

    // for java only
    @JvmStatic
    fun setupBottomMargin(view: View) {
        view.setupBottomMargin()
    }
}