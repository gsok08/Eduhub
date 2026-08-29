package com.example.eduhub20.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.eduhub20.data.SupabaseClientProvider
import com.example.eduhub20.data.SupabaseConfig
import com.example.eduhub20.data.ai.EduHubAiGenerator
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.model.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
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
    val role: String = "STUDENT",
    val updated_at: String = "2026-08-29T00:00:00Z"
)

@Serializable
data class CourseDto(
    val id: String,
    val code: String,
    val title: String,
    val lecturer_name: String,
    val join_code: String,
    val icon_category: String = "CODE",
    val exam_days_left: Int = 30,
    val progress: Float = 0.0f
)

@Serializable
data class AnnouncementDto(
    val id: String,
    val course_id: String,
    val lecturer_name: String,
    val date: String,
    val title: String,
    val content: String
)

@Serializable
data class LectureNoteDto(
    val id: String,
    val course_code: String,
    val course_title: String,
    val semester_period: String,
    val chapter_title: String,
    val raw_content: String,
    val pdf_file_name: String? = null,
    val pdf_url: String? = null
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
// Auth Repository (Full Supabase Cloud Auth & Role Profiles)
// ─────────────────────────────────────────────────────────────────────────────
object AuthRepository {
    private val _currentUser = MutableStateFlow<EduHubUser?>(null)
    val currentUser: StateFlow<EduHubUser?> = _currentUser.asStateFlow()

    fun restoreUser(user: EduHubUser) {
        _currentUser.value = user
        StudyGroupRepository.onUserSignedIn(user)
    }

    suspend fun signInAsLecturer(email: String, password: String): Result<EduHubUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        // 1. Try Supabase Auth login
        try {
            SupabaseClientProvider.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = trimmedPassword
            }

            val userObj = SupabaseClientProvider.auth.currentUserOrNull()
            val userId = userObj?.id ?: UUID.randomUUID().toString()

            // Fetch profile from Supabase profiles table
            var resolvedName: String? = null
            var resolvedRole = UserRole.LECTURER

            try {
                val profile = SupabaseClientProvider.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                if (profile != null) {
                    resolvedName = profile.full_name
                }
            } catch (e: Exception) {}

            if (resolvedName.isNullOrBlank()) {
                val metaName = userObj?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                resolvedName = if (!metaName.isNullOrBlank()) {
                    metaName
                } else {
                    trimmedEmail.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } } + " (Lecturer)"
                }
                // Save to profiles
                try {
                    SupabaseClientProvider.postgrest.from("profiles").upsert(
                        ProfileDto(id = userId, full_name = resolvedName, role = "LECTURER")
                    )
                } catch (e: Exception) {}
            }

            val user = EduHubUser(userId, trimmedEmail, resolvedName, resolvedRole)
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            return@withContext Result.success(user)
        } catch (e: Exception) {
            // Fallback for preset lecturer login if offline
            if (trimmedEmail.lowercase() == SupabaseConfig.LECTURER_EMAIL.lowercase() && trimmedPassword == SupabaseConfig.LECTURER_PASSWORD) {
                val user = EduHubUser("lecturer_preset_01", SupabaseConfig.LECTURER_EMAIL, "Teoh Li Wen (Lecturer)", UserRole.LECTURER)
                _currentUser.value = user
                StudyGroupRepository.onUserSignedIn(user)
                return@withContext Result.success(user)
            }
            return@withContext Result.failure(Exception(e.localizedMessage ?: "Invalid lecturer credentials."))
        }
    }

    suspend fun signInAsStudent(email: String, password: String): Result<EduHubUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        try {
            SupabaseClientProvider.auth.signInWith(Email) {
                this.email = trimmedEmail
                this.password = trimmedPassword
            }
            val userObj = SupabaseClientProvider.auth.currentUserOrNull()
            val userId = userObj?.id ?: UUID.randomUUID().toString()

            var resolvedName: String? = null
            try {
                val profile = SupabaseClientProvider.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                if (profile != null) {
                    resolvedName = profile.full_name
                }
            } catch (e: Exception) {}

            if (resolvedName.isNullOrBlank()) {
                val metaName = userObj?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                resolvedName = if (!metaName.isNullOrBlank()) {
                    metaName
                } else {
                    trimmedEmail.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
                }
                try {
                    SupabaseClientProvider.postgrest.from("profiles").upsert(
                        ProfileDto(id = userId, full_name = resolvedName, role = "STUDENT")
                    )
                } catch (e: Exception) {}
            }

            val user = EduHubUser(userId, trimmedEmail, resolvedName, UserRole.STUDENT)
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Login failed. Check your email and password."))
        }
    }

    suspend fun signUpStudent(email: String, password: String): Result<EduHubUser> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        try {
            val defaultName = trimmedEmail.substringBefore("@").replace(".", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
            SupabaseClientProvider.auth.signUpWith(Email) {
                this.email = trimmedEmail
                this.password = trimmedPassword
                this.data = buildJsonObject {
                    put("full_name", defaultName)
                    put("role", "STUDENT")
                }
            }
            val userObj = SupabaseClientProvider.auth.currentUserOrNull()
            val userId = userObj?.id ?: UUID.randomUUID().toString()
            val user = EduHubUser(userId, trimmedEmail, defaultName, UserRole.STUDENT)

            try {
                SupabaseClientProvider.postgrest.from("profiles").upsert(
                    ProfileDto(id = userId, full_name = defaultName, role = "STUDENT")
                )
            } catch (e: Exception) {}

            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Sign-up failed."))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClientProvider.auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Failed to send reset email."))
        }
    }

    suspend fun updateProfileName(newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        val user = _currentUser.value ?: return@withContext

        try {
            SupabaseClientProvider.auth.updateUser {
                data = buildJsonObject {
                    put("full_name", trimmed)
                }
            }
        } catch (e: Exception) {}

        try {
            SupabaseClientProvider.postgrest.from("profiles").upsert(
                ProfileDto(id = user.id, full_name = trimmed, role = user.role.name)
            )
        } catch (e: Exception) {}

        _currentUser.value = user.copy(name = trimmed)
    }

    fun signOut() {
        try {
            // Non-blocking sign out
        } catch (e: Exception) {}
        _currentUser.value = null
        StudyGroupRepository.onUserSignOut()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Course Repository (Supabase Cloud Sync for Courses & Announcements)
// ─────────────────────────────────────────────────────────────────────────────
object CourseRepository {
    private val _courses = mutableListOf<Course>()
    private val _announcements = mutableListOf<Announcement>()

    fun getCourses(): List<Course> = _courses.toList()

    fun getCourseById(id: String): Course? =
        _courses.find { it.id.equals(id, true) || it.code.equals(id, true) }

    suspend fun fetchCoursesFromSupabase(): List<Course> = withContext(Dispatchers.IO) {
        try {
            val dtoList = SupabaseClientProvider.postgrest.from("courses")
                .select()
                .decodeList<CourseDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} courses from Supabase")

            val mapped = dtoList.map { dto ->
                Course(
                    id = dto.id,
                    code = dto.code,
                    title = dto.title,
                    lecturerName = dto.lecturer_name,
                    joinCode = dto.join_code,
                    iconCategory = dto.icon_category,
                    examDaysLeft = dto.exam_days_left,
                    progress = dto.progress
                )
            }
            if (mapped.isNotEmpty()) {
                val merged = (mapped + _courses).distinctBy { it.id }
                _courses.clear()
                _courses.addAll(merged)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch courses: ${e.message}")
        }
        _courses.toList()
    }

    fun joinCourseWithCode(code: String): Result<Course> {
        val c = _courses.find { it.joinCode.equals(code.trim(), ignoreCase = true) }
        return if (c != null) Result.success(c)
        else Result.failure(Exception("Invalid join code \"${code.trim()}\". Please check with your lecturer."))
    }

    suspend fun createCourse(code: String, title: String, lecturerName: String): Course = withContext(Dispatchers.IO) {
        val upper = code.trim().uppercase()
        val joinCode = upper.replace(" ", "").take(3) + (100..999).random()
        val courseId = UUID.randomUUID().toString()
        val iconCat = if (upper.contains("CS") || upper.contains("IT") || upper.startsWith("AM")) "CODE" else "ENG"

        val nc = Course(
            id = courseId,
            code = upper,
            title = title.trim(),
            lecturerName = lecturerName.trim(),
            joinCode = joinCode,
            iconCategory = iconCat,
            examDaysLeft = 30,
            progress = 0f
        )
        _courses.add(0, nc)

        try {
            SupabaseClientProvider.postgrest.from("courses").insert(
                CourseDto(
                    id = courseId,
                    code = nc.code,
                    title = nc.title,
                    lecturer_name = nc.lecturerName,
                    join_code = nc.joinCode,
                    icon_category = nc.iconCategory,
                    exam_days_left = nc.examDaysLeft,
                    progress = nc.progress
                )
            )
            Log.d("EduHubSupabase", "Inserted course '${nc.code}' into Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to insert course to Supabase: ${e.message}")
        }
        nc
    }

    fun getAnnouncementsForCourse(courseId: String): List<Announcement> =
        _announcements.filter { it.courseId == courseId }

    suspend fun fetchAnnouncementsFromSupabase(courseId: String): List<Announcement> = withContext(Dispatchers.IO) {
        try {
            val dtoList = SupabaseClientProvider.postgrest.from("announcements")
                .select { filter { eq("course_id", courseId) } }
                .decodeList<AnnouncementDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} announcements for course $courseId")

            val mapped = dtoList.map { dto ->
                Announcement(
                    id = dto.id,
                    courseId = dto.course_id,
                    lecturerName = dto.lecturer_name,
                    date = dto.date,
                    title = dto.title,
                    content = dto.content
                )
            }
            if (mapped.isNotEmpty()) {
                val merged = (mapped + _announcements).distinctBy { it.id }
                _announcements.clear()
                _announcements.addAll(merged)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch announcements: ${e.message}")
        }
        getAnnouncementsForCourse(courseId)
    }

    suspend fun addAnnouncement(a: Announcement) = withContext(Dispatchers.IO) {
        _announcements.add(0, a)
        try {
            SupabaseClientProvider.postgrest.from("announcements").insert(
                AnnouncementDto(
                    id = a.id,
                    course_id = a.courseId,
                    lecturer_name = a.lecturerName,
                    date = a.date,
                    title = a.title,
                    content = a.content
                )
            )
            Log.d("EduHubSupabase", "Inserted announcement '${a.title}' into Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to insert announcement to Supabase: ${e.message}")
        }
    }

    suspend fun updateAnnouncement(a: Announcement) = withContext(Dispatchers.IO) {
        val idx = _announcements.indexOfFirst { it.id == a.id }
        if (idx != -1) _announcements[idx] = a
        try {
            SupabaseClientProvider.postgrest.from("announcements").update(
                {
                    set("title", a.title)
                    set("content", a.content)
                }
            ) { filter { eq("id", a.id) } }
            Log.d("EduHubSupabase", "Updated announcement '${a.id}' in Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to update announcement in Supabase: ${e.message}")
        }
    }

    suspend fun deleteAnnouncement(announcementId: String) = withContext(Dispatchers.IO) {
        _announcements.removeAll { it.id == announcementId }
        try {
            SupabaseClientProvider.postgrest.from("announcements").delete {
                filter { eq("id", announcementId) }
            }
            Log.d("EduHubSupabase", "Deleted announcement '$announcementId' from Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to delete announcement from Supabase: ${e.message}")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Note / Quiz Repository (Supabase Cloud Sync & Storage Bucket for PDFs)
// ─────────────────────────────────────────────────────────────────────────────
object NoteQuizRepository {
    private val _notes = mutableListOf<LectureNote>()
    private val _aiCache = mutableMapOf<String, AiGeneratedNote>()
    private val _quizHistory = mutableListOf<QuizHistoryItem>()

    fun getNotes(): List<LectureNote> = _notes.toList()
    fun getNoteById(id: String): LectureNote? = _notes.find { it.id == id }

    suspend fun uploadPdfToSupabase(context: Context, uri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val safeName = "${UUID.randomUUID()}_${fileName.replace(" ", "_")}"
            SupabaseClientProvider.storage.from("lecture-notes").upload(safeName, bytes) {
                upsert = true
            }
            val publicUrl = "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/lecture-notes/$safeName"
            Log.d("EduHubSupabase", "Successfully uploaded PDF to Supabase Storage: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to upload PDF to Supabase Storage: ${e.message}")
            null
        }
    }

    suspend fun fetchNotesFromSupabase(): List<LectureNote> = withContext(Dispatchers.IO) {
        try {
            val dtoList = SupabaseClientProvider.postgrest.from("lecture_notes")
                .select()
                .decodeList<LectureNoteDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} lecture notes from Supabase")

            val mapped = dtoList.map { dto ->
                LectureNote(
                    id = dto.id,
                    courseCode = dto.course_code,
                    courseTitle = dto.course_title,
                    semesterPeriod = dto.semester_period,
                    chapterTitle = dto.chapter_title,
                    rawContent = dto.raw_content,
                    pdfFileName = dto.pdf_file_name,
                    pdfUrl = dto.pdf_url
                )
            }
            if (mapped.isNotEmpty()) {
                val merged = (mapped + _notes).distinctBy { it.id }
                _notes.clear()
                _notes.addAll(merged)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch lecture notes: ${e.message}")
        }
        _notes.toList()
    }

    suspend fun addLectureNote(n: LectureNote) = withContext(Dispatchers.IO) {
        _notes.add(0, n)
        try {
            SupabaseClientProvider.postgrest.from("lecture_notes").insert(
                LectureNoteDto(
                    id = n.id,
                    course_code = n.courseCode,
                    course_title = n.courseTitle,
                    semester_period = n.semesterPeriod,
                    chapter_title = n.chapterTitle,
                    raw_content = n.rawContent,
                    pdf_file_name = n.pdfFileName,
                    pdf_url = n.pdfUrl
                )
            )
            Log.d("EduHubSupabase", "Inserted lecture note '${n.chapterTitle}' into Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to insert lecture note to Supabase: ${e.message}")
        }
    }

    suspend fun updateLectureNote(note: LectureNote) = withContext(Dispatchers.IO) {
        val idx = _notes.indexOfFirst { it.id == note.id }
        if (idx != -1) _notes[idx] = note
        _aiCache.remove(note.id)

        try {
            SupabaseClientProvider.postgrest.from("lecture_notes").update(
                {
                    set("chapter_title", note.chapterTitle)
                    set("semester_period", note.semesterPeriod)
                    set("raw_content", note.rawContent)
                }
            ) { filter { eq("id", note.id) } }
            Log.d("EduHubSupabase", "Updated lecture note '${note.id}' in Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to update lecture note in Supabase: ${e.message}")
        }
    }

    suspend fun deleteLectureNote(noteId: String) = withContext(Dispatchers.IO) {
        _notes.removeAll { it.id == noteId }
        _aiCache.remove(noteId)

        try {
            SupabaseClientProvider.postgrest.from("lecture_notes").delete {
                filter { eq("id", noteId) }
            }
            Log.d("EduHubSupabase", "Deleted lecture note '$noteId' from Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to delete lecture note from Supabase: ${e.message}")
        }
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