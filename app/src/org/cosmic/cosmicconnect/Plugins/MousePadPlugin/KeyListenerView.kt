/*
 * SPDX-FileCopyrightText: 2014 Saikrishna Arcot <saiarcot895@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.MousePadPlugin

import android.content.Context
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import dagger.hilt.EntryPoints
import org.cosmic.cosmicconnect.Core.NetworkPacketBuilder
import org.cosmic.cosmicconnect.di.HiltBridges

class KeyListenerView(context: Context, set: AttributeSet?) : View(context, set) {

    private var deviceId: String? = null
    private val deviceRegistry = EntryPoints.get(context.applicationContext, HiltBridges::class.java).deviceRegistry()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setDeviceId(id: String?) {
        deviceId = id
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
        return KeyInputConnection(this, true)
    }

    override fun onCheckIsTextEditor(): Boolean = true

    fun sendChars(chars: CharSequence) {
        val plugin = deviceRegistry.getDevicePlugin(deviceId, MousePadPlugin::class.java)
        plugin?.sendText(chars.toString())
    }

    private fun sendKeyPressPacket(body: Map<String, Any>) {
        val plugin = deviceRegistry.getDevicePlugin(deviceId, MousePadPlugin::class.java)
        plugin?.sendKeyPacket(body)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // consume events that otherwise would move the focus away from us
        return keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // We don't want to swallow the back button press
            return false
        }

        val body = mutableMapOf<String, Any>()

        var modifier = false
        if (event.isAltPressed) {
            body["alt"] = true
            modifier = true
        }

        if (event.isCtrlPressed) {
            body["ctrl"] = true
            modifier = true
        }

        if (event.isShiftPressed) {
            body["shift"] = true
        }

        if (event.isMetaPressed) {
            body["super"] = true
            modifier = true
        }

        val specialKey = SpecialKeysMap.get(keyCode, -1)

        if (specialKey != -1) {
            body["specialKey"] = specialKey
        } else if (event.displayLabel.code != 0 && modifier) {
            val keyCharacter = event.displayLabel
            body["key"] = keyCharacter.toString().lowercase()
        } else {
            body["key"] = event.unicodeChar.toChar().toString()
        }

        sendKeyPressPacket(body)
        return true
    }

    companion object {
        @JvmField
        val SpecialKeysMap = SparseIntArray()

        init {
            // Special key codes aligned with Desktop (cosmic-connect-core) values
            // See: https://github.com/olafkfreund/cosmic-connect-core protocol spec
            SpecialKeysMap.put(KeyEvent.KEYCODE_DEL, 1)           // Backspace
            SpecialKeysMap.put(KeyEvent.KEYCODE_TAB, 2)           // Tab
            SpecialKeysMap.put(KeyEvent.KEYCODE_ENTER, 12)        // Enter/Return
            SpecialKeysMap.put(KeyEvent.KEYCODE_NUMPAD_ENTER, 12) // Numpad Enter

            // Arrow keys - Desktop values
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_LEFT, 21)    // Left Arrow
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_UP, 22)      // Up Arrow
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, 23)   // Right Arrow
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_DOWN, 24)    // Down Arrow

            // Navigation keys - Desktop values
            SpecialKeysMap.put(KeyEvent.KEYCODE_PAGE_UP, 25)      // Page Up
            SpecialKeysMap.put(KeyEvent.KEYCODE_PAGE_DOWN, 26)    // Page Down
            SpecialKeysMap.put(KeyEvent.KEYCODE_ESCAPE, 27)       // Escape
            SpecialKeysMap.put(KeyEvent.KEYCODE_MOVE_HOME, 28)    // Home
            SpecialKeysMap.put(KeyEvent.KEYCODE_MOVE_END, 29)     // End
            SpecialKeysMap.put(KeyEvent.KEYCODE_FORWARD_DEL, 30)  // Delete (Forward)

            // Function keys - Desktop values (F1=31 through F12=42)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F1, 31)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F2, 32)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F3, 33)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F4, 34)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F5, 35)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F6, 36)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F7, 37)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F8, 38)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F9, 39)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F10, 40)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F11, 41)
            SpecialKeysMap.put(KeyEvent.KEYCODE_F12, 42)

            // Other special keys
            SpecialKeysMap.put(KeyEvent.KEYCODE_SYSRQ, 15)        // Print Screen
            SpecialKeysMap.put(KeyEvent.KEYCODE_SCROLL_LOCK, 16)  // Scroll Lock
        }
    }
}