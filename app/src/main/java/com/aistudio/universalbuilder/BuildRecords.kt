package com.aistudio.universalbuilder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class BuildRecords(context: Context) {
    private val prefs = context.getSharedPreferences("build_records_v3", Context.MODE_PRIVATE)
    @Synchronized fun get(id: String): JSONObject? = prefs.getString(id, null)?.let { JSONObject(it) }
    @Synchronized fun save(id: String, record: JSONObject) { prefs.edit().putString(id, record.toString()).commit() }
    fun all(): List<JSONObject> = prefs.all.values.mapNotNull { value ->
        try { JSONObject(value as String) } catch (_: Exception) { null }
    }.sortedByDescending { it.optLong("created") }
}
