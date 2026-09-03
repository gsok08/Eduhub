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
        val updated = current + amount
        prefs.edit().putInt(KEY_COINS_PREFIX + userId, updated).apply()
        return updated
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
                    addStudyCoins(p.userId, 25)
                    recordFocusMinutes(p.userId, 25)
                }

                nextPhase = if (newIntervalCount % 4 == 0) {
                    // Long break milestone! Bonus 50 coins
                    for (p in current.participants) {
                        addStudyCoins(p.userId, 50)
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
        AmbientSound("none", "Silence", "🔇", "Pure quiet concentration"),
        AmbientSound("rain", "Tokyo Rain", "🌧️", "Gentle rain hitting classroom glass window"),
        AmbientSound("cafe", "Lo-Fi Cafe", "☕", "Subtle coffee shop ambience with soft piano"),
        AmbientSound("forest", "Forest Birdsong", "🌲", "Calm wind and chirping birds in nature", isProOnly = true),
        AmbientSound("white_noise", "Deep White Noise", "📻", "Smooth static blocking out all room chatter", isProOnly = true)
    )

    fun getThemes(): List<PomodoroTheme> = listOf(
        PomodoroTheme("navy", "EduHub Classic", 0xFF1E3A8A, 0xFF3B82F6),
        PomodoroTheme("forest", "Zen Forest", 0xFF064E3B, 0xFF10B981, isProOnly = true),
        PomodoroTheme("sunset", "Sunset Crimson", 0xFF831843, 0xFFF43F5E, isProOnly = true),
        PomodoroTheme("cyber", "Cyberpunk Neon", 0xFF18181B, 0xFFA855F7, isProOnly = true)
    )
}
