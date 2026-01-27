/*
 * SPDX-FileCopyrightText: 2016 Thomas Posch <cosmicconnect@online.posch.name>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
*/

package org.cosmic.cosmicconnect.Plugins.RunCommandPlugin

import org.json.JSONException
import org.json.JSONObject

open class CommandEntry(val name: String, val command: String, val key: String) {

    @Throws(JSONException::class)
    constructor(o: JSONObject) : this(
        o.getString("name"),
        o.getString("command"),
        o.getString("key")
    )
}
