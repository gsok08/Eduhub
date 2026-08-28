package com.example.eduhub20.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.eduhub20.data.model.ChatMessage
import com.example.eduhub20.data.model.StudyGroup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EduHubLocalStorage {

    private const val PREFS_NAME = "eduhub_local_storage"
    private const val KEY_STUDY_GROUPS = "persisted_study_groups"
    private const val PREFIX_JOINED_GROUP_IDS = "persisted_joined_group_ids_"
    private const val PREFIX_CHAT_MESSAGES = "persisted_chat_"

    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

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
