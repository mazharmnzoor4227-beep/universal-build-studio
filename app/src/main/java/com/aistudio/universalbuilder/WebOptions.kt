package com.aistudio.universalbuilder
import android.content.Context
import org.json.JSONObject
class WebOptions(context: Context) {
    private val prefs = context.getSharedPreferences("web_options", Context.MODE_PRIVATE)
    fun clear() { prefs.edit().clear().apply() }
    fun enabled(key: String) = prefs.getBoolean(key, false)
    fun set(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun json(): JSONObject = JSONObject().apply {
        for (key in listOf("camera", "microphone", "library", "media", "landscape", "fullscreen", "location", "notifications", "vibration", "network")) put(key, enabled(key))
    }
}
