/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.about

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cosmicext.connect.UserInterface.About.AboutData
import org.cosmicext.connect.UserInterface.About.getApplicationAboutData
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _aboutData = MutableStateFlow<AboutData?>(null)
    val aboutData: StateFlow<AboutData?> = _aboutData.asStateFlow()

    init {
        loadAboutData()
    }

    private fun loadAboutData() {
        _aboutData.value = getApplicationAboutData(context)
    }
}
