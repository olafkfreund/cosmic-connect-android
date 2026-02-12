/*
 * SPDX-FileCopyrightText: 2026 COSMIC Connect Contributors
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.UserInterface.compose

import androidx.annotation.DrawableRes
import org.cosmicext.connect.R

/**
 * COSMIC Connect icon system for Jetpack Compose.
 *
 * Centralized icon references for all drawable resources in the application.
 * All icons are vector drawables (XML) except where noted as PNG/bitmap.
 *
 * Usage with Compose:
 * ```kotlin
 * Icon(
 *   painter = painterResource(CosmicIcons.Device.phone),
 *   contentDescription = "Phone device",
 *   modifier = Modifier.size(Dimensions.Icon.standard)
 * )
 * ```
 */
object CosmicIcons {
  /**
   * Device type icons (32dp standard size)
   */
  object Device {
    @DrawableRes val desktop: Int = R.drawable.ic_device_desktop_32dp
    @DrawableRes val desktopShortcut: Int = R.drawable.ic_device_desktop_shortcut
    @DrawableRes val laptop: Int = R.drawable.ic_device_laptop_32dp
    @DrawableRes val laptopShortcut: Int = R.drawable.ic_device_laptop_shortcut
    @DrawableRes val phone: Int = R.drawable.ic_device_phone_32dp
    @DrawableRes val phoneShortcut: Int = R.drawable.ic_device_phone_shortcut
    @DrawableRes val tablet: Int = R.drawable.ic_device_tablet_32dp
    @DrawableRes val tabletShortcut: Int = R.drawable.ic_device_tablet_shortcut
    @DrawableRes val tv: Int = R.drawable.ic_device_tv_32dp
    @DrawableRes val tvShortcut: Int = R.drawable.ic_device_tv_shortcut
  }

  /**
   * Pairing and connection icons
   */
  object Pairing {
    @DrawableRes val accept: Int = R.drawable.ic_accept_pairing_24dp
    @DrawableRes val reject: Int = R.drawable.ic_reject_pairing_24dp
    @DrawableRes val connected: Int = R.drawable.ic_phonelink_36dp
    @DrawableRes val disconnected: Int = R.drawable.ic_phonelink_off_36dp
    @DrawableRes val wifi: Int = R.drawable.ic_wifi
    @DrawableRes val key: Int = R.drawable.ic_key
  }

  /**
   * Media control icons (24dp standard size)
   */
  object Media {
    // Playback controls - Black
    @DrawableRes val playBlack: Int = R.drawable.ic_play_black
    @DrawableRes val pauseBlack: Int = R.drawable.ic_pause_black
    @DrawableRes val nextBlack: Int = R.drawable.ic_next_black
    @DrawableRes val previousBlack: Int = R.drawable.ic_previous_black
    @DrawableRes val fastForwardBlack: Int = R.drawable.ic_fast_forward_black
    @DrawableRes val rewindBlack: Int = R.drawable.ic_rewind_black
    @DrawableRes val stop: Int = R.drawable.ic_stop

    // Playback controls - White
    @DrawableRes val playWhite: Int = R.drawable.ic_play_white
    @DrawableRes val pauseWhite: Int = R.drawable.ic_pause_white
    @DrawableRes val nextWhite: Int = R.drawable.ic_next_white
    @DrawableRes val previousWhite: Int = R.drawable.ic_previous_white

    // Volume controls
    @DrawableRes val volume: Int = R.drawable.ic_volume
    @DrawableRes val volumeMute: Int = R.drawable.ic_volume_mute

    // Playback modes
    @DrawableRes val loopNone: Int = R.drawable.ic_loop_none_black
    @DrawableRes val loopTrack: Int = R.drawable.ic_loop_track_black
    @DrawableRes val loopPlaylist: Int = R.drawable.ic_loop_playlist_black
    @DrawableRes val shuffleOff: Int = R.drawable.ic_shuffle_off_black
    @DrawableRes val shuffleOn: Int = R.drawable.ic_shuffle_on_black

