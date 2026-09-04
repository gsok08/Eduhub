package com.example.eduhub20

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduhub20.ui.theme.Eduhub20Theme
import kotlinx.coroutines.delay

/**
 * CrashRecoveryActivity - shown when the global uncaught exception handler
 * catches a fatal crash. Clears corrupted local cache and restarts MainActivity.
 */
class CrashRecoveryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearCorruptedCache()

        setContent {
            Eduhub20Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alpha = remember { Animatable(0f) }

                    LaunchedEffect(Unit) {
                        alpha.animateTo(1f, animationSpec = tween(600))
                        delay(2000)
                        restartApp()
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(alpha.value),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "📚", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "EduHub",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recovering and restarting…",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }
            }
        }
    }

    private fun clearCorruptedCache() {
        try {
            val cachePrefs = getSharedPreferences("eduhub_local_cache", Context.MODE_PRIVATE)
            val allKeys = cachePrefs.all.keys.toList()
            val safeToClearPrefixes = listOf(
                "cached_courses",
                "cached_announcements",
                "cached_lecture_notes",
                "cached_ai_notes_map",
                "cached_study_groups",
                "cached_past_year_papers",
                "chat_messages_"
            )
            cachePrefs.edit().also { editor ->
                allKeys.forEach { key ->
                    if (safeToClearPrefixes.any { key.startsWith(it) }) {
                        editor.remove(key)
                    }
                }
            }.apply()
        } catch (_: Exception) {}
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
}
