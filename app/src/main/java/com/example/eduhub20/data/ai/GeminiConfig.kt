package com.example.eduhub20.data.ai

import android.content.Context
import androidx.core.content.edit
import com.example.eduhub20.BuildConfig

object GeminiConfig {
    private const val PREFS_NAME = "eduhub_gemini_config"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_BACKEND_URL = "flask_backend_url"

    // Loaded securely from local.properties via BuildConfig (never committed to git)
    var GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY

    // Flask Backend Server URL (default emulator: 10.0.2.2:5000, physical phone: http://192.168.x.x:5000)
    var BACKEND_URL: String = "http://10.0.2.2:5000"

    // Official recommended multimodal model endpoint
    const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent"

    /** True when a Gemini API key is configured — gates all AI features. */
    val isAvailable: Boolean get() = GEMINI_API_KEY.isNotBlank()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_API_KEY, "") ?: ""
        if (savedKey.isNotBlank()) {
            GEMINI_API_KEY = savedKey
        } else if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
            prefs.edit { putString(KEY_API_KEY, GEMINI_API_KEY) }
        }

        val savedBackend = prefs.getString(KEY_BACKEND_URL, "") ?: ""
        if (savedBackend.isNotBlank()) {
            BACKEND_URL = savedBackend.trim().removeSuffix("/")
        }
    }

    fun saveApiKey(context: Context, key: String) {
        GEMINI_API_KEY = key.trim()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_API_KEY, GEMINI_API_KEY) }
    }

    fun saveBackendUrl(context: Context, url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        BACKEND_URL = if (cleanUrl.isNotBlank()) cleanUrl else "http://10.0.2.2:5000"
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_BACKEND_URL, BACKEND_URL) }
    }
}
