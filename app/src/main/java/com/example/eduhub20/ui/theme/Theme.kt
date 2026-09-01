package com.example.eduhub20.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.content.edit

object ThemeState {
    internal const val PREFS_NAME = "eduhub_theme_prefs"
    internal const val KEY_DARK_THEME = "dark_theme"

    var isDarkTheme = mutableStateOf(false)
        private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isDarkTheme.value = prefs.getBoolean(KEY_DARK_THEME, false)
    }

    fun toggleTheme() {
        isDarkTheme.value = !isDarkTheme.value
        saveTheme()
    }

    fun setTheme(isDark: Boolean) {
        isDarkTheme.value = isDark
        saveTheme()
    }

    private fun saveTheme() {
        // Save to SharedPreferences - will be called from context
    }
}

fun ThemeState.saveTheme(context: Context) {
    val prefs = context.getSharedPreferences(ThemeState.PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(ThemeState.KEY_DARK_THEME, ThemeState.isDarkTheme.value) }
}
private val DarkColorScheme = darkColorScheme(
    primary = EduHubSecondary,
    secondary = EduHubAccentGreen,
    tertiary = EduHubAccentOrange,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = EduHubPrimary,
    onPrimary = Color.White,
    secondary = EduHubAccentGreen,
    tertiary = EduHubAccentOrange,
    background = EduHubBgLight,
    surface = EduHubSurfaceLight,
    onSurface = EduHubTextPrimary,
    onSurfaceVariant = EduHubTextSecondary
)

@Composable
fun Eduhub20Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val useDarkTheme = ThemeState.isDarkTheme.value || darkTheme
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}