package com.dailycurator.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedHashSet

private const val T = "t"
private const val V = "v"

/** Typed export so restore preserves int/long/float/boolean/string/string sets. */
internal fun exportCuratorPrefsToJson(context: Context): String {
    val prefs = context.getSharedPreferences(CURATOR_PREFS_NAME, Context.MODE_PRIVATE)
    val root = JSONObject()
    for ((key, value) in prefs.all) {
        val cell = JSONObject()
        when (value) {
            is Boolean -> {
                cell.put(T, "bool")
                cell.put(V, value)
            }
            is String -> {
                cell.put(T, "str")
                cell.put(V, value)
            }
            is Int -> {
                cell.put(T, "int")
                cell.put(V, value)
            }
            is Long -> {
                cell.put(T, "long")
                cell.put(V, value)
            }
            is Float -> {
                cell.put(T, "float")
                cell.put(V, value.toDouble())
            }
            is Set<*> -> {
                cell.put(T, "strset")
                val arr = JSONArray()
                @Suppress("UNCHECKED_CAST")
                (value as Set<String>).sorted().forEach { arr.put(it) }
                cell.put(V, arr)
            }
            else -> continue
        }
        root.put(key, cell)
    }
    return root.toString()
}

internal fun restoreCuratorPrefsFromJson(context: Context, json: String) {
    val prefs = context.getSharedPreferences(CURATOR_PREFS_NAME, Context.MODE_PRIVATE)
    val root = JSONObject(json)
    val editor = prefs.edit().clear()
    val keys = root.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val cell = root.getJSONObject(key)
        when (cell.getString(T)) {
            "bool" -> editor.putBoolean(key, cell.getBoolean(V))
            "str" -> editor.putString(key, cell.getString(V))
            "int" -> editor.putInt(key, cell.getInt(V))
            "long" -> editor.putLong(key, cell.getLong(V))
            "float" -> editor.putFloat(key, cell.getDouble(V).toFloat())
            "strset" -> {
                val arr = cell.getJSONArray(V)
                val set = LinkedHashSet<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                editor.putStringSet(key, set)
            }
        }
    }
    editor.apply()
}
