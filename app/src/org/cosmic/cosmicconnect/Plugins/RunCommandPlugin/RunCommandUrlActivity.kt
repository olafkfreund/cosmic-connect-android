/*
 * SPDX-FileCopyrightText: 2018 Nicolas Fella <nicolas.fella@gmx.de>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.cosmic.cosmicconnect.CosmicConnect
import org.cosmic.cosmicconnect.R

class RunCommandUrlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != null) {
            try {
                val uri = intent.data ?: return
                val deviceId = uri.pathSegments[0]

                val device = CosmicConnect.getInstance().getDevice(deviceId)

                if (device == null) {
                    error(R.string.runcommand_nosuchdevice)
                    return
                }

                if (!device.isPaired) {
                    error(R.string.runcommand_notpaired)
                    return
                }

                if (!device.isReachable) {
                    error(R.string.runcommand_notreachable)
                    return
                }

                val plugin = device.getPlugin(RunCommandPlugin::class.java)
                if (plugin == null) {
                    error(R.string.runcommand_noruncommandplugin)
                    return
                }

                val commandKey = uri.pathSegments[1]

                // Show confirmation dialog before executing command from external intent
                AlertDialog.Builder(this)
                    .setTitle(R.string.runcommand_confirm_title)
                    .setMessage(getString(R.string.runcommand_confirm_message, device.name))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        plugin.runCommand(commandKey)

                        val vibrator = getSystemService(Vibrator::class.java)
                        if (vibrator != null && vibrator.hasVibrator()) {
                            vibrator.vibrate(100)
                        }
                        finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        finish()
                    }
                    .setOnCancelListener {
                        finish()
                    }
                    .show()

            } catch (e: Exception) {
                Log.e("RuncommandPlugin", "Exception", e)
            }
        }
    }

    private fun error(message: Int) {
        val view = TextView(this)
        view.setText(message)
        view.gravity = Gravity.CENTER
        setContentView(view)
    }
}
