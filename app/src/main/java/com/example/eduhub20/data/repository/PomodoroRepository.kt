package com.example.eduhub20.data.repository

import android.content.Context
import android.util.Log
import com.example.eduhub20.data.model.AmbientSound
import com.example.eduhub20.data.model.EduHubUser
import com.example.eduhub20.data.model.PomodoroParticipant
import com.example.eduhub20.data.model.PomodoroPhase
import com.example.eduhub20.data.model.PomodoroRoomState
import com.example.eduhub20.data.model.PomodoroTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PomodoroRepository {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val roomStates = mutableMapOf<String, MutableStateFlow<PomodoroRoomState>>()
    private val roomJobs = mutableMapOf<String, Job>()

    // StudyCoins persistence in SharedPreferences
    private const val PREFS_NAME = "eduhub_pomodoro_prefs"
    private const val KEY_COINS_PREFIX = "coins_"
    private const val KEY_STREAK_PREFIX = "streak_"
    private const val KEY_MINUTES_PREFIX = "focus_minutes_"
    private const val KEY_PRO_PREFIX = "is_pro_"
    private const val KEY_PURCHASED_PREFIX = "purchased_"
    private const val KEY_EQUIPPED_THEME_PREFIX = "equipped_theme_"
    private const val KEY_EQUIPPED_BADGE_PREFIX = "equipped_badge_"
    private const val KEY_ACTIVE_BOOSTERS_PREFIX = "active_boosters_"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isProUser(userId: String): Boolean {
        val ctx = appContext ?: return false
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PRO_PREFIX + userId, false)
    }

    fun setProUser(userId: String, isPro: Boolean = true) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PRO_PREFIX + userId, isPro).apply()
    }

    fun getStudyCoins(userId: String): Int {
        val ctx = appContext ?: return 120 // Default starting coins if context not yet bound
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COINS_PREFIX + userId, 120)
    }

    fun addStudyCoins(userId: String, amount: Int): Int {
        val ctx = appContext ?: return 120 + amount
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_COINS_PREFIX + userId, 120)
        val updated = (current + amount).coerceAtLeast(0)
        prefs.edit().putInt(KEY_COINS_PREFIX + userId, updated).apply()
        return updated
    }

    fun getPurchasedItemIds(userId: String): Set<String> {
        val ctx = appContext ?: return setOf("theme_classic", "sound_none", "sound_rain", "sound_cafe")
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_PURCHASED_PREFIX + userId, null)
        val defaults = setOf("theme_classic", "sound_none", "sound_rain", "sound_cafe")
        return if (saved == null) defaults else (saved + defaults)
    }

    fun isItemPurchased(userId: String, itemId: String): Boolean {
        if (isProUser(userId)) return true
        return getPurchasedItemIds(userId).contains(itemId)
    }

    fun purchaseItem(userId: String, itemId: String, priceCoins: Int): Boolean {
        val ctx = appContext ?: return false
        val currentCoins = getStudyCoins(userId)
        if (currentCoins < priceCoins) return false

        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updatedCoins = currentCoins - priceCoins
        val currentPurchased = getPurchasedItemIds(userId).toMutableSet()
        currentPurchased.add(itemId)

        prefs.edit()
            .putInt(KEY_COINS_PREFIX + userId, updatedCoins)
            .putStringSet(KEY_PURCHASED_PREFIX + userId, currentPurchased)
            .apply()
        return true
    }

    fun getEquippedThemeId(userId: String): String {
        val ctx = appContext ?: return "theme_classic"
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EQUIPPED_THEME_PREFIX + userId, "theme_classic") ?: "theme_classic"
    }

    fun setEquippedThemeId(userId: String, themeId: String) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_EQUIPPED_THEME_PREFIX + userId, themeId).apply()
    }

    fun getEquippedTheme(userId: String): PomodoroTheme {
        val themeId = getEquippedThemeId(userId)
        return getThemes().find { it.id == themeId } ?: getThemes().first()
    }

    fun getEquippedBadge(userId: String): String? {
        val ctx = appContext ?: return null
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_EQUIPPED_BADGE_PREFIX + userId, null)
    }

    fun setEquippedBadge(userId: String, badgeTitle: String?) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (badgeTitle == null) {
            prefs.edit().remove(KEY_EQUIPPED_BADGE_PREFIX + userId).apply()
        } else {
            prefs.edit().putString(KEY_EQUIPPED_BADGE_PREFIX + userId, badgeTitle).apply()
        }
    }

    fun hasActiveBooster(userId: String, boosterId: String): Boolean {
        val ctx = appContext ?: return false
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val boosters = prefs.getStringSet(KEY_ACTIVE_BOOSTERS_PREFIX + userId, emptySet()) ?: emptySet()
        return boosters.contains(boosterId)
    }

    fun activateBooster(userId: String, boosterId: String) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_ACTIVE_BOOSTERS_PREFIX + userId, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(boosterId)
        prefs.edit().putStringSet(KEY_ACTIVE_BOOSTERS_PREFIX + userId, current).apply()
    }

    fun getDailyStreak(userId: String): Int {
        val ctx = appContext ?: return 3
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_STREAK_PREFIX + userId, 3)
    }

    fun getTotalFocusMinutes(userId: String): Int {
        val ctx = appContext ?: return 75
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_MINUTES_PREFIX + userId, 75)
    }

    private fun recordFocusMinutes(userId: String, minutes: Int) {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_MINUTES_PREFIX + userId, 75)
        prefs.edit().putInt(KEY_MINUTES_PREFIX + userId, current + minutes).apply()
    }

    fun createNewFocusRoom(customName: String? = null, user: EduHubUser? = null): Pair<String, String> {
        val randomTail = java.util.UUID.randomUUID().toString().filter { it.isLetterOrDigit() }.takeLast(5).uppercase()
        val roomId = "focus-$randomTail"
        val roomCode = PomodoroRoomState.formatRoomCode(roomId)
        val resolvedName = if (!customName.isNullOrBlank()) customName.trim() else "Focus Room $roomCode"
        getRoomState(roomId, resolvedName, user)
        return Pair(roomId, roomCode)
    }

    fun resolveRoomIdByCode(input: String): String {
        val clean = input.trim()
        val shortClean = clean.removePrefix("EDU-").removePrefix("edu-").filter { it.isLetterOrDigit() }.uppercase()
        val match = roomStates.keys.find { rId ->
            rId.equals(clean, ignoreCase = true) ||
            PomodoroRoomState.formatRoomCode(rId).equals(clean, ignoreCase = true) ||
            (shortClean.isNotBlank() && rId.filter { it.isLetterOrDigit() }.takeLast(5).equals(shortClean, ignoreCase = true))
        }
        if (match != null) return match
        if (shortClean.length == 5) {
            return "focus-$shortClean"
        }
        if (clean.isNotBlank()) {
            return clean
        }
        return "focus-${java.util.UUID.randomUUID().toString().filter { it.isLetterOrDigit() }.takeLast(5).uppercase()}"
    }

    fun getRoomState(roomId: String, roomName: String = "Focus Room", user: EduHubUser? = null): StateFlow<PomodoroRoomState> {
        val flow = roomStates.getOrPut(roomId) {
            val initialHostId = user?.id ?: ""
            val initialParticipants = if (user != null) {
                listOf(
                    PomodoroParticipant(
                        userId = user.id,
                        userName = user.name,
                        avatarUrl = user.avatarUrl,
                        isHost = true,
                        status = "Focusing 🎯"
                    )
                )
            } else emptyList()

            MutableStateFlow(
                PomodoroRoomState(
                    roomId = roomId,
                    roomName = roomName,
                    phase = PomodoroPhase.FOCUS,
                    remainingSeconds = 25 * 60,
                    totalSeconds = 25 * 60,
                    isRunning = false,
                    completedIntervals = 0,
                    hostUserId = initialHostId,
                    participants = initialParticipants
                )
            )
        }
        return flow.asStateFlow()
    }

    fun joinRoom(roomId: String, roomName: String = "Focus Room", user: EduHubUser?) {
        if (user == null) return
        val flow = roomStates.getOrPut(roomId) {
            MutableStateFlow(
                PomodoroRoomState(
                    roomId = roomId,
                    roomName = roomName,
                    hostUserId = user.id,
                    participants = emptyList()
                )
            )
        }

        val current = flow.value
        val existing = current.participants.find { it.userId == user.id }
        if (existing == null) {
            val isFirstMember = current.participants.isEmpty()
            val newParticipant = PomodoroParticipant(
                userId = user.id,
                userName = user.name,
                avatarUrl = user.avatarUrl,
                isHost = isFirstMember || current.hostUserId == user.id,
                status = if (current.phase == PomodoroPhase.FOCUS) "Focusing 🎯" else "Resting ☕"
            )
            flow.value = current.copy(
                participants = current.participants + newParticipant,
                hostUserId = if (isFirstMember) user.id else current.hostUserId
            )
        }
    }

    fun leaveRoom(roomId: String, userId: String) {
        val flow = roomStates[roomId] ?: return
        val current = flow.value
        val updatedParticipants = current.participants.filter { it.userId != userId }
        val newHostId = if (current.hostUserId == userId && updatedParticipants.isNotEmpty()) {
            updatedParticipants.first().userId
        } else {
            current.hostUserId
        }
        flow.value = current.copy(
            participants = updatedParticipants,
            hostUserId = newHostId
        )
    }

    fun startTimer(roomId: String) {
        val flow = roomStates[roomId] ?: return
        if (flow.value.isRunning && roomJobs[roomId]?.isActive == true) return

        flow.value = flow.value.copy(isRunning = true)
        launchTimerJob(roomId)
    }

    private fun launchTimerJob(roomId: String) {
        val flow = roomStates[roomId] ?: return
        roomJobs[roomId]?.cancel()
        roomJobs[roomId] = scope.launch {
            while (isActive) {
                delay(1000L)
                val current = flow.value
                if (!current.isRunning) break

                if (current.remainingSeconds > 1) {
                    flow.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
                } else {
                    // Interval finished! Transition to next phase and automatically continue counting down
                    advancePhase(roomId, autoStart = true)
                    break
                }
            }
        }
    }

    fun pauseTimer(roomId: String) {
        val flow = roomStates[roomId] ?: return
        roomJobs[roomId]?.cancel()
        roomJobs.remove(roomId)
        flow.value = flow.value.copy(isRunning = false)
    }

    fun resetTimer(roomId: String) {
        val flow = roomStates[roomId] ?: return
        pauseTimer(roomId)
        val current = flow.value
        flow.value = current.copy(
            isRunning = false,
            remainingSeconds = current.phase.defaultMinutes * 60,
            totalSeconds = current.phase.defaultMinutes * 60
        )
    }

    fun switchPhase(roomId: String, newPhase: PomodoroPhase) {
        val flow = roomStates[roomId] ?: return
        pauseTimer(roomId)
        val current = flow.value
        flow.value = current.copy(
            isRunning = false,
            phase = newPhase,
            remainingSeconds = newPhase.defaultMinutes * 60,
            totalSeconds = newPhase.defaultMinutes * 60,
            participants = current.participants.map { p ->
                p.copy(status = if (newPhase == PomodoroPhase.FOCUS) "Focusing 🎯" else "Resting ☕")
            }
        )
    }

    fun skipPhase(roomId: String) {
        advancePhase(roomId, autoStart = true)
    }

    private fun advancePhase(roomId: String, autoStart: Boolean = true) {
        val flow = roomStates[roomId] ?: return
        val current = flow.value

        // Cancel previous job cleanly
        roomJobs[roomId]?.cancel()
        roomJobs.remove(roomId)

        val nextPhase: PomodoroPhase
        val newIntervalCount: Int

        when (current.phase) {
            PomodoroPhase.FOCUS -> {
                newIntervalCount = current.completedIntervals + 1

                // Award coins & log focus minutes for participants
                for (p in current.participants) {
                    val multiplier = if (hasActiveBooster(p.userId, "booster_2x")) 2 else 1
                    addStudyCoins(p.userId, 25 * multiplier)
                    recordFocusMinutes(p.userId, 25)
                }

                nextPhase = if (newIntervalCount % 4 == 0) {
                    // Long break milestone! Bonus 50 coins
                    for (p in current.participants) {
                        val multiplier = if (hasActiveBooster(p.userId, "booster_2x")) 2 else 1
                        addStudyCoins(p.userId, 50 * multiplier)
                    }
                    PomodoroPhase.LONG_BREAK
                } else {
                    PomodoroPhase.SHORT_BREAK
                }
            }
            PomodoroPhase.SHORT_BREAK, PomodoroPhase.LONG_BREAK -> {
                newIntervalCount = current.completedIntervals
                nextPhase = PomodoroPhase.FOCUS
            }
        }

        flow.value = current.copy(
            isRunning = autoStart,
            phase = nextPhase,
            completedIntervals = newIntervalCount,
            remainingSeconds = nextPhase.defaultMinutes * 60,
            totalSeconds = nextPhase.defaultMinutes * 60,
            participants = current.participants.map {
                it.copy(status = if (nextPhase == PomodoroPhase.FOCUS) "Focusing 🎯" else "Resting ☕")
            }
        )

        if (autoStart) {
            launchTimerJob(roomId)
        }
    }

    fun setParticipantStatus(roomId: String, userId: String, status: String) {
        val flow = roomStates[roomId] ?: return
        val current = flow.value
        flow.value = current.copy(
            participants = current.participants.map {
                if (it.userId == userId) it.copy(status = status) else it
            }
        )
    }

    fun renameRoom(roomId: String, newName: String) {
        val flow = roomStates[roomId] ?: return
        val current = flow.value
        flow.value = current.copy(roomName = newName.trim())
    }

    fun getAmbientSounds(): List<AmbientSound> = listOf(
        AmbientSound("sound_none", "Silence", "🔇", "Pure quiet concentration"),
        AmbientSound("sound_rain", "Tokyo Rain", "🌧️", "Gentle rain hitting classroom glass window"),
        AmbientSound("sound_cafe", "Lo-Fi Cafe", "☕", "Subtle coffee shop ambience with soft piano"),
        AmbientSound("sound_binaural", "Alpha Waves", "🧠", "14Hz deep memory frequency"),
        AmbientSound("sound_forest", "Forest Birds", "🌲", "Calm wind and chirping birds"),
        AmbientSound("sound_whitenoise", "White Noise", "📻", "Smooth static blocking room chatter"),
        AmbientSound("sound_campfire", "Campfire", "🔥", "Warm crackling embers")
    )

    fun getThemes(): List<PomodoroTheme> = listOf(
        PomodoroTheme("theme_classic", "EduHub Classic", 0xFF2563EB, 0xFF60A5FA),
        PomodoroTheme("theme_emerald", "Emerald Zen", 0xFF059669, 0xFF34D399),
        PomodoroTheme("theme_sunset", "Sunset Crimson", 0xFFDC2626, 0xFFF87171),
        PomodoroTheme("theme_cyber", "Cyber Neon", 0xFF7C3AED, 0xFFC084FC),
        PomodoroTheme("theme_gold", "Golden Champion", 0xFFD97706, 0xFFFBBF24)
    )

    fun getShopCatalog(): List<com.example.eduhub20.data.model.ShopItem> = listOf(
        // Themes
        com.example.eduhub20.data.model.ShopItem(
            id = "theme_classic",
            title = "EduHub Classic",
            category = com.example.eduhub20.data.model.ShopCategory.THEMES,
            iconEmoji = "🔵",
            description = "Default crisp blue EduHub focus palette",
            priceCoins = 0,
            themeData = PomodoroTheme("theme_classic", "EduHub Classic", 0xFF2563EB, 0xFF60A5FA)
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "theme_emerald",
            title = "Emerald Zen",
            category = com.example.eduhub20.data.model.ShopCategory.THEMES,
            iconEmoji = "🌿",
            description = "Calming botanical forest emerald theme",
            priceCoins = 40,
            themeData = PomodoroTheme("theme_emerald", "Emerald Zen", 0xFF059669, 0xFF34D399)
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "theme_sunset",
            title = "Sunset Crimson",
            category = com.example.eduhub20.data.model.ShopCategory.THEMES,
            iconEmoji = "🌅",
            description = "Warm aesthetic sunset dusk gradients",
            priceCoins = 50,
            themeData = PomodoroTheme("theme_sunset", "Sunset Crimson", 0xFFDC2626, 0xFFF87171)
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "theme_cyber",
            title = "Cyber Neon",
            category = com.example.eduhub20.data.model.ShopCategory.THEMES,
            iconEmoji = "⚡",
            description = "Futuristic cyberpunk neon purple glow",
            priceCoins = 60,
            themeData = PomodoroTheme("theme_cyber", "Cyber Neon", 0xFF7C3AED, 0xFFC084FC)
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "theme_gold",
            title = "Golden Champion",
            category = com.example.eduhub20.data.model.ShopCategory.THEMES,
            iconEmoji = "🏆",
            description = "Prestige metallic amber & gold aura",
            priceCoins = 80,
            themeData = PomodoroTheme("theme_gold", "Golden Champion", 0xFFD97706, 0xFFFBBF24)
        ),

        // Sounds
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_none",
            title = "Pure Silence",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "🔇",
            description = "Zero noise uninterrupted study flow",
            priceCoins = 0
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_rain",
            title = "Tokyo Rain",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "🌧️",
            description = "Gentle droplets on classroom windows",
            priceCoins = 0
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_cafe",
            title = "Lo-Fi Cafe",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "☕",
            description = "Warm acoustic study lounge coffee vibes",
            priceCoins = 0
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_binaural",
            title = "Binaural Alpha Waves",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "🧠",
            description = "14Hz frequency tuned for deep memory retention",
            priceCoins = 35
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_forest",
            title = "Forest Birdsong",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "🌲",
            description = "Peaceful nature breeze and soft chirping",
            priceCoins = 35
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_whitenoise",
            title = "Deep White Noise",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "📻",
            description = "Blocks background dorm and campus noise",
            priceCoins = 30
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "sound_campfire",
            title = "Campfire Crackle",
            category = com.example.eduhub20.data.model.ShopCategory.SOUNDS,
            iconEmoji = "🔥",
            description = "Cozy fireside embers for evening sessions",
            priceCoins = 30
        ),

        // Badges / Titles
        com.example.eduhub20.data.model.ShopItem(
            id = "badge_scholar",
            title = "Focus Scholar",
            category = com.example.eduhub20.data.model.ShopCategory.BADGES,
            iconEmoji = "🎓",
            description = "Displays on your Profile & Squad roster",
            priceCoins = 45,
            badgeTitle = "🎓 Focus Scholar"
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "badge_speed",
            title = "Speed Learner",
            category = com.example.eduhub20.data.model.ShopCategory.BADGES,
            iconEmoji = "⚡",
            description = "Proves your rapid problem-solving drive",
            priceCoins = 50,
            badgeTitle = "⚡ Speed Learner"
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "badge_owl",
            title = "Night Owl",
            category = com.example.eduhub20.data.model.ShopCategory.BADGES,
            iconEmoji = "🦉",
            description = "Dedicated badge for late-night study masters",
            priceCoins = 60,
            badgeTitle = "🦉 Night Owl"
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "badge_legend",
            title = "Focus Legend",
            category = com.example.eduhub20.data.model.ShopCategory.BADGES,
            iconEmoji = "🔥",
            description = "Ultimate gold prestige title badge",
            priceCoins = 80,
            badgeTitle = "🔥 Focus Legend"
        ),

        // Boosters
        com.example.eduhub20.data.model.ShopItem(
            id = "booster_streak",
            title = "Streak Saver Shield",
            category = com.example.eduhub20.data.model.ShopCategory.BOOSTERS,
            iconEmoji = "🛡️",
            description = "Safeguards your study streak if you miss a day",
            priceCoins = 40
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "booster_2x",
            title = "2x Coin Multiplier",
            category = com.example.eduhub20.data.model.ShopCategory.BOOSTERS,
            iconEmoji = "☕",
            description = "Doubles all StudyCoin rewards on completed cycles",
            priceCoins = 50
        ),
        com.example.eduhub20.data.model.ShopItem(
            id = "booster_ai",
            title = "AI Exam Hint Pass",
            category = com.example.eduhub20.data.model.ShopCategory.BOOSTERS,
            iconEmoji = "💡",
            description = "Grants 5 bonus smart AI exam hints in flashcards",
            priceCoins = 30
        )
    )
}
