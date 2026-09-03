package com.example.eduhub20.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Auth : Screen("auth")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object PastYear : Screen("past_year", "Past Paper", Icons.Default.Description)
    object NoteQuiz : Screen("note_quiz", "Note/Quiz", Icons.Default.EditNote)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Group : Screen("group", "Group", Icons.Default.Groups)
    object Profile : Screen("profile")

    object CourseDetail : Screen("course_detail/{courseId}") {
        fun createRoute(courseId: String): String = "course_detail/$courseId"
    }

    object NoteDetailAi : Screen("note_detail_ai/{noteId}") {
        fun createRoute(noteId: String): String = "note_detail_ai/$noteId"
    }

    object QuizTaking : Screen("quiz_taking/{noteId}/{courseCode}") {
        fun createRoute(noteId: String, courseCode: String): String = "quiz_taking/$noteId/$courseCode"
    }

    object ChatRoom : Screen("chat_room/{groupId}") {
        fun createRoute(groupId: String): String = "chat_room/$groupId"
    }

    object GroupInfo : Screen("group_info/{groupId}") {
        fun createRoute(groupId: String): String = "group_info/$groupId"
    }

    object LecturerDashboard : Screen("lecturer_dashboard")

    object Pomodoro : Screen("pomodoro?roomId={roomId}&roomName={roomName}") {
        fun createRoute(roomId: String = "general", roomName: String = "Focus Room"): String {
            val encodedName = try {
                java.net.URLEncoder.encode(roomName, "UTF-8")
            } catch (_: Exception) { roomName }
            return "pomodoro?roomId=$roomId&roomName=$encodedName"
        }
    }

    object TngPayment : Screen("tng_payment")
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.PastYear,
    Screen.NoteQuiz,
    Screen.Calendar,
    Screen.Group
)