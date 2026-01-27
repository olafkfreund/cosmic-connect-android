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
import org.cosmic.cosmicconnect.NetworkPacket
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

    private fun sendKeyPressPacket(np: NetworkPacket) {
        val plugin = deviceRegistry.getDevicePlugin(deviceId, MousePadPlugin::class.java)
        plugin?.sendPacket(np)
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

        val np = NetworkPacket(MousePadPlugin.PACKET_TYPE_MOUSEPAD_REQUEST)

        var modifier = false
        if (event.isAltPressed) {
            np.set("alt", true)
            modifier = true
        }

        if (event.isCtrlPressed) {
            np.set("ctrl", true)
            modifier = true
        }

        if (event.isShiftPressed) {
            np.set("shift", true)
        }

        if (event.isMetaPressed) {
            np.set("super", true)
            modifier = true
        }

        val specialKey = SpecialKeysMap.get(keyCode, -1)

        if (specialKey != -1) {
            np.set("specialKey", specialKey)
        } else if (event.displayLabel.code != 0 && modifier) {
            val keyCharacter = event.displayLabel
            np.set("key", keyCharacter.toString().lowercase())
        } else {
            np.set("key", event.unicodeChar.toChar().toString())
        }

        sendKeyPressPacket(np)
        return true
    }

    companion object {
        @JvmField
        val SpecialKeysMap = SparseIntArray()

        init {
            var i = 0
            SpecialKeysMap.put(KeyEvent.KEYCODE_DEL, ++i) // 1
            SpecialKeysMap.put(KeyEvent.KEYCODE_TAB, ++i) // 2
            SpecialKeysMap.put(KeyEvent.KEYCODE_ENTER, 12)
            ++i // 3 is not used, return is 12 instead
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_LEFT, ++i) // 4
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_UP, ++i) // 5
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, ++i) // 6
            SpecialKeysMap.put(KeyEvent.KEYCODE_DPAD_DOWN, ++i) // 7
            SpecialKeysMap.put(KeyEvent.KEYCODE_PAGE_UP, ++i) // 8
            SpecialKeysMap.put(KeyEvent.KEYCODE_PAGE_DOWN, ++i) // 9
            SpecialKeysMap.put(KeyEvent.KEYCODE_MOVE_HOME, ++i) // 10
            SpecialKeysMap.put(KeyEvent.KEYCODE_MOVE_END, ++i) // 11
            SpecialKeysMap.put(KeyEvent.KEYCODE_NUMPAD_ENTER, ++i) // 12
            SpecialKeysMap.put(KeyEvent.KEYCODE_FORWARD_DEL, ++i) // 13
            SpecialKeysMap.put(KeyEvent.KEYCODE_ESCAPE, ++i) // 14
            SpecialKeysMap.put(KeyEvent.KEYCODE_SYSRQ, ++i) // 15
            SpecialKeysMap.put(KeyEvent.KEYCODE_SCROLL_LOCK, ++i) // 16
            ++i // 17
            ++i // 18
            ++i // 19
            ++i // 20
            SpecialKeysMap.put(KeyEvent.KEYCODE_F1, ++i) // 21
            SpecialKeysMap.put(KeyEvent.KEYCODE_F2, ++i) // 22
            SpecialKeysMap.put(KeyEvent.KEYCODE_F3, ++i) // 23
            SpecialKeysMap.put(KeyEvent.KEYCODE_F4, ++i) // 24
            SpecialKeysMap.put(KeyEvent.KEYCODE_F5, ++i) // 25
            SpecialKeysMap.put(KeyEvent.KEYCODE_F6, ++i) // 26
            SpecialKeysMap.put(KeyEvent.KEYCODE_F7, ++i) // 27
            SpecialKeysMap.put(KeyEvent.KEYCODE_F8, ++i) // 28
            SpecialKeysMap.put(KeyEvent.KEYCODE_F9, ++i) // 29
            SpecialKeysMap.put(KeyEvent.KEYCODE_F10, ++i) // 30
            SpecialKeysMap.put(KeyEvent.KEYCODE_F11, ++i) // 31
            SpecialKeysMap.put(KeyEvent.KEYCODE_F12, ++i) // 32
        }
    }
}