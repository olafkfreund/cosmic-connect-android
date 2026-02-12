/*
 * SPDX-FileCopyrightText: 2017 Holger Kaelberer <holger.k@elberer.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.RemoteKeyboardPlugin

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.cosmicext.connect.R
import org.cosmicext.connect.UserInterface.MainActivity
import org.cosmicext.connect.UserInterface.PluginSettingsActivity

class RemoteKeyboardService : InputMethodService(), OnKeyboardActionListener {

    /**
     * Whether this InputMethod is currently visible.
     */
    var isVisible: Boolean = false
        private set

    private var inputView: KeyboardView? = null
    private val handler: Handler = Handler(Looper.getMainLooper())

    fun updateInputView() {
        val currentInputView = inputView ?: return
        val currentKeyboard = currentInputView.keyboard
        val keys = currentKeyboard.keys
        val connected = RemoteKeyboardPlugin.isConnected()
        
        val disconnectedIcon = R.drawable.ic_phonelink_off_36dp
        val connectedIcon = R.drawable.ic_phonelink_36dp
        val statusKeyIdx = 3
        
        keys[statusKeyIdx].icon = ContextCompat.getDrawable(this, if (connected) connectedIcon else disconnectedIcon)
        currentInputView.invalidateKey(statusKeyIdx)
    }

    override fun onCreate() {
        super.onCreate()
        isVisible = false
        instance = this
        Log.d("RemoteKeyboardService", "Remote keyboard initialized")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d("RemoteKeyboardService", "Destroyed")
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this, null)
        view.keyboard = Keyboard(this, R.xml.remotekeyboardplugin_keyboard)
        view.isPreviewEnabled = false
        view.setOnKeyboardActionListener(this)
        inputView = view
        updateInputView()
        return view
    }

    override fun onStartInputView(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)
        isVisible = true
        val instances = RemoteKeyboardPlugin.acquireInstances()
        try {
            for (i in instances) {
                i.notifyKeyboardState(true)
            }
        } finally {
            RemoteKeyboardPlugin.releaseInstances()
        }

        window.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        isVisible = false
        val instances = RemoteKeyboardPlugin.acquireInstances()
        try {
            for (i in instances) {
                i.notifyKeyboardState(false)
            }
        } finally {
            RemoteKeyboardPlugin.releaseInstances()
        }

        window.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPress(primaryCode: Int) {
        when (primaryCode) {
            0 -> { // "hide keyboard"
                requestHideSelf(0)
            }
            1 -> { // "settings"
                val instances = RemoteKeyboardPlugin.acquireInstances()
                try {
                    if (instances.size == 1) { // single instance of RemoteKeyboardPlugin -> access its settings
                        val plugin = instances[0]
                        if (plugin != null) {
                            val intent = Intent(this, PluginSettingsActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra(PluginSettingsActivity.EXTRA_DEVICE_ID, plugin.deviceIdValue)
                                putExtra(PluginSettingsActivity.EXTRA_PLUGIN_KEY, plugin.pluginKey)
                            }
                            startActivity(intent)
                        }
                    } else { // != 1 instance of plugin -> show main activity view
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra(MainActivity.FLAG_FORCE_OVERVIEW, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                        if (instances.isEmpty()) {
                            Toast.makeText(this, R.string.remotekeyboard_not_connected, Toast.LENGTH_SHORT).show()
                        } else { // instances.size() > 1
                            Toast.makeText(this, R.string.remotekeyboard_multiple_connections, Toast.LENGTH_SHORT).show()
                        }
                    }
                } finally {
                    RemoteKeyboardPlugin.releaseInstances()
                }
            }
            2 -> { // "keyboard"
                val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
                imm?.showInputMethodPicker()
            }
            3 -> { // "connected"?
                if (RemoteKeyboardPlugin.isConnected()) {
                    Toast.makeText(this, R.string.remotekeyboard_connected, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.remotekeyboard_not_connected, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {}

    override fun onText(text: CharSequence?) {}

    override fun swipeRight() {}

    override fun swipeLeft() {}

    override fun swipeDown() {}

    override fun swipeUp() {}

    override fun onRelease(primaryCode: Int) {}

    companion object {
        /**
         * Reference to our instance
         * null if this InputMethod is not currently selected.
         */
        @JvmStatic
        var instance: RemoteKeyboardService? = null
            private set
    }
}
