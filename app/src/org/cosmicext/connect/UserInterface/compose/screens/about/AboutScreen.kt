/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.About.AboutData
import org.cosmicext.connect.UserInterface.About.AboutPerson
import org.cosmicext.connect.UserInterface.compose.CosmicBottomNavigationBar
import org.cosmicext.connect.UserInterface.compose.CosmicIcons
import org.cosmicext.connect.UserInterface.compose.CosmicTopAppBar
import org.cosmicext.connect.UserInterface.compose.Dimensions
import org.cosmicext.connect.UserInterface.compose.NavigationDestination
import org.cosmicext.connect.UserInterface.compose.SectionHeader
import org.cosmicext.connect.UserInterface.compose.SimpleListItem
import org.cosmicext.connect.UserInterface.compose.Spacing
import org.cosmicext.connect.ui.components.status.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToEasterEgg: () -> Unit,
    onNavigateToAboutKde: () -> Unit, // Or open URL if we remove the Activity
    modifier: Modifier = Modifier
) {
    val aboutData by viewModel.aboutData.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            CosmicTopAppBar(
                title = stringResource(R.string.about),
                navigationIcon = CosmicIcons.Navigation.back,
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        if (aboutData == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator()
            }
        } else {
            AboutContent(
                aboutData = aboutData!!,
                onNavigateToLicenses = onNavigateToLicenses,
                onNavigateToEasterEgg = onNavigateToEasterEgg,
                onNavigateToAboutKde = onNavigateToAboutKde,
                onOpenUrl = { url ->
                    if (!url.isNullOrEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun AboutContent(
    aboutData: AboutData,
    onNavigateToLicenses: () -> Unit,
    onNavigateToEasterEgg: () -> Unit,
    onNavigateToAboutKde: () -> Unit,
    onOpenUrl: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tapCount by remember { mutableIntStateOf(0) }
    var firstTapMillis by remember { mutableLongStateOf(0L) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
    ) {
        // App Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large)
                    .clickable {
                        val currentTime = System.currentTimeMillis()
                        if (firstTapMillis == 0L || currentTime - firstTapMillis > 500) {
                            firstTapMillis = currentTime
                            tapCount = 1
                        } else {
                            tapCount++
                        }

                        if (tapCount >= 3) {
                            tapCount = 0
                            firstTapMillis = 0L
                            onNavigateToEasterEgg()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(aboutData.icon),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(Spacing.medium))
                Text(
                    text = aboutData.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.version, aboutData.versionName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Links
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.medium)
            ) {
                Column {
                    SimpleListItem(
                        text = stringResource(R.string.report_bug),
                        icon = CosmicIcons.Status.error, // Bug icon
                        onClick = { onOpenUrl(aboutData.bugURL) }
                    )
                    SimpleListItem(
                        text = stringResource(R.string.donate),
                        icon = CosmicIcons.Action.add, // Or appropriate donate icon
                        onClick = { onOpenUrl(aboutData.donateURL) }
                    )
                    SimpleListItem(
                        text = stringResource(R.string.source_code),
                        icon = CosmicIcons.Action.edit, // Code icon
                        onClick = { onOpenUrl(aboutData.sourceCodeURL) }
                    )
                    SimpleListItem(
                        text = stringResource(R.string.website),
                        icon = CosmicIcons.Action.share, // Web icon
                        onClick = { onOpenUrl(aboutData.websiteURL) }
                    )
                    SimpleListItem(
                        text = stringResource(R.string.licenses),
                        icon = CosmicIcons.Status.info,
                        onClick = onNavigateToLicenses
                    )
                    // TODO: Decide if we keep AboutKDEActivity or replace with web link/Composable
                    // SimpleListItem(
                    //     text = stringResource(R.string.about_kde),
                    //     icon = CosmicIcons.About.kde,
                    //     onClick = onNavigateToAboutKde
                    // )
                }
            }
        }

        // Authors
        item {
            SectionHeader(title = stringResource(R.string.authors))
        }

        items(aboutData.authors) { person ->
            AuthorItem(person = person, onEmailClick = { email ->
                onOpenUrl("mailto:$email")
            })
        }

//        aboutData.authorsFooterText?.let { footerRes ->
//            item {
//                Text(
//                    text = context.getString(footerRes!!),
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    textAlign = TextAlign.Center,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(Spacing.medium)
//                )
//            }
//        }
        
        item {
            Spacer(modifier = Modifier.height(Spacing.large))
        }
    }
}

@Composable
private fun AuthorItem(
    person: AboutPerson,
    onEmailClick: (String) -> Unit
) {
    SimpleListItem(
        text = person.name,
        secondaryText = person.task?.let { stringResource(it) },
        // icon = R.drawable.ic_person, // Optional: Add person icon
        onClick = if (person.emailAddress != null) {
            { onEmailClick(person.emailAddress) }
        } else null
    )
}