    // Other media
    @DrawableRes val microphone: Int = R.drawable.ic_mic_black
    @DrawableRes val albumArtPlaceholder: Int = R.drawable.ic_album_art_placeholder
  }

  /**
   * Navigation icons (24dp standard size)
   */
  object Navigation {
    @DrawableRes val back: Int = R.drawable.ic_arrow_back_black_24dp
    @DrawableRes val forward: Int = R.drawable.ic_arrow_forward_black_24dp
    @DrawableRes val up: Int = R.drawable.ic_arrow_upward_black_24dp
    @DrawableRes val down: Int = R.drawable.ic_arrow_downward_black_24dp
    @DrawableRes val home: Int = R.drawable.ic_home_black_24dp

    // Outline arrows
    @DrawableRes val arrowLeft: Int = R.drawable.outline_arrow_left_24
    @DrawableRes val arrowRight: Int = R.drawable.outline_arrow_right_24
    @DrawableRes val arrowDropDown: Int = R.drawable.outline_arrow_drop_down_24
    @DrawableRes val arrowDropUp: Int = R.drawable.outline_arrow_drop_up_24

    // Simple arrow
    @DrawableRes val arrowSimple: Int = R.drawable.ic_arrow_black
    @DrawableRes val arrowDropDownSimple: Int = R.drawable.ic_arrow_drop_down_24px

    // Menu (placeholder - using settings icon temporarily)
    @DrawableRes val menu: Int = R.drawable.ic_settings_24dp
  }

  /**
   * Action icons (24dp standard size)
   */
  object Action {
    @DrawableRes val add: Int = R.drawable.ic_add
    @DrawableRes val addCircle: Int = R.drawable.ic_action_content_add_circle_outline_32dp
    @DrawableRes val delete: Int = R.drawable.ic_delete
    @DrawableRes val edit: Int = R.drawable.ic_action_image_edit_24dp
    @DrawableRes val editNote: Int = R.drawable.ic_edit_note_24dp
    @DrawableRes val refresh: Int = R.drawable.ic_action_refresh_24dp
    @DrawableRes val send: Int = R.drawable.ic_baseline_send_24
    @DrawableRes val share: Int = R.drawable.ic_share_white
    @DrawableRes val keyboard: Int = R.drawable.ic_action_keyboard_24dp
    @DrawableRes val keyboardHide: Int = R.drawable.ic_keyboard_hide_36dp
    @DrawableRes val keyboardReturn: Int = R.drawable.ic_keyboard_return_black_24dp
    @DrawableRes val paste: Int = R.drawable.ic_baseline_content_paste_24
    @DrawableRes val search: Int = R.drawable.ic_search_24
    @DrawableRes val openInFull: Int = R.drawable.ic_open_in_full_24dp

    // More options (placeholder - using edit icon temporarily)
    @DrawableRes val more: Int = R.drawable.ic_action_image_edit_24dp
  }

  /**
   * Settings and configuration icons
   */
  object Settings {
    @DrawableRes val settings: Int = R.drawable.ic_settings_24dp
    @DrawableRes val settingsWhite: Int = R.drawable.ic_settings_white_32dp
  }

  /**
   * Plugin-specific icons (24dp standard size)
   */
  object Plugin {
    @DrawableRes val mpris: Int = R.drawable.mpris_plugin_action_24dp
    @DrawableRes val share: Int = R.drawable.share_plugin_action_24dp
    @DrawableRes val runCommand: Int = R.drawable.run_command_plugin_icon_24dp
    @DrawableRes val touchpad: Int = R.drawable.touchpad_plugin_action_24dp
    @DrawableRes val presenter: Int = R.drawable.ic_presenter_24dp
    @DrawableRes val draw: Int = R.drawable.ic_draw_24dp
    @DrawableRes val finger: Int = R.drawable.ic_finger_24dp
    @DrawableRes val tvRemote: Int = R.drawable.tv_remote_24px
  }

