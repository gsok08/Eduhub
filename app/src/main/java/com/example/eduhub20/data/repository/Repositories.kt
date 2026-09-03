package com.example.eduhub20.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.eduhub20.data.SupabaseClientProvider
import com.example.eduhub20.data.SupabaseConfig
import com.example.eduhub20.data.ai.EduHubAiGenerator
import com.example.eduhub20.data.local.EduHubLocalStorage
import com.example.eduhub20.data.model.*
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import androidx.core.content.edit
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name")
    val fullName: String,
    val role: String = "STUDENT",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("campus")
    val campus: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CourseDto(
    val id: String,
    val code: String,
    val title: String,
    @SerialName("lecturer_name")
    val lecturerName: String,
    @SerialName("join_code")
    val joinCode: String,
    @SerialName("icon_category")
    val iconCategory: String = "CODE",
    @SerialName("exam_days_left")
    val examDaysLeft: Int = 30,
    val progress: Float = 0.0f
)

@Serializable
data class CourseEnrollmentDto(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("course_id")
    val courseId: String,
    @SerialName("student_name")
    val studentName: String = "",
    @SerialName("student_email")
    val studentEmail: String = ""
)

data class EnrolledStudent(
    val userId: String,
    val fullName: String,
    val role: String = "STUDENT"
)

@Serializable
data class AnnouncementDto(
    val id: String,
    @SerialName("course_id")
    val courseId: String,
    @SerialName("lecturer_name")
    val lecturerName: String,
    val date: String,
    val title: String,
    val content: String
)

@Serializable
data class LectureNoteDto(
    val id: String,
    @SerialName("course_code")
    val courseCode: String,
    @SerialName("course_title")
    val courseTitle: String,
    @SerialName("semester_period")
    val semesterPeriod: String,
    @SerialName("chapter_title")
    val chapterTitle: String,
    @SerialName("raw_content")
    val rawContent: String,
    @SerialName("pdf_file_name")
    val pdfFileName: String? = null,
    @SerialName("pdf_url")
    val pdfUrl: String? = null
)

@Serializable
data class AiGeneratedNoteDto(
    val id: String,
    @SerialName("note_id")
    val noteId: String,
    val title: String,
    @SerialName("key_takeaways")
    val keyTakeaways: String,
    @SerialName("key_terminology")
    val keyTerminology: String,
    val summary: String,
    @SerialName("original_slides_url")
    val originalSlidesUrl: String = ""
)

@Serializable
data class StudyGroupDto(
    val id: String,
    val name: String,
    val host: String,
    val details: String,
    @SerialName("current_members")
    val currentMembers: Int = 1,

    @SerialName("max_members")
    val maxMembers: Int = 6,

    val category: String = "GROUP",

    @SerialName("host_user_id")
    val hostUserId: String = "",

    @SerialName("course_id")
    val courseId: String = "",

    @SerialName("course_code")
    val courseCode: String = "",

    @SerialName("course_title")
    val courseTitle: String = "",

    val status: String = "INACTIVE"
)

@Serializable
data class StudyRoomMember(
    val id: String? = null,

    @SerialName("group_id")
    val groupId: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("joined_at")
    val joinedAt: String? = null
)

@Serializable
data class ChatMessageDto(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("sender_name")
    val senderName: String,
    @SerialName("sender_role")
    val senderRole: String,
    val message: String,
    val timestamp: String,
    @SerialName("is_from_me")
    val isFromMe: Boolean = false,
    @SerialName("sender_avatar_url")
    val senderAvatarUrl: String? = null,
    @SerialName("sender_id")
    val senderId: String = ""
)

@Serializable
data class GroupMemberDto(
    val id: String,
    @SerialName("group_id")
    val groupId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val role: String = "MEMBER"
)

