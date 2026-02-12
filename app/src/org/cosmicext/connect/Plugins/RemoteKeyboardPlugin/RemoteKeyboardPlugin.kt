/*
 * SPDX-FileCopyrightText: 2017 Holger Kaelberer <holger.k@elberer.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.RemoteKeyboardPlugin

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.util.SparseIntArray
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import androidx.annotation.NonNull
import androidx.core.util.Pair
import androidx.fragment.app.DialogFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import org.cosmicext.connect.Core.*
import org.cosmicext.connect.Device
import org.cosmicext.connect.Plugins.Plugin
import org.cosmicext.connect.Plugins.PluginFactory
import org.cosmicext.connect.Plugins.di.PluginCreator
import org.cosmicext.connect.UserInterface.MainActivity
import org.cosmicext.connect.UserInterface.PluginSettingsFragment
import org.cosmicext.connect.UserInterface.StartActivityAlertDialogFragment
import org.cosmicext.connect.R
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock

class RemoteKeyboardPlugin @AssistedInject constructor(
    @ApplicationContext context: Context,
    @Assisted device: Device,
) : Plugin(context, device), SharedPreferences.OnSharedPreferenceChangeListener {

    @AssistedFactory
    interface Factory : PluginCreator {
        override fun create(device: Device): RemoteKeyboardPlugin
    }

    override fun onCreate(): Boolean {
        Log.d("RemoteKeyboardPlugin", "Creating for device ${device.name}")
        acquireInstances()
        try {
            instances.add(this)
        } finally {
            releaseInstances()
        }
        RemoteKeyboardService.instance?.let { service ->
            service.updateInputView()
        }

        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this)

        val editingOnly = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.remotekeyboard_editing_only), true)
        val visible = RemoteKeyboardService.instance?.isVisible == true
        notifyKeyboardState(!editingOnly || visible)

        return true
    }

    override fun onDestroy() {
        acquireInstances()
        try {
            if (instances.contains(this)) {
                instances.remove(this)
                if (instances.isEmpty()) {
                    RemoteKeyboardService.instance?.updateInputView()
                }
            }
        } finally {
            releaseInstances()
        }

        Log.d("RemoteKeyboardPlugin", "Destroying for device ${device.name}")
        super.onDestroy()
    }

    override val displayName: String
        get() = context.getString(R.string.pref_plugin_remotekeyboard)

    override val description: String
        get() = context.getString(R.string.pref_plugin_remotekeyboard_desc)

    override fun hasSettings(): Boolean = true

    override fun getSettingsFragment(activity: Activity): PluginSettingsFragment {
        return PluginSettingsFragment.newInstance(pluginKey, R.xml.remotekeyboardplugin_preferences)
    }

    override val supportedPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_MOUSEPAD_REQUEST)

    override val outgoingPacketTypes: Array<String>
        get() = arrayOf(PACKET_TYPE_MOUSEPAD_ECHO, PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE)

    private fun isValidSpecialKey(key: Int): Boolean {
        return specialKeyMap.get(key, 0) > 0
    }

    private fun getCharPos(extractedText: ExtractedText?, ch: Char, forward: Boolean): Int {
        if (extractedText != null) {
            val text = extractedText.text.toString()
            return if (!forward) { // backward
                text.lastIndexOf(ch, extractedText.selectionEnd - 2)
            } else {
                text.indexOf(ch, extractedText.selectionEnd + 1)
            }
        }
        return -1
    }

    private fun currentTextLength(extractedText: ExtractedText?): Int {
        return extractedText?.text?.length ?: -1
    }

    private fun currentCursorPos(extractedText: ExtractedText?): Int {
        return extractedText?.selectionEnd ?: -1
    }

    private fun currentSelection(extractedText: ExtractedText?): Pair<Int, Int> {
        return if (extractedText != null) {
            Pair(extractedText.selectionStart, extractedText.selectionEnd)
        } else {
            Pair(-1, -1)
        }
    }

    private fun handleSpecialKey(key: Int, shift: Boolean, ctrl: Boolean, alt: Boolean): Boolean {
        val keyEvent = specialKeyMap.get(key, 0)
        if (keyEvent == 0) return false
        
        val inputConn = RemoteKeyboardService.instance?.currentInputConnection ?: return false

        // special sequences:
        if (ctrl && (keyEvent == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            // Ctrl + right -> next word
            val extractedText = inputConn.getExtractedText(ExtractedTextRequest(), 0)
            var pos = getCharPos(extractedText, ' ', true)
            if (pos == -1) {
                pos = currentTextLength(extractedText)
            } else {
                pos++
            }
            var startPos = pos
            val endPos = pos
            if (shift) { // Shift -> select word (otherwise jump)
                val sel = currentSelection(extractedText)
                val cursor = currentCursorPos(extractedText)
                startPos = cursor
                if (sel.first < cursor || sel.first > sel.second) {
                    startPos = sel.first
                }
            }
            inputConn.setSelection(startPos, endPos)
        } else if (ctrl && keyEvent == KeyEvent.KEYCODE_DPAD_LEFT) {
            // Ctrl + left -> previous word
            val extractedText = inputConn.getExtractedText(ExtractedTextRequest(), 0)
            var pos = getCharPos(extractedText, ' ', false)
            if (pos == -1) {
                pos = 0
            } else {
                pos++
            }
            var startPos = pos
            val endPos = pos
            if (shift) {
                val sel = currentSelection(extractedText)
                val cursor = currentCursorPos(extractedText)
                startPos = cursor
                if (cursor < sel.first || sel.first < sel.second) {
                    startPos = sel.first
                }
            }
            inputConn.setSelection(startPos, endPos)
        } else if (shift && (keyEvent == KeyEvent.KEYCODE_DPAD_LEFT || keyEvent == KeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyEvent == KeyEvent.KEYCODE_DPAD_UP || keyEvent == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyEvent == KeyEvent.KEYCODE_MOVE_HOME || keyEvent == KeyEvent.KEYCODE_MOVE_END)) {
            // Shift + up/down/left/right/home/end
            val now = SystemClock.uptimeMillis()
            inputConn.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0))
            inputConn.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyEvent, 0, KeyEvent.META_SHIFT_LEFT_ON))
            inputConn.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyEvent, 0, KeyEvent.META_SHIFT_LEFT_ON))
            inputConn.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0))
        } else if (keyEvent == KeyEvent.KEYCODE_NUMPAD_ENTER || keyEvent == KeyEvent.KEYCODE_ENTER) {
            // Enter key
            val editorInfo = RemoteKeyboardService.instance?.currentInputEditorInfo
            if (editorInfo != null && ((editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0) || ctrl)) {
                // check for special DONE/GO/etc actions first:
                val actions = intArrayOf(
                    EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT,
                    EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_SEARCH,
                    EditorInfo.IME_ACTION_DONE
                )
                for (action in actions) {
                    if ((editorInfo.imeOptions and action) == action) {
                        inputConn.performEditorAction(action)
                        return true
                    }
                }
            } else {
                inputConn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
                inputConn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
            }
        } else {
            // default handling:
            inputConn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
            inputConn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
        }

        return true
    }

    private fun handleVisibleKey(key: String, shift: Boolean, ctrl: Boolean, alt: Boolean): Boolean {
        if (key.isEmpty()) return false

        val inputConn = RemoteKeyboardService.instance?.currentInputConnection ?: return false

        // ctrl+c/v/x/a
        if (ctrl) {
            when (key.lowercase()) {
                "c" -> return inputConn.performContextMenuAction(android.R.id.copy)
                "v" -> return inputConn.performContextMenuAction(android.R.id.paste)
                "x" -> return inputConn.performContextMenuAction(android.R.id.cut)
                "a" -> return inputConn.performContextMenuAction(android.R.id.selectAll)
            }
        }

        inputConn.commitText(key, key.length)
        return true
    }

    private fun handleEvent(np: org.cosmicext.connect.Core.NetworkPacket): Boolean {
        if (np.has("specialKey") && isValidSpecialKey(np.getInt("specialKey"))) {
            return handleSpecialKey(
                np.getInt("specialKey"),
                np.getBoolean("shift"),
                np.getBoolean("ctrl"),
                np.getBoolean("alt")
            )
        }

        // try visible key
        return handleVisibleKey(
            np.getString("key"),
            np.getBoolean("shift"),
            np.getBoolean("ctrl"),
            np.getBoolean("alt")
        )
    }

    enum class MousePadPacketType {
        Keyboard,
        Mouse,
    }

    override fun onPacketReceived(tp: TransferPacket): Boolean {
        val np = tp.packet
        if (np.type != PACKET_TYPE_MOUSEPAD_REQUEST) {
            Log.e("RemoteKeyboardPlugin", "Invalid packet type for RemoteKeyboardPlugin: ${np.type}")
            return false
        }

        if (getMousePadPacketType(np) != MousePadPacketType.Keyboard) {
            return false // This packet will be handled by the MouseReceiverPlugin instead, silently ignore
        }

        val service = RemoteKeyboardService.instance
        if (service == null) {
            Log.i("RemoteKeyboardPlugin", "Remote keyboard is not the currently selected input method, dropping key")
            return false
        }

        if (!service.isVisible &&
            PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.remotekeyboard_editing_only), true)) {
            Log.i("RemoteKeyboardPlugin", "Remote keyboard is currently not visible, dropping key")
            return false
        }

        if (!handleEvent(np)) {
            Log.i("RemoteKeyboardPlugin", "Could not handle event!")
            return false
        }

        if (np.getBoolean("sendAck")) {
            // Build echo packet body
            val body = mutableMapOf<String, Any>()
            body["key"] = np.getString("key")
            if (np.has("specialKey")) body["specialKey"] = np.getInt("specialKey")
            if (np.has("shift")) body["shift"] = np.getBoolean("shift")
            if (np.has("ctrl")) body["ctrl"] = np.getBoolean("ctrl")
            if (np.has("alt")) body["alt"] = np.getBoolean("alt")
            body["isAck"] = true

            // Create packet using FFI
            val packet = RemoteKeyboardPacketsFFI.createEchoPacket(JSONObject(body).toString())

            // Send packet
            device.sendPacket(TransferPacket(packet))
        }

        return true
    }

    fun notifyKeyboardState(state: Boolean) {
        Log.d("RemoteKeyboardPlugin", "Keyboardstate changed to $state")

        // Create packet using FFI
        val packet = RemoteKeyboardPacketsFFI.createKeyboardStatePacket(state)

        // Send packet
        device.sendPacket(TransferPacket(packet))
    }

    val deviceIdValue: String
        get() = device.deviceId

    override fun checkRequiredPermissions(): Boolean {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val inputMethodList = inputMethodManager.enabledInputMethodList
        return inputMethodList.any { context.packageName == it.packageName }
    }

    override val permissionExplanationDialog: DialogFragment
        get() {
            return StartActivityAlertDialogFragment.Builder()
                .setTitle(R.string.pref_plugin_remotekeyboard)
                .setMessage(R.string.no_permissions_remotekeyboard)
                .setPositiveButton(R.string.open_settings)
                .setNegativeButton(R.string.cancel)
                .setIntentAction(Settings.ACTION_INPUT_METHOD_SETTINGS)
                .setStartForResult(true)
                .setRequestCode(MainActivity.RESULT_NEEDS_RELOAD)
                .create()
        }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key == context.getString(R.string.remotekeyboard_editing_only)) {
            val editingOnly = sharedPreferences.getBoolean(context.getString(R.string.remotekeyboard_editing_only), true)
            val visible = RemoteKeyboardService.instance?.isVisible == true
            notifyKeyboardState(!editingOnly || visible)
        }
    }

    companion object {
        private const val PACKET_TYPE_MOUSEPAD_REQUEST = "cconnect.mousepad.request"
        private const val PACKET_TYPE_MOUSEPAD_ECHO = "cconnect.mousepad.echo"
        private const val PACKET_TYPE_MOUSEPAD_KEYBOARDSTATE = "cconnect.mousepad.keyboardstate"

        /**
         * Track and expose plugin instances to allow for a 'connected'-indicator in the IME:
         */
        private val instances = mutableListOf<RemoteKeyboardPlugin>()
        private val instancesLock = ReentrantLock(true)

        @JvmStatic
        fun acquireInstances(): ArrayList<RemoteKeyboardPlugin> {
            instancesLock.lock()
            return ArrayList(instances)
        }

        @JvmStatic
        fun releaseInstances() {
            instancesLock.unlock()
        }

        @JvmStatic
        fun isConnected(): Boolean {
            return instances.isNotEmpty()
        }

        @JvmStatic
        fun getMousePadPacketType(np: org.cosmicext.connect.Core.NetworkPacket): MousePadPacketType {
            return if (np.has("key") || np.has("specialKey")) {
                MousePadPacketType.Keyboard
            } else {
                MousePadPacketType.Mouse
            }
        }

        private val specialKeyMap = SparseIntArray()

        init {
            var i = 0
            specialKeyMap.put(++i, KeyEvent.KEYCODE_DEL) // 1
            specialKeyMap.put(++i, KeyEvent.KEYCODE_TAB) // 2
            ++i // 3 is not used
            specialKeyMap.put(++i, KeyEvent.KEYCODE_DPAD_LEFT) // 4
            specialKeyMap.put(++i, KeyEvent.KEYCODE_DPAD_UP) // 5
            specialKeyMap.put(++i, KeyEvent.KEYCODE_DPAD_RIGHT) // 6
            specialKeyMap.put(++i, KeyEvent.KEYCODE_DPAD_DOWN) // 7
            specialKeyMap.put(++i, KeyEvent.KEYCODE_PAGE_UP) // 8
            specialKeyMap.put(++i, KeyEvent.KEYCODE_PAGE_DOWN) // 9
            specialKeyMap.put(++i, KeyEvent.KEYCODE_MOVE_HOME) // 10
            specialKeyMap.put(++i, KeyEvent.KEYCODE_MOVE_END) // 11
            specialKeyMap.put(++i, KeyEvent.KEYCODE_ENTER) // 12
            specialKeyMap.put(++i, KeyEvent.KEYCODE_FORWARD_DEL) // 13
            specialKeyMap.put(++i, KeyEvent.KEYCODE_ESCAPE) // 14
            specialKeyMap.put(++i, KeyEvent.KEYCODE_SYSRQ) // 15
            specialKeyMap.put(++i, KeyEvent.KEYCODE_SCROLL_LOCK) // 16
            ++i // 17
            ++i // 18
            ++i // 19
            ++i // 20
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F1) // 21
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F2) // 22
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F3) // 23
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F4) // 24
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F5) // 25
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F6) // 26
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F7) // 27
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F8) // 28
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F9) // 29
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F10) // 30
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F11) // 31
            specialKeyMap.put(++i, KeyEvent.KEYCODE_F12) // 32
        }
    }
}