  /**
   * Input device icons
   */
  object Input {
    @DrawableRes val mousePointer: Int = R.drawable.mouse_pointer
    @DrawableRes val mousePointerClicked: Int = R.drawable.mouse_pointer_clicked
    @DrawableRes val leftClick: Int = R.drawable.left_click_48dp
    @DrawableRes val rightClick: Int = R.drawable.right_click_48dp
  }

  /**
   * Status and indicator icons
   */
  object Status {
    @DrawableRes val error: Int = R.drawable.ic_error_outline_48dp
    @DrawableRes val warning: Int = R.drawable.ic_warning
    @DrawableRes val info: Int = R.drawable.ic_baseline_info_24
  }

  /**
   * Communication icons
   */
  object Communication {
    @DrawableRes val sms: Int = R.drawable.ic_baseline_sms_24
    @DrawableRes val web: Int = R.drawable.ic_baseline_web_24
    @DrawableRes val notification: Int = R.drawable.ic_notification
  }

  /**
   * About and information icons
   */
  object About {
    @DrawableRes val bugReport: Int = R.drawable.ic_baseline_bug_report_24
    @DrawableRes val code: Int = R.drawable.ic_baseline_code_24
    @DrawableRes val gavel: Int = R.drawable.ic_baseline_gavel_24
    @DrawableRes val attachMoney: Int = R.drawable.ic_baseline_attach_money_24
  }

  /**
   * Branding icons
   */
  object Brand {
    @DrawableRes val cosmic24: Int = R.drawable.icon
    @DrawableRes val cosmic48: Int = R.drawable.icon
    @DrawableRes val appIcon: Int = R.drawable.icon
  }

  /**
   * Background and decorative elements
   */
  object Background {
    @DrawableRes val buttonRound: Int = R.drawable.button_round
    @DrawableRes val listDivider: Int = R.drawable.list_divider
    @DrawableRes val sinkItemBackground: Int = R.drawable.sink_item_background
    @DrawableRes val drawerShadow: Int = R.drawable.drawer_shadow  // 9-patch PNG
  }

  /**
   * Notification and launcher icons
   */
  object Launcher {
    @DrawableRes val notificationIcon: Int = R.drawable.ic_notification  // PNG
    @DrawableRes val appIcon: Int = R.drawable.icon  // PNG
    @DrawableRes val launcherBackground: Int = R.drawable.ic_launcher_background
    @DrawableRes val launcherForeground: Int = R.drawable.ic_launcher_foreground
    @DrawableRes val launcherMonochrome: Int = R.drawable.ic_launcher_monochrome
    @DrawableRes val launcherBannerBackground: Int = R.drawable.ic_launcher_banner_background
    @DrawableRes val launcherBannerForeground: Int = R.drawable.ic_launcher_banner_foreground
  }

  /**
   * Widget preview images
   */
  object Widget {
    @DrawableRes val remoteCommandPreview: Int = R.drawable.remotecommand_widget_preview  // PNG
  }
}

/**
 * Helper functions for icon usage in Compose
 */

/**
 * Get device icon based on device type string
 *
 * @param deviceType Device type identifier
 * @param shortcut Whether to use shortcut variant
 * @return Drawable resource ID for the device icon
 */
@DrawableRes
fun getDeviceIcon(deviceType: String, shortcut: Boolean = false): Int {
  return when (deviceType.lowercase()) {
    "desktop" -> if (shortcut) CosmicIcons.Device.desktopShortcut else CosmicIcons.Device.desktop
    "laptop" -> if (shortcut) CosmicIcons.Device.laptopShortcut else CosmicIcons.Device.laptop
    "phone", "smartphone" -> if (shortcut) CosmicIcons.Device.phoneShortcut else CosmicIcons.Device.phone
    "tablet" -> if (shortcut) CosmicIcons.Device.tabletShortcut else CosmicIcons.Device.tablet
    "tv" -> if (shortcut) CosmicIcons.Device.tvShortcut else CosmicIcons.Device.tv
    else -> CosmicIcons.Device.phone  // Default to phone
  }
}

/**
 * Get media control icon with theme awareness
 *
 * @param action Media action (play, pause, next, previous)
 * @param useWhite Whether to use white variant
 * @return Drawable resource ID for the media control icon
 */
