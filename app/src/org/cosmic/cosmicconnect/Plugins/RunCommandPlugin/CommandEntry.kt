/*
 * SPDX-FileCopyrightText: 2016 Thomas Posch <cosmicconnect@online.posch.name>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import org.json.JSONException
import org.json.JSONObject
import org.cosmic.cosmicconnect.UserInterface.List.EntryItem

open class CommandEntry(name: String, cmd: String, val key: String) : EntryItem(name, cmd) {

    @Throws(JSONException::class)
    constructor(o: JSONObject) : this(
        o.getString("name"),
        o.getString("command"),
        o.getString("key")
    )

    val name: String
        get() = title ?: ""

    val command: String
        get() = subtitle ?: ""
}
