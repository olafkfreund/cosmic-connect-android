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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import org.cosmicext.connect.R
import java.nio.charset.Charset
import javax.inject.Inject

@HiltViewModel
class LicensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _licenses = MutableStateFlow<List<String>>(emptyList())
    val licenses: StateFlow<List<String>> = _licenses.asStateFlow()

    init {
        loadLicenses()
    }

    private fun loadLicenses() {
        viewModelScope.launch {
            val licensesText = withContext(Dispatchers.IO) {
                try {
                    context.resources.openRawResource(R.raw.license).use { inputStream ->
                        IOUtils.toString(inputStream, Charset.defaultCharset())
                    }
                } catch (e: Exception) {
                    "Error loading licenses: ${e.message}"
                }
            }
            _licenses.value = licensesText.split("\n\n")
        }
    }
}
