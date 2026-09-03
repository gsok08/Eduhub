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

    object LecturerHome : Screen("lecturer_home")

    object LecturerPapers : Screen(
        route = "lecturer_papers",
        title = "Papers"
    )

    object LecturerCourses : Screen(
        route = "lecturer_courses",
        title = "Courses"
    )

    object CreateCourse : Screen(
        route = "create_course",
        title = "Create Course"
    )

    object CourseMain : Screen(
        route = "course_main/{courseId}/{mode}",
        title = "Course"
    ) {

        fun createRoute(
            courseId: String,
            mode: String
        ): String {
            return "course_main/$courseId/$mode"
        }
    }
    object Announcement : Screen(
        route = "announcement/{courseId}",
        title = "Announcement"
    ) {
        fun createRoute(courseId: String) =
            "announcement/$courseId"
    }
    object Materials : Screen(
        route = "materials/{courseId}",
        title = "Materials"
    ) {
        fun createRoute(courseId: String) =
            "materials/$courseId"
    }
    object Students : Screen(
        route = "students/{courseId}",
        title = "Students"
    ) {
        fun createRoute(courseId: String) =
            "students/$courseId"
    }
    object UploadLectureNote : Screen(
        route = "upload_lecture_note/{courseId}",
        title = "Upload Lecture Note"
    ){
        fun createRoute(courseId: String) =
            "upload_lecture_note/$courseId"
    }

    object PastYearPaperUpload : Screen(
        route = "past_year_upload/{courseId}",
        title = "Upload Past Year Paper"
    ){
        fun createRoute(courseId:String)=
            "past_year_upload/$courseId"
    }
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.PastYear,
    Screen.NoteQuiz,
    Screen.Calendar,
    Screen.Group
)