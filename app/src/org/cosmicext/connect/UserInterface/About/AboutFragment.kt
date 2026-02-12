/*
 * SPDX-FileCopyrightText: 2021 Maxim Leshchenko <cnmaks90@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.About

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.compose.CosmicTheme
import org.cosmicext.connect.UserInterface.compose.screens.about.AboutScreen
import org.cosmicext.connect.UserInterface.compose.screens.about.AboutViewModel

@AndroidEntryPoint
class AboutFragment : Fragment() {

    private val viewModel: AboutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CosmicTheme(context = context) {
                    AboutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onNavigateToLicenses = {
                            startActivity(Intent(context, LicensesActivity::class.java))
                        },
                        onNavigateToEasterEgg = {
                            startActivity(Intent(context, EasterEggActivity::class.java))
                        },
                        onNavigateToAboutKde = {
                            startActivity(Intent(context, AboutKDEActivity::class.java))
                        }
                    )
                }
            }
        }
    }
}
