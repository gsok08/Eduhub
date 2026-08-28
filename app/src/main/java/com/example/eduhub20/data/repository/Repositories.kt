package com.example.eduhub20.data.repository

import android.util.Log
import com.example.eduhub20.data.SupabaseClientProvider
import com.example.eduhub20.data.SupabaseConfig
import com.example.eduhub20.data.ai.EduHubAiGenerator
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.model.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
data class ProfileDto(
    val id: String,
    val full_name: String,
    val updated_at: String = "2026-08-28T00:00:00Z"
)

@Serializable
data class StudyGroupDto(
    val id: String,
    val name: String,
    val host: String,
    val details: String,
    val current_members: Int = 1,
    val max_members: Int = 6,
    val category: String = "GROUP"
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val group_id: String,
    val sender_name: String,
    val sender_role: String,
    val message: String,
    val timestamp: String,
    val is_from_me: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Auth Repository
// ─────────────────────────────────────────────────────────────────────────────
object AuthRepository {
    private val _currentUser = MutableStateFlow<EduHubUser?>(null)
    val currentUser: StateFlow<EduHubUser?> = _currentUser.asStateFlow()

    fun restoreUser(user: EduHubUser) {
        _currentUser.value = user
        StudyGroupRepository.onUserSignedIn(user)
    }

    suspend fun signInAsLecturer(email: String, password: String): Result<EduHubUser> {
        val e = email.trim().lowercase()
        val p = password.trim()
        return if (e == SupabaseConfig.LECTURER_EMAIL.lowercase() && p == SupabaseConfig.LECTURER_PASSWORD) {
            val user = EduHubUser("lecturer_01", SupabaseConfig.LECTURER_EMAIL, "Teoh Li Wen (Lecturer)", UserRole.LECTURER)
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid lecturer email or password."))
        }
    }

    suspend fun signInAsStudent(email: String, password: String): Result<EduHubUser> {
        return try {
            SupabaseClientProvider.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password.trim()
            }
            val userObj = SupabaseClientProvider.auth.currentUserOrNull()
            val userId = userObj?.id ?: UUID.randomUUID().toString()
            val nameMeta = userObj?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
            val resolvedName = if (!nameMeta.isNullOrBlank()) {
                nameMeta
            } else {
                email.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
            }
            val user = EduHubUser(userId, email.trim(), resolvedName, UserRole.STUDENT)
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Login failed. Check your email and password."))
        }
    }

    suspend fun signUpStudent(email: String, password: String): Result<EduHubUser> {
        return try {
            val defaultName = email.substringBefore("@").replace(".", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
            SupabaseClientProvider.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = password.trim()
                this.data = buildJsonObject {
                    put("full_name", defaultName)
                }
            }
            val userObj = SupabaseClientProvider.auth.currentUserOrNull()
            val userId = userObj?.id ?: UUID.randomUUID().toString()
            val user = EduHubUser(userId, email.trim(), defaultName, UserRole.STUDENT)
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Sign-up failed."))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            SupabaseClientProvider.auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email."))
        }
    }

    suspend fun updateProfileName(newName: String) {
        val trimmed = newName.trim()
        val user = _currentUser.value ?: return

        try {
            SupabaseClientProvider.auth.updateUser {
                data = buildJsonObject {
                    put("full_name", trimmed)
                }
            }
        } catch (e: Exception) {}

        try {
            SupabaseClientProvider.postgrest.from("profiles").upsert(
                ProfileDto(id = user.id, full_name = trimmed)
            )
        } catch (e: Exception) {}

        _currentUser.value = user.copy(name = trimmed)
    }

    fun signOut() {
        try {} catch (e: Exception) {}
        _currentUser.value = null
        StudyGroupRepository.onUserSignOut()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Course Repository
// ─────────────────────────────────────────────────────────────────────────────
object CourseRepository {
    private val _courses = mutableListOf<Course>()
    private val _announcements = mutableListOf<Announcement>()

    fun getCourses(): List<Course> = _courses.toList()

    fun getCourseById(id: String): Course? =
        _courses.find { it.id.equals(id, true) || it.code.equals(id, true) }

    fun joinCourseWithCode(code: String): Result<Course> {
        val c = _courses.find { it.joinCode.equals(code.trim(), ignoreCase = true) }
        return if (c != null) Result.success(c)
        else Result.failure(Exception("Invalid join code \"${code.trim()}\". Please check with your lecturer."))
    }

    fun createCourse(code: String, title: String, lecturerName: String): Course {
        val upper = code.trim().uppercase()
        val joinCode = upper.replace(" ", "").take(3) + (100..999).random()
        val nc = Course(
            id = UUID.randomUUID().toString(),
            code = upper,
            title = title.trim(),
            lecturerName = lecturerName,
            joinCode = joinCode,
            iconCategory = if (upper.contains("CS") || upper.contains("IT") || upper.startsWith("AM")) "CODE" else "ENG",
            examDaysLeft = 30,
            progress = 0f
        )
        _courses.add(0, nc)
        return nc
    }

    fun getAnnouncementsForCourse(courseId: String): List<Announcement> =
        _announcements.filter { it.courseId == courseId }

    fun addAnnouncement(a: Announcement) = _announcements.add(0, a)

    fun updateAnnouncement(a: Announcement) {
        val idx = _announcements.indexOfFirst { it.id == a.id }
        if (idx != -1) _announcements[idx] = a
    }

    fun deleteAnnouncement(announcementId: String) {
        _announcements.removeAll { it.id == announcementId }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Note / Quiz Repository
// ─────────────────────────────────────────────────────────────────────────────
object NoteQuizRepository {
    private val _notes = mutableListOf<LectureNote>()
    private val _aiCache = mutableMapOf<String, AiGeneratedNote>()
    private val _quizHistory = mutableListOf<QuizHistoryItem>()

    fun getNotes(): List<LectureNote> = _notes.toList()
    fun getNoteById(id: String): LectureNote? = _notes.find { it.id == id }
    fun addLectureNote(n: LectureNote) { _notes.add(0, n) }

    fun updateLectureNote(note: LectureNote) {
        val idx = _notes.indexOfFirst { it.id == note.id }
        if (idx != -1) _notes[idx] = note
        _aiCache.remove(note.id)
    }

    fun deleteLectureNote(noteId: String) {
        _notes.removeAll { it.id == noteId }
        _aiCache.remove(noteId)
    }

    suspend fun getOrGenerateAiNote(note: LectureNote): AiGeneratedNote =
        _aiCache.getOrPut(note.id) { EduHubAiGenerator.generateNoteSummary(note) }

    fun getQuizHistory(): List<QuizHistoryItem> = _quizHistory.toList()

    fun recordQuizCompletion(noteId: String, courseCode: String, title: String, score: Int) {
        val safeTitle = if (title.isBlank()) "$courseCode Quiz" else title
        val item = QuizHistoryItem(UUID.randomUUID().toString(), noteId, courseCode, safeTitle, score, score >= 70)
        val idx = _quizHistory.indexOfFirst { it.noteId == noteId }
        if (idx != -1) _quizHistory[idx] = item else _quizHistory.add(0, item)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calendar Repository
// ─────────────────────────────────────────────────────────────────────────────
object CalendarRepository {
    private val _tasks = mutableListOf<CalendarTask>()
    private val _countdowns = mutableListOf<ExamCountdown>()

    fun getTasks(date: String = ""): List<CalendarTask> =
        if (date.isBlank()) _tasks.toList() else _tasks.filter { it.date == date }

    fun toggleTask(id: String) {
        val i = _tasks.indexOfFirst { it.id == id }
        if (i != -1) _tasks[i] = _tasks[i].copy(isCompleted = !_tasks[i].isCompleted)
    }

    fun addTask(title: String, date: String) {
        _tasks.add(CalendarTask(UUID.randomUUID().toString(), title, false, date))
    }

    fun getCountdowns(): List<ExamCountdown> = _countdowns.toList()

    fun addCountdown(cd: ExamCountdown) = _countdowns.add(0, cd)
}

// ─────────────────────────────────────────────────────────────────────────────
// Study Group Repository (User-Scoped Group Membership & Multi-Device Sync)
// ─────────────────────────────────────────────────────────────────────────────
object StudyGroupRepository {
    private val _groups = mutableListOf<StudyGroup>()
    private val _joinedGroupIds = mutableSetOf<String>()
    private val _members = mutableListOf<StudyRoomMember>()
    private val _messages = mutableMapOf<String, MutableList<ChatMessage>>()

    fun onUserSignedIn(user: EduHubUser) {
        _joinedGroupIds.clear()
        _joinedGroupIds.addAll(EduHubLocalStorage.loadJoinedGroupIds(user.id))
        _groups.clear()
        val local = EduHubLocalStorage.loadGroups()
        if (local.isNotEmpty()) {
            _groups.addAll(local.map { g ->
                val isHost = g.host.equals(user.name, true) || g.host.equals(user.email, true)
                g.copy(isJoined = isHost || _joinedGroupIds.contains(g.id))
            })
        }
    }

    fun onUserSignOut() {
        _joinedGroupIds.clear()
        _groups.clear()
        _messages.clear()
    }

    fun getGroups(): List<StudyGroup> {
        val user = AuthRepository.currentUser.value
        if (_groups.isEmpty() && user != null) {
            onUserSignedIn(user)
        }
        return _groups.toList()
    }

    suspend fun fetchGroupsFromSupabase(): List<StudyGroup> = withContext(Dispatchers.IO) {
        val currentUser = AuthRepository.currentUser.value
        val userId = currentUser?.id ?: "guest"
        _joinedGroupIds.clear()
        _joinedGroupIds.addAll(EduHubLocalStorage.loadJoinedGroupIds(userId))

        try {
            val dtoList = SupabaseClientProvider.postgrest.from("study_groups")
                .select()
                .decodeList<StudyGroupDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} groups from Supabase")

            val currentUserName = currentUser?.name ?: ""
            val currentUserEmail = currentUser?.email ?: ""

            val remoteMapped = dtoList.map { dto ->
                val isHost = (currentUserName.isNotBlank() && dto.host.equals(currentUserName, ignoreCase = true)) ||
                        (currentUserEmail.isNotBlank() && dto.host.equals(currentUserEmail, ignoreCase = true)) ||
                        (currentUserEmail.isNotBlank() && dto.host.equals(currentUserEmail.substringBefore("@"), ignoreCase = true))

                val isJoined = isHost || _joinedGroupIds.contains(dto.id)

                if (isJoined) {
                    _joinedGroupIds.add(dto.id)
                }

                StudyGroup(
                    id = dto.id,
                    name = dto.name,
                    host = dto.host,
                    details = dto.details,
                    currentMembers = dto.current_members,
                    maxMembers = dto.max_members,
                    isJoined = isJoined,
                    category = dto.category
                )
            }

            if (remoteMapped.isNotEmpty()) {
                val merged = (remoteMapped + _groups).distinctBy { it.id }
                _groups.clear()
                _groups.addAll(merged)
                EduHubLocalStorage.saveGroups(_groups.toList())
                if (currentUser != null) {
                    EduHubLocalStorage.saveJoinedGroupIds(currentUser.id, _joinedGroupIds)
                }
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch study_groups from Supabase: ${e.message}")
        }
        _groups.toList()
    }

    suspend fun joinGroup(groupId: String) = withContext(Dispatchers.IO) {
        val currentUser = AuthRepository.currentUser.value
        _joinedGroupIds.add(groupId)
        if (currentUser != null) {
            EduHubLocalStorage.saveJoinedGroupIds(currentUser.id, _joinedGroupIds)
        }

        val i = _groups.indexOfFirst { it.id == groupId }
        if (i != -1) {
            val updated = _groups[i].copy(isJoined = true, currentMembers = _groups[i].currentMembers + 1)
            _groups[i] = updated
            EduHubLocalStorage.saveGroups(_groups.toList())

            try {
                SupabaseClientProvider.postgrest.from("study_groups").update(
                    {
                        set("current_members", updated.currentMembers)
                    }
                ) {
                    filter { eq("id", groupId) }
                }
                Log.d("EduHubSupabase", "Updated member count for group $groupId in Supabase")
            } catch (e: Exception) {
                Log.e("EduHubSupabase", "Failed to update member count in Supabase: ${e.message}")
            }
        }
    }

    suspend fun createGroup(name: String, details: String, hostUser: EduHubUser?): StudyGroup = withContext(Dispatchers.IO) {
        val groupId = UUID.randomUUID().toString()
        val resolvedHost = if (hostUser != null && hostUser.name.isNotBlank() && hostUser.name != "Me") {
            hostUser.name
        } else if (hostUser != null && hostUser.email.isNotBlank()) {
            hostUser.email.substringBefore("@").replace(".", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
        } else {
            "Student"
        }

        _joinedGroupIds.add(groupId)
        if (hostUser != null) {
            EduHubLocalStorage.saveJoinedGroupIds(hostUser.id, _joinedGroupIds)
        }

        val g = StudyGroup(groupId, name, resolvedHost, details, 1, 6, true, "GROUP")
        _groups.add(0, g)
        EduHubLocalStorage.saveGroups(_groups.toList())

        val welcomeMsg = ChatMessage(
            UUID.randomUUID().toString(), groupId, "System", "Info",
            "Welcome to \"$name\"! Created by $resolvedHost.", "Just now", false
        )
        val msgList = _messages.getOrPut(groupId) { mutableListOf() }
        msgList.add(welcomeMsg)
        EduHubLocalStorage.saveChatMessages(groupId, msgList.toList())

        try {
            SupabaseClientProvider.postgrest.from("study_groups").insert(
                StudyGroupDto(
                    id = groupId,
                    name = name,
                    host = resolvedHost,
                    details = details,
                    current_members = 1,
                    max_members = 6,
                    category = "GROUP"
                )
            )
            Log.d("EduHubSupabase", "Successfully inserted study group '$name' (Host: $resolvedHost) into Supabase")

            SupabaseClientProvider.postgrest.from("chat_messages").insert(
                ChatMessageDto(
                    id = welcomeMsg.id,
                    group_id = groupId,
                    sender_name = welcomeMsg.senderName,
                    sender_role = welcomeMsg.senderRole,
                    message = welcomeMsg.message,
                    timestamp = welcomeMsg.timestamp,
                    is_from_me = false
                )
            )
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to insert study group to Supabase: ${e.message}")
        }

        g
    }

    fun getStudyRoomMembers(): List<StudyRoomMember> = _members.toList()

    fun getChatMessages(groupId: String): List<ChatMessage> {
        val inMem = _messages[groupId]
        if (inMem != null && inMem.isNotEmpty()) return inMem.toList()
        val local = EduHubLocalStorage.loadChatMessages(groupId)
        if (local.isNotEmpty()) {
            _messages[groupId] = local.toMutableList()
            return local
        }
        return emptyList()
    }

    suspend fun fetchChatMessages(groupId: String, currentUserName: String = "Me"): List<ChatMessage> = withContext(Dispatchers.IO) {
        val local = EduHubLocalStorage.loadChatMessages(groupId)
        if (local.isNotEmpty()) {
            _messages[groupId] = local.toMutableList()
        }

        try {
            val dtoList = SupabaseClientProvider.postgrest.from("chat_messages")
                .select {
                    filter { eq("group_id", groupId) }
                }
                .decodeList<ChatMessageDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} messages for group $groupId from Supabase")

            val remoteMapped = dtoList.map { dto ->
                ChatMessage(
                    id = dto.id,
                    groupId = dto.group_id,
                    senderName = dto.sender_name,
                    senderRole = dto.sender_role,
                    message = dto.message,
                    timestamp = dto.timestamp,
                    isFromMe = dto.sender_name.equals(currentUserName, ignoreCase = true) || dto.is_from_me
                )
            }
            if (remoteMapped.isNotEmpty()) {
                val merged = (remoteMapped + (_messages[groupId] ?: emptyList())).distinctBy { it.id }
                _messages[groupId] = merged.toMutableList()
                EduHubLocalStorage.saveChatMessages(groupId, merged)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch chat_messages from Supabase: ${e.message}")
        }

        _messages.getOrPut(groupId) { mutableListOf() }.toList()
    }

    suspend fun sendMessage(groupId: String, text: String, senderName: String = "Me", senderRole: String = "Student") = withContext(Dispatchers.IO) {
        val now = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val msgId = UUID.randomUUID().toString()
        val msg = ChatMessage(msgId, groupId, senderName, senderRole, text, now, true)

        val list = _messages.getOrPut(groupId) { mutableListOf() }
        list.add(msg)
        EduHubLocalStorage.saveChatMessages(groupId, list.toList())

        try {
            SupabaseClientProvider.postgrest.from("chat_messages").insert(
                ChatMessageDto(
                    id = msgId,
                    group_id = groupId,
                    sender_name = senderName,
                    sender_role = senderRole,
                    message = text,
                    timestamp = now,
                    is_from_me = true
                )
            )
            Log.d("EduHubSupabase", "Sent chat message to Supabase for group $groupId")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to send message to Supabase: ${e.message}")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Past Year Paper Repository
// ─────────────────────────────────────────────────────────────────────────────
object PastYearRepository {
    private val _papers = mutableListOf<PastYearPaper>()

    fun addPaper(paper: PastYearPaper) = _papers.add(0, paper)

    fun searchPapers(query: String, subjectFilter: String, yearFilter: String): List<PastYearPaper> =
        _papers.filter { p ->
            val q = query.isBlank() || p.courseCode.contains(query, true) ||
                    p.courseTitle.contains(query, true) || p.session.contains(query, true)
            val s = subjectFilter == "All" || p.subjectCategory.contains(subjectFilter, true)
            val y = yearFilter == "All" || p.year.contains(yearFilter, true)
            q && s && y
        }
}