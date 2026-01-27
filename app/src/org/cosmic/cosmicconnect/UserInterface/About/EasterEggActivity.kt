/*
 * SPDX-FileCopyrightText: 2021 Maxim Leshchenko <cnmaks90@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.About

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.cosmic.cosmicconnect.UserInterface.compose.CosmicTheme
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.EasterEggScreen

class EasterEggActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lock to portrait to avoid sensor coordinate confusion
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        setContent {
            CosmicTheme(context = this) {
                EasterEggScreen()
            }
        }
    }
}