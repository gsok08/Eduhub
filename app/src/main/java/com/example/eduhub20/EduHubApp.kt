package com.example.eduhub20

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.eduhub20.data.ai.GeminiConfig
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.repository.PomodoroRepository
import com.example.eduhub20.data.service.GroupNotificationService
import com.example.eduhub20.ui.theme.ThemeState

class EduHubApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // ── Initialise all persistent stores immediately on Application start ──
        // This ensures no Activity, Service, or ViewModel ever hits a null prefs.
        EduHubLocalStorage.init(this)
        ThemeState.init(this)
        GeminiConfig.init(this)
        PomodoroRepository.init(this)

        // ── Global Uncaught Exception Handler ─────────────────────────────────
        // Catches any unhandled crash and restarts CrashRecoveryActivity instead
        // of showing the system "App has stopped" dialog.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("EduHubCrash", "Uncaught exception on thread ${thread.name}", throwable)
                // Write crash to local log for debugging
                writeCrashLog(throwable)
                // Launch CrashRecoveryActivity
                val intent = Intent(applicationContext, CrashRecoveryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            } catch (_: Exception) {
                // If our handler itself fails, fall back to the original handler
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        // ── Background Notification Service ───────────────────────────────────
        val authPrefs = getSharedPreferences("eduhub_auth_prefs", Context.MODE_PRIVATE)
        val rememberMe = authPrefs.getBoolean("remember_me", false)
        val savedUserId = authPrefs.getString("saved_user_id", null)
        if (rememberMe && !savedUserId.isNullOrBlank()) {
            try {
                GroupNotificationService.start(this)
            } catch (_: Exception) {}
        }
    }

    private fun writeCrashLog(throwable: Throwable) {
        try {
            val logFile = java.io.File(filesDir, "crash_log.txt")
            val timestamp = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date())
            val entry = buildString {
                append("[$timestamp]\n")
                append("${throwable::class.java.simpleName}: ${throwable.message}\n")
                throwable.stackTrace.take(15).forEach { append("  at $it\n") }
                append("\n")
            }
            // Keep only last 50 KB to avoid filling storage
            val existing = if (logFile.exists()) logFile.readText() else ""
            val trimmed = if (existing.length > 40_000) existing.takeLast(40_000) else existing
            logFile.writeText(trimmed + entry)
        } catch (_: Exception) {}
    }
}
