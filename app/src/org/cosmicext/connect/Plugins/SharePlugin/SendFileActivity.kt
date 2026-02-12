/*
 * SPDX-FileCopyrightText: 2014 Albert Vaca Cintora <albertvaka@gmail.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */

package org.cosmicext.connect.Plugins.SharePlugin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import org.cosmicext.connect.Core.DeviceRegistry
import org.cosmicext.connect.Helpers.ThreadHelper
import org.cosmicext.connect.R
import javax.inject.Inject

@AndroidEntryPoint
class SendFileActivity : AppCompatActivity() {

    @Inject lateinit var deviceRegistry: DeviceRegistry

    private var mDeviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mDeviceId = intent.getStringExtra("deviceId")

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.send_files)), Activity.RESULT_FIRST_USER
            )
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_file_browser, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Activity.RESULT_FIRST_USER -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uris = ArrayList<Uri>()

                    data.data?.let { uris.add(it) }

                    data.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            uris.add(clipData.getItemAt(i).uri)
                        }
                    }

                    if (uris.isEmpty()) {
                        Log.w("SendFileActivity", "No files to send?")
                    } else {
                        ThreadHelper.execute {
                            val plugin = deviceRegistry.getDevicePlugin(mDeviceId, SharePlugin::class.java)
                            if (plugin == null) {
                                finish()
                                return@execute
                            }
                            plugin.sendUriList(uris)
                        }
                    }
                }
                finish()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}