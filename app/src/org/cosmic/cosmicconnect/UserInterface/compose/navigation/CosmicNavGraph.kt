/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.UserInterface.compose.navigation

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.cosmic.cosmicconnect.UserInterface.compose.screens.*
import org.cosmic.cosmicconnect.UserInterface.compose.screens.about.*
import org.cosmic.cosmicconnect.UserInterface.compose.screens.config.*
import org.cosmic.cosmicconnect.UserInterface.compose.screens.plugins.*
import org.cosmic.cosmicconnect.UserInterface.PluginSettingsActivity

object Screen {
    const val Pairing = "pairing"
    const val DeviceDetail = "device/{deviceId}"
    const val Settings = "settings"
    const val About = "about"
    const val Licenses = "licenses"
    const val EasterEgg = "easter_egg"
    const val AboutKde = "about_kde"
    const val CustomDevices = "custom_devices"
    const val TrustedNetworks = "trusted_networks"
    const val FindMyPhone = "plugin/findmyphone/{deviceId}"
    const val RunCommand = "plugin/runcommand/{deviceId}"
    const val Presenter = "plugin/presenter/{deviceId}"
    const val Mpris = "plugin/mpris/{deviceId}"
    const val MousePad = "plugin/mousepad/{deviceId}"
    const val Digitizer = "plugin/digitizer/{deviceId}"
    const val Share = "plugin/share/{deviceId}"
    const val NotificationFilter = "plugin/notifications/filter/{prefKey}"
    const val AudioStream = "plugin/audiostream/{deviceId}"
    const val FileSync = "plugin/filesync/{deviceId}"
    const val VirtualMonitor = "plugin/virtualmonitor/{deviceId}"

    fun deviceDetail(deviceId: String) = "device/$deviceId"
    fun findMyPhone(deviceId: String) = "plugin/findmyphone/$deviceId"
    fun runCommand(deviceId: String) = "plugin/runcommand/$deviceId"
    fun presenter(deviceId: String) = "plugin/presenter/$deviceId"
    fun mpris(deviceId: String) = "plugin/mpris/$deviceId"
    fun mousePad(deviceId: String) = "plugin/mousepad/$deviceId"
    fun digitizer(deviceId: String) = "plugin/digitizer/$deviceId"
    fun share(deviceId: String) = "plugin/share/$deviceId"
    fun notificationFilter(prefKey: String) = "plugin/notifications/filter/$prefKey"
    fun audioStream(deviceId: String) = "plugin/audiostream/$deviceId"
    fun fileSync(deviceId: String) = "plugin/filesync/$deviceId"
    fun virtualMonitor(deviceId: String) = "plugin/virtualmonitor/$deviceId"
}

