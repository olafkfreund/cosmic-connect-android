/*
 * SPDX-FileCopyrightText: 2021 Art Pinch <leonardo906@mail.ru>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmicext.connect.Plugins.SystemVolumePlugin

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemGapDecoration(private val gap: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)
        val adapter = parent.adapter ?: return

        if (itemPosition in 0 until adapter.itemCount - 1) {
            outRect.bottom = gap
        }
    }
}
