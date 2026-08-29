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
data class CourseEnrollmentDto(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String,
    val course_id: String
)

data class EnrolledStudent(
    val userId: String,
    val fullName: String,
    val role: String = "STUDENT"
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
// Auth Repository (100% Dynamic Supabase Auth & Strict Role Isolation)
// ─────────────────────────────────────────────────────────────────────────────
object AuthRepository {
    private val _currentUser = MutableStateFlow<EduHubUser?>(null)
    val currentUser: StateFlow<EduHubUser?> = _currentUser.asStateFlow()

    fun restoreUser(user: EduHubUser) {
        _currentUser.value = user
        StudyGroupRepository.onUserSignedIn(user)
        CourseRepository.onUserSignedIn(user)
    }

    suspend fun signInAsLecturer(email: String, password: String): Result<EduHubUser> = withContext(Dispatchers.IO) {
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
            var roleInDb: String? = null

            try {
                val profile = SupabaseClientProvider.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                if (profile != null) {
                    resolvedName = profile.full_name
                    roleInDb = profile.role
                }
            } catch (e: Exception) {}

            // Enforce role separation: If registered as STUDENT in Supabase, reject lecturer login
            if (roleInDb.equals("STUDENT", ignoreCase = true)) {
                SupabaseClientProvider.auth.signOut()
                return@withContext Result.failure(Exception("This account is registered as a Student. Please switch to the Student login tab."))
            }

            if (resolvedName.isNullOrBlank()) {
                val metaName = userObj?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                resolvedName = if (!metaName.isNullOrBlank()) {
                    metaName
                } else {
                    trimmedEmail.substringBefore("@").replace(".", " ").split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } } + " (Lecturer)"
                }
                try {
                    SupabaseClientProvider.postgrest.from("profiles").upsert(
                        ProfileDto(id = userId, full_name = resolvedName, role = "LECTURER")
                    )
                } catch (e: Exception) {}
            }

            val user = EduHubUser(userId, trimmedEmail, resolvedName, UserRole.LECTURER)
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            CourseRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Invalid lecturer credentials."))
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
            var roleInDb: String? = null

            try {
                val profile = SupabaseClientProvider.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                if (profile != null) {
                    resolvedName = profile.full_name
                    roleInDb = profile.role
                }
            } catch (e: Exception) {}

            // Enforce role separation: If registered as LECTURER in Supabase, reject student login
            if (roleInDb.equals("LECTURER", ignoreCase = true)) {
                SupabaseClientProvider.auth.signOut()
                return@withContext Result.failure(Exception("This account is registered as a Lecturer. Please switch to the Lecturer login tab."))
            }

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
            CourseRepository.onUserSignedIn(user)
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
            CourseRepository.onUserSignedIn(user)
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
        CourseRepository.onUserSignOut()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Course Repository (Enrollment Scoping, Student Roster & Course Hiding)
// ─────────────────────────────────────────────────────────────────────────────
object CourseRepository {
    private val _courses = mutableListOf<Course>()
    private val _announcements = mutableListOf<Announcement>()
    private val _enrolledCourseIds = mutableSetOf<String>()
    private val _hiddenCourseIds = mutableSetOf<String>()
    private val _enrolledStudentsMap = mutableMapOf<String, MutableList<EnrolledStudent>>()

    init {
        val localCourses = EduHubLocalStorage.loadCourses()
        if (localCourses.isNotEmpty()) {
            _courses.addAll(localCourses)
        }
        val localAnnouncements = EduHubLocalStorage.loadAnnouncements()
        if (localAnnouncements.isNotEmpty()) {
            _announcements.addAll(localAnnouncements)
        }
    }

    fun onUserSignedIn(user: EduHubUser) {
        _enrolledCourseIds.clear()
        _enrolledCourseIds.addAll(EduHubLocalStorage.loadEnrolledCourseIds(user.id))
        _hiddenCourseIds.clear()
        _hiddenCourseIds.addAll(EduHubLocalStorage.loadHiddenCourseIds(user.id))
    }

    fun onUserSignOut() {
        _enrolledCourseIds.clear()
        _hiddenCourseIds.clear()
    }

    fun getCourses(): List<Course> {
        if (_courses.isEmpty()) {
            val local = EduHubLocalStorage.loadCourses()
            if (local.isNotEmpty()) _courses.addAll(local)
        }
        return _courses.toList()
    }

    fun getCoursesForUser(user: EduHubUser?, includeHidden: Boolean = false): List<Course> {
        val all = getCourses()
        if (user == null) return emptyList()

        return if (user.role == UserRole.LECTURER) {
            // Lecturer sees courses taught by them or all portal courses
            val myTaught = all.filter { it.lecturerName.equals(user.name, ignoreCase = true) || it.lecturerName.contains("Lecturer", true) }
            if (myTaught.isNotEmpty()) myTaught else all
        } else {
            // Student ONLY sees courses they have joined via code
            if (_enrolledCourseIds.isEmpty()) {
                _enrolledCourseIds.addAll(EduHubLocalStorage.loadEnrolledCourseIds(user.id))
            }
            if (_hiddenCourseIds.isEmpty()) {
                _hiddenCourseIds.addAll(EduHubLocalStorage.loadHiddenCourseIds(user.id))
            }
            val enrolled = all.filter { _enrolledCourseIds.contains(it.id) || _enrolledCourseIds.contains(it.code) }
            if (includeHidden) {
                enrolled
            } else {
                enrolled.filter { !_hiddenCourseIds.contains(it.id) && !_hiddenCourseIds.contains(it.code) }
            }
        }
    }

    fun getHiddenCoursesForUser(user: EduHubUser?): List<Course> {
        if (user == null) return emptyList()
        val all = getCourses()
        return all.filter { _hiddenCourseIds.contains(it.id) || _hiddenCourseIds.contains(it.code) }
    }

    fun hideCourse(userId: String, courseId: String) {
        _hiddenCourseIds.add(courseId)
        EduHubLocalStorage.saveHiddenCourseIds(userId, _hiddenCourseIds)
    }

    fun unhideCourse(userId: String, courseId: String) {
        _hiddenCourseIds.remove(courseId)
        EduHubLocalStorage.saveHiddenCourseIds(userId, _hiddenCourseIds)
    }

    fun getCourseById(id: String): Course? =
        getCourses().find { it.id.equals(id, true) || it.code.equals(id, true) }

    suspend fun fetchCoursesFromSupabase(): List<Course> = withContext(Dispatchers.IO) {
        val currentUser = AuthRepository.currentUser.value
        if (currentUser != null) {
            _enrolledCourseIds.addAll(EduHubLocalStorage.loadEnrolledCourseIds(currentUser.id))
            _hiddenCourseIds.addAll(EduHubLocalStorage.loadHiddenCourseIds(currentUser.id))
        }

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
                EduHubLocalStorage.saveCourses(_courses)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch courses: ${e.message}")
        }
        _courses.toList()
    }

    suspend fun joinCourseWithCode(code: String, studentUser: EduHubUser?): Result<Course> = withContext(Dispatchers.IO) {
        val trimmedCode = code.trim().uppercase()
        val all = getCourses()
        val found = all.find { it.joinCode.equals(trimmedCode, ignoreCase = true) }

        if (found != null) {
            _enrolledCourseIds.add(found.id)
            _enrolledCourseIds.add(found.code)
            _hiddenCourseIds.remove(found.id)
            _hiddenCourseIds.remove(found.code)

            if (studentUser != null) {
                EduHubLocalStorage.saveEnrolledCourseIds(studentUser.id, _enrolledCourseIds)
                EduHubLocalStorage.saveHiddenCourseIds(studentUser.id, _hiddenCourseIds)

                val studentList = _enrolledStudentsMap.getOrPut(found.id) { mutableListOf() }
                if (!studentList.any { it.userId == studentUser.id }) {
                    studentList.add(EnrolledStudent(studentUser.id, studentUser.name, "STUDENT"))
                }

                try {
                    SupabaseClientProvider.postgrest.from("course_enrollments").insert(
                        CourseEnrollmentDto(user_id = studentUser.id, course_id = found.id)
                    )
                } catch (e: Exception) {}
            }
            Result.success(found)
        } else {
            Result.failure(Exception("Invalid join code \"$trimmedCode\". Please verify with your lecturer."))
        }
    }

    suspend fun fetchEnrolledStudents(courseId: String): List<EnrolledStudent> = withContext(Dispatchers.IO) {
        val students = _enrolledStudentsMap.getOrPut(courseId) { mutableListOf() }

        try {
            val enrollments = SupabaseClientProvider.postgrest.from("course_enrollments")
                .select { filter { eq("course_id", courseId) } }
                .decodeList<CourseEnrollmentDto>()

            val studentList = mutableListOf<EnrolledStudent>()
            for (enroll in enrollments) {
                var name = "Student (${enroll.user_id.take(6)})"
                try {
                    val p = SupabaseClientProvider.postgrest.from("profiles")
                        .select { filter { eq("id", enroll.user_id) } }
                        .decodeSingleOrNull<ProfileDto>()
                    if (p != null && p.full_name.isNotBlank()) {
                        name = p.full_name
                    }
                } catch (e: Exception) {}

                studentList.add(EnrolledStudent(enroll.user_id, name, "STUDENT"))
            }

            if (studentList.isNotEmpty()) {
                students.clear()
                students.addAll(studentList)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch enrolled students: ${e.message}")
        }

        students.toList()
    }

    suspend fun removeStudentFromCourse(courseId: String, studentUserId: String) = withContext(Dispatchers.IO) {
        _enrolledStudentsMap[courseId]?.removeAll { it.userId == studentUserId }

        val currentUser = AuthRepository.currentUser.value
        if (currentUser?.id == studentUserId) {
            _enrolledCourseIds.remove(courseId)
            EduHubLocalStorage.saveEnrolledCourseIds(studentUserId, _enrolledCourseIds)
        }

        try {
            SupabaseClientProvider.postgrest.from("course_enrollments").delete {
                filter {
                    eq("course_id", courseId)
                    eq("user_id", studentUserId)
                }
            }
            Log.d("EduHubSupabase", "Removed student $studentUserId from course $courseId in Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to remove student from course: ${e.message}")
        }
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
        EduHubLocalStorage.saveCourses(_courses)

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
                EduHubLocalStorage.saveAnnouncements(_announcements)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch announcements: ${e.message}")
        }
        getAnnouncementsForCourse(courseId)
    }

    suspend fun addAnnouncement(a: Announcement) = withContext(Dispatchers.IO) {
        _announcements.add(0, a)
        EduHubLocalStorage.saveAnnouncements(_announcements)
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
        EduHubLocalStorage.saveAnnouncements(_announcements)
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
        EduHubLocalStorage.saveAnnouncements(_announcements)
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
// Note / Quiz Repository (Full Local + Cloud Persistence)
// ─────────────────────────────────────────────────────────────────────────────
object NoteQuizRepository {
    private val _notes = mutableListOf<LectureNote>()
    private val _aiCache = mutableMapOf<String, AiGeneratedNote>()
    private val _quizHistory = mutableListOf<QuizHistoryItem>()

    init {
        val local = EduHubLocalStorage.loadNotes()
        if (local.isNotEmpty()) {
            _notes.addAll(local)
        }
    }

    fun getNotes(): List<LectureNote> {
        if (_notes.isEmpty()) {
            val local = EduHubLocalStorage.loadNotes()
            if (local.isNotEmpty()) _notes.addAll(local)
        }
        return _notes.toList()
    }

    fun getNoteById(id: String): LectureNote? = getNotes().find { it.id == id }

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
        val local = EduHubLocalStorage.loadNotes()
        if (local.isNotEmpty() && _notes.isEmpty()) {
            _notes.addAll(local)
        }

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
                EduHubLocalStorage.saveNotes(_notes)
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch lecture notes: ${e.message}")
        }
        _notes.toList()
    }

    suspend fun addLectureNote(n: LectureNote) = withContext(Dispatchers.IO) {
        _notes.add(0, n)
        EduHubLocalStorage.saveNotes(_notes)

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
        EduHubLocalStorage.saveNotes(_notes)

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
        EduHubLocalStorage.saveNotes(_notes)

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