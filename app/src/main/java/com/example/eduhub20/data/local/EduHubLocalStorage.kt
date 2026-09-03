package com.example.eduhub20.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.eduhub20.data.model.AiGeneratedNote
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.model.ChatMessage
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.model.StudyGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SerializedStrokePoint(val x: Float, val y: Float)

@Serializable
data class SerializedDrawStroke(
    val points: List<SerializedStrokePoint>,
    val colorArgb: Long,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val pageIndex: Int = 0
)

@Serializable
data class SerializedStickyNote(
    val id: String,
    val pageNumber: Int,
    val content: String,
    val colorHex: Long = 0xFFFEF08A
)

@Serializable
data class PdfAnnotationData(
    val strokes: List<SerializedDrawStroke> = emptyList(),
    val stickyNotes: List<SerializedStickyNote> = emptyList()
)

object EduHubLocalStorage {

    private const val PREFS_NAME = "eduhub_local_cache"
    private const val KEY_COURSES = "cached_courses"
    private const val KEY_ANNOUNCEMENTS = "cached_announcements"
    private const val KEY_NOTES = "cached_lecture_notes"
    private const val KEY_AI_NOTES_MAP = "cached_ai_notes_map"
    private const val KEY_STUDY_GROUPS = "cached_study_groups"
    private const val PREFIX_JOINED_GROUP_IDS = "joined_group_ids_"
    private const val PREFIX_ENROLLED_COURSE_IDS = "enrolled_course_ids_"
    private const val PREFIX_HIDDEN_COURSE_IDS = "hidden_course_ids_"
    private const val PREFIX_CHAT_MESSAGES = "chat_messages_"
    private const val PREFIX_PDF_ANNOTATIONS = "pdf_annotations_"

