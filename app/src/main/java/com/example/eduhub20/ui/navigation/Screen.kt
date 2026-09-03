package com.example.eduhub20.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector


sealed class Screen(
    val route: String,
    val title: String = "",
    val icon: ImageVector? = null
) {

    object Auth : Screen("auth")

    object Home :
        Screen(
            "home",
            "Home",
            Icons.Default.Home
        )

    object PastYear :
        Screen(
            "past_year",
            "Past Paper",
            Icons.Default.Description
        )

    object NoteQuiz :
        Screen(
            "note_quiz",
            "Note/Quiz",
            Icons.Default.EditNote
        )

    object Calendar :
        Screen(
            "calendar",
            "Calendar",
            Icons.Default.CalendarMonth
        )

    object Group :
        Screen(
            "group",
            "Group",
            Icons.Default.Groups
        )

    object Profile :
        Screen("profile")


    // =========================================
    // STUDENT / SHARED ROUTES
    // =========================================

    object CourseDetail :
        Screen(
            "course_detail/{courseId}"
        ) {

        fun createRoute(
            courseId: String
        ): String {

            return "course_detail/$courseId"
        }
    }


    object NoteDetailAi :
        Screen(
            "note_detail_ai/{noteId}"
        ) {

        fun createRoute(
            noteId: String
        ): String {

            return "note_detail_ai/$noteId"
        }
    }


    object QuizTaking :
        Screen(
            "quiz_taking/{noteId}/{courseCode}"
        ) {

        fun createRoute(
            noteId: String,
            courseCode: String
        ): String {

            return "quiz_taking/$noteId/$courseCode"
        }
    }


    object ChatRoom :
        Screen(
            "chat_room/{groupId}"
        ) {

        fun createRoute(
            groupId: String
        ): String {

            return "chat_room/$groupId"
        }
    }


    // =========================================
    // TEAMMATE: GROUP INFO
    // =========================================

    object GroupInfo :
        Screen(
            "group_info/{groupId}"
        ) {

        fun createRoute(
            groupId: String
        ): String {

            return "group_info/$groupId"
        }
    }


    // =========================================
    // LECTURER ROUTES
    // =========================================

    object LecturerHome :
        Screen(
            "lecturer_home"
        )


    object LecturerPapers :
        Screen(
            route = "lecturer_papers",
            title = "Past Year Paper"
        )


    object LecturerCourses :
        Screen(
            route = "lecturer_courses",
            title = "Courses"
        )


    object CreateCourse :
        Screen(
            route = "create_course",
            title = "Create Course"
        )


    object CourseMain :
        Screen(
            route =
                "course_main/{courseId}/{mode}",

            title =
                "Course"
        ) {

        fun createRoute(
            courseId: String,
            mode: String
        ): String {

            return "course_main/$courseId/$mode"
        }
    }


    object Announcement :
        Screen(
            route =
                "announcement/{courseId}",

            title =
                "Announcement"
        ) {

        fun createRoute(
            courseId: String
        ): String {

            return "announcement/$courseId"
        }
    }


    object Materials :
        Screen(
            route =
                "materials/{courseId}",

            title =
                "Materials"
        ) {

        fun createRoute(
            courseId: String
        ): String {

            return "materials/$courseId"
        }
    }


    object Students :
        Screen(
            route =
                "students/{courseId}",

            title =
                "Students"
        ) {

        fun createRoute(
            courseId: String
        ): String {

            return "students/$courseId"
        }
    }


    object UploadLectureNote :
        Screen(
            route =
                "upload_lecture_note/{courseId}",

            title =
                "Upload Lecture Note"
        ) {

        fun createRoute(
            courseId: String
        ): String {

            return "upload_lecture_note/$courseId"
        }
    }


    object PastYearPaperUpload :
        Screen(
            route =
                "past_year_upload/{courseId}",

            title =
                "Upload Past Year Paper"
        ) {

        fun createRoute(
            courseId: String
        ): String {

            return "past_year_upload/$courseId"
        }
    }


    // =========================================
    // TEAMMATE ROUTES
    // =========================================

    object LecturerDashboard :
        Screen(
            "lecturer_dashboard"
        )


    object Pomodoro :
        Screen(
            "pomodoro?roomId={roomId}&roomName={roomName}"
        ) {

        fun createRoute(
            roomId: String = "general",
            roomName: String = "Focus Room"
        ): String {

            val encodedName =
                try {

                    java.net.URLEncoder.encode(
                        roomName,
                        "UTF-8"
                    )

                } catch (_: Exception) {

                    roomName
                }

            return "pomodoro?roomId=$roomId&roomName=$encodedName"
        }
    }


    object TngPayment :
        Screen(
            "tng_payment"
        )
}


// =============================================
// STUDENT BOTTOM NAVIGATION
// =============================================

val bottomNavScreens =
    listOf(
        Screen.Home,
        Screen.PastYear,
        Screen.NoteQuiz,
        Screen.Calendar,
        Screen.Group
    )