/*
 * SPDX-FileCopyrightText: 2021 SohnyBohny <sohny.bean@streber24.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmic.cosmicconnect.Plugins.MouseReceiverPlugin

import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import org.apache.commons.lang3.ArrayUtils
import android.content.Context
import org.cosmic.cosmicconnect.Core.*
import org.cosmic.cosmicconnect.Device
import org.cosmic.cosmicconnect.Plugins.Plugin
import org.cosmic.cosmicconnect.Plugins.RemoteKeyboardPlugin.RemoteKeyboardPlugin
import org.cosmic.cosmicconnect.UserInterface.MainActivity
import org.cosmic.cosmicconnect.UserInterface.StartActivityAlertDialogFragment
import org.cosmic.cosmicconnect.R

@RequiresApi(Build.VERSION_CODES.N)
class MouseReceiverPlugin(context: Context, device: Device) : Plugin(context, device) {

    override fun checkRequiredPermissions(): Boolean {
        return MouseReceiverService.instance != null
    }

    override val permissionExplanationDialog: DialogFragment
        get() {
            return StartActivityAlertDialogFragment.Builder()
                .setTitle(R.string.mouse_receiver_plugin_description)
                .setMessage(R.string.mouse_receiver_no_permissions)
                .setPositiveButton(R.string.open_settings)
                .setNegativeButton(R.string.cancel)
                .setIntentAction(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .setStartForResult(true)
                .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
                .create()
        }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_MOUSEPAD_REQUEST) {
            Log.e("MouseReceiverPlugin", "Invalid packet type for MouseReceiverPlugin: ${np.type}")
            return false
        }

        if (RemoteKeyboardPlugin.getMousePadPacketType(np) != RemoteKeyboardPlugin.MousePadPacketType.Mouse) {
            return false // This packet will be handled by the RemoteKeyboardPlugin instead, silently ignore
        }

        val dx = np.getDouble("dx", 0.0)
        val dy = np.getDouble("dy", 0.0)

        val isSingleClick = np.getBoolean("singleclick", false)
        val isDoubleClick = np.getBoolean("doubleclick", false)
        val isMiddleClick = np.getBoolean("middleclick", false)
        val isForwardClick = np.getBoolean("forwardclick", false)
        val isBackClick = np.getBoolean("backclick", false)

        val isRightClick = np.getBoolean("rightclick", false)
        val isSingleHold = np.getBoolean("singlehold", false)
        val isSingleRelease = np.getBoolean("singlerelease", false)
        val isScroll = np.getBoolean("scroll", false)

        if (isSingleClick || isDoubleClick || isMiddleClick || isRightClick || isSingleHold || isSingleRelease || isScroll || isForwardClick || isBackClick) {
            // Perform click
            return when {
                isSingleClick -> MouseReceiverService.click()
                isDoubleClick -> MouseReceiverService.recentButton()
                isMiddleClick -> MouseReceiverService.homeButton()
                isRightClick -> MouseReceiverService.backButton()
                isForwardClick -> MouseReceiverService.recentButton()
                isBackClick -> MouseReceiverService.backButton()
                isSingleHold -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MouseReceiverService.longClickSwipe()
                    } else {
                        MouseReceiverService.longClick()
                    }
                }
                isSingleRelease -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MouseReceiverService.instance?.stopSwipe() ?: false
                    } else {
                        false
                    }
                }
                isScroll -> MouseReceiverService.scroll(dx, dy)
                else -> false
            }
        } else {
            // Mouse Move
            if (dx != 0.0 || dy != 0.0) {
                return MouseReceiverService.move(dx, dy)
            }
        }

        return super.onPacketReceived(tp)
    }

    override val minSdk: Int
        get() = Build.VERSION_CODES.N

    override val displayName: String
        get() = context.getString(R.string.mouse_receiver_plugin_name)

    override val description: String
        get() = context.getString(R.string.mouse_receiver_plugin_description)

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_MOUSEPAD_REQUEST)

    override val outgoingPacketTypes: Array<String>
        get() = ArrayUtils.EMPTY_STRING_ARRAY

    companion object {
        private const val PACKET_TYPE_MOUSEPAD_REQUEST = "cconnect.mousepad.request"
    }
}
