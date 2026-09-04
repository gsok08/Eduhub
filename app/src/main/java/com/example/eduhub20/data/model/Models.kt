package com.example.eduhub20.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole {
    STUDENT,
    LECTURER
}

data class EduHubUser(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val joinedDate: String = "2026-08-20",
    val avatarUrl: String? = null,
    val campus: String? = null
)

@Serializable
data class Course(
    val id: String,
    val code: String,
    val title: String,
    val lecturerName: String,
    val joinCode: String = "",
    val iconCategory: String = "CODE",
    val examDaysLeft: Int = 12,
    val progress: Float = 0.66f,
    val studentCount: Int = 0,
    val lectureNoteCount: Int = 0,
    val pastYearCount: Int = 0,

)

@Serializable
data class Announcement(
    val id: String,
    val courseId: String,
    val lecturerName: String,
    val date: String,
    val title: String,
    val content: String
)

@Serializable
data class LectureNote(
    val id: String,
    val courseCode: String,
    val courseTitle: String,
    val semesterPeriod: String,
    val chapterTitle: String,
    val rawContent: String,
    val pdfFileName: String? = null,
    val pdfUrl: String? = null,
    val uploadedBy: String = "Lecturer",
    val uploadDate: String = "2026-08-20"
)

@Serializable
data class AiGeneratedNote(
    val id: String,
    val noteId: String,
    val title: String,
    val keyTakeaways: List<String>,
    val keyTerminology: Map<String, String>,
    val summary: String,
    val originalSlidesUrl: String = ""
)

@Serializable
data class QuizQuestion(
    val questionNumber: Int,
    val totalQuestions: Int,
    val questionText: String,
    val tableOrDiagram: String? = null,
    val options: List<String>,
    val correctOptionIndex: Int,
    val reviewExplanation: String
)

@Serializable
data class Quiz(
    val id: String,
    val noteId: String,
    val courseCode: String,
    val title: String,
    val questions: List<QuizQuestion>,
    val isCompleted: Boolean = false,
    val scorePercentage: Int = 0
)

@Serializable
data class QuizHistoryItem(
    val id: String,
    val noteId: String,
    val courseCode: String,
    val title: String,
    val scorePercentage: Int,
    val isCompleted: Boolean
)

@Serializable
data class StudyGroup(
    val id: String,
    val name: String,
    val host: String,
    val details: String,
    val currentMembers: Int,
    val maxMembers: Int,
    val isJoined: Boolean = false,
    val category: String = "GROUP",
    val hostUserId: String = "",
    val courseId: String = "",
    val courseCode: String = "",
    val courseTitle: String = "",
    val status: String = "INACTIVE"
)

@Serializable
data class StudyGroupMember(
    val id: String = "",
    val groupId: String,
    val userId: String,
    val joinedAt: String = ""
)

@Serializable
data class ChatMessage(
    val id: String,
    val groupId: String,
    val senderName: String,
    val senderRole: String = "Student",
    val message: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val attachmentUrl: String? = null,
    val senderAvatarUrl: String? = null,
    val senderId: String = ""
)

@Serializable
data class GroupMember(
    val id: String = "",
    val groupId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val role: String = "MEMBER" // "HOST", "ADMIN", "MEMBER"
)

@Serializable
data class CalendarTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val date: String = "2026-06-28"
)

@Serializable
data class ExamCountdown(
    val courseCode: String,
    val courseTitle: String,
    val daysLeft: Int
)

@Serializable
data class PastYearPaper(
    val id: String,
    val courseCode: String,
    val courseTitle: String,
    val session: String,
    val subjectCategory: String = "Mobile App",
    val year: String = "2025/2026",
    val durationMinutes: Int = 120,
    val totalMarks: Int = 100,
    val pdfUrl: String = ""
)

// ✅ Add this - Campus data class
@Serializable
data class Campus(
    val id: String,
    val name: String,
    val location: String,
    val iconRes: Int = 0  // Optional: for different campus icons
)

@Serializable
data class ExamEntity(
    val id: String,
    val userId: String,
    val name: String,
    val date: Long,
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class TaskEntity(
    val id: String,
    val userId: String,
    val name: String,
    val date: Long,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class ReminderEntity(
    val id: String,
    val userId: String,
    val name: String,
    val date: Long,
    val time: String,
    @SerialName("created_at")
    val createdAt: String = ""
)

// ✅ Add this - Campus list
object CampusData {
    val campusList = listOf(
        Campus("campus_1", "Kuala Lumpur", "Kuala Lumpur"),
        Campus("campus_2", "Perak", "Kampar"),
        Campus("campus_3", "Penang", "Tanjong Bungah"),
        Campus("campus_4", "Johor", "Segamat"),
        Campus("campus_5", "Pahang", "Kuantan"),
        Campus("campus_6", "Sabah", "Kota Kinabalu")
    )

    // Helper function to get campus by ID
    fun getCampusById(id: String): Campus? {
        return campusList.find { it.id == id }
    }

    // Helper function to get campus by name
    fun getCampusByName(name: String): Campus? {
        return campusList.find { it.name.equals(name, ignoreCase = true) }
    }
}