@Composable
fun CosmicNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Pairing,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Pairing) {
            DeviceListScreen(
                viewModel = hiltViewModel(),
                onDeviceClick = { device ->
                    navController.navigate(Screen.deviceDetail(device.deviceId))
                },
                onNavigateToCustomDevices = { navController.navigate(Screen.CustomDevices) },
                onNavigateToTrustedNetworks = { navController.navigate(Screen.TrustedNetworks) },
                onNavigateToSettings = { navController.navigate(Screen.Settings) }
            )
        }

        composable(
            route = Screen.DeviceDetail,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")
            DeviceDetailScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onPluginSettings = { pluginKey ->
                    // For now, still launch the Activity for individual plugin settings
                    val intent = Intent(context, PluginSettingsActivity::class.java).apply {
                        putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                        putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, pluginKey)
                    }
                    context.startActivity(intent)
                },
                onPluginActivity = { pluginKey ->
                    when (pluginKey.lowercase()) {
                        "findmyphoneplugin" -> navController.navigate(Screen.findMyPhone(deviceId!!))
                        "runcommandplugin" -> navController.navigate(Screen.runCommand(deviceId!!))
                        "presenterplugin" -> navController.navigate(Screen.presenter(deviceId!!))
                        "mprisplugin" -> navController.navigate(Screen.mpris(deviceId!!))
                        "mousepadplugin" -> navController.navigate(Screen.mousePad(deviceId!!))
                        "digitizerplugin" -> navController.navigate(Screen.digitizer(deviceId!!))
                        "shareplugin" -> navController.navigate(Screen.share(deviceId!!))
                        "systemvolumeplugin" -> navController.navigate(Screen.mpris(deviceId!!)) // System Volume is in Mpris screen
                        "clipboardplugin" -> {
                            // Clipboard uses the same share UI for sending
                            navController.navigate(Screen.share(deviceId!!))
                        }
                        "notificationsplugin" -> {
                            // TODO: Pass correct prefKey
                            navController.navigate(Screen.notificationFilter("todo"))
                        }
                        "audiostreamplugin" -> navController.navigate(Screen.audioStream(deviceId!!))
                        "filesyncplugin" -> navController.navigate(Screen.fileSync(deviceId!!))
                        "virtualmonitorplugin" -> navController.navigate(Screen.virtualMonitor(deviceId!!))
                    }
                }
            )
        }

        composable(Screen.Settings) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTrustedNetworks = { navController.navigate(Screen.TrustedNetworks) },
                onNavigateToCustomDevices = { navController.navigate(Screen.CustomDevices) }
            )
        }

        composable(Screen.About) {
            AboutScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLicenses = { navController.navigate(Screen.Licenses) },
                onNavigateToEasterEgg = { navController.navigate(Screen.EasterEgg) },
                onNavigateToAboutKde = { navController.navigate(Screen.AboutKde) }
            )
        }

        composable(Screen.Licenses) {
            LicensesScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.EasterEgg) {
            EasterEggScreen()
        }

        composable(Screen.AboutKde) {
            AboutCosmicScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomDevices) {
            CustomDevicesScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TrustedNetworks) {
            TrustedNetworksScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onRequestPermissions = { /* Handled in Activity usually, but can be done here */ }
            )
        }

        composable(
            route = Screen.FindMyPhone,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            FindMyPhoneScreen(
                viewModel = hiltViewModel(),
                deviceId = backStackEntry.arguments?.getString("deviceId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RunCommand,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            RunCommandScreen(
                viewModel = hiltViewModel(),
                deviceId = backStackEntry.arguments?.getString("deviceId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Presenter,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Presenter needs specific Activity behavior (sensors, media session)
            // For now, pure Compose migration of its UI is done, but we might need 
            // to keep it in Activity if sensors don't work well in NavHost.
            // Actually, we already migrated PresenterActivity to ComponentActivity + setContent.
        }

        composable(
            route = Screen.Mpris,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            MprisScreen(
                viewModel = hiltViewModel(),
                deviceId = backStackEntry.arguments?.getString("deviceId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.NotificationFilter,
            arguments = listOf(navArgument("prefKey") { type = NavType.StringType })
        ) { backStackEntry ->
            NotificationFilterScreen(
                viewModel = hiltViewModel(),
                prefKey = backStackEntry.arguments?.getString("prefKey"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MousePad,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")
            MousePadScreen(
                viewModel = hiltViewModel(),
                deviceId = deviceId,
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = {
                    val intent = Intent(context, PluginSettingsActivity::class.java).apply {
                        putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                        putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, "MousePadPlugin")
                    }
                    context.startActivity(intent)
                }
            )
        }

        composable(
            route = Screen.Digitizer,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")
            DigitizerScreen(
                viewModel = hiltViewModel(),
                deviceId = deviceId,
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = {
                    val intent = Intent(context, PluginSettingsActivity::class.java).apply {
                        putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, deviceId)
                        putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, "DigitizerPlugin")
                    }
                    context.startActivity(intent)
                },
                onToggleFullscreen = { /* TODO: Handle fullscreen toggle */ }
            )
        }

        composable(
            route = Screen.Share,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            ShareScreen(
                viewModel = hiltViewModel(),
                shareIntent = null, // No intent when navigating from device detail
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AudioStream,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            AudioStreamScreen(
                viewModel = hiltViewModel(),
                deviceId = backStackEntry.arguments?.getString("deviceId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FileSync,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            FileSyncScreen(
                viewModel = hiltViewModel(),
                deviceId = backStackEntry.arguments?.getString("deviceId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.VirtualMonitor,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            VirtualMonitorScreen(
                viewModel = hiltViewModel(),
                deviceId = backStackEntry.arguments?.getString("deviceId"),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
