package com.example.eduhub20.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.eduhub20.data.model.Announcement
import com.example.eduhub20.data.model.ChatMessage
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.model.LectureNote
import com.example.eduhub20.data.model.StudyGroup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EduHubLocalStorage {

    private const val PREFS_NAME = "eduhub_local_storage"
    private const val KEY_COURSES = "persisted_courses"
    private const val KEY_ANNOUNCEMENTS = "persisted_announcements"
    private const val KEY_LECTURE_NOTES = "persisted_lecture_notes"
    private const val KEY_STUDY_GROUPS = "persisted_study_groups"
    private const val PREFIX_ENROLLED_COURSES = "persisted_enrolled_courses_"
    private const val PREFIX_HIDDEN_COURSES = "persisted_hidden_courses_"
    private const val PREFIX_JOINED_GROUP_IDS = "persisted_joined_group_ids_"
    private const val PREFIX_CHAT_MESSAGES = "persisted_chat_"

    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // ── Courses Persistence ──────────────────────────────────────────────
    fun saveCourses(courses: List<Course>) {
        try {
            val serialized = json.encodeToString(courses)
            prefs?.edit()?.putString(KEY_COURSES, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCourses(): List<Course> {
        return try {
            val serialized = prefs?.getString(KEY_COURSES, null) ?: return emptyList()
            json.decodeFromString<List<Course>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveEnrolledCourseIds(userId: String, courseIds: Set<String>) {
        val safeKey = PREFIX_ENROLLED_COURSES + userId.trim()
        try {
            val serialized = json.encodeToString(courseIds.toList())
            prefs?.edit()?.putString(safeKey, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadEnrolledCourseIds(userId: String): Set<String> {
        val safeKey = PREFIX_ENROLLED_COURSES + userId.trim()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return emptySet()
            json.decodeFromString<List<String>>(serialized).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun saveHiddenCourseIds(userId: String, hiddenIds: Set<String>) {
        val safeKey = PREFIX_HIDDEN_COURSES + userId.trim()
        try {
            val serialized = json.encodeToString(hiddenIds.toList())
            prefs?.edit()?.putString(safeKey, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadHiddenCourseIds(userId: String): Set<String> {
        val safeKey = PREFIX_HIDDEN_COURSES + userId.trim()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return emptySet()
            json.decodeFromString<List<String>>(serialized).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ── Announcements Persistence ────────────────────────────────────────
    fun saveAnnouncements(announcements: List<Announcement>) {
        try {
            val serialized = json.encodeToString(announcements)
            prefs?.edit()?.putString(KEY_ANNOUNCEMENTS, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAnnouncements(): List<Announcement> {
        return try {
            val serialized = prefs?.getString(KEY_ANNOUNCEMENTS, null) ?: return emptyList()
            json.decodeFromString<List<Announcement>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Lecture Notes Persistence ────────────────────────────────────────
    fun saveNotes(notes: List<LectureNote>) {
        try {
            val serialized = json.encodeToString(notes)
            prefs?.edit()?.putString(KEY_LECTURE_NOTES, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadNotes(): List<LectureNote> {
        return try {
            val serialized = prefs?.getString(KEY_LECTURE_NOTES, null) ?: return emptyList()
            json.decodeFromString<List<LectureNote>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Study Groups Persistence ─────────────────────────────────────────
    fun saveGroups(groups: List<StudyGroup>) {
        try {
            val serialized = json.encodeToString(groups)
            prefs?.edit()?.putString(KEY_STUDY_GROUPS, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadGroups(): List<StudyGroup> {
        return try {
            val serialized = prefs?.getString(KEY_STUDY_GROUPS, null) ?: return emptyList()
            json.decodeFromString<List<StudyGroup>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveJoinedGroupIds(userId: String, joinedIds: Set<String>) {
        val safeKey = PREFIX_JOINED_GROUP_IDS + userId.trim()
        try {
            val serialized = json.encodeToString(joinedIds.toList())
            prefs?.edit()?.putString(safeKey, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadJoinedGroupIds(userId: String): Set<String> {
        val safeKey = PREFIX_JOINED_GROUP_IDS + userId.trim()
        return try {
            val serialized = prefs?.getString(safeKey, null) ?: return emptySet()
            json.decodeFromString<List<String>>(serialized).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ── Chat Messages Persistence ────────────────────────────────────────
    fun saveChatMessages(groupId: String, messages: List<ChatMessage>) {
        try {
            val serialized = json.encodeToString(messages)
            prefs?.edit()?.putString(PREFIX_CHAT_MESSAGES + groupId, serialized)?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChatMessages(groupId: String): List<ChatMessage> {
        return try {
            val serialized = prefs?.getString(PREFIX_CHAT_MESSAGES + groupId, null) ?: return emptyList()
            json.decodeFromString<List<ChatMessage>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