@Serializable
data class PastYearPaperDto(
    val id: String,
    @SerialName("course_code")
    val courseCode: String,
    @SerialName("course_title")
    val courseTitle: String,
    val session: String,
    @SerialName("subject_category")
    val subjectCategory: String = "Mobile App",
    val year: String = "2025/2026",
    @SerialName("duration_minutes")
    val durationMinutes: Int = 120,
    @SerialName("total_marks")
    val totalMarks: Int = 100,
    @SerialName("pdf_url")
    val pdfUrl: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// Auth Repository (100% Dynamic Supabase Auth & Strict Role Isolation)
// ─────────────────────────────────────────────────────────────────────────────
object AuthRepository {
    private val _currentUser = MutableStateFlow<EduHubUser?>(null)
    val currentUser: StateFlow<EduHubUser?> = _currentUser.asStateFlow()

    private fun parseAuthError(e: Throwable, defaultMsg: String): String {
        val msg = e.message ?: e.localizedMessage ?: return defaultMsg
        val lower = msg.lowercase()
        return when {
            lower.contains("over_email_send_rate_limit") || lower.contains("email rate limit") || lower.contains("security purposes") || lower.contains("once every") ->
                "Too many email requests. For security purposes, please wait 60 seconds before trying again."
            lower.contains("unexpected_failure") || lower.contains("error sending recovery email") ->
                "Email service limit reached. Supabase default email limit is 3 emails/hour. Please try again later or configure custom SMTP."
            lower.contains("invalid login credentials") || lower.contains("invalid_credentials") || lower.contains("invalid grant") || lower.contains("invalid_grant") ->
                "Incorrect email or password. Please check your details and try again."
            lower.contains("user already registered") || lower.contains("already registered") || lower.contains("user_already_exists") || lower.contains("identity already exists") ->
                "An account with this email already exists. Please sign in instead."
            lower.contains("password should be at least") || lower.contains("weak_password") ->
                "Password must be at least 6 characters long."
            lower.contains("invalid email") || lower.contains("validation_failed") ->
                "Please enter a valid email address (e.g. name@gmail.com)."
            lower.contains("user not found") || lower.contains("user_not_found") ->
                "No account found with this email address. Please check the spelling or sign up."
            lower.contains("network") || lower.contains("connect") || lower.contains("timeout") || lower.contains("unable to resolve host") || lower.contains("failed to connect") ->
                "Unable to connect to server. Please check your internet connection."
            lower.contains("rate limit") || lower.contains("too many requests") || lower.contains("over_request_rate_limit") ->
                "Too many attempts. Please wait a moment and try again."
            lower.contains("registered as a student") ->
                "This account is registered as a Student. Please switch to the Student login tab."
            lower.contains("registered as a lecturer") ->
                "This account is registered as a Lecturer. Please switch to the Lecturer login tab."
            else -> defaultMsg
        }
    }

    private val _currentSessionId = MutableStateFlow("")
    val currentSessionId: String get() = _currentSessionId.value

    suspend fun registerActiveSession(userId: String): String = withContext(Dispatchers.IO) {
        val nowIso = java.time.Instant.now().toString()
        _currentSessionId.value = nowIso

        // 1. Try updating both active_session_id and updated_at
        try {
            val withCol = buildJsonObject {
                put("updated_at", nowIso)
                put("active_session_id", nowIso)
            }
            SupabaseClientProvider.postgrest.from("profiles").update(withCol) {
                filter { eq("id", userId) }
            }
            Log.d("AuthRepository", "✅ Successfully updated active_session_id and updated_at to $nowIso")
        } catch (_: Exception) {
            // 2. Fallback: update updated_at (which is guaranteed to exist in Supabase profiles table)
            try {
                val timestampOnly = buildJsonObject {
                    put("updated_at", nowIso)
                }
                SupabaseClientProvider.postgrest.from("profiles").update(timestampOnly) {
                    filter { eq("id", userId) }
                }
                Log.d("AuthRepository", "✅ Successfully updated profiles.updated_at to $nowIso")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to update session in profiles: ${e.message}")
            }
        }

        // 3. Also update Supabase Auth user_metadata so user record carries active_session_id
        try {
            SupabaseClientProvider.auth.updateUser {
                data = buildJsonObject {
                    put("active_session_id", nowIso)
                }
            }
        } catch (_: Exception) {}

        nowIso
    }

    fun restoreUser(user: EduHubUser, sessionId: String = "") {
        _currentUser.value = user
        if (sessionId.isNotBlank()) {
            _currentSessionId.value = sessionId
        } else {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    val jsonObject = SupabaseClientProvider.postgrest.from("profiles")
                        .select { filter { eq("id", user.id) } }
                        .decodeSingleOrNull<JsonObject>()
                    val dbSession = jsonObject?.get("active_session_id")?.jsonPrimitive?.contentOrNull
                    val dbUpdatedAt = jsonObject?.get("updated_at")?.jsonPrimitive?.contentOrNull
                    _currentSessionId.value = when {
                        !dbSession.isNullOrBlank() -> dbSession
                        !dbUpdatedAt.isNullOrBlank() -> dbUpdatedAt
                        else -> ""
                    }
                    Log.d("AuthRepository", "Restored session for ${user.email}: ${_currentSessionId.value}")
                } catch (_: Exception) {}
            }
        }
        StudyGroupRepository.onUserSignedIn(user)
        CourseRepository.onUserSignedIn(user)
        Log.d("AuthRepository", "✅ User restored with avatar: ${user.avatarUrl}")
    }

    suspend fun checkSessionValid(userId: String): Boolean = withContext(Dispatchers.IO) {
        val localSession = _currentSessionId.value
        if (localSession.isBlank()) return@withContext true

        try {
            val jsonObject = SupabaseClientProvider.postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<JsonObject>()

            if (jsonObject != null) {
                // Check 1: active_session_id if populated in database
                val dbSession = jsonObject["active_session_id"]?.jsonPrimitive?.contentOrNull
                if (!dbSession.isNullOrBlank()) {
                    val valid = dbSession == localSession
                    Log.d("AuthRepository", "Single-device check (active_session_id): db=$dbSession vs local=$localSession => valid=$valid")
                    return@withContext valid
                }

                // Check 2: updated_at timestamp (guaranteed in Supabase profiles)
                val dbUpdatedAt = jsonObject["updated_at"]?.jsonPrimitive?.contentOrNull
                if (!dbUpdatedAt.isNullOrBlank()) {
                    val matches = try {
                        val dbInstant = java.time.Instant.parse(dbUpdatedAt)
                        val localInstant = java.time.Instant.parse(localSession)
                        dbInstant == localInstant
                    } catch (_: Exception) {
                        dbUpdatedAt.startsWith(localSession.substringBefore("Z"))
                    }
                    Log.d("AuthRepository", "Single-device check (updated_at): db=$dbUpdatedAt vs local=$localSession => valid=$matches")
                    return@withContext matches
                }
            }
        } catch (e: Exception) {
            Log.w("AuthRepository", "Failed to check session validity: ${e.message}")
        }
        true
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
            var avatarUrl: String? = null
            var campus: String? = null

            try {
                val profile = SupabaseClientProvider.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                if (profile != null) {
                    resolvedName = profile.fullName
                    roleInDb = profile.role
                    avatarUrl = profile.avatarUrl
                    campus = profile.campus
                }
            } catch (_: Exception) {}

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
            }

            val newSessionId = registerActiveSession(userId)

            try {
                SupabaseClientProvider.postgrest.from("profiles").upsert(
                    ProfileDto(
                        id = userId,
                        fullName = resolvedName,
                        role = "LECTURER",
                        avatarUrl = avatarUrl,
                        campus = campus,
                        updatedAt = newSessionId
                    )
                )
            } catch (_: Exception) {
                try {
                    val baseProfile = buildJsonObject {
                        put("id", userId)
                        put("full_name", resolvedName)
                        put("role", "LECTURER")
                        put("updated_at", newSessionId)
                    }
                    SupabaseClientProvider.postgrest.from("profiles").upsert(baseProfile)
                } catch (_: Exception) {}
            }

            val user = EduHubUser(
                userId,
                trimmedEmail,
                resolvedName,
                UserRole.LECTURER,
                avatarUrl = avatarUrl,
                campus = campus
            )
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            CourseRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(parseAuthError(e, "Incorrect email or password. Please try again.")))
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
            var avatarUrl: String? = null
            var campus: String? = null

            try {
                val profile = SupabaseClientProvider.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<ProfileDto>()
                if (profile != null) {
                    resolvedName = profile.fullName
                    roleInDb = profile.role
                    avatarUrl = profile.avatarUrl
                    campus = profile.campus
                }
            } catch (_: Exception) {}

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
            }

            val newSessionId = registerActiveSession(userId)

            try {
                SupabaseClientProvider.postgrest.from("profiles").upsert(
                    ProfileDto(
                        id = userId,
                        fullName = resolvedName,
                        role = "STUDENT",
                        avatarUrl = avatarUrl,
                        campus = campus,
                        updatedAt = newSessionId
                    )
                )
            } catch (_: Exception) {
                try {
                    val baseProfile = buildJsonObject {
                        put("id", userId)
                        put("full_name", resolvedName)
                        put("role", "STUDENT")
                        put("updated_at", newSessionId)
                    }
                    SupabaseClientProvider.postgrest.from("profiles").upsert(baseProfile)
                } catch (_: Exception) {}
            }

            val user = EduHubUser(
                userId,
                trimmedEmail,
                resolvedName,
                UserRole.STUDENT,
                avatarUrl = avatarUrl,
                campus = campus
                )
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            CourseRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(parseAuthError(e, "Incorrect email or password. Please try again.")))
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

            val newSessionId = registerActiveSession(userId)

            try {
                SupabaseClientProvider.postgrest.from("profiles").upsert(
                    ProfileDto(
                        id = userId,
                        fullName = defaultName,
                        role = "STUDENT",
                        avatarUrl = null,
                        campus = null,
                        updatedAt = newSessionId
                    )
                )
            } catch (_: Exception) {
                try {
                    val baseProfile = buildJsonObject {
                        put("id", userId)
                        put("full_name", defaultName)
                        put("role", "STUDENT")
                        put("updated_at", newSessionId)
                    }
                    SupabaseClientProvider.postgrest.from("profiles").upsert(baseProfile)
                } catch (_: Exception) {}
            }

            val user = EduHubUser(
                id = userId,
                email = trimmedEmail,
                name = defaultName,
                role = UserRole.STUDENT,
                avatarUrl = null,
                campus = null
            )
            _currentUser.value = user
            StudyGroupRepository.onUserSignedIn(user)
            CourseRepository.onUserSignedIn(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception(parseAuthError(e, "Sign-up failed. Please check your email and password.")))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClientProvider.auth.resetPasswordForEmail(email.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(parseAuthError(e, "Failed to send reset email. Please check the email address.")))
        }
    }

    suspend fun verifyOtpAndResetPassword(email: String, token: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClientProvider.auth.verifyEmailOtp(
                type = OtpType.Email.RECOVERY,
                email = email.trim(),
                token = token.trim()
            )
            SupabaseClientProvider.auth.updateUser {
                password = newPassword.trim()
            }
            SupabaseClientProvider.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(parseAuthError(e, "Invalid or expired OTP code. Please check your email and try again.")))
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
        } catch (_: Exception) {}

        try {
            SupabaseClientProvider.postgrest.from("profiles").upsert(
                ProfileDto(
                    id = user.id,
                    fullName = trimmed,
                    role = user.role.name,
                    avatarUrl = user.avatarUrl,
                    campus = user.campus
                )
            )
        } catch (_: Exception) {}

        _currentUser.value = user.copy(name = trimmed)
    }

    suspend fun updateProfileAvatar(avatarUrl: String, context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("User not logged in"))

        try {
            // Update Supabase
            SupabaseClientProvider.postgrest.from("profiles")
                .update(mapOf("avatar_url" to avatarUrl)) {
                    filter { eq("id", user.id) }
                }

            // Update local user
            _currentUser.value = user.copy(avatarUrl = avatarUrl)

            // ✅ Update SharedPreferences
            val prefs = context.getSharedPreferences("eduhub_auth_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("remember_me", false)) {
                prefs.edit().putString("saved_user_avatar_url", avatarUrl).apply()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update avatar: ${e.message}")
            Result.failure(Exception("Failed to update avatar: ${e.message}"))
        }
    }

    suspend fun updateCampus(campus: String): Result<Unit> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("User not logged in"))

        try {
            // Update profiles table in Supabase
            SupabaseClientProvider.postgrest.from("profiles")
                .update(
                    mapOf("campus" to campus)
                ) {
                    filter { eq("id", user.id) }
                }

            // Update local user
            _currentUser.value = user.copy(campus = campus)

            // Update SharedPreferences if remember me is enabled
            try {
                val context = android.app.Application().applicationContext
                val prefs = context.getSharedPreferences("eduhub_auth_prefs", Context.MODE_PRIVATE)
                if (prefs.getBoolean("remember_me", false)) {
                    prefs.edit { putString("saved_user_campus", campus) }
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to update SharedPreferences: ${e.message}")
            }

            Log.d("AuthRepository", "✅ Campus updated to: $campus")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update campus: ${e.message}")
            Result.failure(Exception("Failed to update campus: ${e.message}"))
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: return@withContext Result.failure(Exception("User not logged in"))

            // Validate new password
            if (newPassword.length < 6) {
                return@withContext Result.failure(Exception("Password must be at least 6 characters long"))
            }

            if (currentPassword == newPassword) {
                return@withContext Result.failure(Exception("New password must be different from current password"))
            }

            if (currentPassword.isBlank()) {
                return@withContext Result.failure(Exception("Please enter your current password"))
            }

            // ✅ Step 1: Verify current password
            try {
                SupabaseClientProvider.auth.signInWith(Email) {
                    this.email = user.email
                    this.password = currentPassword
                }
                Log.d("AuthRepository", "Current password verified")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Current password verification failed: ${e.message}")
                return@withContext Result.failure(Exception("Current password is incorrect. Please try again."))
            }

            // ✅ Step 2: Change to new password
            try {
                SupabaseClientProvider.auth.updateUser {
                    this.password = newPassword
                }
                Log.d("AuthRepository", "Password changed successfully")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to update password: ${e.message}")
                return@withContext Result.failure(Exception("Failed to update password: ${e.message}"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Unexpected error: ${e.message}")
            Result.failure(Exception("Failed to change password: ${e.message}"))
        }
    }


    fun signOut() {
        try {
            // Non-blocking sign out
        } catch (_: Exception) {}
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

    fun getEnrolledCourses(): List<Course> {
        return _courses.filter { it.id in _enrolledCourseIds }
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
                    lecturerName = dto.lecturerName,
                    joinCode = dto.joinCode,
                    iconCategory = dto.iconCategory,
                    examDaysLeft = dto.examDaysLeft,
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
                EduHubLocalStorage.saveStudentName(studentUser.id, studentUser.name)

                val studentList = _enrolledStudentsMap.getOrPut(found.id) { mutableListOf() }
                if (!studentList.any { it.userId == studentUser.id }) {
                    studentList.add(EnrolledStudent(studentUser.id, studentUser.name, "STUDENT"))
                }

                try {
                    SupabaseClientProvider.postgrest.from("profiles").upsert(
                        ProfileDto(id = studentUser.id, fullName = studentUser.name, role = "STUDENT")
                    )
                } catch (_: Exception) {}

                try {
                    SupabaseClientProvider.postgrest.from("course_enrollments").upsert(
                        CourseEnrollmentDto(
                            userId = studentUser.id,
                            courseId = found.id,
                            studentName = studentUser.name,
                            studentEmail = studentUser.email
                        )
                    )
                } catch (_: Exception) {
                    try {
                        val fallbackDto = CourseEnrollmentDto(
                            userId = studentUser.id,
                            courseId = found.id
                        )
                        SupabaseClientProvider.postgrest.from("course_enrollments").insert(fallbackDto)
                    } catch (_: Exception) {}
                }
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

            val profileMap = try {
                SupabaseClientProvider.postgrest.from("profiles")
                    .select()
                    .decodeList<ProfileDto>()
                    .associateBy { it.id }
            } catch (_: Exception) {
                emptyMap()
            }

            val studentList = mutableListOf<EnrolledStudent>()
            for (enroll in enrollments) {
                val profile = profileMap[enroll.userId]
                val cachedName = EduHubLocalStorage.loadStudentName(enroll.userId)

                val resolvedName = when {
                    profile != null && profile.fullName.isNotBlank() -> profile.fullName
                    enroll.studentName.isNotBlank() -> enroll.studentName
                    !cachedName.isNullOrBlank() -> cachedName
                    else -> "Student (${enroll.userId.take(6)})"
                }

                if (resolvedName.isNotBlank() && !resolvedName.startsWith("Student (")) {
                    EduHubLocalStorage.saveStudentName(enroll.userId, resolvedName)
                }

                studentList.add(EnrolledStudent(enroll.userId, resolvedName, "STUDENT"))
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
                    lecturerName = nc.lecturerName,
                    joinCode = nc.joinCode,
                    iconCategory = nc.iconCategory,
                    examDaysLeft = nc.examDaysLeft,
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
                    courseId = dto.courseId,
                    lecturerName = dto.lecturerName,
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
                    courseId = a.courseId,
                    lecturerName = a.lecturerName,
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
// Note / Quiz Repository (Full Local + Cloud Persistence & AI Note Caching)
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
        val localAiNotes = EduHubLocalStorage.loadAllAiNotes()
        if (localAiNotes.isNotEmpty()) {
            _aiCache.putAll(localAiNotes)
        }
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
                    courseCode = dto.courseCode,
                    courseTitle = dto.courseTitle,
                    semesterPeriod = dto.semesterPeriod,
                    chapterTitle = dto.chapterTitle,
                    rawContent = dto.rawContent,
                    pdfFileName = dto.pdfFileName,
                    pdfUrl = dto.pdfUrl
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
                    courseCode = n.courseCode,
                    courseTitle = n.courseTitle,
                    semesterPeriod = n.semesterPeriod,
                    chapterTitle = n.chapterTitle,
                    rawContent = n.rawContent,
                    pdfFileName = n.pdfFileName,
                    pdfUrl = n.pdfUrl
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

    fun getCachedAiNoteOnly(noteId: String): AiGeneratedNote? {
        val inMem = _aiCache[noteId]
        if (inMem != null && EduHubAiGenerator.isCleanAiNote(inMem)) return inMem

        val localSaved = EduHubLocalStorage.loadAiNote(noteId)
        if (localSaved != null && EduHubAiGenerator.isCleanAiNote(localSaved)) {
            _aiCache[noteId] = localSaved
            return localSaved
        }
        return null
    }

    suspend fun fetchCachedAiNoteFromSupabaseOrDisk(noteId: String): AiGeneratedNote? = withContext(Dispatchers.IO) {
        val cached = getCachedAiNoteOnly(noteId)
        if (cached != null) return@withContext cached

        // Check Supabase ai_generated_notes without triggering AI generation
        try {
            val remoteDto = SupabaseClientProvider.postgrest.from("ai_generated_notes")
                .select { filter { eq("note_id", noteId) } }
                .decodeSingleOrNull<AiGeneratedNoteDto>()

            if (remoteDto != null) {
                val takeaways = Json.decodeFromString<List<String>>(remoteDto.keyTakeaways)
                val terminology = Json.decodeFromString<Map<String, String>>(remoteDto.keyTerminology)
                val remoteNote = AiGeneratedNote(
                    id = remoteDto.id,
                    noteId = remoteDto.noteId,
                    title = remoteDto.title,
                    keyTakeaways = takeaways,
                    keyTerminology = terminology,
                    summary = remoteDto.summary,
                    originalSlidesUrl = remoteDto.originalSlidesUrl
                )
                if (EduHubAiGenerator.isCleanAiNote(remoteNote)) {
                    _aiCache[noteId] = remoteNote
                    EduHubLocalStorage.saveAiNote(noteId, remoteNote)
                    return@withContext remoteNote
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Retrieves the cached AI note from disk/memory or generates a new one via Gemini API.
     */
    suspend fun getOrGenerateAiNote(note: LectureNote, forceRegenerate: Boolean = false): AiGeneratedNote = withContext(Dispatchers.IO) {
        if (!forceRegenerate) {
            val inMem = _aiCache[note.id]
            if (inMem != null && EduHubAiGenerator.isCleanAiNote(inMem)) return@withContext inMem

            val localSaved = EduHubLocalStorage.loadAiNote(note.id)
            if (localSaved != null && EduHubAiGenerator.isCleanAiNote(localSaved)) {
                _aiCache[note.id] = localSaved
                return@withContext localSaved
            }

            // Check Supabase ai_generated_notes
            try {
                val remoteDto = SupabaseClientProvider.postgrest.from("ai_generated_notes")
                    .select { filter { eq("note_id", note.id) } }
                    .decodeSingleOrNull<AiGeneratedNoteDto>()

                if (remoteDto != null) {
                    val takeaways = Json.decodeFromString<List<String>>(remoteDto.keyTakeaways)
                    val terminology = Json.decodeFromString<Map<String, String>>(remoteDto.keyTerminology)
                    val remoteNote = AiGeneratedNote(
                        id = remoteDto.id,
                        noteId = remoteDto.noteId,
                        title = remoteDto.title,
                        keyTakeaways = takeaways,
                        keyTerminology = terminology,
                        summary = remoteDto.summary,
                        originalSlidesUrl = remoteDto.originalSlidesUrl
                    )
                    if (EduHubAiGenerator.isCleanAiNote(remoteNote)) {
                        _aiCache[note.id] = remoteNote
                        EduHubLocalStorage.saveAiNote(note.id, remoteNote)
                        return@withContext remoteNote
                    }
                }
            } catch (_: Exception) {}
        }

        // Generate new note via Gemini API
        val generated = EduHubAiGenerator.generateNoteSummary(note)
        _aiCache[note.id] = generated
        EduHubLocalStorage.saveAiNote(note.id, generated)

        // Save to Supabase ai_generated_notes
        try {
            SupabaseClientProvider.postgrest.from("ai_generated_notes").upsert(
                AiGeneratedNoteDto(
                    id = generated.id,
                    noteId = generated.noteId,
                    title = generated.title,
                    keyTakeaways = Json.encodeToString(generated.keyTakeaways),
                    keyTerminology = Json.encodeToString(generated.keyTerminology),
                    summary = generated.summary,
                    originalSlidesUrl = generated.originalSlidesUrl
                )
            )
        } catch (_: Exception) {}

        generated
    }

    /**
     * Saves user-edited study note to local storage and Supabase.
     */
    suspend fun saveOrUpdateAiNote(aiNote: AiGeneratedNote) = withContext(Dispatchers.IO) {
        _aiCache[aiNote.noteId] = aiNote
        EduHubLocalStorage.saveAiNote(aiNote.noteId, aiNote)

        try {
            SupabaseClientProvider.postgrest.from("ai_generated_notes").upsert(
                AiGeneratedNoteDto(
                    id = aiNote.id,
                    noteId = aiNote.noteId,
                    title = aiNote.title,
                    keyTakeaways = Json.encodeToString(aiNote.keyTakeaways),
                    keyTerminology = Json.encodeToString(aiNote.keyTerminology),
                    summary = aiNote.summary,
                    originalSlidesUrl = aiNote.originalSlidesUrl
                )
            )
            Log.d("EduHubSupabase", "Updated AI study note '${aiNote.title}' in Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to update AI note in Supabase: ${e.message}")
        }
    }

    fun getNotes(): List<LectureNote> {
        if (_notes.isEmpty()) {
            val local = EduHubLocalStorage.loadNotes()
            if (local.isNotEmpty()) _notes.addAll(local)
        }
        return _notes.toList()
    }

    fun getNotesForUser(user: EduHubUser?): List<LectureNote> {
        val allNotes = getNotes()
        if (user == null) return emptyList()
        if (user.role == UserRole.LECTURER) return allNotes

        val userCourses = CourseRepository.getCoursesForUser(user).map { it.code.trim().uppercase() }.toSet()
        return allNotes.filter { note ->
            userCourses.contains(note.courseCode.trim().uppercase())
        }
    }

    fun getQuizHistory(): List<QuizHistoryItem> = _quizHistory.toList()

    fun getQuizHistoryForUser(user: EduHubUser?): List<QuizHistoryItem> {
        val allQuizzes = getQuizHistory()
        if (user == null) return emptyList()
        if (user.role == UserRole.LECTURER) return allQuizzes

        val userCourses = CourseRepository.getCoursesForUser(user).map { it.code.trim().uppercase() }.toSet()
        return allQuizzes.filter { quiz ->
            userCourses.contains(quiz.courseCode.trim().uppercase())
        }
    }

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
        _joinedGroupIds.addAll(
            EduHubLocalStorage.loadJoinedGroupIds(userId)
        )

        try {
            val joinedIds = if (currentUser != null) {
                SupabaseClientProvider.postgrest
                    .from("study_group_members")
                    .select {
                        filter {
                            eq("user_id", currentUser.id)
                        }
                    }
                    .decodeList<StudyGroupMember>()
                    .map { it.groupId }
                    .toSet()
            } else {
                emptySet()
            }

            val dtoList = SupabaseClientProvider.postgrest
                .from("study_groups")
                .select()
                .decodeList<StudyGroupDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} groups from Supabase")

            val allMembers = try {
                SupabaseClientProvider.postgrest.from("group_members").select().decodeList<GroupMemberDto>()
            } catch (_: Exception) {
                emptyList()
            }
            val memberCountMap = allMembers.groupBy { it.groupId }.mapValues { it.value.size }
            val userJoinedGroupIdsFromDb = if (currentUser != null) {
                allMembers.filter { it.userId == currentUser.id }.map { it.groupId }.toSet()
            } else emptySet()

            val remoteMapped = dtoList.map { dto ->
                val isHost = (currentUser != null && dto.hostUserId.isNotBlank() && dto.hostUserId == currentUser.id) ||
                        (currentUser != null && dto.host.equals(currentUser.name, true))
                val isJoined = isHost || userJoinedGroupIdsFromDb.contains(dto.id) || _joinedGroupIds.contains(dto.id)

                if (isJoined) {
                    _joinedGroupIds.add(dto.id)
                }

                val realCount = if (memberCountMap.containsKey(dto.id) && memberCountMap[dto.id]!! > 0) {
                    memberCountMap[dto.id]!!.coerceAtMost(dto.maxMembers)
                } else {
                    dto.currentMembers.coerceIn(1, dto.maxMembers)
                }

                // Self-heal Supabase current_members column if out of sync
                if (realCount != dto.currentMembers) {
                    try {
                        SupabaseClientProvider.postgrest.from("study_groups").update(
                            { set("current_members", realCount) }
                        ) { filter { eq("id", dto.id) } }
                    } catch (_: Exception) {}
                }

                StudyGroup(
                    id = dto.id,
                    name = dto.name,
                    host = dto.host,
                    details = dto.details,
                    currentMembers = realCount,
                    maxMembers = dto.maxMembers,
                    isJoined = isJoined,
                    category = dto.category,
                    hostUserId = dto.hostUserId,
                    courseId = dto.courseId,
                    courseCode = dto.courseCode,
                    courseTitle = dto.courseTitle,
                    status = dto.status
                )
            }

            val remoteIds = remoteMapped.map { it.id }.toSet()

            val enrolledCourseIds = CourseRepository.getEnrolledCourses()
                .map { it.id }
                .toSet()

            val filteredGroups = remoteMapped.filter { group ->
                group.courseId.isNotBlank() && group.courseId in enrolledCourseIds
            }

            // Purge deleted groups and their chat messages
            _joinedGroupIds.retainAll(remoteIds)
            val deletedGroups = _groups.filter { !remoteIds.contains(it.id) }
            for (deleted in deletedGroups) {
                _messages.remove(deleted.id)
                EduHubLocalStorage.saveChatMessages(deleted.id, emptyList())
            }

            _groups.clear()
            _groups.addAll(filteredGroups)
            EduHubLocalStorage.saveGroups(filteredGroups)
            if (currentUser != null) {
                EduHubLocalStorage.saveJoinedGroupIds(currentUser.id, _joinedGroupIds)
            }

        } catch (e: Exception) {
            Log.e(
                "EduHubSupabase",
                "Failed to fetch study_groups from Supabase: ${e.message}"
            )
        }

        _groups.toList()
    }

    suspend fun joinGroup(groupId: String) = withContext(Dispatchers.IO) {
        val currentUser = AuthRepository.currentUser.value

        // Find the group
        val i = _groups.indexOfFirst { it.id == groupId }

        // Group does not exist
        if (i == -1) {
            return@withContext
        }

        val group = _groups[i]

        // User already joined this group
        if (_joinedGroupIds.contains(groupId)) {
            return@withContext
        }

        // Group is full
        if (group.currentMembers >= group.maxMembers) {
            Log.d(
                "EduHubSupabase",
                "Cannot join group: group is full"
            )
            return@withContext
        }

        // Add group to user's joined groups
        _joinedGroupIds.add(groupId)

        if (currentUser != null) {
            EduHubLocalStorage.saveJoinedGroupIds(
                currentUser.id,
                _joinedGroupIds
            )
        }

        // Increase member count
        val newCount = (group.currentMembers + 1)
            .coerceAtMost(group.maxMembers)

        val updated = group.copy(
            isJoined = true,
            currentMembers = newCount
        )

        _groups[i] = updated

        EduHubLocalStorage.saveGroups(_groups.toList())

        // Update member count in Supabase
        try {
            SupabaseClientProvider.postgrest
                .from("study_groups")
                .update(
                    {
                        set("current_members", newCount)
                    }
                ) {
                    filter {
                        eq("id", groupId)
                    }
                }

            Log.d(
                "EduHubSupabase",
                "Updated member count for group $groupId"
            )

        } catch (e: Exception) {
            Log.e(
                "EduHubSupabase",
                "Failed to update member count: ${e.message}"
            )
        }

        // Add user to study_group_members
        if (currentUser != null) {
            try {
                SupabaseClientProvider.postgrest
                    .from("study_group_members")
                    .insert(
                        StudyGroupMember(
                            groupId = groupId,
                            userId = currentUser.id
                        )
                    )

            } catch (e: Exception) {
                Log.e(
                    "EduHubSupabase",
                    "Failed to add study group member: ${e.message}"
                )
            }

            // Add user to group_members as MEMBER
            try {
                SupabaseClientProvider.postgrest
                    .from("group_members")
                    .upsert(
                        GroupMemberDto(
                            id = "${groupId}_${currentUser.id}",
                            groupId = groupId,
                            userId = currentUser.id,
                            userName = currentUser.name,
                            avatarUrl = currentUser.avatarUrl,
                            role = "MEMBER"
                        )
                    )

            } catch (e: Exception) {
                Log.e(
                    "EduHubSupabase",
                    "Failed to add group member: ${e.message}"
                )
            }
        }
    }

    suspend fun getGroupMembers(groupId: String): List<GroupMember> =
        withContext(Dispatchers.IO) {
            try {
                SupabaseClientProvider.postgrest
                    .from("group_members")
                    .select {
                        filter {
                            eq("group_id", groupId)
                        }
                    }
                    .decodeList<GroupMember>()

            } catch (e: Exception) {
                Log.e(
                    "EduHubSupabase",
                    "Failed to fetch group members: ${e.message}"
                )

                emptyList()
            }
        }

    suspend fun createGroup(name: String, details: String, course: Course? = null, hostUser: EduHubUser? = null): StudyGroup = withContext(Dispatchers.IO) {
        val groupId = UUID.randomUUID().toString()
        val hostId = hostUser?.id ?: ""
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

        val courseId = course?.id ?: ""
        val courseCode = course?.code ?: ""
        val courseTitle = course?.title ?: ""

        val g = StudyGroup(
            id = groupId,
            name = name,
            host = resolvedHost,
            details = details,
            currentMembers = 1,
            maxMembers = 6,
            isJoined = true,
            category = "GROUP",
            hostUserId = hostId,
            courseId = courseId,
            courseCode = courseCode,
            courseTitle = courseTitle,
            status = "INACTIVE"
        )
        try {
            SupabaseClientProvider.postgrest
                .from("study_group_members")
                .insert(
                    StudyGroupMember(
                        groupId = groupId,
                        userId = hostId
                    )
                )
        } catch (_: Exception) {}

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
            SupabaseClientProvider.postgrest.from("study_groups").upsert(
                StudyGroupDto(
                    id = groupId,
                    name = name,
                    host = resolvedHost,
                    details = details,
                    currentMembers = 1,
                    maxMembers = 6,
                    category = "GROUP",
                    hostUserId = hostId,
                    courseId = courseId,
                    courseCode = courseCode,
                    courseTitle = courseTitle,
                    status = "INACTIVE"
                )
            )
            Log.d("EduHubSupabase", "Successfully upserted study group '$name' into Supabase")
        } catch (e: Exception) {
            Log.w("EduHubSupabase", "Primary group upsert failed: ${e.message}. Retrying with base JSON...")
            try {
                val baseGroup = buildJsonObject {
                    put("id", groupId)
                    put("name", name)
                    put("host", resolvedHost)
                    put("details", details)
                    put("current_members", 1)
                    put("max_members", 6)
                    put("category", "GROUP")
                    if (courseId.isNotBlank()) put("course_id", courseId)
                    if (courseCode.isNotBlank()) put("course_code", courseCode)
                    if (courseTitle.isNotBlank()) put("course_title", courseTitle)
                }
                SupabaseClientProvider.postgrest.from("study_groups").upsert(baseGroup)
            } catch (inner: Exception) {
                Log.e("EduHubSupabase", "Failed to upsert study group into Supabase: ${inner.message}")
            }
        }

        // Add creator as Host in group_members
        if (hostUser != null) {
            try {
                SupabaseClientProvider.postgrest.from("group_members").upsert(
                    GroupMemberDto(
                        id = "${groupId}_${hostUser.id}",
                        groupId = groupId,
                        userId = hostUser.id,
                        userName = resolvedHost,
                        avatarUrl = hostUser.avatarUrl,
                        role = "HOST"
                    )
                )
            } catch (_: Exception) {}
        }

        try {
            val welcomeJson = buildJsonObject {
                put("id", welcomeMsg.id)
                put("group_id", groupId)
                put("sender_name", welcomeMsg.senderName)
                put("sender_role", welcomeMsg.senderRole)
                put("message", welcomeMsg.message)
                put("timestamp", welcomeMsg.timestamp)
                put("is_from_me", false)
            }
            SupabaseClientProvider.postgrest.from("chat_messages").insert(welcomeJson)
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to insert initial chat message: ${e.message}")
        }

        g
    }

    suspend fun createGroup(name: String, details: String, hostUser: EduHubUser?): StudyGroup =
        createGroup(name, details, null, hostUser)

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

        val currentUser = AuthRepository.currentUser.value

        try {
            val dtoList = SupabaseClientProvider.postgrest.from("chat_messages")
                .select {
                    filter { eq("group_id", groupId) }
                }
                .decodeList<ChatMessageDto>()

            Log.d("EduHubSupabase", "Fetched ${dtoList.size} messages for group $groupId from Supabase")

            val remoteMapped = dtoList.map { dto ->
                val isMe = (currentUser != null && dto.senderId.isNotBlank() && dto.senderId == currentUser.id) ||
                        dto.senderName.equals(currentUserName, ignoreCase = true)
                ChatMessage(
                    id = dto.id,
                    groupId = dto.groupId,
                    senderName = dto.senderName,
                    senderRole = dto.senderRole,
                    message = dto.message,
                    timestamp = dto.timestamp,
                    isFromMe = isMe,
                    senderAvatarUrl = dto.senderAvatarUrl,
                    senderId = dto.senderId
                )
            }

            // Preserve any newly sent local messages until they sync with Supabase
            val remoteIds = remoteMapped.map { it.id }.toSet()
            val localPending = (_messages[groupId] ?: emptyList()).filter { !remoteIds.contains(it.id) }
            val merged = (remoteMapped + localPending).distinctBy { it.id }

            _messages[groupId] = merged.toMutableList()
            EduHubLocalStorage.saveChatMessages(groupId, merged)
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to fetch chat_messages from Supabase: ${e.message}")
        }

        _messages.getOrPut(groupId) { mutableListOf() }.toList()
    }

    suspend fun sendMessage(
        groupId: String,
        text: String,
        senderName: String = "Me",
        senderRole: String = "Student",
        customMsgId: String? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        val now = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        val msgId = customMsgId ?: UUID.randomUUID().toString()
        val currentUser = AuthRepository.currentUser.value
        val senderAvatarUrl = currentUser?.avatarUrl
        val senderId = currentUser?.id ?: ""

        val msg = ChatMessage(
            id = msgId,
            groupId = groupId,
            senderName = senderName,
            senderRole = senderRole,
            message = text,
            timestamp = now,
            isFromMe = true,
            senderAvatarUrl = senderAvatarUrl,
            senderId = senderId
        )

        val list = _messages.getOrPut(groupId) { mutableListOf() }
        if (list.none { it.id == msgId }) {
            list.add(msg)
        }
        EduHubLocalStorage.saveChatMessages(groupId, list.toList())

        // Insert into Supabase with fallback to ensure messages NEVER fail even if columns are missing
        try {
            val fullMsg = buildJsonObject {
                put("id", msgId)
                put("group_id", groupId)
                put("sender_name", senderName)
                put("sender_role", senderRole)
                put("message", text)
                put("timestamp", now)
                put("is_from_me", true)
                if (!senderAvatarUrl.isNullOrBlank()) put("sender_avatar_url", senderAvatarUrl)
                if (senderId.isNotBlank()) put("sender_id", senderId)
            }
            SupabaseClientProvider.postgrest.from("chat_messages").insert(fullMsg)
            Log.d("EduHubSupabase", "✅ Sent chat message to Supabase for group $groupId")
        } catch (e: Exception) {
            Log.w("EduHubSupabase", "Insert with extra columns failed: ${e.message}. Retrying with base columns...")
            try {
                val baseMsg = buildJsonObject {
                    put("id", msgId)
                    put("group_id", groupId)
                    put("sender_name", senderName)
                    put("sender_role", senderRole)
                    put("message", text)
                    put("timestamp", now)
                    put("is_from_me", true)
                }
                SupabaseClientProvider.postgrest.from("chat_messages").insert(baseMsg)
                Log.d("EduHubSupabase", "✅ Sent base chat message to Supabase successfully")
            } catch (inner: Exception) {
                Log.e("EduHubSupabase", "Failed to send message to Supabase: ${inner.message}")
            }
        }

        msg
    }

    suspend fun clearChatHistory(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        _messages[groupId]?.clear()
        EduHubLocalStorage.saveChatMessages(groupId, emptyList())
        try {
            SupabaseClientProvider.postgrest.from("chat_messages").delete {
                filter { eq("group_id", groupId) }
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to clear chat history in Supabase: ${e.message}")
        }
        Result.success(Unit)
    }

    suspend fun fetchGroupMembers(groupId: String, groupHostName: String = "", hostUserId: String = ""): List<GroupMember> = withContext(Dispatchers.IO) {
        try {
            val dtoList = SupabaseClientProvider.postgrest.from("group_members")
                .select { filter { eq("group_id", groupId) } }
                .decodeList<GroupMemberDto>()

            if (dtoList.isNotEmpty()) {
                return@withContext dtoList.map {
                    GroupMember(
                        id = it.id,
                        groupId = it.groupId,
                        userId = it.userId,
                        userName = it.userName,
                        userAvatarUrl = it.avatarUrl,
                        role = it.role
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EduHubSupabase", "Failed to fetch group_members from Supabase: ${e.message}")
        }

        // Fallback roster
        val currentUser = AuthRepository.currentUser.value
        val list = mutableListOf<GroupMember>()
        if (groupHostName.isNotBlank() || hostUserId.isNotBlank()) {
            list.add(
                GroupMember(
                    id = "${groupId}_host",
                    groupId = groupId,
                    userId = hostUserId,
                    userName = if (groupHostName.isNotBlank()) groupHostName else "Host",
                    userAvatarUrl = if (currentUser?.id == hostUserId) currentUser.avatarUrl else null,
                    role = "HOST"
                )
            )
        }
        if (currentUser != null && currentUser.id != hostUserId && _joinedGroupIds.contains(groupId)) {
            list.add(
                GroupMember(
                    id = "${groupId}_${currentUser.id}",
                    groupId = groupId,
                    userId = currentUser.id,
                    userName = currentUser.name,
                    userAvatarUrl = currentUser.avatarUrl,
                    role = "MEMBER"
                )
            )
        }
        if (list.isNotEmpty()) {
            for (m in list) {
                try {
                    SupabaseClientProvider.postgrest.from("group_members").upsert(
                        GroupMemberDto(
                            id = m.id,
                            groupId = m.groupId,
                            userId = m.userId,
                            userName = m.userName,
                            avatarUrl = m.userAvatarUrl,
                            role = m.role
                        )
                    )
                } catch (_: Exception) {}
            }
        }
        list
    }

    suspend fun setMemberRole(groupId: String, targetUserId: String, newRole: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClientProvider.postgrest.from("group_members").update(
                { set("role", newRole) }
            ) {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", targetUserId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to update member role: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun kickMember(groupId: String, targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            SupabaseClientProvider.postgrest.from("group_members").delete {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", targetUserId)
                }
            }
            try {
                SupabaseClientProvider.postgrest
                    .from("study_group_members")
                    .delete {
                        filter {
                            eq("group_id", groupId)
                            eq("user_id", targetUserId)
                        }
                    }
            } catch (e: Exception) {
                Log.e(
                    "EduHubSupabase",
                    "Failed to delete from study_group_members: ${e.message}"
                )
            }
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to delete from group_members: ${e.message}")
        }

        val i = _groups.indexOfFirst { it.id == groupId }
        if (i != -1) {
            val updatedMembers = maxOf(1, _groups[i].currentMembers - 1)
            _groups[i] = _groups[i].copy(currentMembers = updatedMembers)
            EduHubLocalStorage.saveGroups(_groups.toList())

            try {
                SupabaseClientProvider.postgrest.from("study_groups").update(
                    { set("current_members", updatedMembers) }
                ) { filter { eq("id", groupId) } }
            } catch (_: Exception) {}
        }

        val currentUser = AuthRepository.currentUser.value
        if (currentUser?.id == targetUserId) {
            _joinedGroupIds.remove(groupId)
            EduHubLocalStorage.saveJoinedGroupIds(targetUserId, _joinedGroupIds)
        }

        Result.success(Unit)
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUser = AuthRepository.currentUser.value ?: return@withContext Result.failure(Exception("Not signed in"))
        kickMember(groupId, currentUser.id)
    }

    suspend fun joinGroupByCodeOrLink(rawInput: String): Result<StudyGroup> = withContext(Dispatchers.IO) {
        val cleanInput = rawInput.trim()
        val cleanId = when {
            cleanInput.contains("join/") -> cleanInput.substringAfter("join/").substringBefore("?").substringBefore("/").trim()
            cleanInput.contains("groupId=") -> cleanInput.substringAfter("groupId=").substringBefore("&").trim()
            else -> cleanInput
        }
        if (cleanId.isBlank()) return@withContext Result.failure(Exception("Please enter a valid Group Code or Invitation Link."))

        var group = _groups.find { it.id.equals(cleanId, ignoreCase = true) }
        if (group == null) {
            try {
                val dto = SupabaseClientProvider.postgrest.from("study_groups")
                    .select { filter { eq("id", cleanId) } }
                    .decodeSingleOrNull<StudyGroupDto>()
                if (dto != null) {
                    group = StudyGroup(
                        id = dto.id,
                        name = dto.name,
                        host = dto.host,
                        details = dto.details,
                        currentMembers = dto.currentMembers,
                        maxMembers = dto.maxMembers,
                        category = dto.category,
                        hostUserId = dto.hostUserId,
                        isJoined = true
                    )
                    _groups.add(0, group)
                    EduHubLocalStorage.saveGroups(_groups.toList())
                }
            } catch (e: Exception) {
                Log.e("EduHubSupabase", "Failed to lookup group by id $cleanId: ${e.message}")
            }
        }

        if (group == null) {
            return@withContext Result.failure(Exception("Study group not found. Please check the code or link."))
        }

        joinGroup(group.id)
        Result.success(group)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Past Year Paper Repository (Lecturer Upload & Full Cloud Sync)
// ─────────────────────────────────────────────────────────────────────────────
object PastYearRepository {
    private val _papers = mutableListOf<PastYearPaper>()

    init {
        val local = EduHubLocalStorage.loadPastYearPapers()
        if (local.isNotEmpty()) {
            _papers.addAll(local)
        } else {
            // Sample exam papers
            _papers.addAll(
                listOf(
                    PastYearPaper(
                        id = "paper-1",
                        courseCode = "AMIT3353",
                        courseTitle = "Mobile Application Development",
                        session = "2025/2026 Semester 1 Final Exam",
                        subjectCategory = "Mobile App",
                        year = "2025/2026",
                        durationMinutes = 120,
                        totalMarks = 100,
                        pdfUrl = ""
                    ),
                    PastYearPaper(
                        id = "paper-2",
                        courseCode = "BACS2063",
                        courseTitle = "Data Structures & Algorithms",
                        session = "2024/2025 Semester 2 Final Exam",
                        subjectCategory = "Computer Science",
                        year = "2024/2025",
                        durationMinutes = 150,
                        totalMarks = 100,
                        pdfUrl = ""
                    ),
                    PastYearPaper(
                        id = "paper-3",
                        courseCode = "BAIT1013",
                        courseTitle = "Calculus and Linear Algebra",
                        session = "2023/2024 Semester 1 Midterm Exam",
                        subjectCategory = "Calculus",
                        year = "2023/2024",
                        durationMinutes = 90,
                        totalMarks = 50,
                        pdfUrl = ""
                    )
                )
            )
            EduHubLocalStorage.savePastYearPapers(_papers)
        }
    }

    fun getPapers(): List<PastYearPaper> = _papers.toList()

    suspend fun uploadPdfToSupabase(context: Context, uri: Uri, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
            val safeName = "past_year_${UUID.randomUUID()}_${fileName.replace(" ", "_")}"
            SupabaseClientProvider.storage.from("lecture-notes").upload(safeName, bytes) {
                upsert = true
            }
            val publicUrl = "${SupabaseConfig.SUPABASE_URL}/storage/v1/object/public/lecture-notes/$safeName"
            Log.d("EduHubSupabase", "Successfully uploaded Past Year PDF: $publicUrl")
            publicUrl
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to upload Past Year PDF: ${e.message}")
            null
        }
    }

    suspend fun addPaper(paper: PastYearPaper) = withContext(Dispatchers.IO) {
        _papers.add(0, paper)
        EduHubLocalStorage.savePastYearPapers(_papers)

        try {
            SupabaseClientProvider.postgrest.from("past_year_papers").upsert(
                PastYearPaperDto(
                    id = paper.id,
                    courseCode = paper.courseCode,
                    courseTitle = paper.courseTitle,
                    session = paper.session,
                    subjectCategory = paper.subjectCategory,
                    year = paper.year,
                    durationMinutes = paper.durationMinutes,
                    totalMarks = paper.totalMarks,
                    pdfUrl = paper.pdfUrl
                )
            )
            Log.d("EduHubSupabase", "Upserted past year paper '${paper.session}' into Supabase")
        } catch (e: Exception) {
            Log.e("EduHubSupabase", "Failed to upsert past year paper to Supabase: ${e.message}")
        }
    }

    suspend fun fetchPapersFromSupabase(): List<PastYearPaper> = withContext(Dispatchers.IO) {
        try {
            val dtoList = SupabaseClientProvider.postgrest.from("past_year_papers")
                .select()
                .decodeList<PastYearPaperDto>()

            val mapped = dtoList.map { dto ->
                PastYearPaper(
                    id = dto.id,
                    courseCode = dto.courseCode,
                    courseTitle = dto.courseTitle,
                    session = dto.session,
                    subjectCategory = dto.subjectCategory,
                    year = dto.year,
                    durationMinutes = dto.durationMinutes,
                    totalMarks = dto.totalMarks,
                    pdfUrl = dto.pdfUrl
                )
            }
            if (mapped.isNotEmpty()) {
                val merged = (mapped + _papers).distinctBy { it.id }
                _papers.clear()
                _papers.addAll(merged)
                EduHubLocalStorage.savePastYearPapers(_papers)
            }
        } catch (_: Exception) {}
        _papers.toList()
    }

    fun searchPapers(query: String, subjectFilter: String, yearFilter: String): List<PastYearPaper> =
        _papers.filter { p ->
            val q = query.isBlank() || p.courseCode.contains(query, true) ||
                    p.courseTitle.contains(query, true) || p.session.contains(query, true)
            val s = subjectFilter == "All" || p.subjectCategory.contains(subjectFilter, true)
            val y = yearFilter == "All" || p.year.contains(yearFilter, true)
            q && s && y
        }
}