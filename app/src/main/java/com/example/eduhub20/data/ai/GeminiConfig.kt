package com.example.eduhub20.data.ai

import android.content.Context
import androidx.core.content.edit
import com.example.eduhub20.BuildConfig

object GeminiConfig {
    private const val PREFS_NAME = "eduhub_gemini_config"
    private const val KEY_API_KEY = "gemini_api_key"

    // Loaded securely from local.properties via BuildConfig (never committed to git)
    var GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY

    // Official recommended multimodal model endpoint
    const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_API_KEY, "") ?: ""
        if (saved.isNotBlank()) {
            GEMINI_API_KEY = saved
        } else if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
            prefs.edit { putString(KEY_API_KEY, GEMINI_API_KEY) }
        }
    }

    fun saveApiKey(context: Context, key: String) {
        GEMINI_API_KEY = key.trim()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_API_KEY, GEMINI_API_KEY) }
    }
}
