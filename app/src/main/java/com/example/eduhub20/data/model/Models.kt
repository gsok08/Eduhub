package com.example.eduhub20.data.model

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
    val joinedDate: String = "2026-08-20"
)

@Serializable
data class Course(
    val id: String,
    val code: String,
    val title: String,
    val lecturerName: String,
    val joinCode: String = "EDU353",
    val iconCategory: String = "CODE",
    val examDaysLeft: Int = 12,
    val progress: Float = 0.66f
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
    val category: String = "GROUP"
)

@Serializable
data class StudyRoomMember(
    val name: String,
    val currentStatus: String
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
    val attachmentUrl: String? = null
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