@DrawableRes
fun getMediaControlIcon(action: String, useWhite: Boolean = false): Int {
  return when (action.lowercase()) {
    "play" -> if (useWhite) CosmicIcons.Media.playWhite else CosmicIcons.Media.playBlack
    "pause" -> if (useWhite) CosmicIcons.Media.pauseWhite else CosmicIcons.Media.pauseBlack
    "next" -> if (useWhite) CosmicIcons.Media.nextWhite else CosmicIcons.Media.nextBlack
    "previous" -> if (useWhite) CosmicIcons.Media.previousWhite else CosmicIcons.Media.previousBlack
    else -> CosmicIcons.Media.playBlack  // Default to play
  }
}

/**
 * Get plugin icon based on plugin key
 *
 * @param pluginKey Plugin unique identifier
 * @return Drawable resource ID for the plugin icon
 */
@DrawableRes
fun getPluginIcon(pluginKey: String): Int {
  return when (pluginKey.lowercase()) {
    "battery", "batteryplugin" -> CosmicIcons.Pairing.connected // Needs dedicated icon
    "clipboard", "clipboardplugin" -> CosmicIcons.Action.paste
    "mpris", "mprisplugin" -> CosmicIcons.Plugin.mpris
    "notification", "notificationsplugin" -> CosmicIcons.Communication.notification
    "mousepad", "mousepadplugin" -> CosmicIcons.Plugin.touchpad
    "runcommand", "runcommandplugin" -> CosmicIcons.Plugin.runCommand
    "share", "shareplugin" -> CosmicIcons.Plugin.share
    "telephony", "telephonyplugin" -> CosmicIcons.Communication.sms
    "findmyphone", "findmyphoneplugin" -> CosmicIcons.Navigation.home // Needs dedicated
    "sftp", "sftpplugin" -> CosmicIcons.Action.openInFull
    "presenter", "presenterplugin" -> CosmicIcons.Plugin.presenter
    "connectivity", "connectivityreportplugin" -> CosmicIcons.Pairing.wifi
    "systemvolume", "systemvolumeplugin" -> CosmicIcons.Media.volume
    "contacts", "contactsplugin" -> CosmicIcons.About.code // Needs dedicated
    "digitizer", "digitizerplugin" -> CosmicIcons.Plugin.draw
    else -> CosmicIcons.Status.info
  }
}

/**
 * Usage examples:
 *
 * ```kotlin
 * // Device icon in list item
 * Icon(
 *   painter = painterResource(CosmicIcons.Device.phone),
 *   contentDescription = "Phone device",
 *   modifier = Modifier.size(Dimensions.Icon.device),
 *   tint = MaterialTheme.colorScheme.primary
 * )
 *
 * // Dynamic device icon
 * Icon(
 *   painter = painterResource(getDeviceIcon(device.type)),
 *   contentDescription = device.name,
 *   modifier = Modifier.size(Dimensions.Icon.device)
 * )
 *
 * // Media control button
 * IconButton(
 *   onClick = { playPause() },
 *   modifier = Modifier.size(Dimensions.Button.fabSize)
 * ) {
 *   Icon(
 *     painter = painterResource(
 *       if (isPlaying) CosmicIcons.Media.pauseBlack
 *       else CosmicIcons.Media.playBlack
 *     ),
 *     contentDescription = if (isPlaying) "Pause" else "Play",
 *     modifier = Modifier.size(Dimensions.Icon.standard)
 *   )
 * }
 *
 * // Navigation icon in top app bar
 * IconButton(onClick = { navController.popBackStack() }) {
 *   Icon(
 *     painter = painterResource(CosmicIcons.Navigation.back),
 *     contentDescription = "Navigate back"
 *   )
 * }
 *
 * // Plugin icon in card
 * Icon(
 *   painter = painterResource(CosmicIcons.Plugin.mpris),
 *   contentDescription = "MPRIS Plugin",
 *   modifier = Modifier.size(Dimensions.Icon.large),
 *   tint = MaterialTheme.colorScheme.primary
 * )
 * ```
 */