    private var prefs: SharedPreferences? = null
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // ── Courses Persistence ───────────────────────────────────────────────
    fun saveCourses(courses: List<Course>) {
        try {
            val serialized = json.encodeToString(courses)
            prefs?.edit { putString(KEY_COURSES, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadCourses(): List<Course> {
        return try {
            val serialized = prefs?.getString(KEY_COURSES, null) ?: return emptyList()
            json.decodeFromString<List<Course>>(serialized)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveEnrolledCourseIds(userId: String, enrolledIds: Set<String>) {
        val safeKey = PREFIX_ENROLLED_COURSE_IDS + userId.trim()
        try {
            val serialized = json.encodeToString(enrolledIds.toList())
            prefs?.edit { putString(safeKey, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadEnrolledCourseIds(userId: String): Set<String> {
        val safeKey = PREFIX_ENROLLED_COURSE_IDS + userId.trim()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return emptySet()
            json.decodeFromString<List<String>>(serialized).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun saveHiddenCourseIds(userId: String, hiddenIds: Set<String>) {
        val safeKey = PREFIX_HIDDEN_COURSE_IDS + userId.trim()
        try {
            val serialized = json.encodeToString(hiddenIds.toList())
            prefs?.edit { putString(safeKey, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadHiddenCourseIds(userId: String): Set<String> {
        val safeKey = PREFIX_HIDDEN_COURSE_IDS + userId.trim()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return emptySet()
            json.decodeFromString<List<String>>(serialized).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    // ── Announcements Persistence ────────────────────────────────────────
    fun saveAnnouncements(announcements: List<Announcement>) {
        try {
            val serialized = json.encodeToString(announcements)
            prefs?.edit { putString(KEY_ANNOUNCEMENTS, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadAnnouncements(): List<Announcement> {
        return try {
            val serialized = prefs?.getString(KEY_ANNOUNCEMENTS, null) ?: return emptyList()
            json.decodeFromString<List<Announcement>>(serialized)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Notes Persistence ────────────────────────────────────────────────
    fun saveNotes(notes: List<LectureNote>) {
        try {
            val serialized = json.encodeToString(notes)
            prefs?.edit { putString(KEY_NOTES, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadNotes(): List<LectureNote> {
        return try {
            val serialized = prefs?.getString(KEY_NOTES, null) ?: return emptyList()
            json.decodeFromString<List<LectureNote>>(serialized)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── AI Generated Study Notes Persistence ─────────────────────────────
    fun saveAiNote(noteId: String, aiNote: AiGeneratedNote) {
        val currentMap = loadAllAiNotes().toMutableMap()
        currentMap[noteId] = aiNote
        saveAllAiNotes(currentMap)
    }

    fun loadAiNote(noteId: String): AiGeneratedNote? {
        val currentMap = loadAllAiNotes()
        return currentMap[noteId]
    }

    fun saveAllAiNotes(map: Map<String, AiGeneratedNote>) {
        try {
            val serialized = json.encodeToString(map)
            prefs?.edit { putString(KEY_AI_NOTES_MAP, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadAllAiNotes(): Map<String, AiGeneratedNote> {
        return try {
            val serialized = prefs?.getString(KEY_AI_NOTES_MAP, null) ?: return emptyMap()
            json.decodeFromString<Map<String, AiGeneratedNote>>(serialized)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // ── Study Groups Persistence ─────────────────────────────────────────
    fun saveGroups(groups: List<StudyGroup>) {
        try {
            val serialized = json.encodeToString(groups)
            prefs?.edit { putString(KEY_STUDY_GROUPS, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadGroups(): List<StudyGroup> {
        return try {
            val serialized = prefs?.getString(KEY_STUDY_GROUPS, null) ?: return emptyList()
            json.decodeFromString<List<StudyGroup>>(serialized)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveJoinedGroupIds(userId: String, joinedIds: Set<String>) {
        val safeKey = PREFIX_JOINED_GROUP_IDS + userId.trim()
        try {
            val serialized = json.encodeToString(joinedIds.toList())
            prefs?.edit { putString(safeKey, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadJoinedGroupIds(userId: String): Set<String> {
        val safeKey = PREFIX_JOINED_GROUP_IDS + userId.trim()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return emptySet()
            json.decodeFromString<List<String>>(serialized).toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    // ── Chat Messages Persistence ────────────────────────────────────────
    fun saveChatMessages(groupId: String, messages: List<ChatMessage>) {
        try {
            val serialized = json.encodeToString(messages)
            prefs?.edit { putString(PREFIX_CHAT_MESSAGES + groupId, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadChatMessages(groupId: String): List<ChatMessage> {
        return try {
            val serialized = prefs?.getString(PREFIX_CHAT_MESSAGES + groupId, null) ?: return emptyList()
            json.decodeFromString<List<ChatMessage>>(serialized)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearAllChatMessages() {
        try {
            val allKeys = prefs?.all?.keys ?: return
            val chatKeys = allKeys.filter { it.startsWith(PREFIX_CHAT_MESSAGES) }
            prefs?.edit {
                for (k in chatKeys) {
                    remove(k)
                }
            }
        } catch (_: Exception) {
        }
    }

    // ── PDF Slide Annotations Persistence ────────────────────────────────
    fun savePdfAnnotations(documentKey: String, data: PdfAnnotationData) {
        val safeKey = PREFIX_PDF_ANNOTATIONS + documentKey.hashCode().toString()
        try {
            val serialized = json.encodeToString(data)
            prefs?.edit { putString(safeKey, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadPdfAnnotations(documentKey: String): PdfAnnotationData? {
        val safeKey = PREFIX_PDF_ANNOTATIONS + documentKey.hashCode().toString()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return null
            json.decodeFromString<PdfAnnotationData>(serialized)
        } catch (_: Exception) {
            null
        }
    }

    // ── Past Year Papers Persistence ────────────────────────────────────
    private const val KEY_PAST_PAPERS = "cached_past_year_papers"

    fun savePastYearPapers(papers: List<com.example.eduhub20.data.model.PastYearPaper>) {
        try {
            val serialized = json.encodeToString(papers)
            prefs?.edit { putString(KEY_PAST_PAPERS, serialized) }
        } catch (_: Exception) {
        }
    }

    fun loadPastYearPapers(): List<com.example.eduhub20.data.model.PastYearPaper> {
        return try {
            val serialized = prefs?.getString(KEY_PAST_PAPERS, null) ?: return emptyList()
            json.decodeFromString<List<com.example.eduhub20.data.model.PastYearPaper>>(serialized)
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Student Name Cache ───────────────────────────────────────────────
    fun saveStudentName(userId: String, name: String) {
        if (userId.isNotBlank() && name.isNotBlank()) {
            prefs?.edit { putString("student_name_" + userId.trim(), name.trim()) }
        }
    }

    fun loadStudentName(userId: String): String? {
        return prefs?.getString("student_name_" + userId.trim(), null)
    }
}
