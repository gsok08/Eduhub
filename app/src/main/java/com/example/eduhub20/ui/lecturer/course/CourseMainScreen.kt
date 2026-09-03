package com.example.eduhub20.ui.lecturer.course

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eduhub20.data.model.Course
import com.example.eduhub20.data.repository.CourseRepository
import com.example.eduhub20.ui.lecturer.materials.LecturerAnnouncementsTab
import com.example.eduhub20.ui.lecturer.materials.MaterialsScreen

enum class LecturerCourseMode {
    VIEW,
    MANAGE;

    companion object {
        fun fromString(value: String): LecturerCourseMode {
            return if (
                value.equals("manage", ignoreCase = true)
            ) {
                MANAGE
            } else {
                VIEW
            }
        }
    }
}

@Composable
fun CourseMainScreen(
    courseId: String,
    mode: LecturerCourseMode,
    onBack: () -> Unit,
    onAddAnnouncement: () -> Unit,
    onUploadNote: (String) -> Unit,
    onUploadPaper: (String) -> Unit
) {

    var selectedTab by rememberSaveable {
        mutableIntStateOf(0)
    }

    var course by remember(courseId) {
        mutableStateOf<Course?>(
            CourseRepository.getCourseById(courseId)
        )
    }

    var loading by remember {
        mutableStateOf(course == null)
    }

    LaunchedEffect(courseId) {

        loading = true

        CourseRepository.fetchCoursesFromSupabase()

        course =
            CourseRepository.getCourseById(courseId)

        loading = false
    }

    if (loading) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        return
    }

    val selectedCourse = course

    if (selectedCourse == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text("Course not found")
        }

        return
    }

    val canManage =
        mode == LecturerCourseMode.MANAGE

    val tabs = listOf(
        "Announcements",
        "Materials",
        "Students"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column {

                Text(
                    text = selectedCourse.code,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    text = selectedCourse.title,
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        if (canManage) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),

                shape =
                    RoundedCornerShape(14.dp),

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(alpha = 0.35f)
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Course Join Code",
                        style =
                            MaterialTheme.typography
                                .labelMedium,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            if (
                                selectedCourse.joinCode
                                    .isNotBlank()
                            ) {
                                selectedCourse.joinCode
                            } else {
                                "No join code"
                            },

                        style =
                            MaterialTheme.typography
                                .headlineSmall,

                        color =
                            MaterialTheme.colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Share this code with students to join the course.",

                        style =
                            MaterialTheme.typography
                                .bodySmall,

                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab
        ) {

            tabs.forEachIndexed { index, title ->

                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                    },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            when (selectedTab) {

                0 -> {

                    LecturerAnnouncementsTab(
                        courseId = selectedCourse.id,
                        canManage = canManage,
                        onAddAnnouncement =
                            onAddAnnouncement
                    )
                }

                1 -> {

                    MaterialsScreen(
                        courseCode =
                            selectedCourse.code,
                        canManage = canManage,

                        onUploadNote = {
                            onUploadNote(
                                selectedCourse.code
                            )
                        },

                        onOpenPaper = {
                            onUploadPaper(
                                selectedCourse.code
                            )
                        }
                    )
                }

                2 -> {

                    StudentsScreen(
                        courseId =
                            selectedCourse.id,
                        canManage = canManage
                    )
                }
            }
        }
    }
}