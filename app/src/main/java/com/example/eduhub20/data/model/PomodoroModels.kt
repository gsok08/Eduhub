package com.example.eduhub20.data.model

import kotlinx.serialization.Serializable

enum class PomodoroPhase(
    val title: String,
    val defaultMinutes: Int,
    val badgeLabel: String,
    val description: String
) {
    FOCUS("Focus Session", 25, "🎯 Focus", "Time to immerse and study together without distractions"),
    SHORT_BREAK("Short Break", 5, "☕ Rest", "Stretch, grab some water, and relax your eyes"),
    LONG_BREAK("Long Break", 15, "🌴 Recharge", "Great job! Take a well-deserved extended break")
}

@Serializable
data class PomodoroParticipant(
    val userId: String,
    val userName: String,
    val avatarUrl: String? = null,
    val isHost: Boolean = false,
    val status: String = "Focusing 🎯",
    val joinedAt: Long = System.currentTimeMillis()
)

@Serializable
data class PomodoroRoomState(
    val roomId: String,
    val roomName: String,
    val roomCode: String = formatRoomCode(roomId),
    val phase: PomodoroPhase = PomodoroPhase.FOCUS,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val completedIntervals: Int = 0,
    val hostUserId: String = "",
    val participants: List<PomodoroParticipant> = emptyList()
) {
    companion object {
        fun formatRoomCode(rawId: String): String {
            val clean = rawId.filter { it.isLetterOrDigit() }.uppercase()
            val code = if (clean.length >= 5) clean.takeLast(5) else (clean + "EDU20").take(5)
            return "EDU-$code"
        }
    }
}

data class AmbientSound(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val description: String,
    val isProOnly: Boolean = false
)

data class PomodoroTheme(
    val id: String,
    val name: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val isProOnly: Boolean = false
)

enum class ShopCategory(val title: String, val iconEmoji: String) {
    THEMES("Themes", "🎨"),
    SOUNDS("Sounds", "🎧"),
    BADGES("Badges", "🏆"),
    BOOSTERS("Boosters", "⚡")
}

data class ShopItem(
    val id: String,
    val title: String,
    val category: ShopCategory,
    val iconEmoji: String,
    val description: String,
    val priceCoins: Int,
    val isProOnly: Boolean = false,
    val themeData: PomodoroTheme? = null,
    val badgeTitle: String? = null
)
