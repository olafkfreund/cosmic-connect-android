/*
 * SPDX-FileCopyrightText: 2014 The Android Open Source Project
 * SPDX-FileCopyrightText: 1997, 2021, Oracle and/or its affiliates. All rights reserved
 *
 * SPDX-FileCopyrightText: 2024 ShellWen Chen <me@shellwen.com>
 *
 * SPDX-License-Identifier: GPL-2.0-only OR GPL-3.0-only OR LicenseRef-KDE-Accepted-GPL
 */
package org.cosmic.cosmicconnect.Helpers

import android.os.Build
import android.os.Build.VERSION
import java.io.Serializable
import java.util.*

/** @noinspection unused*/
object CollectionsBackport {
    @JvmStatic
    fun <T> unmodifiableNavigableSet(s: NavigableSet<T>): NavigableSet<T> {
        return if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Collections.unmodifiableNavigableSet(s)
        } else {
            // Simple wrapper for lower APIs if needed, but for now let's try to use standard Collections
            // If it fails at runtime we can implement a full backport
            Collections.unmodifiableSortedSet(s) as NavigableSet<T>
        }
    }

    @JvmStatic
    fun <T> unmodifiableSet(s: Set<T>): Set<T> {
        return Collections.unmodifiableSet(s)
    }

    @JvmStatic
    fun <T> unmodifiableCollection(c: Collection<T>): Collection<T> {
        return Collections.unmodifiableCollection(c)
    }

    @JvmStatic
    fun <K, V> unmodifiableNavigableMap(m: NavigableMap<K, V>): NavigableMap<K, V> {
        return if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Collections.unmodifiableNavigableMap(m)
        } else {
            Collections.unmodifiableSortedMap(m) as NavigableMap<K, V>
        }
    }

    @JvmStatic
    fun <K, V> unmodifiableMap(m: Map<K, V>): Map<K, V> {
        return Collections.unmodifiableMap(m)
    }

    @JvmStatic
    fun <T> emptyNavigableSet(): NavigableSet<T> {
        return TreeSet<T>()
    }

    @JvmStatic
    fun <K, V> emptyNavigableMap(): NavigableMap<K, V> {
        return TreeMap<K, V>()
    }
}
