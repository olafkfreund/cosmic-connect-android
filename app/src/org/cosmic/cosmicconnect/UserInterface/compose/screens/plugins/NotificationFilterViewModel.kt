/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
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
import org.cosmic.cosmicconnect.Plugins.NotificationsPlugin.AppDatabase
import javax.inject.Inject

data class AppListInfo(
    val pkg: String,
    val name: String,
    val icon: Drawable?,
    val isEnabled: Boolean,
    val blockContents: Boolean = false,
    val blockImages: Boolean = false
)

data class NotificationFilterUiState(
    val apps: List<AppListInfo> = emptyList(),
    val searchQuery: String = "",
    val allEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val screenOffNotification: Boolean = false
)

@HiltViewModel
class NotificationFilterViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val appDatabase = AppDatabase.getInstance(context)
    private var allApps: List<AppListInfo> = emptyList()
    private var prefKey: String? = null

    private val _uiState = MutableStateFlow(NotificationFilterUiState())
    val uiState: StateFlow<NotificationFilterUiState> = _uiState.asStateFlow()

    fun loadApps(prefKey: String?) {
        this.prefKey = prefKey
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val prefs = context.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
            val screenOff = prefs.getBoolean("screen_off_notification_state", false)

            val loadedApps = withContext(Dispatchers.IO) {
                val packageManager = context.packageManager
                val installedApps = packageManager.getInstalledApplications(0)
                val allPackageNames = mutableSetOf<String>()
                val result = mutableListOf<AppListInfo>()

                for (appInfo in installedApps) {
                    val pkg = appInfo.packageName
                    result.add(AppListInfo(
                        pkg = pkg,
                        name = appInfo.loadLabel(packageManager).toString(),
                        icon = appInfo.loadIcon(packageManager),
                        isEnabled = appDatabase.isEnabled(pkg),
                        blockContents = appDatabase.getPrivacy(pkg, AppDatabase.PrivacyOptions.BLOCK_CONTENTS),
                        blockImages = appDatabase.getPrivacy(pkg, AppDatabase.PrivacyOptions.BLOCK_IMAGES)
                    ))
                    allPackageNames.add(pkg)
                }

                // Add work profile apps
                try {
                    val currentUser = Process.myUserHandle()
                    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    val um = context.getSystemService(Context.USER_SERVICE) as UserManager
                    val userProfiles = um.userProfiles
                    for (userProfile in userProfiles) {
                        if (userProfile == currentUser) continue
                        val userActivityList = launcher.getActivityList(null, userProfile)
                        for (app in userActivityList) {
                            val pkg = app.applicationInfo.packageName
                            if (allPackageNames.contains(pkg)) continue
                            val appInfo = app.applicationInfo
                            result.add(AppListInfo(
                                pkg = pkg,
                                name = appInfo.loadLabel(packageManager).toString(),
                                icon = appInfo.loadIcon(packageManager),
                                isEnabled = appDatabase.isEnabled(pkg),
                                blockContents = appDatabase.getPrivacy(pkg, AppDatabase.PrivacyOptions.BLOCK_CONTENTS),
                                blockImages = appDatabase.getPrivacy(pkg, AppDatabase.PrivacyOptions.BLOCK_IMAGES)
                            ))
                            allPackageNames.add(pkg)
                        }
                    }
                } catch (e: Exception) {
                    // Log error
                }

                result.sortBy { it.name.lowercase() }
                result
            }

            allApps = loadedApps
            _uiState.value = NotificationFilterUiState(
                apps = filterApps(loadedApps, _uiState.value.searchQuery),
                allEnabled = appDatabase.allEnabled,
                isLoading = false,
                screenOffNotification = screenOff
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            apps = filterApps(allApps, query)
        )
    }

    private fun filterApps(apps: List<AppListInfo>, query: String): List<AppListInfo> {
        if (query.isEmpty()) return apps
        val lowerQuery = query.lowercase().trim()
        return apps.filter { it.name.lowercase().contains(lowerQuery) }
    }

    fun toggleApp(pkg: String, enabled: Boolean) {
        appDatabase.setEnabled(pkg, enabled)
        updateAppInState(pkg) { it.copy(isEnabled = enabled) }
    }

    fun toggleAll(enabled: Boolean) {
        appDatabase.allEnabled = enabled
        allApps = allApps.map { it.copy(isEnabled = enabled) }
        _uiState.value = _uiState.value.copy(
            allEnabled = enabled,
            apps = filterApps(allApps, _uiState.value.searchQuery)
        )
    }

    fun updatePrivacy(pkg: String, option: AppDatabase.PrivacyOptions, enabled: Boolean) {
        appDatabase.setPrivacy(pkg, option, enabled)
        updateAppInState(pkg) { 
            when (option) {
                AppDatabase.PrivacyOptions.BLOCK_CONTENTS -> it.copy(blockContents = enabled)
                AppDatabase.PrivacyOptions.BLOCK_IMAGES -> it.copy(blockImages = enabled)
                else -> it
            }
        }
    }

    private fun updateAppInState(pkg: String, transform: (AppListInfo) -> AppListInfo) {
        allApps = allApps.map { if (it.pkg == pkg) transform(it) else it }
        _uiState.value = _uiState.value.copy(
            apps = filterApps(allApps, _uiState.value.searchQuery)
        )
    }

    fun setScreenOffNotification(enabled: Boolean) {
        val prefs = context.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("screen_off_notification_state", enabled).apply()
        _uiState.value = _uiState.value.copy(screenOffNotification = enabled)
    }
}
